package com.outlandr.irc.client.events;

public class UpdateChatEvent extends Event {

    private String text;

    private String channel;

    public UpdateChatEvent(String channel, String text) {
        this.channel = channel;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public String getChannel() {
        return channel;
    }
}
