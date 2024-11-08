package org.asyncj.handlers;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public abstract class AsyncHandler implements Handler {

    public AsyncHandler(SelectableChannel aChannel) throws IOException {
        channel = aChannel;
        channel.configureBlocking(false);
        criteria = getDefaultCriteria();
    }

    public int getCriteria() {
        return criteria;
    }

    public void setCriteria(int mask) {
        criteria = mask;
    }

    public void close() throws IOException {
        channel.close();
    }

    protected SelectableChannel channel;

    protected int criteria;

    public void setWriteReady(boolean isReady) {
        if (isReady) {
            setCriteria(getCriteria() | SelectionKey.OP_WRITE);
        } else {
            setCriteria(getCriteria() & ~SelectionKey.OP_WRITE);
        }
    }

    public String toString() {
        return channel.toString();
    }

    public SelectableChannel getChannel() {
        return channel;
    }
}
