package de.sciss.fscape.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import de.sciss.fscape.io.GenericFile;
import de.sciss.fscape.prop.Presets;
import de.sciss.fscape.prop.PropertyArray;
import de.sciss.fscape.session.DocumentFrame;
import de.sciss.fscape.util.Constants;
import de.sciss.fscape.util.Filter;
import de.sciss.fscape.util.Param;
import de.sciss.fscape.util.ParamSpace;
import de.sciss.fscape.util.Util;
import de.sciss.io.AudioFile;
import de.sciss.io.AudioFileDescr;

/**
 *  @author		Hanns Holger Rutz
 *  @version	0.72, 11-Aug-09
 *  
 *  @todo		multichannel version: could use the next
 *  			vector (row in u multiplied by s-gain), or hue and saturation
 */
public class BeltramiDecompositionDlg extends DocumentFrame {

    private static final int PR_INIMGFILE = 0;

    private static final int PR_OUTPUTFILE = 1;

    private static final int PR_OUTPUTTYPE = 0;

    private static final int PR_OUTPUTRES = 1;

    private static final int PR_OUTPUTRATE = 2;

    private static final int PR_GAINTYPE = 3;

    private static final int PR_SCANDIR = 4;

    private static final int PR_CLPSEDIR = 5;

    private static final int PR_GAIN = 0;

    private static final int PR_CHUNKSIZE = 1;

    private static final int PR_SPACEOVERLAP = 2;

    private static final int PR_TIMEOVERLAP = 3;

    private static final int PR_TIMEJITTER = 4;

    private static final int PR_HIGHPASS = 0;

    private static final String PRN_INIMGFILE = "InImgFile";

    private static final String PRN_OUTPUTFILE = "OutputFile";

    private static final String PRN_OUTPUTTYPE = "OutputType";

    private static final String PRN_OUTPUTRES = "OutputReso";

    private static final String PRN_OUTPUTRATE = "OutputRate";

    private static final String PRN_SCANDIR = "ScanDir";

    private static final String PRN_CLPSEDIR = "CollapseDir";

    private static final String PRN_CHUNKSIZE = "ChunkSize";

    private static final String PRN_SPACEOVERLAP = "SpaceOverlap";

    private static final String PRN_TIMEOVERLAP = "TimeOverlap";

    private static final String PRN_TIMEJITTER = "TimeJitter";

    private static final String PRN_HIGHPASS = "HighPass";

    private static final int DIR_VERT = 0;

    private static final int DIR_HORIZ = 1;

    private static final String prText[] = { "", "" };

    private static final String prTextName[] = { PRN_INIMGFILE, PRN_OUTPUTFILE };

    private static final int prIntg[] = { 0, 0, 0, GAIN_UNITY, DIR_VERT, DIR_VERT };

    private static final String prIntgName[] = { PRN_OUTPUTTYPE, PRN_OUTPUTRES, PRN_OUTPUTRATE, PRN_GAINTYPE, PRN_SCANDIR, PRN_CLPSEDIR };

    private static final Param prPara[] = { null, null, null, null, null };

    private static final String prParaName[] = { PRN_GAIN, PRN_CHUNKSIZE, PRN_SPACEOVERLAP, PRN_TIMEOVERLAP, PRN_TIMEJITTER };

    private static final boolean prBool[] = { false };

    private static final String prBoolName[] = { PRN_HIGHPASS };

    private static final int GG_INIMGFILE = GG_OFF_PATHFIELD + PR_INIMGFILE;

    private static final int GG_OUTPUTFILE = GG_OFF_PATHFIELD + PR_OUTPUTFILE;

    private static final int GG_OUTPUTTYPE = GG_OFF_CHOICE + PR_OUTPUTTYPE;

    private static final int GG_OUTPUTRES = GG_OFF_CHOICE + PR_OUTPUTRES;

    private static final int GG_OUTPUTRATE = GG_OFF_CHOICE + PR_OUTPUTRATE;

    private static final int GG_GAINTYPE = GG_OFF_CHOICE + PR_GAINTYPE;

    private static final int GG_SCANDIR = GG_OFF_CHOICE + PR_SCANDIR;

    private static final int GG_CLPSEDIR = GG_OFF_CHOICE + PR_CLPSEDIR;

    private static final int GG_HIGHPASS = GG_OFF_CHECKBOX + PR_HIGHPASS;

    private static final int GG_GAIN = GG_OFF_PARAMFIELD + PR_GAIN;

    private static final int GG_CHUNKSIZE = GG_OFF_PARAMFIELD + PR_CHUNKSIZE;

    private static final int GG_SPACEOVERLAP = GG_OFF_PARAMFIELD + PR_SPACEOVERLAP;

    private static final int GG_TIMEOVERLAP = GG_OFF_PARAMFIELD + PR_TIMEOVERLAP;

    private static final int GG_TIMEJITTER = GG_OFF_PARAMFIELD + PR_TIMEJITTER;

    private static PropertyArray static_pr = null;

    private static Presets static_presets = null;

    /**
	 *	!! setVisible() bleibt dem Aufrufer ueberlassen
	 */
    public BeltramiDecompositionDlg() {
        super("Beltrami Decomposition");
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
            static_pr.para[PR_CHUNKSIZE] = new Param(128.0, Param.NONE);
            static_pr.para[PR_SPACEOVERLAP] = new Param(50.0, Param.FACTOR_TIME);
            static_pr.para[PR_TIMEOVERLAP] = new Param(12.5, Param.FACTOR_TIME);
            static_pr.para[PR_TIMEJITTER] = new Param(0.0, Param.FACTOR_TIME);
            static_pr.paraName = prParaName;
            fillDefaultAudioDescr(static_pr.intg, PR_OUTPUTTYPE, PR_OUTPUTRES, PR_OUTPUTRATE);
            fillDefaultGain(static_pr.para, PR_GAIN);
            static_presets = new Presets(getClass(), static_pr.toProperties(true));
        }
        presets = static_presets;
        pr = (PropertyArray) static_pr.clone();
        final GridBagConstraints con;
        final PathField ggInImgFile, ggOutputFile;
        final PathField[] ggInputs;
        final Component[] ggGain;
        final ParamField ggSpaceOverlap, ggTimeOverlap, ggChunkSize, ggTimeJitter;
        final ParamSpace spcChunk;
        final JComboBox ggScanDir, ggClpseDir;
        final JCheckBox ggHighPass;
        gui = new GUISupport();
        con = gui.getGridBagConstraints();
        con.insets = new Insets(1, 2, 1, 2);
        final ItemListener il = new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
            }
        };
        con.fill = GridBagConstraints.BOTH;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gui.addLabel(new GroupLabel("Waveform I/O", GroupLabel.ORIENT_HORIZONTAL, GroupLabel.BRACE_NONE));
        ggInImgFile = new PathField(PathField.TYPE_INPUTFILE, "Select input image file");
        con.gridwidth = 1;
        con.weightx = 0.1;
        gui.addLabel(new JLabel("Image input", SwingConstants.RIGHT));
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weightx = 0.9;
        gui.addPathField(ggInImgFile, GG_INIMGFILE, null);
        ggOutputFile = new PathField(PathField.TYPE_OUTPUTFILE + PathField.TYPE_FORMATFIELD + PathField.TYPE_RESFIELD + PathField.TYPE_RATEFIELD, "Select output file");
        ggOutputFile.handleTypes(GenericFile.TYPES_SOUND);
        ggInputs = new PathField[1];
        ggInputs[0] = ggInImgFile;
        ggOutputFile.deriveFrom(ggInputs, "$D0$F0Svd$E");
        con.gridwidth = 1;
        con.weightx = 0.1;
        gui.addLabel(new JLabel("Waveform Output", SwingConstants.RIGHT));
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weightx = 0.9;
        gui.addPathField(ggOutputFile, GG_OUTPUTFILE, null);
        gui.registerGadget(ggOutputFile.getTypeGadget(), GG_OUTPUTTYPE);
        gui.registerGadget(ggOutputFile.getResGadget(), GG_OUTPUTRES);
        gui.registerGadget(ggOutputFile.getRateGadget(), GG_OUTPUTRATE);
        ggGain = createGadgets(GGTYPE_GAIN);
        con.weightx = 0.1;
        con.gridwidth = 1;
        gui.addLabel(new JLabel("Gain", SwingConstants.RIGHT));
        con.weightx = 0.4;
        gui.addParamField((ParamField) ggGain[0], GG_GAIN, null);
        con.weightx = 0.5;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gui.addChoice((JComboBox) ggGain[1], GG_GAINTYPE, il);
        gui.addLabel(new GroupLabel("Settings", GroupLabel.ORIENT_HORIZONTAL, GroupLabel.BRACE_NONE));
        spcChunk = new ParamSpace(1, Integer.MAX_VALUE, 1, Param.NONE);
        ggScanDir = new JComboBox();
        ggScanDir.addItem("Vertical");
        ggScanDir.addItem("Horizontal");
        con.weightx = 0.1;
        con.gridwidth = 1;
        gui.addLabel(new JLabel("Scan Dir", SwingConstants.RIGHT));
        con.weightx = 0.2;
        gui.addChoice(ggScanDir, GG_SCANDIR, il);
        ggSpaceOverlap = new ParamField(Constants.spaces[Constants.ratioTimeSpace]);
        con.weightx = 0.1;
        gui.addLabel(new JLabel("Space Overlap", SwingConstants.RIGHT));
        con.weightx = 0.4;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gui.addParamField(ggSpaceOverlap, GG_SPACEOVERLAP, null);
        ggClpseDir = new JComboBox();
        ggClpseDir.addItem("Vertical");
        ggClpseDir.addItem("Horizontal");
        con.weightx = 0.1;
        con.gridwidth = 1;
        gui.addLabel(new JLabel("Collapse Dir", SwingConstants.RIGHT));
        con.weightx = 0.2;
        gui.addChoice(ggClpseDir, GG_CLPSEDIR, il);
        ggTimeOverlap = new ParamField(Constants.spaces[Constants.ratioTimeSpace]);
        con.weightx = 0.1;
        gui.addLabel(new JLabel("Time Overlap", SwingConstants.RIGHT));
        con.weightx = 0.4;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gui.addParamField(ggTimeOverlap, GG_TIMEOVERLAP, null);
        ggChunkSize = new ParamField(spcChunk);
        con.weightx = 0.1;
        con.gridwidth = 1;
        gui.addLabel(new JLabel("Chunk Size", SwingConstants.RIGHT));
        con.weightx = 0.4;
        gui.addParamField(ggChunkSize, GG_CHUNKSIZE, null);
        ggTimeJitter = new ParamField(Constants.spaces[Constants.ratioTimeSpace]);
        con.weightx = 0.1;
        gui.addLabel(new JLabel("Time Jitter", SwingConstants.RIGHT));
        con.weightx = 0.4;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gui.addParamField(ggTimeJitter, GG_TIMEJITTER, null);
        ggHighPass = new JCheckBox();
        con.weightx = 0.1;
        con.gridwidth = 1;
        gui.addLabel(new JLabel("High Pass ", SwingConstants.RIGHT));
        con.weightx = 0.4;
        gui.addCheckbox(ggHighPass, GG_HIGHPASS, null);
        initGUI(this, FLAGS_PRESETS | FLAGS_PROGBAR, gui);
    }

    protected void printVector(float[] v, String name) {
        final DecimalFormat fmt = getDecimalFormat();
        System.out.println("Vector '" + name + "':");
        System.out.print("   ");
        for (int j = 0; j < v.length; j++) {
            System.out.print("          [," + j + "]");
        }
        System.out.println();
        System.out.print(" [1]");
        for (int j = 0; j < v.length; j++) {
            System.out.print(" " + fmt.format(v[j]));
        }
        System.out.println("\n");
    }

    private static DecimalFormat getDecimalFormat() {
        final DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        final DecimalFormat fmt = new DecimalFormat(" 0.000000E00;-0.000000E00", dfs);
        return fmt;
    }

    protected void printMatrix(float[][] m, String name) {
        final DecimalFormat fmt = getDecimalFormat();
        System.out.println("Matrix '" + name + "':");
        for (int i = 0; i < m.length; i++) {
            if (i == 0) {
                System.out.print("     ");
                for (int j = 0; j < m[i].length; j++) {
                    System.out.print("          [," + j + "]");
                }
                System.out.println();
            }
            System.out.print(" [" + (i + 1) + ",]");
            for (int j = 0; j < m[i].length; j++) {
                System.out.print(" " + fmt.format(m[i][j]));
            }
            System.out.println();
        }
        System.out.println("\n");
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
        BufferedImage img = null;
        AudioFile outF = null;
        AudioFile tmpF = null;
        final AudioFileDescr outStream;
        final Param ampRef = new Param(1.0, Param.ABS_AMP);
        float gain = 1.0f;
        float maxAmp = 0.0f;
        final PathField ggOutput;
        final int scanDir = pr.intg[PR_SCANDIR];
        final int clpseDir = pr.intg[PR_CLPSEDIR];
        int chunkSize = (int) pr.para[PR_CHUNKSIZE].val;
        final double spaceOverlap = pr.para[PR_SPACEOVERLAP].val / 100;
        final double timeOverlap = pr.para[PR_TIMEOVERLAP].val / 100;
        final double timeJitter = pr.para[PR_TIMEJITTER].val / 100;
        final boolean highpass = pr.bool[PR_HIGHPASS];
        final int numChunks, winSize, winSizeH, overLen, timeStep;
        final int timeJitMin, timeJitMax, timeJitRange;
        final int width, height, ns, m, n, procNum;
        final int cellWidth, cellHeight, cellStepX, cellStepY;
        final float[] hsb = new float[3];
        final float[][] outBuf;
        final float[] s, win, overBuf;
        final float[][] mat, u;
        final Random rnd;
        float[] chunkBuf;
        int rgb, xOff, yOff, writeLen, realStep;
        float gain2;
        double d1, d2, dcMem0 = 0.0, dcMem1 = 0.0;
        topLevel: try {
            img = ImageIO.read(new File(pr.text[PR_INIMGFILE]));
            if (img == null) throw new IOException("No matching image decoder found");
            if (!threadRunning) break topLevel;
            ggOutput = (PathField) gui.getItemObj(GG_OUTPUTFILE);
            if (ggOutput == null) throw new IOException(ERR_MISSINGPROP);
            outStream = new AudioFileDescr();
            outStream.channels = 1;
            ggOutput.fillStream(outStream);
            outF = AudioFile.openAsWrite(outStream);
            if (!threadRunning) break topLevel;
            width = img.getWidth() - (highpass ? 1 : 0);
            height = img.getHeight() - (highpass ? 1 : 0);
            switch(scanDir) {
                case DIR_VERT:
                    cellWidth = width;
                    cellHeight = Math.min(height, chunkSize);
                    cellStepX = 0;
                    cellStepY = Math.max(1, (int) (cellHeight * (1.0 - spaceOverlap) + 0.5));
                    numChunks = (height - cellHeight) / cellStepY + 1;
                    break;
                case DIR_HORIZ:
                    cellWidth = Math.min(width, chunkSize);
                    cellHeight = height;
                    cellStepX = Math.max(1, (int) (cellWidth * (1.0 - spaceOverlap) + 0.5));
                    cellStepY = 0;
                    numChunks = (width - cellWidth) / cellStepX + 1;
                    break;
                default:
                    throw new IllegalArgumentException(String.valueOf(scanDir));
            }
            switch(clpseDir) {
                case DIR_VERT:
                    m = cellWidth;
                    n = cellHeight;
                    break;
                case DIR_HORIZ:
                    m = cellHeight;
                    n = cellWidth;
                    break;
                default:
                    throw new IllegalArgumentException(String.valueOf(clpseDir));
            }
            overLen = Math.min(m - 1, (int) (m * timeOverlap + 0.5));
            timeStep = m - overLen;
            winSizeH = Math.min(m >> 1, overLen) & ~1;
            winSize = winSizeH << 1;
            win = Filter.createFullWindow(winSize, Filter.WIN_HANNING);
            timeJitMin = -Math.min(timeStep - 1, (int) (timeStep * timeJitter + 0.5));
            timeJitMax = Math.min(overLen - 1, (int) (overLen * timeJitter + 0.5));
            timeJitRange = timeJitMax - timeJitMin + 1;
            rnd = new Random();
            overBuf = new float[m];
            if (pr.intg[PR_GAINTYPE] == GAIN_UNITY) {
                tmpF = createTempFile(outStream);
                procNum = numChunks + 1;
            } else {
                gain = (float) (Param.transform(pr.para[PR_GAIN], Param.ABS_AMP, ampRef, null)).val;
                procNum = numChunks;
            }
            if (!threadRunning) break topLevel;
            mat = new float[m][n];
            ns = Math.min(m + 1, n);
            s = new float[ns];
            u = new float[m][Math.min(m, n)];
            xOff = 0;
            yOff = 0;
            outBuf = new float[1][Math.max(8192, m)];
            chunkBuf = outBuf[0];
            for (int chunkIdx = 0; chunkIdx < numChunks; chunkIdx++) {
                switch(clpseDir) {
                    case DIR_VERT:
                        for (int row = 0, x = xOff; row < m; x++, row++) {
                            for (int col = 0, y = yOff; col < n; y++, col++) {
                                rgb = img.getRGB(x, y);
                                Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
                                mat[row][col] = hsb[2];
                                if (highpass) {
                                    rgb = img.getRGB(x + 1, y);
                                    Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
                                    mat[row][col] -= hsb[2];
                                    rgb = img.getRGB(x, y + 1);
                                    Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
                                    mat[row][col] -= hsb[2];
                                    rgb = img.getRGB(x + 1, y + 1);
                                    Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
                                    mat[row][col] += hsb[2];
                                }
                            }
                        }
                        break;
                    case DIR_HORIZ:
                        for (int col = 0, x = xOff; col < n; x++, col++) {
                            for (int row = 0, y = yOff; row < m; y++, row++) {
                                rgb = img.getRGB(x, y);
                                Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
                                mat[row][col] = hsb[2];
                                if (highpass) {
                                    rgb = img.getRGB(x + 1, y);
                                    Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
                                    mat[row][col] -= hsb[2];
                                    rgb = img.getRGB(x, y + 1);
                                    Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
                                    mat[row][col] -= hsb[2];
                                    rgb = img.getRGB(x + 1, y + 1);
                                    Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
                                    mat[row][col] += hsb[2];
                                }
                            }
                        }
                        break;
                    default:
                        assert false : clpseDir;
                }
                Util.clear(s);
                for (int i = 0; i < u.length; i++) Util.clear(u[i]);
                svd(mat, s, u, null, (float) (chunkIdx + 1) / procNum, false);
                if (!threadRunning) break topLevel;
                gain2 = gain * s[0];
                dcMem0 = u[0][0] * gain2;
                for (int i = 0; i < m; i++) {
                    d1 = u[i][0] * gain2;
                    d2 = d1 - dcMem0 + 0.99 * dcMem1;
                    chunkBuf[i] = (float) d2;
                    dcMem0 = d1;
                    dcMem1 = d2;
                }
                Util.mult(win, 0, chunkBuf, 0, winSizeH);
                Util.mult(win, winSizeH, chunkBuf, m - winSizeH, winSizeH);
                Util.add(overBuf, 0, chunkBuf, 0, m);
                if (timeJitter > 0) {
                    realStep = timeStep + rnd.nextInt(timeJitRange) + timeJitMin;
                } else {
                    realStep = timeStep;
                }
                System.arraycopy(chunkBuf, realStep, overBuf, 0, m - realStep);
                Util.clear(overBuf, m - realStep, realStep);
                writeLen = (chunkIdx < numChunks - 1) ? realStep : m;
                if (tmpF == null) {
                    outF.writeFrames(outBuf, 0, writeLen);
                } else {
                    tmpF.writeFrames(outBuf, 0, writeLen);
                }
                maxAmp = Math.max(maxAmp, Util.maxAbs(chunkBuf, 0, m));
                xOff += cellStepX;
                yOff += cellStepY;
            }
            if (!threadRunning) break topLevel;
            if (pr.intg[PR_GAINTYPE] == GAIN_UNITY) {
                gain = (float) (Param.transform(pr.para[PR_GAIN], Param.ABS_AMP, new Param(1.0 / maxAmp, Param.ABS_AMP), null)).val;
                normalizeAudioFile(tmpF, outF, outBuf, gain, 1.0f);
                maxAmp *= gain;
            }
            if (!threadRunning) break topLevel;
            outF.close();
            outF = null;
            handleClipping(maxAmp);
        } catch (IOException e1) {
            setError(e1);
        } catch (OutOfMemoryError e2) {
            setError(new Exception(ERR_MEMORY));
        }
        if (outF != null) outF.cleanUp();
    }

    private void svd(float[][] mat, float[] s, float[][] u, float[][] v, float progStop, boolean noProg) {
        final int m = mat.length;
        final int n = mat[0].length;
        final int nu = Math.min(m, n);
        final int ns = Math.min(m + 1, n);
        final boolean wantu = u != null;
        final boolean wantv = v != null;
        if ((s.length != ns) || (wantu && ((u.length != m) || (u[0].length != nu))) || (wantv && ((v.length != n) || (v[0].length != n)))) throw new IllegalArgumentException();
        final float[] e = new float[n];
        final float[] work = new float[m];
        final float eps = (float) Math.pow(2.0, -48.0);
        final float tiny = (float) Math.pow(2.0, -120.0);
        final int nct = Math.min(m - 1, n);
        final int nrt = Math.max(0, Math.min(n - 2, m));
        final int nk = Math.max(nct, nrt);
        final int np = Math.min(n, m + 1);
        final float progOff = getProgression();
        final float progW = (progStop - progOff) / (((long) nk * (nk + 1) / 2) + ((long) np * (np + 1) / 2) + (wantu ? ((long) nct * (nct + 1) / 2) : 0) + (wantv ? n : 0));
        long progC = 0;
        for (int k = 0; (k < nk) && (noProg || threadRunning); k++) {
            if (k < nct) {
                s[k] = 0;
                for (int i = k; i < m; i++) {
                    s[k] = hypot(s[k], mat[i][k]);
                }
                if (s[k] != 0.0f) {
                    if (mat[k][k] < 0.0f) {
                        s[k] = -s[k];
                    }
                    for (int i = k; i < m; i++) {
                        mat[i][k] /= s[k];
                    }
                    mat[k][k] += 1.0f;
                }
                s[k] = -s[k];
            }
            for (int j = k + 1; j < n; j++) {
                if ((k < nct) && (s[k] != 0.0f)) {
                    float t = 0;
                    for (int i = k; i < m; i++) {
                        t += mat[i][k] * mat[i][j];
                    }
                    t = -t / mat[k][k];
                    for (int i = k; i < m; i++) {
                        mat[i][j] += t * mat[i][k];
                    }
                }
                e[j] = mat[k][j];
            }
            if (wantu && (k < nct)) {
                for (int i = k; i < m; i++) {
                    u[i][k] = mat[i][k];
                }
            }
            if (k < nrt) {
                e[k] = 0;
                for (int i = k + 1; i < n; i++) {
                    e[k] = hypot(e[k], e[i]);
                }
                if (e[k] != 0.0f) {
                    if (e[k + 1] < 0.0f) {
                        e[k] = -e[k];
                    }
                    for (int i = k + 1; i < n; i++) {
                        e[i] /= e[k];
                    }
                    e[k + 1] += 1.0f;
                }
                e[k] = -e[k];
                if (((k + 1) < m) && (e[k] != 0.0f)) {
                    for (int i = k + 1; i < m; i++) {
                        work[i] = 0.0f;
                    }
                    for (int j = k + 1; j < n; j++) {
                        for (int i = k + 1; i < m; i++) {
                            work[i] += e[j] * mat[i][j];
                        }
                    }
                    for (int j = k + 1; j < n; j++) {
                        final float t = -e[j] / e[k + 1];
                        for (int i = k + 1; i < m; i++) {
                            mat[i][j] += t * work[i];
                        }
                    }
                }
                if (wantv) {
                    for (int i = k + 1; i < n; i++) {
                        v[i][k] = e[i];
                    }
                }
            }
            progC += (nk - k);
            setProgression(progC * progW + progOff);
        }
        if (!(noProg || threadRunning)) return;
        int p = np;
        if (nct < n) {
            s[nct] = mat[nct][nct];
        }
        if (m < p) {
            s[p - 1] = 0.0f;
        }
        if ((nrt + 1) < p) {
            e[nrt] = mat[nrt][p - 1];
        }
        e[p - 1] = 0.0f;
        if (wantu) {
            for (int j = nct; j < nu; j++) {
                for (int i = 0; i < m; i++) {
                    u[i][j] = 0.0f;
                }
                u[j][j] = 1.0f;
            }
            for (int k = nct - 1; k >= 0; k--) {
                if (s[k] != 0.0f) {
                    for (int j = k + 1; j < nu; j++) {
                        float t = 0;
                        for (int i = k; i < m; i++) {
                            t += u[i][k] * u[i][j];
                        }
                        t = -t / u[k][k];
                        for (int i = k; i < m; i++) {
                            u[i][j] += t * u[i][k];
                        }
                    }
                    for (int i = k; i < m; i++) {
                        u[i][k] = -u[i][k];
                    }
                    u[k][k] = 1.0f + u[k][k];
                    for (int i = 0; i < k - 1; i++) {
                        u[i][k] = 0.0f;
                    }
                } else {
                    for (int i = 0; i < m; i++) {
                        u[i][k] = 0.0f;
                    }
                    u[k][k] = 1.0f;
                }
                progC += (nct - k);
                setProgression(progC * progW + progOff);
            }
        }
        if (wantv) {
            for (int k = n - 1; k >= 0; k--) {
                if ((k < nrt) && (e[k] != 0.0f)) {
                    for (int j = k + 1; j < nu; j++) {
                        float t = 0;
                        for (int i = k + 1; i < n; i++) {
                            t += v[i][k] * v[i][j];
                        }
                        t = -t / v[k + 1][k];
                        for (int i = k + 1; i < n; i++) {
                            v[i][j] += t * v[i][k];
                        }
                    }
                }
                for (int i = 0; i < n; i++) {
                    v[i][k] = 0.0f;
                }
                v[k][k] = 1.0f;
                progC++;
                setProgression(progC * progW + progOff);
            }
        }
        final int pp = p - 1;
        int iter = 0;
        while ((p > 0) && (noProg || threadRunning)) {
            int k, kase;
            for (k = p - 2; k >= -1; k--) {
                if (k == -1) break;
                if (Math.abs(e[k]) <= tiny + eps * (Math.abs(s[k]) + Math.abs(s[k + 1]))) {
                    e[k] = 0.0f;
                    break;
                }
            }
            if (k == p - 2) {
                kase = 4;
            } else {
                int ks;
                for (ks = p - 1; ks >= k; ks--) {
                    if (ks == k) break;
                    final float t = (ks != p ? Math.abs(e[ks]) : 0.0f) + (ks != k + 1 ? Math.abs(e[ks - 1]) : 0.0f);
                    if (Math.abs(s[ks]) <= tiny + eps * t) {
                        s[ks] = 0.0f;
                        break;
                    }
                }
                if (ks == k) {
                    kase = 3;
                } else if (ks == p - 1) {
                    kase = 1;
                } else {
                    kase = 2;
                    k = ks;
                }
            }
            k++;
            switch(kase) {
                case 1:
                    {
                        float f = e[p - 2];
                        e[p - 2] = 0.0f;
                        for (int j = p - 2; j >= k; j--) {
                            final float t = hypot(s[j], f);
                            final float cs = s[j] / t;
                            final float sn = f / t;
                            s[j] = t;
                            if (j != k) {
                                f = -sn * e[j - 1];
                                e[j - 1] = cs * e[j - 1];
                            }
                            if (wantv) {
                                for (int i = 0; i < n; i++) {
                                    final float tt = cs * v[i][j] + sn * v[i][p - 1];
                                    v[i][p - 1] = -sn * v[i][j] + cs * v[i][p - 1];
                                    v[i][j] = tt;
                                }
                            }
                        }
                    }
                    break;
                case 2:
                    {
                        float f = e[k - 1];
                        e[k - 1] = 0.0f;
                        for (int j = k; j < p; j++) {
                            final float t = hypot(s[j], f);
                            final float cs = s[j] / t;
                            final float sn = f / t;
                            s[j] = t;
                            f = -sn * e[j];
                            e[j] = cs * e[j];
                            if (wantu) {
                                for (int i = 0; i < m; i++) {
                                    final float tt = cs * u[i][j] + sn * u[i][k - 1];
                                    u[i][k - 1] = -sn * u[i][j] + cs * u[i][k - 1];
                                    u[i][j] = tt;
                                }
                            }
                        }
                    }
                    break;
                case 3:
                    {
                        final float scale = Math.max(Math.max(Math.max(Math.max(Math.abs(s[p - 1]), Math.abs(s[p - 2])), Math.abs(e[p - 2])), Math.abs(s[k])), Math.abs(e[k]));
                        final float sp = s[p - 1] / scale;
                        final float spm1 = s[p - 2] / scale;
                        final float epm1 = e[p - 2] / scale;
                        final float sk = s[k] / scale;
                        final float ek = e[k] / scale;
                        final float b = ((spm1 + sp) * (spm1 - sp) + epm1 * epm1) / 2.0f;
                        final float c = (sp * epm1) * (sp * epm1);
                        final float shift;
                        if ((b != 0.0f) || (c != 0.0f)) {
                            final float t;
                            if (b >= 0.0f) {
                                t = (float) Math.sqrt(b * b + c);
                            } else {
                                t = (float) -Math.sqrt(b * b + c);
                            }
                            shift = c / (b + t);
                        } else {
                            shift = 0.0f;
                        }
                        float f = (sk + sp) * (sk - sp) + shift;
                        float g = sk * ek;
                        for (int j = k; j < (p - 1); j++) {
                            float t = hypot(f, g);
                            float cs = f / t;
                            float sn = g / t;
                            if (j != k) {
                                e[j - 1] = t;
                            }
                            f = cs * s[j] + sn * e[j];
                            e[j] = cs * e[j] - sn * s[j];
                            g = sn * s[j + 1];
                            s[j + 1] = cs * s[j + 1];
                            if (wantv) {
                                for (int i = 0; i < n; i++) {
                                    final float tt = cs * v[i][j] + sn * v[i][j + 1];
                                    v[i][j + 1] = -sn * v[i][j] + cs * v[i][j + 1];
                                    v[i][j] = tt;
                                }
                            }
                            t = hypot(f, g);
                            cs = f / t;
                            sn = g / t;
                            s[j] = t;
                            f = cs * e[j] + sn * s[j + 1];
                            s[j + 1] = -sn * e[j] + cs * s[j + 1];
                            g = sn * e[j + 1];
                            e[j + 1] = cs * e[j + 1];
                            if (wantu && (j < (m - 1))) {
                                for (int i = 0; i < m; i++) {
                                    final float tt = cs * u[i][j] + sn * u[i][j + 1];
                                    u[i][j + 1] = -sn * u[i][j] + cs * u[i][j + 1];
                                    u[i][j] = tt;
                                }
                            }
                        }
                        e[p - 2] = f;
                        iter++;
                    }
                    break;
                case 4:
                    {
                        if (s[k] <= 0.0f) {
                            s[k] = (s[k] < 0.0f ? -s[k] : 0.0f);
                            if (wantv) {
                                for (int i = 0; i <= pp; i++) {
                                    v[i][k] = -v[i][k];
                                }
                            }
                        }
                        while (k < pp) {
                            if (s[k] >= s[k + 1]) break;
                            float t = s[k];
                            s[k] = s[k + 1];
                            s[k + 1] = t;
                            if (wantv && (k < (n - 1))) {
                                for (int i = 0; i < n; i++) {
                                    t = v[i][k + 1];
                                    v[i][k + 1] = v[i][k];
                                    v[i][k] = t;
                                }
                            }
                            if (wantu && (k < (m - 1))) {
                                for (int i = 0; i < m; i++) {
                                    t = u[i][k + 1];
                                    u[i][k + 1] = u[i][k];
                                    u[i][k] = t;
                                }
                            }
                            k++;
                        }
                        iter = 0;
                        progC += p;
                        p--;
                        setProgression(progC * progW + progOff);
                    }
                    break;
            }
        }
    }

    private static float hypot(float a, float b) {
        if (Math.abs(a) > Math.abs(b)) {
            final float div = b / a;
            return ((float) (Math.abs(a) * Math.sqrt(1 + div * div)));
        } else if (b != 0) {
            final float div = a / b;
            return ((float) (Math.abs(b) * Math.sqrt(1 + div * div)));
        } else {
            return 0.0f;
        }
    }
}
