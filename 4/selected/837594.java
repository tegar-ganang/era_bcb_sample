package com.ractoc.pffj.distributed.shared;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

/**
 * Class responsible for writing messages to the client. This class runs as a
 * Callable inside the main Connector ExecuterService. After creating the class,
 * a WriterListener needs to be added.
 * 
 * @author Schrijver
 * @version 0.1
 */
public class Writer implements Callable<Integer> {

    private static Logger logger = Logger.getLogger(Writer.class.getName());

    private static final int WAIT_FOR_NEW_MESSAGES_INTERVAL = 500;

    private static final int QUEUE_SIZE = 1000;

    private static final int WRITE_EVENT_WAIT_INTERVAL = 10000;

    private final Selector selector;

    private int port;

    private ConnectionClosedListener writerListener;

    private Queue<RegisterableClient> registerableClients = new ArrayBlockingQueue<RegisterableClient>(QUEUE_SIZE);

    private Queue<RegisterableClient> unregisterableClients = new ArrayBlockingQueue<RegisterableClient>(QUEUE_SIZE);

    private Map<String, Queue<Message>> clientMessages = new ConcurrentHashMap<String, Queue<Message>>();

    private Map<String, Long> keepAliveTimers = new HashMap<String, Long>();

    private int totalMessageCount;

    private Thread writerThread;

    /**
     * Constructor, also opens the Selector.
     * 
     * @throws IOException
     *             Opening the selector can throw an IOException, this is passed
     *             on to the Connector.
     */
    public Writer() throws IOException {
        selector = Selector.open();
    }

    /**
     * Add a ConnectionClosedListener to the Writer. This listener is needed to
     * be able to tell the Connector that the connection has been closed.
     * 
     * @param writerListenerParam
     *            Listener to set.
     */
    public final void setWriterListener(final ConnectionClosedListener writerListenerParam) {
        this.writerListener = writerListenerParam;
    }

    /**
     * Get the port that the Acceptor listens to.
     * 
     * @return The port.
     */
    public final int getPort() {
        return port;
    }

    /**
     * Registers the supplied SocketChannel.
     * 
     * @param clientId
     *            Client the SocketChannel belongs to.
     * @param sChannel
     *            Channel to register.
     */
    public final void create(final String clientId, final SocketChannel sChannel) {
        registerableClients.offer(new RegisterableClient(clientId, sChannel));
        clientMessages.put(clientId, new ArrayBlockingQueue<Message>(QUEUE_SIZE));
        keepAliveTimers.put(clientId, 0L);
        if (writerThread != null) {
            writerThread.interrupt();
        }
        selector.wakeup();
    }

    /**
     * Close the connection. Adds the connection to the queue to be closed.
     * 
     * @param clientId
     *            Client the connection belongs to.
     * @param sChannel
     *            Actual connection to be closed.
     */
    public final void close(final String clientId, final SocketChannel sChannel) {
        unregisterableClients.offer(new RegisterableClient(clientId, sChannel));
        keepAliveTimers.remove(clientId);
        totalMessageCount -= clientMessages.get(clientId).size();
        clientMessages.remove(clientId);
        if (writerThread != null) {
            writerThread.interrupt();
        }
        selector.wakeup();
    }

    /**
     * Does the actual writing of the messages as well as the registering and
     * unregistering of the connections with the selector.
     * 
     * {@inheritDoc}
     */
    @Override
    public final Integer call() throws java.io.IOException {
        Iterator<SelectionKey> it = null;
        SelectionKey selKey = null;
        if (writerThread == null) {
            writerThread = Thread.currentThread();
        }
        while (true) {
            selector.select(WRITE_EVENT_WAIT_INTERVAL);
            registerClients();
            unregisterClients();
            it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                selKey = it.next();
                it.remove();
                if (selKey.isWritable()) {
                    try {
                        write(selKey);
                    } catch (final WriterException e) {
                        logger.warn("unable to write message", e);
                    }
                }
            }
            if (totalMessageCount == 0) {
                try {
                    Thread.sleep(WAIT_FOR_NEW_MESSAGES_INTERVAL);
                } catch (final InterruptedException e) {
                }
            }
        }
    }

    private void unregisterClients() throws IOException {
        for (RegisterableClient client : unregisterableClients) {
            logger.debug("unregistering client " + client.getClientId() + " for writing");
            unregisterableClients.remove(client);
            client.getChannel().keyFor(selector).cancel();
            client.getChannel().close();
            selector.selectNow();
        }
    }

    private void registerClients() throws IOException {
        SelectionKey selKey;
        for (RegisterableClient client : registerableClients) {
            logger.debug("registering client " + client.getClientId() + " for writing");
            registerableClients.remove(client);
            selKey = client.getChannel().register(selector, SelectionKey.OP_WRITE);
            selKey.attach(client.getClientId());
        }
    }

    /**
     * Write the supplied message to the supplied client. Adds the message to
     * the clientMessages Queue for the supplied client.
     * 
     * @param message
     *            ConnectorMessage that needs to be sent.
     * @return Was the message successfully added to the Queue?
     */
    public final boolean write(final Message message) {
        final boolean result = clientMessages.get(message.getClientId()).offer(message);
        totalMessageCount++;
        if (writerThread != null) {
            writerThread.interrupt();
        }
        return result;
    }

    private void write(final SelectionKey selKey) throws WriterException {
        final String clientId = (String) selKey.attachment();
        if (!clientMessages.get(clientId).isEmpty()) {
            final Message message = clientMessages.get(clientId).remove();
            writeMessageToClient((SocketChannel) selKey.channel(), message);
        }
    }

    private void writeMessageToClient(final SocketChannel sChannel, final Message message) throws WriterException {
        boolean connectionClosed = false;
        try {
            logger.debug("message to write: " + message.getPluginMessage());
            logger.debug("result to write: " + message.getPluginMessageResult());
            final ByteBuffer bbuf = ByteBuffer.wrap(message.getBytes());
            bbuf.flip();
            int bytesWritten = 0;
            for (; ; ) {
                bytesWritten = sChannel.write(bbuf);
                if (bytesWritten > 0 && !bbuf.hasRemaining()) {
                    break;
                }
                bbuf.compact();
            }
            logger.debug("message written");
        } catch (final MessageException e) {
            connectionClosed = true;
            throw new WriterException(e);
        } catch (final IOException e) {
            connectionClosed = true;
            throw new WriterException(e);
        } finally {
            if (connectionClosed) {
                logger.info("connection closed for clientId " + message.getClientId());
                writerListener.connectionClosed(message.getClientId(), sChannel);
            }
        }
        totalMessageCount--;
    }

    /**
     * Data container used with the registerableClients and
     * unregisterableClients Queues.
     * 
     * @author Schrijver
     * @version 0.1
     */
    private class RegisterableClient {

        private String clientId;

        private SocketChannel sChannel;

        public RegisterableClient(final String clientIdParam, final SocketChannel sChannelParam) {
            this.clientId = clientIdParam;
            this.sChannel = sChannelParam;
        }

        public String getClientId() {
            return clientId;
        }

        public SocketChannel getChannel() {
            return sChannel;
        }
    }
}
