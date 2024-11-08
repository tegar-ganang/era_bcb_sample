package org.jsynthlib.synthdrivers.Yamaha01v;

import java.io.InputStream;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class Yamaha01vCtrlChangeTabDriver extends Driver {

    private static final SysexHandler SYS_REQ = new SysexHandler("F0 43 *ID* 7E 4C 4D 20 20 38 42 33 34 43 20 F7");

    public Yamaha01vCtrlChangeTabDriver(final Device device) {
        super(device, "Control Change Table", "Robert Wirski");
        sysexID = "F0430*7E02604C4D20203842333443";
        patchSize = 360;
        patchNameStart = 0;
        patchNameSize = 0;
        deviceIDoffset = 2;
        checksumStart = 6;
        checksumOffset = 358;
        checksumEnd = 357;
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
            InputStream fileIn = getClass().getResourceAsStream("01v_CtrlChangeTab.syx");
            fileIn.read(sysex);
            fileIn.close();
        } catch (Exception e) {
            System.err.println("Unable to find 01v_CtrlChangeTab.syx.");
        }
        p = new Patch(sysex, this);
        return p;
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("ID", getDeviceID() + 0x1F)));
    }
}
