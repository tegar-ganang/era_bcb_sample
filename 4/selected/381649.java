package org.jsynthlib.synthdrivers.YamahaDX7.common;

import java.io.UnsupportedEncodingException;
import org.jsynthlib.core.BankDriver;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class DX7FamilyPerformanceIIBankDriver extends BankDriver {

    byte[] initSysex;

    String[] dxPatchNumbers;

    String[] dxBankNumbers;

    private static final int dxPatchNameSize = 20;

    private static final int dxPatchNameOffset = 31;

    private static final int dxSinglePackedSize = 51;

    private static final int dxSysexHeaderSize = 16;

    public DX7FamilyPerformanceIIBankDriver(final Device device, byte[] initSysex, String[] dxPatchNumbers, String[] dxBankNumbers) {
        super(device, "Performance Bank", "Torsten Tittmann", dxPatchNumbers.length, 4);
        this.initSysex = initSysex;
        this.dxPatchNumbers = dxPatchNumbers;
        this.dxBankNumbers = dxBankNumbers;
        sysexID = "f0430*7e0c6a4c4d202038393733504d";
        sysexRequestDump = new SysexHandler("f0 43 @@ 7e 4c 4d 20 20 38 39 37 33 50 4d f7");
        deviceIDoffset = 2;
        patchNameStart = 0;
        patchNameSize = 0;
        patchNumbers = dxPatchNumbers;
        bankNumbers = dxBankNumbers;
        singleSysexID = "F0430*7E003d4c4d2020383937335045";
        singleSize = 69;
        checksumOffset = 1648;
        checksumStart = 6;
        checksumEnd = 1647;
        patchSize = 1650;
        trimSize = patchSize;
    }

    public int getPatchStart(int patchNum) {
        return (dxSinglePackedSize * patchNum) + dxSysexHeaderSize;
    }

    public int getPatchNameStart(int patchNum) {
        return getPatchStart(patchNum) + dxPatchNameOffset;
    }

    public String getPatchName(Patch p, int patchNum) {
        int nameStart = getPatchNameStart(patchNum);
        try {
            return new String(p.sysex, nameStart, dxPatchNameSize, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            return "-";
        }
    }

    public void setPatchName(Patch p, int patchNum, String name) {
        int nameStart = getPatchNameStart(patchNum);
        while (name.length() < dxPatchNameSize) {
            name += " ";
        }
        try {
            byte[] namebytes = name.getBytes("US-ASCII");
            System.arraycopy(namebytes, 0, p.sysex, nameStart, dxPatchNameSize);
        } catch (UnsupportedEncodingException ex) {
            return;
        }
    }

    public void putPatch(Patch bank, Patch p, int patchNum) {
        if (!canHoldPatch(p)) {
            DX7FamilyStrings.dxShowError(toString(), "This type of patch does not fit in to this type of bank.");
            return;
        }
        for (int i = 0; i < 51; i++) {
            bank.sysex[getPatchStart(patchNum) + i] = p.sysex[16 + i];
        }
        calculateChecksum(bank);
    }

    public Patch getPatch(Patch bank, int patchNum) {
        try {
            byte[] sysex = new byte[singleSize];
            sysex[0] = (byte) 0xf0;
            sysex[1] = (byte) 0x43;
            sysex[2] = (byte) 0x00;
            sysex[3] = (byte) 0x7e;
            sysex[4] = (byte) 0x00;
            sysex[5] = (byte) 0x3d;
            sysex[6] = (byte) 0x4c;
            sysex[7] = (byte) 0x4d;
            sysex[8] = (byte) 0x20;
            sysex[9] = (byte) 0x20;
            sysex[10] = (byte) 0x38;
            sysex[11] = (byte) 0x39;
            sysex[12] = (byte) 0x37;
            sysex[13] = (byte) 0x33;
            sysex[14] = (byte) 0x50;
            sysex[15] = (byte) 0x45;
            sysex[singleSize - 1] = (byte) 0xf7;
            for (int i = 0; i < 51; i++) {
                sysex[16 + i] = bank.sysex[getPatchStart(patchNum) + i];
            }
            Patch p = new Patch(sysex, getDevice());
            p.calculateChecksum();
            return p;
        } catch (Exception e) {
            Logger.reportError(getManufacturerName() + " " + getModelName(), "Error in " + toString(), e);
            return null;
        }
    }

    public Patch createNewPatch() {
        byte[] sysex = new byte[trimSize];
        sysex[0] = (byte) 0xf0;
        sysex[1] = (byte) 0x43;
        sysex[2] = (byte) 0x00;
        sysex[3] = (byte) 0x7e;
        sysex[4] = (byte) 0x0c;
        sysex[5] = (byte) 0x6a;
        sysex[6] = (byte) 0x4c;
        sysex[7] = (byte) 0x4d;
        sysex[8] = (byte) 0x20;
        sysex[9] = (byte) 0x20;
        sysex[10] = (byte) 0x38;
        sysex[11] = (byte) 0x39;
        sysex[12] = (byte) 0x37;
        sysex[13] = (byte) 0x33;
        sysex[14] = (byte) 0x50;
        sysex[15] = (byte) 0x4d;
        sysex[trimSize - 1] = (byte) 0xf7;
        Patch v = new Patch(initSysex, getDevice());
        Patch p = new Patch(sysex, this);
        for (int i = 0; i < getNumPatches(); i++) {
            putPatch(p, v, i);
        }
        calculateChecksum(p);
        return p;
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
