package javacream.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javacream.util.Attributes;

/**
 * LobbyServer
 * 
 * @author Glenn Powell
 *
 */
public class LobbyServer extends PacketServer {

    public static final String PING = "PING";

    public static final String SET = "SET";

    public static final String GET = "GET";

    public static final String REPLY = "REPLY";

    public static final String RESULT = "RESULT";

    public static final String MESSAGE = "MESSAGE";

    public static final String VARIABLES = "VARIABLES";

    public static final String VALUES = "VALUES";

    public static final String CHANNEL = "CHANNEL";

    public static final String CHANNELS = "CHANNELS";

    public static final String CREATE_CHANNEL = "CREATE_CHANNEL";

    public static final String JOIN_CHANNEL = "JOIN_CHANNEL";

    public static final String LEAVE_CHANNEL = "LEAVE_CHANNEL";

    public static final String NAME = "NAME";

    public static final String ADDRESS = "ADDRESS";

    public static final String PORT = "PORT";

    public static final String USER = "USER";

    public static final String USERS = "USERS";

    public static final String HOST = "HOST";

    public static final String HOSTS = "HOSTS";

    public static final String CREATE_HOST = "CREATE_HOST";

    public static final String CANCEL_HOST = "CANCEL_HOST";

    public static final String USER_CONNECTED = "USER_CONNECTED";

    public static final String USER_UPDATED = "USER_UPDATED";

    public static final String USER_DISCONNECTED = "USER_DISCONNECTED";

    public static final String HOST_CREATED = "HOST_CREATED";

    public static final String HOST_UPDATED = "HOST_UPDATED";

    public static final String HOST_CANCELED = "HOST_CANCELED";

    public static final String CHANNEL_CREATED = "CHANNEL_CREATED";

    public static final String CHANNEL_UPDATED = "CHANNEL_UPDATED";

    public static final String CHANNEL_JOINED = "CHANNEL_JOINED";

    public static final String CHANNEL_LEFT = "CHANNEL_LEFT";

    public static final String CHANNEL_REMOVED = "CHANNEL_REMOVED";

    public static final int SUCCESS = 0;

    public static final int FAILURE = -1;

    public static final int INVALID_PORT = -1;

    private String name;

    private ConcurrentHashMap<User, Host> hosts = new ConcurrentHashMap<User, Host>();

    private ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<String, Channel>();

    private ArrayList<LobbyListener> listeners = new ArrayList<LobbyListener>();

    public LobbyServer() {
        this(null);
    }

    public LobbyServer(String name) {
        this.name = (name != null ? name : String.valueOf(getLocalHost()));
    }

    public String getLobbyName() {
        return name;
    }

    public void setLobbyName(String name) {
        this.name = name;
    }

    public Host[] getHosts() {
        synchronized (this) {
            Host[] hostArray = new Host[hosts.size()];
            int i = 0;
            for (Enumeration<Host> itr = hosts.elements(); itr.hasMoreElements(); ++i) {
                Host host = itr.nextElement();
                hostArray[i] = host;
            }
            return hostArray;
        }
    }

    public Host findHost(User user) {
        return hosts.get(user);
    }

    public Host findHost(SocketThread socketThread) {
        synchronized (this) {
            User user = findUser(socketThread);
            if (user != null) return findHost(user);
        }
        return null;
    }

    public Host findHost(String hostName) {
        synchronized (this) {
            for (Enumeration<Host> itr = hosts.elements(); itr.hasMoreElements(); ) {
                Host host = itr.nextElement();
                if (hostName.equals(host.getName())) return host;
            }
        }
        return null;
    }

    public Channel[] getChannels() {
        synchronized (this) {
            Channel[] channelArray = new Channel[channels.size()];
            int i = 0;
            for (Enumeration<Channel> itr = channels.elements(); itr.hasMoreElements(); ++i) {
                Channel channel = itr.nextElement();
                channelArray[i] = channel;
            }
            return channelArray;
        }
    }

    public Channel findChannel(String name) {
        return channels.get(name);
    }

    public synchronized void addLobbyListener(LobbyListener listener) {
        listeners.add(listener);
    }

    public synchronized LobbyListener[] getLobbyListeners() {
        return listeners.toArray(new LobbyListener[listeners.size()]);
    }

    public synchronized void removeLobbyListener(LobbyListener listener) {
        listeners.remove(listener);
    }

    public void socketClosed(SocketThread socketThread) {
        synchronized (this) {
            User user = findUser(socketThread);
            if (user != null) {
                hosts.remove(user);
                for (Iterator<Entry<String, Channel>> itr = channels.entrySet().iterator(); itr.hasNext(); ) {
                    Entry<String, Channel> entry = itr.next();
                    Channel channel = entry.getValue();
                    channel.removeUser(user.getUsername());
                    if (channel.getUserCount() == 0) itr.remove();
                }
            }
        }
        super.socketClosed(socketThread);
    }

    public void packetRead(SocketThread socketThread, Packet packet) {
        super.packetRead(socketThread, packet);
        User user = findUser(socketThread);
        if (user != null) {
            if (packet.getCommand().equals(PING)) {
                long time = packet.getTime();
                sendReply(socketThread, SUCCESS, PING, new Long(time));
            } else if (packet.getCommand().equals(GET)) {
                String userName = packet.getString(USER, null);
                String[] userNames = packet.get(String[].class, USERS, null);
                String hostName = packet.getString(HOST, null);
                String[] variables = packet.get(String[].class, VARIABLES, null);
                if (variables != null) {
                    if (userName != null) {
                        User getUser = findUser(userName);
                        if (getUser != null) {
                            Attributes attributes = getUser.getAttributes();
                            if (attributes != null) {
                                Attributes resultAttributes = new Attributes();
                                for (int j = 0; j < variables.length; ++j) {
                                    if (isValidUserGetVariable(variables[j])) resultAttributes.put(variables[j], attributes.get(variables[j]));
                                }
                                sendReply(socketThread, SUCCESS, userName, resultAttributes);
                            } else {
                                sendFailure(socketThread, packet, "No Attributes found for given " + USER);
                            }
                        } else {
                            sendFailure(socketThread, packet, "Invalid " + USER + ": " + userName);
                        }
                    } else if (hostName != null) {
                        Host host = findHost(hostName);
                        if (host != null) {
                            Attributes attributes = host.getAttributes();
                            if (attributes != null) {
                                Attributes resultAttributes = new Attributes();
                                for (int j = 0; j < variables.length; ++j) {
                                    if (isValidHostGetVariable(variables[j])) resultAttributes.put(variables[j], attributes.get(variables[j]));
                                }
                                hostUpdated(host);
                                sendReply(socketThread, SUCCESS, hostName, resultAttributes);
                            } else {
                                sendFailure(socketThread, packet, "No Attributes found for given " + HOST);
                            }
                        } else {
                            sendFailure(socketThread, packet, "Invalid " + HOST + ": " + userName);
                        }
                    } else if (userNames != null) {
                        ArrayList<String> userList = new ArrayList<String>();
                        ArrayList<Attributes> attributesList = new ArrayList<Attributes>();
                        for (int i = 0; i < userNames.length; ++i) {
                            User getUser = findUser(userNames[i]);
                            if (getUser != null) {
                                Attributes attributes = getUser.getAttributes();
                                if (attributes != null) {
                                    userList.add(userNames[i]);
                                    Attributes resultAttributes = new Attributes();
                                    attributesList.add(resultAttributes);
                                    for (int j = 0; j < variables.length; ++j) {
                                        if (isValidUserGetVariable(variables[j])) resultAttributes.put(variables[j], attributes.get(variables[j]));
                                    }
                                }
                            }
                        }
                        if (attributesList.size() > 0) sendReply(socketThread, SUCCESS, userList.toArray(new String[userList.size()]), attributesList.toArray(new Attributes[attributesList.size()])); else sendFailure(socketThread, packet, "No Attributes found for given " + USERS);
                    } else {
                        Attributes attributes = getAttributes();
                        Attributes resultAttributes = new Attributes();
                        for (int i = 0; i < variables.length; ++i) {
                            if (isValidGetVariable(variables[i])) {
                                if (variables[i].equals(USERS)) {
                                    resultAttributes.put(USERS, getUsers());
                                } else if (variables[i].equals(HOSTS)) {
                                    resultAttributes.put(HOSTS, getHosts());
                                } else if (variables[i].equals(CHANNELS)) {
                                    resultAttributes.put(CHANNELS, getChannels());
                                } else {
                                    resultAttributes.put(variables[i], attributes.get(variables[i]));
                                }
                            }
                        }
                        sendReply(socketThread, SUCCESS, getLobbyName(), resultAttributes);
                    }
                } else {
                    sendFailure(socketThread, packet, "No " + VARIABLES + " requested");
                }
            } else if (packet.getCommand().equals(SET)) {
                String[] variables = packet.get(String[].class, VARIABLES, null);
                Object[] values = packet.get(Object[].class, VALUES, null);
                if (variables != null) {
                    sendFailure(socketThread, packet, "No " + VARIABLES + " given");
                } else if (values != null) {
                    sendFailure(socketThread, packet, "No " + VALUES + " given");
                } else {
                    Attributes attributes = getAttributes(socketThread);
                    if (attributes != null) {
                        ArrayList<String> invalid = new ArrayList<String>();
                        for (int i = 0; i < variables.length && i < values.length; ++i) {
                            if (isValidUserSetVariable(variables[i])) attributes.put(variables[i], values[i]); else invalid.add(variables[i]);
                        }
                        if (invalid.size() == 0) sendSuccess(socketThread, packet); else {
                            StringBuilder invalids = new StringBuilder();
                            for (Iterator<String> itr = invalid.iterator(); itr.hasNext(); ) invalids.append(itr.next() + ", ");
                            sendFailure(socketThread, packet, "Invalid " + VARIABLES + " given: " + invalids);
                        }
                    } else {
                        sendFailure(socketThread, packet, "No Attributes found for User");
                    }
                }
            } else if (packet.getCommand().equals(MESSAGE)) {
                String channelName = packet.getString(CHANNEL, null);
                String message = packet.getString(MESSAGE, null);
                Channel channel = null;
                if (channelName != null) {
                    channel = findChannel(channelName);
                    if (channel != null) {
                        redirectMessage(user.getUsername(), channel, message);
                    } else {
                        sendFailure(socketThread, packet, "Invalid " + CHANNEL);
                    }
                } else {
                    redirectMessage(user.getUsername(), null, message);
                }
            } else if (packet.getCommand().equals(CREATE_HOST)) {
                String hostName = packet.getString(NAME, null);
                String portName = packet.getString(PORT, null);
                if (hostName == null) {
                    sendFailure(socketThread, packet, "No " + NAME + " given");
                } else if (portName == null) {
                    sendFailure(socketThread, packet, "No " + PORT + " given");
                } else if (hosts.contains(user)) {
                    sendFailure(socketThread, packet, "Host already active for current User");
                } else {
                    try {
                        int port = Integer.parseInt(portName);
                        InetSocketAddress address = new InetSocketAddress(socketThread.getSocket().getInetAddress(), port);
                        Host host = new Host(hostName, user.getUsername(), address);
                        synchronized (this) {
                            hosts.put(user, host);
                        }
                        sendSuccess(socketThread, packet);
                        hostCreated(host);
                    } catch (NumberFormatException e) {
                        sendFailure(socketThread, packet, "Invalid " + PORT);
                    }
                }
            } else if (packet.getCommand().equals(CANCEL_HOST)) {
                Host host = findHost(user);
                if (host != null) {
                    synchronized (this) {
                        hosts.remove(user);
                    }
                    sendSuccess(socketThread, packet);
                    hostCanceled(host);
                } else {
                    sendFailure(socketThread, packet, "No Host active for current User");
                }
            } else if (packet.getCommand().equals(CREATE_CHANNEL)) {
                String channelName = packet.getString(NAME, null);
                if (channelName != null) {
                    Channel channel = findChannel(channelName);
                    if (channel == null) {
                        channel = new Channel(channelName);
                        channel.addUser(user.getUsername());
                        synchronized (this) {
                            channels.put(channelName, channel);
                        }
                        sendSuccess(socketThread, packet);
                        channelCreated(channel);
                    } else {
                        sendFailure(socketThread, packet, NAME + " already exists: " + channelName);
                    }
                } else {
                    sendFailure(socketThread, packet, "No " + NAME + " given");
                }
            } else if (packet.getCommand().equals(JOIN_CHANNEL)) {
                String channelName = packet.getString(NAME, null);
                if (channelName != null) {
                    Channel channel = findChannel(channelName);
                    if (channel != null) {
                        channel.addUser(user.getUsername());
                        sendSuccess(socketThread, packet);
                        channelJoined(user, channel);
                    } else {
                        sendFailure(socketThread, packet, "Invalid " + NAME + ": " + channelName);
                    }
                } else {
                    sendFailure(socketThread, packet, "No " + NAME + " given");
                }
            } else if (packet.getCommand().equals(LEAVE_CHANNEL)) {
                String channelName = packet.getString(NAME, null);
                if (channelName != null) {
                    Channel channel = findChannel(channelName);
                    if (channel != null) {
                        channel.removeUser(user.getUsername());
                        sendSuccess(socketThread, packet);
                        channelLeft(user, channel);
                        if (channel.getUserCount() == 0) {
                            synchronized (this) {
                                channels.remove(channel);
                            }
                            channelRemoved(channel);
                        }
                    } else {
                        sendFailure(socketThread, packet, "Invalid " + NAME + ": " + channelName);
                    }
                } else {
                    sendFailure(socketThread, packet, "No " + NAME + " given");
                }
            }
        } else {
            sendFailure(socketThread, packet, "Not signed in");
        }
    }

    public boolean isHost(User user) {
        return hosts.contains(user);
    }

    public boolean isValidGetVariable(String variable) {
        return variable.equals(USERS) || variable.equals(HOSTS) || variable.equals(CHANNELS);
    }

    public boolean isValidHostGetVariable(String variable) {
        return variable.equals(NAME) || variable.equals(ADDRESS) || variable.equals(PORT) || variable.equals(USER) || variable.equals(USERS);
    }

    public boolean isValidUserGetVariable(String variable) {
        return false;
    }

    public boolean isValidUserSetVariable(String variable) {
        return false;
    }

    public void packetWrite(SocketThread socketThread, Packet packet) {
    }

    protected void userAccepted(SocketThread socketThread, User user) {
        super.userAccepted(socketThread, user);
        send(new Packet(getLocalHost(), USER_CONNECTED, new String[] { USER }, new Object[] { user }));
        synchronized (this) {
            for (Iterator<LobbyListener> itr = listeners.iterator(); itr.hasNext(); ) {
                LobbyListener listener = itr.next();
                listener.lobbyUserConnected(user);
            }
        }
    }

    protected void userClosed(User user) {
        super.userClosed(user);
        send(new Packet(getLocalHost(), USER_DISCONNECTED, new String[] { USER }, new Object[] { user }));
        synchronized (this) {
            for (Iterator<LobbyListener> itr = listeners.iterator(); itr.hasNext(); ) {
                LobbyListener listener = itr.next();
                listener.lobbyUserDisconnected(user);
            }
        }
    }

    public void sendMessage(String message) {
        sendMessage((String) null, message);
    }

    public void sendMessage(String channelName, String message) {
        Channel channel = null;
        if (channelName != null) channel = findChannel(channelName);
        sendMessage(channel, message);
    }

    public void sendMessage(Channel channel, String message) {
        Packet packet = createMessagePacket(getLobbyName(), getLocalHost(), message, channel != null ? channel.getName() : null);
        sendMessage(null, channel, packet);
    }

    public void redirectMessage(String sender, Channel channel, String message) {
        Packet packet = createMessagePacket(sender, getLocalHost(), message, channel != null ? channel.getName() : null);
        sendMessage(null, channel, packet);
    }

    private void sendMessage(User user, Channel channel, Packet packet) {
        if (channel != null) {
            String[] userNames = channel.getUsers();
            for (int i = 0; i < userNames.length; ++i) {
                SocketThread socketThread = findSocketThread(userNames[i]);
                if (socketThread != null) socketThread.send(packet);
            }
        } else {
            send(packet);
        }
        messageSent(user, channel, packet);
    }

    protected static Packet createMessagePacket(String sender, InetAddress source, String message, String channel) {
        return new Packet(source, MESSAGE, new String[] { MESSAGE, CHANNEL, NAME }, new Object[] { message, channel, sender });
    }

    private synchronized void messageSent(User user, Channel channel, Packet packet) {
        for (Iterator<LobbyListener> itr = listeners.iterator(); itr.hasNext(); ) {
            LobbyListener listener = itr.next();
            listener.lobbyMessageSent(user, channel, packet);
        }
    }

    private synchronized void hostCreated(Host host) {
        send(new Packet(getLocalHost(), HOST_CREATED, new String[] { HOST }, new Object[] { host }));
        for (Iterator<LobbyListener> itr = listeners.iterator(); itr.hasNext(); ) {
            LobbyListener listener = itr.next();
            listener.lobbyHostCreated(host);
        }
    }

    private synchronized void hostCanceled(Host host) {
        send(new Packet(getLocalHost(), HOST_CANCELED, new String[] { HOST }, new Object[] { host }));
        for (Iterator<LobbyListener> itr = listeners.iterator(); itr.hasNext(); ) {
            LobbyListener listener = itr.next();
            listener.lobbyHostCanceled(host);
        }
    }

    private synchronized void hostUpdated(Host host) {
        send(new Packet(getLocalHost(), HOST_UPDATED, new String[] { HOST }, new Object[] { host }));
        for (Iterator<LobbyListener> itr = listeners.iterator(); itr.hasNext(); ) {
            LobbyListener listener = itr.next();
            listener.lobbyHostUpdated(host);
        }
    }

    private synchronized void channelCreated(Channel channel) {
        send(new Packet(getLocalHost(), CHANNEL_CREATED, new String[] { CHANNEL }, new Object[] { channel }));
        for (Iterator<LobbyListener> itr = listeners.iterator(); itr.hasNext(); ) {
            LobbyListener listener = itr.next();
            listener.lobbyChannelCreated(channel);
        }
    }

    private synchronized void channelJoined(User user, Channel channel) {
        send(new Packet(getLocalHost(), CHANNEL_JOINED, new String[] { USER, CHANNEL }, new Object[] { user, channel }));
        for (Iterator<LobbyListener> itr = listeners.iterator(); itr.hasNext(); ) {
            LobbyListener listener = itr.next();
            listener.lobbyChannelJoined(user, channel);
        }
    }

    private synchronized void channelLeft(User user, Channel channel) {
        send(new Packet(getLocalHost(), CHANNEL_LEFT, new String[] { USER, CHANNEL }, new Object[] { user, channel }));
        for (Iterator<LobbyListener> itr = listeners.iterator(); itr.hasNext(); ) {
            LobbyListener listener = itr.next();
            listener.lobbyChannelLeft(user, channel);
        }
    }

    private synchronized void channelRemoved(Channel channel) {
        send(new Packet(getLocalHost(), CHANNEL_REMOVED, new String[] { CHANNEL }, new Object[] { channel }));
        for (Iterator<LobbyListener> itr = listeners.iterator(); itr.hasNext(); ) {
            LobbyListener listener = itr.next();
            listener.lobbyChannelRemoved(channel);
        }
    }

    public static void sendGet(SocketThread socketThread, String variable) {
        sendGet(socketThread, new String[] { variable });
    }

    public static void sendGet(SocketThread socketThread, String[] variables) {
        socketThread.send(new Packet(socketThread, GET, new String[] { VARIABLES }, new Object[] { variables }));
    }

    public static void sendGet(SocketThread socketThread, String variable, String user) {
        sendGet(socketThread, new String[] { variable }, user);
    }

    public static void sendGet(SocketThread socketThread, String[] variables, String user) {
        socketThread.send(new Packet(socketThread, GET, new String[] { VARIABLES, USER }, new Object[] { variables, user }));
    }

    public static void sendGet(SocketThread socketThread, String variable, String[] users) {
        sendGet(socketThread, new String[] { variable }, users);
    }

    public static void sendGet(SocketThread socketThread, String[] variables, String[] users) {
        socketThread.send(new Packet(socketThread, GET, new String[] { VARIABLES, USERS }, new Object[] { variables, users }));
    }

    public static void sendGetUsers(SocketThread socketThread) {
        sendGet(socketThread, new String[] { USERS });
    }

    public static void sendGetHosts(SocketThread socketThread) {
        sendGet(socketThread, new String[] { HOSTS });
    }

    public static void sendSet(SocketThread socketThread, String variable, Object value) {
        sendSet(socketThread, new String[] { variable }, new Object[] { value });
    }

    public static void sendSet(SocketThread socketThread, String[] variables, Object[] values) {
        socketThread.send(new Packet(socketThread, SET, new String[] { VARIABLES, VALUES }, new Object[] { variables, values }));
    }

    protected static void sendReply(SocketThread socketThread, int result, String variable, Object value) {
        sendReply(socketThread, result, new String[] { variable }, new Object[] { value });
    }

    protected static void sendReply(SocketThread socketThread, int result, String[] variables, Object[] values) {
        Packet packet = new Packet(socketThread, REPLY, variables, values);
        packet.putInt(RESULT, result);
        socketThread.send(packet);
    }

    protected static void sendReply(SocketThread socketThread, int result, String message) {
        Packet packet = new Packet(socketThread, REPLY);
        packet.putInt(RESULT, result);
        packet.put(MESSAGE, message);
        socketThread.send(packet);
    }

    protected static void sendSuccess(SocketThread socketThread, Packet packet) {
        sendSuccess(socketThread, packet, null);
    }

    protected static void sendSuccess(SocketThread socketThread, Packet packet, String message) {
        sendReply(socketThread, SUCCESS, "Successful " + packet.getCommand() + (message != null ? ": " + message : ""));
    }

    protected static void sendFailure(SocketThread socketThread, Packet packet) {
        sendFailure(socketThread, packet, null);
    }

    protected static void sendFailure(SocketThread socketThread, Packet packet, String message) {
        sendReply(socketThread, FAILURE, "Failed " + packet.getCommand() + (message != null ? ": " + message : ""));
    }
}
