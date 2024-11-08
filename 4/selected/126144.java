package org.xaware.server.engine.channel.smtp;

import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import org.xaware.server.engine.IScopedChannel;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This class implements IScopedChannel for SMTP. It allows us to
 * keep a transport open as long as necessary, providing access to the SMTP
 * client.
 * 
 * @author dwieland
 */
public class SmtpTemplate implements IScopedChannel {

    private static String className = SmtpTemplate.class.getName();

    private static XAwareLogger lf = XAwareLogger.getXAwareLogger(className);

    /** Our Channel object holds properties */
    SmtpChannelSpecification channelObject = null;

    /** Don't initialize more than once */
    boolean initialized = false;

    /** The transport we will give out to whoever wants it */
    Transport m_mailTransport;

    /**
	 * Constructor you can't call
	 */
    private SmtpTemplate() {
    }

    /**
	 * Constructor you must call
	 */
    public SmtpTemplate(SmtpBizDriver driver) throws XAwareException {
        super();
        channelObject = (SmtpChannelSpecification) driver.createChannelObject();
    }

    /**
	 * implement the interface's getScopedChannelType() Get the scoped channel
	 * type for this scoped channel instance.
	 * 
	 * @return the IScopedChannel.Type for this IScopedChannel instance.
	 */
    public Type getScopedChannelType() {
        return Type.SMTP;
    }

    /**
	 * implement the interface's close() - close open stream
	 * 
	 */
    public void close(boolean success) {
        if (m_mailTransport != null) {
            try {
                m_mailTransport.close();
            } catch (MessagingException e) {
                lf.debug("Transport Close failed with exception: " + e.getMessage());
            }
        }
        initialized = false;
    }

    /**
	 * Initialize ourself
	 * 
	 * @throws XAwareException
	 */
    public void initResources() throws XAwareException {
        if (!initialized) {
            final Properties props = new Properties();
            props.setProperty(SmtpChannelSpecification.AUTH_KEY, channelObject.getProperty(SmtpChannelSpecification.AUTH_KEY));
            final Session session = Session.getDefaultInstance(props, null);
            String sServer = channelObject.getProperty(XAwareConstants.BIZDRIVER_SERVER);
            int port = Integer.parseInt(channelObject.getProperty(XAwareConstants.BIZDRIVER_PORT));
            String sUserId = channelObject.getProperty(XAwareConstants.BIZDRIVER_USER);
            String sPassword = channelObject.getProperty(XAwareConstants.BIZDRIVER_PWD);
            try {
                m_mailTransport = session.getTransport("smtp");
                m_mailTransport.connect(sServer, port, sUserId, sPassword);
            } catch (NoSuchProviderException e) {
                String msg = "Exception initializing transport: " + e.getMessage();
                lf.debug(msg);
                throw new XAwareException(msg);
            } catch (MessagingException e) {
                String msg = "Exception initializing transport: " + e.getMessage();
                lf.debug(msg);
                throw new XAwareException(msg);
            }
            initialized = true;
        }
    }

    /**
     * @return the channelObject
     */
    public SmtpChannelSpecification getChannelObject() {
        return channelObject;
    }

    /**
     * @return the connector
     */
    public Transport getMailTransport() throws XAwareException {
        if (!initialized) {
            initResources();
        }
        return m_mailTransport;
    }
}
