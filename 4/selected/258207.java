package com.safi.asterisk.actionstep.impl;

import java.util.logging.Level;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.action.RedirectAction;
import org.asteriskjava.manager.event.DialEvent;
import org.asteriskjava.manager.event.HangupEvent;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.response.ManagerError;
import org.asteriskjava.manager.response.ManagerResponse;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import com.safi.asterisk.Call;
import com.safi.asterisk.actionstep.ActionstepPackage;
import com.safi.asterisk.actionstep.Transfer;
import com.safi.asterisk.util.AsteriskSafletConstants;
import com.safi.core.actionstep.ActionStepException;
import com.safi.core.actionstep.DynamicValue;
import com.safi.core.actionstep.impl.ActionStepImpl;
import com.safi.core.call.CallConsumer1;
import com.safi.core.call.CallConsumer2;
import com.safi.core.call.CallPackage;
import com.safi.core.call.SafiCall;
import com.safi.core.saflet.Saflet;
import com.safi.core.saflet.SafletContext;
import com.safi.db.VariableType;
import com.safi.db.util.VariableTranslator;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Transfer</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.TransferImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.TransferImpl#getCall2 <em>Call2</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.TransferImpl#getContext <em>Context</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.TransferImpl#getExtension <em>Extension</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.TransferImpl#getPriority <em>Priority</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class TransferImpl extends AsteriskActionStepImpl implements Transfer {

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
	 * The cached value of the '{@link #getContext() <em>Context</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getContext()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue context;

    /**
	 * The cached value of the '{@link #getExtension() <em>Extension</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getExtension()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue extension;

    /**
	 * The default value of the '{@link #getPriority() <em>Priority</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getPriority()
	 * @generated
	 * @ordered
	 */
    protected static final int PRIORITY_EDEFAULT = 1;

    /**
	 * The cached value of the '{@link #getPriority() <em>Priority</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getPriority()
	 * @generated
	 * @ordered
	 */
    protected int priority = PRIORITY_EDEFAULT;

    private Object lock = new Object();

    private Object lock2 = new Object();

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected TransferImpl() {
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
        AgiChannel channel = ((Call) call1).getChannel();
        Exception exception = null;
        try {
            RedirectAction action = new RedirectAction();
            Object dynValue = resolveDynamicValue(this.context, context);
            String ctx = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
            action.setContext(ctx);
            action.setPriority(priority);
            dynValue = resolveDynamicValue(extension, context);
            String ext = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
            action.setExten(ext);
            String chan = channel.getName();
            action.setChannel(chan);
            if (call2 != null) action.setExtraChannel(((Call) call2).getChannelName());
            StringBuffer buf = new StringBuffer();
            RedirectCallManagerEventListener eventListener = new RedirectCallManagerEventListener(buf, channel.getUniqueId());
            try {
                connection.addEventListener(eventListener);
                ManagerResponse response = connection.sendAction(action, Saflet.DEFAULT_MANAGER_ACTION_TIMEOUT);
                if (response instanceof ManagerError) exception = new ActionStepException("Couldn't redirect call to extension: " + response); else {
                    try {
                        synchronized (lock) {
                            if (!eventListener.stopped) {
                                lock.wait(10000);
                            }
                        }
                    } catch (Exception e) {
                    }
                }
                if (buf.length() == 0) {
                    exception = new ActionStepException("Call to " + ext + " failed to initiate");
                } else {
                    if (debugLog.isLoggable(Level.FINEST)) debug("The call was established " + buf);
                }
            } finally {
                connection.removeEventListener(eventListener);
                try {
                    eventListener.stop();
                } catch (Exception e) {
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
        return ActionstepPackage.Literals.TRANSFER;
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.TRANSFER__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.TRANSFER__CALL1, oldCall1, call1));
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.TRANSFER__CALL2, oldCall2, call2));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.TRANSFER__CALL2, oldCall2, call2));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getContext() {
        return context;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetContext(DynamicValue newContext, NotificationChain msgs) {
        DynamicValue oldContext = context;
        context = newContext;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.TRANSFER__CONTEXT, oldContext, newContext);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setContext(DynamicValue newContext) {
        if (newContext != context) {
            NotificationChain msgs = null;
            if (context != null) msgs = ((InternalEObject) context).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.TRANSFER__CONTEXT, null, msgs);
            if (newContext != null) msgs = ((InternalEObject) newContext).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.TRANSFER__CONTEXT, null, msgs);
            msgs = basicSetContext(newContext, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.TRANSFER__CONTEXT, newContext, newContext));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getExtension() {
        return extension;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetExtension(DynamicValue newExtension, NotificationChain msgs) {
        DynamicValue oldExtension = extension;
        extension = newExtension;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.TRANSFER__EXTENSION, oldExtension, newExtension);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setExtension(DynamicValue newExtension) {
        if (newExtension != extension) {
            NotificationChain msgs = null;
            if (extension != null) msgs = ((InternalEObject) extension).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.TRANSFER__EXTENSION, null, msgs);
            if (newExtension != null) msgs = ((InternalEObject) newExtension).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.TRANSFER__EXTENSION, null, msgs);
            msgs = basicSetExtension(newExtension, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.TRANSFER__EXTENSION, newExtension, newExtension));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public int getPriority() {
        return priority;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setPriority(int newPriority) {
        int oldPriority = priority;
        priority = newPriority;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.TRANSFER__PRIORITY, oldPriority, priority));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.TRANSFER__CONTEXT:
                return basicSetContext(null, msgs);
            case ActionstepPackage.TRANSFER__EXTENSION:
                return basicSetExtension(null, msgs);
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
            case ActionstepPackage.TRANSFER__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.TRANSFER__CALL2:
                if (resolve) return getCall2();
                return basicGetCall2();
            case ActionstepPackage.TRANSFER__CONTEXT:
                return getContext();
            case ActionstepPackage.TRANSFER__EXTENSION:
                return getExtension();
            case ActionstepPackage.TRANSFER__PRIORITY:
                return getPriority();
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
            case ActionstepPackage.TRANSFER__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.TRANSFER__CALL2:
                setCall2((SafiCall) newValue);
                return;
            case ActionstepPackage.TRANSFER__CONTEXT:
                setContext((DynamicValue) newValue);
                return;
            case ActionstepPackage.TRANSFER__EXTENSION:
                setExtension((DynamicValue) newValue);
                return;
            case ActionstepPackage.TRANSFER__PRIORITY:
                setPriority((Integer) newValue);
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
            case ActionstepPackage.TRANSFER__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.TRANSFER__CALL2:
                setCall2((SafiCall) null);
                return;
            case ActionstepPackage.TRANSFER__CONTEXT:
                setContext((DynamicValue) null);
                return;
            case ActionstepPackage.TRANSFER__EXTENSION:
                setExtension((DynamicValue) null);
                return;
            case ActionstepPackage.TRANSFER__PRIORITY:
                setPriority(PRIORITY_EDEFAULT);
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
            case ActionstepPackage.TRANSFER__CALL1:
                return call1 != null;
            case ActionstepPackage.TRANSFER__CALL2:
                return call2 != null;
            case ActionstepPackage.TRANSFER__CONTEXT:
                return context != null;
            case ActionstepPackage.TRANSFER__EXTENSION:
                return extension != null;
            case ActionstepPackage.TRANSFER__PRIORITY:
                return priority != PRIORITY_EDEFAULT;
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
                case ActionstepPackage.TRANSFER__CALL1:
                    return CallPackage.CALL_CONSUMER1__CALL1;
                default:
                    return -1;
            }
        }
        if (baseClass == CallConsumer2.class) {
            switch(derivedFeatureID) {
                case ActionstepPackage.TRANSFER__CALL2:
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
                    return ActionstepPackage.TRANSFER__CALL1;
                default:
                    return -1;
            }
        }
        if (baseClass == CallConsumer2.class) {
            switch(baseFeatureID) {
                case CallPackage.CALL_CONSUMER2__CALL2:
                    return ActionstepPackage.TRANSFER__CALL2;
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
        result.append(" (priority: ");
        result.append(priority);
        result.append(')');
        return result.toString();
    }

    public class RedirectCallManagerEventListener implements ManagerEventListener, Runnable {

        private StringBuffer buf;

        private String uniqueId;

        volatile boolean stopped;

        public RedirectCallManagerEventListener(StringBuffer buf, String uniqueId) {
            this.buf = buf;
            this.uniqueId = uniqueId;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RedirectCallManagerEventListener)) return false;
            RedirectCallManagerEventListener o = (RedirectCallManagerEventListener) obj;
            return new EqualsBuilder().append(o.buf, buf).append(o.uniqueId, uniqueId).append(o.stopped, stopped).isEquals();
        }

        @Override
        public void onManagerEvent(ManagerEvent event) {
            if (event instanceof DialEvent) {
                DialEvent evt = (DialEvent) event;
                if (StringUtils.equals(uniqueId, evt.getUniqueId())) {
                    buf.append(evt.getDestUniqueId());
                    stop();
                }
            } else if (event instanceof HangupEvent) {
                if (StringUtils.equals(((HangupEvent) event).getUniqueId(), uniqueId)) {
                    stop();
                }
            } else {
            }
        }

        public synchronized void stop() {
            synchronized (lock) {
                stopped = true;
                lock.notifyAll();
            }
            synchronized (lock2) {
                lock2.notifyAll();
            }
        }

        @Override
        public void run() {
            synchronized (lock2) {
                try {
                    lock2.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public StringBuffer getBuf() {
            return buf;
        }

        public String getUniqueId() {
            return uniqueId;
        }

        public boolean isStopped() {
            return stopped;
        }
    }
}
