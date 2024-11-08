package com.safi.asterisk.actionstep.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import com.safi.asterisk.actionstep.ActionstepPackage;
import com.safi.asterisk.actionstep.GetAvailableChannel;
import com.safi.core.actionstep.DynamicValue;
import com.safi.core.actionstep.impl.ActionStepImpl;
import com.safi.core.call.CallConsumer1;
import com.safi.core.call.CallPackage;
import com.safi.core.call.SafiCall;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Get Available Channel</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetAvailableChannelImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetAvailableChannelImpl#getChannels <em>Channels</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetAvailableChannelImpl#getVariableName <em>Variable Name</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetAvailableChannelImpl#isIgnoreInUse <em>Ignore In Use</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetAvailableChannelImpl#isJumpPriorityOnFail <em>Jump Priority On Fail</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class GetAvailableChannelImpl extends AsteriskActionStepImpl implements GetAvailableChannel {

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
	 * The cached value of the '{@link #getChannels() <em>Channels</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getChannels()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue channels;

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
	 * The default value of the '{@link #isIgnoreInUse() <em>Ignore In Use</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isIgnoreInUse()
	 * @generated
	 * @ordered
	 */
    protected static final boolean IGNORE_IN_USE_EDEFAULT = false;

    /**
	 * The cached value of the '{@link #isIgnoreInUse() <em>Ignore In Use</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isIgnoreInUse()
	 * @generated
	 * @ordered
	 */
    protected boolean ignoreInUse = IGNORE_IN_USE_EDEFAULT;

    /**
	 * The default value of the '{@link #isJumpPriorityOnFail() <em>Jump Priority On Fail</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isJumpPriorityOnFail()
	 * @generated
	 * @ordered
	 */
    protected static final boolean JUMP_PRIORITY_ON_FAIL_EDEFAULT = false;

    /**
	 * The cached value of the '{@link #isJumpPriorityOnFail() <em>Jump Priority On Fail</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isJumpPriorityOnFail()
	 * @generated
	 * @ordered
	 */
    protected boolean jumpPriorityOnFail = JUMP_PRIORITY_ON_FAIL_EDEFAULT;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected GetAvailableChannelImpl() {
        super();
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return ActionstepPackage.Literals.GET_AVAILABLE_CHANNEL;
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.GET_AVAILABLE_CHANNEL__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_AVAILABLE_CHANNEL__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getChannels() {
        return channels;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetChannels(DynamicValue newChannels, NotificationChain msgs) {
        DynamicValue oldChannels = channels;
        channels = newChannels;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_AVAILABLE_CHANNEL__CHANNELS, oldChannels, newChannels);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setChannels(DynamicValue newChannels) {
        if (newChannels != channels) {
            NotificationChain msgs = null;
            if (channels != null) msgs = ((InternalEObject) channels).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_AVAILABLE_CHANNEL__CHANNELS, null, msgs);
            if (newChannels != null) msgs = ((InternalEObject) newChannels).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_AVAILABLE_CHANNEL__CHANNELS, null, msgs);
            msgs = basicSetChannels(newChannels, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_AVAILABLE_CHANNEL__CHANNELS, newChannels, newChannels));
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
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_AVAILABLE_CHANNEL__VARIABLE_NAME, oldVariableName, newVariableName);
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
            if (variableName != null) msgs = ((InternalEObject) variableName).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_AVAILABLE_CHANNEL__VARIABLE_NAME, null, msgs);
            if (newVariableName != null) msgs = ((InternalEObject) newVariableName).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_AVAILABLE_CHANNEL__VARIABLE_NAME, null, msgs);
            msgs = basicSetVariableName(newVariableName, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_AVAILABLE_CHANNEL__VARIABLE_NAME, newVariableName, newVariableName));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isIgnoreInUse() {
        return ignoreInUse;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setIgnoreInUse(boolean newIgnoreInUse) {
        boolean oldIgnoreInUse = ignoreInUse;
        ignoreInUse = newIgnoreInUse;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_AVAILABLE_CHANNEL__IGNORE_IN_USE, oldIgnoreInUse, ignoreInUse));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isJumpPriorityOnFail() {
        return jumpPriorityOnFail;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setJumpPriorityOnFail(boolean newJumpPriorityOnFail) {
        boolean oldJumpPriorityOnFail = jumpPriorityOnFail;
        jumpPriorityOnFail = newJumpPriorityOnFail;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_AVAILABLE_CHANNEL__JUMP_PRIORITY_ON_FAIL, oldJumpPriorityOnFail, jumpPriorityOnFail));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__CHANNELS:
                return basicSetChannels(null, msgs);
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__VARIABLE_NAME:
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
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__CHANNELS:
                return getChannels();
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__VARIABLE_NAME:
                return getVariableName();
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__IGNORE_IN_USE:
                return isIgnoreInUse();
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__JUMP_PRIORITY_ON_FAIL:
                return isJumpPriorityOnFail();
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
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__CHANNELS:
                setChannels((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__VARIABLE_NAME:
                setVariableName((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__IGNORE_IN_USE:
                setIgnoreInUse((Boolean) newValue);
                return;
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__JUMP_PRIORITY_ON_FAIL:
                setJumpPriorityOnFail((Boolean) newValue);
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
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__CHANNELS:
                setChannels((DynamicValue) null);
                return;
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__VARIABLE_NAME:
                setVariableName((DynamicValue) null);
                return;
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__IGNORE_IN_USE:
                setIgnoreInUse(IGNORE_IN_USE_EDEFAULT);
                return;
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__JUMP_PRIORITY_ON_FAIL:
                setJumpPriorityOnFail(JUMP_PRIORITY_ON_FAIL_EDEFAULT);
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
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__CALL1:
                return call1 != null;
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__CHANNELS:
                return channels != null;
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__VARIABLE_NAME:
                return variableName != null;
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__IGNORE_IN_USE:
                return ignoreInUse != IGNORE_IN_USE_EDEFAULT;
            case ActionstepPackage.GET_AVAILABLE_CHANNEL__JUMP_PRIORITY_ON_FAIL:
                return jumpPriorityOnFail != JUMP_PRIORITY_ON_FAIL_EDEFAULT;
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
                case ActionstepPackage.GET_AVAILABLE_CHANNEL__CALL1:
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
                    return ActionstepPackage.GET_AVAILABLE_CHANNEL__CALL1;
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
        result.append(" (ignoreInUse: ");
        result.append(ignoreInUse);
        result.append(", jumpPriorityOnFail: ");
        result.append(jumpPriorityOnFail);
        result.append(')');
        return result.toString();
    }
}
