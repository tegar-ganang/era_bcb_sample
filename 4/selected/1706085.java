package com.markpiper.tvtray;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

/**
 * @author mark
 */
public class PrefsChannelListModel extends AbstractTableModel {

    private ChannelManager channelManager;

    private ChannelManager updateManager;

    private String[] cols = { "Name", "Alias", "Active" };

    private final int COL_NAME = 0;

    private final int COL_ALIAS = 1;

    private final int COL_ACTIVE = 2;

    public PrefsChannelListModel(ChannelManager mgr) {
        channelManager = mgr;
        updateManager = (ChannelManager) mgr.clone();
    }

    public void swapRows(int row1, int row2) {
        updateManager.changeOrder(row1, row2);
        fireTableChanged(new TableModelEvent(this));
        fireTableStructureChanged();
    }

    public int getRowCount() {
        return updateManager.getNumberOfChannels();
    }

    public int getColumnCount() {
        return cols.length;
    }

    public String getColumnName(int arg0) {
        return cols[arg0];
    }

    public Class getColumnClass(int arg0) {
        if (arg0 < 2) {
            return String.class;
        } else {
            return Boolean.class;
        }
    }

    /**
     *
     */
    public boolean isCellEditable(int row, int col) {
        switch(col) {
            case COL_NAME:
                return false;
            case COL_ALIAS:
                return true;
            case COL_ACTIVE:
                return true;
        }
        return false;
    }

    public Object getValueAt(int row, int col) {
        Channel ch = updateManager.getChannelByOrder(row);
        switch(col) {
            case COL_NAME:
                return ch.getName();
            case COL_ALIAS:
                return ch.getAlias();
            case COL_ACTIVE:
                return new Boolean(ch.isActive());
        }
        return null;
    }

    /**
     *
     */
    public void setValueAt(Object value, int row, int col) {
        Channel ch = updateManager.getChannelByOrder(row);
        switch(col) {
            case COL_NAME:
                ch.setName((String) value);
                return;
            case COL_ALIAS:
                ch.setAlias((String) value);
                return;
            case COL_ACTIVE:
                ch.setActive(((Boolean) value).booleanValue());
                return;
        }
    }

    /**
     *
     */
    public void addTableModelListener(TableModelListener arg0) {
    }

    /**
     *
     */
    public void removeTableModelListener(TableModelListener arg0) {
    }

    /** 
     * Called to update ChannelManager with changes made within this model
     *
     */
    public ChannelManager getUpdatedManager() {
        return updateManager;
    }
}
