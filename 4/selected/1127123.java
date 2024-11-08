package de.sciss.fscape.op;

import java.awt.*;
import java.io.*;
import de.sciss.fscape.gui.*;
import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.*;
import de.sciss.fscape.spect.*;
import de.sciss.fscape.util.*;

/**
 *	Input Operator zum Einlesen von fft Files
 *
 *  @version	0.71, 14-Nov-07
 */
public class InputOp extends Operator {

    protected static final String defaultName = "Input file";

    protected static Presets static_presets = null;

    protected static Prefs static_prefs = null;

    protected static PropertyArray static_pr = null;

    protected static final int SLOT_OUTPUT = 0;

    private static final int PR_FILENAME = 0;

    private static final int PR_CHANNELS = 0;

    private static final int PR_ADJUSTSTART = 0;

    private static final int PR_ADJUSTLENGTH = 1;

    private static final int PR_REMOVEDC = 2;

    private static final int PR_STARTSHIFT = 0;

    private static final int PR_LENGTH = 1;

    private static final String PRN_FILENAME = "Filename";

    private static final String PRN_CHANNELS = "Channels";

    private static final String PRN_ADJUSTSTART = "AdjustStart";

    private static final String PRN_ADJUSTLENGTH = "AdjustLength";

    private static final String PRN_REMOVEDC = "RemoveDC";

    private static final String PRN_STARTSHIFT = "StartShift";

    private static final String PRN_LENGTH = "Length";

    private static final int PR_CHANNELS_UNTOUCHED = SpectFrame.FLAGS_UNTOUCHED;

    private static final String prText[] = { "" };

    private static final String prTextName[] = { PRN_FILENAME };

    private static final int prIntg[] = { PR_CHANNELS_UNTOUCHED };

    private static final String prIntgName[] = { PRN_CHANNELS };

    private static final boolean prBool[] = { false, false, false };

    private static final String prBoolName[] = { PRN_ADJUSTSTART, PRN_ADJUSTLENGTH, PRN_REMOVEDC };

    private static final Param prPara[] = { null, null };

    private static final String prParaName[] = { PRN_STARTSHIFT, PRN_LENGTH };

    protected static final String ERR_NOINPUT = "No input file";

    public InputOp() {
        super();
        if (static_prefs == null) {
            static_prefs = new OpPrefs(getClass(), getDefaultPrefs());
        }
        if (static_pr == null) {
            static_pr = new PropertyArray();
            static_pr.text = prText;
            static_pr.textName = prTextName;
            static_pr.intg = prIntg;
            static_pr.intgName = prIntgName;
            static_pr.bool = prBool;
            static_pr.boolName = prBoolName;
            static_pr.para = prPara;
            static_pr.para[PR_STARTSHIFT] = new Param(0.0, Param.ABS_MS);
            static_pr.para[PR_LENGTH] = new Param(5000.0, Param.ABS_MS);
            static_pr.paraName = prParaName;
            static_pr.superPr = Operator.op_static_pr;
        }
        if (static_presets == null) {
            static_presets = new Presets(getClass(), static_pr.toProperties(true));
        }
        opName = "InputOp";
        prefs = static_prefs;
        presets = static_presets;
        pr = (PropertyArray) static_pr.clone();
        slots.addElement(new SpectStreamSlot(this, Slots.SLOTS_WRITER));
        icon = new OpIcon(this, OpIcon.ID_INPUT, defaultName);
    }

    /**
	 *	TESTSTADIUM XXX
	 */
    public void run() {
        runInit();
        String runFileName;
        SpectralFile runFile = null;
        int runStartFrame;
        int runFrames;
        int runFramesRead = 0;
        SpectStreamSlot runOutSlot;
        SpectStream runInStream;
        SpectStream runOutStream;
        SpectFrame runInFr;
        SpectFrame runOutFr;
        Param inLength;
        double startShift;
        double outLength;
        if ((pr.text[PR_FILENAME] == null) || (pr.text[PR_FILENAME].length() == 0)) {
            FileDialog fDlg;
            String fiName, fiDir;
            final Component ownerF = owner.getWindow().getWindow();
            final boolean makeF = !(ownerF instanceof Frame);
            final Frame f = makeF ? new Frame() : (Frame) ownerF;
            fDlg = new FileDialog(f, ((OpIcon) getIcon()).getName() + ": Select inputfile");
            fDlg.setVisible(true);
            if (makeF) f.dispose();
            fiName = fDlg.getFile();
            fiDir = fDlg.getDirectory();
            fDlg.dispose();
            if (fiDir == null) fiDir = "";
            if (fiName == null) {
                runQuit(new IOException(ERR_NOINPUT));
                return;
            }
            runFileName = fiDir + fiName;
        } else {
            runFileName = pr.text[PR_FILENAME];
        }
        try {
            runFile = new SpectralFile(runFileName, GenericFile.MODE_INPUT | (pr.bool[PR_REMOVEDC] ? SpectralFile.MODE_REMOVEDC : 0));
            runInStream = runFile.getDescr();
            inLength = new Param(SpectStream.framesToMillis(runInStream, runInStream.frames), Param.ABS_MS);
            startShift = Param.transform(pr.para[PR_STARTSHIFT], Param.ABS_MS, inLength, runInStream).val;
            outLength = Param.transform(pr.para[PR_LENGTH], Param.ABS_MS, inLength, runInStream).val;
            if (pr.bool[PR_ADJUSTSTART]) {
                runStartFrame = (int) SpectStream.millisToFrames(runInStream, startShift);
                if (runStartFrame >= runInStream.frames) {
                    runStartFrame = runInStream.frames;
                }
            } else {
                runStartFrame = 0;
            }
            runFile.seekFrame(runStartFrame);
            if (pr.bool[PR_ADJUSTLENGTH]) {
                runFrames = (int) SpectStream.millisToFrames(runInStream, outLength);
                if (runStartFrame + runFrames > runInStream.frames) {
                    runFrames = runInStream.frames - runStartFrame;
                }
            } else {
                runFrames = runInStream.frames - runStartFrame;
            }
            runInStream.setEstimatedLength(runFrames);
            runOutStream = new SpectStream(runInStream);
            if (pr.intg[PR_CHANNELS] != PR_CHANNELS_UNTOUCHED) {
                runOutStream.setChannels(1);
            }
            runOutSlot = ((SpectStreamSlot) slots.elementAt(SLOT_OUTPUT));
            runOutSlot.initWriter(runOutStream);
            runSlotsReady();
            while ((runFramesRead < runFrames) && !threadDead) {
                runInFr = runFile.readFrame();
                runFramesRead++;
                runOutFr = new SpectFrame(runInFr, pr.intg[PR_CHANNELS]);
                runFile.freeFrame(runInFr);
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
            runFile.close();
            runOutStream.closeWriter();
        } catch (IOException e) {
            if (runFile != null) {
                runFile.cleanUp();
            }
            runQuit(e);
            return;
        } catch (SlotAlreadyConnectedException e) {
            if (runFile != null) {
                runFile.cleanUp();
            }
            runQuit(e);
            return;
        }
        runQuit(null);
    }

    public PropertyGUI createGUI(int type) {
        PropertyGUI gui;
        SpectralFile inputFile = null;
        SpectStream inStream;
        double fileLength;
        String reference = "";
        if (type != GUI_PREFS) return null;
        if ((pr.text[PR_FILENAME] != null) && (pr.text[PR_FILENAME].length() != 0)) {
            try {
                inputFile = new SpectralFile(pr.text[PR_FILENAME], GenericFile.MODE_INPUT);
                inStream = inputFile.getDescr();
                fileLength = (double) (inStream.frames * inStream.smpPerFrame) / ((double) inStream.smpRate / 1000);
                inputFile.close();
                reference = ",re" + fileLength + "|" + Param.ABS_MS;
            } catch (IOException e) {
                if (inputFile != null) inputFile.cleanUp();
            }
        }
        gui = new PropertyGUI("gl" + GroupLabel.NAME_GENERAL + "\n" + "lbFile name;io" + PathField.TYPE_INPUTFILE + "|Select input file,pr" + PRN_FILENAME + "\n" + "lbChannel mode;ch,pr" + PRN_CHANNELS + "," + "itLeave untouched," + "itLeft channel only," + "itRight channel only," + "itSum left + right\n" + "cbRemove DC offset,pr" + PRN_REMOVEDC + "\n" + "glTruncation\n" + "cbStart shift,actrue|1|en,acfalse|1|di,pr" + PRN_ADJUSTSTART + ";" + "pf" + Constants.absMsSpace + "|" + Constants.absBeatsSpace + "|" + Constants.ratioTimeSpace + reference + ",id1,pr" + PRN_STARTSHIFT + "\n" + "cbLength,actrue|2|en,acfalse|2|di,pr" + PRN_ADJUSTLENGTH + ";" + "pf" + Constants.absMsSpace + "|" + Constants.absBeatsSpace + "|" + Constants.ratioTimeSpace + reference + ",id2,pr" + PRN_LENGTH);
        return gui;
    }
}
