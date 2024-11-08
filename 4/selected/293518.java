package uk.org.toot.audio.distort;

import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.ChannelFormat;
import uk.org.toot.audio.core.SimpleAudioProcess;
import uk.org.toot.audio.filter.ToneStackSection;
import uk.org.toot.dsp.DCBlocker;
import uk.org.toot.dsp.filter.*;
import static uk.org.toot.dsp.FastMath.tanh;

public class GuitarAmpProcess extends SimpleAudioProcess {

    private Variables vars;

    private OverSampler overSampler;

    private DCBlocker dc1, dc2;

    private ToneStackSection stack;

    private int sampleRate = 44100;

    private boolean wasBypassed;

    private final int R = 3;

    public GuitarAmpProcess(Variables vars) {
        this.vars = vars;
        stack = new ToneStackSection();
        dc1 = new DCBlocker();
        dc2 = new DCBlocker();
        design();
    }

    private void design() {
        FIRSpecification s = new FIRSpecification();
        s.f1 = 0;
        s.fN = R * sampleRate / 2;
        s.dBatten = 48;
        s.dBripple = 3;
        s.f2 = 7000;
        s.ft = 20000 - s.f2;
        s.mod = R;
        s.order = -1;
        float[] ia = FIRDesignerPM.design(s);
        s.f2 = 3000;
        s.ft = 20000 - s.f2;
        s.mod = 0;
        s.order = -1;
        float[] da = FIRDesignerPM.design(s);
        overSampler = new FIROverSampler2(R, 1, ia, da);
    }

    public int processAudio(AudioBuffer buffer) {
        boolean bypassed = vars.isBypassed();
        if (bypassed) {
            if (!wasBypassed) {
                stack.clear();
                overSampler.clear();
                wasBypassed = true;
            }
            return AUDIO_OK;
        }
        wasBypassed = bypassed;
        if (buffer.getChannelCount() > 1) {
            buffer.convertTo(ChannelFormat.MONO);
        }
        int srate = (int) buffer.getSampleRate();
        if (srate != sampleRate) {
            sampleRate = srate;
            design();
            stack.updateCoefficients(vars.setSampleRate(R * sampleRate));
        } else if (vars.hasChanged()) {
            stack.updateCoefficients(vars.getCoefficients());
        }
        float bias = vars.getBias();
        float gain1 = vars.getGain1();
        float inverseGain1 = 1f / gain1;
        if (inverseGain1 < 0.1f) inverseGain1 = 0.1f;
        float gain2 = vars.getGain2();
        float inverseGain2 = 1f / gain2;
        if (inverseGain2 < 0.1f) inverseGain2 = 0.1f;
        inverseGain2 *= vars.getMaster();
        float[] upSamples;
        float sample;
        float[] samples = buffer.getChannel(0);
        int nsamples = buffer.getSampleCount();
        for (int s = 0; s < nsamples; s++) {
            upSamples = overSampler.interpolate(gain1 * samples[s], 0);
            for (int i = 0; i < upSamples.length; i++) {
                sample = tanh(bias + upSamples[i]);
                sample += 0.1f * sample * sample;
                sample = stack.filter(inverseGain1 * dc1.block(sample));
                sample = tanh(sample * gain2);
                sample += 0.1f * sample * sample;
                upSamples[i] = sample;
            }
            samples[s] = inverseGain2 * dc2.block(overSampler.decimate(upSamples, 0));
        }
        return AUDIO_OK;
    }

    public interface Variables {

        boolean isBypassed();

        float getBias();

        float getGain1();

        float getGain2();

        float getMaster();

        boolean hasChanged();

        ToneStackSection.Coefficients setSampleRate(float rate);

        ToneStackSection.Coefficients getCoefficients();
    }
}
