package org.sepp.connections;

import iaik.utils.Util;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sepp.api.components.SecureSelfOrganizingNetwork;
import org.sepp.database.Database;
import org.sepp.messages.common.CloseConnectionInfo;
import org.sepp.messages.common.Message;
import org.sepp.messages.common.OpenConnectionInfo;
import org.sepp.utils.Constants;
import org.sepp.utils.XMLTags;

public class ChannelListener implements Runnable {

    private final ByteBuffer buffer = ByteBuffer.allocate(1024);

    private StringBuffer stringBuffer = new StringBuffer();

    private Log log;

    private boolean threadSuspended = true;

    private ServerSocketChannel ssc;

    private ServerSocket serverSocket;

    private Selector selector;

    private ConnectionManager connectionManager;

    private MessageProcessor messageProcessor;

    public ChannelListener(ConnectionManager connectionManager, MessageProcessor messageProcessor) {
        this.connectionManager = connectionManager;
        this.messageProcessor = messageProcessor;
        log = LogFactory.getLog(this.getClass());
        try {
            selector = Selector.open();
        } catch (IOException e) {
            log.debug("Couldn't create channel listener. Reason: " + e.getMessage());
        }
    }

    /**
	 * Performs the actual listening process on the currently registered
	 * channels. This process is halted until either a connection request to a
	 * registered {@link ServerSocketChannel} has been made or data is available
	 * on a {@link SocketChannel}.
	 */
    public void run() {
        try {
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            serverSocket = ssc.socket();
            InetSocketAddress isa = new InetSocketAddress(Constants.STANDARD_PORT);
            serverSocket.bind(isa);
            Selector selector = Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            log.info("Connection listener started!");
            while (true) {
                if (threadSuspended) {
                    log.debug("Waiting for thread to be resumed.");
                    synchronized (this) {
                        wait();
                    }
                } else {
                    log.debug("Waiting for channel selections.");
                    int num = selector.select();
                    if (num == 0) {
                        continue;
                    }
                    log.debug("Channel selections occured.");
                    Set keys = selector.selectedKeys();
                    Iterator it = keys.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = (SelectionKey) it.next();
                        if (key.isAcceptable()) {
                            SocketChannel sc = serverSocket.accept().getChannel();
                            sc.configureBlocking(false);
                            sc.register(selector, SelectionKey.OP_READ);
                            connectionManager.createConnection(sc);
                        } else if (key.isReadable()) {
                            receiveMessage((SocketChannel) key.channel());
                        }
                    }
                    keys.clear();
                }
            }
        } catch (IOException ioe) {
            log.error("Error during chanmel operation. Reason: " + ioe.getMessage());
        } catch (InterruptedException ie) {
            log.error("Error in waiting for thread execution. Reason: " + ie.getMessage());
        }
    }

    /**
	 * Stops the listening process. The process can be started again by calling
	 * the {@link #start()} method.
	 */
    public void stop() {
        threadSuspended = true;
        selector.wakeup();
    }

    /**
	 * Starts the listening process or resumes it after it has been stopped.
	 */
    public void start() {
        threadSuspended = false;
        synchronized (this) {
            notify();
        }
    }

    /**
	 * This method is an internal method which actually obtains waiting data
	 * from the input stream and creates a {@link Message} object from it. If
	 * there are more than one messages available from the input stream all of
	 * them are created and stored in the internal receive buffer. From this
	 * buffer the {@link #receiveMessage()} method obtains one message a time.
	 * 
	 * TODO: Currently no handling for incomplete received messages has been
	 * implemented. For instance if the input stream contains only parts of the
	 * messages without the closing <code></SeppMessage></code> tag the received
	 * data isn't recognized as message and discarded even if the next time this
	 * method is called the missing data is contained in the input stream.
	 */
    private boolean receiveMessage(SocketChannel channel) {
        int index = 0;
        String[] messages = getMessages(channel);
        while (messages instanceof String[] && index < messages.length) {
            Message message = new Message(messages[index]);
            log.debug(messages[index]);
            DirectConnection connection = connectionManager.getConnection(channel);
            if (message.getType() == OpenConnectionInfo.type) {
                Database.database.storeMessageInfo(message, SecureSelfOrganizingNetwork.localPeerId, message.getSource(), System.currentTimeMillis(), false);
                OpenConnectionInfo info = new OpenConnectionInfo(message);
                connection.peerId = message.getSource();
                connection.remoteAddress = info.getListenAddress();
                connection.remotePort = info.getListenPort();
                connection.ready = true;
            } else if (message.getType() == CloseConnectionInfo.type) {
                Database.database.storeMessageInfo(message, SecureSelfOrganizingNetwork.localPeerId, message.getSource(), System.currentTimeMillis(), false);
                connection.closed = true;
                connection.ready = false;
            } else {
                message.setReceivedFrom(connection.getPeerId());
                messageProcessor.processMessage(message);
                if (!message.getSource().equalsIgnoreCase(connection.getPeerId())) connectionManager.setReady(message.getSource());
                Database.database.storeMessageInfo(message, SecureSelfOrganizingNetwork.localPeerId, message.getSource(), System.currentTimeMillis(), false);
                log.info(message.getTypeDescription() + " message from peer " + message.getSource() + " for peer " + message.getDestination() + " received");
            }
            index++;
        }
        return true;
    }

    /**
	 * Reads the data from the channel and creates an array of Strings each
	 * containing one received message. This is done in the case that more than
	 * one message is received at once. If one message isn't received completely
	 * the data is buffered and at the next time we receive data it is appended
	 * to form the complete message.
	 * 
	 * TODO: Currently no handling for incompletely received messages is
	 * provided. This could happen if some data is received but not all and we
	 * want to create a message and we always assume that the next received data
	 * belongs to the former not completely received.
	 * 
	 * @param channel
	 *            The channel from which data should be received from.
	 * @return An array of strings each containing a complete message.
	 */
    private String[] getMessages(SocketChannel channel) {
        try {
            int index = 0;
            buffer.clear();
            int dataLength = channel.read(buffer);
            buffer.flip();
            byte[] readBytes = new byte[dataLength];
            buffer.get(readBytes, 0, dataLength);
            stringBuffer.append(Util.toASCIIString(readBytes));
            boolean lastMessageComplete = stringBuffer.toString().endsWith("</" + XMLTags.MESSAGE + ">");
            String messages[] = stringBuffer.toString().split("</" + XMLTags.MESSAGE + ">");
            int count = 0;
            if (lastMessageComplete) count = messages.length; else count = messages.length - 1;
            for (; index < count; index++) {
                messages[index] = messages[index] + "</" + XMLTags.MESSAGE + ">";
            }
            stringBuffer.delete(0, stringBuffer.length());
            if (!lastMessageComplete) {
                stringBuffer.append(messages[messages.length - 1]);
            }
            return messages;
        } catch (Exception ioe) {
            log.debug("Reason: " + ioe.getMessage());
            connectionManager.removeConnection(channel);
            return null;
        }
    }

    /**
	 * Adds a channel on which it should also be listened for incoming
	 * connection requests and data to be read.
	 * 
	 * @param channel
	 *            The channel which should be added to this listener.
	 */
    public void addChannel(SocketChannel channel) {
        try {
            threadSuspended = true;
            selector.wakeup();
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            threadSuspended = false;
            synchronized (this) {
                notify();
            }
            log.debug("Added a new channel to this listener.");
        } catch (IOException e) {
            log.error("Couldn't register channel for reading.");
        }
    }
}
