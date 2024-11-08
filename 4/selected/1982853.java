package eu.davidgamez.mas.midi;

import java.util.HashMap;
import javax.sound.midi.*;
import org.w3c.dom.Document;
import java.rmi.server.UID;
import eu.davidgamez.mas.Util;
import eu.davidgamez.mas.exception.MASXmlException;
import eu.davidgamez.mas.gui.MsgHandler;

public abstract class Agent {

    private String name = "Untitled";

    private String agentDescription = "No Description";

    private String agentType = null;

    private boolean isEnabled = true;

    /** Unique id of the agent */
    private String uniqueID;

    /** Holds all of the tracks that the agent is connected to */
    protected HashMap<String, Track> trackMap = new HashMap<String, Track>();

    /** Constructor */
    public Agent(String agName, String agentDescription, String agentType) {
        this.name = agName;
        this.agentDescription = agentDescription;
        this.agentType = agentType;
        uniqueID = new UID().toString();
    }

    /** Called on agents when they are to look at the buffers in the tracks that they are monitoring
      and update it if appropriate. The agent returns true when it has completed all of its
      operations and false if it wants to be called again to do more work before the buffer advances */
    protected abstract boolean updateTracks(long bufferStart_ppq, long bufferEnd_ppq) throws InvalidMidiDataException;

    /** Called when the enabled status of an agent is changed */
    protected void enabledStatusChanged() {
    }

    /** Called when the connection status of an agent is changed */
    public void connectionStatusChanged() {
    }

    /** Resets the agent */
    protected abstract void reset();

    /** Returns an XML string describing the state of the agent
    	Should be called by the agent class that inherits from this, which should wrap
    	this XML in <agent_midi> tags. */
    public String getXML(String indent) {
        String agentStr = indent + "<name>" + name + "</name>";
        agentStr += indent + "<type>" + agentType + "</type>";
        agentStr += indent + "<class>" + getClass().getCanonicalName() + "</class>";
        agentStr += indent + "<enabled>" + isEnabled + "</enabled>";
        agentStr += indent + "<id>" + getID() + "</id>";
        return agentStr;
    }

    /** Loads the parameters of the agent from the supplied XML string */
    public void loadFromXML(String xmlStr) throws MASXmlException {
        try {
            Document xmlDoc = Util.getXMLDocument(xmlStr);
            setName(Util.getStringParameter("name", xmlDoc));
            setID(Util.getStringParameter("id", xmlDoc));
            setEnabled(Util.getBoolParameter("enabled", xmlDoc));
        } catch (Exception ex) {
            System.out.println(xmlStr);
            ex.printStackTrace();
            MsgHandler.error(ex.getMessage());
        }
    }

    public void addTrack(Track mTrack) {
        trackMap.put(mTrack.getID(), mTrack);
        connectionStatusChanged();
    }

    public void deleteTrack(String trackID) {
        trackMap.remove(trackID);
        connectionStatusChanged();
    }

    public void print() {
        System.out.println("Agent Name: " + name);
        for (String tmpTrackUID : trackMap.keySet()) {
            System.out.println("\tTrack name: " + trackMap.get(tmpTrackUID).getName());
            System.out.println("\tTrack ID: " + tmpTrackUID);
            System.out.println("\tTrack channel: " + (trackMap.get(tmpTrackUID).getChannel() + 1));
        }
    }

    /** Returns the unique id of the agent */
    public String getID() {
        return uniqueID;
    }

    private void setID(String newID) {
        this.uniqueID = newID;
    }

    public boolean isConnected() {
        return !trackMap.isEmpty();
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public int numberOfConnections() {
        return trackMap.size();
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        enabledStatusChanged();
    }

    public String getName() {
        return name;
    }

    public void setName(String agentName) {
        this.name = agentName;
    }

    public String getAgentType() {
        return agentType;
    }

    public String getAgentDescription() {
        return agentDescription;
    }
}
