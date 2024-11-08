package com.usarsim;

import ade.*;
import com.reddy.Humanoid;
import com.reddy.HumanoidImpl;
import com.*;
import com.vision.MemoryObject;
import java.lang.reflect.Array;
import java.lang.ThreadGroup;
import java.math.*;
import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.util.*;
import javax.sound.sampled.*;
import static utilities.Util.*;

/**
 * The ADE USARSim server, providing access to a simulated Reddy torso and
 * Pioneer base with SICK LRF.
 */
public class USARSimServerImpl extends HumanoidImpl implements Humanoid, USARSimServer {

    private static String prg = "USARSimServerImpl";

    private static String type = "USARSimServer";

    private static boolean reddy = false;

    private static String host = "127.0.0.1";

    private static int port = 3000;

    private Socket socket;

    private BufferedReader in;

    private OutputStream out;

    private Map<String, double[]> startPoses = new HashMap<String, double[]>();

    private Updater u;

    private ContMotionServer CMServer;

    private static double threshold = 0.01;

    private static int[] updated = new int[3];

    private static double[] curLeftArm = new double[3];

    private static double[] curRightArm = new double[3];

    private static double[] curEyes = new double[3];

    private static double[] curEyebrows = new double[2];

    private static double[] curMouth = new double[4];

    private static double[] curMouthMiddle = new double[4];

    private static double[] curHead = new double[2];

    private static boolean runTest = false;

    private static boolean autoPose = false;

    private static String desiredPose;

    private static double defTV = 0.25;

    private static double defRV = Math.PI / 2 * 0.25;

    private double curTV = 0.0;

    private double curRV = 0.0;

    private double lastTV = 0.0;

    private double lastRV = 0.0;

    private double lastLeft = 0.0;

    private double lastRight = 0.0;

    private double[] laser = new double[181];

    private double[] laserLeft = new double[60];

    private double[] laserFront = new double[61];

    private double[] laserRight = new double[60];

    private double oldTime = 0.0;

    private double curTime = 0.0;

    public final short X = 0;

    public final short Y = 1;

    public final short Z = 2;

    public final short THETA = 3;

    private static double initx = 0.0;

    private static double inity = 0.0;

    private static double initz = -0.3;

    private static double initt = 0.0;

    private double[] oldLoc = new double[] { 0.0, 0.0, 0.0 };

    private double[] curLoc = new double[] { initx, inity, initz };

    private double[] oldRot = new double[] { 0.0, 0.0, 0.0 };

    private double[] curRot = new double[] { 0.0, 0.0, initt };

    private static double wheelRad = 0;

    private static double wheelSep = 0;

    private static double wheelBase = 0;

    private static double mass = 0;

    public static double CRITICALDIST = 0.3;

    public static boolean avoidObstacles = true;

    public boolean safeRight = true;

    public boolean safeFront = true;

    public boolean safeLeft = true;

    public boolean openRight = true;

    public boolean openFront = true;

    public boolean openLeft = true;

    public boolean inDanger = false;

    public static boolean posed = false;

    private String name;

    private static boolean doSim = false;

    private static Long headID = 0L;

    /**
	 * This method will be activated whenever the heartbeat returns a
	 * remote exception (i.e., the server this is sending a
	 * heartbeat to has failed). 
	 */
    protected void serverDownReact(String s) {
        System.out.println(prg + ": reacting to down " + s + "...");
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
        System.out.println(myID + ": reacting to connecting " + s + " server...");
        return;
    }

    /** 	
	 * Implements the local shutdown mechanism that derived classes need to
	 * implement to cleanly shutdown
	 */
    protected void localshutdown() {
    }

    /** This server is ready as soon as it has acquired the request servers */
    protected boolean localServicesReady() {
        return (!doSpeak || fsi != null);
    }

    /**
	 * Provide additional information for usage...
	 */
    protected String additionalUsageInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Server-specific options:\n\n");
        sb.append("  -host <host>          <override default (" + host + ")>\n");
        sb.append("  -port <port>          <override default (" + port + ")>\n");
        sb.append("  -critical c	<default critical dist>\n");
        sb.append("  -deftv tv		<default translational velocity>\n");
        sb.append("  -defrv rv		<default rotational velocity>\n");
        sb.append("  -initpose x y z t \n     OR -initpose getpose p	<initial pose>\n");
        sb.append("  -noobst		<disable obstacle avoidance>\n");
        sb.append("  -cfg f		<use environment config file f>\n");
        sb.append("  -test              <run test sequence>\n");
        sb.append("  -reddy				<instantiate cramer instead of pioneer>\n");
        sb.append("  -speak                     <enable speech production>\n");
        sb.append("  -outdev <n>                <use device n for audio out>\n");
        sb.append("  -sleepdec <n>              <set sleepDec to n>\n");
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
            } else if (args[i].equalsIgnoreCase("-critical")) {
                double tv;
                try {
                    tv = Double.parseDouble(args[i + 1]);
                    i++;
                    CRITICALDIST = tv;
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": critical " + args[i + 1]);
                    System.err.println(prg + ": " + nfe);
                    System.err.println(prg + ": reverting to critical = " + CRITICALDIST);
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-deftv")) {
                double tv;
                try {
                    tv = Double.parseDouble(args[i + 1]);
                    i++;
                    defTV = tv;
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": deftv " + args[i + 1]);
                    System.err.println(prg + ": " + nfe);
                    System.err.println(prg + ": reverting to defTV = " + defTV);
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-defrv")) {
                double rv;
                try {
                    rv = Double.parseDouble(args[i + 1]);
                    i++;
                    defRV = rv;
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": defrv " + args[i + 1]);
                    System.err.println(prg + ": " + nfe);
                    System.err.println(prg + ": reverting to defRV = " + defRV);
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-initpose")) {
                double[] init = new double[4];
                posed = true;
                if (args[i + 1].equalsIgnoreCase("getpose")) {
                    autoPose = true;
                    desiredPose = args[i + 2];
                    i += 2;
                } else {
                    try {
                        init[X] = Double.parseDouble(args[i + 1]);
                        i++;
                        init[Y] = Double.parseDouble(args[i + 1]);
                        i++;
                        init[Z] = Double.parseDouble(args[i + 1]);
                        i++;
                        init[THETA] = Double.parseDouble(args[i + 1]);
                        i++;
                        initx = init[X];
                        inity = init[Y];
                        initz = init[Z];
                        initt = init[THETA];
                    } catch (NumberFormatException nfe) {
                        System.err.println(prg + ": initpose " + args[i + 1]);
                        System.err.println(prg + ": " + nfe);
                        System.err.println(prg + ": reverting to default pose");
                    }
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-reddy")) {
                reddy = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-speak")) {
                doSpeak = true;
                found = true;
            } else if (args[i].equalsIgnoreCase("-outdev")) {
                int dev;
                try {
                    dev = Integer.parseInt(args[i + 1]);
                    i++;
                    outDev = dev;
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": outdev " + args[i + 1]);
                    System.err.println(prg + ": " + nfe);
                    System.err.println(prg + ": default outDev is " + outDev);
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-sleepdec")) {
                long dec;
                try {
                    dec = Long.parseLong(args[i + 1]);
                    i++;
                    sleepDec = dec;
                } catch (NumberFormatException nfe) {
                    System.err.println(prg + ": sleepdec " + args[i + 1]);
                    System.err.println(prg + ": " + nfe);
                    System.err.println(prg + ": default sleepDec is " + sleepDec);
                }
                found = true;
            } else if (args[i].equalsIgnoreCase("-noobst")) {
                avoidObstacles = false;
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
	 * Get translational velocity.
	 * @return the most recent TV reading.
	 */
    public double getTV() throws RemoteException {
        return curTV;
    }

    /**
	 * Get rotational velocity.
	 * @return the most recent RV reading.
	 */
    public double getRV() throws RemoteException {
        return curRV;
    }

    /**
	 * Get translational and rotational velocity.
	 * @return the most recent velocity readings.
	 */
    public double[] getVels() throws RemoteException {
        return new double[] { curTV, curRV };
    }

    /**
	 * Get the default velocities used by MoPo functions.
	 * @return the default velocities.
	 */
    public double[] getDefaultVels() throws RemoteException {
        return new double[] { defTV, defRV };
    }

    /**
	 * Stop.
	 */
    public void stop() throws RemoteException {
        canLogIt("Stopping.");
        setVels(0, 0);
        lastTV = 0.0;
        lastRV = 0.0;
        lastLeft = 0.0;
        lastRight = 0.0;
    }

    /**
	 * Set translational velocity.
	 * @param tv the new TV
	 * @return true if there's nothing in front of the robot, false
	 * otherwise.
	 */
    public boolean setTV(double tv) throws RemoteException {
        canLogIt("setTV: " + tv);
        double speedLeft = 1 * ((tv - (1.25 * lastRV * wheelSep) / 2) / wheelRad);
        double speedRight = 1 * ((tv + (1.25 * lastRV * wheelSep) / 2) / wheelRad);
        if (safeFront || (!safeFront && tv < 0)) {
            sendMsg("DRIVE {Left " + speedLeft + "} {Right " + speedRight + "}");
            lastTV = tv;
            lastLeft = speedLeft;
            lastRight = speedRight;
            inDanger = false;
            return true;
        } else {
            System.out.println("Obstacle in front. Not setting TV.");
        }
        return false;
    }

    /**
	 * Set rotational velocity.
	 * @param rv the new RV
	 * @return true if there's nothing to the sides of the robot, false
	 * otherwise.
	 */
    public boolean setRV(double rv) throws RemoteException {
        canLogIt("setRV: " + rv);
        double speedLeft = ((lastTV - (1.25 * rv * wheelSep) / 2) / wheelRad);
        double speedRight = ((lastTV + (1.25 * rv * wheelSep) / 2) / wheelRad);
        if ((rv < 0 && safeLeft) || (rv > 0 && safeRight) || rv == 0) {
            sendMsg("DRIVE {Left " + speedLeft + "} {Right " + speedRight + "}");
            lastRV = rv;
            lastLeft = speedLeft;
            lastRight = speedRight;
            inDanger = false;
            return true;
        } else {
            System.out.println("Obstacle to side. Not setting RV.");
        }
        return false;
    }

    /**
	 * Set both velocities.
	 * @param tv the new TV
	 * @param rv the new RV
	 * @return true if there's nothing in front of the robot, false
	 * otherwise.
	 */
    public boolean setVels(double tv, double rv) throws RemoteException {
        boolean retval = true;
        canLogIt("setVels: " + tv + " " + rv);
        System.out.println("setVels: " + tv + " " + rv);
        if (tv > 0.0) {
            if (rv > 0.0) retval = (safeFront | safeLeft); else if (rv < 0.0) retval = (safeFront | safeRight); else retval = safeFront;
        } else if (rv > 0.0) {
            retval = safeLeft;
        } else if (rv < 0.0) {
            retval = safeRight;
        }
        if (!safeFront && tv > 0) {
            System.out.println("!safeFront, setting tv to 0");
            tv = 0.0;
        }
        double speedLeft = ((tv - (1.25 * rv * wheelSep) / 2) / wheelRad);
        double speedRight = ((tv + (1.25 * rv * wheelSep) / 2) / wheelRad);
        sendMsg("DRIVE {Left " + speedLeft + "} {Right " + speedRight + "}");
        lastTV = tv;
        lastRV = rv;
        lastLeft = speedLeft;
        lastRight = speedRight;
        inDanger = false;
        return retval;
    }

    /**
	 * Get current location.
	 * @return the current location.
	 */
    public double[] getPoseGlobal() throws RemoteException {
        double[] d = new double[4];
        d[X] = curLoc[X];
        d[Y] = curLoc[Y];
        d[Z] = curRot[2];
        d[THETA] = curRot[2];
        return d;
    }

    /**
	 * Get the distance that MoPo takes to be the distance at which obstacle
	 * avoidance should engage.
	 * @return the critical distance.
	 */
    public double getCritDist() throws RemoteException {
        return CRITICALDIST;
    }

    /**
	 * Set the distance that MoPo takes to be the distance at which obstacle
	 * avoidance should engage.
	 * @param dist the new critical distance
	 */
    public void setCritDist(double dist) throws RemoteException {
        CRITICALDIST = dist;
    }

    /**
	 * Check open spaces.  A sector is open if there's space there for the
	 * robot to move into.
	 * @return an array of booleans indicating open*
	 */
    public boolean[] getOpenSpaces() throws RemoteException {
        return new boolean[] { openRight, openFront, openLeft };
    }

    /**
	 * Check whether there is currently an obstacle in front.
	 * @return true if an obstacle is present, false otherwise.
	 */
    public boolean checkObstacle() throws RemoteException {
        return !safeFront;
    }

    /**
	 * Get LRF readings.
	 * @return the most recent set of LRF readings.
	 */
    public double[] getLaserReadings() throws RemoteException {
        return laser;
    }

    /**
	 * Check safe spaces.  A sector is safe if there's no obstacle detected
	 * within CRITICAL_DIST.
	 * @return an array of booleans indicating safe*
	 */
    public boolean[] getSafeSpaces() throws RemoteException {
        return new boolean[] { safeRight, safeFront, safeLeft };
    }

    public boolean moveHead(int pitch, int yaw) throws RemoteException {
        return moveHead(pitch, yaw, 15);
    }

    public boolean moveLeftArm(int ePitch, int sYaw, int sPitch) throws RemoteException {
        return moveArm(false, ePitch, sYaw, sPitch);
    }

    public boolean moveRightArm(int ePitch, int sYaw, int sPitch) throws RemoteException {
        return moveArm(true, ePitch, sYaw, sPitch);
    }

    public boolean moveArm(boolean Right, int ePitch, int sYaw, int sPitch) throws RemoteException {
        return moveArm(Right, ePitch, sYaw, sPitch, 15);
    }

    public boolean moveEyes(int left, int right, int pitch) throws RemoteException {
        return moveEyes(left, right, pitch, 15);
    }

    public boolean moveEyeBrows(int left, int right) throws RemoteException {
        return moveEyeBrows(left, right, 15);
    }

    public boolean moveMouth(int uLeft, int lLeft, int uRight, int lRight) throws RemoteException {
        if (uLeft < 20) uLeft = 20; else if (uLeft > 70) uLeft = 70;
        if (lLeft < 20) lLeft = 20; else if (lLeft > 70) lLeft = 70;
        if (uRight < 20) uRight = 20; else if (uRight > 70) uRight = 70;
        if (lRight < 20) lRight = 20; else if (lRight > 70) lRight = 70;
        double uLeftf = (((double) (uLeft - 45) * Math.PI) / 180);
        double lLeftf = (((double) (lLeft - 45) * Math.PI) / 180);
        double uRightf = (((double) (uRight - 45) * Math.PI) / 180);
        double lRightf = (((double) (lRight - 45) * Math.PI) / 180);
        double uMidPrisf = (Math.asin(((1.74625 * Math.sin(uLeftf)) + (1.74625 * Math.sin(uRightf))) / (5.635)));
        double uMidTiltf = (Math.asin(((1.74625 * Math.sin(uRightf)) - (1.74625 * Math.sin(uLeftf))) / (5)));
        double lMidPrisf = (Math.asin(((1.74625 * Math.sin(lLeftf)) + (1.74625 * Math.sin(lRightf))) / (5.635)));
        double lMidTiltf = (Math.asin(((1.74625 * Math.sin(lRightf)) - (1.74625 * Math.sin(lLeftf))) / (5)));
        mouth[0] = (byte) uLeft;
        mouth[1] = (byte) lLeft;
        mouth[2] = (byte) uRight;
        mouth[3] = (byte) lRight;
        return sendMsg("MISPKG {Name head} {Link 8} {Value " + uLeftf + "} {Link 9} {Value " + uMidPrisf + "} {Link 10} {Value " + uMidTiltf + "} {Link 11} {Value " + uRightf + "} {Link 12} {Value " + lLeftf + "} {Link 13} {Value " + lMidPrisf + "} {Link 14} {Value " + lMidTiltf + "} {Link 15} {Value " + lRightf + "}");
    }

    public boolean moveHead(int pitch, int yaw, int speed) throws RemoteException {
        return moveHead(pitch, yaw, speed, speed);
    }

    public boolean moveHead(int pitch, int yaw, int pSpeed, int ySpeed) throws RemoteException {
        return CMServer.moveHead(pitch, yaw, pSpeed, ySpeed);
    }

    public boolean moveLeftArm(int ePitch, int sYaw, int sPitch, int speed) throws RemoteException {
        return moveLeftArm(ePitch, sYaw, sPitch, speed, speed, speed);
    }

    public boolean moveLeftArm(int ePitch, int sYaw, int sPitch, int ePitSpeed, int sYawSpeed, int sPitSpeed) throws RemoteException {
        return moveArm(false, ePitch, sYaw, sPitch, ePitSpeed, sYawSpeed, sPitSpeed);
    }

    public boolean moveRightArm(int ePitch, int sYaw, int sPitch, int speed) throws RemoteException {
        return moveRightArm(ePitch, sYaw, sPitch, speed, speed, speed);
    }

    public boolean moveRightArm(int ePitch, int sYaw, int sPitch, int ePitSpeed, int sYawSpeed, int sPitSpeed) throws RemoteException {
        return moveArm(true, ePitch, sYaw, sPitch, ePitSpeed, sYawSpeed, sPitSpeed);
    }

    public boolean moveArm(boolean Right, int ePitch, int sYaw, int sPitch, int speed) throws RemoteException {
        return moveArm(Right, ePitch, sYaw, sPitch, speed, speed, speed);
    }

    public boolean moveArm(boolean Right, int ePitch, int sYaw, int sPitch, int ePitSpeed, int sYawSpeed, int sPitSpeed) throws RemoteException {
        return CMServer.moveArm(Right, ePitch, sYaw, sPitch, ePitSpeed, sYawSpeed, sPitSpeed);
    }

    public boolean moveEyes(int left, int right, int pitch, int speed) throws RemoteException {
        return moveEyes(left, right, pitch, speed, speed, speed);
    }

    public boolean moveEyes(int left, int right, int pitch, int lSpeed, int rSpeed, int pSpeed) throws RemoteException {
        return CMServer.moveEyes(left, right, pitch, lSpeed, rSpeed, pSpeed);
    }

    public boolean moveEyeBrows(int left, int right, int speed) throws RemoteException {
        return moveEyeBrows(left, right, speed, speed);
    }

    public boolean moveEyeBrows(int left, int right, int lSpeed, int rSpeed) throws RemoteException {
        return CMServer.moveEyeBrows(left, right, lSpeed, rSpeed);
    }

    public boolean moveMouth(int uLeft, int lLeft, int uRight, int lRight, int speed) throws RemoteException {
        return moveMouth(uLeft, lLeft, uRight, lRight, speed, speed, speed, speed);
    }

    public boolean moveMouth(int uLeft, int lLeft, int uRight, int lRight, int uLSpeed, int lLSpeed, int uRSpeed, int lRSpeed) throws RemoteException {
        return CMServer.moveMouth(uLeft, lLeft, uRight, lRight, uLSpeed, lLSpeed, uRSpeed, lRSpeed);
    }

    public void waveLeftHand(int n) throws RemoteException {
        System.out.println("Something left hand");
        moveLeftArm(5, 5, 5);
        Sleep(500);
        for (int i = 0; i < n; i++) {
            moveLeftArm(90, 90, 90);
            Sleep(500);
            moveLeftArm(80, 90, 90);
            Sleep(500);
        }
        moveLeftArm(90, 90, 90);
        Sleep(500);
        moveLeftArm(5, 5, 5);
    }

    public void waveRightHand(int n) throws RemoteException {
        System.out.println("Something right hand");
        moveRightArm(5, 5, 5);
        Sleep(500);
        for (int i = 0; i < n; i++) {
            moveRightArm(90, 90, 90);
            Sleep(500);
            moveRightArm(80, 90, 90);
            Sleep(500);
        }
        moveRightArm(90, 90, 90);
        Sleep(500);
        moveRightArm(5, 5, 5);
    }

    public boolean setCamera(int view) throws RemoteException {
        if (view == 1) {
            return sendMsg("SET {Type Viewports} {Config 0} {Viewport1 TalkingHead}");
        } else if (view == 2) {
            return sendMsg("SET {Type Viewports} {Config 1} {Viewport1 LBumbleCamera} {Viewport2 RBumbleCamera} {Viewport3 Disable} {Viewport4 Disable}");
        } else if (view == 3) {
            return sendMsg("SET {Type Viewports} {Config 1} {Viewport1 LEyeCamera} {Viewport2 REyeCamera} {Viewport3 Disable} {Viewport4 Disable}");
        } else if (view == 4) {
            return sendMsg("SET {Type Viewports} {Config 1} {Viewport1 LEyeCamera} {Viewport2 REyeCamera} {Viewport3 LBumbleCamera} {Viewport4 RBumbleCamera}");
        }
        return false;
    }

    public void openMouth() {
        sendMsg("MISPKG {Name head} {Link 8} {Value 0.1088190451} {Link 9} {Value 0.0911087444} {Link 10} {Value 0.025} {Link 11} {Value 0.1688190451} {Link 12} {Value -0.3490658504} {Link 13} {Value -0.213600206} {Link 14} {Value 0} {Link 15} {Value -0.3490658504}");
    }

    public void closeMouth() {
        sendMsg("MISPKG {Name head} {Link 8} {Value -0.085} {Link 9} {Value -0.055} {Link 10} {Value 0.02} {Link 11} {Value -0.065} {Link 12} {Value -0.15} {Link 13} {Value -0.085} {Link 14} {Value 0} {Link 15} {Value -0.15}");
    }

    public void restoreMouth() {
        double uLeftf = (((double) (mouth[0] - 45) * Math.PI) / 180);
        double lLeftf = (((double) (mouth[1] - 45) * Math.PI) / 180);
        double uRightf = (((double) (mouth[2] - 45) * Math.PI) / 180);
        double lRightf = (((double) (mouth[3] - 45) * Math.PI) / 180);
        double uMidPrisf = (Math.asin(((1.74625 * Math.sin(uLeftf)) + (1.74625 * Math.sin(uRightf))) / (5.635)));
        double uMidTiltf = (Math.asin(((1.74625 * Math.sin(uRightf)) - (1.74625 * Math.sin(uLeftf))) / (2.375)));
        double lMidPrisf = (Math.asin(((1.74625 * Math.sin(lLeftf)) + (1.74625 * Math.sin(lRightf))) / (5.635)));
        double lMidTiltf = (Math.asin(((1.74625 * Math.sin(lRightf)) - (1.74625 * Math.sin(lLeftf))) / (2.375)));
        sendMsg("MISPKG {Name head} {Link 8} {Value " + uLeftf + "} {Link 9} {Value " + uMidPrisf + "} {Link 10} {Value " + uMidTiltf + "} {Link 11} {Value " + uRightf + "} {Link 12} {Value " + lLeftf + "} {Link 13} {Value " + lMidPrisf + "} {Link 14} {Value " + lMidTiltf + "} {Link 15} {Value " + lRightf + "}");
    }

    public boolean frown() {
        return sendMsg("MISPKG {Name head} {Link 3} {Value 0.2890658504} {Link 4} {Value 0.2890658504} {Link 8} {Value 0.1088190451} {Link 9} {Value 0.0911087444} {Link 10} {Value 0.025} {Link 11} {Value 0.1688190451} {Link 12} {Value 0.2420201433} {Link 13} {Value 0.1532275784} {Link 14} {Value 0} {Link 15} {Value 0.2420201433}");
    }

    public boolean scowl() {
        return sendMsg("MISPKG {Name head} {Link 3} {Value -0.3490658504} {Link 4} {Value -0.3490658504} {Link 8} {Value 0.1088190451} {Link 9} {Value 0.0911087444} {Link 10} {Value 0.025} {Link 11} {Value 0.1688190451} {Link 12} {Value 0.2420201433} {Link 13} {Value 0.1532275784} {Link 14} {Value 0} {Link 15} {Value 0.2420201433}");
    }

    public boolean smile() {
        return sendMsg("MISPKG {Name head} {Link 3} {Value 0.1036481777} {Link 4} {Value 0.1036481777} {Link 8} {Value -0.2490658504} {Link 9} {Value -0.183600206} {Link 10} {Value -0.025} {Link 11} {Value -0.2990658504} {Link 12} {Value -0.3490658504} {Link 13} {Value -0.213600206} {Link 14} {Value 0} {Link 15} {Value -0.3490658504}");
    }

    public boolean neutralFace() {
        return sendMsg("MISPKG {Name head} {Link 3} {Value 0.1036481777} {Link 4} {Value 0.1036481777} {Link 8} {Value -0.085} {Link 9} {Value -0.05} {Link 10} {Value 0.0125} {Link 11} {Value -0.065} {Link 12} {Value -0.15} {Link 13} {Value -0.085} {Link 14} {Value 0} {Link 15} {Value -0.15}");
    }

    public USARSimServerImpl() throws RemoteException {
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
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = socket.getOutputStream();
        } catch (IOException ioe) {
            System.err.println(prg + ": Error creating reader/writer: " + ioe);
            System.exit(-1);
        }
        if (reddy) {
            name = "CRAMER";
        } else {
            name = "Pioneer";
        }
        u = new Updater();
        u.start();
        getStartPoses();
        if (autoPose) {
            if (startPoses.isEmpty()) {
                System.out.println("Failed to Retrieve StartPose Data");
            } else {
                if (startPoses.containsKey(desiredPose)) {
                    double[] poseLoc = startPoses.get(desiredPose);
                    initx = poseLoc[0];
                    inity = poseLoc[1];
                    initz = poseLoc[2];
                    initt = poseLoc[3];
                } else {
                    System.out.println(desiredPose + " is Not a Possible Pose");
                }
            }
        }
        sendMsg("INIT {ClassName USARBot." + ((reddy) ? "Cramer" : "StereoP2AT") + "} {Name " + name + "} {Location " + initx + "," + -(inity) + "," + initz + "} {Rotation 0.0,0.0," + -initt + "}");
        sendMsg("GETGEO {Type Robot}");
        sendMsg("GETCONF {Type Robot}");
        CMServer = new ContMotionServer();
        initialize();
        if (runTest) test();
        if (doDemo) {
            smile();
            Sleep(5000);
            try {
                moveRightArm(90, 90, 90);
            } catch (RemoteException re) {
            }
            Sleep(1500);
            WaveSpeak("com/reddy/wav/kramer1.wav");
            try {
                moveHead(80, 105);
                moveLeftArm(5, 10, 90);
                moveRightArm(5, 5, 5);
                moveMouth(45, 45, 45, 45);
                moveEyeBrows(55, 55);
            } catch (RemoteException re) {
            }
            Sleep(1500);
            WaveSpeak("com/reddy/wav/kramer2.wav");
            try {
                moveHead(90, 90);
                moveLeftArm(90, 10, 90);
                moveRightArm(5, 5, 5);
            } catch (RemoteException re) {
            }
            scowl();
            Sleep(1500);
            WaveSpeak("com/reddy/wav/kramer3.wav");
            Sleep(5000);
            System.exit(0);
        }
        if (doSmile) smile();
        if (doFrown) scowl();
        if (doHRIvideo) {
            try {
                Sleep(7000);
                smile();
                Sleep(1000);
                WaveSpeak("com/reddy/wav/hri08video01.wav");
                Sleep(6000);
                moveMouth(45, 45, 45, 45);
                moveHead(70, 90);
                Sleep(2000);
                moveHead(90, 90);
                Sleep(2000);
                moveEyeBrows(15, 15, 5);
                Sleep(8000);
                moveEyeBrows(30, 30);
                WaveSpeak("com/reddy/wav/hri08video02.wav");
                Sleep(4000);
                moveEyeBrows(45, 45);
                WaveSpeak("com/reddy/wav/hri08video03.wav");
                moveLeftArm(15, 5, 90);
                Sleep(1500);
                WaveSpeak("com/reddy/wav/hri08video04.wav");
                moveLeftArm(15, 30, 5);
                Sleep(1500);
                scowl();
                WaveSpeak("com/reddy/wav/hri08video05.wav");
                moveLeftArm(5, 5, 5);
                Sleep(1500);
                WaveSpeak("com/reddy/wav/hri08video06.wav");
                Sleep(500);
                WaveSpeak("com/reddy/wav/hri08video07.wav");
                Sleep(500);
                WaveSpeak("com/reddy/wav/hri08video08.wav");
            } catch (RemoteException re) {
            }
        }
    }

    public void getStartPoses() {
        Sleep(250);
        sendMsg("GETSTARTPOSES");
        Sleep(1000);
    }

    /**
	 * Get a message from the simulator.
	 * @return a String containing the message.
	 */
    public String recvMsg() {
        String msg = null;
        try {
            while (in.read() < 1) ;
            msg = new String(in.readLine());
        } catch (Exception e) {
            System.err.println(prg + ": Error reading from socket: " + e);
            msg = null;
        }
        return msg;
    }

    /**
	 * Send a message to the simulator.
	 * @param msg a String containing the message.
	 * @return true if successfully sent, false otherwise
	 */
    public boolean sendMsg(String msg) {
        msg = msg + "\r\n";
        try {
            synchronized (out) {
                Sleep(5);
                out.write(msg.getBytes(), 0, msg.length());
            }
            return true;
        } catch (Exception e) {
            System.err.println(prg + ": Error sending to socket: " + e);
        }
        return false;
    }

    /**
	 * Parse a message from the simulator.
	 * @param msg a String containing the mesage.
	 */
    public void parseMsg(String msg) {
        String tmp;
        if (msg != null) {
            if (msg.startsWith("EN")) {
                StringTokenizer st = new StringTokenizer(msg.substring(3), "{ }");
                tmp = st.nextToken();
                if (tmp.equals("Time")) {
                    tmp = st.nextToken();
                    try {
                        curTime = Double.parseDouble(tmp);
                    } catch (Exception e) {
                        System.err.println(prg + ": error parsing double " + tmp);
                    }
                    while (st.hasMoreTokens()) {
                        tmp = st.nextToken();
                        if (tmp.equals("Type")) {
                            tmp = st.nextToken();
                            if (tmp.equals("RangeScanner")) {
                                int a = 0;
                                for (int i = 0; i < 8; i++) {
                                    tmp = st.nextToken();
                                }
                                StringTokenizer st2 = new StringTokenizer(tmp, ",");
                                safeLeft = safeFront = safeRight = true;
                                openLeft = openFront = openRight = true;
                                while (st2.hasMoreTokens()) {
                                    tmp = st2.nextToken();
                                    try {
                                        laser[a] = Double.parseDouble(tmp);
                                        if (a < 60) {
                                            if (laser[a] <= CRITICALDIST) {
                                                safeRight = false;
                                                System.out.println("Obstacle to right.");
                                            }
                                            openRight = openRight && (laser[a] > CRITICALDIST * 1.25);
                                        } else if (a < 121) {
                                            if (laser[a] <= CRITICALDIST) {
                                                safeFront = false;
                                                System.out.println("Obstacle to front.");
                                            }
                                            openFront = openFront && (laser[a] > CRITICALDIST * 1.25);
                                        } else {
                                            if (laser[a] <= CRITICALDIST) {
                                                safeLeft = false;
                                                System.out.println("Obstacle to left.");
                                            }
                                            openLeft = openLeft && (laser[a] > CRITICALDIST * 1.25);
                                        }
                                    } catch (Exception e) {
                                        System.err.println(prg + ": error parsing double " + tmp + ": " + e);
                                    }
                                    a++;
                                }
                                if (!safeFront && !inDanger) {
                                    try {
                                        stop();
                                        inDanger = true;
                                    } catch (RemoteException e) {
                                        System.err.println(prg + ": couldn't stop : " + e);
                                    }
                                }
                            } else if (tmp.equals("GroundTruth")) {
                                int a = 0;
                                for (int i = 0; i < 3; i++) {
                                    oldLoc[i] = curLoc[i];
                                }
                                for (int i = 0; i < 4; i++) {
                                    tmp = st.nextToken();
                                }
                                StringTokenizer st2 = new StringTokenizer(tmp, ",");
                                while (st2.hasMoreTokens()) {
                                    tmp = st2.nextToken();
                                    try {
                                        curLoc[a] = Double.parseDouble(tmp);
                                    } catch (Exception e) {
                                        System.err.println(prg + ": error parsing double " + tmp + ": " + e);
                                    }
                                    a++;
                                }
                                double TVBuffer = 0;
                                for (int i = 0; i < 3; i++) {
                                    TVBuffer = TVBuffer + ((curLoc[i] - oldLoc[i]) * (curLoc[i] - oldLoc[i]));
                                }
                                curTV = (Math.sqrt(TVBuffer)) / (curTime - oldTime);
                                a = 0;
                                for (int i = 0; i < 3; i++) {
                                    oldRot[i] = curRot[i];
                                }
                                for (int i = 0; i < 2; i++) {
                                    tmp = st.nextToken();
                                }
                                StringTokenizer st3 = new StringTokenizer(tmp, ",");
                                while (st3.hasMoreTokens()) {
                                    tmp = st3.nextToken();
                                    try {
                                        curRot[a] = ((Double.parseDouble(tmp)) % (2 * Math.PI));
                                    } catch (Exception e) {
                                        System.err.println(prg + ": error parsing double " + tmp + ": " + e);
                                    }
                                    a++;
                                }
                                curRot[2] = -curRot[2];
                                curRV = (curRot[2] - oldRot[2]) / (curTime - oldTime);
                                curLoc[1] = -curLoc[1];
                                oldTime = curTime;
                            }
                        }
                    }
                }
            } else if (msg.startsWith("FO")) {
                StringTokenizer st = new StringTokenizer(msg.substring(3), "{ }");
                tmp = st.nextToken();
                if (tmp.equals("StartPoses")) {
                    System.out.println("Received Pose Data");
                    for (int i = 0; i < 2; i++) {
                        tmp = st.nextToken();
                    }
                    while (st.hasMoreTokens()) {
                        String pose = tmp;
                        int i = 0;
                        int a = 3;
                        double[] poseLoc = new double[6];
                        for (int t = 0; t < 2; t++) {
                            tmp = st.nextToken();
                            StringTokenizer loc = new StringTokenizer(tmp, ",");
                            while (i < a) {
                                String comp = loc.nextToken();
                                poseLoc[i] = Double.parseDouble(comp);
                                i++;
                            }
                            a += 3;
                        }
                        startPoses.put(pose, poseLoc);
                    }
                    System.out.println("Pose Data Parsing Successful: " + startPoses.keySet());
                }
            } else if (msg.startsWith("EO")) {
                System.out.println("Received Geometrical Data");
                StringTokenizer st = new StringTokenizer(msg.substring(3), "{ }");
                while (st.hasMoreTokens()) {
                    tmp = st.nextToken();
                    if (tmp.equals("WheelRadius")) {
                        tmp = st.nextToken();
                        try {
                            wheelRad = Double.parseDouble(tmp);
                        } catch (Exception e) {
                            System.err.println(prg + ": error parsing double " + tmp);
                        }
                    } else if (tmp.equals("WheelSeparation")) {
                        tmp = st.nextToken();
                        try {
                            wheelSep = Double.parseDouble(tmp);
                        } catch (Exception e) {
                            System.err.println(prg + ": error parsing double " + tmp);
                        }
                    } else if (tmp.equals("WheelBase")) {
                        tmp = st.nextToken();
                        try {
                            wheelBase = Double.parseDouble(tmp);
                        } catch (Exception e) {
                            System.err.println(prg + ": error parsing double " + tmp);
                        }
                    }
                }
            } else if (msg.startsWith("ONF")) {
                System.out.println("Received Configuration Data");
                StringTokenizer st = new StringTokenizer(msg.substring(4), "{ }");
                while (st.hasMoreTokens()) {
                    tmp = st.nextToken();
                    if (tmp.equals("Mass")) {
                        tmp = st.nextToken();
                        try {
                            mass = Double.parseDouble(tmp);
                        } catch (Exception e) {
                            System.err.println(prg + ": error parsing double " + tmp);
                        }
                    }
                }
            } else if (msg.startsWith("ISSTA")) {
                StringTokenizer st = new StringTokenizer(msg.substring(7), "{ }");
                while (st.hasMoreTokens()) {
                    tmp = st.nextToken();
                    if (tmp.equalsIgnoreCase("LeftArm")) {
                        for (int i = 0; i < 4; i++) {
                            tmp = st.nextToken();
                        }
                        curLeftArm[2] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curLeftArm[1] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curLeftArm[0] = Double.parseDouble(tmp);
                        updated[0] += 1;
                    } else if (tmp.equalsIgnoreCase("RightArm")) {
                        for (int i = 0; i < 4; i++) {
                            tmp = st.nextToken();
                        }
                        curRightArm[2] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curRightArm[1] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curRightArm[0] = Double.parseDouble(tmp);
                        updated[1] += 1;
                    }
                    if (tmp.equalsIgnoreCase("Head")) {
                        for (int i = 0; i < 4; i++) {
                            tmp = st.nextToken();
                        }
                        curHead[0] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curHead[1] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curEyebrows[0] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curEyebrows[1] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curEyes[2] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curEyes[0] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curEyes[1] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curMouth[0] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curMouthMiddle[0] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curMouthMiddle[1] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curMouth[1] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curMouth[2] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curMouthMiddle[2] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curMouthMiddle[3] = Double.parseDouble(tmp);
                        for (int i = 0; i < 6; i++) {
                            tmp = st.nextToken();
                        }
                        curMouth[3] = Double.parseDouble(tmp);
                        updated[2] += 1;
                    }
                }
            }
        }
    }

    /**
	 * The <code>Updater</code> receives periodic messages to the simulator.
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
                    msg = recvMsg();
                    parseMsg(msg);
                    Sleep(10);
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

    private class ContMotionServer {

        CheckThread[] curHeadCommand = new CheckThread[3];

        CheckThread[] curLeftArmCommand = new CheckThread[3];

        CheckThread[] curRightArmCommand = new CheckThread[3];

        CheckThread[] curEyesCommand = new CheckThread[3];

        CheckThread[] curEyebrowsCommand = new CheckThread[2];

        CheckThread[] curMouthCommand = new CheckThread[4];

        CheckThread[] curMouthMiddleCommand = new CheckThread[4];

        private class CheckThread extends Thread {

            double[] misPkg;

            String missionPackage;

            String name;

            int joint;

            int link;

            double dest;

            int sign;

            int time;

            double speed;

            public CheckThread(String name, int joint, double dest, double speed, int sign) {
                this.name = name;
                this.joint = joint;
                this.dest = dest;
                this.sign = sign;
                this.speed = speed;
                if (name.equals("Head")) {
                    missionPackage = "head";
                    misPkg = curHead;
                    link = joint + 1;
                } else if (name.equals("Eyes")) {
                    missionPackage = "head";
                    misPkg = curEyes;
                    switch(joint) {
                        case 0:
                            link = 6;
                            break;
                        case 1:
                            link = 7;
                            break;
                        case 2:
                            link = 5;
                            break;
                    }
                } else if (name.equals("Eyebrows")) {
                    missionPackage = "head";
                    misPkg = curEyebrows;
                    link = joint + 3;
                } else if (name.equals("Mouth")) {
                    missionPackage = "head";
                    misPkg = curMouth;
                    switch(joint) {
                        case 0:
                            link = 8;
                            break;
                        case 1:
                            link = 11;
                            break;
                        case 2:
                            link = 12;
                            break;
                        case 3:
                            link = 15;
                            break;
                    }
                } else if (name.equals("LeftArm")) {
                    missionPackage = name;
                    misPkg = curLeftArm;
                    link = (2 - joint) + 1;
                } else if (name.equals("RightArm")) {
                    missionPackage = name;
                    misPkg = curRightArm;
                    link = (2 - joint) + 1;
                } else {
                    missionPackage = "head";
                    misPkg = curMouthMiddle;
                    switch(joint) {
                        case 0:
                            link = 9;
                            break;
                        case 1:
                            link = 10;
                            break;
                        case 2:
                            link = 13;
                            break;
                        case 3:
                            link = 14;
                            break;
                    }
                }
            }

            public void run() {
                try {
                    if (sign != 0) {
                        time = (int) (1000 * (Math.abs((misPkg[joint] - dest) / speed)));
                        this.sleep(time);
                        sendMsg("MISPKG {Name " + this.missionPackage + "} {Link " + this.link + "} {Value 0.0} {Order 1}");
                    }
                } catch (InterruptedException e) {
                    System.out.println(missionPackage + " command interrupted by new command.");
                    sendMsg("MISPKG {Name " + this.missionPackage + "} {Link " + this.link + "} {Value 0.0} {Order 1}");
                    return;
                }
            }
        }

        public void waitForUpdate(int pkg) {
            int k = 0;
            int interval = 5;
            updated[pkg] = 0;
            while (updated[pkg] < 1) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException ie) {
                    System.out.println("Interrupted while waiting for update(s)");
                }
                k++;
            }
            System.out.println("waited for " + (k * interval) + " ms.");
        }

        public int[] signCheck(double[] misPkg, double[] desPos) {
            int length = desPos.length;
            int[] outSign = new int[length];
            for (int i = 0; i < length; i++) {
                if (Math.abs(desPos[i] - misPkg[i]) < threshold) {
                    outSign[i] = 0;
                } else if (desPos[i] < misPkg[i]) {
                    outSign[i] = -1;
                } else {
                    outSign[i] = 1;
                }
            }
            return outSign;
        }

        public double[] convertToRadians(int[] angles) {
            int length = angles.length;
            double[] outAngles = new double[length];
            for (int i = 0; i < length; i++) {
                outAngles[i] = (((double) angles[i] * Math.PI) / 180);
            }
            return outAngles;
        }

        public double[] mapSpeeds(String name, int[] speeds, int[] signs) {
            int length = speeds.length;
            double[] outSpeeds = new double[length];
            double[] coeffs = new double[8];
            double x;
            boolean test = false;
            for (int i = 0; i < length; i++) {
                if (name.equals("LeftArm")) {
                    if (i == 0) {
                        double[] temp = { 0.000000000000729838, 0.126718, -0.00000000000000501899, -0.000169366, 0.00000000000000000949764, 0.000000118885, -0.00000000000000000000486217, -0.0000000000235884 };
                        coeffs = temp;
                    } else if (i == 1) {
                        double[] temp = { 0.0180649, 0.11129, -0.00120205, -0.000129685, 0.00000200818, 0.000000116973, -0.00000000103199, -0.0000000000489066 };
                        coeffs = temp;
                    } else {
                        double[] temp = { 0.0243997, 0.110996, -0.000568493, -0.0000277491, -0.000000312085, -0.0000000350602, 0.000000000432061, 0.000000000027166 };
                        coeffs = temp;
                    }
                } else if (name.equals("RightArm")) {
                    if (i == 0) {
                        double[] temp = { 0.000000000000394917, 0.111668, -0.00000000000000216186, -0.0000938411, 0.000000000000000004278, -0.00000000130719, -0.00000000000000000000276602, 0.0000000000373483 };
                        coeffs = temp;
                    } else if (i == 1) {
                        double[] temp = { 0.00300068, 0.095205, 0.00102549, -0.0000292124, -0.00000404018, -0.0000000268662, 0.0000000030891, 0.0000000000174947 };
                        coeffs = temp;
                    } else if (i == 2) {
                        double[] temp = { 0, 0, 0, 0, 0, 0, 0, 0 };
                        coeffs = temp;
                    }
                } else if (name.equals("Head")) {
                    if (i == 0) {
                        double[] temp = { -0.0000000036152, 0.0892562, 0.0000000000472915, -0.000167825, -0.00000000000012101, 0.000000220021, 0.0000000000000000815064, -0.000000000104968 };
                        coeffs = temp;
                    } else {
                        double[] temp = { 0.0000000346369, 0.0953132, -0.000000000330388, -0.000146721, 0.000000000000761296, 0.00000018968, -0.000000000000000487538, -0.000000000087277 };
                        coeffs = temp;
                    }
                } else if (name.equals("Eyes")) {
                    double[] temp = { 0.000000000000227398, 0.0369691, -0.00000000000000136703, -0.0000654958, 0.000000000000000000678222, 0.000000108978, 0.00000000000000000000151735, -0.000000000058971 };
                    coeffs = temp;
                } else if (name.equals("Eyebrows")) {
                    double[] temp = { -0.00000000126211, 0.0315154, 0.0000000000133489, 0.0000327479, -0.0000000000000319906, -0.0000000544892, 0.000000000000208974, 0.0000000000294855 };
                    coeffs = temp;
                } else if (name.equals("Mouth")) {
                    double[] temp = { -3.87158, -0.320437, 0.0372638, 0.00234501, -0.0000851736, -0.00000450279, 0.0000000541287, 0.00000000259135 };
                    coeffs = temp;
                } else {
                    test = true;
                }
                x = (double) (speeds[i] * signs[i]);
                if (test) {
                    outSpeeds[i] = (double) (signs[i] * speeds[i]) / 100;
                } else if (x == 0) {
                    outSpeeds[i] = 0;
                } else {
                    outSpeeds[i] = coeffs[0] + coeffs[1] * x + coeffs[2] * (Math.pow(x, 2)) + coeffs[3] * (Math.pow(x, 3)) + coeffs[4] * (Math.pow(x, 4)) + coeffs[5] * (Math.pow(x, 5)) + coeffs[6] * (Math.pow(x, 6)) + coeffs[7] * (Math.pow(x, 7));
                }
            }
            return outSpeeds;
        }

        public boolean moveArm(final boolean right, int ePitch, int sYaw, int sPitch, int ePitSpeed, int sYawSpeed, int sPitSpeed) {
            if (ePitch < 0) ePitch = 0; else if (ePitch > 90) ePitch = 90;
            if (sYaw < 0) sYaw = 0; else if (sYaw > 90) sYaw = 90;
            if (sPitch < 0) sPitch = 0; else if (sPitch > 180) sPitch = 180;
            System.out.println(prg + ": move" + ((right) ? "RightArm" : "LeftArm") + " " + right + " " + ePitch + " " + sYaw + " " + sPitch + " " + ePitSpeed + " " + sYawSpeed + " " + sPitSpeed);
            int[] degAngles = { ePitch, sYaw, sPitch };
            int[] intSpeeds = { ePitSpeed, sYawSpeed, sPitSpeed };
            final double[] angles = convertToRadians(degAngles);
            final int[] signs = signCheck(((right) ? curRightArm : curLeftArm), angles);
            final double[] speeds = mapSpeeds(((right) ? "RightArm" : "LeftArm"), intSpeeds, signs);
            new Thread() {

                public void run() {
                    boolean wait = false;
                    for (int i = 0; i < 3; i++) {
                        try {
                            if (((right) ? curRightArmCommand : curLeftArmCommand)[i].isAlive()) {
                                ((right) ? curRightArmCommand : curLeftArmCommand)[i].interrupt();
                                wait = true;
                            }
                        } catch (NullPointerException e) {
                        }
                        ((right) ? curRightArmCommand : curLeftArmCommand)[i] = new CheckThread(((right) ? "RightArm" : "LeftArm"), i, angles[i], speeds[i], signs[i]);
                    }
                    if (wait) {
                        waitForUpdate((right) ? 1 : 0);
                    }
                    sendMsg("MISPKG {Name " + ((right) ? "RightArm" : "LeftArm") + "} {Link 1} {Value " + speeds[2] + "} {Order 1} {Link 2} {Value " + speeds[1] + "} {Order 1} {Link 3} {Value " + speeds[0] + "} {Order 1}");
                    for (int i = 0; i < 3; i++) {
                        ((right) ? curRightArmCommand : curLeftArmCommand)[i].start();
                    }
                }
            }.start();
            return true;
        }

        public boolean moveHeadOld(int pitch, int yaw, int pSpeed, int ySpeed) {
            System.out.println(prg + ": moveHead " + pitch + " " + yaw + " " + pSpeed + " " + ySpeed);
            if (pitch < 70) pitch = 70; else if (pitch > 110) pitch = 110;
            if (yaw < 50) yaw = 50; else if (yaw > 130) yaw = 130;
            head[0] = (byte) pitch;
            head[1] = (byte) yaw;
            int[] degAngles = { (pitch - 90), (yaw - 90) };
            int[] intSpeeds = { pSpeed, ySpeed };
            final double[] angles = convertToRadians(degAngles);
            final int[] signs = signCheck(curHead, angles);
            final double[] speeds = mapSpeeds("Head", intSpeeds, signs);
            new Thread() {

                public void run() {
                    boolean wait = false;
                    for (int i = 0; i < 2; i++) {
                        try {
                            if (curHeadCommand[i].isAlive()) {
                                curHeadCommand[i].interrupt();
                                wait = true;
                            }
                        } catch (NullPointerException e) {
                        }
                        curHeadCommand[i] = new CheckThread("Head", i, angles[i], speeds[i], signs[i]);
                    }
                    if (wait) {
                        waitForUpdate(2);
                    }
                    sendMsg("MISPKG {Name head} {Link 1} {Value " + speeds[0] + "} {Order 1} {Link 2} {Value " + speeds[1] + "} {Order 1}");
                    for (int i = 0; i < 2; i++) {
                        curHeadCommand[i].start();
                    }
                }
            }.start();
            return true;
        }

        public boolean moveHead(int pitch, int yaw, int pSpeed, int ySpeed) {
            if (pitch < 70) pitch = 70; else if (pitch > 110) pitch = 110;
            if (yaw < 50) yaw = 50; else if (yaw > 130) yaw = 130;
            head[0] = (byte) pitch;
            head[1] = (byte) yaw;
            int[] degAngles = { (pitch - 90), (yaw - 90) };
            int[] intSpeeds = { pSpeed, ySpeed };
            final double[] angles = convertToRadians(degAngles);
            final int[] signs = signCheck(curHead, angles);
            final double[] speeds = mapSpeeds("Head", intSpeeds, signs);
            final Long myID = System.currentTimeMillis();
            new Thread() {

                public void run() {
                    System.out.println(prg + ": moveHead " + myID + " " + (int) (Math.toDegrees(angles[0]) + 90) + " " + (int) (Math.toDegrees(angles[1]) + 90));
                    int j1 = 1, j2 = 2;
                    long t0 = 0, t1 = 0, t2 = 0;
                    double p, y;
                    int proceed = 0;
                    int waits = 0;
                    synchronized (headID) {
                        if (headID < myID) {
                            System.out.println("moveHead " + myID + " interrupting " + headID);
                            headID = myID;
                            sendMsg("MISPKG {Name head} {Link 1} {Value 0.0} {Order 1} {Link 2} {Value 0.0} {Order 1}");
                            p = curHead[0];
                            y = curHead[1];
                        } else {
                            return;
                        }
                    }
                    waitForUpdate(2);
                    synchronized (headID) {
                        if (headID == myID) {
                            t0 = (int) (1000 * (Math.abs((curHead[0] - angles[0]) / speeds[0])));
                            t1 = (int) (1000 * (Math.abs((curHead[1] - angles[1]) / speeds[1])));
                            if (t0 < 0) {
                                t0 = 0L;
                                speeds[0] = 0.0;
                            }
                            if (t1 < 0) {
                                t1 = 0L;
                                speeds[1] = 0.0;
                            }
                            if (t0 == 0L && t1 == 0L) {
                                return;
                            } else if (t0 == 0L) {
                                t2 = 0L;
                                j1 = 2;
                                j2 = 1;
                            } else if (t1 == 0L) {
                                t1 = t0;
                                t2 = 0L;
                            } else if (t1 > t0) {
                                t2 = t1;
                                t1 = t0;
                            } else {
                                t2 = t0;
                                j1 = 2;
                                j2 = 1;
                            }
                            t2 += (System.currentTimeMillis());
                            sendMsg("MISPKG {Name head} {Link 1} {Value " + speeds[0] + "} {Order 1} {Link 2} {Value " + speeds[1] + "} {Order 1}");
                        } else {
                            return;
                        }
                    }
                    Sleep(t1);
                    synchronized (headID) {
                        if (headID == myID) {
                            if ((t2 - System.currentTimeMillis()) < 15) {
                                sendMsg("MISPKG {Name head} {Link 1} {Value 0.0} {Order 1} {Link 2} {Value 0.0} {Order 1}");
                                System.out.println(myID + " Preemptively stopping joint " + j2 + " (" + (t2 - System.currentTimeMillis()) + ")");
                                return;
                            }
                            sendMsg("MISPKG {Name head} {Link " + j1 + "} {Value 0.0} {Order 1}");
                        } else {
                            return;
                        }
                    }
                    t2 -= System.currentTimeMillis();
                    Sleep(t2);
                    synchronized (headID) {
                        if (headID == myID) {
                            sendMsg("MISPKG {Name head} {Link 1} {Value 0.0} {Order 1} {Link 2} {Value 0.0} {Order 1}");
                        } else {
                            return;
                        }
                    }
                    System.out.println(prg + ": moveHead " + myID + " " + (int) (Math.toDegrees(angles[0]) + 90) + " " + (int) (Math.toDegrees(angles[1]) + 90) + " ended up at " + (int) (Math.toDegrees(curHead[0]) + 90) + " " + (int) (Math.toDegrees(curHead[1]) + 90));
                }
            }.start();
            return true;
        }

        public boolean moveEyes(int left, int right, int pitch, int lSpeed, int rSpeed, int pSpeed) {
            System.out.println(prg + ": moveEyes " + left + " " + right + " " + pitch + " " + lSpeed + " " + rSpeed + " " + pSpeed);
            if (left < 26) left = 26; else if (left > 64) left = 64;
            if (right < 26) right = 26; else if (right > 64) right = 64;
            if (pitch < 26) pitch = 26; else if (pitch > 64) pitch = 64;
            left += 10;
            eyes[0] = (byte) left;
            eyes[1] = (byte) right;
            eyes[2] = (byte) pitch;
            int[] degAngles = { (45 - left), (45 - right), (pitch - 45) };
            int[] intSpeeds = { lSpeed, rSpeed, pSpeed };
            final double[] angles = convertToRadians(degAngles);
            final int[] signs = signCheck(curEyes, angles);
            final double[] speeds = mapSpeeds("Eyes", intSpeeds, signs);
            new Thread() {

                public void run() {
                    boolean wait = false;
                    for (int i = 0; i < 3; i++) {
                        try {
                            if (curEyesCommand[i].isAlive()) {
                                curEyesCommand[i].interrupt();
                                wait = true;
                            }
                        } catch (NullPointerException e) {
                        }
                        curEyesCommand[i] = new CheckThread("Eyes", i, angles[i], speeds[i], signs[i]);
                    }
                    if (wait) {
                        waitForUpdate(2);
                    }
                    sendMsg("MISPKG {Name head} {Link 5} {Value " + speeds[2] + "} {Order 1} {Link 6} {Value " + speeds[0] + "} {Order 1} {Link 7} {Value " + speeds[1] + "} {Order 1}");
                    for (int i = 0; i < 3; i++) {
                        curEyesCommand[i].start();
                    }
                }
            }.start();
            return true;
        }

        public boolean moveEyeBrows(int left, int right, int lSpeed, int rSpeed) {
            System.out.println(prg + ": moveEyeBrows " + left + " " + right + " " + lSpeed + " " + rSpeed);
            if (left < 10) left = 10; else if (left > 60) left = 60;
            if (right < 10) right = 10; else if (right > 60) right = 60;
            left -= 10;
            right -= 10;
            int[] degAngles = { (left - 35), (right - 35) };
            int[] intSpeeds = { lSpeed, rSpeed };
            final double[] angles = convertToRadians(degAngles);
            final int[] signs = signCheck(curEyebrows, angles);
            final double[] speeds = mapSpeeds("Eyebrows", intSpeeds, signs);
            new Thread() {

                public void run() {
                    boolean wait = false;
                    for (int i = 0; i < 2; i++) {
                        try {
                            if (curEyebrowsCommand[i].isAlive()) {
                                curEyebrowsCommand[i].interrupt();
                                wait = true;
                            }
                        } catch (NullPointerException e) {
                        }
                        curEyebrowsCommand[i] = new CheckThread("Eyebrows", i, angles[i], speeds[i], signs[i]);
                    }
                    if (wait) {
                        waitForUpdate(2);
                    }
                    boolean successful = sendMsg("MISPKG {Name head} {Link 3} {Value " + speeds[0] + "} {Order 1} {Link 4} {Value " + speeds[1] + "} {Order 1}");
                    for (int i = 0; i < 2; i++) {
                        curEyebrowsCommand[i].start();
                    }
                }
            }.start();
            return true;
        }

        private void moveLipMiddle(final boolean upper, final double[] origin, final double[] jointVel, final double[] dest) {
            final int resolution = 100;
            double[] time = { (Math.abs(dest[0] - origin[0])) / jointVel[0], (Math.abs(dest[1] - origin[1])) / jointVel[1] };
            final int iterations;
            final double remainder;
            if (time[0] < time[1]) {
                iterations = (int) (time[1] / resolution);
                remainder = (time[1] - (iterations * resolution / 1000));
            } else {
                iterations = (int) (time[0] / resolution);
                remainder = (time[0] - (iterations * resolution / 1000));
            }
            new Thread() {

                public void run() {
                    double[] start = origin;
                    for (int i = 0; i < (iterations + 1); i++) {
                        for (int j = 0; j < 2; j++) {
                            if (i == iterations) {
                                start[j] += dest[j];
                            } else {
                                double step = (jointVel[j] * ((double) resolution / 1000));
                                start[j] = ((jointVel[j] < 0) ? (((start[j] + step) < dest[j]) ? dest[j] : (start[j] + step)) : (((start[j] + step) > dest[j]) ? dest[j] : (start[j] + step)));
                            }
                        }
                        double[] angles = { (Math.asin(((1.74625 * Math.sin(start[0])) + (1.74625 * Math.sin(start[1]))) / (5.635))), -1 * (Math.asin(((1.74625 * Math.sin(start[0])) - (1.74625 * Math.sin(start[1]))) / (5))) };
                        double[] speeds = { (angles[0] - curMouthMiddle[((upper) ? 0 : 2)]) / (resolution / 1000), (angles[1] - curMouthMiddle[((upper) ? 1 : 3)]) / (resolution / 1000) };
                        double[] curMouthSub = { curMouthMiddle[((upper) ? 0 : 2)], curMouthMiddle[((upper) ? 1 : 3)] };
                        int[] signs = signCheck(curMouthSub, angles);
                        for (int k = 0; k < 2; k++) {
                            try {
                                if (curMouthMiddleCommand[k].isAlive()) {
                                    curMouthMiddleCommand[k].interrupt();
                                }
                            } catch (NullPointerException e) {
                            }
                            curMouthCommand[k] = new CheckThread("MouthMiddle", k, angles[k], speeds[k], signs[k]);
                        }
                        sendMsg("MISPKG {Name head} {Link " + ((upper) ? 9 : 13) + "} {Value " + speeds[0] + "} {Order 1} {Link " + ((upper) ? 10 : 14) + "} {Value " + speeds[1] + "} {Order 1}");
                        Sleep(((i == iterations) ? ((long) (remainder * 1000)) : ((long) (resolution * 1000))));
                    }
                    sendMsg("MISPKG {Name head} {Link " + ((upper) ? 9 : 13) + "} {Value 0.0} {Order 1} {Link " + ((upper) ? 10 : 14) + "} {Value 0.0} {Order 1}");
                }
            }.start();
        }

        public boolean moveMouth(int uLeft, int lLeft, int uRight, int lRight, int uLSpeed, int lLSpeed, int uRSpeed, int lRSpeed) {
            if (uLeft < 20) uLeft = 20; else if (uLeft > 70) uLeft = 70;
            if (lLeft < 20) lLeft = 20; else if (lLeft > 70) lLeft = 70;
            if (uRight < 20) uRight = 20; else if (uRight > 70) uRight = 70;
            if (lRight < 20) lRight = 20; else if (lRight > 70) lRight = 70;
            System.out.println(prg + ": moveMouth " + uLeft + " " + lLeft + " " + uRight + " " + lRight + " " + uLSpeed + " " + lLSpeed + " " + uRSpeed + " " + lRSpeed);
            int[] degAngles = { (uRight - 45), (uLeft - 45), (lRight - 45), (lLeft - 45) };
            int[] intSpeeds = { uRSpeed, uLSpeed, lRSpeed, lLSpeed };
            final double[] angles = convertToRadians(degAngles);
            final int[] signs = signCheck(curMouth, angles);
            final double[] speeds = mapSpeeds("Mouth", intSpeeds, signs);
            new Thread() {

                public void run() {
                    boolean wait = false;
                    for (int i = 0; i < 4; i++) {
                        try {
                            if (curMouthCommand[i].isAlive()) {
                                curMouthCommand[i].interrupt();
                                wait = true;
                            }
                        } catch (NullPointerException e) {
                        }
                        curMouthCommand[i] = new CheckThread("Mouth", i, angles[i], speeds[i], signs[i]);
                    }
                    if (wait) {
                        waitForUpdate(2);
                    }
                    boolean successful = sendMsg("MISPKG {Name head} {Link 8} {Value " + speeds[0] + "} {Order 1} {Link 9} {Value " + speeds[0] / 1.6 + "} {Order 1} {Link 10} {Value " + 0 + "} {Order 1} {Link 11} {Value " + speeds[1] + "} {Order 1} {Link 12} {Value " + speeds[2] + "} {Order 1} {Link 13} {Value " + speeds[2] / 1.6 + "} {Order 1} {Link 14} {Value " + 0 + "} {Order 1} {Link 15} {Value " + speeds[3] + "} {Order 1}");
                    for (int i = 0; i < 4; i++) {
                        curMouthCommand[i].start();
                    }
                }
            }.start();
            return true;
        }
    }

    public void test() {
        Sleep(1000);
        System.out.println("Testing...");
        int[] test = new int[4];
        try {
            moveHead(109, 53, 27, 26);
            moveArm(false, 134, 2, 65, 8, 3, 2);
            Sleep(5000);
            moveEyeBrows(56, 12, 24, 5);
            moveArm(true, 122, 7, 86, 1, 10, 26);
            Sleep(5000);
            smile();
            moveMouth(58, 34, 36, 24);
            moveArm(false, 70, 51, 80, 13, 21, 8);
            moveArm(true, 122, 9, 33, 29, 15, 17);
            Sleep(3000);
            moveEyes(26, 43, 45, 30, 7, 5);
            moveEyeBrows(60, 49, 5, 16);
            moveMouth(36, 60, 23, 50);
            Sleep(4000);
            moveHead(91, 87, 9, 14);
            moveMouth(46, 22, 53, 62);
            moveArm(false, 43, 65, 44, 1, 25, 13);
            Sleep(5000);
            moveEyeBrows(49, 17, 5, 18);
            moveArm(true, 105, 11, 9, 15, 6, 10);
            Sleep(1000);
            moveEyes(43, 30, 29, 7, 13, 22);
            moveArm(false, 93, 77, 65, 8, 28, 28);
            moveArm(true, 70, 51, 80, 13, 30, 9);
            Sleep(5000);
            moveHead(104, 112, 26, 30);
            moveEyes(49, 30, 58, 30, 7, 17);
            Sleep(5000);
            moveArm(true, 163, 2, 86, 3, 8, 16);
            moveMouth(63, 26, 25, 56);
            Sleep(3000);
            moveArm(false, 158, 2, 57, 13, 14, 18);
            moveEyeBrows(55, 50, 13, 11);
            Sleep(5000);
            moveArm(false, 0, 0, 0);
            moveArm(true, 0, 0, 0);
            moveHead(90, 90);
            moveEyes(45, 45, 45);
            moveEyeBrows(35, 35);
            moveMouth(45, 45, 45, 45);
        } catch (RemoteException e) {
            System.err.println(prg + ": RemoteException..." + e);
        }
    }

    public static void main(String[] args) throws Exception {
        ADEServerImpl.main(args);
    }
}
