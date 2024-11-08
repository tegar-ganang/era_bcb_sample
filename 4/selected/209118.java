package game;

import java.io.*;
import javax.sound.sampled.*;

/**
 *
 * @author priya
 */
public class SoundEngine {

    private Clip newSoundClip = null;

    private int newSoundDuration = 0;

    private int newSoundFrameLen = 0;

    public SoundEngine() {
    }

    /**
     * 
     * This method is used to play the sound file. It uses loadSound to load the sound File and 
     * it uses start() to start playing the file. In this context it is used to play only wmv files.
     * 
     * @param File soundFile
     * @return boolean
     */
    public boolean play(File soundFile) {
        if (loadSound(soundFile)) {
            newSoundClip.start();
            return true;
        } else {
            return false;
        }
    }

    private boolean loadSound(File file) {
        newSoundDuration = 0;
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = stream.getFormat();
            if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                stream = AudioSystem.getAudioInputStream(tmp, stream);
                format = tmp;
            }
            DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
            newSoundClip = (Clip) AudioSystem.getLine(info);
            newSoundClip.open(stream);
            newSoundFrameLen = (int) stream.getFrameLength();
            newSoundDuration = (int) (newSoundClip.getBufferSize() / (newSoundClip.getFormat().getFrameSize() * newSoundClip.getFormat().getFrameRate()));
            return true;
        } catch (Exception ex) {
            newSoundClip = null;
            return false;
        }
    }
}
