package engine.utils;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

/**
 * classe ControlMsg.java
 * @author Marc Haussaire
 */
public class ControlMsg {

    public int channel;

    public int control;

    public int value;

    public boolean valid = false;

    public ControlMsg(MidiMessage m) {
        if (!(m instanceof ShortMessage)) return;
        ShortMessage m2 = (ShortMessage) m;
        if (m2.getCommand() != ShortMessage.CONTROL_CHANGE) return;
        channel = m2.getChannel();
        control = m2.getData1();
        value = m2.getData2();
        System.out.println("value " + m2.getData2());
        valid = true;
    }

    public static MidiMessage getControlMsg(int channel, int control, int value) {
        ShortMessage m = new ShortMessage();
        try {
            m.setMessage(ShortMessage.CONTROL_CHANGE, channel, control, value);
        } catch (Exception e) {
            throw new Error(e);
        }
        return m;
    }

    public static boolean wheel(MidiMessage m) {
        if (m instanceof ShortMessage) {
            ShortMessage mes = (ShortMessage) m;
            return mes.getCommand() == ShortMessage.PITCH_BEND;
        }
        return false;
    }
}
