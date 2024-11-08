package be.lassi.ui.main;

import javax.swing.table.AbstractTableModel;
import be.lassi.context.ShowContext;
import be.lassi.domain.Channel;
import be.lassi.domain.Channels;
import be.lassi.domain.Dimmer;
import be.lassi.domain.Dimmers;

/**
 * Instances of this class can be interrogated by JTable to find the
 * information needed to display variables in tabular format.
 */
public class DimmerTableModel extends AbstractTableModel {

    private ShowContext context;

    public DimmerTableModel(final ShowContext context) {
        this.context = context;
    }

    public void clearPatch() {
        Dimmers dimmers = context.getShow().getDimmers();
        for (int i = 0; i < dimmers.size(); i++) {
            dimmers.get(i).setChannel(null);
        }
        context.getShow().updateChannelInputs();
        fireTableDataChanged();
    }

    public void defaultPatch() {
        Dimmers dimmers = context.getShow().getDimmers();
        Channels channels = context.getShow().getChannels();
        for (int i = 0; i < dimmers.size(); i++) {
            if (i < channels.size()) {
                dimmers.get(i).setChannel(channels.get(i));
            } else {
                dimmers.get(i).setChannel(null);
            }
        }
        context.getShow().updateChannelInputs();
        fireTableDataChanged();
    }

    public Channels getChannels() {
        return context.getShow().getChannels();
    }

    @SuppressWarnings("unchecked")
    public Class<Object> getColumnClass(final int col) {
        Class result = String.class;
        if (col == 2) {
            return result = Channel.class;
        }
        return result;
    }

    /**
     * Keep this method in sync with getColumnName(int) and getValueAt(int, int).
     *
     */
    public int getColumnCount() {
        return 3;
    }

    /**
     * This overides the default of A, B, C, ... AA, AB, ...
     *
     * Keep this method in sync with getValueAt(int, int) and getColumnCount().
     */
    public String getColumnName(final int col) {
        String name;
        switch(col) {
            case 0:
                name = "Id";
                break;
            case 1:
                name = "Dimmer";
                break;
            case 2:
                name = "Channel";
                break;
            default:
                name = "?";
        }
        return name;
    }

    public int getRowCount() {
        return context.getShow().getDimmers().size();
    }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(final int row, final int col) {
        Object value;
        Dimmer dimmer = context.getShow().getDimmers().get(row);
        Channels channels = context.getShow().getChannels();
        switch(col) {
            case 0:
                value = "" + (row + 1);
                break;
            case 1:
                value = dimmer.getName();
                break;
            case 2:
                if (dimmer.getChannelId() == -1) {
                    Channel c = new Channel(0, "not patched");
                    c.setId(-1);
                    value = c;
                } else {
                    value = channels.get(dimmer.getChannelId());
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
    public boolean isCellEditable(final int row, final int col) {
        boolean value;
        switch(col) {
            case 1:
            case 2:
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
        Dimmer dimmer = context.getShow().getDimmers().get(row);
        switch(col) {
            case 1:
                dimmer.setName((String) value);
                break;
            case 2:
                dimmer.setChannel((Channel) value);
                break;
            default:
        }
    }
}
