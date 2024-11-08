package com.itbs.aimcer.commune.smack;

import com.itbs.aimcer.bean.*;
import com.itbs.aimcer.commune.*;
import com.itbs.util.GeneralUtils;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.muc.SubjectUpdatedListener;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides connection to Jabber.
 *
 * @author Alex Rass
 * @since Dec 24, 2004
 */
public class SmackConnection extends AbstractMessageConnection implements FileTransferSupport, ChatRoomSupport {

    private static final Logger log = Logger.getLogger(SmackConnection.class.getName());

    public static final int DEFAULT_PORT = 5222;

    public static final int DEFAULT_PORT_SSL = 5223;

    protected XMPPConnection connection;

    FileTransferManager fileTransferManager;

    MultiUserChat multiUserChat;

    private static final String GROUP_UNFILED = "UnFiled";

    public SmackConnection() {
        serverName = "jabber.org";
        serverPort = DEFAULT_PORT;
    }

    protected XMPPConnection getNewConnection() throws XMPPException {
        return new XMPPConnection(new ConnectionConfiguration(System.getProperty("JABBER_HOST", getServerName()), Integer.getInteger("JABBER_PORT", getServerPort())));
    }

    /**
     * Non-blocking call.
     */
    public void connect() throws SecurityException, Exception {
        super.connect();
        notifyConnectionInitiated();
        new Thread() {

            public void run() {
                connectReal();
            }
        }.start();
    }

    public void connectReal() {
        try {
            connection = getNewConnection();
            connection.connect();
            System.out.println("SMACK VERSION: " + SmackConfiguration.getVersion());
            setAuthenticationMethod();
            addListeners();
            connection.login(getUserName(), getPassword());
            fireConnect();
        } catch (XMPPException e) {
            log.log(Level.INFO, "", e);
            disconnect(false);
            String error = e.getXMPPError() == null ? e.getMessage() : e.getXMPPError().getMessage();
            error = error == null ? "" : error;
            notifyConnectionFailed("Connection Failed. " + error);
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "UNCAUGHT EXCEPTION! PLEASE FIX!", e);
            disconnect(false);
            notifyConnectionFailed("Connection Failed. UNUSUAL TERMINATION!" + e.getMessage());
        }
    }

    /**
     * Starting smack 3.2 we can add listeners before login.  
     * 
     * I'm moving all the listeners adding code here - Alejandro
     */
    private void addListeners() {
        connection.getRoster().addRosterListener(new RosterListener() {

            public void entriesAdded(Collection<String> addresses) {
            }

            public void entriesUpdated(Collection<String> addresses) {
            }

            public void entriesDeleted(Collection<String> addresses) {
            }

            public void presenceChanged(Presence presence) {
                String normalizedName = normalizeName(presence.getFrom());
                Contact contact = getContactFactory().create(normalizedName, SmackConnection.this);
                RosterEntry rentry = connection.getRoster().getEntry(normalizedName);
                if (rentry != null && rentry.getName() != null) contact.setDisplayName(rentry.getName());
                Status status = (Status) contact.getStatus().clone();
                contact.getStatus().setOnline(presence.isAvailable());
                contact.getStatus().setAway(presence.isAway() && Presence.Mode.dnd != presence.getMode());
                contact.getStatus().setIdleTime(0);
                notifyStatusChanged(contact, status);
            }
        });
        PacketFilter filter = new MessageTypeFilter(org.jivesoftware.smack.packet.Message.Type.chat);
        PacketListener messageListener = new PacketListener() {

            public void processPacket(Packet packet) {
                processSmackPacket(packet);
            }
        };
        connection.addPacketListener(messageListener, filter);
        connection.addConnectionListener(new ConnectionListener() {

            public void connectionClosed() {
                notifyConnectionLost();
            }

            public void connectionClosedOnError(Exception e) {
                notifyConnectionLost();
            }

            /**
            * The connection will retry to reconnect in the specified number of seconds.
            *
            * @param seconds remaining seconds before attempting a reconnection.
            */
            public void reconnectingIn(int seconds) {
            }

            /**
            * The connection has reconnected successfully to the server. Connections will
            * reconnect to the server when the previous socket connection was abruptly closed.
            */
            public void reconnectionSuccessful() {
                notifyConnectionEstablished();
            }

            /**
            * An attempt to connect to the server has failed. The connection will keep trying
            * reconnecting to the server in a moment.
            *
            * @param e the exception that caused the reconnection to fail.
            */
            public void reconnectionFailed(Exception e) {
                notifyConnectionLost();
            }
        });
        PacketListener subscribeRequestListener = new PacketListener() {

            public void processPacket(Packet packet) {
                Presence pp = (Presence) packet;
                if (pp.getType() == Presence.Type.subscribe) {
                    boolean accept = true;
                    for (ConnectionEventListener eventHandler : eventHandlers) {
                        accept = eventHandler.contactRequestReceived(packet.getFrom(), SmackConnection.this);
                        if (!accept) break;
                    }
                    Presence response;
                    if (accept) response = new Presence(Presence.Type.subscribed); else response = new Presence(Presence.Type.unsubscribed);
                    response.setTo(pp.getFrom());
                    connection.sendPacket(response);
                }
            }
        };
        PacketFilter addedFiter = new org.jivesoftware.smack.filter.PacketTypeFilter(org.jivesoftware.smack.packet.Presence.class);
        connection.addPacketListener(subscribeRequestListener, addedFiter);
    }

    /**
     *  Set auth method in protected mode to allow subclasses to override it.
     */
    protected void setAuthenticationMethod() {
        SASLAuthentication.supportSASLMechanism("PLAIN", 0);
    }

    protected void fireConnect() {
        connection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.manual);
        Group lastGroup;
        Contact contact;
        for (RosterGroup rosterGroup : connection.getRoster().getGroups()) {
            getGroupList().add(lastGroup = getGroupFactory().create(rosterGroup.getName()));
            lastGroup.clear(this);
            for (RosterEntry rosterEntry : rosterGroup.getEntries()) {
                contact = getContactFactory().create(rosterEntry.getUser(), this);
                if (rosterEntry.getName() != null) {
                    contact.setDisplayName(rosterEntry.getName());
                }
                lastGroup.add(contact);
            }
        }
        if (connection.getRoster().getUnfiledEntryCount() > 0) {
            getGroupList().add(lastGroup = getGroupFactory().create(GROUP_UNFILED));
            for (RosterEntry rosterEntry : connection.getRoster().getUnfiledEntries()) {
                contact = getContactFactory().get(rosterEntry.getUser(), this);
                if (contact == null) {
                    contact = getContactFactory().create(rosterEntry.getUser(), this);
                    if (rosterEntry.getName() != null) {
                        contact.setDisplayName(rosterEntry.getName());
                    }
                    lastGroup.add(contact);
                }
            }
        }
        for (ConnectionEventListener eventHandler : eventHandlers) {
            eventHandler.statusChanged(this);
        }
        notifyConnectionEstablished();
    }

    protected void processSmackPacket(Packet packet) {
        try {
            Message message;
            Contact contact = null;
            boolean typingNotification = false;
            String from = normalizeName(packet.getFrom());
            if (packet instanceof org.jivesoftware.smack.packet.Message) {
                org.jivesoftware.smack.packet.Message smackMessage = (org.jivesoftware.smack.packet.Message) packet;
                contact = getContactFactory().create(from, this);
                RosterEntry entry = connection.getRoster().getEntry(from);
                if (entry != null && entry.getName() != null) contact.setDisplayName(entry.getName());
                message = new MessageImpl(contact, false, false, smackMessage.getBody());
                if (smackMessage.getBody() == null || smackMessage.getBody().length() == 0) typingNotification = true;
            } else {
                message = new MessageImpl(getContactFactory().create(from, this), false, false, (String) packet.getProperty("body"));
            }
            for (int i = 0; i < eventHandlers.size(); i++) {
                try {
                    if (typingNotification) {
                        (eventHandlers.get(i)).typingNotificationReceived(this, contact);
                    } else (eventHandlers.get(i)).messageReceived(this, message);
                } catch (Exception e) {
                    for (ConnectionEventListener eventHandler : eventHandlers) {
                        eventHandler.errorOccured(i + ": Failure while processing a received message.", e);
                    }
                }
            }
        } catch (Exception e) {
            for (ConnectionEventListener eventHandler : eventHandlers) {
                eventHandler.errorOccured("Failure while receiving a message", e);
            }
        }
    }

    private String normalizeName(String userName) {
        int index = userName.indexOf('/');
        if (index > 0) {
            return userName.substring(0, index);
        }
        return userName;
    }

    public void disconnect(boolean intentional) {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
        super.disconnect(intentional);
    }

    public void reconnect() {
        try {
            if (connection != null) {
                connection.disconnect();
            }
            connect();
        } catch (Exception e) {
            log.log(Level.SEVERE, "", e);
        }
    }

    public boolean isLoggedIn() {
        return connection != null && connection.isConnected();
    }

    /**
     * Cancel login.
     */
    public void cancel() {
        if (!isLoggedIn()) connection.disconnect();
    }

    public void setTimeout(int timeout) {
    }

    public void addContact(Nameable contact, Group group) {
        String[] groupNames = new String[1];
        groupNames[0] = group.getName();
        try {
            connection.getRoster().createEntry(fixUserName(contact.getName()), contact.getName(), groupNames);
        } catch (XMPPException e) {
            for (ConnectionEventListener connectionEventListener : eventHandlers) {
                connectionEventListener.errorOccured("Failed to add a contact " + contact.getName(), e);
            }
        }
        group.add(contact);
    }

    /**
     * Used to fix the usernames for the jabber protocol.<br>
     * Usernames need server name.
     * @param name of the user's account to fix
     * @return name, including server.
     */
    protected String fixUserName(String name) {
        if (name.indexOf('@') > -1) return name;
        return name + "@" + getServerName();
    }

    public boolean removeContact(Nameable contact, Group group) {
        if (group != null && group.getName().equals(GROUP_UNFILED)) {
            for (RosterEntry rosterEntry : connection.getRoster().getUnfiledEntries()) {
                if (rosterEntry != null && rosterEntry.getUser() != null && rosterEntry.getUser().equals(contact.getName())) {
                    try {
                        connection.getRoster().removeEntry(rosterEntry);
                        cleanGroup(group, contact);
                    } catch (XMPPException e) {
                        notifyErrorOccured("Found, but failed to remove the contact", e);
                        return false;
                    }
                    return true;
                }
            }
        } else {
            for (RosterGroup rosterGroup : connection.getRoster().getGroups()) {
                if (group == null || rosterGroup.getName().equalsIgnoreCase(group.getName())) {
                    for (RosterEntry rosterEntry : rosterGroup.getEntries()) {
                        if (rosterEntry.getName().equals(contact.getName())) {
                            try {
                                rosterGroup.removeEntry(rosterEntry);
                                cleanGroup(group, contact);
                            } catch (XMPPException e) {
                                notifyErrorOccured("Found, but failed to remove the contact", e);
                                return false;
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void moveContact(Nameable contact, Group oldGroup, Group newGroup) {
        removeContact(contact, oldGroup);
        addContact(contact, newGroup);
    }

    public void addContactGroup(Group group) {
        connection.getRoster().createGroup(group.getName());
    }

    public void removeContactGroup(Group group) {
    }

    /**
     * Returns a short name for the service.
     * "AIM", "ICQ" etc.
     *
     * @return service name
     */
    public String getServiceName() {
        return "Jabber";
    }

    /**
     * True if this is a system message.
     *
     * @param contact to check
     * @return true if a system message
     */
    public boolean isSystemMessage(Nameable contact) {
        return false;
    }

    /**
     * Sets the away flag.
     *
     * @param away true if so
     */
    public void setAway(boolean away) {
        if (connection != null && connection.isConnected()) {
            Presence presence = new Presence(Presence.Type.available);
            if (away) {
                presence.setMode(Presence.Mode.away);
                presence.setStatus(getProperties().getIamAwayMessage());
            }
            connection.sendPacket(presence);
        } else {
            notifyConnectionFailed("Connection unavailable.");
        }
        super.setAway(away);
    }

    /**
     * Overide this message with code that sends the message out.
     *
     * @param message to send
     * @throws java.io.IOException problems
     */
    protected void processMessage(Message message) throws IOException {
        Chat chat = connection.getChatManager().createChat(message.getContact().getName(), null);
        try {
            chat.sendMessage(message.getText());
        } catch (XMPPException e) {
            log.log(Level.SEVERE, "", e);
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Overide this message with code that sends the message out.
     *
     * @param message to send
     * @throws java.io.IOException problems
     */
    protected void processSecureMessage(Message message) throws IOException {
        processMessage(message);
    }

    public String getDefaultIconName() {
        return "jabber.gif";
    }

    public void initiateFileTransfer(FileTransferListener ftl) throws IOException {
        ftl.notifyNegotiation();
        OutgoingFileTransfer oft = fileTransferManager.createOutgoingFileTransfer(fixUserName(ftl.getContactName()));
        try {
            ftl.notifyTransfer();
            oft.sendFile(ftl.getFile(), ftl.getFileDescription());
        } catch (XMPPException e) {
            ftl.notifyFail();
            log.log(Level.SEVERE, "Transfer failed", e);
        }
    }

    public void acceptFileTransfer(FileTransferListener ftl, Object connectionInfo) {
        FileTransferRequest fileTransferRequest = (FileTransferRequest) connectionInfo;
        ftl.notifyNegotiation();
        IncomingFileTransfer ift = fileTransferRequest.accept();
        try {
            ftl.notifyTransfer();
            ift.recieveFile(ftl.getFile());
        } catch (XMPPException e) {
            ftl.notifyFail();
            log.log(Level.SEVERE, "Transfer failed", e);
        }
    }

    public void rejectFileTransfer(Object connectionInfo) {
        FileTransferRequest fileTransferRequest = (FileTransferRequest) connectionInfo;
        fileTransferRequest.reject();
    }

    public void join(String room, final String nickname, final ChatRoomEventListener listener) {
        join(false, room, nickname, listener);
    }

    public void create(String room, String nickname, ChatRoomEventListener listener) {
        join(true, room, nickname, listener);
    }

    private void join(boolean create, String room, final String nickname, final ChatRoomEventListener listener) {
        try {
            multiUserChat = new MultiUserChat(connection, room);
            if (create) multiUserChat.create(nickname); else multiUserChat.join(nickname);
            RoomInfo info = MultiUserChat.getRoomInfo(connection, room);
            if (info.getOccupantsCount() != -1) listener.serverNotification("Number of occupants: " + info.getOccupantsCount());
            if (GeneralUtils.isNotEmpty(info.getSubject())) listener.serverNotification("Room Subject: " + info.getSubject());
            multiUserChat.addSubjectUpdatedListener(new SubjectUpdatedListener() {

                public void subjectUpdated(String subject, String from) {
                    listener.serverNotification("Room Subject:" + subject);
                }
            });
            multiUserChat.addMessageListener(new PacketListener() {

                public void processPacket(Packet packet) {
                    int lastSlash = packet.getFrom().lastIndexOf('/');
                    String from = lastSlash == -1 || lastSlash >= packet.getFrom().length() ? packet.getFrom().trim() : packet.getFrom().substring(lastSlash + 1);
                    if (from.equalsIgnoreCase(nickname)) return;
                    if (packet instanceof org.jivesoftware.smack.packet.Message) {
                        org.jivesoftware.smack.packet.Message message = (org.jivesoftware.smack.packet.Message) packet;
                        try {
                            listener.messageReceived(SmackConnection.this, new MessageImpl(getContactFactory().create(from, SmackConnection.this), false, message.getBody()));
                        } catch (Exception e) {
                            listener.errorOccured("Error receiving message: " + e.getMessage(), e);
                        }
                    }
                }
            });
        } catch (XMPPException e) {
            listener.errorOccured("Error connecting to the room: " + e.getMessage(), e);
        }
    }

    public void sendChatMessage(String message) {
        if (isJoined()) try {
            multiUserChat.sendMessage(message);
        } catch (XMPPException e) {
            for (ConnectionEventListener eventHandler : eventHandlers) {
                eventHandler.errorOccured("Failure while sending a message", e);
            }
        }
    }

    public boolean isJoined() {
        return multiUserChat != null && multiUserChat.isJoined();
    }
}
