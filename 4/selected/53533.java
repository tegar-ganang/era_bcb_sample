package com.peterhi.net.util;

import java.io.IOException;
import java.net.SocketAddress;
import com.peterhi.net.Channel;

/**
 * A wrapper to {@link Channel} which blocks
 * on each operation until successful. An
 * exception will be thrown on each operation
 * if it has waited longer than a timeout,
 * specified by the constructor.
 * @author hytparadisee
 */
public class BlockingChannel implements Channel {

    private final Channel channel;

    private long timeout;

    /**
	 * Creates a new instance of {@link BlockingChannel},
	 * wrapping a {@link Channel}, with a specified timeout.
	 * @param channel The non-blocking {@link Channel} to wrap.
	 * @param timeout The timeout value to throw an {@link Exception},
	 * or 0 to disable timeout.
	 */
    public BlockingChannel(Channel channel, long timeout) {
        this.channel = channel;
    }

    public SocketAddress getSocketAddress() {
        return channel.getSocketAddress();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        try {
            while (true) {
                int read = channel.read(b, off, len);
                if (read > 0) return read;
                Thread.sleep(100);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    public int write(byte[] b, int off, int len) throws IOException {
        int total = 0;
        try {
            while (total < len) {
                int written = channel.write(b, off + total, len - total);
                if (written > 0) {
                    total += written;
                    while (channel.write(null, 0, 0) != 0) Thread.sleep(100);
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        return total;
    }
}
