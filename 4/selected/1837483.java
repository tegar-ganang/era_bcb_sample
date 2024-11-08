package org.speakmon.babble.events;

import java.util.EventObject;
import org.speakmon.babble.ChannelModeInfo;

/**
 * This event models a reponse to a channel mode info request.
 * @version $Id: ChannelModeRequestEvent.java 239 2004-07-28 05:09:17Z speakmon $
 * @author Ben Speakmon
 */
public class ChannelModeRequestEvent extends EventObject {

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Holds value of property modes.
     */
    private ChannelModeInfo[] modes;

    /**
     * Creates a new ChannelModeRequestEvent.
     * @param source the channel for which modes were requested
     */
    public ChannelModeRequestEvent(Object source) {
        super(source);
        channel = (String) source;
    }

    /**
     * Returns the name of the channel for which modes were requested.
     * @return name of the channel
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Returns the <code>ChannelModeInfo</code> array for the channel.
     * @return mode info for the channel
     */
    public ChannelModeInfo[] getModes() {
        return this.modes;
    }

    /**
     * Sets the mode info for the channel.
     * @param modes mode info for the channel
     */
    public void setModes(ChannelModeInfo[] modes) {
        this.modes = modes;
    }
}
