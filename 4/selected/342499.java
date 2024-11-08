package ch.ethz.dcg.spamato.peerato.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.*;

/**
 * Common superclass for managing connections.
 * Channels are in nonblocking mode so there is only one thread needed to manage all connections.
 * Subclasses only have to implement <code>printWelcomeMsg</code> and <code>createConnection</code>.
 * <code>createConnection</code> should return a custom connection
 * that can handle messages received by the implemented ConnectionManager.
 * 
 * @author Michelle Ackermann
 */
public abstract class ConnectionManager extends Thread implements TaskQueue {

    protected static final boolean LIMIT_INCOMING_CONNECTIONS = false;

    protected static final int ONE_SECOND = 1000;

    protected static final int ONE_MINUTE = 60 * ONE_SECOND;

    protected static ConnectionManager instance;

    private ServerSocketChannel serverChannel;

    protected Settings settings;

    protected Logger logger;

    private Timer timer;

    protected Selector selector;

    private ConnectionTimeouter connectionTimeouter;

    private LinkedList<Connection> writeQueue = new LinkedList<Connection>();

    private boolean emptyQueueLastWrite = true;

    private WriteTask writeTask;

    private LinkedList<Task> taskQueue = new LinkedList<Task>();

    private LinkedList<Connection> connectionQueue = new LinkedList<Connection>();

    private long bytesSent;

    private long bytesReceived;

    private boolean running;

    /**
	 * The instance is set to the newly created ConnectionManager.
	 * The subclass or application is responsible for calling the constructor
	 * before calling <code>getInstance</code>.
	 * @throws IOException  if an I/O error occurs while opening a new selector
	 */
    protected ConnectionManager(Settings settings) throws IOException {
        instance = this;
        logger = Logger.getLogger(settings.getLoggerName());
        this.settings = settings;
        selector = Selector.open();
        timer = new Timer();
        writeTask = new WriteTask(selector, timer);
        int portNumber = settings.getSocketPortNumber();
        serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(new InetSocketAddress(portNumber));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        connectionTimeouter = new ConnectionTimeouter(this);
        timer.schedule(connectionTimeouter, settings.getConnectionTimeoutMillis(), settings.getConnectionTimeoutMillis());
        running = true;
        logger.info("Listening to port " + portNumber);
    }

    /**
	 * Gets the instance that was created on first calling the constructor.
	 * If the constructor was not called yet, the returned instance is null.
	 * @return the instance
	 */
    public static ConnectionManager getInstance() {
        return instance;
    }

    public Timer getTimer() {
        return timer;
    }

    public Settings getSettings() {
        return settings;
    }

    public Logger getLogger() {
        return logger;
    }

    public void run() {
        while (running) {
            int n;
            performTasks();
            writeData();
            try {
                n = selector.select();
            } catch (IOException e) {
                n = 0;
                logger.log(Level.SEVERE, "Uncaught exception", e);
            }
            if (n == 0) {
                continue;
            }
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                try {
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        if (!LIMIT_INCOMING_CONNECTIONS || isAcceptable(server)) {
                            try {
                                SocketChannel channel = server.accept();
                                registerChannel(server, channel);
                            } catch (IOException e) {
                                logger.log(Level.SEVERE, "Uncaught exception", e);
                            }
                        } else {
                            SocketChannel channel;
                            try {
                                channel = server.accept();
                                channel.close();
                                logger.warning("Too many connections");
                            } catch (IOException e) {
                                logger.log(Level.SEVERE, "Uncaught exception", e);
                            }
                        }
                    }
                    if (key.isReadable()) {
                        readData(key);
                    }
                    if (key.isWritable()) {
                        writeData(key);
                    }
                } catch (CancelledKeyException e) {
                }
                it.remove();
            }
        }
    }

    public void close() {
        running = false;
        try {
            timer.cancel();
            connectionTimeouter.cancel();
        } catch (Exception e) {
        }
        try {
            selector.wakeup();
            selector.close();
        } catch (Exception e) {
        }
        try {
            serverChannel.close();
        } catch (Exception e) {
        }
    }

    public void logStatistics() {
        logger.info("Statistics: " + bytesSent + "/" + bytesReceived);
    }

    /**
	 * Calls <code>readData</code> of the connection.
	 * If an I/O error occurs the connection is closed.
	 * @param key SelectionKey that contains the readable channel and the corresponding connection
	 */
    private void readData(SelectionKey key) {
        Connection connection = (Connection) key.attachment();
        try {
            int bytesRead = connection.readData();
            bytesReceived += bytesRead;
            logger.finest(connection + " Read " + bytesRead + " bytes.");
        } catch (IOException e) {
            connection.close("IO Exception occured while reading data.");
        }
    }

    /**
	 * Retrieves the connection from the SelectionKey attachement and enqueues it in the <code>writeQueue</code>.
	 * The write operation is removed from the interested operations of the key.
	 * @param key SelectionKey that contains the writable channel and the corresponding connection
	 */
    private void writeData(SelectionKey key) {
        Connection connection = (Connection) key.attachment();
        connection.setNoInterestWrite();
        writeQueue.add(connection);
    }

    /**
	 * If the writeTask is ready the next connection is removed from the writeQueue
	 * and the <code>writeData</code> of the connection is called.
	 * If an I/O error occurs the connection is closed.
	 * Then it is calculated how much time is needed to send the bytes written.
	 * 
	 */
    private void writeData() {
        if (writeTask.ready()) {
            long writeTime = writeTask.getWriteTime();
            Iterator<Connection> iter = writeQueue.iterator();
            while (writeTime >= 0 && iter.hasNext()) {
                if (emptyQueueLastWrite) {
                    writeTime = 0;
                    emptyQueueLastWrite = false;
                }
                Connection connection = (Connection) iter.next();
                iter.remove();
                if (connection.isClosed()) {
                    continue;
                }
                connection.setInterestWrite();
                int bytesWritten = 0;
                try {
                    bytesWritten = connection.writeData();
                    bytesSent += bytesWritten;
                    logger.finest(connection + " Wrote " + bytesWritten + " bytes.");
                } catch (IOException e) {
                    connection.close("IO Exception occured while writing data.");
                }
                long writeWaitingTime = ((long) bytesWritten * (long) ONE_SECOND) / settings.getUploadSpeedMax();
                writeTime -= writeWaitingTime;
            }
            if (writeTime < 0) {
                writeTask = new WriteTask();
                writeTask.schedule(-writeTime);
            } else {
                writeTask.reset();
            }
            if (!iter.hasNext()) {
                emptyQueueLastWrite = true;
            }
        }
    }

    public void addTask(Task job) {
        synchronized (taskQueue) {
            taskQueue.add(job);
            selector.wakeup();
        }
    }

    public void addConnection(Connection connection) {
        synchronized (connectionQueue) {
            connectionQueue.add(connection);
        }
    }

    /**
	 * Removes timeouted peers from the data structures of the DownloadManager instance,
	 * ends PeerListRequests to the tracker and ChunkListRequests to the other peers
	 * when it is time to perform these tasks.
	 * @see ch.ethz.distributor.common.ConnectionManager#performTimerTasks()
	 */
    public void performTasks() {
        synchronized (taskQueue) {
            Iterator<Task> iter = taskQueue.listIterator();
            while (iter.hasNext()) {
                Task job = (Task) iter.next();
                iter.remove();
                job.perform();
            }
        }
        synchronized (connectionQueue) {
            Iterator<Connection> iter = connectionQueue.listIterator();
            while (iter.hasNext() && !maxConnectionsReached()) {
                Connection connection = (Connection) iter.next();
                iter.remove();
                try {
                    connection.connect();
                } catch (IOException e) {
                    connection.close("IO Exception occured while connecting.");
                }
            }
        }
    }

    /**
	 * Registers the given channel in the selector for reads and writes.
	 * The channel is set to non-blocking mode.
	 * A new connection is created for the channel.
	 * @param server ServerSocketChannel from which the channel was accepted
	 * @param channel Channel that will be registred
	 */
    private void registerChannel(ServerSocketChannel server, SocketChannel channel) {
        if (channel == null) {
            return;
        }
        registerChannel(channel, createConnection(server, channel), SelectionKey.OP_READ);
    }

    /**
	 * Registers the given channel in the selector for reads and writes.
	 * The channel is set to non-blocking mode.
	 * @param channel Channel that will be registred
	 * @param connection Connection around the Channel that will be attached to the selection key
	 */
    void registerChannel(SocketChannel channel, Connection connection, int operation) {
        if (channel == null) {
            return;
        }
        try {
            channel.configureBlocking(false);
            SelectionKey key = channel.register(selector, operation, connection);
            connection.setSelectionKey(key);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Uncaught exception", e);
        }
        logger.info(connection + " Registred new channel");
    }

    /**
	 * @return <code>true</code> if the maximum number of connections is reached, <code>false</code> otherwise
	 */
    public boolean maxConnectionsReached() {
        return selector.keys().size() >= settings.getConnectionsMax();
    }

    /**
	 * A channel is acceptable if maximum connections hasn't been reached.
	 * @param server ServerSocketChannel of the channel to accept
	 * @return <code>true</code> if the maximum number of connections is reached, <code>false</code> otherwise
	 */
    protected boolean isAcceptable(ServerSocketChannel server) {
        return !maxConnectionsReached();
    }

    /**
	 * Closes unused connections.
	 */
    void removeOldConnections() {
        ArrayList<Connection> timeoutedConnections = new ArrayList<Connection>();
        for (SelectionKey key : selector.keys()) {
            Connection connection = (Connection) key.attachment();
            if (connection != null && connection.isTimeouted()) {
                timeoutedConnections.add(connection);
            }
        }
        for (int i = 0; i < timeoutedConnections.size(); i++) {
            Connection connection = (Connection) timeoutedConnections.get(i);
            connection.close("Timeouted.");
        }
    }

    /**
	 * Creates a connection that wraps the given channel.
	 * @param channel channel that needs a Connection around
	 * @return a custom connection wrapping the channel
	 */
    protected abstract Connection createConnection(ServerSocketChannel server, SocketChannel channel);
}
