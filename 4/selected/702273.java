package jerklib.events.impl;

import java.util.List;
import jerklib.Channel;
import jerklib.Session;
import jerklib.events.NickListEvent;

public class NickListEventImpl implements NickListEvent {

    private final Type type = Type.NICK_LIST_EVENT;

    private final List<String> nicks;

    private final Channel channel;

    private final String rawEventData;

    private final Session session;

    public NickListEventImpl(String rawEventData, Session session, Channel channel, List<String> nicks) {
        this.rawEventData = rawEventData;
        this.session = session;
        this.channel = channel;
        this.nicks = nicks;
    }

    public Channel getChannel() {
        return channel;
    }

    public List<String> getNicks() {
        return nicks;
    }

    public String getRawEventData() {
        return rawEventData;
    }

    public Session getSession() {
        return session;
    }

    public Type getType() {
        return type;
    }
}
