package jxmpp.com.code.google.core.connectivity;

import com.google.inject.Inject;
import jxmpp.com.code.google.core.configuration.Configuration;
import jxmpp.com.code.google.core.listeners.CommonPacketListener;
import jxmpp.com.code.google.core.listeners.ConnectionStateListener;
import jxmpp.com.code.google.core.listeners.PresencePacketListener;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

/**
 * Created by IntelliJ IDEA.
 * User: ternovykh
 * Date: 28.07.11
 * Time: 14:39
 */
public class ConnectionDirector {

    private static final Logger log = Logger.getLogger(ConnectionDirector.class.getName());

    @Inject
    private ConnectionDirector(Configuration configuration, ConnectionStateListener connectionListener, CommonPacketListener listener, PresencePacketListener presenceListener) {
        this.configuration = configuration;
        this.connectionListener = connectionListener;
        this.listener = listener;
        this.presenceListener = presenceListener;
        connection = initConnection();
    }

    public void run() throws XMPPException {
        log.info("Establishing connection to server");
        connection.connect();
        log.info("Connected to server");
        connection.addConnectionListener(connectionListener);
        connection.addPacketListener(listener, new PacketTypeFilter(Message.class));
        connection.addPacketListener(presenceListener, new PacketTypeFilter(Presence.class));
        log.info("Performing authentication");
        connection.login(configuration.getUserName(), configuration.getPassword());
        log.info("Authenticated to server");
    }

    public XMPPConnection getConnection() {
        return connection;
    }

    private XMPPConnection initConnection() {
        ConnectionConfiguration connConf = new ConnectionConfiguration(configuration.getHost(), configuration.getPort());
        connConf.setSASLAuthenticationEnabled(true);
        connection = new XMPPConnection(connConf);
        return connection;
    }

    private Configuration configuration;

    private XMPPConnection connection;

    private CommonPacketListener listener;

    private PresencePacketListener presenceListener;

    private ConnectionStateListener connectionListener;
}
