package jmetest.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import com.jme.app.SimpleGame;
import com.jme.scene.Text;
import com.jmex.audio.AudioSystem;
import com.jmex.audio.AudioTrack;
import com.jmex.audio.openal.OpenALStreamedAudioPlayer;
import com.jmex.audio.player.StreamedAudioPlayer;
import com.jmex.audio.stream.AudioInputStream;

/**
 * Demonstration of subclassing the audio system to provide your own data.
 * (Requires OpenAL)
 * 
 * @author toxcwav
 */
public class TestDynamicJMESound extends SimpleGame {

    private AudioSystem audio;

    private Text label;

    public static void main(String[] args) {
        TestDynamicJMESound app = new TestDynamicJMESound();
        app.setConfigShowMode(ConfigShowMode.AlwaysShow);
        app.start();
    }

    @Override()
    protected void simpleInitGame() {
        audio = AudioSystem.getSystem();
        audio.getEar().trackOrientation(cam);
        audio.getEar().trackPosition(cam);
        AudioTrack music = getDynamic();
        audio.getEnvironmentalPool().addTrack(music);
        label = Text.createDefaultTextLabel("listen", "Static noise should be playing.  Hit esc to quit.");
        label.updateRenderState();
        label.setLocalTranslation((display.getWidth() - label.getWidth()) / 2f, (display.getHeight() - label.getHeight()) / 2f, 0);
    }

    private AudioTrack getDynamic() {
        return new DynamicAudioTrack();
    }

    @Override()
    protected void simpleUpdate() {
        audio.update();
    }

    @Override
    protected void simpleRender() {
        super.simpleRender();
        label.draw(display.getRenderer());
    }

    @Override()
    protected void cleanup() {
        audio.cleanup();
    }

    class DynamicAudioStream extends AudioInputStream {

        private int samplesPerSecond = 44100;

        public DynamicAudioStream() throws IOException {
            super(DynamicAudioStream.class.getClassLoader().getResource("."), 10.0f);
        }

        @Override
        public int available() {
            return samplesPerSecond;
        }

        @Override
        public int getBitRate() {
            return getChannelCount() * getDepth() / 8 * samplesPerSecond;
        }

        @Override
        public int getChannelCount() {
            return 1;
        }

        @Override
        public int getDepth() {
            return 16;
        }

        @Override
        public AudioInputStream makeNew() throws IOException {
            return this;
        }

        /** synthesizes the next audio buffer */
        @Override
        public int read(ByteBuffer b, int offset, int length) throws IOException {
            for (int i = offset; i < length; i++) {
                b.put(i, (byte) (Math.random() * 200));
            }
            return length;
        }
    }

    class DynamicAudioTrack extends AudioTrack {

        private StreamedAudioPlayer stream;

        public DynamicAudioTrack() {
            super(DynamicAudioStream.class.getClassLoader().getResource("."), true);
            try {
                this.stream = new OpenALStreamedAudioPlayer(new DynamicAudioStream(), this);
                stream.init();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            setPlayer(stream);
            setEnabled(true);
            setType(TrackType.ENVIRONMENT);
            setRelative(true);
            setTargetVolume(0.9F);
        }
    }
}
