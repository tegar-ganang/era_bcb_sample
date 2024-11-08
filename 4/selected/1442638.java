package net.walend.somnifugi;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.jms.QueueSession;
import javax.jms.Queue;
import javax.jms.JMSException;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueBrowser;
import javax.jms.TemporaryQueue;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.MessageConsumer;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import javax.jms.TemporaryTopic;
import javax.jms.InvalidDestinationException;

/**
A Session for SomniQueues.

@author @dwalend@
@author @pwang@ added support for client acknowledgement.
 */
public class SomniQueueSession extends SomniSession implements QueueSession {

    private int tempCount = 0;

    protected SomniQueueSession(String name, SomniExceptionListener exceptionLisetener, boolean started, Context context, int acknowledgeMode) {
        super(name, exceptionLisetener, started, context, acknowledgeMode);
    }

    /** Creates a queue identity given a <CODE>Queue</CODE> name.
      *
<P>This facility is provided for the rare cases where clients need to
dynamically manipulate queue identity. It allows the creation of a
queue identity with a provider-specific name. Clients that depend 
on this ability are not portable.
      *
<P>Note that this method is not for creating the physical queue. 
The physical creation of queues is an administrative task and is not
to be initiated by the JMS API. The one exception is the
creation of temporary queues, which is accomplished with the 
<CODE>createTemporaryQueue</CODE> method.
      *
@param queueName the name of this <CODE>Queue</CODE>
      *
@return a <CODE>Queue</CODE> with the given name
      *
@exception JMSException if the session fails to create a queue
                        due to some internal error.
@exception UnsupportedOperationException because it isn't implemented.
      */
    public Queue createQueue(String queueName) throws JMSException {
        return SomniQueueCache.IT.getQueue(queueName, getContext());
    }

    /** Creates a <CODE>QueueReceiver</CODE> object to receive messages from the
specified queue.
      *
@param queue the <CODE>Queue</CODE> to access
      *
@exception JMSException if the session fails to create a receiver
                        due to some internal error.
@exception InvalidDestinationException if an invalid queue is specified.
      */
    public QueueReceiver createReceiver(Queue queue) throws JMSException {
        SomniQueue aqueue = (SomniQueue) queue;
        synchronized (guard) {
            checkClosed();
            String consumerName = createConsumerName(aqueue.getName(), "Receiver");
            SomniQueueReceiver result = new SomniQueueReceiver((SomniQueue) queue, consumerName, getExceptionListener(), this);
            addConsumer(result);
            return result;
        }
    }

    /** Creates a <CODE>QueueReceiver</CODE> object to receive messages from the 
specified queue using a message selector.
 
@param queue the <CODE>Queue</CODE> to access
@param messageSelector only messages with properties matching the
message selector expression are delivered. A value of null or
an empty string indicates that there is no message selector 
for the message consumer.
 
@exception JMSException if the session fails to create a receiver
                        due to some internal error.
@exception InvalidDestinationException if an invalid queue is specified.
@exception InvalidSelectorException if the message selector is invalid.
@exception UnsupportedOperationException because it isn't implemented.
      *
      */
    public QueueReceiver createReceiver(Queue queue, String messageSelector) throws JMSException {
        throw new UnsupportedOperationException("I haven't done this yet.");
    }

    /** Creates a <CODE>QueueSender</CODE> object to send messages to the 
specified queue.
      *
@param queue the <CODE>Queue</CODE> to access, or null if this is an 
unidentified producer
      *
@exception JMSException if the session fails to create a sender
                        due to some internal error.
@exception InvalidDestinationException if an invalid queue is specified.
      */
    public QueueSender createSender(Queue queue) throws JMSException {
        synchronized (guard) {
            checkClosed();
            SomniQueueSender result = new SomniQueueSender((SomniQueue) queue, createProducerName(queue.getQueueName(), "Sender"));
            addProducer(result);
            return result;
        }
    }

    /** Creates a <CODE>QueueBrowser</CODE> object to peek at the messages on 
the specified queue.
      *
@param queue the <CODE>Queue</CODE> to access
      *
@exception JMSException if the session fails to create a browser
                        due to some internal error.
@exception InvalidDestinationException if an invalid queue is specified.
@exception UnsupportedOperationException because it isn't implemented.
      */
    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        throw new UnsupportedOperationException("I haven't done this yet.");
    }

    /** Creates a <CODE>QueueBrowser</CODE> object to peek at the messages on 
the specified queue using a message selector.
 
@param queue the <CODE>Queue</CODE> to access
@param messageSelector only messages with properties matching the
message selector expression are delivered. A value of null or
an empty string indicates that there is no message selector 
for the message consumer.
 
@exception JMSException if the session fails to create a browser
                        due to some internal error.
@exception InvalidDestinationException if an invalid queue is specified.
@exception InvalidSelectorException if the message selector is invalid.
@exception UnsupportedOperationException because it isn't implemented.
      */
    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
        throw new UnsupportedOperationException("I haven't done this yet.");
    }

    /** Creates a <CODE>TemporaryQueue</CODE> object. Its lifetime will be that 
of the <CODE>QueueConnection</CODE> unless it is deleted earlier.
      *
@return a temporary queue identity
      *
@exception JMSException if the session fails to create a temporary queue
                        due to some internal error.
      */
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        try {
            String tempName;
            synchronized (guard) {
                tempName = getName() + ":temp" + tempCount;
                tempCount++;
            }
            SomniTemporaryQueue queue = new SomniTemporaryQueue(tempName, ChannelFactoryCache.IT.getChannelFactoryForContext(getContext()), getContext());
            SomniQueueCache.IT.putTemporaryQueue(queue);
            return queue;
        } catch (NamingException ne) {
            throw new SomniNamingException(ne);
        }
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
            return createSender((Queue) destination);
        } catch (ClassCastException cce) {
            throw new InvalidDestinationException("destination must be a Queue, not a " + destination.getClass().getName(), cce.getMessage());
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
            return createReceiver((Queue) destination);
        } catch (ClassCastException cce) {
            throw new InvalidDestinationException("destination must be a Queue, not a " + destination.getClass().getName(), cce.getMessage());
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
            return createReceiver((Queue) destination, messageSelector);
        } catch (ClassCastException cce) {
            throw new InvalidDestinationException("destination must be a Queue, not a " + destination.getClass().getName(), cce.getMessage());
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
        if (NoLocal) {
            throw new UnsupportedOperationException("You're using NoLocal in Somnifugi? That's a pretty boring consumer.");
        }
        try {
            return createReceiver((Queue) destination, messageSelector);
        } catch (ClassCastException cce) {
            throw new InvalidDestinationException("destination must be a Queue, not a " + destination.getClass().getName(), cce.getMessage());
        }
    }

    /** Creates a topic identity given a <CODE>Topic</CODE> name.
      *
      * <P>This facility is provided for the rare cases where clients need to
      * dynamically manipulate topic identity. This allows the creation of a
      * topic identity with a provider-specific name. Clients that depend 
      * on this ability are not portable.
      *
      * <P>Note that this method is not for creating the physical topic. 
      * The physical creation of topics is an administrative task and is not
      * to be initiated by the JMS API. The one exception is the
      * creation of temporary topics, which is accomplished with the 
      * <CODE>createTemporaryTopic</CODE> method.
      *  
      * @param topicName the name of this <CODE>Topic</CODE>
      *
      * @return a <CODE>Topic</CODE> with the given name
      *
      * @exception JMSException if the session fails to create a topic
      *                         due to some internal error.
      * @since 1.1
      */
    public Topic createTopic(String topicName) throws JMSException {
        throw new IllegalStateException("Don't use a QueueSession to work with Topics");
    }

    /** Creates a durable subscriber to the specified topic.
      *  
      * <P>If a client needs to receive all the messages published on a 
      * topic, including the ones published while the subscriber is inactive,
      * it uses a durable <CODE>TopicSubscriber</CODE>. The JMS provider
      * retains a record of this 
      * durable subscription and insures that all messages from the topic's 
      * publishers are retained until they are acknowledged by this 
      * durable subscriber or they have expired.
      *
      * <P>Sessions with durable subscribers must always provide the same 
      * client identifier. In addition, each client must specify a name that 
      * uniquely identifies (within client identifier) each durable 
      * subscription it creates. Only one session at a time can have a 
      * <CODE>TopicSubscriber</CODE> for a particular durable subscription.
      *
      * <P>A client can change an existing durable subscription by creating 
      * a durable <CODE>TopicSubscriber</CODE> with the same name and a new 
      * topic and/or 
      * message selector. Changing a durable subscriber is equivalent to 
      * unsubscribing (deleting) the old one and creating a new one.
      *
      * <P>In some cases, a connection may both publish and subscribe to a 
      * topic. The subscriber <CODE>NoLocal</CODE> attribute allows a subscriber
      * to inhibit the delivery of messages published by its own connection.
      * The default value for this attribute is false.
      *
      * @param topic the non-temporary <CODE>Topic</CODE> to subscribe to
      * @param name the name used to identify this subscription
      *  
      * @exception JMSException if the session fails to create a subscriber
      *                         due to some internal error.
      * @exception InvalidDestinationException if an invalid topic is specified.
      *
      * @since 1.1
      */
    public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
        throw new IllegalStateException("Don't use a QueueSession to work with Topics");
    }

    /** Creates a durable subscriber to the specified topic, using a
      * message selector and specifying whether messages published by its
      * own connection should be delivered to it.
      *  
      * <P>If a client needs to receive all the messages published on a 
      * topic, including the ones published while the subscriber is inactive,
      * it uses a durable <CODE>TopicSubscriber</CODE>. The JMS provider
      * retains a record of this 
      * durable subscription and insures that all messages from the topic's 
      * publishers are retained until they are acknowledged by this 
      * durable subscriber or they have expired.
      *
      * <P>Sessions with durable subscribers must always provide the same
      * client identifier. In addition, each client must specify a name which
      * uniquely identifies (within client identifier) each durable
      * subscription it creates. Only one session at a time can have a
      * <CODE>TopicSubscriber</CODE> for a particular durable subscription.
      * An inactive durable subscriber is one that exists but
      * does not currently have a message consumer associated with it.
      *
      * <P>A client can change an existing durable subscription by creating 
      * a durable <CODE>TopicSubscriber</CODE> with the same name and a new 
      * topic and/or 
      * message selector. Changing a durable subscriber is equivalent to 
      * unsubscribing (deleting) the old one and creating a new one.
      *
      * @param topic the non-temporary <CODE>Topic</CODE> to subscribe to
      * @param name the name used to identify this subscription
      * @param messageSelector only messages with properties matching the
      * message selector expression are delivered.  A value of null or
      * an empty string indicates that there is no message selector 
      * for the message consumer.
      * @param noLocal if set, inhibits the delivery of messages published
      * by its own connection
      *  
      * @exception JMSException if the session fails to create a subscriber
      *                         due to some internal error.
      * @exception InvalidDestinationException if an invalid topic is specified.
      * @exception InvalidSelectorException if the message selector is invalid.
      *
      * @since 1.1
      */
    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
        throw new IllegalStateException("Don't use a QueueSession to work with Topics");
    }

    /** Creates a <CODE>TemporaryTopic</CODE> object. Its lifetime will be that 
      * of the <CODE>Connection</CODE> unless it is deleted earlier.
      *
      * @return a temporary topic identity
      *
      * @exception JMSException if the session fails to create a temporary
      *                         topic due to some internal error.
      *
      * @since 1.1  
      */
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        throw new IllegalStateException("Don't use a QueueSession to work with Topics");
    }

    /** Unsubscribes a durable subscription that has been created by a client.
      *  
      * <P>This method deletes the state being maintained on behalf of the 
      * subscriber by its provider.
      *
      * <P>It is erroneous for a client to delete a durable subscription
      * while there is an active <CODE>MessageConsumer</CODE>
      * or <CODE>TopicSubscriber</CODE> for the 
      * subscription, or while a consumed message is part of a pending 
      * transaction or has not been acknowledged in the session.
      *
      * @param name the name used to identify this subscription
      *  
      * @exception JMSException if the session fails to unsubscribe to the 
      *                         durable subscription due to some internal error.
      * @exception InvalidDestinationException if an invalid subscription name
      *                                        is specified.
      *
      * @since 1.1
      */
    public void unsubscribe(String name) throws JMSException {
        throw new IllegalStateException("Don't use a QueueSession to work with Topics");
    }
}
