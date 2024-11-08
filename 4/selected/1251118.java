package net.sf.insim4j.client.impl;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.sf.insim4j.InSimHost;
import net.sf.insim4j.client.InSimClient;
import net.sf.insim4j.client.InSimClientStateListener;
import net.sf.insim4j.client.InSimClientStateListener.InSimClientState;
import net.sf.insim4j.client.InSimError;
import net.sf.insim4j.client.ResponseHandler;
import net.sf.insim4j.client.impl.ChangeRequest.ChangeRequestType;
import net.sf.insim4j.i18n.ExceptionMessages;
import net.sf.insim4j.i18n.LogMessages;
import net.sf.insim4j.insim.InSimRequestPacket;
import net.sf.insim4j.utils.FormatUtils;
import net.sf.insim4j.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for NIO clients.
 *
 * @author Jiří Sotona
 *
 */
abstract class AbstractInSimNioClient implements InSimClient {

    private static class ResponseHandlerThreadFactory implements ThreadFactory {

        private final ThreadGroup fGroup;

        private final String fNamePrefix;

        private final AtomicInteger fThreadNumber = new AtomicInteger(1);

        public ResponseHandlerThreadFactory(final String namePrefix) {
            final SecurityManager s = System.getSecurityManager();
            fGroup = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            fNamePrefix = namePrefix;
        }

        @Override
        public Thread newThread(final Runnable runnable) {
            final Thread handlerThread = new Thread(fGroup, runnable, fNamePrefix + fThreadNumber.getAndIncrement());
            handlerThread.setDaemon(true);
            if (handlerThread.getPriority() != Thread.NORM_PRIORITY) {
                handlerThread.setPriority(Thread.NORM_PRIORITY);
            }
            return handlerThread;
        }
    }

    /**
	 * Channel Information.
	 *
	 * @author Jiří Sotona
	 *
	 */
    class ConnectionInfo {

        /** Host to which channel is connected. */
        private final InSimHost fHost;

        /** Channel to remote machine. */
        private final SelectableChannel fChannel;

        /**
		 * Selection key of this connection. Initially null, set by selector
		 * thread while registering channel
		 */
        private SelectionKey fSelectionKey;

        /** Local port from which connection is initialized. */
        private final int fLocalPort;

        /** Handler which will handle responses on this channel. */
        private final ResponseHandler fResponseHandler;

        /**
		 * Constructor.
		 *
		 * @param inSimHost
		 *            host to which channel is connected
		 * @param channel connection channel
		 * @param localPort
		 *            local port from which connection is initialized
		 * @param responseHandler
		 *            handler which will handle responses on this channel
		 * @throws IllegalArgumentException
		 *             if Host is null
		 * @throws IllegalArgumentException
		 *             if port is negative or in system range (<1, 1024>
		 *             inclusive)
		 * @throws IllegalArgumentException
		 *             if ResponseHandler is null
		 */
        public ConnectionInfo(final InSimHost inSimHost, final SelectableChannel channel, final int localPort, final ResponseHandler responseHandler) {
            this.checkHost(inSimHost);
            this.checkChannel(channel);
            this.checkLocalPort(localPort);
            this.checkResponseHandler(responseHandler);
            fHost = inSimHost;
            fChannel = channel;
            fLocalPort = localPort;
            fResponseHandler = responseHandler;
        }

        /**
		 * Check validity.
		 *
		 * @param inSimHost
		 *
		 * @throws IllegalArgumentException
		 *             if Host is null
		 */
        private void checkHost(final InSimHost inSimHost) {
            if (inSimHost == null) {
                throw new IllegalArgumentException(AbstractInSimNioClient.FORMAT_UTILS.format(ExceptionMessages.getString("Object.iae.nonNull"), "Host"));
            }
        }

        /**
		 * Check validity.
		 *
		 * @param localPort
		 *
		 * @throws IllegalArgumentException
		 *             if port does not meet requirements. Requirements are:
		 *             localPort == 0 || (1023 < localPort && localPort < 65536)
		 */
        private void checkLocalPort(final int localPort) {
            if ((localPort < 0) || ((0 < localPort) && (localPort < 1024)) || (65535 < localPort)) {
                throw new IllegalArgumentException(AbstractInSimNioClient.FORMAT_UTILS.format(ExceptionMessages.getString("Number.iae.wrongPortNumber"), "LocalPort"));
            }
        }

        /**
		 * Check validity.
		 *
		 * @param responseHandler
		 * @throws IllegalArgumentException
		 *             if ResponseHandler is null
		 */
        private void checkResponseHandler(final ResponseHandler responseHandler) {
            if (responseHandler == null) {
                throw new IllegalArgumentException(AbstractInSimNioClient.FORMAT_UTILS.format(ExceptionMessages.getString("Object.iae.nonNull"), "Response Handler"));
            }
        }

        /**
		 * Check validity.
		 *
		 * @param responseHandler
		 * @throws IllegalArgumentException
		 *             if channel is null
		 */
        private void checkChannel(final SelectableChannel channel) {
            if (channel == null) {
                throw new IllegalArgumentException(AbstractInSimNioClient.FORMAT_UTILS.format(ExceptionMessages.getString("Object.iae.nonNull"), "Channel"));
            }
        }

        /**
		 * Getter.<br />
		 * Host to which channel is connected.
		 *
		 * @return the host
		 */
        InSimHost getHost() {
            return fHost;
        }

        /**
		 * @return connection channel
		 */
        SelectableChannel getChannel() {
            return fChannel;
        }

        /**
		 * Getter.<br />
		 * Local port from which connection is initialized.
		 *
		 * @return the localPort
		 */
        int getLocalPort() {
            return fLocalPort;
        }

        /**
		 * Getter.<br />
		 * Handler which will handle responses on this channel.
		 *
		 * @return the responseHandler
		 */
        ResponseHandler getResponseHandler() {
            return fResponseHandler;
        }

        /**
		 * @return selection key
		 */
        SelectionKey getSelectionKey() {
            return fSelectionKey;
        }

        /**
		 *
		 * @param selectionKey
		 */
        void setSelectionKey(final SelectionKey selectionKey) {
            fSelectionKey = selectionKey;
        }
    }

    private static final int BUFFER_SIZE = 1024;

    /**
	 * Log for this class. NOT static. For more details see
	 * http://commons.apache.org/logging/guide.html#Obtaining%20a%20Log%20Object
	 */
    private final Logger logger = LoggerFactory.getLogger(AbstractInSimNioClient.class);

    /** Formatting utilities */
    protected static final FormatUtils FORMAT_UTILS = FormatUtils.getInstance();

    /** Used by descendants. */
    protected static final StringUtils STRING_UTILS = StringUtils.getInstance();

    /** Time step to check if connection attempt was successful. */
    private static final int TIME_STEP = 100;

    /** Timeout of selector. */
    private static final int SELECTOR_TIMEOUT = 3000;

    /** Main cycle control variable. */
    private boolean fStopRequested = false;

    /** Indicates if selector thread is running. */
    private boolean fRunning = false;

    /** The buffer into which we'll read data when it's available. */
    protected ByteBuffer fReadBuffer = ByteBuffer.allocate(AbstractInSimNioClient.BUFFER_SIZE);

    /** The selector we'll be monitoring. */
    private Selector fSelector;

    /** A list of PendingChange instances. */
    private final List<ChangeRequest> fChangeRequests = new LinkedList<ChangeRequest>();

    /**
	 * Maps InSimRequestPacket instances to SelectableChannel. Models
	 * connections to multiple LFS instances - each connection is represented by
	 * one channel and data which are to be sent to LFS.
	 */
    Map<SelectableChannel, List<ByteBuffer>> fPendingRequestData = new HashMap<SelectableChannel, List<ByteBuffer>>();

    /**
	 * Maps a {@link InSimHost} to a {@link ConnectionInfo}.
	 * {@link ConnectionInfo} holds info about connection to {@link InSimHost}
	 */
    protected final Map<InSimHost, ConnectionInfo> fConnectionsInfo = Collections.synchronizedMap(new HashMap<InSimHost, ConnectionInfo>());

    /**
	 * Main selector thread that run main selector loop -
	 * {@link #selectorThreadBody()}
	 */
    private final Thread fSelectorThread;

    /** Executor to run event handling. */
    private final ExecutorService fExecService;

    /** State listeners to be notified about client state changes. */
    private final Set<InSimClientStateListener> fStateListeners;

    /**
	 * Constructor.
	 *
	 */
    public AbstractInSimNioClient() {
        final ThreadFactory tf = new ResponseHandlerThreadFactory("ResponseHandlerThread-");
        fExecService = Executors.newSingleThreadExecutor(tf);
        fSelector = null;
        fSelectorThread = new Thread(new Runnable() {

            @Override
            public void run() {
                AbstractInSimNioClient.this.selectorThreadBody();
            }
        }, "SelectorThread");
        fSelectorThread.setDaemon(true);
        fStateListeners = new CopyOnWriteArraySet<InSimClientStateListener>();
    }

    @Override
    public final InSimHost connect(final InSimHost inSimHost, final ResponseHandler responseHandler) throws UnknownHostException, IOException {
        return this.connect(inSimHost, 0, responseHandler, 500);
    }

    @Override
    public final InSimHost connect(final InSimHost inSimHost, final int localPort, final ResponseHandler responseHandler) throws UnknownHostException, IOException {
        return this.connect(inSimHost, localPort, responseHandler, 500);
    }

    @Override
    public final InSimHost connect(final InSimHost inSimHost, final int localPort, final ResponseHandler responseHandler, final int timeout) throws UnknownHostException, IOException {
        if (fSelector == null) {
            fSelector = this.initSelector();
            this.notifyStateListeners(InSimClientState.RUNNING);
        }
        if (!fSelectorThread.isAlive()) {
            fSelectorThread.start();
        }
        final InSimHost host = new InSimHost(inSimHost);
        final ConnectionInfo connectionInfo = this.initiateConnection(localPort, host, responseHandler);
        fConnectionsInfo.put(host, connectionInfo);
        fSelector.wakeup();
        for (int time = 0; time < timeout; time += AbstractInSimNioClient.TIME_STEP) {
            try {
                Thread.sleep(AbstractInSimNioClient.TIME_STEP);
            } catch (final InterruptedException e) {
            }
            if (this.isConnected(host)) {
                return host;
            }
        }
        return null;
    }

    @Override
    public final void disconnect(final InSimHost inSimHost) {
        final ConnectionInfo connInfo = fConnectionsInfo.remove(inSimHost);
        final SelectableChannel channel = connInfo.getChannel();
        if (channel != null) {
            synchronized (fChangeRequests) {
                fChangeRequests.add(new ChangeRequest(connInfo, ChangeRequestType.CANCEL, 0));
            }
        }
        fSelector.wakeup();
        logger.info(AbstractInSimNioClient.FORMAT_UTILS.format(LogMessages.getString("Client.disconnected"), inSimHost));
    }

    @Override
    public abstract boolean isConnected(final InSimHost host);

    @Override
    public final void send(final InSimHost inSimHost, final InSimRequestPacket requestPacket) {
        final ConnectionInfo connInfo = fConnectionsInfo.get(inSimHost);
        final SelectableChannel channel = connInfo.getChannel();
        if ((channel == null) || !this.isConnected(inSimHost)) {
            throw new IllegalStateException(AbstractInSimNioClient.FORMAT_UTILS.format(ExceptionMessages.getString("InSimClient.ise.connectionNotEstablished"), inSimHost != null ? inSimHost : "null"));
        }
        synchronized (fPendingRequestData) {
            List<ByteBuffer> queue = fPendingRequestData.get(channel);
            if (queue == null) {
                queue = new ArrayList<ByteBuffer>();
                fPendingRequestData.put(channel, queue);
            }
            final ByteBuffer buf = requestPacket.compile();
            buf.rewind();
            queue.add(buf);
        }
        synchronized (fChangeRequests) {
            fChangeRequests.add(new ChangeRequest(connInfo, ChangeRequestType.CHANGE_OPS, SelectionKey.OP_WRITE));
        }
        logger.debug(AbstractInSimNioClient.FORMAT_UTILS.format(LogMessages.getString("Client.send.packetQueued"), inSimHost, requestPacket));
        fSelector.wakeup();
    }

    @Override
    public final void requestStop() {
        synchronized (this) {
            fStopRequested = true;
        }
        fSelector.wakeup();
        this.notifyStateListeners(InSimClientState.STOP_REQUESTED);
    }

    @Override
    public final boolean isRunning() {
        return fRunning;
    }

    @Override
    public final void registerStateListener(final InSimClientStateListener listener) {
        fStateListeners.add(listener);
    }

    @Override
    public final void unregisterStateListener(final InSimClientStateListener listener) {
        fStateListeners.remove(listener);
    }

    /**
	 * Main selector loop.
	 */
    void selectorThreadBody() {
        fRunning = true;
        while (!this.isStopRequested()) {
            synchronized (fChangeRequests) {
                for (final ChangeRequest change : fChangeRequests) {
                    try {
                        switch(change.getType()) {
                            case CHANGE_OPS:
                                {
                                    final ConnectionInfo connInfo = change.getConnectionInfo();
                                    final SelectionKey key = connInfo.getSelectionKey();
                                    if (key != null) {
                                        key.interestOps(change.getOps());
                                    } else {
                                        logger.warn(AbstractInSimNioClient.FORMAT_UTILS.format(LogMessages.getString("Client.channel.nullKey"), change));
                                    }
                                    break;
                                }
                            case REGISTER:
                                {
                                    final ConnectionInfo connInfo = change.getConnectionInfo();
                                    final SelectableChannel channel = connInfo.getChannel();
                                    final SelectionKey key = channel.register(fSelector, change.getOps());
                                    key.attach(connInfo);
                                    connInfo.setSelectionKey(key);
                                    break;
                                }
                            case CANCEL:
                                {
                                    final ConnectionInfo connInfo = change.getConnectionInfo();
                                    final SelectionKey key = connInfo.getSelectionKey();
                                    key.cancel();
                                    key.channel().close();
                                    break;
                                }
                        }
                    } catch (final ClosedChannelException cche) {
                        logger.warn("Exception: ", cche);
                    } catch (final IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }
                fChangeRequests.clear();
            }
            try {
                fSelector.select(AbstractInSimNioClient.SELECTOR_TIMEOUT);
            } catch (final IOException ioe) {
                throw new RuntimeException(ioe);
            }
            final Iterator<SelectionKey> selectedKeys = fSelector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                final SelectionKey key = selectedKeys.next();
                selectedKeys.remove();
                if (!key.isValid()) {
                    continue;
                }
                try {
                    if (key.isConnectable()) {
                        this.finishConnection(key);
                    } else if (key.isReadable()) {
                        this.read(key);
                        final ConnectionInfo connInfo = (ConnectionInfo) key.attachment();
                        final int size = fReadBuffer.position();
                        if (size == 0) {
                            logger.warn("Empty packet recieved from connection " + connInfo.getHost());
                            continue;
                        }
                        final byte[] data = new byte[size];
                        System.arraycopy(fReadBuffer.array(), 0, data, 0, data.length);
                        this.handleResponse(connInfo, data);
                    } else if (key.isWritable()) {
                        this.write(key);
                    }
                } catch (final IOException ioe) {
                    final ConnectionInfo connInfo = (ConnectionInfo) key.attachment();
                    logger.debug(AbstractInSimNioClient.FORMAT_UTILS.format(ExceptionMessages.getString("Client.ioe.channel"), key.channel(), ioe));
                    this.handleException(connInfo, ioe);
                }
            }
        }
        fRunning = false;
        this.notifyStateListeners(InSimClientState.STOPPED);
    }

    /**
	 * Getter.
	 *
	 * @return true if selector stop is requested; false otherwise
	 */
    private synchronized boolean isStopRequested() {
        return fStopRequested;
    }

    /**
	 * Notify registered listeners about state change.
	 *
	 * @param newState
	 *            new state of InSim client
	 */
    private void notifyStateListeners(final InSimClientState newState) {
        for (final InSimClientStateListener listener : fStateListeners) {
            listener.stateChanged(newState);
        }
    }

    /**
	 * Handle response coming from LFS.
	 *
	 * @param channel
	 *            channel the data are coming from
	 * @param responseData
	 *            coming data
	 */
    private void handleResponse(final ConnectionInfo connectionInfo, final byte[] responseData) {
        final ResponseHandler handler = connectionInfo.getResponseHandler();
        final InSimHost host = connectionInfo.getHost();
        fExecService.execute(new Runnable() {

            @Override
            public void run() {
                handler.handleResponse(new InSimResponseDataImpl(host, responseData));
            }
        });
    }

    /**
	 * Handle exception which occurred during communication on channel.
	 *
	 * @param channel
	 *            channel the exception is related to
	 * @param exception
	 *            exception occurred
	 */
    private void handleException(final ConnectionInfo connectionInfo, final Exception exception) {
        final ResponseHandler handler = connectionInfo.getResponseHandler();
        final InSimHost host = connectionInfo.getHost();
        fExecService.execute(new Runnable() {

            @Override
            public void run() {
                handler.handleError(new InSimError(host, exception));
            }
        });
    }

    /**
	 * Initiate multiplexor of selectable channel.
	 *
	 * @return multiplexor (selector)
	 * @throws IOException
	 */
    private Selector initSelector() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    /**
	 * Add request to queue of instructions for selection thread.
	 *
	 * @param changeRequest
	 */
    protected void addChangeRequest(final ChangeRequest changeRequest) {
        synchronized (fChangeRequests) {
            fChangeRequests.add(changeRequest);
        }
    }

    /**
	 * Initiate connection.
	 *
	 * @param localPort
	 *            local port from which connection will be initialized
	 * @param inSimHost
	 *            host to connect to
	 * @param responseHandler handler to be notified about responses
	 * @return channel of connection
	 * @throws IOException
	 *             if an I/O error occurs
	 */
    protected abstract ConnectionInfo initiateConnection(final int localPort, final InSimHost inSimHost, final ResponseHandler responseHandler) throws IOException;

    /**
	 * Finish connection initialization.
	 *
	 * @param selectionKey
	 * @throws IOException
	 *             if an I/O error occurs
	 */
    protected abstract void finishConnection(final SelectionKey selectionKey) throws IOException;

    /**
	 * Read data
	 *
	 * @param selectionKey
	 *            key which identifies channel to read from
	 * @return read data or empty array if error
	 * @throws IOException
	 *             if an I/O error occurs
	 */
    protected abstract byte[] read(final SelectionKey selectionKey) throws IOException;

    /**
	 * Write data.
	 *
	 * @param selectionKey
	 *            key which identifies channel to write to
	 * @throws IOException
	 *             if an I/O error occurs
	 */
    protected abstract void write(final SelectionKey selectionKey) throws IOException;
}
