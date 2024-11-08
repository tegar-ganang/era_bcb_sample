package jerklib.events.impl;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.IRCEvent;
import jerklib.events.JoinEvent;

public class JoinEventImpl implements JoinEvent {

    private final Type type = IRCEvent.Type.JOIN;

    private final String rawEventData, who, channelName, hostName, username;

    private final Session session;

    private final Channel chan;

    private String passwd;

    public JoinEventImpl(String rawEventData, Session session, String who, String username, String hostName, String channelName, Channel chan) {
        this.rawEventData = rawEventData;
        this.session = session;
        this.who = who;
        this.username = username;
        this.hostName = hostName;
        this.channelName = channelName;
        this.chan = chan;
    }

    public JoinEventImpl(String rawEventData, Session session, String who, String username, String hostName, String channelName, String passwd, Channel chan) {
        this.rawEventData = rawEventData;
        this.session = session;
        this.who = who;
        this.username = username;
        this.hostName = hostName;
        this.channelName = channelName;
        this.chan = chan;
        this.passwd = passwd;
    }

    public final String getPass() {
        return passwd;
    }

    public final String getNick() {
        return who;
    }

    public String getHostName() {
        return hostName;
    }

    public final String getChannelName() {
        return channelName;
    }

    public final Channel getChannel() {
        return chan;
    }

    public final Type getType() {
        return type;
    }

    public final String getRawEventData() {
        return rawEventData;
    }

    public final Session getSession() {
        return session;
    }

    public String toString() {
        return rawEventData;
    }

    public String getUserName() {
        return username;
    }
}
