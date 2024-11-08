package net.sf.xqz.model.engine.impl;

import net.sf.xqz.model.engine.CmdEngine;
import net.sf.xqz.model.engine.EnginePackage;
import net.sf.xqz.model.engine.Port;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.internal.cdo.CDOObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Port</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link net.sf.xqz.model.engine.impl.PortImpl#getChannel <em>Channel</em>}</li>
 *   <li>{@link net.sf.xqz.model.engine.impl.PortImpl#getEngine <em>Engine</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class PortImpl extends CDOObjectImpl implements Port {

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    protected PortImpl() {
        super();
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return EnginePackage.Literals.PORT;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected int eStaticFeatureCount() {
        return 0;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public Object getChannel() {
        return (Object) eGet(EnginePackage.Literals.PORT__CHANNEL, true);
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public void setChannel(Object newChannel) {
        eSet(EnginePackage.Literals.PORT__CHANNEL, newChannel);
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public CmdEngine getEngine() {
        return (CmdEngine) eGet(EnginePackage.Literals.PORT__ENGINE, true);
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public void setEngine(CmdEngine newEngine) {
        eSet(EnginePackage.Literals.PORT__ENGINE, newEngine);
    }
}
