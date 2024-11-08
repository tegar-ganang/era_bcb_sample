package org.jeuron.jlightning.connection.manager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import org.jeuron.jlightning.container.initiator.Initiator;
import org.jeuron.jlightning.container.listener.Listener;
import org.jeuron.jlightning.connection.Connection;
import org.jeuron.jlightning.connection.ConnectionException;
import org.jeuron.jlightning.connection.DefaultSocketConnection;
import org.jeuron.jlightning.connection.SocketConnection;
import org.jeuron.jlightning.connection.commander.Request;
import org.jeuron.jlightning.connection.commander.Response;
import org.jeuron.jlightning.connection.commander.Result;
import org.jeuron.jlightning.connection.protocol.Protocol;
import org.jeuron.jlightning.event.EventCatagory;
import org.jeuron.jlightning.event.system.SystemEventType;
import org.jeuron.jlightning.event.system.ConnectionEvent;
import org.jeuron.jlightning.event.system.ConnectionManagerEvent;
import org.jeuron.jlightning.event.system.InitiatorEvent;
import org.jeuron.jlightning.event.system.ListenerEvent;
import org.jeuron.jlightning.message.handler.MessageHandler;
import org.jeuron.jlightning.message.handler.MessageHandlerException;

/**
 *
 * @author Mike Karrys
 * @since 1.1
 */
public class StandardSocketConnectionManager extends AbstractConnectionManager {

    /**
     * Process a selector OP_READ.
     * @param key selectionKey to process
     */
    protected void doRead(SelectionKey key) throws ConnectionManagerException, InterruptedException {
        SocketConnection connection = (SocketConnection) key.attachment();
        SocketChannel socketChannel = (SocketChannel) connection.getChannel();
        try {
            if (connection != null) {
                if (connection.isTerminated()) {
                    container.sendEvent(new ConnectionEvent(EventCatagory.STATUS, SystemEventType.CLOSE, "closed", container.getName(), connection.getParent(), connection.getName()));
                    container.removeConnection(socketChannel.socket().getRemoteSocketAddress());
                    connection.close();
                    connection = null;
                    key.attach(null);
                } else {
                    connection.readBuffer();
                }
            } else {
                container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.GENERAL, "doRead()- connection is null.", container.getName()));
            }
        } catch (IOException ex) {
            throw new ConnectionManagerException(ex);
        }
    }

    /**
     * Process a selector OP_WRITE.
     * @param key selectionKey to process
     */
    protected void doWrite(SelectionKey key) throws ConnectionManagerException, InterruptedException {
        SocketConnection connection = (SocketConnection) key.attachment();
        SocketChannel socketChannel = (SocketChannel) connection.getChannel();
        try {
            if (connection != null) {
                if (connection.isTerminated()) {
                    container.sendEvent(new ConnectionEvent(EventCatagory.STATUS, SystemEventType.CLOSE, "closed", container.getName(), connection.getParent(), connection.getName()));
                    container.removeConnection(socketChannel.socket().getRemoteSocketAddress());
                    connection.close();
                    connection = null;
                    key.attach(null);
                } else {
                    connection.writeBuffer();
                }
            } else {
                container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.GENERAL, "doWrite()- connection is null.", container.getName()));
            }
        } catch (IOException ex) {
            throw new ConnectionManagerException(ex);
        }
    }

    /**
     * Stops the SocketListener from listening and closes all inbound connections.
     *
     */
    protected void doAccept(SelectionKey key) throws ConnectionManagerException {
        Listener listener = null;
        ServerSocketChannel ssc = null;
        SocketChannel socketChannel = null;
        SocketConnection connection = null;
        Protocol protocol = null;
        try {
            listener = (Listener) key.attachment();
            ssc = (ServerSocketChannel) key.channel();
            socketChannel = ssc.accept();
            socketChannel.configureBlocking(false);
            if (container != null && container.checkLogLevel(EventCatagory.INFO)) {
                System.out.println("doAccept - receiveBufferSize(" + socketChannel.socket().getReceiveBufferSize() + ")");
                System.out.println("doAccept - sendBufferSize(" + socketChannel.socket().getSendBufferSize() + ")");
            }
            connection = new DefaultSocketConnection();
            connection.setName(socketChannel.socket().getRemoteSocketAddress().toString());
            connection.setParent(listener.getName());
            connection.setChannel(socketChannel);
            connection.setContainer(container);
            connection.setConnectionManager(this);
            connection.setTimeoutInterval(listener.getTimeout());
            connection.setKeepaliveInterval(listener.getKeepalive());
            connection.setQueueSize(container.getQueueSize());
            MessageHandler messageHandler = messageHandlerFactory.getMessageHandler();
            messageHandler.setConnection(connection);
            messageHandler.setContainer(container);
            messageHandler.setQueueSize(container.getQueueSize());
            messageHandler.init();
            connection.setMessageHandler(messageHandler);
            protocol = protocolFactory.createProtocol();
            protocol.setContainer(container);
            protocol.setConnection(connection);
            protocol.setQueueSize(container.getQueueSize());
            protocol.init();
            connection.setProtocol(protocol);
            connection.setSocketBufferSize(listener.getBufferSize());
            connection.init();
            container.addConnection(socketChannel.socket().getRemoteSocketAddress(), connection);
            socketChannel.register(selector, SelectionKey.OP_READ, connection);
            container.sendEvent(new ConnectionManagerEvent(EventCatagory.STATUS, SystemEventType.ACCEPT, socketChannel.socket().getRemoteSocketAddress().toString(), container.getName()));
        } catch (MessageHandlerException ex) {
            container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName()));
        } catch (ConnectionException ex) {
            container.sendEvent(new ConnectionManagerEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName()));
        } catch (IOException ex) {
            throw new ConnectionManagerException(ex);
        }
    }

    /**
     * Open the serverSocketChannel and start accepting inbound connections.
     *
     */
    protected void doBind(String name) throws IOException {
        ServerSocketChannel ssc = null;
        SocketAddress localSocketAddress = null;
        Listener listener = container.getListener(name);
        localSocketAddress = new InetSocketAddress(listener.getBindAddress(), listener.getLocalPort());
        ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        if (listener.getBufferSize() != 0) {
            ssc.socket().setReceiveBufferSize(listener.getBufferSize());
        }
        ssc.socket().bind(localSocketAddress);
        ssc.register(selector, SelectionKey.OP_ACCEPT, listener);
        listener.setChannel(ssc);
        container.sendEvent(new ListenerEvent(EventCatagory.STATUS, SystemEventType.BIND, ssc.socket().getLocalSocketAddress().toString(), container.getName(), name));
    }

    /**
     * Returns with the SocketListener is listening
     *
     */
    protected boolean doIsBound(String name) {
        ServerSocketChannel ssc = null;
        Listener listener = container.getListener(name);
        ssc = (ServerSocketChannel) listener.getChannel();
        if (ssc != null) {
            if (!ssc.socket().isClosed()) {
                return ssc.socket().isBound();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Stops the SocketListener from listening and closes all inbound connections.
     */
    protected void doUnBind(String name, boolean closeAll) throws IOException {
        ServerSocketChannel ssc = null;
        Listener listener = null;
        SelectionKey selectionKey = null;
        Object o = null;
        listener = container.getListener(name);
        ssc = (ServerSocketChannel) listener.getChannel();
        selectionKey = ssc.keyFor(selector);
        if (selectionKey.attachment() instanceof Listener) {
            selectionKey.attach(null);
            if (ssc != null) {
                container.sendEvent(new ListenerEvent(EventCatagory.STATUS, SystemEventType.UNBIND, ssc.socket().getLocalSocketAddress().toString(), container.getName(), name));
                if (!ssc.isOpen()) {
                    ssc.close();
                }
            }
        } else {
            container.sendEvent(new ConnectionManagerEvent(EventCatagory.ERROR, SystemEventType.UNBIND, "Selection Key is not of type Listener", container.getName()));
        }
        if (closeAll) {
            doCloseAll(name);
        }
    }

    /**
     * loses all inbound connections for a Listener or Initiator.
     */
    protected void doCloseAll(String parent) throws IOException {
        Map<SocketAddress, Connection> connectionMap = null;
        SocketConnection connection = null;
        SocketChannel socketChannel = null;
        SelectionKey selectionKey = null;
        SocketAddress socketAddress = null;
        connectionMap = container.getConnectionMap();
        for (Map.Entry<SocketAddress, Connection> entry : connectionMap.entrySet()) {
            socketAddress = entry.getKey();
            connection = (SocketConnection) entry.getValue();
            if (connection.getParent().compareTo(parent) == 0) {
                container.removeConnection(socketAddress);
                socketChannel = (SocketChannel) connection.getChannel();
                selectionKey = socketChannel.keyFor(selector);
                if (selectionKey != null) {
                    selectionKey.attach(null);
                }
                if (!connection.isTerminated()) {
                    connection.close();
                }
            }
        }
    }

    /**
     * Initiates the connection to the server.
     *
     */
    protected void doConnect(String name) throws ConnectionManagerException {
        SocketChannel sc = null;
        SocketAddress remoteSocketAddress = null;
        Initiator initiator = container.getInitiator(name);
        try {
            sc = SocketChannel.open();
            sc.configureBlocking(false);
            remoteSocketAddress = new InetSocketAddress(initiator.getRemoteHost(), initiator.getRemotePort());
            sc.connect(remoteSocketAddress);
            sc.register(selector, SelectionKey.OP_CONNECT, initiator);
            if (initiator.getBufferSize() != 0) {
                sc.socket().setReceiveBufferSize(initiator.getBufferSize());
                sc.socket().setSendBufferSize(initiator.getBufferSize());
            } else if (sc.socket().getReceiveBufferSize() != sc.socket().getSendBufferSize()) {
                sc.socket().setSendBufferSize(sc.socket().getReceiveBufferSize());
            }
            if (container != null && container.checkLogLevel(EventCatagory.INFO)) {
                System.out.println("doConnect() - receiveBufferSize(" + sc.socket().getReceiveBufferSize() + ")");
                System.out.println("doConnect() - sendBufferSize(" + sc.socket().getSendBufferSize() + ")");
            }
        } catch (SocketException ex) {
            container.sendEvent(new InitiatorEvent(EventCatagory.WARN, SystemEventType.ACCEPT, ex.getLocalizedMessage(), container.getName(), initiator.getName()));
            throw new ConnectionManagerException(ex);
        } catch (IOException ex) {
            container.sendEvent(new InitiatorEvent(EventCatagory.WARN, SystemEventType.ACCEPT, ex.getLocalizedMessage(), container.getName(), initiator.getName()));
            throw new ConnectionManagerException(ex);
        }
        initiator.setChannel(sc);
    }

    /**
     * Disconnects the connection from the server.
     *
     */
    protected void doDisconnect(String name) throws ConnectionManagerException {
        SocketAddress socketAddress = null;
        SocketChannel sc = null;
        Initiator initiator = container.getInitiator(name);
        sc = (SocketChannel) initiator.getChannel();
        socketAddress = sc.socket().getLocalSocketAddress();
        SelectionKey key = sc.keyFor(selector);
        if (key != null && key.attachment() instanceof SocketConnection) {
            SocketConnection connection = (SocketConnection) key.attachment();
            try {
                if (connection != null) {
                    container.removeConnection(socketAddress);
                    container.sendEvent(new InitiatorEvent(EventCatagory.STATUS, SystemEventType.DISCONNECT, sc.socket().getLocalSocketAddress().toString(), container.getName(), name));
                    connection.close();
                    connection = null;
                    key.attach(null);
                }
            } catch (IOException ex) {
                throw new ConnectionManagerException(ex);
            }
        } else {
            container.sendEvent(new ConnectionManagerEvent(EventCatagory.ERROR, SystemEventType.DISCONNECT, "Selection Key is not of type Initiator", container.getName()));
        }
    }

    /**
     * Finish the connection process for socket connections.
     */
    protected void doFinishConnect(SelectionKey key) {
        SocketChannel sc = null;
        SocketConnection connection = null;
        Initiator initiator = null;
        try {
            initiator = (Initiator) key.attachment();
            sc = (SocketChannel) key.channel();
            sc.finishConnect();
            initiator.setChannel(sc);
            connection = new DefaultSocketConnection();
            connection.setChannel(sc);
            connection.setName(sc.socket().getLocalSocketAddress().toString());
            connection.setParent(initiator.getName());
            connection.setContainer(container);
            connection.setConnectionManager(this);
            connection.setTimeoutInterval(initiator.getTimeout());
            connection.setKeepaliveInterval(initiator.getKeepalive());
            connection.setQueueSize(container.getQueueSize());
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
            key.attach(connection);
            key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            container.addConnection(sc.socket().getLocalSocketAddress(), connection);
            container.sendEvent(new InitiatorEvent(EventCatagory.STATUS, SystemEventType.CONNECT, sc.socket().getRemoteSocketAddress().toString(), container.getName(), initiator.getName()));
        } catch (MessageHandlerException ex) {
            container.sendEvent(new InitiatorEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName(), initiator.getName()));
        } catch (IOException ex) {
            container.sendEvent(new InitiatorEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName(), initiator.getName()));
        } catch (ConnectionException ex) {
            container.sendEvent(new InitiatorEvent(EventCatagory.WARN, SystemEventType.EXCEPTION, ex.getLocalizedMessage(), container.getName(), initiator.getName()));
        }
    }

    /**
     * Returns the connection status of the connection.
     * @return boolean returns <b>true</b> if connected else <b>false</b>
     */
    protected boolean doIsConnected(String name) {
        SocketChannel sc = null;
        Initiator initiator = container.getInitiator(name);
        sc = (SocketChannel) initiator.getChannel();
        if (sc == null) {
            return false;
        } else {
            return sc.socket().isConnected();
        }
    }

    protected void doTerminate() {
        startTermination();
    }

    /**
     * Process selector key operations.
     * @param key {@link SelectionKey} key to operate on
     */
    @Override
    protected void processKey(SelectionKey key) throws ConnectionManagerException, InterruptedException {
        if (key.isValid() && key.isAcceptable()) {
            doAccept(key);
        }
        if (key.isValid() && key.isConnectable()) {
            doFinishConnect(key);
        }
        if (key.isValid() && (!key.isAcceptable() || key.isConnectable())) {
            doRead(key);
        }
        if (key.isValid() && (!key.isAcceptable() || key.isConnectable())) {
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
                    doUnBind(name, true);
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
