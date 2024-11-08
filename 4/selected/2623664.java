package de.sciss.fscape.op;

import java.io.*;
import de.sciss.fscape.gui.*;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.71, 14-Nov-07
 */
public class Mono2StereoOp extends Operator {

    protected static final String defaultName = "Mono→Stereo";

    protected static Presets static_presets = null;

    protected static Prefs static_prefs = null;

    protected static PropertyArray static_pr = null;

    protected static final int SLOT_INPUT = 0;

    protected static final int SLOT_OUTPUT = 1;

    private static final int PR_PHASEMOD = 0;

    private static final int PR_HIDEPTH = 0;

    private static final int PR_LODEPTH = 1;

    private static final int PR_BANDWIDTH = 2;

    private static final int PR_PHASEMODFREQ = 3;

    private static final int PR_GAIN = 4;

    private static final String PRN_PHASEMOD = "PhaseMod";

    private static final String PRN_HIDEPTH = "HiDepth";

    private static final String PRN_LODEPTH = "LoDepth";

    private static final String PRN_BANDWIDTH = "Bandwidth";

    private static final String PRN_PHASEMODFREQ = "PhaseModFreq";

    private static final String PRN_GAIN = "Gain";

    private static final boolean prBool[] = { true };

    private static final String prBoolName[] = { PRN_PHASEMOD };

    private static final Param prPara[] = { null, null, null, null, null };

    private static final String prParaName[] = { PRN_HIDEPTH, PRN_LODEPTH, PRN_BANDWIDTH, PRN_PHASEMODFREQ, PRN_GAIN };

    protected static final float hiFreq = 16000.0f;

    public Mono2StereoOp() {
        super();
        if (static_prefs == null) {
            static_prefs = new OpPrefs(getClass(), getDefaultPrefs());
        }
        if (static_pr == null) {
            static_pr = new PropertyArray();
            static_pr.bool = prBool;
            static_pr.boolName = prBoolName;
            static_pr.para = prPara;
            static_pr.para[PR_HIDEPTH] = new Param(7.5, Param.DECIBEL_AMP);
            static_pr.para[PR_LODEPTH] = new Param(3.0, Param.DECIBEL_AMP);
            static_pr.para[PR_BANDWIDTH] = new Param(92.0, Param.OFFSET_HZ);
            static_pr.para[PR_PHASEMODFREQ] = new Param(0.66, Param.ABS_HZ);
            static_pr.para[PR_GAIN] = new Param(0.0, Param.DECIBEL_AMP);
            static_pr.paraName = prParaName;
            static_pr.superPr = Operator.op_static_pr;
        }
        if (static_presets == null) {
            static_presets = new Presets(getClass(), static_pr.toProperties(true));
        }
        opName = "Mono2StereoOp";
        prefs = static_prefs;
        presets = static_presets;
        pr = (PropertyArray) static_pr.clone();
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_READER));
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_WRITER));
        icon = new OpIcon(this, OpIcon.ID_MONO2STEREO, defaultName);
    }

    public void run() {
        runInit();
        SpectStreamSlot runInSlot;
        SpectStreamSlot runOutSlot;
        SpectStream runInStream = null;
        SpectStream runOutStream = null;
        SpectFrame runInFr = null;
        SpectFrame runOutFr = null;
        Param ampRef = new Param(1.0, Param.ABS_AMP);
        double gain;
        int ch2;
        boolean recalc = true;
        float phaseOffset = 0.0f;
        float foo;
        int loBand;
        Param freq;
        Param freqFloorOffs;
        Param freqCeilOffs;
        Param freqFloor;
        Param freqCeil;
        float depthFactor[];
        float depth[];
        float freqPhase[];
        double val;
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
            depthFactor = new float[runInStream.bands];
            depth = new float[runInStream.bands];
            freqPhase = new float[runInStream.bands];
            ch2 = 1 % runInStream.chanNum;
            runOutSlot = (SpectStreamSlot) slots.elementAt(SLOT_OUTPUT);
            runOutStream = new SpectStream(runInStream);
            runOutStream.setChannels(2);
            runOutSlot.initWriter(runOutStream);
            gain = (float) Param.transform(pr.para[PR_GAIN], Param.ABS_AMP, ampRef, runInStream).val;
            freq = new Param(0.0, Param.ABS_HZ);
            freqCeilOffs = pr.para[PR_BANDWIDTH];
            freqFloorOffs = new Param(-freqCeilOffs.val, freqCeilOffs.unit);
            loBand = 1;
            for (int i = loBand; i < runInStream.bands; i++) {
                freq.val = i * runInStream.hiFreq / (runInStream.bands - 1);
                freqCeil = Param.transform(freqCeilOffs, Param.ABS_HZ, freq, runInStream);
                freqFloor = Param.transform(freqFloorOffs, Param.ABS_HZ, freq, runInStream);
                freqPhase[i] = (float) ((freq.val / (freqCeil.val - freqFloor.val)) % 1.0);
                depthFactor[i] = (float) (pr.para[PR_LODEPTH].val + ((pr.para[PR_HIDEPTH].val - pr.para[PR_LODEPTH].val) * freq.val / hiFreq));
            }
            runSlotsReady();
            mainLoop: while (!threadDead) {
                if (pr.bool[PR_PHASEMOD]) {
                    foo = (float) (pr.para[PR_PHASEMODFREQ].val * ((runInStream.getTime() / 1000.0) % (1.0 / pr.para[PR_PHASEMODFREQ].val)));
                    if (Math.abs(foo - phaseOffset) >= 0.01) {
                        phaseOffset = foo;
                        recalc = true;
                    }
                }
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
                if (recalc) {
                    for (int i = loBand; i < runInStream.bands; i++) {
                        val = Math.sin(Math.PI * (freqPhase[i] + phaseOffset));
                        val = (val * val - 0.5) * depthFactor[i];
                        depth[i] = (float) Math.exp(val / 20 * Constants.ln10);
                    }
                    recalc = false;
                }
                System.arraycopy(runInFr.data[0], 0, runOutFr.data[0], 0, runInFr.data[0].length);
                System.arraycopy(runInFr.data[ch2], 0, runOutFr.data[1], 0, runInFr.data[ch2].length);
                for (int band = loBand; band < runOutStream.bands; band++) {
                    runOutFr.data[0][(band << 1) + SpectFrame.AMP] *= gain * depth[band];
                    runOutFr.data[1][(band << 1) + SpectFrame.AMP] *= gain / depth[band];
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
        gui = new PropertyGUI("gl" + GroupLabel.NAME_GENERAL + "\n" + "lbHigh freq stereo depth;pf" + Constants.decibelAmpSpace + ",pr" + PRN_HIDEPTH + "\n" + "lbLow freq stereo depth;pf" + Constants.decibelAmpSpace + ",pr" + PRN_LODEPTH + "\n" + "lbBandwidth;pf" + Constants.offsetFreqSpace + "|" + Constants.offsetHzSpace + "|" + Constants.offsetSemitonesSpace + ",pr" + PRN_BANDWIDTH + "\n" + "lbTotal gain;pf" + Constants.decibelAmpSpace + ",pr" + PRN_GAIN + "\n" + "gl" + GroupLabel.NAME_MODULATION + "\n" + "cbPhase motion,actrue|1|en,acfalse|1|di,pr" + PRN_PHASEMOD + ";" + "pf" + Constants.lfoHzSpace + ",id1,pr" + PRN_PHASEMODFREQ);
        return gui;
    }
}
