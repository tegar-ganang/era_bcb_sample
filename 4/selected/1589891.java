package org.jsynthlib.synthdrivers.Line6BassPod;

import org.jsynthlib.core.*;

/** Line6 Edit Buffer Driver. Used for Line6 Edit Buffer patch.
* Note that on the Pod, the edit buffer patch has an 8 byte header and the
* program patch has a 9 byte header. The only reason for having this driver
* is to be able to request an edit buffer patch. As soon as the edit buffer
* patch is received, Line6BassPodConverter converts it to a regular program
* patch. From that point on it is handled like any other program patch.
*
* @author Jeff Weber
*/
public class Line6BassPodEdBufDriver extends Line6BassPodSingleDriver {

    /** Edit Buffer Dump Request
    */
    private static final SysexHandler SYS_REQ = new SysexHandler(Constants.EDIT_DUMP_REQ_ID);

    /** Constructs a Line6BassPodEdBufDriver.
    */
    public Line6BassPodEdBufDriver(final Device device) {
        super(device, Constants.EDIT_PATCH_TYP_STR, Constants.AUTHOR);
        sysexID = Constants.EDIT_SYSEX_MATCH_ID;
        patchSize = Constants.EDMP_HDR_SIZE + Constants.SIGL_SIZE + 1;
        patchNameStart = Constants.PATCH_NAME_START;
        patchNameSize = Constants.PATCH_NAME_SIZE;
        deviceIDoffset = Constants.DEVICE_ID_OFFSET;
        bankNumbers = Constants.EDIT_BANK_LIST;
        patchNumbers = Constants.EDIT_PATCH_LIST;
    }

    /** Requests a dump of the Line6 edit buffer.
        * The bankNum and patchNum parameters are ignored.
        */
    public void requestPatchDump(int bankNum, int patchNum) {
        int progNum = 0;
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("progNum", progNum)));
    }
}
