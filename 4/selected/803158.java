package org.jeuron.jlightning.connection;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import org.jeuron.jlightning.connection.protocol.ProtocolException;
import org.jeuron.jlightning.event.EventCatagory;
import org.jeuron.jlightning.event.system.SystemEventType;
import org.jeuron.jlightning.event.system.ConnectionEvent;
import org.jeuron.jlightning.event.system.ProtocolEvent;
import org.jeuron.jlightning.message.handler.AbstractRunnableMessageHandler;
import org.jeuron.jlightning.message.handler.MessageHandler;

/**
 * Implements basic {@link SocketConnection} operations for {@link Socket} based NIO
 * connections. 
 *
 * @author Mike Karrys
 * @since 1.0
 * @see Connection
 * @see AbstractConnection
 */
public class DefaultSocketConnection extends AbstractConnection implements SocketConnection {

    private SocketChannel socketChannel = null;

    private ByteBuffer readBuffer = null;

    private ByteBuffer writeBuffer = null;

    /**
     * Get the associated {@link Channel} for the this socket connection.
     * @return channel
     */
    public Channel getChannel() {
        return socketChannel;
    }

    /**
     * Sets the associated {@link Channel} for the this socket connection.
     * @param channel
     */
    @Override
    public void setChannel(Channel channel) throws ConnectionException {
        if (channel instanceof SocketChannel) {
            this.socketChannel = (SocketChannel) channel;
        } else {
            throw new ConnectionException("Channel must by of type SocketChannel.");
        }
    }

    /**
     * Closes socket associated with this channel.
     * 
     */
    @Override
    public void doCloseChannel() throws ConnectionException {
        if (socketChannel != null) {
            try {
                socketChannel.socket().close();
                socketChannel.close();
                socketChannel = null;
            } catch (IOException ex) {
                throw new ConnectionException(ex);
            }
        }
    }

    /**
     * Queues a message to change the interestOps for this channel.
     * @param interestOps
     */
    @Override
    public void queueInterestOpsForChannel(int interestOps) throws InterruptedException {
        connectionManager.queueSysOpsChangeForChannel(socketChannel, interestOps);
    }

    /**
     * Initializes the socket connection by setting the send and receive buffer sizes
     * and executing the associated {@link AbstractRunnableMessageHandler},
     * will execute the {@link MessageHandler}.
     */
    @Override
    public void init() {
        try {
            if (socketBufferSize > 10) {
                socketChannel.socket().setReceiveBufferSize(socketBufferSize);
                socketChannel.socket().setSendBufferSize(socketBufferSize);
            }
            readBuffer = ByteBuffer.allocate(socketChannel.socket().getReceiveBufferSize());
            writeBuffer = ByteBuffer.allocate(socketChannel.socket().getSendBufferSize());
            writeBuffer.limit(0);
            if (container.checkLogLevel(EventCatagory.TRACE)) {
                System.out.println("init() - ReceiveBufferSize(" + socketChannel.socket().getReceiveBufferSize() + ")");
                System.out.println("init() - SendBufferSize(" + socketChannel.socket().getSendBufferSize() + ")");
                System.out.println("init() - readBuffer(" + readBuffer.capacity() + ")");
                System.out.println("init() - writeBuffer(" + writeBuffer.capacity() + ")");
            }
            if (timeout > 0) {
                scheduleTimeout();
            }
            if (messageHandler != null && (messageHandler instanceof Runnable)) {
                container.executeThread((AbstractRunnableMessageHandler) messageHandler);
            }
        } catch (SocketException ex) {
            container.sendEvent(new ConnectionEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName(), getParent(), getName()));
        }
    }

    /**
     * Reads a buffer from the socket connection.
     */
    @Override
    public void readBuffer() throws InterruptedException {
        int bytesRead = 0;
        boolean readMore = false;
        try {
            if (!isTerminated()) {
                if (!messageHandler.isInBoundMessageQueueFull()) {
                    if (!protocol.hasMoreToRead(readBuffer)) {
                        protocol.clearBuffer(readBuffer);
                        bytesRead = socketChannel.read(readBuffer);
                        readBuffer.flip();
                    }
                    if (bytesRead > 0 || protocol.hasMoreToRead(readBuffer)) {
                        if (container != null && container.checkLogLevel(EventCatagory.TRACE)) {
                            System.out.println("readBuffer() - bytesRead(" + bytesRead + ") readBuffer(" + readBuffer + "):");
                            byte[] a = readBuffer.array();
                            for (int i = 0; i < a.length; i++) {
                                System.out.printf("%2X", a[i]);
                            }
                            System.out.println(" ");
                        }
                        protocol.processInBoundBuffer(readBuffer);
                    }
                }
                scheduleTimeout();
                scheduleKeepalive();
                queueInterestOpsForChannel(SelectionKey.OP_READ);
                if (protocol.hasMoreToWrite() || messageHandler.isInBoundMessageQueueFull()) {
                    queueInterestOpsForChannel(SelectionKey.OP_WRITE);
                }
                if (bytesRead == -1) {
                    setTerminated(true);
                    queueInterestOpsForChannel(SelectionKey.OP_WRITE);
                }
            }
        } catch (ProtocolException ex) {
            container.sendEvent(new ProtocolEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName()));
        } catch (IOException ex) {
            container.sendEvent(new ConnectionEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName(), getParent(), getName()));
            setTerminated(true);
            queueInterestOpsForChannel(SelectionKey.OP_WRITE);
        }
    }

    /**
     * Writes a buffer to the socket connection.
     */
    @Override
    public void writeBuffer() throws InterruptedException {
        int written = 0;
        if (!isTerminated()) {
            try {
                if (!writeBuffer.hasRemaining()) {
                    protocol.clearBuffer(writeBuffer);
                    if (protocol.hasMoreToWrite()) {
                        if (protocol.outBoundMessageHasRemaining()) {
                            protocol.fillNextOutBoundBuffer(writeBuffer);
                        } else if (protocol.isOutBoundMessageWaiting()) {
                            protocol.loadNextOutBoundMessage();
                            protocol.fillNextOutBoundBuffer(writeBuffer);
                        } else {
                            protocol.fillNextOutBoundBuffer(writeBuffer);
                        }
                    }
                    writeBuffer.flip();
                }
                if (writeBuffer.hasRemaining()) {
                    written = socketChannel.write(writeBuffer);
                    if (container != null && container.checkLogLevel(EventCatagory.TRACE)) {
                        System.out.println("writeBuffer() Buffer Trace(" + written + ")");
                        byte[] a = writeBuffer.array();
                        for (int i = 0; i < a.length; i++) {
                            System.out.printf("%2X", a[i]);
                        }
                        System.out.println(" ");
                    }
                }
                queueInterestOpsForChannel(SelectionKey.OP_READ);
                if (protocol.hasMoreToWrite() || messageHandler.isInBoundMessageQueueFull()) {
                    queueInterestOpsForChannel(SelectionKey.OP_WRITE);
                }
            } catch (ProtocolException ex) {
                container.sendEvent(new ConnectionEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName(), getParent(), getName()));
            } catch (IOException ioe) {
                container.sendEvent(new ConnectionEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ioe.getLocalizedMessage(), container.getName(), getParent(), getName()));
                setTerminated(true);
                queueInterestOpsForChannel(SelectionKey.OP_WRITE);
            }
        }
    }
}
