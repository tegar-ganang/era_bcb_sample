package gov.sns.apps.knobs;

import gov.sns.tools.text.FormattedNumber;
import gov.sns.tools.apputils.iconlib.IconLib;
import gov.sns.xal.tools.widgets.*;
import gov.sns.xal.smf.*;
import java.util.*;
import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

/** view for editing knob detail */
public class KnobEditor extends Box implements KnobListener {

    /** the knob to edit */
    protected final Knob _knob;

    /** table model of knob elements */
    protected final KnobElementTableModel _elementTableModel;

    /** table of knob elements */
    protected final JTable _elementTable;

    /** PV Picker */
    protected final PVPickerView _PVPicker;

    /** main split pane */
    protected JSplitPane _mainSplitPane;

    /** Constructor */
    public KnobEditor(final Knob knob) {
        super(BoxLayout.X_AXIS);
        _knob = knob;
        _PVPicker = new PVPickerView(knob.getAccelerator());
        handlePVPickerEvents(_PVPicker);
        _elementTableModel = new KnobElementTableModel();
        _elementTable = new JTable(_elementTableModel);
        buildView();
        knob.addKnobListener(this);
    }

    /** build view */
    protected void buildView() {
        _mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _PVPicker, buildKnobElementTableView());
        add(_mainSplitPane);
    }

    /** build knob element table view */
    protected Component buildKnobElementTableView() {
        final Box view = new Box(BoxLayout.Y_AXIS);
        view.add(new JScrollPane(_elementTable));
        view.add(buildBottomEditingRow());
        return view;
    }

    /** build the add/remove button row */
    protected Component buildBottomEditingRow() {
        final Box row = new Box(BoxLayout.X_AXIS);
        final JButton removeButton = new JButton(IconLib.getIcon(IconLib.IconGroup.TABLE, "RowDelete24.gif"));
        removeButton.setToolTipText("Remove selected knob elements.");
        final JButton addButton = new JButton(IconLib.getIcon(IconLib.IconGroup.TABLE, "RowInsertAfter24.gif"));
        addButton.setToolTipText("Add a new knob element.");
        addButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                final KnobElement element = new KnobElement();
                _knob.addElement(element);
                _elementTableModel.fireTableDataChanged();
            }
        });
        removeButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                final List<KnobElement> elements = new ArrayList<KnobElement>(_knob.getElements());
                final int[] selectedRows = _elementTable.getSelectedRows();
                for (int row : selectedRows) {
                    final KnobElement element = elements.get(row);
                    _knob.removeElement(element);
                }
                _elementTableModel.fireTableDataChanged();
            }
        });
        row.add(removeButton);
        row.add(addButton);
        row.add(Box.createHorizontalGlue());
        final JButton okayButton = new JButton("Okay");
        okayButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                SwingUtilities.getRoot(KnobEditor.this).setVisible(false);
            }
        });
        row.add(okayButton);
        return row;
    }

    /** handle PV Picker events */
    protected void handlePVPickerEvents(final PVPickerView picker) {
        picker.addPVPickerListener(new PVPickerListener() {

            /**
			 * Handle the event indicating that items have been selected.
			 * @param view the PVPickerView view where the selection was made
			 * @param items the selected items
			 */
            public void itemsSelected(final PVPickerView view, final List selectedItems) {
            }

            /**
			 * Handle the event indicating that channel references have been selected form the specified PVPickerView.
			 * @param view the PVPickerView view where the selection was made
			 * @param channelRefs the node-channel references
			 */
            public void channelReferencesSelected(final PVPickerView view, final List<NodeChannelRef> channelRefs) {
                for (NodeChannelRef channelRef : channelRefs) {
                    final KnobElement element = new KnobElement();
                    element.setNodeChannelRef(channelRef);
                    _knob.addElement(element);
                }
                _elementTableModel.fireTableDataChanged();
            }
        });
    }

    /** override the painting of this view to set the divider location when it becomes visible */
    @Override
    public void paint(java.awt.Graphics graphics) {
        super.paint(graphics);
        _mainSplitPane.setDividerLocation(0.25);
    }

    /** revresh this view */
    public void refresh() {
        _elementTableModel.fireTableDataChanged();
        _PVPicker.setSequence(_knob.getAccelerator());
        _PVPicker.setVisible(_knob.getAccelerator() != null);
        _mainSplitPane.setEnabled(_knob.getAccelerator() != null);
    }

    /** event indicating that the specified knob's name has changed */
    public void nameChanged(final Knob knob, final String newName) {
    }

    /** ready state changed */
    public void readyStateChanged(final Knob knob, final boolean isReady) {
    }

    /** event indicating that the knob's limits have changed */
    public void limitsChanged(final Knob knob, final double lowerLimit, final double upperLimit) {
    }

    /** event indicating that the knob's current value setting has changed */
    public void currentSettingChanged(final Knob knob, final double value) {
    }

    /** event indicating that the knob's most previously pending set operation has completed */
    public void valueSettingPublished(final Knob knob) {
    }

    /** event indicating that an element has been added */
    public void elementAdded(final Knob knob, final KnobElement element) {
        _elementTableModel.fireTableDataChanged();
    }

    /** event indicating that an element has been removed */
    public void elementRemoved(final Knob knob, final KnobElement element) {
        _elementTableModel.fireTableDataChanged();
    }

    /** event indicating that the specified knob element has been modified */
    public void elementModified(final Knob knob, final KnobElement element) {
        _elementTableModel.fireTableDataChanged();
    }

    /** table model of knob elements */
    protected class KnobElementTableModel extends AbstractTableModel {

        protected final int PV_COLUMN = 0;

        protected final int COEFFICIENT_COLUMN = 1;

        protected final int CUSTOM_LIMITS_COLUMN = 2;

        protected final int LOWER_LIMIT_COLUMN = 3;

        protected final int UPPER_LIMIT_COLUMN = 4;

        /** Constructor */
        public KnobElementTableModel() {
        }

        /** get the number of rows */
        public int getRowCount() {
            return _knob.getElements().size();
        }

        /** get the number of columns */
        public int getColumnCount() {
            return 5;
        }

        /** get the column name */
        @Override
        public String getColumnName(final int column) {
            switch(column) {
                case PV_COLUMN:
                    return "PV";
                case COEFFICIENT_COLUMN:
                    return "Coefficient";
                case CUSTOM_LIMITS_COLUMN:
                    return "Custom Limits";
                case LOWER_LIMIT_COLUMN:
                    return "Lower Limit";
                case UPPER_LIMIT_COLUMN:
                    return "Upper Limit";
                default:
                    return "?";
            }
        }

        /** get the class for values associated with the specified column */
        @Override
        public Class getColumnClass(final int column) {
            switch(column) {
                case PV_COLUMN:
                    return String.class;
                case COEFFICIENT_COLUMN:
                    return FormattedNumber.class;
                case CUSTOM_LIMITS_COLUMN:
                    return Boolean.class;
                case LOWER_LIMIT_COLUMN:
                    return FormattedNumber.class;
                case UPPER_LIMIT_COLUMN:
                    return FormattedNumber.class;
                default:
                    return String.class;
            }
        }

        /** determine if the table cell is editable */
        @Override
        public boolean isCellEditable(final int row, final int column) {
            final KnobElement element = _knob.getElements().get(row);
            switch(column) {
                case PV_COLUMN:
                    return true;
                case COEFFICIENT_COLUMN:
                    return true;
                case CUSTOM_LIMITS_COLUMN:
                    return true;
                case LOWER_LIMIT_COLUMN:
                    return element != null && element.isUsingCustomLimits();
                case UPPER_LIMIT_COLUMN:
                    return element != null && element.isUsingCustomLimits();
                default:
                    return false;
            }
        }

        /** get the value at the specified row and column */
        public Object getValueAt(final int row, final int column) {
            final KnobElement element = _knob.getElements().get(row);
            if (element == null) return null;
            switch(column) {
                case PV_COLUMN:
                    return element.getChannelString();
                case COEFFICIENT_COLUMN:
                    return new FormattedNumber(element.getCoefficient());
                case CUSTOM_LIMITS_COLUMN:
                    return element.isUsingCustomLimits();
                case LOWER_LIMIT_COLUMN:
                    return new FormattedNumber(element.getLowerLimit());
                case UPPER_LIMIT_COLUMN:
                    return new FormattedNumber(element.getUpperLimit());
                default:
                    return "?";
            }
        }

        /** Set the value for the specified table cell */
        @Override
        public void setValueAt(final Object value, final int row, final int column) {
            final KnobElement element = _knob.getElements().get(row);
            if (element == null) return;
            switch(column) {
                case PV_COLUMN:
                    setElementChannel(element, value.toString().trim());
                    break;
                case COEFFICIENT_COLUMN:
                    final double coefficient = ((Number) value).doubleValue();
                    element.setCoefficient(coefficient);
                    break;
                case CUSTOM_LIMITS_COLUMN:
                    element.setUsingCustomLimits((Boolean) value);
                    break;
                case LOWER_LIMIT_COLUMN:
                    element.setCustomLowerLimit(((Number) value).doubleValue());
                    break;
                case UPPER_LIMIT_COLUMN:
                    element.setCustomUpperLimit(((Number) value).doubleValue());
                    break;
                default:
                    break;
            }
        }

        /** set the specified element's channel */
        protected void setElementChannel(final KnobElement element, final String channelString) {
            final Accelerator accelerator = _knob.getAccelerator();
            final NodeChannelRef channelRef = accelerator != null ? NodeChannelRef.getInstance(accelerator, channelString) : null;
            if (channelRef != null) {
                element.setNodeChannelRef(channelRef);
            } else {
                element.setPV(channelString);
            }
        }
    }
}
