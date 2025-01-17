package synthdrivers.NovationNova1;

import core.*;
import java.io.*;
import javax.swing.*;

public class NovationNova1BankDriver extends BankDriver {

    static final SysexHandler bankARequestDump = new SysexHandler("F0 00 20 29 01 21 @@ 12 05 F7 ");

    static final SysexHandler bankBRequestDump = new SysexHandler("F0 00 20 29 01 21 @@ 12 06 F7 ");

    public NovationNova1BankDriver() {
        super("Bank", "Yves Lefebvre", 128, 4);
        sysexID = "F000202901210*020*";
        deviceIDoffset = 6;
        bankNumbers = new String[] { "Bank A", "Bank B" };
        patchNumbers = new String[] { "00-", "01-", "02-", "03-", "04-", "05-", "06-", "07-", "08-", "09-", "10-", "11-", "12-", "13-", "14-", "15-", "16-", "17-", "18-", "19-", "20-", "21-", "22-", "23-", "24-", "25-", "26-", "27-", "28-", "29-", "30-", "31-", "32-", "33-", "34-", "35-", "36-", "37-", "38-", "39-", "40-", "41-", "42-", "43-", "44-", "45-", "46-", "47-", "48-", "49-", "50-", "51-", "52-", "53-", "54-", "55-", "56-", "57-", "58-", "59-", "60-", "61-", "62-", "63-", "64-", "65-", "66-", "67-", "68-", "69-", "70-", "71-", "72-", "73-", "74-", "75-", "76-", "77-", "78-", "79-", "80-", "81-", "82-", "83-", "84-", "85-", "86-", "87-", "88-", "89-", "90-", "91-", "92-", "93-", "94-", "95-", "96-", "97-", "98-", "99-", "100-", "101-", "102-", "103-", "104-", "105-", "106-", "107-", "108-", "109-", "110-", "111-", "112-", "113-", "114-", "115-", "116-", "117-", "118-", "119-", "120-", "121-", "122-", "123-", "124-", "125-", "126-", "127" };
        singleSysexID = "F000202901210*0009";
        singleSize = 296;
    }

    public int getPatchStart(int patchNum) {
        int start = (297 * patchNum);
        start += 10;
        return start;
    }

    public String getPatchName(Patch p, int patchNum) {
        int nameStart = getPatchStart(patchNum);
        try {
            StringBuffer s = new StringBuffer(new String(((Patch) p).sysex, nameStart, 16, "US-ASCII"));
            return s.toString();
        } catch (UnsupportedEncodingException ex) {
            return "-";
        }
    }

    public void setPatchName(Patch p, int patchNum, String name) {
        patchNameSize = 16;
        patchNameStart = getPatchStart(patchNum);
        if (name.length() < patchNameSize) {
            name = name + "            ";
        }
        byte[] namebytes = new byte[64];
        try {
            namebytes = name.getBytes("US-ASCII");
            for (int i = 0; i < patchNameSize; i++) ((Patch) p).sysex[patchNameStart + i] = namebytes[i];
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
        System.arraycopy(((Patch) p).sysex, 9, ((Patch) bank).sysex, getPatchStart(patchNum), 296 - 9);
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
            System.arraycopy(((Patch) bank).sysex, getPatchStart(patchNum), sysex, 9, 296 - 9);
            Patch p = new Patch(sysex, getDevice());
            p.calculateChecksum();
            return p;
        } catch (Exception e) {
            ErrorMsg.reportError("Error", "Error in Nova1 Bank Driver", e);
            return null;
        }
    }

    public Patch createNewPatch() {
        byte[] sysex = new byte[(297 * 128)];
        byte[] sysexHeader = new byte[10];
        sysexHeader[0] = (byte) 0xF0;
        sysexHeader[1] = (byte) 0x00;
        sysexHeader[2] = (byte) 0x20;
        sysexHeader[3] = (byte) 0x29;
        sysexHeader[4] = (byte) 0x01;
        sysexHeader[5] = (byte) 0x21;
        sysexHeader[6] = (byte) (getChannel() - 1);
        sysexHeader[7] = (byte) 0x02;
        sysexHeader[8] = (byte) (0x05);
        sysexHeader[9] = (byte) 0x00;
        Patch p = new Patch(sysex, this);
        for (int i = 0; i < 128; i++) {
            sysexHeader[9] = (byte) i;
            System.arraycopy(sysexHeader, 0, p.sysex, i * 297, 10);
            System.arraycopy(NovationNova1InitPatch.initpatch, 9, p.sysex, (i * 297) + 10, 296 - 9);
            p.sysex[(297 * i) + 296] = (byte) 0xF7;
        }
        calculateChecksum(p);
        return p;
    }

    public void storePatch(Patch bank, int bankNum, int patchNum) {
        for (int i = 0; i < 128; i++) {
            ((Patch) bank).sysex[getPatchStart(i) - 2] = (byte) (bankNum + 5);
        }
        byte[] newsysex = new byte[297];
        Patch p = new Patch(newsysex);
        try {
            for (int i = 0; i < 128; i++) {
                System.arraycopy(((Patch) bank).sysex, 297 * i, p.sysex, 0, 297);
                sendPatchWorker(p);
                Thread.sleep(150);
            }
        } catch (Exception e) {
            ErrorMsg.reportError("Error", "Unable to send Patch", e);
        }
    }

    public void setBankNum(int bankNum) {
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        if (bankNum == 0) send(bankARequestDump.toSysexMessage(getChannel(), patchNum)); else send(bankBRequestDump.toSysexMessage(getChannel(), patchNum));
    }
}
