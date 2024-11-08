package net.sf.babble.events;

import java.util.EventObject;
import net.sf.babble.UserInfo;

/**
 * This event models a user-issued INVITE command to a specific channel.
 * @version $Id: InviteEvent.java 262 2007-02-06 06:55:29 +0000 (Tue, 06 Feb 2007) speakmon $
 * @author Ben Speakmon
 */
public class InviteEvent extends EventObject {

    /**
     * Holds value of property user.
     */
    private UserInfo user;

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Creates a new InviteEvent.
     * @param source the <code>UserInfo</code> object that extended the invite
     */
    public InviteEvent(Object source) {
        super(source);
        user = (UserInfo) source;
    }

    /**
     * Returns the <code>UserInfo</code> object that extended the invite.
     * @return Value of property user.
     */
    public UserInfo getUser() {
        return this.user;
    }

    /**
     * Returns the channel associated with the invite.
     * @return the associated channel
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Sets the channel associated with the invite.
     * @param channel the associated channel
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }
}
