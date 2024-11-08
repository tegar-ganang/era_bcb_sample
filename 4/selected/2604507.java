package jhomenet.commons.responsive.condition;

import jhomenet.commons.GeneralApplicationContext;
import jhomenet.commons.hw.sensor.Sensor;
import jhomenet.commons.responsive.ResponsiveException;

/**
 * An abstract implementation of the sensor responsive system (SRS)
 * condition interface. In particular, this class can be used as a
 * base class for any concrete condition implementation that uses a
 * jHomeNet sensor as its input. In particular, a SRS condition object
 * is going to define some type of condition and using a sensor condition
 * the system will retrieve the current information from the sensor and
 * then compare it against the pre-defined test condition.
 *
 * @see jhomenet.commons.responsive.condition.Condition
 * @see jhomenet.commons.responsive.conditino.AbstractCondition
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public abstract class SensorCondition<T extends Sensor> extends AbstractCondition {

    /**
	 * Reference to the sensor object.
	 */
    private T sensor;

    private String hardwareAddr;

    /**
	 * 
	 */
    private Integer channel;

    /**
	 * The default channel.
	 */
    private static final Integer defaultChannel = 0;

    /**
	 * Constructor.
	 * 
	 * @param conditionName
	 * @param sensor
	 * @param channel
	 */
    public SensorCondition(String conditionName, T sensor, Integer channel) {
        super(conditionName);
        this.sensor = sensor;
        if (this.sensor != null) this.hardwareAddr = this.sensor.getHardwareAddr();
        this.channel = channel;
    }

    /**
	 * Constructor.
	 * 
	 * @param conditionName
	 * @param sensor
	 */
    public SensorCondition(String conditionName, T sensor) {
        this(conditionName, sensor, defaultChannel);
    }

    /**
	 * @param hardwareAddr the hardwareAddr to set
	 */
    public void setHardwareAddr(String hardwareAddr) {
        this.hardwareAddr = hardwareAddr;
    }

    /**
	 * Get the sensor's hardware address.
	 * 
	 * @return
	 */
    public String getHardwareAddr() {
        return hardwareAddr;
    }

    /**
	 * 
	 * @return
	 */
    public final T getSensor() {
        return this.sensor;
    }

    /**
	 * 
	 * @param sensor
	 */
    final void setSensor(T sensor) {
        this.sensor = sensor;
    }

    /**
	 * @return the channel
	 */
    public Integer getChannel() {
        return channel;
    }

    /**
	 * 
	 * @param channel
	 */
    private void setChannel(Integer channel) {
        this.channel = channel;
    }

    /**
	 * @see java.lang.Object#hashCode()
	 */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());
        result = prime * result + ((sensor == null) ? 0 : sensor.hashCode());
        return result;
    }

    /**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        final SensorCondition other = (SensorCondition) obj;
        if (channel == null) {
            if (other.channel != null) return false;
        } else if (!channel.equals(other.channel)) return false;
        if (sensor == null) {
            if (other.sensor != null) return false;
        } else if (!sensor.equals(other.sensor)) return false;
        return true;
    }
}
