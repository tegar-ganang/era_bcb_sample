package net.sf.ncsimulator.channels.ideal;

import net.sf.ncsimulator.models.network.NetworkPackage;
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
 * @see net.sf.ncsimulator.channels.ideal.idealFactory
 * @model kind="package"
 * @generated
 */
public class idealPackage extends EPackageImpl {

    /**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final String eNAME = "ideal";

    /**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final String eNS_URI = "http://ncsimulator.sf.net/channels/ideal";

    /**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final String eNS_PREFIX = "ideal";

    /**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public static final idealPackage eINSTANCE = net.sf.ncsimulator.channels.ideal.idealPackage.init();

    /**
	 * The meta object id for the '{@link net.sf.ncsimulator.channels.ideal.IdealChannel <em>Ideal Channel</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see net.sf.ncsimulator.channels.ideal.IdealChannel
	 * @see net.sf.ncsimulator.channels.ideal.idealPackage#getIdealChannel()
	 * @generated
	 */
    public static final int IDEAL_CHANNEL = 0;

    /**
	 * The number of structural features of the '<em>Ideal Channel</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
    public static final int IDEAL_CHANNEL_FEATURE_COUNT = NetworkPackage.CHANNEL_BEHAVIOR_FEATURE_COUNT + 0;

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    private EClass idealChannelEClass = null;

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
	 * @see net.sf.ncsimulator.channels.ideal.idealPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
    private idealPackage() {
        super(eNS_URI, idealFactory.INSTANCE);
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
	 * <p>This method is used to initialize {@link idealPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
    public static idealPackage init() {
        if (isInited) return (idealPackage) EPackage.Registry.INSTANCE.getEPackage(idealPackage.eNS_URI);
        idealPackage theidealPackage = (idealPackage) (EPackage.Registry.INSTANCE.get(eNS_URI) instanceof idealPackage ? EPackage.Registry.INSTANCE.get(eNS_URI) : new idealPackage());
        isInited = true;
        NetworkPackage.eINSTANCE.eClass();
        theidealPackage.createPackageContents();
        theidealPackage.initializePackageContents();
        theidealPackage.freeze();
        EPackage.Registry.INSTANCE.put(idealPackage.eNS_URI, theidealPackage);
        return theidealPackage;
    }

    /**
	 * Returns the meta object for class '{@link net.sf.ncsimulator.channels.ideal.IdealChannel <em>Ideal Channel</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Ideal Channel</em>'.
	 * @see net.sf.ncsimulator.channels.ideal.IdealChannel
	 * @generated
	 */
    public EClass getIdealChannel() {
        return idealChannelEClass;
    }

    /**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
    public idealFactory getidealFactory() {
        return (idealFactory) getEFactoryInstance();
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
        idealChannelEClass = createEClass(IDEAL_CHANNEL);
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
        idealChannelEClass.getESuperTypes().add(theNetworkPackage.getChannelBehavior());
        initEClass(idealChannelEClass, IdealChannel.class, "IdealChannel", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
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
		 * The meta object literal for the '{@link net.sf.ncsimulator.channels.ideal.IdealChannel <em>Ideal Channel</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see net.sf.ncsimulator.channels.ideal.IdealChannel
		 * @see net.sf.ncsimulator.channels.ideal.idealPackage#getIdealChannel()
		 * @generated
		 */
        public static final EClass IDEAL_CHANNEL = eINSTANCE.getIdealChannel();
    }
}
