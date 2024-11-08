package net.timeslicer.jabber;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.Occupant;

public class ModThread implements Runnable {

    static Thread t;

    static StringBuffer messageBuffer = new StringBuffer();

    static ConnectionConfiguration config;

    static XMPPConnection connection;

    static MultiUserChat room;

    static Map userMessageMap = new HashMap();

    static List flaggedWords = new ArrayList();

    private static Logger log = Logger.getLogger(ModThread.class);

    String host;

    int port;

    String serviceName;

    String user;

    String password;

    String roomUrl;

    String botName;

    String propsFilePath;

    public static void main(String[] args) {
        t = new Thread(new ModThread(args[0]));
        t.start();
    }

    private ModThread(String path) {
        this.propsFilePath = path;
    }

    public static void registerLongName(final String jid) {
        try {
            final String nick = room.getOccupant(jid).getNick();
            if (nick.length() >= 35) {
                final Occupant occ = room.getOccupant(jid);
                if (occ == null) {
                    log.error("ERROR");
                } else {
                    room.kickParticipant(nick, "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void registerSpam(final Occupant occ) {
        try {
            room.kickParticipant(occ.getNick(), "Spamming the room.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Occupant getSender(String roomOcc) {
        return room.getOccupant(roomOcc);
    }

    public static void cleanGuests() {
        try {
            final Iterator<String> occs = room.getOccupants();
            while (occs.hasNext()) {
                final Occupant occ = room.getOccupant(occs.next());
                if (occ.getNick().toLowerCase().startsWith("guest")) {
                    room.banUser(occ.getJid(), "Pick a name when you're asked next time.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void listMods() {
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void banByNick(final String nick) {
        try {
            log.info("banning " + nick);
            final Iterator<String> occs = room.getOccupants();
            while (occs.hasNext()) {
                final Occupant occ = room.getOccupant(occs.next());
                if (occ.getNick().toLowerCase().equals(nick.toLowerCase())) {
                    room.banUser(occ.getJid(), "Mr Brooks didn't like you.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void banOccupant(final Occupant occ) {
        try {
            log.info("banning " + occ.getNick());
            room.banUser(occ.getJid(), "Mr Brooks didn't like you.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void kickByNick(final String nick) {
        try {
            log.info("kick " + nick);
            final Iterator<String> occs = room.getOccupants();
            while (occs.hasNext()) {
                final Occupant occ = room.getOccupant(occs.next());
                final String[] nickParts = StringUtils.split(occ.getNick(), "@");
                log.info(nickParts[0].toLowerCase());
                if (nickParts[0].toLowerCase().equals(nick.toLowerCase())) {
                    room.kickParticipant(occ.getNick(), "Mr Brooks didn't like you.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void kickOccupant(final Occupant occ) {
        try {
            log.info("kick " + occ.getNick());
            room.kickParticipant(occ.getNick(), "Mr Brooks didn't like you.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void muteGuest(final String nick) {
        try {
            room.revokeVoice(nick);
        } catch (Exception e) {
        }
    }

    public static void muteNick(final String nick) {
        try {
            log.info("muting " + nick);
            final Iterator<String> occs = room.getOccupants();
            while (occs.hasNext()) {
                final Occupant occ = room.getOccupant(occs.next());
                final String[] nickParts = StringUtils.split(occ.getNick(), "@");
                log.info(nickParts[0].toLowerCase());
                if (nickParts[0].toLowerCase().equals(nick.toLowerCase())) {
                    room.revokeVoice(occ.getNick());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void unMuteGuest(final String nick) {
        try {
            log.info("unmuting " + nick);
            final Iterator<String> occs = room.getOccupants();
            while (occs.hasNext()) {
                final Occupant occ = room.getOccupant(occs.next());
                final String[] nickParts = StringUtils.split(occ.getNick(), "@");
                log.info(nickParts[0].toLowerCase());
                if (nickParts[0].toLowerCase().equals(nick.toLowerCase())) {
                    room.grantVoice(occ.getNick());
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void unMuteAll() {
        try {
            final Iterator<String> occs = room.getOccupants();
            while (occs.hasNext()) {
                final Occupant occ = room.getOccupant(occs.next());
                final String[] nickParts = StringUtils.split(occ.getNick(), "@");
                if (!StringUtils.contains(occ.getNick(), "guest")) room.grantVoice(occ.getNick());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void registerShiftEnter(final Occupant occ) {
        try {
            room.kickParticipant(occ.getNick(), "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void postShiftTabs() throws Exception {
        room.sendMessage("");
    }

    @Override
    public void run() {
        try {
            Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(this.propsFilePath));
                this.botName = properties.getProperty("botName");
                this.host = properties.getProperty("host");
                this.port = Integer.parseInt(properties.getProperty("port"));
                this.password = properties.getProperty("password");
                this.user = properties.getProperty("user");
                this.serviceName = properties.getProperty("serviceName");
                this.roomUrl = properties.getProperty("roomUrl");
            } catch (Exception e) {
                e.printStackTrace();
            }
            boolean connected = false;
            while (!connected) {
                try {
                    config = new ConnectionConfiguration(this.host, this.port, this.serviceName);
                    connection = new XMPPConnection(config);
                    connection.connect();
                    connection.login(this.user, this.password);
                } catch (Exception iggy) {
                    iggy.printStackTrace();
                }
                connected = true;
            }
            connected = false;
            while (!connected) {
                try {
                    room = new MultiUserChat(connection, this.roomUrl);
                    room.join(this.botName);
                } catch (Exception iggy) {
                    iggy.printStackTrace();
                }
                connected = true;
            }
            t.sleep(500);
            room.addMessageListener(new SpamMessageListener());
            room.addParticipantStatusListener(new SimpleStatusListener());
            checkLongNames();
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                t.sleep(200);
            } catch (InterruptedException e) {
            }
        }
    }

    private void checkLongNames() {
        try {
            final Iterator<String> it = room.getOccupants();
            while (it.hasNext()) {
                String jid = it.next();
                log.info("check: " + jid);
                Occupant oc = room.getOccupant(jid);
                String nick = oc.getNick();
                if (nick.length() > 35) {
                    ModThread.registerLongName(jid);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
