package de.mcb.sampler;

import de.mcb.util.Sample;
import de.mcb.util.WaveSample;
import de.mcb.pitcher.PitchedSample;

/**
 * User: Max
 * Date: 20.12.2009
 */
public class Button {

    private String name;

    private int param;

    private int channel;

    private boolean looping;

    private boolean ownBpm = false;

    private float bpm = -1;

    private WaveSample baseSample;

    private PitchedSample pitchedSample;

    private PitchedSample preparedSample;

    public Button(String name, int param, int channel, boolean looping) {
        this.name = name;
        this.param = param;
        this.channel = channel;
        this.looping = looping;
    }

    public Button(String name, int param, boolean looping) {
        this(name, param, 0, looping);
    }

    public Button(String name, int param) {
        this(name, param, 0, false);
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public String getName() {
        return name;
    }

    public int getParam() {
        return param;
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public PitchedSample getPitchedSample() {
        return pitchedSample;
    }

    public void setPitchedSample(PitchedSample pitchedSample) {
        System.out.println(this + " set pitched sample");
        this.pitchedSample = pitchedSample;
    }

    public PitchedSample getPreparedSample() {
        return preparedSample;
    }

    public void setPreparedSample(PitchedSample preparedSample) {
        System.out.println(this + " set prepared sample");
        this.preparedSample = preparedSample;
    }

    public WaveSample getBaseSample() {
        return baseSample;
    }

    public void setBaseSample(WaveSample baseSample) {
        System.out.println(this + " set base sample");
        this.baseSample = baseSample;
    }

    public void resetSample() {
        if (baseSample != null) baseSample.reset();
        if (pitchedSample != null) pitchedSample.reset();
        if (preparedSample != null) preparedSample.reset();
    }

    public float getBpm() {
        return bpm;
    }

    public void setBpm(float bpm) {
        this.bpm = bpm;
    }

    public boolean isOwnBpm() {
        return ownBpm;
    }

    public void setOwnBpm(boolean ownBpm) {
        this.ownBpm = ownBpm;
    }

    public String toString() {
        return name;
    }

    public float nextValue() {
        Sample sample = getSample();
        if (sample != null) return sample.nextValue();
        return 0;
    }

    public int valuesRemaining() {
        Sample sample = getSample();
        if (sample != null) return sample.valuesRemaining();
        return 0;
    }

    public String getSampleType() {
        return getSample().toString();
    }

    private Sample getSample() {
        if (pitchedSample != null) return pitchedSample;
        return baseSample;
    }
}
