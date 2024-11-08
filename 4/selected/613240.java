package world.sound.tunes;

import java.util.ArrayList;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;

/** Represents/initializes the MIDI synthesizer and manages the
 *    channels/instruments in the MIDI program.  Based in part on a
 *    class originally designed by Viera K. Proulx. */
public class MusicBox implements SoundConstants {

    /** MIDI synthesizer that plays notes */
    private Synthesizer synth;

    /** MIDI channels that are currently assigned to the synthesizer */
    private MidiChannel[] channels;

    /** Is this MusicBox ready to play notes? (initialized correctly) */
    private boolean READY = false;

    /** The default constructor just initializes the MIDI synthesizer
     *     and sets the default program for the instruments. */
    public MusicBox() {
        initMusic();
        initChannels();
    }

    /** The MIDI synthesizer can also be initialized with a list of instrument
     *     numbers rather than setting the default program. */
    public MusicBox(int... instruments) {
        initMusic();
        initChannels(instruments);
    }

    /** Initialize this MusicBox (instruments/program/channels) */
    protected void initMusic() {
        try {
            this.synth = MidiSystem.getSynthesizer();
            this.synth.open();
            this.synth.loadAllInstruments(this.synth.getDefaultSoundbank());
            this.channels = this.synth.getChannels();
            if (this.channels != null) {
                this.initChannels();
                this.READY = true;
            }
        } catch (javax.sound.midi.MidiUnavailableException e) {
            System.err.println("MidiUnavailableException: " + e.getMessage());
            this.READY = false;
        } catch (NullPointerException e) {
            System.err.println("Midi Initialization Error: " + e.getMessage());
            this.READY = false;
        }
    }

    /** Initialize the program to the default set of instruments defined in 
     *    {@link world.sound.tunes.SoundConstants}. */
    public void initChannels() {
        this.initChannels(SoundConstants.INSTRUMENTS);
    }

    /** Initialize the MIDI channels to the given set of instruments.  You
     *   may give up to 16 instrument numbers (as instrument numbers defined in
     *   {@link world.sound.tunes.SoundConstants}) to assign to the channels. */
    public void initChannels(int... instruments) {
        for (int i = 0; i < this.channels.length && i < instruments.length; i++) {
            this.channels[i].programChange(instruments[i] - 1);
        }
    }

    /** Produce the instrument currently assigned to the given channel. */
    public int getProgram(int channel) {
        return this.channels[channel].getProgram();
    }

    /** Play all tunes in the given list of <code>Tune</code>s */
    public void playOn(ArrayList<Tune> tunes) {
        for (int i = 0; i < tunes.size(); i++) this.playTune(tunes.get(i));
    }

    /** Play the given tune on the channel assigned to it. */
    public void playTune(Tune tune) {
        for (int i = 0; i < tune.chord.notes.size(); i++) {
            Note n = tune.chord.notes.get(i);
            if (!n.isSilent()) {
                this.channels[tune.channel].noteOn(n.getPitch(), n.getVelocity());
                n.nextBeat();
            }
        }
    }

    /** Stop playing all tunes in the given list of <code>Tune</code>s. */
    public void playOff(ArrayList<Tune> tunes) {
        for (int i = 0; i < tunes.size(); i++) this.stopTune(tunes.get(i));
    }

    /** Stop playing the given tune on the channel assigned to it. */
    public void stopTune(Tune tune) {
        for (int i = 0; i < tune.chord.notes.size(); i++) {
            Note n = tune.chord.notes.get(i);
            this.channels[tune.channel].noteOff(n.getPitch(), n.getVelocity());
        }
    }

    /** Has this music box been initialized? */
    public boolean isReady() {
        return READY;
    }
}
