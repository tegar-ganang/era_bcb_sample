package jhomenet.ui.table.model;

import java.util.Vector;
import jhomenet.commons.utils.FormatUtils;

/**
 *
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class DefaultDataTableModel extends AbstractTableModel<DefaultDataTableRow> {

    /**
	 * Class parameters.
	 */
    public static enum Columns {

        /**
		 * Data
		 */
        DATA(0), /**
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
    public DefaultDataTableModel() {
        super();
        columnIdentifiers.add("Data");
        columnIdentifiers.add("I/O channel");
        columnIdentifiers.add("Timestamp");
    }

    /**
	 * @see jhomenet.ui.table.model.AbstractTableModel#getColumnIdentifiers()
	 */
    @Override
    protected Vector<String> getColumnIdentifiers() {
        return this.columnIdentifiers;
    }

    /**
	 * @see jhomenet.ui.table.model.AbstractTableModel#getValueAt(int, int)
	 */
    @Override
    public Object getValueAt(int rowIndex, int colIndex) {
        DefaultDataTableRow row = getRow(rowIndex);
        if (colIndex == Columns.DATA.getColumnIndex()) {
            return row.getDataAsString();
        } else if (colIndex == Columns.CHANNEL.getColumnIndex()) {
            return row.getChannel();
        } else if (colIndex == Columns.TIMESTAMP.getColumnIndex()) {
            return FormatUtils.dateTimeFormat.format(row.getTimestamp());
        } else {
            return "";
        }
    }
}
