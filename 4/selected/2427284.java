package synthdrivers.KawaiK1;

import core.Driver;
import core.DriverUtil;
import core.JSLFrame;
import core.Patch;
import core.SysexHandler;

/**
 * Single Voice Patch Driver for Kawai K1.
 *
 * @version $Id: KawaiK1SingleDriver.java 939 2005-03-03 04:05:40Z hayashi $
 */
public class KawaiK1SingleDriver extends Driver {

    /** Header Size */
    private static final int HSIZE = 8;

    /** Single Patch size */
    private static final int SSIZE = 88;

    private static final SysexHandler SYS_REQ = new SysexHandler("F0 40 @@ 00 00 03 *bankNum* *patchNum* F7");

    public KawaiK1SingleDriver() {
        super("Single", "John McCabe");
        sysexID = "F040**2*0003";
        patchSize = HSIZE + SSIZE + 1;
        patchNameStart = HSIZE;
        patchNameSize = 10;
        deviceIDoffset = 2;
        checksumStart = HSIZE;
        checksumEnd = HSIZE + SSIZE - 2;
        checksumOffset = HSIZE + SSIZE - 1;
        bankNumbers = new String[] { "0-Internal", "1-External" };
        patchNumbers = new String[2 * 4 * 8];
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "A-#"), 0, patchNumbers, 0, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "B-#"), 0, patchNumbers, 8, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "C-#"), 0, patchNumbers, 16, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "D-#"), 0, patchNumbers, 24, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "a-#"), 0, patchNumbers, 32, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "b-#"), 0, patchNumbers, 40, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "c-#"), 0, patchNumbers, 48, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "d-#"), 0, patchNumbers, 56, 8);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        setBankNum(bankNum);
        setPatchNum(patchNum);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
        p.sysex[3] = (byte) 0x20;
        p.sysex[6] = (byte) (bankNum << 1);
        p.sysex[7] = (byte) (patchNum);
        sendPatchWorker(p);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
        setPatchNum(patchNum);
    }

    public void sendPatch(Patch p) {
        storePatch(p, 0, 63);
    }

    protected void calculateChecksum(Patch p, int start, int end, int ofs) {
        int sum = 0;
        for (int i = start; i <= end; i++) {
            sum += p.sysex[i];
        }
        sum += 0xA5;
        p.sysex[ofs] = (byte) (sum % 128);
    }

    public Patch createNewPatch() {
        byte[] sysex = new byte[HSIZE + SSIZE + 1];
        sysex[0] = (byte) 0xF0;
        sysex[1] = (byte) 0x40;
        sysex[2] = (byte) 0x00;
        sysex[3] = (byte) 0x20;
        sysex[4] = (byte) 0x00;
        sysex[5] = (byte) 0x03;
        sysex[6] = (byte) 0x00;
        sysex[7] = (byte) 0x3F;
        sysex[HSIZE + SSIZE] = (byte) 0xF7;
        Patch p = new Patch(sysex, this);
        setPatchName(p, "New Patch");
        calculateChecksum(p);
        return p;
    }

    public JSLFrame editPatch(Patch p) {
        return null;
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("bankNum", bankNum << 1), new SysexHandler.NameValue("patchNum", patchNum)));
    }
}
