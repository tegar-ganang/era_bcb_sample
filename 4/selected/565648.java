package eu.davidgamez.mas.midi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPacket;
import com.illposed.osc.OSCPortOut;
import eu.davidgamez.mas.Globals;
import eu.davidgamez.mas.exception.MASException;

/** Class that runs as a separate thread to manage agents as they generate
     the MIDI notes. */
class AgentPlayer extends Thread {

    /** MIDI sequencer. Used to start MIDI play when buffer is full */
    MASSequencer masSequencer;

    /** Controls run method */
    private boolean stop = true;

    /** Records if there has been an error */
    private boolean error = false;

    /** Error message when an error has occurred */
    private String errorMessage = "";

    /** Records when agents have finished updating the buffer */
    HashMap<String, Boolean> agentsCompleteMap = new HashMap<String, Boolean>();

    /** Declare boolean once to save recreating it for every agent */
    Boolean updateComplete = new Boolean(true);

    /** Records time at which play started  - used for OSC messages */
    private long playStartTime_ms;

    private long packetPlayTime_ms;

    private ArrayList<ShortMessage> tmpArrayList;

    private TreeMap<Long, ArrayList<ShortMessage>> loadedBuffer;

    /** List of receivers that receive MIDI messages */
    ArrayList<OSCPortOut> oscPortOutList;

    /** Constructor */
    public AgentPlayer(MASSequencer masSequencer) {
        this.masSequencer = masSequencer;
    }

    /** Returns error message */
    public String getErrorMessage() {
        return errorMessage;
    }

    /** Returns true if an error has occurred */
    public boolean isError() {
        return error;
    }

    /** Returns true if the player is in its run method */
    public boolean isPlaying() {
        return !stop;
    }

    /** Resets the state of the class - currently only clears the errors */
    public void reset() {
        clearError();
    }

    /** Main run method inherited from thread. */
    public void run() {
        stop = false;
        this.setPriority(MIN_PRIORITY);
        clearError();
        Buffer.reset();
        oscPortOutList = OSCDeviceManager.getOSCPortOutArrayList();
        AgentHolder.resetAgents();
        TrackHolder.resetTracks();
        long headStart_buffers = Buffer.getHeadStart_buffers();
        int loadBufferCount;
        playStartTime_ms = System.currentTimeMillis();
        try {
            for (int i = 0; i < headStart_buffers; ++i) {
                agentBufferUpdate();
                Buffer.advanceLoadBuffer();
            }
            masSequencer.play();
            while (!stop) {
                agentBufferUpdate();
                Buffer.advanceLoadBuffer();
                loadBufferCount = Buffer.getLoadBufferCount();
                while (!stop && (loadBufferCount - headStart_buffers >= Buffer.getPlayBufferCount())) {
                    sleep(50);
                }
            }
        } catch (Exception ex) {
            setError(ex.getMessage());
        }
        stop = true;
    }

    /** Causes thread to exit run method and terminate */
    public void stopThread() {
        stop = true;
    }

    /**  Update each agent requesting it to update the tracks that it is watching. 
	 	FIXME: TRACK RETURN VALUE AND CALL AGAIN IF NECESSARY. */
    private void agentBufferUpdate() throws InvalidMidiDataException {
        HashMap<String, Agent> agentMap = AgentHolder.getAgentMap();
        ArrayList<String> agentOrderList = AgentHolder.getAgentOrderList();
        agentsCompleteMap.clear();
        while (agentsCompleteMap.size() != agentMap.size()) {
            for (String agentID : agentOrderList) {
                if (agentMap.get(agentID).isEnabled()) {
                    if (agentMap.get(agentID).updateTracks(Buffer.getLoadStart_ticks(), Buffer.getLoadEnd_ticks())) agentsCompleteMap.put(agentID, updateComplete);
                } else {
                    agentsCompleteMap.put(agentID, updateComplete);
                }
            }
        }
    }

    /** Clears the error state */
    private void clearError() {
        errorMessage = "";
        error = false;
    }

    /** Sends OSC messages to all receivers */
    private void sendOSCMessages() throws IOException, MASException {
        loadedBuffer = Buffer.getLoadedBuffer();
        double nanoSecPerTick = Globals.getNanoSecPerTick();
        for (Long msgPosition_ticks : loadedBuffer.keySet()) {
            tmpArrayList = loadedBuffer.get(msgPosition_ticks);
            packetPlayTime_ms = Math.round((msgPosition_ticks * nanoSecPerTick) / 1000000) + playStartTime_ms;
            OSCBundle oscBundle = new OSCBundle(new Date(packetPlayTime_ms));
            for (ShortMessage midiMsg : tmpArrayList) {
                oscBundle.addPacket(getOSCPacket(midiMsg));
            }
            for (OSCPortOut tmpOscPortOut : oscPortOutList) {
                tmpOscPortOut.send(oscBundle);
            }
        }
    }

    private OSCPacket getOSCPacket(ShortMessage midiMsg) {
        Object packetContents[] = new Object[2];
        if (midiMsg.getCommand() == 144) {
            packetContents[0] = new Integer(midiMsg.getData1());
            packetContents[1] = new Integer(midiMsg.getData2());
        } else if (midiMsg.getCommand() == 128) {
            packetContents[0] = new Integer(midiMsg.getData1());
            packetContents[1] = new Integer(0);
        } else {
            setError("Unrecognized command: " + midiMsg.getCommand());
        }
        return new OSCMessage("/mas/channel" + (midiMsg.getChannel() + 1), packetContents);
    }

    /** Sets the error state */
    private void setError(String errorMessage) {
        this.errorMessage = errorMessage;
        error = true;
        stop = true;
        System.out.println("AgentPlayer Error: " + errorMessage);
    }
}
