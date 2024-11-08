package com.safi.asterisk.impl;

import java.util.HashMap;
import java.util.Map;
import org.asteriskjava.fastagi.AgiChannel;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import com.safi.asterisk.AsteriskPackage;
import com.safi.asterisk.Call;
import com.safi.asterisk.CallState;
import com.safi.asterisk.util.AsteriskSafletConstants;
import com.safi.core.call.impl.SafiCallImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Call</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.safi.asterisk.impl.CallImpl#getChannel <em>Channel</em>}</li>
 *   <li>{@link com.safi.asterisk.impl.CallImpl#getCallerIdName <em>Caller Id Name</em>}</li>
 *   <li>{@link com.safi.asterisk.impl.CallImpl#getCallerIdNum <em>Caller Id Num</em>}</li>
 *   <li>{@link com.safi.asterisk.impl.CallImpl#getUniqueId <em>Unique Id</em>}</li>
 *   <li>{@link com.safi.asterisk.impl.CallImpl#getChannelName <em>Channel Name</em>}</li>
 *   <li>{@link com.safi.asterisk.impl.CallImpl#getCallState <em>Call State</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class CallImpl extends SafiCallImpl implements Call {

    /**
	 * The default value of the '{@link #getChannel() <em>Channel</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getChannel()
	 * @generated
	 * @ordered
	 */
    protected static final AgiChannel CHANNEL_EDEFAULT = null;

    /**
	 * The default value of the '{@link #getCallerIdName() <em>Caller Id Name</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getCallerIdName()
	 * @generated
	 * @ordered
	 */
    protected static final String CALLER_ID_NAME_EDEFAULT = null;

    /**
	 * The cached value of the '{@link #getCallerIdName() <em>Caller Id Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCallerIdName()
	 * @generated
	 * @ordered
	 */
    protected String callerIdName = CALLER_ID_NAME_EDEFAULT;

    /**
	 * The default value of the '{@link #getCallerIdNum() <em>Caller Id Num</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getCallerIdNum()
	 * @generated
	 * @ordered
	 */
    protected static final String CALLER_ID_NUM_EDEFAULT = null;

    /**
	 * The cached value of the '{@link #getCallerIdNum() <em>Caller Id Num</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCallerIdNum()
	 * @generated
	 * @ordered
	 */
    protected String callerIdNum = CALLER_ID_NUM_EDEFAULT;

    /**
	 * The default value of the '{@link #getUniqueId() <em>Unique Id</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getUniqueId()
	 * @generated
	 * @ordered
	 */
    protected static final String UNIQUE_ID_EDEFAULT = null;

    /**
	 * The cached value of the '{@link #getUniqueId() <em>Unique Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUniqueId()
	 * @generated
	 * @ordered
	 */
    protected String uniqueId = UNIQUE_ID_EDEFAULT;

    /**
	 * The default value of the '{@link #getChannelName() <em>Channel Name</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getChannelName()
	 * @generated
	 * @ordered
	 */
    protected static final String CHANNEL_NAME_EDEFAULT = null;

    /**
	 * The cached value of the '{@link #getChannelName() <em>Channel Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getChannelName()
	 * @generated
	 * @ordered
	 */
    protected String channelName = CHANNEL_NAME_EDEFAULT;

    /**
	 * The default value of the '{@link #getCallState() <em>Call State</em>}' attribute.
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @see #getCallState()
	 * @generated
	 * @ordered
	 */
    protected static final CallState CALL_STATE_EDEFAULT = CallState.AVAILABLE;

    /**
	 * The cached value of the '{@link #getCallState() <em>Call State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCallState()
	 * @generated
	 * @ordered
	 */
    protected CallState callState = CALL_STATE_EDEFAULT;

    private Map<String, Object> dataMap;

    private AgiChannel channel;

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    protected CallImpl() {
        super();
    }

    @Override
    public void cleanup() {
        super.cleanup();
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return AsteriskPackage.Literals.CALL;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated NOT
	 */
    public AgiChannel getChannel() {
        return channel;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated NOT
	 */
    public void setChannel(AgiChannel newChannel) {
        channel = newChannel;
        if (newChannel == null) setChannelName(null); else {
            try {
                setChannelName(newChannel.getName());
            } catch (Exception e) {
                setChannelName(null);
            }
        }
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public String getCallerIdName() {
        return callerIdName;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setCallerIdName(String newCallerIdName) {
        String oldCallerIdName = callerIdName;
        callerIdName = newCallerIdName;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, AsteriskPackage.CALL__CALLER_ID_NAME, oldCallerIdName, callerIdName));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public String getCallerIdNum() {
        return callerIdNum;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setCallerIdNum(String newCallerIdNum) {
        String oldCallerIdNum = callerIdNum;
        callerIdNum = newCallerIdNum;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, AsteriskPackage.CALL__CALLER_ID_NUM, oldCallerIdNum, callerIdNum));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setUniqueId(String newUniqueId) {
        String oldUniqueId = uniqueId;
        uniqueId = newUniqueId;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, AsteriskPackage.CALL__UNIQUE_ID, oldUniqueId, uniqueId));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public String getChannelName() {
        return channelName;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setChannelName(String newChannelName) {
        String oldChannelName = channelName;
        channelName = newChannelName;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, AsteriskPackage.CALL__CHANNEL_NAME, oldChannelName, channelName));
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public CallState getCallState() {
        return callState;
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    public void setCallState(CallState newCallState) {
        CallState oldCallState = callState;
        callState = newCallState == null ? CALL_STATE_EDEFAULT : newCallState;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, AsteriskPackage.CALL__CALL_STATE, oldCallState, callState));
    }

    /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated NOT
   */
    public void setData(String name, Object value) {
        if (dataMap == null) dataMap = new HashMap<String, Object>();
        dataMap.put(name, value);
    }

    /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated NOT
   */
    public Object getData(String name) {
        return dataMap == null ? null : dataMap.get(name);
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch(featureID) {
            case AsteriskPackage.CALL__CHANNEL:
                return getChannel();
            case AsteriskPackage.CALL__CALLER_ID_NAME:
                return getCallerIdName();
            case AsteriskPackage.CALL__CALLER_ID_NUM:
                return getCallerIdNum();
            case AsteriskPackage.CALL__UNIQUE_ID:
                return getUniqueId();
            case AsteriskPackage.CALL__CHANNEL_NAME:
                return getChannelName();
            case AsteriskPackage.CALL__CALL_STATE:
                return getCallState();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    /**
	 * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public void eSet(int featureID, Object newValue) {
        switch(featureID) {
            case AsteriskPackage.CALL__CHANNEL:
                setChannel((AgiChannel) newValue);
                return;
            case AsteriskPackage.CALL__CALLER_ID_NAME:
                setCallerIdName((String) newValue);
                return;
            case AsteriskPackage.CALL__CALLER_ID_NUM:
                setCallerIdNum((String) newValue);
                return;
            case AsteriskPackage.CALL__UNIQUE_ID:
                setUniqueId((String) newValue);
                return;
            case AsteriskPackage.CALL__CHANNEL_NAME:
                setChannelName((String) newValue);
                return;
            case AsteriskPackage.CALL__CALL_STATE:
                setCallState((CallState) newValue);
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
            case AsteriskPackage.CALL__CHANNEL:
                setChannel(CHANNEL_EDEFAULT);
                return;
            case AsteriskPackage.CALL__CALLER_ID_NAME:
                setCallerIdName(CALLER_ID_NAME_EDEFAULT);
                return;
            case AsteriskPackage.CALL__CALLER_ID_NUM:
                setCallerIdNum(CALLER_ID_NUM_EDEFAULT);
                return;
            case AsteriskPackage.CALL__UNIQUE_ID:
                setUniqueId(UNIQUE_ID_EDEFAULT);
                return;
            case AsteriskPackage.CALL__CHANNEL_NAME:
                setChannelName(CHANNEL_NAME_EDEFAULT);
                return;
            case AsteriskPackage.CALL__CALL_STATE:
                setCallState(CALL_STATE_EDEFAULT);
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
            case AsteriskPackage.CALL__CHANNEL:
                return CHANNEL_EDEFAULT == null ? getChannel() != null : !CHANNEL_EDEFAULT.equals(getChannel());
            case AsteriskPackage.CALL__CALLER_ID_NAME:
                return CALLER_ID_NAME_EDEFAULT == null ? callerIdName != null : !CALLER_ID_NAME_EDEFAULT.equals(callerIdName);
            case AsteriskPackage.CALL__CALLER_ID_NUM:
                return CALLER_ID_NUM_EDEFAULT == null ? callerIdNum != null : !CALLER_ID_NUM_EDEFAULT.equals(callerIdNum);
            case AsteriskPackage.CALL__UNIQUE_ID:
                return UNIQUE_ID_EDEFAULT == null ? uniqueId != null : !UNIQUE_ID_EDEFAULT.equals(uniqueId);
            case AsteriskPackage.CALL__CHANNEL_NAME:
                return CHANNEL_NAME_EDEFAULT == null ? channelName != null : !CHANNEL_NAME_EDEFAULT.equals(channelName);
            case AsteriskPackage.CALL__CALL_STATE:
                return callState != CALL_STATE_EDEFAULT;
        }
        return super.eIsSet(featureID);
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
        result.append(" (callerIdName: ");
        result.append(callerIdName);
        result.append(", callerIdNum: ");
        result.append(callerIdNum);
        result.append(", uniqueId: ");
        result.append(uniqueId);
        result.append(", channelName: ");
        result.append(channelName);
        result.append(", callState: ");
        result.append(callState);
        result.append(')');
        return result.toString();
    }

    @Override
    public String getPlatformID() {
        return AsteriskSafletConstants.PLATFORM_ID;
    }
}
