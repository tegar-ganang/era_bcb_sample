package eu.davidgamez.mas.agents.pitchshifter.midi;

import java.util.Iterator;
import java.util.Vector;
import java.util.TreeMap;
import java.util.ArrayList;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import org.w3c.dom.Document;
import eu.davidgamez.mas.Util;
import eu.davidgamez.mas.event.TrackBufferUpdateListener;
import eu.davidgamez.mas.exception.MASXmlException;
import eu.davidgamez.mas.gui.MsgHandler;
import eu.davidgamez.mas.midi.Agent;
import eu.davidgamez.mas.midi.Track;

public class PitchShifter extends Agent {

    int pitchShiftAmount = 0;

    ArrayList<TrackBufferUpdateListener> trackBufferUpdateListenerArray = new ArrayList<TrackBufferUpdateListener>();

    public PitchShifter() {
        super("Pitch Shifter", "Pitch Shifter", "PitchShifter");
    }

    public String getXML(String indent) {
        String tmpStr = indent + "<midi_agent>";
        tmpStr += super.getXML(indent + "\t");
        tmpStr += indent + "\t<pitch_shift_amount>" + pitchShiftAmount + "</pitch_shift_amount>";
        tmpStr += indent + "</midi_agent>";
        return tmpStr;
    }

    public void loadFromXML(String xmlStr) throws MASXmlException {
        super.loadFromXML(xmlStr);
        try {
            Document xmlDoc = Util.getXMLDocument(xmlStr);
            pitchShiftAmount = Util.getIntParameter("pitch_shift_amount", xmlDoc);
        } catch (Exception ex) {
            System.out.println(xmlStr);
            ex.printStackTrace();
            MsgHandler.error(ex.getMessage());
        }
    }

    protected boolean updateTracks(long bufferStart, long bufferLength) throws InvalidMidiDataException {
        for (TrackBufferUpdateListener tbul : trackBufferUpdateListenerArray) tbul.trackBufferUpdate();
        for (Track midiTrack : trackMap.values()) {
            TreeMap<Long, ArrayList<ShortMessage>> midiMessageArrays = midiTrack.getMidiMessages();
            for (ArrayList<ShortMessage> messageArray : midiMessageArrays.values()) {
                for (ShortMessage tempMessage : messageArray) {
                    if (tempMessage.getCommand() == ShortMessage.NOTE_ON || tempMessage.getCommand() == ShortMessage.NOTE_OFF) {
                        int newNote = tempMessage.getData1() + pitchShiftAmount;
                        if (newNote > 127) newNote = 127; else if (newNote < 0) newNote = 0;
                        tempMessage.setMessage(tempMessage.getCommand(), tempMessage.getChannel(), newNote, tempMessage.getData2());
                    }
                }
            }
        }
        return true;
    }

    public void setPitchShiftAmount(int amnt) {
        pitchShiftAmount = amnt;
    }

    public int getPitchShiftAmount() {
        return pitchShiftAmount;
    }

    public void addTrackBufferUpdateListener(TrackBufferUpdateListener tbul) {
        trackBufferUpdateListenerArray.add(tbul);
    }

    public void removeTrackBufferUpdateListener(TrackBufferUpdateListener tbul) {
        trackBufferUpdateListenerArray.remove(tbul);
    }

    @Override
    protected void reset() {
    }
}
