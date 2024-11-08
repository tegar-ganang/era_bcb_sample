package net.jxta.impl.endpoint.tcp;

import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.id.ID;
import net.jxta.impl.endpoint.BlockingMessenger;
import net.jxta.impl.endpoint.EndpointServiceImpl;
import net.jxta.impl.endpoint.msgframing.MessagePackageHeader;
import net.jxta.impl.endpoint.msgframing.WelcomeMessage;
import net.jxta.impl.endpoint.transportMeter.TransportBindingMeter;
import net.jxta.impl.endpoint.transportMeter.TransportMeterBuildSettings;
import net.jxta.impl.util.TimeUtils;
import net.jxta.logging.Logging;
import net.jxta.peer.PeerID;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements a messenger which sends messages via raw TCP sockets.
 */
public class TcpMessenger extends BlockingMessenger implements Runnable {

    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(TcpMessenger.class.getName());

    /**
     * The number of times we allow our write selector to be selected with no
     * progress before we give up.
     */
    private static final int MAX_WRITE_ATTEMPTS = 3;

    /**
     * Description of our current location within the stream.
     */
    private enum readState {

        /**
         * Reading the initial welcome
         */
        WELCOME, /**
         * Reading a message header
         */
        HEADER, /**
         * Reading a message
         */
        BODY
    }

    /**
     * The source address of messages sent on this messenger.
     */
    private final EndpointAddress srcAddress;

    private final MessageElement srcAddressElement;

    /**
     * Cache of the logical destination of this messenger. (It helps if it works even after close)
     */
    private EndpointAddress logicalDestAddress;

    /**
     * The message tcpTransport we are working for.
     */
    private final TcpTransport tcpTransport;

    private EndpointAddress dstAddress = null;

    private EndpointAddress origAddress = null;

    private EndpointAddress fullDstAddress = null;

    private InetAddress inetAddress = null;

    private int port = 0;

    private volatile boolean closed = false;

    private boolean closingDueToFailure = false;

    private WelcomeMessage itsWelcome = null;

    private final long createdAt = TimeUtils.timeNow();

    private long lastUsed = TimeUtils.timeNow();

    private SocketChannel socketChannel = null;

    private TransportBindingMeter transportBindingMeter;

    /**
     * If this is an incoming connection we must not close it when the messenger
     * disappears. It has many reasons to disappear while the connection must
     * keep receiving messages. This is causing some problems for incoming
     * messengers that are managed by some entity, such as the router or the
     * relay. These two do call close explicitly when they discard a messenger,
     * and their intent is truly to close the underlying connection. So
     * basically we need to distinguish between incoming messengers that are
     * abandoned without closing (for these we must protect the input side
     * because that's the only reason for the connection being there) and
     * incoming messengers that are explicitly closed (in which case we must let
     * the entire connection be closed).
     */
    private boolean initiator;

    private AtomicReference<readState> state = new AtomicReference<readState>(readState.WELCOME);

    private static final int MAX_LEN = 4096;

    private ByteBuffer buffer = ByteBuffer.allocate(MAX_LEN);

    /**
     * Header from the current incoming message (if any).
     */
    private MessagePackageHeader header = null;

    /**
     * Time at which we began receiving the current incoming message.
     */
    long receiveBeginTime = 0;

    /**
     * Enforces single writer on message write in case the messenger is being
     * used on multiple threads.
     */
    private final ReentrantLock writeLock = new ReentrantLock();

    /**
     * Create a new TcpMessenger for the specified address.
     *
     * @param socketChannel the SocketChannel for the messenger
     * @param transport     the tcp MessageSender we are working for.
     * @throws java.io.IOException if an io error occurs
     */
    TcpMessenger(SocketChannel socketChannel, TcpTransport transport) throws IOException {
        super(transport.group.getPeerGroupID(), new EndpointAddress(transport.getProtocolName(), socketChannel.socket().getInetAddress().getHostAddress() + ":" + socketChannel.socket().getPort(), null, null), true);
        initiator = false;
        this.socketChannel = socketChannel;
        this.tcpTransport = transport;
        this.srcAddress = transport.getPublicAddress();
        this.srcAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NAME, srcAddress.toString(), null);
        try {
            if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                LOG.info("Connection from " + socketChannel.socket().getInetAddress().getHostAddress() + ":" + socketChannel.socket().getPort());
            }
            Socket socket = socketChannel.socket();
            int useBufferSize = Math.max(TcpTransport.SendBufferSize, socket.getSendBufferSize());
            socket.setSendBufferSize(useBufferSize);
            inetAddress = socketChannel.socket().getInetAddress();
            port = socketChannel.socket().getPort();
            socket.setKeepAlive(true);
            socket.setSoTimeout(TcpTransport.connectionTimeOut);
            socket.setSoLinger(true, TcpTransport.LingerDelay);
            socket.setTcpNoDelay(true);
            dstAddress = new EndpointAddress(this.tcpTransport.getProtocolName(), inetAddress.getHostAddress() + ":" + port, null, null);
            fullDstAddress = dstAddress;
            startMessenger();
        } catch (IOException io) {
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                transportBindingMeter = this.tcpTransport.getUnicastTransportBindingMeter(null, dstAddress);
                if (transportBindingMeter != null) {
                    transportBindingMeter.connectionFailed(initiator, TimeUtils.timeNow() - createdAt);
                }
            }
            if (socketChannel != null) {
                socketChannel.close();
            }
            throw io;
        }
        if (TransportMeterBuildSettings.TRANSPORT_METERING) {
            transportBindingMeter = this.tcpTransport.getUnicastTransportBindingMeter((PeerID) getDestinationPeerID(), dstAddress);
            if (transportBindingMeter != null) {
                transportBindingMeter.connectionEstablished(initiator, TimeUtils.timeNow() - createdAt);
            }
        }
        if (!isConnected()) {
            throw new IOException("Failed to establish connection to " + dstAddress);
        }
    }

    /**
     * Create a new TcpMessenger for the specified address.
     *
     * @param destaddr     the destination of the messenger
     * @param tcpTransport the tcp MessageSender we are working for.
     * @throws java.io.IOException if an io error occurs
     */
    TcpMessenger(EndpointAddress destaddr, TcpTransport tcpTransport) throws IOException {
        this(destaddr, tcpTransport, true);
    }

    /**
     * Create a new TcpMessenger for the specified address.
     *
     * @param destaddr     the destination of the messenger
     * @param tcpTransport the tcp MessageSender we are working for.
     * @param selfDestruct indicates whether the messenger created will self destruct when idle
     * @throws java.io.IOException if an io error occurs
     */
    TcpMessenger(EndpointAddress destaddr, TcpTransport tcpTransport, boolean selfDestruct) throws IOException {
        super(tcpTransport.group.getPeerGroupID(), destaddr, selfDestruct);
        this.origAddress = destaddr;
        initiator = true;
        this.tcpTransport = tcpTransport;
        this.fullDstAddress = destaddr;
        this.dstAddress = new EndpointAddress(destaddr, null, null);
        this.srcAddress = tcpTransport.getPublicAddress();
        srcAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NAME, srcAddress.toString(), null);
        String protoAddr = destaddr.getProtocolAddress();
        int portIndex = protoAddr.lastIndexOf(":");
        if (portIndex == -1) {
            throw new IllegalArgumentException("Invalid Protocol Address (port # missing) ");
        }
        String portString = protoAddr.substring(portIndex + 1);
        try {
            port = Integer.valueOf(portString);
        } catch (NumberFormatException caught) {
            throw new IllegalArgumentException("Invalid Protocol Address (port # invalid): " + portString);
        }
        if ((port <= 0) || (port > 65535)) {
            throw new IllegalArgumentException("Invalid port number in Protocol Address : " + port);
        }
        String hostString = protoAddr.substring(0, portIndex);
        inetAddress = InetAddress.getByName(hostString);
        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("Creating new TCP Connection to : " + dstAddress + " / " + inetAddress.getHostAddress() + ":" + port);
        }
        try {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Connecting to " + inetAddress.getHostAddress() + ":" + port + " via " + this.tcpTransport.usingInterface.getHostAddress() + ":0");
            }
            socketChannel = SocketChannel.open();
            Socket socket = socketChannel.socket();
            SocketAddress bindAddress = new InetSocketAddress(this.tcpTransport.usingInterface, 0);
            socket.bind(bindAddress);
            int useBufferSize = Math.max(TcpTransport.SendBufferSize, socket.getSendBufferSize());
            socket.setSendBufferSize(useBufferSize);
            useBufferSize = Math.max(TcpTransport.RecvBufferSize, socket.getReceiveBufferSize());
            socket.setReceiveBufferSize(useBufferSize);
            socket.setKeepAlive(true);
            socket.setSoTimeout(TcpTransport.connectionTimeOut);
            socket.setSoLinger(true, TcpTransport.LingerDelay);
            socket.setTcpNoDelay(true);
            SocketAddress connectAddress = new InetSocketAddress(inetAddress, port);
            socketChannel.connect(connectAddress);
            startMessenger();
        } catch (IOException io) {
            if (TransportMeterBuildSettings.TRANSPORT_METERING) {
                transportBindingMeter = this.tcpTransport.getUnicastTransportBindingMeter(null, dstAddress);
                if (transportBindingMeter != null) {
                    transportBindingMeter.connectionFailed(initiator, TimeUtils.timeNow() - createdAt);
                }
            }
            if (socketChannel != null) {
                socketChannel.close();
            }
            throw io;
        }
        if (TransportMeterBuildSettings.TRANSPORT_METERING) {
            transportBindingMeter = this.tcpTransport.getUnicastTransportBindingMeter((PeerID) getDestinationPeerID(), dstAddress);
            if (transportBindingMeter != null) {
                transportBindingMeter.connectionEstablished(initiator, TimeUtils.timeNow() - createdAt);
            }
        }
        if (!isConnected()) {
            throw new IOException("Failed to establish connection to " + dstAddress);
        }
    }

    /**
     * The cost of just having a finalize routine is high. The finalizer is
     * a bottleneck and can delay garbage collection all the way to heap
     * exhaustion. Leave this comment as a reminder to future maintainers.
     * Below is the reason why finalize is not needed here.
     * <p/>
     * These messengers are never given to application layers. Endpoint code
     * always calls close when needed.
     * <p/>
     * There used to be an incoming special case in order to *prevent*
     * closure because the inherited finalize used to call close. This is
     * no-longer the case. For the outgoing case, we do not need to call close
     * for the reason explained above.
     */
    protected void finalize() throws Throwable {
        closeImpl();
        super.finalize();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Now everyone knows its closed and the connection can no-longer be
     * obtained. So, we can go about our business of closing it.
     * It can happen that a redundant close() is done but it does not matter.
     * close() is idempotent.
     */
    public synchronized void closeImpl() {
        super.close();
        if (closed) {
            return;
        }
        closed = true;
        setLastUsed(0);
        if (socketChannel != null) {
            tcpTransport.unregister(socketChannel);
            try {
                socketChannel.close();
            } catch (IOException e) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Failed to close messenger " + toString(), e);
                }
            }
            if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                LOG.info((closingDueToFailure ? "Failure" : "Normal") + " close (open " + TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(), createdAt) + "ms) of socket to : " + dstAddress + " / " + inetAddress.getHostAddress() + ":" + port);
                if (closingDueToFailure && Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "stack trace", new Throwable("stack trace"));
                }
            }
        }
        if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
            if (closingDueToFailure) {
                transportBindingMeter.connectionDropped(initiator, TimeUtils.timeNow() - createdAt);
            } else {
                transportBindingMeter.connectionClosed(initiator, TimeUtils.timeNow() - createdAt);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClosed() {
        if (isConnected()) {
            return false;
        }
        super.close();
        return true;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Since we probe the connection status, we'll keep a messenger as long
     * as the connection is active, even if only on the incoming side.
     * So we're being a bit nice to the other side. Anyway, incoming
     * connections do not go away when the messenger does. There's a receive
     * timeout for that.
     */
    public boolean isIdleImpl() {
        return (TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(), getLastUsed()) > 15 * TimeUtils.AMINUTE);
    }

    /**
     * {@inheritDoc}
     */
    public EndpointAddress getLogicalDestinationImpl() {
        return logicalDestAddress;
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessageBImpl(Message message, String service, String serviceParam) throws IOException {
        sendMessageDirect(message, service, serviceParam, false);
    }

    public void sendMessageDirect(Message message, String service, String serviceParam, boolean direct) throws IOException {
        if (isClosed()) {
            IOException failure = new IOException("Messenger was closed, it cannot be used to send messages.");
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, failure.getMessage(), failure);
            }
            throw failure;
        }
        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NS, srcAddressElement);
        EndpointAddress destAddressToUse;
        if (direct) {
            destAddressToUse = origAddress;
        } else {
            destAddressToUse = getDestAddressToUse(service, serviceParam);
        }
        MessageElement dstAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NAME, destAddressToUse.toString(), null);
        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NS, dstAddressElement);
        try {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Sending " + message + " to " + destAddressToUse + " on connection " + getDestinationAddress());
            }
            xmitMessage(message);
        } catch (IOException caught) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Message send failed for " + message, caught);
            }
            closeImpl();
            throw caught;
        }
    }

    private void startMessenger() throws IOException {
        socketChannel.configureBlocking(true);
        WelcomeMessage myWelcome = new WelcomeMessage(fullDstAddress, tcpTransport.getPublicAddress(), tcpTransport.group.getPeerID(), false);
        long written = write(new ByteBuffer[] { myWelcome.getByteBuffer() });
        tcpTransport.incrementBytesSent(written);
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine("welcome message sent");
        }
        while (state.get() == readState.WELCOME) {
            if (TimeUtils.toRelativeTimeMillis(TimeUtils.timeNow(), this.createdAt) > (TcpTransport.connectionTimeOut)) {
                throw new SocketTimeoutException("Failed to receive remote welcome message before timeout.");
            }
            read();
            processBuffer();
        }
        if (!closed) {
            socketChannel.configureBlocking(false);
            tcpTransport.register(socketChannel, this);
        }
    }

    /**
     * Send message to the remote peer.
     *
     * @param msg the message to send.
     * @throws java.io.IOException For errors sending the message.
     */
    private void xmitMessage(Message msg) throws IOException {
        if (closed) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("Connection was closed to : " + dstAddress);
            }
            throw new IOException("Connection was closed to : " + dstAddress);
        }
        long sendBeginTime = TimeUtils.timeNow();
        long size = 0;
        try {
            WireFormatMessage serialed = WireFormatMessageFactory.toWire(msg, WireFormatMessageFactory.DEFAULT_WIRE_MIME, null);
            MessagePackageHeader header = new MessagePackageHeader();
            header.setContentTypeHeader(serialed.getMimeType());
            size = serialed.getByteLength();
            header.setContentLengthHeader(size);
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Sending " + msg + " (" + size + ") to " + dstAddress + " via " + inetAddress.getHostAddress() + ":" + port);
            }
            List<ByteBuffer> partBuffers = new ArrayList<ByteBuffer>();
            partBuffers.add(header.getByteBuffer());
            partBuffers.addAll(Arrays.asList(serialed.getByteBuffers()));
            long written;
            writeLock.lock();
            try {
                written = write(partBuffers.toArray(new ByteBuffer[partBuffers.size()]));
            } finally {
                writeLock.unlock();
            }
            if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                transportBindingMeter.messageSent(initiator, msg, TimeUtils.timeNow() - sendBeginTime, written);
            }
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine(MessageFormat.format("Sent {0} bytes {1} successfully via {2}:{3}", written, msg, inetAddress.getHostAddress(), port));
            }
            tcpTransport.incrementBytesSent(written);
            tcpTransport.incrementMessagesSent();
            setLastUsed(TimeUtils.timeNow());
        } catch (SocketTimeoutException failed) {
            SocketTimeoutException failure = new SocketTimeoutException("Failed sending " + msg + " to : " + inetAddress.getHostAddress() + ":" + port);
            failure.initCause(failed);
            throw failure;
        } catch (IOException failed) {
            if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                transportBindingMeter.sendFailure(initiator, msg, TimeUtils.timeNow() - sendBeginTime, size);
            }
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Message send failed for " + inetAddress.getHostAddress() + ":" + port, failed);
            }
            closingDueToFailure = true;
            close();
            IOException failure = new IOException("Failed sending " + msg + " to : " + inetAddress.getHostAddress() + ":" + port);
            failure.initCause(failed);
            throw failure;
        }
    }

    /**
     * Blocking write of byte buffers to the socket channel.
     *
     * @param byteBuffers The bytes to write.
     * @return The number of bytes written.
     * @throws IOException Thrown for errors while writing message.
     */
    private long write(final ByteBuffer[] byteBuffers) throws IOException {
        long bytesToWrite = 0;
        for (ByteBuffer byteBuffer : byteBuffers) {
            bytesToWrite += byteBuffer.remaining();
        }
        if (bytesToWrite == 0L) {
            return 0L;
        }
        long bytesWritten = 0;
        Selector writeSelector = null;
        SelectionKey wKey = null;
        int attempts = 1;
        try {
            do {
                long wroteBytes;
                do {
                    wroteBytes = socketChannel.write(byteBuffers);
                    bytesWritten += wroteBytes;
                    if (wroteBytes < 0) {
                        throw new EOFException();
                    }
                    if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                        LOG.finer(MessageFormat.format("Wrote {0} bytes", wroteBytes));
                    }
                } while (wroteBytes != 0);
                if (bytesWritten == bytesToWrite) {
                    break;
                }
                attempts++;
                if (attempts > MAX_WRITE_ATTEMPTS) {
                    throw new IOException(MessageFormat.format("Max write attempts ({0}) exceeded ({1})", attempts, MAX_WRITE_ATTEMPTS));
                }
                if (writeSelector == null) {
                    try {
                        writeSelector = tcpTransport.getSelector();
                    } catch (InterruptedException woken) {
                        InterruptedIOException incompleteIO = new InterruptedIOException("Interrupted while acquiring write selector.");
                        incompleteIO.initCause(woken);
                        incompleteIO.bytesTransferred = (int) Math.min(bytesWritten, Integer.MAX_VALUE);
                        throw incompleteIO;
                    }
                    if (writeSelector == null) {
                        continue;
                    }
                    wKey = socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
                }
                int ready = writeSelector.select(TcpTransport.connectionTimeOut);
                if (ready == 0) {
                    throw new SocketTimeoutException("Timeout during socket write");
                } else {
                    attempts--;
                }
            } while (attempts <= MAX_WRITE_ATTEMPTS);
        } finally {
            if (wKey != null) {
                wKey.cancel();
                wKey = null;
            }
            if (writeSelector != null) {
                writeSelector.selectNow();
                tcpTransport.returnSelector(writeSelector);
            }
        }
        return bytesWritten;
    }

    /**
     * parses a welcome from a buffer
     *
     * @param buffer the buffer to parse, if successful the state is set to HEADER
     * @return true if successfully parsed
     */
    private boolean processWelcome(ByteBuffer buffer) {
        try {
            if (itsWelcome == null) {
                itsWelcome = new WelcomeMessage();
            }
            if (!itsWelcome.read(buffer)) {
                return false;
            }
            dstAddress = itsWelcome.getPublicAddress();
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Creating a logical address from : " + itsWelcome.getWelcomeString());
            }
            fullDstAddress = dstAddress;
            logicalDestAddress = new EndpointAddress("jxta", itsWelcome.getPeerID().getUniqueValue().toString(), null, null);
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Hello from " + itsWelcome.getPublicAddress() + " [" + itsWelcome.getPeerID() + "] ");
            }
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Registering Messenger from " + socketChannel.socket().getInetAddress().getHostAddress() + ":" + socketChannel.socket().getPort());
            }
            try {
                tcpTransport.messengerReadyEvent(this, getConnectionAddress());
            } catch (Throwable all) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.log(Level.SEVERE, "Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
                }
                IOException failure = new IOException("Failure announcing messenger.");
                failure.initCause(all);
                throw failure;
            }
        } catch (IOException e) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Error while parsing the welcome message", e);
            }
            closeImpl();
            return false;
        }
        return true;
    }

    /**
     * parses a header from a buffer
     *
     * @param buffer the buffer to parse, if successful the state is set to BODY
     * @return true if successfully parsed
     */
    private boolean processHeader(ByteBuffer buffer) {
        if (null == header) {
            header = new MessagePackageHeader();
        }
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine(MessageFormat.format("{0} Processing message package header, buffer stats:{1}", Thread.currentThread(), buffer.toString()));
        }
        try {
            if (!header.readHeader(buffer)) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine(MessageFormat.format("{0} maintaining current state at header, buffer stats :{1}", Thread.currentThread(), buffer.toString()));
                }
                return false;
            }
        } catch (IOException e) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Error while parsing the message header", e);
            }
            if (!socketChannel.isConnected()) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("SocketChannel closed. Closing the messenger");
                }
                closeImpl();
            }
        }
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine(MessageFormat.format("{0} setting current state to body, Buffer stats :{1}, remaining elements {2}:", Thread.currentThread(), buffer.toString(), buffer.remaining()));
        }
        return true;
    }

    private Message processMessage(ByteBuffer buffer, MessagePackageHeader header) throws IOException {
        MimeMediaType msgMime = header.getContentTypeHeader();
        return WireFormatMessageFactory.fromBuffer(buffer, msgMime, null);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This is what gets run by the Executor. It reads whatever is available,
     * processes it and then goes back to the selector waiting for more IO
     */
    public void run() {
        try {
            while (read()) {
                List<Message> msgs = processBuffer();
                for (Message msg : msgs) {
                    tcpTransport.executor.execute(new MessageProcessor(msg));
                }
            }
            if (socketChannel != null) {
                tcpTransport.register(socketChannel, this);
            }
        } catch (Throwable all) {
            if (Logging.SHOW_SEVERE) {
                LOG.log(Level.SEVERE, "Uncaught Throwable", all);
            }
        }
    }

    /**
     * @return true to indicate read maybe required
     */
    private boolean read() {
        if (closed || socketChannel == null) {
            return false;
        }
        if (!socketChannel.isConnected()) {
            closeImpl();
            return false;
        }
        try {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine(MessageFormat.format("{0} State before read(): {1}, buffer stats : {2}, remaining :{3}", Thread.currentThread(), state.get(), buffer.toString(), buffer.remaining()));
            }
            int read = socketChannel.read(buffer);
            if (read < 0) {
                if (!socketChannel.isConnected() || read < 0) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine(MessageFormat.format("{0} Closing due to EOF", Thread.currentThread()));
                    }
                    closeImpl();
                }
                return false;
            } else if (read == 0) {
                return false;
            }
            tcpTransport.incrementBytesReceived(read);
            buffer.flip();
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine(MessageFormat.format("{0} SocketChannel.read() == {1} bytes. Buffer stats:{2}, remaining {3}", Thread.currentThread(), read, buffer.toString(), buffer.remaining()));
            }
            return true;
        } catch (ClosedChannelException e) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Channel closed while reading data", e);
            }
            closeImpl();
            return false;
        } catch (InterruptedIOException woken) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning(MessageFormat.format("tcp receive - interrupted : read() {0} {1}:{2}", woken.bytesTransferred, inetAddress.getHostAddress(), port));
            }
        } catch (IOException ioe) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "IOException occured while reading data", ioe);
            }
            closeImpl();
            return false;
        } catch (Throwable e) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, MessageFormat.format("tcp receive - Error on connection {0}:{1}", inetAddress.getHostAddress(), port), e);
            }
            closingDueToFailure = true;
            closeImpl();
            return false;
        }
        return (socketChannel.validOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ;
    }

    /**
     * processes the input byte buffer
     * @return the list of messages present in the buffer
     */
    @SuppressWarnings("fallthrough")
    public List<Message> processBuffer() {
        List<Message> msgs = new ArrayList<Message>();
        boolean done = false;
        while (!done) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine(MessageFormat.format("{0} processBuffer({1}). Buffer stats:{2}, elements remaining {3}", Thread.currentThread(), state.getClass(), buffer.toString(), buffer.remaining()));
            }
            switch(state.get()) {
                case WELCOME:
                    boolean wseen = processWelcome(buffer);
                    if (wseen) {
                        state.set(readState.HEADER);
                    }
                    done = true;
                    break;
                case HEADER:
                    boolean hseen = processHeader(buffer);
                    if (!hseen) {
                        done = true;
                        break;
                    }
                    receiveBeginTime = TimeUtils.timeNow();
                    if (header.getContentLengthHeader() > buffer.capacity()) {
                        ByteBuffer src = buffer;
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.fine(MessageFormat.format("{0} Reallocating a new buffer of size {1} to replace :{2}", Thread.currentThread(), header.getContentLengthHeader(), buffer.toString()));
                        }
                        buffer = ByteBuffer.allocate((int) header.getContentLengthHeader());
                        buffer.put(src);
                        buffer.flip();
                    }
                    state.set(readState.BODY);
                case BODY:
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine(MessageFormat.format(" {0} Proccessing Message Body. expecting {1}, {2} elements remaining {3}", Thread.currentThread(), header.getContentLengthHeader(), buffer.toString(), buffer.remaining()));
                    }
                    if (buffer.remaining() >= (int) header.getContentLengthHeader()) {
                        Message msg;
                        try {
                            msg = processMessage(buffer, header);
                        } catch (IOException io) {
                            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                                LOG.log(Level.FINE, "Failed to parse a message from buffer. closing connection", io);
                            }
                            closeImpl();
                            done = true;
                            break;
                        }
                        if (TransportMeterBuildSettings.TRANSPORT_METERING && (transportBindingMeter != null)) {
                            transportBindingMeter.messageReceived(initiator, msg, TimeUtils.timeNow() - receiveBeginTime, header.getContentLengthHeader());
                        }
                        tcpTransport.incrementMessagesReceived();
                        setLastUsed(TimeUtils.timeNow());
                        state.set(readState.HEADER);
                        header = null;
                        msgs.add(msg);
                    } else {
                        done = true;
                        break;
                    }
            }
        }
        buffer.compact();
        return msgs;
    }

    /**
     * A small class for processing individual messages. 
     */
    private class MessageProcessor implements Runnable {

        private Message msg;

        MessageProcessor(Message msg) {
            this.msg = msg;
        }

        public void run() {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine(MessageFormat.format("{0} calling EndpointService.demux({1})", Thread.currentThread(), msg, inetAddress.getHostAddress(), port));
            }
            tcpTransport.endpoint.demux(msg);
        }
    }

    /**
     * return the current connection status.
     *
     * @return true if there is an active connection to the remote peer, otherwise false.
     */
    private boolean isConnected() {
        return !closed;
    }

    /**
     * Return the absolute time in milliseconds at which this Connection was last used.
     *
     * @return absolute time in milliseconds.
     */
    private long getLastUsed() {
        return lastUsed;
    }

    /**
     * Set the last used time for this connection in absolute milliseconds.
     *
     * @param time absolute time in milliseconds.
     */
    private void setLastUsed(long time) {
        lastUsed = time;
    }

    /**
     * Returns the metering object for this tcpTransport
     *
     * @return the metering object for this tcpTransport
     */
    TransportBindingMeter getTransportBindingMeter() {
        return transportBindingMeter;
    }

    /**
     * Returns the remote address
     *
     * @return the remote address
     */
    private EndpointAddress getConnectionAddress() {
        return itsWelcome.getDestinationAddress();
    }

    /**
     * Returns Remote PeerID
     *
     * @return Remote PeerID
     */
    private ID getDestinationPeerID() {
        return itsWelcome.getPeerID();
    }
}
