package net.sourceforge.entrainer.eeg;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract implementation implementing common {@link EEGDevice} methods.
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractEEGDevice implements EEGDevice {

    private List<EEGReadListener> readListeners = new ArrayList<EEGReadListener>();

    private List<EEGDeviceStatusListener> statusListeners = new ArrayList<EEGDeviceStatusListener>();

    private List<EEGChannelState> channelStates = new ArrayList<EEGChannelState>();

    private List<EEGChannelValue> currentChannelValues = new ArrayList<EEGChannelValue>();

    private long millisFromStart;

    private String statusOfDevice;

    private boolean open;

    private String deviceDescription;

    private boolean calibrating;

    private long millisBetweenReads;

    /**
	 * Subclasses must call this constructor with a description of the device
	 */
    protected AbstractEEGDevice(String deviceDescription) {
        super();
        setDeviceDescription(deviceDescription);
    }

    public void addEEGReadListener(EEGReadListener readListener) {
        if (readListener != null) {
            readListeners.add(readListener);
        }
    }

    public void removeEEGReadListener(EEGReadListener readListener) {
        if (readListener != null) {
            readListeners.remove(readListener);
        }
    }

    /**
	 * Subclasses should call this method after each reading & interpretting of
	 * the device's data stream.
	 */
    protected void notifyEEGReadListeners() {
        if (readListeners.isEmpty()) {
            return;
        }
        EEGReadEvent e = new EEGReadEvent(this, getCurrentChannelValues());
        for (EEGReadListener readListener : readListeners) {
            readListener.readEventPerformed(e);
        }
    }

    public void addDeviceStatusListener(EEGDeviceStatusListener deviceStatusListener) {
        if (deviceStatusListener != null) {
            statusListeners.add(deviceStatusListener);
        }
    }

    public void removeDeviceStatusListener(EEGDeviceStatusListener deviceStatusListener) {
        if (deviceStatusListener != null) {
            statusListeners.remove(deviceStatusListener);
        }
    }

    private void notifyDeviceStatusListeners(String oldStatus, String newStatus) {
        if (statusListeners.isEmpty()) {
            return;
        }
        EEGDeviceStatusEvent e = new EEGDeviceStatusEvent(this, oldStatus, newStatus);
        for (EEGDeviceStatusListener statusListener : statusListeners) {
            statusListener.statusChanged(e);
        }
    }

    public EEGChannelState getChannelState(FrequencyType frequencyType) {
        for (EEGChannelState state : channelStates) {
            if (state.getFrequencyType().equals(frequencyType)) {
                return state;
            }
        }
        return null;
    }

    public void removeChannelState(FrequencyType frequencyType) {
        EEGChannelState toRemove = null;
        for (EEGChannelState channelState : channelStates) {
            if (channelState.getFrequencyType().equals(frequencyType)) {
                toRemove = channelState;
                break;
            }
        }
        if (toRemove != null) {
            channelStates.remove(toRemove);
        }
    }

    public void addChannelState(EEGChannelState channelState) {
        if (containsChannelState(channelState.getFrequencyType())) {
            removeChannelState(channelState.getFrequencyType());
        }
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
	 * Subclasses should use this method to set the number
	 * of millis from read start.
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
        return new ArrayList<EEGChannelValue>(currentChannelValues);
    }

    /**
	 * Subclasses should use this method to set the current {@link EEGChannelValue}
	 * for the given {@link FrequencyType}.
	 */
    protected void setChannel(FrequencyType frequencyType, double strength) {
        EEGChannelState state = getChannelState(frequencyType);
        if (state != null) {
            setChannelValue(getChannelValue(frequencyType), state, strength);
        }
    }

    private void setChannelValue(EEGChannelValue value, EEGChannelState state, double strength) {
        if (value != null) {
            currentChannelValues.remove(value);
        }
        currentChannelValues.add(new EEGChannelValue(state, strength, getMillisFromStart()));
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
	 * Subclasses should use this method to set the current status
	 * of the device
	 */
    protected void setStatusOfDevice(String statusOfDevice) {
        notifyDeviceStatusListeners(this.statusOfDevice, statusOfDevice);
        this.statusOfDevice = statusOfDevice;
    }

    public boolean isOpen() {
        return open;
    }

    /**
	 * Subclasses should call this method when the device is opened
	 * or closed.
	 */
    protected void setOpen(boolean open) {
        this.open = open;
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
	 * Subclasses should call this method with the appropriate boolean
	 * value during device calibration.
	 */
    protected void setCalibrating(boolean calibrating) {
        this.calibrating = calibrating;
    }

    protected long getMillisBetweenReads() {
        return millisBetweenReads;
    }

    /**
	 * Subclasses should call this method from their 'openDevice' implementation 
	 * @param millisBetweenReads
	 */
    protected void setMillisBetweenReads(long millisBetweenReads) {
        this.millisBetweenReads = millisBetweenReads;
    }
}
