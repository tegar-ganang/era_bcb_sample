package com.safi.asterisk.actionstep.impl;

import java.util.logging.Level;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiRequest;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import com.safi.asterisk.Call;
import com.safi.asterisk.actionstep.ActionstepPackage;
import com.safi.asterisk.actionstep.GetCallInfo;
import com.safi.asterisk.util.AsteriskSafletConstants;
import com.safi.core.actionstep.ActionStepException;
import com.safi.core.actionstep.DynamicValue;
import com.safi.core.actionstep.impl.ActionStepImpl;
import com.safi.core.call.CallConsumer1;
import com.safi.core.call.CallPackage;
import com.safi.core.call.SafiCall;
import com.safi.core.saflet.SafletContext;
import com.safi.core.saflet.SafletEnvironment;
import com.safi.core.saflet.SafletException;
import com.safi.db.Variable;
import com.safi.db.VariableScope;
import com.safi.db.util.VariableTranslator;

/**
 * <!-- begin-user-doc --> An implementation of the model object '
 * <em><b>Get Call Info</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getAccountCodeVar <em>Account Code Var</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getCallerIdNameVar <em>Caller Id Name Var</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getCallerIdNumVar <em>Caller Id Num Var</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getChannelNameVar <em>Channel Name Var</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getContextVar <em>Context Var</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getExtensionVar <em>Extension Var</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getDialedNumber <em>Dialed Number</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getPriorityVar <em>Priority Var</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getStateVar <em>State Var</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getUniqueIdVar <em>Unique Id Var</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getAni2Var <em>Ani2 Var</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getRdnis <em>Rdnis</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.GetCallInfoImpl#getType <em>Type</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class GetCallInfoImpl extends AsteriskActionStepImpl implements GetCallInfo {

    /**
   * The cached value of the '{@link #getCall1() <em>Call1</em>}' reference. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getCall1()
   * @generated
   * @ordered
   */
    protected SafiCall call1;

    /**
	 * The cached value of the '{@link #getAccountCodeVar() <em>Account Code Var</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getAccountCodeVar()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue accountCodeVar;

    /**
	 * The cached value of the '{@link #getCallerIdNameVar() <em>Caller Id Name Var</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getCallerIdNameVar()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue callerIdNameVar;

    /**
	 * The cached value of the '{@link #getCallerIdNumVar() <em>Caller Id Num Var</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getCallerIdNumVar()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue callerIdNumVar;

    /**
	 * The cached value of the '{@link #getChannelNameVar() <em>Channel Name Var</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getChannelNameVar()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue channelNameVar;

    /**
	 * The cached value of the '{@link #getContextVar() <em>Context Var</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getContextVar()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue contextVar;

    /**
	 * The cached value of the '{@link #getExtensionVar() <em>Extension Var</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getExtensionVar()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue extensionVar;

    /**
	 * The cached value of the '{@link #getDialedNumber() <em>Dialed Number</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getDialedNumber()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue dialedNumber;

    /**
	 * The cached value of the '{@link #getPriorityVar() <em>Priority Var</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getPriorityVar()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue priorityVar;

    /**
	 * The cached value of the '{@link #getStateVar() <em>State Var</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getStateVar()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue stateVar;

    /**
	 * The cached value of the '{@link #getUniqueIdVar() <em>Unique Id Var</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getUniqueIdVar()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue uniqueIdVar;

    /**
	 * The cached value of the '{@link #getAni2Var() <em>Ani2 Var</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getAni2Var()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue ani2Var;

    /**
	 * The cached value of the '{@link #getRdnis() <em>Rdnis</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getRdnis()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue rdnis;

    /**
	 * The cached value of the '{@link #getType() <em>Type</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue type;

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    protected GetCallInfoImpl() {
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
            AgiRequest request = null;
            try {
                final Object val = context.getVariableRawValue(AsteriskSafletConstants.VAR_KEY_REQUEST);
                if (val != null && val instanceof AgiRequest) request = (AgiRequest) val;
            } catch (Exception e) {
            }
            if (accountCodeVar != null) {
                if (request == null) throw new SafletException("No AgiRequest object available for current context");
                try {
                    Object result = request.getAccountCode();
                    setVariableValue(accountCodeVar, result, context);
                } catch (Exception e) {
                    if (debugLog.isLoggable(Level.FINEST)) debugLog.log(Level.SEVERE, e.getLocalizedMessage());
                    exception = e;
                }
            }
            if (callerIdNameVar != null) try {
                Object result = ((Call) call1).getCallerIdName();
                setVariableValue(callerIdNameVar, result, context);
            } catch (Exception e) {
                if (debugLog.isLoggable(Level.FINEST)) debugLog.log(Level.SEVERE, e.getLocalizedMessage());
                exception = e;
            }
            if (callerIdNumVar != null) try {
                Object result = ((Call) call1).getCallerIdNum();
                setVariableValue(callerIdNumVar, result, context);
            } catch (Exception e) {
                if (debugLog.isLoggable(Level.FINEST)) debugLog.log(Level.SEVERE, e.getLocalizedMessage());
                exception = e;
            }
            if (channelNameVar != null) try {
                Object result = ((Call) call1).getChannelName();
                setVariableValue(channelNameVar, result, context);
            } catch (Exception e) {
                if (debugLog.isLoggable(Level.FINEST)) debugLog.log(Level.SEVERE, e.getLocalizedMessage());
                exception = e;
            }
            if (contextVar != null) {
                if (request == null) throw new SafletException("No AgiRequest object available for current context");
                try {
                    Object result = request.getContext();
                    setVariableValue(contextVar, result, context);
                } catch (Exception e) {
                    if (debugLog.isLoggable(Level.FINEST)) debugLog.log(Level.SEVERE, e.getLocalizedMessage());
                    exception = e;
                }
            }
            if (extensionVar != null) {
                if (request == null) throw new SafletException("No AgiRequest object available for current context");
                try {
                    Object result = request.getExtension();
                    setVariableValue(extensionVar, result, context);
                } catch (Exception e) {
                    if (debugLog.isLoggable(Level.FINEST)) debugLog.log(Level.SEVERE, e.getLocalizedMessage());
                    exception = e;
                }
            }
            if (dialedNumber != null) {
                if (request == null) throw new SafletException("No AgiRequest object available for current context");
                try {
                    Object result = request.getDnid();
                    setVariableValue(dialedNumber, result, context);
                } catch (Exception e) {
                    if (debugLog.isLoggable(Level.FINEST)) debugLog.log(Level.SEVERE, e.getLocalizedMessage());
                    exception = e;
                }
            }
            if (priorityVar != null) {
                if (request == null) throw new SafletException("No AgiRequest object available for current context");
                try {
                    Object result = request.getPriority();
                    setVariableValue(priorityVar, result, context);
                } catch (Exception e) {
                    if (debugLog.isLoggable(Level.FINEST)) debugLog.log(Level.SEVERE, e.getLocalizedMessage());
                    exception = e;
                }
            }
            if (stateVar != null) try {
                Object result = ((Call) call1).getChannel().getChannelStatus();
                setVariableValue(stateVar, result, context);
            } catch (Exception e) {
                if (debugLog.isLoggable(Level.FINEST)) debugLog.log(Level.SEVERE, e.getLocalizedMessage());
                exception = e;
            }
            if (uniqueIdVar != null) try {
                Object result = ((Call) call1).getUniqueId();
                setVariableValue(uniqueIdVar, result, context);
            } catch (Exception e) {
                if (debugLog.isLoggable(Level.FINEST)) debugLog.log(Level.SEVERE, e.getLocalizedMessage());
                exception = e;
            }
            if (rdnis != null) {
                if (request == null) throw new SafletException("No AgiRequest object available for current context");
                try {
                    Object result = request.getRdnis();
                    setVariableValue(rdnis, result, context);
                } catch (Exception e) {
                    if (debugLog.isLoggable(Level.FINEST)) debugLog.log(Level.SEVERE, e.getLocalizedMessage());
                    exception = e;
                }
            }
            if (ani2Var != null) {
                if (request == null) throw new SafletException("No AgiRequest object available for current context");
                try {
                    Object result = request.getCallingAni2();
                    setVariableValue(ani2Var, result, context);
                } catch (Exception e) {
                    if (debugLog.isLoggable(Level.FINEST)) debugLog.log(Level.SEVERE, e.getLocalizedMessage());
                    exception = e;
                }
            }
            if (type != null) {
                if (request == null) throw new SafletException("No AgiRequest object available for current context");
                try {
                    Object result = request.getType();
                    setVariableValue(type, result, context);
                } catch (Exception e) {
                    if (debugLog.isLoggable(Level.FINEST)) debugLog.log(Level.SEVERE, e.getLocalizedMessage());
                    exception = e;
                }
            }
        } catch (Exception e) {
            handleException(context, e);
            return;
        }
        handleSuccess(context);
    }

    private void setVariableValue(DynamicValue varName, Object result, SafletContext context) throws ActionStepException {
        Variable v = resolveVariableFromName(varName, context);
        if (v != null) {
            if (v.getScope() != VariableScope.GLOBAL) context.setVariableRawValue(v.getName(), VariableTranslator.translateValue(v.getType(), result)); else {
                SafletEnvironment env = getSaflet().getSafletEnvironment();
                env.setGlobalVariableValue(v.getName(), VariableTranslator.translateValue(v.getType(), result));
            }
        }
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return ActionstepPackage.Literals.GET_CALL_INFO;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public SafiCall getCall1() {
        if (call1 != null && call1.eIsProxy()) {
            InternalEObject oldCall1 = (InternalEObject) call1;
            call1 = (SafiCall) eResolveProxy(oldCall1);
            if (call1 != oldCall1) {
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.GET_CALL_INFO__CALL1, oldCall1, call1));
            }
        }
        return call1;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getAccountCodeVar() {
        return accountCodeVar;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetAccountCodeVar(DynamicValue newAccountCodeVar, NotificationChain msgs) {
        DynamicValue oldAccountCodeVar = accountCodeVar;
        accountCodeVar = newAccountCodeVar;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__ACCOUNT_CODE_VAR, oldAccountCodeVar, newAccountCodeVar);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setAccountCodeVar(DynamicValue newAccountCodeVar) {
        if (newAccountCodeVar != accountCodeVar) {
            NotificationChain msgs = null;
            if (accountCodeVar != null) msgs = ((InternalEObject) accountCodeVar).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__ACCOUNT_CODE_VAR, null, msgs);
            if (newAccountCodeVar != null) msgs = ((InternalEObject) newAccountCodeVar).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__ACCOUNT_CODE_VAR, null, msgs);
            msgs = basicSetAccountCodeVar(newAccountCodeVar, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__ACCOUNT_CODE_VAR, newAccountCodeVar, newAccountCodeVar));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getCallerIdNameVar() {
        return callerIdNameVar;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetCallerIdNameVar(DynamicValue newCallerIdNameVar, NotificationChain msgs) {
        DynamicValue oldCallerIdNameVar = callerIdNameVar;
        callerIdNameVar = newCallerIdNameVar;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__CALLER_ID_NAME_VAR, oldCallerIdNameVar, newCallerIdNameVar);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setCallerIdNameVar(DynamicValue newCallerIdNameVar) {
        if (newCallerIdNameVar != callerIdNameVar) {
            NotificationChain msgs = null;
            if (callerIdNameVar != null) msgs = ((InternalEObject) callerIdNameVar).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__CALLER_ID_NAME_VAR, null, msgs);
            if (newCallerIdNameVar != null) msgs = ((InternalEObject) newCallerIdNameVar).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__CALLER_ID_NAME_VAR, null, msgs);
            msgs = basicSetCallerIdNameVar(newCallerIdNameVar, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__CALLER_ID_NAME_VAR, newCallerIdNameVar, newCallerIdNameVar));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getCallerIdNumVar() {
        return callerIdNumVar;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetCallerIdNumVar(DynamicValue newCallerIdNumVar, NotificationChain msgs) {
        DynamicValue oldCallerIdNumVar = callerIdNumVar;
        callerIdNumVar = newCallerIdNumVar;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__CALLER_ID_NUM_VAR, oldCallerIdNumVar, newCallerIdNumVar);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setCallerIdNumVar(DynamicValue newCallerIdNumVar) {
        if (newCallerIdNumVar != callerIdNumVar) {
            NotificationChain msgs = null;
            if (callerIdNumVar != null) msgs = ((InternalEObject) callerIdNumVar).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__CALLER_ID_NUM_VAR, null, msgs);
            if (newCallerIdNumVar != null) msgs = ((InternalEObject) newCallerIdNumVar).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__CALLER_ID_NUM_VAR, null, msgs);
            msgs = basicSetCallerIdNumVar(newCallerIdNumVar, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__CALLER_ID_NUM_VAR, newCallerIdNumVar, newCallerIdNumVar));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getChannelNameVar() {
        return channelNameVar;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetChannelNameVar(DynamicValue newChannelNameVar, NotificationChain msgs) {
        DynamicValue oldChannelNameVar = channelNameVar;
        channelNameVar = newChannelNameVar;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__CHANNEL_NAME_VAR, oldChannelNameVar, newChannelNameVar);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setChannelNameVar(DynamicValue newChannelNameVar) {
        if (newChannelNameVar != channelNameVar) {
            NotificationChain msgs = null;
            if (channelNameVar != null) msgs = ((InternalEObject) channelNameVar).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__CHANNEL_NAME_VAR, null, msgs);
            if (newChannelNameVar != null) msgs = ((InternalEObject) newChannelNameVar).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__CHANNEL_NAME_VAR, null, msgs);
            msgs = basicSetChannelNameVar(newChannelNameVar, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__CHANNEL_NAME_VAR, newChannelNameVar, newChannelNameVar));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getContextVar() {
        return contextVar;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetContextVar(DynamicValue newContextVar, NotificationChain msgs) {
        DynamicValue oldContextVar = contextVar;
        contextVar = newContextVar;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__CONTEXT_VAR, oldContextVar, newContextVar);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setContextVar(DynamicValue newContextVar) {
        if (newContextVar != contextVar) {
            NotificationChain msgs = null;
            if (contextVar != null) msgs = ((InternalEObject) contextVar).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__CONTEXT_VAR, null, msgs);
            if (newContextVar != null) msgs = ((InternalEObject) newContextVar).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__CONTEXT_VAR, null, msgs);
            msgs = basicSetContextVar(newContextVar, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__CONTEXT_VAR, newContextVar, newContextVar));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getExtensionVar() {
        return extensionVar;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetExtensionVar(DynamicValue newExtensionVar, NotificationChain msgs) {
        DynamicValue oldExtensionVar = extensionVar;
        extensionVar = newExtensionVar;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__EXTENSION_VAR, oldExtensionVar, newExtensionVar);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setExtensionVar(DynamicValue newExtensionVar) {
        if (newExtensionVar != extensionVar) {
            NotificationChain msgs = null;
            if (extensionVar != null) msgs = ((InternalEObject) extensionVar).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__EXTENSION_VAR, null, msgs);
            if (newExtensionVar != null) msgs = ((InternalEObject) newExtensionVar).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__EXTENSION_VAR, null, msgs);
            msgs = basicSetExtensionVar(newExtensionVar, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__EXTENSION_VAR, newExtensionVar, newExtensionVar));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getDialedNumber() {
        return dialedNumber;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetDialedNumber(DynamicValue newDialedNumber, NotificationChain msgs) {
        DynamicValue oldDialedNumber = dialedNumber;
        dialedNumber = newDialedNumber;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__DIALED_NUMBER, oldDialedNumber, newDialedNumber);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setDialedNumber(DynamicValue newDialedNumber) {
        if (newDialedNumber != dialedNumber) {
            NotificationChain msgs = null;
            if (dialedNumber != null) msgs = ((InternalEObject) dialedNumber).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__DIALED_NUMBER, null, msgs);
            if (newDialedNumber != null) msgs = ((InternalEObject) newDialedNumber).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__DIALED_NUMBER, null, msgs);
            msgs = basicSetDialedNumber(newDialedNumber, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__DIALED_NUMBER, newDialedNumber, newDialedNumber));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getPriorityVar() {
        return priorityVar;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetPriorityVar(DynamicValue newPriorityVar, NotificationChain msgs) {
        DynamicValue oldPriorityVar = priorityVar;
        priorityVar = newPriorityVar;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__PRIORITY_VAR, oldPriorityVar, newPriorityVar);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setPriorityVar(DynamicValue newPriorityVar) {
        if (newPriorityVar != priorityVar) {
            NotificationChain msgs = null;
            if (priorityVar != null) msgs = ((InternalEObject) priorityVar).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__PRIORITY_VAR, null, msgs);
            if (newPriorityVar != null) msgs = ((InternalEObject) newPriorityVar).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__PRIORITY_VAR, null, msgs);
            msgs = basicSetPriorityVar(newPriorityVar, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__PRIORITY_VAR, newPriorityVar, newPriorityVar));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getStateVar() {
        return stateVar;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetStateVar(DynamicValue newStateVar, NotificationChain msgs) {
        DynamicValue oldStateVar = stateVar;
        stateVar = newStateVar;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__STATE_VAR, oldStateVar, newStateVar);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setStateVar(DynamicValue newStateVar) {
        if (newStateVar != stateVar) {
            NotificationChain msgs = null;
            if (stateVar != null) msgs = ((InternalEObject) stateVar).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__STATE_VAR, null, msgs);
            if (newStateVar != null) msgs = ((InternalEObject) newStateVar).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__STATE_VAR, null, msgs);
            msgs = basicSetStateVar(newStateVar, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__STATE_VAR, newStateVar, newStateVar));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getUniqueIdVar() {
        return uniqueIdVar;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetUniqueIdVar(DynamicValue newUniqueIdVar, NotificationChain msgs) {
        DynamicValue oldUniqueIdVar = uniqueIdVar;
        uniqueIdVar = newUniqueIdVar;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__UNIQUE_ID_VAR, oldUniqueIdVar, newUniqueIdVar);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setUniqueIdVar(DynamicValue newUniqueIdVar) {
        if (newUniqueIdVar != uniqueIdVar) {
            NotificationChain msgs = null;
            if (uniqueIdVar != null) msgs = ((InternalEObject) uniqueIdVar).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__UNIQUE_ID_VAR, null, msgs);
            if (newUniqueIdVar != null) msgs = ((InternalEObject) newUniqueIdVar).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__UNIQUE_ID_VAR, null, msgs);
            msgs = basicSetUniqueIdVar(newUniqueIdVar, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__UNIQUE_ID_VAR, newUniqueIdVar, newUniqueIdVar));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getAni2Var() {
        return ani2Var;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetAni2Var(DynamicValue newAni2Var, NotificationChain msgs) {
        DynamicValue oldAni2Var = ani2Var;
        ani2Var = newAni2Var;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__ANI2_VAR, oldAni2Var, newAni2Var);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setAni2Var(DynamicValue newAni2Var) {
        if (newAni2Var != ani2Var) {
            NotificationChain msgs = null;
            if (ani2Var != null) msgs = ((InternalEObject) ani2Var).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__ANI2_VAR, null, msgs);
            if (newAni2Var != null) msgs = ((InternalEObject) newAni2Var).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__ANI2_VAR, null, msgs);
            msgs = basicSetAni2Var(newAni2Var, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__ANI2_VAR, newAni2Var, newAni2Var));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getRdnis() {
        return rdnis;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetRdnis(DynamicValue newRdnis, NotificationChain msgs) {
        DynamicValue oldRdnis = rdnis;
        rdnis = newRdnis;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__RDNIS, oldRdnis, newRdnis);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setRdnis(DynamicValue newRdnis) {
        if (newRdnis != rdnis) {
            NotificationChain msgs = null;
            if (rdnis != null) msgs = ((InternalEObject) rdnis).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__RDNIS, null, msgs);
            if (newRdnis != null) msgs = ((InternalEObject) newRdnis).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__RDNIS, null, msgs);
            msgs = basicSetRdnis(newRdnis, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__RDNIS, newRdnis, newRdnis));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getType() {
        return type;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetType(DynamicValue newType, NotificationChain msgs) {
        DynamicValue oldType = type;
        type = newType;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__TYPE, oldType, newType);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setType(DynamicValue newType) {
        if (newType != type) {
            NotificationChain msgs = null;
            if (type != null) msgs = ((InternalEObject) type).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__TYPE, null, msgs);
            if (newType != null) msgs = ((InternalEObject) newType).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.GET_CALL_INFO__TYPE, null, msgs);
            msgs = basicSetType(newType, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.GET_CALL_INFO__TYPE, newType, newType));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.GET_CALL_INFO__ACCOUNT_CODE_VAR:
                return basicSetAccountCodeVar(null, msgs);
            case ActionstepPackage.GET_CALL_INFO__CALLER_ID_NAME_VAR:
                return basicSetCallerIdNameVar(null, msgs);
            case ActionstepPackage.GET_CALL_INFO__CALLER_ID_NUM_VAR:
                return basicSetCallerIdNumVar(null, msgs);
            case ActionstepPackage.GET_CALL_INFO__CHANNEL_NAME_VAR:
                return basicSetChannelNameVar(null, msgs);
            case ActionstepPackage.GET_CALL_INFO__CONTEXT_VAR:
                return basicSetContextVar(null, msgs);
            case ActionstepPackage.GET_CALL_INFO__EXTENSION_VAR:
                return basicSetExtensionVar(null, msgs);
            case ActionstepPackage.GET_CALL_INFO__DIALED_NUMBER:
                return basicSetDialedNumber(null, msgs);
            case ActionstepPackage.GET_CALL_INFO__PRIORITY_VAR:
                return basicSetPriorityVar(null, msgs);
            case ActionstepPackage.GET_CALL_INFO__STATE_VAR:
                return basicSetStateVar(null, msgs);
            case ActionstepPackage.GET_CALL_INFO__UNIQUE_ID_VAR:
                return basicSetUniqueIdVar(null, msgs);
            case ActionstepPackage.GET_CALL_INFO__ANI2_VAR:
                return basicSetAni2Var(null, msgs);
            case ActionstepPackage.GET_CALL_INFO__RDNIS:
                return basicSetRdnis(null, msgs);
            case ActionstepPackage.GET_CALL_INFO__TYPE:
                return basicSetType(null, msgs);
        }
        return super.eInverseRemove(otherEnd, featureID, msgs);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch(featureID) {
            case ActionstepPackage.GET_CALL_INFO__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.GET_CALL_INFO__ACCOUNT_CODE_VAR:
                return getAccountCodeVar();
            case ActionstepPackage.GET_CALL_INFO__CALLER_ID_NAME_VAR:
                return getCallerIdNameVar();
            case ActionstepPackage.GET_CALL_INFO__CALLER_ID_NUM_VAR:
                return getCallerIdNumVar();
            case ActionstepPackage.GET_CALL_INFO__CHANNEL_NAME_VAR:
                return getChannelNameVar();
            case ActionstepPackage.GET_CALL_INFO__CONTEXT_VAR:
                return getContextVar();
            case ActionstepPackage.GET_CALL_INFO__EXTENSION_VAR:
                return getExtensionVar();
            case ActionstepPackage.GET_CALL_INFO__DIALED_NUMBER:
                return getDialedNumber();
            case ActionstepPackage.GET_CALL_INFO__PRIORITY_VAR:
                return getPriorityVar();
            case ActionstepPackage.GET_CALL_INFO__STATE_VAR:
                return getStateVar();
            case ActionstepPackage.GET_CALL_INFO__UNIQUE_ID_VAR:
                return getUniqueIdVar();
            case ActionstepPackage.GET_CALL_INFO__ANI2_VAR:
                return getAni2Var();
            case ActionstepPackage.GET_CALL_INFO__RDNIS:
                return getRdnis();
            case ActionstepPackage.GET_CALL_INFO__TYPE:
                return getType();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    @SuppressWarnings("unchecked")
    @Override
    public void eSet(int featureID, Object newValue) {
        switch(featureID) {
            case ActionstepPackage.GET_CALL_INFO__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.GET_CALL_INFO__ACCOUNT_CODE_VAR:
                setAccountCodeVar((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_CALL_INFO__CALLER_ID_NAME_VAR:
                setCallerIdNameVar((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_CALL_INFO__CALLER_ID_NUM_VAR:
                setCallerIdNumVar((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_CALL_INFO__CHANNEL_NAME_VAR:
                setChannelNameVar((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_CALL_INFO__CONTEXT_VAR:
                setContextVar((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_CALL_INFO__EXTENSION_VAR:
                setExtensionVar((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_CALL_INFO__DIALED_NUMBER:
                setDialedNumber((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_CALL_INFO__PRIORITY_VAR:
                setPriorityVar((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_CALL_INFO__STATE_VAR:
                setStateVar((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_CALL_INFO__UNIQUE_ID_VAR:
                setUniqueIdVar((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_CALL_INFO__ANI2_VAR:
                setAni2Var((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_CALL_INFO__RDNIS:
                setRdnis((DynamicValue) newValue);
                return;
            case ActionstepPackage.GET_CALL_INFO__TYPE:
                setType((DynamicValue) newValue);
                return;
        }
        super.eSet(featureID, newValue);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public void eUnset(int featureID) {
        switch(featureID) {
            case ActionstepPackage.GET_CALL_INFO__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.GET_CALL_INFO__ACCOUNT_CODE_VAR:
                setAccountCodeVar((DynamicValue) null);
                return;
            case ActionstepPackage.GET_CALL_INFO__CALLER_ID_NAME_VAR:
                setCallerIdNameVar((DynamicValue) null);
                return;
            case ActionstepPackage.GET_CALL_INFO__CALLER_ID_NUM_VAR:
                setCallerIdNumVar((DynamicValue) null);
                return;
            case ActionstepPackage.GET_CALL_INFO__CHANNEL_NAME_VAR:
                setChannelNameVar((DynamicValue) null);
                return;
            case ActionstepPackage.GET_CALL_INFO__CONTEXT_VAR:
                setContextVar((DynamicValue) null);
                return;
            case ActionstepPackage.GET_CALL_INFO__EXTENSION_VAR:
                setExtensionVar((DynamicValue) null);
                return;
            case ActionstepPackage.GET_CALL_INFO__DIALED_NUMBER:
                setDialedNumber((DynamicValue) null);
                return;
            case ActionstepPackage.GET_CALL_INFO__PRIORITY_VAR:
                setPriorityVar((DynamicValue) null);
                return;
            case ActionstepPackage.GET_CALL_INFO__STATE_VAR:
                setStateVar((DynamicValue) null);
                return;
            case ActionstepPackage.GET_CALL_INFO__UNIQUE_ID_VAR:
                setUniqueIdVar((DynamicValue) null);
                return;
            case ActionstepPackage.GET_CALL_INFO__ANI2_VAR:
                setAni2Var((DynamicValue) null);
                return;
            case ActionstepPackage.GET_CALL_INFO__RDNIS:
                setRdnis((DynamicValue) null);
                return;
            case ActionstepPackage.GET_CALL_INFO__TYPE:
                setType((DynamicValue) null);
                return;
        }
        super.eUnset(featureID);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public boolean eIsSet(int featureID) {
        switch(featureID) {
            case ActionstepPackage.GET_CALL_INFO__CALL1:
                return call1 != null;
            case ActionstepPackage.GET_CALL_INFO__ACCOUNT_CODE_VAR:
                return accountCodeVar != null;
            case ActionstepPackage.GET_CALL_INFO__CALLER_ID_NAME_VAR:
                return callerIdNameVar != null;
            case ActionstepPackage.GET_CALL_INFO__CALLER_ID_NUM_VAR:
                return callerIdNumVar != null;
            case ActionstepPackage.GET_CALL_INFO__CHANNEL_NAME_VAR:
                return channelNameVar != null;
            case ActionstepPackage.GET_CALL_INFO__CONTEXT_VAR:
                return contextVar != null;
            case ActionstepPackage.GET_CALL_INFO__EXTENSION_VAR:
                return extensionVar != null;
            case ActionstepPackage.GET_CALL_INFO__DIALED_NUMBER:
                return dialedNumber != null;
            case ActionstepPackage.GET_CALL_INFO__PRIORITY_VAR:
                return priorityVar != null;
            case ActionstepPackage.GET_CALL_INFO__STATE_VAR:
                return stateVar != null;
            case ActionstepPackage.GET_CALL_INFO__UNIQUE_ID_VAR:
                return uniqueIdVar != null;
            case ActionstepPackage.GET_CALL_INFO__ANI2_VAR:
                return ani2Var != null;
            case ActionstepPackage.GET_CALL_INFO__RDNIS:
                return rdnis != null;
            case ActionstepPackage.GET_CALL_INFO__TYPE:
                return type != null;
        }
        return super.eIsSet(featureID);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public int eBaseStructuralFeatureID(int derivedFeatureID, Class<?> baseClass) {
        if (baseClass == CallConsumer1.class) {
            switch(derivedFeatureID) {
                case ActionstepPackage.GET_CALL_INFO__CALL1:
                    return CallPackage.CALL_CONSUMER1__CALL1;
                default:
                    return -1;
            }
        }
        return super.eBaseStructuralFeatureID(derivedFeatureID, baseClass);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public int eDerivedStructuralFeatureID(int baseFeatureID, Class<?> baseClass) {
        if (baseClass == CallConsumer1.class) {
            switch(baseFeatureID) {
                case CallPackage.CALL_CONSUMER1__CALL1:
                    return ActionstepPackage.GET_CALL_INFO__CALL1;
                default:
                    return -1;
            }
        }
        return super.eDerivedStructuralFeatureID(baseFeatureID, baseClass);
    }
}
