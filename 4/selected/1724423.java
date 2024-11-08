package mirrormap.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import mirrormap.Utils;
import mirrormap.lifecycle.AbstractLifeCycle;
import mirrormap.nio.IConnectionEndPointReceiver.IFactory;

/**
 * A server that opens a TCP server socket and spawns {@link TcpConnectionEndPoint} instances to handle each of the
 * client connections.
 * 
 * @author Ramon Servadei
 */
public class TcpServer extends AbstractLifeCycle {

    static final Logger LOGGER = Logger.getLogger(TcpServer.class.getName());

    /** The string name for the tcp server thread */
    public static final String TCP_SERVER_THREAD_PREFIX = TcpServer.class.getSimpleName();

    /**
     * The system property for overriding the default receive buffer size for the server port. This must be specified in
     * units of bytes.
     */
    public static final String CONTEXT_RCV_BUFFER = "TcpServer.bufferSize";

    /** The default TCP port */
    public static final int DEFAULT_TCP_PORT = 16025;

    /** The host name */
    public static final String HOST_NAME;

    static {
        String name = null;
        try {
            name = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOGGER.log(Level.SEVERE, "Could not get host name", e);
            System.exit(1);
        }
        HOST_NAME = name;
    }

    /**
     * Task that handles new connections to this server. It creates a {@link TcpConnectionEndPoint} for each connection.
     * 
     * @author Ramon Servadei
     */
    private final class ConnectionAcceptTask implements ISelectorTasksStateListener, ISelectionKeyTask {

        /** The selection key */
        private SelectionKey selectionKey;

        public ConnectionAcceptTask() {
            super();
        }

        @Override
        public void run() {
            if (TcpServer.this.isActive() && getSelectionKey().isAcceptable()) {
                try {
                    SocketChannel socketChannel = TcpServer.this.serverSocketChannel.accept();
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("(<-) Accepted inbound " + socketChannel);
                    }
                    socketChannel.configureBlocking(false);
                    final String remoteAddress = socketChannel.socket().getInetAddress().getHostName();
                    final int remotePort = socketChannel.socket().getPort();
                    final TcpConnectionEndPoint endPoint = new TcpConnectionEndPoint(TcpServer.this.endPointReceiverFactory.createEndPointRecevier(), TcpServer.this.selectorTasks, TcpServer.this.writer, remoteAddress, remotePort, false);
                    endPoint.setSocketChannel(socketChannel);
                    endPoint.start();
                } catch (Exception e) {
                    Utils.logException(LOGGER, Utils.safeToString(this) + " could not accept connection", e);
                }
            }
        }

        @Override
        public void selectorOpened(SelectorTasks tasks) {
            if (TcpServer.this.isActive() && TcpServer.this.serverSocketChannel != null && TcpServer.this.connectionAcceptTask != null) {
                TcpServer.this.selectorTasks.register(SelectionKey.OP_ACCEPT, TcpServer.this.serverSocketChannel, TcpServer.this.connectionAcceptTask);
            }
        }

        public SelectionKey getSelectionKey() {
            return this.selectionKey;
        }

        @Override
        public void setSelectionKey(SelectionKey selectionKey) {
            this.selectionKey = selectionKey;
        }

        @Override
        public final void destroy() {
            this.selectionKey.cancel();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    /** The IP address for the TCP/IP socket of this server */
    private final String address;

    /** The port number for the TCP/IP socket of this server */
    private final int port;

    /** The server socket channel of this server */
    ServerSocketChannel serverSocketChannel;

    /** The selector tasks */
    final SelectorTasks selectorTasks;

    /** The task that handles new socket connections to the server socket */
    final ConnectionAcceptTask connectionAcceptTask;

    /** The thread that handles all the socket IO activity */
    private Thread connectionThread;

    /** Creates the thread for the server */
    private static final ThreadFactory threadFactory = new ThreadFactory() {

        int id;

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, TcpServer.class.getSimpleName() + " NIO writer " + ++this.id);
            thread.setDaemon(true);
            return thread;
        }
    };

    /** Handles socket writing activity */
    final ExecutorService writer = Executors.newFixedThreadPool(1, threadFactory);

    /** Creates {@link IConnectionEndPointReceiver} instances per client socket */
    final IFactory endPointReceiverFactory;

    /**
     * Construct the server with the factory that will create the {@link IConnectionEndPointReceiver} per client socket
     * connection and the server socket address and port
     * 
     * @param endPointReceiverFactory
     *            the factory to create the {@link IConnectionEndPointReceiver} for each client socket
     * @param address
     *            the server socket address or host name
     * @param port
     *            the server socket TCP port
     */
    public TcpServer(IConnectionEndPointReceiver.IFactory endPointReceiverFactory, String address, int port) {
        super();
        this.address = address == null ? TcpServer.HOST_NAME : address;
        this.port = port <= 0 ? TcpServer.DEFAULT_TCP_PORT : port;
        this.selectorTasks = new SelectorTasks();
        this.connectionAcceptTask = new ConnectionAcceptTask();
        this.selectorTasks.setStateListener(this.connectionAcceptTask);
        this.endPointReceiverFactory = endPointReceiverFactory;
    }

    @Override
    public String toString() {
        return TCP_SERVER_THREAD_PREFIX + "[" + this.address + ":" + this.port + "]";
    }

    @Override
    protected void doDestroy() {
        this.writer.shutdownNow();
        this.selectorTasks.destroy();
        try {
            this.serverSocketChannel.close();
        } catch (IOException e) {
            Utils.logException(LOGGER, "Could not close " + Utils.safeToString(this.serverSocketChannel), e);
        }
    }

    @Override
    protected void doStart() {
        try {
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.configureBlocking(false);
            if (System.getProperty(CONTEXT_RCV_BUFFER) != null) {
                this.serverSocketChannel.socket().setReceiveBufferSize(Integer.parseInt(System.getProperty(CONTEXT_RCV_BUFFER)));
            }
            final InetSocketAddress endpoint = new InetSocketAddress(this.address, this.port);
            this.serverSocketChannel.socket().bind(endpoint);
            this.selectorTasks.start();
            this.connectionThread = new Thread(this.selectorTasks.getRunnable(), this.toString());
            this.connectionThread.setDaemon(true);
            this.connectionThread.start();
            LOGGER.info("Started " + this);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not open channel for " + this, e);
            System.exit(1);
        }
    }
}
