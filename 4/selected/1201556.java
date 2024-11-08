package org.xi8ix.async;

import org.xi8ix.async.spi.AsyncProvider;
import java.io.IOException;
import java.net.InetSocketAddress;
import static java.nio.channels.SelectionKey.*;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Monitors and manages asynchronous selectable channels and drives i/o operations within the framework.
 * The AsyncEngine manages it's own i/o thread(s) (sending and receiving is done within the AsyncEngine
 * threads and not caller threads).</p>
 *
 * <h2>Implementation note</h2>
 *
 * <p>Since the engine thread drives all i/o operations, future versions of the engine should allow
 * a configurable number of i/o threads. Each thread should have it's own selector, spreading out i/o
 * load across the the threads and each thread's dedicated selector.</p>
 *
 * @author Iain Shigeoka
 */
public class AsyncEngine {

    /** Class static logger. */
    private static final Logger LOG = Logger.getLogger(AsyncEngine.class.getName());

    /** Selector used by the engine. */
    private Selector selector;

    /** Channels monitored by the engine. */
    private final HashMap<SelectableChannel, SelectableInfo> selectableChannels = new HashMap<SelectableChannel, SelectableInfo>();

    /** Provider of implementations. */
    private AsyncProvider provider = new AsyncProvider(this);

    /** List of changes that should occur to the selector. */
    private ConcurrentLinkedQueue<Runnable> changes = new ConcurrentLinkedQueue<Runnable>();

    /**
     * Create a new asynchronous engine in a separate thread.
     * The selector is created and the i/o threads started immediately
     * during the constructor. Problems with startup will be reported
     * via IOException (which should be rare).
     *
     * @param daemon true if the engine's thread(s) should be run as daemon threads
     * @throws IOException if there is a problem starting the engine
     */
    public AsyncEngine(boolean daemon) throws IOException {
        selector = Selector.open();
        Thread main = new Thread(new Runnable() {

            public void run() {
                while (selector.isOpen()) {
                    try {
                        LOG.finest("calling select ()");
                        selector.select();
                    } catch (CancelledKeyException e) {
                        LOG.log(Level.FINEST, "Cancelled key exception", e);
                    } catch (IOException e) {
                        if (!e.getMessage().equals("Interrupted system call")) {
                            LOG.log(Level.SEVERE, "unhandled exception: " + e, e);
                            System.exit(1);
                        }
                    }
                    LOG.finest("select returned");
                    if (selector.isOpen()) {
                        Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                        while (keyIterator.hasNext()) {
                            SelectionKey skey = keyIterator.next();
                            keyIterator.remove();
                            SelectableInfo si = selectableChannels.get(skey.channel());
                            if (si == null) continue;
                            if (skey.isValid() && (si.accept != null) && ((skey.readyOps() & OP_ACCEPT) != 0)) {
                                si.accept.run();
                            }
                            if (skey.isValid() && (si.connect != null) && ((skey.readyOps() & OP_CONNECT) != 0)) {
                                si.connect.run();
                            }
                            if (skey.isValid() && (si.read != null) && ((skey.readyOps() & OP_READ) != 0)) {
                                si.read.run();
                            }
                            if (skey.isValid() && (si.write != null) && ((skey.readyOps() & OP_WRITE) != 0)) {
                                si.write.run();
                            }
                        }
                    }
                    for (Runnable runnable = changes.poll(); runnable != null; runnable = changes.poll()) {
                        runnable.run();
                    }
                }
            }
        }, "async-engine");
        main.setDaemon(daemon);
        main.start();
    }

    /**
     * Checks to see if the engine is open and running.
     *
     * @return true if the engine is open and running.
     */
    public boolean isOpen() {
        return selector.isOpen();
    }

    /**
     * Closes the engine and stops the engine's i/o thread(s).
     *
     * @throws IOException if there was a problem closing the engine.
     */
    public void close() throws IOException {
        selector.close();
    }

    /**
     * Register a selectable channel to receive callbacks with the registered interest ops. Calling
     * the method with an already registered channel allows the assignment of a new callback to
     * an existing registration (removing any existing callbacks). Similarly, you can remove a callback
     * by calling this method with the callback set to null although it's cleaner to remove the interest
     * operation entirely using the selection key's java.nio.channels.SelectionKey.interestOps(int) method.
     *
     * @param channel the channel to monitor
     * @param ops the operations interested in
     * @param callback a callback to onSend when the channel has ready data
     * @throws ClosedChannelException if the channel registered was closed
     */
    public void add(SelectableChannel channel, int ops, Runnable callback) throws ClosedChannelException {
        changes.add(new AddCommand(channel, ops, callback));
        selector.wakeup();
    }

    /**
     * Removes the channel from the engine's management and eliminates selector call backs.
     *
     * @param channel the channel to remove registration
     */
    public void remove(SelectableChannel channel) {
        changes.add(new CancelCommand(channel));
        selector.wakeup();
    }

    /**
     * Create a datagram (UDP) server that binds to a local address and waits for incoming data (which
     * generates peers).
     *
     * @param localAddress the local address to bind the socket to
     * @param receiver receives incoming data
     * @return the server
     * @throws IOException if there was a problem creating the server
     */
    public Server createDatagramServer(InetSocketAddress localAddress, Receiver receiver) throws IOException {
        return provider.createDatagramServer(localAddress, receiver);
    }

    /**
     * Create a socket (TCP) server that binds to a local address and waits for incoming connections (which
     * generates peers).
     *
     * @param localAddress the local address to bind the socket to
     * @param receiver receives incoming data
     * @return the server
     * @throws IOException if there was a problem creating the server
     */
    public Server createSocketServer(InetSocketAddress localAddress, Receiver receiver) throws IOException {
        return provider.createSocketServer(localAddress, receiver);
    }

    /**
     * Create a socket peer (client TCP socket) that binds to a particular local address and connects to
     * the given remote remote address.
     *
     * @param localAddress the local address to bind the socket to or null to use an ephemeral local address
     * @param remoteAddress the remote address to connect the socket to
     * @param receiver receives incoming data
     * @return the peer
     * @throws IOException if there was a problem creating the peer
     */
    public Peer createSocketPeer(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Receiver receiver) throws IOException {
        return provider.createSocketPeer(localAddress, remoteAddress, receiver);
    }

    /**
     * A simple data structure use to track the callbacks for various interest ops registered with the engine.
     *
     * @author Iain Shigeoka
     */
    private static class SelectableInfo {

        public Runnable accept, connect, read, write;
    }

    private class CancelCommand implements Runnable {

        private SelectableChannel channel;

        public CancelCommand(SelectableChannel channel) {
            this.channel = channel;
        }

        public void run() {
            selectableChannels.remove(channel);
        }
    }

    private class AddCommand implements Runnable {

        private SelectableChannel channel;

        private int ops;

        private Runnable callback;

        public AddCommand(SelectableChannel channel, int ops, Runnable callback) {
            this.channel = channel;
            this.ops = ops;
            this.callback = callback;
        }

        public void run() {
            LOG.finest("begin");
            SelectionKey skey = channel.keyFor(selector);
            if (skey == null) {
                try {
                    channel.register(selector, ops);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            } else {
                if (skey.isValid()) {
                    skey.interestOps(skey.interestOps() | ops);
                }
            }
            SelectableInfo si = selectableChannels.get(channel);
            if (si == null) {
                si = new SelectableInfo();
                selectableChannels.put(channel, si);
            }
            if ((ops & SelectionKey.OP_ACCEPT) != 0) {
                si.accept = callback;
            }
            if ((ops & SelectionKey.OP_CONNECT) != 0) {
                si.connect = callback;
            }
            if ((ops & SelectionKey.OP_READ) != 0) {
                si.read = callback;
            }
            if ((ops & SelectionKey.OP_WRITE) != 0) {
                si.write = callback;
            }
        }
    }
}
