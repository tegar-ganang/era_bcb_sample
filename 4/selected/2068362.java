package jm.midi;

import java.util.Enumeration;
import java.util.Stack;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import ren.music.Player.Playable;
import jm.JMC;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;

/**
 * MidiSynth.java
 *
 * Created: Mon May 07 11:21:30 2001
 *
 * @author Mark Elston (enhanced by Andrew Brown)
 */
public class MidiSynth implements MetaEventListener {

    /** Pulses per quarter note value */
    private short m_ppqn;

    private Playable player;

    /** The Synthesizer we are using */
    private Synthesizer m_synth;

    /** The Sequencer we are using */
    private Sequencer m_sequencer;

    private Sequence seq;

    private Track callBackTrack;

    /** The current tempo value */
    private float m_currentTempo;

    /** The overall (Score) tempo value */
    private float m_masterTempo;

    /** All previous tempos */
    private Stack m_tempoHistory;

    /** The diff. beteen the score and part tempi */
    private double trackTempoRatio = 1.0;

    /** The diff. between the score and phrase tempi */
    private double elementTempoRatio = 1.0;

    /** The name of the jMusic score */
    private String scoreTitle;

    private long startPos = 0;

    private static final int StopType = 47;

    public MidiSynth() {
        this((short) 480);
    }

    public MidiSynth(short ppqn) {
        m_ppqn = ppqn;
        m_synth = null;
        m_tempoHistory = new Stack();
        this.initSynthesizer();
    }

    /**
    * Plays the score data via a MIDI synthesizer
    * @param Score score - data to change to SMF
    * @exception Exception 
    */
    public void play(Score score) throws InvalidMidiDataException {
        scoreTitle = score.getTitle();
        m_masterTempo = (float) score.getTempo();
        scoreToSeq(score);
        m_sequencer.setSequence(seq);
        m_sequencer.setTickPosition(this.startPos);
        m_sequencer.setTempoInBPM(m_masterTempo);
        m_sequencer.start();
    }

    private void callBackFired(int id) {
        this.player.getBeatTracker().increment();
    }

    public void addCallback(double startBeat, int callbackID) {
        if (callBackTrack == null) {
            callBackTrack = seq.createTrack();
        }
        long startTime = (long) (startBeat * this.m_ppqn);
        try {
            MetaMessage mm = new MetaMessage();
            mm.setMessage(70, new byte[] { new Integer(callbackID).byteValue() }, 1);
            callBackTrack.add(new MidiEvent(mm, startTime));
        } catch (javax.sound.midi.InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    /**
     * Invoked when a Sequencer has encountered and processed a MetaMessage
     * in the Sequence it is processing.
     * @param MetaMessage meta - the meta-message that the sequencer
     *                           encountered
     */
    public void meta(MetaMessage metaEvent) {
        switch(metaEvent.getType()) {
            case 70:
                callBackFired(new Byte((metaEvent.getData())[0]).intValue());
                break;
            case StopType:
                this.player.stop();
                break;
        }
    }

    /**
     * Create a Note On Event
     * @param int channel is the channel to change
     * @param int pitch is the pitch of the note
     * @param int velocity is the velocity of the note
     * @param long tick is the time this event occurs
     */
    protected static MidiEvent createNoteOnEvent(int channel, int pitch, int velocity, long tick) throws InvalidMidiDataException {
        ShortMessage msg = new ShortMessage();
        msg.setMessage(0x90 + channel, pitch, velocity);
        MidiEvent evt = new MidiEvent(msg, tick);
        return evt;
    }

    /**
     * Create a Note Off Event
     * @param int channel is the channel to change
     * @param int pitch is the pitch of the note
     * @param int velocity is the velocity of the note
     * @param long tick is the time this event occurs
     */
    protected static MidiEvent createNoteOffEvent(int channel, int pitch, int velocity, long tick) throws InvalidMidiDataException {
        ShortMessage msg = new ShortMessage();
        msg.setMessage(0x80 + channel, pitch, velocity);
        MidiEvent evt = new MidiEvent(msg, tick);
        return evt;
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
	 * Create a Control Change event
	 * @param int channel is the channel to use
	 * @param int controlNum is the control change number to use
	 * @param int value is the value of the control change
	 */
    protected static MidiEvent createCChangeEvent(int channel, int controlNum, int value, long tick) throws InvalidMidiDataException {
        ShortMessage msg = new ShortMessage();
        msg.setMessage(0xB0 + channel, controlNum, value);
        MidiEvent evt = new MidiEvent(msg, tick);
        return evt;
    }

    public void empty() {
        if (this.m_sequencer != null) {
            if (seq != null) {
                Track[] tracks = seq.getTracks();
                for (int i = 0; i < tracks.length; i++) {
                    seq.deleteTrack(tracks[i]);
                }
                callBackTrack = null;
            }
            this.update();
        }
    }

    public void update() {
        try {
            this.m_sequencer.setSequence(seq);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
    * Converts jmusic score data into a MIDI Sequence
    * @param Score score - data to play
    * @return Sequence to be played
    * @exception Exception 
    */
    protected void scoreToSeq(Score score) throws InvalidMidiDataException {
        if (seq == null) seq = new Sequence(Sequence.PPQ, m_ppqn); else empty();
        m_masterTempo = m_currentTempo = new Float(score.getTempo()).floatValue();
        Track longestTrack = null;
        double longestTime = 0.0;
        double longestRatio = 1.0;
        Enumeration parts = score.getPartList().elements();
        while (parts.hasMoreElements()) {
            Part inst = (Part) parts.nextElement();
            int currChannel = inst.getChannel();
            if (currChannel > 16) {
                InvalidMidiDataException ex = new InvalidMidiDataException(inst.getTitle() + " - Invalid Channel Number: " + currChannel);
                ex.fillInStackTrace();
                throw ex;
            }
            m_tempoHistory.push(new Float(m_currentTempo));
            float tempo = new Float(inst.getTempo()).floatValue();
            if (tempo != Part.DEFAULT_TEMPO) {
                m_currentTempo = tempo;
            } else if (tempo < Part.DEFAULT_TEMPO) System.out.println("jMusic MidiSynth error: Part TempoEvent (BPM) too low = " + tempo);
            trackTempoRatio = m_masterTempo / m_currentTempo;
            int instrument = inst.getInstrument();
            if (instrument == JMC.NO_INSTRUMENT) instrument = 0;
            Enumeration phrases = inst.getPhraseList().elements();
            double max = 0;
            double currentTime = 0.0;
            Track currTrack = seq.createTrack();
            while (phrases.hasMoreElements()) {
                Phrase phrase = (Phrase) phrases.nextElement();
                currentTime = phrase.getStartTime();
                long phraseTick = (long) (currentTime * m_ppqn * trackTempoRatio);
                MidiEvent evt;
                if (phrase.getInstrument() != JMC.NO_INSTRUMENT) instrument = phrase.getInstrument();
                evt = createProgramChangeEvent(currChannel, instrument, phraseTick);
                currTrack.add(evt);
                m_tempoHistory.push(new Float(m_currentTempo));
                tempo = new Float(phrase.getTempo()).floatValue();
                if (tempo != Phrase.DEFAULT_TEMPO) {
                    m_currentTempo = tempo;
                }
                elementTempoRatio = m_masterTempo / m_currentTempo;
                double lastPanPosition = -1.0;
                int offSetTime = 0;
                Enumeration notes = phrase.getNoteList().elements();
                while (notes.hasMoreElements()) {
                    Note note = (Note) notes.nextElement();
                    offSetTime = (int) (note.getOffset() * m_ppqn * elementTempoRatio);
                    int pitch = -1;
                    pitch = note.getPitch();
                    int dynamic = note.getDynamic();
                    if (pitch == Note.REST) {
                        phraseTick += note.getRhythmValue() * m_ppqn * elementTempoRatio;
                        continue;
                    }
                    long onTick = (long) (phraseTick);
                    if (note.getPan() != lastPanPosition) {
                        evt = createCChangeEvent(currChannel, 10, (int) (note.getPan() * 127), onTick);
                        currTrack.add(evt);
                        lastPanPosition = note.getPan();
                    }
                    evt = createNoteOnEvent(currChannel, pitch, dynamic, onTick + offSetTime);
                    currTrack.add(evt);
                    long offTick = (long) (phraseTick + note.getDuration() * m_ppqn * elementTempoRatio);
                    evt = createNoteOffEvent(currChannel, pitch, dynamic, offTick + offSetTime);
                    currTrack.add(evt);
                    phraseTick += note.getRhythmValue() * m_ppqn * elementTempoRatio;
                    if ((double) offTick > longestTime) {
                        longestTime = (double) offTick;
                        longestTrack = currTrack;
                    }
                }
                Float d = (Float) m_tempoHistory.pop();
                m_currentTempo = d.floatValue();
            }
            Float d = (Float) m_tempoHistory.pop();
            m_currentTempo = d.floatValue();
        }
        addCallBacksToSeq(longestTime);
        addEndEvent(longestTrack, longestTime);
    }

    private void addCallBacksToSeq(double longestTime) {
        int countCallTime = 0;
        while ((long) (countCallTime * this.player.getBeatTracker().getRes() * this.m_ppqn) < longestTime) {
            countCallTime++;
            this.addCallback(countCallTime * this.player.getBeatTracker().getRes(), countCallTime % 64);
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

    private boolean initSynthesizer() {
        if (null == m_synth) {
            try {
                if (MidiSystem.getSequencer() == null) {
                    System.err.println("MidiSystem Sequencer Unavailable");
                    return false;
                }
                m_synth = MidiSystem.getSynthesizer();
                m_synth.open();
                m_sequencer = MidiSystem.getSequencer();
                m_sequencer.open();
                m_sequencer.addMetaEventListener(this);
            } catch (MidiUnavailableException e) {
                System.err.println("Midi System Unavailable:" + e);
                return false;
            }
        }
        return true;
    }

    public Sequencer getSequencer() {
        return this.m_sequencer;
    }

    /**
     * used from external eg manual specification of beats
     * @param beats
     */
    public void setBeats(double beats) {
        this.startPos = (long) (beats * this.m_ppqn * 1.0);
        if (this.m_sequencer == null) return;
        this.m_sequencer.setTickPosition(startPos);
    }

    public double getBeats() {
        if (this.m_sequencer == null) return this.startPos * 1.0 / this.m_ppqn * 1.0;
        return this.m_sequencer.getTickPosition() * 1.0 / this.m_ppqn * 1.0;
    }

    public boolean isPlaying() {
        return this.m_sequencer.isRunning();
    }

    public void addPlayable(Playable playable) {
        this.player = playable;
    }

    public void unpause() {
        this.m_sequencer.start();
    }

    public void pause() {
        this.m_sequencer.stop();
    }

    public void stop() {
        if (this.m_sequencer.isRunning()) {
            this.m_sequencer.stop();
        }
        this.startPos = 0;
        this.resetPosition();
    }

    public void resetPosition() {
        if (this.m_sequencer.isRunning()) this.m_sequencer.stop();
        this.m_sequencer.setTickPosition(startPos);
        this.player.getBeatTracker().setBeat(this.getBeats());
    }
}
