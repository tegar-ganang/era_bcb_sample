package jist.swans.trans;

import jist.swans.net.NetAddress;
import jist.swans.misc.Message;
import jist.swans.trans.TransTcp.TcpMessage;
import jist.swans.Constants;
import jist.runtime.JistAPI;
import jist.runtime.Channel;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.net.SocketImplFactory;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.InetSocketAddress;

/**
 * SWANS Implementation of Server Socket entity.
 *
 * @author Kelwin Tamtoro &lt;kt222@cs.cornell.edu&gt;
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: TcpServerSocket.java,v 1.1 2006/10/21 00:04:10 lmottola Exp $
 * @since SWANS1.0
 */
public class TcpServerSocket implements SocketInterface.TcpServerSocketInterface {

    /**
   * Entity reference to TcpServerSocket.
   */
    private SocketInterface.TcpServerSocketInterface self;

    /** Local port. */
    private int lport;

    /** Local address. */
    private InetAddress laddr;

    /** Channel (for blocking implementation). */
    private Channel channel;

    /** Reference to the server socket's callback. */
    private TransInterface.SocketHandler.TcpHandler callback;

    /** Entity reference to transport layer. */
    private TransInterface.TransTcpInterface tcpEntity;

    /** The bind state of the server socket. */
    private boolean isBound;

    /** The state of the socket (true if socket is closed). */
    private boolean isClosed;

    /** Backlog. */
    private int backlog;

    /** Indicator if socket is to be bound when created. */
    private boolean bindInConstructor;

    /** Current state of the socket. */
    private int currentState;

    /**
   * Create an entity reference to itself.
   */
    public void createProxy() {
        self = (SocketInterface.TcpServerSocketInterface) JistAPI.proxy(this, SocketInterface.TcpServerSocketInterface.class);
    }

    /**
   * Returns the entity reference to the server socket itself.
   *
   * @return Entity reference to TcpServerSocket
   */
    public SocketInterface.TcpServerSocketInterface getProxy() {
        return self;
    }

    /** {@inheritDoc} */
    public void setTcpEntity(TransInterface.TransTcpInterface tcpEntity) {
        this.tcpEntity = tcpEntity;
    }

    /**
   * Creates an unbound server socket.
   */
    public TcpServerSocket() {
        initializeAll(null, 0, 50, false);
    }

    /**
   * Creates a server socket, bound to the specified port.
   *
   * @param port the port number, or 0 to use any free port.
   */
    public TcpServerSocket(int port) {
        this(port, 50, null);
    }

    /**
   * Creates a server socket and binds it to the specified 
   * local port number, with the specified backlog.
   *
   * @param port the specified port, or 0 to use any free port.
   * @param backlog the maximum length of the queue. 
   */
    public TcpServerSocket(int port, int backlog) {
        this(port, backlog, null);
    }

    /**
   * Creates a server with the specified port, listen backlog, 
   * and local IP address to bind to.
   *
   * @param port the local TCP port
   * @param backlog the listen backlog
   * @param bindAddr the local InetAddress the server will bind to 
   */
    public TcpServerSocket(int port, int backlog, InetAddress bindAddr) {
        initializeAll(bindAddr, port, backlog, true);
    }

    /** {@inheritDoc} */
    public void _jistPostInit() {
        if (bindInConstructor) {
            bind(new InetSocketAddress(laddr, lport), backlog);
        }
    }

    /** {@inheritDoc} */
    public TcpSocket accept() {
        currentState = Constants.TCPSTATES.LISTEN;
        TcpSocket ts = (TcpSocket) channel.receive();
        ts.establishingConnection();
        return ts;
    }

    /** {@inheritDoc} */
    public void bind(SocketAddress bindpoint) {
        bind(new InetSocketAddress(laddr, lport), 50);
    }

    /** {@inheritDoc} */
    public void bind(SocketAddress endpoint, int backlog) {
        InetSocketAddress inetAddr = (InetSocketAddress) endpoint;
        this.lport = inetAddr.getPort();
        this.laddr = inetAddr.getAddress();
        this.backlog = backlog;
        if (isBound) {
            tcpEntity.delSocketHandler(lport);
        }
        tcpEntity.addSocketHandler(this.lport, callback);
        isBound = true;
    }

    /** {@inheritDoc} */
    public void close() {
        if (isBound) {
            tcpEntity.delSocketHandler(lport);
            isBound = false;
        }
        isClosed = true;
    }

    /** {@inheritDoc} */
    public ServerSocketChannel getChannel() {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public InetAddress getInetAddress() {
        return laddr;
    }

    /** {@inheritDoc} */
    public int getLocalPort() {
        return lport;
    }

    /** {@inheritDoc} */
    public SocketAddress getLocalSocketAddress() {
        return isBound ? new InetSocketAddress(laddr, lport) : null;
    }

    /** {@inheritDoc} */
    public int getReceiveBufferSize() {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public boolean getReuseAddress() {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public int getSoTimeout() {
        throw new RuntimeException("not implemented");
    }

    /**
   * Subclasses of ServerSocket use this method to override 
   * accept() to return their own subclass of socket.
   *
   * @param s the Socket
   */
    protected void implAccept(Socket s) {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public boolean isBound() {
        return isBound;
    }

    /** {@inheritDoc} */
    public boolean isClosed() {
        return isClosed;
    }

    /** {@inheritDoc} */
    public void setReceiveBufferSize(int size) {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public void setReuseAddress(boolean on) {
        throw new RuntimeException("not implemented");
    }

    /**
   * Sets the server socket implementation factory for the application.
   *
   * @param fac the desired factory.
   */
    public static void setSocketFactory(SocketImplFactory fac) {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public void setSoTimeout(int timeout) {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public String toString() {
        return "TcpServerSocket:(" + laddr + ":" + lport + ")";
    }

    /**
   * Initializes all of the server socket variables.
   *
   * @param lAddr local address
   * @param lPort local port
   * @param backlog listen backlog length
   * @param doBind true if socket is to be bound when created
   */
    private void initializeAll(InetAddress lAddr, int lPort, int backlog, boolean doBind) {
        createProxy();
        this.lport = lPort;
        this.laddr = lAddr;
        this.backlog = backlog;
        this.channel = JistAPI.createChannel();
        this.callback = new TcpServerSocketCallback(getProxy());
        isBound = false;
        isClosed = false;
        currentState = Constants.TCPSTATES.CLOSED;
        bindInConstructor = doBind;
    }

    /** {@inheritDoc} */
    public void checkPacketandState(TcpMessage msg, NetAddress src) {
        if (TcpSocket.PRINTOUT >= TcpSocket.TCP_DEBUG) {
            System.out.println("TcpServerSocket::checkPacketandState: " + msg);
        }
        switch(currentState) {
            case Constants.TCPSTATES.LISTEN:
                if (msg.getSYN() && !msg.getACK()) {
                    TcpSocket newSocket = new TcpSocket(this.tcpEntity, src.getIP(), msg.getSrcPort(), laddr, (short) 0, msg.getSeqNum(), msg.getWindowSize());
                    newSocket.bind(new InetSocketAddress(laddr, 0));
                    newSocket.sendSYNACKPacket();
                    channel.sendNonBlock(newSocket, true, false);
                }
                break;
            default:
                break;
        }
    }

    /** 
   * Method to get a random sequence number.
   *
   * @return random number less than 1000000
   */
    public static int getRandomSequenceNumber() {
        return (Math.abs(Constants.random.nextInt()) % 1000000);
    }

    /**
   * Implementation of Socket Callback for TcpServerSocket.
   */
    public static class TcpServerSocketCallback implements TransInterface.SocketHandler.TcpHandler {

        /** Entity reference to TcpServerSocket. */
        private SocketInterface.TcpServerSocketInterface serverSocketEntity;

        /**
     * Constructor.
     *
     * @param entity the entity reference to TcpServerSocket
     */
        public TcpServerSocketCallback(SocketInterface.TcpServerSocketInterface entity) {
            serverSocketEntity = entity;
        }

        /** {@inheritDoc} */
        public void receive(Message msg, NetAddress src, int srcPort) {
            serverSocketEntity.checkPacketandState((TcpMessage) msg, src);
        }
    }
}
