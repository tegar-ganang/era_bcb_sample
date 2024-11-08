package jhomenet.ui.table.model;

import java.util.Vector;
import org.apache.log4j.Logger;
import jhomenet.commons.utils.FormatUtils;

/**
 * TODO: Class description.
 * <p>
 * Id: $Id: $
 *
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class StateDataTableModel extends AbstractTableModel<StateDataTableRow> {

    /**
	 * Define the logging object.
	 */
    protected static Logger logger = Logger.getLogger(StateDataTableModel.class);

    /**
	 * Class parameters.
	 */
    public static enum Columns {

        /**
		 * Data value
		 */
        STATE(0), /**
		 * 
		 */
        CHANNEL(1), /**
		 * 
		 */
        TIMESTAMP(2);

        private int columnIndex;

        private Columns(int columnIndex) {
            this.columnIndex = columnIndex;
        }

        /**
		 * @return The column index
		 */
        public int getColumnIndex() {
            return columnIndex;
        }
    }

    /**
	 * The <code>Vector</code> of column identifiers.
	 */
    private final Vector<String> columnIdentifiers = new Vector<String>();

    /**
	 * 
	 */
    public StateDataTableModel() {
        super();
        columnIdentifiers.add("State");
        columnIdentifiers.add("I/O channel");
        columnIdentifiers.add("Timestamp");
    }

    /**
	 * @see jhomenet.ui.table.model.AbstractDataTableModel#getColumnIdentifiers()
	 */
    @Override
    protected Vector<String> getColumnIdentifiers() {
        return columnIdentifiers;
    }

    /**
	 * @see jhomenet.ui.table.model.AbstractDataTableModel#getValueAt(int, int)
	 */
    @Override
    public Object getValueAt(int rowIndex, int colIndex) {
        StateDataTableRow row = getRow(rowIndex);
        if (colIndex == Columns.STATE.getColumnIndex()) {
            return row.toString();
        } else if (colIndex == Columns.CHANNEL.getColumnIndex()) {
            return row.getChannel();
        } else if (colIndex == Columns.TIMESTAMP.getColumnIndex()) {
            return FormatUtils.dateTimeFormat.format(row.getTimestamp());
        } else {
            return "";
        }
    }
}
