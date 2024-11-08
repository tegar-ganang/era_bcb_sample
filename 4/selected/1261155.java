package org.rdv.datapanel.audio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class Player {

    public static final float DEFAULT_SAMPLE_TIME = 5;

    public static final float DEFAULT_SAMPLE_RATE = 44100;

    public static final int DEFAULT_SAMPLE_SIZE = 16;

    public static final int DEFAULT_NUM_CHANNELS = 1;

    public static final boolean DEFAULT_SIGNED = true;

    public static final boolean DEFUALT_BIGENDIAN = false;

    private AudioFormat format;

    private int bufferSize;

    private int numSamples;

    public Player() {
        this(DEFAULT_SAMPLE_TIME, DEFAULT_SAMPLE_RATE, DEFAULT_SAMPLE_SIZE, DEFAULT_NUM_CHANNELS, DEFAULT_SIGNED, DEFUALT_BIGENDIAN);
    }

    public Player(float sampleTime, float sampleRate, int sampleSize, int numChannels, boolean signed, boolean bigEndian) {
        format = new AudioFormat(sampleRate, sampleSize, numChannels, signed, bigEndian);
        bufferSize = (int) (sampleTime * sampleRate * sampleSize) / 8;
        numSamples = (int) (sampleTime * sampleRate);
    }

    private class AudioBlock {

        public double time;

        public byte[] data;

        public AudioBlock(byte[] data, double time) {
            this.data = data;
            this.time = time;
        }
    }

    private AudioBlock playing, next;

    public void play(final byte[] audio, double timestamp) {
        if (playing == null) {
            System.out.println("play");
            playing = new AudioBlock(audio, timestamp);
            outputAudio();
        } else {
            System.out.println("queue");
            next = new AudioBlock(audio, timestamp);
        }
    }

    public boolean isQueued(double timestamp) {
        return (playing != null && playing.time == timestamp) || (next != null && next.time == timestamp);
    }

    private void outputAudio() {
        new Thread() {

            public void run() {
                try {
                    AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(playing.data), format, numSamples);
                    AudioFormat audioFormat = in.getFormat();
                    AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioFormat.getSampleRate(), 16, audioFormat.getChannels(), audioFormat.getChannels() * 2, audioFormat.getSampleRate(), false);
                    AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(decodedFormat);
                    rawplay(din, line);
                    in.close();
                } catch (Exception e) {
                }
                if (next != null) {
                    playing = next;
                    next = null;
                    outputAudio();
                    System.out.println("play queued!");
                } else {
                    playing = null;
                }
            }
        }.start();
    }

    public AudioFormat getFormat() {
        return format;
    }

    private void rawplay(AudioInputStream din, SourceDataLine line) throws IOException {
        line.start();
        byte[] data = new byte[4096];
        int nBytesRead = 0;
        while (nBytesRead != -1) {
            nBytesRead = din.read(data, 0, data.length);
            if (nBytesRead != -1) {
                line.write(data, 0, nBytesRead);
            }
        }
        line.drain();
        line.stop();
        line.close();
        din.close();
    }

    public String toString() {
        return format + "\tBuffer: " + bufferSize + " bytes\t# of Samples: " + numSamples;
    }
}
