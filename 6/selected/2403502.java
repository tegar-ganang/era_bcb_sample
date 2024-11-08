package org.decody.jabber;

import java.util.Vector;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

/**
 * A basic jabber client class.
 */
public class JabberClient extends Thread {

    /** time to sleep the thread when a new message is sent (ms) */
    private static final int SLEEP_TIME = 500;

    /** jabber username */
    private String jabberUser;

    /** jabber password */
    private String jabberPass;

    /** jabber server */
    private String jabberServer;

    /** jabber port */
    private int jabberPort;

    /** connection status */
    private boolean connectionStatus;

    /** jabber connection */
    private XMPPConnection jabberConnection;

    /** chat session */
    private Chat jabberChat;

    /** messages queue */
    private Vector<String> messages;

    /**
	 * Create basic client class.
	 */
    public JabberClient() {
        jabberUser = null;
        jabberPass = null;
        jabberServer = null;
        jabberPort = 0;
        connectionStatus = false;
        messages = new Vector<String>();
    }

    /**
	 * Connect to a jabber server.
	 */
    public void connect() {
        if (jabberUser == null || jabberPass == null || jabberServer == null || jabberPort == 0) {
            System.out.println("You must to specify connection settings.");
            return;
        }
        try {
            ConnectionConfiguration c = new ConnectionConfiguration(jabberServer);
            c.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
            if (isConnected()) this.close();
            jabberConnection = new XMPPConnection(c);
            jabberConnection.connect();
            jabberConnection.login(jabberUser, jabberPass);
            connectionStatus = true;
        } catch (XMPPException e) {
            System.out.println("Connect exception: " + e.toString());
        }
    }

    /**
	 * Close the jabber connection.
	 */
    public void close() {
        if (isConnected()) {
            jabberConnection.disconnect();
            connectionStatus = false;
        }
    }

    /**
	 * Create a chat for talk with other user.
	 * @param user Remote user.
	 * @param server Server the remote user is connected.
	 */
    public void createChat(String user, String server) {
        if (isConnected()) {
            jabberChat = jabberConnection.getChatManager().createChat(user + "@" + server, new JabberMessageListener());
        }
    }

    /**
	 * Get the connection status
	 * @return Current connection status
	 */
    public boolean isConnected() {
        return connectionStatus;
    }

    /**
	 * Main jabber client loop
	 */
    public void run() {
        while (true) {
            synchronized (messages) {
                if (messages.size() > 0) {
                    sendMessage(messages.get(0));
                    messages.remove(0);
                }
            }
            try {
                sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                System.out.println("sleep interrupted!!!");
            }
        }
    }

    /**
	 * Put a new message in the queue
	 * @param message New message
	 */
    public void newMessage(String message) {
        synchronized (messages) {
            messages.add(message);
        }
    }

    /**
	 * Set the connection parameters.
	 * @param user User for jabber connection 
	 * @param pass Password for jabber connection 
	 * @param server Server to connect to
	 * @param port Port use in the connection
	 */
    public void setConnectionSettings(String user, String pass, String server, int port) {
        this.jabberUser = user;
        this.jabberPass = pass;
        this.jabberServer = server;
        this.jabberPort = port;
    }

    /**
	 * Send a message via jabber.
	 * @param message The message will be sent.
	 * @return If the message was sent ocrrectly.
	 */
    private boolean sendMessage(String message) {
        if (isConnected()) {
            Message jabber_message = new Message();
            jabber_message.setType(Message.Type.chat);
            jabber_message.addBody("en", message);
            try {
                jabberChat.sendMessage(jabber_message);
            } catch (XMPPException e) {
                close();
                return false;
            }
            return true;
        }
        return false;
    }
}
