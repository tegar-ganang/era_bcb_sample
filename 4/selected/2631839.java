package net.sourceforge.entrainer.eeg.core;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * This class encapsulates the current channel strength value and time from
 * start for the given {@link EEGChannelState}. The channel strength must be in
 * the range of 0 to 1.
 * 
 * @author burton
 */
public class EEGChannelValue implements Serializable {

    private static final long serialVersionUID = 6226957468270733110L;

    private EEGChannelState channelState;

    private double channelStrength;

    private double channelStrengthWithCalibration;

    private double normalizedFactor;

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

    public boolean equals(Object o) {
        if (o == null || !(o instanceof EEGChannelValue)) {
            return false;
        }
        EEGChannelValue value = (EEGChannelValue) o;
        return value.getChannelState().equals(getChannelState());
    }

    /**
	 * The channel strength must be in the range of 0 to 1. This is the
	 * uncalibrated value.
	 */
    public double getChannelStrength() {
        return channelStrength;
    }

    /**
	 * This is the calibrated value. For most applications requiring channel strength,
	 * this is the value that will be used.
	 * 
	 * @see EEGSignalProcessor
	 */
    public double getChannelStrengthWithCalibration() {
        double cs = channelStrengthWithCalibration == 0 ? channelStrength : channelStrengthWithCalibration;
        if (getNormalizedFactor() > 0) {
            return new BigDecimal(cs).divide(new BigDecimal(getNormalizedFactor()), new MathContext(5, RoundingMode.HALF_UP)).doubleValue();
        }
        return cs;
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

    public void setChannelStrengthWithCalibration(double channelStrengthWithCalibration) {
        this.channelStrengthWithCalibration = channelStrengthWithCalibration;
    }

    public double getNormalizedFactor() {
        return normalizedFactor;
    }

    public void setNormalizedFactor(double normalizedFactor) {
        this.normalizedFactor = normalizedFactor;
    }
}
