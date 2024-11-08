package com.safi.asterisk.actionstep.impl;

import java.util.logging.Level;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.action.MonitorAction;
import org.asteriskjava.manager.response.ManagerError;
import org.asteriskjava.manager.response.ManagerResponse;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import com.safi.asterisk.Call;
import com.safi.asterisk.actionstep.ActionstepPackage;
import com.safi.asterisk.actionstep.Monitor;
import com.safi.asterisk.util.AsteriskSafletConstants;
import com.safi.core.actionstep.ActionStepException;
import com.safi.core.actionstep.DynamicValue;
import com.safi.core.actionstep.impl.ActionStepImpl;
import com.safi.core.call.CallConsumer1;
import com.safi.core.call.CallPackage;
import com.safi.core.call.SafiCall;
import com.safi.core.saflet.Saflet;
import com.safi.core.saflet.SafletContext;
import com.safi.db.VariableType;
import com.safi.db.util.VariableTranslator;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Monitor</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.MonitorImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.MonitorImpl#getFilenamePrefix <em>Filename Prefix</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.MonitorImpl#getFormat <em>Format</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.MonitorImpl#isMix <em>Mix</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class MonitorImpl extends AsteriskActionStepImpl implements Monitor {

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
	 * The cached value of the '{@link #getFilenamePrefix() <em>Filename Prefix</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getFilenamePrefix()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue filenamePrefix;

    /**
	 * The default value of the '{@link #getFormat() <em>Format</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getFormat()
	 * @generated
	 * @ordered
	 */
    protected static final String FORMAT_EDEFAULT = "wav";

    /**
	 * The cached value of the '{@link #getFormat() <em>Format</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getFormat()
	 * @generated
	 * @ordered
	 */
    protected String format = FORMAT_EDEFAULT;

    /**
	 * The default value of the '{@link #isMix() <em>Mix</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isMix()
	 * @generated
	 * @ordered
	 */
    protected static final boolean MIX_EDEFAULT = false;

    /**
	 * The cached value of the '{@link #isMix() <em>Mix</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isMix()
	 * @generated
	 * @ordered
	 */
    protected boolean mix = MIX_EDEFAULT;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected MonitorImpl() {
        super();
    }

    @Override
    public void beginProcessing(SafletContext context) throws ActionStepException {
        super.beginProcessing(context);
        Object variableRawValue = context.getVariableRawValue(AsteriskSafletConstants.VAR_KEY_MANAGER_CONNECTION);
        if (variableRawValue == null || !(variableRawValue instanceof ManagerConnection)) {
            handleException(context, new ActionStepException("No manager connection found in current context"));
            return;
        }
        ManagerConnection connection = (ManagerConnection) variableRawValue;
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
        Exception exception = null;
        try {
            MonitorAction action = new MonitorAction();
            String filename = (String) VariableTranslator.translateValue(VariableType.TEXT, resolveDynamicValue(filenamePrefix, context));
            if (debugLog.isLoggable(Level.FINEST)) debug("Monitor recording to filename with prefix: " + filename);
            action.setFile(filename);
            action.setFormat(format);
            action.setChannel(((Call) call1).getChannelName());
            action.setMix(mix);
            ManagerResponse response = connection.sendAction(action, Saflet.DEFAULT_MANAGER_ACTION_TIMEOUT);
            if (debugLog.isLoggable(Level.FINEST)) debug("Monitor returned " + response.getMessage() + " of type " + response.getResponse());
            if (response instanceof ManagerError) exception = new ActionStepException("Couldn't monitor channel: " + response.getMessage());
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
        return ActionstepPackage.Literals.MONITOR;
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.MONITOR__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.MONITOR__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getFilenamePrefix() {
        return filenamePrefix;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetFilenamePrefix(DynamicValue newFilenamePrefix, NotificationChain msgs) {
        DynamicValue oldFilenamePrefix = filenamePrefix;
        filenamePrefix = newFilenamePrefix;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.MONITOR__FILENAME_PREFIX, oldFilenamePrefix, newFilenamePrefix);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setFilenamePrefix(DynamicValue newFilenamePrefix) {
        if (newFilenamePrefix != filenamePrefix) {
            NotificationChain msgs = null;
            if (filenamePrefix != null) msgs = ((InternalEObject) filenamePrefix).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.MONITOR__FILENAME_PREFIX, null, msgs);
            if (newFilenamePrefix != null) msgs = ((InternalEObject) newFilenamePrefix).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.MONITOR__FILENAME_PREFIX, null, msgs);
            msgs = basicSetFilenamePrefix(newFilenamePrefix, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.MONITOR__FILENAME_PREFIX, newFilenamePrefix, newFilenamePrefix));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public String getFormat() {
        return format;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setFormat(String newFormat) {
        String oldFormat = format;
        format = newFormat;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.MONITOR__FORMAT, oldFormat, format));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isMix() {
        return mix;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setMix(boolean newMix) {
        boolean oldMix = mix;
        mix = newMix;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.MONITOR__MIX, oldMix, mix));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.MONITOR__FILENAME_PREFIX:
                return basicSetFilenamePrefix(null, msgs);
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
            case ActionstepPackage.MONITOR__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.MONITOR__FILENAME_PREFIX:
                return getFilenamePrefix();
            case ActionstepPackage.MONITOR__FORMAT:
                return getFormat();
            case ActionstepPackage.MONITOR__MIX:
                return isMix();
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
            case ActionstepPackage.MONITOR__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.MONITOR__FILENAME_PREFIX:
                setFilenamePrefix((DynamicValue) newValue);
                return;
            case ActionstepPackage.MONITOR__FORMAT:
                setFormat((String) newValue);
                return;
            case ActionstepPackage.MONITOR__MIX:
                setMix((Boolean) newValue);
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
            case ActionstepPackage.MONITOR__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.MONITOR__FILENAME_PREFIX:
                setFilenamePrefix((DynamicValue) null);
                return;
            case ActionstepPackage.MONITOR__FORMAT:
                setFormat(FORMAT_EDEFAULT);
                return;
            case ActionstepPackage.MONITOR__MIX:
                setMix(MIX_EDEFAULT);
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
            case ActionstepPackage.MONITOR__CALL1:
                return call1 != null;
            case ActionstepPackage.MONITOR__FILENAME_PREFIX:
                return filenamePrefix != null;
            case ActionstepPackage.MONITOR__FORMAT:
                return FORMAT_EDEFAULT == null ? format != null : !FORMAT_EDEFAULT.equals(format);
            case ActionstepPackage.MONITOR__MIX:
                return mix != MIX_EDEFAULT;
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
                case ActionstepPackage.MONITOR__CALL1:
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
                    return ActionstepPackage.MONITOR__CALL1;
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
        result.append(" (format: ");
        result.append(format);
        result.append(", mix: ");
        result.append(mix);
        result.append(')');
        return result.toString();
    }
}
