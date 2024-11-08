package CADI.Client.Network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketException;

/**
 * This class inherits the {@link java.net.Socket} adding new useful methods
 * and other ones has been overrided and adapted to new requirements.
 * 
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0   2007-2012/10/27
 */
public class ClientSocket extends Socket {

    /**
	 * Contains the JPIP server name.
	 */
    private String server = "";

    /**
	 * It is the server port where request must be sent.
	 */
    private int port = -1;

    /**
	 * The default port.
	 */
    public static final int DEFAULT_PORT = 80;

    /**
	 * 
	 */
    private int timeout = 0;

    /**
	 * 
	 */
    private int DEFAULT_TIMEOUT = 5000;

    /**
	 * 
	 */
    private boolean connected = false;

    /**
	 * Opens a connection between the client and the server. This connection is
	 * kept opened until the {@link #close()} method is called.
	 * 
	 * @param	server		the server name.
	 *
	 * @throws	IOException	an IOException will be thrown is the connection
	 * 							can not be opened.
	 */
    public void connect(String server) throws IOException {
        connect(server, DEFAULT_PORT, DEFAULT_TIMEOUT);
    }

    /**
	 * Opens a connection between the client and the server. This connection is
	 * kept opened until the {@link #close()} method is called.
	 * 
	 * @param	server
	 * @param	port
	 * 
	 * @throws	IOException an IOException will be thrown is the connection
	 * 							can not be opened.
	 */
    public void connect(String server, int port) throws IOException {
        connect(server, port, DEFAULT_TIMEOUT);
    }

    /**
	 * Opens a connection between the client and the server. This connection is
	 * kept opened until the {@link #close()} method is called.
	 * 
	 * @param server
	 * @param port
	 * @param timeout
	 * 
	 * @throws	IOException an IOException will be thrown is the connection
	 * 							can not be opened.
	 */
    public void connect(String server, int port, int timeout) throws IOException {
        if (server == null) {
            throw new ProtocolException("Invalid remote host name");
        }
        if (port <= 0) {
            throw new ProtocolException("Invalid port number");
        }
        if (timeout < 0) {
            throw new ProtocolException("Invalid socket timeout");
        }
        this.server = server;
        this.port = port;
        this.timeout = timeout;
        InetAddress addr = InetAddress.getByName(server);
        InetSocketAddress sockaddr = new InetSocketAddress(addr, port);
        try {
            super.connect(sockaddr, this.timeout);
        } catch (IOException ioe) {
            throw new IOException("There is not link to server " + server);
        }
        connected = true;
    }

    /**
	 * Reconnects the socket using the last server, port, and timeout.
	 */
    public void reconnect() throws IOException {
        connect(server, port, timeout);
    }

    @Override
    public void close() {
        connected = false;
        try {
            super.close();
        } catch (Exception e) {
        }
    }

    @Override
    public boolean isConnected() {
        return (connected ? super.isConnected() : false);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (isClosed() || !isConnected()) {
            throw new IOException();
        }
        return super.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (isClosed() || !isConnected()) {
            throw new IOException();
        }
        return super.getOutputStream();
    }

    /**
	 * Returns the name of the endpoint this client is connected to, or null if
	 * it is unconnected.
	 * 
	 * @return the name of the remote host.
	 */
    public String getRemoteHost() {
        return server;
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        this.timeout = timeout;
        super.setSoTimeout(timeout);
    }

    /**
	 * For debugging purposes. It shows all socket information.
	 */
    public String toString() {
        String str = "";
        str = getClass().getName() + " [\n";
        try {
            str += "-------------------------" + "\n";
            str += "   Socket Information    " + "\n";
            str += "-------------------------" + "\n";
            str += "socket               : " + this + "\n";
            str += "Keep Alive           : " + getKeepAlive() + "\n";
            str += "Receive Buffer Size  : " + getReceiveBufferSize() + "\n";
            str += "Send Buffer Size     : " + getSendBufferSize() + "\n";
            str += "Is Socket Bound?     : " + isBound() + "\n";
            str += "Is Socket Connected? : " + isConnected() + "\n";
            str += "Is Socket Closed?    : " + isClosed() + "\n";
            str += "So Timeout           : " + getSoTimeout() + "\n";
            str += "So Linger            : " + getSoLinger() + "\n";
            str += "TCP No Delay         : " + getTcpNoDelay() + "\n";
            str += "Traffic Class        : " + getTrafficClass() + "\n";
            str += "Socket Channel       : " + getChannel() + "\n";
            str += "Reuse Address?       : " + getReuseAddress() + "\n";
            str += "\n\n";
            InetAddress inetAddrServer = getInetAddress();
            str += "---------------------------" + "\n";
            str += "Remote (Server) Information" + "\n";
            str += "---------------------------" + "\n";
            str += "InetAddress - (Structure) : " + inetAddrServer + "\n";
            str += "Socket Address - (Remote) : " + getRemoteSocketAddress() + "\n";
            str += "Canonical Name            : " + inetAddrServer.getCanonicalHostName() + "\n";
            str += "Host Name                 : " + inetAddrServer.getHostName() + "\n";
            str += "Host Address              : " + inetAddrServer.getHostAddress() + "\n";
            str += "Port                      : " + getPort();
            str += "RAW IP Address - (byte[]) : ";
            byte[] b1 = inetAddrServer.getAddress();
            for (int i = 0; i < b1.length; i++) {
                if (i > 0) {
                    str += ".";
                }
                str += b1[i] & 0xff;
            }
            str += "\n";
            str += "Is Loopback Address?      : " + inetAddrServer.isLoopbackAddress() + "\n";
            str += "Is Multicast Address?     : " + inetAddrServer.isMulticastAddress() + "\n";
            str += "\n\n";
            InetAddress inetAddrClient = getLocalAddress();
            str += "--------------------------" + "\n";
            str += "Local (Client) Information" + "\n";
            str += "--------------------------" + "\n";
            str += "InetAddress - (Structure) : " + inetAddrClient + "\n";
            str += "Socket Address - (Local)  : " + getLocalSocketAddress() + "\n";
            str += "Canonical Name            : " + inetAddrClient.getCanonicalHostName() + "\n";
            str += "Host Name                 : " + inetAddrClient.getHostName() + "\n";
            str += "Host Address              : " + inetAddrClient.getHostAddress() + "\n";
            str += "Port                      : " + getLocalPort() + "\n";
            str += "RAW IP Address - (byte[]) : ";
            byte[] b2 = inetAddrClient.getAddress();
            for (int i = 0; i < b2.length; i++) {
                if (i > 0) {
                    str += ".";
                }
                str += b2[i] & 0xff;
            }
            str += "\n";
            str += "Is Loopback Address?      : " + inetAddrClient.isLoopbackAddress() + "\n";
            str += "Is Multicast Address?     : " + inetAddrClient.isMulticastAddress() + "\n";
            str += "\n";
            str += "]";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
	 * Prints this ClientSocket out to the specified output stream. This method
	 * is useful for debugging.
	 * 
	 * @param out
	 *           an output stream.
	 */
    public void list(PrintStream out) {
        out.println("-- ClientSocket --");
        try {
            out.println("Keep Alive           : " + getKeepAlive());
            out.println("Receive Buffer Size  : " + getReceiveBufferSize());
            out.println("Send Buffer Size     : " + getSendBufferSize());
            out.println("Is Socket Bound?     : " + isBound());
            out.println("Is Socket Connected? : " + isConnected());
            out.println("Is Socket Closed?    : " + isClosed());
            out.println("So Timeout           : " + getSoTimeout());
            out.println("So Linger            : " + getSoLinger());
            out.println("TCP No Delay         : " + getTcpNoDelay());
            out.println("Traffic Class        : " + getTrafficClass());
            out.println("Socket Channel       : " + getChannel());
            out.println("Reuse Address?       : " + getReuseAddress());
            out.println("\n");
            InetAddress inetAddrServer = getInetAddress();
            out.println("---------------------------");
            out.println("Remote (Server) Information");
            out.println("---------------------------");
            out.println("InetAddress - (Structure) : " + inetAddrServer);
            out.println("Socket Address - (Remote) : " + getRemoteSocketAddress());
            out.println("Canonical Name            : " + inetAddrServer.getCanonicalHostName());
            out.println("Host Name                 : " + inetAddrServer.getHostName());
            out.println("Host Address              : " + inetAddrServer.getHostAddress());
            out.println("Port                      : " + getPort());
            out.print("RAW IP Address - (byte[]) : ");
            byte[] b1 = inetAddrServer.getAddress();
            for (int i = 0; i < b1.length; i++) {
                if (i > 0) {
                    out.print(".");
                }
                out.print(b1[i] & 0xff);
            }
            out.println("\n");
            out.println("Is Loopback Address?      : " + inetAddrServer.isLoopbackAddress());
            out.println("Is Multicast Address?     : " + inetAddrServer.isMulticastAddress());
            out.println("\n");
            InetAddress inetAddrClient = getLocalAddress();
            out.println("--------------------------");
            out.println("Local (Client) Information");
            out.println("--------------------------");
            out.println("InetAddress - (Structure) : " + inetAddrClient);
            out.println("Socket Address - (Local)  : " + getLocalSocketAddress());
            out.println("Canonical Name            : " + inetAddrClient.getCanonicalHostName());
            out.println("Host Name                 : " + inetAddrClient.getHostName());
            out.println("Host Address              : " + inetAddrClient.getHostAddress());
            out.println("Port                      : " + getLocalPort());
            out.println("RAW IP Address - (byte[]) : ");
            byte[] b2 = inetAddrClient.getAddress();
            for (int i = 0; i < b2.length; i++) {
                if (i > 0) {
                    out.print(".");
                }
                out.print(b2[i] & 0xff);
            }
            out.println("\n");
            out.println("Is Loopback Address?      : " + inetAddrClient.isLoopbackAddress());
            out.println("Is Multicast Address?     : " + inetAddrClient.isMulticastAddress());
            out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
        out.flush();
    }
}
