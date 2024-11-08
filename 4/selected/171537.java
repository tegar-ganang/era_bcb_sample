package com.mycila.jms;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.LinkedHashMap;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
final class InboundMessage<T extends Serializable> extends BasicMessage<T> implements JMSInboundMessage<T> {

    final transient Sender sender;

    final String replyTo;

    public InboundMessage(Sender sender, Message message) {
        super((T) extractBody(message));
        this.replyTo = extractDestination(message);
        this.sender = sender;
        extractProperties(message);
    }

    @Override
    public <T extends Serializable> JMSReply<T> createReply(T content) {
        if (replyTo == null) throw new JMSClientException("Cannot reply to this message: it does not expect any reply");
        return new Reply<T>(this, content);
    }

    @Override
    public boolean isReplyExpected() {
        return replyTo != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "replyTo='" + replyTo + '\'' + ", headers=" + headers + ", properties=" + properties + '}';
    }

    private void extractProperties(Message message) {
        try {
            if (message.getJMSCorrelationID() != null) headers.put(JMSHeader.CorrelationID, message.getJMSCorrelationID());
            Enumeration<String> names = message.getPropertyNames();
            while (names.hasMoreElements()) {
                String key = names.nextElement();
                properties.put(key, (Serializable) message.getObjectProperty(key));
            }
        } catch (JMSException e) {
            throw new JMSClientException(e);
        }
    }

    private static Serializable extractBody(Message message) {
        try {
            if (message instanceof TextMessage) return ((TextMessage) message).getText();
            if (message instanceof ObjectMessage) return ((ObjectMessage) message).getObject();
            if (message instanceof MapMessage) {
                LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
                Enumeration<String> names = ((MapMessage) message).getMapNames();
                while (names.hasMoreElements()) {
                    String key = names.nextElement();
                    map.put(key, ((MapMessage) message).getObject(key));
                }
                return map;
            }
            if (message instanceof BytesMessage) {
                BytesMessage bm = (BytesMessage) message;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8096];
                int c;
                while ((c = bm.readBytes(buffer)) != -1) baos.write(buffer, 0, c);
                return baos.toByteArray();
            }
        } catch (JMSException e) {
            throw new JMSClientException(e);
        }
        throw new JMSClientException("Unsupported message type: " + message.getClass().getName() + " (" + message + ")");
    }

    private static String extractDestination(Message message) {
        Destination destination;
        try {
            destination = message.getJMSReplyTo();
            if (destination == null) return null;
            if (destination instanceof Queue) return "queue:" + ((Queue) destination).getQueueName();
            if (destination instanceof Topic) return "topic:" + ((Topic) destination).getTopicName();
        } catch (JMSException e) {
            throw new JMSClientException(e);
        }
        throw new JMSClientException("Unsupported destination type: " + destination.getClass().getName());
    }
}
