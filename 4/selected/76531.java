package net.sf.zorobot.irc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import be.trc.core.HostParser;
import be.trc.core.IRCServer;
import be.trc.core.hive.DefaultHostParser;
import net.sf.zorobot.core.Message;
import net.sf.zorobot.core.ZorobotProperties;
import net.sf.zorobot.core.ZorobotSystem;
import net.sf.zorobot.game.ZorobotGame;
import net.sf.zorobot.util.IRCBot;

public class IrcBot2rc extends IRCBot implements IrcInterface {

    static InputThread iThread = new InputThread();

    static OutputThread oThread = new OutputThread();

    private int msgId = 0;

    private int lastChannelId = 0;

    private String channelIdArray[];

    private HashMap<String, Integer> channelNameMap;

    private IRCServer server;

    private HashSet registeredNick = new HashSet();

    private HashSet isRegisteredNick = new HashSet();

    private Hashtable verifyingNick = new Hashtable();

    private HashSet<String> pingingNick = new HashSet<String>();

    private Hashtable<String, String> pingResult = new Hashtable<String, String>();

    private net.sf.zorobot.core.Zorobot zorobot;

    int maxChannel = 1;

    int numOfFixedChannel = 1;

    final int MAX_PRIORITY = 10;

    Queue<Message>[] msgQueue = new Queue[MAX_PRIORITY + 1];

    public IrcBot2rc() {
        zorobot = new net.sf.zorobot.core.Zorobot(this);
        synchronized (msgQueue) {
            for (int i = 0; i <= MAX_PRIORITY; i++) {
                msgQueue[i] = new LinkedList<Message>();
            }
        }
        maxChannel = ZorobotSystem.getMaxChannel();
        channelIdArray = new String[maxChannel];
        channelNameMap = new HashMap<String, Integer>();
    }

    public void setServer(IRCServer server) {
        this.server = server;
    }

    public void setNumOfFixedChannel(int n) {
        numOfFixedChannel = n;
    }

    public int getChannelId(String channelName) {
        Integer id = channelNameMap.get(channelName);
        if (id == null) return -1;
        return id.intValue();
    }

    public String getChannelName(int channelId) {
        return channelIdArray[channelId];
    }

    public boolean isRegisteredNick(String nick) {
        synchronized (verifyingNick) {
            if (!verifyingNick.containsKey(nick)) {
                registeredNick.remove(nick);
                sendToServer("WHOIS " + nick, oThread.server);
                for (; ; ) {
                    try {
                        verifyingNick.wait();
                    } catch (InterruptedException e) {
                    }
                    if (!verifyingNick.containsKey(nick)) {
                        System.out.println("verifying test: " + nick);
                        return isRegisteredNick.contains(nick);
                    }
                }
            } else {
                return true;
            }
        }
    }

    public void message(int channelId, int msgId, String sender, Message message) {
        message.channelId = channelId;
        ZorobotSystem.info(System.currentTimeMillis() + " ADD TO QUEUE: " + channelId + ": " + message);
        synchronized (msgQueue) {
            int priority = message.getPriority();
            if (priority > MAX_PRIORITY) priority = MAX_PRIORITY;
            msgQueue[priority].offer(message);
            msgQueue.notify();
        }
    }

    public void sendMessage(int channelId, String msg, be.trc.core.IRCServer server) {
        String channelName = getChannelName(channelId);
        ZorobotSystem.info(System.currentTimeMillis() + " SEND: " + channelName + ": " + msg);
        if (channelName != null) {
            sendMessage(channelName, msg, server);
        }
    }

    public void onPublicMessage(String nickname, String host, String username, String message, String channel, be.trc.core.IRCServer server) {
        try {
            if (iThread.server == null) iThread.server = server;
            if (oThread.server == null) oThread.server = server;
            message = message.replaceAll("\003[0-9]{0,2}(\\,[0-9]{1,2})?", "").replaceAll("\003|\002", "");
            message = message.trim();
            zorobot.message(getChannelId(channel), msgId++, nickname, new Message(message));
        } catch (Exception e) {
            ZorobotSystem.exception(e);
        }
    }

    public static void main(String[] args) {
        try {
            final IrcBot2rc bot = new IrcBot2rc();
            ZorobotProperties props = ZorobotSystem.props;
            bot.setDefaultNickname(props.getProperty("irc.nick"));
            bot.setDefaultFullname("Roronoa_2oro's Bot: zorobot 08");
            bot.setDefaultUsername(props.getProperty("irc.fullname"));
            be.trc.core.IRCServer server = bot.connect(props.getProperty("irc.server"), Integer.parseInt(props.getProperty("irc.port")));
            Thread.sleep(2000);
            System.out.println(bot.isConnected(server));
            String nickPass = props.getProperty("irc.password");
            if (nickPass != null && !"".equals(nickPass)) {
                Thread.sleep(5000);
                bot.sendMessage("nickserv", "IDENTIFY " + nickPass, server);
            }
            String channels = props.getProperty("irc.channel");
            final String channel[] = channels.split(",");
            long joinDelay = 10;
            try {
                joinDelay = Long.parseLong(props.getProperty("irc.joindelay"));
            } catch (Exception eeee) {
            }
            Thread.sleep(joinDelay * 1000);
            bot.setNumOfFixedChannel(channel.length);
            bot.setServer(server);
            for (int i = 0; i < channel.length; i++) {
                bot.joinChannel(channel[i]);
                System.out.println("join " + channel[i]);
            }
            new Thread() {

                public void run() {
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {
                    }
                    for (int i = 0; i < channel.length; i++) {
                        System.out.println("ensuring join " + channel[i]);
                        if (!bot.ensureJoin(channel[i])) {
                            bot.leaveChannel(bot.getChannelId(channel[i]), true);
                        }
                    }
                }
            }.start();
            iThread.bot = bot;
            iThread.start();
            oThread.bot = bot;
            oThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IRCServer getIRCServer() {
        return server;
    }

    @Override
    public void whoisCallback(String msg, boolean endWhoIs) {
        boolean isVerified = false;
        if (endWhoIs) {
            String m[] = msg.split(" ");
            if (registeredNick.contains(m[1])) {
                isRegisteredNick.add(m[1]);
                isVerified = true;
            }
            synchronized (verifyingNick) {
                verifyingNick.remove(m[1]);
                verifyingNick.notifyAll();
            }
        } else {
            if (msg.endsWith("has identified for this nick")) {
                String m[] = msg.split(" ");
                registeredNick.add(m[1]);
            }
        }
    }

    public void processNotice(String from, String target, String message, IRCServer ircServer) {
        HostParser hp = new DefaultHostParser(from);
        String fromNickname = hp.getNickname();
        System.out.println("NOTICE " + fromNickname + "> " + message);
        if (message.startsWith('\001' + "PING") && message.endsWith("" + '\001')) {
            long remoteTime = Long.parseLong(message.substring(6, message.length() - 1));
            long time = System.currentTimeMillis() - 13243546 - remoteTime;
            synchronized (pingResult) {
                if (time > 0 && time < 1800000) {
                    pingResult.put(fromNickname, new java.text.DecimalFormat("0.000").format((double) time / 1000));
                    pingResult.notifyAll();
                } else {
                    time = System.currentTimeMillis() - (remoteTime * 1000L);
                    if (time > 0 && time < 1800000) {
                        pingResult.put(fromNickname, "about " + new java.text.DecimalFormat("0.000").format((double) time / 1000));
                        pingResult.notifyAll();
                    } else {
                        pingResult.put(fromNickname, "unknown");
                        pingResult.notifyAll();
                    }
                }
            }
        }
    }

    public String getServer() {
        return server.getHost() + ":" + server.getPort();
    }

    public String ping(String nick) {
        boolean shouldPing = false;
        synchronized (pingingNick) {
            if (!pingingNick.contains(nick)) {
                shouldPing = true;
                pingingNick.add(nick);
            }
        }
        if (shouldPing) {
            System.out.println("Sending ping...");
            sendMessage(nick, "\001PING " + (System.currentTimeMillis() - 13243546) + "\001", server);
            for (; ; ) {
                synchronized (pingResult) {
                    try {
                        pingResult.wait();
                    } catch (InterruptedException e) {
                    }
                    if (pingResult.containsKey(nick)) {
                        pingingNick.remove(nick);
                        return pingResult.remove(nick);
                    }
                }
            }
        }
        return null;
    }

    public synchronized int joinChannel(String channelName) throws Exception {
        int id = -1;
        for (int i = 0; i < maxChannel; i++) {
            if (channelIdArray[i] == null) {
                ZorobotSystem.getGame(i).getSetting().reset();
                channelIdArray[i] = channelName;
                channelNameMap.put(channelName, new Integer(i));
                id = i;
                joinChannel(channelName, server);
                break;
            }
        }
        return id;
    }

    public synchronized boolean ensureJoin(String channelName) {
        int id = getChannelId(channelName);
        String chans[] = getChannels(server);
        boolean found = false;
        for (int j = 0; j < chans.length; j++) {
            if (chans[j].equalsIgnoreCase(channelName)) {
                found = true;
                break;
            }
        }
        return found;
    }

    public synchronized int leaveChannel(int id) {
        return leaveChannel(id, false);
    }

    public synchronized int leaveChannel(int id, boolean forced) {
        if (id >= numOfFixedChannel || forced) {
            channelNameMap.remove(channelIdArray[id]);
            partChannel(channelIdArray[id], "As requested", server);
            channelIdArray[id] = null;
            return id;
        }
        return -1;
    }
}

class InputThread extends Thread {

    public IrcBot2rc bot = null;

    public be.trc.core.IRCServer server = null;

    public void run() {
        InputStreamReader reader = new InputStreamReader(System.in);
        BufferedReader buf_in = new BufferedReader(reader);
        String str = "q";
        do {
            try {
                str = buf_in.readLine();
                if (bot != null && server != null && str != null && !"".equals(str)) {
                }
            } catch (IOException e) {
                System.out.println("IO exception = " + e);
            }
        } while (!str.toLowerCase().equals("/q"));
        if (bot != null) {
            bot.disconnect("Bye-bye", server);
        }
    }
}

class OutputThread extends Thread {

    public IrcBot2rc bot = null;

    public be.trc.core.IRCServer server = null;

    public void run() {
        for (; ; ) {
            try {
                Message toBeSent = null;
                for (int priority = 0; priority <= bot.MAX_PRIORITY; priority++) {
                    synchronized (bot.msgQueue) {
                        toBeSent = bot.msgQueue[priority].poll();
                    }
                    if (toBeSent != null) break;
                }
                if (toBeSent != null) {
                    ZorobotGame game = ZorobotSystem.getGame(toBeSent.channelId);
                    ArrayList<String> lines = toBeSent.toIrcString(game.color1, game.color2, 300);
                    Iterator<String> iter = lines.iterator();
                    while (iter.hasNext()) {
                        String line = iter.next();
                        bot.sendMessage(toBeSent.channelId, line, server);
                        try {
                            Thread.sleep(line.length() * 4L + 400L);
                        } catch (Exception ee) {
                        }
                    }
                } else {
                    synchronized (bot.msgQueue) {
                        bot.msgQueue.wait();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                ZorobotSystem.error("OutputThread error");
            }
        }
    }
}
