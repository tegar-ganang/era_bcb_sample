package net.sf.babble.events;

import java.util.EventObject;

/**
 * This event models a response to a topic request for a channel.
 * @version $Id: TopicRequestEvent.java 262 2007-02-06 06:55:29 +0000 (Tue, 06 Feb 2007) speakmon $
 * @author Ben Speakmon
 */
public class TopicRequestEvent extends EventObject {

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Holds value of property topic.
     */
    private String topic;

    /**
     * Creates a new TopicRequestEvent.
     * @param source the channel this topic is for
     */
    public TopicRequestEvent(Object source) {
        super(source);
        channel = (String) source;
    }

    /**
     * Returns the channel for which the topic was requested.
     * @return the channel name
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Returns the topic.
     * @return the topic
     */
    public String getTopic() {
        return this.topic;
    }

    /**
     * Sets the topic.
     * @param topic the topic
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }
}
