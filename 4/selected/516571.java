package org.scohen.juploadr.upload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BandwidthThrottlingOutputStream extends OutputStream {

    private static final int FREQUENCY = 8;

    private static final int WINDOW_LENGTH = 1000 / FREQUENCY;

    private int bytesPerWindow;

    private int bytesWrittenThisWindow = 0;

    private long windowEnd;

    private OutputStream out;

    long writeStartedAt = -1;

    public BandwidthThrottlingOutputStream(final OutputStream out, int maxBytesPerSecond) {
        super();
        this.bytesPerWindow = maxBytesPerSecond / FREQUENCY;
        this.out = out;
    }

    public void write(int b) throws IOException {
        checkTime();
        if (bytesWrittenThisWindow > bytesPerWindow) {
            synchronized (out) {
                long howLongToWait = windowEnd - System.currentTimeMillis();
                if (howLongToWait > 0) {
                    try {
                        out.wait(howLongToWait);
                    } catch (InterruptedException e) {
                        throw new IOException("Write interrupted");
                    }
                }
            }
        }
        out.write(b);
        bytesWrittenThisWindow++;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (len <= bytesPerWindow) {
            out.write(b, off, len);
        } else {
            byte[] buffer = new byte[bytesPerWindow];
            ByteArrayInputStream in = new ByteArrayInputStream(b, off, len);
            while (in.available() > 0) {
                checkTime();
                int numread = in.read(buffer);
                out.write(buffer, 0, numread);
                bytesWrittenThisWindow += numread;
                if (bytesWrittenThisWindow > bytesPerWindow) {
                    synchronized (out) {
                        long howLongToWait = windowEnd - System.currentTimeMillis();
                        if (howLongToWait > 0) {
                            try {
                                out.wait(howLongToWait);
                            } catch (InterruptedException e) {
                                throw new IOException("Write interrupted");
                            }
                        }
                    }
                }
            }
        }
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void setMaxBytesPerSecond(int bytesPerSecond) {
        this.bytesPerWindow = bytesPerSecond;
    }

    private void checkTime() {
        if (System.currentTimeMillis() - writeStartedAt >= WINDOW_LENGTH) {
            writeStartedAt = System.currentTimeMillis();
            windowEnd = writeStartedAt + WINDOW_LENGTH;
            bytesWrittenThisWindow = 0;
        }
    }
}
