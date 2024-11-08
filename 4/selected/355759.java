package org.jsynthlib.synthdrivers.KawaiK5000;

import java.io.InputStream;
import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

public class KawaiK5000ADDSingleDriver extends Driver {

    static final SysexHandler SYSEX_REQUEST_A_DUMP = new SysexHandler("F0 40 @@ 00 00 0A 00 00 *patchNum* F7");

    static final SysexHandler SYSEX_REQUEST_D_DUMP = new SysexHandler("F0 40 @@ 00 00 0A 00 02 *patchNum* F7");

    public KawaiK5000ADDSingleDriver(final Device device) {
        super(device, "Add Single", "Brian Klock");
        sysexID = "F040**20000A000*";
        patchSize = 0;
        patchNameStart = 49;
        patchNameSize = 8;
        deviceIDoffset = 2;
        checksumStart = 10;
        checksumEnd = 0;
        checksumOffset = 9;
        bankNumbers = new String[] { "0-Bank A", "1-Bank B", "2-Bank C", "3-Bank D" };
        patchNumbers = new String[] { "01-", "02-", "03-", "04-", "05-", "06-", "07-", "08-", "09-", "10-", "11-", "12-", "13-", "14-", "15-", "16-", "17-", "18-", "19-", "20-", "21-", "22-", "23-", "24-", "25-", "26-", "27-", "28-", "29-", "30-", "31-", "32-", "33-", "34-", "35-", "36-", "37-", "38-", "39-", "40-", "41-", "42-", "43-", "44-", "45-", "46-", "47-", "48-", "49-", "50-", "51-", "52-", "53-", "54-", "55-", "56-", "57-", "58-", "59-", "60-", "61-", "62-", "63-", "64-", "65-", "66-", "67-", "68-", "69-", "70-", "71-", "72-", "73-", "74-", "75-", "76-", "77-", "78-", "79-", "80-", "81-", "82-", "83-", "84-", "85-", "86-", "87-", "88-", "89-", "90-", "91-", "92-", "93-", "94-", "95-", "104-", "105-", "106-", "107-", "108-", "109-", "110-", "111-", "112-", "113-", "114-", "115-", "116-", "117-", "118-", "119-", "120-", "121-", "122-", "123-", "124-", "125-", "126-", "127-", "128-" };
    }

    public void storePatch(Patch p, int bankNum, int patchNum) {
        setBankNum(bankNum);
        setPatchNum(patchNum);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
        p.sysex[3] = (byte) 0x20;
        p.sysex[8] = (byte) (patchNum);
        if (bankNum == 0) {
            p.sysex[7] = 0;
        }
        if (bankNum == 3) {
            p.sysex[7] = 2;
        }
        sendPatchWorker(p);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
        setPatchNum(patchNum);
    }

    public void sendPatch(Patch p) {
        storePatch(p, 0, 0);
    }

    public void calculateChecksum(Patch ip) {
        calculateChecksum(ip, checksumStart, 90 + ip.sysex[60] * 86, checksumOffset);
        int sourceDataStart = 91;
        int numWaveData = 0;
        for (int i = 0; i < ip.sysex[60]; i++) {
            if (((ip.sysex[sourceDataStart + 28] & 7) * 128 + ip.sysex[sourceDataStart + 29]) == 512) {
                numWaveData++;
            }
            sourceDataStart += 86;
        }
        int waveDataStart = 91 + ip.sysex[60] * 86;
        for (int i = 0; i < numWaveData; i++) {
            calculateChecksum(ip, waveDataStart + 1, waveDataStart + 805, waveDataStart);
            waveDataStart += 806;
        }
    }

    protected void calculateChecksum(Patch p, int start, int end, int ofs) {
        int sum = 0;
        for (int i = start; i <= end; i++) {
            sum += p.sysex[i];
        }
        sum += (byte) 0xA5;
        p.sysex[ofs] = (byte) (sum % 128);
    }

    public void requestPatchDump(int bankNum, int patchNum) {
        if (bankNum == 0) {
            send(SYSEX_REQUEST_A_DUMP.toSysexMessage(getChannel(), patchNum));
        } else {
            send(SYSEX_REQUEST_D_DUMP.toSysexMessage(getChannel(), patchNum));
        }
    }

    public JSLFrame editPatch(Patch p) {
        return new KawaiK5000ADDSingleEditor(p);
    }

    public Patch createNewPatch() {
        try {
            InputStream fileIn = getClass().getResourceAsStream("k5k.syx");
            byte[] buffer = new byte[2768];
            fileIn.read(buffer);
            fileIn.close();
            return new Patch(buffer, this);
        } catch (Exception e) {
            Logger.reportError("Error", "Unable to find Defaults", e);
            return null;
        }
    }
}
