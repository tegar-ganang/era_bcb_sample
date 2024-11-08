package linker.account;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

/**
 * The connection class supports your login, disconnecting and getting
 * XMPPConnection
 * 
 * This class is to manage the connecting process, which includes login,
 * disconnecting and getting the original XMPPConnection.
 * 
 * @version 2008-05-16
 * @author Jianfeng tujf.cn@gmail.com
 * @author AwakenRz awakenrz@gmail.com
 * 
 */
public final class Connect {

    /**
	 * The default constructor.
	 */
    private Connect() {
    }

    /**
	 * The XMPPConnection.
	 */
    private static XMPPConnection connection;

    /**
	 * The flag record whether the connect procedure is canceled or not.
	 */
    private static boolean canceled = false;

    /**
	 * The port used for xmpp connect.
	 */
    private static final int XMPPPORT = 5222;

    /**
	 * @return The used XMPPConnection.
	 */
    public static XMPPConnection getConnecdtion() {
        return connection;
    }

    /**
	 * The login progress.
	 * 
	 * @param progress
	 *            The LoginProgress refer
	 * @param username
	 *            User name.
	 * @param password
	 *            password.
	 * @param host
	 *            The host of the server.
	 * @param portStr
	 *            The string of the port number.
	 * @param domain
	 *            The domain of the server.
	 * @return Whether connection success or not.
	 */
    public static boolean login(final LoginProgress progress, final String username, final String password, final String host, final String portStr, final String domain) {
        int port = XMPPPORT;
        String localDomain = domain;
        String localHost = host;
        canceled = false;
        if (localDomain == null) {
            localDomain = username.substring(username.indexOf("@") + 1);
        }
        if (portStr != null) {
            port = Integer.parseInt(portStr);
        }
        if (localHost == null) {
            if (username.endsWith("gmail.com")) {
                localHost = "talk.google.com";
            } else if (username.endsWith("xiaonei.com")) {
                localHost = "talk.xiaonei.com";
            } else {
                localHost = "talk.jabber.org";
            }
            System.out.println("Host:" + localHost);
        }
        ConnectionConfiguration config = new ConnectionConfiguration(localHost, port, localDomain);
        connection = new XMPPConnection(config);
        try {
            connection.connect();
        } catch (XMPPException e) {
            if (canceled) {
                return false;
            }
            progress.append("Failed");
            return false;
        }
        try {
            if (canceled) {
                connection.disconnect();
                return false;
            }
            progress.append("Connected.\n\tLogin...");
            connection.login(username, password);
        } catch (Exception e) {
            if (canceled) {
                connection.disconnect();
                return false;
            }
            progress.append("\n\tAuthen failed");
            return false;
        }
        if (canceled) {
            connection.disconnect();
            return false;
        }
        progress.append("Logined.\n\tInitial...");
        return true;
    }

    /**
	 * Cancel the connect.
	 */
    public static void cancel() {
        canceled = true;
    }
}
