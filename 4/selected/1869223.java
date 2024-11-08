package org.jsynthlib.synthdrivers.YamahaMotif;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/** Base driver for Yamaha Motif voices */
public abstract class YamahaMotifSingleDriver extends Driver {

    String base_address;

    String edit_buffer_base_address;

    String parameter_base_address;

    String defaults_filename = null;

    public YamahaMotifSingleDriver(final Device device) {
        super(device, "Single", "Rib Rdb");
    }

    public YamahaMotifSingleDriver(final Device device, String patchType, String authors) {
        super(device, patchType, authors);
    }

    protected void yamaha_init() {
        sysexID = "F0430*6B00000E" + base_address + "**";
        sysexRequestDump = new SysexHandler("F0 43 @@ 6B 0E " + base_address + " ** F7");
        for (int i = 0; i < patchNumbers.length; i++) {
            StringBuffer sb = new StringBuffer(4);
            sb.append((char) ('A' + i / 16));
            sb.append('-');
            sb.append((i % 16) + 1);
            patchNumbers[i] = new String(sb);
        }
    }

    public String getPatchName(Patch ip) {
        int address = Byte.parseByte(parameter_base_address, 16);
        address = (address << 16) | 0x007000;
        int offset = YamahaMotifSysexUtility.findBaseAddressOffset(ip.sysex, address);
        try {
            return new String(ip.sysex, offset + YamahaMotifSysexUtility.DATA_OFFSET, 10, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            return "-";
        }
    }

    public void setPatchName(Patch p, String name) {
        byte[] namebytes;
        int address = Byte.parseByte(parameter_base_address, 16);
        address = (address << 16) | 0x007000;
        int offset = YamahaMotifSysexUtility.findBaseAddressOffset(p.sysex, address);
        try {
            namebytes = name.getBytes("US-ASCII");
            for (int i = 0; i < 10; i++) {
                if (i >= namebytes.length) {
                    p.sysex[offset + i + YamahaMotifSysexUtility.DATA_OFFSET] = (byte) ' ';
                } else {
                    p.sysex[offset + i + YamahaMotifSysexUtility.DATA_OFFSET] = namebytes[i];
                }
            }
            YamahaMotifSysexUtility.checksum(p.sysex, offset);
        } catch (UnsupportedEncodingException e) {
            return;
        }
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        sendPatchWorker(p, patchNum);
    }

    /** Send to edit buffer */
    protected void sendPatch(Patch p) {
        sendPatchWorker(p, -1);
    }

    /**Does the actual work to send a patch to the synth*/
    protected void sendPatchWorker(Patch p, int patchnum) {
        for (int offset = 0; offset <= p.sysex.length - YamahaMotifSysexUtility.SYSEX_OVERHEAD; offset += p.sysex.length - YamahaMotifSysexUtility.SYSEX_OVERHEAD) {
            p.sysex[offset + YamahaMotifSysexUtility.ADDRESS_OFFSET + 1] = (patchnum == -1) ? Byte.parseByte(edit_buffer_base_address, 16) : Byte.parseByte(base_address, 16);
            p.sysex[offset + YamahaMotifSysexUtility.ADDRESS_OFFSET + 2] = (byte) ((patchnum == -1) ? 0 : (patchnum & 128));
            YamahaMotifSysexUtility.checksum(p.sysex, offset);
        }
        YamahaMotifSysexUtility.splitAndSendBulk(p.sysex, this, getChannel() - 1);
        p.sysex[YamahaMotifSysexUtility.ADDRESS_OFFSET + 1] = Byte.parseByte(base_address, 16);
    }

    public void calculateChecksum(Patch p) {
        YamahaMotifSysexUtility.checksum(p.sysex);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(sysexRequestDump.toSysexMessage((getChannel() + 32), patchNum));
    }

    public Patch createNewPatch() {
        try {
            InputStream fileIn = getClass().getResourceAsStream(defaults_filename);
            byte[] buffer = new byte[patchSize];
            fileIn.read(buffer);
            fileIn.close();
            return new Patch(buffer, this);
        } catch (Exception e) {
            Logger.reportError("Error", "Unable to find " + defaults_filename, e);
            return null;
        }
    }
}
