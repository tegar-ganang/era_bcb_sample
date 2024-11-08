package de.sciss.fscape.op;

import java.io.*;
import de.sciss.fscape.gui.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

/**
 *  @version	0.71, 14-Nov-07
 */
public class MindmachineOp extends Operator {

    protected static final String defaultName = "Mindmachine";

    protected static Presets static_presets = null;

    protected static Prefs static_prefs = null;

    protected static PropertyArray static_pr = null;

    private static final int PR_ANGLE = 0;

    private static final String PRN_ANGLE = "Angle";

    protected static final int SLOT_INPUT1 = 0;

    protected static final int SLOT_INPUT2 = 1;

    protected static final int SLOT_OUTPUT = 2;

    private static final Param prPara[] = { null };

    private static final String prParaName[] = { PRN_ANGLE };

    public MindmachineOp() {
        super();
        if (static_prefs == null) {
            static_prefs = new OpPrefs(getClass(), getDefaultPrefs());
        }
        if (static_pr == null) {
            static_pr = new PropertyArray();
            static_pr.para = prPara;
            static_pr.para[PR_ANGLE] = new Param(25.0, Param.NONE);
            static_pr.paraName = prParaName;
            static_pr.superPr = Operator.op_static_pr;
        }
        if (static_presets == null) {
            static_presets = new Presets(getClass(), static_pr.toProperties(true));
        }
        opName = "MindmachineOp";
        prefs = static_prefs;
        presets = static_presets;
        pr = (PropertyArray) static_pr.clone();
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_READER, "in1"));
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_READER, "in2"));
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_WRITER));
        icon = new OpIcon(this, OpIcon.ID_FLIPFREQ, defaultName);
    }

    public void run() {
        runInit();
        int ch, i, j, k, m;
        double d1, d2;
        SpectStreamSlot[] runInSlot = new SpectStreamSlot[2];
        SpectStreamSlot runOutSlot;
        SpectStream[] runInStream = new SpectStream[2];
        SpectStream runOutStream = null;
        SpectFrame[] runInFr = new SpectFrame[2];
        SpectFrame runOutFr = null;
        int[] srcBands = new int[2];
        int fftSize, fullFFTsize, complexFFTsize;
        float[] convBuf1, convBuf2;
        float[][] fftBuf;
        float[] win;
        int readDone, oldReadDone;
        int[] phase = new int[2];
        topLevel: try {
            for (i = 0; i < 2; i++) {
                runInSlot[i] = (SpectStreamSlot) slots.elementAt(SLOT_INPUT1 + i);
                if (runInSlot[i].getLinked() == null) {
                    runStop();
                }
                for (boolean initDone = false; !initDone && !threadDead; ) {
                    try {
                        runInStream[i] = runInSlot[i].getDescr();
                        initDone = true;
                        srcBands[i] = runInStream[i].bands;
                    } catch (InterruptedException e) {
                    }
                    runCheckPause();
                }
            }
            if (threadDead) break topLevel;
            runOutSlot = (SpectStreamSlot) slots.elementAt(SLOT_OUTPUT);
            runOutStream = new SpectStream(runInStream[0]);
            runOutSlot.initWriter(runOutStream);
            fftSize = srcBands[0] - 1;
            fullFFTsize = fftSize << 1;
            complexFFTsize = fullFFTsize << 1;
            fftBuf = new float[2][complexFFTsize];
            win = new float[fullFFTsize];
            d1 = 1.0 / (double) fullFFTsize * Math.PI;
            for (i = 0; i < fullFFTsize; i++) {
                d2 = Math.cos(i * d1);
                win[i] = (float) (d2 * d2);
            }
            phase[0] = (int) (pr.para[PR_ANGLE].val / 100.0 * fullFFTsize + 0.5) % fullFFTsize;
            phase[1] = (phase[0] + fftSize) % fullFFTsize;
            runSlotsReady();
            mainLoop: while (!threadDead) {
                for (readDone = 0; (readDone < 2) && !threadDead; ) {
                    oldReadDone = readDone;
                    for (i = 0; i < 2; i++) {
                        try {
                            if (runInStream[i].framesReadable() > 0) {
                                runInFr[i] = runInSlot[i].readFrame();
                                readDone++;
                            }
                        } catch (InterruptedException e) {
                        } catch (EOFException e) {
                            break mainLoop;
                        }
                        runCheckPause();
                    }
                    if (oldReadDone == readDone) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                        }
                        runCheckPause();
                    }
                }
                if (threadDead) break mainLoop;
                runOutFr = runOutStream.allocFrame();
                for (ch = 0; ch < runOutStream.chanNum; ch++) {
                    for (k = 0; k < 2; k++) {
                        convBuf1 = runInFr[k].data[ch];
                        convBuf2 = fftBuf[k];
                        for (i = 0; i <= fullFFTsize; ) {
                            convBuf2[i] = (float) Math.log(Math.max(1.0e-24, convBuf1[i]));
                            i++;
                            convBuf2[i] = convBuf1[i];
                            i++;
                        }
                        for (i = fullFFTsize + 2, j = fullFFTsize - 2; i < complexFFTsize; j -= 2) {
                            convBuf2[i++] = convBuf2[j];
                            convBuf2[i++] = -convBuf2[j + 1];
                        }
                        Fourier.complexTransform(convBuf2, fullFFTsize, Fourier.INVERSE);
                        m = phase[k];
                        for (i = 0; m < fullFFTsize; ) {
                            convBuf2[i++] *= win[m];
                            convBuf2[i++] *= win[m++];
                        }
                        for (m = 0; i < complexFFTsize; ) {
                            convBuf2[i++] *= win[m];
                            convBuf2[i++] *= win[m++];
                        }
                    }
                    convBuf1 = fftBuf[0];
                    convBuf2 = runOutFr.data[ch];
                    Util.add(fftBuf[1], 0, convBuf1, 0, complexFFTsize);
                    Fourier.complexTransform(convBuf1, fullFFTsize, Fourier.FORWARD);
                    for (i = 0; i <= fullFFTsize; ) {
                        convBuf2[i] = (float) Math.exp(convBuf1[i]);
                        i++;
                        convBuf2[i] = convBuf1[i];
                        i++;
                    }
                }
                runInSlot[0].freeFrame(runInFr[0]);
                runInSlot[1].freeFrame(runInFr[1]);
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
            runInStream[0].closeReader();
            runInStream[1].closeReader();
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
        gui = new PropertyGUI("gl" + GroupLabel.NAME_GENERAL + "\n" + "lbShift;pf" + Constants.unsignedModSpace + ",pr" + PRN_ANGLE + "\n");
        return gui;
    }
}
