package net.sourceforge.stat4j.log4j;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.WireFeedOutput;

/**
 * It creates a feed and writes it to a file.
 * <p>
 * 
 * @author Lara D'Abreo
 *  
 */
public class FeedWriter {

    protected int MAGIC_NUMBER = 6;

    protected static String rss = "rss";

    protected static String channelTail = "</channel>";

    protected static String atomTail = "</feed>";

    protected static String rssTail = "</rss>";

    protected String feedType;

    protected SyndFeed feed;

    protected File file;

    protected boolean fileExists = false;

    protected boolean fileAppend = true;

    protected boolean isRSS;

    protected OutputStream fos;

    protected Writer writer;

    protected RandomAccessFile raFile;

    protected FileChannel fileChannel;

    protected Format format;

    protected XMLOutputter outputter;

    protected WireFeedOutput wfoutput;

    protected SyndFeedOutput output;

    /**
     * 
     * @param feedType
     * @param filename
     * @param title
     * @param description
     * @param link
     * @param author
     * @param encoding
     * @param language
     */
    public FeedWriter(String feedType, String filename, boolean fileAppend, String title, String description, String link, String author, String encoding, String language) {
        this.feedType = feedType;
        this.file = new File(filename);
        this.fileAppend = fileAppend;
        this.feed = new SyndFeedImpl();
        feed.setFeedType(feedType);
        feed.setLink(link);
        feed.setTitle(title);
        feed.setDescription(description);
        feed.setAuthor(author);
        feed.setEncoding(encoding);
        feed.setLanguage(language);
        this.isRSS = feedType.startsWith(rss);
        this.format = Format.getPrettyFormat();
        if (encoding != null) {
            format.setEncoding(encoding);
        }
        this.outputter = new XMLOutputter(format);
        this.wfoutput = new WireFeedOutput();
        this.output = new SyndFeedOutput();
    }

    public SyndEntry toRomeEntry(RSSEntry entry) {
        SyndEntry romeEntry = new SyndEntryImpl();
        romeEntry.setAuthor(entry.getAuthor());
        if (entry.getDescription() != null) {
            SyndContentImpl content = new SyndContentImpl();
            content.setValue(entry.getDescription());
            romeEntry.setDescription(content);
        }
        romeEntry.setLink(entry.getLink());
        romeEntry.setPublishedDate(entry.getPublishedDate());
        romeEntry.setTitle(entry.getTitle());
        romeEntry.setUri(entry.getUri());
        return romeEntry;
    }

    /**
     * Write RSSEntries to the RSS feed
     * 
     * @param entries
     * @throws Exception
     */
    public void writeEntries(RSSEntry[] entries) throws Exception {
        List romeEntries = new ArrayList(entries.length);
        for (int i = 0; i < entries.length; ++i) {
            romeEntries.add(toRomeEntry(entries[i]));
        }
        feed.setEntries(romeEntries);
        boolean createFeed = createFeed();
        if (writer == null) {
            setupFileWriter();
        }
        if (createFeed) {
            feed.setPublishedDate(new Date());
            generateFeedFile();
        } else {
            appendToFeedFile();
        }
    }

    protected synchronized boolean createFeed() {
        if (!fileExists) {
            fileExists = file.exists();
        }
        return !fileExists || !fileAppend;
    }

    /**
     * @return Returns the isRSS.
     */
    public boolean isRSS() {
        return isRSS;
    }

    protected void appendToFeedFile() throws Exception {
        try {
            trimTail((isRSS) ? channelTail.length() + rssTail.length() : atomTail.length() + rssTail.length());
            WireFeed wfeed = feed.createWireFeed();
            Document doc = wfoutput.outputJDom(wfeed);
            Element root = doc.getRootElement();
            if (isRSS()) {
                List channels = root.getChildren();
                for (int i = 0; i < channels.size(); ++i) {
                    Element channel = (Element) channels.get(i);
                    List items = channel.getChildren();
                    printEntries(writer, items, "item");
                    writeNewLine(writer);
                    indent(writer, 1);
                    writer.write(channelTail);
                }
            } else {
                List items = root.getChildren();
                printEntries(writer, items, "entry");
                writeNewLine(writer);
                indent(writer, 1);
                writer.write(atomTail);
            }
            writeNewLine(writer);
            writer.write(rssTail);
            writer.flush();
        } catch (Exception e) {
            close();
            throw e;
        }
    }

    protected void generateFeedFile() throws Exception {
        output.output(feed, writer);
        writer.flush();
        fileExists = true;
        fileAppend = true;
    }

    protected void writeNewLine(Writer writer) throws Exception {
        writer.write(format.getLineSeparator());
    }

    protected void indent(Writer writer, int level) throws Exception {
        for (int i = 0; i < level; i++) {
            writer.write(format.getIndent());
        }
    }

    protected void printEntries(Writer writer, List elements, String name) throws Exception {
        indent(writer, 2);
        for (int i = 0; i < elements.size(); ++i) {
            Element item = (Element) elements.get(i);
            if (item.getName().equals(name)) {
                outputter.output(item, writer);
            }
        }
    }

    public void close() {
        try {
            if (writer != null) {
                writer.close();
                writer = null;
            }
            if (fos != null) {
                fos.close();
                fos = null;
            }
            if (fileChannel != null) {
                fileChannel.close();
                fileChannel = null;
            }
            if (raFile != null) {
                raFile.close();
                raFile = null;
            }
        } catch (Exception e) {
        }
    }

    protected void setupFileWriter() throws Exception {
        try {
            raFile = new RandomAccessFile(file, "rw");
            fileChannel = raFile.getChannel();
            fos = Channels.newOutputStream(fileChannel);
            writer = new OutputStreamWriter(fos);
        } catch (Exception e) {
            close();
            throw e;
        }
    }

    protected void trimTail(int trim) throws Exception {
        long size = fileChannel.size();
        fileChannel.truncate(size - (trim + MAGIC_NUMBER));
        fileChannel.force(false);
    }
}
