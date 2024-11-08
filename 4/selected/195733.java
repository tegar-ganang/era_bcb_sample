package org.omegat.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * Utility class for copying untranslatable files.
 *
 * @author Keith Godfrey
 * @author Kim Bruning
 * @author Maxym Mykhalchuk
 */
public class LFileCopy {

    private static int BUFSIZE = 1024;

    /** Copies one file. Creates directories on the path to dest if necessary. */
    public static void copy(String src, String dest) throws IOException {
        File ifp = new File(src);
        File ofp = new File(dest);
        copy(ifp, ofp);
    }

    /** Copies one file. Creates directories on the path to dest if necessary. */
    public static void copy(File src, File dest) throws IOException {
        if (!src.exists()) {
            throw new IOException(StaticUtils.format(OStrings.getString("LFC_ERROR_FILE_DOESNT_EXIST"), new Object[] { src.getAbsolutePath() }));
        }
        FileInputStream fis = new FileInputStream(src);
        dest.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(dest);
        byte[] b = new byte[BUFSIZE];
        int readBytes;
        while ((readBytes = fis.read(b)) > 0) fos.write(b, 0, readBytes);
        fis.close();
        fos.close();
    }

    /** Stores a file from input stream. Input stream is not closed. */
    public static void copy(InputStream src, File dest) throws IOException {
        dest.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(dest);
        byte[] b = new byte[BUFSIZE];
        int readBytes;
        while ((readBytes = src.read(b)) > 0) fos.write(b, 0, readBytes);
        fos.close();
    }

    /** Transfers all the input stream to the output stream. Input and output streams are not closed. */
    public static void copy(InputStream src, OutputStream dest) throws IOException {
        byte[] b = new byte[BUFSIZE];
        int readBytes;
        while ((readBytes = src.read(b)) > 0) dest.write(b, 0, readBytes);
    }

    /** Transfers all data from reader to writer. Reader and writer are not closed. */
    public static void copy(Reader src, Writer dest) throws IOException {
        char[] b = new char[BUFSIZE];
        int readChars;
        while ((readChars = src.read(b)) > 0) dest.write(b, 0, readChars);
    }

    /** Loads contents of a file into output stream. Output stream is not closed. */
    public static void copy(File src, OutputStream dest) throws IOException {
        if (!src.exists()) {
            throw new IOException(StaticUtils.format(OStrings.getString("LFC_ERROR_FILE_DOESNT_EXIST"), new Object[] { src.getAbsolutePath() }));
        }
        FileInputStream fis = new FileInputStream(src);
        byte[] b = new byte[BUFSIZE];
        int readBytes;
        while ((readBytes = fis.read(b)) > 0) dest.write(b, 0, readBytes);
        fis.close();
    }
}
