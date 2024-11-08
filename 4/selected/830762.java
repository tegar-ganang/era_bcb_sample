package be.lassi.ui.group;

import static be.lassi.util.Util.newArrayList;
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
import be.lassi.base.StringHolder;
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

public class GroupsPresentationModel {

    private final ShowContext context;

    private final GroupsTableModel groupsTableModel;

    private final ChannelsTableModel inGroupTableModel;

    private final ChannelsTableModel notInGroupTableModel;

    private final RowSelectionModel groupsSelectionModel = new RowSelectionModel();

    private final RowSelectionModel inGroupSelectionModel = new RowSelectionModel();

    private final RowSelectionModel notInGroupSelectionModel = new RowSelectionModel();

    private final LassiAction actionGroupsAdd = new ActionGroupsAdd();

    private final LassiAction actionGroupsRemove = new ActionGroupsRemove();

    private final LassiAction actionGroupsUp = new ActionGroupsUp();

    private final LassiAction actionGroupsDown = new ActionGroupsDown();

    private final LassiAction actionInGroupAll = new ActionInGroupAll();

    private final LassiAction actionInGroupNone = new ActionInGroupNone();

    private final LassiAction actionInGroupUp = new ActionInGroupUp();

    private final LassiAction actionInGroupDown = new ActionInGroupDown();

    private final LassiAction actionNotInGroupAll = new ActionNotInGroupAll();

    private final LassiAction actionNotInGroupNone = new ActionNotInGroupNone();

    private final LassiAction actionNotInAnyGroup = new ActionNotInAnyGroup();

    private final LassiAction actionAddToGroup = new ActionAddToGroup();

    private final LassiAction actionRemoveFromGroup = new ActionRemoveFromGroup();

    private final LassiAction actionAny = new ActionAny();

    private Group selectedGroup = null;

    private final List<Channel> channelsInSelectedGroup = newArrayList();

    private final List<Channel> channelsNotInSelectedGroup = newArrayList();

    private final BooleanHolder notInAnyGroup = new BooleanHolder("", true);

    private final StringHolder notInGroupLabel = new StringHolder("", NLS.get("groups.label.notInAnyGroup"));

    private final NameListener channelNameListener = new MyNameListener();

    public GroupsPresentationModel(final ShowContext context) {
        this.context = context;
        notInAnyGroup.setValue(true);
        groupsTableModel = new GroupsTableModel(context);
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

    public LassiAction getActionGroupsAdd() {
        return actionGroupsAdd;
    }

    public LassiAction getActionGroupsRemove() {
        return actionGroupsRemove;
    }

    public LassiAction getActionGroupsUp() {
        return actionGroupsUp;
    }

    public LassiAction getActionGroupsDown() {
        return actionGroupsDown;
    }

    public LassiAction getActionInGroupAll() {
        return actionInGroupAll;
    }

    public LassiAction getActionInGroupNone() {
        return actionInGroupNone;
    }

    public LassiAction getActionAddToGroup() {
        return actionAddToGroup;
    }

    public LassiAction getActionRemoveFromGroup() {
        return actionRemoveFromGroup;
    }

    public LassiAction getActionInGroupUp() {
        return actionInGroupUp;
    }

    public LassiAction getActionInGroupDown() {
        return actionInGroupDown;
    }

    public LassiAction getActionNotInGroupAll() {
        return actionNotInGroupAll;
    }

    public LassiAction getActionNotInGroupNone() {
        return actionNotInGroupNone;
    }

    public LassiAction getActionAny() {
        return actionAny;
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

    public StringHolder getNotInGroupLabel() {
        return notInGroupLabel;
    }

    private class ActionGroupsAdd extends LassiAction {

        private ActionGroupsAdd() {
            super("groups.groups.add");
        }

        @Override
        public void action() {
            int groupCount = getGroups().size();
            String name = NLS.get("groups.default.groupname") + (groupCount + 1);
            Group group = new Group(context.getDirtyShow());
            group.setName(name);
            getGroups().add(group);
            updateChannels();
        }
    }

    private class ActionGroupsRemove extends LassiAction {

        private ActionGroupsRemove() {
            super("groups.groups.remove");
        }

        @Override
        public void action() {
            int[] indexes = groupsSelectionModel.getSelectedRows();
            Arrays.sort(indexes);
            for (int i = indexes.length - 1; i >= 0; i--) {
                getGroups().remove(indexes[i]);
            }
            updateChannels();
        }
    }

    private class ActionGroupsUp extends LassiAction {

        private ActionGroupsUp() {
            super("groups.groups.up");
        }

        @Override
        public void action() {
            int[] indexes = groupsSelectionModel.getSelectedRows();
            SelectionInterval selection = getGroups().moveUp(indexes);
            groupsTableModel.fireTableDataChanged();
            groupsSelectionModel.setSelectionInterval(selection.getIndex1(), selection.getIndex2());
        }
    }

    private class ActionGroupsDown extends LassiAction {

        private ActionGroupsDown() {
            super("groups.groups.down");
        }

        @Override
        public void action() {
            int[] indexes = groupsSelectionModel.getSelectedRows();
            SelectionInterval selection = getGroups().moveDown(indexes);
            groupsTableModel.fireTableDataChanged();
            groupsSelectionModel.setSelectionInterval(selection.getIndex1(), selection.getIndex2());
        }
    }

    private class ActionInGroupAll extends LassiAction {

        private ActionInGroupAll() {
            super("groups.inGroup.all");
        }

        @Override
        public void action() {
            int last = inGroupTableModel.getRowCount() - 1;
            inGroupSelectionModel.setSelectionInterval(0, last);
            updateActions();
        }
    }

    private class ActionInGroupNone extends LassiAction {

        private ActionInGroupNone() {
            super("groups.inGroup.none");
        }

        @Override
        public void action() {
            inGroupSelectionModel.clearSelection();
            updateActions();
        }
    }

    private class ActionAddToGroup extends LassiAction {

        private ActionAddToGroup() {
            super("groups.action.addToGroup");
        }

        @Override
        public void action() {
            int[] rows = notInGroupSelectionModel.getSelectedRows();
            for (int row : rows) {
                Channel channel = channelsNotInSelectedGroup.get(row);
                selectedGroup.add(channel);
            }
            updateChannels();
        }
    }

    private class ActionRemoveFromGroup extends LassiAction {

        private ActionRemoveFromGroup() {
            super("groups.action.removeFromGroup");
        }

        @Override
        public void action() {
            int[] rows = inGroupSelectionModel.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) {
                selectedGroup.remove(rows[i]);
            }
            updateChannels();
        }
    }

    private class ActionInGroupUp extends LassiAction {

        private ActionInGroupUp() {
            super("groups.inGroup.up");
        }

        @Override
        public void action() {
            int[] indexes = inGroupSelectionModel.getSelectedRows();
            SelectionInterval selection = selectedGroup.moveUp(indexes);
            updateChannels();
            inGroupSelectionModel.setSelectionInterval(selection.getIndex1(), selection.getIndex2());
        }
    }

    private class ActionInGroupDown extends LassiAction {

        private ActionInGroupDown() {
            super("groups.inGroup.down");
        }

        @Override
        public void action() {
            int[] indexes = inGroupSelectionModel.getSelectedRows();
            SelectionInterval selection = selectedGroup.moveDown(indexes);
            updateChannels();
            inGroupSelectionModel.setSelectionInterval(selection.getIndex1(), selection.getIndex2());
        }
    }

    private class ActionNotInGroupAll extends LassiAction {

        private ActionNotInGroupAll() {
            super("groups.notInGroup.all");
        }

        @Override
        public void action() {
            int index1 = channelsNotInSelectedGroup.size() - 1;
            notInGroupSelectionModel.setSelectionInterval(0, index1);
            updateActions();
        }
    }

    private class ActionNotInGroupNone extends LassiAction {

        private ActionNotInGroupNone() {
            super("groups.notInGroup.none");
        }

        @Override
        public void action() {
            notInGroupSelectionModel.clearSelection();
            updateActions();
        }
    }

    private class ActionNotInAnyGroup extends LassiAction {

        private ActionNotInAnyGroup() {
            super("");
        }

        @Override
        public void action() {
            notInGroupTableModel.fireTableDataChanged();
        }
    }

    private class ActionAny extends LassiAction {

        private ActionAny() {
            super("groups.notInGroup.any");
        }

        @Override
        public void action() {
            if (notInAnyGroup.getValue()) {
                notInGroupLabel.setValue(NLS.get("groups.label.notInGroup"));
                notInAnyGroup.setValue(false);
            } else {
                notInGroupLabel.setValue(NLS.get("groups.label.notInAnyGroup"));
                notInAnyGroup.setValue(true);
            }
        }
    }

    private void updateActions() {
        boolean groupSelected = groupsSelectionModel.getSelectedRowCount() > 0;
        boolean singleGroupSelected = groupsSelectionModel.getSelectedRowCount() == 1;
        actionGroupsRemove.setEnabled(groupSelected);
        boolean up = false;
        boolean down = false;
        if (groupsSelectionModel.getSelectedRowCount() > 0) {
            int[] indexes = groupsSelectionModel.getSelectedRows();
            up = indexes[0] > 0;
            int last = indexes[indexes.length - 1];
            down = last < (groupsTableModel.getRowCount() - 1);
        }
        actionGroupsUp.setEnabled(up);
        actionGroupsDown.setEnabled(down);
        actionInGroupAll.setEnabled(inGroupTableModel.getRowCount() > 0);
        actionInGroupNone.setEnabled(inGroupSelectionModel.getSelectedRowCount() > 0);
        up = false;
        down = false;
        if (inGroupSelectionModel.getSelectedRowCount() > 0) {
            int[] indexes = inGroupSelectionModel.getSelectedRows();
            up = indexes[0] > 0;
            int last = indexes[indexes.length - 1];
            down = last < (inGroupTableModel.getRowCount() - 1);
        }
        actionInGroupUp.setEnabled(up);
        actionInGroupDown.setEnabled(down);
        actionAddToGroup.setEnabled(groupSelected && notInGroupSelectionModel.getSelectedRowCount() > 0);
        actionRemoveFromGroup.setEnabled(groupSelected && inGroupSelectionModel.getSelectedRowCount() > 0);
        actionNotInGroupAll.setEnabled(singleGroupSelected && notInGroupTableModel.getRowCount() > 0);
        actionNotInGroupNone.setEnabled(notInGroupSelectionModel.getSelectedRowCount() > 0);
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

        public void nameChanged(final NamedObject object) {
            inGroupTableModel.fireTableDataChanged();
            notInGroupTableModel.fireTableDataChanged();
        }
    }
}
