package gullsview;

import java.io.*;

public abstract class FileDumper {

    public abstract void next(String path) throws IOException;

    public abstract void write(byte[] buffer, int offset, int length) throws IOException;

    public abstract void close() throws IOException;

    public void pump(InputStream is) throws IOException {
        this.pump(is, 1024);
    }

    public void pump(InputStream is, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int count;
        while ((count = is.read(buffer, 0, buffer.length)) >= 0) this.write(buffer, 0, count);
    }
}
