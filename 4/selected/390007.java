package genericirc.irccore;

import java.util.EventObject;

/**
 * Contains information about new topics for channels.
 * @author Steve "Uru" West <uruwolf@users.sourceforge.net>
 * @version 2011-07-28
 */
public class TopicUpdatedEvent extends EventObject {

    private String newTopic;

    private String channel;

    /**
     * Creates a new TopicUpdatedEvent object.
     * @param source The source of the event
     * @param newTopic The new topic for the channel
     * @param channel The channel where the change happened
     */
    public TopicUpdatedEvent(Object source, String newTopic, String channel) {
        super(source);
        this.newTopic = newTopic;
        this.channel = channel;
    }

    /**
     * @return the newTopic
     */
    public String getNewTopic() {
        return newTopic;
    }

    /**
     * @return the channel with the changed topic
     */
    public String getChannel() {
        return channel;
    }
}
