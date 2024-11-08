package org.timothyb89.jtelirc.user;

import java.util.ArrayList;
import java.util.List;
import org.timothyb89.jtelirc.admin.AdminList;
import org.timothyb89.jtelirc.channel.Channel;
import org.timothyb89.jtelirc.filter.FilterSet;
import org.timothyb89.jtelirc.filter.base.DefaultFilter;
import org.timothyb89.jtelirc.listener.MessageListener;
import org.timothyb89.jtelirc.message.Message;
import org.timothyb89.jtelirc.message.UserMessage;
import org.timothyb89.jtelirc.server.Server;
import org.timothyb89.jtelirc.util.ListUtil;

/**
 * Manages user information.
 * For the most part, changes are handled automatically (such as nick changing).
 *
 * @author timothyb89
 */
public class User implements MessageListener {

    /**
	 * The server this User is on.
	 */
    private Server server;

    /**
	 * The current user ID.
	 * This should also be the number of users known to the bot.
	 */
    private static int CID = 0;

    /**
	 * The raw text of this user.
	 */
    private String raw;

    /**
	 * The nick of this user
	 */
    private String nick;

    /**
	 * The username of the user
	 */
    private String user;

    /**
	 * This user's host.
	 */
    private String host;

    /**
	 * This user's ID.
	 */
    private int id;

    /**
	 * The list of listeners for this user.
	 */
    private List<UserListener> listeners;

    /**
	 * Constructs a new User.
	 * @param server The server this user is on.
	 * @param raw The raw text of the user.
	 */
    public User(Server server, String raw) {
        this.server = server;
        this.raw = raw;
        server.addMessageListener(this);
        listeners = new ArrayList<UserListener>();
        id = CID;
        CID++;
        parse();
    }

    /**
	 * Constructs a user with the given parameters.
	 * @param server The server this user is on.
	 * @param nick The nick
	 * @param user The user string
	 * @param host The hostname
	 */
    public User(Server server, String nick, String user, String host) {
        this.server = server;
        this.nick = nick;
        this.user = user;
        this.host = host;
        server.addMessageListener(this);
        listeners = new ArrayList<UserListener>();
        id = CID;
        CID++;
        buildRaw();
    }

    /**
	 * Parses the user string.
	 */
    private void parse() {
        try {
            String[] spraw = raw.split("@", 2);
            host = spraw[1];
            String[] spu = spraw[0].split("!");
            nick = spu[0];
            user = spu[1];
        } catch (Exception ex) {
            System.err.println("Failed parsing user! (raw=" + raw + ")");
            ex.printStackTrace();
        }
    }

    /**
	 * Adds a UserListener to the notification list.
	 * @param l The listener to add
	 */
    public void addListener(UserListener l) {
        listeners.add(l);
    }

    /**
	 * Removes a UserListener from the notification list.
	 * @param l The listener to remove.
	 */
    public void removeListener(UserListener l) {
        listeners.remove(l);
    }

    /**
	 * Gets the hostname of this user
	 * @return The hostname
	 */
    public String getHost() {
        return host;
    }

    /**
	 * Gets the nick of this user
	 * @return The nick
	 */
    public String getNick() {
        return nick;
    }

    /**
	 * Gets the raw text used to construct this message.
	 * If this User was constructed with the nick, user, and host parameters,
	 * this returns an artifically constructed version:
	 *		<code>raw = nick + "!" + user + "@" + host;</code>
	 * @return The raw text, or the full user string.
	 */
    public String getRaw() {
        return raw;
    }

    /**
	 * Gets the username of this User (nick!<b>user</b>@host)
	 * @return The username of this User.
	 */
    public String getUser() {
        return user;
    }

    /**
	 * Rebuilds and sets the raw text of this user.
	 * This is used when processing nick changes.
	 * @return The built raw text
	 */
    public String buildRaw() {
        return (raw = nick + "!" + user + "@" + host);
    }

    /**
	 * Processes a nick change, but setting and rebuilding the nick of this user.
	 * This is called automatically, and should not be needed.
	 * TODO: Notify listeners of nick change.
	 * @param newNick The new nick of the user
	 */
    public void processNickChange(String newNick) {
        nick = newNick;
        buildRaw();
    }

    /**
	 * Returns true when this user is an admin according to AdminList.isAdmin():
	 *		<code>AdminList.isAdmin(this);</code>
	 * @return True if this user is an admin.
	 */
    public boolean isAdmin() {
        return AdminList.get().isAdmin(this);
    }

    /**
	 * Gets the ID of this user.
	 * The ID is just a counter for how many users have been logged.
	 * @return The ID of this user
	 */
    public int getID() {
        return id;
    }

    /**
	 * Sends a message to this user.
	 * @param text The text to send.
	 */
    public void sendMessage(String text) {
        server.sendMessage(getNick(), text);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final User other = (User) obj;
        if ((this.raw == null) ? (other.raw != null) : !this.raw.equals(other.raw)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.raw != null ? this.raw.hashCode() : 0);
        return hash;
    }

    /**
	 * Called to notify listeners of a message received from this user.
	 * This is only called for PRIVMSGs
	 * @param m The message received.
	 */
    protected void notifyMessageReceived(UserMessage m) {
        for (UserListener l : listeners) {
            l.onUserMessageReceived(this, m);
        }
    }

    /**
	 * Called to notify listeners of this user joining a channel the client is
	 * currently in.
	 * @param c The channel joined.
	 */
    protected void notifyJoined(Channel c) {
        for (UserListener l : listeners) {
            l.onUserJoinedChannel(this, c);
        }
    }

    /**
	 * Called to notify listeners of this user leaving a channel the client is
	 * currently in.
	 * @param c The channel left
	 * @param text The part message
	 */
    protected void notifyParted(Channel c, String text) {
        for (UserListener l : listeners) {
            l.onUserLeftChannel(this, c, text);
        }
    }

    /**
	 * Called to notify listeners of this user leaving the server.
	 * @param text The quit message.
	 */
    protected void notifyQuit(String text) {
        for (UserListener l : listeners) {
            l.onUserQuit(this, text);
        }
    }

    /**
	 * Called when any message is received. This only handles UserMessages sent
	 * from this user; all others are ignored.
	 * @param m The message received.
	 */
    public void onMessageReceived(Message m) {
        if (m instanceof UserMessage) {
            UserMessage message = (UserMessage) m;
            String cmd = message.getCommand();
            if (message.getUser().equals(this)) {
                if (cmd.equalsIgnoreCase("privmsg")) {
                    notifyMessageReceived(message);
                } else if (cmd.equalsIgnoreCase("join")) {
                    notifyJoined(message.getChannel());
                } else if (cmd.equalsIgnoreCase("part")) {
                    notifyParted(message.getChannel(), message.getText());
                } else if (cmd.equalsIgnoreCase("quit")) {
                    notifyQuit(message.getText());
                }
            }
        }
    }

    /**
	 * The list of filters for this message listener.
	 * This accepts all messages.
	 * @return The list of filters for this listener (single DefaultFilter).
	 */
    public List<FilterSet> getFilters() {
        return ListUtil.singleFilterSet(new DefaultFilter());
    }
}
