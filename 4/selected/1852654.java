package org.speakmon.babble.events;

import java.util.EventObject;
import org.speakmon.babble.ChannelModeInfo;
import org.speakmon.babble.ModeAction;
import org.speakmon.babble.UserInfo;

/**
 * This event models a channel mode change.
 * @see org.speakmon.babble.UserInfo
 * @see org.speakmon.babble.ChannelMode
 * @see org.speakmon.babble.ChannelModeInfo
 * @version $Id: ChannelModeChangeEvent.java 239 2004-07-28 05:09:17Z speakmon $
 * @author Ben Speakmon
 */
public class ChannelModeChangeEvent extends EventObject {

    /**
     * Holds value of property userInfo.
     */
    private UserInfo userInfo;

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Holds value of property modes.
     */
    private ChannelModeInfo[] modes;

    /**
     * Creates a new ChannelModeChangeEvent.
     * @param source the <code>UserInfo</code> for the user that changed the mode
     */
    public ChannelModeChangeEvent(Object source) {
        super(source);
        userInfo = (UserInfo) source;
    }

    /**
     * Returns the <code>UserInfo</code> for the user who changed the mode.
     * @return a <code>UserInfo</code> object
     */
    public UserInfo getUserInfo() {
        return this.userInfo;
    }

    /**
     * Returns the name of the channel that had its mode changed.
     * @return name of the channel
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Sets the name of the channel for which the mode was changed.
     * @param channel the channel name
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the <code>ChannelModeInfo</code> for the changed mode.
     * @return info for the changed modes
     */
    public ChannelModeInfo[] getModes() {
        return this.modes;
    }

    /**
     * Sets the <code>ChannelModeInfo</code>s for the changed mode.
     * @param modes the channel mode information
     */
    public void setModes(ChannelModeInfo[] modes) {
        this.modes = modes;
    }
}
