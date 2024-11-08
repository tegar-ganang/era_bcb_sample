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
import com.safi.asterisk.actionstep.MeetMeAdmin;
import com.safi.asterisk.actionstep.MeetMeAdminCommand;
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
 * An implementation of the model object '<em><b>Meet Me Admin</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.MeetMeAdminImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.MeetMeAdminImpl#getConferenceNumber <em>Conference Number</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.MeetMeAdminImpl#getCommand <em>Command</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.MeetMeAdminImpl#getUser <em>User</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class MeetMeAdminImpl extends AsteriskActionStepImpl implements MeetMeAdmin {

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
	 * The cached value of the '{@link #getConferenceNumber() <em>Conference Number</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getConferenceNumber()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue conferenceNumber;

    /**
	 * The default value of the '{@link #getCommand() <em>Command</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getCommand()
	 * @generated
	 * @ordered
	 */
    protected static final MeetMeAdminCommand COMMAND_EDEFAULT = MeetMeAdminCommand.EJECT_LAST_USER;

    /**
	 * The cached value of the '{@link #getCommand() <em>Command</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getCommand()
	 * @generated
	 * @ordered
	 */
    protected MeetMeAdminCommand command = COMMAND_EDEFAULT;

    /**
	 * The cached value of the '{@link #getUser() <em>User</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getUser()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue user;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected MeetMeAdminImpl() {
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
            String conferenceNum = (String) VariableTranslator.translateValue(VariableType.TEXT, resolveDynamicValue(conferenceNumber, context));
            if (debugLog.isLoggable(Level.FINEST)) debug("Getting meetme count for conference: " + conferenceNum);
            if (StringUtils.isBlank(conferenceNum)) {
                exception = new ActionStepException("Conference number is required for MeetMeCount");
            } else {
                Object dynValue = resolveDynamicValue(user, context);
                String userStr = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
                StringBuffer appCmd = new StringBuffer(conferenceNum);
                appCmd.append('|').append(translateCommand(command));
                if (StringUtils.isNotBlank(userStr)) {
                    appCmd.append('|').append(userStr.trim());
                }
                if (debugLog.isLoggable(Level.FINEST)) debug("MeetMeAdmin being called with args " + appCmd);
                int result = channel.exec("MeetMeAdmin", appCmd.toString());
                if (debugLog.isLoggable(Level.FINEST)) debug("MeetMeAdmin returned " + translateAppReturnValue(result) + " of int " + result);
                if (result == -2) {
                    exception = new ActionStepException("Application MeetMeCount not found");
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

    private char translateCommand(MeetMeAdminCommand command) {
        switch(command) {
            case EJECT_LAST_USER:
                return 'e';
            case KICK_ALL_USERS:
                return 'K';
            case KICK_ONE_USER:
                return 'k';
            case LOCK:
                return 'L';
            case LOWER_CONF_LISTEN_VOLUME:
                return 'v';
            case LOWER_CONF_SPEAK_VOLUME:
                return 's';
            case LOWER_USER_LISTEN_VOLUME:
                return 'u';
            case LOWER_USER_TALK_VOLUME:
                return 't';
            case MUTE_USER:
                return 'M';
            case MUTE_NON_ADMIN:
                return 'N';
            case RAISE_CONF_LISTEN_VOLUME:
                return 'V';
            case RAISE_CONF_SPEAK_VOLUME:
                return 'S';
            case RAISE_USER_LISTEN_VOLUME:
                return 'U';
            case RAISE_USER_TALK_VOLUME:
                return 'T';
            case RESET_ALL_USERS_VOLUME:
                return 'R';
            case RESET_USER_VOLUME:
                return 'r';
            case UNLOCK:
                return 'l';
            case UNMUTE_USER:
                return 'm';
            case UNMUTE_ALL_USERS:
                return 'n';
        }
        return 0;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return ActionstepPackage.Literals.MEET_ME_ADMIN;
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.MEET_ME_ADMIN__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.MEET_ME_ADMIN__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getConferenceNumber() {
        return conferenceNumber;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetConferenceNumber(DynamicValue newConferenceNumber, NotificationChain msgs) {
        DynamicValue oldConferenceNumber = conferenceNumber;
        conferenceNumber = newConferenceNumber;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.MEET_ME_ADMIN__CONFERENCE_NUMBER, oldConferenceNumber, newConferenceNumber);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setConferenceNumber(DynamicValue newConferenceNumber) {
        if (newConferenceNumber != conferenceNumber) {
            NotificationChain msgs = null;
            if (conferenceNumber != null) msgs = ((InternalEObject) conferenceNumber).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.MEET_ME_ADMIN__CONFERENCE_NUMBER, null, msgs);
            if (newConferenceNumber != null) msgs = ((InternalEObject) newConferenceNumber).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.MEET_ME_ADMIN__CONFERENCE_NUMBER, null, msgs);
            msgs = basicSetConferenceNumber(newConferenceNumber, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.MEET_ME_ADMIN__CONFERENCE_NUMBER, newConferenceNumber, newConferenceNumber));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public MeetMeAdminCommand getCommand() {
        return command;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setCommand(MeetMeAdminCommand newCommand) {
        MeetMeAdminCommand oldCommand = command;
        command = newCommand == null ? COMMAND_EDEFAULT : newCommand;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.MEET_ME_ADMIN__COMMAND, oldCommand, command));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getUser() {
        return user;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetUser(DynamicValue newUser, NotificationChain msgs) {
        DynamicValue oldUser = user;
        user = newUser;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.MEET_ME_ADMIN__USER, oldUser, newUser);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setUser(DynamicValue newUser) {
        if (newUser != user) {
            NotificationChain msgs = null;
            if (user != null) msgs = ((InternalEObject) user).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.MEET_ME_ADMIN__USER, null, msgs);
            if (newUser != null) msgs = ((InternalEObject) newUser).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.MEET_ME_ADMIN__USER, null, msgs);
            msgs = basicSetUser(newUser, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.MEET_ME_ADMIN__USER, newUser, newUser));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.MEET_ME_ADMIN__CONFERENCE_NUMBER:
                return basicSetConferenceNumber(null, msgs);
            case ActionstepPackage.MEET_ME_ADMIN__USER:
                return basicSetUser(null, msgs);
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
            case ActionstepPackage.MEET_ME_ADMIN__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.MEET_ME_ADMIN__CONFERENCE_NUMBER:
                return getConferenceNumber();
            case ActionstepPackage.MEET_ME_ADMIN__COMMAND:
                return getCommand();
            case ActionstepPackage.MEET_ME_ADMIN__USER:
                return getUser();
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
            case ActionstepPackage.MEET_ME_ADMIN__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.MEET_ME_ADMIN__CONFERENCE_NUMBER:
                setConferenceNumber((DynamicValue) newValue);
                return;
            case ActionstepPackage.MEET_ME_ADMIN__COMMAND:
                setCommand((MeetMeAdminCommand) newValue);
                return;
            case ActionstepPackage.MEET_ME_ADMIN__USER:
                setUser((DynamicValue) newValue);
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
            case ActionstepPackage.MEET_ME_ADMIN__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.MEET_ME_ADMIN__CONFERENCE_NUMBER:
                setConferenceNumber((DynamicValue) null);
                return;
            case ActionstepPackage.MEET_ME_ADMIN__COMMAND:
                setCommand(COMMAND_EDEFAULT);
                return;
            case ActionstepPackage.MEET_ME_ADMIN__USER:
                setUser((DynamicValue) null);
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
            case ActionstepPackage.MEET_ME_ADMIN__CALL1:
                return call1 != null;
            case ActionstepPackage.MEET_ME_ADMIN__CONFERENCE_NUMBER:
                return conferenceNumber != null;
            case ActionstepPackage.MEET_ME_ADMIN__COMMAND:
                return command != COMMAND_EDEFAULT;
            case ActionstepPackage.MEET_ME_ADMIN__USER:
                return user != null;
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
                case ActionstepPackage.MEET_ME_ADMIN__CALL1:
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
                    return ActionstepPackage.MEET_ME_ADMIN__CALL1;
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
        result.append(" (command: ");
        result.append(command);
        result.append(')');
        return result.toString();
    }
}
