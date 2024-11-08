package jerklib.events.impl;

import jerklib.Session;
import jerklib.events.WhoisEvent;
import java.util.Date;
import java.util.List;

public class WhoisEventImpl implements WhoisEvent {

    private final Type type = Type.WHOIS_EVENT;

    private final String host, user, realName, nick;

    private final Session session;

    private String whoisServer, whoisServerInfo, rawEventData;

    private List<String> channelNames;

    private boolean isOp;

    private long secondsIdle;

    private int signOnTime;

    public WhoisEventImpl(String nick, String realName, String user, String host, String rawEventData, Session session) {
        this.nick = nick;
        this.realName = realName;
        this.user = user;
        this.host = host;
        this.session = session;
        this.rawEventData = rawEventData;
    }

    public List<String> getChannelNames() {
        return channelNames;
    }

    public void setChannelNamesList(List<String> chanNames) {
        channelNames = chanNames;
    }

    public String getHost() {
        return host;
    }

    public String getUser() {
        return user;
    }

    public String getRealName() {
        return realName;
    }

    public String getNick() {
        return nick;
    }

    public boolean isAnOperator() {
        return isOp;
    }

    public boolean isIdle() {
        return secondsIdle > 0;
    }

    public long secondsIdle() {
        return secondsIdle;
    }

    public void setSecondsIdle(int secondsIdle) {
        this.secondsIdle = secondsIdle();
    }

    public Date signOnTime() {
        return new Date(1000L * signOnTime);
    }

    public void setSignOnTime(int signOnTime) {
        this.signOnTime = signOnTime;
    }

    public String whoisServer() {
        return whoisServer;
    }

    public void setWhoisServer(String whoisServer) {
        this.whoisServer = whoisServer;
    }

    public String whoisServerInfo() {
        return whoisServerInfo;
    }

    public void setWhoisServerInfo(String whoisServerInfo) {
        this.whoisServerInfo = whoisServerInfo;
    }

    public String getRawEventData() {
        return rawEventData;
    }

    public void appendRawEventData(String rawEventData) {
        this.rawEventData += "\r\n" + rawEventData;
    }

    public Session getSession() {
        return session;
    }

    public Type getType() {
        return type;
    }
}
