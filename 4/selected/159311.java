package com.joystick;

import ade.exceptions.ADECallException;
import ade.*;
import ade.ADEGlobals.RecoveryState;
import ade.ADEGlobals.ServerState;
import com.*;
import static utilities.Util.*;
import java.io.*;
import java.rmi.*;
import java.util.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/** 
 * <code>KeyboardJoystickServer</code> takes input from the keyboard (arrow
 * keys) and passes on velocity commands to servers implementing the {@link
 * com.interfaces.VelocityServer VelocityServer} interface.  The default TV can be
 * increased and decreased using PAGE_UP and PAGE_DOWN keys.
 */
public class KeyboardJoystickServer extends ADEServerImpl implements KeyListener {

    private static String prg = "KeyboardJoystickServer";

    private static String type = "KeyboardJoystickServer";

    private static boolean verbose = false;

    private static boolean useVel = false;

    private static boolean gotVel = false;

    private static String velVersion = "MoPoServerImpl";

    private Object vel;

    private JFrame frame;

    private JLabel label;

    private double[] defaultVels;

    private boolean[] arrows = { false, false, false, false };

    private Writer w;

    private boolean shutdown = false;

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
        if (s.indexOf(velVersion) >= 0) {
            gotVel = false;
            while (!gotVel && !shutdown) {
                vel = getClient(velVersion);
            }
        }
        return;
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
        if (s.indexOf(velVersion) >= 0) {
            gotVel = true;
            System.out.println("Got connection to " + velVersion);
        }
        return;
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
        shutdown = true;
        w.halt();
        Sleep(100);
        if (useVel && gotVel) {
            try {
                call(vel, "setVels", 0.0, 0.0);
            } catch (ADECallException ace) {
                System.err.println(prg + ": Error setting vels: " + ace);
            }
        }
        System.out.println("done.");
    }

    /** This server is ready as soon as it is initialized */
    protected boolean localServicesReady() {
        return true;
    }

    /** 
     * KeyboardJoystickServer constructor.
     */
    public KeyboardJoystickServer() throws RemoteException {
        super();
        frame = new JFrame("KeyboardJoystick");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addKeyListener(this);
        label = new JLabel("tv: 0.0 rv: 0.0");
        label.setBackground(Color.BLUE);
        label.setForeground(Color.WHITE);
        label.setOpaque(true);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(150, 20));
        frame.getContentPane().add(label);
        frame.pack();
        frame.setLocation(0, 30);
        frame.setVisible(true);
        JFrame.setDefaultLookAndFeelDecorated(true);
        vel = getClient(velVersion);
        while (!gotVel) Sleep(100);
        try {
            defaultVels = (double[]) call(vel, "getDefaultVels");
            defaultVels[1] *= 2;
        } catch (ADECallException ace) {
            System.err.println(prg + ": Error getting def vels: " + ace);
            defaultVels = new double[2];
        }
        System.out.println("defaultVels: " + defaultVels[0] + " " + defaultVels[1]);
        w = new Writer();
        w.start();
    }

    /**
     * Provide additional information for usage...
     */
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Server-specific options:\n\n");
        sb.append("  -pioneer                   <request Pioneer server>\n");
        sb.append("  -motion                    <request Motion server>\n");
        sb.append("  -segway                    <request Segway server>\n");
        sb.append("  -simbad                    <request Simbad server>\n");
        sb.append("  -adesim                    <request ADESim server>\n");
        sb.append("  -usarsim                   <request USARSim server>\n");
        sb.append("  -mopo                      <request MoPo server>\n");
        sb.append("  -verbose                   <verbose debug printing>\n");
        return sb.toString();
    }

    /** 
     * Parse additional command-line arguments
     * @return "true" if parse is successful, "false" otherwise 
     */
    protected boolean parseadditionalargs(String[] args) {
        boolean found = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-pioneer")) {
                useVel = true;
                velVersion = "PioneerServerImpl";
                found = true;
            } else if (args[i].equalsIgnoreCase("-motion")) {
                useVel = true;
                velVersion = "MotionServerImpl";
                found = true;
            } else if (args[i].equalsIgnoreCase("-segway")) {
                useVel = true;
                velVersion = "SegwayServerImpl";
                found = true;
            } else if (args[i].equalsIgnoreCase("-simbad")) {
                useVel = true;
                velVersion = "SimbadServerImpl";
                found = true;
            } else if (args[i].equalsIgnoreCase("-adesim")) {
                useVel = true;
                velVersion = "ADESimServerImpl";
                found = true;
            } else if (args[i].equalsIgnoreCase("-usarsim")) {
                useVel = true;
                velVersion = "USARSimServerImpl";
                found = true;
            } else if (args[i].equalsIgnoreCase("-mopo")) {
                useVel = true;
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
     * Handler for key press events.
     * @param event the keyboard event
     */
    public void keyPressed(KeyEvent event) {
        int kchar = event.getKeyCode();
        if (kchar == KeyEvent.VK_UP) {
            arrows[0] = true;
        } else if (kchar == KeyEvent.VK_DOWN) {
            arrows[1] = true;
        } else if (kchar == KeyEvent.VK_LEFT) {
            arrows[2] = true;
        } else if (kchar == KeyEvent.VK_RIGHT) {
            arrows[3] = true;
        } else if (kchar == KeyEvent.VK_PAGE_DOWN) {
            defaultVels[0] *= 0.8;
        } else if (kchar == KeyEvent.VK_PAGE_UP) {
            defaultVels[0] *= 1.25;
        } else if (kchar == KeyEvent.VK_C) {
            if (event.isControlDown()) {
                System.exit(0);
            }
        }
    }

    /**
     * Handler for key release events.
     * @param event the keyboard event
     */
    public void keyReleased(KeyEvent event) {
        int kchar = event.getKeyCode();
        if (kchar == KeyEvent.VK_UP) {
            arrows[0] = false;
        } else if (kchar == KeyEvent.VK_DOWN) {
            arrows[1] = false;
        } else if (kchar == KeyEvent.VK_LEFT) {
            arrows[2] = false;
        } else if (kchar == KeyEvent.VK_RIGHT) {
            arrows[3] = false;
        }
    }

    /**
     * Handler for key type events.
     * @param event the keyboard event
     */
    public void keyTyped(KeyEvent event) {
    }

    /**
     * The <code>Writer</code> is the main loop that "interprets" the
     * joystick and sends to Vel if requested.
     */
    private class Writer extends Thread {

        private boolean shouldWrite;

        public Writer() {
            shouldWrite = true;
        }

        public void run() {
            int i = 0;
            double tv = 0.0, rv = 0.0;
            while (shouldWrite) {
                Sleep(100);
                tv = rv = 0.0;
                if (verbose) System.out.println("Arrows: " + arrows[0] + " " + arrows[1] + " " + arrows[2] + " " + arrows[3]);
                if (arrows[0] && !arrows[1]) tv = defaultVels[0]; else if (arrows[1] && !arrows[0]) tv = -defaultVels[0];
                if (arrows[2] && !arrows[3]) rv = defaultVels[1]; else if (arrows[3] && !arrows[2]) rv = -defaultVels[1];
                if (verbose) System.out.println("tv: " + tv + " rv: " + rv);
                label.setText("tv: " + tv + " rv: " + rv);
                if (gotVel) {
                    try {
                        call(vel, "setVels", tv, rv);
                    } catch (ADECallException ace) {
                        System.err.println(prg + ": Error setting vels: " + ace);
                    }
                }
            }
            System.out.println(prg + ": Exiting Writer thread...");
        }

        public void halt() {
            System.out.print("halting write thread...");
            shouldWrite = false;
        }
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
        ADEServerImpl.main(args);
    }
}
