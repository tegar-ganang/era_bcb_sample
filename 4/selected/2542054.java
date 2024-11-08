package org.jsynthlib.synthdrivers.KorgWavestation;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

/** Driver for Korg Wavestation Single Patches
 *
 * Be carefull: Untested, because I only have access to
 * a file containing some WS patches....
 *
 * @author Gerrit Gehnen
 */
public class KorgWavestationSinglePatchDriver extends Driver {

    public KorgWavestationSinglePatchDriver(final Device device) {
        super(device, "Single Patch", "Gerrit Gehnen");
        sysexID = "F0423*2840";
        sysexRequestDump = new SysexHandler("F0 42 @@ 28 10 *bankNum* *patchNum* F7");
        trimSize = 852 + 9;
        patchNameStart = 0;
        patchNameSize = 0;
        deviceIDoffset = 0;
        checksumStart = 7;
        checksumEnd = 852 + 6;
        checksumOffset = 852 + 7;
        bankNumbers = new String[] { "RAM1", "RAM2", "ROM1", "CARD", "RAM3" };
        patchNumbers = new String[] { "01-", "02-", "03-", "04-", "05-", "06-", "07-", "08-", "09-", "10-", "11-", "12-", "13-", "14-", "15-", "16-", "17-", "18-", "19-", "20-", "21-", "22-", "23-", "24-", "25-", "26-", "27-", "28-", "29-", "30-", "31-", "32-", "33-", "34-", "35-" };
    }

    /** Stores the patch in the specified memory.
     * Special handling here is, that the transmission of the data
     * copys the patch into the edit buffer.
     * A seperate command must transmitted to store the edit
     * buffer contents in the RAM.
     */
    public void storePatch(Patch p, int bankNum, int patchNum) {
        setBankNum(bankNum);
        setPatchNum(patchNum);
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
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
        byte[] sysex = new byte[8];
        sysex[0] = (byte) 0xF0;
        sysex[1] = (byte) 0x42;
        sysex[2] = (byte) (0x30 + getChannel() - 1);
        sysex[3] = (byte) 0x28;
        sysex[4] = (byte) 0x11;
        sysex[5] = (byte) (bankNum);
        sysex[6] = (byte) (patchNum);
        sysex[7] = (byte) 0xF7;
        try {
            send(sysex);
        } catch (Exception e) {
            Logger.reportStatus(e);
        }
    }

    public void sendPatch(Patch p) {
        p.sysex[2] = (byte) (0x30 + getChannel() - 1);
        try {
            send(p.sysex);
        } catch (Exception e) {
            Logger.reportStatus(e);
        }
    }

    public Patch createNewPatch() {
        byte[] sysex = new byte[852 + 9];
        sysex[00] = (byte) 0xF0;
        sysex[01] = (byte) 0x42;
        sysex[2] = (byte) (0x30 + getChannel() - 1);
        sysex[03] = (byte) 0x28;
        sysex[04] = (byte) 0x40;
        sysex[05] = (byte) 0x00;
        sysex[06] = (byte) 0;
        sysex[852 + 8] = (byte) 0xF7;
        Patch p = new Patch(sysex, this);
        setPatchName(p, "New Patch");
        calculateChecksum(p);
        return p;
    }

    protected void calculateChecksum(Patch p, int start, int end, int ofs) {
        int i;
        int sum = 0;
        for (i = start; i <= end; i++) {
            sum += p.sysex[i];
        }
        p.sysex[ofs] = (byte) (sum % 128);
    }

    public void setPatchNum(int patchNum) {
        try {
            send(0xC0 + (getChannel() - 1), patchNum);
        } catch (Exception e) {
        }
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        SysexHandler.NameValue nv[] = new SysexHandler.NameValue[2];
        nv[0] = new SysexHandler.NameValue("bankNum", bankNum);
        nv[1] = new SysexHandler.NameValue("patchNum", patchNum);
        send(sysexRequestDump.toSysexMessage(getChannel(), nv));
    }
}
