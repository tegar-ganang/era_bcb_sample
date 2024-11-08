package com.sesca.audio;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import com.sesca.misc.Logger;

public class SineInput extends Thread implements AudioSource {

    int k = 0;

    int BUFFER_SIZE = 2560;

    boolean running = false;

    boolean suspended = false;

    long timeStamp = 0;

    long timeElapsed = 0;

    int bytesperframe = 0;

    int millisecondsperframe = 0;

    MathContext mc = null;

    File soundFile;

    AudioFormat format;

    int frameSize;

    byte[] frame;

    AudioSourceListener listener;

    double f1 = 400;

    double f2 = 0;

    long index = 0;

    int hz = 8000;

    int bitRate = 8;

    int toneDurationInMilliS = 20;

    public SineInput(AudioFormat f, int frameSize) {
        mc = new MathContext(4);
    }

    public void init(AudioSourceListener l, AudioFormat f, int fs) {
        listener = l;
        format = f;
        frameSize = fs;
        frame = new byte[fs];
        DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, format, BUFFER_SIZE);
        bitRate = format.getSampleSizeInBits();
        int bytespersecond = (int) (f.getSampleRate() * f.getFrameSize() * f.getChannels());
        int framespersecond = bytespersecond / frameSize;
        millisecondsperframe = 1000 / framespersecond;
    }

    public void run() {
        running = true;
        try {
            while (running) {
                k++;
                if (suspended) {
                    wait();
                }
                try {
                    timeStamp = System.nanoTime();
                    do {
                        while (running && !suspended) {
                            if (running && !suspended) {
                                listener.onIncomingRawFrame(generateTone());
                                k++;
                                timeElapsed += millisecondsperframe * 1000000;
                                long currentTime = System.nanoTime();
                                long timespentincode = currentTime - timeStamp;
                                long delta = (timeElapsed - timespentincode) / 1000000;
                                if ((timeElapsed / 1000000) % 1000 == 1000) {
                                    System.out.print("audiotime=" + timeElapsed / 1000000);
                                    System.out.print(", runtime=" + timespentincode / 1000000);
                                    System.out.println(", delta=" + delta);
                                }
                                if (delta > 200) {
                                    Thread.sleep(delta - 200);
                                }
                            } else System.out.println("runnig=" + running + ", suspended=" + suspended);
                        }
                    } while (running && !suspended);
                } finally {
                }
            }
        } catch (Exception e) {
            running = false;
            e.printStackTrace();
            halt();
        }
    }

    public void halt() {
        suspended = true;
    }

    public boolean unhalt() {
        if (suspended == true) {
            try {
                suspended = false;
                notify();
            } finally {
            }
        }
        return true;
    }

    public void go() {
        Logger.debug("Starting FileInput");
        start();
    }

    public void close() {
        running = false;
        halt();
        try {
        } finally {
        }
    }

    byte[] flip(byte b[]) {
        byte c[] = new byte[b.length];
        int j = 0;
        for (int i = b.length - 1; i >= 0; i--) {
            c[j] = b[i];
        }
        return c;
    }

    byte[] generateTone() {
        long bytesPerSecond = hz * bitRate / 8;
        long satsi = bytesPerSecond * toneDurationInMilliS / 1000;
        long framesPerSatsi = satsi / frame.length;
        satsi = framesPerSatsi * frame.length;
        for (int k = 0; k < framesPerSatsi; k++) {
            for (int i = 0; i < frame.length; i += 2) {
                double d1 = 0;
                double d2 = 0;
                if (f1 != 0) d1 = 16383 / 2 * Math.sin(index * 2 * Math.PI * f1 / hz);
                if (f2 != 0) d2 = 16383 / 2 * Math.sin(index * 2 * Math.PI * f2 / hz);
                double d = d2 + d1;
                int sample = (int) d;
                frame[i] = (byte) (sample & 0xFF);
                frame[i + 1] = (byte) (int) ((sample >> 8) & 0xFF);
                index++;
            }
        }
        return frame;
    }
}
