package vivace.helper;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import vivace.exception.MidiMessageNotFoundException;
import vivace.model.*;

/** 
 * Helper function for project actions
 * 	- filtering functionality for MIDI data
 *  - printing of MIDI events
 */
public class ProjectHelper {

    /**
	 * Returns the name and octave of the note with the specified number
	 * Specification found at http://www.harmony-central.com/MIDI/Doc/table2.html
	 */
    public static String getNoteName(int note, boolean displayOctave) {
        String s = "";
        if (note < 0 || note > 127) {
            return "N/A";
        }
        switch(note % 12) {
            case 0:
                s = "C";
                break;
            case 1:
                s = "C#";
                break;
            case 2:
                s = "D";
                break;
            case 3:
                s = "D#";
                break;
            case 4:
                s = "E";
                break;
            case 5:
                s = "F";
                break;
            case 6:
                s = "F#";
                break;
            case 7:
                s = "G";
                break;
            case 8:
                s = "G#";
                break;
            case 9:
                s = "A";
                break;
            case 10:
                s = "A#";
                break;
            case 11:
                s = "B";
                break;
        }
        int octave = (int) Math.ceil(note / 12) - 1;
        return s + (displayOctave ? " " + octave : "");
    }

    /**
	 * Converts an unsigned byte to a integer value
	 */
    public static int byteToInt(byte b) {
        return b & 0xFF;
    }

    /**
	 * Converts an unsigned byte to a hexadecimal value
	 */
    public static String byteToHex(byte b) {
        int i = b & 0xFF;
        return Integer.toHexString(i);
    }

    /**
	 * Returns the first meta message event of the given type in the given track
	 * @param track
	 * @param type
	 * @throws MidiMessageNotFoundException if no matching events were found
	 * @return
	 */
    public static MidiEvent filterFirstMetaMessage(Track track, int type) throws MidiMessageNotFoundException {
        Vector<MidiEvent> metaMessageEvents = filterMetaMessages(track, type);
        if (metaMessageEvents.size() > 0) {
            return metaMessageEvents.firstElement();
        } else {
            throw new MidiMessageNotFoundException();
        }
    }

    /**
	 * Returns the first short message in the given track of the given command type
	 * or null if no matching short messages were found.
	 * @param track
	 * @param command
	 * @return
	 */
    public static ShortMessage filterFirstShortMessage(Track track, int command) throws MidiMessageNotFoundException {
        Vector<MidiEvent> shortMessageEvents = filterShortMessages(track, command);
        if (shortMessageEvents.size() > 0) {
            return (ShortMessage) shortMessageEvents.firstElement().getMessage();
        } else {
            throw new MidiMessageNotFoundException();
        }
    }

    /**
	 * Returns the first meta message of the track
	 * @param track
	 * @return
	 */
    public static MetaMessage firstMetaMessage(Track track) throws MidiMessageNotFoundException {
        Vector<MidiEvent> metaMessageEvents = allMetaMessages(track);
        if (metaMessageEvents.size() > 0) {
            return (MetaMessage) metaMessageEvents.firstElement().getMessage();
        } else {
            throw new MidiMessageNotFoundException();
        }
    }

    /**
	 * Returns the first short message of the track
	 * @param track
	 * @return
	 */
    public static ShortMessage firstShortMessage(Track track) throws MidiMessageNotFoundException {
        Vector<MidiEvent> shortMessageEvents = allShortMessages(track);
        if (shortMessageEvents.size() > 0) {
            return (ShortMessage) shortMessageEvents.firstElement().getMessage();
        } else {
            throw new MidiMessageNotFoundException();
        }
    }

    /**
	 * Returns all the MidiEvents containing MetaMessages of the specified type in the given track 
	 * @param track
	 * @param type
	 * @return
	 */
    public static Vector<MidiEvent> filterMetaMessages(Track track, int type) {
        Vector<MidiEvent> events = new Vector<MidiEvent>();
        int i = 0;
        MidiEvent e;
        MidiMessage m;
        do {
            try {
                e = track.get(i);
            } catch (ArrayIndexOutOfBoundsException ex) {
                return events;
            }
            m = e.getMessage();
            if (m instanceof MetaMessage) {
                MetaMessage mm = (MetaMessage) m;
                if (mm.getType() == type) {
                    events.add(e);
                }
            }
            i++;
        } while (e != null);
        return events;
    }

    /**
	 * Returns all the MidiEvents containing ShortMessages of the specified command in the given track 
	 * @param track
	 * @param command
	 * @return
	 */
    public static Vector<MidiEvent> filterShortMessages(Track track, int command) {
        Vector<MidiEvent> events = new Vector<MidiEvent>();
        MidiEvent e;
        MidiMessage m;
        int i = 0;
        do {
            try {
                e = track.get(i);
            } catch (ArrayIndexOutOfBoundsException ex) {
                return events;
            }
            m = e.getMessage();
            if (m instanceof ShortMessage) {
                ShortMessage sm = (ShortMessage) m;
                if (sm.getCommand() == command) {
                    events.add(e);
                }
            }
            i++;
        } while (e != null);
        return events;
    }

    /**
	 * Returns a vector of all control change messages of the given control
	 * change number (ccNumber) on the given track (t)
	 * @param t 
	 * @param ccNumber
	 * @return
	 */
    public static Vector<MidiEvent> filterControlChanges(Track t, int ccNumber) {
        Vector<MidiEvent> events = filterShortMessages(t, ShortMessage.CONTROL_CHANGE);
        Vector<MidiEvent> controlChanges = new Vector<MidiEvent>();
        ShortMessage m;
        for (MidiEvent e : events) {
            m = (ShortMessage) e.getMessage();
            if (m.getData1() == ccNumber) {
                controlChanges.add(e);
            }
        }
        return controlChanges;
    }

    /**
	 * Returns all MetaMessages in the given track 
	 * as a vector of MidiEvents 
	 * @param track
	 * @return
	 */
    public static Vector<MidiEvent> allMetaMessages(Track track) {
        Vector<MidiEvent> events = new Vector<MidiEvent>();
        int i = 0;
        MidiEvent e;
        MidiMessage m;
        do {
            try {
                e = track.get(i);
            } catch (ArrayIndexOutOfBoundsException ex) {
                return events;
            }
            m = e.getMessage();
            if (m instanceof MetaMessage) {
                events.add(e);
            }
            i++;
        } while (e != null);
        return events;
    }

    public static Vector<NoteEvent> allNotes() {
        Vector<NoteEvent> notes = new Vector<NoteEvent>();
        Iterator<Integer> selection = App.UI.getTrackSelection().iterator();
        while (selection.hasNext()) {
            notes.addAll(allNotes(selection.next().intValue()));
        }
        return notes;
    }

    /**
	 * Returns all notes in the given track 
	 * as a vector of NoteEvents 
	 * @param track
	 * @return
	 */
    public static Vector<NoteEvent> allNotes(int trackIndex) {
        Vector<NoteEvent> notes = new Vector<NoteEvent>();
        Track track = App.Project.getTrack(trackIndex);
        MidiEvent memory[] = new MidiEvent[128];
        MidiEvent e;
        MidiMessage m;
        ShortMessage sm;
        for (int i = 0; i < track.size(); i++) {
            e = track.get(i);
            m = e.getMessage();
            if (m instanceof ShortMessage) {
                sm = (ShortMessage) m;
                if (sm.getData1() < 0 || sm.getData1() > 127) continue; else if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0) {
                    memory[sm.getData1()] = e;
                } else if (sm.getCommand() == ShortMessage.NOTE_OFF || sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0) {
                    if (memory[sm.getData1()] != null) {
                        notes.add(new NoteEvent(trackIndex, memory[sm.getData1()], e));
                        memory[sm.getData1()] = null;
                    }
                }
            }
        }
        return notes;
    }

    /**
	 * Returns all ShortMessages in the given track 
	 * as a vector of MidiEvents 
	 * @param track
	 * @return
	 */
    public static Vector<MidiEvent> allShortMessages(Track track) {
        Vector<MidiEvent> events = new Vector<MidiEvent>();
        MidiEvent e;
        MidiMessage m;
        int i = 0;
        do {
            try {
                e = track.get(i);
            } catch (ArrayIndexOutOfBoundsException ex) {
                return events;
            }
            m = e.getMessage();
            if (m instanceof ShortMessage) {
                events.add(e);
            }
            i++;
        } while (e != null);
        return events;
    }

    /**
	 * Returns all ShortMessages in the given track 
	 * as a vector of MidiEvents 
	 * @param track
	 * @return
	 */
    public static Vector<MidiEvent> allSysEx(Track track) {
        Vector<MidiEvent> events = new Vector<MidiEvent>();
        MidiEvent e;
        MidiMessage sex;
        int i = 0;
        do {
            try {
                e = track.get(i);
            } catch (ArrayIndexOutOfBoundsException ex) {
                return events;
            }
            sex = e.getMessage();
            if (sex instanceof SysexMessage) {
                events.add(e);
            }
            i++;
        } while (e != null);
        return events;
    }

    /**
	 * Factory method. Returns a clone of the Sequence s given as
	 * a parameter to the method.
	 * 
	 * @param s
	 * @return
	 */
    public static Sequence cloneSequence(Sequence s) {
        Track[] tracks = s.getTracks();
        try {
            Sequence s2 = new Sequence(s.getDivisionType(), s.getResolution());
            Track t2;
            MidiEvent e, e2;
            MidiMessage m, m2;
            int i;
            int tc = 0;
            for (Track t : tracks) {
                t2 = s2.createTrack();
                i = 0;
                do {
                    try {
                        e = t.get(i);
                        m = e.getMessage();
                        m2 = (MidiMessage) m.clone();
                        e2 = new MidiEvent(m2, e.getTick());
                        t2.add(e2);
                        i++;
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        break;
                    }
                } while (e != null);
                tc++;
            }
            return s2;
        } catch (InvalidMidiDataException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
	 * Returns all midi events on a track as a vector of MidiEventS
	 * @param t 
	 * @return
	 */
    public static Vector<MidiEvent> allMidiEvents(Track t) {
        Vector<MidiEvent> events = new Vector<MidiEvent>();
        MidiEvent e;
        int i = 0;
        do {
            try {
                e = t.get(i);
                events.add(e);
            } catch (ArrayIndexOutOfBoundsException ex) {
                return events;
            }
            i++;
        } while (e != null);
        return events;
    }

    /**
     * Returns all midi events for the tracks provided, sorted by when they occur.
     * If two events occur simultaneously, the event from t1 gets before the event from t2 in the list.
     * @param t
     * @return Every event in the current tracks in a Vector sorted by ticks
     */
    public static Vector<MidiEvent> allMidiEvents(Track t1, Track t2) {
        Vector<MidiEvent> events = new Vector<MidiEvent>();
        if (t1.size() == 0) return allMidiEvents(t2); else if (t2.size() == 0) return allMidiEvents(t1); else {
            MidiEvent e1, e2;
            e1 = t1.get(0);
            e2 = t2.get(0);
            for (int imax = t1.size(), jmax = t2.size(), i = 0, j = 0; ; ) {
                if (e1.getTick() < e2.getTick()) {
                    if (i < imax) {
                        events.add(e1);
                        i++;
                        if (i < imax) e1 = t1.get(i);
                    } else e1 = new MidiEvent(new ShortMessage(), Long.MAX_VALUE - 1);
                } else if (e1.getTick() == e2.getTick()) {
                    if (i < imax) {
                        events.add(e1);
                        i++;
                    }
                    if (j < jmax) {
                        events.add(e2);
                        j++;
                    }
                    if (i < imax) e1 = t1.get(i); else e2 = new MidiEvent(new ShortMessage(), Long.MAX_VALUE - 1);
                    if (j < jmax) e2 = t2.get(j); else e2 = new MidiEvent(new ShortMessage(), Long.MAX_VALUE - 1);
                } else {
                    if (j < jmax) {
                        events.add(e2);
                        j++;
                        if (j < jmax) {
                            e2 = t2.get(j);
                        } else e2 = new MidiEvent(new ShortMessage(), Long.MAX_VALUE - 1);
                    }
                }
                if (j >= jmax && i >= imax) return events;
            }
        }
    }

    /**
     * Returns a string representation of a MidiEvent
     * Can be seen as a static version of toString()
     * @param e
     * @return
     */
    public static String prettyPrint(MidiEvent e) {
        String s = String.valueOf(e.getTick()) + ": ";
        MidiMessage m = e.getMessage();
        if (m instanceof ShortMessage) {
            ShortMessage sm = (ShortMessage) m;
            s += "(SH) " + prettyPrint(sm);
        } else if (m instanceof MetaMessage) {
            MetaMessage mm = (MetaMessage) m;
            s += "(ME) " + prettyPrint(mm);
        } else if (m instanceof SysexMessage) {
            SysexMessage sm = (SysexMessage) m;
            s += "(SY) No. " + sm.getStatus() + " (data=" + Arrays.toString(sm.getData()) + ")";
        } else {
            s += "(UN) " + m.getClass().getName() + " (" + m.getStatus() + ")";
        }
        return s;
    }

    /**
     * Returns a string representation of a MetaMessage
     * @param mm
     * @return
     */
    private static String prettyPrint(MetaMessage mm) {
        byte[] data = mm.getData();
        switch(mm.getType()) {
            case MetaMessageType.TRACK_NAME:
                return "Track name: " + new String(data);
            case MetaMessageType.TRACK_TIMESIGNATURE:
                return "Time signature change: " + data[0] + " / " + (int) Math.pow(2, data[1]);
            case MetaMessageType.END_OF_TRACK:
                return "End of track";
            default:
                return "No. " + String.valueOf(mm.getType()) + " (data=" + Arrays.toString(mm.getData()) + ")";
        }
    }

    /**
     * Returns a string representation of a ShortMessage
     * @param sm
     * @return
     */
    private static String prettyPrint(ShortMessage sm) {
        switch(sm.getCommand()) {
            case ShortMessage.NOTE_OFF:
                return "Note #" + sm.getData1() + " OFF (ch=" + sm.getChannel() + ", vel=" + sm.getData2() + ")";
            case ShortMessage.NOTE_ON:
                return "Note #" + sm.getData1() + " ON (ch=" + sm.getChannel() + ", vel=" + sm.getData2() + ")";
            case ShortMessage.CONTROL_CHANGE:
                switch(sm.getData1()) {
                    case ControlChangeNumber.CHANNEL_VOLUME_LSB:
                        return "Channel " + sm.getChannel() + " volume LSB=" + sm.getData2();
                    case ControlChangeNumber.CHANNEL_VOLUME_MSB:
                        return "Channel " + sm.getChannel() + " volume MSB=" + sm.getData2();
                    case ControlChangeNumber.CHANNEL_BALANCE_LSB:
                        return "Channel " + sm.getChannel() + " balance LSB=" + sm.getData2();
                    case ControlChangeNumber.CHANNEL_BALANCE_MSB:
                        return "Channel " + sm.getChannel() + " balance MSB=" + sm.getData2();
                    default:
                        return "Control change no. " + sm.getData1() + " (ch=" + sm.getChannel() + ", val=" + sm.getData2() + ")";
                }
            default:
                return "Cmd: " + sm.getCommand() + " (ch=" + sm.getChannel() + ", data1=" + sm.getData1() + ", data2=" + sm.getData2() + ")";
        }
    }
}
