package jhomenet.commons.responsive.condition;

import org.apache.log4j.Logger;
import jhomenet.commons.GeneralApplicationContext;
import jhomenet.commons.data.ValueData;
import jhomenet.commons.hw.HardwareException;
import jhomenet.commons.hw.RegisteredHardware;
import jhomenet.commons.hw.mngt.NoSuchHardwareException;
import jhomenet.commons.hw.sensor.*;
import jhomenet.commons.hw.states.State;
import jhomenet.commons.responsive.ResponsiveException;

/**
 * An implementation of the sensor responsive system (SRS) condition
 * interface. The two primary inputs to the state condition are the
 * test state and a state sensor. When evaluated, the state condition
 * checks whether the state sensor matches the test state and returns
 * the result.
 * <p>
 * As an example, suppose the test state sensor is an wind direction sensor
 * and the test state is West (or W). The condition first retrieves the
 * current wind direction state from the sensor. The condition then compares
 * this state against the test state and returns Boolean.TRUE if the actual 
 * state and the test state match, otherwise it returns Boolean.FALSE.
 * 
 * @see jhomenet.commons.responsive.condition.Condition
 * @see jhomenet.commons.responsive.condition.AbstractCondition
 * @author David Irwin (jhomenet at gmail dot com)
 */
public class StateCondition extends SensorCondition<StateSensor> {

    /**
	 * Define a logging mechanism.
	 */
    private static Logger logger = Logger.getLogger(StateCondition.class.getName());

    /**
	 * Reference to the hardware state.
	 */
    private State testState;

    /**
	 * Constructor.
	 * 
	 * @param conditionName Condition name
	 * @param stateSensor Reference to the state sensor
	 * @param channel The hardware I/O channel
	 * @param testState The test state
	 */
    public StateCondition(String conditionName, StateSensor stateSensor, Integer channel, State testState) {
        super(conditionName, stateSensor, channel);
        this.testState = testState;
    }

    /**
	 * Constructor.
	 * 
	 * @param conditionName Condition name
	 * @param stateSensor Reference to the state sensor
	 * @param testState The test state
	 */
    public StateCondition(String conditionName, StateSensor stateSensor, State testState) {
        super(conditionName, stateSensor);
        this.testState = testState;
    }

    /**
	 * 
	 */
    private StateCondition() {
        this(null, null, null);
    }

    /**
	 * @see jhomenet.commons.responsive.condition.AbstractCondition#injectAppContext(jhomenet.commons.GeneralApplicationContext)
	 */
    @Override
    public void injectAppContext(GeneralApplicationContext serverContext) {
        try {
            RegisteredHardware hw = serverContext.getHardwareManager().getRegisteredHardware(getHardwareAddr());
            if (hw instanceof StateSensor) this.setSensor((StateSensor) hw);
        } catch (NoSuchHardwareException nshe) {
        }
    }

    /**
	 * Set the state condition's test state.
	 *
	 * @param testState
	 */
    public void setTestState(State testState) {
        this.testState = testState;
    }

    /**
	 * Get the state condition's test state.
	 * 
	 * @return
	 */
    public State getTestState() {
        return this.testState;
    }

    /**
	 * @see jhomenet.commons.responsive.condition.Condition#evaluate()
	 */
    public ConditionResult evaluate() throws ResponsiveException {
        try {
            State currentState = getSensor().readFromSensor(getChannel()).getDataObject();
            if (currentState.equals(testState)) {
                return new StateConditionResult(Boolean.TRUE, currentState);
            } else {
                return new StateConditionResult(Boolean.FALSE, currentState);
            }
        } catch (HardwareException he) {
            logger.error("Hardware exception while evaluating state condition: " + he.getMessage());
            throw new ResponsiveException(he);
        }
    }

    /**
	 * @see java.lang.Object#hashCode()
	 */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((testState == null) ? 0 : testState.hashCode());
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
        final StateCondition other = (StateCondition) obj;
        if (testState == null) {
            if (other.testState != null) return false;
        } else if (!testState.equals(other.testState)) return false;
        return true;
    }

    private class StateConditionResult implements ConditionResult {

        private final Boolean result;

        private final State state;

        private StateConditionResult(Boolean result, State state) {
            super();
            this.result = result;
            this.state = state;
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
            buf.append("State condition result");
            buf.append("  Hardware desc: " + getSensor().getHardwareSetupDescription());
            buf.append("  Hardware address: " + getSensor().getHardwareAddr());
            buf.append("  State: " + state.toString());
            return buf.toString();
        }
    }
}
