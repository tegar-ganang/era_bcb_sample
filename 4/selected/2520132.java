package org.eaiframework.impl;

import java.util.HashMap;
import java.util.Map;
import org.eaiframework.Channel;
import org.eaiframework.ChannelManager;
import org.eaiframework.Message;
import org.eaiframework.MessageException;
import org.eaiframework.MessageProducer;
import org.eaiframework.MessageProducerFactory;
import org.eaiframework.MessageSender;

/**
 * 
 */
public class MessageSenderImpl implements MessageSender {

    private Map<String, MessageProducer> messageProducers = new HashMap<String, MessageProducer>();

    private ChannelManager channelManager;

    private MessageProducerFactory messageProducerFactory;

    public MessageSenderImpl() {
    }

    public void sendMessage(Message message, String channelId) throws MessageException {
        Channel channel = channelManager.getChannel(channelId);
        if (channel == null) {
            throw new MessageException("Channnel '" + channelId + "' not found.");
        }
        MessageProducer messageProducer = messageProducers.get(channelId);
        if (messageProducer == null) {
            messageProducer = messageProducerFactory.createMessageProducer();
            messageProducers.put(channelId, messageProducer);
        }
        message.setDestination(channelId);
        messageProducer.produceMessage(message);
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
