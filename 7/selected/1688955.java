package de.sciss.fscape.gui;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import de.sciss.fscape.io.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.session.*;
import de.sciss.fscape.util.*;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;
import de.sciss.io.IOUtil;

/**
 *	Tried something out.
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 15-Nov-07
 */
public class TestDlg extends DocumentFrame {

    private static final int PR_INPUTFILE = 0;

    private static final int PR_OUTPUTFILE = 1;

    private static final int PR_OUTPUTTYPE = 0;

    private static final int PR_OUTPUTRES = 1;

    private static final int PR_GAINTYPE = 2;

    private static final int PR_GAIN = 0;

    private static final int PR_CALCLEN = 1;

    private static final int PR_STEPSIZE = 2;

    private static final int PR_LPORDER = 3;

    private static final int PR_RESIDUAL = 0;

    private static final String PRN_INPUTFILE = "InputFile";

    private static final String PRN_OUTPUTFILE = "OutputFile";

    private static final String PRN_OUTPUTTYPE = "OutputType";

    private static final String PRN_OUTPUTRES = "OutputReso";

    private static final String PRN_CALCLEN = "CalcLen";

    private static final String PRN_STEPSIZE = "StepSize";

    private static final String PRN_LPORDER = "LPOrder";

    private static final String PRN_RESIDUAL = "Residual";

    private static final String prText[] = { "", "" };

    private static final String prTextName[] = { PRN_INPUTFILE, PRN_OUTPUTFILE };

    private static final int prIntg[] = { 0, 0, GAIN_UNITY };

    private static final String prIntgName[] = { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_GAINTYPE };

    private static final Param prPara[] = { null, null, null, null };

    private static final String prParaName[] = { PRN_GAIN, PRN_CALCLEN, PRN_STEPSIZE, PRN_LPORDER };

    private static final boolean prBool[] = { true };

    private static final String prBoolName[] = { PRN_RESIDUAL };

    private static final int GG_INPUTFILE = GG_OFF_PATHFIELD + PR_INPUTFILE;

    private static final int GG_OUTPUTFILE = GG_OFF_PATHFIELD + PR_OUTPUTFILE;

    private static final int GG_OUTPUTTYPE = GG_OFF_CHOICE + PR_OUTPUTTYPE;

    private static final int GG_OUTPUTRES = GG_OFF_CHOICE + PR_OUTPUTRES;

    private static final int GG_GAINTYPE = GG_OFF_CHOICE + PR_GAINTYPE;

    private static final int GG_GAIN = GG_OFF_PARAMFIELD + PR_GAIN;

    private static final int GG_CALCLEN = GG_OFF_PARAMFIELD + PR_CALCLEN;

    private static final int GG_STEPSIZE = GG_OFF_PARAMFIELD + PR_STEPSIZE;

    private static final int GG_LPORDER = GG_OFF_PARAMFIELD + PR_LPORDER;

    private static final int GG_RESIDUAL = GG_OFF_CHECKBOX + PR_RESIDUAL;

    private static PropertyArray static_pr = null;

    private static Presets static_presets = null;

    /**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
    public TestDlg() {
        super("Sample Duplication");
        init2();
    }

    protected void buildGUI() {
        if (static_pr == null) {
            static_pr = new PropertyArray();
            static_pr.text = prText;
            static_pr.textName = prTextName;
            static_pr.intg = prIntg;
            static_pr.intgName = prIntgName;
            static_pr.bool = prBool;
            static_pr.boolName = prBoolName;
            static_pr.para = prPara;
            static_pr.para[PR_CALCLEN] = new Param(32.0, Param.ABS_MS);
            static_pr.para[PR_STEPSIZE] = new Param(8.0, Param.ABS_MS);
            static_pr.para[PR_LPORDER] = new Param(10.0, Param.NONE);
            static_pr.paraName = prParaName;
            fillDefaultAudioDescr(static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES);
            fillDefaultGain(static_pr.para, PR_GAIN);
            static_presets = new Presets(getClass(), static_pr.toProperties(true));
        }
        presets = static_presets;
        pr = (PropertyArray) static_pr.clone();
        GridBagConstraints con;
        PathField ggInputFile, ggOutputFile;
        Component[] ggGain;
        PathField[] ggInputs;
        ParamField ggCalcLen, ggStepSize, ggLPOrder;
        JCheckBox ggResidual;
        gui = new GUISupport();
        con = gui.getGridBagConstraints();
        con.insets = new Insets(1, 2, 1, 2);
        con.fill = GridBagConstraints.BOTH;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gui.addLabel(new GroupLabel("Waveform I/O", GroupLabel.ORIENT_HORIZONTAL, GroupLabel.BRACE_NONE));
        ggInputFile = new PathField(PathField.TYPE_INPUTFILE + PathField.TYPE_FORMATFIELD, "Select input file");
        ggInputFile.handleTypes(GenericFile.TYPES_SOUND);
        con.gridwidth = 1;
        con.weightx = 0.1;
        gui.addLabel(new JLabel("Input file", SwingConstants.RIGHT));
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weightx = 0.9;
        gui.addPathField(ggInputFile, GG_INPUTFILE, null);
        ggOutputFile = new PathField(PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD + PathField.TYPE_RESFIELD, "Select output file");
        ggOutputFile.handleTypes(GenericFile.TYPES_SOUND);
        ggInputs = new PathField[1];
        ggInputs[0] = ggInputFile;
        ggOutputFile.deriveFrom(ggInputs, "$D0$F0Pre$E");
        con.gridwidth = 1;
        con.weightx = 0.1;
        gui.addLabel(new JLabel("Output file", SwingConstants.RIGHT));
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weightx = 0.9;
        gui.addPathField(ggOutputFile, GG_OUTPUTFILE, null);
        gui.registerGadget(ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE);
        gui.registerGadget(ggOutputFile.getResGadget(), GG_OUTPUTRES);
        ggGain = createGadgets(GGTYPE_GAIN);
        con.weightx = 0.1;
        con.gridwidth = 1;
        gui.addLabel(new JLabel("Gain", SwingConstants.RIGHT));
        con.weightx = 0.4;
        gui.addParamField((ParamField) ggGain[0], GG_GAIN, null);
        con.weightx = 0.5;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gui.addChoice((JComboBox) ggGain[1], GG_GAINTYPE, null);
        con.fill = GridBagConstraints.BOTH;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gui.addLabel(new GroupLabel("LP Settings", GroupLabel.ORIENT_HORIZONTAL, GroupLabel.BRACE_NONE));
        ggCalcLen = new ParamField(Constants.spaces[Constants.absMsSpace]);
        con.weightx = 0.1;
        con.gridwidth = 1;
        gui.addLabel(new JLabel("Calc. Interval", SwingConstants.RIGHT));
        con.weightx = 0.4;
        gui.addParamField(ggCalcLen, GG_CALCLEN, null);
        ggLPOrder = new ParamField(new ParamSpace(2.0, 100000.0, 1.0, Param.NONE));
        con.weightx = 0.1;
        gui.addLabel(new JLabel("LP Order", SwingConstants.RIGHT));
        con.weightx = 0.4;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gui.addParamField(ggLPOrder, GG_LPORDER, null);
        ggStepSize = new ParamField(Constants.spaces[Constants.absMsSpace]);
        con.weightx = 0.1;
        con.gridwidth = 1;
        gui.addLabel(new JLabel("Step Size", SwingConstants.RIGHT));
        con.weightx = 0.4;
        gui.addParamField(ggStepSize, GG_STEPSIZE, null);
        ggResidual = new JCheckBox();
        con.weightx = 0.1;
        gui.addLabel(new JLabel("Residual", SwingConstants.RIGHT));
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weightx = 0.4;
        gui.addCheckbox(ggResidual, GG_RESIDUAL, null);
        initGUI(this, FLAGS_PRESETS | FLAGS_PROGBAR, gui);
    }

    /**
	 *	Werte aus Prop-Array in GUI uebertragen
	 */
    public void fillGUI() {
        super.fillGUI();
        super.fillGUI(gui);
    }

    /**
	 *	Werte aus GUI in Prop-Array uebertragen
	 */
    public void fillPropertyArray() {
        super.fillPropertyArray();
        super.fillPropertyArray(gui);
    }

    protected void process() {
        int i, j;
        int len, ch;
        long progOff, progLen;
        float f1;
        AudioFile inF = null;
        AudioFile outF = null;
        AudioFileDescr inStream = null;
        AudioFileDescr outStream = null;
        FloatFile[] floatF = null;
        File tempFile[] = null;
        float[][] inBuf, outBuf;
        float[] convBuf1, convBuf2;
        float gain = 1.0f;
        Param ampRef = new Param(1.0, Param.ABS_AMP);
        int inLength, inChanNum, outLength, inBufSize, outBufSize;
        int framesRead, framesWritten;
        float maxAmp = 0.0f;
        PathField ggOutput;
        topLevel: try {
            inF = AudioFile.openAsRead(new File(pr.text[PR_INPUTFILE]));
            inStream = inF.getDescr();
            inChanNum = inStream.channels;
            inLength = (int) inStream.length;
            if ((inLength * inChanNum) < 1) throw new EOFException(ERR_EMPTY);
            if (!threadRunning) break topLevel;
            ggOutput = (PathField) gui.getItemObj(GG_OUTPUTFILE);
            if (ggOutput == null) throw new IOException(ERR_MISSINGPROP);
            outStream = new AudioFileDescr(inStream);
            ggOutput.fillStream(outStream);
            outF = AudioFile.openAsWrite(outStream);
            if (!threadRunning) break topLevel;
            outLength = inLength << 1;
            inBufSize = 4096;
            outBufSize = inBufSize << 1;
            inBuf = new float[inChanNum][inBufSize];
            outBuf = new float[inChanNum][outBufSize];
            progOff = 0;
            progLen = (long) inLength + (long) outLength;
            if (pr.intg[PR_GAINTYPE] == GAIN_UNITY) {
                tempFile = new File[inChanNum];
                floatF = new FloatFile[inChanNum];
                for (ch = 0; ch < inChanNum; ch++) {
                    tempFile[ch] = null;
                    floatF[ch] = null;
                }
                for (ch = 0; ch < inChanNum; ch++) {
                    tempFile[ch] = IOUtil.createTempFile();
                    floatF[ch] = new FloatFile(tempFile[ch], GenericFile.MODE_OUTPUT);
                }
                progLen += inLength;
            } else {
                gain = (float) (Param.transform(pr.para[PR_GAIN], Param.ABS_AMP, ampRef, null)).val;
            }
            if (!threadRunning) break topLevel;
            framesRead = 0;
            framesWritten = 0;
            while (threadRunning && (framesWritten < outLength)) {
                len = Math.min(inBufSize, inLength - framesRead);
                inF.readFrames(inBuf, 0, len);
                progOff += len;
                framesRead += len;
                setProgression((float) progOff / (float) progLen);
                if (!threadRunning) break topLevel;
                for (ch = 0; ch < inChanNum; ch++) {
                    convBuf1 = inBuf[ch];
                    convBuf2 = outBuf[ch];
                    for (i = 0, j = 0; i < len; ) {
                        convBuf2[j++] = convBuf1[i];
                        convBuf2[j++] = convBuf1[i++];
                    }
                }
                progOff += len;
                setProgression((float) progOff / (float) progLen);
                if (!threadRunning) break topLevel;
                len <<= 1;
                if (floatF != null) {
                    for (ch = 0; ch < inChanNum; ch++) {
                        convBuf1 = outBuf[ch];
                        for (i = 0; i < len; i++) {
                            f1 = Math.abs(convBuf1[i]);
                            if (f1 > maxAmp) {
                                maxAmp = f1;
                            }
                        }
                        floatF[ch].writeFloats(convBuf1, 0, len);
                    }
                } else {
                    for (ch = 0; ch < inChanNum; ch++) {
                        convBuf1 = outBuf[ch];
                        for (i = 0; i < len; i++) {
                            f1 = Math.abs(convBuf1[i]);
                            convBuf1[i] *= gain;
                            if (f1 > maxAmp) {
                                maxAmp = f1;
                            }
                        }
                    }
                    outF.writeFrames(outBuf, 0, len);
                }
                framesWritten += len;
                progOff += len;
                setProgression((float) progOff / (float) progLen);
            }
            if (!threadRunning) break topLevel;
            inF.close();
            inF = null;
            if (pr.intg[PR_GAINTYPE] == GAIN_UNITY) {
                gain = (float) (Param.transform(pr.para[PR_GAIN], Param.ABS_AMP, new Param(1.0 / maxAmp, Param.ABS_AMP), null)).val;
                normalizeAudioFile(floatF, outF, inBuf, gain, 1.0f);
                for (ch = 0; ch < inChanNum; ch++) {
                    floatF[ch].cleanUp();
                    floatF[ch] = null;
                    tempFile[ch].delete();
                    tempFile[ch] = null;
                }
            }
            if (!threadRunning) break topLevel;
            outF.close();
            outF = null;
            maxAmp *= gain;
            handleClipping(maxAmp);
        } catch (IOException e1) {
            setError(e1);
        } catch (OutOfMemoryError e2) {
            inStream = null;
            outStream = null;
            inBuf = null;
            outBuf = null;
            convBuf1 = null;
            convBuf2 = null;
            System.gc();
            setError(new Exception(ERR_MEMORY));
            ;
        }
        if (outF != null) {
            outF.cleanUp();
        }
        if (inF != null) {
            inF.cleanUp();
        }
    }

    protected static void lpFilter(float[] data, int dataOff, float[] coeffBuf, int coeffNum, float[] future, int futOff, int futLen) {
        int i, j, k, m;
        float sum;
        for (j = futOff, m = dataOff - 1, i = futOff + futLen; j < i; m++) {
            sum = 0.0f;
            for (k = 0; k < coeffNum; k++) {
                sum += coeffBuf[k] * data[m - k];
            }
            future[j++] = sum;
        }
    }

    protected static void linearPrediction(float[] data, int dataOff, float[] coeffBuf, int coeffNum, float[] future, int futOff, int futLen) {
        int i, j, k;
        float sum;
        float[] reg = new float[coeffNum];
        for (j = 0, k = dataOff; j < coeffNum; ) {
            reg[j++] = data[--k];
        }
        for (j = futOff, i = futOff + futLen; j < i; j++) {
            sum = 0.0f;
            for (k = 0; k < coeffNum; k++) {
                sum += coeffBuf[k] * reg[k];
            }
            System.arraycopy(reg, 0, reg, 1, coeffNum - 1);
            reg[0] = sum;
            future[j] = sum;
        }
    }

    protected static float lpCoeffs(float[] data, int dataOff, int dataLen, float[] coeffBuf, int coeffNum) {
        int i, j, k;
        float f;
        float[] wk1 = new float[dataLen - 1];
        float[] wk2 = new float[dataLen - 1];
        float[] wkm = new float[coeffNum - 1];
        float xms, num, denom;
        for (j = dataLen, i = dataLen + dataOff, f = 0.0f; j < i; j++) {
            f += data[j] * data[j];
        }
        xms = f / dataLen;
        wk1[0] = data[0];
        wk2[dataLen - 2] = data[dataLen - 1];
        System.arraycopy(data, dataOff, wk1, 0, dataLen - 1);
        System.arraycopy(data, dataOff + 1, wk2, 0, dataLen - 1);
        for (k = 0; ; ) {
            num = 0.0f;
            denom = 0.0f;
            for (j = 0, i = dataLen - k - 1; j < i; j++) {
                num += wk1[j] * wk2[j];
                denom += wk1[j] * wk1[j] + wk2[j] * wk2[j];
            }
            if (denom > 0.0f) {
                f = 2.0f * num / denom;
            } else {
                f = 1.0f;
            }
            coeffBuf[k] = f;
            xms *= 1.0f - f * f;
            for (i = 0; i < k; i++) {
                coeffBuf[i] = wkm[i] - f * wkm[k - i - 1];
            }
            if (++k == coeffNum) return xms;
            System.arraycopy(coeffBuf, 0, wkm, 0, k);
            for (j = 0, i = dataLen - k - 1; j < i; j++) {
                wk1[j] -= f * wk2[j];
                wk2[j] = wk2[j + 1] - f * wk1[j + 1];
            }
        }
    }

    protected static void fixRoots(float coeffBuf[], int coeffNum) {
        int i, j, coeffNum2;
        float rootsRe, rootsIm, f1;
        coeffNum2 = coeffNum << 1;
        float[] a = new float[coeffNum2 + 2];
        float[] roots = new float[coeffNum2];
        float[] cmplxRes = new float[2];
        a[coeffNum2] = 1.0f;
        a[coeffNum2 + 1] = 0.0f;
        for (i = 0, j = coeffNum2; j > 0; ) {
            a[--j] = 0.0f;
            a[--j] = -coeffBuf[i++];
        }
        zRoots(a, coeffNum, roots, true);
        for (j = 0; j < coeffNum2; j += 2) {
            f1 = complexAbs(roots[j], roots[j + 1]);
            if (f1 > 1.0f) {
                complexDiv(1.0f, 0.0f, roots[j], -roots[j + 1], cmplxRes);
                roots[j] = cmplxRes[0];
                roots[j + 1] = cmplxRes[1];
            }
        }
        a[0] = -roots[0];
        a[1] = -roots[1];
        a[2] = 1.0f;
        a[3] = 0.0f;
        for (j = 2; j < coeffNum2; j += 2) {
            a[j + 2] = 1.0f;
            a[j + 3] = 0.0f;
            rootsRe = roots[j];
            rootsIm = roots[j + 1];
            for (i = j; i >= 2; i -= 2) {
                a[i] = a[i - 2] - (rootsRe * a[i] - rootsIm * a[i + 1]);
                a[i + 1] = a[i - 1] - (rootsIm * a[i] + rootsRe * a[i + 1]);
            }
            a[0] = -rootsRe * a[0] + rootsIm * a[1];
            a[1] = -rootsIm * a[0] - rootsRe * a[1];
        }
        for (i = coeffNum, j = 0; j < coeffNum2; j += 2) {
            coeffBuf[--i] = -a[j];
        }
    }

    protected static final float EXPECTEDERROR2 = 4.0e-6f;

    protected static void zRoots(float[] coeffBuf, int coeffNum, float[] roots, boolean polish) {
        int i, j, jj;
        float bRe, bIm, cRe, cIm;
        int coeffNum2 = coeffNum << 1;
        float[] ad = new float[coeffNum2 + 2];
        float[] x = new float[2];
        System.arraycopy(coeffBuf, 0, ad, 0, coeffNum2 + 2);
        for (j = coeffNum2; j >= 2; j -= 2) {
            jj = j - 2;
            x[0] = 0.0f;
            x[1] = 0.0f;
            i = laguerre(ad, j >> 1, x);
            if (Math.abs(x[1]) <= EXPECTEDERROR2 * Math.abs(x[0])) {
                x[1] = 0.0f;
            }
            roots[jj] = x[0];
            roots[jj + 1] = x[1];
            bRe = ad[j];
            bIm = ad[j + 1];
            for (; jj >= 0; jj -= 2) {
                cRe = ad[jj];
                cIm = ad[jj + 1];
                ad[jj] = bRe;
                ad[jj + 1] = bIm;
                bRe = x[0] * bRe - x[1] * bIm + cRe;
                bIm = x[1] * bRe + x[0] * bIm + cIm;
            }
        }
        if (polish) {
            for (j = 0; j < coeffNum2; ) {
                x[0] = roots[j];
                x[1] = roots[j + 1];
                laguerre(coeffBuf, coeffNum, x);
                roots[j++] = x[0];
                roots[j++] = x[1];
            }
        }
        for (j = 2; j < coeffNum2; j += 2) {
            x[0] = roots[j];
            x[1] = roots[j + 1];
            for (i = j - 2; i >= 2; i -= 2) {
                if (roots[i] <= x[0]) break;
                roots[i + 2] = roots[i];
                roots[i + 3] = roots[i + 1];
            }
            roots[i + 2] = x[0];
            roots[i + 3] = x[1];
        }
    }

    protected static final float EXPECTEDERROR = 1.0e-7f;

    protected static final int MR = 8;

    protected static final int MT = 10;

    protected static final int MAXITER = (MT * MR);

    protected static final float[] frac = { 0.0f, 0.5f, 0.25f, 0.75f, 0.13f, 0.38f, 0.62f, 0.88f, 1.0f };

    protected static int laguerre(float[] coeffBuf, int coeffNum, float[] x) {
        int iter, j, coeffNum2, fooInt;
        float absX, absP, absM, err, absB;
        float bRe, bIm, fRe, fIm, dRe, dIm, g2Re, g2Im, gRe, gIm, gmRe, gmIm;
        float gpRe, gpIm, sqRe, sqIm, hRe, hIm, x1Re, x1Im, dxRe, dxIm, fooFloat;
        float[] cmplxRes = new float[2];
        coeffNum2 = coeffNum << 1;
        for (iter = 1; iter <= MAXITER; iter++) {
            bRe = coeffBuf[coeffNum2];
            bIm = coeffBuf[coeffNum2 + 1];
            absB = complexAbs(bRe, bIm);
            err = absB;
            fRe = 0.0f;
            fIm = 0.0f;
            dRe = 0.0f;
            dIm = 0.0f;
            absX = complexAbs(x[0], x[1]);
            for (j = coeffNum2; j > 0; ) {
                dRe = x[0] * dRe - x[1] * dIm + bRe;
                dIm = x[1] * dRe + x[0] * dIm + bIm;
                bIm = x[1] * bRe + x[0] * bIm + coeffBuf[--j];
                bRe = x[0] * bRe - x[1] * bIm + coeffBuf[--j];
                absB = complexAbs(bRe, bIm);
                err = absB + absX * err;
            }
            err *= EXPECTEDERROR;
            if (absB <= err) return iter;
            complexDiv(dRe, dIm, bRe, bIm, cmplxRes);
            gRe = cmplxRes[0];
            gIm = cmplxRes[1];
            g2Re = gRe * gRe - gIm * gIm;
            g2Im = gIm * gRe * 2;
            complexDiv(fRe, fIm, bRe, bIm, cmplxRes);
            hRe = g2Re - 2.0f * cmplxRes[0];
            hIm = g2Im - 2.0f * cmplxRes[1];
            sqRe = (coeffNum - 1) * (coeffNum * hRe - g2Re);
            sqIm = (coeffNum - 1) * (coeffNum * hIm - g2Im);
            complexSqrt(sqRe, sqIm, cmplxRes);
            sqRe = cmplxRes[0];
            sqIm = cmplxRes[1];
            gpRe = gRe + sqRe;
            gpIm = gIm + sqIm;
            gmRe = gRe - sqRe;
            gmIm = gIm - sqIm;
            absP = complexAbs(gpRe, gpIm);
            absM = complexAbs(gmRe, gmIm);
            if (absP < absM) {
                gpRe = gmRe;
                gpIm = gmIm;
            }
            if ((absP > 0.0f) || (absM > 0.0f)) {
                complexDiv(coeffNum, 0.0f, gpRe, gpIm, cmplxRes);
                dxRe = cmplxRes[0];
                dxIm = cmplxRes[1];
            } else {
                fooFloat = (1.0f + absX);
                dxRe = fooFloat * (float) Math.cos(iter);
                dxIm = fooFloat * (float) Math.sin(iter);
            }
            x1Re = x[0] - dxRe;
            x1Im = x[1] - dxIm;
            if ((dxRe == 0.0f) && (dxIm == 0.0f)) return iter;
            if ((iter % MT) != 0) {
                x[0] = x1Re;
                x[1] = x1Im;
            } else {
                fooInt = iter / MT;
                x[0] -= frac[fooInt] * dxRe;
                x[1] -= frac[fooInt] * dxIm;
            }
        }
        return 0;
    }

    protected static float complexAbs(float re, float im) {
        if (re == 0.0f) return Math.abs(im);
        if (im == 0.0f) return Math.abs(re);
        return (float) Math.sqrt(re * re + im * im);
    }

    protected static void complexDiv(float aRe, float aIm, float bRe, float bIm, float[] result) {
        float r, den;
        if (Math.abs(bRe) >= Math.abs(bIm)) {
            r = bIm / bRe;
            den = bRe + r * bIm;
            result[0] = (aRe + r * aIm) / den;
            result[1] = (aIm - r * aRe) / den;
        } else {
            r = bRe / bIm;
            den = bIm + r * bRe;
            result[0] = (aRe * r + aIm) / den;
            result[1] = (aIm * r - aRe) / den;
        }
    }

    protected static void complexSqrt(float Re, float Im, float[] result) {
        float absRe, absIm, w, r;
        if ((Re == 0.0f) && (Im == 0.0f)) {
            result[0] = 0.0f;
            result[1] = 0.0f;
        } else {
            absRe = Math.abs(Re);
            absIm = Math.abs(Im);
            if (absRe >= absIm) {
                r = absIm / absRe;
                w = (float) (Math.sqrt(absRe) * Math.sqrt(0.5f * (1.0f + Math.sqrt(1.0f + r * r))));
            } else {
                r = absRe / absIm;
                w = (float) (Math.sqrt(absIm) * Math.sqrt(0.5f * (r + Math.sqrt(1.0f + r * r))));
            }
            if (Re >= 0.0) {
                result[0] = w;
                result[1] = Im / (2.0f * w);
            } else {
                result[1] = (Im >= 0.0f) ? w : -w;
                result[0] = Im / (2.0f * result[1]);
            }
        }
    }
}
