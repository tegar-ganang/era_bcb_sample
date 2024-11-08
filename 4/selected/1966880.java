package org.mortbay.cometd;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletContext;
import org.mortbay.util.DateCache;
import org.mortbay.util.ajax.JSON;
import dojox.cometd.Bayeux;
import dojox.cometd.Channel;
import dojox.cometd.Client;
import dojox.cometd.DataFilter;

/**
 * @author gregw
 * @author aabeling: added JSONP transport
 * 
 */
public abstract class AbstractBayeux implements Bayeux {

    public static final String META = "/meta/";

    public static final String META_CONNECT = "/meta/connect";

    public static final String META_DISCONNECT = "/meta/disconnect";

    public static final String META_HANDSHAKE = "/meta/handshake";

    public static final String META_PING = "/meta/ping";

    public static final String META_RECONNECT = "/meta/reconnect";

    public static final String META_STATUS = "/meta/status";

    public static final String META_SUBSCRIBE = "/meta/subscribe";

    public static final String META_UNSUBSCRIBE = "/meta/unsubscribe";

    public static final String CLIENT_FIELD = "clientId";

    public static final String DATA_FIELD = "data";

    public static final String CHANNEL_FIELD = "channel";

    public static final String ID_FIELD = "id";

    public static final String TIMESTAMP_FIELD = "timestamp";

    public static final String TRANSPORT_FIELD = "transport";

    public static final String ADVICE_FIELD = "advice";

    public static final String SUCCESSFUL_FIELD = "successful";

    public static final String SUBSCRIPTION_FIELD = "subscription";

    public static final String EXT_FIELD = "ext";

    public static final ChannelId META_CONNECT_ID = new ChannelId(META_CONNECT);

    public static final ChannelId META_DISCONNECT_ID = new ChannelId(META_DISCONNECT);

    public static final ChannelId META_HANDSHAKE_ID = new ChannelId(META_HANDSHAKE);

    public static final ChannelId META_PING_ID = new ChannelId(META_PING);

    public static final ChannelId META_RECONNECT_ID = new ChannelId(META_RECONNECT);

    public static final ChannelId META_STATUS_ID = new ChannelId(META_STATUS);

    public static final ChannelId META_SUBSCRIBE_ID = new ChannelId(META_SUBSCRIBE);

    public static final ChannelId META_UNSUBSCRIBE_ID = new ChannelId(META_UNSUBSCRIBE);

    public static final JSON.Literal TRANSPORTS = new JSON.Literal("[\"long-polling\",\"callback-polling\"]");

    private static final JSON.Literal EXT_JSON_COMMENTED = new JSON.Literal("{\"json-comment-filtered\":true}");

    private static HashMap<String, Class> _transports = new HashMap<String, Class>();

    private static final JSON.Literal __NO_ADVICE = new JSON.Literal("{}");

    private static final JSON.Literal __MULTI_FRAME_ADVICE = new JSON.Literal("{\"reconnect\":\"retry\",\"interval\":1500,\"multiple-clients\":true}");

    static {
        _transports.put("long-polling", JSONTransport.class);
        _transports.put("callback-polling", JSONPTransport.class);
    }

    HashMap<String, Handler> _handlers = new HashMap<String, Handler>();

    ChannelImpl _root = new ChannelImpl("/", this);

    ConcurrentHashMap<String, ClientImpl> _clients = new ConcurrentHashMap<String, ClientImpl>();

    SecurityPolicy _securityPolicy = new DefaultPolicy();

    Object _advice = new JSON.Literal("{\"reconnect\":\"retry\",\"interval\":0}");

    Object _unknownAdvice = new JSON.Literal("{\"reconnect\":\"handshake\",\"interval\":500}");

    int _logLevel;

    long _clientTimeoutMs = 60000;

    boolean _JSONCommented;

    boolean _initialized;

    ConcurrentHashMap<String, Set<String>> _browserPolls = new ConcurrentHashMap<String, Set<String>>();

    transient ServletContext _context;

    transient Random _random;

    transient DateCache _dateCache;

    transient ConcurrentHashMap<String, ChannelId> _channelIdCache;

    private boolean _alwaysResumePoll;

    /**
     * @param context.
     *            The logLevel init parameter is used to set the logging to
     *            0=none, 1=info, 2=debug
     */
    protected AbstractBayeux() {
        _handlers.put("*", new PublishHandler());
        _handlers.put(META_HANDSHAKE, new HandshakeHandler());
        _handlers.put(META_CONNECT, new ConnectHandler());
        _handlers.put(META_RECONNECT, new ReconnectHandler());
        _handlers.put(META_DISCONNECT, new DisconnectHandler());
        _handlers.put(META_SUBSCRIBE, new SubscribeHandler());
        _handlers.put(META_UNSUBSCRIBE, new UnsubscribeHandler());
        _handlers.put(META_STATUS, new StatusHandler());
        _handlers.put(META_PING, new PingHandler());
    }

    /**
     * @param channels
     *            A {@link ChannelId}
     * @param filter
     *            The filter instance to apply to new channels matching the
     *            pattern
     */
    public void addFilter(String channels, DataFilter filter) {
        synchronized (this) {
            ChannelImpl channel = (ChannelImpl) getChannel(channels, true);
            channel.addDataFilter(filter);
        }
    }

    public void removeFilter(String channels, DataFilter filter) {
        synchronized (this) {
            ChannelImpl channel = (ChannelImpl) getChannel(channels, false);
            if (channel != null) channel.removeDataFilter(filter);
        }
    }

    /**
     * @param id
     * @return
     */
    public ChannelImpl getChannel(ChannelId id) {
        return _root.getChild(id);
    }

    public ChannelImpl getChannel(String id) {
        ChannelId cid = getChannelId(id);
        if (cid.depth() == 0) return null;
        return _root.getChild(cid);
    }

    public Channel getChannel(String id, boolean create) {
        synchronized (this) {
            ChannelImpl channel = getChannel(id);
            if (channel == null && create) {
                channel = new ChannelImpl(id, this);
                _root.addChild(channel);
                if (isLogInfo()) logInfo("newChannel: " + channel);
            }
            return channel;
        }
    }

    public ChannelId getChannelId(String id) {
        ChannelId cid = _channelIdCache.get(id);
        if (cid == null) {
            cid = new ChannelId(id);
            _channelIdCache.put(id, cid);
        }
        return cid;
    }

    public Client getClient(String client_id) {
        synchronized (this) {
            if (client_id == null) return null;
            Client client = _clients.get(client_id);
            return client;
        }
    }

    public Set getClientIDs() {
        return _clients.keySet();
    }

    public long getClientTimeoutMs() {
        return _clientTimeoutMs;
    }

    /**
     * @return the logLevel. 0=none, 1=info, 2=debug
     */
    public int getLogLevel() {
        return _logLevel;
    }

    public SecurityPolicy getSecurityPolicy() {
        return _securityPolicy;
    }

    /** Handle a Bayeux message.
     * This is normally only called by the bayeux servlet or a test harness.
     * @param client The client if known
     * @param transport The transport to use for the message
     * @param message The bayeux message.
     */
    public String handle(ClientImpl client, Transport transport, Map<String, Object> message) throws IOException {
        final String METHOD = "handle: ";
        String channel_id = (String) message.get(CHANNEL_FIELD);
        Handler handler = (Handler) _handlers.get(channel_id);
        if (handler == null) handler = (Handler) _handlers.get("*");
        if (isLogDebug()) logDebug(METHOD + "handler=" + handler);
        handler.handle(client, transport, message);
        return channel_id;
    }

    public boolean hasChannel(String id) {
        ChannelId cid = getChannelId(id);
        return _root.getChild(cid) != null;
    }

    public boolean isInitialized() {
        return _initialized;
    }

    /**
     * @return the commented
     */
    public boolean isJSONCommented() {
        return _JSONCommented;
    }

    public boolean isLogDebug() {
        return _logLevel > 1;
    }

    public boolean isLogInfo() {
        return _logLevel > 0;
    }

    public void logDebug(String message) {
        if (_logLevel > 1) _context.log(message);
    }

    public void logDebug(String message, Throwable th) {
        if (_logLevel > 1) _context.log(message, th);
    }

    public void logInfo(String message) {
        if (_logLevel > 0) _context.log(message);
    }

    public Client newClient(String idPrefix, dojox.cometd.Listener listener) {
        return new ClientImpl(this, idPrefix, listener);
    }

    public abstract ClientImpl newRemoteClient();

    /** Create new transport object for a bayeux message
     * @param client The client
     * @param message the bayeux message
     * @return the negotiated transport.
     */
    public Transport newTransport(ClientImpl client, Map message) {
        if (isLogDebug()) logDebug("newTransport: client=" + client + ",message=" + message);
        Transport result = null;
        try {
            String type = client == null ? null : client.getConnectionType();
            if (type == null) type = (String) message.get("connectionType");
            if (type == null) {
                String jsonp = (String) message.get("jsonp");
                if (jsonp != null) {
                    if (isLogDebug()) logDebug("newTransport: using JSONPTransport with jsonp=" + jsonp);
                    result = new JSONPTransport(client != null && client.isJSONCommented());
                    ((JSONPTransport) result).setJsonp(jsonp);
                }
            }
            if ((type != null) && (result == null)) {
                Class trans_class = (Class) _transports.get(type);
                if (trans_class != null) {
                    if (trans_class.equals(JSONPTransport.class)) {
                        String jsonp = (String) message.get("jsonp");
                        if (jsonp == null) {
                            throw new Exception("JSONPTransport needs jsonp parameter");
                        }
                        result = new JSONPTransport(client != null && client.isJSONCommented());
                        ((JSONPTransport) result).setJsonp(jsonp);
                    } else {
                        result = (Transport) (trans_class.newInstance());
                        result.setJSONCommented(client != null && client.isJSONCommented());
                    }
                }
            }
            if (result == null) {
                result = new JSONTransport(client != null && client.isJSONCommented());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (isLogDebug()) logDebug("newTransport: result=" + result);
        return result;
    }

    public void publish(ChannelId to, Client from, Object data, String msgId) {
        HashMap<String, Object> msg = new HashMap<String, Object>();
        msg.put(CHANNEL_FIELD, to.toString());
        msg.put(TIMESTAMP_FIELD, getTimeOnServer());
        if (msgId == null) {
            long id = msg.hashCode() ^ (to == null ? 0 : to.hashCode()) ^ (from == null ? 0 : from.hashCode());
            id = id < 0 ? -id : id;
            msg.put(ID_FIELD, Long.toString(id, 36));
        } else msg.put(ID_FIELD, msgId);
        msg.put(DATA_FIELD, data);
        _root.publish(to, from, msg);
    }

    public void publish(Client fromClient, String toChannelId, Object data, String msgId) {
        publish(getChannelId(toChannelId), fromClient, data, msgId);
    }

    public boolean removeChannel(ChannelId channelId) {
        return false;
    }

    Client removeClient(String client_id) {
        ClientImpl client;
        synchronized (this) {
            if (client_id == null) return null;
            client = _clients.remove(client_id);
        }
        if (client != null) {
            client.unsubscribeAll();
        }
        return client;
    }

    public void setClientTimeoutMs(long ms) {
        _clientTimeoutMs = ms;
    }

    /**
     * @param commented the commented to set
     */
    public void setJSONCommented(boolean commented) {
        _JSONCommented = commented;
    }

    /**
     * @param logLevel
     *            the logLevel: 0=none, 1=info, 2=debug
     */
    public void setLogLevel(int logLevel) {
        _logLevel = logLevel;
    }

    public void setSecurityPolicy(SecurityPolicy securityPolicy) {
        _securityPolicy = securityPolicy;
    }

    public void subscribe(String toChannel, Client subscriber) {
        ChannelImpl channel = (ChannelImpl) getChannel(toChannel, true);
        if (channel != null) channel.subscribe(subscriber);
    }

    public void unsubscribe(String toChannel, Client subscriber) {
        ChannelImpl channel = (ChannelImpl) getChannel(toChannel);
        if (channel != null) channel.unsubscribe(subscriber);
    }

    /**
     * 
     */
    protected void initialize(ServletContext context) {
        synchronized (this) {
            _initialized = true;
            _context = context;
            try {
                _random = SecureRandom.getInstance("SHA1PRNG");
            } catch (Exception e) {
                context.log("Could not get secure random for ID generation", e);
                _random = new Random();
            }
            _random.setSeed(_random.nextLong() ^ hashCode() ^ (context.hashCode() << 32) ^ Runtime.getRuntime().freeMemory());
            _dateCache = new DateCache();
            _channelIdCache = new ConcurrentHashMap<String, ChannelId>();
        }
    }

    /** Send advice to a client
     * @param client The client to send the advice to
     * @param transport The transport to use
     * @param advice The advice to send.
     * @throws IOException
     */
    public void advise(Client client, Transport transport, Object advice) throws IOException {
        if (advice == null) advice = _advice;
        if (advice == null) advice = __NO_ADVICE;
        String channel = "/meta/connections/" + client.getId();
        Map<String, Object> reply = new HashMap<String, Object>();
        reply.put(CHANNEL_FIELD, channel);
        reply.put(SUCCESSFUL_FIELD, Boolean.TRUE);
        reply.put(ADVICE_FIELD, advice);
        transport.send(reply);
    }

    protected long getRandom(long variation) {
        long l = _random.nextLong() ^ variation;
        return l < 0 ? -l : l;
    }

    /**
     * @return
     */
    String getTimeOnServer() {
        return _dateCache.format(System.currentTimeMillis());
    }

    private static class DefaultPolicy implements SecurityPolicy {

        public boolean authenticate(String scheme, String user, String credentials) {
            return true;
        }

        public boolean canCreate(Client client, ChannelId channel, Map message) {
            return client != null && !"meta".equals(channel.getSegment(0));
        }

        public boolean canSend(Client client, ChannelId channel, Map message) {
            return client != null && !"meta".equals(channel.getSegment(0));
        }

        public boolean canSubscribe(Client client, ChannelId channel, Map message) {
            return client != null && !"meta".equals(channel.getSegment(0));
        }
    }

    private abstract class Handler {

        abstract void handle(ClientImpl client, Transport transport, Map<String, Object> message) throws IOException;

        void unknownClient(Transport transport, String channel) throws IOException {
            Map<String, Object> reply = new HashMap<String, Object>();
            reply.put(CHANNEL_FIELD, channel);
            reply.put(SUCCESSFUL_FIELD, Boolean.FALSE);
            reply.put("error", "Unknown client");
            reply.put("advice", new JSON.Literal("{\"reconnect\":\"handshake\"}"));
            transport.send(reply);
        }
    }

    private class ConnectHandler extends Handler {

        public void handle(ClientImpl client, Transport transport, Map<String, Object> message) throws IOException {
            if (client == null) {
                unknownClient(transport, META_CONNECT);
                return;
            }
            Map<String, Object> reply = new HashMap<String, Object>();
            reply.put(CHANNEL_FIELD, META_CONNECT);
            String type = (String) message.get("connectionType");
            client.setConnectionType(type);
            ChannelImpl connection = client.connect();
            if (connection != null) {
                reply.put(SUCCESSFUL_FIELD, Boolean.TRUE);
                reply.put("error", "");
            } else {
                reply.put(SUCCESSFUL_FIELD, Boolean.FALSE);
                reply.put("error", "unknown client ID");
                if (_unknownAdvice != null) reply.put(ADVICE_FIELD, _unknownAdvice);
            }
            reply.put("timestamp", _dateCache.format(System.currentTimeMillis()));
            transport.send(reply);
            transport.setPolling(false);
            _root.publish(META_CONNECT_ID, client, reply);
        }
    }

    private class DisconnectHandler extends Handler {

        public void handle(ClientImpl client, Transport transport, Map<String, Object> message) throws IOException {
            if (client == null) {
                unknownClient(transport, META_DISCONNECT);
                return;
            }
            client.remove(false);
            Map<String, Object> reply = new HashMap<String, Object>();
            reply.put(CHANNEL_FIELD, META_DISCONNECT);
            reply.put(SUCCESSFUL_FIELD, Boolean.TRUE);
            reply.put("error", "");
            reply.put("timestamp", _dateCache.format(System.currentTimeMillis()));
            transport.send(reply);
            transport.setPolling(false);
            _root.publish(META_DISCONNECT_ID, client, reply);
        }
    }

    private class HandshakeHandler extends Handler {

        public void handle(ClientImpl client, Transport transport, Map<String, Object> message) throws IOException {
            if (client != null) throw new IllegalStateException();
            _root.publish(META_HANDSHAKE_ID, client, message);
            if (_securityPolicy.authenticate((String) message.get("authScheme"), (String) message.get("authUser"), (String) message.get("authToken"))) client = newRemoteClient();
            Map ext = (Map) message.get(EXT_FIELD);
            boolean commented = _JSONCommented && ext != null && ((Boolean) ext.get("json-comment-filtered")).booleanValue();
            Map<String, Object> reply = new HashMap<String, Object>();
            reply.put(CHANNEL_FIELD, META_HANDSHAKE);
            reply.put("version", new Double(0.1));
            reply.put("minimumVersion", new Double(0.1));
            if (isJSONCommented()) reply.put(EXT_FIELD, EXT_JSON_COMMENTED);
            if (client != null) {
                reply.put("supportedConnectionTypes", TRANSPORTS);
                reply.put("successful", Boolean.TRUE);
                reply.put("authSuccessful", Boolean.TRUE);
                reply.put(CLIENT_FIELD, client.getId());
                if (_advice != null) reply.put(ADVICE_FIELD, _advice);
                client.setJSONCommented(commented);
                transport.setJSONCommented(commented);
            } else {
                reply.put("successful", Boolean.FALSE);
                if (_advice != null) reply.put(ADVICE_FIELD, _advice);
            }
            if (isLogDebug()) logDebug("handshake.handle: reply=" + reply);
            transport.send(reply);
            _root.publish(META_HANDSHAKE_ID, client, reply);
        }
    }

    private class PingHandler extends Handler {

        public void handle(ClientImpl client, Transport transport, Map<String, Object> message) throws IOException {
        }
    }

    private class PublishHandler extends Handler {

        public void handle(ClientImpl client, Transport transport, Map<String, Object> message) throws IOException {
            String channel_id = (String) message.get(CHANNEL_FIELD);
            if (client == null) {
                unknownClient(transport, channel_id);
                return;
            }
            String id = (String) message.get(ID_FIELD);
            ChannelId cid = getChannelId(channel_id);
            Object data = message.get("data");
            if (client == null) {
                if (_securityPolicy.authenticate((String) message.get("authScheme"), (String) message.get("authUser"), (String) message.get("authToken"))) client = newRemoteClient();
            }
            Map<String, Object> reply = new HashMap<String, Object>();
            reply.put(CHANNEL_FIELD, channel_id);
            if (id != null) reply.put(ID_FIELD, id);
            if (data != null && _securityPolicy.canSend(client, cid, message)) {
                publish(cid, client, data, id);
                reply.put(SUCCESSFUL_FIELD, Boolean.TRUE);
                reply.put("error", "");
            } else {
                reply.put(SUCCESSFUL_FIELD, Boolean.FALSE);
                reply.put("error", "unknown channel");
            }
            transport.send(reply);
        }
    }

    private class ReconnectHandler extends Handler {

        public void handle(ClientImpl client, Transport transport, Map<String, Object> message) throws IOException {
            if (client == null) {
                unknownClient(transport, META_RECONNECT);
                return;
            }
            _root.publish(META_RECONNECT_ID, client, message);
            Map<String, Object> reply = new HashMap<String, Object>();
            reply.put(CHANNEL_FIELD, META_RECONNECT);
            reply.put("timestamp", _dateCache.format(System.currentTimeMillis()));
            if (client == null) {
                reply.put(SUCCESSFUL_FIELD, Boolean.FALSE);
                reply.put("error", "unknown clientID");
                if (_unknownAdvice != null) reply.put(ADVICE_FIELD, _unknownAdvice);
                transport.setPolling(false);
                transport.send(reply);
            } else {
                String type = (String) message.get("connectionType");
                if (type != null) {
                    if (isLogDebug()) logDebug("Reconnect.handle: old connectionType=" + client.getConnectionType());
                    if ("callback-polling".equals(client.getConnectionType())) logDebug("Reconnect.handle: connectionType remains callback-polling"); else {
                        client.setConnectionType(type);
                        if (isLogDebug()) logDebug("Reconnect.handle: connectionType reset to " + type);
                    }
                }
                reply.put(SUCCESSFUL_FIELD, Boolean.TRUE);
                reply.put("error", "");
                String browser_id = client.getBrowserId();
                if (browser_id != null) {
                    int count = client.onBrowser(browser_id);
                    if (count > 1) {
                        transport.setPolling(false);
                        reply.put("advice", AbstractBayeux.__MULTI_FRAME_ADVICE);
                    } else {
                        transport.setPolling(true);
                        reply.put("advice", AbstractBayeux.__NO_ADVICE);
                    }
                } else {
                    transport.setPolling(true);
                    reply.put("advice", AbstractBayeux.__NO_ADVICE);
                }
                transport.send(reply);
            }
            _root.publish(META_RECONNECT_ID, client, reply);
        }
    }

    private class StatusHandler extends Handler {

        public void handle(ClientImpl client, Transport transport, Map<String, Object> message) throws IOException {
        }
    }

    private class SubscribeHandler extends Handler {

        public void handle(ClientImpl client, Transport transport, Map<String, Object> message) throws IOException {
            if (client == null) {
                unknownClient(transport, META_SUBSCRIBE);
                return;
            }
            _root.publish(META_SUBSCRIBE_ID, client, message);
            String subscribe_id = (String) message.get(SUBSCRIPTION_FIELD);
            if (subscribe_id == null) {
                subscribe_id = Long.toString(getRandom(message.hashCode() ^ client.hashCode()), 36);
                while (getChannel(subscribe_id) != null) subscribe_id = Long.toString(getRandom(message.hashCode() ^ client.hashCode()), 36);
            }
            ChannelId cid = getChannelId(subscribe_id);
            Map<String, Object> reply = new HashMap<String, Object>();
            reply.put(CHANNEL_FIELD, META_SUBSCRIBE);
            reply.put(SUBSCRIPTION_FIELD, subscribe_id);
            if (_securityPolicy.canSubscribe(client, cid, message)) {
                ChannelImpl channel = getChannel(cid);
                if (channel == null && _securityPolicy.canCreate(client, cid, message)) channel = (ChannelImpl) getChannel(subscribe_id, true);
                if (channel != null) {
                    channel.subscribe(client);
                    reply.put(SUCCESSFUL_FIELD, Boolean.TRUE);
                    reply.put("error", "");
                } else {
                    reply.put(SUCCESSFUL_FIELD, Boolean.FALSE);
                    reply.put("error", "cannot create");
                }
            } else {
                reply.put(SUCCESSFUL_FIELD, Boolean.FALSE);
                reply.put("error", "cannot subscribe");
            }
            transport.send(reply);
            _root.publish(META_SUBSCRIBE_ID, client, reply);
        }
    }

    private class UnsubscribeHandler extends Handler {

        public void handle(ClientImpl client, Transport transport, Map<String, Object> message) throws IOException {
            if (client == null) {
                unknownClient(transport, META_UNSUBSCRIBE);
                return;
            }
            _root.publish(META_UNSUBSCRIBE_ID, client, message);
            String channel_id = (String) message.get(SUBSCRIPTION_FIELD);
            ChannelImpl channel = getChannel(channel_id);
            if (channel != null) channel.unsubscribe(client);
            Map<String, Object> reply = new HashMap<String, Object>();
            reply.put(CHANNEL_FIELD, META_UNSUBSCRIBE);
            reply.put(SUBSCRIPTION_FIELD, channel.getId());
            reply.put(SUCCESSFUL_FIELD, Boolean.TRUE);
            reply.put("error", "");
            transport.send(reply);
            _root.publish(META_UNSUBSCRIBE_ID, client, reply);
        }
    }

    public boolean getAlwaysResumePoll() {
        return _alwaysResumePoll;
    }

    /**
     * @param always True if polls are always resumed when messages are received.  This may be needed for some cross domain transports.
     */
    public void setAlwaysResumePoll(boolean always) {
        _alwaysResumePoll = always;
    }
}
