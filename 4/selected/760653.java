package com.noahsloan.nutils.streams;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ScatteringByteChannel;

/**
 * Wraps an InputStream so it can be used easily with Java's NIO classes. Not as
 * efficient as using a real channel since it involves a read into a byte array,
 * and then copying into the byte buffers.
 * 
 * @author noah
 * 
 */
public class InputStreamChannel implements ScatteringByteChannel {

    private final InputStream in;

    private volatile boolean closed;

    private InputStreamChannel(final InputStream in) {
        super();
        this.in = in;
    }

    public static ScatteringByteChannel getChannel(InputStream in) {
        if (in instanceof FileInputStream) {
            return ((FileInputStream) in).getChannel();
        } else {
            return new InputStreamChannel(in);
        }
    }

    public int read(ByteBuffer dst) throws IOException {
        return (int) read(new ByteBuffer[] { dst });
    }

    public long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }

    public synchronized long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        long len = 0;
        for (int i = offset; i < length; i++) {
            len += dsts[i].limit() - dsts[i].position();
        }
        byte[] buffer = new byte[(int) Math.min(len, OutputStreamChannel.MAX_BUFFER)];
        int read = in.read(buffer);
        if (read == -1) {
            close();
        } else {
            int copied = 0;
            for (int i = offset; i < length; i++) {
                int lim = Math.min(read - copied, dsts[i].limit() - dsts[i].position());
                dsts[i].put(buffer, copied, lim);
                copied += lim;
            }
        }
        return read;
    }

    public synchronized void close() throws IOException {
        in.close();
        closed = true;
    }

    public boolean isOpen() {
        return !closed;
    }
}
