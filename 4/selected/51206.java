package net.walend.somnifugi;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.jms.TopicSession;
import javax.jms.Topic;
import javax.jms.JMSException;
import javax.jms.TopicSubscriber;
import javax.jms.TopicPublisher;
import javax.jms.TemporaryTopic;
import javax.jms.InvalidDestinationException;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.TemporaryQueue;

/**
A Session for Topics. Use this to create TopicPublishers, TopicSubscribers, and Messages.

@author @dwalend@
@author @pwang@ added support for client acknowledgement.
 */
public class SomniTopicSession extends SomniSession implements TopicSession {

    private int tempCount = 0;

    protected SomniTopicSession(String name, SomniExceptionListener exceptionLisetener, boolean started, Context context, int acknowledgeMode) {
        super(name, exceptionLisetener, started, context, acknowledgeMode);
    }

    /** 
A SomnifugiJMS-only method.
<P>
Creates a nondurable subscriber to the specified topic, using a
message selector or specifying whether messages published by its
own connection should be delivered to it.

<P>A client uses a <CODE>TopicSubscriber</CODE> object to receive 
messages that have been published to a topic.
 
<P>Regular <CODE>TopicSubscriber</CODE> objects are not durable. 
They receive only messages that are published while they are active.

<P>Messages filtered out by a subscriber's message selector will 
never be delivered to the subscriber. From the subscriber's 
perspective, they do not exist.

<P>In some cases, a connection may both publish and subscribe to a 
topic. The subscriber <CODE>NoLocal</CODE> attribute allows a subscriber
to inhibit the delivery of messages published by its own connection.
The default value for this attribute is false.

@param topic the <CODE>Topic</CODE> to subscribe to
@param messageSelector only Messages that this MessageSelector matches will be delivered.
A value of null or an empty string indicates that there is no message selector 
for the message consumer.

@exception JMSException if the session fails to create a subscriber
                        due to some internal error.
@exception InvalidDestinationException if an invalid topic is specified.
@exception InvalidSelectorException if the message selector is invalid.
      */
    public TopicSubscriber createSubscriber(Topic topic, SomniMessageSelector messageSelector) throws JMSException {
        SomniTopic atopic = (SomniTopic) topic;
        synchronized (guard) {
            checkClosed();
            String subscriberName = createConsumerName(atopic.getTopicName(), "Subscriber");
            SomniTopicSubscriber result = new SomniTopicSubscriber(atopic, atopic.addSubscriber(subscriberName, false, messageSelector), subscriberName, getExceptionListener(), messageSelector, this);
            addConsumer(result);
            return result;
        }
    }

    /** Creates a topic identity given a <CODE>Topic</CODE> name.
      *
<P>This facility is provided for the rare cases where clients need to
dynamically manipulate topic identity. This allows the creation of a
topic identity with a provider-specific name. Clients that depend 
on this ability are not portable.
      *
<P>Note that this method is not for creating the physical topic. 
The physical creation of topics is an administrative task and is not
to be initiated by the JMS API. The one exception is the
creation of temporary topics, which is accomplished with the 
<CODE>createTemporaryTopic</CODE> method.
 
@param topicName the name of this <CODE>Topic</CODE>
      *
@return a <CODE>Topic</CODE> with the given name
      *
@exception JMSException if the session fails to create a topic
                        due to some internal error.
      */
    public Topic createTopic(String topicName) throws JMSException {
        return SomniTopicCache.IT.getTopic(topicName, getContext());
    }

    /** Creates a nondurable subscriber to the specified topic.
 
<P>A client uses a <CODE>TopicSubscriber</CODE> object to receive 
messages that have been published to a topic.
      *
<P>Regular <CODE>TopicSubscriber</CODE> objects are not durable. 
They receive only messages that are published while they are active.
      *
<P>In some cases, a connection may both publish and subscribe to a 
topic. The subscriber <CODE>NoLocal</CODE> attribute allows a subscriber
to inhibit the delivery of messages published by its own connection.
The default value for this attribute is false.
      *
@param topic the <CODE>Topic</CODE> to subscribe to
 
@exception JMSException if the session fails to create a subscriber
                        due to some internal error.
@exception InvalidDestinationException if an invalid topic is specified.
      */
    public TopicSubscriber createSubscriber(Topic topic) throws JMSException {
        SomniTopic atopic = (SomniTopic) topic;
        synchronized (guard) {
            checkClosed();
            String subscriberName = createConsumerName(atopic.getTopicName(), "Subscriber");
            SomniTopicSubscriber result = new SomniTopicSubscriber(atopic, atopic.addSubscriber(subscriberName, false, null), subscriberName, getExceptionListener(), this);
            addConsumer(result);
            return result;
        }
    }

    /** Creates a nondurable subscriber to the specified topic, using a
message selector or specifying whether messages published by its
own connection should be delivered to it.
      *
<P>A client uses a <CODE>TopicSubscriber</CODE> object to receive 
messages that have been published to a topic.
 
<P>Regular <CODE>TopicSubscriber</CODE> objects are not durable. 
They receive only messages that are published while they are active.
      *
<P>Messages filtered out by a subscriber's message selector will 
never be delivered to the subscriber. From the subscriber's 
perspective, they do not exist.
      *
<P>In some cases, a connection may both publish and subscribe to a 
topic. The subscriber <CODE>NoLocal</CODE> attribute allows a subscriber
to inhibit the delivery of messages published by its own connection.
The default value for this attribute is false.
      *
@param topic the <CODE>Topic</CODE> to subscribe to
@param messageSelector only messages with properties matching the
message selector expression are delivered. A value of null or
an empty string indicates that there is no message selector 
for the message consumer.
@param noLocal if set, inhibits the delivery of messages published
by its own connection

@exception JMSException if the session fails to create a subscriber
                        due to some internal error.
@exception InvalidDestinationException if an invalid topic is specified.
@exception InvalidSelectorException if the message selector is invalid.
@exception UnsupportedOperationException because it isn't implemented.
      */
    public TopicSubscriber createSubscriber(Topic topic, String messageSelector, boolean noLocal) throws JMSException {
        throw new UnsupportedOperationException("I haven't done this yet.");
    }

    /** Creates a durable subscriber to the specified topic.
 
<P>If a client needs to receive all the messages published on a 
topic, including the ones published while the subscriber is inactive,
it uses a durable <CODE>TopicSubscriber</CODE>. The JMS provider
retains a record of this 
durable subscription and insures that all messages from the topic's 
publishers are retained until they are acknowledged by this 
durable subscriber or they have expired.
      *
<P>Sessions with durable subscribers must always provide the same 
client identifier. In addition, each client must specify a name that 
uniquely identifies (within client identifier) each durable 
subscription it creates. Only one session at a time can have a 
<CODE>TopicSubscriber</CODE> for a particular durable subscription.
      *
<P>A client can change an existing durable subscription by creating 
a durable <CODE>TopicSubscriber</CODE> with the same name and a new 
topic and/or 
message selector. Changing a durable subscriber is equivalent to 
unsubscribing (deleting) the old one and creating a new one.
      *
<P>In some cases, a connection may both publish and subscribe to a 
topic. The subscriber <CODE>NoLocal</CODE> attribute allows a subscriber
to inhibit the delivery of messages published by its own connection.
The default value for this attribute is false.
      *
@param topic the non-temporary <CODE>Topic</CODE> to subscribe to
@param name the name used to identify this subscription
 
@exception JMSException if the session fails to create a subscriber
                        due to some internal error.
@exception InvalidDestinationException if an invalid topic is specified.
      */
    public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
        SomniTopic atopic = (SomniTopic) topic;
        synchronized (guard) {
            checkClosed();
            SomniTopicSubscriber result = new SomniTopicSubscriber(atopic, atopic.addSubscriber(name, true, null), name, getExceptionListener(), this);
            addConsumer(result);
            return result;
        }
    }

    /** Creates a durable subscriber to the specified topic, using a
message selector or specifying whether messages published by its
own connection should be delivered to it.
 
<P>If a client needs to receive all the messages published on a 
topic, including the ones published while the subscriber is inactive,
it uses a durable <CODE>TopicSubscriber</CODE>. The JMS provider
retains a record of this 
durable subscription and insures that all messages from the topic's 
publishers are retained until they are acknowledged by this 
durable subscriber or they have expired.
      *
<P>Sessions with durable subscribers must always provide the same
client identifier. In addition, each client must specify a name which
uniquely identifies (within client identifier) each durable
subscription it creates. Only one session at a time can have a
<CODE>TopicSubscriber</CODE> for a particular durable subscription.
An inactive durable subscriber is one that exists but
does not currently have a message consumer associated with it.
      *
<P>A client can change an existing durable subscription by creating 
a durable <CODE>TopicSubscriber</CODE> with the same name and a new 
topic and/or 
message selector. Changing a durable subscriber is equivalent to 
unsubscribing (deleting) the old one and creating a new one.
      *
@param topic the non-temporary <CODE>Topic</CODE> to subscribe to
@param name the name used to identify this subscription
@param messageSelector only messages with properties matching the
message selector expression are delivered.  A value of null or
an empty string indicates that there is no message selector 
for the message consumer.
@param noLocal if set, inhibits the delivery of messages published
by its own connection
 
@exception JMSException if the session fails to create a subscriber
                        due to some internal error.
@exception InvalidDestinationException if an invalid topic is specified.
@exception InvalidSelectorException if the message selector is invalid.
@exception UnsupportedOperationException because it isn't implemented.
      */
    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
        throw new UnsupportedOperationException("I haven't done this yet.");
    }

    /** Creates a publisher for the specified topic.
      *
<P>A client uses a <CODE>TopicPublisher</CODE> object to publish 
messages on a topic.
Each time a client creates a <CODE>TopicPublisher</CODE> on a topic, it
defines a 
new sequence of messages that have no ordering relationship with the 
messages it has previously sent.
      *
@param topic the <CODE>Topic</CODE> to publish to, or null if this is an
unidentified producer
      *
@exception JMSException if the session fails to create a publisher
                        due to some internal error.
@exception InvalidDestinationException if an invalid topic is specified.
     */
    public TopicPublisher createPublisher(Topic topic) throws JMSException {
        synchronized (guard) {
            checkClosed();
            SomniTopicPublisher result = new SomniTopicPublisher((SomniTopic) topic, createProducerName(topic.getTopicName(), "Publisher"));
            addProducer(result);
            return result;
        }
    }

    /** Creates a <CODE>TemporaryTopic</CODE> object. Its lifetime will be that 
of the <CODE>TopicConnection</CODE> unless it is deleted earlier.
      *
@return a temporary topic identity
      *
@exception JMSException if the session fails to create a temporary
                        topic due to some internal error.
      */
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        try {
            String tempName;
            synchronized (guard) {
                tempName = getName() + ":temp" + tempCount;
                tempCount++;
            }
            SomniTemporaryTopic topic = new SomniTemporaryTopic(tempName, ChannelFactoryCache.IT.getChannelFactoryForContext(getContext()), getContext());
            SomniTopicCache.IT.putTemporaryTopic(topic);
            return topic;
        } catch (NamingException ne) {
            throw new SomniNamingException(ne);
        }
    }

    /** Unsubscribes a durable subscription that has been created by a client.
 
<P>This method deletes the state being maintained on behalf of the 
subscriber by its provider.
      *
<P>It is erroneous for a client to delete a durable subscription
while there is an active <CODE>TopicSubscriber</CODE> for the 
subscription, or while a consumed message is part of a pending 
transaction or has not been acknowledged in the session.
      *
@param name the name used to identify this subscription
 
@exception JMSException if the session fails to unsubscribe to the 
                        durable subscription due to some internal error.
@exception InvalidDestinationException if an invalid subscription name
                                       is specified.
      */
    public void unsubscribe(String name) throws JMSException {
        SomniTopicCache.IT.endDurableSubscription(name);
        SomniLogger.IT.finer(getName() + " unsubscribed from " + name);
    }

    /** Creates a <CODE>MessageProducer</CODE> to send messages to the specified 
      * destination.
      *
      * <P>A client uses a <CODE>MessageProducer</CODE> object to send 
      * messages to a destination. Since <CODE>Queue</CODE> and <CODE>Topic</CODE> 
      * both inherit from <CODE>Destination</CODE>, they can be used in
      * the destination parameter to create a <CODE>MessageProducer</CODE> object.
      * 
      * @param destination the <CODE>Destination</CODE> to send to, 
      * or null if this is a producer which does not have a specified 
      * destination.
      *
      * @exception JMSException if the session fails to create a MessageProducer
      *                         due to some internal error.
      * @exception InvalidDestinationException if an invalid destination
      * is specified.
      *
      * @since 1.1 
      * 
     */
    public MessageProducer createProducer(Destination destination) throws JMSException {
        try {
            return createPublisher((Topic) destination);
        } catch (ClassCastException cce) {
            throw new InvalidDestinationException("destination must be a Topic, not a " + destination.getClass().getName(), cce.getMessage());
        }
    }

    /** Creates a <CODE>MessageConsumer</CODE> for the specified destination.
      * Since <CODE>Queue</CODE> and <CODE>Topic</CODE> 
      * both inherit from <CODE>Destination</CODE>, they can be used in
      * the destination parameter to create a <CODE>MessageConsumer</CODE>.
      *
      * @param destination the <CODE>Destination</CODE> to access. 
      *
      * @exception JMSException if the session fails to create a consumer
      *                         due to some internal error.
      * @exception InvalidDestinationException if an invalid destination 
      *                         is specified.
      *
      * @since 1.1 
      */
    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        try {
            return createSubscriber((Topic) destination);
        } catch (ClassCastException cce) {
            throw new InvalidDestinationException("destination must be a Topic, not a " + destination.getClass().getName(), cce.getMessage());
        }
    }

    /** Creates a <CODE>MessageConsumer</CODE> for the specified destination, 
      * using a message selector. 
      * Since <CODE>Queue</CODE> and <CODE>Topic</CODE> 
      * both inherit from <CODE>Destination</CODE>, they can be used in
      * the destination parameter to create a <CODE>MessageConsumer</CODE>.
      *
      * <P>A client uses a <CODE>MessageConsumer</CODE> object to receive 
      * messages that have been sent to a destination.
      *  
      *       
      * @param destination the <CODE>Destination</CODE> to access
      * @param messageSelector only messages with properties matching the
      * message selector expression are delivered. A value of null or
      * an empty string indicates that there is no message selector 
      * for the message consumer. 
      * 
      *  
      * @exception JMSException if the session fails to create a MessageConsumer
      *                         due to some internal error.
      * @exception InvalidDestinationException if an invalid destination
       * is specified.
     
      * @exception InvalidSelectorException if the message selector is invalid.
      *
      * @since 1.1 
      */
    public MessageConsumer createConsumer(Destination destination, java.lang.String messageSelector) throws JMSException {
        try {
            return createSubscriber((Topic) destination, messageSelector, false);
        } catch (ClassCastException cce) {
            throw new InvalidDestinationException("destination must be a Topic, not a " + destination.getClass().getName(), cce.getMessage());
        }
    }

    /** Creates <CODE>MessageConsumer</CODE> for the specified destination, using a
      * message selector. This method can specify whether messages published by 
      * its own connection should be delivered to it, if the destination is a 
      * topic. 
      *<P> Since <CODE>Queue</CODE> and <CODE>Topic</CODE> 
      * both inherit from <CODE>Destination</CODE>, they can be used in
      * the destination parameter to create a <CODE>MessageConsumer</CODE>.
      * <P>A client uses a <CODE>MessageConsumer</CODE> object to receive 
      * messages that have been published to a destination. 
      *               
      * <P>In some cases, a connection may both publish and subscribe to a 
      * topic. The consumer <CODE>NoLocal</CODE> attribute allows a consumer
      * to inhibit the delivery of messages published by its own connection.
      * The default value for this attribute is False. The <CODE>noLocal</CODE> 
      * value must be supported by destinations that are topics. 
      *
      * @param destination the <CODE>Destination</CODE> to access 
      * @param messageSelector only messages with properties matching the
      * message selector expression are delivered. A value of null or
      * an empty string indicates that there is no message selector 
      * for the message consumer.
      * @param NoLocal  - if true, and the destination is a topic,
      *                   inhibits the delivery of messages published
      *                   by its own connection.  The behavior for
      *                   <CODE>NoLocal</CODE> is 
      *                   not specified if the destination is a queue.
      * 
      * @exception JMSException if the session fails to create a MessageConsumer
      *                         due to some internal error.
      * @exception InvalidDestinationException if an invalid destination
       * is specified.
     
      * @exception InvalidSelectorException if the message selector is invalid.
      *
      * @since 1.1 
      *
      */
    public MessageConsumer createConsumer(Destination destination, java.lang.String messageSelector, boolean NoLocal) throws JMSException {
        try {
            return createSubscriber((Topic) destination, messageSelector, NoLocal);
        } catch (ClassCastException cce) {
            throw new InvalidDestinationException("destination must be a Topic, not a " + destination.getClass().getName(), cce.getMessage());
        }
    }

    /** Creates a queue identity given a <CODE>Queue</CODE> name.
      *
      * <P>This facility is provided for the rare cases where clients need to
      * dynamically manipulate queue identity. It allows the creation of a
      * queue identity with a provider-specific name. Clients that depend 
      * on this ability are not portable.
      *
      * <P>Note that this method is not for creating the physical queue. 
      * The physical creation of queues is an administrative task and is not
      * to be initiated by the JMS API. The one exception is the
      * creation of temporary queues, which is accomplished with the 
      * <CODE>createTemporaryQueue</CODE> method.
      *
      * @param queueName the name of this <CODE>Queue</CODE>
      *
      * @return a <CODE>Queue</CODE> with the given name
      *
      * @exception JMSException if the session fails to create a queue
      *                         due to some internal error.
      * @since 1.1
      */
    public Queue createQueue(String queueName) throws JMSException {
        throw new IllegalStateException("Don't use a TopicSession to create a Queue.");
    }

    /** Creates a <CODE>QueueBrowser</CODE> object to peek at the messages on 
      * the specified queue.
      *  
      * @param queue the <CODE>queue</CODE> to access
      *
      *  
      * @exception JMSException if the session fails to create a browser
      *                         due to some internal error.
      * @exception InvalidDestinationException if an invalid destination
      *                         is specified 
      *
      * @since 1.1 
      */
    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        throw new IllegalStateException("Don't use a TopicSession to work with Queues.");
    }

    /** Creates a <CODE>QueueBrowser</CODE> object to peek at the messages on 
      * the specified queue using a message selector.
      *  
      * @param queue the <CODE>queue</CODE> to access
      *
      * @param messageSelector only messages with properties matching the
      * message selector expression are delivered. A value of null or
      * an empty string indicates that there is no message selector 
      * for the message consumer.
      *  
      * @exception JMSException if the session fails to create a browser
      *                         due to some internal error.
      * @exception InvalidDestinationException if an invalid destination
      *                         is specified 
      * @exception InvalidSelectorException if the message selector is invalid.
      *
      * @since 1.1 
      */
    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
        throw new IllegalStateException("Don't use a TopicSession to work with Queues.");
    }

    /** Creates a <CODE>TemporaryQueue</CODE> object. Its lifetime will be that 
      * of the <CODE>Connection</CODE> unless it is deleted earlier.
      *
      * @return a temporary queue identity
      *
      * @exception JMSException if the session fails to create a temporary queue
      *                         due to some internal error.
      *
      *@since 1.1
      */
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        throw new IllegalStateException("Don't use a TopicSession to work with Queues.");
    }
}
