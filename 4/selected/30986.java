package org.jsynthlib.synthdrivers.YamahaDX7.common;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class DX7FamilySystemSetupIIIDriver extends Driver {

    byte[] initSysex;

    String[] dxPatchNumbers;

    String[] dxBankNumbers;

    public DX7FamilySystemSetupIIIDriver(final Device device, byte[] initSysex, String[] dxPatchNumbers, String[] dxBankNumbers) {
        super(device, "System Setup", "Torsten Tittmann");
        this.initSysex = initSysex;
        this.dxPatchNumbers = dxPatchNumbers;
        this.dxBankNumbers = dxBankNumbers;
        sysexID = "F0430*7E02114C4D2020383935325320";
        patchNameStart = 0;
        patchNameSize = 0;
        deviceIDoffset = 2;
        checksumOffset = 279;
        checksumStart = 6;
        checksumEnd = 278;
        patchNumbers = dxPatchNumbers;
        bankNumbers = dxBankNumbers;
        patchSize = 281;
        trimSize = 281;
        sysexRequestDump = new SysexHandler("F0 43 @@ 7E 4C 4D 20 20 38 39 35 32 53 20 F7");
    }

    public Patch createNewPatch() {
        return new Patch(initSysex, getDevice());
    }

    public JSLFrame editPatch(Patch p) {
        return new DX7FamilySystemSetupIIIEditor(getManufacturerName() + " " + getModelName() + " \"" + getPatchType() + "\" Editor", p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
