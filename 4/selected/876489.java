package com.safi.asterisk.actionstep.impl;

import java.util.logging.Level;
import org.asteriskjava.fastagi.AgiChannel;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import com.safi.asterisk.Call;
import com.safi.asterisk.actionstep.ActionstepPackage;
import com.safi.asterisk.actionstep.WaitForDigit;
import com.safi.asterisk.saflet.AsteriskSafletContext;
import com.safi.core.actionstep.ActionStepException;
import com.safi.core.actionstep.ActionStepFactory;
import com.safi.core.actionstep.Output;
import com.safi.core.actionstep.OutputType;
import com.safi.core.actionstep.impl.ActionStepImpl;
import com.safi.core.call.CallConsumer1;
import com.safi.core.call.CallPackage;
import com.safi.core.call.SafiCall;
import com.safi.core.saflet.SafletContext;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Wait For Digit</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.WaitForDigitImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.WaitForDigitImpl#getTimeout <em>Timeout</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.WaitForDigitImpl#getAcceptedDigits <em>Accepted Digits</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class WaitForDigitImpl extends AsteriskActionStepImpl implements WaitForDigit {

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
	 * The default value of the '{@link #getTimeout() <em>Timeout</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getTimeout()
	 * @generated
	 * @ordered
	 */
    protected static final long TIMEOUT_EDEFAULT = -1L;

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
	 * The default value of the '{@link #getAcceptedDigits() <em>Accepted Digits</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getAcceptedDigits()
	 * @generated
	 * @ordered
	 */
    protected static final String ACCEPTED_DIGITS_EDEFAULT = "0123456789#";

    /**
	 * The cached value of the '{@link #getAcceptedDigits() <em>Accepted Digits</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getAcceptedDigits()
	 * @generated
	 * @ordered
	 */
    protected String acceptedDigits = ACCEPTED_DIGITS_EDEFAULT;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected WaitForDigitImpl() {
        super();
    }

    @Override
    public void beginProcessing(SafletContext context) throws ActionStepException {
        super.beginProcessing(context);
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
        boolean success = false;
        try {
            char c = Character.MAX_VALUE;
            while (true) {
                c = channel.waitForDigit((int) timeout);
                if (debugLog.isLoggable(Level.FINEST)) debug("Waitfordigit got " + c);
                if (c == 0) break;
                if (c != 0 && (acceptedDigits == null || acceptedDigits.indexOf(c) >= 0)) {
                    String digitPressed = String.valueOf(c);
                    ((AsteriskSafletContext) context).appendBufferedDigits(digitPressed);
                    success = true;
                    break;
                }
            }
        } catch (Exception e) {
            handleException(context, e);
            return;
        }
        handleSuccess(context, success ? 1 : 2);
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
        return ActionstepPackage.Literals.WAIT_FOR_DIGIT;
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.WAIT_FOR_DIGIT__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.WAIT_FOR_DIGIT__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.WAIT_FOR_DIGIT__TIMEOUT, oldTimeout, timeout));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public String getAcceptedDigits() {
        return acceptedDigits;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setAcceptedDigits(String newAcceptedDigits) {
        String oldAcceptedDigits = acceptedDigits;
        acceptedDigits = newAcceptedDigits;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.WAIT_FOR_DIGIT__ACCEPTED_DIGITS, oldAcceptedDigits, acceptedDigits));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch(featureID) {
            case ActionstepPackage.WAIT_FOR_DIGIT__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.WAIT_FOR_DIGIT__TIMEOUT:
                return getTimeout();
            case ActionstepPackage.WAIT_FOR_DIGIT__ACCEPTED_DIGITS:
                return getAcceptedDigits();
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
            case ActionstepPackage.WAIT_FOR_DIGIT__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.WAIT_FOR_DIGIT__TIMEOUT:
                setTimeout((Long) newValue);
                return;
            case ActionstepPackage.WAIT_FOR_DIGIT__ACCEPTED_DIGITS:
                setAcceptedDigits((String) newValue);
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
            case ActionstepPackage.WAIT_FOR_DIGIT__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.WAIT_FOR_DIGIT__TIMEOUT:
                setTimeout(TIMEOUT_EDEFAULT);
                return;
            case ActionstepPackage.WAIT_FOR_DIGIT__ACCEPTED_DIGITS:
                setAcceptedDigits(ACCEPTED_DIGITS_EDEFAULT);
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
            case ActionstepPackage.WAIT_FOR_DIGIT__CALL1:
                return call1 != null;
            case ActionstepPackage.WAIT_FOR_DIGIT__TIMEOUT:
                return timeout != TIMEOUT_EDEFAULT;
            case ActionstepPackage.WAIT_FOR_DIGIT__ACCEPTED_DIGITS:
                return ACCEPTED_DIGITS_EDEFAULT == null ? acceptedDigits != null : !ACCEPTED_DIGITS_EDEFAULT.equals(acceptedDigits);
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
                case ActionstepPackage.WAIT_FOR_DIGIT__CALL1:
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
                    return ActionstepPackage.WAIT_FOR_DIGIT__CALL1;
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
        result.append(", acceptedDigits: ");
        result.append(acceptedDigits);
        result.append(')');
        return result.toString();
    }
}
