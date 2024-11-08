package jhomenet.shell.server.telnet;

import jhomenet.system.*;
import jhomenet.shell.*;
import jhomenet.shell.server.*;
import java.io.*;
import java.net.*;
import org.apache.log4j.*;

/**
 * A simple server that uses the Telnet protocol as described in RFC 854. 
 * This server uses a ServerSocket to listen on the specified port
 * (defaults to port 23) for Telnet connection requests.  For each
 * connection made, a Telnet session is created.  All command processing
 * is handled by the Telnet session, not this server.
 */
public class TelnetServer extends Server {

    /**
     * Define a logging mechanism.
     */
    private static Logger logger = Logger.getLogger(TelnetServer.class.getName());

    static final int PORT = 23;

    private int timeout;

    private static String welcomeFile;

    private static boolean rootLoginAllowed;

    private ServerSocket socket;

    /**
     * Prepares the Telnet server to listen on the well known Telnet port (23).  The server will not
     * be started and no connections will be accepted until its <code>run()</code> method is executed.
     */
    public TelnetServer(SystemInterface systemInterface) throws IOException {
        this(systemInterface, PORT);
    }

    /**
     * Prepares the Telnet server to listen on an arbitrary port.  The server will not
     * be started and no connections will be accepted until its <code>run()</code> method is executed.
     */
    public TelnetServer(SystemInterface systemInterface, int port) throws IOException {
        super(systemInterface);
        socket = new ServerSocket(port);
        welcomeFile = ServerProperties.getProperty("telnet.welcome");
        if ((welcomeFile != null) && (welcomeFile.length() == 0)) welcomeFile = null;
        String value = null;
        if ((value = ServerProperties.getProperty("telnet.allow_root")) != null) {
            rootLoginAllowed = !value.equals("false");
        } else {
            rootLoginAllowed = true;
        }
        if ((value = ServerProperties.getProperty("telnet.timeout")) != null) {
            try {
                timeout = Integer.parseInt(value);
            } catch (NumberFormatException nfe) {
                timeout = 0;
            }
        } else timeout = 0;
    }

    /**
     * Gets the location of the file to be displayed when after a user logs in to this server.  
     * This file is specified by setting the "TELNET_WELCOME" properties variable equal to 
     * the location of the file.  This method will always return the value the variable had 
     * when the server was constructed.  Changing the value of properties variable after
     * creating the server will have no effect.
     *
     * @return  the location of the welcome file to display, or <code>null</code>
     * if no welcome file was specified
     */
    public static String getWelcomeFile() {
        return welcomeFile;
    }

    /**
     * Indicates whether root access is allowed to this Telnet server.  This
     * is specified with the properties variable "TELNET_ROOT_ALLOWED".  Set the
     * variable to "false" to disallow root login.  All other values will
     * be interpreted as <code>true</code> and root logins will be accepted.
     * This method will always return the value the variable had when the server
     * was constructed.  Changing the value of properties variable after
     * creating the server will have no effect.
     *
     * @return <code>true</code> if root is allowed to login
     */
    public static boolean isRootAllowed() {
        return rootLoginAllowed;
    }

    /**
     * Listens on the connection port for connection requests.  Once a
     * request is made, it creates, initializes, and returns a new 
     * TelnetSession to handle that request.  This method will block until
     * a connection is made.
     *
     * @return  a new <code>TelnetSession</code>
     */
    private int last_wait = 100;

    private static final int MAXIMUM_WAIT = 1000 * 60 * 5;

    protected Session acceptNewSession() {
        TelnetSession newSession = null;
        try {
            Socket sock = null;
            try {
                sock = socket.accept();
                logger.debug("accepted new telnet connection: " + sock.getInetAddress().toString());
                last_wait = 100;
                sock.setSoTimeout(timeout);
            } catch (BindException be) {
                throw be;
            } catch (IOException ioe) {
                try {
                    Thread.sleep(last_wait);
                } catch (InterruptedException ie) {
                }
                last_wait = last_wait << 1;
                if (last_wait > MAXIMUM_WAIT) {
                    last_wait = MAXIMUM_WAIT;
                }
                return null;
            } catch (OutOfMemoryError oome) {
                System.gc();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ioe) {
                }
                return null;
            }
            if (shutdown) return null;
            SystemPrintStream sout = new SystemPrintStream(sock.getOutputStream());
            TelnetInputStream sin = new TelnetInputStream(sock.getInputStream(), sout);
            logger.debug("creating new telnet session");
            newSession = new TelnetSession(sin, sout, sout, sock, this);
            logger.debug("new telnet session created");
            sin.setSession(newSession);
            try {
                logger.debug("starting new telnet session");
                newSession.start();
            } catch (Throwable t) {
                sout.write("Thread limit reached.  Connection Terminated.".getBytes());
                sock.close();
                newSession = null;
            }
        } catch (IOException ioe) {
            logger.error("Telnet error: " + ioe.getLocalizedMessage(), ioe);
            try {
                if (!shutdown) shutDown();
            } catch (Throwable t) {
                shutdown = true;
            }
        }
        return newSession;
    }

    /**
     * Closes the ServerSocket used to listen for connections.
     */
    protected synchronized void closeAllPorts() throws IOException {
        socket.close();
    }
}
