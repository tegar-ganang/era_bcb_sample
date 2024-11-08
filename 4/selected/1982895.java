package gov.sns.apps.sclsetcm;

import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.RfCavity;
import gov.sns.xal.smf.impl.SCLCavity;
import gov.sns.xal.model.probe.traj.*;
import gov.sns.ca.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author y32
 */
public class SCLCmSelector {

    JTable cavTable;

    AcceleratorSeq mySeq;

    SCLCmDocument myDoc;

    JPanel controlPanel = null;

    JPanel boxPanel = null;

    JPanel selectPanel;

    List allCavs;

    CavityTableModel cavTableModel;

    JButton done;

    JButton toff;

    JButton tune;

    JButton setup;

    JButton ton;

    ChannelFactory cf = ChannelFactory.defaultFactory();

    int numberOfCav;

    protected JPanel makeSelectPanel() {
        done = new JButton("Accept selection");
        done.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                initCavity();
                myDoc.getController().excuteReset();
            }
        });
        toff = new JButton("Turn Off");
        toff.setForeground(Color.RED);
        toff.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < myDoc.numberOfCav; i++) {
                    try {
                        Channel LoopOff = cf.getChannel(myDoc.cav[i].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "LoopOff");
                        LoopOff.putVal("Close!");
                        Channel RFKill = cf.getChannel(myDoc.cav[i].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "RFKill");
                        RFKill.putVal("Kill");
                    } catch (ConnectionException ce) {
                        myDoc.errormsg("Error, cannot connect to PV!");
                    } catch (PutException pe) {
                        myDoc.errormsg("Error, cannot write to PV!");
                    }
                }
            }
        });
        ton = new JButton("Turn On");
        ton.setForeground(Color.RED);
        ton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < myDoc.numberOfCav; i++) {
                    try {
                        Channel ca2 = cf.getChannel(myDoc.cav[i].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "RunState");
                        ca2.putVal("Ramp");
                        Channel ca3 = cf.getChannel("SCL_HPRF:Tun" + myDoc.cav[i].channelSuite().getChannel("cavAmpSet").getId().substring(12, 16) + "Tun_Ctl");
                        ca3.putVal("Auto-Tune");
                    } catch (ConnectionException ce) {
                        myDoc.errormsg("Error, cannot connect to PV!");
                    } catch (PutException pe) {
                        myDoc.errormsg("Error, cannot write to PV!");
                    }
                }
            }
        });
        tune = new JButton("Tune Cavity");
        tune.setForeground(Color.RED);
        tune.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SCLCmTune scltun = new SCLCmTune(myDoc);
                if (myDoc.numberOfCav > 0) {
                    String[] phs = new String[myDoc.numberOfCav];
                    String bmgate = "ICS_Tim:Util:event46Count";
                    for (int i = 0; i < myDoc.numberOfCav; i++) {
                        phs[i] = myDoc.cav[i].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Field_WfP";
                    }
                    if (scltun.setPV1(phs) && scltun.setPV3(bmgate)) {
                        Thread t = new Thread(scltun);
                        t.start();
                    }
                }
            }
        });
        setup = new JButton("Setup Cavity");
        setup.setForeground(Color.RED);
        setup.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < myDoc.numberOfCav; i++) {
                    try {
                        if (myDoc.cavPhaseSt[i] >= -180 && myDoc.cavPhaseSt[i] <= 180) myDoc.cav[i].setCavPhase(myDoc.cavPhaseSt[i]);
                        if (myDoc.designAmp[i] >= 4 && myDoc.designAmp[i] <= 25) myDoc.cav[i].setCavAmp(myDoc.designAmp[i]);
                    } catch (ConnectionException ce) {
                        myDoc.errormsg("Error, cannot connect to PV!");
                    } catch (PutException pe) {
                        myDoc.errormsg("Error, cannot write to PV!");
                    }
                }
            }
        });
        selectPanel = new JPanel();
        JScrollPane sp = new JScrollPane(cavTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel controls = new JPanel();
        controls.setLayout(new GridLayout(12, 1));
        controls.setPreferredSize(new Dimension(180, 265));
        JLabel dmy2 = new JLabel("");
        JLabel dmy4 = new JLabel("");
        JLabel dmy5 = new JLabel("");
        JLabel dmy6 = new JLabel("");
        JLabel dmy7 = new JLabel("");
        JLabel dmy8 = new JLabel("");
        JLabel dmy9 = new JLabel("");
        controls.add(dmy2);
        controls.add(done);
        controls.add(dmy4);
        controls.add(dmy5);
        controls.add(toff);
        controls.add(dmy6);
        controls.add(tune);
        controls.add(dmy7);
        controls.add(dmy8);
        controls.add(setup);
        controls.add(dmy9);
        controls.add(ton);
        JPanel selPanel = new JPanel();
        BorderLayout bgl = new BorderLayout();
        selPanel.setLayout(bgl);
        selPanel.setPreferredSize(new Dimension(800, 300));
        selPanel.add(sp, BorderLayout.CENTER);
        selPanel.add(controls, BorderLayout.WEST);
        GridLayout gdl = new GridLayout(2, 1);
        selectPanel.setLayout(gdl);
        selectPanel.add(selPanel);
        selectPanel.setVisible(true);
        return selectPanel;
    }

    public SCLCmSelector(SCLCmDocument doc) {
        myDoc = doc;
        try {
            mySeq = myDoc.mySeq;
            if (myDoc.selected == null) myDoc.selected = new ArrayList(); else myDoc.selected.clear();
            if (mySeq != null) allCavs = mySeq.getAllNodesOfType("RfCavity");
            cavTableModel = new CavityTableModel();
            cavTable = new JTable(cavTableModel);
            for (int i = 0; i < allCavs.size(); i++) {
                cavTableModel.addRowName(((RfCavity) allCavs.get(i)).getId(), i);
                cavTableModel.setValueAt(new Boolean(false), i, 1);
            }
        } catch (NullPointerException ne) {
            myDoc.errormsg("Error, in initializing the on-line model!");
        }
        numberOfCav = 0;
    }

    protected void resetcav() {
        myDoc.designAmp = new double[numberOfCav];
        myDoc.designPhase = new double[numberOfCav];
        myDoc.energy = new double[numberOfCav];
        myDoc.engbeam = new double[numberOfCav];
        myDoc.quality = new double[numberOfCav];
        myDoc.position = new double[numberOfCav];
        myDoc.displace = new double[numberOfCav];
        myDoc.signalP = new double[numberOfCav];
        myDoc.signalA = new double[numberOfCav];
        myDoc.noiseP = new double[numberOfCav];
        myDoc.noiseA = new double[numberOfCav];
        myDoc.beamPhase = new double[numberOfCav];
        myDoc.beamAmp = new double[numberOfCav];
        myDoc.beamLoad = new double[numberOfCav];
        myDoc.rms = new double[numberOfCav];
        myDoc.modelPhase = new double[numberOfCav];
        myDoc.measuredAmp = new double[numberOfCav];
        myDoc.cavPhaseSt = new double[numberOfCav];
        myDoc.cavAmpSt = new double[numberOfCav];
        myDoc.cavPhaseRl = new double[numberOfCav];
        for (int i = 0; i < numberOfCav; i++) {
            myDoc.designAmp[i] = myDoc.cav[i].getDfltCavAmp() * myDoc.cav[i].getStructureTTF();
            myDoc.quality[i] = myDoc.cav[i].getQLoaded();
            myDoc.designPhase[i] = myDoc.cav[i].getDfltAvgCavPhase();
            myDoc.energy[i] = ((EnvelopeTrajectory) myDoc.getScenario().getTrajectory()).stateForElement(myDoc.selectedCav[i]).getKineticEnergy() / 1.e6;
            myDoc.position[i] = ((EnvelopeTrajectory) myDoc.getScenario().getTrajectory()).stateForElement(myDoc.selectedCav[i]).getPosition();
        }
    }

    protected void initCavity() {
        if (myDoc.selected != null) numberOfCav = myDoc.selected.size();
        myDoc.numberOfCav = numberOfCav;
        if (numberOfCav == 0) {
            myDoc.errormsg("Error, 0 cavity been selected!");
            return;
        }
        myDoc.selectedCav = new String[numberOfCav];
        myDoc.cav = new SCLCavity[numberOfCav];
        if (numberOfCav > 1) {
            String tmp;
            for (int i = 0; i < numberOfCav; i++) myDoc.selectedCav[i] = (String) myDoc.selected.get(i);
            for (int i = 0; i < numberOfCav; i++) {
                for (int j = i + 1; j < numberOfCav; j++) {
                    if (myDoc.selectedCav[i].compareTo(myDoc.selectedCav[j]) > 0) {
                        tmp = myDoc.selectedCav[i];
                        myDoc.selectedCav[i] = myDoc.selectedCav[j];
                        myDoc.selectedCav[j] = tmp;
                    }
                }
            }
            for (int i = 0; i < numberOfCav; i++) {
                myDoc.cav[i] = (SCLCavity) (mySeq.getNodeWithId(myDoc.selectedCav[i]));
            }
        } else {
            myDoc.selectedCav[0] = (String) myDoc.selected.get(0);
            myDoc.cav[0] = (SCLCavity) (mySeq.getNodeWithId(myDoc.selectedCav[0]));
        }
        resetcav();
        myDoc.getMonitor().reset();
        ChannelFactory caF = ChannelFactory.defaultFactory();
        try {
            Channel ca = caF.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Wf_Dt");
            myDoc.llrfDt = ca.getValDbl();
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error, cannot connect to Wf_Dt PV!");
        } catch (GetException ge) {
            myDoc.errormsg("Error, cannot get Dt value!");
        } finally {
        }
        if (myDoc.getMonitor().monitorPanel == null) {
            myDoc.getMonitor().monitorPanel = myDoc.getMonitor().makeMonitorPanel();
            myDoc.myWindow().getMainPanel().add("Monitor", myDoc.getMonitor().monitorPanel);
        } else {
            if (boxPanel != null) myDoc.getMonitor().monitorPanel.remove(boxPanel); else myDoc.getMonitor().monitorPanel.remove(myDoc.getMonitor().boxPanel);
            boxPanel = myDoc.getMonitor().makeboxPanel();
            myDoc.getMonitor().monitorPanel.add(boxPanel, BorderLayout.WEST);
        }
        if (controlPanel == null) {
            controlPanel = myDoc.getController().makeControlPanel();
            selectPanel.add(controlPanel);
        } else {
            selectPanel.setVisible(false);
            selectPanel.remove(controlPanel);
            controlPanel = myDoc.getController().makeControlPanel();
            selectPanel.add(controlPanel);
            selectPanel.setVisible(true);
        }
        myDoc.myWindow().repaint();
        return;
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
                if (((Boolean) value).booleanValue() && myDoc.selected.size() == 0) {
                    myDoc.selected.add(0, rowNames.get(row));
                } else if (!((Boolean) value).booleanValue()) {
                    for (int i = 0; i < myDoc.selected.size(); i++) {
                        if (myDoc.selected.get(i).equals(rowNames.get(row))) {
                            myDoc.selected.remove(i);
                        }
                    }
                } else if (((Boolean) value).booleanValue()) {
                    int j = 0;
                    for (int i = 0; i < myDoc.selected.size(); i++) {
                        if (myDoc.selected.get(i).equals(rowNames.get(row))) {
                            j = 1;
                        }
                    }
                    if (j == 0) {
                        myDoc.selected.add(myDoc.selected.size(), rowNames.get(row));
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
