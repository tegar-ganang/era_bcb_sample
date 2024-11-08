package doors.util;

import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import java.text.DecimalFormat;

/**
 * MIDI Utilities
 *
 * @author Adam Buckley, adambuckley@mail.com
 */
public class MidiUtil {

    public static final String[] NOTE_VALUES = { "C0", "Db0", "D0", "Eb0", "E0", "F0", "F#0", "G0", "G#0", "A0", "Bb0", "B0", "C1", "Db1", "D1", "Eb1", "E1", "F1", "F#1", "G1", "G#1", "A1", "Bb1", "B1", "C2", "Db2", "D2", "Eb2", "E2", "F2", "F#2", "G2", "G#2", "A2", "Bb2", "B2", "C3", "Db3", "D3", "Eb3", "E3", "F3", "F#3", "G3", "G#3", "A3", "Bb3", "B3", "C4", "Db4", "D4", "Eb4", "E4", "F4", "F#4", "G4", "G#4", "A4", "Bb4", "B4", "C5", "Db5", "D5", "Eb5", "E5", "F5", "F#5", "G5", "G#5", "A5", "Bb5", "B5", "C6", "Db6", "D6", "Eb6", "E6", "F6", "F#6", "G6", "G#6", "A6", "Bb6", "B6", "C7", "Db7", "D7", "Eb7", "E7", "F7", "F#7", "G7", "G#7", "A7", "Bb7", "B7", "C8", "Db8", "D8", "Eb8", "E8", "F8", "F#8", "G8", "G#8", "A8", "Bb8", "B8", "C9", "Db9", "D9", "Eb9", "E9", "F9", "F#9", "G9", "G#9", "A9", "Bb9", "B9", "C10", "Db10", "D10", "Eb10", "E10", "F10", "F#10", "G10" };

    private static DecimalFormat numberFormatter = new DecimalFormat("000");

    /**
	 * @returns an array of 16 booleans, each, of which, represents the presence of
	 * events for the corresponding channel.
	 */
    public static boolean[] getChannelPresence(Sequence seq) {
        boolean[] eventPresence = new boolean[16];
        Track[] tracks = seq.getTracks();
        for (int tn = 0; tn < tracks.length; tn++) {
            Track track = tracks[tn];
            for (int en = 0; en < track.size(); en++) {
                MidiEvent me = track.get(en);
                MidiMessage mm = me.getMessage();
                int status = mm.getStatus();
                int channel = status & 0x0F;
                if (mm instanceof ShortMessage) eventPresence[channel] = true;
            }
        }
        return eventPresence;
    }

    /**
	 * Returns a String representation of a tick
	 */
    public static String tickToString(long tick, int resolution) {
        long beat = (tick / resolution) + 1;
        long t = tick % resolution;
        return beat + ":" + numberFormatter.format(t);
    }

    /**
	 * Returns a string representation of a note number
	 */
    public static String noteValueToString(byte noteValue) {
        if (noteValue > 127 || noteValue < 0) return "###";
        return NOTE_VALUES[noteValue];
    }

    /**
	 * Returns the tempo of a <code>Sequence</code>
	 */
    public static float getTempo(Sequence s) {
        return (float) (60000000 * s.getTickLength()) / (s.getResolution() * s.getMicrosecondLength());
    }

    /**
	 * Gets the PPQ of a sequence, regardless of the internal division type
	 * in use.
	 */
    public static int getPPQ(Sequence sequence) {
        int r = 0;
        if (sequence.getDivisionType() == Sequence.PPQ) {
            r = sequence.getResolution();
        } else {
            throw new RuntimeException("Unsuported division type!");
        }
        return r;
    }

    /**
	 * Retuns a string representation of the status byte of a <code>ShortMessage</code>
	 */
    public static String statusToString(int status) {
        String r;
        switch(status) {
            case ShortMessage.ACTIVE_SENSING:
                r = "Active Sensing";
                break;
            case ShortMessage.CHANNEL_PRESSURE:
                r = "Channel Pressure";
                break;
            case ShortMessage.CONTINUE:
                r = "Continue";
                break;
            case ShortMessage.CONTROL_CHANGE:
                r = "Control Change";
                break;
            case ShortMessage.END_OF_EXCLUSIVE:
                r = "End of Exclusive";
                break;
            case ShortMessage.MIDI_TIME_CODE:
                r = "Midi Time Code";
                break;
            case ShortMessage.NOTE_OFF:
                r = "Note Off";
                break;
            case ShortMessage.NOTE_ON:
                r = "Note On";
                break;
            case ShortMessage.PITCH_BEND:
                r = "Pitch Bend";
                break;
            case ShortMessage.POLY_PRESSURE:
                r = "Poly Pressure";
                break;
            case ShortMessage.PROGRAM_CHANGE:
                r = "Program Change";
                break;
            case ShortMessage.SONG_POSITION_POINTER:
                r = "Song Position Pointer";
                break;
            case ShortMessage.SONG_SELECT:
                r = "Song Select";
                break;
            case ShortMessage.START:
                r = "Start";
                break;
            case ShortMessage.STOP:
                r = "Stop";
                break;
            case ShortMessage.SYSTEM_RESET:
                r = "System Reset";
                break;
            case ShortMessage.TIMING_CLOCK:
                r = "Timing Clock";
                break;
            case ShortMessage.TUNE_REQUEST:
                r = "Tune Request";
                break;
            default:
                r = "Unknown (" + status + ")";
        }
        return r;
    }

    /**
	 * Returns a string representation of a <code>MidiMessage</code>
	 */
    public static String midiMessageToString(MidiMessage mm) {
        String r = "(unknown)";
        if (mm instanceof MetaMessage) {
            MetaMessage m = (MetaMessage) mm;
            r = "MetaMessage:" + m.getType();
        } else if (mm instanceof ShortMessage) {
            ShortMessage m = (ShortMessage) mm;
            r = "c" + m.getChannel() + ":" + statusToString(m.getCommand()) + ":" + m.getData1() + ":" + m.getData2();
        } else if (mm instanceof SysexMessage) {
            r = "SysexMessage";
        }
        return r;
    }

    public static double tickToBeat(long tick, int resolution) {
        return ((double) tick) / ((double) resolution);
    }

    public static long beatToTick(double beat, int resolution) {
        return new Double(beat * resolution).longValue();
    }

    public static double microsecondToBeat(long microsecond, double bpm) {
        return (bpm * microsecond) / 60000000.0;
    }

    public static long beatToMicrosecond(double beat, double bpm) {
        return new Double(beat * bpmToMicrosecondsPerBeat(bpm)).longValue();
    }

    public static long microsecondToTick(long microsecond, double bpm, int resolution) {
        double beat = microsecondToBeat(microsecond, bpm);
        return beatToTick(beat, resolution);
    }

    public static long tickToMicrosecond(long tick, double bpm, int resolution) {
        double beat = tickToBeat(tick, resolution);
        return beatToMicrosecond(beat, bpm);
    }

    public static double currentTimeBeat(long beatOne, double bpm, long currentTimeMicros) {
        long currentTimeMicroseconds = currentTimeMicros - beatOne;
        return microsecondToBeat(currentTimeMicroseconds, bpm);
    }

    public static int bpmToMicrosecondsPerBeat(double bpm) {
        return (int) (60000000.0 / bpm);
    }

    public static double microsecondsPerBeatToBpm(int microseconds) {
        return (60000000.0 / microseconds);
    }

    /**
	 * midiArrayToString
	 * 
	 * @param data a variable-length array of bytes
	 * @return Eg. (0x92, 36, 127)
	 */
    public static String midiArrayToString(byte[] data) {
        StringBuffer sb = new StringBuffer();
        sb.append("(0x" + (Integer.toString(data[0] & 0xFF, 16)) + ", ");
        for (int j = 1; j < data.length - 1; j++) {
            sb.append((data[j] & 0xFF) + ", ");
        }
        sb.append(data[data.length - 1] + ")");
        return sb.toString();
    }
}
