package org.jsynthlib.synthdrivers.AlesisDM5;

import org.jsynthlib.core.*;

/** Alesis DM5 System Info Driver.
*
* @author Jeff Weber
*/
public class AlesisDM5SysInfoDriver extends Driver {

    /** DM5 System Info Dump Request
    */
    private static final SysexHandler SYS_REQ = new SysexHandler(Constants.SYS_INFO_DUMP_REQ_ID);

    /** Sysex program dump byte array representing a new system info patch
    */
    private static final byte NEW_SYSEX[] = { (byte) 0xF0, (byte) 0x00, (byte) 0x00, (byte) 0x0E, (byte) 0x13, (byte) 0x00, (byte) 0x00, (byte) 0x4C, (byte) 0x00, (byte) 0x00, (byte) 0xF7 };

    /** Constructs a AlesisDM5SysInfoDriver.
    */
    public AlesisDM5SysInfoDriver(Device device) {
        super(device, Constants.SYS_INFO_PATCH_TYP_STR, Constants.AUTHOR);
        sysexID = Constants.SYS_INFO_SYSEX_MATCH_ID;
        patchSize = Constants.HDR_SIZE + Constants.SYS_INFO_SIZE + 1;
        deviceIDoffset = Constants.DEVICE_ID_OFFSET;
        bankNumbers = Constants.SYS_INFO_BANK_LIST;
        patchNumbers = Constants.SYS_INFO_PATCH_LIST;
    }

    /** Constructs a AlesisDM5SysInfoDriver. Called by AlesisDM5EdBufDriver
        */
    public AlesisDM5SysInfoDriver(Device device, String patchType, String authors) {
        super(device, patchType, authors);
    }

    /** Send Program Change MIDI message. The Alesis System Info driver does
        * not utilize program change messages. This method is overriden with a
        * null method.
        */
    protected void setPatchNum(int patchNum) {
    }

    /** Send Control Change (Bank Select) MIDI message. The Alesis System Info
        * driver does not utilize bank select. This method is overriden with a
        * null method.
        */
    protected void setBankNum(int bankNum) {
    }

    /** Requests a dump of the system info message.
        * This patch does not utilize bank select or program changes.
        */
    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("channel", getChannel())));
    }

    /** Alesis DM5SysInfoDriver patch does not have checksum. This is overridden
        * by a null method.
        */
    protected void calculateChecksum(Patch p) {
    }

    /** Creates a new system info patch with default values.
        */
    protected Patch createNewPatch() {
        Patch p = new Patch(NEW_SYSEX, this);
        return p;
    }

    /** Opens an edit window on the specified patch.
        */
    protected JSLFrame editPatch(Patch p) {
        return new AlesisDM5SysInfoEditor((Patch) p);
    }
}
