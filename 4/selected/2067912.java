package engine.utils;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class MidiUtils {

    public static void updateShortMes(MidiMessage m, int channel, int command, int data1, int data2) {
        if (!(m instanceof ShortMessage)) return;
        ShortMessage me = (ShortMessage) m;
        try {
            me.setMessage(command, channel, data1, data2);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public static void setDatas(MidiMessage m, int data1, int data2) {
        if (!(m instanceof ShortMessage)) return;
        ShortMessage me = (ShortMessage) m;
        try {
            me.setMessage(me.getCommand(), me.getChannel(), data1, data2);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public static MidiMessage setChannel2(MidiMessage m, int channel) {
        if (!(m instanceof ShortMessage)) throw new IllegalArgumentException();
        ShortMessage me = (ShortMessage) m;
        ShortMessage res = new ShortMessage();
        try {
            res.setMessage(me.getCommand(), channel, me.getData1(), me.getData2());
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static void setChannel(MidiMessage m, int channel) {
        if (!(m instanceof ShortMessage)) return;
        ShortMessage me = (ShortMessage) m;
        try {
            me.setMessage(me.getCommand(), channel, me.getData1(), me.getData2());
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public static void setCommand(MidiMessage m, int command) {
        if (!(m instanceof ShortMessage)) return;
        ShortMessage me = (ShortMessage) m;
        try {
            me.setMessage(command, me.getChannel(), me.getData1(), me.getData2());
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public static void setStatus(MidiMessage m, int status) {
        if (!(m instanceof ShortMessage)) return;
        ShortMessage me = (ShortMessage) m;
        try {
            me.setMessage(status);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }
}
