package org.wejde.muel;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smack.XMPPException;

/**
 * This class contains the members and methods for sending
 * events to a XMPP-based server.
 * 
 * The methods include establishing the XMPP connection,
 * submitting the event to the XMPP server processing
 * incoming XMPP message packets and XMPP presence packets.
 * 
 * @author Eric Martin
 * @author Webb Pinner
 *
 */
public class xmppLogClient implements Runnable, MessageListener, ConnectionListener {

    boolean joinedOnce;

    /**
	 * Set the Debug Flag.
	 */
    private static final boolean DEBUG = true;

    /**
	 * The chatroomPane object to link the XMPP messages to
	 * for displaying.
	 */
    private chatroomPane chatroom;

    /**
	 * The rosterPane object for displaying the users
	 * participating in the chatroom.
	 */
    private rosterPane roster;

    /**
	 * The JID for the current user.
	 */
    private String user;

    /**
	 * The password for the current user.
	 */
    private String pass;

    /**
	 * The name of the chatroom.
	 */
    private String room;

    /**
	 * A thread object for forking the task of maintaining
	 * the connection to the chatroom.
	 */
    private Thread runner;

    /**
	 * The XMPPConnection object handles the connection
	 * with the XMPP server.
	 */
    private XMPPConnection connection;

    /**
	 * The name of the Multi-User Chatroom (MUC).
	 */
    private MultiUserChat muc;

    /**
	 * Constructor method.
	 * @param server XMPP server to connect to
	 * @param port Network port on the XMPP server to
	 * connect to.
	 */
    public xmppLogClient(String server, int port) {
        this.user = "";
        this.pass = "";
        this.room = "";
        this.chatroom = null;
        XMPPConnection.DEBUG_ENABLED = xmppLogClient.DEBUG;
        ConnectionConfiguration config = new ConnectionConfiguration(server, port);
        config.setSASLAuthenticationEnabled(false);
        this.connection = new XMPPConnection(config);
        this.runner = null;
        this.joinedOnce = false;
    }

    /**
	 * Constructor Method.
	 * 
	 * @param server XMPP server to connect to
	 * @param port Network port on the XMPP server to
	 * connect to.
	 * @param username Username to use when connecting to the
	 * XMPP server.
	 * @param password Password to use when connecting to the
	 * XMPP server.
	 * @param roomname Name of the MUC on the XMPP server
	 * to connect to.
	 */
    public xmppLogClient(String server, int port, String username, String password, String roomname) {
        this(server, port);
        this.user = username;
        this.pass = password;
        this.room = roomname;
    }

    /**
	 * Log into the XMPP server using the supplied username
	 * /password.
	 *  
	 * @param user User to log into the XMPP server with
	 * @param pass Password to log into the XMPP server
	 * with.
	 * 
	 * @return boolean flag designating login was a success
	 * (true) or failure (false).
	 */
    public boolean login(String user, String pass) {
        if (!connection.isConnected()) {
            System.err.print("Connecting to XMPP Server... ");
            try {
                connection.connect();
                System.err.println("Success!");
            } catch (XMPPException e) {
                System.err.println("FAIL: " + e.getMessage());
                return false;
            }
        }
        if (!connection.isAuthenticated()) {
            System.err.print("Logging into XMPP Server... ");
            try {
                connection.login(user, pass);
                System.err.println("Success!");
                this.user = user;
                this.pass = pass;
            } catch (XMPPException e) {
                System.err.println("FAIL: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    /**
	 * Log into the Multi-User Chatroom on the XMPP server
	 * using the supplied chatroom name
	 *  
	 * @param room name of the chatroom.
	 * 
	 * @return boolean flag designating login was a success
	 * (true) or failure (false).
	 */
    public boolean joinMUC(String room) {
        System.err.print("Joining chatroom: " + room + "@conference." + this.connection.getHost() + "... ");
        this.muc = new MultiUserChat(this.connection, room + "@conference." + this.connection.getHost());
        DiscussionHistory history = new DiscussionHistory();
        if (this.joinedOnce) {
            history.setMaxStanzas(0);
        } else {
            history.setMaxStanzas(5);
        }
        try {
            this.muc.join(this.user, null, history, SmackConfiguration.getPacketReplyTimeout());
            this.muc.addMessageListener(this.chatroom);
            this.muc.addParticipantListener(this.roster);
            this.room = room;
            System.err.println("Success!");
            if (this.runner == null) {
                this.runner = new Thread(this);
            }
            if (!this.runner.isAlive()) {
                this.runner.start();
            }
        } catch (XMPPException e) {
            System.err.println("FAIL! " + e);
            return false;
        }
        this.joinedOnce = true;
        return true;
    }

    /**
	 * Submit event messages to the server.
	 * 
	 * @param msg the string message to submit.
	 */
    public boolean submitEvent(String msg) {
        if (this.connection.isAuthenticated() && muc.isJoined()) {
            try {
                this.muc.sendMessage(msg);
                return true;
            } catch (XMPPException e) {
                System.err.println("ERROR: " + e);
                this.chatroom.addEventMsg("ERROR: Problem Communicating with server!");
                return false;
            }
        } else {
            System.err.println("ERROR: Not connected to Chatroom!");
            this.chatroom.addEventMsg("ERROR: Not connected to Chatroom!");
            return false;
        }
    }

    /**
	 * Status flag to determine if conenction with the XMPP
	 * server is active.
	 * 
	 * @return boolean flag designating that the connection
	 * is active (true) or disconnected (false).
	 */
    public boolean isXMPPLoggedIn() {
        return connection.isAuthenticated();
    }

    /**
	 * Status flag to determine if conenction with the XMPP
	 * server is active.
	 * 
	 * @return boolean flag designating that the connection
	 * is active (true) or disconnected (false).
	 */
    public boolean isXMPPConnected() {
        return connection.isConnected();
    }

    /**
	 * Set the reference to the chatroomPane.
	 * 
	 * @param chatroomPane the chatroomPane object.
	 */
    public void setChatroomPane(chatroomPane chatroomPane) {
        this.chatroom = chatroomPane;
    }

    /**
	 * Set the reference to the rosterPane.
	 * 
	 * @param rosterPane the rosterPane object.
	 */
    public void setRosterPane(rosterPane rosterPane) {
        this.roster = rosterPane;
    }

    /**
	 * Disconnect from the XMPP server and if necessary the
	 * chatroom.
	 */
    public void disconnect() {
        if (this.muc != null) {
            this.runner = null;
            System.err.print("Leaving Chatroom... ");
            this.muc.leave();
            System.err.println("Success!");
        }
        System.err.print("Disconnecting from XMPP Server... ");
        this.connection.disconnect();
        System.err.println("Success!");
    }

    /**
	 * De-Constructor method.
	 */
    public void destroy() {
        disconnect();
    }

    /**
	 * Get the muc object.  Used when linking the
	 * rosterPane and chatroomPane to this XMPPLogClient.
	 * 
	 * @return the Multi-User Chatroom object.
	 */
    public MultiUserChat getMUC() {
        return this.muc;
    }

    /**
	 * Require by the MessageListener interface.
	 */
    @Override
    public void processMessage(Chat chat, Message msg) {
        if (msg.getType() == Message.Type.chat) {
        }
    }

    /**
	 * If the client become disconnected from the XMPP
	 * server, this method tries to reconnect to the server
	 * once a second until the connection is re-
	 * established.
	 * 
	 * Require by the Runnable interface.
	 */
    @Override
    public void run() {
        Thread thisThread = Thread.currentThread();
        boolean sendError = true;
        while (this.runner == thisThread) {
            if (!this.connection.isConnected()) {
                System.err.println("Error: Lost connection from server!");
                if (sendError) {
                    this.chatroom.addEventMsg("Error: Lost connection from server!\n");
                    sendError = false;
                }
                System.err.print("Attempting to reconnect... ");
                try {
                    this.connection.connect();
                    if (this.connection.isConnected()) {
                        System.err.println("Success!");
                        this.chatroom.addEventMsg("Connection to server restored!\n");
                        sendError = true;
                        this.joinMUC(this.room);
                    }
                } catch (XMPPException e) {
                    System.err.println("Fail!  Will try again in 1 second.");
                }
            } else if (!this.muc.isJoined()) {
                System.err.println("Error: Lost connection with Chatroom!");
                if (sendError) {
                    this.chatroom.addEventMsg("Error: Lost connection to Chatroom!\n");
                    sendError = false;
                }
                System.err.print("Attempting to reconnect... ");
                if (this.joinMUC(this.room)) {
                    System.err.println("Success!");
                    this.chatroom.addEventMsg("Connection to chatroom restored!\n");
                    sendError = true;
                } else {
                    System.err.println("Fail!  Will try again in 1 second.");
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void connectionClosed() {
        System.err.println("Connection Lost");
    }

    @Override
    public void connectionClosedOnError(Exception arg0) {
        System.err.println("Connection Lost On Error");
    }

    @Override
    public void reconnectingIn(int arg0) {
    }

    @Override
    public void reconnectionFailed(Exception arg0) {
        System.err.println("Reconnect Failed");
    }

    @Override
    public void reconnectionSuccessful() {
    }
}
