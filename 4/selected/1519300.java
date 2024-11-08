package org.jsynthlib.synthdrivers.YamahaDX7.common;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class DX7FamilyPerformanceIISingleDriver extends Driver {

    byte[] initSysex;

    String[] dxPatchNumbers;

    String[] dxBankNumbers;

    public DX7FamilyPerformanceIISingleDriver(final Device device, byte[] initSysex, String[] dxPatchNumbers, String[] dxBankNumbers) {
        super(device, "Single Performance", "Torsten Tittmann");
        this.initSysex = initSysex;
        this.dxPatchNumbers = dxPatchNumbers;
        this.dxBankNumbers = dxBankNumbers;
        sysexID = "F0430*7E003d4c4d2020383937335045";
        patchNameStart = 47;
        patchNameSize = 20;
        deviceIDoffset = 2;
        checksumOffset = 67;
        checksumStart = 6;
        checksumEnd = 66;
        patchNumbers = dxPatchNumbers;
        bankNumbers = dxBankNumbers;
        patchSize = 69;
        trimSize = 69;
        sysexRequestDump = new SysexHandler("f0 43 @@ 7e 4c 4d 20 20 38 39 37 33 50 45 f7");
    }

    public Patch createNewPatch() {
        return new Patch(initSysex, this);
    }

    public JSLFrame editPatch(Patch p) {
        return new DX7FamilyPerformanceIIEditor(getManufacturerName() + " " + getModelName() + " \"" + getPatchType() + "\" Editor", (Patch) p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
