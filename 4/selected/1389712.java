package com.jetigy.magicbus.event.bus;

public class ChannelEventType {

    private String name;

    private String channel;

    /**
   * 
   * @param name
   * @param channel
   */
    protected ChannelEventType(String name, String channel) {
        this.name = name;
        this.channel = channel;
    }

    /**
   * @return Returns the channel.
   */
    public String getChannel() {
        return channel;
    }

    /**
   * @return Returns the name.
   */
    public String getName() {
        return name;
    }
}
