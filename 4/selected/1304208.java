package org.jsynthlib.synthdrivers.KorgX3;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

/**
 * This class is a single driver for Korg X3 -synthesizer to be used in
 * JSynthLib-program. Might work directly with Korg X2 as well.
 * Making drivers for N-series (N264, N364) should be an easy
 * task if one has the original reference guide.
 *
 * Known issues: Midi channel is partly fixed to 0. This is
 * set in the third byte to be send, actually it's four
 * lower bits is exacty the channel to be used
 *
 * @author Juha Tukkinen
 */
public class KorgX3SingleDriver extends Driver {

    public static final int EXTRA_HEADER = 23;

    /**
   * Default constructor. Initialize default values for
   * class variables.
   */
    public KorgX3SingleDriver(final Device device) {
        super(device, "Single", "Juha Tukkinen");
        sysexID = "F042**35";
        sysexRequestDump = new SysexHandler("F0 42 30 35 10 F7");
        patchSize = 164 + EXTRA_HEADER;
        patchNameStart = EXTRA_HEADER;
        patchNameSize = 10;
        deviceIDoffset = 2;
        bankNumbers = new String[] { "Bank A", "Bank B" };
        patchNumbers = new String[] { "00-", "01-", "02-", "03-", "04-", "05-", "06-", "07-", "08-", "09-", "10-", "11-", "12-", "13-", "14-", "15-", "16-", "17-", "18-", "19-", "20-", "21-", "22-", "23-", "24-", "25-", "26-", "27-", "28-", "29-", "30-", "31-", "32-", "33-", "34-", "35-", "36-", "37-", "38-", "39-", "40-", "41-", "42-", "43-", "44-", "45-", "46-", "47-", "48-", "49-", "50-", "51-", "52-", "53-", "54-", "55-", "56-", "57-", "58-", "59-", "60-", "61-", "62-", "63-", "64-", "65-", "66-", "67-", "68-", "69-", "70-", "71-", "72-", "73-", "74-", "75-", "76-", "77-", "78-", "79-", "80-", "81-", "82-", "83-", "84-", "85-", "86-", "87-", "88-", "89-", "90-", "91-", "92-", "93-", "94-", "95-", "96-", "97-", "98-", "99-" };
    }

    private byte[] programMode = { (byte) 0xF0, (byte) 0x42, 0, (byte) 0x35, (byte) 0x4E, (byte) 0x02, (byte) 0x00, (byte) 0xF7 };

    private byte[] programEditMode = { (byte) 0xF0, (byte) 0x42, 0, (byte) 0x35, (byte) 0x4E, (byte) 0x03, (byte) 0x00, (byte) 0xF7 };

    private byte[] programWriteRequest = { (byte) 0xF0, (byte) 0x42, 0, (byte) 0x35, (byte) 0x11, 0, 0, (byte) 0xF7 };

    /**
   * Overrided setPatchNum. Sets the appropriate active patchnumber
   *
   * @param patchNum Patch number
   */
    public void setPatchNum(int patchNum) {
        try {
            programMode[2] = (byte) (0x30 + (getChannel() - 1));
            send(programMode);
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
            programEditMode[2] = (byte) (0x30 + (getChannel() - 1));
            send(0xC0 + (getChannel() - 1), patchNum, 0xF7);
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
            send(programEditMode);
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
    }

    /**
   * Overrided setBankNum. Sets the appropriate active bank
   *
   * @param bankNum Bank number
   */
    public void setBankNum(int bankNum) {
        try {
            programMode[2] = (byte) (0x30 + (getChannel() - 1));
            send(programMode);
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
            send(0xB0 + (getChannel() - 1), 0x00, 0x00);
            send(0xB0 + (getChannel() - 1), 0x20, 0x00 + bankNum);
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
    }

    /**
   * Overrided storePatch. Sends a patch to the current edit buffer
   * on the synthesizer and saves the patch.
   *
   * @param p Patch to be sent
   * @param bankNum Bank number
   * @param patchNum Patch number
   */
    public void storePatch(Patch p, int bankNum, int patchNum) {
        setBankNum(bankNum);
        setPatchNum(patchNum);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
        sendPatchWorker(p);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
        try {
            programWriteRequest[2] = (byte) (0x30 + (getChannel() - 1));
            programWriteRequest[5] = (byte) bankNum;
            programWriteRequest[6] = (byte) patchNum;
            send(programWriteRequest);
        } catch (Exception e) {
            Logger.reportError("Error", "Error with patch storing", e);
        }
    }

    protected void sendPatch(Patch p) {
        byte[] pd = new byte[188 + 6];
        pd[0] = (byte) 0xF0;
        pd[1] = (byte) 0x42;
        pd[2] = (byte) ((byte) 0x30 + (byte) (getChannel() - 1));
        pd[3] = (byte) 0x35;
        pd[4] = (byte) 0x40;
        pd[193] = (byte) 0xF7;
        int j = 0;
        for (int i = 0; i < 164; ) {
            byte b7 = (byte) 0x00;
            for (int k = 0; k < 7; k++) {
                if (i + k < 164) {
                    b7 += (p.sysex[i + k + EXTRA_HEADER] & 128) >> (7 - k);
                    pd[j + k + 1 + 5] = (byte) (p.sysex[i + k + EXTRA_HEADER] & (byte) 0x7F);
                }
            }
            pd[j + 5] = b7;
            j += 8;
            i += 7;
        }
        try {
            send(pd);
        } catch (Exception e) {
            Logger.reportStatus(e);
        }
    }

    /**
   * Creates a new empty patch with name 'Init'.
   *
   * @return A new empty patch
   */
    public Patch createNewPatch() {
        byte[] sysex = new byte[187];
        sysex[0] = (byte) 0xF0;
        sysex[1] = (byte) 0x42;
        sysex[2] = (byte) 0x30;
        sysex[3] = (byte) 0x35;
        sysex[53] = (byte) 0x32;
        sysex[60] = (byte) 0x02;
        sysex[65] = (byte) 0x32;
        sysex[73] = (byte) 0x63;
        sysex[74] = (byte) 0x3C;
        sysex[88] = (byte) 0x32;
        sysex[89] = (byte) 0x3C;
        sysex[95] = (byte) 0x63;
        sysex[97] = (byte) 0x63;
        sysex[99] = (byte) 0x63;
        sysex[105] = (byte) 0x99;
        sysex[109] = (byte) 0x0f;
        sysex[112] = (byte) 0x32;
        sysex[120] = (byte) 0x63;
        sysex[121] = (byte) 0x3C;
        sysex[135] = (byte) 0x32;
        sysex[136] = (byte) 0x3C;
        sysex[142] = (byte) 0x63;
        sysex[144] = (byte) 0x63;
        sysex[146] = (byte) 0x63;
        sysex[152] = (byte) 0x99;
        sysex[156] = (byte) 0x0F;
        sysex[164] = (byte) 0x65;
        sysex[165] = (byte) 0x01;
        for (int i = 23; i <= 32; i++) {
            sysex[i] = (byte) 0x20;
        }
        Patch p = new Patch(sysex, this);
        setPatchName(p, "Init");
        return p;
    }

    /**
   * Overrided editPatch. Returns an editor window for this patch.
   *
   * @param p Patch to be edited
   * @return Editor window
   */
    public JSLFrame editPatch(Patch p) {
        return new KorgX3SingleEditor(p);
    }
}
