package uk.org.toot.audio.delay;

import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;
import uk.org.toot.dsp.FastMath;
import uk.org.toot.misc.Tempo;

/**
 * A Tempo linked Delay Process
 * Basically delegating to DelayBuffer
 */
public class TempoDelayProcess implements AudioProcess {

    /**
     * @link aggregationByValue
     * @supplierCardinality 1 
     */
    private DelayBuffer delayBuffer;

    /**
     * @link aggregationByValue
     * @supplierCardinality 1 
     */
    private DelayBuffer tappedBuffer;

    private Tempo.Listener tempoListener;

    /**
     * @link aggregation
     * @supplierCardinality 1 
     */
    private final Variables vars;

    private boolean wasBypassed;

    private float bpm = 120f;

    private float meansquare = 0f;

    private float meanK = 0.2f;

    private float smoothedMix;

    public TempoDelayProcess(Variables vars) {
        this.vars = vars;
        wasBypassed = !vars.isBypassed();
        tempoListener = new Tempo.Listener() {

            public void tempoChanged(float newTempo) {
                bpm = newTempo;
            }
        };
        smoothedMix = vars.getMix();
    }

    public void open() {
        Tempo.addTempoListener(tempoListener);
    }

    public int processAudio(AudioBuffer buffer) {
        boolean bypassed = vars.isBypassed();
        if (bypassed) {
            if (!wasBypassed) {
                if (delayBuffer != null) {
                    delayBuffer.makeSilence();
                }
                wasBypassed = true;
            }
            return AUDIO_OK;
        }
        float sampleRate = buffer.getSampleRate();
        int ns = buffer.getSampleCount();
        int nc = buffer.getChannelCount();
        float feedback = vars.getFeedback();
        float mix = vars.getMix();
        if (delayBuffer == null) {
            delayBuffer = new DelayBuffer(nc, msToSamples(vars.getMaxDelayMilliseconds(), sampleRate), sampleRate);
        } else {
            delayBuffer.conform(buffer);
        }
        if (tappedBuffer == null) {
            tappedBuffer = new DelayBuffer(nc, ns, sampleRate);
        } else {
            tappedBuffer.conform(buffer);
            if (tappedBuffer.getSampleCount() != ns) {
                tappedBuffer.changeSampleCount(ns, false);
            }
        }
        float ducking = vars.getDucking();
        if (ducking < 1f) {
            float square = buffer.square();
            meansquare += meanK * (square - meansquare);
            float rms = 10f * (float) FastMath.sqrt(meansquare);
            if (rms < ducking) {
            } else if (rms > 1f) {
                mix *= ducking;
            } else {
                mix *= ducking / rms;
            }
        }
        smoothedMix += 0.05f * (mix - smoothedMix);
        tappedBuffer.makeSilence();
        int delay = (int) msToSamples(60000 * vars.getDelayFactor() / bpm, sampleRate);
        for (int c = 0; c < nc; c++) {
            if (delay < ns) continue;
            delayBuffer.tap(c, tappedBuffer, delay, 1f);
        }
        delayBuffer.appendFiltered(buffer, tappedBuffer, feedback * 1.1f, vars.getLowpassCoefficient());
        for (int c = 0; c < nc; c++) {
            float[] samples = buffer.getChannel(c);
            float[] tapped = tappedBuffer.getChannel(c);
            for (int i = 0; i < ns; i++) {
                samples[i] += smoothedMix * tapped[i];
            }
        }
        wasBypassed = bypassed;
        return AUDIO_OK;
    }

    public void close() {
        delayBuffer = null;
        tappedBuffer = null;
        Tempo.removeTempoListener(tempoListener);
    }

    protected int msToSamples(float ms, float sr) {
        return (int) ((ms * sr) / 1000);
    }

    public interface Variables extends DelayVariables {

        float getDelayFactor();

        float getFeedback();

        float getMix();

        float getDucking();

        float getLowpassCoefficient();
    }
}
