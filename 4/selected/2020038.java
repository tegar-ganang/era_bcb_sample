package jist.swans.trans;

import jist.swans.net.NetAddress;
import jist.swans.misc.Message;
import jist.swans.misc.MessageBytes;
import jist.swans.trans.TransTcp.TcpMessage;
import jist.swans.Constants;
import jist.runtime.JistAPI;
import jist.runtime.Channel;
import java.nio.channels.SocketChannel;
import java.net.SocketImplFactory;
import java.net.SocketImpl;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import jist.swans.app.io.InputStream;
import jist.swans.app.io.OutputStream;
import java.io.IOException;

/**
 * SWANS Implementation of Socket entity.
 *
 * @author Kelwin Tamtoro &lt;kt222@cs.cornell.edu&gt;
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: TcpSocket.java,v 1.1 2006/10/21 00:04:10 lmottola Exp $
 * @since SWANS1.0
 */
public class TcpSocket implements SocketInterface.TcpSocketInterface {

    /**
   * Debug printing flag.
   * OFF = no debug output
   */
    public static final int OFF = 0;

    /**
   * Debug printing flag.
   * INFO = minimum debug output
   */
    public static final int INFO = 1;

    /**
   * Debug printing flag.
   * TCP_DEBUG = debug output, showing only packet traffic
   */
    public static final int TCP_DEBUG = 2;

    /**
   * Debug printing flag.
   * FULL_DEBUG = full debug output
   */
    public static final int FULL_DEBUG = 3;

    /**
   * Indicator for printing debug outputs.
   */
    public static final int PRINTOUT = OFF;

    /**
   * Maximum Segment Size.
   * Number of bytes to transfer in one packet.
   */
    public static final int MSS = 536;

    /**
   * Maximum Segment Lifetime.
   * Maximum amount of time any segment can exist in the network before being discarded.
   * (Can be 30 seconds, 1 minute, or 2 minutes)
   * (TCP has to wait for 2*MSL before the socket pair (client IP address, client port
   * number, server IP address, and server port number) can be reused)
   */
    public static final int MSL = 30;

    /**
   * Number of milliseconds to wait before sending an ACK.
   */
    public static final long DELAYED_ACK_TIME = 200 * Constants.MILLI_SECOND;

    /**
   * Number of seconds to wait for first retransmission timer.
   */
    public static final long RETRANSMIT_TIMEOUT = (long) (1.5 * (double) Constants.SECOND);

    /**
   * Number of seconds to wait for second retransmission timer.
   */
    public static final long RETRANSMIT_TIMEOUT_FINAL = 64 * Constants.SECOND;

    /**
   * Number of seconds to wait before sending probe packets. This
   * happens when receiver is advertising zero window.
   */
    public static final long PERSIST_TIMER = 60 * Constants.SECOND;

    /**
   * Initial window size.
   */
    public static final short INIT_WINDOW_SIZE = 4096;

    /**
   * A pointer to this socket's proxy.
   */
    private SocketInterface.TcpSocketInterface self;

    /**
   * Local port number for the connection.
   */
    private int lport;

    /**
   * Remote port number for the connection.
   */
    private int rport;

    /**
   * Local address for the connection.
   */
    private NetAddress laddr;

    /**
   * Remote address for the connection.
   */
    private NetAddress raddr;

    /**
   * A channel for blocking send/receive.
   */
    private Channel channel;

    /**
   * A callback object used by other entities to call this socket.
   */
    private TransInterface.SocketHandler.TcpHandler callback;

    /**
   * A reference to TCP Entity.
   */
    private TransInterface.TransTcpInterface tcpEntity;

    /**
   * booleans to support socket implementation.
   */
    private boolean isBound, isClosed, isConnected, isTcpNoDelay;

    /**
   * Input Stream for this connection.
   */
    private TcpInputStream in;

    /**
   * Output Stream for this connection.
   */
    private TcpOutputStream out;

    /**
   * booleans to support TCP implementation.
   */
    private boolean connectInConstructor, isClosing;

    /**
   * flag to note if application layer is waiting for messages.
   */
    private boolean isApplicationWaiting;

    /**
   * number of bytes requested by application layer while waiting.
   */
    private int numBytesRequest;

    /**
   * State of the TCP connection.
   * (See Constants.java for details)
   */
    private int currentState;

    /**
   * Buffers to hold bytes received.
   */
    private CircularBuffer receiveBuffer;

    /**
   * Buffers to hold bytes to be sent.
   */
    private CircularBuffer sendBuffer;

    /**
   * Initial sequence number.
   */
    private int initSeqNum;

    /**
   * Initial acknowledgement number.
   */
    private int initAckNum;

    /**
   * next sequence number to be sent.
   */
    private int snd_nxt;

    /**
   * oldest unacknowledged sequence number.
   */
    private int snd_una;

    /**
   * most recently advertised receiver window.
   */
    private short rwnd;

    /**
   * congestion window.
   */
    private short cwnd;

    /**
   * to hold the increment of congestion window during congestion avoidance.
   */
    private double cwnd_increment;

    /**
   * threshold for slow start/congestion avoidance.
   */
    private short sshtresh;

    /**
   * counter for number of duplicate ACKs that we currently receive.
   */
    private int dupAckCounter;

    /**
   * temporary variable to store the current probe message (if not null).
   */
    private TcpMessage probeMessage;

    /**
   * a list to hold packets that might need to be retransmitted.
   */
    private PriorityList rList;

    /**
   * a list to hold out-of-order received packets that are in window.
   */
    private PriorityList rMsgBuffer;

    /**
   * next sequence number expected on an incoming segment.
   */
    private int rcv_nxt;

    /**
   * receiver window.
   */
    private short rcv_wnd;

    /**
   * temporary receiver window (used to hold update 
   * after some data are sent to application layer).
   */
    private short temp_rcv_wnd;

    /**
   * the receiving window that we most recently advertised.
   */
    private short last_adv_wnd;

    /**
   * to count how many probes we have received for this current session
   * (session means the time when the receiver advertises zero window).
   */
    private int probeCounter;

    /**
   * ID for persist timer.
   */
    private int persistTimerId;

    /**
   * ID for reset timer (for special cases).
   */
    private int resetTimerId;

    /**
   * Number of packets sent by the socket.
   */
    private int packetCounter = 0;

    /**
   * Create an entity reference to itself.
   */
    public void createProxy() {
        self = (SocketInterface.TcpSocketInterface) JistAPI.proxy(this, SocketInterface.TcpSocketInterface.class);
    }

    /**
   * Returns the entity reference to the socket itself.
   *
   * @return Entity reference to TcpSocket
   */
    public SocketInterface.TcpSocketInterface getProxy() {
        return self;
    }

    /** {@inheritDoc} */
    public void setTcpEntity(TransInterface.TransTcpInterface tcpEntity) {
        this.tcpEntity = tcpEntity;
    }

    /**
   * Creates an unconnected socket, with the system-default type of SocketImpl.
   */
    public TcpSocket() {
        initializeAll(null, 0, null, 0, false);
    }

    /**
   * Creates a stream socket and connects it to the specified port 
   * number at the specified IP address.
   *
   * @param address the IP address
   * @param port the port number
   */
    public TcpSocket(InetAddress address, int port) {
        this(address, port, null, 0);
    }

    /**
   * Deprecated. Use DatagramSocket instead for UDP transport.
   *
   * @param host the IP address
   * @param port the port number
   * @param stream if true, create a stream socket; 
   * otherwise, create a datagram socket.
   */
    public TcpSocket(InetAddress host, int port, boolean stream) {
        throw new RuntimeException("not implemented");
    }

    /**
   * Creates a socket and connects it to the specified remote 
   * address on the specified remote port.
   *
   * @param address the remote address
   * @param port the remote port
   * @param localAddr the local address the socket is bound to
   * @param localPort the local port the socket is bound to 
   */
    public TcpSocket(InetAddress address, int port, InetAddress localAddr, int localPort) {
        initializeAll(address, port, localAddr, localPort, true);
    }

    /**
   * Creates an unconnected Socket with a user-specified SocketImpl.
   *
   * @param impl an instance of a SocketImpl the subclass wishes to use on the Socket
   */
    protected TcpSocket(SocketImpl impl) {
        throw new RuntimeException("not implemented");
    }

    /**
   * Creates an unconnected Socket with a user-specified TransInterface
   * Entity and SYN packet received by TcpServerSocket.
   *
   * @param tcpEntity entity reference to transport layer
   * @param rAddr remote address
   * @param rPort remote port number
   * @param lAddr local address
   * @param lPort local port number
   * @param initialAckNum initial acknowledgement number
   * @param winSize the other side's receiver window size
   */
    protected TcpSocket(TransInterface.TransTcpInterface tcpEntity, InetAddress rAddr, short rPort, InetAddress lAddr, short lPort, int initialAckNum, short winSize) {
        this();
        this.tcpEntity = tcpEntity;
        setLocalAddress(lAddr, lPort);
        setRemoteAddress(rAddr, rPort);
        setInitAckNum(initialAckNum);
        currentState = Constants.TCPSTATES.SYN_RECEIVED;
        rwnd = winSize;
    }

    /**
   * Creates a stream socket and connects it to the specified 
   * port number on the named host.
   *
   * @param host the host name, or null for the loopback address.
   * @param port the port number. 
   */
    public TcpSocket(String host, int port) {
        this(host, port, null, 0);
    }

    /**
   * Deprecated. Use DatagramSocket instead for UDP transport.
   *
   * @param host the host name, or null for the loopback address.
   * @param port the port number.
   * @param stream a boolean indicating whether this is 
   * a stream socket or a datagram socket.
   */
    public TcpSocket(String host, int port, boolean stream) {
        throw new RuntimeException("not implemented");
    }

    /**
   * Creates a socket and connects it to the specified remote 
   * host on the specified remote port.
   *
   * @param host the name of the remote host, or null for the loopback address.
   * @param port the remote port
   * @param localAddr the local address the socket is bound to
   * @param localPort the local port the socket is bound to 
   */
    public TcpSocket(String host, int port, InetAddress localAddr, int localPort) {
        InetAddress address = null;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            address = null;
        }
        initializeAll(address, port, localAddr, localPort, true);
    }

    /** {@inheritDoc} */
    public void _jistPostInit() {
        if (connectInConstructor) {
            bind(new InetSocketAddress(laddr.getIP(), lport));
            connect(raddr.getIP(), rport, 0);
        }
    }

    /** {@inheritDoc} */
    public void bind(SocketAddress bindpoint) {
        InetSocketAddress inetAddr = (InetSocketAddress) bindpoint;
        setLocalAddress(inetAddr.getAddress(), inetAddr.getPort());
        while (this.lport == 0) {
            this.lport = Math.abs(Constants.random.nextInt()) % 5000;
            TransInterface.TransTcpInterface tempEntity = (TransInterface.TransTcpInterface) tcpEntity;
            if (tempEntity.checkSocketHandler(this.lport)) {
                this.lport = 0;
            }
        }
        if (isBound) {
            tcpEntity.delSocketHandler(lport);
        }
        if (PRINTOUT >= INFO) {
            System.out.println("TcpSocket:: bind --> port = " + this.lport);
        }
        tcpEntity.addSocketHandler(this.lport, callback);
        isBound = true;
    }

    /** {@inheritDoc} */
    public void close() {
        if (PRINTOUT >= INFO) {
            System.out.println("TcpSocket::close for port " + lport + " at time = " + JistAPI.getTime());
        }
        if (sendBuffer.getTotalBytesInBuffer() > 0) {
            isClosing = true;
        } else {
            initiateClosingConnection();
        }
    }

    /** {@inheritDoc} */
    public void connect(SocketAddress endpoint) {
        connect(endpoint, 0);
    }

    /** {@inheritDoc} */
    public void connect(SocketAddress endpoint, int timeout) {
        connect(((InetSocketAddress) endpoint).getAddress(), ((InetSocketAddress) endpoint).getPort(), timeout);
    }

    /**
   * Connects this socket to a specified address and timeout value.
   *
   * @param raddr remote address
   * @param rport remote port
   * @param timeout the timeout value to be used in milliseconds
   */
    private void connect(InetAddress raddr, int rport, int timeout) {
        setRemoteAddress(raddr, rport);
        sendSYNPacket();
        currentState = Constants.TCPSTATES.SYN_SENT;
        channel.receive();
    }

    /** {@inheritDoc} */
    public SocketChannel getChannel() {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public InetAddress getInetAddress() {
        return raddr.getIP();
    }

    /** {@inheritDoc} */
    public InputStream getInputStream() {
        return in;
    }

    /** {@inheritDoc} */
    public boolean getKeepAlive() {
        return true;
    }

    /** {@inheritDoc} */
    public InetAddress getLocalAddress() {
        return laddr.getIP();
    }

    /** {@inheritDoc} */
    public int getLocalPort() {
        return lport;
    }

    /** {@inheritDoc} */
    public SocketAddress getLocalSocketAddress() {
        return (isBound ? (new InetSocketAddress(laddr.getIP(), lport)) : null);
    }

    /** {@inheritDoc} */
    public boolean getOOBInline() {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public OutputStream getOutputStream() {
        return out;
    }

    /** {@inheritDoc} */
    public int getPort() {
        return rport;
    }

    /** {@inheritDoc} */
    public int getReceiveBufferSize() {
        return receiveBuffer.getCurrentBufferSize();
    }

    /** {@inheritDoc} */
    public SocketAddress getRemoteSocketAddress() {
        return (isBound ? (new InetSocketAddress(raddr.getIP(), rport)) : null);
    }

    /** {@inheritDoc} */
    public boolean getReuseAddress() {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public int getSendBufferSize() {
        return sendBuffer.getCurrentBufferSize();
    }

    /** {@inheritDoc} */
    public int getSoLinger() {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public int getSoTimeout() {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public boolean getTcpNoDelay() {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public int getTrafficClass() {
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
    public boolean isConnected() {
        return isConnected;
    }

    /** {@inheritDoc} */
    public boolean isInputShutdown() {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public boolean isOutputShutdown() {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public void sendUrgentData(int data) {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public void setKeepAlive(boolean on) {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public void setOOBInline(boolean on) {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public void setReceiveBufferSize(int size) {
        receiveBuffer.resizeBuffer(size);
    }

    /** {@inheritDoc} */
    public void setReuseAddress(boolean on) {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public void setSendBufferSize(int size) {
        sendBuffer.resizeBuffer(size);
    }

    /**
   * Sets the client socket implementation factory for the application.
   *
   * @param fac the desired factory
   */
    public static void setSocketImplFactory(SocketImplFactory fac) {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public void setSoLinger(boolean on, int linger) {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public void setSoTimeout(int timeout) {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public void setTcpNoDelay(boolean on) {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public void setTrafficClass(int tc) {
        throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public void shutdownInput() throws IOException {
        in.close();
    }

    /** {@inheritDoc} */
    public void shutdownOutput() throws IOException {
        out.close();
    }

    /** {@inheritDoc} */
    public String toString() {
        return ("TcpSocket:(" + laddr + ":" + lport + ")");
    }

    /** 
   * Set the remote address and remote port for this socket.
   *
   * @param address remote address
   * @param port remote port number
   */
    private void setRemoteAddress(InetAddress address, int port) {
        this.raddr = new NetAddress(address);
        this.rport = port;
    }

    /** 
   * Set the local address and local port for this socket.
   *
   * @param address local address
   * @param port local port number
   */
    private void setLocalAddress(InetAddress address, int port) {
        this.laddr = new NetAddress(address);
        this.lport = port;
    }

    /**
   * Initialize all the socket variables.
   */
    private void initializeSocketVariables() {
        isBound = false;
        isClosed = false;
        isConnected = false;
        isClosing = false;
        isApplicationWaiting = false;
    }

    /**
   * Initialize all variables needed for TCP Implementation.
   */
    private void initializeTCPVariables() {
        snd_nxt = TcpServerSocket.getRandomSequenceNumber();
        initSeqNum = snd_nxt;
        rcv_nxt = 0;
        currentState = Constants.TCPSTATES.CLOSED;
        cwnd = MSS;
        sshtresh = (short) 65535;
        cwnd_increment = 0;
        rcv_wnd = INIT_WINDOW_SIZE;
        rwnd = 0;
        temp_rcv_wnd = 0;
        last_adv_wnd = rcv_wnd;
        snd_una = snd_nxt;
        dupAckCounter = 0;
        probeCounter = 0;
        initializeTimerVariables();
        initializeTCPBuffers();
    }

    /**
   * Initialize all variables needed for TCP timers.
   */
    private void initializeTimerVariables() {
        persistTimerId = 0;
        resetTimerId = 0;
    }

    /**
   * Initialize all buffers needed for TCP implementation.
   */
    private void initializeTCPBuffers() {
        receiveBuffer = new CircularBuffer(INIT_WINDOW_SIZE * 2);
        sendBuffer = new CircularBuffer(INIT_WINDOW_SIZE * 2);
        rList = new PriorityList();
        rMsgBuffer = new PriorityList();
    }

    /**
   * Initialize all variables needed.
   * 
   * @param rAddr remote address
   * @param rPort remote port number
   * @param lAddr local address
   * @param lPort local port number
   * @param doConnect set to true to try to connect in constructor
   */
    private void initializeAll(InetAddress rAddr, int rPort, InetAddress lAddr, int lPort, boolean doConnect) {
        createProxy();
        setLocalAddress(lAddr, lPort);
        setRemoteAddress(rAddr, rPort);
        this.channel = JistAPI.createChannel();
        this.callback = new TcpSocketCallback(getProxy());
        initializeSocketVariables();
        initializeTCPVariables();
        connectInConstructor = doConnect;
    }

    /** 
   * Create the input and output streams for this socket.
   */
    private void createStreams() {
        in = new TcpInputStream(getProxy());
        out = new TcpOutputStream(getProxy());
    }

    /**
   * this method is used to wait for incoming ACK packet for connection
   * establishment.
   */
    protected void establishingConnection() {
        channel.receive();
    }

    /**
   * this method is called to close the connection.
   */
    private void initiateClosingConnection() {
        sendFINPacket();
        switch(currentState) {
            case Constants.TCPSTATES.ESTABLISHED:
                currentState = Constants.TCPSTATES.FIN_WAIT_1;
                break;
            case Constants.TCPSTATES.CLOSE_WAIT:
                currentState = Constants.TCPSTATES.LAST_ACK;
                break;
            default:
                break;
        }
    }

    /** 
   * Set the initial acknowledgement number for this socket.
   *
   * @param ackNum acknowledgement number
   */
    private void setInitAckNum(int ackNum) {
        this.rcv_nxt = ackNum + 1;
        this.initAckNum = ackNum;
    }

    /** 
   * Prints out the message header and payload. 
   * 
   * @param msg TCP message to print out
   * @param isReceive true if printing for receiving side
   */
    protected void printMessage(TcpMessage msg, boolean isReceive) {
        String direction = isReceive ? "** <-- RCV " : "** SND -->";
        System.out.println(direction + " packet " + packetCounter + " on (" + lport + ") at time=" + JistAPI.getTime() + ": " + msg);
    }

    /**
   * Returns the string name of current state.
   * (check Constants.java)
   *
   * @return string representation of current state
   */
    private String getCurrentStateString() {
        switch(currentState) {
            case 800:
                return "LISTEN";
            case 801:
                return "SYN_SENT";
            case 802:
                return "SYN_RECEIVED";
            case 803:
                return "ESTABLISHED";
            case 804:
                return "FIN_WAIT_1";
            case 805:
                return "FIN_WAIT_2";
            case 806:
                return "CLOSE_WAIT";
            case 807:
                return "CLOSING";
            case 808:
                return "LAST_ACK";
            case 809:
                return "TIME_WAIT";
            case 810:
                return "CLOSED";
            default:
                return "INVALID STATE";
        }
    }

    /**
   * Returns the difference between two unsigned shorts.
   *
   * @param a first short
   * @param b second short
   * @return a - b
   */
    private int compareUnsignedShort(short a, short b) {
        int aInt = a & 0xffff;
        int bInt = b & 0xffff;
        return (aInt - bInt);
    }

    /**
   * Returns the difference between two unsigned integers.
   *
   * @param a first integer
   * @param b second integer
   * @return a - b
   */
    private long compareUnsignedInt(int a, int b) {
        long aLong = a & 0xffffffff;
        long bLong = b & 0xffffffff;
        return (aLong - bLong);
    }

    /** 
   * A method to send SYN packet. 
   */
    protected void sendSYNPacket() {
        TcpMessage msg = TcpMessage.createSYNPacket(lport, rport, snd_nxt, rcv_wnd);
        sendMessage(msg);
        rList.insert(msg);
        self.startRetransmitTimer(msg.getSeqNum(), RETRANSMIT_TIMEOUT * 4);
        ++snd_nxt;
    }

    /** 
   * A method to send SYNACK packet. 
   */
    protected void sendSYNACKPacket() {
        TcpMessage msg = TcpMessage.createSYNACKPacket(lport, rport, snd_nxt, rcv_nxt, rcv_wnd);
        sendMessage(msg);
        ++snd_nxt;
    }

    /** 
   * A method to send ACK packet.
   */
    protected void sendFirstACKPacket() {
        TcpMessage msg = TcpMessage.createACKPacket(lport, rport, snd_nxt, rcv_nxt, rcv_wnd);
        sendMessage(msg);
    }

    /** 
   * A method to send FIN packet.
   */
    protected void sendFINPacket() {
        TcpMessage msg = TcpMessage.createFINPacket(lport, rport, snd_nxt, rcv_nxt, rcv_wnd);
        sendMessage(msg);
        rList.insert(msg);
        self.startRetransmitTimer(msg.getSeqNum(), RETRANSMIT_TIMEOUT * 4);
        ++snd_nxt;
    }

    /** 
   * A method to send RST packet.
   *
   * @param seqNum sequence number
   * @param ackNum acknowledgement number
   */
    protected void sendRSTPacket(int seqNum, int ackNum) {
        TcpMessage msg = TcpMessage.createRSTPacket(lport, rport, seqNum, ackNum, rcv_wnd);
        sendMessage(msg);
    }

    /** 
   * Send a packet to the remote socket. To send an ACK packet, set data
   * to null.
   *
   * @param data data to send
   * @return sent packet  
   */
    protected TcpMessage sendDataPacket(byte[] data) {
        final short offset = 5;
        final boolean URG = false, ACK = true, PSH = false, RST = false, SYN = false, FIN = false;
        if (data == null) last_adv_wnd = rcv_wnd;
        TcpMessage msg = new TcpMessage((short) lport, (short) rport, snd_nxt, rcv_nxt, offset, URG, ACK, PSH, RST, SYN, FIN, rcv_wnd, new MessageBytes(data));
        sendMessage(msg);
        if (data != null) snd_nxt += data.length;
        return msg;
    }

    /**
   * Send a TCP message to remote socket.
   * 
   * @param msg message to send
   */
    private void sendMessage(TcpMessage msg) {
        if (PRINTOUT >= TCP_DEBUG) {
            printMessage(msg, false);
        }
        packetCounter++;
        tcpEntity.send(msg, raddr, rport, lport, Constants.NET_PRIORITY_NORMAL);
        JistAPI.sleep(Constants.TRANS_PROCESSING_DELAY);
    }

    /** 
   * This method is called to send bytes in the buffer until they fill
   * the receiver window. The bytes will be broken down to packets based
   * on MSS (maximum segment size).
   */
    private void sendBytesInBuffer() {
        if (sendBuffer.getTotalBytesInBuffer() > 0) {
            constructPackets();
        }
        if ((sendBuffer.getTotalBytesInBuffer() == 0) && isClosing) {
            initiateClosingConnection();
        }
    }

    /** {@inheritDoc} */
    public void constructPackets() {
        int curWindow = getCurrentReceiverWindow() & 0xffff;
        if (curWindow > (snd_nxt - snd_una)) {
            if (sendBuffer.getTotalBytesInBuffer() == 0) {
                return;
            }
            TcpMessage sentMsg = null;
            if ((curWindow - (snd_nxt - snd_una)) < MSS) {
                int tempLength = curWindow - (snd_nxt - snd_una);
                byte[] data = sendBuffer.retrieveBytes(tempLength);
                sentMsg = sendDataPacket(data);
                rList.insert(sentMsg);
            } else {
                byte[] data = sendBuffer.retrieveBytes(MSS);
                sentMsg = sendDataPacket(data);
                rList.insert(sentMsg);
            }
            if (sentMsg != null) {
                self.startRetransmitTimer(sentMsg.getSeqNum(), RETRANSMIT_TIMEOUT);
            }
            constructPackets();
        }
    }

    /** {@inheritDoc} */
    public int queueBytes(byte[] data) {
        int numBytes = sendBuffer.storeBytes(data);
        sendBytesInBuffer();
        return numBytes;
    }

    /**
   * This method sends bytes to channel.
   * The number of bytes to send is specified in the parameter.
   *
   * @param length number of bytes to send to application layer
   */
    private void sendBytesToChannel(int length) {
        byte[] data = receiveBuffer.retrieveBytes(length);
        channel.sendNonBlock(data, true, false);
    }

    /** {@inheritDoc} */
    public byte[] getBytesFromSocket(int length) {
        if (length <= 0) return null;
        byte[] data = receiveBuffer.retrieveBytes(length);
        if (data == null) {
            if (PRINTOUT >= FULL_DEBUG) {
                System.out.println("sendBytesToChannel: no data available");
            }
            isApplicationWaiting = true;
            numBytesRequest = length;
            data = (byte[]) channel.receive();
        }
        temp_rcv_wnd += data.length;
        if (temp_rcv_wnd >= MSS) {
            rcv_wnd += temp_rcv_wnd;
            temp_rcv_wnd = 0;
        }
        return data;
    }

    /** 
   * Returns the smaller window (between the last advertised window 
   * and the congestion window) for the receiver.
   *
   * @return receiver window or congestion window, whichever
   * is smaller. 
   */
    private short getCurrentReceiverWindow() {
        return (compareUnsignedShort(rwnd, cwnd) > 0 ? cwnd : rwnd);
    }

    /** {@inheritDoc} */
    public void startRetransmitTimer(int seqNum, long time) {
        JistAPI.sleep(time);
        self.retransmitTimerTimeout(seqNum, time);
    }

    /** {@inheritDoc} */
    public void retransmitTimerTimeout(int seqNum, long time) {
        if (onRetransmit(seqNum)) {
            if (PRINTOUT >= FULL_DEBUG) {
                System.out.println("TcpSocket::retransmitTimerTimeout(t=" + JistAPI.getTime() + "): seqNum = " + seqNum + "(time=" + time + ")");
                System.out.println("time = " + time);
                System.out.println("RETRANSMIT_TIMEOUT_FINAL = " + RETRANSMIT_TIMEOUT_FINAL);
            }
            if (time < RETRANSMIT_TIMEOUT_FINAL) {
                long tempTime = time * 2;
                if (tempTime > RETRANSMIT_TIMEOUT_FINAL) {
                    tempTime = RETRANSMIT_TIMEOUT_FINAL;
                }
                self.startRetransmitTimer(seqNum, tempTime);
            } else {
                closeSocket(true);
            }
        }
    }

    /** {@inheritDoc} */
    public void startPersistTimer(int seqNum) {
        cancelPersistTimer();
        if (PRINTOUT >= FULL_DEBUG) {
            System.out.println("TcpSocket: startPersistTimer at t = " + JistAPI.getTime());
        }
        JistAPI.sleep(PERSIST_TIMER);
        self.persistTimerTimeout(persistTimerId, seqNum);
    }

    /**
   * Cancel the current persist timer.
   */
    private void cancelPersistTimer() {
        persistTimerId++;
    }

    /** {@inheritDoc} */
    public void persistTimerTimeout(int timerId, int seqNum) {
        if (timerId == persistTimerId) {
            if (PRINTOUT >= FULL_DEBUG) {
                System.out.println("TcpSocket: persistTimerTimeout at t = " + JistAPI.getTime());
            }
            if (PRINTOUT >= FULL_DEBUG) {
                System.out.println("Sending probe");
            }
            TcpMessage probe = rList.retrieve(seqNum);
            if (probe != null) {
                sendMessage(probe);
                self.startPersistTimer(seqNum);
            }
        }
    }

    /**
   * Create a probe message (for persist timer) by extracting one byte
   * of data from the send buffer. 
   *
   * @return true if a probe message is available
   */
    private boolean createProbeMessage() {
        if (probeMessage == null) {
            byte[] data = sendBuffer.retrieveBytes(1);
            if (data == null) return false;
            short offset = 5;
            final boolean URG = false, ACK = true, PSH = false, RST = false, SYN = false, FIN = false;
            MessageBytes probeByte = new MessageBytes(data);
            probeMessage = new TcpMessage((short) lport, (short) rport, snd_nxt, rcv_nxt, offset, URG, ACK, PSH, RST, SYN, FIN, rcv_wnd, probeByte);
            snd_nxt += data.length;
            rList.insert(probeMessage);
        }
        return true;
    }

    /** {@inheritDoc} */
    public void startResetTimer() {
        cancelResetTimer();
        if (PRINTOUT >= FULL_DEBUG) {
            System.out.println("TcpSocket: startResetTimer at t = " + JistAPI.getTime());
        }
        JistAPI.sleep(RETRANSMIT_TIMEOUT * 4);
        self.resetTimerTimeout(resetTimerId);
    }

    /**
   * Cancel the current reset timer.
   */
    private void cancelResetTimer() {
        resetTimerId++;
    }

    /** {@inheritDoc} */
    public void resetTimerTimeout(int timerId) {
        if (timerId == resetTimerId) {
            if (PRINTOUT >= FULL_DEBUG) {
                System.out.println("TcpSocket: resetTimerTimeout at t = " + JistAPI.getTime());
            }
            closeSocket(true);
        }
    }

    /** {@inheritDoc} */
    public void startTimeWaitTimer() {
        JistAPI.sleep(2 * MSL);
        self.timeWaitTimerTimeout();
    }

    /** {@inheritDoc} */
    public void timeWaitTimerTimeout() {
        closeSocket(true);
    }

    /** {@inheritDoc} */
    public void checkPacketandState(TcpMessage msg, NetAddress src) {
        if (PRINTOUT >= TCP_DEBUG) {
            if (PRINTOUT >= FULL_DEBUG) {
                System.out.println();
                System.out.println("TcpSocket: checkPacketandState (" + lport + "): " + getCurrentStateString());
            }
            printMessage(msg, true);
        }
        switch(currentState) {
            case Constants.TCPSTATES.CLOSED:
                onClosedState(msg, src);
                break;
            case Constants.TCPSTATES.SYN_SENT:
                onSynSentState(msg);
                break;
            case Constants.TCPSTATES.SYN_RECEIVED:
                onSynReceivedState(msg);
                break;
            case Constants.TCPSTATES.ESTABLISHED:
                onEstablishedState(msg);
                break;
            case Constants.TCPSTATES.FIN_WAIT_1:
                onFinWait1State(msg);
                break;
            case Constants.TCPSTATES.FIN_WAIT_2:
                onFinWait2State(msg);
                break;
            case Constants.TCPSTATES.CLOSING:
                onClosingState(msg);
                break;
            case Constants.TCPSTATES.CLOSE_WAIT:
                break;
            case Constants.TCPSTATES.LAST_ACK:
                onLastAckState(msg);
                break;
            case Constants.TCPSTATES.TIME_WAIT:
                self.startTimeWaitTimer();
                break;
            default:
                break;
        }
        if (PRINTOUT >= FULL_DEBUG) {
            System.out.println("TcpSocket: new state: checkPacketandState (" + lport + "): " + getCurrentStateString());
        }
    }

    /**
   * This method takes care of all the steps that need to be done
   * if we receive a packet during CLOSED state.
   *
   * @param msg incoming packet
   * @param src packet source
   */
    private void onClosedState(TcpMessage msg, NetAddress src) {
        if (msg.getSYN() && !msg.getACK()) {
            initializeAll(laddr.getIP(), lport, src.getIP(), msg.getSrcPort(), false);
            setInitAckNum(msg.getSeqNum());
            currentState = Constants.TCPSTATES.SYN_RECEIVED;
            rwnd = msg.getWindowSize();
            sendSYNACKPacket();
        } else if (!msg.getRST()) {
            rport = msg.getSrcPort();
            sendRSTPacket(msg.getAckNum(), msg.getSeqNum());
        }
    }

    /**
   * This method takes care of all the steps that need to be done
   * if we receive a packet during SYN_SENT state.
   *
   * @param msg incoming packet
   */
    private void onSynSentState(TcpMessage msg) {
        if (msg.getSYN() && msg.getACK() && (msg.getAckNum() == snd_nxt)) {
            rport = msg.getSrcPort();
            rcv_nxt = msg.getSeqNum() + 1;
            initAckNum = msg.getSeqNum();
            snd_una = msg.getAckNum();
            rList.removeMessages(msg.getAckNum());
            sendFirstACKPacket();
            cancelResetTimer();
            currentState = Constants.TCPSTATES.ESTABLISHED;
            rwnd = msg.getWindowSize();
            isConnected = true;
            createStreams();
            channel.sendNonBlock(null, true, false);
        } else if (msg.getSYN() && !msg.getACK()) {
            rport = msg.getSrcPort();
            rcv_nxt = msg.getSeqNum() + 1;
            initAckNum = msg.getSeqNum();
            rList.removeMessages(snd_nxt);
            sendSYNACKPacket();
            currentState = Constants.TCPSTATES.SYN_RECEIVED;
            rwnd = msg.getWindowSize();
        } else {
            rport = msg.getSrcPort();
            sendRSTPacket(msg.getAckNum(), msg.getSeqNum());
            --snd_nxt;
            sendSYNPacket();
        }
    }

    /**
   * This method takes care of all the steps that need to be done
   * if we receive a packet during SYN_RECEIVED state.
   *
   * @param msg incoming packet
   */
    private void onSynReceivedState(TcpMessage msg) {
        if (msg.getRST()) {
            closeSocket(false);
        }
        if (!msg.getSYN() && msg.getACK() && (msg.getAckNum() == snd_nxt)) {
            currentState = Constants.TCPSTATES.ESTABLISHED;
            snd_una = msg.getAckNum();
            isConnected = true;
            cancelResetTimer();
            createStreams();
            channel.sendNonBlock(null, true, false);
            if (msg.getPayload().getSize() > 0) {
                byte[] temp = new byte[msg.getPayload().getSize()];
                msg.getPayload().getBytes(temp, 0);
                receiveBuffer.storeBytes(temp);
                rcv_nxt = rcv_nxt + msg.getPayload().getSize();
                sendDataPacket(null);
            }
            sendBytesInBuffer();
        } else if (msg.getSYN() && !msg.getACK()) {
            --snd_nxt;
            sendSYNACKPacket();
        } else if (msg.getSYN() && msg.getACK() && (msg.getAckNum() == snd_nxt)) {
            rport = msg.getSrcPort();
            rcv_nxt = msg.getSeqNum() + 1;
            snd_una = msg.getAckNum();
            sendFirstACKPacket();
            cancelResetTimer();
            rwnd = msg.getWindowSize();
            isConnected = true;
            createStreams();
            channel.sendNonBlock(null, true, false);
        }
    }

    /**
   * This method takes care of all the steps that need to be done
   * if we receive a packet during ESTABLISHED state.
   *
   * @param msg incoming packet
   */
    private void onEstablishedState(TcpMessage msg) {
        if (msg.getRST()) {
            closeSocket(false);
        }
        if (isPacketOutofWindow(msg)) {
            if (PRINTOUT >= FULL_DEBUG) {
                System.out.println(getCurrentStateString() + ": sending DUPACK because of out of window");
            }
            sendDataPacket(null);
            return;
        }
        if (!isAcceptableACK(msg)) {
            return;
        }
        if (msg.getFIN() && msg.getACK()) {
            if (rcv_nxt == msg.getSeqNum()) {
                if (PRINTOUT >= FULL_DEBUG) {
                    System.out.println("receiving FIN from port " + msg.getSrcPort() + "(" + lport + ")");
                }
                currentState = Constants.TCPSTATES.CLOSE_WAIT;
                rcv_nxt = rcv_nxt + 1;
            }
            sendDataPacket(null);
        } else if (!msg.getSYN() && msg.getACK()) {
            onReceiveValidDataorACK(msg);
        }
    }

    /**
   * Check if the incoming packet is out of receiving window.
   *
   * @param msg incoming packet
   * @return true if packet is out of receiving window
   */
    private boolean isPacketOutofWindow(TcpMessage msg) {
        if ((msg.getSeqNum() >= rcv_nxt) && (msg.getSeqNum() <= (rcv_nxt + rcv_wnd)) && msg.getFIN()) {
            return false;
        } else if ((msg.getSeqNum() < rcv_nxt) || (msg.getSeqNum() >= (rcv_nxt + rcv_wnd))) {
            if (PRINTOUT >= FULL_DEBUG) {
                System.out.println(getCurrentStateString() + ": packet ignored: out of the receiving window");
                System.out.println(msg.getSeqNum() + " " + rcv_nxt + " " + (rcv_nxt + rcv_wnd));
            }
            return true;
        }
        return false;
    }

    /**
   * Check if the ACK packet received is acknowledging the 
   * data that have not been acknowledged.
   *
   * @param msg incoming packet
   * @return true if ACK packet is acknowledging unacknowledged bytes
   */
    private boolean isAcceptableACK(TcpMessage msg) {
        if (msg.getACK() && (msg.getAckNum() < snd_una) && (msg.getAckNum() > snd_nxt)) {
            if (PRINTOUT >= FULL_DEBUG) {
                System.out.println(getCurrentStateString() + ": packet ignored: not an acceptable ACK");
            }
            return false;
        }
        return true;
    }

    /**
   * This method is to check if receive data packet
   * or ACK packet. Then, the appropiate method is called
   * to handle each case.
   *
   * @param msg incoming packet
   */
    private void onReceiveValidDataorACK(TcpMessage msg) {
        if (msg.getSeqNum() == rcv_nxt) {
            if (msg.getPayload().getSize() > 0) {
                onReceiveData(msg);
            } else {
                onReceiveACK(msg);
            }
        } else {
            if (PRINTOUT >= FULL_DEBUG) {
                System.out.println(getCurrentStateString() + ": out-of-order: msg.getSeqNum() = " + msg.getSeqNum() + "\trcv_nxt = " + rcv_nxt);
                System.out.println(getCurrentStateString() + ": sending DUPACK because of not in-order");
            }
            sendDataPacket(null);
            rMsgBuffer.insert(msg);
        }
    }

    /**
   * This method handles the case when data packet
   * is received.
   * Will send ACK packet back, take care of probe
   * messages, and send the data received to application
   * layer if requested.
   *
   * @param msg incoming packet
   */
    private void onReceiveData(TcpMessage msg) {
        if (last_adv_wnd == 0) {
            ++probeCounter;
            if (probeCounter >= 3) {
                rcv_wnd += temp_rcv_wnd;
            }
            if (rcv_wnd > msg.getPayload().getSize()) {
                byte[] temp = new byte[msg.getPayload().getSize()];
                msg.getPayload().getBytes(temp, 0);
                receiveBuffer.storeBytes(temp);
                rcv_nxt = rcv_nxt + msg.getPayload().getSize();
                rcv_wnd = (short) (rcv_wnd - (short) msg.getPayload().getSize());
            }
            sendDataPacket(null);
        } else {
            if (rcv_wnd >= msg.getPayload().getSize()) {
                probeCounter = 0;
                byte[] temp = new byte[msg.getPayload().getSize()];
                msg.getPayload().getBytes(temp, 0);
                receiveBuffer.storeBytes(temp);
                rcv_nxt = rcv_nxt + msg.getPayload().getSize();
                rcv_wnd = (short) (rcv_wnd - (short) msg.getPayload().getSize());
                TcpMessage tempMsg = null;
                while ((tempMsg = rMsgBuffer.retrieve(rcv_nxt)) != null) {
                    if (rcv_wnd >= tempMsg.getPayload().getSize()) {
                        temp = new byte[tempMsg.getPayload().getSize()];
                        tempMsg.getPayload().getBytes(temp, 0);
                        receiveBuffer.storeBytes(temp);
                        rMsgBuffer.removeMessages(rcv_nxt);
                        rcv_nxt = rcv_nxt + tempMsg.getPayload().getSize();
                        rcv_wnd = (short) (rcv_wnd - (short) tempMsg.getPayload().getSize());
                    } else {
                        break;
                    }
                }
            }
            sendDataPacket(null);
            if (isApplicationWaiting) {
                sendBytesToChannel(numBytesRequest);
            }
        }
    }

    /**
   * This method handles the case when ACK packet
   * is received.
   * Will send more data or probe message (if advertised
   * window is zero) and do congestion control algorithm.
   *
   * @param msg incoming packet
   */
    private void onReceiveACK(TcpMessage msg) {
        rwnd = msg.getWindowSize();
        if ((msg.getWindowSize() == 0) && (sendBuffer.getTotalBytesInBuffer() > 0)) {
            if (snd_una < msg.getAckNum()) {
                snd_una = msg.getAckNum();
            }
            if (createProbeMessage()) {
                self.startPersistTimer(msg.getAckNum());
            }
        } else {
            cancelPersistTimer();
            if (probeMessage != null) {
                probeMessage = null;
            }
            if (snd_una == msg.getAckNum()) {
                ++dupAckCounter;
                FastRetransmit();
            } else {
                if (compareUnsignedShort(sshtresh, cwnd) > 0) {
                    SlowStart();
                } else {
                    CongestionAvoidance();
                }
                dupAckCounter = 0;
                snd_una = msg.getAckNum();
                rList.removeMessages(msg.getAckNum());
                if (currentState == Constants.TCPSTATES.ESTABLISHED) {
                    sendBytesInBuffer();
                }
            }
        }
    }

    /**
   * This method implements Fast Retransmit algorithm.
   */
    private void FastRetransmit() {
        if (dupAckCounter > 3) {
            if (PRINTOUT >= FULL_DEBUG) {
                System.out.println("ESTABLISHED: RETRANSMIT: snd_una == msg.getAkNum() = " + snd_una);
            }
            onRetransmit(snd_una);
            int temp = cwnd & 0xffff;
            sshtresh = (short) (temp / 2);
            cwnd = MSS;
            if (PRINTOUT >= FULL_DEBUG) {
                System.out.println("Congestion detected: new threshold = " + sshtresh);
            }
        }
    }

    /**
   * This method implements Start phase for congestion control.
   */
    private void SlowStart() {
        int temp = cwnd & 0xffff;
        if ((temp * 2) > 65536) {
            cwnd = (short) 65536;
        } else {
            cwnd = (short) (temp + MSS);
        }
        if (PRINTOUT >= FULL_DEBUG) {
            System.out.println("&&&%%%$$$ NEW CONGESTION WINDOW = " + temp + "(" + cwnd + ")");
        }
    }

    /**
   * This method implements Congestion Avoidance phase for congestion control.
   */
    private void CongestionAvoidance() {
        int temp = cwnd & 0xffff;
        cwnd_increment += (double) MSS / (double) temp;
        if (cwnd_increment >= 1.0) {
            if ((temp + MSS) < 65536) {
                cwnd = (short) (temp + MSS);
            } else {
                cwnd = (short) 65536;
            }
            cwnd_increment = 0.0;
        }
        if (PRINTOUT >= FULL_DEBUG) {
            System.out.println("&&&%%%$$$ NEW CONGESTION WINDOW = " + temp + "(" + cwnd + ")\ttemp_cwnd_increment = " + cwnd_increment);
        }
    }

    /**
   * Called when retransmission happens. Parameter is to
   * denote which message to send.
   *
   * @param seqNumToSend sequence number of the packet to be retransmitted
   * @return true if the message is retransmitted, false otherwise.
   */
    private boolean onRetransmit(int seqNumToSend) {
        TcpMessage msgToRetransmit = rList.retrieve(seqNumToSend);
        if (msgToRetransmit != null) {
            if (PRINTOUT >= FULL_DEBUG) {
                System.out.println("~~~~~ TcpSocket::onRetransmit: seqNumToSend = " + seqNumToSend);
                System.out.println("~~~~~ Message to retransmit: " + msgToRetransmit);
            }
            sendMessage(msgToRetransmit);
            return true;
        }
        return false;
    }

    /**
   * This method takes care of all the steps that need to be done
   * if we receive a packet during FIN_WAIT_1 state.
   *
   * @param msg incoming packet
   */
    private void onFinWait1State(TcpMessage msg) {
        if (isPacketOutofWindow(msg) || !isAcceptableACK(msg)) {
            return;
        }
        if (msg.getFIN() && msg.getACK()) {
            rcv_nxt = rcv_nxt + 1;
            sendDataPacket(null);
            currentState = Constants.TCPSTATES.CLOSING;
        } else if (!msg.getFIN() && msg.getACK()) {
            onReceiveValidDataorACK(msg);
            if (snd_nxt == snd_una) {
                currentState = Constants.TCPSTATES.FIN_WAIT_2;
                cancelResetTimer();
            } else {
                startResetTimer();
            }
        }
    }

    /**
   * This method takes care of all the steps that need to be done
   * if we receive a packet during FIN_WAIT_2 state.
   *
   * @param msg incoming packet
   */
    private void onFinWait2State(TcpMessage msg) {
        if (isPacketOutofWindow(msg) || !isAcceptableACK(msg)) {
            return;
        }
        if (msg.getFIN() && msg.getACK()) {
            rcv_nxt = rcv_nxt + 1;
            sendDataPacket(null);
            currentState = Constants.TCPSTATES.TIME_WAIT;
            self.checkPacketandState(null, null);
        }
    }

    /**
   * This method takes care of all the steps that need to be done
   * if we receive a packet during CLOSING state.
   *
   * @param msg incoming packet
   */
    private void onClosingState(TcpMessage msg) {
        if (isPacketOutofWindow(msg) || !isAcceptableACK(msg)) {
            return;
        }
        if (msg.getACK()) {
            currentState = Constants.TCPSTATES.TIME_WAIT;
            JistAPI.sleep(Constants.EPSILON_DELAY);
            self.checkPacketandState(null, null);
        }
    }

    /**
   * This method takes care of all the steps that need to be done
   * if we receive a packet during LAST_ACK state.
   *
   * @param msg incoming packet
   */
    private void onLastAckState(TcpMessage msg) {
        if (isPacketOutofWindow(msg) || !isAcceptableACK(msg)) {
            return;
        }
        if (msg.getACK()) {
            cancelResetTimer();
            closeSocket(true);
        }
    }

    /**
   * This method is called when socket is to be closed.
   * (also unbind from current port).
   *
   * @param doUnbind set to true to unbind from current port
   * when closing socket. 
   */
    private void closeSocket(boolean doUnbind) {
        if (PRINTOUT >= INFO) {
            System.out.println("***###*** TcpSocket::closeSocket: Port " + lport + " closed (Sent = " + (snd_nxt - initSeqNum) + " Received = " + (rcv_nxt - initAckNum) + ")");
            if (PRINTOUT >= FULL_DEBUG) rList.printList();
        }
        if (doUnbind && isBound) {
            tcpEntity.delSocketHandler(lport);
            isBound = false;
            setLocalAddress(null, 0);
        }
        isConnected = false;
        currentState = Constants.TCPSTATES.CLOSED;
        isClosed = true;
        initializeAll(laddr.getIP(), lport, null, 0, false);
    }

    /**
   * Implementation of Socket Callback for TcpSocket.
   */
    public static class TcpSocketCallback implements TransInterface.SocketHandler.TcpHandler {

        /** Entity reference to TcpSocket. */
        private SocketInterface.TcpSocketInterface socketEntity;

        /**
     * Constructor.
     *
     * @param entity the entity reference to TcpSocket
     */
        public TcpSocketCallback(SocketInterface.TcpSocketInterface entity) {
            socketEntity = entity;
        }

        /** {@inheritDoc} */
        public void receive(Message msg, NetAddress src, int srcPort) {
            socketEntity.checkPacketandState((TcpMessage) msg, src);
        }
    }
}
