package de.tudresden.inf.rn.mobilis.server.locpairs.model;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

/**
 * The Class Connection.
 */
public class Connection {

    private XMPPConnection connection = null;

    private String username = "server";

    private String password = "7Dj3S";

    private String host = "141.30.203.90";

    private String resource = "Smack";

    private String jid = null;

    private Game game = null;

    public Connection(Game game, String host, String username, String password) {
        this.game = game;
        this.host = host;
        this.username = username;
        this.password = password;
        XMPPConnection.DEBUG_ENABLED = true;
        resource = createResource();
        jid = username + "@" + host + "/" + resource;
        System.out.println(jid);
        connect();
    }

    /**
	 * Connect.
	 *
	 * @return true, if successful
	 */
    public boolean connect() {
        connection = new XMPPConnection(host);
        try {
            connection.connect();
        } catch (XMPPException e) {
            e.printStackTrace();
            return false;
        }
        try {
            connection.login(username, password, resource);
        } catch (XMPPException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String createResource() {
        String resource1 = "Smack";
        return resource1;
    }

    /**
	 * Gets the user name.
	 *
	 * @return the user name
	 */
    public String getUsername() {
        return username;
    }

    /**
	 * Gets the password.
	 *
	 * @return the password
	 */
    public String getPassword() {
        return password;
    }

    /**
	 * Gets the host.
	 *
	 * @return the host
	 */
    public String getHost() {
        return host;
    }

    /**
	 * Gets the resource.
	 *
	 * @return the resource
	 */
    public String getResource() {
        return resource;
    }

    /**
	 * Gets the jid.
	 *
	 * @return the jid
	 */
    public String getJid() {
        return jid;
    }

    /**
	 * Gets the connection.
	 *
	 * @return the connection
	 */
    public XMPPConnection getConnection() {
        return connection;
    }
}
