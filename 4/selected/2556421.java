package net.walend.somnifugi;

import java.util.Map;
import java.util.HashMap;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.jms.JMSException;

/**
SomniQueueCache holds maps of names to queues. SomnifugiJMS uses it internally to create new and store existing queues for SomniJNDIByPass and SomniQueueFactory to hand out. Use those objects to get Queues.

@author @dwalend@
 */
class SomniQueueCache {

    public static final SomniQueueCache IT = new SomniQueueCache();

    private Map keysToQueues = new HashMap();

    private final Object guard = new Object();

    private SomniQueueCache() {
    }

    SomniQueue getQueue(String key, Context context) throws SomniNamingException {
        try {
            synchronized (guard) {
                if (containsQueue(key)) {
                    return (SomniQueue) keysToQueues.get(key);
                } else {
                    ChannelFactory factory = ChannelFactoryCache.IT.getChannelFactoryForContext(context);
                    SomniQueue queue = new SomniQueue(key, factory, context);
                    keysToQueues.put(key, queue);
                    SomniLogger.IT.config("Added " + queue.getName() + " Queue.");
                    return queue;
                }
            }
        } catch (NamingException ne) {
            throw new SomniNamingException(ne);
        }
    }

    void putTemporaryQueue(SomniTemporaryQueue queue) throws JMSException {
        if (!containsQueue(queue.getQueueName())) {
            keysToQueues.put(queue.getQueueName(), queue);
            SomniLogger.IT.config("Added " + queue.getQueueName() + " Queue.");
        } else {
            throw new SomniTempDestinationNameException(queue.getQueueName());
        }
    }

    SomniQueue removeQueue(String key) {
        synchronized (guard) {
            SomniLogger.IT.config("Removed " + key + " Queue.");
            return (SomniQueue) keysToQueues.remove(key);
        }
    }

    boolean containsQueue(String key) {
        synchronized (guard) {
            return keysToQueues.containsKey(key);
        }
    }
}
