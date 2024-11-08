package hailmary.network.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;

/**
 * A simple IRC server, implementing only a small subset of the IRC RFC,
 * and allowing only a single channel.
 * @author Corvass
 * @see ConnectionThread
 */
public class Server extends Thread {

    /** A string identifying this version of the IRC server. */
    public static final String VERSION = "BBIrc.1.0a";

    private ServerSocket serverSocket;

    private boolean running = true;

    private Hashtable nickToConnection = new Hashtable();

    private HashSet operators = new HashSet();

    private HashSet channelMembers = new HashSet();

    private Timer timer = new Timer();

    private String channel;

    private Date creationDate;

    private String topic;

    private String topicNick;

    private Date topicDate;

    /**
   * Constructs a single channel IRC server with the given port number
   * and channel. The server starts listening for incoming connections
   * immediately.
   * @param port the port on which to listen
   * @param channel the channel this server uses
   * @throws IOException if the server socket can't be created
   */
    public Server(int port, String channel) throws IOException {
        this.channel = channel;
        creationDate = new Date();
        serverSocket = new ServerSocket(port);
        topic = "";
        start();
    }

    /**
   * Registers a connection with the server.
   * @param connection the connection to register
   * @param nick the nick associated with the connection
   */
    public void registerConnection(ConnectionThread connection, String nick) {
        nick = nick.toLowerCase();
        nickToConnection.put(nick, connection);
    }

    /**
   * Removes a registered connection.
   * @param connection the connection to remove
   * @return <code>true</code> if a connection was removed
   */
    public boolean unregisterConnection(ConnectionThread connection) {
        String nick = connection.getNick().toLowerCase();
        operators.remove(nick);
        channelMembers.remove(nick);
        return (nickToConnection.remove(nick) != null);
    }

    /**
   * Changes the nick associated with a connection.
   * @param connection the connection whose nick is changing
   * @param nick the new nick associated with the connection
   */
    public void nickChange(ConnectionThread connection, String nick) {
        String oldNick = connection.getNick().toLowerCase();
        nick = nick.toLowerCase();
        nickToConnection.remove(oldNick);
        nickToConnection.put(nick, connection);
        if (operators.remove(oldNick)) operators.add(nick);
        if (channelMembers.remove(oldNick)) channelMembers.add(nick);
    }

    /**
   * Adds a user to the channel.
   * @param nick the nick of the user who is to be added.
   */
    public void join(String nick) {
        nick = nick.toLowerCase();
        channelMembers.add(nick);
        if (operators.isEmpty()) operators.add(nick);
    }

    /**
   * Removes a user from the channel.
   * @param nick the nick of the user who is to be removed.
   */
    public void part(String nick) {
        nick = nick.toLowerCase();
        channelMembers.remove(nick);
        operators.remove(nick);
        if (operators.isEmpty()) {
            for (Iterator it = nickToConnection.values().iterator(); it.hasNext(); ) ((ConnectionThread) it.next()).disconnect("Server terminated");
            running = false;
        } else {
            ((ConnectionThread) nickToConnection.get(nick)).disconnect("Left channel");
        }
    }

    /**
   * Changes the topic of the channel.
   * @param topic the new topic
   * @param nick the nick of the user who set the topic
   */
    public void setTopic(String topic, String nick) {
        this.topic = topic;
        topicNick = nick;
        topicDate = new Date();
    }

    /**
   * Sends a message to a single recipient or all users on the channel.
   * @param recipient the nick of the message recipient, 
   *                  or <code>null</code> to send to the channel
   * @param message the message to send
   */
    public void send(String recipient, String message) {
        if (recipient != null) {
            recipient = recipient.toLowerCase();
            ConnectionThread connection = (ConnectionThread) nickToConnection.get(recipient);
            if (connection != null) connection.send(message);
        } else {
            for (Iterator it = channelMembers.iterator(); it.hasNext(); ) ((ConnectionThread) nickToConnection.get((String) it.next())).send(message);
        }
    }

    /**
   * Sends a message to everyone on the channel except one recipient.
   * @param recipient the nick of the user who shouldn't get the message,
   *                  or <code>null</code> to send to the channel
   * @param message the message to send
   */
    public void sendExcept(String recipient, String message) {
        if (recipient == null) send(null, message); else {
            recipient = recipient.toLowerCase();
            ConnectionThread except = (ConnectionThread) nickToConnection.get(recipient);
            if (except == null) send(null, message); else {
                for (Iterator it = channelMembers.iterator(); it.hasNext(); ) {
                    ConnectionThread connection = ((ConnectionThread) nickToConnection.get((String) it.next()));
                    if (!connection.equals(except)) connection.send(message);
                }
            }
        }
    }

    /**
   * Returns the channel of this server.
   * @return the channel of this server
   */
    public String getChannel() {
        return channel;
    }

    /**
   * Returns the creation date of this server.
   * @return the creation date of this server
   */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
   * Returns the topic of this server's channel.
   * @return the topic of this server's channel
   */
    public String getTopic() {
        return topic;
    }

    /**
   * Returns the nick of the user who set the topic.
   * @return the nick of the user who set the topic
   */
    public String getTopicNick() {
        return topicNick;
    }

    /**
   * Returns the date the topic was set.
   * @return the date the topic was set
   */
    public Date getTopicDate() {
        return topicDate;
    }

    /**
   * Returns <code>true</code> if the nick is acceptable according
   * to the IRC RFC. The RFC specifies the following nick syntax:
   * <pre>
   * &lt;nick&gt;       ::= &lt;letter&gt; { &lt;letter&gt; | &lt;number&gt; | &lt;special&gt; }
   * &lt;letter&gt;     ::= 'a' ... 'z' | 'A' ... 'Z'
   * &lt;number&gt;     ::= '0' ... '9'
   * &lt;special&gt;    ::= '-' | '[' | ']' | '\' | '`' | '^' | '{' | '}'
   * </pre>
   * @param nick the nick to check
   * @return <code>true</code> if the nick is acceptable
   */
    public boolean nickIsAcceptable(String nick) {
        if (nick == null || nick.length() == 0) return false;
        char c = nick.charAt(0);
        if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z')) return false;
        for (int i = 1; i < nick.length(); i++) {
            c = nick.charAt(1);
            if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '-' && c != '[' && c != ']' && c != '\\' && c != '`' && c != '^' && c != '{' && c != '}') return false;
        }
        return true;
    }

    /**
   * Returns <code>true</code> if the given nick exists on this server.
   * @param nick the nick to check
   * @return <code>true</code> if the given nick exists on this server
   */
    public boolean nickExists(String nick) {
        return nickToConnection.containsKey(nick.toLowerCase());
    }

    /**
   * Returns <code>true</code> if the given nick is in the channel.
   * @param nick the nick to check
   * @return <code>true</code> if the given nick is in the channel
   */
    public boolean nickInChannel(String nick) {
        return channelMembers.contains(nick.toLowerCase());
    }

    /**
   * Returns <code>true</code> if the given nick is a channel operator.
   * @param nick the nick to check
   * @return <code>true</code> if the given nick is a channel operator
   */
    public boolean isOperator(String nick) {
        return operators.contains(nick.toLowerCase());
    }

    /**
   * Returns a space-seperated list containing all nicks in the channel.
   * Operators are prefixed with a <code>@</code> character.
   * @return a space-seperated list containing all nicks in the channel
   */
    public String getNickList() {
        String s = "";
        for (Iterator it = channelMembers.iterator(); it.hasNext(); ) {
            String nick = (String) it.next();
            if (operators.contains(nick)) s += "@";
            s += ((ConnectionThread) nickToConnection.get(nick)).getNick();
            if (it.hasNext()) s += " ";
        }
        return s;
    }

    /**
   * Returns the host name of this server.
   * @return the host name of this server
   */
    public String getServerName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            return "<unknown host>";
        }
    }

    /**
   * Returns a server global timer. This timer is used to
   * send pings at regular intervals and check for timeouts.
   * @return a server global timer.
   */
    public Timer getTimer() {
        return timer;
    }

    /**
   * Waits for incoming connections and starts a
   * <code>ConnectionThread<code> for each new connection.
   * @see ConnectionThread
   */
    public void run() {
        while (running) try {
            new ConnectionThread(serverSocket.accept(), this);
        } catch (IOException e) {
        }
    }
}
