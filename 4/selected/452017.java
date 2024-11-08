package com.codename1.impl.ios;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Implements the output stream interface on top of NSData
 *
 * @author Shai Almog
 */
public class NSDataOutputStream extends ByteArrayOutputStream {

    private String file;

    private long nsDataPtr;

    private int written;

    public NSDataOutputStream(String file) {
        this.file = file;
    }

    public NSDataOutputStream(String file, int offset) throws IOException {
        this.file = file;
        write(readOff(file, offset));
    }

    private static byte[] readOff(String file, int offset) throws IOException {
        byte[] bytes = new byte[IOSNative.getFileSize(file)];
        IOSNative.readFile(file, bytes);
        if (offset != bytes.length) {
            byte[] d = new byte[offset];
            System.arraycopy(bytes, 0, d, 0, Math.min(offset, bytes.length));
            return d;
        }
        return bytes;
    }

    public void flush() throws IOException {
        super.flush();
        byte[] b = toByteArray();
        if (b.length == written) {
            return;
        }
        written = b.length;
        IOSNative.writeToFile(b, file);
    }

    @Override
    public void close() throws IOException {
        flush();
        super.close();
    }
}
