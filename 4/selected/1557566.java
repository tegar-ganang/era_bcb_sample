package org.jsynthlib.synthdrivers.KawaiK5000;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class KawaiK5000CombiDriver extends Driver {

    static final int PATCH_DATA_START = 8;

    static final int PATCH_DATA_SIZE = 103;

    static final int PATCH_NAME_START = 47;

    static final int PATCH_NAME_SIZE = 8;

    static final String[] BANK_NUMBERS = new String[] { "Multi" };

    static final String[] PATCH_NUMBERS = new String[] { "01-", "02-", "03-", "04-", "05-", "06-", "07-", "08-", "09-", "10-", "11-", "12-", "13-", "14-", "15-", "16-", "17-", "18-", "19-", "20-", "21-", "22-", "23-", "24-", "25-", "26-", "27-", "28-", "29-", "30-", "31-", "32-", "33-", "34-", "35-", "36-", "37-", "38-", "39-", "40-", "41-", "42-", "43-", "44-", "45-", "46-", "47-", "48-", "49-", "50-", "51-", "52-", "53-", "54-", "55-", "56-", "57-", "58-", "59-", "60-", "61-", "62-", "63-", "64-" };

    static final byte[] BSYSEX_HEADER = (new SysexHandler("F0 40 00 20 00 0A 20 00")).toByteArray();

    static final SysexHandler SYSEX_REQUEST_DUMP = new SysexHandler("F0 40 @@ 00 00 0A 20 00 *patchNum* F7");

    public KawaiK5000CombiDriver(final Device device) {
        super(device, "Combi", "Phil Shepherd");
        sysexID = "F040**20000A20";
        sysexRequestDump = SYSEX_REQUEST_DUMP;
        patchSize = 0;
        patchNameStart = PATCH_NAME_START;
        patchNameSize = PATCH_NAME_SIZE;
        deviceIDoffset = 2;
        checksumStart = PATCH_DATA_START + 1;
        checksumEnd = PATCH_DATA_START + PATCH_DATA_SIZE - 1;
        checksumOffset = PATCH_DATA_START;
        bankNumbers = BANK_NUMBERS;
        patchNumbers = PATCH_NUMBERS;
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        Logger.reportStatus("KawaiK5000CombiDriver->storePatch: " + bankNum + " | " + patchNum);
        setBankNum(bankNum);
        setPatchNum(patchNum);
        try {
            Thread.sleep(300);
        } catch (Exception e) {
        }
        ((Patch) p).sysex[3] = (byte) 0x20;
        ((Patch) p).sysex[7] = (byte) (patchNum);
        sendPatchWorker(p);
        try {
            Thread.sleep(300);
        } catch (Exception e) {
        }
        setPatchNum(patchNum);
    }

    public void sendPatch(Patch p) {
        storePatch(p, 0, 0);
    }

    public void setBankNum(int bankNum) {
        try {
            send(0xB0 + (getChannel() - 1), 0x00, 0x65);
            send(0xB0 + (getChannel() - 1), 0x20, 0);
        } catch (Exception e) {
        }
    }

    protected void calculateChecksum(Patch p, int start, int end, int ofs) {
        int sum = 0;
        for (int i = start; i <= end; i++) sum += p.sysex[i];
        sum += (byte) 0xA5;
        p.sysex[ofs] = (byte) (sum % 128);
    }

    public Patch createNewPatch() {
        Patch p = createPatchFromData(new byte[PATCH_DATA_SIZE], 0, PATCH_DATA_SIZE);
        p.setName("New Patch");
        p.calculateChecksum();
        return p;
    }

    public static Patch createPatchFromData(byte[] data, int dataOffset, int dataLength) {
        byte[] sysex = new byte[dataLength + BSYSEX_HEADER.length + 1];
        System.arraycopy(BSYSEX_HEADER, 0, sysex, 0, BSYSEX_HEADER.length);
        System.arraycopy(data, dataOffset, sysex, BSYSEX_HEADER.length, dataLength);
        sysex[sysex.length - 1] = (byte) 0xF7;
        Patch p = new Patch(sysex);
        p.calculateChecksum();
        return p;
    }
}
