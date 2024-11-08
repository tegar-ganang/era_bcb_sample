package uk.org.toot.audio.delay;

import static uk.org.toot.audio.core.FloatDenormals.zeroDenorm;
import org.tritonus.share.sampled.FloatSampleBuffer;

/**
 * A DelayBuffer is a FloatSampleBuffer with convenience methods for delayed
 * signals, either buffered or unbuffered. The buffered methods are efficient
 * for static delays, the unbuffered methods are useful for modulated delays.
 */
public class DelayBuffer extends FloatSampleBuffer {

    private int writeIndex = 0;

    private int readIndex = 0;

    private float[] apzm1 = new float[6];

    private Filter[] lowpass;

    private float lowpassK = 1;

    public DelayBuffer(int channelCount, int sampleCount, float sampleRate) {
        super(channelCount, sampleCount, sampleRate);
        lowpass = new Filter[8];
        for (int i = 0; i < 8; i++) {
            lowpass[i] = new Filter();
        }
    }

    public void nudge(int on) {
        readIndex = writeIndex;
        writeIndex += on;
        int ns = getSampleCount();
        if (writeIndex >= ns) {
            writeIndex -= ns;
        }
    }

    /**
     * Append a single value to one channel of this buffer.
     */
    public void append(int chan, float value) {
        getChannel(chan)[writeIndex] = value;
    }

    /**
	 * Appends to this sample buffer, the data in <code>source</code>.
     * Performs an efficient array copy.
	 */
    public void append(FloatSampleBuffer source) {
        conform(source);
        int count = source.getSampleCount();
        int count2 = 0;
        if ((writeIndex + count) > getSampleCount()) {
            count = getSampleCount() - writeIndex;
            count2 = source.getSampleCount() - count;
        }
        for (int ch = 0; ch < source.getChannelCount(); ch++) {
            System.arraycopy(source.getChannel(ch), 0, getChannel(ch), writeIndex, count);
        }
        if (count2 > 0) for (int ch = 0; ch < source.getChannelCount(); ch++) {
            System.arraycopy(source.getChannel(ch), count, getChannel(ch), 0, count2);
        }
        nudge(source.getSampleCount());
    }

    /**
	 * Appends to this sample buffer, the data in <code>source1 + source2 * level2</code>
     * It's particularly useful for multitapped delays.
	 */
    public void append(FloatSampleBuffer source1, FloatSampleBuffer source2, float level2) {
        conform(source1);
        int count = source1.getSampleCount();
        int count2 = 0;
        if ((writeIndex + count) >= getSampleCount()) {
            count = getSampleCount() - writeIndex;
            count2 = source1.getSampleCount() - count;
        }
        for (int ch = 0; ch < source1.getChannelCount(); ch++) {
            float[] dest = getChannel(ch);
            float[] src1 = source1.getChannel(ch);
            float[] src2 = source2.getChannel(ch);
            for (int i = 0; i < count; i++) {
                dest[i + writeIndex] = src1[i] + level2 * src2[i];
            }
        }
        if (count2 > 0) for (int ch = 0; ch < source1.getChannelCount(); ch++) {
            float[] dest = getChannel(ch);
            float[] src1 = source1.getChannel(ch);
            float[] src2 = source2.getChannel(ch);
            for (int i = 0, j = count; i < count2; i++, j++) {
                dest[i] = src1[j] + level2 * src2[j];
            }
        }
        nudge(source1.getSampleCount());
    }

    /**
	 * Appends to this sample buffer, the data in <code>source1 + source2 * level2</code>
     * It's particularly useful for multitapped delays.
	 */
    public void appendFiltered(FloatSampleBuffer source1, FloatSampleBuffer source2, float level2, float k) {
        conform(source1);
        lowpassK = k;
        int count = source1.getSampleCount();
        int count2 = 0;
        if ((writeIndex + count) >= getSampleCount()) {
            count = getSampleCount() - writeIndex;
            count2 = source1.getSampleCount() - count;
        }
        for (int ch = 0; ch < source1.getChannelCount(); ch++) {
            Filter lp = lowpass[ch];
            float[] dest = getChannel(ch);
            float[] src1 = source1.getChannel(ch);
            float[] src2 = source2.getChannel(ch);
            for (int i = 0; i < count; i++) {
                dest[i + writeIndex] = src1[i] + lp.filter(level2 * src2[i]);
            }
        }
        if (count2 > 0) for (int ch = 0; ch < source1.getChannelCount(); ch++) {
            Filter lp = lowpass[ch];
            float[] dest = getChannel(ch);
            float[] src1 = source1.getChannel(ch);
            float[] src2 = source2.getChannel(ch);
            for (int i = 0, j = count; i < count2; i++, j++) {
                dest[i] = src1[j] + lp.filter(level2 * src2[j]);
            }
        }
        nudge(source1.getSampleCount());
    }

    public float outU(int chan, int delay) {
        int p = readIndex - delay;
        if (p < 0) p += getSampleCount();
        return getChannel(chan)[p];
    }

    public float out(int chan, float delay) {
        int ns = getSampleCount();
        float[] samples = getChannel(chan);
        int d1 = (int) delay;
        float w = delay - d1;
        int p1 = readIndex - d1;
        if (p1 < 0) p1 += ns;
        int p2 = readIndex - d1 - 1;
        if (p2 < 0) p2 += ns;
        return (samples[p1] * (1 - w)) + (samples[p2] * w);
    }

    public float outA(int chan, float delay) {
        int ns = getSampleCount();
        float[] samples = getChannel(chan);
        int d1 = (int) delay;
        float w = delay - d1;
        int p1 = readIndex - d1;
        if (p1 < 0) p1 += ns;
        int p2 = readIndex - d1 - 1;
        if (p2 < 0) p2 += ns;
        return apzm1[chan] = samples[p2] + (samples[p1] - apzm1[chan]) * ((1 - w) / (1 + w));
    }

    public void tap(FloatSampleBuffer buf, int delay, float weight) {
        for (int ch = 0; ch < buf.getChannelCount(); ch++) {
            tap(ch, buf, delay, weight);
        }
    }

    public void tap(int ch, FloatSampleBuffer buf, int delay, float weight) {
        if (weight < 0.001f) return;
        int sns = getSampleCount();
        float[] source = getChannel(ch);
        int dns = buf.getSampleCount();
        float[] dest = buf.getChannel(ch);
        int j = readIndex - delay;
        if (j < 0) j += sns;
        int count = Math.min(sns - j, dns);
        int i;
        for (i = 0; i < count; i++) {
            dest[i] += source[i + j] * weight;
        }
        j = -i;
        for (; i < dns; i++) {
            dest[i] += source[i + j] * weight;
        }
    }

    public void conform(FloatSampleBuffer buf) {
        while (getChannelCount() < buf.getChannelCount()) {
            addChannel(true);
        }
        if (getSampleRate() != buf.getSampleRate()) {
            setSampleRate(buf.getSampleRate());
            makeSilence();
        }
    }

    public float msToSamples(float ms) {
        return ms * getSampleRate() * 0.001f;
    }

    private class Filter {

        private float zm1 = 0;

        public float filter(float sample) {
            zm1 = zeroDenorm(zm1 + lowpassK * (sample - zm1));
            return zm1;
        }
    }
}
