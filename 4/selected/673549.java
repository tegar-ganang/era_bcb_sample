package net.sf.babble.events;

import java.util.EventObject;
import net.sf.babble.ChannelMode;
import net.sf.babble.UserInfo;

/**
 * This event models the response from a channel LIST command.
 * @see net.sf.babble.ChannelMode
 * @see net.sf.babble.UserInfo
 * @version $Id: ChannelListEvent.java 262 2007-02-06 06:55:29 +0000 (Tue, 06 Feb 2007) speakmon $
 * @author Ben Speakmon
 */
public class ChannelListEvent extends EventObject {

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Holds value of property channelMode.
     */
    private ChannelMode channelMode;

    /**
     * Holds value of property item.
     */
    private String item;

    /**
     * Holds value of property userInfo.
     */
    private UserInfo userInfo;

    /**
     * Holds value of property whenSet.
     */
    private long whenSet;

    /**
     * Holds value of property last.
     */
    private boolean last;

    /**
     * Creates a new ChannelListEvent.
     * @param source the channel for which modes were requested
     */
    public ChannelListEvent(Object source) {
        super(source);
        channel = (String) source;
    }

    /**
     * Returns the channel for which modes were requested.
     * @return Value of property channel.
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Returns the <code>ChannelMode</code> at this point in the list.
     * @return a channel mode
     */
    public ChannelMode getChannelMode() {
        return this.channelMode;
    }

    /**
     * Sets the channel mode for this channel.
     * @param channelMode New value of property channelMode.
     */
    public void setChannelMode(ChannelMode channelMode) {
        this.channelMode = channelMode;
    }

    /**
     * Returns the item for this event.
     * @return this event's item
     */
    public String getItem() {
        return this.item;
    }

    /**
     * Sets this event's item.
     * @param item the event's item
     */
    public void setItem(String item) {
        this.item = item;
    }

    /**
     * Returns the <code>UserInfo</code> for this event.
     * @return this event's <code>UserInfo</code>
     */
    public UserInfo getUserInfo() {
        return this.userInfo;
    }

    /**
     *
     * @param userInfo New value of property userInfo.
     */
    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    /**
     * Returns the time when the mode was set as the number of seconds since epoch.
     * @return time the mode was set
     */
    public long getWhenSet() {
        return this.whenSet;
    }

    /**
     * Setter for property whenSet.
     * @param whenSet New value of property whenSet.
     */
    public void setWhenSet(long whenSet) {
        this.whenSet = whenSet;
    }

    /**
     * Returns true if this is the last <code>ChannelListEvent</code> from the same
     * LIST command.
     * @return true if this is the last event
     */
    public boolean isLast() {
        return this.last;
    }

    /**
     * Setter for property last.
     * @param last New value of property last.
     */
    public void setLast(boolean last) {
        this.last = last;
    }
}
