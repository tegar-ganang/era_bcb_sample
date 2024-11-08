package org.jsynthlib.synthdrivers.AlesisDM5;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

/** Alesis DM5 Trigger Setup Driver.
*
* @author Jeff Weber
*/
public class AlesisDM5TrSetDriver extends Driver {

    /** Alesis DM5 Trigger Setup Driver.
    */
    private static final SysexHandler SYS_REQ = new SysexHandler(Constants.TRIG_SETP_DUMP_REQ_ID);

    /** Sysex program dump byte array representing a new trigger setup patch*/
    private static final byte NEW_SYSEX[] = { (byte) 0xF0, (byte) 0x00, (byte) 0x00, (byte) 0x0E, (byte) 0x13, (byte) 0x00, (byte) 0x05, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x07, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x07, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x07, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x07, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x07, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x07, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x07, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x07, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x07, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x07, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x07, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x07, (byte) 0x63, (byte) 0x63, (byte) 0x63, (byte) 0x24, (byte) 0xF7 };

    /** Constructs a AlesisDM5TrSetDriver.
    */
    public AlesisDM5TrSetDriver(Device device) {
        super(device, Constants.TRIG_SETP_PATCH_TYP_STR, Constants.AUTHOR);
        sysexID = Constants.TRIG_SETP_SYSEX_MATCH_ID;
        patchSize = Constants.HDR_SIZE + Constants.TRIG_SETP_SIZE + 1;
        deviceIDoffset = Constants.DEVICE_ID_OFFSET;
        bankNumbers = Constants.TRIG_SETP_BANK_LIST;
        patchNumbers = Constants.TRIG_SETP_PATCH_LIST;
        checksumStart = Constants.HDR_SIZE;
        checksumEnd = patchSize - 3;
        checksumOffset = checksumEnd + 1;
    }

    /** Constructs a AlesisDM5TrSetDriver.
        */
    public AlesisDM5TrSetDriver(Device device, String patchType, String authors) {
        super(device, patchType, authors);
    }

    /** Send Program Change MIDI message. The Alesis Trigger Setup driver does
        * not utilize program change messages. This method is overriden with a
        * null method.
        */
    protected void setPatchNum(int patchNum) {
    }

    /** Send Control Change (Bank Select) MIDI message. The Alesis Trigger Setup
        * driver does not utilize bank select. This method is overriden with a
        * null method.
        */
    protected void setBankNum(int bankNum) {
    }

    /** Calculates the checksum for the DM5. Equal to the mod 128 of the sum of
        * all the bytes from offset header+1 to offset total patchlength-3.
        */
    protected void calculateChecksum(Patch patch, int start, int end, int offset) {
        int sum = 0;
        for (int i = start; i <= end; i++) {
            sum += patch.sysex[i];
        }
        patch.sysex[offset] = (byte) (sum % 128);
    }

    /** Requests a dump of the system info message.
        * This patch does not utilize bank select or program changes.
        */
    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("channel", getChannel())));
    }

    /** Creates a new trigger setup patch with default values.
        */
    protected Patch createNewPatch() {
        Patch p = new Patch(NEW_SYSEX, this);
        calculateChecksum(p);
        return p;
    }

    /** Opens an edit window on the specified patch.
        */
    protected JSLFrame editPatch(Patch p) {
        return new AlesisDM5TrSetEditor((Patch) p);
    }
}
