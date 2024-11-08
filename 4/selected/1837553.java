package de.teamwork.jaicwain.session.irc;

import java.net.InetAddress;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.*;
import de.teamwork.ctcp.CTCPMessage;
import de.teamwork.irc.*;
import de.teamwork.irc.msgutils.*;
import de.teamwork.jaicwain.App;
import de.teamwork.jaicwain.session.*;
import de.teamwork.jaicwain.session.protocols.*;

/**
 * Provides basic IRC communication stuff. That includes connecting to an IRC
 * server, some basic dealing with IRC messages and channel management.
 * 
 * @author Christoph Daniel Schulze
 * @version $Id: AbstractIRCSession.java 3 2003-01-07 14:16:38Z captainnuss $
 */
public abstract class AbstractIRCSession implements Session, IRCConnectionListener {

    /**
     * The user's primary nick name. Needed to log in.
     */
    private String primaryNick = "Noname";

    /**
     * The user's secondary nick name. Needed to log in.
     */
    private String secondaryNick = "IRCUser";

    /**
     * The user's user name. Needed to log in.
     */
    private String userName = "JWUser";

    /**
     * The user's real name. Needed to log in.
     */
    private String realName = "Jaic Wain";

    /**
     * The user's password.
     */
    private String password = "";

    /**
     * <code>IRCConnection</code> used to connect to an IRC server.
     */
    protected IRCConnection conn = null;

    /**
     * The actual user.
     */
    protected IRCUserDescriptor user = new IRCUserDescriptor(primaryNick, userName, "");

    /**
     * List of channels.
     */
    private ArrayList channels = new ArrayList(5);

    /**
     * <code>ArrayList</code> of <code>IRCSessionListener</code>s.
     */
    protected ArrayList listeners = new ArrayList();

    /**
     * <code>ArrayList</code> of <code>SessionStatusEventListener</code>s.
     */
    protected ArrayList statusListeners = new ArrayList();

    /**
     * Indicates which nickname will be used.
     * <ul>
     *   <li>0 - The user's primary nick will be used.</li>
     *   <li>1 - The user's secondary nick will be used.</li>
     *   <li>&gt;1 - <code>nick - 1</code> underscores will be prepended to the
     *               user's primary nick name.</li>
     * </ul>
     */
    private int nick = 0;

    /**
     * <code>boolean</code> variable indicating whether the user tries to change
     * his nick or not.
     */
    private boolean nickChange = false;

    /**
     * <code>boolean</code> variable indicating whether we shall reconnect after
     * a connection has been terminated or not.
     */
    private boolean reconnect = false;

    /**
     * Creates a new <code>AbstractIRCSession</code> object and initializes it.
     */
    public AbstractIRCSession() {
        conn = new IRCConnection(this, null, -1);
    }

    /**
     * Adds a <code>IRCSessionListener</code>.
     * 
     * @param l <code>IRCSessionListener</code> to be added.
     */
    public void addIRCSessionListener(IRCSessionListener l) {
        listeners.add(l);
    }

    /**
     * Removes a <code>IRCSessionListener</code>.
     * 
     * @param l <code>IRCSessionListener</code> to be removed.
     */
    public void removeIRCSessionListener(IRCSessionListener l) {
        listeners.remove(l);
    }

    /**
     * Notifies all <code>IRCSessionListener</code>s that the session has just
     * processed an incoming message.
     */
    protected void fireMessageProcessedEvent(final IRCMessage msg) {
        App.logger.logp(Level.FINEST, "AbstractIRCSession", "fireMessageProcessedEvent(IRCMessage)", "IRCMessage processed: " + msg.toString());
        final AbstractIRCSession session = this;
        Object[] list = listeners.toArray();
        for (int i = list.length - 1; i >= 0; i--) {
            if (list[i] instanceof IRCSessionListener) ((IRCSessionListener) list[i]).messageProcessed(session, msg);
        }
    }

    /**
     * Notifies all <code>IRCSessionListener</code>s that the session has just
     * created a new channel.
     */
    private void fireChannelCreatedEvent(final AbstractIRCChannel channel) {
        App.logger.logp(Level.INFO, "AbstractIRCSession", "fireChannelCreatedEvent(AbstractIRCChannel)", "Channel created.");
        final AbstractIRCSession session = this;
        Object[] list = listeners.toArray();
        for (int i = list.length - 1; i >= 0; i--) {
            if (list[i] instanceof IRCSessionListener) ((IRCSessionListener) list[i]).channelCreated(session, channel);
        }
    }

    /**
     * Notifies all <code>IRCSessionListener</code>s that the session has just
     * removed an existing channel.
     */
    private void fireChannelRemovedEvent(final AbstractIRCChannel channel) {
        App.logger.logp(Level.INFO, "AbstractIRCSession", "fireChannelRemovedEvent(AbstractIRCChannel)", "Channel removed.");
        final AbstractIRCSession session = this;
        Object[] list = listeners.toArray();
        for (int i = list.length - 1; i >= 0; i--) {
            if (list[i] instanceof IRCSessionListener) ((IRCSessionListener) list[i]).channelRemoved(session, channel);
        }
    }

    /**
     * Notifies all <code>SessionStatusEventListener</code>s that the session
     * status has changed.
     * 
     * @param session <code>Session</code> that changed its status.
     * @param oldStatus <code>int</code> containing the old status before the
     *                  change.
     */
    private void fireStatusChangedEvent(final Session session, final int oldStatus) {
        App.logger.logp(Level.INFO, "AbstractIRCSession", "fireStatusChangedEvent(Session, int)", "Status changed from " + String.valueOf(oldStatus) + " to " + String.valueOf(getStatus()));
        Object[] list = statusListeners.toArray();
        for (int i = list.length - 1; i >= 0; i--) {
            if (list[i] instanceof SessionStatusEventListener) ((SessionStatusEventListener) list[i]).sessionStatusChanged(session, oldStatus);
        }
    }

    /**
     * Returns the <code>IRCConnection</code> used by this thing.
     * 
     * @return <code>IRCConnection</code> used by this thing.
     */
    public IRCConnection getConnection() {
        return conn;
    }

    /**
     * Returns the <code>IRCUserDescriptor</code>.
     * 
     * @return <code>IRCUserDescriptor</code>.
     */
    public IRCUserDescriptor getUser() {
        return user;
    }

    /**
     * Returns the primary nick name used to log in. This may not be the user's
     * current nick name! Use {@link #getUser()} to get the current nick name.
     * 
     * @return <code>String</code> containing the primary login nick.
     */
    public String getLoginNick() {
        return primaryNick;
    }

    /**
     * Returns the secondary nick name used to log in if the primary nick is
     * already in use. This may not be the user's current nick name! Use
     * {@link #getUser()} to get the current nick name.
     * 
     * @return <code>String</code> containing the secondary login nick.
     */
    public String getLoginAltNick() {
        return secondaryNick;
    }

    /**
     * Returns the user name used to log in. Use {@link #getUser()} to get the
     * current user name.
     * 
     * @return <code>String</code> containing the user name.
     */
    public String getLoginUserName() {
        return userName;
    }

    /**
     * Returns the user's real name.
     * 
     * @return <code>String</code> containing the real name.
     */
    public String getRealName() {
        return realName;
    }

    /**
     * Return's the user's password.
     * 
     * @return <code>String</code> containing the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns whether the user wants to change his nick or not.
     * 
     * @return <code>boolean</code> indicating whether the user wants to change
     *         his nick (<code>true</code>) or not.
     */
    protected boolean isNickChange() {
        return nickChange;
    }

    /**
     * Sets the primary nick name used to log in.
     * 
     * @param nick <code>String</code> containing the primary login nick.
     */
    public void setLoginNick(String nick) {
        if (nick != null) {
            primaryNick = nick;
            user.setNick(nick);
        }
    }

    /**
     * Sets the secondary nick name used to log in.
     * 
     * @param nick <code>String</code> containing the secondary login nick.
     */
    public void setLoginAltNick(String nick) {
        if (nick != null) secondaryNick = nick;
    }

    /**
     * Sets the user name used to log in.
     * 
     * @param name <code>String</code> containing the user name.
     */
    public void setLoginUserName(String name) {
        if (name != null) {
            userName = name;
            user.setUser(name);
        }
    }

    /**
     * Sets the real name of the user. If this thing's currently connected, this
     * method does nothing.
     * 
     * @param name <code>String</code> containing the real name.
     */
    public void setRealName(String name) {
        if (conn.isActive()) return;
        if (name != null) realName = name;
    }

    /**
     * Sets the user's password. The password isn't necessary to log in.
     * 
     * @param password <code>String</code> containing the user's password.
     */
    public void setPassword(String password) {
        if (conn.isActive()) return;
        if (password == null) this.password = ""; else this.password = password;
    }

    /**
     * Sets whether the user wants to change his nick or not.
     * 
     * @param flag <code>boolean</code> indicating whether the user wants to
     *             change his nick (<code>true</code>) or not.
     */
    protected void setNickChange(boolean flag) {
        nickChange = flag;
    }

    /**
     * Adds a channel with the specified name to the collection. This method
     * should only be used by outer classes when they want to open a new channel
     * for a direct user-to-user chat, not invoking an IRC channel.
     * 
     * @param name <code>String</code> containing the channel's name.
     * @return The newly created <code>AbstractIRCChannel</code> or
     *         <code>null</code> if something went wrong.
     */
    public AbstractIRCChannel addChannel(String name) {
        if (name == null) return null;
        AbstractIRCChannel channel = getChannelImpl(name);
        if (channel == null) return null;
        channels.add(channel);
        fireChannelCreatedEvent(channel);
        return channel;
    }

    /**
     * Removes the channel with the specified name from the collection.
     * 
     * @param name <code>String</code> containing the channel's name.
     */
    protected void removeChannel(String name) {
        if (name == null) return;
        AbstractIRCChannel channel = getChannel(name);
        if (channel == null) return;
        channels.remove(channel);
        fireChannelRemovedEvent(channel);
    }

    /**
     * Returns the channel with the specified name. If a channel with that name
     * doesn't exist, no channel will be created.
     * 
     * @param name <code>String</code> containing the channel's name.
     * @return <code>AbstractIRCChannel</code> with the specified name or
     *         <code>null</code> if none exists.
     */
    public AbstractIRCChannel getChannel(String name) {
        for (int i = 0; i < channels.size(); i++) {
            if (((AbstractIRCChannel) channels.get(i)).getName().equalsIgnoreCase(name)) {
                return (AbstractIRCChannel) channels.get(i);
            }
        }
        return null;
    }

    /**
     * Returns all channels owned by this session.
     * 
     * @return Array of <code>AbstractIRCChannel</code>s.
     */
    public AbstractIRCChannel[] getChannels() {
        return (AbstractIRCChannel[]) channels.toArray(new AbstractIRCChannel[0]);
    }

    /**
     * Forwards the given irc message event to the given channel(s).
     * 
     * @param e <code>IRCMessageEvent</code> to be forwarded.
     * @param channel <code>String</code> containing the name of the target
     *                channel. If this is <code>null</code> or an empty string,
     *                the message will be forwarded to all channels. If the
     *                specified channel doesn't exist, it is created.
     */
    protected void forwardMessage(IRCMessageEvent e, String channel) {
        if (e == null) return;
        AbstractIRCChannel[] chans;
        if (channel == null || channel.equals("")) {
            chans = getChannels();
        } else {
            AbstractIRCChannel chan = getChannel(channel);
            if (chan == null) {
                chan = addChannel(channel);
                if (chan == null) return;
            }
            chans = new AbstractIRCChannel[] { chan };
        }
        for (int i = 0; i < chans.length; i++) chans[i].messageReceived(e);
    }

    /**
     * Returns a subclass of <code>AbstractIRCChannel</code>.
     * 
     * @param name <code>String</code> containing the channel's name.
     * @return <code>AbstractIRCChannel</code> subclass instance.
     */
    protected abstract AbstractIRCChannel getChannelImpl(String name);

    /**
     * Connects to the IRC server using the given login information and the
     * primary nick.
     */
    public void connect() {
        InetAddress remote;
        if ((remote = conn.getRemoteAddress()) == null) {
            App.logger.logp(Level.WARNING, "AbstractIRCSession", "connect()", "No remote address set");
            return;
        }
        App.logger.logp(Level.INFO, "AbstractIRCSession", "connect()", "Connecting to " + remote.getHostName() + " (attempt 1)");
        nick = 0;
        reconnect = false;
        conn.connect();
    }

    /**
     * Connects to the IRC server using the given login information and another
     * nick than the primary one. This method is called whenever a login attempt
     * fails due to nick collision reasons.
     */
    private void reconnect() {
        InetAddress remote = conn.getRemoteAddress();
        App.logger.logp(Level.INFO, "AbstractIRCSession", "reconnect()", "Connecting to " + remote.getHostName() + " (attempt " + String.valueOf(nick) + ")");
        reconnect = false;
        conn.connect();
    }

    /**
     * Disconnects from the server by issuing a MSG_QUIT message. If the option
     * "quitmessage" ("irc" set) is set, that will be used as quit comment. If
     * it isn't set, no comment is used.
     */
    public void disconnect() {
        conn.disconnect();
    }

    /**
     * This method creates a new nickname depending on the value of the
     * <code>nick</code> integer value.
     * <ul>
     *   <li>nick == 0: The primary nick will be returned.</li>
     *   <li>nick == 1: The secondary nick will be returned.</li>
     *   <li>nick &gt; 1: A new nick will be created. It is created by
     *                    prepending <code>nick - 1</code> underscores to the
     *                    primary nickname.</li>
     * </ul>
     *
     * @return String containing the new nickname.
     */
    private String createNick() {
        ++nick;
        if (nick == 1) {
            user.setNick(getLoginNick());
            return getLoginNick();
        } else if (nick == 2) {
            user.setNick(getLoginAltNick());
            return getLoginAltNick();
        } else {
            String temp = new String(primaryNick);
            for (int i = 2; i < nick; i++) temp = new String("_" + temp);
            user.setNick(temp);
            return temp;
        }
    }

    public Protocol getProtocol() {
        return new IRCProtocol();
    }

    public int getLocalPort() {
        return conn.getLocalPort();
    }

    public InetAddress getRemoteAddress() {
        return conn.getRemoteAddress();
    }

    public int getRemotePort() {
        return conn.getRemotePort();
    }

    public boolean isServer() {
        return false;
    }

    public int getStatus() {
        return conn.getStatus();
    }

    public boolean isActive() {
        return conn.isActive();
    }

    public void shutdown() {
        disconnect();
    }

    public void addSessionStatusEventListener(SessionStatusEventListener l) {
        statusListeners.add(l);
    }

    public void removeSessionStatusEventListener(SessionStatusEventListener l) {
        statusListeners.remove(l);
    }

    public void statusChanged(IRCConnection conn, int oldStatus) {
        if (conn.getStatus() == Session.AUTHENTICATING) {
            String args[];
            IRCMessage msg;
            if (!getPassword().equals("")) conn.send(PassMessage.createMessage("", "", "", getPassword()));
            conn.send(NickMessage.createMessage("", "", "", createNick()));
            conn.send(UserMessage.createMessage("", "", "", userName, "0", realName));
        } else if (conn.getStatus() == Session.CONNECTED) {
            AbstractIRCChannel[] chans = getChannels();
            IRCMessage msg;
            for (int i = 0; i < chans.length; i++) {
                if (IRCUtils.isChannel(chans[i].getName())) {
                    msg = JoinMessage.createMessage("", "", "", chans[i].getName());
                    conn.send(msg);
                }
            }
        } else if (conn.getStatus() == Session.UNCONNECTED) {
            AbstractIRCChannel[] chans = getChannels();
            for (int i = 0; i < chans.length; i++) removeChannel(chans[i].getName());
        }
        fireStatusChangedEvent(this, oldStatus);
        if (reconnect) reconnect();
    }

    public void messageSendable(IRCMessageEvent e) {
    }

    public void messageSent(IRCMessageEvent e) {
    }

    public void messageReceived(IRCMessageEvent e) {
        if (e.isConsumed()) return;
        IRCMessage msg = e.getMessage();
        if (msg.getType().equals(IRCMessageTypes.MSG_ADMIN)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_AWAY)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_CONNECT)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_DIE)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_ERROR)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_INFO)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_INVITE)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_ISON)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_JOIN)) {
            ArrayList chans = JoinMessage.getChannels(msg);
            for (int i = 0; i < chans.size(); i++) forwardMessage(e, (String) chans.get(i));
            e.consume();
            fireMessageProcessedEvent(msg);
        } else if (msg.getType().equals(IRCMessageTypes.MSG_KICK)) {
            forwardMessage(e, KickMessage.getChannel(msg));
            if (KickMessage.getUser(msg).equals(user.getNick())) removeChannel(KickMessage.getChannel(msg));
        } else if (msg.getType().equals(IRCMessageTypes.MSG_KILL)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_LINKS)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_LIST)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_LUSERS)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_MODE)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_MOTD)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_NAMES)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_NICK)) {
            if (msg.getNick().equals(user.getNick())) user.setNick(NickMessage.getNickname(msg));
            forwardMessage(e, null);
        } else if (msg.getType().equals(IRCMessageTypes.MSG_NOTICE)) {
            MessageFormat f = new MessageFormat("[{0}] {1}");
            try {
                Object[] o = f.parse(NoticeMessage.getText(msg));
                if (IRCUtils.isChannel(o[0].toString())) {
                    forwardMessage(e, o[0].toString());
                    e.consume();
                    fireMessageProcessedEvent(msg);
                    return;
                }
            } catch (ParseException exc) {
            }
        } else if (msg.getType().equals(IRCMessageTypes.MSG_OPER)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_PART)) {
            ArrayList chans = PartMessage.getChannels(msg);
            for (int i = 0; i < chans.size(); i++) {
                forwardMessage(e, (String) chans.get(i));
                if (msg.getNick().equals(user.getNick())) removeChannel((String) chans.get(i));
            }
            e.consume();
            fireMessageProcessedEvent(msg);
        } else if (msg.getType().equals(IRCMessageTypes.MSG_PASS)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_PING)) {
            conn.send(PongMessage.createMessage("", "", "", PingMessage.getServer1(msg)));
            e.consume();
            fireMessageProcessedEvent(msg);
        } else if (msg.getType().equals(IRCMessageTypes.MSG_PONG)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_PRIVMSG)) {
            if (IRCUtils.isChannel(PrivateMessage.getMsgtarget(msg))) forwardMessage(e, PrivateMessage.getMsgtarget(msg)); else {
                if (CTCPMessage.isCTCPMessage(PrivateMessage.getText(msg))) return; else forwardMessage(e, msg.getNick());
            }
            e.consume();
            fireMessageProcessedEvent(msg);
        } else if (msg.getType().equals(IRCMessageTypes.MSG_QUIT)) {
            forwardMessage(e, null);
            e.consume();
            fireMessageProcessedEvent(msg);
        } else if (msg.getType().equals(IRCMessageTypes.MSG_REHASH)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_RESTART)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_SERVICE)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_SERVLIST)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_SQUERY)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_SQUIT)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_STATS)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_SUMMON)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_TIME)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_TOPIC)) {
            forwardMessage(e, TopicMessage.getChannel(msg));
            e.consume();
            fireMessageProcessedEvent(msg);
        } else if (msg.getType().equals(IRCMessageTypes.MSG_TRACE)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_USER)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_USERHOST)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_USERS)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_VERSION)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_WALLOPS)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_WHO)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_WHOIS)) {
        } else if (msg.getType().equals(IRCMessageTypes.MSG_WHOWAS)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOSUCHNICK)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOSUCHSERVER)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOSUCHCHANNEL)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_CANNOTSENDTOCHAN)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_TOOMANYCHANNELS)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_WASNOSUCHNICK)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_TOOMANYTARGETS)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOSUCHSERVICE)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOORIGIN)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NORECIPIENT)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOTEXTTOSEND)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOTOPLEVEL)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_WILDTOPLEVEL)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_BADMASK)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_UNKNOWNCOMMAND)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOMOTD)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOADMININFO)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_FILEERROR)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NONICKNAMEGIVEN)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_ERRONEUSNICKNAME)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NICKNAMEINUSE)) {
            if (isNickChange() == false) {
                disconnect();
                e.consume();
                fireMessageProcessedEvent(msg);
                reconnect = true;
            }
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NICKCOLLISION)) {
            disconnect();
            e.consume();
            fireMessageProcessedEvent(msg);
            reconnect = true;
        } else if (msg.getType().equals(IRCMessageTypes.ERR_UNAVAILRESOURCE)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_USERNOTINCHANNEL)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOTONCHANNEL)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_USERONCHANNEL)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOLOGIN)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_SUMMONDISABLED)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_USERSDISABLED)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOTREGISTERED)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NEEDMOREPARAMS)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_ALREADYREGISTERED)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOPERMFORHOST)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_PASSWDMISMATCH)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_YOUREBANNEDCREEP)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_YOUWILLBEBANNED)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_KEYSET)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_CHANNELISFULL)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_UNKNOWNMODE)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_INVITEONLYCHAN)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_BANNEDFROMCHAN)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_BADCHANNELKEY)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_BADCHANMASK)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOCHANMODES)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_BANLISTFULL)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOPRIVILEGES)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_CHANOPRIVSNEEDED)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_CANTKILLSERVER)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_RESTRICTED)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_UNIQOPPRIVSNEEDED)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_NOOPERHOST)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_UMODEUNKNOWNFLAG)) {
        } else if (msg.getType().equals(IRCMessageTypes.ERR_USERSDONTMATCH)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_WELCOME)) {
            String str = WelcomeReply.getNick(msg);
            if (user.getNick().startsWith(str)) user.setNick(str);
            if (conn.isActive()) conn.setStatus(Session.CONNECTED);
        } else if (msg.getType().equals(IRCMessageTypes.RPL_YOURHOST)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_CREATED)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_MYINFO)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_BOUNCE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TRACELINK)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TRACECONNECTING)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TRACEHANDSHAKE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TRACEUNKNOWN)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TRACEOPERATOR)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TRACEUSER)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TRACESERVER)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TRACESERVICE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TRACENEWTYPE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_STATSLINKINFO)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_STATSCOMMANDS)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_STATSCLINE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_STATSNLINE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_STATSILINE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_STATSKLINE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_STATSYLINE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ENDOFSTATS)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_UMODEIS)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_SERVLIST)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_STATSLLINE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_STATSUPTIME)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_STATSOLINE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_STATSHLINE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_LUSERCLIENT)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_LUSEROP)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_LUSERUNKNOWN)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_LUSERCHANNELS)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_LUSERME)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ADMINME)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ADMINLOC1)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ADMINLOC2)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ADMINEMAIL)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TRACELOG)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TRACEEND)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TRYAGAIN)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_NONE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_AWAY)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_USERHOST)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ISON)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_UNAWAY)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_NOWAWAY)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_WHOISUSER)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_WHOISSERVER)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_WHOISOPERATOR)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_WHOWASUSER)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ENDOFWHO)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_WHOISIDLE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ENDOFWHOIS)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_WHOISCHANNELS)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_LISTSTART)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_LIST)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_LISTEND)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_CHANNELMODEIS)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_UNIQOPIS)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_NOTOPIC)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TOPIC)) {
            forwardMessage(e, TopicReply.getChannel(msg));
            e.consume();
            fireMessageProcessedEvent(msg);
        } else if (msg.getType().equals(IRCMessageTypes.RPL_INVITING)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_SUMMONING)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_INVITELIST)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ENDOFINVITELIST)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ENDOFEXCEPTLIST)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_VERSION)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_WHOREPLY)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_NAMREPLY)) {
            forwardMessage(e, NamesReply.getChannel(msg));
            e.consume();
            fireMessageProcessedEvent(msg);
        } else if (msg.getType().equals(IRCMessageTypes.RPL_LINKS)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ENDOFLINKS)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ENDOFNAMES)) {
            forwardMessage(e, EndofnamesReply.getChannel(msg));
            e.consume();
            fireMessageProcessedEvent(msg);
        } else if (msg.getType().equals(IRCMessageTypes.RPL_BANLIST)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ENDOFBANLIST)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ENDOFWHOWAS)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_INFO)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_MOTD)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ENDOFINFO)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_MOTDSTART)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ENDOFMOTD)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_YOUREOPER)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_REHASHING)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_YOURESERVICE)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_TIME)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_USERSSTART)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_USERS)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_ENDOFUSERS)) {
        } else if (msg.getType().equals(IRCMessageTypes.RPL_NOUSERS)) {
        }
    }
}
