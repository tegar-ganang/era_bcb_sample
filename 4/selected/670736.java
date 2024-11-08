package test.xito.media;

import javazoom.jl.decoder.Equalizer;
import javazoom.jl.player.Player;
import javax.sound.sampled.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.TimerTask;

/**
 * MainTest
 *
 * @author drichan
 */
public class MainTest {

    public static void main(String args[]) {
        jlayer();
    }

    public static void jlayer() {
        try {
            InputStream in = new URL("http://www.thestreetdate.com/mp3com/YoungVeins-Change.mp3").openStream();
            final Player player = new Player(in);
            Thread playThread = new Thread() {

                public void run() {
                    System.out.println("Start Playing");
                    try {
                        player.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            playThread.start();
            new javax.swing.Timer(1000, new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    System.out.println(player.getPosition());
                }
            }).start();
            new java.util.Timer().schedule(new TimerTask() {

                @Override
                public void run() {
                    System.out.println("Done Playing");
                    player.close();
                    System.exit(0);
                }
            }, 30000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void jmf() {
        AudioInputStream din = null;
        try {
            File f = new File("/Users/drichan/Music/iTunes/iTunes Music/Music/U2/Rattle And Hum/03 Desire.mp3");
            AudioInputStream in = AudioSystem.getAudioInputStream(f);
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
                    line.write(data, 0, nBytesRead);
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
