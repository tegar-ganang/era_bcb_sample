package com.outlandr.irc.client.events;

import com.outlandr.irc.client.Channel;

public class NameReplyEvent extends Event {

    private Channel channel;

    public NameReplyEvent(Channel channel) {
        this.channel = channel;
    }

    public String getChannel() {
        return channel.getName();
    }

    public String[] getChannelMembers() {
        return channel.getMembers();
    }
}
