package XMPP;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;

/**
 *
 * @author stylesuxx@gmail.com
 */
public class XMPPcon {

    private String server = "";

    private int port = 0;

    private ConnectionConfiguration config;

    private XMPPConnection conn;

    private MultiUserChat muc;

    /**
     * Writes a config file for the server to be used
     *
     * @param server Hostname to use
     * @param port Port to use
     */
    public XMPPcon(String server, int port) {
        this.server = server;
        this.port = port;
        config = new ConnectionConfiguration(server, port);
        config.setSASLAuthenticationEnabled(true);
        SASLAuthentication.supportSASLMechanism("PLAIN", 0);
    }

    /**
     * Connects to Server and logins with the given config file and details
     *
     * @param username Username
     * @param password Password
     * @param resource Resource
     * @return Returns a XMPP connection
     */
    public XMPPConnection connect(String username, String password, String resource) {
        conn = new XMPPConnection(config);
        try {
            conn.connect();
            conn.login(username, password, resource);
            System.out.println("Login to Jabber Server: " + server + ":" + port + " as " + username + " was successfull.");
        } catch (XMPPException e) {
            System.err.println("Login to Jabber Server: " + server + ":" + port + " as " + username + " failed.");
            System.exit(1);
        }
        return conn;
    }

    /**
     * Joins a MUC and returns it
     *
     * @param room Romm to join.
     * @param screenname Name to be used in the MUC.
     * @param welcome Welcome message to send.
     * @return Returns a MUC
     */
    public MultiUserChat joinMUC(String room, String screenname, String welcome) {
        try {
            muc = new MultiUserChat(conn, room);
            DiscussionHistory history = new DiscussionHistory();
            history.setMaxStanzas(0);
            muc.join(screenname, " ", history, 10000);
            ServerFlags SF = new ServerFlags();
            muc.sendConfigurationForm(SF.getRoomForm());
            muc.sendMessage(welcome);
            System.out.println("Joined room: " + room);
        } catch (XMPPException e) {
            System.err.println("Couldn`t join: " + room + " maybe that room already is owned by someone else.");
            System.exit(1);
        }
        return muc;
    }
}
