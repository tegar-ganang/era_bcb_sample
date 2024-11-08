package synthdrivers.Yamaha01v;

import core.Driver;
import core.Patch;
import core.SysexHandler;

public class Yamaha01vRemoteUserDriver extends Driver {

    private static final SysexHandler SYS_REQ = new SysexHandler("F0 43 *ID* 7E 4C 4D 20 20 38 42 33 34 55 *patchNum* F7");

    public Yamaha01vRemoteUserDriver() {
        super("Remote(User Define)", "Robert Wirski");
        sysexID = "F0430*7E0B2A4C4D20203842333455";
        patchSize = 1458;
        patchNameStart = 0;
        patchNameSize = 0;
        deviceIDoffset = 2;
        checksumStart = 6;
        checksumOffset = 1456;
        checksumEnd = 1455;
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
            java.io.InputStream fileIn = getClass().getResourceAsStream("01v_RemoteUser.syx");
            fileIn.read(sysex);
            fileIn.close();
        } catch (Exception e) {
            System.err.println("Unable to find 01v_RemoteUser.syx.");
        }
        ;
        p = new Patch(sysex, this);
        return p;
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("ID", getDeviceID() + 0x1F), new SysexHandler.NameValue("patchNum", patchNum)));
    }
}
