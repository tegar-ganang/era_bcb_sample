package eu.davidgamez.mas.midi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import javax.sound.midi.*;
import org.w3c.dom.Document;
import java.io.BufferedWriter;
import java.rmi.server.UID;
import eu.davidgamez.mas.*;
import eu.davidgamez.mas.event.BufferListener;
import eu.davidgamez.mas.event.EventRouter;
import eu.davidgamez.mas.gui.MsgHandler;

public class Track implements BufferListener, Constants {

    private String name = "Untitled";

    private int midiChannel = DEFAULT_MIDI_CHANNEL;

    private TreeMap<Long, ArrayList<ShortMessage>> midiMessageMap = new TreeMap<Long, ArrayList<ShortMessage>>();

    private TreeMap<Long, ArrayList<AgentMessage>> agentMessageMap = new TreeMap<Long, ArrayList<AgentMessage>>();

    private TreeMap<Long, ArrayList<ShortMessage>>[] globalBufferArray;

    private String uniqueID;

    private boolean mute = false;

    /** Static map holding all of the tracks that are currently soloed */
    private static HashMap<Track, Boolean> soloHashMap = new HashMap<Track, Boolean>();

    /** Standard constructor */
    public Track() {
        uniqueID = new UID().toString();
        EventRouter.addBufferListener(this);
    }

    /** Constructor when loading from XML */
    public Track(String xmlStr) {
        loadFromXML(xmlStr);
        EventRouter.addBufferListener(this);
    }

    /** Called just before the Buffer advances to the next load buffer */
    public void startLoadBufferAdvanceOccurred(long bufferCount) {
        Buffer.addMidiMessages(midiMessageMap);
        agentMessageMap.clear();
        midiMessageMap.clear();
    }

    /** Called when the advance to the next buffer is complete */
    public void endLoadBufferAdvanceOccurred(long bufferCount) {
    }

    /** Called when play is started to set buffer start and end to the correct initial values */
    public void reset() {
        agentMessageMap.clear();
        midiMessageMap.clear();
    }

    public void playBufferAdvanceOccurred(long bufferCount) {
    }

    public void trackAdvanceOccurred(long beatCount) {
    }

    public void addAgentMessage(long messageTime, AgentMessage agMsg) throws InvalidMidiDataException {
        if (mute) return;
        if (!soloHashMap.isEmpty() && !soloHashMap.containsKey(this)) return;
        if (messageTime < Buffer.getLoadStart_ticks() || messageTime >= Buffer.getLoadEnd_ticks()) throw new InvalidMidiDataException("Trying to add agent message outside buffer. Message time = " + messageTime + " bufferStart = " + Buffer.getLoadStart_ticks() + " bufferEnd = " + Buffer.getLoadEnd_ticks());
        Long messageKey = new Long(messageTime);
        if (agentMessageMap.containsKey(messageKey)) {
            ArrayList<AgentMessage> tempArrayList = agentMessageMap.get(messageKey);
            tempArrayList.add(agMsg);
        } else {
            ArrayList tempArrayList = new ArrayList<AgentMessage>();
            tempArrayList.add(agMsg);
            agentMessageMap.put(messageKey, tempArrayList);
        }
    }

    public void addMidiMessage(long messageTime, ShortMessage shortMsg) throws InvalidMidiDataException {
        if (mute) return;
        if (!soloHashMap.isEmpty() && !soloHashMap.containsKey(this)) return;
        if (messageTime < Buffer.getLoadStart_ticks() || messageTime >= Buffer.getLoadEnd_ticks()) {
            Utilities.printShortMessage(shortMsg);
            throw new InvalidMidiDataException("Trying to add short message outside buffer. Message time = " + messageTime + " bufferStart = " + Buffer.getLoadStart_ticks() + " bufferEnd = " + Buffer.getLoadEnd_ticks());
        }
        Long messageKey = new Long(messageTime);
        if (midiMessageMap.containsKey(messageKey)) {
            ArrayList<ShortMessage> tempArrayList = midiMessageMap.get(messageKey);
            tempArrayList.add(shortMsg);
        } else {
            ArrayList<ShortMessage> tempArrayList = new ArrayList<ShortMessage>();
            tempArrayList.add(shortMsg);
            midiMessageMap.put(messageKey, tempArrayList);
        }
    }

    /** Removes a short MIDI message */
    public boolean removeMidiMessage(Long messageTime, ShortMessage shortMessage) {
        if (!midiMessageMap.containsKey(messageTime)) return false;
        if (!midiMessageMap.get(messageTime).contains(shortMessage)) return false;
        midiMessageMap.get(messageTime).remove(shortMessage);
        return true;
    }

    /** Returns an XML string with the track's parameters */
    public String getXML(String indent) {
        String trackStr = indent + "<midi_track>";
        trackStr += indent + "\t<name>" + name + "</name>";
        trackStr += indent + "\t<id>" + uniqueID + "</id>";
        trackStr += indent + "\t<channel>" + midiChannel + "</channel>";
        trackStr += indent + "</midi_track>";
        return trackStr;
    }

    /** Loads track parameters from the XML string. */
    public void loadFromXML(String xmlStr) {
        try {
            Document xmlDoc = Util.getXMLDocument(xmlStr);
            setName(Util.getStringParameter("name", xmlDoc));
            setUniqueID(Util.getStringParameter("id", xmlDoc));
            setChannel(Util.getIntParameter("channel", xmlDoc));
        } catch (Exception ex) {
            System.out.println(xmlStr);
            ex.printStackTrace();
            MsgHandler.error(ex.getMessage());
        }
    }

    public ArrayList<AgentMessage> getAgentMessages(long time) {
        Long messageKey = new Long(time);
        if (agentMessageMap.containsKey(messageKey)) {
            return (ArrayList<AgentMessage>) agentMessageMap.get(messageKey);
        }
        return new ArrayList<AgentMessage>();
    }

    public int getChannel() {
        return midiChannel;
    }

    public TreeMap<Long, ArrayList<AgentMessage>> getAgentMessages() {
        return agentMessageMap;
    }

    public TreeMap<Long, ArrayList<ShortMessage>> getMidiMessages() {
        return midiMessageMap;
    }

    public String getName() {
        return name;
    }

    public ArrayList<ShortMessage> getMidiMessages(long time) {
        Long messageKey = new Long(time);
        if (midiMessageMap.containsKey(messageKey)) {
            return (ArrayList<ShortMessage>) midiMessageMap.get(messageKey);
        }
        return new ArrayList<ShortMessage>();
    }

    public String getID() {
        return uniqueID;
    }

    public boolean muted() {
        return mute;
    }

    /** Returns true if this track is soloed */
    public boolean soloed() {
        if (soloHashMap.containsKey(this)) return true;
        return false;
    }

    /** Sets the soloed state of this track */
    public void setSoloed(boolean soloed) {
        if (soloed) soloHashMap.put(this, new Boolean(true)); else soloHashMap.remove(this);
    }

    public void setChannel(int chNumber) {
        midiChannel = chNumber;
    }

    /** Sets all of the MIDI messages  */
    public void setMidiMessageMap(TreeMap<Long, ArrayList<ShortMessage>> newMsgMap) {
        this.midiMessageMap = newMsgMap;
    }

    public void setMuted(boolean mtd) {
        mute = mtd;
    }

    public void setName(String n) {
        name = n;
    }

    public void setUniqueID(String uid) {
        this.uniqueID = uid;
    }

    public void printTrack() {
        System.out.println("Track name: " + name + "; Midi channel: " + midiChannel);
    }
}
