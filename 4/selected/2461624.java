package gov.sns.services.tripmonitor;

import java.util.*;
import gov.sns.ca.ChannelTimeRecord;
import gov.sns.xal.smf.AcceleratorNode;
import gov.sns.tools.messaging.MessageCenter;

/** monitors a single cavity */
public class NodeMonitor implements ChannelEventListener {

    /** event message center */
    protected MessageCenter MESSAGE_CENTER;

    /** proxy for posting node monitor events */
    protected NodeMonitorListener EVENT_PROXY;

    /** trip monitor filter */
    protected final TripMonitorFilter MONITOR_FILTER;

    /** accelerator node to monitor */
    protected final AcceleratorNode NODE;

    /** channel monitors */
    protected final List<ChannelMonitor> CHANNEL_MONITORS;

    /** Constructor */
    public NodeMonitor(final AcceleratorNode node, final TripMonitorFilter monitorFilter) {
        MESSAGE_CENTER = new MessageCenter("Node Monitor");
        EVENT_PROXY = MESSAGE_CENTER.registerSource(this, NodeMonitorListener.class);
        NODE = node;
        MONITOR_FILTER = monitorFilter;
        CHANNEL_MONITORS = new ArrayList<ChannelMonitor>();
        populateChannelMonitors();
    }

    /** add a node monitor listener */
    public void addNodeMonitorListener(final NodeMonitorListener listener) {
        MESSAGE_CENTER.registerTarget(listener, this, NodeMonitorListener.class);
    }

    /** remove the node monitor listener */
    public void removeNodeMonitorListener(final NodeMonitorListener listener) {
        MESSAGE_CENTER.removeTarget(listener, this, NodeMonitorListener.class);
    }

    /** get the accelerator node */
    public AcceleratorNode getNode() {
        return NODE;
    }

    /** get the node ID */
    public String getID() {
        return NODE.getId();
    }

    /** get the channel monitors */
    public List<ChannelMonitor> getChannelMonitors() {
        return CHANNEL_MONITORS;
    }

    /** run the monitor */
    public void run() {
        if (Main.isVerbose()) {
            System.out.println("Run monitor for node:  " + NODE.getId());
        }
        for (final ChannelMonitor channelMonitor : CHANNEL_MONITORS) {
            channelMonitor.requestConnection();
        }
    }

    /** populate the channel monitors */
    protected void populateChannelMonitors() {
        final List<String> pvs = MONITOR_FILTER.getTripPVs(NODE);
        for (final String pv : pvs) {
            final ChannelMonitor channelMonitor = new ChannelMonitor(pv);
            CHANNEL_MONITORS.add(channelMonitor);
            channelMonitor.addChannelEventListener(this);
        }
    }

    /**
	 * The PV's monitored trip count has been incremented.
	 * @param monitor the channel monitor whose trip count has changed
	 * @param tripRecord record of the trip
	 */
    public void handleTrip(final ChannelMonitor monitor, final TripRecord tripRecord) {
        Main.printlnIfVerbose("Trip:  " + tripRecord);
        EVENT_PROXY.handleTrip(this, tripRecord);
    }

    /**
	 * The PV's monitored value has changed.
	 * @param monitor the channel monitor whose value has changed
	 * @param record The channel time record of the new value
	 */
    public void valueChanged(final ChannelMonitor monitor, final ChannelTimeRecord record) {
    }

    /**
	 * The channel's connection has changed.  Either it has established a new connection or the existing connection has dropped.
	 * @param monitor The channel monitor whose connection status has changed.
	 * @param isConnected The channel's new connection state
	 */
    public void connectionChanged(final ChannelMonitor monitor, final boolean isConnected) {
        Main.printlnIfVerbose(monitor + " is connected:  " + isConnected);
        EVENT_PROXY.connectionChanged(this, monitor, isConnected);
    }

    /** description of this instance */
    @Override
    public String toString() {
        return "Node:  " + NODE + ", channel monitors:  " + CHANNEL_MONITORS;
    }
}
