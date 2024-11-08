package de.fu_berlin.inf.dpp.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.picocontainer.annotations.Inject;
import de.fu_berlin.inf.dpp.SafeConnectionListener;
import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.net.UPnP.UPnPManager;
import de.fu_berlin.inf.dpp.net.util.NetworkingUtils;
import de.fu_berlin.inf.dpp.util.Utils;

/**
 * Class containing network layer components of Saros.
 */
public class SarosNet {

    private static final Logger log = Logger.getLogger(SarosNet.class);

    protected Connection connection;

    protected String loginUsername;

    protected String loginPassword;

    protected ConnectionConfiguration prevConnectionConfiguration;

    protected boolean enableDebug;

    protected int proxyPort;

    protected boolean proxyEnabled;

    protected String stunServer;

    protected int stunPort;

    protected boolean autoPortMapping;

    /**
     * The RQ-JID of the local user or null if the user is
     * {@link ConnectionState#NOT_CONNECTED}.
     */
    protected JID myJID;

    protected ConnectionState connectionState = ConnectionState.NOT_CONNECTED;

    protected Exception connectionError;

    protected final List<IConnectionListener> listeners = new CopyOnWriteArrayList<IConnectionListener>();

    protected ConnectionListener smackConnectionListener;

    /**
     * Configures this instance with debug, proxy, stun and UPnP settings.
     * 
     * @param enableDebug
     *            boolean, true to show Smack Debug Window upon XMPP connection
     * @param proxyEnabled
     *            boolean, true to enable Socks5Proxy
     * @param proxyPort
     *            int, sets the Socks5Proxy port
     * @param stunServer
     *            String, STUN server (address)
     * @param stunPort
     *            int, STUN server port
     * @param autoPortMapping
     *            boolean, true to enable UPnP port mapping
     */
    public void setSettings(boolean enableDebug, boolean proxyEnabled, int proxyPort, String stunServer, int stunPort, boolean autoPortMapping) {
        Connection.DEBUG_ENABLED = enableDebug;
        this.enableDebug = enableDebug;
        this.proxyPort = proxyPort;
        this.proxyEnabled = proxyEnabled;
        this.stunServer = stunServer;
        this.stunPort = stunPort;
        this.autoPortMapping = autoPortMapping;
        setBytestreamConnectionProperties();
    }

    /**
     * Performs one time initializations. (currently register a
     * ConnectionCreationListener only)
     */
    public void initialize() {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {

            public void connectionCreated(Connection connection2) {
                if (connection != connection2) {
                    return;
                }
                ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
                sdm.addFeature(Saros.NAMESPACE);
                setBytestreamConnectionProperties();
            }
        });
    }

    @Inject
    StunHelper stunHelper;

    @Inject
    UPnPManager upnpManager;

    /**
     * Configures Bytestream related settings, like PacketReplyTimeout,
     * Socks5Proxy configuration, look up streamhost address candiates for
     * Socks5Proxy, update UPnP settings on proxy setting change.
     */
    protected void setBytestreamConnectionProperties() {
        SmackConfiguration.setPacketReplyTimeout(30000);
        boolean settingsChanged = false;
        boolean portChanged = false;
        Socks5Proxy proxy = NetworkingUtils.getSocks5ProxySafe();
        if (proxyEnabled != SmackConfiguration.isLocalSocks5ProxyEnabled()) {
            settingsChanged = true;
            SmackConfiguration.setLocalSocks5ProxyEnabled(proxyEnabled);
        }
        if (proxyPort != SmackConfiguration.getLocalSocks5ProxyPort()) {
            settingsChanged = true;
            portChanged = true;
            SmackConfiguration.setLocalSocks5ProxyPort(proxyPort);
        }
        if (proxyEnabled) {
            try {
                List<InetAddress> myAdresses = NetworkingUtils.getAllNonLoopbackLocalIPAdresses(true);
                if (!myAdresses.isEmpty()) {
                    List<String> myAdressesStr = new LinkedList<String>();
                    for (InetAddress ip : myAdresses) myAdressesStr.add(ip.getHostAddress());
                    proxy.replaceLocalAddresses(myAdressesStr);
                }
                if (stunHelper != null && !stunHelper.isLocalIPthePublicIP()) stunHelper.startWANIPDetection(stunServer, stunPort, false);
            } catch (Exception e) {
                log.debug("Error while retrieving IP addresses", e);
            }
        }
        if (settingsChanged || proxy.isRunning() != proxyEnabled) {
            StringBuilder sb = new StringBuilder();
            if (settingsChanged) sb.append("Socks5Proxy properties changed. ");
            if (proxy.isRunning()) {
                proxy.stop();
                sb.append("Socks5Proxy stopped. ");
            }
            if (proxyEnabled) {
                proxy.start();
                sb.append("Socks5Proxy started on port " + proxy.getPort() + "...");
            }
            log.debug(sb);
            if (upnpManager != null) {
                if (proxyEnabled && portChanged && upnpManager.isMapped()) upnpManager.createSarosPortMapping();
                if (proxyEnabled && autoPortMapping && !upnpManager.isMapped()) upnpManager.createSarosPortMapping();
                if (!proxyEnabled && upnpManager.isMapped()) upnpManager.removeSarosPortMapping();
                if (upnpManager.isMapped()) {
                    String gatewayPublicIP = upnpManager.getPublicGatewayIP();
                    if (gatewayPublicIP != null) NetworkingUtils.addProxyAddress(gatewayPublicIP, true);
                }
            }
        }
    }

    /**
     * The RQ-JID of the local user if connected, null otherwise
     */
    public JID getMyJID() {
        return this.myJID;
    }

    /**
     * Returns the {@code org.jivesoftware.smack.Roster} of the established XMPP
     * connection. Or <code>null</code> if not connected.
     */
    public Roster getRoster() {
        if (!isConnected()) {
            return null;
        }
        return this.connection.getRoster();
    }

    /**
     * Connects using the given credentials.
     * 
     * @param connectionConfiguration
     *            {@link ConnectionConfiguration} Configuration for connecting
     *            to the server.
     * @param username
     *            XMPP account name to login
     * @param password
     *            password to login with
     * @praram failSilently true, if upon connection failure no feedback message
     *         is shown
     * 
     * @blocking
     */
    public void connect(ConnectionConfiguration connectionConfiguration, String username, String password, boolean failSilently) throws Exception {
        if (isConnected()) {
            disconnect();
        }
        prevConnectionConfiguration = connectionConfiguration;
        loginUsername = username;
        loginPassword = password;
        try {
            this.setConnectionState(ConnectionState.CONNECTING, null);
            this.connection = new XMPPConnection(connectionConfiguration);
            this.connection.connect();
            if (this.smackConnectionListener == null) {
                this.smackConnectionListener = new SafeConnectionListener(log, new XMPPConnectionListener());
            }
            connection.addConnectionListener(this.smackConnectionListener);
            Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);
            this.connection.login(username, password, Saros.RESOURCE);
            this.myJID = new JID(this.connection.getUser());
            setConnectionState(ConnectionState.CONNECTED, null);
        } catch (IllegalArgumentException e) {
            setConnectionState(ConnectionState.ERROR, null);
            throw (e);
        } catch (XMPPException e) {
            Throwable t = e.getWrappedThrowable();
            Exception cause = (t != null) ? (Exception) t : e;
            setConnectionState(ConnectionState.ERROR, cause);
            throw (e);
        } catch (Exception e) {
            setConnectionState(ConnectionState.ERROR, e);
            throw (e);
        }
    }

    /**
     * Disconnects (if currently connected)
     * 
     * @blocking
     * 
     * @post this.myjid == null && this.connection == null &&
     *       this.connectionState == ConnectionState.NOT_CONNECTED
     */
    public void disconnect() {
        if (isConnected()) {
            setConnectionState(ConnectionState.DISCONNECTING, null);
            disconnectInternal();
            setConnectionState(ConnectionState.NOT_CONNECTED, null);
        }
        this.myJID = null;
        if (this.connectionState != ConnectionState.NOT_CONNECTED) {
            log.warn("Connection state is out of sync");
            this.connectionState = ConnectionState.NOT_CONNECTED;
        }
        if (this.connection != null) {
            log.warn("Connection has not been closed");
            this.connection = null;
        }
    }

    protected void disconnectInternal() {
        if (connection != null) {
            try {
                connection.removeConnectionListener(smackConnectionListener);
                connection.disconnect();
            } catch (RuntimeException e) {
                log.warn("Could not disconnect old XMPPConnection: ", e);
            } finally {
                connection = null;
            }
        }
    }

    /**
     * Returns whether this instance is currently connected to the XMPP network.
     */
    public boolean isConnected() {
        return this.connectionState == ConnectionState.CONNECTED;
    }

    /**
     * @return the current state of the connection.
     */
    public ConnectionState getConnectionState() {
        return this.connectionState;
    }

    /**
     * @return Exception that occurred during recent connection failure or
     *         <code>null</code> if not applicable.
     */
    public Exception getConnectionError() {
        return this.connectionError;
    }

    /**
     * @return the currently established connection or <code>null</code> if
     *         there is none.
     */
    public Connection getConnection() {
        return this.connection;
    }

    public void addListener(IConnectionListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(IConnectionListener listener) {
        this.listeners.remove(listener);
    }

    protected void assertConnection() throws XMPPException {
        if (!isConnected()) {
            throw new XMPPException("No connection");
        }
    }

    /**
     * Sets a new connection state and notifies all connection listeners.
     */
    protected void setConnectionState(ConnectionState state, Exception error) {
        this.connectionState = state;
        this.connectionError = error;
        String prefix = "";
        if (connection != null) {
            String user = connection.getUser();
            if (user != null) prefix = Utils.prefix(new JID(user));
        }
        if (error == null) {
            log.debug(prefix + "New Connection State == " + state);
        } else {
            log.error(prefix + "New Connection State == " + state, error);
        }
        for (IConnectionListener listener : this.listeners) {
            try {
                listener.connectionStateChanged(this.connection, state);
            } catch (RuntimeException e) {
                log.error("Internal error in setConnectionState:", e);
            }
        }
    }

    protected class XMPPConnectionListener implements ConnectionListener {

        public void connectionClosed() {
            setConnectionState(ConnectionState.NOT_CONNECTED, null);
        }

        public void connectionClosedOnError(Exception e) {
            log.error("XMPP Connection Error: ", e);
            if (e.toString().equals("stream:error (conflict)")) {
                disconnect();
                setConnectionState(ConnectionState.NOT_CONNECTED, e);
                return;
            }
            if (getConnectionState() != ConnectionState.CONNECTED) return;
            setConnectionState(ConnectionState.ERROR, e);
            disconnectInternal();
            Utils.runSafeAsync(log, new Runnable() {

                public void run() {
                    int i = 0;
                    final int CONNECTION_RETRIES = 15;
                    while (!isConnected() && i++ < CONNECTION_RETRIES) {
                        try {
                            log.info("Reconnecting...(" + InetAddress.getLocalHost().toString() + ")");
                            reconnect(true);
                            if (!isConnected()) Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            log.error("Code not designed to be interruptable", e);
                            Thread.currentThread().interrupt();
                            return;
                        } catch (UnknownHostException e) {
                            log.info("Could not get localhost, maybe the network interface is down.");
                        } catch (XMPPException e) {
                            log.info("Login failed:" + e.getMessage());
                        } catch (Exception e) {
                            log.info("Login failed:" + e.getMessage());
                        }
                    }
                    if (isConnected()) {
                        log.debug("XMPP reconnected");
                    }
                }
            });
        }

        public void reconnectingIn(int seconds) {
            assert false : "Reconnecting is disabled";
        }

        public void reconnectionFailed(Exception e) {
            assert false : "Reconnecting is disabled";
        }

        public void reconnectionSuccessful() {
            assert false : "Reconnecting is disabled";
        }
    }

    private void reconnect(boolean failSilently) throws Exception {
        if (connection.isConnected()) return;
        connect(prevConnectionConfiguration, loginUsername, loginPassword, failSilently);
    }

    /**
     * Last resort of cleaning up, performing disconnect.
     */
    public void uninitialize() {
        if (isConnected()) {
            Utils.runSafeAsync(log, new Runnable() {

                public void run() {
                    disconnect();
                }
            });
        }
    }
}
