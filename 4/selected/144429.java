package net.walend.somnifugi;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.jms.JMSException;

/**
SomniTopicCache holds maps of names to topics. SomnifugiJMS uses it internally to create new and store existing topics for SomniJNDIByPass and SomniTopicFactory to hand out. Use those objects to get Topics.

@author @dwalend@
 */
class SomniTopicCache {

    public static final SomniTopicCache IT = new SomniTopicCache();

    private Map keysToTopics = new HashMap();

    private final Object guard = new Object();

    private SomniTopicCache() {
    }

    SomniTopic getTopic(String key, Context context) throws SomniNamingException {
        try {
            synchronized (guard) {
                if (containsTopic(key)) {
                    return (SomniTopic) keysToTopics.get(key);
                } else {
                    ChannelFactory factory = ChannelFactoryCache.IT.getChannelFactoryForContext(context);
                    SomniTopic topic = new SomniTopic(key, factory, context);
                    keysToTopics.put(key, topic);
                    SomniLogger.IT.config("Added " + topic.getName() + " Topic.");
                    return topic;
                }
            }
        } catch (NamingException ne) {
            throw new SomniNamingException(ne);
        }
    }

    void putTemporaryTopic(SomniTemporaryTopic topic) throws JMSException {
        if (!containsTopic(topic.getTopicName())) {
            keysToTopics.put(topic.getTopicName(), topic);
            SomniLogger.IT.config("Added " + topic.getTopicName() + " Topic.");
        } else {
            throw new SomniTempDestinationNameException(topic.getTopicName());
        }
    }

    SomniTopic removeTopic(String key) {
        synchronized (guard) {
            SomniLogger.IT.config("Removed " + key + " Topic.");
            return (SomniTopic) keysToTopics.remove(key);
        }
    }

    boolean containsTopic(String key) {
        synchronized (guard) {
            return keysToTopics.containsKey(key);
        }
    }

    void endDurableSubscription(String subscriptionName) {
        synchronized (guard) {
            Iterator it = keysToTopics.values().iterator();
            while (it.hasNext()) {
                SomniTopic topic = (SomniTopic) it.next();
                topic.removeDurableSubscriber(subscriptionName);
            }
        }
    }
}
