package com.wd.abom.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    /**
	 * Simple utilities to return the stack trace of an exception as a String.
	 * 
	 * @author http://www.javapractices.com/topic/TopicAction.do?Id=78
	 * @param Throwable
	 *            exception
	 * @return String representation of exception
	 */
    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }

    /**
	 * get the absolut path to a file in the jar
	 * 
	 * @param resource
	 *            path to file in jar
	 * @return absolute path of resource in filesystem
	 */
    public static String getAbsoluteResource(String resource) {
        URL relativePath = Utils.class.getResource(resource);
        if (relativePath == null) {
            return "";
        }
        String absolutePath = relativePath.getPath();
        absolutePath = absolutePath.substring(1);
        absolutePath = absolutePath.replaceAll("/", "\\\\");
        absolutePath = absolutePath.replaceAll("%20", " ");
        absolutePath = absolutePath.replaceAll("%c3%bc", "�");
        absolutePath = absolutePath.replaceAll("%c3%9f", "�");
        return absolutePath;
    }

    /**
	 * Copy a file Code borrowed by Real's HowTo
	 * 
	 * @author http://www.rgagnon.com/javadetails/java-0064.html
	 * @param in
	 * @param out
	 * @throws IOException
	 */
    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            int maxCount = 67076096;
            long size = inChannel.size();
            long position = 0;
            while (position < size) {
                position += inChannel.transferTo(position, maxCount, outChannel);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    public static boolean move(String sourceFile, String destFile) {
        logger.info("Moving file " + sourceFile + " to " + destFile);
        File sf = new File(sourceFile);
        File df = new File(destFile);
        if (sf.renameTo(df)) {
            return true;
        }
        return false;
    }

    /**
	 * delete a file or directory and all its childs
	 * 
	 * @param dir
	 * @return
	 */
    public static boolean del(File dir) {
        if (dir.isDirectory()) {
            String[] childs = dir.list();
            for (String c : childs) {
                File aktFile = new File(dir.getPath(), c);
                del(aktFile);
            }
            return dir.delete();
        } else {
            return dir.delete();
        }
    }

    /**
	 * returns the name of a file without path and postfix
	 * @param filename
	 * @return
	 */
    public static String getFilename(final String filename) {
        String file = "";
        int start = filename.lastIndexOf(System.getProperty("file.separator"));
        int end = filename.lastIndexOf(".");
        if (start < 0 || end < 0 || start >= end) {
            file = filename;
        } else {
            file = filename.substring(start, end);
        }
        return file;
    }
}
