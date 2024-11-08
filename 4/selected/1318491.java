package com.cart;

import ade.*;
import ade.ADEGlobals.RecoveryState;
import ade.ADEGlobals.ServerState;
import com.*;
import static utilities.Util.*;
import java.io.*;
import java.lang.reflect.Array;
import java.math.*;
import java.net.*;
import java.rmi.*;
import java.util.*;

/** <code>CartServerImpl</code> for controlling the IU golf cart.
*/
public class CartServerImpl extends ADEServerImpl implements CartServer {

    private static String prg = "CartServerImpl";

    private static String type = "CartServer";

    private static boolean verbose = false;

    private static boolean debug = false;

    public static boolean useLocalLogger = false;

    public Object lrf;

    public boolean gotLRF = false;

    public static boolean useSick = false;

    public static boolean useUrg = false;

    private static String host = "127.0.0.1";

    private static int port = 5000;

    private Socket socket;

    private InputStream in;

    private OutputStream out;

    private Updater u;

    private static boolean runTest = false;

    private double nomTV = 0.0;

    private double nomRV = 0.0;

    private double curTV = 0.0;

    private double curRV = 0.0;

    private double defTV = 1.5;

    private double defRV = -0.3;

    private double lat = 0.0;

    private double lon = 0.0;

    private double heading = 0.0;

    private boolean obstacle = false;

    /**
     * This method will be activated whenever a client calls the
     * requestConnection(uid) method. Any connection-specific initialization
     * should be included here, either general or user-specific.
     */
    protected void clientConnectReact(String user) {
        System.out.println(myID + ": got connection from " + user + "!");
        return;
    }

    /**
     * This method will be activated whenever a client that has called the
     * requestConnection(uid) method fails to update (meaning that the
     * heartbeat signal has not been received by the reaper), allowing both
     * general and user specific reactions to lost connections. If it returns
     * true, the client's connection is removed.
     */
    protected boolean clientDownReact(String user) {
        System.out.println(myID + ": lost connection with " + user + "!");
        return false;
    }

    /**
     * This method will be activated whenever a client that has called
     * the requestConnection(uid) method fails to update (meaning that the
     * heartbeat signal has not been received by the reaper). If it returns
     * true, the client's connection is removed. 
     */
    protected boolean attemptRecoveryClientDown(String user) {
        return false;
    }

    /**
     * This method will be activated whenever the heartbeat returns a
     * remote exception (i.e., the server this is sending a
     * heartbeat to has failed). 
     */
    protected void serverDownReact(String s) {
        if (verbose) System.out.println(prg + ": reacting to down " + s + "...");
        if (s.indexOf("SickLRFServer") >= 0) {
            gotLRF = false;
        } else if (s.indexOf("UrgLRFServer") >= 0) {
            gotLRF = false;
        }
    }

    /** This method will be activated whenever the heartbeat reconnects
     * to a client (e.g., the server this is sending a heartbeat to has
     * failed and then recovered). <b>NOTE:</b> the pseudo-reference will
     * not be set until <b>after</b> this method is executed. To perform
     * operations on the newly (re)acquired reference, you must use the
     * <tt>ref</tt> parameter object.
     * @param s the ID of the {@link ade.ADEServer ADEServer} that connected
     * @param ref the pseudo-reference for the requested server */
    protected void serverConnectReact(String s, Object ref) {
        if (verbose) System.out.println(prg + ": reacting to down " + s + "...");
        if (s.indexOf("SickLRFServer") >= 0) {
            gotLRF = true;
        } else if (s.indexOf("UrgLRFServer") >= 0) {
            gotLRF = true;
        }
    }

    /**
     * Adds additional local checks for credentials before allowing a shutdown
     * must return "false" if shutdown is denied, true if permitted
     */
    protected boolean localrequestShutdown(Object credentials) {
        return false;
    }

    /**
     * Implements the local shutdown mechanism that derived classes need to
     * implement to cleanly shutdown
     */
    protected void localshutdown() {
        System.out.print("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.print(prg + " shutting down...");
        try {
            u.halt();
            Sleep(1000);
            socket.close();
        } catch (IOException ioe) {
            System.err.println(prg + ": Error closing socket: " + ioe);
        }
        System.out.println("done.");
    }

    /**
     * Get translational velocity.
     * @return the most recent TV reading.
     */
    public double getTV() throws RemoteException {
        synchronized (this) {
            return curRV;
        }
    }

    /**
     * Get rotational velocity.
     * @return the most recent RV reading.
     */
    public double getRV() throws RemoteException {
        synchronized (this) {
            return curRV;
        }
    }

    /**
     * Get translational and rotational velocity.
     * @return the most recent velocity readings.
     */
    public double[] getVels() throws RemoteException {
        double[] vels;
        synchronized (this) {
            vels = new double[] { curTV, curRV };
        }
        return vels;
    }

    /**
     * Get the default velocities used by MoPo functions.
     * @return the default velocities.
     */
    public double[] getDefaultVels() throws RemoteException {
        double[] vels;
        synchronized (this) {
            vels = new double[] { defTV, defRV };
        }
        return vels;
    }

    /**
     * Stop.
     */
    public void stop() throws RemoteException {
        synchronized (this) {
            nomTV = 0.0;
            nomRV = 0.0;
        }
    }

    /**
     * Set translational velocity.
     * @param tv the new TV
     * @return true if there's nothing in front of the robot, false
     * otherwise.
     */
    public boolean setTV(double tv) throws RemoteException {
        if (obstacle) return false;
        synchronized (this) {
            nomTV = tv;
        }
        return true;
    }

    /**
     * Set rotational velocity.
     * @param rv the new RV
     */
    public boolean setRV(double rv) throws RemoteException {
        synchronized (this) {
            nomRV = rv;
        }
        return true;
    }

    /**
     * Set both velocities.
     * @param tv the new TV
     * @param rv the new RV
     * @return true if there's nothing in front of the robot, false
     * otherwise.
     */
    public boolean setVels(double tv, double rv) throws RemoteException {
        if (obstacle) return false;
        synchronized (this) {
            nomTV = tv;
            nomRV = rv;
        }
        return true;
    }

    /**
     * Get current location.
     * @return the current location, as indicated by the Player localizer or
     * the GPS server.  The Cart server returns lat/lon instead of x,y so it
     * needs to be used accordingly (see movement server)
     */
    public double[] getPoseGlobal() throws RemoteException {
        double[] pos;
        synchronized (this) {
            pos = new double[] { lon, lat, heading };
        }
        return pos;
    }

    /** The server is always ready when it has all its required references */
    protected boolean localServicesReady() {
        return ((!useSick || lrf != null) && (!useUrg || lrf != null));
    }

    /** 
     * CartServerImpl constructor.
     */
    public CartServerImpl() throws RemoteException {
        super();
        boolean gotSocket = false;
        for (int i = 0; i < 10; i++) {
            try {
                socket = new Socket(host, port);
                socket.setSoTimeout(500);
                gotSocket = true;
                break;
            } catch (IOException ioe) {
                System.err.println(prg + ": Error opening socket: " + ioe);
                System.exit(-1);
            }
        }
        if (!gotSocket) {
            System.err.println(prg + ": Error opening socket");
            System.exit(-1);
        }
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException ioe) {
            System.err.println(prg + ": Error creating reader/writer: " + ioe);
            System.exit(-1);
        }
        if (useSick) {
            if (verbose) System.out.println("Attempting to get Sick LRF client...");
            lrf = getClient("SickLRFServerImpl");
        }
        if (useUrg) {
            if (verbose) System.out.println("Attempting to get Urg LRF client...");
            lrf = getClient("UrgLRFServerImpl");
        }
        u = new Updater();
        u.start();
        if (runTest) test();
    }

    /**
     * Get a message from the cart.
     * @return a String containing the message.
     */
    public String recvMsg() {
        String msg;
        byte[] buf = new byte[1024];
        try {
            in.read(buf);
            msg = new String(buf);
        } catch (Exception e) {
            System.err.println(prg + ": Error reading from socket: " + e);
            msg = null;
        }
        return msg;
    }

    /**
     * Send a message to the cart.
     * @param msg a String containing the mesage.
     * @return true if successfully sent, false otherwise
     */
    public boolean sendMsg(String msg) {
        try {
            out.write(msg.getBytes(), 0, msg.length());
            return true;
        } catch (Exception e) {
            System.err.println(prg + ": Error sending to socket: " + e);
        }
        return false;
    }

    /**
     * Parse a message from the cart.
     * @param msg a String containing the mesage.
     * @return a String containing the message.
     */
    public String parseMsg(String msg) {
        StringTokenizer st = new StringTokenizer(msg, ",");
        StringBuilder sb = new StringBuilder();
        String tmp, num;
        int cs;
        double spde = 0.0, spdn = 0.0;
        while (st.hasMoreTokens()) {
            tmp = st.nextToken();
            if (tmp.startsWith("StatusVehSpeed=")) {
                StringTokenizer st2 = new StringTokenizer(tmp, "=");
                st2.nextToken();
                num = st2.nextToken();
                if (verbose) System.out.println("Message: " + tmp);
                try {
                    curTV = Double.parseDouble(num);
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": error parsing double " + num);
                }
            } else if (tmp.startsWith("CmdVehSpeed=")) {
                tmp = new String("CmdVehSpeed=" + nomTV);
            } else if (tmp.startsWith("StatusTurnRadius=")) {
                StringTokenizer st2 = new StringTokenizer(tmp, "=");
                st2.nextToken();
                num = st2.nextToken();
                if (verbose) System.out.println("Message: " + tmp);
                try {
                    curRV = Double.parseDouble(num);
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": error parsing double " + num);
                }
            } else if (tmp.startsWith("GPSLon=")) {
                StringTokenizer st2 = new StringTokenizer(tmp, "=");
                st2.nextToken();
                num = st2.nextToken();
                if (verbose) System.out.println("Message: " + tmp);
                try {
                    lon = degrees(num);
                } catch (Exception e) {
                    System.err.println(prg + ": error getting degrees " + num);
                    System.err.println(prg + ": not changing lon " + lon);
                }
            } else if (tmp.startsWith("GPSLat=")) {
                StringTokenizer st2 = new StringTokenizer(tmp, "=");
                st2.nextToken();
                num = st2.nextToken();
                if (verbose) System.out.println("Message: " + tmp);
                try {
                    lat = degrees(num);
                } catch (Exception e) {
                    System.err.println(prg + ": error getting degrees " + num);
                    System.err.println(prg + ": not changing lat " + lat);
                }
            } else if (tmp.startsWith("GPSSpdE=")) {
                StringTokenizer st2 = new StringTokenizer(tmp, "=");
                st2.nextToken();
                num = st2.nextToken();
                if (verbose) System.out.println("Message: " + tmp);
                try {
                    spde = Double.parseDouble(num);
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": error parsing double " + num);
                }
            } else if (tmp.startsWith("GPSSpdN=")) {
                StringTokenizer st2 = new StringTokenizer(tmp, "=");
                st2.nextToken();
                num = st2.nextToken();
                if (verbose) System.out.println("Message: " + tmp);
                try {
                    spdn = Double.parseDouble(num);
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": error parsing double " + num);
                }
            } else if (tmp.startsWith("CmdTurnRadius=")) {
                tmp = new String("CmdTurnRadius=" + nomRV);
            }
            sb.append(tmp + ",");
        }
        heading = Math.atan2(spdn, spde);
        cs = checksum(msg);
        if (verbose) System.out.println("spd: " + curTV + ", trn: " + curRV + ", lon: " + lon + ", lat: " + lat + ", hdg: " + heading);
        return sb.toString() + String.format("%02x", cs);
    }

    /**
     * Parse the NMEA degree/min/sec string to degrees.
     * @param nmea a String containing the NMEA position
     * @return the dosition in degrees
     */
    public double degrees(String nmea) {
        double deg;
        int dot = nmea.indexOf('.');
        int beg = 0;
        if (nmea.startsWith("-") || nmea.startsWith("+")) beg = 1;
        if (beg == (dot - 2)) deg = 0.0; else deg = Double.parseDouble(nmea.substring(beg, dot - 2));
        deg += Double.parseDouble(nmea.substring(dot - 2)) / 60.0;
        if (nmea.startsWith("-")) deg = -deg;
        return deg;
    }

    /**
     * Compute the checksum for a cart message.
     * @param msg a String containing the message.
     * @return the checksum.
     */
    public int checksum(String msg) {
        char cs = 0;
        byte[] buf = msg.getBytes();
        for (int i = 0; i < msg.length(); i++) {
            cs += buf[i];
        }
        cs %= 255;
        return (int) cs;
    }

    /**
     * The <code>Updater</code> sends periodic messages to the cart.
     */
    private class Updater extends Thread {

        boolean shouldUpdate;

        public Updater() {
            shouldUpdate = true;
        }

        public void run() {
            int i = 1;
            String msg;
            while (shouldUpdate) {
                try {
                    if (gotLRF) obstacle = (Boolean) call(lrf, "checkObstacle");
                    if (obstacle) nomTV = 0.0;
                    msg = recvMsg();
                    synchronized (this) {
                        msg = parseMsg(msg);
                    }
                    sendMsg(msg);
                    Sleep(50);
                } catch (Exception e1) {
                    System.out.println(prg + ": got generic exception");
                    e1.printStackTrace();
                }
            }
            System.out.println(prg + ": Exiting Updater thread...");
        }

        public void halt() {
            System.out.print("halting update thread...");
            shouldUpdate = false;
        }
    }

    /**
     * Provide additional information for usage...
     */
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Server-specific options:\n\n");
        sb.append("  -host <host>          <override default (" + host + ")>\n");
        sb.append("  -port <port>          <override default (" + port + ")>\n");
        sb.append("  -sick                 <request Sick LRF server>\n");
        sb.append("  -urg                  <request Urg LRF server>\n");
        sb.append("  -verbose              <verbose output>\n");
        sb.append("  -test                 <run test sequence>\n");
        return sb.toString();
    }

    /** 
     * Parse additional command-line arguments
     * @return "true" if parse is successful, "false" otherwise 
     */
    protected boolean parseadditionalargs(String[] args) {
        boolean found = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-host") && (++i < args.length)) {
                host = args[i];
                found = true;
            } else if (args[i].equalsIgnoreCase("-port") && (++i < args.length)) {
                int p;
                try {
                    p = Integer.parseInt(args[i]);
                    port = p;
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": Incorrect port format (" + args[i] + "), keeping default");
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-sick")) {
                useSick = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-urg")) {
                useUrg = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-verbose")) {
                verbose = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-test")) {
                runTest = true;
                found = true;
            } else {
                System.out.println("Unrecognized argument: " + args[i]);
                return false;
            }
        }
        return found;
    }

    /**
     * Log a message using ADE Server Logging, if possible.  The ASL facility
     * takes care of adding timestamp, etc.
     * @param o the message to be logged
     */
    protected void logItASL(Object o) {
        canLogIt(o);
    }

    /**
     * Set the state of ADE Server Logging.  When true and logging is not
     * started, this starts logging.  When false and logging is started, this
     * stops logging and causes the log files to be written to disk.  ADE server
     * logging is a global logging facility, so starting logging here enables
     * logging in a currently instantiated ADE servers.  NOTE: You want to stop
     * ADE server logging before quitting, or the files will not be complete.
     * @param state indicates whether to start (true) or stop (false) logging.
     */
    protected void setASL(boolean state) {
        try {
            setADEServerLogging(state);
        } catch (Exception e) {
            System.out.println("setASL: " + e);
        }
    }

    public void test() {
        System.out.println(prg + ": setting cart velocity to " + defTV + ".");
        nomTV = defTV;
        Sleep(5000);
        System.out.println(prg + ": setting cart velocity to 0.");
        nomTV = 0.0;
    }

    /**
     * <code>main</code> passes the arguments up to the ADEServerImpl
     * parent.  The parent does some magic and gets the system going.
     */
    public static void main(String[] args) throws Exception {
        ADEServerImpl.main(args);
    }
}
