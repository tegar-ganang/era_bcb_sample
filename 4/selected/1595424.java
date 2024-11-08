package ch.comtools.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @author Roger Dudler <roger.dudler@gmail.com>
 * @since 1.0
 * @version $Id$
 */
public class SSHConnection {

    public static final int SSH_EXTENDED_DATA_STDERR = 1;

    public static final int SSH_DISCONNECT_HOST_NOT_ALLOWED_TO_CONNECT = 1;

    public static final int SSH_DISCONNECT_PROTOCOL_ERROR = 2;

    public static final int SSH_DISCONNECT_KEY_EXCHANGE_FAILED = 3;

    public static final int SSH_DISCONNECT_RESERVED = 4;

    public static final int SSH_DISCONNECT_MAC_ERROR = 5;

    public static final int SSH_DISCONNECT_COMPRESSION_ERROR = 6;

    public static final int SSH_DISCONNECT_SERVICE_NOT_AVAILABLE = 7;

    public static final int SSH_DISCONNECT_PROTOCOL_VERSION_NOT_SUPPORTED = 8;

    public static final int SSH_DISCONNECT_HOST_KEY_NOT_VERIFIABLE = 9;

    public static final int SSH_DISCONNECT_CONNECTION_LOST = 10;

    public static final int SSH_DISCONNECT_BY_APPLICATION = 11;

    public static final int SSH_DISCONNECT_TOO_MANY_CONNECTIONS = 12;

    public static final int SSH_DISCONNECT_AUTH_CANCELLED_BY_USER = 13;

    public static final int SSH_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE = 14;

    public static final int SSH_DISCONNECT_ILLEGAL_USER_NAME = 15;

    public static final int SSH_OPEN_ADMINISTRATIVELY_PROHIBITED = 1;

    public static final int SSH_OPEN_CONNECT_FAILED = 2;

    public static final int SSH_OPEN_UNKNOWN_CHANNEL_TYPE = 3;

    public static final int SSH_OPEN_RESOURCE_SHORTAGE = 4;

    private String host;

    private int port;

    private SocketChannel channel;

    /**
	 * Creates a new {@link SSHConnection} instance.
	 */
    public SSHConnection() {
    }

    /**
	 * Creates a new {@link SSHConnection} instance using a given host address 
	 * and a port. Use {@link #connect()} before using this {@link SSHConnection}.
	 * @param host host to connect to
	 * @param port port where socket server is listening to
	 */
    public SSHConnection(String host, int port) {
        this.setHost(host);
        this.setPort(port);
    }

    /**
	 * Connect to an SSH server.
	 * A {@link SocketChannel} will be created.
	 */
    public synchronized void connect() throws IOException {
        this.channel = this.createSocketChannel(this.host, this.port);
    }

    /**
     * Creates a new {@link SocketChannel}, which is already connected and
     * ready to use. Do not forget to close the channel using it's close
     * method.
     * @param host host to connect to
     * @param port port where socket server is listening to
     * @return {@link SocketChannel}
     * @throws IOException
     */
    private SocketChannel createSocketChannel(String host, int port) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(host, port));
        return channel;
    }

    /**
     * Close this {@link SSHConnection} and the underlying {@link SocketChannel}. Be
     * sure to call this method after using this {@link SSHConnection}.
     * @throws IOException 
     */
    public void close() throws IOException {
        this.channel.close();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public SocketChannel getChannel() {
        return channel;
    }
}
