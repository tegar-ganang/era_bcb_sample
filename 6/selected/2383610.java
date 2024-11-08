package topchat.server.dummyclient;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

/**
 * Dumb client used for testing purposes only. Only connects to the server and
 * then stays idle.
 * 
 * Uses the SMACK library.
 */
public class DumbClient implements Runnable {

    /** The connection to the server */
    public static XMPPConnection conn;

    private static Logger logger = Logger.getLogger(DumbClient.class);

    /**
	 * Connects to the server on localhost. The SMACK library is used for
	 * connecting.
	 * 
	 * @throws XMPPException
	 *             if the connection is unsuccessful.
	 */
    public static void makeConnection() throws XMPPException {
        String user = "elena";
        String pass = "parolica";
        ConnectionConfiguration config = new ConnectionConfiguration("localhost", 5222);
        conn = new XMPPConnection(config);
        try {
            conn.connect();
            if (conn.isConnected()) {
                SASLAuthentication.supportSASLMechanism("PLAIN", 0);
                conn.login(user, pass, "");
                logger.info("I'm connected!");
            }
        } catch (XMPPException ex) {
            logger.fatal(ex);
        }
    }

    /**
	 * The DumbClient first makes a connection to the server and than sits in a
	 * loop doing nothing.
	 */
    public void run() {
        boolean running = true;
        try {
            makeConnection();
        } catch (XMPPException e) {
            logger.debug("Exception on connection " + e);
        }
        while (running) {
        }
    }

    /**
	 * Starts the dumb client.
	 * 
	 * @param args
	 *            the arguments sent to the program
	 */
    public static void main(String[] args) {
        (new Thread(new DumbClient())).start();
    }
}
