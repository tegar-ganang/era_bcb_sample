package jfbchat;

import jfbchat.facebook.JFBClient;
import jfbchat.debug.DMessage;
import jfbchat.debug.DebugMessage;
import jfbchat.debug.Error;
import jfbchat.frames.ChatFrame;
import jfbchat.listeners.MyRosterListener;
import jfbchat.listeners.PacketListening;
import jfbchat.resources.FBConnectionConfiguration;
import jfbchat.resources.Options;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

/**
 * This class represent a connection with the server
 *
 * @author Digitex (Giuseppe Federico - digitex3d@gmail.com)
 */
public class Connection {

    private User user;

    private Presence presence;

    private XMPPConnection connection;

    private ContactList contactList;

    private MyChatManager myChatManager;

    private PacketListening packetListening;

    private MyVCard vCard;

    private ChatFrame chatFrame;

    public Connection(User user) {
        this.user = user;
        this.contactList = new ContactList(this);
        this.myChatManager = new MyChatManager();
        this.connection = new XMPPConnection(new FBConnectionConfiguration(Options.SERVER, Options.PORT, Options.SERVICE_NAME));
        chatFrame = new ChatFrame(this);
        vCard = null;
    }

    /**
     * Connect to the server , login , set the presence avaiable, get the
     * contact list and start the packege listening.
     */
    public void connect() {
        new DMessage("Connection to " + Options.SERVER + " as " + user.getUsername() + "...");
        try {
            connection.connect();
            System.out.println("Connected to " + connection.getHost());
            connection.login(user.getUsername(), user.getPassword());
            System.out.println("Logged in as " + user.getUsername() + ".");
            updatePresence(new Presence(Presence.Type.available));
            contactList.getList();
            contactList.defineGroups();
            startPacketListening();
        } catch (XMPPException ex) {
            new Error(this, 1, "Wrong Username or Password");
        }
        vCard = new MyVCard(this);
    }

    /**
     * Listen for incoming packets and roster
     */
    public void startPacketListening() {
        try {
            packetListening = new PacketListening(this);
            contactList.getRoster().addRosterListener(new MyRosterListener(this));
        } catch (Exception e) {
            new Error(2, e.getMessage());
        }
    }

    /**
     * Update the presence to the server
     */
    public void updatePresence(Presence presence) {
        try {
            this.presence = presence;
            connection.sendPacket(this.presence);
            System.out.println("Presence is now " + this.presence.toString() + ".");
        } catch (Exception e) {
            new DebugMessage("Cannot update presence " + e.toString());
        }
    }

    /**
     * Should be called after a presence change
     */
    public void updatePresence() {
        try {
            connection.sendPacket(presence);
            System.out.println("Presence is now " + presence.toString() + ".");
        } catch (Exception e) {
            new DebugMessage("Cannot update presence " + e.toString());
        }
    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public Presence getPresence() {
        return presence;
    }

    /**
     * Close the connection with the server
     */
    public void closeConnection() {
        if (connection.isConnected()) {
            try {
                this.myChatManager.clear();
                connection.disconnect(new Presence(Presence.Type.unavailable));
                connection.disconnect();
            } catch (Exception e) {
                new DebugMessage("Cannot close connection: " + e.toString());
            }
        } else {
            new DebugMessage("Cannot close connection, the client is disconnected...");
        }
    }

    public MyChatManager getChatManager() {
        return myChatManager;
    }

    public ContactList getContactList() {
        return contactList;
    }

    public User getUser() {
        return this.user;
    }

    public MyVCard getVCard() {
        return this.vCard;
    }

    public String getJID() {
        return this.connection.getUser().replaceAll("/.*", "");
    }

    /**
     * 
     * @return the ChatFrame
     */
    public ChatFrame getChatFrame() {
        return this.chatFrame;
    }

    public void setContactList(ContactList list) {
        contactList = list;
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public String toString() {
        String msg = "";
        if (isConnected()) {
            msg += "Connected to " + Options.SERVER + " at port " + Options.PORT + " Status is " + presence.getStatus() + ".";
        } else {
            msg += "Not connected.";
        }
        return msg;
    }
}
