package be.lassi.ui.sheet;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumnModel;
import be.lassi.base.Listener;
import be.lassi.context.ShowContext;
import be.lassi.context.ShowContextListener;
import be.lassi.cues.LightCues;
import be.lassi.domain.Channel;
import be.lassi.domain.Groups;
import be.lassi.domain.Show;

/**
 * Abstract class where the classes {@link SheetTableModelHeaders SheetTableModelHeaders} and
 * {@link SheetTableModelHeaders SheetTableModelHeaders} share what they have in
 * common.  This class contains the 'row' definitions, whereas the
 * subclasses handle the 'column' oriented stuff.
 *
 */
public abstract class SheetTableModel extends AbstractTableModel implements ShowContextListener {

    /**
     * The show context.
     */
    private final ShowContext context;

    /**
     * Listens to changes in the show groups: the table needs to be refreshed if
     * the groups get enabled or disabled.
     */
    private final Listener groupListener;

    private final TableColumnModel columnModel = new DefaultTableColumnModel();

    /**
     * Constructs a new instance.
     *
     * @param context the show context
     */
    protected SheetTableModel(final ShowContext context) {
        this.context = context;
        groupListener = new Listener() {

            public void changed() {
                groupsChanged();
            }
        };
        context.addShowContextListener(this);
        postShowChange();
    }

    protected void groupsChanged() {
        fireTableDataChanged();
    }

    public TableColumnModel getColumnModel() {
        return columnModel;
    }

    /**
     * Gets the current show.
     *
     * @return the current show
     */
    protected Show getShow() {
        return context.getShow();
    }

    /**
     * Gets the show light cues.
     *
     * @return the light cues
     */
    protected LightCues getCues() {
        return getShow().getCues().getLightCues();
    }

    /**
     * The total number of rows in the sheet.
     */
    public int getRowCount() {
        Groups groups = context.getShow().getGroups();
        int channelCount = 0;
        if (groups.isEnabled()) {
            channelCount = groups.getEnabledGroupsChannelCount();
        } else {
            channelCount = context.getShow().getNumberOfChannels();
        }
        return 1 + context.getShow().getNumberOfSubmasters() + channelCount;
    }

    /**
     * Gets the row index for the channel at given index.  Note that depending
     * on the current group selection, this index is not necessarily the same
     * as the channel id.
     *
     * @param index the index of channel in the collection of channels that are currently shown
     * @return int
     */
    protected int getRowChannel(final int index) {
        return getShow().getNumberOfSubmasters() + index + 1;
    }

    /**
     * Gets the row index for given channel.
     *
     * @param channel the channel for which to determine the row number
     * @return the row number, -1 if channel not currently visible
     */
    protected int getRowChannelWithId(final int channelId) {
        int row = -1;
        Groups groups = context.getShow().getGroups();
        if (groups.isEnabled()) {
            int index = groups.getIndexOfChannelWithId(channelId);
            row = getRowChannel(index);
        } else {
            row = getRowChannel(channelId);
        }
        return row;
    }

    /**
     * Gets the row index for given submaster.
     *
     * @param submasterIndex
     * @return int
     */
    protected int getRowSubmaster(final int submasterIndex) {
        return submasterIndex + 1;
    }

    /**
     * Gets the index in the channel collection of the channel on given row.
     *
     * @param row
     * @return int
     */
    protected int getChannelIndex(final int row) {
        int channelIndex = row - getShow().getNumberOfSubmasters() - 1;
        Groups groups = context.getShow().getGroups();
        if (groups.isEnabled()) {
            Channel channel = groups.getChannel(channelIndex);
            channelIndex = channel.getId();
        }
        return channelIndex;
    }

    /**
     * Gets the index in the submaster collection of the submaster on given row.
     *
     * @param row
     * @return int
     */
    protected int getSubmasterIndex(final int row) {
        return row - 1;
    }

    /**
     * Indicates whether the row with given index contains timing information.
     *
     * @param row
     * @return boolean
     */
    public boolean isRowTiming(final int row) {
        return row == 0;
    }

    /**
     * Indicates whether the row with given index contains submaster information.
     *
     * @param row
     * @return boolean
     */
    public boolean isRowSubmaster(final int row) {
        return row > 0 && row <= getShow().getNumberOfSubmasters();
    }

    /**
     * Indicates whether the row with given index contains channel information.
     *
     * @param row
     * @return boolean
     */
    public boolean isRowChannel(final int row) {
        return row > getShow().getNumberOfSubmasters();
    }

    /**
     * Notifies listeners that a cue channel cell has been updated.
     *
     * @param cueIndex
     * @param channelId
     */
    public void fireCueChannelUpdated(final int cueIndex, final int channelId) {
        int row = getRowChannelWithId(channelId);
        fireTableCellUpdated(row, cueIndex);
    }

    /**
     * Notifies listeners that a cue submaster cell has been updated.
     *
     * @param cueIndex
     * @param submasterIndex
     */
    public void fireCueSubmasterUpdated(final int cueIndex, final int submasterIndex) {
        int row = getRowSubmaster(submasterIndex);
        for (int i = 0; i < getRowCount(); i++) {
            fireTableCellUpdated(i, cueIndex);
        }
    }

    /**
     * Notifies listeners that row has been updated.
     *
     * @param row
     */
    public void fireTableRowUpdated(final int row) {
        fireTableRowsUpdated(row, row);
    }

    /**
     * Indicates whether given row has a special grid line.
     *
     * @param row
     * @return
     */
    public boolean isRowWithSpecialGridLine(final int row) {
        boolean result = false;
        if (row == 0) {
            result = true;
        } else if (row == getShow().getNumberOfSubmasters()) {
            result = true;
        }
        return result;
    }

    public boolean isColumnWithSpecialGridLine(final int col) {
        return false;
    }

    public boolean isCurrentCueColumn(final int col) {
        return false;
    }

    public abstract boolean keyAction(final int actionId, final int[] rows, final int[] cols);

    public ShowContext getContext() {
        return context;
    }

    /**
     * {@inheritDoc}
     * Start listening for changes in the show groups.
     */
    public void postShowChange() {
        getContext().getShow().getGroups().getListeners().add(groupListener);
    }

    /**
     * {@inheritDoc}
     * Stop listening for changes in the show groups.
     */
    public void preShowChange() {
        getContext().getShow().getGroups().getListeners().remove(groupListener);
    }
}
