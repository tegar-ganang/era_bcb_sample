package com.safi.asterisk.actionstep.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.apache.commons.lang.StringUtils;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.response.ManagerError;
import org.asteriskjava.manager.response.ManagerResponse;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import com.safi.asterisk.AsteriskFactory;
import com.safi.asterisk.Call;
import com.safi.asterisk.actionstep.ActionstepPackage;
import com.safi.asterisk.actionstep.OriginateCall;
import com.safi.asterisk.impl.AsteriskFactoryImpl;
import com.safi.asterisk.saflet.AsteriskSafletEnvironment;
import com.safi.asterisk.util.AsteriskSafletConstants;
import com.safi.core.actionstep.ActionStepException;
import com.safi.core.actionstep.ActionStepFactory;
import com.safi.core.actionstep.DynamicValue;
import com.safi.core.actionstep.Output;
import com.safi.core.actionstep.OutputType;
import com.safi.core.call.CallPackage;
import com.safi.core.call.CallSource1;
import com.safi.core.call.SafiCall;
import com.safi.core.saflet.Saflet;
import com.safi.core.saflet.SafletConstants;
import com.safi.core.saflet.SafletContext;
import com.safi.db.VariableType;
import com.safi.db.astdb.AsteriskServer;
import com.safi.db.util.VariableTranslator;

/**
 * <!-- begin-user-doc --> An implementation of the model object '
 * <em><b>Originate Call</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#getNewCall1 <em>New Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#isAsync <em>Async</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#getAccount <em>Account</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#getApplication <em>Application</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#getCallerId <em>Caller Id</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#getContext <em>Context</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#getData <em>Data</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#getExtension <em>Extension</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#getPriority <em>Priority</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#getTimeout <em>Timeout</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#getCallingPresentation <em>Calling Presentation</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#getChannel <em>Channel</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#isTakeControl <em>Take Control</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.OriginateCallImpl#getVariables <em>Variables</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class OriginateCallImpl extends AsteriskActionStepImpl implements OriginateCall {

    /**
	 * The cached value of the '{@link #getNewCall1() <em>New Call1</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getNewCall1()
	 * @generated
	 * @ordered
	 */
    protected SafiCall newCall1;

    /**
   * The default value of the '{@link #isAsync() <em>Async</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #isAsync()
   * @generated
   * @ordered
   */
    protected static final boolean ASYNC_EDEFAULT = false;

    /**
   * The cached value of the '{@link #isAsync() <em>Async</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #isAsync()
   * @generated
   * @ordered
   */
    protected boolean async = ASYNC_EDEFAULT;

    /**
	 * The cached value of the '{@link #getAccount() <em>Account</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getAccount()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue account;

    /**
	 * The cached value of the '{@link #getApplication() <em>Application</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getApplication()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue application;

    /**
	 * The cached value of the '{@link #getCallerId() <em>Caller Id</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getCallerId()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue callerId;

    /**
	 * The cached value of the '{@link #getContext() <em>Context</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getContext()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue context;

    /**
	 * The cached value of the '{@link #getData() <em>Data</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getData()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue data;

    /**
	 * The cached value of the '{@link #getExtension() <em>Extension</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getExtension()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue extension;

    /**
   * The default value of the '{@link #getPriority() <em>Priority</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getPriority()
   * @generated
   * @ordered
   */
    protected static final int PRIORITY_EDEFAULT = 1;

    /**
   * The cached value of the '{@link #getPriority() <em>Priority</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getPriority()
   * @generated
   * @ordered
   */
    protected int priority = PRIORITY_EDEFAULT;

    /**
   * The default value of the '{@link #getTimeout() <em>Timeout</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getTimeout()
   * @generated
   * @ordered
   */
    protected static final long TIMEOUT_EDEFAULT = 0L;

    /**
   * The cached value of the '{@link #getTimeout() <em>Timeout</em>}' attribute. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getTimeout()
   * @generated
   * @ordered
   */
    protected long timeout = TIMEOUT_EDEFAULT;

    /**
   * The default value of the '{@link #getCallingPresentation()
   * <em>Calling Presentation</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @see #getCallingPresentation()
   * @generated
   * @ordered
   */
    protected static final int CALLING_PRESENTATION_EDEFAULT = 1;

    /**
   * The cached value of the '{@link #getCallingPresentation()
   * <em>Calling Presentation</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @see #getCallingPresentation()
   * @generated
   * @ordered
   */
    protected int callingPresentation = CALLING_PRESENTATION_EDEFAULT;

    /**
	 * The cached value of the '{@link #getChannel() <em>Channel</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getChannel()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue channel;

    /**
	 * The default value of the '{@link #isTakeControl() <em>Take Control</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #isTakeControl()
	 * @generated
	 * @ordered
	 */
    protected static final boolean TAKE_CONTROL_EDEFAULT = false;

    /**
	 * The cached value of the '{@link #isTakeControl() <em>Take Control</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #isTakeControl()
	 * @generated
	 * @ordered
	 */
    protected boolean takeControl = TAKE_CONTROL_EDEFAULT;

    /**
	 * The cached value of the '{@link #getVariables() <em>Variables</em>}' containment reference.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @see #getVariables()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue variables;

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    protected OriginateCallImpl() {
        super();
    }

    @Override
    public void beginProcessing(SafletContext context) throws ActionStepException {
        super.beginProcessing(context);
        int idx = 1;
        Exception exception = null;
        Object variableRawValue = context.getVariableRawValue(AsteriskSafletConstants.VAR_KEY_MANAGER_CONNECTION);
        if (variableRawValue == null || !(variableRawValue instanceof ManagerConnection)) exception = new ActionStepException("No manager connection found in current context"); else {
            ManagerConnection connection = (ManagerConnection) variableRawValue;
            try {
                if (takeControl) {
                    exception = takeControl(connection, context);
                } else {
                    OriginateAction action = new OriginateAction();
                    Object dynValue = resolveDynamicValue(account, context);
                    String acctVal = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
                    action.setAccount(acctVal);
                    dynValue = resolveDynamicValue(application, context);
                    String app = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
                    action.setApplication(app);
                    action.setAsync(async);
                    dynValue = resolveDynamicValue(callerId, context);
                    String cid = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
                    action.setCallerId(cid == null ? "" : cid);
                    action.setCallingPres(callingPresentation);
                    dynValue = resolveDynamicValue(this.context, context);
                    String ctx = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
                    action.setContext(ctx);
                    dynValue = resolveDynamicValue(data, context);
                    String dat = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
                    action.setData(dat);
                    dynValue = resolveDynamicValue(extension, context);
                    String ext = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
                    action.setExten(ext);
                    dynValue = resolveDynamicValue(this.channel, context);
                    String chan = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
                    if (StringUtils.isBlank(chan)) {
                        if (debugLog.isLoggable(Level.FINEST)) debug("No channel specified, using current: " + chan);
                        exception = new ActionStepException("Channel name must be specified");
                    } else {
                        action.setChannel(chan);
                        action.setPriority(priority);
                        action.setTimeout(timeout);
                        Object vo = resolveDynamicValue(variables, context);
                        if (vo != null) {
                            Map<String, String> vars = new HashMap<String, String>();
                            List varlist = null;
                            if (vo instanceof List) {
                                varlist = (List) vo;
                            } else {
                                varlist = VariableTranslator.parseArray(vo.toString());
                            }
                            if (varlist != null) {
                                String vname = null;
                                for (Object o : varlist) {
                                    if (vname == null) {
                                        vname = o.toString();
                                    } else {
                                        vars.put(vname, o == null ? "" : o.toString());
                                        vname = null;
                                    }
                                }
                                action.setVariables(vars);
                            } else {
                                if (debugLog.isLoggable(Level.FINEST)) debugLog.warning("Variables must be in array format.  Ignoring variables " + vo);
                            }
                        }
                        ManagerResponse response = connection.sendAction(action, Saflet.DEFAULT_MANAGER_ACTION_TIMEOUT);
                        if (response instanceof ManagerError) exception = new ActionStepException("Couldn't place call to extension: " + response);
                    }
                }
                idx = 1;
            } catch (Exception e) {
                exception = e;
            }
        }
        if (exception != null) {
            if (exception instanceof TimeoutException) {
                idx = 2;
            } else {
                handleException(context, exception);
                return;
            }
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
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated NOT
   */
    public void setTakeControl(boolean newTakeControl) {
        boolean oldTakeControl = takeControl;
        takeControl = newTakeControl;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__TAKE_CONTROL, oldTakeControl, takeControl));
        if (oldTakeControl != newTakeControl) {
            if (!newTakeControl) {
                setNewCall1(null);
            } else if (newCall1 == null && getSaflet() != null) {
                Call call = AsteriskFactory.eINSTANCE.createCall();
                call.setName(getSaflet().getUniqueCallName("Call"));
                setNewCall1(call);
            }
        }
    }

    private Exception takeControl(ManagerConnection connection, SafletContext context) throws ActionStepException, IOException, IllegalArgumentException, IllegalStateException, TimeoutException, AgiException {
        OriginateAction action = new OriginateAction();
        Object dynValue = resolveDynamicValue(this.channel, context);
        String chan = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
        if (StringUtils.isBlank(chan)) {
            return new ActionStepException("Channel name must be specified");
        }
        if (debugLog.isLoggable(Level.FINEST)) debug("Creating call from channel " + chan);
        action.setContext("default");
        action.setApplication("Agi");
        AsteriskServer server = (AsteriskServer) context.getVariableRawValue(SafletConstants.VAR_KEY_TELEPHONY_SUBSYSTEM);
        String serverAddr = server == null ? null : server.getVisibleSafiServerIP();
        if (StringUtils.isBlank(serverAddr)) serverAddr = connection.getLocalAddress().getCanonicalHostName();
        action.setData("agi://" + serverAddr + "/safletEngine.agi?loopback=true");
        action.setChannel(chan);
        action.setAsync(async);
        dynValue = resolveDynamicValue(callerId, context);
        String cid = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
        action.setCallerId(cid);
        dynValue = resolveDynamicValue(account, context);
        String acctVal = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
        action.setAccount(acctVal);
        UUID uuid = UUID.randomUUID();
        AsteriskSafletEnvironment handlerEnvironment = (AsteriskSafletEnvironment) getSaflet().getSafletEnvironment();
        Long timeoutVal = new Long(timeout <= 0 ? Saflet.DEFAULT_MANAGER_ACTION_TIMEOUT : timeout);
        handlerEnvironment.setLoopbackLock(uuid.toString(), timeoutVal);
        action.setVariable("SafiUUID", uuid.toString());
        ManagerResponse response = connection.sendAction(action, Saflet.DEFAULT_MANAGER_ACTION_TIMEOUT);
        if (response instanceof ManagerError) return new ActionStepException("Couldn't place call: " + response.getMessage());
        Object returned = handlerEnvironment.getLoopbackCall(uuid.toString());
        if (returned instanceof Object[]) {
            Object[] pair = (Object[]) returned;
            if (newCall1 == null) setNewCall1(AsteriskFactoryImpl.eINSTANCE.createCall());
            ((Call) newCall1).setChannel((AgiChannel) pair[0]);
            ((Call) newCall1).setData("AgiRequest", pair[1]);
        } else {
            return new ActionStepException("Loopback for call failed!");
        }
        action.setCallingPres(callingPresentation);
        dynValue = resolveDynamicValue(this.context, context);
        String ctx = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
        ((Call) newCall1).getChannel().setContext(ctx);
        dynValue = resolveDynamicValue(extension, context);
        String ext = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
        ((Call) newCall1).getChannel().setExtension(ext);
        ((Call) newCall1).getChannel().setPriority(String.valueOf(priority));
        return null;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return ActionstepPackage.Literals.ORIGINATE_CALL;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public SafiCall getNewCall1() {
        return newCall1;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetNewCall1(SafiCall newNewCall1, NotificationChain msgs) {
        SafiCall oldNewCall1 = newCall1;
        newCall1 = newNewCall1;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__NEW_CALL1, oldNewCall1, newNewCall1);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public void setNewCall1(SafiCall newNewCall1) {
        if (newNewCall1 != newCall1) {
            NotificationChain msgs = null;
            if (newCall1 != null) msgs = ((InternalEObject) newCall1).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__NEW_CALL1, null, msgs);
            if (newNewCall1 != null) msgs = ((InternalEObject) newNewCall1).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__NEW_CALL1, null, msgs);
            msgs = basicSetNewCall1(newNewCall1, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__NEW_CALL1, newNewCall1, newNewCall1));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isAsync() {
        return async;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setAsync(boolean newAsync) {
        boolean oldAsync = async;
        async = newAsync;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__ASYNC, oldAsync, async));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getAccount() {
        return account;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetAccount(DynamicValue newAccount, NotificationChain msgs) {
        DynamicValue oldAccount = account;
        account = newAccount;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__ACCOUNT, oldAccount, newAccount);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setAccount(DynamicValue newAccount) {
        if (newAccount != account) {
            NotificationChain msgs = null;
            if (account != null) msgs = ((InternalEObject) account).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__ACCOUNT, null, msgs);
            if (newAccount != null) msgs = ((InternalEObject) newAccount).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__ACCOUNT, null, msgs);
            msgs = basicSetAccount(newAccount, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__ACCOUNT, newAccount, newAccount));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getApplication() {
        return application;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetApplication(DynamicValue newApplication, NotificationChain msgs) {
        DynamicValue oldApplication = application;
        application = newApplication;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__APPLICATION, oldApplication, newApplication);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setApplication(DynamicValue newApplication) {
        if (newApplication != application) {
            NotificationChain msgs = null;
            if (application != null) msgs = ((InternalEObject) application).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__APPLICATION, null, msgs);
            if (newApplication != null) msgs = ((InternalEObject) newApplication).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__APPLICATION, null, msgs);
            msgs = basicSetApplication(newApplication, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__APPLICATION, newApplication, newApplication));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getCallerId() {
        return callerId;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetCallerId(DynamicValue newCallerId, NotificationChain msgs) {
        DynamicValue oldCallerId = callerId;
        callerId = newCallerId;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__CALLER_ID, oldCallerId, newCallerId);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setCallerId(DynamicValue newCallerId) {
        if (newCallerId != callerId) {
            NotificationChain msgs = null;
            if (callerId != null) msgs = ((InternalEObject) callerId).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__CALLER_ID, null, msgs);
            if (newCallerId != null) msgs = ((InternalEObject) newCallerId).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__CALLER_ID, null, msgs);
            msgs = basicSetCallerId(newCallerId, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__CALLER_ID, newCallerId, newCallerId));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getContext() {
        return context;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetContext(DynamicValue newContext, NotificationChain msgs) {
        DynamicValue oldContext = context;
        context = newContext;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__CONTEXT, oldContext, newContext);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setContext(DynamicValue newContext) {
        if (newContext != context) {
            NotificationChain msgs = null;
            if (context != null) msgs = ((InternalEObject) context).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__CONTEXT, null, msgs);
            if (newContext != null) msgs = ((InternalEObject) newContext).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__CONTEXT, null, msgs);
            msgs = basicSetContext(newContext, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__CONTEXT, newContext, newContext));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getData() {
        return data;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetData(DynamicValue newData, NotificationChain msgs) {
        DynamicValue oldData = data;
        data = newData;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__DATA, oldData, newData);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setData(DynamicValue newData) {
        if (newData != data) {
            NotificationChain msgs = null;
            if (data != null) msgs = ((InternalEObject) data).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__DATA, null, msgs);
            if (newData != null) msgs = ((InternalEObject) newData).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__DATA, null, msgs);
            msgs = basicSetData(newData, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__DATA, newData, newData));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getExtension() {
        return extension;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetExtension(DynamicValue newExtension, NotificationChain msgs) {
        DynamicValue oldExtension = extension;
        extension = newExtension;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__EXTENSION, oldExtension, newExtension);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setExtension(DynamicValue newExtension) {
        if (newExtension != extension) {
            NotificationChain msgs = null;
            if (extension != null) msgs = ((InternalEObject) extension).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__EXTENSION, null, msgs);
            if (newExtension != null) msgs = ((InternalEObject) newExtension).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__EXTENSION, null, msgs);
            msgs = basicSetExtension(newExtension, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__EXTENSION, newExtension, newExtension));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public int getPriority() {
        return priority;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setPriority(int newPriority) {
        int oldPriority = priority;
        priority = newPriority;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__PRIORITY, oldPriority, priority));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public long getTimeout() {
        return timeout;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setTimeout(long newTimeout) {
        long oldTimeout = timeout;
        timeout = newTimeout;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__TIMEOUT, oldTimeout, timeout));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public int getCallingPresentation() {
        return callingPresentation;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setCallingPresentation(int newCallingPresentation) {
        int oldCallingPresentation = callingPresentation;
        callingPresentation = newCallingPresentation;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__CALLING_PRESENTATION, oldCallingPresentation, callingPresentation));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getChannel() {
        return channel;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetChannel(DynamicValue newChannel, NotificationChain msgs) {
        DynamicValue oldChannel = channel;
        channel = newChannel;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__CHANNEL, oldChannel, newChannel);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setChannel(DynamicValue newChannel) {
        if (newChannel != channel) {
            NotificationChain msgs = null;
            if (channel != null) msgs = ((InternalEObject) channel).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__CHANNEL, null, msgs);
            if (newChannel != null) msgs = ((InternalEObject) newChannel).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__CHANNEL, null, msgs);
            msgs = basicSetChannel(newChannel, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__CHANNEL, newChannel, newChannel));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isTakeControl() {
        return takeControl;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getVariables() {
        return variables;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetVariables(DynamicValue newVariables, NotificationChain msgs) {
        DynamicValue oldVariables = variables;
        variables = newVariables;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__VARIABLES, oldVariables, newVariables);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setVariables(DynamicValue newVariables) {
        if (newVariables != variables) {
            NotificationChain msgs = null;
            if (variables != null) msgs = ((InternalEObject) variables).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__VARIABLES, null, msgs);
            if (newVariables != null) msgs = ((InternalEObject) newVariables).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.ORIGINATE_CALL__VARIABLES, null, msgs);
            msgs = basicSetVariables(newVariables, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.ORIGINATE_CALL__VARIABLES, newVariables, newVariables));
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    public void setVariable(String name, String value) {
        throw new UnsupportedOperationException();
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.ORIGINATE_CALL__NEW_CALL1:
                return basicSetNewCall1(null, msgs);
            case ActionstepPackage.ORIGINATE_CALL__ACCOUNT:
                return basicSetAccount(null, msgs);
            case ActionstepPackage.ORIGINATE_CALL__APPLICATION:
                return basicSetApplication(null, msgs);
            case ActionstepPackage.ORIGINATE_CALL__CALLER_ID:
                return basicSetCallerId(null, msgs);
            case ActionstepPackage.ORIGINATE_CALL__CONTEXT:
                return basicSetContext(null, msgs);
            case ActionstepPackage.ORIGINATE_CALL__DATA:
                return basicSetData(null, msgs);
            case ActionstepPackage.ORIGINATE_CALL__EXTENSION:
                return basicSetExtension(null, msgs);
            case ActionstepPackage.ORIGINATE_CALL__CHANNEL:
                return basicSetChannel(null, msgs);
            case ActionstepPackage.ORIGINATE_CALL__VARIABLES:
                return basicSetVariables(null, msgs);
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
            case ActionstepPackage.ORIGINATE_CALL__NEW_CALL1:
                return getNewCall1();
            case ActionstepPackage.ORIGINATE_CALL__ASYNC:
                return isAsync();
            case ActionstepPackage.ORIGINATE_CALL__ACCOUNT:
                return getAccount();
            case ActionstepPackage.ORIGINATE_CALL__APPLICATION:
                return getApplication();
            case ActionstepPackage.ORIGINATE_CALL__CALLER_ID:
                return getCallerId();
            case ActionstepPackage.ORIGINATE_CALL__CONTEXT:
                return getContext();
            case ActionstepPackage.ORIGINATE_CALL__DATA:
                return getData();
            case ActionstepPackage.ORIGINATE_CALL__EXTENSION:
                return getExtension();
            case ActionstepPackage.ORIGINATE_CALL__PRIORITY:
                return getPriority();
            case ActionstepPackage.ORIGINATE_CALL__TIMEOUT:
                return getTimeout();
            case ActionstepPackage.ORIGINATE_CALL__CALLING_PRESENTATION:
                return getCallingPresentation();
            case ActionstepPackage.ORIGINATE_CALL__CHANNEL:
                return getChannel();
            case ActionstepPackage.ORIGINATE_CALL__TAKE_CONTROL:
                return isTakeControl();
            case ActionstepPackage.ORIGINATE_CALL__VARIABLES:
                return getVariables();
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
            case ActionstepPackage.ORIGINATE_CALL__NEW_CALL1:
                setNewCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.ORIGINATE_CALL__ASYNC:
                setAsync((Boolean) newValue);
                return;
            case ActionstepPackage.ORIGINATE_CALL__ACCOUNT:
                setAccount((DynamicValue) newValue);
                return;
            case ActionstepPackage.ORIGINATE_CALL__APPLICATION:
                setApplication((DynamicValue) newValue);
                return;
            case ActionstepPackage.ORIGINATE_CALL__CALLER_ID:
                setCallerId((DynamicValue) newValue);
                return;
            case ActionstepPackage.ORIGINATE_CALL__CONTEXT:
                setContext((DynamicValue) newValue);
                return;
            case ActionstepPackage.ORIGINATE_CALL__DATA:
                setData((DynamicValue) newValue);
                return;
            case ActionstepPackage.ORIGINATE_CALL__EXTENSION:
                setExtension((DynamicValue) newValue);
                return;
            case ActionstepPackage.ORIGINATE_CALL__PRIORITY:
                setPriority((Integer) newValue);
                return;
            case ActionstepPackage.ORIGINATE_CALL__TIMEOUT:
                setTimeout((Long) newValue);
                return;
            case ActionstepPackage.ORIGINATE_CALL__CALLING_PRESENTATION:
                setCallingPresentation((Integer) newValue);
                return;
            case ActionstepPackage.ORIGINATE_CALL__CHANNEL:
                setChannel((DynamicValue) newValue);
                return;
            case ActionstepPackage.ORIGINATE_CALL__TAKE_CONTROL:
                setTakeControl((Boolean) newValue);
                return;
            case ActionstepPackage.ORIGINATE_CALL__VARIABLES:
                setVariables((DynamicValue) newValue);
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
            case ActionstepPackage.ORIGINATE_CALL__NEW_CALL1:
                setNewCall1((SafiCall) null);
                return;
            case ActionstepPackage.ORIGINATE_CALL__ASYNC:
                setAsync(ASYNC_EDEFAULT);
                return;
            case ActionstepPackage.ORIGINATE_CALL__ACCOUNT:
                setAccount((DynamicValue) null);
                return;
            case ActionstepPackage.ORIGINATE_CALL__APPLICATION:
                setApplication((DynamicValue) null);
                return;
            case ActionstepPackage.ORIGINATE_CALL__CALLER_ID:
                setCallerId((DynamicValue) null);
                return;
            case ActionstepPackage.ORIGINATE_CALL__CONTEXT:
                setContext((DynamicValue) null);
                return;
            case ActionstepPackage.ORIGINATE_CALL__DATA:
                setData((DynamicValue) null);
                return;
            case ActionstepPackage.ORIGINATE_CALL__EXTENSION:
                setExtension((DynamicValue) null);
                return;
            case ActionstepPackage.ORIGINATE_CALL__PRIORITY:
                setPriority(PRIORITY_EDEFAULT);
                return;
            case ActionstepPackage.ORIGINATE_CALL__TIMEOUT:
                setTimeout(TIMEOUT_EDEFAULT);
                return;
            case ActionstepPackage.ORIGINATE_CALL__CALLING_PRESENTATION:
                setCallingPresentation(CALLING_PRESENTATION_EDEFAULT);
                return;
            case ActionstepPackage.ORIGINATE_CALL__CHANNEL:
                setChannel((DynamicValue) null);
                return;
            case ActionstepPackage.ORIGINATE_CALL__TAKE_CONTROL:
                setTakeControl(TAKE_CONTROL_EDEFAULT);
                return;
            case ActionstepPackage.ORIGINATE_CALL__VARIABLES:
                setVariables((DynamicValue) null);
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
            case ActionstepPackage.ORIGINATE_CALL__NEW_CALL1:
                return newCall1 != null;
            case ActionstepPackage.ORIGINATE_CALL__ASYNC:
                return async != ASYNC_EDEFAULT;
            case ActionstepPackage.ORIGINATE_CALL__ACCOUNT:
                return account != null;
            case ActionstepPackage.ORIGINATE_CALL__APPLICATION:
                return application != null;
            case ActionstepPackage.ORIGINATE_CALL__CALLER_ID:
                return callerId != null;
            case ActionstepPackage.ORIGINATE_CALL__CONTEXT:
                return context != null;
            case ActionstepPackage.ORIGINATE_CALL__DATA:
                return data != null;
            case ActionstepPackage.ORIGINATE_CALL__EXTENSION:
                return extension != null;
            case ActionstepPackage.ORIGINATE_CALL__PRIORITY:
                return priority != PRIORITY_EDEFAULT;
            case ActionstepPackage.ORIGINATE_CALL__TIMEOUT:
                return timeout != TIMEOUT_EDEFAULT;
            case ActionstepPackage.ORIGINATE_CALL__CALLING_PRESENTATION:
                return callingPresentation != CALLING_PRESENTATION_EDEFAULT;
            case ActionstepPackage.ORIGINATE_CALL__CHANNEL:
                return channel != null;
            case ActionstepPackage.ORIGINATE_CALL__TAKE_CONTROL:
                return takeControl != TAKE_CONTROL_EDEFAULT;
            case ActionstepPackage.ORIGINATE_CALL__VARIABLES:
                return variables != null;
        }
        return super.eIsSet(featureID);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public int eBaseStructuralFeatureID(int derivedFeatureID, Class<?> baseClass) {
        if (baseClass == CallSource1.class) {
            switch(derivedFeatureID) {
                case ActionstepPackage.ORIGINATE_CALL__NEW_CALL1:
                    return CallPackage.CALL_SOURCE1__NEW_CALL1;
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
        if (baseClass == CallSource1.class) {
            switch(baseFeatureID) {
                case CallPackage.CALL_SOURCE1__NEW_CALL1:
                    return ActionstepPackage.ORIGINATE_CALL__NEW_CALL1;
                default:
                    return -1;
            }
        }
        return super.eDerivedStructuralFeatureID(baseFeatureID, baseClass);
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public String toString() {
        if (eIsProxy()) return super.toString();
        StringBuffer result = new StringBuffer(super.toString());
        result.append(" (async: ");
        result.append(async);
        result.append(", priority: ");
        result.append(priority);
        result.append(", timeout: ");
        result.append(timeout);
        result.append(", callingPresentation: ");
        result.append(callingPresentation);
        result.append(", takeControl: ");
        result.append(takeControl);
        result.append(')');
        return result.toString();
    }
}
