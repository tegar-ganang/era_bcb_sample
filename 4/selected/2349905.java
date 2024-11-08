package org.jsynthlib.synthdrivers.AlesisA6;

import javax.swing.JOptionPane;
import org.jsynthlib.core.App;
import org.jsynthlib.core.BankDriver;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class AlesisA6PgmBankDriver extends BankDriver {

    public AlesisA6PgmBankDriver(final AlesisA6Device device) {
        super(device, "Prog Bank", "Kenneth L. Martinez", AlesisA6PgmSingleDriver.patchList.length, 4);
        sysexID = "F000000E1D00**00";
        sysexRequestDump = new SysexHandler("F0 00 00 0E 1D 0A *bankNum* F7");
        patchSize = 300800;
        patchNameStart = 2;
        patchNameSize = 16;
        deviceIDoffset = -1;
        bankNumbers = AlesisA6PgmSingleDriver.bankList;
        patchNumbers = AlesisA6PgmSingleDriver.patchList;
        singleSize = 2350;
        singleSysexID = "F000000E1D00";
    }

    public void calculateChecksum(Patch p) {
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        if (bankNum == 1 || bankNum == 2) {
            JOptionPane.showMessageDialog(App.getInstance(), "Cannot send to a preset bank", "Store Patch", JOptionPane.WARNING_MESSAGE);
        } else {
            sendPatchWorker(p, bankNum);
        }
    }

    public void putPatch(Patch bank, Patch p, int patchNum) {
        if (!canHoldPatch(p)) {
            Logger.reportError("Error", "This type of patch does not fit in to this type of bank.");
            return;
        }
        System.arraycopy(p.sysex, 0, bank.sysex, patchNum * 2350, 2350);
        bank.sysex[patchNum * 2350 + 6] = 0;
        bank.sysex[patchNum * 2350 + 7] = (byte) patchNum;
    }

    public Patch getPatch(Patch bank, int patchNum) {
        byte sysex[] = new byte[2350];
        System.arraycopy(bank.sysex, patchNum * 2350, sysex, 0, 2350);
        return new Patch(sysex, getDevice());
    }

    public String getPatchName(Patch p, int patchNum) {
        Patch pgm = getPatch(p, patchNum);
        try {
            char c[] = new char[patchNameSize];
            for (int i = 0; i < patchNameSize; i++) {
                c[i] = (char) (AlesisA6PgmSingleDriver.getA6PgmByte(pgm.sysex, i + patchNameStart));
            }
            return new String(c);
        } catch (Exception ex) {
            return "-";
        }
    }

    public void setPatchName(Patch p, int patchNum, String name) {
        Patch pgm = getPatch(p, patchNum);
        if (name.length() < patchNameSize + 4) {
            name += "                ";
        }
        byte nameByte[] = name.getBytes();
        for (int i = 0; i < patchNameSize; i++) {
            AlesisA6PgmSingleDriver.setA6PgmByte(nameByte[i], pgm.sysex, i + patchNameStart);
        }
        putPatch(p, pgm, patchNum);
    }

    protected void sendPatchWorker(Patch p, int bankNum) {
        byte tmp[] = new byte[2350];
        try {
            App.showWaitDialog();
            for (int i = 0; i < 128; i++) {
                System.arraycopy(p.sysex, i * 2350, tmp, 0, 2350);
                tmp[6] = (byte) bankNum;
                tmp[7] = (byte) i;
                send(tmp);
                Thread.sleep(15);
            }
            App.hideWaitDialog();
        } catch (Exception e) {
            Logger.reportStatus(e);
            Logger.reportError("Error", "Unable to send Patch");
        }
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(sysexRequestDump.toSysexMessage(((byte) getChannel()), new SysexHandler.NameValue[] { new SysexHandler.NameValue("bankNum", bankNum), new SysexHandler.NameValue("patchNum", patchNum) }));
    }
}
