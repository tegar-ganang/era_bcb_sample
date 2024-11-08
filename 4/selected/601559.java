package net.sourceforge.entrainer.eeg;

import java.io.Serializable;

/**
 * This class encapsulates the current channel strength value and time from start
 * for the given {@link EEGChannelState}. The channel strength must be in the range of
 * 0 to 1.
 * 
 * @author burton
 *
 */
public class EEGChannelValue implements Serializable {

    private static final long serialVersionUID = 6226957468270733110L;

    private EEGChannelState channelState;

    private double channelStrength;

    private long millisFromStart;

    /**
	 * The channel strength must be in the range of 0 to 1. 
	 */
    public EEGChannelValue(EEGChannelState channelState, double channelStrength, long millisFromStart) {
        super();
        setChannelState(channelState);
        setChannelStrength(channelStrength);
        setMillisFromStart(millisFromStart);
    }

    /**
	 * The channel strength must be in the range of 0 to 1. 
	 */
    public double getChannelStrength() {
        return channelStrength;
    }

    public void setChannelStrength(double channelStrength) {
        this.channelStrength = channelStrength;
    }

    public long getMillisFromStart() {
        return millisFromStart;
    }

    public void setMillisFromStart(long msFromStart) {
        this.millisFromStart = msFromStart;
    }

    public EEGChannelState getChannelState() {
        return channelState;
    }

    public void setChannelState(EEGChannelState channelState) {
        this.channelState = channelState;
    }

    public boolean isForFrequencyType(FrequencyType frequencyType) {
        return getChannelState().getFrequencyType().equals(frequencyType);
    }
}
