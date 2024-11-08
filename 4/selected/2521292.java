package jhomenet.commons.hw.sensor;

import java.util.List;
import java.util.ArrayList;
import java.io.*;
import org.apache.log4j.*;
import jhomenet.commons.hw.data.HardwareData;
import jhomenet.commons.hw.*;
import jhomenet.commons.polling.PollingIntervals;

/**
 * This is the superclass of all sensor hardware as part of the Homenet
 * system. It extends the <code>HomenetHardware</code> class that provides
 * general functionality to all registered hardware. The <code>Sensor</code>
 * class adds additional functionality common to all sensors including reading
 * data from the sensor, storing data, and the sensor polling type.
 * 
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public abstract class Sensor<D extends HardwareData> extends HomenetHardware implements Serializable {

    /**
	 * Serial version ID information - used for the serialization process.
	 */
    private static final long serialVersionUID = 00001;

    /**
	 * Define a logging mechanism.
	 */
    private static Logger logger = Logger.getLogger(Sensor.class.getName());

    /**
	 * Reference to the current data list. This list only contains the most recent
	 * hardware data where each slot in the list corresponds to the hardware
	 * data for each corresponding I/O channel.
	 * <p>
	 * For example, if a sensor has four I/O channels, then the current data list will
	 * have a length of 4. And slot 0 will correspond to I/O channel 0, slot 1 will
	 * correspond to I/O channel 1 and so on. 
	 * <p>
	 * To get a history of hardware data, it's recommended that the data be retrieved
	 * from the persistence layer. This will keep the overall memory footprint of the
	 * server smaller.
	 */
    private List<D> currentData;

    /**
	 * Current data property name.
	 */
    public static final String PROPERTYNAME_CURRENTDATA = "currentData";

    /**
	 * The sensor's polling interval.
	 */
    private volatile PollingIntervals pollingInterval = PollingIntervals.NO_POLLING;

    /**
	 * Polling interval property name.
	 */
    public static final String PROPERTYNAME_POLLINGINTERVAL = "pollingInterval";

    /**
	 * A flag used to indicate whether this is the sensor's first poll or not.
	 */
    protected transient boolean firstPoll = true;

    /**
	 * Flag indicating whether the sensor read was successful or not.
	 */
    protected transient boolean readFailed = false;

    /**
	 * Default constructor.
	 * 
	 * @param hardwareAddr The physical address of the hardware sensor
	 * @param hardwareSetupDescription The hardware setup description
	 * @param numChannels The number of communication channels
	 */
    public Sensor(String hardwareAddr, String hardwareSetupDescription, String hardwareAppDescription, Integer numChannels) {
        super(hardwareAddr, hardwareSetupDescription, hardwareAppDescription, numChannels);
        resetData();
    }

    /**
	 * 
	 * @param hardwareAppDescription
	 * @param numChannels
	 */
    public Sensor(String hardwareAppDescription, Integer numChannels) {
        super(hardwareAppDescription, numChannels);
        resetData();
    }

    /**
	 * Copying constructor.
	 * 
	 * @param sensor
	 */
    public Sensor(Sensor sensor) {
        super(sensor);
        this.pollingInterval = sensor.getPollingInterval();
        resetData();
    }

    /**
	 * Get the current data list.
	 *
	 * @return Current data list
	 */
    public final List<D> getCurrentData() {
        return currentData;
    }

    /**
	 * Get the current data for a particular communication channel.
	 * 
	 * @param channel The communication channel
	 * @return The current data
	 */
    public final D getCurrentData(Integer channel) {
        return currentData.get(channel);
    }

    /**
	 * Get the current data for a particular communication channel.
	 * 
	 * @param channel The communication channel
	 * @return The current data
	 */
    public final D getCurrentData(Channel channel) {
        return currentData.get(channel.getChannelNum());
    }

    /**
	 * Set the current data.
	 * <p>
	 * This method may be overridden by implementing methods in order to process
	 * the hardware data prior to saving it. However, the overridden method should
	 * always call the super() method in order to ensure that the hardware data
	 * is stored.
	 *
	 * @param data The current data
	 */
    protected synchronized void setCurrentData(List<D> data) {
        logger.debug("Setting current data [hardwareAddr=" + getHardwareAddr() + "]");
        List<D> oldDataList = getCurrentData();
        this.currentData = data;
        firePropertyChange(PROPERTYNAME_CURRENTDATA, oldDataList, this.currentData);
    }

    /**
	 * Set the current data.
	 * <p>
	 * This method may be overridden by implementing methods in order to process
	 * the hardware data prior to saving it. However, the overridden method should
	 * always call the super() method in order to ensure that the hardware data
	 * is stored.
	 * 
	 * @param data
	 */
    protected final synchronized void setCurrentData(D data) {
        getCurrentData().add(data.getChannel(), data);
    }

    /**
	 * Get the current data as a string.
	 * 
	 * @return The current data as a string
	 */
    public final String getCurrentDataAsString() {
        StringBuffer buf = new StringBuffer("");
        for (HardwareData data : currentData) buf.append("[CH-" + data.getChannel() + "] " + data.getDataString());
        return buf.toString();
    }

    /**
	 * Get the current data as a string.
	 * 
	 * @param channel The channel to retrieve
	 * @return The current data as a string
	 */
    public final String getCurrentDataAsString(Integer channel) {
        return currentData.get(channel).getDataString();
    }

    /**
	 * Reset the sensor data.
	 * <p>
	 * This may be overridden by implementing classes but a call to the super()
	 * method should always be called as well to ensure that the current hardware
	 * data list is also reset.
	 */
    protected synchronized void resetData() {
        currentData = new ArrayList<D>(this.getNumChannels().intValue());
    }

    /**
	 * This method simply reads from all available communication channels and
	 * returns a hardware data list with the corresponding hardware data.
	 * 
	 * @return A hardware data list of corresponding hardware data
	 * @throws HardwareException
	 */
    public List<D> readFromSensor() throws HardwareException {
        List<D> hardwareData = new ArrayList<D>(getNumChannels());
        for (Integer channel = 0; channel < getNumChannels(); channel++) {
            try {
                hardwareData.add(channel, readFromSensor(channel));
            } catch (HardwareException he) {
                logger.error("Error while reading from sensor [channel=" + channel + "]: " + he.getMessage());
                throw new HardwareException("Error while capturing sensor data");
            }
        }
        return hardwareData;
    }

    /**
	 * Read from the sensor.
	 *  
	 * @param channel The channel to read from
	 * @return
	 * @throws HardwareException
	 */
    public abstract D readFromSensor(int channel) throws HardwareException;

    /**
	 * Capture sensor data from the hardware driver and store it internally in the sensor.
	 * Calling this method both reads data from the sensor and stores it which may notify
	 * sensor listeners of a change in the internal hardware data.  
	 * 
	 * @return A hardware data list containing data for each of the sensor's I/O channels
	 * @throws HardwareException
	 */
    public final List<D> capture() throws HardwareException {
        List<D> dataList = readFromSensor();
        setCurrentData(dataList);
        return dataList;
    }

    /**
	 * @return Returns the sensor's pollingInterval.
	 */
    public PollingIntervals getPollingInterval() {
        return this.pollingInterval;
    }

    /**
	 * @param pollingInterval The pollingInterval to set.
	 */
    public void setPollingInterval(PollingIntervals pollingInterval) {
        PollingIntervals oldValue = getPollingInterval();
        this.pollingInterval = pollingInterval;
        this.firePropertyChange(PROPERTYNAME_POLLINGINTERVAL, oldValue, this.pollingInterval);
        logger.info("Polling interval set: " + pollingInterval.getDescription());
    }

    /**
	 * @param pollingInterval The pollingInterval to set.
	 */
    private final void setPollingInterval(String pollingInterval) {
        PollingIntervals pi = PollingIntervals.valueOf(pollingInterval);
        setPollingInterval(pi);
    }

    /**
	 * Whether this is the first time the hardware has been polled.
	 *
	 * @return True if it's the first time the hardware has been polled
	 */
    protected boolean isFirstPoll() {
        return firstPoll;
    }
}
