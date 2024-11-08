package net.sf.babble.events;

import java.util.EventObject;
import net.sf.babble.UserInfo;

/**
 * This event models a TOPIC response indicating that the current channel's topic
 * has been changed.
 * @version $Id: TopicEvent.java 262 2007-02-06 06:55:29 +0000 (Tue, 06 Feb 2007) speakmon $
 * @author Ben Speakmon
 */
public class TopicEvent extends EventObject {

    /**
     * Holds value of property user.
     */
    private UserInfo user;

    /**
     * Holds value of property channel.
     */
    private String channel;

    /**
     * Holds value of property newTopic.
     */
    private String newTopic;

    /**
     * Creates a new TopicEvent.
     * @param source the <code>UserInfo</code> for the user that changed the topic
     */
    public TopicEvent(Object source) {
        super(source);
        user = (UserInfo) source;
    }

    /**
     * Getter for property user.
     * @return Value of property user.
     */
    public UserInfo getUser() {
        return this.user;
    }

    /**
     * Returns the channel whose topic was changed.
     * @return the affected channel
     */
    public String getChannel() {
        return this.channel;
    }

    /**
     * Sets the channel whose topic was changed.
     * @param channel the affected channel
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Returns the new topic.
     * @return the new topic
     */
    public String getNewTopic() {
        return this.newTopic;
    }

    /**
     * Sets the new topic.
     * @param newTopic the new topic
     */
    public void setNewTopic(String newTopic) {
        this.newTopic = newTopic;
    }
}
