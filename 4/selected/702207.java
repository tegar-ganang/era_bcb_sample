package gov.sns.apps.scldriftbeam;

import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.RfCavity;
import gov.sns.xal.smf.impl.SCLCavity;
import gov.sns.xal.smf.impl.sclcavity.*;
import gov.sns.xal.smf.impl.CurrentMonitor;
import gov.sns.xal.model.probe.traj.*;
import gov.sns.ca.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;

/**
 * 
 * @author Paul Chu
 *
 */
public class CavitySelector {

    JDialog cavSelector = new JDialog();

    JTable cavTable;

    AcceleratorSeq mySeq;

    List allCavs;

    CavityTableModel cavTableModel;

    String selectedCav = null;

    SCLCavity rfCav;

    JButton done, cancel;

    DriftBeamDocument myDoc;

    NumberFormat nf = NumberFormat.getNumberInstance();

    double noise2signal = 0.45;

    double pulseWidthdt = 0.;

    double[] phaseArry = new double[512];

    double[] amplitudeArry = new double[512];

    double noiseAmplitude = 0.;

    double noisePhase = 0.;

    double amplitudeFluctuation = 0.;

    double phaseFluctuation = 0.;

    double beamAmplitude = 0.;

    double beamPhase = 0.;

    public CavitySelector(AcceleratorSeq seq, DriftBeamDocument doc) {
        mySeq = seq;
        myDoc = doc;
        allCavs = seq.getAllNodesOfType("RfCavity");
        cavTableModel = new CavityTableModel();
        cavTable = new JTable(cavTableModel);
        for (int i = 0; i < allCavs.size(); i++) {
            cavTableModel.addRowName(((RfCavity) allCavs.get(i)).getId(), i);
            cavTableModel.setValueAt(new Boolean(false), i, 1);
        }
        done = new JButton("OK");
        cancel = new JButton("Cancel");
        done.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                initCavity(e);
            }
        });
        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cavSelector.setVisible(false);
            }
        });
    }

    protected void initCavity(ActionEvent ae) {
        selectedCav = cavTableModel.getRowName(cavTable.getSelectedRow());
        cavSelector.setVisible(false);
        rfCav = (SCLCavity) (mySeq.getNodeWithId(selectedCav));
        myDoc.getSCLPhase().setCavity(rfCav);
        myDoc.getSCLPhase().cavName.setForeground(Color.blue);
        myDoc.getSCLPhase().cavName.setText("Cavity: " + selectedCav);
        int cavInd = Integer.parseInt(rfCav.getId().substring(10, 12));
        if (cavInd < 12) {
            myDoc.getSCLPhase().cavType.setSelectedIndex(1);
        } else {
            myDoc.getSCLPhase().cavType.setSelectedIndex(0);
        }
        myDoc.getSCLPhase().tfAccFld.setValue(rfCav.getDfltCavAmp() * rfCav.getStructureTTF());
        nf.setMaximumFractionDigits(1);
        myDoc.getSCLPhase().tfQuality.setText(nf.format(rfCav.getQLoaded()));
        myDoc.getSCLPhase().tfPhaseSt.setValue(rfCav.getDfltAvgCavPhase());
        myDoc.getSCLPhase().setFrequency(rfCav.getCavFreq());
        double eng = ((EnvelopeTrajectory) myDoc.getScenario().getTrajectory()).stateForElement(selectedCav).getKineticEnergy() / 1.e6;
        myDoc.getSCLPhase().tfEnergy.setValue(eng);
        ChannelFactory caF = ChannelFactory.defaultFactory();
        if (myDoc.isOnline) {
            try {
                Channel ca = caF.getChannel("ICS_Tim:Gate_BeamRef:GateWidth");
                myDoc.getSCLPhase().tfPulse.setValue(ca.getValDbl());
                Channel ca1 = caF.getChannel(rfCav.channelSuite().getChannel("cavAmpSet").getId().substring(0, 9) + "ResCtrl" + rfCav.channelSuite().getChannel("cavAmpSet").getId().substring(12, 15) + ":ResErr_Avg");
                myDoc.getSCLPhase().tfDetuning.setValue(ca1.getValDbl());
                Channel ca2 = caF.getChannel(rfCav.channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Wf_Dt");
                pulseWidthdt = ca2.getValDbl();
                myDoc.llrfx = new double[512];
                for (int i = 0; i < 512; i++) myDoc.llrfx[i] = pulseWidthdt * i;
                Channel ca3 = caF.getChannel(rfCav.channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Field_WfP");
                Channel ca4 = caF.getChannel(rfCav.channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Field_WfA");
                myDoc.setpv(ca3, ca4);
                Channel cavFieldSetC = null;
                cavFieldSetC = rfCav.lazilyGetAndConnect(RfCavity.CAV_AMP_SET_HANDLE, cavFieldSetC);
                try {
                    Monitor cavFieldMonitor = cavFieldSetC.addMonitorValTime(new IEventSinkValTime() {

                        public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                            double cavField = newRecord.doubleValue();
                            myDoc.getSCLPhase().cavFieldSetPt(cavField);
                            if (cavField > 0.5) myDoc.getSCLPhase().cavFieldSP.setForeground(Color.RED);
                            myDoc.getSCLPhase().cavFieldSP.setText("Field Set Pt.: " + nf.format(cavField));
                        }
                    }, Monitor.VALUE);
                } catch (ConnectionException e) {
                    System.out.println("Cannot connect to " + cavFieldSetC.getId());
                } catch (MonitorException e) {
                }
                Channel cavPhaseSetC = null;
                cavPhaseSetC = rfCav.lazilyGetAndConnect(RfCavity.CAV_PHASE_SET_HANDLE, cavPhaseSetC);
                try {
                    Monitor cavPhaseMonitor = cavPhaseSetC.addMonitorValTime(new IEventSinkValTime() {

                        public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                            double cavPhase = newRecord.doubleValue();
                            myDoc.getSCLPhase().cavPhaseSP.setText("Phase Set Pt.: " + nf.format(cavPhase));
                        }
                    }, Monitor.VALUE);
                } catch (ConnectionException e) {
                    System.out.println("Cannot connect to " + cavPhaseSetC.getId());
                } catch (MonitorException e) {
                }
            } catch (ConnectionException ce) {
                System.out.println("Cannot connect to PV(s)!");
            } catch (GetException ge) {
                System.out.println("Cannot get PV value(s)!");
            }
            myDoc.getSCLPhase().lbPhaseBm.setEnabled(true);
            myDoc.getSCLPhase().btPulse.setEnabled(true);
            myDoc.getSCLPhase().cavOff.setEnabled(true);
            if (myDoc.fullRecord) myDoc.getSCLPhase().phaseAvgBtn.setEnabled(true); else myDoc.getSCLPhase().phaseAvgBtn.setEnabled(false);
            myDoc.getSCLPhase().phaseAvgBtn1.setEnabled(true);
        }
        try {
            CurrentMonitor bcm = (CurrentMonitor) (mySeq.getAllNodesOfType("BCM").get(0));
            Channel beamIAvgC = null;
            beamIAvgC = bcm.lazilyGetAndConnect(CurrentMonitor.I_AVG_HANDLE, beamIAvgC);
            try {
                Monitor beamIAvgMonitor = beamIAvgC.addMonitorValTime(new IEventSinkValTime() {

                    public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                        double beamIAvg = newRecord.doubleValue();
                        if (myDoc.isOnline) {
                            myDoc.getSCLPhase().tfCurrent.setText(nf.format(beamIAvg));
                        } else {
                            myDoc.getSCLPhase().lbPhaseBm.setEnabled(false);
                            myDoc.getSCLPhase().btPulse.setEnabled(false);
                            myDoc.getSCLPhase().cavOff.setEnabled(false);
                            myDoc.getSCLPhase().phaseAvgBtn.setEnabled(false);
                            myDoc.getSCLPhase().phaseAvgBtn1.setEnabled(false);
                        }
                    }
                }, Monitor.VALUE);
            } catch (ConnectionException e) {
                System.out.println("Cannot connect to " + beamIAvgC.getId());
            } catch (MonitorException e) {
                System.out.println("CurrentMonitor error " + beamIAvgC.getId());
            }
        } catch (NullPointerException ne) {
            System.out.println("No BCM available in the selected sequence");
        } catch (ConnectionException ce) {
            System.out.println("Cannot connect to PV(s)!");
        } catch (NoSuchChannelException nse) {
            System.out.println("Cannot connect to BCM!");
            myDoc.getSCLPhase().tfCurrent.setValue(20.);
        }
    }

    protected SCLCavity getSelectedRfCavity() {
        return rfCav;
    }

    protected double getNoiseA() {
        return noiseAmplitude;
    }

    protected double getNoiseP() {
        return noisePhase;
    }

    protected double getAmpFlu() {
        return amplitudeFluctuation;
    }

    protected double getPhaseFlu() {
        return phaseFluctuation;
    }

    protected double getBeamLoad() {
        return beamAmplitude;
    }

    protected double getCavPhaseAvg(double start, double end, int totalPulses) {
        myDoc.getSCLPhase().lbPhaseBm.setEnabled(false);
        myDoc.getSCLPhase().btPulse.setEnabled(false);
        myDoc.getSCLPhase().phaseAvgBtn.setEnabled(false);
        myDoc.getSCLPhase().phaseAvgBtn1.setEnabled(false);
        int shot = myDoc.getLatest();
        phaseArry = myDoc.signalphase[shot];
        amplitudeArry = myDoc.signalamp[shot];
        if (totalPulses != 0) {
            final int startPt = (int) Math.round(start * 512. / myDoc.llrfx[511]);
            final int pts = (int) Math.round((end - start) * 512. / myDoc.llrfx[511]);
            double sigPhs = 0.;
            double sigAmp = 0.;
            double amin = 1.E8;
            double amax = -1.E8;
            double pmin = 1.E8;
            double pmax = -1.E8;
            noiseAmplitude = 0.;
            noisePhase = 0.;
            if (totalPulses == 1) {
                double a1 = 0.;
                double a2 = 0.;
                double p1 = 0.;
                double p2 = 0.;
                int previous = shot - 1;
                if (previous < 0) previous = 39;
                for (int i = 0; i < pts; i++) {
                    a1 = a1 + myDoc.signalamp[shot][startPt + i] / pts;
                    a2 = a2 + myDoc.signalamp[previous][startPt + i] / pts;
                    p1 = p1 + myDoc.signalphase[shot][startPt + i] / pts;
                    p2 = p2 + myDoc.signalphase[previous][startPt + i] / pts;
                }
                if (a1 > a2) {
                    sigAmp = a1;
                    sigPhs = p1;
                } else {
                    sigAmp = a2;
                    sigPhs = p2;
                }
            } else {
                myDoc.stopCorrelator();
                double[][] phaseRecord = new double[40][pts];
                double[][] ampRecord = new double[40][pts];
                double[] ampPeak = new double[40];
                int[] beamIndx = new int[40];
                int beamcount = 0;
                int noisecount = 0;
                double totals = 0.;
                double totala = 0.;
                double totalphase = 0.;
                for (int i = 0; i < 40; i++) {
                    beamIndx[i] = 0;
                    ampPeak[i] = -1.E8;
                    for (int j = 0; j < pts; j++) {
                        ampRecord[i][j] = myDoc.signalamp[i][startPt + j];
                        phaseRecord[i][j] = myDoc.signalphase[i][startPt + j];
                        if (ampRecord[i][j] > ampPeak[i]) ampPeak[i] = ampRecord[i][j];
                        if ((phaseRecord[i][j] - phaseRecord[0][0]) > 180) phaseRecord[i][j] = phaseRecord[i][j] - 360; else if ((phaseRecord[i][j] - phaseRecord[0][0]) < -180) phaseRecord[i][j] = phaseRecord[i][j] + 360;
                    }
                    amax = Math.max(amax, ampPeak[i]);
                }
                for (int i = 0; i < 40; i++) {
                    if (ampPeak[i] > 0.85 * amax) {
                        beamIndx[i] = 2;
                        beamcount++;
                        for (int j = 0; j < pts; j++) totalphase = totalphase + phaseRecord[i][j];
                    } else if (ampPeak[i] < noise2signal * amax) {
                        beamIndx[i] = 1;
                        noisecount++;
                        for (int j = 0; j < pts; j++) {
                            totals = totals + phaseRecord[i][j];
                            totala = totala + ampRecord[i][j];
                        }
                    }
                }
                if (beamcount * pts > 0) totalphase = totalphase / (pts * beamcount);
                for (int i = 0; i < 40; i++) {
                    if (beamIndx[i] == 2) {
                        for (int j = 0; j < pts; j++) {
                            if (Math.abs(phaseRecord[i][j] - totalphase) > 30) {
                                beamIndx[i] = 0;
                                beamcount--;
                                break;
                            }
                        }
                    }
                }
                for (int i = 0; i < 40; i++) {
                    if (beamIndx[i] == 2) {
                        sigAmp = sigAmp + ampPeak[i];
                        for (int j = 0; j < pts; j++) {
                            pmin = Math.min(pmin, phaseRecord[i][j]);
                            pmax = Math.max(pmax, phaseRecord[i][j]);
                            amin = Math.min(amin, ampRecord[i][j]);
                            sigPhs = sigPhs + phaseRecord[i][j];
                        }
                    }
                }
                if (beamcount * pts > 0) {
                    sigPhs = sigPhs / (pts * beamcount);
                    sigAmp = sigAmp / beamcount;
                    phaseFluctuation = 0.5 * (pmax - pmin);
                    amplitudeFluctuation = 100. * Math.max(amax - sigAmp, sigAmp - amin) / sigAmp;
                }
                if (noisecount * pts > 0) {
                    noisePhase = totals / (noisecount * pts);
                    noiseAmplitude = totala / (noisecount * pts);
                }
                if (beamcount + noisecount <= 30) {
                    myDoc.getSCLPhase().lbPhaseCav.setForeground(Color.RED);
                } else if (beamcount + noisecount <= 37) {
                    myDoc.getSCLPhase().lbPhaseCav.setForeground(Color.BLUE);
                } else {
                    myDoc.getSCLPhase().lbPhaseCav.setForeground(Color.BLACK);
                }
            }
            substratenoise(sigPhs, sigAmp);
        }
        myDoc.getSCLPhase().setPhasePlot(phaseArry, myDoc.llrfx);
        myDoc.getSCLPhase().setAmpPlot(amplitudeArry, myDoc.llrfx);
        myDoc.getSCLPhase().lbPhaseBm.setEnabled(true);
        myDoc.getSCLPhase().btPulse.setEnabled(true);
        if (myDoc.fullRecord) myDoc.getSCLPhase().phaseAvgBtn.setEnabled(true); else myDoc.getSCLPhase().phaseAvgBtn.setEnabled(false);
        myDoc.getSCLPhase().phaseAvgBtn1.setEnabled(true);
        return beamPhase;
    }

    private void substratenoise(double sp, double sa) {
        Phasor signal = new Phasor(sa, sp * Constant.rad);
        Phasor noise = new Phasor(noiseAmplitude, noisePhase * Constant.rad);
        signal.minus(noise);
        beamAmplitude = signal.getam();
        beamPhase = Constant.deg * signal.getph();
        if (beamPhase > 180) {
            beamPhase = beamPhase - 360;
        }
    }

    public JDialog popupCavSelector() {
        JScrollPane sp = new JScrollPane(cavTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cavSelector.getContentPane().add(sp, BorderLayout.CENTER);
        Box controls = new Box(BoxLayout.X_AXIS);
        controls.add(done);
        controls.add(cancel);
        cavSelector.getContentPane().add(controls, BorderLayout.SOUTH);
        cavSelector.pack();
        cavSelector.setVisible(true);
        return cavSelector;
    }

    public void setLocationRelativeTo(DriftBeamWindow win) {
        cavSelector.setLocationRelativeTo(win);
    }

    class CavityTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1;

        final String[] columnNames = { "Cavity", "Select" };

        final Object[][] data = new Object[allCavs.size()][columnNames.length];

        /** Container for row labels */
        private ArrayList rowNames = new ArrayList(allCavs.size());

        @Override
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public String getRowName(int row) {
            return (String) rowNames.get(row);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            if (col < 1) {
                return false;
            } else {
                return true;
            }
        }

        /** method to add a row name */
        public void addRowName(String name, int row) {
            rowNames.add(row, name);
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return rowNames.get(rowIndex);
            } else {
                return data[rowIndex][columnIndex];
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 1) {
                data[row][col] = value;
                if (cavTable != null && ((Boolean) value).booleanValue()) {
                    for (int i = 0; i < cavTable.getSelectedRow(); i++) {
                        setValueAt(new Boolean(false), i, 1);
                    }
                    for (int i = cavTable.getSelectedRow() + 1; i < allCavs.size() && i != cavTable.getSelectedRow(); i++) {
                        setValueAt(new Boolean(false), i, 1);
                    }
                }
            } else {
                data[row][col] = value;
            }
            fireTableCellUpdated(row, col);
            return;
        }
    }
}
