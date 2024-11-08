package bmm.impl;

import bmm.BmmPackage;
import bmm.BusinessProcess;
import bmm.CourseOfAction;
import bmm.DesiredResult;
import bmm.Directive;
import java.util.Collection;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EObjectWithInverseResolvingEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Course Of Action</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link bmm.impl.CourseOfActionImpl#getComposedOf <em>Composed Of</em>}</li>
 *   <li>{@link bmm.impl.CourseOfActionImpl#getPartOf <em>Part Of</em>}</li>
 *   <li>{@link bmm.impl.CourseOfActionImpl#getEnabledBy <em>Enabled By</em>}</li>
 *   <li>{@link bmm.impl.CourseOfActionImpl#getEnables <em>Enables</em>}</li>
 *   <li>{@link bmm.impl.CourseOfActionImpl#getChannelsEffortToward <em>Channels Effort Toward</em>}</li>
 *   <li>{@link bmm.impl.CourseOfActionImpl#getRealizedBy <em>Realized By</em>}</li>
 *   <li>{@link bmm.impl.CourseOfActionImpl#getGovernedBy <em>Governed By</em>}</li>
 *   <li>{@link bmm.impl.CourseOfActionImpl#getFormulatedBasedOn <em>Formulated Based On</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public abstract class CourseOfActionImpl extends MeansImpl implements CourseOfAction {

    /**
	 * The cached value of the '{@link #getComposedOf() <em>Composed Of</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getComposedOf()
	 * @generated
	 * @ordered
	 */
    protected EList<CourseOfAction> composedOf;

    /**
	 * The cached value of the '{@link #getPartOf() <em>Part Of</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPartOf()
	 * @generated
	 * @ordered
	 */
    protected EList<CourseOfAction> partOf;

    /**
	 * The cached value of the '{@link #getEnabledBy() <em>Enabled By</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEnabledBy()
	 * @generated
	 * @ordered
	 */
    protected EList<CourseOfAction> enabledBy;

    /**
	 * The cached value of the '{@link #getEnables() <em>Enables</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEnables()
	 * @generated
	 * @ordered
	 */
    protected EList<CourseOfAction> enables;

    /**
	 * The cached value of the '{@link #getChannelsEffortToward() <em>Channels Effort Toward</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getChannelsEffortToward()
	 * @generated
	 * @ordered
	 */
    protected EList<DesiredResult> channelsEffortToward;

    /**
	 * The cached value of the '{@link #getRealizedBy() <em>Realized By</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRealizedBy()
	 * @generated
	 * @ordered
	 */
    protected EList<BusinessProcess> realizedBy;

    /**
	 * The cached value of the '{@link #getGovernedBy() <em>Governed By</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGovernedBy()
	 * @generated
	 * @ordered
	 */
    protected EList<Directive> governedBy;

    /**
	 * The cached value of the '{@link #getFormulatedBasedOn() <em>Formulated Based On</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFormulatedBasedOn()
	 * @generated
	 * @ordered
	 */
    protected EList<Directive> formulatedBasedOn;

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    protected CourseOfActionImpl() {
        super();
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    @Override
    protected EClass eStaticClass() {
        return BmmPackage.Literals.COURSE_OF_ACTION;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public EList<CourseOfAction> getComposedOf() {
        if (composedOf == null) {
            composedOf = new EObjectWithInverseResolvingEList.ManyInverse<CourseOfAction>(CourseOfAction.class, this, BmmPackage.COURSE_OF_ACTION__COMPOSED_OF, BmmPackage.COURSE_OF_ACTION__PART_OF);
        }
        return composedOf;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public EList<CourseOfAction> getPartOf() {
        if (partOf == null) {
            partOf = new EObjectWithInverseResolvingEList.ManyInverse<CourseOfAction>(CourseOfAction.class, this, BmmPackage.COURSE_OF_ACTION__PART_OF, BmmPackage.COURSE_OF_ACTION__COMPOSED_OF);
        }
        return partOf;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public EList<CourseOfAction> getEnabledBy() {
        if (enabledBy == null) {
            enabledBy = new EObjectWithInverseResolvingEList.ManyInverse<CourseOfAction>(CourseOfAction.class, this, BmmPackage.COURSE_OF_ACTION__ENABLED_BY, BmmPackage.COURSE_OF_ACTION__ENABLES);
        }
        return enabledBy;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public EList<CourseOfAction> getEnables() {
        if (enables == null) {
            enables = new EObjectWithInverseResolvingEList.ManyInverse<CourseOfAction>(CourseOfAction.class, this, BmmPackage.COURSE_OF_ACTION__ENABLES, BmmPackage.COURSE_OF_ACTION__ENABLED_BY);
        }
        return enables;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public EList<DesiredResult> getChannelsEffortToward() {
        if (channelsEffortToward == null) {
            channelsEffortToward = new EObjectWithInverseResolvingEList.ManyInverse<DesiredResult>(DesiredResult.class, this, BmmPackage.COURSE_OF_ACTION__CHANNELS_EFFORT_TOWARD, BmmPackage.DESIRED_RESULT__SUPPORTED_BY);
        }
        return channelsEffortToward;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public EList<BusinessProcess> getRealizedBy() {
        if (realizedBy == null) {
            realizedBy = new EObjectWithInverseResolvingEList.ManyInverse<BusinessProcess>(BusinessProcess.class, this, BmmPackage.COURSE_OF_ACTION__REALIZED_BY, BmmPackage.BUSINESS_PROCESS__REALIZES);
        }
        return realizedBy;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public EList<Directive> getGovernedBy() {
        if (governedBy == null) {
            governedBy = new EObjectWithInverseResolvingEList.ManyInverse<Directive>(Directive.class, this, BmmPackage.COURSE_OF_ACTION__GOVERNED_BY, BmmPackage.DIRECTIVE__GOVERNS);
        }
        return governedBy;
    }

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
    public EList<Directive> getFormulatedBasedOn() {
        if (formulatedBasedOn == null) {
            formulatedBasedOn = new EObjectWithInverseResolvingEList.ManyInverse<Directive>(Directive.class, this, BmmPackage.COURSE_OF_ACTION__FORMULATED_BASED_ON, BmmPackage.DIRECTIVE__SOURCE_OF);
        }
        return formulatedBasedOn;
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
            case BmmPackage.COURSE_OF_ACTION__COMPOSED_OF:
                return ((InternalEList<InternalEObject>) (InternalEList<?>) getComposedOf()).basicAdd(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__PART_OF:
                return ((InternalEList<InternalEObject>) (InternalEList<?>) getPartOf()).basicAdd(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__ENABLED_BY:
                return ((InternalEList<InternalEObject>) (InternalEList<?>) getEnabledBy()).basicAdd(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__ENABLES:
                return ((InternalEList<InternalEObject>) (InternalEList<?>) getEnables()).basicAdd(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__CHANNELS_EFFORT_TOWARD:
                return ((InternalEList<InternalEObject>) (InternalEList<?>) getChannelsEffortToward()).basicAdd(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__REALIZED_BY:
                return ((InternalEList<InternalEObject>) (InternalEList<?>) getRealizedBy()).basicAdd(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__GOVERNED_BY:
                return ((InternalEList<InternalEObject>) (InternalEList<?>) getGovernedBy()).basicAdd(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__FORMULATED_BASED_ON:
                return ((InternalEList<InternalEObject>) (InternalEList<?>) getFormulatedBasedOn()).basicAdd(otherEnd, msgs);
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
            case BmmPackage.COURSE_OF_ACTION__COMPOSED_OF:
                return ((InternalEList<?>) getComposedOf()).basicRemove(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__PART_OF:
                return ((InternalEList<?>) getPartOf()).basicRemove(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__ENABLED_BY:
                return ((InternalEList<?>) getEnabledBy()).basicRemove(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__ENABLES:
                return ((InternalEList<?>) getEnables()).basicRemove(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__CHANNELS_EFFORT_TOWARD:
                return ((InternalEList<?>) getChannelsEffortToward()).basicRemove(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__REALIZED_BY:
                return ((InternalEList<?>) getRealizedBy()).basicRemove(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__GOVERNED_BY:
                return ((InternalEList<?>) getGovernedBy()).basicRemove(otherEnd, msgs);
            case BmmPackage.COURSE_OF_ACTION__FORMULATED_BASED_ON:
                return ((InternalEList<?>) getFormulatedBasedOn()).basicRemove(otherEnd, msgs);
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
            case BmmPackage.COURSE_OF_ACTION__COMPOSED_OF:
                return getComposedOf();
            case BmmPackage.COURSE_OF_ACTION__PART_OF:
                return getPartOf();
            case BmmPackage.COURSE_OF_ACTION__ENABLED_BY:
                return getEnabledBy();
            case BmmPackage.COURSE_OF_ACTION__ENABLES:
                return getEnables();
            case BmmPackage.COURSE_OF_ACTION__CHANNELS_EFFORT_TOWARD:
                return getChannelsEffortToward();
            case BmmPackage.COURSE_OF_ACTION__REALIZED_BY:
                return getRealizedBy();
            case BmmPackage.COURSE_OF_ACTION__GOVERNED_BY:
                return getGovernedBy();
            case BmmPackage.COURSE_OF_ACTION__FORMULATED_BASED_ON:
                return getFormulatedBasedOn();
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
            case BmmPackage.COURSE_OF_ACTION__COMPOSED_OF:
                getComposedOf().clear();
                getComposedOf().addAll((Collection<? extends CourseOfAction>) newValue);
                return;
            case BmmPackage.COURSE_OF_ACTION__PART_OF:
                getPartOf().clear();
                getPartOf().addAll((Collection<? extends CourseOfAction>) newValue);
                return;
            case BmmPackage.COURSE_OF_ACTION__ENABLED_BY:
                getEnabledBy().clear();
                getEnabledBy().addAll((Collection<? extends CourseOfAction>) newValue);
                return;
            case BmmPackage.COURSE_OF_ACTION__ENABLES:
                getEnables().clear();
                getEnables().addAll((Collection<? extends CourseOfAction>) newValue);
                return;
            case BmmPackage.COURSE_OF_ACTION__CHANNELS_EFFORT_TOWARD:
                getChannelsEffortToward().clear();
                getChannelsEffortToward().addAll((Collection<? extends DesiredResult>) newValue);
                return;
            case BmmPackage.COURSE_OF_ACTION__REALIZED_BY:
                getRealizedBy().clear();
                getRealizedBy().addAll((Collection<? extends BusinessProcess>) newValue);
                return;
            case BmmPackage.COURSE_OF_ACTION__GOVERNED_BY:
                getGovernedBy().clear();
                getGovernedBy().addAll((Collection<? extends Directive>) newValue);
                return;
            case BmmPackage.COURSE_OF_ACTION__FORMULATED_BASED_ON:
                getFormulatedBasedOn().clear();
                getFormulatedBasedOn().addAll((Collection<? extends Directive>) newValue);
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
            case BmmPackage.COURSE_OF_ACTION__COMPOSED_OF:
                getComposedOf().clear();
                return;
            case BmmPackage.COURSE_OF_ACTION__PART_OF:
                getPartOf().clear();
                return;
            case BmmPackage.COURSE_OF_ACTION__ENABLED_BY:
                getEnabledBy().clear();
                return;
            case BmmPackage.COURSE_OF_ACTION__ENABLES:
                getEnables().clear();
                return;
            case BmmPackage.COURSE_OF_ACTION__CHANNELS_EFFORT_TOWARD:
                getChannelsEffortToward().clear();
                return;
            case BmmPackage.COURSE_OF_ACTION__REALIZED_BY:
                getRealizedBy().clear();
                return;
            case BmmPackage.COURSE_OF_ACTION__GOVERNED_BY:
                getGovernedBy().clear();
                return;
            case BmmPackage.COURSE_OF_ACTION__FORMULATED_BASED_ON:
                getFormulatedBasedOn().clear();
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
            case BmmPackage.COURSE_OF_ACTION__COMPOSED_OF:
                return composedOf != null && !composedOf.isEmpty();
            case BmmPackage.COURSE_OF_ACTION__PART_OF:
                return partOf != null && !partOf.isEmpty();
            case BmmPackage.COURSE_OF_ACTION__ENABLED_BY:
                return enabledBy != null && !enabledBy.isEmpty();
            case BmmPackage.COURSE_OF_ACTION__ENABLES:
                return enables != null && !enables.isEmpty();
            case BmmPackage.COURSE_OF_ACTION__CHANNELS_EFFORT_TOWARD:
                return channelsEffortToward != null && !channelsEffortToward.isEmpty();
            case BmmPackage.COURSE_OF_ACTION__REALIZED_BY:
                return realizedBy != null && !realizedBy.isEmpty();
            case BmmPackage.COURSE_OF_ACTION__GOVERNED_BY:
                return governedBy != null && !governedBy.isEmpty();
            case BmmPackage.COURSE_OF_ACTION__FORMULATED_BASED_ON:
                return formulatedBasedOn != null && !formulatedBasedOn.isEmpty();
        }
        return super.eIsSet(featureID);
    }
}
