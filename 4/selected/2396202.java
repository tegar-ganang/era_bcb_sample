package net.sf.babble.events;

import java.util.EventObject;
import net.sf.babble.UserInfo;

/**
 * This event models the response to a WHO command.
 * @version $Id: WhoEvent.java 262 2007-02-06 06:55:29 +0000 (Tue, 06 Feb 2007) speakmon $
 * @author Ben Speakmon
 */
public class WhoEvent extends EventObject {

    /**
     * Holds value of property user.
     */
    private UserInfo user;

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Holds value of property ircServer.
     */
    private String ircServer;

    /**
     * Holds value of property mask.
     */
    private String mask;

    /**
     * Holds value of property hopCount.
     */
    private int hopCount;

    /**
     * Holds value of property realName.
     */
    private String realName;

    /**
     * Holds value of property last.
     */
    private boolean last;

    /**
     * Creates a new WhoEvent.
     * @param source the <code>UserInfo</code> for the specified user
     */
    public WhoEvent(Object source) {
        super(source);
        user = (UserInfo) source;
    }

    /**
     * Returns the <code>UserInfo</code> for the user who was the target of the WHO
     * command.
     * @return the targeted user's <code>UserInfo</code>
     */
    public UserInfo getUser() {
        return this.user;
    }

    /**
     * Returns the channel the user is in.
     * @return the user's channel
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Sets the user's channel.
     * @param channel the user's channel
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the server the user is on.
     * @return the user's server
     */
    public String getIrcServer() {
        return this.ircServer;
    }

    /**
     * Sets the server the user is on.
     * @param ircServer the user's server
     */
    public void setIrcServer(String ircServer) {
        this.ircServer = ircServer;
    }

    /**
     * Returns the user's hostmask.
     * @return the user's hostmask
     */
    public String getMask() {
        return this.mask;
    }

    /**
     * Sets the user's hostmask.
     * @param mask the user's hostmask
     */
    public void setMask(String mask) {
        this.mask = mask;
    }

    /**
     * Returns the user's hop count to the server.
     * @return the user's hop count
     */
    public int getHopCount() {
        return this.hopCount;
    }

    /**
     * Sets the user's hop count to the server.
     * @param hopCount the user's hop count
     */
    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    /**
     * Returns the user's real name.
     * @return the user's real name
     */
    public String getRealName() {
        return this.realName;
    }

    /**
     * Sets the user's real name.
     * @param realName the user's real name
     */
    public void setRealName(String realName) {
        this.realName = realName;
    }

    /**
     * Returns true if this is the last response to the WHO command.
     * @return true if this is the last response
     */
    public boolean isLast() {
        return this.last;
    }

    /**
     * Sets whether this is the last response to the WHO command.
     * @param last whether this is the last response to the WHO command
     */
    public void setLast(boolean last) {
        this.last = last;
    }
}
