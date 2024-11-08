package mobi.ilabs.authentication;

import mobi.ilabs.EILog;
import org.apache.commons.logging.Log;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

/**
 * Authenticate users the XMPP protocol. Our method is to create an XMPP session
 * on behalf of the user. If we can do that with the credentials provided by the
 * user, we assume that the user has a valid account at the XMPP domain we're
 * consulting.
 */
public class AuthenticatorForXMPP implements RawAuthenticator {

    /**
     * Fully qualified hostname for the XMPP server being used
     * for authentication.
     */
    private String servername;

    /**
     * Get name of server being served by this
     * class instance.
     * @return the servername
     */
    public final String getServername() {
        return servername;
    }

    /**
     * A logger for events originating in this class.
     */
    private static final Log LOG = EILog.getLog(AuthenticatorForXMPP.class);

    /**
     * Create an authenticator using XMPP.
     * @param srv The XMPP server being used for authentication.
     */
    public AuthenticatorForXMPP(final String srv) {
        super();
        this.servername = srv;
    }

    /**
     * The TCP port number used by XMPP by default.
     */
    static final int XMPP_PORT_NUMBER = 5222;

    /**
     * Raw authentication for XMPP. The user ID. This may
     * be a simple username, or it   may be a composite
     * string string with syntax  username@domain.  In
     * the first case the username is assumed to be
     * validated directly by the  underlying XMPP server,
     * in the latter case it is assumed that the XMPP
     * serve can handle multiple domains, and the username
     * and domain is split and added as parts of a
     *  ConnectionConfiguration objects before being
     *  submitted to the XMPP server for actual
     *  authentication.
     * @param inputUserId - the user id string (simple or composite)
     * @param password - the password being checked.
     * @return the authentication result
     */
    public final AuthenticationResult rawAuthenticate(final String inputUserId, final String password) {
        boolean isAuthenticated = false;
        boolean unknown = true;
        try {
            String username;
            XMPPConnection connection;
            String[] parsedUsername = inputUserId.split("@", 2);
            if (parsedUsername.length > 1) {
                String userdomain = parsedUsername[1];
                username = parsedUsername[0];
                ConnectionConfiguration configuration = new ConnectionConfiguration(servername, XMPP_PORT_NUMBER, userdomain);
                connection = new XMPPConnection(configuration);
            } else {
                connection = new XMPPConnection(servername);
                username = inputUserId;
            }
            connection.connect();
            connection.login(username, password);
            isAuthenticated = connection.isAuthenticated();
            unknown = false;
            connection.disconnect();
        } catch (XMPPException e) {
            LOG.error("Authentication caught an exception: " + e);
            String exceptionString = e.toString();
            if (exceptionString.contains("authentication failed")) {
                unknown = false;
                isAuthenticated = false;
            }
        }
        AuthenticationResult result;
        if (unknown) {
            result = AuthenticationResult.UNKNOWN;
        } else if (isAuthenticated) {
            result = AuthenticationResult.AUTHENTICATED;
        } else {
            result = AuthenticationResult.REJECTED;
        }
        return result;
    }
}
