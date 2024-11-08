package org.javver.xmpp;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.Roster;

public class XMPPManager {

    private static JavverConnection connection;

    private static Roster roster;

    public XMPPManager() {
    }

    public static void connectAndLogin() throws XMPPException {
        connection = new JavverConnection();
        connection.connect();
        connection.login();
    }
}
