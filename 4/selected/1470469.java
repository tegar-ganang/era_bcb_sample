package net.sf.babble.events;

import java.util.EventObject;

/**
 * This event models the response from a NAMES command.
 * @version $Id: NamesEvent.java 262 2007-02-06 06:55:29 +0000 (Tue, 06 Feb 2007) speakmon $
 * @author Ben Speakmon
 */
public class NamesEvent extends EventObject {

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Holds value of property nicks.
     */
    private String[] nicks;

    /**
     * Holds value of property last.
     */
    private boolean last;

    /**
     * Creates a new NamesEvent.
     * @param source the channel that received the NAMES command
     */
    public NamesEvent(Object source) {
        super(source);
        channel = (String) source;
    }

    /**
     * Returns the channel that received the NAMES command.
     * @return the channel
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Returns an array of the nicks in the channel.
     * @return nicks in the channel
     */
    public String[] getNicks() {
        return this.nicks;
    }

    /**
     * Sets the nicks in the channel.
     * @param nicks nicks in the channel
     */
    public void setNicks(String[] nicks) {
        this.nicks = nicks;
    }

    /**
     * Returns true if this is the last line in the response to this command.
     * @return true if this is the last line in the response
     */
    public boolean isLast() {
        return this.last;
    }

    /**
     * Sets whether this is the last line in response to this command.
     * @param last whether this is the last line in response
     */
    public void setLast(boolean last) {
        this.last = last;
    }
}
