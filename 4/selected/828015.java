package gov.sns.apps.orbitcorrect;

import gov.sns.ca.*;
import gov.sns.tools.correlator.*;
import gov.sns.xal.smf.*;
import java.util.*;

/**
 * LiveOrbitSource is an orbit source that generates Orbit instances from live channel access data.
 * @author   tap
 * @since    Jun 15, 2004
 */
public class LiveOrbitSource extends OrbitSource implements RepRateListener {

    /** handler of BPM events */
    protected BpmEventListener _bpmEventHandler;

    /** active orbit */
    protected MutableOrbit _activeOrbit;

    /** last orbit */
    protected Orbit _latestOrbit;

    /** accelertor rep-rate monitor */
    protected RepRateMonitor _repRateMonitor;

    /** correlator for monitoring BPM signals */
    protected BPMCorrelator _bpmCorrelator;

    /** poster of correlation events */
    protected PeriodicPoster _bpmPeriodicPoster;

    /** beam event channel */
    protected Channel _beamEventChannel;

    /** handler of BPM correlation events */
    protected BPMCorrelationHandler _bpmCorrelationHandler;

    /**
	 * Primary Constructor
	 * @param  label     Label for this orbit source.
	 * @param  sequence  The sequence for which this orbit source supplies orbits.
	 * @param  bpmAgents      The BPM agents to include in the Orbit.
	 */
    public LiveOrbitSource(final String label, final AcceleratorSeq sequence, final List<BpmAgent> bpmAgents) {
        super(label, sequence, bpmAgents);
    }

    /**
	 * Constructor
	 * @param label  Label for this orbit source.
	 */
    public LiveOrbitSource(final String label) {
        this(label, null, null);
    }

    /** Initial setup to create the busy lock. */
    @Override
    protected void setup() {
        _repRateMonitor = new RepRateMonitor();
        _repRateMonitor.addRepRateListener(this);
        _bpmCorrelator = new BPMCorrelator(BpmAgent.DEFAULT_CORRELATION_WINDOW);
        _bpmPeriodicPoster = new PeriodicPoster(_bpmCorrelator, 1.0);
        _bpmCorrelationHandler = new BPMCorrelationHandler();
        _bpmPeriodicPoster.addCorrelationNoticeListener(_bpmCorrelationHandler);
    }

    /** dispose of this instance's resources */
    @Override
    public void dispose() {
        removeBpmMonitors();
        _repRateMonitor.removeRepRateListener(this);
        _repRateMonitor.dispose();
        super.dispose();
    }

    /**
	 * Set the sequence and its BPM agents to monitor
	 * @param sequence  The new sequence value
	 * @param bpmAgents      the list of BPMs to monitor
	 */
    @Override
    public void setSequence(final AcceleratorSeq sequence, final List<BpmAgent> bpmAgents) {
        _busyLock.lock();
        try {
            removeBpmMonitors();
            Accelerator accelerator = (sequence != null) ? sequence.getAccelerator() : null;
            if (accelerator != getAccelerator()) {
                _repRateMonitor.setAccelerator(accelerator);
            }
            super.setSequence(sequence, bpmAgents);
            _activeOrbit = new MutableOrbit(sequence);
            _latestOrbit = _activeOrbit.getOrbit();
            if (bpmAgents != null) {
                monitorBpms();
                monitorBeamEvents();
            }
        } finally {
            _busyLock.unlock();
            _proxy.sequenceChanged(this, sequence, getBpmAgents());
        }
    }

    /**
	 * Get the latest orbit.
	 * @return   the latest orbit.
	 */
    @Override
    public Orbit getOrbit() {
        synchronized (_activeOrbit) {
            return _latestOrbit;
        }
    }

    /** Listen for BPM Agent events from this source's BPM agents */
    protected void monitorBpms() {
        for (final BpmAgent bpmAgent : _bpmAgents) {
            _bpmCorrelator.addBPM(bpmAgent);
            _repRateMonitor.addRepRateListener(bpmAgent);
        }
        _bpmCorrelator.startMonitoring();
        _bpmPeriodicPoster.start();
    }

    /** Stop listening for BPM Agent events from this source's BPM agents */
    protected void removeBpmMonitors() {
        if (_bpmAgents == null) {
            return;
        }
        _bpmPeriodicPoster.stop();
        _bpmCorrelator.stopMonitoring();
        for (final BpmAgent bpmAgent : _bpmAgents) {
            _bpmCorrelator.removeBPM(bpmAgent);
            _repRateMonitor.removeRepRateListener(bpmAgent);
        }
    }

    /**
	 * Notification that the rep-rate has changed.
	 * @param monitor  The monitor announcing the new rep-rate.
	 * @param repRate  The new rep-rate.
	 */
    public void repRateChanged(RepRateMonitor monitor, double repRate) {
        if (_bpmCorrelator != null) {
            double timeWindow = (!Double.isNaN(repRate) && (repRate > 0) && (repRate < 10000)) ? 0.5 / repRate : BpmAgent.DEFAULT_CORRELATION_WINDOW;
            _bpmCorrelator.setBinTimespan(timeWindow);
        }
    }

    /** Monitor beam events which indicate that a new beam pulse has been generated.  */
    protected void monitorBeamEvents() {
        if (_beamEventChannel == null && _sequence != null) {
            final TimingCenter timingCenter = _sequence.getAccelerator().getTimingCenter();
            if (timingCenter != null) {
                _beamEventChannel = timingCenter.getChannel(TimingCenter.BEAM_ON_EVENT_HANDLE);
                _bpmCorrelator.addBeamEvent(_beamEventChannel);
            }
        }
    }

    /**
	 * Handle correlation events
	 * @author   t6p
	 */
    protected class BPMCorrelationHandler implements CorrelationNotice {

        /**
		 * Handle the correlation event.  This method gets called when a correlation was posted.
		 * @param sender The poster of the correlation event.
		 * @param correlation The correlation that was posted.
		 */
        public void newCorrelation(final Object sender, final Correlation correlation) {
            synchronized (_activeOrbit) {
                final MutableOrbit orbit = new MutableOrbit(_sequence);
                for (final BpmAgent bpmAgent : _bpmAgents) {
                    final BpmRecord record = (BpmRecord) correlation.getRecord(bpmAgent.getID());
                    if (record != null) {
                        orbit.addRecord(record);
                    }
                }
                _latestOrbit = orbit.getOrbit();
                _proxy.orbitChanged(LiveOrbitSource.this, orbit);
            }
        }

        /**
		 * Handle the no correlation event.  This method gets called when no correlation was found within some prescribed time period.
		 * @param sender The poster of the "no correlation" event.
		 */
        public void noCorrelationCaught(final Object sender) {
        }
    }
}
