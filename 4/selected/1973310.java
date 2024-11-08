package org.slasoi.orcmockup.pac.impl;

import org.apache.log4j.Logger;
import org.slasoi.common.messaging.pubsub.MessageEvent;
import org.slasoi.common.messaging.pubsub.PubSubMessage;

public class ORCMessageListener {

    final int wait_time = 50000;

    final int wait_step = 100;

    protected volatile Object obj = new Object();

    private volatile org.slasoi.common.messaging.Message message = null;

    protected IMessageFilter filter;

    public ORCMessageListener() {
    }

    public ORCMessageListener(IMessageFilter filter) {
        this.filter = filter;
    }

    public org.slasoi.common.messaging.Message getMessage() {
        int time_left = wait_time;
        while (time_left > 0) {
            synchronized (obj) {
                if (message != null) {
                    break;
                }
            }
            try {
                Thread.sleep(wait_step);
                time_left -= wait_step;
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        System.out.println(String.format("ORCMessageListener: WAITED %d MS", wait_time - time_left));
        return message;
    }

    protected void setMessage(org.slasoi.common.messaging.Message message) {
        synchronized (obj) {
            if (filter == null || filter.isAcceptable(message)) {
                this.message = message;
            }
        }
    }
}

class PPMessageListener extends ORCMessageListener implements org.slasoi.common.messaging.pointtopoint.MessageListener {

    private static Logger logger = Logger.getLogger(ProvisioningAdjustmentImpl.class.getName());

    public void processMessage(org.slasoi.common.messaging.pointtopoint.MessageEvent messageEvent) {
        setMessage(messageEvent.getMessage());
        logger.info(messageEvent.getMessage().getPayload());
    }
}

class PubSubMessageListener extends ORCMessageListener implements org.slasoi.common.messaging.pubsub.MessageListener {

    private static Logger logger = Logger.getLogger(ProvisioningAdjustmentImpl.class.getName());

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
