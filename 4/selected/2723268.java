package uk.org.toot.audio.meter;

import java.util.List;
import java.util.ArrayList;
import uk.org.toot.control.*;
import uk.org.toot.audio.core.AudioControls;
import uk.org.toot.audio.core.ChannelFormat;
import static uk.org.toot.misc.Localisation.*;

/**
 * MeterControls provides composite controls for MeterProcess.
 */
public class MeterControls extends AudioControls {

    public static final int METER = 100;

    public static final int METER_TYPE = 2;

    public static final int METER_RESET = 3;

    public static final int METER_OVERS = 4;

    public static final int METER_INDICATOR = 5;

    public static final int METER_MIN_DB = 6;

    private static LinearLaw meterLaw = new LinearLaw(-60f, 20f, "dB");

    private ChannelFormat channelFormat;

    private ChannelState[] channelState;

    private TypeControl typeControl;

    private MindBControl mindBControl;

    private float peakRelease = 0.005f;

    private float averageSmooth = 0.038f;

    private float maxdB = 20, mindB = -80;

    public MeterControls(ChannelFormat format, String name) {
        this(format, name, true);
    }

    public MeterControls(ChannelFormat format, String name, boolean full) {
        super(METER, name);
        channelFormat = format;
        int nchannels = channelFormat.getCount();
        channelState = new ChannelState[nchannels];
        for (int i = 0; i < nchannels; i++) {
            channelState[i] = new ChannelState();
        }
        if (full) {
            add(new ResetControl());
            add(new OverIndicator());
            add(typeControl = new TypeControl());
        }
        add(new MeterIndicator(name));
        if (full) {
            add(mindBControl = new MindBControl());
            derive(typeControl);
            derive(mindBControl);
        }
    }

    @Override
    protected void derive(Control c) {
        switch(c.getId()) {
            case METER_TYPE:
                maxdB = typeControl.getMaxdB();
                break;
            case METER_MIN_DB:
                mindB = mindBControl.getMindB();
                break;
        }
    }

    public boolean canBypass() {
        return false;
    }

    public boolean isAlwaysVertical() {
        return true;
    }

    public float getMaxdB() {
        return maxdB;
    }

    public float getMindB() {
        return mindB;
    }

    public ChannelFormat getChannelFormat() {
        return channelFormat;
    }

    protected boolean invalidChannel(int chan) {
        return chan < 0 || chan >= channelFormat.getCount();
    }

    public ChannelState getState(int chan) {
        if (invalidChannel(chan)) return null;
        return channelState[chan];
    }

    public void resetOvers() {
        for (int c = 0; c < channelFormat.getCount(); c++) {
            channelState[c].overs = 0;
        }
    }

    public void resetMaxima() {
        for (int c = 0; c < channelFormat.getCount(); c++) {
            channelState[c].maxPeak = channelState[c].peak;
            channelState[c].maxAverage = channelState[c].average;
        }
    }

    public void addOvers(int chan, int overs) {
        if (invalidChannel(chan) || overs == 0) return;
        channelState[chan].overs += overs;
    }

    public void setPeak(int chan, float peak) {
        if (invalidChannel(chan)) return;
        ChannelState state = channelState[chan];
        if (peak > state.peak) {
            state.peak = peak;
        } else {
            state.peak += peakRelease * (peak - state.peak);
        }
        if (state.peak > state.maxPeak) {
            state.maxPeak = state.peak;
        }
    }

    public void setAverage(int chan, float average) {
        if (invalidChannel(chan)) return;
        ChannelState state = channelState[chan];
        if (average != average) average = 0;
        state.average += averageSmooth * (average - state.average);
        if (state.average > state.maxAverage) {
            state.maxAverage = state.average;
        }
    }

    private static double PEAK_K_PER_MS = Math.log(0.05) / 3000;

    private static double AV_K_PER_MS = Math.log(0.01) / 600;

    public void setUpdateTime(float ms) {
        peakRelease = (float) (1.0 - Math.exp(ms * PEAK_K_PER_MS));
        averageSmooth = (float) (1.0 - Math.exp(ms * AV_K_PER_MS));
    }

    /**
     * A ChannelState represents the states of a particular meter channel.
     */
    public static class ChannelState {

        public int overs;

        public float maxPeak;

        public float peak;

        public float maxAverage;

        public float average;
    }

    /**
     * A TypeControl concretizes EnumControl with the different K-System meter
     * types, K-20/RMS, K-14/RMS and K-12/RMS.
     */
    public static class TypeControl extends EnumControl {

        private List<String> values = new ArrayList<String>();

        private float[] floatValues;

        public TypeControl() {
            super(METER_TYPE, "Type", "K-20/RMS");
            values.add("K-20/RMS");
            values.add("K-14/RMS");
            values.add("K-12/RMS");
            floatValues = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                floatValues[i] = Float.valueOf(values.get(i).substring(2, 4));
            }
        }

        public List getValues() {
            return values;
        }

        public float getMaxdB() {
            int i = values.indexOf(getValue());
            return floatValues[i];
        }

        public int getWidthLimit() {
            return 127;
        }
    }

    /**
     * A ResetControl is a momentary acting BooleanControl that resets
     * the 'overs' and 'maxima' states.
     */
    public class ResetControl extends BooleanControl {

        public ResetControl() {
            super(METER_RESET, getString("Reset"), false, true);
        }

        public void momentaryAction() {
            resetOvers();
            resetMaxima();
        }

        public int getWidthLimit() {
            return 127;
        }
    }

    /**
     * Somewhat unusually, OverIndicator extends Control directly, to use
     * its Observer pattern support to indicate state changes.
     */
    public static class OverIndicator extends Control {

        public OverIndicator() {
            super(METER_OVERS, "");
        }
    }

    /**
     * A MindBControl concretizes EnumControl with various minimum dB values
     * for the meter (display), from -12dB to -100dB, suitable for both normal
     * operation and calibration.
     */
    public static class MindBControl extends EnumControl {

        private List<String> values = new ArrayList<String>();

        private float[] floatValues;

        public MindBControl() {
            super(METER_MIN_DB, "Min", "-60");
            values.add("-12");
            values.add("-20");
            values.add("-40");
            values.add("-60");
            values.add("-80");
            values.add("-100");
            values.add("-120");
            floatValues = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                floatValues[i] = Float.valueOf((String) values.get(i));
            }
        }

        public List getValues() {
            return values;
        }

        public float getMindB() {
            int i = values.indexOf(getValue());
            return floatValues[i];
        }

        public int getWidthLimit() {
            return 127;
        }
    }

    /**
     * A MeterIndicator is the primary meter 'movement', the main reason for
     * these controls.
     */
    public static class MeterIndicator extends FloatControl {

        public MeterIndicator(String name) {
            super(METER_INDICATOR, name, meterLaw, 0.5f, -60f);
            indicator = true;
        }
    }
}
