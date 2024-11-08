package com.shimari.bot;

import org.schwering.irc.lib.*;
import java.net.InetSocketAddress;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.shimari.framework.*;
import java.util.*;

/**
 * A Connection manages a connection to an IRC server. It goes 
 * beyond being a simple IRC_Connecton and automates some of the 
 * connection-management behavior. This leaves the Bot itself free 
 * to deal with higher level issues. This interface also isolates
 * us a little from the underlying IRC protocol implementation.
 */
public class IRC_Connection implements IRCEventListener, Connection {

    private static final Logger logger = Logger.getLogger(IRC_Connection.class.getName());

    private static final String[] NULL_STRING_ARRAY = new String[0];

    private final String nick;

    private final String server;

    private final int port;

    private final String password;

    private final String userName;

    private final String realName;

    private boolean isActiveListener = false;

    private boolean isInvitable = true;

    private final Set channels = new HashSet();

    private final Set broadcastChannels = new HashSet();

    private Collection messageQueue;

    private IRCConnection conn = null;

    private final String nickForms[];

    final int delay;

    long nextTime = 0;

    long currDelay;

    /**
     * Initialize the component
     */
    public synchronized void init(Registry components) {
    }

    /**
     * Create (but do not connect) a new Connection.  The password
     * may be null if none is required. Note that this connection does not 
     * establish join any channels--you must call join explicitly,
     * either before or after a connect.
     */
    public IRC_Connection(Config config) throws ConfigException {
        this.nick = config.getString("nick");
        InetSocketAddress addr = config.getAddress("address");
        this.server = addr.getHostName();
        this.port = addr.getPort();
        this.password = config.getString("password", null);
        this.realName = config.getString("realname", "shimari.sourceforge.net");
        this.userName = config.getString("username", "j-bot");
        this.delay = config.getInteger("delayMsec");
        this.currDelay = delay / 4;
        this.nickForms = new String[] { nick + ": ", nick + ", " };
        String ch[] = config.getStringArray("channels");
        for (int i = 0; i < ch.length; i++) {
            joinChannel(ch[i], true);
        }
    }

    /**
     * CONNECTOR API
     * All messages that should be processed must be added to this
     * message queue. The Connection is required to synchronize on 
     * this queue before accessing it and should only add messages
     * to the queue.
     */
    public synchronized void setMessageQueue(Collection queue) {
        messageQueue = queue;
    }

    /**
     * CONNECTOR API
     * Return true if connected.
     */
    public synchronized boolean isConnected() {
        return (conn != null && conn.isConnected());
    }

    /**
     * Return nick @ server : port
     */
    public String toString() {
        return nick + "@" + server + ":" + port;
    }

    /**
     * Introduce a delay so as not to flood the server
     */
    private void delay() {
        long time = System.currentTimeMillis();
        if (time < nextTime) {
            try {
                long sleepTime = nextTime - time;
                Thread.sleep(sleepTime);
                time = nextTime;
                if (currDelay < delay) currDelay *= 1.189;
            } catch (InterruptedException ie) {
            }
        } else {
            currDelay = delay / 4;
        }
        nextTime = time + currDelay;
    }

    /**
     * CONNECTOR API
     * Target can be a nick or a channel (beginning with '#')
     */
    public synchronized void sendMessage(String target, String message) {
        if (isConnected()) {
            delay();
            conn.doPrivmsg(target, message);
        }
    }

    /**
     * CHANNEL API
     */
    public synchronized void broadcastNotice(String message) {
        Iterator i = broadcastChannels.iterator();
        if (isConnected()) {
            while (i.hasNext()) {
                delay();
                conn.doNotice(i.next().toString(), message);
            }
        }
    }

    /**
     * CHANNEL API
     */
    public synchronized void broadcastMessage(String message) {
        Iterator i = broadcastChannels.iterator();
        if (isConnected()) {
            while (i.hasNext()) {
                delay();
                conn.doPrivmsg(i.next().toString(), message);
            }
        }
    }

    /**
     * CONNECTOR API
     * Connect to the IRC server. If already connected, disconnect and 
     * reconnect. This starts a thread to listen to messages from the server.
     */
    public synchronized void connect() throws java.io.IOException {
        if (conn != null) {
            disconnect();
        }
        logger.info("Connecting: " + toString());
        conn = new IRCConnection(server, port, port, password, nick, userName, realName);
        logger.finer("Setting up connection");
        conn.setColors(false);
        conn.setPong(true);
        conn.setDaemon(true);
        conn.addIRCEventListener(this);
        conn.connect();
        logger.info("Connection started: " + conn);
    }

    /**
     * CONNECTOR API
     * Disconnect from the IRC server.
     */
    public synchronized void disconnect() {
        if (conn != null) {
            conn.doQuit();
            conn.interrupt();
            conn = null;
            logger.info("Disconnected: " + toString());
        }
    }

    /** 
     * IRC_Connection extension
     */
    public synchronized String getNick() {
        return nick;
    }

    /** 
     * IRC_Connection extension
     */
    public synchronized String getServer() {
        return server;
    }

    /** 
     * IRC_Connection extension
     */
    public synchronized int getPort() {
        return port;
    }

    /** 
     * IRC_Connection extension
     */
    public synchronized String getUserName() {
        return userName;
    }

    /** 
     * IRC_Connection extension
     */
    public synchronized String getRealName() {
        return realName;
    }

    /**
     * IRC_Connection extension
     * Return a list of the channels which have been added.
     */
    public synchronized String[] getChannels() {
        return (String[]) channels.toArray(NULL_STRING_ARRAY);
    }

    /**
     * IRC_Connection extension
     * An active listener will relay all public messages to the Bot, 
     * whether or not they are addressed to our nick. A non-active 
     * listener will relay private messages, as well as any public 
     * message which explicitly references the connection's nick.
     * The default is FALSE: relay only directly addressed messages.
     */
    public synchronized boolean isActiveListener() {
        return isActiveListener;
    }

    /**
     * Set active listener status: active listener will relay all 
     * messages even if they are unaddressed.
     */
    public synchronized void isActiveListener(boolean willReceivePublicMessages) {
        isActiveListener = willReceivePublicMessages;
    }

    /**
     * IRC_Connection extension
     * An invitable connection will automatically join any channel 
     * to which it is invited by another user.
     */
    public synchronized boolean isInvitable() {
        return isInvitable;
    }

    /**
     * IRC_Connection extension
     * Set whether we are invitable
     */
    public synchronized void isInvitable(boolean willJoinChannelOnInvite) {
        isInvitable = willJoinChannelOnInvite;
    }

    /**
     * IRC_Connection extension
     * Join a channel. We'll remember that we joined and rejoin
     * the channel if we reconnect.
     */
    public synchronized void joinChannel(String channel, boolean broadcastChannel) {
        logger.fine("Joining channel: " + channel);
        if (!channels.contains(channel)) {
            channels.add(channel);
            if (broadcastChannel) broadcastChannels.add(channel);
            if (isConnected()) {
                conn.doJoin(channel);
            }
        }
    }

    /** 
    * IRCEventListener API --
    * Fired when the own connection is successfully established. 
    * This is the case when the first PING? is received. <br />
    * This happens between the connection is opened with a socket and the 
    * connection is registered: The client sends his information to the server 
    * (nickname, username). The server says hello to you by sending you 
    * some <code>NOTICE</code>s. And if your nickname is invalid or in use or 
    * anything else is wrong with your nickname, it asks you for a new one.
    */
    public synchronized void onRegistered() {
        logger.info(toString() + " registered");
        Iterator i = channels.iterator();
        if (isConnected()) {
            while (i.hasNext()) {
                String channel = (String) i.next();
                conn.doJoin(channel);
            }
        }
    }

    /** 
    * IRCEventListener API --
   * Fired when the own connection is broken.
   */
    public synchronized void onDisconnected() {
        if (logger.isLoggable(Level.FINER)) logger.finer(this + ": onDisconnected()");
        if (conn != null) {
            logger.info("Disconnected!");
            conn = null;
        }
    }

    /** 
    * IRCEventListener API --
   * Fired when an <code>ERROR</code> command is received.
   * @param msg The message of the error.
   */
    public synchronized void onError(String msg) {
        logger.warning(toString() + ": " + msg);
    }

    /**
    * IRCEventListener API --
   * Fired when a numeric error is received.
   * The server often sends numeric errors (wrong nickname etc.). 
   * The <code>msg</code>'s format is different for every reply. All replies'
   * formats are described in the {@link org.schwering.irc.lib.IRCUtil}. 
   * The first word in the <code>msg</code> is always your own nickname! 
   * @param num The identifier (usually a 3-digit number).
   * @param msg The message of the error. 
   */
    public synchronized void onError(int num, String msg) {
        logger.warning(toString() + ": " + msg + " [code:" + num + "]");
    }

    /** 
    * IRCEventListener API --
   * Fired when somebody is invited to a channel.
   * @param chan The channel the user is invited to.
   * @param user The user who invites another. Contains nick, username and host.
   * @param passiveNick The nickname of the user who is invited by another user 
   *                    (passive).
   */
    public synchronized void onInvite(String chan, IRCUser user, String passiveNick) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": " + "onInvite(" + chan + "," + user + ", " + passiveNick + ")");
        if (isInvitable) {
            joinChannel(chan, false);
        }
    }

    /** 
    * IRCEventListener API --
   * Fired when somebody joins a channel.
   * @param chan The channel the person joins.
   * @param user The user who joins. Contains nick, username and host.
   */
    public synchronized void onJoin(String chan, IRCUser user) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": " + "onJoin(" + chan + "," + user + ")");
    }

    /** 
    * IRCEventListener API --
   * Fired when somebody is kicked from a channel.
   * @param chan The channel somebody is kicked from.
   * @param user The user who kicks another user from a channel. 
   *             Contains nick, username and host.
   * @param passiveNick The nickname of the user who is kicked from a channel 
   *                    (passive).
   * @param msg The message the active user has set. This is <code>""</code> if 
   *            no message was set.
   */
    public synchronized void onKick(String chan, IRCUser user, String passiveNick, String msg) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": " + "onKick(" + chan + ", " + user + ", " + passiveNick + ", " + msg + ")");
    }

    /** 
    * IRCEventListener API --
   * Fired when an operator changes the modes of a channel. 
   * For example, he can set somebody as an operator, too, or take him the 
   * oper-status. 
   * Also keys, moderated and other channelmodes are fired here.
   * @param chan The channel in which the modes are changed. 
   * @param user The user who changes the modes. 
   *             Contains nick, username and host.
   * @param modeParser The <code>IRCModeParser</code> object which contains the 
   *                   parsed information about the modes which are changed. 
   */
    public synchronized void onMode(String chan, IRCUser user, IRCModeParser modeParser) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": " + "onMode(" + chan + "," + user + "," + modeParser + ")");
    }

    /** 
    * IRCEventListener API --
   * Fired when somebody changes somebody's usermodes. 
   * Note that this event is not fired when a channel-mode is set, for example
   * when someone sets another user as operator or the mode moderated.
   * @param user The user who changes the modes of another user or himself. 
   *             Contains nick, username and host.
   * @param passiveNick The nickname of the person whose modes are changed by 
   *                    another user or himself. 
   * @param mode The changed modes which are set.
   */
    public synchronized void onMode(IRCUser user, String passiveNick, String mode) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": " + "onMode(" + user + "," + passiveNick + "," + mode + ")");
    }

    /** 
    * IRCEventListener API --
   * Fired when somebody changes his nickname successfully.
   * @param user The user who changes his nickname. 
   *             Contains nick, username and host.
   * @param newNick The new nickname of the user who changes his nickname.
   */
    public synchronized void onNick(IRCUser user, String newNick) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": " + "onNick(" + user + "," + newNick + ")");
    }

    /** 
    * IRCEventListener API --
   * Fired when somebody sends a <code>NOTICE</code> to a user or a group. 
   * @param target The channel or nickname the user sent a <code>NOTICE</code> 
   *               to.
   * @param user The user who notices another person or a group. 
   *             Contains nick, username and host.
   * @param msg The message.
   */
    public synchronized void onNotice(String target, IRCUser user, String msg) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": " + "onNotice(" + target + "," + user + "," + msg + ")");
    }

    /** 
    * IRCEventListener API --
   * Fired when somebody parts from a channel.
   * @param chan The channel somebody parts from.
   * @param user The user who parts from a channel. 
   *             Contains nick, username and host.
   * @param msg The part-message which is optionally. 
   *            If it's empty, msg is <code>""</code>.
   */
    public synchronized void onPart(String chan, IRCUser user, String msg) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": " + "onPart(" + chan + "," + user + "," + msg + ")");
    }

    /** 
    * IRCEventListener API --
   * Fired when a <code>PING</code> comes in. 
   * The IRC server tests in different periods if the client is still there by 
   * sending PING &lt;ping&gt;. The client must response PONG &lt;ping&gt;.
   * @param ping The ping which is received from the server.
   */
    public synchronized void onPing(String ping) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": " + "onPing(" + ping + ")");
    }

    /** 
    * IRCEventListener API --
   * Fired when a user sends a <code>PRIVMSG</code> to a user or to a
   * group.
   * @param target The channel or nickname the user sent a <code>PRIVMSG</code> 
   *               to.
   * @param user The user who sent the <code>PRIVMSG</code>. 
   *             Contains nick, username and host.
   * @param msg The message the user transmits.
   */
    public synchronized void onPrivmsg(String target, IRCUser user, String msg) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": onPrivmsg(" + target + "," + user + "," + msg + ")");
        for (int i = 0; i < nickForms.length; i++) {
            if (msg.startsWith(nickForms[i])) {
                msg = msg.substring(nickForms[i].length());
                queue(new IRC_Message(this, user, (nick.equals(target) ? null : target), IRC_Message.ADDRESSED, msg));
                return;
            }
        }
        if (nick.equals(target)) {
            queue(new IRC_Message(this, user, null, IRC_Message.ADDRESSED, msg));
            return;
        }
        if (isActiveListener) {
            queue(new IRC_Message(this, user, target, IRC_Message.UNADDRESSED, msg));
        }
    }

    private void queue(IRC_Message m) {
        synchronized (messageQueue) {
            messageQueue.add(m);
            messageQueue.notify();
        }
    }

    /** 
    * IRCEventListener API --
   * Fired when somebody quits from the network.
   * @param user The user who quits. Contains nick, username and host.
   * @param msg The optional message. <code>""</code> if no message is set by 
   *            the user.
   */
    public synchronized void onQuit(IRCUser user, String msg) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": onQuit(" + user + "," + msg + ")");
    }

    /** 
    * IRCEventListener API --
   * Fired when a numeric reply is received. 
   * For example, <code>WHOIS</code> queries are answered by the server with 
   * numeric replies. 
   * The <code>msg</code>'s format is different for every reply. All replies'
   * formats are described in the {@link org.schwering.irc.lib.IRCUtil}. 
   * The first word in the <code>msg</code> is always your own nickname! 
   * @param num The numeric reply. 
   * @param value The first part of the message.
   * @param msg The main part of the message.
   */
    public synchronized void onReply(int num, String value, String msg) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": onReply(" + num + "," + value + "," + msg + ")");
    }

    /** 
    * IRCEventListener API --
   * Fired when the topic is changed by operators. 
   * Note that the topic is given as a numeric reply fired in 
   * <code>onReply</code> when you join a channel.
   * @param chan The channel where the topic is changed. 
   * @param user The user who changes the topic. 
   *             Contains nick, username and host.
   * @param topic The new topic.
   */
    public synchronized void onTopic(String chan, IRCUser user, String topic) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": onTopic(" + chan + "," + user + ", " + topic + ")");
    }

    /** 
    * IRCEventListener API --
   * This event is fired when the incoming line can not be identified as a known
   * event. 
   * @param prefix The prefix of the incoming line.
   * @param command The command of the incoming line.
   * @param middle The part until the colon (<code>:</code>).
   * @param trailing The part behind the colon (<code>:</code>). 
   */
    public synchronized void unknown(String prefix, String command, String middle, String trailing) {
        if (logger.isLoggable(Level.FINER)) logger.finer(toString() + ": onUnknown(" + prefix + "," + command + "," + middle + "," + trailing + ")");
    }
}
