package org.slasoi.common.messaging.pubsub;

import org.slasoi.common.messaging.Message;

public class PubSubMessage extends Message {

    private static final long serialVersionUID = 8072816867741537231L;

    private Channel channel;

    private String from;

    public PubSubMessage(String channelName, String payload) {
        super(payload);
        this.channel = new Channel(channelName);
    }

    public PubSubMessage() {
        super("");
        this.channel = new Channel();
    }

    public String getChannelName() {
        return channel.getName();
    }

    public void setChannelName(String channelName) {
        this.channel = new Channel(channelName);
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
