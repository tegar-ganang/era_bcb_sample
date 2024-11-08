package com.safi.asterisk.actionstep.impl;

import org.apache.commons.lang.StringUtils;
import org.asteriskjava.fastagi.AgiChannel;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import com.safi.asterisk.Call;
import com.safi.asterisk.actionstep.ActionstepPackage;
import com.safi.asterisk.actionstep.GetDigits;
import com.safi.asterisk.saflet.AsteriskSafletContext;
import com.safi.core.actionstep.ActionStepException;
import com.safi.core.actionstep.ActionStepFactory;
import com.safi.core.actionstep.DynamicValue;
import com.safi.core.actionstep.Output;
import com.safi.core.actionstep.OutputType;
import com.safi.core.actionstep.impl.ActionStepImpl;
import com.safi.core.call.CallConsumer1;
import com.safi.core.call.CallPackage;
import com.safi.core.call.SafiCall;
import com.safi.core.saflet.SafletContext;
import com.safi.core.saflet.SafletEnvironment;
import com.safi.db.Variable;
import com.safi.db.VariableScope;
import com.safi.db.util.VariableTranslator;

/**
 * <!-- begin-user-doc --> An implementation of the model object '
 * <em><b>Get Digits</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link com.safi.asterisk.actionstep.impl.GetDigitsImpl#getCall1 <em>Call1
 * </em>}</li>
 * <li>{@link com.safi.asterisk.actionstep.impl.GetDigitsImpl#getInputTimeout
 * <em>Input Timeout</em>}</li>
 * <li>
 * {@link com.safi.asterisk.actionstep.impl.GetDigitsImpl#isUseBufferedDigits
 * <em>Use Buffered Digits</em>}</li>
 * <li>{@link com.safi.asterisk.actionstep.impl.GetDigitsImpl#getEscapeDigits
 * <em>Escape Digits</em>}</li>
 * <li>{@link com.safi.asterisk.actionstep.impl.GetDigitsImpl#getVariableName
 * <em>Variable Name</em>}</li>
 * <li>{@link com.safi.asterisk.actionstep.impl.GetDigitsImpl#getMaxDigits <em>
 * Max Digits</em>}</li>
 * <li>{@link com.safi.asterisk.actionstep.impl.GetDigitsImpl#getAcceptedDigits
 * <em>Accepted Digits</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class GetDigitsImpl extends AsteriskActionStepImpl implements GetDigits {

    /**
	 * The cached value of the '{@link #getCall1() <em>Call1</em>}' reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getCall1()
	 * @generated
	 * @ordered
	 */
    protected SafiCall call1;

    /**
	 * The default value of the '{@link #getInputTimeout() <em>Input Timeout</em>}
	 * ' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getInputTimeout()
	 * @generated
	 * @ordered
	 */
    protected static final long INPUT_TIMEOUT_EDEFAULT = -1L;

    /**
	 * The cached value of the '{@link #getInputTimeout() <em>Input Timeout</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getInputTimeout()
	 * @generated
	 * @ordered
	 */
    protected long inputTimeout = INPUT_TIMEOUT_EDEFAULT;

    /**
	 * The default value of the '{@link #isUseBufferedDigits()
	 * <em>Use Buffered Digits</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #isUseBufferedDigits()
	 * @generated
	 * @ordered
	 */
    protected static final boolean USE_BUFFERED_DIGITS_EDEFAULT = false;

    /**
	 * The cached value of the '{@link #isUseBufferedDigits()
	 * <em>Use Buffered Digits</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #isUseBufferedDigits()
	 * @generated
	 * @ordered
	 */
    protected boolean useBufferedDigits = USE_BUFFERED_DIGITS_EDEFAULT;

    /**
	 * The default value of the '{@link #getEscapeDigits() <em>Escape Digits</em>}
	 * ' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getEscapeDigits()
	 * @generated
	 * @ordered
	 */
    protected static final String ESCAPE_DIGITS_EDEFAULT = "#";

    /**
	 * The cached value of the '{@link #getEscapeDigits() <em>Escape Digits</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getEscapeDigits()
	 * @generated
	 * @ordered
	 */
    protected String escapeDigits = ESCAPE_DIGITS_EDEFAULT;

    /**
	 * The cached value of the '{@link #getVariableName() <em>Variable Name</em>}'
	 * containment reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getVariableName()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue variableName;

    /**
	 * The default value of the '{@link #getMaxDigits() <em>Max Digits</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getMaxDigits()
	 * @generated
	 * @ordered
	 */
    protected static final int MAX_DIGITS_EDEFAULT = 1;

    /**
	 * The cached value of the '{@link #getMaxDigits() <em>Max Digits</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getMaxDigits()
	 * @generated
	 * @ordered
	 */
    protected int maxDigits = MAX_DIGITS_EDEFAULT;

    /**
	 * The default value of the '{@link #getAcceptedDigits()
	 * <em>Accepted Digits</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #getAcceptedDigits()
	 * @generated
	 * @ordered
	 */
    protected static final String ACCEPTED_DIGITS_EDEFAULT = "0123456789#";

    /**
	 * The cached value of the '{@link #getAcceptedDigits()
	 * <em>Accepted Digits</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @see #getAcceptedDigits()
	 * @generated
	 * @ordered
	 */
    protected String acceptedDigits = ACCEPTED_DIGITS_EDEFAULT;

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    protected GetDigitsImpl() {
        super();
    }

    @Override
    public void beginProcessing(SafletContext context) throws ActionStepException {
        super.beginProcessing(context);
        Exception exception = null;
        int idx = 0;
        if (call1 == null) {
            handleException(context, new ActionStepException("No current call found"));
            return;
        } else if (!(call1 instanceof Call)) {
            handleException(context, new ActionStepException("Call isn't isn't an Asterisk call: " + call1.getClass().getName()));
            return;
        }
        if (((Call) call1).getChannel() == null) {
            handleException(context, new ActionStepException("No channel found in current context"));
            return;
        }
        AgiChannel channel = ((Call) call1).getChannel();
        try {
            Variable v = resolveVariableFromName(variableName, context);
            StringBuilder buf = new StringBuilder();
            boolean rcvdEscape = false;
            int numRemainingDigits = maxDigits;
            if (useBufferedDigits) {
                for (int i = 0; i < maxDigits; i++) {
                    char next = ((AsteriskSafletContext) context).popBufferedDigit();
                    if (next == 253) break;
                    if (escapeDigits.indexOf(next) >= 0) {
                        rcvdEscape = true;
                        break;
                    }
                    numRemainingDigits--;
                    buf.append(next);
                }
            } else ((AsteriskSafletContext) context).flushBufferedDigits();
            if (!rcvdEscape && numRemainingDigits > 0) {
                if ("#".equals(escapeDigits)) buf.append(channel.getData("silence/1", inputTimeout, numRemainingDigits));
                for (int i = buf.length(); i < maxDigits; i++) {
                    char c = channel.waitForDigit((int) inputTimeout);
                    if (StringUtils.isNotBlank(escapeDigits) && escapeDigits.indexOf(c) >= 0) {
                        idx = 1;
                        break;
                    }
                    if (c != 0) {
                        String digitPressed = String.valueOf(c);
                        buf.append(digitPressed);
                    } else {
                        break;
                    }
                }
            }
            if (buf.length() > 0) idx = 1; else idx = 2;
            if (v != null) {
                if (v.getScope() != VariableScope.GLOBAL) context.setVariableRawValue(v.getName(), VariableTranslator.translateValue(v.getType(), buf.toString())); else {
                    SafletEnvironment env = getSaflet().getSafletEnvironment();
                    env.setGlobalVariableValue(v.getName(), VariableTranslator.translateValue(v.getType(), buf.toString()));
                }
            }
        } catch (Exception e) {
            exception = e;
        }
        if (exception != null) {
            handleException(context, exception);
            return;
        }
        handleSuccess(context, idx);
    }

    @Override
    public void createDefaultOutputs() {
        super.createDefaultOutputs();
        Output o = ActionStepFactory.eINSTANCE.createOutput();
        o.setOutputType(OutputType.CHOICE);
        o.setName("timeout");
        setErrorOutput(o);
        getOutputs().add(o);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return ActionstepPackage.Literals.GET_DIGITS;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public SafiCall getCall1() {
        if (call1 != null && call1.eIsProxy()) {
            InternalEObject oldCall1 = (InternalEObject) call1;
            call1 = (SafiCall) eResolveProxy(oldCall1);
            if (call1 != oldCall1) {
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.GET_DIGITS__CALL1, oldCall1, call1));
            }
        }
        return call1;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public SafiCall basicGetCall1() {
        return call1;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public void setCall1(SafiCall newCall1) {
        SafiCall oldCall1 = call1;
        call1 = newCall1;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_DIGITS__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public long getInputTimeout() {
        return inputTimeout;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public void setInputTimeout(long newInputTimeout) {
        long oldInputTimeout = inputTimeout;
        inputTimeout = newInputTimeout;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_DIGITS__INPUT_TIMEOUT, oldInputTimeout, inputTimeout));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public boolean isUseBufferedDigits() {
        return useBufferedDigits;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public void setUseBufferedDigits(boolean newUseBufferedDigits) {
        boolean oldUseBufferedDigits = useBufferedDigits;
        useBufferedDigits = newUseBufferedDigits;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_DIGITS__USE_BUFFERED_DIGITS, oldUseBufferedDigits, useBufferedDigits));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public String getEscapeDigits() {
        return escapeDigits;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public void setEscapeDigits(String newEscapeDigits) {
        String oldEscapeDigits = escapeDigits;
        escapeDigits = newEscapeDigits;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_DIGITS__ESCAPE_DIGITS, oldEscapeDigits, escapeDigits));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public DynamicValue getVariableName() {
        return variableName;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public NotificationChain basicSetVariableName(DynamicValue newVariableName, NotificationChain msgs) {
        DynamicValue oldVariableName = variableName;
        variableName = newVariableName;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_DIGITS__VARIABLE_NAME, oldVariableName, newVariableName);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public void setVariableName(DynamicValue newVariableName) {
        if (newVariableName != variableName) {
            NotificationChain msgs = null;
            if (variableName != null) msgs = ((InternalEObject) variableName).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_DIGITS__VARIABLE_NAME, null, msgs);
            if (newVariableName != null) msgs = ((InternalEObject) newVariableName).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_DIGITS__VARIABLE_NAME, null, msgs);
            msgs = basicSetVariableName(newVariableName, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_DIGITS__VARIABLE_NAME, newVariableName, newVariableName));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public int getMaxDigits() {
        return maxDigits;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public void setMaxDigits(int newMaxDigits) {
        int oldMaxDigits = maxDigits;
        maxDigits = newMaxDigits;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_DIGITS__MAX_DIGITS, oldMaxDigits, maxDigits));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public String getAcceptedDigits() {
        return acceptedDigits;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public void setAcceptedDigits(String newAcceptedDigits) {
        String oldAcceptedDigits = acceptedDigits;
        acceptedDigits = newAcceptedDigits;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_DIGITS__ACCEPTED_DIGITS, oldAcceptedDigits, acceptedDigits));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.GET_DIGITS__VARIABLE_NAME:
                return basicSetVariableName(null, msgs);
        }
        return super.eInverseRemove(otherEnd, featureID, msgs);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch(featureID) {
            case ActionstepPackage.GET_DIGITS__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.GET_DIGITS__INPUT_TIMEOUT:
                return getInputTimeout();
            case ActionstepPackage.GET_DIGITS__USE_BUFFERED_DIGITS:
                return isUseBufferedDigits();
            case ActionstepPackage.GET_DIGITS__ESCAPE_DIGITS:
                return getEscapeDigits();
            case ActionstepPackage.GET_DIGITS__VARIABLE_NAME:
                return getVariableName();
            case ActionstepPackage.GET_DIGITS__MAX_DIGITS:
                return getMaxDigits();
            case ActionstepPackage.GET_DIGITS__ACCEPTED_DIGITS:
                return getAcceptedDigits();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    @SuppressWarnings("unchecked")
    @Override
    public void eSet(int featureID, Object newValue) {
        switch(featureID) {
            case ActionstepPackage.GET_DIGITS__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.GET_DIGITS__INPUT_TIMEOUT:
                setInputTimeout((Long) newValue);
                return;
            case ActionstepPackage.GET_DIGITS__USE_BUFFERED_DIGITS:
                setUseBufferedDigits((Boolean) newValue);
                return;
            case ActionstepPackage.GET_DIGITS__ESCAPE_DIGITS:
                setEscapeDigits((String) newValue);
                return;
            case ActionstepPackage.GET_DIGITS__VARIABLE_NAME:
                setVariableName((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_DIGITS__MAX_DIGITS:
                setMaxDigits((Integer) newValue);
                return;
            case ActionstepPackage.GET_DIGITS__ACCEPTED_DIGITS:
                setAcceptedDigits((String) newValue);
                return;
        }
        super.eSet(featureID, newValue);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    @Override
    public void eUnset(int featureID) {
        switch(featureID) {
            case ActionstepPackage.GET_DIGITS__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.GET_DIGITS__INPUT_TIMEOUT:
                setInputTimeout(INPUT_TIMEOUT_EDEFAULT);
                return;
            case ActionstepPackage.GET_DIGITS__USE_BUFFERED_DIGITS:
                setUseBufferedDigits(USE_BUFFERED_DIGITS_EDEFAULT);
                return;
            case ActionstepPackage.GET_DIGITS__ESCAPE_DIGITS:
                setEscapeDigits(ESCAPE_DIGITS_EDEFAULT);
                return;
            case ActionstepPackage.GET_DIGITS__VARIABLE_NAME:
                setVariableName((DynamicValue) null);
                return;
            case ActionstepPackage.GET_DIGITS__MAX_DIGITS:
                setMaxDigits(MAX_DIGITS_EDEFAULT);
                return;
            case ActionstepPackage.GET_DIGITS__ACCEPTED_DIGITS:
                setAcceptedDigits(ACCEPTED_DIGITS_EDEFAULT);
                return;
        }
        super.eUnset(featureID);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    @Override
    public boolean eIsSet(int featureID) {
        switch(featureID) {
            case ActionstepPackage.GET_DIGITS__CALL1:
                return call1 != null;
            case ActionstepPackage.GET_DIGITS__INPUT_TIMEOUT:
                return inputTimeout != INPUT_TIMEOUT_EDEFAULT;
            case ActionstepPackage.GET_DIGITS__USE_BUFFERED_DIGITS:
                return useBufferedDigits != USE_BUFFERED_DIGITS_EDEFAULT;
            case ActionstepPackage.GET_DIGITS__ESCAPE_DIGITS:
                return ESCAPE_DIGITS_EDEFAULT == null ? escapeDigits != null : !ESCAPE_DIGITS_EDEFAULT.equals(escapeDigits);
            case ActionstepPackage.GET_DIGITS__VARIABLE_NAME:
                return variableName != null;
            case ActionstepPackage.GET_DIGITS__MAX_DIGITS:
                return maxDigits != MAX_DIGITS_EDEFAULT;
            case ActionstepPackage.GET_DIGITS__ACCEPTED_DIGITS:
                return ACCEPTED_DIGITS_EDEFAULT == null ? acceptedDigits != null : !ACCEPTED_DIGITS_EDEFAULT.equals(acceptedDigits);
        }
        return super.eIsSet(featureID);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    @Override
    public int eBaseStructuralFeatureID(int derivedFeatureID, Class<?> baseClass) {
        if (baseClass == CallConsumer1.class) {
            switch(derivedFeatureID) {
                case ActionstepPackage.GET_DIGITS__CALL1:
                    return CallPackage.CALL_CONSUMER1__CALL1;
                default:
                    return -1;
            }
        }
        return super.eBaseStructuralFeatureID(derivedFeatureID, baseClass);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    @Override
    public int eDerivedStructuralFeatureID(int baseFeatureID, Class<?> baseClass) {
        if (baseClass == CallConsumer1.class) {
            switch(baseFeatureID) {
                case CallPackage.CALL_CONSUMER1__CALL1:
                    return ActionstepPackage.GET_DIGITS__CALL1;
                default:
                    return -1;
            }
        }
        return super.eDerivedStructuralFeatureID(baseFeatureID, baseClass);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    @Override
    public String toString() {
        if (eIsProxy()) return super.toString();
        StringBuffer result = new StringBuffer(super.toString());
        result.append(" (inputTimeout: ");
        result.append(inputTimeout);
        result.append(", useBufferedDigits: ");
        result.append(useBufferedDigits);
        result.append(", escapeDigits: ");
        result.append(escapeDigits);
        result.append(", maxDigits: ");
        result.append(maxDigits);
        result.append(", acceptedDigits: ");
        result.append(acceptedDigits);
        result.append(')');
        return result.toString();
    }
}
