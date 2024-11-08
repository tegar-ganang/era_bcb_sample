package org.xsocket.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import org.xsocket.ClosedConnectionException;
import org.xsocket.NonBlockingConnection;
import org.xsocket.util.TextUtils;

/**
 * A non-blocking connection which will be used in a managed 
 * mode  
 * 
 * @author grro@xsocket.org
 */
final class ManagedConnection extends NonBlockingConnection {

    private static final Logger LOG = Logger.getLogger(ManagedConnection.class.getName());

    private final WriteQueue writeQueue = new WriteQueue();

    private IManagedConnectionListener connectionListener = null;

    private DirectMemoryManager ioMemoryManager = null;

    private IHandler attachedAppHandler = null;

    private IHandlerTypeInfo attachedAppHandlerTypeInfo = null;

    private boolean isCloseOccured = false;

    private boolean isIdleTimeouOccured = false;

    private boolean isConnectionTimeoutOccured = false;

    /**
	 * constructor 
	 * 
	 * @param channel  the underlying channel
	 * @param id  the assigned id 
	 * @param sslContext the assigned ssl context
	 * @param sslOn true, if ssl should be activated
	 * @throws IOException If some other I/O error occurs
	 */
    public ManagedConnection(SocketChannel channel, String id, SSLContext sslContext, boolean sslOn) throws IOException {
        super(channel, id, false, sslContext, sslOn);
        channel.configureBlocking(false);
        if (LOG.isLoggable(Level.FINE)) {
            logFine("new connection " + toString());
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void init() {
        super.init();
    }

    /**
	 * set the assigned io manager 
	 * 
	 * @param ioMemoryManager the io manager to use
	 */
    void setIOMemoryManager(DirectMemoryManager ioMemoryManager) {
        this.ioMemoryManager = ioMemoryManager;
    }

    /**
	 * register a connection listener 
	 * 
	 * @param connectionListener the connection listener to register
	 */
    void setConnectionListener(IManagedConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    /**
	 * drain the write queue
	 * 
	 * @return the write queue content
	 * @throws IOException If some other I/O error occurs
	 */
    List<ByteBuffer> drainWriteQueue() throws IOException {
        if (writeQueue.isEmtpy()) {
            return null;
        } else {
            return writeQueue.readAvailable();
        }
    }

    /**
	 * add a ByteBuffer array to the first position of the write queue 
	 * @param buffers the ByteBuffer array to add
	 */
    void addAsFirstToWriteQueue(ByteBuffer[] buffers) {
        writeQueue.addFirst(buffers);
    }

    /**
	 * flush the out buffer (selektion key will be set with read-write)
	 */
    @Override
    protected final void flushOutgoing() {
        if (!writeQueue.isEmtpy()) {
            connectionListener.onConnectionDataToSendEvent(this);
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    protected SocketChannel getChannel() {
        return super.getChannel();
    }

    /**
	 * overriden close method. This method just 
	 * signals the listener that the connection
	 * goes into the close state
	 *  
	 */
    @Override
    public void close() {
        if (!isCloseOccured) {
            isCloseOccured = true;
            connectionListener.onConnectionCloseEvent(this);
        }
    }

    /**
	 * the "real" close method
	 *
	 */
    void destroy() {
        super.close();
    }

    /**
	 * overriden writePhysical method. write physical 
	 * will just add the data to the write queue. 
	 *  
	 */
    @Override
    protected synchronized ByteBuffer[] writePhysical(ByteBuffer[] buffers) throws ClosedConnectionException, IOException {
        writeQueue.append(buffers);
        return null;
    }

    /**
	 * the "real" write physical method
	 *  
	 */
    ByteBuffer[] realWritePhysical(ByteBuffer[] buffers) throws ClosedConnectionException, IOException {
        return super.writePhysical(buffers);
    }

    /**
	 * overriden read method. this method will be
	 * called within the receive method of the super
	 * class. But in a managed environment a active
	 * read is not allowed. Therefore this method
	 * is deactivated
	 */
    @Override
    protected int readIncoming() throws IOException, ClosedConnectionException {
        return 0;
    }

    /**
	 * the "real" read method
	 * 
	 */
    public int receive() {
        assert (Dispatcher.isDispatcherThread()) : "must be performed in single threaded dispatcher to avoid read mess";
        try {
            return super.readIncoming();
        } catch (IOException ioe) {
            close();
            return 0;
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    protected void onConnect() {
        super.onConnect();
        connectionListener.onConnectionConnectEvent(this);
    }

    /**
	 * return the attached application handler
	 * @return the attached application handler
	 */
    IHandler getAttachedAppHandler() {
        return attachedAppHandler;
    }

    /**
	 * attach a application handler
	 * @param attachedAppHandler the application handler to attach
	 */
    void setAttachedAppHandler(IHandler attachedAppHandler) {
        this.attachedAppHandler = attachedAppHandler;
    }

    /** 
	 * get the attached application handler type info object
	 * @return the attached application handler type info object
	 */
    IHandlerTypeInfo getAttachedAppHandlerTypeInfo() {
        return attachedAppHandlerTypeInfo;
    }

    /**
	 * attach a application handler type info object
	 * @param attachedAppHandlerTypeInfo the application handler type info object to attach
	 */
    void setAttachedAppHandlerTypeInfo(IHandlerTypeInfo attachedAppHandlerTypeInfo) {
        this.attachedAppHandlerTypeInfo = attachedAppHandlerTypeInfo;
    }

    boolean isSSLActivated() {
        return super.isSSLOn();
    }

    /**
	 * perform a idle timeout check
	 *  
	 * @param currentTime the current time 
	 * @param connectionTimeout the connection timeout 
	 * @return true, if the timeout has been occured
	 */
    boolean checkIdleTimeoutOccured(long currentTime, long idleTimeout) {
        try {
            if (idleTimeout != Long.MAX_VALUE) {
                if (currentTime > (getLastReceivingTime() + idleTimeout)) {
                    if (isIdleTimeouOccured) {
                        return false;
                    }
                    isIdleTimeouOccured = true;
                    if (LOG.isLoggable(Level.FINE)) {
                        logFine("idle timeout (" + TextUtils.printFormatedDuration(idleTimeout) + ") reached for connection " + toString());
                    }
                    connectionListener.onConnectionIdleTimeoutEvent(this);
                    return true;
                }
            }
        } catch (Throwable t) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("error occured by performing timeout check: " + t.toString());
            }
        }
        return false;
    }

    /**
	 * perform a connection timeout check
	 *  
	 * @param currentTime the current time 
	 * @param connectionTimeout the connection timeout 
	 * @return true, if the timeout has been occured
	 */
    boolean checkConnectionTimeoutOccured(long currentTime, long connectionTimeout) {
        try {
            if (connectionTimeout != Long.MAX_VALUE) {
                if (currentTime > (getConnectionOpenedTime() + connectionTimeout)) {
                    if (isConnectionTimeoutOccured) {
                        return false;
                    }
                    isConnectionTimeoutOccured = true;
                    if (LOG.isLoggable(Level.FINE)) {
                        logFine("connection timeout (" + TextUtils.printFormatedDuration(connectionTimeout) + ") reached for connection " + toString());
                    }
                    connectionListener.onConnectionTimeoutEvent(this);
                    return true;
                }
            }
        } catch (Throwable t) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("error occured by performing timeout check: " + t.toString());
            }
        }
        return false;
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    protected ByteBuffer acquireIOReadMemory() {
        return ioMemoryManager.acquireMemory();
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    protected void recycleIOReadMemory(ByteBuffer buffer) {
        ioMemoryManager.recycleMemory(buffer);
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public String toString() {
        String s = super.toString();
        return (s + ", sendQueueSize=" + writeQueue.getSize());
    }

    private static final class WriteQueue {

        private LinkedList<ByteBuffer> bufferQueue = new LinkedList<ByteBuffer>();

        public synchronized boolean isEmtpy() {
            return bufferQueue.isEmpty();
        }

        public synchronized int append(ByteBuffer[] buffers) {
            int written = 0;
            for (ByteBuffer buffer : buffers) {
                written += buffer.limit() - buffer.position();
                bufferQueue.addLast(buffer);
            }
            return written;
        }

        public synchronized void addFirst(ByteBuffer... buffer) {
            for (int i = (buffer.length - 1); i >= 0; i--) {
                bufferQueue.addFirst(buffer[i]);
            }
        }

        public synchronized LinkedList<ByteBuffer> readAvailable() throws IOException {
            LinkedList<ByteBuffer> result = bufferQueue;
            bufferQueue = new LinkedList<ByteBuffer>();
            return result;
        }

        public synchronized int getSize() {
            int i = 0;
            for (ByteBuffer buffer : bufferQueue) {
                i += buffer.remaining();
            }
            return i;
        }

        @Override
        public String toString() {
            return TextUtils.toTextOrHexString(bufferQueue.toArray(new ByteBuffer[bufferQueue.size()]), "US-ASCII", 500);
        }
    }
}
