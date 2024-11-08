package org.marre.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Contains various utiity functions related to io operations.
 * 
 * @author Markus
 * @version $Id: IOUtil.java 340 2005-04-07 19:58:36Z c95men $
 */
public final class IOUtil {

    /**
     * The default buffer size to use for the copy method.
     */
    private static final int DEFAULT_COPY_SIZE = 1024 * 8;

    /**
     * This class isn't intended to be instantiated.
     */
    private IOUtil() {
    }

    /**
     * Copy data from in to out using the workbuff as temporary storage.
     * 
     * @param in stream to read from
     * @param out stream to write to
     * @param workbuff buffer to use as temporary storage while copying
     * @return number of bytes copied
     * @throws IOException if an I/O error occurs
     */
    public static int copy(InputStream in, OutputStream out, byte[] workbuff) throws IOException {
        int bytescopied = 0;
        int bytesread = 0;
        while ((bytesread = in.read(workbuff)) != -1) {
            out.write(workbuff, 0, bytesread);
            bytescopied += bytesread;
        }
        return bytescopied;
    }

    /**
     * Copy data from in to out using a temporary buffer of workbuffsize bytes.
     * 
     * @param in stream to read from
     * @param out stream to write to
     * @param workbuffsize how large the work buffer should be
     * @return number of bytes copied
     * @throws IOException if an I/O error occurs
     */
    public static int copy(InputStream in, OutputStream out, int workbuffsize) throws IOException {
        return IOUtil.copy(in, out, new byte[workbuffsize]);
    }

    /**
     * Copy data from in to out using a default temporary buffer.
     * 
     * @param in stream to read from
     * @param out stream to write to
     * @return number of bytes copied
     * @throws IOException if an I/O error occurs
     */
    public static int copy(InputStream in, OutputStream out) throws IOException {
        return IOUtil.copy(in, out, new byte[DEFAULT_COPY_SIZE]);
    }
}
