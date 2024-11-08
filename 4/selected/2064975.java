package net.sf.syracuse.net.impl;

import net.sf.syracuse.net.AcceptServicer;
import net.sf.syracuse.net.NetworkRequest;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Concrete implementation of {@code AcceptEventHandler}.
 *
 * @author Chris Conrad
 * @since 1.0.0
 */
public final class AcceptEventHandlerImpl extends AbstractHandler implements AcceptEventHandler {

    private AcceptServicer acceptServicer;

    /**
     * {@inheritDoc}
     * <p/>
     * <p>The accept event is handled in three stages.
     * <ol>
     * <li>The new connection is accepted</li>
     * <li>The server socket which caused the accept event has interest operation {@link SelectionKey#OP_ACCEPT}
     * set.</li>
     * <li>The newly accepted connection is passed to the {@code AcceptServicer} pipeline for further processing.</li>
     * </ol>
     *
     * @param networkRequest {@inheritDoc}
     */
    public void handleAccept(NetworkRequest networkRequest) {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) networkRequest.getChannel();
        try {
            SocketChannel channel = serverSocketChannel.accept();
            networkEventThread.addChannelInterestOps(serverSocketChannel, SelectionKey.OP_ACCEPT);
            acceptServicer.accept(channel);
        } catch (IOException e) {
            log.error("Error accepting connection", e);
        }
    }

    /**
     * Sets the {@code AcceptServicer} pipeline.
     *
     * @param acceptServicer the {@code AcceptServicer} pipeline to set
     */
    public void setAcceptServicer(AcceptServicer acceptServicer) {
        this.acceptServicer = acceptServicer;
    }
}
