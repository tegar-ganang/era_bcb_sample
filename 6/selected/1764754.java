package com.jiaho.xmpp.core.connection;

import com.jiaho.xmpp.core.util.log.JiahoLog;
import com.jiaho.xmpp.core.util.messages.JiahoGeneralProperties;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;

/**
 * JiahoConnection is the XMPP Connection Management Class.
 *
 * @author Manuel Martins
 */
public abstract class JiahoConnection implements ConnectionListener {

    /**
     * Private Global Variables
     */
    private XMPPConnection connection;

    private ConnectionConfiguration config;

    private String username;

    private String password;

    private String host;

    private static final String serviceName = "jiaho";

    /**
     * Constructs a new <tt>Connection</tt> with the specified arguments.
     *
     * @param host       the host server to connect
     * @param port       the port accepting connections from the host server
     * @param username   the username name
     * @param password   the username password
     * @param newAccount <code>true</code> if is new account
     */
    public JiahoConnection(final String host, final int port, final String username, final String password, final boolean newAccount) {
        try {
            this.username = username;
            this.password = password;
            this.host = host;
            this.config = new ConnectionConfiguration(host, port);
            this.connection = new XMPPConnection(this.config);
            this.connection.connect();
            SASLAuthentication.supportSASLMechanism("PLAIN", 0);
            this.config.setSASLAuthenticationEnabled(true);
            if (newAccount) {
                this.connection.getAccountManager().createAccount(username, password);
                this.connection.login(username, password, serviceName);
                this.connection.addConnectionListener(this);
            } else {
                this.connection.login(username, password, serviceName);
                this.connection.addConnectionListener(this);
            }
            JiahoLog.Info(JiahoConnection.class, this.connection.getUser() + " : " + JiahoGeneralProperties.InfoLogin);
        } catch (final XMPPException e) {
            JiahoLog.Warn(JiahoConnection.class, this.username + "@" + this.host + " : " + JiahoGeneralProperties.WarnLogin, e);
        }
    }

    /**
     * Constructs a new <tt>Connection</tt> with the specified arguments.
     * Connects to the server Anonymously
     *
     * @param host the host server to connect
     * @param port the port accepting connections from the host server
     */
    public JiahoConnection(final String host, final int port) {
        try {
            this.config = new ConnectionConfiguration(host, port);
            this.connection = new XMPPConnection(this.config);
            this.connection.connect();
            this.connection.loginAnonymously();
            this.connection.addConnectionListener(this);
            JiahoLog.Info(JiahoConnection.class, this.connection.getUser() + " : " + JiahoGeneralProperties.InfoLogin);
        } catch (final XMPPException e) {
            JiahoLog.Warn(JiahoConnection.class, "AnonymousLogin : " + JiahoGeneralProperties.WarnLogin, e);
        }
    }

    /**
     * Changes the password of connected username.
     *
     * @param oldPassword the old password
     * @param newPassword the new password
     * @return <tt>boolean</tt>success
     */
    public boolean changePassword(final String oldPassword, final String newPassword) {
        boolean success = false;
        if (oldPassword != null && newPassword != null && !oldPassword.isEmpty() && !newPassword.isEmpty()) {
            try {
                if (this.password.equals(oldPassword) && !this.password.equals(newPassword)) {
                    this.connection.getAccountManager().changePassword(newPassword);
                    this.password = newPassword;
                    success = true;
                    JiahoLog.Info(JiahoConnection.class, this.connection.getUser() + " : " + JiahoGeneralProperties.InfoPasswordChange);
                }
            } catch (final XMPPException e) {
                JiahoLog.Warn(JiahoConnection.class, this.connection.getUser() + " : " + JiahoGeneralProperties.WarnPasswordChange, e);
            }
        }
        return success;
    }

    /**
     * Returns the username name of the username connected.
     *
     * @return <tt>username</tt>the username name
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Returns the host where the username is connected.
     *
     * @return <tt>host</tt>the host server
     */
    public String getHostServer() {
        return this.host;
    }

    /**
     * Returns true if username is authenticated.
     *
     * @return <tt>boolean</tt>authenticated
     */
    public boolean isAuthenticated() {
        return this.connection.isAuthenticated();
    }

    /**
     * Returns true if username is connected with server.
     *
     * @return <tt>boolean</tt>connected
     */
    public boolean isConnected() {
        return this.connection.isConnected();
    }

    /**
     * Returns the Jabber Identification (JID) from the username.
     *
     * @return <tt>jid</tt>the JabberID
     */
    public String getJID() {
        return StringUtils.parseBareAddress(this.connection.getUser());
    }

    /**
     * Disconnects from host server.
     */
    public void disconnect() {
        final String userLogout = this.connection.getUser();
        try {
            this.connection.disconnect();
            JiahoLog.Info(JiahoConnection.class, userLogout + " : " + JiahoGeneralProperties.InfoLogout);
        } catch (Exception e) {
            JiahoLog.Warn(JiahoConnection.class, userLogout + " : " + JiahoGeneralProperties.WarnLogout, e);
        }
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
            JiahoLog.Error(JiahoConnection.class, this.connection.getUser() + " : " + JiahoGeneralProperties.ErrorConnectionDoesNotExist, e);
        }
        return con;
    }

    /**
     * If the connection was closed this method is invoked. (smack -
     * ConnectionListener)
     */
    @Override
    public void connectionClosed() {
    }

    /**
     * If the connection was closed on Error this method is invoked. (smack -
     * ConnectionListener)
     *
     * @param e the Exception
     */
    @Override
    public void connectionClosedOnError(Exception e) {
    }

    /**
     * Reconnecting in x seconds. (smack - ConnectionListener)
     *
     * @param seconds the time in seconds
     */
    @Override
    public void reconnectingIn(int seconds) {
    }

    /**
     * Reconnection Successful.(smack - ConnectionListener)
     */
    @Override
    public void reconnectionSuccessful() {
    }

    /**
     * if reconnection fails this method is invoked. (smack -
     * ConnectionListener)
     *
     * @param e the Exception
     */
    @Override
    public void reconnectionFailed(Exception e) {
    }
}
