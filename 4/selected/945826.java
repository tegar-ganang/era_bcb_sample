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
import com.safi.asterisk.actionstep.ExecuteApplication;
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
 * An implementation of the model object '<em><b>Execute Application</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.ExecuteApplicationImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.ExecuteApplicationImpl#getArguments <em>Arguments</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.ExecuteApplicationImpl#getApplication <em>Application</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ExecuteApplicationImpl extends AsteriskActionStepImpl implements ExecuteApplication {

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
	 * The cached value of the '{@link #getArguments() <em>Arguments</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getArguments()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue arguments;

    /**
	 * The cached value of the '{@link #getApplication() <em>Application</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getApplication()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue application;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected ExecuteApplicationImpl() {
        super();
    }

    @Override
    public void beginProcessing(SafletContext context) throws ActionStepException {
        super.beginProcessing(context);
        Exception exception = null;
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
            Object dynValue = resolveDynamicValue(arguments, context);
            String args = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
            if (debugLog.isLoggable(Level.FINEST)) debug("Asterisk application is " + application + " with args " + args);
            Object appDynValue = resolveDynamicValue(application, context);
            String appname = (String) VariableTranslator.translateValue(VariableType.TEXT, appDynValue);
            if (StringUtils.isBlank(appname)) {
                exception = new ActionStepException("Asterisk Application was not specified!");
            } else {
                int result = channel.exec(appname, args);
                if (debugLog.isLoggable(Level.FINEST)) debug(application + " returned " + result);
                if (result == -2) exception = new ActionStepException("Asterisk Application " + application + " was not found!");
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
        return ActionstepPackage.Literals.EXECUTE_APPLICATION;
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.EXECUTE_APPLICATION__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.EXECUTE_APPLICATION__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getApplication() {
        return application;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetApplication(DynamicValue newApplication, NotificationChain msgs) {
        DynamicValue oldApplication = application;
        application = newApplication;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.EXECUTE_APPLICATION__APPLICATION, oldApplication, newApplication);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public void setApplication(DynamicValue newApplication) {
        if (newApplication != application) {
            NotificationChain msgs = null;
            if (application != null) msgs = ((InternalEObject) application).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.EXECUTE_APPLICATION__APPLICATION, null, msgs);
            if (newApplication != null) msgs = ((InternalEObject) newApplication).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.EXECUTE_APPLICATION__APPLICATION, null, msgs);
            msgs = basicSetApplication(newApplication, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.EXECUTE_APPLICATION__APPLICATION, newApplication, newApplication));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getArguments() {
        return arguments;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetArguments(DynamicValue newArguments, NotificationChain msgs) {
        DynamicValue oldArguments = arguments;
        arguments = newArguments;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.EXECUTE_APPLICATION__ARGUMENTS, oldArguments, newArguments);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setArguments(DynamicValue newArguments) {
        if (newArguments != arguments) {
            NotificationChain msgs = null;
            if (arguments != null) msgs = ((InternalEObject) arguments).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.EXECUTE_APPLICATION__ARGUMENTS, null, msgs);
            if (newArguments != null) msgs = ((InternalEObject) newArguments).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.EXECUTE_APPLICATION__ARGUMENTS, null, msgs);
            msgs = basicSetArguments(newArguments, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.EXECUTE_APPLICATION__ARGUMENTS, newArguments, newArguments));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.EXECUTE_APPLICATION__ARGUMENTS:
                return basicSetArguments(null, msgs);
            case ActionstepPackage.EXECUTE_APPLICATION__APPLICATION:
                return basicSetApplication(null, msgs);
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
            case ActionstepPackage.EXECUTE_APPLICATION__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.EXECUTE_APPLICATION__ARGUMENTS:
                return getArguments();
            case ActionstepPackage.EXECUTE_APPLICATION__APPLICATION:
                return getApplication();
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
            case ActionstepPackage.EXECUTE_APPLICATION__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.EXECUTE_APPLICATION__ARGUMENTS:
                setArguments((DynamicValue) newValue);
                return;
            case ActionstepPackage.EXECUTE_APPLICATION__APPLICATION:
                setApplication((DynamicValue) newValue);
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
            case ActionstepPackage.EXECUTE_APPLICATION__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.EXECUTE_APPLICATION__ARGUMENTS:
                setArguments((DynamicValue) null);
                return;
            case ActionstepPackage.EXECUTE_APPLICATION__APPLICATION:
                setApplication((DynamicValue) null);
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
            case ActionstepPackage.EXECUTE_APPLICATION__CALL1:
                return call1 != null;
            case ActionstepPackage.EXECUTE_APPLICATION__ARGUMENTS:
                return arguments != null;
            case ActionstepPackage.EXECUTE_APPLICATION__APPLICATION:
                return application != null;
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
                case ActionstepPackage.EXECUTE_APPLICATION__CALL1:
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
                    return ActionstepPackage.EXECUTE_APPLICATION__CALL1;
                default:
                    return -1;
            }
        }
        return super.eDerivedStructuralFeatureID(baseFeatureID, baseClass);
    }
}
