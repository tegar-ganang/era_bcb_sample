package eu.medeia.mule.interceptor;

import java.io.IOException;
import java.io.InputStream;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * The Class ToJMSInInterceptor.
 */
public class ToJMSInInterceptor extends AbstractPhaseInterceptor<Message> {

    /**
	 * Instantiates a new to jms in interceptor.
	 */
    public ToJMSInInterceptor() {
        super(Phase.RECEIVE);
    }

    public void handleMessage(Message message) throws Fault {
        InputStream is = message.getContent(InputStream.class);
        if (is == null) {
            return;
        }
        CachedOutputStream bos = new CachedOutputStream();
        try {
            IOUtils.copy(is, bos);
            is.close();
            bos.close();
            sendMsg("Inbound Message \n" + "--------------" + bos.getOut().toString() + "\n--------------");
            message.setContent(InputStream.class, bos.getInputStream());
        } catch (IOException e) {
            throw new Fault(e);
        }
    }

    /** The ack mode. */
    private static int ackMode = Session.AUTO_ACKNOWLEDGE;

    /** The queue name. */
    private static String queueName = "instancemodels.queue";

    /** The producer. */
    private MessageProducer producer;

    /**
	 * Send msg.
	 * 
	 * @param msg
	 *            the msg
	 */
    private void sendMsg(String msg) {
        boolean transacted = false;
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://gaspra:61616");
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(transacted, ackMode);
            Destination instancemodelsQueue = session.createQueue(queueName);
            producer = session.createProducer(instancemodelsQueue);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            TextMessage message = session.createTextMessage();
            message.setText(msg);
            this.producer.send(message);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
