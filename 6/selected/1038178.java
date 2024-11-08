package org.javver.xmpp.old;

import java.util.Collection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.apache.log4j.Logger;
import org.javver.xmpp.old.OldJavverProperties;

public class OldConnection implements ConnectionListener, Runnable {

    private XMPPConnection connection;

    private Roster roster;

    private final Logger logger = Logger.getLogger(OldConnection.class);

    public OldConnection() {
        logger.debug("CONNECTION CONSTRUCTOR");
        connection = new XMPPConnection(OldJavverProperties.HOST);
    }

    public void run() {
        if (!connection.isConnected()) {
            try {
                logger.debug("CONNECTING...");
                connection.connect();
                connection.addConnectionListener(this);
                connection.login(OldJavverProperties.USERNAME, OldJavverProperties.PASSWORD);
            } catch (XMPPException e) {
                e.printStackTrace();
            }
        } else {
            logger.debug("DISCONNECTING");
            connection.disconnect();
        }
    }

    public XMPPConnection getConnection() {
        return this.connection;
    }

    public void sendsth() {
        Message msg = new Message();
        msg.setTo("pablo@acmelabs.pl");
        msg.setSubject("subject");
        msg.setBody("HELLO WORLD from JAVVER ;)");
        msg.setType(Message.Type.chat);
        connection.sendPacket(msg);
    }

    public void getroster() {
        Roster rst = connection.getRoster();
        Collection<RosterEntry> list = rst.getEntries();
        for (RosterEntry re : list) {
            logger.info(re.getUser() + ": " + re.getStatus());
        }
    }

    public void connectionClosed() {
        logger.debug("CONNECTION CLOSED");
    }

    public void connectionClosedOnError(Exception arg0) {
        logger.debug("CONNECTION CLOSED ON ERROR: " + arg0.getMessage());
    }

    public void reconnectingIn(int arg0) {
        logger.debug("RECONNECTING IN: " + arg0);
    }

    public void reconnectionFailed(Exception arg0) {
        logger.debug("CONNECTION FAILED: " + arg0.getMessage());
    }

    public void reconnectionSuccessful() {
        logger.debug("RECONNECTING SUCCESSFUL");
    }
}
