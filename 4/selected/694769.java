package eu.davidgamez.mas.agents.noteemphasis.midi;

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
import eu.davidgamez.mas.midi.Track;
import eu.davidgamez.mas.midi.Utilities;

public class NoteEmphasis extends Agent implements Constants {

    static final int maxVelocity = 127;

    static final int minVelocity = 0;

    private boolean synchToBar = true;

    private int sequenceLength_ppq;

    private int emphasisCounter = 0;

    private long insertionPoint_ppq = -1;

    private boolean addEmphasis = false;

    private int[] noteEmphasisArray = new int[] { 0, 2 * PPQ_RESOLUTION };

    private int[] velocityEmphasisArray = new int[] { 120, 110 };

    public NoteEmphasis() {
        super("Note Emphasis", "Note Emphasis", "NoteEmphasis");
    }

    @Override
    public void connectionStatusChanged() {
        if (trackMap.isEmpty()) {
            reset();
        }
    }

    /** Returns an XML string describing the agent */
    public String getXML(String indent) {
        String tmpStr = indent + "<midi_agent>";
        tmpStr += super.getXML(indent + "\t");
        tmpStr += indent + "\t<note_emphasis_array>";
        for (int i = 0; i < noteEmphasisArray.length - 1; ++i) tmpStr += noteEmphasisArray[i] + ",";
        tmpStr += noteEmphasisArray[noteEmphasisArray.length - 1] + "</note_emphasis_array>";
        tmpStr += indent + "\t<velocity_emphasis_array>";
        for (int i = 0; i < velocityEmphasisArray.length - 1; ++i) tmpStr += velocityEmphasisArray[i] + ",";
        tmpStr += velocityEmphasisArray[velocityEmphasisArray.length - 1] + "</velocity_emphasis_array>";
        tmpStr += indent + "\t<synch_to_bar>" + synchToBar + "</synch_to_bar>";
        tmpStr += indent + "</midi_agent>";
        return tmpStr;
    }

    /** Loads the agent's state from the supplied XML string */
    public void loadFromXML(String xmlStr) throws MASXmlException {
        super.loadFromXML(xmlStr);
        try {
            Document xmlDoc = Util.getXMLDocument(xmlStr);
            synchToBar = Util.getBoolParameter("synch_to_bar", xmlDoc);
            noteEmphasisArray = Util.getIntArrayParameter("note_emphasis_array", xmlDoc);
            velocityEmphasisArray = Util.getIntArrayParameter("velocity_emphasis_array", xmlDoc);
        } catch (Exception ex) {
            System.out.println(xmlStr);
            ex.printStackTrace();
            MsgHandler.error(ex.getMessage());
            return;
        }
        calculateSequenceLength();
        reset();
    }

    /** Sets the emphasis created by the agent */
    public void setNoteEmphasis(TreeMap<Integer, Integer> noteList) {
        noteEmphasisArray = new int[noteList.size()];
        velocityEmphasisArray = new int[noteList.size()];
        int i = 0;
        for (Integer tempInt : noteList.keySet()) {
            noteEmphasisArray[i] = tempInt.intValue();
            velocityEmphasisArray[i] = noteList.get(tempInt).intValue();
            ++i;
        }
        calculateSequenceLength();
        reset();
    }

    public int[] getNoteEmphasisArray() {
        return noteEmphasisArray;
    }

    public int[] getVelocityEmphasisArray() {
        return velocityEmphasisArray;
    }

    public void setSynchToBar(boolean val) {
        synchToBar = val;
        reset();
    }

    public boolean getSynchToBar() {
        return synchToBar;
    }

    @Override
    protected void enabledStatusChanged() {
        if (!this.isEnabled()) reset();
    }

    protected boolean updateTracks(long bufferStart_ppq, long bufferEnd_ppq) throws InvalidMidiDataException {
        for (Track midiTrack : trackMap.values()) {
            if (synchToBar) {
                for (long beat_ppq = Utilities.getFirstBeatInBuffer(bufferStart_ppq); beat_ppq < bufferEnd_ppq; beat_ppq += PPQ_RESOLUTION) {
                    ArrayList<AgentMessage> agMsgArrayList = midiTrack.getAgentMessages(beat_ppq);
                    for (AgentMessage agMsg : agMsgArrayList) {
                        if (agMsg.getType() == AgentMessage.START_BAR) {
                            emphasisCounter = 0;
                            insertionPoint_ppq = beat_ppq;
                            addEmphasis = true;
                        }
                    }
                    if (addEmphasis) {
                        long emphasisPoint_ppq = noteEmphasisArray[emphasisCounter] + insertionPoint_ppq;
                        while (emphasisPoint_ppq >= beat_ppq && emphasisPoint_ppq < beat_ppq + PPQ_RESOLUTION && emphasisPoint_ppq < bufferEnd_ppq && addEmphasis) {
                            emphasiseNotes(emphasisPoint_ppq, velocityEmphasisArray[emphasisCounter], midiTrack);
                            ++emphasisCounter;
                            if (emphasisCounter == noteEmphasisArray.length) {
                                emphasisCounter = 0;
                                addEmphasis = false;
                            }
                            emphasisPoint_ppq = noteEmphasisArray[emphasisCounter] + insertionPoint_ppq;
                        }
                    }
                }
            } else {
                if (insertionPoint_ppq == -1) insertionPoint_ppq = Utilities.getFirstBeatInBuffer(bufferStart_ppq);
                long emphasisPoint_ppq = noteEmphasisArray[emphasisCounter] + insertionPoint_ppq;
                while (emphasisPoint_ppq >= bufferStart_ppq && emphasisPoint_ppq < bufferEnd_ppq) {
                    emphasiseNotes(emphasisPoint_ppq, velocityEmphasisArray[emphasisCounter], midiTrack);
                    ++emphasisCounter;
                    emphasisCounter %= noteEmphasisArray.length;
                    if (emphasisCounter == 0) insertionPoint_ppq += sequenceLength_ppq;
                    emphasisPoint_ppq = noteEmphasisArray[emphasisCounter] + insertionPoint_ppq;
                }
            }
        }
        return true;
    }

    @Override
    protected void reset() {
        emphasisCounter = 0;
        insertionPoint_ppq = -1;
        addEmphasis = false;
    }

    /** Adds the emphasis to the notes */
    private void emphasiseNotes(long beat_ppq, int velocityPercentageChange, Track midiTrack) throws InvalidMidiDataException {
        for (ShortMessage tempShortMsg : midiTrack.getMidiMessages(beat_ppq)) {
            if (tempShortMsg.getCommand() == ShortMessage.NOTE_ON) {
                int newVelocity = tempShortMsg.getData2() * velocityPercentageChange / 100;
                if (newVelocity > maxVelocity) newVelocity = maxVelocity; else if (newVelocity < minVelocity) newVelocity = minVelocity;
                tempShortMsg.setMessage(ShortMessage.NOTE_ON, midiTrack.getChannel(), tempShortMsg.getData1(), newVelocity);
            }
        }
    }

    private void calculateSequenceLength() {
        if (noteEmphasisArray.length > 0) sequenceLength_ppq = ((noteEmphasisArray[noteEmphasisArray.length - 1] / PPQ_RESOLUTION) * PPQ_RESOLUTION) + PPQ_RESOLUTION; else {
            System.out.println("Note emphasis array length is zero!");
            sequenceLength_ppq = 0;
        }
    }
}
