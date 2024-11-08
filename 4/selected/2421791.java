package free.util.audio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import free.util.BlockingQueue;
import free.util.IOUtilities;
import free.util.PlatformUtils;

/**
 * An abstract AudioPlayer implementation which uses the javax.sound.sampled API
 * to play sounds. This API is only available since JDK1.3.
 * Runnable.run is left to be implemented by the concrete subclass. Its function
 * is to loop forever, obtaining <code>free.util.AudiClip</code> instances from
 * the <code>clipQueue</code> and playing them. 
 */
public abstract class JavaxSampledAudioPlayer implements Runnable, AudioPlayer {

    /**
   * The current thread playing the sound.
   */
    private Thread playerThread = null;

    /**
   * A BlockingQueue of AudioClips queued for playing.
   */
    protected final BlockingQueue clipQueue = new BlockingQueue();

    /**
   * Returns whether we're running under Java 1.3 or later.
   */
    public boolean isSupported() {
        return PlatformUtils.isJavaBetterThan("1.3");
    }

    /**
   * Plays the given AudioClip.
   */
    public synchronized void play(AudioClip clip) throws java.io.IOException {
        if (playerThread == null) {
            playerThread = new Thread(this, "JavaxSampledAudioPlayer");
            playerThread.setDaemon(true);
            playerThread.setPriority(Thread.MAX_PRIORITY);
            playerThread.start();
        }
        clipQueue.push(clip);
    }

    /**
   * Finds and returns the AudioFormat appropriate for playing the specified
   * audio data.
   */
    protected static AudioFormat getFormatForPlaying(byte[] audioData) throws UnsupportedAudioFileException, IOException {
        AudioFormat format = AudioSystem.getAudioFileFormat(new ByteArrayInputStream(audioData)).getFormat();
        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true); else return format;
    }

    /**
   * Converts the specified audio data to the specified format. 
   */
    protected static byte[] convertAudioData(byte[] audioData, AudioFormat format) throws UnsupportedAudioFileException, IOException {
        AudioInputStream stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioData));
        if (format.matches(stream.getFormat())) return audioData;
        stream = AudioSystem.getAudioInputStream(format, stream);
        return IOUtilities.readToEnd(stream);
    }
}
