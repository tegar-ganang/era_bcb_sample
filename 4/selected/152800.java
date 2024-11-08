package uk.org.toot.audio.mixer;

import java.awt.Color;
import uk.org.toot.control.*;
import uk.org.toot.audio.core.*;
import static uk.org.toot.audio.mixer.MixerControlsIds.*;
import uk.org.toot.audio.fader.*;
import static uk.org.toot.audio.mixer.MixControlIds.*;
import static uk.org.toot.misc.Localisation.*;

/**
 * MixControls are the composite Controls for a MixProcess.
 */
public class MixControls extends AudioControls implements MixVariables {

    private static final float HALF_ROOT_TWO = (float) (Math.sqrt(2) / 2);

    private BooleanControl soloControl = null;

    private BooleanControl muteControl;

    private GainControl gainControl;

    private LCRControl lcrControl;

    private FrontRearControl frontRearControl;

    /**
     * @supplierCardinality 1
     * @link aggregationByValue 
     */
    private BusControls busControls;

    /**
     * @link aggregation
     * @supplierCardinality 1 
     */
    protected MixerControls mixerControls;

    private boolean isMaster;

    private int channelCount;

    private boolean mute, solo;

    private float gain, left, right, front, rear;

    public MixControls(MixerControls mixerControls, int stripId, BusControls busControls, boolean isMaster) {
        super(busControls.getId(), busControls.getName());
        this.mixerControls = mixerControls;
        this.busControls = busControls;
        this.isMaster = isMaster;
        int busId = busControls.getId();
        ChannelFormat format = getChannelFormat();
        channelCount = format.getCount();
        if (format.getLFE() >= 0) {
        }
        if (channelCount >= 4) {
            frontRearControl = new FrontRearControl();
            add(frontRearControl);
            derive(frontRearControl);
        }
        if (format.getCenter() >= 0 && channelCount > 1) {
        }
        if (channelCount > 1) {
            if (stripId == CHANNEL_STRIP) {
                PanControl pc = new PanControl();
                add(pc);
                lcrControl = pc;
            } else {
                BalanceControl bc = new BalanceControl();
                add(bc);
                lcrControl = bc;
            }
            derive(lcrControl);
        }
        ControlRow enables = new ControlRow();
        if (isMaster) {
            enables.add(busControls.getSoloIndicator());
        } else {
            enables.add(soloControl = createSoloControl());
            derive(soloControl);
            soloControl.addObserver(busControls);
        }
        enables.add(muteControl = createMuteControl());
        derive(muteControl);
        add(enables);
        if (busId == MAIN_BUS) {
            EnumControl routeControl = createRouteControl(stripId);
            if (routeControl != null) {
                add(routeControl);
            }
        }
        float initialdB = ((busId == AUX_BUS || busId == FX_BUS) && !isMaster) ? -FaderLaw.ATTENUATION_CUTOFF : 0f;
        gainControl = new GainControl(initialdB);
        gainControl.setInsertColor(isMaster ? Color.BLUE.darker() : Color.black);
        add(gainControl);
        derive(gainControl);
    }

    @Override
    protected void derive(Control c) {
        switch(c.getId()) {
            case MUTE:
                mute = muteControl.getValue();
                break;
            case SOLO:
                solo = soloControl.getValue();
                break;
            case GAIN:
                gain = gainControl.getGain();
                break;
            case LCR:
                left = lcrControl.getLeft();
                right = lcrControl.getRight();
                break;
            case FRONT_SURROUND:
                front = frontRearControl.getFront();
                rear = frontRearControl.getRear();
                break;
        }
    }

    public BooleanControl getSoloControl() {
        return soloControl;
    }

    public BooleanControl getMuteControl() {
        return muteControl;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public ChannelFormat getChannelFormat() {
        return busControls.getChannelFormat();
    }

    public boolean canBypass() {
        return false;
    }

    public boolean isAlwaysVertical() {
        return true;
    }

    public boolean canBeDeleted() {
        return false;
    }

    public boolean hasPresets() {
        return false;
    }

    public boolean isSolo() {
        return soloControl == null ? hasSolo() : solo;
    }

    public boolean isMute() {
        return mute;
    }

    public boolean isEnabled() {
        return !(isMute() || isSolo() != hasSolo());
    }

    public boolean hasSolo() {
        return busControls.hasSolo();
    }

    public float getGain() {
        return gain;
    }

    public void getChannelGains(float[] dest) {
        switch(channelCount) {
            case 6:
                dest[5] = gain;
                dest[4] = gain;
            case 4:
                final float r = gain * rear;
                dest[3] = r * right;
                dest[2] = r * left;
                final float f = gain * front;
                dest[1] = f * right;
                dest[0] = f * left;
                break;
            case 2:
                dest[1] = gain * right;
                dest[0] = gain * left;
                break;
            case 1:
                dest[0] = gain;
                break;
        }
    }

    protected EnumControl createRouteControl(int stripId) {
        return null;
    }

    protected BooleanControl createMuteControl() {
        BooleanControl c = new BooleanControl(MUTE, getString("Mute"), false);
        c.setAnnotation(c.getName().substring(0, 1));
        c.setStateColor(true, Color.orange);
        return c;
    }

    protected BooleanControl createSoloControl() {
        BooleanControl c = new BooleanControl(SOLO, getString("Solo"), false);
        c.setAnnotation(c.getName().substring(0, 1));
        c.setStateColor(true, Color.green);
        return c;
    }

    /**
     * An abstract implementation of a Left/Center/Right control such as
     * a pan or balance control.
     */
    public abstract static class LCRControl extends FloatControl {

        private static final String[] presetNames = { getString("Center"), getString("Left"), getString("Right") };

        public LCRControl(String name, ControlLaw law, float precision, float initialValue) {
            super(LCR, name, law, precision, initialValue);
            setInsertColor(java.awt.Color.pink);
        }

        public abstract float getLeft();

        public abstract float getRight();

        public String[] getPresetNames() {
            return presetNames;
        }

        public void applyPreset(String presetName) {
            if (presetName.equals(getString("Center"))) {
                setValue(0.5f);
            } else if (presetName.equals(getString("Left"))) {
                setValue(0f);
            } else if (presetName.equals(getString("Right"))) {
                setValue(1f);
            }
        }
    }

    /**
     * A PanControl implements stereo pan.
     */
    public static class PanControl extends LCRControl {

        private float left = HALF_ROOT_TWO;

        private float right = HALF_ROOT_TWO;

        public PanControl() {
            super(getString("Pan"), LinearLaw.UNITY, 0.01f, 0.5f);
        }

        public float getLeft() {
            return left;
        }

        public float getRight() {
            return right;
        }

        public void setValue(float value) {
            left = (float) Math.cos(Math.PI / 2 * value);
            right = (float) Math.sin(Math.PI / 2 * value);
            super.setValue(value);
        }
    }

    /**
     * A BalanceControl implements stereo balance.
     */
    public static class BalanceControl extends LCRControl {

        private float left = 1;

        private float right = 1;

        public BalanceControl() {
            super(getString("Balance"), LinearLaw.UNITY, 0.01f, 0.5f);
        }

        public float getLeft() {
            return left;
        }

        public float getRight() {
            return right;
        }

        public void setValue(float value) {
            left = value < 0.5f ? 1f : 2 * (1 - value);
            right = value > 0.5f ? 1f : 2 * value;
            super.setValue(value);
        }
    }

    /**
     * A FrontRearControl.
     */
    public static class FrontRearControl extends FloatControl {

        private float front = HALF_ROOT_TWO;

        private float rear = HALF_ROOT_TWO;

        private static final String[] presetNames = { getString("Front"), getString("Middle"), getString("Rear") };

        public FrontRearControl() {
            super(FRONT_SURROUND, getString("F.S"), LinearLaw.UNITY, 0.01f, 0.5f);
            setInsertColor(Color.GREEN.darker());
        }

        public float getFront() {
            return front;
        }

        public float getRear() {
            return rear;
        }

        public void setValue(float value) {
            front = (float) Math.cos(Math.PI / 2 * value);
            rear = (float) Math.sin(Math.PI / 2 * value);
            super.setValue(value);
        }

        public String[] getPresetNames() {
            return presetNames;
        }

        public void applyPreset(String presetName) {
            if (presetName.equals(getString("Middle"))) {
                setValue(0.5f);
            } else if (presetName.equals(getString("Front"))) {
                setValue(0f);
            } else if (presetName.equals(getString("Rear"))) {
                setValue(1f);
            }
        }
    }

    /**
     * A GainControl is a FaderControl which implements GainVariables.
     */
    public static class GainControl extends FaderControl {

        private float gain;

        public GainControl(float initialdB) {
            super(GAIN, FaderLaw.BROADCAST, initialdB);
            gain = (float) Math.pow(10.0, initialdB / 20.0);
            if (initialdB <= -FaderLaw.ATTENUATION_CUTOFF) {
                gain = 0f;
            }
        }

        public void setValue(float value) {
            if (value <= -FaderLaw.ATTENUATION_CUTOFF) {
                gain = 0f;
            } else {
                gain = (float) Math.pow(10.0, value / 20.0);
            }
            super.setValue(value);
        }

        public float getGain() {
            return gain;
        }
    }
}
