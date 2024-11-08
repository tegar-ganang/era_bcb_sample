package org.eaiframework.impl.jdk;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import org.eaiframework.Message;

/**
 * 
 */
public class JdkChannelRegistry {

    private static JdkChannelRegistry instance;

    private Map<String, Queue<Message>> queues = new HashMap<String, Queue<Message>>();

    private JdkChannelRegistry() {
    }

    public static JdkChannelRegistry getInstance() {
        if (instance == null) {
            instance = new JdkChannelRegistry();
        }
        return instance;
    }

    public void addChannel(String queueName, Queue<Message> queue) {
        queues.put(queueName, queue);
    }

    public void removeChannel(String queueName) {
        queues.remove(queueName);
    }

    public Queue<Message> getChannel(String queueName) {
        return queues.get(queueName);
    }
}
