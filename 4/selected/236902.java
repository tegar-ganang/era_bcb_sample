package net.sf.insim4j.client.impl.communicator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
import net.sf.insim4j.client.communicator.Communicator;
import net.sf.insim4j.client.communicator.Communicator.StateListener.CommunicatorState;
import net.sf.insim4j.client.impl.communicator.ChangeRequest.ChangeRequestType;
import net.sf.insim4j.i18n.ExceptionMessages;
import net.sf.insim4j.i18n.LogMessages;
import net.sf.insim4j.utils.FormatUtils;
import net.sf.insim4j.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link Communicator}s.
 *
 * @author Jiří Sotona
 *
 */
abstract class AbstractCommunicator implements Communicator {

    /**
	 * Channel Information.
	 *
	 * @author Jiří Sotona
	 *
	 */
    class ConnectionInfo {

        /** Host to which channel is connected. */
        private final InetSocketAddress fHostAddress;

        /** Channel to remote machine. */
        private final SelectableChannel fChannel;

        /**
		 * Selection key of this connection. Initially null, set by selector
		 * thread while registering channel
		 */
        private SelectionKey fSelectionKey;

        /**
		 * Constructor.
		 *
		 * @param hostAddress
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
        public ConnectionInfo(final InetSocketAddress hostAddress, final SelectableChannel channel) {
            this.checkChannel(channel);
            fHostAddress = hostAddress;
            fChannel = channel;
        }

        /**
		 * Check validity of channel.
		 *
		 * @param responseHandler
		 * @throws IllegalArgumentException
		 *             if channel is null
		 */
        private void checkChannel(final SelectableChannel channel) {
            if (channel == null) {
                throw new IllegalArgumentException(fFormatUtils.format(ExceptionMessages.getString("Object.iae.nonNull"), "Channel"));
            }
        }

        /**
		 * Getter.<br />
		 * Host to which channel is connected.
		 *
		 * @return the host
		 */
        InetSocketAddress getHost() {
            return fHostAddress;
        }

        /**
		 * @return connection channel
		 */
        SelectableChannel getChannel() {
            return fChannel;
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
    private final Logger logger = LoggerFactory.getLogger(AbstractCommunicator.class);

    /** Time step to check if connection attempt was successful. */
    private static final int TIME_STEP = 100;

    /** Timeout of selector. */
    private static final int SELECTOR_TIMEOUT = 3000;

    /** Main cycle control variable. */
    private boolean fStopRequested = false;

    /** Indicates if selector thread is running. */
    private boolean fRunning = false;

    /** The buffer into which we'll read data when it's available. */
    protected ByteBuffer fReadBuffer = ByteBuffer.allocate(AbstractCommunicator.BUFFER_SIZE);

    /** The selector we will be monitoring. */
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
	 * Maps a {@link InetAddress} to a {@link ConnectionInfo}.
	 * {@link ConnectionInfo} holds info about connection to {@link InetAddress}
	 */
    protected final Map<InetSocketAddress, ConnectionInfo> fConnectionsInfo = Collections.synchronizedMap(new HashMap<InetSocketAddress, ConnectionInfo>());

    /**
	 * Main selector thread that run main selector loop -
	 * {@link #selectorThreadBody()}
	 */
    private final Thread fSelectorThread;

    /** Root response handler. Responsible for notifying registered response
	 * handlers. */
    final NotificationDispatcher fNotificationDispatcher;

    protected final ChangeRequestFactory fChangeRequestFactory;

    protected final StringUtils fStringUtils;

    protected final FormatUtils fFormatUtils;

    /**
	 * Constructor.
	 *
	 */
    protected AbstractCommunicator(final ChangeRequestFactory changeRequestFactory, final StringUtils stringUtils, final FormatUtils formatUtils) {
        fChangeRequestFactory = changeRequestFactory;
        fStringUtils = stringUtils;
        fFormatUtils = formatUtils;
        fSelector = null;
        fSelectorThread = new Thread(new Runnable() {

            @Override
            public void run() {
                AbstractCommunicator.this.selectorThreadBody();
            }
        }, "SelectorThread");
        fSelectorThread.setDaemon(true);
        fNotificationDispatcher = new NotificationDispatcher();
    }

    @Override
    public void connect(final InetSocketAddress hostAddress) throws UnknownHostException, IOException {
        this.connect(hostAddress, 0, 500);
    }

    @Override
    public void connect(final InetSocketAddress hostAddress, final int localPort) throws UnknownHostException, IOException {
        this.connect(hostAddress, localPort, 500);
    }

    @Override
    public void connect(final InetSocketAddress hostAddress, final int localPort, final int timeout) throws UnknownHostException, IOException {
        if (fSelector == null) {
            fSelector = this.initSelector();
        }
        if (!fSelectorThread.isAlive()) {
            fSelectorThread.start();
        }
        final ConnectionInfo connectionInfo = this.initiateConnection(localPort, hostAddress);
        fConnectionsInfo.put(hostAddress, connectionInfo);
        fSelector.wakeup();
        for (int time = 0; time < timeout; time += AbstractCommunicator.TIME_STEP) {
            try {
                Thread.sleep(AbstractCommunicator.TIME_STEP);
            } catch (final InterruptedException e) {
            }
            if (this.isConnected(hostAddress)) {
                return;
            }
        }
    }

    @Override
    public final void disconnect(final InetSocketAddress hostAddress) {
        final ConnectionInfo connInfo = fConnectionsInfo.remove(hostAddress);
        final SelectableChannel channel = connInfo.getChannel();
        if (channel != null) {
            synchronized (fChangeRequests) {
                fChangeRequests.add(fChangeRequestFactory.createChangeRequest(connInfo, ChangeRequestType.CANCEL, 0));
            }
        }
        fSelector.wakeup();
        logger.info(fFormatUtils.format(LogMessages.getString("Client.disconnected"), hostAddress));
    }

    @Override
    public abstract boolean isConnected(final InetSocketAddress hostAddress);

    @Override
    public final void send(final InetSocketAddress hostAddress, final byte[] data) {
        final ConnectionInfo connInfo = fConnectionsInfo.get(hostAddress);
        final SelectableChannel channel = connInfo.getChannel();
        if (channel == null || !this.isConnected(hostAddress)) {
            throw new IllegalStateException(fFormatUtils.format(ExceptionMessages.getString("InSimClient.ise.connectionNotEstablished"), hostAddress != null ? hostAddress : "null"));
        }
        synchronized (fPendingRequestData) {
            List<ByteBuffer> sendQueue = fPendingRequestData.get(channel);
            if (sendQueue == null) {
                sendQueue = new ArrayList<ByteBuffer>();
                fPendingRequestData.put(channel, sendQueue);
            }
            final ByteBuffer buf = ByteBuffer.wrap(data);
            buf.rewind();
            sendQueue.add(buf);
        }
        synchronized (fChangeRequests) {
            fChangeRequests.add(fChangeRequestFactory.createChangeRequest(connInfo, ChangeRequestType.CHANGE_OPS, SelectionKey.OP_WRITE));
        }
        fSelector.wakeup();
    }

    @Override
    public final void requestStop() {
        synchronized (this) {
            fStopRequested = true;
        }
        fSelector.wakeup();
        this.notifyStateListeners(CommunicatorState.STOP_REQUESTED);
    }

    @Override
    public final boolean isRunning() {
        return fRunning;
    }

    @Override
    public final void addDataListener(final DataListener listener) {
        fNotificationDispatcher.addDataListener(listener);
    }

    @Override
    public final void removeDataListener(final DataListener listener) {
        fNotificationDispatcher.removeDataListener(listener);
    }

    @Override
    public final void addStateListener(final StateListener listener) {
        fNotificationDispatcher.addStateListener(listener);
    }

    @Override
    public final void removeStateListener(final StateListener listener) {
        fNotificationDispatcher.removeStateListener(listener);
    }

    /**
	 * Main selector loop.
	 */
    void selectorThreadBody() {
        fRunning = true;
        this.notifyStateListeners(CommunicatorState.RUNNING);
        while (!this.isStopRequested()) {
            this.processChangeRequests();
            try {
                fSelector.select(AbstractCommunicator.SELECTOR_TIMEOUT);
            } catch (final IOException ioe) {
                throw new RuntimeException(ioe);
            }
            final Iterator<SelectionKey> selectedKeys = fSelector.selectedKeys().iterator();
            this.processSelectedKeys(selectedKeys);
        }
        fRunning = false;
        this.notifyStateListeners(CommunicatorState.STOPPED);
    }

    /**
	 * Process change requests: channel interest ops, channel registration
	 * within selector, cancel of channel within selector, ...
	 */
    private void processChangeRequests() {
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
                                    logger.warn(fFormatUtils.format(LogMessages.getString("Client.channel.nullKey"), change));
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
    }

    /**
	 * Process selected keys: read data from channel, write data to channel,
	 * connect channel, ...
	 *
	 * @param selectedKeys
	 */
    private void processSelectedKeys(final Iterator<SelectionKey> selectedKeys) {
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
                logger.debug(fFormatUtils.format(ExceptionMessages.getString("Client.ioe.channel"), key.channel(), ioe));
                this.handleException(connInfo, ioe);
            }
        }
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
    private void notifyStateListeners(final CommunicatorState newState) {
        fNotificationDispatcher.dispatchStateChange(newState);
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
        final InetSocketAddress hostAddress = connectionInfo.getHost();
        fNotificationDispatcher.dispatchData(hostAddress, responseData);
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
        final InetSocketAddress hostAddress = connectionInfo.getHost();
        fNotificationDispatcher.dispatchError(hostAddress, exception);
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
    protected abstract ConnectionInfo initiateConnection(final int localPort, final InetSocketAddress hostAddress) throws IOException;

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
