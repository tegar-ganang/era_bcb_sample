package com.ewansilver.raindrop.nio;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;

/**
 * A ServerSocketCloseTask will shut down a server-side socket. A
 * ServerSocketCloseEvent will be generated as a response to this task.
 * 
 * @author Ewan Silver
 */
public class ServerSocketCloseTask extends SelectorTask {

    private SelectableChannel channel;

    /**
     * Constructor.
     * 
     * @param aContext
     *            the context that will allow us to work out which ServerSocket
     *            we need to shut down.
     */
    public ServerSocketCloseTask(ServerSocketContext aContext) {
        super(aContext, null, 0, aContext.getTaskQueue());
        channel = aContext.getChannel();
    }

    /**
     * Get the ServerSocketContext attached to this Task.
     * 
     * @return the ServerSocketContext.
     */
    public ServerSocketContext getContext() {
        return (ServerSocketContext) getAttachment();
    }

    protected void register(Selector aSelector) throws CancelledKeyException, ClosedChannelException {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object getSelectorResponse(boolean isSuccessful) {
        return new ServerSocketCloseEvent(getAttachment(), isSuccessful);
    }
}
