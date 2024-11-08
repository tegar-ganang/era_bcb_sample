package de.sciss.fscape.op;

import java.awt.*;
import java.io.*;
import de.sciss.fscape.gui.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;
import de.sciss.io.AudioFileDescr;

/**
 *  @version	0.71, 14-Nov-07
 */
public class CepstralOp extends Operator {

    protected static final String defaultName = "Cepstral";

    protected static Presets static_presets = null;

    protected static Prefs static_prefs = null;

    protected static PropertyArray static_pr = null;

    protected static final int SLOT_INPUT = 0;

    protected static final int SLOT_OUTPUT = 1;

    private static final int PR_LOFREQ = 0;

    private static final int PR_HIFREQ = 1;

    private static final int PR_QUALITY = 0;

    private static final String PRN_HIFREQ = "HiFreq";

    private static final String PRN_LOFREQ = "LoFreq";

    private static final String PRN_QUALITY = "Quality";

    private static final int prIntg[] = { FIRDesignerDlg.QUAL_MEDIUM };

    private static final String prIntgName[] = { PRN_QUALITY };

    private static final Param prPara[] = { null, null };

    private static final String prParaName[] = { PRN_LOFREQ, PRN_HIFREQ };

    protected static final String ERR_BANDS = "Band# not power of 2";

    public CepstralOp() {
        super();
        if (static_prefs == null) {
            static_prefs = new OpPrefs(getClass(), getDefaultPrefs());
        }
        if (static_pr == null) {
            static_pr = new PropertyArray();
            static_pr.intg = prIntg;
            static_pr.intgName = prIntgName;
            static_pr.para = prPara;
            static_pr.para[PR_HIFREQ] = new Param(22050.0, Param.ABS_HZ);
            static_pr.para[PR_LOFREQ] = new Param(50.0, Param.ABS_HZ);
            static_pr.paraName = prParaName;
            static_pr.superPr = Operator.op_static_pr;
        }
        if (static_presets == null) {
            static_presets = new Presets(getClass(), static_pr.toProperties(true));
        }
        opName = "CepstralOp";
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
        float f1, f2, phase1, phase2;
        SpectStreamSlot runInSlot;
        SpectStreamSlot runOutSlot;
        SpectStream runInStream = null;
        SpectStream runOutStream = null;
        SpectFrame runInFr = null;
        SpectFrame runOutFr = null;
        int skip, srcBands, srcSize, fftSize, fullFFTsize;
        float loFreq, hiFreq;
        float[] fftBuf, fltBuf;
        AudioFileDescr tmpStream;
        FilterBox fltBox;
        Point fltLength;
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
            srcSize = srcBands << 1;
            loFreq = Math.min(runInStream.smpRate / 2, (float) pr.para[PR_LOFREQ].val);
            hiFreq = Math.max(0.5f, Math.min(runInStream.smpRate / 2, (float) pr.para[PR_HIFREQ].val));
            if (loFreq > hiFreq) {
                f1 = loFreq;
                loFreq = hiFreq;
                hiFreq = f1;
            }
            fltBox = new FilterBox();
            if (loFreq <= 0.1f) {
                fltBox.filterType = FilterBox.FLT_LOWPASS;
                fltBox.cutOff = new Param(hiFreq, Param.ABS_HZ);
            } else if (hiFreq >= runInStream.smpRate / 2) {
                fltBox.filterType = FilterBox.FLT_HIGHPASS;
                fltBox.cutOff = new Param(loFreq, Param.ABS_HZ);
            } else {
                fltBox.filterType = FilterBox.FLT_BANDPASS;
                fltBox.cutOff = new Param((loFreq + hiFreq) / 2, Param.ABS_HZ);
                fltBox.bandwidth = new Param(hiFreq - loFreq, Param.OFFSET_HZ);
            }
            tmpStream = new AudioFileDescr();
            tmpStream.rate = runInStream.smpRate;
            fltLength = fltBox.calcLength(tmpStream, pr.intg[PR_QUALITY]);
            skip = fltLength.x;
            i = fltLength.x + fltLength.y;
            j = i + srcBands - 1;
            for (fftSize = 2; fftSize < j; fftSize <<= 1) ;
            fullFFTsize = fftSize << 1;
            fftBuf = new float[fullFFTsize];
            fltBuf = new float[fullFFTsize];
            Util.clear(fltBuf);
            fltBox.calcIR(tmpStream, pr.intg[PR_QUALITY], Filter.WIN_BLACKMAN, fltBuf, fltLength);
            for (i = fftSize, j = fullFFTsize; i > 0; ) {
                fltBuf[--j] = 0.0f;
                fltBuf[--j] = (float) fltBuf[--i];
            }
            Util.rotate(fltBuf, fullFFTsize, fftBuf, -(skip << 1));
            Fourier.complexTransform(fltBuf, fftSize, Fourier.FORWARD);
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
                    System.arraycopy(runInFr.data[ch], 0, fftBuf, 0, srcSize);
                    for (i = 0; i < srcSize; i += 2) {
                        f1 = fftBuf[i];
                        if (f1 > 1.266416555e-14f) {
                            fftBuf[i] = (float) (Math.log(f1));
                        } else {
                            fftBuf[i] = -32f;
                        }
                    }
                    Fourier.unwrapPhases(fftBuf, 0, fftBuf, 0, srcSize);
                    phase1 = fftBuf[1];
                    phase2 = fftBuf[srcSize - 1];
                    f2 = (float) (srcSize - 2);
                    for (i = 0; i < srcSize; ) {
                        f1 = (float) i++ / f2;
                        fftBuf[i++] += phase1 * (f1 - 1.0f) - phase2 * f1;
                    }
                    for (i = srcSize; i < fullFFTsize; ) {
                        fftBuf[i++] = 0.0f;
                    }
                    Fourier.complexTransform(fftBuf, fftSize, Fourier.FORWARD);
                    Fourier.complexMult(fltBuf, 0, fftBuf, 0, fftBuf, 0, fullFFTsize);
                    Fourier.complexTransform(fftBuf, fftSize, Fourier.INVERSE);
                    f2 = (float) (srcSize - 2);
                    for (i = 0; i < srcSize; ) {
                        fftBuf[i] = (float) Math.exp(fftBuf[i]);
                        f1 = (float) i++ / f2;
                        fftBuf[i++] += phase1 * (1.0f - f1) + phase2 * f1;
                    }
                    System.arraycopy(fftBuf, 0, runOutFr.data[ch], 0, srcSize);
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
        StringBuffer qualJComboBox = new StringBuffer();
        if (type != GUI_PREFS) return null;
        for (int i = 0; i < FIRDesignerDlg.QUAL_NAMES.length; i++) {
            qualJComboBox.append(",it");
            qualJComboBox.append(FIRDesignerDlg.QUAL_NAMES[i]);
        }
        gui = new PropertyGUI("gl" + GroupLabel.NAME_GENERAL + "\n" + "lbHigh frequency;pf" + Constants.absHzSpace + ",pr" + PRN_HIFREQ + "\n" + "lbLow frequency;pf" + Constants.absHzSpace + ",pr" + PRN_LOFREQ + "\n" + "lbQuality;ch,pr" + PRN_QUALITY + qualJComboBox.toString() + "\n");
        return gui;
    }
}
