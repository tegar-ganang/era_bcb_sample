package jerklib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
import jerklib.ServerInformation.ModeType;
import jerklib.events.IRCEvent;
import jerklib.events.JoinEvent;
import jerklib.events.KickEvent;
import jerklib.events.ModeEvent;
import jerklib.events.NickChangeEvent;
import jerklib.events.PartEvent;
import jerklib.events.QuitEvent;
import jerklib.events.TopicEvent;
import jerklib.events.ConnectionCompleteEvent;
import jerklib.events.MessageEvent;
import jerklib.events.impl.ModeEventImpl;
import jerklib.events.impl.ServerInformationEventImpl;
import jerklib.events.impl.TopicEventImpl;
import jerklib.events.impl.WhoisEventImpl;

/**
 * InternalEventParser is the first place IRCEvents are sent. Here they are
 * parsed and made into more specific events then re-dispachted to any listeners
 * via the ConnectionManager.
 * 
 * This class is mostly convience for users of the lib , however some internal
 * stuff depends on this class as well. Like keeping track of nicks in a channel
 * or knowing when a connection is ready to join channels etc.
 * 
 * Thanks to Gracenotes for the help
 * @author mohadib
 * 
 */
public class InternalEventParser {

    private ConnectionManager manager;

    private Map<Channel, TopicEvent> topicMap = new HashMap<Channel, TopicEvent>();

    private WhoisEventImpl we;

    Logger log = Logger.getLogger(this.getClass().getName());

    public InternalEventParser(ConnectionManager manager) {
        this.manager = manager;
    }

    /**
	 * Takes an IRCEvent and tries to parse it into a more specific event then
	 * redispatch the more specfic event.
	 * 
	 * @param event
	 *          <code>IRCEvent</code> the event to parse
	 * 
	 */
    void parseEvent(IRCEvent event) {
        Session session = event.getSession();
        System.out.println("PARSEEVENT1: " + session);
        System.out.println("PARSEEVENT2: " + session.getConnection());
        Connection con = session.getConnection();
        String data = event.getRawEventData();
        String nick = con.getProfile().getActualNick();
        String[] tokens = data.split("\\s+");
        if (tokens.length > 1) {
            if (tokens[1].matches("^\\d{3}$")) {
                numericEvent(data, con, event, Integer.parseInt(tokens[1]));
            } else {
                String command = tokens[1];
                if (command.equals("PRIVMSG")) {
                    message(data, con);
                } else if (command.equals("QUIT")) {
                    QuitEvent qEvent = IRCEventFactory.quit(data, con);
                    con.removeNickFromAllChannels(qEvent.getWho());
                    manager.addToRelayList(qEvent);
                } else if (command.equals("JOIN")) {
                    Pattern p = Pattern.compile("^:\\Q" + nick + "\\E\\!.*?\\s+JOIN\\s+:?(\\S+)$");
                    Matcher m = p.matcher(data);
                    if (m.matches()) {
                        Channel channel = new Channel(m.group(1).toLowerCase(), manager.getSessionFor(con));
                        con.addChannel(channel);
                        manager.getSessionFor(con).addChannelName(channel.getName());
                        manager.addToRelayList(IRCEventFactory.joinCompleted(data, con, nick, channel));
                    } else {
                        JoinEvent jEvent = IRCEventFactory.regularJoin(data, con);
                        jEvent.getChannel().addNick(jEvent.getNick());
                        manager.addToRelayList(jEvent);
                    }
                } else if (command.equals("MODE")) {
                    mode(event);
                } else if (command.equals("PART")) {
                    PartEvent pEvent = IRCEventFactory.part(data, con);
                    if (!pEvent.getChannel().removeNick(pEvent.getWho())) {
                        System.err.println("Could Not remove nick " + pEvent.getWho() + " from " + pEvent.getChannelName());
                    }
                    if (pEvent.getWho().equalsIgnoreCase(nick)) {
                        con.removeChannel(pEvent.getChannel());
                        manager.getSessionFor(con).removeChannelName(pEvent.getChannelName());
                    }
                    manager.addToRelayList(pEvent);
                } else if (command.equals("NOTICE")) {
                    manager.addToRelayList(IRCEventFactory.notice(data, con));
                } else if (command.equals("TOPIC")) {
                    Pattern p = Pattern.compile("^.+?TOPIC\\s+(.+?)\\s+.*$");
                    Matcher m = p.matcher(data);
                    m.matches();
                    event.getSession().sayRaw("TOPIC " + m.group(1));
                } else if (command.equals("INVITE")) {
                    manager.addToRelayList(IRCEventFactory.invite(data, con));
                } else if (command.equals("NICK")) {
                    NickChangeEvent nEvent = IRCEventFactory.nickChange(data, con);
                    con.nickChanged(nEvent.getOldNick(), nEvent.getNewNick());
                    if (nEvent.getOldNick().equals(nick)) {
                        event.getSession().updateProfileSuccessfully(true);
                    }
                    manager.addToRelayList(nEvent);
                } else if (command.equals("KICK")) {
                    KickEvent ke = IRCEventFactory.kick(data, con);
                    if (!ke.getChannel().removeNick(ke.getWho())) {
                        log.info("COULD NOT REMOVE NICK " + ke.getWho() + " from channel " + ke.getChannel().getName());
                    }
                    if (ke.getWho().equals(nick)) {
                        con.removeChannel(ke.getChannel());
                        if (manager.getSessionFor(con).isRejoinOnKick()) manager.getSessionFor(con).join(ke.getChannel().getName());
                    }
                    manager.addToRelayList(ke);
                } else if (data.matches("^PING.*")) {
                    con.pong(event);
                    manager.addToRelayList(event);
                } else if (data.matches(".*PONG.*")) {
                    con.gotPong();
                    manager.addToRelayList(event);
                } else if (data.matches("^NOTICE\\s+(.*$)$")) {
                    manager.addToRelayList(IRCEventFactory.notice(data, con));
                } else {
                    manager.addToRelayList(event);
                }
            }
        }
    }

    private void channelMode(IRCEvent event) {
        Pattern p = Pattern.compile("^\\S+\\s+\\S+\\s+\\S+\\s+(\\S+)\\s+(.+)$");
        Matcher m = p.matcher(event.getRawEventData());
        m.matches();
        event.getSession().getChannel(m.group(1)).setModeString(m.group(2));
        ModeEvent me = new ModeEventImpl(event.getRawEventData(), event.getSession(), null, null, event.getSession().getChannel(m.group(1)));
        manager.addToRelayList(me);
    }

    private void mode(IRCEvent event) {
        if (log.isLoggable(Level.FINE)) {
            log.fine(event.getRawEventData());
        }
        String[] rawTokens = event.getRawEventData().split("\\s+");
        String[] rawModeTokens = rawTokens[3].split("");
        String[] modeTokens = new String[rawModeTokens.length - 1];
        System.arraycopy(rawModeTokens, 1, modeTokens, 0, rawModeTokens.length - 1);
        String[] arguments = new String[rawTokens.length - 4];
        System.arraycopy(rawTokens, 4, arguments, 0, arguments.length);
        Map<String, List<String>> modeMap = new HashMap<String, List<String>>();
        ServerInformation info = event.getSession().getServerInformation();
        String[] channelPrefixes = info.getChannelPrefixes();
        boolean userMode = true;
        for (String prefix : channelPrefixes) {
            if (rawTokens[2].startsWith(prefix)) {
                userMode = false;
            }
        }
        if (userMode) {
            if (log.isLoggable(Level.INFO)) {
                log.info("MODE  " + Arrays.toString(modeTokens));
            }
            if (modeTokens[0].equals(":")) {
                String[] cleanTokens = new String[modeTokens.length - 1];
                System.arraycopy(modeTokens, 1, cleanTokens, 0, modeTokens.length - 1);
                modeTokens = cleanTokens;
            }
            char action = '+';
            List<String> targets = new ArrayList<String>();
            targets.add(event.getSession().getNick());
            for (String mode : modeTokens) {
                if (mode.equals("+") || mode.equals("-")) action = mode.charAt(0); else {
                    modeMap.put(action + mode, targets);
                }
            }
            ModeEvent me = new ModeEventImpl(event.getRawEventData(), event.getSession(), modeMap, rawTokens[0].substring(1).split("\\!")[0], null);
            manager.addToRelayList(me);
            return;
        }
        char action = '+';
        int argumntOffset = 0;
        for (String mode : modeTokens) {
            if (mode.equals("+") || mode.equals("-")) action = mode.charAt(0); else {
                ModeType type = info.getTypeForMode(mode);
                if (type == ModeType.GROUP_A || type == ModeType.GROUP_B) {
                    List<String> modeArgs = modeMap.get(action + mode);
                    if (modeArgs == null) modeArgs = new ArrayList<String>();
                    modeArgs.add(arguments[argumntOffset]);
                    System.err.println("Mode " + action + mode + " " + arguments[argumntOffset]);
                    argumntOffset++;
                    modeMap.put(action + mode, modeArgs);
                } else if (type == ModeType.GROUP_C) {
                    List<String> modeArgs = modeMap.get(action + mode);
                    if (modeArgs == null) modeArgs = new ArrayList<String>();
                    if (action == '-') {
                        if (!modeMap.containsKey(action + mode)) {
                            modeMap.put(action + mode, new ArrayList<String>());
                            System.err.println("Mode " + action + mode);
                        }
                    } else {
                        modeArgs.add(arguments[argumntOffset]);
                        System.err.println("Mode " + action + mode + " " + arguments[argumntOffset]);
                        argumntOffset++;
                        modeMap.put(action + mode, modeArgs);
                    }
                } else if (type == ModeType.GROUP_D) {
                    modeMap.put(action + mode, new ArrayList<String>());
                    System.err.println("Mode " + action + mode);
                } else {
                    System.err.println("unreconzied mode " + mode);
                }
            }
        }
        Channel chan = event.getSession().getChannel(rawTokens[2]);
        List<String> voicedNicks = modeMap.get("+v") == null ? new ArrayList<String>() : modeMap.get("+v");
        List<String> opedNicks = modeMap.get("+o") == null ? new ArrayList<String>() : modeMap.get("+o");
        List<String> devoicedNicks = modeMap.get("-v") == null ? new ArrayList<String>() : modeMap.get("-v");
        List<String> deopedNicks = modeMap.get("-o") == null ? new ArrayList<String>() : modeMap.get("-o");
        for (String nick : voicedNicks) {
            chan.updateUsersMode(nick, "+v");
        }
        for (String nick : opedNicks) {
            chan.updateUsersMode(nick, "+o");
        }
        for (String nick : devoicedNicks) {
            chan.updateUsersMode(nick, "-v");
        }
        for (String nick : deopedNicks) {
            chan.updateUsersMode(nick, "-o");
        }
        ModeEvent me = new ModeEventImpl(event.getRawEventData(), event.getSession(), modeMap, rawTokens[0].substring(1).split("\\!")[0], chan);
        manager.addToRelayList(me);
    }

    private void whois(String data, Session session, int numeric) {
        switch(numeric) {
            case 311:
                {
                    we = (WhoisEventImpl) IRCEventFactory.whois(data, session);
                    break;
                }
            case 319:
                {
                    System.err.println(data);
                    Pattern p = Pattern.compile("^:\\S+\\s\\d{3}\\s\\S+\\s\\S+\\s:(.*)$");
                    Matcher m = p.matcher(data);
                    if (we != null && m.matches()) {
                        List<String> chanNames = Arrays.asList(m.group(1).split("\\s+"));
                        we.setChannelNamesList(chanNames);
                        we.appendRawEventData(data);
                    }
                    break;
                }
            case 312:
                {
                    Pattern p = Pattern.compile("^:\\S+\\s\\d{3}\\s\\S+\\s\\S+\\s(\\S+)\\s:(.*)$");
                    Matcher m = p.matcher(data);
                    if (we != null && m.matches()) {
                        we.setWhoisServer(m.group(1));
                        we.setWhoisServerInfo(m.group(2));
                        we.appendRawEventData(data);
                    }
                    break;
                }
            case 320:
                {
                    Pattern p = Pattern.compile("^:\\S+\\s\\d{3}\\s\\S+\\s(\\S+)\\s:(.*)$");
                    Matcher m = p.matcher(data);
                    if (we != null && m.matches()) {
                        we.appendRawEventData(data);
                    }
                    break;
                }
            case 317:
                {
                    Pattern p = Pattern.compile("^:\\S+\\s\\d{3}\\s\\S+\\s\\S+\\s+(\\d+)\\s+(\\d+)\\s+:.*$");
                    Matcher m = p.matcher(data);
                    if (we != null && m.matches()) {
                        we.setSignOnTime(Integer.parseInt(m.group(2)));
                        we.setSecondsIdle(Integer.parseInt(m.group(1)));
                    }
                }
            case 318:
                {
                    if (we != null) {
                        we.appendRawEventData(data);
                        manager.addToRelayList(we);
                        we = null;
                    }
                    break;
                }
            default:
                log.info(data.toString());
                break;
        }
    }

    private void serverInfo(String data, IRCEvent event) {
        Session session = (Session) event.getSession();
        session.getServerInformation().parseServerInfo(data);
        ServerInformationEventImpl se = new ServerInformationEventImpl(session, data, session.getServerInformation());
        manager.addToRelayList(se);
    }

    private void numericEvent(String data, Connection con, IRCEvent event, int numeric) {
        switch(numeric) {
            case 001:
                connectionComplete(data, con, event);
                break;
            case 005:
                serverInfo(data, event);
                break;
            case 301:
                manager.addToRelayList(IRCEventFactory.away(data, con, numeric));
                break;
            case 305:
                manager.addToRelayList(IRCEventFactory.away(data, con, numeric));
                break;
            case 306:
                manager.addToRelayList(IRCEventFactory.away(data, con, numeric));
                break;
            case 314:
                manager.addToRelayList(IRCEventFactory.whowas(data, con));
                break;
            case 311:
            case 312:
            case 317:
            case 318:
            case 319:
            case 320:
                whois(data, event.getSession(), numeric);
                break;
            case 321:
                break;
            case 322:
                manager.addToRelayList(IRCEventFactory.chanList(data, con));
                break;
            case 323:
                break;
            case 324:
                channelMode(event);
                break;
            case 332:
                firstPartOfTopic(data, con);
                break;
            case 333:
                secondPartOfTopic(data, con);
                break;
            case 351:
                manager.addToRelayList(IRCEventFactory.serverVersion(data, con));
                break;
            case 352:
                manager.addToRelayList(IRCEventFactory.who(data, con));
                break;
            case 353:
                namesLine(data, con);
                break;
            case 366:
                manager.addToRelayList(IRCEventFactory.nickList(data, con));
                break;
            case 372:
            case 375:
            case 376:
                manager.addToRelayList(IRCEventFactory.motd(data, con));
                break;
            case 433:
                nick(data, con, event.getSession());
                break;
            case 401:
            case 402:
            case 403:
            case 404:
            case 405:
            case 406:
            case 407:
            case 409:
            case 411:
            case 412:
            case 413:
            case 414:
            case 421:
            case 422:
            case 423:
            case 424:
            case 431:
            case 432:
            case 436:
            case 441:
            case 442:
            case 443:
            case 444:
            case 445:
            case 446:
            case 451:
            case 461:
            case 462:
            case 463:
            case 464:
            case 465:
            case 467:
            case 471:
            case 472:
            case 473:
            case 474:
            case 475:
            case 481:
            case 482:
            case 483:
            case 491:
            case 501:
            case 502:
                manager.addToRelayList(IRCEventFactory.numericError(data, con, numeric));
                break;
            default:
                manager.addToRelayList(event);
        }
    }

    private void firstPartOfTopic(String data, Connection con) {
        TopicEvent tEvent = IRCEventFactory.topic(data, con);
        if (topicMap.containsValue(tEvent.getChannel())) {
            ((TopicEventImpl) topicMap.get(tEvent.getChannel())).appendToTopic(tEvent.getTopic());
        } else {
            topicMap.put(tEvent.getChannel(), tEvent);
        }
    }

    private void secondPartOfTopic(String data, Connection con) {
        Pattern p = Pattern.compile(":(\\S+)\\s+333\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)$");
        Matcher m = p.matcher(data);
        m.matches();
        Channel chan = (Channel) con.getChannel(m.group(3).toLowerCase());
        if (topicMap.containsKey(chan)) {
            TopicEventImpl tEvent = (TopicEventImpl) topicMap.get(chan);
            topicMap.remove(chan);
            tEvent.setSetBy(m.group(4));
            tEvent.setSetWhen(m.group(5));
            chan.setTopicEvent(tEvent);
            manager.addToRelayList(tEvent);
        }
    }

    Random rand = new Random();

    private void nick(String data, Connection con, Session session) {
        if (session.isConnected() && session.isProfileUpdating()) {
            session.updateProfileSuccessfully(false);
        }
        if (!con.loggedInSuccessfully()) {
            Profile p = session.getRequestedConnection().getProfile();
            String aNick = p.getActualNick();
            String newNick = p.getFirstNick() + rand.nextInt(100);
            if (aNick.equals(p.getFirstNick())) {
                newNick = p.getSecondNick();
            } else if (aNick.equals(p.getSecondNick())) {
                newNick = p.getThirdNick();
            }
            ((ProfileImpl) p).setActualNick(newNick);
            session.changeProfile(p);
        }
        manager.addToRelayList(IRCEventFactory.nickInUse(data, con));
    }

    private void connectionComplete(String data, Connection con, IRCEvent event) {
        ConnectionCompleteEvent ccEvent = IRCEventFactory.connectionComplete(data, con);
        con.loginSuccess();
        con.setHostName(ccEvent.getActualHostName());
        manager.addToRelayList(ccEvent);
    }

    private void message(String data, Connection con) {
        ServerInformation serverInfo = manager.getSessionFor(con).getServerInformation();
        String[] chanPrefixes = serverInfo.getChannelPrefixes();
        String channelPrefixRegex = "";
        if (chanPrefixes != null) {
            for (String prefix : serverInfo.getChannelPrefixes()) {
                channelPrefixRegex += prefix;
            }
            channelPrefixRegex = "[" + channelPrefixRegex + "]";
        }
        MessageEvent me = IRCEventFactory.privateMsg(data, con, channelPrefixRegex);
        String msg = me.getMessage();
        if (msg.startsWith("")) {
            String ctcpString = msg.substring(0, msg.length() - 1).substring(1);
            if (ctcpString.equals("VERSION")) {
                me.getSession().sayRaw("NOTICE " + me.getNick() + " :\001VERSION " + ConnectionManager.getVersion() + "\001\r\n");
            } else if (ctcpString.equals("PING")) {
                me.getSession().sayRaw("NOTICE " + me.getNick() + " :\001PING \001\r\n");
            }
            me = IRCEventFactory.ctcp(me, ctcpString);
        }
        manager.addToRelayList(me);
    }

    private void namesLine(String data, Connection con) {
        Pattern p = Pattern.compile("^:(?:.+?)\\s+353\\s+\\S+\\s+(?:=|@)\\s+(\\S+)\\s:(.+)$");
        Matcher m = p.matcher(data);
        if (m.matches()) {
            Channel chan = con.getChannel(m.group(1).toLowerCase());
            String[] names = m.group(2).split("\\s+");
            for (String name : names) {
                if (name != null && name.length() > 0) {
                    chan.addNick(name);
                }
            }
        }
    }
}
