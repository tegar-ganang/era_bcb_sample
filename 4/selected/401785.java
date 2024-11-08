package sun.rmi.transport.tcp;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.rmi.server.LogStream;
import java.rmi.server.RMIFailureHandler;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.RemoteCall;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UID;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import sun.rmi.runtime.Log;
import sun.rmi.runtime.NewThreadAction;
import sun.rmi.transport.Channel;
import sun.rmi.transport.Connection;
import sun.rmi.transport.DGCAckHandler;
import sun.rmi.transport.Endpoint;
import sun.rmi.transport.StreamRemoteCall;
import sun.rmi.transport.Target;
import sun.rmi.transport.Transport;
import sun.rmi.transport.TransportConstants;
import sun.rmi.transport.proxy.HttpReceiveSocket;
import sun.security.action.GetIntegerAction;
import sun.security.action.GetLongAction;
import sun.security.action.GetPropertyAction;

/**
 * TCPTransport is the socket-based implementation of the RMI Transport
 * abstraction.
 *
 * @author Ann Wollrath
 * @author Peter Jones
 */
public class TCPTransport extends Transport {

    static final Log tcpLog = Log.getLog("sun.rmi.transport.tcp", "tcp", LogStream.parseLevel(AccessController.doPrivileged(new GetPropertyAction("sun.rmi.transport.tcp.logLevel"))));

    /** maximum number of connection handler threads */
    private static final int maxConnectionThreads = AccessController.doPrivileged(new GetIntegerAction("sun.rmi.transport.tcp.maxConnectionThreads", Integer.MAX_VALUE));

    /** keep alive time for idle connection handler threads */
    private static final long threadKeepAliveTime = AccessController.doPrivileged(new GetLongAction("sun.rmi.transport.tcp.threadKeepAliveTime", 60000));

    /** thread pool for connection handlers */
    private static final ExecutorService connectionThreadPool = new ThreadPoolExecutor(0, maxConnectionThreads, threadKeepAliveTime, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {

        public Thread newThread(Runnable runnable) {
            return AccessController.doPrivileged(new NewThreadAction(runnable, "TCP Connection(idle)", true, true));
        }
    });

    /** total connections handled */
    private static final AtomicInteger connectionCount = new AtomicInteger(0);

    /** client host for the current thread's connection */
    private static final ThreadLocal<ConnectionHandler> threadConnectionHandler = new ThreadLocal<ConnectionHandler>();

    /** endpoints for this transport */
    private final LinkedList<TCPEndpoint> epList;

    /** number of objects exported on this transport */
    private int exportCount = 0;

    /** server socket for this transport */
    private ServerSocket server = null;

    /** table mapping endpoints to channels */
    private final Map<TCPEndpoint, Reference<TCPChannel>> channelTable = new WeakHashMap<TCPEndpoint, Reference<TCPChannel>>();

    static final RMISocketFactory defaultSocketFactory = RMISocketFactory.getDefaultSocketFactory();

    /** number of milliseconds in accepted-connection timeout.
     * Warning: this should be greater than 15 seconds (the client-side
     * timeout), and defaults to 2 hours.
     * The maximum representable value is slightly more than 24 days
     * and 20 hours.
     */
    private static final int connectionReadTimeout = AccessController.doPrivileged(new GetIntegerAction("sun.rmi.transport.tcp.readTimeout", 2 * 3600 * 1000));

    /**
     * Constructs a TCPTransport.
     */
    TCPTransport(LinkedList<TCPEndpoint> epList) {
        this.epList = epList;
        if (tcpLog.isLoggable(Log.BRIEF)) {
            tcpLog.log(Log.BRIEF, "Version = " + TransportConstants.Version + ", ep = " + getEndpoint());
        }
    }

    /**
     * Closes all cached connections in every channel subordinated to this
     * transport.  Currently, this only closes outgoing connections.
     */
    public void shedConnectionCaches() {
        List<TCPChannel> channels;
        synchronized (channelTable) {
            channels = new ArrayList<TCPChannel>(channelTable.values().size());
            for (Reference<TCPChannel> ref : channelTable.values()) {
                TCPChannel ch = ref.get();
                if (ch != null) {
                    channels.add(ch);
                }
            }
        }
        for (TCPChannel channel : channels) {
            channel.shedCache();
        }
    }

    /**
     * Returns a <I>Channel</I> that generates connections to the
     * endpoint <I>ep</I>. A Channel is an object that creates and
     * manages connections of a particular type to some particular
     * address space.
     * @param ep the endpoint to which connections will be generated.
     * @return the channel or null if the transport cannot
     * generate connections to this endpoint
     */
    public TCPChannel getChannel(Endpoint ep) {
        TCPChannel ch = null;
        if (ep instanceof TCPEndpoint) {
            synchronized (channelTable) {
                Reference<TCPChannel> ref = channelTable.get(ep);
                if (ref != null) {
                    ch = ref.get();
                }
                if (ch == null) {
                    TCPEndpoint tcpEndpoint = (TCPEndpoint) ep;
                    ch = new TCPChannel(this, tcpEndpoint);
                    channelTable.put(tcpEndpoint, new WeakReference<TCPChannel>(ch));
                }
            }
        }
        return ch;
    }

    /**
     * Removes the <I>Channel</I> that generates connections to the
     * endpoint <I>ep</I>.
     */
    public void free(Endpoint ep) {
        if (ep instanceof TCPEndpoint) {
            synchronized (channelTable) {
                Reference<TCPChannel> ref = channelTable.remove(ep);
                if (ref != null) {
                    TCPChannel channel = ref.get();
                    if (channel != null) {
                        channel.shedCache();
                    }
                }
            }
        }
    }

    /**
     * Export the object so that it can accept incoming calls.
     */
    public void exportObject(Target target) throws RemoteException {
        synchronized (this) {
            listen();
            exportCount++;
        }
        boolean ok = false;
        try {
            super.exportObject(target);
            ok = true;
        } finally {
            if (!ok) {
                synchronized (this) {
                    decrementExportCount();
                }
            }
        }
    }

    protected synchronized void targetUnexported() {
        decrementExportCount();
    }

    /**
     * Decrements the count of exported objects, closing the current
     * server socket if the count reaches zero.
     **/
    private void decrementExportCount() {
        assert Thread.holdsLock(this);
        exportCount--;
        if (exportCount == 0 && getEndpoint().getListenPort() != 0) {
            ServerSocket ss = server;
            server = null;
            try {
                ss.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Verify that the current access control context has permission to
     * accept the connection being dispatched by the current thread.
     */
    protected void checkAcceptPermission(AccessControlContext acc) {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return;
        }
        ConnectionHandler h = threadConnectionHandler.get();
        if (h == null) {
            throw new Error("checkAcceptPermission not in ConnectionHandler thread");
        }
        h.checkAcceptPermission(sm, acc);
    }

    private TCPEndpoint getEndpoint() {
        synchronized (epList) {
            return epList.getLast();
        }
    }

    /**
     * Listen on transport's endpoint.
     */
    private void listen() throws RemoteException {
        assert Thread.holdsLock(this);
        TCPEndpoint ep = getEndpoint();
        int port = ep.getPort();
        if (server == null) {
            if (tcpLog.isLoggable(Log.BRIEF)) {
                tcpLog.log(Log.BRIEF, "(port " + port + ") create server socket");
            }
            try {
                server = ep.newServerSocket();
                Thread t = AccessController.doPrivileged(new NewThreadAction(new AcceptLoop(server), "TCP Accept-" + port, true));
                t.start();
            } catch (java.net.BindException e) {
                throw new ExportException("Port already in use: " + port, e);
            } catch (IOException e) {
                throw new ExportException("Listen failed on port: " + port, e);
            }
        } else {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkListen(port);
            }
        }
    }

    /**
     * Worker for accepting connections from a server socket.
     **/
    private class AcceptLoop implements Runnable {

        private final ServerSocket serverSocket;

        private long lastExceptionTime = 0L;

        private int recentExceptionCount;

        AcceptLoop(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public void run() {
            try {
                executeAcceptLoop();
            } finally {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                }
            }
        }

        /**
         * Accepts connections from the server socket and executes
         * handlers for them in the thread pool.
         **/
        private void executeAcceptLoop() {
            if (tcpLog.isLoggable(Log.BRIEF)) {
                tcpLog.log(Log.BRIEF, "listening on port " + getEndpoint().getPort());
            }
            while (true) {
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                    InetAddress clientAddr = socket.getInetAddress();
                    String clientHost = (clientAddr != null ? clientAddr.getHostAddress() : "0.0.0.0");
                    try {
                        connectionThreadPool.execute(new ConnectionHandler(socket, clientHost));
                    } catch (RejectedExecutionException e) {
                        closeSocket(socket);
                        tcpLog.log(Log.BRIEF, "rejected connection from " + clientHost);
                    }
                } catch (Throwable t) {
                    try {
                        if (serverSocket.isClosed()) {
                            break;
                        }
                        try {
                            if (tcpLog.isLoggable(Level.WARNING)) {
                                tcpLog.log(Level.WARNING, "accept loop for " + serverSocket + " throws", t);
                            }
                        } catch (Throwable tt) {
                        }
                    } finally {
                        if (socket != null) {
                            closeSocket(socket);
                        }
                    }
                    if (!(t instanceof SecurityException)) {
                        try {
                            TCPEndpoint.shedConnectionCaches();
                        } catch (Throwable tt) {
                        }
                    }
                    if (t instanceof Exception || t instanceof OutOfMemoryError || t instanceof NoClassDefFoundError) {
                        if (!continueAfterAcceptFailure(t)) {
                            return;
                        }
                    } else {
                        throw (Error) t;
                    }
                }
            }
        }

        /**
         * Returns true if the accept loop should continue after the
         * specified exception has been caught, or false if the accept
         * loop should terminate (closing the server socket).  If
         * there is an RMIFailureHandler, this method returns the
         * result of passing the specified exception to it; otherwise,
         * this method always returns true, after sleeping to throttle
         * the accept loop if necessary.
         **/
        private boolean continueAfterAcceptFailure(Throwable t) {
            RMIFailureHandler fh = RMISocketFactory.getFailureHandler();
            if (fh != null) {
                return fh.failure(t instanceof Exception ? (Exception) t : new InvocationTargetException(t));
            } else {
                throttleLoopOnException();
                return true;
            }
        }

        /**
         * Throttles the accept loop after an exception has been
         * caught: if a burst of 10 exceptions in 5 seconds occurs,
         * then wait for 10 seconds to curb busy CPU usage.
         **/
        private void throttleLoopOnException() {
            long now = System.currentTimeMillis();
            if (lastExceptionTime == 0L || (now - lastExceptionTime) > 5000) {
                lastExceptionTime = now;
                recentExceptionCount = 0;
            } else {
                if (++recentExceptionCount >= 10) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        }
    }

    /** close socket and eat exception */
    private static void closeSocket(Socket sock) {
        try {
            sock.close();
        } catch (IOException ex) {
        }
    }

    /**
     * handleMessages decodes transport operations and handles messages
     * appropriately.  If an exception occurs during message handling,
     * the socket is closed.
     */
    void handleMessages(Connection conn, boolean persistent) {
        int port = getEndpoint().getPort();
        try {
            DataInputStream in = new DataInputStream(conn.getInputStream());
            do {
                int op = in.read();
                if (op == -1) {
                    if (tcpLog.isLoggable(Log.BRIEF)) {
                        tcpLog.log(Log.BRIEF, "(port " + port + ") connection closed");
                    }
                    break;
                }
                if (tcpLog.isLoggable(Log.BRIEF)) {
                    tcpLog.log(Log.BRIEF, "(port " + port + ") op = " + op);
                }
                switch(op) {
                    case TransportConstants.Call:
                        RemoteCall call = new StreamRemoteCall(conn);
                        if (serviceCall(call) == false) return;
                        break;
                    case TransportConstants.Ping:
                        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                        out.writeByte(TransportConstants.PingAck);
                        conn.releaseOutputStream();
                        break;
                    case TransportConstants.DGCAck:
                        DGCAckHandler.received(UID.read(in));
                        break;
                    default:
                        throw new IOException("unknown transport op " + op);
                }
            } while (persistent);
        } catch (IOException e) {
            if (tcpLog.isLoggable(Log.BRIEF)) {
                tcpLog.log(Log.BRIEF, "(port " + port + ") exception: ", e);
            }
        } finally {
            try {
                conn.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Returns the client host for the current thread's connection.  Throws
     * ServerNotActiveException if no connection is active for this thread.
     */
    public static String getClientHost() throws ServerNotActiveException {
        ConnectionHandler h = threadConnectionHandler.get();
        if (h != null) {
            return h.getClientHost();
        } else {
            throw new ServerNotActiveException("not in a remote call");
        }
    }

    /**
     * Services messages on accepted connection
     */
    private class ConnectionHandler implements Runnable {

        /** int value of "POST" in ASCII (Java's specified data formats
         *  make this once-reviled tactic again socially acceptable) */
        private static final int POST = 0x504f5354;

        /** most recently accept-authorized AccessControlContext */
        private AccessControlContext okContext;

        /** cache of accept-authorized AccessControlContexts */
        private Map<AccessControlContext, Reference<AccessControlContext>> authCache;

        /** security manager which authorized contexts in authCache */
        private SecurityManager cacheSecurityManager = null;

        private Socket socket;

        private String remoteHost;

        ConnectionHandler(Socket socket, String remoteHost) {
            this.socket = socket;
            this.remoteHost = remoteHost;
        }

        String getClientHost() {
            return remoteHost;
        }

        /**
         * Verify that the given AccessControlContext has permission to
         * accept this connection.
         */
        void checkAcceptPermission(SecurityManager sm, AccessControlContext acc) {
            if (sm != cacheSecurityManager) {
                okContext = null;
                authCache = new WeakHashMap<AccessControlContext, Reference<AccessControlContext>>();
                cacheSecurityManager = sm;
            }
            if (acc.equals(okContext) || authCache.containsKey(acc)) {
                return;
            }
            InetAddress addr = socket.getInetAddress();
            String host = (addr != null) ? addr.getHostAddress() : "*";
            sm.checkAccept(host, socket.getPort());
            authCache.put(acc, new SoftReference<AccessControlContext>(acc));
            okContext = acc;
        }

        public void run() {
            Thread t = Thread.currentThread();
            String name = t.getName();
            try {
                t.setName("RMI TCP Connection(" + connectionCount.incrementAndGet() + ")-" + remoteHost);
                run0();
            } finally {
                t.setName(name);
            }
        }

        private void run0() {
            TCPEndpoint endpoint = getEndpoint();
            int port = endpoint.getPort();
            threadConnectionHandler.set(this);
            try {
                socket.setTcpNoDelay(true);
            } catch (Exception e) {
            }
            try {
                if (connectionReadTimeout > 0) socket.setSoTimeout(connectionReadTimeout);
            } catch (Exception e) {
            }
            try {
                InputStream sockIn = socket.getInputStream();
                InputStream bufIn = sockIn.markSupported() ? sockIn : new BufferedInputStream(sockIn);
                bufIn.mark(4);
                DataInputStream in = new DataInputStream(bufIn);
                int magic = in.readInt();
                if (magic == POST) {
                    tcpLog.log(Log.BRIEF, "decoding HTTP-wrapped call");
                    bufIn.reset();
                    try {
                        socket = new HttpReceiveSocket(socket, bufIn, null);
                        remoteHost = "0.0.0.0";
                        sockIn = socket.getInputStream();
                        bufIn = new BufferedInputStream(sockIn);
                        in = new DataInputStream(bufIn);
                        magic = in.readInt();
                    } catch (IOException e) {
                        throw new RemoteException("Error HTTP-unwrapping call", e);
                    }
                }
                short version = in.readShort();
                if (magic != TransportConstants.Magic || version != TransportConstants.Version) {
                    closeSocket(socket);
                    return;
                }
                OutputStream sockOut = socket.getOutputStream();
                BufferedOutputStream bufOut = new BufferedOutputStream(sockOut);
                DataOutputStream out = new DataOutputStream(bufOut);
                int remotePort = socket.getPort();
                if (tcpLog.isLoggable(Log.BRIEF)) {
                    tcpLog.log(Log.BRIEF, "accepted socket from [" + remoteHost + ":" + remotePort + "]");
                }
                TCPEndpoint ep;
                TCPChannel ch;
                TCPConnection conn;
                byte protocol = in.readByte();
                switch(protocol) {
                    case TransportConstants.SingleOpProtocol:
                        ep = new TCPEndpoint(remoteHost, socket.getLocalPort(), endpoint.getClientSocketFactory(), endpoint.getServerSocketFactory());
                        ch = new TCPChannel(TCPTransport.this, ep);
                        conn = new TCPConnection(ch, socket, bufIn, bufOut);
                        handleMessages(conn, false);
                        break;
                    case TransportConstants.StreamProtocol:
                        out.writeByte(TransportConstants.ProtocolAck);
                        if (tcpLog.isLoggable(Log.VERBOSE)) {
                            tcpLog.log(Log.VERBOSE, "(port " + port + ") " + "suggesting " + remoteHost + ":" + remotePort);
                        }
                        out.writeUTF(remoteHost);
                        out.writeInt(remotePort);
                        out.flush();
                        String clientHost = in.readUTF();
                        int clientPort = in.readInt();
                        if (tcpLog.isLoggable(Log.VERBOSE)) {
                            tcpLog.log(Log.VERBOSE, "(port " + port + ") client using " + clientHost + ":" + clientPort);
                        }
                        ep = new TCPEndpoint(remoteHost, socket.getLocalPort(), endpoint.getClientSocketFactory(), endpoint.getServerSocketFactory());
                        ch = new TCPChannel(TCPTransport.this, ep);
                        conn = new TCPConnection(ch, socket, bufIn, bufOut);
                        handleMessages(conn, true);
                        break;
                    case TransportConstants.MultiplexProtocol:
                        if (tcpLog.isLoggable(Log.VERBOSE)) {
                            tcpLog.log(Log.VERBOSE, "(port " + port + ") accepting multiplex protocol");
                        }
                        out.writeByte(TransportConstants.ProtocolAck);
                        if (tcpLog.isLoggable(Log.VERBOSE)) {
                            tcpLog.log(Log.VERBOSE, "(port " + port + ") suggesting " + remoteHost + ":" + remotePort);
                        }
                        out.writeUTF(remoteHost);
                        out.writeInt(remotePort);
                        out.flush();
                        ep = new TCPEndpoint(in.readUTF(), in.readInt(), endpoint.getClientSocketFactory(), endpoint.getServerSocketFactory());
                        if (tcpLog.isLoggable(Log.VERBOSE)) {
                            tcpLog.log(Log.VERBOSE, "(port " + port + ") client using " + ep.getHost() + ":" + ep.getPort());
                        }
                        ConnectionMultiplexer multiplexer;
                        synchronized (channelTable) {
                            ch = getChannel(ep);
                            multiplexer = new ConnectionMultiplexer(ch, bufIn, sockOut, false);
                            ch.useMultiplexer(multiplexer);
                        }
                        multiplexer.run();
                        break;
                    default:
                        out.writeByte(TransportConstants.ProtocolNack);
                        out.flush();
                        break;
                }
            } catch (IOException e) {
                tcpLog.log(Log.BRIEF, "terminated with exception:", e);
            } finally {
                closeSocket(socket);
            }
        }
    }
}
