package octlight.util;

import java.io.*;

/**
 * @author $Author: creator $
 * @version $Revision: 1.1.1.1 $
 */
public class StreamUtil {

    private StreamUtil() {
    }

    public static final void readAll(InputStream in, byte[] data, int offset, int length) throws IOException {
        while (length > 0) {
            int len = in.read(data, offset, length);
            if (len == -1) throw new IOException("End of stream reached!");
            offset += len;
            length -= len;
        }
    }

    public static final byte[] readAll(InputStream is) throws IOException {
        byte[] data = new byte[is.available()];
        int pos = 0;
        while (pos < data.length) pos += is.read(data, pos, data.length - pos);
        is.close();
        return data;
    }

    public static final byte[] writeObject(Object object) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream o2 = new ObjectOutputStream(out);
        o2.writeObject(object);
        o2.close();
        return ArrayUtil.part(out.toByteArray(), 0, out.size());
    }

    public static final Object readObject(byte[] data) throws IOException, ClassNotFoundException {
        return new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
    }

    public static void readAll(InputStream in, byte[] data) throws IOException {
        readAll(in, data, 0, data.length);
    }

    public static void write(InputStream in, OutputStream out, int length) throws IOException {
        byte[] data = new byte[10000];
        while (length > 0) {
            int read = in.read(data, 0, Math.min(data.length, length));
            out.write(data, 0, read);
            length -= read;
        }
    }
}
