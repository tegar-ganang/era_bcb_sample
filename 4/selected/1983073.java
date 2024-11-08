package org.jsynthlib.synthdrivers.AlesisA6;

import javax.swing.JOptionPane;
import org.jsynthlib.core.App;
import org.jsynthlib.core.BankDriver;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class AlesisA6MixBankDriver extends BankDriver {

    public AlesisA6MixBankDriver(final AlesisA6Device device) {
        super(device, "Mix Bank", "Kenneth L. Martinez", AlesisA6PgmSingleDriver.patchList.length, 4);
        sysexID = "F000000E1D04**00";
        sysexRequestDump = new SysexHandler("F0 00 00 0E 1D 0B *bankNum* F7");
        patchSize = 151040;
        patchNameStart = 2;
        patchNameSize = 16;
        deviceIDoffset = -1;
        bankNumbers = AlesisA6PgmSingleDriver.bankList;
        patchNumbers = AlesisA6PgmSingleDriver.patchList;
        singleSize = 1180;
        singleSysexID = "F000000E1D04";
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
        System.arraycopy(p.sysex, 0, bank.sysex, patchNum * 1180, 1180);
        bank.sysex[patchNum * 1180 + 6] = 0;
        bank.sysex[patchNum * 1180 + 7] = (byte) patchNum;
    }

    public Patch getPatch(Patch bank, int patchNum) {
        byte sysex[] = new byte[1180];
        System.arraycopy(bank.sysex, patchNum * 1180, sysex, 0, 1180);
        return new Patch(sysex, getDevice());
    }

    public String getPatchName(Patch p, int patchNum) {
        Patch mix = getPatch(p, patchNum);
        try {
            char c[] = new char[patchNameSize];
            for (int i = 0; i < patchNameSize; i++) {
                c[i] = (char) (AlesisA6PgmSingleDriver.getA6PgmByte(mix.sysex, i + patchNameStart));
            }
            return new String(c);
        } catch (Exception ex) {
            return "-";
        }
    }

    public void setPatchName(Patch p, int patchNum, String name) {
        Patch mix = getPatch(p, patchNum);
        if (name.length() < patchNameSize + 4) {
            name += "                ";
        }
        byte nameByte[] = name.getBytes();
        for (int i = 0; i < patchNameSize; i++) {
            AlesisA6PgmSingleDriver.setA6PgmByte(nameByte[i], mix.sysex, i + patchNameStart);
        }
        putPatch(p, mix, patchNum);
    }

    protected void sendPatchWorker(Patch p, int bankNum) {
        byte tmp[] = new byte[1180];
        try {
            App.showWaitDialog();
            for (int i = 0; i < 128; i++) {
                System.arraycopy(p.sysex, i * 1180, tmp, 0, 1180);
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
