package jerklib.events.impl;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.TopicEvent;
import java.util.Date;

public class TopicEventImpl implements TopicEvent {

    private String setBy, data, hostname;

    private Date setWhen;

    private Session session;

    private Channel channel;

    private StringBuffer buff = new StringBuffer();

    public TopicEventImpl(String rawEventData, Session session, Channel channel, String topic) {
        this.data = rawEventData;
        this.session = session;
        this.channel = channel;
        buff.append(topic);
    }

    public String getTopic() {
        return buff.toString();
    }

    public String getHostName() {
        return hostname;
    }

    public String getRawEventData() {
        return data;
    }

    public Session getSession() {
        return session;
    }

    public Type getType() {
        return Type.TOPIC;
    }

    public void setSetWhen(String setWhen) {
        this.setWhen = new Date(1000L * Long.parseLong(setWhen));
    }

    public void setSetBy(String setBy) {
        this.setBy = setBy;
    }

    public String getSetBy() {
        return setBy;
    }

    public Date getSetWhen() {
        return setWhen;
    }

    public Channel getChannel() {
        return channel;
    }

    public void appendToTopic(String topic) {
        buff.append(topic);
    }

    public int hashCode() {
        return channel.hashCode();
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof TopicEventImpl && o.hashCode() == hashCode()) {
            return ((TopicEvent) o).getChannel().equals(getChannel());
        }
        return false;
    }
}
