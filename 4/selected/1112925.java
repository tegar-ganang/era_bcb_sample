package org.p2s.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamProxy extends InputStream {

    InputStream in;

    FileOutputStream out;

    String file;

    public InputStreamProxy(InputStream in, String file) {
        this.in = in;
        this.file = file;
        try {
            out = new FileOutputStream(file + ".tmp");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public int read() throws IOException {
        int read = in.read();
        if (out != null && read > 0) out.write(read);
        return read;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        if (out != null && read > 0) out.write(b, off, read);
        return read;
    }

    public int read(byte[] b) throws IOException {
        int read = in.read(b);
        if (out != null && read > 0) out.write(b, 0, read);
        return read;
    }

    public int available() throws IOException {
        return in.available();
    }

    public void close() throws IOException {
        in.close();
        if (out != null) {
            out.close();
            (new File(file + ".tmp")).renameTo(new File(file));
        }
    }

    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    public boolean markSupported() {
        return in.markSupported();
    }

    public synchronized void reset() throws IOException {
        in.reset();
    }

    public long skip(long n) throws IOException {
        return in.skip(n);
    }
}
