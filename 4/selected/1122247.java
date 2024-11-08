package com.lemu.music.jmjs;

import java.util.Enumeration;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import ren.util.PO;
import jm.JMC;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;

public class JmJsUtil {

    private static int resolution_ppqn = Integer.valueOf(System.getProperty("com.lemu.resolution_ppqn", "240"));

    private static double update_resolution_beats = Double.valueOf(System.getProperty("com.lemu.update_resolution_beats", "0.25"));

    public static final int StopType = 47;

    /**
	 * Converts jmusic score data into a MIDI Sequence
	 * 
	 * @param Score
	 *            score - score to convert
	 * @return Sequence that has been create from the score
	 * @exception Exception
	 */
    public Sequence scoreToSeq(Score score) throws InvalidMidiDataException {
        PO.p("score to seq \n" + score);
        Sequence seq = new Sequence(Sequence.PPQ, resolution_ppqn);
        ScoreTracker st = new ScoreTracker();
        st.longestTime = 0;
        Enumeration partList = score.getPartList().elements();
        while (partList.hasMoreElements()) {
            Part part = (Part) partList.nextElement();
            st.currChannel = ((part.getChannel() - 1) % 16) + 1;
            st.currPort = (int) (part.getChannel() * 1.0 / 16.0);
            st.currTrack = seq.createTrack();
            addInstrument(st, part.getInstrument());
            Enumeration phrases = part.getPhraseList().elements();
            while (phrases.hasMoreElements()) {
                Phrase phrase = (Phrase) phrases.nextElement();
                st.phraseTick = (long) (phrase.getStartTime() * resolution_ppqn);
                Enumeration notes = phrase.getNoteList().elements();
                while (notes.hasMoreElements()) {
                    Note note = (Note) notes.nextElement();
                    noteToEvents(note, st);
                }
            }
        }
        addCallBacksToSeq(st.longestTime, seq);
        addEndEvent(st.longestTrack, st.longestTime);
        return seq;
    }

    private void addInstrument(ScoreTracker st, int instrument) throws InvalidMidiDataException {
        instrument = (instrument == JMC.NO_INSTRUMENT) ? JMC.PIANO : instrument;
        st.currTrack.add(createProgramChangeEvent(st.currChannel, instrument, 0));
    }

    /**
     * Create a Program Change Event
     * @param int channel is the channel to change
     * @param int value is the new value to use
     * @param long tick is the time this event occurs
     */
    protected static MidiEvent createProgramChangeEvent(int channel, int value, long tick) throws InvalidMidiDataException {
        ShortMessage msg = new ShortMessage();
        msg.setMessage(0xC0 + channel, value, 0);
        MidiEvent evt = new MidiEvent(msg, tick);
        return evt;
    }

    /**
	 * 
	 * @param note
	 * @return the time elapsed in the rendering of this note (to be added to
	 *         phrase tick)
	 */
    private void noteToEvents(Note note, ScoreTracker st) throws InvalidMidiDataException {
        int noteOnTick = convertRhythmToPulses(note.getRhythmValue());
        if (note.getPitch() != Note.REST) {
            st.currTrack.add(createNoteOnEvent(st.currChannel, note.getPitch(), note.getDynamic(), st.phraseTick + noteOnTick));
            long offTick = (long) (st.phraseTick + convertRhythmToPulses(note.getDuration()));
            st.currTrack.add(createNoteOffEvent(st.currChannel, note.getPitch(), note.getDynamic(), offTick + noteOnTick));
            if ((double) offTick > st.longestTime) {
                st.longestTime = (double) offTick;
                st.longestTrack = st.currTrack;
            }
        }
        st.phraseTick += noteOnTick;
    }

    private void addCallBacksToSeq(double longestTime, Sequence seq) {
        int countCallTime = 0;
        while ((long) (countCallTime * update_resolution_beats * resolution_ppqn) < longestTime) {
            countCallTime++;
            this.addCallback(countCallTime * update_resolution_beats, countCallTime % 64, seq);
        }
    }

    public void addCallback(double startBeat, int callbackID, Sequence seq) {
        Track track = seq.createTrack();
        long startTime = (long) (startBeat * resolution_ppqn);
        try {
            MetaMessage mm = new MetaMessage();
            mm.setMessage(70, new byte[] { new Integer(callbackID).byteValue() }, 1);
            track.add(new MidiEvent(mm, startTime));
        } catch (javax.sound.midi.InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    private void addEndEvent(Track longestTrack, double longestTime) throws InvalidMidiDataException {
        if (longestTime > 0.0 && longestTrack != null) {
            MetaMessage msg = new MetaMessage();
            byte[] data = new byte[0];
            msg.setMessage(StopType, data, 0);
            MidiEvent evt = new MidiEvent(msg, (long) longestTime + 100);
            longestTrack.add(evt);
        }
    }

    private int convertRhythmToPulses(double beats) {
        return (int) (beats * resolution_ppqn);
    }

    /**
	 * Create a Note On Event
	 * 
	 * @param int channel is the channel to change
	 * @param int pitch is the pitch of the note
	 * @param int velocity is the velocity of the note
	 * @param long tick is the time this event occurs
	 */
    public static MidiEvent createNoteOnEvent(int channel, int pitch, int velocity, long tick) throws InvalidMidiDataException {
        ShortMessage msg = new ShortMessage();
        msg.setMessage(0x90 + channel, pitch, velocity);
        MidiEvent evt = new MidiEvent(msg, tick);
        return evt;
    }

    /**
	 * Create a Note Off Event
	 * 
	 * @param int channel is the channel to change
	 * @param int pitch is the pitch of the note
	 * @param int velocity is the velocity of the note
	 * @param long tick is the time this event occurs
	 */
    public static MidiEvent createNoteOffEvent(int channel, int pitch, int velocity, long tick) throws InvalidMidiDataException {
        ShortMessage msg = new ShortMessage();
        msg.setMessage(0x80 + channel, pitch, velocity);
        MidiEvent evt = new MidiEvent(msg, tick);
        return evt;
    }

    public long beatToTick(double at) {
        return (long) (at * resolution_ppqn);
    }
}

class ScoreTracker {

    public ScoreTracker() {
    }

    long phraseTick;

    int currChannel;

    int currPort;

    Track currTrack;

    double longestTime;

    Track longestTrack;
}
