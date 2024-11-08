package util.midi;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import javax.sound.midi.*;

/**
 * MidiSynthesizerSample.java
 * Purpose: Sample code to be used for reference. Acts as a simple midi player that can change volumes of channels
 * Use when testing/changing new MIDI channel properties
 * 
 * @author Ryan Tenorio
 *
 */
public class MidiSynthesizerSample {

    /**
     * Main method
     * @param args
     */
    public static void main(String[] args) {
        try {
            Sequence sequence = MidiSystem.getSequence(new File("midi/verdi_requiem.mid"));
            Sequencer sequencer = MidiSystem.getSequencer(false);
            Receiver receiver = MidiSystem.getReceiver();
            sequencer.open();
            sequencer.getTransmitter().setReceiver(receiver);
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            synthesizer.loadAllInstruments(MidiSystem.getSoundbank(new File("midi/soundbank-deluxe.gm")));
            sequencer.setSequence(sequence);
            sequencer.start();
            setVolume(0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

    /**
     * Change the reverb (channel 91)
     * @param value
     */
    public static void setReverb(int value) {
        try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            Receiver receiver = synthesizer.getReceiver();
            Sequencer sequencer = MidiSystem.getSequencer(false);
            Transmitter transmitter = sequencer.getTransmitter();
            transmitter.setReceiver(receiver);
            javax.sound.midi.MidiChannel[] channels = synthesizer.getChannels();
            ShortMessage volumeMessage = new ShortMessage();
            for (int i = 0; i < 9; i++) {
                volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, i, 91, value);
                MidiSystem.getReceiver().send(volumeMessage, -1);
            }
            for (int i = 11; i < channels.length; i++) {
                volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, i, 91, value);
                MidiSystem.getReceiver().send(volumeMessage, -1);
            }
        } catch (Exception e) {
        }
    }

    /**
     * 
     * @param value
     */
    public static void setVolume(int value) {
        try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            Receiver receiver = synthesizer.getReceiver();
            Sequencer sequencer = MidiSystem.getSequencer(false);
            Transmitter transmitter = sequencer.getTransmitter();
            transmitter.setReceiver(receiver);
            javax.sound.midi.MidiChannel[] channels = synthesizer.getChannels();
            ShortMessage volumeMessage = new ShortMessage();
            for (int i = 0; i < 9; i++) {
                volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, i, 7, value);
                MidiSystem.getReceiver().send(volumeMessage, -1);
            }
            for (int i = 11; i < channels.length; i++) {
                volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, i, 7, value);
                MidiSystem.getReceiver().send(volumeMessage, -1);
            }
        } catch (Exception e) {
        }
    }
}
