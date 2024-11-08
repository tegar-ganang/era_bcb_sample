package org.apache.hadoop.ipc;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.ObjectWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.ipc.metrics.RpcMetrics;
import org.apache.hadoop.security.UserGroupInformation;

/** An abstract IPC service.  IPC calls take a single {@link Writable} as a
 * parameter, and return a {@link Writable} as their value.  A service runs on
 * a port and is defined by a parameter class and a value class.
 * 
 * @see Client
 */
public abstract class Server {

    /**
   * The first four bytes of Hadoop RPC connections
   */
    public static final ByteBuffer HEADER = ByteBuffer.wrap("hrpc".getBytes());

    public static final byte CURRENT_VERSION = 2;

    /**
   * How many calls/handler are allowed in the queue.
   */
    private static final int MAX_QUEUE_SIZE_PER_HANDLER = 100;

    public static final Log LOG = LogFactory.getLog(Server.class);

    private static final ThreadLocal<Server> SERVER = new ThreadLocal<Server>();

    /** Returns the server instance called under or null.  May be called under
   * {@link #call(Writable, long)} implementations, and under {@link Writable}
   * methods of paramters and return values.  Permits applications to access
   * the server context.*/
    public static Server get() {
        return SERVER.get();
    }

    /** This is set to Call object before Handler invokes an RPC and reset
   * after the call returns.
   */
    private static final ThreadLocal<Call> CurCall = new ThreadLocal<Call>();

    /** Returns the remote side ip address when invoked inside an RPC 
   *  Returns null incase of an error.
   */
    public static InetAddress getRemoteIp() {
        Call call = CurCall.get();
        if (call != null) {
            return call.connection.socket.getInetAddress();
        }
        return null;
    }

    /** Returns remote address as a string when invoked inside an RPC.
   *  Returns null in case of an error.
   */
    public static String getRemoteAddress() {
        InetAddress addr = getRemoteIp();
        return (addr == null) ? null : addr.getHostAddress();
    }

    private String bindAddress;

    private int port;

    private int handlerCount;

    private Class<? extends Writable> paramClass;

    private int maxIdleTime;

    private int thresholdIdleConnections;

    int maxConnectionsToNuke;

    protected RpcMetrics rpcMetrics;

    private Configuration conf;

    private int maxQueueSize;

    private int socketSendBufferSize;

    private final boolean tcpNoDelay;

    private volatile boolean running = true;

    private BlockingQueue<Call> callQueue;

    private List<Connection> connectionList = Collections.synchronizedList(new LinkedList<Connection>());

    private Listener listener = null;

    private Responder responder = null;

    private int numConnections = 0;

    private Handler[] handlers = null;

    /**
   * A convenience method to bind to a given address and report 
   * better exceptions if the address is not a valid host.
   * @param socket the socket to bind
   * @param address the address to bind to
   * @param backlog the number of connections allowed in the queue
   * @throws BindException if the address can't be bound
   * @throws UnknownHostException if the address isn't a valid host name
   * @throws IOException other random errors from bind
   */
    public static void bind(ServerSocket socket, InetSocketAddress address, int backlog) throws IOException {
        try {
            socket.bind(address, backlog);
        } catch (BindException e) {
            BindException bindException = new BindException("Problem binding to " + address + " : " + e.getMessage());
            bindException.initCause(e);
            throw bindException;
        } catch (SocketException e) {
            if ("Unresolved address".equals(e.getMessage())) {
                throw new UnknownHostException("Invalid hostname for server: " + address.getHostName());
            } else {
                throw e;
            }
        }
    }

    /** A call queued for handling. */
    private static class Call {

        private int id;

        private Writable param;

        private Connection connection;

        private long timestamp;

        private ByteBuffer response;

        public Call(int id, Writable param, Connection connection) {
            this.id = id;
            this.param = param;
            this.connection = connection;
            this.timestamp = System.currentTimeMillis();
            this.response = null;
        }

        @Override
        public String toString() {
            return param.toString() + " from " + connection.toString();
        }

        public void setResponse(ByteBuffer response) {
            this.response = response;
        }
    }

    /** Listens on the socket. Creates jobs for the handler threads*/
    private class Listener extends Thread {

        private ServerSocketChannel acceptChannel = null;

        private Selector selector = null;

        private InetSocketAddress address;

        private Random rand = new Random();

        private long lastCleanupRunTime = 0;

        private long cleanupInterval = 10000;

        private int backlogLength = conf.getInt("ipc.server.listen.queue.size", 128);

        public Listener() throws IOException {
            address = new InetSocketAddress(bindAddress, port);
            acceptChannel = ServerSocketChannel.open();
            acceptChannel.configureBlocking(false);
            bind(acceptChannel.socket(), address, backlogLength);
            port = acceptChannel.socket().getLocalPort();
            selector = Selector.open();
            acceptChannel.register(selector, SelectionKey.OP_ACCEPT);
            this.setName("IPC Server listener on " + port);
            this.setDaemon(true);
        }

        /** cleanup connections from connectionList. Choose a random range
     * to scan and also have a limit on the number of the connections
     * that will be cleanedup per run. The criteria for cleanup is the time
     * for which the connection was idle. If 'force' is true then all 
     * connections will be looked at for the cleanup.
     */
        private void cleanupConnections(boolean force) {
            if (force || numConnections > thresholdIdleConnections) {
                long currentTime = System.currentTimeMillis();
                if (!force && (currentTime - lastCleanupRunTime) < cleanupInterval) {
                    return;
                }
                int start = 0;
                int end = numConnections - 1;
                if (!force) {
                    start = rand.nextInt() % numConnections;
                    end = rand.nextInt() % numConnections;
                    int temp;
                    if (end < start) {
                        temp = start;
                        start = end;
                        end = temp;
                    }
                }
                int i = start;
                int numNuked = 0;
                while (i <= end) {
                    Connection c;
                    synchronized (connectionList) {
                        try {
                            c = connectionList.get(i);
                        } catch (Exception e) {
                            return;
                        }
                    }
                    if (c.timedOut(currentTime)) {
                        if (LOG.isDebugEnabled()) LOG.debug(getName() + ": disconnecting client " + c.getHostAddress());
                        closeConnection(c);
                        numNuked++;
                        end--;
                        c = null;
                        if (!force && numNuked == maxConnectionsToNuke) break;
                    } else i++;
                }
                lastCleanupRunTime = System.currentTimeMillis();
            }
        }

        @Override
        public void run() {
            LOG.info(getName() + ": starting");
            SERVER.set(Server.this);
            while (running) {
                SelectionKey key = null;
                try {
                    selector.select();
                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        key = iter.next();
                        iter.remove();
                        try {
                            if (key.isValid()) {
                                if (key.isAcceptable()) doAccept(key); else if (key.isReadable()) doRead(key);
                            }
                        } catch (IOException e) {
                        }
                        key = null;
                    }
                } catch (OutOfMemoryError e) {
                    LOG.warn("Out of Memory in server select", e);
                    closeCurrentConnection(key, e);
                    cleanupConnections(true);
                    try {
                        Thread.sleep(60000);
                    } catch (Exception ie) {
                    }
                } catch (InterruptedException e) {
                    if (running) {
                        LOG.info(getName() + " caught: " + StringUtils.stringifyException(e));
                    }
                } catch (Exception e) {
                    closeCurrentConnection(key, e);
                }
                cleanupConnections(false);
            }
            LOG.info("Stopping " + this.getName());
            synchronized (this) {
                try {
                    acceptChannel.close();
                    selector.close();
                } catch (IOException e) {
                }
                selector = null;
                acceptChannel = null;
                while (!connectionList.isEmpty()) {
                    closeConnection(connectionList.remove(0));
                }
            }
        }

        private void closeCurrentConnection(SelectionKey key, Throwable e) {
            if (key != null) {
                Connection c = (Connection) key.attachment();
                if (c != null) {
                    if (LOG.isDebugEnabled()) LOG.debug(getName() + ": disconnecting client " + c.getHostAddress());
                    closeConnection(c);
                    c = null;
                }
            }
        }

        InetSocketAddress getAddress() {
            return (InetSocketAddress) acceptChannel.socket().getLocalSocketAddress();
        }

        void doAccept(SelectionKey key) throws IOException, OutOfMemoryError {
            Connection c = null;
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            for (int i = 0; i < 10; i++) {
                SocketChannel channel = server.accept();
                if (channel == null) return;
                channel.configureBlocking(false);
                channel.socket().setTcpNoDelay(tcpNoDelay);
                SelectionKey readKey = channel.register(selector, SelectionKey.OP_READ);
                c = new Connection(readKey, channel, System.currentTimeMillis());
                readKey.attach(c);
                synchronized (connectionList) {
                    connectionList.add(numConnections, c);
                    numConnections++;
                }
                if (LOG.isDebugEnabled()) LOG.debug("Server connection from " + c.toString() + "; # active connections: " + numConnections + "; # queued calls: " + callQueue.size());
            }
        }

        void doRead(SelectionKey key) throws InterruptedException {
            int count = 0;
            Connection c = (Connection) key.attachment();
            if (c == null) {
                return;
            }
            c.setLastContact(System.currentTimeMillis());
            try {
                count = c.readAndProcess();
            } catch (InterruptedException ieo) {
                throw ieo;
            } catch (Exception e) {
                LOG.debug(getName() + ": readAndProcess threw exception " + e + ". Count of bytes read: " + count, e);
                count = -1;
            }
            if (count < 0) {
                if (LOG.isDebugEnabled()) LOG.debug(getName() + ": disconnecting client " + c.getHostAddress() + ". Number of active connections: " + numConnections);
                closeConnection(c);
                c = null;
            } else {
                c.setLastContact(System.currentTimeMillis());
            }
        }

        synchronized void doStop() {
            if (selector != null) {
                selector.wakeup();
                Thread.yield();
            }
            if (acceptChannel != null) {
                try {
                    acceptChannel.socket().close();
                } catch (IOException e) {
                    LOG.info(getName() + ":Exception in closing listener socket. " + e);
                }
            }
        }
    }

    private class Responder extends Thread {

        private Selector writeSelector;

        private int pending;

        static final int PURGE_INTERVAL = 900000;

        Responder() throws IOException {
            this.setName("IPC Server Responder");
            this.setDaemon(true);
            writeSelector = Selector.open();
            pending = 0;
        }

        @Override
        public void run() {
            LOG.info(getName() + ": starting");
            SERVER.set(Server.this);
            long lastPurgeTime = 0;
            while (running) {
                try {
                    waitPending();
                    writeSelector.select(PURGE_INTERVAL);
                    Iterator<SelectionKey> iter = writeSelector.selectedKeys().iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        try {
                            if (key.isValid() && key.isWritable()) {
                                doAsyncWrite(key);
                            }
                        } catch (IOException e) {
                            LOG.info(getName() + ": doAsyncWrite threw exception " + e);
                        }
                    }
                    long now = System.currentTimeMillis();
                    if (now < lastPurgeTime + PURGE_INTERVAL) {
                        continue;
                    }
                    lastPurgeTime = now;
                    LOG.debug("Checking for old call responses.");
                    ArrayList<Call> calls;
                    synchronized (writeSelector.keys()) {
                        calls = new ArrayList<Call>(writeSelector.keys().size());
                        iter = writeSelector.keys().iterator();
                        while (iter.hasNext()) {
                            SelectionKey key = iter.next();
                            Call call = (Call) key.attachment();
                            if (call != null && key.channel() == call.connection.channel) {
                                calls.add(call);
                            }
                        }
                    }
                    for (Call call : calls) {
                        try {
                            doPurge(call, now);
                        } catch (IOException e) {
                            LOG.warn("Error in purging old calls " + e);
                        }
                    }
                } catch (OutOfMemoryError e) {
                    LOG.warn("Out of Memory in server select", e);
                    try {
                        Thread.sleep(60000);
                    } catch (Exception ie) {
                    }
                } catch (Exception e) {
                    LOG.warn("Exception in Responder " + StringUtils.stringifyException(e));
                }
            }
            LOG.info("Stopping " + this.getName());
        }

        private void doAsyncWrite(SelectionKey key) throws IOException {
            Call call = (Call) key.attachment();
            if (call == null) {
                return;
            }
            if (key.channel() != call.connection.channel) {
                throw new IOException("doAsyncWrite: bad channel");
            }
            synchronized (call.connection.responseQueue) {
                if (processResponse(call.connection.responseQueue, false)) {
                    try {
                        key.interestOps(0);
                    } catch (CancelledKeyException e) {
                        LOG.warn("Exception while changing ops : " + e);
                    }
                }
            }
        }

        private void doPurge(Call call, long now) throws IOException {
            LinkedList<Call> responseQueue = call.connection.responseQueue;
            synchronized (responseQueue) {
                Iterator<Call> iter = responseQueue.listIterator(0);
                while (iter.hasNext()) {
                    call = iter.next();
                    if (now > call.timestamp + PURGE_INTERVAL) {
                        closeConnection(call.connection);
                        break;
                    }
                }
            }
        }

        private boolean processResponse(LinkedList<Call> responseQueue, boolean inHandler) throws IOException {
            boolean error = true;
            boolean done = false;
            int numElements = 0;
            Call call = null;
            try {
                synchronized (responseQueue) {
                    numElements = responseQueue.size();
                    if (numElements == 0) {
                        error = false;
                        return true;
                    }
                    call = responseQueue.removeFirst();
                    SocketChannel channel = call.connection.channel;
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(getName() + ": responding to #" + call.id + " from " + call.connection);
                    }
                    int numBytes = channelWrite(channel, call.response);
                    if (numBytes < 0) {
                        return true;
                    }
                    if (!call.response.hasRemaining()) {
                        call.connection.decRpcCount();
                        if (numElements == 1) {
                            done = true;
                        } else {
                            done = false;
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(getName() + ": responding to #" + call.id + " from " + call.connection + " Wrote " + numBytes + " bytes.");
                        }
                    } else {
                        call.connection.responseQueue.addFirst(call);
                        if (inHandler) {
                            call.timestamp = System.currentTimeMillis();
                            incPending();
                            try {
                                writeSelector.wakeup();
                                channel.register(writeSelector, SelectionKey.OP_WRITE, call);
                            } catch (ClosedChannelException e) {
                                done = true;
                            } finally {
                                decPending();
                            }
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(getName() + ": responding to #" + call.id + " from " + call.connection + " Wrote partial " + numBytes + " bytes.");
                        }
                    }
                    error = false;
                }
            } finally {
                if (error && call != null) {
                    LOG.warn(getName() + ", call " + call + ": output error");
                    done = true;
                    closeConnection(call.connection);
                }
            }
            return done;
        }

        void doRespond(Call call) throws IOException {
            synchronized (call.connection.responseQueue) {
                call.connection.responseQueue.addLast(call);
                if (call.connection.responseQueue.size() == 1) {
                    processResponse(call.connection.responseQueue, true);
                }
            }
        }

        private synchronized void incPending() {
            pending++;
        }

        private synchronized void decPending() {
            pending--;
            notify();
        }

        private synchronized void waitPending() throws InterruptedException {
            while (pending > 0) {
                wait();
            }
        }
    }

    /** Reads calls from a connection and queues them for handling. */
    private class Connection {

        private boolean versionRead = false;

        private boolean headerRead = false;

        private SocketChannel channel;

        private ByteBuffer data;

        private ByteBuffer dataLengthBuffer;

        private LinkedList<Call> responseQueue;

        private volatile int rpcCount = 0;

        private long lastContact;

        private int dataLength;

        private Socket socket;

        private String hostAddress;

        private int remotePort;

        private UserGroupInformation ticket = null;

        public Connection(SelectionKey key, SocketChannel channel, long lastContact) {
            this.channel = channel;
            this.lastContact = lastContact;
            this.data = null;
            this.dataLengthBuffer = ByteBuffer.allocate(4);
            this.socket = channel.socket();
            InetAddress addr = socket.getInetAddress();
            if (addr == null) {
                this.hostAddress = "*Unknown*";
            } else {
                this.hostAddress = addr.getHostAddress();
            }
            this.remotePort = socket.getPort();
            this.responseQueue = new LinkedList<Call>();
            if (socketSendBufferSize != 0) {
                try {
                    socket.setSendBufferSize(socketSendBufferSize);
                } catch (IOException e) {
                    LOG.warn("Connection: unable to set socket send buffer size to " + socketSendBufferSize);
                }
            }
        }

        @Override
        public String toString() {
            return getHostAddress() + ":" + remotePort;
        }

        public String getHostAddress() {
            return hostAddress;
        }

        public void setLastContact(long lastContact) {
            this.lastContact = lastContact;
        }

        public long getLastContact() {
            return lastContact;
        }

        private boolean isIdle() {
            return rpcCount == 0;
        }

        private void decRpcCount() {
            rpcCount--;
        }

        private void incRpcCount() {
            rpcCount++;
        }

        private boolean timedOut(long currentTime) {
            if (isIdle() && currentTime - lastContact > maxIdleTime) return true;
            return false;
        }

        public int readAndProcess() throws IOException, InterruptedException {
            while (true) {
                int count = -1;
                if (dataLengthBuffer.remaining() > 0) {
                    count = channelRead(channel, dataLengthBuffer);
                    if (count < 0 || dataLengthBuffer.remaining() > 0) return count;
                }
                if (!versionRead) {
                    ByteBuffer versionBuffer = ByteBuffer.allocate(1);
                    count = channelRead(channel, versionBuffer);
                    if (count <= 0) {
                        return count;
                    }
                    int version = versionBuffer.get(0);
                    dataLengthBuffer.flip();
                    if (!HEADER.equals(dataLengthBuffer) || version != CURRENT_VERSION) {
                        LOG.warn("Incorrect header or version mismatch from " + hostAddress + ":" + remotePort + " got version " + version + " expected version " + CURRENT_VERSION);
                        return -1;
                    }
                    dataLengthBuffer.clear();
                    versionRead = true;
                    continue;
                }
                if (data == null) {
                    dataLengthBuffer.flip();
                    dataLength = dataLengthBuffer.getInt();
                    if (dataLength == Client.PING_CALL_ID) {
                        dataLengthBuffer.clear();
                        return 0;
                    }
                    data = ByteBuffer.allocate(dataLength);
                    incRpcCount();
                }
                count = channelRead(channel, data);
                if (data.remaining() == 0) {
                    dataLengthBuffer.clear();
                    data.flip();
                    if (headerRead) {
                        processData();
                        data = null;
                        return count;
                    } else {
                        processHeader();
                        headerRead = true;
                        data = null;
                        continue;
                    }
                }
                return count;
            }
        }

        private void processHeader() throws IOException {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data.array()));
            ticket = (UserGroupInformation) ObjectWritable.readObject(in, conf);
        }

        private void processData() throws IOException, InterruptedException {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data.array()));
            int id = dis.readInt();
            if (LOG.isDebugEnabled()) LOG.debug(" got #" + id);
            Writable param = ReflectionUtils.newInstance(paramClass, conf);
            param.readFields(dis);
            Call call = new Call(id, param, this);
            callQueue.put(call);
        }

        private synchronized void close() throws IOException {
            data = null;
            dataLengthBuffer = null;
            if (!channel.isOpen()) return;
            try {
                socket.shutdownOutput();
            } catch (Exception e) {
            }
            if (channel.isOpen()) {
                try {
                    channel.close();
                } catch (Exception e) {
                }
            }
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    /** Handles queued calls . */
    private class Handler extends Thread {

        public Handler(int instanceNumber) {
            this.setDaemon(true);
            this.setName("IPC Server handler " + instanceNumber + " on " + port);
        }

        @Override
        public void run() {
            LOG.info(getName() + ": starting");
            SERVER.set(Server.this);
            ByteArrayOutputStream buf = new ByteArrayOutputStream(10240);
            while (running) {
                try {
                    Call call = callQueue.take();
                    if (LOG.isDebugEnabled()) LOG.debug(getName() + ": has #" + call.id + " from " + call.connection);
                    String errorClass = null;
                    String error = null;
                    Writable value = null;
                    CurCall.set(call);
                    UserGroupInformation previous = UserGroupInformation.getCurrentUGI();
                    UserGroupInformation.setCurrentUGI(call.connection.ticket);
                    try {
                        value = call(call.param, call.timestamp);
                    } catch (Throwable e) {
                        LOG.info(getName() + ", call " + call + ": error: " + e, e);
                        errorClass = e.getClass().getName();
                        error = StringUtils.stringifyException(e);
                    }
                    UserGroupInformation.setCurrentUGI(previous);
                    CurCall.set(null);
                    buf.reset();
                    DataOutputStream out = new DataOutputStream(buf);
                    out.writeInt(call.id);
                    out.writeBoolean(error != null);
                    if (error == null) {
                        value.write(out);
                    } else {
                        WritableUtils.writeString(out, errorClass);
                        WritableUtils.writeString(out, error);
                    }
                    call.setResponse(ByteBuffer.wrap(buf.toByteArray()));
                    responder.doRespond(call);
                } catch (InterruptedException e) {
                    if (running) {
                        LOG.info(getName() + " caught: " + StringUtils.stringifyException(e));
                    }
                } catch (Exception e) {
                    LOG.info(getName() + " caught: " + StringUtils.stringifyException(e));
                }
            }
            LOG.info(getName() + ": exiting");
        }
    }

    protected Server(String bindAddress, int port, Class<? extends Writable> paramClass, int handlerCount, Configuration conf) throws IOException {
        this(bindAddress, port, paramClass, handlerCount, conf, Integer.toString(port));
    }

    /** Constructs a server listening on the named port and address.  Parameters passed must
   * be of the named class.  The <code>handlerCount</handlerCount> determines
   * the number of handler threads that will be used to process calls.
   * 
   */
    protected Server(String bindAddress, int port, Class<? extends Writable> paramClass, int handlerCount, Configuration conf, String serverName) throws IOException {
        this.bindAddress = bindAddress;
        this.conf = conf;
        this.port = port;
        this.paramClass = paramClass;
        this.handlerCount = handlerCount;
        this.socketSendBufferSize = 0;
        this.maxQueueSize = handlerCount * MAX_QUEUE_SIZE_PER_HANDLER;
        this.callQueue = new LinkedBlockingQueue<Call>(maxQueueSize);
        this.maxIdleTime = 2 * conf.getInt("ipc.client.connection.maxidletime", 1000);
        this.maxConnectionsToNuke = conf.getInt("ipc.client.kill.max", 10);
        this.thresholdIdleConnections = conf.getInt("ipc.client.idlethreshold", 4000);
        listener = new Listener();
        this.port = listener.getAddress().getPort();
        this.rpcMetrics = new RpcMetrics(serverName, Integer.toString(this.port), this);
        this.tcpNoDelay = conf.getBoolean("ipc.server.tcpnodelay", false);
        responder = new Responder();
    }

    private void closeConnection(Connection connection) {
        synchronized (connectionList) {
            if (connectionList.remove(connection)) numConnections--;
        }
        try {
            connection.close();
        } catch (IOException e) {
        }
    }

    /** Sets the socket buffer size used for responding to RPCs */
    public void setSocketSendBufSize(int size) {
        this.socketSendBufferSize = size;
    }

    /** Starts the service.  Must be called before any calls will be handled. */
    public synchronized void start() throws IOException {
        responder.start();
        listener.start();
        handlers = new Handler[handlerCount];
        for (int i = 0; i < handlerCount; i++) {
            handlers[i] = new Handler(i);
            handlers[i].start();
        }
    }

    /** Stops the service.  No new calls will be handled after this is called. */
    public synchronized void stop() {
        LOG.info("Stopping server on " + port);
        running = false;
        if (handlers != null) {
            for (int i = 0; i < handlerCount; i++) {
                if (handlers[i] != null) {
                    handlers[i].interrupt();
                }
            }
        }
        listener.interrupt();
        listener.doStop();
        responder.interrupt();
        notifyAll();
        if (this.rpcMetrics != null) {
            this.rpcMetrics.shutdown();
        }
    }

    /** Wait for the server to be stopped.
   * Does not wait for all subthreads to finish.
   *  See {@link #stop()}.
   */
    public synchronized void join() throws InterruptedException {
        while (running) {
            wait();
        }
    }

    /**
   * Return the socket (ip+port) on which the RPC server is listening to.
   * @return the socket (ip+port) on which the RPC server is listening to.
   */
    public synchronized InetSocketAddress getListenerAddress() {
        return listener.getAddress();
    }

    /** Called for each call. */
    public abstract Writable call(Writable param, long receiveTime) throws IOException;

    /**
   * The number of open RPC conections
   * @return the number of open rpc connections
   */
    public int getNumOpenConnections() {
        return numConnections;
    }

    /**
   * The number of rpc calls in the queue.
   * @return The number of rpc calls in the queue.
   */
    public int getCallQueueLen() {
        return callQueue.size();
    }

    /**
   * When the read or write buffer size is larger than this limit, i/o will be 
   * done in chunks of this size. Most RPC requests and responses would be
   * be smaller.
   */
    private static int NIO_BUFFER_LIMIT = 8 * 1024;

    /**
   * This is a wrapper around {@link WritableByteChannel#write(ByteBuffer)}.
   * If the amount of data is large, it writes to channel in smaller chunks. 
   * This is to avoid jdk from creating many direct buffers as the size of 
   * buffer increases. This also minimizes extra copies in NIO layer
   * as a result of multiple write operations required to write a large 
   * buffer.  
   *
   * @see WritableByteChannel#write(ByteBuffer)
   */
    private static int channelWrite(WritableByteChannel channel, ByteBuffer buffer) throws IOException {
        return (buffer.remaining() <= NIO_BUFFER_LIMIT) ? channel.write(buffer) : channelIO(null, channel, buffer);
    }

    /**
   * This is a wrapper around {@link ReadableByteChannel#read(ByteBuffer)}.
   * If the amount of data is large, it writes to channel in smaller chunks. 
   * This is to avoid jdk from creating many direct buffers as the size of 
   * ByteBuffer increases. There should not be any performance degredation.
   * 
   * @see ReadableByteChannel#read(ByteBuffer)
   */
    private static int channelRead(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        return (buffer.remaining() <= NIO_BUFFER_LIMIT) ? channel.read(buffer) : channelIO(channel, null, buffer);
    }

    /**
   * Helper for {@link #channelRead(ReadableByteChannel, ByteBuffer)}
   * and {@link #channelWrite(WritableByteChannel, ByteBuffer)}. Only
   * one of readCh or writeCh should be non-null.
   * 
   * @see #channelRead(ReadableByteChannel, ByteBuffer)
   * @see #channelWrite(WritableByteChannel, ByteBuffer)
   */
    private static int channelIO(ReadableByteChannel readCh, WritableByteChannel writeCh, ByteBuffer buf) throws IOException {
        int originalLimit = buf.limit();
        int initialRemaining = buf.remaining();
        int ret = 0;
        while (buf.remaining() > 0) {
            try {
                int ioSize = Math.min(buf.remaining(), NIO_BUFFER_LIMIT);
                buf.limit(buf.position() + ioSize);
                ret = (readCh == null) ? writeCh.write(buf) : readCh.read(buf);
                if (ret < ioSize) {
                    break;
                }
            } finally {
                buf.limit(originalLimit);
            }
        }
        int nBytes = initialRemaining - buf.remaining();
        return (nBytes > 0) ? nBytes : ret;
    }
}
