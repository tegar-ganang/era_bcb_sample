package org.jeuron.jlightning.connection.manager;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.jeuron.jlightning.connection.Connection;
import org.jeuron.jlightning.connection.ConnectionException;
import org.jeuron.jlightning.connection.DatagramConnection;
import org.jeuron.jlightning.connection.DefaultDatagramConnection;
import org.jeuron.jlightning.connection.commander.Request;
import org.jeuron.jlightning.connection.commander.Response;
import org.jeuron.jlightning.connection.commander.Result;
import org.jeuron.jlightning.connection.protocol.Protocol;
import org.jeuron.jlightning.event.EventCatagory;
import org.jeuron.jlightning.event.system.SystemEventType;
import org.jeuron.jlightning.container.initiator.Initiator;
import org.jeuron.jlightning.container.listener.Listener;
import org.jeuron.jlightning.event.system.ConnectionEvent;
import org.jeuron.jlightning.event.system.ConnectionManagerEvent;
import org.jeuron.jlightning.event.system.InitiatorEvent;
import org.jeuron.jlightning.event.system.ListenerEvent;
import org.jeuron.jlightning.message.ExceptionMessage;
import org.jeuron.jlightning.message.Message;
import org.jeuron.jlightning.message.handler.MessageHandler;
import org.jeuron.jlightning.message.handler.MessageHandlerException;

/**
 *
 * @author Mike Karrys
 * @since 1.1
 */
public class StandardDatagramConnectionManager extends AbstractConnectionManager implements DatagramConnectionManager {

    protected BlockingQueue<SocketAddress> connectionWriteQueue = new LinkedBlockingQueue<SocketAddress>();

    protected ByteBuffer readBuffer = null;

    protected ByteBuffer writeBuffer = null;

    /**
     * Returns <b>true</b> if the connectionWriteQueue is not empty.
     * @return boolean
     */
    public boolean isConnectionWriteWaiting() {
        return !connectionWriteQueue.isEmpty();
    }

    /**
     * Queues a {@link SocketAddress} on the connectionWriteQueue.
     * @param socketAddress socketAddress to put on queue
     */
    public void queueConnectionWrite(DatagramChannel dc, SocketAddress socketAddress) throws InterruptedException {
        if (!connectionWriteQueue.contains(socketAddress)) {
            connectionWriteQueue.put(socketAddress);
        }
        queueSysOpsChangeForChannel(dc, SelectionKey.OP_WRITE);
    }

    /**
     * Gets next {@link SocketAddress} from connetionWriteQueue.
     * @return socketAddress
     */
    public SocketAddress getConnectionWrite() throws InterruptedException {
        return (SocketAddress) connectionWriteQueue.take();
    }

    /**
     * Gets next {@link SocketAddress} from connetionWriteQueue.
     * @return socketAddress
     */
    public void removeConnectionWrite(SocketAddress socketAddress) throws InterruptedException {
        if (connectionWriteQueue.contains(socketAddress)) {
            connectionWriteQueue.remove(socketAddress);
        }
    }

    /**
     * Creates a new {@link DefaultDatagramConnection} object.
     * @param key current selection key
     * @param socketAddress socketAddress of new connection
     */
    protected DatagramConnection doCreateDatagramConnection(SelectionKey key, SocketAddress socketAddress) {
        DatagramChannel dc = null;
        DatagramConnection connection = null;
        Listener listener = null;
        Initiator initiator = null;
        String name = null;
        dc = (DatagramChannel) key.channel();
        Object o = key.attachment();
        if (o instanceof Listener) {
            listener = (Listener) o;
            name = listener.getName();
        } else if (o instanceof Initiator) {
            initiator = (Initiator) o;
            name = initiator.getName();
        }
        try {
            connection = new DefaultDatagramConnection();
            connection.setChannel(dc);
            connection.setName(socketAddress.toString());
            connection.setContainer(container);
            connection.setConnectionManager(this);
            connection.setRemoteSocketAddress(socketAddress);
            if (o instanceof Listener) {
                connection.setTimeoutInterval(listener.getTimeout());
                connection.setKeepaliveInterval(listener.getKeepalive());
                connection.setParent(listener.getName());
                connection.setSocketBufferSize(listener.getBufferSize());
            } else if (o instanceof Initiator) {
                connection.setTimeoutInterval(initiator.getTimeout());
                connection.setKeepaliveInterval(initiator.getKeepalive());
                connection.setParent(initiator.getName());
                connection.setSocketBufferSize(initiator.getBufferSize());
            }
            MessageHandler messageHandler = messageHandlerFactory.getMessageHandler();
            messageHandler.setConnection(connection);
            messageHandler.setContainer(container);
            messageHandler.setQueueSize(container.getQueueSize());
            messageHandler.init();
            connection.setMessageHandler(messageHandler);
            Protocol protocol = protocolFactory.createProtocol();
            protocol.setContainer(container);
            protocol.setConnection(connection);
            protocol.setQueueSize(container.getQueueSize());
            protocol.init();
            connection.setProtocol(protocol);
            connection.init();
            container.addConnection(socketAddress, connection);
            container.sendEvent(new ConnectionEvent(EventCatagory.STATUS, SystemEventType.CONNECT, "created", container.getName(), name, socketAddress.toString()));
        } catch (MessageHandlerException ex) {
            container.sendEvent(new ConnectionEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName(), null, name));
        } catch (ConnectionException ex) {
            container.sendEvent(new ConnectionEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName(), null, name));
        }
        return connection;
    }

    /**
     * Process a selector OP_READ.
     */
    protected void doRead(SelectionKey key) throws ConnectionManagerException {
        DatagramChannel dc = null;
        DatagramConnection connection = null;
        SocketAddress inBoundRemoteSocketAddress = null;
        dc = (DatagramChannel) key.channel();
        try {
            readBuffer.clear();
            inBoundRemoteSocketAddress = dc.receive(readBuffer);
            if (inBoundRemoteSocketAddress != null) {
                connection = (DatagramConnection) container.getConnection(inBoundRemoteSocketAddress);
                if (connection == null) {
                    connection = doCreateDatagramConnection(key, inBoundRemoteSocketAddress);
                }
            }
            readBuffer.flip();
            connection.processReadBuffer(readBuffer);
            if (connection.isTerminated()) {
                container.removeConnection(connection.getRemoteSocketAddress());
                removeConnectionWrite(connection.getRemoteSocketAddress());
                connection.close();
            }
        } catch (IOException ex) {
            try {
                Message exceptionMessge = new ExceptionMessage(new ConnectionManagerException(ex));
                connection.queueInBoundMessage(exceptionMessge);
            } catch (IOException ioe) {
                container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ioe.getLocalizedMessage(), container.getName()));
            } catch (InterruptedException ie) {
                container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ie.getLocalizedMessage(), container.getName()));
            }
            throw new ConnectionManagerException(ex);
        } catch (InterruptedException ex) {
            container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName()));
        }
    }

    /**
     * Process a selector OP_WRITE.
     */
    protected void doWrite(SelectionKey key) throws ConnectionManagerException {
        DatagramChannel dc = null;
        SocketAddress outBoundRemoteSocketAddress = null;
        DatagramConnection connection = null;
        dc = (DatagramChannel) key.channel();
        if (isConnectionWriteWaiting()) {
            try {
                outBoundRemoteSocketAddress = getConnectionWrite();
                connection = (DatagramConnection) container.getConnection(outBoundRemoteSocketAddress);
                connection.fillWriteBuffer(writeBuffer);
                writeBuffer.flip();
                dc.send(writeBuffer, outBoundRemoteSocketAddress);
                writeBuffer.clear();
                if (connection.isTerminated()) {
                    try {
                        container.removeConnection(connection.getRemoteSocketAddress());
                        removeConnectionWrite(connection.getRemoteSocketAddress());
                        connection.close();
                    } catch (InterruptedException ex) {
                        container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName()));
                    }
                }
            } catch (IOException ex) {
                container.sendEvent(new ConnectionManagerEvent(EventCatagory.INFO, SystemEventType.GENERAL, "doWrite() In IOException()", container.getName()));
                throw new ConnectionManagerException(ex);
            } catch (InterruptedException ex) {
                container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName()));
            }
        }
    }

    protected void doAccept(SelectionKey key) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Opens channel and binds socket to localSocketAddress.
     *
     */
    protected void doBind(String name) throws IOException {
        DatagramChannel dc = null;
        DatagramSocket datagramSocket = null;
        SocketAddress localSocketAddress = null;
        Listener listener = container.getListener(name);
        try {
            dc = DatagramChannel.open();
            dc.configureBlocking(false);
            datagramSocket = dc.socket();
            localSocketAddress = new InetSocketAddress(listener.getBindAddress(), listener.getLocalPort());
            datagramSocket.bind(localSocketAddress);
            dc.register(selector, SelectionKey.OP_READ, listener);
            if (listener.getBufferSize() > 7) {
                datagramSocket.setReceiveBufferSize(listener.getBufferSize());
                datagramSocket.setSendBufferSize(listener.getBufferSize());
            }
            readBuffer = ByteBuffer.allocate(datagramSocket.getReceiveBufferSize());
            writeBuffer = ByteBuffer.allocate(datagramSocket.getSendBufferSize());
            container.sendEvent(new ConnectionEvent(EventCatagory.STATUS, SystemEventType.BIND, "bind", container.getName(), null, name));
            container.sendEvent(new ConnectionEvent(EventCatagory.INFO, SystemEventType.GENERAL, "init() - listener.getBufferSize(" + listener.getBufferSize() + ")\n" + "init() - ReceiveBufferSize(" + datagramSocket.getReceiveBufferSize() + ")\n" + "init() - SendBufferSize(" + datagramSocket.getSendBufferSize() + ")\n" + "init() - readBuffer(" + readBuffer.capacity() + ")\n" + "init() - writeBuffer(" + writeBuffer.capacity() + ")", container.getName(), null, name));
            listener.setChannel(dc);
        } catch (IOException ex) {
            container.sendEvent(new ConnectionEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName(), null, name));
        }
    }

    /**
     * Returns bind status of socket.
     *
     */
    protected boolean doIsBound(String name) {
        DatagramChannel dc = null;
        Listener listener = container.getListener(name);
        dc = (DatagramChannel) listener.getChannel();
        if (dc != null) {
            return dc.socket().isBound();
        } else {
            return false;
        }
    }

    /**
     * Disconnects the connection from the server.
     *
     */
    protected void doUnbind(String name, boolean closeAll) throws IOException {
        DatagramChannel dc = null;
        Listener listener = null;
        SelectionKey selectionKey = null;
        listener = container.getListener(name);
        dc = (DatagramChannel) listener.getChannel();
        if (dc != null) {
            selectionKey = dc.keyFor(selector);
            if (selectionKey.attachment() instanceof Listener) {
                selectionKey.attach(null);
                container.sendEvent(new ListenerEvent(EventCatagory.STATUS, SystemEventType.UNBIND, "unbind", container.getName(), name));
                if (dc != null) {
                    dc.socket().close();
                }
            } else {
                container.sendEvent(new ConnectionManagerEvent(EventCatagory.ERROR, SystemEventType.UNBIND, "Selection Key is not of type Listener", container.getName()));
            }
            if (closeAll) {
                doCloseAll(name);
            }
        } else {
            container.sendEvent(new ListenerEvent(EventCatagory.ERROR, SystemEventType.EXCEPTION, "Listener Channel is null", container.getName(), name));
        }
    }

    /**
     * loses all inbound connections for a Listener or Initiator.
     */
    protected void doCloseAll(String parent) throws IOException {
        Map<SocketAddress, Connection> connectionMap = null;
        DatagramConnection connection = null;
        DatagramChannel datagramChannel = null;
        SocketAddress socketAddress = null;
        connectionWriteQueue.clear();
        connectionMap = container.getConnectionMap();
        for (Map.Entry<SocketAddress, Connection> entry : connectionMap.entrySet()) {
            socketAddress = entry.getKey();
            connection = (DatagramConnection) entry.getValue();
            if (connection.getParent().compareTo(parent) == 0) {
                container.removeConnection(socketAddress);
                if (!connection.isTerminated()) {
                    connection.close();
                }
            }
        }
        connectionMap.clear();
        connectionMap = null;
    }

    /**
     * Initiates the connection to the server.
     *
     */
    protected void doConnect(String name) throws ConnectionManagerException {
        SelectionKey key = null;
        DatagramChannel dc = null;
        DatagramSocket datagramSocket = null;
        SocketAddress localSocketAddress = null;
        SocketAddress remoteSocketAddress = null;
        Initiator initiator = container.getInitiator(name);
        try {
            dc = DatagramChannel.open();
            dc.configureBlocking(false);
            datagramSocket = dc.socket();
            localSocketAddress = new InetSocketAddress(initiator.getBindAddress(), initiator.getLocalPort());
            remoteSocketAddress = new InetSocketAddress(initiator.getRemoteHost(), initiator.getRemotePort());
            dc.socket().bind(localSocketAddress);
        } catch (IOException ex) {
            container.sendEvent(new InitiatorEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName(), name));
            throw new ConnectionManagerException(ex);
        }
        try {
            key = dc.register(selector, SelectionKey.OP_WRITE, initiator);
            container.sendEvent(new InitiatorEvent(EventCatagory.STATUS, SystemEventType.CONNECT, remoteSocketAddress.toString(), container.getName(), name));
            if (initiator.getBufferSize() > 7) {
                datagramSocket.setReceiveBufferSize(initiator.getBufferSize());
                datagramSocket.setSendBufferSize(initiator.getBufferSize());
            }
            readBuffer = ByteBuffer.allocate(dc.socket().getReceiveBufferSize());
            writeBuffer = ByteBuffer.allocate(dc.socket().getSendBufferSize());
            container.sendEvent(new InitiatorEvent(EventCatagory.INFO, SystemEventType.GENERAL, "init() - initiator.getBufferSize(" + initiator.getBufferSize() + ")\n" + "init() - ReceiveBufferSize(" + datagramSocket.getReceiveBufferSize() + ")\n" + "init() - SendBufferSize(" + datagramSocket.getSendBufferSize() + ")\n" + "init() - readBuffer(" + readBuffer.capacity() + ")\n" + "init() - writeBuffer(" + writeBuffer.capacity() + ")", container.getName(), name));
            doCreateDatagramConnection(key, remoteSocketAddress);
            queueConnectionWrite(dc, remoteSocketAddress);
            initiator.setChannel(dc);
        } catch (IOException ex) {
            container.sendEvent(new InitiatorEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName(), name));
            throw new ConnectionManagerException(ex);
        } catch (InterruptedException ex) {
        }
    }

    /**
     * Disconnects the connection from the server.
     *
     */
    protected void doDisconnect(String name) throws ConnectionManagerException {
        DatagramChannel dc = null;
        Initiator initiator = container.getInitiator(name);
        dc = (DatagramChannel) initiator.getChannel();
        try {
            if (dc != null) {
                dc.socket().close();
                dc.close();
                dc = null;
            }
            doCloseAll(name);
        } catch (IOException ex) {
            throw new ConnectionManagerException(ex);
        }
    }

    /**
     * Returns the connection status of the connection.
     * @return boolean returns <b>true</b> if connected else <b>false</b>
     */
    protected boolean doIsConnected(String name) {
        DatagramChannel dc = null;
        Initiator initiator = container.getInitiator(name);
        dc = (DatagramChannel) initiator.getChannel();
        if (dc == null) {
            return false;
        } else {
            return dc.socket().isConnected();
        }
    }

    protected void doTerminate() {
        connectionWriteQueue = null;
        startTermination();
    }

    /**
     * Process selector key operations.
     * @param key {@link SelectionKey} key to operate on
     */
    protected void processKey(SelectionKey key) throws ConnectionManagerException {
        if (key.isValid() && key.isReadable()) {
            doRead(key);
        }
        if (key.isValid() && key.isWritable()) {
            doWrite(key);
        }
    }

    /**
     * Executes a set of operations to control the connectionManager.
     * @param request a {@link Request} that contains the operation to perform
     */
    @Override
    public void execute(Request request) {
        Response response = new Response();
        String name = (String) request.getParameter("name");
        try {
            switch(request.getCommand()) {
                case BIND:
                    doBind(name);
                    response.setResult(Result.SUCCESS);
                    break;
                case UNBIND:
                    doUnbind(name, true);
                    response.setResult(Result.SUCCESS);
                    break;
                case ISBOUND:
                    if (doIsBound(name)) {
                        response.setResult(Result.TRUE);
                    } else {
                        response.setResult(Result.FALSE);
                    }
                    break;
                case CONNECT:
                    doConnect(name);
                    response.setResult(Result.SUCCESS);
                    break;
                case DISCONNECT:
                    doDisconnect(name);
                    response.setResult(Result.SUCCESS);
                    break;
                case ISCONNECTED:
                    if (doIsConnected(name)) {
                        response.setResult(Result.TRUE);
                    } else {
                        response.setResult(Result.FALSE);
                    }
                    break;
                case ISRUNNING:
                    if (isRunning()) {
                        response.setResult(Result.TRUE);
                    } else {
                        response.setResult(Result.FALSE);
                    }
                    break;
                case TERMINATE:
                    doTerminate();
                    response.setResult(Result.SUCCESS);
                    break;
            }
            commander.queueResponse(response);
        } catch (IOException ex) {
            response.setResult(Result.EXCEPTION);
            response.setException(ex);
            commander.queueResponse(response);
            container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName()));
        } catch (ConnectionManagerException ex) {
            response.setResult(Result.EXCEPTION);
            response.setException(ex);
            commander.queueResponse(response);
            container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName()));
        }
    }
}
