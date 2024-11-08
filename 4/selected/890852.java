package org.jsynthlib.synthdrivers.YamahaDX7.common;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class DX7FamilyPerformanceIIISingleDriver extends Driver {

    byte[] initSysex;

    String[] dxPatchNumbers;

    String[] dxBankNumbers;

    public DX7FamilyPerformanceIIISingleDriver(final Device device, byte[] initSysex, String[] dxPatchNumbers, String[] dxBankNumbers) {
        super(device, "Single Performance", "Torsten Tittmann");
        this.initSysex = initSysex;
        this.dxPatchNumbers = dxPatchNumbers;
        this.dxBankNumbers = dxBankNumbers;
        sysexID = "F0430*7E01684C4D2020383935325045";
        patchNameStart = 208;
        patchNameSize = 40;
        deviceIDoffset = 2;
        checksumOffset = 248;
        checksumStart = 6;
        checksumEnd = 247;
        patchNumbers = dxPatchNumbers;
        bankNumbers = dxBankNumbers;
        patchSize = 250;
        trimSize = 250;
        sysexRequestDump = new SysexHandler("F0 43 @@ 7E 4C 4D 20 20 38 39 35 32 50 45 F7");
    }

    public Patch createNewPatch() {
        return new Patch(initSysex, this);
    }

    public JSLFrame editPatch(Patch p) {
        return new DX7FamilyPerformanceIIIEditor(getManufacturerName() + " " + getModelName() + " \"" + getPatchType() + "\" Editor", (Patch) p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }

    public String getPatchName(Patch p) {
        Patch ip = (Patch) p;
        try {
            byte[] b = new byte[patchNameSize / 2];
            for (int i = 0; i < b.length; i++) {
                b[i] = (byte) (DX7FamilyByteEncoding.AsciiHex2Value(ip.sysex[16 + 2 * (96 + i)]) * 16 + DX7FamilyByteEncoding.AsciiHex2Value(ip.sysex[16 + 2 * (96 + i) + 1]));
            }
            StringBuffer s = new StringBuffer(new String(b, 0, patchNameSize / 2, "US-ASCII"));
            return s.toString();
        } catch (Exception ex) {
            return "-";
        }
    }

    public void setPatchName(Patch p, String name) {
        byte[] namebytes = new byte[patchNameSize / 2];
        try {
            while (name.length() < patchNameSize / 2) {
                name = name + " ";
            }
            namebytes = name.getBytes("US-ASCII");
            for (int i = 0; i < namebytes.length; i++) {
                ((Patch) p).sysex[16 + 2 * (96 + i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(namebytes[i]));
                ((Patch) p).sysex[16 + 2 * (96 + i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(namebytes[i]));
            }
        } catch (Exception e) {
        }
        calculateChecksum(p);
    }
}
