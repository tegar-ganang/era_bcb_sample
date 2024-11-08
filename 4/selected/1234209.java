package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import net.spy.SpyObject;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationState;

/**
 * Connection to a cluster of memcached servers.
 */
public final class MemcachedConnection extends SpyObject {

    private static final int DOUBLE_CHECK_EMPTY = 256;

    private static final int EXCESSIVE_EMPTY = 0x1000000;

    private static final long MAX_DELAY = 30000;

    private volatile boolean shutDown = false;

    private boolean optimizeGets = true;

    private Selector selector = null;

    private final NodeLocator locator;

    private int emptySelects = 0;

    private final ConcurrentLinkedQueue<MemcachedNode> addedQueue;

    private final SortedMap<Long, MemcachedNode> reconnectQueue;

    /**
	 * Construct a memcached connection.
	 *
	 * @param bufSize the size of the buffer used for reading from the server
	 * @param f the factory that will provide an operation queue
	 * @param a the addresses of the servers to connect to
	 *
	 * @throws IOException if a connection attempt fails early
	 */
    public MemcachedConnection(int bufSize, ConnectionFactory f, List<InetSocketAddress> a) throws IOException {
        reconnectQueue = new TreeMap<Long, MemcachedNode>();
        addedQueue = new ConcurrentLinkedQueue<MemcachedNode>();
        selector = Selector.open();
        List<MemcachedNode> connections = new ArrayList<MemcachedNode>(a.size());
        for (SocketAddress sa : a) {
            SocketChannel ch = SocketChannel.open();
            ch.configureBlocking(false);
            MemcachedNode qa = f.createMemcachedNode(sa, ch, bufSize);
            int ops = 0;
            if (ch.connect(sa)) {
                getLogger().info("Connected to %s immediately", qa);
                qa.connected();
                assert ch.isConnected();
            } else {
                getLogger().info("Added %s to connect queue", qa);
                ops = SelectionKey.OP_CONNECT;
            }
            qa.setSk(ch.register(selector, ops, qa));
            assert ch.isConnected() || qa.getSk().interestOps() == SelectionKey.OP_CONNECT : "Not connected, and not wanting to connect";
            connections.add(qa);
        }
        locator = f.createLocator(connections);
    }

    /**
	 * Enable or disable get optimization.
	 *
	 * When enabled (default), multiple sequential gets are collapsed into one.
	 */
    public void setGetOptimization(boolean to) {
        optimizeGets = to;
    }

    private boolean selectorsMakeSense() {
        for (MemcachedNode qa : locator.getAll()) {
            if (qa.getSk() != null && qa.getSk().isValid()) {
                if (qa.getChannel().isConnected()) {
                    int sops = qa.getSk().interestOps();
                    int expected = 0;
                    if (qa.hasReadOp()) {
                        expected |= SelectionKey.OP_READ;
                    }
                    if (qa.hasWriteOp()) {
                        expected |= SelectionKey.OP_WRITE;
                    }
                    if (qa.getBytesRemainingToWrite() > 0) {
                        expected |= SelectionKey.OP_WRITE;
                    }
                    assert sops == expected : "Invalid ops:  " + qa + ", expected " + expected + ", got " + sops;
                } else {
                    int sops = qa.getSk().interestOps();
                    assert sops == SelectionKey.OP_CONNECT : "Not connected, and not watching for connect: " + sops;
                }
            }
        }
        getLogger().debug("Checked the selectors.");
        return true;
    }

    /**
	 * MemcachedClient calls this method to handle IO over the connections.
	 */
    @SuppressWarnings("unchecked")
    public void handleIO() throws IOException {
        if (shutDown) {
            throw new IOException("No IO while shut down");
        }
        handleInputQueue();
        getLogger().debug("Done dealing with queue.");
        long delay = 0;
        if (!reconnectQueue.isEmpty()) {
            long now = System.currentTimeMillis();
            long then = reconnectQueue.firstKey();
            delay = Math.max(then - now, 1);
        }
        getLogger().debug("Selecting with delay of %sms", delay);
        assert selectorsMakeSense() : "Selectors don't make sense.";
        int selected = selector.select(delay);
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        if (selectedKeys.isEmpty() && !shutDown) {
            getLogger().debug("No selectors ready, interrupted: " + Thread.interrupted());
            if (++emptySelects > DOUBLE_CHECK_EMPTY) {
                for (SelectionKey sk : selector.keys()) {
                    getLogger().info("%s has %s, interested in %s", sk, sk.readyOps(), sk.interestOps());
                    if (sk.readyOps() != 0) {
                        getLogger().info("%s has a ready op, handling IO", sk);
                        handleIO(sk);
                    } else {
                        queueReconnect((MemcachedNode) sk.attachment());
                    }
                }
                assert emptySelects < EXCESSIVE_EMPTY : "Too many empty selects";
            }
        } else {
            getLogger().debug("Selected %d, selected %d keys", selected, selectedKeys.size());
            emptySelects = 0;
            for (SelectionKey sk : selectedKeys) {
                handleIO(sk);
            }
            selectedKeys.clear();
        }
        if (!shutDown && !reconnectQueue.isEmpty()) {
            attemptReconnects();
        }
    }

    private void handleInputQueue() {
        if (!addedQueue.isEmpty()) {
            getLogger().debug("Handling queue");
            Collection<MemcachedNode> toAdd = new HashSet<MemcachedNode>();
            Collection<MemcachedNode> todo = new HashSet<MemcachedNode>();
            try {
                MemcachedNode qa = null;
                while ((qa = addedQueue.remove()) != null) {
                    todo.add(qa);
                }
            } catch (NoSuchElementException e) {
            }
            for (MemcachedNode qa : todo) {
                boolean readyForIO = false;
                if (qa.isActive()) {
                    if (qa.getCurrentWriteOp() != null) {
                        readyForIO = true;
                        getLogger().debug("Handling queued write %s", qa);
                    }
                } else {
                    toAdd.add(qa);
                }
                qa.copyInputQueue();
                if (readyForIO) {
                    try {
                        if (qa.getWbuf().hasRemaining()) {
                            handleWrites(qa.getSk(), qa);
                        }
                    } catch (IOException e) {
                        getLogger().warn("Exception handling write", e);
                        queueReconnect(qa);
                    }
                }
                qa.fixupOps();
            }
            addedQueue.addAll(toAdd);
        }
    }

    private void handleIO(SelectionKey sk) {
        MemcachedNode qa = (MemcachedNode) sk.attachment();
        try {
            getLogger().debug("Handling IO for:  %s (r=%s, w=%s, c=%s, op=%s)", sk, sk.isReadable(), sk.isWritable(), sk.isConnectable(), sk.attachment());
            if (sk.isConnectable()) {
                getLogger().info("Connection state changed for %s", sk);
                final SocketChannel channel = qa.getChannel();
                if (channel.finishConnect()) {
                    assert channel.isConnected() : "Not connected.";
                    qa.connected();
                    addedQueue.offer(qa);
                    if (qa.getWbuf().hasRemaining()) {
                        handleWrites(sk, qa);
                    }
                } else {
                    assert !channel.isConnected() : "connected";
                }
            } else {
                if (sk.isReadable()) {
                    handleReads(sk, qa);
                }
                if (sk.isWritable()) {
                    handleWrites(sk, qa);
                }
            }
        } catch (Exception e) {
            getLogger().info("Reconnecting due to exception on %s", qa, e);
            queueReconnect(qa);
        }
        qa.fixupOps();
    }

    private void handleWrites(SelectionKey sk, MemcachedNode qa) throws IOException {
        qa.fillWriteBuffer(optimizeGets);
        boolean canWriteMore = qa.getBytesRemainingToWrite() > 0;
        while (canWriteMore) {
            int wrote = qa.writeSome();
            qa.fillWriteBuffer(optimizeGets);
            canWriteMore = wrote > 0 && qa.getBytesRemainingToWrite() > 0;
        }
    }

    private void handleReads(SelectionKey sk, MemcachedNode qa) throws IOException {
        Operation currentOp = qa.getCurrentReadOp();
        ByteBuffer rbuf = qa.getRbuf();
        final SocketChannel channel = qa.getChannel();
        int read = channel.read(rbuf);
        while (read > 0) {
            getLogger().debug("Read %d bytes", read);
            rbuf.flip();
            while (rbuf.remaining() > 0) {
                assert currentOp != null : "No read operation";
                currentOp.readFromBuffer(rbuf);
                if (currentOp.getState() == OperationState.COMPLETE) {
                    getLogger().debug("Completed read op: %s and giving the next %d bytes", currentOp, rbuf.remaining());
                    Operation op = qa.removeCurrentReadOp();
                    assert op == currentOp : "Expected to pop " + currentOp + " got " + op;
                    currentOp = qa.getCurrentReadOp();
                }
            }
            rbuf.clear();
            read = channel.read(rbuf);
        }
    }

    static String dbgBuffer(ByteBuffer b, int size) {
        StringBuilder sb = new StringBuilder();
        byte[] bytes = b.array();
        for (int i = 0; i < size; i++) {
            char ch = (char) bytes[i];
            if (Character.isWhitespace(ch) || Character.isLetterOrDigit(ch)) {
                sb.append(ch);
            } else {
                sb.append("\\x");
                sb.append(Integer.toHexString(bytes[i] & 0xff));
            }
        }
        return sb.toString();
    }

    private void queueReconnect(MemcachedNode qa) {
        if (!shutDown) {
            getLogger().warn("Closing, and reopening %s, attempt %d.", qa, qa.getReconnectCount());
            if (qa.getSk() != null) {
                qa.getSk().cancel();
                assert !qa.getSk().isValid() : "Cancelled selection key is valid";
            }
            qa.reconnecting();
            try {
                if (qa.getChannel() != null && qa.getChannel().socket() != null) {
                    qa.getChannel().socket().close();
                } else {
                    getLogger().info("The channel or socket was null for %s", qa);
                }
            } catch (IOException e) {
                getLogger().warn("IOException trying to close a socket", e);
            }
            qa.setChannel(null);
            long delay = Math.min((100 * qa.getReconnectCount()) ^ 2, MAX_DELAY);
            reconnectQueue.put(System.currentTimeMillis() + delay, qa);
            qa.setupResend();
        }
    }

    private void attemptReconnects() throws IOException {
        final long now = System.currentTimeMillis();
        final Map<MemcachedNode, Boolean> seen = new IdentityHashMap<MemcachedNode, Boolean>();
        for (Iterator<MemcachedNode> i = reconnectQueue.headMap(now).values().iterator(); i.hasNext(); ) {
            final MemcachedNode qa = i.next();
            i.remove();
            if (!seen.containsKey(qa)) {
                seen.put(qa, Boolean.TRUE);
                getLogger().info("Reconnecting %s", qa);
                final SocketChannel ch = SocketChannel.open();
                ch.configureBlocking(false);
                int ops = 0;
                if (ch.connect(qa.getSocketAddress())) {
                    getLogger().info("Immediately reconnected to %s", qa);
                    assert ch.isConnected();
                } else {
                    ops = SelectionKey.OP_CONNECT;
                }
                qa.registerChannel(ch, ch.register(selector, ops, qa));
                assert qa.getChannel() == ch : "Channel was lost.";
            } else {
                getLogger().debug("Skipping duplicate reconnect request for %s", qa);
            }
        }
    }

    /**
	 * Get the node locator used by this connection.
	 */
    NodeLocator getLocator() {
        return locator;
    }

    /**
	 * Add an operation to the given connection.
	 *
	 * @param which the connection offset
	 * @param o the operation
	 */
    public void addOperation(final String key, final Operation o) {
        MemcachedNode placeIn = null;
        MemcachedNode primary = locator.getPrimary(key);
        if (primary.isActive()) {
            placeIn = primary;
        } else {
            for (Iterator<MemcachedNode> i = locator.getSequence(key); placeIn == null && i.hasNext(); ) {
                MemcachedNode n = i.next();
                if (n.isActive()) {
                    placeIn = n;
                }
            }
            if (placeIn == null) {
                placeIn = primary;
            }
        }
        assert placeIn != null : "No node found for key " + key;
        addOperation(placeIn, o);
    }

    public void addOperation(final MemcachedNode node, final Operation o) {
        o.initialize();
        node.addOp(o);
        addedQueue.offer(node);
        Selector s = selector.wakeup();
        assert s == selector : "Wakeup returned the wrong selector.";
        getLogger().debug("Added %s to %s", o, node);
    }

    public void addOperations(final Map<MemcachedNode, Operation> ops) {
        for (Map.Entry<MemcachedNode, Operation> me : ops.entrySet()) {
            final MemcachedNode node = me.getKey();
            Operation o = me.getValue();
            o.initialize();
            node.addOp(o);
            addedQueue.offer(node);
        }
        Selector s = selector.wakeup();
        assert s == selector : "Wakeup returned the wrong selector.";
    }

    /**
	 * Broadcast an operation to all nodes.
	 */
    public CountDownLatch broadcastOperation(final BroadcastOpFactory of) {
        final CountDownLatch latch = new CountDownLatch(locator.getAll().size());
        for (MemcachedNode node : locator.getAll()) {
            Operation op = of.newOp(node, latch);
            op.initialize();
            node.addOp(op);
            addedQueue.offer(node);
        }
        Selector s = selector.wakeup();
        assert s == selector : "Wakeup returned the wrong selector.";
        return latch;
    }

    /**
	 * Shut down all of the connections.
	 */
    public void shutdown() throws IOException {
        shutDown = true;
        Selector s = selector.wakeup();
        assert s == selector : "Wakeup returned the wrong selector.";
        for (MemcachedNode qa : locator.getAll()) {
            if (qa.getChannel() != null) {
                qa.getChannel().close();
                qa.setSk(null);
                if (qa.getBytesRemainingToWrite() > 0) {
                    getLogger().warn("Shut down with %d bytes remaining to write", qa.getBytesRemainingToWrite());
                }
                getLogger().debug("Shut down channel %s", qa.getChannel());
            }
        }
        selector.close();
        getLogger().debug("Shut down selector %s", selector);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{MemcachedConnection to");
        for (MemcachedNode qa : locator.getAll()) {
            sb.append(" ");
            sb.append(qa.getSocketAddress());
        }
        sb.append("}");
        return sb.toString();
    }
}
