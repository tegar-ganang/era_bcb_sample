package net.sf.ncsimulator.channels.bpsk;

import net.sf.ncsimulator.models.network.NetworkPackage;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
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
 * @see net.sf.ncsimulator.channels.bpsk.bpskFactory
 * @model kind="package"
 * @generated
 */
public class bpskPackage extends EPackageImpl {

    /**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final String eNAME = "bpsk";

    /**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final String eNS_URI = "http://ncsimulator.sf.net/channels/bpsk";

    /**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final String eNS_PREFIX = "bpsk";

    /**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final bpskPackage eINSTANCE = net.sf.ncsimulator.channels.bpsk.bpskPackage.init();

    /**
	 * The meta object id for the '{@link net.sf.ncsimulator.channels.bpsk.BPSKoverAWGNChannelBehaviour <em>BPS Kover AWGN Channel Behaviour</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see net.sf.ncsimulator.channels.bpsk.BPSKoverAWGNChannelBehaviour
	 * @see net.sf.ncsimulator.channels.bpsk.bpskPackage#getBPSKoverAWGNChannelBehaviour()
	 * @generated
	 */
    public static final int BPS_KOVER_AWGN_CHANNEL_BEHAVIOUR = 0;

    /**
	 * The feature id for the '<em><b>Sigma</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int BPS_KOVER_AWGN_CHANNEL_BEHAVIOUR__SIGMA = NetworkPackage.CHANNEL_BEHAVIOR_FEATURE_COUNT + 0;

    /**
	 * The number of structural features of the '<em>BPS Kover AWGN Channel Behaviour</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int BPS_KOVER_AWGN_CHANNEL_BEHAVIOUR_FEATURE_COUNT = NetworkPackage.CHANNEL_BEHAVIOR_FEATURE_COUNT + 1;

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    private EClass bpsKoverAWGNChannelBehaviourEClass = null;

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
	 * @see net.sf.ncsimulator.channels.bpsk.bpskPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
    private bpskPackage() {
        super(eNS_URI, bpskFactory.INSTANCE);
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
	 * <p>This method is used to initialize {@link bpskPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
    public static bpskPackage init() {
        if (isInited) return (bpskPackage) EPackage.Registry.INSTANCE.getEPackage(bpskPackage.eNS_URI);
        bpskPackage thebpskPackage = (bpskPackage) (EPackage.Registry.INSTANCE.get(eNS_URI) instanceof bpskPackage ? EPackage.Registry.INSTANCE.get(eNS_URI) : new bpskPackage());
        isInited = true;
        NetworkPackage.eINSTANCE.eClass();
        thebpskPackage.createPackageContents();
        thebpskPackage.initializePackageContents();
        thebpskPackage.freeze();
        EPackage.Registry.INSTANCE.put(bpskPackage.eNS_URI, thebpskPackage);
        return thebpskPackage;
    }

    /**
	 * Returns the meta object for class '{@link net.sf.ncsimulator.channels.bpsk.BPSKoverAWGNChannelBehaviour <em>BPS Kover AWGN Channel Behaviour</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>BPS Kover AWGN Channel Behaviour</em>'.
	 * @see net.sf.ncsimulator.channels.bpsk.BPSKoverAWGNChannelBehaviour
	 * @generated
	 */
    public EClass getBPSKoverAWGNChannelBehaviour() {
        return bpsKoverAWGNChannelBehaviourEClass;
    }

    /**
	 * Returns the meta object for the attribute '{@link net.sf.ncsimulator.channels.bpsk.BPSKoverAWGNChannelBehaviour#getSigma <em>Sigma</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Sigma</em>'.
	 * @see net.sf.ncsimulator.channels.bpsk.BPSKoverAWGNChannelBehaviour#getSigma()
	 * @see #getBPSKoverAWGNChannelBehaviour()
	 * @generated
	 */
    public EAttribute getBPSKoverAWGNChannelBehaviour_Sigma() {
        return (EAttribute) bpsKoverAWGNChannelBehaviourEClass.getEStructuralFeatures().get(0);
    }

    /**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
    public bpskFactory getbpskFactory() {
        return (bpskFactory) getEFactoryInstance();
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
        bpsKoverAWGNChannelBehaviourEClass = createEClass(BPS_KOVER_AWGN_CHANNEL_BEHAVIOUR);
        createEAttribute(bpsKoverAWGNChannelBehaviourEClass, BPS_KOVER_AWGN_CHANNEL_BEHAVIOUR__SIGMA);
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
        NetworkPackage theNetworkPackage = (NetworkPackage) EPackage.Registry.INSTANCE.getEPackage(NetworkPackage.eNS_URI);
        bpsKoverAWGNChannelBehaviourEClass.getESuperTypes().add(theNetworkPackage.getChannelBehavior());
        initEClass(bpsKoverAWGNChannelBehaviourEClass, BPSKoverAWGNChannelBehaviour.class, "BPSKoverAWGNChannelBehaviour", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
        initEAttribute(getBPSKoverAWGNChannelBehaviour_Sigma(), ecorePackage.getEDouble(), "sigma", "1", 0, 1, BPSKoverAWGNChannelBehaviour.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
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
		 * The meta object literal for the '{@link net.sf.ncsimulator.channels.bpsk.BPSKoverAWGNChannelBehaviour <em>BPS Kover AWGN Channel Behaviour</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see net.sf.ncsimulator.channels.bpsk.BPSKoverAWGNChannelBehaviour
		 * @see net.sf.ncsimulator.channels.bpsk.bpskPackage#getBPSKoverAWGNChannelBehaviour()
		 * @generated
		 */
        public static final EClass BPS_KOVER_AWGN_CHANNEL_BEHAVIOUR = eINSTANCE.getBPSKoverAWGNChannelBehaviour();

        /**
		 * The meta object literal for the '<em><b>Sigma</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
        public static final EAttribute BPS_KOVER_AWGN_CHANNEL_BEHAVIOUR__SIGMA = eINSTANCE.getBPSKoverAWGNChannelBehaviour_Sigma();
    }
}
