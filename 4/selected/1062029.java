package net.sf.ncsimulator.models.network;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.impl.EPackageImpl;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see net.sf.ncsimulator.models.network.NetworkFactory
 * @generated
 */
public class NetworkPackage extends EPackageImpl {

    /**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final String eNAME = "network";

    /**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final String eNS_URI = "http://ncsimulator.sf.net/models/network/";

    /**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final String eNS_PREFIX = "network";

    /**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final NetworkPackage eINSTANCE = net.sf.ncsimulator.models.network.NetworkPackage.init();

    /**
	 * The meta object id for the '{@link net.sf.ncsimulator.models.network.Node <em>Node</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see net.sf.ncsimulator.models.network.Node
	 * @see net.sf.ncsimulator.models.network.NetworkPackage#getNode()
	 * @generated
	 */
    public static final int NODE = 0;

    /**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int NODE__NAME = 0;

    /**
	 * The feature id for the '<em><b>Network</b></em>' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int NODE__NETWORK = 1;

    /**
	 * The feature id for the '<em><b>Output Channels</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int NODE__OUTPUT_CHANNELS = 2;

    /**
	 * The feature id for the '<em><b>Input Channels</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int NODE__INPUT_CHANNELS = 3;

    /**
	 * The feature id for the '<em><b>Behaviour Instance</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int NODE__BEHAVIOUR_INSTANCE = 4;

    /**
	 * The number of structural features of the '<em>Node</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int NODE_FEATURE_COUNT = 5;

    /**
	 * The meta object id for the '{@link net.sf.ncsimulator.models.network.Network <em>Network</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see net.sf.ncsimulator.models.network.Network
	 * @see net.sf.ncsimulator.models.network.NetworkPackage#getNetwork()
	 * @generated
	 */
    public static final int NETWORK = 1;

    /**
	 * The feature id for the '<em><b>Nodes</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int NETWORK__NODES = 0;

    /**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int NETWORK__NAME = 1;

    /**
	 * The feature id for the '<em><b>Channels</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int NETWORK__CHANNELS = 2;

    /**
	 * The number of structural features of the '<em>Network</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int NETWORK_FEATURE_COUNT = 3;

    /**
	 * The meta object id for the '{@link net.sf.ncsimulator.models.network.Channel <em>Channel</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see net.sf.ncsimulator.models.network.Channel
	 * @see net.sf.ncsimulator.models.network.NetworkPackage#getChannel()
	 * @generated
	 */
    public static final int CHANNEL = 2;

    /**
	 * The feature id for the '<em><b>Source</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int CHANNEL__SOURCE = 0;

    /**
	 * The feature id for the '<em><b>Target</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int CHANNEL__TARGET = 1;

    /**
	 * The feature id for the '<em><b>Network</b></em>' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int CHANNEL__NETWORK = 2;

    /**
	 * The feature id for the '<em><b>Behaviour</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int CHANNEL__BEHAVIOUR = 3;

    /**
	 * The feature id for the '<em><b>Title</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int CHANNEL__TITLE = 4;

    /**
	 * The feature id for the '<em><b>Behaviour Instance</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int CHANNEL__BEHAVIOUR_INSTANCE = 5;

    /**
	 * The number of structural features of the '<em>Channel</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int CHANNEL_FEATURE_COUNT = 6;

    /**
	 * The meta object id for the '{@link net.sf.ncsimulator.models.network.ChannelBehavior <em>Channel Behavior</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see net.sf.ncsimulator.models.network.ChannelBehavior
	 * @see net.sf.ncsimulator.models.network.NetworkPackage#getChannelBehavior()
	 * @generated
	 */
    public static final int CHANNEL_BEHAVIOR = 3;

    /**
	 * The number of structural features of the '<em>Channel Behavior</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int CHANNEL_BEHAVIOR_FEATURE_COUNT = 0;

    /**
	 * The meta object id for the '{@link net.sf.ncsimulator.models.network.NodeBehavior <em>Node Behavior</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see net.sf.ncsimulator.models.network.NodeBehavior
	 * @see net.sf.ncsimulator.models.network.NetworkPackage#getNodeBehavior()
	 * @generated
	 */
    public static final int NODE_BEHAVIOR = 4;

    /**
	 * The number of structural features of the '<em>Node Behavior</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int NODE_BEHAVIOR_FEATURE_COUNT = 0;

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    private EClass nodeEClass = null;

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    private EClass networkEClass = null;

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    private EClass channelEClass = null;

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    private EClass channelBehaviorEClass = null;

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    private EClass nodeBehaviorEClass = null;

    /**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see net.sf.ncsimulator.models.network.NetworkPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
    private NetworkPackage() {
        super(eNS_URI, NetworkFactory.INSTANCE);
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    private static boolean isInited = false;

    /**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
	 * 
	 * <p>This method is used to initialize {@link NetworkPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
    public static NetworkPackage init() {
        if (isInited) return (NetworkPackage) EPackage.Registry.INSTANCE.getEPackage(NetworkPackage.eNS_URI);
        NetworkPackage theNetworkPackage = (NetworkPackage) (EPackage.Registry.INSTANCE.get(eNS_URI) instanceof NetworkPackage ? EPackage.Registry.INSTANCE.get(eNS_URI) : new NetworkPackage());
        isInited = true;
        theNetworkPackage.createPackageContents();
        theNetworkPackage.initializePackageContents();
        theNetworkPackage.freeze();
        EPackage.Registry.INSTANCE.put(NetworkPackage.eNS_URI, theNetworkPackage);
        return theNetworkPackage;
    }

    /**
	 * Returns the meta object for class '{@link net.sf.ncsimulator.models.network.Node <em>Node</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Node</em>'.
	 * @see net.sf.ncsimulator.models.network.Node
	 * @generated
	 */
    public EClass getNode() {
        return nodeEClass;
    }

    /**
	 * Returns the meta object for the attribute '{@link net.sf.ncsimulator.models.network.Node#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see net.sf.ncsimulator.models.network.Node#getName()
	 * @see #getNode()
	 * @generated
	 */
    public EAttribute getNode_Name() {
        return (EAttribute) nodeEClass.getEStructuralFeatures().get(0);
    }

    /**
	 * Returns the meta object for the container reference '{@link net.sf.ncsimulator.models.network.Node#getNetwork <em>Network</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the container reference '<em>Network</em>'.
	 * @see net.sf.ncsimulator.models.network.Node#getNetwork()
	 * @see #getNode()
	 * @generated
	 */
    public EReference getNode_Network() {
        return (EReference) nodeEClass.getEStructuralFeatures().get(1);
    }

    /**
	 * Returns the meta object for the reference list '{@link net.sf.ncsimulator.models.network.Node#getOutputChannels <em>Output Channels</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Output Channels</em>'.
	 * @see net.sf.ncsimulator.models.network.Node#getOutputChannels()
	 * @see #getNode()
	 * @generated
	 */
    public EReference getNode_OutputChannels() {
        return (EReference) nodeEClass.getEStructuralFeatures().get(2);
    }

    /**
	 * Returns the meta object for the reference list '{@link net.sf.ncsimulator.models.network.Node#getInputChannels <em>Input Channels</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Input Channels</em>'.
	 * @see net.sf.ncsimulator.models.network.Node#getInputChannels()
	 * @see #getNode()
	 * @generated
	 */
    public EReference getNode_InputChannels() {
        return (EReference) nodeEClass.getEStructuralFeatures().get(3);
    }

    /**
	 * Returns the meta object for the containment reference '{@link net.sf.ncsimulator.models.network.Node#getBehaviourInstance <em>Behaviour Instance</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Behaviour Instance</em>'.
	 * @see net.sf.ncsimulator.models.network.Node#getBehaviourInstance()
	 * @see #getNode()
	 * @generated
	 */
    public EReference getNode_BehaviourInstance() {
        return (EReference) nodeEClass.getEStructuralFeatures().get(4);
    }

    /**
	 * Returns the meta object for class '{@link net.sf.ncsimulator.models.network.Network <em>Network</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Network</em>'.
	 * @see net.sf.ncsimulator.models.network.Network
	 * @generated
	 */
    public EClass getNetwork() {
        return networkEClass;
    }

    /**
	 * Returns the meta object for the containment reference list '{@link net.sf.ncsimulator.models.network.Network#getNodes <em>Nodes</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Nodes</em>'.
	 * @see net.sf.ncsimulator.models.network.Network#getNodes()
	 * @see #getNetwork()
	 * @generated
	 */
    public EReference getNetwork_Nodes() {
        return (EReference) networkEClass.getEStructuralFeatures().get(0);
    }

    /**
	 * Returns the meta object for the attribute '{@link net.sf.ncsimulator.models.network.Network#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see net.sf.ncsimulator.models.network.Network#getName()
	 * @see #getNetwork()
	 * @generated
	 */
    public EAttribute getNetwork_Name() {
        return (EAttribute) networkEClass.getEStructuralFeatures().get(1);
    }

    /**
	 * Returns the meta object for the containment reference list '{@link net.sf.ncsimulator.models.network.Network#getChannels <em>Channels</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Channels</em>'.
	 * @see net.sf.ncsimulator.models.network.Network#getChannels()
	 * @see #getNetwork()
	 * @generated
	 */
    public EReference getNetwork_Channels() {
        return (EReference) networkEClass.getEStructuralFeatures().get(2);
    }

    /**
	 * Returns the meta object for class '{@link net.sf.ncsimulator.models.network.Channel <em>Channel</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Channel</em>'.
	 * @see net.sf.ncsimulator.models.network.Channel
	 * @generated
	 */
    public EClass getChannel() {
        return channelEClass;
    }

    /**
	 * Returns the meta object for the reference '{@link net.sf.ncsimulator.models.network.Channel#getSource <em>Source</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Source</em>'.
	 * @see net.sf.ncsimulator.models.network.Channel#getSource()
	 * @see #getChannel()
	 * @generated
	 */
    public EReference getChannel_Source() {
        return (EReference) channelEClass.getEStructuralFeatures().get(0);
    }

    /**
	 * Returns the meta object for the reference '{@link net.sf.ncsimulator.models.network.Channel#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Target</em>'.
	 * @see net.sf.ncsimulator.models.network.Channel#getTarget()
	 * @see #getChannel()
	 * @generated
	 */
    public EReference getChannel_Target() {
        return (EReference) channelEClass.getEStructuralFeatures().get(1);
    }

    /**
	 * Returns the meta object for the container reference '{@link net.sf.ncsimulator.models.network.Channel#getNetwork <em>Network</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the container reference '<em>Network</em>'.
	 * @see net.sf.ncsimulator.models.network.Channel#getNetwork()
	 * @see #getChannel()
	 * @generated
	 */
    public EReference getChannel_Network() {
        return (EReference) channelEClass.getEStructuralFeatures().get(2);
    }

    /**
	 * Returns the meta object for the attribute '{@link net.sf.ncsimulator.models.network.Channel#getBehaviour <em>Behaviour</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Behaviour</em>'.
	 * @see net.sf.ncsimulator.models.network.Channel#getBehaviour()
	 * @see #getChannel()
	 * @generated
	 */
    public EAttribute getChannel_Behaviour() {
        return (EAttribute) channelEClass.getEStructuralFeatures().get(3);
    }

    /**
	 * Returns the meta object for the attribute '{@link net.sf.ncsimulator.models.network.Channel#getTitle <em>Title</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Title</em>'.
	 * @see net.sf.ncsimulator.models.network.Channel#getTitle()
	 * @see #getChannel()
	 * @generated
	 */
    public EAttribute getChannel_Title() {
        return (EAttribute) channelEClass.getEStructuralFeatures().get(4);
    }

    /**
	 * Returns the meta object for the containment reference '{@link net.sf.ncsimulator.models.network.Channel#getBehaviourInstance <em>Behaviour Instance</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Behaviour Instance</em>'.
	 * @see net.sf.ncsimulator.models.network.Channel#getBehaviourInstance()
	 * @see #getChannel()
	 * @generated
	 */
    public EReference getChannel_BehaviourInstance() {
        return (EReference) channelEClass.getEStructuralFeatures().get(5);
    }

    /**
	 * Returns the meta object for class '{@link net.sf.ncsimulator.models.network.ChannelBehavior <em>Channel Behavior</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Channel Behavior</em>'.
	 * @see net.sf.ncsimulator.models.network.ChannelBehavior
	 * @generated
	 */
    public EClass getChannelBehavior() {
        return channelBehaviorEClass;
    }

    /**
	 * Returns the meta object for class '{@link net.sf.ncsimulator.models.network.NodeBehavior <em>Node Behavior</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Node Behavior</em>'.
	 * @see net.sf.ncsimulator.models.network.NodeBehavior
	 * @generated
	 */
    public EClass getNodeBehavior() {
        return nodeBehaviorEClass;
    }

    /**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
    public NetworkFactory getNetworkFactory() {
        return (NetworkFactory) getEFactoryInstance();
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    private boolean isCreated = false;

    /**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public void createPackageContents() {
        if (isCreated) return;
        isCreated = true;
        nodeEClass = createEClass(NODE);
        createEAttribute(nodeEClass, NODE__NAME);
        createEReference(nodeEClass, NODE__NETWORK);
        createEReference(nodeEClass, NODE__OUTPUT_CHANNELS);
        createEReference(nodeEClass, NODE__INPUT_CHANNELS);
        createEReference(nodeEClass, NODE__BEHAVIOUR_INSTANCE);
        networkEClass = createEClass(NETWORK);
        createEReference(networkEClass, NETWORK__NODES);
        createEAttribute(networkEClass, NETWORK__NAME);
        createEReference(networkEClass, NETWORK__CHANNELS);
        channelEClass = createEClass(CHANNEL);
        createEReference(channelEClass, CHANNEL__SOURCE);
        createEReference(channelEClass, CHANNEL__TARGET);
        createEReference(channelEClass, CHANNEL__NETWORK);
        createEAttribute(channelEClass, CHANNEL__BEHAVIOUR);
        createEAttribute(channelEClass, CHANNEL__TITLE);
        createEReference(channelEClass, CHANNEL__BEHAVIOUR_INSTANCE);
        channelBehaviorEClass = createEClass(CHANNEL_BEHAVIOR);
        nodeBehaviorEClass = createEClass(NODE_BEHAVIOR);
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    private boolean isInitialized = false;

    /**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public void initializePackageContents() {
        if (isInitialized) return;
        isInitialized = true;
        setName(eNAME);
        setNsPrefix(eNS_PREFIX);
        setNsURI(eNS_URI);
        initEClass(nodeEClass, Node.class, "Node", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getNode_Name(), ecorePackage.getEString(), "Name", "", 1, 1, Node.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getNode_Network(), this.getNetwork(), this.getNetwork_Nodes(), "Network", null, 1, 1, Node.class, !IS_TRANSIENT, !IS_VOLATILE, !IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getNode_OutputChannels(), this.getChannel(), this.getChannel_Source(), "OutputChannels", null, 0, -1, Node.class, !IS_TRANSIENT, !IS_VOLATILE, !IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);
        initEReference(getNode_InputChannels(), this.getChannel(), this.getChannel_Target(), "InputChannels", null, 0, -1, Node.class, !IS_TRANSIENT, !IS_VOLATILE, !IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);
        initEReference(getNode_BehaviourInstance(), this.getNodeBehavior(), null, "BehaviourInstance", null, 0, 1, Node.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEClass(networkEClass, Network.class, "Network", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getNetwork_Nodes(), this.getNode(), this.getNode_Network(), "Nodes", null, 0, -1, Network.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        getNetwork_Nodes().getEKeys().add(this.getNode_Name());
        initEAttribute(getNetwork_Name(), ecorePackage.getEString(), "Name", null, 0, 1, Network.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getNetwork_Channels(), this.getChannel(), this.getChannel_Network(), "Channels", null, 0, -1, Network.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEClass(channelEClass, Channel.class, "Channel", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEReference(getChannel_Source(), this.getNode(), this.getNode_OutputChannels(), "Source", null, 1, 1, Channel.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getChannel_Target(), this.getNode(), this.getNode_InputChannels(), "Target", null, 1, 1, Channel.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getChannel_Network(), this.getNetwork(), this.getNetwork_Channels(), "Network", null, 1, 1, Channel.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getChannel_Behaviour(), ecorePackage.getEString(), "Behaviour", "net.sf.ncsimulator.channels.ideal.IdealChannelFactory", 1, 1, Channel.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEAttribute(getChannel_Title(), ecorePackage.getEString(), "Title", "", 0, 1, Channel.class, IS_TRANSIENT, IS_VOLATILE, !IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEReference(getChannel_BehaviourInstance(), this.getChannelBehavior(), null, "BehaviourInstance", null, 0, 1, Channel.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
        initEClass(channelBehaviorEClass, ChannelBehavior.class, "ChannelBehavior", IS_ABSTRACT, IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEClass(nodeBehaviorEClass, NodeBehavior.class, "NodeBehavior", IS_ABSTRACT, IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        createResource(eNS_URI);
    }

    /**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public interface Literals {

        /**
		 * The meta object literal for the '{@link net.sf.ncsimulator.models.network.Node <em>Node</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see net.sf.ncsimulator.models.network.Node
		 * @see net.sf.ncsimulator.models.network.NetworkPackage#getNode()
		 * @generated
		 */
        public static final EClass NODE = eINSTANCE.getNode();

        /**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EAttribute NODE__NAME = eINSTANCE.getNode_Name();

        /**
		 * The meta object literal for the '<em><b>Network</b></em>' container reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EReference NODE__NETWORK = eINSTANCE.getNode_Network();

        /**
		 * The meta object literal for the '<em><b>Output Channels</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EReference NODE__OUTPUT_CHANNELS = eINSTANCE.getNode_OutputChannels();

        /**
		 * The meta object literal for the '<em><b>Input Channels</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EReference NODE__INPUT_CHANNELS = eINSTANCE.getNode_InputChannels();

        /**
		 * The meta object literal for the '<em><b>Behaviour Instance</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EReference NODE__BEHAVIOUR_INSTANCE = eINSTANCE.getNode_BehaviourInstance();

        /**
		 * The meta object literal for the '{@link net.sf.ncsimulator.models.network.Network <em>Network</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see net.sf.ncsimulator.models.network.Network
		 * @see net.sf.ncsimulator.models.network.NetworkPackage#getNetwork()
		 * @generated
		 */
        public static final EClass NETWORK = eINSTANCE.getNetwork();

        /**
		 * The meta object literal for the '<em><b>Nodes</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EReference NETWORK__NODES = eINSTANCE.getNetwork_Nodes();

        /**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EAttribute NETWORK__NAME = eINSTANCE.getNetwork_Name();

        /**
		 * The meta object literal for the '<em><b>Channels</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EReference NETWORK__CHANNELS = eINSTANCE.getNetwork_Channels();

        /**
		 * The meta object literal for the '{@link net.sf.ncsimulator.models.network.Channel <em>Channel</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see net.sf.ncsimulator.models.network.Channel
		 * @see net.sf.ncsimulator.models.network.NetworkPackage#getChannel()
		 * @generated
		 */
        public static final EClass CHANNEL = eINSTANCE.getChannel();

        /**
		 * The meta object literal for the '<em><b>Source</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EReference CHANNEL__SOURCE = eINSTANCE.getChannel_Source();

        /**
		 * The meta object literal for the '<em><b>Target</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EReference CHANNEL__TARGET = eINSTANCE.getChannel_Target();

        /**
		 * The meta object literal for the '<em><b>Network</b></em>' container reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EReference CHANNEL__NETWORK = eINSTANCE.getChannel_Network();

        /**
		 * The meta object literal for the '<em><b>Behaviour</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EAttribute CHANNEL__BEHAVIOUR = eINSTANCE.getChannel_Behaviour();

        /**
		 * The meta object literal for the '<em><b>Title</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EAttribute CHANNEL__TITLE = eINSTANCE.getChannel_Title();

        /**
		 * The meta object literal for the '<em><b>Behaviour Instance</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EReference CHANNEL__BEHAVIOUR_INSTANCE = eINSTANCE.getChannel_BehaviourInstance();

        /**
		 * The meta object literal for the '{@link net.sf.ncsimulator.models.network.ChannelBehavior <em>Channel Behavior</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see net.sf.ncsimulator.models.network.ChannelBehavior
		 * @see net.sf.ncsimulator.models.network.NetworkPackage#getChannelBehavior()
		 * @generated
		 */
        public static final EClass CHANNEL_BEHAVIOR = eINSTANCE.getChannelBehavior();

        /**
		 * The meta object literal for the '{@link net.sf.ncsimulator.models.network.NodeBehavior <em>Node Behavior</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see net.sf.ncsimulator.models.network.NodeBehavior
		 * @see net.sf.ncsimulator.models.network.NetworkPackage#getNodeBehavior()
		 * @generated
		 */
        public static final EClass NODE_BEHAVIOR = eINSTANCE.getNodeBehavior();
    }
}
