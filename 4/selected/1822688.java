package com.safi.asterisk.actionstep.impl;

import java.util.logging.Level;
import org.apache.commons.lang.StringUtils;
import org.asteriskjava.fastagi.AgiChannel;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import com.safi.asterisk.Call;
import com.safi.asterisk.actionstep.ActionstepPackage;
import com.safi.asterisk.actionstep.PromptGetDigits;
import com.safi.asterisk.saflet.AsteriskSafletContext;
import com.safi.core.actionstep.ActionStepException;
import com.safi.core.actionstep.ActionStepFactory;
import com.safi.core.actionstep.DynamicValue;
import com.safi.core.actionstep.DynamicValueType;
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
import com.safi.db.VariableType;
import com.safi.db.util.VariableTranslator;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Prompt Get Digits</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.PromptGetDigitsImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.PromptGetDigitsImpl#getFilename <em>Filename</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.PromptGetDigitsImpl#getTimeout <em>Timeout</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.PromptGetDigitsImpl#getVariableName <em>Variable Name</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.PromptGetDigitsImpl#getMaxDigits <em>Max Digits</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.PromptGetDigitsImpl#isUseBufferedDigits <em>Use Buffered Digits</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class PromptGetDigitsImpl extends AsteriskActionStepImpl implements PromptGetDigits {

    /**
	 * The cached value of the '{@link #getCall1() <em>Call1</em>}' reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getCall1()
	 * @generated
	 * @ordered
	 */
    protected SafiCall call1;

    /**
	 * The cached value of the '{@link #getFilename() <em>Filename</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getFilename()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue filename;

    /**
	 * The default value of the '{@link #getTimeout() <em>Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getTimeout()
	 * @generated
	 * @ordered
	 */
    protected static final long TIMEOUT_EDEFAULT = 0L;

    /**
	 * The cached value of the '{@link #getTimeout() <em>Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getTimeout()
	 * @generated
	 * @ordered
	 */
    protected long timeout = TIMEOUT_EDEFAULT;

    /**
	 * The cached value of the '{@link #getVariableName() <em>Variable Name</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getVariableName()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue variableName;

    /**
	 * The default value of the '{@link #getMaxDigits() <em>Max Digits</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getMaxDigits()
	 * @generated
	 * @ordered
	 */
    protected static final int MAX_DIGITS_EDEFAULT = 1;

    /**
	 * The cached value of the '{@link #getMaxDigits() <em>Max Digits</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getMaxDigits()
	 * @generated
	 * @ordered
	 */
    protected int maxDigits = MAX_DIGITS_EDEFAULT;

    /**
	 * The default value of the '{@link #isUseBufferedDigits() <em>Use Buffered Digits</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isUseBufferedDigits()
	 * @generated
	 * @ordered
	 */
    protected static final boolean USE_BUFFERED_DIGITS_EDEFAULT = false;

    /**
	 * The cached value of the '{@link #isUseBufferedDigits() <em>Use Buffered Digits</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isUseBufferedDigits()
	 * @generated
	 * @ordered
	 */
    protected boolean useBufferedDigits = USE_BUFFERED_DIGITS_EDEFAULT;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected PromptGetDigitsImpl() {
        super();
    }

    @Override
    public void beginProcessing(SafletContext context) throws ActionStepException {
        super.beginProcessing(context);
        Exception exception = null;
        int idx = 1;
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
            Object dynValue = resolveDynamicValue(filename, context);
            String filenameStr = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
            {
                Variable v = null;
                try {
                    v = resolveVariableFromName(variableName, context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (StringUtils.isNotBlank(filenameStr) && filename.getType() == DynamicValueType.CUSTOM) filenameStr = getSaflet().getPromptPathByName(filenameStr);
                StringBuffer buf = new StringBuffer();
                boolean rcvdEscape = false;
                int numRemainingDigits = maxDigits;
                if (useBufferedDigits) {
                    for (int i = 0; i < maxDigits; i++) {
                        char next = ((AsteriskSafletContext) context).popBufferedDigit();
                        if (next == 253) break;
                        if (next == '#') {
                            rcvdEscape = true;
                            break;
                        }
                        numRemainingDigits--;
                        buf.append(next);
                    }
                } else ((AsteriskSafletContext) context).flushBufferedDigits();
                if (!rcvdEscape && numRemainingDigits > 0) {
                    buf.append(channel.getData(StringUtils.isBlank(filenameStr) || (useBufferedDigits && buf.length() > 0) ? "silence/1" : filenameStr, timeout, numRemainingDigits));
                }
                if (debugLog.isLoggable(Level.FINEST)) debug("Data returned was " + buf);
                if (StringUtils.isBlank(buf.toString()) || buf.toString().toLowerCase().indexOf("(timeout)") >= 0) {
                    if (debugLog.isLoggable(Level.FINEST)) info("Taking timeout path...");
                    idx = 2;
                } else if (v != null) {
                    if (v.getScope() != VariableScope.GLOBAL) context.setVariableRawValue(v.getName(), VariableTranslator.translateValue(v.getType(), buf.toString())); else {
                        SafletEnvironment env = getSaflet().getSafletEnvironment();
                        env.setGlobalVariableValue(v.getName(), VariableTranslator.translateValue(v.getType(), buf.toString()));
                    }
                } else {
                    String msg = "warning: variable not specified. Discarding digits " + buf.toString();
                    if (debugLog.isLoggable(Level.FINEST)) debugLog.warning(msg);
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
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return ActionstepPackage.Literals.PROMPT_GET_DIGITS;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public SafiCall getCall1() {
        if (call1 != null && call1.eIsProxy()) {
            InternalEObject oldCall1 = (InternalEObject) call1;
            call1 = (SafiCall) eResolveProxy(oldCall1);
            if (call1 != oldCall1) {
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.PROMPT_GET_DIGITS__CALL1, oldCall1, call1));
            }
        }
        return call1;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public SafiCall basicGetCall1() {
        return call1;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public void setCall1(SafiCall newCall1) {
        SafiCall oldCall1 = call1;
        call1 = newCall1;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.PROMPT_GET_DIGITS__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getFilename() {
        return filename;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetFilename(DynamicValue newFilename, NotificationChain msgs) {
        DynamicValue oldFilename = filename;
        filename = newFilename;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.PROMPT_GET_DIGITS__FILENAME, oldFilename, newFilename);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setFilename(DynamicValue newFilename) {
        if (newFilename != filename) {
            NotificationChain msgs = null;
            if (filename != null) msgs = ((InternalEObject) filename).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.PROMPT_GET_DIGITS__FILENAME, null, msgs);
            if (newFilename != null) msgs = ((InternalEObject) newFilename).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.PROMPT_GET_DIGITS__FILENAME, null, msgs);
            msgs = basicSetFilename(newFilename, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.PROMPT_GET_DIGITS__FILENAME, newFilename, newFilename));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public long getTimeout() {
        return timeout;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setTimeout(long newTimeout) {
        long oldTimeout = timeout;
        timeout = newTimeout;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.PROMPT_GET_DIGITS__TIMEOUT, oldTimeout, timeout));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getVariableName() {
        return variableName;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetVariableName(DynamicValue newVariableName, NotificationChain msgs) {
        DynamicValue oldVariableName = variableName;
        variableName = newVariableName;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.PROMPT_GET_DIGITS__VARIABLE_NAME, oldVariableName, newVariableName);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setVariableName(DynamicValue newVariableName) {
        if (newVariableName != variableName) {
            NotificationChain msgs = null;
            if (variableName != null) msgs = ((InternalEObject) variableName).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.PROMPT_GET_DIGITS__VARIABLE_NAME, null, msgs);
            if (newVariableName != null) msgs = ((InternalEObject) newVariableName).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.PROMPT_GET_DIGITS__VARIABLE_NAME, null, msgs);
            msgs = basicSetVariableName(newVariableName, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.PROMPT_GET_DIGITS__VARIABLE_NAME, newVariableName, newVariableName));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public int getMaxDigits() {
        return maxDigits;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setMaxDigits(int newMaxDigits) {
        int oldMaxDigits = maxDigits;
        maxDigits = newMaxDigits;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.PROMPT_GET_DIGITS__MAX_DIGITS, oldMaxDigits, maxDigits));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isUseBufferedDigits() {
        return useBufferedDigits;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setUseBufferedDigits(boolean newUseBufferedDigits) {
        boolean oldUseBufferedDigits = useBufferedDigits;
        useBufferedDigits = newUseBufferedDigits;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.PROMPT_GET_DIGITS__USE_BUFFERED_DIGITS, oldUseBufferedDigits, useBufferedDigits));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.PROMPT_GET_DIGITS__FILENAME:
                return basicSetFilename(null, msgs);
            case ActionstepPackage.PROMPT_GET_DIGITS__VARIABLE_NAME:
                return basicSetVariableName(null, msgs);
        }
        return super.eInverseRemove(otherEnd, featureID, msgs);
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch(featureID) {
            case ActionstepPackage.PROMPT_GET_DIGITS__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.PROMPT_GET_DIGITS__FILENAME:
                return getFilename();
            case ActionstepPackage.PROMPT_GET_DIGITS__TIMEOUT:
                return getTimeout();
            case ActionstepPackage.PROMPT_GET_DIGITS__VARIABLE_NAME:
                return getVariableName();
            case ActionstepPackage.PROMPT_GET_DIGITS__MAX_DIGITS:
                return getMaxDigits();
            case ActionstepPackage.PROMPT_GET_DIGITS__USE_BUFFERED_DIGITS:
                return isUseBufferedDigits();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @SuppressWarnings("unchecked")
    @Override
    public void eSet(int featureID, Object newValue) {
        switch(featureID) {
            case ActionstepPackage.PROMPT_GET_DIGITS__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.PROMPT_GET_DIGITS__FILENAME:
                setFilename((DynamicValue) newValue);
                return;
            case ActionstepPackage.PROMPT_GET_DIGITS__TIMEOUT:
                setTimeout((Long) newValue);
                return;
            case ActionstepPackage.PROMPT_GET_DIGITS__VARIABLE_NAME:
                setVariableName((DynamicValue) newValue);
                return;
            case ActionstepPackage.PROMPT_GET_DIGITS__MAX_DIGITS:
                setMaxDigits((Integer) newValue);
                return;
            case ActionstepPackage.PROMPT_GET_DIGITS__USE_BUFFERED_DIGITS:
                setUseBufferedDigits((Boolean) newValue);
                return;
        }
        super.eSet(featureID, newValue);
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public void eUnset(int featureID) {
        switch(featureID) {
            case ActionstepPackage.PROMPT_GET_DIGITS__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.PROMPT_GET_DIGITS__FILENAME:
                setFilename((DynamicValue) null);
                return;
            case ActionstepPackage.PROMPT_GET_DIGITS__TIMEOUT:
                setTimeout(TIMEOUT_EDEFAULT);
                return;
            case ActionstepPackage.PROMPT_GET_DIGITS__VARIABLE_NAME:
                setVariableName((DynamicValue) null);
                return;
            case ActionstepPackage.PROMPT_GET_DIGITS__MAX_DIGITS:
                setMaxDigits(MAX_DIGITS_EDEFAULT);
                return;
            case ActionstepPackage.PROMPT_GET_DIGITS__USE_BUFFERED_DIGITS:
                setUseBufferedDigits(USE_BUFFERED_DIGITS_EDEFAULT);
                return;
        }
        super.eUnset(featureID);
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public boolean eIsSet(int featureID) {
        switch(featureID) {
            case ActionstepPackage.PROMPT_GET_DIGITS__CALL1:
                return call1 != null;
            case ActionstepPackage.PROMPT_GET_DIGITS__FILENAME:
                return filename != null;
            case ActionstepPackage.PROMPT_GET_DIGITS__TIMEOUT:
                return timeout != TIMEOUT_EDEFAULT;
            case ActionstepPackage.PROMPT_GET_DIGITS__VARIABLE_NAME:
                return variableName != null;
            case ActionstepPackage.PROMPT_GET_DIGITS__MAX_DIGITS:
                return maxDigits != MAX_DIGITS_EDEFAULT;
            case ActionstepPackage.PROMPT_GET_DIGITS__USE_BUFFERED_DIGITS:
                return useBufferedDigits != USE_BUFFERED_DIGITS_EDEFAULT;
        }
        return super.eIsSet(featureID);
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public int eBaseStructuralFeatureID(int derivedFeatureID, Class<?> baseClass) {
        if (baseClass == CallConsumer1.class) {
            switch(derivedFeatureID) {
                case ActionstepPackage.PROMPT_GET_DIGITS__CALL1:
                    return CallPackage.CALL_CONSUMER1__CALL1;
                default:
                    return -1;
            }
        }
        return super.eBaseStructuralFeatureID(derivedFeatureID, baseClass);
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public int eDerivedStructuralFeatureID(int baseFeatureID, Class<?> baseClass) {
        if (baseClass == CallConsumer1.class) {
            switch(baseFeatureID) {
                case CallPackage.CALL_CONSUMER1__CALL1:
                    return ActionstepPackage.PROMPT_GET_DIGITS__CALL1;
                default:
                    return -1;
            }
        }
        return super.eDerivedStructuralFeatureID(baseFeatureID, baseClass);
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public String toString() {
        if (eIsProxy()) return super.toString();
        StringBuffer result = new StringBuffer(super.toString());
        result.append(" (timeout: ");
        result.append(timeout);
        result.append(", maxDigits: ");
        result.append(maxDigits);
        result.append(", useBufferedDigits: ");
        result.append(useBufferedDigits);
        result.append(')');
        return result.toString();
    }
}
