package de.fhg.igd.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Creates a ZIP archive from a directory.
 *
 * @author Volker Roth
 * @version "$Id: Zipper.java 1913 2007-08-08 02:41:53Z jpeters $"
 */
public class Zipper extends Object implements ScannerDelegate {

    /**
      * The size of the byte buffer used to copy a file to
      * the Zip stream.
      */
    public static final int BLOCK_SIZE = 1024;

    /**
      * The output stream to which the ZIP archive is written.
      */
    protected ZipOutputStream sink_;

    /**
      * The directory scanner being used.
      */
    protected DirectoryScanner scanner_;

    /**
      * Main method for debugging.
      *
      */
    public static void main(String[] argv) {
        Zipper zipper;
        if (argv.length != 2) {
            System.out.println("USAGE: <dir> <arch.zip> or <arch.zip> <dest>");
            System.exit(0);
        }
        try {
            if (argv[0].endsWith(".zip")) {
                FileInputStream fis;
                fis = new FileInputStream(argv[0]);
                Zipper.unzip(fis, new File(argv[1]));
                fis.close();
            } else {
                FileOutputStream fos;
                fos = new FileOutputStream(argv[1]);
                zipper = new Zipper(new File(argv[0]), fos);
                zipper.start();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
      * Creates a directory zipper. The directory zipper uses
      * a directory scanner to traverse the directory recursively.
      * Each file and directory gets added to the archive.
      *
      * @param directory The directory that is zipped.
      * @param out The stream to which the archive is written. If
      *   the archive is to be saved to disk then provide a
      *   FileOutputStream here.
      * @exception NullPointerException if the output stream or
      *   the directory is <code>null</code>.
      */
    public Zipper(File directory, OutputStream out) {
        scanner_ = new DirectoryScanner(directory, this);
        if (out == null) throw new NullPointerException("OutputStream required!");
        sink_ = new ZipOutputStream(out);
    }

    /**
      * Creates a directory zipper. The directory zipper uses
      * a directory scanner to traverse the directory recursively.
      * Each file and directory gets added to the archive.
      *
      * @param directory The directory that is zipped.
      * @param out The stream to which the archive is written. If
      *   the archive is to be saved to disk then provide a
      *   FileOutputStream here.
      * @param prefix The prefix prepended to the relative path name
      *   before creating the ZIP entries. Hence, the prefix can be
      *   used to &quot;relocate&quot; the files in the ZIP archive.
      * @exception NullPointerException if the output stream or
      *   the directory is <code>null</code>.
      */
    public Zipper(File directory, String prefix, OutputStream out) {
        scanner_ = new DirectoryScanner(directory, prefix, this);
        if (out == null) throw new NullPointerException("OutputStream required!");
        sink_ = new ZipOutputStream(out);
    }

    /**
      * Starts the zipping. If the directory was already zipped,
      * nothing happens, even if the previous call to this
      * method terminated with an error. A new Zipper must be
      * created in that case.
      */
    public void start() throws Exception {
        if (sink_ == null) return;
        scanner_.start();
    }

    /**
      * Creates a directory entry in the ZIP archive with the
      * name of the directory currently scanned. The name is
      * taken from the <code>path_</code> variable of the parent
      * class. This path is relative to the base directory with
      * the name of the base directory as the first path element.
      *
      * @param file The current directory whose entry needs
      *   to be created.
      */
    public void preScan(File dir, DirectoryScanner scanner) throws IOException {
        sink_.putNextEntry(new ZipEntry(scanner.path_.toString().replace(File.separatorChar, '/') + "/"));
        sink_.closeEntry();
    }

    /**
      * Writes a file to the Zip archive. The file's name is the
      * one taken from the <code>path_</code> variable of the
      * parent class. This path is relative to the base directory.
      *
      * @param file The file that is to be added to the archive.
      */
    public void inScan(File file, DirectoryScanner scanner) throws Exception {
        int i;
        FileInputStream in;
        byte[] buffer = new byte[BLOCK_SIZE];
        sink_.putNextEntry(new ZipEntry(scanner.path_.toString().replace(File.separatorChar, '/')));
        in = new FileInputStream(file);
        try {
            while ((i = in.read(buffer)) > 0) sink_.write(buffer, 0, i);
        } catch (Exception e) {
        }
        in.close();
        sink_.closeEntry();
    }

    /**
      * Called upon completition of the scanning. Closes
      * the Zip stream.
      *
      */
    public void endScan(DirectoryScanner scanner) throws IOException {
        sink_.close();
        sink_ = null;
    }

    /**
      * Called by the directory scanner befor the scanning
      * starts.
      *
      */
    public void startScan(DirectoryScanner scanner) throws Exception {
    }

    /**
      * Called by the directory scanner after the recursion
      * returned from the directory given as the argument.
      *
      * @param file The directory which was scanned.
      */
    public void postScan(File dir, DirectoryScanner scanner) throws Exception {
    }

    /**
      * Unzips the data from the given InputStream to the
      * given directory.
      *
      * @param in The InputStream from which the data is read.
      * @param dir The directory to which the deflated
      *   files are written.
      */
    public static void unzip(InputStream in, File dir) throws Exception {
        unzip(in, dir, 0);
    }

    /**
      * Unzips the data from the given InputStream to the
      * given directory. At most <code>max</code> bytes are
      * decompressed. If the archive contains more data then the
      * process is aborted and an exception is thrown. Otherwise
      * the actual number of bytes being decompressed is returned.
      *
      * @param in The InputStream from which the data is read.
      * @param dir The directory to which the deflated
      *   files are written.
      * @exception ZipException iff the archive contains more than
      *   <code>max</code> bytes.
      */
    public static long unzip(InputStream in, File dir, long max) throws Exception {
        int i;
        long sum;
        OutputStream out;
        ZipInputStream zip;
        ZipEntry entry;
        String name;
        File file, parent;
        byte[] buffer = new byte[BLOCK_SIZE];
        sum = 0;
        zip = new ZipInputStream(in);
        while ((entry = zip.getNextEntry()) != null) {
            name = entry.getName().replace('/', File.separatorChar);
            file = new File(dir, name);
            if (entry.isDirectory()) file.mkdirs(); else {
                parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                out = new BufferedOutputStream(new FileOutputStream(file));
                while ((i = zip.read(buffer)) > 0) {
                    out.write(buffer, 0, i);
                    sum += i;
                    if (max > 0 && sum > max) {
                        out.close();
                        zip.closeEntry();
                        zip.close();
                        throw new ZipException("Archive contains more than " + max + " bytes!");
                    }
                }
                out.close();
            }
            zip.closeEntry();
        }
        zip.close();
        return sum;
    }
}
