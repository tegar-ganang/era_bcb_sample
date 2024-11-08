package de.sciss.fscape.op;

import java.io.*;
import de.sciss.fscape.gui.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

/**
 *  @version	0.71, 14-Nov-07
 */
public class ShrinkOp extends Operator {

    protected static final String defaultName = "Shrink";

    protected static Presets static_presets = null;

    protected static Prefs static_prefs = null;

    protected static PropertyArray static_pr = null;

    protected static final int SLOT_INPUT = 0;

    protected static final int SLOT_OUTPUT = 1;

    private static final int PR_FACTOR = 0;

    private static final int PR_MODE = 1;

    private static final String PRN_FACTOR = "Factor";

    private static final String PRN_MODE = "Mode";

    protected static final int MODE_SHRINK = 0;

    protected static final int MODE_EXPAND = 1;

    private static final int prIntg[] = { 0, MODE_SHRINK };

    private static final String prIntgName[] = { PRN_FACTOR, PRN_MODE };

    public ShrinkOp() {
        super();
        if (static_prefs == null) {
            static_prefs = new OpPrefs(getClass(), getDefaultPrefs());
        }
        if (static_pr == null) {
            static_pr = new PropertyArray();
            static_pr.intg = prIntg;
            static_pr.intgName = prIntgName;
            static_pr.superPr = Operator.op_static_pr;
        }
        if (static_presets == null) {
            static_presets = new Presets(getClass(), static_pr.toProperties(true));
        }
        opName = "ShrinkOp";
        prefs = static_prefs;
        presets = static_presets;
        pr = (PropertyArray) static_pr.clone();
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_READER));
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_WRITER));
        icon = new OpIcon(this, OpIcon.ID_FLIPFREQ, defaultName);
    }

    public void run() {
        runInit();
        int ch, i, j;
        int fltSmpPerCrossing, fltCrossings, fltLen;
        float flt[], fltD[], filter[][], fltWin, fltRolloff;
        float fltGain;
        SpectStreamSlot runInSlot;
        SpectStreamSlot runOutSlot;
        SpectStream runInStream = null;
        SpectStream runOutStream = null;
        SpectFrame runInFr = null;
        SpectFrame runOutFr = null;
        int shrinkFactor;
        int srcBands, destBands;
        float rsmpFactor;
        float[] srcBuf, destBuf, rectBuf;
        topLevel: try {
            runInSlot = (SpectStreamSlot) slots.elementAt(SLOT_INPUT);
            if (runInSlot.getLinked() == null) {
                runStop();
            }
            for (boolean initDone = false; !initDone && !threadDead; ) {
                try {
                    runInStream = runInSlot.getDescr();
                    initDone = true;
                } catch (InterruptedException e) {
                }
                runCheckPause();
            }
            if (threadDead) break topLevel;
            shrinkFactor = 2 << pr.intg[PR_FACTOR];
            srcBands = runInStream.bands;
            runOutSlot = (SpectStreamSlot) slots.elementAt(SLOT_OUTPUT);
            runOutStream = new SpectStream(runInStream);
            if (pr.intg[PR_MODE] == MODE_SHRINK) {
                destBands = (srcBands - 1) / shrinkFactor + 1;
                rsmpFactor = (float) destBands / (float) srcBands;
                runOutStream.smpPerFrame /= shrinkFactor;
            } else {
                destBands = (srcBands - 1) * shrinkFactor + 1;
                rsmpFactor = (float) destBands / (float) srcBands;
                runOutStream.smpPerFrame *= shrinkFactor;
            }
            runOutStream.bands = destBands;
            runOutSlot.initWriter(runOutStream);
            fltSmpPerCrossing = 4096;
            fltCrossings = 5;
            fltRolloff = 0.70f;
            fltWin = 6.5f;
            fltSmpPerCrossing = 4096;
            fltLen = (int) ((float) (fltSmpPerCrossing * fltCrossings) / fltRolloff + 0.5f);
            flt = new float[fltLen];
            fltD = null;
            fltGain = Filter.createAntiAliasFilter(flt, fltD, fltLen, fltSmpPerCrossing, fltRolloff, fltWin);
            filter = new float[3][];
            filter[0] = flt;
            filter[1] = fltD;
            filter[2] = new float[2];
            filter[2][0] = fltSmpPerCrossing;
            filter[2][1] = fltGain;
            rectBuf = new float[Math.max(srcBands, destBands) << 1];
            srcBuf = new float[srcBands + 1];
            destBuf = new float[destBands + 1];
            runSlotsReady();
            mainLoop: while (!threadDead) {
                for (boolean readDone = false; (readDone == false) && !threadDead; ) {
                    try {
                        runInFr = runInSlot.readFrame();
                        readDone = true;
                        runOutFr = runOutStream.allocFrame();
                    } catch (InterruptedException e) {
                    } catch (EOFException e) {
                        break mainLoop;
                    }
                    runCheckPause();
                }
                if (threadDead) break mainLoop;
                for (ch = 0; ch < runOutStream.chanNum; ch++) {
                    Fourier.polar2Rect(runInFr.data[ch], 0, rectBuf, 0, srcBands << 1);
                    for (i = 0, j = 0; i < srcBands; i++, j += 2) {
                        srcBuf[i] = rectBuf[j];
                    }
                    Filter.resample(srcBuf, 0.0, destBuf, 0, destBands, rsmpFactor, filter);
                    for (i = 0, j = 0; i < destBands; i++, j += 2) {
                        rectBuf[j] = destBuf[i];
                    }
                    for (i = 0, j = 1; i < srcBands; i++, j += 2) {
                        srcBuf[i] = rectBuf[j];
                    }
                    Filter.resample(srcBuf, 0.0, destBuf, 0, destBands, rsmpFactor, filter);
                    for (i = 0, j = 1; i < destBands; i++, j += 2) {
                        rectBuf[j] = destBuf[i];
                    }
                    Fourier.rect2Polar(rectBuf, 0, runOutFr.data[ch], 0, destBands << 1);
                }
                runInSlot.freeFrame(runInFr);
                for (boolean writeDone = false; (writeDone == false) && !threadDead; ) {
                    try {
                        runOutSlot.writeFrame(runOutFr);
                        writeDone = true;
                        runFrameDone(runOutSlot, runOutFr);
                        runOutStream.freeFrame(runOutFr);
                    } catch (InterruptedException e) {
                    }
                    runCheckPause();
                }
            }
            runInStream.closeReader();
            runOutStream.closeWriter();
        } catch (IOException e) {
            runQuit(e);
            return;
        } catch (SlotAlreadyConnectedException e) {
            runQuit(e);
            return;
        }
        runQuit(null);
    }

    public PropertyGUI createGUI(int type) {
        PropertyGUI gui;
        if (type != GUI_PREFS) return null;
        gui = new PropertyGUI("gl" + GroupLabel.NAME_GENERAL + "\n" + "lbMode;ch,pr" + PRN_MODE + ",itShrink,itExpand\n" + "lbFactor;ch,pr" + PRN_FACTOR + "," + "it1:2,it1:4,it1:8,it1:16");
        return gui;
    }
}
