package uk.org.toot.audio.delay;

import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;
import uk.org.toot.audio.core.ChannelFormat;
import uk.org.toot.dsp.FastMath;
import static uk.org.toot.audio.core.FloatDenormals.*;

/**
 * A Modulated Delay Process
 * Currently very crude with no interpolation so it's noisey at longer delays.
 */
public class ModulatedDelayProcess implements AudioProcess {

    /**
     * @link aggregationByValue
     * @supplierCardinality 1 
     */
    private DelayBuffer delayBuffer;

    /**
     * @link aggregation
     * @supplierCardinality 1 
     */
    private final Variables vars;

    protected int[] modulatorMap;

    protected float modulatorPhase, modulatorPhaseQuad;

    private boolean phaseQuad = false;

    private float[][] samples = new float[6][];

    protected ChannelFormat format;

    private boolean wasBypassed;

    public ModulatedDelayProcess(ModulatedDelayProcess.Variables vars) {
        this.vars = vars;
        modulatorMap = new int[8];
        wasBypassed = !vars.isBypassed();
    }

    public void open() {
    }

    public int processAudio(AudioBuffer buffer) {
        boolean bypassed = vars.isBypassed();
        if (bypassed) {
            if (!wasBypassed) {
                if (delayBuffer != null) delayBuffer.makeSilence();
                wasBypassed = true;
            }
            return AUDIO_OK;
        }
        wasBypassed = bypassed;
        phaseQuad = vars.isPhaseQuadrature();
        if (phaseQuad) buffer.monoToStereo();
        float sampleRate = buffer.getSampleRate();
        if (delayBuffer == null) {
            delayBuffer = new DelayBuffer(buffer.getChannelCount(), msToSamples(vars.getMaxDelayMilliseconds(), sampleRate), sampleRate);
        }
        int ns = buffer.getSampleCount();
        int nc = buffer.getChannelCount();
        float depth = vars.getDepth();
        float feedback = vars.getFeedback();
        float dry = vars.getDry();
        float wet = vars.getWet();
        int staticDelay = (int) (delayBuffer.msToSamples(vars.getDelayMilliseconds()));
        delayBuffer.conform(buffer);
        ChannelFormat f = buffer.getChannelFormat();
        if (format != f) {
            format = f;
            buildModulatorMap(buffer);
        }
        float timeDelta = 1 / sampleRate;
        float depth2 = staticDelay * depth;
        for (int c = 0; c < nc; c++) {
            samples[c] = buffer.getChannel(c);
        }
        for (int s = 0; s < ns; s++) {
            incrementModulators(timeDelta);
            for (int ch = 0; ch < nc; ch++) {
                float in = samples[ch][s];
                delayBuffer.append(ch, zeroDenorm(in - feedback * delayBuffer.outU(ch, staticDelay)));
                samples[ch][s] = dry * in + wet * delayBuffer.outA(ch, staticDelay + modulation(ch) * depth2);
            }
            delayBuffer.nudge(1);
        }
        return AUDIO_OK;
    }

    public void close() {
        delayBuffer = null;
    }

    protected void buildModulatorMap(AudioBuffer buffer) {
        int nc = buffer.getChannelCount();
        for (int ch = 0; ch < nc; ch++) {
            if (format.isLFE(ch)) modulatorMap[ch] = -1; else if (!phaseQuad) {
                modulatorMap[ch] = 0;
            } else {
                if (format.isLeft(ch)) modulatorMap[ch] = 0; else if (format.isRight(ch)) modulatorMap[ch] = 1; else modulatorMap[ch] = -1;
            }
        }
    }

    protected void incrementModulators(float timeDelta) {
        double phaseDelta = timeDelta * vars.getRate() * 2 * Math.PI;
        modulatorPhase += phaseDelta;
        if (modulatorPhase > Math.PI) {
            modulatorPhase -= 2 * Math.PI;
        }
        if (!phaseQuad) return;
        modulatorPhaseQuad = modulatorPhase + 0.5f * (float) Math.PI;
        if (modulatorPhaseQuad > Math.PI) {
            modulatorPhaseQuad -= 2 * Math.PI;
        }
    }

    protected float modulation(int chan) {
        if (modulatorMap[chan] < 0) return 0f;
        float phase = modulatorMap[chan] == 0 ? modulatorPhase : modulatorPhaseQuad;
        int shape = vars.getLFOShape();
        float mod = (shape == 0) ? FastMath.sin(phase) : FastMath.triangle(phase);
        if (mod < -1f) mod = -1f; else if (mod > 1f) mod = 1f;
        return mod;
    }

    protected int msToSamples(float ms, float sr) {
        return Math.round((ms * sr) / 1000);
    }

    public interface Variables extends DelayVariables {

        float getDelayMilliseconds();

        float getRate();

        float getDepth();

        float getFeedback();

        float getWet();

        float getDry();

        int getLFOShape();

        boolean isPhaseQuadrature();
    }
}
