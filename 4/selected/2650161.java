package Gui;

import java.io.IOException;
import javax.sound.sampled.*;

public class SoundPlayer implements Runnable {

    private boolean paused = true;

    private boolean stop = false;

    private Object lock = new Object();

    private String song1;

    private String song2;

    private String song3;

    public SoundPlayer(String songName1, String songName2, String songName3) {
        this.song1 = songName1;
        this.song2 = songName2;
        this.song3 = songName3;
    }

    public SoundPlayer(String songName) {
        this.song1 = songName;
    }

    public void pause() {
        paused = true;
    }

    public void stop() {
        stop = true;
    }

    public void play() {
        synchronized (lock) {
            paused = false;
            lock.notifyAll();
        }
    }

    public boolean getStatus() {
        return paused;
    }

    public void run() {
        while (true) {
            playSong(song1);
            stop = false;
            if (song2 != null) playSong(song2);
            stop = false;
            if (song3 != null) playSong(song3);
            stop = false;
        }
    }

    private void playSong(String fileName) {
        paused = false;
        AudioInputStream din = null;
        try {
            AudioInputStream in = AudioSystem.getAudioInputStream(getClass().getResourceAsStream(fileName));
            AudioFormat baseFormat = in.getFormat();
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
            din = AudioSystem.getAudioInputStream(decodedFormat, in);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            if (line != null) {
                line.open(decodedFormat);
                byte[] data = new byte[4096];
                line.start();
                int nBytesRead;
                synchronized (lock) {
                    while ((nBytesRead = din.read(data, 0, data.length)) != -1) {
                        if (stop) break;
                        while (paused) {
                            if (line.isRunning()) {
                                line.stop();
                            }
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                        if (!line.isRunning()) {
                            line.start();
                        }
                        line.write(data, 0, nBytesRead);
                    }
                }
                line.drain();
                line.stop();
                line.close();
                din.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (din != null) {
                try {
                    din.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
