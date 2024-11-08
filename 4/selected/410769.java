package org.speakmon.babble.events;

import java.util.EventObject;

/**
 * This event models the response to a LIST command.
 * @version $Id: ListEvent.java 239 2004-07-28 05:09:17Z speakmon $
 * @author Ben Speakmon
 */
public class ListEvent extends EventObject {

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Holds value of property visibleNickCount.
     */
    private int visibleNickCount;

    /**
     * Holds value of property topic.
     */
    private String topic;

    /**
     * Holds value of property last.
     */
    private boolean last;

    /**
     * Creates a new ListEvent.
     * @param source the channel for this LIST
     */
    public ListEvent(Object source) {
        super(source);
        channel = (String) source;
    }

    /**
     * Returns the channel to which the LIST command was sent.
     * @return the channel that received the LIST command
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Returns the number of visible nicks in the channel.
     * @return number of visible nicks
     */
    public int getVisibleNickCount() {
        return this.visibleNickCount;
    }

    /**
     * Sets the number of visible nicks in the channel.
     * @param visibleNickCount number of visible nicks
     */
    public void setVisibleNickCount(int visibleNickCount) {
        this.visibleNickCount = visibleNickCount;
    }

    /**
     * Returns the topic for this channel.
     * @return this channel's topic
     */
    public String getTopic() {
        return this.topic;
    }

    /**
     * Sets the topic for this channel.
     * @param topic the topic for this channel
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * Returns true if this is the last response to this LIST command.
     * @return true if this is the last response
     */
    public boolean isLast() {
        return this.last;
    }

    /**
     * Sets whether this is the last response to this LIST command.
     * @param last true if this is the last response
     */
    public void setLast(boolean last) {
        this.last = last;
    }
}
