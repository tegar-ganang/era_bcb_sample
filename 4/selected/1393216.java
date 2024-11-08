package org.gamegineer.table.internal.net.transport.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import net.jcip.annotations.NotThreadSafe;

/**
 * A connector in the TCP transport layer Acceptor-Connector pattern
 * implementation.
 * 
 * <p>
 * A connector is responsible for actively connecting and initializing a
 * service.
 * </p>
 */
@NotThreadSafe
final class Connector extends AbstractEventHandler {

    /**
     * Initializes a new instance of the {@code Connector} class.
     * 
     * @param transportLayer
     *        The transport layer associated with the connector; must not be
     *        {@code null}.
     */
    Connector(final AbstractTransportLayer transportLayer) {
        super(transportLayer);
    }

    @Override
    void close(final Exception exception) {
        assert exception == null : "asynchronous connection not supported";
        assert isTransportLayerThread();
        setState(State.CLOSED);
    }

    /**
     * Opens the connector and connects to the peer host.
     * 
     * <p>
     * This method blocks until the connection is established or an error
     * occurs.
     * </p>
     * 
     * @param hostName
     *        The host name of the remote peer; must not be {@code null}.
     * @param port
     *        The port of the remote peer.
     * 
     * @throws java.io.IOException
     *         If an I/O error occurs
     */
    void connect(final String hostName, final int port) throws IOException {
        assert hostName != null;
        assert isTransportLayerThread();
        assert getState() == State.PRISTINE;
        try {
            final SocketChannel channel = createSocketChannel(hostName, port);
            final AbstractTransportLayer transportLayer = getTransportLayer();
            final ServiceHandler serviceHandler = new ServiceHandler(transportLayer, transportLayer.createService());
            serviceHandler.open(channel);
        } finally {
            close();
        }
    }

    private static SocketChannel createSocketChannel(final String hostName, final int port) throws IOException {
        assert hostName != null;
        final InetSocketAddress address = new InetSocketAddress(hostName, port);
        if (address.isUnresolved()) {
            throw new IOException(NonNlsMessages.Connector_createSocketChannel_addressUnresolved);
        }
        final SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(true);
        channel.connect(address);
        channel.configureBlocking(false);
        return channel;
    }

    @Override
    SelectableChannel getChannel() {
        assert isTransportLayerThread();
        throw new UnsupportedOperationException("asynchronous connection not supported");
    }

    @Override
    int getInterestOperations() {
        assert isTransportLayerThread();
        throw new UnsupportedOperationException("asynchronous connection not supported");
    }

    @Override
    void run() {
        assert isTransportLayerThread();
        throw new UnsupportedOperationException("asynchronous connection not supported");
    }
}
