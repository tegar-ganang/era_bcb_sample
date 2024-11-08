package br.unb.unbiquitous.ubiquitos.network.loopback;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import br.unb.unbiquitous.ubiquitos.network.exceptions.NetworkException;
import br.unb.unbiquitous.ubiquitos.network.loopback.connection.LoopbackClientConnection;
import br.unb.unbiquitous.ubiquitos.network.loopback.connection.LoopbackServerConnection;

/**
 * 
 * This class is the manager of the connection's channels, providing static methods to manage
 * and control the loopback connections. Thus, it represents a channel between two devices.
 * 
 * @author Lucas Paranhos Quintella
 *
 */
public class LoopbackChannel {

    /** The set of the opened channels established in the middleware */
    private static Set<LoopbackChannel> openedChannels;

    /** The blocked threads vector of the servers waiting for connection */
    private static Vector<LoopbackServerConnection> waitingServers;

    /** The buffers shared by the two devices of the channel */
    private Vector<Byte> input;

    private Vector<Byte> output;

    /** The ID of the channel. It is the same of the server who established the connection */
    private long channelId;

    /** The ID of the devices */
    private long clientSideClientId;

    private long serverSideClientId;

    /**
	 * Add a new server as waiting for a connection. The thread which
	 * made the call should block by itself. This method will not block. 
	 * 
	 * @param serverConnection The server that will be waiting for new connections.
	 */
    public static synchronized void addWaitingServer(LoopbackServerConnection serverConnection) {
        waitingServers.add(serverConnection);
    }

    /**
	 * Clears all still opened channels and connections
	 */
    public static synchronized void tearDown() {
        LoopbackChannel[] channels = new LoopbackChannel[openedChannels.size()];
        int i = 0;
        for (LoopbackChannel channel : openedChannels) {
            channels[i++] = channel;
        }
        for (int j = 0; j < channels.length; j++) {
            channels[j].close();
        }
        openedChannels.clear();
        for (LoopbackServerConnection server : waitingServers) {
            try {
                server.closeConnection();
            } catch (IOException e) {
            }
        }
        waitingServers.clear();
    }

    /**
	 * Initializes the channel for opening new channels and connections. This should be called
	 * before using any static methods from the LoopbackChannel.
	 */
    public static synchronized void initChannel() {
        openedChannels = new HashSet<LoopbackChannel>();
        waitingServers = new Vector<LoopbackServerConnection>();
    }

    /**
	 * This method establishes a connection and a new channel is created between
	 * the waiting server with the given ID and the given client connection.
	 * 
	 * @param clientConnection The client of the connection to be established.
	 * @param serverId The ID of the server in which the connection will be established.
	 */
    public static synchronized void establishConnection(LoopbackClientConnection clientConnection, long serverId) throws NetworkException {
        LoopbackServerConnection serverConnection = null;
        for (LoopbackServerConnection server : waitingServers) {
            if (server.getDeviceId() == serverId) {
                serverConnection = server;
                waitingServers.remove(server);
                break;
            }
        }
        if (serverConnection == null) {
            throw new NetworkException("LoopbackChannel : There's no such server to establish the connection");
        }
        LoopbackDevice clientDevice = new LoopbackDevice(serverId);
        LoopbackClientConnection clientServerSide = new LoopbackClientConnection(clientDevice);
        LoopbackChannel newChannel = new LoopbackChannel(clientConnection.getDeviceId(), clientServerSide.getDeviceId(), serverId);
        clientServerSide.setChannel(newChannel);
        clientConnection.setChannel(newChannel);
        synchronized (serverConnection) {
            serverConnection.setConnectedClient(clientServerSide);
            serverConnection.notify();
        }
    }

    /**
	 * Makes the given server to stop waiting for connections. If the given server is not 
	 * listening to connections, no changes are done.
	 * 
	 * @param server The waiting server.
	 */
    public static synchronized void removeWaitingServer(LoopbackServerConnection server) {
        for (LoopbackServerConnection waitingServer : waitingServers) {
            if (waitingServer.getDeviceId() == server.getDeviceId()) {
                synchronized (waitingServer) {
                    waitingServers.remove(waitingServer);
                    waitingServer.setAsClosed();
                    waitingServer.notify();
                }
                break;
            }
        }
    }

    /**
	 * Constructor of the channel. Each established connection has its own channel and two 
	 * corresponding communicating parts, the LoopbackDevices.
	 * 
	 * @param clientSideClientId The device of the client side of connection
	 * @param serverSideClientId The device of the server side of the connection
	 * @param serverId The ID of the waiting server
	 */
    public LoopbackChannel(long clientSideClientId, long serverSideClientId, long serverId) {
        this.channelId = serverId;
        this.input = new Vector<Byte>();
        this.output = new Vector<Byte>();
        this.clientSideClientId = clientSideClientId;
        this.serverSideClientId = serverSideClientId;
        synchronized (openedChannels) {
            openedChannels.add(this);
        }
    }

    /**
	 * Instantiates a new LoopbackInputStream based on the given ID.
	 * 
	 * @param clientId The ID of the device.
	 * @return The InputStream relative to the given device.
	 */
    public LoopbackInputStream getInputStream(long clientId) {
        if (clientId == this.clientSideClientId) {
            return new LoopbackInputStream(this.input);
        } else if (clientId == this.serverSideClientId) {
            return new LoopbackInputStream(this.output);
        } else {
            throw new RuntimeException("Invalid clientId");
        }
    }

    /**
	 * Instantiates a new LoopbackOutputStream based on the given ID.
	 * 
	 * @param clientId The ID of the device.
	 * @return The OutputStream relative to the given device.
	 */
    public LoopbackOutputStream getOutputStream(long clientId) {
        if (clientId == this.clientSideClientId) {
            return new LoopbackOutputStream(this.output);
        } else if (clientId == this.serverSideClientId) {
            return new LoopbackOutputStream(this.input);
        } else {
            throw new RuntimeException("Invalid clientId");
        }
    }

    /**
	 * Getter of the channel ID.
	 * 
	 * @return The ID of this channel.
	 */
    public long getChannelId() {
        return this.channelId;
    }

    /**
	 * Closes the channel of the established connection.
	 */
    public void close() {
        synchronized (openedChannels) {
            for (LoopbackChannel channel : openedChannels) {
                if (channel.getChannelId() == this.channelId) {
                    openedChannels.remove(channel);
                    break;
                }
            }
        }
        this.input.clear();
        this.output.clear();
    }
}
