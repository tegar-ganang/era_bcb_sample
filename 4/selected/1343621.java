package de.jlab.ui.modules.runs;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import de.jlab.GlobalsLocator;
import de.jlab.boards.Board;
import de.jlab.config.runs.Run;
import de.jlab.config.runs.RunConfiguration;
import de.jlab.config.runs.RunDefinition;
import de.jlab.lab.Lab;
import de.jlab.lab.runs.RunDefinitionAnalyzed;

public class RunUICompositionPanel extends JPanel {

    Lab theLab = null;

    DefaultTableModel dtm = new DefaultTableModel(new Object[] { GlobalsLocator.translate("runs.table.header.curve"), GlobalsLocator.translate("runs.table.header.board"), GlobalsLocator.translate("runs.table.header.parameter") }, 0);

    JTable jTableComposition = new JTable(dtm);

    JComboBox jComboBoxRun = new JComboBox();

    JComboBox jComboBoxBoard = new JComboBox();

    JButton jButtonAdd = new JButton(GlobalsLocator.translate("runs.button.add"));

    JButton jButtonRemove = new JButton(GlobalsLocator.translate("runs.button.remove"));

    JScrollPane jScroller = new JScrollPane();

    JPanel helper = new JPanel();

    RunUIMainPanel mainPanel = null;

    public RunUICompositionPanel(Lab lab, RunUIMainPanel mainPanel) {
        theLab = lab;
        this.mainPanel = mainPanel;
        initUI();
        initConfig();
    }

    public void initConfig() {
        dtm.setRowCount(0);
        RunConfiguration runConf = theLab.getConfig().getRunConfiguration();
        if (runConf != null) {
            for (Run currRun : runConf.getRuns()) {
                Board board = theLab.getBoardForCommChannelNameAndAddress(currRun.getChannel(), currRun.getAddress());
                String identifier = "???";
                if (board != null) {
                    identifier = board.getBoardIdentifier();
                }
                RunDefinitionAnalyzed defAnalyzed = theLab.getRunSetsPerRunDefinition().get(currRun.getName());
                dtm.addRow(new Object[] { currRun.getName(), identifier + " - " + currRun.getChannel() + "(" + currRun.getAddress() + ")", defAnalyzed.getParameter() });
            }
        }
        jComboBoxRun.removeAllItems();
        if (theLab.getConfig().getRunDefinitions() != null) {
            for (RunDefinition runDef : theLab.getConfig().getRunDefinitions()) {
                jComboBoxRun.addItem(runDef);
            }
        }
        jComboBoxBoard.removeAllItems();
        for (Board currBoard : theLab.getAllBoardsFound()) {
            jComboBoxBoard.addItem(currBoard);
        }
    }

    private void initUI() {
        this.setLayout(new GridBagLayout());
        jScroller.getViewport().add(jTableComposition);
        jScroller.setPreferredSize(new Dimension(200, 100));
        helper.add(jComboBoxRun);
        helper.add(jComboBoxBoard);
        helper.add(jButtonAdd);
        this.add(helper, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        this.add(jScroller, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        this.add(jButtonRemove, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        jButtonAdd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addRun();
            }
        });
        jButtonRemove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeRun();
            }
        });
        jTableComposition.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                changedSelection();
            }
        });
    }

    private void changedSelection() {
        List<String> curves = new ArrayList<String>();
        int[] rows = jTableComposition.getSelectedRows();
        if (rows != null) {
            for (int i = 0; i < rows.length; ++i) {
                String curve = dtm.getValueAt(rows[i], 0).toString();
                curves.add(curve);
            }
        }
        mainPanel.repaintPreview(curves);
    }

    private void addRun() {
        Run newRun = new Run();
        RunDefinition selectedRunDef = (RunDefinition) jComboBoxRun.getSelectedItem();
        Board selectedBoard = (Board) jComboBoxBoard.getSelectedItem();
        newRun.setName(selectedRunDef.getName());
        newRun.setChannel(selectedBoard.getCommChannel().getChannelName());
        newRun.setAddress(selectedBoard.getAddress());
        dtm.addRow(new Object[] { selectedRunDef.getName(), selectedBoard.getBoardIdentifier() + " - " + newRun.getChannel() + "(" + newRun.getAddress() + ")", selectedRunDef.getParameter() });
        theLab.getConfig().addRunConfigurationRun(newRun);
    }

    private void removeRun() {
        int[] rows = jTableComposition.getSelectedRows();
        if (rows != null) {
            for (int i = rows.length - 1; i >= 0; --i) {
                dtm.removeRow(rows[i]);
                theLab.getConfig().getRunConfiguration().getRuns().remove(rows[i]);
            }
        }
    }

    public void selectAllCurves() {
        if (jTableComposition.getRowCount() > 0) jTableComposition.getSelectionModel().setSelectionInterval(0, jTableComposition.getRowCount() - 1);
    }
}
