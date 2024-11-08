package org.gamegineer.table.internal.net.transport.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import net.jcip.annotations.NotThreadSafe;
import org.gamegineer.table.internal.net.Loggers;

/**
 * An acceptor in the TCP transport layer Acceptor-Connector pattern
 * implementation.
 * 
 * <p>
 * An acceptor is responsible for passively connecting and initializing a
 * service.
 * </p>
 */
@NotThreadSafe
final class Acceptor extends AbstractEventHandler {

    /** Indicates the acceptor is registered with the dispatcher. */
    private boolean isRegistered_;

    /** The server socket channel on which incoming connections are accepted. */
    private ServerSocketChannel serverChannel_;

    /**
     * Initializes a new instance of the {@code Acceptor} class.
     * 
     * @param transportLayer
     *        The transport layer associated with the acceptor; must not be
     *        {@code null}.
     */
    Acceptor(final AbstractTransportLayer transportLayer) {
        super(transportLayer);
        isRegistered_ = false;
        serverChannel_ = null;
    }

    /**
     * Invoked when the server channel is ready to accept a new connection.
     */
    private void accept() {
        final SocketChannel clientChannel;
        try {
            clientChannel = serverChannel_.accept();
            if (clientChannel == null) {
                return;
            }
            clientChannel.configureBlocking(false);
            final AbstractTransportLayer transportLayer = getTransportLayer();
            final ServiceHandler serviceHandler = new ServiceHandler(transportLayer, transportLayer.createService());
            serviceHandler.open(clientChannel);
        } catch (final IOException e) {
            Loggers.getDefaultLogger().log(Level.SEVERE, NonNlsMessages.Acceptor_accept_ioError, e);
        }
    }

    /**
     * Opens the acceptor and binds the acceptor channel.
     * 
     * <p>
     * This method blocks until the acceptor channel is bound or an error
     * occurs.
     * </p>
     * 
     * @param hostName
     *        The host name to which the acceptor will be bound; must not be
     *        {@code null}.
     * @param port
     *        The port to which the acceptor will be bound.
     * 
     * @throws java.io.IOException
     *         If an I/O error occurs
     */
    void bind(final String hostName, final int port) throws IOException {
        assert hostName != null;
        assert isTransportLayerThread();
        assert getState() == State.PRISTINE;
        try {
            serverChannel_ = createServerSocketChannel(hostName, port);
            setState(State.OPEN);
            getTransportLayer().getDispatcher().registerEventHandler(this);
            isRegistered_ = true;
        } catch (final IOException e) {
            close(e);
            throw e;
        }
    }

    @Override
    void close(final Exception exception) {
        assert isTransportLayerThread();
        final State previousState = getState();
        if (previousState == State.OPEN) {
            if (isRegistered_) {
                isRegistered_ = false;
                getTransportLayer().getDispatcher().unregisterEventHandler(this);
            }
            try {
                serverChannel_.close();
            } catch (final IOException e) {
                Loggers.getDefaultLogger().log(Level.SEVERE, NonNlsMessages.Acceptor_close_ioError, e);
            } finally {
                serverChannel_ = null;
            }
        }
        setState(State.CLOSED);
        if (previousState == State.OPEN) {
            getTransportLayer().disconnected(exception);
        }
    }

    private static ServerSocketChannel createServerSocketChannel(final String hostName, final int port) throws IOException {
        assert hostName != null;
        final ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(new InetSocketAddress(hostName, port));
        return channel;
    }

    @Override
    SelectableChannel getChannel() {
        assert isTransportLayerThread();
        return serverChannel_;
    }

    @Override
    int getInterestOperations() {
        assert isTransportLayerThread();
        return SelectionKey.OP_ACCEPT;
    }

    @Override
    void run() {
        assert isTransportLayerThread();
        final SelectionKey selectionKey = getSelectionKey();
        if ((selectionKey != null) && selectionKey.isAcceptable()) {
            accept();
        }
    }
}
