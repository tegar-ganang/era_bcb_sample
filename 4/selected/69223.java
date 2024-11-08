package telkku;

import javax.swing.table.AbstractTableModel;
import telkku.channellist.ChannelList;
import telkku.channellist.Channel;

/**
 *
 * @author Omistaja
 */
public class TelkkuTableModel extends AbstractTableModel {

    private ChannelList channelList = null;

    public TelkkuTableModel() {
    }

    public void setChannelList(ChannelList channelList, boolean hasStructureChanged) {
        this.channelList = channelList;
        if (hasStructureChanged) {
            fireTableStructureChanged();
        } else {
            fireTableDataChanged();
        }
    }

    public int getColumnCount() {
        if (channelList != null && channelList.getChannelCount() > 0) {
            return channelList.getChannelCount() + 1;
        }
        return 0;
    }

    public int getRowCount() {
        if (channelList != null && channelList.getChannelCount() > 0) {
            return channelList.getStartTimes().size();
        }
        return 0;
    }

    @Override
    public String getColumnName(int col) {
        if (channelList != null) {
            if (col == 0) {
                return "";
            }
            return channelList.getChannelAt(col - 1).getChannelName();
        }
        return null;
    }

    public Object getValueAt(int row, int col) {
        if (channelList != null) {
            if (col == 0) {
                if (row < channelList.getStartTimes().size()) {
                    return channelList.getStartTimes().get(row).toString();
                }
                return null;
            }
            Channel channel = channelList.getChannelAt(col - 1);
            int h = channelList.getStartTimes().get(row).first();
            int m = channelList.getStartTimes().get(row).second();
            String showName = channel.getShowStartingAt(h, m);
            return showName;
        }
        return null;
    }

    @Override
    public Class getColumnClass(int c) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        fireTableCellUpdated(row, col);
    }
}
