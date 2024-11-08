package consciouscode.bonsai.components;

import java.awt.Dimension;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import consciouscode.bonsai.channels.Channel;
import consciouscode.bonsai.channels.ChannelProvider;

/**
    A JTable that renders a list of elements, one per row.
    <p>
    The rows of the table correspond to the elements of the
    <code>listChannel</code>, whose contents may be either
    {@link java.util.List} or <code>Object[]</code>.  The selected row is
    synchronized with the <code>selectedElementChannel</code>, which holds one
    of the elements in the list.
    <p>
    The view is most easily configured by adding instances of
    {@link BListTableColumn}, which can configure the underlying Swing models.
*/
public class BListTable extends JTable implements ChannelProvider {

    /**
       Create a table in <code>SINGLE_SELECTION</code> mode.
    */
    public BListTable(Channel listChannel, Channel selectedElementChannel) {
        super(new BListTableModel(listChannel), null, new BListSelectionModel(selectedElementChannel, listChannel));
        myTableModel = (BListTableModel) getModel();
        mySelectedElementChannel = selectedElementChannel;
    }

    /**
       Create a table in <code>SINGLE_SELECTION</code> mode.
    */
    public BListTable(BListTableModel tableModel, Channel selectedElementChannel) {
        super(tableModel, null, new BListSelectionModel(selectedElementChannel, tableModel.getElementsChannel()));
        myTableModel = tableModel;
        mySelectedElementChannel = selectedElementChannel;
    }

    public BListTable(BListTableModel tableModel, BListSelectionModel rowSelectionModel) {
        super(tableModel, null, rowSelectionModel);
        myTableModel = tableModel;
        mySelectedElementChannel = rowSelectionModel.getSelectionChannel();
    }

    public final Object getElementAt(int index) {
        return myTableModel.getElementAt(index);
    }

    public final Object getSelection() {
        return mySelectedElementChannel.getValue();
    }

    public final void selectRow(int row) {
        setRowSelectionInterval(row, row);
    }

    /**
       Adjusts the preferred size to fit a given number of rows.

       @see #setPreferredScrollableViewportSize
    */
    public void setPreferredRows(int rows) {
        Dimension preferred = getPreferredScrollableViewportSize();
        preferred.height = getRowHeight() * rows;
        setPreferredScrollableViewportSize(preferred);
    }

    public void addColumn(BListTableColumn columnSpec) {
        int oldColumnCount = myTableModel.getColumnCount();
        myTableModel.addColumn(columnSpec);
        if ((oldColumnCount == 1) && (myTableModel.getColumnCount() == 1)) {
            TableColumn view = getColumnModel().getColumn(0);
            columnSpec.prepareView(view);
        } else {
            TableColumn view = new TableColumn(oldColumnCount);
            columnSpec.prepareView(view);
            addColumn(view);
        }
    }

    public Channel getSelectedElementChannel() {
        return mySelectedElementChannel;
    }

    public Channel getElementsChannel() {
        return myTableModel.getElementsChannel();
    }

    public Channel getChannel(String channelName) {
        if (channelName.equals("elements")) {
            return getElementsChannel();
        }
        if (channelName.equals("selection")) {
            return mySelectedElementChannel;
        }
        throw new IllegalArgumentException("Bad channelName: " + channelName);
    }

    public Object getChannelValue(String channelName) {
        return getChannel(channelName).getValue();
    }

    public void setChannelValue(String channelName, Object value) {
        getChannel(channelName).setValue(value);
    }

    private BListTableModel myTableModel;

    private Channel mySelectedElementChannel;
}
