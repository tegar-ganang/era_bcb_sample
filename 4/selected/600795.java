package org.jsynthlib.synthdrivers.KorgWavestation;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

/** Driver for Korg Wavestation System Setup.
 *
 * Be carefull: This driver is untested, because I
 * only have acces to a file containing WS patches....
 *
 * @author Gerrit Gehnen
 */
public class KorgWavestationSystemSetupDriver extends Driver {

    public KorgWavestationSystemSetupDriver(final Device device) {
        super(device, "System Setup", "Gerrit Gehnen");
        sysexID = "F0423*2851";
        sysexRequestDump = new SysexHandler("F0 42 @@ 28 0E F7");
        trimSize = 75;
        patchNameStart = 0;
        patchNameSize = 0;
        deviceIDoffset = 0;
        checksumStart = 5;
        checksumEnd = 72;
        checksumOffset = 73;
    }

    public void storePatch(final Patch p, final int bankNum, final int patchNum) {
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
        p.sysex[2] = (byte) (0x30 + getChannel() - 1);
        try {
            send(p.sysex);
        } catch (Exception e) {
            Logger.reportStatus(e);
        }
    }

    public void sendPatch(final Patch p) {
        p.sysex[2] = (byte) (0x30 + getChannel() - 1);
        try {
            send(p.sysex);
        } catch (Exception e) {
            Logger.reportStatus(e);
        }
    }

    public Patch createNewPatch() {
        byte[] sysex = new byte[75];
        sysex[0] = (byte) 0xF0;
        sysex[1] = (byte) 0x42;
        sysex[2] = (byte) (0x30 + getChannel() - 1);
        sysex[3] = (byte) 0x28;
        sysex[4] = (byte) 0x51;
        sysex[74] = (byte) 0xF7;
        Patch p = new Patch(sysex, this);
        setPatchName(p, "New Patch");
        calculateChecksum(p);
        return p;
    }

    protected void calculateChecksum(final Patch p, final int start, final int end, final int ofs) {
        int sum = 0;
        for (int i = start; i <= end; ++i) {
            sum += p.sysex[i];
        }
        p.sysex[ofs] = (byte) (sum % 128);
    }

    public void requestPatchDump(final int bankNum, final int patchNum) {
        send(sysexRequestDump.toSysexMessage(getChannel(), 0));
    }
}
