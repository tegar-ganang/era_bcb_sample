package jerklib.events.impl;

import jerklib.events.ChannelListEvent;
import jerklib.events.IRCEvent;
import jerklib.Session;

public class ChannelListEventImpl implements ChannelListEvent {

    private final Session session;

    private final String rawEventData, channelName, topic;

    private final int numUsers;

    private final Type type = IRCEvent.Type.CHANNEL_LIST_EVENT;

    public ChannelListEventImpl(String rawEventData, String channelName, String topic, int numUsers, Session session) {
        this.rawEventData = rawEventData;
        this.session = session;
        this.channelName = channelName;
        this.topic = topic;
        this.numUsers = numUsers;
    }

    public String getChannelName() {
        return channelName;
    }

    public int getNumberOfUser() {
        return numUsers;
    }

    public String getTopic() {
        return topic;
    }

    public Type getType() {
        return type;
    }

    public String getRawEventData() {
        return rawEventData;
    }

    public Session getSession() {
        return session;
    }
}
