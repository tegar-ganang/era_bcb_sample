package com.safi.asterisk.actionstep.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import com.safi.asterisk.actionstep.ActionstepPackage;
import com.safi.asterisk.actionstep.Bridge;
import com.safi.core.actionstep.DynamicValue;
import com.safi.core.actionstep.impl.ActionStepImpl;
import com.safi.core.call.CallConsumer1;
import com.safi.core.call.CallConsumer2;
import com.safi.core.call.CallPackage;
import com.safi.core.call.SafiCall;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Bridge</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.BridgeImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.BridgeImpl#getCall2 <em>Call2</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.BridgeImpl#getChannel1 <em>Channel1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.BridgeImpl#getChannel2 <em>Channel2</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.BridgeImpl#isUseCourtesyTone <em>Use Courtesy Tone</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class BridgeImpl extends AsteriskActionStepImpl implements Bridge {

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
	 * The cached value of the '{@link #getCall2() <em>Call2</em>}' reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getCall2()
	 * @generated
	 * @ordered
	 */
    protected SafiCall call2;

    /**
	 * The cached value of the '{@link #getChannel1() <em>Channel1</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getChannel1()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue channel1;

    /**
	 * The cached value of the '{@link #getChannel2() <em>Channel2</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getChannel2()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue channel2;

    /**
	 * The default value of the '{@link #isUseCourtesyTone() <em>Use Courtesy Tone</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isUseCourtesyTone()
	 * @generated
	 * @ordered
	 */
    protected static final boolean USE_COURTESY_TONE_EDEFAULT = false;

    /**
	 * The cached value of the '{@link #isUseCourtesyTone() <em>Use Courtesy Tone</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isUseCourtesyTone()
	 * @generated
	 * @ordered
	 */
    protected boolean useCourtesyTone = USE_COURTESY_TONE_EDEFAULT;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected BridgeImpl() {
        super();
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return ActionstepPackage.Literals.BRIDGE;
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.BRIDGE__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.BRIDGE__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public SafiCall getCall2() {
        if (call2 != null && call2.eIsProxy()) {
            InternalEObject oldCall2 = (InternalEObject) call2;
            call2 = (SafiCall) eResolveProxy(oldCall2);
            if (call2 != oldCall2) {
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.BRIDGE__CALL2, oldCall2, call2));
            }
        }
        return call2;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public SafiCall basicGetCall2() {
        return call2;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public void setCall2(SafiCall newCall2) {
        SafiCall oldCall2 = call2;
        call2 = newCall2;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.BRIDGE__CALL2, oldCall2, call2));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getChannel1() {
        return channel1;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetChannel1(DynamicValue newChannel1, NotificationChain msgs) {
        DynamicValue oldChannel1 = channel1;
        channel1 = newChannel1;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.BRIDGE__CHANNEL1, oldChannel1, newChannel1);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setChannel1(DynamicValue newChannel1) {
        if (newChannel1 != channel1) {
            NotificationChain msgs = null;
            if (channel1 != null) msgs = ((InternalEObject) channel1).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.BRIDGE__CHANNEL1, null, msgs);
            if (newChannel1 != null) msgs = ((InternalEObject) newChannel1).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.BRIDGE__CHANNEL1, null, msgs);
            msgs = basicSetChannel1(newChannel1, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.BRIDGE__CHANNEL1, newChannel1, newChannel1));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getChannel2() {
        return channel2;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetChannel2(DynamicValue newChannel2, NotificationChain msgs) {
        DynamicValue oldChannel2 = channel2;
        channel2 = newChannel2;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.BRIDGE__CHANNEL2, oldChannel2, newChannel2);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setChannel2(DynamicValue newChannel2) {
        if (newChannel2 != channel2) {
            NotificationChain msgs = null;
            if (channel2 != null) msgs = ((InternalEObject) channel2).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.BRIDGE__CHANNEL2, null, msgs);
            if (newChannel2 != null) msgs = ((InternalEObject) newChannel2).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.BRIDGE__CHANNEL2, null, msgs);
            msgs = basicSetChannel2(newChannel2, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.BRIDGE__CHANNEL2, newChannel2, newChannel2));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isUseCourtesyTone() {
        return useCourtesyTone;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setUseCourtesyTone(boolean newUseCourtesyTone) {
        boolean oldUseCourtesyTone = useCourtesyTone;
        useCourtesyTone = newUseCourtesyTone;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.BRIDGE__USE_COURTESY_TONE, oldUseCourtesyTone, useCourtesyTone));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.BRIDGE__CHANNEL1:
                return basicSetChannel1(null, msgs);
            case ActionstepPackage.BRIDGE__CHANNEL2:
                return basicSetChannel2(null, msgs);
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
            case ActionstepPackage.BRIDGE__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.BRIDGE__CALL2:
                if (resolve) return getCall2();
                return basicGetCall2();
            case ActionstepPackage.BRIDGE__CHANNEL1:
                return getChannel1();
            case ActionstepPackage.BRIDGE__CHANNEL2:
                return getChannel2();
            case ActionstepPackage.BRIDGE__USE_COURTESY_TONE:
                return isUseCourtesyTone();
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
            case ActionstepPackage.BRIDGE__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.BRIDGE__CALL2:
                setCall2((SafiCall) newValue);
                return;
            case ActionstepPackage.BRIDGE__CHANNEL1:
                setChannel1((DynamicValue) newValue);
                return;
            case ActionstepPackage.BRIDGE__CHANNEL2:
                setChannel2((DynamicValue) newValue);
                return;
            case ActionstepPackage.BRIDGE__USE_COURTESY_TONE:
                setUseCourtesyTone((Boolean) newValue);
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
            case ActionstepPackage.BRIDGE__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.BRIDGE__CALL2:
                setCall2((SafiCall) null);
                return;
            case ActionstepPackage.BRIDGE__CHANNEL1:
                setChannel1((DynamicValue) null);
                return;
            case ActionstepPackage.BRIDGE__CHANNEL2:
                setChannel2((DynamicValue) null);
                return;
            case ActionstepPackage.BRIDGE__USE_COURTESY_TONE:
                setUseCourtesyTone(USE_COURTESY_TONE_EDEFAULT);
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
            case ActionstepPackage.BRIDGE__CALL1:
                return call1 != null;
            case ActionstepPackage.BRIDGE__CALL2:
                return call2 != null;
            case ActionstepPackage.BRIDGE__CHANNEL1:
                return channel1 != null;
            case ActionstepPackage.BRIDGE__CHANNEL2:
                return channel2 != null;
            case ActionstepPackage.BRIDGE__USE_COURTESY_TONE:
                return useCourtesyTone != USE_COURTESY_TONE_EDEFAULT;
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
                case ActionstepPackage.BRIDGE__CALL1:
                    return CallPackage.CALL_CONSUMER1__CALL1;
                default:
                    return -1;
            }
        }
        if (baseClass == CallConsumer2.class) {
            switch(derivedFeatureID) {
                case ActionstepPackage.BRIDGE__CALL2:
                    return CallPackage.CALL_CONSUMER2__CALL2;
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
                    return ActionstepPackage.BRIDGE__CALL1;
                default:
                    return -1;
            }
        }
        if (baseClass == CallConsumer2.class) {
            switch(baseFeatureID) {
                case CallPackage.CALL_CONSUMER2__CALL2:
                    return ActionstepPackage.BRIDGE__CALL2;
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
        result.append(" (useCourtesyTone: ");
        result.append(useCourtesyTone);
        result.append(')');
        return result.toString();
    }
}
