package com.buckosoft.fibs.BuckoFIBS;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Let's make some noise. <br>
 * 
 * @author Dick Balaska
 * @author Ingo Macherius
 * @since 2009/02/02
 * @version $Revision: 1.6 $ <br>
 *          $Date: 2009/02/25 09:41:12 $
 * @see <a
 *      href="http://cvs.buckosoft.com/Projects/BuckoFIBS/BuckoFIBS/src/com/buckosoft/fibs/BuckoFIBS/AudioManager.java">cvs
 *      AudioManager.java</a>
 */
public class AudioManager {

    private static final boolean DEBUG = false;

    static final Clip[] clips = new Clip[AudioCue.values().length];

    /**
	 * The default constructor. Don't do anything useful because we want to
	 * catch the exceptions via the initialize method.
	 */
    public AudioManager() {
        initAudio();
    }

    /**
	 * Initialize the AudioManager.
	 * 
	 */
    public boolean initAudio() {
        boolean allClipsInitialized = true;
        try {
            for (AudioCue cue : AudioCue.values()) {
                if (DEBUG) {
                    System.out.println("Loading audio clip " + cue.getResourcePath());
                }
                clips[cue.ordinal()] = initializeClip(cue);
            }
        } catch (UnsupportedAudioFileException uafe) {
            allClipsInitialized = false;
        } catch (IOException ioe) {
            allClipsInitialized = false;
        } catch (LineUnavailableException lue) {
            allClipsInitialized = false;
        }
        return allClipsInitialized;
    }

    public void closeAudio() {
        for (AudioCue cue : AudioCue.values()) if ((clips[cue.ordinal()] != null) && (clips[cue.ordinal()].isOpen())) clips[cue.ordinal()].close();
    }

    public void play(AudioCue cue) {
        if (cue == null) return;
        try {
            if ((clips[cue.ordinal()] != null) && (clips[cue.ordinal()].isOpen())) {
                FloatControl gainControl = (FloatControl) clips[cue.ordinal()].getControl(FloatControl.Type.MASTER_GAIN);
                double gain = 0.8;
                float dB = (float) (Math.log(gain) / Math.log(10.0D) * 20.0D);
                gainControl.setValue(dB);
                clips[cue.ordinal()].start();
            }
        } catch (ArrayIndexOutOfBoundsException aiobe) {
            int panicSoundId = AudioCue.FibsAttention.ordinal();
            if ((clips[panicSoundId] != null) && (clips[panicSoundId].isOpen())) clips[panicSoundId].start();
        }
    }

    private Clip initializeClip(final AudioCue cue) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        if ((clips[cue.ordinal()] != null) && (clips[cue.ordinal()].isOpen())) {
            return clips[cue.ordinal()];
        }
        InputStream sampleStream = this.getClass().getResourceAsStream(cue.getResourcePath());
        AudioInputStream stream = AudioSystem.getAudioInputStream(sampleStream);
        AudioFormat format = stream.getFormat();
        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
            stream = AudioSystem.getAudioInputStream(format, stream);
        }
        DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), (int) stream.getFrameLength() * format.getFrameSize());
        Clip clip = (Clip) AudioSystem.getLine(info);
        clip.addLineListener(new LineListener() {

            public void update(LineEvent evt) {
                if (evt.getType() != LineEvent.Type.STOP) return;
                clips[cue.ordinal()].stop();
                clips[cue.ordinal()].setFramePosition(0);
            }
        });
        clip.open(stream);
        return clip;
    }
}
