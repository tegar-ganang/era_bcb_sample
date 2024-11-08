package xtrememp.player.dsp;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author Besmir Beqiri
 */
public class DssContext {

    private SourceDataLine sourceDataLine;

    private AudioFormat audioFormat;

    private FloatBuffer[] channelsBuffer;

    private int offset;

    private int sampleSize;

    private int channels;

    private int frameSize;

    private int ssib;

    private int channelSize;

    private float audioSampleSize;

    /**
     * Create a DSS context from a source data line with a fixed sample size.
     *
     * @param sourceDataLine The source data line.
     * @param sampleSize The sample size.
     */
    public DssContext(SourceDataLine sourceDataLine, int sampleSize) {
        this.sourceDataLine = sourceDataLine;
        this.audioFormat = sourceDataLine.getFormat();
        this.sampleSize = sampleSize;
        channels = audioFormat.getChannels();
        frameSize = audioFormat.getFrameSize();
        ssib = audioFormat.getSampleSizeInBits();
        channelSize = frameSize / channels;
        audioSampleSize = (1 << (ssib - 1));
        this.channelsBuffer = new FloatBuffer[channels];
        for (int ch = 0; ch < channels; ch++) {
            channelsBuffer[ch] = FloatBuffer.allocate(sampleSize);
        }
    }

    public void normalizeData(ByteBuffer audioDataBuffer) {
        long lfp = sourceDataLine.getLongFramePosition();
        offset = (int) ((long) (lfp * frameSize) % (long) (audioDataBuffer.capacity()));
        for (int sp = 0, pos = offset; sp < sampleSize; sp++, pos += frameSize) {
            if (pos >= audioDataBuffer.capacity()) {
                pos = 0;
            }
            for (int ch = 0, cdp = 0; ch < channels; ch++, cdp += channelSize) {
                float sm = (audioDataBuffer.get(pos + cdp) & 0xFF) - 128.0F;
                for (int bt = 8, bp = 1; bt < ssib; bt += 8) {
                    sm += audioDataBuffer.get(pos + cdp + bp) << bt;
                    bp++;
                }
                channelsBuffer[ch].put(sp, sm / audioSampleSize);
            }
        }
    }

    /**
     * Returns a normalized sample from the DSS data buffer.
     *
     * @return An array of {@link FloatBuffer}.
     */
    public FloatBuffer[] getDataNormalized() {
        return channelsBuffer;
    }

    /**
     * Returns the sample size to read from the data buffer.
     *
     * @return The sample size to read from the data buffer
     */
    public int getSampleSize() {
        return sampleSize;
    }

    /**
     * Returns the data buffer offset to start reading from. Please note that the offset + length
     * can be beyond the buffer length. This simply means, the rest of data sample has rolled over
     * to the beginning of the data buffer.
     *
     * @return The data buffer offset to start reading from.
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Returns the monitored source data line.
     *
     * @return A {@link SourceDataLine} object.
     */
    public SourceDataLine getSourceDataLine() {
        return sourceDataLine;
    }
}
