package org.slasoi.orcsample.pac.messagelisteners;

import org.apache.log4j.Logger;
import org.slasoi.common.messaging.pubsub.MessageEvent;
import org.slasoi.common.messaging.pubsub.PubSubMessage;
import org.slasoi.orcsample.pac.messagefilters.IMessageFilter;

public class PubSubMessageListener extends ORCMessageListener implements org.slasoi.common.messaging.pubsub.MessageListener {

    private static Logger logger = Logger.getLogger(PubSubMessageListener.class.getName());

    public PubSubMessageListener() {
    }

    public PubSubMessageListener(IMessageFilter filter) {
        super(filter);
    }

    public void processMessage(MessageEvent messageEvent) {
        PubSubMessage message = messageEvent.getMessage();
        logger.info(message.getChannelName());
        logger.info(message.getPayload());
        setMessage(message);
    }
}
