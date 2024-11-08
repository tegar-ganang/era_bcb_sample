package com.umc.gui.scanner.tablemodels;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import de.umcProject.xmlbeans.UmcConfigDocument.UmcConfig.Tv.Channel;

public class VdrTableModel extends AbstractTableModel {

    private static final long serialVersionUID = -755973393537731248L;

    private List<Channel> channelList = null;

    private List<String> columns = null;

    public VdrTableModel(List<String> columns) throws Exception {
        this.columns = new ArrayList<String>(columns);
        this.channelList = new ArrayList<Channel>();
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    public int getColumnCount() {
        return columns.size();
    }

    public int getRowCount() {
        return channelList.size();
    }

    public String getColumnName(int column) {
        return columns.get(column);
    }

    public Object getValueAt(int row, int col) {
        switch(col) {
            case 0:
                return channelList.get(row).getName();
            case 1:
                return channelList.get(row).getLink();
            default:
                return "";
        }
    }

    public void setValueAt(Object value, int row, int col) {
        switch(col) {
            case 0:
                channelList.get(row).setName((String) value);
                break;
            case 1:
                channelList.get(row).setLink((String) value);
        }
        this.fireTableRowsUpdated(0, getRowCount() - 1);
    }

    public Channel getChannel(int row) {
        if (row >= 0 && row < getRowCount()) return channelList.get(row);
        return null;
    }

    public void addChannel(Channel channel) {
        channelList.add(channel);
    }

    public void removeChannel(int index) {
        channelList.remove(index);
    }

    public boolean contains(String name) {
        for (Channel b : channelList) {
            if (b.getName() == null || b.getName().equals(name)) return true;
        }
        return false;
    }

    public Class<?> getColumnClass(int col) {
        switch(col) {
            case 0:
                return String.class;
            case 1:
                return String.class;
            default:
                return String.class;
        }
    }
}

;
