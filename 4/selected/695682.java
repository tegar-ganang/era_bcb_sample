package synthdrivers.CasioCZ1000;

import javax.swing.JOptionPane;
import core.BankDriver;
import core.ErrorMsg;
import core.Patch;

public class CasioCZ1000BankDriver extends BankDriver {

    public CasioCZ1000BankDriver() {
        super("Bank", "Yves Lefebvre", 16, 4);
        sysexID = "F04400007*";
        deviceIDoffset = 0;
        bankNumbers = new String[] { "Internal Bank" };
        patchNumbers = new String[] { "01-", "02-", "03-", "04-", "05-", "06-", "07-", "08-", "09-", "10-", "11-", "12-", "13-", "14-", "15-", "16-" };
        singleSysexID = "F04400007*";
        singleSize = 264;
    }

    public int getPatchStart(int patchNum) {
        int start = (264 * patchNum);
        start += 7;
        return start;
    }

    public void calculateChecksum(Patch p) {
    }

    public void putPatch(Patch bank, Patch p, int patchNum) {
        if (!canHoldPatch(p)) {
            JOptionPane.showMessageDialog(null, "This type of patch does not fit in to this type of bank.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        System.arraycopy(((Patch) p).sysex, 7, ((Patch) bank).sysex, getPatchStart(patchNum), 264 - 7);
        calculateChecksum(bank);
    }

    public Patch getPatch(Patch bank, int patchNum) {
        try {
            byte[] sysex = new byte[264];
            sysex[0] = (byte) 0xF0;
            sysex[1] = (byte) 0x44;
            sysex[2] = (byte) 0x00;
            sysex[3] = (byte) 0x00;
            sysex[4] = (byte) (0x70 + getChannel() - 1);
            sysex[5] = (byte) 0x20;
            sysex[6] = (byte) (0x60);
            sysex[263] = (byte) 0xF7;
            System.arraycopy(((Patch) bank).sysex, getPatchStart(patchNum), sysex, 7, 264 - 7);
            Patch p = new Patch(sysex, getDevice());
            p.calculateChecksum();
            return p;
        } catch (Exception e) {
            ErrorMsg.reportError("Error", "Error in Nova1 Bank Driver", e);
            return null;
        }
    }

    public Patch createNewPatch() {
        byte[] sysex = new byte[(264 * 16)];
        byte[] sysexHeader = new byte[7];
        sysexHeader[0] = (byte) 0xF0;
        sysexHeader[1] = (byte) 0x44;
        sysexHeader[2] = (byte) 0x00;
        sysexHeader[3] = (byte) 0x00;
        sysexHeader[4] = (byte) (0x70 + getChannel() - 1);
        sysexHeader[5] = (byte) 0x20;
        sysexHeader[6] = (byte) 0x20;
        Patch p = new Patch(sysex, this);
        for (int i = 0; i < 16; i++) {
            sysexHeader[6] = (byte) (0x20 + i);
            System.arraycopy(sysexHeader, 0, p.sysex, i * 264, 7);
            p.sysex[(263 * (i + 1))] = (byte) 0xF7;
        }
        calculateChecksum(p);
        return p;
    }

    public void storePatch(Patch bank, int bankNum, int patchNum) {
        byte[] newsysex = new byte[264];
        Patch p = new Patch(newsysex, getDevice());
        try {
            for (int i = 0; i < 16; i++) {
                System.arraycopy(((Patch) bank).sysex, 264 * i, p.sysex, 0, 264);
                sendPatchWorker(p);
                Thread.sleep(100);
            }
        } catch (Exception e) {
            ErrorMsg.reportError("Error", "Unable to send Patch", e);
        }
    }

    public void setBankNum(int bankNum) {
    }

    protected String getPatchName(Patch bank, int patchNum) {
        return "-";
    }

    protected void setPatchName(Patch bank, int patchNum, String name) {
    }
}
