package de.suse.swamp.util;

import java.io.*;
import java.util.zip.*;

/**
 * @author tschmidt
 * Helper utils for handling file operations.
 */
public class FileUtils {

    public static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger("de.suse.swamp.util.FileUtils");

    private FileUtils() {
    }

    public static void copyFile(File in, File out) throws Exception {
        org.apache.commons.io.FileUtils.copyFile(in, out);
    }

    public static void uncompress(File inputFile, String targetPath) throws Exception {
        uncompress(new FileInputStream(inputFile), targetPath);
    }

    /**
     * Decompress the provided Stream to "targetPath"
     */
    public static void uncompress(InputStream inputFile, String targetPath) throws Exception {
        ZipInputStream zin = new ZipInputStream(new BufferedInputStream(inputFile));
        BufferedOutputStream dest = null;
        ZipEntry entry;
        while ((entry = zin.getNextEntry()) != null) {
            int count;
            byte data[] = new byte[2048];
            if (entry.isDirectory()) {
                org.apache.commons.io.FileUtils.forceMkdir(new File(targetPath + "/" + entry.getName()));
            } else {
                FileOutputStream fos = new FileOutputStream(targetPath + "/" + entry.getName());
                dest = new BufferedOutputStream(fos, 2048);
                while ((count = zin.read(data, 0, 2048)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
        }
    }

    /**
     * Return the text content of a file
     */
    public static String getText(File file) throws Exception {
        if (file != null && file.exists() && file.canRead()) {
            byte[] b = new byte[(int) file.length()];
            log.debug("Getting text from file: " + file);
            FileInputStream filestream = null;
            filestream = new FileInputStream(file);
            filestream.read(b);
            filestream.close();
            return new String(b);
        } else {
            throw new Exception("Could not read from file: " + file.getAbsolutePath());
        }
    }
}
