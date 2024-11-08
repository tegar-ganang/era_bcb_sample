package com.cnoja.jmsncn.utils.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {

    private int copyBufferSize = 1024;

    public int getCopyBufferSize() {
        return copyBufferSize;
    }

    public void setCopyBufferSize(int copyBufferSize) {
        this.copyBufferSize = copyBufferSize;
    }

    public void copy(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[copyBufferSize];
        int read = -1;
        while ((read = in.read(b, 0, b.length)) != -1) {
            out.write(b, 0, read);
        }
    }

    public void copy(InputStream in, OutputStream out, boolean closeIn, boolean closeOut) throws IOException {
        byte[] b = new byte[copyBufferSize];
        int read = -1;
        while ((read = in.read()) != -1) {
            out.write(b, 0, read);
        }
        if (closeIn) {
            in.close();
        }
        if (closeOut) {
            out.close();
        }
    }

    public String getAsString(FileInputStream fileInputStream, String encoding) throws IOException {
        if (fileInputStream == null) {
            throw new IllegalArgumentException("The input stream is null!");
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        copy(new BufferedInputStream(fileInputStream), stream);
        return stream.toString(encoding);
    }
}
