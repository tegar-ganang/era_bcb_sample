package de.nava.risotto;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import javax.swing.table.AbstractTableModel;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.utils.ItemComparator;

public class ItemModel extends AbstractTableModel {

    private static final SimpleDateFormat date_fmt = new SimpleDateFormat("dd.MM.yyyy, HH:mm:ss");

    private String[] headers = { "Title", "Found" };

    private ItemIF[] items;

    private ChannelIF channel;

    public ItemModel(ChannelIF channel) {
        setChannel(channel);
    }

    public ChannelIF getChannel() {
        return channel;
    }

    public void setChannel(ChannelIF channel) {
        this.channel = channel;
        if (channel != null) {
            items = (ItemIF[]) channel.getItems().toArray(new de.nava.informa.impl.basic.Item[0]);
            Arrays.sort(items, new ItemComparator(true));
        } else {
            items = null;
        }
        this.fireTableDataChanged();
    }

    /**
   * Gets the item for the given rowIndex in the table.
   */
    public ItemIF getItem(int rowIndex) {
        return items[rowIndex];
    }

    /**
   * Adds a new item at the beginning of the existing array of items.
   */
    public void addItem(ItemIF newItem) {
        ItemIF[] newItems = new de.nava.informa.impl.basic.Item[items.length + 1];
        newItems[0] = newItem;
        System.arraycopy(items, 0, newItems, 1, items.length);
        items = newItems;
        this.fireTableDataChanged();
    }

    public int getRowCount() {
        if (items != null) return items.length; else return 0;
    }

    public int getColumnCount() {
        return headers.length;
    }

    public String getColumnName(int columnIndex) {
        return headers[columnIndex];
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) return items[rowIndex].getTitle(); else if (columnIndex == 1) return date_fmt.format(items[rowIndex].getFound()); else throw new IllegalArgumentException("Invalid column " + columnIndex + " specified.");
    }
}
