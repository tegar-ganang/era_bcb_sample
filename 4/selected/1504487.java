package fr.soleil.bensikin.models;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import javax.swing.JOptionPane;
import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.TangoDs.TangoConst;
import fr.soleil.bensikin.components.BensikinComparator;
import fr.soleil.bensikin.components.snapshot.detail.SnapshotAttributeComparator;
import fr.soleil.bensikin.containers.BensikinFrame;
import fr.soleil.bensikin.containers.snapshot.SnapshotDetailTabbedPane;
import fr.soleil.bensikin.containers.snapshot.SnapshotDetailTabbedPaneContent;
import fr.soleil.bensikin.data.snapshot.BensikinDAOKey;
import fr.soleil.bensikin.data.snapshot.Snapshot;
import fr.soleil.bensikin.data.snapshot.SnapshotAttribute;
import fr.soleil.bensikin.data.snapshot.SnapshotAttributeReadValue;
import fr.soleil.bensikin.data.snapshot.SnapshotAttributeValue;
import fr.soleil.bensikin.data.snapshot.SnapshotAttributeWriteValue;
import fr.soleil.bensikin.data.snapshot.SnapshotAttributes;
import fr.soleil.bensikin.tools.Messages;
import fr.soleil.comete.dao.AbstractKey;
import fr.soleil.comete.dao.util.DefaultDataArrayDAO;
import fr.soleil.comete.util.DataArray;

/**
 * The table model used by SnapshotDetailTable, this model lists the current
 * list of snapshots. Its rows are SnapshotAttribute objects. A static reference
 * Hashtable allows for keeping track of precedently created instances (using as
 * reference the Snapshot the rows of which the table is displaying).
 *
 * @author CLAISSE
 */
public class SnapshotDetailTableModel extends SortedTableModel {

    public static enum SnapshotColumns {

        NAME, WRITE, READ, DELTA, CAN_SET, WRITE_ABS, READ_ABS, DELTA_ABS
    }

    private static final long serialVersionUID = -4689031046562249463L;

    private SnapshotAttribute[] rows;

    private SnapshotAttribute[] rowsFilter;

    private static String[] columnsNames;

    private boolean filter;

    private List<Integer> attributesNumRowList = new ArrayList<Integer>();

    private static HashMap<Snapshot, SnapshotDetailTableModel> snapshotDetailTableModelHashtable = null;

    private HashMap<SnapshotAttributeValue, AbstractKey> cometeKeys = new HashMap<SnapshotAttributeValue, AbstractKey>();

    public List<Integer> getAttributesNumRowList() {
        return attributesNumRowList;
    }

    public void setAttributesNumRowList(List<Integer> attributesNumRowList) {
        this.attributesNumRowList = attributesNumRowList;
    }

    public boolean isFilter() {
        return filter;
    }

    public void setFilter(boolean filter) {
        this.filter = filter;
    }

    /**
     * Creates a statically referenced instance of model for the given Snapshot.
     *
     * @param snapshot
     *            The model of which we're displaying the details
     * @return The instance found for the specified Snapshot reference, or a new
     *         one is none exist yet.
     */
    public static SnapshotDetailTableModel getInstance(Snapshot snapshot) {
        if (snapshotDetailTableModelHashtable == null) {
            snapshotDetailTableModelHashtable = new HashMap<Snapshot, SnapshotDetailTableModel>();
        }
        if (snapshotDetailTableModelHashtable.containsKey(snapshot)) {
            return snapshotDetailTableModelHashtable.get(snapshot);
        } else {
            SnapshotDetailTableModel newSnapshotDetailTableModel = new SnapshotDetailTableModel();
            newSnapshotDetailTableModel.load(snapshot);
            snapshotDetailTableModelHashtable.put(snapshot, newSnapshotDetailTableModel);
            return newSnapshotDetailTableModel;
        }
    }

    /**
     * Initializes the columns titles.
     */
    protected SnapshotDetailTableModel() {
        super();
        if (columnsNames == null) {
            columnsNames = new String[8];
            columnsNames[0] = Messages.getMessage("SNAPSHOT_DETAIL_COLUMNS_NAME");
            columnsNames[1] = Messages.getMessage("SNAPSHOT_DETAIL_COLUMNS_W");
            columnsNames[2] = Messages.getMessage("SNAPSHOT_DETAIL_COLUMNS_R");
            columnsNames[3] = Messages.getMessage("SNAPSHOT_DETAIL_COLUMNS_DELTA");
            columnsNames[4] = Messages.getMessage("SNAPSHOT_DETAIL_COLUMNS_CHECKBOX");
            columnsNames[5] = Messages.getMessage("SNAPSHOT_DETAIL_COLUMNS_W_ABS");
            columnsNames[6] = Messages.getMessage("SNAPSHOT_DETAIL_COLUMNS_R_ABS");
            columnsNames[7] = Messages.getMessage("SNAPSHOT_DETAIL_COLUMNS_DELTA_ABS");
        }
    }

    /**
     * Builds itself with the list of attributes contained in <code>snapshot</code>.
     * 
     * @param snapshot
     *            The Snapshot containing the attributes to display
     */
    public void load(Snapshot snapshot) {
        SnapshotAttributes snapshotAttributes = snapshot.getSnapshotAttributes();
        if (snapshotAttributes == null) {
            this.rows = null;
        } else {
            this.rows = snapshotAttributes.getSnapshotAttributes();
        }
        cometeKeys.clear();
        fireTableDataChanged();
    }

    /**
     * Builds the matched list of attributes contained in snapshot.
     *
     * @param snapshot
     *            The Snapshot containing the attributes to display
     */
    public void reloadMatchingValues(Snapshot snapshot) {
        SnapshotAttributes snapshotAttributes = snapshot.getSnapshotAttributes();
        SnapshotAttribute snapshotAttribute[] = snapshotAttributes.getSnapshotAttributes();
        if (isFilter()) {
            this.rowsFilter = new SnapshotAttribute[getAttributesNumRowList().size()];
            for (int i = 0; i < getAttributesNumRowList().size(); i++) {
                this.rowsFilter[i] = snapshotAttribute[getAttributesNumRowList().get(i)];
            }
            this.rows = this.rowsFilter;
        } else {
            this.rows = snapshotAttributes.getSnapshotAttributes();
        }
        cometeKeys.clear();
        fireTableDataChanged();
    }

    @Override
    public int getColumnCount() {
        return SnapshotColumns.values().length;
    }

    @Override
    public int getRowCount() {
        if (rows == null) {
            return 0;
        }
        return rows.length;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        switch(columnIndex) {
            case 1:
                SnapshotAttributeWriteValue writeValue = (SnapshotAttributeWriteValue) getValueAt(rowIndex, columnIndex);
                if (writeValue.getDataFormat() == SnapshotAttributeValue.SPECTRUM_DATA_FORMAT) {
                    if (aValue instanceof Double[] || aValue instanceof Float[] || aValue instanceof Integer[] || aValue instanceof Short[] || aValue instanceof Byte[] || aValue instanceof String[] || aValue instanceof Boolean[]) {
                        writeValue.setSpectrumValue((Object[]) aValue);
                        writeValue.setModified(true);
                        SnapshotDetailTabbedPane pane = SnapshotDetailTabbedPane.getInstance();
                        SnapshotDetailTabbedPaneContent currentTab = (SnapshotDetailTabbedPaneContent) pane.getSelectedComponent();
                        currentTab.setModified(true);
                        return;
                    } else {
                        String title = Messages.getMessage("MODIFY_SNAPSHOT_INVALID_VALUE_TITLE");
                        String msg = Messages.getMessage("MODIFY_SNAPSHOT_INVALID_VALUE_MESSAGE");
                        JOptionPane.showMessageDialog(BensikinFrame.getInstance(), msg, title, JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                boolean isOKValue = writeValue.validateValue(aValue);
                if (!isOKValue) {
                    String title = Messages.getMessage("MODIFY_SNAPSHOT_INVALID_VALUE_TITLE");
                    String msg = Messages.getMessage("MODIFY_SNAPSHOT_INVALID_VALUE_MESSAGE");
                    JOptionPane.showMessageDialog(BensikinFrame.getInstance(), msg, title, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                SnapshotAttributeWriteValue afterValue = new SnapshotAttributeWriteValue(writeValue.getDataFormat(), writeValue.getDataType(), writeValue.getValue());
                afterValue.setDisplayFormat(writeValue.getDisplayFormat());
                afterValue.setValue(aValue);
                afterValue.setSettable(writeValue.isSettable());
                if (!(afterValue.toXMLString().equals(writeValue.toXMLString()) || afterValue.toFormatedString().equals(writeValue.toFormatedString()))) {
                    writeValue.setModified(true);
                    SnapshotDetailTabbedPane pane = SnapshotDetailTabbedPane.getInstance();
                    SnapshotDetailTabbedPaneContent currentTab = (SnapshotDetailTabbedPaneContent) pane.getSelectedComponent();
                    currentTab.setModified(true);
                }
                writeValue.setValue(aValue);
                rows[rowIndex].setWriteValue(writeValue);
                break;
            case 4:
                writeValue = (SnapshotAttributeWriteValue) getValueAt(rowIndex, 1);
                if (aValue instanceof Boolean) {
                    writeValue.setSettable(((Boolean) aValue).booleanValue());
                    SnapshotDetailTabbedPaneContent currentTab = (SnapshotDetailTabbedPaneContent) SnapshotDetailTabbedPane.getInstance().getSelectedComponent();
                    currentTab.setMayFilter(true);
                }
                break;
            default:
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rows != null && rowIndex > -1 && rowIndex < rows.length) {
            switch(columnIndex) {
                case 0:
                    return rows[rowIndex].getAttribute_complete_name();
                case 1:
                    return rows[rowIndex].getWriteValue();
                case 2:
                    return rows[rowIndex].getReadValue();
                case 3:
                    return rows[rowIndex].getDeltaValue();
                case 4:
                    return new Boolean(rows[rowIndex].getWriteValue().isSettable());
                case 5:
                    return rows[rowIndex].getWriteAbsValue();
                case 6:
                    return rows[rowIndex].getReadAbsValue();
                case 7:
                    return rows[rowIndex].getDeltaAbsValue();
                default:
                    return null;
            }
        }
        return null;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnsNames[columnIndex];
    }

    public SnapshotAttribute getSnapshotAttributeAtRow(int rowIndex) {
        if (rows != null) {
            return rows[rowIndex];
        } else {
            return null;
        }
    }

    /**
     * @param newValue
     */
    public void selectAllOrNone(boolean newValue) {
        if (rows == null) {
            return;
        }
        boolean hasCausedChanges = false;
        for (int i = 0; i < rows.length; i++) {
            SnapshotAttribute attr = this.getSnapshotAttributeAtRow(i);
            SnapshotAttributeWriteValue writeValue = attr.getWriteValue();
            boolean oldValue = writeValue.isSettable();
            if (oldValue != newValue) {
                hasCausedChanges = true;
            }
            writeValue.setSettable(newValue);
        }
        this.fireTableRowsUpdated(0, this.getRowCount() - 1);
        if (hasCausedChanges) {
            SnapshotDetailTabbedPaneContent currentTab = (SnapshotDetailTabbedPaneContent) SnapshotDetailTabbedPane.getInstance().getSelectedComponent();
            currentTab.setMayFilter(true);
        }
    }

    public void transferSelectedReadToWrite() {
        if (rows == null) {
            return;
        }
        for (int i = 0; i < rows.length; i++) {
            SnapshotAttribute attr = this.getSnapshotAttributeAtRow(i);
            SnapshotAttributeReadValue readValue = attr.getReadValue();
            SnapshotAttributeWriteValue writeValue = attr.getWriteValue();
            if (writeValue.isSettable() && (!writeValue.isNotApplicable()) && (!readValue.isNotApplicable())) {
                switch(attr.getData_format()) {
                    case AttrDataFormat._SCALAR:
                        Object scalarValue = readValue.getScalarValue();
                        if (scalarValue != null) {
                            setValueAt(scalarValue, i, 1);
                        }
                        break;
                    case AttrDataFormat._SPECTRUM:
                        Object[] spectrumValue = readValue.getSpectrumValue();
                        if (spectrumValue != null) {
                            setValueAt(spectrumValue, i, 1);
                        }
                        break;
                    case AttrDataFormat._IMAGE:
                        Object[] imageValue = readValue.getImageValue();
                        if (imageValue != null) {
                            setValueAt(imageValue, i, 1);
                        }
                        break;
                }
            }
        }
    }

    /**
     * Sorts the table's lines relative to the specified column. If the the
     * table is already sorted relative to this column, reverses the sort.
     *
     * @param clickedColumnIndex
     *            The index of the column to sort the lines by
     */
    @Override
    public void sort(int clickedColumnIndex) {
        switch(clickedColumnIndex) {
            case 0:
                sortByColumn(SnapshotAttributeComparator.COMPARE_NAME);
                sortedColumn = clickedColumnIndex;
                break;
            case 1:
                sortByColumn(SnapshotAttributeComparator.COMPARE_WRITE_VALUE);
                sortedColumn = clickedColumnIndex;
                break;
            case 2:
                sortByColumn(SnapshotAttributeComparator.COMPARE_READ_VALUE);
                sortedColumn = clickedColumnIndex;
                break;
            case 3:
                sortByColumn(SnapshotAttributeComparator.COMPARE_DELTA_VALUE);
                sortedColumn = clickedColumnIndex;
                break;
            case 5:
                sortByColumn(SnapshotAttributeComparator.COMPARE_WRITE_ABS_VALUE);
                sortedColumn = clickedColumnIndex;
                break;
            case 6:
                sortByColumn(SnapshotAttributeComparator.COMPARE_READ_ABS_VALUE);
                sortedColumn = clickedColumnIndex;
                break;
            case 7:
                sortByColumn(SnapshotAttributeComparator.COMPARE_DELTA_ABS_VALUE);
                sortedColumn = clickedColumnIndex;
                break;
        }
    }

    /**
     * Sorts the table's lines relative to the specified field. If the the table
     * is already sorted relative to this column, reverses the sort.
     *
     * @param compareCase
     *            The type of field to sort the lines by
     */
    protected void sortByColumn(int compareCase) {
        int newSortType = BensikinComparator.getNewSortType(this.idSort);
        Vector<SnapshotAttribute> v = new Vector<SnapshotAttribute>();
        for (int i = 0; i < rows.length; i++) {
            v.add(this.getSnapshotAttributeAtRow(i));
        }
        SnapshotAttributeComparator comparator = new SnapshotAttributeComparator(compareCase);
        if (compareCase == SnapshotAttributeComparator.COMPARE_NAME) {
            Collections.sort(v, comparator);
        } else {
            Vector<SnapshotAttribute> notComparableAttrs = new Vector<SnapshotAttribute>();
            Vector<SnapshotAttribute> booleanScalarValues = new Vector<SnapshotAttribute>();
            Vector<SnapshotAttribute> stringScalarValues = new Vector<SnapshotAttribute>();
            Vector<SnapshotAttribute> numberScalarValues = new Vector<SnapshotAttribute>();
            Vector<SnapshotAttribute> booleanSpectrumValues = new Vector<SnapshotAttribute>();
            Vector<SnapshotAttribute> stringSpectrumValues = new Vector<SnapshotAttribute>();
            Vector<SnapshotAttribute> numberSpectrumValues = new Vector<SnapshotAttribute>();
            Vector<SnapshotAttribute> booleanImageValues = new Vector<SnapshotAttribute>();
            Vector<SnapshotAttribute> stringImageValues = new Vector<SnapshotAttribute>();
            Vector<SnapshotAttribute> numberImageValues = new Vector<SnapshotAttribute>();
            int notNull = -1;
            notNull = sortNotComparableAttrFirst(v);
            for (int i = 0; i < notNull; i++) {
                notComparableAttrs.add(v.get(i));
            }
            int index = notNull;
            while (index > 0) {
                v.remove(0);
                index--;
            }
            for (int i = 0; i < v.size(); i++) {
                SnapshotAttribute attr = v.get(i);
                switch(attr.getData_format()) {
                    case AttrDataFormat._SCALAR:
                        switch(attr.getData_type()) {
                            case TangoConst.Tango_DEV_BOOLEAN:
                                booleanScalarValues.add(attr);
                                break;
                            case TangoConst.Tango_DEV_STRING:
                                stringScalarValues.add(attr);
                                break;
                            case TangoConst.Tango_DEV_CHAR:
                            case TangoConst.Tango_DEV_UCHAR:
                            case TangoConst.Tango_DEV_SHORT:
                            case TangoConst.Tango_DEV_USHORT:
                            case TangoConst.Tango_DEV_LONG:
                            case TangoConst.Tango_DEV_ULONG:
                            case TangoConst.Tango_DEV_FLOAT:
                            case TangoConst.Tango_DEV_DOUBLE:
                                numberScalarValues.add(attr);
                                break;
                            default:
                                notComparableAttrs.add(attr);
                                break;
                        }
                        break;
                    case AttrDataFormat._SPECTRUM:
                        switch(attr.getData_type()) {
                            case TangoConst.Tango_DEV_BOOLEAN:
                                booleanSpectrumValues.add(attr);
                                break;
                            case TangoConst.Tango_DEV_STRING:
                                stringSpectrumValues.add(attr);
                                break;
                            case TangoConst.Tango_DEV_CHAR:
                            case TangoConst.Tango_DEV_UCHAR:
                            case TangoConst.Tango_DEV_SHORT:
                            case TangoConst.Tango_DEV_USHORT:
                            case TangoConst.Tango_DEV_LONG:
                            case TangoConst.Tango_DEV_ULONG:
                            case TangoConst.Tango_DEV_FLOAT:
                            case TangoConst.Tango_DEV_DOUBLE:
                                numberSpectrumValues.add(attr);
                                break;
                            default:
                                notComparableAttrs.add(attr);
                                break;
                        }
                        break;
                    case AttrDataFormat._IMAGE:
                        switch(attr.getData_type()) {
                            case TangoConst.Tango_DEV_BOOLEAN:
                                booleanImageValues.add(attr);
                                break;
                            case TangoConst.Tango_DEV_STRING:
                                stringImageValues.add(attr);
                                break;
                            case TangoConst.Tango_DEV_CHAR:
                            case TangoConst.Tango_DEV_UCHAR:
                            case TangoConst.Tango_DEV_SHORT:
                            case TangoConst.Tango_DEV_USHORT:
                            case TangoConst.Tango_DEV_LONG:
                            case TangoConst.Tango_DEV_ULONG:
                            case TangoConst.Tango_DEV_FLOAT:
                            case TangoConst.Tango_DEV_DOUBLE:
                                numberImageValues.add(attr);
                                break;
                            default:
                                notComparableAttrs.add(attr);
                                break;
                        }
                        break;
                    default:
                        notComparableAttrs.add(attr);
                        break;
                }
            }
            notNull = sortNotComparableValueFirst(booleanScalarValues, compareCase);
            for (int i = 0; i < notNull; i++) {
                notComparableAttrs.add(booleanScalarValues.get(i));
            }
            if (booleanScalarValues.size() > 0) index = notNull;
            while (index > 0) {
                booleanScalarValues.remove(0);
                index--;
            }
            notNull = sortNotComparableValueFirst(numberScalarValues, compareCase);
            for (int i = 0; i < notNull; i++) {
                notComparableAttrs.add(numberScalarValues.get(i));
            }
            if (numberScalarValues.size() > 0) index = notNull;
            while (index > 0) {
                numberScalarValues.remove(0);
                index--;
            }
            notNull = sortNotComparableValueFirst(stringScalarValues, compareCase);
            for (int i = 0; i < notNull; i++) {
                notComparableAttrs.add(stringScalarValues.get(i));
            }
            if (stringScalarValues.size() > 0) index = notNull;
            while (index > 0) {
                stringScalarValues.remove(0);
                index--;
            }
            notNull = sortNotComparableValueFirst(booleanSpectrumValues, compareCase);
            for (int i = 0; i < notNull; i++) {
                notComparableAttrs.add(booleanSpectrumValues.get(i));
            }
            index = notNull;
            while (index > 0) {
                booleanSpectrumValues.remove(0);
                index--;
            }
            notNull = sortNotComparableValueFirst(numberSpectrumValues, compareCase);
            for (int i = 0; i < notNull; i++) {
                notComparableAttrs.add(numberSpectrumValues.get(i));
            }
            index = notNull;
            while (index > 0) {
                numberSpectrumValues.remove(0);
                index--;
            }
            notNull = sortNotComparableValueFirst(stringSpectrumValues, compareCase);
            for (int i = 0; i < notNull; i++) {
                notComparableAttrs.add(stringSpectrumValues.get(i));
            }
            index = notNull;
            while (index > 0) {
                stringSpectrumValues.remove(0);
                index--;
            }
            notNull = sortNotComparableValueFirst(booleanImageValues, compareCase);
            for (int i = 0; i < notNull; i++) {
                notComparableAttrs.add(booleanImageValues.get(i));
            }
            index = notNull;
            while (index > 0) {
                booleanImageValues.remove(0);
                index--;
            }
            notNull = sortNotComparableValueFirst(numberImageValues, compareCase);
            for (int i = 0; i < notNull; i++) {
                notComparableAttrs.add(numberImageValues.get(i));
            }
            index = notNull;
            while (index > 0) {
                numberImageValues.remove(0);
                index--;
            }
            notNull = sortNotComparableValueFirst(stringImageValues, compareCase);
            for (int i = 0; i < notNull; i++) {
                notComparableAttrs.add(stringImageValues.get(i));
            }
            index = notNull;
            while (index > 0) {
                stringImageValues.remove(0);
                index--;
            }
            Collections.sort(booleanScalarValues, comparator);
            Collections.sort(numberScalarValues, comparator);
            Collections.sort(stringScalarValues, comparator);
            Collections.sort(booleanSpectrumValues, comparator);
            Collections.sort(numberSpectrumValues, comparator);
            Collections.sort(stringSpectrumValues, comparator);
            Collections.sort(booleanImageValues, comparator);
            Collections.sort(numberScalarValues, comparator);
            Collections.sort(stringScalarValues, comparator);
            v.clear();
            v.addAll(notComparableAttrs);
            v.addAll(booleanScalarValues);
            v.addAll(numberScalarValues);
            v.addAll(stringScalarValues);
            v.addAll(booleanSpectrumValues);
            v.addAll(numberSpectrumValues);
            v.addAll(stringSpectrumValues);
            v.addAll(booleanImageValues);
            v.addAll(numberImageValues);
            v.addAll(stringImageValues);
        }
        if (newSortType == BensikinComparator.SORT_DOWN) {
            Collections.reverse(v);
        }
        SnapshotAttribute[] newRows = new SnapshotAttribute[rows.length];
        Enumeration<SnapshotAttribute> enumer = v.elements();
        int i = 0;
        while (enumer.hasMoreElements()) {
            newRows[i] = enumer.nextElement();
            i++;
        }
        this.rows = newRows;
        this.fireTableDataChanged();
        this.idSort = newSortType;
    }

    protected int sortNotComparableAttrFirst(Vector<SnapshotAttribute> attrs) {
        Vector<SnapshotAttribute> sorted = new Vector<SnapshotAttribute>();
        int index = 0;
        for (int i = 0; i < attrs.size(); i++) {
            SnapshotAttribute attr = attrs.get(i);
            if (attr == null || attr.getData_format() == SnapshotAttributeValue.NOT_APPLICABLE_DATA_FORMAT) {
                sorted.add(attrs.get(i));
                index++;
            }
        }
        for (int i = 0; i < attrs.size(); i++) {
            SnapshotAttribute attr = attrs.get(i);
            if (attr != null && attr.getData_format() != SnapshotAttributeValue.NOT_APPLICABLE_DATA_FORMAT) {
                sorted.add(attrs.get(i));
            }
        }
        attrs.clear();
        attrs.addAll(sorted);
        sorted.clear();
        sorted = null;
        return index;
    }

    protected int sortNotComparableValueFirst(Vector<SnapshotAttribute> comps, int valueType) {
        Vector<SnapshotAttribute> sorted = new Vector<SnapshotAttribute>();
        int index = 0;
        for (int i = 0; i < comps.size(); i++) {
            SnapshotAttribute attr = comps.get(i);
            SnapshotAttributeValue value = null;
            switch(valueType) {
                case SnapshotAttributeComparator.COMPARE_READ_VALUE:
                    value = attr.getReadValue();
                    break;
                case SnapshotAttributeComparator.COMPARE_WRITE_VALUE:
                    value = attr.getWriteValue();
                    break;
                case SnapshotAttributeComparator.COMPARE_DELTA_VALUE:
                    value = attr.getDeltaValue();
                    break;
                case SnapshotAttributeComparator.COMPARE_READ_ABS_VALUE:
                    value = attr.getReadAbsValue();
                    break;
                case SnapshotAttributeComparator.COMPARE_WRITE_ABS_VALUE:
                    value = attr.getWriteAbsValue();
                    break;
                case SnapshotAttributeComparator.COMPARE_DELTA_ABS_VALUE:
                    value = attr.getDeltaAbsValue();
                    break;
            }
            if ((value.getDataFormat() == SnapshotAttributeValue.NOT_APPLICABLE_DATA_FORMAT || value.isNotApplicable()) || (value.getScalarValue() == null && value.getSpectrumValue() == null && value.getImageValue() == null)) {
                sorted.add(comps.get(i));
                index++;
            }
            attr = null;
            value = null;
        }
        for (int i = 0; i < comps.size(); i++) {
            SnapshotAttribute attr = comps.get(i);
            SnapshotAttributeValue value = null;
            switch(valueType) {
                case SnapshotAttributeComparator.COMPARE_READ_VALUE:
                    value = attr.getReadValue();
                    break;
                case SnapshotAttributeComparator.COMPARE_WRITE_VALUE:
                    value = attr.getWriteValue();
                    break;
                case SnapshotAttributeComparator.COMPARE_DELTA_VALUE:
                    value = attr.getDeltaValue();
                    break;
                case SnapshotAttributeComparator.COMPARE_READ_ABS_VALUE:
                    value = attr.getReadValue();
                    break;
                case SnapshotAttributeComparator.COMPARE_WRITE_ABS_VALUE:
                    value = attr.getWriteValue();
                    break;
                case SnapshotAttributeComparator.COMPARE_DELTA_ABS_VALUE:
                    value = attr.getDeltaValue();
                    break;
            }
            if ((value.getDataFormat() != SnapshotAttributeValue.NOT_APPLICABLE_DATA_FORMAT && !value.isNotApplicable()) && (value.getScalarValue() != null || value.getSpectrumValue() != null || value.getImageValue() != null)) {
                sorted.add(comps.get(i));
            }
            attr = null;
            value = null;
        }
        comps.clear();
        comps.addAll(sorted);
        sorted.clear();
        sorted = null;
        return index;
    }

    public AbstractKey getCometeKey(int row, int column) {
        AbstractKey key = null;
        Object value = getValueAt(row, column);
        if (value instanceof SnapshotAttributeReadValue) {
            SnapshotAttributeReadValue readValue = (SnapshotAttributeReadValue) value;
            key = cometeKeys.get(readValue);
            if (key == null) {
                switch(readValue.getDataFormat()) {
                    case AttrDataFormat._SPECTRUM:
                        switch(readValue.getDataType()) {
                            case TangoConst.Tango_DEV_CHAR:
                            case TangoConst.Tango_DEV_UCHAR:
                            case TangoConst.Tango_DEV_LONG:
                            case TangoConst.Tango_DEV_ULONG:
                            case TangoConst.Tango_DEV_SHORT:
                            case TangoConst.Tango_DEV_USHORT:
                            case TangoConst.Tango_DEV_FLOAT:
                            case TangoConst.Tango_DEV_DOUBLE:
                                String name = (String) getValueAt(row, 0);
                                DefaultDataArrayDAO dao = new DefaultDataArrayDAO();
                                ArrayList<DataArray> dataList = new ArrayList<DataArray>();
                                Object spectrumValue = readValue.getSpectrumValue();
                                if ((!"Nan".equals(spectrumValue)) && (spectrumValue != null) && (spectrumValue instanceof Number[])) {
                                    DataArray spectrumArray = new DataArray();
                                    spectrumArray.setId(name);
                                    spectrumArray.setDisplayName(name);
                                    for (int i = 0; i < Array.getLength(spectrumValue); i++) {
                                        Number nb = (Number) Array.get(spectrumValue, i);
                                        if (nb == null) spectrumArray.add(new Integer(i + 1), new Double(Double.NaN)); else spectrumArray.add(new Integer(i + 1), nb.doubleValue());
                                    }
                                    dataList.add(spectrumArray);
                                }
                                dao.setData(dataList);
                                key = new BensikinDAOKey(name, dao);
                                break;
                        }
                        break;
                    default:
                        key = null;
                }
            }
        }
        return key;
    }
}
