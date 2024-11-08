package org.eaiframework.impl;

import org.eaiframework.ChannelManager;
import org.eaiframework.LifecycleException;
import org.eaiframework.MessageProducerFactory;
import org.eaiframework.MessageSender;
import org.eaiframework.MessageSenderFactory;

/**
 * 
 */
public class MessageSenderFactoryImpl implements MessageSenderFactory {

    private ChannelManager channelManager;

    private MessageProducerFactory messageProducerFactory;

    public MessageSender createMessageSender() throws LifecycleException {
        MessageSenderImpl messageSender = new MessageSenderImpl();
        messageSender.setChannelManager(channelManager);
        messageSender.setMessageProducerFactory(messageProducerFactory);
        return messageSender;
    }

    /**
	 * @return the channelManager
	 */
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    /**
	 * @param channelManager the channelManager to set
	 */
    public void setChannelManager(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    /**
	 * @return the messageProducerFactory
	 */
    public MessageProducerFactory getMessageProducerFactory() {
        return messageProducerFactory;
    }

    /**
	 * @param messageProducerFactory the messageProducerFactory to set
	 */
    public void setMessageProducerFactory(MessageProducerFactory messageProducerFactory) {
        this.messageProducerFactory = messageProducerFactory;
    }
}
