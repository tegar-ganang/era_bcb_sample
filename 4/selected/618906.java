package gov.sns.apps.rtbtwizard;

import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.ca.*;
import java.util.*;

/**
 * Wrap a BPM node with methods that conveniently provide information and control 
 * relevant to orbit correction.  A BPM agent provides data specific to a 
 * particular plane, hence a BPM nodes gets wrapped by a horizontal and a 
 * vertical BpmAgent.  BpmAgent is abstract, but it has two concrete subclasses
 * that provide plane specific identification (HorzBpmAgent and VertBpmAgent).
 *
 * @author  cp3
 */
public class BpmAgent {

    protected BPM bpmNode;

    protected AcceleratorSeq sequence;

    protected Channel xavgch;

    protected Channel yavgch;

    public double[] xtbtdata;

    public double[] ytbtdata;

    public int fitsize;

    /** Creates new BpmAgent */
    public BpmAgent(AcceleratorSeq aSequence, BPM newBpmNode) {
        bpmNode = newBpmNode;
        sequence = aSequence;
        xavgch = bpmNode.getChannel(BPM.X_TBT_HANDLE);
        yavgch = bpmNode.getChannel(BPM.Y_TBT_HANDLE);
        xavgch.requestConnection();
        yavgch.requestConnection();
        bpmNode.getChannel(BPM.X_AVG_HANDLE).requestConnection();
        bpmNode.getChannel(BPM.Y_AVG_HANDLE).requestConnection();
    }

    /** Name of the BPM as given by its unique ID */
    public String name() {
        return bpmNode.getId();
    }

    /** Get the node being wrapped */
    public BPM getNode() {
        return bpmNode;
    }

    /** Get the position of the BPM */
    public double position() {
        return getPosition();
    }

    /** Get the position of the BPM */
    public double getPosition() {
        return sequence.getPosition(bpmNode);
    }

    /** Test if the BPM node is okay */
    public boolean isConnected() {
        return xavgch.isConnected() && yavgch.isConnected() && bpmNode.getChannel(BPM.X_AVG_HANDLE).isConnected() && bpmNode.getChannel(BPM.Y_AVG_HANDLE).isConnected();
    }

    /** Test if the BPM node is okay */
    public boolean isOkay() {
        return isOkay(bpmNode);
    }

    /** Test whether the given BPM node has a good status and all its channels can connect */
    public static boolean isOkay(BPM bpm) {
        return bpm.getStatus();
    }

    /** Test whether the BPM's xAvg, yAvg and ampAvg channels can connect */
    public static boolean nodeCanConnect(BPM bpm) {
        boolean canConnectx = true;
        boolean canConnecty = true;
        boolean canConnect = false;
        try {
            canConnectx = bpm.getChannel(BPM.X_TBT_HANDLE).connectAndWait();
            canConnectx = bpm.getChannel(BPM.Y_TBT_HANDLE).connectAndWait();
        } catch (NoSuchChannelException excpt) {
            if (!canConnectx || !canConnecty) {
                canConnect = false;
            }
        }
        return canConnect;
    }

    /** Identify the bpm agent by its BPM name */
    @Override
    public String toString() {
        return name();
    }

    /** Return a list of BPM nodes associated with the list of bpmAgents */
    public static List getNodes(List bpmAgents) {
        int count = bpmAgents.size();
        List bpmNodes = new ArrayList(count);
        for (int index = 0; index < count; index++) {
            BpmAgent bpmAgent = (BpmAgent) bpmAgents.get(index);
            bpmNodes.add(bpmAgent.getNode());
        }
        return bpmNodes;
    }

    /** Get the average horizontal position reading from the last turn **/
    public double getXAvg() {
        double xavg = 0.0;
        try {
            xavg = bpmNode.getXAvg();
        } catch (ConnectionException e) {
            System.out.println("Could not connect with:" + bpmNode.getId());
        } catch (GetException e) {
            System.out.println("Could not get value for:" + bpmNode.getId());
        }
        return xavg;
    }

    /** Get the horizontal TBT array **/
    public double[] getXAvgTBTArray() {
        double[] value;
        int size = 0;
        try {
            size = xavgch.elementCount();
        } catch (ConnectionException e) {
        }
        value = new double[size];
        try {
            value = xavgch.getArrDbl();
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to: " + xavgch.channelName());
        } catch (GetException e) {
            System.err.println("Unable to get process variables.");
        }
        return value;
    }

    /** Get the average vertical position reading from the last turn **/
    public double getYAvg() {
        double yavg = 0.0;
        try {
            yavg = bpmNode.getYAvg();
        } catch (ConnectionException e) {
            System.out.println("Could not connect with:" + bpmNode.getId());
        } catch (GetException e) {
            System.out.println("Could not get value from:" + bpmNode.getId());
        }
        return yavg;
    }

    /** Get the vertical TBT array **/
    public double[] getYAvgTBTArray() {
        double[] value;
        int size = 0;
        try {
            size = yavgch.elementCount();
        } catch (ConnectionException e) {
        }
        value = new double[size];
        try {
            value = yavgch.getArrDbl();
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (GetException e) {
            System.err.println("Unable to get process variables.");
        }
        return value;
    }
}
