package com.swgman.util;

import java.io.*;

/**
 * This class provides static utility methods for doing I/O.
 */
public abstract class IOUtils {

    public static final int EOF = -1;

    private IOUtils() {
    }

    public static void fill(InputStream in, OutputStream out) throws IOException {
        for (int b = in.read(); b != EOF; b = in.read()) out.write(b);
        out.flush();
    }

    public static long fillCount(InputStream in, OutputStream out) throws IOException {
        long count = 0;
        for (int b = in.read(); b != EOF; b = in.read()) {
            out.write(b);
            count++;
        }
        out.flush();
        return count;
    }

    public static byte[] toByteArray(InputStream in) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        fill(in, bytes);
        return bytes.toByteArray();
    }

    public static void repeatWrite(int b, int count, OutputStream out) throws IOException {
        for (; count > 0; count--) out.write(b);
    }

    /**
   * When reading from an InputStream it is sometimes necessary to read multiple times. 
   * In particular, this is true for BufferedInputStream.
   * This method tries to read the all of the requested number of bytes by calling the read method
   * multiple times until either of the following is true:
   * <ul>
   *   <li> The specified number of bytes have been read, or
   *   <li> The read method returns -1, indicating end-of-file, or 
   *   <li> The read method returns zero, indicating that there are no more bytes to read.
   * </ul>
   * @param in the stream to read from.
   * @param b destination buffer.
   * @param off offset at which to start storing bytes. 
   * @param len maximum number of bytes to read.
   * @return the number of bytes read, or -1 if the end of the stream has been reached.
   * @throws IOException if an I/O error occurs.
   */
    public static int readBlock(InputStream in, byte[] b, int off, int len) throws IOException {
        final int res1 = in.read(b, off, len);
        if (res1 <= 0 || res1 == len) return res1; else {
            final int res2 = readBlock(in, b, off + res1, len - res1);
            return res2 <= 0 ? res1 : res1 + res2;
        }
    }

    /**
   * This method simply performs the call 
   * <code>{@link IOUtils#readBlock(InputStream, byte[], int, int) readBlock(in, b, 0, b.length)}
   * </code> and returns the result.
   * @param in the stream to read from.
   * @param b the buffer into which the data is read.
   * @return the number of bytes read, or -1 if the end of the stream has been reached.
   * @throws IOException if an I/O error occurs.
   */
    public static int readBlock(InputStream in, byte[] b) throws IOException {
        return readBlock(in, b, 0, b.length);
    }

    /**
   * The skip method of InputStream is allowed to skip less than the specified number of bytes.
   * This does not necessarily indicate that end-of-file has been reached. This method performs
   * multiple calls to skip until either of the following is true:
   * <ul>
   *   <li> The specified number of bytes have been skipped, or
   *   <li> The skip method returns 0, indicating end-of-file. 
   * </ul>
   * @param in the stream to skip from.
   * @param n maximum number of bytes to skip.
   * @return the number of bytes actually skipped (this can only be less than <code>n</code> if
   * end-of-file has been reached).
   * @throws IOException if an I/O error occurs.
   */
    public static long skipBlock(InputStream in, long n) throws IOException {
        final long res1 = in.skip(n);
        if (res1 <= 0 || res1 == n) return res1; else {
            final long res2 = skipBlock(in, n - res1);
            return res2 <= 0 ? res1 : res1 + res2;
        }
    }

    public static boolean isBinaryStream(InputStream is, int readLimit) throws IOException {
        if (!is.markSupported()) return true;
        is.mark(readLimit);
        for (int b = is.read(); b != EOF; b = is.read()) {
            if (b < 9 || (b > 13 && b < 31) || b > 126) {
                is.reset();
                return true;
            }
        }
        is.reset();
        return false;
    }
}
