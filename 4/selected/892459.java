package com.sun.mail.mbox;

import java.io.*;

/**
 * Count the number of bytes in the body of the message written to the stream.
 */
class ContentLengthCounter extends OutputStream {

    private long size = 0;

    private boolean inHeader = true;

    private int lastb1 = -1, lastb2 = -1;

    public void write(int b) throws IOException {
        if (inHeader) {
            if (b == '\r' && lastb1 == '\r') inHeader = false; else if (b == '\n') {
                if (lastb1 == '\n') inHeader = false; else if (lastb1 == '\r' && lastb2 == '\n') inHeader = false;
            }
            lastb2 = lastb1;
            lastb1 = b;
        } else size++;
    }

    public void write(byte[] b) throws IOException {
        if (inHeader) super.write(b); else size += b.length;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (inHeader) super.write(b, off, len); else size += len;
    }

    public long getSize() {
        return size;
    }

    public static void main(String argv[]) throws Exception {
        int b;
        ContentLengthCounter os = new ContentLengthCounter();
        while ((b = System.in.read()) >= 0) os.write(b);
        System.out.println("size " + os.getSize());
    }
}
