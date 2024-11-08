package org.esme.kroak.server;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

/**
 * 
 * @author Julien Bouvet
 *
 */
public class IMServiceJabberImpl {

    static XMPPConnection connexion;

    Roster roster;

    ConnectionConfiguration config;

    AccountManager accountManager;

    /**
	 * Constructor of the IMServiceJaberImpl class 
	 *
	 */
    public IMServiceJabberImpl() {
    }

    public Roster getRoster() {
        return this.roster;
    }

    /**
	 * The method starts a XMPPconnection to a jabber server
	 * @param adress : JID of the user
	 * @param password : password for the user account
	 * @param status : Show if the connection has succeeded
	 * @return the XMPPconnexion
	 */
    public XMPPConnection connexion(String adress, String password, boolean status) {
        status = true;
        int arobas;
        String login, server;
        int port = 5222;
        arobas = adress.indexOf("@");
        if (arobas < 0) {
            status = false;
            return connexion;
        }
        login = adress.substring(0, arobas);
        server = adress.substring(arobas + 1);
        if (server == "ecole.fr") server = "127.0.0.1";
        config = new ConnectionConfiguration(server, port);
        connexion = new XMPPConnection(config);
        try {
            connexion.connect();
            connexion.login(login, password);
        } catch (XMPPException error) {
            status = false;
            connexion = null;
            return connexion;
        }
        Presence presence = new Presence(Presence.Type.available);
        connexion.sendPacket(presence);
        return connexion;
    }

    /**
	 * 
	 * @param login : JID the user wants to create
	 * @param password : password chosen by the user for his Jabber account
	 * @param test
	 * @return return the XMPP connection
	 * @throws XMPPException 
	 */
    public XMPPConnection createAccount(String login, String password, String server, int port) throws XMPPException {
        config = new ConnectionConfiguration(server, port);
        connexion = new XMPPConnection(config);
        connexion.connect();
        accountManager = new AccountManager(connexion);
        try {
            accountManager.createAccount(login, password);
        } catch (XMPPException error) {
            error.printStackTrace();
        }
        return connexion;
    }

    public XMPPConnection getAccountConnexion() {
        return connexion;
    }

    /**
	 * 
	 * @throws XMPPException
	 */
    void deleteAccount() throws XMPPException {
        accountManager.deleteAccount();
    }

    /**
	 * 
	 * @param newPassword
	 * @throws XMPPException
	 */
    void ChangePassword(String newPassword) throws XMPPException {
        accountManager.changePassword(newPassword);
    }

    /**
	 * 
	 *
	 */
    public static void deconnection() {
        connexion.disconnect();
    }
}
