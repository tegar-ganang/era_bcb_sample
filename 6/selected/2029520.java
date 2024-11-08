package com.itbs.aimcer.commune.msn;

import com.itbs.aimcer.bean.*;
import com.itbs.aimcer.commune.AbstractMessageConnection;
import com.itbs.aimcer.commune.ConnectionEventListener;
import com.itbs.util.GeneralUtils;
import net.sf.jml.*;
import net.sf.jml.event.MsnAdapter;
import net.sf.jml.impl.MsnMessengerFactory;
import net.sf.jml.message.*;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This implementation provides a MSN Messenger connection using the JML and JClaim libraries.
 * <p>
 * Implementation Note: Some people prefer friend.getDisplayName() v.s. friend.getFriendlyName().
 * That's why getMSNName() is intoduced.
 *
 * @author Chris Chiappone, Alex Rass
 * @since Oct, 2006
 */
public class JmlMsnConnection extends AbstractMessageConnection {

    private static final Logger log = Logger.getLogger(JmlMsnConnection.class.getName());

    MsnMessenger connection = null;

    Map<Email, MsnSwitchboard> sessions = new ConcurrentHashMap<Email, MsnSwitchboard>();

    private static final String SYSTEM = "System Message";

    protected boolean loggedIn;

    public void connect() throws SecurityException, Exception {
        loggedIn = false;
        log.setLevel(Level.ALL);
        super.connect();
        sessions.clear();
        notifyConnectionInitiated();
        String username = getUserName();
        if (username.indexOf('@') == -1) {
            throw new SecurityException("MSN usernames must contain full domain");
        }
        connection = MsnMessengerFactory.createMsnMessenger(username, getPassword());
        connection.setLogIncoming(false);
        connection.setLogOutgoing(false);
        initMessenger(connection);
        connection.login();
    }

    class ConnectionListener extends MsnAdapter {

        public void exceptionCaught(MsnMessenger messenger, Throwable throwable) {
            loggedIn = false;
            if (throwable.getMessage().startsWith("Login Failed")) {
                notifyConnectionFailed("Failed to login.");
            } else {
                notifyErrorOccured(messenger + throwable.toString(), new Exception(throwable));
            }
            log.log(Level.SEVERE, messenger + throwable.toString(), throwable);
        }

        public void loginCompleted(MsnMessenger messenger) {
            log.fine(messenger + " login complete ");
        }

        public void logout(MsnMessenger messenger) {
            loggedIn = false;
            notifyConnectionLost();
            log.fine(messenger + " logout");
        }

        public void instantMessageReceived(MsnSwitchboard switchboard, MsnInstantMessage message, MsnContact friend) {
            sessions.put(friend.getEmail(), switchboard);
            Message jcMessage = new MessageImpl(getContactFactory().create(friend.getEmail().getEmailAddress(), JmlMsnConnection.this), false, false, message.getContent());
            for (ConnectionEventListener eventHandler : eventHandlers) {
                try {
                    eventHandler.messageReceived(JmlMsnConnection.this, jcMessage);
                } catch (Exception e) {
                    notifyErrorOccured("Failure while receiving a message", e);
                }
            }
        }

        public void systemMessageReceived(MsnMessenger messenger, MsnSystemMessage message) {
            log.fine(messenger + " recv system message " + message);
            Message jcMessage = new MessageImpl(getContactFactory().create(SYSTEM, JmlMsnConnection.this), false, false, message.getContent());
            for (ConnectionEventListener eventHandler : eventHandlers) {
                try {
                    eventHandler.messageReceived(JmlMsnConnection.this, jcMessage);
                } catch (Exception e) {
                    notifyErrorOccured("Failure while receiving a message", e);
                }
            }
        }

        public void controlMessageReceived(MsnSwitchboard switchboard, MsnControlMessage message, MsnContact contact) {
            log.fine(switchboard + " recv control message from " + contact.getEmail());
            message.setTypingUser(switchboard.getMessenger().getOwner().getEmail().getEmailAddress());
            switchboard.sendMessage(message, false);
        }

        public void datacastMessageReceived(MsnSwitchboard switchboard, MsnDatacastMessage message, MsnContact friend) {
            log.fine(switchboard + " recv datacast message " + message);
            switchboard.sendMessage(message, false);
        }

        public void unknownMessageReceived(MsnSwitchboard switchboard, MsnUnknownMessage message, MsnContact friend) {
            log.fine(switchboard + " recv unknown message: " + message);
            MsnInstantMessage msnInstantMessage = new MsnInstantMessage();
            if (message.getContent() != null) {
                msnInstantMessage.setContent(message.getContentAsString());
                instantMessageReceived(switchboard, msnInstantMessage, friend);
            }
        }

        public void contactListInitCompleted(MsnMessenger messenger) {
            log.fine(messenger + " contact list init completed");
            loggedIn = true;
            notifyConnectionEstablished();
        }

        public void contactListSyncCompleted(MsnMessenger messenger) {
            log.fine(messenger + " contact list sync completed");
            MsnGroup[] groups = connection.getContactList().getGroups();
            Group gw;
            for (MsnGroup group : groups) {
                gw = getGroupFactory().create(group.getGroupName());
                populateContactsFromList(group.getContacts(), gw, false);
            }
            MsnGroup defaultGroup = connection.getContactList().getDefaultGroup();
            gw = getGroupFactory().create(defaultGroup.getGroupName());
            populateContactsFromList(connection.getContactList().getContactsInList(MsnList.FL), gw, true);
            for (ConnectionEventListener eventHandler : eventHandlers) {
                eventHandler.statusChanged(JmlMsnConnection.this);
            }
        }

        /**
         * Populates the contact list from MSN's data.
         * @param contacts to run through
         * @param gw to assign to.
         * @param orphansOnly determins if only orphaned contacts be assigned to that group. false = everyone, true=only orphans
         */
        private void populateContactsFromList(MsnContact[] contacts, Group gw, boolean orphansOnly) {
            Contact cw;
            for (MsnContact friend : contacts) {
                if (!orphansOnly || friend.getBelongGroups().length == 0) {
                    boolean online = MsnUserStatus.OFFLINE != friend.getStatus() && MsnUserStatus.HIDE != friend.getStatus();
                    boolean away = online && MsnUserStatus.ONLINE != friend.getStatus();
                    cw = getContactFactory().create(friend.getEmail().getEmailAddress(), JmlMsnConnection.this);
                    cw.getStatus().setOnline(online);
                    cw.getStatus().setAway(away);
                    cw.setDisplayName(GeneralUtils.stripHTML(getMSNName(friend)));
                    gw.add(cw);
                }
            }
            getGroupList().add(gw);
        }

        public void contactStatusChanged(MsnMessenger messenger, MsnContact friend) {
            log.fine(messenger + " friend " + friend.getEmail() + " status changed from " + friend.getOldStatus() + " to " + friend.getStatus());
            Contact contact = getContactFactory().create(friend.getEmail().getEmailAddress(), JmlMsnConnection.this);
            contact.setDisplayName(GeneralUtils.stripHTML(getMSNName(friend)));
            boolean online = MsnUserStatus.OFFLINE != friend.getStatus() && MsnUserStatus.HIDE != friend.getStatus();
            boolean away = online && MsnUserStatus.ONLINE != friend.getStatus();
            Status status = (Status) contact.getStatus().clone();
            contact.getStatus().setOnline(online);
            contact.getStatus().setAway(away);
            contact.getStatus().setIdleTime(0);
            notifyStatusChanged(contact, status);
        }

        public void ownerStatusChanged(MsnMessenger messenger) {
            log.fine(messenger + " status changed from " + messenger.getOwner().getOldStatus() + " to " + messenger.getOwner().getStatus());
        }

        public void contactAddedMe(MsnMessenger messenger, MsnContact friend) {
            log.fine(friend.getEmail() + " add " + messenger);
        }

        public void contactRemovedMe(MsnMessenger messenger, MsnContact friend) {
            log.fine(friend.getEmail() + " remove " + messenger);
        }

        public void switchboardClosed(MsnSwitchboard switchboard) {
            log.fine(switchboard + " closed");
            sessions.remove(switchboard.getMessenger().getOwner().getEmail());
        }

        public void switchboardStarted(MsnSwitchboard switchboard) {
            log.fine(switchboard + " started");
            sessions.put(switchboard.getMessenger().getOwner().getEmail(), switchboard);
        }

        public void contactJoinSwitchboard(MsnSwitchboard switchboard, MsnContact friend) {
            log.fine(friend.getEmail() + " join " + switchboard);
        }

        public void contactLeaveSwitchboard(MsnSwitchboard switchboard, MsnContact friend) {
            log.fine(friend.getEmail() + " leave " + switchboard);
        }
    }

    protected String getMSNName(MsnContact friend) {
        return friend.getDisplayName();
    }

    protected void initMessenger(MsnMessenger messenger) {
        messenger.getOwner().setInitStatus(MsnUserStatus.ONLINE);
        messenger.addListener(new ConnectionListener());
    }

    protected void processMessage(Message content) throws IOException {
        connection.sendText(Email.parseStr(content.getContact().getName()), content.getPlainText());
    }

    protected void processSecureMessage(Message content) throws IOException {
        connection.sendText(Email.parseStr(content.getContact().getName()), content.getPlainText());
    }

    public boolean isSystemMessage(Nameable arg0) {
        return SYSTEM.equalsIgnoreCase(arg0.getName());
    }

    public void addContact(Nameable contact, Group group) {
        connection.addGroup(group.getName());
        Email email = Email.parseStr(contact.getName());
        if (email == null) {
            notifyErrorOccured("Name is not not an email address (MSN requirement).", null);
        }
        connection.addFriend(email, contact.getName());
        connection.moveFriend(email, "", group.getName());
    }

    public void addContactGroup(Group group) {
        connection.addGroup(group.getName());
    }

    public void cancel() {
        disconnect(true);
    }

    public void disconnect(boolean intentional) {
        sessions.clear();
        if (connection != null) {
            connection.logout();
        }
        super.disconnect(intentional);
    }

    public void reconnect() {
        disconnect(false);
        try {
            connect();
        } catch (Exception e) {
            log.info("Failed to reconnect");
        }
    }

    public String getServiceName() {
        return "MSN";
    }

    public boolean isLoggedIn() {
        return connection != null && connection.getConnection() != null && loggedIn;
    }

    public boolean removeContact(Nameable contact, Group group) {
        connection.removeFriend(Email.parseStr(contact.getName()), group.getName());
        cleanGroup(group, contact);
        return true;
    }

    public void removeContactGroup(Group group) {
        connection.removeGroup(group.getName());
    }

    public void moveContact(Nameable contact, Group oldGroup, Group newGroup) {
        connection.moveFriend(Email.parseStr(contact.getName()), oldGroup.getName(), newGroup.getName());
        oldGroup.remove(contact);
        newGroup.add(contact);
    }

    public void setTimeout(int arg0) {
    }
}
