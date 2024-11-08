package synthdrivers.Yamaha01v;

import core.Driver;
import core.DriverUtil;
import core.Patch;
import core.SysexHandler;

public class Yamaha01vDynamicsDriver extends Driver {

    private static final SysexHandler SYS_REQ = new SysexHandler("F0 43 *ID* 7E 4C 4D 20 20 38 42 33 34 59 *patchNum* F7");

    public Yamaha01vDynamicsDriver() {
        super("Dynamics Library", "Robert Wirski");
        sysexID = "F0430*7E00244C4D20203842333459";
        patchSize = 44;
        patchNameStart = 16;
        patchNameSize = 12;
        deviceIDoffset = 2;
        checksumStart = 6;
        checksumOffset = 42;
        checksumEnd = 41;
        bankNumbers = new String[] { "" };
        patchNumbers = new String[97];
        System.arraycopy(DriverUtil.generateNumbers(1, 80, "Lib 00"), 0, patchNumbers, 0, 80);
        System.arraycopy(DriverUtil.generateNumbers(1, 12, "Ch 0"), 0, patchNumbers, 80, 12);
        patchNumbers[91] = new String("Ch 13/14");
        patchNumbers[92] = new String("Ch 15/16");
        System.arraycopy(DriverUtil.generateNumbers(1, 4, "AUX 0"), 0, patchNumbers, 93, 4);
        patchNumbers[96] = new String("ST MAS");
    }

    /**
     * Correct the patchNumber value due to the fact, that
     * there is a hole in the patch numbers in Equalizer
     * Library dump request.<p>
     */
    protected int correctPatchNumber(int patchNumber) {
        if (patchNumber > 79) patchNumber += 16;
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
            java.io.InputStream fileIn = getClass().getResourceAsStream("01v_Dynamics.syx");
            fileIn.read(sysex);
            fileIn.close();
        } catch (Exception e) {
            System.err.println("Unable to find 01v_Dynamics.syx.");
        }
        ;
        p = new Patch(sysex, this);
        return p;
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("ID", getDeviceID() + 0x1F), new SysexHandler.NameValue("patchNum", correctPatchNumber(patchNum))));
    }
}
