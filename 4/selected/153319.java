package com.javable.dataview.analysis;

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
 * Generates dialog box to build a current-voltage relationship (I-V)
 */
public class IVOptions extends javax.swing.JDialog {

    /** A return status code - returned if Cancel button has been pressed */
    public static final int RET_CANCEL = 0;

    /** A return status code - returned if OK button has been pressed */
    public static final int RET_OK = 1;

    private DataDesktop desktop;

    private double xmin;

    private double xmax;

    private ChannelStatsProxy proxy = new ChannelStatsProxy();

    /**
     * Creates new IVOptions
     * 
     * @param parent parent frame
     * @param d desktop
     */
    public IVOptions(java.awt.Frame parent, DataDesktop d) {
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
        xchanLabel = new javax.swing.JLabel();
        xchanTextField = new com.javable.utils.IntegerField();
        cursorLabel = new javax.swing.JLabel();
        cursorComboBox = new javax.swing.JComboBox();
        yPanel = new javax.swing.JPanel();
        ychanLabel = new javax.swing.JLabel();
        ychanTextField = new com.javable.utils.IntegerField();
        valueLabel = new javax.swing.JLabel();
        valueComboBox = new javax.swing.JComboBox();
        regionLabel = new javax.swing.JLabel();
        regionSelector = new RegionSelector();
        resPanel = new javax.swing.JPanel();
        replaceRadioButton = new javax.swing.JRadioButton();
        appendRadioButton = new javax.swing.JRadioButton();
        selectionScrollPane = new javax.swing.JScrollPane();
        selectionTable = new javax.swing.JTable();
        setTitle(ResourceManager.getResource("Build_I-V"));
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
        xPanel.setLayout(new java.awt.GridBagLayout());
        xPanel.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), ResourceManager.getResource("X-Axis")));
        xchanLabel.setText(ResourceManager.getResource("Channel_"));
        xchanLabel.setDisplayedMnemonic('c');
        xchanLabel.setLabelFor(xchanTextField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        xPanel.add(xchanLabel, gridBagConstraints);
        xchanTextField.setText("1");
        xchanTextField.setPositive(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 5.0;
        xPanel.add(xchanTextField, gridBagConstraints);
        cursorLabel.setText(ResourceManager.getResource("Value_at_cursor_"));
        cursorLabel.setDisplayedMnemonic('v');
        cursorLabel.setLabelFor(cursorComboBox);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        xPanel.add(cursorLabel, gridBagConstraints);
        cursorComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        xPanel.add(cursorComboBox, gridBagConstraints);
        paramsPanel.add(xPanel);
        yPanel.setLayout(new java.awt.GridBagLayout());
        yPanel.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), ResourceManager.getResource("Y-Axis")));
        ychanLabel.setText(ResourceManager.getResource("Channel_"));
        ychanLabel.setDisplayedMnemonic('n');
        ychanLabel.setLabelFor(ychanTextField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        yPanel.add(ychanLabel, gridBagConstraints);
        ychanTextField.setText("2");
        xchanTextField.setPositive(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        yPanel.add(ychanTextField, gridBagConstraints);
        valueLabel.setText(ResourceManager.getResource("Value_"));
        valueLabel.setDisplayedMnemonic('u');
        valueLabel.setLabelFor(valueComboBox);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        yPanel.add(valueLabel, gridBagConstraints);
        valueComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { ResourceManager.getResource("At_cursor_1"), ResourceManager.getResource("Mean"), ResourceManager.getResource("Positive_peak"), ResourceManager.getResource("Negative_peak"), ResourceManager.getResource("Absolute_peak") }));
        valueComboBox.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                valueComboBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        yPanel.add(valueComboBox, gridBagConstraints);
        regionLabel.setText(ResourceManager.getResource("Region_"));
        regionLabel.setDisplayedMnemonic('g');
        regionLabel.setLabelFor(regionSelector.getRegionComboBox());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        yPanel.add(regionLabel, gridBagConstraints);
        regionSelector.getRegionComboBox().setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
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

    private void valueComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {
        regionSelector.getRegionComboBox().setEnabled(!(valueComboBox.getSelectedIndex() == 0));
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        com.javable.utils.SwingWorker worker = new com.javable.utils.SwingWorker() {

            DataStorage source, result;

            DataChannel xres, yres;

            DataFrame ifr;

            ViewContent iv;

            public Object construct() {
                doClose(RET_OK);
                com.javable.dataview.DataChannel xchannel = null;
                com.javable.dataview.DataChannel xychannel = null;
                com.javable.dataview.DataChannel yychannel = null;
                ifr = (DataFrame) desktop.getActiveFrame();
                if (ifr != null) {
                    iv = ifr.getIVContent();
                    if ((iv == null) || replaceRadioButton.isSelected()) {
                        iv = new ViewContent();
                        ifr.setIVContent(iv);
                    }
                    DataView view = ifr.getDataContent().getView();
                    source = view.getStorage();
                    result = iv.getStorage();
                    double r[] = regionSelector.getLimits(view);
                    xmin = r[0];
                    xmax = r[1];
                    String inf = "";
                    inf = "Cursor " + cursorComboBox.getSelectedItem() + ", Channel " + xchanTextField.getValue();
                    xres = new DataChannel(inf, source.getGroupsSize());
                    if (valueComboBox.getSelectedIndex() == 0) {
                        inf = valueComboBox.getSelectedItem() + " ";
                    } else {
                        inf = valueComboBox.getSelectedItem() + " " + regionSelector.getRegionComboBox().getSelectedItem();
                    }
                    inf = inf + ", Channel " + ychanTextField.getValue();
                    yres = new DataChannel(inf, source.getGroupsSize());
                    try {
                        for (int i = 0; i < source.getGroupsSize(); i++) {
                            DataGroup group = source.getGroup(i);
                            xchannel = source.getChannel(i, group.getXChannel());
                            xychannel = source.getChannel(i, xchanTextField.getValue());
                            yychannel = source.getChannel(i, ychanTextField.getValue());
                            if ((xchannel != null) && (xychannel != null) && (yychannel != null) && (xychannel.getAttribute().isNormal()) && (yychannel.getAttribute().isNormal())) {
                                if (i == 0) xres.setUnits(xychannel.getUnits());
                                xres.setData(i, ChannelStats.getValueAtX(view.getCursors().getCursorSlider().getValueAt(cursorComboBox.getSelectedIndex()), xchannel, xychannel));
                                if (i == 0) yres.setUnits(yychannel.getUnits());
                                if (valueComboBox.getSelectedIndex() == 0) {
                                    yres.setData(i, ChannelStats.getValueAtX(view.getCursors().getCursorSlider().getValueAt(0), xchannel, yychannel));
                                } else {
                                    yres.setData(i, getStat(valueComboBox.getSelectedIndex(), xchannel, yychannel));
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
                return ResourceManager.getResource("Done");
            }

            public void finished() {
                int gp = result.getGroupsSize();
                DataGroup group = new DataGroup(ResourceManager.getResource("IV") + " " + gp, "");
                group.addChannel(xres);
                group.addChannel(yres);
                result.addGroup(group);
                ifr.setSelectedContent(iv);
                desktop.getTasks().removeTask();
            }
        };
        desktop.getTasks().addTask(ResourceManager.getResource("Build_I-V"));
        worker.start();
    }

    private void fillSelection() {
        DataFrame ifr = (DataFrame) desktop.getActiveFrame();
        DataStorage source = ifr.getDataContent().getView().getStorage();
        Object[][] dat = new Object[source.getGroupsSize()][3];
        for (int i = 0; i < source.getGroupsSize(); i++) {
            dat[i][0] = "" + i;
            try {
                if (!source.getChannel(i, xchanTextField.getValue()).getAttribute().isNormal()) throw new NullPointerException();
                dat[i][1] = source.getChannel(i, xchanTextField.getValue()).getName();
            } catch (Exception e) {
                dat[i][1] = "--";
            }
            try {
                if (!source.getChannel(i, ychanTextField.getValue()).getAttribute().isNormal()) throw new NullPointerException();
                dat[i][2] = source.getChannel(i, ychanTextField.getValue()).getName();
            } catch (Exception e) {
                dat[i][2] = "--";
            }
        }
        selectionTable.setModel(new DefaultTableModel(dat, new String[] { ResourceManager.getResource("Sweep"), ResourceManager.getResource("X-Axis"), ResourceManager.getResource("Y-Axis") }));
    }

    private double getStat(int type, DataChannel xchannel, DataChannel ychannel) {
        proxy.clear();
        if (type == 1) return proxy.getMean(xmin, xmax, xchannel, ychannel);
        if (type == 2) return proxy.getPositivePeak(xmin, xmax, xchannel, ychannel);
        if (type == 3) return proxy.getNegativePeak(xmin, xmax, xchannel, ychannel);
        if (type == 4) return proxy.getAbsolutePeak(xmin, xmax, xchannel, ychannel);
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

    private javax.swing.JComboBox valueComboBox;

    private javax.swing.JRadioButton appendRadioButton;

    private javax.swing.JPanel buttonPanel;

    private com.javable.utils.IntegerField xchanTextField;

    private javax.swing.JLabel valueLabel;

    private javax.swing.JPanel yPanel;

    private javax.swing.JLabel regionLabel;

    private javax.swing.JPanel xPanel;

    private javax.swing.JButton okButton;

    private javax.swing.JButton cancelButton;

    private javax.swing.JComboBox cursorComboBox;

    private javax.swing.JPanel paramsPanel;

    private javax.swing.JLabel xchanLabel;

    private com.javable.utils.IntegerField ychanTextField;

    private javax.swing.JLabel ychanLabel;

    private javax.swing.JTable selectionTable;

    private javax.swing.ButtonGroup resButtonGroup;

    private javax.swing.JLabel cursorLabel;

    private RegionSelector regionSelector;

    private javax.swing.JPanel resPanel;

    private javax.swing.JScrollPane selectionScrollPane;

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
}
