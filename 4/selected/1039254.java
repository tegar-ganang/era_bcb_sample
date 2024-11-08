package gov.sns.tools.apputils.pvlogbrowser;

import gov.sns.tools.ArrayTool;
import gov.sns.tools.pvlogger.*;
import java.util.*;
import javax.swing.table.*;

/**
 * SnapshotDetailTableModel is the table model for displaying the channel snapshots associated with the
 * selected machine snapshot.
 *
 * @author  tap
 */
public class SnapshotDetailTableModel extends AbstractTableModel implements BrowserControllerListener {

    protected static final int SIGNAL_COLUMN = 0;

    protected static final int TIMESTAMP_COLUMN = 1;

    protected static final int VALUE_COLUMN = 2;

    protected static final int STATUS_COLUMN = 3;

    protected static final int SEVERITY_COLUMN = 4;

    protected BrowserController _controller;

    protected MachineSnapshot _snapshot;

    protected ChannelSnapshot[] _channelSnapshots;

    protected ChannelSnapshot[] _filteredChannelSnapshots;

    /**
	 * Primary constructor
	 * @param controller the selection controller
	 * @param snapshot the machine snapshot whose filtered channel snapshots are to be displayed
	 */
    public SnapshotDetailTableModel(BrowserController controller, MachineSnapshot snapshot) {
        _controller = controller;
        setMachineSnapshot(snapshot);
    }

    /**
	 * Constructor
	 * @param controller the selection controller
	 */
    public SnapshotDetailTableModel(BrowserController controller) {
        this(controller, null);
    }

    /**
	 * Set the machine snapshot whose filtered channel snapshots are to be displayed
	 * @param snapshot the machine snapshot
	 */
    public void setMachineSnapshot(MachineSnapshot snapshot) {
        _snapshot = snapshot;
        _channelSnapshots = (snapshot != null) ? snapshot.getChannelSnapshots() : new ChannelSnapshot[0];
        _filteredChannelSnapshots = _controller.filterSnapshots(_channelSnapshots);
        fireTableDataChanged();
    }

    /**
	 * Get the number of table rows to display
	 * @return the number of rows
	 */
    public int getRowCount() {
        return _filteredChannelSnapshots.length;
    }

    /**
	 * Get the number of table columns to display
	 * @return the number of columns
	 */
    public int getColumnCount() {
        return 5;
    }

    /**
	 * Get the value of the item to display in the specified table cell
	 * @param row the index of the cell's row
	 * @param column the index of the cell's column
	 * @return the value to display in the cell
	 */
    public Object getValueAt(int row, int column) {
        ChannelSnapshot channelSnapshot = _filteredChannelSnapshots[row];
        switch(column) {
            case SIGNAL_COLUMN:
                return channelSnapshot.getPV();
            case TIMESTAMP_COLUMN:
                return channelSnapshot.getTimestamp();
            case VALUE_COLUMN:
                double[] arrayValue = channelSnapshot.getValue();
                if (arrayValue.length == 1) {
                    return new Double(arrayValue[0]);
                } else {
                    return ArrayTool.asString(arrayValue);
                }
            case STATUS_COLUMN:
                return new Integer(channelSnapshot.getStatus());
            case SEVERITY_COLUMN:
                return new Integer(channelSnapshot.getSeverity());
            default:
                return "";
        }
    }

    /**
	 * Get the name to display in the column's header
	 * @param column the index of the column
	 * @return the name to display in the column's header
	 */
    @Override
    public String getColumnName(int column) {
        switch(column) {
            case SIGNAL_COLUMN:
                return "Signal";
            case TIMESTAMP_COLUMN:
                return "Timestamp";
            case VALUE_COLUMN:
                return "Value";
            case STATUS_COLUMN:
                return "STATUS";
            case SEVERITY_COLUMN:
                return "Severity";
            default:
                return "";
        }
    }

    /** 
	 * event indicating that a snapshot has been selected
	 * @param controller The controller managing selection state
	 * @param snapshot The snapshot that has been selected
	 */
    public void snapshotSelected(BrowserController controller, MachineSnapshot snapshot) {
        setMachineSnapshot(snapshot);
    }

    /**
	 * event indicating that the selected channel group changed
	 * @param source the browser controller sending this notice
	 * @param newGroup the newly selected channel group
	 */
    public void selectedChannelGroupChanged(BrowserController source, ChannelGroup newGroup) {
    }

    /**
	 * Event indicating that the selected signals have changed
	 * @param source the controller sending the event
	 * @param selectedSignals the new collection of selected signals
	 */
    public void selectedSignalsChanged(BrowserController source, Collection<String> selectedSignals) {
        _filteredChannelSnapshots = source.filterSnapshots(_channelSnapshots);
        fireTableDataChanged();
    }
}
