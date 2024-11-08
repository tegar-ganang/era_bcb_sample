package de.jlab.ui.valuewatch;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import de.jlab.GlobalsLocator;
import de.jlab.boards.Board;
import de.jlab.boards.BoardSubchannelInfo;
import de.jlab.config.ValueWatchConfig;
import de.jlab.lab.Lab;

public class ValueWatchConfiguratorPanel extends JPanel {

    JTabbedPane jTabbedPaneModules = new JTabbedPane();

    Lab theLab = null;

    Map<Board, Set<CheckBoxInfo>> checkBoxesForBoard = new HashMap<Board, Set<CheckBoxInfo>>();

    public ValueWatchConfiguratorPanel(Lab lab) {
        this.theLab = lab;
        initUI();
    }

    private void initUI() {
        this.setLayout(new GridBagLayout());
        this.add(jTabbedPaneModules, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        int colCount = 0;
        for (Board currBoard : theLab.getAllBoardsFound()) {
            Set<JCheckBox> allCheckBoxes = new HashSet<JCheckBox>();
            Set<CheckBoxInfo> allCheckBoxInfos = new HashSet<CheckBoxInfo>();
            JPanel newModulePanel = new JPanel();
            newModulePanel.setLayout(new GridBagLayout());
            jTabbedPaneModules.add(currBoard.getBoardInstanceIdentifier(), newModulePanel);
            List<BoardSubchannelInfo> modules = currBoard.getSnapshotChannels();
            if (modules == null) continue;
            int row = 0;
            int column = 0;
            for (int mod = 0; mod < modules.size(); mod++) {
                JCheckBox newBox = new JCheckBox(modules.get(mod).getDescription());
                newModulePanel.add(newBox, new GridBagConstraints(row, column, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
                allCheckBoxes.add(newBox);
                allCheckBoxInfos.add(new CheckBoxInfo(newBox, modules.get(mod).getSubchannel()));
                column++;
                if (column == 8) {
                    column = 0;
                    row++;
                }
            }
            checkBoxesForBoard.put(currBoard, allCheckBoxInfos);
            JPanel ctrlPanel = new JPanel();
            JButton jButtonEnableAll = new JButton(GlobalsLocator.translate("button-value-watch-enable-all"));
            JButton jButtonDisableAll = new JButton(GlobalsLocator.translate("button-value-watch-disable-all"));
            ctrlPanel.add(jButtonEnableAll);
            ctrlPanel.add(jButtonDisableAll);
            newModulePanel.add(ctrlPanel, new GridBagConstraints(0, 8, row + 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            jButtonEnableAll.addActionListener(new CheckBoxController(allCheckBoxes, true));
            jButtonDisableAll.addActionListener(new CheckBoxController(allCheckBoxes, false));
            colCount++;
        }
    }

    public void setCheckBoxesForConfig(List<ValueWatchConfig> enabledWatches) {
        for (Board currBoard : checkBoxesForBoard.keySet()) {
            Set<CheckBoxInfo> allCheckBoxInfos = checkBoxesForBoard.get(currBoard);
            for (CheckBoxInfo currBox : allCheckBoxInfos) {
                if (enabledWatches == null) {
                    currBox.getCheckBox().setSelected(true);
                    continue;
                }
                boolean enabled = false;
                for (ValueWatchConfig currWatchConfig : enabledWatches) {
                    if (currWatchConfig.getAddress() == currBoard.getAddress() && currWatchConfig.getCommChannelName() == currBoard.getCommChannel().getChannelName() && currWatchConfig.getSubchannel() == currBox.getSubchannel()) {
                        enabled = true;
                        break;
                    }
                }
                currBox.getCheckBox().setSelected(enabled);
            }
        }
    }

    public ArrayList<ValueWatchConfig> getEnabledWatches() {
        ArrayList<ValueWatchConfig> enabledWatches = new ArrayList<ValueWatchConfig>();
        for (Board currBoard : checkBoxesForBoard.keySet()) {
            Set<CheckBoxInfo> allCheckBoxInfos = checkBoxesForBoard.get(currBoard);
            for (CheckBoxInfo currBox : allCheckBoxInfos) {
                if (currBox.getCheckBox().isSelected()) {
                    ValueWatchConfig newConfig = new ValueWatchConfig();
                    newConfig.setAddress(currBoard.getAddress());
                    newConfig.setCommChannelName(currBoard.getCommChannel().getChannelName());
                    newConfig.setSubchannel(currBox.getSubchannel());
                    enabledWatches.add(newConfig);
                }
            }
        }
        return enabledWatches;
    }
}

class CheckBoxInfo {

    private JCheckBox checkBox;

    private int subchannel;

    public CheckBoxInfo(JCheckBox checkBox, int subchannel) {
        super();
        this.checkBox = checkBox;
        this.subchannel = subchannel;
    }

    public JCheckBox getCheckBox() {
        return checkBox;
    }

    public void setCheckBox(JCheckBox checkBox) {
        this.checkBox = checkBox;
    }

    public int getSubchannel() {
        return subchannel;
    }

    public void setSubchannel(int subchannel) {
        this.subchannel = subchannel;
    }
}

class CheckBoxController implements ActionListener {

    Set<JCheckBox> allCheckBoxes = null;

    boolean setFlag = false;

    public void actionPerformed(ActionEvent e) {
        for (JCheckBox currBox : allCheckBoxes) {
            currBox.setSelected(setFlag);
        }
    }

    public CheckBoxController(Set<JCheckBox> allCheckBoxes, boolean enable) {
        super();
        this.allCheckBoxes = allCheckBoxes;
        this.setFlag = enable;
    }
}
