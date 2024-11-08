package gov.sns.apps.orbitcorrect;

import gov.sns.ca.*;
import gov.sns.xal.smf.*;
import gov.sns.tools.messaging.MessageCenter;

/**
 * RepRateMonitor monitors the accelerator's rep-rate.
 *
 * @author    tap
 * @since     Aug 05, 2004
 */
public class RepRateMonitor implements ChannelEventListener {

    /** accelerator */
    protected Accelerator _accelerator;

    /** wrapper for connecting to and monitoring the rep-rate channel */
    protected ChannelWrapper _repRateWrapper;

    /** center for dispatching events to registered listeners */
    protected MessageCenter _messageCenter;

    /** proxy for messages to be forwarded to registered listeners */
    protected RepRateListener _eventProxy;

    /** latest rep-rate */
    protected volatile double _repRate;

    /**
	 * Primary constructor
	 *
	 * @param accelerator  The accelerator whose rep-rate we wish to monitor.
	 */
    public RepRateMonitor(final Accelerator accelerator) {
        _messageCenter = new MessageCenter("Rep Rate Monitor");
        _eventProxy = _messageCenter.registerSource(this, RepRateListener.class);
        setAccelerator(accelerator);
    }

    /** Constructor */
    public RepRateMonitor() {
        this(null);
    }

    /**
	 * Get the current rep rate.
	 * @return the rep rate in Hz
	 */
    public double getRepRate() {
        return _repRate;
    }

    /**
	 * Set this monitor's accelerator.
	 *
	 * @param accelerator  The new accelerator value
	 */
    public void setAccelerator(final Accelerator accelerator) {
        if (accelerator == _accelerator) {
            return;
        }
        _accelerator = accelerator;
        disposeRepRateWrapper();
        _repRate = Double.NaN;
        if (accelerator == null) {
            return;
        }
        try {
            Channel channel = accelerator.getTimingCenter().getChannel(TimingCenter.REP_RATE_HANDLE);
            _repRateWrapper = new ChannelWrapper(channel);
            _repRateWrapper.addChannelEventListener(this);
            _repRateWrapper.requestConnection();
        } catch (NoSuchChannelException exception) {
            System.out.println("No rep-rate channel available...");
        }
    }

    /** Dispose of the rep rate channel wrapper. */
    public void disposeRepRateWrapper() {
        if (_repRateWrapper != null) {
            _repRateWrapper.removeChannelEventListener(this);
            _repRateWrapper.dispose();
            _repRateWrapper = null;
        }
    }

    /** Dispose of this monitor. */
    public void dispose() {
        disposeRepRateWrapper();
    }

    /**
	 * Adds the listener as a receiver of rep-rate events from this monitor.
	 *
	 * @param listener  The listener to add as a receiver of rep-rate events.
	 */
    public void addRepRateListener(final RepRateListener listener) {
        _messageCenter.registerTarget(listener, this, RepRateListener.class);
        listener.repRateChanged(this, _repRate);
    }

    /**
	 * Removes the listener from receiving rep-rate events from this monitor.
	 *
	 * @param listener  The listener to remove from receiving rep-rate events.
	 */
    public void removeRepRateListener(final RepRateListener listener) {
        _messageCenter.removeTarget(listener, this, RepRateListener.class);
    }

    /**
	 * Received a rep-rate change event.
	 *
	 * @param channel  the channel whose value has changed
	 * @param record   The channel time record of the new value
	 */
    public void valueChanged(Channel channel, ChannelTimeRecord record) {
        final double repRate = record.doubleValue();
        if (repRate != _repRate) {
            _repRate = repRate;
            _eventProxy.repRateChanged(this, repRate);
        }
    }

    /**
	 * The channel's connection has changed. Either it has established a new
	 * connection or the existing connection has dropped.
	 *
	 * @param channel    The channel whose connection has changed.
	 * @param connected  The channel's new connection state
	 */
    public void connectionChanged(Channel channel, boolean connected) {
        double repRate;
        try {
            repRate = connected ? channel.getValDbl() : Double.NaN;
        } catch (Exception exception) {
            repRate = Double.NaN;
        }
        _repRate = repRate;
        _eventProxy.repRateChanged(this, repRate);
    }
}
