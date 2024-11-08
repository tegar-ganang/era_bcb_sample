package gov.sns.apps.mpsinputtest;

import gov.sns.application.*;
import gov.sns.apps.mpsinputtest.MPSFrame;
import gov.sns.apps.mpsinputtest.TestMagPSPanel;
import gov.sns.tools.database.*;
import oracle.sql.*;
import oracle.jdbc.pool.OracleDataSource;
import javax.swing.JOptionPane;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.Cursor;
import java.awt.Color;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.*;
import java.util.List;
import java.sql.*;
import javax.sql.DataSource;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * MPSWindow is the main window for displaying the status of a remote MPS service.
 *
 * @author  tap
 * @author  Delphy Armstrong
 */
class MPSWindow extends XalWindow implements SwingConstants, DataKeys {

    /** Table of MPS tools running on the local network */
    protected JTable mpsTable;

    /** State indicating whether the table has any selected rows */
    protected boolean mpsTableHasSelectedRows;

    /** Action for shutting down the selected service */
    protected Action _shutdownServiceAction;

    public JSplitPane splitPane;

    private JFrame tableFrame = new JFrame();

    private static MPSFrame mainWindow;

    private ChannelAccess ca = new ChannelAccess();

    private DataSource connectionPool;

    private JScrollPane scrollPane = new JScrollPane();

    public static String[][] FaultLogValues = new String[500][10];

    public static int numGoodPvs, numFaultPvs;

    private static String selModeMask;

    private JPanel RTDLPanel = new JPanel();

    private JPanel SwPanel = new JPanel();

    private Font labelFont = new Font("TimesNewRoman", Font.BOLD, 14);

    private Font subLabelFont = new Font("TimesNewRoman", Font.BOLD, 12);

    private JPanel topPanel = new JPanel();

    private FlowLayout topPanelLayout = new FlowLayout();

    private FlowLayout switchPanelLayout = new FlowLayout();

    private static List MebtLLRFwrap, MPSmagWrap, HPRFtripWrap, MEBTmagWrap;

    private static List HPRFwrap, MPSblmWrap;

    private ListSelectionModel listSelectionModel;

    public static int mmIndex;

    public static Map LLRFtest, MEBTmagTest, MagTest, HPRFtest, HPRFtripTest, BLMTest;

    private int switchSel, srcSel;

    public static MPSmmTableModel mpsMMmodel;

    private JLabel btnSel;

    private Box mainView, subView;

    private Box mpsPanel, ChainView, DeviceView, SubSystemView;

    private Box RTDLView, MachView;

    private JPanel btnSelPanel = new JPanel();

    private JPanel switchPanel = new JPanel();

    private JLabel RbmdLabel, RmmLabel, MbmdLabel, MmmLabel, ChainLabel;

    private JLabel DeviceLabel, SubSystemLabel;

    private double[] MEBTBSSumStat, CCLBSSumStat, LDmpSumStat, IDmpSumStat;

    private double[] RingSumStat, EDmpSumStat, TgtSumStat;

    private double[] SwitchBmMd, RTDLBmMd, SwitchMachMd, RTDLMachMd;

    public static int[] MPSHPRFTripRdy = new int[300];

    public static int[] MPSHPRFMPSpvsRdy = new int[300];

    public static int[] MPSLLRFTestRdy = new int[300];

    public static int[] MEBTmagTestRdy = new int[300];

    public static int[] MPSmagTestRdy = new int[300];

    public static int[] MPSHPRFTestRdy = new int[300];

    public static ListSelectionModel lsm;

    private JPanel radioButtonPanel = new JPanel();

    private JRadioButton NoneButton, DeviceButton, SubSystemButton;

    private GridLayout radioButtonPanelLayout = new GridLayout();

    public String[][] MPSblmTestValues = new String[5][300];

    private JButton TestButton;

    private String SelChain;

    public static Map MEBTmag;

    public static Map MPSmag;

    public static Map MPSllrf;

    public static Map MPShprf;

    public static Map MPStripHPRF;

    public static Map MPShprfPVs;

    public static Map isTestedMap, PFstatusMap;

    public static int selectedRow;

    public static TestMagPSPanel testMagPSpanel;

    public static Map jeriMM;

    public Map jeriIOC;

    public Map jeriDevs;

    public Map jeriSubSys;

    public Map jeriChanNo;

    private static String selLbl;

    public static int[] selectedIx = new int[300];

    protected Map arrayDescriptorMap = new HashMap();

    public MPSchainComboBox chainBox;

    public JComboBox deviceBox, subsystemBox;

    public static JTable mpsMMtable;

    public static DefaultTableModel model = new DefaultTableModel();

    public static JLabel[][] MPSDisplayLabels = { { null, null }, { null, null }, { null, null }, { null, null }, { null, null } };

    /** Creates a new instance of MainWindow */
    public MPSWindow(MPSDocument aDocument) {
        super(aDocument);
        setSize(1000, 500);
        mpsTableHasSelectedRows = false;
        if (mainWindow == null) mainWindow = new MPSFrame();
        makeContent();
    }

    /**
	 * Determine whether to display the toolbar.
	 * @return true to display the toolbar and false otherwise.
	 */
    @Override
    public boolean usesToolbar() {
        return false;
    }

    /**
	 * Build the component contents of the window.
	 * @param mpsTableModel The table model for the MPS view
	 */
    protected void makeContent() {
        Object SignalID, SignalMM;
        int retVal = 0;
        Border raisedBdr = BorderFactory.createRaisedBevelBorder();
        Border panelBdr = BorderFactory.createTitledBorder("Selected");
        btnSel = new JLabel("");
        RTDLView = new Box(VERTICAL);
        RbmdLabel = new JLabel(getRTDLBmMd());
        RTDLView.add(RbmdLabel);
        RmmLabel = new JLabel(getRTDLMachMd());
        RTDLView.add(RmmLabel);
        RTDLPanel.setFont(subLabelFont);
        RTDLPanel.setBorder(BorderFactory.createTitledBorder("RTDL"));
        RTDLPanel.add(RTDLView);
        MachView = new Box(VERTICAL);
        MbmdLabel = new JLabel(getSwitchBmMd());
        MachView.add(MbmdLabel);
        MmmLabel = new JLabel(getSwitchMachMd());
        MachView.add(MmmLabel);
        SwPanel.setFont(subLabelFont);
        SwPanel.setBorder(BorderFactory.createTitledBorder("Switch"));
        SwPanel.add(MachView);
        radioButtonPanel.setLayout(radioButtonPanelLayout);
        radioButtonPanelLayout.setRows(3);
        radioButtonPanelLayout.setColumns(1);
        radioButtonPanelLayout.setVgap(2);
        radioButtonPanel.setBorder(BorderFactory.createTitledBorder("Filters"));
        NoneButton = new JRadioButton("None");
        NoneButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deviceBox.removeAllItems();
                subsystemBox.removeAllItems();
            }
        });
        NoneButton.setSelected(true);
        radioButtonPanel.add(NoneButton);
        SubSystemButton = new JRadioButton("SubSystem");
        SubSystemButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SelChain = chainBox.getSelectedItem().toString();
                try {
                    deviceBox.removeAllItems();
                    getSubSystems(SelChain, subsystemBox);
                } catch (java.sql.SQLException ex) {
                    ex.printStackTrace();
                } finally {
                }
            }
        });
        radioButtonPanel.add(SubSystemButton);
        DeviceButton = new JRadioButton("Device");
        DeviceButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SelChain = chainBox.getSelectedItem().toString();
                try {
                    subsystemBox.removeAllItems();
                    getDevices(SelChain, deviceBox);
                } catch (java.sql.SQLException ex) {
                    ex.printStackTrace();
                } finally {
                }
            }
        });
        radioButtonPanel.add(DeviceButton);
        ButtonGroup group = new ButtonGroup();
        group.add(NoneButton);
        group.add(DeviceButton);
        group.add(SubSystemButton);
        switchPanel.setLayout(switchPanelLayout);
        switchPanel.add(radioButtonPanel);
        switchPanel.add(RTDLPanel);
        switchPanel.add(SwPanel);
        btnSelPanel.setLayout(switchPanelLayout);
        btnSelPanel.add(btnSel);
        subView = new Box(HORIZONTAL);
        mainView = new Box(VERTICAL);
        getContentPane().add(mainView);
        mpsPanel = new Box(HORIZONTAL);
        selLbl = " ";
        btnSel.setForeground(new Color(0x000000));
        btnSel.setText(selLbl);
        JTable tmpMMtable;
        tmpMMtable = new JTable(model);
        tmpMMtable.setModel(model);
        tmpMMtable.setVisible(false);
        scrollPane.getViewport().add(tmpMMtable, null);
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        topPanel.setLayout(topPanelLayout);
        makeMPSInspectorBtns(topPanel);
        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(scrollPane);
        splitPane.setResizeWeight(0);
        mpsPanel.add(splitPane);
        mainView.add(btnSelPanel);
        mainView.add(switchPanel);
        mainView.add(mpsPanel);
    }

    public JTable getMPSTable() {
        if (mpsMMtable == null) {
            String chainSel = chainBox.getSelectedItem();
            String filterSel = "";
            int filterItem = 1;
            if (SubSystemButton.isSelected()) {
                filterItem = 2;
                filterSel = (String) subsystemBox.getSelectedItem();
            } else if (DeviceButton.isSelected()) {
                filterItem = 3;
                filterSel = (String) deviceBox.getSelectedItem();
            }
            if (mpsMMmodel != null) mpsMMmodel.clear();
            mpsMMtable = makeMPSTable(chainSel, filterSel, filterItem, 0);
            mpsMMtable.validate();
        }
        return mpsMMtable;
    }

    public JTable clearMPSTable(MPSmmTableModel mpsMMmodel) {
        mpsMMtable = new JTable(mpsMMmodel);
        mpsMMtable.setAutoCreateColumnsFromModel(false);
        if (mpsMMtable.getColumnCount() <= 0) return mpsMMtable;
        mpsMMtable.getColumnModel().getColumn(0).setHeaderValue("Signal");
        mpsMMtable.getColumnModel().getColumn(0).setPreferredWidth(100);
        mpsMMtable.getColumnModel().getColumn(1).setHeaderValue("Subsystem");
        mpsMMtable.getColumnModel().getColumn(1).setPreferredWidth(30);
        mpsMMtable.getColumnModel().getColumn(2).setHeaderValue("Device");
        mpsMMtable.getColumnModel().getColumn(2).setPreferredWidth(30);
        mpsMMtable.getColumnModel().getColumn(3).setHeaderValue("IOC");
        mpsMMtable.getColumnModel().getColumn(4).setHeaderValue("Channel Number");
        mpsMMtable.getColumnModel().getColumn(4).setPreferredWidth(30);
        mpsMMtable.getColumnModel().getColumn(5).setHeaderValue("Tested ?");
        mpsMMtable.getColumnModel().getColumn(5).setPreferredWidth(30);
        mpsMMtable.getColumnModel().getColumn(6).setHeaderValue("Ready to Test ?");
        mpsMMtable.getColumnModel().getColumn(6).setPreferredWidth(30);
        mpsMMtable.setShowVerticalLines(true);
        mpsMMtable.setShowHorizontalLines(false);
        mpsMMtable.setColumnSelectionAllowed(false);
        mpsMMtable.setSelectionForeground(Color.white);
        mpsMMtable.setSelectionBackground(Color.blue);
        String deviceSel = (String) deviceBox.getSelectedItem();
        if (DeviceButton.isSelected() && deviceSel.equals("BLM")) mpsMMtable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); else mpsMMtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mpsMMtable.setRowSelectionAllowed(true);
        mpsMMtable.setVisible(true);
        mpsMMtable.validate();
        return mpsMMtable;
    }

    public JTable makeMPSTable(String MachMode, String mpsFilter, int filterNo, int isNew) {
        String filterSel;
        String chainSel = chainBox.getSelectedItem();
        String subsysSel = (String) subsystemBox.getSelectedItem();
        String deviceSel = (String) deviceBox.getSelectedItem();
        Iterator m1;
        Map.Entry p1;
        if (isNew == 1) {
            if (filterNo == 1) jeriMM = reloadSignals(MachMode, isNew); else if (filterNo == 2) {
                filterSel = (String) subsystemBox.getSelectedItem();
                jeriMM = reloadSubSystems(MachMode, filterSel, isNew);
            } else {
                filterSel = (String) deviceBox.getSelectedItem();
                jeriMM = reloadDevices(MachMode, filterSel, isNew);
            }
            jeriIOC = mainWindow.reloadIOC();
            jeriDevs = mainWindow.getDeviceMap();
            jeriSubSys = mainWindow.getSubSystemMap();
            jeriChanNo = mainWindow.getChannelNoMap();
            isTestedMap = mainWindow.getIsTested_Map();
            PFstatusMap = mainWindow.getPFstatus_Map();
        }
        Iterator keyValue = jeriMM.keySet().iterator();
        while (keyValue.hasNext()) {
            Object key = keyValue.next();
            if (key.toString().indexOf("_Mag:PS_DC") != -1 || key.toString().indexOf("_DCH") != -1 || key.toString().indexOf("_Mag:DCH") != -1) {
                jeriMM.remove(key);
                jeriIOC.remove(key);
                jeriDevs.remove(key);
                jeriSubSys.remove(key);
                jeriChanNo.remove(key);
                isTestedMap.remove(key);
                PFstatusMap.remove(key);
                keyValue = jeriMM.keySet().iterator();
            }
        }
        mpsMMmodel = new MPSmmTableModel(jeriMM, jeriDevs, jeriIOC, jeriChanNo, isTestedMap, PFstatusMap);
        mpsMMmodel.addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                mpsTableModel_tableChanged(e);
            }
        });
        mpsMMtable = clearMPSTable(mpsMMmodel);
        mpsMMtable.setAutoCreateColumnsFromModel(false);
        mpsMMtable.setShowVerticalLines(true);
        mpsMMtable.setShowHorizontalLines(false);
        mpsMMtable.setColumnSelectionAllowed(false);
        mpsMMtable.setSelectionForeground(Color.white);
        mpsMMtable.setSelectionBackground(Color.blue);
        deviceSel = (String) deviceBox.getSelectedItem();
        if (DeviceButton.isSelected() && deviceSel.equals("BLM")) mpsMMtable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); else mpsMMtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mpsMMtable.setRowSelectionAllowed(true);
        mpsMMtable.setVisible(true);
        ListSelectionModel rowSM = mpsMMtable.getSelectionModel();
        rowSM.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                lsm = (ListSelectionModel) e.getSource();
                if (lsm.isSelectionEmpty()) {
                    selectedRow = -1;
                } else {
                    selectedRow = lsm.getMinSelectionIndex();
                }
            }
        });
        mpsMMtable.validate();
        return mpsMMtable;
    }

    public String getSelChain() {
        return chainBox.getSelectedItem().toString();
    }

    /**
	 * Make an inspector for a selected MPS service
	 * @return the inspector view
	 */
    protected void makeMPSInspectorBtns(JPanel tPanel) {
        switchSel = getSwitchBmMdVal();
        srcSel = getSwitchMachMdVal();
        mmIndex = switchSel + (srcSel * 5);
        ChainView = new Box(HORIZONTAL);
        ChainLabel = new JLabel("CHAINS");
        DeviceView = new Box(HORIZONTAL);
        DeviceLabel = new JLabel("DEVICES");
        deviceBox = new JComboBox();
        SubSystemView = new Box(HORIZONTAL);
        SubSystemLabel = new JLabel("SUBSYSTEMS");
        subsystemBox = new JComboBox();
        chainBox = new MPSchainComboBox(deviceBox, subsystemBox, NoneButton);
        ChainView.add(ChainLabel);
        ChainView.add(Box.createRigidArea(new Dimension(10, 0)));
        ChainView.add(chainBox.getComboBox());
        ChainView.add(Box.createRigidArea(new Dimension(10, 0)));
        DeviceView.add(DeviceLabel);
        DeviceView.add(Box.createRigidArea(new Dimension(10, 0)));
        DeviceView.add(deviceBox);
        DeviceView.add(Box.createRigidArea(new Dimension(10, 0)));
        SubSystemView.add(SubSystemLabel);
        SubSystemView.add(Box.createRigidArea(new Dimension(10, 0)));
        SubSystemView.add(subsystemBox);
        SubSystemView.add(Box.createRigidArea(new Dimension(10, 0)));
        subView.add(ChainView);
        subView.add(DeviceView);
        subView.add(SubSystemView);
        Action ExecuteAction = new AbstractAction("Generate") {

            public void actionPerformed(ActionEvent evt) {
                {
                    ExecuteButton_actionPerformed(evt);
                }
            }
        };
        subView.add(new JButton(ExecuteAction));
        subView.add(Box.createRigidArea(new Dimension(10, 0)));
        Action TestAction = new AbstractAction("Test") {

            public void actionPerformed(final ActionEvent evt) {
                {
                    final Runnable updateLabels = new Runnable() {

                        public void run() {
                            TestButton_actionPerformed(evt);
                        }
                    };
                    Thread appThread = new Thread() {

                        @Override
                        public void run() {
                            try {
                                SwingUtilities.invokeLater(updateLabels);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    appThread.start();
                }
            }
        };
        subView.add(new JButton(TestAction));
        subView.add(Box.createRigidArea(new Dimension(10, 0)));
        tPanel.add(subView);
        return;
    }

    /**
	 * Convenience method for getting the document as an instance of HistoryDocument.
	 * @return The document cast as an instace of HistoryDocument.
	 */
    public MPSDocument getDocument() {
        return (MPSDocument) document;
    }

    /**
     * Register actions specific to this window instance. 
     * @param commander The commander with which to register the custom commands.
     */
    @Override
    protected void customizeCommands(Commander commander) {
        _shutdownServiceAction = new AbstractAction("shutdown-service") {

            public void actionPerformed(ActionEvent event) {
                final String message = "Are you sure you want to shutdown the selected service?";
                int result = JOptionPane.showConfirmDialog(MPSWindow.this, message, "Careful!", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                }
            }
        };
        _shutdownServiceAction.setEnabled(false);
    }

    public Map reloadSignals(String MachMode, int isNew) {
        Map MpsModeMask = null;
        int srcSel = 0;
        try {
            MpsModeMask = mainWindow.loadDefaults(mainWindow.getConnection(), MachMode, isNew);
        } catch (java.sql.SQLException ex) {
            ex.printStackTrace();
        } finally {
        }
        try {
            isTestedMap = mainWindow.loadIsTested(mainWindow.getConnection(), PFstatusMap);
        } catch (java.sql.SQLException ex) {
            ex.printStackTrace();
        } finally {
        }
        return MpsModeMask;
    }

    public Map reloadDevices(String MachMode, String DeviceSel, int isNew) {
        Map MpsDevices = null;
        int srcSel = 0;
        try {
            MpsDevices = mainWindow.loadDevices(mainWindow.getConnection(), MachMode, DeviceSel, isNew);
        } catch (java.sql.SQLException ex) {
            ex.printStackTrace();
        } finally {
        }
        try {
            isTestedMap = mainWindow.loadIsTested(mainWindow.getConnection(), PFstatusMap);
        } catch (java.sql.SQLException ex) {
            ex.printStackTrace();
        } finally {
        }
        return MpsDevices;
    }

    public Map reloadSubSystems(String MachMode, String SubSysSel, int isNew) {
        Map MpsSubSys = null;
        int srcSel = 0;
        try {
            MpsSubSys = mainWindow.loadSubSystems(mainWindow.getConnection(), MachMode, SubSysSel, isNew);
        } catch (java.sql.SQLException ex) {
            ex.printStackTrace();
        } finally {
        }
        try {
            isTestedMap = mainWindow.loadIsTested(mainWindow.getConnection(), PFstatusMap);
        } catch (java.sql.SQLException ex) {
            ex.printStackTrace();
        } finally {
        }
        return MpsSubSys;
    }

    public void setupModel(int retIndex, JTable mmTable) {
        scrollPane.getViewport().add(mmTable, null);
    }

    public Map getMEBTmagRdyToTestPVs(String initPV) {
        TestMEBTMagPSPanel newPanel = new TestMEBTMagPSPanel();
        MEBTmagWrap = newPanel.getMebtMagWrap();
        return newPanel.getRdyToTestMebtMag(initPV);
    }

    public int[] getRdyToTestHPRF(String pv) {
        String[][] MPStestValues = { { "", "", "", "", "", "" }, { "", "", "", "", "", "" }, { "", "", "", "", "", "" } };
        ChannelWrapper wrapper;
        ContMPSHPRFPanel newPanel = new ContMPSHPRFPanel();
        HPRFtest = newPanel.getRdyToTestHPRF(pv);
        HPRFwrap = newPanel.getHPRFwrap();
        Iterator iter = HPRFwrap.iterator();
        int i = 0;
        String hprfpv;
        while (iter.hasNext()) {
            wrapper = (ChannelWrapper) iter.next();
            hprfpv = wrapper.getName();
            MPStestValues[0][i] = hprfpv;
            MPSHPRFTestRdy[i] = wrapper.getValue();
            i++;
            if (i > 5) i = 0;
        }
        return MPSHPRFTestRdy;
    }

    public int[] getRdyToTestHPRFTrips(String pv) {
        String[][] MPStestValues = { { "", "", "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "", "", "" } };
        ChannelWrapper wrapper;
        TestMPSHPRFPanel newPanel = new TestMPSHPRFPanel();
        if (HPRFtripTest == null) {
            HPRFtripTest = newPanel.getRdyToTestHPRFtrip(pv);
        }
        HPRFtripWrap = newPanel.getHPRFtripWrap();
        Iterator iter = HPRFtripWrap.iterator();
        int i = 0;
        String rftrippv;
        while (iter.hasNext()) {
            wrapper = (ChannelWrapper) iter.next();
            rftrippv = wrapper.getName();
            if (rftrippv.lastIndexOf(":Flt_VSWR_PA") > -1) i = 0; else if (rftrippv.lastIndexOf(":Flt_VSWR_IPA") > -1) i = 1; else if (rftrippv.lastIndexOf(":Flt_Gnd") > -1) i = 2; else if (rftrippv.lastIndexOf(":Flt_PW") > -1) i = 3; else if (rftrippv.lastIndexOf(":Flt_PA") > -1) i = 4; else if (rftrippv.lastIndexOf(":Flt_IPA") > -1) i = 5; else if (rftrippv.lastIndexOf(":Flt_OT_Stk") > -1) i = 6; else if (rftrippv.lastIndexOf(":Flt_OT_Cab") > -1) i = 7; else if (rftrippv.lastIndexOf(":FPAR_MEBT_BS_cable_status") > -1) i = 8; else if (rftrippv.lastIndexOf(":FPAR_MEBT_BS_chan_status") > -1) i = 9;
            MPStestValues[0][i] = rftrippv;
            MPSHPRFTripRdy[i] = wrapper.getValue();
        }
        return MPSHPRFTripRdy;
    }

    public Map getRdyToTestLLRF(String initPvStr, String chainSel) {
        String SrcChain = chainBox.getSelectedItem();
        TestMPSLLRFPanel newPanel = new TestMPSLLRFPanel();
        MebtLLRFwrap = newPanel.getMebtLLRFwrap();
        return newPanel.getRdyToTestLLRF(initPvStr, chainSel, SrcChain);
    }

    public Map getRdyToTestPVs(String initPvStr) {
        TestMagPSPanel newPanel = new TestMagPSPanel();
        MPSmagWrap = newPanel.getMPSmagWrap(initPvStr);
        return newPanel.getRdyToTestMPSmag(initPvStr);
    }

    public Map getRdyToTestBLMPVs(String initPvStr) {
        TestBLMPanel newPanel = new TestBLMPanel();
        MPSblmWrap = newPanel.getMPSblmWrap(initPvStr);
        return newPanel.getRdyToTestMPSblm(initPvStr);
    }

    public Map getRdyToTestAllBLMPVs(String initPvStr) {
        TestAllBLMsPanel allBLMpanel = new TestAllBLMsPanel();
        MPSblmWrap = allBLMpanel.getMPSblmWrap(initPvStr);
        return allBLMpanel.getRdyToTestMPSblm(initPvStr);
    }

    int getSwitchBmMdVal() {
        SwitchBmMd = ca.getSwitchBmMdPVs();
        if (SwitchBmMd[0] == 0.0) return 0; else if (SwitchBmMd[1] == 0.0) return 1; else if (SwitchBmMd[2] == 0.0) return 2; else if (SwitchBmMd[3] == 0.0) return 3; else if (SwitchBmMd[4] == 0.0) return 4; else if (SwitchBmMd[5] == 0.0) return -1; else if (SwitchBmMd[6] == 0.0) return -2; else if (SwitchBmMd[7] == 0.0) return -1; else return -1;
    }

    String getSwitchBmMd() {
        SwitchBmMd = ca.getSwitchBmMdPVs();
        if (SwitchBmMd[0] == 0.0) return "10uSec"; else if (SwitchBmMd[1] == 0.0) return "50uSec"; else if (SwitchBmMd[2] == 0.0) return "100uSec"; else if (SwitchBmMd[3] == 0.0) return "1mSec"; else if (SwitchBmMd[4] == 0.0) return "FullPwr"; else if (SwitchBmMd[5] == 0.0) return "Off"; else if (SwitchBmMd[6] == 0.0) return "StandBy"; else if (SwitchBmMd[7] == 0.0) return "MPSTest"; else return "???";
    }

    String getRTDLBmMd() {
        RTDLBmMd = ca.getRTDLBmMdPVs();
        if (RTDLBmMd[0] == 1.0) return "10uSec"; else if (RTDLBmMd[1] == 1.0) return "50uSec"; else if (RTDLBmMd[2] == 1.0) return "100uSec"; else if (RTDLBmMd[3] == 1.0) return "1mSec"; else if (RTDLBmMd[4] == 1.0) return "FullPwr"; else if (RTDLBmMd[5] == 1.0) return "Off"; else if (RTDLBmMd[6] == 1.0) return "StandBy"; else if (RTDLBmMd[7] == 1.0) return "MPSTest"; else return "???";
    }

    String getSwitchMachMd() {
        SwitchMachMd = ca.getSwitchMachMdPVs();
        if (SwitchMachMd[0] == 0.0) return "MEBT_BS"; else if (SwitchMachMd[1] == 0.0) return "CCL_BS"; else if (SwitchMachMd[2] == 0.0) return "LDmp"; else if (SwitchMachMd[3] == 0.0) return "IDmp"; else if (SwitchMachMd[4] == 0.0) return "Ring"; else if (SwitchMachMd[5] == 0.0) return "EDmp"; else if (SwitchMachMd[6] == 0.0) return "Tgt"; else return "???";
    }

    int getSwitchMachMdVal() {
        SwitchMachMd = ca.getSwitchMachMdPVs();
        if (SwitchMachMd[0] == 0.0) return 0; else if (SwitchMachMd[1] == 0.0) return 1; else if (SwitchMachMd[2] == 0.0) return 2; else if (SwitchMachMd[3] == 0.0) return 3; else if (SwitchMachMd[4] == 0.0) return 4; else if (SwitchMachMd[5] == 0.0) return 5; else if (SwitchMachMd[6] == 0.0) return 6; else return -1;
    }

    String getRTDLMachMd() {
        RTDLMachMd = ca.getRTDLMachMdPVs();
        if (RTDLMachMd[0] == 1.0) return "MEBT_BS"; else if (RTDLMachMd[1] == 1.0) return "CCL_BS"; else if (RTDLMachMd[2] == 1.0) return "LDmp"; else if (RTDLMachMd[3] == 1.0) return "IDmp"; else if (RTDLMachMd[4] == 1.0) return "Ring"; else if (RTDLMachMd[5] == 1.0) return "EDmp"; else if (RTDLMachMd[6] == 1.0) return "Tgt"; else return "???";
    }

    void ExecuteButton_actionPerformed(ActionEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        int isnew = 1;
        makeMPStable(isnew);
        setCursor(Cursor.getDefaultCursor());
    }

    public JTable makeMPStable(int isNew) {
        String chainSel = chainBox.getSelectedItem();
        String filterSel = "";
        int filterItem = 1;
        if (SubSystemButton.isSelected()) {
            filterItem = 2;
            filterSel = (String) subsystemBox.getSelectedItem();
        } else if (DeviceButton.isSelected()) {
            filterItem = 3;
            filterSel = (String) deviceBox.getSelectedItem();
        }
        selLbl = chainSel + " " + filterSel;
        btnSel.setText(selLbl);
        lsm = null;
        if (mpsMMmodel != null && isNew > 0) mpsMMmodel.clear();
        mpsMMtable = makeMPSTable(chainSel, filterSel, filterItem, isNew);
        mpsMMtable.validate();
        if (mpsMMtable.getRowCount() > 0) {
            scrollPane.getViewport().add(mpsMMtable, null);
            setupModel(jeriMM.size(), mpsMMtable);
        } else {
            mpsMMtable = clearMPSTable(mpsMMmodel);
            scrollPane.getViewport().add(mpsMMtable, null);
        }
        return mpsMMtable;
    }

    void TestButton_actionPerformed(ActionEvent e) {
        String chainSel = chainBox.getSelectedItem();
        String subsysSel = (String) subsystemBox.getSelectedItem();
        String deviceSel = (String) deviceBox.getSelectedItem();
        String selTwo;
        int row;
        if (subsysSel != null) selTwo = subsysSel; else if (deviceSel != null) selTwo = deviceSel; else selTwo = "";
        mpsMMtable = getMPSTable();
        row = mpsMMtable.getSelectedRow();
        if (row < 0) {
            row = checkRowSelection();
        }
        if (chainSel.equals("MEBT_BS") && ((subsysSel != null && subsysSel.equals("Mag")) || (deviceSel != null && deviceSel.equals("PS")))) testMEBTMag(selTwo, row); else if (deviceSel != null && deviceSel.equals("BLM")) testMPSblm(selTwo, row); else if ((chainSel.equals("CCL_BS") || chainSel.equals("LDmp")) && ((subsysSel != null && subsysSel.equals("Mag")) || (deviceSel != null && deviceSel.equals("PS")))) testMPSmag(selTwo, row, chainSel); else if ((chainSel.equals("MEBT_BS") || chainSel.equals("CCL_BS") || chainSel.equals("LDmp")) && ((subsysSel != null && subsysSel.equals("LLRF")) || (deviceSel != null && deviceSel.equals("HPM")))) testMPSLLRF(selTwo, row); else if (chainSel.equals("MEBT_BS") && ((subsysSel != null && subsysSel.equals("RF")) || (deviceSel != null && deviceSel.equals("Bnch")))) testMPSHPRF(selTwo, row);
    }

    void testMEBTMag(String selTwo, int row) {
        String[] fullnames = { "Fault = OK", "Sts_AC = OK", "Ilk = OK", "Sts_OVR = OK", "Sts_Rdy = ON", "Sts_Rem = REM", "Sts_H2O = 1", "Enable = ENA", "FPL_MEBT_BS_cable_status = CONNECTED" };
        String[] names = { "Fault", "Sts_AC", "Ilk", "Sts_OVR", "Sts_Rdy", "Sts_Rem", "Sts_H2O", "Enable", "FPL_MEBT_BS_cable_status" };
        JLabel name = new JLabel(names[1]);
        if (row < 0) {
            JOptionPane.showMessageDialog(mainWindow, "Please select a row from the table to test.", "No Selection Made", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String boxTitle = (String) mpsMMtable.getValueAt(row, 0);
        String[][] MPStestValues = { { "", "", "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "", "", "" } };
        getMEBTmagRdyToTestPVs(boxTitle);
        ListDialog m_dialog = ListDialog.showDialog(this, TestButton, boxTitle, "MPS Test", MPStestValues, name.getText(), SelChain, selTwo);
    }

    void testMPSmag(String selTwo, int row, String selChain) {
        String[] names = { "FltS", "FPL_" + selChain + "_chan_status", "FPL_" + selChain + "_input_status", "FPL_" + selChain + "_cable_status", "CCmd" };
        JLabel name = new JLabel(names[1]);
        if (row < 0) {
            JOptionPane.showMessageDialog(mainWindow, "Please select a row from the table to test.", "No Selection Made", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String boxTitle = (String) mpsMMtable.getValueAt(row, 0);
        String[][] MPStestValues = { { "", "", "", "", "" }, { "", "", "", "", "" }, { "", "", "", "", "" }, { "", "", "", "", "" }, { "", "", "", "", "" } };
        getRdyToTestPVs(boxTitle);
        String str;
        TestMagPSPanel newPanel = new TestMagPSPanel();
        if (MagTest == null) {
            MagTest = newPanel.getRdyToTestMPSmag(boxTitle);
        }
        MPSmagWrap = newPanel.getMPSmagWrap(boxTitle);
        Iterator iter = MPSmagWrap.iterator();
        ChannelWrapper wrapper;
        int i = 0;
        String magpv;
        while (iter.hasNext()) {
            wrapper = (ChannelWrapper) iter.next();
            magpv = wrapper.getName();
            str = "" + wrapper.getValue();
            MPStestValues[0][i] = str;
            MPStestValues[1][i] = magpv;
            i++;
        }
        String dummy = "";
        ListDialog m_dialog = ListDialog.showDialog(TestButton, this, boxTitle, "MPS Test", MPStestValues, name.getText(), SelChain, selTwo, dummy);
    }

    void testMPSblm(final String selTwo, int row) {
        String boxTitle;
        String bTitle = "";
        selectedIx = mpsMMtable.getSelectedRows();
        if (row < 0) {
            JOptionPane.showMessageDialog(mainWindow, "Please select a row from the table to test.", "No Selection Made", JOptionPane.ERROR_MESSAGE);
            return;
        }
        final int numSel = selectedIx.length;
        if (numSel == 0) {
            JOptionPane.showMessageDialog(mainWindow, "Please press the Generate button again.", "Selection Not Read", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (numSel == 1) {
            int sel = selectedIx[0];
            bTitle = (String) mpsMMtable.getValueAt(sel, 0);
            TestBLMPanel newPanel = new TestBLMPanel();
            boxTitle = (String) mpsMMtable.getValueAt(row, 0);
            getRdyToTestBLMPVs(boxTitle);
            BLMTest = newPanel.getRdyToTestMPSblm(boxTitle);
            MPSblmWrap = newPanel.getMPSblmWrap(boxTitle);
            Iterator iter = MPSblmWrap.iterator();
            ChannelWrapper wrapper;
            String blmpv, str;
            if (iter.hasNext()) {
                wrapper = (ChannelWrapper) iter.next();
                blmpv = wrapper.getName();
                str = "" + wrapper.getValue();
                MPSblmTestValues[0][0] = str;
                MPSblmTestValues[1][0] = blmpv;
            }
            ListDialog m_dialog = ListDialog.showBLMDialog(TestButton, this, bTitle, "MPS Test", MPSblmTestValues, SelChain, selTwo, numSel);
            m_dialog.dispose();
        } else {
            int firstSelIx = mpsMMtable.getSelectedRow();
            boxTitle = (String) mpsMMtable.getValueAt(row, 0);
            String str;
            final TestAllBLMsPanel allBLMpanel = new TestAllBLMsPanel();
            getRdyToTestAllBLMPVs(boxTitle);
            BLMTest = allBLMpanel.getRdyToTestMPSblm(boxTitle);
            MPSblmWrap = allBLMpanel.getMPSblmWrap(boxTitle);
            Iterator iter = MPSblmWrap.iterator();
            ChannelWrapper wrapper;
            int j = 0;
            String blmpv;
            while (iter.hasNext()) {
                wrapper = (ChannelWrapper) iter.next();
                blmpv = wrapper.getName();
                str = "" + wrapper.getValue();
                MPSblmTestValues[0][j] = str;
                MPSblmTestValues[1][j] = blmpv;
                j++;
            }
            ListDialog m_dialog = ListDialog.showBLMDialog(TestButton, this, bTitle, "MPS Test", MPSblmTestValues, SelChain, selTwo, numSel);
            m_dialog.dispose();
            final JTable mpsMMtable = getMPSTable();
            selectedIx = mpsMMtable.getSelectedRows();
            final Component locationComp = this;
            final SwingWorker worker = new SwingWorker() {

                @Override
                public Object construct() {
                    String lbltxt = (String) mpsMMtable.getValueAt(selectedIx[0], 0);
                    allBLMpanel.TestStart();
                    for (int i = 0; i < numSel; i++) {
                        lbltxt = (String) mpsMMtable.getValueAt(selectedIx[i], 0);
                        allBLMpanel.createChannelWrappers(lbltxt);
                    }
                    allBLMpanel.initPLLwrappers();
                    for (int i = 0; i < numSel; i++) {
                        lbltxt = (String) mpsMMtable.getValueAt(selectedIx[i], 0);
                        allBLMpanel.createPLLwrappers(lbltxt);
                    }
                    for (int i = 0; i < numSel; i++) {
                        lbltxt = (String) mpsMMtable.getValueAt(selectedIx[i], 0);
                        Frame frame = JOptionPane.getFrameForComponent(TestButton);
                        allBLMpanel.createChannelWrappers(lbltxt);
                        allBLMpanel.updatePanel(frame, locationComp, MPSblmTestValues, "MPS Test", lbltxt, SelChain, selTwo);
                    }
                    allBLMpanel.TestDone();
                    return 1;
                }
            };
            worker.start();
        }
    }

    void testMPSLLRF(String selTwo, int row) {
        String[] names = { "Flt10", "_cable_status" };
        JLabel name = new JLabel(names[1]);
        if (row < 0) {
            JOptionPane.showMessageDialog(mainWindow, "Please select a row from the table to test.", "No Selection Made", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String boxTitle = (String) mpsMMtable.getValueAt(row, 0);
        String[][] MPStestValues = { { "", "", "" }, { "", "", "" }, { "", "", "" }, { "", "" }, { "", "", "" } };
        LLRFtest = getRdyToTestLLRF(boxTitle, SelChain);
        Iterator iter = MebtLLRFwrap.iterator();
        int i = 0;
        String llrfpv;
        ChannelWrapper wrapper;
        while (iter.hasNext()) {
            wrapper = (ChannelWrapper) iter.next();
            llrfpv = wrapper.getName();
            MPStestValues[0][i] = String.valueOf(wrapper.getValue());
            MPSLLRFTestRdy[i] = wrapper.getValue();
            i++;
        }
        ListDialog m_dialog = ListDialog.showLLRFDialog(this, TestButton, boxTitle, "MPS Test", MPStestValues, name.getText(), SelChain, selTwo);
    }

    void testMPSHPRF(String selTwo, int row) {
        String[] names = { "Sts_AC", "Sts_Cool", "Enable", "Lcl ", "FPL_MEBT_BS_cable_status" };
        JLabel name = new JLabel(names[1]);
        if (row < 0) {
            JOptionPane.showMessageDialog(mainWindow, "Please select a row from the table to test.", "No Selection Made", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String boxTitle = (String) mpsMMtable.getValueAt(row, 0);
        if (boxTitle.lastIndexOf("Bnch") < 0) {
            JOptionPane.showMessageDialog(mainWindow, "Please select a Rebuncher row from the table to test.", "Invalid Selection Made", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String[][] MPStestValues = { { "", "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "", "" } };
        getRdyToTestHPRF(boxTitle);
        Iterator iter = HPRFwrap.iterator();
        int i = 0;
        String llrfpv;
        ChannelWrapper wrapper;
        while (iter.hasNext()) {
            wrapper = (ChannelWrapper) iter.next();
            llrfpv = wrapper.getName();
            MPStestValues[0][i] = String.valueOf(wrapper.getValue());
            MPSLLRFTestRdy[i] = wrapper.getValue();
            i++;
            if (i > 5) i = 0;
        }
        getRdyToTestHPRFTrips(boxTitle);
        for (i = 0; i < 9; i++) MPStestValues[0][i] = String.valueOf(MPSHPRFTestRdy[i]);
        ListDialog m_dialog = ListDialog.showHPRFDialog(this, TestButton, boxTitle, "MPS Test", MPStestValues, SelChain, selTwo);
    }

    void clearModel() {
        if (mpsMMmodel != null) mpsMMmodel.clear();
    }

    /**
   * Called when the selected item in the filter selection changes. 
   */
    private void radioButtonPanel_itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            System.out.println(e.getItem().toString());
        }
    }

    public void updateViews() {
        RbmdLabel.setText(getRTDLBmMd());
        RmmLabel.setText(getRTDLMachMd());
        MbmdLabel.setText(getSwitchBmMd());
        MmmLabel.setText(getSwitchMachMd());
        RbmdLabel.validate();
        RmmLabel.validate();
        MbmdLabel.validate();
        MmmLabel.validate();
        switchPanel.validate();
    }

    /**
	 * Get the array descriptor for the specified array type
	 * @param type An SQL array type
	 * @param connection A database connection
	 * @return the array descriptor for the array type
	 * @throws java.sql.SQLException if a database exception is thrown
	 */
    private ArrayDescriptor getArrayDescriptor(final String type, final Connection connection) throws SQLException {
        if (arrayDescriptorMap.containsKey(type)) {
            return (ArrayDescriptor) arrayDescriptorMap.get(type);
        } else {
            ArrayDescriptor descriptor = ArrayDescriptor.createDescriptor(type, connection);
            arrayDescriptorMap.put(type, descriptor);
            return descriptor;
        }
    }

    /**
	 * Get an SQL Array given an SQL array type, connection and a primitive array
	 * @param type An SQL array type identifying the type of array
	 * @param connection An SQL connection
	 * @param array The primitive Java array
	 * @return the SQL array which wraps the primitive array
	 * @throws gov.sns.tools.database.DatabaseException if a database exception is thrown
	 */
    public Array getArray(String type, Connection connection, Object array) throws DatabaseException {
        try {
            final ArrayDescriptor descriptor = getArrayDescriptor(type, connection);
            return new ARRAY(descriptor, connection, array);
        } catch (java.sql.SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainWindow, "Exception generating an SQL array.", "SQL Error", JOptionPane.ERROR_MESSAGE);
        } finally {
        }
        return null;
    }

    /** Publish the test results.  */
    public void publishMPSinputTestResultsToLogbook(String labelText, String TestResult, String chainSel, String result) {
        try {
            StringBuffer summary = new StringBuffer();
            if (TestResult.length() == 0) TestResult = "Failed";
            String title = "MPS Input Test for " + labelText + " " + TestResult;
            if (title.lastIndexOf("Failed") > -1) summary.append(title + " " + result + "\n"); else summary.append(title + "\n");
            sendToMPS_CHAN_AUDIT(title, TestResult, labelText, chainSel);
            MPSinputElogUtil(title, summary.toString(), mainWindow);
        } catch (Exception exception) {
            System.err.println("Exception while publishing MPS input test results to logbook: " + exception);
            exception.printStackTrace();
        }
    }

    public void publishMPSinputTestResultsToLogbook(String labelText, String TestResult, String chainSel) {
        try {
            StringBuffer summary = new StringBuffer();
            if (TestResult.length() == 0) TestResult = "Failed";
            String title = "MPS Input Test for " + labelText + " " + TestResult;
            if (title.lastIndexOf("Failed") > -1) summary.append(title + "\n"); else summary.append(title + "\n");
            sendToMPS_CHAN_AUDIT(title, TestResult, labelText, chainSel);
            MPSinputElogUtil(title, summary.toString(), mainWindow);
        } catch (Exception exception) {
            System.err.println("Exception while publishing MPS input test results to logbook: " + exception);
            exception.printStackTrace();
        }
    }

    public void MPSinputElogUtil(String title, String summary, MPSFrame mainWindow) {
        try {
            sendToElog(title, summary);
            sendEmail(title, summary, "nypaverdj@ornl.gov");
        } catch (java.sql.SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainWindow, ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
        } finally {
        }
    }

    private void getDevices(String chainSel, JComboBox deviceBox) throws java.sql.SQLException {
        Connection oracleConnection = mainWindow.getConnection();
        try {
            StringBuffer sql = new StringBuffer("SELECT DISTINCT DVC.DVC_TYPE_ID ");
            sql.append("FROM EPICS.DVC, EPICS.MACHINE_MODE, EPICS.MPS_SGNL_PARAM ");
            sql.append("WHERE ((MPS_SGNL_PARAM.DVC_ID =  MACHINE_MODE.DVC_ID) AND (");
            sql.append("MPS_SGNL_PARAM.APPR_DTE = MACHINE_MODE.APPR_DTE) AND (MACHINE_MODE.MPS_DVC_ID = ");
            if (chainSel.equals("All Chains")) {
                sql.append("DVC.DVC_ID)");
                sql.append(") ORDER BY DVC.DVC_TYPE_ID");
            } else {
                sql.append("DVC.DVC_ID) AND MPS_SGNL_PARAM.MPS_CHAIN_ID = '");
                sql.append(chainSel);
                sql.append("') ORDER BY DVC.DVC_TYPE_ID");
            }
            PreparedStatement query = oracleConnection.prepareStatement(sql.toString());
            deviceBox.removeAllItems();
            ResultSet result = null;
            try {
                result = query.executeQuery();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Unable to connect to database.", "Timeout?", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
            if (result != null) {
                try {
                    while (result.next()) {
                        deviceBox.addItem(result.getString(1));
                    }
                } finally {
                    result.close();
                    query.close();
                }
            }
        } finally {
        }
    }

    private void getSubSystems(String chainSel, JComboBox subsystemBox) throws java.sql.SQLException {
        Connection oracleConnection = mainWindow.getConnection();
        try {
            StringBuffer sql = new StringBuffer("SELECT DISTINCT DVC.SUBSYS_ID ");
            sql.append("FROM EPICS.DVC, EPICS.MACHINE_MODE, EPICS.MPS_SGNL_PARAM ");
            sql.append("WHERE ((MPS_SGNL_PARAM.DVC_ID =  MACHINE_MODE.DVC_ID) AND (");
            sql.append("MPS_SGNL_PARAM.APPR_DTE = MACHINE_MODE.APPR_DTE) AND (MACHINE_MODE.MPS_DVC_ID = ");
            if (chainSel.equals("All Chains")) {
                sql.append("DVC.DVC_ID))");
            } else {
                sql.append("DVC.DVC_ID) AND MPS_SGNL_PARAM.MPS_CHAIN_ID = '");
                sql.append(chainSel);
                sql.append("')");
            }
            PreparedStatement query = oracleConnection.prepareStatement(sql.toString());
            subsystemBox.removeAllItems();
            ResultSet result = null;
            try {
                result = query.executeQuery();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Unable to connect to database.", "Timeout?", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
            if (result != null) {
                try {
                    while (result.next()) {
                        subsystemBox.addItem(result.getString(1));
                    }
                } finally {
                    result.close();
                    query.close();
                }
            }
        } finally {
        }
    }

    private void sendToMPS_CHAN_AUDIT(String title, String result, String lText, String dvcID) throws java.sql.SQLException {
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yy");
        SimpleDateFormat dspFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = formatter.format(now);
        String dspDate = dspFormat.format(now);
        int errNbr = 0;
        String status = "'Y'";
        if (result.equals("Failed")) {
            status = "'N'";
            if (lText.lastIndexOf("MEBT") > -1) {
                if (dvcID.lastIndexOf("Mag") > -1) errNbr = 1; else if (dvcID.lastIndexOf("LLRF") > -1) errNbr = 4; else if (dvcID.lastIndexOf("Bnch") > -1) errNbr = 7;
            } else if (lText.lastIndexOf("CCL") > -1) {
                if (dvcID.lastIndexOf("Mag") > -1) errNbr = 2; else if (dvcID.lastIndexOf("LLRF") > -1) errNbr = 5; else if (dvcID.lastIndexOf("HPM") > -1) errNbr = 8; else if (dvcID.lastIndexOf("BLM") > -1) errNbr = 10;
            } else if (lText.lastIndexOf("LDmp") > -1) {
                if (dvcID.lastIndexOf("Mag") > -1) errNbr = 3; else if (dvcID.lastIndexOf("LLRF") > -1) errNbr = 6; else if (dvcID.lastIndexOf("HPM") > -1) errNbr = 9; else if (dvcID.lastIndexOf("BLM") > -1) errNbr = 11;
            }
        }
        LoginDialog login = mainWindow.getLogin();
        Connection oracleConnection = mainWindow.getConnection();
        try {
            Statement query = oracleConnection.createStatement();
            StringBuffer buffer = new StringBuffer("SELECT BN, FIRST_NM, LAST_NAME FROM OPER.EMPLOYEE_V WHERE USER_ID = '");
            buffer.append(((OracleDataSource) getDataSource()).getUser().toUpperCase());
            buffer.append("'");
            ResultSet userData = null;
            try {
                userData = query.executeQuery(buffer.toString());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Unable to connect to database.", "Timeout?", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
            userData.next();
            String uid = userData.getString("BN");
            query.close();
            Statement auditUpdateQuery;
            String todate = "to_date('", restStr = "','dd-mon-yy')";
            StringBuffer sql = new StringBuffer("UPDATE epics.MPS_CHAN_AUDIT set AUDIT_DTE = ");
            if (errNbr > 0 && status.equals("'N'")) {
                sql.append(todate + dateString + restStr + ", PASS_IND = ");
                sql.append(status + ", ERR_NBR = " + errNbr);
            } else sql.append(todate + dateString + restStr + ", PASS_IND = " + status);
            sql.append(" where (MPS_DVC_ID = '" + lText + "')");
            auditUpdateQuery = oracleConnection.createStatement();
            try {
                auditUpdateQuery.execute(sql.toString());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, sql.toString(), "SQL Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
            mainWindow.getConnection().commit();
            auditUpdateQuery.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
        }
        String newDateStr = "";
        if (result.equals("Failed")) newDateStr = "<html><body><font COLOR=#ff0000>" + dspDate + "</font></body></html>"; else newDateStr = dspDate;
        updateIsTestedMap(lText, newDateStr, result);
        makeMPStable(0);
    }

    private void sendToElog(String title, String summary) throws java.sql.SQLException {
        String[] categories = null;
        LoginDialog login = mainWindow.getLogin();
        Connection oracleConnection = mainWindow.getConnection();
        Array categoryArray = getArray("LOGBOOK.LOGBOOK_CAT_TAB_TYP", oracleConnection, categories);
        try {
            Statement query = oracleConnection.createStatement();
            StringBuffer buffer = new StringBuffer("SELECT BN, FIRST_NM, LAST_NAME FROM OPER.EMPLOYEE_V WHERE USER_ID = '");
            buffer.append(((OracleDataSource) getDataSource()).getUser().toUpperCase());
            buffer.append("'");
            ResultSet userData = null;
            try {
                userData = query.executeQuery(buffer.toString());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Unable to connect to database.", "Timeout?", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
            userData.next();
            String uid = userData.getString("BN");
            query.close();
            String procedureCall = "{call LOGBOOK.LOGBOOK_PKG.INSERT_LOGBOOK_ENTRY(?, ?, ?, ?, ?)}";
            CallableStatement procedure = oracleConnection.prepareCall(procedureCall);
            buffer = new StringBuffer("Date: ");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy");
            buffer.append(dateFormat.format(new java.util.Date()));
            buffer.append("\n");
            buffer.append(summary);
            procedure.setString(1, uid);
            procedure.setString(2, "Machine Protection System");
            procedure.setString(3, title);
            procedure.setArray(4, categoryArray);
            procedure.setString(5, buffer.toString());
            procedure.execute();
            procedure.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
        }
    }

    private void sendEmail(String title, String summary, String receiver) throws java.sql.SQLException {
        Connection oracleConnection = mainWindow.getConnection();
        try {
            Statement query = oracleConnection.createStatement();
            StringBuffer buffer = new StringBuffer("SELECT BN, FIRST_NM, LAST_NAME FROM OPER.EMPLOYEE_V WHERE USER_ID = '");
            buffer.append(((OracleDataSource) getDataSource()).getUser().toUpperCase());
            buffer.append("'");
            ResultSet userData = null;
            try {
                userData = query.executeQuery(buffer.toString());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Unable to connect to database.", "Timeout?", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
            userData.next();
            query.close();
            String procedureCall = "{call Ops$oracle.global_utils.smtp_email(?, ?, ?, ?)}";
            CallableStatement procedure = oracleConnection.prepareCall(procedureCall);
            procedure.setString(1, "nypaverdj@ornl.gov");
            procedure.setString(2, receiver);
            procedure.setString(3, title);
            buffer = new StringBuffer("\nDate: ");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy");
            buffer.append(dateFormat.format(new java.util.Date()));
            buffer.append("\n");
            buffer.append(summary);
            buffer.append("\n");
            if (buffer.length() > 4000) {
                buffer.setLength(3800);
                buffer.append("\n*** Faulted PV List TRUNCATED! ***\n");
            }
            procedure.setString(4, buffer.toString());
            procedure.execute();
            procedure.close();
        } catch (java.sql.SQLException ex) {
            ex.printStackTrace();
        } finally {
        }
    }

    /**
   * Gets the <CODE>DataSource</CODE> from which the application will get it's 
   * connections to the database.
   *
   * @return connectionPool The instance of <CODE>OracleDataSource</CODE> for the application.
   */
    public DataSource getDataSource() {
        connectionPool = mainWindow.getConnectionPool();
        return connectionPool;
    }

    /**
   * Selects and shows an <CODE>JInternalFrame</CODE>. If the window is closed 
   * or minimized, this method restores it before showing and selecting it.
   *
   * @param frame The <CODE>JInternaFrame</CODE> to select.
   */
    private void selectWindow(JInternalFrame frame) {
        try {
            if (!frame.isVisible()) frame.setVisible(true);
            if (!frame.isSelected()) frame.setSelected(true);
        } catch (java.beans.PropertyVetoException ex) {
            ex.printStackTrace();
        }
    }

    public MPSFrame getMPSFrame() {
        return mainWindow;
    }

    public void putMagLabels(JLabel[][] MagPSLabels) {
        MPSDisplayLabels = MagPSLabels;
    }

    public void putBLMLabels(JLabel[][] BLMLabels) {
        MPSDisplayLabels = BLMLabels;
    }

    public void putLLRFlabels(JLabel[][] LLRFlabels) {
        MPSDisplayLabels = LLRFlabels;
    }

    public Map getLLRFmap() {
        return MPSllrf;
    }

    public void putHPRFlabels(JLabel[][] HPRFlabels) {
        MPSDisplayLabels = HPRFlabels;
    }

    public Map getHPRFmap() {
        return MPShprf;
    }

    public Map getMPShprfMap() {
        return MPShprfPVs;
    }

    private void mpsTableModel_tableChanged(final TableModelEvent e) {
    }

    public int[] getMPSllrfTestRdy(String pv) {
        String[][] MPStestValues = { { "", "", "" }, { "", "", "" }, { "", "", "" }, { "", "" }, { "", "", "" } };
        ChannelWrapper wrapper;
        TestMPSLLRFPanel newPanel = new TestMPSLLRFPanel();
        if (LLRFtest == null) {
            String SrcChain = chainBox.getSelectedItem();
            String chainSel = getChainSel();
            LLRFtest = newPanel.getRdyToTestLLRF(pv, chainSel, SrcChain);
        }
        MebtLLRFwrap = newPanel.getMebtLLRFwrap();
        Iterator iter = MebtLLRFwrap.iterator();
        int i = 0;
        String llrfpv;
        while (iter.hasNext()) {
            wrapper = (ChannelWrapper) iter.next();
            llrfpv = wrapper.getName();
            if ((llrfpv.lastIndexOf("_MEBT_BS_chan_status") > -1) || (llrfpv.lastIndexOf(":ChtFlt") > -1)) {
                MPStestValues[0][i] = llrfpv;
                MPSLLRFTestRdy[i] = wrapper.getValue();
            }
        }
        return MPSLLRFTestRdy;
    }

    int MEBTMagTestRdy(String initPV) {
        int TestRdy = 1;
        String[] testReady = { "0", "0", "0", "0", "1", "1", "1", "1", "1" };
        if (MEBTmagTestRdy == null) getMEBTmagTestRdy(initPV);
        for (int i = 0; i < MEBTmagTestRdy.length && TestRdy == 1; i++) {
            if (MEBTmagTestRdy[i] == -1) TestRdy = -1; else if (testReady[i] != String.valueOf(MEBTmagTestRdy[i])) TestRdy = 0;
        }
        return TestRdy;
    }

    public int[] getMPSTestRdy(String pv) {
        String[][] MPStestValues = { { "", "", "", "", "" }, { "", "", "", "", "" }, { "", "", "", "", "" } };
        ChannelWrapper wrapper;
        TestMagPSPanel newPanel = new TestMagPSPanel();
        if (MagTest == null || MPSmagWrap == null) {
            MagTest = newPanel.getRdyToTestMPSmag(pv);
        }
        int i = 0;
        String magpv, magval;
        MPSmagWrap = newPanel.getMPSmagWrap(pv);
        Iterator l = MPSmagWrap.iterator();
        while (l.hasNext()) {
            wrapper = (ChannelWrapper) l.next();
            magpv = wrapper.getName();
            MPStestValues[0][i] = magpv;
            MPSmagTestRdy[i] = wrapper.getValue();
            magval = "" + MPSmagTestRdy[i];
            MPStestValues[1][i] = magval;
            i++;
            if (i > 4) i = 0;
        }
        return MPSmagTestRdy;
    }

    public int[] getMEBTmagTestRdy(String pv) {
        String[][] MPStestValues = { { "", "", "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "", "", "" }, { "", "", "", "", "", "", "", "", "", "" } };
        ChannelWrapper wrapper;
        TestMEBTMagPSPanel newPanel = new TestMEBTMagPSPanel();
        if (MEBTmagTest == null) {
            MEBTmagTest = newPanel.getRdyToTestMebtMag(pv);
        }
        MEBTmagWrap = newPanel.getMebtMagWrap();
        Iterator iter = MEBTmagWrap.iterator();
        int i = 0;
        String mebtmagpv;
        while (iter.hasNext()) {
            wrapper = (ChannelWrapper) iter.next();
            mebtmagpv = wrapper.getName();
            MPStestValues[0][i] = mebtmagpv;
            MEBTmagTestRdy[i] = wrapper.getValue();
            i++;
        }
        return MEBTmagTestRdy;
    }

    public String getChainSel() {
        return chainBox.getSelectedItem();
    }

    public String getSubSysSel() {
        String subsysSel = (String) subsystemBox.getSelectedItem();
        if (subsysSel != null) return subsysSel;
        return "";
    }

    public String getDeviceSel() {
        String deviceSel = (String) deviceBox.getSelectedItem();
        if (deviceSel != null) return deviceSel;
        return "";
    }

    public int checkRowSelection() {
        if (lsm == null || lsm.isSelectionEmpty()) {
            selectedRow = -1;
        } else {
            selectedRow = lsm.getMinSelectionIndex();
        }
        return selectedRow;
    }

    public void putLLRFval(String initPvStr, int val) {
        ca.setLLRFpv(initPvStr, val);
    }

    public void resetMag(String initPvStr, int val) {
        ca.resetMag(initPvStr, val);
    }

    public void turnMagOn(String initPvStr, int val) {
        ca.MagOn(initPvStr, val);
    }

    public void turnMagOff(String initPvStr, int val) {
        ca.MagOff(initPvStr, val);
    }

    public void setTestMagPSPanel(TestMagPSPanel testMagPSPanel) {
        testMagPSpanel = testMagPSPanel;
    }

    public TestMagPSPanel getTestMagPSPanel() {
        return testMagPSpanel;
    }

    public boolean isMagPSPanel() {
        String chainSel = chainBox.getSelectedItem();
        String subsysSel = (String) subsystemBox.getSelectedItem();
        String deviceSel = (String) deviceBox.getSelectedItem();
        if ((chainSel.equals("CCL_BS") || chainSel.equals("LDmp")) && ((subsysSel != null && subsysSel.equals("Mag")) || (deviceSel != null && deviceSel.equals("PS")))) return true; else return false;
    }

    public void updateIsTestedMap(String pv, String status, String result) {
        isTestedMap = mainWindow.getIsTested_Map();
        PFstatusMap = mainWindow.getPFstatus_Map();
        Iterator m1 = isTestedMap.entrySet().iterator();
        Map.Entry p1, p2;
        Iterator m2 = PFstatusMap.entrySet().iterator();
        int row = mpsMMtable.getSelectedRow();
        mpsMMtable.setValueAt(status, row, 5);
        mpsMMtable.validate();
        while (m1.hasNext()) {
            p1 = (Map.Entry) m1.next();
            if (p1.getKey().equals(pv)) {
                p1.setValue(status);
                break;
            }
        }
        while (m2.hasNext()) {
            p2 = (Map.Entry) m2.next();
            if (p2.getKey().equals(pv)) {
                if (result.equals("Passed")) p2.setValue("'Y'"); else p2.setValue("'N'");
                break;
            }
        }
        mainWindow.putIsTested_Map(isTestedMap);
        mainWindow.putPFstatus_Map(PFstatusMap);
    }

    public int NumRowsSel() {
        return selectedIx.length;
    }
}
