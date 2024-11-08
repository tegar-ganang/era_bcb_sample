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

public class FileInput extends Thread implements AudioSource {

    int BUFFER_SIZE = 2560;

    String filename = "c:\\sepi16b8k.wav";

    boolean running = false;

    boolean suspended = false;

    long timeStamp = 0;

    long timeElapsed = 0;

    int bytesperframe = 0;

    int millisecondsperframe = 0;

    MathContext mc = null;

    File soundFile;

    AudioFormat format;

    AudioInputStream inputStream;

    int frameSize;

    byte[] frame;

    AudioSourceListener listener;

    public FileInput(AudioFormat f, int frameSize) {
        mc = new MathContext(4);
    }

    public void init(AudioSourceListener l, AudioFormat f, int fs) {
        listener = l;
        format = f;
        frameSize = fs;
        frame = new byte[fs];
        DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, format, BUFFER_SIZE);
        soundFile = new File(filename);
        if (!soundFile.exists()) {
            System.err.println("Wave file not found: " + filename);
            return;
        }
        inputStream = null;
        try {
            inputStream = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat ff = inputStream.getFormat();
            int bytespersecond = (int) (ff.getSampleRate() * ff.getFrameSize() * ff.getChannels());
            int framespersecond = bytespersecond / frameSize;
            millisecondsperframe = 1000 / framespersecond;
        } catch (UnsupportedAudioFileException e1) {
            e1.printStackTrace();
            return;
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
    }

    public void run() {
        if (inputStream == null) return;
        int nBytesRead = 0;
        int sentframes = 0;
        byte[] abData = new byte[frameSize];
        running = true;
        try {
            while (running) {
                try {
                    timeStamp = System.nanoTime();
                    do {
                        while (nBytesRead != -1) {
                            nBytesRead = inputStream.read(abData, 0, frameSize);
                            if (nBytesRead >= 0) {
                                listener.onIncomingRawFrame(abData);
                                sentframes++;
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
                            }
                        }
                        inputStream.close();
                        inputStream = AudioSystem.getAudioInputStream(soundFile);
                        System.out.println("End of file");
                        while (running) {
                            Thread.sleep(300);
                            sentframes++;
                            timeElapsed += millisecondsperframe * 1000000;
                            long currentTime = System.nanoTime();
                            long timespentincode = currentTime - timeStamp;
                            long delta = timeElapsed - timespentincode;
                            if (delta / 1000000 <= 20 || delta / 1000000 >= 60) {
                                System.out.print("audiotime=" + timeElapsed / 1000000);
                                System.out.print(", runtime=" + timespentincode / 1000000);
                                System.out.println(", delta=" + delta / 1000000);
                            }
                        }
                        if (listener != null) listener.onEndOfData(); else System.out.println("FileInput: listener==null!!!");
                        running = false;
                        halt();
                        break;
                    } while (true);
                } catch (IOException e) {
                    e.printStackTrace();
                    halt();
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
                inputStream.skip(inputStream.available());
                notify();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
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
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
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
}
