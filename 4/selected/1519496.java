package org.speakmon.babble.events;

import java.util.EventObject;
import org.speakmon.babble.UserInfo;

/**
 * This event models a JOIN message from the server.
 * @version $Id: JoinEvent.java 239 2004-07-28 05:09:17Z speakmon $
 * @author Ben Speakmon
 */
public class JoinEvent extends EventObject {

    /**
     * Holds value of property user.
     */
    private UserInfo user;

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Creates a new JoinEvent.
     * @param source the <code>UserInfo</code> object for the user that joined
     */
    public JoinEvent(Object source) {
        super(source);
        user = (UserInfo) source;
    }

    /**
     * Returns the <code>UserInfo</code> object for the user that just joined.
     * @return the joining <code>UserInfo</code>
     */
    public UserInfo getUser() {
        return this.user;
    }

    /**
     * Returns the channel the user joined.
     * @return the channel
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Sets the channel the user joined.
     * @param channel the channel
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }
}
