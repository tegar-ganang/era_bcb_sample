package co.edu.unal.ungrid.client.util;

import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class SoundManager {

    private SoundManager() {
    }

    static class PlayThread extends Thread {

        PlayThread(byte[] buff, SourceDataLine line, int times) {
            this.buff = buff;
            this.line = line;
            this.times = times;
        }

        public void run() {
            try {
                for (int i = 0; i < times; i++) {
                    line.write(buff, 0, buff.length);
                    line.drain();
                    Thread.sleep(2000);
                }
                line.close();
            } catch (Exception exc) {
                System.out.println(exc);
            }
        }

        byte[] buff;

        SourceDataLine line;

        int times;
    }

    public static void play(String sResource, int nTimes) {
        try {
            File f = new File(sResource);
            if (f.exists()) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(f);
                AudioFormat format = ais.getFormat();
                AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                ais = AudioSystem.getAudioInputStream(tmp, ais);
                AudioFormat af = ais.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(af);
                line.start();
                byte[] buff = new byte[(int) f.length()];
                ais.read(buff, 0, buff.length);
                Thread playThread = new Thread(new PlayThread(buff, line, nTimes));
                playThread.start();
            }
        } catch (Exception exc) {
            System.out.println("SoundManager::play(): " + exc);
        }
    }
}
