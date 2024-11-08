package org.jsynthlib.synthdrivers.Yamaha01v;

import java.io.InputStream;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class Yamaha01vSetupDriver extends Driver {

    private static final SysexHandler SYS_REQ = new SysexHandler("F0 43 *ID* 7E 4C 4D 20 20 38 42 33 34 53 20 F7");

    public Yamaha01vSetupDriver(final Device device) {
        super(device, "Setup", "Robert Wirski");
        sysexID = "F0430*7E020A4C4D20203842333453";
        patchSize = 274;
        patchNameStart = 0;
        patchNameSize = 0;
        deviceIDoffset = 2;
        checksumStart = 6;
        checksumOffset = 271;
        checksumEnd = 272;
        bankNumbers = new String[] { "" };
        patchNumbers = new String[] { "" };
    }

    /**
     * @see core.Driver#createNewPatch()
     */
    public Patch createNewPatch() {
        byte[] sysex = new byte[patchSize];
        try {
            InputStream fileIn = getClass().getResourceAsStream("01v_Setup.syx");
            fileIn.read(sysex);
            fileIn.close();
        } catch (Exception e) {
            System.err.println("Unable to find 01v_Setup.syx.");
        }
        return new Patch(sysex, this);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("ID", getDeviceID() + 0x1F)));
    }
}
