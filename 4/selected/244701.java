package melonlauncher;

import mil.navy.nps.dis.*;
import mil.navy.nps.disEnumerations.*;
import java.text.*;
import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.*;

/**
 *
 * Generate default example PDU stream for NPS AUV track #1 in AuvInBeachTanks VRML world.
 *
 * Authors: Doug Miller & Bill Bohman
 * Start Date: 6 JAN 99
 * Revised:  14 DEC 2000
 * Description: Generates DIS-JAVA-VRML entity state Pdus to drive the NPS AUV
 * All calculations are in the tank coordinate system, converted to DIS only when
 * creating the PDU and sending.
 *Modified by:  Andrew Wiest
 *Description: A simple exercise in passing PDU packets to move an object in a VRML scene
 *
 *
 *
 * To Do: 1) Why are positions doubles and everything else floats thus creating need for casts
 *        2) How to implement time steps, hard wired or passed in? if passed in, when/where/
 *           by whom
 *
 *<dt><b>Invocation:</b>
 *<dd> java demo.auv.AuvPduGenerator
 *
 *<P>
 *
 *<B>Entity coordinate systems (right-hand rule applies):</B>
 *<PRE>
 *
 *          DIS                 VRML=BeachTank!
 *
 *      -Z entity up         Y entity up
 *           ^                   ^
 *           |                   |
 *           |                   |
 *           |                   |
 *           +------> X          +-------> X    nose of entity body
 *          /                   /
 *         /                   /
 *        /                   /
 *       Y                   Z
 *         right-hand side
 *         of entity body
 *
 *  Rotation angle axes (right-hand rule applies):
 *
 *           DIS        VRML      Angle of rotation
 *
 *  Roll      X          X         phi
 *  Pitch     Y          Z        theta
 *  Yaw       Z         -Y         psi
 *</PRE>
 *
*/
public class MelonLauncherPduGenerator {

    private static EntityStatePdu espdu;

    private String entityName;

    private static BufferedReader telemetryStream;

    private static final String fileName1 = "mission.waypoints.dat";

    private static final String fileName2 = "/vrtp/demo/auv/mission.waypoints.dat";

    private static final String fileName3 = "c:/vrtp/demo/auv/mission.waypoints.dat";

    private static final String fileName4 = "d:/vrtp/demo/auv/mission.waypoints.dat";

    private static final String fileName5 = "~brutzman/.public_html/vrtp/demo/auv/mission.waypoints.dat";

    private static String nextLine;

    private static String telemetryLine;

    private static DecimalFormat precision;

    private static boolean DEBUG;

    private float pduSendInterval = 1;

    private BehaviorStreamBufferUDP behaviorStreamBufferUDP;

    private String ipAddress;

    private int portNumber;

    private static int timeToLive = 15;

    public MelonLauncherPduGenerator(String name) {
        espdu = new EntityStatePdu();
        entityName = name;
        nextLine = new String();
        precision = new DecimalFormat("###0.#");
        DEBUG = false;
        ipAddress = "224.2.181.145";
        portNumber = 62040;
        behaviorStreamBufferUDP = new BehaviorStreamBufferUDP(ipAddress, portNumber);
        behaviorStreamBufferUDP.setTimeToLive(timeToLive);
        ArticulationParameter forwardRudder, asternRudder;
        forwardRudder = new ArticulationParameter();
        asternRudder = new ArticulationParameter();
        espdu.addArticulationParameter(forwardRudder);
        espdu.addArticulationParameter(asternRudder);
        forwardRudder.setParameterTypeDesignator(ParameterTypeDesignatorField.ARTICULATEDPART);
        asternRudder.setParameterTypeDesignator(ParameterTypeDesignatorField.ARTICULATEDPART);
        forwardRudder.setChangeIndicator(0);
        asternRudder.setChangeIndicator(0);
        forwardRudder.setArticulationAttachmentID(0);
        asternRudder.setArticulationAttachmentID(0);
        forwardRudder.setParameterType(1024 + 15);
        asternRudder.setParameterType(1024 + 15);
        forwardRudder.setParameterValue(30.0 * Math.PI / 180.0);
        asternRudder.setParameterValue(-30.0 * Math.PI / 180.0);
        short[] entityID = new short[3];
        entityID[0] = 1;
        entityID[1] = 5;
        entityID[2] = 6;
        espdu.setEntityID(entityID[0], entityID[1], entityID[2]);
        System.out.println("espdu entity ID is set to [" + espdu.getEntityID().toString() + "]");
        espdu.setMarking("melon");
        System.out.println("espdu  marking  is set to [" + espdu.getMarking() + "]");
    }

    public static void debug(String message) {
        if (DEBUG) {
            System.out.println(message);
        }
    }

    public void moveToWaypoint(double xPoint, double yPoint, double zPoint, float speed) {
        double currentXpos = espdu.getEntityLocationX();
        double currentYpos = -espdu.getEntityLocationZ();
        double currentZpos = espdu.getEntityLocationY();
        double xDistance = xPoint - currentXpos;
        double yDistance = yPoint - currentYpos;
        double zDistance = zPoint - currentZpos;
        double distance = Math.sqrt(xDistance * xDistance + yDistance * yDistance + zDistance * zDistance);
        double xzDistance = Math.sqrt(xDistance * xDistance + zDistance * zDistance);
        double direction = Math.atan2(zDistance, xDistance);
        double pitch = Math.atan(yDistance / xzDistance);
        espdu.setEntityLinearVelocityX((float) (speed * xDistance / distance));
        espdu.setEntityLinearVelocityY((float) (speed * zDistance / distance));
        espdu.setEntityLinearVelocityZ((float) (speed * (-yDistance / distance)));
        espdu.setEntityOrientationTheta((float) pitch);
        espdu.setEntityOrientationPsi((float) direction);
        if (DEBUG) {
            debug("AUV tank coordinates (" + precision.format(espdu.getEntityLocationX()) + ", " + precision.format(-espdu.getEntityLocationZ()) + ", " + precision.format(espdu.getEntityLocationY()) + ")" + "\nHeading equals ... " + precision.format(espdu.getEntityOrientationPsi()) + "\nPitch equals ... " + precision.format(espdu.getEntityOrientationTheta()) + "\n");
        }
        for (float ix = speed * pduSendInterval; ix < distance; ix += speed * pduSendInterval) {
            float tempXdot = espdu.getEntityLinearVelocityX();
            float tempYdot = espdu.getEntityLinearVelocityY();
            float tempZdot = espdu.getEntityLinearVelocityZ();
            espdu.setEntityLocationX(espdu.getEntityLocationX() + tempXdot * pduSendInterval);
            espdu.setEntityLocationY(espdu.getEntityLocationY() + tempYdot * pduSendInterval);
            espdu.setEntityLocationZ(espdu.getEntityLocationZ() + tempZdot * pduSendInterval);
            System.out.println("Melon coordinates (" + precision.format(espdu.getEntityLocationX()) + ", " + precision.format(-espdu.getEntityLocationZ()) + ", " + precision.format(espdu.getEntityLocationY()) + "), " + "heading " + precision.format(espdu.getEntityOrientationPsi() * 180.0 / Math.PI));
            behaviorStreamBufferUDP.sendPdu(espdu, ipAddress, portNumber);
            if (DEBUG) {
                debug("AuvPduGenerator sleep " + pduSendInterval);
            }
            try {
                Thread.sleep((long) pduSendInterval * 1000);
            } catch (InterruptedException interruptedException) {
                throw new RuntimeException(" exceptional sleep ");
            }
        }
        espdu.setEntityLocationX(xPoint);
        espdu.setEntityLocationY(zPoint);
        espdu.setEntityLocationZ(-yPoint);
        behaviorStreamBufferUDP.sendPdu(espdu, ipAddress, portNumber);
        debug("\nEnding location is ... (" + precision.format(espdu.getEntityLocationX()) + ", " + precision.format(-espdu.getEntityLocationZ()) + ", " + precision.format(espdu.getEntityLocationY()) + ")" + "\nHeading equals ... " + precision.format(espdu.getEntityOrientationPsi()) + ", Pitch equals ... " + precision.format(espdu.getEntityOrientationTheta()) + "\n");
    }

    public void moveToFileWaypoint() {
        double xVal = 0;
        double yVal = 0;
        double zVal = 0;
        float spd = 0;
        TelemetryFileOpen();
        while (true) {
            do {
                nextLine = readNextTelemetryState();
            } while ((nextLine == "") || (nextLine.charAt(0) == '#') || ((nextLine.charAt(0) == '/') && (nextLine.charAt(1) == '/')));
            StringTokenizer tokens = new StringTokenizer(nextLine);
            if (tokens.countTokens() > 0 && tokens.nextElement().equals("waypoint")) {
                int counter = 0;
                while (tokens.hasMoreTokens()) {
                    if (counter == 0) {
                        xVal = Double.valueOf(tokens.nextToken()).doubleValue();
                    }
                    if (counter == 1) {
                        yVal = Double.valueOf(tokens.nextToken()).doubleValue();
                    }
                    if (counter == 2) {
                        zVal = Double.valueOf(tokens.nextToken()).doubleValue();
                    }
                    if (counter == 3) {
                        spd = Float.valueOf(tokens.nextToken()).floatValue();
                    }
                    counter++;
                }
                moveToWaypoint(xVal, yVal, zVal, spd);
            }
        }
    }

    public static void TelemetryFileOpen() {
        try {
            telemetryStream = new BufferedReader(new FileReader(new File(fileName1)));
            System.out.println("found " + fileName1 + ", loading...");
        } catch (IOException e1) {
            try {
                telemetryStream = new BufferedReader(new FileReader(new File(fileName2)));
                System.out.println("found " + fileName2 + ", loading...");
            } catch (IOException e2) {
                try {
                    telemetryStream = new BufferedReader(new FileReader(new File(fileName3)));
                    System.out.println("found " + fileName3 + ", loading...");
                } catch (IOException e3) {
                    try {
                        telemetryStream = new BufferedReader(new FileReader(new File(fileName4)));
                        System.out.println("found " + fileName4 + ", loading...");
                    } catch (IOException e4) {
                        try {
                            telemetryStream = new BufferedReader(new FileReader(new File(fileName5)));
                            System.out.println("found " + fileName5 + ", loading...");
                        } catch (IOException e5) {
                            System.out.println("Failure to open input file: " + fileName1);
                            System.out.println("Failure to open input file: " + fileName2);
                            System.out.println("Failure to open input file: " + fileName3);
                            System.out.println("Failure to open input file: " + fileName4);
                            System.out.println("Failure to open input file: " + fileName5);
                            java.lang.System.exit(-1);
                        }
                    }
                }
            }
        }
    }

    public static void TelemetryFileClose() {
        try {
            telemetryStream.close();
        } catch (IOException ioe) {
            System.err.println("Error during file close: " + ioe.toString());
        }
    }

    public static String readNextTelemetryState() {
        try {
            System.out.println("Reading next waypoint from the file");
            nextLine = telemetryStream.readLine();
            if (nextLine == null) {
                System.out.println("Entering if loop for nextLine == null");
                TelemetryFileClose();
                TelemetryFileOpen();
                nextLine = readNextTelemetryState();
            }
            return nextLine;
        } catch (EOFException eof) {
            System.err.println("End of waypoint file, reopen\n" + eof.toString());
            TelemetryFileClose();
            TelemetryFileOpen();
            readNextTelemetryState();
        } catch (IOException e) {
            System.err.println("Error during read from file\n" + e.toString());
            System.exit(1);
        }
        return "";
    }

    public static void main(String[] args) {
        boolean ttlMatch = false;
        boolean rtpMatch = false;
        for (int index = 0; index < args.length; index++) {
            if (!rtpMatch) try {
                rtpMatch = ((args[index].compareToIgnoreCase("rtp") == 0) || (args[index].compareToIgnoreCase("-rtp") == 0));
                if (rtpMatch) continue;
            } catch (java.lang.NoSuchMethodError nsme) {
                rtpMatch = ((args[index].compareTo("rtp") == 0) || (args[index].compareTo("-rtp") == 0) || (args[index].compareTo("RTP") == 0) || (args[index].compareTo("-RTP") == 0));
                if (rtpMatch) continue;
            }
            if (!(ttlMatch) && (args.length - index > 1)) {
                try {
                    ttlMatch = ((args[index].compareToIgnoreCase("ttl") == 0) || (args[index].compareToIgnoreCase("-ttl") == 0) || (args[index].compareToIgnoreCase("timeToLive") == 0) || (args[index].compareToIgnoreCase("-timeToLive") == 0));
                } catch (java.lang.NoSuchMethodError nsme) {
                    ttlMatch = ((args[index].compareTo("ttl") == 0) || (args[index].compareTo("-ttl") == 0) || (args[index].compareTo("timeToLive") == 0) || (args[index].compareTo("-timeToLive") == 0));
                }
                if (ttlMatch) {
                    try {
                        timeToLive = Integer.parseInt(args[index + 1]);
                        index++;
                    } catch (Exception e) {
                        System.out.println(e);
                        System.out.println("illegal timeToLive value, exiting");
                        System.exit(-1);
                    }
                    if ((timeToLive < 0) || (timeToLive > 127)) {
                        System.out.println("multicast timeToLive ttl=" + timeToLive + " is out of range [0..127], exiting");
                        System.exit(-1);
                    }
                }
            } else {
                System.out.println("Usage: java demo.auv.AuvPduGenerator [-ttl <value 1..127>] [-rtp]");
                System.exit(-1);
            }
        }
        System.out.println("multicast timeToLive ttl=" + timeToLive);
        System.out.println("RTP headers prepended=" + rtpMatch);
        MelonLauncherPduGenerator Dallas = new MelonLauncherPduGenerator("NPS-AUV");
        WorldCoordinate startPos = new WorldCoordinate(5.0, 3.0, 2.0);
        Dallas.espdu.setEntityLocation(startPos);
        Dallas.espdu.setRtpHeaderEnabled(rtpMatch);
        System.out.println("Start values = Position:     " + Dallas.espdu.getEntityLocationX() + " / " + -Dallas.espdu.getEntityLocationZ() + " / " + Dallas.espdu.getEntityLocationY() + "\n                    Linear Vel:  " + Dallas.espdu.getEntityLinearVelocityX() + " / " + -Dallas.espdu.getEntityLinearVelocityZ() + " / " + Dallas.espdu.getEntityLinearVelocityY() + "\n                    Orientation: " + Dallas.espdu.getEntityOrientationPsi() + " / " + Dallas.espdu.getEntityOrientationTheta() + " / " + Dallas.espdu.getEntityOrientationPhi() + "\n                    Angular Vel: " + Dallas.espdu.getEntityAngularVelocityX() + " / " + -Dallas.espdu.getEntityAngularVelocityZ() + " / " + Dallas.espdu.getEntityAngularVelocityY());
        Dallas.moveToFileWaypoint();
    }
}
