package oldStuff;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 *
 * @author pjl
 */
public class MidiReader {

    /**
     *
     * Creates a SortedSet (assending tick value) of MyNotes from a MidiFile
     *
     * @param name file name
     * @return
     * @throws javax.sound.midi.InvalidMidiDataException
     * @throws java.io.IOException
     */
    static TreeSet<MyNote> readFile(String name) throws InvalidMidiDataException, IOException {
        File file = new File(name);
        TreeSet<MyNote> notes = new TreeSet<MyNote>();
        Sequence seq = MidiSystem.getSequence(file);
        for (Track track : seq.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage mess = event.getMessage();
                if (event.getMessage() instanceof ShortMessage) {
                    ShortMessage shm = (ShortMessage) event.getMessage();
                    int cmd = shm.getCommand();
                    long tick = event.getTick();
                    if (cmd == ShortMessage.NOTE_ON || cmd == ShortMessage.NOTE_OFF) {
                        int chan = shm.getChannel();
                        int pitch = shm.getData1();
                        int vel = shm.getData2();
                        notes.add(new MyNote(chan, pitch, vel, tick));
                    }
                }
            }
        }
        return notes;
    }

    public static void main(String args[]) throws InvalidMidiDataException, IOException {
        String name = "/home/pjl/MIDI/A07.mid";
        TreeSet<MyNote> notes = readFile(name);
        System.out.println("-----------------------------");
        for (MyNote note : notes) {
            System.out.println(note);
        }
    }
}
