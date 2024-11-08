package com.gps;

import ade.*;
import com.*;
import java.io.*;
import java.math.*;
import java.net.*;
import java.rmi.*;
import java.util.*;
import ade.ADEGlobals.RecoveryState;
import ade.ADEGlobals.ServerState;
import gnu.io.*;
import static utilities.Util.*;

/**
 * <code>GPSD</code> version of the GPS server.
 */
public class GPSDServerImpl extends GPSServerImpl implements GPSServer {

    public static boolean useLocalLogger = false;

    public static boolean verbose = false;

    private Updater u;

    private static String host = "127.0.0.1";

    private static int port = 2947;

    private Socket socket;

    private InputStream in;

    private OutputStream out;

    private boolean status = false;

    private double lat = 0.0;

    private double lon = 0.0;

    private double heading = 0.0;

    private double vel = 0.0;

    /**
     * Check whether the GPS device is ready to provide valid results.
     * @return true if ready, false otherwise
     */
    public boolean GPSReady() throws RemoteException {
        return status;
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

    /**
     * Implements the local shutdown mechanism that derived classes need to
     * implement to cleanly shutdown
     */
    public void shutdown() {
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

    /** The server is always ready to provide its service after it has come up */
    protected boolean localServicesReady() {
        return true;
    }

    /** 
     * GPSDServerImpl constructor.
     */
    public GPSDServerImpl() throws RemoteException {
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
        u = new Updater();
        u.start();
    }

    /**
     * Get a message from gpsd.
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
     * Request and parse a report from gpsd.
     */
    public void queryGPS() {
        String msg;
        StringTokenizer st;
        String tmp;
        double num;
        if (!sendMsg("o\n")) {
            return;
        }
        msg = recvMsg();
        st = new StringTokenizer(msg, " ");
        tmp = st.nextToken();
        if (!tmp.startsWith("GPSD,O=")) {
            System.err.println("Unrecognized response: " + msg);
            return;
        }
        tmp = st.nextToken();
        tmp = st.nextToken();
        tmp = st.nextToken();
        try {
            num = Double.parseDouble(tmp);
            lat = num;
        } catch (NumberFormatException nfe) {
            System.err.println("Error parsing double: " + tmp);
        }
        tmp = st.nextToken();
        try {
            num = Double.parseDouble(tmp);
            lon = num;
        } catch (NumberFormatException nfe) {
            System.err.println("Error parsing double: " + tmp);
        }
        tmp = st.nextToken();
        tmp = st.nextToken();
        tmp = st.nextToken();
        tmp = st.nextToken();
        try {
            num = Double.parseDouble(tmp);
            heading = num;
        } catch (NumberFormatException nfe) {
            System.err.println("Error parsing double: " + tmp);
        }
        tmp = st.nextToken();
        try {
            num = Double.parseDouble(tmp);
            vel = num;
        } catch (NumberFormatException nfe) {
            System.err.println("Error parsing double: " + tmp);
        }
        tmp = st.nextToken();
        tmp = st.nextToken();
        tmp = st.nextToken();
        tmp = st.nextToken();
        tmp = st.nextToken();
        if (tmp.equals("2") || tmp.equals("3")) {
            status = true;
        } else {
            status = false;
        }
        if (verbose) System.out.print("lat: " + lat + ", lon: " + lon + ", heading: " + heading + ", vel: " + vel + ", mode: " + tmp);
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
            String msg;
            System.out.print("Waiting for GPS data...");
            while (!status) {
                sendMsg("m\n");
                msg = recvMsg();
                if (msg.startsWith("GPSD,M=2") || msg.startsWith("GPSD,M=3")) {
                    status = true;
                    System.out.println("got it.");
                }
            }
            while (shouldUpdate) {
                try {
                    synchronized (this) {
                        queryGPS();
                    }
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
        sb.append("  -verbose              <verbose output>\n");
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
            } else if (args[i].equalsIgnoreCase("-verbose")) {
                verbose = true;
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
     * stops logging and causes the log files to be written to disk.  ADE server     * logging is a global logging facility, so starting logging here enables
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

    /**
     * <code>main</code> passes the arguments up to the ADEServerImpl
     * parent.  The parent does some magic and gets the system going.
     */
    public static void main(String[] args) throws Exception {
        try {
            Class.forName("gnu.io.NoSuchPortException");
        } catch (ClassNotFoundException e) {
            System.out.println("\nERROR: Can't find gnu.io classes; is RXTXcomm.jar in the cpasspath?");
            System.exit(0);
        }
        ADEServerImpl.main(args);
    }
}
