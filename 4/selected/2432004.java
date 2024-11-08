package lrf.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ByteOutputStream extends OutputStream {

    private byte[] buffer = null;

    private int offset = 0;

    public ByteOutputStream() {
        buffer = new byte[1024];
    }

    @Override
    public void write(int b) throws IOException {
        if (offset == buffer.length) {
            byte b2[] = new byte[2 * buffer.length];
            System.arraycopy(buffer, 0, b2, 0, buffer.length);
            buffer = b2;
        }
        buffer[offset] = (byte) b;
        offset++;
    }

    public byte[] getBytes() {
        byte ret[] = new byte[offset];
        System.arraycopy(buffer, 0, ret, 0, offset);
        return ret;
    }

    public void write(byte tw[], int pos, int len) {
        if (offset + len >= buffer.length) {
            byte b2[] = new byte[2 * buffer.length + len];
            System.arraycopy(buffer, 0, b2, 0, offset);
            buffer = b2;
        }
        System.arraycopy(tw, pos, buffer, offset, len);
    }

    public void write(InputStream is) throws IOException {
        byte ib[] = new byte[1024];
        int readed;
        while ((readed = is.read(ib)) != -1) write(ib, 0, readed);
    }
}
