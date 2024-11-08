package gov.sns.apps.sclaffmonitor;

import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.SCLCavity;
import gov.sns.xal.smf.impl.RfCavity;
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
public class AffSelector extends Thread {

    JTable cavTable;

    AcceleratorSeq mySeq;

    AffDocument myDoc;

    JPanel controlPanel = null;

    JPanel monitorPanel = null;

    JPanel selectPanel;

    JButton selected;

    List allCavs;

    CavityTableModel cavTableModel;

    AffMeasure[] affm;

    int allsclcavs;

    ChannelFactory cf = ChannelFactory.defaultFactory();

    int numberOfCav;

    protected JPanel makeSelectPanel() {
        selected = new JButton("Accept Selection");
        selected.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                initCavity();
            }
        });
        JPanel controls = new JPanel(new GridLayout(1, 5));
        JLabel dum1 = new JLabel("");
        JLabel dum2 = new JLabel("");
        JLabel dum3 = new JLabel("");
        JLabel dum4 = new JLabel("");
        selected.setForeground(Color.BLUE);
        controls.add(dum1);
        controls.add(dum2);
        controls.add(selected);
        controls.add(dum3);
        controls.add(dum4);
        JScrollPane sp = new JScrollPane(cavTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JPanel selPanel = new JPanel();
        BorderLayout bgl = new BorderLayout();
        selPanel.setLayout(bgl);
        selPanel.setPreferredSize(new Dimension(800, 500));
        selPanel.add(sp, bgl.CENTER);
        selPanel.add(controls, bgl.NORTH);
        selectPanel = new JPanel();
        bgl = new BorderLayout();
        controlPanel = myDoc.getController().makeControlPanel();
        selectPanel.setLayout(bgl);
        selectPanel.add(selPanel, bgl.CENTER);
        selectPanel.add(controlPanel, bgl.SOUTH);
        selectPanel.setVisible(true);
        start();
        return selectPanel;
    }

    @Override
    public void run() {
        allsclcavs = allCavs.size();
        if (allsclcavs > 0) {
            try {
                affm = new AffMeasure[allsclcavs];
                String[] err = new String[allsclcavs];
                for (int i = 0; i < allsclcavs; i++) {
                    err[i] = ((RfCavity) allCavs.get(i)).channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "PeakErr";
                    affm[i] = new AffMeasure(myDoc);
                    affm[i].row = i;
                    if (affm[i].setPV1(err[i])) {
                        Thread nm = new Thread(affm[i]);
                        nm.start();
                    }
                }
            } catch (NullPointerException ne) {
            }
        }
    }

    public AffSelector(AffDocument doc) {
        myDoc = doc;
        try {
            mySeq = myDoc.mySeq;
            if (myDoc.selected == null) myDoc.selected = new ArrayList(); else myDoc.selected.clear();
            if (mySeq != null) {
                allCavs = mySeq.getAllNodesOfType("RfCavity");
                myDoc.affk = new double[allCavs.size()];
                myDoc.affkp = new double[allCavs.size()];
                myDoc.affki = new double[allCavs.size()];
                myDoc.afftimeshift = new double[allCavs.size()];
                myDoc.affstart = new double[allCavs.size()];
                myDoc.affduration = new double[allCavs.size()];
                myDoc.affmax = new int[allCavs.size()];
                myDoc.affavg = new int[allCavs.size()];
                myDoc.afflimit = new double[allCavs.size()];
                myDoc.affpeak = new double[allCavs.size()];
                myDoc.afferror = new String[allCavs.size()];
                myDoc.affstatus = new String[allCavs.size()];
                myDoc.affbuffer = new String[allCavs.size()];
            }
            cavTableModel = new CavityTableModel();
            cavTable = new JTable(cavTableModel);
            for (int i = 0; i < allCavs.size(); i++) {
                cavTableModel.addRowName(((RfCavity) allCavs.get(i)).getId(), i);
                cavTableModel.setValueAt(new Boolean(false), i, 1);
                cavTableModel.setValueAt("aff", i, 2);
                cavTableModel.setValueAt(0., i, 3);
                cavTableModel.setValueAt(0., i, 4);
                cavTableModel.setValueAt(0., i, 5);
                cavTableModel.setValueAt(0., i, 6);
                cavTableModel.setValueAt(0., i, 7);
                cavTableModel.setValueAt(0., i, 8);
                cavTableModel.setValueAt(0, i, 9);
                cavTableModel.setValueAt(0, i, 10);
                cavTableModel.setValueAt("buf", i, 11);
                cavTableModel.setValueAt("0.00", i, 12);
                cavTableModel.setValueAt(1.5, i, 13);
            }
        } catch (NullPointerException ne) {
            myDoc.errormsg("Error, in initializing the on-line model!");
        }
        numberOfCav = 0;
    }

    protected void initCavity() {
        if (myDoc.selected != null) {
            numberOfCav = myDoc.selected.size();
            myDoc.numberOfCav = numberOfCav;
        } else if (numberOfCav == 0) {
            myDoc.errormsg("Error, 0 cavity been selected!");
            return;
        }
        myDoc.selectedCav = new String[numberOfCav];
        myDoc.cav = new SCLCavity[numberOfCav];
        if (numberOfCav > 1) {
            String tmp;
            for (int i = 0; i < numberOfCav; i++) {
                myDoc.selectedCav[i] = (String) myDoc.selected.get(i);
                myDoc.cav[i] = (SCLCavity) (mySeq.getNodeWithId(myDoc.selectedCav[i]));
            }
        } else {
            myDoc.selectedCav[0] = (String) myDoc.selected.get(0);
            myDoc.cav[0] = (SCLCavity) (mySeq.getNodeWithId(myDoc.selectedCav[0]));
        }
        if (myDoc.getMonitor().monitorPanel == null) {
            myDoc.getMonitor().monitorPanel = myDoc.getMonitor().makeMonitorPanel();
            myDoc.myWindow().getMainPanel().add("Monitor", myDoc.getMonitor().monitorPanel);
        } else {
            myDoc.getMonitor().update();
        }
        return;
    }

    class CavityTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1;

        final String[] columnNames = { "Cavity", "Select", "Status", "Start", "Duration", "K", "Kp", "Ki", "Tshift", "Max.", "Average", "Buffer", "Error", "Limit" };

        final Object[][] data = new Object[allCavs.size()][columnNames.length];

        /** Container for row labels */
        private ArrayList rowNames = new ArrayList(allCavs.size());

        @Override
        public Class getColumnClass(int col) {
            Class colDataType = super.getColumnClass(col);
            switch(col) {
                case 0:
                case 2:
                case 11:
                case 12:
                    colDataType = String.class;
                    break;
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 13:
                    colDataType = Double.class;
                    break;
                case 9:
                case 10:
                    colDataType = Integer.class;
                    break;
                case 1:
                    return getValueAt(0, col).getClass();
            }
            return colDataType;
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

        public Object getValueAt(int row, int col) {
            if (col == 0) {
                return rowNames.get(row);
            } else if (col == 12) {
                if (myDoc.affpeak[row] >= myDoc.afflimit[row]) {
                    return "<html><body><font color=RED>" + myDoc.afferror[row] + "</font></body></html>";
                }
            } else if (col == 2) {
                if (myDoc.affstatus[row].contains("ff")) return "<html><body><font color=BLUE>" + myDoc.affstatus[row] + "</font></body></html>";
            }
            return data[row][col];
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (data.length < row) {
                System.out.println("Out of object boundary.");
                return;
            }
            if (data[0].length < col) {
                System.out.println("Out of object boundary.");
                return;
            }
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
                switch(col) {
                    case 2:
                        myDoc.affstatus[row] = (String) value;
                        break;
                    case 3:
                        myDoc.affstart[row] = (Double) value;
                        break;
                    case 4:
                        myDoc.affduration[row] = (Double) value;
                        break;
                    case 5:
                        myDoc.affk[row] = (Double) value;
                        break;
                    case 6:
                        myDoc.affkp[row] = (Double) value;
                        break;
                    case 7:
                        myDoc.affki[row] = (Double) value;
                        break;
                    case 8:
                        myDoc.afftimeshift[row] = (Double) value;
                        break;
                    case 9:
                        myDoc.affmax[row] = (Integer) value;
                        break;
                    case 10:
                        myDoc.affavg[row] = (Integer) value;
                        break;
                    case 11:
                        myDoc.affbuffer[row] = (String) value;
                        break;
                    case 12:
                        myDoc.afferror[row] = (String) value;
                        break;
                    case 13:
                        myDoc.afflimit[row] = (Double) value;
                }
                data[row][col] = value;
            }
            fireTableCellUpdated(row, col);
            return;
        }
    }
}
