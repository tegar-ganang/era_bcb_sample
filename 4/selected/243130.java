package jhomenet.commons.responsive.condition;

import org.apache.log4j.Logger;
import jhomenet.commons.GeneralApplicationContext;
import jhomenet.commons.data.ValueData;
import jhomenet.commons.hw.RegisteredHardware;
import jhomenet.commons.hw.HardwareException;
import jhomenet.commons.hw.mngt.NoSuchHardwareException;
import jhomenet.commons.hw.sensor.ValueSensor;
import jhomenet.commons.responsive.ResponsiveException;

/**
 * An implementation of the sensor responsive system (SRS) condition
 * interface. The three primary inputs to the value condition are the
 * test value and test operator along with the value sensor. When
 * evaluated, the value condition compares the value from the value 
 * sensor against the test value and test operator and returns the 
 * result of this comparison.
 * <p>
 * As an example, suppose the test value sensor is a temperature sensor
 * and the test value is 70F and the test operator is Greater Than. The
 * condition first retrieves the current temperature from the temperature
 * sensor. The condition then compares this value against the test value
 * using the test operator and returns Boolean.TRUE if the condition is
 * true, otherwise it returns Boolean.FALSE.
 * 
 * @see jhomenet.commons.responsive.condition.Condition
 * @see jhomenet.commons.responsive.condition.AbstractCondition
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class ValueCondition extends SensorCondition<ValueSensor> {

    /**
	 * Define a logging mechanism.
	 */
    private static Logger logger = Logger.getLogger(ValueCondition.class.getName());

    /**
	 * The test operator.
	 */
    private ValueConditionOperator testOperator;

    /**
	 * The test value.
	 */
    private ValueData testValue;

    /**
	 * Constructor.
	 * 
	 * @param conditionName Condition name
	 * @param valueSensor Reference to the value sensor
	 * @param ioChannel The I/O channel
	 * @param testOperator The test operator
	 * @param testValue The test value
	 */
    public ValueCondition(String conditionName, ValueSensor valueSensor, Integer ioChannel, ValueConditionOperator testOperator, ValueData testValue) {
        super(conditionName, valueSensor, ioChannel);
        this.setTestOperator(testOperator);
        this.setTestValue(testValue);
    }

    /**
	 * Constructor.
	 * 
	 * @param conditionName Condition name
	 * @param valueSensor Reference to the value sensor
	 * @param testOperator The test operator
	 * @param testValue The test value
	 */
    public ValueCondition(String conditionName, ValueSensor valueSensor, ValueConditionOperator testOperator, ValueData testValue) {
        super(conditionName, valueSensor);
        this.setTestOperator(testOperator);
        this.setTestValue(testValue);
    }

    /**
	 * 
	 */
    private ValueCondition() {
        this(null, null, null, null);
    }

    /**
	 * @see jhomenet.commons.responsive.condition.AbstractCondition#injectAppContext(jhomenet.commons.GeneralApplicationContext)
	 */
    @Override
    public void injectAppContext(GeneralApplicationContext serverContext) {
        try {
            RegisteredHardware hw = serverContext.getHardwareManager().getRegisteredHardware(getHardwareAddr());
            if (hw instanceof ValueSensor) this.setSensor((ValueSensor) hw);
        } catch (NoSuchHardwareException nshe) {
        }
    }

    /**
	 * Set the test operator.
	 *
	 * @param testOperator
	 */
    public void setTestOperator(ValueConditionOperator testOperator) {
        this.testOperator = testOperator;
    }

    /**
	 * 
	 * @return
	 */
    public ValueConditionOperator getTestOperator() {
        return this.testOperator;
    }

    /**
	 * Set the test value.
	 *
	 * @param testValue
	 */
    public void setTestValue(ValueData testValue) {
        this.testValue = testValue;
    }

    /**
	 * 
	 * @return
	 */
    public ValueData getTestValue() {
        return this.testValue;
    }

    /**
	 * @see jhomenet.commons.responsive.condition.Condition#evaluate()
	 */
    public ConditionResult evaluate() throws ResponsiveException {
        ValueData data;
        try {
            data = getSensor().readFromSensor(getChannel()).getDataObject();
        } catch (HardwareException he) {
            logger.error("Error while reading value from sensor: " + he.getMessage());
            throw new ResponsiveException("Error while evaluating condition: cannot read from sensor: ", he);
        }
        try {
            switch(testOperator) {
                case GREATER_THAN:
                    if (data.compareTo(testValue) > 0) return new ValueConditionResult(Boolean.TRUE, data);
                    break;
                case LESS_THAN:
                    if (data.compareTo(testValue) < 0) return new ValueConditionResult(Boolean.TRUE, data);
                    break;
                default:
                    logger.debug("Unknown operator type: " + testOperator);
            }
        } catch (ClassCastException cce) {
            logger.error("Error while evaluating condition: incompatible units");
            throw new ResponsiveException(cce);
        }
        return new ValueConditionResult(Boolean.FALSE, data);
    }

    /**
	 * @see java.lang.Object#hashCode()
	 */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((testOperator == null) ? 0 : testOperator.hashCode());
        result = prime * result + ((testValue == null) ? 0 : testValue.hashCode());
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
        final ValueCondition other = (ValueCondition) obj;
        if (testOperator == null) {
            if (other.testOperator != null) return false;
        } else if (!testOperator.equals(other.testOperator)) return false;
        if (testValue == null) {
            if (other.testValue != null) return false;
        } else if (!testValue.equals(other.testValue)) return false;
        return true;
    }

    private class ValueConditionResult implements ConditionResult {

        private final Boolean result;

        private final ValueData data;

        private ValueConditionResult(Boolean result, ValueData data) {
            super();
            this.result = result;
            this.data = data;
        }

        /**
		 * @see jhomenet.commons.responsive.condition.ConditionResult#getResult()
		 */
        @Override
        public Boolean getResult() {
            return this.result;
        }

        /**
		 * @see jhomenet.commons.responsive.condition.ExpressionResult#getResultAsString()
		 */
        @Override
        public String getResultAsString() {
            return String.valueOf(result);
        }

        /**
		 * @see jhomenet.commons.responsive.condition.ConditionResult#getResultDetails()
		 */
        @Override
        public String getResultDetails() {
            StringBuffer buf = new StringBuffer();
            buf.append("Value condition result");
            buf.append("  Hardware desc: " + getSensor().getHardwareSetupDescription());
            buf.append("  Hardware address: " + getSensor().getHardwareAddr());
            buf.append("  Data value: " + data.getValue() + " " + data.getUnit().toString());
            buf.append("  Test value: " + testValue.getValue() + " " + testValue.getUnit().toString());
            buf.append("  Test operator: " + testOperator.getOperatorStr());
            return buf.toString();
        }
    }
}
