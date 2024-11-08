package be.lassi.ui.group;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import be.lassi.context.ShowContext;
import be.lassi.domain.Channel;
import be.lassi.domain.Group;
import be.lassi.domain.Groups;
import be.lassi.ui.icons.Icons;
import be.lassi.ui.util.Components;
import be.lassi.ui.util.SelectionInterval;
import be.lassi.util.Help;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 *
 *
 *
 */
public class GroupDefinitionFrame extends AbstractGroupFrame {

    private JFrame lessFrame;

    private JTable inGroupTable;

    private JTable notInGroupTable;

    private InGroupTableModel inGroupTableModel;

    private NotInGroupTableModel notInGroupTableModel;

    private JButton buttonAddGroup = new JButton("Add");

    private JButton buttonRemoveGroup = new JButton("Remove");

    private JButton buttonUpGroup = new JButton(Icons.get("up.gif"));

    private JButton buttonDownGroup = new JButton(Icons.get("down.gif"));

    private JButton buttonLess = new JButton("Less", Icons.get("left.gif"));

    private JButton buttonAllInGroup = new JButton("All");

    private JButton buttonNoneInGroup = new JButton("None");

    private JButton buttonAddToGroup = new JButton(Icons.get("left.gif"));

    private JButton buttonRemoveFromGroup = new JButton(Icons.get("right.gif"));

    private JButton buttonUpChannel = new JButton(Icons.get("up.gif"));

    private JButton buttonDownChannel = new JButton(Icons.get("down.gif"));

    private JButton buttonAllNotInGroup = new JButton("All");

    private JButton buttonNoneNotInGroup = new JButton("None");

    private JCheckBox notInAnyGroup = new JCheckBox("Any", true);

    private JLabel notInGroupLabel;

    public GroupDefinitionFrame(final ShowContext context) {
        super(context, "Channel Groups");
        init();
        fixMinimumSize();
        build();
        updateWidgets();
    }

    public void setLessFrame(final JFrame lessFrame) {
        this.lessFrame = lessFrame;
    }

    protected JComponent createPanel() {
        FormLayout layout = new FormLayout("min:grow, 12dlu, min:grow, 4dlu, pref, 4dlu, min:grow", "pref, 4dlu, 50dlu, 4dlu, pref, 4dlu, pref, 4dlu, pref:grow, 4dlu, pref");
        layout.setColumnGroups(new int[][] { { 3, 7 } });
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        CellConstraints cc = new CellConstraints();
        builder.addTitle("Groups", cc.xy(1, 1));
        builder.add(createTableGroups(), cc.xywh(1, 3, 1, 7));
        builder.add(createButtonPanelGroups(), cc.xy(1, 11));
        builder.addTitle("In group", cc.xy(3, 1));
        builder.add(createTableInGroup(), cc.xywh(3, 3, 1, 7));
        builder.add(createButtonPanelInGroup(), cc.xy(3, 11));
        builder.add(buttonAddToGroup, cc.xy(5, 5));
        builder.add(buttonRemoveFromGroup, cc.xy(5, 7));
        builder.add(createTopRight(), cc.xy(7, 1));
        builder.add(createTableNotInGroup(), cc.xywh(7, 3, 1, 7));
        builder.add(createButtonPanelNotInGroup(), cc.xy(7, 11));
        Dimension d1 = buttonAddGroup.getPreferredSize();
        Dimension d2 = new Dimension(d1.height, d1.height);
        buttonAddToGroup.setPreferredSize(d2);
        buttonRemoveFromGroup.setPreferredSize(d2);
        Components buttonGroup = new Components();
        buttonGroup.add(buttonAddGroup);
        buttonGroup.add(buttonRemoveGroup);
        buttonGroup.add(buttonLess);
        buttonGroup.add(buttonAllInGroup);
        buttonGroup.add(buttonNoneInGroup);
        buttonGroup.add(buttonAllNotInGroup);
        buttonGroup.add(buttonNoneNotInGroup);
        buttonGroup.equalize();
        return builder.getPanel();
    }

    private JPanel createTopRight() {
        FormLayout layout = new FormLayout("pref, 4dlu:grow, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        notInGroupLabel = builder.getComponentFactory().createTitle("Not in group");
        builder.add(notInGroupLabel, cc.xy(1, 1));
        builder.add(Help.createHelpButton(), cc.xy(3, 1));
        return builder.getPanel();
    }

    private JPanel createButtonPanelGroups() {
        FormLayout layout = new FormLayout("pref, 4dlu, pref, 4dlu:grow, pref,  4dlu,  pref, 4dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(buttonAddGroup, cc.xy(1, 1));
        builder.add(buttonRemoveGroup, cc.xy(3, 1));
        builder.add(buttonUpGroup, cc.xy(5, 1));
        builder.add(buttonDownGroup, cc.xy(7, 1));
        builder.add(buttonLess, cc.xy(9, 1));
        Dimension d1 = buttonAddGroup.getPreferredSize();
        Dimension d2 = new Dimension(d1.height, d1.height);
        buttonUpGroup.setPreferredSize(d2);
        buttonDownGroup.setPreferredSize(d2);
        return builder.getPanel();
    }

    private JPanel createButtonPanelInGroup() {
        FormLayout layout = new FormLayout("pref, 4dlu, pref, 4dlu:grow, pref,  4dlu,  pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(buttonAllInGroup, cc.xy(1, 1));
        builder.add(buttonNoneInGroup, cc.xy(3, 1));
        builder.add(buttonUpChannel, cc.xy(5, 1));
        builder.add(buttonDownChannel, cc.xy(7, 1));
        Dimension d1 = buttonAddGroup.getPreferredSize();
        Dimension d2 = new Dimension(d1.height, d1.height);
        buttonUpChannel.setPreferredSize(d2);
        buttonDownChannel.setPreferredSize(d2);
        return builder.getPanel();
    }

    private JPanel createButtonPanelNotInGroup() {
        FormLayout layout = new FormLayout("pref, 4dlu, pref, 4dlu:grow, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(buttonAllNotInGroup, cc.xy(1, 1));
        builder.add(buttonNoneNotInGroup, cc.xy(3, 1));
        builder.add(notInAnyGroup, cc.xy(5, 1));
        return builder.getPanel();
    }

    private JComponent createTableInGroup() {
        inGroupTableModel = new InGroupTableModel(this);
        inGroupTable = new JTable(inGroupTableModel);
        int w = new JLabel("99999").getPreferredSize().width;
        inGroupTable.getColumnModel().getColumn(0).setMaxWidth(w);
        inGroupTable.getColumnModel().getColumn(0).setMinWidth(w);
        inGroupTable.getTableHeader().setReorderingAllowed(false);
        inGroupTable.setShowGrid(false);
        inGroupTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent e) {
                updateWidgets();
            }
        });
        return new JScrollPane(inGroupTable);
    }

    private JComponent createTableNotInGroup() {
        notInGroupTableModel = new NotInGroupTableModel(this);
        notInGroupTable = new JTable(notInGroupTableModel);
        int w = new JLabel("99999").getPreferredSize().width;
        notInGroupTable.getColumnModel().getColumn(0).setMaxWidth(w);
        notInGroupTable.getColumnModel().getColumn(0).setMinWidth(w);
        notInGroupTable.getTableHeader().setReorderingAllowed(false);
        notInGroupTable.setShowGrid(false);
        notInGroupTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent e) {
                updateWidgets();
            }
        });
        return new JScrollPane(notInGroupTable);
    }

    private void build() {
        Help.enable(this, "groupDefinition");
        Help.enable(getGroupTable(), "groupDefinition.groupTable");
        Help.enable(inGroupTable, "groupDefinition.inGroupTable");
        Help.enable(notInGroupTable, "groupDefinition.notInGroupTable");
        Help.enable(buttonAddGroup, "groupDefinition.addGroup");
        Help.enable(buttonRemoveGroup, "groupDefinition.removeGroup");
        Help.enable(buttonUpGroup, "groupDefinition.upGroup");
        Help.enable(buttonDownGroup, "groupDefinition.downGroup");
        Help.enable(buttonLess, "groupDefinition.less");
        Help.enable(buttonAllInGroup, "groupDefinition.allInGroup");
        Help.enable(buttonNoneInGroup, "groupDefinition.noneInGroup");
        Help.enable(buttonAddToGroup, "groupDefinition.addToGroup");
        Help.enable(buttonRemoveFromGroup, "groupDefinition.removeFromGroup");
        Help.enable(buttonUpChannel, "groupDefinition.upChannel");
        Help.enable(buttonDownChannel, "groupDefinition.downChannel");
        Help.enable(buttonAllNotInGroup, "groupDefinition.allNotInGroup");
        Help.enable(buttonNoneNotInGroup, "groupDefinition.noneNotInGroup");
        Help.enable(notInAnyGroup, "groupDefinition.notInAnyGroup");
        buttonAddGroup.setToolTipText("Add a new channel group");
        buttonRemoveGroup.setToolTipText("Remove the currently selected channel group");
        buttonUpGroup.setToolTipText("Move the currently selected channel group(s) up in the list");
        buttonDownGroup.setToolTipText("Move the currently selected channel group(s) down in the list");
        buttonLess.setToolTipText("Switch to the 'simple' group window");
        buttonAllInGroup.setToolTipText("Select all channels in the currently selected group");
        buttonNoneInGroup.setToolTipText("Clear the selection of the channels in the currently selected group");
        buttonAddToGroup.setToolTipText("Add the channels selected in the list on the right to the channels of the selected group");
        buttonRemoveFromGroup.setToolTipText("Remove the channels selected in the list on the left from the channels of the selected group");
        buttonUpChannel.setToolTipText("Move the selected channel(s) up in the list");
        buttonDownChannel.setToolTipText("Move the selected channel(s) down in the list");
        buttonAllNotInGroup.setToolTipText("Select all channels that are not in the currently selected group");
        buttonNoneNotInGroup.setToolTipText("Clear the selection of the channels that are not in the currently selected group");
        notInAnyGroup.setToolTipText("Only show channels that are not assigned to ANY of the groups yet");
        buttonAddGroup.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionAddGroup();
            }
        });
        buttonRemoveGroup.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionRemoveGroup();
            }
        });
        buttonUpGroup.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionUpGroup();
            }
        });
        buttonDownGroup.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionDownGroup();
            }
        });
        buttonLess.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionLess();
            }
        });
        buttonAllInGroup.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionSelectAllInGroup();
            }
        });
        buttonNoneInGroup.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionSelectNoneInGroup();
            }
        });
        buttonAddToGroup.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionAddToGroup();
            }
        });
        buttonRemoveFromGroup.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionRemoveFromGroup();
            }
        });
        buttonUpChannel.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionUpChannel();
            }
        });
        buttonDownChannel.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionDownChannel();
            }
        });
        buttonAllNotInGroup.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionSelectAllNotInGroup();
            }
        });
        buttonNoneNotInGroup.addActionListener(new AbstractAction() {

            public void actionPerformed(final ActionEvent evt) {
                actionSelectNoneNotInGroup();
            }
        });
        getGroupTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent e) {
                groupSelectionChanged();
            }
        });
        notInAnyGroup.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (notInAnyGroup.isSelected()) {
                    notInGroupLabel.setText("Not in any group");
                } else {
                    notInGroupLabel.setText("Not in group");
                }
                notInGroupTableModel.fireTableDataChanged();
                updateWidgets();
            }
        });
    }

    private void actionAddGroup() {
        int groupCount = getContext().getShow().getGroups().size();
        String name = "Group " + (groupCount + 1);
        Group group = new Group();
        group.setName(name);
        getContext().getShow().getGroups().add(group);
        getGroupTableModel().fireTableDataChanged();
        getGroupTable().getSelectionModel().setSelectionInterval(groupCount, groupCount);
        updateWidgets();
    }

    private void actionRemoveGroup() {
        int index = getGroupTable().getSelectedRow();
        if (index >= 0) {
            getShow().getGroups().remove(index);
            getGroupTableModel().fireTableDataChanged();
        }
        updateWidgets();
    }

    private void actionUpGroup() {
        int[] indexes = getGroupTable().getSelectedRows();
        SelectionInterval selection = getShow().getGroups().moveUp(indexes);
        getGroupTableModel().fireTableDataChanged();
        getGroupTable().getSelectionModel().setSelectionInterval(selection.getIndex1(), selection.getIndex2());
    }

    private void actionDownGroup() {
        int[] indexes = getGroupTable().getSelectedRows();
        SelectionInterval selection = getShow().getGroups().moveDown(indexes);
        getGroupTableModel().fireTableDataChanged();
        getGroupTable().getSelectionModel().setSelectionInterval(selection.getIndex1(), selection.getIndex2());
    }

    private void actionLess() {
        Rectangle thisBounds = getBounds();
        Rectangle bounds = lessFrame.getBounds();
        bounds.x = thisBounds.x;
        bounds.y = thisBounds.y;
        lessFrame.setBounds(bounds);
        lessFrame.setVisible(true);
        this.setVisible(false);
    }

    private void actionSelectAllInGroup() {
        inGroupTable.selectAll();
        updateWidgets();
    }

    private void actionSelectNoneInGroup() {
        inGroupTable.clearSelection();
        updateWidgets();
    }

    private void actionUpChannel() {
        int[] indexes = inGroupTable.getSelectedRows();
        SelectionInterval selection = getSelectedGroup().moveUp(indexes);
        inGroupTableModel.fireTableDataChanged();
        inGroupTable.getSelectionModel().setSelectionInterval(selection.getIndex1(), selection.getIndex2());
    }

    private void actionDownChannel() {
        int[] indexes = inGroupTable.getSelectedRows();
        SelectionInterval selection = getSelectedGroup().moveDown(indexes);
        inGroupTableModel.fireTableDataChanged();
        inGroupTable.getSelectionModel().setSelectionInterval(selection.getIndex1(), selection.getIndex2());
    }

    private void actionSelectAllNotInGroup() {
        int index1 = getChannelsNotInGroupCount() - 1;
        notInGroupTable.getSelectionModel().setSelectionInterval(0, index1);
        updateWidgets();
    }

    private void actionSelectNoneNotInGroup() {
        notInGroupTable.getSelectionModel().clearSelection();
        updateWidgets();
    }

    private void actionAddToGroup() {
        int[] channelIndexes = getChannelsNotInGroup();
        int[] rows = notInGroupTable.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            int channelIndex = channelIndexes[rows[i]];
            Channel channel = getContext().getShow().getChannels().get(channelIndex);
            getSelectedGroup().add(channel);
        }
        updateWidgets();
        inGroupTableModel.fireTableDataChanged();
        notInGroupTableModel.fireTableDataChanged();
    }

    private void actionRemoveFromGroup() {
        int[] rows = inGroupTable.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) {
            getSelectedGroup().remove(rows[i]);
        }
        updateWidgets();
        inGroupTableModel.fireTableDataChanged();
        notInGroupTableModel.fireTableDataChanged();
    }

    public Group getSelectedGroup() {
        Group group = null;
        int index = getSelectedGroupIndex();
        if (index >= 0) {
            group = getContext().getShow().getGroups().get(index);
        }
        return group;
    }

    private int getSelectedGroupIndex() {
        int index = -1;
        if (getGroupTable().getSelectedRowCount() == 1) {
            index = getGroupTable().getSelectedRow();
        }
        return index;
    }

    public int[] getChannelsNotInGroup() {
        Groups groups = getContext().getShow().getGroups();
        boolean any = notInAnyGroup.isSelected();
        int groupIndex = getSelectedGroupIndex();
        int totalChannelCount = getContext().getShow().getNumberOfChannels();
        int[] indexes = groups.getChannelsNotInGroup(any, groupIndex, totalChannelCount);
        return indexes;
    }

    public int getChannelsNotInGroupCount() {
        Groups groups = getContext().getShow().getGroups();
        boolean any = notInAnyGroup.isSelected();
        int groupIndex = getSelectedGroupIndex();
        int totalChannelCount = getContext().getShow().getNumberOfChannels();
        int channelCount = groups.getChannelsNotInGroupCount(any, groupIndex, totalChannelCount);
        return channelCount;
    }

    private void updateWidgets() {
        boolean groupSelected = getGroupTable().getSelectedRowCount() > 0;
        buttonRemoveGroup.setEnabled(groupSelected);
        notInGroupTable.setEnabled(groupSelected);
        boolean up = false;
        boolean down = false;
        if (getGroupTable().getSelectedRowCount() > 0) {
            int[] indexes = getGroupTable().getSelectedRows();
            up = indexes[0] > 0;
            int last = indexes[indexes.length - 1];
            down = last < (getGroupTable().getModel().getRowCount() - 1);
        }
        buttonUpGroup.setEnabled(up);
        buttonDownGroup.setEnabled(down);
        buttonAllInGroup.setEnabled(inGroupTableModel.getRowCount() > 0);
        buttonNoneInGroup.setEnabled(inGroupTable.getSelectedRowCount() > 0);
        up = false;
        down = false;
        if (inGroupTable.getSelectedRowCount() > 0) {
            int[] indexes = inGroupTable.getSelectedRows();
            up = indexes[0] > 0;
            int last = indexes[indexes.length - 1];
            down = last < (inGroupTable.getModel().getRowCount() - 1);
        }
        buttonUpChannel.setEnabled(up);
        buttonDownChannel.setEnabled(down);
        buttonAddToGroup.setEnabled(groupSelected && notInGroupTable.getSelectedRowCount() > 0);
        buttonRemoveFromGroup.setEnabled(groupSelected && inGroupTable.getSelectedRowCount() > 0);
        buttonAllNotInGroup.setEnabled(groupSelected && notInGroupTableModel.getRowCount() > 0);
        buttonNoneNotInGroup.setEnabled(notInGroupTable.getSelectedRowCount() > 0);
    }

    private void groupSelectionChanged() {
        inGroupTableModel.fireTableDataChanged();
        notInGroupTableModel.fireTableDataChanged();
        updateWidgets();
    }
}
