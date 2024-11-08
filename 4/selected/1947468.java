package synthdrivers.KawaiK5000;

import java.io.UnsupportedEncodingException;
import core.BankDriver;
import core.ErrorMsg;
import core.Patch;
import core.SysexHandler;
import core.Utility;

public class KawaiK5000CombiBankDriver extends BankDriver {

    static final int FIRST_PATCH_START = 7;

    static final SysexHandler SYSEX_REQUEST_DUMP = new SysexHandler("F0 40 @@ 01 00 0A 20 00 *patchNum* F7");

    public KawaiK5000CombiBankDriver() {
        super("CombiBank", "Phil Shepherd", KawaiK5000CombiDriver.PATCH_NUMBERS.length, 4);
        sysexID = "F040**21000A20";
        sysexRequestDump = SYSEX_REQUEST_DUMP;
        patchSize = 0;
        numSysexMsgs = 1;
        deviceIDoffset = 2;
        singleSysexID = "F040**20000A20";
        singleSize = 0;
        bankNumbers = KawaiK5000CombiDriver.BANK_NUMBERS;
        patchNumbers = KawaiK5000CombiDriver.PATCH_NUMBERS;
    }

    public void setBankNum(int bankNum) {
        try {
            send(0xB0 + (getChannel() - 1), 0x00, 0x65);
            send(0xB0 + (getChannel() - 1), 0x20, 0);
        } catch (Exception e) {
        }
        ;
    }

    public int patchIndex(int patchNum) {
        return FIRST_PATCH_START + patchNum * KawaiK5000CombiDriver.PATCH_DATA_SIZE;
    }

    public String getPatchName(Patch ip) {
        return (((Patch) ip).sysex.length / 1024) + " Kilobytes";
    }

    public int getPatchNameStart(int patchNum) {
        return patchIndex(patchNum) + KawaiK5000CombiDriver.PATCH_NAME_START - KawaiK5000CombiDriver.PATCH_DATA_START;
    }

    public String getPatchName(Patch p, int patchNum) {
        try {
            return new String(((Patch) p).sysex, getPatchNameStart(patchNum), 8, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            return "-??????-";
        }
    }

    public void setPatchName(Patch bank, int patchNum, String name) {
        Patch p = getPatch(bank, patchNum);
        p.setName(name);
        p.calculateChecksum();
        putPatch(bank, p, patchNum);
    }

    public Patch getPatch(Patch bank, int patchNum) {
        try {
            return KawaiK5000CombiDriver.createPatchFromData(((Patch) bank).sysex, patchIndex(patchNum), KawaiK5000CombiDriver.PATCH_DATA_SIZE);
        } catch (Exception ex) {
            ErrorMsg.reportError("Error", "Error in K5000 Combi Bank Driver", ex);
            return null;
        }
    }

    public void putPatch(Patch bank, Patch p, int patchNum) {
        ((Patch) bank).sysex = Utility.byteArrayReplace(((Patch) bank).sysex, patchIndex(patchNum), KawaiK5000CombiDriver.PATCH_DATA_SIZE, ((Patch) p).sysex, KawaiK5000CombiDriver.PATCH_DATA_START, KawaiK5000CombiDriver.PATCH_DATA_SIZE);
    }
}
