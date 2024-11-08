package net.sf.babble;

/**
 * This class models the response from a WHOIS command.
 * @version $Id: WhoisInfo.java 219 2004-07-08 07:51:59Z speakmon $
 * @author Ben Speakmon
 */
public final class WhoisInfo {

    /** Holds value of property userInfo. */
    private UserInfo userInfo;

    /** Holds value of property realName. */
    private String realName;

    /** Holds value of property server. */
    private String server;

    /** Holds value of property serverDescription. */
    private String serverDescription;

    /** Holds value of property idleTime. */
    private long idleTime;

    /** Holds value of property isOperator. */
    private boolean isOperator;

    /**
     * Holds value of property channels.
     */
    private String[] channels;

    /**
     * Creates a new instance of WhoisInfo.
     */
    protected WhoisInfo() {
    }

    /**
     * Returns the <code>UserInfo</code> object for this instance.
     * @return the <code>UserInfo</code> object for this instance
     */
    public UserInfo getUserInfo() {
        return this.userInfo;
    }

    /**
     * Sets the <code>UserInfo</code> object for this instance.
     * @param userInfo the <code>UserInfo</code> object for this instance
     * @see UserInfo
     */
    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    /**
     * Returns the real name.
     * @return the real name
     */
    public String getRealName() {
        return this.realName;
    }

    /**
     * Sets the real name.
     * @param realName the real name
     */
    public void setRealName(String realName) {
        this.realName = realName;
    }

    /**
     * Returns the server name.
     * @return the server name
     */
    public String getServer() {
        return this.server;
    }

    /**
     * Sets the server name.
     * @param server the server name
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * Returns the server description.
     * @return the server description
     */
    public String getServerDescription() {
        return this.serverDescription;
    }

    /**
     * Sets the server description.
     * @param serverDescription the server description
     */
    public void setServerDescription(String serverDescription) {
        this.serverDescription = serverDescription;
    }

    /**
     * Returns the idle time as the number of seconds since epoch.
     * @return the idle time
     */
    public long getIdleTime() {
        return this.idleTime;
    }

    /**
     * Sets the idle time.
     * @param idleTime the idle time in seconds since epoch
     */
    public void setIdleTime(long idleTime) {
        this.idleTime = idleTime;
    }

    /**
     * Returns true if this user is an operator.
     * @return true if this user is an operator
     */
    public boolean isOperator() {
        return this.isOperator;
    }

    /**
     * Sets whether this user is an operator.
     * @param isOperator whether this user is an operator
     */
    public void setOperator(boolean isOperator) {
        this.isOperator = isOperator;
    }

    /**
     * Returns all channels that this user is in.
     * @return channels this user is in
     */
    public String[] getChannels() {
        return this.channels;
    }

    /**
     * Sets the channels that this user is in.
     * @param channels channels that this user is in
     */
    protected void setChannels(String[] channels) {
        this.channels = channels;
    }
}
