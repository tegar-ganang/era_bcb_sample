package synthdrivers.YamahaDX7.common;

import java.io.UnsupportedEncodingException;
import core.BankDriver;
import core.ErrorMsg;
import core.Patch;
import core.SysexHandler;

public class DX7FamilyPerformanceIIIBankDriver extends BankDriver {

    byte[] initSysex;

    String[] dxPatchNumbers;

    String[] dxBankNumbers;

    private static final int dxPatchNameSize = 20;

    private static final int dxPatchNameOffset = 12 + (2 * 64);

    private static final int dxSinglePackedSize = 3 + 10 + (2 * 84);

    private static final int dxSysexHeaderSize = 4;

    public DX7FamilyPerformanceIIIBankDriver(byte[] initSysex, String[] dxPatchNumbers, String[] dxBankNumbers) {
        super("Performance Bank", "Torsten Tittmann", dxPatchNumbers.length, 4);
        this.initSysex = initSysex;
        this.dxPatchNumbers = dxPatchNumbers;
        this.dxBankNumbers = dxBankNumbers;
        sysexID = "F0430*7E01284C4D202038393532504d";
        sysexRequestDump = new SysexHandler("F0 43 @@ 7E 4C 4D 20 20 38 39 35 32 50 4D F7");
        deviceIDoffset = 2;
        patchNameStart = 0;
        patchNameSize = 0;
        bankNumbers = dxBankNumbers;
        patchNumbers = dxPatchNumbers;
        singleSysexID = "F0430*7E01684C4D2020383935325045";
        singleSize = 250;
        numSysexMsgs = 1;
        patchSize = 11589;
        trimSize = patchSize;
    }

    public void calculateChecksum(Patch p) {
    }

    public int getPatchStart(int patchNum) {
        return (dxSinglePackedSize * patchNum) + dxSysexHeaderSize;
    }

    public int getPatchNameStart(int patchNum) {
        return getPatchStart(patchNum) + dxPatchNameOffset;
    }

    public String getPatchName(Patch p, int patchNum) {
        int patchNameStart = getPatchNameStart(patchNum);
        try {
            byte[] b = new byte[dxPatchNameSize];
            for (int i = 0; i < b.length; i++) {
                b[i] = (byte) (DX7FamilyByteEncoding.AsciiHex2Value(((Patch) p).sysex[patchNameStart + (2 * i)]) * 16 + DX7FamilyByteEncoding.AsciiHex2Value(((Patch) p).sysex[patchNameStart + (2 * i) + 1]));
            }
            StringBuffer s = new StringBuffer(new String(b, 0, dxPatchNameSize, "US-ASCII"));
            return s.toString();
        } catch (Exception ex) {
            return "-";
        }
    }

    public void setPatchName(Patch p, int patchNum, String name) {
        int patchNameStart = getPatchNameStart(patchNum);
        while (name.length() < dxPatchNameSize) name = name + " ";
        byte[] namebytes = new byte[dxPatchNameSize];
        try {
            namebytes = name.getBytes("US-ASCII");
            for (int i = 0; i < dxPatchNameSize; i++) {
                ((Patch) p).sysex[patchNameStart + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(namebytes[i]));
                ((Patch) p).sysex[patchNameStart + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(namebytes[i]));
            }
        } catch (UnsupportedEncodingException ex) {
            return;
        }
    }

    public int getByte(Patch p, int index) {
        return (DX7FamilyByteEncoding.AsciiHex2Value(p.sysex[index]) * 16 + DX7FamilyByteEncoding.AsciiHex2Value(p.sysex[index + 1]));
    }

    public void putPatch(Patch bank, Patch ip, int patchNum) {
        Patch p = (Patch) ip;
        if (!canHoldPatch(p)) {
            DX7FamilyStrings.dxShowError(toString(), "This type of patch does not fit in to this type of bank.");
            return;
        }
        int value;
        ((Patch) bank).sysex[getPatchStart(patchNum) + 0] = (byte) (0x01);
        ((Patch) bank).sysex[getPatchStart(patchNum) + 1] = (byte) (0x28);
        ((Patch) bank).sysex[getPatchStart(patchNum) + 2] = (byte) (0x4C);
        ((Patch) bank).sysex[getPatchStart(patchNum) + 3] = (byte) (0x4D);
        ((Patch) bank).sysex[getPatchStart(patchNum) + 4] = (byte) (0x20);
        ((Patch) bank).sysex[getPatchStart(patchNum) + 5] = (byte) (0x20);
        ((Patch) bank).sysex[getPatchStart(patchNum) + 6] = (byte) (0x38);
        ((Patch) bank).sysex[getPatchStart(patchNum) + 7] = (byte) (0x39);
        ((Patch) bank).sysex[getPatchStart(patchNum) + 8] = (byte) (0x35);
        ((Patch) bank).sysex[getPatchStart(patchNum) + 9] = (byte) (0x32);
        ((Patch) bank).sysex[getPatchStart(patchNum) + 10] = (byte) (0x50);
        ((Patch) bank).sysex[getPatchStart(patchNum) + 11] = (byte) (0x4D);
        for (int i = 0; i < 8; i++) {
            value = getByte(p, 16 + (2 * 0) + (2 * i)) * 32 + getByte(p, 16 + (2 * 8) + (2 * i));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 0) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 0) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
        }
        for (int i = 0; i < 8; i++) {
            value = getByte(p, 16 + (2 * 16) + (2 * i));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 8) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 8) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
        }
        for (int i = 0; i < 8; i++) {
            value = getByte(p, 16 + (2 * 88) + (2 * i));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 16) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 16) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
        }
        for (int i = 0; i < 8; i++) {
            value = getByte(p, 16 + (2 * 32) + (2 * i));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 24) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 24) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
        }
        for (int i = 0; i < 8; i++) {
            value = getByte(p, 16 + (2 * 24) + (2 * i)) * 16 + getByte(p, 16 + (2 * 80) + (2 * i)) * 8 + getByte(p, 16 + (2 * 40) + (2 * i));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 32) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 32) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
        }
        for (int i = 0; i < 8; i++) {
            value = getByte(p, 16 + (2 * 48) + (2 * i));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 40) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 40) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
        }
        for (int i = 0; i < 8; i++) {
            value = getByte(p, 16 + (2 * 56) + (2 * i));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 48) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 48) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
        }
        for (int i = 0; i < 8; i++) {
            value = getByte(p, 16 + (2 * 72) + (2 * i)) * 64 + getByte(p, 16 + (2 * 64) + (2 * i));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 56) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 56) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
        }
        for (int i = 0; i < 20; i++) {
            value = getByte(p, 16 + (2 * 96) + (2 * i));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 64) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
            ((Patch) bank).sysex[getPatchStart(patchNum) + 12 + (2 * 64) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
        }
        calculateChecksum(bank, getPatchStart(patchNum) + 2, getPatchStart(patchNum) + 179, getPatchStart(patchNum) + 180);
    }

    public Patch getPatch(Patch b, int patchNum) {
        try {
            Patch bank = (Patch) b;
            byte[] sysex = new byte[singleSize];
            int value;
            sysex[0] = (byte) 0xF0;
            sysex[1] = (byte) 0x43;
            sysex[2] = (byte) 0x00;
            sysex[3] = (byte) 0x7E;
            sysex[4] = (byte) 0x01;
            sysex[5] = (byte) 0x68;
            sysex[6] = (byte) 0x4C;
            sysex[7] = (byte) 0x4D;
            sysex[8] = (byte) 0x20;
            sysex[9] = (byte) 0x20;
            sysex[10] = (byte) 0x38;
            sysex[11] = (byte) 0x39;
            sysex[12] = (byte) 0x35;
            sysex[13] = (byte) 0x32;
            sysex[14] = (byte) 0x50;
            sysex[15] = (byte) 0x45;
            sysex[singleSize - 1] = (byte) 0xF7;
            for (int i = 0; i < 8; i++) {
                value = (getByte(bank, getPatchStart(patchNum) + 12 + (2 * 0) + (2 * i)) & 224) / 32;
                sysex[16 + (2 * 0) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
                sysex[16 + (2 * 0) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
            }
            for (int i = 0; i < 8; i++) {
                value = (getByte(bank, getPatchStart(patchNum) + 12 + (2 * 0) + (2 * i)) & 31);
                sysex[16 + (2 * 8) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
                sysex[16 + (2 * 8) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
            }
            for (int i = 0; i < 8; i++) {
                value = (getByte(bank, getPatchStart(patchNum) + 12 + (2 * 8) + (2 * i)) & 255);
                sysex[16 + (2 * 16) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
                sysex[16 + (2 * 16) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
            }
            for (int i = 0; i < 8; i++) {
                value = (getByte(bank, getPatchStart(patchNum) + 12 + (2 * 32) + (2 * i)) & 112) / 16;
                sysex[16 + (2 * 24) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
                sysex[16 + (2 * 24) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
            }
            for (int i = 0; i < 8; i++) {
                value = (getByte(bank, getPatchStart(patchNum) + 12 + (2 * 24) + (2 * i)) & 127);
                sysex[16 + (2 * 32) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
                sysex[16 + (2 * 32) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
            }
            for (int i = 0; i < 8; i++) {
                value = (getByte(bank, getPatchStart(patchNum) + 12 + (2 * 32) + (2 * i)) & 7);
                sysex[16 + (2 * 40) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
                sysex[16 + (2 * 40) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
            }
            for (int i = 0; i < 8; i++) {
                value = (getByte(bank, getPatchStart(patchNum) + 12 + (2 * 40) + (2 * i)) & 127);
                sysex[16 + (2 * 48) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
                sysex[16 + (2 * 48) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
            }
            for (int i = 0; i < 8; i++) {
                value = (getByte(bank, getPatchStart(patchNum) + 12 + (2 * 48) + (2 * i)) & 127);
                sysex[16 + (2 * 56) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
                sysex[16 + (2 * 56) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
            }
            for (int i = 0; i < 8; i++) {
                value = (getByte(bank, getPatchStart(patchNum) + 12 + (2 * 56) + (2 * i)) & 63);
                sysex[16 + (2 * 64) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
                sysex[16 + (2 * 64) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
            }
            for (int i = 0; i < 8; i++) {
                value = (getByte(bank, getPatchStart(patchNum) + 12 + (2 * 56) + (2 * i)) & 64) / 64;
                sysex[16 + (2 * 72) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
                sysex[16 + (2 * 72) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
            }
            for (int i = 0; i < 8; i++) {
                value = (getByte(bank, getPatchStart(patchNum) + 12 + (2 * 32) + (2 * i)) & 8) / 8;
                sysex[16 + (2 * 80) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
                sysex[16 + (2 * 80) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
            }
            for (int i = 0; i < 8; i++) {
                value = (getByte(bank, getPatchStart(patchNum) + 12 + (2 * 16) + (2 * i)) & 255);
                sysex[16 + (2 * 88) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
                sysex[16 + (2 * 88) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
            }
            for (int i = 0; i < 20; i++) {
                value = (getByte(bank, getPatchStart(patchNum) + 12 + (2 * 64) + (2 * i)) & 255);
                sysex[16 + (2 * 96) + (2 * i)] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexHigh(value));
                sysex[16 + (2 * 96) + (2 * i) + 1] = (byte) (DX7FamilyByteEncoding.Value2AsciiHexLow(value));
            }
            Patch p = new Patch(sysex, getDevice());
            p.calculateChecksum();
            return p;
        } catch (Exception e) {
            ErrorMsg.reportError(getManufacturerName() + " " + getModelName(), "Error in " + toString(), e);
            return null;
        }
    }

    public Patch createNewPatch() {
        byte[] sysex = new byte[trimSize];
        sysex[0] = (byte) 0xF0;
        sysex[1] = (byte) 0x43;
        sysex[2] = (byte) 0x00;
        sysex[3] = (byte) 0x7E;
        sysex[trimSize - 1] = (byte) 0xF7;
        Patch v = new Patch(initSysex, getDevice());
        Patch p = new Patch(sysex, this);
        for (int i = 0; i < getNumPatches(); i++) putPatch(p, v, i);
        return p;
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(sysexRequestDump.toSysexMessage(getChannel() + 0x20));
    }
}
