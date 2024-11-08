package net.sf.babble.events;

import java.util.EventObject;
import net.sf.babble.UserInfo;

/**
 * This event models a JOIN message from the server.
 * @version $Id: JoinEvent.java 262 2007-02-06 06:55:29 +0000 (Tue, 06 Feb 2007) speakmon $
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
