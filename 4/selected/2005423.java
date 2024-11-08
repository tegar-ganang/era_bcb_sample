package vavi.sound.mfi.vavi.track;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.vavi.MfiContext;
import vavi.sound.mfi.vavi.MfiConvertible;
import vavi.sound.mfi.vavi.MidiContext;

/**
 * mfi convertible utility facade.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 071011 nsano initial version <br>
 */
public class MfiConvertibleMessage implements MfiConvertible {

    /** BANK LSB */
    private int[] bankLSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** BANK MSB  */
    private int[] bankMSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** */
    public static final int RPN_PITCH_BEND_SENSITIVITY = 0x0000;

    /** */
    public static final int RPN_FINE_TUNING = 0x0001;

    /** */
    public static final int RPN_COURCE_TUNING = 0x0002;

    /** */
    public static final int RPN_TUNING_PROGRAM_SELECT = 0x0003;

    /** */
    public static final int RPN_TUNING_BANK_SELECT = 0x0004;

    /** */
    public static final int RPN_NULL = 0x7f7f;

    /** RPN LSB */
    private int[] rpnLSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** RPN MSB */
    private int[] rpnMSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** NRPN LSB */
    private int[] nrpnLSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** NRPN MSB */
    private int[] nrpnMSB = new int[MidiContext.MAX_MIDI_CHANNELS];

    /** bank, rpn, nrpn */
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context) throws InvalidMfiDataException {
        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int data1 = shortMessage.getData1();
        int data2 = shortMessage.getData2();
        switch(data1) {
            case 0:
                bankMSB[channel] = data2;
                break;
            case 32:
                bankLSB[channel] = data2;
                break;
            case 98:
                nrpnLSB[channel] = data2;
                break;
            case 99:
                nrpnMSB[channel] = data2;
                break;
            case 100:
                rpnLSB[channel] = data2;
                break;
            case 101:
                rpnMSB[channel] = data2;
                break;
            default:
                break;
        }
        return null;
    }
}
