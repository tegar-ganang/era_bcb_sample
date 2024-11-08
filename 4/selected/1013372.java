package org.easyway.sounds;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import org.easyway.utils.Utility;

/**
 * Play wave sounds
 */
public class WaveCore implements LineListener {

    /**
	 * the stream of audio
	 */
    private InputStream clipStream;

    /**
	 * the path of audio
	 */
    private String path;

    /**
	 * the audio clip
	 */
    private Clip clip;

    /**
	 * indicates if the clip is looping or not
	 */
    private boolean loop;

    /**
	 * the clip's volume
	 */
    private float volume;

    /**
	 * the wave player thread
	 */
    private WaveThread player;

    /**
	 * indicates if is avaiable the wave audio
	 */
    private static boolean available = true;

    /**
	 * indicates if is supported the volume
	 */
    private static boolean volumeSupported;

    static {
        Thread thread = new Thread() {

            public final void run() {
                try {
                    URL sample = WaveCore.class.getResource("test.wav");
                    AudioInputStream ain = AudioSystem.getAudioInputStream(sample);
                    AudioFormat format = ain.getFormat();
                    DataLine.Info info = new DataLine.Info(Clip.class, ain.getFormat(), ((int) ain.getFrameLength() * format.getFrameSize()));
                    Clip clip = (Clip) AudioSystem.getLine(info);
                    clip.open(ain);
                    Control.Type volType = FloatControl.Type.VOLUME;
                    volumeSupported = clip.isControlSupported(volType);
                    clip.drain();
                    clip.close();
                } catch (Exception e) {
                    Utility.error("Your computer doesn't support Wave audio", "WaveTest");
                    available = false;
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    /**
	 * Creates new wave audio renderer.
	 */
    public WaveCore(String path) {
        {
            if (path.startsWith("/")) path = path.substring(1);
            int index;
            while ((index = path.indexOf("\\")) != -1) path = path.substring(0, index) + '/' + path.substring(index + 1);
            this.path = path;
        }
        assert clipStream != null;
        volume = 1.0f;
    }

    protected WaveCore(WaveCore wave) {
        this.path = wave.path;
        this.volume = wave.volume;
        this.loop = false;
        player = new WaveThread();
        player.setDaemon(false);
        player.start();
    }

    protected void openClipStream() {
        try {
            clipStream = Thread.currentThread().getContextClassLoader().getResource(path).openStream();
        } catch (IOException e) {
            Utility.error("EXCEPTION: Wave '" + path + "' was not found!", "WaveCore(String)", e);
        }
    }

    /**
	 * rewind the clip and then execute it
	 * 
	 * @throws LineUnavailableException
	 */
    public void replay() {
        if (clip != null && clip.isOpen()) {
            clip.removeLineListener(this);
            clip.flush();
            clip.drain();
            clip.stop();
            clip.setMicrosecondPosition(0);
            clip.setFramePosition(0);
            clip.addLineListener(this);
            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                clip.start();
            }
            System.out.println("RE");
            return;
        }
        System.out.println("NEW");
        play();
    }

    /**
	 * play a new instance of the sound
	 */
    public void play() {
        if (clip != null && clip.isOpen()) {
            new WaveCore(this);
            return;
        }
        player = new WaveThread();
        player.setDaemon(false);
        player.start();
    }

    public void stop() {
        if (clip == null) return;
        clip.stop();
        clip.setMicrosecondPosition(0);
        clip.removeLineListener(this);
        clip.close();
        clip = null;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean value) {
        if (loop == value) return;
        loop = value;
        if (clip != null && clip.isOpen()) {
            clip.stop();
            play();
        }
    }

    /**
	 * Notify when the sound is stopped externally.
	 */
    public void update(LineEvent e) {
        if (e.getType() == LineEvent.Type.STOP) {
            synchronized (clip) {
                clip.stop();
                clip.setMicrosecondPosition(0);
                clip.removeLineListener(this);
                clip.close();
                clip = null;
            }
        }
    }

    public void setVolume(float volume) {
        if (clip == null) {
            this.volume = volume;
            return;
        } else if (this.volume == volume) {
            return;
        }
        if (isVolumeSupported() == false) {
            Utility.error("Settng wave volume not supported", "WaveCore.setVolume(float)");
            return;
        }
        this.volume = volume;
        Control.Type volType = FloatControl.Type.VOLUME;
        FloatControl control = (FloatControl) clip.getControl(volType);
        control.setValue(volume);
    }

    public float getVolume() {
        return volume;
    }

    public boolean isVolumeSupported() {
        return volumeSupported;
    }

    public String getAudioFile() {
        return path;
    }

    public InputStream getAudioStream() {
        return clipStream;
    }

    public boolean isPlaying() {
        return clip != null && clip.isOpen();
    }

    public boolean isAvailable() {
        return available;
    }

    private class WaveThread extends Thread {

        public final void run() {
            if (clip != null && clip.isOpen()) return;
            try {
                openClipStream();
                AudioInputStream ain = AudioSystem.getAudioInputStream(clipStream);
                AudioFormat format = ain.getFormat();
                if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                    AudioFormat temp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                    ain = AudioSystem.getAudioInputStream(temp, ain);
                    format = temp;
                }
                DataLine.Info info = new DataLine.Info(Clip.class, ain.getFormat(), ((int) ain.getFrameLength() * format.getFrameSize()));
                clip = (Clip) AudioSystem.getLine(info);
                clip.addLineListener(WaveCore.this);
                clip.open(ain);
                if (loop) {
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                } else {
                    clip.start();
                }
                if (volume != 1.0f) {
                    volume = 1.0f;
                    setVolume(volume);
                }
            } catch (Exception e) {
                if (clip != null) {
                    clip = null;
                }
                Utility.error("Error playing the wav: '" + path + "'", "WaveCore.play()", e);
            }
        }
    }
}
