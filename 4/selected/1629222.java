package de.sciss.fscape.op;

import java.io.*;
import de.sciss.fscape.gui.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

/**
 *  @version	0.71, 14-Nov-07
 */
public class SmearOp extends Operator {

    protected static final String defaultName = "Smear";

    protected static Presets static_presets = null;

    protected static Prefs static_prefs = null;

    protected static PropertyArray static_pr = null;

    protected static final int SLOT_INPUT = 0;

    protected static final int SLOT_OUTPUT = 1;

    private static final int PR_DECAYMOD = 0;

    private static final int PR_MODE = 0;

    private static final int PR_DRYAPPLY = 1;

    private static final int PR_DECAY = 0;

    private static final int PR_DECAYMODDEPTH = 1;

    private static final int PR_DECAYMODENV = 0;

    private static final int PR_MODE_SMEAR = 0;

    private static final int PR_MODE_FREEZE = 1;

    private static final int PR_APPLY_NONE = 0;

    private static final int PR_APPLY_SUB = 1;

    private static final int PR_APPLY_THRESH = 2;

    private static final String PRN_DECAYMOD = "DecayMod";

    private static final String PRN_MODE = "Mode";

    private static final String PRN_DRYAPPLY = "LoDepth";

    private static final String PRN_DECAY = "Decay";

    private static final String PRN_DECAYMODDEPTH = "DecayModDepth";

    private static final String PRN_DECAYMODENV = "DecayModEnv";

    private static final boolean prBool[] = { false };

    private static final String prBoolName[] = { PRN_DECAYMOD };

    private static final int prIntg[] = { PR_MODE_SMEAR, PR_APPLY_NONE };

    private static final String prIntgName[] = { PRN_MODE, PRN_DRYAPPLY };

    private static final Param prPara[] = { null, null };

    private static final String prParaName[] = { PRN_DECAY, PRN_DECAYMODDEPTH };

    private static final Envelope prEnvl[] = { null };

    private static final String prEnvlName[] = { PRN_DECAYMODENV };

    public SmearOp() {
        super();
        if (static_prefs == null) {
            static_prefs = new OpPrefs(getClass(), getDefaultPrefs());
        }
        if (static_pr == null) {
            static_pr = new PropertyArray();
            static_pr.bool = prBool;
            static_pr.boolName = prBoolName;
            static_pr.intg = prIntg;
            static_pr.intgName = prIntgName;
            static_pr.para = prPara;
            static_pr.para[PR_DECAY] = new Param(60, Param.DECIBEL_AMP);
            static_pr.para[PR_DECAYMODDEPTH] = new Param(60, Param.DECIBEL_AMP);
            static_pr.paraName = prParaName;
            static_pr.envl = prEnvl;
            static_pr.envl[PR_DECAYMODENV] = Envelope.createBasicEnvelope(Envelope.BASIC_TIME);
            static_pr.envlName = prEnvlName;
            static_pr.superPr = Operator.op_static_pr;
        }
        if (static_presets == null) {
            static_presets = new Presets(getClass(), static_pr.toProperties(true));
        }
        opName = "SmearOp";
        prefs = static_prefs;
        presets = static_presets;
        pr = (PropertyArray) static_pr.clone();
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_READER));
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_WRITER));
        icon = new OpIcon(this, OpIcon.ID_SMEAR, defaultName);
    }

    public void run() {
        runInit();
        SpectStreamSlot runInSlot;
        SpectStreamSlot runOutSlot;
        SpectStream runInStream = null;
        SpectStream runOutStream = null;
        SpectFrame runInFr = null;
        SpectFrame runOutFr = null;
        SpectFrame bufFr = null;
        Param ampRef = new Param(1.0, Param.ABS_AMP);
        Param decayBase;
        Param decay;
        Modulator decayMod = null;
        float srcAmp, srcPhase;
        float srcAmp2, srcPhase2;
        double destReal, destImg;
        int divisor = 0;
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
            decayBase = Param.transform(new Param(pr.para[PR_DECAY].val * SpectStream.framesToMillis(runInStream, 1) / 1000, pr.para[PR_DECAY].unit), Param.ABS_AMP, ampRef, runInStream);
            decay = decayBase;
            if (pr.bool[PR_DECAYMOD]) {
                decayMod = new Modulator(decayBase, pr.para[PR_DECAYMODDEPTH], pr.envl[PR_DECAYMODENV], runInStream);
            }
            bufFr = new SpectFrame(runInStream.chanNum, runInStream.bands);
            SpectFrame.clear(bufFr);
            runSlotsReady();
            mainLoop: while (!threadDead) {
                if (pr.bool[PR_DECAYMOD]) {
                    decay = decayMod.calc();
                }
                for (boolean readDone = false; (readDone == false) && !threadDead; ) {
                    try {
                        runInFr = runInSlot.readFrame();
                        readDone = true;
                    } catch (InterruptedException e) {
                    } catch (EOFException e) {
                        break mainLoop;
                    }
                    runCheckPause();
                }
                if (threadDead) break mainLoop;
                switch(pr.intg[PR_MODE]) {
                    case PR_MODE_SMEAR:
                        switch(pr.intg[PR_DRYAPPLY]) {
                            case PR_APPLY_NONE:
                                for (int ch = 0; ch < runOutStream.chanNum; ch++) {
                                    for (int band = 0; band < runOutStream.bands; band++) {
                                        srcAmp = bufFr.data[ch][(band << 1) + SpectFrame.AMP] / (float) decay.val;
                                        srcPhase = bufFr.data[ch][(band << 1) + SpectFrame.PHASE];
                                        destImg = srcAmp * Math.sin(srcPhase);
                                        destReal = srcAmp * Math.cos(srcPhase);
                                        srcAmp = runInFr.data[ch][(band << 1) + SpectFrame.AMP];
                                        srcPhase = runInFr.data[ch][(band << 1) + SpectFrame.PHASE];
                                        destImg += srcAmp * Math.sin(srcPhase);
                                        destReal += srcAmp * Math.cos(srcPhase);
                                        bufFr.data[ch][(band << 1) + SpectFrame.AMP] = (float) Math.sqrt(destImg * destImg + destReal * destReal);
                                        bufFr.data[ch][(band << 1) + SpectFrame.PHASE] = (float) Math.atan2(destImg, destReal);
                                    }
                                }
                                runOutFr = new SpectFrame(bufFr);
                                break;
                            case PR_APPLY_SUB:
                                runOutFr = runOutStream.allocFrame();
                                for (int ch = 0; ch < runOutStream.chanNum; ch++) {
                                    for (int band = 0; band < runOutStream.bands; band++) {
                                        srcAmp = bufFr.data[ch][(band << 1) + SpectFrame.AMP] / (float) decay.val;
                                        srcPhase = bufFr.data[ch][(band << 1) + SpectFrame.PHASE];
                                        runOutFr.data[ch][(band << 1) + SpectFrame.AMP] = srcAmp;
                                        runOutFr.data[ch][(band << 1) + SpectFrame.PHASE] = srcPhase;
                                        destImg = srcAmp * Math.sin(srcPhase);
                                        destReal = srcAmp * Math.cos(srcPhase);
                                        srcAmp = runInFr.data[ch][(band << 1) + SpectFrame.AMP];
                                        srcPhase = runInFr.data[ch][(band << 1) + SpectFrame.PHASE];
                                        destImg += srcAmp * Math.sin(srcPhase);
                                        destReal += srcAmp * Math.cos(srcPhase);
                                        bufFr.data[ch][(band << 1) + SpectFrame.AMP] = (float) Math.sqrt(destImg * destImg + destReal * destReal);
                                        bufFr.data[ch][(band << 1) + SpectFrame.PHASE] = (float) Math.atan2(destImg, destReal);
                                    }
                                }
                                break;
                            case PR_APPLY_THRESH:
                                runOutFr = runOutStream.allocFrame();
                                for (int ch = 0; ch < runOutStream.chanNum; ch++) {
                                    for (int band = 0; band < runOutStream.bands; band++) {
                                        srcAmp = bufFr.data[ch][(band << 1) + SpectFrame.AMP] / (float) decay.val;
                                        srcPhase = bufFr.data[ch][(band << 1) + SpectFrame.PHASE];
                                        destImg = srcAmp * Math.sin(srcPhase);
                                        destReal = srcAmp * Math.cos(srcPhase);
                                        srcAmp2 = runInFr.data[ch][(band << 1) + SpectFrame.AMP];
                                        srcPhase2 = runInFr.data[ch][(band << 1) + SpectFrame.PHASE];
                                        destImg += srcAmp2 * Math.sin(srcPhase2);
                                        destReal += srcAmp2 * Math.cos(srcPhase2);
                                        bufFr.data[ch][(band << 1) + SpectFrame.AMP] = (float) Math.sqrt(destImg * destImg + destReal * destReal);
                                        bufFr.data[ch][(band << 1) + SpectFrame.PHASE] = (float) Math.atan2(destImg, destReal);
                                        runOutFr.data[ch][(band << 1) + SpectFrame.AMP] = Math.max(0.0f, srcAmp - srcAmp2);
                                        runOutFr.data[ch][(band << 1) + SpectFrame.PHASE] = srcPhase;
                                    }
                                }
                                break;
                        }
                        break;
                    case PR_MODE_FREEZE:
                        divisor++;
                        runOutFr = runOutStream.allocFrame();
                        switch(pr.intg[PR_DRYAPPLY]) {
                            case PR_APPLY_NONE:
                                for (int ch = 0; ch < runOutStream.chanNum; ch++) {
                                    for (int band = 0; band < runOutStream.bands; band++) {
                                        srcAmp = runInFr.data[ch][(band << 1) + SpectFrame.AMP];
                                        srcPhase = runInFr.data[ch][(band << 1) + SpectFrame.PHASE];
                                        bufFr.data[ch][band << 1] += srcAmp * Math.sin(srcPhase);
                                        bufFr.data[ch][(band << 1) + 1] += srcAmp * Math.cos(srcPhase);
                                        destImg = bufFr.data[ch][band << 1] / divisor;
                                        destReal = bufFr.data[ch][(band << 1) + 1] / divisor;
                                        runOutFr.data[ch][(band << 1) + SpectFrame.AMP] = (float) Math.sqrt(destImg * destImg + destReal * destReal);
                                        runOutFr.data[ch][(band << 1) + SpectFrame.PHASE] = (float) Math.atan2(destImg, destReal);
                                    }
                                }
                                break;
                            case PR_APPLY_SUB:
                                for (int ch = 0; ch < runOutStream.chanNum; ch++) {
                                    for (int band = 0; band < runOutStream.bands; band++) {
                                        srcAmp = runInFr.data[ch][(band << 1) + SpectFrame.AMP];
                                        srcPhase = runInFr.data[ch][(band << 1) + SpectFrame.PHASE];
                                        destImg = srcAmp * Math.sin(srcPhase);
                                        destReal = srcAmp * Math.cos(srcPhase);
                                        bufFr.data[ch][band << 1] += destImg;
                                        bufFr.data[ch][(band << 1) + 1] += destReal;
                                        destImg -= bufFr.data[ch][band << 1] / divisor;
                                        destReal -= bufFr.data[ch][(band << 1) + 1] / divisor;
                                        runOutFr.data[ch][(band << 1) + SpectFrame.AMP] = (float) Math.sqrt(destImg * destImg + destReal * destReal);
                                        runOutFr.data[ch][(band << 1) + SpectFrame.PHASE] = (float) Math.atan2(destImg, destReal);
                                    }
                                }
                                break;
                            case PR_APPLY_THRESH:
                                for (int ch = 0; ch < runOutStream.chanNum; ch++) {
                                    for (int band = 0; band < runOutStream.bands; band++) {
                                        srcAmp = runInFr.data[ch][(band << 1) + SpectFrame.AMP];
                                        srcPhase = runInFr.data[ch][(band << 1) + SpectFrame.PHASE];
                                        bufFr.data[ch][band << 1] += srcAmp * Math.sin(srcPhase);
                                        bufFr.data[ch][(band << 1) + 1] += srcAmp * Math.cos(srcPhase);
                                        destImg = bufFr.data[ch][band << 1] / divisor;
                                        destReal = bufFr.data[ch][(band << 1) + 1] / divisor;
                                        runOutFr.data[ch][(band << 1) + SpectFrame.AMP] = Math.max(0.0f, srcAmp - (float) Math.sqrt(destImg * destImg + destReal * destReal));
                                        runOutFr.data[ch][(band << 1) + SpectFrame.PHASE] = srcPhase;
                                    }
                                }
                                break;
                        }
                        break;
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
        gui = new PropertyGUI("gl" + GroupLabel.NAME_GENERAL + "\n" + "lbMode;ch,ac0|1|en|2|en,ac1|1|di|2|di,pr" + PRN_MODE + "," + "itSmear," + "itFreeze\n" + "lbDecay per sec;pf" + Constants.decibelAmpSpace + ",id1,pr" + PRN_DECAY + "\n" + "lbDry application;ch,pr" + PRN_DRYAPPLY + "," + "itNone," + "itSubtract," + "itThreshold\n" + "gl" + GroupLabel.NAME_MODULATION + "\n" + "cbDecay,actrue|3|en|4|en,acfalse|3|di|4|di,id2,pr" + PRN_DECAYMOD + ";" + "pf" + Constants.decibelAmpSpace + "|" + Constants.offsetAmpSpace + ",re1,id3,pr" + PRN_DECAYMODDEPTH + ";en,id4,pr" + PRN_DECAYMODENV);
        return gui;
    }
}
