package org.benetech.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Utilities for manipulating zip files.
 * @author Reuben Firmin
 */
public final class ZipUtils {

    private static int chunk = 4096;

    /**
     * Non constructor.
     */
    private ZipUtils() {
    }

    /**
     * Extract a zip file to a tmp dir, and return a map of
     * file names -> file references.
     * @param zipFile The zip file to extract
     * @param tmpDir Initialized writable temporary director
     * @return never null
     * @throws IOException if the zip file doesn't exist, or can't be parsed.
     */
    public static ZipContents extractZip(final File zipFile, final File tmpDir) throws IOException {
        final File tmpCopy = File.createTempFile(FileUtils.sanitizeFileName(zipFile.getName()), "copy");
        FileUtils.copyFile(zipFile, tmpCopy);
        final ZipFile zip = new ZipFile(tmpCopy, ZipFile.OPEN_READ);
        final Map<String, File> fileEntryMap = new LinkedHashMap<String, File>();
        final File tmpPathDir = new File(tmpDir.getAbsolutePath() + File.separator + FileUtils.sanitizeFileName(zipFile.getName()));
        tmpPathDir.mkdirs();
        final Enumeration<ZipEntry> enumerated = (Enumeration<ZipEntry>) zip.entries();
        while (enumerated.hasMoreElements()) {
            final ZipEntry zipEntry = enumerated.nextElement();
            final File out = FileUtils.copyFileFromZip(zip, zipEntry, tmpPathDir);
            fileEntryMap.put(out.getAbsolutePath(), out);
        }
        return new ZipContents(fileEntryMap, tmpPathDir);
    }

    /**
     * Create a zip file in the given destination, based upon the structure of a given directory.
     * @param fileName The output file name of the zip
     * @param dirToZip The directory to zip - complete tree of the output contents
     * @param destDir The destination directory that the zip should be placed in
     * @return a file reference to the new zip
     * @throws IOException if file access fails
     */
    public static File createZip(final String fileName, final File dirToZip, final File destDir) throws IOException {
        if (!destDir.exists() || !destDir.isDirectory()) {
            throw new IOException("The destination dir doesn't exist or isn't a directory");
        }
        if (!dirToZip.exists() || !dirToZip.isDirectory()) {
            throw new IOException("The directory to zip doesn't exist or isn't a directory");
        }
        final File outFile = new File(destDir.getAbsolutePath() + File.separator + fileName);
        outFile.getParentFile().mkdirs();
        final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile));
        try {
            zipDir(dirToZip, zos, "");
        } finally {
            zos.close();
        }
        return outFile;
    }

    /**
     * Zip a directory. This is used by the createZip method, above.
     * @param zipDir The directory to zip
     * @param zos The zip outputstream to add to
     * @param path The path that this method is working in
     * @throws IOException If file access fails
     */
    private static void zipDir(final File zipDir, final ZipOutputStream zos, final String path) throws IOException {
        final String[] dirList = zipDir.list();
        final byte[] readBuffer = new byte[chunk];
        int bytesIn = 0;
        for (int i = 0; i < dirList.length; i++) {
            final File file = new File(zipDir, dirList[i]);
            final String filePath = ((path != null && path.length() > 0) ? (path + File.separator) : "") + file.getName();
            if (file.isDirectory()) {
                zipDir(file, zos, filePath);
                continue;
            }
            final FileInputStream fis = new FileInputStream(file);
            try {
                final ZipEntry anEntry = new ZipEntry(filePath);
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                zos.flush();
            } finally {
                fis.close();
            }
        }
    }
}
