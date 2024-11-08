package jhomenet.commons.hw.sensor;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import org.apache.log4j.Logger;
import javax.measure.unit.Unit;
import jhomenet.commons.hw.data.HardwareValueData;

/**
 * Class for value based sensors.
 *  
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public abstract class ValueSensor extends Sensor<HardwareValueData> {

    /**
     * Define a logging mechanism.
     */
    private static Logger logger = Logger.getLogger(ValueSensor.class.getName());

    /**
     * Define an unknown value
     */
    public static final float INVALID_DATA = -9999999999999F;

    /**
     * Hardware data variables for current, maximum, and minimum values.
     */
    protected List<HardwareValueData> maxValueList;

    protected List<HardwareValueData> minValueList;

    /**
     * Property names.
     */
    public static final String PROPERTYNAME_MAXVALUE = "maxValueList";

    public static final String PROPERTYNAME_MINVALUE = "minValueList";

    /**
     * Flag indicating whether the underlying hardware supports minimum/maximum values.
     * Some hardware types intrinsically don't support a minimum or maximum value (such
     * as a wind direction sensor). 
     */
    private static final boolean DEFAULT_SUPPORTMINMAXVALUES = false;

    private final boolean supportMinMaxValues;

    /**
     * Twenty-four hour minimum and maximum values.
     */
    protected List<HardwareValueData> maxDailyValue, minDailyValue;

    /**
     * Minimum/maximum daily value property names.
     */
    public static final String PROPERTYNAME_MAXDAILYVALUE = "maxDailyValue";

    public static final String PROPERTYNAME_MINDAILYVALUE = "minDailyValue";

    /**
     * Keep a list of daily minimum and maximum data objects.
     */
    private final List<List<HardwareValueData>> maxValuesDailyList = new LinkedList<List<HardwareValueData>>();

    private final List<List<HardwareValueData>> minValuesDailyList = new LinkedList<List<HardwareValueData>>();

    /**
     * Define the number of minimum and maximum values to log.
     */
    private static final int defaultDaysToLog = 7;

    private int daysToLog = defaultDaysToLog;

    /**
     * Reference to the desired hardware data unit. The data unit is applied to hardware
     * data in all the supporting input channels.
     */
    private static final Unit DEFAULT_UNIT = Unit.ONE;

    private Unit preferredDataUnit = DEFAULT_UNIT;

    /**
     * List of available units.
     */
    private final Set<Unit> availableUnits = new LinkedHashSet<Unit>();

    /**
     * Preferred data unit property name.
     */
    public static final String PROPERTYNAME_PREFERREDDATAUNIT = "preferredDataUnit";

    /**
     * Constructor with minimum/maximum data support control and hardware data unit support.
     * 
     * @param hardwareAddr Hardware address of the hardware
     * @param hardwareSetupDescription
     * @param appHardwareDescription 
     * @param supportsMinMaxValues Set to <code>true</code> if the hardware
     *  supports minimum/maximum values
     * @param numChannels
     * @param dataUnit The desired data unit
     */
    public ValueSensor(String hardwareAddr, String hardwareSetupDescription, String hardwareAppDescription, Integer numChannels, boolean supportsMinMaxValues, Unit dataUnit) {
        super(hardwareAddr, hardwareSetupDescription, hardwareAppDescription, numChannels);
        this.supportMinMaxValues = supportsMinMaxValues;
        this.preferredDataUnit = dataUnit;
        this.initAvailableUnits(availableUnits);
        resetData();
    }

    /**
     * Constructor.
     * 
     * @param hardwareAddr
     * @param hardwareSetupDescription
     * @param appHardwareDescription
     * @param supportsMinMaxValues
     * @param numChannels
     */
    public ValueSensor(String hardwareAddr, String hardwareSetupDescription, String hardwareAppDescription, Integer numChannels, boolean supportsMinMaxValues) {
        super(hardwareAddr, hardwareSetupDescription, hardwareAppDescription, numChannels);
        this.supportMinMaxValues = supportsMinMaxValues;
        this.preferredDataUnit = DEFAULT_UNIT;
        this.initAvailableUnits(availableUnits);
        resetData();
    }

    /**
     * 
     * @param hardwareAppDescription
     * @param numChannels
     * @param supportsMinMaxValues
     * @param dataUnit
     */
    public ValueSensor(String hardwareAppDescription, Integer numChannels, boolean supportsMinMaxValues, Unit dataUnit) {
        super(hardwareAppDescription, numChannels);
        this.supportMinMaxValues = supportsMinMaxValues;
        this.preferredDataUnit = dataUnit;
        this.initAvailableUnits(availableUnits);
        resetData();
    }

    /**
     * 
     * @param hardwareAppDescription
     * @param numChannels
     * @param supportsMinMaxValues
     */
    public ValueSensor(String hardwareAppDescription, Integer numChannels, boolean supportsMinMaxValues) {
        super(hardwareAppDescription, numChannels);
        this.supportMinMaxValues = supportsMinMaxValues;
        this.preferredDataUnit = DEFAULT_UNIT;
        this.initAvailableUnits(availableUnits);
        resetData();
    }

    /**
     * Copying constructor.
     * 
     * @param sensor
     */
    public ValueSensor(ValueSensor sensor) {
        super(sensor);
        this.supportMinMaxValues = sensor.doesSupportMinMaxValues();
        this.preferredDataUnit = sensor.getPreferredDataUnit();
        this.initAvailableUnits(this.availableUnits);
        resetData();
    }

    /**
     * Get whether the sensor supports minimum/maximum values.
     * 
     * @return Boolean.TRUE if the value sensor supports minimum/maximum values
     */
    public boolean doesSupportMinMaxValues() {
        return this.supportMinMaxValues;
    }

    /**
     * 
     * @param availableUnits
     */
    protected abstract void initAvailableUnits(Set<Unit> availableUnits);

    /**
     * @return A list of available units
     */
    public final Set<Unit> getAvailableUnits() {
        return availableUnits;
    }

    /**
     * Set the current data value.
     * 
     * @see jhomenet.commons.hw.sensor.Sensor#setCurrentData(jhomenet.commons.hw.data.AbstractHardwareData)
     */
    @Override
    protected final synchronized void setCurrentData(List<HardwareValueData> dataList) {
        for (HardwareValueData dataValue : dataList) {
            if (!getPreferredDataUnit().equals(dataValue.getDataObject().getUnit())) {
                logger.debug("Converting hardware data [CH-" + dataValue.getChannel() + ", " + dataValue.getDataString() + ", hardwareAddr=" + getHardwareAddr() + "]");
                dataValue = dataValue.convertDataUnit(getPreferredDataUnit());
                logger.debug("Hardware data converted [CH-" + dataValue.getChannel() + ", " + dataValue.getDataString() + ", hardwareAddr=" + getHardwareAddr() + "]");
            }
        }
        super.setCurrentData(dataList);
        if (doesSupportMinMaxValues()) {
            setMaxValueList(dataList);
            setMinValueList(dataList);
        }
        if (firstPoll) {
        }
        firstPoll = false;
    }

    /**
     * Get the minimum daily value.
     * 
     * @return The minimum daily value
     */
    public List<HardwareValueData> getMinDailyValue() {
        return minDailyValue;
    }

    /**
     * Set the minimum daily value.
     *
     * @param dataList The minimum daily value
     */
    protected synchronized void setMinDailyValue(List<HardwareValueData> dataList) {
        logger.debug("Updating minimum daily value");
        for (HardwareValueData dataValue : dataList) {
            if (!getPreferredDataUnit().equals(dataValue.getDataObject().getUnit())) dataValue = dataValue.convertDataUnit(getPreferredDataUnit());
        }
        List<HardwareValueData> oldValue = getMaxDailyValue();
        this.minDailyValue = dataList;
        firePropertyChange(PROPERTYNAME_MINDAILYVALUE, oldValue, dataList);
    }

    /**
     * Get the maximum daily value.
     * 
     * @return The maximum daily value
     */
    public final List<HardwareValueData> getMaxDailyValue() {
        return maxDailyValue;
    }

    /**
     * Set the maximum daily value.
     *
     * @param dataList The maximum daily value
     */
    protected final synchronized void setMaxDailyValue(List<HardwareValueData> dataList) {
        logger.debug("Updating maximum daily value");
        for (HardwareValueData dataValue : dataList) {
            if (!getPreferredDataUnit().equals(dataValue.getDataObject().getUnit())) dataValue = dataValue.convertDataUnit(getPreferredDataUnit());
        }
        List<HardwareValueData> oldValue = getMaxDailyValue();
        this.maxDailyValue = dataList;
        firePropertyChange(PROPERTYNAME_MAXDAILYVALUE, oldValue, dataList);
    }

    /**
     * Get the maximum data object.
     * 
     * @return The maximum value
     */
    public final List<HardwareValueData> getMaxValueList() {
        return maxValueList;
    }

    /**
     * 
     * @param channel
     * @return
     */
    public final HardwareValueData getMaxValue(int channel) {
        if (maxValueList.size() == 0) return null; else return maxValueList.get(channel);
    }

    /**
     * Set the maximum data object.
     * 
     * @param valueDataList
     */
    protected final synchronized void setMaxValueList(List<HardwareValueData> valueDataList) {
        boolean maxValueUpdated = false;
        for (HardwareValueData value : valueDataList) {
            HardwareValueData oldValue = getMaxValue(value.getChannel());
            if (oldValue == null) {
                value = value.convertDataUnit(this.getPreferredDataUnit());
                maxValueList.add(value);
                maxValueUpdated = true;
                logger.debug("Updating maximum value: [CH-" + value.getChannel() + " " + value.getDataString() + "]");
            } else if (oldValue != null && value.compareTo(oldValue) > 0) {
                value = value.convertDataUnit(this.getPreferredDataUnit());
                maxValueList.add(value);
                maxValueUpdated = true;
                logger.debug("Updating maximum value: [CH-" + value.getChannel() + " " + value.getDataString() + "]");
            }
        }
        if (maxValueUpdated) {
            firePropertyChange(PROPERTYNAME_MAXVALUE, getMaxValueList(), valueDataList);
        }
    }

    /**
     * Get the minimum value.
     * 
     * @return The minimum value
     */
    public List<HardwareValueData> getMinValueList() {
        return minValueList;
    }

    /**
     * 
     * @param channel
     * @return
     */
    public final HardwareValueData getMinValue(int channel) {
        if (minValueList.size() == 0) return null; else return minValueList.get(channel);
    }

    /**
     * Set the minimum value.
     * 
     * @param valueDataList
     */
    protected synchronized void setMinValueList(List<HardwareValueData> valueDataList) {
        boolean minValueUpdated = false;
        for (HardwareValueData value : valueDataList) {
            HardwareValueData oldValue = getMinValue(value.getChannel());
            if (oldValue == null) {
                value = value.convertDataUnit(this.getPreferredDataUnit());
                minValueList.add(value);
                minValueUpdated = true;
                logger.debug("Updating minimum value: [CH-" + value.getChannel() + " " + value.getDataString() + "]");
            } else if (oldValue != null && value.compareTo(oldValue) < 0) {
                value = value.convertDataUnit(this.getPreferredDataUnit());
                minValueList.add(value);
                minValueUpdated = true;
                logger.debug("Updating minimum value: [CH-" + value.getChannel() + " " + value.getDataString() + "]");
            }
        }
        if (minValueUpdated) {
            firePropertyChange(PROPERTYNAME_MINVALUE, getMaxValueList(), valueDataList);
        }
    }

    /**
     * Get the desired hardware data unit.
     * 
     * @return The data unit
     */
    public Unit getPreferredDataUnit() {
        return preferredDataUnit;
    }

    /**
     * Set the desired hardware data unit.
     * 
     * @param preferredDataUnit
     */
    public synchronized void setPreferredDataUnit(Unit preferredDataUnit) {
        Unit currentPreferredDataUnit = getPreferredDataUnit();
        if (preferredDataUnit.isCompatible(currentPreferredDataUnit)) {
            logger.debug("Updating data unit to " + preferredDataUnit.toString());
            this.preferredDataUnit = preferredDataUnit;
            if (doesSupportMinMaxValues()) {
                List<HardwareValueData> currentList = new ArrayList<HardwareValueData>(this.getNumChannels().intValue());
                List<HardwareValueData> minList = new ArrayList<HardwareValueData>(this.getNumChannels().intValue());
                List<HardwareValueData> maxList = new ArrayList<HardwareValueData>(this.getNumChannels().intValue());
                for (HardwareValueData data : getCurrentData()) {
                    currentList.add(data.convertDataUnit(preferredDataUnit));
                    minList.add(data.convertDataUnit(preferredDataUnit));
                    maxList.add(data.convertDataUnit(preferredDataUnit));
                }
                this.setCurrentData(currentList);
                this.setMinValueList(minList);
                this.setMaxValueList(maxList);
            }
            firePropertyChange(PROPERTYNAME_PREFERREDDATAUNIT, currentPreferredDataUnit, this.preferredDataUnit);
        } else {
            logger.debug("Can't update preferred data unit: incompatible data units");
        }
    }

    /**
     * 
     * @param preferredDataUnit
     */
    private void setPreferredDataUnit(String preferredDataUnit) {
        setPreferredDataUnit(Unit.valueOf(preferredDataUnit));
    }

    /**
     * @see jhomenet.commons.hw.sensor.Sensor#resetData()
     */
    @Override
    protected final synchronized void resetData() {
        super.resetData();
        minValueList = new ArrayList<HardwareValueData>(this.getNumChannels().intValue());
        maxValueList = new ArrayList<HardwareValueData>(this.getNumChannels().intValue());
    }
}
