package de.sciss.fscape.op;

import java.io.*;
import de.sciss.fscape.gui.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

public class ContrastOp extends Operator {

    protected static final String defaultName = "Contrast";

    protected static Presets static_presets = null;

    protected static Prefs static_prefs = null;

    protected static PropertyArray static_pr = null;

    protected static final int SLOT_INPUT = 0;

    protected static final int SLOT_OUTPUT = 1;

    private static final int PR_CONTRAST = 0;

    private static final int PR_MAXBOOST = 1;

    private static final String PRN_CONTRAST = "Contrast";

    private static final String PRN_MAXBOOST = "MaxBoost";

    private static final Param prPara[] = { null, null };

    private static final String prParaName[] = { PRN_CONTRAST, PRN_MAXBOOST };

    public ContrastOp() {
        super();
        if (static_prefs == null) {
            static_prefs = new OpPrefs(getClass(), getDefaultPrefs());
        }
        if (static_pr == null) {
            static_pr = new PropertyArray();
            static_pr.para = prPara;
            static_pr.para[PR_CONTRAST] = new Param(24.0, Param.DECIBEL_AMP);
            static_pr.para[PR_MAXBOOST] = new Param(36.0, Param.DECIBEL_AMP);
            static_pr.paraName = prParaName;
            static_pr.superPr = Operator.op_static_pr;
        }
        if (static_presets == null) {
            static_presets = new Presets(getClass(), static_pr.toProperties(true));
        }
        opName = "ContrastOp";
        prefs = static_prefs;
        presets = static_presets;
        pr = (PropertyArray) static_pr.clone();
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_READER));
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_WRITER));
        icon = new OpIcon(this, OpIcon.ID_FLIPFREQ, defaultName);
    }

    public void run() {
        runInit();
        int ch, i;
        float f1, f2, maxGain;
        double exp;
        Param ampRef = new Param(1.0, Param.ABS_AMP);
        SpectStreamSlot runInSlot;
        SpectStreamSlot runOutSlot;
        SpectStream runInStream = null;
        SpectStream runOutStream = null;
        SpectFrame runInFr = null;
        SpectFrame runOutFr = null;
        int srcBands, fftSize, fullFFTsize, winSize, winHalf;
        float[] fftBuf, convBuf1, convBuf2, win;
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
            runOutSlot = (SpectStreamSlot) slots.elementAt(SLOT_OUTPUT);
            runOutStream = new SpectStream(runInStream);
            runOutSlot.initWriter(runOutStream);
            srcBands = runInStream.bands;
            winSize = srcBands - 1;
            winHalf = winSize >> 1;
            win = Filter.createFullWindow(winSize, Filter.WIN_BLACKMAN);
            fftSize = srcBands - 1;
            fullFFTsize = fftSize << 1;
            fftBuf = new float[fullFFTsize + 2];
            exp = (Param.transform(pr.para[PR_CONTRAST], Param.ABS_AMP, ampRef, null)).val - 1.0;
            maxGain = (float) (Param.transform(pr.para[PR_MAXBOOST], Param.ABS_AMP, ampRef, null)).val;
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
                    convBuf1 = runInFr.data[ch];
                    convBuf2 = runOutFr.data[ch];
                    fftBuf[0] = 1.0f;
                    fftBuf[1] = 0.0f;
                    for (i = 2; i < fullFFTsize; ) {
                        f2 = (convBuf1[i - 2] + convBuf1[i + 2]);
                        if (f2 > 0.0f) {
                            f1 = (float) Math.min(maxGain, Math.pow(2.0f * convBuf1[i] / f2, exp));
                        } else {
                            if (convBuf1[i] == 0.0f) {
                                f1 = 1.0f;
                            } else {
                                f1 = maxGain;
                            }
                        }
                        fftBuf[i++] = f1;
                        fftBuf[i++] = 0.0f;
                    }
                    fftBuf[i++] = 1.0f;
                    fftBuf[i++] = 0.0f;
                    Fourier.realTransform(fftBuf, fullFFTsize, Fourier.INVERSE);
                    Util.mult(win, winHalf, fftBuf, 0, winHalf);
                    for (i = winHalf; i < fullFFTsize - winHalf; ) {
                        fftBuf[i++] = 0.0f;
                    }
                    Util.mult(win, 0, fftBuf, i, winHalf);
                    Fourier.realTransform(fftBuf, fullFFTsize, Fourier.FORWARD);
                    for (i = 0; i <= fullFFTsize; ) {
                        convBuf2[i] = convBuf1[i] * fftBuf[i];
                        i++;
                        convBuf2[i] = convBuf1[i];
                        i++;
                    }
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
        gui = new PropertyGUI("gl" + GroupLabel.NAME_GENERAL + "\n" + "lbContrast;pf" + Constants.decibelAmpSpace + ",pr" + PRN_CONTRAST + "\n" + "lbMax.boost;pf" + Constants.decibelAmpSpace + ",pr" + PRN_MAXBOOST + "\n");
        return gui;
    }
}
