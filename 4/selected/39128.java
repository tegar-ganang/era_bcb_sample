package org.yes.cart.utils.impl;

import org.apache.commons.io.IOUtils;
import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * by Piotr Gabryanczyk on May 7, 2008
 * http://piotrga.wordpress.com/
 */
public class ZipUtils {

    /**
     * Unzip archive to given folder.
     *
     * @param archive   given archive
     * @param outputDir given folder
     * @throws IOException in case of error
     */
    public static void unzipArchive(final String archive, final String outputDir) throws IOException {
        unzipArchive(new File(archive), new File(outputDir));
    }

    /**
     * Unzip archive to given folder.
     * @param archive given archive
     * @param outputDir  given folder
     * @throws IOException in case of error
     */
    public static void unzipArchive(final File archive, final File outputDir) throws IOException {
        ZipFile zipfile = new ZipFile(archive);
        for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            unzipEntry(zipfile, entry, outputDir);
        }
        zipfile.close();
    }

    private static void unzipEntry(final ZipFile zipfile, final ZipEntry entry, final File outputDir) throws IOException {
        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getName()));
            return;
        }
        File outputFile = new File(outputDir, entry.getName());
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }
        BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
        try {
            IOUtils.copy(inputStream, outputStream);
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }

    private static void createDir(final File dir) {
        if (!dir.mkdirs()) throw new RuntimeException("Can not create dir " + dir);
    }
}
