package thirdparty.biz.lulanet.swing;

import java.util.Date;
import java.util.Vector;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 * <p>Title: JOrganizer</p>
 * <p>Description: Java organizer for home business management</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: </p>
 * @author Marco Carlini
 * @version 1.0
 */
public class TableSorter extends TableMap {

    static final long serialVersionUID = 0;

    int indexes[];

    int columns[];

    Vector<Integer> sortingColumns = new Vector<Integer>();

    boolean ascending = false;

    AbstractTableModel sortedModel = null;

    static boolean debug = false;

    int compares;

    public TableSorter() {
        indexes = new int[0];
        columns = new int[0];
    }

    public TableSorter(AbstractTableModel model) {
        setModel(model);
        sortedModel = model;
    }

    public static void setDebug(boolean dbg) {
        debug = dbg;
    }

    public void setModel(AbstractTableModel model) {
        super.setModel(model);
        reallocateIndexes();
        reallocateColumns();
        fireTableChanged(new TableModelEvent(this));
    }

    public AbstractTableModel getModel() {
        return sortedModel;
    }

    public int compareRowsByColumn(int row1, int row2, int column) {
        Class<?> type = model.getColumnClass(column);
        TableModel data = model;
        Object o1 = data.getValueAt(row1, column);
        Object o2 = data.getValueAt(row2, column);
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        }
        if (type.getSuperclass() == java.lang.Number.class) {
            Number n1 = (Number) data.getValueAt(row1, column);
            double d1 = n1.doubleValue();
            Number n2 = (Number) data.getValueAt(row2, column);
            double d2 = n2.doubleValue();
            if (d1 < d2) return -1; else if (d1 > d2) return 1; else return 0;
        } else if (type == java.util.Date.class) {
            Date d1 = (Date) data.getValueAt(row1, column);
            long n1 = d1.getTime();
            Date d2 = (Date) data.getValueAt(row2, column);
            long n2 = d2.getTime();
            if (n1 < n2) return -1; else if (n1 > n2) return 1; else return 0;
        } else if (type == String.class) {
            String s1 = (String) data.getValueAt(row1, column);
            String s2 = (String) data.getValueAt(row2, column);
            int result = s1.compareTo(s2);
            if (result < 0) return -1; else if (result > 0) return 1; else return 0;
        } else if (type == Boolean.class) {
            Boolean bool1 = (Boolean) data.getValueAt(row1, column);
            boolean b1 = bool1.booleanValue();
            Boolean bool2 = (Boolean) data.getValueAt(row2, column);
            boolean b2 = bool2.booleanValue();
            if (b1 == b2) return 0; else if (b1) return 1; else return -1;
        } else {
            Object v1 = data.getValueAt(row1, column);
            String s1 = v1.toString();
            Object v2 = data.getValueAt(row2, column);
            String s2 = v2.toString();
            int result = s1.compareTo(s2);
            if (result < 0) return -1; else if (result > 0) return 1; else return 0;
        }
    }

    public int compare(int row1, int row2) {
        compares++;
        for (int level = 0; level < sortingColumns.size(); level++) {
            Integer column = (Integer) sortingColumns.elementAt(level);
            int result = compareRowsByColumn(row1, row2, column.intValue());
            if (result != 0) return ascending ? result : -result;
        }
        return 0;
    }

    public void reallocateIndexes() {
        int rowCount = model.getRowCount();
        indexes = new int[rowCount];
        for (int row = 0; row < rowCount; row++) indexes[row] = row;
    }

    public void reallocateColumns() {
        int columnCount = model.getColumnCount();
        if (debug) System.out.println("Count columns: " + columnCount);
        columns = new int[columnCount];
        for (int column = 0; column < columnCount; column++) columns[column] = column;
    }

    public void orderColumns(int from, int to) {
        if (debug) System.out.println("Move column from: " + from + " to: " + to);
        if (from < to) for (int j = from; j < to; j++) {
            int temp;
            temp = columns[j];
            columns[j] = columns[j + 1];
            columns[j + 1] = temp;
            if (debug) System.out.println("Index: " + j);
        } else for (int j = from; j > to; j--) {
            int temp;
            temp = columns[j];
            columns[j] = columns[j - 1];
            columns[j - 1] = temp;
            if (debug) System.out.println("Index: " + j);
        }
        if (debug) if (from < to) for (int i = from; i <= to; i++) System.out.println("Index at: " + i + "=" + columns[i]); else for (int i = to; i <= from; i++) System.out.println("Index at: " + i + "=" + columns[i]);
    }

    public int getColumn(int index) {
        return columns[index];
    }

    public int getColumnIndex(String col) {
        int i = 0;
        for (i = 0; i < sortedModel.getColumnCount(); i++) {
            if (col.equalsIgnoreCase(sortedModel.getColumnName(i))) return columns[i];
        }
        return -1;
    }

    public void tableChanged(TableModelEvent e) {
        reallocateIndexes();
        reallocateColumns();
        super.tableChanged(e);
    }

    public void checkModel() {
        if (indexes.length != model.getRowCount()) {
            System.err.println("Sorter not informed of a change in model.");
        }
    }

    public void sort(Object sender) {
        checkModel();
        compares = 0;
        shuttlesort((int[]) indexes.clone(), indexes, 0, indexes.length);
    }

    public void n2sort() {
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = i + 1; j < getRowCount(); j++) {
                if (compare(indexes[i], indexes[j]) == -1) {
                    swap(i, j);
                }
            }
        }
    }

    public void shuttlesort(int from[], int to[], int low, int high) {
        if (high - low < 2) {
            return;
        }
        int middle = (low + high) / 2;
        shuttlesort(to, from, low, middle);
        shuttlesort(to, from, middle, high);
        int p = low;
        int q = middle;
        if (high - low >= 4 && compare(from[middle - 1], from[middle]) <= 0) {
            for (int i = low; i < high; i++) {
                to[i] = from[i];
            }
            return;
        }
        for (int i = low; i < high; i++) {
            if (q >= high || (p < middle && compare(from[p], from[q]) <= 0)) {
                to[i] = from[p++];
            } else {
                to[i] = from[q++];
            }
        }
    }

    public void swap(int i, int j) {
        int tmp = indexes[i];
        indexes[i] = indexes[j];
        indexes[j] = tmp;
    }

    public Object getValueAt(int aRow, int aColumn) {
        checkModel();
        return model.getValueAt(indexes[aRow], aColumn);
    }

    public void setValueAt(Object aValue, int aRow, int aColumn) {
        checkModel();
        model.setValueAt(aValue, indexes[aRow], aColumn);
    }

    public void sortByColumn(int column) {
        sortByColumn(getColumn(column), true);
    }

    public void sortByColumn(int column, boolean ascending) {
        this.ascending = ascending;
        sortingColumns.removeAllElements();
        sortingColumns.addElement(new Integer(getColumn(column)));
        sort(this);
        fireTableChanged(new TableModelEvent(this));
    }

    public int getIndexAt(int ind) {
        return indexes[ind];
    }

    public Vector getRow(int row) {
        Vector temp = new Vector();
        for (int i = 1; i < getColumnCount(); i++) temp.addElement(getValueAt(row, i));
        return temp;
    }
}
