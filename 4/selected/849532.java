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
import com.safi.asterisk.actionstep.SetMusicOn;
import com.safi.core.actionstep.ActionStepException;
import com.safi.core.actionstep.DynamicValue;
import com.safi.core.actionstep.impl.ActionStepImpl;
import com.safi.core.call.CallConsumer1;
import com.safi.core.call.CallPackage;
import com.safi.core.call.SafiCall;
import com.safi.core.saflet.SafletContext;
import com.safi.db.VariableType;
import com.safi.db.util.VariableTranslator;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Set Music On</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.SetMusicOnImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.SetMusicOnImpl#getHoldClass <em>Hold Class</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class SetMusicOnImpl extends AsteriskActionStepImpl implements SetMusicOn {

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
	 * The cached value of the '{@link #getHoldClass() <em>Hold Class</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getHoldClass()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue holdClass;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected SetMusicOnImpl() {
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
        Exception exception = null;
        try {
            Object dynValue = resolveDynamicValue(holdClass, context);
            String holdClassStr = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
            if (StringUtils.isBlank(holdClassStr)) {
                exception = new ActionStepException("Hold class was not specified.");
            } else {
                int result = channel.exec("SetMusicOnHold", holdClassStr);
                if (debugLog.isLoggable(Level.FINEST)) debug("SetMusicOnHold returned " + translateAppReturnValue(result) + " of int " + result);
                if (result == -2) {
                    exception = new ActionStepException("Application SetMusicOnHold not found");
                } else if (result == -1) {
                    exception = new ActionStepException("Channel was hung up");
                }
            }
        } catch (Exception e) {
            exception = e;
        }
        if (exception != null) {
            handleException(context, exception);
            return;
        }
        handleSuccess(context);
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return ActionstepPackage.Literals.SET_MUSIC_ON;
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.SET_MUSIC_ON__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.SET_MUSIC_ON__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getHoldClass() {
        return holdClass;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetHoldClass(DynamicValue newHoldClass, NotificationChain msgs) {
        DynamicValue oldHoldClass = holdClass;
        holdClass = newHoldClass;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.SET_MUSIC_ON__HOLD_CLASS, oldHoldClass, newHoldClass);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setHoldClass(DynamicValue newHoldClass) {
        if (newHoldClass != holdClass) {
            NotificationChain msgs = null;
            if (holdClass != null) msgs = ((InternalEObject) holdClass).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.SET_MUSIC_ON__HOLD_CLASS, null, msgs);
            if (newHoldClass != null) msgs = ((InternalEObject) newHoldClass).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.SET_MUSIC_ON__HOLD_CLASS, null, msgs);
            msgs = basicSetHoldClass(newHoldClass, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.SET_MUSIC_ON__HOLD_CLASS, newHoldClass, newHoldClass));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.SET_MUSIC_ON__HOLD_CLASS:
                return basicSetHoldClass(null, msgs);
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
            case ActionstepPackage.SET_MUSIC_ON__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.SET_MUSIC_ON__HOLD_CLASS:
                return getHoldClass();
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
            case ActionstepPackage.SET_MUSIC_ON__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.SET_MUSIC_ON__HOLD_CLASS:
                setHoldClass((DynamicValue) newValue);
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
            case ActionstepPackage.SET_MUSIC_ON__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.SET_MUSIC_ON__HOLD_CLASS:
                setHoldClass((DynamicValue) null);
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
            case ActionstepPackage.SET_MUSIC_ON__CALL1:
                return call1 != null;
            case ActionstepPackage.SET_MUSIC_ON__HOLD_CLASS:
                return holdClass != null;
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
                case ActionstepPackage.SET_MUSIC_ON__CALL1:
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
                    return ActionstepPackage.SET_MUSIC_ON__CALL1;
                default:
                    return -1;
            }
        }
        return super.eDerivedStructuralFeatureID(baseFeatureID, baseClass);
    }
}
