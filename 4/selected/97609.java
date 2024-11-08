package com.incendiaryblue.msg;

import com.incendiaryblue.appframework.ServerConfig;
import com.incendiaryblue.config.XMLConfigurable;
import com.incendiaryblue.config.XMLConfigurationException;
import com.incendiaryblue.config.XMLContext;
import org.w3c.dom.*;
import javax.naming.*;
import javax.jms.*;
import java.util.*;
import java.io.*;

/**
 * JMSRequestHandler can be configured to listen for messages on a JMS queue,
 * and forward them on to a {@link MessageDelivery} implementation which will
 * generate a response. This object will then pass that response on to the
 * requestor.
 */
public class JMSRequestHandler implements XMLConfigurable, javax.jms.MessageListener {

    private String contextFactory;

    private String url;

    private String connectionFactory;

    private String queueName;

    private QueueConnection conn;

    private QueueSession session;

    private QueueReceiver receiver;

    private QueueSender sender;

    private MessageDelivery delivery;

    private static int messagesReceived = 0;

    /**
	 * Called when a JMS Message is placed on the queue. The method expects the
	 * Message to be a simple stream of bytes. The bytes are extracted from the
	 * Message, and sent via this object's MessageDelivery to the final request
	 * handler. The returned byte[] is then wrapped as a Message object and
	 * placed on the Message source's return queue.
	 */
    public void onMessage(javax.jms.Message msg) {
        try {
            BytesMessage bmsg = (BytesMessage) msg;
            javax.jms.Queue replyTo = (javax.jms.Queue) msg.getJMSReplyTo();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte b;
            try {
                while (true) {
                    bos.write(bmsg.readByte());
                }
            } catch (MessageEOFException e) {
            }
            byte[] response;
            try {
                response = delivery.send(bos.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
                response = "ERROR".getBytes();
            }
            synchronized (this) {
                BytesMessage reply = this.session.createBytesMessage();
                reply.writeBytes(response);
                this.sender.send(replyTo, reply);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Configures this object based on settings received from an XML Element.
	 * The following attributes must be present in the XML element and name a
	 * property in the server config file which contains the required value:<br>
	 *
	 * <ul><li>contextFactory - the class name of the JNDI InitialContextFactory
	 * which should be used to create initial context
	 * <li>url - the URL of the JNDI provider
	 * <li>connectionFactory - the JNDI name of a QueueConnectionFactory object
	 * <li>queueName - the JNDI name of the queue on which to listen for
	 * requests.</ul>
	 *
	 * @throws XMLConfigurationException if any of the above attributes are not
	 * present, or the properties they point to are not present in the server config file,
	 * or if an exception is raised while connecting to the message
	 * queue.
	 */
    public Object configure(Element element, XMLContext context) throws XMLConfigurationException {
        String contextFactoryProp = element.getAttribute("contextFactory");
        String urlProp = element.getAttribute("url");
        String connectionFactoryProp = element.getAttribute("connectionFactory");
        String queueNameProp = element.getAttribute("queueName");
        if (contextFactoryProp == null || urlProp == null || connectionFactoryProp == null || queueNameProp == null) {
            throw new XMLConfigurationException("JMSRequestHandler: Missing one or more attributes in XML config file");
        }
        this.contextFactory = ServerConfig.get(contextFactoryProp);
        this.url = ServerConfig.get(urlProp);
        this.connectionFactory = ServerConfig.get(connectionFactoryProp);
        this.queueName = ServerConfig.get(queueNameProp);
        if (this.contextFactory == null || this.url == null || this.connectionFactory == null || this.queueName == null) {
            throw new XMLConfigurationException("JMSRequestHandler: Missing one or more attributes in server config file");
        }
        try {
            Properties p = new Properties();
            p.put(Context.INITIAL_CONTEXT_FACTORY, this.contextFactory);
            p.put(Context.PROVIDER_URL, this.url);
            InitialContext ctx = new InitialContext(p);
            QueueConnectionFactory qcf = (QueueConnectionFactory) ctx.lookup(this.connectionFactory);
            javax.jms.Queue q = (javax.jms.Queue) ctx.lookup(this.queueName);
            this.conn = qcf.createQueueConnection();
            this.session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            this.receiver = session.createReceiver(q);
            this.sender = session.createSender(null);
            this.receiver.setMessageListener(this);
            this.conn.start();
        } catch (Exception e) {
            throw new XMLConfigurationException(e.getMessage());
        }
        return this;
    }

    /**
	 * Set the {@link MessageDelivery} implementation which will be used to forward
	 * requests from the JMS queue to the required service.
	 */
    public void setDelivery(MessageDelivery md) {
        this.delivery = md;
    }

    /**
	 * Register a child element in the XML config with this object. If the
	 * object is a MessageDelivery implementation, it is set as this objects'
	 * {@link MessageDelivery}
	 */
    public void registerChild(Object o) {
        if (o instanceof MessageDelivery) {
            this.setDelivery((MessageDelivery) o);
        }
    }
}
