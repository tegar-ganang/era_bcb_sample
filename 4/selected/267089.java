package jhomenet.hw.data;

import java.util.*;

/**
 * An abstract bean for holding sensor hardware data. <br>
 * Id: $Id: AbstractHardwareData.java 1134 2006-01-05 22:37:01Z dhirwinjr $
 * 
 * @author David Irwin
 */
public abstract class AbstractHardwareData<D> {

    /**
	 * Data object Id. (required for use with Hibernate)
	 */
    private long id;

    /**
	 * Data object's hardware Id reference.
	 */
    private String hardwareId;

    /**
	 * The data value's timestamp.
	 */
    private Date timestamp;

    /**
	 * The data input channel number.
	 */
    private static final int DEFAULT_CHANNEL = 0;

    private int channel = DEFAULT_CHANNEL;

    /**
	 * Default constructor.
	 * 
	 * @param hardwareId
	 *            The hardware Id reference
	 */
    public AbstractHardwareData(String hardwareId, D data, int channel) {
        initialize();
        setHardwareId(hardwareId);
        setData(data);
        setChannel(channel);
    }

    public AbstractHardwareData(String hardwareId, D data) {
        this(hardwareId, data, DEFAULT_CHANNEL);
    }

    /**
	 * (required for use with Hibernate)
	 */
    public AbstractHardwareData() {
        super();
    }

    /**
	 * Initialize the hardware data object.
	 */
    private void initialize() {
        setTimestamp(new Date(System.currentTimeMillis()));
    }

    /**
	 * Set the data object Id. (required for use with Hibernate)
	 * 
	 * @param Id
	 */
    private void setId(long Id) {
        this.id = Id;
    }

    /**
	 * Get the data object Id. (required for use with Hibernate)
	 * 
	 * @return Get the data object ID
	 */
    public long getId() {
        return id;
    }

    /**
	 * Set the hardware Id reference.
	 * 
	 * @param hardwareId
	 */
    public void setHardwareId(String hardwareId) {
        this.hardwareId = hardwareId;
    }

    /**
	 * Get the hardware Id reference.
	 * 
	 * @return The hardware Id reference
	 */
    public String getHardwareId() {
        return hardwareId;
    }

    /**
	 * Set the data value timestamp.
	 * 
	 * @param timestamp
	 *            Data value's timestamp
	 */
    private void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
	 * Get the data value's timestamp.
	 * 
	 * @return The data value's timestamp
	 */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
	 * Set the input channel number.
	 * 
	 * @param channel
	 */
    private void setChannel(int channel) {
        this.channel = channel;
    }

    /**
	 * Get the input channel number.
	 * 
	 * @return
	 */
    public int getChannel() {
        return channel;
    }

    /**
	 * Store the data.
	 * 
	 * @param data
	 *            Data object to store
	 */
    protected abstract void setData(D data);

    /**
	 * Get the data object.
	 * 
	 * @return
	 */
    public abstract D getData();

    /**
	 * Get the hardware data as a string representation.
	 * 
	 * @return A string representation of the hardware data
	 */
    public abstract String getDataString();
}
