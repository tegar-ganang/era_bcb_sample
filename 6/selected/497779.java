package com.itbs.aimcer.commune.msn;

import com.itbs.aimcer.bean.*;
import com.itbs.aimcer.commune.AbstractMessageConnection;
import com.itbs.aimcer.commune.ConnectionEventListener;
import com.itbs.aimcer.commune.FileTransferListener;
import com.itbs.util.GeneralUtils;
import rath.msnm.BuddyList;
import rath.msnm.MSNMessenger;
import rath.msnm.SwitchboardSession;
import rath.msnm.UserStatus;
import rath.msnm.entity.MsnFriend;
import rath.msnm.event.MsnAdapter;
import rath.msnm.ftp.VolatileDownloader;
import rath.msnm.ftp.VolatileTransferServer;
import rath.msnm.msg.MimeMessage;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides connection to MSN.
 *
 * @author Alex Rass
 * @since Dec 24, 2004
 */
public class MSNConnection extends AbstractMessageConnection {

    private static Logger log = Logger.getLogger(MSNConnection.class.getName());

    MSNMessenger connection = null;

    Map<String, SwitchboardSession> sessions = new ConcurrentHashMap<String, SwitchboardSession>();

    /**
     * Non-blocking call.
     */
    public void connect() throws Exception {
        try {
            super.connect();
            sessions.clear();
            notifyConnectionInitiated();
            String username = getUserName();
            if (username.indexOf('@') == -1) throw new SecurityException("MSN usernames must contain '@msn.com' or '@hotmail.com'");
            connection = new MSNMessenger("", "");
            connection.setInitialStatus(UserStatus.ONLINE);
            connection.addMsnListener(new ConnectionListener());
            connection.setInitialStatus(UserStatus.ONLINE);
            connection.login(getUserName(), getPassword());
        } catch (Exception e) {
            notifyConnectionFailed(e.getMessage());
            throw e;
        }
    }

    class ConnectionListener extends MsnAdapter {

        public void loginComplete(MsnFriend own) {
            notifyConnectionEstablished();
        }

        public void progressTyping(SwitchboardSession ss, MsnFriend friend, String typingUser) {
            sessions.put(friend.getLoginName(), ss);
            for (ConnectionEventListener connectionEventListener : eventHandlers) {
                connectionEventListener.typingNotificationReceived(MSNConnection.this, getContactFactory().create(friend.getLoginName(), MSNConnection.this));
            }
        }

        public void notifyUnreadMail(Properties Prop, int unread) {
            final String text = "Unread Email Count: " + unread;
            Message message = new MessageImpl(getContactFactory().create(getUserName(), MSNConnection.this), false, text);
            notifyEmailReceived(message);
        }

        public void userOnline(MsnFriend friend) {
            Contact contact = getContactFactory().get(friend.getLoginName(), MSNConnection.this);
            if (contact != null) {
                contact.setDisplayName(GeneralUtils.stripHTML(friend.getFormattedFriendlyName()));
                Status oldStatus = (Status) contact.getStatus().clone();
                contact.getStatus().setOnline(true);
                contact.getStatus().setAway(false);
                contact.getStatus().setIdleTime(0);
                notifyStatusChanged(contact, oldStatus);
            } else {
                log.fine("got MSN contact status w/o it being in the list");
            }
        }

        public void userOffline(String loginName) {
            Contact contact = getContactFactory().get(loginName, MSNConnection.this);
            if (contact != null) {
                Status oldStatus = (Status) contact.getStatus().clone();
                contact.getStatus().setOnline(false);
                notifyStatusChanged(contact, oldStatus);
            } else {
                log.fine("got MSN contact status w/o it being in the list");
            }
        }

        public void loginError(String header) {
            String message;
            if ("911".equals(header)) message = "Incorrect logon name or password."; else if ("921".equals(header)) message = "Can't connect to server - too many users."; else message = "Connection failed.";
            connection.logout();
            for (ConnectionEventListener eventHandler : eventHandlers) {
                eventHandler.connectionFailed(MSNConnection.this, message);
            }
        }

        public void listAdd(MsnFriend friend) {
            Contact cw = getContactFactory().create(friend.getLoginName(), MSNConnection.this);
            cw.getStatus().setOnline(!friend.getStatus().equals(UserStatus.OFFLINE));
            cw.setDisplayName(GeneralUtils.stripHTML(friend.getFormattedFriendlyName()));
            try {
                log.fine("friend " + friend.getStatus() + " group index " + friend.getGroupIndex());
                rath.msnm.entity.Group msnGroup = connection.getBuddyGroup().getGroupList().getGroup(friend.getGroupIndex());
                if (msnGroup != null) {
                    Group gw = getGroupFactory().create(msnGroup.getName());
                    gw.add(cw);
                    getGroupList().add(gw);
                } else {
                }
            } catch (NullPointerException e) {
                String bug = "Bug in msn implementation. ";
                try {
                    bug = "Bug in msn implementation: " + (connection == null ? " connection is null" : (connection.getBuddyGroup() == null ? "connection.getBuddyGroup()==null" : (connection.getBuddyGroup().getGroupList() == null ? "connection.getBuddyGroup().getGroupList() is null" : (friend.getGroupIndex() == null ? "friend.getGroupIndex() is null" : (connection.getBuddyGroup().getGroupList().getGroup(friend.getGroupIndex()) == null ? "connection.getBuddyGroup().getGroupList().getGroup(friend.getGroupIndex()) is null" : "unknown problem")))));
                } catch (NullPointerException ex) {
                }
                log.log(Level.SEVERE, "Bug!", e);
                for (ConnectionEventListener eventHandler : eventHandlers) {
                    eventHandler.errorOccured("Found a bug in MSN protocol, please report back to us:\n" + bug, e);
                }
            }
        }

        public void instantMessageReceived(SwitchboardSession ss, MsnFriend friend, MimeMessage mime) {
            sessions.put(friend.getLoginName(), ss);
            Message message = new MessageImpl(getContactFactory().create(friend.getLoginName(), MSNConnection.this), false, false, mime.getMessage());
            for (ConnectionEventListener eventHandler : eventHandlers) {
                try {
                    eventHandler.messageReceived(MSNConnection.this, message);
                } catch (Exception e) {
                    notifyErrorOccured("Failure while receiving a message", e);
                }
            }
        }

        public void buddyListModified() {
            BuddyList blist = connection.getBuddyGroup().getForwardList();
            for (int i = 0; i < blist.size(); i++) {
                MsnFriend friend = blist.get(i);
                listAdd(friend);
            }
            for (ConnectionEventListener eventHandler : eventHandlers) {
                eventHandler.statusChanged(MSNConnection.this);
            }
        }

        public void addFailed(int errorCode) {
            notifyErrorOccured("Failed to add.  Error code " + errorCode, null);
        }

        /**
         * 로그�?� 한 후, �?태를 온�?��?�으로 바꾸었�?�때, �?신�?�
         * Contact list�? 있는 사용�?중�? �?태가 Online(혹�?� substate)�?�
         * 사용�?들�?� �?� 메소드를 통해 임�?��?� 길�?�로 날려준다.
         * 문제�?�?� 호출 종료지�?�?� 정확하게 알 수 없다는 것�?�다.
         * <p/>
         * 만약 Online Contact list를 가지고 싶다면, MsnFriend �?체를
         * Map�? 저장해�?면 편리하다. (Key값�?� loginName으로 하면 �?� 좋다)
         */
        public void listOnline(MsnFriend friend) {
            Contact contact = getContactFactory().create(friend.getLoginName(), MSNConnection.this);
            contact.setDisplayName(GeneralUtils.stripHTML(friend.getFriendlyName()));
            Status status = (Status) contact.getStatus().clone();
            contact.getStatus().setOnline(true);
            contact.getStatus().setAway(false);
            contact.getStatus().setIdleTime(0);
            notifyStatusChanged(contact, status);
        }

        public void switchboardSessionStarted(SwitchboardSession ss) {
            sessions.put(ss.getMsnFriend().getLoginName(), ss);
        }

        public void switchboardSessionEnded(SwitchboardSession ss) {
            sessions.remove(ss.getName());
        }

        /**
         * 로그�?� 시, Synchronization value가 달�?�?�때, 서버로부터
         * FL/AL/BL/RL, Group list등�?� 모�? 받게 �?�는�?�, 만만치 않�?�
         * 작업�?�므로, 모�? 다 Update�?�었�?�때 통지�?�는 �?�벤트�?�다.
         */
        public void allListUpdated() {
            buddyListModified();
        }

        public void logoutNotify() {
            notifyConnectionLost();
        }

        /**
         * ???? ???? ??? ???? ?? ???? ?????.
         *
         * @param ss       ??? ???? ? ??? ??? Switchboard??
         * @param cookie   ??? ??? ???.
         * @param filename ?????? ??? ??
         * @param filesize ?????? ??? ??(byte??)
         * @since 0.3
         */
        public void filePosted(SwitchboardSession ss, int cookie, String filename, int filesize) {
            super.filePosted(ss, cookie, filename, filesize);
            log.fine("MSNConnection$ConnectionListener.filePosted");
        }

        /**
         * ???? ?? ??? ??? ???? Accept????? ???? ?????.
         * ? ???? ????? ??? ??? ?????? ??.
         * ? ? ???? ??? ?, ??? ??? ??? ?????? thread?
         * ??? ??? 6891 port? bind??? ?? ???? ???.
         *
         * @param ss     ??? ???? ? ??? ??? Switchboard??
         * @param cookie ??? ????? ???.
         */
        public void fileSendAccepted(SwitchboardSession ss, int cookie) {
            super.fileSendAccepted(ss, cookie);
            log.fine("MSNConnection$ConnectionListener.fileSendAccepted");
        }

        /**
         * ???? ?? ??? ??? ???? Reject??? ???? ?????.
         * ? ???? ????? ??? ??? ?????? ??.
         *
         * @param ss     ??? ???? ? ??? ??? Switchboard??
         * @param cookie ??? ????? ???.
         * @param reason ??? ??? ???? ??? ????.
         */
        public void fileSendRejected(SwitchboardSession ss, int cookie, String reason) {
            super.fileSendRejected(ss, cookie, reason);
            log.fine("MSNConnection$ConnectionListener.fileSendRejected");
        }

        /**
         * ?? ??? ??? ?????? ????.
         *
         * @param server ?? ?? ?? ??.
         */
        public void fileSendStarted(VolatileTransferServer server) {
            super.fileSendStarted(server);
            log.fine("MSNConnection$ConnectionListener.fileSendStarted");
        }

        /**
         * ?? ??? ??? ?????? ????.
         */
        public void fileSendEnded(VolatileTransferServer server) {
            super.fileSendEnded(server);
            log.fine("MSNConnection$ConnectionListener.fileSendEnded");
        }

        /**
         * ?? ???? ??? ?????? ????.
         *
         * @param downloader ?? ???? thread ??.
         */
        public void fileReceiveStarted(VolatileDownloader downloader) {
            log.fine("MSNConnection$ConnectionListener.fileReceiveStarted");
            Contact contact = getContactFactory().get(downloader.getName(), MSNConnection.this);
            for (ConnectionEventListener eventHandler : eventHandlers) {
            }
        }

        /**
         * ??? ????? ??? ?????? ???? ?????.
         * ???, ?? ?? thread? ????.
         */
        public void fileSendError(VolatileTransferServer server, Throwable e) {
            log.fine("MSNConnection$ConnectionListener.fileSendError");
            for (ConnectionEventListener eventHandler : eventHandlers) {
                eventHandler.errorOccured("ERROR while transfering file: " + server.getFilename() + " " + e.getMessage(), null);
            }
        }

        /**
         * ??? ????? ??? ?????? ???? ?????.
         * ???, ?? ?? thread? ????.
         */
        public void fileReceiveError(VolatileDownloader downloader, Throwable e) {
            log.log(Level.SEVERE, "fileReceiveError", e);
            for (ConnectionEventListener eventHandler : eventHandlers) {
                eventHandler.errorOccured("ERROR while transfering file: " + downloader.getFilename() + " " + e.getMessage(), null);
            }
        }
    }

    public void disconnect(boolean intentional) {
        sessions.clear();
        if (connection != null) connection.logout();
        super.disconnect(intentional);
    }

    public void reconnect() {
        disconnect(false);
        try {
            connect();
        } catch (Exception e) {
            log.log(Level.INFO, "Failed to reconnect", e);
        }
    }

    public boolean isLoggedIn() {
        return connection != null && connection.isLoggedIn();
    }

    /**
     * Cancel login.
     */
    public void cancel() {
        disconnect(false);
    }

    public void setTimeout(int timeout) {
    }

    public void addContact(Nameable contact, Group group) {
        try {
            connection.addGroup(group.getName());
            connection.addFriend(contact.getName());
        } catch (IOException e) {
            for (ConnectionEventListener connectionEventListener : eventHandlers) {
                connectionEventListener.errorOccured("Failed to add a contact", e);
            }
            return;
        }
        group.add(contact);
        Thread.yield();
        String index = null;
        Iterator target = connection.getBuddyGroup().getGroupList().iterator();
        while (target.hasNext()) {
            rath.msnm.entity.Group targetGroup = (rath.msnm.entity.Group) target.next();
            if (group.getName().equalsIgnoreCase(targetGroup.getName())) {
                index = targetGroup.getIndex();
                break;
            }
        }
        if (index != null) {
            BuddyList blist = connection.getBuddyGroup().getForwardList();
            for (int i = 0; i < blist.size(); i++) {
                MsnFriend friend = blist.get(i);
                if (contact.getName().equalsIgnoreCase(friend.getLoginName())) {
                    friend.setGroupIndex(index);
                    break;
                }
            }
        } else {
            log.fine("Never found the index.");
        }
    }

    public boolean removeContact(Nameable contact, Group group) {
        try {
            connection.removeFriend(contact.getName());
            cleanGroup(group, contact);
        } catch (IOException e) {
            notifyErrorOccured("ERROR removing a contact " + contact, e);
            return false;
        }
        return true;
    }

    public void addContactGroup(Group group) {
        try {
            connection.addGroup(group.getName());
        } catch (IOException e) {
            for (ConnectionEventListener eventHandler : eventHandlers) {
                eventHandler.errorOccured("ERROR adding a contact.", e);
            }
        }
    }

    public void removeContactGroup(Group group) {
        try {
            connection.removeGroup(group.getName());
        } catch (IOException e) {
            for (ConnectionEventListener eventHandler : eventHandlers) {
                eventHandler.errorOccured("ERROR removing a contact group.", e);
            }
        }
    }

    public void moveContact(Nameable contact, Group oldGroup, Group newGroup) {
        try {
            connection.moveGroupAsFriend(new MsnFriend(contact.getName()), oldGroup.getName(), newGroup.getName());
            oldGroup.remove(contact);
            newGroup.add(contact);
        } catch (IOException e) {
            for (ConnectionEventListener eventHandler : eventHandlers) {
                eventHandler.errorOccured("ERROR moving a contact between groups.", e);
            }
        }
    }

    /**
     * Returns a short name for the service.
     * "AIM", "ICQ" etc.
     *
     * @return service name
     */
    public String getServiceName() {
        return "MSN";
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

    public boolean isAway() {
        if (connection == null) return super.isAway();
        return !UserStatus.ONLINE.equals(connection.getMyStatus());
    }

    /**
     * Sets the away flag.
     *
     * @param away true if so
     */
    public void setAway(boolean away) {
        if (connection != null) try {
            connection.setMyStatus(away ? UserStatus.AWAY_FROM_COMPUTER : UserStatus.ONLINE);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to set status", e);
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
        final MimeMessage msg = new MimeMessage(message.getText());
        msg.setKind(MimeMessage.KIND_MESSAGE);
        SwitchboardSession ss = getSession(message.getContact().getName());
        if (ss == null) {
            for (ConnectionEventListener eventHandler : eventHandlers) {
                eventHandler.errorOccured("Failed to send your message, try again. (Creating Session failed)", null);
            }
        } else {
            try {
                ss.sendInstantMessage(msg);
            } catch (NullPointerException e) {
                sessions.remove(message.getContact().getName());
                for (ConnectionEventListener eventHandler : eventHandlers) {
                    eventHandler.errorOccured("Failed to send your message, try again.", null);
                }
            }
        }
    }

    SwitchboardSession getSession(String name) throws IOException {
        SwitchboardSession ss = sessions.get(name);
        if (ss == null) {
            try {
                ss = connection.doCallWait(name);
                if (ss != null) sessions.put(name, ss);
            } catch (InterruptedException e) {
            }
        }
        return ss;
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

    /**
     * Starts a file transfer.
     *
     * @param ftl listener
     * @throws java.io.IOException on error
     */
    public void initiateFileTransfer(FileTransferListener ftl) throws IOException {
        SwitchboardSession session = getSession(ftl.getContactName());
        if (session != null) {
            try {
                connection.sendFileRequest(getUserName(), ftl.getFile(), session);
            } catch (IOException e) {
                ftl.notifyFail();
            }
        }
    }

    /**
     * Sets up file for receival
     *
     * @param ftl            param
     * @param connectionInfo connection's info needed for transfer
     */
    public void acceptFileTransfer(FileTransferListener ftl, Object connectionInfo) {
        if (connectionInfo instanceof VolatileDownloader) connection.fireFileReceiveStartedEvent((VolatileDownloader) connectionInfo); else log.fine("MSNConnection.acceptFileTransfer not the right class " + connectionInfo.getClass().getName());
    }

    /**
     * Request to cancel the file transfer in progress.
     */
    public void rejectFileTransfer() {
    }
}
