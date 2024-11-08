package net.hypotenubel.irc.net;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;
import org.apache.log4j.Logger;
import net.hypotenubel.ctcp.CTCPMessage;
import net.hypotenubel.irc.*;
import net.hypotenubel.irc.msgutils.*;
import net.hypotenubel.jaicwain.App;
import net.hypotenubel.jaicwain.session.*;
import net.hypotenubel.jaicwain.session.irc.IRCProtocol;

/**
 * Provides basic IRC communication functionality. That includes connecting to
 * an IRC server, some basic dealing with IRC messages and channel management.<p>
 * 
 * Whenever an {@code AbstractIRCSession} is notified that a message was received,
 * it looks in its message map whether any {@link IRCMessageHandler}s are registered
 * for the message's message type. If so, the message event is given to every registered
 * handler, starting with the oldest. Thus, any subclass that wants to handle
 * certain message types should register a message handler for it.
 * 
 * @author Christoph Daniel Schulze
 * @version $Id: AbstractIRCSession.java 155 2006-10-08 22:11:13Z captainnuss $
 */
public abstract class AbstractIRCSession implements Session, IRCConnectionListener {

    /**
     * The user's primary nick name. Needed to log in.
     */
    private String primaryLoginNick = "Noname";

    /**
     * The user's secondary nick name. Needed to log in.
     */
    private String secondaryLoginNick = "IRCUser";

    /**
     * The user's user name. Needed to log in.
     */
    private String loginUserName = "JaicWain";

    /**
     * The user's real name. Needed to log in.
     */
    private String realName = "Jaic Wain";

    /**
     * The user's operator password, if any.
     */
    private String password = "";

    /**
     * {@code IRCConnection} used to connect to an IRC server.
     */
    protected IRCConnection conn = null;

    /**
     * The actual user.
     */
    protected IRCMessagePrefix user = new IRCMessagePrefix(primaryLoginNick, loginUserName, "");

    /**
     * List of channels.
     */
    private Map<String, AbstractIRCChannel> channels = new HashMap<String, AbstractIRCChannel>(10);

    /**
     * Message map mapping message types to lists of registered handlers.
     */
    private Map<String, List<IRCMessageHandler>> messageMap = new HashMap<String, List<IRCMessageHandler>>();

    /**
     * List of {@code IRCSessionListener}s.
     */
    protected ArrayList<IRCSessionListener> listeners = new ArrayList<IRCSessionListener>();

    /**
     * List of {@code SessionStatusEventListener}s.
     */
    protected ArrayList<SessionStatusEventListener> statusListeners = new ArrayList<SessionStatusEventListener>();

    /**
     * Indicates which nickname will be used.
     * <ul>
     *   <li>0 - The user's primary nick will be used.</li>
     *   <li>1 - The user's secondary nick will be used.</li>
     *   <li>&gt;1 - {@code nick - 1} underscores will be prepended to the
     *               user's primary nick name.</li>
     * </ul>
     */
    private int nick = 0;

    /**
     * Flag indicating whether the user tries to change his nick or not.
     */
    private boolean changeingNick = false;

    /**
     * Flag indicating whether we shall reconnect after a connection has
     * been terminated or not.
     */
    private boolean reconnect = false;

    /**
     * Creates a new instance and initializes it.
     */
    public AbstractIRCSession() {
        conn = new IRCConnection(this, null, -1);
    }

    /**
     * Returns the {@code IRCConnection} used by this thing.
     * 
     * @return {@code IRCConnection} used by this thing.
     */
    public IRCConnection getConnection() {
        return conn;
    }

    /**
     * Returns the current nick name.
     * 
     * @return the current nick name.
     */
    public String getNickName() {
        return user.getNickName();
    }

    /**
     * Returns the primary nick name used to log in. This may not be the user's
     * current nick name! Use {@link #getNickName()} to get the current nick name.
     * 
     * @return the primary login nick.
     */
    public String getPrimaryLoginNick() {
        return primaryLoginNick;
    }

    /**
     * Returns the secondary nick name used to log in if the primary nick is
     * already in use. This may not be the user's current nick name! Use
     * {@link #getNickName()} to get the current nick name.
     * 
     * @return the secondary login nick.
     */
    public String getSecondaryLoginNick() {
        return secondaryLoginNick;
    }

    /**
     * Returns the user name used to login.
     * 
     * @return the user name.
     */
    public String getLoginUserName() {
        return loginUserName;
    }

    /**
     * Returns the user's real name.
     * 
     * @return the real name.
     */
    public String getRealName() {
        return realName;
    }

    /**
     * Returns the user's operator password.
     * 
     * @return the operator password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Checks whether the user is changeing his nick or not.
     * 
     * @return {@code boolean} indicating whether the user wants to change
     *         his nick ({@code true}) or not.
     */
    protected boolean isChangeingNick() {
        return changeingNick;
    }

    /**
     * Sets the primary nick name used to log in.
     * 
     * @param nick the primary login nick.
     * @throws IllegalStateException if {@link #isActive()} returns {@code true}.
     */
    public void setPrimaryLoginNick(String nick) {
        if (this.isActive()) {
            throw new IllegalStateException("can't change the name while connected.");
        }
        if (nick != null) {
            primaryLoginNick = nick;
            user.setNickName(nick);
        }
    }

    /**
     * Sets the secondary nick name used to log in.
     * 
     * @param nick the secondary login nick.
     * @throws IllegalStateException if {@link #isActive()} returns {@code true}.
     */
    public void setSecondaryLoginNick(String nick) {
        if (this.isActive()) {
            throw new IllegalStateException("can't change the name while connected.");
        }
        if (nick != null) {
            secondaryLoginNick = nick;
        }
    }

    /**
     * Sets the user name used to login.
     * 
     * @param name the user name.
     * @throws IllegalStateException if {@link #isActive()} returns {@code true}.
     */
    public void setLoginUserName(String name) {
        if (this.isActive()) {
            throw new IllegalStateException("can't change the name while connected.");
        }
        if (name != null) {
            loginUserName = name;
            user.setUser(name);
        }
    }

    /**
     * Sets the real name of the user.
     * 
     * @param name {@code String} containing the real name.
     * @throws IllegalStateException if {@link #isActive()} returns {@code true}.
     */
    public void setRealName(String name) {
        if (this.isActive()) {
            throw new IllegalStateException("can't change the name while connected.");
        }
        if (name != null) {
            realName = name;
        }
    }

    /**
     * Sets the user's password. The password isn't necessary to log in.
     * 
     * @param password the user's operator password.
     * @throws IllegalStateException if {@link #isActive()} returns {@code true}.
     */
    public void setPassword(String password) {
        if (this.isActive()) {
            throw new IllegalStateException("can't change the password while connected.");
        }
        if (password != null) {
            this.password = password;
        }
    }

    /**
     * Sets whether the user wants to change his nick or not.
     * 
     * @param flag {@code boolean} indicating whether the user wants to
     *             change his nick ({@code true}) or not.
     */
    protected void setChangeingNick(boolean flag) {
        changeingNick = flag;
    }

    /**
     * Adds a channel with the specified name to the collection. This method
     * should only be used by outer classes when they want to open a new channel
     * for a direct user-to-user chat, not for joining an IRC channel.
     * 
     * @param name the channel's name.
     * @return the newly created {@code AbstractIRCChannel} or
     *         {@code null} if something went wrong.
     */
    public AbstractIRCChannel addChannel(String name) {
        if (name == null) {
            return null;
        }
        AbstractIRCChannel channel = getChannel(name);
        if (channel != null) {
            return channel;
        }
        channel = getChannelImpl(name);
        if (channel == null) {
            return null;
        }
        channels.put(name, channel);
        fireChannelCreatedEvent(channel);
        return channel;
    }

    /**
     * Removes the channel with the specified name from the collection.
     * 
     * @param name the channel's name.
     */
    public void removeChannel(String name) {
        if (name == null) {
            return;
        }
        AbstractIRCChannel channel = getChannel(name);
        if (channel == null) {
            return;
        }
        channels.remove(channel);
        fireChannelRemovedEvent(channel);
    }

    /**
     * Returns the channel with the specified name. If a channel with that name
     * doesn't exist, no channel will be created.
     * 
     * @param name the channel's name.
     * @return {@code AbstractIRCChannel} with the specified name or
     *         {@code null} if none exists.
     */
    public AbstractIRCChannel getChannel(String name) {
        return this.channels.get(name);
    }

    /**
     * Returns all channels owned by this session.
     * 
     * @return array of {@code AbstractIRCChannel}s.
     */
    public AbstractIRCChannel[] getChannels() {
        Collection<AbstractIRCChannel> c = channels.values();
        return c.toArray(new AbstractIRCChannel[c.size()]);
    }

    /**
     * Forwards the given IRC message event to the given channel(s).
     * 
     * @param e the event to be forwarded.
     * @param channel the name of the target channel. If this is {@code null}
     *                or an empty string, the message will be forwarded to
     *                all channels. If the specified channel doesn't exist,
     *                it is created.
     */
    public void forwardMessage(IRCMessageEvent e, String channel) {
        if (e == null) {
            return;
        }
        AbstractIRCChannel[] chans;
        if (channel == null || channel.equals("")) {
            chans = getChannels();
        } else {
            AbstractIRCChannel chan = getChannel(channel);
            if (chan == null) {
                chan = addChannel(channel);
                if (chan == null) {
                    return;
                }
            }
            chans = new AbstractIRCChannel[] { chan };
        }
        for (AbstractIRCChannel c : chans) {
            c.messageReceived(e);
        }
    }

    /**
     * Returns an implementation of {@code AbstractIRCChannel}.
     * 
     * @param name the channel's name.
     * @return {@code AbstractIRCChannel} implementation.
     */
    protected abstract AbstractIRCChannel getChannelImpl(String name);

    /**
     * Connects to the IRC server using the given login information and the
     * primary nick.
     */
    public void connect() {
        String remote;
        if ((remote = conn.getServerName()) == null) {
            Logger.getLogger(AbstractIRCSession.class).error("Can't connect: no remote address set!");
            return;
        }
        Logger.getLogger(AbstractIRCSession.class).info("Connecting to " + remote + " (attempt 1)");
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
        String remote = conn.getServerName();
        Logger.getLogger(AbstractIRCSession.class).info("Connecting to " + remote + " (attempt " + nick + ")");
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
     * {@code nick} integer value.
     * <ul>
     *   <li>nick == 0: The primary nick will be returned.</li>
     *   <li>nick == 1: The secondary nick will be returned.</li>
     *   <li>nick &gt; 1: A new nick will be created. It is created by
     *                    prepending {@code nick - 1} underscores to the
     *                    primary nickname.</li>
     * </ul>
     *
     * @return String containing the new nickname.
     */
    private String createNick() {
        ++nick;
        if (nick == 1) {
            user.setNickName(getPrimaryLoginNick());
            return getPrimaryLoginNick();
        } else if (nick == 2) {
            user.setNickName(getSecondaryLoginNick());
            return getSecondaryLoginNick();
        } else {
            String temp = new String(primaryLoginNick);
            for (int i = 2; i < nick; i++) {
                temp = new String("_" + temp);
            }
            user.setNickName(temp);
            return temp;
        }
    }

    public Protocol getProtocol() {
        return new IRCProtocol();
    }

    public int getLocalPort() {
        return conn.getLocalPort();
    }

    public String getRemoteAddress() {
        return conn.getServerName();
    }

    public int getRemotePort() {
        return conn.getRemotePort();
    }

    public boolean isServer() {
        return false;
    }

    public SessionStatus getStatus() {
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

    public void statusChanged(IRCConnection conn, SessionStatus oldStatus) {
        if (conn.getStatus() == SessionStatus.AUTHENTICATING) {
            if (!getPassword().equals("")) {
                conn.send(PassMessage.createMessage("", "", "", getPassword()));
            }
            conn.send(NickMessage.createMessage("", "", "", createNick()));
            conn.send(UserMessage.createMessage("", "", "", loginUserName, "0", realName));
        } else if (conn.getStatus() == SessionStatus.CONNECTED) {
            AbstractIRCChannel[] chans = getChannels();
            IRCMessage msg;
            for (int i = 0; i < chans.length; i++) {
                if (IRCUtils.isChannel(chans[i].getName())) {
                    msg = JoinMessage.createMessage("", "", "", chans[i].getName());
                    conn.send(msg);
                }
            }
        } else if (conn.getStatus() == SessionStatus.UNCONNECTED) {
            AbstractIRCChannel[] chans = getChannels();
            for (int i = 0; i < chans.length; i++) {
                removeChannel(chans[i].getName());
            }
            if (!reconnect) {
                App.sessions.removeSession(this);
            }
        }
        fireStatusChangedEvent(this, oldStatus);
        if (reconnect) {
            reconnect();
        }
    }

    public void messageSendable(IRCMessageEvent e) {
    }

    public void messageSent(IRCMessageEvent e) {
        if (e.getMessage().getType().equals(IRCMessageTypes.MSG_NICK)) {
            if (getStatus() == SessionStatus.CONNECTED) {
                setChangeingNick(true);
            }
        }
    }

    /**
     * Forwards the message event to all registered {@link IRCMessageHandler}s
     * for the given message type.
     * 
     * @param e message event.
     */
    public void messageReceived(IRCMessageEvent e) {
        if (e.isConsumed()) {
            return;
        }
        List<IRCMessageHandler> handlers = this.messageMap.get(e.getMessage().getType());
        if (handlers != null) {
            for (int i = 0; i < handlers.size(); i++) {
                handlers.get(i).handleMessage(e);
            }
        }
    }

    /**
     * Adds the given message handler to the message map for the given
     * message type.
     * 
     * @param type the message type. Probably one of the constants defined in
     *             {@link net.hypotenubel.irc.IRCMessageTypes}.
     * @param handler the message handler to add.
     * @throws IllegalArgumentException if {@code type == null},
     *                                  {@code type.trim().length() == 0} or
     *                                  {@code handler == null}.
     */
    public void addMessageHandler(String type, IRCMessageHandler handler) {
        if (type == null || type.trim().length() == 0) {
            throw new IllegalArgumentException("type can't be empty.");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler can't be null.");
        }
        List<IRCMessageHandler> handlers = this.messageMap.get(type);
        if (handlers == null) {
            handlers = new ArrayList<IRCMessageHandler>(5);
            this.messageMap.put(type, handlers);
        }
        handlers.add(handler);
    }

    /**
     * Sets the given message handler as the only handler for the given
     * message type, removing other handlers, if any.
     * 
     * @param type the message type. Probably one of the constants defined in
     *             {@link net.hypotenubel.irc.IRCMessageTypes}.
     * @param handler the message handler to add.
     * @throws IllegalArgumentException if {@code type == null},
     *                                  {@code type.trim().length() == 0} or
     *                                  {@code handler == null}.
     */
    public void setMessageHandler(String type, IRCMessageHandler handler) {
        if (type == null || type.trim().length() == 0) {
            throw new IllegalArgumentException("type can't be empty.");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler can't be null.");
        }
        List<IRCMessageHandler> handlers = this.messageMap.get(type);
        if (handlers == null) {
            handlers = new ArrayList<IRCMessageHandler>(5);
            this.messageMap.put(type, handlers);
        } else {
            handlers.clear();
        }
        handlers.add(handler);
    }

    /**
     * Removes the given message handler from the message map for the given
     * message type.
     * 
     * @param type the message type. Probably one of the constants defined in
     *             {@link net.hypotenubel.irc.IRCMessageTypes}.
     * @param handler the message handler to remove.
     * @throws IllegalArgumentException if {@code type == null},
     *                                  {@code type.trim().length() == 0} or
     *                                  {@code handler == null}.
     */
    public void removeMessageHandler(String type, IRCMessageHandler handler) {
        if (type == null || type.trim().length() == 0) {
            throw new IllegalArgumentException("type can't be empty.");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler can't be null.");
        }
        List<IRCMessageHandler> handlers = this.messageMap.get(type);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    /**
     * Adds a {@code IRCSessionListener}.
     * 
     * @param l {@code IRCSessionListener} to be added.
     */
    public void addIRCSessionListener(IRCSessionListener l) {
        listeners.add(l);
    }

    /**
     * Removes a {@code IRCSessionListener}.
     * 
     * @param l {@code IRCSessionListener} to be removed.
     */
    public void removeIRCSessionListener(IRCSessionListener l) {
        listeners.remove(l);
    }

    /**
     * Notifies all {@code IRCSessionListener}s that the session has just
     * processed an incoming message.
     */
    protected void fireMessageProcessedEvent(final IRCMessage msg) {
        final AbstractIRCSession session = this;
        Object[] list = listeners.toArray();
        for (int i = list.length - 1; i >= 0; i--) {
            if (list[i] instanceof IRCSessionListener) {
                ((IRCSessionListener) list[i]).messageProcessed(session, msg);
            }
        }
    }

    /**
     * Notifies all {@code IRCSessionListener}s that the session has just
     * created a new channel.
     */
    private void fireChannelCreatedEvent(final AbstractIRCChannel channel) {
        Logger.getLogger(AbstractIRCSession.class).debug("Channel created: " + channel.getName());
        final AbstractIRCSession session = this;
        Object[] list = listeners.toArray();
        for (int i = list.length - 1; i >= 0; i--) {
            if (list[i] instanceof IRCSessionListener) {
                ((IRCSessionListener) list[i]).channelCreated(session, channel);
            }
        }
    }

    /**
     * Notifies all {@code IRCSessionListener}s that the session has just
     * removed an existing channel.
     */
    private void fireChannelRemovedEvent(final AbstractIRCChannel channel) {
        Logger.getLogger(AbstractIRCSession.class).debug("Channel removed: " + channel.getName());
        final AbstractIRCSession session = this;
        Object[] list = listeners.toArray();
        for (int i = list.length - 1; i >= 0; i--) {
            if (list[i] instanceof IRCSessionListener) {
                ((IRCSessionListener) list[i]).channelRemoved(session, channel);
            }
        }
    }

    /**
     * Notifies all {@code SessionStatusEventListener}s that the session
     * status has changed.
     * 
     * @param session {@code Session} that changed its status.
     * @param oldStatus the old status before the change.
     */
    private void fireStatusChangedEvent(final Session session, final SessionStatus oldStatus) {
        Logger.getLogger(AbstractIRCSession.class).debug("Status changed from " + oldStatus + " to " + getStatus());
        Object[] list = statusListeners.toArray();
        for (int i = list.length - 1; i >= 0; i--) {
            if (list[i] instanceof SessionStatusEventListener) {
                ((SessionStatusEventListener) list[i]).sessionStatusChanged(session, oldStatus);
            }
        }
    }

    /**
     * Registers all message handlers provided by this class.
     */
    protected void setupMessageMap() {
        this.addMessageHandler(IRCMessageTypes.MSG_JOIN, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                ArrayList chans = JoinMessage.getChannels(e.getMessage());
                for (int i = 0; i < chans.size(); i++) {
                    forwardMessage(e, (String) chans.get(i));
                }
                e.consume();
                fireMessageProcessedEvent(e.getMessage());
            }
        });
        this.addMessageHandler(IRCMessageTypes.MSG_KICK, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                forwardMessage(e, KickMessage.getChannel(e.getMessage()));
                if (KickMessage.getUser(e.getMessage()).equals(user.getNickName())) {
                    removeChannel(KickMessage.getChannel(e.getMessage()));
                }
            }
        });
        this.addMessageHandler(IRCMessageTypes.MSG_NICK, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                if (e.getMessage().getNick().equals(user.getNickName())) {
                    user.setNickName(NickMessage.getNickname(e.getMessage()));
                }
                forwardMessage(e, null);
            }
        });
        this.addMessageHandler(IRCMessageTypes.MSG_NOTICE, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                MessageFormat f = new MessageFormat("[{0}] {1}");
                try {
                    Object[] o = f.parse(NoticeMessage.getText(e.getMessage()));
                    if (IRCUtils.isChannel(o[0].toString())) {
                        forwardMessage(e, o[0].toString());
                        e.consume();
                        fireMessageProcessedEvent(e.getMessage());
                        return;
                    }
                } catch (ParseException exc) {
                }
                if (e.getMessage().getNick().equals("ChanServ") || e.getMessage().getNick().equals("NickServ")) {
                    forwardMessage(e, e.getMessage().getNick());
                    e.consume();
                }
            }
        });
        this.addMessageHandler(IRCMessageTypes.MSG_PART, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                ArrayList chans = PartMessage.getChannels(e.getMessage());
                for (int i = 0; i < chans.size(); i++) {
                    forwardMessage(e, (String) chans.get(i));
                    if (e.getMessage().getNick().equals(user.getNickName())) {
                        removeChannel((String) chans.get(i));
                    }
                }
                e.consume();
                fireMessageProcessedEvent(e.getMessage());
            }
        });
        this.addMessageHandler(IRCMessageTypes.MSG_PING, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                conn.send(PongMessage.createMessage("", "", "", PingMessage.getServer1(e.getMessage())));
                e.consume();
                fireMessageProcessedEvent(e.getMessage());
            }
        });
        this.addMessageHandler(IRCMessageTypes.MSG_PRIVMSG, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                if (IRCUtils.isChannel(PrivateMessage.getMsgtarget(e.getMessage()))) {
                    forwardMessage(e, PrivateMessage.getMsgtarget(e.getMessage()));
                } else {
                    if (CTCPMessage.isCTCPMessage(PrivateMessage.getText(e.getMessage()))) {
                        return;
                    } else {
                        forwardMessage(e, e.getMessage().getNick());
                    }
                }
                e.consume();
                fireMessageProcessedEvent(e.getMessage());
            }
        });
        this.addMessageHandler(IRCMessageTypes.MSG_QUIT, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                forwardMessage(e, null);
                e.consume();
                fireMessageProcessedEvent(e.getMessage());
            }
        });
        this.addMessageHandler(IRCMessageTypes.MSG_TOPIC, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                forwardMessage(e, TopicMessage.getChannel(e.getMessage()));
                e.consume();
                fireMessageProcessedEvent(e.getMessage());
            }
        });
        this.addMessageHandler(IRCMessageTypes.ERR_NOSUCHNICK, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                String channel = NosuchnickError.getNickname(e.getMessage());
                if (getChannel(channel) != null) {
                    forwardMessage(e, channel);
                    e.consume();
                    fireMessageProcessedEvent(e.getMessage());
                }
            }
        });
        this.addMessageHandler(IRCMessageTypes.ERR_NOSUCHCHANNEL, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                String channel = NosuchchannelError.getChannelname(e.getMessage());
                if (getChannel(channel) != null) {
                    forwardMessage(e, channel);
                    e.consume();
                    fireMessageProcessedEvent(e.getMessage());
                }
            }
        });
        this.addMessageHandler(IRCMessageTypes.ERR_NICKNAMEINUSE, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                if (isChangeingNick() == false) {
                    disconnect();
                    e.consume();
                    fireMessageProcessedEvent(e.getMessage());
                    reconnect = true;
                }
            }
        });
        this.addMessageHandler(IRCMessageTypes.ERR_NICKCOLLISION, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                disconnect();
                e.consume();
                fireMessageProcessedEvent(e.getMessage());
                reconnect = true;
            }
        });
        this.addMessageHandler(IRCMessageTypes.RPL_WELCOME, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                String str = WelcomeReply.getNick(e.getMessage());
                if (user.getNickName().startsWith(str)) {
                    user.setNickName(str);
                }
                if (conn.isActive()) {
                    conn.setStatus(SessionStatus.CONNECTED);
                }
            }
        });
        this.addMessageHandler(IRCMessageTypes.RPL_AWAY, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                forwardMessage(e, AwayReply.getNick(e.getMessage()));
                e.consume();
                fireMessageProcessedEvent(e.getMessage());
            }
        });
        this.addMessageHandler(IRCMessageTypes.RPL_TOPIC, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                forwardMessage(e, TopicReply.getChannel(e.getMessage()));
                e.consume();
                fireMessageProcessedEvent(e.getMessage());
            }
        });
        this.addMessageHandler(IRCMessageTypes.RPL_TOPICSET, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                forwardMessage(e, TopicsetReply.getChannel(e.getMessage()));
                e.consume();
                fireMessageProcessedEvent(e.getMessage());
            }
        });
        this.addMessageHandler(IRCMessageTypes.RPL_NAMREPLY, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                String channel = NamesReply.getChannel(e.getMessage());
                if (getChannel(channel) != null) {
                    forwardMessage(e, channel);
                    e.consume();
                    fireMessageProcessedEvent(e.getMessage());
                }
            }
        });
        this.addMessageHandler(IRCMessageTypes.RPL_ENDOFNAMES, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                String channel = EndofnamesReply.getChannel(e.getMessage());
                if (getChannel(channel) != null) {
                    forwardMessage(e, channel);
                    e.consume();
                    fireMessageProcessedEvent(e.getMessage());
                }
            }
        });
    }
}
