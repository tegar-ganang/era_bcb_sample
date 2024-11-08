package org.rascalli.framework.jabber;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

/**
 * Utility class for registering XMPP/Jabber accounts for Rascalli agents.
 * 
 * @author gregor.siebe@ofai.at
 */
public class JabberAccountManager {

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Register a new account on a XMPP/Jabber server that allow in-band
     * registration.
     * 
     * @param jabberServer
     *            the jabber server hostname
     * @param jabberId
     *            the jabber ID to be created
     * @param jabberPassword
     *            the password to be set
     * @return true if the account was created, false otherwise
     */
    public boolean registerAccount(String jabberServer, String jabberId, String jabberPassword) {
        XMPPConnection connection = new XMPPConnection(jabberServer);
        try {
            connection.connect();
            AccountManager acm = connection.getAccountManager();
            if (acm.supportsAccountCreation()) {
                acm.createAccount(jabberId, jabberPassword);
                return true;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("Jabber Server " + jabberServer + " does not allow account registration");
                }
                return false;
            }
        } catch (XMPPException e) {
            if (log.isDebugEnabled()) {
                log.debug(e);
            }
            return false;
        } finally {
            if (connection.isConnected()) {
                connection.disconnect();
            }
        }
    }

    public void deletAccount(String jabberServer, String jabberId, String jabberPassword) throws XMPPException {
        XMPPConnection connection = new XMPPConnection(jabberServer);
        connection.connect();
        connection.login(jabberId, jabberPassword);
        AccountManager acm = connection.getAccountManager();
        acm.deleteAccount();
    }

    public static void main(String[] args) {
        try {
            JabberAccountManager accountManager = new JabberAccountManager();
            final String server = "www2.ofai.at";
            final String id = "rascalli-foobar-1";
            final String pwd = "foobar";
            accountManager.registerAccount(server, id, pwd);
            XMPPConnection connection = new XMPPConnection(server);
            connection.connect();
            connection.login(id, pwd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
