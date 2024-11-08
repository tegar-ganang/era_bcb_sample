package uk.ac.ebi.intact.commons.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Various IntAct related utilities. If a pice of code is used more
 * than once, but does not really belong to a specific class, it
 * should become part of this class.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk) et at.
 * @version $Id: CompressionUtils.java 12148 2008-09-29 14:08:26Z prem_intact $
 */
public class CompressionUtils {

    private CompressionUtils() {
    }

    /**
     * Compresses a file using GZIP
     * @param sourceFile the file to compress
     * @param destFile the zipped file
     * @param deleteOriginalFile if true, the original file is deleted and only the gzipped file remains
     * @throws java.io.IOException thrown if there is a problem finding or writing the files
     */
    public static void gzip(File sourceFile, File destFile, boolean deleteOriginalFile) throws IOException {
        GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(destFile));
        FileInputStream in = new FileInputStream(sourceFile);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.finish();
        out.close();
        if (deleteOriginalFile) {
            sourceFile.delete();
        }
    }

    public static void zip(File[] sourceFiles, File destFile, boolean deleteOriginalFiles) throws IOException {
        zip(sourceFiles, destFile, deleteOriginalFiles, false);
    }

    /**
     * Compresses a file using GZIP
     *
     * @param sourceFiles         the files to include in the zip
     * @param destFile the zipped file
     * @param deleteOriginalFiles if true, the original file is deleted and only the gzipped file remains
     * @param includeFullPathName if true, then zip file is given full path name, if false, only the file name
     * @throws java.io.IOException thrown if there is a problem finding or writing the files
     */
    public static void zip(File[] sourceFiles, File destFile, boolean deleteOriginalFiles, boolean includeFullPathName) throws IOException {
        byte[] buf = new byte[1024];
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(destFile));
        for (File sourceFile : sourceFiles) {
            FileInputStream in = new FileInputStream(sourceFile);
            if (includeFullPathName) {
                out.putNextEntry(new ZipEntry(sourceFile.toString()));
            } else {
                out.putNextEntry(new ZipEntry(sourceFile.getName()));
            }
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.closeEntry();
            in.close();
        }
        out.close();
        if (deleteOriginalFiles) {
            for (File sourceFile : sourceFiles) {
                sourceFile.delete();
            }
        }
    }

    /**
     * Uncompress gzipped files
     * @param gzippedFile The file to uncompress
     * @param destinationFile The resulting file
     * @throws java.io.IOException thrown if there is a problem finding or writing the files
     */
    public static void gunzip(File gzippedFile, File destinationFile) throws IOException {
        int buffer = 2048;
        FileInputStream in = new FileInputStream(gzippedFile);
        GZIPInputStream zipin = new GZIPInputStream(in);
        byte[] data = new byte[buffer];
        FileOutputStream out = new FileOutputStream(destinationFile);
        int length;
        while ((length = zipin.read(data, 0, buffer)) != -1) out.write(data, 0, length);
        out.close();
        zipin.close();
    }

    /**
     * Uncompresses zipped files
     * @param zippedFile The file to uncompress
     * @return list of unzipped files
     * @throws java.io.IOException thrown if there is a problem finding or writing the files
     */
    public static List<File> unzip(File zippedFile) throws IOException {
        return unzip(zippedFile, null);
    }

    /**
     * Uncompresses zipped files
     * @param zippedFile The file to uncompress
     * @param destinationDir Where to put the files
     * @return  list of unzipped files
     * @throws java.io.IOException thrown if there is a problem finding or writing the files
     */
    public static List<File> unzip(File zippedFile, File destinationDir) throws IOException {
        int buffer = 2048;
        List<File> unzippedFiles = new ArrayList<File>();
        BufferedOutputStream dest;
        BufferedInputStream is;
        ZipEntry entry;
        ZipFile zipfile = new ZipFile(zippedFile);
        Enumeration e = zipfile.entries();
        while (e.hasMoreElements()) {
            entry = (ZipEntry) e.nextElement();
            is = new BufferedInputStream(zipfile.getInputStream(entry));
            int count;
            byte data[] = new byte[buffer];
            File destFile;
            if (destinationDir != null) {
                destFile = new File(destinationDir, entry.getName());
            } else {
                destFile = new File(entry.getName());
            }
            FileOutputStream fos = new FileOutputStream(destFile);
            dest = new BufferedOutputStream(fos, buffer);
            while ((count = is.read(data, 0, buffer)) != -1) {
                dest.write(data, 0, count);
            }
            dest.flush();
            dest.close();
            is.close();
            unzippedFiles.add(destFile);
        }
        return unzippedFiles;
    }
}
