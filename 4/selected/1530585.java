package gov.sns.services.mpstool;

import gov.sns.tools.correlator.*;
import gov.sns.ca.correlator.*;
import gov.sns.ca.*;
import gov.sns.tools.logbook.ElogUtility;
import java.util.*;
import java.text.*;

/**
 * MPSMonitor
 * @author    jdg
 * @author    tap
 * @created   March 12, 2004
 */
public class MPSMonitor {

    /** format for displaying timestamps */
    private static final DateFormat TIMESTAMP_FORMAT;

    /** size of the MPS event buffer */
    public static final int MPS_EVENT_BUFFER_SIZE = 1000;

    /** Chanenl wrappers */
    protected volatile ChannelWrapper[] _mpsChannelWrappers;

    /** Map of input monitors keyed by MPS signal */
    protected Map _inputMonitors;

    /** Type of MPS signals to monitor (e.g. FPL or FPAR) */
    protected String _mpsType;

    /** Source of MPS signals */
    protected SignalSource _signalSource;

    /** the correlator to use to gather MPS signals in a single macropulse */
    private ChannelCorrelator _correlator;

    /** the ordered list of most recent MPS events sorted by timestamp */
    private volatile LinkedList _mpsEventBuffer;

    /** Filter used to set the amount of missing MPS PVs allowed to constitute a legitimate correlation set */
    private CorrelationFilter _filter;

    /** The poster to grab + post correlations every 60 Hz */
    private PeriodicPoster _poster;

    /** time to wait while monitoring a correlated set (sec) */
    private Double _dwellTime;

    /** max timeStamp difference to consitute a correlated set (sec) */
    private Double _deltaT;

    /**
	 * Map keyed by PV signal specifying the number of times the PV has been the first MPS PV to
	 * trip among a group of correlated MPS trips. This map gets cleared daily.
	 */
    private Map _firstHitStats;

    /** Map keyed by PV signal specifying the number of times the MPS PV has tripped. This map gets cleared daily. */
    private Map _mpsTripStats;

    /** time of last MPS event */
    private volatile Date _lastMPSEventTime;

    /** time of last MPS channel connection event */
    private volatile Date _lastMPSConnectionEventTime;

    /** time of last Input channel connection event */
    private volatile Date _lastInputConnectionEventTime;

    /** handler of connection events of MPS channels for this monitor */
    private MPSConnectionHandler _mpsConnectionHandler;

    /** handler of connection events of input channels for this monitor */
    private InputConnectionHandler _inputConnectionHandler;

    /** The start time for populating daily statistics */
    protected Calendar _startTime;

    /** Synchronization lock for accessing daily statistics */
    protected Object _statsLock;

    /** Timer for updating daily statistics */
    protected final Timer _statsUpdateTimer;

    static {
        TIMESTAMP_FORMAT = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
    }

    /**
	 * Primary Constructor
	 * @param mpsType       MPS Type (e.g. FPL or FPAR)
	 * @param signalSource  Data source that supplies MPS signals
	 */
    public MPSMonitor(String mpsType, SignalSource signalSource) {
        _deltaT = new Double(0.016);
        _dwellTime = new Double(0.1);
        _mpsEventBuffer = new LinkedList();
        _firstHitStats = new HashMap();
        _mpsTripStats = new HashMap();
        _mpsType = mpsType;
        _signalSource = signalSource;
        _statsLock = new Object();
        _startTime = Calendar.getInstance();
        _lastMPSEventTime = _startTime.getTime();
        _lastMPSConnectionEventTime = _startTime.getTime();
        _lastInputConnectionEventTime = _startTime.getTime();
        _statsUpdateTimer = startStatsUpdateTimer();
        _inputConnectionHandler = new InputConnectionHandler();
        loadSignals();
        setupCorrelator();
        restartCorrelator();
        System.out.println("MPS Monitor is running for " + mpsType);
    }

    /** Dispose of this monitor and its resources */
    public void dispose() {
        stopCorrelator();
        _statsUpdateTimer.cancel();
        checkDayUpdateDailyStats();
    }

    /**
	 * Get the named MPS type ("FPL" or "FPAR") of this monitor
	 *
	 * @return   The type of MPS signal to monitor (e.g. "FPL" or "FPAR")
	 */
    public String getMPSType() {
        return _mpsType;
    }

    /** Load the signals to monitor from the data source */
    public void loadSignals() {
        String[] signals = _signalSource.fetchMPSSignals(_mpsType);
        _mpsChannelWrappers = new ChannelWrapper[signals.length];
        for (int index = 0; index < signals.length; index++) {
            _mpsChannelWrappers[index] = new ChannelWrapper(signals[index]);
        }
        _lastMPSConnectionEventTime = new Date();
        _inputMonitors = _signalSource.fetchInputMonitors(_mpsType);
        if (_inputMonitors != null) {
            _inputConnectionHandler.requestConnections(_inputMonitors.values());
        }
        _lastInputConnectionEventTime = new Date();
    }

    /**
	 * Reload the signals to monitor from the data source. Stop the correlator, recreate a new
	 * correlator with the new signals and restart the correlator.
	 */
    public void reloadSignals() {
        stopCorrelator();
        _mpsConnectionHandler.ignoreAll(_mpsChannelWrappers);
        _correlator.dispose();
        _inputConnectionHandler.ignoreAll(_inputMonitors.values());
        loadSignals();
        setupCorrelator();
        restartCorrelator();
    }

    /** set up the correlator from the PV list */
    private void setupCorrelator() {
        _filter = CorrelationFilterFactory.minCountFilter(1);
        _correlator = new ChannelCorrelator(_deltaT.doubleValue(), _filter);
        RecordFilter recordFilter = RecordFilterFactory.equalityDoubleFilter(0.0);
        _mpsConnectionHandler = new MPSConnectionHandler(recordFilter);
        for (int index = 0; index < _mpsChannelWrappers.length; index++) {
            final ChannelWrapper wrapper = _mpsChannelWrappers[index];
            _mpsConnectionHandler.requestToCorrelate(wrapper);
        }
        _poster = new PeriodicPoster(_correlator, _dwellTime.doubleValue());
        _poster.addCorrelationNoticeListener(new CorrelationNotice() {

            /**
				 * handle no correlation found events
				 *
				 * @param sender  - the provider of the correlation
				 */
            public void noCorrelationCaught(Object sender) {
            }

            /**
				 * Handle a correlation event by logging the correlated MPS events
				 *
				 * @param sender       - the provider of the correlation
				 * @param correlation  - the correlation object containing the answer !
				 */
            public synchronized void newCorrelation(Object sender, Correlation correlation) {
                checkDayUpdateDailyStats();
                MPSEvent newEvent = new MPSEvent(correlation);
                updateEventBuffer(newEvent);
                updateStats(newEvent);
                _lastMPSEventTime = newEvent.getTimestamp();
            }
        });
    }

    /**
	 * Get the correlator
	 * @return   The correlator which correlates MPS events
	 */
    public ChannelCorrelator getCorrelator() {
        return _correlator;
    }

    /**
	 * set the dwell time between correlate attempts
	 * @param aTime  new dwell time (sec)
	 */
    public void setDwellTime(Double aTime) {
        _poster.setPeriod(aTime.doubleValue());
        _dwellTime = aTime;
    }

    /**
	 * get the dwell time between correlate attempts
	 * @return   dwell time in seconds
	 */
    public Double getDwellTime() {
        return _dwellTime;
    }

    /**
	 * set the time window for correlated data
	 * @param delta  correlation time window (sec)
	 */
    public void setDeltaT(Double delta) {
        _deltaT = delta;
        _correlator.setBinTimespan(delta.doubleValue());
    }

    /**
	 * get the time window to define a correlation
	 * @return   The correlation time window in seconds
	 */
    public Double getDeltaT() {
        return _deltaT;
    }

    /**
	 * Get the array of MPS channel wrappers
	 * @return   the array of monitored MPS channel wrappers
	 */
    public ChannelWrapper[] getMPSChannelWrappers() {
        return _mpsChannelWrappers;
    }

    /**
	 * Get the collection of input monitors
	 * @return   the collection of input monitors
	 */
    public Collection getInputMonitors() {
        return _inputMonitors.values();
    }

    /**
	 * Get the input monitor corresponding to the specified MPS signal.
	 * @param mpsSignal  The MPS signal for which to get the corresponding input monitor
	 * @return           the input monitor for the MPS signal or null if none exists
	 */
    public InputMonitor getInputMonitor(final String mpsSignal) {
        return (InputMonitor) _inputMonitors.get(mpsSignal);
    }

    /**
	 * Get the input signal corresponding to the specified MPS signal.
	 * @param mpsSignal  The MPS signal for which to get the corresponding input signal
	 * @return           the input signal for the MPS signal or null if none exists
	 */
    public String getInputSignal(final String mpsSignal) {
        final InputMonitor inputMonitor = getInputMonitor(mpsSignal);
        return inputMonitor != null ? inputMonitor.getSignal() : null;
    }

    /**
	 * Determing if the correlator is running.
	 * @return   true if the correlator is running and false otherwise.
	 */
    public boolean isRunning() {
        return _correlator.isRunning();
    }

    /**
	 * Determing if the poster is running.
	 * @return   true if the poster is running and false otherwise.
	 */
    public boolean isPosting() {
        return _poster.isRunning();
    }

    /** Stops the poster from posting, but the correlator is still running behind the scene */
    public void pausePoster() {
        _poster.stop();
    }

    /** Stop looking for MPS trips */
    public void stopCorrelator() {
        _poster.stop();
        _correlator.stopMonitoring();
    }

    /** Restart the poster after a pause */
    public void restartCorrelator() {
        _correlator.startMonitoring();
        _poster.start();
    }

    /**
	 * Get the timestamp of the latest MPS event. This can be used by clients to determine if
	 * their first hit log is current.
	 * @return   the wall clock timestamp of the latest MPS event
	 */
    public Date getLastMPSEventTime() {
        checkDayUpdateDailyStats();
        return _lastMPSEventTime;
    }

    /**
	 * Get the timestamp of the last MPS channel event. This can be used by clients to determine if
	 * their list of channels is up to date. This timestamp changes when the list of monitored
	 * channels changes or the connection state of one or more monitored channels changes.
	 * @return   the wall clock timestamp of the latest channel event.
	 */
    public Date getLastMPSChannelEventTime() {
        return _lastMPSConnectionEventTime;
    }

    /**
	 * Get the timestamp of the last MPS channel event. This can be used by clients to determine if
	 * their list of channels is up to date. This timestamp changes when the list of monitored
	 * channels changes or the connection state of one or more monitored channels changes.
	 * @return   the wall clock timestamp of the latest channel event.
	 */
    public Date getLastInputChannelEventTime() {
        return _lastInputConnectionEventTime;
    }

    /**
	 * Update the circular buffer of MPS events to include the latest event
	 * @param newEvent  the latest MPS event
	 */
    private void updateEventBuffer(final MPSEvent newEvent) {
        synchronized (_mpsEventBuffer) {
            _mpsEventBuffer.addFirst(newEvent);
            while (_mpsEventBuffer.size() > MPS_EVENT_BUFFER_SIZE) {
                _mpsEventBuffer.removeLast();
            }
        }
    }

    /**
	 * Updates the daily statistics of MPS trips.
	 * @param newEvent  The new MPS event to include in the statistics
	 */
    private void updateStats(final MPSEvent newEvent) {
        synchronized (_statsLock) {
            updateFirstHitStats(newEvent);
            updateMPSTripStats(newEvent);
        }
    }

    /**
	 * Update the first hit statistics to include the new MPS event.
	 * @param newEvent  The new MPS event to include in the daily statistics.
	 */
    private void updateFirstHitStats(final MPSEvent newEvent) {
        synchronized (_statsLock) {
            final String firstPV = newEvent.getFirstSignalEvent().getSignal();
            incrementFirstHits(firstPV);
        }
    }

    /**
	 * Increment the number of times the MPS signal has been the first to trip in correlated MPS trips.
	 * The stats are only valid for the present day.
	 * @param mpsPV   The MPS PV for which to increment the first hit trips
	 */
    protected final void incrementFirstHits(final String mpsPV) {
        synchronized (_statsLock) {
            TripStatistics stats = (TripStatistics) _firstHitStats.get(mpsPV);
            if (stats == null) {
                stats = getMPSTripStats(mpsPV);
                _firstHitStats.put(mpsPV, stats);
            }
            stats.incrementFirstHits();
        }
    }

    /**
	 * Incrment the number of times the MPS signal tripped.
	 * The stats are only valid for the present day.
	 * @param mpsPV   The MPS PV for which to increment the trips
	 */
    protected final void incrementMPSTrips(final String mpsPV) {
        synchronized (_statsLock) {
            getMPSTripStats(mpsPV).incrementMPSTrips();
        }
    }

    /**
	 * Incrment the number of times the MPS signal's input has tripped.
	 * The stats are only valid for the present day.
	 * @param mpsPV   The MPS PV for which to increment the input statistics
	 */
    protected final void incrementInputTrips(final String mpsPV) {
        synchronized (_statsLock) {
            getMPSTripStats(mpsPV).incrementInputTrips();
        }
    }

    /**
	 * Get the trip statistics for the specified MPS PV.
	 * @param mpsPV the PV for which to retrieve the trip statistics 
	 * @return the trip statistics for the specified MPS PV
	 */
    protected final TripStatistics getMPSTripStats(final String mpsPV) {
        synchronized (_statsLock) {
            TripStatistics stats = (TripStatistics) _mpsTripStats.get(mpsPV);
            if (stats == null) {
                stats = new TripStatistics(mpsPV, getInputSignal(mpsPV));
                _mpsTripStats.put(mpsPV, stats);
            }
            return stats;
        }
    }

    /**
	 * Update the statistics that maintains the number of MPS trips per MPS signal to include the
	 * specified MPS event.
	 * @param mpsEvent  The new MPS event to include in the statistics.
	 */
    private void updateMPSTripStats(final MPSEvent mpsEvent) {
        synchronized (_statsLock) {
            final List signalEvents = mpsEvent.getSignalEvents();
            final List inputMonitors = new ArrayList(signalEvents.size());
            final Iterator eventIter = signalEvents.iterator();
            while (eventIter.hasNext()) {
                final SignalEvent signalEvent = (SignalEvent) eventIter.next();
                final String signal = signalEvent.getSignal();
                InputMonitor inputMonitor = getInputMonitor(signal);
                if (inputMonitor != null) {
                    inputMonitor.requestValueUpdate();
                    inputMonitors.add(inputMonitor);
                }
                incrementMPSTrips(signalEvent);
            }
            try {
                Thread.sleep(10);
                incrementInputTrips(inputMonitors);
            } catch (Exception exception) {
            }
        }
    }

    /**
	 * Get the number of times the MPS signal has tripped within the present day.
	 * @param signal  The MPS signal for which to get the trip count.
	 * @return        The number of times the MPS signal has tripped.
	 */
    public final int getMPSTripCount(final String signal) {
        synchronized (_statsLock) {
            return _mpsTripStats.containsKey(signal) ? ((Integer) _mpsTripStats.get(signal)).intValue() : 0;
        }
    }

    /**
	 * Increment the number of MPS trips for the specified signal event.
	 * @param signalEvent  The new signal event to include in the statistics.
	 */
    private void incrementMPSTrips(final SignalEvent signalEvent) {
        synchronized (_statsLock) {
            final String signal = signalEvent.getSignal();
            incrementMPSTrips(signal);
        }
    }

    /**
	 * Get the latest values for the specified input monitors. Check which inputs have tripped and
	 * increment their daily trip statistics accordingly.
	 * @param inputMonitors  the input monitors to check
	 */
    private void incrementInputTrips(final List inputMonitors) {
        Iterator monitorIter = inputMonitors.iterator();
        while (monitorIter.hasNext()) {
            final InputMonitor monitor = (InputMonitor) monitorIter.next();
            if (monitor.isInputTripped()) {
                final String mpsSignal = monitor.getMPSPV();
                incrementInputTrips(mpsSignal);
            }
        }
    }

    /**
	 * Check to see if the day has changed since the the startTime. If so, reset the startTime to
	 * be the start of the new day and clear the daily statistics.
	 */
    private void checkDayUpdateDailyStats() {
        synchronized (_statsLock) {
            int today = Calendar.getInstance().get(Calendar.DATE);
            int startDay = _startTime.get(Calendar.DATE);
            if (today != startDay) {
                publishDailyStats();
                resetDailyStats();
            }
        }
    }

    /** Publish the latest daily stats.  */
    private void publishDailyStats() {
        publishDailyStatsToLogbook();
        publishDailyStatsToDatabase();
    }

    /** Publish the latest daily stats.  */
    private void publishDailyStatsToLogbook() {
        try {
            final String firstHitText = getFirstHitText();
            final String mpsTripSummary = getMPSTripSummary();
            final String summary = firstHitText + "\n\n\n" + mpsTripSummary;
            final String title = "MPS " + _mpsType + " Daily Statistics";
            ElogUtility.defaultUtility().postEntry(ElogUtility.CONTROLS_LOGBOOK, title, summary);
        } catch (Exception exception) {
            System.err.println("Exception while publishing daily stats to logbook: " + exception);
        }
    }

    /** Publish the latest daily stats.  */
    private void publishDailyStatsToDatabase() {
        try {
            final Collection stats = _mpsTripStats.values();
            _signalSource.publishDailyStatistics(_startTime.getTime(), stats);
        } catch (Exception exception) {
            System.err.println("Exception while publishing daily stats to database: " + exception);
        }
    }

    /** Reset the daily statistics by clearing them and setting the startTime to the beginning of the day. */
    private synchronized void resetDailyStats() {
        synchronized (_statsLock) {
            _firstHitStats.clear();
            _mpsTripStats.clear();
            Calendar newStartTime = Calendar.getInstance();
            _startTime = new GregorianCalendar(newStartTime.get(Calendar.YEAR), newStartTime.get(Calendar.MONTH), newStartTime.get(Calendar.DATE));
            _lastMPSEventTime = new Date();
        }
    }

    /**
	 * Get the buffer of MPS events
	 * @return   the buffer of MPS events
	 */
    public List getMPSEventBuffer() {
        synchronized (_mpsEventBuffer) {
            return new ArrayList(_mpsEventBuffer);
        }
    }

    /**
	 * Get the list of MPS events which have occured since the specified time. Events which occur
	 * before or at the specified time are excluded from the list.
	 * @param time  The time since which we wish to get events
	 * @return      the list of events since the specified time
	 */
    public List getMPSEventsSince(final Date time) {
        synchronized (_mpsEventBuffer) {
            final int count = _mpsEventBuffer.size();
            int index;
            for (index = 0; index < count; index++) {
                MPSEvent event = (MPSEvent) _mpsEventBuffer.get(index);
                if (!event.getTimestamp().after(time)) {
                    break;
                }
            }
            return _mpsEventBuffer.subList(0, index);
        }
    }

    /**
	 * get the first hit statistics as a String
	 * @return   the first hit statistics as a string
	 */
    public String getFirstHitText() {
        synchronized (_statsLock) {
            if (_firstHitStats.isEmpty()) {
                return "No MPS events since " + TIMESTAMP_FORMAT.format(_startTime.getTime());
            }
            Date now = new Date();
            String stats = "MPS First Hit stats since " + TIMESTAMP_FORMAT.format(_startTime.getTime()) + "\n\n";
            final List records = new ArrayList(_firstHitStats.values());
            Collections.sort(records, TripStatistics.firstHitComparator());
            Collections.reverse(records);
            for (Iterator iterator = records.iterator(); iterator.hasNext(); ) {
                TripStatistics fhc = (TripStatistics) iterator.next();
                stats += fhc.getMPSPV() + ", counts = " + fhc.getMPSTrips() + "\n";
            }
            return stats;
        }
    }

    /**
	 * Get a summary of MPS trips as a string.
	 * @return   a summary of MPS trips
	 */
    public String getMPSTripSummary() {
        synchronized (_statsLock) {
            if (_mpsTripStats.isEmpty()) {
                return "No MPS trips since " + TIMESTAMP_FORMAT.format(_startTime.getTime());
            }
            Date now = new Date();
            StringBuffer stats = new StringBuffer("MPS trip summary since " + TIMESTAMP_FORMAT.format(_startTime.getTime()));
            stats.append("\n\n");
            final List records = new ArrayList(_mpsTripStats.values());
            Collections.sort(records, TripStatistics.mpsTripComparator());
            Collections.reverse(records);
            for (Iterator iterator = records.iterator(); iterator.hasNext(); ) {
                final TripStatistics tripStats = (TripStatistics) iterator.next();
                final String mpsSignal = tripStats.getMPSPV();
                stats.append(mpsSignal + ", trips = " + tripStats.getMPSTrips());
                final String inputPV = tripStats.getInputSignal();
                if (inputPV != null) {
                    final int inputTrips = tripStats.getInputTrips();
                    stats.append(", with input " + inputPV + ", trips = " + inputTrips);
                }
                stats.append("\n");
            }
            return stats.toString();
        }
    }

    /**
	 * Create and start a new timer for checking every hour whether to update the daily statistics.
	 * @return   a new timer for scheduling daily statistics updates.
	 */
    public Timer startStatsUpdateTimer() {
        final long period = 3600 * 1000;
        Calendar now = Calendar.getInstance();
        Calendar startTime = Calendar.getInstance();
        startTime.clear();
        startTime.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DATE), now.get(Calendar.HOUR), 1);
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            /** update the daily statistics */
            @Override
            public void run() {
                checkDayUpdateDailyStats();
            }
        }, startTime.getTime(), period);
        return timer;
    }

    /**
	 * Handle connection events from monitored MPS channels.
	 * @author    t6p
	 * @created   March 12, 2004
	 */
    protected class MPSConnectionHandler implements ConnectionListener {

        /** Record filter to use for connected channels with the correlator */
        protected RecordFilter _recordFilter;

        /**
		 * Constructor
		 * @param recordFilter  The record filter to apply to connected channels with the correlator
		 */
        public MPSConnectionHandler(final RecordFilter recordFilter) {
            _recordFilter = recordFilter;
        }

        /**
		 * Request that the channel monitor events be correlated within the specified correlator.
		 * @param wrapper  The channel wrapper to correlate
		 */
        public void requestToCorrelate(ChannelWrapper wrapper) {
            wrapper.addConnectionListener(this);
            if (wrapper.isConnected()) {
                connectionMade(wrapper.getChannel());
            } else {
                wrapper.requestConnection();
            }
        }

        /**
		 * Ignore connection events from the specified wrapper
		 * @param wrapper  the wrapper from which to ignore connection events
		 */
        public void ignore(ChannelWrapper wrapper) {
            wrapper.removeConnectionListener(this);
        }

        /**
		 * Ignore connection events from each channel in the wrappers array
		 * @param wrappers  The array of wrappers to ignore
		 */
        public void ignoreAll(final ChannelWrapper[] wrappers) {
            for (int index = 0; index < wrappers.length; index++) {
                ignore(wrappers[index]);
            }
        }

        /**
		 * Indicates that a connection to the specified channel has been established. Add the channel
		 * to the correlator with the record filter. Update the connection event timestamp.
		 * @param channel  The channel which has been connected.
		 */
        public void connectionMade(Channel channel) {
            if (!_correlator.hasSource(channel.channelName())) {
                _correlator.addChannel(channel, _recordFilter);
            }
            _lastMPSConnectionEventTime = new Date();
        }

        /**
		 * Indicates that a connection to the specified channel has been dropped. Update the
		 * connection event timestamp.
		 * @param channel  The channel which has been disconnected.
		 */
        public void connectionDropped(Channel channel) {
            _lastMPSConnectionEventTime = new Date();
        }
    }

    /**
	 * Handle connection events from monitored channels.
	 * @author    t6p
	 * @created   March 12, 2004
	 */
    protected class InputConnectionHandler implements ConnectionListener {

        /**
		 * Request a connection for the channel wrapper and monitor its connection events.
		 * @param wrapper  The channel wrapper to correlate
		 */
        public void requestConnection(final ChannelWrapper wrapper) {
            wrapper.addConnectionListener(this);
            wrapper.requestConnection();
        }

        /**
		 * Request connections for the channel wrappers and monitor their connection events.
		 * @param wrapper  The channel wrapper to correlate
		 */
        public void requestConnections(final Collection wrappers) {
            for (Iterator iter = wrappers.iterator(); iter.hasNext(); ) {
                ChannelWrapper wrapper = (ChannelWrapper) iter.next();
                requestConnection(wrapper);
            }
            Channel.flushIO();
        }

        /**
		 * Ignore connection events from the specified wrapper
		 * @param wrapper  the wrapper from which to ignore connection events
		 */
        public void ignore(final ChannelWrapper wrapper) {
            wrapper.removeConnectionListener(this);
        }

        /**
		 * Ignore connection events from each channel in the wrappers array
		 * @param wrappers  The collection of wrappers to ignore
		 */
        public void ignoreAll(final Collection wrappers) {
            if (wrappers == null) return;
            for (Iterator iter = wrappers.iterator(); iter.hasNext(); ) {
                final ChannelWrapper wrapper = (ChannelWrapper) iter.next();
                ignore(wrapper);
            }
        }

        /**
		 * Indicates that a connection to the specified channel has been established. Add the channel
		 * to the correlator with the record filter. Update the connection event timestamp.
		 * @param channel  The channel which has been connected.
		 */
        public void connectionMade(final Channel channel) {
            _lastInputConnectionEventTime = new Date();
        }

        /**
		 * Indicates that a connection to the specified channel has been dropped. Update the
		 * connection event timestamp.
		 * @param channel  The channel which has been disconnected.
		 */
        public void connectionDropped(final Channel channel) {
            _lastInputConnectionEventTime = new Date();
        }
    }
}
