package promidi;

import java.util.Vector;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

/**
 * A cache object to keep hold of notes that are currently on.
 */
class NoteOnCache {

    private Vector pendingNoteOffs = new Vector();

    NoteOnCache() {
        pendingNoteOffs.ensureCapacity(256);
    }

    void interceptMessage(MidiMessage msg) {
        try {
            ShortMessage shm = (ShortMessage) msg;
            if (shm.getCommand() == ShortMessage.NOTE_ON) {
                if (shm.getData2() == 0) {
                    pendingNoteOffs.remove(new Integer(shm.getChannel() << 8 | shm.getData1()));
                } else pendingNoteOffs.add(new Integer(shm.getChannel() << 8 | shm.getData1()));
            }
        } catch (Exception e) {
        }
    }

    Vector getPendingNoteOffs() {
        return pendingNoteOffs;
    }

    void releasePendingNoteOffs() {
        pendingNoteOffs.clear();
    }
}
