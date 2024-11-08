package com.neuemusic.eartoner;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import com.neuemusic.eartoner.util.Logger;

/**
 * @author Tom Jensen
 *
 * Contains information about a specific note, from which it is able to generate a MidiEvent,
 * when requested.
 */
public class Note extends BasicNote implements Cloneable {

    /**
	 * The MIDI value for the pitch bend wheel being at center (no detuning)
	 */
    public static final int PITCH_CENTER = 64;

    private long offset = 0;

    private long duration = 0;

    private int velocity = 0;

    private int channel = 0;

    public Note(int midiValue, long offset, long duration, int velocity) {
        this(midiValue, offset, duration, velocity, 0);
    }

    public Note(int midiValue, long offset, long duration, int velocity, int channel) {
        this(midiValue, offset, duration, velocity, channel, 0);
    }

    public Note(int midiValue, long offset, long duration, int velocity, int channel, int detune) {
        setNoteValue(midiValue);
        setOffset(offset);
        setDuration(duration);
        setVelocity(velocity);
        setChannel(channel);
        setDetune(detune);
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
        this.duration = duration;
    }

    /**
	 * @return Returns the offset.
	 */
    public long getOffset() {
        return offset;
    }

    /**
	 * @param offset The offset to set.
	 */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    /**
	 * @return Returns the velocity.
	 */
    public int getVelocity() {
        return velocity;
    }

    /**
	 * @param velocity The velocity to set.
	 */
    public void setVelocity(int velocity) {
        if (velocity > 127) {
            velocity = 127;
        }
        this.velocity = velocity;
    }

    /**
	 * @return Returns the channel.
	 */
    public int getChannel() {
        return channel;
    }

    /**
	 * @param channel The channel to set.
	 */
    public void setChannel(int channel) {
        if (channel > 15) {
            channel = 15;
        }
        this.channel = channel;
    }

    /**
	 * Checks to see if the object passed is 1) a Note object, and if so, 2) all of the internal
	 * values (noteValue, duration, offset, velocity, channel) are the same.
	 */
    public boolean equals(Object o) {
        if (o instanceof Note) {
            Note tmpNote = (Note) o;
            if (super.equals(tmpNote) && tmpNote.duration == duration && tmpNote.offset == offset && tmpNote.velocity == velocity && tmpNote.channel == channel) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Generates midi events that correspond to this note, two or four events, one
	 * to turn the note on, one to turn the note off.  It is four events if there is
	 * some detuning specified on the note, in which case a pitch bend message is
	 * sent on the note on and a pitch bend return message is sent on the note off. 
	 */
    public MidiEvent[] getAsMidiEvents() {
        MidiEvent[] events = null;
        if (getDetune() != 0) {
            events = new MidiEvent[4];
        } else {
            events = new MidiEvent[2];
        }
        ShortMessage onMsg = new ShortMessage();
        try {
            onMsg.setMessage(ShortMessage.NOTE_ON, channel, getNoteValue(), velocity);
        } catch (InvalidMidiDataException e) {
            Logger.log(e);
        }
        events[0] = new MidiEvent(onMsg, offset);
        ShortMessage offMsg = new ShortMessage();
        try {
            offMsg.setMessage(ShortMessage.NOTE_OFF, channel, getNoteValue(), velocity);
        } catch (InvalidMidiDataException e1) {
            Logger.log(e1);
        }
        events[1] = new MidiEvent(offMsg, offset + duration);
        if (getDetune() != 0) {
            events[2] = events[1];
            events[1] = events[0];
            ShortMessage detuneMsg = new ShortMessage();
            try {
                detuneMsg.setMessage(ShortMessage.PITCH_BEND, channel, 0, PITCH_CENTER + getDetune());
            } catch (InvalidMidiDataException e) {
                Logger.log(e);
            }
            events[0] = new MidiEvent(detuneMsg, offset);
            ShortMessage returnMsg = new ShortMessage();
            try {
                returnMsg.setMessage(ShortMessage.PITCH_BEND, 0, PITCH_CENTER);
            } catch (InvalidMidiDataException e) {
                Logger.log(e);
            }
            events[3] = new MidiEvent(returnMsg, offset + duration);
        }
        return events;
    }

    public String toString() {
        return "Note Object {" + "Value: " + getNoteValue() + ", " + "Offset: " + offset + ", " + "Duration: " + duration + ", " + "Velocity: " + velocity + ", " + "Channel: " + channel + ", " + "Detune: " + getDetune() + "}";
    }

    public Object clone() {
        Note note = new Note(getNoteValue(), getOffset(), getDuration(), getVelocity(), getChannel(), getDetune());
        return note;
    }
}
