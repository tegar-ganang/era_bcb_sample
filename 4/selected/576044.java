package gov.sns.apps.lossviewer;

import gov.sns.tools.*;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.ca.*;
import gov.sns.tools.messaging.*;
import java.util.*;

/**
 * Wrap a BLM node with methods that conveniently provide information and control 
 * of the loss monitoring.  
 *
 * @author  cp3
 */
public class BlmAgent {

    protected BLM blmNode;

    protected AcceleratorSeq sequence;

    protected Channel lossavgch;

    public double lossavg = 0.0;

    public double lossmonitormade = 0;

    /** Flag for determining if the connection is available */
    public volatile boolean isconnected;

    /** Creates new BlmAgent */
    public BlmAgent(AcceleratorSeq aSequence, BLM newBlmNode) {
        blmNode = newBlmNode;
        sequence = aSequence;
        lossavgch = blmNode.getChannel(BLM.LOSS_AVG_HANDLE);
        makeLossChannelConnectionListener();
        lossavgch.requestConnection();
        lossavgch.pendIO(5);
    }

    /** Name of the BLM as given by its unique ID */
    public String name() {
        return blmNode.getId();
    }

    /** Get the node being wrapped */
    public BLM getNode() {
        return blmNode;
    }

    /** Get the position of the BLM */
    public double position() {
        return getPosition();
    }

    /** Get the position of the BLM */
    public double getPosition() {
        return sequence.getPosition(blmNode);
    }

    /** Test if the BLM node is okay */
    public boolean isOkay() {
        return blmNode.getStatus();
    }

    public boolean isConnected() {
        return lossavgch.isConnected();
    }

    /** Test whether the given BLM node has a good status and all its channels can connect */
    public static boolean isOkay(BLM blm) {
        return blm.getStatus() && nodeCanConnect(blm);
    }

    /** Test whether the BLM's lossAvg channels can connect */
    public static boolean nodeCanConnect(BLM blm) {
        boolean canConnect = true;
        try {
            canConnect = blm.getChannel(BLM.LOSS_AVG_HANDLE).connectAndWait();
            System.out.println(blm.getId() + " is Connected");
        } catch (NoSuchChannelException excpt) {
            canConnect = false;
            System.out.println(blm.getId() + " Can't connect");
        }
        return canConnect;
    }

    /** Identify the blm agent by its BLM name */
    public String toString() {
        return name();
    }

    /** Return a list of BLM nodes associated with the list of blmAgents */
    public static List getNodes(List blmAgents) {
        int count = blmAgents.size();
        List blmNodes = new ArrayList(count);
        for (int index = 0; index < count; index++) {
            BlmAgent blmAgent = (BlmAgent) blmAgents.get(index);
            blmNodes.add(blmAgent.getNode());
        }
        return blmNodes;
    }

    public double getLossAvg() {
        return lossavg;
    }

    public double readLossAvgChannel() {
        double lossAvg;
        try {
            lossAvg = blmNode.getLossAvg();
        } catch (Exception exception) {
            throw new ExceptionWrapper(exception);
        }
        return lossAvg;
    }

    public void addLossChannelConnectionListener(ConnectionListener listener) {
        lossavgch.addConnectionListener(listener);
    }

    public void makeLossChannelConnectionListener() {
        this.addLossChannelConnectionListener(new ConnectionListener() {

            public void connectionMade(Channel aChannel) {
                if (lossmonitormade != 1) {
                    makeBLMMonitor();
                    lossmonitormade = 1;
                }
                isconnected = true;
            }

            public void connectionDropped(Channel aChannel) {
                isconnected = false;
            }
        });
    }

    public void makeBLMMonitor() {
        try {
            lossavgch.addMonitorValue(new IEventSinkValue() {

                public void eventValue(ChannelRecord record, Channel chan) {
                    lossavg = record.doubleValue();
                }
            }, Monitor.VALUE);
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (MonitorException e) {
            e.printStackTrace();
        }
    }
}
