package org.xsocket.connection.spi;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xsocket.ClosedException;
import org.xsocket.IDispatcher;
import org.xsocket.IHandle;
import org.xsocket.DataConverter;

/**
 * Socket based io handler
 *
 * @author grro@xsocket.org
 */
final class IoSocketHandler extends ChainableIoHandler implements IHandle {

    private static final Logger LOG = Logger.getLogger(IoSocketHandler.class.getName());

    private static final int MAXSIZE_LOG_READ = 2000;

    @SuppressWarnings("unchecked")
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

    private final IoQueue sendQueue = new IoQueue();

    private String id = null;

    private int idleTimeoutSec = IClientIoProvider.DEFAULT_IDLE_TIMEOUT_SEC;

    private long idleTimeoutDateMillis = Long.MAX_VALUE;

    private int connectionTimeoutSec = IClientIoProvider.DEFAULT_CONNECTION_TIMEOUT_SEC;

    private long connectionTimeoutDateMillis = Long.MAX_VALUE;

    private boolean suspendRead = false;

    private int soRcvbuf = 0;

    private long openTime = -1;

    private long lastTimeReceivedMillis = System.currentTimeMillis();

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
    IoSocketHandler(SocketChannel channel, IoSocketDispatcher dispatcher, String connectionId) throws IOException {
        super(null);
        assert (channel != null);
        this.channel = channel;
        openTime = System.currentTimeMillis();
        channel.configureBlocking(false);
        this.dispatcher = dispatcher;
        this.id = connectionId;
        soRcvbuf = (Integer) getOption(DefaultIoProvider.SO_RCVBUF);
    }

    public void init(IIoHandlerCallback callbackHandler) throws IOException, SocketTimeoutException {
        setPreviousCallback(callbackHandler);
        blockUntilIsConnected();
        dispatcher.register(this, SelectionKey.OP_READ);
    }

    /**
     * {@inheritDoc}
     */
    public boolean reset() {
        try {
            sendQueue.drain();
            resumeRead();
            return super.reset();
        } catch (Exception e) {
            return false;
        }
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDataToSend() {
        return !sendQueue.isEmpty();
    }

    /**
	 * {@inheritDoc}
	 */
    public void setOption(String name, Object value) throws IOException {
        DefaultIoProvider.setOption(channel.socket(), name, value);
        if (name.equals(DefaultIoProvider.SO_RCVBUF)) {
            soRcvbuf = (Integer) value;
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public Object getOption(String name) throws IOException {
        return DefaultIoProvider.getOption(channel.socket(), name);
    }

    /**
	 * {@inheritDoc}
	 */
    @SuppressWarnings("unchecked")
    public Map<String, Class> getOptions() {
        return Collections.unmodifiableMap(SUPPORTED_OPTIONS);
    }

    /**
     * {@inheritDoc}
     */
    public void setIdleTimeoutSec(int timeoutSec) {
        if (timeoutSec <= 0) {
            LOG.warning("connection timeout " + timeoutSec + " sec is invalid");
            return;
        }
        this.idleTimeoutSec = timeoutSec;
        this.idleTimeoutDateMillis = System.currentTimeMillis() + DataConverter.unsignedIntToLong(idleTimeoutSec);
        dispatcher.updateTimeoutCheckPeriod(idleTimeoutSec * 100L);
    }

    /**
	 * sets the connection timeout
	 *
	 * @param timeout the connection timeout
	 */
    public void setConnectionTimeoutSec(int timeoutSec) {
        if (timeoutSec <= 0) {
            LOG.warning("connection timeout " + timeoutSec + " sec is invalid");
            return;
        }
        this.connectionTimeoutSec = timeoutSec;
        this.connectionTimeoutDateMillis = System.currentTimeMillis() + DataConverter.unsignedIntToLong(connectionTimeoutSec);
        dispatcher.updateTimeoutCheckPeriod(connectionTimeoutSec * 100L);
    }

    /**
	 * gets the connection timeout
	 *
	 * @return the connection timeout
	 */
    public int getConnectionTimeoutSec() {
        return connectionTimeoutSec;
    }

    /**
     * {@inheritDoc}
     */
    public int getIdleTimeoutSec() {
        return idleTimeoutSec;
    }

    /**
	 * check the  timeout
	 *
	 * @param currentMillis   the current time
	 * @return true, if the connection has been timed out
	 */
    boolean checkIdleTimeout(Long currentMillis) {
        if (getRemainingSecToIdleTimeout(currentMillis) <= 0) {
            getPreviousCallback().onIdleTimeout();
            return true;
        }
        return false;
    }

    /**
	 * {@inheritDoc}
	 */
    public int getRemainingSecToIdleTimeout() {
        return getRemainingSecToIdleTimeout(System.currentTimeMillis());
    }

    private int getRemainingSecToIdleTimeout(long currentMillis) {
        long remaining = idleTimeoutDateMillis - currentMillis;
        if (remaining > 0) {
            return DataConverter.unsignedLongToInt(remaining);
        } else {
            remaining = (lastTimeReceivedMillis + DataConverter.unsignedIntToLong(idleTimeoutSec)) - currentMillis;
            return DataConverter.unsignedLongToInt(remaining);
        }
    }

    /**
	 * check if the underlying connection is timed out
	 *
	 * @param currentMillis   the current time
	 * @return true, if the connection has been timed out
	 */
    boolean checkConnectionTimeout(Long currentMillis) {
        if (getRemainingSecToConnectionTimeout(currentMillis) <= 0) {
            getPreviousCallback().onConnectionTimeout();
            return true;
        }
        return false;
    }

    /**
	 * {@inheritDoc}
	 */
    public int getRemainingSecToConnectionTimeout() {
        return getRemainingSecToConnectionTimeout(System.currentTimeMillis());
    }

    private int getRemainingSecToConnectionTimeout(long currentMillis) {
        long remaining = connectionTimeoutDateMillis - currentMillis;
        return DataConverter.unsignedLongToInt(remaining);
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

    void onConnectEvent() throws IOException {
        getPreviousCallback().onConnect();
    }

    int onReadableEvent() throws IOException {
        assert (IoSocketDispatcher.isDispatcherThread()) : "receiveQueue can only be accessed by the dispatcher thread";
        int read = 0;
        try {
            ByteBuffer[] received = readSocket();
            if (received != null) {
                getPreviousCallback().onData(received);
            }
            checkPreallocatedReadMemory();
        } catch (ClosedException ce) {
            close(false);
        } catch (Exception t) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + getId() + "] error occured by handling readable event. reason: " + t.toString());
            }
            close(false);
        } catch (Error e) {
            close(false);
            throw e;
        }
        return read;
    }

    int onWriteableEvent() throws IOException {
        assert (IoSocketDispatcher.isDispatcherThread());
        int sent = 0;
        if (suspendRead) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("[" + getId() + "] writeable event occured. update interested to none (because suspendRead is set) and write data to socket");
            }
            updateInterestedSetNonen();
        } else {
            updateInterestedSetRead();
        }
        sent = writeSocket();
        if (sendQueue.isEmpty()) {
            if (shouldClosedPhysically()) {
                realClose();
            }
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("[" + id + "] remaining data to send. initiate sending of the remaining (" + DataConverter.toFormatedBytesSize(sendQueue.getSize()) + ")");
            }
            updateInterestedSetWrite();
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("[" + getId() + "] writeable event handled");
        }
        return sent;
    }

    private void blockUntilIsConnected() throws IOException, SocketTimeoutException {
        while (!getChannel().finishConnect()) {
            getChannel().configureBlocking(true);
            getChannel().finishConnect();
            getChannel().configureBlocking(false);
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
    public void write(ByteBuffer[] buffers) throws IOException {
        if (buffers != null) {
            sendQueue.append(buffers);
            updateInterestedSetWrite();
        }
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

    private void updateInterestedSetWrite() throws ClosedException {
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
            throw new ClosedException("connection " + id + " is already closed", ioe);
        }
    }

    private void updateInterestedSetRead() throws ClosedException {
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
            throw new ClosedException("connection " + id + " is already closed", ioe);
        }
    }

    private void updateInterestedSetNonen() throws ClosedException {
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
            throw new ClosedException("connection " + id + " is already closed", ioe);
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
        if (suspendRead) {
            suspendRead = false;
            updateInterestedSetWrite();
        }
    }

    /**
	 * reads socket into read queue
	 *
	 * @return the received data or <code>null</code>
	 * @throws IOException If some other I/O error occurs
	 * @throws ClosedException if the underlying channel is closed
	 */
    private ByteBuffer[] readSocket() throws IOException {
        assert (IoSocketDispatcher.isDispatcherThread()) : "receiveQueue can only be accessed by the dispatcher thread";
        ByteBuffer[] received = null;
        int read = 0;
        lastTimeReceivedMillis = System.currentTimeMillis();
        if (isOpen() && !suspendRead) {
            assert (memoryManager instanceof UnsynchronizedMemoryManager);
            ByteBuffer readBuffer = memoryManager.acquireMemoryStandardSizeOrPreallocated(soRcvbuf);
            int pos = readBuffer.position();
            int limit = readBuffer.limit();
            try {
                read = channel.read(readBuffer);
            } catch (IOException ioe) {
                readBuffer.position(pos);
                readBuffer.limit(limit);
                memoryManager.recycleMemory(readBuffer);
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("[" + id + "] error occured while reading channel: " + ioe.toString());
                }
                throw ioe;
            }
            switch(read) {
                case -1:
                    memoryManager.recycleMemory(readBuffer);
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + id + "] channel has reached end-of-stream (maybe closed by peer)");
                    }
                    ClosedException cce = new ClosedException("[" + id + "] End of stream reached");
                    throw cce;
                case 0:
                    memoryManager.recycleMemory(readBuffer);
                    return null;
                default:
                    int remainingFreeSize = readBuffer.remaining();
                    ByteBuffer dataBuffer = memoryManager.extractAndRecycleMemory(readBuffer, read);
                    if (received == null) {
                        received = new ByteBuffer[1];
                        received[0] = dataBuffer;
                    }
                    receivedBytes += read;
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("[" + id + "] received (" + (dataBuffer.limit() - dataBuffer.position()) + " bytes, total " + (receivedBytes + read) + " bytes): " + DataConverter.toTextOrHexString(new ByteBuffer[] { dataBuffer.duplicate() }, "UTF-8", MAXSIZE_LOG_READ));
                    }
                    if (remainingFreeSize == 0) {
                        if (read < memoryManager.gettPreallocationBufferSize()) {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("[" + id + "] complete read buffer has been used, initiating repeated read");
                            }
                            ByteBuffer[] repeatedReceived = readSocket();
                            if (repeatedReceived != null) {
                                ByteBuffer[] newReceived = new ByteBuffer[received.length + 1];
                                newReceived[0] = dataBuffer;
                                System.arraycopy(repeatedReceived, 0, newReceived, 1, repeatedReceived.length);
                                received = newReceived;
                                return received;
                            } else {
                                return received;
                            }
                        } else {
                            return received;
                        }
                    }
                    return received;
            }
        } else {
            if (LOG.isLoggable(Level.FINEST)) {
                if (!isOpen()) {
                    LOG.finest("[" + getId() + "] couldn't read socket because socket is already closed");
                }
                if (suspendRead) {
                    LOG.finest("[" + getId() + "] read is suspended, do nothing");
                }
            }
            return null;
        }
    }

    /**
	 * check if preallocated read buffer size is sufficient. if not increase it
	 */
    private void checkPreallocatedReadMemory() {
        assert (IoSocketDispatcher.isDispatcherThread());
        memoryManager.preallocate();
    }

    /**
	 * writes the content of the send queue to the socket
	 *
	 * @throws IOException If some other I/O error occurs
	 * @throws ClosedException if the underlying channel is closed
	 */
    @SuppressWarnings("unchecked")
    private int writeSocket() throws IOException {
        assert (IoSocketDispatcher.isDispatcherThread());
        int sent = 0;
        if (isOpen()) {
            ByteBuffer[] buffers = sendQueue.drain();
            if (buffers == null) {
                return 0;
            }
            boolean hasUnwrittenBuffers = false;
            try {
                for (int i = 0; i < buffers.length; i++) {
                    if (buffers[i] != null) {
                        int writeSize = buffers[i].remaining();
                        if (writeSize > 0) {
                            if (LOG.isLoggable(Level.FINE)) {
                                if (LOG.isLoggable(Level.FINE)) {
                                    LOG.fine("[" + id + "] sending (" + writeSize + " bytes): " + DataConverter.toTextOrHexString(buffers[i].duplicate(), "UTF-8", 500));
                                }
                            }
                            try {
                                int written = channel.write(buffers[i]);
                                sent += written;
                                sendBytes += written;
                                if (written == writeSize) {
                                    try {
                                        getPreviousCallback().onWritten(buffers[i]);
                                    } catch (Exception e) {
                                        if (LOG.isLoggable(Level.FINE)) {
                                            LOG.fine("error occured by notifying that buffer has been written " + e.toString());
                                        }
                                    }
                                    buffers[i] = null;
                                } else {
                                    hasUnwrittenBuffers = true;
                                    if (LOG.isLoggable(Level.FINE)) {
                                        LOG.fine("[" + id + "] " + written + " of " + (writeSize - written) + " bytes has been sent (" + DataConverter.toFormatedBytesSize((writeSize - written)) + ")");
                                    }
                                    break;
                                }
                            } catch (IOException ioe) {
                                if (LOG.isLoggable(Level.FINE)) {
                                    LOG.fine("error " + ioe.toString() + " occured by writing " + DataConverter.toTextOrHexString(buffers[i].duplicate(), "US-ASCII", 500));
                                }
                                try {
                                    getPreviousCallback().onWriteException(ioe, buffers[i]);
                                } catch (Exception e) {
                                    if (LOG.isLoggable(Level.FINE)) {
                                        LOG.fine("error occured by notifying that write exception (" + e.toString() + ") has been occured " + e.toString());
                                    }
                                }
                                buffers[i] = null;
                                return sent;
                            }
                        }
                    }
                }
            } finally {
                if (hasUnwrittenBuffers) {
                    sendQueue.addFirst(buffers);
                }
            }
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
        return sent;
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
            return "(" + channel.socket().getInetAddress().toString() + ":" + channel.socket().getPort() + " -> " + channel.socket().getLocalAddress().toString() + ":" + channel.socket().getLocalPort() + ")" + " received=" + DataConverter.toFormatedBytesSize(receivedBytes) + ", sent=" + DataConverter.toFormatedBytesSize(sendBytes) + ", age=" + DataConverter.toFormatedDuration(System.currentTimeMillis() - openTime) + ", lastReceived=" + DataConverter.toFormatedDate(lastTimeReceivedMillis) + ", sendQueueSize=" + DataConverter.toFormatedBytesSize(sendQueue.getSize()) + " [" + id + "]";
        } catch (Throwable e) {
            return super.toString();
        }
    }
}
