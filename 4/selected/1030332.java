package com.frinika.sequencer.model;

import java.util.Comparator;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import com.frinika.sequencer.FrinikaTrackWrapper;
import com.frinika.sequencer.gui.virtualkeyboard.VirtualKeyboard;

/**
 * 
 * @author Peter Johan Salomonsen
 *
 */
public class NoteEvent extends ChannelEvent {

    public static final Comparator<NoteEvent> noteComparator = new NoteComparator();

    public static final StartComparator startComparator = new StartComparator();

    public static final EndComparator endComparator = new EndComparator();

    private static final long serialVersionUID = 1L;

    transient MidiEvent startEvent;

    transient MidiEvent endEvent;

    int note;

    int velocity;

    long duration;

    boolean valid = false;

    /**
     * Constructor for creating a note event. For registering the new note event in the track use FrinikaTrackWrapper.add(MultiEvent evt)
     * @param track
     * @param startTick
     * @param note
     * @param velocity
     * @param channel
     * @param duration
     * @deprecated
     */
    public NoteEvent(FrinikaTrackWrapper track, long startTick, int note, int velocity, int channel, long duration) {
        super(track, startTick);
        this.note = note;
        this.velocity = velocity;
        this.channel = channel;
        this.duration = duration;
    }

    /**
     * 
     * @param group
     * @param startTick
     * @param note
     * @param velocity
     * @param channel
     * @param duration
     */
    public NoteEvent(MidiPart part, long startTick, int note, int velocity, int channel, long duration) {
        super(part, startTick);
        this.note = note;
        this.velocity = velocity;
        this.channel = channel;
        this.duration = duration;
    }

    /**
     * The process generating the NoteEvent should first supply it start event, 
     * and when ready the end event - to form a complete Note event
     * @param startEvent
     * 
     */
    NoteEvent(MidiPart part, MidiEvent startEvent) {
        super(part, startEvent.getTick());
        this.startEvent = startEvent;
        ShortMessage shm = (ShortMessage) startEvent.getMessage();
        note = shm.getData1();
        velocity = shm.getData2();
        channel = shm.getChannel();
        startTick = startEvent.getTick();
    }

    public void setEndEvent(MidiEvent endEvent) {
        this.endEvent = endEvent;
        duration = endEvent.getTick() - startTick;
        if (duration < 0) {
            System.out.println(" NEGATIVE LENGTH NOTE FIXED");
            duration = 0;
            endEvent.setTick(startEvent.getTick());
        }
        valid = true;
    }

    public long getEndTick() {
        return startTick + duration;
    }

    public int getNote() {
        return note;
    }

    public String getNoteName() {
        return VirtualKeyboard.getNoteString(note);
    }

    public void setNote(int note) {
        if (this.note == note) return;
        this.note = note;
    }

    public int getVelocity() {
        return velocity;
    }

    public void setVelocity(int velocity) {
        this.velocity = velocity;
    }

    /**
     * @return Returns the duration.
     */
    public long getDuration() {
        return duration;
    }

    /**
     * @param duration The duration to set.
     */
    public void setDuration(long duration) {
        if (duration < 0) {
            System.out.println(" Sorry but I won't make a note negative length ");
            duration = 0;
            return;
        }
        this.duration = duration;
    }

    @SuppressWarnings("deprecation")
    void commitRemoveImpl() {
        if (zombie) {
            try {
                throw new Exception(" Attempt to remove a zombie note from the track ");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (startEvent == null) {
            try {
                throw new Exception(" Attempt to remove a note with null start from the track ");
            } catch (Exception e) {
                System.out.println(" You can ignore this exception  . . . . ");
                e.printStackTrace();
            }
        } else {
            getTrack().remove(startEvent);
        }
        if (endEvent == null) {
            try {
                throw new Exception(" Attempt to remove a note with null end from the track ");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            getTrack().remove(endEvent);
        }
        zombie = true;
    }

    @SuppressWarnings("deprecation")
    public void commitAddImpl() {
        if (part.lane == null) return;
        try {
            ShortMessage shm = new ShortMessage();
            shm.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
            startEvent = new MidiEvent(shm, startTick);
            getTrack().add(startEvent);
            shm = new ShortMessage();
            shm.setMessage(ShortMessage.NOTE_ON, channel, note, 0);
            endEvent = new MidiEvent(shm, startTick + duration);
            getTrack().add(endEvent);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
        zombie = false;
    }

    public void restoreFromClone(EditHistoryRecordable object) {
        NoteEvent note = (NoteEvent) object;
        this.part = note.part;
        this.startTick = note.startTick;
        this.velocity = note.velocity;
        this.channel = note.channel;
        this.duration = note.duration;
        this.note = note.note;
    }

    public void setValue(int val) {
        this.velocity = val;
    }

    public int getValue() {
        return velocity;
    }

    public long rightTickForMove() {
        return startTick + duration;
    }

    public boolean isDrumHit() {
        return duration == 0;
    }

    /**
	 * PJL HACK NOT DO NOT USE
	 *
	 */
    public void validate() {
        if (endEvent == null) {
            System.err.println("Fixing null end event");
            ShortMessage shm = new ShortMessage();
            try {
                shm.setMessage(ShortMessage.NOTE_ON, channel, note, 0);
            } catch (InvalidMidiDataException e) {
                e.printStackTrace();
            }
            duration = 0;
            endEvent = new MidiEvent(shm, startTick);
        }
    }

    /**
	 * 
	 * Utility comparators for sorted sets. Notes are never equaly unless they are the same object.
	 * 
	 * @author pjl
	 *
	 */
    public static class StartComparator implements Comparator<NoteEvent> {

        public int compare(NoteEvent o1, NoteEvent o2) {
            if (o1.startTick > o2.startTick) return 1;
            if (o1.startTick < o2.startTick) return -1;
            if (o1.startTick + o1.duration > o2.startTick + o2.duration) return 1;
            if (o1.startTick + o1.duration < o2.startTick + o2.duration) return -1;
            if (o1.note > o2.note) return 1;
            if (o1.note < o2.note) return -1;
            return o1.compareTo(o2);
        }
    }

    public static class EndComparator implements Comparator<NoteEvent> {

        public int compare(NoteEvent o1, NoteEvent o2) {
            if (o1.startTick + o1.duration > o2.startTick + o2.duration) return 1;
            if (o1.startTick + o1.duration < o2.startTick + o2.duration) return -1;
            if (o1.startTick > o2.startTick) return 1;
            if (o1.startTick < o2.startTick) return -1;
            if (o1.note > o2.note) return 1;
            if (o1.note < o2.note) return -1;
            return o1.compareTo(o2);
        }
    }

    public static class NoteComparator implements Comparator<NoteEvent> {

        public int compare(NoteEvent o1, NoteEvent o2) {
            if (o1.note > o2.note) return 1;
            if (o1.note < o2.note) return -1;
            if (o1.startTick > o2.startTick) return 1;
            if (o1.startTick < o2.startTick) return -1;
            if (o1.startTick + o1.duration > o2.startTick + o2.duration) return 1;
            if (o1.startTick + o1.duration < o2.startTick + o2.duration) return -1;
            return o1.compareTo(o2);
        }
    }
}
