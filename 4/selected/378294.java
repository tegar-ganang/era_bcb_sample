package wotlas.libs.sound;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Properties;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import wotlas.utils.Debug;

/** A Sound Player for reading short WAV, AU, etc. sound files via the JAVA Sound API.
 *
 * @author Aldiss
 */
public class JavaSoundPlayer implements SoundPlayer {

    /** Our Resource Locator
     */
    private SoundResourceLocator resourceLocator;

    /** No sound option
     */
    private boolean noSoundState;

    /** Current Sound Volume
     */
    private short soundVolume;

    /** Constructor.
     */
    public JavaSoundPlayer() {
    }

    /** To init the sound player. The resource locator can be used to get a stream on
     *  a sound file.
     * @param props properties for init.
     * @param resourceLocator to locate sound resources.
     */
    public void init(Properties props, SoundResourceLocator resourceLocator) {
        this.resourceLocator = resourceLocator;
        this.noSoundState = false;
        this.soundVolume = 100;
    }

    /** Closes this sound player. Does Nothing here.
     */
    public void close() {
    }

    /** To play a sound.
     * @param soundName sound file name in the sound database.
     *        we'll search the file via the resourceLocator.
     */
    public void playSound(String soundName) {
        if (this.noSoundState || soundName == null) return;
        Clip sound = loadSound(this.resourceLocator.getSoundStream(soundName));
        if (sound == null) return;
        setGain(sound, this.soundVolume);
        sound.setFramePosition(0);
        sound.start();
    }

    /** To get the sound volume in [0, 100].
     * @return volume new volume in [0,100]
     */
    public short getSoundVolume() {
        return this.soundVolume;
    }

    /** To set the sound volume ( wave sounds ) in the [0,100] range.
     * @return volume new volume in [0,100]
     */
    public void setSoundVolume(short soundVolume) {
        this.soundVolume = soundVolume;
    }

    /** Tells if we want the player to play sounds or just ignore sounds 'play' requests.
     * @return true if we must ignore sound play requests
     */
    public boolean getNoSoundState() {
        return this.noSoundState;
    }

    /** To set/unset the "No Sound" option.
     * @param noSoundState true if requests to play sounds must be ignored, false to play sounds
     *        when asked to.
     */
    public void setNoSoundState(boolean noSoundState) {
        this.noSoundState = noSoundState;
    }

    /** To get the name of this sound player.
     */
    public String getSoundPlayerName() {
        return "Java Sound Player";
    }

    /** To load a Sound Clip. We only read Wave PCM & Wave ALAW/ULAW.
     * @param soudStream stream from the sound clip.
     * @return sound clip
     */
    protected Clip loadSound(InputStream soundStream) {
        AudioInputStream stream = null;
        Clip clip = null;
        if (soundStream == null) return null;
        try {
            stream = AudioSystem.getAudioInputStream(new BufferedInputStream(soundStream, 2048));
        } catch (Exception ex) {
            Debug.signal(Debug.ERROR, this, "Failed to load sound : " + ex);
            return null;
        }
        try {
            AudioFormat format = stream.getFormat();
            if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                AudioFormat tmp = new javax.sound.sampled.AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                stream = AudioSystem.getAudioInputStream(tmp, stream);
                format = tmp;
            }
            DataLine.Info info = new DataLine.Info(javax.sound.sampled.Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);
        } catch (Exception ex) {
            Debug.signal(Debug.ERROR, this, "Failed to read sound : " + ex);
            return null;
        }
        return clip;
    }

    /** To set the gain for sounds. The volume range is [0..100].
     * @param clip clip to adjust.
     * @param volume volume to set.
     */
    protected void setGain(Clip clip, int soundVolume) {
        double value = soundVolume / 100.0;
        try {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(value == 0.0 ? 0.0001 : value) / Math.log(10.0) * 20.0);
            gainControl.setValue(dB);
        } catch (Exception ex) {
            Debug.signal(Debug.WARNING, this, "Failed to change sound volume :" + ex);
        }
    }
}
