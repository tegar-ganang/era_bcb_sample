package com.sleepycat.je.rep.utilint;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.rep.impl.node.RepNode;
import com.sleepycat.je.utilint.LoggerUtils;

/**
 * NamedChannelWithTimeout permits association of timeouts with a SocketChannel.
 * This mechanism is necessary, since the standard mechanism for associating
 * timeouts with sockets using Socket.setSoTimeout is not supported by nio
 * SocketChannels.
 */
public class NamedChannelWithTimeout extends NamedChannel {

    private volatile boolean readActivity;

    private volatile int timeoutMs;

    private final EnvironmentImpl envImpl;

    private final Logger logger;

    private long lastCheckMs = 0l;

    public NamedChannelWithTimeout(RepNode repNode, SocketChannel channel, int timeoutMs) {
        super(channel);
        this.timeoutMs = timeoutMs;
        this.envImpl = repNode.getRepImpl();
        this.logger = repNode.getLogger();
        readActivity = true;
        if (timeoutMs > 0) {
            repNode.getChannelTimeoutTask().register(this);
        }
    }

    /**
     * Used to modify the timeout associated with the channel.
     *
     * @param timeoutMs the new timeout value
     */
    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
        readActivity = true;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        final int bytes = channel.read(dst);
        if (bytes > 0) {
            readActivity = true;
        }
        return bytes;
    }

    @Override
    public void close() throws IOException {
        channel.close();
        readActivity = false;
    }

    private void resetActivityCounter(long timeMs) {
        lastCheckMs = timeMs;
        readActivity = false;
    }

    /**
     * Method invoked by the time thread to check on the channel on a periodic
     * basis. Note that the time that is passed in is a "pseudo" time that is
     * only meaningful for calculating time differences.
     *
     * @param timeMs the pseudo time
     *
     * @return true if the channel is active, false if it isn't and has been
     * closed
     */
    public boolean isActive(long timeMs) {
        if (!channel.isOpen()) {
            return false;
        }
        if (!channel.isConnected()) {
            return true;
        }
        if (readActivity) {
            resetActivityCounter(timeMs);
            return true;
        }
        if ((timeoutMs == 0) || (timeMs - lastCheckMs) < timeoutMs) {
            return true;
        }
        LoggerUtils.info(logger, envImpl, "Inactive channel: " + getNameIdPair() + " forced close. Timeout: " + timeoutMs + "ms.");
        try {
            channel.close();
        } catch (IOException e) {
        }
        return false;
    }
}
