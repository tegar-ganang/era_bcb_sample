package clavicom.gui.engine.sound;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioSystem;
import java.io.File;

public class Sound {

    SourceDataLine source;

    AudioInputStream ais;

    public Sound(String fileName) {
    }

    public static void readAudioFile(String fileName) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(fileName));
            AudioFormat format = ais.getFormat();
            Info info = new Info(SourceDataLine.class, format);
            SourceDataLine source = (SourceDataLine) AudioSystem.getLine(info);
            source.open(format);
            source.start();
            int read = 0;
            byte[] audioData = new byte[16384];
            while (read > -1) {
                read = ais.read(audioData, 0, audioData.length);
                if (read >= 0) source.write(audioData, 0, read);
            }
            source.drain();
            source.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
