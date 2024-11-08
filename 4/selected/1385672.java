package quizgame.common;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 *
 * @author duchen
 */
public class SoundPlayer {

    private Map<String, Clip> soundClipMap;

    private boolean soundEnabled = true;

    private static SoundPlayer theInstance;

    private int masterGain = 100;

    /** Creates a new instance of SoundPlayer */
    private SoundPlayer() {
        soundClipMap = Collections.synchronizedMap(new HashMap<String, Clip>());
    }

    public static SoundPlayer getInstance() {
        if (theInstance == null) {
            theInstance = new SoundPlayer();
        }
        return theInstance;
    }

    /**
     *  Loads a sound and prepares it for playback.
     *  @param sound The sound to load and prepare.
     */
    public void loadSound(String filename, String sound) {
        synchronized (soundClipMap) {
            AudioInputStream ais = null;
            try {
                ais = AudioSystem.getAudioInputStream(new File(filename));
            } catch (UnsupportedAudioFileException ex) {
                ex.printStackTrace();
                return;
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
            AudioFormat audioFormat = ais.getFormat();
            if (audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioFormat.getSampleRate(), 16, audioFormat.getChannels(), audioFormat.getChannels() * 2, audioFormat.getSampleRate(), false);
                ais = AudioSystem.getAudioInputStream(targetFormat, ais);
                audioFormat = ais.getFormat();
            }
            DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
            Clip clip = null;
            try {
                clip = (Clip) AudioSystem.getLine(info);
            } catch (LineUnavailableException ex) {
                ex.printStackTrace();
                return;
            }
            try {
                clip.open(ais);
            } catch (LineUnavailableException ex) {
                ex.printStackTrace();
                return;
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
            soundClipMap.put(sound, clip);
            FloatControl c = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            c.setValue(calcGain(c, masterGain));
        }
    }

    /**
     *  Tells whether a sound is loaded or not.
     *  @param sound The sound to check.
     *  @return true if loaded. Otherwise false.
     */
    public boolean isLoaded(String sound) {
        if (soundClipMap.containsKey(sound)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *  Starts to play a loaded sound.
     *  @param sound The loaded sound to play.
     */
    public void play(String sound) {
        if (soundEnabled) {
            if (soundClipMap.containsKey(sound)) {
                Clip clip = soundClipMap.get(sound);
                clip.flush();
                clip.setFramePosition(0);
                clip.start();
            } else {
                System.out.println("Tried to play() a sound that wasn't loaded.");
            }
        }
    }

    /**
     *  This method stops a sound that is currently playing and resets the sounds position to zero.
     *  @param sound The sound to stop.
     */
    public void stop(String sound) {
        if (soundClipMap.containsKey(sound)) {
            Clip clip = soundClipMap.get(sound);
            clip.stop();
            clip.flush();
            clip.setFramePosition(0);
        } else {
            System.out.println("Tried to stop() a sound that wasn't loaded.");
        }
    }

    /**
     *  This method will make the sound to loop.
     *  @param sound The loaded sound to loop.
     *  @param nLoops If Greater than zero, it describes number of loops. If zero, the loop is canceled. If lesser than zero, the sound will loop continuously.
     */
    public void loop(String sound, int nLoops) {
        if (soundEnabled) {
            if (soundClipMap.containsKey(sound)) {
                Clip clip = soundClipMap.get(sound);
                if (nLoops < 0) {
                    clip.loop(clip.LOOP_CONTINUOUSLY);
                } else {
                    clip.loop(nLoops);
                }
            }
        }
    }

    /**
     *  This method makes a playing sound to pause. Use play() to make the sound play again from the paused position.
     *  @param sound The sound to pause.
     */
    public void pause(String sound) {
        if (soundClipMap.containsKey(sound)) {
            Clip clip = soundClipMap.get(sound);
            clip.stop();
            clip.flush();
        }
    }

    /**
     *  Stops all sound currently playing and resets their position to zero.
     */
    public void stopAll() {
        for (Clip clip : soundClipMap.values()) {
            clip.stop();
            clip.flush();
            clip.setFramePosition(0);
        }
    }

    /**
     *  Pauses all sound currently playing and keeps their position.
     */
    public void pauseAll() {
        for (Clip clip : soundClipMap.values()) {
            clip.stop();
            clip.flush();
        }
    }

    /**
     *  Unloads a sound and releases any system resources connected to it.
     *  @param sound The sound to unload.
     */
    public void unloadSound(String sound) {
        synchronized (soundClipMap) {
            if (soundClipMap.containsKey(sound)) {
                Clip clip = soundClipMap.get(sound);
                clip.stop();
                clip.flush();
                clip.close();
                soundClipMap.remove(sound);
            }
        }
    }

    /**
     *  Unloads all sounds that has been loaded and releases any system resource the sounds had.
     */
    public void unloadAllsounds() {
        synchronized (soundClipMap) {
            for (Clip clip : soundClipMap.values()) {
                clip.stop();
                clip.flush();
                clip.close();
            }
            soundClipMap.clear();
        }
    }

    /**
     *  Used by Options. Turns all sound off/on.
     *  @param value true means on, false means off.
     */
    public void setSoundEnabled(boolean value) {
        soundEnabled = value;
        if (!soundEnabled) {
            stopAll();
        }
    }

    /**
     *  Used by Options. Tells if sound is off or on.
     *  @return true if sound is on. Otherwise false.
     */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    /**
     *  Sets the gain of a playing sound.
     *  @param sound A loaded sound.
     *  @param value An int from 1-100 describing the new gain.
     */
    public void setGain(String sound, int value) {
        if (soundClipMap.containsKey(sound)) {
            Clip clip = soundClipMap.get(sound);
            FloatControl c = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            c.setValue(calcGain(c, value));
        }
    }

    /**
     *  Sets the master gain which will be used on all sounds.
     */
    public void setMasterGain(int value) {
        synchronized (soundClipMap) {
            for (Clip clip : soundClipMap.values()) {
                FloatControl c = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                c.setValue(calcGain(c, value));
            }
            masterGain = value;
        }
    }

    private float calcGain(FloatControl c, int value) {
        return (float) c.getMinimum() + 50.0f + ((c.getMaximum() - c.getMinimum() - 50.0f) * ((float) value / 100.0f));
    }
}
