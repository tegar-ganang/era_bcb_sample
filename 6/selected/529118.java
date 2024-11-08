package org.jiaho.server;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jiaho.client.JiahoContact;
import org.jiaho.client.JiahoMessage;
import org.jiaho.client.JiahoMessageBox;

/**
 *
 * @author Manuel Martins
 */
public class testeConnection implements PacketListener, RosterListener, ConnectionListener, InvitationListener, MessageListener {

    private XMPPConnection connection;

    private ConnectionConfiguration config;

    private boolean isLogin = false;

    private boolean isClose = true;

    private boolean newMessages = false;

    private boolean newRooster = false;

    private String user = "";

    public static enum TYPE {

        UNSECURE_CONNECTION, SECURE_CONNECTION
    }

    ;

    public static enum PRESENCE_MODE {

        available, away, dnd, xa
    }

    ;

    public static enum PRESENCE_TYPE {

        available, error, subscribe, subscribed, unavailable, unsubscribe, unsubscribed
    }

    ;

    private Vector<JiahoMessage> messagelist = new Vector<JiahoMessage>();

    private Vector<JiahoContact> contactlist = new Vector<JiahoContact>();

    private Vector<String> new_entries = new Vector<String>();

    private ChatManager chat_manager;

    private Chat newChat;

    private HashMap<String, Chat> open_chats = new HashMap<String, Chat>();

    private Date date;

    /**
     * Constructs a new <tt>Connection</tt> with the specified arguments.
     *
     * @param host the host server to connect
     * @param port the port acepting connections from the host server
     * @param username the username
     * @param password the user password
     * @return nothing
     */
    public testeConnection(String host, int port, String username, String password, boolean newAccount) {
        try {
            this.config = new ConnectionConfiguration(host, port);
            this.config.setSASLAuthenticationEnabled(true);
            this.connection = new XMPPConnection(this.config);
            this.connection.connect();
            this.user = username;
            if (newAccount) {
                this.connection.getAccountManager().createAccount(username, password);
                this.connection.login(username, password, "jiaho");
                this.connection.addPacketListener(this, new MessageTypeFilter(Message.Type.chat));
                this.connection.getRoster().addRosterListener(this);
                this.connection.addConnectionListener(this);
                this.chat_manager = this.connection.getChatManager();
                this.isLogin = true;
                this.isClose = false;
            } else {
                this.connection.login(username, password, "jiaho");
                this.connection.addPacketListener(this, new MessageTypeFilter(Message.Type.chat));
                this.connection.getRoster().addRosterListener(this);
                this.connection.addConnectionListener(this);
                this.chat_manager = this.connection.getChatManager();
                this.isLogin = true;
                this.isClose = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the username of the user connected.
     *
     * @return <tt>user</tt>the username
     */
    public String getUsername() {
        return (this.user);
    }

    /**
     * Returns the Jabber Identification (JID) from the user connected.
     *
     * @return <tt>jid</tt>the JabberID
     */
    public String getJID() {
        String temp = StringUtils.parseBareAddress(this.connection.getUser());
        return (temp);
    }

    /**
     * Disconnects from host server.
     */
    public void disconnect() {
        try {
            this.connection.disconnect();
            this.isLogin = false;
            this.isClose = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Return if user is login.
     *
     * @return <tt>true</tt> if the user is login
     */
    public boolean isLogin() {
        return (this.isLogin);
    }

    /**
     * Returns the <tt>XMPPConnection</tt> initialized.
     *
     * @return <tt>XMPPConnection</tt> this connection
     */
    public XMPPConnection getConnection() {
        XMPPConnection con = null;
        try {
            con = this.connection;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (con);
    }

    /**
     * Process the received packets, acording to MessageTypeFilter. (smack - PacketListener)
     * 
     * @param <tt>packet</tt> the packet received
     */
    public void processPacket(Packet packet) {
        try {
            org.jivesoftware.smack.packet.Message message = (org.jivesoftware.smack.packet.Message) packet;
            JiahoMessage chatMessage = new JiahoMessage(StringUtils.parseBareAddress(message.getFrom()));
            this.date = new Date();
            chatMessage.setBody("<FONT COLOR='#008000'>" + this.formatDate(this.date) + ", <B>" + StringUtils.parseName(message.getFrom()) + " says: </B><BR>" + message.getBody() + "</FONT><BR>");
            chatMessage.setSubject(message.getSubject());
            chatMessage.setNewMSG(true);
            this.messagelist.add(chatMessage);
            this.newMessages = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Formats the date to: day month day hh:mm:ss, ex. Thu Oct 18 19:00:10.
     *
     * @param date_unformated the date to format
     * @return a string with formated date
     */
    private String formatDate(Date date_unformated) {
        String temp = new String();
        String[] temp1 = new String[2];
        temp = String.valueOf(date_unformated);
        temp1 = temp.split(" BST");
        temp = temp1[0];
        return (temp);
    }

    /**
     * Sends a message.
     *
     * @param <tt>packet</tt> the packet to send (message.type.chat)
     */
    public void sendMessage(Packet packet) {
        try {
            this.connection.sendPacket(packet);
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    /**
     * Returns a Vector containing the chat messages received.
     *
     * @return <tt>chatMessages</tt> the chat messages vector
     */
    public Vector getChatMessages() {
        Vector chatMessages = new Vector();
        for (int i = 0; i < this.messagelist.size(); i++) {
            chatMessages.add(this.messagelist.get(i));
        }
        this.messagelist.clear();
        return (chatMessages);
    }

    /**
     * Returns a Vector containing the users from the roster.
     *
     * @return <tt>contactlist</tt> the contact list Vector
     */
    public Vector getUsersfromRoster() {
        this.contactlist.clear();
        try {
            Roster roster = this.connection.getRoster();
            for (RosterEntry entry : roster.getEntries()) {
                JiahoContact contact = new JiahoContact();
                contact.setPresenceMode(this.getPresence(entry.getUser()));
                contact.setJabberID(entry.getUser());
                contact.setName(entry.getName());
                this.contactlist.add(contact);
            }
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        return (this.contactlist);
    }

    /**
     * Returns a Vector containing the users from the roster.
     * 
     * @return <tt>contactlist</tt> the contact list Vector
     */
    public Vector getRosterVector() {
        return (this.contactlist);
    }

    /**
     * Returns true if a new message was received.
     *
     * @return true if new message received
     */
    public boolean isNewMessage() {
        return (this.newMessages);
    }

    /**
     * Returns true if new entries were added to rooster.
     *
     * @return true if new entry was added
     */
    public boolean isNewRooster() {
        return (this.newRooster);
    }

    /**
     * Returns the Presence from specified user.
     * 
     * @param jid the JabberID from the user
     * @return the presence
     */
    public String getPresence(String jid) {
        Roster roster = this.connection.getRoster();
        String temp = roster.getPresence(jid).toString();
        return (temp);
    }

    /**
     * Returns a Vector containing the new entries added.
     *
     * @return <tt>new_entries</tt> the entries added
     */
    public Vector getNewEntries() {
        return (this.new_entries);
    }

    /**
     * Sends a packet containing the presence.
     *
     * @param presence the presence to send
     * @return true if succeed
     */
    public boolean sendPresence(Presence presence) {
        boolean success = false;
        try {
            this.connection.sendPacket(presence);
            success = true;
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        return (success);
    }

    /**
     * If new entries were added to rooster this method is invoked. (smack - RosterListener)
     *
     * @param entries the entries added
     */
    public void entriesAdded(Collection<String> entries) {
        this.new_entries.addAll(entries);
        this.newRooster = true;
    }

    /**
     * If entries were updated this method is invoked. (smack - RosterListener)
     *
     * @param entries the entries updated
     */
    public void entriesUpdated(Collection<String> entries) {
    }

    /**
     * If entries were deleted this method is invoked. (smack - RosterListener)
     *
     * @param entries the deleted entries
     */
    public void entriesDeleted(Collection<String> entries) {
    }

    /**
     * If presence changes this method is invoked. (smack - RosterListener)
     *
     * @param presence the presence
     */
    public void presenceChanged(Presence presence) {
    }

    /**
     * Returns the connection State
     * 
     * @return <tt>isClose</tt> true if closed
     */
    public boolean isClose() {
        return (this.isClose);
    }

    /**
     * If the connection was closed this method is invoked. (smack - ConnectionListener)
     */
    public void connectionClosed() {
    }

    /**
     * If the connection was closed on Error this method is invoked. (smack - ConnectionListener)
     *
     * @param e the Exception
     */
    public void connectionClosedOnError(Exception e) {
        this.isClose = true;
    }

    /**
     * Reconnecting in x seconds. (smack - ConnectionListener)
     *
     * @param seconds the time in seconds
     */
    public void reconnectingIn(int seconds) {
    }

    /**
     * Reconnection Successful.(smack - ConnectionListener)
     */
    public void reconnectionSuccessful() {
    }

    /**
     * if reconnection fails this method is invoked. (smack - ConnectionListener)
     * @param e the Exception
     */
    public void reconnectionFailed(Exception e) {
    }

    /**
     * Adds a new contact to the rooster.
     *
     * @param JID the JabberID of the user to add
     */
    public void addContact(String JID) {
        try {
            Roster roster = this.connection.getRoster();
            roster.createEntry(JID, StringUtils.parseName(JID), null);
        } catch (XMPPException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Remove a contact.
     *
     * @param JID the JabberID to remove
     */
    public void remContact(String JID) {
        try {
            Roster roster = this.connection.getRoster();
            RosterEntry rosterentry = roster.getEntry(JID);
            roster.removeEntry(rosterentry);
        } catch (XMPPException ex) {
            ex.printStackTrace();
        }
    }

    public void createChat(String JID) {
        try {
            this.newChat = chat_manager.createChat(JID, this);
            this.open_chats.put(JID, this.newChat);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void removeChat(String JID) {
        try {
            if (this.open_chats.containsKey(JID)) {
                this.open_chats.remove(JID);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendMessageToExistingChat(String JID, org.jivesoftware.smack.packet.Message message) {
        try {
            if (this.open_chats.containsKey(JID)) {
                this.open_chats.get(JID).sendMessage(message);
            }
        } catch (XMPPException ex) {
            ex.printStackTrace();
        }
    }

    public void invitationReceived(XMPPConnection conn, String room, String inviter, String reason, String password, Message message) {
    }

    public void processMessage(Chat chat, Message message) {
        String JID = chat.getParticipant();
        JiahoMessage chatMessage = new JiahoMessage(StringUtils.parseBareAddress(message.getFrom()));
        this.date = new Date();
        chatMessage.setBody("<FONT COLOR='#008000'>" + this.formatDate(this.date) + ", <B>" + StringUtils.parseName(message.getFrom()) + " says: </B><BR>" + message.getBody() + "</FONT><BR>");
        chatMessage.setSubject(message.getSubject());
        chatMessage.setNewMSG(true);
        this.messagelist.add(chatMessage);
        this.newMessages = true;
    }
}
