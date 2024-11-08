package org.jsynthlib.synthdrivers.Line6Pod20;

import java.io.UnsupportedEncodingException;
import javax.swing.JOptionPane;
import org.jsynthlib.core.BankDriver;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

/** Line6 Bank Driver. Used for Line6 bank patch.
* Line6 devices appear to the user to have nine banks consisting of four patch locations apiece.
* The banks are numbered 1 through 9 and the patches are designated by the letters A, B, C, and D.
* The actual physical layout is a single bank consisting of 36 patch locations numbered 0 to 35.
*
* @author Jeff Weber
*/
public class Line6Pod20BankDriver extends BankDriver {

    /** Program Bank Dump Request
    */
    private static final SysexHandler SYS_REQ = new SysexHandler(Constants.BANK_DUMP_REQ_ID);

    /** Constructs a Line6Pod20BankDriver
    */
    public Line6Pod20BankDriver(final Device device) {
        super(device, Constants.BANK_PATCH_TYP_STR, Constants.AUTHOR, Constants.PATCHES_PER_BANK, 1);
        sysexID = Constants.BANK_SYSEX_MATCH_ID;
        deviceIDoffset = Constants.DEVICE_ID_OFFSET;
        bankNumbers = Constants.BANK_BANK_LIST;
        patchNumbers = Constants.BANK_PATCH_LIST;
        singleSysexID = Constants.SIGL_SYSEX_MATCH_ID;
        singleSize = Constants.SIGL_SIZE + Constants.PDMP_HDR_SIZE + 1;
        patchSize = Constants.PATCHES_PER_BANK * Constants.SIGL_SIZE + Constants.BDMP_HDR_SIZE + 1;
        patchNameStart = Constants.BDMP_HDR_SIZE + Constants.PATCH_NAME_START;
        patchNameSize = Constants.PATCH_NAME_SIZE;
    }

    /** Returns the offset within a virtual (un-nibblized) bank patch of the
        * single patch given by patchNum. The value returned includes the header
        * bytes and is represents the offset in un-nibblized (non-native) bytes.
        * Called by getPatchName and setPatchName.
        */
    private int getUnNibblizedPatchStart(final int patchNum) {
        int start = (Constants.SIGL_SIZE / 2 * patchNum);
        start += Constants.BDMP_HDR_SIZE;
        return start;
    }

    /** Gets the name of a patch within the bank.
        * Patch p is the bank patch. int patchNum represents the location of the
        * single patch within the bank, designated by a number between 0 and 35.
        */
    protected String getPatchName(Patch p, int patchNum) {
        int nameOfs = getUnNibblizedPatchStart(patchNum);
        nameOfs += Constants.PATCH_NAME_START;
        char c[] = new char[patchNameSize];
        for (int i = 0; i < patchNameSize; i++) {
            c[i] = (char) PatchBytes.getSysexByte(p.sysex, Constants.BDMP_HDR_SIZE, i + nameOfs);
        }
        return new String(c);
    }

    /** Sets the name of a patch within the bank.
        * Patch p is the bank patch. int patchNum represents the location of the
        * single patch within the bank, designated by a number between 0 and 35.
        * String name contains the name to be assigned to the patch.
        */
    protected void setPatchName(Patch p, int patchNum, String name) {
        int nameOfs = getUnNibblizedPatchStart(patchNum);
        nameOfs += Constants.PATCH_NAME_START;
        if (name.length() < patchNameSize) name = name + "                ";
        byte[] namebytes = new byte[patchNameSize];
        try {
            namebytes = name.getBytes("US-ASCII");
            for (int i = 0; i < patchNameSize; i++) {
                PatchBytes.setSysexByte(p, Constants.BDMP_HDR_SIZE, i + nameOfs, namebytes[i]);
            }
        } catch (UnsupportedEncodingException ex) {
            return;
        }
    }

    /** Puts a single program patch into a bank.
        * The target bank is given by the bank parameter. The target location within the bank
        * is given by patchNum, where patchNum is in the range 0 through 35. The header and
        * trailer bytes are stripped from the sysex data and the target location within the bank
        * is overwritten.
        */
    protected void putPatch(Patch bank, Patch p, int patchNum) {
        if (!canHoldPatch(p)) {
            JOptionPane.showMessageDialog(null, "This type of patch does not fit in to this type of bank.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        System.arraycopy(p.sysex, Constants.PDMP_HDR_SIZE, bank.sysex, getNibblizedSysexStart(patchNum), Constants.SIGL_SIZE);
    }

    /** Returns a single program patch from a bank.
        * The source bank is given by the bank parameter. The patch location within the bank
        * is given by patchNum, where patchNum is in the range 0 through 35. The patch is
        * extracted from the bank and a valid Line6 program patch header is appended at the
        * beginning and 0xF7 is appended at the end.
        */
    protected Patch getPatch(Patch bank, int patchNum) {
        byte[] sysex = new byte[Constants.SIGL_SIZE + Constants.PDMP_HDR_SIZE + 1];
        System.arraycopy(Constants.SIGL_DUMP_HDR_BYTES, 0, sysex, 0, Constants.PDMP_HDR_SIZE);
        sysex[7] = (byte) patchNum;
        sysex[Constants.SIGL_SIZE + Constants.BDMP_HDR_SIZE + 1] = (byte) 0xF7;
        System.arraycopy(bank.sysex, getNibblizedSysexStart(patchNum), sysex, Constants.PDMP_HDR_SIZE, Constants.SIGL_SIZE);
        try {
            Patch p = new Patch(sysex, getDevice());
            return p;
        } catch (Exception e) {
            Logger.reportError("Error", "Error in POD 2.0 Bank Driver", e);
            return null;
        }
    }

    /** Returns the offset within a native (nibblized) bank patch of the single
        * patch given by patchNum. The value returned includes the header bytes
        * and represents the offset in nibblized (Line6-native) bytes. Called by
        * getPatch, putPatch and createNewPatch.
        */
    private int getNibblizedSysexStart(int patchNum) {
        int start = (Constants.SIGL_SIZE * patchNum);
        start += Constants.BDMP_HDR_SIZE;
        return start;
    }

    /** Creates a new bank patch.*/
    protected Patch createNewPatch() {
        byte[] sysex = new byte[Constants.BDMP_HDR_SIZE + (Constants.SIGL_SIZE * Constants.PATCHES_PER_BANK) + 1];
        System.arraycopy(Constants.BANK_DUMP_HDR_BYTES, 0, sysex, 0, Constants.BDMP_HDR_SIZE);
        sysex[Constants.BDMP_HDR_SIZE + (Constants.SIGL_SIZE * Constants.PATCHES_PER_BANK)] = (byte) 0xF7;
        Patch p = new Patch(sysex, this);
        for (int i = 0; i < Constants.PATCHES_PER_BANK; i++) {
            System.arraycopy(Constants.NEW_SYSEX, Constants.PDMP_HDR_SIZE, p.sysex, getNibblizedSysexStart(i), Constants.SIGL_SIZE);
            setPatchName(p, i, "New Patch");
        }
        return p;
    }

    /** Requests a dump of a Line6 bank consisting of 36 patches.
        * The bankNum and patchNum parameters are ignored.
        */
    public void requestPatchDump(int bankNum, int patchNum) {
        send(SYS_REQ.toSysexMessage(getChannel(), new SysexHandler.NameValue("bankNum", bankNum << 1)));
    }

    /** Sends a bank patch to the device.
        * Patch p represents the bank patch to be stored. For the Pods, the bankNum
        * and patchNum parameters are ignored (since the Pod only has one bank).
        */
    protected void storePatch(Patch p, int bankNum, int patchNum) {
        Patch[] thisPatch = new Patch[Constants.PATCHES_PER_BANK];
        for (int progNbr = 0; progNbr < Constants.PATCHES_PER_BANK; progNbr++) {
            thisPatch[progNbr] = getPatch(p, progNbr);
        }
        for (int progNbr = 0; progNbr < Constants.PATCHES_PER_BANK; progNbr++) {
            int bankNbr = progNbr / 4;
            int ptchNbr = progNbr % 4;
            ((Line6Pod20SingleDriver) thisPatch[progNbr].getDriver()).storePatch(thisPatch[progNbr], bankNbr, ptchNbr);
        }
    }
}
