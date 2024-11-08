package gov.sns.apps.sclsetcm;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;
import gov.sns.tools.swing.DecimalField;

/**
 *
 * @author y32
 */
public class SCLCmController implements ItemListener, ActionListener {

    JTable sclTable;

    SCLTableModel sclTableModel;

    SCLCmMeasure msrcav;

    SCLCmRun runcav;

    SCLCmPhase phase;

    JButton start;

    JButton stop;

    JButton reset;

    JButton run;

    JButton calibrate;

    JTextField tfmon;

    double dEin;

    double dEout;

    double[] phase1;

    double[] phase2;

    JLabel inEnergy = new JLabel("Input Energy (MeV):");

    JLabel outEnergy = new JLabel("Output Energy (MeV):");

    DecimalField tfEin = new DecimalField(dEin, 10);

    DecimalField tfEout = new DecimalField(dEout, 10);

    SCLCmDocument myDoc;

    NumberFormat nf = NumberFormat.getNumberInstance();

    public SCLCmController(SCLCmDocument doc) {
        myDoc = doc;
    }

    public JPanel makeControlPanel() {
        JPanel controlPanel = new JPanel();
        sclTableModel = new SCLTableModel(myDoc);
        start = new JButton("Start");
        ;
        stop = new JButton("Stop");
        reset = new JButton("Reset");
        ;
        run = new JButton("RUN");
        calibrate = new JButton("COMP");
        tfmon = new JTextField(5);
        msrcav = new SCLCmMeasure(myDoc);
        sclTable = new JTable(sclTableModel);
        start.setEnabled(true);
        start.addActionListener(this);
        stop.setEnabled(false);
        stop.addActionListener(this);
        reset.setEnabled(false);
        reset.addActionListener(this);
        run.setEnabled(false);
        run.addActionListener(this);
        calibrate.setEnabled(false);
        calibrate.addActionListener(this);
        controlPanel.setLayout(new BorderLayout());
        JScrollPane sp = new JScrollPane(sclTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JPanel controls = new JPanel(new GridLayout(5, 5, 6, 7));
        JLabel dummy1 = new JLabel("");
        JLabel dummy2 = new JLabel("");
        JLabel dummy3 = new JLabel("");
        JLabel dummy4 = new JLabel("");
        JLabel dummy5 = new JLabel("");
        JLabel dummy6 = new JLabel("");
        JLabel dummy7 = new JLabel("");
        JLabel dummy8 = new JLabel("");
        JLabel dummy9 = new JLabel("");
        JLabel dummya = new JLabel("");
        JLabel dummyb = new JLabel("");
        JLabel dummyc = new JLabel("");
        JLabel dummyd = new JLabel("");
        JLabel dummye = new JLabel("");
        JLabel dummyf = new JLabel("");
        JLabel dummyg = new JLabel("");
        JLabel dummyh = new JLabel("");
        controls.add(dummy1);
        controls.add(dummy2);
        controls.add(dummy3);
        controls.add(dummy4);
        controls.add(dummy5);
        controls.add(start);
        controls.add(dummy6);
        controls.add(stop);
        controls.add(dummy7);
        controls.add(reset);
        controls.add(run);
        controls.add(dummy8);
        controls.add(tfmon);
        controls.add(dummya);
        controls.add(calibrate);
        controls.add(inEnergy);
        controls.add(tfEin);
        controls.add(dummyb);
        controls.add(outEnergy);
        controls.add(tfEout);
        controls.add(dummyc);
        controls.add(dummyd);
        controls.add(dummye);
        controls.add(dummyf);
        controls.add(dummyg);
        controls.setVisible(true);
        controlPanel.add(controls, BorderLayout.NORTH);
        controlPanel.add(sp, BorderLayout.CENTER);
        nf.setMaximumFractionDigits(2);
        dEin = myDoc.energy[0];
        tfEin.setText(nf.format(dEin));
        controlPanel.setVisible(true);
        String[] amp = new String[myDoc.numberOfCav];
        String[] phs = new String[myDoc.numberOfCav];
        for (int i = 0; i < myDoc.numberOfCav; i++) {
            amp[i] = myDoc.cav[i].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Field_WfA";
            phs[i] = myDoc.cav[i].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Field_WfP";
        }
        if (msrcav.setPV1(phs) && msrcav.setPV2(amp)) {
            myDoc.stopped = true;
        }
        return controlPanel;
    }

    public void itemStateChanged(ItemEvent ie) {
        Checkbox cb = (Checkbox) ie.getItemSelectable();
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getActionCommand().equals("Reset")) {
            excuteReset();
            myDoc.cavSelector.resetcav();
        } else if (ae.getActionCommand().equals("Start")) {
            excuteStart();
        } else if (ae.getActionCommand().equals("Stop")) {
            excuteStop();
        } else if (ae.getActionCommand().equals("RUN")) {
            myDoc.energy[0] = tfEin.getValue();
            runcav = new SCLCmRun(myDoc);
            phase = new SCLCmPhase(myDoc);
            excuteRun();
        } else if (ae.getActionCommand().equals("COMP")) {
            excuteCalibrate();
        }
    }

    protected void excuteReset() {
        start.setEnabled(true);
        stop.setEnabled(false);
        calibrate.setEnabled(false);
        run.setEnabled(false);
        reset.setEnabled(false);
        nf.setMaximumFractionDigits(2);
        dEin = myDoc.energy[0];
        tfEin.setText(nf.format(dEin));
        for (int i = 0; i < myDoc.numberOfCav; i++) {
            sclTableModel.setValueAt(0., i, 3);
            sclTableModel.setValueAt(0., i, 4);
            sclTableModel.setValueAt(0., i, 5);
            sclTableModel.setValueAt(0., i, 6);
            sclTableModel.setValueAt(myDoc.energy[i], i, 7);
            sclTableModel.setValueAt(0., i, 8);
            sclTableModel.setValueAt(0., i, 9);
            sclTableModel.setValueAt(0., i, 11);
        }
        msrcav.reset();
        tfmon.setText(String.valueOf(msrcav.correlated));
    }

    private void excuteStart() {
        myDoc.getMonitor().stopmonitor();
        stop.setEnabled(true);
        start.setEnabled(false);
        reset.setEnabled(false);
        run.setEnabled(false);
        calibrate.setEnabled(false);
        myDoc.beamShape = myDoc.getMonitor().getBeamOnly();
        if (myDoc.stopped) msrcav.startCorrelator();
    }

    private void excuteStop() {
        stop.setEnabled(false);
        start.setEnabled(true);
        calibrate.setEnabled(false);
        reset.setEnabled(true);
        run.setEnabled(true);
        msrcav.stopCorrelator();
        double[] endShape = myDoc.getMonitor().getBeamOnly();
        try {
            for (int i = 0; i < endShape.length; i++) {
                myDoc.beamShape[i] += endShape[i];
                myDoc.beamShape[i] *= 0.5;
            }
        } catch (NullPointerException ne) {
            myDoc.errormsg("Error, beam pulse array hasn't been initialized!");
        }
        for (int i = 0; i < myDoc.numberOfCav; i++) {
            sclTableModel.setValueAt(myDoc.beamAmp[i], i, 3);
            sclTableModel.setValueAt(myDoc.beamPhase[i], i, 4);
        }
        myDoc.setcoef(myDoc.beamShape.length);
    }

    private double unify(double p) {
        double ps = p;
        while (ps > 180) {
            ps -= 360;
        }
        while (ps < -180) {
            ps += 360;
        }
        return ps;
    }

    private void excuteRun() {
        run.setEnabled(false);
        calibrate.setEnabled(true);
        runcav.setrun(0);
        runcav.excute();
        phase2 = phase.getphase(myDoc.designAmp, myDoc.designPhase);
        nf.setMaximumFractionDigits(2);
        dEout = myDoc.engbeam[myDoc.numberOfCav - 1];
        tfEout.setText(nf.format(dEout));
        phase1 = phase.getphase(myDoc.beamLoad);
        for (int i = 0; i < myDoc.numberOfCav; i++) {
            myDoc.cavPhaseSt[i] = unify(myDoc.modelPhase[i] + phase2[i] - phase1[i]);
            sclTableModel.setValueAt(myDoc.beamLoad[i], i, 5);
            sclTableModel.setValueAt(myDoc.cavPhaseSt[i], i, 6);
            sclTableModel.setValueAt(myDoc.energy[i], i, 7);
        }
        run.setEnabled(true);
    }

    private void excuteCalibrate() {
        calibrate.setEnabled(false);
        SCLCmCalibrate sclcal = new SCLCmCalibrate(myDoc);
        sclcal.measureAmp();
        phase2 = phase.getphase(myDoc.cavAmpSt, myDoc.designPhase);
        nf.setMaximumFractionDigits(2);
        dEout = myDoc.engbeam[myDoc.numberOfCav - 1];
        tfEout.setText(nf.format(dEout));
        runcav.setrun(1);
        runcav.excute();
        for (int i = 0; i < myDoc.numberOfCav; i++) {
            myDoc.cavPhaseRl[i] = unify(myDoc.modelPhase[i] + phase2[i] - phase1[i]);
            sclTableModel.setValueAt(myDoc.energy[i], i, 7);
            sclTableModel.setValueAt(myDoc.cavAmpSt[i], i, 8);
            sclTableModel.setValueAt(myDoc.cavPhaseRl[i], i, 9);
        }
        calibrate.setEnabled(true);
    }
}

class SCLTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1;

    protected SCLCmDocument doc;

    private ArrayList rowNames;

    public Object[][] dataValue;

    NumberFormat nf = NumberFormat.getNumberInstance();

    public static final int CAV_NAME = 0;

    public static final int DESIGN_AMP = 1;

    public static final int DESIGN_PHASE = 2;

    public static final int BEAM_AMP = 3;

    public static final int BEAM_PHASE = 4;

    public static final int BEAM_LOAD = 5;

    public static final int CAV_PHASE = 6;

    public static final int OUT_ENERGY = 7;

    public static final int CAL_AMP = 8;

    public static final int CAL_PHASE = 9;

    public static final int CAL_QLOAD = 10;

    public static final int CAL_PLACE = 11;

    public final String[] columnHeader = { "Cav #", "Design Amp", "Design Phase", "Beam Amp", "Beam Phase", "Beam Load", "Phase SetPt", "Input Energy", "CLBRT AMP", "CLBRT PHASE", "Q Loaded", "Displace" };

    public SCLTableModel(SCLCmDocument aDoc) {
        doc = aDoc;
        if (rowNames != null) rowNames.clear();
        rowNames = new ArrayList(doc.numberOfCav);
        dataValue = new Object[doc.numberOfCav][columnHeader.length];
        for (int i = 0; i < doc.numberOfCav; i++) {
            addRowName(doc.selectedCav[i].substring(10, 13), i);
            setValueAt(doc.designAmp[i], i, 1);
            setValueAt(doc.designPhase[i], i, 2);
            setValueAt(doc.energy[i], i, 7);
            setValueAt(doc.quality[i], i, 10);
        }
    }

    public void addRowName(String name, int row) {
        rowNames.add(row, name);
    }

    public String getRowName(int row) {
        return (String) rowNames.get(row);
    }

    public int getColumnCount() {
        return columnHeader.length;
    }

    public int getRowCount() {
        return dataValue.length;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        if (col == 0) return false; else return true;
    }

    public Object getValueAt(int row, int col) {
        if (col == 0) {
            return rowNames.get(row);
        } else {
            return dataValue[row][col];
        }
    }

    @Override
    public String getColumnName(int col) {
        return columnHeader[col];
    }

    @Override
    public void setValueAt(Object val, int row, int col) {
        if (dataValue.length < row) {
            System.out.println("Out of object boundary.");
            return;
        }
        if (dataValue[0].length < col) {
            System.out.println("Out of object boundary.");
            return;
        }
        switch(col) {
            case 1:
                doc.designAmp[row] = (Double) val;
                break;
            case 2:
                doc.designPhase[row] = (Double) val;
                break;
            case 3:
                doc.beamAmp[row] = (Double) val;
                break;
            case 4:
                doc.beamPhase[row] = (Double) val;
                break;
            case 5:
                doc.beamLoad[row] = (Double) val;
                break;
            case 7:
                doc.energy[row] = (Double) val;
                break;
            case 8:
                doc.cavAmpSt[row] = (Double) val;
                break;
            case 10:
                doc.quality[row] = (Double) val;
            case 11:
                doc.displace[row] = (Double) val;
        }
        dataValue[row][col] = val;
        fireTableCellUpdated(row, col);
        return;
    }

    @Override
    public Class getColumnClass(int col) {
        Class colDataType = super.getColumnClass(col);
        if (col == CAV_NAME) {
            colDataType = String.class;
            return colDataType;
        }
        colDataType = Double.class;
        return colDataType;
    }
}
