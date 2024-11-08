package net.sourceforge.libairc;

import net.sourceforge.libairc.invokers.*;
import net.sourceforge.libairc.events.*;
import java.util.*;
import java.net.*;
import java.io.*;

/**
 * The client class is the heart of libairc
 * It represents a connection to an IRC server and allows
 * you to join channels, receive messages, send commands, and so forth
 * 
 * @author alx
 * @author p-static
 */
public class Client {

    /**
	 * Connection to the IRC server
	 */
    private Socket connection;

    /**
	 * Info we used to connect
	 */
    private ConnectInfo info;

    /**
	 * List of channels
	 */
    private Map channels;

    /**
	 * List of users
	 * TODO we really shouldn't care about this
	 */
    private Map users;

    /**
	 * Incoming messages thread
	 */
    private Incoming incoming;

    /**
	 * Outgoing messages thread
	 */
    private Outgoing outgoing;

    /**
	 * Main queue
	 */
    private PrioritizedQueue queue;

    /**
	 * Event queue
	 */
    private EventQueue events;

    /**
	 * Lets ping the server thread
	 */
    private Pinger pinger;

    /**
	 * Our own nick
	 */
    private String nick;

    /**
	 * List of channel message listeners
	 */
    private List lsnChanMsgs;

    /**
	 * List of channel event listeners
	 */
    private List lsnChanEvents;

    /**
	 * List of CTCP listeners
	 */
    private List lsnCTCPs;

    /**
	 * List of private message listeners
	 */
    private List lsnPMs;

    /**
	 * List of CTCP Version listeners
	 */
    private List lsnCTCPVs;

    /**
	 * NickServ object
	 */
    private NickServ nickserv;

    /**
	 * ChanServ object
	 */
    private ChanServ chanserv;

    /**
	 * Local address
	 */
    private InetAddress localAddress;

    /**
	 * DCC Manager
	 */
    private DCCManager dccManager;

    /**
	 * Default constructor, takes no parameters
	 * All connection info is passed inside a ConnectInfo object
	 */
    public Client() {
        channels = new HashMap();
        users = new HashMap();
        lsnChanMsgs = new ArrayList();
        lsnChanEvents = new ArrayList();
        lsnCTCPs = new ArrayList();
        lsnPMs = new ArrayList();
        lsnCTCPVs = new ArrayList();
        events = new EventQueue();
        localAddress = null;
    }

    /**
	 * Connect to an IRC server
	 * 
	 * @param info A ConnectInfo object containing all the information about nickname, login, realname, etc.
	 */
    public void connect(ConnectInfo info) {
        this.info = info;
        connect();
    }

    /**
	 * Connect to an IRC server using connection info already set
	 */
    public boolean connect() {
        if (info == null) {
            return false;
        }
        String nick = "NICK " + info.getNick();
        this.nick = info.getNick();
        String user = "USER " + info.getLogin() + " 8 * :" + info.getName();
        try {
            connection = new Socket(info.getServer(), info.getPort());
        } catch (UnknownHostException ex) {
            libairc.exception("Hostname resolution of: " + info.getServer(), ex);
            return false;
        } catch (IOException ex) {
            libairc.exception("Connecting to server", ex);
            return false;
        }
        incoming = new Incoming(this, connection);
        outgoing = new Outgoing(this, connection, info.getNumPriorities());
        queue = outgoing.getQueue();
        pinger = new Pinger(queue, info.getPingDelay(), info.getPingPriority());
        Catcher catcher = incoming.catchMessages();
        events.start();
        incoming.start();
        queue.send(user, 0);
        queue.send(nick, 0);
        addCTCPListener(getDCCManager());
        incoming.stopCatchingMessages(catcher);
        return true;
    }

    /**
	 * Called internally when a disconnect from the server is detected
	 *
	 */
    public void disconnected() {
        if (info.getAutoReconnect()) {
            while (!connect()) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    /**
	 * Join a channel at a certain priority
	 *
	 * @param channel the channel to join
	 * @param priority the priority to join at
	 */
    public void join(String channel, int priority) {
        queue.send("JOIN " + channel, priority);
        queue.send("MODE " + channel, priority);
        queue.send("MODE " + channel + " b", priority);
    }

    /**
	 * Sends a raw command to the IRC server at the specified priority
	 * 
	 * @param raw The raw line from the IRC protocol to send. Use this lightly.
	 * @param priority The priority to send the line at (0 - info.getNumPriorities())
	 */
    public void sendRaw(String raw, int priority) {
        queue.send(raw, priority);
    }

    /**
	 * Sends a set of raw commands to the IRC server at the specified priority
	 * 
	 * @param list A list of lines from the IRC protocol to send. Use this lightly.
	 * @param priority The priority to send the line at (0 - info.getNumPriorities())
	 */
    public void sendRaw(List list, int priority) {
        queue.send(list, priority);
    }

    /**
	 * Get a channel we've joined
	 *
	 * @param channel the name of the channel to get
	 * @return the channel object, or null if not found
	 */
    public Channel getChannel(String channel) {
        return getChannel(channel, false);
    }

    /**
	 * Get a channel with the potential to create it
	 *
	 * @param channel the channel name
	 * @param create whether or not to create a new channel object and add it if we haven't heard of that channel before
	 * @return the Channel object
	 */
    private Channel getChannel(String channel, boolean create) {
        channel = channel.toLowerCase();
        if (channels.containsKey(channel)) {
            return (Channel) channels.get(channel);
        } else if (create) {
            channels.put(channel, new Channel(this, channel));
            return (Channel) channels.get(channel);
        } else {
            return null;
        }
    }

    /**
	 * Get a user we might know about
	 *
	 * @param nick the nick of the user
	 * @return the User object
	 */
    public User getUser(String nick) {
        if (!users.containsKey(nick)) {
            return null;
        }
        return (User) users.get(nick);
    }

    /**
	 * Get our current nickname
	 *
	 * @return the current nickname
	 */
    public String getNick() {
        return nick;
    }

    /**
	 * Get the nickserv object
	 *
	 * @return the nickserv object
	 */
    public NickServ nickServ() {
        if (nickserv == null) {
            nickserv = new NickServ(this);
        }
        return nickserv;
    }

    /**
	 * Get the chanserv object
	 *
	 * @return the chanserv object
	 */
    public ChanServ chanServ() {
        if (chanserv == null) {
            chanserv = new ChanServ(this);
        }
        return chanserv;
    }

    /**
	 * Add a ChannelMessageListener
	 * 
	 * @param listener the listener to add
	 */
    public void addChannelMessageListener(ChannelMessageListener listener) {
        synchronized (lsnChanMsgs) {
            lsnChanMsgs.add(listener);
        }
    }

    /**
	 * Add a ChannelEventListener
	 * 
	 * @param listener the listener to add
	 */
    public void addChannelEventListener(ChannelEventListener listener) {
        synchronized (lsnChanEvents) {
            lsnChanEvents.add(listener);
        }
    }

    /**
	 * Add a PrivateMessageListener
	 *
	 * @param listener the listener to add
	 */
    public void addPrivateMessageListener(PrivateMessageListener listener) {
        synchronized (lsnPMs) {
            lsnPMs.add(listener);
        }
    }

    /**
	 * Add a CTCPListener
	 *
	 * @param listener the listener to add
	 */
    public void addCTCPListener(CTCPListener listener) {
        synchronized (lsnCTCPs) {
            lsnCTCPs.add(listener);
        }
    }

    /**
	 * Add a CTCPVersionListener
	 *
	 * @param listener the listener to add
	 */
    public void addCTCPVersionListener(CTCPVersionListener listener) {
        synchronized (lsnCTCPVs) {
            lsnCTCPVs.add(listener);
        }
    }

    void process(String raw) {
        String[] parts = libairc.splitRaw(raw);
        if (parts == null) {
            return;
        }
        processHost(parts[0].substring(1));
        if (parts[1].equalsIgnoreCase("join")) {
            onJoin(parts[0].substring(1), parts[2].substring(1));
        } else if (parts[1].equalsIgnoreCase("part")) {
            onPart(parts[0].substring(1), parts[2]);
        } else if (parts[1].equalsIgnoreCase("quit")) {
            onQuit(parts[0].substring(1));
        } else if (parts[1].equalsIgnoreCase("privmsg")) {
            onPrivMsg(parts[0].substring(1), parts[2], libairc.recombine(parts, 3).substring(1));
        } else if (parts[1].equalsIgnoreCase("mode")) {
            onMode(parts[0].substring(1), parts[2], libairc.recombine(parts, 3));
        }
    }

    /**
	 * Whenever libairc receives a full nih it should update its own internal records
	 *
	 * @param host the host that was received
	 */
    private void processHost(String host) {
        String[] parts = libairc.splitHost(host);
        if (parts == null) {
            return;
        }
        User user;
        if (libairc.getNickFromHost(host).equalsIgnoreCase(nick)) return;
        synchronized (users) {
            if (users.containsKey(parts[0])) {
                user = (User) users.get(parts[0]);
            } else {
                users.put(parts[0], new User(this, parts[0], parts[1], parts[2]));
                return;
            }
        }
        synchronized (user) {
            user.update(parts[0], parts[1], parts[2]);
        }
    }

    /**
	 * Called whenever something joins a channel
	 *
	 * @param host the host that joined
	 * @param channel the channel that host joined
	 */
    private void onJoin(String host, String channel) {
        Channel chanRecord = getChannel(channel, true);
        if (libairc.getNickFromHost(host).equalsIgnoreCase(nick)) {
            libairc.debug("BotJoin", "Channel: " + channel);
            new Thread(new channelJoinInvoker(incoming.catchMessages(), channel)).start();
        } else {
            libairc.debug("Join", "Nick: " + libairc.getNickFromHost(host) + ", Channel: " + channel);
            User user = getUser(libairc.getNickFromHost(host));
            if (user == null) {
                libairc.debug("Join", "Unknown user: " + libairc.getNickFromHost(host));
                return;
            }
            chanRecord.addUser(user);
        }
    }

    class channelJoinInvoker implements Runnable {

        Catcher c;

        String chan;

        public channelJoinInvoker(Catcher cat, String channel) {
            c = cat;
            chan = channel;
        }

        public void run() {
            onJoinHelper(c, chan);
        }
    }

    /**
	 * Called whenever something parts a channel
	 *
	 * @param host the host that parted
	 * @param channel the channel that host parted
	 */
    private void onPart(String host, String channel) {
        Channel chanRecord = getChannel(channel, false);
        if (chanRecord == null) {
            libairc.debug("Part", "Unknown channel: " + channel);
            return;
        }
        if (libairc.getNickFromHost(host).equalsIgnoreCase(nick)) {
            chanRecord.cleanUsers();
            synchronized (channels) {
                channels.remove(channel);
            }
        } else {
            libairc.debug("Part", "Nick: " + libairc.getNickFromHost(host) + ", Channel: " + channel);
            chanRecord.remUser(libairc.getNickFromHost(host));
            Object[] chans;
            String nick = libairc.getNickFromHost(host);
            boolean contained = false;
            synchronized (channels) {
                chans = channels.keySet().toArray();
                for (int index = 0; index < chans.length && !contained; index++) {
                    if (((Channel) channels.get(chans[index])).containsUser(nick)) {
                        contained = true;
                    }
                }
                if (!contained) {
                    libairc.debug("Part", "User '" + nick + "' not found in any channels...deleting");
                    synchronized (users) {
                        users.remove(nick);
                    }
                }
            }
        }
    }

    /**
	 * Called whenever something quits IRC
	 *
	 * @param host the host that quit
	 */
    private void onQuit(String host) {
        String nick = libairc.getNickFromHost(host);
        Object[] chans;
        synchronized (channels) {
            chans = channels.keySet().toArray();
            for (int index = 0; index < chans.length; index++) {
                ((Channel) channels.get(chans[index])).remUser(nick);
            }
        }
        synchronized (users) {
            libairc.debug("Quit", "Nick: " + nick);
            users.remove(nick);
        }
    }

    /**
	 * Called on a PRIVMSG
	 *
	 * @param host the sending host
	 * @param target where they sent it
	 * @param message the message they sent
	 */
    private void onPrivMsg(String host, String target, String message) {
        if (target.startsWith("#") || target.startsWith("&")) {
            onChanMsg(host, target, message);
        } else if (message.startsWith(libairc.CTCP) && message.endsWith(libairc.CTCP)) {
            User user = getUser(libairc.getNickFromHost(host));
            String arguments = message.substring(1, message.length() - 1);
            if (arguments.equals("VERSION")) {
                events.add(new CTCPVersionInvoker(this, user, lsnCTCPVs));
            } else if (arguments.startsWith("ACTION ")) {
                events.add(new PrivateMessageInvoker(this, user, arguments.substring(7), true, lsnPMs));
            }
            events.add(new CTCPInvoker(this, user, arguments, lsnCTCPs));
        } else {
            User user = getUser(libairc.getNickFromHost(host));
            events.add(new PrivateMessageInvoker(this, user, message, false, lsnPMs));
        }
    }

    /**
	 * Called on a PRIVMSG to a channel
	 * @param host the sending host
	 * @param chanenl where they sent it
	 * @param message the message they sent
	 */
    private void onChanMsg(String host, String channel, String message) {
        User user = getUser(libairc.getNickFromHost(host));
        Channel chanRec = getChannel(channel, false);
        if (chanRec == null) {
            return;
        }
        if (message.startsWith(libairc.CTCP + "ACTION ") && message.endsWith(libairc.CTCP)) {
            events.add(new ChannelMessageInvoker(this, user, chanRec, message.substring(8, message.length() - 1), true, lsnChanMsgs));
        } else {
            events.add(new ChannelMessageInvoker(this, user, chanRec, message, false, lsnChanMsgs));
        }
    }

    private void onMode(String host, String target, String mode) {
        if (target.startsWith("#")) {
            onChanMode(host, target, mode);
        } else {
        }
    }

    private void onChanMode(String host, String channel, String mode) {
        boolean plus = true;
        String[] list = mode.split(" ");
        char modeChar;
        String modePiece = "";
        int param = 1;
        Channel chan = getChannel(channel);
        final String pmArgs = "ohvbeI";
        final String pArgs = "lk";
        try {
            for (int index = 0; index < list[0].length(); index++) {
                modeChar = list[0].charAt(index);
                if (modeChar == '+') {
                    plus = true;
                } else if (modeChar == '-') {
                    plus = false;
                } else if (pmArgs.indexOf("" + modeChar) >= 0) {
                    modePiece = modeChar + " " + list[param++];
                } else if (pArgs.indexOf("" + modeChar) >= 0) {
                    if (plus) {
                        modePiece = modeChar + " " + list[param++];
                    } else {
                        modePiece = "" + modeChar;
                    }
                } else {
                    modePiece = "" + modeChar;
                }
                if (plus && modeChar != '+' && modeChar != '-') {
                    modePiece = "+" + modePiece;
                } else {
                    modePiece = "-" + modePiece;
                }
                if (modeChar != '+' && modeChar != '-') {
                    char flag = modePiece.charAt(1);
                    if (modePiece.indexOf(" ") >= 0) {
                        String arg = modePiece.split(" ")[1];
                        plus = modePiece.charAt(0) == '+';
                        if (flag == 'o') {
                            if (plus) {
                                if (arg.equals(nick)) {
                                    chan.setMyOp(true);
                                } else {
                                    chan.addOp(getUser(arg));
                                }
                            } else {
                                if (arg.equals(nick)) {
                                    chan.setMyOp(false);
                                } else {
                                    chan.delOp(arg);
                                }
                            }
                        } else if (flag == 'h') {
                            if (plus) {
                                if (arg.equals(nick)) {
                                    chan.setMyHop(true);
                                } else {
                                    chan.addHop(getUser(arg));
                                }
                            } else {
                                if (arg.equals(nick)) {
                                    chan.setMyHop(false);
                                } else {
                                    chan.delHop(arg);
                                }
                            }
                        } else if (flag == 'v') {
                            if (plus) {
                                if (arg.equals(nick)) {
                                    chan.setMyVoice(true);
                                } else {
                                    chan.addVoice(getUser(arg));
                                }
                            } else {
                                if (arg.equals(nick)) {
                                    chan.setMyVoice(false);
                                } else {
                                    chan.delVoice(arg);
                                }
                            }
                        } else if (flag == 'k') {
                            chan.setKey(arg);
                        } else if (flag == 'l') {
                            chan.setLimit(arg);
                        }
                    } else {
                        if (modePiece.charAt(0) == '+') {
                            chan.setModeFlag(flag);
                        } else {
                            if (flag == 'k') {
                                chan.clearKey();
                            } else if (flag == 'l') {
                                chan.clearLimit();
                            } else {
                                chan.clearModeFlag(flag);
                            }
                        }
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            libairc.debug("Mode", "Channel " + channel + " received invalid mode string " + mode + ".");
        }
    }

    void onJoinHelper(Catcher catcher, String channel) {
        Channel chanRec = getChannel(channel, true);
        String[] message;
        String nick;
        boolean catching = true;
        boolean names = false;
        boolean topic = false;
        boolean mode = false;
        while (catching) {
            catcher.waitForMessages();
            message = catcher.getNextMessage().split(" ");
            if (message.length > 5 && message[1].equalsIgnoreCase("353") && message[4].equalsIgnoreCase(channel)) {
                message[5] = message[5].substring(1);
                for (int index = 5; index < message.length; index++) {
                    nick = libairc.getNickFromNames(message[index]);
                    if (!this.nick.equalsIgnoreCase(nick)) {
                        if (!users.containsKey(nick)) {
                            libairc.debug("Names", "Added user record for: " + nick);
                            users.put(nick, new User(this, nick));
                        }
                        chanRec.addUser(getUser(nick));
                    }
                }
            } else if (message.length > 4 && message[1].equalsIgnoreCase("366") && message[3].equalsIgnoreCase(channel)) {
                incoming.stopCatchingMessages(catcher);
                catching = false;
            }
        }
    }

    /**
	 * Quick and dirty quit from the IRC server
	 */
    public void quit() {
        quit("");
    }

    /**
	 * Much more polite quit with a message
	 *
	 * @param message quit message
	 */
    public void quit(String message) {
        pinger.stop();
        incoming.stop();
        outgoing.stop();
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            writer.write("QUIT :" + message + "\n");
            writer.flush();
        } catch (IOException ioEx) {
            try {
                connection.close();
            } catch (IOException lolEx) {
            }
        }
        connection = null;
    }

    /**
	 * Get the local inet address
	 * 
	 * @return local inet address
	 */
    public InetAddress getLocalAddress() {
        return localAddress;
    }

    /**
	 * Set debug messages status
	 *
	 * @param b debug messages enabling flag
	 */
    public void setDebugMessages(boolean b) {
        libairc.showDebugMessages = b;
    }

    /**
	 * Get the DCC Manager
	 * 
	 * If the DCC Manager does not exist, this method will create a new one with the default constructor
	 * 
	 * @return the DCCManager object associated with this client
	 */
    public DCCManager getDCCManager() {
        if (dccManager == null) dccManager = new DCCManager(this);
        return dccManager;
    }

    /**
	 * Set the DCC Manager
	 *
	 * @param dcc the DCCManager to use
	 */
    public void setDCCManager(DCCManager dcc) {
        dccManager = dcc;
    }
}
