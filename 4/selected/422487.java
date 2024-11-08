package jerklib.events.impl;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.CtcpEvent;

public class CtcpEventImpl implements CtcpEvent {

    private String ctcpString, hostName, message, nick, userName, rawEventData;

    private Channel channel;

    private Session session;

    public CtcpEventImpl(String ctcpString, String hostName, String message, String nick, String userName, String rawEventData, Channel channel, Session session) {
        super();
        this.ctcpString = ctcpString;
        this.hostName = hostName;
        this.message = message;
        this.nick = nick;
        this.userName = userName;
        this.rawEventData = rawEventData;
        this.channel = channel;
        this.session = session;
    }

    public String getCtcpString() {
        return ctcpString;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getHostName() {
        return hostName;
    }

    public String getMessage() {
        return message;
    }

    public String getNick() {
        return nick;
    }

    public String getUserName() {
        return userName;
    }

    public String getRawEventData() {
        return rawEventData;
    }

    public Session getSession() {
        return session;
    }

    public Type getType() {
        return Type.CTCP_EVENT;
    }
}
