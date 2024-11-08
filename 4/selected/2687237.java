package be.lassi.ui.patch;

import static be.lassi.util.Util.newArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import be.lassi.base.NameListener;
import be.lassi.base.NamedObject;
import be.lassi.context.ShowContext;
import be.lassi.context.ShowContextListener;
import be.lassi.domain.Channel;
import be.lassi.domain.Channels;
import be.lassi.ui.util.table.SortableTableModel;
import be.lassi.util.NLS;

/**
 * Table model that provides access to information about the
 * channels in the currently selected group.
 *
 */
public class PatchChannelTableModel extends AbstractTableModel implements SortableTableModel, ShowContextListener, NameListener {

    /**
     * The index of the column with the channel number.
     */
    private static final int COLUMN_CHANNEL_NUMBER = 0;

    /**
     * The index of the column with the channel name.
     */
    private static final int COLUMN_CHANNEL_NAME = 1;

    /**
     * The index of the column that indicates whether the channel is patched.
     */
    private static final int COLUMN_CHANNEL_PATCHED = 2;

    /**
     * Copy of the channels in the currently selected channel group,
     * or all show channels if no channel group is selected; the
     * channels are sorted on sortColumn.
     *
     * @aggregation Channel
     */
    private final List<Channel> channels = newArrayList();

    /**
     * The show context.
     */
    private final ShowContext context;

    /**
     * The index of the column on which to sort the table.
     */
    private int sortColumn = COLUMN_CHANNEL_NUMBER;

    /**
     * Constructs a new table model.
     *
     * @param context the show context
     */
    public PatchChannelTableModel(final ShowContext context) {
        this.context = context;
        context.addShowContextListener(this);
        postShowChange();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getColumnClass(final int col) {
        Class<?> clazz = String.class;
        if (col == COLUMN_CHANNEL_PATCHED) {
            clazz = Boolean.class;
        }
        return clazz;
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        return 3;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(final int col) {
        String name = "?";
        switch(col) {
            case COLUMN_CHANNEL_NUMBER:
                name = NLS.get("patch.channels.column.id");
                break;
            case COLUMN_CHANNEL_NAME:
                name = NLS.get("patch.channels.column.channel");
                break;
            case COLUMN_CHANNEL_PATCHED:
                name = NLS.get("patch.channels.column.patched");
                break;
        }
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return channels.size();
    }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(final int row, final int col) {
        Channel channel = channels.get(row);
        Object value;
        switch(col) {
            case COLUMN_CHANNEL_NUMBER:
                value = channel.getId() + 1;
                break;
            case COLUMN_CHANNEL_NAME:
                value = channel.getName();
                break;
            case COLUMN_CHANNEL_PATCHED:
                value = channel.isPatched();
                break;
            default:
                value = "?";
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int row, final int col) {
        return col == COLUMN_CHANNEL_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public void postShowChange() {
        context.getShow().getChannels().addNameListener(this);
        channels.clear();
        Channels c = context.getShow().getChannels();
        for (int i = 0; i < c.size(); i++) {
            channels.add(c.get(i));
        }
        sort();
        fireTableDataChanged();
    }

    /**
     * {@inheritDoc}
     */
    public void preShowChange() {
        context.getShow().getChannels().removeNameListener(this);
    }

    /**
     * Sets the index of the column on which the table should be sorted.
     *
     * @param sortColumn the column to sort on
     */
    public void setSortColumn(final int sortColumn) {
        this.sortColumn = sortColumn;
        sort();
        fireTableDataChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object value, final int row, final int col) {
        if (col == COLUMN_CHANNEL_NAME) {
            Channel channel = channels.get(row);
            channel.removeNameListener(this);
            channel.setName((String) value);
            channel.addNameListener(this);
        }
    }

    /**
     * Gets the channel at given row.
     *
     * @param row the row index of the channel to retrieve
     * @return the channel at given row
     */
    public Channel getChannel(final int row) {
        return channels.get(row);
    }

    private void sort() {
        if (sortColumn == COLUMN_CHANNEL_NUMBER) {
            sortOnChannelNumber();
        } else if (sortColumn == COLUMN_CHANNEL_NAME) {
            sortOnChannelName();
        } else if (sortColumn == COLUMN_CHANNEL_PATCHED) {
            sortOnChannelNumberPatchedFirst();
        } else {
            throw new IllegalArgumentException("Illegal sort column: " + sortColumn);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fireTableDataChanged() {
        sort();
        super.fireTableDataChanged();
    }

    private void sortOnChannelName() {
        Collections.sort(channels, new Comparator<Channel>() {

            public int compare(final Channel channel1, final Channel channel2) {
                String name1 = channel1.getName();
                String name2 = channel2.getName();
                return name1.compareTo(name2);
            }
        });
    }

    private void sortOnChannelNumber() {
        Collections.sort(channels, new Comparator<Channel>() {

            public int compare(final Channel channel1, final Channel channel2) {
                return channel1.getId() - channel2.getId();
            }
        });
    }

    private void sortOnChannelNumberPatchedFirst() {
        Collections.sort(channels, new Comparator<Channel>() {

            public int compare(final Channel channel1, final Channel channel2) {
                int id1 = channel1.getId();
                int id2 = channel2.getId();
                boolean patched1 = channel1.isPatched();
                boolean patched2 = channel2.isPatched();
                int result = 0;
                if (patched1 == patched2) {
                    result = id1 - id2;
                } else if (patched1) {
                    result = 1;
                } else {
                    result = -1;
                }
                return result;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void nameChanged(final NamedObject object) {
        int row = -1;
        for (int i = 0; row == -1 && i < channels.size(); i++) {
            Channel channel = channels.get(i);
            if (channel == object) {
                row = i;
            }
        }
        if (row != -1) {
            fireTableCellUpdated(row, COLUMN_CHANNEL_NAME);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getSortColumn() {
        return sortColumn;
    }

    /**
     * {@inheritDoc}
     */
    public Object getObject(final int row) {
        return channels.get(row);
    }

    public void shift(final int target, final int[] indexes) {
        new ChannelShifter(channels).shift(target, indexes);
    }
}
