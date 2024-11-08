package com.frinika.sequencer.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.Collection;

/** 
 * Allows to 'snoop' the data sent to a receiver, by passing data on to
 * MidiMessageListeners.
 * 
 * Note that instances of MidiMessageListener don't get directly connected to a
 * MonitorReceiver (there are no addMidiMessageListener() /
 * removeMidiMessageListener() methods on MonitorReceiver), but will be added to /
 * removed from higher-level classes that use MonitorReceivers.
 * 
 * @see MidiMessageListener
 * @author Jens Gulden
 */
public class MonitorReceiver implements Receiver {

    protected Receiver chained;

    protected Collection<MidiMessageListener> listeners;

    private static boolean isLinux = System.getProperty("os.name").equals("Linux");

    public MonitorReceiver(Collection<MidiMessageListener> listeners, Receiver chained) {
        this.chained = chained;
        this.listeners = listeners;
    }

    public void send(MidiMessage message, long timeStamp) {
        if (message.getStatus() >= ShortMessage.MIDI_TIME_CODE) return;
        if (isLinux) {
            if (message.getStatus() == ShortMessage.PITCH_BEND) {
                ShortMessage mess = (ShortMessage) message;
                short low = (byte) mess.getData1();
                short high = (byte) mess.getData2();
                int channel = mess.getChannel();
                low = (byte) mess.getData1();
                high = (byte) mess.getData2();
                high = (short) ((high + 64) & 0x007f);
                try {
                    mess.setMessage(ShortMessage.PITCH_BEND, channel, low, high);
                } catch (InvalidMidiDataException e) {
                    e.printStackTrace();
                }
            }
        }
        chained.send(message, timeStamp);
        notifyListeners(message);
    }

    public void close() {
        chained.close();
    }

    protected void notifyListeners(MidiMessage message) {
        for (MidiMessageListener l : listeners) {
            l.midiMessage(message);
        }
    }
}
