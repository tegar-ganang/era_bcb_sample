package jhomenet.commons.responsive.exec;

import java.util.*;
import org.apache.log4j.Logger;
import jhomenet.commons.JHomenetException;
import jhomenet.commons.data.ValueData;
import jhomenet.commons.hw.*;
import jhomenet.commons.hw.sensor.*;
import jhomenet.commons.hw.states.State;
import jhomenet.commons.hw.mngt.HardwareManager;
import jhomenet.commons.hw.mngt.NoSuchHardwareException;
import jhomenet.commons.responsive.ResponsiveException;
import jhomenet.commons.responsive.condition.*;
import jhomenet.commons.utils.ReflectionUtil;

/**
 * This class is used to convert between sensor responsive objects and string
 * representations of those objects. This class is used in conjunction with the
 * text based sensor responsive persistence layer implementation.
 * 
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class DefaultResponsiveStringConverter implements ResponsiveStringConverter {

    /**
	 * Define a logging mechanism.
	 */
    private static Logger logger = Logger.getLogger(DefaultResponsiveStringConverter.class.getName());

    /**
	 * Define the maximum expression string size. 
	 */
    private final int eSizeLimit = 1000;

    /**
	 * Reference to the hardware manager.
	 */
    private final HardwareManager hardwareManager;

    /**
	 * Constructor, however it's never instantiated.
	 * 
	 * @param hardwareManager
	 */
    public DefaultResponsiveStringConverter(HardwareManager hardwareManager) {
        super();
        if (hardwareManager == null) throw new IllegalArgumentException("Hardware manager cannot be null!");
        this.hardwareManager = hardwareManager;
    }

    /**
	 * @see jhomenet.commons.responsive.exec.ResponsiveConverter#fromExpression(jhomenet.commons.responsive.condition.Expression)
	 */
    public String fromExpression(Expression expression) throws ResponsiveConverterException {
        StringBuffer buffer = new StringBuffer();
        buffer.append(Identifiers.OPERATOR_CONTEXT_CLASS.getIdentifier());
        buffer.append(Delimeters.KEYVALUE.getDelimeter());
        buffer.append(expression.getOperatorContext().getClass().getName());
        buffer.append(Delimeters.KEYVALUE_PAIR.getDelimeter());
        for (ExpressionComponent component : expression.getExpressionComponentList(null)) {
            if (component instanceof Condition) {
                buffer.append(this.fromCondition((Condition) component));
            } else {
                try {
                    buffer.append(component.evaluate().getResultAsString());
                } catch (ResponsiveException re) {
                }
            }
        }
        return buffer.toString();
    }

    /**
	 * @see jhomenet.commons.responsive.exec.ResponsiveConverter#toExpression(java.lang.String)
	 */
    public Expression toExpression(String eString) throws ResponsiveConverterException {
        logger.debug("Compiling expression string: " + eString);
        ExecutorOperatorContext operatorContext = loadOperatorContextClass(eString);
        int firstIndex = eString.indexOf(Delimeters.KEYVALUE_PAIR.getDelimeter());
        eString = eString.substring(firstIndex + 1);
        Expression expression = new Expression(operatorContext);
        for (int position = 0; position < eString.length() && position != -1; ) {
            String s = Character.toString(eString.charAt(position));
            ExpressionComponent eComponent = operatorContext.getOperator(s);
            if (eComponent != null) {
                expression.addExpressionComponent(eComponent);
                position++;
            } else if (Identifiers.CONDITION_START.equals(s)) {
                int nextPosition = eString.indexOf(Identifiers.CONDITION_STOP.toString(), position);
                String conditionString = eString.substring(++position, nextPosition);
                expression.addExpressionComponent(toCondition(conditionString));
                position = nextPosition + 1;
            } else if (Identifiers.CONDITION_STOP.equals(s)) {
                position++;
            } else if (s.equalsIgnoreCase(" ")) {
                position++;
            } else {
                logger.error("Unknown expression string element: " + s);
                throw new ResponsiveConverterException("Unknown expression string element: " + s);
            }
            if (position > eSizeLimit) {
                throw new ResponsiveConverterException("Expression size exceeds limit!");
            }
        }
        return expression;
    }

    /**
	 * 
	 * @param eString
	 * @return
	 * @throws ResponsiveConverterException
	 */
    private String getExpressionDescription(String eString) throws ResponsiveConverterException {
        if (eString.startsWith(Identifiers.EXPRESSION_DESC.getIdentifier())) {
            String ocKeyPair = (eString.split(Delimeters.KEYVALUE_PAIR.getDelimeter()))[0];
            return ocKeyPair.split(Delimeters.KEYVALUE.getDelimeter())[1];
        }
        return "";
    }

    /**
	 * Load an operator context given a string representation of an expression. This class
	 * parses the expression string and attempts to identify the operator context class name. 
	 * 
	 * @param eString A string representation of an <code>Expression</code> object
	 * @return
	 */
    private ExecutorOperatorContext loadOperatorContextClass(String eString) throws ResponsiveConverterException {
        if (eString.startsWith(Identifiers.OPERATOR_CONTEXT_CLASS.getIdentifier())) {
            String ocKeyPair = (eString.split(Delimeters.KEYVALUE_PAIR.getDelimeter()))[0];
            String value = ocKeyPair.split(Delimeters.KEYVALUE.getDelimeter())[1];
            if (value != null && value.length() > 0) {
                try {
                    return ReflectionUtil.createObject(value);
                } catch (JHomenetException jhe) {
                    throw new ResponsiveConverterException(jhe);
                }
            }
        }
        throw new ResponsiveConverterException("Unable to locate operator context classname within espression string!");
    }

    /**
	 * Create a condition from a condition string.
	 *
	 * @see jhomenet.commons.responsive.exec.ResponsiveConverter#toCondition(java.lang.Object)
	 */
    public final Condition toCondition(String cString) throws ResponsiveConverterException {
        if (cString.startsWith(Identifiers.CONDITION.getIdentifier())) {
            cString = cString.substring(Identifiers.CONDITION.length());
            logger.debug("Compiling condition string: " + cString);
            Map<String, String> conditionElements = buildConditionMap(cString);
            String conditionName = conditionElements.get(Identifiers.NAME.getIdentifier());
            String hardwareAddr = conditionElements.get(Identifiers.HW_ADDR_REF.getIdentifier());
            String ioChannel = conditionElements.get(Identifiers.IO_CHANNEL.getIdentifier());
            String cType = conditionElements.get(Identifiers.TYPE.getIdentifier());
            RegisteredHardware hw = null;
            try {
                hw = hardwareManager.getRegisteredHardware(hardwareAddr);
            } catch (NoSuchHardwareException nshe) {
                throw new ResponsiveConverterException(nshe);
            }
            if (cType == null) {
                throw new ResponsiveConverterException("Condition type must be set!");
            }
            if (Inputs.STATE_CONDITION.equals(cType)) {
                logger.debug("Parsing state condition...");
                if (!(hw instanceof StateSensor)) {
                    throw new ClassCastException("Condition and sensor types don't match!");
                }
                String testState = conditionElements.get(Identifiers.TEST_STATE.getIdentifier());
                StateCondition condition = null;
                if (Inputs.ON.equals(testState)) {
                    if (conditionName != null) condition = new StateCondition(conditionName, (StateSensor) hw, State.ONSTATE); else condition = new StateCondition(conditionName, (StateSensor) hw, State.ONSTATE);
                } else if (Inputs.OFF.equals(testState)) {
                    if (conditionName != null) condition = new StateCondition(conditionName, (StateSensor) hw, State.OFFSTATE); else condition = new StateCondition(conditionName, (StateSensor) hw, State.OFFSTATE);
                } else {
                    throw new ResponsiveConverterException("Unknown test state");
                }
                return condition;
            } else if (Inputs.VALUE_CONDITION.equals(cType)) {
                logger.debug("Parsing value condition...");
                if (!(hw instanceof ValueSensor)) throw new ResponsiveConverterException("Condition and sensor types don't match!");
                String testOperator = conditionElements.get(Identifiers.TEST_OPERATOR.getIdentifier());
                String testValue = conditionElements.get(Identifiers.TEST_VALUE.getIdentifier());
                AbstractCondition condition = null;
                if (ValueConditionOperator.GREATER_THAN.equals(testOperator)) {
                    condition = new ValueCondition(conditionName, (ValueSensor) hw, ValueConditionOperator.GREATER_THAN, ValueData.parseValueData(testValue));
                } else if (ValueConditionOperator.LESS_THAN.equals(testOperator)) {
                    condition = new ValueCondition(conditionName, (ValueSensor) hw, ValueConditionOperator.LESS_THAN, ValueData.parseValueData(testValue));
                } else {
                    throw new ResponsiveConverterException("Unknown test operator: " + testOperator);
                }
                return condition;
            } else if (Inputs.BOOLEAN_CONDITION.equals(cType)) {
                logger.debug("Parsing boolean condition...");
                String testValue = conditionElements.get(Identifiers.TEST_STATE.getIdentifier());
                BooleanCondition bCondition = new BooleanCondition(conditionName, Boolean.valueOf(testValue));
                return bCondition;
            }
            return null;
        } else {
            throw new ResponsiveConverterException("Invalid condition identifier");
        }
    }

    /**
	 * Convert a condition into a string representation.
	 */
    public final String fromCondition(Condition condition) throws ResponsiveConverterException {
        if (condition instanceof ValueCondition) {
            return fromValueCondition((ValueCondition) condition);
        } else if (condition instanceof StateCondition) {
            return fromStateCondition((StateCondition) condition);
        } else if (condition instanceof BooleanCondition) {
            return fromBooleanCondition((BooleanCondition) condition);
        } else {
            throw new ResponsiveConverterException("Unknown condition type...can't convert");
        }
    }

    /**
	 * 
	 * @param booleanCondition
	 * @return
	 * @throws ResponsiveConverterException
	 */
    private String fromBooleanCondition(BooleanCondition booleanCondition) throws ResponsiveConverterException {
        StringBuffer conditionStr = new StringBuffer();
        conditionStr.append(Identifiers.CONDITION_START.getIdentifier());
        conditionStr.append(Identifiers.CONDITION.getIdentifier());
        conditionStr.append(Identifiers.NAME.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(booleanCondition.getConditionName());
        conditionStr.append(Delimeters.KEYVALUE_PAIR.getDelimeter());
        conditionStr.append(Identifiers.TYPE.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(Inputs.BOOLEAN_CONDITION.asString());
        conditionStr.append(Delimeters.KEYVALUE_PAIR.getDelimeter());
        conditionStr.append(Identifiers.TEST_STATE.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(booleanCondition.getDesiredState().toString());
        conditionStr.append(Identifiers.CONDITION_STOP.getIdentifier());
        return conditionStr.toString();
    }

    /**
	 * Convert a state condition into a string representation.
	 * 
	 * @param stateCondition
	 * @return
	 * @throws ResponsiveConverterException
	 */
    private String fromStateCondition(StateCondition stateCondition) throws ResponsiveConverterException {
        StringBuffer conditionStr = new StringBuffer();
        conditionStr.append(Identifiers.CONDITION_START.getIdentifier());
        conditionStr.append(Identifiers.CONDITION.getIdentifier());
        conditionStr.append(Identifiers.NAME.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(stateCondition.getConditionName());
        conditionStr.append(Delimeters.KEYVALUE_PAIR.getDelimeter());
        conditionStr.append(Identifiers.HW_ADDR_REF.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(stateCondition.getHardwareAddr());
        conditionStr.append(Delimeters.KEYVALUE_PAIR.getDelimeter());
        conditionStr.append(Identifiers.IO_CHANNEL.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(stateCondition.getChannel().toString());
        conditionStr.append(Delimeters.KEYVALUE_PAIR.getDelimeter());
        conditionStr.append(Identifiers.TYPE.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(Inputs.STATE_CONDITION.asString());
        conditionStr.append(Delimeters.KEYVALUE_PAIR.getDelimeter());
        conditionStr.append(Identifiers.TEST_STATE.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(stateCondition.getTestState().toString());
        conditionStr.append(Identifiers.CONDITION_STOP.getIdentifier());
        return conditionStr.toString();
    }

    /**
	 * Converter a value condition into a string representation.
	 * 
	 * @param valueCondition
	 * @return
	 */
    private String fromValueCondition(ValueCondition valueCondition) {
        StringBuffer conditionStr = new StringBuffer();
        conditionStr.append(Identifiers.CONDITION_START.getIdentifier());
        conditionStr.append(Identifiers.CONDITION.getIdentifier());
        conditionStr.append(Identifiers.NAME.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(valueCondition.getConditionName());
        conditionStr.append(Delimeters.KEYVALUE_PAIR.getDelimeter());
        conditionStr.append(Identifiers.HW_ADDR_REF.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(valueCondition.getHardwareAddr());
        conditionStr.append(Delimeters.KEYVALUE_PAIR.getDelimeter());
        conditionStr.append(Identifiers.IO_CHANNEL.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(valueCondition.getChannel().toString());
        conditionStr.append(Delimeters.KEYVALUE_PAIR.getDelimeter());
        conditionStr.append(Identifiers.TYPE.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(Inputs.VALUE_CONDITION.asString());
        conditionStr.append(Delimeters.KEYVALUE_PAIR.getDelimeter());
        conditionStr.append(Identifiers.TEST_OPERATOR.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(valueCondition.getTestOperator().getOperatorStr());
        conditionStr.append(Delimeters.KEYVALUE_PAIR.getDelimeter());
        conditionStr.append(Identifiers.TEST_VALUE.getIdentifier());
        conditionStr.append(Delimeters.KEYVALUE.getDelimeter());
        conditionStr.append(valueCondition.getTestValue().toString());
        conditionStr.append(Identifiers.CONDITION_STOP.getIdentifier());
        return conditionStr.toString();
    }

    /**
	 * 
	 * @param keyValuePair
	 * @return
	 */
    private String getValue(String keyValuePair) {
        String pairs[] = keyValuePair.split(Delimeters.KEYVALUE.getDelimeter());
        return pairs[1].trim();
    }

    /**
	 * Build a map of condition components. It is assumed that the passed condition
	 * string is delimited by a semicolon (;).
	 *
	 * @param cString Condition string
	 * @return Map with descriptor/input pairs
	 */
    public static Map<String, String> buildConditionMap(String cString) throws ResponsiveConverterException {
        final Map<String, String> map = new HashMap<String, String>();
        StringTokenizer st = new StringTokenizer(cString, Delimeters.KEYVALUE_PAIR.getDelimeter());
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            String[] descriptorInputPair = s.split(Delimeters.KEYVALUE.getDelimeter());
            if (descriptorInputPair.length == 1) {
                map.put(descriptorInputPair[0], "");
            } else if (descriptorInputPair.length == 2) {
                map.put(descriptorInputPair[0], descriptorInputPair[1]);
            } else {
                throw new ResponsiveConverterException("Invalid descriptor pair format: " + s);
            }
        }
        return map;
    }
}
