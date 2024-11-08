package jhomenet.ui.table.model;

import java.util.Date;
import jhomenet.commons.hw.data.AbstractHardwareData;

/**
 * TODO: class description
 * <p>
 * Id: $Id: $
 *
 * @author Dave Irwin (david.irwin@jhu.edu)
 */
public abstract class AbstractDataTableRow<T extends AbstractHardwareData> extends AbstractTableRow {

    /**
	 * Data property name.
	 */
    public static final String PROPERTY_DATA = "data";

    /**
	 * 
	 */
    protected final T data;

    /**
	 * Constructor.
	 * 
	 * @param data
	 */
    public AbstractDataTableRow(T data) {
        super();
        if (data == null) throw new IllegalArgumentException("Data cannot be null!");
        this.data = data;
    }

    /**
	 * 
	 * @return
	 */
    protected T getData() {
        return data;
    }

    /**
	 * 
	 * @return
	 */
    public final Integer getChannel() {
        return data.getChannel();
    }

    /**
	 * 
	 * @return
	 */
    public Date getTimestamp() {
        return data.getTimestamp();
    }
}
