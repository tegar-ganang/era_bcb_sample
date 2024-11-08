package de.sciss.fscape.op;

import java.io.EOFException;
import java.io.IOException;
import de.sciss.fscape.gui.GroupLabel;
import de.sciss.fscape.gui.OpIcon;
import de.sciss.fscape.gui.PropertyGUI;
import de.sciss.fscape.prop.OpPrefs;
import de.sciss.fscape.prop.Prefs;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.spect.SpectFrame;
import de.sciss.fscape.spect.SpectStream;
import de.sciss.fscape.spect.SpectStreamSlot;
import de.sciss.fscape.util.Envelope;
import de.sciss.fscape.util.Modulator;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.Slots;
import de.sciss.fscape.util.Util;

/**
 * 	@author		Hanns Holger Rutz
 *  @version	0.71, 19-Feb-10
 */
public class AmpEnvOp extends Operator {

    protected static final String defaultName = "Amp Env";

    protected static Presets static_presets = null;

    protected static Prefs static_prefs = null;

    protected static PropertyArray static_pr = null;

    protected static final int SLOT_INPUT = 0;

    protected static final int SLOT_OUTPUT = 1;

    private static final int PR_ENV = 0;

    private static final String PRN_ENV = "Env";

    private static final Envelope prEnvl[] = { null };

    private static final String prEnvlName[] = { PRN_ENV };

    public AmpEnvOp() {
        super();
        if (static_prefs == null) {
            static_prefs = new OpPrefs(getClass(), getDefaultPrefs());
        }
        if (static_pr == null) {
            static_pr = new PropertyArray();
            static_pr.envl = prEnvl;
            static_pr.envl[PR_ENV] = Envelope.createBasicEnvelope(Envelope.BASIC_TIME);
            static_pr.envlName = prEnvlName;
            static_pr.superPr = Operator.op_static_pr;
        }
        if (static_presets == null) {
            static_presets = new Presets(getClass(), static_pr.toProperties(true));
        }
        opName = "AmpEnvOp";
        prefs = static_prefs;
        presets = static_presets;
        pr = (PropertyArray) static_pr.clone();
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_READER));
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_WRITER));
        icon = new OpIcon(this, OpIcon.ID_FLIPFREQ, defaultName);
    }

    public void run() {
        runInit();
        final SpectStreamSlot runInSlot;
        final SpectStreamSlot runOutSlot;
        SpectStream runInStream = null;
        final SpectStream runOutStream;
        SpectFrame runInFr = null;
        SpectFrame runOutFr = null;
        float gain;
        float[] convBuf1;
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
            final Modulator envMod = new Modulator(new Param(0.0, Param.ABS_AMP), new Param(1.0, Param.ABS_AMP), pr.envl[PR_ENV], runInStream);
            runSlotsReady();
            mainLoop: while (!threadDead) {
                gain = (float) envMod.calc().val;
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
                Util.copy(runInFr.data, 0, runOutFr.data, 0, runInStream.bands << 1);
                for (int ch = 0; ch < runInStream.chanNum; ch++) {
                    convBuf1 = runOutFr.data[ch];
                    for (int i = 0, j = 0; j < runInStream.bands; i += 2, j++) {
                        convBuf1[i] *= gain;
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
        gui = new PropertyGUI("gl" + GroupLabel.NAME_GENERAL + "\n" + "en,pr" + PRN_ENV);
        return gui;
    }
}
