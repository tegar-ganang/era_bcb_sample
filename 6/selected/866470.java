package com.itbs.aimcer.commune.daim;

import com.itbs.aimcer.bean.*;
import com.itbs.aimcer.commune.*;
import com.itbs.util.GeneralUtils;
import org.walluck.oscar.*;
import org.walluck.oscar.channel.aolim.AOLIM;
import org.walluck.oscar.client.AbstractOscarClient;
import org.walluck.oscar.client.Buddy;
import org.walluck.oscar.client.DaimLoginEvent;
import org.walluck.oscar.client.Oscar;
import org.walluck.oscar.handlers.icq.ICQSMSMessage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages AIM connectivity.  Singlehandedly.
 *
 * @author Alex Rass
 * @since Sep 22, 2004
 */
public class DaimConnection extends AbstractMessageConnection implements IconSupport, FileTransferSupport, SMSSupport {

    private static Logger log = Logger.getLogger(DaimConnection.class.getName());

    DaimClient connection;

    ConnectionInfo connectionInfo = new ConnectionInfo(AIMConstants.LOGIN_SERVER_DEFAULT, AIMConstants.LOGIN_PORT);

    boolean loggedIn = false;

    public String getServiceName() {
        return "AIM";
    }

    public boolean isSystemMessage(Nameable contact) {
        return "aolsystemmsg".equals(GeneralUtils.getSimplifiedName(contact.getName()));
    }

    public String getSupportAccount() {
        return "JClaimHelp";
    }

    public boolean isAway() {
        return super.isAway();
    }

    public void setAway(boolean away) {
        super.setAway(away);
    }

    public String getServerName() {
        return connectionInfo.getIp();
    }

    public void setServerName(String address) {
        if (System.getProperty("OSCAR_HOST") == null) {
            connectionInfo.setIp(address);
        }
    }

    public int getServerPort() {
        return connectionInfo.getPort();
    }

    public void setServerPort(int port) {
        if (System.getProperty("OSCAR_PORT") == null) {
            connectionInfo.setPort(port);
        }
    }

    public final void connect() throws Exception {
        try {
            super.connect();
            notifyConnectionInitiated();
            if (getUserName() == null || getPassword() == null) {
                throw new SecurityException("Login information was not available");
            }
            connection = new DaimClient();
            connection.login(getUserName(), getPassword());
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to login", e);
            connection = null;
            loggedIn = false;
            notifyConnectionFailed(e.getMessage());
        }
    }

    public void reconnect() {
        if (getGroupList().size() > 0) try {
            connect();
        } catch (Exception e) {
            log.log(Level.INFO, "Failed to reconnect", e);
        }
    }

    public void disconnect(boolean intentional) {
        if (connection != null) {
            AIMConnection.killAllInSess(connection.getSession());
            connection = null;
            loggedIn = false;
        }
        super.disconnect(intentional);
    }

    public boolean isLoggedIn() {
        return connection != null && loggedIn;
    }

    public void cancel() {
        if (connection != null) disconnect(true);
    }

    public void setTimeout(int timeout) {
    }

    public void processMessage(Message message) {
        if (connection != null) {
            try {
                connection.sendIM(message.getContact().getName(), message.getText(), Oscar.getICQCaps());
            } catch (IOException e) {
                notifyErrorOccured("Failed to send", e);
            }
        } else {
            notifyErrorOccured("Not connected.", null);
        }
    }

    public void processSecureMessage(Message message) throws IOException {
        processMessage(message);
    }

    /**
     * Finds a group.  Helper.
     * @param group to find
     * @return group or null
     * todo implement
     */
    Group findGroup(com.itbs.aimcer.bean.Group group) {
        return null;
    }

    /**
     * Helper method for finding a buddy.
     * @param contact to find
     * @return
     * todo implement
     */
    Buddy findBuddy(Contact contact) {
        return null;
    }

    /**
     *
     * @param contact contact
     * @param group  group
     * @param inGroup true/false
     * @return
     * todo implement
     */
    Buddy findBuddyViaGroup(Nameable contact, com.itbs.aimcer.bean.Group group, boolean inGroup) {
        return null;
    }

    /**
     * Call to add a new contact to your list.
     *
     * @param contact to add
     * @param group   to add to
     */
    public void addContact(final Nameable contact, final com.itbs.aimcer.bean.Group group) {
        try {
            connection.addBuddy(contact.getName(), group.getName());
        } catch (IOException e) {
            notifyErrorOccured("Failed to add buddy.", e);
        }
    }

    /**
     * Call to remove a contact you no longer want.
     *
     * @param contact to remove
     * @param group to delete from
     */
    public boolean removeContact(final Nameable contact, Group group) {
        boolean result = false;
        if (group == null) {
            group = findGroupViaBuddy(contact);
        }
        if (group != null) {
            try {
                result = connection.removeBuddy(contact.getName(), group.getName());
            } catch (IOException e) {
                notifyErrorOccured("Error while removing contact", e);
            }
        }
        cleanGroup(group, contact);
        return result;
    }

    /**
     *
     * @param group
     * todo implement
     */
    public void addContactGroup(com.itbs.aimcer.bean.Group group) {
    }

    /**
     *
     * @param group
     * todo implement
     */
    public void removeContactGroup(com.itbs.aimcer.bean.Group group) {
    }

    /**
     *
     * @param contact to move
     * @param group to move to
     */
    public void moveContact(Nameable contact, com.itbs.aimcer.bean.Group group) {
        moveContact(contact, findGroupViaBuddy(contact), group);
    }

    public void moveContact(Nameable contact, Group oldGroup, Group newGroup) {
        try {
            connection.moveBuddy(contact.getName(), oldGroup.getName(), newGroup.getName());
        } catch (IOException e) {
            notifyErrorOccured("Error while removing contact", e);
        }
        oldGroup.remove(contact);
        newGroup.add(contact);
    }

    public void initiateFileTransfer(final FileTransferListener ftl) throws IOException {
        connection.sendFile(ftl.getContactName(), ftl.getFile());
    }

    public void rejectFileTransfer(Object connectionInfo) {
    }

    public void acceptFileTransfer(FileTransferListener ftl, Object connectionInfo) {
    }

    public void requestPictureForUser(final Contact contact) {
    }

    /**
     * Will remove the picture.
     * todo implement
     */
    public void clearPicture() {
    }

    /**
     * Use this picture for me.
     *
     * @param picture filename
     * todo implement
     */
    public void uploadPicture(final File picture) {
    }

    /**
     * Doing this internally so I have access to all the variables and methods available directly in DaimConnection.
     */
    class DaimClient extends AbstractOscarClient {

        public DaimClient() {
            super();
        }

        AIMSession getSession() {
            return session;
        }

        public void loginDone(DaimLoginEvent dle) {
            loggedIn = true;
            notifyConnectionEstablished();
        }

        public void loginError(DaimLoginEvent dle) {
            String errorMsg = dle.getErrorMsg() == null ? "Unknown" : dle.getErrorMsg();
            loggedIn = false;
            notifyConnectionFailed(errorMsg);
        }

        /**
         * Apparently ICQ can send URLs.
         *
         * @param from user
         * @param uin  UIN# (ICQ)
         * @param url that was passed
         * @param description of the url
         * @param massmessage message
         */
        public void receivedURL(UserInfo from, int uin, String url, String description, boolean massmessage) {
            incomingICQ(from, uin, 0, description);
        }

        /**
         * ICQ can send contact info back and forth.
         * @param from initiator
         * @param uin icq
         * @param contact list of contacts
         * @param massmessage related message
         */
        public void receivedContacts(UserInfo from, int uin, Map contact, boolean massmessage) {
            incomingICQ(from, uin, 0, contact.toString() + massmessage);
        }

        public void incomingIM(Buddy buddy, UserInfo from, AOLIM args) {
            Message message = new MessageImpl(getContactFactory().create(AIMUtil.normalize(buddy.getName()), DaimConnection.this), false, (args.getFlags() & (AIMConstants.AIM_IMFLAGS_AWAY | AIMConstants.AIM_IMFLAGS_OFFLINE)) != 0, args.getMsg());
            for (ConnectionEventListener eventHandler : eventHandlers) {
                try {
                    eventHandler.messageReceived(DaimConnection.this, message);
                } catch (Exception e) {
                    notifyErrorOccured("Failure while receiving a message", e);
                }
            }
        }

        public void incomingICQ(UserInfo from, int uin, int args, String msg) {
            Message message = new MessageImpl(getContactFactory().create(from.getSN(), DaimConnection.this), false, from.getIdleTime() > 0, msg);
            for (ConnectionEventListener eventHandler : eventHandlers) {
                try {
                    eventHandler.messageReceived(DaimConnection.this, message);
                } catch (Exception e) {
                    notifyErrorOccured("Failure while receiving an ICQ message", e);
                }
            }
        }

        public void receivedICQSMS(UserInfo from, int uin, ICQSMSMessage msg, boolean massmessage) {
            Message message = new MessageImpl(getContactFactory().create(from.getSN(), DaimConnection.this), false, massmessage, msg.getText());
            for (ConnectionEventListener eventHandler : eventHandlers) {
                try {
                    eventHandler.messageReceived(DaimConnection.this, message);
                } catch (Exception e) {
                    notifyErrorOccured("Failure while receiving an ICQ message", e);
                }
            }
        }

        private void removeOldBuddies() {
            for (int i = 0; i < getGroupList().size(); i++) {
                Group group = getGroupList().get(i);
                for (int j = 0; j < group.size(); j++) {
                    Nameable contact = group.get(j);
                    if (contact instanceof Contact) {
                        Contact cw = (Contact) contact;
                        if (DaimConnection.this == cw.getConnection()) {
                            group.remove(contact);
                        }
                    }
                }
            }
        }

        private String printProperty(Buddy buddy, Object property) {
            Object res = buddy.getProperty(property);
            if (res != null) return "  " + property + ": " + buddy.getProperty(property) + "\n"; else return "";
        }

        private void printProperties(String prefix, Buddy buddy) {
            if (false) log.info(prefix + ":" + buddy.getName() + ": \n" + printProperty(buddy, Buddy.AVAILABLE) + printProperty(buddy, Buddy.CAPABILITIES) + printProperty(buddy, Buddy.GROUP) + printProperty(buddy, Buddy.IDLE_TIME) + printProperty(buddy, Buddy.MEMBER_SINCE) + printProperty(buddy, Buddy.SIGNON_TIME) + printProperty(buddy, Buddy.STATE) + printProperty(buddy, Buddy.WARN_LEVEL));
        }

        public void newBuddyList(Buddy[] buddies) {
            removeOldBuddies();
            for (Buddy buddy : buddies) {
                Group bGroup = getGroupFactory().create(buddy.getProperty(Buddy.GROUP).toString());
                Contact contact = getContactFactory().create(AIMUtil.normalize(buddy.getName()), DaimConnection.this);
                contact.setDisplayName(buddy.getName());
                printProperties("newBuddyList", buddy);
                bGroup.add(contact);
                getGroupFactory().getGroupList().add(bGroup);
            }
            for (ConnectionEventListener eventHandler : eventHandlers) {
                try {
                    eventHandler.statusChanged(DaimConnection.this);
                } catch (Exception e) {
                    notifyErrorOccured("Failure while receiving an ICQ message", e);
                }
            }
        }

        public void buddyOffline(String sn, Buddy buddy) {
            Contact contact = getContactFactory().create(AIMUtil.normalize(buddy.getName()), DaimConnection.this);
            Status status = (Status) contact.getStatus().clone();
            contact.getStatus().setOnline(false);
            printProperties("buddyOffline", buddy);
            notifyStatusChanged(contact, status);
        }

        public void buddyOnline(String sn, Buddy buddy) {
            Contact contact = getContactFactory().create(AIMUtil.normalize(buddy.getName()), DaimConnection.this);
            Status status = (Status) contact.getStatus().clone();
            contact.getStatus().setOnline(true);
            contact.getStatus().setAway(buddy.isTrue(Buddy.STATE, Buddy.BUDDY_STATE_AWAY));
            int idle = GeneralUtils.getInt(buddy.getProperty(Buddy.IDLE_TIME));
            contact.getStatus().setIdleTime(idle);
            printProperties("buddyOnline", buddy);
            notifyStatusChanged(contact, status);
        }

        public void sendFile(String contactName, File file) {
            filtool.sendFile(contactName, file.getAbsolutePath());
        }

        public void typingNotification(String sn, short typing) {
            Contact contact = getContactFactory().create(AIMUtil.normalize(sn), DaimConnection.this);
            for (ConnectionEventListener eventHandler : eventHandlers) {
                eventHandler.typingNotificationReceived(DaimConnection.this, contact);
            }
        }

        public void logout() {
            notifyConnectionLost();
            loggedIn = false;
        }

        public void setIcon(File icon) {
        }
    }

    public String veryfySupport(String id) {
        if (!GeneralUtils.isNotEmpty(id)) return "Number can't be empty";
        return id.startsWith("+1") ? null : "Must start with +1, like: +18005551234";
    }
}
