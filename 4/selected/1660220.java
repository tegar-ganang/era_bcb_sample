package net.sf.xwav.soundrenderer;

import java.io.*;
import java.util.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFileFormat;

public class WavWriter extends WaveWriter {

    private SoundDescriptor soundDescriptor;

    private OutputStream out;

    public enum Wave {

        SQUARE, SINE
    }

    ;

    /**
	 * @param stream
	 * @param soundDescriptor
	 */
    public WavWriter(OutputStream stream, SoundDescriptor soundDescriptor) {
        super(stream, soundDescriptor);
        this.soundDescriptor = soundDescriptor;
        this.out = stream;
    }

    protected AudioFormat getAudioFormat(SoundDescriptor soundDescriptor) {
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        boolean bigEndian = true;
        float sampleRate = soundDescriptor.getSampleRate();
        int sampleSizeInBits = soundDescriptor.getBitDepth();
        int channels = soundDescriptor.getNumberOfChannels();
        int frameSize = (sampleSizeInBits / 8) * channels;
        float frameRate = 0;
        if (encoding == AudioFormat.Encoding.PCM_SIGNED || encoding == AudioFormat.Encoding.PCM_UNSIGNED) {
            frameRate = sampleRate;
        } else {
            throw new Error("Can only assume frameRate == sampleRate when encoding=PCM");
        }
        return new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
    }

    @Override
    public void close() {
        try {
            this.out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected int getByteLength(SoundBuffer buffer, SoundDescriptor soundDescriptor) {
        return buffer.getDataLength() * soundDescriptor.getNumberOfChannels() * (soundDescriptor.getBitDepth() / 8);
    }

    protected byte[] convertBuffer(SoundBuffer buffer, SoundDescriptor soundDescriptor) {
        int channels = buffer.getNumberOfChannels();
        int length = getByteLength(buffer, soundDescriptor);
        byte[] outData = new byte[length];
        AudioFormat format = getAudioFormat(soundDescriptor);
        float ditherBits = 0;
        List<float[]> inDataList = new ArrayList<float[]>();
        try {
            for (int i = 0; i < channels; i++) {
                inDataList.add(buffer.getChannelData(i));
            }
        } catch (BadParameterException e) {
            e.printStackTrace();
        }
        int inOffset = 0;
        int outByteOffset = 0;
        FloatSampleTools.float2byte(inDataList, inOffset, outData, outByteOffset, (int) soundDescriptor.getNumberOfSamples(), format, ditherBits);
        return outData;
    }

    @Override
    public void write(SoundBuffer buffer) {
        AudioFormat audioFormat = getAudioFormat(this.soundDescriptor);
        AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;
        byte[] data = convertBuffer(buffer, this.soundDescriptor);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        AudioInputStream ais = new AudioInputStream(bais, audioFormat, data.length / audioFormat.getFrameSize());
        try {
            AudioSystem.write(ais, targetType, this.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static SoundBuffer generateTestData(float loudness, int sampleRate, int pitch, int channels, float lengthInSeconds, Wave wave) {
        float maximumBufferLengthInSeconds = lengthInSeconds;
        float[][] data;
        int maximumBufferLengthInFrames = (int) (maximumBufferLengthInSeconds * sampleRate);
        int periodLengthInFrames = sampleRate / pitch;
        if ((periodLengthInFrames % 2) != 0) periodLengthInFrames++;
        int numPeriodsInBuffer = maximumBufferLengthInFrames / periodLengthInFrames;
        int numFramesInBuffer = numPeriodsInBuffer * periodLengthInFrames;
        float amplitude = (float) (loudness / 100.0);
        data = new float[channels][numFramesInBuffer];
        float value = 0.0f;
        for (int period = 0; period < numPeriodsInBuffer; period++) {
            for (int frame = 0; frame < periodLengthInFrames; frame++) {
                if (wave == Wave.SQUARE) {
                    value = frame < (periodLengthInFrames / 2) ? amplitude : -amplitude;
                } else {
                    value = (float) (Math.sin(((double) frame / (double) periodLengthInFrames) * 2.0 * Math.PI) * amplitude);
                }
                int baseAddr = (period * periodLengthInFrames + frame);
                for (int channel = 0; channel < channels; channel++) {
                    try {
                        data[channel][baseAddr] = value;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        e.printStackTrace();
                        System.exit(0);
                    }
                }
            }
        }
        SoundBuffer buffer = new SoundBuffer(channels, numFramesInBuffer);
        return buffer;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        float loudness = 60.01f;
        int sampleRate = 44100;
        int pitch = 200;
        int channels = 2;
        float lengthInSeconds = 3.0F;
        Wave wave = Wave.SINE;
        SoundBuffer soundBuffer = generateTestData(loudness, sampleRate, pitch, channels, lengthInSeconds, wave);
        int bitDepth = 16;
        SoundDescriptor soundDescriptor = new SoundDescriptor();
        soundDescriptor.setBitDepth(bitDepth);
        soundDescriptor.setNumberOfChannels(channels);
        soundDescriptor.setNumberOfSamples(soundBuffer.getDataLength());
        soundDescriptor.setSampleRate(sampleRate);
        File outFile = new File("test.wav");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WavWriter wavWriter = new WavWriter(out, soundDescriptor);
        wavWriter.write(soundBuffer);
        wavWriter.close();
    }
}
