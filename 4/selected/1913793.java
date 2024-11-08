package gov.sns.tools.pvlogger;

import gov.sns.ca.Channel;
import gov.sns.ca.ConnectionListener;
import java.util.*;

/**
 * ChannelGroup is a wrapper for the PV_SET data source which defines a group of related PVs.
 *
 * @author  tap
 */
public class ChannelGroup {

    /** default logging period (seconds) for the group */
    protected final double DEFAULT_LOGGING_PERIOD;

    /** label of the channel group */
    protected final String LABEL;

    /** description of the channel group */
    protected final String DESCRIPTION;

    /** array of channel wrappers */
    protected ChannelWrapper[] _channelWrappers;

    /** handler of channel connection events */
    protected ConnectionHandler _connectionHandler;

    /** time of last channel connection event */
    private Date _lastChannelEventTime;

    /**
	 * Constructor
	 * @param label The group's label
	 * @param description A description of the group
	 * @param pvs The PVs in the group
	 * @param loggingPeriod The default logging period for the group
	 */
    public ChannelGroup(String label, String description, String[] pvs, double loggingPeriod) {
        LABEL = label;
        DESCRIPTION = description;
        DEFAULT_LOGGING_PERIOD = loggingPeriod;
        _lastChannelEventTime = new Date();
        _connectionHandler = new ConnectionHandler();
        wrapPVs(pvs);
    }

    /**
	 * Constructor
	 * @param groupLabel The group's label
	 * @param pvs The PVs in the group
	 * @param loggingPeriod The default logging period for the group
	 */
    public ChannelGroup(String groupLabel, String[] pvs, double loggingPeriod) {
        this(groupLabel, "", pvs, loggingPeriod);
    }

    /**
	 * Dispose of this channel group's resources
	 */
    public void dispose() {
        for (int index = 0; index < _channelWrappers.length; index++) {
            _channelWrappers[index].removeConnectionListener(_connectionHandler);
        }
    }

    /**
	 * Get the group label.
	 * @return the group label.
	 */
    public String getLabel() {
        return LABEL;
    }

    /**
	 * Get a description of the group
	 * @return a description of this group
	 */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
	 * Get the default logging period for this group
	 * @return the default logging period in seconds
	 */
    public double getDefaultLoggingPeriod() {
        return DEFAULT_LOGGING_PERIOD;
    }

    /**
	 * Create ChannelWrappers for the specified pvs.
	 * @param pvs the list of PVs to wrap
	 */
    protected void wrapPVs(final String[] pvs) {
        List<ChannelWrapper> wrappers = new ArrayList<ChannelWrapper>(pvs.length);
        for (int index = 0; index < pvs.length; index++) {
            ChannelWrapper wrapper = new ChannelWrapper(pvs[index]);
            wrapper.addConnectionListener(_connectionHandler);
            wrappers.add(wrapper);
        }
        _channelWrappers = wrappers.toArray(new ChannelWrapper[wrappers.size()]);
    }

    /**
	 * Request connections to the channel wrappers.
	 */
    public void requestConnections() {
        for (int index = 0; index < _channelWrappers.length; index++) {
            _channelWrappers[index].requestConnection();
        }
    }

    /**
	 * Get the channel wrappers for the channels associated with this group.
	 * @return The array of channel wrappers of this group.
	 */
    public ChannelWrapper[] getChannelWrappers() {
        return _channelWrappers;
    }

    /**
	 * Get the collection of channels which we attempt to monitor
	 * @return a collection of channels corresponding to the channel wrappers
	 */
    public Collection<Channel> getChannels() {
        Set<Channel> channels = new HashSet<Channel>(_channelWrappers.length);
        for (int index = 0; index < _channelWrappers.length; index++) {
            channels.add(_channelWrappers[index].getChannel());
        }
        return channels;
    }

    /**
	 * Get the number of channels in this group
	 * @return The number of channels in this group
	 */
    public int getChannelCount() {
        return _channelWrappers.length;
    }

    /**
	 * Get the timestamp of the last channel event (e.g. channel connected/disconnected event)
	 * @return the wall clock timestamp of the last channel event
	 */
    public Date getLastChannelEventTime() {
        return _lastChannelEventTime;
    }

    /**
	 * Override toString() to return the group label and a description
	 * @return the group label and description
	 */
    @Override
    public String toString() {
        return "[label: " + LABEL + ", description: " + DESCRIPTION + "]";
    }

    /**
	 *
	 */
    protected class ConnectionHandler implements ConnectionListener {

        /**
		 * Indicates that a connection to the specified channel has been established.
		 * @param channel The channel which has been connected.
		 */
        public void connectionMade(Channel channel) {
            _lastChannelEventTime = new Date();
        }

        /**
		 * Indicates that a connection to the specified channel has been dropped.
		 * @param channel The channel which has been disconnected.
		 */
        public void connectionDropped(Channel channel) {
            _lastChannelEventTime = new Date();
        }
    }
}
