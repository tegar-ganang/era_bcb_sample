package czestmyr.jjsched;

import java.lang.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;

/**
 * The main class of the program.
 * Handles the program initialization, parameter parsing and creation of class instances.
 */
public class jjsched {

    /** 
     * Application entry point.
     * Parses the parameters and creates instance of self. Also contains the main program loop.
     */
    public static void main(String[] args) {
        Thread.currentThread().setName("JJSched main thread");
        Thread.currentThread().setPriority(9);
        boolean autocreate = false;
        String configFile = "jjsched.conf";
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("--autocreate")) {
                autocreate = true;
            } else if (args[i].equals("--configfile")) {
                ++i;
                if (i >= args.length) {
                    System.err.println("No configuration file specified after option \"--configfile\"");
                    return;
                }
                configFile = args[i];
            } else if (args[i].equals("--help")) {
                System.out.println("JJSched - Java Jabber Scheduler\n" + "A simple XMPP scheduling application, version 0.1\n" + "Copyright Cestmir 'Czestmyr' Houska, 2008\n" + "\n" + "Command-line options:\n" + "--configfile file.conf - specifies where the configuration file resides\n" + "--autocreate           - automatically creates the needed DB tables if they are not created yet\n" + "                       - Be careful with this option because it can destroy other tables that would have the same name as the jjsched tables");
                return;
            }
        }
        jjsched me = new jjsched(configFile, autocreate);
        me.init();
        if (me.bad) return;
        while (me.run() && me.doContinue) {
        }
        me.theConsole.discontinue();
    }

    /**
     * Constructor.
     * Creates instances of JJSched classes and interconnects them. Tries to initialize the connections
     * using their respective classes.
     */
    jjsched(String config, boolean autocreate) {
        theMacros = new Macros();
        theConfig = new Config(theMacros);
        try {
            theConfig.parse(config);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            bad = true;
            return;
        }
        String server = null;
        String username = null;
        String password = null;
        try {
            server = theConfig.getString("XMPPServerName");
            username = theConfig.getString("XMPPUserName");
            password = theConfig.getString("XMPPPassword");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            bad = true;
            return;
        }
        dbc = new DatabaseConnector(theConfig, theTime);
        try {
            dbc.connect(autocreate);
        } catch (Exception e) {
            System.out.println("Error during MySQL initialization:");
            System.out.println(e.getMessage());
            bad = true;
            return;
        }
        theConnectionConfig = new ConnectionConfiguration(server, 5222);
        theConnectionConfig.setCompressionEnabled(true);
        theConnectionConfig.setSASLAuthenticationEnabled(true);
        try {
            theConnector = new Connector(new XMPPConnection(theConnectionConfig), dbc);
            theConnector.connect();
            theConnector.login(username, password);
        } catch (Exception e) {
            try {
                theConnector.disconnect();
            } catch (Exception e2) {
            }
            System.out.println("Could not connect to the XMPP server. Check your configuration file: " + e.getMessage());
            bad = true;
            return;
        }
        theParser = new CommandParser(dbc, theConfig, theMacros, theTime);
        theExecutor = new ActionExecutor(theConnector, dbc, theConfig, theTime, this);
        theConsole = new Console(theParser, theExecutor, theMacros);
        theConsole.start();
        theHandler = new PacketHandler(theConnector, theConfig, dbc, theTime, theMacros, theParser, theExecutor);
        theConnector.addMessageListener(theHandler);
        theSender = new Sender(dbc, theConnector);
    }

    /**
     * Initializes the time counters.
     */
    private void init() {
        previousSendingTime = System.currentTimeMillis();
        nextWaitingTime = 1000;
    }

    /**
     * Runs the program, sending all actual messages in Sender.
     *
     * @return  True if the program should run on, false otherwise.
     */
    private boolean run() {
        try {
            Thread.currentThread().sleep(nextWaitingTime);
        } catch (InterruptedException e) {
        }
        theSender.sendActual();
        nextWaitingTime = 100 + previousSendingTime - System.currentTimeMillis();
        if (nextWaitingTime < 0) nextWaitingTime = 0;
        previousSendingTime = System.currentTimeMillis();
        return true;
    }

    /**
	 * Quit the application
	 */
    public void quit() {
        doContinue = false;
    }

    private ConnectionConfiguration theConnectionConfig;

    private Connector theConnector;

    private Config theConfig;

    private Macros theMacros;

    private PacketHandler theHandler;

    private DatabaseConnector dbc;

    private ActionExecutor theExecutor;

    private CommandParser theParser;

    private TimeHandler theTime = new TimeHandler();

    private Sender theSender;

    private Console theConsole;

    private boolean init = false;

    private boolean bad = false;

    private boolean doContinue = true;

    private long nextWaitingTime;

    private long previousSendingTime;
}
