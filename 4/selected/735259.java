package org.sepp.connections;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sepp.api.components.SecureSelfOrganizingNetwork;
import org.sepp.config.Peer;
import org.sepp.database.Database;
import org.sepp.exceptions.SeppCommunicationException;
import org.sepp.messages.common.CloseConnectionInfo;
import org.sepp.messages.common.Message;
import org.sepp.messages.common.OpenConnectionInfo;
import org.sepp.utils.Constants;

public class DirectConnection {

    private Log log;

    protected String peerId = Constants.NO_PEER_ID;

    protected SocketChannel channel;

    protected long lastUsed;

    protected String networkId;

    protected String remoteAddress;

    protected int remotePort;

    protected boolean closed = false;

    protected boolean ready = false;

    protected List receiveBuffer = new ArrayList();

    /**
	 * Creates a {@link DirectConnection} object from the provided host address,
	 * host port and peerId. The host address and port are used to open a socket
	 * and the peerId is then associated with the remote host. This method
	 * creates the necessary input and output streams from the socket and
	 * records the time it has been created in the <code>lastUsed</code>
	 * variable.
	 * 
	 * @param hostAddress
	 *            The IP address associated with the remote peer
	 * @param hostPort
	 *            The port on which the remote peer is listening.
	 * @param peerId
	 *            The ID of the remote peer.
	 * @throws SeppCommunicationException
	 *             Thrown if the connection couldn't be created.
	 */
    public DirectConnection(String hostAddress, int hostPort, String peerId, String networkId) throws SeppCommunicationException {
        try {
            log = LogFactory.getLog(this.getClass());
            this.peerId = peerId;
            this.networkId = networkId;
            channel = SocketChannel.open();
            channel.connect(new InetSocketAddress(hostAddress, hostPort));
            channel.finishConnect();
            log.debug("Creating direct connection from IP and port.");
            lastUsed = System.currentTimeMillis();
            sendMessage(new Message(new OpenConnectionInfo(SecureSelfOrganizingNetwork.localAddress, Constants.STANDARD_PORT), peerId));
            ready = true;
        } catch (Exception e) {
            throw new SeppCommunicationException("Couldn't create direct connection to peer " + peerId + ". Reason: " + e.getMessage());
        }
    }

    /**
	 * Creates a {@link DirectConnection} object from an accepted listen socket.
	 * This method creates the necessary input and output streams from the
	 * socket and records the time it has been created in the
	 * <code>lastUsed</code> variable.
	 * 
	 * @param channel
	 *            The accepted listen socket to create the connection from.
	 * @throws SeppCommunicationException
	 *             Thrown if the connection couldn't be created.
	 */
    public DirectConnection(SocketChannel channel, String networkId) throws SeppCommunicationException {
        log = LogFactory.getLog(this.getClass());
        this.channel = channel;
        this.networkId = networkId;
        lastUsed = System.currentTimeMillis();
        log.debug("Creating direct connection from channel.");
    }

    /**
	 * Sends the provided message to the connected peer. Since several threads
	 * or services can use the same connection to send messages to the connected
	 * peer this method is synchronized. If the message couldn't be sent only an
	 * log output is generated.
	 * 
	 * TODO:Check if this behavior is correct.
	 * 
	 * @param message
	 *            The message which should be sent to the connected peer.
	 */
    public void sendMessage(Message message) throws SeppCommunicationException {
        synchronized (channel) {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(message.getBytes().length);
                buffer.put(message.getBytes());
                buffer.flip();
                channel.write(buffer);
                Database.database.storeMessageInfo(message, SecureSelfOrganizingNetwork.localPeerId, peerId, System.currentTimeMillis(), true);
                if (message.getType() != OpenConnectionInfo.type && message.getType() != CloseConnectionInfo.type) {
                    log.info(message.getTypeDescription() + " message to peer " + peerId + " for peer " + message.getDestination() + " successfully sent!");
                }
                lastUsed = System.currentTimeMillis();
            } catch (IOException ioe) {
                throw new SeppCommunicationException("Couldn't send message to peer " + peerId + ". Reason: " + ioe.getMessage());
            }
        }
    }

    /**
	 * Returns the underlying channel to this connection. The channel has either
	 * been actively created using IP and port or has been obtained through the
	 * {@link ServerSocketChannel#accept()} method.
	 * 
	 * @return The {@link SocketChannel} associated with this connection.
	 */
    public SocketChannel getChannel() {
        return channel;
    }

    /**
	 * Closes the direct connection with the remote peer gracefully. This means
	 * that a CloseConnectionInfo message is being sent if possible. Otherwise
	 * the direct connection is closed anyway.
	 */
    public synchronized void activeClose() {
        try {
            try {
                sendMessage(new Message(new CloseConnectionInfo(), peerId));
            } catch (SeppCommunicationException e) {
                log.error("Couldn't send CloseConnectionInfo to peer " + peerId + ".");
            }
            channel.close();
            log.debug("Connection to peer " + peerId + " closed.");
        } catch (Exception ioe) {
            log.error("Couldn't close the streams and socket to peer " + peerId);
            log.debug("Reason: " + ioe.getMessage());
        }
        closed = true;
    }

    /**
	 * Closes this connection without sending a {@link CloseConnectionInfo}
	 * message to the remote peer. This method is usually used if an error has
	 * occurred during sending or receiving over this connection, which
	 * indicates that the connection is not available anymore.
	 */
    public void passiveClose() {
        try {
            channel.close();
        } catch (Exception ioe) {
            log.error("Couldn't close the socket to peer " + peerId);
            log.debug("Reason: " + ioe.getMessage());
        }
        closed = true;
    }

    /**
	 * Returns the peer ID of the communication counterpart which is in all case
	 * an neighbor because to no other peers a direct connection must be made.
	 * This is because we have a heterogeneous network we must to elect
	 * neighbors since they don't come naturally. In a LAN we can have many but
	 * it is also possible that in the SEPP network itself nobody would be a
	 * natural neighbor. Therefore, we elect them among the peers with some
	 * specific criteria, for instance the peers with the shortest response
	 * time.
	 * 
	 * @return The ID of the destination of this connection.
	 */
    public String getPeerId() {
        return peerId;
    }

    /**
	 * Returns the time when this connection has been used the last time. The
	 * time is recorded if either data has been received or sent through this
	 * connection. It returns the time as long.
	 * 
	 * @return The time this connection has last been used as long value.
	 */
    public long getLastUsed() {
        return lastUsed;
    }

    /**
	 * Returns if this connection has been closed. This method returns true if
	 * the remote peer has sent the {@link CloseConnectionInfo} to indicate that
	 * he is closing his side of the connection and that we can't use it anymore
	 * to send data to him. Also if we close the connection and send the
	 * {@link CloseConnectionInfo} to the remote peer this method returns true.
	 * But usually the {@link #activeClose()} method is called right before the
	 * object itself is destroyed and therefore this method is not called
	 * thereafter.
	 * 
	 * @return True if the connection has been closed through the
	 *         {@link CloseConnectionInfo}.
	 */
    public boolean isClosed() {
        return closed;
    }

    /**
	 * Returns the network id of the connection or more precisely the Id of the
	 * network to which the connected peer is joined.
	 * 
	 * @return The network id as {@link String}.
	 */
    public String getNetworkId() {
        return networkId;
    }

    /**
	 * Sets the id of the network to which the connected peer is joined.
	 * 
	 * @param networkId
	 *            The network id as {@link String}.
	 */
    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    /**
	 * Returns true if this connection has been established successfully and is
	 * ready to be used. Otherwise false is returned.
	 * 
	 * @return A boolean indicating the status of this connection.
	 */
    public boolean isReady() {
        return ready;
    }

    /**
	 * Returns a {@link Peer} object for remote peer which is connected through
	 * this {@link DirectConnection} object. The peer object contains the remote
	 * address, port and the peer id.
	 * 
	 * @return The {@link Peer} object for the connected peer.
	 */
    public Peer getPeer() {
        return new Peer(remoteAddress, remotePort, peerId, false);
    }
}
