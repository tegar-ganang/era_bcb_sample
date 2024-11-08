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
import com.safi.asterisk.actionstep.VoicemailMain;
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
 * An implementation of the model object '<em><b>Voicemail Main</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.VoicemailMainImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.VoicemailMainImpl#getMailbox <em>Mailbox</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.VoicemailMainImpl#isSkipPasswordCheck <em>Skip Password Check</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.VoicemailMainImpl#isUsePrefix <em>Use Prefix</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.VoicemailMainImpl#getRecordingGain <em>Recording Gain</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.VoicemailMainImpl#getDefaultFolder <em>Default Folder</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class VoicemailMainImpl extends AsteriskActionStepImpl implements VoicemailMain {

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
	 * The cached value of the '{@link #getMailbox() <em>Mailbox</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getMailbox()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue mailbox;

    /**
	 * The default value of the '{@link #isSkipPasswordCheck() <em>Skip Password Check</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isSkipPasswordCheck()
	 * @generated
	 * @ordered
	 */
    protected static final boolean SKIP_PASSWORD_CHECK_EDEFAULT = false;

    /**
	 * The cached value of the '{@link #isSkipPasswordCheck() <em>Skip Password Check</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isSkipPasswordCheck()
	 * @generated
	 * @ordered
	 */
    protected boolean skipPasswordCheck = SKIP_PASSWORD_CHECK_EDEFAULT;

    /**
	 * The default value of the '{@link #isUsePrefix() <em>Use Prefix</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isUsePrefix()
	 * @generated
	 * @ordered
	 */
    protected static final boolean USE_PREFIX_EDEFAULT = false;

    /**
	 * The cached value of the '{@link #isUsePrefix() <em>Use Prefix</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isUsePrefix()
	 * @generated
	 * @ordered
	 */
    protected boolean usePrefix = USE_PREFIX_EDEFAULT;

    /**
	 * The default value of the '{@link #getRecordingGain() <em>Recording Gain</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getRecordingGain()
	 * @generated
	 * @ordered
	 */
    protected static final int RECORDING_GAIN_EDEFAULT = 0;

    /**
	 * The cached value of the '{@link #getRecordingGain() <em>Recording Gain</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getRecordingGain()
	 * @generated
	 * @ordered
	 */
    protected int recordingGain = RECORDING_GAIN_EDEFAULT;

    /**
	 * The cached value of the '{@link #getDefaultFolder() <em>Default Folder</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getDefaultFolder()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue defaultFolder;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected VoicemailMainImpl() {
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
            String mb = (String) VariableTranslator.translateValue(VariableType.TEXT, resolveDynamicValue(mailbox, context));
            if (debugLog.isLoggable(Level.FINEST)) debug("Getting VoicemailMain for mailbox: " + mb);
            if (StringUtils.isBlank(mb)) {
                exception = new ActionStepException("mailbox is required for VoicemailMain");
            } else {
                String folder = (String) VariableTranslator.translateValue(VariableType.TEXT, resolveDynamicValue(this.defaultFolder, context));
                if (StringUtils.isBlank(folder)) {
                    folder = null;
                }
                StringBuffer appCmd = new StringBuffer();
                appCmd.append(mb);
                if (skipPasswordCheck) appCmd.append("|s");
                if (usePrefix) appCmd.append("|p");
                if (recordingGain != 0) appCmd.append("|g(").append(recordingGain).append(")");
                if (folder != null) appCmd.append("|a(").append(folder).append(")");
                if (debugLog.isLoggable(Level.FINEST)) {
                    debug("sending: VoiceMailMain " + appCmd);
                }
                int result = channel.exec("VoiceMailMain", appCmd.toString());
                if (debugLog.isLoggable(Level.FINEST)) debug("VoiceMailMain returned " + translateAppReturnValue(result) + " of int " + result);
                if (result == -2) {
                    exception = new ActionStepException("Application VoiceMailMain not found");
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

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return ActionstepPackage.Literals.VOICEMAIL_MAIN;
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.VOICEMAIL_MAIN__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.VOICEMAIL_MAIN__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getMailbox() {
        return mailbox;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetMailbox(DynamicValue newMailbox, NotificationChain msgs) {
        DynamicValue oldMailbox = mailbox;
        mailbox = newMailbox;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.VOICEMAIL_MAIN__MAILBOX, oldMailbox, newMailbox);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setMailbox(DynamicValue newMailbox) {
        if (newMailbox != mailbox) {
            NotificationChain msgs = null;
            if (mailbox != null) msgs = ((InternalEObject) mailbox).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.VOICEMAIL_MAIN__MAILBOX, null, msgs);
            if (newMailbox != null) msgs = ((InternalEObject) newMailbox).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.VOICEMAIL_MAIN__MAILBOX, null, msgs);
            msgs = basicSetMailbox(newMailbox, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.VOICEMAIL_MAIN__MAILBOX, newMailbox, newMailbox));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isSkipPasswordCheck() {
        return skipPasswordCheck;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setSkipPasswordCheck(boolean newSkipPasswordCheck) {
        boolean oldSkipPasswordCheck = skipPasswordCheck;
        skipPasswordCheck = newSkipPasswordCheck;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.VOICEMAIL_MAIN__SKIP_PASSWORD_CHECK, oldSkipPasswordCheck, skipPasswordCheck));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isUsePrefix() {
        return usePrefix;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setUsePrefix(boolean newUsePrefix) {
        boolean oldUsePrefix = usePrefix;
        usePrefix = newUsePrefix;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.VOICEMAIL_MAIN__USE_PREFIX, oldUsePrefix, usePrefix));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public int getRecordingGain() {
        return recordingGain;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setRecordingGain(int newRecordingGain) {
        int oldRecordingGain = recordingGain;
        recordingGain = newRecordingGain;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.VOICEMAIL_MAIN__RECORDING_GAIN, oldRecordingGain, recordingGain));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getDefaultFolder() {
        return defaultFolder;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetDefaultFolder(DynamicValue newDefaultFolder, NotificationChain msgs) {
        DynamicValue oldDefaultFolder = defaultFolder;
        defaultFolder = newDefaultFolder;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.VOICEMAIL_MAIN__DEFAULT_FOLDER, oldDefaultFolder, newDefaultFolder);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setDefaultFolder(DynamicValue newDefaultFolder) {
        if (newDefaultFolder != defaultFolder) {
            NotificationChain msgs = null;
            if (defaultFolder != null) msgs = ((InternalEObject) defaultFolder).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.VOICEMAIL_MAIN__DEFAULT_FOLDER, null, msgs);
            if (newDefaultFolder != null) msgs = ((InternalEObject) newDefaultFolder).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.VOICEMAIL_MAIN__DEFAULT_FOLDER, null, msgs);
            msgs = basicSetDefaultFolder(newDefaultFolder, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.VOICEMAIL_MAIN__DEFAULT_FOLDER, newDefaultFolder, newDefaultFolder));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.VOICEMAIL_MAIN__MAILBOX:
                return basicSetMailbox(null, msgs);
            case ActionstepPackage.VOICEMAIL_MAIN__DEFAULT_FOLDER:
                return basicSetDefaultFolder(null, msgs);
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
            case ActionstepPackage.VOICEMAIL_MAIN__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.VOICEMAIL_MAIN__MAILBOX:
                return getMailbox();
            case ActionstepPackage.VOICEMAIL_MAIN__SKIP_PASSWORD_CHECK:
                return isSkipPasswordCheck();
            case ActionstepPackage.VOICEMAIL_MAIN__USE_PREFIX:
                return isUsePrefix();
            case ActionstepPackage.VOICEMAIL_MAIN__RECORDING_GAIN:
                return getRecordingGain();
            case ActionstepPackage.VOICEMAIL_MAIN__DEFAULT_FOLDER:
                return getDefaultFolder();
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
            case ActionstepPackage.VOICEMAIL_MAIN__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.VOICEMAIL_MAIN__MAILBOX:
                setMailbox((DynamicValue) newValue);
                return;
            case ActionstepPackage.VOICEMAIL_MAIN__SKIP_PASSWORD_CHECK:
                setSkipPasswordCheck((Boolean) newValue);
                return;
            case ActionstepPackage.VOICEMAIL_MAIN__USE_PREFIX:
                setUsePrefix((Boolean) newValue);
                return;
            case ActionstepPackage.VOICEMAIL_MAIN__RECORDING_GAIN:
                setRecordingGain((Integer) newValue);
                return;
            case ActionstepPackage.VOICEMAIL_MAIN__DEFAULT_FOLDER:
                setDefaultFolder((DynamicValue) newValue);
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
            case ActionstepPackage.VOICEMAIL_MAIN__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.VOICEMAIL_MAIN__MAILBOX:
                setMailbox((DynamicValue) null);
                return;
            case ActionstepPackage.VOICEMAIL_MAIN__SKIP_PASSWORD_CHECK:
                setSkipPasswordCheck(SKIP_PASSWORD_CHECK_EDEFAULT);
                return;
            case ActionstepPackage.VOICEMAIL_MAIN__USE_PREFIX:
                setUsePrefix(USE_PREFIX_EDEFAULT);
                return;
            case ActionstepPackage.VOICEMAIL_MAIN__RECORDING_GAIN:
                setRecordingGain(RECORDING_GAIN_EDEFAULT);
                return;
            case ActionstepPackage.VOICEMAIL_MAIN__DEFAULT_FOLDER:
                setDefaultFolder((DynamicValue) null);
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
            case ActionstepPackage.VOICEMAIL_MAIN__CALL1:
                return call1 != null;
            case ActionstepPackage.VOICEMAIL_MAIN__MAILBOX:
                return mailbox != null;
            case ActionstepPackage.VOICEMAIL_MAIN__SKIP_PASSWORD_CHECK:
                return skipPasswordCheck != SKIP_PASSWORD_CHECK_EDEFAULT;
            case ActionstepPackage.VOICEMAIL_MAIN__USE_PREFIX:
                return usePrefix != USE_PREFIX_EDEFAULT;
            case ActionstepPackage.VOICEMAIL_MAIN__RECORDING_GAIN:
                return recordingGain != RECORDING_GAIN_EDEFAULT;
            case ActionstepPackage.VOICEMAIL_MAIN__DEFAULT_FOLDER:
                return defaultFolder != null;
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
                case ActionstepPackage.VOICEMAIL_MAIN__CALL1:
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
                    return ActionstepPackage.VOICEMAIL_MAIN__CALL1;
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
        result.append(" (skipPasswordCheck: ");
        result.append(skipPasswordCheck);
        result.append(", usePrefix: ");
        result.append(usePrefix);
        result.append(", recordingGain: ");
        result.append(recordingGain);
        result.append(')');
        return result.toString();
    }
}
