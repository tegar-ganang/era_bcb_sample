package gov.sns.apps.injectionpainting;

import gov.sns.ca.*;
import gov.sns.application.*;
import gov.sns.tools.bricks.WindowReference;
import gov.sns.tools.messaging.MessageCenter;
import java.net.URL;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.Timer;
import java.util.*;
import java.util.Date;
import java.sql.Connection;
import java.awt.Color;
import java.net.*;
import java.io.*;
import gov.sns.tools.plot.*;
import gov.sns.tools.apputils.files.RecentFileTracker;

/** Controller for the client that monitors the trip monitor services */
public class WaveformFace {

    /** reference to the main window */
    protected final WindowReference windowReference;

    /** Kicker ramp time in us */
    private double ramptime = 2000.0;

    /** Pre-injection flat top time */
    private double flattime = 1000.0;

    /** Duration of the kicker paiting function */
    private double painttime = 1000.0;

    /** Time duration for the linear fall off of the kickers to zero */
    private double fallofftime = 500.0;

    /** Any extra time needed to fill up the full 5000us waveform */
    private double leftovertime = 0.0;

    /** Spacing between points */
    private double deltat = 0.508626;

    /** Digitized signal spacing */
    private double dsignal = 0.049;

    /** Size of waveform array */
    private int wavesize = 16384;

    /** Maximum allowed nonzero waveform time **/
    private int maxtime = 5000;

    /** Safety time buffer between max allowed time and endpaint **/
    private int maxbuffertime = 20;

    /** Maximum number of characters allowed in user root name **/
    private int maxcharlength = 17;

    private JFileChooser fc;

    private JFileChooser fcload;

    protected double[] masterhwave = new double[wavesize];

    protected double[] mastervwave = new double[wavesize];

    JTable hTable;

    JTable vTable;

    InputTableModel hinputtablemodel;

    InputTableModel vinputtablemodel;

    FunctionGraphsJPanel hplot;

    FunctionGraphsJPanel vplot;

    JButton hplotButton;

    JButton vplotButton;

    JScrollPane hPane;

    JScrollPane vPane;

    JLabel hwavelabel;

    JLabel vwavelabel;

    JComboBox hwaveformBox;

    JComboBox vwaveformBox;

    String hwaveformtype = new String("Root t");

    String vwaveformtype = new String("Root t");

    JButton hwriteButton;

    JButton vwriteButton;

    JButton loadFileButton;

    JLabel fileLabel;

    JTextField hrootname;

    JTextField vrootname;

    String hchildname = new String("");

    String vchildname = new String("");

    String hcurrentfilename = new String("");

    String vcurrentfilename = new String("");

    RecentFileTracker ft;

    RecentFileTracker ftload;

    JSplitPane masterPane;

    /** Constructor */
    public WaveformFace(final WindowReference mainWindowReference) {
        windowReference = mainWindowReference;
        initializeViews();
        makeTables();
        setAction();
    }

    /** initialize views */
    protected void initializeViews() {
        masterPane = (JSplitPane) windowReference.getView("masterPane");
        hplot = (FunctionGraphsJPanel) windowReference.getView("HWaveformPlot");
        vplot = (FunctionGraphsJPanel) windowReference.getView("VWaveformPlot");
        hplot.setAxisNames(" t (us)", "Signal (%)");
        vplot.setAxisNames(" t (us)", "Signal (%)");
        hTable = (JTable) windowReference.getView("H Table");
        vTable = (JTable) windowReference.getView("V Table");
        hplotButton = (JButton) windowReference.getView("HPlotButton");
        hplotButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                hplot.removeAllGraphData();
                makeHWave();
            }
        });
        vplotButton = (JButton) windowReference.getView("VPlotButton");
        vplotButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                vplot.removeAllGraphData();
                makeVWave();
            }
        });
        hwavelabel = (JLabel) windowReference.getView("HWaveLabel");
        hwaveformBox = (JComboBox) windowReference.getView("H Combo Box");
        hwaveformBox.addItem(new String("root t"));
        hwaveformBox.addItem(new String("flattop"));
        hwaveformBox.addItem(new String("linear"));
        vwavelabel = (JLabel) windowReference.getView("VWaveLabel");
        vwaveformBox = (JComboBox) windowReference.getView("V Combo Box");
        vwaveformBox.addItem(new String("root t"));
        vwaveformBox.addItem(new String("flattop"));
        vwaveformBox.addItem(new String("linear"));
        hPane = (JScrollPane) windowReference.getView("H Scroll Pane");
        vPane = (JScrollPane) windowReference.getView("V Scroll Pane");
        hwriteButton = (JButton) windowReference.getView("HWriteButton");
        vwriteButton = (JButton) windowReference.getView("VWriteButton");
        vrootname = (JTextField) windowReference.getView("vrootname");
        hrootname = (JTextField) windowReference.getView("hrootname");
        loadFileButton = (JButton) windowReference.getView("loadFileButton");
        fileLabel = (JLabel) windowReference.getView("fileLabel");
        fc = new JFileChooser();
        fc.setFileSelectionMode(fc.DIRECTORIES_ONLY);
        fc.setDialogTitle("Select Location to Save File");
        fcload = new JFileChooser();
        ft = new RecentFileTracker(1, this.getClass(), "wsfile");
        ft.applyRecentFolder(fc);
        ftload = new RecentFileTracker(1, this.getClass(), "wsfile");
        ft.applyRecentFolder(fcload);
    }

    public void setAction() {
        hwaveformBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (hwaveformBox.getSelectedIndex() == 0) {
                    hwaveformtype = "Root t";
                }
                if (hwaveformBox.getSelectedIndex() == 1) {
                    hwaveformtype = "Flat t";
                }
                if (hwaveformBox.getSelectedIndex() == 2) {
                    hwaveformtype = "Linear t";
                }
            }
        });
        vwaveformBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (vwaveformBox.getSelectedIndex() == 0) {
                    vwaveformtype = "Root t";
                }
                if (vwaveformBox.getSelectedIndex() == 1) {
                    vwaveformtype = "Flat t";
                }
                if (vwaveformBox.getSelectedIndex() == 2) {
                    vwaveformtype = "Linear t";
                }
            }
        });
        hwriteButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                generateHBinary(masterhwave);
            }
        });
        vwriteButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                generateVBinary(mastervwave);
            }
        });
        loadFileButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int returnValue = fcload.showOpenDialog(windowReference.getWindow());
                if (returnValue == fcload.APPROVE_OPTION) {
                    File file = fcload.getSelectedFile();
                    ftload.cacheURL(file);
                    String name = file.getName();
                    String[] tokens;
                    tokens = name.split("_");
                    System.out.println("tokens are " + tokens[0] + "\t" + tokens[1]);
                    if (tokens[1].contains("H")) {
                        hcurrentfilename = tokens[0];
                        submitHWaveforms();
                    }
                    if (tokens[1].contains("V")) {
                        vcurrentfilename = tokens[0];
                        submitVWaveforms();
                    }
                } else {
                    System.out.println("Open command canceled by user.");
                }
            }
        });
    }

    /** make the H plot */
    protected void makeHPlot(double[] sdata, double[] data) {
        final BasicGraphData graphData = new BasicGraphData();
        graphData.setGraphColor(Color.BLUE);
        graphData.addPoint(sdata, data);
        hplot.addGraphData(graphData);
        double painttime = Double.valueOf(((String) hTable.getValueAt(2, 1)).trim()).doubleValue();
        double startamp = Double.valueOf(((String) hTable.getValueAt(0, 1)).trim()).doubleValue();
        double endamp = Double.valueOf(((String) hTable.getValueAt(1, 1)).trim()).doubleValue();
        Integer paintint = new Integer((new Double(painttime)).intValue());
        Integer startampint = new Integer((new Double(startamp)).intValue());
        Integer endampint = new Integer((new Double(endamp)).intValue());
        String paintstring = paintint.toString();
        String startampstring = startampint.toString();
        String endampstring = endampint.toString();
        int paintform = hwaveformBox.getSelectedIndex();
        String paintformstring = new String("");
        if (paintform == 0) paintformstring = "root";
        if (paintform == 1) paintformstring = "flat";
        if (paintform == 2) paintformstring = "lin";
        hchildname = new String(paintformstring + paintstring + "us-" + startampstring + "t" + endampstring);
        System.out.println("hchildname is " + hchildname);
        hrootname.setText(hchildname);
    }

    /** make the V plot */
    protected void makeVPlot(double[] sdata, double[] data) {
        final BasicGraphData graphData = new BasicGraphData();
        graphData.setGraphColor(Color.BLUE);
        graphData.addPoint(sdata, data);
        vplot.addGraphData(graphData);
        double painttime = Double.valueOf(((String) hTable.getValueAt(2, 1)).trim()).doubleValue();
        double startamp = Double.valueOf(((String) hTable.getValueAt(0, 1)).trim()).doubleValue();
        double endamp = Double.valueOf(((String) hTable.getValueAt(1, 1)).trim()).doubleValue();
        Integer paintint = new Integer((new Double(painttime)).intValue());
        Integer startampint = new Integer((new Double(startamp)).intValue());
        Integer endampint = new Integer((new Double(endamp)).intValue());
        String paintstring = paintint.toString();
        String startampstring = startampint.toString();
        String endampstring = endampint.toString();
        int paintform = hwaveformBox.getSelectedIndex();
        String paintformstring = new String("");
        if (paintform == 0) paintformstring = "root";
        if (paintform == 1) paintformstring = "flat";
        if (paintform == 2) paintformstring = "lin";
        vchildname = new String(paintformstring + paintstring + "us-" + startampstring + "t" + endampstring);
        System.out.println("vchildname is " + vchildname);
        vrootname.setText(vchildname);
    }

    /** make the Table */
    protected void makeTables() {
        String[] colnames = { "Parameter", "Value" };
        int nrows = 2;
        hinputtablemodel = new InputTableModel(colnames, 4);
        hTable.setModel(hinputtablemodel);
        hTable.getColumnModel().getColumn(0).setMinWidth(150);
        hTable.getColumnModel().getColumn(1).setMinWidth(100);
        hinputtablemodel.setValueAt(new String("Start Paint Amp (%)"), 0, 0);
        hinputtablemodel.setValueAt(new String("End Paint Amp (%)"), 1, 0);
        hinputtablemodel.setValueAt(new String("Paint Time (us)"), 2, 0);
        hinputtablemodel.setValueAt(new String("Fall-Off Time (us)"), 3, 0);
        hinputtablemodel.setValueAt(new String("100.0"), 0, 1);
        hinputtablemodel.setValueAt(new String("50.0"), 1, 1);
        hinputtablemodel.setValueAt(new String("1000.0"), 2, 1);
        hinputtablemodel.setValueAt(new String("500.0"), 3, 1);
        hinputtablemodel.fireTableDataChanged();
        vinputtablemodel = new InputTableModel(colnames, 4);
        vTable.setModel(vinputtablemodel);
        vTable.getColumnModel().getColumn(0).setMinWidth(150);
        vTable.getColumnModel().getColumn(1).setMinWidth(100);
        vinputtablemodel.setValueAt(new String("Start Paint Amp (%)"), 0, 0);
        vinputtablemodel.setValueAt(new String("End Paint Amp (%)"), 1, 0);
        vinputtablemodel.setValueAt(new String("Paint Time (us)"), 2, 0);
        vinputtablemodel.setValueAt(new String("Fall-Off Time (us)"), 3, 0);
        vinputtablemodel.setValueAt(new String("100.0"), 0, 1);
        vinputtablemodel.setValueAt(new String("50.0"), 1, 1);
        vinputtablemodel.setValueAt(new String("1000.0"), 2, 1);
        vinputtablemodel.setValueAt(new String("500.0"), 3, 1);
        vinputtablemodel.fireTableDataChanged();
    }

    /** make the H Waveform */
    protected void makeHWave() {
        double[] swave = new double[wavesize];
        double[] wave = new double[wavesize];
        double ttime = 0.0;
        double startamp = Double.valueOf(((String) hTable.getValueAt(0, 1)).trim()).doubleValue();
        double endamp = Double.valueOf(((String) hTable.getValueAt(1, 1)).trim()).doubleValue();
        double amp = startamp - endamp;
        double painttime = Double.valueOf(((String) hTable.getValueAt(2, 1)).trim()).doubleValue();
        double falltime = Double.valueOf(((String) hTable.getValueAt(3, 1)).trim()).doubleValue();
        double transitiontime = 0.0;
        boolean slewflag = false;
        if (startamp < endamp) {
            boolean transitionneeded = true;
            transitiontime = 50.0 * deltat;
            falltime -= transitiontime;
        }
        double toffset1 = ramptime + flattime;
        double toffset2 = ramptime + flattime + painttime;
        double toffset3 = ramptime + flattime + painttime + transitiontime;
        double toffset4 = ramptime + flattime + painttime + transitiontime + falltime;
        double riseslope = startamp / ramptime;
        double fallslope = -endamp / falltime;
        double totaltime = ramptime + flattime + painttime + transitiontime + falltime;
        fileLabel.setText("");
        if (totaltime > (maxtime - maxbuffertime)) {
            fileLabel.setText("I'm sorry, I can not create this waveform because it is too long. Please reduce fall-off time.");
        } else {
            for (int i = 0; i < wavesize; i++) {
                swave[i] = deltat * i;
                if (swave[i] < ramptime) {
                    wave[i] = riseslope * swave[i];
                } else if (swave[i] > ramptime && swave[i] < (toffset1)) {
                    wave[i] = startamp;
                } else if (swave[i] > toffset1 && swave[i] < toffset2) {
                    if (hwaveformtype.equals("Flat t")) {
                        wave[i] = startamp;
                    }
                    if (hwaveformtype.equals("Root t")) {
                        wave[i] = amp * (1 - Math.sqrt((swave[i] - toffset1) / painttime)) + endamp;
                    }
                    if (hwaveformtype.equals("Linear t")) {
                        wave[i] = amp * (1 - (swave[i] - toffset1) / painttime) + endamp;
                    }
                } else if (swave[i] > toffset2 && swave[i] < toffset3) {
                    wave[i] = wave[i - 1];
                } else if (swave[i] > toffset3 && swave[i] <= toffset4) {
                    if (hwaveformtype.equals("Flat t")) {
                        wave[i] = (-startamp / falltime) * (swave[i] - toffset3) + startamp;
                    }
                    wave[i] = fallslope * (swave[i] - toffset3) + endamp;
                } else {
                    wave[i] = 0.0;
                }
                if (!slewflag && i > 0) {
                    if (Math.abs(wave[i] - wave[i - 1]) > 0.203) {
                        slewflag = true;
                    }
                }
            }
            if (slewflag) {
            } else {
                fileLabel.setText("");
            }
            masterhwave = wave;
            makeHPlot(swave, wave);
        }
    }

    /** make the H Waveform */
    protected void makeVWave() {
        double[] swave = new double[wavesize];
        double[] wave = new double[wavesize];
        double ttime = 0.0;
        double startamp = Double.valueOf(((String) vTable.getValueAt(0, 1)).trim()).doubleValue();
        double endamp = Double.valueOf(((String) vTable.getValueAt(1, 1)).trim()).doubleValue();
        double amp = startamp - endamp;
        double painttime = Double.valueOf(((String) vTable.getValueAt(2, 1)).trim()).doubleValue();
        double falltime = Double.valueOf(((String) vTable.getValueAt(3, 1)).trim()).doubleValue();
        double transitiontime = 0.0;
        boolean slewflag = false;
        if (startamp < endamp) {
            boolean transitionneeded = true;
            transitiontime = 50.0 * deltat;
            falltime -= transitiontime;
            System.out.println("A transition time is necessary before fall-off.");
        }
        double toffset1 = ramptime + flattime;
        double toffset2 = ramptime + flattime + painttime;
        double toffset3 = ramptime + flattime + painttime + transitiontime;
        double toffset4 = ramptime + flattime + painttime + transitiontime + falltime;
        double riseslope = startamp / ramptime;
        double fallslope = -endamp / falltime;
        double totaltime = ramptime + flattime + painttime + transitiontime + falltime;
        fileLabel.setText("");
        if (totaltime > (maxtime - maxbuffertime)) {
            fileLabel.setText("I'm sorry, I can not create this waveform because it is too long. Please reduce fall-off time.");
        } else {
            for (int i = 0; i < wavesize; i++) {
                swave[i] = deltat * i;
                if (swave[i] < ramptime) {
                    wave[i] = riseslope * swave[i];
                } else if (swave[i] > ramptime && swave[i] < (toffset1)) {
                    wave[i] = startamp;
                } else if (swave[i] > toffset1 && swave[i] < toffset2) {
                    if (vwaveformtype.equals("Flat t")) {
                        wave[i] = startamp;
                    }
                    if (vwaveformtype.equals("Root t")) {
                        wave[i] = amp * (1 - Math.sqrt((swave[i] - toffset1) / painttime)) + endamp;
                    }
                    if (vwaveformtype.equals("Linear t")) {
                        wave[i] = amp * (1 - (swave[i] - toffset1) / painttime) + endamp;
                    }
                } else if (swave[i] > toffset2 && swave[i] < toffset3) {
                    wave[i] = wave[i - 1];
                } else if (swave[i] > toffset3 && swave[i] < toffset4) {
                    wave[i] = fallslope * (swave[i] - toffset3) + endamp;
                } else {
                    wave[i] = 0.0;
                }
                if (!slewflag && i > 0) {
                    if (Math.abs(wave[i] - wave[i - 1]) > 0.203) {
                        slewflag = true;
                    }
                }
            }
            if (slewflag) {
            } else {
                fileLabel.setText("");
            }
            mastervwave = wave;
            makeVPlot(swave, wave);
        }
    }

    protected void generateHBinary(double[] wave) {
        int length = wave.length;
        char[] h1wavechars = new char[length];
        char[] h2wavechars = new char[length];
        char[] h3wavechars = new char[length];
        char[] h4wavechars = new char[length];
        double scaledwave;
        char swappedchar;
        double h1ratio = 1.0;
        double h2ratio = 0.588;
        double h3ratio = 0.588;
        double h4ratio = 1.0;
        for (int i = 0; i < length; i++) {
            scaledwave = (h1ratio * wave[i] / 100.0 + 1) / 2.0 * 4095.0;
            swappedchar = (char) scaledwave;
            h1wavechars[i] = (char) ((char) (swappedchar >> 8) + (char) (swappedchar << 8));
            scaledwave = (h2ratio * wave[i] / 100.0 + 1) / 2.0 * 4095.0;
            swappedchar = (char) scaledwave;
            h2wavechars[i] = (char) ((char) (swappedchar >> 8) + (char) (swappedchar << 8));
            scaledwave = (h3ratio * wave[i] / 100.0 + 1) / 2.0 * 4095.0;
            swappedchar = (char) scaledwave;
            h3wavechars[i] = (char) ((char) (swappedchar >> 8) + (char) (swappedchar << 8));
            scaledwave = (h4ratio * wave[i] / 100.0 + 1) / 2.0 * 4095.0;
            swappedchar = (char) scaledwave;
            h4wavechars[i] = (char) ((char) (swappedchar >> 8) + (char) (swappedchar << 8));
        }
        writeHFiles(h1wavechars, h2wavechars, h3wavechars, h4wavechars);
    }

    protected void generateVBinary(double[] wave) {
        int length = wave.length;
        char[] v1wavechars = new char[length];
        char[] v2wavechars = new char[length];
        char[] v3wavechars = new char[length];
        char[] v4wavechars = new char[length];
        double scaledwave;
        char swappedchar;
        double v1ratio = 0.744;
        double v2ratio = 1.0;
        double v3ratio = 1.0;
        double v4ratio = 0.744;
        for (int i = 0; i < length; i++) {
            scaledwave = (v1ratio * wave[i] / 100.0 + 1) / 2.0 * 4095.0;
            swappedchar = (char) scaledwave;
            v1wavechars[i] = (char) ((char) (swappedchar >> 8) + (char) (swappedchar << 8));
            scaledwave = (v2ratio * wave[i] / 100.0 + 1) / 2.0 * 4095.0;
            swappedchar = (char) scaledwave;
            v2wavechars[i] = (char) ((char) (swappedchar >> 8) + (char) (swappedchar << 8));
            scaledwave = (v3ratio * wave[i] / 100.0 + 1) / 2.0 * 4095.0;
            swappedchar = (char) scaledwave;
            v3wavechars[i] = (char) ((char) (swappedchar >> 8) + (char) (swappedchar << 8));
            scaledwave = (v4ratio * wave[i] / 100.0 + 1) / 2.0 * 4095.0;
            swappedchar = (char) scaledwave;
            v4wavechars[i] = (char) ((char) (swappedchar >> 8) + (char) (swappedchar << 8));
        }
        writeVFiles(v1wavechars, v2wavechars, v3wavechars, v4wavechars);
    }

    protected void writeHFiles(char[] h1wave, char[] h2wave, char[] h3wave, char[] h4wave) {
        try {
            int returnValue = fc.showOpenDialog(windowReference.getWindow());
            if (returnValue == fc.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                String parentpath = file.getPath();
                String childname = hrootname.getText();
                if (childname.length() <= maxcharlength && childname.indexOf("_") == -1) {
                    String h1filename = childname + "_H1.w16";
                    String h2filename = childname + "_H2.w16";
                    String h3filename = childname + "_H3.w16";
                    String h4filename = childname + "_H4.w16";
                    File h1file = new File(parentpath, h1filename);
                    FileOutputStream h1out = new FileOutputStream(h1file);
                    DataOutputStream h1data = new DataOutputStream(h1out);
                    for (int j = 0; j < h1wave.length; j++) {
                        h1data.writeChar(h1wave[j]);
                    }
                    File h2file = new File(parentpath, h2filename);
                    FileOutputStream h2out = new FileOutputStream(h2file);
                    DataOutputStream h2data = new DataOutputStream(h2out);
                    for (int j = 0; j < h2wave.length; j++) {
                        h2data.writeChar(h2wave[j]);
                    }
                    File h3file = new File(parentpath, h3filename);
                    FileOutputStream h3out = new FileOutputStream(h3file);
                    DataOutputStream h3data = new DataOutputStream(h3out);
                    for (int j = 0; j < h3wave.length; j++) {
                        h3data.writeChar(h3wave[j]);
                    }
                    File h4file = new File(parentpath, h4filename);
                    FileOutputStream h4out = new FileOutputStream(h4file);
                    DataOutputStream h4data = new DataOutputStream(h4out);
                    for (int j = 0; j < h4wave.length; j++) {
                        h4data.writeChar(h4wave[j]);
                    }
                    h1data.flush();
                    h2data.flush();
                    h3data.flush();
                    h4data.flush();
                    h1out.close();
                    h2out.close();
                    h3out.close();
                    h4out.close();
                    hcurrentfilename = childname;
                    fileLabel.setText("Wrote files with rootnames " + h1filename + ", " + h2filename + ", " + h3filename + ", " + h4filename);
                } else {
                    fileLabel.setText("Error: Either your base filename exceeds the 17 character limit, or you have used the illegal '_' character.  Please try again.");
                }
            } else {
                System.out.println("Save command canceled by user.");
            }
        } catch (IOException ioe) {
        }
    }

    protected void writeVFiles(char[] v1wave, char[] v2wave, char[] v3wave, char[] v4wave) {
        try {
            int returnValue = fc.showOpenDialog(windowReference.getWindow());
            if (returnValue == fc.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                ft.cacheURL(file);
                String parentpath = file.getPath();
                String childname = vrootname.getText();
                if (childname.length() <= maxcharlength && childname.indexOf("_") == -1) {
                    String v1filename = childname + "_V1.w16";
                    String v2filename = childname + "_V2.w16";
                    String v3filename = childname + "_V3.w16";
                    String v4filename = childname + "_V4.w16";
                    File v1file = new File(parentpath, v1filename);
                    FileOutputStream v1out = new FileOutputStream(v1file);
                    DataOutputStream v1data = new DataOutputStream(v1out);
                    for (int j = 0; j < v1wave.length; j++) {
                        v1data.writeChar(v1wave[j]);
                    }
                    File v2file = new File(parentpath, v2filename);
                    FileOutputStream v2out = new FileOutputStream(v2file);
                    DataOutputStream v2data = new DataOutputStream(v2out);
                    for (int j = 0; j < v2wave.length; j++) {
                        v2data.writeChar(v2wave[j]);
                    }
                    File v3file = new File(parentpath, v3filename);
                    FileOutputStream v3out = new FileOutputStream(v3file);
                    DataOutputStream v3data = new DataOutputStream(v3out);
                    for (int j = 0; j < v3wave.length; j++) {
                        v3data.writeChar(v3wave[j]);
                    }
                    File v4file = new File(parentpath, v4filename);
                    FileOutputStream v4out = new FileOutputStream(v4file);
                    DataOutputStream v4data = new DataOutputStream(v4out);
                    for (int j = 0; j < v4wave.length; j++) {
                        v4data.writeChar(v4wave[j]);
                    }
                    v1data.flush();
                    v2data.flush();
                    v3data.flush();
                    v4data.flush();
                    v1out.close();
                    v2out.close();
                    v3out.close();
                    v4out.close();
                    vcurrentfilename = childname;
                    fileLabel.setText("Wrote files with rootnames " + v1filename + ", " + v2filename + ", " + v3filename + ", " + v4filename);
                } else {
                    fileLabel.setText("Error: Either your base filename exceeds the 17 character limit, or you have used the illegal '_' character.  Please try again.");
                }
            } else {
                System.out.println("Save command canceled by user.");
            }
        } catch (IOException ioe) {
        }
    }

    /** Routine to submit the last saved H waveform */
    protected void submitHWaveforms() {
        String h1name = new String(hcurrentfilename + "_H1.w16");
        String h2name = new String(hcurrentfilename + "_H2.w16");
        String h3name = new String(hcurrentfilename + "_H3.w16");
        String h4name = new String(hcurrentfilename + "_H4.w16");
        if (hcurrentfilename.equals("")) {
            System.out.println("There is no waveform file saved.");
        } else {
            Channel h1ch;
            Channel h2ch;
            Channel h3ch;
            Channel h4ch;
            h1ch = ChannelFactory.defaultFactory().getChannel("Ring_Mag:PS_IKickH01:7121:FGWAVE");
            h2ch = ChannelFactory.defaultFactory().getChannel("Ring_Mag:PS_IKickH02:7121:FGWAVE");
            h3ch = ChannelFactory.defaultFactory().getChannel("Ring_Mag:PS_IKickH03:7121:FGWAVE");
            h4ch = ChannelFactory.defaultFactory().getChannel("Ring_Mag:PS_IKickH04:7121:FGWAVE");
            h1ch.connectAndWait();
            h2ch.connectAndWait();
            h3ch.connectAndWait();
            h4ch.connectAndWait();
            try {
                h1ch.putVal(h1name);
                h2ch.putVal(h2name);
                h3ch.putVal(h3name);
                h4ch.putVal(h4name);
                Channel.flushIO();
                System.out.println("Loaded filenames: " + h1name + "\t" + h2name + "\t" + h3name + "\t" + h4name);
                fileLabel.setText("Loaded filenames: " + h1name + "\t" + h2name + "\t" + h3name + "\t" + h4name);
            } catch (ConnectionException e) {
                System.err.println("Unable to connect to channel access.");
                fileLabel.setText("Unable to connect to channel access.");
            } catch (PutException e) {
                System.err.println("Unable to set process variables.");
                fileLabel.setText("Unable to set process variables.");
            }
        }
    }

    /** Routine to submit the last saved V waveform */
    protected void submitVWaveforms() {
        String v1name = new String(vcurrentfilename + "_V1.w16");
        String v2name = new String(vcurrentfilename + "_V2.w16");
        String v3name = new String(vcurrentfilename + "_V3.w16");
        String v4name = new String(vcurrentfilename + "_V4.w16");
        if (vcurrentfilename.equals("")) {
            System.out.println("There is no waveform file saved.");
        } else {
            Channel v1ch;
            Channel v2ch;
            Channel v3ch;
            Channel v4ch;
            v1ch = ChannelFactory.defaultFactory().getChannel("Ring_Mag:PS_IKickV01:7121:FGWAVE");
            v2ch = ChannelFactory.defaultFactory().getChannel("Ring_Mag:PS_IKickV02:7121:FGWAVE");
            v3ch = ChannelFactory.defaultFactory().getChannel("Ring_Mag:PS_IKickV03:7121:FGWAVE");
            v4ch = ChannelFactory.defaultFactory().getChannel("Ring_Mag:PS_IKickV04:7121:FGWAVE");
            v1ch.connectAndWait();
            v2ch.connectAndWait();
            v3ch.connectAndWait();
            v4ch.connectAndWait();
            try {
                v1ch.putVal(v1name);
                v2ch.putVal(v2name);
                v3ch.putVal(v3name);
                v4ch.putVal(v4name);
                Channel.flushIO();
                System.out.println("Loaded filenames: " + v1name + "\t" + v2name + "\t" + v3name + "\t" + v4name);
                fileLabel.setText("Loaded filenames: " + v1name + "\t" + v2name + "\t" + v3name + "\t" + v4name);
            } catch (ConnectionException e) {
                System.err.println("Unable to connect to channel access.");
                fileLabel.setText("Unable to connect to channel access.");
            } catch (PutException e) {
                System.err.println("Unable to set process variables.");
                fileLabel.setText("Unable to set process variables.");
            }
        }
    }

    /** get the main window */
    protected DefaultXalWindow getMainWindow() {
        return (DefaultXalWindow) windowReference.getWindow();
    }
}
