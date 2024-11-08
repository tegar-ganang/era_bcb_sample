package de.z8bn.ircg;

import org.jibble.pircbot.Colors;

/**
 *
 * @author  cf2
 */
public class Trigger {

    /**
     * Holds value of property user.
     */
    private String user;

    /**
     * Holds value of property trigger.
     */
    private String trigger;

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Holds value of property lastCrawled.
     */
    private long lastCrawled;

    /**
     * Holds value of property needsDccServer.
     */
    private boolean needsDccServer;

    /**
     * Holds value of property login.
     */
    private String login;

    /**
     * Holds value of property host.
     */
    private String host;

    /**
     * Holds value of property FServInformation.
     */
    private FservInf fservInformation;

    /** Creates a new instance of Trigger */
    public Trigger() {
    }

    public Trigger(String user, String trigger, String channel) {
        this.user = user;
        this.trigger = trigger;
        this.channel = channel;
    }

    public static Trigger parseTrigger(String trigger, String channel) {
        String[] parts = trigger.split("\\s+", 3);
        Trigger ret = null;
        try {
            ret = new Trigger(parts[1], parts[2], channel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public boolean equals(Object o) {
        return (o instanceof Trigger && ((Trigger) o).channel.equals(channel) && ((Trigger) o).user.equals(user) && ((Trigger) o).trigger.equals(trigger));
    }

    public int hashCode() {
        return 17 + user.hashCode() + channel.hashCode() + trigger.hashCode();
    }

    public String toString() {
        return user + ":" + trigger;
    }

    /**
     * Getter for property user.
     * @return Value of property user.
     */
    public String getUser() {
        return this.user;
    }

    /**
     * Setter for property user.
     * @param user New value of property user.
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Getter for property trigger.
     * @return Value of property trigger.
     */
    public String getTrigger() {
        return this.trigger;
    }

    /**
     * Setter for property trigger.
     * @param trigger New value of property trigger.
     */
    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    /**
     * Getter for property channel.
     * @return Value of property channel.
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Setter for property channel.
     * @param channel New value of property channel.
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Getter for property lastCrawled.
     * @return Value of property lastCrawled.
     */
    public long getLastCrawled() {
        return this.lastCrawled;
    }

    /**
     * Setter for property lastCrawled.
     * @param lastCrawled New value of property lastCrawled.
     */
    public void setLastCrawled(long lastCrawled) {
        this.lastCrawled = lastCrawled;
    }

    /**
     * Getter for property needsDccServer.
     * @return Value of property needsDccServer.
     */
    public boolean isNeedsDccServer() {
        return this.needsDccServer;
    }

    /**
     * Setter for property needsDccServer.
     * @param needsDccServer New value of property needsDccServer.
     */
    public void setNeedsDccServer(boolean needsDccServer) {
        this.needsDccServer = needsDccServer;
    }

    /**
     * Getter for property login.
     * @return Value of property login.
     */
    public String getLogin() {
        return this.login;
    }

    /**
     * Setter for property login.
     * @param login New value of property login.
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Getter for property host.
     * @return Value of property host.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Setter for property host.
     * @param host New value of property host.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Getter for property FServInformation.
     * @return Value of property FServInformation.
     */
    public FservInf getFServInformation() {
        if (fservInformation == null) fservInformation = new FservInf();
        return this.fservInformation;
    }
}
