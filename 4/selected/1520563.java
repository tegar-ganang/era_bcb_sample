package gov.sns.xal.tools.widgets;

import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.qualify.*;
import gov.sns.ca.Channel;
import gov.sns.tools.messaging.MessageCenter;
import gov.sns.tools.apputils.iconlib.IconLib;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** View for allowing the user to pick PVs from the XAL node tree. */
public class PVPickerView extends Box {

    /** message center */
    protected final MessageCenter MESSAGE_CENTER;

    /** proxy which forwards events to registered listeners */
    protected final PVPickerListener EVENT_PROXY;

    /** the table model */
    protected NavigationTableModel _tableModel;

    /** the table */
    protected JTable _table;

    /** back button */
    protected JButton _backButton;

    /** next button */
    protected JButton _selectButton;

    /** current navigation state */
    protected NavigationState<?> _navigationState;

    /** Constructor */
    public PVPickerView(final AcceleratorSeq sequence) {
        super(BoxLayout.Y_AXIS);
        MESSAGE_CENTER = new MessageCenter("PV Picker View");
        EVENT_PROXY = MESSAGE_CENTER.registerSource(this, PVPickerListener.class);
        _tableModel = new NavigationTableModel();
        makeContents();
        setSequence(sequence);
    }

    /**
     * Add a listener of PV Picker View events.
     * @param listener the listener to add
     */
    public void addPVPickerListener(final PVPickerListener listener) {
        MESSAGE_CENTER.registerTarget(listener, this, PVPickerListener.class);
    }

    /**
     * Remove the listener from receiving PV Picker events.
     * @param listener the listener to remove
     */
    public void removePVPickerListener(final PVPickerListener listener) {
        MESSAGE_CENTER.removeTarget(listener, this, PVPickerListener.class);
    }

    /**
     * Set whether or not to allow multiple selection (default).
     * @param allowsMultipleSelection true to allow multiple selection and false to disable multiple selection
     */
    public void setAllowsMultipleSelection(final boolean allowsMultipleSelection) {
        _table.setSelectionMode(allowsMultipleSelection ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
    }

    /** make the contents */
    protected void makeContents() {
        _table = new JTable(_tableModel);
        _table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent event) {
                if (event.getClickCount() == 2) {
                    goToNextState();
                }
            }
        });
        add(new JScrollPane(_table));
        add(makeNavigationButtonRow());
    }

    /** make the navigation button row */
    protected Component makeNavigationButtonRow() {
        _backButton = new JButton(IconLib.getIcon(IconLib.IconGroup.NAVIGATION, "Back24.gif"));
        _backButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                goToPreviousState();
            }
        });
        _selectButton = new JButton("Select");
        _selectButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                goToNextState();
            }
        });
        final Box row = new Box(BoxLayout.X_AXIS);
        row.add(_backButton);
        row.add(Box.createHorizontalGlue());
        row.add(_selectButton);
        row.add(Box.createHorizontalStrut(15));
        row.setMaximumSize(new Dimension(10000, row.getPreferredSize().height));
        return row;
    }

    /** go to the next state */
    protected void goToNextState() {
        if (_table.getSelectedRow() >= 0) {
            _navigationState.setSelectedItems(_table.getSelectedRows());
            final NavigationState<?> nextState = _navigationState.nextState();
            if (nextState != null) {
                setNavigationState(nextState);
            }
        } else {
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }

    /** go to the previous state */
    protected void goToPreviousState() {
        final NavigationState<?> previousState = _navigationState.previousState();
        if (previousState != null) {
            setNavigationState(previousState);
        }
        applyStateSelectionsToTable();
    }

    /** apply the state selections to the table */
    protected void applyStateSelectionsToTable() {
        final ListSelectionModel selectionModel = _table.getSelectionModel();
        final int[] selectionIndices = _navigationState.getSelectionIndices();
        for (int selectionIndex : selectionIndices) {
            selectionModel.addSelectionInterval(selectionIndex, selectionIndex);
        }
    }

    /** set the sequence */
    public void setSequence(final AcceleratorSeq sequence) {
        setNavigationState(NavigationState.getInstance(sequence));
    }

    /** set the navigation state */
    protected void setNavigationState(final NavigationState<?> state) {
        final NavigationState<?> oldState = _navigationState;
        _navigationState = state;
        if (_tableModel != null) {
            _tableModel.fireTableStructureChanged();
        }
        _backButton.setEnabled(state != null && state.previousState() != null);
        _selectButton.setEnabled(state != null && state.nextState() != null);
        if (oldState != null) {
            EVENT_PROXY.itemsSelected(this, oldState.getSelectedItems());
            if (oldState instanceof PVHandleNavigationState) {
                EVENT_PROXY.channelReferencesSelected(this, ((PVHandleNavigationState) oldState).getSelectedItems());
            }
        }
    }

    /** Get the selected node channel references */
    public List<NodeChannelRef> getSelectedNodeChannelRefs() {
        final NavigationState<?> state = _navigationState;
        if (state != null && state instanceof PVNavigationState) {
            return ((PVNavigationState) state).previousState().getItems();
        } else {
            return null;
        }
    }

    /** Get the selected node channel references */
    public List<AcceleratorSeq> getSelectedSequences() {
        NavigationState<?> state = _navigationState;
        while (state != null && !(state instanceof SequenceNavigationState)) {
            state = state.previousState();
        }
        return state != null ? ((SequenceNavigationState) state).getSequences() : null;
    }

    /** Get the selected node channel references */
    public List<String> getSelectedNodeTypes() {
        NavigationState<?> state = _navigationState;
        while (state != null && !(state instanceof TypeNavigationState)) {
            state = state.previousState();
        }
        return state != null ? ((TypeNavigationState) state).getTypes() : null;
    }

    /** Get the selected node channel references */
    public List<AcceleratorNode> getSelectedNodes() {
        NavigationState<?> state = _navigationState;
        while (state != null && !(state instanceof NodeNavigationState)) {
            state = state.previousState();
        }
        return state != null ? ((NodeNavigationState) state).getNodes() : null;
    }

    /** Get the selected node channel references */
    public List<String> getSelectedPVHandles() {
        NavigationState<?> state = _navigationState;
        while (state != null && !(state instanceof PVHandleNavigationState)) {
            state = state.previousState();
        }
        return state != null ? ((PVHandleNavigationState) state).getHandles() : null;
    }

    /** the table model for navigation */
    class NavigationTableModel extends AbstractTableModel {

        protected final int LABEL_COLUMN = 0;

        /** get the number of rows */
        public int getRowCount() {
            final NavigationState<?> state = _navigationState;
            return state != null ? state.getItems().size() : 0;
        }

        /** get the number of columns */
        public int getColumnCount() {
            return 1;
        }

        /** get the name for the specified column */
        @Override
        public String getColumnName(final int column) {
            final NavigationState<?> state = _navigationState;
            switch(column) {
                case LABEL_COLUMN:
                    return state != null ? state.getTitle() : "Items";
                default:
                    return "";
            }
        }

        /** get the value for the specified cell */
        public Object getValueAt(int row, int column) {
            return doGetValueAt(row, column, _navigationState);
        }

        private <T> Object doGetValueAt(int row, int column, NavigationState<T> state) {
            if (state == null) return null;
            final List<T> items = state.getItems();
            final T item = items != null ? items.get(row) : null;
            switch(column) {
                case LABEL_COLUMN:
                    return state.getItemLabel(item);
                default:
                    return null;
            }
        }
    }
}

/** represents the current state of navigation */
abstract class NavigationState<T> {

    List<T> _items;

    List<T> _selectedItems = new ArrayList<T>();

    /** get an instance */
    static NavigationState<?> getInstance(final AcceleratorSeq sequence) {
        return sequence instanceof Accelerator ? new AcceleratorNavigationState((Accelerator) sequence) : new SequenceNavigationState(sequence);
    }

    /** get the title to display */
    abstract String getTitle();

    /** get the previousState */
    abstract NavigationState<?> previousState();

    /** next navigation state */
    abstract NavigationState<?> nextState();

    /** get selected items */
    List<T> getSelectedItems() {
        return _selectedItems;
    }

    /** set selected items */
    void setSelectedItems(final List<T> selectedItems) {
        _selectedItems = selectedItems;
    }

    /** set selected items */
    void setSelectedItems(final int[] indices) {
        final List<T> items = _items;
        _selectedItems.clear();
        for (int index : indices) {
            if (index < _items.size()) {
                _selectedItems.add(items.get(index));
            }
        }
    }

    /** get the selection indices */
    int[] getSelectionIndices() {
        final int[] indices = new int[_selectedItems.size()];
        int index = 0;
        for (Object item : _selectedItems) {
            indices[index++] = _items.indexOf(item);
        }
        return indices;
    }

    /** get the items */
    abstract List<T> getItems();

    /** get the item label */
    String getItemLabel(final T item) {
        return item != null ? item.toString() : null;
    }

    /** sort the items in the list */
    void sortItems(final List<T> items) {
        Collections.sort(items, getItemComparator());
    }

    /** get the item comparator */
    Comparator<T> getItemComparator() {
        return new Comparator<T>() {

            public int compare(T item1, T item2) {
                String label1 = getItemLabel(item1);
                String label2 = getItemLabel(item2);
                if (label1 == null && label2 == null) {
                    return 0;
                }
                if (label1 != null && label2 == null) {
                    return 1;
                }
                if (label1 == null && label2 != null) {
                    return -1;
                }
                return label1.compareTo(label2);
            }
        };
    }
}

/** Accelerator navigation state */
class AcceleratorNavigationState extends NavigationState<AcceleratorSeq> {

    final Accelerator _accelerator;

    /** Primary Constructor */
    AcceleratorNavigationState(final Accelerator accelerator) {
        _accelerator = accelerator;
    }

    @Override
    String getTitle() {
        return "Accelerator Sequences";
    }

    @Override
    NavigationState<Void> previousState() {
        return null;
    }

    @Override
    SequenceNavigationState nextState() {
        return new SequenceNavigationState(this, _selectedItems);
    }

    @Override
    List<AcceleratorSeq> getItems() {
        if (_items == null) {
            _items = new ArrayList<AcceleratorSeq>();
            _items.addAll(_accelerator.getSequences(true));
            _items.addAll(_accelerator.getComboSequences());
            sortItems(_items);
        }
        return _items;
    }
}

/** Sequence navigation state */
class SequenceNavigationState extends NavigationState<String> {

    final AcceleratorNavigationState _previousState;

    final List<AcceleratorSeq> _sequences;

    /** Primary Constructor */
    SequenceNavigationState(final AcceleratorNavigationState previousState, final List<AcceleratorSeq> sequences) {
        _previousState = previousState;
        _sequences = sequences;
    }

    /** Primary Constructor */
    SequenceNavigationState(final AcceleratorNavigationState previousState, final AcceleratorSeq sequence) {
        this(previousState, Collections.singletonList(sequence));
    }

    /** Constructor */
    SequenceNavigationState(final AcceleratorSeq sequence) {
        this(null, sequence);
    }

    @Override
    String getTitle() {
        return "Accelerator Node Types";
    }

    /** get the sequences */
    List<AcceleratorSeq> getSequences() {
        return _sequences;
    }

    @Override
    AcceleratorNavigationState previousState() {
        return _previousState;
    }

    @Override
    TypeNavigationState nextState() {
        return new TypeNavigationState(this, _sequences, _selectedItems);
    }

    @Override
    List<String> getItems() {
        if (_items == null) {
            _items = new ArrayList<String>(ElementTypeManager.defaultManager().getTypes());
            sortItems(_items);
        }
        return _items;
    }
}

/** Sequence navigation state */
class TypeNavigationState extends NavigationState<AcceleratorNode> {

    final SequenceNavigationState _previousState;

    final List<String> _types;

    final List<AcceleratorSeq> _sequences;

    /** Primary Constructor */
    TypeNavigationState(final SequenceNavigationState previousState, final List<AcceleratorSeq> sequences, final List<String> types) {
        _previousState = previousState;
        _sequences = sequences;
        _types = types;
    }

    @Override
    String getTitle() {
        return "Accelerator Nodes";
    }

    /** get the node types */
    List<String> getTypes() {
        return _types;
    }

    @Override
    SequenceNavigationState previousState() {
        return _previousState;
    }

    @Override
    NodeNavigationState nextState() {
        return new NodeNavigationState(this, _selectedItems);
    }

    @Override
    List<AcceleratorNode> getItems() {
        if (_items == null) {
            final Set<AcceleratorNode> nodes = new HashSet<AcceleratorNode>();
            for (String type : _types) {
                final TypeQualifier qualifier = KindQualifier.qualifierWithStatusAndType(true, type);
                for (AcceleratorSeq sequence : _sequences) {
                    nodes.addAll(sequence.getAllInclusiveNodesWithQualifier(qualifier));
                }
            }
            _items = new ArrayList<AcceleratorNode>(nodes);
            sortItems(_items);
        }
        return _items;
    }

    @Override
    String getItemLabel(final AcceleratorNode item) {
        return item != null ? item.getId() : null;
    }
}

/** Sequence navigation state */
class NodeNavigationState extends NavigationState<String> {

    final TypeNavigationState _previousState;

    final List<AcceleratorNode> _nodes;

    /** Primary Constructor */
    NodeNavigationState(final TypeNavigationState previousState, final List<AcceleratorNode> nodes) {
        _previousState = previousState;
        _nodes = nodes;
    }

    @Override
    String getTitle() {
        return "PV Handles";
    }

    /** get the nodes */
    List<AcceleratorNode> getNodes() {
        return _nodes;
    }

    @Override
    TypeNavigationState previousState() {
        return _previousState;
    }

    @Override
    PVHandleNavigationState nextState() {
        return new PVHandleNavigationState(this, _nodes, _selectedItems);
    }

    @Override
    List<String> getItems() {
        if (_items == null) {
            final Set<String> handles = new HashSet<String>();
            for (AcceleratorNode node : _nodes) {
                handles.addAll(node.getHandles());
            }
            _items = new ArrayList<String>(handles);
            sortItems(_items);
        }
        return _items;
    }
}

/** Sequence navigation state */
class PVHandleNavigationState extends NavigationState<NodeChannelRef> {

    final NodeNavigationState _previousState;

    final List<String> _handles;

    final List<AcceleratorNode> _nodes;

    /** Primary Constructor */
    PVHandleNavigationState(final NodeNavigationState previousState, final List<AcceleratorNode> nodes, final List<String> handles) {
        _previousState = previousState;
        _nodes = nodes;
        _handles = handles;
    }

    @Override
    String getTitle() {
        return "Node Channels";
    }

    /** get the handles */
    List<String> getHandles() {
        return _handles;
    }

    @Override
    NodeNavigationState previousState() {
        return _previousState;
    }

    @Override
    PVNavigationState nextState() {
        return new PVNavigationState(this, _selectedItems);
    }

    @Override
    List<NodeChannelRef> getItems() {
        if (_items == null) {
            _items = new ArrayList<NodeChannelRef>();
            for (AcceleratorNode node : _nodes) {
                for (String handle : _handles) {
                    try {
                        final Channel channel = node.getChannel(handle);
                        if (channel != null) {
                            _items.add(new NodeChannelRef(node, handle));
                        }
                    } catch (NoSuchChannelException exception) {
                    }
                }
            }
            sortItems(_items);
        }
        return _items;
    }
}

/** Sequence navigation state */
class PVNavigationState extends NavigationState<String> {

    final PVHandleNavigationState _previousState;

    final List<NodeChannelRef> _channelRefs;

    /** Primary Constructor */
    PVNavigationState(final PVHandleNavigationState previousState, final List<NodeChannelRef> channelRefs) {
        _previousState = previousState;
        _channelRefs = channelRefs;
    }

    @Override
    String getTitle() {
        return "Process Variables";
    }

    @Override
    PVHandleNavigationState previousState() {
        return _previousState;
    }

    @Override
    NavigationState<Void> nextState() {
        return null;
    }

    @Override
    List<String> getItems() {
        if (_items == null) {
            _items = new ArrayList<String>();
            for (NodeChannelRef channelRef : _channelRefs) {
                _items.add(channelRef.getChannel().channelName());
            }
            sortItems(_items);
        }
        return _items;
    }
}
