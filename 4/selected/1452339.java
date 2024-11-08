package de.jlab.ui.modules.snapshotmanager;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import de.jlab.GlobalsLocator;
import de.jlab.boards.Board;
import de.jlab.boards.BoardSubchannelInfo;
import de.jlab.communication.BoardCommunication;
import de.jlab.config.SnapshotConfig;
import de.jlab.config.SnapshotValueConfig;
import de.jlab.lab.Lab;
import de.jlab.ui.tools.NameDialog;
import de.jlab.ui.tools.Ordering;

public class SnapshotManagerUI extends JPanel {

    private static Logger stdlog = Logger.getLogger(SnapshotManagerUI.class.getName());

    Lab theLab = null;

    JScrollPane jScrollPanePresetSets = new JScrollPane();

    JScrollPane jScrollPaneCurrentPreset = new JScrollPane();

    JSplitPane jSplitPaneMaster = new JSplitPane();

    JButton jButtonActivateSnapshot = new JButton(GlobalsLocator.translate("snapshot-manager-activate-snapshot"));

    JButton jButtonExportSnapshot = new JButton(GlobalsLocator.translate("snapshot-manager-export-snapshot"));

    JButton jButtonTakeSnapshot = new JButton(GlobalsLocator.translate("snapshot-manager-take-snapshot"));

    JButton jButtonRemoveSnapshot = new JButton(GlobalsLocator.translate("snapshot-manager-remove-selected-snapshot"));

    JButton jButtonRemoveSnapshotValue = new JButton(GlobalsLocator.translate("snapshot-manager-remove-selected-snapshot-values"));

    JPanel jPanelControl = new JPanel();

    SnapshotValuesTableModel snapshotValuesTableModel = new SnapshotValuesTableModel();

    DefaultTableModel snapshotTableModel = new DefaultTableModel(new Object[] { "Name" }, 0);

    JTable jTableSnapshots = new JTable(snapshotTableModel);

    JTable jTableCurrentSnapshot = new JTable(snapshotValuesTableModel);

    JPanel jPanelSnapshots = new JPanel();

    JPanel jPanelCurrSnapshot = new JPanel();

    JPanel jPanelSnapshotControls = new JPanel();

    public SnapshotManagerUI(Lab lab) {
        theLab = lab;
        initUI();
    }

    private void initUI() {
        jPanelSnapshots.setLayout(new GridBagLayout());
        jPanelCurrSnapshot.setLayout(new GridBagLayout());
        jScrollPaneCurrentPreset.getViewport().add(jTableCurrentSnapshot);
        jScrollPanePresetSets.getViewport().add(jTableSnapshots);
        this.setLayout(new GridBagLayout());
        this.add(jSplitPaneMaster, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
        this.add(jPanelControl, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
        jSplitPaneMaster.setLeftComponent(jPanelSnapshots);
        jSplitPaneMaster.setRightComponent(jPanelCurrSnapshot);
        jPanelSnapshotControls.add(jButtonActivateSnapshot);
        jPanelSnapshotControls.add(jButtonRemoveSnapshot);
        jPanelSnapshotControls.add(jButtonExportSnapshot);
        jPanelSnapshots.add(jScrollPanePresetSets, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
        jPanelSnapshots.add(jPanelSnapshotControls, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
        jPanelCurrSnapshot.add(jScrollPaneCurrentPreset, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
        jPanelCurrSnapshot.add(jButtonRemoveSnapshotValue, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
        jPanelControl.add(jButtonTakeSnapshot);
        jButtonTakeSnapshot.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                takeSnapshot();
            }
        });
        jButtonActivateSnapshot.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                activateSnapshot();
            }
        });
        jButtonRemoveSnapshot.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeSelectedSnapshot();
            }
        });
        jButtonRemoveSnapshotValue.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeSelectedSnapshotValues();
            }
        });
        jButtonExportSnapshot.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                exportSnapshot();
            }
        });
        jTableSnapshots.getSelectionModel().addListSelectionListener(new PresetSelectListener());
        refreshTables();
    }

    private void activateSnapshot() {
        int row = jTableSnapshots.getSelectedRow();
        if (row >= 0) {
            String selectedSet = (String) ((Vector) snapshotTableModel.getDataVector().get(row)).get(0);
            List<SnapshotValueConfig> foundPresets = theLab.getConfig().getSnapshotByName(selectedSet);
            Set<Integer> alreadySetChannels = new HashSet<Integer>();
            for (SnapshotValueConfig currPreset : foundPresets) {
                setValueToSubchannel(currPreset, alreadySetChannels, foundPresets);
            }
        }
    }

    private void exportSnapshot() {
        DecimalFormat df = new DecimalFormat("#0.###");
        int row = jTableSnapshots.getSelectedRow();
        if (row >= 0) {
            String selectedSet = (String) ((Vector) snapshotTableModel.getDataVector().get(row)).get(0);
            List<SnapshotValueConfig> foundPresets = theLab.getConfig().getSnapshotByName(selectedSet);
            JFileChooser exportFileChooser = new JFileChooser();
            int reply = exportFileChooser.showSaveDialog(GlobalsLocator.getMainFrame());
            if (reply == JFileChooser.APPROVE_OPTION) {
                try {
                    FileOutputStream fos = new FileOutputStream(exportFileChooser.getSelectedFile());
                    String header = "JLab Snapshot Export " + new Date() + "\n";
                    fos.write(header.getBytes());
                    fos.write("Address;Subchannel;Value;Description\n".getBytes());
                    for (SnapshotValueConfig currValue : foundPresets) {
                        String exportString = currValue.getAddress() + ";" + currValue.getSubchannel() + ";" + df.format(currValue.getDoubleValue()) + ";" + currValue.getDescription() + "\n";
                        fos.write(exportString.getBytes());
                    }
                    fos.close();
                } catch (Exception ex) {
                    stdlog.log(Level.SEVERE, "Error in writing Snapshot", ex);
                }
            }
        }
    }

    private void setValueToSubchannel(SnapshotValueConfig currPreset, Set<Integer> alreadySet, List<SnapshotValueConfig> foundPresets) {
        if (currPreset.isReadonly()) return;
        int address = currPreset.getAddress();
        int subchannel = currPreset.getSubchannel();
        Integer addressSubchannelId = address * 10000 + subchannel;
        if (alreadySet.contains(addressSubchannelId)) return;
        if (currPreset.getDependsOnSubchannel() >= 0) {
            for (SnapshotValueConfig dependingPreset : foundPresets) {
                if (dependingPreset.getAddress() == address && dependingPreset.getSubchannel() == currPreset.getDependsOnSubchannel()) {
                    setValueToSubchannel(dependingPreset, alreadySet, foundPresets);
                    break;
                }
            }
        }
        BoardCommunication commChannel = theLab.getCommChannelByNameMap().get(currPreset.getCommChannelName());
        if (currPreset.isEnableEepromWrite()) {
            commChannel.sendCommand(address, 250, 1);
        }
        if (currPreset.isEnableEepromWrite()) {
            if (commChannel.queryDoubleValue(address, subchannel) != currPreset.getDoubleValue()) {
                commChannel.sendCommand(address, 250, 1);
                commChannel.sendCommand(address, subchannel, currPreset.getDoubleValue());
                commChannel.queryValueAsynchronously(address, subchannel);
                alreadySet.add(addressSubchannelId);
            }
        } else {
            commChannel.sendCommand(address, subchannel, currPreset.getDoubleValue());
            commChannel.queryValueAsynchronously(address, subchannel);
            alreadySet.add(addressSubchannelId);
        }
    }

    private void removeSelectedSnapshot() {
        int response = JOptionPane.showConfirmDialog(GlobalsLocator.getMainFrame(), GlobalsLocator.translate("delete_snapshot_warning"), GlobalsLocator.translate("delete_snapshot_header"), JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            int row = jTableSnapshots.getSelectedRow();
            if (row >= 0) {
                String selectedSet = (String) ((Vector) snapshotTableModel.getDataVector().get(row)).get(0);
                theLab.getConfig().removePresetSetByName(selectedSet);
                refreshTables();
            }
        }
    }

    private void removeSelectedSnapshotValues() {
        int response = JOptionPane.showConfirmDialog(GlobalsLocator.getMainFrame(), GlobalsLocator.translate("delete_snapshot_values_warning"), GlobalsLocator.translate("delete_snapshot_values_header"), JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            int[] rows = jTableCurrentSnapshot.getSelectedRows();
            if (rows != null) {
                List<SnapshotValueConfig> snapshotValues = snapshotValuesTableModel.getValuesList();
                for (int i = rows.length - 1; i >= 0; --i) {
                    int currIndex = rows[i];
                    snapshotValues.remove(currIndex);
                }
                snapshotValuesTableModel.setSnapshotValueList(snapshotValues);
            }
        }
    }

    private void takeSnapshot() {
        List<SnapshotValueConfig> snapshotValues = new ArrayList<SnapshotValueConfig>();
        for (Board currBoard : theLab.getAllBoardsFound()) {
            String currModule = currBoard.getBoardInstanceIdentifier();
            List<BoardSubchannelInfo> channelInfos = currBoard.getSnapshotChannels();
            if (channelInfos == null) {
                stdlog.severe("Module " + currModule + " does not provide channels to snapshot");
                continue;
            }
            for (BoardSubchannelInfo currInfo : channelInfos) {
                double value = currBoard.queryDoubleValue(currInfo.getSubchannel());
                SnapshotValueConfig newSnapshotValueConfig = new SnapshotValueConfig(currBoard.getCommChannel().getChannelName(), currBoard.getAddress(), currInfo.getModuleId(), currInfo.getSubchannel(), value, currInfo.isEnableEepromWriteBefore(), currInfo.getDependsOnSubchannel(), currInfo.getDescription());
                newSnapshotValueConfig.setReadonly(currInfo.isReadonly());
                snapshotValues.add(newSnapshotValueConfig);
            }
        }
        NameDialog dlg = new NameDialog(GlobalsLocator.getMainFrame(), GlobalsLocator.translate("enter_snapshotname"), true);
        dlg.pack();
        Ordering.centerDlgInFrame(GlobalsLocator.getMainFrame(), dlg);
        dlg.setVisible(true);
        if (dlg.isOKPressed()) {
            SnapshotConfig newSet = new SnapshotConfig();
            newSet.setName(dlg.getName());
            newSet.setSnapshotValues(snapshotValues);
            theLab.getConfig().getSnapshots().add(newSet);
            refreshTables();
        }
    }

    private void refreshTables() {
        snapshotTableModel.setRowCount(0);
        for (SnapshotConfig currConfig : theLab.getConfig().getSnapshots()) {
            snapshotTableModel.addRow(new Object[] { currConfig.getName() });
        }
        if (snapshotTableModel.getRowCount() == 0) {
            snapshotValuesTableModel.setSnapshotValueList(null);
        }
    }

    class PresetSelectListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            int row = jTableSnapshots.getSelectedRow();
            if (row >= 0) {
                String selectedSnapshot = (String) ((Vector) snapshotTableModel.getDataVector().get(row)).get(0);
                List<SnapshotValueConfig> foundSnapshotValues = theLab.getConfig().getSnapshotByName(selectedSnapshot);
                snapshotValuesTableModel.setSnapshotValueList(foundSnapshotValues);
            }
        }
    }
}
