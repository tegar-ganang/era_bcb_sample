package org.jsynthlib.synthdrivers.Yamaha01v;

import java.io.InputStream;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.DriverUtil;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class Yamaha01vEffectDriver extends Driver {

    private static final SysexHandler SYS_REQ = new SysexHandler("F0 43 *ID* 7E 4C 4D 20 20 38 42 33 34 45 *patchNum* F7");

    public Yamaha01vEffectDriver(final Device device) {
        super(device, "Effect Library", "Robert Wirski");
        sysexID = "F0430*7E00684C4D20203842333445";
        patchSize = 112;
        patchNameStart = 16;
        patchNameSize = 12;
        deviceIDoffset = 2;
        checksumStart = 6;
        checksumOffset = 110;
        checksumEnd = 109;
        bankNumbers = new String[] { "" };
        patchNumbers = new String[101];
        System.arraycopy(DriverUtil.generateNumbers(1, 99, "Lib 00"), 0, patchNumbers, 0, 99);
        patchNumbers[99] = "EFFECT 1";
        patchNumbers[100] = "EFFECT 2";
    }

    /**
     * Correct the patchNumber value due to the fact, that
     * there is a hole in the patch numbers in Equalizer
     * Library dump request.<p>
     */
    protected int correctPatchNumber(int patchNumber) {
        if (patchNumber > 98) {
            patchNumber += 13;
        }
        return patchNumber;
    }

    /**
     * Sends a patch to a set location on a synth.<p>
     *
     * @see Patch#send(int, int)
     */
    protected void storePatch(Patch p, int bankNum, int patchNum) {
        patchNum = correctPatchNumber(patchNum);
        setPatchNum(patchNum);
        setBankNum(0);
        p.sysex[15] = (byte) patchNum;
        calculateChecksum(p);
        sendPatchWorker(p);
    }

    /**
     * @see core.Driver#createNewPatch()
     */
    public Patch createNewPatch() {
        byte[] sysex = new byte[patchSize];
        Patch p;
        try {
            InputStream fileIn = getClass().getResourceAsStream("01v_Effect.syx");
            fileIn.read(sysex);
            fileIn.close();
        } catch (Exception e) {
            System.err.println("Unable to find 01v_Effect.syx.");
        }
        p = new Patch(sysex, this);
        return p;
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("ID", getDeviceID() + 0x1F), new SysexHandler.NameValue("patchNum", correctPatchNumber(patchNum))));
    }
}
