package de.jochenbrissier.backyard.core;

public class ChannelListenerBuffer {

    ChannelListener cl;

    String channel;

    public ChannelListenerBuffer() {
    }

    public ChannelListenerBuffer(String channel, ChannelListener cl) {
        this.cl = cl;
        this.channel = channel;
    }

    public ChannelListener getCl() {
        return cl;
    }

    public void setCl(ChannelListener cl) {
        this.cl = cl;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
