package adv.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * UnZip -- print or unzip a JAR or PKZIP file using java.util.zip. Command-line
 * version: extracts files.
 *
 * @author Ian Darwin, Ian@DarwinSys.com $Id: UnZip.java,v 1.7 2004/03/07
 *         17:40:35 ian Exp $
 */
public class UnZip {

    public interface ZipEntryCommand {

        public File preProcess(String name, File f);

        public void postUnzip(String name, File f);

        public void postSkip(String name, File f);
    }

    public static void unZip(File fileName, File dest) throws IOException {
        unZip(fileName, dest, false, null);
    }

    public static void unZip(File fileName, File dest, boolean force, ZipEntryCommand zec) throws IOException {
        new UnZip()._unZip(fileName, dest, force, zec);
    }

    /**
     * The ZipFile that is used to read an archive
     */
    protected ZipFile zippy;

    protected byte[] b = new byte[4096];

    protected SortedSet dirsMade;

    private void _unZip(File fileName, File dest, boolean force, ZipEntryCommand zec) throws IOException {
        dirsMade = new TreeSet();
        zippy = new ZipFile(fileName);
        Enumeration all = zippy.entries();
        while (all.hasMoreElements()) {
            unzipEntry((ZipEntry) all.nextElement(), dest, force, zec);
        }
        zippy.close();
    }

    protected void unzipEntry(ZipEntry e, File dest, boolean force, ZipEntryCommand zec) throws IOException {
        String zipName = e.getName();
        if (zipName.startsWith("/")) {
            zipName = zipName.substring(1);
        }
        if (zipName.endsWith("/")) {
            return;
        }
        int ix = zipName.lastIndexOf('/');
        if (ix > 0) {
            String dirName = zipName.substring(0, ix);
            if (!dirsMade.contains(dirName)) {
                File d = new File(dest, dirName);
                if (!(d.exists() && d.isDirectory())) {
                    if (!d.mkdirs()) {
                        throw new IOException("[UnZip.unzipEntry] Warning: unable to mkdir " + dirName);
                    }
                    dirsMade.add(dirName);
                }
            }
        }
        File fout = new File(dest, zipName);
        if (zec != null) {
            File fzec = zec.preProcess(zipName, fout);
            if (fzec != null) {
                fout = fzec;
            }
        }
        if (force || !fout.exists()) {
            FileOutputStream os = new FileOutputStream(fout);
            InputStream is = zippy.getInputStream(e);
            int n = 0;
            while ((n = is.read(b)) > 0) os.write(b, 0, n);
            is.close();
            os.close();
            zec.postUnzip(zipName, fout);
        } else {
            zec.postSkip(zipName, fout);
        }
    }
}
