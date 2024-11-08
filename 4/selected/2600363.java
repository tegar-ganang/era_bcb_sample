package synthdrivers.Yamaha01v;

import core.Driver;
import core.Patch;
import core.SysexHandler;

public class Yamaha01vRemoteIntDriver extends Driver {

    private static final SysexHandler SYS_REQ = new SysexHandler("F0 43 *ID* 7E 4C 4D 20 20 38 42 33 34 49 *patchNum* F7");

    public Yamaha01vRemoteIntDriver() {
        super("Remote(Internal Par.)", "Robert Wirski");
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
        Patch p;
        try {
            java.io.InputStream fileIn = getClass().getResourceAsStream("01v_RemoteInt.syx");
            fileIn.read(sysex);
            fileIn.close();
        } catch (Exception e) {
            System.err.println("Unable to find 01v_RemoteInt.syx.");
        }
        ;
        p = new Patch(sysex, this);
        return p;
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("ID", getDeviceID() + 0x1F), new SysexHandler.NameValue("patchNum", patchNum)));
    }
}
