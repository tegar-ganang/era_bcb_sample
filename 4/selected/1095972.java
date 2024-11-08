package jorgan.fluidsynth.midi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import jorgan.fluidsynth.Fluidsynth;

/**
 * Java Wrapper for a Fluidsynth.
 */
public class FluidsynthMidiDevice implements MidiDevice {

    private Info info;

    private List<ReceiverImpl> receivers = new ArrayList<ReceiverImpl>();

    private boolean open;

    private Fluidsynth synth;

    public FluidsynthMidiDevice(Info info, Fluidsynth synth) {
        this.info = info;
        this.synth = synth;
    }

    public Info getDeviceInfo() {
        return info;
    }

    public Fluidsynth getSynth() {
        return synth;
    }

    public void close() {
        open = false;
        for (ReceiverImpl receiver : new ArrayList<ReceiverImpl>(receivers)) {
            receiver.close();
        }
        receivers.clear();
    }

    public int getMaxReceivers() {
        return -1;
    }

    public int getMaxTransmitters() {
        return 0;
    }

    public long getMicrosecondPosition() {
        return 0;
    }

    public List<Receiver> getReceivers() {
        return new ArrayList<Receiver>(receivers);
    }

    public List<Transmitter> getTransmitters() {
        return Collections.emptyList();
    }

    public Receiver getReceiver() throws MidiUnavailableException {
        if (!open) {
            throw new IllegalStateException();
        }
        return new ReceiverImpl();
    }

    public Transmitter getTransmitter() throws MidiUnavailableException {
        throw new MidiUnavailableException();
    }

    public boolean isOpen() {
        return open;
    }

    public void open() throws MidiUnavailableException {
        open = true;
    }

    private class ReceiverImpl implements Receiver {

        private boolean closed;

        public ReceiverImpl() {
            receivers.add(this);
        }

        public void close() {
            closed = true;
            receivers.remove(this);
        }

        public void send(MidiMessage message, long timeStamp) {
            if (closed) {
                throw new IllegalStateException();
            }
            if (message instanceof ShortMessage) {
                ShortMessage shortMessage = (ShortMessage) message;
                synth.send(shortMessage.getChannel(), shortMessage.getCommand(), shortMessage.getData1(), shortMessage.getData2());
            }
        }
    }
}
