package com.noahsloan.nutils.streams;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Echos all bytes read to an output stream. Useful for debugging.
 * 
 * @author noah
 * 
 */
public class EchoInputStream extends FilterInputStream {

    private OutputStream out;

    public EchoInputStream(InputStream in) {
        this(in, System.out);
    }

    public EchoInputStream(InputStream in, OutputStream out) {
        super(in);
        this.out = out;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            out.write(b);
        }
        return b;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        if (read != -1) {
            out.write(b, off, read);
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        super.close();
        out.close();
    }
}
