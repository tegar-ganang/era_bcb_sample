package synthdrivers.Yamaha01v;

import core.Driver;
import core.Patch;
import core.SysexHandler;

public class Yamaha01vPrChangeTabDriver extends Driver {

    private static final SysexHandler SYS_REQ = new SysexHandler("F0 43 *ID* 7E 4C 4D 20 20 38 42 33 34 50 20 F7");

    public Yamaha01vPrChangeTabDriver() {
        super("Program Change Table", "Robert Wirski");
        sysexID = "F0430*7E010A4C4D20203842333450";
        patchSize = 146;
        patchNameStart = 0;
        patchNameSize = 0;
        deviceIDoffset = 2;
        checksumStart = 6;
        checksumOffset = 144;
        checksumEnd = 143;
        bankNumbers = new String[] { "" };
        patchNumbers = new String[] { "" };
    }

    /**
     * @see core.Driver#createNewPatch()
     */
    public Patch createNewPatch() {
        byte[] sysex = new byte[patchSize];
        Patch p;
        try {
            java.io.InputStream fileIn = getClass().getResourceAsStream("01v_PrChangeTab.syx");
            fileIn.read(sysex);
            fileIn.close();
        } catch (Exception e) {
            System.err.println("Unable to find 01v_PrChangeTab.syx.");
        }
        ;
        p = new Patch(sysex, this);
        return p;
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("ID", getDeviceID() + 0x1F)));
    }
}
