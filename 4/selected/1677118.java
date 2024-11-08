package jhomenet.ui.table.model;

import java.util.Date;
import jhomenet.commons.hw.data.HardwareData;

/**
 *
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class DefaultDataTableRow extends AbstractTableRow {

    /**
	 * 
	 */
    private final HardwareData data;

    /**
	 * 
	 */
    public DefaultDataTableRow(HardwareData data) {
        this.data = data;
    }

    /**
	 * @see jhomenet.ui.table.model.AbstractTableRow#getRowId()
	 */
    @Override
    protected Object getRowId() {
        return data.hashCode();
    }

    /**
	 * 
	 * @return
	 */
    protected String getDataAsString() {
        return data.getDataString();
    }

    /**
	 * 
	 * @return
	 */
    protected Integer getChannel() {
        return data.getChannel();
    }

    /**
	 * 
	 * @return
	 */
    protected Date getTimestamp() {
        return data.getTimestamp();
    }
}
