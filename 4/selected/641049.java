package jhomenet.commons.hw.data;

import java.util.*;

/**
 * A non-mutable abstract implementation of the <code>HardwareData</code>
 * hardware data interface. 
 * 
 * @see jhomenet.commons.hw.data.HardwareData
 * @see jhomenet.commons.hw.data.HardwareValueData
 * @see jhomenet.commons.hw.data.HardwareStateData
 * @see jhomenet.commons.hw.states.State
 * 
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public abstract class AbstractHardwareData<D> implements HardwareData<D> {

    /**
	 * Data object Id. (required for use with Hibernate)
	 */
    private long id;

    /**
	 * Hardware address reference.
	 */
    protected String hardwareAddrRef;

    /**
	 * The data value's timestamp.
	 */
    protected Date timestamp;

    /**
	 * Define the communication channel the data was read from.
	 */
    protected Integer channel;

    /**
	 * The default communication channel.
	 */
    private static final Integer DEFAULT_CHANNEL = 0;

    /**
	 * Constructor.
	 * 
	 * @param channel The I/O channel associated with this data
	 * @param hardwareAddrRef A hardware reference to the underlying hardware sensor
	 */
    public AbstractHardwareData(Integer channel, String hardwareAddrRef) {
        super();
        setTimestamp(new Date(System.currentTimeMillis()));
        setHardwareAddrRef(hardwareAddrRef);
        setChannel(channel);
    }

    /**
	 * Constructor.
	 * 
	 * @param hardwareAddrRef A hardware reference to the underlying hardware sensor
	 */
    public AbstractHardwareData(String hardwareAddrRef) {
        this(DEFAULT_CHANNEL, hardwareAddrRef);
    }

    /**
	 * Empty constructor (required for use with Hibernate).
	 */
    public AbstractHardwareData() {
        super();
        this.channel = DEFAULT_CHANNEL;
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
	 * @see jhomenet.commons.hw.data.HardwareData#getHardwareAddrRef()
	 */
    public String getHardwareAddrRef() {
        return hardwareAddrRef;
    }

    /**
	 * Get the data's timestamp value.
	 * 
	 * @see jhomenet.commons.hw.data.HardwareData#getTimestamp()
	 */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
	 * @see jhomenet.commons.hw.data.HardwareData#getChannel()
	 */
    public Integer getChannel() {
        return channel;
    }

    /**
	 * Get the data.
	 * 
	 * @return
	 */
    public abstract D getDataObject();

    /**
	 * Get the hardware data as a string representation.
	 * 
	 * @return A string representation of the hardware data
	 */
    public abstract String getDataString();

    /**
	 * @param channel The channel to set.
	 */
    private void setChannel(int channel) {
        this.channel = channel;
    }

    /**
	 * Set the data object Id. (required for use with Hibernate)
	 * 
	 * @param Id
	 */
    void setId(long Id) {
        this.id = Id;
    }

    /**
	 * @param hardwareAddrRef
	 */
    private void setHardwareAddrRef(String hardwareAddrRef) {
        this.hardwareAddrRef = hardwareAddrRef;
    }

    /**
	 * @param timestamp the timestamp to set
	 */
    void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
	 * @see java.lang.Object#hashCode()
	 */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());
        result = prime * result + ((hardwareAddrRef == null) ? 0 : hardwareAddrRef.hashCode());
        result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
        return result;
    }

    /**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final AbstractHardwareData other = (AbstractHardwareData) obj;
        if (channel == null) {
            if (other.channel != null) return false;
        } else if (!channel.equals(other.channel)) return false;
        if (hardwareAddrRef == null) {
            if (other.hardwareAddrRef != null) return false;
        } else if (!hardwareAddrRef.equals(other.hardwareAddrRef)) return false;
        if (timestamp == null) {
            if (other.timestamp != null) return false;
        } else if (!timestamp.equals(other.timestamp)) return false;
        return true;
    }
}
