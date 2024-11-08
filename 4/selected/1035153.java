package net.sourceforge.jcoupling2.adapter.jms;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.naming.NamingException;
import net.sourceforge.jcoupling2.adapter.MiddlewareAdapter;
import net.sourceforge.jcoupling2.exception.ConfigurationException;
import net.sourceforge.jcoupling2.exception.JCouplingException;
import net.sourceforge.jcoupling2.persistence.Channel;
import net.sourceforge.jcoupling2.persistence.DataMapper;
import net.sourceforge.jcoupling2.persistence.Message;
import net.sourceforge.jcoupling2.wca.TechnologyMetadata;
import org.apache.log4j.Logger;

/**
 * @author Lachlan Aldred
 */
public class LocalJMSCommunicationAdapter extends MiddlewareAdapter {

    private static Logger logger = Logger.getLogger(LocalJMSCommunicationAdapter.class);

    private static LocalJMSCommunicationAdapter _instance;

    private static BootstrapJMS _bootstrapJMS;

    private DataMapper dataMapper = null;

    public LocalJMSCommunicationAdapter() throws JCouplingException {
        super(TechnologyMetadata.JMSTechnologyMetaData);
        try {
            _bootstrapJMS = new BootstrapJMS("tcp://radix:3035");
        } catch (Throwable e) {
            logger.error("Error connecting to JMS.", e);
            throw new JCouplingException("Error connecting to JMS.", e);
        }
        dataMapper = new DataMapper();
    }

    public boolean isSendEnabled() {
        return true;
    }

    public boolean isReceiveEnabled() {
        return true;
    }

    /**
	 * einreihen
	 */
    public void enqueue(Message message, Channel channel) {
        try {
            _bootstrapJMS.enqueue(_bootstrapJMS.getJmsDestination(channel.getChannelName()), message.getBody());
        } catch (NamingException e) {
            throw new RuntimeException("Couldn't find queue named [" + channel.getChannelName() + "].", e);
        } catch (JMSException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void initialiseThis(Channel channel) throws ConfigurationException {
        logger.debug("initialising channel: " + channel.getChannelName());
        if (channel.supportsInbound()) {
            logger.debug(channel.getChannelName() + " supports inbound so setting up JMS listener.");
            JMSMessageListener listener = new JMSMessageListener(channel, this);
            try {
                Destination destination = _bootstrapJMS.getJmsDestination(channel.getChannelName());
                _bootstrapJMS.registerMessageHandler(listener, destination);
            } catch (NamingException e) {
                throw new ConfigurationException("Failed to find channel [" + channel.getChannelName() + "]. " + e.getMessage(), e);
            } catch (JMSException e) {
                throw new ConfigurationException("Failed registed channel [" + channel.getChannelName() + "] with JMS server.");
            }
        } else {
            logger.warn(channel.getChannelName() + " does not support inbound so not setting up a listener.");
        }
    }

    @Override
    public void notify(Message jcouplingMessage, Channel channel) {
        try {
            dataMapper.addMessage(jcouplingMessage);
        } catch (JCouplingException e) {
            logger.error("ERROR", e);
        }
    }

    @Override
    public void enqueuechannel(net.sourceforge.jcoupling2.persistence.Message message) {
    }
}
