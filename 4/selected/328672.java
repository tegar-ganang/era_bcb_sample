package com.sun.mail.mbox;

import java.io.*;

/**
 * Count number of lines output.
 */
class LineCounter extends FilterOutputStream {

    private int lastb = -1;

    protected int lineCount;

    public LineCounter(OutputStream os) {
        super(os);
    }

    public void write(int b) throws IOException {
        if (b == '\r' || (b == '\n' && lastb != '\r')) lineCount++;
        out.write(b);
        lastb = b;
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            write(b[off + i]);
        }
    }

    public int getLineCount() {
        return lineCount;
    }

    public static void main(String argv[]) throws Exception {
        int b;
        LineCounter os = new LineCounter(System.out);
        while ((b = System.in.read()) >= 0) os.write(b);
        os.flush();
        System.out.println(os.getLineCount());
    }
}
