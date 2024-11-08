package org.jsynthlib.synthdrivers.AlesisDM5;

import org.jsynthlib.core.*;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.InvalidMidiDataException;

/** The NRPNSender class is used to send NRPNs to the DM5. The parameters
* handled by the NRPNSender are specifically related to single drumsets--i.e. all
* the parameters represented by a single set sysex record.
*
* @author Jeff Weber
*/
class NRPNSender implements SysexWidget.ISender {

    static final int NOTE_BANK = 0x08;

    static final int NOTE_SOUND = 0x09;

    static final int NOTE_COARSE_TUNE = 0x0A;

    static final int NOTE_FINE_TUNE = 0x0B;

    static final int NOTE_VOLUME = 0x0C;

    static final int NOTE_PAN = 0x0D;

    static final int NOTE_OUTPUT = 0x0E;

    static final int NOTE_GROUP = 0x0F;

    static final int SET_ROOT_NOTE = 0x10;

    static final int PREVIEW_NOTE = 0x19;

    static final int FW_CLOSE_NOTE = 0x1C;

    static final int FW_HELD_NOTE = 0x1D;

    private static final int NRPN_MSB = 99;

    private static final int NRPN_LSB = 98;

    private static final int DATA_ENTRY_MSB = 6;

    protected int param;

    protected int max;

    protected int ccMap[];

    /** Constructs an NRPNSender given  the parameter, which
        * represents the mask value, and the maximum value.
        */
    NRPNSender(int param, int[] conValues) {
        this.param = param;
        this.max = conValues.length;
        this.ccMap = conValues;
    }

    /** Constructs an NRPNSender given the parameter, which
        * represents the mask, and the maximum value.
        */
    NRPNSender(int param, int max) {
        this.param = param;
        this.max = max;
        this.ccMap = getCCMap(max);
    }

    /** Sets the number values that the NRPN Sender can send. This is used
        * exclusively by the DM5ComboBoxWidget for the Drum Sound parameter in
        * the single set editor. When the user selects a new family using the
        * Family ComboBox, the Drum Sound ComboBox list has to be updated to show
        * the list of voices for the selected family. Since each family has a
        * different number of voices, the maximum number of voices for the Drum
        * Sound comboBox has to be changed and the lookup table has to be re-
        * calculated.
        */
    void setMax(int max) {
        this.max = max;
        this.ccMap = getCCMap(max);
    }

    /** Recalculates the lookup table used to determine what value to send out
        * when a parameter is changed. Lookup tables are used instead of calcula-
        * ting the value because the values being sent out generally cover a larger
        * range--0-127--than the input value. The input value does not have a small
        * enough granularity to accurately generate the output value using a
        * calculation because of rounding errors and/or dropping of remainders.
        * Since the DM5 probably uses it's own internal table to convert the values
        * we have to mimic as close as possible what the DM5 is doing. In some cases
        * this doesn't even work, which is why we furnish another constructor that
        * lets the user specify the conversion table explicitly.
        */
    private int[] getCCMap(int max) {
        int[] convTable = new int[max + 1];
        for (int i = 127; i >= 0; i--) {
            int cc = (i * max) / 127;
            convTable[cc] = i;
        }
        return convTable;
    }

    /** Sends NRPN messages to the DM5 to adjust the selected parameter.
        * NRPNSenders are used exclusively by the AlesisDM5SgSetEditor to send out
        * NRPNs associated with any of the parameters of the single drumset
        * record. A single invocation of the send method will cause three NRPN
        * messages to be sent: the MSB, the LSB representing the selected
        * parameter, followed by the Data Entry value for that parameter.
        */
    public void send(IPatchDriver driver, int value) {
        try {
            driver.send(newControlChange(driver, NRPN_MSB, 0));
            driver.send(newControlChange(driver, NRPN_LSB, param));
            driver.send(newControlChange(driver, DATA_ENTRY_MSB, ccMap[value]));
        } catch (InvalidMidiDataException e) {
            Logger.reportStatus(e);
        }
        try {
            Thread.sleep(50);
        } catch (Exception e) {
        }
    }

    /** Returns a ShortMessage representing a single NRPN value.
        */
    protected ShortMessage newControlChange(IPatchDriver driver, int controlNumber, int value) throws InvalidMidiDataException {
        ShortMessage ccMessage = new ShortMessage();
        ccMessage.setMessage(ShortMessage.CONTROL_CHANGE, driver.getDevice().getChannel() - 1, controlNumber, value);
        return ccMessage;
    }
}
