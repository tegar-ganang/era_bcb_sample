package org.tcpfile.xmpp;

import java.util.Collection;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMPPAccount {

    private static Logger log = LoggerFactory.getLogger(XMPPAccount.class);

    private String server;

    private String username;

    private String password;

    private boolean savePassword;

    private transient XMPPConnection con;

    public XMPPAccount(String server, String username, String password) {
        super();
        this.server = server;
        this.password = password;
        this.username = username;
        savePassword = true;
    }

    public XMPPConnection getConnection() {
        if (con != null && con.isConnected()) return con;
        con = new XMPPConnection(server);
        try {
            con.connect();
            con.login(username, password);
        } catch (XMPPException e) {
            log.warn("", e);
        }
        con.getRoster().addRosterListener(new RosterListener() {

            public void presenceChanged(Presence arg0) {
                log.info("{}", arg0);
            }

            public void entriesAdded(Collection<String> arg0) {
            }

            public void entriesDeleted(Collection<String> arg0) {
            }

            public void entriesUpdated(Collection<String> arg0) {
            }
        });
        return con;
    }
}
