package org.tritonus.dsp.ais;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import org.tritonus.share.sampled.FloatSampleBuffer;
import org.tritonus.share.sampled.convert.TSynchronousFilteredAudioInputStream;

/** Base class for ... .
 */
public abstract class FloatAudioInputStream extends TSynchronousFilteredAudioInputStream {

    private AudioFormat intermediateFloatBufferFormat;

    private FloatSampleBuffer m_floatBuffer = null;

    public FloatAudioInputStream(AudioInputStream sourceStream, AudioFormat targetFormat) {
        super(sourceStream, new AudioFormat(targetFormat.getEncoding(), sourceStream.getFormat().getSampleRate(), targetFormat.getSampleSizeInBits(), targetFormat.getChannels(), targetFormat.getChannels() * targetFormat.getSampleSizeInBits() / 8, sourceStream.getFormat().getFrameRate(), targetFormat.isBigEndian()));
        int floatChannels = targetFormat.getChannels();
        intermediateFloatBufferFormat = new AudioFormat(targetFormat.getEncoding(), sourceStream.getFormat().getSampleRate(), targetFormat.getSampleSizeInBits(), floatChannels, floatChannels * targetFormat.getSampleSizeInBits() / 8, sourceStream.getFormat().getFrameRate(), targetFormat.isBigEndian());
    }

    protected int convert(byte[] inBuffer, byte[] outBuffer, int outByteOffset, int inFrameCount) {
        int sampleCount = inFrameCount * getOriginalStream().getFormat().getChannels();
        int byteCount = sampleCount * (getOriginalStream().getFormat().getSampleSizeInBits() / 8);
        if (m_floatBuffer == null) {
            m_floatBuffer = new FloatSampleBuffer();
        }
        m_floatBuffer.initFromByteArray(inBuffer, 0, byteCount, getOriginalStream().getFormat());
        convert(m_floatBuffer);
        m_floatBuffer.convertToByteArray(outBuffer, outByteOffset, intermediateFloatBufferFormat);
        return inFrameCount;
    }

    protected abstract void convert(FloatSampleBuffer buffer);
}
