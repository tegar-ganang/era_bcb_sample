package org.speakmon.babble.events;

import java.util.EventObject;
import org.speakmon.babble.UserInfo;

/**
 * This event models a user-issued INVITE command to a specific channel.
 * @version $Id: InviteEvent.java 239 2004-07-28 05:09:17Z speakmon $
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
