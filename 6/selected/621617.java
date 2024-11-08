package de.tudresden.inf.rn.mobilis.server.xhunt;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import de.tudresden.inf.rn.mobilis.server.xhunt.services.IQService;
import de.tudresden.inf.rn.mobilis.server.xhunt.services.MessageService;

/**
 * Creates the connection to the XMPP-Server and activates the packetlistener
 * @author Daniel Esser
 *
 */
public class Connection {

    private XMPPConnection xmppCon;

    private XHuntController control;

    /**
	 * Connection
	 * @param control XHuntController, who administrates the whole life cycle
	 */
    public Connection(XHuntController control) {
        this.control = control;
    }

    /**
	 * Organizes the connection procedure
	 * @throws XMPPException
	 */
    public void startConnection() throws XMPPException {
        xmppCon.DEBUG_ENABLED = true;
        try {
            connect();
            activatePacketListener();
        } catch (XMPPException e) {
            throw e;
        }
    }

    /**
	 * Organizes the disconnection
	 */
    public void stopConnection() {
        disconnect();
    }

    /**
	 * Activates the packetlisteners for messages and IQs
	 */
    private void activatePacketListener() {
        MessageService mesServ = new MessageService(control);
        PacketTypeFilter mesFil = new PacketTypeFilter(Message.class);
        xmppCon.addPacketListener(mesServ, mesFil);
        IQService iqServ = new IQService(control);
        PacketTypeFilter locFil = new PacketTypeFilter(IQ.class);
        xmppCon.addPacketListener(iqServ, locFil);
    }

    /**
	 * Connects to XMPP-Server
	 * @throws XMPPException
	 */
    private void connect() throws XMPPException {
        ConnectionConfiguration conf = new ConnectionConfiguration(Settings.getInstance().getServer(), Settings.getInstance().getPort());
        xmppCon = new XMPPConnection(conf);
        try {
            xmppCon.connect();
            xmppCon.login(Settings.getInstance().getLogin(), Settings.getInstance().getPassword());
            control.getMainFrame().addStatusMessage("Connected to XMPP-Server " + xmppCon.getHost() + ":" + xmppCon.getPort());
            for (RosterEntry re : xmppCon.getRoster().getEntries()) {
                System.out.println("Name: " + re.getName() + " User: " + re.getUser());
            }
        } catch (XMPPException e) {
            xmppCon = null;
            throw e;
        }
    }

    /**
	 * Disconnects from XMPP-Server
	 */
    private void disconnect() {
        if (xmppCon != null) {
            xmppCon.disconnect();
        }
    }

    /**
	 * Return the XMPP-Connection
	 * @return
	 */
    public XMPPConnection getXmppCon() {
        return xmppCon;
    }
}
