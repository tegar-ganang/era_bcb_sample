package be.lassi.ui.main;

import javax.swing.table.AbstractTableModel;
import be.lassi.domain.Channel;
import be.lassi.domain.Channels;

/**
 * Instances of this class can be interrogated by JTable to find the
 * information needed to display variables in tabular format.
 *
 */
public class ChannelTableModel extends AbstractTableModel {

    /**
     * Collection of <code>Channel</code> objects shown in the table.
     */
    private Channels channels;

    /**
     * Method ChannelTableModel.
     * @param channels
     */
    public ChannelTableModel(final Channels channels) {
        this.channels = channels;
    }

    /**
     * Get channel collection.
     * @return Channels
     */
    public Channels getChannels() {
        return channels;
    }

    /**
     * {@inheritDoc}
     */
    public Class<String> getColumnClass(final int col) {
        return String.class;
    }

    public int getColumnCount() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    public String getColumnName(final int col) {
        String name;
        switch(col) {
            case 0:
                name = "Id";
                break;
            case 1:
                name = "Channel";
                break;
            default:
                name = "?";
        }
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        int rowCount = 0;
        if (channels != null) {
            rowCount = channels.size();
        }
        return rowCount;
    }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(final int row, final int col) {
        Object value;
        Channel channel = channels.get(row);
        switch(col) {
            case 0:
                value = "" + (row + 1);
                break;
            case 1:
                value = channel.getName();
                break;
            default:
                value = "?";
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCellEditable(final int row, final int col) {
        boolean value;
        switch(col) {
            case 1:
                value = true;
                break;
            default:
                value = false;
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public void setValueAt(final Object value, final int row, final int col) {
        Channel channel = channels.get(row);
        switch(col) {
            case 1:
                channel.setName((String) value);
                break;
            default:
        }
    }
}
