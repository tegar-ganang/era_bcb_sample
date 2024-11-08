package eu.davidgamez.mas.agents.simplerandom.midi;

import java.util.Iterator;
import java.util.Random;
import java.util.ArrayList;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import org.w3c.dom.Document;
import eu.davidgamez.mas.Constants;
import eu.davidgamez.mas.Util;
import eu.davidgamez.mas.exception.MASAgentException;
import eu.davidgamez.mas.exception.MASXmlException;
import eu.davidgamez.mas.gui.MsgHandler;
import eu.davidgamez.mas.midi.Agent;
import eu.davidgamez.mas.midi.MIDIEvent;
import eu.davidgamez.mas.midi.Track;

public class SimpleRandom extends Agent implements Constants {

    /** Velocity used for all notes */
    static final int NOTE_VELOCITY = 80;

    /** The minimum pitch of the randomly generated notes. Ranges from 0-127 
	 	Must be <= maximumPitch. */
    private int minimumPitch = 0;

    /** The maximum pitch of the randomly generated notes. Ranges from 0-127. 
	 	Must be >=minimumPitch. */
    private int maximumPitch = 127;

    /** The frequency at which the random function will be evaluated */
    private int noteFrequency_ppq = PPQ_RESOLUTION;

    /** The probability that a note is generated each time the random function is evaluated */
    private double density = 0.5;

    /** The minimum length of note that is generated */
    private int minNoteLength_ppq = PPQ_RESOLUTION / 4;

    /** The maximum length of note that is generated */
    private int maxNoteLength_ppq = PPQ_RESOLUTION / 2;

    /** Stores note off events that extend beyond the end of the bar */
    private ArrayList<MIDIEvent> midiEventArrayList = new ArrayList<MIDIEvent>();

    /** Random number generator for the class */
    private Random randomGenerator = new Random();

    /** Constructor */
    public SimpleRandom() {
        super("Simple Random", "Simple Random", "SimpleRandom");
    }

    @Override
    public String getXML(String indent) {
        String tmpStr = indent + "<midi_agent>";
        tmpStr += super.getXML(indent + "\t");
        tmpStr += indent + "\t<min_pitch>" + minimumPitch + "</min_pitch>";
        tmpStr += indent + "\t<max_pitch>" + maximumPitch + "</max_pitch>";
        tmpStr += indent + "\t<note_frequency>" + noteFrequency_ppq + "</note_frequency>";
        tmpStr += indent + "\t<density>" + density + "</density>";
        tmpStr += indent + "\t<min_note_length>" + minNoteLength_ppq + "</min_note_length>";
        tmpStr += indent + "\t<max_note_length>" + maxNoteLength_ppq + "</max_note_length>";
        tmpStr += indent + "</midi_agent>";
        return tmpStr;
    }

    @Override
    public void loadFromXML(String xmlStr) throws MASXmlException {
        super.loadFromXML(xmlStr);
        try {
            Document xmlDoc = Util.getXMLDocument(xmlStr);
            minimumPitch = Util.getIntParameter("min_pitch", xmlDoc);
            maximumPitch = Util.getIntParameter("max_pitch", xmlDoc);
            noteFrequency_ppq = Util.getIntParameter("note_frequency", xmlDoc);
            density = Util.getDoubleParameter("density", xmlDoc);
            minNoteLength_ppq = Util.getIntParameter("min_note_length", xmlDoc);
            maxNoteLength_ppq = Util.getIntParameter("max_note_length", xmlDoc);
        } catch (Exception ex) {
            System.out.println(xmlStr);
            ex.printStackTrace();
            MsgHandler.error(ex.getMessage());
        }
    }

    @Override
    public boolean updateTracks(long bufferStart_ppq, long bufferEnd_ppq) throws InvalidMidiDataException {
        outputStoredNotes(bufferStart_ppq, bufferEnd_ppq);
        long noteInsertionPoint_ppq = 0;
        if (bufferStart_ppq % noteFrequency_ppq == 0) noteInsertionPoint_ppq = bufferStart_ppq; else noteInsertionPoint_ppq = bufferStart_ppq - (bufferStart_ppq % noteFrequency_ppq) + noteFrequency_ppq;
        while (noteInsertionPoint_ppq < bufferEnd_ppq) {
            addNote(noteInsertionPoint_ppq, bufferEnd_ppq);
            noteInsertionPoint_ppq += noteFrequency_ppq;
        }
        return true;
    }

    @Override
    public void connectionStatusChanged() {
    }

    @Override
    public void enabledStatusChanged() {
    }

    @Override
    protected void reset() {
        midiEventArrayList.clear();
    }

    /** Generates a random note and adds it to all tracks. 
	   	Note off messages that fall outside the buffer are stored for a later point in time.	 */
    private void addNote(long noteStart_ppq, long bufferEnd_ppq) throws InvalidMidiDataException {
        if (randomGenerator.nextDouble() > density) return;
        int pitch = minimumPitch + randomGenerator.nextInt(maximumPitch - minimumPitch + 1);
        int length_ppq = minNoteLength_ppq + randomGenerator.nextInt(maxNoteLength_ppq - minNoteLength_ppq + 1);
        ShortMessage on = new ShortMessage();
        for (Track midiTrack : trackMap.values()) {
            on.setMessage(ShortMessage.NOTE_ON, midiTrack.getChannel(), pitch, NOTE_VELOCITY);
            midiTrack.addMidiMessage(noteStart_ppq, on);
        }
        ShortMessage off = new ShortMessage();
        if ((noteStart_ppq + length_ppq) >= bufferEnd_ppq) {
            off.setMessage(ShortMessage.NOTE_OFF, 0, pitch, NOTE_VELOCITY);
            midiEventArrayList.add(new MIDIEvent(off, noteStart_ppq + length_ppq));
        } else {
            for (Track midiTrack : trackMap.values()) {
                off.setMessage(ShortMessage.NOTE_OFF, midiTrack.getChannel(), pitch, NOTE_VELOCITY);
                midiTrack.addMidiMessage(noteStart_ppq + length_ppq, off);
            }
        }
    }

    /** Add any MIDI events that were generated in the previous cycle, but could not be added
        because they fell outside the buffer */
    private void outputStoredNotes(long bufferStart_ppq, long bufferEnd_ppq) throws InvalidMidiDataException {
        Iterator<MIDIEvent> iter = midiEventArrayList.iterator();
        while (iter.hasNext()) {
            MIDIEvent tempMidiEvent = iter.next();
            if (tempMidiEvent.getTimeStamp() < bufferStart_ppq) {
                iter.remove();
            } else if (tempMidiEvent.getTimeStamp() < bufferEnd_ppq) {
                for (Track midiTrack : trackMap.values()) {
                    ShortMessage shortMsg = tempMidiEvent.getMessage();
                    shortMsg.setMessage(shortMsg.getCommand(), midiTrack.getChannel(), shortMsg.getData1(), shortMsg.getData2());
                    midiTrack.addMidiMessage(tempMidiEvent.getTimeStamp(), shortMsg);
                    iter.remove();
                }
            }
        }
    }

    public int getMinimumPitch() {
        return minimumPitch;
    }

    public int getMaximumPitch() {
        return maximumPitch;
    }

    public void setPitchRange(int minimumPitch, int maximumPitch) throws MASAgentException {
        if (minimumPitch > 127 || minimumPitch < 0) throw new MASAgentException("SimpleRandom: Minimum pitch must be in the range 0-127");
        if (minimumPitch > maximumPitch) throw new MASAgentException("SimpleRandom: Minimum pitch must <= maximumPitch");
        if (maximumPitch > 127 || maximumPitch < 0) throw new MASAgentException("SimpleRandom: Maximum pitch must be in the range 0-127");
        if (maximumPitch < minimumPitch) throw new MASAgentException("SimpleRandom: Maximum pitch must >= minimumPitch");
        this.maximumPitch = maximumPitch;
        this.minimumPitch = minimumPitch;
    }

    public int getNoteFrequency_ppq() {
        return noteFrequency_ppq;
    }

    public void setNoteFrequency_ppq(int noteFrequency_ppq) {
        this.noteFrequency_ppq = noteFrequency_ppq;
    }

    public double getDensity() {
        return density;
    }

    public void setDensity(double density) throws MASAgentException {
        if (density < 0.0 || density > 1.0) throw new MASAgentException("SimpleRandom: Density must be in the range 0-1");
        this.density = density;
    }

    public int getMinNoteLength_ppq() {
        return minNoteLength_ppq;
    }

    public void setNoteLengthRange_ppq(int minNoteLength_ppq, int maxNoteLength_ppq) throws MASAgentException {
        if (minNoteLength_ppq > maxNoteLength_ppq) throw new MASAgentException("SimpleRandom: Minimum note length cannot be greater than the maximum note length");
        if (maxNoteLength_ppq < minNoteLength_ppq) throw new MASAgentException("SimpleRandom: Maximum note length cannot be less than the minimum note length");
        this.minNoteLength_ppq = minNoteLength_ppq;
        this.maxNoteLength_ppq = maxNoteLength_ppq;
    }

    public int getMaxNoteLength_ppq() {
        return maxNoteLength_ppq;
    }
}
