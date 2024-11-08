package org.hsqldb.lib;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

/**
 * A wrapper for OutputStream
 *
 * @author Fred Toussi (fredt@users dot sourceforge.net)
 * @version 1.9.0
 * @since 1.9.0
 */
public class DataOutputStream extends java.io.BufferedOutputStream {

    byte[] tempBuffer = new byte[8];

    public DataOutputStream(OutputStream stream) {
        super(stream, 8);
    }

    public final void writeByte(int v) throws IOException {
        write(v);
    }

    public final void writeInt(int v) throws IOException {
        int count = 0;
        tempBuffer[count++] = (byte) (v >>> 24);
        tempBuffer[count++] = (byte) (v >>> 16);
        tempBuffer[count++] = (byte) (v >>> 8);
        tempBuffer[count++] = (byte) v;
        write(tempBuffer, 0, count);
    }

    public final void writeLong(long v) throws IOException {
        writeInt((int) (v >>> 32));
        writeInt((int) v);
    }

    public void writeChar(int v) throws IOException {
        int count = 0;
        tempBuffer[count++] = (byte) (v >>> 8);
        tempBuffer[count++] = (byte) v;
        write(tempBuffer, 0, count);
    }

    public void writeChars(String s) throws IOException {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            int v = s.charAt(i);
            int count = 0;
            tempBuffer[count++] = (byte) (v >>> 8);
            tempBuffer[count++] = (byte) v;
            write(tempBuffer, 0, count);
        }
    }

    public void writeChars(char[] c) throws IOException {
        writeChars(c, c.length);
    }

    public void writeChars(char[] c, int length) throws IOException {
        for (int i = 0; i < length; i++) {
            int v = c[i];
            int count = 0;
            tempBuffer[count++] = (byte) (v >>> 8);
            tempBuffer[count++] = (byte) v;
            write(tempBuffer, 0, count);
        }
    }

    public void write(Reader reader, long length) throws IOException {
        InputStream inputStream = new ReaderInputStream(reader);
        write(inputStream, length * 2);
    }

    public void write(InputStream inputStream, long length) throws IOException {
        CountdownInputStream countStream = new CountdownInputStream(inputStream);
        countStream.setCount(length);
        byte[] data = new byte[128];
        while (true) {
            int count = countStream.read(data);
            if (count < 1) {
                if (countStream.getCount() != 0) {
                    throw new EOFException();
                }
                break;
            }
            write(data, 0, count);
        }
    }
}
