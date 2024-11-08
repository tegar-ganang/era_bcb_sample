package com.ibm.tuningfork.infra.util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioFormat.Encoding;
import com.ibm.tuningfork.infra.Logging;

public class AudioChunk {

    protected static final int TIME_DISTORT_CHUNKSIZE_BYTES = 4 << 10;

    public final AudioFormat audioFormat;

    public final int frameSize;

    public final double frameRate;

    protected byte[] data;

    protected int framesPerTimeDistort;

    protected final int sampleSizeInBits;

    protected final int sampleSizeInBytes;

    protected final AudioFormat.Encoding encoding;

    protected final int numChannels;

    protected final boolean littleEndian;

    protected final boolean isSignedSixteen;

    protected final boolean isUnsignedEight;

    protected final boolean isSignedEight;

    protected final double amplitudeRange;

    static long totalDuration = 0;

    static long totalDurationCount = 0;

    private int getSample(int pos) {
        int sample;
        if (isSignedSixteen) {
            int firstByte = data[pos] & 0xff;
            int secondByte = data[pos + 1];
            sample = littleEndian ? (firstByte + (secondByte << 8)) : (secondByte + (firstByte << 8));
        } else {
            if (isUnsignedEight) {
                sample = (data[pos] & 0xff) - 0x7f;
            } else {
                sample = data[pos];
            }
        }
        return sample;
    }

    public double[] getSamples() {
        double[] result = new double[data.length / frameSize];
        double minAmp = 1.0, maxAmp = -1.0;
        for (int pos = 0; pos < data.length; pos += frameSize) {
            double relativeAmplitude = 0.0;
            for (int ch = 0; ch < numChannels; ch++) {
                int sample = getSample(pos + ch * sampleSizeInBytes);
                relativeAmplitude += sample / amplitudeRange;
            }
            relativeAmplitude /= numChannels;
            result[pos / frameSize] = relativeAmplitude;
            minAmp = Math.min(minAmp, relativeAmplitude);
            maxAmp = Math.max(maxAmp, relativeAmplitude);
        }
        return result;
    }

    public static boolean supports(AudioFormat audioFormat) {
        Encoding encoding = audioFormat.getEncoding();
        int sampleSizeInBits = audioFormat.getSampleSizeInBits();
        boolean isSignedSixteen = (encoding == AudioFormat.Encoding.PCM_SIGNED && sampleSizeInBits == 16);
        boolean isUnsignedEight = (encoding == AudioFormat.Encoding.PCM_UNSIGNED && sampleSizeInBits == 8);
        boolean isSignedEight = (encoding == AudioFormat.Encoding.PCM_SIGNED && sampleSizeInBits == 8);
        return (isSignedSixteen || isUnsignedEight || isSignedEight);
    }

    public AudioChunk(AudioFormat audioFormat, byte[] data) {
        if (!supports(audioFormat)) {
            throw new NullPointerException();
        }
        this.audioFormat = audioFormat;
        frameSize = audioFormat.getFrameSize();
        frameRate = audioFormat.getFrameRate();
        framesPerTimeDistort = TIME_DISTORT_CHUNKSIZE_BYTES / frameSize;
        this.data = data;
        sampleSizeInBits = audioFormat.getSampleSizeInBits();
        sampleSizeInBytes = sampleSizeInBits / 8;
        encoding = audioFormat.getEncoding();
        numChannels = audioFormat.getChannels();
        littleEndian = !audioFormat.isBigEndian();
        isSignedSixteen = (encoding == AudioFormat.Encoding.PCM_SIGNED && sampleSizeInBits == 16);
        isUnsignedEight = (encoding == AudioFormat.Encoding.PCM_UNSIGNED && sampleSizeInBits == 8);
        isSignedEight = (encoding == AudioFormat.Encoding.PCM_SIGNED && sampleSizeInBits == 8);
        amplitudeRange = isSignedSixteen ? (double) 0x7fff : (double) 0x7f;
    }

    public int length() {
        return data.length;
    }

    public void write(SourceDataLine line) {
        line.write(data, 0, data.length);
    }

    public AudioChunk reverse() {
        byte[] newData = data.clone();
        int numFrames = newData.length / frameSize;
        byte[] chunk = new byte[frameSize];
        for (int i = 0; i < numFrames / 2; i++) {
            int from = i * frameSize;
            int to = (numFrames - 1 - i) * frameSize;
            System.arraycopy(newData, from, chunk, 0, frameSize);
            System.arraycopy(newData, to, newData, from, frameSize);
            System.arraycopy(chunk, 0, newData, to, frameSize);
        }
        return new AudioChunk(audioFormat, newData);
    }

    public AudioChunk timeDistort(double scale) {
        if (scale == 1.0) {
            return this;
        }
        if (scale == 0.0) {
            Logging.errorln("timeDistort with a scale of zero is non-sensical");
            return null;
        }
        byte newData[];
        int snippetSize = frameSize * framesPerTimeDistort;
        int numSnippets = data.length / snippetSize;
        if (scale < 1) {
            int slowDown = (int) (1 / scale);
            if (Math.abs(slowDown * scale - 1) > 1e-6) {
                Logging.error("Slowing down audio chunks by non-integral values not supported: scale = " + scale);
                return null;
            }
            newData = new byte[data.length * slowDown];
            for (int i = 0; i < numSnippets; i++) {
                int fromPos = i * snippetSize;
                for (int j = 0; j < slowDown; j++) {
                    int toPos = (i * slowDown + j) * snippetSize;
                    System.arraycopy(data, fromPos, newData, toPos, snippetSize);
                }
            }
        } else {
            int speedUp = (int) scale;
            if (speedUp != scale) {
                Logging.errorln("Speeding up by non-integral values not supported: scale = " + scale);
                return null;
            }
            int newNumSnippets = numSnippets / speedUp;
            newData = new byte[newNumSnippets * snippetSize];
            for (int i = 0; i < newNumSnippets; i++) {
                int fromPos = i * speedUp * snippetSize;
                int toPos = i * snippetSize;
                System.arraycopy(data, fromPos, newData, toPos, snippetSize);
            }
        }
        return new AudioChunk(audioFormat, newData);
    }
}
