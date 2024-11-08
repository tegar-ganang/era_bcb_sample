package de.goesberserk.xmpp.utils;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import javax.net.ssl.*;
import org.jivesoftware.spark.util.DummySSLSocketFactory;

/**
 *
 * @author till.klocke
 */
public class JConnection {

    private String username;

    private String password;

    private String ressource;

    private String server;

    private XMPPConnection connection;

    private Presence presence;

    private String statusMessage = "Jabber rocks!!!";

    private boolean ssl;

    public JConnection(String server, String username, String password) {
        this.server = server;
        this.username = username;
        this.password = password;
        this.ressource = this.generateRessource();
        this.presence = new Presence(Presence.Type.available, statusMessage, 1, Presence.Mode.dnd);
    }

    public JConnection(String server, String username, String password, String ressource) {
        this(server, username, password);
        this.ressource = ressource;
    }

    public JConnection(String server, String username, String password, String ressource, String statusMessage) {
        this(server, username, password, ressource);
        this.statusMessage = statusMessage;
        this.presence = new Presence(Presence.Type.available, statusMessage, 1, Presence.Mode.dnd);
    }

    public XMPPConnection connect() throws XMPPException {
        if (!this.ssl) {
            this.connection = new XMPPConnection(this.getServer());
            if (connection != null) {
                connection.connect();
                if (connection.isConnected()) {
                    connection.login(getUsername(), getPassword(), getRessource());
                    if (connection.isAuthenticated()) {
                        connection.sendPacket(presence);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return this.connectSLL(5223);
        }
        return connection;
    }

    public XMPPConnection connectSLL(int port) throws XMPPException {
        ConnectionConfiguration con = new ConnectionConfiguration(this.getServer(), port);
        con.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
        con.setSASLAuthenticationEnabled(true);
        con.setSocketFactory(new DummySSLSocketFactory());
        this.connection = new XMPPConnection(con);
        if (this.connection != null) {
            this.connection.connect();
            if (this.connection.isConnected()) {
                this.connection.login(getUsername(), getPassword(), getRessource());
                if (this.connection.isAuthenticated()) {
                    this.connection.sendPacket(presence);
                }
            }
        }
        return this.connection;
    }

    public void sendMessage(String recipient, String message) {
        Message packet = new Message();
        packet.setBody(message);
        packet.setFrom(this.getUsername() + "@" + this.getServer());
        packet.setTo(recipient);
        if (connection.isAuthenticated()) {
            connection.sendPacket(packet);
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isConnected() {
        return this.connection.isConnected();
    }

    public void disconnect() {
        Presence logout = new Presence(Presence.Type.unavailable);
        connection.disconnect(logout);
    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public Roster getRoster() {
        if (connection.isConnected() && connection.isAuthenticated()) {
            return connection.getRoster();
        }
        return null;
    }

    private String generateRessource() {
        String ressource = new String();
        for (int i = 0; i < 64; i++) {
            char part = (char) (Math.random() * 255);
            ressource = ressource + part;
        }
        return ressource;
    }

    public void setSSL(boolean ssl) {
        this.ssl = ssl;
    }

    public boolean isSSL() {
        return this.ssl;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the ressource
     */
    public String getRessource() {
        return ressource;
    }

    /**
     * @return the server
     */
    public String getServer() {
        return server;
    }
}
