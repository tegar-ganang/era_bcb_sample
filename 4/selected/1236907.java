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
import com.safi.asterisk.actionstep.ChanSpy;
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
 * An implementation of the model object '<em><b>Chan Spy</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.ChanSpyImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.ChanSpyImpl#getChannelnamePrefix <em>Channelname Prefix</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.ChanSpyImpl#isSpyBridgedOnly <em>Spy Bridged Only</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.ChanSpyImpl#getGroup <em>Group</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.ChanSpyImpl#isBeep <em>Beep</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.ChanSpyImpl#getRecordFilenamePrefix <em>Record Filename Prefix</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.ChanSpyImpl#getVolume <em>Volume</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.ChanSpyImpl#isWhisperEnabled <em>Whisper Enabled</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.ChanSpyImpl#isPrivateWhisperEnabled <em>Private Whisper Enabled</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ChanSpyImpl extends AsteriskActionStepImpl implements ChanSpy {

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
	 * The cached value of the '{@link #getChannelnamePrefix() <em>Channelname Prefix</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getChannelnamePrefix()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue channelnamePrefix;

    /**
	 * The default value of the '{@link #isSpyBridgedOnly() <em>Spy Bridged Only</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isSpyBridgedOnly()
	 * @generated
	 * @ordered
	 */
    protected static final boolean SPY_BRIDGED_ONLY_EDEFAULT = false;

    /**
	 * The cached value of the '{@link #isSpyBridgedOnly() <em>Spy Bridged Only</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isSpyBridgedOnly()
	 * @generated
	 * @ordered
	 */
    protected boolean spyBridgedOnly = SPY_BRIDGED_ONLY_EDEFAULT;

    /**
	 * The default value of the '{@link #getGroup() <em>Group</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getGroup()
	 * @generated
	 * @ordered
	 */
    protected static final String GROUP_EDEFAULT = null;

    /**
	 * The cached value of the '{@link #getGroup() <em>Group</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getGroup()
	 * @generated
	 * @ordered
	 */
    protected String group = GROUP_EDEFAULT;

    /**
	 * The default value of the '{@link #isBeep() <em>Beep</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isBeep()
	 * @generated
	 * @ordered
	 */
    protected static final boolean BEEP_EDEFAULT = true;

    /**
	 * The cached value of the '{@link #isBeep() <em>Beep</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isBeep()
	 * @generated
	 * @ordered
	 */
    protected boolean beep = BEEP_EDEFAULT;

    /**
	 * The default value of the '{@link #getRecordFilenamePrefix() <em>Record Filename Prefix</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getRecordFilenamePrefix()
	 * @generated
	 * @ordered
	 */
    protected static final String RECORD_FILENAME_PREFIX_EDEFAULT = null;

    /**
	 * The cached value of the '{@link #getRecordFilenamePrefix() <em>Record Filename Prefix</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getRecordFilenamePrefix()
	 * @generated
	 * @ordered
	 */
    protected String recordFilenamePrefix = RECORD_FILENAME_PREFIX_EDEFAULT;

    /**
	 * The default value of the '{@link #getVolume() <em>Volume</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getVolume()
	 * @generated
	 * @ordered
	 */
    protected static final int VOLUME_EDEFAULT = 0;

    /**
	 * The cached value of the '{@link #getVolume() <em>Volume</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getVolume()
	 * @generated
	 * @ordered
	 */
    protected int volume = VOLUME_EDEFAULT;

    /**
	 * The default value of the '{@link #isWhisperEnabled() <em>Whisper Enabled</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isWhisperEnabled()
	 * @generated
	 * @ordered
	 */
    protected static final boolean WHISPER_ENABLED_EDEFAULT = false;

    /**
	 * The cached value of the '{@link #isWhisperEnabled() <em>Whisper Enabled</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isWhisperEnabled()
	 * @generated
	 * @ordered
	 */
    protected boolean whisperEnabled = WHISPER_ENABLED_EDEFAULT;

    /**
	 * The default value of the '{@link #isPrivateWhisperEnabled() <em>Private Whisper Enabled</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isPrivateWhisperEnabled()
	 * @generated
	 * @ordered
	 */
    protected static final boolean PRIVATE_WHISPER_ENABLED_EDEFAULT = false;

    /**
	 * The cached value of the '{@link #isPrivateWhisperEnabled() <em>Private Whisper Enabled</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #isPrivateWhisperEnabled()
	 * @generated
	 * @ordered
	 */
    protected boolean privateWhisperEnabled = PRIVATE_WHISPER_ENABLED_EDEFAULT;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected ChanSpyImpl() {
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
            String channelName = (String) VariableTranslator.translateValue(VariableType.TEXT, resolveDynamicValue(channelnamePrefix, context));
            if (debugLog.isLoggable(Level.FINEST)) debug("Spying on channels starting with: " + channelName);
            StringBuffer appCmd = new StringBuffer();
            if (StringUtils.isNotBlank(channelName)) appCmd.append(channelName);
            appCmd.append('|');
            if (spyBridgedOnly) appCmd.append('b');
            if (StringUtils.isNotBlank(group)) appCmd.append("g(").append(group).append(')');
            if (!beep) appCmd.append('q');
            if (StringUtils.isNotBlank(recordFilenamePrefix)) appCmd.append("r[").append(recordFilenamePrefix).append(']');
            if (volume != 0) appCmd.append("v(").append(Math.min(4, Math.max(-4, volume))).append(')');
            if (privateWhisperEnabled) appCmd.append('W'); else if (whisperEnabled) appCmd.append('w');
            int result = channel.exec("Chanspy", appCmd.toString());
            if (debugLog.isLoggable(Level.FINEST)) debug("ChanSpy returned " + translateAppReturnValue(result) + " of int " + result);
            if (result == -2) {
                exception = new ActionStepException("Application ChansSpy not found");
            } else if (result == -1) {
                exception = new ActionStepException("Channel was hung up");
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
        return ActionstepPackage.Literals.CHAN_SPY;
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.CHAN_SPY__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.CHAN_SPY__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getChannelnamePrefix() {
        return channelnamePrefix;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetChannelnamePrefix(DynamicValue newChannelnamePrefix, NotificationChain msgs) {
        DynamicValue oldChannelnamePrefix = channelnamePrefix;
        channelnamePrefix = newChannelnamePrefix;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.CHAN_SPY__CHANNELNAME_PREFIX, oldChannelnamePrefix, newChannelnamePrefix);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setChannelnamePrefix(DynamicValue newChannelnamePrefix) {
        if (newChannelnamePrefix != channelnamePrefix) {
            NotificationChain msgs = null;
            if (channelnamePrefix != null) msgs = ((InternalEObject) channelnamePrefix).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.CHAN_SPY__CHANNELNAME_PREFIX, null, msgs);
            if (newChannelnamePrefix != null) msgs = ((InternalEObject) newChannelnamePrefix).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.CHAN_SPY__CHANNELNAME_PREFIX, null, msgs);
            msgs = basicSetChannelnamePrefix(newChannelnamePrefix, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.CHAN_SPY__CHANNELNAME_PREFIX, newChannelnamePrefix, newChannelnamePrefix));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isSpyBridgedOnly() {
        return spyBridgedOnly;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setSpyBridgedOnly(boolean newSpyBridgedOnly) {
        boolean oldSpyBridgedOnly = spyBridgedOnly;
        spyBridgedOnly = newSpyBridgedOnly;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.CHAN_SPY__SPY_BRIDGED_ONLY, oldSpyBridgedOnly, spyBridgedOnly));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public String getGroup() {
        return group;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setGroup(String newGroup) {
        String oldGroup = group;
        group = newGroup;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.CHAN_SPY__GROUP, oldGroup, group));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isBeep() {
        return beep;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setBeep(boolean newBeep) {
        boolean oldBeep = beep;
        beep = newBeep;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.CHAN_SPY__BEEP, oldBeep, beep));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public String getRecordFilenamePrefix() {
        return recordFilenamePrefix;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setRecordFilenamePrefix(String newRecordFilenamePrefix) {
        String oldRecordFilenamePrefix = recordFilenamePrefix;
        recordFilenamePrefix = newRecordFilenamePrefix;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.CHAN_SPY__RECORD_FILENAME_PREFIX, oldRecordFilenamePrefix, recordFilenamePrefix));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public int getVolume() {
        return volume;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setVolume(int newVolume) {
        int oldVolume = volume;
        volume = newVolume;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.CHAN_SPY__VOLUME, oldVolume, volume));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isWhisperEnabled() {
        return whisperEnabled;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setWhisperEnabled(boolean newWhisperEnabled) {
        boolean oldWhisperEnabled = whisperEnabled;
        whisperEnabled = newWhisperEnabled;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.CHAN_SPY__WHISPER_ENABLED, oldWhisperEnabled, whisperEnabled));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public boolean isPrivateWhisperEnabled() {
        return privateWhisperEnabled;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setPrivateWhisperEnabled(boolean newPrivateWhisperEnabled) {
        boolean oldPrivateWhisperEnabled = privateWhisperEnabled;
        privateWhisperEnabled = newPrivateWhisperEnabled;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.CHAN_SPY__PRIVATE_WHISPER_ENABLED, oldPrivateWhisperEnabled, privateWhisperEnabled));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.CHAN_SPY__CHANNELNAME_PREFIX:
                return basicSetChannelnamePrefix(null, msgs);
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
            case ActionstepPackage.CHAN_SPY__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.CHAN_SPY__CHANNELNAME_PREFIX:
                return getChannelnamePrefix();
            case ActionstepPackage.CHAN_SPY__SPY_BRIDGED_ONLY:
                return isSpyBridgedOnly();
            case ActionstepPackage.CHAN_SPY__GROUP:
                return getGroup();
            case ActionstepPackage.CHAN_SPY__BEEP:
                return isBeep();
            case ActionstepPackage.CHAN_SPY__RECORD_FILENAME_PREFIX:
                return getRecordFilenamePrefix();
            case ActionstepPackage.CHAN_SPY__VOLUME:
                return getVolume();
            case ActionstepPackage.CHAN_SPY__WHISPER_ENABLED:
                return isWhisperEnabled();
            case ActionstepPackage.CHAN_SPY__PRIVATE_WHISPER_ENABLED:
                return isPrivateWhisperEnabled();
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
            case ActionstepPackage.CHAN_SPY__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.CHAN_SPY__CHANNELNAME_PREFIX:
                setChannelnamePrefix((DynamicValue) newValue);
                return;
            case ActionstepPackage.CHAN_SPY__SPY_BRIDGED_ONLY:
                setSpyBridgedOnly((Boolean) newValue);
                return;
            case ActionstepPackage.CHAN_SPY__GROUP:
                setGroup((String) newValue);
                return;
            case ActionstepPackage.CHAN_SPY__BEEP:
                setBeep((Boolean) newValue);
                return;
            case ActionstepPackage.CHAN_SPY__RECORD_FILENAME_PREFIX:
                setRecordFilenamePrefix((String) newValue);
                return;
            case ActionstepPackage.CHAN_SPY__VOLUME:
                setVolume((Integer) newValue);
                return;
            case ActionstepPackage.CHAN_SPY__WHISPER_ENABLED:
                setWhisperEnabled((Boolean) newValue);
                return;
            case ActionstepPackage.CHAN_SPY__PRIVATE_WHISPER_ENABLED:
                setPrivateWhisperEnabled((Boolean) newValue);
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
            case ActionstepPackage.CHAN_SPY__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.CHAN_SPY__CHANNELNAME_PREFIX:
                setChannelnamePrefix((DynamicValue) null);
                return;
            case ActionstepPackage.CHAN_SPY__SPY_BRIDGED_ONLY:
                setSpyBridgedOnly(SPY_BRIDGED_ONLY_EDEFAULT);
                return;
            case ActionstepPackage.CHAN_SPY__GROUP:
                setGroup(GROUP_EDEFAULT);
                return;
            case ActionstepPackage.CHAN_SPY__BEEP:
                setBeep(BEEP_EDEFAULT);
                return;
            case ActionstepPackage.CHAN_SPY__RECORD_FILENAME_PREFIX:
                setRecordFilenamePrefix(RECORD_FILENAME_PREFIX_EDEFAULT);
                return;
            case ActionstepPackage.CHAN_SPY__VOLUME:
                setVolume(VOLUME_EDEFAULT);
                return;
            case ActionstepPackage.CHAN_SPY__WHISPER_ENABLED:
                setWhisperEnabled(WHISPER_ENABLED_EDEFAULT);
                return;
            case ActionstepPackage.CHAN_SPY__PRIVATE_WHISPER_ENABLED:
                setPrivateWhisperEnabled(PRIVATE_WHISPER_ENABLED_EDEFAULT);
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
            case ActionstepPackage.CHAN_SPY__CALL1:
                return call1 != null;
            case ActionstepPackage.CHAN_SPY__CHANNELNAME_PREFIX:
                return channelnamePrefix != null;
            case ActionstepPackage.CHAN_SPY__SPY_BRIDGED_ONLY:
                return spyBridgedOnly != SPY_BRIDGED_ONLY_EDEFAULT;
            case ActionstepPackage.CHAN_SPY__GROUP:
                return GROUP_EDEFAULT == null ? group != null : !GROUP_EDEFAULT.equals(group);
            case ActionstepPackage.CHAN_SPY__BEEP:
                return beep != BEEP_EDEFAULT;
            case ActionstepPackage.CHAN_SPY__RECORD_FILENAME_PREFIX:
                return RECORD_FILENAME_PREFIX_EDEFAULT == null ? recordFilenamePrefix != null : !RECORD_FILENAME_PREFIX_EDEFAULT.equals(recordFilenamePrefix);
            case ActionstepPackage.CHAN_SPY__VOLUME:
                return volume != VOLUME_EDEFAULT;
            case ActionstepPackage.CHAN_SPY__WHISPER_ENABLED:
                return whisperEnabled != WHISPER_ENABLED_EDEFAULT;
            case ActionstepPackage.CHAN_SPY__PRIVATE_WHISPER_ENABLED:
                return privateWhisperEnabled != PRIVATE_WHISPER_ENABLED_EDEFAULT;
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
                case ActionstepPackage.CHAN_SPY__CALL1:
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
                    return ActionstepPackage.CHAN_SPY__CALL1;
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
        result.append(" (spyBridgedOnly: ");
        result.append(spyBridgedOnly);
        result.append(", group: ");
        result.append(group);
        result.append(", beep: ");
        result.append(beep);
        result.append(", recordFilenamePrefix: ");
        result.append(recordFilenamePrefix);
        result.append(", volume: ");
        result.append(volume);
        result.append(", whisperEnabled: ");
        result.append(whisperEnabled);
        result.append(", privateWhisperEnabled: ");
        result.append(privateWhisperEnabled);
        result.append(')');
        return result.toString();
    }
}
