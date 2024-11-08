package com.frinika.sequencer;

import java.util.HashMap;
import java.util.HashSet;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

/**
 * A cache object to keep hold of notes that are currently on.
 * @author Peter Johan Salomonsen
 *
 */
public class NoteOnCache {

    private HashMap<Receiver, HashSet<Integer>> pendingNoteOffs = new HashMap<Receiver, HashSet<Integer>>();

    public final void interceptMessage(MidiMessage msg, Receiver receiver) {
        try {
            ShortMessage shm = (ShortMessage) msg;
            if (shm.getCommand() == ShortMessage.NOTE_ON) {
                if (shm.getData2() == 0) {
                    pendingNoteOffs.get(receiver).remove(shm.getChannel() << 8 | shm.getData1());
                } else {
                    if (!pendingNoteOffs.containsKey(receiver)) pendingNoteOffs.put(receiver, new HashSet<Integer>());
                    pendingNoteOffs.get(receiver).add(shm.getChannel() << 8 | shm.getData1());
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * Send note-off to all pending notes
     *
     */
    public final void releasePendingNoteOffs() {
        for (Receiver recv : pendingNoteOffs.keySet()) {
            for (int note : pendingNoteOffs.get(recv)) {
                ShortMessage shm = new ShortMessage();
                try {
                    shm.setMessage(ShortMessage.NOTE_ON, (note >> 8) & 0xf, note & 0xff, 0);
                    recv.send(shm, -1);
                } catch (InvalidMidiDataException e) {
                    e.printStackTrace();
                }
            }
        }
        pendingNoteOffs.clear();
    }
}
