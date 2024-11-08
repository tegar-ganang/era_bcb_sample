package com.totalchange.jizz.broadcaster;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CbrLiveStream extends InputStream {

    /** The number of bits in a byte. */
    private static final int BITS_PER_BYTE = 8;

    /** The number or bits in a kilobit. */
    private static final int ONE_KILOBIT = 1000;

    /** The number of milliseconds in a second. */
    private static final int MS_PER_SECOND = 1000;

    private static final Logger log = LoggerFactory.getLogger(CbrLiveStream.class);

    private InputStream wrapped;

    private long start;

    private int kbps;

    private long currentPos = 0;

    public CbrLiveStream(InputStream wrapped, long start, int kbps) {
        this.wrapped = wrapped;
        this.start = start;
        this.kbps = kbps;
        if (log.isTraceEnabled()) {
            log.trace("Created new constant bitrate input stream due to " + "start at " + new java.util.Date(start) + " with bitrate " + kbps + "kbps");
        }
    }

    /**
     * Decides how many milliseconds to wait based on the bitrate (kilobits per
     * second) and the number of bytes ({@link #BITS_PER_BYTE} bits per byte).
     * So the calculation is <em>ms = (({@link #BITS_PER_BYTE} * bytes) /
     * ({@link #ONE_KILOBIT} * bitrate)) * {@link #MS_PER_SECOND}</em>.
     * 
     * @param bitRate
     *            Bitrate in kilobits per second.
     * @param bytes
     *            Number of bytes that have been read.
     * @return How long it would take to play the number of bytes at the
     *         specified bitrate.
     */
    private static final long howManyMs(int bitRate, long bytes) {
        double secs = (bytes * BITS_PER_BYTE) / (bitRate * ONE_KILOBIT);
        return (long) (secs * MS_PER_SECOND);
    }

    /**
     * Causes the current {@link Thread} to wait for the specified period.
     * 
     * @param waitUntil
     *            When to wait until.
     */
    private static final void waitUntil(long waitUntil) {
        while (System.currentTimeMillis() < waitUntil) {
            try {
                Thread.sleep(waitUntil - System.currentTimeMillis());
            } catch (InterruptedException iEx) {
                log.warn("Thread has been interrupted while pausing", iEx);
            }
        }
    }

    /**
     * Causes the current thread to be blocked as we wait for the virtual time
     * it would take to play this file.
     */
    private void enforcePause() {
        waitUntil(start + howManyMs(kbps, currentPos));
    }

    /**
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    @Override
    public int read() throws IOException {
        enforcePause();
        currentPos++;
        return wrapped.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        enforcePause();
        int read = wrapped.read(b, off, len);
        currentPos += read;
        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        enforcePause();
        int read = wrapped.read(b);
        currentPos += read;
        return read;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Opening destination cbrout.jizz");
        OutputStream out = new BufferedOutputStream(new FileOutputStream("cbrout.jizz"));
        System.out.println("Opening source output.jizz");
        InputStream in = new CbrLiveStream(new BufferedInputStream(new FileInputStream("output.jizz")), System.currentTimeMillis() + 10000, 128);
        System.out.println("Starting read/write loop");
        boolean started = false;
        int len;
        byte[] buf = new byte[4 * 1024];
        while ((len = in.read(buf)) > -1) {
            if (!started) {
                System.out.println("Starting at " + new Date());
                started = true;
            }
            out.write(buf, 0, len);
        }
        System.out.println("Finished at " + new Date());
        out.close();
        in.close();
    }
}
