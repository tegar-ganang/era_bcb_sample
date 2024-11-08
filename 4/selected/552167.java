package org.jsynthlib.synthdrivers.RolandMT32;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

/**
 * Patch Memory Driver for Roland MT32.
 */
public class RolandMT32PatchMemoryDriver extends Driver {

    /** Header Size of the Data set DT1 message. */
    private static final int HSIZE = 5;

    /** Single Patch size */
    private static final int SSIZE = 0x0B;

    /** Definition of the Request message RQ1 */
    private static final SysexHandler SYS_REQ = new SysexHandler("F0 41 10 16 11 05 *partAddrM* *partAddrL* 00 00 07 *checkSum* F7");

    public RolandMT32PatchMemoryDriver(final Device device) {
        super(device, "Patch Memory", "Fred Jan Kraan");
        sysexID = "F041**16";
        patchSize = HSIZE + SSIZE + 1;
        patchNameStart = 0;
        patchNameSize = 0;
        deviceIDoffset = 0;
        checksumStart = 5;
        checksumEnd = 10;
        checksumOffset = 0;
        bankNumbers = new String[] { "" };
        int PMCount = 128;
        patchNumbers = new String[PMCount];
        for (int i = 0; i < PMCount; i++) {
            patchNumbers[i] = ("PMA-" + (i + 1));
        }
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        setBankNum(bankNum);
        setPatchNum(patchNum);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            Logger.reportStatus(e);
        }
        int patchAddr = patchNum * 0x08;
        int patAddrM = patchAddr / 0x80;
        int patAddrL = patchAddr & 0x7F;
        p.sysex[0] = (byte) 0xF0;
        p.sysex[5] = (byte) 0x05;
        p.sysex[6] = (byte) patAddrM;
        p.sysex[7] = (byte) patAddrL;
        calculateChecksum(p, HSIZE, HSIZE + SSIZE - 2, HSIZE + SSIZE - 1);
        Logger.reportStatus("Store patchNum " + patchNum + " to patAddrM/L " + patAddrM + " / " + patAddrL);
        try {
            sendPatchWorker(p);
            Thread.sleep(100);
        } catch (Exception e) {
            Logger.reportStatus(e);
        }
        setPatchNum(patchNum);
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
        sysex[5] = (byte) 0x05;
        sysex[6] = (byte) 0x0;
        sysex[HSIZE + SSIZE] = (byte) 0xF7;
        Patch p = new Patch(sysex, this);
        calculateChecksum(p);
        return p;
    }

    public JSLFrame editPatch(Patch p) {
        return new RolandMT32PatchMemoryEditor(p);
    }

    public void requestPatchDump(int bankNum, int patNum) {
        int patchAddr = patNum * 0x08;
        int patAddrH = 0x05;
        int patAddrM = patchAddr / 0x80;
        int patAddrL = patchAddr & 0x7F;
        int patSizeH = 0x00;
        int patSizeM = 0x00;
        int patSizeL = 0x07;
        int checkSum = (0 - (patAddrH + patAddrM + patAddrL + patSizeH + patSizeM + patSizeL)) & 0x7F;
        SysexHandler.NameValue nVs[] = new SysexHandler.NameValue[3];
        nVs[0] = new SysexHandler.NameValue("partAddrM", patAddrM);
        nVs[1] = new SysexHandler.NameValue("partAddrL", patAddrL);
        nVs[2] = new SysexHandler.NameValue("checkSum", checkSum);
        send(SYS_REQ.toSysexMessage(getChannel(), nVs));
    }
}
