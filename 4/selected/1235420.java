package org.proclos.etlcore.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author Christian Schwarzinger. Mail: christian.schwarzinger@proclos.com
 *
 */
public class FileUtil {

    private static final Log log = LogFactory.getLog(FileUtil.class);

    public static boolean isRelativ(String filename) {
        if (filename != null) {
            File f = new File(filename);
            return !f.isAbsolute();
        }
        return true;
    }

    public static boolean isAbsolute(String filename) {
        return !isRelativ(filename);
    }

    public static String getDirectory(URL url) {
        if (url == null) return null;
        return url.getPath().substring(0, url.getPath().lastIndexOf("/") + 1);
    }

    public static String getFile(URL url) {
        if (url == null) return null;
        return url.getPath().substring(url.getPath().lastIndexOf("/") + 1);
    }

    public static String getFilePrefix(URL url) {
        if (url == null) return null;
        int pos = url.getPath().lastIndexOf(".");
        if (pos == -1) return url.getPath().substring(url.getPath().lastIndexOf("/") + 1);
        return url.getPath().substring(url.getPath().lastIndexOf("/") + 1, pos);
    }

    public static URL stringToUrl(String locator) {
        URL url = null;
        try {
            url = new URL(locator);
        } catch (Exception e) {
            log.error("Failed to build url from String: " + locator + ": " + e.getMessage());
            log.debug(e);
        }
        return url;
    }

    public static String readStreamAsString(InputStream in) throws java.io.IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF8"));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }
}
