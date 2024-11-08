package org.enml.measures.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.enml.measures.LevelHistory;
import org.enml.measures.MeasuresPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Level History</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.enml.measures.impl.LevelHistoryImpl#getDescriptor <em>Descriptor</em>}</li>
 *   <li>{@link org.enml.measures.impl.LevelHistoryImpl#getChannel <em>Channel</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class LevelHistoryImpl extends TimeHistoryImpl implements LevelHistory {

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final String copyright = "enml.org (C) 2007";

    /**
	 * The default value of the '{@link #getDescriptor() <em>Descriptor</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescriptor()
	 * @generated
	 * @ordered
	 */
    protected static final long DESCRIPTOR_EDEFAULT = 0L;

    /**
	 * The cached value of the '{@link #getDescriptor() <em>Descriptor</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDescriptor()
	 * @generated
	 * @ordered
	 */
    protected long descriptor = DESCRIPTOR_EDEFAULT;

    /**
	 * The default value of the '{@link #getChannel() <em>Channel</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getChannel()
	 * @generated
	 * @ordered
	 */
    protected static final int CHANNEL_EDEFAULT = 0;

    /**
	 * The cached value of the '{@link #getChannel() <em>Channel</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getChannel()
	 * @generated
	 * @ordered
	 */
    protected int channel = CHANNEL_EDEFAULT;

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    protected LevelHistoryImpl() {
        super();
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return MeasuresPackage.Literals.LEVEL_HISTORY;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public long getDescriptor() {
        return descriptor;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public void setDescriptor(long newDescriptor) {
        long oldDescriptor = descriptor;
        descriptor = newDescriptor;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, MeasuresPackage.LEVEL_HISTORY__DESCRIPTOR, oldDescriptor, descriptor));
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public int getChannel() {
        return channel;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public void setChannel(int newChannel) {
        int oldChannel = channel;
        channel = newChannel;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, MeasuresPackage.LEVEL_HISTORY__CHANNEL, oldChannel, channel));
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch(featureID) {
            case MeasuresPackage.LEVEL_HISTORY__DESCRIPTOR:
                return getDescriptor();
            case MeasuresPackage.LEVEL_HISTORY__CHANNEL:
                return getChannel();
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
            case MeasuresPackage.LEVEL_HISTORY__DESCRIPTOR:
                setDescriptor((Long) newValue);
                return;
            case MeasuresPackage.LEVEL_HISTORY__CHANNEL:
                setChannel((Integer) newValue);
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
            case MeasuresPackage.LEVEL_HISTORY__DESCRIPTOR:
                setDescriptor(DESCRIPTOR_EDEFAULT);
                return;
            case MeasuresPackage.LEVEL_HISTORY__CHANNEL:
                setChannel(CHANNEL_EDEFAULT);
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
            case MeasuresPackage.LEVEL_HISTORY__DESCRIPTOR:
                return descriptor != DESCRIPTOR_EDEFAULT;
            case MeasuresPackage.LEVEL_HISTORY__CHANNEL:
                return channel != CHANNEL_EDEFAULT;
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
        result.append(" (descriptor: ");
        result.append(descriptor);
        result.append(", channel: ");
        result.append(channel);
        result.append(')');
        return result.toString();
    }
}
