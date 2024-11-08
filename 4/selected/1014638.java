package com.frinika.tootX.midi;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import uk.org.toot.control.BooleanControl;
import uk.org.toot.control.Control;
import uk.org.toot.control.ControlLaw;
import uk.org.toot.control.LawControl;

/**
 * 
 * Used to control a toot Control using the data of a midi message.
 * 
 * 
 * @author pjl
 */
public class ControlMapper implements Receiver {

    Control cntrl;

    ShortMessage proto;

    interface Valueizer {

        float getValue(ShortMessage mess);
    }

    Valueizer valueizer;

    /**
    * 
    * @param cntrl  the control to be tweaked 
    * @param proto  an example of the midi message to do the job
    */
    public ControlMapper(Control cntrl, ShortMessage proto) {
        this.cntrl = cntrl;
        this.proto = proto;
        switch(proto.getCommand()) {
            case ShortMessage.CONTROL_CHANGE:
                valueizer = new Valueizer() {

                    public float getValue(ShortMessage mess) {
                        return (float) (mess.getData2() / 127.0);
                    }
                };
                break;
            case ShortMessage.NOTE_ON:
                valueizer = new Valueizer() {

                    public float getValue(ShortMessage mess) {
                        return (float) (mess.getData2() / 127.0);
                    }
                };
                break;
            case ShortMessage.PITCH_BEND:
                valueizer = new Valueizer() {

                    public float getValue(ShortMessage mess) {
                        short low = (byte) mess.getData1();
                        short high = (byte) mess.getData2();
                        short val = (short) ((high << 7) | low);
                        System.out.println(" val = " + val);
                        return (float) (val / 8192.0);
                    }
                };
        }
    }

    public void close() {
    }

    public void send(MidiMessage mess, long arg1) {
        ShortMessage smsg = (ShortMessage) mess;
        System.out.println("ch cmd data1 data2: " + smsg.getChannel() + " " + smsg.getCommand() + " " + smsg.getData1() + " " + smsg.getData2());
        double t = valueizer.getValue((ShortMessage) mess);
        System.out.println(" Send message to " + cntrl + " " + t);
        if (cntrl instanceof ControlLaw) {
            ControlLaw law = ((LawControl) cntrl).getLaw();
            float val = (float) (law.getMaximum() * t + law.getMinimum() * (1 - t));
            ((LawControl) cntrl).setValue(val);
        } else if (cntrl instanceof BooleanControl) {
            System.out.println(" BOOLEAN");
            ((BooleanControl) cntrl).setValue(t > 0);
        }
    }
}
