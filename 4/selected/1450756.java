package net.sf.babble.events;

import java.util.EventObject;
import net.sf.babble.UserInfo;

/**
 * This event models a public message to a channel.
 * @version $Id: PublicMessageEvent.java 262 2007-02-06 06:55:29 +0000 (Tue, 06 Feb 2007) speakmon $
 * @author Ben Speakmon
 */
public class PublicMessageEvent extends EventObject {

    /**
     * Holds value of property user.
     */
    private UserInfo user;

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Holds value of property message.
     */
    private String message;

    /**
     * Creates a new PublicMessageEvent.
     * @param source the <code>UserInfo</code> of the user who sent the public message
     */
    public PublicMessageEvent(Object source) {
        super(source);
        user = (UserInfo) source;
    }

    /**
     * Returns the <code>UserInfo</code> for the sender of the public message.
     * @return the sender's <code>UserInfo</code>
     */
    public UserInfo getUser() {
        return this.user;
    }

    /**
     * Returns the channel the message was sent to.
     * @return the destination channel
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Sets the channel the message was sent to.
     * @param channel the destination channel
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the message text.
     * @return the message text
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Sets the message text.
     * @param message the message text
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
