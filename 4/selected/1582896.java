package tools;

import java.io.*;
import javax.sound.sampled.*;

/**
 * This class plays a sound.
 * @author johannes
 *
 */
public class PlaySound {

    Clip clip;

    /**
 * Plays a sound file, specified by the name attribute.
 * @param name
 * @param pan
 * @param gain
 * @throws Exception
 */
    public void playSampleFile(String name, float pan, float gain) throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(name));
        AudioFormat format = ais.getFormat();
        if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
            AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
            ais = AudioSystem.getAudioInputStream(tmp, ais);
            format = tmp;
        }
        DataLine.Info info = new DataLine.Info(Clip.class, format, ((int) ais.getFrameLength() * format.getFrameSize()));
        clip = (Clip) AudioSystem.getLine(info);
        clip.open(ais);
        clip.start();
        while (true) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
            if (!clip.isRunning()) {
                break;
            }
        }
        clip.stop();
        clip.close();
    }

    public void anhalten() {
        clip.stop();
        clip.close();
    }
}
