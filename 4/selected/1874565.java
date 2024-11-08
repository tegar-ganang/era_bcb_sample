package com.netx.generics.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class Streams {

    private Streams() {
    }

    public static int getDefaultBufferSize() {
        return 1024;
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        copy(in, out, null, getDefaultBufferSize());
    }

    public static void copy(InputStream in, OutputStream out, ProgressObserver observer) throws IOException {
        copy(in, out, observer, getDefaultBufferSize());
    }

    public static void copy(InputStream in, OutputStream out, int bufferSize) throws IOException {
        copy(in, out, null, bufferSize);
    }

    public static void copy(InputStream in, OutputStream out, ProgressObserver observer, int bufferSize) throws IOException {
        if (!(in instanceof BufferedInputStream)) {
            in = new BufferedInputStream(in);
        }
        byte[] buffer = new byte[bufferSize];
        int bytes_read = in.read(buffer, 0, bufferSize);
        while (bytes_read > 0) {
            out.write(buffer, 0, bytes_read);
            if (observer != null) {
                observer.increment(bytes_read);
            }
            bytes_read = in.read(buffer, 0, bufferSize);
        }
        out.flush();
    }

    public static void copy(Reader in, Writer out) throws IOException {
        BufferedReader reader = null;
        if (!(in instanceof BufferedReader)) {
            reader = new BufferedReader(in);
        } else {
            reader = (BufferedReader) in;
        }
        String line = reader.readLine();
        while (line != null) {
            out.write(line);
            out.write("\r\n");
            line = reader.readLine();
        }
        in.close();
        out.flush();
    }
}
