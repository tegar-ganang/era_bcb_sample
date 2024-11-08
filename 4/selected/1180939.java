package de.sciss.fscape.op;

import java.io.*;
import de.sciss.fscape.gui.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

/**
 *  @version	0.71, 14-Nov-07
 */
public class PercussionOp extends Operator {

    protected static final String defaultName = "Percussion";

    protected static Presets static_presets = null;

    protected static Prefs static_prefs = null;

    protected static PropertyArray static_pr = null;

    protected static final int SLOT_INPUT = 0;

    protected static final int SLOT_OUTPUT = 1;

    private static final int PR_CRR = 0;

    private static final int PR_CRI = 1;

    private static final int PR_CLR = 2;

    private static final int PR_CLI = 3;

    private static final int PR_CCR = 4;

    private static final int PR_CCI = 5;

    private static final int PR_CAR = 6;

    private static final int PR_CAI = 7;

    private static final String PRN_CRR = "CRR";

    private static final String PRN_CRI = "CRI";

    private static final String PRN_CLR = "CLR";

    private static final String PRN_CLI = "CLI";

    private static final String PRN_CCR = "CCR";

    private static final String PRN_CCI = "CCI";

    private static final String PRN_CAR = "CAR";

    private static final String PRN_CAI = "CAI";

    private static final int prIntg[] = { 2, 2, 1, 1, 2, 0, 1, 1 };

    private static final String prIntgName[] = { PRN_CRR, PRN_CRI, PRN_CLR, PRN_CLI, PRN_CCR, PRN_CCI, PRN_CAR, PRN_CAI };

    public PercussionOp() {
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
        opName = "PercussionOp";
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
        float f1, f2;
        SpectStreamSlot runInSlot;
        SpectStreamSlot runOutSlot;
        SpectStream runInStream = null;
        SpectStream runOutStream = null;
        SpectFrame runInFr = null;
        SpectFrame runOutFr = null;
        int srcBands, fftSize, fullFFTsize, complexFFTsize;
        float[] fftBuf, convBuf1, convBuf2;
        int clr, cli, crr, cri, ccr, cci, car, cai;
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
            fftSize = srcBands - 1;
            fullFFTsize = fftSize << 1;
            complexFFTsize = fullFFTsize << 1;
            fftBuf = new float[complexFFTsize];
            crr = pr.intg[PR_CRR] - 1;
            cri = pr.intg[PR_CRI] - 1;
            clr = pr.intg[PR_CLR] - 1;
            cli = pr.intg[PR_CLI] - 1;
            ccr = pr.intg[PR_CCR] - 1;
            cci = pr.intg[PR_CCI] - 1;
            car = pr.intg[PR_CAR] - 1;
            cai = pr.intg[PR_CAI] - 1;
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
                    for (i = 0; i <= fullFFTsize; ) {
                        fftBuf[i] = (float) Math.log(Math.max(1.0e-24, convBuf1[i]));
                        i++;
                        fftBuf[i] = convBuf1[i];
                        i++;
                    }
                    for (i = fullFFTsize + 2, j = fullFFTsize - 2; i < complexFFTsize; j -= 2) {
                        fftBuf[i++] = fftBuf[j];
                        fftBuf[i++] = -fftBuf[j + 1];
                    }
                    Fourier.complexTransform(fftBuf, fullFFTsize, Fourier.INVERSE);
                    fftBuf[0] *= crr;
                    fftBuf[1] *= cri;
                    for (i = 2, j = complexFFTsize - 2; i < fullFFTsize; i += 2, j -= 2) {
                        f1 = fftBuf[i];
                        f2 = fftBuf[j];
                        fftBuf[i] = crr * f1 + ccr * f2;
                        fftBuf[j] = clr * f2 + car * f1;
                        f1 = fftBuf[i + 1];
                        f2 = fftBuf[j + 1];
                        fftBuf[i + 1] = cri * f1 + cci * f2;
                        fftBuf[j + 1] = cli * f2 + cai * f1;
                    }
                    fftBuf[i++] *= ccr + clr;
                    fftBuf[i++] *= cci + cli;
                    Fourier.complexTransform(fftBuf, fullFFTsize, Fourier.FORWARD);
                    for (i = 0; i <= fullFFTsize; ) {
                        convBuf2[i] = (float) Math.exp(fftBuf[i]);
                        i++;
                        convBuf2[i] = fftBuf[i];
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
        String coeff = ",it -1,it  0,it +1\n";
        gui = new PropertyGUI("glCoefficients\n" + "lbRight Wing Real;ch,pr" + PRN_CRR + coeff + "lbRight Wing Imag;ch,pr" + PRN_CRI + coeff + "lbLeft Wing Real;ch,pr" + PRN_CLR + coeff + "lbLeft Wing Imag;ch,pr" + PRN_CLI + coeff + "lbCausal Real;ch,pr" + PRN_CCR + coeff + "lbCausal Imag;ch,pr" + PRN_CCI + coeff + "lbAnticausal Real;ch,pr" + PRN_CAR + coeff + "lbAnticausal Imag;ch,pr" + PRN_CAI + coeff);
        return gui;
    }
}
