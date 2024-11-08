package de.sciss.fscape.op;

import java.io.*;
import de.sciss.fscape.gui.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

/**
 *  @version	0.71, 14-Nov-07
 */
public class SplitterOp extends Operator {

    protected static final String defaultName = "Splitter";

    protected static Presets static_presets = null;

    protected static Prefs static_prefs = null;

    protected static PropertyArray static_pr = null;

    protected static final int NUM_OUTPUT = 6;

    protected static final int SLOT_INPUT = 0;

    protected static final int SLOT_OUTPUT = 1;

    private static final int PR_CHANNELS = 0;

    private static final int PR_NORMALIZE = 0;

    private static final int PR_GAINMOD = 1;

    private static final int PR_NORMGAIN = 0;

    private static final int PR_GAIN = 1;

    private static final int PR_GAINMODDEPTH = 7;

    private static final int PR_GAINMODENV = 0;

    private static final int PR_CHANNELS_ALL = 0;

    private static final int PR_CHANNELS_SINGLE = 1;

    private static final String PRN_CHANNELS = "Channels";

    private static final String PRN_GAIN = "Gain";

    private static final String PRN_GAINMOD = "GainMod";

    private static final String PRN_GAINMODDEPTH = "GainModDepth";

    private static final String PRN_GAINMODENV = "GainModEnv";

    private static final String PRN_NORMALIZE = "Normalize";

    private static final String PRN_NORMGAIN = "NormGain";

    private static final int prIntg[] = { PR_CHANNELS_ALL };

    private static final String prIntgName[] = { PRN_CHANNELS };

    public SplitterOp() {
        super();
        if (static_prefs == null) {
            static_prefs = new OpPrefs(getClass(), getDefaultPrefs());
        }
        if (static_pr == null) {
            static_pr = new PropertyArray();
            static_pr.intg = prIntg;
            static_pr.intgName = prIntgName;
            static_pr.bool = new boolean[1 + NUM_OUTPUT];
            static_pr.boolName = new String[1 + NUM_OUTPUT];
            static_pr.para = new Param[1 + (NUM_OUTPUT << 1)];
            static_pr.paraName = new String[1 + (NUM_OUTPUT << 1)];
            static_pr.envl = new Envelope[NUM_OUTPUT];
            static_pr.envlName = new String[NUM_OUTPUT];
            static_pr.bool[PR_NORMALIZE] = false;
            static_pr.boolName[PR_NORMALIZE] = PRN_NORMALIZE;
            static_pr.para[PR_NORMGAIN] = new Param(0.0, Param.DECIBEL_AMP);
            static_pr.paraName[PR_NORMGAIN] = PRN_NORMGAIN;
            for (int i = 0; i < NUM_OUTPUT; i++) {
                static_pr.para[PR_GAIN + i] = new Param(0.0, Param.DECIBEL_AMP);
                static_pr.para[PR_GAINMODDEPTH + i] = new Param(96.0, Param.DECIBEL_AMP);
                static_pr.paraName[PR_GAIN + i] = PRN_GAIN + (i + 1);
                static_pr.paraName[PR_GAINMODDEPTH + i] = PRN_GAINMODDEPTH + (i + 1);
                static_pr.envl[PR_GAINMODENV + i] = Envelope.createBasicEnvelope(Envelope.BASIC_TIME);
                static_pr.envlName[PR_GAINMODENV + i] = PRN_GAINMODENV + (i + 1);
                static_pr.bool[PR_GAINMOD + i] = false;
                static_pr.boolName[PR_GAINMOD + i] = PRN_GAINMOD + (i + 1);
            }
            static_pr.superPr = Operator.op_static_pr;
        }
        if (static_presets == null) {
            static_presets = new Presets(getClass(), static_pr.toProperties(true));
        }
        opName = "SplitterOp";
        prefs = static_prefs;
        presets = static_presets;
        pr = (PropertyArray) static_pr.clone();
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_READER));
        for (int i = 0; i < NUM_OUTPUT; i++) {
            slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_WRITER, Slots.SLOTS_DEFWRITER + (i + 1)));
        }
        icon = new OpIcon(this, OpIcon.ID_SPLITTER, defaultName);
    }

    /**
	 *	TESTSTADIUM XXX
	 */
    public void run() {
        runInit();
        SpectStreamSlot runInSlot;
        SpectStreamSlot runOutSlot[] = new SpectStreamSlot[NUM_OUTPUT];
        SpectStream runInStream = null;
        SpectStream runOutStream[] = new SpectStream[NUM_OUTPUT];
        SpectFrame runInFr = null;
        SpectFrame runOutFr[] = new SpectFrame[NUM_OUTPUT];
        int runSlotNum[] = new int[NUM_OUTPUT];
        int runChanNum[] = new int[NUM_OUTPUT];
        Param ampRef = new Param(1.0, Param.ABS_AMP);
        double normBase;
        Param gainBase[] = new Param[NUM_OUTPUT];
        float gain[] = new float[NUM_OUTPUT];
        Modulator gainMod[] = new Modulator[NUM_OUTPUT];
        double sumGain;
        int numOut = 0;
        int oldWriteDone;
        int writeable;
        int chanNum;
        float srcData[];
        float destData[];
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
            for (int i = 0; i < NUM_OUTPUT; i++) {
                runOutSlot[numOut] = (SpectStreamSlot) slots.elementAt(SLOT_OUTPUT + i);
                if (runOutSlot[numOut].getLinked() != null) {
                    runOutStream[numOut] = new SpectStream(runInStream);
                    if (pr.intg[PR_CHANNELS] == PR_CHANNELS_SINGLE) {
                        runOutStream[numOut].setChannels(1);
                        runChanNum[numOut] = numOut % runInStream.chanNum;
                    } else {
                        runChanNum[numOut] = 0;
                    }
                    runOutSlot[numOut].initWriter(runOutStream[numOut]);
                    runSlotNum[numOut] = i;
                    numOut++;
                }
            }
            normBase = Param.transform(pr.para[PR_NORMGAIN], Param.ABS_AMP, ampRef, runInStream).val;
            for (int i = 0; i < numOut; i++) {
                gainBase[i] = Param.transform(pr.para[PR_GAIN + runSlotNum[i]], Param.ABS_AMP, ampRef, runInStream);
                if (pr.bool[PR_GAINMOD + runSlotNum[i]]) {
                    gainMod[i] = new Modulator(gainBase[i], pr.para[PR_GAINMODDEPTH + runSlotNum[i]], pr.envl[PR_GAINMODENV + runSlotNum[i]], runInStream);
                }
            }
            if (pr.intg[PR_CHANNELS] == PR_CHANNELS_SINGLE) {
                chanNum = 1;
            } else {
                chanNum = runInStream.chanNum;
            }
            runSlotsReady();
            mainLoop: while (!threadDead && (numOut > 0)) {
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
                sumGain = 0.0;
                for (int i = 0; i < numOut; i++) {
                    if (pr.bool[PR_GAINMOD + runSlotNum[i]]) {
                        gain[i] = (float) gainMod[i].calc().val;
                    } else {
                        gain[i] = (float) gainBase[i].val;
                    }
                    sumGain += gain[i];
                }
                if (pr.bool[PR_NORMALIZE]) {
                    for (int i = 0; i < numOut; i++) {
                        gain[i] *= (float) (normBase / sumGain);
                    }
                }
                calcFrame: for (int i = 0; i < numOut; i++) {
                    if ((Math.abs(gain[i] - 1.0f) < 0.01f) && (pr.intg[PR_CHANNELS] == PR_CHANNELS_ALL)) {
                        runOutFr[i] = new SpectFrame(runInFr);
                        gain[i] = 1.0f;
                    } else {
                        for (int j = 0; j < i; j++) {
                            if ((Math.abs(gain[i] - gain[j]) < 0.01f) && ((pr.intg[PR_CHANNELS] == PR_CHANNELS_ALL) || ((i % runInStream.chanNum) == (j % runInStream.chanNum)))) {
                                runOutFr[i] = new SpectFrame(runOutFr[j]);
                                continue calcFrame;
                            }
                        }
                        runOutFr[i] = runOutStream[i].allocFrame();
                        for (int ch = 0; ch < chanNum; ch++) {
                            srcData = runInFr.data[runChanNum[i] + ch];
                            destData = runOutFr[i].data[ch];
                            if (gain[i] < 0.01f) {
                                for (int j = 0; j < destData.length; j++) {
                                    destData[j] = 0.0f;
                                }
                            } else {
                                System.arraycopy(srcData, 0, destData, 0, srcData.length);
                                if (Math.abs(gain[i] - 1.0f) >= 0.01f) {
                                    for (int j = 0; j < destData.length; j += 2) {
                                        destData[j + SpectFrame.AMP] *= gain[i];
                                    }
                                } else {
                                    gain[i] = 1.0f;
                                }
                            }
                        }
                    }
                }
                runFrameDone(runInSlot, runInFr);
                runInSlot.freeFrame(runInFr);
                for (int writeDone = 0; (writeDone < numOut) && !threadDead; ) {
                    oldWriteDone = writeDone;
                    for (int i = 0; i < numOut; i++) {
                        try {
                            if (runOutFr[i] != null) {
                                writeable = runOutStream[i].framesWriteable();
                                if (writeable > 0) {
                                    runOutSlot[i].writeFrame(runOutFr[i]);
                                    writeDone++;
                                    runOutStream[i].freeFrame(runOutFr[i]);
                                    runOutFr[i] = null;
                                } else if (writeable < 0) {
                                    writeDone++;
                                    runFrameDone(runOutSlot[i], runOutFr[i]);
                                    runOutStream[i].freeFrame(runOutFr[i]);
                                    for (int j = i + 1; j < numOut; j++) {
                                        runOutSlot[j] = runOutSlot[j - 1];
                                        runOutStream[j] = runOutStream[j - 1];
                                        runOutFr[j] = runOutFr[j - 1];
                                        runSlotNum[j] = runSlotNum[j - 1];
                                        runChanNum[j] = runChanNum[j - 1];
                                        gainBase[j] = gainBase[j - 1];
                                        gain[j] = gain[j - 1];
                                        gainMod[j] = gainMod[j - 1];
                                    }
                                    numOut--;
                                    i--;
                                }
                            }
                        } catch (InterruptedException e) {
                            break mainLoop;
                        }
                        runCheckPause();
                    }
                    if (oldWriteDone == writeDone) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                        }
                        runCheckPause();
                    }
                }
            }
            runInStream.closeReader();
            for (int i = 0; i < numOut; i++) {
                runOutStream[i].closeWriter();
            }
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
        StringBuffer gain = new StringBuffer();
        StringBuffer gainMod = new StringBuffer();
        if (type != GUI_PREFS) return null;
        for (int i = 1; i <= NUM_OUTPUT; i++) {
            gain.append("\nlbSlot " + i + ";pf" + Constants.decibelAmpSpace + ",id" + (i << 2) + ",pr" + pr.paraName[PR_GAIN + i - 1]);
            gainMod.append("\ncbSlot " + i + " Gain,actrue|" + ((i << 2) + 1) + "|en|" + ((i << 2) + 2) + "|en,acfalse|" + ((i << 2) + 1) + "|di|" + ((i << 2) + 2) + "|di,pr" + (PRN_GAINMOD + i) + ";pf" + Constants.decibelAmpSpace + ",id" + ((i << 2) + 1) + ",pr" + (PRN_GAINMODDEPTH + i) + ";en,id" + ((i << 2) + 2) + ",pr" + (PRN_GAINMODENV + i));
        }
        gui = new PropertyGUI("gl" + GroupLabel.NAME_GENERAL + "\n" + "lbEach slot carries;ch,pr" + PRN_CHANNELS + "," + "itAll channels," + "itOne channel\n" + "cbNormalize sum,actrue|100|en,acfalse|100|di,pr" + PRN_NORMALIZE + ";" + "pf" + Constants.decibelAmpSpace + ",id100,pr" + PRN_NORMGAIN + "\n" + "glOutput slot gain" + gain + "\ngl" + GroupLabel.NAME_MODULATION + gainMod);
        return gui;
    }
}
