package jerklib;

import jerklib.events.*;
import jerklib.events.IRCEvent.Type;
import jerklib.events.impl.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class IRCEventFactory {

    private static ConnectionManager myManager;

    static void setManager(ConnectionManager manager) {
        myManager = manager;
    }

    static final Logger log = Logger.getLogger(IRCEventFactory.class.getName());

    static final Pattern nickInUsePattern = Pattern.compile("\\S+\\s433\\s.*?\\s(\\S+)\\s:?.*$");

    static final Pattern serverVersionPattern = Pattern.compile("^:\\S+\\s351\\s\\S+\\s(\\S+)\\s(\\S+)\\s:(.*)$");

    static final Pattern connectionCompletePattern = Pattern.compile(":(\\S+)\\s+001\\s+\\S+\\s+:.*$");

    static final Pattern whoPattern = Pattern.compile("^:.+?\\s+352\\s+.+?\\s+(.+?)\\s+(.+?)\\s+(.+?)\\s+(.+?)\\s+(.+?)\\s+(.+?):(\\d+)\\s+(.+)$");

    static final Pattern whowasPattern = Pattern.compile("^:\\S+\\s314\\s\\S+\\s(\\S+)\\s(\\S+)\\s(\\S+).+?:(.*)$");

    static final Pattern numericPattern = Pattern.compile("^:\\S+\\s\\d{3}\\s\\S+\\s(.*)$");

    static final Pattern awayPattern306305 = Pattern.compile("^:\\S+\\s\\d{3}\\s+(\\S+)\\s:(.*)$");

    static final Pattern awayPattern301 = Pattern.compile("^:\\S+\\s+\\d{3}\\s+\\S+\\s+(\\S+)\\s+:(.*)$");

    static final Pattern motdPattern = Pattern.compile(":(\\S+)\\s+\\d+\\s+(\\S+)\\s+:(.*)$");

    static final Pattern channelListPattern = Pattern.compile("^:\\S+\\s322\\s\\S+\\s(\\S+)\\s(\\d+)\\s:(.*)$");

    static final Pattern joinPattern = Pattern.compile("^:(\\S+?)!(\\S+?)@(\\S+)\\s+JOIN\\s+:?(\\S+)$");

    static final Pattern whoisPattern = Pattern.compile("^:\\S+\\s\\d{3}\\s\\S+\\s(\\S+)\\s(\\S+)\\s(\\S+).*?:(.*)$");

    static final Pattern nickListPattern = Pattern.compile("^:(?:.*?)\\s+366\\s+\\S+\\s+(.*?)\\s+.*$");

    static final Pattern kickPattern = Pattern.compile("^:(\\S+?)!(\\S+?)@(\\S+)\\s+KICK\\s+(\\S+)\\s+(\\S+)\\s+:(.*)$");

    static final Pattern topicPattern = Pattern.compile(":(\\S+)\\s+332\\s+(\\S+)\\s+(\\S+)\\s+:(.*)$");

    static final Pattern privmsgPattern = Pattern.compile("^:(\\S+?)\\!(\\S+?)@(\\S+)\\s+PRIVMSG\\s+(\\S+)\\s+:(.*)$");

    static final Pattern noticePattern1 = Pattern.compile("^NOTICE\\s+(.*$)$");

    static final Pattern noticePattern2 = Pattern.compile("^:(.*?)\\!.*?\\s+NOTICE\\s+(.*?)\\s+:(.*)$");

    static final Pattern noticePattern3 = Pattern.compile("^:(.*?)\\!.*?\\s+NOTICE\\s+(.*?)\\s+:(.*)$");

    static final Pattern noticePattern4 = Pattern.compile("^:(\\S+)\\s+NOTICE\\s+(\\S+)\\s+:(.*)$");

    static final Pattern quitPattern = Pattern.compile("^:(\\S+?)!(\\S+?)@(\\S+)\\s+QUIT\\s+:(.*)$");

    static final Pattern partPattern = Pattern.compile("^:(\\S+?)!(\\S+?)@(\\S+)\\s+PART\\s+:?(\\S+?)(?:\\s+:(.*))?$");

    static final Pattern nickChangePattern = Pattern.compile("^:(\\S+)!(\\S+)@(\\S+)\\s+NICK\\s+:(.*)$");

    static final Pattern invitePattern = Pattern.compile("^:(\\S+?)!(\\S+?)@(\\S+)\\s+INVITE.+?:(.*)$");

    static ServerVersionEvent serverVersion(String data, Connection con) {
        Matcher m = serverVersionPattern.matcher(data);
        if (m.matches()) {
            return new ServerVersionEventImpl(m.group(3), m.group(2), m.group(1), "", data, myManager.getSessionFor(con));
        }
        debug("SERVER_VERSION", data);
        return null;
    }

    static ConnectionCompleteEvent connectionComplete(String data, Connection con) {
        Matcher m = connectionCompletePattern.matcher(data);
        if (m.matches()) {
            ConnectionCompleteEvent e = new ConnectionCompleteEventImpl(data, m.group(1).toLowerCase(), myManager.getSessionFor(con), con.getHostName());
            return e;
        }
        debug("CONN_COMPLETE", data);
        return null;
    }

    static WhoEvent who(String data, Connection con) {
        Matcher m = whoPattern.matcher(data);
        if (m.matches()) {
            boolean away = m.group(6).charAt(0) == 'G';
            return new WhoEventImpl(m.group(1), Integer.parseInt(m.group(7)), m.group(3), away, m.group(5), data, m.group(8), m.group(4), myManager.getSessionFor(con), m.group(2));
        }
        debug("WHO", data);
        return null;
    }

    static WhowasEvent whowas(String data, Connection con) {
        Matcher m = whowasPattern.matcher(data);
        if (m.matches()) {
            return new WhowasEventImpl(m.group(3), m.group(2), m.group(1), m.group(4), data, myManager.getSessionFor(con));
        }
        debug("WHOWAS", data);
        return null;
    }

    static NumericErrorEvent numericError(String data, Connection con, int numeric) {
        Matcher m = numericPattern.matcher(data);
        if (m.matches()) {
            return new NumericEventImpl(m.group(1), data, numeric, myManager.getSessionFor(con));
        }
        debug("NUMERIC ERROR", data);
        return null;
    }

    static WhoisEvent whois(String data, Session session) {
        Matcher m = whoisPattern.matcher(data);
        if (m.matches()) {
            return new WhoisEventImpl(m.group(1), m.group(4), m.group(2), m.group(3), data, session);
        }
        debug("WHOIS", data);
        return null;
    }

    static NickListEvent nickList(String data, Connection con) {
        Matcher m = nickListPattern.matcher(data);
        if (m.matches()) {
            NickListEvent nle = new NickListEventImpl(data, myManager.getSessionFor(con), con.getChannel(m.group(1).toLowerCase()), con.getChannel(m.group(1).toLowerCase()).getNicks());
            return nle;
        }
        debug("NICK_LIST", data);
        return null;
    }

    static KickEvent kick(String data, Connection con) {
        System.out.println("IN KICK()");
        Matcher m = kickPattern.matcher(data);
        if (m.matches()) {
            System.out.println("CONNECTION: " + con);
            System.out.println("myManager: " + myManager);
            String channelName = m.group(4).toLowerCase();
            Channel c = con.getChannel(channelName);
            Session session = myManager.getSessionFor(con);
            System.out.println("SESSION RETRIEVED: " + session);
            KickEvent ke = new KickEventImpl(data, session, m.group(1), m.group(2), m.group(3), m.group(5), m.group(6), c);
            log.severe("BUILT EVENT: " + ke.toString());
            return ke;
        }
        debug("KICK", data);
        return null;
    }

    static TopicEvent topic(String data, Connection con) {
        Matcher m = topicPattern.matcher(data);
        if (m.matches()) {
            TopicEvent topicEvent = new TopicEventImpl(data, myManager.getSessionFor(con), con.getChannel(m.group(3).toLowerCase()), m.group(4));
            return topicEvent;
        }
        debug("TOPIC", data);
        return null;
    }

    static MessageEvent privateMsg(String data, Connection con, String channelPrefixRegex) {
        if (data.matches("^:\\S+\\s+PRIVMSG\\s+\\S+\\s+:.*$")) {
            Matcher m = privmsgPattern.matcher(data);
            m.matches();
            String target = m.group(4);
            return new MessageEventImpl(target.matches("^" + channelPrefixRegex + "{1}.+") ? con.getChannel(target.toLowerCase()) : null, m.group(3), m.group(5), m.group(1), data, myManager.getSessionFor(con), target.matches("^" + channelPrefixRegex + "{1}.+") ? Type.CHANNEL_MESSAGE : Type.PRIVATE_MESSAGE, m.group(2));
        }
        debug("MESSAGE", data);
        return null;
    }

    static CtcpEvent ctcp(MessageEvent event, String ctcpString) {
        return new CtcpEventImpl(ctcpString, event.getHostName(), event.getMessage(), event.getNick(), event.getUserName(), event.getRawEventData(), event.getChannel(), event.getSession());
    }

    static NickInUseEvent nickInUse(String data, Connection con) {
        Matcher m = nickInUsePattern.matcher(data);
        if (m.matches()) {
            return new NickInUseEventImpl(m.group(1), data, myManager.getSessionFor(con));
        }
        debug("NICK_IN_USE", data);
        return null;
    }

    static AwayEvent away(String data, Connection con, int numeric) {
        Matcher m = awayPattern306305.matcher(data);
        if (m.matches()) {
            switch(numeric) {
                case 305:
                    return new AwayEventImpl(myManager.getSessionFor(con), AwayEvent.EventType.RETURNED_FROM_AWAY, false, true, myManager.getDefaultProfile().getActualNick(), data);
                case 306:
                    return new AwayEventImpl(myManager.getSessionFor(con), AwayEvent.EventType.WENT_AWAY, true, true, myManager.getDefaultProfile().getActualNick(), data);
            }
        }
        m = awayPattern301.matcher(data);
        m.matches();
        return new AwayEventImpl(m.group(2), AwayEvent.EventType.USER_IS_AWAY, true, false, m.group(1), data, myManager.getSessionFor(con));
    }

    static MotdEvent motd(String data, Connection con) {
        Matcher m = motdPattern.matcher(data);
        if (!m.matches()) {
            debug("MOTD", data);
            return null;
        }
        return new MotdEventImpl(data, myManager.getSessionFor(con), m.group(3), m.group(1));
    }

    static NoticeEvent notice(String data, Connection con) {
        Matcher m = noticePattern1.matcher(data);
        if (m.matches()) {
            NoticeEvent noticeEvent = new NoticeEventImpl(data, myManager.getSessionFor(con), "generic", m.group(1), "", "", null);
            return noticeEvent;
        }
        m = noticePattern2.matcher(data);
        if (m.matches()) {
            NoticeEvent ne = new NoticeEventImpl(data, myManager.getSessionFor(con), "channel", m.group(3), "", m.group(1), con.getChannel(m.group(2).toLowerCase()));
            return ne;
        }
        m = noticePattern3.matcher(data);
        if (m.matches()) {
            NoticeEvent ne = new NoticeEventImpl(data, myManager.getSessionFor(con), "user", m.group(3), m.group(2), m.group(1), null);
            return ne;
        }
        m = noticePattern4.matcher(data);
        if (m.matches()) {
            NoticeEvent ne = new NoticeEventImpl(data, myManager.getSessionFor(con), "user", m.group(3), m.group(2), m.group(1), null);
            return ne;
        }
        debug("NOTICE", data);
        return null;
    }

    static void ServerTimeEvent() {
    }

    static QuitEvent quit(String data, Connection con) {
        Matcher matcher = quitPattern.matcher(data);
        if (matcher.matches()) {
            List<Channel> chanList = con.removeNickFromAllChannels(matcher.group(1));
            QuitEvent quitEvent = new QuitEventImpl(data, myManager.getSessionFor(con), matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), chanList);
            debug("QUIT", data);
            return quitEvent;
        }
        return null;
    }

    static JoinEvent regularJoin(String data, Connection con) {
        Matcher m = joinPattern.matcher(data);
        if (m.matches()) {
            try {
                JoinEvent joinEvent = new JoinEventImpl(data, myManager.getSessionFor(con), m.group(1), m.group(2), m.group(3).toLowerCase(), con.getChannel(m.group(4).toLowerCase()).getName(), con.getChannel(m.group(4).toLowerCase()));
                return joinEvent;
            } catch (Exception e) {
                System.err.println(data);
                for (Channel chan : con.getChannels()) {
                    System.err.println(chan.getName());
                }
                e.printStackTrace();
            }
        }
        debug("JOIN_EVENT", data);
        return null;
    }

    static ChannelListEvent chanList(String data, Connection con) {
        if (log.isLoggable(Level.FINE)) {
            log.fine(data);
        }
        Matcher m = channelListPattern.matcher(data);
        if (m.matches()) {
            return new ChannelListEventImpl(data, m.group(1), m.group(3), Integer.parseInt(m.group(2)), myManager.getSessionFor(con));
        }
        debug("CHAN_LIST", data);
        return null;
    }

    static JoinCompleteEvent joinCompleted(String data, Connection con, String nick, Channel channel) {
        return new JoinCompleteEventImpl(data, myManager.getSessionFor(con), channel);
    }

    static PartEvent part(String data, Connection con) {
        Matcher m = partPattern.matcher(data);
        if (m.matches()) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("HERE? " + m.group(4));
                log.fine(data);
            }
            PartEvent partEvent = new PartEventImpl(data, myManager.getSessionFor(con), m.group(1), m.group(2), m.group(3), con.getChannel(m.group(4).toLowerCase()).getName(), con.getChannel(m.group(4).toLowerCase()), m.group(5));
            return partEvent;
        } else {
            log.severe("NO MATCH");
        }
        debug("PART", data);
        return null;
    }

    static NickChangeEvent nickChange(String data, Connection con) {
        Matcher m = nickChangePattern.matcher(data);
        if (m.matches()) {
            NickChangeEvent nickChangeEvent = new NickChangeEventImpl(data, myManager.getSessionFor(con), m.group(1), m.group(4), m.group(3), m.group(2));
            return nickChangeEvent;
        }
        debug("NICK_CHANGE", data);
        return null;
    }

    static InviteEvent invite(String data, Connection con) {
        Matcher m = invitePattern.matcher(data);
        if (m.matches()) {
            return new InviteEventImpl(m.group(4).toLowerCase(), m.group(1), m.group(2), m.group(3), data, myManager.getSessionFor(con));
        }
        debug("INVITE", data);
        return null;
    }

    private static void debug(String method, String data) {
        if (!ConnectionManager.debug) {
            return;
        }
        log.info("Returning null from " + method + " in IRCEventFactory. Offending data:");
        log.info(data);
    }
}
