package engine.utils;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

/**
 * classe Note.java
 * @author Marc Haussaire
 */
public class Note {

    public boolean valid = false;

    public boolean noteON = true;

    public int channel;

    public int note;

    public int force;

    public Note(MidiMessage mes) {
        if (!(mes instanceof ShortMessage)) return;
        ShortMessage mes2 = (ShortMessage) mes;
        if (mes2.getCommand() != ShortMessage.NOTE_ON && mes2.getCommand() != ShortMessage.NOTE_OFF) return;
        if (mes2.getData2() == 0) noteON = false;
        channel = mes2.getChannel();
        note = mes2.getData1();
        force = mes2.getData2();
        valid = true;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Note)) return false;
        Note n = (Note) o;
        return true;
    }

    public MidiMessage toMsg() {
        return getMsg(ShortMessage.NOTE_ON, this.channel, this.note, this.force);
    }

    public static MidiMessage getMsgON(int channel, int note, int force) {
        ShortMessage m = new ShortMessage();
        try {
            m.setMessage(ShortMessage.NOTE_ON, channel, note, force);
        } catch (Exception e) {
            throw new Error(e);
        }
        return m;
    }

    public static MidiMessage getMsg(int type, int channel, int note, int force) {
        ShortMessage m = new ShortMessage();
        try {
            m.setMessage(type, channel, note, force);
        } catch (Exception e) {
            throw new Error(e);
        }
        return m;
    }

    public static MidiMessage getMsgOFF(int channel, int note, int force) {
        ShortMessage m = new ShortMessage();
        try {
            m.setMessage(ShortMessage.NOTE_OFF, channel, note, force);
        } catch (Exception e) {
            throw new Error(e);
        }
        return m;
    }
}
