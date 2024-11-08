package org.jsynthlib.synthdrivers.RolandMT32;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

/**
 * (Rhythm) Setup Temp Driver for Roland MT32.
 */
public class RolandMT32RhythmSetupTempDriver extends Driver {

    private static final int HSIZE = 0x05;

    /** Single Patch size */
    private static final int SSIZE = 0x08;

    private static final SysexHandler SYS_REQ = new SysexHandler("F0 41 10 16 11 03 01 10 00 00 04 *checkSum* F7");

    public RolandMT32RhythmSetupTempDriver(final Device device) {
        super(device, "Rhythm Setup Temp", "Fred Jan Kraan");
        sysexID = "F041**16";
        patchSize = HSIZE + SSIZE + 1;
        patchNameStart = 0;
        patchNameSize = 0;
        deviceIDoffset = 0;
        checksumStart = 5;
        checksumEnd = 10;
        checksumOffset = 0;
        bankNumbers = new String[] { "" };
        patchNumbers = new String[] { "" };
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        p.sysex[0] = (byte) 0xF0;
        p.sysex[5] = (byte) 0x03;
        p.sysex[6] = (byte) 0x01;
        p.sysex[7] = (byte) 0x10;
        calculateChecksum(p, HSIZE, HSIZE + SSIZE - 2, HSIZE + SSIZE - 1);
        try {
            sendPatchWorker(p);
            Thread.sleep(100);
        } catch (Exception e) {
            Logger.reportStatus(e);
        }
    }

    public void sendPatch(Patch p) {
        sendPatchWorker(p);
    }

    protected void calculateChecksum(Patch p, int start, int end, int ofs) {
        int sum = 0;
        for (int i = start; i <= end; i++) {
            sum += p.sysex[i];
        }
        sum = (0 - sum) & 0x7F;
        p.sysex[ofs] = (byte) (sum % 128);
    }

    public Patch createNewPatch() {
        byte[] sysex = new byte[HSIZE + SSIZE + 1];
        sysex[0] = (byte) 0xF0;
        sysex[1] = (byte) 0x41;
        sysex[2] = (byte) 0x10;
        sysex[3] = (byte) 0x16;
        sysex[4] = (byte) 0x12;
        sysex[5] = (byte) 0x03;
        sysex[6] = (byte) 0x01;
        sysex[7] = (byte) 0x10;
        sysex[HSIZE + 0] = (byte) 0x41;
        sysex[HSIZE + 1] = (byte) 0x50;
        sysex[HSIZE + 2] = (byte) 0x07;
        sysex[HSIZE + 3] = (byte) 0x00;
        sysex[HSIZE + SSIZE] = (byte) 0xF7;
        Patch p = new Patch(sysex, this);
        calculateChecksum(p);
        return p;
    }

    public JSLFrame editPatch(Patch p) {
        return new RolandMT32RhythmSetupTempEditor(p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("checkSum", 0x68)));
    }
}
