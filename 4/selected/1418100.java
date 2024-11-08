package ampt.examples.filters;

import java.io.File;
import java.io.PrintStream;
import java.io.IOException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Sequence;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Transmitter;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.InvalidMidiDataException;

public class ChangeNoteFilter implements Receiver, Transmitter {

    private Receiver destination;

    private PrintStream out;

    private int offset;

    public ChangeNoteFilter(Receiver destination, int offset, PrintStream out) {
        this.destination = destination;
        this.out = out;
        this.offset = offset;
    }

    public ChangeNoteFilter(Receiver destination, int offset) {
        this.destination = destination;
        this.offset = offset;
    }

    public void setReceiver(Receiver receiver) {
        destination = receiver;
    }

    public Receiver getReceiver() {
        return destination;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public void send(MidiMessage message, long timeStamp) {
        if (message instanceof ShortMessage) {
            ShortMessage sMsg = (ShortMessage) message;
            if (out != null) out.println("Type: Short Message; Command : " + sMsg.getCommand() + "; Channel: " + sMsg.getChannel() + "; data1: " + sMsg.getData1() + "; data2: " + sMsg.getData2());
            if ((sMsg.getCommand() == ShortMessage.NOTE_ON) || (sMsg.getCommand() == ShortMessage.NOTE_OFF)) {
                try {
                    sMsg.setMessage(sMsg.getCommand(), sMsg.getChannel(), sMsg.getData1() + offset, sMsg.getData2());
                } catch (InvalidMidiDataException mde) {
                    mde.printStackTrace();
                }
            }
            destination.send(sMsg, timeStamp);
        } else {
            if (out != null) out.println("Non ShortMessage: " + message);
            destination.send(message, timeStamp);
        }
    }

    public void close() {
    }

    public static void main(String args[]) {
        Sequencer sqncr;
        Synthesizer synth;
        Receiver rcvr;
        Transmitter trans;
        try {
            sqncr = MidiSystem.getSequencer();
            synth = MidiSystem.getSynthesizer();
            rcvr = synth.getReceiver();
            ChangeNoteFilter filter = new ChangeNoteFilter(rcvr, 12, System.out);
            trans = sqncr.getTransmitter();
            trans.setReceiver(filter);
            sqncr.open();
            synth.open();
            File midiFile = new File("../resources/BackInTheUSSR.mid");
            Sequence seq = MidiSystem.getSequence(midiFile);
            sqncr.setSequence(seq);
            sqncr.start();
        } catch (MidiUnavailableException mue) {
        } catch (InvalidMidiDataException mde) {
        } catch (IOException ioe) {
        }
    }
}
