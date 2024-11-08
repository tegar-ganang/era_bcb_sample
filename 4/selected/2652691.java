package org.slasoi.common.messaging.pubsub.amqp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slasoi.common.messaging.MessagingException;
import org.slasoi.common.messaging.Setting;
import org.slasoi.common.messaging.Settings;
import org.slasoi.common.messaging.pubsub.Channel;
import org.slasoi.common.messaging.pubsub.MessageEvent;
import org.slasoi.common.messaging.pubsub.PubSubMessage;
import org.slasoi.common.messaging.pubsub.Subscription;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

/**
 * @author primoz.hadalin@xlab.si
 * 
 */
public class PubSubManager extends org.slasoi.common.messaging.pubsub.PubSubManager implements IMessageEventHandler {

    private Connection connection;

    private com.rabbitmq.client.Channel amqpChannel;

    private ConsumerCallback callback;

    private Map<String, String> queues;

    private String id;

    public PubSubManager(Settings settings) throws MessagingException {
        super(settings);
        queues = new HashMap<String, String>();
        connect();
    }

    @Override
    public void fireMessageEvent(MessageEvent messageEvent) {
        super.fireMessageEvent(messageEvent);
    }

    @Override
    public void connect() throws MessagingException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(getSetting(Setting.amqp_username));
        factory.setPassword(getSetting(Setting.amqp_password));
        factory.setVirtualHost(getSetting(Setting.amqp_virtualhost));
        factory.setHost(getSetting(Setting.amqp_host));
        factory.setPort(Integer.parseInt(getSetting(Setting.amqp_port)));
        try {
            connection = factory.newConnection();
            amqpChannel = connection.createChannel();
            id = factory.getUsername() + "@" + factory.getHost() + ":" + factory.getVirtualHost();
            callback = new ConsumerCallback(this, id);
        } catch (IOException e) {
            throw new MessagingException(e);
        }
    }

    @Override
    public String getId() throws MessagingException {
        return id;
    }

    @Override
    public void createChannel(Channel channel) throws MessagingException {
        try {
            amqpChannel.exchangeDeclare(channel.getName(), "fanout", true);
        } catch (IOException e) {
            throw new MessagingException(e);
        }
    }

    @Override
    public boolean isChannel(String channel) throws MessagingException {
        throw new MessagingException("AMQP PubSub implementation does not support this method");
    }

    @Override
    public void deleteChannel(String channel) throws MessagingException {
        try {
            amqpChannel.exchangeDelete(channel);
        } catch (IOException e) {
            throw new MessagingException(e);
        }
    }

    @Override
    public void publish(PubSubMessage message) throws MessagingException {
        try {
            amqpChannel.basicPublish(message.getChannelName(), "#", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getPayload().getBytes());
        } catch (IOException e) {
            throw new MessagingException(e);
        }
    }

    @Override
    public void subscribe(String channel) throws MessagingException {
        if (!queues.containsKey(channel)) {
            try {
                String queue = amqpChannel.queueDeclare().getQueue();
                amqpChannel.queueBind(queue, channel, "#");
                queues.put(channel, queue);
                amqpChannel.basicConsume(queue, true, callback);
            } catch (IOException e) {
                throw new MessagingException(e);
            }
        }
    }

    @Override
    public List<Subscription> getSubscriptions() throws MessagingException {
        List<Subscription> subscriptions = new ArrayList<Subscription>();
        for (String channel : queues.keySet()) {
            Subscription subscription = new Subscription(queues.get(channel), channel);
            subscriptions.add(subscription);
        }
        return subscriptions;
    }

    @Override
    public void unsubscribe(String channel) throws MessagingException {
        if (queues.containsKey(channel)) {
            try {
                amqpChannel.queueDelete(queues.get(channel));
                queues.remove(channel);
            } catch (IOException e) {
                throw new MessagingException(e);
            }
        }
    }

    @Override
    public void close() throws MessagingException {
        try {
            amqpChannel.close();
            connection.close();
        } catch (IOException e) {
            throw new MessagingException(e);
        }
    }
}
