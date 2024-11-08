package jrackattack.midi;

import javax.sound.midi.InvalidMidiDataException;
import jonkoshare.util.VersionInformation;

/**
 * Chord message. Forms a MidiMessage to synchronously send
 * NOTE_ON-events to form a chord.
 * TODO: make use of MULTI_PRESS-Midi-status
 *
 * @author methke01
 */
@VersionInformation(lastChanged = "$LastChangedDate: 2009-07-25 05:59:33 -0400 (Sat, 25 Jul 2009) $", authors = { "Alexander Methke" }, revision = "$LastChangedRevision: 11 $", lastEditor = "$LastChangedBy: onkobu $", id = "$Id")
public class ChordMessage extends DurationMessage {

    /** Creates a new instance of ChordMessage */
    public ChordMessage(int[] pitches, int velo, int dur, int status) throws InvalidMidiDataException {
        this(pitches, -1, velo, dur, status);
    }

    public ChordMessage(int[] pitches, int channel, int velo, int dur, int status) throws InvalidMidiDataException {
        NoteMessage sm = new NoteMessage();
        if (channel == -1) {
            int idx = 0;
            for (int pitch : pitches) {
                sm.setMessage(status, pitch, velo);
                sm.setDuration(dur);
                switch(idx) {
                    case 0:
                        {
                            setNote1(sm);
                        }
                        break;
                    case 1:
                        {
                            setNote2(sm);
                        }
                        break;
                    case 2:
                        {
                            setNote3(sm);
                        }
                        break;
                    case 3:
                        {
                            setNote4(sm);
                        }
                        break;
                }
                idx++;
            }
        } else {
            int idx = 0;
            for (int pitch : pitches) {
                sm.setMessage(status, channel, pitch, velo);
                sm.setDuration(dur);
                switch(idx) {
                    case 0:
                        {
                            setNote1(sm);
                        }
                        break;
                    case 1:
                        {
                            setNote2(sm);
                        }
                        break;
                    case 2:
                        {
                            setNote3(sm);
                        }
                        break;
                    case 3:
                        {
                            setNote4(sm);
                        }
                        break;
                }
                idx++;
            }
        }
        setPitches(pitches);
        setVelocity(velo);
        setDuration(dur);
        setChannel(channel);
    }

    public ChordMessage(ChordMessage copy, int status) throws InvalidMidiDataException {
        this(copy.getPitches(), copy.getChannel(), copy.getVelocity(), copy.getDuration(), status);
    }

    /**
	 * Getter for property note1.
	 * @return Value of property note1.
	 */
    public javax.sound.midi.ShortMessage getNote1() {
        return this.note1;
    }

    /**
	 * Setter for property note1.
	 * @param note1 New value of property note1.
	 */
    public void setNote1(javax.sound.midi.ShortMessage note1) {
        this.note1 = note1;
    }

    /**
	 * Getter for property note2.
	 * @return Value of property note2.
	 */
    public javax.sound.midi.ShortMessage getNote2() {
        return this.note2;
    }

    /**
	 * Setter for property note2.
	 * @param note2 New value of property note2.
	 */
    public void setNote2(javax.sound.midi.ShortMessage note2) {
        this.note2 = note2;
    }

    /**
	 * Getter for property note3.
	 * @return Value of property note3.
	 */
    public javax.sound.midi.ShortMessage getNote3() {
        return this.note3;
    }

    /**
	 * Setter for property note3.
	 * @param note3 New value of property note3.
	 */
    public void setNote3(javax.sound.midi.ShortMessage note3) {
        this.note3 = note3;
    }

    /**
	 * Getter for property note4.
	 * @return Value of property note4.
	 */
    public javax.sound.midi.ShortMessage getNote4() {
        return this.note4;
    }

    /**
	 * Setter for property note4.
	 * @param note4 New value of property note4.
	 */
    public void setNote4(javax.sound.midi.ShortMessage note4) {
        this.note4 = note4;
    }

    /**
	 * Getter for property velocity.
	 * @return Value of property velocity.
	 */
    public int getVelocity() {
        return this.velocity;
    }

    /**
	 * Setter for property velocity.
	 * @param velocity New value of property velocity.
	 */
    public void setVelocity(int velocity) {
        this.velocity = velocity;
    }

    /**
	 * Getter for property channel.
	 * @return Value of property channel.
	 */
    public int getChannel() {
        return this.channel;
    }

    /**
	 * Setter for property channel.
	 * @param channel New value of property channel.
	 */
    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int[] getPitches() {
        return pitches;
    }

    public void setPitches(int[] pit) {
        pitches = pit;
    }

    /**
	 * Holds value of property note1.
	 */
    private javax.sound.midi.ShortMessage note1;

    /**
	 * Holds value of property note2.
	 */
    private javax.sound.midi.ShortMessage note2;

    /**
	 * Holds value of property note3.
	 */
    private javax.sound.midi.ShortMessage note3;

    /**
	 * Holds value of property note4.
	 */
    private javax.sound.midi.ShortMessage note4;

    /**
	 * Holds value of property velocity.
	 */
    private int velocity;

    /**
	 * Holds value of property channel.
	 */
    private int channel;

    private int[] pitches;
}
