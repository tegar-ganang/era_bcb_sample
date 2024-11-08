package org.timothyb89.jtelirc.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import org.timothyb89.jtelirc.channel.Channel;
import org.timothyb89.jtelirc.config.Configuration;
import org.timothyb89.jtelirc.config.ConfigurationProvider;
import org.timothyb89.jtelirc.config.xml.XMLConfigurationProvider;
import org.timothyb89.jtelirc.listener.InitListener;
import org.timothyb89.jtelirc.listener.MessageListener;
import org.timothyb89.jtelirc.listener.MessageNotifier;
import org.timothyb89.jtelirc.listener.PingListener;
import org.timothyb89.jtelirc.message.type.MessageIdentifier;
import org.timothyb89.jtelirc.plugin.PluginManager;
import org.timothyb89.jtelirc.user.UserList;

/**
 *
 * @author tim
 */
public class Server {

    /**
	 * The default file to read MessageListeners from.
	 */
    public static final File LISTENERS_FILE = new File("conf/listeners.conf");

    /**
	 * The default configuration file.
	 */
    public static final File DEFAULT_CONFIG_FILE = new File("conf/config.xml");

    /**
	 * The default configuration provider.
	 */
    public static final ConfigurationProvider DEFAULT_PROVIDER = new XMLConfigurationProvider(DEFAULT_CONFIG_FILE);

    /**
	 * The configuration of the client.
	 */
    private Configuration configuration;

    /**
	 * The socket to manage server IO.
	 */
    private Socket socket = null;

    /**
	 * The InputThread to manage input from the server.
	 */
    private InputThread inputThread = null;

    /**
	 * The OutputThread to manage output to the server.
	 */
    private OutputThread outputThread = null;

    /**
	 * The MessageIdentifier to identify the type of incoming messages.
	 */
    private MessageIdentifier identifier;

    /**
	 * The list of channels this client is in.
	 */
    private List<Channel> channels;

    /**
	 * The list of ServerListeners.
	 */
    private List<ServerListener> listeners;

    /**
	 * The MessageNotifier that notifies listeners of incoming messages.
	 */
    private MessageNotifier notifier;

    /**
	 * The list of users
	 */
    private UserList userList;

    /**
	 * The plugin manager
	 */
    private PluginManager pluginManager;

    /**
	 * The connection state of this client.
	 */
    private boolean connected = false;

    /**
	 * Creates a new Server object.
	 * @param host The hostname or IP of the server.
	 * @param port The port of the server.
	 * @param nick The nick for the bot to use.
	 */
    public Server(String host, int port, String nick) {
        this(host, port, nick, null);
    }

    /**
	 * Creates a new Server object.
	 * @param host The hostname or IP of the server.
	 * @param port The port of the server.
	 * @param nick The nick for the bot to use.
	 * @param pass The pass to use when connecting to the server.
	 */
    public Server(String host, int port, String nick, String pass) {
        this(new Configuration(host, port, nick, pass));
    }

    /**
	 * Constructs a Server using the given ConfigurationProvider for the config.
	 * @param provider The provider used to load the Configuration.
	 */
    public Server(ConfigurationProvider provider) {
        this(provider.load());
    }

    /**
	 * Constructs a Server with the given configuration.
	 * @param configuration The configuration to constructs this Server with.
	 */
    public Server(Configuration configuration) {
        if (configuration == null) {
            throw new Error("Invalid configuration!");
        }
        this.configuration = configuration;
        listeners = new LinkedList<ServerListener>();
        notifier = new MessageNotifier(this);
        identifier = new MessageIdentifier(this);
        pluginManager = new PluginManager(this);
        pluginManager.loadPlugins();
    }

    /**
	 * Constructs a Server using the default configuration provider.
	 */
    public Server() {
        this(DEFAULT_PROVIDER);
    }

    /**
	 * Connects to the server.
	 * This initializes the socket, IO, listeners, and attempts to send login
	 * information.
	 * @throws java.net.UnknownHostException When the host is not found
	 * @throws java.io.IOException When there is a communication error
	 */
    public void connect() throws UnknownHostException, IOException {
        socket = new Socket(getHost(), getPort());
        outputThread = new OutputThread(this);
        inputThread = new InputThread(this);
        userList = new UserList(this);
        channels = new LinkedList<Channel>();
        System.out.println("DEBUG: Connection established");
        sendLogin();
        notifyConnected();
    }

    /**
	 * Loads the listener at the given classname via the Reflection API.
	 * @param classname The name of the class to load
	 */
    public void addMessageListener(String classname) {
        try {
            Class<?> clazz = Class.forName(classname);
            Constructor cstr = clazz.getConstructor(Server.class);
            MessageListener listener = (MessageListener) cstr.newInstance(this);
            getNotifier().addListener(listener);
        } catch (Exception ex) {
            System.err.println("Failed adding MessageListener:  " + ex.toString());
            ex.printStackTrace();
        }
    }

    /**
	 * Adds the MessageListener to the notification queue.
	 * @param l The listener to add
	 */
    public void addMessageListener(MessageListener l) {
        getNotifier().addListener(l);
    }

    /**
	 * Gets the list of MessageListeners attached to this server.
	 * @return a List of MessageListeners
	 */
    public List<MessageListener> getMessageListeners() {
        return getNotifier().getListeners();
    }

    /**
	 * Removes ALL non-essential MessageListeners.
	 * PingListener and InitListener will not be removed.
	 */
    public void clearMessageListeners() {
        for (MessageListener ml : getMessageListeners()) {
            if (!(ml instanceof PingListener) && !(ml instanceof InitListener)) {
                getNotifier().removeListener(ml);
            }
        }
    }

    /**
	 * Called to notify the bot when identification has been completed. This
	 * prevents the bot from trying to join a channel/sending a message/whatever
	 * when it isn't allowed.
	 */
    public void notifyIdentCompleted() {
        outputThread.startQueue();
    }

    /**
	 * Immediately sends the given text.
	 * @param text The raw text to send
	 */
    public void sendRaw(String text) {
        outputThread.sendRaw(text);
    }

    /**
	 * Queues and sends the given text.
	 * This method must be used for anything required after post identification
	 * (auto joining channels, etc). After initial identification, sendRaw may
	 * be used as well.
	 * @param text The text to send.
	 */
    public void sendQueue(String text) {
        outputThread.queue(text);
    }

    /**
	 * Sends the login commands (NICK, USER, PASS) to the server.
	 * The PASS will not be sent if it is null or empty.
	 * The user and real name are the nick, unless specified in the
	 * configuration. The initial mode is 0 unless configured differently.
	 */
    protected void sendLogin() {
        sendRaw("NICK " + getNick());
        String user = getNick();
        if (configuration.hasProperty("user")) {
            user = configuration.getString("user");
        }
        String real = getNick();
        if (configuration.hasProperty("real name")) {
            real = configuration.getString("real name");
        }
        int mode = 0;
        if (configuration.hasProperty("initial mode")) {
            mode = configuration.getInt("initial mode");
        }
        String umsg = "USER " + user + " " + mode + " * : " + real;
        System.out.println("USER: " + umsg);
        sendRaw(umsg);
        if (getPass() != null && !getPass().isEmpty()) {
            sendRaw("PASS " + getPass());
        }
    }

    /**
	 * Joins a channel. This does not handle errors.
	 * @param channel The channel to join
	 */
    public void join(String channel) {
        Channel c = new Channel(channel, this);
        join(c);
    }

    /**
	 * A wrapper method for Channel.join(); joins the specified channel.
	 * @param channel The channel to join
	 */
    public void join(Channel channel) {
        channel.join();
    }

    /**
	 * Sends a message to the given destination, using the given text.
	 * @param dest The destination of the message
	 * @param text The text to send.
	 */
    public void sendMessage(String dest, String text) {
        text = text.replace("\t", "        ");
        sendQueue("PRIVMSG " + dest + " :" + text);
    }

    public void sendNotice(String dest, String text) {
        text = text.replace("\t", "        ");
        sendQueue("NOTICE " + dest + " :" + text);
    }

    /**
	 * Disconnects from the IRC server, using the quit message
	 * <code>message</code>.
	 * @param message The quit message to use.
	 */
    public void quit(String message) {
        sendRaw("QUIT :" + message);
    }

    /**
	 * Changes the bot's nick.
	 * @param nick The new nick
	 */
    public void setNick(String nick) {
        getConfiguration().getNick().setValue(nick);
        sendRaw("NICK " + nick);
    }

    /**
	 * Gets the host of the IRC server.
	 * @return The IRC server's hostname
	 */
    public String getHost() {
        return (String) configuration.getHost().getValue();
    }

    /**
	 * Gets the InputThread used to process and dispatch incoming messages.
	 * @return The InputThread used by this Server
	 */
    public InputThread getInputThread() {
        return inputThread;
    }

    /**
	 * Gets the nick the bot is currently using.
	 * @return The nick of the bot.
	 */
    public String getNick() {
        return (String) configuration.getNick().getValue();
    }

    /**
	 * Gets the output thread used for dispatching messages sent to the server.
	 * <code>Server</code> contains wrappers for OuputThread.sendRaw() and
	 * OuputThread.sendQueue()
	 * @return this server's OutputThread
	 */
    public OutputThread getOutputThread() {
        return outputThread;
    }

    /**
	 * Gets the pass used to connect to the server. This property is ignored if
	 * it is null.
	 * Note that some servers may be configured to login to NickServ when sending
	 * a PASS command that is not required to connect.
	 * @return The pass that the bot sends during connection
	 */
    public String getPass() {
        return (String) configuration.getPass().getValue();
    }

    /**
	 * The port used to connect to the server.
	 * @return The IRC server's port
	 */
    public Integer getPort() {
        return (Integer) configuration.getPort().getValue();
    }

    /**
	 * Gets the configuration.
	 * @return The configuration.
	 */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
	 * Gets the list of ServerListeners
	 * @return The list of ServerListeners.
	 */
    public List<ServerListener> getListeners() {
        return listeners;
    }

    /**
	 * Gets the Socket used to communicate with the server.
	 * @return The communication Socket.
	 */
    public Socket getSocket() {
        return socket;
    }

    /**
	 * Gets the list of channels the bot is currently in.
	 * @return A list of Channels.
	 */
    public List<Channel> getChannels() {
        return channels;
    }

    /**
	 * Adds a channel to the local channel list.
	 * This should only be called when a channel is joined.
	 * @param c The channel.
	 */
    public void addChannel(Channel c) {
        channels.add(c);
    }

    /**
	 * Gets the channel with the specified name.
	 * This only searches currently joined channels.
	 * @param name The name of the channel to get
	 * @return A Channel with the specified name.
	 */
    public Channel getChannel(String name) {
        for (Channel c : getChannels()) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    /**
	 * Gets the UserList.
	 * The UserList is re-initiated during reconnect.
	 * @return The list of users.
	 */
    public UserList getUserList() {
        return userList;
    }

    /**
	 * Adds a ServerListener
	 * @param l The listener to add
	 */
    public void addListener(ServerListener l) {
        listeners.add(l);
    }

    /**
	 * Removes a ServerListener
	 * @param l The listener to remove
	 */
    public void removeListener(ServerListener l) {
        listeners.add(l);
    }

    /**
	 * Gets the MessageIdentifier, used to determine MessageTypes and begin the
	 * processing of incoming messages.
	 * @return This server's MessageIdentifier
	 */
    public MessageIdentifier getIdentifier() {
        return identifier;
    }

    /**
	 * Gets the MessageNotifier, used to dispatch messages to MessageListeners
	 * @return the MessageNotifier
	 */
    public MessageNotifier getNotifier() {
        return notifier;
    }

    /**
	 * Gets the PluginManager instance.
	 * @return this Server's instance of PluginManager
	 */
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
	 * Returns true if this client is currently connected to an IRC server.
	 * @return True if connected
	 */
    public boolean isConnected() {
        return connected;
    }

    /**
	 * Notifies listeners of a connection being established.
	 * This is called after login info is sent, so it should be safe to send
	 * any messages when listeners are notified.
	 *
	 * Remember that listeners will NOT be notified if they are added after
	 * connection occurs!
	 */
    public void notifyConnected() {
        connected = true;
        for (ServerListener l : listeners) {
            l.onConnected(this);
        }
    }

    /**
	 * Notifies listeners of the client disconnecting from the server.
	 */
    public void notifyDisconnected() {
        connected = false;
        for (ServerListener l : listeners) {
            l.onDisconnected(this);
        }
    }

    /**
	 * Notifies listeners of a channel being joined
	 * @param c The channel joined
	 */
    public void notifyChannelJoined(Channel c) {
        channels.add(c);
        for (ServerListener l : listeners) {
            l.onChannelJoined(c);
        }
    }

    /**
	 * Notifies listeners of a channel being parted
	 * @param c The channel parted
	 * @param msg The part message
	 */
    public void notifyChannelParted(Channel c, String msg) {
        channels.remove(c);
        for (ServerListener l : listeners) {
            l.onChannelParted(c, msg);
        }
    }

    /**
	 * Runs the server, using the default constructor.
	 * @param args The arguments (TODO: actually look at these)
	 */
    public static void main(String[] args) {
        Server server = new Server();
        try {
            server.connect();
            server.join("#jtelirc");
        } catch (Exception ex) {
            System.err.println("Could not connect to server!");
            ex.printStackTrace();
        }
    }
}
