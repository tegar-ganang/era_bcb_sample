package org.jsynthlib.synthdrivers.KawaiK4;

import java.io.UnsupportedEncodingException;
import javax.swing.JOptionPane;
import org.jsynthlib.core.BankDriver;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.DriverUtil;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

/**
 * Bank driver for KAWAI K4/K4r voice patch.
 * @version $Id: KawaiK4BankDriver.java 1186 2011-12-19 14:11:46Z chriswareham $
 */
public class KawaiK4BankDriver extends BankDriver {

    /** Header Size */
    private static final int HSIZE = 8;

    /** Single Patch size */
    private static final int SSIZE = 131;

    /** the number of single patches in a bank patch. */
    private static final int NS = 64;

    private static final SysexHandler SYS_REQ = new SysexHandler("F0 40 @@ 01 00 04 *bankNum* 00 F7");

    public KawaiK4BankDriver(final Device device) {
        super(device, "Bank", "Brian Klock", NS, 4);
        sysexID = "F040**210004**00";
        deviceIDoffset = 2;
        bankNumbers = new String[] { "0-Internal", "1-External" };
        patchNumbers = new String[16 * 4];
        System.arraycopy(DriverUtil.generateNumbers(1, 16, "A-##"), 0, patchNumbers, 0, 16);
        System.arraycopy(DriverUtil.generateNumbers(1, 16, "B-##"), 0, patchNumbers, 16, 16);
        System.arraycopy(DriverUtil.generateNumbers(1, 16, "C-##"), 0, patchNumbers, 32, 16);
        System.arraycopy(DriverUtil.generateNumbers(1, 16, "D-##"), 0, patchNumbers, 48, 16);
        singleSysexID = "F040**2*0004";
        singleSize = HSIZE + SSIZE + 1;
        patchSize = HSIZE + SSIZE * NS + 1;
    }

    public int getPatchStart(int patchNum) {
        return HSIZE + (SSIZE * patchNum);
    }

    public String getPatchName(Patch p, int patchNum) {
        int nameStart = getPatchStart(patchNum);
        nameStart += 0;
        try {
            return new String(p.sysex, nameStart, 10, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            return "-";
        }
    }

    public void setPatchName(Patch p, int patchNum, String name) {
        patchNameSize = 10;
        patchNameStart = getPatchStart(patchNum);
        if (name.length() < patchNameSize) name = name + "            ";
        byte[] namebytes = new byte[64];
        try {
            namebytes = name.getBytes("US-ASCII");
            for (int i = 0; i < patchNameSize; i++) p.sysex[patchNameStart + i] = namebytes[i];
        } catch (UnsupportedEncodingException ex) {
            return;
        }
    }

    protected void calculateChecksum(Patch p, int start, int end, int ofs) {
        int i;
        int sum = 0;
        for (i = start; i <= end; i++) sum += p.sysex[i];
        sum += 0xA5;
        p.sysex[ofs] = (byte) (sum % 128);
    }

    public void calculateChecksum(Patch p) {
        for (int i = 0; i < NS; i++) calculateChecksum(p, HSIZE + (i * SSIZE), HSIZE + (i * SSIZE) + SSIZE - 2, HSIZE + (i * SSIZE) + SSIZE - 1);
    }

    public void putPatch(Patch bank, Patch p, int patchNum) {
        if (!canHoldPatch(p)) {
            JOptionPane.showMessageDialog(null, "This type of patch does not fit in to this type of bank.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        System.arraycopy(p.sysex, HSIZE, bank.sysex, getPatchStart(patchNum), SSIZE);
        calculateChecksum(bank);
    }

    public Patch getPatch(Patch bank, int patchNum) {
        byte[] sysex = new byte[HSIZE + SSIZE + 1];
        sysex[0] = (byte) 0xF0;
        sysex[1] = (byte) 0x40;
        sysex[2] = (byte) 0x00;
        sysex[3] = (byte) 0x20;
        sysex[4] = (byte) 0x00;
        sysex[5] = (byte) 0x04;
        sysex[6] = (byte) 0x00;
        sysex[7] = (byte) patchNum;
        sysex[HSIZE + SSIZE] = (byte) 0xF7;
        System.arraycopy(bank.sysex, getPatchStart(patchNum), sysex, HSIZE, SSIZE);
        try {
            Patch p = new Patch(sysex, getDevice());
            p.calculateChecksum();
            return p;
        } catch (Exception e) {
            Logger.reportError("Error", "Error in K4 Bank Driver", e);
            return null;
        }
    }

    public Patch createNewPatch() {
        byte[] sysex = new byte[HSIZE + SSIZE * NS + 1];
        sysex[0] = (byte) 0xF0;
        sysex[1] = (byte) 0x40;
        sysex[2] = (byte) 0x00;
        sysex[3] = (byte) 0x21;
        sysex[4] = (byte) 0x00;
        sysex[5] = (byte) 0x04;
        sysex[6] = (byte) 0x0;
        sysex[7] = 0;
        sysex[HSIZE + SSIZE * NS] = (byte) 0xF7;
        Patch p = new Patch(sysex, this);
        for (int i = 0; i < NS; i++) setPatchName(p, i, "New Patch");
        calculateChecksum(p);
        return p;
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("bankNum", bankNum << 1)));
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
        p.sysex[3] = (byte) 0x21;
        p.sysex[6] = (byte) (bankNum << 1);
        p.sysex[7] = (byte) 0x0;
        sendPatchWorker(p);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
    }
}
