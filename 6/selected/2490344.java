package jabber;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.Roster.*;
import org.jivesoftware.smack.filter.*;
import java.util.*;
import gui.*;

/**
 * JabberChat is a wrapper class for the Smack API by Ignite Realtime
 * It manages communications with the Jabber.org IM server
 *
 * @author Robert Walker (mstie74@cs)
 */
public class JabberChat {

    private ConnectionConfiguration config;

    private XMPPConnection chatConnection;

    private Presence presence;

    private ChatManager chatManager;

    private ChatStateManager chatStateManager;

    private Roster roster;

    private Set<String> currentChats;

    private final Object mutex;

    private PacketListener packetListener;

    /**
     * Constructor for Jabber
     */
    public JabberChat() {
        mutex = new Object();
        config = new ConnectionConfiguration("jabber.org", 5222, "jabber.org");
        config.setCompressionEnabled(false);
        config.setSASLAuthenticationEnabled(true);
        chatConnection = new XMPPConnection(config);
        currentChats = new HashSet();
    }

    /**
     * Logs the chat client into the jabber.org chat server.
     * Login assumes the user already has a valid @jabber.org account
     * @param userName the user name to logon without the @jabber.org suffix
     * @param password password in cleartext of the user
     * @throws org.jivesoftware.smack.XMPPException
     */
    public void Login(String userName, String password) throws XMPPException {
        try {
            this.Login(userName, password, Presence.Type.available);
        } catch (XMPPException e) {
            chatConnection.disconnect();
            throw e;
        }
    }

    /**
     * Logs the chat client into the jabber.org chat server using the specified
     * presence type.
     * Login assumes the user already has a valid @jabber.org account
     * @param userName the user name to logon without the @jabber.org suffix
     * @param password password in cleartext of the user
     * @param presenceType presence type of the user.  i.e. Available, Away, etc
     * @throws org.jivesoftware.smack.XMPPException
     */
    public void Login(String userName, String password, Presence.Type presenceType) throws XMPPException {
        try {
            if (chatConnection == null) chatConnection = new XMPPConnection(config);
            if (!this.IsConnected()) chatConnection.connect();
            SASLAuthentication.supportSASLMechanism("PLAIN", 0);
            presence = new Presence(presenceType);
            if (presenceType.equals(Presence.Type.available)) presence.setMode(Presence.Mode.available);
            chatConnection.login(userName, password, "Nestor");
            chatConnection.sendPacket(presence);
            roster = chatConnection.getRoster();
            for (RosterEntry entry : roster.getEntries()) {
                this.SubscribeToPresence(entry.getUser());
            }
            chatConnection.getRoster().setSubscriptionMode(SubscriptionMode.accept_all);
            chatManager = chatConnection.getChatManager();
            chatStateManager = ChatStateManager.getInstance(chatConnection);
            this.AddChatListener();
            this.AddPacketListener();
        } catch (XMPPException e) {
            chatConnection.disconnect();
            throw e;
        }
    }

    /**
      * Subscribes this client to a user's presence
      * This must be called whenever a EntriesAdded is called
      * in the Roster Listener
      * @param userJID User JID (JabberID) to subscribe to
      */
    public void SubscribeToPresence(String userJID) {
        Presence packet = new Presence(Presence.Type.subscribe);
        packet.setTo(userJID);
        chatConnection.sendPacket(packet);
    }

    /**
      * Unsubscribes this client to a user's presence
      * This must be called whenever a entriesDeleted is called
      * in the Roster Listener
      * @param userJID User JID (JabberID) to unsubscribe to
      */
    public void UnsubscribeToPresence(String userJID) {
        Presence packet = new Presence(Presence.Type.unsubscribe);
        packet.setTo(userJID);
        chatConnection.sendPacket(packet);
    }

    /**
     * Disconnects the current chat client thus ending the session
     */
    public synchronized void Disconnect() {
        if (chatConnection != null) {
            chatConnection.disconnect();
            chatManager = null;
            roster = null;
            chatStateManager = null;
            presence = null;
            packetListener = null;
            chatConnection = null;
            currentChats.clear();
        }
    }

    /**
     * Begins a chat session with another jabber.org user
     * Method does not create a message listener
     * Chat.addMessageListener should be called following this method
     * CreateChat assumes that Login has already been called
     *   so that the chat manager and chat state manager objects
     *   are already instantiated
     * @param userJID user JID (JabberID) to begin chat with
     * @return chat object
     * @see Chat
     */
    public Chat CreateChat(String userJID) {
        Chat chat;
        if (chatManager == null) {
            chatManager = chatConnection.getChatManager();
        }
        if (chatStateManager == null) {
            chatStateManager = ChatStateManager.getInstance(chatConnection);
        }
        chat = chatManager.createChat(userJID, null);
        synchronized (mutex) {
            currentChats.add(userJID);
        }
        return chat;
    }

    /**
     * Removes all listeners for a given chat
     * Sets passed chat object to null
     * @param chat chat to close
     */
    public void CloseChat(Chat chat) {
        if (chat != null) {
            Collection<MessageListener> listeners = chat.getListeners();
            if (!listeners.isEmpty()) {
                for (MessageListener listener : listeners) {
                    chat.removeMessageListener(listener);
                }
            }
            synchronized (mutex) {
                currentChats.remove(chat.getParticipant());
            }
            chat = null;
        }
    }

    /**
     * Gets the user's roster object
     * @return roster roster object of user
     * @see Roster
     */
    public Roster GetRoster() {
        return roster;
    }

    /**
     * Returns the user JID (JabberID) of the currently logged in user
     * @return string containing user JID (JabberID)
     */
    public String GetUser() {
        String user = chatConnection.getUser();
        if (user.indexOf("/") > 0) {
            String[] tokens = user.split("/");
            return tokens[0];
        } else return user;
    }

    /**
     * Returns the presence type of a specified user
     * e.g. available, unavailable, etc
     * @param userJID user JID (JabberID) to obtain presence of
     * @return Presence.Type type of user
     * @see Presence.Type
     */
    public Presence.Type GetPresenceType(String userJID) {
        return roster.getPresence(userJID).getType();
    }

    /**
     * Set the presence type of the user
     * e.g. available, unavailable, etc.
     * @param presenceType Presence type to set
     * @see Presence.Type
     */
    public void SetPresenceType(Presence.Type presenceType) {
        synchronized (this) {
            this.presence.setType(presenceType);
            chatConnection.sendPacket(this.presence);
        }
    }

    /**
     * Set the status of the currently logged in user
     * This can be any string the user wants
     * e.g. "I'm out to lunch right now"
     * @param status status of user
     */
    public synchronized void SetStatus(String status) {
        synchronized (this) {
            this.presence.setStatus(status);
            chatConnection.sendPacket(this.presence);
        }
    }

    /**
     * Returns the status of a given user
     * @param userJID user JID (JabberID) to get status of
     * @return status of user
     */
    public String GetStatus(String userJID) {
        return roster.getPresence(userJID).getStatus();
    }

    /**
     * Returns the mode of the current user or null if not set
     * e.g. Free to chat, away, DND, etc.
     * @return Presence.Mode type
     * @see Presence
     */
    public Presence.Mode GetPresenceMode() {
        return this.presence.getMode();
    }

    /**
     * Sets the mode of the current user
     * This shouldn't be confused with SetPresenceType
     * e.g. Free to chat, away, DND, etc.
     * @see Presence.Mode
     */
    public void SetPresenceMode(Presence.Mode mode) {
        synchronized (this) {
            this.presence.setMode(mode);
            chatConnection.sendPacket(this.presence);
        }
    }

    /**
     * Sets the state of the logged on user in a given chat
     * @param chat chat to set state on
     * @param state state of user
     * @throws XMPPException
     * @see ChatState
     */
    public void SetChatState(Chat chat, ChatState state) throws XMPPException {
        if ((chatManager == null) || (chatStateManager == null)) throw new XMPPException("No active chats found");
        chatStateManager.setCurrentState(state, chat);
    }

    /**
     * Adds a buddy to the roster
     * This is an asynchronous call the the jabber server so entries may
     * not appear immediately.
     * @param userJID user JID (JabberID) of the user to add
     * @param nickName nick name for the buddy
     * @throws org.jivesoftware.smack.XMPPException
     */
    public void AddBuddy(String userJID, String nickName) throws XMPPException {
        if (!this.GetUser().equals(userJID)) {
            chatConnection.getRoster().createEntry(userJID, nickName, null);
            roster = chatConnection.getRoster();
        }
    }

    /**
     * Removes a buddy from the roster
     * This is an asynchronous call the the jabber server so entries may
     * not remove immediately.
     * @param entry RosterEntry of buddy to remove
     * @throws XMPPException
     */
    public void RemoveBuddy(RosterEntry entry) throws XMPPException {
        chatConnection.getRoster().removeEntry(entry);
        roster = chatConnection.getRoster();
    }

    /**
     * Checks to see if the user is authenticated
     * @return true if authenticated, false if not
     */
    public boolean IsAuthenticated() {
        return chatConnection.isAuthenticated();
    }

    /**
     * Checks to see if the user is connected to the IM server
     * @return true if connected, false if not
     */
    public boolean IsConnected() {
        return chatConnection.isConnected();
    }

    /**
     * Private method to add a Chat Listener to the JabberChat object
     * The Chat Listener listens for newly created chats
     * and deals with them as necessary
     * @see ChatManagerListener
     */
    private void AddChatListener() {
        final JabberChat conn = this;
        synchronized (mutex) {
            chatManager.addChatListener(new ChatManagerListener() {

                public void chatCreated(Chat chat, boolean createdLocally) {
                    ChatWindow chatWindow;
                    if ((!createdLocally) && (!currentChats.contains(chat.getParticipant()))) {
                        currentChats.add(chat.getParticipant());
                        try {
                            chat.sendMessage("");
                            chatWindow = new ChatWindow(conn, chat);
                            chatWindow.setVisible(true);
                        } catch (XMPPException ex) {
                            Logger.getLogger(JabberChat.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });
        }
    }

    /**
     * Private method to add a Packet Listener to the JabberChat object
     * The Packet Listener listens for presence updates for unsubscribe packets
     * and removes the user from the roster
     * @see PacketListener
     * @see PacketTypeFilter
     */
    private void AddPacketListener() {
        packetListener = new PacketListener() {

            public void processPacket(Packet packet) {
                try {
                    Presence presence = (Presence) packet;
                    if (presence.getType().equals(Presence.Type.unsubscribe)) {
                        for (RosterEntry entry : roster.getEntries()) {
                            if (entry.getUser().equals(presence.getFrom())) roster.removeEntry(entry);
                        }
                    }
                } catch (XMPPException e) {
                }
            }

            ;
        };
        PacketTypeFilter filter = new PacketTypeFilter(Presence.class);
        this.chatConnection.addPacketListener(packetListener, filter);
    }
}
