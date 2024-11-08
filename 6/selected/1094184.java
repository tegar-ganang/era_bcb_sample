package vavi.net.im.protocol.xmpp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smackx.muc.MultiUserChat;
import vavi.net.im.Buddy;
import vavi.net.im.Group;
import vavi.net.im.Message;
import vavi.net.im.Session;
import vavi.net.im.SmileyComponent;
import vavi.net.im.event.CallableIMEvent;
import vavi.net.im.event.IMEvent;
import vavi.net.im.event.IMEvent.IMEventName;
import vavi.net.im.protocol.Protocol;
import vavi.util.Debug;

/**
 * Jabber Protocol. (smack version)
 * 
 * @see "http://www.jivesoftware.org/smack/"
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 */
public class JabberProtocol extends Protocol {

    /** */
    static Logger log = Logger.getLogger(JabberProtocol.class.getName());

    /** smac */
    private XMPPConnection connection;

    /** */
    private int priority = 5;

    /** */
    private TimerTask pinger = new TimerTask() {

        /** */
        public void run() {
            try {
                Presence presence = new Presence(Presence.Type.available);
                presence.setStatus(self.getCustomStatus());
                presence.setPriority(priority);
                presence.setMode((Mode) localizeStatus(self.getStatus()));
                connection.sendPacket(presence);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /** */
    private Timer pingTimer;

    /**
     * Default Constructor
     * <pre>
     * System Properties:
     *  vavi.net.im.protocol.xmpp.JabberProtocol.debug = true|false
     *  vavi.net.im.protocol.xmpp.JabberProtocol.priority = integer
     * </pre>
     */
    public JabberProtocol() {
        if ("true".equals(System.getProperty("vavi.net.im.protocol.xmpp.JabberProtocol.debug"))) {
            XMPPConnection.DEBUG_ENABLED = true;
        }
        try {
            priority = Integer.parseInt(System.getProperty("vavi.net.im.protocol.xmpp.JabberProtocol.priority"));
        } catch (NumberFormatException e) {
            Debug.println("use default priority: " + priority);
        }
        statusMap.put(Mode.available.toString(), Mode.available);
        statusMap.put(Mode.away.toString(), Mode.away);
        statusMap.put(Mode.chat.toString(), Mode.chat);
        statusMap.put(Mode.dnd.toString(), Mode.dnd);
        statusMap.put(Mode.xa.toString(), Mode.xa);
        features.put(Feature.BuddyAddRequestSupported, true);
        features.put(Feature.BuddyGroupSupported, true);
        features.put(Feature.IgnoreSupported, true);
        features.put(Feature.OfflineMessageSupported, true);
        features.put(Feature.TypingNotifySupported, true);
        features.put(Feature.ConferenceSupported, true);
        features.put(Feature.MailNotifySupported, false);
        features.put(Feature.InvisibleSupported, true);
        features.put(Feature.BuddyNameAliasSupported, true);
    }

    /** */
    private ConnectionListener connectionListener = new ConnectionListener() {

        /** */
        public void connectionClosedOnError(Exception exception) {
            try {
                listeners.eventHappened(new IMEvent(this, JabberEventName.exception, exception));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /** */
        public void connectionClosed() {
            pingTimer.cancel();
        }

        @Override
        public void reconnectingIn(int arg0) {
        }

        @Override
        public void reconnectionFailed(Exception arg0) {
        }

        @Override
        public void reconnectionSuccessful() {
        }
    };

    /** */
    private MessageListener messageListener = new MessageListener() {

        /** */
        public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message smackMessage) {
            try {
                String from = smackMessage.getFrom();
                if (from == null) {
                    return;
                }
                String body = smackMessage.getBody();
                if (body == null) {
                    return;
                }
                Buddy buddy = findBuddyForce(from);
                Message message = new Message(body);
                if (smackMessage.getType() == org.jivesoftware.smack.packet.Message.Type.normal || smackMessage.getType() == org.jivesoftware.smack.packet.Message.Type.chat) {
                    listeners.eventHappened(new IMEvent(this, IMEventName.instantMessageReceived, findSessionForce(buddy), message));
                } else if (smackMessage.getType() == org.jivesoftware.smack.packet.Message.Type.groupchat) {
                    String sessionName = smackMessage.getSubject();
                    listeners.eventHappened(new IMEvent(this, IMEventName.conferenceMessageReceived, findSessionForce(sessionName, buddy), message));
                } else {
                    Debug.println("unknown type: " + smackMessage.getType());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /** */
    private PacketListener presenceListener = new PacketListener() {

        /**
         * we set up a timer to run every 2 seconds to reload the buddies,
         * this way we can minimize the number of JTree's that are created
         * when buddies sign on/off
         */
        private Timer timer;

        /** */
        public void processPacket(Packet packet) {
            try {
                Presence presence = (Presence) packet;
                String from = presence.getFrom();
                if (findSession(new Buddy(from)) != null) {
                    return;
                }
                if (presence.getType() == Presence.Type.subscribe) {
                    confirmSubscription(from);
                    return;
                }
                if (presence.getType() != Presence.Type.available && presence.getType() != Presence.Type.unavailable) {
                    return;
                }
                if (!self.getUsername().equals(from)) {
                    Buddy buddy = findBuddy(from);
                    if (buddy == null) {
                        confirmSubscription(from);
                    } else {
                        buddy.setStatus(normalizeStatus(presence.getMode()));
                        listeners.eventHappened(new IMEvent(this, IMEventName.buddyStatusChanged, buddy));
                    }
                }
                timer = new Timer();
                timer.schedule(new TimerTask() {

                    public void run() {
                        timer.cancel();
                        reload();
                    }
                }, 200);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /** */
        private void confirmSubscription(String from) {
            try {
                CallableIMEvent imEvent = new CallableIMEvent(this, IMEventName.doConfirmation, "Do you accept to add contact: " + from);
                listeners.eventHappened(imEvent);
                if (imEvent.getResult() != null && (Boolean) imEvent.getResult()) {
                    Presence packet = new Presence(Presence.Type.subscribed);
                    packet.setTo(from);
                    connection.sendPacket(packet);
                    Debug.println("SUBSCRIBE: " + from);
                }
                if (findBuddy(from) == null) {
                    listeners.eventHappened(new IMEvent(this, IMEventName.buddyAdded, new Buddy(from)));
                }
            } catch (IOException e) {
                Debug.printStackTrace(e);
            }
        }
    };

    /** */
    private PacketListener groupParticipantListener = new PacketListener() {

        /**
         * Processes incoming presence packets (from group chats)
         */
        public void processPacket(Packet packet) {
            try {
                Presence presence = (Presence) packet;
                String from = packet.getFrom();
                Buddy buddy = new Buddy(from);
                if (presence.getType() == Presence.Type.unavailable) {
                    listeners.eventHappened(new IMEvent(this, IMEventName.conferenceParticipantLeft, findSessionForce(buddy), buddy));
                } else {
                    if (presence.getStatus() != null) {
                        buddy.setStatus(presence.getStatus());
                        listeners.eventHappened(new IMEvent(this, IMEventName.buddyStatusChanged, buddy));
                    }
                    listeners.eventHappened(new IMEvent(this, IMEventName.conferenceParticipantJoined, findSessionForce(buddy), buddy));
                }
            } catch (IOException e) {
                Debug.printStackTrace(e);
            }
        }
    };

    /** */
    private void reload() {
        Roster roster = connection.getRoster();
        groups.clear();
        for (RosterGroup smackGroup : roster.getGroups()) {
            Group group = new Group(smackGroup.getName());
            groups.add(group);
            for (RosterEntry smackBuddy : smackGroup.getEntries()) {
                Buddy buddy = new Buddy(smackBuddy.getUser());
                buddy.setAlias(smackBuddy.getName());
                Presence presence = roster.getPresence(smackBuddy.getUser());
                if (presence == null) {
                    buddy.setStatus("invisible");
                } else {
                    buddy.setStatus(presence.getMode().toString());
                }
                group.addBuddy(buddy);
            }
        }
    }

    /**
     * Connect to the Jabber server.
     * 
     * @param props "username"*, "password"*, "resource", "server"*, "isUsessl" (* required)
     */
    protected void connectInternal(Properties props) throws IOException {
        boolean isUsessl = (Boolean) props.get("isUsessl");
        String host = props.getProperty("server");
        String username = props.getProperty("username");
        String password = props.getProperty("password");
        int port = Integer.parseInt(props.getProperty("port", isUsessl ? "5223" : "5222"));
        String resource = props.getProperty("resource", "JabberWeb");
        try {
            ConnectionConfiguration config = new ConnectionConfiguration(host, port);
            config.setCompressionEnabled(true);
            config.setSASLAuthenticationEnabled(true);
            if (isUsessl) {
                config.setSecurityMode(SecurityMode.enabled);
            }
            connection = new XMPPConnection(config);
            connection.connect();
            connection.login(username, password, resource);
            PacketFilter pf = new PacketTypeFilter(Presence.class);
            connection.addPacketListener(presenceListener, pf);
            connection.addPacketListener(groupParticipantListener, pf);
            connection.addConnectionListener(connectionListener);
            self = new Buddy(new JabberIdentifier(username, host, resource).toString());
            changeStatus(normalizeStatus(Presence.Type.available));
            reload();
            pingTimer = new Timer();
            pingTimer.schedule(pinger, 0, 240000);
        } catch (XMPPException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

    /**
     * Disconnect from Jabber
     */
    protected void disconnectInternal() throws IOException {
        if (connection.isConnected()) {
            connection.disconnect();
        }
    }

    /** */
    protected void changeStatusInternal(String status) throws IOException {
        Presence presence = new Presence(Type.available);
        presence.setPriority(priority);
        presence.setMode((Mode) localizeStatus(status));
        if (self.getCustomStatus() != null) {
            presence.setStatus(self.getCustomStatus());
        }
        connection.sendPacket(presence);
    }

    /** */
    protected Object localizeStatusInternal(String status) {
        return Presence.Mode.available;
    }

    /**
     * Gets the protocol name
     * 
     * @return String containing the protocol name
     */
    public String getProtocolName() {
        return "Jabber";
    }

    /** common/native mapping <Session, Chet|GroupChat> */
    private Map<Session, Object> chats = new HashMap<Session, Object>();

    /**
     * Starts a conference
     * @param session Session
     */
    protected void startSessionInternal(Session session) throws IOException {
        ChatManager chatManager = connection.getChatManager();
        Chat chat = chatManager.createChat(session.getParticipants()[0].getUsername(), messageListener);
        chats.put(session, chat);
    }

    /**
     * Starts a conference
     * @param session Session
     */
    protected void startSessionInternal(Session session, String message) throws IOException {
        MultiUserChat chat = new MultiUserChat(connection, session.getName());
        chats.put(session, chat);
    }

    /**
     * Quits a conference.
     * 
     * @param session conference to quit
     */
    protected void quitSessionInternal(Session session) throws IOException {
        if (session.isGroupSession()) {
            MultiUserChat chat = (MultiUserChat) chats.get(session);
            chat.leave();
        }
    }

    /**
     * Sends an instant message to the conference.
     * 
     * @param session Session
     * @param message Message to send to the conference
     */
    protected void sendSessionMessageInternal(Session session, Message message) throws IOException {
        try {
            org.jivesoftware.smack.packet.Message smackMessage = new org.jivesoftware.smack.packet.Message();
            smackMessage.setBody(message.toString());
            smackMessage.setType(org.jivesoftware.smack.packet.Message.Type.groupchat);
            MultiUserChat chat = (MultiUserChat) chats.get(session);
            chat.sendMessage(smackMessage);
        } catch (XMPPException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

    /**
     * Sends an instant message to a buddy.
     * 
     * @param session Buddy to send message to
     * @param message Message to send
     */
    protected void sendInstantMessageInternal(Session session, Message message) throws IOException {
        try {
            org.jivesoftware.smack.packet.Message smackMessage = new org.jivesoftware.smack.packet.Message();
            smackMessage.setTo(session.getParticipants()[0].getUsername());
            smackMessage.setBody(message.toString());
            smackMessage.setType(org.jivesoftware.smack.packet.Message.Type.chat);
            Chat chat = (Chat) chats.get(session);
            chat.sendMessage(smackMessage);
        } catch (XMPPException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

    /**
     * Adds a buddy to the Buddy List.
     * 
     * @param buddy Buddy to be added to the Buddy List
     */
    protected void addToBuddyListInternal(Buddy buddy) throws IOException {
        try {
            Roster roster = connection.getRoster();
            roster.createEntry(buddy.getUsername(), buddy.getAlias(), null);
        } catch (XMPPException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

    /**
     * Deletes a buddy from the Buddy List.
     * 
     * @param buddy Buddy to be deleted from the Buddy List
     */
    protected void deleteFromBuddyListInternal(Buddy buddy) throws IOException {
    }

    /**
     * Ignores a buddy.
     * 
     * @param buddy Buddy to ignore
     */
    protected void ignoreBuddyInternal(Buddy buddy) throws IOException {
    }

    /**
     * Unignores a buddy.
     * 
     * @param buddy Buddy to unignore
     */
    protected void unignoreBuddyInternal(Buddy buddy) throws IOException {
    }

    /** */
    protected void startTypingInternal(Buddy buddy) throws IOException {
    }

    /** */
    protected void stopTypingInternal(Buddy buddy) throws IOException {
    }

    /** */
    public SmileyComponent[] getSupportedSmileys() {
        return null;
    }

    /** */
    protected void changeBuddyAliasInternal(Buddy buddy, String alias) throws IOException {
        Roster roster = connection.getRoster();
        RosterEntry entry = roster.getEntry(buddy.getUsername());
        entry.setName(alias);
    }

    /** */
    protected void addGroupInternal(Group group) throws IOException {
        Roster roster = connection.getRoster();
        roster.createGroup(group.getName());
    }

    /** */
    protected void removeGroupInternal(Group group) throws IOException {
    }

    /** */
    protected void changeGroupNameInternal(Group group, String newName) throws IOException {
        Roster roster = connection.getRoster();
        RosterGroup smackGroup = roster.getGroup(group.getName());
        smackGroup.setName(newName);
    }

    /** */
    protected void changeGroupOfBuddyInternal(Buddy buddy, Group oldGroup, Group newGroup) throws IOException {
        try {
            Roster roster = connection.getRoster();
            RosterEntry entry = roster.getEntry(buddy.getUsername());
            System.err.print(buddy.getUsername() + " moved ");
            for (RosterGroup oldSmackGroup : entry.getGroups()) {
                if (oldSmackGroup.getName().equals(oldGroup.getName())) {
                    oldSmackGroup.removeEntry(entry);
                    System.err.print("from " + oldGroup.getName());
                }
            }
            RosterGroup newSmackGroup = roster.getGroup(newGroup.getName());
            newSmackGroup.addEntry(entry);
            System.err.println(" to " + newGroup.getName());
        } catch (XMPPException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }
}
