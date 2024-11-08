package org.jsynthlib.synthdrivers.RolandMT32;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

/**
 * System Driver for Roland MT32.
 */
public class RolandMT32SystemDriver extends Driver {

    private static final int HSIZE = 5;

    /** Single Patch size */
    private static final int SSIZE = 3 + 0x17 + 1;

    /** Definition of the Request message RQ1 */
    private static final SysexHandler SYS_REQ = new SysexHandler("F0 41 10 16 11 10 00 00 00 00 17 *checkSum* F7");

    public RolandMT32SystemDriver(final Device device) {
        super(device, "System", "Fred Jan Kraan");
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
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            Logger.reportStatus(e);
        }
        p.sysex[0] = (byte) 0xF0;
        p.sysex[5] = (byte) 0x10;
        p.sysex[6] = (byte) 0x00;
        p.sysex[7] = (byte) 0x00;
        calculateChecksum(p, HSIZE, HSIZE + SSIZE - 2, HSIZE + SSIZE - 1);
        try {
            sendPatchWorker(p);
            Thread.sleep(100);
        } catch (Exception e) {
            Logger.reportStatus(e);
        }
    }

    public void sendPatch(Patch p) {
        try {
            sendPatchWorker(p);
            Thread.sleep(100);
        } catch (Exception e) {
            Logger.reportStatus(e);
        }
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
        sysex[5] = (byte) 0x10;
        sysex[6] = (byte) 0x00;
        sysex[7] = (byte) 0x00;
        sysex[HSIZE + 0] = (byte) 0x28;
        sysex[HSIZE + 1] = (byte) 0x00;
        sysex[HSIZE + 2] = (byte) 0x05;
        sysex[HSIZE + 3] = (byte) 0x03;
        sysex[HSIZE + 22] = (byte) 0x3F;
        sysex[HSIZE + 4] = (byte) 0x03;
        sysex[HSIZE + 5] = (byte) 0x0A;
        sysex[HSIZE + 6] = (byte) 0x06;
        sysex[HSIZE + 7] = (byte) 0x04;
        sysex[HSIZE + 8] = (byte) 0x03;
        sysex[HSIZE + 9] = (byte) 0x00;
        sysex[HSIZE + 10] = (byte) 0x00;
        sysex[HSIZE + 11] = (byte) 0x00;
        sysex[HSIZE + 12] = (byte) 0x06;
        sysex[HSIZE + 13] = (byte) 0x01;
        sysex[HSIZE + 14] = (byte) 0x02;
        sysex[HSIZE + 15] = (byte) 0x03;
        sysex[HSIZE + 16] = (byte) 0x04;
        sysex[HSIZE + 17] = (byte) 0x05;
        sysex[HSIZE + 18] = (byte) 0x06;
        sysex[HSIZE + 19] = (byte) 0x07;
        sysex[HSIZE + 20] = (byte) 0x08;
        sysex[HSIZE + 21] = (byte) 0x09;
        sysex[HSIZE + SSIZE] = (byte) 0xF7;
        Patch p = new Patch(sysex, this);
        calculateChecksum(p, 5, HSIZE + SSIZE - 2, HSIZE + SSIZE - 1);
        return p;
    }

    public JSLFrame editPatch(Patch p) {
        return new RolandMT32SystemEditor(p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("checkSum", 0x59)));
    }
}
