package jrox.util.xmpp;

import java.util.HashMap;
import java.util.Map;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

/**
 * Singleton to manage XMPPConnections
 * @author matthijs
 *
 */
public class XMPPConnectionManager {

    private static XMPPConnectionManager instance;

    private final Map<XMPPURI, XMPPConnection> managedConnections = new HashMap<XMPPURI, XMPPConnection>();

    private final Map<XMPPConnection, Integer> leases = new HashMap<XMPPConnection, Integer>();

    /**
	 * Not to be instantiated outside this class
	 */
    private XMPPConnectionManager() {
    }

    public XMPPConnection getXMPPConnection(final String uri) throws XMPPException {
        return getXMPPConnection(new XMPPURI(uri));
    }

    public XMPPConnection getXMPPConnection(final XMPPURI uri) throws XMPPException {
        final XMPPURI connectionUri = new XMPPURI();
        connectionUri.setHost(uri.getHost());
        connectionUri.setPort(uri.getPort());
        connectionUri.setUser(uri.getUser());
        connectionUri.setUserPassword(uri.getUserPassword());
        final XMPPConnection connection;
        if (managedConnections.containsKey(connectionUri)) {
            connection = managedConnections.get(connectionUri);
        } else {
            connection = new XMPPConnection(new ConnectionConfiguration(connectionUri.getHost(), connectionUri.getPort())) {

                @Override
                protected void shutdown(final Presence unavailablePresence) {
                    final int lease = leases.get(this);
                    if (lease > 1) {
                        leases.put(this, lease - 1);
                    } else {
                        leases.remove(this);
                        super.shutdown(unavailablePresence);
                    }
                }
            };
        }
        final int lease = leases.containsKey(connection.getConnectionID()) ? leases.get(connection.getConnectionID()) + 1 : 1;
        leases.put(connection, lease);
        if (!connection.isConnected()) {
            connection.connect();
        }
        if (!connection.isAuthenticated()) {
            if (connectionUri.getUser() != null) {
                connection.login(connectionUri.getUser(), connectionUri.getUserPassword(), "JSONRPC");
            } else {
                connection.loginAnonymously();
            }
        }
        return connection;
    }

    public static XMPPConnectionManager getInstance() {
        if (instance == null) {
            instance = new XMPPConnectionManager();
        }
        return instance;
    }
}
