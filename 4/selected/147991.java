package de.kapsi.net.daap.nio;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.kapsi.net.daap.DaapConfig;
import de.kapsi.net.daap.DaapConnection;
import de.kapsi.net.daap.DaapServer;
import de.kapsi.net.daap.DaapSession;
import de.kapsi.net.daap.DaapStreamException;
import de.kapsi.net.daap.Library;
import de.kapsi.net.daap.SessionId;

/**
 * A DAAP server written with NIO and a single Thread.
 *
 * @author  Roger Kapsi
 */
public class DaapServerNIO extends DaapServer<DaapConnectionNIO> {

    private static final Log LOG = LogFactory.getLog(DaapServerNIO.class);

    /** Selector.select() timeout */
    private static final long TIMEOUT = 250;

    /** The ServerSocket */
    private ServerSocketChannel ssc = null;

    /** Selector for ServerSocket and Sockets */
    private Selector selector = null;

    /** Flag to indicate that all clients shall be disconnected */
    private boolean disconnectAll = false;

    /** 
     * Flag to indicate there are Library updates 
     * available in the queue 
     */
    private boolean update = false;

    /**
     * Creates a new DAAP server with Library and {@see SimpleConfig}
     * 
     * @param library a Library
     */
    public DaapServerNIO(Library library) {
        this(library, new DaapConfig());
    }

    /**
     * Creates a new DAAP server with Library and DaapConfig
     * 
     * @param library a Library
     * @param config a DaapConfig
     */
    public DaapServerNIO(Library library, DaapConfig config) {
        super(library, config);
    }

    /**
     * Binds this server to the SocketAddress supplied by DaapConfig
     * 
     * @throws IOException
     */
    public void bind() throws IOException {
        SocketAddress bindAddr = config.getInetSocketAddress();
        int backlog = config.getBacklog();
        try {
            ssc = ServerSocketChannel.open();
            ServerSocket socket = ssc.socket();
            socket.setReuseAddress(false);
            try {
                socket.bind(bindAddr, backlog);
            } catch (SocketException err) {
                throw new BindException(err.getMessage());
            }
            ssc.configureBlocking(false);
            if (LOG.isInfoEnabled()) {
                LOG.info("DaapServerNIO bound to " + bindAddr);
            }
        } catch (IOException err) {
            close();
            throw err;
        }
    }

    protected synchronized void update() {
        update = true;
    }

    /**
    * Stops the DAAP Server
    */
    public synchronized void stop() {
        running = false;
    }

    /**
     * Cloeses the server and releases all resources
     */
    private synchronized void close() {
        running = false;
        update = false;
        disconnectAll = false;
        if (selector != null) {
            for (SelectionKey key : selector.keys()) cancel(key);
            try {
                selector.close();
            } catch (IOException err) {
                LOG.error("Selector.close()", err);
            }
            selector = null;
        }
        if (ssc != null) {
            try {
                ssc.close();
            } catch (IOException err) {
                LOG.error("ServerSocketChannel.close()", err);
            }
            ssc = null;
        }
        sessionIds.clear();
        connections.clear();
        libraryQueue.clear();
    }

    /**
     * Disconnects all DAAP and Stream connections
     */
    public synchronized void disconnectAll() {
        disconnectAll = true;
    }

    /**
     * Cancel SelesctionKey, close Channel and "free" the attachment
     */
    private void cancel(SelectionKey sk) {
        sk.cancel();
        SelectableChannel channel = sk.channel();
        try {
            channel.close();
        } catch (IOException err) {
            LOG.error("Channel.close()", err);
        }
        DaapConnection connection = (DaapConnection) sk.attachment();
        if (connection != null) {
            closeConnection(connection);
        }
    }

    protected void closeConnection(DaapConnection connection) {
        DaapSession session = connection.getSession(false);
        if (session != null) {
            destroySessionId(session.getSessionId());
        }
        connection.close();
        try {
            removeConnection(connection);
        } catch (IllegalStateException err) {
            LOG.error(err);
        }
    }

    /**
     * Accept an icoming connection
     * 
     * @throws IOException
     */
    private void processAccept(SelectionKey sk) throws IOException {
        if (!sk.isValid()) return;
        ServerSocketChannel ssc = (ServerSocketChannel) sk.channel();
        SocketChannel channel = ssc.accept();
        if (channel == null) return;
        try {
            Socket socket = channel.socket();
            if (channel.isOpen() && accept(socket.getInetAddress())) {
                channel.configureBlocking(false);
                DaapConnectionNIO connection = new DaapConnectionNIO(this, channel);
                channel.register(selector, SelectionKey.OP_READ, connection);
                addPendingConnection(connection);
            } else {
                channel.close();
            }
        } catch (IOException err) {
            LOG.error(err);
            try {
                channel.close();
            } catch (IOException iox) {
            }
        }
    }

    /**
     * Read data
     * 
     * @throws IOException
     */
    private void processRead(SelectionKey sk) throws IOException {
        if (!sk.isValid()) return;
        DaapConnectionNIO connection = (DaapConnectionNIO) sk.attachment();
        boolean keepAlive = false;
        keepAlive = connection.read();
        if (keepAlive) {
            sk.interestOps(connection.interrestOps());
        } else {
            cancel(sk);
        }
    }

    /**
     * Write data
     * 
     * @throws IOException
     */
    private void processWrite(SelectionKey sk) throws IOException {
        if (!sk.isValid()) return;
        DaapConnectionNIO connection = (DaapConnectionNIO) sk.attachment();
        boolean keepAlive = false;
        try {
            keepAlive = connection.write();
        } catch (DaapStreamException err) {
            keepAlive = false;
            LOG.error(err);
        }
        if (keepAlive) {
            sk.interestOps(connection.interrestOps());
        } else {
            cancel(sk);
        }
    }

    /**
     * Disconnects all clients from this server
     */
    private void processDisconnectAll() {
        Iterator it = selector.keys().iterator();
        while (it.hasNext()) {
            SelectionKey sk = (SelectionKey) it.next();
            SelectableChannel channel = sk.channel();
            if (channel instanceof SocketChannel) {
                cancel(sk);
            }
        }
        libraryQueue.clear();
    }

    /**
     * Notify all clients about an update of the Library
     */
    private void processUpdate() {
        for (DaapConnectionNIO connection : getDaapConnections()) {
            SelectionKey sk = connection.getChannel().keyFor(selector);
            try {
                for (int i = 0; i < libraryQueue.size(); i++) {
                    connection.enqueueLibrary(libraryQueue.get(i));
                }
                connection.update();
                if (sk.isValid()) {
                    try {
                        sk.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    } catch (CancelledKeyException err) {
                        cancel(sk);
                        LOG.error("SelectionKey.interestOps()", err);
                    }
                }
            } catch (ClosedChannelException err) {
                cancel(sk);
                LOG.error("DaapConnection.update()", err);
            } catch (IOException err) {
                cancel(sk);
                LOG.error("DaapConnection.update()", err);
            }
        }
        libraryQueue.clear();
    }

    /**
     * 1) Disconnect all connections that are in undefined state and
     * that have exceeded their timeout.
     * 
     * 2) Empty the libraryQueue of daap connections if they've 
     * exceeded their timeout. Some clients do not support live updates
     * and this will prevent us from running out of memory if the client 
     * doesn't fetch its updates).
     */
    protected void processTimeout() {
        for (DaapConnectionNIO connection : getPendingConnections()) {
            if (connection.timeout()) {
                cancelConnection(connection);
            }
        }
        for (DaapConnectionNIO connection : getDaapConnections()) {
            if (connection.timeout()) {
                connection.clearLibraryQueue();
            }
        }
    }

    protected void cancelConnection(DaapConnectionNIO connection) {
        SelectionKey sk = connection.getChannel().keyFor(selector);
        cancel(sk);
    }

    /**
     * The actual NIO run loop
     * 
     * @throws IOException
     */
    private void process() throws IOException {
        int n = -1;
        running = true;
        update = false;
        disconnectAll = false;
        while (running) {
            try {
                n = selector.select(TIMEOUT);
            } catch (NullPointerException err) {
                continue;
            } catch (CancelledKeyException err) {
                continue;
            }
            synchronized (this) {
                if (!running) {
                    break;
                }
                if (disconnectAll) {
                    processDisconnectAll();
                    disconnectAll = false;
                    continue;
                }
                if (update) {
                    processUpdate();
                    update = false;
                }
                if (n > 0) {
                    for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext() && running; ) {
                        SelectionKey sk = it.next();
                        it.remove();
                        try {
                            if (sk.isAcceptable()) {
                                processAccept(sk);
                            } else {
                                if (sk.isReadable()) {
                                    try {
                                        processRead(sk);
                                    } catch (IOException err) {
                                        cancel(sk);
                                        LOG.error("An exception occured in processRead()", err);
                                    }
                                }
                                if (sk.isWritable()) {
                                    try {
                                        processWrite(sk);
                                    } catch (IOException err) {
                                        cancel(sk);
                                        LOG.error("An exception occured in processWrite()", err);
                                    }
                                }
                            }
                        } catch (CancelledKeyException err) {
                            continue;
                        }
                    }
                }
                processTimeout();
            }
        }
    }

    /**
     * The run loop
     */
    public void run() {
        try {
            if (running) {
                LOG.error("DaapServerNIO is already running.");
                return;
            }
            selector = Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            process();
        } catch (IOException err) {
            LOG.error(err);
            throw new RuntimeException(err);
        } finally {
            close();
        }
    }

    protected synchronized DaapConnectionNIO getAudioConnection(SessionId sessionId) {
        return super.getAudioConnection(sessionId);
    }

    protected synchronized DaapConnectionNIO getDaapConnection(SessionId sessionId) {
        return super.getDaapConnection(sessionId);
    }

    protected synchronized boolean isSessionIdValid(SessionId sessionId) {
        return super.isSessionIdValid(sessionId);
    }

    protected synchronized boolean updateConnection(DaapConnectionNIO connection) {
        return super.updateConnection(connection);
    }
}
