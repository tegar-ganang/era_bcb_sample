package r2q2.util.streams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class to copy stream-based sources.
 * 
 * @author Gavin Willingham
 */
public class StreamCopier {

    private static int bufferSize = 1024;

    public static int getBufferSize() {
        return bufferSize;
    }

    public static void setBufferSize(int _in) {
        bufferSize = _in;
    }

    /**
     * Directly copies from an InputStream to an OutputStream
     * @param _in The source stream
     * @param _out The target stream
     * @throws IOException If an error occurs during copy
     */
    public static void copy(InputStream _in, OutputStream _out) throws IOException {
        byte[] buf = new byte[1024];
        int len = 0;
        while ((len = _in.read(buf)) > 0) _out.write(buf, 0, len);
    }

    /**
     * Copies from an InputStream to a Byte array
     * @param _in The source stream
     * @throws IOException If an error occurs during copy
     */
    public static byte[] copyToByteArray(InputStream _in) throws IOException {
        byte[] buf = new byte[1024];
        int len = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((len = _in.read(buf)) > 0) out.write(buf, 0, len);
        return out.toByteArray();
    }
}
