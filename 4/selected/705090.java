package net.sf.babble.events;

import java.util.EventObject;

/**
 * This event models the sending of an INVITE command to the server.
 * @version $Id: InviteSentEvent.java 262 2007-02-06 06:55:29 +0000 (Tue, 06 Feb 2007) speakmon $
 * @author Ben Speakmon
 */
public class InviteSentEvent extends EventObject {

    /**
     * Holds value of property nick.
     */
    private String nick;

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Creates a new InviteSentEvent.
     * @param source the invited nick
     */
    public InviteSentEvent(Object source) {
        super(source);
        nick = (String) source;
    }

    /**
     * Returns the invited nick.
     * @return Value of property nick.
     */
    public String getNick() {
        return this.nick;
    }

    /**
     * Gets the channel the user was invited to.
     * @return the channel
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Sets the channel the user was invited to.
     * @param channel the channel
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }
}
