package data;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

public class SimpleSynth {

    Synthesizer synth;

    Receiver rcvr;

    static MidiChannel[] channels = null;

    public SimpleSynth(int note) {
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            rcvr = synth.getReceiver();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        }
        channels = synth.getChannels();
        channels[0].programChange(81);
        MidiMessage noteOn = getNoteOnMessage(note);
        rcvr.send(noteOn, 0);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MidiMessage noteOff = getNoteOffMessage(note);
        rcvr.send(noteOff, 0);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        synth.close();
    }

    private MidiMessage getNoteOnMessage(int note) {
        return getMessage(ShortMessage.NOTE_ON, note);
    }

    private MidiMessage getNoteOffMessage(int note) {
        return getMessage(ShortMessage.NOTE_OFF, note);
    }

    private MidiMessage getMessage(int cmd, int note) {
        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(cmd, 0, note, 60);
            return (MidiMessage) msg;
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }
}
