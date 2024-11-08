package org.jsynthlib.synthdrivers.SCIProphet600;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class P600ProgSingleDriver extends Driver {

    static final String BANK_LIST[] = new String[] { "User" };

    static final String PATCH_LIST[] = new String[] { "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99" };

    static final int PATCH_NUM_OFFSET = 3;

    static final byte NEW_PATCH[] = { (byte) 0xF0, (byte) 0x01, (byte) 0x02, (byte) 0x63, (byte) 0x03, (byte) 0x0B, (byte) 0x0F, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x03, (byte) 0x06, (byte) 0x08, (byte) 0x01, (byte) 0x08, (byte) 0x04, (byte) 0x08, (byte) 0x0F, (byte) 0x07, (byte) 0x02, (byte) 0x0E, (byte) 0x07, (byte) 0x0F, (byte) 0x0D, (byte) 0x00, (byte) 0x04, (byte) 0x0E, (byte) 0x0F, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x09, (byte) 0x02, (byte) 0x05, (byte) 0x03, (byte) 0x00, (byte) 0x0F7 };

    public P600ProgSingleDriver(final Device device) {
        super(device, "Prog Single", "Kenneth L. Martinez");
        sysexID = "F00102**";
        sysexRequestDump = new SysexHandler("F0 01 00 *patchNum* F7");
        patchSize = 37;
        patchNameStart = -1;
        patchNameSize = 0;
        deviceIDoffset = -1;
        bankNumbers = BANK_LIST;
        patchNumbers = PATCH_LIST;
    }

    public void calculateChecksum(Patch p) {
    }

    public String getPatchName(Patch p) {
        return "prog" + p.sysex[PATCH_NUM_OFFSET];
    }

    public void setPatchName(Patch p, String name) {
    }

    public void sendPatch(Patch p) {
        sendPatch(p, 0, 99);
    }

    public void sendPatch(Patch p, int bankNum, int patchNum) {
        Patch p2 = new Patch(p.sysex);
        p2.sysex[PATCH_NUM_OFFSET] = 99;
        sendPatchWorker(p2);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        sendPatch(p, bankNum, patchNum);
    }

    protected void playPatch(Patch p) {
        byte sysex[] = new byte[patchSize];
        System.arraycopy(p.sysex, 0, sysex, 0, patchSize);
        sysex[PATCH_NUM_OFFSET] = 99;
        Patch p2 = new Patch(sysex);
        try {
            Thread.sleep(50);
            sendPatch(p2);
            Thread.sleep(50);
            send(0xC0 + getChannel() - 1, 99);
            Thread.sleep(50);
            super.playPatch(p2);
        } catch (Exception e) {
            Logger.reportStatus(e);
        }
    }

    public Patch createNewPatch() {
        return new Patch(NEW_PATCH, this);
    }

    public JSLFrame editPatch(Patch p) {
        return new P600ProgSingleEditor(p);
    }
}
