package jerklib.events.impl;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.IRCEvent;
import jerklib.events.MessageEvent;

/**
 * Created: Feb 6, 2008 9:50:27 PM
 *
 * @author <a href="mailto:robby.oconnor@gmail.com">Robert O'Connor</a>
 */
public class MessageEventImpl implements MessageEvent {

    private final String nick, userName, hostName, message, rawEventData;

    private final Channel channel;

    private final Type type;

    private final Session session;

    public MessageEventImpl(Channel channel, String hostName, String message, String nick, String rawEventData, Session session, IRCEvent.Type type, String userName) {
        this.channel = channel;
        this.hostName = hostName;
        this.message = message;
        this.nick = nick;
        this.rawEventData = rawEventData;
        this.session = session;
        this.type = type;
        this.userName = userName;
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

    public String getRawEventData() {
        return rawEventData;
    }

    public Type getType() {
        return type;
    }

    public String getUserName() {
        return userName;
    }

    public Session getSession() {
        return session;
    }
}
