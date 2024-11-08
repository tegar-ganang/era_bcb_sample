package be.lassi.ui.patch;

import javax.swing.table.AbstractTableModel;
import be.lassi.base.Holder;
import be.lassi.base.NameListener;
import be.lassi.base.NamedObject;
import be.lassi.context.ShowContext;
import be.lassi.context.ShowContextListener;
import be.lassi.domain.Channel;
import be.lassi.domain.Dimmer;
import be.lassi.domain.Dimmers;
import be.lassi.ui.util.table.SortableTableModel;
import be.lassi.util.NLS;

/**
 * Instances of this class can be interrogated by JTable to find the
 * information needed to display variables in tabular format.
 *
 */
public class PatchDetailTableModel extends AbstractTableModel implements SortableTableModel, ShowContextListener, NameListener {

    private final ShowContext context;

    private final Patch patch;

    private final Patcher patcher;

    private final Holder<Boolean> stageFollowsSelection;

    /**
     * Constructs a new model.
     *
     * @param context the show context
     * @param patcher the patcher
     * @param patch the patch information
     */
    public PatchDetailTableModel(final ShowContext context, final Patcher patcher, final Patch patch, final Holder<Boolean> stageFollowsSelection) {
        this.context = context;
        this.patcher = patcher;
        this.patch = patch;
        this.stageFollowsSelection = stageFollowsSelection;
        context.addShowContextListener(this);
        postShowChange();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getColumnClass(final int col) {
        Class<?> clazz = String.class;
        if (col == 0) {
            clazz = Boolean.class;
        }
        return clazz;
    }

    /**
     * {@inheritDoc}
     */
    public int getColumnCount() {
        return 6;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(final int col) {
        String name;
        switch(col) {
            case PatchDetail.ON:
                name = NLS.get("patch.dimmers.column.on");
                break;
            case PatchDetail.DIMMER_NUMBER:
                name = NLS.get("patch.dimmers.column.dimmerNumber");
                break;
            case PatchDetail.DIMMER_NAME:
                name = NLS.get("patch.dimmers.column.dimmer");
                break;
            case PatchDetail.LANBOX_CHANNEL_NUMBER:
                name = NLS.get("patch.dimmers.column.lanbox");
                break;
            case PatchDetail.CHANNEL_NUMBER:
                name = NLS.get("patch.dimmers.column.channelNumber");
                break;
            case PatchDetail.CHANNEL_NAME:
                name = NLS.get("patch.dimmers.column.channelName");
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
        return context.getShow().getNumberOfDimmers();
    }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(final int row, final int col) {
        Object value;
        PatchDetail detail = patch.getDetail(row);
        switch(col) {
            case PatchDetail.ON:
                value = detail.isOn();
                break;
            case PatchDetail.DIMMER_NUMBER:
                value = detail.getDimmer().getId() + 1;
                break;
            case PatchDetail.DIMMER_NAME:
                value = detail.getDimmer().getName();
                break;
            case PatchDetail.LANBOX_CHANNEL_NUMBER:
                int channelId = detail.getDimmer().getLanboxChannelId();
                value = channelId + 1;
                break;
            case PatchDetail.CHANNEL_NUMBER:
                Channel channel = detail.getDimmer().getChannel();
                if (channel == null) {
                    value = "";
                } else {
                    value = channel.getId() + 1;
                }
                break;
            case PatchDetail.CHANNEL_NAME:
                channel = detail.getDimmer().getChannel();
                if (channel == null) {
                    value = "";
                } else {
                    value = channel.getName();
                }
                break;
            default:
                value = "?";
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public int getSortColumn() {
        return patch.getSortColumn();
    }

    /**
     * {@inheritDoc}
     */
    public void setSortColumn(final int column) {
        patch.setSortColumn(column);
        fireTableDataChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int row, final int col) {
        boolean editable = false;
        if (col == PatchDetail.ON) {
            editable = !stageFollowsSelection.getValue();
        } else if (col == PatchDetail.DIMMER_NAME) {
            editable = true;
        } else if (col == PatchDetail.CHANNEL_NAME) {
            PatchDetail detail = patch.getDetail(row);
            editable = detail.getDimmer().isPatched();
        }
        return editable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object value, final int row, final int col) {
        PatchDetail detail = patch.getDetail(row);
        if (col == PatchDetail.ON) {
            boolean on = ((Boolean) value).booleanValue();
            patcher.setOn(row, on);
            fireTableDataChanged();
        } else if (col == PatchDetail.DIMMER_NAME) {
            setDimmerName(detail, (String) value);
        } else if (col == PatchDetail.CHANNEL_NAME) {
            setChannelName(detail, (String) value);
        }
    }

    private void setDimmerName(final PatchDetail detail, final String name) {
        Dimmer dimmer = detail.getDimmer();
        dimmer.removeNameListener(this);
        dimmer.setName(name);
        dimmer.addNameListener(this);
    }

    private void setChannelName(final PatchDetail detail, final String name) {
        Channel channel = detail.getDimmer().getChannel();
        channel.removeNameListener(this);
        channel.setName(name);
        channel.addNameListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public void postShowChange() {
        context.getShow().getDimmers().addNameListener(this);
        context.getShow().getChannels().addNameListener(this);
        fireTableDataChanged();
    }

    /**
     * {@inheritDoc}
     */
    public void preShowChange() {
        context.getShow().getDimmers().removeNameListener(this);
        context.getShow().getChannels().removeNameListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public void nameChanged(final NamedObject object) {
        if (object instanceof Channel) {
            channelNameChanged((Channel) object);
        } else if (object instanceof Dimmer) {
            dimmerNameChanged((Dimmer) object);
        }
    }

    private void channelNameChanged(final Channel changedChannel) {
        for (int i = 0; i < patch.getDimmerCount(); i++) {
            PatchDetail detail = patch.getDetail(i);
            Channel channel = detail.getDimmer().getChannel();
            if (channel == changedChannel) {
                fireTableCellUpdated(i, PatchDetail.CHANNEL_NAME);
            }
        }
    }

    private void dimmerNameChanged(final Dimmer changedDimmer) {
        int row = -1;
        Dimmers dimmers = context.getShow().getDimmers();
        for (int i = 0; row == -1 && i < dimmers.size(); i++) {
            Dimmer dimmer = dimmers.get(i);
            if (dimmer == changedDimmer) {
                row = i;
            }
        }
        if (row != -1) {
            fireTableCellUpdated(row, PatchDetail.DIMMER_NAME);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getObject(final int row) {
        return patch.getDetail(row);
    }
}
