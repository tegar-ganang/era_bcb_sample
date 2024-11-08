package jerklib.events.impl;

import java.util.List;
import java.util.Map;
import jerklib.Channel;
import jerklib.Session;
import jerklib.events.ModeEvent;

public class ModeEventImpl implements ModeEvent {

    private final Type type = Type.MODE_EVENT;

    private final Session session;

    private final String rawEventData, setBy;

    private final Channel channel;

    private final Map<String, List<String>> modes;

    public ModeEventImpl(String rawEventData, Session session, Map<String, List<String>> modes, String setBy, Channel channel) {
        this.rawEventData = rawEventData;
        this.session = session;
        this.modes = modes;
        this.setBy = setBy;
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    public Map<String, List<String>> getModeMap() {
        return modes;
    }

    public String setBy() {
        return setBy;
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
