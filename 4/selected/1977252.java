package org.one.stone.soup.server.http.helpers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class HttpPostInputStreamBuffer extends InputStream implements Runnable {

    private InputStream iStream;

    private int[] store = new int[5000];

    private int writeCursor = 0;

    private int readCursor = 0;

    private Exception exception;

    private boolean finished = false;

    public HttpPostInputStreamBuffer(InputStream iStream) {
        this.iStream = iStream;
        if ((iStream instanceof BufferedInputStream) == false) {
            iStream = new BufferedInputStream(iStream);
        }
        new Thread(this, "HttpPostInputStreamBuffer").start();
    }

    public int bytesAvailable() {
        return writeCursor - readCursor;
    }

    public int read() throws IOException {
        if (readCursor >= writeCursor) {
            int count = 0;
            while (readCursor >= writeCursor) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                }
                count++;
                if (count > 1000) {
                    throw new IOException("HttpPostInputStreamBuffer.read timed out.");
                }
            }
        }
        int out = store[readCursor % 5000];
        readCursor++;
        return out;
    }

    public void isFinished() {
    }

    public void run() {
        try {
            int in = iStream.read();
            while (in != -1) {
                store[writeCursor % 5000] = in;
                writeCursor++;
                if ((writeCursor - readCursor) >= 5000) {
                    int count = 0;
                    while ((writeCursor - readCursor) >= 5000) {
                        try {
                            Thread.sleep(10);
                        } catch (Exception e) {
                        }
                        count++;
                        if (count > 1000) {
                            throw new IOException("HttpPostInputStreamBuffer.run timed out.");
                        }
                    }
                }
                in = iStream.read();
            }
        } catch (Exception e) {
            exception = e;
        }
        finished = true;
    }
}
