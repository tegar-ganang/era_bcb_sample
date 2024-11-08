package org.maverickdbms.server.telnet;

import java.io.PrintWriter;
import java.io.Reader;
import net.wimpi.telnetd.TelnetD;
import net.wimpi.telnetd.net.Connection;
import net.wimpi.telnetd.net.ConnectionEvent;
import net.wimpi.telnetd.io.BasicTerminalIO;
import net.wimpi.telnetd.io.toolkit.*;
import net.wimpi.telnetd.shell.Shell;
import org.maverickdbms.basic.mvConstants;
import org.maverickdbms.basic.mvIO;
import org.maverickdbms.basic.Properties;
import org.maverickdbms.basic.Session;
import org.maverickdbms.basic.mvString;
import org.maverickdbms.util.SH;

/**
 * This class is an example implmentation of a Shell.<br>
 * It is used for testing the system.<br>
 * At the moment you can see all io toolkit classes in action,
 * pressing "t" at its prompt (instead of the enter, which is
 * requested for logging out again).
 * 
 * @author Dieter Wimberger
 * @version 1.0 25/08/2000
 */
public class mvShell implements Shell {

    private Connection mycon;

    private BasicTerminalIO myio;

    /**
     * Method that runs a shell 
     * 
     * @param con Connection that runs the shell.
     */
    public void run(Connection con) {
        mycon = con;
        myio = mycon.getTerminalIO();
        mycon.addConnectionListener(this);
        Reader reader = new mvTelnetReader(myio);
        PrintWriter writer = new PrintWriter(new mvTelnetWriter(myio));
        Properties prop = new Properties();
        String prop_file = System.getProperty("maverick.config");
        if (prop_file != null) prop.loadFile(prop_file);
        Session session = new Session(prop, reader, writer);
        mvString[] args = new mvString[0];
        SH shell = new SH();
        shell.run(session, args);
    }

    public void connectionTimedOut(ConnectionEvent ce) {
        myio.write("CONNECTION_TIMEDOUT");
        myio.flush();
    }

    public void connectionIdle(ConnectionEvent ce) {
        myio.write("CONNECTION_IDLE");
        myio.flush();
    }

    public void connectionLoggedOff(ConnectionEvent ce) {
        myio.write("CONNECTION_LOGGEDOFF");
        myio.flush();
    }

    public void connectionLogoutRequest(ConnectionEvent ce) {
        myio.write("CONNECTION_LOGOUTREQUEST");
        myio.flush();
    }

    public void connectionBroken(ConnectionEvent ce) {
        myio.write("CONNECTION_BROKEN");
        myio.flush();
    }

    public void connectionSentBreak(ConnectionEvent ce) {
        myio.write("CONNECTION_BREAK");
        myio.flush();
    }

    public static Shell createShell() {
        return new mvShell();
    }
}
