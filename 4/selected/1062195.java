package jerklib.events.impl;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.IRCEvent;
import jerklib.events.NoticeEvent;

public class NoticeEventImpl implements NoticeEvent {

    private final Type type = IRCEvent.Type.NOTICE;

    private final String rawEventData, noticeType, message, toWho, byWho;

    private final Session session;

    private final Channel channel;

    public NoticeEventImpl(String rawEventData, Session session, String noticeType, String message, String toWho, String byWho, Channel channel) {
        this.rawEventData = rawEventData;
        this.session = session;
        this.noticeType = noticeType;
        this.message = message;
        this.toWho = toWho;
        this.byWho = byWho;
        this.channel = channel;
    }

    public String getNoticeType() {
        return noticeType;
    }

    public String getNoticeMessage() {
        return message;
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

    public String toString() {
        return rawEventData;
    }

    public String byWho() {
        return byWho;
    }

    public Channel getChannel() {
        return channel;
    }

    public String toWho() {
        return toWho;
    }
}
