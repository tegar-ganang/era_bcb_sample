package gov.sns.tools.apputils.pvlogbrowser;

import gov.sns.tools.pvlogger.*;
import gov.sns.tools.swing.patternfilter.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.SwingUtilities;

/**
 * ChannelTableModel is the table model for displaying the filtered signals and allowing the
 * user to select/deselect signals.
 *
 * @author  tap
 */
public class ChannelTableModel extends AbstractTableModel implements BrowserControllerListener {

    public static final int SELECTION_COLUMN = 0;

    public static final int CHANNEL_COLUMN = 1;

    private final Object DATA_LOCK;

    protected PatternListFilter<String> _signalFilter;

    protected PatternEventPoster _patternPoster;

    protected ListFilterListener<String> _signalFilterHandler;

    protected BrowserController _controller;

    protected List<String> _signals;

    protected List<String> _filteredSignals;

    /**
	 * Primary constructor
	 * @param controller the selection controller
	 * @param signals the array of signals to be filtered for display
	 */
    public ChannelTableModel(final BrowserController controller, final String[] signals) {
        DATA_LOCK = new Object();
        _controller = controller;
        _patternPoster = null;
        _signalFilterHandler = new SignalFilterHandler();
        setSignalsImmediate(signals);
        _controller.addBrowserControllerListener(this);
    }

    /**
	 * Constructor
	 * @param controller the selection controller
	 * @param channelWrappers the channel wrappers whose signals are to be filtered for display
	 */
    public ChannelTableModel(final BrowserController controller, final ChannelWrapper[] channelWrappers) {
        this(controller, BrowserController.convertToPVs(channelWrappers));
    }

    /**
	 * Constructor
	 * @param controller the selection controller
	 */
    public ChannelTableModel(final BrowserController controller) {
        this(controller, new String[0]);
    }

    /**
	 * Set the signals to be filtered for display.
	 * @param signals the array of signals
	 */
    public void setSignals(final String[] signals) {
        final List<String> signalList = new ArrayList<String>(signals.length);
        for (int index = 0; index < signals.length; index++) {
            signalList.add(signals[index]);
        }
        setSignals(signalList);
    }

    /**
	 * Set the signals to be filtered for display.
	 * @param signals the array of signals
	 */
    public void setSignalsImmediate(final String[] signals) {
        final List<String> signalList = new ArrayList<String>(signals.length);
        for (int index = 0; index < signals.length; index++) {
            signalList.add(signals[index]);
        }
        setSignalsImmediate(signalList);
    }

    /**
	 * Set the list of signals to be filtered for display.
	 * @param signals the list of signals
	 */
    public void setSignals(final List<String> signals) {
        try {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    setSignalsImmediate(signals);
                }
            });
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
		* Set the list of signals to be filtered for display.
	 * @param signals the list of signals
	 */
    public void setSignalsImmediate(final List<String> signals) {
        synchronized (DATA_LOCK) {
            _signals = signals;
            if (_patternPoster != null) {
                setSignalFilter(new PatternListFilter<String>(_signals, _patternPoster));
                _filteredSignals = _signalFilter.getFilteredList();
            } else {
                _filteredSignals = signals;
            }
        }
        fireTableDataChanged();
    }

    /**
	 * Set the pattern filter to be used for filtering the signals for display.
	 * @param patternFilter the filter for filtering signals for display
	 */
    public void setPatternFilter(final PatternEventPoster patternFilter) {
        synchronized (DATA_LOCK) {
            _patternPoster = patternFilter;
            if (_signals != null) {
                setSignalFilter(new PatternListFilter<String>(_signals, _patternPoster));
            }
        }
    }

    /**
	 * Set the list filter used for filtering the list of signals for display
	 * @param filter the filter for filtering the list of signals for display
	 */
    protected void setSignalFilter(final PatternListFilter<String> filter) {
        if (_signalFilter != null) {
            _signalFilter.removeListFilterListener(_signalFilterHandler);
        }
        synchronized (DATA_LOCK) {
            if (_signals != null) {
                _signalFilter = new PatternListFilter<String>(_signals, _patternPoster);
                _signalFilter.addListFilterListener(_signalFilterHandler);
            }
        }
    }

    /**
	 * Select/deselect the list of filtered signals.
	 * @param selected true to select the list of filtered signals and false to deselect them.
	 */
    public void setFilteredSignalsSelected(final boolean selected) {
        synchronized (DATA_LOCK) {
            _controller.selectSignals(_filteredSignals, selected);
        }
    }

    /**
	 * Get the number of rows to display
	 * @return the number of rows to display
	 */
    public int getRowCount() {
        return _filteredSignals.size();
    }

    /**
	 * Get the number of columns to display
	 * @return the number of columns to display
	 */
    public int getColumnCount() {
        return 2;
    }

    /**
	 * Get the value to display in the cell at the specified row and column
	 * @param row the table row of the cell
	 * @param column the table column of the cell
	 * @return the value to display in the cell
	 */
    public Object getValueAt(final int row, final int column) {
        switch(column) {
            case SELECTION_COLUMN:
                return new Boolean(_controller.isSignalSelected(_filteredSignals.get(row)));
            case CHANNEL_COLUMN:
                return _filteredSignals.get(row);
            default:
                return "";
        }
    }

    /**
	 * Set the value of the item displayed in the cell.
	 * @param value the new value for the item in the cell
	 * @param row the row of the cell
	 * @param column the column of the cell
	 */
    @Override
    public void setValueAt(final Object value, final int row, final int column) {
        switch(column) {
            case SELECTION_COLUMN:
                _controller.selectSignal(_filteredSignals.get(row), ((Boolean) value).booleanValue());
                break;
            default:
                break;
        }
    }

    /**
	 * Get the class for displaying the items in the column
	 * @param column the table column
	 * @return the class associated with the items to display in the column
	 */
    @Override
    public Class<?> getColumnClass(final int column) {
        switch(column) {
            case SELECTION_COLUMN:
                return Boolean.class;
            default:
                return String.class;
        }
    }

    /**
	 * Determine if the cell is editable.  Only the selection cell is editable.
	 * @param row the row of the table cell
	 * @param column the column of the table cell
	 */
    @Override
    public boolean isCellEditable(final int row, final int column) {
        return column == SELECTION_COLUMN;
    }

    /**
	 * Get the name to display in the header of the column
	 * @param column the index of the column
	 * @return the name to display in the header of the column
	 */
    @Override
    public String getColumnName(final int column) {
        switch(column) {
            case SELECTION_COLUMN:
                return "Use";
            case CHANNEL_COLUMN:
                return "Channel";
            default:
                return "";
        }
    }

    /** 
	 * event indicating that a snapshot has been selected
	 * @param controller The controller managing selection state
	 * @param snapshot The snapshot that has been selected
	 */
    public void snapshotSelected(final BrowserController controller, final MachineSnapshot snapshot) {
    }

    /**
	 * event indicating that the selected channel group changed
	 * @param source the browser controller sending this notice
	 * @param newGroup the newly selected channel group
	 */
    public void selectedChannelGroupChanged(final BrowserController controller, final ChannelGroup newGroup) {
        if (newGroup != null) {
            ChannelWrapper[] wrappers = newGroup.getChannelWrappers();
            setSignals(BrowserController.convertToPVs(wrappers));
        } else {
            setSignals(new String[0]);
        }
    }

    /**
	 * Event indicating that the selected signals have changed
	 * @param source the controller sending the event
	 * @param selectedSignals the new collection of selected signals
	 */
    public void selectedSignalsChanged(final BrowserController source, final Collection<String> selectedSignals) {
        fireTableDataChanged();
    }

    /**
	 * Handler of filter events from the list filter.
	 */
    protected class SignalFilterHandler implements ListFilterListener<String> {

        /**
		 * Event indicating that a list has been filtered and a new filtered list is available.
		 * When the event occurs, update the filtered list of signals and update the table.
		 * @param source the filter used to filter the list and post the event
		 * @param filteredList the new filtered list
		 */
        public void filteredListChanged(final PatternListFilter<String> source, final List<String> filteredList) {
            synchronized (DATA_LOCK) {
                _filteredSignals = filteredList;
            }
            fireTableDataChanged();
        }
    }
}
