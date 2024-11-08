package net.sourceforge.entrainer.eeg.core;

import static net.sourceforge.entrainer.eeg.utils.EEGUtils.rootMeanSquare;
import static net.sourceforge.entrainer.util.Utils.snooze;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.log4j.Logger;
import net.sourceforge.entrainer.eeg.core.signalprocessing.StandardDeviationSignalProcessor;

/**
 * Abstract implementation implementing common {@link EEGDevice} methods. All
 * new {@link EEGDevice}s are recommended to subclass this class. Each subclass
 * must have only one, blank constructor, calling
 * <code>super("device description");</code>
 * 
 * @see EEGDevice
 */
@SuppressWarnings("serial")
public abstract class AbstractEEGDevice implements EEGDevice {

    private static final Logger log = Logger.getLogger(AbstractEEGDevice.class);

    private List<EEGReadListener> readListeners = new ArrayList<EEGReadListener>();

    private List<EEGDeviceStatusListener> statusListeners = new ArrayList<EEGDeviceStatusListener>();

    private List<EEGChannelState> channelStates = new ArrayList<EEGChannelState>();

    private List<EEGChannelValue> currentChannelValues = new ArrayList<EEGChannelValue>();

    private long millisFromStart;

    private String statusOfDevice;

    private boolean open;

    private String deviceDescription;

    private boolean calibrating;

    private double sampleFrequencyInHertz;

    private long milliSecondsSleep;

    private int nanoSecondsSleep;

    private EEGSignalProcessor signalProcessor;

    private Calendar cal = Calendar.getInstance();

    private long startMillis;

    /**
	 * Subclasses must call this constructor with a description of the device
	 */
    protected AbstractEEGDevice(String deviceDescription) {
        super();
        if (log.isDebugEnabled()) {
            log.debug("Initializing " + deviceDescription + " with standard deviation signal processor");
        }
        setSignalProcessor(new StandardDeviationSignalProcessor());
        setDeviceDescription(deviceDescription);
        setSampleFrequencyInHertz(DEFAULT_SAMPLE_RATE);
    }

    public void calibrate() throws EEGException {
        setCalibrating(true);
        setStatusOfDevice("Device is calibrating");
        if (log.isDebugEnabled()) {
            log.debug("Calibrating " + getDeviceDescription());
        }
        final List<EEGChannelValue> values = new ArrayList<EEGChannelValue>();
        final EEGReadListener calibrationListener = new EEGReadListener() {

            public void readEventPerformed(EEGReadEvent e) {
                values.addAll(e.getChannels());
            }
        };
        addEEGReadListener(calibrationListener);
        Thread thread = new Thread("Calibration Thread") {

            public void run() {
                snooze(10000);
                removeEEGReadListener(calibrationListener);
                applyCalibration(values);
                setCalibrating(false);
                setStatusOfDevice("Device is calibrated");
                if (log.isDebugEnabled()) {
                    log.debug("Calibrated " + getDeviceDescription());
                }
            }
        };
        thread.start();
    }

    public void clearCalibration() throws EEGException {
        synchronized (this) {
            List<EEGChannelState> states = getChannelStates();
            for (EEGChannelState state : states) {
                state.clearCalibration();
            }
        }
        setStatusOfDevice("Calibration cleared");
        if (log.isDebugEnabled()) {
            log.debug("Calibration cleared for " + getDeviceDescription());
        }
    }

    public void notifyDeviceInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append(getDeviceDescription());
        builder.append(" using ");
        builder.append(getSignalProcessor().getDescription());
        setStatusOfDevice(builder.toString());
    }

    private void applyCalibration(List<EEGChannelValue> values) {
        List<EEGChannelState> states = getChannelStates();
        for (EEGChannelState state : states) {
            setCalibrationForState(state, values);
        }
    }

    private void setCalibrationForState(EEGChannelState state, List<EEGChannelValue> values) {
        List<EEGChannelValue> stateValues = new ArrayList<EEGChannelValue>();
        for (EEGChannelValue value : values) {
            if (value == null) {
                continue;
            }
            if (value.isForFrequencyType(state.getFrequencyType())) {
                stateValues.add(value);
                if (log.isDebugEnabled()) {
                    log.debug("Setting calibration " + state.getFrequencyType().name() + ", to " + value.getNormalizedFactor());
                }
            }
        }
        state.calculateCalibration(stateValues);
    }

    public void addEEGReadListener(EEGReadListener readListener) {
        if (readListener != null) {
            if (log.isDebugEnabled()) {
                log.debug("Adding EEGReadListener " + readListener);
            }
            readListeners.add(readListener);
        }
    }

    public void removeEEGReadListener(EEGReadListener readListener) {
        if (readListener != null) {
            if (log.isDebugEnabled()) {
                log.debug("Removing EEGReadListener " + readListener);
            }
            readListeners.remove(readListener);
        }
    }

    /**
	 * Subclasses should call this method after each reading & interpreting of
	 * the device's data stream.
	 */
    protected void notifyEEGReadListeners() {
        if (readListeners.isEmpty()) {
            return;
        }
        EEGReadEvent e = new EEGReadEvent(this, getCurrentChannelValues());
        if (log.isDebugEnabled()) {
            for (EEGChannelValue value : e.getChannels()) {
                log.debug("Setting current calibrated value " + value.getChannelStrengthWithCalibration() + " for channel " + value.getChannelState().getFrequencyType().name());
            }
        }
        for (EEGReadListener readListener : readListeners) {
            readListener.readEventPerformed(e);
        }
    }

    public void addDeviceStatusListener(EEGDeviceStatusListener deviceStatusListener) {
        if (deviceStatusListener != null) {
            if (log.isDebugEnabled()) {
                log.debug("Adding EEGDeviceStatusListener " + deviceStatusListener);
            }
            statusListeners.add(deviceStatusListener);
        }
    }

    public void removeDeviceStatusListener(EEGDeviceStatusListener deviceStatusListener) {
        if (deviceStatusListener != null) {
            if (log.isDebugEnabled()) {
                log.debug("Removing EEGDeviceStatusListener " + deviceStatusListener);
            }
            statusListeners.remove(deviceStatusListener);
        }
    }

    private void notifyDeviceStatusListeners(String oldStatus, String newStatus) {
        if (statusListeners.isEmpty()) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("notifying status change from " + oldStatus + " to " + newStatus);
        }
        EEGDeviceStatusEvent e = new EEGDeviceStatusEvent(this, oldStatus, newStatus);
        for (EEGDeviceStatusListener statusListener : statusListeners) {
            statusListener.statusChanged(e);
        }
    }

    public EEGChannelState getChannelState(FrequencyType frequencyType) {
        if (log.isDebugEnabled()) {
            log.debug("getChannelState looking for " + frequencyType.name());
        }
        for (EEGChannelState state : channelStates) {
            if (state.getFrequencyType().equals(frequencyType)) {
                if (log.isDebugEnabled()) {
                    log.debug("getChannelState returning" + frequencyType.name());
                }
                return state;
            }
        }
        return null;
    }

    public void removeChannelState(FrequencyType frequencyType) {
        if (log.isDebugEnabled()) {
            log.debug("removeChannelState looking for " + frequencyType.name());
        }
        EEGChannelState toRemove = null;
        for (EEGChannelState channelState : channelStates) {
            if (channelState.getFrequencyType().equals(frequencyType)) {
                if (log.isDebugEnabled()) {
                    log.debug("removeChannelState found " + frequencyType.name());
                }
                toRemove = channelState;
                break;
            }
        }
        if (toRemove != null) {
            channelStates.remove(toRemove);
        } else if (log.isDebugEnabled()) {
            log.debug("removeChannelState did not find " + frequencyType.name());
        }
    }

    public void addChannelState(EEGChannelState channelState) {
        if (log.isDebugEnabled()) {
            log.debug("addChannelState adding " + channelState.getFrequencyType().name());
        }
        if (containsChannelState(channelState.getFrequencyType())) {
            removeChannelState(channelState.getFrequencyType());
        }
        channelState.setSignalProcessor(getSignalProcessor().getNewSignalProcessor());
        channelStates.add(channelState);
    }

    public boolean containsChannelState(FrequencyType frequencyType) {
        return getChannelState(frequencyType) != null;
    }

    public List<EEGChannelState> getChannelStates() {
        return new ArrayList<EEGChannelState>(channelStates);
    }

    public long getMillisFromStart() {
        return millisFromStart;
    }

    /**
	 * Subclasses should use this method to set the number of millis from read
	 * start.
	 */
    protected void setMillisFromStart(long millisFromStart) {
        this.millisFromStart = millisFromStart;
    }

    public void resetMillisFromStart() {
        setMillisFromStart(0);
    }

    public String getStatusOfDevice() {
        return statusOfDevice;
    }

    public List<EEGChannelValue> getCurrentChannelValues() {
        synchronized (currentChannelValues) {
            return new ArrayList<EEGChannelValue>(currentChannelValues);
        }
    }

    /**
	 * Subclasses use this method to apply the raw data, sampled @ the sampling rate, to the
	 * channel values.
	 * 
	 * @param rawData
	 * @see EEGChannelValue
	 * @see EEGSignalProcessor
	 */
    protected void applySignalToChannels(double[] rawData) {
        if (log.isDebugEnabled()) {
            log.debug("applySignalToChannels: Setting data:");
            for (double d : rawData) {
                log.debug("  " + d);
            }
        }
        double[] data = new double[rawData.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.abs(rawData[i]);
        }
        double[] processed;
        long millis = incrementMillisFromStart();
        if (log.isDebugEnabled()) {
            log.debug("for time from start " + millis);
        }
        for (EEGChannelState state : getChannelStates()) {
            processed = state.applyFilters(data);
            double rms = rootMeanSquare(processed);
            setChannelValue(getChannelValue(state.getFrequencyType()), state, rms, millis);
        }
    }

    private long incrementMillisFromStart() {
        long newMillis = cal.getTimeInMillis();
        if (startMillis == 0) {
            startMillis = newMillis;
        }
        setMillisFromStart(newMillis - startMillis);
        return getMillisFromStart();
    }

    private void setChannelValue(EEGChannelValue value, EEGChannelState state, double strength, long millisFromStart) {
        if (value != null) {
            currentChannelValues.remove(value);
        }
        value = new EEGChannelValue(state, strength, millisFromStart);
        value.setChannelStrengthWithCalibration(state.applyCalibration(strength));
        if (log.isDebugEnabled()) {
            log.debug("Setting channel " + state.getFrequencyType().name() + " using strength " + strength);
        }
        currentChannelValues.add(value);
    }

    private EEGChannelValue getChannelValue(FrequencyType frequencyType) {
        for (EEGChannelValue value : currentChannelValues) {
            if (value.isForFrequencyType(frequencyType)) {
                return value;
            }
        }
        return null;
    }

    /**
	 * Subclasses should use this method to set the current status of the device.
	 * Notifies all {@link EEGDeviceStatusListener}s of the specified information
	 * string.
	 */
    protected void setStatusOfDevice(String statusOfDevice) {
        notifyDeviceStatusListeners(this.statusOfDevice, statusOfDevice);
        this.statusOfDevice = statusOfDevice;
    }

    public boolean isOpen() {
        return open;
    }

    /**
	 * Subclasses should call this method when the device is opened or closed.
	 */
    protected void setOpen(boolean open) {
        this.open = open;
        if (!open) {
            startMillis = 0;
        }
    }

    public String getDeviceDescription() {
        return deviceDescription;
    }

    protected void setDeviceDescription(String deviceDescription) {
        this.deviceDescription = deviceDescription;
    }

    public boolean isCalibrating() {
        return calibrating;
    }

    /**
	 * Subclasses should call this method with the appropriate boolean value
	 * during device calibration.
	 */
    protected void setCalibrating(boolean calibrating) {
        this.calibrating = calibrating;
    }

    public double getSampleFrequencyInHertz() {
        return sampleFrequencyInHertz;
    }

    public void setSampleFrequencyInHertz(double sampleFrequencyInHertz) {
        if (log.isDebugEnabled()) {
            log.debug("Setting sample frequency in hertz " + sampleFrequencyInHertz);
        }
        this.sampleFrequencyInHertz = sampleFrequencyInHertz;
        BigDecimal second = new BigDecimal(1000, new MathContext(10));
        BigDecimal sampFreq = new BigDecimal(sampleFrequencyInHertz, new MathContext(10));
        BigDecimal sleepTime = second.divide(sampFreq, new MathContext(10));
        setMilliSecondsSleep(sleepTime.longValue());
        if (log.isDebugEnabled()) {
            log.debug("Set milli sleep " + getMilliSecondsSleep() + " from " + sampleFrequencyInHertz);
        }
        BigDecimal millis = new BigDecimal(getMilliSecondsSleep(), MathContext.DECIMAL64);
        BigDecimal nanos = sleepTime.subtract(millis).multiply(new BigDecimal(1000, MathContext.DECIMAL64));
        setNanoSecondsSleep(nanos.round(new MathContext(3, RoundingMode.HALF_UP)).intValue());
        if (log.isDebugEnabled()) {
            log.debug("Set nano sleep " + getNanoSecondsSleep() + " from " + sampleFrequencyInHertz);
        }
        for (EEGChannelState state : channelStates) {
            state.setSampleRate(sampleFrequencyInHertz);
        }
        setStatusOfDevice("Set sample frequency to " + sampleFrequencyInHertz + "Hz");
    }

    public void setSignalProcessor(EEGSignalProcessor signalProcessor) {
        if (log.isDebugEnabled()) {
            log.debug("setSignalProcessor using " + signalProcessor.getDescription());
        }
        Class<? extends EEGSignalProcessor> existing = this.signalProcessor == null ? null : this.signalProcessor.getClass();
        Class<? extends EEGSignalProcessor> newSigProc = signalProcessor.getClass();
        if (existing != null && existing.equals(newSigProc)) {
            return;
        }
        for (EEGChannelState state : getChannelStates()) {
            state.setSignalProcessor(signalProcessor.getNewSignalProcessor());
        }
        this.signalProcessor = signalProcessor;
        setStatusOfDevice("Set signal processor to " + signalProcessor.getDescription());
    }

    public EEGSignalProcessor getSignalProcessor() {
        return signalProcessor;
    }

    public long getMilliSecondsSleep() throws RuntimeException {
        if (sampleFrequencyInHertz == 0) {
            throw new RuntimeException("Sample frequency has not been set");
        }
        return milliSecondsSleep;
    }

    private void setMilliSecondsSleep(long milliSecondsSleep) {
        this.milliSecondsSleep = milliSecondsSleep;
    }

    public int getNanoSecondsSleep() {
        if (sampleFrequencyInHertz == 0) {
            throw new RuntimeException("Sample frequency has not been set");
        }
        return nanoSecondsSleep;
    }

    private void setNanoSecondsSleep(int nanoSecondsSleep) {
        this.nanoSecondsSleep = nanoSecondsSleep;
    }
}
