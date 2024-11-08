package eu.davidgamez.mas.midi;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.Vector;
import eu.davidgamez.mas.event.EventRouter;
import eu.davidgamez.mas.event.TempoEvent;
import eu.davidgamez.mas.event.TempoListener;
import eu.davidgamez.mas.event.TransportListener;
import eu.davidgamez.mas.event.NoteEventListener;
import eu.davidgamez.mas.*;

/**----------------------------  MIDIInputHandler --------------------------------
    Receives MIDI events and passes them on to classes that have registered to
      listen to them. This class handles both controllers and synchronization signals.
   -------------------------------------------------------------------------------*/
public class MIDIInputHandler implements Receiver, Constants {

    /** Each controller number has an array list that holds the listeners for
	 	that controller number. */
    private ArrayList<ControllerEventListener>[] controllerEventListeners = new ArrayList[128];

    /** List of listeners for note events */
    private ArrayList<NoteEventListener> noteEventListeners = new ArrayList<NoteEventListener>();

    private int midiClockCount = 0;

    private long midiClockTime = 0;

    /** Constructor */
    public MIDIInputHandler() {
        for (int i = 0; i <= 127; ++i) controllerEventListeners[i] = new ArrayList<ControllerEventListener>();
    }

    /**  Called when the receiver is closed.*/
    public void close() {
        System.out.println("Closing MIDIInputHandler");
    }

    /**Called when the receiver receives a MIDI message. */
    public void send(MidiMessage message, long timeStamp) {
        if (message instanceof ShortMessage) {
            ShortMessage shrtMsg = (ShortMessage) message;
            if (shrtMsg.getCommand() == ShortMessage.NOTE_ON || shrtMsg.getCommand() == ShortMessage.NOTE_OFF) {
                try {
                    if (shrtMsg.getCommand() == ShortMessage.NOTE_ON && shrtMsg.getData2() == 0) shrtMsg.setMessage(ShortMessage.NOTE_OFF, shrtMsg.getChannel(), shrtMsg.getData1(), 0);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                for (NoteEventListener noteEventListnr : noteEventListeners) noteEventListnr.noteEventOcccurred(shrtMsg);
            } else if (shrtMsg.getCommand() == ShortMessage.CONTROL_CHANGE) {
                for (ControllerEventListener contEventList : controllerEventListeners[((ShortMessage) message).getData1()]) contEventList.controlChange((ShortMessage) message);
            } else if (message.getStatus() == ShortMessage.TIMING_CLOCK && Globals.getMasterSyncMode() == MIDI_SYNC) {
                ++midiClockCount;
                if (midiClockCount == 24) {
                    double newTempo = (double) (60000000000l / (System.nanoTime() - midiClockTime));
                    EventRouter.tempoActionPerformed(new TempoEvent(newTempo));
                    midiClockCount = 0;
                    midiClockTime = System.nanoTime();
                }
            } else if (message.getStatus() == ShortMessage.START && Globals.getMasterSyncMode() == MIDI_SYNC) {
                midiClockCount = 0;
                midiClockTime = System.nanoTime();
                EventRouter.playActionPerformed();
            } else if (message.getStatus() == ShortMessage.STOP && Globals.getMasterSyncMode() == MIDI_SYNC) {
                EventRouter.stopActionPerformed();
            }
        }
    }

    /** Adds a listener for controller events of a particular number */
    public void addControllerEventListener(ControllerEventListener listener, int[] controllerNumbers) {
        for (int i = 0; i < controllerNumbers.length; ++i) controllerEventListeners[controllerNumbers[i]].add(listener);
    }

    /** Removes the specified listener from all controller numbers */
    public void removeControllerEventListener(ControllerEventListener listener) {
        for (int i = 0; i <= 127; ++i) {
            controllerEventListeners[i].remove(listener);
        }
    }

    /** Adds a listener for note events of a particular number */
    public void addNoteEventListener(NoteEventListener listener) {
        noteEventListeners.add(listener);
    }

    /** Removes the specified listener for note events */
    public void removeNoteEventListener(NoteEventListener listener) {
        noteEventListeners.remove(listener);
    }

    public int[] getControllerNumbers(ControllerEventListener listener) {
        Vector numberVector = new Vector();
        for (int i = 0; i <= 127; ++i) {
            if (controllerEventListeners[i].contains(listener)) numberVector.add(new Integer(i));
        }
        int[] intArray = new int[numberVector.size()];
        int counter = 0;
        for (Object obj : numberVector) {
            intArray[counter] = ((Integer) obj).intValue();
            ++counter;
        }
        return intArray;
    }
}
