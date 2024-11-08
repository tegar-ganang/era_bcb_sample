package org.columba.core.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Contains utility methods for handling streams.
 */
public class StreamUtils {

    private static final int BUFFERSIZE = 8000;

    /**
	 * Copies length bytes from an InputStream to an OutputStream.
	 * 
	 * @param in
	 *            InputStream from wihch the bytes are to copied.
	 * @param out
	 *            OutputStream in which the bytes are copied.
	 * @param length
	 *            The number of bytes to copy
	 * @return Number of bytes which are copied.
	 * @throws IOException
	 *             If the streams are unavailable.
	 */
    public static int streamCopy(InputStream in, OutputStream out, int length) throws IOException {
        byte[] buffer = new byte[BUFFERSIZE];
        int read;
        int copied = 0;
        while ((read = in.read(buffer, 0, Math.min(BUFFERSIZE, length - copied))) > 0) {
            out.write(buffer, 0, read);
            copied += read;
        }
        return copied;
    }

    /**
	 * Copies all bytes from an InputStream to an OutputStream. The buffer size
	 * is set to 8000 bytes.
	 * 
	 * @param _isInput
	 *            InputStream from wihch the bytes are to copied.
	 * @param _osOutput
	 *            OutputStream in which the bytes are copied.
	 * @return Number of bytes which are copied.
	 * @throws IOException
	 *             If the Streams are unavailable.
	 */
    public static long streamCopy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFERSIZE];
        int read;
        long copied = 0;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
            copied += read;
        }
        return copied;
    }

    /**
	 * Reads a InputStream of chars into a StringBuffer.
	 * 
	 * @param in
	 *            the InputStream to read from
	 * @return the interpreted InputStream
	 * @throws IOException
	 */
    public static StringBuffer readCharacterStream(InputStream in) throws IOException {
        StringBuffer result = new StringBuffer(in.available());
        int read = in.read();
        while (read > 0) {
            result.append((char) read);
            read = in.read();
        }
        in.close();
        return result;
    }

    public static byte[] readInByteArray(InputStream in) throws IOException {
        byte[] result = new byte[in.available()];
        in.read(result);
        in.close();
        return result;
    }

    /**
	 * Copies all bytes from the given InputStream into an internal
	 * ByteArrayOutputStream and returnes a new InputStream with all bytes from
	 * the ByteArrayOutputStream. The data are real copied so this method
	 * "clones" the given Inputstream and returns a new InputStream with same
	 * data.
	 * 
	 * @param from
	 *            InputStream from which all data are to copy
	 * @return a new InputStream with all data from the given InputStream
	 * @throws IOException
	 */
    public static InputStream streamClone(InputStream from) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        streamCopy(from, out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}
