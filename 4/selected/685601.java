package net.sourceforge.pebble.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.*;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.Properties;

/**
 * A collection of utility methods for manipulating files.
 *
 * @author    Simon Brown
 */
public final class FileUtils {

    /** the logger used by this class */
    private static final Log log = LogFactory.getLog(FileUtils.class);

    /** the local content type map */
    private static Properties localFileNameMap;

    static {
        try {
            localFileNameMap = new Properties();
            InputStream in = FileUtils.class.getClassLoader().getResourceAsStream("content-types.properties");
            if (in != null) {
                localFileNameMap.load(in);
                in.close();
            }
        } catch (IOException ioe) {
            log.error("Could not load content types.", ioe);
        }
    }

    /**
   * Determines whether a given file is underneath a given root.
   *
   * @param root    the root directory
   * @param file    the file to test
   * @return    true if the file is underneath the root,
   *            false otherwise or if this can not be determined because
   *            of security constraints in place
   */
    public static boolean underneathRoot(File root, File file) {
        try {
            root = root.getCanonicalFile();
            file = file.getCanonicalFile();
            while (file != null) {
                if (file.equals(root)) {
                    return true;
                } else {
                    file = file.getParentFile();
                }
            }
        } catch (IOException ioe) {
            return false;
        }
        return false;
    }

    /**
   * Deletes a file, including all files and sub-directories if the
   * specified file is a directory.
   *
   * @param directory   a File instance representing the directory to delete
   */
    public static void deleteFile(File directory) {
        File files[] = directory.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteFile(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        directory.delete();
    }

    /**
   * Copies a file.
   *
   * @param source        the source File
   * @param destination   the destination File
   */
    public static void copyFile(File source, File destination) throws IOException {
        FileChannel srcChannel = new FileInputStream(source).getChannel();
        FileChannel dstChannel = new FileOutputStream(destination).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }

    /**
   * Gets the content type for the specified filename.
   *
   * @param name    the name of a file
   * @return  a MIME type, or application/octet-stream if one can't be found
   */
    public static String getContentType(String name) {
        String contentType;
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        contentType = fileNameMap.getContentTypeFor(name);
        if (contentType == null) {
            int index = name.lastIndexOf(".");
            if (index > -1) {
                contentType = localFileNameMap.getProperty(name.substring(index));
            }
        }
        return contentType;
    }
}
