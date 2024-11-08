package org.timothyb89.jtelirc.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.timothyb89.jtelirc.config.Configuration;
import org.timothyb89.jtelirc.filter.FilterSet;
import org.timothyb89.jtelirc.filter.base.DefaultFilter;
import org.timothyb89.jtelirc.listener.MessageListener;
import org.timothyb89.jtelirc.message.Message;
import org.timothyb89.jtelirc.message.SentMessage;
import org.timothyb89.jtelirc.message.ServerMessage;
import org.timothyb89.jtelirc.message.UserMessage;
import org.timothyb89.jtelirc.server.Server;
import org.timothyb89.jtelirc.user.User;
import org.timothyb89.jtelirc.util.ListUtil;

/**
 * Represents a Channel on an IRC server. This handles channel operations and
 * notification.
 * <p>The size of the log can be configured globally via the property
 * PROPERTY_LOG_SIZE, or specifically by appending the channel name to that
 * property. The size can also be set by calling the setLogSize(int) method.</p>
 * @author timothyb89
 */
public class Channel implements MessageListener {

    /**
	 * The default size of the log if no configuration property is found or
	 * none are valid.
	 */
    public static final int DEFAULT_LOG_SIZE = 25;

    /**
	 * The property of the log size.
	 */
    public static final String PROPERTY_LOG_SIZE = "log size";

    /**
	 * A list of channel modes with parameters.
	 */
    public static final String PARAM_MODES = "ohvlb";

    /**
	 * The name of the channel.
	 */
    private String name;

    /**
	 * The channel's current topic.
	 */
    private String topic;

    /**
	 * The list of modes this channel has.
	 */
    private List<String> modes;

    /**
	 * The server instance.
	 */
    private Server server;

    /**
	 * The list of users in the channel.
	 */
    private List<User> users;

    /**
	 * A list of users with oper status (+o, '@')
	 */
    private List<User> operators;

    /**
	 * A list of users with halfop status (+h, '%')
	 */
    private List<User> halfops;

    /**
	 * A list of users with voice (+v, '+') access
	 */
    private List<User> voiced;

    /**
	 * The list of ChannelListeners attached to this Channel.
	 */
    private List<ChannelListener> listeners;

    /**
	 * The log containing a list of the last messages received.
	 */
    private List<Message> log;

    /**
	 * The size of the log for this channel.
	 */
    private int logSize;

    /**
	 * Constructs a new Channel.
	 * This does *not* join the channel, or collect any information on it.
	 * @param name The name of the channel, for example: "#chat"
	 * @param server The server
	 */
    public Channel(String name, Server server) {
        this.name = name;
        this.server = server;
        modes = new ArrayList<String>();
        users = new ArrayList<User>();
        operators = new ArrayList<User>();
        halfops = new ArrayList<User>();
        voiced = new ArrayList<User>();
        listeners = new ArrayList<ChannelListener>();
        initLog(server);
        server.addMessageListener(new WhoListener(this));
        server.addMessageListener(this);
    }

    private void initLog(Server server) {
        log = new ArrayList<Message>();
        Integer size = null;
        Configuration conf = server.getConfiguration();
        String key = PROPERTY_LOG_SIZE + " " + name;
        if (conf.hasProperty(key)) {
            size = conf.getInt(key);
        } else if (conf.hasProperty(PROPERTY_LOG_SIZE)) {
            size = conf.getInt(PROPERTY_LOG_SIZE);
        }
        if (size == null) {
            logSize = DEFAULT_LOG_SIZE;
        } else {
            logSize = size;
        }
    }

    /**
	 * Gets the list of user in this channel
	 * @return The list of users
	 */
    public List<User> getUsers() {
        return users;
    }

    /**
	 * Gets the user in this channel with the given nick. Roughly equivalent to
	 * UserList.getUser()
	 * @param nick The nickname of the user to get
	 * @return A user with the specified nickname, or null if none are found.
	 */
    public User getUser(String nick) {
        for (User u : getUsers()) {
            if (u.getNick().equalsIgnoreCase(nick)) {
                return u;
            }
        }
        return null;
    }

    /**
	 * Returns the user associated with the bot
	 * @return This user
	 */
    public User getUser() {
        return getUser(server.getNick());
    }

    /**
	 * Registers a user in this channel to this client.
	 * This user should already be known in the UserList.
	 * @param user The user to add
	 */
    public void addUser(User user) {
        if (!users.contains(user)) {
            users.add(user);
            notifyUserJoined(user);
        }
    }

    /**
	 * Removes a user from this channel. This removes the user from the local
	 * channel's list of users, not from the sever or the UserList.
	 * @param user The user to remove.
	 * @param partMessage The part or quit message.
	 * @param quit True if the user disconnected
	 */
    public void removeUser(User user, String partMessage, boolean quit) {
        users.remove(user);
        notifyUserLeft(user, partMessage, quit);
    }

    /**
	 * Checks if this channel contains the provided user
	 * @param user The user
	 * @return True if the user is in the channel
	 */
    public boolean hasUser(User user) {
        for (User u : getUsers()) {
            if (u.getNick().equals(user.getNick())) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Checks if this channel contains the user with the provided nick
	 * @param nick The nick to check for
	 * @return True if this channel contains a user with the given nick
	 */
    public boolean hasUser(String nick) {
        for (User u : getUsers()) {
            if (u.getNick().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Randomly chooses a user.
	 * @return a randomly-chosen user from this channel.
	 */
    public User getRandomUser() {
        List<User> ucopy = new ArrayList<User>();
        ucopy.addAll(getUsers());
        ucopy.remove(server.getUserList().getUserByNick(server.getNick()));
        int index = new Random().nextInt(ucopy.size());
        return ucopy.get(index);
    }

    /**
	 * Gets the list of users in this channel with oper status (+o, '@').
	 * @return a list of channel operators
	 */
    public List<User> getOperators() {
        return operators;
    }

    /**
	 * Checks if the given user is a channel operator.
	 * @param u The user to check
	 * @return True if the user has +o status, false if not.
	 */
    public boolean isOperator(User u) {
        return operators.contains(u);
    }

    /**
	 * Checks if this user is a channel operator.
	 * @return True if this user is an operator, false if not.
	 */
    public boolean isOperator() {
        return isOperator(getUser());
    }

    /**
	 * Gets the list of users in this channel with halfop status (+h, '%'). Note
	 * that this list will be empty if halfops are not supported on the server.
	 * @return a list of halfops
	 */
    public List<User> getHalfops() {
        return halfops;
    }

    /**
	 * Checks if the given user is a channel half-operator.
	 * @param u The user to check
	 * @return True if the user is a halfop, false otherwise.
	 */
    public boolean isHalfop(User u) {
        return halfops.contains(u);
    }

    /**
	 * Checks if this user it a halfop
	 * @return true if this user is a halfop, false otherwise
	 */
    public boolean isHalfop() {
        return isHalfop(getUser());
    }

    /**
	 * Gets the list of users in this channel with voice (+v, '+')
	 * @return a list of users with voice
	 */
    public List<User> getVoiced() {
        return voiced;
    }

    /**
	 * Checks if the given user has voice in this channel (+v). Note that
	 * operators and halfoperators always have voice, and this method does not
	 * account for that.
	 * @param u The user to check
	 * @return True if the user has voice, false if not.
	 */
    public boolean isVoiced(User u) {
        return voiced.contains(u);
    }

    /**
	 * Checks if this user is voiced
	 * @return True if so, false otherwise
	 */
    public boolean isVoiced() {
        return isVoiced(getUser());
    }

    /**
	 * The name of this channel
	 * @return The name of the channel
	 */
    public String getName() {
        return name;
    }

    /**
	 * Gets the list of channel modes
	 * @return the list of channel modes.
	 */
    public List<String> getModes() {
        return modes;
    }

    /**
	 * Checks if this channel has the given mode.
	 * @param symbol The symbol to check for
	 * @return True if so, false otherwise
	 */
    public boolean hasMode(String symbol) {
        return modes.contains(symbol);
    }

    /**
	 * Checks if this channel has the given mode.
	 * @param mode The mode to check for
	 * @return True if this channel has the given mode, false otherwise.
	 */
    public boolean hasMode(Mode mode) {
        return hasMode(mode.getSymbol());
    }

    /**
	 * Gets the server instance
	 * @return The server instance
	 */
    public Server getServer() {
        return server;
    }

    /**
	 * Joins the channel.
	 * This sends a "JOIN", then a "WHO" to get information about the users.
	 */
    public void join() {
        server.sendQueue("JOIN " + name);
        server.sendQueue("WHO " + name);
        server.sendQueue("MODE " + name);
    }

    /**
	 * Sends a message to this channel.
	 * @param text The text to send
	 * @throws ChannelException when lacking permission to send the message.
	 */
    public void sendMessage(String text) throws ChannelException {
        if (hasMode(Mode.MODERATED)) {
            if (!isVoiced() && !isHalfop() && !isOperator()) {
                throw new ChannelException("Not allowed to send messages.");
            }
        }
        server.sendMessage(getName(), text);
    }

    /**
	 * Sends a message to this channel, addressing the given user.
	 * @param u The user to address
	 * @param text The text to send
	 * @throws ChannelException when lacking permission to send the message.
	 */
    public void sendMessage(User u, String text) throws ChannelException {
        sendMessage(u.getNick() + ": " + text);
    }

    /**
	 * Sends a notice to this channel.
	 * @param text The text to send.
	 */
    public void sendNotice(String text) {
        server.sendNotice(getName(), text);
    }

    /**
	 * Adds a listener to the channel.
	 * @param l The listener to add
	 */
    public void addListener(ChannelListener l) {
        listeners.add(l);
    }

    /**
	 * Removes a listener from the channel. Notification of events will stop.
	 * @param l The listener to remove
	 */
    public void removeListener(ChannelListener l) {
        listeners.remove(l);
    }

    /**
	 * <p>Gets the channel log, containing a number of the last messages
	 * received up to the log size (getLogSize()).<p>
	 * <p>The size of the log may be set using setLogSize()</p>
	 * <p>Note that this log includes things such as channel events in addition
	 * to text messages received.</p>
	 * @return The log of channel messages
	 */
    public List<Message> getLog() {
        return log;
    }

    /**
	 * Gets the (maximum) size of the log. To get the number of entries
	 * currently contained in the log, use getLog().size().
	 * @return The maximum size of the channel log.
	 */
    public int getLogSize() {
        return logSize;
    }

    /**
	 * <p>Sets the (maximum) size of the channel log.</p>
	 * <p>The change is applied immediately if the current log is larger than
	 * the new log size. If the log size increases, no change is made as bounds
	 * checking takes place when a message is received.</p>
	 * <p>If the new log size is zero, all elements are removed and no new
	 * messages are added. This may have a negative impact on functionality,
	 * so use wisely.</p>
	 * @param logSize The new log size
	 */
    public void setLogSize(int logSize) {
        this.logSize = logSize;
        pruneLog();
    }

    /**
	 * Appends the given message to the log and prunes it.
	 * @param message The message to append
	 */
    protected void appendLog(Message message) {
        log.add(message);
        pruneLog();
    }

    /**
	 * Removes elements from the top of the log to make it conform to the log
	 * size.
	 */
    protected void pruneLog() {
        while (log.size() > logSize) {
            log.remove(0);
        }
    }

    /**
	 * Gets the topic
	 * @return The topic
	 */
    public String getTopic() {
        return topic;
    }

    /**
	 * Checks that the client is currently in this channel.
	 * @return True if joined, false if not
	 */
    public boolean isJoined() {
        return server.getChannels().contains(this);
    }

    /**
	 * Processes a mode change. Note that certain mode changes (ie, h, o, and v)
	 * are separate from the normal channel modes and won't appear in the modes
	 * list. Specifically, ops, halfops, and voiced users appear in their given
	 * lists and not in the modes list.
	 * <p>Note that a potential inconsistency may arise when a user with
	 * halfops is given ops without first removing halfops, or if a user has
	 * both ops and halfops when generating the initial user list. Although
	 * this should be handled correctly afterwards, some issues might be had.
	 * </p>
	 * @param mode The mode changed
	 * @param param A list of parameters, if any
	 * @param notify If true, notifies listeners of the mode change
	 */
    private void internalAddMode(String mode, String param, boolean notify) {
        if (mode.equals("o")) {
            User u = getUser(param);
            if (operators.contains(u)) {
                return;
            }
            operators.add(u);
        } else if (mode.equals("h")) {
            User u = getUser(param);
            if (halfops.contains(u)) {
                return;
            }
            halfops.add(u);
        } else if (mode.equals("v")) {
            User u = getUser(param);
            if (voiced.contains(u)) {
                return;
            }
            voiced.add(u);
        } else {
            if (hasMode(mode)) {
                return;
            }
            modes.add(mode);
        }
        if (notify) {
            notifyModeChanged(mode, param, true);
        }
    }

    /**
	 * Processes a mode change. Note that certain mode changes (ie, h, o, and v)
	 * are separate from the normal channel modes and won't appear in the modes
	 * list. Specifically, ops, halfops, and voiced users appear in their given
	 * lists and not in the modes list.
	 * @param mode The mode changed
	 * @param param A list of parameters, if any
	 * @param notify if true, notifies listeners about the mode change
	 */
    private void internalRemoveMode(String mode, String param, boolean notify) {
        if (mode.equals("o")) {
            User u = getUser(param);
            operators.remove(u);
        } else if (mode.equals("h")) {
            User u = getUser(param);
            halfops.remove(u);
        } else if (mode.equals("v")) {
            User u = getUser(param);
            voiced.remove(u);
        } else {
            modes.remove(mode);
        }
        if (notify) {
            notifyModeChanged(mode, param, false);
        }
    }

    /**
	 * Processes a mode string with optional parameters.
	 * @param modes The mode string
	 * @param params A (possibly empty) list of parameters.
	 * @param notify if true, notifies listeners of the mode change
	 */
    private void processModes(String modes, List<String> params, boolean notify) {
        List<String> added = getAddedModes(modes);
        List<String> removed = getRemovedModes(modes);
        List<String> keys = getParameterKeys(modes);
        for (String s : added) {
            if (keys.contains(s)) {
                int paramIndex = keys.indexOf(s);
                if (paramIndex <= params.size() - 1) {
                    String param = params.get(paramIndex);
                    internalAddMode(s, param, notify);
                } else {
                    internalAddMode(s, null, notify);
                }
            } else {
                internalAddMode(s, null, notify);
            }
        }
        for (String s : removed) {
            if (keys.contains(s)) {
                int paramIndex = keys.indexOf(s);
                if (paramIndex <= params.size() - 1) {
                    String param = params.get(paramIndex);
                    internalRemoveMode(s, param, notify);
                } else {
                    internalRemoveMode(s, null, notify);
                }
            } else {
                internalRemoveMode(s, null, notify);
            }
        }
    }

    /**
	 * Gets a list of modes contained in the given mode list that should have
	 * parameters attached (such as a, o, v, b, and l).
	 * <p>Note that this really only applies to modes <b>added</b> when the
	 * initial MODE query is sent, as multiple parameters can only apply here
	 * and not in user-triggered MODE messages.</p>
	 * <p>Also note that removed modes will not be handled correctly if their
	 * +form requires a parameter while their -form does not (such as +l).
	 * However, this can be safely ignored.</p>
	 * @param modes The modes string
	 * @return a (possibly empty) list of modes, with order maintained.
	 */
    private List<String> getParameterKeys(String modes) {
        List<String> ret = new ArrayList<String>();
        for (String s : ListUtil.charList(modes)) {
            if (s.equals("+") || s.equals("-")) {
                continue;
            }
            if (PARAM_MODES.contains(s)) {
                ret.add(s);
            }
        }
        return ret;
    }

    /**
	 * Finds the modes added in the given mode string.
	 * @param modes The mode string
	 * @return a list of added modes.
	 */
    private List<String> getAddedModes(String modes) {
        List<String> ret = new ArrayList<String>();
        if (!modes.contains("+")) {
            return ret;
        }
        int startIndex = modes.indexOf("+") + 1;
        int endIndex;
        if (modes.contains("-")) {
            if (modes.indexOf("-") > modes.indexOf("+")) {
                endIndex = modes.indexOf("-");
            } else {
                endIndex = modes.length() - 1;
            }
        } else {
            endIndex = modes.length() - 1;
        }
        for (int i = startIndex; i <= endIndex; i++) {
            ret.add(String.valueOf(modes.charAt(i)));
        }
        return ret;
    }

    private List<String> getRemovedModes(String modes) {
        List<String> ret = new ArrayList<String>();
        if (!modes.contains("-")) {
            return ret;
        }
        int startIndex = modes.indexOf("-") + 1;
        int endIndex;
        if (modes.contains("+")) {
            if (modes.indexOf("+") > modes.indexOf("-")) {
                endIndex = modes.indexOf("+");
            } else {
                endIndex = modes.length() - 1;
            }
        } else {
            endIndex = modes.length() - 1;
        }
        for (int i = startIndex; i <= endIndex; i++) {
            ret.add(String.valueOf(modes.charAt(i)));
        }
        return ret;
    }

    /**
	 * Processes a topic change from the server. Use setTopic() to attempt to
	 * modifiy the channel's topic directly.
	 * @param topic The topic text
	 * @param user The user that changed the topic, if any (may be null).
	 */
    private void internalSetTopic(String topic, User user) {
        this.topic = topic;
        notifyTopicChanged(topic, user);
    }

    /**
	 * Notifies listeners of a new user message arriving in the channel.
	 * This is called at the same time as most events.
	 * @param message The message triggering this event
	 */
    protected void notifyMessageReceived(Message message) {
        appendLog(message);
        for (ChannelListener l : listeners) {
            l.onChannelMessageReceived(this, message);
        }
    }

    /**
	 * Notifies listeners when another client joins this channel
	 * @param user The user that joined
	 */
    protected void notifyUserJoined(User user) {
        for (ChannelListener l : listeners) {
            l.onChannelUserJoined(this, user);
        }
    }

    /**
	 * Notifies listeners when another client parts from this channel, or
	 * disconnects from the server.
	 * @param user The user that parted
	 * @param message The message sent
	 * @param quit Should be true if the user quit from the server, false if
	 *		parted.
	 */
    protected void notifyUserLeft(User user, String message, boolean quit) {
        for (ChannelListener l : listeners) {
            l.onChannelUserLeft(this, user, message, quit);
        }
    }

    /**
	 * Notifies listeners of a topic change.
	 * If the topic on a channel is already set, this should also be called
	 * when joining a channel.
	 * @param topic The topic
	 * @param user The user that changed the topic, if any.
	 */
    protected void notifyTopicChanged(String topic, User user) {
        for (ChannelListener l : listeners) {
            l.onChannelTopicChanged(this, topic, user);
        }
    }

    /**
	 * Notifies listeners that a mode has been modified.
	 * @param mode The mode changed
	 * @param param The parameter given with the mode change, if any.
	 * @param state The new state of the mode (true for added, false for removed)
	 */
    protected void notifyModeChanged(String mode, String param, boolean state) {
        for (ChannelListener l : listeners) {
            l.onChannelModeChanged(this, mode, param, state);
        }
    }

    /**
	 * Called when a new message has been received.
	 * @param m The message
	 */
    public void onMessageReceived(Message m) {
        if (m instanceof UserMessage) {
            UserMessage message = (UserMessage) m;
            String destination = message.getDestination();
            String command = message.getCommand().toLowerCase();
            if (destination != null && destination.equals(getName())) {
                if (command.equals("part")) {
                    if (message.getUser().getNick().equals(server.getNick())) {
                        server.notifyChannelParted(this, message.getText());
                    } else {
                        removeUser(message.getUser(), message.getText(), false);
                    }
                } else if (command.equals("topic")) {
                    internalSetTopic(message.getText(), message.getUser());
                } else if (command.equals("mode")) {
                    List<String> strs = ListUtil.safeSplit(message.getText(), " ");
                    if (strs.size() == 0) {
                        return;
                    }
                    String modeStr = strs.get(0);
                    strs.remove(0);
                    processModes(modeStr, strs, true);
                }
                notifyMessageReceived(message);
            } else {
                if (command.equals("join")) {
                    if (message.getText().equals(getName())) {
                        if (message.getUser().getNick().equals(server.getNick())) {
                            server.notifyChannelJoined(this);
                        } else {
                            addUser(message.getUser());
                        }
                        notifyMessageReceived(message);
                    }
                } else if (message.getCommand().equalsIgnoreCase("quit")) {
                    if (hasUser(message.getUser())) {
                        removeUser(message.getUser(), message.getText(), true);
                        notifyMessageReceived(m);
                    }
                }
            }
        } else if (m instanceof ServerMessage) {
            ServerMessage message = (ServerMessage) m;
            List<String> args = message.getArguments();
            if (message.getCommand() == 332) {
                if (args.get(1).equals(getName())) {
                    internalSetTopic(message.getText(), null);
                    appendLog(m);
                }
            } else if (message.getCommand() == 324) {
                if (args.get(1).equals(getName())) {
                    String rawModes = message.getArgument(2);
                    List<String> params = new ArrayList<String>();
                    if (args.size() > 3) {
                        params.addAll(args.subList(3, args.size()));
                    }
                    processModes(rawModes, params, false);
                }
            }
        } else if (m instanceof SentMessage) {
            SentMessage message = (SentMessage) m;
            if (message.getDestination() != null && message.getDestination().equals(getName())) {
                if (message.getCommand().equalsIgnoreCase("privmsg")) {
                    notifyMessageReceived(m);
                }
            }
        }
    }

    /**
	 * Gets the list of filters. This only contains a single filter
	 * (DefaultFilter), so this should be notified of all messages.
	 * @return A list of FilterSets.
	 */
    public List<FilterSet> getFilters() {
        return ListUtil.singleFilterSet(new DefaultFilter());
    }

    /**
	 * Attempts to kick the given user. Note that this requires privileges in
	 * the channel (ie, operator status).
	 * TODO:
	 * @param user The user to kick
	 * @param reason The reason for kicking, can be null.
	 * @throws ChannelException If not allowed to kick
	 */
    public void kick(User user, String reason) throws ChannelException {
        if (!isHalfop() && !isOperator()) {
            throw new ChannelException("Must be an operator to kick.");
        }
        String text = "KICK " + getName() + " " + user.getNick();
        if (reason != null) {
            text += " :" + reason;
        }
        server.sendQueue(text);
    }

    /**
	 * Kicks the given user without reason. Note that this requires privileges
	 * in the channel (ie, operator status).
	 * @param user The user to kick
	 * @throws ChannelException If not allowed to kick
	 */
    public void kick(User user) throws ChannelException {
        kick(user, null);
    }
}
