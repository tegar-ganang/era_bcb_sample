package org.apache.http.impl.nio.reactor;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.http.nio.reactor.IOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.nio.reactor.IOSession;

/**
 * Generic implementation of {@link IOReactor} that can used as a subclass 
 * for more specialized I/O reactors. It is based on a single {@link Selector}
 * instance.
 * 
 *
 * @version $Revision: 747992 $
 *
 * @since 4.0
 */
public abstract class AbstractIOReactor implements IOReactor {

    private volatile IOReactorStatus status;

    private final Object shutdownMutex;

    private final long selectTimeout;

    private final Selector selector;

    private final Set<IOSession> sessions;

    private final Queue<IOSession> closedSessions;

    private final Queue<ChannelEntry> newChannels;

    /**
     * Creates new AbstractIOReactor instance.
     * 
     * @param selectTimeout the select timeout.
     * @throws IOReactorException in case if a non-recoverable I/O error. 
     */
    public AbstractIOReactor(long selectTimeout) throws IOReactorException {
        super();
        if (selectTimeout <= 0) {
            throw new IllegalArgumentException("Select timeout may not be negative or zero");
        }
        this.selectTimeout = selectTimeout;
        this.sessions = Collections.synchronizedSet(new HashSet<IOSession>());
        this.closedSessions = new ConcurrentLinkedQueue<IOSession>();
        this.newChannels = new ConcurrentLinkedQueue<ChannelEntry>();
        try {
            this.selector = Selector.open();
        } catch (IOException ex) {
            throw new IOReactorException("Failure opening selector", ex);
        }
        this.shutdownMutex = new Object();
        this.status = IOReactorStatus.INACTIVE;
    }

    /**
     * Triggered when the key signals {@link SelectionKey#OP_ACCEPT} readiness.
     * <p>
     * Super-classes can implement this method to react to the event.
     * 
     * @param key the selection key.
     */
    protected abstract void acceptable(SelectionKey key);

    /**
     * Triggered when the key signals {@link SelectionKey#OP_CONNECT} readiness.
     * <p>
     * Super-classes can implement this method to react to the event.
     * 
     * @param key the selection key.
     */
    protected abstract void connectable(SelectionKey key);

    /**
     * Triggered when the key signals {@link SelectionKey#OP_READ} readiness.
     * <p>
     * Super-classes can implement this method to react to the event.
     * 
     * @param key the selection key.
     */
    protected abstract void readable(SelectionKey key);

    /**
     * Triggered when the key signals {@link SelectionKey#OP_WRITE} readiness.
     * <p>
     * Super-classes can implement this method to react to the event.
     * 
     * @param key the selection key.
     */
    protected abstract void writable(SelectionKey key);

    /**
     * Triggered to verify whether the I/O session associated with the 
     * given selection key has not timed out.
     * <p>
     * Super-classes can implement this method to react to the event.
     * 
     * @param key the selection key.
     * @param now current time as long value.
     */
    protected abstract void timeoutCheck(SelectionKey key, long now);

    /**
     * Triggered to validate keys currently registered with the selector. This 
     * method is called after each I/O select loop.
     * <p>
     * Super-classes can implement this method to run validity checks on 
     * active sessions and include additional processing that needs to be
     * executed after each I/O select loop.
     * 
     * @param keys all selection keys registered with the selector.
     */
    protected abstract void validate(Set<SelectionKey> keys);

    /**
     * Triggered when new session has been created.
     * <p>
     * Super-classes can implement this method to react to the event.
     * 
     * @param key the selection key.
     * @param session new I/O session.
     */
    protected abstract void sessionCreated(SelectionKey key, IOSession session);

    /**
     * Triggered when a session has been closed.
     * <p>
     * Super-classes can implement this method to react to the event.
     * 
     * @param session closed I/O session.
     */
    protected abstract void sessionClosed(IOSession session);

    /**
     * Obtains {@link IOSession} instance associated with the given selection
     * key.
     * 
     * @param key the selection key.
     * @return I/O session.
     */
    protected abstract IOSession getSession(SelectionKey key);

    public IOReactorStatus getStatus() {
        return this.status;
    }

    /**
     * Adds new channel entry. The channel will be asynchronously registered
     * with the selector.
     *  
     * @param channelEntry the channel entry.
     */
    public void addChannel(final ChannelEntry channelEntry) {
        if (channelEntry == null) {
            throw new IllegalArgumentException("Channel entry may not be null");
        }
        this.newChannels.add(channelEntry);
        this.selector.wakeup();
    }

    /**
     * Activates the I/O reactor. The I/O reactor will start reacting to 
     * I/O events and triggering notification methods.
     * <p>
     * This method will enter the infinite I/O select loop on 
     * the {@link Selector} instance associated with this I/O reactor.
     * <p>
     * The method will remain blocked unto the I/O reactor is shut down or the
     * execution thread is interrupted. 
     * 
     * @see #acceptable(SelectionKey)
     * @see #connectable(SelectionKey)
     * @see #readable(SelectionKey)
     * @see #writable(SelectionKey)
     * @see #timeoutCheck(SelectionKey, long)
     * @see #validate(Set)
     * @see #sessionCreated(SelectionKey, IOSession)
     * @see #sessionClosed(IOSession)
     * 
     * @throws InterruptedIOException if the dispatch thread is interrupted. 
     * @throws IOReactorException in case if a non-recoverable I/O error. 
     */
    protected void execute() throws InterruptedIOException, IOReactorException {
        this.status = IOReactorStatus.ACTIVE;
        try {
            for (; ; ) {
                int readyCount;
                try {
                    readyCount = this.selector.select(this.selectTimeout);
                } catch (InterruptedIOException ex) {
                    throw ex;
                } catch (IOException ex) {
                    throw new IOReactorException("Unexpected selector failure", ex);
                }
                if (this.status == IOReactorStatus.SHUT_DOWN) {
                    break;
                }
                if (this.status == IOReactorStatus.SHUTTING_DOWN) {
                    closeSessions();
                    closeNewChannels();
                }
                if (readyCount > 0) {
                    processEvents(this.selector.selectedKeys());
                }
                validate(this.selector.keys());
                processClosedSessions();
                if (this.status == IOReactorStatus.ACTIVE) {
                    processNewChannels();
                }
                if (this.status.compareTo(IOReactorStatus.ACTIVE) > 0 && this.sessions.isEmpty()) {
                    break;
                }
            }
            closeActiveChannels();
        } catch (ClosedSelectorException ex) {
        } finally {
            synchronized (this.shutdownMutex) {
                this.status = IOReactorStatus.SHUT_DOWN;
                this.shutdownMutex.notifyAll();
            }
        }
    }

    private void processEvents(final Set<SelectionKey> selectedKeys) {
        for (Iterator<SelectionKey> it = selectedKeys.iterator(); it.hasNext(); ) {
            SelectionKey key = it.next();
            processEvent(key);
        }
        selectedKeys.clear();
    }

    /**
     * Processes new event on the given selection key.
     * 
     * @param key the selection key that triggered an event.
     */
    protected void processEvent(final SelectionKey key) {
        try {
            if (key.isAcceptable()) {
                acceptable(key);
            }
            if (key.isConnectable()) {
                connectable(key);
            }
            if (key.isReadable()) {
                readable(key);
            }
            if (key.isWritable()) {
                writable(key);
            }
        } catch (CancelledKeyException ex) {
            IOSession session = getSession(key);
            queueClosedSession(session);
            key.attach(null);
        }
    }

    /**
     * Queues the given I/O session to be processed asynchronously as closed.
     *  
     * @param session the closed I/O session.
     */
    protected void queueClosedSession(final IOSession session) {
        if (session != null) {
            this.closedSessions.add(session);
        }
    }

    private void processNewChannels() throws IOReactorException {
        ChannelEntry entry;
        while ((entry = this.newChannels.poll()) != null) {
            SocketChannel channel;
            SelectionKey key;
            try {
                channel = entry.getChannel();
                channel.configureBlocking(false);
                key = channel.register(this.selector, 0);
            } catch (ClosedChannelException ex) {
                SessionRequestImpl sessionRequest = entry.getSessionRequest();
                if (sessionRequest != null) {
                    sessionRequest.failed(ex);
                }
                return;
            } catch (IOException ex) {
                throw new IOReactorException("Failure registering channel " + "with the selector", ex);
            }
            IOSession session = new IOSessionImpl(key, new SessionClosedCallback() {

                public void sessionClosed(IOSession session) {
                    queueClosedSession(session);
                }
            });
            int timeout = 0;
            try {
                timeout = channel.socket().getSoTimeout();
            } catch (IOException ex) {
            }
            session.setAttribute(IOSession.ATTACHMENT_KEY, entry.getAttachment());
            session.setSocketTimeout(timeout);
            this.sessions.add(session);
            try {
                sessionCreated(key, session);
                SessionRequestImpl sessionRequest = entry.getSessionRequest();
                if (sessionRequest != null) {
                    sessionRequest.completed(session);
                }
            } catch (CancelledKeyException ex) {
                queueClosedSession(session);
                key.attach(null);
            }
        }
    }

    private void processClosedSessions() {
        IOSession session;
        while ((session = this.closedSessions.poll()) != null) {
            if (this.sessions.remove(session)) {
                try {
                    sessionClosed(session);
                } catch (CancelledKeyException ex) {
                }
            }
        }
    }

    /**
     * Closes out all I/O sessions maintained by this I/O reactor.
     */
    protected void closeSessions() {
        synchronized (this.sessions) {
            for (Iterator<IOSession> it = this.sessions.iterator(); it.hasNext(); ) {
                IOSession session = it.next();
                session.close();
            }
        }
    }

    /**
     * Closes out all new channels pending registration with the selector of 
     * this I/O reactor.
     * @throws IOReactorException - not thrown currently
     */
    protected void closeNewChannels() throws IOReactorException {
        ChannelEntry entry;
        while ((entry = this.newChannels.poll()) != null) {
            SessionRequestImpl sessionRequest = entry.getSessionRequest();
            if (sessionRequest != null) {
                sessionRequest.cancel();
            }
            SocketChannel channel = entry.getChannel();
            try {
                channel.close();
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * Closes out all active channels registered with the selector of 
     * this I/O reactor.
     * @throws IOReactorException - not thrown currently
     */
    protected void closeActiveChannels() throws IOReactorException {
        Set<SelectionKey> keys = this.selector.keys();
        for (Iterator<SelectionKey> it = keys.iterator(); it.hasNext(); ) {
            try {
                SelectionKey key = it.next();
                Channel channel = key.channel();
                if (channel != null) {
                    channel.close();
                }
            } catch (IOException ignore) {
            }
        }
        try {
            this.selector.close();
        } catch (IOException ignore) {
        }
    }

    /**
     * Attempts graceful shutdown of this I/O reactor.
     */
    public void gracefulShutdown() {
        if (this.status != IOReactorStatus.ACTIVE) {
            return;
        }
        this.status = IOReactorStatus.SHUTTING_DOWN;
        this.selector.wakeup();
    }

    /**
     * Attempts force-shutdown of this I/O reactor.
     */
    public void hardShutdown() throws IOReactorException {
        if (this.status == IOReactorStatus.SHUT_DOWN) {
            return;
        }
        this.status = IOReactorStatus.SHUT_DOWN;
        closeNewChannels();
        closeActiveChannels();
    }

    /**
     * Blocks for the given period of time in milliseconds awaiting 
     * the completion of the reactor shutdown.
     *  
     * @param timeout the maximum wait time.
     * @throws InterruptedException if interrupted.
     */
    public void awaitShutdown(long timeout) throws InterruptedException {
        synchronized (this.shutdownMutex) {
            long deadline = System.currentTimeMillis() + timeout;
            long remaining = timeout;
            while (this.status != IOReactorStatus.SHUT_DOWN) {
                this.shutdownMutex.wait(remaining);
                if (timeout > 0) {
                    remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
        }
    }

    public void shutdown(long gracePeriod) throws IOReactorException {
        if (this.status != IOReactorStatus.INACTIVE) {
            gracefulShutdown();
            try {
                awaitShutdown(gracePeriod);
            } catch (InterruptedException ignore) {
            }
        }
        if (this.status != IOReactorStatus.SHUT_DOWN) {
            hardShutdown();
        }
    }

    public void shutdown() throws IOReactorException {
        shutdown(1000);
    }
}
