package be.lassi.ui.group;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.domain.Channel;
import be.lassi.domain.Group;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.util.ContextTestCaseA;

/**
 * Tests class <code>GroupsPresentationModel</code>.
 */
public class GroupsPresentationModelTestCase extends ContextTestCaseA {

    private GroupsPresentationModel model;

    private final MyTableModelListener groupsTableListener = new MyTableModelListener();

    private final MyTableModelListener inGroupTableListener = new MyTableModelListener();

    private final MyTableModelListener notInGroupTableListener = new MyTableModelListener();

    @BeforeMethod
    public void before() {
        Show show = ShowBuilder.example();
        getContext().setShow(show);
        groupsTableListener.reset();
        inGroupTableListener.reset();
        notInGroupTableListener.reset();
        model = new GroupsPresentationModel(getContext());
        model.getGroupsTableModel().addTableModelListener(groupsTableListener);
        model.getInGroupTableModel().addTableModelListener(inGroupTableListener);
        model.getNotInGroupTableModel().addTableModelListener(notInGroupTableListener);
    }

    @Test
    public void stateNoGroups() {
        assertTrue(model.getActionGroupsAdd().isEnabled());
        assertFalse(model.getActionGroupsRemove().isEnabled());
        assertFalse(model.getActionGroupsUp().isEnabled());
        assertFalse(model.getActionGroupsDown().isEnabled());
        assertFalse(model.getActionInGroupAll().isEnabled());
        assertFalse(model.getActionInGroupNone().isEnabled());
        assertFalse(model.getActionAddToGroup().isEnabled());
        assertFalse(model.getActionRemoveFromGroup().isEnabled());
        assertFalse(model.getActionInGroupUp().isEnabled());
        assertFalse(model.getActionInGroupDown().isEnabled());
        assertFalse(model.getActionNotInGroupAll().isEnabled());
        assertFalse(model.getActionNotInGroupNone().isEnabled());
    }

    @Test
    public void stateNoGroupSelected() {
        addGroup();
        assertTrue(model.getActionGroupsAdd().isEnabled());
        assertFalse(model.getActionGroupsRemove().isEnabled());
        assertFalse(model.getActionGroupsUp().isEnabled());
        assertFalse(model.getActionGroupsDown().isEnabled());
        assertFalse(model.getActionInGroupAll().isEnabled());
        assertFalse(model.getActionInGroupNone().isEnabled());
        assertFalse(model.getActionAddToGroup().isEnabled());
        assertFalse(model.getActionRemoveFromGroup().isEnabled());
        assertFalse(model.getActionInGroupUp().isEnabled());
        assertFalse(model.getActionInGroupDown().isEnabled());
        assertFalse(model.getActionNotInGroupAll().isEnabled());
        assertFalse(model.getActionNotInGroupNone().isEnabled());
    }

    @Test
    public void stateGroupSelected() {
        addGroup();
        selectGroup(0, 0);
        assertTrue(model.getActionGroupsAdd().isEnabled());
        assertTrue(model.getActionGroupsRemove().isEnabled());
        assertFalse(model.getActionGroupsUp().isEnabled());
        assertFalse(model.getActionGroupsDown().isEnabled());
        assertFalse(model.getActionInGroupAll().isEnabled());
        assertFalse(model.getActionInGroupNone().isEnabled());
        assertFalse(model.getActionAddToGroup().isEnabled());
        assertFalse(model.getActionRemoveFromGroup().isEnabled());
        assertFalse(model.getActionInGroupUp().isEnabled());
        assertFalse(model.getActionInGroupDown().isEnabled());
        assertTrue(model.getActionNotInGroupAll().isEnabled());
        assertFalse(model.getActionNotInGroupNone().isEnabled());
    }

    @Test
    public void stateMultipleGroupsSelected() {
        addGroup();
        addGroup();
        selectGroup(0, 1);
        assertTrue(model.getActionGroupsAdd().isEnabled());
        assertTrue(model.getActionGroupsRemove().isEnabled());
        assertFalse(model.getActionGroupsUp().isEnabled());
        assertFalse(model.getActionGroupsDown().isEnabled());
        assertFalse(model.getActionInGroupAll().isEnabled());
        assertFalse(model.getActionInGroupNone().isEnabled());
        assertFalse(model.getActionAddToGroup().isEnabled());
        assertFalse(model.getActionRemoveFromGroup().isEnabled());
        assertFalse(model.getActionInGroupUp().isEnabled());
        assertFalse(model.getActionInGroupDown().isEnabled());
        assertFalse(model.getActionNotInGroupAll().isEnabled());
        assertFalse(model.getActionNotInGroupNone().isEnabled());
    }

    @Test
    public void stateGroupUpEnabled() {
        addGroup();
        addGroup();
        selectGroup(1, 1);
        assertTrue(model.getActionGroupsAdd().isEnabled());
        assertTrue(model.getActionGroupsRemove().isEnabled());
        assertTrue(model.getActionGroupsUp().isEnabled());
        assertFalse(model.getActionGroupsDown().isEnabled());
        assertFalse(model.getActionInGroupAll().isEnabled());
        assertFalse(model.getActionInGroupNone().isEnabled());
        assertFalse(model.getActionAddToGroup().isEnabled());
        assertFalse(model.getActionRemoveFromGroup().isEnabled());
        assertFalse(model.getActionInGroupUp().isEnabled());
        assertFalse(model.getActionInGroupDown().isEnabled());
        assertTrue(model.getActionNotInGroupAll().isEnabled());
        assertFalse(model.getActionNotInGroupNone().isEnabled());
    }

    @Test
    public void stateGroupDownEnabled() {
        addGroup();
        addGroup();
        selectGroup(0, 0);
        assertTrue(model.getActionGroupsAdd().isEnabled());
        assertTrue(model.getActionGroupsRemove().isEnabled());
        assertFalse(model.getActionGroupsUp().isEnabled());
        assertTrue(model.getActionGroupsDown().isEnabled());
        assertFalse(model.getActionInGroupAll().isEnabled());
        assertFalse(model.getActionInGroupNone().isEnabled());
        assertFalse(model.getActionAddToGroup().isEnabled());
        assertFalse(model.getActionRemoveFromGroup().isEnabled());
        assertFalse(model.getActionInGroupUp().isEnabled());
        assertFalse(model.getActionInGroupDown().isEnabled());
        assertTrue(model.getActionNotInGroupAll().isEnabled());
        assertFalse(model.getActionNotInGroupNone().isEnabled());
    }

    @Test
    public void stateNotInGroupSelected() {
        addGroup();
        selectGroup(0, 0);
        selectNotInGroup(0, 5);
        assertTrue(model.getActionGroupsAdd().isEnabled());
        assertTrue(model.getActionGroupsRemove().isEnabled());
        assertFalse(model.getActionGroupsUp().isEnabled());
        assertFalse(model.getActionGroupsDown().isEnabled());
        assertFalse(model.getActionInGroupAll().isEnabled());
        assertFalse(model.getActionInGroupNone().isEnabled());
        assertTrue(model.getActionAddToGroup().isEnabled());
        assertFalse(model.getActionRemoveFromGroup().isEnabled());
        assertFalse(model.getActionInGroupUp().isEnabled());
        assertFalse(model.getActionInGroupDown().isEnabled());
        assertTrue(model.getActionNotInGroupAll().isEnabled());
        assertTrue(model.getActionNotInGroupNone().isEnabled());
    }

    @Test
    public void stateInGroupPopulated() {
        addGroup();
        selectGroup(0, 0);
        selectNotInGroup(0, 5);
        model.getActionAddToGroup().action();
        assertTrue(model.getActionGroupsAdd().isEnabled());
        assertTrue(model.getActionGroupsRemove().isEnabled());
        assertFalse(model.getActionGroupsUp().isEnabled());
        assertFalse(model.getActionGroupsDown().isEnabled());
        assertTrue(model.getActionInGroupAll().isEnabled());
        assertFalse(model.getActionInGroupNone().isEnabled());
        assertTrue(model.getActionAddToGroup().isEnabled());
        assertFalse(model.getActionRemoveFromGroup().isEnabled());
        assertFalse(model.getActionInGroupUp().isEnabled());
        assertFalse(model.getActionInGroupDown().isEnabled());
        assertTrue(model.getActionNotInGroupAll().isEnabled());
        assertTrue(model.getActionNotInGroupNone().isEnabled());
    }

    @Test
    public void stateInGroupFirstSelected() {
        addGroup();
        selectGroup(0, 0);
        selectNotInGroup(0, 5);
        model.getActionAddToGroup().action();
        selectInGroup(0, 0);
        assertTrue(model.getActionGroupsAdd().isEnabled());
        assertTrue(model.getActionGroupsRemove().isEnabled());
        assertFalse(model.getActionGroupsUp().isEnabled());
        assertFalse(model.getActionGroupsDown().isEnabled());
        assertTrue(model.getActionInGroupAll().isEnabled());
        assertTrue(model.getActionInGroupNone().isEnabled());
        assertTrue(model.getActionAddToGroup().isEnabled());
        assertTrue(model.getActionRemoveFromGroup().isEnabled());
        assertFalse(model.getActionInGroupUp().isEnabled());
        assertTrue(model.getActionInGroupDown().isEnabled());
        assertTrue(model.getActionNotInGroupAll().isEnabled());
        assertTrue(model.getActionNotInGroupNone().isEnabled());
    }

    @Test
    public void stateInGroupSecondSelected() {
        addGroup();
        selectGroup(0, 0);
        selectNotInGroup(0, 5);
        model.getActionAddToGroup().action();
        selectInGroup(1, 1);
        assertTrue(model.getActionGroupsAdd().isEnabled());
        assertTrue(model.getActionGroupsRemove().isEnabled());
        assertFalse(model.getActionGroupsUp().isEnabled());
        assertFalse(model.getActionGroupsDown().isEnabled());
        assertTrue(model.getActionInGroupAll().isEnabled());
        assertTrue(model.getActionInGroupNone().isEnabled());
        assertTrue(model.getActionAddToGroup().isEnabled());
        assertTrue(model.getActionRemoveFromGroup().isEnabled());
        assertTrue(model.getActionInGroupUp().isEnabled());
        assertTrue(model.getActionInGroupDown().isEnabled());
        assertTrue(model.getActionNotInGroupAll().isEnabled());
        assertTrue(model.getActionNotInGroupNone().isEnabled());
    }

    @Test
    public void actionAddGroup() {
        assertEquals(getGroupCount(), 0);
        assertEquals(model.getGroupsTableModel().getRowCount(), 0);
        addGroup();
        assertEquals(getGroupCount(), 1);
        assertEquals(getGroup(0).getName(), "Group 1");
        assertEquals(model.getGroupsTableModel().getRowCount(), 1);
        assertEquals(model.getGroupsTableModel().getValueAt(0, 0), 1);
        assertEquals(model.getGroupsTableModel().getValueAt(0, 1), "Group 1");
        addGroup();
        assertEquals(getGroupCount(), 2);
        assertEquals(getGroup(0).getName(), "Group 1");
        assertEquals(getGroup(1).getName(), "Group 2");
        assertEquals(model.getGroupsTableModel().getRowCount(), 2);
        assertEquals(model.getGroupsTableModel().getValueAt(0, 0), 1);
        assertEquals(model.getGroupsTableModel().getValueAt(1, 0), 2);
        assertEquals(model.getGroupsTableModel().getValueAt(0, 1), "Group 1");
        assertEquals(model.getGroupsTableModel().getValueAt(1, 1), "Group 2");
    }

    @Test
    public void actionRemoveGroup() {
        addGroup();
        addGroup();
        addGroup();
        selectGroup(1);
        model.getActionGroupsRemove().action();
        assertEquals(getGroupCount(), 2);
        assertEquals(getGroup(0).getName(), "Group 1");
        assertEquals(getGroup(1).getName(), "Group 3");
    }

    @Test
    public void actionMoveGroupsUp() {
        addGroup();
        addGroup();
        addGroup();
        selectGroup(1);
        model.getActionGroupsUp().action();
        assertEquals(getGroup(0).getName(), "Group 2");
        assertEquals(getGroup(1).getName(), "Group 1");
        assertEquals(getGroup(2).getName(), "Group 3");
    }

    @Test
    public void actionMoveGroupsDown() {
        addGroup();
        addGroup();
        addGroup();
        selectGroup(1);
        model.getActionGroupsDown().action();
        assertEquals(getGroup(0).getName(), "Group 1");
        assertEquals(getGroup(1).getName(), "Group 3");
        assertEquals(getGroup(2).getName(), "Group 2");
    }

    @Test
    public void actionSelectAllInGroup() {
        createGroup();
        model.getActionInGroupAll().action();
        ListSelectionModel selectionModel = model.getInGroupSelectionModel();
        assertEquals(selectionModel.getMinSelectionIndex(), 0);
        assertEquals(selectionModel.getMaxSelectionIndex(), 2);
    }

    @Test
    public void actionSelectNoneInGroup() {
        createGroup();
        model.getActionInGroupAll().action();
        model.getActionInGroupNone().action();
        ListSelectionModel selectionModel = model.getInGroupSelectionModel();
        assertEquals(selectionModel.getMinSelectionIndex(), -1);
        assertEquals(selectionModel.getMaxSelectionIndex(), -1);
    }

    @Test
    public void actionAddToGroup() {
        createGroup();
        selectChannelsNotInGroup(4, 5);
        model.getActionAddToGroup().action();
        Channel[] channels = getGroup(0).getChannels();
        assertEquals(channels.length, 5);
        assertEquals(channels[0].getName(), "Channel 1");
        assertEquals(channels[1].getName(), "Channel 2");
        assertEquals(channels[2].getName(), "Channel 3");
        assertEquals(channels[3].getName(), "Channel 8");
        assertEquals(channels[4].getName(), "Channel 9");
    }

    @Test
    public void actionRemoveFromGroup() {
        createGroup();
        Channel[] channels = getGroup(0).getChannels();
        assertEquals(channels.length, 3);
        assertEquals(channels[0].getName(), "Channel 1");
        assertEquals(channels[1].getName(), "Channel 2");
        assertEquals(channels[2].getName(), "Channel 3");
        selectChannelsInGroup(1, 1);
        model.getActionRemoveFromGroup().action();
        channels = getGroup(0).getChannels();
        assertEquals(2, channels.length, 2);
        assertEquals(channels[0].getName(), "Channel 1");
        assertEquals(channels[1].getName(), "Channel 3");
    }

    @Test
    public void actionUpChannel() {
        createGroup();
        selectChannelsInGroup(1, 1);
        model.getActionInGroupUp().action();
        Channel[] channels = getGroup(0).getChannels();
        assertEquals(channels.length, 3);
        assertEquals(channels[0].getName(), "Channel 2");
        assertEquals(channels[1].getName(), "Channel 1");
        assertEquals(channels[2].getName(), "Channel 3");
    }

    @Test
    public void actionDownChannel() {
        createGroup();
        selectChannelsInGroup(1, 1);
        model.getActionInGroupDown().action();
        Channel[] channels = getGroup(0).getChannels();
        assertEquals(channels.length, 3);
        assertEquals(channels[0].getName(), "Channel 1");
        assertEquals(channels[1].getName(), "Channel 3");
        assertEquals(channels[2].getName(), "Channel 2");
    }

    @Test
    public void actionAllNotInGroup() {
        model.getActionNotInGroupAll().action();
        int channelCount = getContext().getShow().getChannels().size();
        ListSelectionModel selectionModel = model.getNotInGroupSelectionModel();
        assertEquals(selectionModel.getMinSelectionIndex(), 0);
        assertEquals(selectionModel.getMaxSelectionIndex(), channelCount - 1);
    }

    @Test
    public void actionNoneNotInGroup() {
        int channelCount = getContext().getShow().getChannels().size();
        selectChannelsNotInGroup(0, channelCount - 1);
        model.getActionNotInGroupNone().action();
        ListSelectionModel selectionModel = model.getNotInGroupSelectionModel();
        assertEquals(selectionModel.getMinSelectionIndex(), -1);
        assertEquals(selectionModel.getMaxSelectionIndex(), -1);
    }

    @Test
    public void actionNotInAnyGroup() {
        model.getNotInAnyGroup().setValue(true);
        addGroup();
        selectGroup(0);
        addChannels(0, 2);
        addGroup();
        selectGroup(1);
        addChannels(0, 2);
        Channel[] channels1 = getGroup(0).getChannels();
        Channel[] channels2 = getGroup(1).getChannels();
        assertEquals(channels1.length, 3);
        assertEquals(channels2.length, 3);
        assertEquals(channels1[0].getName(), "Channel 1");
        assertEquals(channels1[1].getName(), "Channel 2");
        assertEquals(channels1[2].getName(), "Channel 3");
        assertEquals(channels2[0].getName(), "Channel 4");
        assertEquals(channels2[1].getName(), "Channel 5");
        assertEquals(channels2[2].getName(), "Channel 6");
        int channelCount = getContext().getShow().getChannels().size();
        model.getNotInAnyGroup().setValue(false);
        assertEquals(model.getNotInGroupTableModel().getRowCount(), channelCount - 3);
        int before = notInGroupTableListener.events.size();
        model.getActionNotInAnyGroup().action();
        assertEquals(notInGroupTableListener.events.size(), before + 1);
        model.getNotInAnyGroup().setValue(true);
        assertEquals(model.getNotInGroupTableModel().getRowCount(), channelCount - 6);
        before = notInGroupTableListener.events.size();
        model.getActionNotInAnyGroup().action();
        assertEquals(notInGroupTableListener.events.size(), before + 1);
    }

    @Test
    public void testShowChange() {
        Show show1 = ShowBuilder.example();
        Show show2 = ShowBuilder.example();
        show1.getGroups().add(new Group("A"));
        show2.getGroups().add(new Group("B"));
        getContext().setShow(show1);
        assertEquals(model.getGroupsTableModel().getValueAt(0, 1), "A");
        getContext().setShow(show2);
        assertEquals(model.getGroupsTableModel().getValueAt(0, 1), "B");
    }

    @Test
    public void testChannelNameChange() {
        Show show1 = ShowBuilder.example();
        Show show2 = ShowBuilder.example();
        getContext().setShow(show1);
        int e1 = inGroupTableListener.getEventCount();
        int e2 = notInGroupTableListener.getEventCount();
        getContext().getShow().getChannels().get(0).setName("A");
        assertEquals(inGroupTableListener.getEventCount(), e1 + 1);
        assertEquals(notInGroupTableListener.getEventCount(), e2 + 1);
        getContext().setShow(show2);
        e1 = inGroupTableListener.getEventCount();
        e2 = notInGroupTableListener.getEventCount();
        getContext().getShow().getChannels().get(0).setName("B");
        assertEquals(inGroupTableListener.getEventCount(), e1 + 1);
        assertEquals(notInGroupTableListener.getEventCount(), e2 + 1);
    }

    @Test
    public void notInGroupLabel() {
        assertEquals(model.getNotInGroupLabel().getValue(), "Not in any group");
        model.getActionAny().action();
        assertEquals(model.getNotInGroupLabel().getValue(), "Not in group");
        model.getActionAny().action();
        assertEquals(model.getNotInGroupLabel().getValue(), "Not in any group");
    }

    private void addGroup() {
        model.getActionGroupsAdd().action();
    }

    private Group getGroup(final int groupIndex) {
        return getContext().getShow().getGroups().get(groupIndex);
    }

    private int getGroupCount() {
        return getContext().getShow().getGroups().size();
    }

    private void selectGroup(final int groupIndex) {
        ListSelectionModel sm = model.getGroupsSelectionModel();
        sm.setSelectionInterval(groupIndex, groupIndex);
    }

    private void selectChannelsNotInGroup(final int index1, final int index2) {
        ListSelectionModel sm = model.getNotInGroupSelectionModel();
        sm.setSelectionInterval(index1, index2);
    }

    private void selectChannelsInGroup(final int index1, final int index2) {
        ListSelectionModel sm = model.getInGroupSelectionModel();
        sm.setSelectionInterval(index1, index2);
    }

    private void addChannels(final int index1, final int index2) {
        selectChannelsNotInGroup(0, 2);
        model.getActionAddToGroup().action();
    }

    private void createGroup() {
        addGroup();
        selectGroup(0);
        selectChannelsNotInGroup(0, 2);
        model.getActionAddToGroup().action();
    }

    private class MyTableModelListener implements TableModelListener {

        final List<TableModelEvent> events = new ArrayList<TableModelEvent>();

        public void tableChanged(final TableModelEvent event) {
            events.add(event);
        }

        public int getEventCount() {
            return events.size();
        }

        public void reset() {
            events.clear();
        }
    }

    private void selectGroup(final int index1, final int index2) {
        model.getGroupsSelectionModel().setSelectionInterval(index1, index2);
    }

    private void selectNotInGroup(final int index1, final int index2) {
        model.getNotInGroupSelectionModel().setSelectionInterval(index1, index2);
    }

    private void selectInGroup(final int index1, final int index2) {
        model.getInGroupSelectionModel().setSelectionInterval(index1, index2);
    }
}
