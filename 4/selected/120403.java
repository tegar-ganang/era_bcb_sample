package synthdrivers.EmuProteusMPS;

import core.*;
import java.io.*;
import javax.swing.*;

public class EmuProteusMPSSingleDriver extends Driver {

    static final SysexHandler SysexRequestDump = new SysexHandler("F0 18 08 00 00 *bankNum* *patchNum* F7");

    public EmuProteusMPSSingleDriver() {
        super("Single", "Brian Klock");
        sysexID = "F01808**01";
        patchSize = 319;
        patchNameStart = 7;
        patchNameSize = 12;
        deviceIDoffset = 3;
        checksumStart = 7;
        checksumEnd = 316;
        checksumOffset = 317;
        bankNumbers = new String[] { "0-ROM Bank", "1-RAM Bank", "2-Card Bank", "3-ROM Bank", "4-ROM Bank" };
        patchNumbers = DriverUtil.generateNumbers(0, 99, "00-");
    }

    public String getPatchName(Patch ip) {
        if (patchNameSize == 0) return ("-");
        try {
            StringBuffer s = new StringBuffer(new String(((Patch) ip).sysex, patchNameStart, patchNameSize * 2 - 1, "US-ASCII"));
            for (int i = 1; i < s.length(); i++) s.deleteCharAt(i);
            return s.toString();
        } catch (UnsupportedEncodingException ex) {
            return "-";
        }
    }

    public void setPatchName(Patch p, String name) {
        if (patchNameSize == 0) {
            JOptionPane.showMessageDialog(null, "The Driver for this patch does not support Patch Name Editing.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (name.length() < patchNameSize) name = name + "            ";
        byte[] namebytes = new byte[64];
        try {
            namebytes = name.getBytes("US-ASCII");
            for (int i = 0; i < patchNameSize; i++) ((Patch) p).sysex[patchNameStart + (i * 2)] = namebytes[i];
        } catch (UnsupportedEncodingException ex) {
            return;
        }
        calculateChecksum(p);
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        ((Patch) p).sysex[5] = (byte) ((bankNum * 100 + patchNum) % 128);
        ((Patch) p).sysex[6] = (byte) ((bankNum * 100 + patchNum) / 128);
        setBankNum(bankNum);
        setPatchNum(patchNum);
        sendPatchWorker(p);
    }

    public void sendPatch(Patch p) {
        Integer patchNum = new Integer(100);
        ((Patch) p).sysex[5] = (byte) (patchNum.intValue() % 128);
        ((Patch) p).sysex[6] = (byte) (patchNum.intValue() / 128);
        setBankNum(patchNum.intValue() / 100);
        setPatchNum(patchNum.intValue() % 100);
        sendPatchWorker(p);
    }

    public void calculateChecksum(Patch ip) {
        Patch p = (Patch) ip;
        int i;
        int sum = 0;
        for (i = 7; i <= 316; i++) sum += p.sysex[i];
        p.sysex[checksumOffset] = (byte) (sum % 128);
    }

    public Patch createNewPatch() {
        byte[] sysex = new byte[319];
        sysex[0] = (byte) 0xF0;
        sysex[1] = (byte) 0x18;
        sysex[2] = (byte) 0x08;
        sysex[3] = (byte) 0x00;
        sysex[4] = (byte) 0x01;
        sysex[318] = (byte) 0xF7;
        Patch p = new Patch(sysex, this);
        setPatchName(p, "New Patch");
        calculateChecksum(p);
        return p;
    }

    public JSLFrame editPatch(Patch p) {
        return new EmuProteusMPSSingleEditor((Patch) p);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        send(SysexRequestDump.toSysexMessage(getChannel(), new SysexHandler.NameValue("bankNum", (bankNum * 100 + patchNum) % 128), new SysexHandler.NameValue("patchNum", (bankNum * 100 + patchNum) / 128)));
    }
}
