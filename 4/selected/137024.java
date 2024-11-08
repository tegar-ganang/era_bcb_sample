package xwh.jPiano.test;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import xwh.jPiano.ChannelData;

public class MidiTest {

    public static MidiDevice midiDevice;

    public static ChannelData channels[];

    public static ChannelData cc_left;

    public static Synthesizer synthesizer;

    public static Sequencer sequencer;

    public static Sequence sequence;

    public static Instrument instruments[];

    public static void main(String[] args) throws InterruptedException {
        try {
            setDevice("Java");
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
        open();
        nodeOn(cc_left, 60, 90);
        Thread.sleep(1000);
        nodeOff(cc_left, 60, 90);
        changeProgram(midiDevice, cc_left, 20);
        nodeOn(cc_left, 62, 90);
        Thread.sleep(1000);
        nodeOff(cc_left, 62, 90);
        nodeOn(cc_left, 64, 90);
        Thread.sleep(1000);
        nodeOff(cc_left, 64, 90);
        close();
    }

    public static void changeProgram(MidiDevice device, ChannelData cc, int program) {
        ShortMessage message = new ShortMessage();
        try {
            message.setMessage(ShortMessage.PROGRAM_CHANGE + cc.num, program, cc.velocity);
            long timeStamp = -1;
            device.getReceiver().send(message, timeStamp);
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        } catch (InvalidMidiDataException e1) {
            e1.printStackTrace();
        }
    }

    public static void setDevice(String deviceName) throws MidiUnavailableException {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < infos.length; i++) {
            String infoString = infos[i].getName();
            System.out.println(infoString);
            if (infoString.startsWith(deviceName)) {
                MidiDevice device = MidiSystem.getMidiDevice(infos[i]);
                if (!device.isOpen()) {
                    try {
                        device.open();
                    } catch (Exception e) {
                        continue;
                    }
                }
                midiDevice = device;
                if (device instanceof Synthesizer) {
                    synthesizer = (Synthesizer) device;
                }
                break;
            }
        }
    }

    public static void nodeOn(ChannelData cc, int num, int pressure) {
        ShortMessage message = new ShortMessage();
        try {
            message.setMessage(ShortMessage.NOTE_ON, cc.num, num, pressure);
            long timeStamp = -1;
            midiDevice.getReceiver().send(message, timeStamp);
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        } catch (InvalidMidiDataException e1) {
            e1.printStackTrace();
        }
    }

    public static void nodeOff(ChannelData cc, int num, int pressure) {
        ShortMessage message = new ShortMessage();
        try {
            message.setMessage(ShortMessage.NOTE_OFF, cc.num, num, pressure);
            long timeStamp = -1;
            midiDevice.getReceiver().send(message, timeStamp);
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        } catch (InvalidMidiDataException e1) {
            e1.printStackTrace();
        }
    }

    public static void open() {
        try {
            if (synthesizer == null) {
                if ((synthesizer = MidiSystem.getSynthesizer()) == null) {
                    System.out.println("getSynthesizer() failed!");
                    return;
                }
            }
            synthesizer.open();
            sequencer = MidiSystem.getSequencer();
            sequence = new Sequence(Sequence.PPQ, 10);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        Soundbank sb = synthesizer.getDefaultSoundbank();
        if (sb != null) {
            instruments = synthesizer.getDefaultSoundbank().getInstruments();
            synthesizer.loadInstrument(instruments[0]);
        }
        MidiChannel midiChannels[] = synthesizer.getChannels();
        channels = new ChannelData[midiChannels.length];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new ChannelData(midiChannels[i], i);
        }
        cc_left = channels[0];
    }

    public static void close() {
        if (synthesizer != null) {
            synthesizer.close();
        }
        if (sequencer != null) {
            sequencer.close();
        }
        sequencer = null;
        synthesizer = null;
        instruments = null;
        channels = null;
    }
}
