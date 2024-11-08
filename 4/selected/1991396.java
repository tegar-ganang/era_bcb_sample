package gov.sns.xal.tools.orbit;

import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.BPM;
import gov.sns.ca.*;
import gov.sns.ca.correlator.*;
import gov.sns.tools.correlator.*;
import gov.sns.tools.messaging.*;
import java.util.*;

/**
 * BpmSource correlates the signals for a single BPM and is a source to 
 * an orbit correlator which correlates several BPMs.
 * 
 * @author  tap
 */
class BpmSource extends SourceAgent implements CorrelationNotice {

    protected BPM bpm;

    protected ConnectionHandler _connectionHandler;

    protected ChannelCorrelator bpmCorrelator;

    /** Creates a new instance of BpmSource */
    public BpmSource(MessageCenter messageCenter, BPM aBpm, String name, BpmFilter aFilter, double binSpan, CorrelationTester tester) {
        super(messageCenter, name, aFilter, tester);
        bpm = aBpm;
        correlateSignals(binSpan, aFilter);
    }

    /** setup the underlying correlator to correlate signals for the bpm */
    protected void correlateSignals(double binSpan, BpmFilter bpmFilter) {
        CorrelationFilter correlationFilter = null;
        if (bpmFilter != null) {
            correlationFilter = CorrelationFilterFactory.correlationFilter(bpmFilter);
        }
        bpmCorrelator = new ChannelCorrelator(binSpan, correlationFilter);
        bpmCorrelator.addListener(this);
        _connectionHandler = new ConnectionHandler(bpmCorrelator);
        addHandle(bpmFilter, BPM.X_AVG_HANDLE);
        addHandle(bpmFilter, BPM.Y_AVG_HANDLE);
        addHandle(bpmFilter, BPM.AMP_AVG_HANDLE);
    }

    /** convenience method for adding the handle for the bpm */
    protected void addHandle(BpmFilter bpmFilter, String handle) {
        try {
            Channel channel = bpm.getChannel(handle);
            RecordFilter recordFilter = bpmFilter.filterForHandle(handle);
            _connectionHandler.requestToCorrelate(channel, recordFilter);
        } catch (NoSuchChannelException exception) {
            System.err.println(exception);
            exception.printStackTrace();
        }
    }

    /** implement setupEventHandler() for SourceAgent */
    @Override
    protected void setupEventHandler(RecordFilter recordFilter) {
    }

    /** implement startMonitor() for SourceAgent */
    @Override
    public boolean startMonitor() {
        bpmCorrelator.startMonitoring();
        return true;
    }

    /** implement stopMonitor() for SourceAgent */
    @Override
    public void stopMonitor() {
        bpmCorrelator.stopMonitoring();
    }

    /** implement CorrelationNotice */
    public synchronized void newCorrelation(Object sender, Correlation correlation) {
        BpmResult result = new BpmResult(bpm, correlation);
        System.out.println(bpm + " correlation: " + result);
        postEvent(result, result.getTimestamp());
    }

    /** implement CorrelationNotice */
    public synchronized void noCorrelationCaught(Object sender) {
        System.out.println("no BPM correlation...");
    }
}

/**
 * Handle connection events from monitored channels.
 */
class ConnectionHandler implements ConnectionListener {

    /** correlator to which to add the connected channels */
    protected ChannelCorrelator _correlator;

    /** Table of record filters keyed by channel */
    protected Map<Channel, RecordFilter> _recordFilters;

    /**
	 * Constructor
	 * @param recordFilter The record filter to apply to connected channels with the correlator
	 */
    public ConnectionHandler(final ChannelCorrelator correlator) {
        _correlator = correlator;
        _recordFilters = new HashMap<Channel, RecordFilter>();
    }

    /**
	 * Get the record filter to use for the specified channel
	 * @return the record filter mapped to the specified channel
	 */
    private RecordFilter getRecordFilter(final Channel channel) {
        return _recordFilters.get(channel);
    }

    /**
	 * Request that the channel monitor events be correlated within the specified correlator.
	 * @param wrapper The channel wrapper to correlate
	 */
    public void requestToCorrelate(Channel channel, RecordFilter recordFilter) {
        System.out.println("Requesting correlation for channel:  " + channel.channelName());
        _recordFilters.put(channel, recordFilter);
        channel.addConnectionListener(this);
        if (channel.isConnected()) {
            connectionMade(channel);
        } else {
            channel.requestConnection();
        }
    }

    /**
	 * Ignore connection events from the specified wrapper
	 * @param wrapper the wrapper from which to ignore connection events
	 */
    public void ignore(Channel channel) {
        channel.removeConnectionListener(this);
    }

    /**
	 * Ignore connection events from each channel in the wrappers array
	 * @param wrappers The array of wrappers to ignore
	 */
    public void ignoreAll(final Channel[] channels) {
        for (int index = 0; index < channels.length; index++) {
            ignore(channels[index]);
        }
    }

    /**
	 * Indicates that a connection to the specified channel has been established.
	 * Add the channel to the correlator with the record filter.  Update the connection event
	 * timestamp.
	 * @param channel The channel which has been connected.
	 */
    public void connectionMade(Channel channel) {
        System.out.println("connection made for channel:  " + channel.channelName());
        if (!_correlator.hasSource(channel.channelName())) {
            RecordFilter recordFilter = getRecordFilter(channel);
            _correlator.addChannel(channel, recordFilter);
        }
    }

    /**
	 * Indicates that a connection to the specified channel has been dropped.
	 * Update the connection event timestamp.
	 * @param channel The channel which has been disconnected.
	 */
    public void connectionDropped(Channel channel) {
        System.out.println("connection dropped for channel:  " + channel.channelName());
    }
}
