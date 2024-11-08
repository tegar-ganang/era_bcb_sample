package org.jtweet.util;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundManager implements Runnable {

    private static SoundManager instance;

    private static Thread thread;

    private boolean isDisposed;

    private String file;

    private SoundManager() {
        isDisposed = false;
        file = null;
    }

    public static SoundManager get() {
        if (instance == null) {
            instance = new SoundManager();
            thread = new Thread(instance);
            thread.start();
        }
        return instance;
    }

    public void run() {
        synchronized (thread) {
            try {
                while (!isDisposed) {
                    thread.wait();
                    if (file != null) {
                        try {
                            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(file));
                            AudioFormat af = ais.getFormat();
                            DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
                            if (AudioSystem.isLineSupported(info)) {
                                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                                line.open(af);
                                line.start();
                                byte[] data = new byte[(int) af.getFrameRate() * af.getFrameSize() / 10];
                                int read;
                                while ((read = ais.read(data)) != -1) {
                                    line.write(data, 0, read);
                                }
                                line.drain();
                                line.stop();
                                line.close();
                            }
                        } catch (UnsupportedAudioFileException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (LineUnavailableException e) {
                            e.printStackTrace();
                        } finally {
                            file = null;
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void notifyThread() {
        synchronized (thread) {
            thread.notify();
        }
    }

    public void dispose() {
        isDisposed = true;
        notifyThread();
    }

    public void playSound(String sound) {
        file = sound;
        notifyThread();
    }
}
