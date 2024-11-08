package ch.unifr.nio.framework;

import ch.unifr.nio.framework.transform.ChannelReader;
import ch.unifr.nio.framework.transform.ChannelWriter;
import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An adapter class that shields conctrete channel handlers from the dirty
 * details of NIO (dispatcher, selection keys, ...)
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class HandlerAdapter extends Thread {

    private static final Logger LOGGER = Logger.getLogger(HandlerAdapter.class.getName());

    private final Dispatcher dispatcher;

    private final ChannelHandler channelHandler;

    private final ChannelReader channelReader;

    private final ChannelWriter channelWriter;

    private final SelectionKey selectionKey;

    private int cachedInterestOps;

    private int cachedReadyOps;

    private boolean opsCached;

    private final String debugName;

    /**
     * Creates a new instance of HandlerAdapter
     *
     * @param debugName a descriptive name for debugging purposes
     * @param selectionKey the selection key of the channel this handler must
     * deal with
     * @param dispatcher the central dispatcher
     * @param channelHandler the concrete channel handler
     */
    public HandlerAdapter(Dispatcher dispatcher, ChannelHandler channelHandler, SelectionKey selectionKey, String debugName) {
        super("HandlerAdapter");
        this.dispatcher = dispatcher;
        this.channelHandler = channelHandler;
        this.selectionKey = selectionKey;
        this.debugName = debugName;
        channelReader = channelHandler.getChannelReader();
        channelWriter = channelHandler.getChannelWriter();
        cachedInterestOps = selectionKey.interestOps();
    }

    @Override
    public void run() {
        LOGGER.log(Level.FINEST, "{0} output handling", debugName);
        try {
            if ((cachedReadyOps & SelectionKey.OP_CONNECT) != 0) {
                SelectableChannel channel = selectionKey.channel();
                if (!(channel instanceof SocketChannel)) {
                    throw new IllegalStateException("SelectionKey is " + "connectable but channel is no SocketChannel!");
                }
                if (!(channelHandler instanceof ClientSocketChannelHandler)) {
                    throw new IllegalStateException("SelectionKey is connectable but handler is no " + "ClientSocketChannelHandler!");
                }
                SocketChannel socketChannel = (SocketChannel) channel;
                synchronized (socketChannel) {
                    if (socketChannel.isOpen()) {
                        ClientSocketChannelHandler clientSocketChannelHandler = (ClientSocketChannelHandler) channelHandler;
                        try {
                            socketChannel.finishConnect();
                            clientSocketChannelHandler.connectSucceeded();
                            cachedInterestOps = SelectionKey.OP_READ;
                        } catch (IOException ex) {
                            cachedReadyOps = 0;
                            selectionKey.cancel();
                            clientSocketChannelHandler.connectFailed(ex);
                        }
                    }
                }
            }
            if ((cachedReadyOps & SelectionKey.OP_WRITE) != 0) {
                if (channelWriter.drain()) {
                    removeInterestOps(SelectionKey.OP_WRITE);
                }
            }
            LOGGER.log(Level.FINEST, "{0} input handling", debugName);
            if ((cachedReadyOps & SelectionKey.OP_READ) != 0) {
                channelReader.read();
                if (channelReader.isClosed()) {
                    LOGGER.log(Level.FINE, "{0} input closed -> removing read interest", debugName);
                    removeInterestOps(SelectionKey.OP_READ);
                    channelHandler.inputClosed();
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "", ex);
            try {
                closeChannel();
            } catch (IOException ex2) {
                LOGGER.log(Level.WARNING, "", ex2);
            }
            channelHandler.channelException(ex);
        } finally {
            synchronized (this) {
                if (selectionKey.isValid()) {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "{0} resuming selection with {1}", new Object[] { debugName, interestToString(cachedInterestOps) });
                    }
                    dispatcher.setInterestOps(selectionKey, cachedInterestOps);
                }
                opsCached = false;
            }
        }
        LOGGER.log(Level.FINEST, "{0} done", debugName);
    }

    /**
     * The dispatcher calls this method to prevent further channel selection.
     * Therefore we must cache the current interestOps so that we can restore
     * them at the end of run().
     *
     * @throws java.nio.channels.CancelledKeyException if the key was cancelled
     */
    public synchronized void cacheOps() throws CancelledKeyException {
        cachedInterestOps = selectionKey.interestOps();
        cachedReadyOps = selectionKey.readyOps();
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "{0} starting with {1}", new Object[] { debugName, interestToString(cachedInterestOps) });
        }
        selectionKey.interestOps(0);
        opsCached = true;
    }

    /**
     * takes caching into account when removing interest ops from the channel
     *
     * @param interestOps the interest ops to remove
     */
    public synchronized void removeInterestOps(int interestOps) {
        if ((cachedInterestOps & interestOps) == 0) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "{0}: {1} not set", new Object[] { debugName, interestToString(interestOps) });
            }
            return;
        }
        cachedInterestOps &= ~interestOps;
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "{0}: cachedInterestOps set to {1}", new Object[] { debugName, interestToString(cachedInterestOps) });
        }
        if (!opsCached) {
            dispatcher.removeInterestOps(selectionKey, interestOps);
        }
    }

    /**
     * takes caching into account when adding interest ops to the channel
     *
     * @param interestOps the interest ops to add
     */
    public synchronized void addInterestOps(int interestOps) {
        if ((cachedInterestOps & interestOps) == interestOps) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "{0}: {1} was already there", new Object[] { debugName, interestToString(interestOps) });
            }
            return;
        }
        cachedInterestOps |= interestOps;
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "{0}: cachedInterestOps set to {1}", new Object[] { debugName, interestToString(cachedInterestOps) });
        }
        if (!opsCached) {
            dispatcher.setInterestOps(selectionKey, cachedInterestOps);
        }
    }

    /**
     * closes the channel
     *
     * @throws java.io.IOException if closing the channel fails
     */
    public void closeChannel() throws IOException {
        cachedReadyOps = 0;
        dispatcher.closeChannel(selectionKey);
    }

    /**
     * returns the Channel we are dealing with
     *
     * (This somehow breaks encapsulation of all the ugly details but there
     * seems to be no simpler way to provide access to all features of the
     * Socket class...)
     *
     * @return the Channel we are dealing with
     */
    public synchronized Channel getChannel() {
        return selectionKey.channel();
    }

    /**
     * returns a String representation of an interest set
     *
     * @param interest an interest set
     * @return a String representation of an interest set
     */
    public static String interestToString(int interest) {
        StringBuilder stringBuilder = new StringBuilder();
        if ((interest & SelectionKey.OP_ACCEPT) != 0) {
            stringBuilder.append("OP_ACCEPT ");
        }
        if ((interest & SelectionKey.OP_CONNECT) != 0) {
            stringBuilder.append("OP_CONNECT ");
        }
        if ((interest & SelectionKey.OP_READ) != 0) {
            stringBuilder.append("OP_READ ");
        }
        if ((interest & SelectionKey.OP_WRITE) != 0) {
            stringBuilder.append("OP_WRITE ");
        }
        if (stringBuilder.length() == 0) {
            stringBuilder.append("NO INTEREST");
        }
        return stringBuilder.toString();
    }
}
