package org.speakmon.babble.events;

import java.util.EventObject;
import org.speakmon.babble.UserInfo;

/**
 * This event models a user leaving a channel.
 * @version $Id: PartEvent.java 239 2004-07-28 05:09:17Z speakmon $
 * @author Ben Speakmon
 */
public class PartEvent extends EventObject {

    /**
     * Holds value of property user.
     */
    private UserInfo user;

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Holds value of property reason.
     */
    private String reason;

    /**
     * Creates a new instance of PartEvent
     * @param source the user who left the channel
     */
    public PartEvent(Object source) {
        super(source);
        user = (UserInfo) source;
    }

    /**
     * Returns the <code>UserInfo</code> for the user who left the channel.
     * @return the user who left the channel
     */
    public UserInfo getUser() {
        return this.user;
    }

    /**
     * Returns the channel that the user left.
     * @return the channel the user left
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Sets the channel the user left.
     * @param channel the channel the user left
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the user's reason for leaving.
     * @return the reason
     */
    public String getReason() {
        return this.reason;
    }

    /**
     * Sets the user's reason for leaving.
     * @param reason the reason for leaving
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
}
