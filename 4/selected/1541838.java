package spacewars.som;

import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioFormatToPCM {

    public static AudioInputStream getPCM(URL url) throws UnsupportedAudioFileException, IOException {
        AudioInputStream in = AudioSystem.getAudioInputStream(url);
        AudioFormat audioFormat = in.getFormat();
        if ((audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) && (audioFormat.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED)) {
            AudioFormat newFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioFormat.getSampleRate(), 16, audioFormat.getChannels(), audioFormat.getChannels() * 2, audioFormat.getSampleRate(), false);
            in = AudioSystem.getAudioInputStream(newFormat, in);
        }
        return in;
    }
}
