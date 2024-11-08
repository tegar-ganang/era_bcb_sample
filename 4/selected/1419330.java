package org.jeuron.jlightning.container;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jeuron.jlightning.connection.Connection;
import org.jeuron.jlightning.connection.commander.Commander;
import org.jeuron.jlightning.connection.commander.Request;
import org.jeuron.jlightning.connection.commander.Command;
import org.jeuron.jlightning.connection.commander.Response;
import org.jeuron.jlightning.connection.commander.Result;
import org.jeuron.jlightning.connection.manager.ConnectionManager;
import org.jeuron.jlightning.connection.protocol.Protocol;
import org.jeuron.jlightning.connection.protocol.factory.ProtocolFactory;
import org.jeuron.jlightning.event.Event;
import org.jeuron.jlightning.event.EventCatagory;
import org.jeuron.jlightning.event.system.SystemEventType;
import org.jeuron.jlightning.event.handler.EventHandler;
import org.jeuron.jlightning.container.initiator.Initiator;
import org.jeuron.jlightning.container.listener.Listener;
import org.jeuron.jlightning.event.system.ContainerEvent;
import org.jeuron.jlightning.event.system.InitiatorEvent;
import org.jeuron.jlightning.event.system.ListenerEvent;
import org.jeuron.jlightning.message.handler.MessageHandler;
import org.jeuron.jlightning.message.handler.factory.MessageHandlerFactory;

/**
 * <p>Extended by {@link StandardContainer} this class implements
 * {@link Container} operations required for all sub-classes.
 *
 * @author Mike Karrys
 * @since 1.0
 * @see Container
 *
 */
abstract class AbstractContainer implements Container {

    protected boolean autostart = false;

    protected ClassLoader classLoader = null;

    protected EventHandler eventHandler = null;

    protected ConnectionManager connectionManager = null;

    protected Commander commander = null;

    protected boolean logEnabled = false;

    protected EventCatagory logLevel = EventCatagory.ERROR;

    protected String name = "default";

    protected int poolSize = 20;

    protected int queueSize = 50;

    protected MessageHandlerFactory messageHandlerFactory = null;

    protected ProtocolFactory protocolFactory = null;

    protected boolean initialized = false;

    protected boolean created = false;

    protected boolean destroyed = false;

    protected String transport = null;

    protected ExecutorService threadPool = null;

    protected Map<SocketAddress, Connection> connectionMap = new ConcurrentHashMap<SocketAddress, Connection>();

    protected Map<String, Listener> listenerMap = new HashMap<String, Listener>();

    protected Map<String, Initiator> initiatorMap = new HashMap<String, Initiator>();

    /**
     * Return true if the container is set to auto start
     * @return the autostart property
     */
    public boolean isAutostart() {
        return autostart;
    }

    /**
     * Sets the autostart property.
     * @param autostart set to true to autostart container
     */
    public void setAutostart(boolean autostart) {
        this.autostart = autostart;
    }

    /**
     * Return the commander registered with this container.
     * @return commander property
     */
    public Commander getCommander() {
        return commander;
    }

    /**
     * Sets the commander for this container.
     * @param commander the commander to set
     */
    public void setCommander(Commander commander) {
        this.commander = commander;
    }

    /**
     * Return the eventHandler for this container.
     * @return eventHandler property
     */
    public EventHandler getEventHandler() {
        return eventHandler;
    }

    /**
     * Sets the eventHandler for this container.
     * @param eventHandler the eventHandler to set
     */
    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    /**
     * Sends and event to the eventHandler for this container.
     * @param event the event to send
     */
    public void sendEvent(Event event) {
        if (eventHandler != null) {
            eventHandler.queueEvent(event);
        }
    }

    /**
     * Returns the connection map.
     * @return map
     */
    public Map getConnectionMap() {
        return this.connectionMap;
    }

    /**
     * Adds {@link Connection} to connection map.
     * @param name name of connection
     * @param connection connection instance
     */
    public void addConnection(SocketAddress socketAddress, Connection connection) {
        this.connectionMap.put(socketAddress, connection);
    }

    /**
     * Removes {@link Connection} from connection map.
     * @param name name of connection
     * @return connection
     */
    public Connection removeConnection(SocketAddress socketAddress) {
        return this.connectionMap.remove(socketAddress);
    }

    /**
     * Finds {@link Connection} in connection map.
     * @param name name of connection
     * @return connection or null if not found.
     */
    public Connection getConnection(SocketAddress socketAddress) {
        return this.connectionMap.get(socketAddress);
    }

    /**
     * Returns the initiator map.
     * @return map
     */
    public Map getInitiatorMap() {
        return this.initiatorMap;
    }

    /**
     * Adds {@link Initiator} to initiator map.
     * @param name name of initiator
     * @param initiator initiator instance
     */
    public void addInitiator(String name, Initiator initiator) {
        this.initiatorMap.put(name, initiator);
    }

    /**
     * Removes {@link Initiator} from initiator map.
     * @param name name of initiator
     * @return initiator
     */
    public Initiator removeInitiator(String name) {
        return this.initiatorMap.remove(name);
    }

    /**
     * Finds {@link Initiator} in initiator map.
     * @param name name of initiator
     * @return initiator or null if not found.
     */
    public Initiator getInitiator(String name) {
        return this.initiatorMap.get(name);
    }

    /**
     * Returns the listener map.
     * @return map
     */
    public Map getListenerMap() {
        return this.initiatorMap;
    }

    /**
     * Adds {@link Listener} to listener map.
     * @param name name of listener
     * @param listener listener instance
     */
    public void addListener(String name, Listener listener) {
        this.listenerMap.put(name, listener);
    }

    /**
     * Removes {@link Listener} from listener map.
     * @param name name of listener
     * @return listener
     */
    public Listener removeListener(String name) {
        return this.listenerMap.remove(name);
    }

    /**
     * Finds {@link Listener} in listener map.
     * @param name name of listener
     * @return listener or null if not found.
     */
    public Listener getListener(String name) {
        return this.listenerMap.get(name);
    }

    /**
     * Return logEnabled.
     * @return logEnabled returns logEnabled property
     */
    public boolean isLogEnabled() {
        return logEnabled;
    }

    /**
     * Sets the logEnabled property.
     * @param logEnabled logEnabled property
     */
    public void setLogEnabled(boolean logEnabled) {
        this.logEnabled = logEnabled;
    }

    /**
     * Return true if checkLogLevel less or equal to logLevel
     * @return boolean returns whether checkLogLevel is in effect
     */
    public boolean checkLogLevel(EventCatagory checkLogLevel) {
        if (checkLogLevel.ordinal() <= this.logLevel.ordinal()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Return logLevel.
     * @return logLevel returns logLevel property
     */
    public EventCatagory getLogLevel() {
        return logLevel;
    }

    /**
     * Sets the logLevel property.
     * @param logLevel logLevel property
     */
    public void setLogLevel(EventCatagory logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * Return container name.
     * @return name returns name property
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the container name property.
     * @param name name property
     */
    public void setName(String name) throws ContainerException {
        if (name.matches("[a-zA-Z_0-9]*")) {
            this.name = name;
        } else {
            throw new ContainerException("Container name [" + name + "] contains invalid characters.");
        }
    }

    /**
     * Return the thread pool size for this container.
     * @return returns thread poolSize size
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * Sets the thread pool size for this container.
     * @param poolSize poolSize size to set
     */
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    /**
     * Return the list queue size for this container.
     * @return returns list queueSize size
     */
    public int getQueueSize() {
        return queueSize;
    }

    /**
     * Sets the list queue size for this container.
     * @param queueSize queueSize size to set
     */
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    /**
     * Return the messageHandler factory set for this container.
     * @return messageHandler
     */
    public MessageHandlerFactory getMessageHandlerFactory() {
        return messageHandlerFactory;
    }

    /**
     * Sets the {@link MessageHandlerFactory} used to create a {@link MessageHandler}
     * which is used to handle inbound and outbound messages from the application.
     * @param messageHandlerFactory messageHandlerFactory to set
     */
    public void setMessageHandlerFactory(MessageHandlerFactory messageHandlerFactory) {
        this.messageHandlerFactory = messageHandlerFactory;
    }

    /**
     * Return the protocol factory set for this container.
     * @return protocolFactory
     */
    public ProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    /**
     * Sets the {@link ProtocolFactory} used to create a {@link Protocol}
     * which is used to process the inbound and outbound data buffers.
     * @param protocolFactory protocolFactory to set
     */
    public void setProtocolFactory(ProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    /**
     * Return true if the container is started.
     * @return started property
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Sets the started property.
     * @param started set to true when container started
     */
    protected void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * Return true if the connectionManager has been created.
     * @return started property
     */
    public boolean isCreated() {
        return created;
    }

    /**
     * Sets the started property.
     * @param started set to true when container started
     */
    protected void setCreated(boolean created) {
        this.created = created;
    }

    /**
     * Return the type of container, either "socket" or "datagram".
     * @return type returns type of container
     */
    public String getTransport() {
        return transport;
    }

    /**
     * Sets the type of container, either "socket" or "datagram".
     * @param type type to set
     */
    public void setTransport(String transport) throws ContainerException {
        if ((transport.compareTo(SOCKET) == 0) || (transport.compareTo(DATAGRAM) == 0)) {
            this.transport = transport;
        } else {
            throw new ContainerException("Container 'transport' must be 'socket' or 'datagram'.");
        }
    }

    /**
     * Return the {@link ExecutorService} thread pool object created by this
     * container.
     * @return threadPool returns threadPool
     */
    public ExecutorService getThreadPool() {
        return threadPool;
    }

    /**
     * Initialize the container.
     *
     */
    protected void startThreadPool() {
        threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Initialize the container.
     *
     */
    protected void stopThreadPool() {
        threadPool.shutdown();
        threadPool = null;
    }

    /**
     * Executes a Runnable object on a thread from the thread pool {@link ExecutorService}
     * @param runnable object to run
     */
    public void executeThread(Runnable runnable) {
        threadPool.execute(runnable);
    }

    /**
     * Returns the running status of the connectionManager.
     * @return boolean returns <b>true</b> if running else <b>false</b>
     */
    public boolean isRunning() throws ContainerException {
        if (connectionManager != null) {
            return connectionManager.isRunning();
        } else {
            return false;
        }
    }

    /**
     * The container <b>bind</b> operation sends a command to the {@link ConnectionManager}
     * to bind to the port and start accepting connections.
     * @return a {@link Response} object with the result of the <b>bind</b> operation
     */
    protected Response doBind(String name) throws ContainerException {
        Response response = null;
        sendEvent(new ListenerEvent(EventCatagory.DEBUG, SystemEventType.BIND, "Bind(START)", getName(), name));
        if (commander != null) {
            Request request = new Request();
            request.setCommand(Command.BIND);
            request.addParameter("name", name);
            response = commander.sendReceive(request);
        } else {
            throw new ContainerException("No Commander Defined.");
        }
        sendEvent(new ListenerEvent(EventCatagory.DEBUG, SystemEventType.BIND, "Bind(END)", getName(), name));
        return response;
    }

    /**
     * The container <b>unbind</b> operation sends a command to the {@link ConnectionManager}
     * to unbind from the port and stop accepting connections.
     * @return a {@link Response} object with the result of the <b>unbind</b> operation
     */
    protected Response doUnbind(String name) throws ContainerException {
        sendEvent(new ListenerEvent(EventCatagory.DEBUG, SystemEventType.UNBIND, "Unbind(START)", getName(), name));
        Response response = null;
        if (commander != null) {
            try {
                Request request = new Request();
                request.setCommand(Command.UNBIND);
                request.addParameter("name", name);
                sendEvent(new ListenerEvent(EventCatagory.DEBUG, SystemEventType.UNBIND, "Unbind(IN)", getName(), name));
                response = commander.sendReceive(request);
                sendEvent(new ListenerEvent(EventCatagory.DEBUG, SystemEventType.UNBIND, "Unbind(After sendReceive)", getName(), name));
            } catch (Exception e) {
                System.out.println("doUnbind:Exception:" + e.getMessage());
            }
        } else {
            throw new ContainerException("No Commander Defined.");
        }
        return response;
    }

    /**
     * The container <b>isBound</b> operation sends a command to the {@link ConnectionManager}
     * to check if it is currently bound to a port.
     * @return a {@link Response} object with the result of the <b>isBound</b> operation
     */
    protected Response doIsBound(String name) throws ContainerException {
        Response response = null;
        if (commander != null) {
            Request request = new Request();
            request.setCommand(Command.ISBOUND);
            request.addParameter("name", name);
            response = commander.sendReceive(request);
        } else {
            throw new ContainerException("No Commander Defined.");
        }
        return response;
    }

    /**
     * The container <b>connect</b> operation sends a command to the {@link ConnectionManager}
     * to connect to the host and port.
     * @return a {@link Response} object with the result of the <b>connect</b> operation
     */
    protected Response doConnect(String name) throws ContainerException {
        sendEvent(new InitiatorEvent(EventCatagory.INFO, SystemEventType.CONNECT, "Connect(START)", getName(), name));
        Response response = null;
        if (commander != null) {
            Request request = new Request();
            request.setCommand(Command.CONNECT);
            request.addParameter("name", name);
            response = commander.sendReceive(request);
        } else {
            throw new ContainerException("No Commander Defined.");
        }
        sendEvent(new InitiatorEvent(EventCatagory.INFO, SystemEventType.CONNECT, "Connect(END)", getName(), name));
        return response;
    }

    /**
     * The container <b>disconnect</b> operation sends a command to the {@link ConnectionManager}
     * to disconnect from the remote host.
     * @return a {@link Response} object with the result of the <b>disconnect</b> operation
     */
    protected Response doDisconnect(String name) throws ContainerException {
        sendEvent(new InitiatorEvent(EventCatagory.INFO, SystemEventType.DISCONNECT, "Disconnect(START)", getName(), name));
        Response response = null;
        if (commander != null) {
            Request request = new Request();
            request.setCommand(Command.DISCONNECT);
            request.addParameter("name", name);
            response = commander.sendReceive(request);
        } else {
            throw new ContainerException("No Commander Defined.");
        }
        sendEvent(new InitiatorEvent(EventCatagory.INFO, SystemEventType.DISCONNECT, "Disconnect(END)", getName(), name));
        return response;
    }

    /**
     * The container <b>isConnected</b> operation sends a command to the {@link ConnectionManager}
     * to check if it is currently connected.
     * @return a {@link Response} object with the result of the <b>isConnected</b> operation
     */
    protected Response doIsConnected(String name) throws ContainerException {
        Response response = null;
        if (commander != null) {
            Request request = new Request();
            request.setCommand(Command.ISCONNECTED);
            request.addParameter("name", name);
            response = commander.sendReceive(request);
        } else {
            throw new ContainerException("No Commander Defined.");
        }
        return response;
    }

    /**
     * The container <b>shutdown</b> operation sends a command to the {@link ConnectionManager}
     * to shutdown and terminate the connection manager.
     * @return a {@link Response} object with the result of the <b>shutdown</b> operation
     */
    protected Response doDestroy() throws ContainerException {
        Listener listener = null;
        Initiator initiator = null;
        Response response = null;
        for (Map.Entry<String, Listener> entry : listenerMap.entrySet()) {
            listener = entry.getValue();
            stop(listener.getName());
        }
        for (Map.Entry<String, Initiator> entry : initiatorMap.entrySet()) {
            initiator = entry.getValue();
            stop(initiator.getName());
        }
        if (commander != null) {
            Request request = new Request();
            request.setCommand(Command.TERMINATE);
            response = commander.sendReceive(request);
        } else {
            throw new ContainerException("No Commander Defined.");
        }
        return response;
    }

    /**
     * The method finishes the shutdown of the container.
     *
     */
    protected void finishShutdown() throws ContainerException {
        eventHandler.close();
        classLoader = null;
        connectionManager = null;
        commander = null;
        messageHandlerFactory = null;
        protocolFactory = null;
    }

    public Response autostart() throws ContainerException {
        String key = null;
        Listener listener = null;
        Initiator initiator = null;
        Response response = null;
        sendEvent(new ContainerEvent(EventCatagory.INFO, SystemEventType.GENERAL, "Autostart(START)", getName()));
        for (Map.Entry<String, Listener> entry : listenerMap.entrySet()) {
            key = entry.getKey();
            listener = entry.getValue();
            if (listener.isAutostart()) {
                response = doBind(listener.getName());
                if (response.getResult() == Result.EXCEPTION) {
                    throw new ContainerException("Autostart Exception:" + response.getException());
                }
            }
        }
        for (Map.Entry<String, Initiator> entry : initiatorMap.entrySet()) {
            key = entry.getKey();
            initiator = entry.getValue();
            if (initiator.isAutostart()) {
                response = doConnect(initiator.getName());
                if (response.getResult() == Result.EXCEPTION) {
                    throw new ContainerException("Autostart Exception:" + response.getException());
                }
            }
        }
        return response;
    }

    /**
     * Initializes container objects.
     *
     */
    public abstract void init() throws ContainerException;

    /**
     * UnInitializes container objects and terminates container.
     *
     */
    public abstract void shutdown() throws ContainerException;

    /**
     * Creates, initializes, and starts Connection Manager.
     *
     */
    public abstract void create() throws ContainerException;

    /**
     * Stops, unInitializes, and destroys Connections Manager object.
     *
     */
    public abstract void destroy() throws ContainerException;

    /**
     * Start initiators and listeners.
     *
     */
    public void start(String name) throws ContainerException {
        Listener listener = null;
        Initiator initiator = null;
        Response response = null;
        if (listenerMap.containsKey(name)) {
            listener = listenerMap.get(name);
            response = doBind(listener.getName());
        } else if (initiatorMap.containsKey(name)) {
            initiator = initiatorMap.get(name);
            response = doConnect(initiator.getName());
        } else {
            throw new ContainerException("EXCEPTION: Start failed because name[" + name + "] not found.");
        }
        if (response.getResult() == Result.EXCEPTION) {
            throw new ContainerException(response.getException());
        }
    }

    /**
     * Stop initiators and listeners.
     *
     */
    public void stop(String name) throws ContainerException {
        Listener listener = null;
        Initiator initiator = null;
        Response response = null;
        if (listenerMap.containsKey(name)) {
            listener = listenerMap.get(name);
            if (listener.getChannel() != null && listener.getChannel().isOpen()) {
                response = doUnbind(listener.getName());
            } else {
                response = new Response();
                response.setResult(Result.SUCCESS);
            }
        } else if (initiatorMap.containsKey(name)) {
            initiator = initiatorMap.get(name);
            if (initiator.getChannel() != null && initiator.getChannel().isOpen()) {
                response = doDisconnect(initiator.getName());
            } else {
                response = new Response();
                response.setResult(Result.SUCCESS);
            }
        } else {
            throw new ContainerException("EXCEPTION: Stop failed because name[" + name + "] not found.");
        }
        if (response.getResult() == Result.EXCEPTION) {
            throw new ContainerException(response.getException());
        }
    }

    /**
     * The container <b>isStarted</b> operation sends a command to the {@link ConnectionManager}
     * to check if it is currently connected.
     * @return a {@link Response} object with the result of the <b>isConnected</b> operation
     */
    public boolean isStarted(String name) throws ContainerException {
        Listener listener = null;
        Initiator initiator = null;
        Response response = null;
        boolean started = false;
        if (isCreated()) {
            if (listenerMap.containsKey(name)) {
                listener = listenerMap.get(name);
                if (listener.getChannel() != null && listener.getChannel().isOpen()) {
                    response = doIsBound(listener.getName());
                } else {
                    response = new Response();
                    response.setResult(Result.SUCCESS);
                }
            } else if (initiatorMap.containsKey(name)) {
                initiator = initiatorMap.get(name);
                if (initiator.getChannel() != null && initiator.getChannel().isOpen()) {
                    response = doIsConnected(initiator.getName());
                } else {
                    response = new Response();
                    response.setResult(Result.SUCCESS);
                }
            } else {
                throw new ContainerException("EXCEPTION: isStarted failed because name[" + name + "] not found.");
            }
            if (response.getResult() == Result.TRUE) {
                started = true;
            } else if (response.getResult() == Result.FALSE) {
                started = false;
            } else if (response.getResult() == Result.EXCEPTION) {
                throw new ContainerException(response.getException());
            }
        }
        return started;
    }

    /**
     * Return true if the connectionManager has been created.
     * @return started property
     */
    public boolean isListener(String name) {
        boolean valid = false;
        if (listenerMap.containsKey(name)) {
            valid = true;
        }
        return valid;
    }

    /**
     * Return true if the connectionManager has been created.
     * @return started property
     */
    public boolean isInitiator(String name) {
        boolean valid = false;
        if (initiatorMap.containsKey(name)) {
            valid = true;
        }
        return valid;
    }

    /**
     * Return true if the connectionManager has been created.
     * @return started property
     */
    public boolean isValidName(String name) {
        boolean valid = false;
        if (listenerMap.containsKey(name)) {
            valid = true;
        } else if (initiatorMap.containsKey(name)) {
            valid = true;
        }
        return valid;
    }

    /**
     * Return {@link List} initiator or listener names.
     * @return list list of initiator or listener names
     */
    public List<String> list(String type) throws ContainerException {
        String key = null;
        List<String> list = new ArrayList<String>();
        sendEvent(new ContainerEvent(EventCatagory.INFO, SystemEventType.GENERAL, "List(START)", getName()));
        if (type.compareTo(Container.LISTENERS) == 0) {
            for (Map.Entry<String, Listener> entry : listenerMap.entrySet()) {
                key = entry.getKey();
                list.add(key);
            }
        } else if (type.compareTo(Container.INITIATORS) == 0) {
            for (Map.Entry<String, Initiator> entry : initiatorMap.entrySet()) {
                key = entry.getKey();
                list.add(key);
            }
        } else {
            throw new ContainerException("Parameter 'type' must be either 'listeners' or 'initiators'.");
        }
        return list;
    }
}
