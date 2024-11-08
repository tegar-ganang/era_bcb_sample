package com.javable.dataview.analysis;

import java.util.Vector;
import javax.swing.table.DefaultTableModel;
import com.javable.dataview.ResourceManager;
import com.javable.dataview.DataChannel;
import com.javable.dataview.DataDesktop;
import com.javable.dataview.DataFrame;
import com.javable.dataview.DataGroup;
import com.javable.dataview.DataStorage;
import com.javable.dataview.DataView;
import com.javable.dataview.ViewContent;

/**
 * Generates dialog box to perform a number of statistical routines on the
 * selected channel
 */
public class StatsOptions extends javax.swing.JDialog {

    /** A return status code - returned if Cancel button has been pressed */
    public static final int RET_CANCEL = 0;

    /** A return status code - returned if OK button has been pressed */
    public static final int RET_OK = 1;

    private DataDesktop desktop;

    private double xmin;

    private double xmax;

    private ChannelStatsProxy proxy;

    /**
     * Creates new StatsOptions
     * 
     * @param parent parent frame
     * @param d desktop
     */
    public StatsOptions(java.awt.Frame parent, DataDesktop d) {
        super(parent, true);
        desktop = d;
        initComponents();
    }

    /**
     * @return the return status of this dialog - one of RET_OK or RET_CANCEL
     */
    public int getReturnStatus() {
        return returnStatus;
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        resButtonGroup = new javax.swing.ButtonGroup();
        buttonPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        viewPanel = new javax.swing.JTabbedPane();
        paramsPanel = new javax.swing.JPanel();
        xPanel = new javax.swing.JPanel();
        yPanel = new javax.swing.JPanel();
        ychanLabel = new javax.swing.JLabel();
        ychanTextField = new com.javable.utils.IntegerField();
        regionLabel = new javax.swing.JLabel();
        regionSelector = new RegionSelector();
        resPanel = new javax.swing.JPanel();
        replaceRadioButton = new javax.swing.JRadioButton();
        appendRadioButton = new javax.swing.JRadioButton();
        selectionScrollPane = new javax.swing.JScrollPane();
        selectionTable = new javax.swing.JTable();
        analysisScrollPane = new javax.swing.JScrollPane();
        analysisTable = new javax.swing.JTable();
        setTitle(ResourceManager.getResource("Basic_Statistics"));
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        okButton.setText(ResourceManager.getResource("OK"));
        okButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(okButton);
        cancelButton.setText(ResourceManager.getResource("Cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(cancelButton);
        getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);
        paramsPanel.setLayout(new javax.swing.BoxLayout(paramsPanel, javax.swing.BoxLayout.Y_AXIS));
        xPanel.setLayout(new java.awt.BorderLayout());
        xPanel.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), ResourceManager.getResource("Analysis")));
        fillAnalysis();
        analysisScrollPane.setPreferredSize(new java.awt.Dimension(120, 140));
        analysisScrollPane.setViewportView(analysisTable);
        xPanel.add(analysisScrollPane, java.awt.BorderLayout.CENTER);
        paramsPanel.add(xPanel);
        yPanel.setLayout(new java.awt.GridBagLayout());
        yPanel.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), ResourceManager.getResource("Data")));
        ychanLabel.setText(ResourceManager.getResource("Channel_"));
        ychanLabel.setDisplayedMnemonic('c');
        ychanLabel.setLabelFor(ychanTextField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        yPanel.add(ychanLabel, gridBagConstraints);
        ychanTextField.setText("1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        yPanel.add(ychanTextField, gridBagConstraints);
        regionLabel.setText(ResourceManager.getResource("Region_"));
        regionLabel.setDisplayedMnemonic('g');
        regionLabel.setLabelFor(regionSelector.getRegionComboBox());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        yPanel.add(regionLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        yPanel.add(regionSelector.getRegionComboBox(), gridBagConstraints);
        paramsPanel.add(yPanel);
        resPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        resPanel.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), ResourceManager.getResource("Results")));
        replaceRadioButton.setSelected(true);
        replaceRadioButton.setText(ResourceManager.getResource("Replace"));
        replaceRadioButton.setMnemonic('r');
        resButtonGroup.add(replaceRadioButton);
        resPanel.add(replaceRadioButton);
        appendRadioButton.setText(ResourceManager.getResource("Append"));
        appendRadioButton.setMnemonic('a');
        resButtonGroup.add(appendRadioButton);
        resPanel.add(appendRadioButton);
        paramsPanel.add(resPanel);
        viewPanel.addTab(ResourceManager.getResource("Parameters"), paramsPanel);
        selectionScrollPane.setPreferredSize(new java.awt.Dimension(40, 40));
        selectionScrollPane.setViewportView(selectionTable);
        viewPanel.addTab(ResourceManager.getResource("Selection"), selectionScrollPane);
        viewPanel.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                if (viewPanel.getSelectedIndex() == 1) {
                    fillSelection();
                }
            }
        });
        getContentPane().add(viewPanel, java.awt.BorderLayout.CENTER);
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        com.javable.utils.SwingWorker worker = new com.javable.utils.SwingWorker() {

            DataStorage source, result;

            DataChannel xres, yres;

            DataFrame ifr;

            ViewContent stats;

            Vector channels;

            public Object construct() {
                doClose(RET_OK);
                com.javable.dataview.DataChannel xchannel = null;
                com.javable.dataview.DataChannel ychannel = null;
                Vector proxies = new Vector();
                ifr = (DataFrame) desktop.getActiveFrame();
                if (ifr != null) {
                    stats = ifr.getStatsContent();
                    if ((stats == null) || replaceRadioButton.isSelected()) {
                        stats = new ViewContent();
                        ifr.setStatsContent(stats);
                    }
                    DataView view = ifr.getDataContent().getView();
                    source = view.getStorage();
                    result = stats.getStorage();
                    double r[] = regionSelector.getLimits(view);
                    xmin = r[0];
                    xmax = r[1];
                    String inf = "";
                    inf = regionSelector.getRegionComboBox().getSelectedItem() + ", Channel " + ychanTextField.getValue();
                    xres = new DataChannel(inf, source.getGroupsSize());
                    try {
                        for (int i = 0; i < source.getGroupsSize(); i++) {
                            DataGroup group = source.getGroup(i);
                            xchannel = source.getChannel(i, group.getXChannel());
                            ychannel = source.getChannel(i, ychanTextField.getValue());
                            if ((xchannel != null) && (ychannel != null) && (ychannel.getAttribute().isNormal())) {
                                xres.setData(i, i);
                                proxies.addElement(new ChannelStatsProxy());
                            }
                        }
                        channels = new Vector();
                        for (int k = 0; k < analysisTable.getModel().getRowCount(); k++) {
                            if (((Boolean) analysisTable.getValueAt(k, 1)).booleanValue()) {
                                inf = (String) analysisTable.getValueAt(k, 0);
                                yres = new DataChannel(inf, source.getGroupsSize());
                                int t = 0;
                                for (int i = 0; i < source.getGroupsSize(); i++) {
                                    DataGroup group = source.getGroup(i);
                                    xchannel = source.getChannel(i, group.getXChannel());
                                    ychannel = source.getChannel(i, ychanTextField.getValue());
                                    if ((xchannel != null) && (ychannel != null) && (ychannel.getAttribute().isNormal())) {
                                        proxy = (ChannelStatsProxy) proxies.get(t);
                                        yres.setData(i, getStat(k, xchannel, ychannel));
                                        t++;
                                    }
                                }
                                channels.add(yres);
                            }
                        }
                    } catch (Exception e) {
                    }
                }
                return ResourceManager.getResource("Done");
            }

            public void finished() {
                int gp = result.getGroupsSize();
                DataGroup group = new DataGroup(ResourceManager.getResource("Statistics") + " " + gp, "");
                group.addChannel(xres);
                for (int i = 0; i < channels.size(); i++) {
                    group.addChannel((DataChannel) channels.get(i));
                }
                result.addGroup(group);
                ifr.setSelectedContent(stats);
                desktop.getTasks().removeTask();
            }
        };
        desktop.getTasks().addTask(ResourceManager.getResource("Basic_Statistics"));
        worker.start();
    }

    private void fillSelection() {
        DataFrame ifr = (DataFrame) desktop.getActiveFrame();
        DataStorage source = ifr.getDataContent().getView().getStorage();
        Object[][] dat = new Object[source.getGroupsSize()][2];
        for (int i = 0; i < source.getGroupsSize(); i++) {
            dat[i][0] = "" + i;
            try {
                if (!source.getChannel(i, ychanTextField.getValue()).getAttribute().isNormal()) throw new NullPointerException();
                dat[i][1] = source.getChannel(i, ychanTextField.getValue()).getName();
            } catch (Exception e) {
                dat[i][1] = "--";
            }
        }
        selectionTable.setModel(new DefaultTableModel(dat, new String[] { ResourceManager.getResource("Sweep"), ResourceManager.getResource("Data") }));
    }

    private void fillAnalysis() {
        java.util.Vector labels = new java.util.Vector();
        labels.add("Number of samples");
        labels.add("Mean");
        labels.add("Variance");
        labels.add("Standard Deviation");
        labels.add("Standard Error");
        labels.add("Negative Peak");
        labels.add("Negative Peak Time");
        labels.add("Positive Peak");
        labels.add("Positive Peak Time");
        labels.add("Absolute Peak");
        labels.add("Absolute Peak Time");
        labels.add("10-90% Slope Rise Time");
        labels.add("90-10% Slope Decay Time");
        labels.add("Max Left Slope");
        labels.add("Max Left Slope Time");
        labels.add("Max Right Slope");
        labels.add("Max Right Slope Time");
        labels.add("Curve Half-Width");
        labels.add("Curve Area");
        Object[][] dat = new Object[labels.size()][2];
        for (int i = 0; i < labels.size(); i++) {
            dat[i][0] = (String) labels.get(i);
            dat[i][1] = new Boolean(true);
        }
        analysisTable.setModel(new FunctionTableModel(dat, new String[] { ResourceManager.getResource("Function"), ResourceManager.getResource("Use") }));
        analysisTable.getColumnModel().getColumn(1).setMaxWidth(25);
    }

    private double getStat(int type, DataChannel xchannel, DataChannel ychannel) {
        if (type == 0) return proxy.getN(xmin, xmax, xchannel, ychannel);
        if (type == 1) return proxy.getMean(xmin, xmax, xchannel, ychannel);
        if (type == 2) return proxy.getVariance(xmin, xmax, xchannel, ychannel);
        if (type == 3) return proxy.getStandardDeviation(xmin, xmax, xchannel, ychannel);
        if (type == 4) return proxy.getStandardError(xmin, xmax, xchannel, ychannel);
        if (type == 5) return proxy.getNegativePeak(xmin, xmax, xchannel, ychannel);
        if (type == 6) return proxy.getNegativePeakTime(xmin, xmax, xchannel, ychannel);
        if (type == 7) return proxy.getPositivePeak(xmin, xmax, xchannel, ychannel);
        if (type == 8) return proxy.getPositivePeakTime(xmin, xmax, xchannel, ychannel);
        if (type == 9) return proxy.getAbsolutePeak(xmin, xmax, xchannel, ychannel);
        if (type == 10) return proxy.getAbsolutePeakTime(xmin, xmax, xchannel, ychannel);
        if (type == 11) return proxy.get10_90RiseTime(xmin, xmax, xchannel, ychannel);
        if (type == 12) return proxy.get90_10DecayTime(xmin, xmax, xchannel, ychannel);
        if (type == 13) return proxy.getMaxSlope(xmin, xmax, ChannelStats.LEFT_SLOPE, xchannel, ychannel);
        if (type == 14) return proxy.getMaxSlopeTime(xmin, xmax, ChannelStats.LEFT_SLOPE, xchannel, ychannel);
        if (type == 15) return proxy.getMaxSlope(xmin, xmax, ChannelStats.RIGHT_SLOPE, xchannel, ychannel);
        if (type == 16) return proxy.getMaxSlopeTime(xmin, xmax, ChannelStats.RIGHT_SLOPE, xchannel, ychannel);
        if (type == 17) return proxy.getHalfWidthTime(xmin, xmax, xchannel, ychannel);
        if (type == 18) return proxy.getCurveArea(xmin, xmax, xchannel, ychannel);
        return 0.0;
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doClose(RET_CANCEL);
    }

    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {
        doClose(RET_CANCEL);
    }

    private void doClose(int retStatus) {
        returnStatus = retStatus;
        setVisible(false);
        dispose();
    }

    private javax.swing.JRadioButton appendRadioButton;

    private javax.swing.JPanel buttonPanel;

    private javax.swing.JPanel yPanel;

    private javax.swing.JLabel regionLabel;

    private javax.swing.JPanel xPanel;

    private javax.swing.JButton okButton;

    private javax.swing.JButton cancelButton;

    private javax.swing.JPanel paramsPanel;

    private com.javable.utils.IntegerField ychanTextField;

    private javax.swing.JLabel ychanLabel;

    private javax.swing.JTable selectionTable;

    private javax.swing.JTable analysisTable;

    private javax.swing.ButtonGroup resButtonGroup;

    private RegionSelector regionSelector;

    private javax.swing.JPanel resPanel;

    private javax.swing.JScrollPane selectionScrollPane;

    private javax.swing.JScrollPane analysisScrollPane;

    private javax.swing.JTabbedPane viewPanel;

    private javax.swing.JRadioButton replaceRadioButton;

    private int returnStatus = RET_CANCEL;

    /**
     * Overrides setVisible(boolean b) in <code>JDialog</code>
     */
    public void setVisible(boolean b) {
        if (b == true) {
            setResizable(true);
            pack();
            setResizable(false);
            getRootPane().setDefaultButton(okButton);
            setLocationRelativeTo(getOwner());
        }
        super.setVisible(b);
    }

    class FunctionTableModel extends DefaultTableModel {

        /**
         * Constructor for FunctionTableModel.
         * 
         * @param data
         * @param columnNames
         */
        public FunctionTableModel(Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }

        /**
         * Check if given cell is editable
         * 
         * @param row table row
         * @param col table column
         */
        public boolean isCellEditable(int row, int col) {
            if (col == 0) {
                return false;
            } else {
                return true;
            }
        }

        /**
         * Returns the Java type for the given column.
         */
        public Class getColumnClass(int column) {
            if (column == 0) return String.class;
            return Boolean.class;
        }
    }
}
