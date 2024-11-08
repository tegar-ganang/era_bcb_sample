package com.javable.dataview;

/**
 * Maps <code>DataStorage</code> content to table representation
 * <p>
 * This class does not contain any data itself, this is just a mapper
 */
public class DataTableModel extends javax.swing.table.AbstractTableModel implements javax.swing.event.TreeModelListener {

    private DataStorage storage;

    /**
     * Creates new DataTableModel
     * 
     * @param s storage that contains actual data
     */
    public DataTableModel(DataStorage s) {
        storage = s;
        storage.addTreeModelListener(this);
    }

    /**
     * Returns number of columns
     * 
     * @return columns number
     */
    public int getColumnCount() {
        int k = 0;
        for (int i = 0; i < storage.getGroupsSize(); i++) {
            k += storage.getChannelsSize(i);
        }
        return k + 1;
    }

    /**
     * Returns column class
     * 
     * @param column column index
     * @return column class
     */
    public Class getColumnClass(int column) {
        if (column == 0) return Integer.class;
        return super.getColumnClass(column);
    }

    /**
     * Returns column name
     * 
     * @param column column index
     * @return column name
     */
    public java.lang.String getColumnName(int column) {
        int k = 0;
        if (column == 0) return "";
        for (int i = 0; i < storage.getGroupsSize(); i++) {
            for (int j = 0; j < storage.getChannelsSize(i); j++) {
                k++;
                if (k == column) return storage.getChannel(i, j).getName();
            }
        }
        return "null";
    }

    /**
     * Returns number of rows
     * 
     * @return rows number
     */
    public int getRowCount() {
        int k = 0;
        for (int i = 0; i < storage.getGroupsSize(); i++) {
            for (int j = 0; j < storage.getChannelsSize(i); j++) {
                DataChannel channel = storage.getChannel(i, j);
                k = Math.max(k, channel.size());
            }
        }
        return k;
    }

    /**
     * Returns object for the cell
     * 
     * @param row row index
     * @param column column index
     * @return object value
     */
    public java.lang.Object getValueAt(int row, int column) {
        int k = 0;
        if (column == 0) return new Integer(row + 1);
        for (int i = 0; i < storage.getGroupsSize(); i++) {
            for (int j = 0; j < storage.getChannelsSize(i); j++) {
                k++;
                if (k == column) {
                    DataChannel channel = storage.getChannel(i, j);
                    if (row < channel.size()) return new Double(channel.getData(row));
                }
            }
        }
        return "--";
    }

    /**
     * Is this cell editable?
     * 
     * @param row row index
     * @param column column index
     * @return true if editable
     */
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    /**
     * @param evt
     */
    public void treeStructureChanged(javax.swing.event.TreeModelEvent evt) {
    }

    /**
     * @param evt
     */
    public void treeNodesInserted(javax.swing.event.TreeModelEvent evt) {
        if (evt.getTreePath().getPathCount() > 1) {
            fireTableStructureChanged();
        }
    }

    /**
     * @param evt
     */
    public void treeNodesRemoved(javax.swing.event.TreeModelEvent evt) {
        if (evt.getTreePath().getPathCount() > 1) {
            fireTableStructureChanged();
        }
    }

    /**
     * @param evt
     */
    public void treeNodesChanged(javax.swing.event.TreeModelEvent evt) {
    }
}
