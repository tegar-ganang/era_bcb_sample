package com.neuemusic.eartoner;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import com.neuemusic.eartoner.util.Logger;
import com.neuemusic.eartoner.util.NoteComparator;

/**
 * @author Tom Jensen
 *
 * Contains the notes for a given test.  So, if it is a single interval, it will contain two notes.
 * It is important to note that if notes put in have detuning on them (pitch bend wheel) that they
 * will either need to note overlap with any other notes or be on a different channel.
 * Channels are an important consideration also.  If there are specific sounds that are needed on
 * specific channels, use the addPatch() method for each instrument.
 */
public class NoteSet {

    public static final int MIN_VAL = 0;

    public static final int MAX_VAL = 1;

    private ArrayList<Note> notes = new ArrayList<Note>();

    private ArrayList<PatchChange> patches = new ArrayList<PatchChange>();

    private ArrayList<Integer> channels = new ArrayList<Integer>();

    /**
	 * Empty constructor
	 *
	 */
    public NoteSet() {
    }

    /**
	 * Attempts to add a note to the note set.  The note is added if it isn't identical to
	 * an already existing note @see Note#equals for more information on equality.
	 */
    public boolean addNote(Note newNote) {
        if (!notes.contains(newNote)) {
            notes.add(newNote);
            if (!channels.contains(new Integer(newNote.getChannel()))) {
                channels.add(newNote.getChannel());
            }
            return true;
        }
        return false;
    }

    public boolean addPatch(PatchChange newPatch) {
        if (!patches.contains(newPatch)) {
            patches.add(newPatch);
            return true;
        }
        return false;
    }

    /**
	 * Tells how many notes are currently in the NoteSet.
	 */
    public int numberOfNotes() {
        return notes.size();
    }

    /**
	 * returns a pair of values, the first being the lowest midi value and the second being the highest
	 * midi value in the set of notes.
	 */
    public int[] getRange() {
        int[] range = new int[2];
        range[MIN_VAL] = 0;
        range[MAX_VAL] = 0;
        for (int x = 0; x < notes.size(); x++) {
            Note note = (Note) notes.get(x);
            if (range[MIN_VAL] == 0 || note.getNoteValue() < range[MIN_VAL]) {
                range[MIN_VAL] = note.getNoteValue();
            }
            if (range[MAX_VAL] == 0 || note.getNoteValue() > range[MAX_VAL]) {
                range[MAX_VAL] = note.getNoteValue();
            }
        }
        return range;
    }

    /**
	 * Returns a set of all of the notes in their appropriate order 
	 * @see com.neuemusic.eartoner.util.NoteComparator
	 * 
	 */
    public Set<Note> getNotesInOrder() {
        TreeSet<Note> orderedNotes = new TreeSet<Note>(new NoteComparator());
        for (int x = 0; x < notes.size(); x++) {
            orderedNotes.add(notes.get(x));
        }
        return orderedNotes;
    }

    /**
	 * Generates a javax.sound.midi.Sequence object to be played of all of the notes in this set.
	 * Each individual note is responsible for its own timing, velocity, etc.
	 */
    public Sequence getAsSequence() {
        Sequence sequence = null;
        try {
            sequence = new Sequence(Sequence.PPQ, 30);
        } catch (InvalidMidiDataException e) {
            Logger.log(e);
        }
        if (sequence != null) {
            Track track = sequence.createTrack();
            MidiEvent[] patches = getPatchesAsMidiEvents();
            for (int x = 0; x < patches.length; x++) {
                track.add(patches[x]);
            }
            for (int x = 0; x < notes.size(); x++) {
                Note note = (Note) notes.get(x);
                MidiEvent[] events = note.getAsMidiEvents();
                for (int y = 0; y < events.length; y++) {
                    track.add(events[y]);
                }
            }
        }
        return sequence;
    }

    /**
	 * Gets all of the patch changes that have been setup for this set of notes.
	 * Makes sure there is a patch change for every channel that has note events
	 * on them, and if there isn't one already it uses the default.
	 * @return
	 */
    private MidiEvent[] getPatchesAsMidiEvents() {
        ArrayList<MidiEvent> events = new ArrayList<MidiEvent>();
        for (int x = 0; x < patches.size(); x++) {
            PatchChange chg = (PatchChange) patches.get(x);
            events.add(chg.getAsMidiEvent());
            channels.remove(new Integer(chg.getChannel()));
        }
        for (int x = 0; x < channels.size(); x++) {
            PatchChange chg = new PatchChange(Settings.getCurrentPatch(), ((Integer) channels.get(x)).intValue());
            events.add(chg.getAsMidiEvent());
        }
        MidiEvent[] evts = new MidiEvent[events.size()];
        events.toArray(evts);
        return evts;
    }

    /**
	 * Returns the requested note in this NoteSet.  The NoteSet remembers the notes in the order
	 * they were added.
	 */
    public Note getNote(int index) {
        return (Note) notes.get(index);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int x = 0; x < notes.size(); x++) {
            sb.append((x + 1)).append(notes.get(x) + "\n");
        }
        return sb.toString();
    }
}
