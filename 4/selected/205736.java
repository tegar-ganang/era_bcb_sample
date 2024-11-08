package com.faunos.skwish.demo.tikluc;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.TeeContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import com.faunos.skwish.SegmentStore;
import com.faunos.skwish.TxnSegment;
import com.faunos.util.io.BufferUtil;
import com.faunos.util.io.file.DirectoryOrdering;
import com.faunos.util.io.file.FileSystemTraverser;
import com.faunos.util.tree.TraverseListener;
import com.faunos.util.xml.ExternalLinkHandler;
import com.faunos.util.xml.LinkSequenceHandler;
import com.faunos.util.xml.LinkSequenceHandler.TypedLink;

/**
 *
 *
 * @author Babak Farhang
 */
public class FileImporter implements Runnable {

    private final TxnSegment cacheTxn;

    private final long cacheTxnId;

    private final TxnSegment tikaTxn;

    private final long tikaTxnId;

    private final TxnSegment metaTxn;

    private final File root;

    private final String parentFilepath;

    private final boolean escapeBackslash;

    private final Parser tikaParser;

    private final SAXTransformerFactory saxHandlerFactory;

    private long count;

    private final AtomicLong globalCount;

    private FileFilter filter;

    private static final Logger logger = Logger.getLogger(FileImporter.class.getName());

    FileImporter(SegmentStore cache, SegmentStore tika, SegmentStore meta, File src, AtomicLong globalCount) throws IOException {
        if (!src.exists()) throw new IllegalArgumentException("no such file or directory: " + src);
        this.cacheTxn = cache.newTransaction();
        this.cacheTxnId = cacheTxn.getTxnId();
        this.tikaTxn = tika.newTransaction();
        this.tikaTxnId = tikaTxn.getTxnId();
        this.metaTxn = meta.newTransaction();
        this.root = src.getCanonicalFile();
        this.parentFilepath = root.getParent();
        this.escapeBackslash = File.separatorChar == '\\';
        this.tikaParser = new AutoDetectParser();
        this.saxHandlerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        this.globalCount = globalCount;
    }

    public void setFilter(FileFilter filter) {
        this.filter = filter;
    }

    public void run() {
        FileSystemTraverser traverser = new FileSystemTraverser(root);
        traverser.setSiblingOrder(DirectoryOrdering.FILE_FIRST);
        traverser.setFilter(filter);
        traverser.setListener(new TraverseListener<File>() {

            public void preorder(File file) {
                try {
                    processFile(file);
                } catch (Exception x) {
                    logger.warning(name() + x.getMessage());
                }
            }

            public void postorder(File file) {
            }
        });
        System.out.println(name() + "starting import..");
        try {
            System.out.println(name() + "cache.tid=" + cacheTxn.getTxnId());
            long time = System.currentTimeMillis();
            traverser.run();
            cacheTxn.commit();
            tikaTxn.commit();
            metaTxn.commit();
            time = System.currentTimeMillis() - time;
            System.out.println(name() + "imported " + (count == 1 ? "1 file" : count + " files") + " in " + time + " msec [" + (time + 500) / 1000 + " sec]");
            System.out.println(name() + "running total of files imported: " + incrementGlobalCount(count));
        } catch (IOException iox) {
            throw new RuntimeException(iox);
        }
    }

    private long incrementGlobalCount(long count) {
        return globalCount == null ? count : globalCount.addAndGet(count);
    }

    public long getCount() {
        return count;
    }

    private String name() {
        return "[" + Thread.currentThread().getName() + "]: ";
    }

    private void processFile(File file) throws IOException, SAXException, TikaException, TransformerConfigurationException {
        String uri = file.getPath().substring(this.parentFilepath.length());
        if (escapeBackslash) uri = uri.replace('\\', '/');
        if (file.isDirectory()) {
            System.out.println(name() + "skipping directory entry " + uri);
            return;
        }
        System.out.println(name() + uri);
        FileChannel input = new FileInputStream(file).getChannel();
        final long cid = cacheTxn.insertEntry(input);
        input.close();
        final long tkid = tikaTxn.getNextId();
        FileChannel tkOut = tikaTxn.getEntryInsertionChannel();
        OutputStream outputStream = Channels.newOutputStream(tkOut);
        Writer writer = new OutputStreamWriter(outputStream);
        TransformerHandler handler = saxHandlerFactory.newTransformerHandler();
        handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "text");
        handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "no");
        handler.setResult(new StreamResult(writer));
        LinkSequenceHandler linkHandler = new ExternalLinkHandler();
        ContentHandler comboHandler = new TeeContentHandler(handler, linkHandler);
        Metadata metadata = new Metadata();
        metadata.set(Metadata.RESOURCE_NAME_KEY, file.getName());
        input = cacheTxn.getEntryChannel(cid);
        InputStream stream = Channels.newInputStream(input);
        try {
            this.tikaParser.parse(stream, comboHandler, metadata);
        } finally {
            writer.close();
            stream.close();
        }
        final long lid;
        StringBuilder metaString;
        final int metaStringInitCap = (2 + ((uri.length() + 48) / 64)) * 64;
        List<TypedLink> links = linkHandler.getLinks();
        if (links.isEmpty()) {
            lid = -1;
            metaString = new StringBuilder(metaStringInitCap);
        } else {
            metaString = new StringBuilder(Math.max(metaStringInitCap, links.size() * 32));
            for (TypedLink link : links) {
                metaString.append(link.getType()).append(' ').append(link.getUri()).append('\n');
            }
            ByteBuffer linkEntry = BufferUtil.INSTANCE.asciiBuffer(metaString);
            lid = tikaTxn.insertEntry(linkEntry);
            metaString.setLength(0);
        }
        metaString.append("uri=").append(uri).append("\ncache.id=").append(cid).append("\ncache.tid=").append(cacheTxnId).append("\ntika.txt.id=").append(tkid).append("\ntika.tid=").append(tikaTxnId);
        if (lid != -1) metaString.append("\ntika.link.id=").append(lid);
        metadata.remove(TikaMetadataKeys.RESOURCE_NAME_KEY);
        String[] tikaMetaKeys = metadata.names();
        for (String tikaKey : tikaMetaKeys) {
            String[] values = metadata.getValues(tikaKey);
            for (String value : values) {
                metaString.append("\ntika.m.").append(tikaKey).append('=').append(value.trim());
            }
        }
        ByteBuffer metaEntry = BufferUtil.INSTANCE.asciiBuffer(metaString);
        metaTxn.insertEntry(metaEntry);
        ++count;
    }
}
