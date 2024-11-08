package ru.adv.util;

import java.io.*;

/**
 * Usefull algorithms for Streams
 */
public class Stream {

    protected static final int BUFF_SIZE_READ_TO_STRING = 1024;

    private static final int BUFFER_SIZE = 8192;

    /**
     * all metoth is static
     */
    private Stream() {
    }

    /**
     * Skiping data in stream <b>in</b> to <b>key</b>.
     * Matching key reading from <b>in</b> (not stay in InputStream)
     *
     * @return true if key was found, else skip data to the end and return false
     * @throws java.io.IOException
     */
    public static boolean skipTo(BufferedInputStream in, byte[] key) throws java.io.IOException {
        return readTo(in, null, key);
    }

    /**
     * Read all and close <code>InputStream</code> to <code>String</code>
     *
     * @param in
     * @param encoding if is's null then using system encoding
     * @return String
     */
    public static String readToString(InputStream in, String encoding) throws java.io.IOException {
        StringBuffer result = new StringBuffer();
        byte buf[] = new byte[BUFF_SIZE_READ_TO_STRING];
        int i = 0;
        if (encoding != null) {
            while ((i = in.read(buf, 0, BUFF_SIZE_READ_TO_STRING)) > 0) {
                result.append(new String(buf, 0, i, encoding));
            }
        } else {
            while ((i = in.read(buf, 0, BUFF_SIZE_READ_TO_STRING)) > 0) {
                result.append(new String(buf, 0, i));
            }
        }
        in.close();
        return result.toString();
    }

    /**
     * Reading data from <code>in</code> to <code>out</code>.
     *
     * @param in
     * @param out
     * @param codeSet
     * @throws java.io.IOException
     */
    public static void readTo(InputStream in, OutputStream out, String codeSet) throws IOException {
        InputStreamReader in_reader = new InputStreamReader(in, codeSet);
        OutputStreamWriter out_writer = new OutputStreamWriter(out, codeSet);
        readTo(in_reader, out_writer);
    }

    public static void readTo(Reader reader, Writer writer) throws IOException {
        char cbuf[] = new char[1024];
        int i = 0;
        while ((i = reader.read(cbuf, 0, 1024)) > 0) {
            writer.write(cbuf, 0, i);
        }
        writer.flush();
    }

    /**
     * Reading data from <code>in</code> to <code>out</code>.
     *
     * @param in
     * @param out
     * @return количество прочитанных-записанных байт
     * @throws java.io.IOException
     */
    public static long readTo(InputStream in, OutputStream out) throws IOException {
        return readTo(in, out, (ProgressCallback) null);
    }

    private static final long CALLBACK_STEP = 1024 * 1024;

    public static long readTo(InputStream in, OutputStream out, ProgressCallback callback) throws IOException {
        long countBytes = 0;
        long callbackStepBytes = 0;
        byte[] cbuf = new byte[BUFFER_SIZE];
        int i = 0;
        while ((i = in.read(cbuf, 0, BUFFER_SIZE)) > 0) {
            out.write(cbuf, 0, i);
            countBytes += i;
            if (callback != null) {
                callbackStepBytes += i;
                if (callbackStepBytes > CALLBACK_STEP) {
                    callback.setProgress(countBytes);
                    callbackStepBytes = 0;
                }
            }
        }
        out.flush();
        if (callback != null) {
            callback.setProgress(countBytes);
        }
        return countBytes;
    }

    /**
     * Reading data from <code>in</code> to <code>out</code>.
     * Читает только определенное количество байт
     *
     * @param in
     * @param out
     * @param countBytes количество читаемых-записываемых байт, если значение меньше 0, то читается поток до конца
     * @throws java.io.IOException при ошибках чтения записи или если не удается перезаписать
     *                             указанное количество байт.
     */
    public static void readTo(InputStream in, OutputStream out, long countBytes) throws java.io.IOException {
        readTo(in, out, countBytes, null);
    }

    public static void readTo(final InputStream in, final OutputStream out, final long countBytes, final ProgressCallback callback) throws java.io.IOException {
        long gotBytes = 0;
        long callbackStepBytes = 0;
        byte[] cbuf = new byte[BUFFER_SIZE];
        int i;
        int toReadBytes = (int) ((BUFFER_SIZE > (countBytes - gotBytes)) ? countBytes - gotBytes : BUFFER_SIZE);
        while ((i = in.read(cbuf, 0, toReadBytes)) > 0) {
            out.write(cbuf, 0, i);
            gotBytes += i;
            toReadBytes = (int) ((BUFFER_SIZE > (countBytes - gotBytes)) ? countBytes - gotBytes : BUFFER_SIZE);
            if (callback != null) {
                callbackStepBytes += i;
                if (callbackStepBytes > CALLBACK_STEP) {
                    callback.setProgress(gotBytes);
                    callbackStepBytes = 0;
                }
            }
        }
        if (gotBytes != countBytes) {
            throw new IOException("Wrong number of read bytes: " + countBytes + "!=" + gotBytes);
        }
        out.flush();
        if (callback != null) {
            callback.setProgress(gotBytes);
        }
    }

    /**
     * Reading data from <b>in</b> to <b>out</b> while not match <b>key</b>.
     * Matching key reading from in (not stay present) and not write to <b>out</b>.
     *
     * @param in Must be is markSupported
     * @return true key is found
     * @throws java.io.IOException
     */
    public static boolean readTo(BufferedInputStream in, OutputStream out, byte[] key) throws java.io.IOException {
        if (key.length == 0) return false;
        byte[] buff = new byte[key.length];
        int b;
        while ((b = in.read()) != -1) {
            if (b == key[0]) {
                in.mark(key.length);
                buff[0] = (byte) b;
                int got_bytes = nonBlockRead(in, buff, 1, buff.length - 1);
                if (got_bytes == buff.length - 1) {
                    if (ru.adv.util.Array.equals(buff, key)) {
                        return true;
                    } else {
                        in.reset();
                        if (out != null) out.write((byte) b);
                    }
                } else {
                    if (out != null) out.write(buff, 0, got_bytes + 1);
                    return false;
                }
            } else {
                if (out != null) out.write((byte) b);
            }
        }
        return false;
    }

    /**
     * Reading data from <b>in</b> and compare same sequence of bytes with <b>key</b>.
     *
     * @return true on success
     * @throws java.io.IOException
     */
    public static boolean isNext(BufferedInputStream in, byte[] key) throws java.io.IOException {
        return isNext(in, key, false);
    }

    /**
     * Reading data from <b>in</b> and compare with <b>key</b>.
     *
     * @param in           Must be is markSupported
     * @param isIgnoreCase compare like String
     * @return true on success
     * @throws java.io.IOException
     */
    public static boolean isNext(BufferedInputStream in, byte[] key, boolean isIgnoreCase) throws java.io.IOException {
        boolean result = false;
        if (key.length != 0) {
            in.mark(key.length);
            byte[] buff = new byte[key.length];
            int got_bytes = nonBlockRead(in, buff);
            if (got_bytes == buff.length) {
                if (isIgnoreCase) {
                    result = new String(buff).equalsIgnoreCase(new String(key));
                } else {
                    result = ru.adv.util.Array.equals(buff, key);
                }
            }
            in.reset();
        }
        return result;
    }

    /**
     * Execute non blocking read to buff
     * This usefull for read to array in one step
     *
     * @return the number of bytes read, or -1 if the end of the stream has been reached.
     * @throws java.io.IOException
     */
    public static int nonBlockRead(BufferedInputStream in, byte[] buff) throws java.io.IOException {
        return nonBlockRead(in, buff, 0, buff.length);
    }

    /**
     * Execute non blocking read to buff
     * This usefull for read to array in one step
     *
     * @param off offset at which to start storing bytes.
     * @param len maximum number of bytes to read.
     * @return the number of bytes read, or -1 if the end of the stream has been reached.
     * @throws java.io.IOException
     */
    public static int nonBlockRead(BufferedInputStream in, byte[] buff, int off, int len) throws java.io.IOException {
        int got_bytes = 0;
        int to_read = len;
        int got = 0;
        while (to_read > 0 && (got = in.read(buff, off + got_bytes, to_read)) != -1) {
            got_bytes += got;
            to_read -= got;
        }
        return (got_bytes == 0 && got == -1) ? -1 : got_bytes;
    }
}
