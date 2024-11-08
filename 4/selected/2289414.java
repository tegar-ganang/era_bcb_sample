package drk.sound;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;
import java.util.*;

public class SoundStreamer extends Thread {

    File song;

    boolean isStopped;

    protected SoundStreamer(File f) {
        song = f;
        isStopped = false;
    }

    public synchronized void stopNow() {
        isStopped = true;
    }

    public void run() {
    }

    static Vector<SoundStreamer> Streams;

    static {
        Streams = new Vector<SoundStreamer>();
    }

    public static int playThreadedStreamedLooped(File song) {
        SoundStreamer th = new SoundStreamer(song);
        th.start();
        int i = Streams.size();
        Streams.add(th);
        return i;
    }

    public static boolean stopPlayImmediately(int id) {
        if (id >= Streams.size()) return false;
        try {
            Streams.elementAt(id).stopNow();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void playSoundFile(File song) {
        AudioInputStream din = null;
        @SuppressWarnings("serial")
        class SoundFinishedException extends Exception {
        }
        try {
            File file = song;
            AudioInputStream in = AudioSystem.getAudioInputStream(file);
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
                while ((nBytesRead = din.read(data, 0, data.length)) != -1) {
                    if (isStopped) throw new SoundFinishedException();
                    line.write(data, 0, nBytesRead);
                }
                line.drain();
                line.stop();
                line.close();
                din.close();
            }
        } catch (SoundFinishedException sfe) {
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

    public static void main(String[] args) {
        SoundStreamer.playThreadedStreamedLooped(new File(args[0]));
        class Lt extends Thread {

            public int sum = 0;

            public void run() {
                for (int i = 0; i < 0xFFFFFFF; i++) {
                    sum += i;
                    System.out.println(i);
                }
            }
        }
        Lt lt3 = new Lt();
        lt3.run();
    }
}
