package com.sun.java.help.search;

import java.io.*;
import java.net.URLConnection;
import java.security.Permission;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

/**
 * This Factory assumes we are on JDK 1.2
 */
public final class RAFFileFactoryOn12 {

    /**
     * Creata a RAFFile from a URLConnection.  Try to use a temporary file
     * if possible.  This is a static public method
     *
     * @param url The URL with the data to the file
     * @return the RAFFile for this data
     * @exception IOException if there is problem reading from the file
     */
    public static RAFFile get(final URLConnection connection) throws IOException {
        RAFFile topBack = null;
        debug("get on " + connection);
        final Permission permission = connection.getPermission();
        int dictLength = connection.getContentLength();
        try {
            topBack = (RAFFile) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                RAFFile back = null;

                public Object run() throws IOException {
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        File tmpFile = File.createTempFile("dict_cache", null);
                        tmpFile.deleteOnExit();
                        if (tmpFile != null) {
                            in = connection.getInputStream();
                            out = new FileOutputStream(tmpFile);
                            int read = 0;
                            byte[] buf = new byte[BUF_SIZE];
                            while ((read = in.read(buf)) != -1) {
                                out.write(buf, 0, read);
                            }
                            back = new TemporaryRAFFile(tmpFile, permission);
                        } else {
                            back = new MemoryRAFFile(connection);
                        }
                    } finally {
                        if (in != null) {
                            in.close();
                        }
                        if (out != null) {
                            out.close();
                        }
                    }
                    return back;
                }
            });
        } catch (PrivilegedActionException pae) {
            topBack = new MemoryRAFFile(connection);
        } catch (SecurityException se) {
            topBack = new MemoryRAFFile(connection);
        }
        return topBack;
    }

    private static int BUF_SIZE = 2048;

    /**
     * Debug code
     */
    private static final boolean debug = false;

    private static void debug(String msg) {
        if (debug) {
            System.err.println("RAFFileFactoryOn12: " + msg);
        }
    }
}
