package net.sf.ncsimulator.models.network;

import java.util.Collection;
import java.util.List;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentWithInverseEList;
import org.eclipse.emf.ecore.util.EObjectWithInverseResolvingEList;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Network</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link net.sf.ncsimulator.models.network.Network#getNodes <em>Nodes</em>}</li>
 *   <li>{@link net.sf.ncsimulator.models.network.Network#getName <em>Name</em>}</li>
 *   <li>{@link net.sf.ncsimulator.models.network.Network#getChannels <em>Channels</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class Network extends EObjectImpl implements EObject {

    /**
	 * The cached value of the '{@link #getNodes() <em>Nodes</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getNodes()
	 * @generated
	 * @ordered
	 */
    protected EList<Node> nodes;

    /**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
    protected static final String NAME_EDEFAULT = null;

    /**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
    protected String name = NAME_EDEFAULT;

    /**
	 * The cached value of the '{@link #getChannels() <em>Channels</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getChannels()
	 * @generated
	 * @ordered
	 */
    protected EList<Channel> channels;

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    protected Network() {
        super();
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return NetworkPackage.Literals.NETWORK;
    }

    /**
	 * Returns the value of the '<em><b>Nodes</b></em>' containment reference list.
	 * The list contents are of type {@link net.sf.ncsimulator.models.network.Node}.
	 * It is bidirectional and its opposite is '{@link net.sf.ncsimulator.models.network.Node#getNetwork <em>Network</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Nodes</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Nodes</em>' containment reference list.
	 * @see net.sf.ncsimulator.models.network.Node#getNetwork
	 * @generated
	 */
    public List<Node> getNodes() {
        if (nodes == null) {
            nodes = new EObjectContainmentWithInverseEList<Node>(Node.class, this, NetworkPackage.NETWORK__NODES, NetworkPackage.NODE__NETWORK);
        }
        return nodes;
    }

    /**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Name</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @generated
	 */
    public String getName() {
        return name;
    }

    /**
	 * Sets the value of the '{@link net.sf.ncsimulator.models.network.Network#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
    public void setName(String newName) {
        String oldName = name;
        name = newName;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, NetworkPackage.NETWORK__NAME, oldName, name));
    }

    /**
	 * Returns the value of the '<em><b>Channels</b></em>' containment reference list.
	 * The list contents are of type {@link net.sf.ncsimulator.models.network.Channel}.
	 * It is bidirectional and its opposite is '{@link net.sf.ncsimulator.models.network.Channel#getNetwork <em>Network</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Channels</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Channels</em>' containment reference list.
	 * @see net.sf.ncsimulator.models.network.Channel#getNetwork
	 * @generated
	 */
    public List<Channel> getChannels() {
        if (channels == null) {
            channels = new EObjectContainmentWithInverseEList<Channel>(Channel.class, this, NetworkPackage.NETWORK__CHANNELS, NetworkPackage.CHANNEL__NETWORK);
        }
        return channels;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    @SuppressWarnings("unchecked")
    @Override
    public NotificationChain eInverseAdd(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case NetworkPackage.NETWORK__NODES:
                return ((InternalEList<InternalEObject>) (InternalEList<?>) getNodes()).basicAdd(otherEnd, msgs);
            case NetworkPackage.NETWORK__CHANNELS:
                return ((InternalEList<InternalEObject>) (InternalEList<?>) getChannels()).basicAdd(otherEnd, msgs);
        }
        return super.eInverseAdd(otherEnd, featureID, msgs);
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch(featureID) {
            case NetworkPackage.NETWORK__NODES:
                return ((InternalEList<?>) getNodes()).basicRemove(otherEnd, msgs);
            case NetworkPackage.NETWORK__CHANNELS:
                return ((InternalEList<?>) getChannels()).basicRemove(otherEnd, msgs);
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
            case NetworkPackage.NETWORK__NODES:
                return getNodes();
            case NetworkPackage.NETWORK__NAME:
                return getName();
            case NetworkPackage.NETWORK__CHANNELS:
                return getChannels();
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
            case NetworkPackage.NETWORK__NODES:
                getNodes().clear();
                getNodes().addAll((Collection<? extends Node>) newValue);
                return;
            case NetworkPackage.NETWORK__NAME:
                setName((String) newValue);
                return;
            case NetworkPackage.NETWORK__CHANNELS:
                getChannels().clear();
                getChannels().addAll((Collection<? extends Channel>) newValue);
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
            case NetworkPackage.NETWORK__NODES:
                getNodes().clear();
                return;
            case NetworkPackage.NETWORK__NAME:
                setName(NAME_EDEFAULT);
                return;
            case NetworkPackage.NETWORK__CHANNELS:
                getChannels().clear();
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
            case NetworkPackage.NETWORK__NODES:
                return nodes != null && !nodes.isEmpty();
            case NetworkPackage.NETWORK__NAME:
                return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
            case NetworkPackage.NETWORK__CHANNELS:
                return channels != null && !channels.isEmpty();
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
        result.append(" (Name: ");
        result.append(name);
        result.append(')');
        return result.toString();
    }
}
