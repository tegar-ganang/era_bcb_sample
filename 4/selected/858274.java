package gov.sns.apps.scope;

import gov.sns.tools.ArrayMath;
import gov.sns.tools.data.*;
import gov.sns.tools.messaging.MessageCenter;
import gov.sns.ca.*;
import gov.sns.tools.correlator.*;
import gov.sns.tools.Lock;
import gov.sns.application.Util;

/**
 * Model for a single waveform channel.
 *
 * @author  tap
 */
public class ChannelModel implements TraceSource, DataListener, TimeModelListener, ConnectionListener {

    static final String DATA_LABEL = "ChannelModel";

    private static final String SAMPLE_PERIOD_PV_SUFFIX;

    private static final String DELAY_PV_SUFFIX;

    protected final String ID;

    protected final MessageCenter MESSAGE_CENTER;

    protected final ChannelModelListener CHANNEL_MODEL_PROXY;

    protected final SettingListener SETTING_PROXY;

    protected TimeModel _timeModel;

    protected final Lock BUSY_LOCK;

    protected boolean _enabled;

    protected double _signalScale;

    protected double _signalOffset;

    protected volatile boolean _isSettingChannel;

    protected volatile boolean _isReady;

    protected volatile boolean _waveformDelayInitialized;

    protected Channel _channel;

    protected Channel _delayChannel;

    protected Monitor _waveformDelayMonitor;

    protected Channel _samplePeriodChannel;

    protected Monitor _samplePeriodMonitor;

    protected double _waveformDelay;

    protected double _samplePeriod;

    protected double[] _elementTimes;

    static {
        final String SAMPLE_PERIOD_PV_SUFFIX_KEY = "waveformSamplePeriodPvSuffix";
        final String DELAY_PV_SUFFIX_KEY = "waveformDelayPvSuffix";
        java.util.Map properties = Util.getPropertiesFromResource("scope");
        SAMPLE_PERIOD_PV_SUFFIX = System.getProperties().getProperty(SAMPLE_PERIOD_PV_SUFFIX_KEY, (String) properties.get(SAMPLE_PERIOD_PV_SUFFIX_KEY));
        DELAY_PV_SUFFIX = System.getProperties().getProperty(DELAY_PV_SUFFIX_KEY, (String) properties.get(DELAY_PV_SUFFIX_KEY));
    }

    /** Creates a new instance of ChannelModel */
    public ChannelModel(final String anId, final TimeModel aTimeModel) {
        this(anId, null, aTimeModel);
    }

    /** Create a new channel model with the specified channel name */
    public ChannelModel(final String anID, final String channelName, final TimeModel aTimeModel) {
        _isReady = false;
        ID = anID;
        MESSAGE_CENTER = new MessageCenter("Channel Model");
        CHANNEL_MODEL_PROXY = MESSAGE_CENTER.registerSource(this, ChannelModelListener.class);
        SETTING_PROXY = MESSAGE_CENTER.registerSource(this, SettingListener.class);
        _isSettingChannel = false;
        BUSY_LOCK = new Lock();
        _timeModel = aTimeModel;
        _timeModel.addTimeModelListener(this);
        setChannel(channelName);
        setEnabled(false);
        setSignalScale(1.0);
        setSignalOffset(0);
        _waveformDelayInitialized = false;
        _waveformDelay = 0;
        _samplePeriod = 0;
        _elementTimes = new double[0];
    }

    /**
     * Get the channel model's ID.
     * @return The channel model's ID.
     */
    public String getID() {
        return ID;
    }

    /**
	 * Get the label for this channel model
	 * @return The name of the channel.
	 */
    public String getLabel() {
        return (_channel != null) ? _channel.channelName() : "";
    }

    /** 
     * Get the name used to identify the class in an external data source.
     * @return The tag for this data node.
     */
    public String dataLabel() {
        return DATA_LABEL;
    }

    /**
     * Instructs the receiver to update its data based on the given adaptor.
     * @param adaptor The data adaptor corresponding to this object's data node.
     */
    public void update(final DataAdaptor adaptor) throws ChannelSetException {
        if (adaptor.hasAttribute("channel")) {
            setChannel(adaptor.stringValue("channel"));
        }
        if (adaptor.hasAttribute("delayChannel")) {
            setDelayChannel(adaptor.stringValue("delayChannel"));
        }
        if (adaptor.hasAttribute("samplePeriodChannel")) {
            setSamplePeriodChannel(adaptor.stringValue("samplePeriodChannel"));
        }
        setSignalScale(adaptor.doubleValue("signalScale"));
        setSignalOffset(adaptor.doubleValue("signalOffset"));
        setEnabled(adaptor.booleanValue("enabled"));
    }

    /**
     * Instructs the receiver to write its data to the adaptor for external
     * storage.
     * @param adaptor The data adaptor corresponding to this object's data node.
     */
    public void write(DataAdaptor adaptor) {
        if (_channel != null) {
            adaptor.setValue("channel", _channel.channelName());
        }
        if (_delayChannel != null) {
            adaptor.setValue("delayChannel", _delayChannel.channelName());
        }
        if (_samplePeriodChannel != null) {
            adaptor.setValue("samplePeriodChannel", _samplePeriodChannel.channelName());
        }
        adaptor.setValue("enabled", _enabled);
        adaptor.setValue("signalScale", _signalScale);
        adaptor.setValue("signalOffset", _signalOffset);
    }

    /**
     * Dispose of the resources held by this model.  In particular, we shutdown
     * the monitors.
     */
    void dispose() {
        try {
            stopChannelEvents();
        } catch (Exception exception) {
            System.err.println("Failed to dispose of channel model: " + ID);
            System.err.println(exception);
            exception.printStackTrace();
        } finally {
        }
    }

    /**
     * Stop listentening for waveform connection events and stop monitoring 
     * the delay and period channels.
     */
    protected void stopChannelEvents() {
        if (_channel != null) {
            _channel.removeConnectionListener(this);
        }
        stopMonitoringTime();
    }

    /** 
     * Add a ChannelModelListener.
     * @param listener The object to add as a listener of channel model events.
     */
    public void addChannelModelListener(final ChannelModelListener listener) {
        MESSAGE_CENTER.registerTarget(listener, this, ChannelModelListener.class);
    }

    /** 
     * Remove a ChannelModelListener.
     * @param listener The object to remove as a listener of channel model events.
     */
    public void removeChannelModelListener(final ChannelModelListener listener) {
        MESSAGE_CENTER.removeTarget(listener, this, ChannelModelListener.class);
    }

    /**
     * Add the listener to be notified when a setting has changed.
     * @param listener Object to receive setting change events.
     */
    void addSettingListener(final SettingListener listener) {
        MESSAGE_CENTER.registerTarget(listener, this, SettingListener.class);
    }

    /**
     * Remove the listener as a receiver of setting change events.
     * @param listener Object to remove from receiving setting change events.
     */
    void removeSettingListener(final SettingListener listener) {
        MESSAGE_CENTER.removeTarget(listener, this, SettingListener.class);
    }

    /**
     * Determine if the channel is being set.
     * @return true if the channel is being set and false otherwise.
     */
    public boolean isSettingChannel() {
        return _isSettingChannel;
    }

    /**
     * Try to get a lock on the model.  If the sender gets the lock it 
     * is responsible for releasing the lock when done.
     * @return true if the sender gets the lock and false otherwise.
     */
    public boolean tryLock() {
        return BUSY_LOCK.tryLock();
    }

    /**
     * Release a lock on the channel model.  Every lock must be balanced by
     * an unlock in order to free the model for a new lock from a separate thread.
     */
    public void unlock() {
        BUSY_LOCK.unlock();
    }

    /** 
     * Change the waveform channel to that specified by the channel name.  Also monitor the waveform's offset from cycle start and the width of each element.
     * @param channelName The name of the channel to set.
     * @throws gov.sns.apps.scope.ChannelSetException if one or more of the required associated channels cannot connect.
     */
    public void setChannel(final String channelName) throws ChannelSetException {
        try {
            _isReady = false;
            _isSettingChannel = true;
            BUSY_LOCK.lock();
            setEnabled(false);
            stopChannelEvents();
            _waveformDelayInitialized = false;
            _samplePeriod = 0;
            _waveformDelay = 0;
            if (channelName == null || channelName == "" || channelName.length() == 0) {
                _channel = null;
                return;
            }
            _channel = ChannelFactory.defaultFactory().getChannel(channelName);
            CHANNEL_MODEL_PROXY.disableChannel(this, _channel);
            _channel.addConnectionListener(this);
            _channel.requestConnection();
            setupTimeChannels();
            Channel.flushIO();
            setEnabled(true);
        } finally {
            BUSY_LOCK.unlock();
            _isSettingChannel = false;
            CHANNEL_MODEL_PROXY.channelChanged(this, _channel);
            SETTING_PROXY.settingChanged(this);
        }
    }

    /** 
     * Get the waveform channel.
     * @return The waveform channel.
     */
    public Channel getChannel() {
        return _channel;
    }

    /** 
     * Get the waveform channel name.
     * @return The waveform channel name.
     */
    public String getChannelName() {
        if (_channel != null) {
            return _channel.channelName();
        } else {
            return null;
        }
    }

    /**
	 * Set the delay channel to the specified PV.
	 * @param pv the PV of the delay channel
	 */
    public void setDelayChannel(final String pv) {
        final Channel oldDelayChannel = _delayChannel;
        if (oldDelayChannel != null && pv.equals(oldDelayChannel.channelName())) return;
        try {
            _isReady = false;
            BUSY_LOCK.lock();
            stopMonitoringDelay();
            if (oldDelayChannel != null) {
                oldDelayChannel.removeConnectionListener(this);
            }
            if (pv != null && pv.length() > 0 && !pv.equals("")) {
                _delayChannel = ChannelFactory.defaultFactory().getChannel(pv);
                _delayChannel.addConnectionListener(this);
                _delayChannel.requestConnection();
                Channel.flushIO();
            } else {
                _delayChannel = null;
            }
        } finally {
            BUSY_LOCK.unlock();
            SETTING_PROXY.settingChanged(this);
        }
    }

    /**
	 * Get the delay channel
	 * @return the delay channel
	 */
    public Channel getDelayChannel() {
        return _delayChannel;
    }

    /**
	 * Set the sample period channel to that specified by the PV.
	 * @param pv the PV for which to set the sample period channel
	 */
    public void setSamplePeriodChannel(final String pv) {
        final Channel oldSamplePeriodChannel = _samplePeriodChannel;
        if (oldSamplePeriodChannel != null && pv.equals(oldSamplePeriodChannel.channelName())) return;
        try {
            stopMonitoringSamplePeriod();
            if (oldSamplePeriodChannel != null) {
                oldSamplePeriodChannel.removeConnectionListener(this);
            }
            if (pv != null && pv.length() > 0 && !pv.equals("")) {
                _samplePeriodChannel = ChannelFactory.defaultFactory().getChannel(pv);
                _samplePeriodChannel.addConnectionListener(this);
                _samplePeriodChannel.requestConnection();
                Channel.flushIO();
            } else {
                _samplePeriodChannel = null;
            }
        } finally {
            BUSY_LOCK.unlock();
            SETTING_PROXY.settingChanged(this);
        }
    }

    /**
	 * Get the period channel
	 * @return the sample period channel
	 */
    public Channel getSamplePeriodChannel() {
        return _samplePeriodChannel;
    }

    /**
     * Get the number of elements in the waveform.
     * @return The number of elements in the waveform
     */
    public int getNumElements() {
        try {
            return _channel.elementCount();
        } catch (ConnectionException exception) {
            System.err.println(exception);
            return 0;
        }
    }

    /**
     * Create and connect to the channels that provide the offset form cycle start and the period between the waveform elements.
     * @throws gov.sns.apps.scope.ChannelSetException if one or more of the required associated channels cannot connect.
     */
    protected void setupTimeChannels() throws ChannelSetException {
        stopMonitoringTime();
        if (_delayChannel != null) {
            _delayChannel.removeConnectionListener(this);
        }
        if (_samplePeriodChannel != null) {
            _samplePeriodChannel.removeConnectionListener(this);
        }
        String channelName = _channel.channelName();
        int handleIndex = channelName.lastIndexOf(":");
        if (handleIndex < 1) return;
        String baseName = channelName.substring(0, handleIndex);
        setDelayChannel(baseName + ":" + DELAY_PV_SUFFIX);
        setSamplePeriodChannel(baseName + ":" + SAMPLE_PERIOD_PV_SUFFIX);
    }

    /**
	 * Handle the waveform connection event.
	 * @param waveformChannel the waveform channel
	 */
    protected void handleWaveformConnection(final Channel waveformChannel) {
        try {
            BUSY_LOCK.lock();
            _isReady = false;
            int numElements = _channel.elementCount();
            _elementTimes = new double[numElements];
            updateElementTimes();
            _isReady = true;
        } catch (ConnectionException exception) {
            System.err.println(exception);
        } finally {
            BUSY_LOCK.unlock();
        }
    }

    /** Monitor the delay channel. */
    protected void monitorDelayChannel() {
        try {
            BUSY_LOCK.lock();
            _isReady = false;
            if (_waveformDelayMonitor == null) {
                _waveformDelayMonitor = _delayChannel.addMonitorValue(new WaveformDelayListener(), Monitor.VALUE);
            }
            updateElementTimes();
            _isReady = true;
        } catch (ConnectionException exception) {
            System.err.println(exception);
        } catch (MonitorException exception) {
            System.err.println(exception);
        } finally {
            BUSY_LOCK.unlock();
        }
    }

    /** Monitor the sample period channel. */
    protected void monitorSamplePeriod() {
        try {
            BUSY_LOCK.lock();
            _isReady = false;
            if (_samplePeriodMonitor == null) {
                _samplePeriodMonitor = _samplePeriodChannel.addMonitorValue(new SamplePeriodListener(), Monitor.VALUE);
            }
            updateElementTimes();
            _isReady = true;
        } catch (ConnectionException exception) {
            System.err.println(exception);
        } catch (MonitorException exception) {
            System.err.println(exception);
        } finally {
            BUSY_LOCK.unlock();
        }
    }

    /**
     * Create the array of time elements (one time element for each element of the waveform).
     * The time element array is generated from the length of the waveform, the delay time
     * and the sample period.
     */
    protected void updateElementTimes() {
        if (_elementTimes == null || !channelsConnected()) return;
        int numElements = _elementTimes.length;
        double timeMark = _waveformDelay;
        try {
            BUSY_LOCK.lock();
            for (int index = 0; index < numElements; index++) {
                _elementTimes[index] = timeMark;
                timeMark += _samplePeriod;
            }
            _timeModel.convertTurns(_elementTimes);
            CHANNEL_MODEL_PROXY.elementTimesChanged(this, _elementTimes);
        } finally {
            BUSY_LOCK.unlock();
        }
    }

    /** Stop monitoring the delay channel. */
    private void stopMonitoringDelay() {
        try {
            BUSY_LOCK.lock();
            _isReady = false;
            if (_waveformDelayMonitor != null) {
                _waveformDelayMonitor.clear();
                _waveformDelayMonitor = null;
            }
        } finally {
            BUSY_LOCK.unlock();
        }
    }

    /** Stop monitoring the sample period channel. */
    private void stopMonitoringSamplePeriod() {
        try {
            BUSY_LOCK.lock();
            _isReady = false;
            if (_samplePeriodMonitor != null) {
                _samplePeriodMonitor.clear();
                _samplePeriodMonitor = null;
            }
        } finally {
            BUSY_LOCK.unlock();
        }
    }

    /** Stop monitoring the time PVs which determine the offset from cycle start and the period between waveform elements. */
    private void stopMonitoringTime() {
        try {
            BUSY_LOCK.lock();
            _isReady = false;
            stopMonitoringDelay();
            stopMonitoringSamplePeriod();
        } finally {
            BUSY_LOCK.unlock();
        }
    }

    /**
     * Get the waveform delay in units of turns.
     * @return the waveform delay in units of turns.
     */
    public final double getWaveformDelay() {
        return _waveformDelay;
    }

    /**
     * Get the sample period in units of turns.
     * @return the sample period in units of turns.
     */
    public final double getSamplePeriod() {
        return _samplePeriod;
    }

    /**
     * Get the array of time elements for the waveform.  Each time element represents
     * the time associated with the corresponding waveform element.  The time unit is 
     * a turn.
     * @return The element times in units of turns relative to cycle start.
     */
    public final double[] getElementTimes() {
        return _elementTimes;
    }

    /**
     * Event indicating that the time units of the time model sender has changed.
     * @param sender The sender of the event.
     */
    public void timeUnitsChanged(final TimeModel sender) {
    }

    /**
     * Event indicating that the time conversion of the time model sender has changed.
     * This is most likely due to the scaling changing.  For example the turn to 
     * microsecond conversion is monitored and may change during the lifetime of 
     * the application.
     * @param sender The sender of the event.
     */
    public void timeConversionChanged(final TimeModel sender) {
        updateElementTimes();
    }

    /** 
     * Get the scale to be applied to the signal trace.
     * @return the scale applied to the singal trace.
     */
    public final double getSignalScale() {
        return _signalScale;
    }

    /** 
     * Set the scale to be applied to the signal trace.
     * @param newScale The new scale to apply to the singal trace.
     */
    public final void setSignalScale(final double newScale) {
        if (_signalScale != newScale) {
            _signalScale = newScale;
            SETTING_PROXY.settingChanged(this);
        }
    }

    /** 
     * Get the offset to be applied to the signal trace.
     * @return The offset applied to the singal trace.
     */
    public final double getSignalOffset() {
        return _signalOffset;
    }

    /** 
     * Set the scale to be applied to the signal trace.
     * @param newOffset The new offset to apply to the signal trace.
     */
    public final void setSignalOffset(final double newOffset) {
        if (_signalOffset != newOffset) {
            _signalOffset = newOffset;
            SETTING_PROXY.settingChanged(this);
        }
    }

    /**
     * Get the trace for the specified record.  Process the raw record to account
     * for the signal scale and signal offset.
     * @param correlation The correlation from which to get the channel's record and generate the trace.
     * @return the waveform trace
     */
    public final double[] getTrace(final Correlation correlation) {
        final ChannelRecord record = (ChannelRecord) correlation.getRecord(ID);
        return record == null ? null : ArrayMath.transform(record.doubleArray(), _signalScale, _signalOffset);
    }

    /**
     * Get the trace event for this trace source extracted from the correlation.
	 * @param correlation The correlation from which the trace is extracted.
	 * @return the trace event corresponding to this trace source and the correlation
     */
    public final TraceEvent getTraceEvent(final Correlation correlation) {
        final ChannelRecord record = (ChannelRecord) correlation.getRecord(ID);
        return record == null ? null : new ChannelTraceEvent(this, record.doubleArray(), _elementTimes);
    }

    /** 
     * Determine if the waveform is enabled.
     * @return true if the waveform is enabled, false otherwise.
     * @see #setEnabled
     */
    public final boolean isEnabled() {
        return _enabled;
    }

    /** 
     * Set whether the waveform is enabled.  The waveform is only enabled if the 
     * waveform can be enabled given the status of the settings.
     * @param state true to enable the waveform and false to disable it.
     * @see #isEnabled
     */
    public void setEnabled(final boolean state) throws ChannelSetException {
        if (_enabled != state) {
            _enabled = (_channel != null) ? state : false;
            if (_enabled) {
                if (channelsConnected()) {
                    monitorDelayChannel();
                    monitorSamplePeriod();
                }
                CHANNEL_MODEL_PROXY.enableChannel(this, _channel);
            } else {
                if (_channel != null) {
                    stopMonitoringTime();
                    CHANNEL_MODEL_PROXY.disableChannel(this, _channel);
                }
            }
            SETTING_PROXY.settingChanged(this);
        }
    }

    /** Toggle the channel enable */
    public void toggleEnable() {
        setEnabled(!_enabled);
    }

    /**
	 * Determine if the waveform channel and time channels are all set and connected
	 * @return true if the channels are set and connected
	 */
    public boolean channelsConnected() {
        return _channel != null && _channel.isConnected() && _delayChannel != null && _delayChannel.isConnected() && _samplePeriodChannel != null && _samplePeriodChannel.isConnected();
    }

    /**
	 * Determine if the channel can be monitored which indicates that the waveform channel,
	 * delay channel and period channel are all connected and element times are set.
	 * @return true if the waveform can be monitored and false if not
	 */
    public boolean canMonitor() {
        return channelsConnected() && _elementTimes != null && _samplePeriod != 0 && _waveformDelayInitialized && _isReady;
    }

    /**
     * Indicates that a connection to the specified channel has been established.
     * @param channel The channel which has been connected.
     */
    public void connectionMade(final Channel channel) {
        if (channel == _channel) {
            handleWaveformConnection(channel);
        } else if (channel == _delayChannel) {
            monitorDelayChannel();
        } else if (channel == _samplePeriodChannel) {
            monitorSamplePeriod();
        }
        CHANNEL_MODEL_PROXY.channelChanged(this, _channel);
    }

    /**
     * Indicates that a connection to the specified channel has been dropped.
     * @param channel The channel which has been disconnected.
     */
    public void connectionDropped(final Channel channel) {
        CHANNEL_MODEL_PROXY.channelChanged(this, _channel);
    }

    /**
     * Listener of monitor events associated with the delay time for the waveform.
     */
    class WaveformDelayListener implements IEventSinkValue {

        /**
         * Callback which updates the waveform delay and then recalculates the 
         * the element time array.
         */
        public void eventValue(final ChannelRecord record, final Channel chan) {
            double newDelay = record.doubleValue();
            boolean postChange = false;
            try {
                BUSY_LOCK.lock();
                if (newDelay != _waveformDelay) {
                    _waveformDelay = newDelay;
                    updateElementTimes();
                }
                if (!_waveformDelayInitialized) {
                    postChange = true;
                    _waveformDelayInitialized = true;
                }
            } finally {
                BUSY_LOCK.unlock();
                if (postChange) {
                    CHANNEL_MODEL_PROXY.channelChanged(ChannelModel.this, _channel);
                }
            }
        }
    }

    /**
     * Listener of monitor events associated with the sample period for the waveform elements.
     */
    class SamplePeriodListener implements IEventSinkValue {

        /**
         * Callback which updates the waveform sample period and then recalculates the 
         * the element time array.
         */
        public void eventValue(final ChannelRecord record, final Channel chan) {
            double newPeriod = record.doubleValue();
            if (newPeriod != _samplePeriod) {
                try {
                    BUSY_LOCK.lock();
                    _samplePeriod = newPeriod;
                    updateElementTimes();
                } finally {
                    BUSY_LOCK.unlock();
                    CHANNEL_MODEL_PROXY.channelChanged(ChannelModel.this, _channel);
                }
            }
        }
    }
}
