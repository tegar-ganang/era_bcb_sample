package com.sun.mail.mbox;

import java.io.*;

/**
 * Update the Content-Length header in the message written to the stream.
 */
class ContentLengthUpdater extends FilterOutputStream {

    private String contentLength;

    private boolean inHeader = true;

    private boolean sawContentLength = false;

    private int lastb1 = -1, lastb2 = -1;

    private StringBuffer line = new StringBuffer();

    public ContentLengthUpdater(OutputStream os, long contentLength) {
        super(os);
        this.contentLength = "Content-Length: " + contentLength;
    }

    public void write(int b) throws IOException {
        if (inHeader) {
            String eol = "\n";
            if (b == '\r') {
                if (lastb1 == '\r') {
                    inHeader = false;
                    eol = "\r";
                } else if (lastb1 == '\n' && lastb2 == '\r') {
                    inHeader = false;
                    eol = "\r\n";
                }
            } else if (b == '\n') {
                if (lastb1 == '\n') {
                    inHeader = false;
                    eol = "\n";
                }
            }
            if (!inHeader && !sawContentLength) {
                out.write(contentLength.getBytes("iso-8859-1"));
                out.write(eol.getBytes("iso-8859-1"));
            }
            if (b == '\r' || (b == '\n' && lastb1 != '\r')) {
                if (line.toString().regionMatches(true, 0, "content-length:", 0, 15)) {
                    sawContentLength = true;
                    out.write(contentLength.getBytes("iso-8859-1"));
                } else {
                    out.write(line.toString().getBytes("iso-8859-1"));
                }
                line.setLength(0);
            }
            if (b == '\r' || b == '\n') out.write(b); else line.append((char) b);
            lastb2 = lastb1;
            lastb1 = b;
        } else out.write(b);
    }

    public void write(byte[] b) throws IOException {
        if (inHeader) write(b, 0, b.length); else out.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (inHeader) {
            for (int i = 0; i < len; i++) {
                write(b[off + i]);
            }
        } else out.write(b, off, len);
    }

    public static void main(String argv[]) throws Exception {
        int b;
        ContentLengthUpdater os = new ContentLengthUpdater(System.out, Long.parseLong(argv[0]));
        while ((b = System.in.read()) >= 0) os.write(b);
        os.flush();
    }
}
