package gov.sns.apps.orbitcorrect;

import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.BPM;
import gov.sns.ca.*;
import gov.sns.ca.correlator.*;
import gov.sns.tools.correlator.*;
import gov.sns.tools.messaging.MessageCenter;
import java.util.*;

/**
 * BpmAgent represents a live BPM which may be connected and monitored.
 * @author   tap
 * @since    Jan 7, 2004
 */
public class BpmAgent implements RepRateListener {

    /** default time window in seconds for correlating this BPM's signal events */
    public static final double DEFAULT_CORRELATION_WINDOW = 10.0;

    /** default amplitude threshold below which this BPM signal is filtered out */
    protected static final double DEFAULT_AMPLITUDE_THRESHOLD = 10.0;

    /** BPM for which this is the agent */
    protected final BPM _bpm;

    /** event message center */
    protected final MessageCenter MESSAGE_CENTER;

    /** proxy for posting channel events */
    protected final BpmEventListener EVENT_PROXY;

    /** channel for xAvg */
    protected Channel _xAvgChannel;

    /** channel for yAvg */
    protected Channel _yAvgChannel;

    /** channel for amplitude average */
    protected Channel _ampAvgChannel;

    /** map of channels keyed by the corresponding BPM handles */
    protected Map<String, Channel> _channelTable;

    /** signal correlator */
    protected ChannelCorrelator _correlator;

    /** last record */
    protected BpmRecord _lastRecord;

    /** synchronize events */
    protected Object _eventLock;

    /**
	 * Primary constructor
	 * @param bpm                the BPM to monitor
	 * @param correlationWindow  the time in seconds for resolving correlated signals
	 */
    public BpmAgent(final BPM bpm, final double correlationWindow) {
        _bpm = bpm;
        MESSAGE_CENTER = new MessageCenter();
        EVENT_PROXY = MESSAGE_CENTER.registerSource(this, BpmEventListener.class);
        _eventLock = new Object();
        _lastRecord = null;
        if (isAvailable()) {
            monitorSignals(correlationWindow);
        }
    }

    /**
	 * Constructor
	 * @param bpm  the BPM to monitor
	 */
    public BpmAgent(final BPM bpm) {
        this(bpm, DEFAULT_CORRELATION_WINDOW);
    }

    /**
	 * Get the BPM ID.
	 * @return the unique BPM ID
	 */
    public String getID() {
        return _bpm.getId();
    }

    /**
	 * Add the specified listener as a receiver of BPM events
	 * @param listener  the listener to receive BPM events
	 */
    public void addBpmEventListener(BpmEventListener listener) {
        MESSAGE_CENTER.registerTarget(listener, this, BpmEventListener.class);
        synchronized (_eventLock) {
            listener.connectionChanged(this, BPM.X_AVG_HANDLE, _xAvgChannel.isConnected());
            listener.connectionChanged(this, BPM.Y_AVG_HANDLE, _yAvgChannel.isConnected());
            listener.connectionChanged(this, BPM.AMP_AVG_HANDLE, _ampAvgChannel.isConnected());
            BpmRecord lastRecord = _lastRecord;
            if (lastRecord != null) {
                listener.stateChanged(this, lastRecord);
            }
        }
    }

    /**
	 * Remove the specified listener from receiving BPM events
	 * @param listener  the listener to remove from receiving BPM events
	 */
    public void removeBpmEventListener(BpmEventListener listener) {
        MESSAGE_CENTER.removeTarget(listener, this, BpmEventListener.class);
    }

    /**
	 * Get the BPM manage by this agent
	 * @return   the BPM managed by this agent
	 */
    public BPM getBPM() {
        return _bpm;
    }

    /**
	 * Determine if this BPM is valid and has a good status.
	 * @return   true if this BPM has a good status and is valid; false otherwise.
	 */
    public boolean isAvailable() {
        return _bpm.getStatus() && _bpm.getValid();
    }

    /**
	 * Determine if this BPM's channels are all connected.
	 * @return   true if this BPM is connected and false if not.
	 */
    public boolean isConnected() {
        try {
            return _xAvgChannel.isConnected() && _yAvgChannel.isConnected() && _ampAvgChannel.isConnected();
        } catch (NullPointerException exception) {
            return false;
        }
    }

    /**
	 * Determine if this BPM is available and all of its channels are connected.
	 * @return   true if this BPM is online and false if not.
	 */
    public boolean isOnline() {
        return isConnected();
    }

    /**
	 * Get the latest record.
	 * @return the latest BPM record
	 */
    public BpmRecord getLatestRecord() {
        return _lastRecord;
    }

    /**
	 * Get the position of the BPM relative to the start of the specified sequence.
	 * @param sequence  The sequence relative to which the BPM's position is measured
	 * @return          the position of the BPM relative to the sequence in meters
	 */
    public double getPositionIn(AcceleratorSeq sequence) {
        return sequence.getPosition(_bpm);
    }

    /**
	 * Get the string representation of the BPM.
	 * @return   the BPM's string representation
	 */
    @Override
    public String toString() {
        return _bpm.toString();
    }

    /**
	 * Monitor and correlated the xAvg, yAvg and ampAvg signals for the BPM.
	 * @param binTimespan  the timespan for the correlation bin
	 */
    protected void monitorSignals(double binTimespan) {
        if (_correlator != null) {
            _correlator.dispose();
        }
        _correlator = new ChannelCorrelator(binTimespan);
        _channelTable = new HashMap<String, Channel>(3);
        _xAvgChannel = monitorChannel(BPM.X_AVG_HANDLE);
        _yAvgChannel = monitorChannel(BPM.Y_AVG_HANDLE);
        _ampAvgChannel = monitorChannel(BPM.AMP_AVG_HANDLE, null);
        _correlator.addListener(new CorrelationNotice() {

            final String X_AVG_ID = _xAvgChannel.getId();

            final String Y_AVG_ID = _xAvgChannel.getId();

            final String AMP_AVG_ID = _ampAvgChannel.getId();

            /**
				 * Handle the correlation event. This method gets called when a correlation was posted.
				 * @param sender       The poster of the correlation event.
				 * @param correlation  The correlation that was posted.
				 */
            public void newCorrelation(Object sender, Correlation correlation) {
                final Date timestamp = correlation.meanDate();
                final double xAvg = getValue(BPM.X_AVG_HANDLE, correlation);
                if (xAvg == Double.NaN) {
                    return;
                }
                final double yAvg = getValue(BPM.Y_AVG_HANDLE, correlation);
                if (yAvg == Double.NaN) {
                    return;
                }
                final double ampAvg = getValue(BPM.AMP_AVG_HANDLE, correlation);
                if (ampAvg == Double.NaN) {
                    return;
                }
                final BpmRecord record = new BpmRecord(BpmAgent.this, timestamp, xAvg, yAvg, ampAvg);
                synchronized (_eventLock) {
                    _lastRecord = record;
                    EVENT_PROXY.stateChanged(BpmAgent.this, record);
                }
            }

            /**
				 * Handle the no correlation event. This method gets called when no correlation was found within some prescribed time period.
				 * @param sender  The poster of the "no correlation" event.
				 */
            public void noCorrelationCaught(Object sender) {
                System.out.println("No BPM event.");
            }

            /**
				 * Get the value for the specified field from the correlation.
				 * @param handle       the handle of the BPM field
				 * @param correlation  the correlation with the correlated data for the BPM event
				 * @return             the correlation's BPM field value corresponding to the handle
				 */
            private double getValue(final String handle, final Correlation correlation) {
                final String channelID = getChannel(handle).getId();
                final ChannelTimeRecord record = (ChannelTimeRecord) correlation.getRecord(channelID);
                return (record != null) ? record.doubleValue() : Double.NaN;
            }
        });
        _correlator.startMonitoring();
    }

    /**
	 * Connect to the channel and monitor it with the correlator.
	 * @param handle  the handle of the channel to monitor with the correlator.
	 * @param filter  the channel's record filter for the correlation.
	 * @return        the channel for which the monitor was requested
	 */
    protected Channel monitorChannel(final String handle, final RecordFilter filter) {
        Channel channel = _bpm.getChannel(handle);
        _channelTable.put(handle, channel);
        correlateSignal(channel, filter);
        channel.addConnectionListener(new ConnectionListener() {

            /**
				 * Indicates that a connection to the specified channel has been established.
				 * @param channel  The channel which has been connected.
				 */
            public void connectionMade(Channel channel) {
                synchronized (_eventLock) {
                    _lastRecord = null;
                    correlateSignal(channel, filter);
                    EVENT_PROXY.connectionChanged(BpmAgent.this, handle, true);
                }
            }

            /**
				 * Indicates that a connection to the specified channel has been dropped.
				 * @param channel  The channel which has been disconnected.
				 */
            public void connectionDropped(Channel channel) {
                synchronized (_eventLock) {
                    _lastRecord = null;
                    EVENT_PROXY.connectionChanged(BpmAgent.this, handle, false);
                }
            }
        });
        if (!channel.isConnected()) {
            channel.requestConnection();
        }
        return channel;
    }

    /**
	 * Connect to the channel and monitor it with the correlator. A null record filter is used for the channel.
	 * @param handle  the handle of the channel to monitor with the correlator.
	 * @return        the channel for which the monitor was requested
	 */
    protected Channel monitorChannel(final String handle) {
        return monitorChannel(handle, null);
    }

    /**
	 * Monitor the channel with the correlator.
	 * @param channel  the channel to monitor with the correlator.
	 * @param filter   the channel's record filter for the correlation.
	 */
    protected void correlateSignal(final Channel channel, final RecordFilter filter) {
        if (!_correlator.hasSource(channel.getId()) && channel.isConnected()) {
            _correlator.addChannel(channel, filter);
        }
    }

    /**
	 * Get this agent's channel corresponding to the specified handle.
	 * @param handle  Description of the Parameter
	 * @return        The channel value
	 */
    public Channel getChannel(final String handle) {
        return _channelTable.get(handle);
    }

    /**
	 * Notification that the rep-rate has changed.
	 * @param monitor  The monitor announcing the new rep-rate.
	 * @param repRate  The new rep-rate.
	 */
    public void repRateChanged(RepRateMonitor monitor, double repRate) {
        if (_correlator != null) {
            double timeWindow = (!Double.isNaN(repRate) && (repRate > 0) && (repRate < 10000)) ? 0.5 / repRate : DEFAULT_CORRELATION_WINDOW;
            _correlator.setBinTimespan(timeWindow);
        }
    }
}
