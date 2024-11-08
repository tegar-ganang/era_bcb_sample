package org.slasoi.common.messaging.pubsub.jms;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slasoi.common.messaging.MessagingException;
import org.slasoi.common.messaging.Setting;
import org.slasoi.common.messaging.Settings;
import org.slasoi.common.messaging.pubsub.Channel;
import org.slasoi.common.messaging.pubsub.MessageEvent;
import org.slasoi.common.messaging.pubsub.PubSubMessage;
import org.slasoi.common.messaging.pubsub.Subscription;
import javax.jms.*;
import java.util.ArrayList;
import java.util.List;

public class PubSubManager extends org.slasoi.common.messaging.pubsub.PubSubManager {

    ActiveMQConnectionFactory connectionFactory;

    TopicConnection connection;

    TopicSession session;

    List<TopicPublisher> publishers;

    List<TopicSubscriber> subscribers;

    public PubSubManager(Settings settings) throws MessagingException {
        super(settings);
        publishers = new ArrayList<TopicPublisher>();
        subscribers = new ArrayList<TopicSubscriber>();
        setUp();
    }

    private void setUp() throws MessagingException {
        try {
            if (getSetting(Setting.jms_host) != null) {
                connectionFactory = new ActiveMQConnectionFactory(getSetting(Setting.jms_username), getSetting(Setting.jms_password), "tcp://" + getSetting(Setting.jms_host) + ":" + getSetting(Setting.jms_port));
            } else {
                connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
            }
            connection = connectionFactory.createTopicConnection();
            connection.start();
            session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (Exception e) {
            throw new MessagingException(e);
        }
    }

    @Override
    protected void connect() throws MessagingException {
        setUp();
    }

    @Override
    public String getId() throws MessagingException {
        try {
            return connection.getClientID();
        } catch (JMSException e) {
            throw new MessagingException(e);
        }
    }

    @Override
    public void close() throws MessagingException {
        try {
            for (TopicPublisher publisher : publishers) {
                publisher.close();
            }
            for (TopicSubscriber subscriber : subscribers) {
                subscriber.close();
            }
            session.close();
            connection.stop();
            connection.close();
        } catch (Exception e) {
            throw new MessagingException(e);
        }
    }

    @Override
    public void createChannel(Channel channel) throws MessagingException {
        System.out.println("JMS: No need to call channel creation method. " + "Channel gets created when publishing a message.");
    }

    @Override
    public void deleteChannel(String channel) throws MessagingException {
        System.out.println("JMS does not support channel deletion.");
    }

    @Override
    public boolean isChannel(String channel) throws MessagingException {
        System.out.println("JMS does not support if channel is available.");
        return false;
    }

    @Override
    public void publish(PubSubMessage message) throws MessagingException {
        try {
            Topic topic = session.createTopic(prepareChannelName(message.getChannelName()));
            TopicPublisher publisher = session.createPublisher(topic);
            publishers.add(publisher);
            ObjectMessage jmsMessage = session.createObjectMessage();
            jmsMessage.setObject(message);
            publisher.publish(topic, jmsMessage);
        } catch (JMSException e) {
            throw new MessagingException(e);
        }
    }

    @Override
    public void subscribe(String channelName) throws MessagingException {
        Channel channel = new Channel(channelName);
        try {
            Topic topic = session.createTopic(prepareChannelName(channel.getName()));
            TopicSubscriber subscriber = session.createSubscriber(topic);
            subscribers.add(subscriber);
            addListener(subscriber);
        } catch (JMSException e) {
            throw new MessagingException(e);
        }
    }

    @Override
    public void unsubscribe(String channelName) throws MessagingException {
        Channel channel = new Channel(channelName);
        List<TopicSubscriber> tmpSubscribers = new ArrayList<TopicSubscriber>();
        for (TopicSubscriber subscriber : subscribers) {
            try {
                if (extractChannelName(subscriber.getTopic().getTopicName()).equals(channel.getName())) {
                    tmpSubscribers.add(subscriber);
                }
            } catch (JMSException e) {
                throw new MessagingException(e);
            }
        }
        for (TopicSubscriber subscriber : tmpSubscribers) {
            try {
                subscriber.close();
            } catch (JMSException e) {
                throw new MessagingException(e);
            }
            subscribers.remove(subscriber);
        }
    }

    private void addListener(TopicSubscriber subscriber) throws MessagingException {
        try {
            subscriber.setMessageListener(new javax.jms.MessageListener() {

                public void onMessage(javax.jms.Message receivedMessage) {
                    ObjectMessage objectMessage = (ObjectMessage) receivedMessage;
                    try {
                        PubSubMessage message = (PubSubMessage) objectMessage.getObject();
                        MessageEvent messageEvent = new MessageEvent(this, message);
                        fireMessageEvent(messageEvent);
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JMSException e) {
            throw new MessagingException(e);
        }
    }

    @Override
    public List<Subscription> getSubscriptions() throws MessagingException {
        throw new MessagingException("Not implemented");
    }
}
