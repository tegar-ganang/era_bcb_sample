package com.outlandr.irc.client.replies;

public class RplTopic implements RplServerReply {

    private String topic;

    private String channelName;

    public RplTopic(String part1, String topic) {
        String[] temp = part1.split(" ");
        this.channelName = temp[3];
        this.topic = topic;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getTopic() {
        return topic;
    }

    @Override
    public int getReplyId() {
        return RPL_TOPIC;
    }
}
