package net.sourceforge.sandirc;

import jerklib.Channel;
import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.ProfileImpl;
import jerklib.Session;
import net.sourceforge.sandirc.gui.IRCWindow;
import net.sourceforge.sandirc.gui.SandIRCFrame;
import net.sourceforge.sandirc.gui.text.IRCDocument;
import net.sourceforge.sandirc.utils.DateUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Propri�taire
 */
public class UserInputHandler implements InputListener {

    private static UserInputHandler defaultHandler;

    static InputListener join = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "$") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session selectedSession = window.getSession();
            selectedSession.joinChannel(tokens[0]);
        }
    };

    static InputListener connect = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.NONE, "$ $?") {

        protected void handleInput(IRCWindow window, final String[] tokens) {
            new Thread(new Runnable() {

                public void run() {
                    ConnectionManager manager = SandIRCFrame.getInstance().getConnectionManager();
                    if (tokens[1] != null) {
                        manager.requestConnection(tokens[0], Integer.parseInt(tokens[1])).addIRCEventListener(new IRCEventHandler());
                    } else {
                        manager.requestConnection(tokens[0]).addIRCEventListener(new IRCEventHandler());
                    }
                }
            }).start();
        }
    };

    static InputListener changeNick = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "$") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session selectedSession = window.getSession();
            Profile current = selectedSession.getRequestedConnection().getProfile();
            Profile newProfile = new ProfileImpl(current.getName(), tokens[0], current.getSecondNick(), current.getThirdNick());
            System.out.println("Changing nick to " + tokens[0]);
            selectedSession.changeProfile(newProfile);
        }
    };

    static InputListener msg = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "$ *") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session selectedSession = window.getSession();
            Channel channel = selectedSession.getChannel(tokens[0]);
            if (channel != null) {
                channel.say(tokens[1]);
            } else {
                selectedSession.sayPrivate(tokens[0], tokens[1]);
            }
            window.insertDefault("[msg >>> " + tokens[0] + "]: " + tokens[1]);
        }
    };

    static InputListener notice = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "$ *") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session selectedSession = window.getSession();
            selectedSession.notice(tokens[0], tokens[1]);
        }
    };

    static InputListener mode = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.CHANNEL, "#? *") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session selectedSession = window.getSession();
            Channel channel = selectedSession.getChannel(tokens[0]);
            if ((tokens[1] != null) && (channel != null)) {
                selectedSession.mode(channel, tokens[1]);
            } else if (channel != null) {
                selectedSession.sayRaw("MODE " + tokens[0]);
            }
        }
    };

    static InputListener quit = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "*?") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session selectedSession = window.getSession();
            selectedSession.close((tokens[0] == null) ? "" : tokens[0]);
        }
    };

    static InputListener part = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.CHANNEL, "*?") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Channel selectedChannel = window.getChannel();
            selectedChannel.part("[SandIRC Client Leaving...] Go and get it, you know you want it...");
        }
    };

    static InputListener quote = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "*") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session selectedSession = window.getSession();
            selectedSession.sayRaw(tokens[0]);
            window.insertDefault("[quote]: " + tokens[0]);
        }
    };

    static InputListener me = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "*") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Channel channel = window.getChannel();
            String privNick = window.getNick();
            Session session = window.getSession();
            String ourNick = session.getNick();
            String input = (char) 1 + "ACTION " + tokens[0] + (char) 1;
            if (channel != null) {
                System.out.println("channel not null");
                channel.say(input);
                window.insertDefault("* " + ourNick + " " + tokens[0]);
            } else if (privNick != null) {
                System.out.println("trying other");
                session.sayPrivate(privNick, input);
                window.insertDefault("* " + ourNick + " " + tokens[0]);
            }
        }
    };

    static InputListener ctcp = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "$ *") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            String input = (char) 1 + tokens[1] + (char) 1;
            Session selectedSession = window.getSession();
            Channel channel = selectedSession.getChannel(tokens[0]);
            if (channel != null) {
                channel.say(input);
            } else {
                selectedSession.sayPrivate(tokens[0], input);
            }
            window.insertDefault("[ctcp >>> " + tokens[0] + "]: " + tokens[1]);
        }
    };

    static InputListener nickserv = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "*") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session session = window.getSession();
            session.sayRaw("nickserv " + tokens[0] + "\r\n");
        }
    };

    static InputListener memoserv = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "*") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session session = window.getSession();
            session.sayRaw("memoserv " + tokens[0] + "\r\n");
        }
    };

    static InputListener topic = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.CHANNEL, "*") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Channel channel = window.getChannel();
            if (channel == null) {
                window.insertDefault("You're not in a channel");
            } else {
                window.insertDefault("Topic for " + channel.getName() + ": " + channel.getTopic() + "\nSet by " + channel.getTopicSetter() + " on " + DateUtils.getTime(channel.getTopicSetTime()));
            }
        }
    };

    static InputListener whois = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "*") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session session = window.getSession();
            session.whois(tokens[0] + " " + tokens[0]);
        }
    };

    static InputListener chanserv = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "*") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session session = window.getSession();
            session.sayRaw("chanserv " + tokens[0] + "\r\n");
        }
    };

    static InputListener away = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "*?") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session session = window.getSession();
            if (tokens[0] == null) {
                session.unsetAway();
            } else {
                session.setAway(tokens[0]);
            }
        }
    };

    static InputListener invite = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.CHANNEL, "$ #?") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session session = window.getSession();
            session.invite(tokens[0], session.getChannel(tokens[1]));
        }
    };

    static InputListener cycle = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.CHANNEL, "*?") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session session = window.getSession();
            Channel channel = window.getChannel();
            session.partChannel(channel, tokens[0] == null ? "" : tokens[0]);
            session.joinChannel(channel.getName());
        }
    };

    static InputListener channelList = new AbstractCommandHandler(AbstractCommandHandler.AcceptedView.SESSION, "*?") {

        protected void handleInput(IRCWindow window, String[] tokens) {
            Session session = window.getSession();
            if (tokens[0] == null) {
                session.channelList();
            } else {
                session.channelList(tokens[0]);
            }
        }
    };

    static InputListener defaultNormal = new InputListener() {

        public void receiveInput(IRCWindow window, String input) {
            if (window == null) {
                throw new IllegalArgumentException("Junk not recognized");
            }
            Channel channel = window.getChannel();
            String privNick = window.getNick();
            Session session = window.getSession();
            String ourNick = session.getNick();
            if (channel != null) {
                System.out.println("channel not null");
                channel.say(input);
                window.insertMsg(ourNick, input);
            } else if (privNick != null) {
                System.out.println("trying other");
                session.sayPrivate(privNick, input);
                window.insertMsg(ourNick, input);
            }
        }
    };

    private Map<String, InputListener> cmdMap;

    private InputListener normal;

    public UserInputHandler() {
        this.cmdMap = new HashMap<String, InputListener>();
    }

    public static synchronized UserInputHandler getDefault() {
        if (defaultHandler == null) {
            defaultHandler = new UserInputHandler();
            defaultHandler.useDefaultMap();
        }
        return defaultHandler;
    }

    public void put(String command, InputListener handler) {
        cmdMap.put(command, handler);
    }

    public InputListener get(String command) {
        return cmdMap.get(command);
    }

    public void remove(String command) {
        cmdMap.remove(command);
    }

    private void useDefaultMap() {
        this.cmdMap = new HashMap<String, InputListener>();
        this.normal = defaultNormal;
        cmdMap.put("away", away);
        cmdMap.put("chanserv", chanserv);
        cmdMap.put("connect", connect);
        cmdMap.put("cs", chanserv);
        cmdMap.put("ctcp", ctcp);
        cmdMap.put("cycle", cycle);
        cmdMap.put("invite", invite);
        cmdMap.put("j", join);
        cmdMap.put("join", join);
        cmdMap.put("list", channelList);
        cmdMap.put("me", me);
        cmdMap.put("memoserv", memoserv);
        cmdMap.put("ms", memoserv);
        cmdMap.put("msg", msg);
        cmdMap.put("nickserv", nickserv);
        cmdMap.put("notice", notice);
        cmdMap.put("ns", nickserv);
        cmdMap.put("mode", mode);
        cmdMap.put("nick", changeNick);
        cmdMap.put("q", quit);
        cmdMap.put("quit", quit);
        cmdMap.put("quote", quote);
        cmdMap.put("p", part);
        cmdMap.put("part", part);
        cmdMap.put("raw", quit);
        cmdMap.put("say", defaultNormal);
        cmdMap.put("server", connect);
        cmdMap.put("t", topic);
        cmdMap.put("topic", topic);
        cmdMap.put("whois", whois);
    }

    /**
     * @param input
     */
    public void receiveInput(IRCWindow window, String input) {
        try {
            if (input.startsWith("/")) {
                String[] tokens = input.split("\\s+", 2);
                String command = tokens[0].substring(1);
                InputListener il = cmdMap.get(command);
                if (il != null) {
                    System.out.println("cmdMap, command: " + command);
                    il.receiveInput(window, (tokens.length == 2) ? tokens[1] : "");
                } else {
                    window.insertDefault("*** Command failed: not recognized ***");
                }
            } else {
                normal.receiveInput(window, input);
            }
        } catch (Exception ex) {
            if (window != null) {
                window.insertDefault("*** Command failed: " + ex.getMessage() + " ***");
            }
        }
    }
}
