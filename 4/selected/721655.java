package keybored.io;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.MidiUnavailableException;

/**
 * Provides a set of methods for interacting with a single midi channel.
 */
public class MidiController {

    /**
	 * The minimum value for velocity.
	 */
    public static final int MIN_VELOCITY = 0;

    /**
	 * The maximum value for velocity.
	 */
    public static final int MAX_VELOCITY = 100;

    private static final int INITIAL_VELOCITY = 98;

    private static final String ERROR_NOCHANNEL = "Could not find an available synth channel.";

    private static MidiController instance = null;

    /**
	 * @return A singleton instance of a MidiController.
	 * 
	 * @throws MidiSubsystemException If the constructor cannot locate or allocate adequate devices or resources.
	 */
    public static MidiController getInstance() throws MidiSubsystemException {
        synchronized (MidiController.class) {
            if (instance == null) {
                synchronized (MidiController.class) {
                    instance = new MidiController();
                }
            }
        }
        return instance;
    }

    /**
	 * The current instrument this controller is playing.
	 */
    private Instrument instrument;

    /**
	 * The channel this controller is using.
	 */
    private MidiChannel channel;

    /**
	 * The underlying synthesizer.
	 */
    private Synthesizer synth;

    /**
	 * The velocity values the keys will use.
	 */
    private int velocity;

    /**
	 * Initializes a midi controller for a single channel.
	 */
    private MidiController() throws MidiSubsystemException {
        try {
            this.synth = MidiSystem.getSynthesizer();
            this.synth.open();
        } catch (MidiUnavailableException e) {
            throw new MidiSubsystemException(e.getMessage());
        }
        MidiChannel[] channels = this.synth.getChannels();
        this.channel = null;
        for (int i = 0; i < channels.length && this.channel == null; i++) {
            if (channels[i] != null) {
                this.channel = channels[i];
            }
        }
        if (channel == null) {
            throw new MidiSubsystemException(ERROR_NOCHANNEL);
        }
        this.instrument = this.getInstrumentIndex(0);
        this.setInstrument(instrument);
        this.setVelocity(INITIAL_VELOCITY);
    }

    /**
	 * Turns a note on.
	 * 
	 * @param note The index of the midi note to be played.
	 */
    public void noteOn(int note) {
        this.channel.noteOn(note, this.velocity);
    }

    /**
	 * Turns a note off.
	 * 
	 * @param note The index of the midi note to be turned off.
	 */
    public void noteOff(int note) {
        this.channel.noteOff(note);
    }

    /**
	 * Turns all notes off.
	 */
    public void allNotesOff() {
        this.channel.allNotesOff();
    }

    /**
	 * @return An array of instruments that can be used.
	 */
    public Instrument[] getInstruments() {
        return synth.getAvailableInstruments();
    }

    /**
	 * Gets an instrument at a specific index in the list.
	 * 
	 * @param index The index from which to get the instrument.
	 * 
	 * @return The instrument at that index.
	 * 
	 * @see getInstruments()
	 */
    public Instrument getInstrumentIndex(int index) {
        return this.getInstruments()[index];
    }

    /**
	 * @return The instrument currently being used.
	 */
    public Instrument getInstrument() {
        return this.instrument;
    }

    /**
	 * Switches the instrument that is being used.
	 * 
	 * @param instrument The new instrument to use.
	 */
    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
        this.channel.programChange(this.instrument.getPatch().getBank(), this.instrument.getPatch().getProgram());
    }

    /**
	 * Sets the velocity of proceeding key presses.
	 * 
	 * @param velocity The value of the velocity (0 to 100).
	 */
    public void setVelocity(int velocity) {
        this.velocity = velocity;
    }

    /**
	 * @return The velocity this channel is currently set to use.
	 */
    public int getVelocity() {
        return this.velocity;
    }
}
