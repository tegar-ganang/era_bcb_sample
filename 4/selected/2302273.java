package com.jme3.network.base;

import com.jme3.network.*;
import com.jme3.network.kernel.Endpoint;
import com.jme3.network.kernel.Kernel;
import com.jme3.network.message.ChannelInfoMessage;
import com.jme3.network.message.ClientRegistrationMessage;
import com.jme3.network.message.DisconnectMessage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  A default implementation of the Server interface that delegates
 *  its network connectivity to kernel.Kernel.
 *
 *  @version   $Revision: 9114 $
 *  @author    Paul Speed
 */
public class DefaultServer implements Server {

    static Logger log = Logger.getLogger(DefaultServer.class.getName());

    private static final int CH_RELIABLE = 0;

    private static final int CH_UNRELIABLE = 1;

    private static final int CH_FIRST = 2;

    private boolean isRunning = false;

    private AtomicInteger nextId = new AtomicInteger(0);

    private String gameName;

    private int version;

    private KernelFactory kernelFactory = KernelFactory.DEFAULT;

    private KernelAdapter reliableAdapter;

    private KernelAdapter fastAdapter;

    private List<KernelAdapter> channels = new ArrayList<KernelAdapter>();

    private List<Integer> alternatePorts = new ArrayList<Integer>();

    private Redispatch dispatcher = new Redispatch();

    private Map<Integer, HostedConnection> connections = new ConcurrentHashMap<Integer, HostedConnection>();

    private Map<Endpoint, HostedConnection> endpointConnections = new ConcurrentHashMap<Endpoint, HostedConnection>();

    private Map<Long, Connection> connecting = new ConcurrentHashMap<Long, Connection>();

    private MessageListenerRegistry<HostedConnection> messageListeners = new MessageListenerRegistry<HostedConnection>();

    private List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<ConnectionListener>();

    public DefaultServer(String gameName, int version, Kernel reliable, Kernel fast) {
        if (reliable == null) throw new IllegalArgumentException("Default server reqiures a reliable kernel instance.");
        this.gameName = gameName;
        this.version = version;
        reliableAdapter = new KernelAdapter(this, reliable, dispatcher, true);
        channels.add(reliableAdapter);
        if (fast != null) {
            fastAdapter = new KernelAdapter(this, fast, dispatcher, false);
            channels.add(fastAdapter);
        }
    }

    public String getGameName() {
        return gameName;
    }

    public int getVersion() {
        return version;
    }

    public int addChannel(int port) {
        if (isRunning) throw new IllegalStateException("Channels cannot be added once server is started.");
        if (channels.size() - CH_FIRST != alternatePorts.size()) throw new IllegalStateException("Channel and port lists do not match.");
        try {
            int result = alternatePorts.size();
            alternatePorts.add(port);
            Kernel kernel = kernelFactory.createKernel(result, port);
            channels.add(new KernelAdapter(this, kernel, dispatcher, true));
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Error adding channel for port:" + port, e);
        }
    }

    protected void checkChannel(int channel) {
        if (channel < 0 || channel >= alternatePorts.size()) throw new IllegalArgumentException("Channel is undefined:" + channel);
    }

    public void start() {
        if (isRunning) throw new IllegalStateException("Server is already started.");
        for (KernelAdapter ka : channels) {
            ka.initialize();
        }
        for (KernelAdapter ka : channels) {
            ka.start();
        }
        isRunning = true;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void close() {
        if (!isRunning) throw new IllegalStateException("Server is not started.");
        try {
            for (KernelAdapter ka : channels) {
                ka.close();
            }
            isRunning = false;
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while closing", e);
        }
    }

    public void broadcast(Message message) {
        broadcast(null, message);
    }

    public void broadcast(Filter<? super HostedConnection> filter, Message message) {
        if (connections.isEmpty()) return;
        ByteBuffer buffer = MessageProtocol.messageToBuffer(message, null);
        FilterAdapter adapter = filter == null ? null : new FilterAdapter(filter);
        if (message.isReliable() || fastAdapter == null) {
            reliableAdapter.broadcast(adapter, buffer, true, false);
        } else {
            fastAdapter.broadcast(adapter, buffer, false, false);
        }
    }

    public void broadcast(int channel, Filter<? super HostedConnection> filter, Message message) {
        if (connections.isEmpty()) return;
        checkChannel(channel);
        ByteBuffer buffer = MessageProtocol.messageToBuffer(message, null);
        FilterAdapter adapter = filter == null ? null : new FilterAdapter(filter);
        channels.get(channel + CH_FIRST).broadcast(adapter, buffer, true, false);
    }

    public HostedConnection getConnection(int id) {
        return connections.get(id);
    }

    public boolean hasConnections() {
        return !connections.isEmpty();
    }

    public Collection<HostedConnection> getConnections() {
        return Collections.unmodifiableCollection((Collection<HostedConnection>) connections.values());
    }

    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    public void addMessageListener(MessageListener<? super HostedConnection> listener) {
        messageListeners.addMessageListener(listener);
    }

    public void addMessageListener(MessageListener<? super HostedConnection> listener, Class... classes) {
        messageListeners.addMessageListener(listener, classes);
    }

    public void removeMessageListener(MessageListener<? super HostedConnection> listener) {
        messageListeners.removeMessageListener(listener);
    }

    public void removeMessageListener(MessageListener<? super HostedConnection> listener, Class... classes) {
        messageListeners.removeMessageListener(listener, classes);
    }

    protected void dispatch(HostedConnection source, Message m) {
        if (source == null) {
            messageListeners.messageReceived(source, m);
        } else {
            synchronized (source) {
                messageListeners.messageReceived(source, m);
            }
        }
    }

    protected void fireConnectionAdded(HostedConnection conn) {
        for (ConnectionListener l : connectionListeners) {
            l.connectionAdded(this, conn);
        }
    }

    protected void fireConnectionRemoved(HostedConnection conn) {
        for (ConnectionListener l : connectionListeners) {
            l.connectionRemoved(this, conn);
        }
    }

    protected int getChannel(KernelAdapter ka) {
        return channels.indexOf(ka);
    }

    protected void registerClient(KernelAdapter ka, Endpoint p, ClientRegistrationMessage m) {
        Connection addedConnection = null;
        synchronized (this) {
            long tempId = m.getId();
            Connection c = connecting.remove(tempId);
            if (c == null) {
                c = new Connection(channels.size());
                log.log(Level.FINE, "Registering client for endpoint, pass 1:{0}.", p);
            } else {
                log.log(Level.FINE, "Refining client registration for endpoint:{0}.", p);
            }
            int channel = getChannel(ka);
            c.setChannel(channel, p);
            log.log(Level.FINE, "Setting up channel:{0}", channel);
            if (channel == CH_RELIABLE) {
                if (!getGameName().equals(m.getGameName()) || getVersion() != m.getVersion()) {
                    log.log(Level.INFO, "Kicking client due to name/version mismatch:{0}.", c);
                    c.close("Server client mismatch, server:" + getGameName() + " v" + getVersion() + "  client:" + m.getGameName() + " v" + m.getVersion());
                    return;
                }
                if (!alternatePorts.isEmpty()) {
                    ChannelInfoMessage cim = new ChannelInfoMessage(m.getId(), alternatePorts);
                    c.send(cim);
                }
            }
            if (c.isComplete()) {
                if (connections.put(c.getId(), c) == null) {
                    for (Endpoint cp : c.channels) {
                        if (cp == null) continue;
                        endpointConnections.put(cp, c);
                    }
                    addedConnection = c;
                }
            } else {
                connecting.put(tempId, c);
            }
        }
        if (addedConnection != null) {
            log.log(Level.INFO, "Client registered:{0}.", addedConnection);
            m = new ClientRegistrationMessage();
            m.setId(addedConnection.getId());
            m.setReliable(true);
            addedConnection.send(m);
            fireConnectionAdded(addedConnection);
        }
    }

    protected HostedConnection getConnection(Endpoint endpoint) {
        return endpointConnections.get(endpoint);
    }

    protected void connectionClosed(Endpoint p) {
        if (p.isConnected()) {
            log.log(Level.INFO, "Connection closed:{0}.", p);
        } else {
            log.log(Level.FINE, "Connection closed:{0}.", p);
        }
        Connection removed = null;
        synchronized (this) {
            connecting.values().remove(p);
            removed = (Connection) endpointConnections.remove(p);
            if (removed != null) {
                connections.remove(removed.getId());
            }
            log.log(Level.FINE, "Connections size:{0}", connections.size());
            log.log(Level.FINE, "Endpoint mappings size:{0}", endpointConnections.size());
        }
        if (removed != null && !removed.closed) {
            log.log(Level.INFO, "Client closed:{0}.", removed);
            removed.closeConnection();
        }
    }

    protected class Connection implements HostedConnection {

        private int id;

        private boolean closed;

        private Endpoint[] channels;

        private int setChannelCount = 0;

        private Map<String, Object> sessionData = new ConcurrentHashMap<String, Object>();

        public Connection(int channelCount) {
            id = nextId.getAndIncrement();
            channels = new Endpoint[channelCount];
        }

        void setChannel(int channel, Endpoint p) {
            if (channels[channel] != null && channels[channel] != p) {
                throw new RuntimeException("Channel has already been set:" + channel + " = " + channels[channel] + ", cannot be set to:" + p);
            }
            channels[channel] = p;
            if (p != null) setChannelCount++;
        }

        boolean isComplete() {
            return setChannelCount == channels.length;
        }

        public Server getServer() {
            return DefaultServer.this;
        }

        public int getId() {
            return id;
        }

        public String getAddress() {
            return channels[CH_RELIABLE] == null ? null : channels[CH_RELIABLE].getAddress();
        }

        public void send(Message message) {
            ByteBuffer buffer = MessageProtocol.messageToBuffer(message, null);
            if (message.isReliable() || channels[CH_UNRELIABLE] == null) {
                channels[CH_RELIABLE].send(buffer);
            } else {
                channels[CH_UNRELIABLE].send(buffer);
            }
        }

        public void send(int channel, Message message) {
            checkChannel(channel);
            ByteBuffer buffer = MessageProtocol.messageToBuffer(message, null);
            channels[channel + CH_FIRST].send(buffer);
        }

        protected void closeConnection() {
            if (closed) return;
            closed = true;
            for (Endpoint p : channels) {
                if (p == null) continue;
                p.close();
            }
            fireConnectionRemoved(this);
        }

        public void close(String reason) {
            DisconnectMessage m = new DisconnectMessage();
            m.setType(DisconnectMessage.KICK);
            m.setReason(reason);
            m.setReliable(true);
            send(m);
            if (channels[CH_RELIABLE] != null) {
                channels[CH_RELIABLE].close(true);
            }
        }

        public Object setAttribute(String name, Object value) {
            if (value == null) return sessionData.remove(name);
            return sessionData.put(name, value);
        }

        @SuppressWarnings("unchecked")
        public <T> T getAttribute(String name) {
            return (T) sessionData.get(name);
        }

        public Set<String> attributeNames() {
            return Collections.unmodifiableSet(sessionData.keySet());
        }

        public String toString() {
            return "Connection[ id=" + id + ", reliable=" + channels[CH_RELIABLE] + ", fast=" + channels[CH_UNRELIABLE] + " ]";
        }
    }

    protected class Redispatch implements MessageListener<HostedConnection> {

        public void messageReceived(HostedConnection source, Message m) {
            dispatch(source, m);
        }
    }

    protected class FilterAdapter implements Filter<Endpoint> {

        private Filter<? super HostedConnection> delegate;

        public FilterAdapter(Filter<? super HostedConnection> delegate) {
            this.delegate = delegate;
        }

        public boolean apply(Endpoint input) {
            HostedConnection conn = getConnection(input);
            if (conn == null) return false;
            return delegate.apply(conn);
        }
    }
}
