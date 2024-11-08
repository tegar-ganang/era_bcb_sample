package music;

import static engine.Engine.FRAME_TIME;
import game.staffobjects.Note;
import game.staffobjects.Rest;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

/**
 * The class MusicGenerator provides a MIDI interface and the necessary methods
 * to produce sound given a specific Note object
 */
public class MusicGenerator {

    private static MusicGenerator mg;

    private static final int QUARTER_DURATION = 30;

    private static final int DURATION_MULTIPLIER = 10;

    private boolean noteIsOn = false;

    private Note currentNote = null;

    private double currentDuration = 0;

    Synthesizer synth;

    Receiver receiver;

    MidiChannel midiChannel;

    Soundbank soundbank = null;

    /**
	 * Constructor of MusicGenerator. 
	 * Obtains Synthesizer of the MidiSystem installed on the system.
	 * Opens the Synthesizer and provides a MidiChannel for it.
	 */
    private MusicGenerator() {
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            midiChannel = synth.getChannels()[0];
            receiver = synth.getReceiver();
        } catch (MidiUnavailableException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
	 * Plays a Note object
	 */
    public void PlayNote(Note note) {
        if (note == null) {
            return;
        }
        currentNote = note;
        currentDuration = currentNote.getDuration() * QUARTER_DURATION * DURATION_MULTIPLIER;
        noteIsOn = true;
        if (midiChannel != null) {
            midiChannel.noteOn(note.getPitch(), 80);
        }
    }

    /**
	 * Turns off any sound
	 */
    public void releaseNote() {
        if (midiChannel != null) {
            midiChannel.allNotesOff();
        }
        currentNote = null;
        noteIsOn = false;
    }

    /**
	 * Creates the singleton MusicGenerator when it has not been created yet.
	 * 
	 * @return the singleton MusicGenerator
	 */
    public static synchronized MusicGenerator getInstance() {
        if (mg == null) {
            mg = new MusicGenerator();
        }
        return mg;
    }

    /**
	 * Updates the MusicGenerator 
	 */
    public void update(boolean menuActive) {
        double playSpeed = menuActive ? 1.0 : 0.4;
        if ((noteIsOn != false) && (currentDuration > 0)) {
            currentDuration = currentDuration - playSpeed * FRAME_TIME;
        }
        if (currentDuration <= 0) {
            releaseNote();
        }
    }

    public boolean isNoteOn() {
        return this.noteIsOn;
    }

    public void waitRest(Rest rest) {
        currentDuration = rest.getDuration() * QUARTER_DURATION * DURATION_MULTIPLIER;
        noteIsOn = true;
    }
}
