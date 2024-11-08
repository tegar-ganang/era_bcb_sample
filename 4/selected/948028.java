package uk.co.pointofcare.echobase.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.log4j.Logger;

/**
 * @author RCHALLEN
 *
 */
public class ZipIO {

    static Logger log = Logger.getLogger(ZipIO.class);

    static ArrayList<String> ls(File filename) throws IOException {
        ArrayList<String> out = new ArrayList<String>();
        ZipFile z = new ZipFile(filename);
        Enumeration<? extends ZipEntry> entries = z.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = (ZipEntry) entries.nextElement();
            out.add(e.getName());
        }
        return out;
    }

    public static void extract(File zipfile, File destDir) throws IOException {
        extract(zipfile, destDir, new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return true;
            }
        });
    }

    public static void extract(File zipfile, File destDir, FilenameFilter filter) throws IOException {
        FileInputStream fis = new FileInputStream(zipfile);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
        ZipEntry zipentry;
        while ((zipentry = zis.getNextEntry()) != null) {
            log.debug("Unzipping: " + zipentry.getName());
            File f = new File(destDir, zipentry.getName());
            if (!f.isDirectory()) {
                if (filter.accept(f.getParentFile(), f.getName())) {
                    f.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(f);
                    log.debug("outputting: " + f.getName());
                    copyInputStream(zis, fos);
                }
            }
        }
    }

    public static void compress(File directory, File zipfile) throws IOException, IllegalArgumentException {
        compress(directory, zipfile, false);
    }

    public static void compress(File directory, File zipfile, boolean remove) throws IOException, IllegalArgumentException {
        if (!directory.isDirectory()) throw new IllegalArgumentException("Not a directory:  " + directory);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
        for (File f : FileIO.recursiveLs(directory)) {
            if (!f.isDirectory()) {
                FileInputStream in = new FileInputStream(f);
                URI path = directory.getCanonicalFile().toURI().relativize(f.getCanonicalFile().toURI());
                ZipEntry entry = new ZipEntry(path.getPath());
                out.putNextEntry(entry);
                appendInputStream(in, out);
            }
        }
        out.close();
        if (remove) FileIO.recursiveDelete(directory);
    }

    private static void appendInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
    }

    private static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        appendInputStream(in, out);
        out.close();
    }
}
