package eu.davidgamez.mas.agents.copy.midi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import org.w3c.dom.Document;
import eu.davidgamez.mas.Constants;
import eu.davidgamez.mas.Util;
import eu.davidgamez.mas.event.BufferListener;
import eu.davidgamez.mas.event.ConnectionListener;
import eu.davidgamez.mas.event.ResetListener;
import eu.davidgamez.mas.exception.MASAgentException;
import eu.davidgamez.mas.exception.MASXmlException;
import eu.davidgamez.mas.gui.MsgHandler;
import eu.davidgamez.mas.midi.Agent;
import eu.davidgamez.mas.midi.Track;

public class CopyAgent extends Agent implements Constants {

    /** Maximum delay of agent */
    public static final int MAX_DELAY_BEATS = 12;

    /** The number of beats that the copied MIDI notes are delayed by */
    private int delay_ppq = 0;

    /** Stores all of the classes that receive reset events from this class. */
    private ArrayList<ConnectionListener> connectionListenerArray = new ArrayList<ConnectionListener>();

    /** The track from which notes are copied */
    private Track fromTrack = null;

    /** The track to which notes are copied */
    private Track toTrack = null;

    /** The id of the from track - used to store id when loading from XML before tracks have been added */
    private String fromTrackLoadID = "";

    /** The id of the to track - used to store id when loading from XML before tracks have been added */
    private String toTrackLoadID = "";

    /** Stores a copy of the delayed notes */
    private TreeMap<Long, ArrayList<ShortMessage>> delayedMessageMap = new TreeMap<Long, ArrayList<ShortMessage>>();

    /** Constructor */
    public CopyAgent() {
        super("Copy", "Copy", "Copy");
    }

    @Override
    public void addTrack(Track mTrack) {
        super.addTrack(mTrack);
        if (mTrack.getID().equals(fromTrackLoadID)) {
            fromTrack = mTrack;
            fromTrackLoadID = "";
        } else if (mTrack.getID().equals(toTrackLoadID)) {
            toTrack = mTrack;
            toTrackLoadID = "";
        }
    }

    @Override
    public void connectionStatusChanged() {
        for (ConnectionListener conListener : connectionListenerArray) {
            conListener.connectionChangeOccurred();
        }
        if (trackMap.isEmpty()) {
            fromTrack = null;
            toTrack = null;
        }
    }

    public void addConnectionListener(ConnectionListener conListener) {
        connectionListenerArray.add(conListener);
    }

    @Override
    public String getXML(String indent) {
        String tmpStr = indent + "<midi_agent>";
        tmpStr += super.getXML(indent + "\t");
        tmpStr += indent + "\t<delay>" + delay_ppq + "</delay>";
        if (fromTrack != null && toTrack != null) {
            tmpStr += indent + "\t<from_track_id>" + fromTrack.getID() + "</from_track_id>";
            tmpStr += indent + "\t<to_track_id>" + toTrack.getID() + "</to_track_id>";
        } else {
            tmpStr += indent + "\t<from_track_id>0</from_track_id>";
            tmpStr += indent + "\t<to_track_id>0</to_track_id>";
        }
        tmpStr += indent + "</midi_agent>";
        return tmpStr;
    }

    @Override
    public void loadFromXML(String xmlStr) throws MASXmlException {
        super.loadFromXML(xmlStr);
        try {
            Document xmlDoc = Util.getXMLDocument(xmlStr);
            delay_ppq = Util.getIntParameter("delay", xmlDoc);
            fromTrackLoadID = Util.getStringParameter("from_track_id", xmlDoc);
            toTrackLoadID = Util.getStringParameter("to_track_id", xmlDoc);
        } catch (Exception ex) {
            System.out.println(xmlStr);
            ex.printStackTrace();
            MsgHandler.error(ex.getMessage());
        }
    }

    /** Sets the source and destination of the copy function */
    public void setCopyConnection(String tmpFromTrackID, String tmpToTrackID) throws MASAgentException {
        if (!trackMap.containsKey(tmpFromTrackID)) throw new MASAgentException("CopyAgent: From track with ID " + tmpFromTrackID + " does not exist.");
        if (!trackMap.containsKey(tmpToTrackID)) throw new MASAgentException("CopyAgent: To track with ID " + tmpToTrackID + " does not exist.");
        this.fromTrack = trackMap.get(tmpFromTrackID);
        this.toTrack = trackMap.get(tmpToTrackID);
    }

    /** Sets the delay value. Throws an exception if it is out of range */
    public void setDelay_beats(int delay_beats) throws MASAgentException {
        if (delay_beats < 0 || delay_beats > MAX_DELAY_BEATS) throw new MASAgentException("CopyAgent: Delay out of range: " + delay_beats);
        delay_ppq = delay_beats * PPQ_RESOLUTION;
    }

    @Override
    protected void reset() {
    }

    @Override
    protected boolean updateTracks(long bufferStart_ppq, long bufferEnd_ppq) throws InvalidMidiDataException {
        if (fromTrack == null || toTrack == null) return true;
        copyBuffer();
        outputDelayedNotes(bufferStart_ppq, bufferEnd_ppq);
        return true;
    }

    /** Copies the notes in the from track into this agent's buffer */
    private void copyBuffer() {
        TreeMap<Long, ArrayList<ShortMessage>> tmpTrackMap = fromTrack.getMidiMessages();
        for (Long key : tmpTrackMap.keySet()) {
            delayedMessageMap.put(key, tmpTrackMap.get(key));
        }
        System.out.println("Track map size: " + tmpTrackMap.size() + "; DELAYED MESSAGE MAP SIZE: " + delayedMessageMap.size());
    }

    /** Outputs delayed notes for this time step and deletes the notes */
    private void outputDelayedNotes(long bufferStart_ppq, long bufferEnd_ppq) throws InvalidMidiDataException {
        long delayBufferStart = bufferStart_ppq - delay_ppq;
        long delayBufferEnd = bufferEnd_ppq - delay_ppq;
        if (delayBufferEnd < 0) return;
        if (delayBufferStart < 0) delayBufferStart = 0;
        System.out.print("Before delayed message map size: " + delayedMessageMap.size());
        SortedMap<Long, ArrayList<ShortMessage>> subMap = delayedMessageMap.subMap(delayBufferStart, delayBufferEnd);
        System.out.print("; sub map size: " + subMap.size());
        int channel = toTrack.getChannel();
        for (Long key : subMap.keySet()) {
            ArrayList<ShortMessage> tmpArrayList = subMap.get(key);
            for (ShortMessage msg : tmpArrayList) {
                msg.setMessage(msg.getCommand(), channel, msg.getData1(), msg.getData2());
                toTrack.addMidiMessage(key + delay_ppq, msg);
            }
        }
        subMap.clear();
        System.out.println("; after delayed message map size: " + delayedMessageMap.size());
    }

    public int getDelay_ppq() {
        return delay_ppq;
    }

    public String getFromTrackID() {
        if (fromTrack == null) return "";
        return fromTrack.getID();
    }

    public String getToTrackID() {
        if (toTrack == null) return "";
        return toTrack.getID();
    }

    public HashMap<String, Track> getTrackMap() {
        return trackMap;
    }
}
