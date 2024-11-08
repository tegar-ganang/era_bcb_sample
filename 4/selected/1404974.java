package org.jsynthlib.synthdrivers.RolandXV5080;

import java.io.UnsupportedEncodingException;
import org.jsynthlib.core.BankDriver;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;
import org.jsynthlib.core.Utility;

public class RolandXV5080PatchBankDriver extends BankDriver {

    static final SysexHandler SYSEX_REQUEST_DUMP = new SysexHandler("F0 41 10 00 10 11 30 *patchNum* 00 00 01 00 00 00 00 F7");

    public RolandXV5080PatchBankDriver(final Device device) {
        super(device, "PatchBank", "Phil Shepherd", RolandXV5080PatchDriver.PATCH_NUMBERS.length, 4);
        sysexID = "F0411000101230000000";
        sysexRequestDump = SYSEX_REQUEST_DUMP;
        patchSize = 128 * RolandXV5080PatchDriver.PATCH_SIZE;
        deviceIDoffset = 0;
        singleSysexID = "F0411000101230**0000";
        singleSize = RolandXV5080PatchDriver.PATCH_SIZE;
        bankNumbers = RolandXV5080PatchDriver.BANK_NUMBERS;
        patchNumbers = RolandXV5080PatchDriver.PATCH_NUMBERS;
    }

    public void setBankNum(int bankNum) {
        try {
            send(0xB0 + (getChannel() - 1), 0x00, 0x65);
            send(0xB0 + (getChannel() - 1), 0x20, 0);
        } catch (Exception e) {
        }
    }

    public String getPatchName(Patch ip) {
        return getNumPatches() + " patches";
    }

    public String getPatchName(Patch p, int patchNum) {
        try {
            return new String(p.sysex, RolandXV5080PatchDriver.PATCH_SIZE * patchNum + RolandXV5080PatchDriver.PATCH_NAME_START, RolandXV5080PatchDriver.PATCH_NAME_SIZE, "US-ASCII");
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
            byte[] sysex = new byte[RolandXV5080PatchDriver.PATCH_SIZE];
            System.arraycopy(bank.sysex, RolandXV5080PatchDriver.PATCH_SIZE * patchNum, sysex, 0, RolandXV5080PatchDriver.PATCH_SIZE);
            return new Patch(sysex, getDevice());
        } catch (Exception ex) {
            Logger.reportError("Error", "Error in XV5080 Patch Bank Driver", ex);
            return null;
        }
    }

    public void putPatch(Patch bank, Patch p, int patchNum) {
        Patch pInsert = new Patch(p.sysex);
        RolandXV5080PatchDriver singleDriver = (RolandXV5080PatchDriver) pInsert.getDriver();
        singleDriver.updatePatchNum(pInsert, patchNum);
        singleDriver.calculateChecksum(pInsert);
        bank.sysex = Utility.byteArrayReplace(bank.sysex, RolandXV5080PatchDriver.PATCH_SIZE * patchNum, RolandXV5080PatchDriver.PATCH_SIZE, pInsert.sysex, 0, RolandXV5080PatchDriver.PATCH_SIZE);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        byte[] sysex = SYSEX_REQUEST_DUMP.toByteArray(getChannel(), patchNum);
        RolandXV5080PatchDriver.calculateChecksum(sysex, 6, sysex.length - 3, sysex.length - 2);
        send(sysex);
    }
}
