package com.frinika.sequencer;

import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import javax.sound.midi.*;
import com.frinika.sequencer.model.EditHistoryContainer;

/**
 * An extended version of the Track class in javax.sound.midi. The purpose of the extension
 * is to make the Sequencer player able to request all events for a given tick, rather than
 * having to perform a binary search, or keeping a track index to get them.
 * 
 * For the track mapping to remain consistent - it is important NOT to modify the MidiEvent
 * objects separately. If a MIDI event needs to be changed, it should be removed and then
 * added again to this Track.
 * 
 * @author Peter Johan Salomonsen
 */
public class FrinikaTrackWrapper {

    private HashMap<Long, Vector<MidiEvent>> tickMap = new HashMap<Long, Vector<MidiEvent>>();

    private HashMap<Integer, SortedMap<Long, Integer>> controllerMap = new HashMap<Integer, SortedMap<Long, Integer>>();

    private SortedMap<Long, MidiMessage> tempoMap = new TreeMap<Long, MidiMessage>();

    Track track;

    Vector<MidiMessage> controllerStateMessages;

    private FrinikaSequence sequence;

    /**
     * Indicates that the channel defined in the multievents should not be
     * overridden by the channel set in this track
     */
    public static final int CHANNEL_FROM_EVENT = -1;

    /**
     * Target MIDI out device
     */
    MidiDevice midiDevice = null;

    /**
     * Target midi cannel;
     */
    int midiChannel = CHANNEL_FROM_EVENT;

    /**
     * Construct a FrinikaTrackWrapper to encapsulate a JavaSound track. 
     * @param track The JavaSound track to wrap around
     */
    FrinikaTrackWrapper(Track track) {
        this.track = track;
        for (int n = 0; n < track.size(); n++) {
            MidiEvent event = track.get(n);
            addEventToTickMap(event);
        }
        controllerStateMessages = new Vector<MidiMessage>();
        controllerStateMessages.ensureCapacity(128);
    }

    /**
	 * 
	 * clear this track of all events 
	 * except the "META END OF TRACK"
	 *
	 * (PJL  tempo track rebuild)
	 */
    public void clear() {
        int N = track.size();
        for (int n = N - 2; n >= 0; n--) {
            MidiEvent event = track.get(n);
            track.remove(event);
        }
        tickMap.clear();
        controllerMap.clear();
    }

    /**
     * Add a MIDI event to the tickMap so that the TrackWrapper can keep track on which events
     * are located on what tick.
     * @param event
     */
    private void addEventToTickMap(MidiEvent event) {
        if (!tickMap.containsKey(event.getTick())) {
            tickMap.put(event.getTick(), new Vector<MidiEvent>());
        }
        tickMap.get(event.getTick()).add(event);
        addToControllerMap(event);
    }

    /**
     * Controller events will be added to the controlmap, so that the trackwrapper can keep track of
     * controller state at any point of time in the sequence
     * @param event
     */
    private void addToControllerMap(MidiEvent event) {
        try {
            ShortMessage shm = (ShortMessage) event.getMessage();
            if (shm.getCommand() == ShortMessage.CONTROL_CHANGE) {
                int ccKey = ((shm.getChannel() & 0xf) << 8) | (shm.getData1() & 0xff);
                SortedMap<Long, Integer> ccValues;
                ccValues = controllerMap.get(ccKey);
                if (ccValues == null) {
                    ccValues = new TreeMap<Long, Integer>();
                    controllerMap.put(ccKey, ccValues);
                }
                ccValues.put(event.getTick(), shm.getData2());
            }
        } catch (Exception e) {
        }
    }

    /**
     * Remove an event from the controllerMap
     * @param event
     */
    private void removeFromControllerMap(MidiEvent event) {
        try {
            ShortMessage shm = (ShortMessage) event.getMessage();
            if (shm.getCommand() == ShortMessage.CONTROL_CHANGE) {
                int ccKey = ((shm.getChannel() & 0xf) << 8) | (shm.getData1() & 0xff);
                controllerMap.get(ccKey).remove(event.getTick());
            }
        } catch (Exception e) {
        }
    }

    /**
     * Add a MidiEvent to the track - note that you should normally use MultiEvents instead of MidiEvents
     * @param event
     * @return
     */
    public synchronized boolean add(MidiEvent event) {
        if (track.add(event)) {
            addEventToTickMap(event);
            return true;
        } else return false;
    }

    /**
     * Remove a MidiEvent from the track - Note that you should normally use MultiEvents instead of MidiEvents
     * @param event
     * @return
     */
    public synchronized boolean remove(MidiEvent event) {
        if (track.remove(event)) {
            Vector<MidiEvent> tt = tickMap.get(event.getTick());
            if (tt == null) {
                System.out.println("oops tickMap did not have event vector ");
            }
            tt.remove(event);
            removeFromControllerMap(event);
            return true;
        } else return false;
    }

    /**
     * Return number of MidiEvents in track
     * @return
     */
    public int size() {
        return track.size();
    }

    /**
     * Return MidiEvent at a given index in the track
     * @param index
     * @return
     */
    public MidiEvent get(int index) {
        return (track.get(index));
    }

    /**
	 * Used by the sequencer player to get the messages it should play at a certain position
	 * @param tick
	 * @return
	 */
    public Vector<MidiEvent> getEventsForTick(long tick) {
        return tickMap.get(tick);
    }

    /**
     * Return a list of midimessages in order to restore controller states at a specific tick.
     * Used when looping a sequence, or when starting playback in the middle of the song.
     * This is a replacement for the chasing mechanism in the JSE RealtimeSequencer implementation.
     * @param tick
     * @return
     */
    public synchronized Vector<MidiMessage> getControllerStateAtTick(long tick) {
        controllerStateMessages.clear();
        for (int ccKey : controllerMap.keySet()) {
            SortedMap<Long, Integer> ccValues = controllerMap.get(ccKey);
            try {
                int ccValue = ccValues.get(ccValues.headMap(tick).lastKey());
                ShortMessage shm = new ShortMessage();
                shm.setMessage(ShortMessage.CONTROL_CHANGE, (ccKey >> 8) & 0xf, ccKey & 0xff, ccValue);
                controllerStateMessages.add(shm);
            } catch (Exception e) {
            }
        }
        return controllerStateMessages;
    }

    /**
     * @return Returns the sequence.
     */
    public FrinikaSequence getSequence() {
        return sequence;
    }

    public void setSequence(FrinikaSequence sequence) {
        this.sequence = sequence;
    }

    /**
     * @return Returns the midi channel for this track
     */
    public int getMidiChannel() {
        return midiChannel;
    }

    /**
     * Set the midi channel for this track
     * @param midiChannel
     */
    public void setMidiChannel(int midiChannel) {
        this.midiChannel = midiChannel;
    }

    /**
     * @return Returns the midiDevice.
     */
    public MidiDevice getMidiDevice() {
        return midiDevice;
    }

    /**
     * @param midiDevice The midiDevice to set.
     */
    public void setMidiDevice(MidiDevice midiDevice) {
        this.midiDevice = midiDevice;
    }

    public EditHistoryContainer getEditHistoryContainer() {
        System.err.println(" Edit hostory does not live here any more (FrinikaSequence) ");
        return null;
    }

    public void attachToSequence() {
        if (sequence.frinikaTrackWrappers.contains(this)) return;
        sequence.frinikaTrackWrappers.add(this);
    }

    public void detachFromSequence() {
        sequence.frinikaTrackWrappers.remove(this);
    }

    public long lastTickUsed() {
        int s = track.size();
        if (s > 1) {
            MidiEvent lastMidiEvent = track.get(s - 2);
            long tick = lastMidiEvent.getTick();
            return tick;
        } else {
            return 0;
        }
    }
}
