package wotlas.libs.sound;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Properties;
import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.Clip;
import wotlas.utils.Debug;
import wotlas.utils.Tools;

/** A Music Player that reads Midi musics files via the JAVA Sound API.
 *
 * @author Aldiss
 */
public class JavaMidiMusicPlayer implements MusicPlayer, MetaEventListener, ControllerEventListener {

    /** Our Resource Locator
     */
    private MusicResourceLocator resourceLocator;

    /** Tells if a sound device is present
     */
    private boolean noMusicDevice;

    /** No music option
     */
    private boolean noMusicState;

    /** Current Music Volume
     */
    private short musicVolume;

    /** Midi Sequencer.
     */
    private Sequencer sequencer;

    /** Midi Synthesizer.
     */
    private Synthesizer synthesizer;

    /** Midi channels.
     */
    private MidiChannel channels[];

    /** Current Music.
     */
    private BufferedInputStream currentMusic;

    /** Current Music playing. Null if none...
     */
    private String currentMusicName;

    /** Lock to avoid events collision
     */
    private byte lockMeta[] = new byte[0];

    /** Constructor.
     */
    public JavaMidiMusicPlayer() {
    }

    /** To init the music player. The resource locator can be used to get a stream on
     *  a music file.
     * @param props properties for init.
     * @param resourceLocator to locate music resources.
     */
    public void init(Properties props, MusicResourceLocator resourceLocator) {
        this.resourceLocator = resourceLocator;
        this.musicVolume = 100;
        this.noMusicState = false;
        this.noMusicDevice = false;
        openDevice();
    }

    /** Closes this music player.
     */
    public void close() {
        if (this.sequencer != null) this.sequencer.close();
    }

    /** To play a music.
     * @param musicName music file name in the music database.
     *        we'll search the file via the resourceLocator.
     */
    public void playMusic(String musicName) {
        if (this.noMusicDevice || this.noMusicState || this.sequencer == null) return;
        synchronized (this.lockMeta) {
            if (this.currentMusicName != null && this.currentMusicName.equals(musicName)) return; else this.currentMusicName = musicName;
            if (this.currentMusic != null) {
                this.sequencer.stop();
                Tools.waitTime(100);
            }
            if (!loadMidiMusic(this.resourceLocator.getMusicStream(musicName))) {
                Debug.signal(Debug.ERROR, this, "Failed to load music " + musicName);
                return;
            }
            setMusicVolume(getMusicVolume());
            this.sequencer.start();
        }
    }

    /** Stops the current music.
     */
    public void stopMusic() {
        if (this.noMusicDevice || this.sequencer == null) return;
        synchronized (this.lockMeta) {
            this.sequencer.stop();
            this.currentMusicName = null;
            this.currentMusic = null;
        }
    }

    /** Resume the current music or restarts the current music.
     */
    public void resumeMusic() {
        if (this.noMusicDevice || this.noMusicState || this.sequencer == null || this.currentMusic == null) return;
        synchronized (this.lockMeta) {
            this.sequencer.start();
        }
    }

    /** To get the music volume in [0, 100].
     *  @return the music volume.
     */
    public short getMusicVolume() {
        return this.musicVolume;
    }

    /** To set the music volume.
     * @param volume new volume in [0,100]
     */
    public void setMusicVolume(short musicVolume) {
        if (musicVolume > 100) musicVolume = 100;
        if (musicVolume < 0) musicVolume = 0;
        this.musicVolume = musicVolume;
        synchronized (this.lockMeta) {
            if (this.channels == null) return;
            int value = (int) (((musicVolume) / 100.0) * 127.0);
            if (value < 0) value = 0;
            if (value > 127) value = 127;
            for (int i = 0; i < this.channels.length; i++) this.channels[i].controlChange(7, value);
            this.noMusicState = (value == 0);
        }
    }

    /** Tells if we want the player to play music or just ignore music 'play' requests.
     * @return true if we must ignore music play requests
     */
    public boolean getNoMusicState() {
        return this.noMusicState;
    }

    /** To set/unset the "No Music" state option.
     * @param noMusicState true if requests to play music must be ignored, false to play music
     *        when asked to.
     */
    public void setNoMusicState(boolean noMusicState) {
        this.noMusicState = noMusicState;
        synchronized (this.lockMeta) {
            if (noMusicState) stopMusic(); else resumeMusic();
        }
    }

    /** To get the name of this music player.
     */
    public String getMusicPlayerName() {
        return "Java Sound Midi Player";
    }

    /** Opens the virtual Midi sound device.
     */
    protected void openDevice() {
        try {
            this.sequencer = MidiSystem.getSequencer();
            if (this.sequencer == null) {
                Debug.signal(Debug.WARNING, null, "no valid MIDI sequencers");
                this.noMusicDevice = true;
                return;
            } else {
                if (this.sequencer instanceof Synthesizer) {
                    this.synthesizer = (Synthesizer) this.sequencer;
                    this.channels = this.synthesizer.getChannels();
                }
                this.sequencer.open();
                this.sequencer.addMetaEventListener(this);
                int[] controllers = { 7 };
                this.sequencer.addControllerEventListener(this, controllers);
            }
            this.noMusicDevice = false;
        } catch (Exception ex) {
            Debug.signal(Debug.ERROR, this, "Failed to open Sound Device..." + ex);
            this.noMusicDevice = true;
        }
    }

    /** To load a Midi Music
     * @param musicStream music stream to use to load the music file
     * @return true if successful, false if loading failed.
     */
    protected boolean loadMidiMusic(InputStream musicStream) {
        if (musicStream == null) return false;
        Clip clip = null;
        this.currentMusic = new BufferedInputStream(musicStream, 4096);
        try {
            this.sequencer.setSequence(this.currentMusic);
        } catch (InvalidMidiDataException imde) {
            Debug.signal(Debug.ERROR, this, "Unsupported audio file : " + imde);
            return false;
        } catch (Exception ex) {
            Debug.signal(Debug.ERROR, this, "Failed to read music : " + ex);
            return false;
        }
        return true;
    }

    /** Midi Events intercepted. We use it for automatic music loopback.
     */
    public void meta(MetaMessage message) {
        if (this.noMusicDevice || this.sequencer == null || this.noMusicState || this.currentMusic == null) return;
        if (message.getType() == 47) {
            if (this.currentMusic != null) {
                synchronized (this.lockMeta) {
                    this.sequencer.stop();
                    Tools.waitTime(100);
                    this.sequencer.start();
                }
            }
        }
    }

    /** Control Change Events intercepted. We use it for automatic adjust volume.
     */
    public void controlChange(ShortMessage message) {
        synchronized (this.lockMeta) {
            if (this.currentMusic == null) return;
            if (message.getCommand() != 176) return;
            this.channels[message.getChannel()].controlChange(7, (int) (((this.musicVolume) / 100.0) * 127.0));
        }
    }
}
