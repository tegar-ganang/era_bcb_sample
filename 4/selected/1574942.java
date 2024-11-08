package org.xsocket.stream.io.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xsocket.ByteBufferQueue;
import org.xsocket.ClosedConnectionException;
import org.xsocket.IDispatcher;
import org.xsocket.IHandle;
import org.xsocket.DataConverter;
import org.xsocket.stream.io.spi.IClientIoProvider;
import org.xsocket.stream.io.spi.IIoHandlerCallback;
import org.xsocket.stream.io.spi.IIoHandlerContext;

/**
 * Socket based io handler
 *
 * @author grro@xsocket.org
 */
final class IoSocketHandler extends ChainableIoHandler implements IHandle {

    private static final Logger LOG = Logger.getLogger(IoSocketHandler.class.getName());

    private static final int MIN_READ_BUFFER_SIZE = 64;

    private static final Map<String, Class> SUPPORTED_OPTIONS = new HashMap<String, Class>();

    static {
        SUPPORTED_OPTIONS.put(IClientIoProvider.SO_RCVBUF, Integer.class);
        SUPPORTED_OPTIONS.put(IClientIoProvider.SO_SNDBUF, Integer.class);
        SUPPORTED_OPTIONS.put(IClientIoProvider.SO_REUSEADDR, Boolean.class);
        SUPPORTED_OPTIONS.put(IClientIoProvider.SO_KEEPALIVE, Boolean.class);
        SUPPORTED_OPTIONS.put(IClientIoProvider.TCP_NODELAY, Boolean.class);
        SUPPORTED_OPTIONS.put(IClientIoProvider.SO_LINGER, Integer.class);
    }

    private boolean isLogicalOpen = true;

    private boolean isDisconnect = false;

    private SocketChannel channel = null;

    private IoSocketDispatcher dispatcher = null;

    private IMemoryManager memoryManager = null;

    private final ByteBufferQueue sendQueue = new ByteBufferQueue();

    private final ByteBufferQueue receiveQueue = new ByteBufferQueue();

    private String id = null;

    private long idleTimeout = Long.MAX_VALUE;

    private long connectionTimeout = Long.MAX_VALUE;

    private boolean suspendRead = false;

    private long openTime = -1;

    private long lastTimeReceived = System.currentTimeMillis();

    private long receivedBytes = 0;

    private long sendBytes = 0;

    /**
	 * constructor
	 *
	 * @param channel         the underlying channel
	 * @param idLocalPrefix   the id namespace prefix
	 * @param dispatcher      the dispatcher
	 * @throws IOException If some other I/O error occurs
	 */
    @SuppressWarnings("unchecked")
    IoSocketHandler(SocketChannel channel, IoSocketDispatcher dispatcher, IIoHandlerContext ctx, String connectionId) throws IOException {
        super(null);
        assert (channel != null);
        this.channel = channel;
        openTime = System.currentTimeMillis();
        channel.configureBlocking(false);
        this.dispatcher = dispatcher;
        this.id = connectionId;
    }

    public void init(IIoHandlerCallback callbackHandler) throws IOException, SocketTimeoutException {
        setPreviousCallback(callbackHandler);
        blockUntilIsConnected();
        dispatcher.register(this, SelectionKey.OP_READ);
    }

    void setMemoryManager(IMemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPendingWriteDataSize() {
        return sendQueue.getSize() + super.getPendingWriteDataSize();
    }

    int getPendingReceiveDataSize() {
        return receiveQueue.getSize() + super.getPendingReceiveDataSize();
    }

    /**
	 * {@inheritDoc}
	 */
    public void setOption(String name, Object value) throws IOException {
        IoProvider.setOption(channel.socket(), name, value);
    }

    /**
	 * {@inheritDoc}
	 */
    public Object getOption(String name) throws IOException {
        return IoProvider.getOption(channel.socket(), name);
    }

    /**
	 * {@inheritDoc}
	 */
    public Map<String, Class> getOptions() {
        return Collections.unmodifiableMap(SUPPORTED_OPTIONS);
    }

    /**
     * {@inheritDoc}
     */
    public void setIdleTimeoutSec(int timeout) {
        if (timeout <= 0) {
            LOG.warning("connection timeout " + timeout + " sec is invalid");
            return;
        }
        long timeoutMillis = ((long) timeout) * 1000L;
        this.idleTimeout = timeoutMillis;
        dispatcher.updateTimeoutCheckPeriod((long) timeout * 250L);
    }

    /**
	 * sets the connection timout
	 *
	 * @param timeout the connection timeout
	 */
    public void setConnectionTimeoutSec(int timeout) {
        if (timeout <= 0) {
            LOG.warning("connection timeout " + timeout + " sec is invalid");
            return;
        }
        long timeoutMillis = ((long) timeout) * 1000L;
        this.connectionTimeout = timeoutMillis;
        dispatcher.updateTimeoutCheckPeriod((long) timeout * 250L);
    }

    /**
	 * gets the connection timeout
	 *
	 * @return the connection timeout
	 */
    public int getConnectionTimeoutSec() {
        return (int) (connectionTimeout / 1000);
    }

    /**
     * {@inheritDoc}
     */
    public int getIdleTimeoutSec() {
        return (int) (idleTimeout / 1000);
    }

    public int getReceiveQueueSize() {
        return receiveQueue.getSize();
    }

    int getSendQueueSize() {
        return sendQueue.getSize();
    }

    /**
	 * check the  timeout
	 *
	 * @param current   the current time
	 * @return true, if the connection has been timed out
	 */
    boolean checkIdleTimeout(Long current) {
        long maxTime = lastTimeReceived + idleTimeout;
        if (maxTime < 0) {
            maxTime = Long.MAX_VALUE;
        }
        boolean timeoutReached = maxTime < current;
        if (timeoutReached) {
            getPreviousCallback().onIdleTimeout();
        }
        return timeoutReached;
    }

    /**
	 * check if the underyling connection is timed out
	 *
	 * @param current   the current time
	 * @return true, if the connection has been timed out
	 */
    void checkConnection() {
        if (!channel.isOpen()) {
            getPreviousCallback().onConnectionAbnormalTerminated();
        }
    }

    /**
	 * check if the underyling connection is timed out
	 *
	 * @param current   the current time
	 * @return true, if the connection has been timed out
	 */
    boolean checkConnectionTimeout(Long current) {
        long maxTime = openTime + connectionTimeout;
        if (maxTime < 0) {
            maxTime = Long.MAX_VALUE;
        }
        boolean timeoutReached = maxTime < current;
        if (timeoutReached) {
            getPreviousCallback().onConnectionTimeout();
        }
        return timeoutReached;
    }

    /**
	 * return the size of the read queue
	 *
	 * @return the read queue size
	 */
    int getIncomingQueueSize() {
        return receiveQueue.getSize();
    }

    void onConnectEvent() throws IOException {
        getPreviousCallback().onConnect();
    }

    void onReadableEvent() throws IOException {
        assert (IoSocketDispatcher.isDispatcherThread());
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("[" + getId() + "] readable event occured for");
        }
        try {
            readSocketIntoReceiveQueue();
            if (getReceiveQueueSize() > 0) {
                getPreviousCallback().onDataRead();
            }
            checkPreallocatedReadMemory();
        } catch (ClosedConnectionException ce) {
            close(false);
        } catch (Throwable t) {
            close(false);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] error occured by handling readable event. reason: " + t.toString());
            }
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("[" + getId() + "] readable event handled");
        }
    }

    void onWriteableEvent() throws IOException {
        assert (IoSocketDispatcher.isDispatcherThread());
        if (suspendRead) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("[" + getId() + "] writeable event occured. update interested to none (because suspendRead is set) and write data to socket");
            }
            updateInterestedSetNonen();
        } else {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("[" + getId() + "] writeable event occured. update interested to read and write data to socket");
            }
            updateInterestedSetRead();
        }
        try {
            writeSendQueueDataToSocket();
            getPreviousCallback().onWritten();
        } catch (IOException ioe) {
            getPreviousCallback().onWriteException(ioe);
        }
        if (getSendQueueSize() > 0) {
            getDispatcher().updateInterestSet(this, SelectionKey.OP_WRITE);
        } else {
            if (shouldClosedPhysically()) {
                realClose();
            }
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("[" + getId() + "] writeable event handled");
        }
    }

    private void blockUntilIsConnected() throws IOException, SocketTimeoutException {
        while (!getChannel().finishConnect()) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException ignore) {
            }
        }
    }

    private boolean shouldClosedPhysically() {
        if (!isLogicalOpen) {
            if (sendQueue.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
	 * {@inheritDoc}
	 */
    @SuppressWarnings("unchecked")
    public void writeOutgoing(ByteBuffer buffer) throws IOException {
        if (buffer != null) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("appending " + buffer.remaining() + " bytes to send buffer, and update interestedSet to write");
            }
            sendQueue.append(buffer);
            updateInterestedSetWrite();
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @SuppressWarnings("unchecked")
    public void writeOutgoing(LinkedList<ByteBuffer> buffers) throws IOException {
        if (buffers != null) {
            if (LOG.isLoggable(Level.FINEST)) {
                int size = 0;
                for (ByteBuffer buffer : buffers) {
                    size += buffer.remaining();
                }
                LOG.finest("appending " + size + " bytes to send buffer, and update interestedSet to write");
            }
            sendQueue.append(buffers);
            updateInterestedSetWrite();
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public LinkedList<ByteBuffer> drainIncoming() {
        return receiveQueue.drain();
    }

    /**
	 * {@inheritDoc}
	 */
    @SuppressWarnings("unchecked")
    public void close(boolean immediate) throws IOException {
        if (immediate || sendQueue.isEmpty()) {
            realClose();
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("postpone close until remaning data to write (" + sendQueue.getSize() + ") has been written");
            }
            isLogicalOpen = false;
            updateInterestedSetWrite();
        }
    }

    private void realClose() {
        try {
            getDispatcher().deregister(this);
        } catch (Exception e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("error occured by deregistering connection " + id + " on dispatcher. reason: " + e.toString());
            }
        }
        try {
            channel.close();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("connection " + id + " has been closed");
            }
        } catch (Exception e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("error occured by closing connection " + id + " reason: " + e.toString());
            }
        }
        if (!isDisconnect) {
            isDisconnect = true;
            getPreviousCallback().onDisconnect();
        }
    }

    void onDispatcherClose() {
        getPreviousCallback().onConnectionAbnormalTerminated();
    }

    private void updateInterestedSetWrite() throws ClosedConnectionException {
        try {
            dispatcher.updateInterestSet(this, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("couldn`t update interested set to write data on socket. Reason: " + ioe.toString());
            }
            try {
                dispatcher.deregister(this);
            } catch (Exception ignore) {
            }
            throw new ClosedConnectionException("connection " + id + " is already closed", ioe);
        }
    }

    private void updateInterestedSetRead() throws ClosedConnectionException {
        try {
            dispatcher.updateInterestSet(this, SelectionKey.OP_READ);
        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("couldn`t update interested set to read data. Reason: " + ioe.toString());
            }
            try {
                dispatcher.deregister(this);
            } catch (Exception ignore) {
            }
            throw new ClosedConnectionException("connection " + id + " is already closed", ioe);
        }
    }

    private void updateInterestedSetNonen() throws ClosedConnectionException {
        try {
            dispatcher.updateInterestSet(this, 0);
        } catch (IOException ioe) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("could not update interested set to nonen. Reason: " + ioe.toString());
            }
            try {
                dispatcher.deregister(this);
            } catch (Exception ignore) {
            }
            throw new ClosedConnectionException("connection " + id + " is already closed", ioe);
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public boolean isOpen() {
        return channel.isOpen();
    }

    /**
	 * return the underlying channel
	 *
	 * @return the underlying channel
	 */
    public SocketChannel getChannel() {
        return channel;
    }

    IDispatcher<IoSocketHandler> getDispatcher() {
        return dispatcher;
    }

    @Override
    public void suspendRead() throws IOException {
        suspendRead = true;
        updateInterestedSetWrite();
    }

    @Override
    public void resumeRead() throws IOException {
        suspendRead = false;
        updateInterestedSetWrite();
    }

    /**
	 * reads socket into read queue
	 *
	 * @return the number of read bytes
	 * @throws IOException If some other I/O error occurs
	 * @throws ClosedConnectionException if the underlying channel is closed
	 */
    private int readSocketIntoReceiveQueue() throws IOException {
        assert (IoSocketDispatcher.isDispatcherThread());
        int read = 0;
        lastTimeReceived = System.currentTimeMillis();
        if (isOpen() && !suspendRead) {
            assert (memoryManager instanceof UnsynchronizedMemoryManager);
            ByteBuffer readBuffer = memoryManager.acquireMemory(MIN_READ_BUFFER_SIZE);
            int pos = readBuffer.position();
            int limit = readBuffer.limit();
            try {
                read = channel.read(readBuffer);
            } catch (IOException ioe) {
                readBuffer.position(pos);
                readBuffer.limit(limit);
                memoryManager.recycleMemory(readBuffer, MIN_READ_BUFFER_SIZE);
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + id + "] error occured while reading channel: " + ioe.toString());
                }
                throw ioe;
            }
            switch(read) {
                case -1:
                    memoryManager.recycleMemory(readBuffer, MIN_READ_BUFFER_SIZE);
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + id + "] channel has reached end-of-stream (maybe closed by peer)");
                    }
                    ClosedConnectionException cce = new ClosedConnectionException("[" + id + "] End of stream reached");
                    throw cce;
                case 0:
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest("[" + getId() + "] nothing to read");
                    }
                    memoryManager.recycleMemory(readBuffer, MIN_READ_BUFFER_SIZE);
                    break;
                default:
                    int savePos = readBuffer.position();
                    int saveLimit = readBuffer.limit();
                    readBuffer.position(savePos - read);
                    readBuffer.limit(savePos);
                    ByteBuffer readData = readBuffer.slice();
                    receiveQueue.append(readData);
                    if (readBuffer.hasRemaining()) {
                        readBuffer.position(savePos);
                        readBuffer.limit(saveLimit);
                        memoryManager.recycleMemory(readBuffer, MIN_READ_BUFFER_SIZE);
                    }
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + id + "] received (" + (readData.limit() - readData.position()) + " bytes, total " + (receivedBytes + read) + " bytes): " + DataConverter.toTextOrHexString(new ByteBuffer[] { readData.duplicate() }, "UTF-8", 500));
                    }
                    break;
            }
            receivedBytes += read;
        } else {
            if (LOG.isLoggable(Level.FINEST)) {
                if (!isOpen()) {
                    LOG.finest("[" + getId() + "] couldn't read socket because socket is already closed");
                }
                if (suspendRead) {
                    LOG.finest("[" + getId() + "] read is suspended, do nothing");
                }
            }
        }
        return read;
    }

    /**
	 * check if preallocated read buffer size is sufficient. if not increaese it
	 */
    private void checkPreallocatedReadMemory() {
        assert (IoSocketDispatcher.isDispatcherThread());
        memoryManager.preallocate(MIN_READ_BUFFER_SIZE);
    }

    /**
	 * writes the content of the send queue to the socket
	 *
	 * @throws IOException If some other I/O error occurs
	 * @throws ClosedConnectionException if the underlying channel is closed
	 */
    @SuppressWarnings("unchecked")
    private void writeSendQueueDataToSocket() throws IOException {
        assert (IoSocketDispatcher.isDispatcherThread());
        if (isOpen() && !sendQueue.isEmpty()) {
            ByteBuffer buffer = null;
            do {
                buffer = sendQueue.removeFirst();
                if (buffer != null) {
                    int writeSize = buffer.remaining();
                    if (writeSize > 0) {
                        if (LOG.isLoggable(Level.FINE)) {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("[" + id + "] sending (" + writeSize + " bytes): " + DataConverter.toTextOrHexString(buffer.duplicate(), "UTF-8", 500));
                            }
                        }
                        int written = channel.write(buffer);
                        sendBytes += written;
                        if (written != writeSize) {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("[" + id + "] " + written + " of " + (writeSize - written) + " bytes has been sent. initiate sending of the remaining (total sent " + sendBytes + " bytes)");
                            }
                            sendQueue.addFirst(buffer);
                            updateInterestedSetWrite();
                            break;
                        }
                    }
                }
            } while (buffer != null);
        } else {
            if (LOG.isLoggable(Level.FINEST)) {
                if (!isOpen()) {
                    LOG.finest("[" + getId() + "] couldn't write send queue to socket because socket is already closed (sendQueuesize=" + DataConverter.toFormatedBytesSize(sendQueue.getSize()) + ")");
                }
                if (sendQueue.isEmpty()) {
                    LOG.finest("[" + getId() + "] nothing to write, because send queue is empty ");
                }
            }
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public final InetAddress getLocalAddress() {
        return channel.socket().getLocalAddress();
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public final int getLocalPort() {
        return channel.socket().getLocalPort();
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public final InetAddress getRemoteAddress() {
        return channel.socket().getInetAddress();
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public final int getRemotePort() {
        return channel.socket().getPort();
    }

    /**
	 * {@inheritDoc}
	 */
    public void flushOutgoing() {
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public String toString() {
        try {
            return "(" + channel.socket().getInetAddress().toString() + ":" + channel.socket().getPort() + " -> " + channel.socket().getLocalAddress().toString() + ":" + channel.socket().getLocalPort() + ")" + " received=" + DataConverter.toFormatedBytesSize(receivedBytes) + ", sent=" + DataConverter.toFormatedBytesSize(sendBytes) + ", age=" + DataConverter.toFormatedDuration(System.currentTimeMillis() - openTime) + ", lastReceived=" + DataConverter.toFormatedDate(lastTimeReceived) + ", sendQueueSize=" + DataConverter.toFormatedBytesSize(sendQueue.getSize()) + " [" + id + "]";
        } catch (Exception e) {
            return super.toString();
        }
    }
}
