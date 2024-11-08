package jhomenet.ui.table.model;

import java.util.Vector;
import org.apache.log4j.Logger;
import jhomenet.commons.utils.FormatUtils;

/**
 * TODO: Class description.
 *
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class ValueDataTableModel extends AbstractTableModel<ValueDataTableRow> {

    /**
	 * Define the logging object.
	 */
    protected static Logger logger = Logger.getLogger(ValueDataTableModel.class);

    /**
	 * Class parameters.
	 */
    public static enum Columns {

        /**
		 * Data value
		 */
        VALUE(0), /**
		 * Data unit
		 */
        UNIT(1), /**
		 * 
		 */
        CHANNEL(2), /**
		 * 
		 */
        TIMESTAMP(3);

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
    public ValueDataTableModel() {
        super();
        columnIdentifiers.add("Value");
        columnIdentifiers.add("Unit");
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
        ValueDataTableRow row = getRow(rowIndex);
        if (colIndex == Columns.VALUE.getColumnIndex()) {
            return row.getValue();
        } else if (colIndex == Columns.UNIT.getColumnIndex()) {
            return row.getUnit().toString();
        } else if (colIndex == Columns.CHANNEL.getColumnIndex()) {
            return row.getChannel();
        } else if (colIndex == Columns.TIMESTAMP.getColumnIndex()) {
            return FormatUtils.dateTimeFormat.format(row.getTimestamp());
        } else {
            return "";
        }
    }
}
