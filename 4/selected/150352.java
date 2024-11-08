package com.safi.asterisk.actionstep.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.lang.StringUtils;
import org.asteriskjava.fastagi.AgiChannel;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;
import com.safi.asterisk.Call;
import com.safi.asterisk.actionstep.ActionstepPackage;
import com.safi.asterisk.actionstep.AudioFileItem;
import com.safi.asterisk.actionstep.MultiStreamAudio;
import com.safi.asterisk.saflet.AsteriskSafletContext;
import com.safi.core.actionstep.ActionStepException;
import com.safi.core.actionstep.CaseItem;
import com.safi.core.actionstep.DynamicValue;
import com.safi.core.actionstep.DynamicValueType;
import com.safi.core.call.CallConsumer1;
import com.safi.core.call.CallPackage;
import com.safi.core.call.SafiCall;
import com.safi.core.saflet.SafletContext;
import com.safi.db.VariableType;
import com.safi.db.util.VariableTranslator;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Multi Stream Audio</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.actionstep.impl.MultiStreamAudioImpl#getCall1 <em>Call1</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.MultiStreamAudioImpl#getEscapeDigits <em>Escape Digits</em>}</li>
 *   <li>{@link com.safi.asterisk.actionstep.impl.MultiStreamAudioImpl#getAudioFilenames <em>Audio Filenames</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class MultiStreamAudioImpl extends AsteriskActionStepImpl implements MultiStreamAudio {

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
	 * The cached value of the '{@link #getEscapeDigits() <em>Escape Digits</em>}' containment reference.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getEscapeDigits()
	 * @generated
	 * @ordered
	 */
    protected DynamicValue escapeDigits;

    /**
	 * The cached value of the '{@link #getAudioFilenames() <em>Audio Filenames</em>}' containment reference list.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getAudioFilenames()
	 * @generated
	 * @ordered
	 */
    protected EList<AudioFileItem> audioFilenames;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected MultiStreamAudioImpl() {
        super();
    }

    /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated NOT
   */
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
            List<String> files = new ArrayList<String>();
            if (audioFilenames != null && !audioFilenames.isEmpty()) {
                for (CaseItem item : audioFilenames) {
                    Object dynValue = resolveDynamicValue(item.getDynamicValue(), context);
                    String filenameStr = (String) VariableTranslator.translateValue(VariableType.TEXT, dynValue);
                    if (StringUtils.isBlank(filenameStr)) continue;
                    if (item.getDynamicValue().getType() == DynamicValueType.CUSTOM) {
                        filenameStr = getSaflet().getPromptPathByName(filenameStr);
                    }
                    files.add(filenameStr);
                }
                if (files.isEmpty()) exception = new ActionStepException("No Filenames Specified"); else {
                    if (debugLog.isLoggable(Level.FINEST)) debug("Streaming files " + files);
                    StringBuffer appCmd = new StringBuffer();
                    for (int i = 0; i < files.size(); i++) {
                        if (i > 0) appCmd.append('&');
                        appCmd.append(files.get(i));
                    }
                    char c = (char) channel.exec("Background", appCmd.toString());
                    if (c > 0) {
                        ((AsteriskSafletContext) context).appendBufferedDigits(String.valueOf(c));
                    } else if (c == -1) {
                        exception = new ActionStepException("Channel was hung up.");
                    }
                    if (debugLog.isLoggable(Level.FINEST)) debug("StreamAudio returned " + translateAppReturnValue(c));
                }
            } else exception = new ActionStepException("No Filenames Specified");
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
        return ActionstepPackage.Literals.MULTI_STREAM_AUDIO;
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
                if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.RESOLVE, ActionstepPackage.MULTI_STREAM_AUDIO__CALL1, oldCall1, call1));
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
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.MULTI_STREAM_AUDIO__CALL1, oldCall1, call1));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public DynamicValue getEscapeDigits() {
        return escapeDigits;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public NotificationChain basicSetEscapeDigits(DynamicValue newEscapeDigits, NotificationChain msgs) {
        DynamicValue oldEscapeDigits = escapeDigits;
        escapeDigits = newEscapeDigits;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, ActionstepPackage.MULTI_STREAM_AUDIO__ESCAPE_DIGITS, oldEscapeDigits, newEscapeDigits);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setEscapeDigits(DynamicValue newEscapeDigits) {
        if (newEscapeDigits != escapeDigits) {
            NotificationChain msgs = null;
            if (escapeDigits != null) msgs = ((InternalEObject) escapeDigits).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.MULTI_STREAM_AUDIO__ESCAPE_DIGITS, null, msgs);
            if (newEscapeDigits != null) msgs = ((InternalEObject) newEscapeDigits).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - ActionstepPackage.MULTI_STREAM_AUDIO__ESCAPE_DIGITS, null, msgs);
            msgs = basicSetEscapeDigits(newEscapeDigits, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, ActionstepPackage.MULTI_STREAM_AUDIO__ESCAPE_DIGITS, newEscapeDigits, newEscapeDigits));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public EList<AudioFileItem> getAudioFilenames() {
        if (audioFilenames == null) {
            audioFilenames = new EObjectContainmentEList<AudioFileItem>(AudioFileItem.class, this, ActionstepPackage.MULTI_STREAM_AUDIO__AUDIO_FILENAMES);
        }
        return audioFilenames;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case ActionstepPackage.MULTI_STREAM_AUDIO__ESCAPE_DIGITS:
                return basicSetEscapeDigits(null, msgs);
            case ActionstepPackage.MULTI_STREAM_AUDIO__AUDIO_FILENAMES:
                return ((InternalEList<?>) getAudioFilenames()).basicRemove(otherEnd, msgs);
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
            case ActionstepPackage.MULTI_STREAM_AUDIO__CALL1:
                if (resolve) return getCall1();
                return basicGetCall1();
            case ActionstepPackage.MULTI_STREAM_AUDIO__ESCAPE_DIGITS:
                return getEscapeDigits();
            case ActionstepPackage.MULTI_STREAM_AUDIO__AUDIO_FILENAMES:
                return getAudioFilenames();
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
            case ActionstepPackage.MULTI_STREAM_AUDIO__CALL1:
                setCall1((SafiCall) newValue);
                return;
            case ActionstepPackage.MULTI_STREAM_AUDIO__ESCAPE_DIGITS:
                setEscapeDigits((DynamicValue) newValue);
                return;
            case ActionstepPackage.MULTI_STREAM_AUDIO__AUDIO_FILENAMES:
                getAudioFilenames().clear();
                getAudioFilenames().addAll((Collection<? extends AudioFileItem>) newValue);
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
            case ActionstepPackage.MULTI_STREAM_AUDIO__CALL1:
                setCall1((SafiCall) null);
                return;
            case ActionstepPackage.MULTI_STREAM_AUDIO__ESCAPE_DIGITS:
                setEscapeDigits((DynamicValue) null);
                return;
            case ActionstepPackage.MULTI_STREAM_AUDIO__AUDIO_FILENAMES:
                getAudioFilenames().clear();
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
            case ActionstepPackage.MULTI_STREAM_AUDIO__CALL1:
                return call1 != null;
            case ActionstepPackage.MULTI_STREAM_AUDIO__ESCAPE_DIGITS:
                return escapeDigits != null;
            case ActionstepPackage.MULTI_STREAM_AUDIO__AUDIO_FILENAMES:
                return audioFilenames != null && !audioFilenames.isEmpty();
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
                case ActionstepPackage.MULTI_STREAM_AUDIO__CALL1:
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
                    return ActionstepPackage.MULTI_STREAM_AUDIO__CALL1;
                default:
                    return -1;
            }
        }
        return super.eDerivedStructuralFeatureID(baseFeatureID, baseClass);
    }
}
