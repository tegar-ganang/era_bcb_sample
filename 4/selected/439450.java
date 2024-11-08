package org.afk.util;

import java.io.*;
import java.nio.channels.*;

/**
 * Utility Class providing simple static methods, for generic usage
 * 
 * @author axel
 *  
 */
public class AfkLib {

    /**
	 * Transforms a Throwable to a String, doing mainly the same alorythm as
	 * Throwable.printStackTrace()
	 * 
	 * @param t
	 *            The throwable to be stringed
	 * @return The Stacktrace as String
	 */
    public static String stackTraceToString(Throwable t) {
        synchronized (t) {
            StringBuffer buffy = new StringBuffer(t.toString()).append("\n");
            StackTraceElement[] trace = t.getStackTrace();
            for (int i = 0; i < trace.length; i++) buffy.append("\t@ ").append(trace[i]).append("\n");
            Throwable ourCause = t.getCause();
            if (ourCause != null) {
                buffy.append(("Caused by: \n")).append(stackTraceToString(ourCause));
            }
            return buffy.toString();
        }
    }

    /**
	 * Copies The content of a file into another
	 * @param source the source files
	 * @param target the target file (Folders will not be created)
	 * @return the number of copied bytes
	 * @throws IOException if not successfull
	 */
    public static long copyFile(File source, File target) throws IOException {
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileInputStream = new FileInputStream(source);
            fileOutputStream = new FileOutputStream(target);
            FileChannel in = fileInputStream.getChannel();
            FileChannel out = fileOutputStream.getChannel();
            return out.transferFrom(in, 0, source.length());
        } finally {
            if (fileInputStream != null) fileInputStream.close();
            if (fileOutputStream != null) fileOutputStream.close();
        }
    }
}
