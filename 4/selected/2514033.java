package synthdrivers.AlesisDM5;

import core.*;

/** Alesis DM5 Edit Buffer Driver. This driver subclasses AlesisDM5SgSetDriver
* because the edit buffer patch is the same format as a single set patch. Once
* an edit buffer patch is read in, it is treated like a single set patch. The
* only purpose for the driver is so that a choice for the edit buffer patch
* shows up in dialogs.
*
* @author Jeff Weber
*/
public class AlesisDM5EdBufDriver extends AlesisDM5SgSetDriver {

    /** Edit Buffer Dump Request
    */
    private static final SysexHandler SYS_REQ = new SysexHandler(Constants.EDIT_BUFF_DUMP_REQ_ID);

    /** Constructs a AlesisDM5EdBufDriver.
    */
    public AlesisDM5EdBufDriver() {
        super(Constants.EDIT_BUFF_PATCH_TYP_STR, Constants.AUTHOR);
        sysexID = Constants.EDIT_BUFF_SYSEX_MATCH_ID;
        patchSize = Constants.HDR_SIZE + Constants.EDIT_BUFF_SIZE + 1;
        patchNameStart = Constants.PATCH_NAME_START;
        patchNameSize = Constants.PATCH_NAME_SIZE;
        deviceIDoffset = Constants.DEVICE_ID_OFFSET;
        bankNumbers = Constants.EDIT_BUFF_BANK_LIST;
        patchNumbers = Constants.EDIT_BUFF_PATCH_LIST;
        checksumStart = Constants.HDR_SIZE;
        checksumEnd = patchSize - 3;
        checksumOffset = checksumEnd + 1;
    }

    /** Constructs a AlesisDM5EdBufDriver. Called by AlesisDM5EdBufDriver
        */
    public AlesisDM5EdBufDriver(String patchType, String authors) {
        super(patchType, authors);
    }

    /** Requests a dump of the system info message.
        * This patch does not utilize bank select or program changes. 
        */
    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("channel", getChannel())));
    }
}
