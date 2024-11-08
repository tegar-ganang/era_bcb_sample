package org.xsocket.datagram;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * an implementation of a non-blocking connected endpoint
 * 
 * @author grro
 */
public final class NonBlockingConnectedEndpoint extends NonBlockingEndpoint implements IConnectedEndpoint {

    private SocketAddress remoteAddress = null;

    private long connectionOpenedTime = -1;

    /**
	 * constructor<br><br>
     * 
     * @param host    the remote host
     * @param port    the remote port
     * @throws IOException If some I/O error occurs
	 */
    public NonBlockingConnectedEndpoint(String host, int port) throws IOException {
        this(new InetSocketAddress(host, port), null, 0, 0);
    }

    /**
	 * constructor<br><br>
     * 
     * @param address   the socket address of the remote endpoint
     * @throws IOException If some I/O error occurs
	 */
    public NonBlockingConnectedEndpoint(SocketAddress address) throws IOException {
        this(address, null, 0, 0);
    }

    /**
	 * constructor
	 * 
     * @param appHandler               the data handler
     * @param receivePacketSize        the receive packet size    
     * @param workerPoolSize           the instance exclusive workerpool size or 0 if global workerpool shot be used
     * @param remoteAddress            the remoteAddress
     * @throws IOException If some I/O error occurs
	 */
    public NonBlockingConnectedEndpoint(SocketAddress remoteAddress, IDatagramHandler appHandler, int receivePacketSize, int workerPoolSize) throws IOException {
        super(0, appHandler, receivePacketSize, workerPoolSize);
        this.remoteAddress = remoteAddress;
        getChannel().connect(remoteAddress);
        connectionOpenedTime = System.currentTimeMillis();
    }

    /**
	 * {@inheritDoc}
	 */
    public final void send(Packet packet) throws IOException {
        packet.setRemoteAddress(remoteAddress);
        super.send(packet);
    }

    /**
	 * {@inheritDoc}
	 */
    public void close() {
        remoteAddress = null;
        super.close();
    }

    /**
	 * {@inheritDoc}
	 */
    public SocketAddress getRemoteSocketAddress() {
        return remoteAddress;
    }
}
