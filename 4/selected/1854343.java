package org.jsynthlib.synthdrivers.RolandGP16;

import org.jsynthlib.core.BankDriver;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.DriverUtil;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;
import java.io.UnsupportedEncodingException;
import javax.swing.JOptionPane;

/**
 * Bank driver for ROLAND GP16.
 * @version $Id: RolandGP16BankDriver.java 1186 2011-12-19 14:11:46Z chriswareham $
 */
public class RolandGP16BankDriver extends BankDriver {

    /** Header Size */
    private static final int HSIZE = 5;

    /** Single Patch size */
    private static final int SSIZE = 121;

    /** The number of single patches in a bank patch. */
    private static final int NS = 8;

    /** The sysex message sent when requesting a patch (from a bank). */
    private static final SysexHandler SYS_REQ = new SysexHandler("F0 41 @@ 2A 11 0B *patchnumber* 00 00 00 75 *checksum* F7");

    /** Time to sleep when doing sysex data transfers. */
    private static final int sleepTime = 100;

    /** Single Driver for GP16 */
    private RolandGP16SingleDriver singleDriver;

    /** The constructor. */
    public RolandGP16BankDriver(final Device device, RolandGP16SingleDriver singleDriver) {
        super(device, "Bank", "Mikael Kurula", NS, 2);
        this.singleDriver = singleDriver;
        sysexID = "F041**2A";
        deviceIDoffset = 2;
        bankNumbers = new String[NS * 2];
        System.arraycopy(DriverUtil.generateNumbers(1, NS, "Group A - Bank ##"), 0, bankNumbers, 0, NS);
        System.arraycopy(DriverUtil.generateNumbers(1, NS, "Group B - Bank ##"), 0, bankNumbers, NS, NS);
        patchNumbers = new String[NS * 1];
        System.arraycopy(DriverUtil.generateNumbers(1, NS, "Patch ##"), 0, patchNumbers, 0, NS);
        singleSysexID = sysexID;
        singleSize = HSIZE + SSIZE + 1;
        patchSize = singleSize * NS;
    }

    /** Return the starting index of a given patch in the bank. */
    public int getPatchStart(int patchNum) {
        return singleSize * patchNum;
    }

    /** Get patch names in bank for bank edit view. */
    public String getPatchName(Patch p, int patchNum) {
        int nameStart = getPatchStart(patchNum);
        nameStart += 108;
        try {
            return new String(p.sysex, nameStart, 16, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            return "-";
        }
    }

    /** Set patch names in bank for bank edit view. */
    public void setPatchName(Patch p, int patchNum, String name) {
        patchNameSize = 16;
        patchNameStart = getPatchStart(patchNum) + 108;
        if (name.length() < patchNameSize) {
            name += "                ";
        }
        try {
            byte[] namebytes = name.getBytes("US-ASCII");
            System.arraycopy(namebytes, 0, p.sysex, patchNameStart, patchNameSize);
        } catch (UnsupportedEncodingException ex) {
        }
    }

    /** Calculate the checksum for all patches in the bank. */
    public void calculateChecksum(Patch p) {
        for (int i = 0; i < NS; i++) {
            calculateChecksum(p, i * singleSize + 5, i * singleSize + 124, i * singleSize + 125);
        }
    }

    /** Insert a given patch into a given position of a given bank. */
    public void putPatch(Patch bank, Patch p, int patchNum) {
        if (!canHoldPatch(p)) {
            JOptionPane.showMessageDialog(null, "This type of patch does not fit in to this type of bank.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        System.arraycopy(p.sysex, 0, bank.sysex, getPatchStart(patchNum), singleSize);
        calculateChecksum(bank);
    }

    /** Extract a given patch from a given bank. */
    public Patch getPatch(Patch bank, int patchNum) {
        byte[] sysex = new byte[singleSize];
        System.arraycopy(bank.sysex, getPatchStart(patchNum), sysex, 0, singleSize);
        try {
            Patch p = new Patch(sysex, getDevice());
            singleDriver.calcChecksum(p);
            return p;
        } catch (Exception e) {
            Logger.reportError("Error", "Error in GP16 Bank Driver", e);
            return null;
        }
    }

    /** A nice bank dump of the GP-16 is just all patches dumped one by one, with correct memory address. */
    public void requestPatchDump(int bankNum, int patchNum) {
        for (int i = 0; i < NS; i++) {
            requestSinglePatchDump(bankNum, i);
        }
    }

    /** Send the bank back as it was received. */
    public void storePatch(Patch bank, int bankNum, int patchNum) {
        for (int i = 0; i < NS; i++) {
            Patch p = getPatch(bank, i);
            storeSinglePatch(p, bankNum, i);
        }
    }

    /** Worker for requestPatchDump. */
    public void requestSinglePatchDump(int bankNum, int patchNum) {
        try {
            Thread.sleep(sleepTime);
        } catch (Exception e) {
        }
        SysexHandler.NameValue nVs[] = new SysexHandler.NameValue[2];
        nVs[0] = new SysexHandler.NameValue("patchnumber", bankNum * 8 + patchNum);
        nVs[1] = new SysexHandler.NameValue("checksum", 0);
        Patch p = new Patch(SYS_REQ.toByteArray(getChannel(), nVs));
        calculateChecksum(p, 5, 10, 11);
        send(p.sysex);
        try {
            Thread.sleep(sleepTime);
        } catch (Exception e) {
        }
    }

    /** Worker for storePatch. */
    public void storeSinglePatch(Patch p, int bankNum, int patchNum) {
        p.sysex[5] = (byte) 0x0B;
        p.sysex[6] = (byte) (bankNum * 8 + patchNum);
        p.sysex[7] = (byte) 0x00;
        sendPatchWorker(p);
        try {
            Thread.sleep(sleepTime);
        } catch (Exception e) {
        }
    }

    /** Create a new bank, that conforms to the format of the GP-16. */
    public Patch createNewPatch() {
        byte[] sysex = new byte[NS * singleSize];
        Patch blankPatch = singleDriver.createNewPatch();
        for (int i = 0; i < NS; i++) {
            System.arraycopy(blankPatch.sysex, 0, sysex, getPatchStart(i), singleSize);
        }
        Patch p = new Patch(sysex, this);
        calculateChecksum(p);
        return p;
    }

    /** The name string of the GP-16 is 16 characters long. */
    public void deletePatch(Patch p, int patchNum) {
        setPatchName(p, patchNum, "                ");
    }

    /** Smarter bank naming, name the bank after the first patch in it. */
    public String getPatchName(Patch p) {
        return getPatchName(p, 0);
    }
}
