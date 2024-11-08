package be.lassi.ui.group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import be.lassi.base.BooleanHolder;
import be.lassi.base.BooleanListener;
import be.lassi.base.NameListener;
import be.lassi.base.NamedObject;
import be.lassi.context.ShowContext;
import be.lassi.context.ShowContextListener;
import be.lassi.domain.Channel;
import be.lassi.domain.Channels;
import be.lassi.domain.Group;
import be.lassi.domain.Groups;
import be.lassi.ui.util.LassiAction;
import be.lassi.ui.util.SelectionInterval;
import be.lassi.ui.util.table.RowSelectionModel;
import be.lassi.util.NLS;

public class GroupPresentationModel {

    private final ShowContext context;

    private final GroupTableModel groupsTableModel;

    private final ChannelsTableModel inGroupTableModel;

    private final ChannelsTableModel notInGroupTableModel;

    private final RowSelectionModel groupsSelectionModel = new RowSelectionModel();

    private final RowSelectionModel inGroupSelectionModel = new RowSelectionModel();

    private final RowSelectionModel notInGroupSelectionModel = new RowSelectionModel();

    private final LassiAction actionEnableAllGroups = new ActionEnableAllGroups();

    private final LassiAction actionDisableAllGroups = new ActionDisableAllGroups();

    private final LassiAction actionAddGroup = new ActionAddGroup();

    private final LassiAction actionRemoveGroup = new ActionRemoveGroup();

    private final LassiAction actionMoveGroupsUp = new ActionMoveGroupsUp();

    private final LassiAction actionMoveGroupsDown = new ActionMoveGroupsDown();

    private final LassiAction actionSelectAllInGroup = new ActionSelectAllInGroup();

    private final LassiAction actionSelectNoneInGroup = new ActionSelectNoneInGroup();

    private final LassiAction actionAddToGroup = new ActionAddToGroup();

    private final LassiAction actionRemoveFromGroup = new ActionRemoveFromGroup();

    private final LassiAction actionUpChannel = new ActionUpChannel();

    private final LassiAction actionDownChannel = new ActionDownChannel();

    private final LassiAction actionAllNotInGroup = new ActionAllNotInGroup();

    private final LassiAction actionNoneNotInGroup = new ActionNoneNotInGroup();

    private final LassiAction actionNotInAnyGroup = new ActionNotInAnyGroup();

    private Group selectedGroup = null;

    private final List<Channel> channelsInSelectedGroup = new ArrayList<Channel>();

    private final List<Channel> channelsNotInSelectedGroup = new ArrayList<Channel>();

    private final BooleanHolder notInAnyGroup = new BooleanHolder();

    private final NameListener channelNameListener = new MyNameListener();

    public GroupPresentationModel(final ShowContext context) {
        this.context = context;
        groupsTableModel = new GroupTableModel(context);
        inGroupTableModel = new ChannelsTableModel(channelsInSelectedGroup);
        notInGroupTableModel = new ChannelsTableModel(channelsNotInSelectedGroup);
        groupsSelectionModel.addListSelectionListener(new GroupSelectionListener());
        notInAnyGroup.add(new NotInAnyGroupListener());
        ListSelectionListener channelSelectionListener = new ChannelSelectionListener();
        inGroupSelectionModel.addListSelectionListener(channelSelectionListener);
        notInGroupSelectionModel.addListSelectionListener(channelSelectionListener);
        ShowContextListener listener = new MyShowContextListener();
        context.addShowContextListener(listener);
        listener.postShowChange();
    }

    public LassiAction getActionEnableAllGroups() {
        return actionEnableAllGroups;
    }

    public LassiAction getActionDisableAllGroups() {
        return actionDisableAllGroups;
    }

    public LassiAction getActionAddGroup() {
        return actionAddGroup;
    }

    public LassiAction getActionRemoveGroup() {
        return actionRemoveGroup;
    }

    public LassiAction getActionMoveGroupsUp() {
        return actionMoveGroupsUp;
    }

    public LassiAction getActionMoveGroupsDown() {
        return actionMoveGroupsDown;
    }

    public LassiAction getActionSelectAllInGroup() {
        return actionSelectAllInGroup;
    }

    public LassiAction getActionSelectNoneInGroup() {
        return actionSelectNoneInGroup;
    }

    public LassiAction getActionAddToGroup() {
        return actionAddToGroup;
    }

    public LassiAction getActionRemoveFromGroup() {
        return actionRemoveFromGroup;
    }

    public LassiAction getActionUpChannel() {
        return actionUpChannel;
    }

    public LassiAction getActionDownChannel() {
        return actionDownChannel;
    }

    public LassiAction getActionAllNotInGroup() {
        return actionAllNotInGroup;
    }

    public LassiAction getActionNoneNotInGroup() {
        return actionNoneNotInGroup;
    }

    public LassiAction getActionNotInAnyGroup() {
        return actionNotInAnyGroup;
    }

    public TableModel getGroupsTableModel() {
        return groupsTableModel;
    }

    public TableModel getInGroupTableModel() {
        return inGroupTableModel;
    }

    public TableModel getNotInGroupTableModel() {
        return notInGroupTableModel;
    }

    public ListSelectionModel getGroupsSelectionModel() {
        return groupsSelectionModel;
    }

    public ListSelectionModel getInGroupSelectionModel() {
        return inGroupSelectionModel;
    }

    public ListSelectionModel getNotInGroupSelectionModel() {
        return notInGroupSelectionModel;
    }

    private class ActionEnableAllGroups extends LassiAction {

        public ActionEnableAllGroups() {
            super(NLS.get("groups.action.all"));
        }

        public void action() {
            getGroups().setAllEnabled(true);
            groupsTableModel.fireTableDataChanged();
            updateActions();
        }
    }

    private class ActionDisableAllGroups extends LassiAction {

        public ActionDisableAllGroups() {
            super(NLS.get("groups.action.none"));
        }

        public void action() {
            getGroups().setAllEnabled(false);
            groupsTableModel.fireTableDataChanged();
            updateActions();
        }
    }

    private class ActionAddGroup extends LassiAction {

        public ActionAddGroup() {
            super(NLS.get("groups.action.add"));
        }

        public void action() {
            int groupCount = getGroups().size();
            String name = NLS.get("groups.default.groupname") + (groupCount + 1);
            Group group = new Group(context.getDirtyShow());
            group.setName(name);
            getGroups().add(group);
            updateChannels();
        }
    }

    private class ActionRemoveGroup extends LassiAction {

        public ActionRemoveGroup() {
            super(NLS.get("groups.action.remove"));
        }

        public void action() {
            int[] indexes = groupsSelectionModel.getSelectedRows();
            Arrays.sort(indexes);
            for (int i = indexes.length - 1; i >= 0; i--) {
                getGroups().remove(indexes[i]);
            }
            updateChannels();
        }
    }

    private class ActionMoveGroupsUp extends LassiAction {

        public void action() {
            int[] indexes = groupsSelectionModel.getSelectedRows();
            SelectionInterval selection = getGroups().moveUp(indexes);
            groupsTableModel.fireTableDataChanged();
            groupsSelectionModel.setSelectionInterval(selection.getIndex1(), selection.getIndex2());
        }
    }

    private class ActionMoveGroupsDown extends LassiAction {

        public void action() {
            int[] indexes = groupsSelectionModel.getSelectedRows();
            SelectionInterval selection = getGroups().moveDown(indexes);
            groupsTableModel.fireTableDataChanged();
            groupsSelectionModel.setSelectionInterval(selection.getIndex1(), selection.getIndex2());
        }
    }

    private class ActionSelectAllInGroup extends LassiAction {

        public ActionSelectAllInGroup() {
            super(NLS.get("groups.inGroup.all"));
        }

        public void action() {
            int last = inGroupTableModel.getRowCount() - 1;
            inGroupSelectionModel.setSelectionInterval(0, last);
            updateActions();
        }
    }

    private class ActionSelectNoneInGroup extends LassiAction {

        public ActionSelectNoneInGroup() {
            super(NLS.get("groups.inGroup.none"));
        }

        public void action() {
            inGroupSelectionModel.clearSelection();
            updateActions();
        }
    }

    private class ActionAddToGroup extends LassiAction {

        public void action() {
            int[] rows = notInGroupSelectionModel.getSelectedRows();
            for (int i = 0; i < rows.length; i++) {
                Channel channel = channelsNotInSelectedGroup.get(i);
                selectedGroup.add(channel);
            }
            updateChannels();
        }
    }

    private class ActionRemoveFromGroup extends LassiAction {

        public void action() {
            int[] rows = inGroupSelectionModel.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) {
                selectedGroup.remove(rows[i]);
            }
            updateChannels();
        }
    }

    private class ActionUpChannel extends LassiAction {

        public void action() {
            int[] indexes = inGroupSelectionModel.getSelectedRows();
            SelectionInterval selection = selectedGroup.moveUp(indexes);
            updateChannels();
            inGroupSelectionModel.setSelectionInterval(selection.getIndex1(), selection.getIndex2());
        }
    }

    private class ActionDownChannel extends LassiAction {

        public void action() {
            int[] indexes = inGroupSelectionModel.getSelectedRows();
            SelectionInterval selection = selectedGroup.moveDown(indexes);
            updateChannels();
            inGroupSelectionModel.setSelectionInterval(selection.getIndex1(), selection.getIndex2());
        }
    }

    private class ActionAllNotInGroup extends LassiAction {

        public ActionAllNotInGroup() {
            super(NLS.get("groups.notInGroup.all"));
        }

        public void action() {
            int index1 = channelsNotInSelectedGroup.size() - 1;
            notInGroupSelectionModel.setSelectionInterval(0, index1);
            updateActions();
        }
    }

    private class ActionNoneNotInGroup extends LassiAction {

        public ActionNoneNotInGroup() {
            super(NLS.get("groups.notInGroup.none"));
        }

        public void action() {
            notInGroupSelectionModel.clearSelection();
            updateActions();
        }
    }

    private class ActionNotInAnyGroup extends LassiAction {

        public ActionNotInAnyGroup() {
            super(NLS.get(""));
        }

        public void action() {
            notInGroupTableModel.fireTableDataChanged();
        }
    }

    private void updateActions() {
        int groupCount = getGroups().size();
        int enabledGroupCount = getGroups().getEnabledGroupCount();
        actionEnableAllGroups.setEnabled(groupCount > 0 && enabledGroupCount < groupCount);
        actionDisableAllGroups.setEnabled(enabledGroupCount > 0);
        boolean groupSelected = groupsSelectionModel.getSelectedRowCount() > 0;
        actionRemoveGroup.setEnabled(groupSelected);
        boolean up = false;
        boolean down = false;
        if (groupsSelectionModel.getSelectedRowCount() > 0) {
            int[] indexes = groupsSelectionModel.getSelectedRows();
            up = indexes[0] > 0;
            int last = indexes[indexes.length - 1];
            down = last < (groupsTableModel.getRowCount() - 1);
        }
        actionMoveGroupsUp.setEnabled(up);
        actionMoveGroupsDown.setEnabled(down);
        actionSelectAllInGroup.setEnabled(inGroupTableModel.getRowCount() > 0);
        actionSelectNoneInGroup.setEnabled(inGroupSelectionModel.getSelectedRowCount() > 0);
        up = false;
        down = false;
        if (inGroupSelectionModel.getSelectedRowCount() > 0) {
            int[] indexes = inGroupSelectionModel.getSelectedRows();
            up = indexes[0] > 0;
            int last = indexes[indexes.length - 1];
            down = last < (inGroupTableModel.getRowCount() - 1);
        }
        actionUpChannel.setEnabled(up);
        actionDownChannel.setEnabled(down);
        actionAddToGroup.setEnabled(groupSelected && notInGroupSelectionModel.getSelectedRowCount() > 0);
        actionRemoveFromGroup.setEnabled(groupSelected && inGroupSelectionModel.getSelectedRowCount() > 0);
        actionAllNotInGroup.setEnabled(groupSelected && notInGroupTableModel.getRowCount() > 0);
        actionNoneNotInGroup.setEnabled(notInGroupSelectionModel.getSelectedRowCount() > 0);
    }

    private Groups getGroups() {
        return context.getShow().getGroups();
    }

    private class GroupSelectionListener implements ListSelectionListener {

        public void valueChanged(final ListSelectionEvent e) {
            int[] rows = groupsSelectionModel.getSelectedRows();
            if (rows.length == 1) {
                selectedGroup = getGroups().get(rows[0]);
            } else {
                selectedGroup = null;
            }
            updateChannels();
        }
    }

    private class ChannelSelectionListener implements ListSelectionListener {

        public void valueChanged(final ListSelectionEvent e) {
            updateActions();
        }
    }

    private void updateChannels() {
        channelsInSelectedGroup.clear();
        channelsNotInSelectedGroup.clear();
        if (selectedGroup != null) {
            for (Channel channel : selectedGroup.getChannels()) {
                channelsInSelectedGroup.add(channel);
            }
            if (notInAnyGroup.getValue()) {
                Groups groups = getGroups();
                Channels channels = context.getShow().getChannels();
                for (Channel channel : channels) {
                    if (!groups.includes(channel)) {
                        channelsNotInSelectedGroup.add(channel);
                    }
                }
            } else {
                Channels channels = context.getShow().getChannels();
                for (Channel channel : channels) {
                    if (!selectedGroup.includes(channel)) {
                        channelsNotInSelectedGroup.add(channel);
                    }
                }
            }
        } else {
            if (notInAnyGroup.getValue()) {
                Groups groups = getGroups();
                Channels channels = context.getShow().getChannels();
                for (Channel channel : channels) {
                    if (!groups.includes(channel)) {
                        channelsNotInSelectedGroup.add(channel);
                    }
                }
            } else {
                Channels channels = context.getShow().getChannels();
                for (Channel channel : channels) {
                    channelsNotInSelectedGroup.add(channel);
                }
            }
        }
        inGroupTableModel.fireTableDataChanged();
        notInGroupTableModel.fireTableDataChanged();
        updateActions();
    }

    private class NotInAnyGroupListener implements BooleanListener {

        public void changed() {
            updateChannels();
        }
    }

    public BooleanHolder getNotInAnyGroup() {
        return notInAnyGroup;
    }

    private class MyShowContextListener implements ShowContextListener {

        public void postShowChange() {
            Channels channels = context.getShow().getChannels();
            for (Channel channel : channels) {
                channel.addNameListener(channelNameListener);
            }
            updateChannels();
        }

        public void preShowChange() {
            Channels channels = context.getShow().getChannels();
            for (Channel channel : channels) {
                channel.removeNameListener(channelNameListener);
            }
        }
    }

    private class MyNameListener implements NameListener {

        public void nameChanged(NamedObject object) {
            inGroupTableModel.fireTableDataChanged();
            notInGroupTableModel.fireTableDataChanged();
        }
    }
}
