package net.alinnistor.nk.service.network.nio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javax.swing.JOptionPane;
import net.alinnistor.nk.service.Serializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class that provides the core of a non-blocking NIO-driven server
 * which can send and receive any type of Objects by serializing them.
 * This class will create a new Thread running the code inside run().
 * 
 *  @author <a href="mailto:nad7ir@yahoo.com">Alin NISTOR</a>
 */
public abstract class TcpManager implements Runnable {

    protected static final Log log = LogFactory.getLog(TcpManager.class);

    protected enum Status {

        INITIALIZING, STARTED, RUNNING, STOPPING, STOPPED, OFFLINE
    }

    protected class ObjToSend {

        SocketChannel sc;

        Object obj;

        public ObjToSend(Object obj, SocketChannel sc) {
            this.obj = obj;
            this.sc = sc;
        }
    }

    public static int defaultByteBufferSize = 1024;

    protected int currentByteBufferSize;

    protected Status status = Status.INITIALIZING;

    protected Selector selector;

    protected int port = -1, targetPort;

    protected ServerSocketChannel servSckChannel;

    protected ByteBuffer buffer, buffer4bytes;

    protected Map<SelectionKey, Queue<ObjToSend>> mapQueues;

    private List<SocketChannel> connectableChannels = new LinkedList<SocketChannel>();

    private Thread thisThread;

    private InetAddress isa;

    /**
	 * Creates a server using the default byte buffer size	
	 */
    public TcpManager(int port) throws UnknownHostException {
        this(port, defaultByteBufferSize);
    }

    /**
	 * Creates a server with a custom byte-buffer size, and ignores the defaultByteBufferSize
	 */
    public TcpManager(int port, int newBufferSize) throws UnknownHostException {
        targetPort = port;
        currentByteBufferSize = newBufferSize;
        buffer = ByteBuffer.allocate(currentByteBufferSize);
        buffer4bytes = ByteBuffer.allocate(4);
        mapQueues = new HashMap<SelectionKey, Queue<ObjToSend>>();
        isa = InetAddress.getLocalHost();
    }

    /**
	 * The very essence of the server. It runs continuously once the server has
	 * started, and continues until a successful call is made to the stop method
	 * <P>
	 * You can check if this method is running by inspecting the status variable	
	 * <P>
	 * This server is single-threaded, using a single NIO Selector to do all the
	 * work of accepting, reading from and writing to connected TCP channels.
	 */
    @Override
    public void run() {
        synchronized (this) {
            if (status != Status.STARTED) {
                log.warn("Started the thread, but the current status is: " + status.toString() + ",\n" + "terminating thread immediately");
                return;
            }
            try {
                servSckChannel = ServerSocketChannel.open();
                servSckChannel.configureBlocking(false);
                servSckChannel.socket().bind(new InetSocketAddress(targetPort));
                port = targetPort;
                log.info("Listening on port [ " + targetPort + " ]");
            } catch (BindException e) {
                status = Status.OFFLINE;
                log.error("Attempted to bind to port " + targetPort + " which is already in use; server going offline", e);
                return;
            } catch (IOException e) {
                status = Status.OFFLINE;
                log.error("Failed to open non-blocking server port = " + targetPort + "; server going offline", e);
                return;
            }
        }
        try {
            synchronized (this) {
                if (status != Status.STARTED) return;
                selector = Selector.open();
                servSckChannel.register(selector, SelectionKey.OP_ACCEPT);
                status = Status.RUNNING;
            }
            while (thisThread != null) {
                synchronized (connectableChannels) {
                    Iterator<SocketChannel> changes = connectableChannels.iterator();
                    while (changes.hasNext()) {
                        SocketChannel change = changes.next();
                        change.register(selector, SelectionKey.OP_CONNECT);
                    }
                    this.connectableChannels.clear();
                }
                log.debug("[" + ((thisThread == null) ? "null" : thisThread.getName()) + ":" + port + "] Selecting ");
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = null;
                    log.debug("Next key");
                    try {
                        key = iterator.next();
                        iterator.remove();
                        if (key.isConnectable()) {
                            log.debug("Connectable key, completing the connection");
                            SocketChannel sc = ((SocketChannel) key.channel());
                            sc.finishConnect();
                            key.interestOps(SelectionKey.OP_READ);
                            synchronized (sc) {
                                sc.notifyAll();
                            }
                        }
                        if (key.isAcceptable()) {
                            log.debug("Acceptable key, accepting and adding new SocketChannel to selector for reading");
                            SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
                            sc.configureBlocking(false);
                            sc.register(selector, SelectionKey.OP_READ);
                            connectionAccepted(sc);
                        }
                        if (key.isReadable()) {
                            log.debug("Readable key");
                            try {
                                Object readObject = getObjectFromKey(key);
                                log.info("[" + ((thisThread == null) ? "null" : thisThread.getName()) + ":" + port + "]   Received object = \"" + readObject);
                                onObjectRead(readObject, key);
                            } catch (IOException e) {
                                log.info("\n\t\tClient probably disconected I'll close the socket");
                                log.debug("Cancelling readable key (" + key + ")");
                                onChannelClose(key.channel());
                                key.channel().close();
                                key.cancel();
                                continue;
                            }
                        }
                        if (key.isWritable()) {
                            log.debug("Writable key");
                            synchronized (mapQueues) {
                                Queue<ObjToSend> queue = mapQueues.get(key);
                                while (!queue.isEmpty()) {
                                    ObjToSend objts = queue.remove();
                                    byte[] bytes = serializeObject((Serializable) objts.obj);
                                    ByteBuffer bbf = createNetworkObject(bytes);
                                    int numWritten = ((WritableByteChannel) objts.sc).write(bbf);
                                }
                                if (queue.isEmpty()) {
                                    key.interestOps(SelectionKey.OP_READ);
                                }
                            }
                        }
                    } catch (CancelledKeyException e) {
                        log.warn("Attempted to write to cancelled key (probably " + "cancelled on read in this loop) (" + key + "); ignoring ", e);
                        continue;
                    } catch (IOException e) {
                        log.info("Cancelling writable key (" + key + ") that generated: ", e);
                        key.cancel();
                        continue;
                    } catch (Exception e) {
                        log.warn("[" + ((thisThread == null) ? "null" : thisThread.getName()) + ":" + port + "] Error handling key = " + key + ", continuing with selection for all other keys", e);
                    }
                    log.debug("Key ended; keys left this run = " + iterator.hasNext());
                }
                log.debug("Last key ended");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "The server went to hell due to" + e + ", please restart NK");
            log.error("The server went to hell ", e);
        } catch (Throwable e) {
            JOptionPane.showMessageDialog(null, "The server went to hell due to" + e + ", please restart NK");
            log.error("The server went to hell ", e);
        } finally {
            try {
                selector.close();
            } catch (Throwable e) {
                log.error("Failed on final attempt to close selector", e);
            }
            try {
                servSckChannel.close();
            } catch (Throwable e) {
                log.error("Failed on final attempt to close ServerSocketChannel", e);
            }
        }
    }

    public ByteBuffer createNetworkObject(byte[] bytes) {
        byte[] ts = new byte[bytes.length + 4];
        byte[] lng = Serializer.serializeInteger(bytes.length);
        ts[0] = lng[0];
        ts[1] = lng[1];
        ts[2] = lng[2];
        ts[3] = lng[3];
        System.arraycopy(bytes, 0, ts, 4, bytes.length);
        ByteBuffer bbf = ByteBuffer.wrap(ts);
        return bbf;
    }

    /**
	 * Override it to determine what exactly will your manager do
	 * when a channel is closed
	 * 
	 * @param channel
	 */
    protected abstract void onChannelClose(SelectableChannel channel);

    /**
	 * How to treat an object read by this manager 
	 * 
	 * @param channel
	 */
    protected abstract void onObjectRead(Object readObject, SelectionKey key);

    /**
	 * What specifically you want to do with the already accepted
	 * connection
	 * 
	 * @param sc
	 */
    protected abstract void connectionAccepted(SocketChannel sc);

    public void sendObject(Object obj, SocketChannel sc) {
        SelectionKey sltdkey = sc.keyFor(selector);
        if (sltdkey != null) {
            synchronized (mapQueues) {
                Queue<ObjToSend> queue = mapQueues.get(sltdkey);
                if (queue == null) {
                    queue = new LinkedList<ObjToSend>();
                    mapQueues.put(sltdkey, queue);
                }
                queue.add(new ObjToSend(obj, sc));
            }
            if ((sltdkey.interestOps() & SelectionKey.OP_WRITE) != SelectionKey.OP_WRITE) {
                try {
                    sltdkey.channel().register(selector, SelectionKey.OP_WRITE);
                } catch (ClosedChannelException e) {
                    log.error("while sending object", e);
                }
            }
        }
        selector.wakeup();
    }

    protected Object getObjectFromKey(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        buffer4bytes.clear();
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        int nr = 0, count = 0, expected = 0;
        if (socketChannel.read(buffer4bytes) == 4) {
            buffer4bytes.flip();
            expected = Serializer.byteArrayToInt(buffer4bytes.array());
        }
        while (count < expected) {
            buffer.clear();
            nr = socketChannel.read(buffer);
            buffer.flip();
            if (nr > 0) {
                if (nr == currentByteBufferSize) {
                    bas.write(buffer.array());
                } else {
                    byte[] readby = buffer.array();
                    byte[] trueBytes = new byte[nr];
                    System.arraycopy(readby, 0, trueBytes, 0, nr);
                    bas.write(trueBytes);
                }
                count += nr;
            }
        }
        byte[] rv = bas.toByteArray();
        return deserializeObject(rv);
    }

    public void start() {
        status = Status.STARTED;
        thisThread = new Thread(this, "TcpManager");
        thisThread.start();
        log.info("Server started");
    }

    public void stop() {
        synchronized (this) {
            status = Status.STOPPING;
            log.info("[" + ((thisThread == null) ? "null" : thisThread.getName()) + ":" + port + "] Stopping");
        }
        Thread oldThread = null;
        if (selector == null) {
            log.info("[" + ((thisThread == null) ? "null" : thisThread.getName()) + ":" + port + "] Selector never started, so not stopping anything");
        } else if (thisThread == null) {
            log.info("[" + ((thisThread == null) ? "null" : thisThread.getName()) + ":" + port + "] Impossible situation - stop() was called before start() returned");
            Thread.dumpStack();
            if (true) {
                throw new UnsupportedOperationException("Impossible situation; stop() was called before start() returned");
            }
        } else {
            oldThread = thisThread;
            thisThread = null;
            selector.wakeup();
            oldThread.interrupt();
            boolean outputMessageYet = false;
            while (oldThread.isAlive() || selector.isOpen()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    log.error("while stopping", e);
                }
                if (!outputMessageYet) {
                    outputMessageYet = true;
                    log.info("[" + ((thisThread == null) ? "null" : thisThread.getName()) + ":" + port + "] Waiting for thread named: " + oldThread.getName() + " to die happy ");
                }
            }
            if (outputMessageYet) {
                log.info("[" + ((thisThread == null) ? "null" : thisThread.getName()) + ":" + port + "] Complete. Thread: " + oldThread.getName() + " is no longer alive");
            }
        }
        status = Status.STOPPED;
        log.info("[" + ((thisThread == null) ? ((oldThread == null) ? "null" : oldThread.getName()) : thisThread.getName()) + ":" + port + "] Stopped.");
    }

    /**
	 * Attempts to connect to the remote listener if a connection to the 
	 * given listener [ client ] doesn't already exist.	 
	 */
    public SocketChannel initiateUniqueConnection(InetSocketAddress isa) throws Throwable {
        if (alreadyExistsConnection(isa)) {
            return null;
        }
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(isa);
        synchronized (this.connectableChannels) {
            this.connectableChannels.add(sc);
        }
        selector.wakeup();
        return sc;
    }

    private boolean alreadyExistsConnection(InetSocketAddress isaRemote) {
        Set<SelectionKey> keys = selector.keys();
        Iterator<SelectionKey> iterator = keys.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            if (key.channel() instanceof SocketChannel) {
                SocketChannel sc = (SocketChannel) key.channel();
                if (sc.socket().isConnected()) {
                    InetSocketAddress isa = (InetSocketAddress) sc.socket().getRemoteSocketAddress();
                    if (isa.equals(isaRemote)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Status getStatus() {
        return status;
    }

    public int getPort() {
        return this.port;
    }

    public InetAddress getIsa() {
        return isa;
    }

    public static byte[] serializeObject(Serializable obj) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            out.close();
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("while serializing object", e);
            return null;
        }
    }

    public static Serializable deserializeObject(byte[] bytes) {
        try {
            ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object obj = in.readObject();
            in.close();
            return (Serializable) obj;
        } catch (Exception e) {
            log.error("while deserializing object", e);
            return null;
        }
    }
}
