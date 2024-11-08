package org.jsynthlib.synthdrivers.NovationNova1;

import java.io.UnsupportedEncodingException;
import javax.swing.JOptionPane;
import org.jsynthlib.core.BankDriver;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class NovationNova1SinglePerformanceDriver extends BankDriver {

    public NovationNova1SinglePerformanceDriver(final Device device) {
        super(device, "Peformance (single)", "Yves Lefebvre", 6, 1);
        sysexID = "F000202901210*000*";
        sysexRequestDump = new SysexHandler("F0 00 20 29 01 21 @@ 08 F7");
        deviceIDoffset = 6;
        bankNumbers = new String[] { "Single Performance" };
        patchNumbers = new String[] { "Part 1-", "Part 2-", "Part 3-", "Part 4-", "Part 5-", "Part 6-" };
        singleSysexID = "F000202901210*000*";
        singleSize = 296;
    }

    public int getPatchStart(int patchNum) {
        int start = 296 * patchNum;
        start += 9;
        return start;
    }

    public String getPatchName(Patch p) {
        try {
            return new String(p.sysex, (296 * 8) + 8, 16, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            return "-";
        }
    }

    public String getPatchName(Patch p, int patchNum) {
        int nameStart = getPatchStart(patchNum);
        try {
            return new String(p.sysex, nameStart, 16, "US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            return "-";
        }
    }

    public void setPatchName(Patch p, int patchNum, String name) {
        patchNameSize = 16;
        patchNameStart = getPatchStart(patchNum);
        if (name.length() < patchNameSize) {
            name += "            ";
        }
        try {
            byte[] namebytes = name.getBytes("US-ASCII");
            for (int i = 0; i < patchNameSize; ++i) {
                p.sysex[patchNameStart + i] = namebytes[i];
            }
        } catch (UnsupportedEncodingException ex) {
            return;
        }
    }

    public void calculateChecksum(Patch p) {
    }

    public void putPatch(Patch bank, Patch p, int patchNum) {
        if (!canHoldPatch(p)) {
            JOptionPane.showMessageDialog(null, "This type of patch does not fit in to this type of bank.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        System.arraycopy(p.sysex, 9, bank.sysex, getPatchStart(patchNum), 296 - 9);
        calculateChecksum(bank);
    }

    public Patch getPatch(Patch bank, int patchNum) {
        try {
            byte[] sysex = new byte[296];
            sysex[0] = (byte) 0xF0;
            sysex[1] = (byte) 0x00;
            sysex[2] = (byte) 0x20;
            sysex[3] = (byte) 0x29;
            sysex[4] = (byte) 0x01;
            sysex[5] = (byte) 0x21;
            sysex[6] = (byte) (getChannel() - 1);
            sysex[7] = (byte) 0x00;
            sysex[8] = (byte) 0x09;
            sysex[295] = (byte) 0xF7;
            System.arraycopy(bank.sysex, getPatchStart(patchNum), sysex, 9, 296 - 9);
            Patch p = new Patch(sysex, getDevice());
            p.calculateChecksum();
            return p;
        } catch (Exception e) {
            Logger.reportError("Error", "Error in Nova1 Bank Driver", e);
            return null;
        }
    }

    public Patch createNewPatch() {
        byte[] sysex = new byte[((296 * 8) + 406)];
        byte[] sysexHeader = new byte[10];
        sysexHeader[0] = (byte) 0xF0;
        sysexHeader[1] = (byte) 0x00;
        sysexHeader[2] = (byte) 0x20;
        sysexHeader[3] = (byte) 0x29;
        sysexHeader[4] = (byte) 0x01;
        sysexHeader[5] = (byte) 0x21;
        sysexHeader[6] = (byte) (getChannel() - 1);
        sysexHeader[7] = (byte) 0x00;
        sysexHeader[8] = (byte) (0x00);
        Patch p = new Patch(sysex, this);
        for (int i = 0; i < 8; i++) {
            sysexHeader[8] = (byte) i;
            System.arraycopy(sysexHeader, 0, p.sysex, i * 296, 9);
            System.arraycopy(NovationNova1InitPatch.initpatch, 9, p.sysex, (i * 296) + 9, 296 - 9);
        }
        System.arraycopy(NovationNova1InitPatch.initperf, 0, p.sysex, (8 * 296), 406);
        return p;
    }

    public void storePatch(Patch bank, int bankNum, int patchNum) {
        JOptionPane.showMessageDialog(null, "You can not store performance data with this driver.\nUse send and save it from the Nova front pannel\n(you will have to decide where to save the actual patch)", "Error", JOptionPane.ERROR_MESSAGE);
    }

    ;

    public void setBankNum(int bankNum) {
    }
}
