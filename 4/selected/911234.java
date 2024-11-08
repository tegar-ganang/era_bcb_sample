package org.speakmon.coffeehouse;

import java.util.Date;

/**
 * This class models an IRC user. It stores the user's nick,
 * channel status, modes, signon date, and other relevant information,
 * all of which is accessible through get() methods. New users are
 * created with nicks.
 *
 * <P>This class makes a distinction between a user's nick and his visible
 * (or plain) nick. A user's nick may be preceded by '@' or '+' if he is opped
 * or voiced for the particular channel he's in. Thus, if a user who happens
 * to be a chanop for the current channel is created with the statememt:
 * <P><code>User user = new User("op");</code>
 * <P>then <code>user.isOp()</code> will return false because the object has no way of
 * knowing that the user is a chanop. To fix this, use the <code>setOp()</code>
 * method, or create the user object with the following statement:
 * <P><code>User user = new User("@op");</code>
 * <P>A user's visible nick is his nick without any prefix, regardless of his actual
 * mode in the current channel. A user's nick includes a mode prefix if applicable.
 * <P><code>new User("@op").getNick()</code> returns <code>@op</code><br>
 *    <code>new User("@op").getVisibleNick()</code> returns <code>op</code>
 *
 * <P>A user may have different
 * privileges in each channel he's in, so the same person in two channels
 * is represented by two distinct <code>User</code> objects.
 *
 * <P>This class implements the <code>Comparable</code> interface and overrides
 * <code>Object.equals()</code> and <code>Object.hashCode()</code>. See the documentation
 * for <code>equals()</code> and <code>compareTo()</code> for definitions of what makes
 * two <code>User</code> objects equal and for the natural ordering of <code>User</code> objects.
 *
 * Created: Sat Dec 14 00:11:29 2002
 *
 * @author <a href="mailto:ben@speakmon.org">Ben Speakmon</a>
 * @version 1.0
 */
public final class User implements Comparable {

    private String nick;

    private ServerListener channel;

    private String username;

    private String hostname;

    private String servername;

    private String visibleNick;

    private boolean isAway;

    private boolean isOp;

    private boolean isVoiced;

    private String realname;

    private boolean isInvisible;

    private Date signOn;

    private Date lastMessage;

    /**
     * Creates a new <code>User</code> instance with the specified nick.
     *
     * @param nick a <code>String</code> value
     */
    public User(String nick) {
        this.nick = nick;
        signOn = new Date();
        setSpecialMode(nick);
    }

    /**
     * Creates a new <code>User</code> instance with the specified nick
     * and the specified signon time.
     *
     * @param nick a <code>String</code> value
     * @param signOn a <code>Date</code> value
     */
    public User(String nick, Date signOn) {
        this.nick = nick;
        this.signOn = signOn;
        setSpecialMode(nick);
    }

    /**
     * Creates a new <code>User</code> instance, initialized with the values
     * present in the <code>User</code> parameter object.
     *
     * @param user an <code>User</code> value
     */
    public User(User user) {
        this.nick = user.getNick();
        this.visibleNick = user.getVisibleNick();
        this.username = user.getUsername();
        this.signOn = user.getSignOn();
        this.isOp = user.isOp();
        this.isVoiced = user.isVoiced();
        this.channel = user.getChannel();
        this.hostname = user.getHostname();
        this.servername = user.getServername();
        this.isAway = user.isAway();
        this.realname = user.getRealname();
    }

    public User(String[] initData) {
        username = initData[0];
        hostname = initData[1];
        servername = initData[2];
        visibleNick = initData[3];
        if (initData[4].equals("G")) {
            isAway = true;
        } else if (initData[4].equals("H")) {
            isAway = false;
        }
        if (initData[5].equals("+")) {
            isVoiced = true;
            nick = initData[5] + visibleNick;
        } else if (initData[5].equals("@")) {
            isOp = true;
            nick = initData[5] + visibleNick;
        } else {
            nick = visibleNick;
        }
        realname = initData[6];
        signOn = new Date();
    }

    private void setSpecialMode(String nick) {
        char c = nick.charAt(0);
        switch(c) {
            case '@':
                visibleNick = nick.substring(1);
                isOp = true;
                break;
            case '+':
                visibleNick = nick.substring(1);
                isVoiced = true;
                break;
            default:
                visibleNick = nick;
                break;
        }
    }

    /**
     * Gets whether this user is opped.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isOp() {
        return isOp;
    }

    /**
     * Sets whether this user is opped.
     *
     * @param isOp a <code>boolean</code> value
     */
    public void setOp(boolean isOp) {
        if (this.isOp == isOp) {
            return;
        }
        this.isOp = isOp;
        if (isOp) {
            if (isVoiced) {
                nick = "@" + nick.substring(1);
                visibleNick = nick.substring(1);
                isVoiced = false;
            } else {
                nick = "@" + nick;
                visibleNick = nick.substring(1);
            }
        } else {
            nick = nick.substring(1);
            visibleNick = nick;
        }
    }

    /**
     * Gets whether this user is voiced.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isVoiced() {
        return isVoiced;
    }

    /**
     * Sets whether this user is voiced.
     *
     * @param isVoiced a <code>boolean</code> value
     */
    public void setVoiced(boolean isVoiced) {
        if (this.isVoiced == isVoiced) {
            return;
        }
        if (this.isOp) {
            return;
        }
        this.isVoiced = isVoiced;
        if (isVoiced) {
            nick = "+" + nick;
            visibleNick = nick.substring(1);
        } else {
            nick = nick.substring(1);
            visibleNick = nick;
        }
    }

    /**
     * Returns true if this user is invisible.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isInvisible() {
        return isInvisible;
    }

    /**
     * Sets whether this user is invisible.
     *
     * @param isInvisible a <code>boolean</code> value
     */
    public void setInvisible(boolean isInvisible) {
        this.isInvisible = isInvisible;
    }

    /**
     * Overrides <code>java.lang.object.equals(Object)</code>.
     * <P>Two users are defined to be equal if their visible nicks are 
     * equal:
     * <UL>
     *   <LI><code>new User("@op").equals(new User("+op"))</code> returns true.
     *   <LI><code>new User("@op").equals(new User("+op2"))</code> returns false.
     * </UL>
     *
     * @param obj an <code>Object</code> value of runtime type 'User'.
     * @return a <code>boolean</code> value
     */
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if (this == obj) {
            return true;
        }
        if (obj instanceof User) {
            User user = (User) obj;
            if (this.getVisibleNick().equals(user.getVisibleNick())) {
                isEqual = true;
            }
        }
        return isEqual;
    }

    /**
     * Overrides <code>java.lang.Object.hashCode()</code>.
     *
     * @return an <code>int</code> value
     */
    public int hashCode() {
        return visibleNick.hashCode();
    }

    /**
     * Gets the nick for this user.
     *
     * @return a <code>String</code> value
     */
    public String getNick() {
        return nick;
    }

    /**
     * Sets the nick for this user.
     *
     * @param nick a <code>String</code> value
     */
    public void setNick(String nick) {
        this.nick = nick;
        setSpecialMode(nick);
    }

    /**
     * Gets the visible nick for this user (i.e., without '@' or '+',
     * even if this user is opped or voiced.)
     *
     * @return a <code>String</code> value
     */
    public String getVisibleNick() {
        return visibleNick;
    }

    /**
     * Sets the visible nick for this user (i.e., without '@' or '+',
     * even if this user is opped or voiced.)
     *
     * @param visibleNick a <code>String</code> value
     */
    public void setVisibleNick(String visibleNick) {
        this.visibleNick = visibleNick;
    }

    /**
     * Gets the username for this user.
     *
     * @return a <code>String</code> value
     */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Get the Hostname value.
     * @return the Hostname value.
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Set the Hostname value.
     * @param newHostname The new Hostname value.
     */
    public void setHostname(String newHostname) {
        this.hostname = newHostname;
    }

    /**
     * Get the Servername value.
     * @return the Servername value.
     */
    public String getServername() {
        return servername;
    }

    /**
     * Set the Servername value.
     * @param newServername The new Servername value.
     */
    public void setServername(String newServername) {
        this.servername = newServername;
    }

    /**
     * Get the isAway value.
     * @return the isAway value.
     */
    public boolean isAway() {
        return isAway;
    }

    /**
     * Set the IsAway value.
     * @param newIsAway The new IsAway value.
     */
    public void setAway(boolean away) {
        isAway = away;
    }

    /**
     * Get the Realname value.
     * @return the Realname value.
     */
    public String getRealname() {
        return realname;
    }

    /**
     * Set the Realname value.
     * @param newRealname The new Realname value.
     */
    public void setRealname(String newRealname) {
        this.realname = newRealname;
    }

    /**
     * Gets the user's IRC signon time.
     *
     * @return a <code>Date</code> value
     */
    public Date getSignOn() {
        return signOn;
    }

    /**
     * Sets the user's IRC signon time.
     *
     * @param signOn a <code>Date</code> value
     */
    public void setSignOn(Date signOn) {
        this.signOn = signOn;
    }

    public ServerListener getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = (ServerListener) channel;
    }

    /**
     * Returns a short description of this object containing the nick.
     *
     * @return a <code>String</code> value
     */
    public String toString() {
        return "'" + nick + "' user object";
    }

    /**
     * Implements <code>java.lang.Comparable.compareTo(Object)</code>.
     * <P>Defines the natural ordering of User objects. Returns -1, 0, or 1
     * according to the general contract of <code>Comparable</code>.
     * <P>The natural ordering of User objects is defined as:<BR>
     * <UL>
     *   <LI>Two references to the same User object are equal.
     *   <LI>Two users that are equal by the User.equals() definition are equal.
     *   <LI>An opped user is always before a non-opped user.
     *   <LI>A voiced user is always before a regular user and always after an opped user.
     *   <LI>A regular user is always after a non-regular user.
     *   <LI>Two opped, voiced, or regular users are ordered by the results of the
     *       String.compareTo() method of their visible nicks.
     * </UL>
     * 
     *
     * @param user an <code>Object</code> value with runtime type <code>User</code>
     * @return -1 if this object's natural ordering is higher than user, 0 if they
     *         have the same natural ordering, 1 if user's natural ordering is higher
     *         than this object.
     */
    public int compareTo(Object user) {
        if (user == null) {
            throw new NullPointerException("compareTo() parameter cannot be null");
        }
        if (user == this) {
            return 0;
        }
        if (user instanceof User) {
            User thisUser = (User) user;
            if (this.equals(thisUser)) {
                return 0;
            }
            if (this.isOp()) {
                if (thisUser.isOp()) {
                    return this.visibleNick.compareToIgnoreCase(thisUser.getVisibleNick());
                } else {
                    return -1;
                }
            } else if (this.isVoiced()) {
                if (thisUser.isOp()) {
                    return 1;
                } else if (thisUser.isVoiced()) {
                    return this.visibleNick.compareToIgnoreCase(thisUser.getVisibleNick());
                } else {
                    return -1;
                }
            } else {
                if (thisUser.isOp() || thisUser.isVoiced()) {
                    return 1;
                } else {
                    return this.visibleNick.compareToIgnoreCase(thisUser.getVisibleNick());
                }
            }
        } else {
            throw new ClassCastException("cannot compare type " + user.getClass().getName() + " to type 'User'");
        }
    }
}
