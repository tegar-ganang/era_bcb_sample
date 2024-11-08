package org.jsynthlib.synthdrivers.YamahaSY85;

import org.jsynthlib.core.Device;
import org.jsynthlib.core.Driver;
import org.jsynthlib.core.DriverUtil;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

/**
 * Driver for Yamaha SY85 Singles's (Yamaha calls them "Voices")
 *
 * @author Christopher Arndt
 */
public class YamahaSY85SingleDriver extends Driver {

    /** patch file name for createNewPatch() */
    private static final String patchFileName = "InitVce.syx";

    public YamahaSY85SingleDriver(final Device device) {
        super(device, "Voice", "Christopher Arndt");
        this.sysexID = SY85Constants.SYSEX_ID;
        this.patchNameStart = SY85Constants.VOICE_NAME_START;
        this.patchNameSize = SY85Constants.VOICE_NAME_SIZE;
        this.deviceIDoffset = 2;
        this.checksumStart = SY85Constants.VOICE_CHECKSUM_START;
        this.checksumEnd = SY85Constants.VOICE_CHECKSUM_END;
        this.checksumOffset = SY85Constants.VOICE_CHECKSUM_OFFSET;
        this.bankNumbers = new String[] { "Internal Voices I-1", "Internal Voices I-2", "Internal Voices I-3", "Internal Voices I-4" };
        patchNumbers = new String[SY85Constants.VOICE_BANK_SIZE];
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "A-##"), 0, patchNumbers, 0, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "B-##"), 0, patchNumbers, 8, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "C-##"), 0, patchNumbers, 16, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "D-##"), 0, patchNumbers, 24, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "E-##"), 0, patchNumbers, 32, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "F-##"), 0, patchNumbers, 40, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 8, "G-##"), 0, patchNumbers, 48, 8);
        System.arraycopy(DriverUtil.generateNumbers(1, 7, "H-##"), 0, patchNumbers, 54, 7);
        this.patchSize = SY85Constants.VOICE_SIZE;
    }

    /**
     * Store the voice in the voice edit buffer of the SY85.
     *
     * @param p The voice data
     */
    public void sendPatch(Patch p) {
        p.sysex[SY85Constants.SYSEX_BANK_NUMBER_OFFSET] = (byte) 127;
        p.sysex[SY85Constants.SYSEX_VOICE_NUMBER_OFFSET] = (byte) 0;
        calculateChecksum(p);
        sendPatchWorker(p);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
    }

    /**
     * Store the voice in the given slot in one of SY85's internal voice banks.
     *
     * @param p The voice data
     * @param bankNum The internal voice bank number
     * @param patchNum The voice program (slot) number
     */
    public void storePatch(Patch p, int bankNum, int patchNum) {
        p.sysex[SY85Constants.SYSEX_BANK_NUMBER_OFFSET] = (byte) (bankNum * 3);
        p.sysex[SY85Constants.SYSEX_VOICE_NUMBER_OFFSET] = (byte) patchNum;
        calculateChecksum(p);
        sendPatchWorker(p);
        try {
            Thread.sleep(50);
        } catch (Exception e) {
        }
        setBankNum(bankNum);
        setPatchNum(patchNum);
        try {
            Thread.sleep(50);
        } catch (Exception e) {
        }
    }

    /**
     * Request the dump of a single voice from SY85's internal voice banks.
     *
     * @param bankNum The internal voice bank number
     * @param patchNum The number of the Voice which is requested
     */
    public void requestPatchDump(int bankNum, int patchNum) {
        try {
            Thread.sleep(50);
        } catch (Exception e) {
        }
        send(SY85Constants.VOICE_DUMP_REQ.toSysexMessage(getDeviceID() + 0x20, new SysexHandler.NameValue("patchNum", patchNum), new SysexHandler.NameValue("bankNum", bankNum * 3)));
    }

    /**
     * Create new voice using a template patch file
     * <code>patchFileName</code>.
     *
     * The the template patch file must be located in the same directory as
     * this driver.
     *
     * @return a
     * <code>Patch</code> value
     */
    public Patch createNewPatch() {
        return (Patch) DriverUtil.createNewPatch(this, patchFileName, patchSize);
    }

    /**
     * Send Control Change (Bank Select) MIDI message.
     *
     * @param bankNum The internal voice bank number
     *
     * @see #storePatch(Patch, int, int)
     */
    protected void setBankNum(int bankNum) {
        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(ShortMessage.CONTROL_CHANGE, getChannel() - 1, 0x00, 0x00);
            send(msg);
            msg.setMessage(ShortMessage.CONTROL_CHANGE, getChannel() - 1, 0x20, bankNum * 3);
            send(msg);
        } catch (InvalidMidiDataException e) {
            Logger.reportStatus(e);
        }
    }
}
