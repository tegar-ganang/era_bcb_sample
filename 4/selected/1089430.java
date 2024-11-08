package com.ractoc.pffj.distributed.shared;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;

/**
 * Class responsible for reading message from the client. This class runs as a
 * Callable inside the main Connector ExecuterService. After creating the class,
 * a ReaderListener needs to be added.
 * 
 * @author Schrijver
 * @version 0.1
 */
public class Reader implements Callable<Integer> {

    private static Logger logger = Logger.getLogger(Reader.class.getName());

    private static final int BUFFER_SIZE = 1024;

    private static final int QUEUE_SIZE = 1000;

    private static final int READ_EVENT_WAIT_INTERVAL = 10000;

    private final Selector selector;

    private int port;

    private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    private ReaderListener readerListener;

    private Queue<RegisterableClient> registerableClients = new ArrayBlockingQueue<RegisterableClient>(QUEUE_SIZE);

    private Queue<RegisterableClient> unregisterableClients = new ArrayBlockingQueue<RegisterableClient>(QUEUE_SIZE);

    /**
     * Constructor, also opens the Selector.
     * 
     * @throws IOException
     *             Opening the selector can throw an IOException, this is passed
     *             on to the Connector.
     */
    public Reader() throws IOException {
        selector = Selector.open();
    }

    /**
     * Add a ReaderListener to the Reader. This listener is needed to be able to
     * tell the Connector that a message has been received.
     * 
     * @param readerListenerParam
     *            Listener to set.
     */
    public final void setReaderListener(final ReaderListener readerListenerParam) {
        this.readerListener = readerListenerParam;
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
     * @throws IOException
     *             Something went wrong with the registration.
     */
    public final void create(final String clientId, final SocketChannel sChannel) throws IOException {
        registerableClients.offer(new RegisterableClient(clientId, sChannel));
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
        selector.wakeup();
    }

    /**
     * Does the actual reading of the messages as well as the registering and
     * unregistering of the connections with the selector.
     * 
     * {@inheritDoc}
     */
    @Override
    public final Integer call() throws ReaderException {
        SelectionKey selKey = null;
        try {
            while (true) {
                selector.select(READ_EVENT_WAIT_INTERVAL);
                registerClients();
                unregisterClients();
                final Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    selKey = it.next();
                    it.remove();
                    if (selKey.isReadable()) {
                        Message message = null;
                        try {
                            message = readMessage(selKey);
                        } catch (final ReaderException e) {
                            logger.warn("Unable to read message.", e);
                        }
                        if (message == null) {
                            readerListener.connectionClosed((String) selKey.attachment(), (SocketChannel) selKey.channel());
                            continue;
                        }
                        readerListener.messageRecevied(message);
                    }
                }
            }
        } catch (final IOException e) {
            throw new ReaderException(e);
        }
    }

    private Message readMessage(final SelectionKey selKey) throws ReaderException {
        Message message = null;
        try {
            SocketChannel sChannel = null;
            String clientId = null;
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] ba = null;
            int bytesRead = 0;
            sChannel = (SocketChannel) selKey.channel();
            clientId = (String) selKey.attachment();
            logger.debug("message received for clientId " + clientId);
            for (; ; ) {
                buffer.clear();
                bytesRead = sChannel.read(buffer);
                if (bytesRead > 0) {
                    buffer.flip();
                    ba = new byte[bytesRead];
                    buffer.get(ba, 0, bytesRead);
                    baos.write(ba);
                } else if (bytesRead < 0) {
                    message = null;
                    break;
                } else if (bytesRead == 0) {
                    break;
                }
            }
            message = new Message();
            message.fromBytes(baos.toByteArray());
            if (message.getPluginMessage() != null) {
                logger.debug("message: " + message.getPluginMessage());
                message.setClientId(clientId);
            } else if (message.getPluginMessageResult() != null) {
                logger.debug("result: " + message.getPluginMessageResult());
            }
        } catch (final MessageException e) {
            throw new ReaderException(e);
        } catch (final IOException e) {
            throw new ReaderException(e);
        }
        return message;
    }

    private void unregisterClients() throws IOException {
        for (RegisterableClient client : unregisterableClients) {
            logger.debug("unregistering client " + client.getClientId() + " for reading");
            unregisterableClients.remove(client);
            client.getChannel().keyFor(selector).cancel();
            client.getChannel().close();
            logger.debug("unregistered client " + client.getClientId() + " for reading");
            selector.selectNow();
        }
    }

    private void registerClients() throws ClosedChannelException {
        SelectionKey selKey = null;
        for (RegisterableClient client : registerableClients) {
            logger.debug("registering client " + client.getClientId() + " for reading");
            registerableClients.remove(client);
            selKey = client.getChannel().register(selector, SelectionKey.OP_READ);
            selKey.attach(client.getClientId());
            logger.debug("registered client " + client.getClientId() + " for reading");
        }
    }

    /**
     * Data container used with the registerableClients and
     * unregisterableClients Queues.
     * 
     * @author Schrijver
     * @version 0.1
     */
    private final class RegisterableClient {

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
