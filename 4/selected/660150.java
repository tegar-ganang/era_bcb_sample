package org.plantstreamer.opc2out.database.export;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import org.communications.CommunicationManager.STATUS;
import org.communications.I18NException;
import org.database.DBConnectionInfo;
import org.database.DatabaseManager;
import org.opcda2out.output.database.AbstractDatabaseStructureHandler;
import org.opcda2out.output.database.upgrade.DatabaseStructureTransformer;
import org.opcda2out.output.database.upgrade.DatabaseStructureTransformer.DatabaseVersion;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.plantstreamer.Main;
import swingextras.EDTWorker;
import swingextras.action.ActionX;
import swingextras.action.ActionXData;
import swingextras.gui.JComponentBorder;
import swingextras.gui.QuestionDialog;
import swingextras.gui.SwingWorkerX;
import swingextras.icons.IconManager;

/**
 * The database data export panel
 * @author  Joao Leal
 */
@SuppressWarnings("serial")
public class ExportJPanel extends javax.swing.JPanel {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("org/plantstreamer/i18n/common");

    private static final Logger logger = Logger.getLogger(ExportJPanel.class.getName());

    private static final Map<String, Integer> dataTableColumns = new HashMap<String, Integer>(1);

    static {
        dataTableColumns.put("t", Types.TIMESTAMP);
    }

    private final SpinnerDateModel initModel, finalModel;

    private final RowFilter<VariableTableModel, Object> tableRowFilter = new RowFilter<VariableTableModel, Object>() {

        @Override
        public boolean include(RowFilter.Entry<? extends VariableTableModel, ? extends Object> entry) {
            String opcType = (String) entry.getValue(1);
            String opcId = (String) entry.getValue(2);
            String units = (String) entry.getValue(3);
            if (!findBar.matches(opcId, opcType, units)) {
                return false;
            }
            return true;
        }
    };

    @SuppressWarnings("serial")
    private final ActionX actionExport = new ActionX(new ActionXData("export")) {

        @Override
        public void actionPerformed(ActionEvent e) {
            export();
        }
    };

    private final PropertyChangeListener workerListener = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (worker.isDone()) {
                actionExport.setEnabled(true);
                worker.getPropertyChangeSupport().removePropertyChangeListener("state", this);
            }
        }
    };

    private CSVExportWorker worker;

    private SwingWorker workerTimeLimit;

    private Map<Integer, ConnectionInformation> serv_id2servers;

    /** Creates new form ExportJPanel */
    public ExportJPanel() {
        initComponents();
        jPanelTime.setBorder(new JComponentBorder(jCheckBoxTime, jPanelTime));
        initModel = (SpinnerDateModel) jSpinnerInitTime.getModel();
        finalModel = (SpinnerDateModel) jSpinnerFinalTime.getModel();
        initModel.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                finalModel.setStart((Date) initModel.getValue());
            }
        });
        finalModel.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                initModel.setEnd((Date) finalModel.getValue());
            }
        });
        finalModel.setStart((Date) initModel.getValue());
        initModel.setEnd((Date) finalModel.getValue());
        jTextFieldFile.setText(new File(System.getProperty("user.home"), "data.csv").getAbsolutePath());
        reloadDatabaseServerList();
        jComboBoxServers.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                serverSelectionChanged();
            }
        });
        jComboBoxServers.setRenderer(new ConnectionInfoComboBoxRenderer(jComboBoxServers));
        simpleStatusBar.addLogger(logger);
        simpleStatusBar.addLogger(Logger.getLogger(CSVExportWorker.class.getName()));
        @SuppressWarnings("serial") ActionX actionCancel = new ActionX(new ActionXData("cancel")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        };
        jButtonCancel.setAction(actionCancel);
        jButtonExport.setAction(actionExport);
        actionExport.setEnabled(false);
        findBar.hideBar();
    }

    private void reloadDatabaseServerList() {
        SwingWorker tableWorker = new ServerInfoWorker();
        simpleStatusBar.addWorker(tableWorker);
        tableWorker.execute();
    }

    private void serverSelectionChanged() {
        final ConnectionInformation server = (ConnectionInformation) jComboBoxServers.getSelectedItem();
        Integer serv_id = null;
        for (Entry<Integer, ConnectionInformation> e : serv_id2servers.entrySet()) {
            if (e.getValue().equals(server)) {
                serv_id = e.getKey();
            }
        }
        VariableTableModel model = new VariableTableModel(serv_id);
        jTableVars.setModel(model);
        if (serv_id == null) {
            actionExport.setEnabled(false);
        } else {
            actionExport.setEnabled(true);
            simpleStatusBar.addWorker(model.launchWorker());
            TableColumn col = jTableVars.getColumnModel().getColumn(0);
            col.setMinWidth(32);
            col.setMaxWidth(32);
            col.setCellRenderer(new VariableTypeCellRenderer());
            col = jTableVars.getColumnModel().getColumn(1);
            col.setMinWidth(100);
            col.setPreferredWidth(250);
            col = jTableVars.getColumnModel().getColumn(2);
            col.setMinWidth(32);
            col.setMaxWidth(200);
            col.setPreferredWidth(150);
            col = jTableVars.getColumnModel().getColumn(3);
            col.setMinWidth(32);
            col.setMaxWidth(200);
            col.setPreferredWidth(80);
            col = jTableVars.getColumnModel().getColumn(4);
            col.setMinWidth(32);
            col.setMaxWidth(150);
            col.setPreferredWidth(80);
            TableRowSorter<VariableTableModel> tableRowSorter = (TableRowSorter<VariableTableModel>) jTableVars.getRowSorter();
            tableRowSorter.setRowFilter(tableRowFilter);
            tableRowSorter.toggleSortOrder(1);
            tableRowSorter.toggleSortOrder(1);
            findBar.setTables(jPanelTags, jTableVars);
            jToggleButtonFind.setAction(findBar.getToggleAction());
        }
        initModel.setStart(null);
        finalModel.setEnd(null);
    }

    /**
     * Determines if a data table has the right structure
     * @param dataTable the data table name
     * @return 
     */
    private boolean checkStructure(String dataTable) {
        try {
            String schema = Main.db.conListInfo.getInfo().getSchema();
            DatabaseManager.checkTableStructure(Main.db.getCon(), schema, dataTable, dataTableColumns);
        } catch (SQLException ex) {
            logger.log(Level.FINE, ex.getLocalizedMessage(), ex);
            return false;
        } catch (I18NException ex) {
            logger.log(Level.FINE, ex.getLocalizedMessage(), ex);
            return false;
        }
        return true;
    }

    /**
     * Starts the export operation
     */
    public synchronized void export() {
        if (worker != null && !worker.isDone()) {
            return;
        }
        File file = new File(jTextFieldFile.getText());
        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this), MessageFormat.format(bundle.getString("File_{0}_already_exists!\nDo_you_really_want_to_overwrite_it?"), file.getPath()), bundle.getString("File_exists"), JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        actionExport.setEnabled(false);
        Date tinit = null;
        Date tfinal = null;
        if (jCheckBoxTime.isSelected()) {
            tinit = (Date) jSpinnerInitTime.getValue();
            tfinal = (Date) jSpinnerFinalTime.getValue();
        }
        VariableTableModel model = (VariableTableModel) jTableVars.getModel();
        worker = new CSVExportWorker(file, tinit, tfinal, model.getSelectedVars(), jCheckBoxHeader.isSelected());
        worker.getPropertyChangeSupport().addPropertyChangeListener("state", workerListener);
        simpleStatusBar.addWorker(worker);
        worker.execute();
    }

    /**
     * Fetches the maximum write time from the database and updates the time limits
     */
    private synchronized void updateTmax() {
        if (workerTimeLimit != null && !workerTimeLimit.isDone()) {
            return;
        }
        workerTimeLimit = new TmaxWorker();
        workerTimeLimit.execute();
        simpleStatusBar.addWorker(workerTimeLimit);
    }

    /**
     * Fetches the minimum write time from the database and updates the time limits
     */
    private synchronized void updateTmin() {
        if (workerTimeLimit != null && !workerTimeLimit.isDone()) {
            return;
        }
        workerTimeLimit = new TminWorker();
        workerTimeLimit.execute();
        simpleStatusBar.addWorker(workerTimeLimit);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        jCheckBoxTime = new javax.swing.JCheckBox();
        jPanelButton = new javax.swing.JPanel();
        jButtonExport = new javax.swing.JButton();
        jButtonCancel = new javax.swing.JButton();
        simpleStatusBar = new swingextras.gui.SimpleStatusBar();
        jTabbedPane = new javax.swing.JTabbedPane();
        jPanelExport = new javax.swing.JPanel();
        jPanelTimeFilter = new javax.swing.JPanel();
        jCheckBoxHeader = new javax.swing.JCheckBox();
        jPanelTime = new javax.swing.JPanel();
        jButtonTmin = new javax.swing.JButton();
        jButtonTmax = new javax.swing.JButton();
        jLabelFinalTime = new javax.swing.JLabel();
        jLabelInitTime = new javax.swing.JLabel();
        jSpinnerInitTime = new javax.swing.JSpinner();
        jSpinnerFinalTime = new javax.swing.JSpinner();
        jPanelGeneral = new javax.swing.JPanel();
        jButtonFile = new javax.swing.JButton();
        jTextFieldFile = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jButtonTablesReload = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jComboBoxServers = new javax.swing.JComboBox();
        jPanelTags = new javax.swing.JPanel();
        jScrollPane = new javax.swing.JScrollPane();
        jTableVars = new javax.swing.JTable();
        jToolBarTags = new javax.swing.JToolBar();
        jToggleButtonFind = new javax.swing.JToggleButton();
        jButtonAll = new javax.swing.JButton();
        jButtonNone = new javax.swing.JButton();
        findBar = new swingextras.gui.FindBar();
        jCheckBoxTime.setText(bundle.getString("filter_write_time"));
        jCheckBoxTime.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxTimeActionPerformed(evt);
            }
        });
        setLayout(new java.awt.GridBagLayout());
        javax.swing.GroupLayout jPanelButtonLayout = new javax.swing.GroupLayout(jPanelButton);
        jPanelButton.setLayout(jPanelButtonLayout);
        jPanelButtonLayout.setHorizontalGroup(jPanelButtonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelButtonLayout.createSequentialGroup().addContainerGap(511, Short.MAX_VALUE).addComponent(jButtonExport).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButtonCancel).addGap(6, 6, 6)));
        jPanelButtonLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { jButtonCancel, jButtonExport });
        jPanelButtonLayout.setVerticalGroup(jPanelButtonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanelButtonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jButtonCancel).addComponent(jButtonExport)));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(jPanelButton, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(simpleStatusBar, gridBagConstraints);
        jPanelExport.setLayout(new java.awt.GridBagLayout());
        jPanelTimeFilter.setLayout(new java.awt.GridBagLayout());
        jCheckBoxHeader.setSelected(true);
        jCheckBoxHeader.setText(bundle.getString("add_header"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        jPanelTimeFilter.add(jCheckBoxHeader, gridBagConstraints);
        jPanelTime.setLayout(new java.awt.GridBagLayout());
        jButtonTmin.setIcon(IconManager.getIcon("16x16/actions/top.png"));
        jButtonTmin.setEnabled(false);
        jButtonTmin.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonTminActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 5, 10);
        jPanelTime.add(jButtonTmin, gridBagConstraints);
        jButtonTmax.setIcon(IconManager.getIcon("16x16/actions/bottom.png"));
        jButtonTmax.setEnabled(false);
        jButtonTmax.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonTmaxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 10, 10);
        jPanelTime.add(jButtonTmax, gridBagConstraints);
        jLabelFinalTime.setText(bundle.getString("Final_time:"));
        jLabelFinalTime.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 10, 5);
        jPanelTime.add(jLabelFinalTime, gridBagConstraints);
        jLabelInitTime.setText(bundle.getString("Initial_time:"));
        jLabelInitTime.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 5, 5);
        jPanelTime.add(jLabelInitTime, gridBagConstraints);
        jSpinnerInitTime.setModel(new javax.swing.SpinnerDateModel());
        jSpinnerInitTime.setEnabled(false);
        jSpinnerInitTime.setPreferredSize(new java.awt.Dimension(200, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 5, 5);
        jPanelTime.add(jSpinnerInitTime, gridBagConstraints);
        jSpinnerFinalTime.setModel(new javax.swing.SpinnerDateModel());
        jSpinnerFinalTime.setEnabled(false);
        jSpinnerFinalTime.setPreferredSize(new java.awt.Dimension(200, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 10, 5);
        jPanelTime.add(jSpinnerFinalTime, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanelTimeFilter.add(jPanelTime, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanelExport.add(jPanelTimeFilter, gridBagConstraints);
        jPanelGeneral.setLayout(new java.awt.GridBagLayout());
        jButtonFile.setIcon(IconManager.getIcon("16x16/actions/fileopen.png"));
        jButtonFile.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFileActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanelGeneral.add(jButtonFile, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanelGeneral.add(jTextFieldFile, gridBagConstraints);
        jLabel2.setText(bundle.getString("File_name:"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanelGeneral.add(jLabel2, gridBagConstraints);
        jButtonTablesReload.setIcon(IconManager.getIcon("16x16/actions/reload.png"));
        jButtonTablesReload.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonTablesReloadActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanelGeneral.add(jButtonTablesReload, gridBagConstraints);
        jLabel3.setText("Server:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanelGeneral.add(jLabel3, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanelGeneral.add(jComboBoxServers, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanelExport.add(jPanelGeneral, gridBagConstraints);
        jTabbedPane.addTab("Export", jPanelExport);
        jPanelTags.setLayout(new java.awt.GridBagLayout());
        jScrollPane.setPreferredSize(new java.awt.Dimension(453, 50));
        jTableVars.setAutoCreateRowSorter(true);
        jScrollPane.setViewportView(jTableVars);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelTags.add(jScrollPane, gridBagConstraints);
        jToolBarTags.setFloatable(false);
        jToolBarTags.setRollover(true);
        jToggleButtonFind.setFocusable(false);
        jToggleButtonFind.setHideActionText(true);
        jToggleButtonFind.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButtonFind.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBarTags.add(jToggleButtonFind);
        jButtonAll.setText(bundle.getString("Select_all"));
        jButtonAll.setFocusable(false);
        jButtonAll.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonAll.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonAll.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAllActionPerformed(evt);
            }
        });
        jToolBarTags.add(jButtonAll);
        jButtonNone.setText(bundle.getString("Deselect_all"));
        jButtonNone.setFocusable(false);
        jButtonNone.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonNone.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonNone.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNoneActionPerformed(evt);
            }
        });
        jToolBarTags.add(jButtonNone);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanelTags.add(jToolBarTags, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanelTags.add(findBar, gridBagConstraints);
        jTabbedPane.addTab(bundle.getString("variables"), jPanelTags);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jTabbedPane, gridBagConstraints);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/plantstreamer/i18n/common");
        jTabbedPane.getAccessibleContext().setAccessibleName(bundle.getString("Export"));
    }

    private void jButtonFileActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        int answer = fileChooser.showSaveDialog(SwingUtilities.getWindowAncestor(this));
        if (answer == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                jTextFieldFile.setText(file.getAbsolutePath());
            }
        }
    }

    private void jButtonTablesReloadActionPerformed(java.awt.event.ActionEvent evt) {
        reloadDatabaseServerList();
    }

    private void jCheckBoxTimeActionPerformed(java.awt.event.ActionEvent evt) {
        if (jCheckBoxTime.isSelected()) {
            setEnableTimeFilter(true);
        } else {
            setEnableTimeFilter(false);
        }
    }

    private void jButtonTminActionPerformed(java.awt.event.ActionEvent evt) {
        updateTmin();
    }

    private void jButtonTmaxActionPerformed(java.awt.event.ActionEvent evt) {
        updateTmax();
    }

    private void jButtonAllActionPerformed(java.awt.event.ActionEvent evt) {
        ((VariableTableModel) jTableVars.getModel()).setExport(true);
    }

    private void jButtonNoneActionPerformed(java.awt.event.ActionEvent evt) {
        ((VariableTableModel) jTableVars.getModel()).setExport(false);
    }

    /**
     * Enables or disables the time filter
     * @param enabled
     */
    private void setEnableTimeFilter(boolean enabled) {
        jSpinnerInitTime.setEnabled(enabled);
        jSpinnerFinalTime.setEnabled(enabled);
        jLabelInitTime.setEnabled(enabled);
        jLabelFinalTime.setEnabled(enabled);
        if (workerTimeLimit == null || workerTimeLimit.isDone()) {
            jButtonTmin.setEnabled(enabled);
            jButtonTmax.setEnabled(enabled);
        } else {
            jButtonTmin.setEnabled(false);
            jButtonTmax.setEnabled(false);
        }
    }

    /**
     * Cancels the export operation
     */
    public void cancel() {
        Window d = SwingUtilities.getWindowAncestor(this);
        if (worker != null && !worker.isDone()) {
            int answer = JOptionPane.showConfirmDialog(this, bundle.getString("Do_you_really_want_to_stop_the_export?"), bundle.getString("Stop_export"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {
                worker.cancel(false);
            }
            return;
        } else if (d != null) {
            d.dispose();
        }
        if (workerTimeLimit != null) {
            workerTimeLimit.cancel(false);
        }
    }

    private swingextras.gui.FindBar findBar;

    private javax.swing.JButton jButtonAll;

    protected javax.swing.JButton jButtonCancel;

    protected javax.swing.JButton jButtonExport;

    private javax.swing.JButton jButtonFile;

    private javax.swing.JButton jButtonNone;

    private javax.swing.JButton jButtonTablesReload;

    private javax.swing.JButton jButtonTmax;

    private javax.swing.JButton jButtonTmin;

    private javax.swing.JCheckBox jCheckBoxHeader;

    private javax.swing.JCheckBox jCheckBoxTime;

    private javax.swing.JComboBox jComboBoxServers;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabelFinalTime;

    private javax.swing.JLabel jLabelInitTime;

    private javax.swing.JPanel jPanelButton;

    private javax.swing.JPanel jPanelExport;

    private javax.swing.JPanel jPanelGeneral;

    private javax.swing.JPanel jPanelTags;

    private javax.swing.JPanel jPanelTime;

    private javax.swing.JPanel jPanelTimeFilter;

    private javax.swing.JScrollPane jScrollPane;

    private javax.swing.JSpinner jSpinnerFinalTime;

    private javax.swing.JSpinner jSpinnerInitTime;

    private javax.swing.JTabbedPane jTabbedPane;

    private javax.swing.JTable jTableVars;

    private javax.swing.JTextField jTextFieldFile;

    private javax.swing.JToggleButton jToggleButtonFind;

    private javax.swing.JToolBar jToolBarTags;

    private swingextras.gui.SimpleStatusBar simpleStatusBar;

    class TLimitWorker extends SwingWorker<Date[], Object> {

        private final String sql;

        public TLimitWorker(String sql) {
            this.sql = sql;
        }

        @Override
        protected Date[] doInBackground() throws Exception {
            Date[] limit = null;
            if (Main.db.getConnectionStatus() != STATUS.CONNECTED) {
                return null;
            }
            final ConnectionInformation ci = (ConnectionInformation) jComboBoxServers.getSelectedItem();
            if (ci == null) {
                return null;
            }
            Integer serv_id = null;
            for (Entry<Integer, ConnectionInformation> e : serv_id2servers.entrySet()) {
                if (ci == e.getValue()) {
                    serv_id = e.getKey();
                    break;
                }
            }
            if (serv_id == null) {
                return null;
            }
            String[] dataTableNames = null;
            try {
                dataTableNames = getDataTableNames(serv_id);
            } catch (Exception ex) {
                logger.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            }
            if (dataTableNames == null || dataTableNames.length == 0) {
                return null;
            }
            limit = new Date[dataTableNames.length];
            Statement stmt = null;
            ResultSet rs = null;
            try {
                for (int i = 0; i < dataTableNames.length; i++) {
                    stmt = Main.db.getCon().createStatement();
                    rs = stmt.executeQuery(sql + dataTableNames[i]);
                    setProgress(5);
                    if (rs.next()) {
                        Timestamp time = rs.getTimestamp(1);
                        if (!rs.wasNull()) {
                            limit[i] = new Date(time.getTime());
                        }
                    }
                    setProgress((i + 1) * 100 / dataTableNames.length);
                }
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            } finally {
                DatabaseManager.closeResultSet(rs);
                DatabaseManager.closeStatement(stmt);
            }
            return limit;
        }

        /**
         * Determines the data tables referenced in the configuration table
         *
         * @param cfgTableName the configuration table name
         * @return the data tables' names
         * @throws SQLException when there is an error determining if the tables exist
         */
        private String[] getDataTableNames(int serv_id) throws SQLException {
            List<String> datatables = new ArrayList<String>(5);
            Connection con = Main.db.getCon();
            DBConnectionInfo info = Main.db.conListInfo.getInfo();
            String schema = info.getSchema();
            Statement stmt = null;
            try {
                stmt = con.createStatement();
                String query = "(SELECT DISTINCT(tableName) FROM " + AbstractDatabaseStructureHandler.CFG + " c JOIN " + AbstractDatabaseStructureHandler.CFG_ITEMS + " i ON i.item_id = c.id WHERE i.serv_id = " + serv_id + ")" + " UNION " + "(SELECT DISTINCT(tableName) FROM " + AbstractDatabaseStructureHandler.CFG + " c JOIN " + AbstractDatabaseStructureHandler.CFG_COMP + " i ON i.comp_id = c.id WHERE i.serv_id = " + serv_id + ")";
                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    datatables.add(rs.getString(1));
                }
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            } finally {
                DatabaseManager.closeStatement(stmt);
            }
            for (int t = datatables.size() - 1; t >= 0; t--) {
                if (!DatabaseManager.tableExists(con, schema, datatables.get(t))) {
                    datatables.remove(t);
                }
            }
            for (int t = datatables.size() - 1; t >= 0; t--) {
                if (!checkStructure(datatables.get(t))) {
                    datatables.remove(t);
                }
            }
            return datatables.toArray(new String[datatables.size()]);
        }
    }

    class TmaxWorker extends TLimitWorker {

        public TmaxWorker() {
            super("SELECT max(t) FROM ");
        }

        @Override
        protected void done() {
            if (!isCancelled()) {
                try {
                    Date[] maximuns = get();
                    Date max = null;
                    if (maximuns != null) {
                        for (Date d : maximuns) {
                            if (d != null && (max == null || max.compareTo(d) < 0)) {
                                max = d;
                            }
                        }
                        if (max != null) {
                            Date tinitMax = (Date) initModel.getEnd();
                            if (tinitMax != null && tinitMax.compareTo(max) > 0) {
                                initModel.setEnd(max);
                            }
                            if (((Date) initModel.getValue()).compareTo(max) > 0) {
                                initModel.setValue(max);
                            }
                            finalModel.setEnd(max);
                            finalModel.setValue(max);
                        }
                    }
                } catch (InterruptedException ex) {
                } catch (ExecutionException ex) {
                }
            }
            if (jCheckBoxTime.isSelected()) {
                jButtonTmin.setEnabled(true);
                jButtonTmax.setEnabled(true);
            }
        }
    }

    class TminWorker extends TLimitWorker {

        public TminWorker() {
            super("SELECT min(t) FROM ");
        }

        @Override
        protected void done() {
            if (!isCancelled()) {
                try {
                    Date[] minimuns = get();
                    Date min = null;
                    if (minimuns != null) {
                        for (Date d : minimuns) {
                            if (d != null && (min == null || min.compareTo(d) > 0)) {
                                min = d;
                            }
                        }
                        if (min != null) {
                            Date tfinalMin = (Date) finalModel.getStart();
                            if (tfinalMin != null && tfinalMin.compareTo(min) < 0) {
                                finalModel.setStart(min);
                            }
                            if (((Date) finalModel.getValue()).compareTo(min) < 0) {
                                finalModel.setValue(min);
                            }
                            initModel.setStart(min);
                            initModel.setValue(min);
                        }
                    }
                } catch (InterruptedException ex) {
                } catch (ExecutionException ex) {
                }
            }
            if (jCheckBoxTime.isSelected()) {
                jButtonTmin.setEnabled(true);
                jButtonTmax.setEnabled(true);
            }
        }
    }

    private class ServerInfoWorker extends SwingWorkerX<Map<Integer, ConnectionInformation>, Object> {

        @Override
        protected Map<Integer, ConnectionInformation> doInBackground() throws Exception {
            final DBConnectionInfo info = Main.db.conListInfo.getInfo();
            final String schema = info.getSchema();
            DatabaseStructureTransformer trans = new DatabaseStructureTransformer();
            final DatabaseVersion version = trans.getTableStructureVersion(Main.db, schema);
            if (version != null) {
                if (version != trans.getCurrentTableStructureVersion()) {
                    EDTWorker<Boolean> worker = new EDTWorker<Boolean>() {

                        @Override
                        protected Boolean performActionInEDT() {
                            Window w = SwingUtilities.getWindowAncestor(ExportJPanel.this);
                            String title = "Database table structure upgrade";
                            String question = "<html><p>The current database schema was used by a previous version of Plantstreamer.<br/> " + "Would you like to upgrade to the newest version?<br/> <br/> It is advised to backup the database before upgrading!</p> </html>";
                            String details = MessageFormat.format("<html><p>The schema ''{0}'' was being used by the Planstreamer version {1}!<br/>" + "Several tables will have to be altered in order to upgrade to the current version.</p></html>", schema, version);
                            int answer = QuestionDialog.display(w, title, question, details, null);
                            return answer == JOptionPane.YES_OPTION;
                        }
                    };
                    worker.runInEDT();
                    Boolean upgrade = worker.get();
                    if (upgrade) {
                        trans.upgradeFrom(Main.db, schema, version);
                    } else {
                        return new HashMap<Integer, ConnectionInformation>(0);
                    }
                }
                return getServers();
            } else {
                return new HashMap<Integer, ConnectionInformation>(0);
            }
        }

        private Map<Integer, ConnectionInformation> getServers() throws SQLException {
            final Map<Integer, ConnectionInformation> servers = new HashMap<Integer, ConnectionInformation>();
            Connection con = Main.db.getCon();
            Statement stmt = null;
            PreparedStatement pstmt = null;
            try {
                stmt = con.createStatement();
                final ResultSet rs = stmt.executeQuery("SELECT DISTINCT serv_id FROM " + AbstractDatabaseStructureHandler.CFG_ITEMS + " UNION " + "SELECT DISTINCT serv_id FROM " + AbstractDatabaseStructureHandler.CFG_COMP);
                pstmt = con.prepareStatement("SELECT host, domain, progid FROM " + AbstractDatabaseStructureHandler.SERVERS + " WHERE serv_id = ? ORDER BY pos LIMIT 1");
                while (rs.next()) {
                    int serv_id = rs.getInt(1);
                    pstmt.setInt(1, serv_id);
                    final ResultSet rs2 = pstmt.executeQuery();
                    rs2.next();
                    ConnectionInformation info = new ConnectionInformation();
                    info.setHost(rs2.getString(1));
                    info.setDomain(rs2.getString(2));
                    info.setProgId(rs2.getString(3));
                    servers.put(serv_id, info);
                }
            } finally {
                DatabaseManager.closeStatements(stmt, pstmt);
            }
            return servers;
        }

        @Override
        protected void done2() {
            Map<Integer, ConnectionInformation> serv_id2servers;
            try {
                serv_id2servers = get();
                ExportJPanel.this.serv_id2servers = serv_id2servers;
                ConnectionInformation oldSelection = (ConnectionInformation) jComboBoxServers.getSelectedItem();
                Collection<ConnectionInformation> servers = serv_id2servers.values();
                DefaultComboBoxModel model = new DefaultComboBoxModel(servers.toArray(new ConnectionInformation[servers.size()]));
                int index = model.getIndexOf(oldSelection);
                if (index == -1) {
                    index = 0;
                }
                jComboBoxServers.setModel(model);
                if (jComboBoxServers.getItemCount() > 0) {
                    jComboBoxServers.setSelectedIndex(index);
                }
                serverSelectionChanged();
            } catch (InterruptedException ex) {
                logger.log(Level.WARNING, ex.getMessage(), ex);
            } catch (ExecutionException ex) {
                logger.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
    }
}
