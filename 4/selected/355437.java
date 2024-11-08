package org.jsynthlib.synthdrivers.CheetahMS6;

import org.jsynthlib.core.Driver;
import org.jsynthlib.core.DriverUtil;
import org.jsynthlib.core.JSLFrame;
import org.jsynthlib.core.Logger;
import org.jsynthlib.core.Patch;
import org.jsynthlib.core.SysexHandler;

/**
 * This class provides a single patch driver for the Cheetah MS6 synthesiser.
 *
 * @author Chris Wareham
 */
public class CheetahMS6SingleDriver extends Driver {

    /**
     * The patch header length (F0 36 02 00 08).
     */
    private static final int HEADER_LEN = 7;

    /**
     * The patch data length.
     */
    private static final int PATCH_LEN = 90;

    /**
     * The tone buffer id.
     */
    private static byte TONE_BUFFER = 1;

    /**
     * The patch request handler.
     */
    private SysexHandler patchRequestHandler = new SysexHandler("F0 36 02 00 18 *bankNum* *patchNum* F7");

    /**
     * Construct a single patch driver for the Cheetah MS6 synthesiser.
     *
     * @param device the device for the Cheetah MS6 synthesiser
     */
    public CheetahMS6SingleDriver(final CheetahMS6Device device) {
        super(device, "Single", CheetahMS6Device.AUTHOR);
        bankNumbers = new String[] { "1 Strings", "2 Pianos, Clavinets and Brass", "3 Organs, Effects, Vox, Strings and Brass", "4 Bass, Sync and Lead", "5 Miscellaneous", "6 User Patches", "7 User Patches" };
        patchNumbers = new String[64];
        System.arraycopy(DriverUtil.generateNumbers(1, 64, "##"), 0, patchNumbers, 0, 64);
        patchSize = HEADER_LEN + PATCH_LEN + 1;
        patchNameStart = 0;
        patchNameSize = 0;
        checksumOffset = 0;
        checksumStart = 0;
        checksumEnd = 0;
        sysexID = "F03602**08";
        deviceIDoffset = 3;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storePatch(final Patch patch, final int bankNum, final int patchNum) {
        setBankNum(bankNum);
        setPatchNum(patchNum);
        pause(100);
        patch.sysex[5] = (byte) (bankNum + 1);
        patch.sysex[6] = (byte) patchNum;
        sendPatchWorker(patch);
        pause(100);
        setPatchNum(patchNum);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendPatch(final Patch patch) {
        patch.sysex[6] = TONE_BUFFER;
        sendPatchWorker(patch);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void calculateChecksum(final Patch patch, final int start, final int end, final int offset) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestPatchDump(final int bankNum, final int patchNum) {
        send(patchRequestHandler.toSysexMessage(getChannel(), new SysexHandler.NameValue("bankNum", bankNum + 1), new SysexHandler.NameValue("patchNum", patchNum)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSLFrame editPatch(final Patch p) {
        return new CheetahMS6SingleEditor(p);
    }

    /**
     * Pause the current thread.
     *
     * @param millis the number of milliseconds to pause the thread for
     */
    private void pause(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Logger.warn("Pause interrupted");
        }
    }
}
