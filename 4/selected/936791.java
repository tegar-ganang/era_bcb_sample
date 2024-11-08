package ch.unifr.nio.framework;

import ch.unifr.nio.framework.transform.ChannelWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The central dispatcher thread in a reactor pattern that manages registered
 * ChannelHandlers.<br> The Dispatcher demultiplexes all I/O requests
 * originating from the registered SelectableChannels and calls the
 * ChannelHandlers, if necessary.<br> ChannelHandlers execution characteristics
 * can be fine-tuned by specifying an Executor.<br> Because of the locking
 * mechanisms in the NIO library all changes to interest ops are done here.
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public class Dispatcher extends Thread {

    private static final Logger LOGGER = Logger.getLogger(Dispatcher.class.getName());

    private final Selector selector;

    private final ScheduledExecutorService scheduledExecutorService;

    private final MyThreadFactory myThreadFactory;

    private Executor executor;

    /**
     * Creates a new Dispatcher
     *
     * @throws java.io.IOException when opening a Selector failes
     */
    public Dispatcher() throws IOException {
        super(Dispatcher.class.getName());
        setDaemon(true);
        selector = Selector.open();
        myThreadFactory = new MyThreadFactory();
        scheduledExecutorService = Executors.newScheduledThreadPool(1, myThreadFactory);
    }

    /**
     * sets the executor that runs the handlers
     *
     * @param executor the executor that runs the handlers
     */
    public synchronized void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void run() {
        synchronized (this) {
            if (executor == null) {
                executor = Executors.newCachedThreadPool(myThreadFactory);
            }
            notifyAll();
        }
        try {
            while (!isInterrupted()) {
                synchronized (this) {
                }
                int updatedKeys = selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "\n\t{0} keys updated\n\t{1} keys " + "in selector's selected key set", new Object[] { updatedKeys, selectedKeys.size() });
                }
                if (!selectedKeys.isEmpty()) {
                    for (SelectionKey key : selectedKeys) {
                        Object attachment = key.attachment();
                        if (attachment instanceof HandlerAdapter) {
                            HandlerAdapter adapter = (HandlerAdapter) attachment;
                            try {
                                adapter.cacheOps();
                                executor.execute(adapter);
                            } catch (CancelledKeyException ckException) {
                                LOGGER.log(Level.WARNING, null, ckException);
                            }
                        } else {
                            LOGGER.log(Level.WARNING, "attachment is no HandlerAdapter: {0}", attachment);
                        }
                    }
                }
                selectedKeys.clear();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
    }

    /**
     * registers a channel at the dispatcher with
     * {@link SelectionKey#OP_READ SelectionKey.OP_READ}
     *
     * @param channel the channel to register
     * @param channelHandler an ChannelHandler for this channel
     * @throws java.nio.channels.ClosedChannelException if the channel to
     * register is already closed
     */
    public void registerChannel(SelectableChannel channel, ChannelHandler channelHandler) throws ClosedChannelException {
        registerChannel(channel, channelHandler, SelectionKey.OP_READ);
    }

    /**
     * registers a channel at the dispatcher
     *
     * @param channel the channel to register
     * @param channelHandler an ChannelHandler for this channel
     * @param interestOps the interest ops to start with
     * @throws java.nio.channels.ClosedChannelException if the channel to
     * register is already closed
     */
    public synchronized void registerChannel(SelectableChannel channel, ChannelHandler channelHandler, int interestOps) throws ClosedChannelException {
        selector.wakeup();
        SelectionKey key = channel.register(selector, interestOps);
        HandlerAdapter handlerAdapter = new HandlerAdapter(this, channelHandler, key, channelHandler.toString());
        key.attach(handlerAdapter);
        channelHandler.getChannelReader().setChannel(channel);
        if (channel instanceof WritableByteChannel) {
            WritableByteChannel writableByteChannel = (WritableByteChannel) channel;
            ChannelWriter channelWriter = channelHandler.getChannelWriter();
            channelWriter.setChannel(writableByteChannel);
            channelWriter.setHandlerAdapter(handlerAdapter);
        }
        channelHandler.channelRegistered(handlerAdapter);
    }

    /**
     * Registers the given clientSocketChannelHandler for a non-blocking socket
     * connection operation.<p/>
     * This includes: <ul> <li>Resolve the given host name within the
     * Dispatcher's Executor threadpool.</li> <li>Create a SocketChannel and
     * initiate a non-blocking connect to the given host and port.</li> </ul>
     *
     * @param host the host name of the target system
     * @param port the port of the target system
     * @param clientSocketChannelHandler the clientSocketChannelHandler for this
     * socket connection
     */
    public synchronized void registerClientSocketChannelHandler(String host, int port, ClientSocketChannelHandler clientSocketChannelHandler) {
        registerClientSocketChannelHandler(host, port, clientSocketChannelHandler, 0);
    }

    /**
     * Registers the given clientSocketChannelHandler for a non-blocking socket
     * connection operation.<p/>
     * This includes: <ul> <li>Resolve the given host name within the
     * Dispatcher's Executor threadpool.</li> <li>Create a SocketChannel and
     * initiate a non-blocking connect to the given host and port.</li> </ul>
     *
     * @param host the host name of the target system
     * @param port the port of the target system
     * @param clientSocketChannelHandler the clientSocketChannelHandler for this
     * socket connection
     * @param timeout The timeout for the connection operation given in
     * milliseconds. If a connect operation does not succeed within the given
     * timeout, ClientSocketChannelHandler.connectFailed() is called.
     */
    public synchronized void registerClientSocketChannelHandler(String host, int port, ClientSocketChannelHandler clientSocketChannelHandler, int timeout) {
        while (executor == null) {
            try {
                wait();
            } catch (InterruptedException ex) {
                LOGGER.log(Level.WARNING, null, ex);
            }
        }
        executor.execute(new Resolver(host, port, clientSocketChannelHandler, timeout));
    }

    /**
     * cancels
     * <CODE>selectionKey</CODE>, removes its attachment and closes its channel
     *
     * @param selectionKey the selection key of the channel to unregister
     * @throws java.io.IOException if closing the channel fails
     */
    public synchronized void closeChannel(SelectionKey selectionKey) throws IOException {
        selector.wakeup();
        selectionKey.cancel();
        selectionKey.attach(null);
        selectionKey.channel().close();
    }

    /**
     * sets the interest ops of a selection key
     *
     * @param selectionKey the selection key of the channel
     * @param interestOps the interestOps to use when resuming the selection
     */
    public synchronized void setInterestOps(SelectionKey selectionKey, int interestOps) {
        selector.wakeup();
        if (selectionKey.isValid()) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "set interestOps to {0}", HandlerAdapter.interestToString(interestOps));
            }
            selectionKey.interestOps(interestOps);
        } else {
            LOGGER.log(Level.WARNING, "key is invalid");
        }
    }

    /**
     * removes interest ops from a SelectionKey
     *
     * @param selectionKey the SelectionKey
     * @param interestOps the interest ops to remove
     */
    public synchronized void removeInterestOps(SelectionKey selectionKey, int interestOps) {
        selector.wakeup();
        if (selectionKey.isValid()) {
            int newOps = selectionKey.interestOps() & ~interestOps;
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "set interestOps to {0}", HandlerAdapter.interestToString(newOps));
            }
            selectionKey.interestOps(newOps);
        } else {
            LOGGER.log(Level.WARNING, "key is invalid");
        }
    }

    /**
     * returns the interest ops of a SelectionKey without blocking
     *
     * @param selectionKey the selection key
     * @return the interest ops of the selection key
     */
    public synchronized int getInterestOps(SelectionKey selectionKey) {
        selector.wakeup();
        return selectionKey.interestOps();
    }

    /**
     * adds interest ops to a SelectionKey
     *
     * @param selectionKey the SelectionKey
     * @param interestOps the new interestOps
     */
    public synchronized void addInterestOps(SelectionKey selectionKey, int interestOps) {
        LOGGER.log(Level.FINEST, "waking up selector");
        selector.wakeup();
        if (selectionKey.isValid()) {
            int newOps = selectionKey.interestOps() | interestOps;
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "set interestOps to {0}", HandlerAdapter.interestToString(newOps));
            }
            selectionKey.interestOps(newOps);
        } else {
            LOGGER.log(Level.WARNING, "key is invalid");
        }
    }

    private class Resolver implements Runnable {

        private final String hostName;

        private final int port;

        private final ClientSocketChannelHandler clientSocketChannelHandler;

        private final int timeout;

        public Resolver(String hostName, int port, ClientSocketChannelHandler clientSocketChannelHandler, int timeout) {
            this.hostName = hostName;
            this.port = port;
            this.clientSocketChannelHandler = clientSocketChannelHandler;
            this.timeout = timeout;
        }

        @Override
        public void run() {
            InetSocketAddress address = new InetSocketAddress(hostName, port);
            if (address.isUnresolved()) {
                clientSocketChannelHandler.resolveFailed();
            } else {
                try {
                    SocketChannel socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(false);
                    boolean connected = socketChannel.connect(address);
                    if (connected) {
                        registerChannel(socketChannel, clientSocketChannelHandler, SelectionKey.OP_READ);
                        clientSocketChannelHandler.connectSucceeded();
                    } else {
                        registerChannel(socketChannel, clientSocketChannelHandler, SelectionKey.OP_CONNECT);
                        if (timeout > 0) {
                            TimeoutHandler timeoutHandler = new TimeoutHandler(socketChannel, clientSocketChannelHandler);
                            scheduledExecutorService.schedule(timeoutHandler, timeout, TimeUnit.MILLISECONDS);
                        }
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, null, ex);
                    clientSocketChannelHandler.connectFailed(ex);
                }
            }
        }
    }

    private static class TimeoutHandler implements Runnable {

        private final WeakReference<SocketChannel> socketChannelReference;

        private final WeakReference<ClientSocketChannelHandler> handlerReference;

        public TimeoutHandler(SocketChannel socketChannel, ClientSocketChannelHandler clientSocketChannelHandler) {
            socketChannelReference = new WeakReference<SocketChannel>(socketChannel);
            handlerReference = new WeakReference<ClientSocketChannelHandler>(clientSocketChannelHandler);
        }

        @Override
        public void run() {
            SocketChannel socketChannel = socketChannelReference.get();
            ClientSocketChannelHandler handler = handlerReference.get();
            if ((socketChannel != null) && (handler != null)) {
                synchronized (socketChannel) {
                    if (socketChannel.isConnectionPending()) {
                        try {
                            socketChannel.close();
                            handler.connectFailed(new ConnectException("Connection timeout"));
                        } catch (IOException ex) {
                            LOGGER.log(Level.WARNING, null, ex);
                        }
                    }
                }
            }
        }
    }

    /**
     * By implementing our own ThreadFactory we solve two problems:
     *
     * 1) our threads get a nice name prefix so that we find them when debugging
     * 2) our threads become daemon threads so that applications may gracefully
     * exit when using the NIO Framework
     */
    static class MyThreadFactory implements ThreadFactory {

        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);

        private final ThreadGroup group;

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private final String namePrefix;

        MyThreadFactory() {
            SecurityManager securityManager = System.getSecurityManager();
            group = securityManager == null ? Thread.currentThread().getThreadGroup() : securityManager.getThreadGroup();
            namePrefix = "NIO Framework-pool-" + POOL_NUMBER.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(group, runnable, namePrefix + threadNumber.getAndIncrement());
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            thread.setDaemon(true);
            return thread;
        }
    }
}
