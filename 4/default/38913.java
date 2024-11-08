import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 *
 * @author rudieri
 */
public class Ogg {

    private String arquivo;

    int bytes = 0;

    public void testPlay(String filename) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        File file = new File(filename);
        AudioInputStream in = AudioSystem.getAudioInputStream(file);
        AudioInputStream din = null;
        if (in != null) {
            AudioFormat baseFormat = in.getFormat();
            System.out.println(in.getFormat());
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
            din = AudioSystem.getAudioInputStream(decodedFormat, in);
            rawplay(decodedFormat, din);
            in.close();
        }
    }

    byte[] data;

    private void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
        data = new byte[4096];
        SourceDataLine line = getLine(targetFormat);
        if (line != null) {
            line.start();
            int nBytesRead = 0, nBytesWritten = 0;
            bytes = 0;
            while (nBytesRead != -1) {
                nBytesRead = din.read(data, 0, data.length);
                bytes += nBytesRead;
                if (nBytesRead != -1) {
                    nBytesWritten = line.write(data, 0, nBytesRead);
                }
            }
            line.drain();
            line.stop();
            line.close();
            din.close();
        }
    }

    private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    public void play(String file) {
        arquivo = file;
        Thread th = new Thread(new Runnable() {

            public void run() {
                try {
                    testPlay(arquivo);
                } catch (UnsupportedAudioFileException ex) {
                    Logger.getLogger(Ogg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(Ogg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (LineUnavailableException ex) {
                    Logger.getLogger(Ogg.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        th.start();
        Thread th2 = new Thread(new Runnable() {

            public void run() {
                while (true) {
                    System.out.println(bytes);
                    try {
                        Thread.sleep(750);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Ogg.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
    }

    public static void main(String agruments[]) {
        Ogg ogg = new Ogg();
        ogg.play("/home/rudieri/OutPut/Beautiful Dangerous.ogg");
        System.out.println("THread");
        Scanner sc = new Scanner(System.in);
    }
}
