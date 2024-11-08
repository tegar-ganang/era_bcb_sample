package org.jsynthlib.synthdrivers.Yamaha01v;

import java.io.InputStream;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.DriverUtil;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class Yamaha01vSceneDriver extends Driver {

    private static final SysexHandler SYS_REQ = new SysexHandler("F0 43 *ID* 7E 4C 4D 20 20 38 42 33 34 4D *patchNum* F7");

    public Yamaha01vSceneDriver(final Device device) {
        super(device, "Scene", "Robert Wirski");
        sysexID = "F0430*7E10004C4D2020384233344D";
        patchSize = 2056;
        patchNameStart = 18;
        patchNameSize = 8;
        deviceIDoffset = 2;
        checksumStart = 6;
        checksumOffset = 2054;
        checksumEnd = 2053;
        bankNumbers = new String[] { "" };
        patchNumbers = new String[101];
        System.arraycopy(DriverUtil.generateNumbers(0, 99, "00"), 0, patchNumbers, 0, 100);
        patchNumbers[100] = new String("Edit Buffer");
    }

    /**
     * Sends a patch to a set location on a synth.<p>
     *
     * @see Patch#send(int, int)
     */
    protected void storePatch(Patch p, int bankNum, int patchNum) {
        if (patchNum == 100) {
            patchNum = 127;
        }
        setPatchNum(patchNum);
        setBankNum(0);
        p.sysex[15] = (byte) patchNum;
        calculateChecksum(p);
        sendPatchWorker(p);
    }

    /**
     * Sends a patch to the synth's edit buffer.<p>
     *
     * @see Patch#send()
     * @see ISinglePatch#send()
     */
    protected void sendPatch(Patch p) {
        p.sysex[15] = (byte) 127;
        calculateChecksum(p);
        sendPatchWorker(p);
    }

    /**
     * @see core.Driver#createNewPatch()
     */
    public Patch createNewPatch() {
        byte[] sysex = new byte[patchSize];
        try {
            InputStream fileIn = getClass().getResourceAsStream("01v_Scene.syx");
            fileIn.read(sysex);
            fileIn.close();
        } catch (Exception e) {
            System.err.println("Unable to find 01v_Scene.syx.");
        }
        return new Patch(sysex, this);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        if (patchNum == 100) {
            patchNum = 127;
        }
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("ID", getDeviceID() + 0x1F), new SysexHandler.NameValue("patchNum", patchNum)));
    }
}
