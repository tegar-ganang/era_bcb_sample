package gov.sns.apps.ringmeasurement;

import gov.sns.tools.*;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.ca.*;
import gov.sns.tools.messaging.*;
import gov.sns.tools.statistics.RunningWeightedStatistics;
import java.util.*;

/**
 * @author cp3, tep
 * BPM agent for use with MIALive.java.
 */
public class BPMAgent {

    protected BPM BPMNode;

    protected AcceleratorSeq sequence;

    protected Channel BPMXChannel;

    protected Channel BPMYChannel;

    public volatile RunningWeightedStatistics xStats;

    public volatile RunningWeightedStatistics yStats;

    public volatile double[] xTBT;

    public volatile double[] yTBT;

    public BPMAgent(AcceleratorSeq aSequence, BPM newBPMNode, int numTurns) {
        xTBT = new double[numTurns];
        yTBT = new double[numTurns];
        xStats = new RunningWeightedStatistics(0.2);
        yStats = new RunningWeightedStatistics(0.2);
        BPMNode = newBPMNode;
        sequence = aSequence;
        BPMXChannel = BPMNode.getChannel(BPM.X_TBT_HANDLE);
        BPMYChannel = BPMNode.getChannel(BPM.Y_TBT_HANDLE);
        makeXChannelConnectionListener();
        makeYChannelConnectionListener();
        BPMXChannel.requestConnection();
        BPMYChannel.requestConnection();
        Channel.flushIO();
    }

    public String name() {
        return BPMNode.getId();
    }

    public BPM getNode() {
        return BPMNode;
    }

    public double getPosition() {
        return sequence.getPosition(BPMNode);
    }

    public boolean isOkay() {
        return BPMNode.getStatus();
    }

    public double[] getXTBT() {
        return xTBT;
    }

    public double[] getYTBT() {
        return yTBT;
    }

    public void addXPoint(double val) {
        xStats.addSample(val);
    }

    public void addYPoint(double val) {
        yStats.addSample(val);
    }

    public double getXMean() {
        return xStats.mean();
    }

    public double getYMean() {
        return yStats.mean();
    }

    public void clearXAvg() {
        xStats.clear();
    }

    public void clearYAvg() {
        yStats.clear();
    }

    public void addXChannelConnectionListener(ConnectionListener listener) {
        BPMXChannel.addConnectionListener(listener);
    }

    public void addYChannelConnectionListener(ConnectionListener listener) {
        BPMYChannel.addConnectionListener(listener);
    }

    public void makeXChannelConnectionListener() {
        this.addXChannelConnectionListener(new ConnectionListener() {

            public void connectionMade(Channel aChannel) {
                makeBPMXMonitor();
            }

            public void connectionDropped(Channel aChannel) {
                System.out.println("x connection lost");
            }
        });
    }

    public void makeYChannelConnectionListener() {
        this.addYChannelConnectionListener(new ConnectionListener() {

            public void connectionMade(Channel aChannel) {
                makeBPMYMonitor();
            }

            public void connectionDropped(Channel aChannel) {
                System.out.println("y connection lost");
            }
        });
    }

    public void makeBPMXMonitor() {
        try {
            BPMXChannel.addMonitorValue(new IEventSinkValue() {

                public void eventValue(ChannelRecord record, Channel chan) {
                    xTBT = record.doubleArray();
                }
            }, Monitor.VALUE);
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (MonitorException e) {
            e.printStackTrace();
        }
    }

    public void makeBPMYMonitor() {
        try {
            BPMYChannel.addMonitorValue(new IEventSinkValue() {

                public void eventValue(ChannelRecord record, Channel chan) {
                    yTBT = record.doubleArray();
                }
            }, Monitor.VALUE);
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (MonitorException e) {
            e.printStackTrace();
        }
    }
}
