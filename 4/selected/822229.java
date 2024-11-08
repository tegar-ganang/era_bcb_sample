package org.jsynthlib.synthdrivers.YamahaDX7.common;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class DX7FamilyMicroTuningSingleDriver extends Driver {

    byte[] initSysex;

    String[] dxPatchNumbers;

    String[] dxBankNumbers;

    protected static final SysexHandler SYSEX_REQUEST_DUMP[] = { new SysexHandler("F0 43 @@ 7E 4C 4D 20 20 4D 43 52 59 45 20 F7"), new SysexHandler("F0 43 @@ 7E 4C 4D 20 20 4D 43 52 59 4D 00 F7"), new SysexHandler("F0 43 @@ 7E 4C 4D 20 20 4D 43 52 59 4D 01 F7") };

    public DX7FamilyMicroTuningSingleDriver(final Device device, byte[] initSysex, String[] dxPatchNumbers, String[] dxBankNumbers) {
        super(device, "Single Micro Tuning", "Torsten Tittmann");
        this.initSysex = initSysex;
        this.dxPatchNumbers = dxPatchNumbers;
        this.dxBankNumbers = dxBankNumbers;
        sysexID = "F0430*7E020A4C4D20204d4352594***";
        patchNameStart = 0;
        patchNameSize = 0;
        deviceIDoffset = 2;
        checksumOffset = 272;
        checksumStart = 6;
        checksumEnd = 271;
        patchNumbers = dxPatchNumbers;
        bankNumbers = dxBankNumbers;
        patchSize = 274;
        trimSize = 274;
    }

    public Patch createNewPatch() {
        return new Patch(initSysex, this);
    }

    public JSLFrame editPatch(Patch p) {
        return new DX7FamilyMicroTuningEditor(getManufacturerName() + " " + getModelName() + " \"" + getPatchType() + "\" Editor", (Patch) p);
    }

    public void sendPatch(Patch p) {
        ((Patch) p).sysex[14] = (byte) (0x45);
        ((Patch) p).sysex[15] = (byte) (0x20);
        super.sendPatch(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if (patchNum == 0) {
            ((Patch) p).sysex[14] = (byte) (0x45);
            ((Patch) p).sysex[15] = (byte) (0x20);
        } else {
            ((Patch) p).sysex[14] = (byte) (0x4D);
            ((Patch) p).sysex[15] = (byte) (0x00 + patchNum - 1);
        }
        super.sendPatch(p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYSEX_REQUEST_DUMP[patchNum].toSysexMessage(getChannel() + 0x20));
    }
}
