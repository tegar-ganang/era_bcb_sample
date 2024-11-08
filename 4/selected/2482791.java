package eu.davidgamez.mas.agents.prolongnotes.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import org.w3c.dom.Document;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.ArrayList;
import eu.davidgamez.mas.Util;
import eu.davidgamez.mas.exception.MASXmlException;
import eu.davidgamez.mas.gui.MsgHandler;
import eu.davidgamez.mas.midi.Agent;
import eu.davidgamez.mas.midi.Track;

public class ProlongNotes extends Agent {

    private HashMap<String, HashMap<Integer, Boolean>> noteOffHashMap = new HashMap<String, HashMap<Integer, Boolean>>();

    private boolean prolongNotes = false;

    public ProlongNotes() {
        super("Prolong Notes", "Prolong Notes", "ProlongNotes");
    }

    protected boolean updateTracks(long bufferStart_ppq, long bufferEnd_ppq) throws InvalidMidiDataException {
        if (!prolongNotes && !noteOffHashMap.isEmpty()) {
            addNoteOffMessages(bufferStart_ppq);
            return true;
        } else if (!prolongNotes) {
            return true;
        }
        for (Track midiTrack : trackMap.values()) {
            TreeMap<Long, ArrayList<ShortMessage>> trackBufferMap = midiTrack.getMidiMessages();
            for (Long bufferKey : trackBufferMap.keySet()) {
                ArrayList<ShortMessage> tempArrayList = (ArrayList<ShortMessage>) trackBufferMap.get(bufferKey);
                for (int i = 0; i >= 0 && i < tempArrayList.size(); ++i) {
                    ShortMessage tempShortMessage = tempArrayList.get(i);
                    if (tempShortMessage.getCommand() == ShortMessage.NOTE_OFF) {
                        noteOffHashMap.get(midiTrack.getID()).put(tempShortMessage.getData1(), new Boolean(true));
                        tempArrayList.remove(i);
                        i--;
                    }
                }
            }
        }
        return true;
    }

    public void enabledStatusChanged() {
    }

    public void connectionStatusChanged() {
        HashMap<String, Boolean> currentTrackHashMap = new HashMap<String, Boolean>();
        for (Track midiTrack : trackMap.values()) {
            currentTrackHashMap.put(midiTrack.getID(), new Boolean(true));
            if (!noteOffHashMap.containsKey(midiTrack.getID())) noteOffHashMap.put(midiTrack.getID(), new HashMap<Integer, Boolean>());
        }
        for (String trackKey : noteOffHashMap.keySet()) {
            if (!currentTrackHashMap.containsKey(trackKey)) noteOffHashMap.remove(trackKey);
        }
    }

    public void loadFromXML(String xmlStr) throws MASXmlException {
        super.loadFromXML(xmlStr);
        try {
            Document xmlDoc = Util.getXMLDocument(xmlStr);
            setProlongNotes(Util.getBoolParameter("prolong_notes", xmlDoc));
        } catch (Exception ex) {
            System.out.println(xmlStr);
            ex.printStackTrace();
            MsgHandler.error(ex.getMessage());
        }
    }

    public String getXML(String indent) {
        String tmpStr = "";
        tmpStr += indent + "<midi_agent>";
        tmpStr += super.getXML(indent + "\t");
        tmpStr += indent + "\t<prolong_notes>" + prolongNotes + "</prolong_notes>";
        tmpStr += indent + "</midi_agent>";
        return tmpStr;
    }

    public void setProlongNotes(boolean pn) {
        prolongNotes = pn;
    }

    public boolean getProlongNotes() {
        return prolongNotes;
    }

    private void addNoteOffMessages(long bufferStart_ppq) throws InvalidMidiDataException {
        for (Track midiTrack : trackMap.values()) {
            HashMap<Integer, Boolean> tempHashMap = noteOffHashMap.get(midiTrack.getID());
            if (tempHashMap != null) {
                for (Integer tempInteger : tempHashMap.keySet()) {
                    ShortMessage off = new ShortMessage();
                    off.setMessage(ShortMessage.NOTE_OFF, midiTrack.getChannel(), tempInteger.intValue(), 0);
                    midiTrack.addMidiMessage(bufferStart_ppq, off);
                }
            }
        }
        for (String trackKey : noteOffHashMap.keySet()) {
            noteOffHashMap.get(trackKey).clear();
        }
    }

    @Override
    protected void reset() {
    }
}
