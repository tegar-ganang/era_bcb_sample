package com.javable.dataview.analysis;

import javax.swing.table.DefaultTableModel;
import com.javable.dataview.DataDesktop;
import com.javable.dataview.DataFrame;
import com.javable.dataview.DataStorage;
import com.javable.dataview.DataView;
import com.javable.dataview.ResourceManager;

/**
 * Generates dialog box to perform averaging of the number of channels
 */
public class AveragingOptions extends javax.swing.JDialog {

    /** A return status code - returned if Cancel button has been pressed */
    public static final int RET_CANCEL = 0;

    /** A return status code - returned if OK button has been pressed */
    public static final int RET_OK = 1;

    private DataDesktop desktop;

    /**
     * Creates new AveragingOptions
     * 
     * @param parent parent frame
     * @param d desktop
     */
    public AveragingOptions(java.awt.Frame parent, DataDesktop d) {
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
        viewPanel = new javax.swing.JTabbedPane();
        paramsPanel = new javax.swing.JPanel();
        yPanel = new javax.swing.JPanel();
        ychanLabel = new javax.swing.JLabel();
        ychanTextField = new com.javable.utils.IntegerField();
        selectionScrollPane = new javax.swing.JScrollPane();
        selectionTable = new javax.swing.JTable();
        setTitle(ResourceManager.getResource("Channel_Average"));
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
        paramsPanel.add(yPanel);
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

            public Object construct() {
                doClose(RET_OK);
                DataFrame ifr = (DataFrame) desktop.getActiveFrame();
                if (ifr != null) {
                    DataView view = ifr.getDataContent().getView();
                    DataStorage source = view.getStorage();
                    try {
                        ChannelAvrg.averageTraces(source, ychanTextField.getValue());
                    } catch (Exception e) {
                    }
                }
                return ResourceManager.getResource("Done");
            }

            public void finished() {
                desktop.getTasks().removeTask();
            }
        };
        desktop.getTasks().addTask(ResourceManager.getResource("Channel_Average"));
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

    private javax.swing.JPanel buttonPanel;

    private javax.swing.JPanel yPanel;

    private javax.swing.JButton okButton;

    private javax.swing.JButton cancelButton;

    private javax.swing.JPanel paramsPanel;

    private com.javable.utils.IntegerField ychanTextField;

    private javax.swing.JLabel ychanLabel;

    private javax.swing.JTable selectionTable;

    private javax.swing.JPanel resPanel;

    private javax.swing.JScrollPane selectionScrollPane;

    private javax.swing.JTabbedPane viewPanel;

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
