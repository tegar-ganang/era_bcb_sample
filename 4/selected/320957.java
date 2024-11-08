package org.xsocket.datagram;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xsocket.DataConverter;

/**
 * Implementation of the {@link IDispatcher}
 *
 * <br/><br/><b>This is a xSocket internal class and subject to change</b>
 *
 * @author grro@xsocket.org
 */
final class IoSocketDispatcher implements Runnable, Closeable {

    private static final Logger LOG = Logger.getLogger(IoSocketDispatcher.class.getName());

    static final String DISPATCHER_PREFIX = "xDispatcher";

    private static final long TIMEOUT_SHUTDOWN_MILLIS = 5L * 1000L;

    private final Queue<AbstractChannelBasedEndpoint> registerQueue = new ConcurrentLinkedQueue<AbstractChannelBasedEndpoint>();

    private final Queue<AbstractChannelBasedEndpoint> deregisterQueue = new ConcurrentLinkedQueue<AbstractChannelBasedEndpoint>();

    private final Queue<KeyUpdateTask> keyUpdateQueue = new ConcurrentLinkedQueue<KeyUpdateTask>();

    private volatile boolean isOpen = true;

    private final Selector selector;

    private long handledRegistractions;

    private long handledReads;

    private long handledWrites;

    /**
	 * constructor
	 *
	 */
    public IoSocketDispatcher() {
        try {
            selector = Selector.open();
        } catch (IOException ioe) {
            String text = "exception occured while opening selector. Reason: " + ioe.toString();
            LOG.severe(text);
            throw new RuntimeException(text, ioe);
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @SuppressWarnings("unchecked")
    public void run() {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("selector  listening ...");
        }
        int handledTasks = 0;
        while (isOpen) {
            try {
                int eventCount = selector.select();
                handledTasks = performRegisterHandlerTasks();
                handledTasks += performKeyUpdateTasks();
                if (eventCount > 0) {
                    Set selectedEventKeys = selector.selectedKeys();
                    Iterator it = selectedEventKeys.iterator();
                    while (it.hasNext()) {
                        SelectionKey eventKey = (SelectionKey) it.next();
                        it.remove();
                        AbstractChannelBasedEndpoint handler = (AbstractChannelBasedEndpoint) eventKey.attachment();
                        if (eventKey.isValid() && eventKey.isReadable()) {
                            onReadableEvent(handler);
                        }
                        if (eventKey.isValid() && eventKey.isWritable()) {
                            onWriteableEvent(handler);
                        }
                    }
                }
                handledTasks += performDeregisterHandlerTasks();
            } catch (Exception e) {
                LOG.warning("[" + Thread.currentThread().getName() + "] exception occured while processing. Reason " + DataConverter.toString(e));
            }
        }
        closeDispatcher();
    }

    private void onReadableEvent(AbstractChannelBasedEndpoint handler) {
        try {
            handler.onReadableEvent();
        } catch (Exception e) {
            LOG.warning("[" + Thread.currentThread().getName() + "] exception occured while handling readable event. Reason " + DataConverter.toString(e));
        }
        handledReads++;
    }

    private void onWriteableEvent(AbstractChannelBasedEndpoint handler) {
        try {
            handler.onWriteableEvent();
        } catch (Exception e) {
            LOG.warning("[" + Thread.currentThread().getName() + "] exception occured while handling writeable event. Reason " + DataConverter.toString(e));
        }
        handledWrites++;
    }

    /**
	 * {@inheritDoc}
	 */
    public void register(AbstractChannelBasedEndpoint handler) {
        assert (!handler.getChannel().isBlocking());
        boolean isWakeUpRequired = false;
        if (registerQueue.isEmpty()) {
            isWakeUpRequired = true;
        }
        registerQueue.add(handler);
        if (isWakeUpRequired) {
            wakeUp();
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("does not wake up selector, because register queue handling is currently running");
            }
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void deregister(AbstractChannelBasedEndpoint handler) {
        boolean isWakeUpRequired = false;
        if (deregisterQueue.isEmpty()) {
            isWakeUpRequired = true;
        }
        deregisterQueue.add(handler);
        if (isWakeUpRequired) {
            wakeUp();
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("does not wake up selector, because deregister queue handling is currently running");
            }
        }
    }

    void wakeUp() {
        selector.wakeup();
    }

    void setSelectionKeyToReadImmediately(final AbstractChannelBasedEndpoint handler) {
        SelectionKey key = handler.getChannel().keyFor(selector);
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        key.interestOps(key.interestOps() | SelectionKey.OP_READ);
    }

    private void setSelectionKeyToWriteImmediate(final AbstractChannelBasedEndpoint handler) {
        SelectionKey key = handler.getChannel().keyFor(selector);
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
    }

    void initiateRead(final AbstractChannelBasedEndpoint handler) {
        KeyUpdateTask task = new KeyUpdateTask(handler) {

            public void run() {
                setSelectionKeyToReadImmediately(handler);
            }
        };
        keyUpdateQueue.add(task);
        wakeUp();
    }

    void initiateWrite(final AbstractChannelBasedEndpoint handler) {
        KeyUpdateTask task = new KeyUpdateTask(handler) {

            public void run() {
                setSelectionKeyToWriteImmediate(handler);
            }
        };
        keyUpdateQueue.add(task);
        wakeUp();
    }

    private int performRegisterHandlerTasks() throws IOException {
        int handledTasks = 0;
        while (true) {
            AbstractChannelBasedEndpoint handler = registerQueue.poll();
            if (handler == null) {
                return handledTasks;
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("registering handler " + handler);
            }
            if (handler.getChannel().isOpen()) {
                handler.getChannel().register(selector, SelectionKey.OP_READ, handler);
                handledRegistractions++;
                handledTasks++;
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("channel " + handler.getId() + " is already closed. Could not register it");
                }
            }
        }
    }

    private int performKeyUpdateTasks() {
        int handledTasks = 0;
        while (true) {
            KeyUpdateTask keyUpdateTask = keyUpdateQueue.poll();
            if (keyUpdateTask == null) {
                return handledTasks;
            }
            keyUpdateTask.init();
            if (keyUpdateTask.getKey() != null) {
                if (keyUpdateTask.getKey().isValid()) {
                    keyUpdateTask.run();
                } else {
                    keyUpdateTask.getKey().cancel();
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("handler " + keyUpdateTask.getHandler() + " (key) is invalid. ignore it");
                    }
                }
            }
            handledTasks++;
        }
    }

    private int performDeregisterHandlerTasks() {
        int handledTasks = 0;
        while (true) {
            AbstractChannelBasedEndpoint handler = deregisterQueue.poll();
            if (handler == null) {
                return handledTasks;
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("registering handler " + handler);
            }
            SelectionKey key = handler.getChannel().keyFor(selector);
            if ((key != null) && key.isValid()) {
                key.cancel();
            }
            handledTasks++;
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public Set<AbstractChannelBasedEndpoint> getRegistered() {
        Set<AbstractChannelBasedEndpoint> registered = new HashSet<AbstractChannelBasedEndpoint>();
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            AbstractChannelBasedEndpoint handler = (AbstractChannelBasedEndpoint) key.attachment();
            registered.add(handler);
        }
        return registered;
    }

    private void closeDispatcher() {
        LOG.fine("closing connections");
        if (selector != null) {
            try {
                selector.close();
            } catch (Exception e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("error occured by close selector within tearDown " + DataConverter.toString(e));
                }
            }
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void close() throws IOException {
        if (selector != null) {
            Set<AbstractChannelBasedEndpoint> registered = getRegistered();
            for (AbstractChannelBasedEndpoint endpoint : registered) {
                endpoint.onDispatcherClose();
            }
            new Thread(new Closer(registered.size())).start();
        }
    }

    /**
	 * returns if the dispatcher is open
	 *
	 * @return true, if the dispatcher is open
	 */
    public boolean isOpen() {
        return isOpen;
    }

    /**
	 * statistic method which returns the number handled registrations
	 * @return the number handled registrations
	 */
    public long getNumberOfHandledRegistrations() {
        return handledRegistractions;
    }

    /**
	 * statistic method which returns the number of handled reads
	 *
	 * @return the number of handled reads
	 */
    public long getNumberOfHandledReads() {
        return handledReads;
    }

    /**
	 * statistic method which returns the number of handled writes
	 *
	 * @return the number of handled writes
	 */
    public long getNumberOfHandledWrites() {
        return handledWrites;
    }

    @Override
    public String toString() {
        return "open channels  " + getRegistered().size();
    }

    private class KeyUpdateTask implements Runnable {

        private AbstractChannelBasedEndpoint handler = null;

        private SelectionKey key = null;

        public KeyUpdateTask(AbstractChannelBasedEndpoint handler) {
            this.handler = handler;
        }

        void init() {
            key = handler.getChannel().keyFor(selector);
        }

        AbstractChannelBasedEndpoint getHandler() {
            return handler;
        }

        SelectionKey getKey() {
            return key;
        }

        public void run() {
        }
    }

    private class Closer implements Runnable {

        private int openConnections = 0;

        public Closer(int openConnections) {
            this.openConnections = openConnections;
        }

        public void run() {
            Thread.currentThread().setName("xDispatcherCloser");
            long start = System.currentTimeMillis();
            int terminatedConnections = 0;
            do {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                if (System.currentTimeMillis() > (start + TIMEOUT_SHUTDOWN_MILLIS)) {
                    LOG.warning("shutdown timeout reached (" + DataConverter.toFormatedDuration(TIMEOUT_SHUTDOWN_MILLIS) + "). kill pending connections");
                    for (SelectionKey sk : selector.keys()) {
                        try {
                            terminatedConnections++;
                            sk.channel().close();
                        } catch (IOException ioe) {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("error occured by closing " + ioe.toString());
                            }
                        }
                    }
                    break;
                }
            } while (getRegistered().size() > 0);
            isOpen = false;
            selector.wakeup();
            if (((openConnections > 0) || (terminatedConnections > 0)) && ((openConnections > 0) && (terminatedConnections > 0))) {
                LOG.info((openConnections - terminatedConnections) + " connections has been closed properly, " + terminatedConnections + " connections has been terminate unclean");
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("dispatcher " + this.hashCode() + " has been closed (shutdown time = " + DataConverter.toFormatedDuration(System.currentTimeMillis() - start) + ")");
            }
        }
    }
}
