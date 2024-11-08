package org.jsynthlib.synthdrivers.Yamaha01v;

import java.io.InputStream;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class Yamaha01vRemoteIntDriver extends Driver {

    private static final SysexHandler SYS_REQ = new SysexHandler("F0 43 *ID* 7E 4C 4D 20 20 38 42 33 34 49 *patchNum* F7");

    public Yamaha01vRemoteIntDriver(final Device device) {
        super(device, "Remote(Internal Par.)", "Robert Wirski");
        sysexID = "F0430*7E00644C4D20203842333449";
        patchSize = 108;
        patchNameStart = 0;
        patchNameSize = 0;
        deviceIDoffset = 2;
        checksumStart = 6;
        checksumOffset = 106;
        checksumEnd = 105;
        bankNumbers = new String[] { "" };
        patchNumbers = new String[] { "BANK 1", "BANK 2", "BANK 3", "BANK 4" };
    }

    /**
     * Sends a patch to a set location on a synth.<p>
     *
     * @see Patch#send(int, int)
     */
    protected void storePatch(Patch p, int bankNum, int patchNum) {
        setPatchNum(patchNum);
        p.sysex[15] = (byte) patchNum;
        calculateChecksum(p);
        sendPatchWorker(p);
    }

    /**
     * @see core.Driver#createNewPatch()
     */
    public Patch createNewPatch() {
        byte[] sysex = new byte[patchSize];
        try {
            InputStream fileIn = getClass().getResourceAsStream("01v_RemoteInt.syx");
            fileIn.read(sysex);
            fileIn.close();
        } catch (Exception e) {
            System.err.println("Unable to find 01v_RemoteInt.syx.");
        }
        return new Patch(sysex, this);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("ID", getDeviceID() + 0x1F), new SysexHandler.NameValue("patchNum", patchNum)));
    }
}
