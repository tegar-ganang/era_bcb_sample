package com.safi.asterisk.actionstep.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import com.safi.asterisk.actionstep.ActionstepPackage;
import com.safi.asterisk.actionstep.SoftHangup;
import com.safi.core.actionstep.DynamicValue;
import com.safi.core.actionstep.impl.ActionStepImpl;
import com.safi.core.call.CallConsumer1;
import com.safi.core.call.CallPackage;
import com.safi.core.call.SafiCall;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Soft Hangup</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.SoftHangupImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.SoftHangupImpl#isHangupAllDeviceCalls <em>Hangup All Device Calls</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.SoftHangupImpl#getChannelName <em>Channel Name</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class SoftHangupImpl extends AsteriskActionStepImpl implements SoftHangup {

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
	 * The default value of the '{@link #isHangupAllDeviceCalls() <em>Hangup All Device Calls</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isHangupAllDeviceCalls()
	 * @generated
	 * @ordered
	 */
    protected static final boolean HANGUP_ALL_DEVICE_CALLS_EDEFAULT = false;

    /**
	 * The cached value of the '{@link #isHangupAllDeviceCalls() <em>Hangup All Device Calls</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isHangupAllDeviceCalls()
	 * @generated
	 * @ordered
	 */
    protected boolean hangupAllDeviceCalls = HANGUP_ALL_DEVICE_CALLS_EDEFAULT;

    /**
	 * The cached value of the '{@link #getChannelName() <em>Channel Name</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getChannelName()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue channelName;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected SoftHangupImpl() {
        super();
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return ActionstepPackage.Literals.SOFT_HANGUP;
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.SOFT_HANGUP__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.SOFT_HANGUP__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isHangupAllDeviceCalls() {
        return hangupAllDeviceCalls;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setHangupAllDeviceCalls(boolean newHangupAllDeviceCalls) {
        boolean oldHangupAllDeviceCalls = hangupAllDeviceCalls;
        hangupAllDeviceCalls = newHangupAllDeviceCalls;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.SOFT_HANGUP__HANGUP_ALL_DEVICE_CALLS, oldHangupAllDeviceCalls, hangupAllDeviceCalls));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getChannelName() {
        return channelName;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetChannelName(DynamicValue newChannelName, NotificationChain msgs) {
        DynamicValue oldChannelName = channelName;
        channelName = newChannelName;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.SOFT_HANGUP__CHANNEL_NAME, oldChannelName, newChannelName);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setChannelName(DynamicValue newChannelName) {
        if (newChannelName != channelName) {
            NotificationChain msgs = null;
            if (channelName != null) msgs = ((InternalEObject) channelName).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.SOFT_HANGUP__CHANNEL_NAME, null, msgs);
            if (newChannelName != null) msgs = ((InternalEObject) newChannelName).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.SOFT_HANGUP__CHANNEL_NAME, null, msgs);
            msgs = basicSetChannelName(newChannelName, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.SOFT_HANGUP__CHANNEL_NAME, newChannelName, newChannelName));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.SOFT_HANGUP__CHANNEL_NAME:
                return basicSetChannelName(null, msgs);
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
            case ActionstepPackage.SOFT_HANGUP__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.SOFT_HANGUP__HANGUP_ALL_DEVICE_CALLS:
                return isHangupAllDeviceCalls();
            case ActionstepPackage.SOFT_HANGUP__CHANNEL_NAME:
                return getChannelName();
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
            case ActionstepPackage.SOFT_HANGUP__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.SOFT_HANGUP__HANGUP_ALL_DEVICE_CALLS:
                setHangupAllDeviceCalls((Boolean) newValue);
                return;
            case ActionstepPackage.SOFT_HANGUP__CHANNEL_NAME:
                setChannelName((DynamicValue) newValue);
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
            case ActionstepPackage.SOFT_HANGUP__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.SOFT_HANGUP__HANGUP_ALL_DEVICE_CALLS:
                setHangupAllDeviceCalls(HANGUP_ALL_DEVICE_CALLS_EDEFAULT);
                return;
            case ActionstepPackage.SOFT_HANGUP__CHANNEL_NAME:
                setChannelName((DynamicValue) null);
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
            case ActionstepPackage.SOFT_HANGUP__CALL1:
                return call1 != null;
            case ActionstepPackage.SOFT_HANGUP__HANGUP_ALL_DEVICE_CALLS:
                return hangupAllDeviceCalls != HANGUP_ALL_DEVICE_CALLS_EDEFAULT;
            case ActionstepPackage.SOFT_HANGUP__CHANNEL_NAME:
                return channelName != null;
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
                case ActionstepPackage.SOFT_HANGUP__CALL1:
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
                    return ActionstepPackage.SOFT_HANGUP__CALL1;
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
        result.append(" (hangupAllDeviceCalls: ");
        result.append(hangupAllDeviceCalls);
        result.append(')');
        return result.toString();
    }
}
