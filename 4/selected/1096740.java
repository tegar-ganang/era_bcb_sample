package eu.davidgamez.mas.agents.basicnotes.midi;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Iterator;
import java.io.BufferedWriter;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import org.w3c.dom.Document;
import eu.davidgamez.mas.Constants;
import eu.davidgamez.mas.Util;
import eu.davidgamez.mas.exception.MASXmlException;
import eu.davidgamez.mas.gui.MsgHandler;
import eu.davidgamez.mas.midi.Agent;
import eu.davidgamez.mas.midi.AgentMessage;
import eu.davidgamez.mas.midi.MIDINote;
import eu.davidgamez.mas.midi.MIDIEvent;
import eu.davidgamez.mas.midi.Track;
import eu.davidgamez.mas.midi.Utilities;

public class BasicNotes extends Agent implements Constants {

    private boolean synchToBar = true;

    private int sequenceLength_ppq = -1;

    private int noteCounter = 0;

    private long insertionPoint_ppq = -1;

    private boolean addNotes = false;

    private long bufferEnd_ppq = -1;

    private int[] notePositionArray = new int[] { 0, 2 * PPQ_RESOLUTION };

    private int[] noteLengthArray = new int[] { 1 * PPQ_RESOLUTION, 1 * PPQ_RESOLUTION };

    private int[] notePitchArray = new int[] { 60, 70 };

    private int noteVelocity = 80;

    private ArrayList<MIDIEvent> midiEventArrayList = new ArrayList<MIDIEvent>();

    public BasicNotes() {
        super("Basic Notes", "Basic Notes", "BasicNotes");
    }

    public String getXML(String indent) {
        String tmpDesc = indent + "<midi_agent>";
        tmpDesc += super.getXML(indent + "\t");
        tmpDesc += indent + "\t<synch_to_bar>" + synchToBar + "</synch_to_bar>";
        tmpDesc += indent + "\t<note_positions>";
        for (int i = 0; i < notePositionArray.length - 1; ++i) tmpDesc += notePositionArray[i] + ",";
        tmpDesc += notePositionArray[notePositionArray.length - 1] + "</note_positions>";
        tmpDesc += indent + "\t<note_lengths>";
        for (int i = 0; i < noteLengthArray.length - 1; ++i) tmpDesc += noteLengthArray[i] + ",";
        tmpDesc += noteLengthArray[noteLengthArray.length - 1] + "</note_lengths>";
        tmpDesc += indent + "\t<note_pitches>";
        for (int i = 0; i < notePitchArray.length - 1; ++i) tmpDesc += notePitchArray[i] + ",";
        tmpDesc += notePitchArray[notePitchArray.length - 1] + "</note_pitches>";
        tmpDesc += indent + "\t<note_velocity>" + noteVelocity + "</note_velocity>";
        tmpDesc += indent + "</midi_agent>";
        return tmpDesc;
    }

    /** Loads agent parameters from the supplied XML string */
    public void loadFromXML(String xmlStr) throws MASXmlException {
        super.loadFromXML(xmlStr);
        try {
            Document xmlDoc = Util.getXMLDocument(xmlStr);
            synchToBar = Util.getBoolParameter("synch_to_bar", xmlDoc);
            notePositionArray = Util.getIntArrayParameter("note_positions", xmlDoc);
            noteLengthArray = Util.getIntArrayParameter("note_lengths", xmlDoc);
            notePitchArray = Util.getIntArrayParameter("note_pitches", xmlDoc);
            noteVelocity = Util.getIntParameter("note_velocity", xmlDoc);
        } catch (Exception ex) {
            System.out.println(xmlStr);
            ex.printStackTrace();
            MsgHandler.error(ex.getMessage());
            return;
        }
        if (notePositionArray.length != notePitchArray.length || notePositionArray.length != noteLengthArray.length) {
            throw new MASXmlException("Array Lengths do not match");
        }
        calculateSequenceLength();
        resetAgent();
    }

    public void setNotes(TreeMap<Integer, MIDINote> noteList) {
        notePositionArray = new int[noteList.size()];
        notePitchArray = new int[noteList.size()];
        noteLengthArray = new int[noteList.size()];
        int i = 0;
        for (Integer tempInt : noteList.keySet()) {
            notePositionArray[i] = tempInt.intValue();
            notePitchArray[i] = noteList.get(tempInt).getPitch();
            noteLengthArray[i] = (int) noteList.get(tempInt).getLength_ppq();
            ++i;
        }
        calculateSequenceLength();
        resetAgent();
    }

    public void enabledStatusChanged() {
        if (!this.isEnabled()) resetAgent();
    }

    public void connectionStatusChanged() {
        if (trackMap.isEmpty()) {
            resetAgent();
        }
    }

    public int[] getNotePositionArray() {
        return notePositionArray;
    }

    public int[] getNotePitchArray() {
        return notePitchArray;
    }

    public int[] getNoteLengthArray() {
        return noteLengthArray;
    }

    public void setSynchToBar(boolean val) {
        synchToBar = val;
        resetAgent();
    }

    public boolean getSynchToBar() {
        return synchToBar;
    }

    protected boolean updateTracks(long bufferStart_ppq, long bufferEnd_ppq) throws InvalidMidiDataException {
        this.bufferEnd_ppq = bufferEnd_ppq;
        for (Track midiTrack : trackMap.values()) {
            Iterator<MIDIEvent> iter = midiEventArrayList.iterator();
            while (iter.hasNext()) {
                MIDIEvent tempMidiEvent = iter.next();
                if (tempMidiEvent.getTimeStamp() < bufferStart_ppq) iter.remove(); else if (tempMidiEvent.getTimeStamp() < bufferEnd_ppq) {
                    midiTrack.addMidiMessage(tempMidiEvent.getTimeStamp(), tempMidiEvent.getMessage());
                    iter.remove();
                }
            }
            if (synchToBar) {
                for (long beat_ppq = Utilities.getFirstBeatInBuffer(bufferStart_ppq); beat_ppq < bufferEnd_ppq; beat_ppq += PPQ_RESOLUTION) {
                    ArrayList<AgentMessage> agMsgArrayList = midiTrack.getAgentMessages(beat_ppq);
                    for (AgentMessage agMsg : agMsgArrayList) {
                        if (agMsg.getType() == AgentMessage.START_BAR) {
                            noteCounter = 0;
                            insertionPoint_ppq = beat_ppq;
                            addNotes = true;
                        }
                    }
                    if (addNotes) {
                        long notePoint_ppq = notePositionArray[noteCounter] + insertionPoint_ppq;
                        while (notePoint_ppq >= beat_ppq && notePoint_ppq < beat_ppq + PPQ_RESOLUTION && notePoint_ppq < bufferEnd_ppq && addNotes) {
                            addNote(notePoint_ppq, notePitchArray[noteCounter], noteLengthArray[noteCounter], midiTrack);
                            ++noteCounter;
                            if (noteCounter == notePositionArray.length) {
                                noteCounter = 0;
                                addNotes = false;
                            }
                            notePoint_ppq = notePositionArray[noteCounter] + insertionPoint_ppq;
                        }
                    }
                }
            } else {
                if (bufferStart_ppq == 0 || insertionPoint_ppq == -1) {
                    insertionPoint_ppq = Utilities.getFirstBeatInBuffer(bufferStart_ppq);
                    noteCounter = 0;
                }
                long notePoint_ppq = notePositionArray[noteCounter] + insertionPoint_ppq;
                while (notePoint_ppq >= bufferStart_ppq && notePoint_ppq < bufferEnd_ppq) {
                    addNote(notePoint_ppq, notePitchArray[noteCounter], noteLengthArray[noteCounter], midiTrack);
                    ++noteCounter;
                    noteCounter %= notePositionArray.length;
                    if (noteCounter == 0) insertionPoint_ppq += sequenceLength_ppq;
                    notePoint_ppq = notePositionArray[noteCounter] + insertionPoint_ppq;
                }
            }
        }
        return true;
    }

    private void addNote(long notePosition_ppq, int notePitch, int noteLength_ppq, Track midiTrack) throws InvalidMidiDataException {
        try {
            ShortMessage on = new ShortMessage();
            on.setMessage(ShortMessage.NOTE_ON, midiTrack.getChannel(), notePitch, noteVelocity);
            midiTrack.addMidiMessage(notePosition_ppq, on);
            ShortMessage off = new ShortMessage();
            off.setMessage(ShortMessage.NOTE_OFF, midiTrack.getChannel(), notePitch, noteVelocity);
            if ((notePosition_ppq + noteLength_ppq) >= bufferEnd_ppq) {
                midiEventArrayList.add(new MIDIEvent(off, notePosition_ppq + noteLength_ppq));
            } else {
                midiTrack.addMidiMessage(notePosition_ppq + noteLength_ppq, off);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void resetAgent() {
        noteCounter = 0;
        insertionPoint_ppq = -1;
        addNotes = false;
        midiEventArrayList.clear();
    }

    private void calculateSequenceLength() {
        sequenceLength_ppq = 0;
        if (notePositionArray.length > 0) {
            sequenceLength_ppq = (((notePositionArray[notePositionArray.length - 1] + noteLengthArray[notePositionArray.length - 1]) / PPQ_RESOLUTION) * PPQ_RESOLUTION) + PPQ_RESOLUTION;
        } else {
            System.out.println("Note emphasis array length is zero!");
            sequenceLength_ppq = 0;
        }
    }

    private void printAgent() {
        System.out.println("------------------------ Basic Notes MIDI Agent -----------------------");
        for (int i = 0; i < notePositionArray.length; ++i) {
            System.out.println("Note position = " + notePositionArray[i] + "; note pitch = " + notePitchArray[i] + "; note length = " + noteLengthArray[i]);
        }
    }

    @Override
    protected void reset() {
    }
}
