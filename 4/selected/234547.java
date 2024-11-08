package org.gamegineer.table.internal.net.transport.tcp;

import static org.gamegineer.common.core.runtime.Assert.assertArgumentNotNull;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.gamegineer.table.internal.net.Loggers;
import org.gamegineer.table.internal.net.transport.IMessage;
import org.gamegineer.table.internal.net.transport.IService;
import org.gamegineer.table.internal.net.transport.IServiceContext;
import org.gamegineer.table.internal.net.transport.MessageEnvelope;

/**
 * A service handler in the TCP transport layer Acceptor-Connector pattern
 * implementation.
 */
@NotThreadSafe
final class ServiceHandler extends AbstractEventHandler implements IServiceContext {

    /** The channel associated with the service handler. */
    private SocketChannel channel_;

    /** The input queue associated with the service handler. */
    private final InputQueue inputQueue_;

    /** The state of the input queue. */
    private QueueState inputQueueState_;

    /** The channel operations in which the handler is interested. */
    private int interestOperations_;

    /** Indicates the service handler has been registered with the dispatcher. */
    private boolean isRegistered_;

    /** Indicates the handler is running. */
    private boolean isRunning_;

    /** The output handler associated with the service handler. */
    private final OutputQueue outputQueue_;

    /** The state of the output queue. */
    private QueueState outputQueueState_;

    /**
     * A snapshot of the channel operations that are ready immediately before
     * the handler is run.
     */
    private int readyOperations_;

    /** The service. */
    private final IService service_;

    /**
     * Initializes a new instance of the {@code AbstractService} class.
     * 
     * @param transportLayer
     *        The transport layer associated with the service handler; must not
     *        be {@code null}.
     * @param service
     *        The service; must not be {@code null}.
     */
    ServiceHandler(final AbstractTransportLayer transportLayer, final IService service) {
        super(transportLayer);
        assert service != null;
        final ByteBufferPool byteBufferPool = transportLayer.getDispatcher().getByteBufferPool();
        channel_ = null;
        inputQueue_ = new InputQueue(byteBufferPool);
        inputQueueState_ = QueueState.OPEN;
        interestOperations_ = SelectionKey.OP_READ;
        isRegistered_ = false;
        isRunning_ = false;
        outputQueue_ = new OutputQueue(byteBufferPool);
        outputQueueState_ = QueueState.OPEN;
        readyOperations_ = 0;
        service_ = service;
    }

    @Override
    void close(final Exception exception) {
        assert isTransportLayerThread();
        final State previousState = getState();
        if (previousState == State.OPEN) {
            if (isRegistered_) {
                isRegistered_ = false;
                getTransportLayer().getDispatcher().unregisterEventHandler(this);
            }
            try {
                channel_.close();
            } catch (final IOException e) {
                Loggers.getDefaultLogger().log(Level.SEVERE, NonNlsMessages.ServiceHandler_close_ioError, e);
            } finally {
                channel_ = null;
            }
        }
        setState(State.CLOSED);
        if (previousState == State.OPEN) {
            service_.stopped(exception);
        }
    }

    /**
     * Drains the output queue to the channel.
     * 
     * @throws java.io.IOException
     *         If an I/O error occurs.
     */
    private void drainOutput() throws IOException {
        if (outputQueueState_ == QueueState.SHUT_DOWN) {
            return;
        }
        if (((readyOperations_ & SelectionKey.OP_WRITE) != 0) && !outputQueue_.isEmpty()) {
            outputQueue_.drainTo(channel_);
        }
        if (outputQueue_.isEmpty()) {
            modifyInterestOperations(0, SelectionKey.OP_WRITE);
        }
    }

    /**
     * Fills the input queue from the channel.
     * 
     * @throws java.io.IOException
     *         If an I/O error occurs.
     */
    private void fillInput() throws IOException {
        if (inputQueueState_ != QueueState.OPEN) {
            return;
        }
        final int bytesRead = inputQueue_.fillFrom(channel_);
        if (bytesRead == -1) {
            modifyInterestOperations(0, SelectionKey.OP_READ);
            if (channel_.socket().isConnected()) {
                try {
                    channel_.socket().shutdownInput();
                } catch (final IOException e) {
                }
            }
            inputQueueState_ = QueueState.SHUTTING_DOWN;
            modifyInterestOperations(SelectionKey.OP_WRITE, 0);
        }
    }

    @Override
    SelectableChannel getChannel() {
        assert isTransportLayerThread();
        return channel_;
    }

    @Override
    int getInterestOperations() {
        assert isTransportLayerThread();
        return interestOperations_;
    }

    /**
     * Modifies the channel operations in which the handler is interested.
     * 
     * @param operationsToSet
     *        A bit mask of channel operations to add to the handler interest
     *        set.
     * @param operationsToReset
     *        A bit mask of channel operations to remove from the handler
     *        interest set.
     */
    private void modifyInterestOperations(final int operationsToSet, final int operationsToReset) {
        interestOperations_ = (interestOperations_ | operationsToSet) & ~operationsToReset;
        if (!isRunning_) {
            getTransportLayer().getDispatcher().enqueueStatusChange(this);
        }
    }

    /**
     * Opens the service handler.
     * 
     * <p>
     * This method must only be called once.
     * </p>
     * 
     * @param channel
     *        The channel associated with the service handler; must not be
     *        {@code null}.
     * 
     * @throws java.io.IOException
     *         If an I/O error occurs
     */
    void open(final SocketChannel channel) throws IOException {
        assert channel != null;
        assert isTransportLayerThread();
        assert getState() == State.PRISTINE;
        channel_ = channel;
        setState(State.OPEN);
        try {
            getTransportLayer().getDispatcher().registerEventHandler(this);
            isRegistered_ = true;
        } catch (final IOException e) {
            close(e);
            throw e;
        }
        service_.started(new ServiceContextProxy());
    }

    @Override
    void prepareToRun() {
        assert isTransportLayerThread();
        final SelectionKey selectionKey = getSelectionKey();
        if (selectionKey != null) {
            interestOperations_ = selectionKey.interestOps();
            readyOperations_ = selectionKey.readyOps();
            isRunning_ = true;
        }
    }

    @Override
    void run() {
        assert isTransportLayerThread();
        if (!isRunning_) {
            return;
        }
        try {
            drainOutput();
            fillInput();
            if (inputQueueState_ != QueueState.SHUT_DOWN) {
                MessageEnvelope messageEnvelope = null;
                while ((messageEnvelope = inputQueue_.dequeueMessageEnvelope()) != null) {
                    service_.messageReceived(messageEnvelope);
                }
                if (inputQueueState_ == QueueState.SHUTTING_DOWN) {
                    inputQueueState_ = QueueState.SHUT_DOWN;
                    service_.peerStopped();
                }
            }
            if ((outputQueueState_ == QueueState.SHUTTING_DOWN) && outputQueue_.isEmpty()) {
                outputQueueState_ = QueueState.SHUT_DOWN;
                close();
            }
        } catch (final Exception e) {
            Loggers.getDefaultLogger().log(Level.SEVERE, NonNlsMessages.ServiceHandler_run_error, e);
            close(e);
        } finally {
            isRunning_ = false;
            readyOperations_ = 0;
        }
    }

    @Override
    public void sendMessage(final IMessage message) {
        assertArgumentNotNull(message, "message");
        assert isTransportLayerThread();
        try {
            outputQueue_.enqueueMessageEnvelope(MessageEnvelope.fromMessage(message));
            modifyInterestOperations(SelectionKey.OP_WRITE, 0);
        } catch (final IOException e) {
            Loggers.getDefaultLogger().log(Level.SEVERE, NonNlsMessages.ServiceHandler_sendMessage_ioError(message), e);
        }
    }

    @Override
    public void stopService() {
        assert isTransportLayerThread();
        if (outputQueueState_ == QueueState.OPEN) {
            if (outputQueue_.isEmpty() && ((interestOperations_ & SelectionKey.OP_WRITE) == 0)) {
                outputQueueState_ = QueueState.SHUT_DOWN;
                close();
            } else {
                outputQueueState_ = QueueState.SHUTTING_DOWN;
            }
        }
    }

    /**
     * The possible states of a service handler queue.
     */
    private enum QueueState {

        /** The queue is open. */
        OPEN, /**
         * The queue is shutting down.
         * 
         * <p>
         * There may be unprocessed data in the queue. For input queues, the
         * underlying stream has been closed. For output queues, the underlying
         * stream will not be closed until the queue has been emptied.
         * </p>
         */
        SHUTTING_DOWN, /**
         * The queue is shut down.
         * 
         * <p>
         * No more data remains in the queue to be processed.
         * </p>
         */
        SHUT_DOWN
    }

    /**
     * A proxy for the {@link IServiceContext} interface that ensures all
     * methods are called on the associated transport layer thread.
     */
    @Immutable
    private final class ServiceContextProxy implements IServiceContext {

        /**
         * Initializes a new instance of the {@code ServiceContextProxy} class.
         */
        ServiceContextProxy() {
        }

        @Override
        public void sendMessage(final IMessage message) {
            try {
                getTransportLayer().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        ServiceHandler.this.sendMessage(message);
                    }
                });
            } catch (final RejectedExecutionException e) {
                Loggers.getDefaultLogger().log(Level.SEVERE, NonNlsMessages.ServiceHandler_transportLayer_shutdown, e);
            }
        }

        @Override
        public void stopService() {
            try {
                getTransportLayer().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        ServiceHandler.this.stopService();
                    }
                });
            } catch (final RejectedExecutionException e) {
                Loggers.getDefaultLogger().log(Level.SEVERE, NonNlsMessages.ServiceHandler_transportLayer_shutdown, e);
            }
        }
    }
}
