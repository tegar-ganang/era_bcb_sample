package synthdrivers.PeaveyPC1600;

import core.Driver;
import core.NibbleSysex;
import core.Patch;
import core.SysexHandler;

public class PeaveyPC1600SingleDriver extends Driver {

    static final int NIBBLE_MULTIPLIER = 16;

    static final int PATCH_SIZE_START = 39;

    static final int PATCH_SIZE_BYTES = 4;

    static final int PATCH_SIZE_FACTOR = 2;

    static final int PATCH_DATA_START = PATCH_SIZE_START + PATCH_SIZE_BYTES;

    static final int NON_DATA_SIZE = PATCH_DATA_START + 1;

    static final int PATCH_NAME_START = 7;

    static final int PATCH_NAME_SIZE = 16;

    static final int PATCH_NAME_CHAR_BYTES = 2;

    static final int DEVICE_ID_OFFSET = 5;

    static final String[] BANK_NUMBERS = new String[] { "User" };

    static final String[] PATCH_NUMBERS = new String[] { "00-", "01-", "02-", "03-", "04-", "05-", "06-", "07-", "08-", "09-", "10-", "11-", "12-", "13-", "14-", "15-", "16-", "17-", "18-", "19-", "20-", "21-", "22-", "23-", "24-", "25-", "26-", "27-", "28-", "29-", "30-", "31-", "32-", "33-", "34-", "35-", "36-", "37-", "38-", "39-", "40-", "41-", "42-", "43-", "44-", "45-", "46-", "47-", "48-", "49-", "Ed-" };

    static final SysexHandler SYSEX_WRITE_EDIT_BUFFER = new SysexHandler("F0 00 00 1B 0B @@ 20 *patchNum* F7");

    static final SysexHandler SYSEX_RECALL_PRESET = new SysexHandler("F0 00 00 1B 0B @@ 00 *patchNum* F7");

    static final SysexHandler SYSEX_REQUEST_EDIT_BUFFER = new SysexHandler("F0 00 00 1B 0B @@ 14 F7");

    public PeaveyPC1600SingleDriver() {
        super("Single", "Phil Shepherd");
        sysexID = "F000001B0B**04";
        numSysexMsgs = 1;
        patchNameStart = PATCH_NAME_START;
        patchNameSize = PATCH_NAME_SIZE;
        deviceIDoffset = DEVICE_ID_OFFSET;
        bankNumbers = BANK_NUMBERS;
        patchNumbers = PATCH_NUMBERS;
    }

    public String getPatchName(Patch ip) {
        NibbleSysex nibbleSysex = new NibbleSysex(((Patch) ip).sysex, PATCH_NAME_START);
        return nibbleSysex.getNibbleStr(PATCH_NAME_SIZE, PATCH_NAME_CHAR_BYTES, NIBBLE_MULTIPLIER);
    }

    public void setPatchName(Patch p, String name) {
        NibbleSysex nibbleSysex = new NibbleSysex(((Patch) p).sysex, PATCH_NAME_START);
        nibbleSysex.putNibbleStr(name, PATCH_NAME_SIZE, PATCH_NAME_CHAR_BYTES, NIBBLE_MULTIPLIER);
    }

    public void sendPatch(Patch p) {
        storePatch(p, 0, 0);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        sendPatchWorker(p);
        send(SYSEX_WRITE_EDIT_BUFFER.toSysexMessage(getChannel(), patchNum));
    }

    public void setBankNum(int bankNum) {
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYSEX_RECALL_PRESET.toSysexMessage(getChannel(), patchNum));
        send(SYSEX_REQUEST_EDIT_BUFFER.toSysexMessage(getChannel()));
    }
}
