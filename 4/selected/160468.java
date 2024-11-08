package com.faunos.skwish.eg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import com.faunos.skwish.SegmentStore;
import com.faunos.skwish.TxnSegment;
import com.faunos.skwish.ext.http.SkwishHttpMountPoint;
import com.faunos.util.io.BufferUtil;
import com.faunos.util.io.file.DirectoryOrdering;
import com.faunos.util.io.file.FileSystemTraverser;
import com.faunos.util.main.Arguments;
import com.faunos.util.net.http.Caroon;
import com.faunos.util.test.AbbreviatedFilepath;
import com.faunos.util.tree.TraverseListener;

/**
 *
 *
 * @author Babak Farhang
 */
public class FileStore {

    public static final String HELP_OPT = "h";

    public static final String CREATE_OPT = "c";

    public static final String WEB_OPT = "w";

    public static void main(String[] args) throws Exception {
        try {
            mainImpl(args);
        } catch (Exception e) {
            printUsage(System.err);
            printCurrentDir(System.err);
            throw e;
        }
    }

    private static void mainImpl(String[] args) throws Exception {
        Arguments arguments = new Arguments();
        arguments.getOptions().getIdentifiers().add(HELP_OPT);
        arguments.getOptions().getIdentifiers().add(CREATE_OPT);
        arguments.getOptions().getIdentifiers().add(WEB_OPT);
        arguments.parse(args);
        if (arguments.getOptionList().contains(HELP_OPT)) {
            printUsage(System.out);
            return;
        }
        List<String> filepaths = arguments.getArgumentList();
        if (filepaths.size() < 1) throw new IllegalArgumentException(filepaths.toString());
        File storageDir = new File(filepaths.get(0));
        final boolean create = arguments.getOptionList().contains(CREATE_OPT);
        if (create) storageDir.mkdirs();
        if (!storageDir.isDirectory()) throw new IllegalArgumentException(storageDir + " is not a directory");
        final SegmentStore cache, meta;
        {
            File cacheDir = new File(storageDir, "cache");
            File metaDir = new File(storageDir, "meta");
            if (create) {
                cache = SegmentStore.writeNewInstance(cacheDir.getPath());
                meta = SegmentStore.writeNewInstance(metaDir.getPath());
            } else {
                cache = SegmentStore.loadInstance(cacheDir.getPath());
                meta = SegmentStore.loadInstance(metaDir.getPath());
            }
        }
        Caroon http;
        if (arguments.getOptionList().contains(WEB_OPT)) {
            http = new Caroon(8880, true);
            SkwishHttpMountPoint mountPoint = SkwishHttpMountPoint.newSkwishMountPoint("/", http);
            mountPoint.mapRelativeUriToSkwish("cache", cache);
            mountPoint.mapRelativeUriToSkwish("meta", meta);
            http.start();
        } else http = null;
        for (int i = 1; i < filepaths.size(); ++i) {
            FileImporter importer = new FileImporter(cache, meta, new File(filepaths.get(i)));
            new Thread(importer).start();
        }
        try {
            Thread.sleep(120000);
        } catch (InterruptedException ix) {
        }
    }

    private static void printUsage(PrintStream ps) {
        ps.println();
        ps.println("Usage:");
        ps.println(" java " + FileStore.class.getName() + " [-chw] <storage dir> [source file/dir]*");
        ps.println();
        ps.println(" Arguments:");
        ps.println("   <storage dir>      The storage directory. Required.");
        ps.println("   [source file/dir]* Zero or more files or directories " + "for import. Each");
        ps.println("                      file or directory is imported in a " + "separate thread.");
        ps.println();
        ps.println(" Options:");
        ps.println("   -c  Creates a new storage dir");
        ps.println("   -h  Prints this message");
        ps.println("   -w  Exposes storage contents via HTTP");
        ps.println();
        ps.println(" Description:");
        ps.println("   A Skwish demo program that imports files into a " + "\"storage\" container.");
        ps.println("   The container consists of 2 segment stores:");
        ps.println("     1. cache. Contains file contents.");
        ps.println("     2. meta.  Contains meta information about the files.");
        ps.println("   Both segment stores are accessible through HTTP " + "(option -w).");
        ps.println();
    }

    private static void printCurrentDir(PrintStream ps) throws IOException {
        File currentDir = new File(".").getCanonicalFile();
        ps.println("current dir: " + new AbbreviatedFilepath(currentDir));
        ps.println("    " + currentDir);
        ps.println();
    }

    static class FileImporter implements Runnable {

        private final TxnSegment cacheTxn;

        private final long cacheTxnId;

        private final TxnSegment metaTxn;

        private final File root;

        private final String parentFilepath;

        private final boolean escapeBackslash;

        private int count;

        FileImporter(SegmentStore cache, SegmentStore meta, File src) throws IOException {
            if (!src.exists()) throw new IllegalArgumentException("no such file or directory: " + src);
            cacheTxn = cache.newTransaction();
            cacheTxnId = cacheTxn.getTxnId();
            metaTxn = meta.newTransaction();
            this.root = src.getCanonicalFile();
            this.parentFilepath = root.getParent();
            this.escapeBackslash = File.separatorChar == '\\';
        }

        public void run() {
            FileSystemTraverser traverser = new FileSystemTraverser(root);
            traverser.setSiblingOrder(DirectoryOrdering.FILE_FIRST);
            traverser.setListener(new TraverseListener<File>() {

                public void preorder(File file) {
                    try {
                        processFile(file);
                    } catch (IOException iox) {
                        throw new RuntimeException(iox);
                    }
                }

                public void postorder(File file) {
                }
            });
            System.out.println(name() + "starting import..");
            try {
                System.out.println(name() + "c.tid=" + cacheTxn.getTxnId());
                long time = System.currentTimeMillis();
                traverser.run();
                cacheTxn.commit();
                metaTxn.commit();
                time = System.currentTimeMillis() - time;
                System.out.println(name() + "imported " + (count == 1 ? "1 file" : count + " files") + " in " + time + " msec [" + (time + 500) / 1000 + " sec]");
            } catch (IOException iox) {
                throw new RuntimeException(iox);
            }
        }

        private String name() {
            return "[" + Thread.currentThread().getName() + "]: ";
        }

        private void processFile(File file) throws IOException {
            String uri = file.getPath().substring(this.parentFilepath.length());
            if (escapeBackslash) uri = uri.replace('\\', '/');
            if (file.isDirectory()) {
                System.out.println(name() + "skipping directory entry " + uri);
                return;
            }
            System.out.println(name() + uri);
            FileChannel input = new FileInputStream(file).getChannel();
            long cid = cacheTxn.insertEntry(input);
            input.close();
            StringBuilder metaString = new StringBuilder(uri.length() + 32);
            metaString.append("uri=").append(uri).append("\nc.id=").append(cid).append("\nc.tid=").append(cacheTxnId);
            ByteBuffer metaEntry = BufferUtil.INSTANCE.asciiBuffer(metaString);
            metaTxn.insertEntry(metaEntry);
            ++count;
        }
    }
}
