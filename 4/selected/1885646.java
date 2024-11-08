package com.ianzepp.logging.jms.appender;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

public class RemoteAppender extends AppenderSkeleton {

    private static final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG);

    private static String localHostName;

    private static final String NS_URI = "http://ianzepp.com/logging";

    /**
	 * 
	 * TODO Method description for <code>getDateFormat()</code>
	 * 
	 * @return
	 */
    public static DateFormat getDateFormat() {
        return dateFormat;
    }

    /**
	 * 
	 * TODO Method description for <code>getLocalHostName()</code>
	 * 
	 * @return
	 */
    public static String getLocalHostName() {
        return localHostName;
    }

    /**
	 * 
	 * TODO Method description for <code>setLocalHostName()</code>
	 * 
	 * @param localHostName
	 */
    public static void setLocalHostName(final String localHostName) {
        RemoteAppender.localHostName = localHostName;
    }

    private String brokerUri;

    private Queue queue;

    private QueueConnection queueConnection;

    private QueueConnectionFactory queueFactory;

    private String queueName;

    private QueueSender queueSender;

    private QueueSession queueSession;

    /**
	 * 
	 * Constructor for RemoteAppender
	 * 
	 */
    public RemoteAppender() {
        try {
            setLocalHostName(InetAddress.getLocalHost().getHostName());
        } catch (final UnknownHostException e) {
            setLocalHostName("unknown.local");
        }
    }

    /**
	 * 
	 * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
	 */
    @Override
    protected void append(final LoggingEvent event) {
        try {
            getQueueSender().send(getQueueSession().createTextMessage(convertToXml(event)));
        } catch (final JMSException e) {
            e.printStackTrace(new PrintStream(System.err));
        } catch (final XMLStreamException e) {
            e.printStackTrace(new PrintStream(System.err));
        }
    }

    /**
	 * 
	 * @see org.apache.log4j.AppenderSkeleton#close()
	 */
    @Override
    public void close() {
    }

    /**
	 * 
	 * TODO Method description for <code>convertToXml()</code>
	 * 
	 * @param event
	 * @return
	 * @throws XMLStreamException
	 * @throws IOException
	 */
    public String convertToXml(final LoggingEvent event) throws XMLStreamException {
        final StringWriter writerOut = new StringWriter(2048);
        final XMLOutputFactory factory = XMLOutputFactory.newInstance();
        final XMLStreamWriter writer = factory.createXMLStreamWriter(writerOut);
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("eventRequest");
        writer.writeAttribute("xmlns", NS_URI);
        writer.writeStartElement("host");
        writer.writeCharacters(getLocalHostName());
        writer.writeEndElement();
        writer.writeStartElement("logger");
        writer.writeCharacters(event.getLoggerName());
        writer.writeEndElement();
        writer.writeStartElement("level");
        writer.writeCharacters(event.getLevel().toString());
        writer.writeEndElement();
        writer.writeStartElement("message");
        writer.writeCharacters(event.getRenderedMessage());
        writer.writeEndElement();
        writer.writeStartElement("timestamp");
        writer.writeCharacters(getDateFormat().format(new Date()));
        writer.writeEndElement();
        writer.writeStartElement("thread");
        writer.writeCharacters(Thread.currentThread().getName());
        writer.writeEndElement();
        if (event.getMDC("service.projectName") != null) {
            writer.writeStartElement("project");
            writer.writeCharacters(event.getMDC("service.projectName").toString());
            writer.writeEndElement();
        }
        if (event.getMDC("service.name") != null) {
            writer.writeStartElement("service");
            writer.writeCharacters(event.getMDC("service.name").toString());
            writer.writeEndElement();
        }
        if (event.getMDC("message.correlationId") != null) {
            writer.writeStartElement("correlationId");
            writer.writeCharacters(event.getMDC("message.correlationId").toString());
            writer.writeEndElement();
        }
        if (event.getMDC("message.messageId") != null) {
            writer.writeStartElement("messageId");
            writer.writeCharacters(event.getMDC("message.messageId").toString());
            writer.writeEndElement();
        }
        final LocationInfo locationInfo = event.getLocationInformation();
        if (locationInfo != null) {
            writer.writeStartElement("location");
            if (locationInfo.getFileName() != null) {
                writer.writeStartElement("fileName");
                writer.writeCharacters(locationInfo.getFileName());
                writer.writeEndElement();
            }
            if (locationInfo.getClassName() != null) {
                writer.writeStartElement("className");
                writer.writeCharacters(locationInfo.getClassName());
                writer.writeEndElement();
            }
            if (locationInfo.getMethodName() != null) {
                writer.writeStartElement("methodName");
                writer.writeCharacters(locationInfo.getMethodName());
                writer.writeEndElement();
            }
            if (locationInfo.getLineNumber() != null) {
                writer.writeStartElement("lineNumber");
                writer.writeCharacters(locationInfo.getLineNumber());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        final ThrowableInformation throwableInfo = event.getThrowableInformation();
        if (throwableInfo != null) {
            writer.writeStartElement("exception");
            final String[] detail = event.getThrowableInformation().getThrowableStrRep();
            if (throwableInfo.getClass() != null) {
                writer.writeStartElement("exceptionName");
                writer.writeCharacters(throwableInfo.getThrowable().getClass().getName());
                writer.writeEndElement();
            }
            if (detail.length > 0) {
                writer.writeStartElement("message");
                writer.writeCharacters(detail[0]);
                writer.writeEndElement();
            }
            if (detail.length > 1) {
                writer.writeStartElement("detail");
                for (int i = 1; i < detail.length; i++) {
                    writer.writeCharacters(detail[i]);
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        writer.writeEndDocument();
        writer.close();
        return writerOut.toString();
    }

    /**
	 * 
	 * TODO Method description for <code>getBrokerUri()</code>
	 * 
	 * @return
	 */
    public String getBrokerUri() {
        return brokerUri;
    }

    /**
	 * 
	 * TODO Method description for <code>getTopicName()</code>
	 * 
	 * @return
	 */
    public String getQueueName() {
        return queueName;
    }

    /**
	 * 
	 * TODO Method description for <code>getQueueSender()</code>
	 * 
	 * @return
	 * @throws JMSException
	 */
    private synchronized QueueSender getQueueSender() throws JMSException {
        if (queue == null) {
            queue = new ActiveMQQueue(getQueueName());
        }
        if (queueSender == null) {
            queueSender = getQueueSession().createSender(queue);
        }
        return queueSender;
    }

    /**
	 * 
	 * TODO Method description for <code>getQueueSession()</code>
	 * 
	 * @return
	 * @throws JMSException
	 */
    private synchronized QueueSession getQueueSession() throws JMSException {
        if (queueFactory == null) {
            queueFactory = new ActiveMQConnectionFactory(getBrokerUri());
        }
        if (queueConnection == null) {
            queueConnection = queueFactory.createQueueConnection();
        }
        if (queueSession == null) {
            queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        }
        return queueSession;
    }

    /**
	 * 
	 * @see org.apache.log4j.AppenderSkeleton#requiresLayout()
	 */
    @Override
    public boolean requiresLayout() {
        return false;
    }

    /**
	 * 
	 * TODO Method description for <code>setBrokerUri()</code>
	 * 
	 * @param brokerUri
	 */
    public void setBrokerUri(final String brokerUri) {
        this.brokerUri = brokerUri;
    }

    /**
	 * 
	 * TODO Method description for <code>setTopicName()</code>
	 * 
	 * @param queueName
	 */
    public void setQueueName(final String queueName) {
        this.queueName = queueName;
    }
}
