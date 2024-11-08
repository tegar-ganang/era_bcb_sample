package decode;

import gui.VideoWindow;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IStreamCoder;

public class SharedFunctions {

    /**
	 * The audio line we'll output sound to.
	 * It'll be the default audio device on your system if available
	 */
    private static SourceDataLine mLine;

    private static VideoWindow mScreen;

    public SharedFunctions(SourceDataLine mLine, VideoWindow mScreen) {
        SharedFunctions.mLine = mLine;
        SharedFunctions.mScreen = mScreen;
    }

    /**
	 * Opens a Swing window on screen.
	 */
    protected static void openJavaVideo() {
        mScreen = new VideoWindow();
    }

    /**
	 * Forces the swing thread to terminate; I'm sure there is a right
	 * way to do this in swing, but this works too.
	 */
    protected static void closeJavaVideo() {
        System.exit(0);
    }

    protected static void playJavaSound(IAudioSamples aSamples) {
        byte[] rawBytes = aSamples.getData().getByteArray(0, aSamples.getSize());
        mLine.write(rawBytes, 0, aSamples.getSize());
    }

    protected static void closeJavaSound() {
        if (mLine != null) {
            mLine.drain();
            mLine.close();
            mLine = null;
        }
    }

    protected static void openJavaSound(IStreamCoder aAudioCoder) throws LineUnavailableException {
        AudioFormat audioFormat = new AudioFormat(aAudioCoder.getSampleRate(), (int) IAudioSamples.findSampleBitDepth(aAudioCoder.getSampleFormat()), aAudioCoder.getChannels(), true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        mLine = (SourceDataLine) AudioSystem.getLine(info);
        mLine.open(audioFormat);
        mLine.start();
    }
}
