package eu.davidgamez.mas.midi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPacket;
import com.illposed.osc.OSCPortOut;
import eu.davidgamez.mas.Constants;
import eu.davidgamez.mas.Globals;
import eu.davidgamez.mas.event.EventRouter;
import eu.davidgamez.mas.exception.MASException;

public class MIDIPlayer extends Thread implements Constants {

    /** Used to exit from run loop */
    private boolean stop = true;

    /** Records if an error has occurred */
    private boolean error = false;

    /** Message associated with an error */
    private String errorMessage = "";

    /** List of receivers that receive MIDI messages */
    ArrayList<Receiver> receiverArrayList;

    /** List of receivers that receive MIDI messages */
    ArrayList<OSCPortOut> oscPortOutList;

    /** Run method of thread */
    public void run() {
        System.out.println("Starting MIDIPlayer.");
        Globals.setPlaying(true);
        receiverArrayList = MIDIDeviceManager.getReceiverArrayList();
        oscPortOutList = OSCDeviceManager.getOSCPortOutArrayList();
        stop = false;
        clearError();
        if ((receiverArrayList == null || receiverArrayList.isEmpty()) && (oscPortOutList == null || oscPortOutList.isEmpty())) setError("No receivers set, cannot play without receivers");
        try {
            play();
        } catch (MASException ex) {
            setError(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            setError(ex.getMessage());
        }
        stop = true;
        Globals.setPlaying(false);
        EventRouter.stopActionPerformed();
        System.out.println("Stopping MIDIPlayer");
    }

    public void clearError() {
        error = false;
        errorMessage = "";
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isError() {
        return error;
    }

    public boolean isPlaying() {
        return !stop;
    }

    public void stopThread() {
        stop = true;
    }

    /** Play works by loading the next list of midi events
	 * and waiting until they should be played.
	 * The time stamp associated with a message is in ticks, starting from the start of play.
	 */
    private void play() throws MASException, InterruptedException, IOException {
        ArrayList<ShortMessage> tmpArrayList;
        TreeMap<Long, ArrayList<ShortMessage>> playBuffer;
        this.setPriority(MAX_PRIORITY);
        long bufferLength_ticks = Buffer.getLength_ticks();
        long lastMsg_ticks = 0;
        long startBuffer_ticks;
        long endBuffer_ns = System.nanoTime();
        long delay_ms;
        while (!stop) {
            double nanoSecPerTick = Globals.getNanoSecPerTick();
            playBuffer = Buffer.getPlayBuffer();
            startBuffer_ticks = bufferLength_ticks * Buffer.getPlayBufferCount();
            delay_ms = Math.round(((startBuffer_ticks - lastMsg_ticks) * nanoSecPerTick - (System.nanoTime() - endBuffer_ns)) / 1000000);
            if (delay_ms > 0) sleep(delay_ms);
            lastMsg_ticks = startBuffer_ticks;
            for (Long msgPosition_ticks : playBuffer.keySet()) {
                tmpArrayList = playBuffer.get(msgPosition_ticks);
                delay_ms = Math.round(((msgPosition_ticks - lastMsg_ticks) * nanoSecPerTick) / 1000000);
                if (delay_ms > 0) sleep(delay_ms);
                for (ShortMessage midiMsg : tmpArrayList) {
                    for (Receiver tmpRcv : receiverArrayList) {
                        tmpRcv.send(midiMsg, -1);
                    }
                    for (OSCPortOut tmpOscPortOut : oscPortOutList) {
                        tmpOscPortOut.send(getOSCPacket(midiMsg));
                    }
                }
                lastMsg_ticks = msgPosition_ticks;
            }
            endBuffer_ns = System.nanoTime();
            Buffer.advancePlayBuffer();
        }
    }

    private OSCPacket getOSCPacket(ShortMessage midiMsg) {
        Object packetContents[] = new Object[2];
        if (midiMsg.getCommand() == 144) {
            packetContents[0] = new Integer(midiMsg.getData1());
            packetContents[1] = new Float(midiMsg.getData2() / 127.0);
        } else if (midiMsg.getCommand() == 128) {
            packetContents[0] = new Integer(midiMsg.getData1());
            packetContents[1] = new Float(0.0);
        } else {
            setError("Unrecognized command: " + midiMsg.getCommand());
        }
        return new OSCMessage("/mas/channel" + (midiMsg.getChannel() + 1), packetContents);
    }

    /** Sets the thread into the error state, sets the thread to stop
	 * and stores the error message 
	 * @param errorMessage
	 */
    private void setError(String errorMessage) {
        this.errorMessage = errorMessage;
        stop = true;
        error = true;
        System.out.println("MIDIPlayer Error: " + errorMessage);
    }
}
