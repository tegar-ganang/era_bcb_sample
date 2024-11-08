package org.dbe.composer.wfengine.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class SdlFileUtil {

    /** Default buffer size. */
    public static final int DEFAULT_BUFFER = 1024 * 4;

    /** prevent instantiation */
    private SdlFileUtil() {
    }

    /**
     * Copy the source file to the destination file.  Neither file can
     * be a directory.
     * @param aSource The source file.
     * @param aDestination The destination file.
     * @throws IOException
     */
    public static void copyFile(File aSource, File aDestination) throws IOException {
        if (!aSource.isFile()) {
            throw new IOException(aSource + " is not a valid file. ");
        }
        FileInputStream input = new FileInputStream(aSource);
        try {
            FileOutputStream output = new FileOutputStream(aDestination);
            try {
                SdlFileUtil.copy(input, output);
            } finally {
                SdlCloser.close(output);
            }
        } finally {
            SdlCloser.close(input);
        }
        if (aSource.length() != aDestination.length()) {
            throw new IOException("Failed to copy " + aSource + " to " + aDestination);
        }
    }

    /**
     * Read the contents of the input stream and write them to the output stream.
     * Uses the default buffer size.  The method returns the number of bytes that
     * copied.
     * @param aIn
     * @param aOut
     * @throws IOException
     */
    public static long copy(InputStream aIn, OutputStream aOut) throws IOException {
        return copy(aIn, aOut, DEFAULT_BUFFER);
    }

    /**
     * Read the contents of the input stream and write them to the output stream.
     * Uses the given buffer size.  Returns the number of bytes copied.
     * @param aIn
     * @param aOut
     * @param aBufferSize The buffer size to use for reading.
     * @throws IOException
     */
    public static long copy(InputStream aIn, OutputStream aOut, int aBufferSize) throws IOException {
        byte[] buffer = new byte[aBufferSize];
        int read = 0;
        long totalBytes = 0;
        while (-1 != (read = aIn.read(buffer))) {
            aOut.write(buffer, 0, read);
            totalBytes += read;
        }
        return totalBytes;
    }

    /**
     * Recursivelt deletes the given directory and all of
     * its nested contents. Use carefully!
     * @param aDirectory
     */
    public static void recursivelyDelete(File aDirectory) {
        if (!aDirectory.isDirectory()) {
            return;
        }
        File[] files = aDirectory.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    files[i].delete();
                } else {
                    recursivelyDelete(files[i]);
                }
            }
        }
        aDirectory.delete();
    }

    /**
     * Copies a given JAR entry to the given output stream.
     *
     * @param aIn The JAR input stream.
     * @param aOut The output stream to write to.
     * @throws IOException
     */
    protected static void copyEntry(JarInputStream aIn, OutputStream aOut) throws IOException {
        try {
            SdlFileUtil.copy(aIn, aOut);
        } finally {
            try {
                aIn.closeEntry();
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
    }

    /**
     * Unpacks the contents of a given JAR file to the given target directory.
     *
     * @param aFile The source JAR file.
     * @param aTargetDir The target directory to unpack into.
     * @throws IOException
     */
    public static void unpack(File aFile, File aTargetDir) throws IOException {
        File rootDir = aTargetDir;
        rootDir.mkdirs();
        JarInputStream in = null;
        try {
            in = new JarInputStream(new FileInputStream(aFile));
            JarEntry entry = in.getNextJarEntry();
            while (entry != null) {
                FileOutputStream out = null;
                try {
                    if (!entry.isDirectory()) {
                        File newFile = new File(rootDir, entry.getName());
                        newFile.getParentFile().mkdirs();
                        out = new FileOutputStream(newFile);
                        copyEntry(in, out);
                    }
                    entry = in.getNextJarEntry();
                } catch (IOException io) {
                    io.printStackTrace();
                    throw io;
                } finally {
                    SdlCloser.close(out);
                }
            }
        } finally {
            SdlCloser.close(in);
        }
    }

    /**
     * Returns the filename without the extension.  Will return the
     * string passed in if the filename does not contain a '.' character.
     * @param aFileName
     */
    public static String stripOffExtenstion(String aFileName) {
        if (aFileName.indexOf('.') != -1) {
            return aFileName.substring(0, aFileName.lastIndexOf('.'));
        } else {
            return aFileName;
        }
    }
}
