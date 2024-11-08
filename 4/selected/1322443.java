package org.sepp.connections;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.SocketChannel;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sepp.api.components.SecureSelfOrganizingNetwork;
import org.sepp.config.Peer;
import org.sepp.exceptions.SeppCommunicationException;
import org.sepp.messages.MessageQueue;
import org.sepp.messages.common.Message;
import org.sepp.routing.Route;
import org.sepp.services.PeerService;
import org.sepp.services.RoutingService;
import org.sepp.utils.Constants;

public class ConnectionManager implements Runnable {

    private Log log;

    /***************************************************************************
	 * Communication variables
	 **************************************************************************/
    private String localPeerId;

    private Vector directConnections = new Vector();

    private Vector createdConnections = new Vector();

    private SecureSelfOrganizingNetwork sson;

    private PeerService peerService;

    private RoutingService routingService;

    private MessageQueue sendQueue = new MessageQueue();

    private ChannelListener channelListener;

    public ConnectionManager(SecureSelfOrganizingNetwork sson) {
        log = LogFactory.getLog(this.getClass());
        this.sson = sson;
        this.localPeerId = sson.getPeerId();
        this.peerService = sson.getPeerService();
        this.routingService = sson.getRoutingService();
    }

    /**
	 * This method takes the provided message and stores it in the sending queue
	 * of this class. The items of the sending queue itself are periodically
	 * checked if they are ready to be sent and are sent if possible through the
	 * associated direct connection.
	 * 
	 * @param message
	 *            The message which should be sent.
	 */
    public void sendMessage(Message message) {
        synchronized (sendQueue) {
            sendQueue.add(message);
        }
    }

    /**
	 * Obtains the {@link DirectConnection} which is associated with the
	 * provided {@link SocketChannel}. If no association is found
	 * <code>null</code> is returned.
	 * 
	 * @param channel
	 *            The {@link SocketChannel} for which a {@link DirectConnection}
	 *            should be returned.
	 * @return The associated {@link DirectConnection} or null.
	 */
    public synchronized DirectConnection getConnection(SocketChannel channel) {
        for (int index = 0; index < directConnections.size(); index++) {
            DirectConnection connection = (DirectConnection) directConnections.get(index);
            if (connection.getChannel().equals(channel)) return connection;
        }
        for (int index = 0; index < createdConnections.size(); index++) {
            DirectConnection connection = (DirectConnection) createdConnections.get(index);
            if (connection.getChannel().equals(channel)) return connection;
        }
        return null;
    }

    public void run() {
        while (true) {
            send(sendQueue.getNext());
        }
    }

    /**
	 * Verify that the newly created direct connections are ready for usage and
	 * check for incoming messages on the established direct connections with
	 * our neighbors.
	 */
    public synchronized void checkConnections() {
        int index = 0;
        DirectConnection connection = null;
        for (index = 0; index < createdConnections.size(); index++) {
            connection = (DirectConnection) createdConnections.get(index);
            if (connection.isReady()) {
                directConnections.add(connection);
                peerService.addNeighbor(connection.getPeer());
                createdConnections.remove(index);
                index--;
                setReady(connection.getPeerId());
            } else if (connection.isClosed()) {
                createdConnections.remove(index);
                index--;
            }
        }
        for (index = 0; index < directConnections.size(); index++) {
            connection = (DirectConnection) directConnections.get(index);
            if (connection.getLastUsed() + Constants.CONNECTION_TIMEOUT < System.currentTimeMillis()) {
                connection.activeClose();
                directConnections.remove(connection);
                log.debug("DirectConnection with peer " + connection.getPeerId() + " has timed out and been removed.");
            } else if (connection.isClosed()) {
                connection.passiveClose();
                directConnections.remove(connection);
                log.debug("DirectConnection with peer " + connection.getPeerId() + " has been closed and removed.");
            }
        }
    }

    /**
	 * Checks if messages are available for sending from the outgoing queue and
	 * obtains a direct connection to send the available message. If either no
	 * connection for a distinct destination could be obtained or during the
	 * sending process an error has occurred, all the messages destined to this
	 * peer are set to the status not ready.
	 */
    private void send(Message message) {
        if (message.getDestination().equalsIgnoreCase(Constants.ALL_PEERS)) {
            for (int index = 0; index < directConnections.size(); index++) send((DirectConnection) directConnections.get(index), message);
        } else {
            send(getConnection(message), message);
        }
    }

    /**
	 * Actually sends the message over the provided connection and performs some
	 * exception handling if necessary. A MAC is applied to each application
	 * message.
	 * 
	 * @param connection
	 *            The connection over which we want to send data.
	 * @param message
	 *            The message which should be sent.
	 */
    private void send(DirectConnection connection, Message message) {
        try {
            if (message.getType() > PeerService.type) sson.createMessageAuthentication(message);
            if (connection instanceof DirectConnection) connection.sendMessage(message); else throw new SeppCommunicationException("Couldn't send message because no connection available.");
        } catch (SeppCommunicationException e) {
            log.error("Error sending message. Reason: " + e.getMessage());
            if (routingService.isRouted(message)) {
                if (sendQueue.isReady(message.getDestination())) {
                    setNotReady(message.getDestination());
                    if (routingService.isSourceRoutingAlgorithm()) routingService.routeError(message, message.getRoute().getNextHop()); else routingService.routeError(message, routingService.getNextHop(message.getDestination()));
                }
                if (message.getSource().equalsIgnoreCase(localPeerId)) sendQueue.add(message);
            }
            if (connection instanceof DirectConnection) {
                deleteConnection(connection.getPeerId());
            }
        }
    }

    /**
	 * Obtains a {@link DirectConnection} object for the provided destination.
	 * This must not mean that the connection object's end point is the
	 * destination but that the end point is at least the next hop on the path
	 * to the destination.
	 * 
	 * @param destination
	 *            The destination for which a connection should be obtained.
	 * @return The {@link DirectConnection} for the specified message.
	 */
    private DirectConnection getConnection(Message message) {
        DirectConnection connection = null;
        String neighbor = null;
        Route route = null;
        if (routingService.isSourceRoutingAlgorithm()) {
            route = message.getRoute();
            if (route instanceof Route) {
                neighbor = route.getNextHop();
            } else {
                if (message.getSource().equalsIgnoreCase(localPeerId)) {
                    route = routingService.getRoute(message.getDestination());
                    if (route instanceof Route) {
                        message.setRoute(route);
                        neighbor = route.getNextHop();
                    }
                } else {
                    log.error("Message without source route can't be forwarded.");
                }
            }
        } else {
            neighbor = routingService.getNextHop(message.getDestination());
        }
        if (neighbor instanceof String) {
            connection = getConnection(neighbor);
            if (!(connection instanceof DirectConnection)) setNotReady(message.getDestination());
        } else {
            setNotReady(message.getDestination());
            log.error("No neighbor found to forward the message to peer " + message.getDestination());
        }
        return connection;
    }

    /**
	 * Creates a {@link DirectConnection} object from the provided socket. The
	 * socket has been created from the {@link ServerSocket#accept()} method.
	 * 
	 * @param channel
	 *            The incoming socket which should be used for the new
	 *            connection.
	 */
    public void createConnection(SocketChannel channel) {
        DirectConnection connection = null;
        try {
            connection = new DirectConnection(channel, sson.getNetworkId());
            synchronized (directConnections) {
                createdConnections.add(connection);
            }
        } catch (SeppCommunicationException e) {
            log.error("Couldn't create DirectConnection from socket. Reason: " + e.getMessage());
        }
    }

    /**
	 * Removes the {@link DirectConnection} to the specified peer. Only if the
	 * specified peer is an endpoint of a {@link DirectConnection} and therefore
	 * a neighbor of this peer a {@link DirectConnection} actually exists and
	 * can therefore be deleted. Otherwise, nothing is done.
	 * 
	 * @param peerId
	 *            The peer to whom the connection should be closed.
	 */
    public void removeConnection(SocketChannel channel) {
        DirectConnection connection = getConnection(channel);
        if (connection instanceof DirectConnection) {
            deleteConnection(connection.peerId);
        } else {
            try {
                channel.close();
            } catch (IOException e) {
                log.error("Couldn't close connection. Reason: " + e.getMessage());
            }
        }
    }

    /**
	 * Removes the {@link DirectConnection} to the specified peer. Only if the
	 * specified peer is an endpoint of a {@link DirectConnection} and therefore
	 * a neighbor of this peer a {@link DirectConnection} actually exists and
	 * can therefore be deleted. Otherwise, nothing is done.
	 * 
	 * @param peerId
	 *            The peer to whom the connection should be closed.
	 */
    public synchronized void deleteConnection(String peerId) {
        DirectConnection connection = null;
        for (int index = 0; index < directConnections.size(); index++) {
            connection = (DirectConnection) directConnections.get(index);
            if (connection.getPeerId().equalsIgnoreCase(peerId)) {
                connection.activeClose();
                directConnections.remove(index);
                connection = null;
                break;
            }
        }
    }

    /**
	 * Sets the specified destination peer to be ready to receive messages.
	 * Therefore, all the messages in the sending queue are no ready to be sent
	 * during the next periodic sending process.
	 * 
	 * @param peerId
	 *            The peer which is now ready to receive messages.
	 */
    public synchronized void setReady(String peerId) {
        sendQueue.setReady(peerId);
    }

    /**
	 * Sets the specified destination peer to be not ready to receive messages.
	 * Therefore, all the messages in the sending queue are set to not ready and
	 * will not be considered during the next periodic sending process.
	 * 
	 * @param peerId
	 *            The destination which is not ready to receive messages.
	 */
    public synchronized void setNotReady(String peerId) {
        sendQueue.setNotReady(peerId);
    }

    /**
	 * Sets the membership of the peer in the associated connection object. Each
	 * neighbor is connected through a {@link DirectConnection} object. Since
	 * not all neighbors must be in the same network all the time we need to
	 * specify the network to which they and their associated connection
	 * belongs. Because after one is joined to a distinct SEPP network
	 * broadcasts which are intended for this network must not be forwarded to
	 * others.
	 * 
	 * @param peerId
	 *            The neighbor for which the membership status of its associated
	 *            {@link DirectConnection} object should be set to the provided
	 *            network ID.
	 * @param networkId
	 *            The ID of the network to which the specified neighbor belongs.
	 */
    public synchronized void setNetworkForNeighbor(String peerId, String networkId) {
        DirectConnection connection = null;
        for (int index = 0; index < directConnections.size(); index++) {
            connection = (DirectConnection) directConnections.get(index);
            if (connection.getPeerId().equalsIgnoreCase(peerId)) {
                connection.setNetworkId(networkId);
                break;
            }
        }
    }

    /**
	 * Adds the channel listener which should be used for new connections. The
	 * method {@link ChannelListener#addChannel(SocketChannel)} will be called
	 * after a new connection has been initiated by this peer.
	 * 
	 * @param channelListener
	 *            The {@link ChannelListener} which should be used for new
	 *            connections.
	 */
    public void setChannelListener(ChannelListener channelListener) {
        this.channelListener = channelListener;
    }

    /**
	 * Obtains a {@link DirectConnection} for the specified neighbor if already
	 * available or creates a new connection if correct address information has
	 * been specified for this neighbor.
	 * 
	 * @param neighbor
	 *            The neighbor to which a connection should be established.
	 * @return The obtained {@link DirectConnection} object.
	 */
    private DirectConnection getConnection(String neighbor) {
        DirectConnection connection = null;
        for (int index = 0; index < directConnections.size(); index++) {
            connection = (DirectConnection) directConnections.get(index);
            if (connection.getPeerId().equalsIgnoreCase(neighbor)) return connection;
            connection = null;
        }
        Peer peer = peerService.getNeighbor(neighbor);
        if (peer instanceof Peer) {
            try {
                connection = new DirectConnection(peer.getIp(), peer.getPort(), peer.getId(), sson.getNetworkId());
                synchronized (directConnections) {
                    directConnections.add(connection);
                }
                channelListener.addChannel(connection.getChannel());
                log.debug("Created a direct connection with peer " + peer.getId());
            } catch (SeppCommunicationException e) {
                log.debug(e.getMessage());
            }
        }
        if (!(connection instanceof DirectConnection)) {
            peerService.setNeighborStatus(neighbor, false);
        }
        return connection;
    }
}
