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
import com.safi.asterisk.actionstep.GetFullVariable;
import com.safi.core.actionstep.ActionStepException;
import com.safi.core.actionstep.DynamicValue;
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
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Get Full Variable</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetFullVariableImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetFullVariableImpl#getVariable <em>Variable</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetFullVariableImpl#getAssignToVar <em>Assign To Var</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class GetFullVariableImpl extends AsteriskActionStepImpl implements GetFullVariable {

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
	 * The default value of the '{@link #getVariable() <em>Variable</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getVariable()
	 * @generated
	 * @ordered
	 */
    protected static final String VARIABLE_EDEFAULT = null;

    /**
	 * The cached value of the '{@link #getVariable() <em>Variable</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getVariable()
	 * @generated
	 * @ordered
	 */
    protected String variable = VARIABLE_EDEFAULT;

    /**
	 * The cached value of the '{@link #getAssignToVar() <em>Assign To Var</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getAssignToVar()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue assignToVar;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected GetFullVariableImpl() {
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
        try {
            String astVar = variable;
            if (StringUtils.isNotBlank(astVar)) {
                if (!astVar.startsWith("${")) {
                    astVar = "${" + astVar;
                }
                if (!astVar.endsWith("}")) astVar += "}";
            }
            String value = channel.getFullVariable(astVar);
            Variable v = resolveVariableFromName(assignToVar, context);
            if (debugLog.isLoggable(Level.FINEST)) {
                debug("Got value " + value + " of variable " + astVar + ". Assigning to " + (v == null ? "null" : v.getName()));
            }
            if (v.getScope() != VariableScope.GLOBAL) context.setVariableRawValue(v.getName(), VariableTranslator.translateValue(v.getType(), value)); else {
                SafletEnvironment env = getSaflet().getSafletEnvironment();
                env.setGlobalVariableValue(v.getName(), VariableTranslator.translateValue(v.getType(), value));
            }
        } catch (Exception e) {
            handleException(context, e);
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
        return ActionstepPackage.Literals.GET_FULL_VARIABLE;
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.GET_FULL_VARIABLE__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_FULL_VARIABLE__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public String getVariable() {
        return variable;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setVariable(String newVariable) {
        String oldVariable = variable;
        variable = newVariable;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_FULL_VARIABLE__VARIABLE, oldVariable, variable));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getAssignToVar() {
        return assignToVar;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetAssignToVar(DynamicValue newAssignToVar, NotificationChain msgs) {
        DynamicValue oldAssignToVar = assignToVar;
        assignToVar = newAssignToVar;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_FULL_VARIABLE__ASSIGN_TO_VAR, oldAssignToVar, newAssignToVar);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setAssignToVar(DynamicValue newAssignToVar) {
        if (newAssignToVar != assignToVar) {
            NotificationChain msgs = null;
            if (assignToVar != null) msgs = ((InternalEObject) assignToVar).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_FULL_VARIABLE__ASSIGN_TO_VAR, null, msgs);
            if (newAssignToVar != null) msgs = ((InternalEObject) newAssignToVar).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_FULL_VARIABLE__ASSIGN_TO_VAR, null, msgs);
            msgs = basicSetAssignToVar(newAssignToVar, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_FULL_VARIABLE__ASSIGN_TO_VAR, newAssignToVar, newAssignToVar));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.GET_FULL_VARIABLE__ASSIGN_TO_VAR:
                return basicSetAssignToVar(null, msgs);
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
            case ActionstepPackage.GET_FULL_VARIABLE__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.GET_FULL_VARIABLE__VARIABLE:
                return getVariable();
            case ActionstepPackage.GET_FULL_VARIABLE__ASSIGN_TO_VAR:
                return getAssignToVar();
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
            case ActionstepPackage.GET_FULL_VARIABLE__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.GET_FULL_VARIABLE__VARIABLE:
                setVariable((String) newValue);
                return;
            case ActionstepPackage.GET_FULL_VARIABLE__ASSIGN_TO_VAR:
                setAssignToVar((DynamicValue) newValue);
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
            case ActionstepPackage.GET_FULL_VARIABLE__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.GET_FULL_VARIABLE__VARIABLE:
                setVariable(VARIABLE_EDEFAULT);
                return;
            case ActionstepPackage.GET_FULL_VARIABLE__ASSIGN_TO_VAR:
                setAssignToVar((DynamicValue) null);
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
            case ActionstepPackage.GET_FULL_VARIABLE__CALL1:
                return call1 != null;
            case ActionstepPackage.GET_FULL_VARIABLE__VARIABLE:
                return VARIABLE_EDEFAULT == null ? variable != null : !VARIABLE_EDEFAULT.equals(variable);
            case ActionstepPackage.GET_FULL_VARIABLE__ASSIGN_TO_VAR:
                return assignToVar != null;
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
                case ActionstepPackage.GET_FULL_VARIABLE__CALL1:
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
                    return ActionstepPackage.GET_FULL_VARIABLE__CALL1;
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
        result.append(" (variable: ");
        result.append(variable);
        result.append(')');
        return result.toString();
    }
}
