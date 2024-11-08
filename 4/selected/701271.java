package org.szegedi.nioserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import org.szegedi.julog.Logger;
import org.szegedi.julog.LoggerFactory;

/**
 * <p>
 * The runtime class for non-blocking servers. Every NioServer manages a number
 * of threads for accepting connections, reading, and writing. In the basic case,
 * you will have a single thread for accepting connections and another single
 * thread for processing read and write operations. However, if you are running
 * the code on a multiprocessor machine, you can create more initial threads
 * to improve CPU utilization. Also, when running on Windows, the server will
 * spawn new threads when all current threads service 63 channels - the maximum
 * number of channels a single thread (more precisely "the maximum number of
 * keys a single selector") can service under this OS family. Note that any
 * number of different services (that is, server sockets) bound to any IP 
 * address and any port on the local machine can be serviced by a single 
 * server instance - the non-blocking nature allows accepting connections
 * from multiple server sockets on a single thread.
 * </p>
 * <p>
 * <b>An implementation note:</b> the server by default operates in 
 * "half-duplex" mode. This means that read and write notifications for each 
 * connection are serviced on a single thread, therefore the server will never 
 * both read and write a connection concurrently - if the connection is ready 
 * for both reading and writing, read will occur before write. You can force
 * the server into a "full-duplex" mode where read and write operations are
 * serviced on separate threads by seting the
 * <tt>org.szegedi.nio.fullDuplex</tt> system property to <tt>true</tt>.
 * Note however, that some combinations of a JVM and OS cause erroneous
 * operation in full-duplex mode, so test for yourself. Since most protocols
 * follow some kind of request-response semantics, this is usually not a big
 * problem. Also note that half-duplex applies only to a single connection;
 * if you have multiple read-write threads, then the connections on separate
 * threads are still serviced concurrently. The term half-duplex applies
 * exclusively to serialization of read and write operations in a context of
 * single connection.
 * </p>
 * @author Attila Szegedi, szegedia at freemail dot hu 
 * @author zombi at mailbox dot hu
 * @version $Id : $ */
public class NioServer {

    private static final boolean fullDuplexTransfer = Boolean.getBoolean("org.szegedi.nio.fullDuplex");

    private final SelectorPool acceptSelectors;

    private final SelectorPool readSelectors;

    private final SelectorPool writeSelectors;

    private final SelectorPool connectSelectors;

    private final Map services = new IdentityHashMap();

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Creates a new non-blocking server that will use one permanent thread
     * for accepting connections and two permanent thread for read/write
     * operations.
     */
    public NioServer() throws IOException {
        this(1, 2);
    }

    /**
     * Creates a new non-blocking server that will use the specified number
     * of permanent threads for accepting connections and for read/write
     * operations.
     * @param permanentAcceptThreads number of threads that are always
     * available for accepting connections, regardless of the server load.
     * One thread will usually be sufficient, however on a SMP machine with
     * several network interfaces you can achieve better throughput with
     * more threads.
     * @param permanentTransferThreads number of threads that are always
     * available for servicing socket reads and writes, regardless of server
     * load. Using two threads per CPU should yield satisfactory results.
     */
    public NioServer(int permanentAcceptThreads, int permanentTransferThreads) throws IOException {
        if (permanentAcceptThreads < 1) throw new IllegalArgumentException("permanentAcceptThreads < 1");
        if (permanentTransferThreads < 1) throw new IllegalArgumentException("permanentTransferThreads < 1");
        acceptSelectors = new SelectorPool(new AcceptSelectorLoop(), this, permanentAcceptThreads);
        if (fullDuplexTransfer) {
            readSelectors = new SelectorPool(new ReadSelectorLoop(), this, permanentTransferThreads);
            writeSelectors = new SelectorPool(new WriteSelectorLoop(), this, permanentTransferThreads);
        } else {
            readSelectors = new SelectorPool(new ReadWriteSelectorLoop(), this, permanentTransferThreads);
            writeSelectors = null;
        }
        connectSelectors = new SelectorPool(new ConnectSelectorLoop(this), this, permanentAcceptThreads);
    }

    /**
     * Stops this server. All threads are terminated and all services are
     * stopped and unregistered.
     */
    public void stop() {
        synchronized (services) {
            Iterator it = services.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Service service = (Service) entry.getKey();
                ServerSocketChannel ssc = (ServerSocketChannel) entry.getValue();
                service.setStopped(true);
                closeServerSocketChannel(ssc);
                it.remove();
            }
        }
        acceptSelectors.stop();
        readSelectors.stop();
        if (writeSelectors != null) {
            writeSelectors.stop();
        }
        connectSelectors.stop();
    }

    /**
     * Adds a service to this server. A service is a coupling of a protocol
     * handler factory and a server socket configuration. After a service is
     * added, the server will accept connections on the socket address specified
     * by the server configuration and hand over accepted connections to the 
     * protocol handler factory. The protocol handlers created by the factory
     * will then be called whenever there is a pending read or write operation
     * on their associated connection.
     * @param factory the protocol handler factory that will create protocol
     * handlers for servicing connections
     * @param ssconf a ServerSocketConfiguration object that specifies server
     * socket address, port, timeout behavior, etc.
     * @return a service object that represents the registered network service.
     */
    public Service registerService(ProtocolHandlerFactory factory, ServerSocketConfiguration ssconf) throws IOException {
        ServerSocket ss = ssconf.createSocket();
        ServerSocketChannel ssc = ss.getChannel();
        Service service = new Service(ss.getLocalSocketAddress(), factory, this);
        acceptSelectors.register(ssc, service);
        synchronized (services) {
            services.put(service, ss);
        }
        return service;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    Logger getLogger() {
        return logger;
    }

    void removeService(Service service) {
        synchronized (services) {
            ServerSocketChannel ssc = (ServerSocketChannel) services.remove(service);
            if (ssc != null) closeServerSocketChannel(ssc);
        }
    }

    void registerProtocolHandler(SocketChannel sc, ProtocolHandler handler) {
        try {
            int validOps = handler.validOps();
            if (writeSelectors == null) {
                if ((validOps & (SelectionKey.OP_READ | SelectionKey.OP_WRITE)) != 0) readSelectors.register(sc, handler);
            } else {
                if ((validOps & SelectionKey.OP_READ) != 0) readSelectors.register(sc, handler);
                if ((validOps & SelectionKey.OP_WRITE) != 0) writeSelectors.register(sc, handler);
            }
        } catch (ClosedChannelException e) {
            logger.warn("Can't register protocol handler" + handler.getClass().getName(), e);
        } catch (IOException e) {
            logger.error("Can't register protocol handler" + handler.getClass().getName(), e);
        }
    }

    /**
     * Registers a client protocol handler with this server instance.
     * The NioServer can host client side of the protocols as well in addition
     * to hosting server side. If this method returns successfully, then the
     * server has taken responsibility over channel closing in case of an
     * exception until the channel is connected. After the channel is connected,
     * the protocol handler's read and write methods are responsible for
     * closing the channel upon protocol termination.
     * @param addr the address of remote socket to connect to
     * @param sc the socket channel that abstracts the connection
     * @param handler the client-side protocol handler.
     */
    public void registerClient(SocketAddress addr, SocketChannel sc, ClientProtocolHandler handler) throws IOException {
        try {
            sc.configureBlocking(false);
            if (sc.connect(addr)) {
                finishConnect(sc, handler);
            } else {
                connectSelectors.register(sc, handler);
            }
        } catch (IOException e) {
            if (sc.isOpen()) {
                sc.close();
            }
            throw e;
        }
    }

    boolean finishConnect(SocketChannel sc, ClientProtocolHandler handler) throws IOException {
        try {
            if (sc.finishConnect()) {
                if (handler.afterConnect()) {
                    registerProtocolHandler(sc, handler);
                } else {
                    if (sc.isOpen()) {
                        sc.close();
                    }
                }
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            if (sc.isOpen()) {
                sc.close();
            }
            throw e;
        }
    }

    void closeServerSocketChannel(ServerSocketChannel ssc) {
        try {
            ssc.close();
        } catch (IOException e) {
            logger.error("Cannot close ServerSocketChannel", e);
        }
    }

    /**
     * Method used to access inner statistics for purpose of unit testing
     */
    int[][] getThreadLoad() {
        int[][] retval = new int[3][];
        retval[0] = acceptSelectors.getThreadLoad();
        retval[1] = readSelectors.getThreadLoad();
        retval[2] = writeSelectors == null ? new int[0] : writeSelectors.getThreadLoad();
        return retval;
    }

    boolean isHalfDuplex() {
        return writeSelectors == null;
    }
}
