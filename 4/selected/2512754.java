package com.jirclib.event;

/**
 * Created by IntelliJ IDEA.
 * User: Aelin
 * Date: May 12, 2009
 * Time: 3:11:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChannelMessageEvent extends MessageEvent {

    protected String channel;

    public ChannelMessageEvent(String sender, String message, String channel) {
        super(sender, message);
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }
}
