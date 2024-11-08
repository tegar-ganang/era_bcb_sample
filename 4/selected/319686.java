package gov.sns.apps.magnetcycling.utils;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.*;
import gov.sns.tools.swing.*;
import gov.sns.tools.apputils.*;
import gov.sns.tools.plot.*;
import gov.sns.tools.xml.*;
import gov.sns.tools.scan.SecondEdition.UpdatingEventController;

/**
 *  The RunnerController class is a container for Runner class instance and
 *  provides other operations realted to the cycling control.
 *
 *@author     shishlo
 *@created    September 27, 2006
 */
public class RunnerController {

    private ContentController contentController = null;

    private Runner runner = new Runner();

    private PowerSupplyGroup runnerPSG = new PowerSupplyGroup();

    private Vector powerSupplyGroupV = new Vector();

    UpdatingEventController runnerUC = new UpdatingEventController();

    private JTextField messageTextLocal = new JTextField();

    private JPanel runnerControllerPanel = new JPanel();

    private JList groupList = new JList(new DefaultListModel());

    private JTable psTable = new JTable();

    private FunctionGraphsJPanel graphSubPanel = new FunctionGraphsJPanel();

    private Vector PowerSupplyCyclerV = new Vector();

    private Color[] colors = { Color.red, Color.blue, Color.green, Color.black, Color.magenta, Color.cyan };

    /**
	 *  Constructor for the RunnerController object
	 */
    public RunnerController() {
        runnerUC.setUpdateTime(1.0);
        runnerPSG.setName("Runner PS Group");
        runner.addPowerSupplyGroup(runnerPSG);
        JPanel cntrlPanel = new JPanel();
        cntrlPanel.setLayout(new VerticalLayout());
        cntrlPanel.add(runner.getPanel());
        groupList.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "pv goups"));
        cntrlPanel.add(groupList);
        runnerControllerPanel.setLayout(new BorderLayout());
        runnerControllerPanel.add(cntrlPanel, BorderLayout.WEST);
        runnerControllerPanel.add(graphSubPanel, BorderLayout.CENTER);
        SimpleChartPopupMenu.addPopupMenuTo(graphSubPanel);
        graphSubPanel.setOffScreenImageDrawing(true);
        graphSubPanel.setName("Cycler : PV Values vs. Time");
        graphSubPanel.setAxisNames("time, sec", "PV Values");
        graphSubPanel.setGraphBackGroundColor(Color.white);
        graphSubPanel.setLegendButtonVisible(true);
        graphSubPanel.setLegendBackground(Color.white);
        graphSubPanel.setChooseModeButtonVisible(true);
        JScrollPane scrollPane = new JScrollPane(psTable);
        scrollPane.setPreferredSize(new Dimension(1, 150));
        cntrlPanel.add(scrollPane);
        groupList.setVisibleRowCount(5);
        groupList.setEnabled(true);
        groupList.setFixedCellWidth(10);
        ListSelectionListener groupListListener = new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                updatePSCyclerTable();
            }
        };
        groupList.addListSelectionListener(groupListListener);
        AbstractTableModel tableModel = new AbstractTableModel() {

            public String getColumnName(int col) {
                if (col == 0) {
                    return "Power Supply PV";
                }
                return " Use";
            }

            public int getRowCount() {
                return PowerSupplyCyclerV.size();
            }

            public int getColumnCount() {
                return 2;
            }

            public Object getValueAt(int row, int col) {
                PowerSupplyCycler psc = (PowerSupplyCycler) PowerSupplyCyclerV.get(row);
                if (col == 1) {
                    return new Boolean(psc.getActive());
                }
                return psc.getChannelName();
            }

            public boolean isCellEditable(int row, int col) {
                if (col == 1) {
                    return true;
                }
                return false;
            }

            public Class getColumnClass(int c) {
                if (c == 1) {
                    return (new Boolean(true)).getClass();
                }
                return (new String("a")).getClass();
            }

            public void setValueAt(Object value, int row, int col) {
                if (col == 1) {
                    PowerSupplyCycler psc = (PowerSupplyCycler) PowerSupplyCyclerV.get(row);
                    psc.setActive(!psc.getActive());
                    runner.setNeedInit();
                    updateGraphDataSet();
                }
                fireTableCellUpdated(row, col);
            }
        };
        psTable.setModel(tableModel);
        psTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableColumn column = null;
        for (int i = 0; i < 2; i++) {
            column = psTable.getColumnModel().getColumn(i);
            if (i == 1) {
                column.setPreferredWidth(1);
            } else {
                column.setPreferredWidth(1000);
            }
        }
        runner.addStartListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                groupList.setEnabled(false);
                psTable.setEnabled(false);
                runnerUC.update();
            }
        });
        runner.addStepListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                runnerUC.update();
            }
        });
        runner.addStopListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (runner.isStartReady()) {
                    groupList.setEnabled(true);
                    psTable.setEnabled(true);
                } else {
                    groupList.setEnabled(false);
                    psTable.setEnabled(false);
                }
                runnerUC.update();
            }
        });
        runner.addInitListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (runner.isStartReady()) {
                    groupList.setEnabled(true);
                    psTable.setEnabled(true);
                } else {
                    groupList.setEnabled(false);
                    psTable.setEnabled(false);
                }
                runnerUC.update();
            }
        });
        runnerUC.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                graphSubPanel.refreshGraphJPanel();
            }
        });
    }

    /**
	 *  Updates the ps cyclers table using selection in PS Group List
	 */
    private void updatePSCyclerTable() {
        Object[] objs = groupList.getSelectedValues();
        if (objs.length > 0) {
            Vector v_tmp = new Vector();
            runnerPSG.removePowerSupplyCyclers();
            for (int i = 0, n = objs.length; i < n; i++) {
                PowerSupplyGroup psg = (PowerSupplyGroup) objs[i];
                Vector pscV = psg.getPowerSupplyCyclers();
                v_tmp.addAll(pscV);
                for (int j = 0, m = pscV.size(); j < m; j++) {
                    PowerSupplyCycler psc = (PowerSupplyCycler) pscV.get(j);
                    runnerPSG.addPowerSupplyCycler(psc);
                }
            }
            PowerSupplyCyclerV = v_tmp;
            ((AbstractTableModel) psTable.getModel()).fireTableDataChanged();
            runner.setNeedInit();
            updateGraphDataSet();
        }
    }

    /**
	 *  Adds graph data to the Graph Panel.
	 */
    private void updateGraphDataSet() {
        Vector gdV = new Vector();
        int count = 0;
        for (int i = 0, n = PowerSupplyCyclerV.size(); i < n; i++) {
            PowerSupplyCycler psc = (PowerSupplyCycler) PowerSupplyCyclerV.get(i);
            if (psc.getActive()) {
                BasicGraphData gd = psc.getGraphSetPV();
                gd.setGraphColor(colors[count % colors.length]);
                gdV.add(gd);
                gd = psc.getGraphReadBackPV();
                gd.setGraphColor(colors[count % colors.length]);
                gdV.add(gd);
                count++;
            }
        }
        graphSubPanel.setGraphData(gdV);
    }

    /**
	 *  Returns the panel attribute of the RunnerController object
	 *
	 *@return    The panel value
	 */
    public JPanel getPanel() {
        return runnerControllerPanel;
    }

    /**
	 *  Sets the all component fonts
	 *
	 *@param  fnt  The new font
	 */
    public void setFontForAll(Font fnt) {
        runner.setFontForAll(fnt);
        groupList.setFont(fnt);
        ((TitledBorder) groupList.getBorder()).setTitleFont(fnt);
    }

    /**
	 *  Sets the ContentController to the RunnerController object
	 *
	 *@param  contentController  The ContentController instance
	 */
    public void setContentController(ContentController contentController) {
        this.contentController = contentController;
    }

    /**
	 *  Adds a PowerSupplyGroup.
	 *
	 *@param  psg  The PowerSupplyGroup
	 */
    public void addPowerSupplyGroup(PowerSupplyGroup psg) {
        powerSupplyGroupV.add(psg);
        DefaultListModel listModel = (DefaultListModel) groupList.getModel();
        listModel.addElement(psg);
        groupList.setSelectedIndex(-1);
        contentController.addPowerSupplyGroup(psg);
    }

    /**
	 *  Removes the PowerSupplyGroup.
	 *
	 *@param  psg  The PowerSupplyGroup
	 */
    public void removePowerSupplyGroup(PowerSupplyGroup psg) {
        powerSupplyGroupV.remove(psg);
        DefaultListModel listModel = (DefaultListModel) groupList.getModel();
        listModel.removeElement(psg);
        groupList.setSelectedIndex(-1);
        contentController.removePowerSupplyGroup(psg);
    }

    /**
	 *  Removes all PowerSupplyGroups.
	 */
    public void removePowerSupplyGroups() {
        powerSupplyGroupV.clear();
        DefaultListModel listModel = (DefaultListModel) groupList.getModel();
        listModel.clear();
        groupList.setSelectedIndex(-1);
        contentController.removePowerSupplyGroups();
    }

    /**
	 *  Returns the powerSupplyGroups attribute of the RunnerController object
	 *
	 *@return    The powerSupplyGroups value
	 */
    public Vector getPowerSupplyGroups() {
        return powerSupplyGroupV;
    }

    /**
	 *  Sets the selected PS Group indexes of the RunnerController object
	 *
	 *@param  indV  The Vector of Integers with indexes
	 */
    public void setSelectedPSGroupIndexes(Vector indV) {
        int n = indV.size();
        int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = ((Integer) indV.get(i)).intValue();
        }
        groupList.setSelectedIndices(indices);
    }

    /**
	 *  Does actions before the panel is going to show up
	 */
    public void isGoingToShowUp() {
        updatePSCyclerTable();
        messageTextLocal.setText(null);
    }

    /**
	 *  Returns the messageText attribute
	 *
	 *@return    The messageText value
	 */
    public JTextField getMessageText() {
        return messageTextLocal;
    }

    /**
	 *  Reads data about power supplys groups and cyclers and initializes the
	 *  internal structures from data adaptor
	 *
	 *@param  data  The data adaptor
	 */
    public void readData(XmlDataAdaptor data) {
        messageTextLocal.setText(null);
        XmlDataAdaptor runnerDA = (XmlDataAdaptor) data.childAdaptor("RUNNER_CNTRL");
        if (runnerDA != null) {
            java.util.Iterator psgIt = runnerDA.childAdaptorIterator("PowerSupplyGroup");
            while (psgIt.hasNext()) {
                XmlDataAdaptor psgDA = (XmlDataAdaptor) psgIt.next();
                PowerSupplyGroup psg = new PowerSupplyGroup();
                psg.setName(psgDA.stringValue("group_name"));
                java.util.Iterator pscIt = psgDA.childAdaptorIterator("PowerSupplyCycler");
                while (pscIt.hasNext()) {
                    XmlDataAdaptor pscDA = (XmlDataAdaptor) pscIt.next();
                    PowerSupplyCycler psc = new PowerSupplyCycler();
                    XmlDataAdaptor pscSetPV_DA = (XmlDataAdaptor) pscDA.childAdaptor("PV_Set");
                    if (!pscSetPV_DA.stringValue("pv_name").equals("null")) {
                        psc.setChannelNameSet(pscSetPV_DA.stringValue("pv_name"));
                    } else {
                        psc.setChannelNameSet(null);
                    }
                    XmlDataAdaptor pscRbPV_DA = (XmlDataAdaptor) pscDA.childAdaptor("PV_Rb");
                    if (!pscRbPV_DA.stringValue("pv_name").equals("null")) {
                        psc.setChannelNameRB(pscRbPV_DA.stringValue("pv_name"));
                    } else {
                        psc.setChannelNameRB(null);
                    }
                    XmlDataAdaptor pscMaxCurr_DA = (XmlDataAdaptor) pscDA.childAdaptor("Max_I_Ampers");
                    psc.setMaxCurrent(pscMaxCurr_DA.doubleValue("I"));
                    XmlDataAdaptor pscnCycl_DA = (XmlDataAdaptor) pscDA.childAdaptor("Number_of_Cycles");
                    psc.setnCycles(pscnCycl_DA.intValue("n"));
                    XmlDataAdaptor pscChangeRate_DA = (XmlDataAdaptor) pscDA.childAdaptor("Rate_Amper_per_sec");
                    psc.setChangeRate(pscChangeRate_DA.doubleValue("rate"));
                    XmlDataAdaptor pscMinCurrTime_DA = (XmlDataAdaptor) pscDA.childAdaptor("MinI_Time_sec");
                    psc.setMinCurrTime(pscMinCurrTime_DA.doubleValue("time"));
                    XmlDataAdaptor pscMaxCurrTime_DA = (XmlDataAdaptor) pscDA.childAdaptor("MaxI_Time_sec");
                    psc.setMaxCurrTime(pscMaxCurrTime_DA.doubleValue("time"));
                    XmlDataAdaptor pscActive_DA = (XmlDataAdaptor) pscDA.childAdaptor("Active");
                    psc.setActive(pscActive_DA.booleanValue("isActive"));
                    psg.addPowerSupplyCycler(psc);
                }
                addPowerSupplyGroup(psg);
            }
        } else {
            messageTextLocal.setText("The dummaged input file.");
        }
        updatePSCyclerTable();
    }

    /**
	 *  Writes data about power supplys groups and cyclers into the data adaptor
	 *
	 *@param  data  The data adaptor
	 */
    public void writeData(XmlDataAdaptor data) {
        messageTextLocal.setText(null);
        XmlDataAdaptor runnerDA = (XmlDataAdaptor) data.createChild("RUNNER_CNTRL");
        Vector psgV = getPowerSupplyGroups();
        for (int i = 0, n = psgV.size(); i < n; i++) {
            PowerSupplyGroup psg = (PowerSupplyGroup) psgV.get(i);
            XmlDataAdaptor psgDA = (XmlDataAdaptor) runnerDA.createChild("PowerSupplyGroup");
            psgDA.setValue("group_name", psg.getName());
            for (int j = 0, nj = psg.getPowerSupplyCyclers().size(); j < nj; j++) {
                PowerSupplyCycler psc = (PowerSupplyCycler) psg.getPowerSupplyCyclers().get(j);
                XmlDataAdaptor pscDA = (XmlDataAdaptor) psgDA.createChild("PowerSupplyCycler");
                XmlDataAdaptor pscSetPV_DA = (XmlDataAdaptor) pscDA.createChild("PV_Set");
                pscSetPV_DA.setValue("pv_name", psc.getChannelName());
                XmlDataAdaptor pscRbPV_DA = (XmlDataAdaptor) pscDA.createChild("PV_Rb");
                pscRbPV_DA.setValue("pv_name", psc.getChannelNameRB());
                XmlDataAdaptor pscMaxCurr_DA = (XmlDataAdaptor) pscDA.createChild("Max_I_Ampers");
                pscMaxCurr_DA.setValue("I", psc.getMaxCurrent());
                XmlDataAdaptor pscnCycl_DA = (XmlDataAdaptor) pscDA.createChild("Number_of_Cycles");
                pscnCycl_DA.setValue("n", psc.getnCycles());
                XmlDataAdaptor pscChangeRate_DA = (XmlDataAdaptor) pscDA.createChild("Rate_Amper_per_sec");
                pscChangeRate_DA.setValue("rate", psc.getChangeRate());
                XmlDataAdaptor pscMinCurrTime_DA = (XmlDataAdaptor) pscDA.createChild("MinI_Time_sec");
                pscMinCurrTime_DA.setValue("time", psc.getMinCurrTime());
                XmlDataAdaptor pscMaxCurrTime_DA = (XmlDataAdaptor) pscDA.createChild("MaxI_Time_sec");
                pscMaxCurrTime_DA.setValue("time", psc.getMaxCurrTime());
                XmlDataAdaptor pscActive_DA = (XmlDataAdaptor) pscDA.createChild("Active");
                pscActive_DA.setValue("isActive", psc.getActive());
            }
        }
    }
}
