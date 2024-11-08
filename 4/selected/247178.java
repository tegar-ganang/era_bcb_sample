package com.javable.dataview.analysis;

import java.util.Vector;
import com.javable.dataview.DataChannel;
import com.javable.dataview.DataDesktop;
import com.javable.dataview.DataFrame;
import com.javable.dataview.DataStorage;
import com.javable.dataview.ResourceManager;
import com.javable.dataview.plots.ChannelAttribute;
import com.javable.utils.ExceptionDialog;

/**
 * Generates dialog box to select sweep(s) from the list or user-entered string
 */
public class SweepSelector extends javax.swing.JDialog {

    /** A return status code - returned if Cancel button has been pressed */
    public static final int RET_CANCEL = 0;

    /** A return status code - returned if OK button has been pressed */
    public static final int RET_OK = 1;

    private DataDesktop desktop;

    /**
     * Generates dialog box to select sweeps from the list or by entering
     * numbers
     * 
     * @param parent parent frame
     * @param d desktop
     */
    public SweepSelector(java.awt.Frame parent, DataDesktop d) {
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
        buttonPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        sweepPanel = new javax.swing.JPanel();
        listPanel = new javax.swing.JPanel();
        listScrollPane = new javax.swing.JScrollPane();
        sweepList = new javax.swing.JList();
        fieldPanel = new javax.swing.JPanel();
        sweepLabel = new javax.swing.JLabel();
        sweepComboBox = new javax.swing.JComboBox();
        selectionPanel = new javax.swing.JPanel();
        allButton = new javax.swing.JButton();
        noneButton = new javax.swing.JButton();
        inverseButton = new javax.swing.JButton();
        setTitle(ResourceManager.getResource("Select_Sweeps"));
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
        sweepPanel.setLayout(new java.awt.BorderLayout());
        listPanel.setLayout(new java.awt.BorderLayout());
        listPanel.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), ResourceManager.getResource("Select_sweeps_from_the_list")));
        listPanel.setPreferredSize(new java.awt.Dimension(271, 155));
        listPanel.setMinimumSize(new java.awt.Dimension(271, 155));
        sweepList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {

            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                sweepListValueChanged(evt);
            }
        });
        listScrollPane.setViewportView(sweepList);
        listPanel.add(listScrollPane, java.awt.BorderLayout.CENTER);
        sweepPanel.add(listPanel, java.awt.BorderLayout.CENTER);
        fieldPanel.setLayout(new java.awt.GridBagLayout());
        fieldPanel.setBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EtchedBorder(), ResourceManager.getResource("Enter_sweep_number")));
        sweepLabel.setText(ResourceManager.getResource("Sweeps_"));
        sweepLabel.setDisplayedMnemonic('s');
        sweepLabel.setLabelFor(sweepComboBox);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        fieldPanel.add(sweepLabel, gridBagConstraints);
        sweepComboBox.setEditable(true);
        sweepComboBox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sweepComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        fieldPanel.add(sweepComboBox, gridBagConstraints);
        sweepPanel.add(fieldPanel, java.awt.BorderLayout.SOUTH);
        selectionPanel.setLayout(new java.awt.GridBagLayout());
        allButton.setMnemonic('a');
        allButton.setText(ResourceManager.getResource("Select_all"));
        allButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                allButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(20, 5, 10, 5);
        selectionPanel.add(allButton, gridBagConstraints);
        noneButton.setMnemonic('n');
        noneButton.setText(ResourceManager.getResource("Select_none"));
        noneButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                noneButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 10, 5);
        selectionPanel.add(noneButton, gridBagConstraints);
        inverseButton.setMnemonic('i');
        inverseButton.setText(ResourceManager.getResource("Invert"));
        inverseButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inverseButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 20, 5);
        selectionPanel.add(inverseButton, gridBagConstraints);
        sweepPanel.add(selectionPanel, java.awt.BorderLayout.EAST);
        getContentPane().add(sweepPanel, java.awt.BorderLayout.CENTER);
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doClose(RET_CANCEL);
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        sweepComboBoxActionPerformed(evt);
        int[] indices = sweepList.getSelectedIndices();
        DataFrame ifr = (DataFrame) desktop.getActiveFrame();
        if (ifr != null) {
            DataStorage source = ifr.getDataContent().getView().getStorage();
            for (int i = 0; i < source.getGroupsSize(); i++) {
                boolean visible = false;
                for (int k = 0; k < indices.length; k++) {
                    if (indices[k] == i) visible = true;
                }
                for (int j = 0; j < source.getChannelsSize(i); j++) {
                    DataChannel chan = source.getChannel(i, j);
                    if (visible) {
                        chan.getAttribute().setVisiblity(ChannelAttribute.NORMAL_CHANNEL);
                    } else {
                        chan.getAttribute().setVisiblity(ChannelAttribute.HIDDEN_CHANNEL);
                    }
                }
            }
        }
        doClose(RET_OK);
        ifr.getDataContent().getLegend().getTreeComponent().repaint();
        ifr.getDataContent().getView().repaint();
    }

    private void sweepComboBoxActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            sweepList.setSelectedIndices(SweepTokenizer.parseSweeps((String) sweepComboBox.getEditor().getItem()));
            sweepComboBox.addItem(sweepComboBox.getEditor().getItem());
        } catch (NumberFormatException e) {
            ExceptionDialog.showExceptionDialog(ResourceManager.getResource("Error"), ResourceManager.getResource("Error_sweep_message"), e);
        }
    }

    private void sweepListValueChanged(javax.swing.event.ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) return;
        sweepComboBox.getEditor().setItem(SweepTokenizer.parseSelection(sweepList.getSelectedIndices()));
    }

    private void inverseButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int[] indices = sweepList.getSelectedIndices();
        Vector selection = new Vector();
        for (int i = 0; i < sweepList.getModel().getSize(); i++) {
            selection.add(new Integer(i));
            for (int j = 0; j < indices.length; j++) {
                if (indices[j] == i) {
                    selection.remove(selection.size() - 1);
                }
            }
        }
        indices = new int[selection.size()];
        for (int i = 0; i < selection.size(); i++) {
            indices[i] = ((Integer) selection.get(i)).intValue();
        }
        sweepList.setSelectedIndices(indices);
    }

    private void noneButtonActionPerformed(java.awt.event.ActionEvent evt) {
        sweepList.clearSelection();
    }

    private void allButtonActionPerformed(java.awt.event.ActionEvent evt) {
        sweepList.addSelectionInterval(0, sweepList.getModel().getSize() - 1);
    }

    private void doClose(int retStatus) {
        returnStatus = retStatus;
        setVisible(false);
        dispose();
    }

    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {
        doClose(RET_CANCEL);
    }

    /**
     * Overrides setVisible(boolean b) in <code>JDialog</code>
     */
    public void setVisible(boolean b) {
        if (b == true) {
            setSweeps();
            setResizable(true);
            pack();
            setResizable(false);
            getRootPane().setDefaultButton(okButton);
            setLocationRelativeTo(getOwner());
        }
        super.setVisible(b);
    }

    private void setSweeps() {
        Vector data = new Vector();
        DataFrame ifr = (DataFrame) desktop.getActiveFrame();
        if (ifr != null) {
            DataStorage source = ifr.getDataContent().getView().getStorage();
            for (int i = 0; i < source.getGroupsSize(); i++) {
                data.add(source.getGroup(i).getName());
            }
        }
        sweepList.setListData(data);
    }

    private javax.swing.JPanel listPanel;

    private javax.swing.JPanel buttonPanel;

    private javax.swing.JList sweepList;

    private javax.swing.JPanel sweepPanel;

    private javax.swing.JComboBox sweepComboBox;

    private javax.swing.JButton okButton;

    private javax.swing.JButton cancelButton;

    private javax.swing.JButton allButton;

    private javax.swing.JLabel sweepLabel;

    private javax.swing.JPanel fieldPanel;

    private javax.swing.JButton inverseButton;

    private javax.swing.JScrollPane listScrollPane;

    private javax.swing.JPanel selectionPanel;

    private javax.swing.JButton noneButton;

    private int returnStatus = RET_CANCEL;
}
