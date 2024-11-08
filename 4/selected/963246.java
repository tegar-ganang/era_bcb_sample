package uk.org.toot.audio.dynamics;

import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.SimpleAudioProcess;
import static uk.org.toot.dsp.FastMath.*;

public abstract class MidSideDynamicsProcess extends SimpleAudioProcess {

    protected float[] envelope = new float[2];

    protected boolean isPeak = false;

    protected float[] threshold;

    protected float[] attack, release;

    protected float[] makeupGain;

    protected Variables vars;

    private boolean wasBypassed;

    private int sampleRate = 0;

    private int NSQUARESUMS = 10;

    private float[] squaresumsM = new float[NSQUARESUMS];

    private float[] squaresumsS = new float[NSQUARESUMS];

    private int nsqsum = 0;

    private float[] samplesM, samplesS;

    public MidSideDynamicsProcess(Variables vars) {
        this(vars, false);
    }

    public MidSideDynamicsProcess(Variables vars, boolean peak) {
        this.vars = vars;
        this.isPeak = peak;
        wasBypassed = !vars.isBypassed();
    }

    public void clear() {
        envelope[0] = envelope[1] = 1f;
        vars.setDynamicGain(1f, 1f);
    }

    /**
     * Called once per AudioBuffer
     */
    protected void cacheProcessVariables() {
        threshold = vars.getThreshold();
        attack = vars.getAttack();
        release = vars.getRelease();
        makeupGain = vars.getGain();
    }

    /**
     * Called once per AudioBuffer
     */
    public int processAudio(AudioBuffer buffer) {
        boolean bypassed = vars.isBypassed();
        if (bypassed) {
            if (!wasBypassed) {
                clear();
            }
            wasBypassed = true;
            return AUDIO_OK;
        }
        int sr = (int) buffer.getSampleRate();
        if (sr != sampleRate) {
            sampleRate = sr;
            vars.update(sr);
        }
        cacheProcessVariables();
        float targetGainM = 1f;
        float targetGainS = 1f;
        float gainM = 0f;
        float gainS = 0f;
        int len = buffer.getSampleCount();
        int mslen = (int) (buffer.getSampleRate() * 0.001f);
        float sumdiv = 1f / (mslen + mslen);
        if (!buffer.encodeMidSide()) return AUDIO_OK;
        samplesM = buffer.getChannel(0);
        samplesS = buffer.getChannel(1);
        for (int i = 0; i < len; i++) {
            float keyM = 0;
            float keyS = 0;
            if (isPeak) {
                keyM = max(keyM, abs(samplesM[i]));
                keyS = max(keyS, abs(samplesS[i]));
                targetGainM = function(0, keyM);
                targetGainS = function(1, keyS);
            } else if ((i % mslen) == 0 && (i + mslen) < len) {
                float sumOfSquaresM = 0f;
                float sumOfSquaresS = 0f;
                for (int j = 0, k = i; j < mslen; j++, k++) {
                    sumOfSquaresM += samplesM[k] * samplesM[k];
                    sumOfSquaresS += samplesS[k] * samplesS[k];
                }
                squaresumsM[nsqsum] = sumOfSquaresM * sumdiv;
                squaresumsS[nsqsum] = sumOfSquaresS * sumdiv;
                float meanM = 0;
                float meanS = 0;
                for (int s = 0; s < NSQUARESUMS; s++) {
                    meanM += squaresumsM[s];
                    meanS += squaresumsS[s];
                }
                if (++nsqsum >= NSQUARESUMS) nsqsum = 0;
                targetGainM = function(0, (float) sqrt(meanM / NSQUARESUMS));
                targetGainS = function(1, (float) sqrt(meanS / NSQUARESUMS));
            }
            gainM = dynamics(0, targetGainM);
            gainS = dynamics(1, targetGainS);
            samplesM[i] *= gainM * makeupGain[0];
            samplesS[i] *= gainS * makeupGain[1];
        }
        buffer.decodeMidSide();
        vars.setDynamicGain(gainM, gainS);
        wasBypassed = bypassed;
        return AUDIO_OK;
    }

    protected abstract float function(int i, float value);

    protected float dynamics(int i, float target) {
        float factor = target < envelope[i] ? attack[i] : release[i];
        envelope[i] = factor * (envelope[i] - target) + target;
        return envelope[i];
    }

    /**
     * Specifies parameters in implementation terms
     */
    public interface Variables {

        void update(float sampleRate);

        boolean isBypassed();

        float[] getThreshold();

        float[] getInverseThreshold();

        float[] getInverseRatio();

        float[] getKnee();

        float[] getAttack();

        int[] getHold();

        float[] getRelease();

        float[] getDepth();

        float[] getGain();

        void setDynamicGain(float gainM, float gainS);
    }
}
