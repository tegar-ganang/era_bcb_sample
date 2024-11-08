package org.gamegineer.table.internal.net.transport.tcp;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.gamegineer.common.core.util.concurrent.SynchronousFuture;
import org.gamegineer.common.core.util.concurrent.TaskUtils;
import org.gamegineer.table.internal.net.Activator;
import org.gamegineer.table.internal.net.Debug;
import org.gamegineer.table.internal.net.Loggers;

/**
 * An event dispatcher in the TCP transport layer Acceptor-Connector pattern
 * implementation.
 * 
 * <p>
 * An event dispatcher is responsible for managing event handlers and
 * appropriately dispatching events that occur on the event handler channels.
 * </p>
 * 
 * <p>
 * All client methods of this class are expected to be invoked on the associated
 * transport layer thread except where explicitly noted.
 * </p>
 */
@NotThreadSafe
final class Dispatcher {

    /** The byte buffer pool associated with the dispatcher. */
    private final ByteBufferPool bufferPool_;

    /**
     * The asynchronous completion token for the task executing the event
     * dispatch thread or {@code null} if the event dispatch thread is not
     * running.
     */
    private Future<?> eventDispatchTaskFuture_;

    /** The event handler shutdown timeout in milliseconds. */
    private long eventHandlerShutdownTimeout_;

    /** The collection of registered event handlers. */
    private final Collection<AbstractEventHandler> eventHandlers_;

    /**
     * The dispatcher channel multiplexor executing on the event dispatch thread
     * or {@code null} if the event dispatch thread is not running.
     */
    @GuardedBy("selectorGuard_")
    private Selector selector_;

    /** The selector lock. */
    private final ReadWriteLock selectorGuard_;

    /** The dispatcher state. */
    private State state_;

    /** The event handler status change queue. */
    private final Queue<AbstractEventHandler> statusChangeQueue_;

    /** The transport layer associated with the dispatcher. */
    private final AbstractTransportLayer transportLayer_;

    /**
     * Initializes a new instance of the {@code Dispatcher} class.
     * 
     * @param transportLayer
     *        The transport layer associated with the dispatcher; must not be
     *        {@code null}.
     */
    Dispatcher(final AbstractTransportLayer transportLayer) {
        assert transportLayer != null;
        bufferPool_ = new ByteBufferPool(4096);
        eventDispatchTaskFuture_ = null;
        eventHandlerShutdownTimeout_ = 10000L;
        eventHandlers_ = new ArrayList<AbstractEventHandler>();
        selector_ = null;
        selectorGuard_ = new ReentrantReadWriteLock();
        state_ = State.PRISTINE;
        statusChangeQueue_ = new LinkedList<AbstractEventHandler>();
        transportLayer_ = transportLayer;
    }

    /**
     * Acquires the selector guard.
     */
    private void acquireSelectorGuard() {
        selectorGuard_.readLock().lock();
        if (selector_ != null) {
            selector_.wakeup();
        }
    }

    Future<Void> beginClose() {
        assert isTransportLayerThread();
        if (state_ != State.OPEN) {
            state_ = State.CLOSED;
            return new SynchronousFuture<Void>();
        }
        final Closer closer = new Closer();
        return Activator.getDefault().getExecutorService().submit(new Callable<Void>() {

            @Override
            public Void call() {
                closer.close();
                return null;
            }
        });
    }

    /**
     * Processes event handlers in the status change queue.
     */
    private void checkStatusChangeQueue() {
        AbstractEventHandler eventHandler = null;
        while ((eventHandler = statusChangeQueue_.poll()) != null) {
            if (eventHandler.getState() != State.CLOSED) {
                resumeSelection(eventHandler);
            }
        }
    }

    /**
     * Dispatches events on all channels registered with the specified selector
     * until this thread is interrupted.
     * 
     * @param selector
     *        The event selector; must not be {@code null}.
     */
    private void dispatchEvents(final Selector selector) {
        assert selector != null;
        Thread.currentThread().setName(NonNlsMessages.Dispatcher_eventDispatchThread_name);
        Debug.getDefault().trace(Debug.OPTION_DEFAULT, "Event dispatch thread started");
        try {
            while (!Thread.interrupted()) {
                selectorGuardBarrier();
                final int readyKeyCount = selector.select();
                final Set<SelectionKey> selectionKeys = (readyKeyCount > 0) ? selector.selectedKeys() : Collections.<SelectionKey>emptySet();
                try {
                    try {
                        transportLayer_.syncExec(new Runnable() {

                            @Override
                            @SuppressWarnings("synthetic-access")
                            public void run() {
                                checkStatusChangeQueue();
                                for (final SelectionKey selectionKey : selectionKeys) {
                                    processEvents(selectionKey);
                                }
                            }
                        });
                    } catch (final ExecutionException e) {
                        throw TaskUtils.launderThrowable(e.getCause());
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } finally {
                    selectionKeys.clear();
                }
            }
        } catch (final Exception e) {
            Loggers.getDefaultLogger().log(Level.SEVERE, NonNlsMessages.Dispatcher_dispatchEvents_error, e);
        } finally {
            Debug.getDefault().trace(Debug.OPTION_DEFAULT, "Event dispatch thread stopped");
        }
    }

    /**
     * Ends an asynchronous operation to close the dispatcher.
     * 
     * <p>
     * This method does nothing if the dispatcher is already closed.
     * </p>
     * 
     * <p>
     * This method may be called from any thread. It must not be called on the
     * transport layer thread if the operation is not done.
     * </p>
     * 
     * @param future
     *        The asynchronous completion token associated with the operation;
     *        must not be {@code null}.
     * 
     * @throws java.lang.InterruptedException
     *         If this thread is interrupted while waiting for the operation to
     *         complete.
     */
    void endClose(final Future<Void> future) throws InterruptedException {
        assert future != null;
        assert !isTransportLayerThread() || future.isDone();
        try {
            future.get();
        } catch (final ExecutionException e) {
            throw TaskUtils.launderThrowable(e.getCause());
        }
    }

    /**
     * Adds the specified event handler to the status change queue.
     * 
     * @param eventHandler
     *        The event handler; must not be {@code null}.
     */
    void enqueueStatusChange(final AbstractEventHandler eventHandler) {
        assert eventHandler != null;
        assert isTransportLayerThread();
        statusChangeQueue_.add(eventHandler);
        acquireSelectorGuard();
        releaseSelectorGuard();
    }

    ByteBufferPool getByteBufferPool() {
        assert isTransportLayerThread();
        return bufferPool_;
    }

    /**
     * Indicates the current thread is the transport layer thread for the
     * transport layer associated with the dispatcher.
     * 
     * <p>
     * This method may be called from any thread.
     * </p>
     * 
     * @return {@code true} if the current thread is the transport layer thread
     *         for the transport layer associated with the dispatcher; otherwise
     *         {@code false}.
     */
    private boolean isTransportLayerThread() {
        return transportLayer_.isTransportLayerThread();
    }

    /**
     * Opens the dispatcher.
     * 
     * <p>
     * This method must only be called once.
     * </p>
     * 
     * @throws java.io.IOException
     *         If an I/O error occurs.
     */
    void open() throws IOException {
        assert isTransportLayerThread();
        assert state_ == State.PRISTINE;
        final Selector selector;
        try {
            selector = Selector.open();
        } catch (final IOException e) {
            state_ = State.CLOSED;
            throw e;
        }
        state_ = State.OPEN;
        acquireSelectorGuard();
        try {
            selector_ = selector;
            eventDispatchTaskFuture_ = Activator.getDefault().getExecutorService().submit(new Runnable() {

                @Override
                @SuppressWarnings("synthetic-access")
                public void run() {
                    dispatchEvents(selector);
                }
            });
        } finally {
            releaseSelectorGuard();
        }
    }

    /**
     * Processes the events associated with the specified selection key.
     * 
     * @param selectionKey
     *        The selection key; must not be {@code null}.
     */
    private void processEvents(final SelectionKey selectionKey) {
        assert selectionKey != null;
        final AbstractEventHandler eventHandler = (AbstractEventHandler) selectionKey.attachment();
        eventHandler.prepareToRun();
        selectionKey.interestOps(0);
        try {
            eventHandler.run();
        } catch (final RuntimeException e) {
            Loggers.getDefaultLogger().log(Level.SEVERE, NonNlsMessages.Dispatcher_processEvents_unexpectedError, e);
        } finally {
            enqueueStatusChange(eventHandler);
        }
    }

    /**
     * Releases the selector guard.
     */
    private void releaseSelectorGuard() {
        selectorGuard_.readLock().unlock();
    }

    /**
     * Registers the specified event handler.
     * 
     * <p>
     * This method must only be called while the dispatcher is open.
     * </p>
     * 
     * @param eventHandler
     *        The event handler; must not be {@code null}; must not have been
     *        previously registered.
     * 
     * @throws java.io.IOException
     *         If an I/O error occurs.
     */
    void registerEventHandler(final AbstractEventHandler eventHandler) throws IOException {
        assert eventHandler != null;
        assert isTransportLayerThread();
        assert state_ == State.OPEN;
        assert !eventHandlers_.contains(eventHandler);
        acquireSelectorGuard();
        try {
            eventHandler.setSelectionKey(eventHandler.getChannel().register(selector_, eventHandler.getInterestOperations(), eventHandler));
        } finally {
            releaseSelectorGuard();
        }
        eventHandlers_.add(eventHandler);
        Debug.getDefault().trace(Debug.OPTION_DEFAULT, String.format("Registered event handler '%s'", eventHandler));
    }

    /**
     * Resumes event selection of the specified event handler.
     * 
     * @param eventHandler
     *        The event handler; must not be {@code null}.
     */
    private static void resumeSelection(final AbstractEventHandler eventHandler) {
        assert eventHandler != null;
        final SelectionKey selectionKey = eventHandler.getSelectionKey();
        if (selectionKey.isValid()) {
            selectionKey.interestOps(eventHandler.getInterestOperations());
        }
    }

    /**
     * Blocks the event dispatch thread until no client threads hold the
     * selector guard.
     */
    private void selectorGuardBarrier() {
        selectorGuard_.writeLock().lock();
        selectorGuard_.writeLock().unlock();
    }

    /**
     * Sets the event handler shutdown timeout.
     * 
     * <p>
     * This value represents the time the dispatcher will wait for all event
     * handlers to shutdown after it has been requested to close.
     * </p>
     * 
     * @param eventHandlerShutdownTimeout
     *        The event handler shutdown timeout in milliseconds; must not be
     *        negative.
     */
    void setEventHandlerShutdownTimeout(final long eventHandlerShutdownTimeout) {
        assert eventHandlerShutdownTimeout >= 0;
        assert isTransportLayerThread();
        eventHandlerShutdownTimeout_ = eventHandlerShutdownTimeout;
    }

    /**
     * Unregisters the specified event handler.
     * 
     * <p>
     * This method must only be called while the dispatcher is open.
     * </p>
     * 
     * @param eventHandler
     *        The event handler; must not be {@code null}; must have been
     *        previously registered.
     */
    void unregisterEventHandler(final AbstractEventHandler eventHandler) {
        assert eventHandler != null;
        assert isTransportLayerThread();
        assert state_ == State.OPEN;
        final boolean wasEventHandlerRemoved = eventHandlers_.remove(eventHandler);
        assert wasEventHandlerRemoved;
        acquireSelectorGuard();
        try {
            final SelectionKey selectionKey = eventHandler.getSelectionKey();
            if (selectionKey != null) {
                selectionKey.cancel();
                eventHandler.setSelectionKey(null);
            }
        } finally {
            releaseSelectorGuard();
        }
        Debug.getDefault().trace(Debug.OPTION_DEFAULT, String.format("Unregistered event handler '%s'", eventHandler));
    }

    /**
     * Responsible for closing the dispatcher.
     */
    @Immutable
    @SuppressWarnings("synthetic-access")
    private final class Closer {

        /**
         * The asynchronous completion token for the task executing the event
         * dispatch thread.
         */
        @SuppressWarnings("hiding")
        private final Future<?> eventDispatchTaskFuture_;

        /**
         * Initializes a new instance of the {@code Closer} class.
         * 
         * <p>
         * This constructor must be called on the transport layer thread.
         * </p>
         */
        Closer() {
            assert isTransportLayerThread();
            eventDispatchTaskFuture_ = Dispatcher.this.eventDispatchTaskFuture_;
            assert eventDispatchTaskFuture_ != null;
        }

        /**
         * Closes the dispatcher.
         */
        void close() {
            assert !isTransportLayerThread();
            try {
                waitForEventHandlersToShutdown();
                waitForEventDispatchTaskToShutdown();
                closeDispatcher();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Closes the dispatcher.
         * 
         * <p>
         * This method must not be called on the transport layer thread.
         * </p>
         * 
         * @throws java.lang.InterruptedException
         *         If this thread is interrupted while waiting for the
         *         dispatcher to shutdown.
         */
        private void closeDispatcher() throws InterruptedException {
            assert !isTransportLayerThread();
            try {
                transportLayer_.syncExec(new Runnable() {

                    @Override
                    public void run() {
                        Dispatcher.this.eventDispatchTaskFuture_ = null;
                        acquireSelectorGuard();
                        try {
                            selector_.close();
                        } catch (final IOException e) {
                            Loggers.getDefaultLogger().log(Level.SEVERE, NonNlsMessages.Dispatcher_closeDispatcher_error, e);
                        } finally {
                            selector_ = null;
                            releaseSelectorGuard();
                        }
                        state_ = State.CLOSED;
                    }
                });
            } catch (final ExecutionException e) {
                throw TaskUtils.launderThrowable(e.getCause());
            }
        }

        /**
         * Closes any orphaned event handlers before the dispatcher is closed.
         */
        private void closeOrphanedEventHandlers() {
            assert isTransportLayerThread();
            final Collection<AbstractEventHandler> eventHandlers = new ArrayList<AbstractEventHandler>(eventHandlers_);
            for (final AbstractEventHandler eventHandler : eventHandlers) {
                Debug.getDefault().trace(Debug.OPTION_DEFAULT, String.format("Closing orphaned event handler '%s'", eventHandler));
                eventHandler.close();
            }
        }

        /**
         * Waits for the event dispatch task to shutdown.
         * 
         * <p>
         * This method must not be called on the transport layer thread.
         * </p>
         * 
         * @throws java.lang.InterruptedException
         *         If this thread is interrupted while waiting for the event
         *         dispatch task to shutdown.
         */
        private void waitForEventDispatchTaskToShutdown() throws InterruptedException {
            assert !isTransportLayerThread();
            eventDispatchTaskFuture_.cancel(true);
            try {
                eventDispatchTaskFuture_.get(10, TimeUnit.SECONDS);
            } catch (final CancellationException e) {
            } catch (final TimeoutException e) {
                Loggers.getDefaultLogger().log(Level.SEVERE, NonNlsMessages.Dispatcher_waitForEventDispatchTaskToShutdown_timeout, e);
            } catch (final ExecutionException e) {
                throw TaskUtils.launderThrowable(e.getCause());
            }
        }

        /**
         * Waits for all event handlers to shutdown.
         * 
         * <p>
         * If all event handlers are not shutdown within the allowable timeout,
         * they will be forcibly closed.
         * </p>
         * 
         * <p>
         * This method must not be called on the transport layer thread.
         * </p>
         * 
         * @throws java.lang.InterruptedException
         *         If this thread is interrupted while waiting for the event
         *         handlers to shutdown.
         */
        private void waitForEventHandlersToShutdown() throws InterruptedException {
            assert !isTransportLayerThread();
            final long startTime = System.currentTimeMillis();
            while (true) {
                try {
                    final boolean areEventHandlersClosed = transportLayer_.syncExec(new Callable<Boolean>() {

                        @Override
                        public Boolean call() {
                            if (eventHandlers_.isEmpty()) {
                                return Boolean.TRUE;
                            } else if ((System.currentTimeMillis() - startTime) >= eventHandlerShutdownTimeout_) {
                                closeOrphanedEventHandlers();
                                return Boolean.TRUE;
                            }
                            return Boolean.FALSE;
                        }
                    }).booleanValue();
                    if (areEventHandlersClosed) {
                        break;
                    }
                } catch (final ExecutionException e) {
                    throw TaskUtils.launderThrowable(e.getCause());
                }
                Thread.sleep(100L);
            }
        }
    }
}
