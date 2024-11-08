package topchat.server.dummyclient;

import org.apache.log4j.Logger;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

/**
 * Dumber client used for testing purposes only. Only connects to the server and
 * then stays idle.
 * 
 * Uses the SMACK library.
 */
public class DumberClient implements Runnable {

    /** The connection to the server */
    public static XMPPConnection conn;

    private static String user = null;

    private static String pass = null;

    private static String server = null;

    private static Logger logger = Logger.getLogger(DumbClient.class);

    /**
	 * Connects to the server on localhost. The SMACK library is used for
	 * connecting.
	 * 
	 * @throws XMPPException
	 *             if the connection is unsuccessful.
	 */
    public static void makeConnection() throws XMPPException {
        ConnectionConfiguration config = new ConnectionConfiguration(server, 5222);
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

    public DumberClient(String user, String password, String server) {
        this.user = user;
        this.pass = password;
        this.server = server;
    }

    /**
	 * The DumbClient first makes a connection to the server and than sits in a
	 * loop doing nothing.
	 */
    public void run() {
        boolean running = true;
        try {
            long time = System.currentTimeMillis();
            makeConnection();
            logger.info("Time to connect " + (System.currentTimeMillis() - time) + "ms");
        } catch (XMPPException e) {
            logger.debug("Exception on connection " + e);
        }
        while (running) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
    }

    /**
	 * Starts the dumb client.
	 * 
	 * @param args
	 *            the arguments sent to the program: username and password
	 */
    public static void main(String[] args) {
        (new Thread(new DumberClient(args[0], args[1], args[2]))).start();
    }
}
