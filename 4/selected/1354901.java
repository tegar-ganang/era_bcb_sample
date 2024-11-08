package ampt.core.devices;

import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import ampt.midi.chord.ChordType;
import ampt.midi.chord.ChordInversion;
import java.util.LinkedList;

/**
 * This is an implementation of a filter that creates a chord.  In this 
 * implementation, the class implements MidiDevice, allowing it to be plugged-in
 * to the MidiSystem.
 * 
 * This class contains inner classes which represent it's receivers and 
 * transmitters.
 *
 * @author Christopher
 */
public class ChordFilterDevice extends AmptDevice {

    private static final String DEVICE_NAME = "Chord Filter";

    private static final String DEVICE_DESCRIPTION = "Creates a chord from a single note";

    private ChordType chordType;

    private ChordInversion chordInversion;

    public ChordFilterDevice() {
        super(DEVICE_NAME, DEVICE_DESCRIPTION);
        chordType = ChordType.MAJOR;
        chordInversion = ChordInversion.ROOT_POSITION;
    }

    public void setChordType(ChordType chordType) {
        this.chordType = chordType;
    }

    public void setChordInversion(ChordInversion chordInversion) {
        this.chordInversion = chordInversion;
    }

    @Override
    protected void initDevice() {
    }

    @Override
    protected void closeDevice() {
    }

    /**
     * Returns a new Chord Filter Receiver that is not yet bound to any
     * transmitters.
     *
     * @return a new ChordFilterReceiver
     */
    @Override
    protected Receiver getAmptReceiver() {
        return new ChordFilterReceiver();
    }

    /**
     * Inner class that implements a receiver for a chord filter.  This is
     * where all of the actual filtering takes place.
     */
    public class ChordFilterReceiver extends AmptReceiver {

        int channel, command, data1, data2;

        @Override
        protected void filter(MidiMessage message, long timeStamp) {
            List<MidiMessage> messages = new LinkedList<MidiMessage>();
            ShortMessage third = null;
            ShortMessage fifth = null;
            if (message instanceof ShortMessage) {
                ShortMessage root = (ShortMessage) message;
                command = root.getCommand();
                channel = root.getChannel();
                data1 = root.getData1();
                data2 = root.getData2();
                if (root.getCommand() == ShortMessage.NOTE_ON || root.getCommand() == ShortMessage.NOTE_OFF) {
                    try {
                        root.setMessage(command, channel, data1 + chordInversion.getRootInterval(), data2);
                        messages.add(root);
                    } catch (InvalidMidiDataException ex) {
                    }
                    third = new ShortMessage();
                    try {
                        third.setMessage(command, channel, data1 + chordType.getThirdInterval() + chordInversion.getThirdInterval(), data2);
                        messages.add(third);
                    } catch (InvalidMidiDataException ex) {
                    }
                    fifth = new ShortMessage();
                    try {
                        fifth.setMessage(command, channel, data1 + chordType.getFifthInterval() + chordInversion.getFifthInterval(), data2);
                        messages.add(fifth);
                    } catch (InvalidMidiDataException ex) {
                    }
                }
            }
            sendNow(messages);
        }
    }
}
