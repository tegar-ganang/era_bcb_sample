package net.taylor.uml2.uml.edit.providers;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import net.taylor.uml2.uml.edit.UMLEditPlugin;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.ResourceLocator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.provider.EObjectItemProvider;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.FeatureMapUtil;
import org.eclipse.emf.edit.command.CommandParameter;
import org.eclipse.emf.edit.command.CreateChildCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.provider.ComposeableAdapterFactory;
import org.eclipse.emf.edit.provider.ComposedImage;
import org.eclipse.emf.edit.provider.IEditingDomainItemProvider;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.provider.IItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.IItemPropertySource;
import org.eclipse.emf.edit.provider.IStructuredItemContentProvider;
import org.eclipse.emf.edit.provider.ITreeItemContentProvider;
import org.eclipse.emf.edit.provider.ItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.ItemPropertyDescriptorDecorator;
import org.eclipse.emf.edit.provider.ViewerNotification;
import org.eclipse.uml2.common.util.UML2Util;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Image;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.util.UMLUtil;

/**
 * This is the item provider adapter for a {@link org.eclipse.uml2.uml.Element}
 * object. <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */
public class ElementItemProvider extends EObjectItemProvider implements IEditingDomainItemProvider, IStructuredItemContentProvider, ITreeItemContentProvider, IItemLabelProvider, IItemPropertySource {

    /**
	 * This constructs an instance from a factory and a notifier. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public ElementItemProvider(AdapterFactory adapterFactory) {
        super(adapterFactory);
    }

    /**
	 * This returns the property descriptors for the adapted class. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public List getPropertyDescriptors(Object object) {
        if (itemPropertyDescriptors == null) {
            super.getPropertyDescriptors(object);
            addOwnedElementPropertyDescriptor(object);
            addOwnerPropertyDescriptor(object);
            addOwnedCommentPropertyDescriptor(object);
        }
        return itemPropertyDescriptors;
    }

    /**
	 * This adds a property descriptor for the Owned Element feature. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    protected void addOwnedElementPropertyDescriptor(Object object) {
        itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory) adapterFactory).getRootAdapterFactory(), getResourceLocator(), getString("_UI_Element_ownedElement_feature"), getString("_UI_PropertyDescriptor_description", "_UI_Element_ownedElement_feature", "_UI_Element_type"), UMLPackage.Literals.ELEMENT__OWNED_ELEMENT, false, null, null, new String[] { "org.eclipse.ui.views.properties.expert" }));
    }

    /**
	 * This adds a property descriptor for the Owner feature. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    protected void addOwnerPropertyDescriptor(Object object) {
        itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory) adapterFactory).getRootAdapterFactory(), getResourceLocator(), getString("_UI_Element_owner_feature"), getString("_UI_PropertyDescriptor_description", "_UI_Element_owner_feature", "_UI_Element_type"), UMLPackage.Literals.ELEMENT__OWNER, false, null, null, new String[] { "org.eclipse.ui.views.properties.expert" }));
    }

    /**
	 * This adds a property descriptor for the Owned Comment feature. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    protected void addOwnedCommentPropertyDescriptor(Object object) {
        itemPropertyDescriptors.add(createItemPropertyDescriptor(((ComposeableAdapterFactory) adapterFactory).getRootAdapterFactory(), getResourceLocator(), getString("_UI_Element_ownedComment_feature"), getString("_UI_PropertyDescriptor_description", "_UI_Element_ownedComment_feature", "_UI_Element_type"), UMLPackage.Literals.ELEMENT__OWNED_COMMENT, true, null, null, new String[] { "org.eclipse.ui.views.properties.expert" }));
    }

    /**
	 * This specifies how to implement {@link #getChildren} and is used to
	 * deduce an appropriate feature for an
	 * {@link org.eclipse.emf.edit.command.AddCommand},
	 * {@link org.eclipse.emf.edit.command.RemoveCommand} or
	 * {@link org.eclipse.emf.edit.command.MoveCommand} in
	 * {@link #createCommand}. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public Collection getChildrenFeatures(Object object) {
        if (childrenFeatures == null) {
            super.getChildrenFeatures(object);
            childrenFeatures.add(EcorePackage.Literals.EMODEL_ELEMENT__EANNOTATIONS);
            childrenFeatures.add(UMLPackage.Literals.ELEMENT__OWNED_COMMENT);
        }
        return childrenFeatures;
    }

    /**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    protected EStructuralFeature getChildFeature(Object object, Object child) {
        return super.getChildFeature(object, child);
    }

    /**
	 * This returns the label text for the adapted class. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public String getText(Object object) {
        return getString("_UI_Element_type");
    }

    /**
	 * This handles model notifications by calling {@link #updateChildren} to
	 * update any cached children and by creating a viewer notification, which
	 * it passes to {@link #fireNotifyChanged}. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
    public void notifyChanged(Notification notification) {
        updateChildren(notification);
        switch(notification.getFeatureID(Element.class)) {
            case UMLPackage.ELEMENT__EANNOTATIONS:
            case UMLPackage.ELEMENT__OWNED_COMMENT:
                fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), true, false));
                return;
        }
        super.notifyChanged(notification);
    }

    /**
	 * This adds to the collection of
	 * {@link org.eclipse.emf.edit.command.CommandParameter}s describing all of
	 * the children that can be created under this object. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    protected void collectNewChildDescriptors(Collection newChildDescriptors, Object object) {
        super.collectNewChildDescriptors(newChildDescriptors, object);
        newChildDescriptors.add(createChildParameter(EcorePackage.Literals.EMODEL_ELEMENT__EANNOTATIONS, EcoreFactory.eINSTANCE.createEAnnotation()));
        newChildDescriptors.add(createChildParameter(UMLPackage.Literals.ELEMENT__OWNED_COMMENT, UMLFactory.eINSTANCE.createComment()));
    }

    /**
	 * This returns the icon image for
	 * {@link org.eclipse.emf.edit.command.CreateChildCommand}. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated NOT
	 */
    public Object getCreateChildImage(Object owner, Object feature, Object child, Collection selection) {
        if (feature instanceof EStructuralFeature && FeatureMapUtil.isFeatureMap((EStructuralFeature) feature)) {
            FeatureMap.Entry entry = (FeatureMap.Entry) child;
            feature = entry.getEStructuralFeature();
            child = entry.getValue();
        }
        if (feature instanceof EReference && child instanceof EObject) {
            String name = "full/obj16/" + ((EObject) child).eClass().getName();
            try {
                List images = new ArrayList();
                ResourceLocator resourceLocator = getResourceLocator();
                images.add(resourceLocator.getImage(name));
                images.add(resourceLocator.getImage("full/ovr16/CreateChild"));
                return new ComposedImage(images) {

                    public List getDrawPoints(Size size) {
                        List result = super.getDrawPoints(size);
                        ((Point) result.get(1)).x = size.width - 7;
                        return result;
                    }
                };
            } catch (Exception e) {
                UMLEditPlugin.INSTANCE.log(e);
            }
        }
        return super.getCreateChildImage(owner, feature, child, selection);
    }

    /**
	 * Return the resource locator for this item provider's resources. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    public ResourceLocator getResourceLocator() {
        return UMLEditPlugin.INSTANCE;
    }

    public Object getParent(Object object) {
        EObject eContainer = ((EObject) object).eContainer();
        Element baseElement = eContainer == null ? null : UMLUtil.getBaseElement(eContainer);
        return baseElement == null ? super.getParent(object) : baseElement;
    }

    public Collection getChildren(Object object) {
        List children = new ArrayList(super.getChildren(object));
        for (Iterator stereotypeApplications = ((Element) object).getStereotypeApplications().iterator(); stereotypeApplications.hasNext(); ) {
            Object stereotypeApplication = stereotypeApplications.next();
            ITreeItemContentProvider treeItemContentProvider = (ITreeItemContentProvider) adapterFactory.adapt(stereotypeApplication, ITreeItemContentProvider.class);
            if (treeItemContentProvider != null) {
                children.addAll(treeItemContentProvider.getChildren(stereotypeApplication));
            }
        }
        return children;
    }

    public Collection getNewChildDescriptors(Object object, EditingDomain editingDomain, Object sibling) {
        List newChildDescriptors = new ArrayList(super.getNewChildDescriptors(object, editingDomain, sibling));
        for (Iterator stereotypeApplications = ((Element) object).getStereotypeApplications().iterator(); stereotypeApplications.hasNext(); ) {
            Object stereotypeApplication = stereotypeApplications.next();
            IEditingDomainItemProvider editingDomainItemProvider = (IEditingDomainItemProvider) adapterFactory.adapt(stereotypeApplication, IEditingDomainItemProvider.class);
            if (editingDomainItemProvider != null) {
                for (Iterator ncd = editingDomainItemProvider.getNewChildDescriptors(stereotypeApplication, editingDomain, null).iterator(); ncd.hasNext(); ) {
                    CommandParameter newChildDescriptor = (CommandParameter) ncd.next();
                    newChildDescriptor.setOwner(stereotypeApplication);
                    newChildDescriptors.add(newChildDescriptor);
                }
            }
        }
        return newChildDescriptors;
    }

    public List getStereotypeApplicationPropertyDescriptors(Object object) {
        List stereotypeApplications = ((Element) object).getStereotypeApplications();
        if (stereotypeApplications.isEmpty()) {
            return null;
        } else {
            List stereotypeApplicationPropertyDescriptors = new ArrayList();
            for (Iterator sa = stereotypeApplications.iterator(); sa.hasNext(); ) {
                final Object stereotypeApplication = sa.next();
                IItemPropertySource itemPropertySource = (IItemPropertySource) adapterFactory.adapt(stereotypeApplication, IItemPropertySource.class);
                if (itemPropertySource != null) {
                    for (Iterator propertyDescriptors = itemPropertySource.getPropertyDescriptors(stereotypeApplication).iterator(); propertyDescriptors.hasNext(); ) {
                        stereotypeApplicationPropertyDescriptors.add(new ItemPropertyDescriptorDecorator(stereotypeApplication, (IItemPropertyDescriptor) propertyDescriptors.next()));
                    }
                }
            }
            return stereotypeApplicationPropertyDescriptors;
        }
    }

    public IItemPropertyDescriptor getStereotypeApplicationPropertyDescriptor(Object object, Object propertyId) {
        for (Iterator i = getStereotypeApplicationPropertyDescriptors(object).iterator(); i.hasNext(); ) {
            IItemPropertyDescriptor itemPropertyDescriptor = (IItemPropertyDescriptor) i.next();
            if (propertyId.equals(itemPropertyDescriptor.getId(object))) {
                return itemPropertyDescriptor;
            }
        }
        return null;
    }

    public Command createCommand(Object object, EditingDomain domain, Class commandClass, CommandParameter commandParameter) {
        if (commandClass == CreateChildCommand.class) {
            EObject eOwner = ((CommandParameter) unwrapCommandValues(commandParameter, commandClass).getValue()).getEOwner();
            if (eOwner != null && eOwner != object) {
                IEditingDomainItemProvider editingDomainItemProvider = (IEditingDomainItemProvider) adapterFactory.adapt(eOwner, IEditingDomainItemProvider.class);
                if (editingDomainItemProvider != null) {
                    commandParameter.setOwner(eOwner);
                    return editingDomainItemProvider.createCommand(eOwner, domain, commandClass, commandParameter);
                }
            }
        }
        return super.createCommand(object, domain, commandClass, commandParameter);
    }

    protected boolean shouldTranslate() {
        return UMLEditPlugin.INSTANCE.shouldTranslate();
    }

    public String getQualifiedText(Object object) {
        return UML2Util.getQualifiedText((EObject) object, new UMLUtil.QualifiedTextProvider() {

            public String getFeatureText(EStructuralFeature eStructuralFeature) {
                return ElementItemProvider.this.getFeatureText(eStructuralFeature);
            }

            public String getClassText(EObject eObject) {
                return getTypeText(eObject);
            }
        });
    }

    protected StringBuffer appendKeywords(StringBuffer text, Object object) {
        if (object instanceof Element) {
            Element element = (Element) object;
            Iterator appliedStereotypes = element.getAppliedStereotypes().iterator();
            Iterator keywords = element.getKeywords().iterator();
            if (appliedStereotypes.hasNext() || keywords.hasNext()) {
                if (text.length() > 0) {
                    text.append(' ');
                }
                text.append("<<");
                while (appliedStereotypes.hasNext()) {
                    text.append((getStereotypeName((Stereotype) appliedStereotypes.next())));
                    if (appliedStereotypes.hasNext() || keywords.hasNext()) {
                        text.append(", ");
                    }
                }
                while (keywords.hasNext()) {
                    text.append((String) keywords.next());
                    if (keywords.hasNext()) {
                        text.append(", ");
                    }
                }
                text.append(">>");
            }
        }
        return text;
    }

    protected String getStereotypeName(Stereotype stereotype) {
        String name = stereotype.getQualifiedName();
        int i = name.lastIndexOf(".");
        return name.substring(i + 1);
    }

    protected StringBuffer appendType(StringBuffer text, Object object) {
        text.insert(0, ' ');
        text.insert(0, '>');
        text.insert(0, getTypeText(object));
        text.insert(0, '<');
        return text;
    }

    protected StringBuffer appendType(StringBuffer text, String key) {
        text.insert(0, ' ');
        text.insert(0, '>');
        text.insert(0, getString(key));
        text.insert(0, '<');
        return text;
    }

    protected StringBuffer appendLabel(StringBuffer text, Object object) {
        if (object instanceof NamedElement) {
            int i = text.indexOf(">") + 1;
            String label = ((NamedElement) object).getLabel(shouldTranslate());
            if (!UML2Util.isEmpty(label)) {
                text.insert(i, label);
                text.insert(i, ' ');
            }
        }
        return text;
    }

    protected StringBuffer appendString(StringBuffer text, String string) {
        if (!UML2Util.isEmpty(string)) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(string);
        }
        return text;
    }

    protected String getTypeText(ResourceLocator resourceLocator, EClass eClass) {
        if (resourceLocator != null) {
            String typeKey = eClass.getName();
            try {
                return resourceLocator.getString("_UI_" + typeKey + "_type");
            } catch (MissingResourceException mre) {
                return typeKey;
            }
        }
        return getString("_UI_Unknown_type");
    }

    protected String getFeatureText(Object feature) {
        String featureKey = "Unknown";
        if (feature instanceof EStructuralFeature) {
            EStructuralFeature eFeature = (EStructuralFeature) feature;
            featureKey = eFeature.getEContainingClass().getName() + "_" + eFeature.getName();
        }
        try {
            return getResourceLocator().getString("_UI_" + featureKey + "_feature");
        } catch (MissingResourceException mre) {
            return featureKey;
        }
    }

    protected ItemPropertyDescriptor createItemPropertyDescriptor(AdapterFactory adapterFactory, ResourceLocator resourceLocator, String displayName, String description, EStructuralFeature feature, boolean isSettable, Object staticImage, String category, String[] filterFlags) {
        return new UMLItemPropertyDescriptor(adapterFactory, resourceLocator, displayName, description, feature, isSettable, staticImage, category == null ? getTypeText(resourceLocator, feature.getEContainingClass()) : category, filterFlags);
    }

    protected ComposedImage getComposedImage(Object object, Object image) {
        List images = new ArrayList();
        images.add(image);
        return new ComposedImage(images);
    }

    protected Object overlayImage(Object object, Object image) {
        ComposedImage composedImage = getComposedImage(object, image);
        Collection images = composedImage.getImages();
        if (object instanceof Element) {
            Element element = (Element) object;
            for (Iterator appliedStereotypes = element.getAppliedStereotypes().iterator(); appliedStereotypes.hasNext(); ) {
                Stereotype appliedStereotype = (Stereotype) appliedStereotypes.next();
                Resource eResource = appliedStereotype.eResource();
                if (eResource != null) {
                    ResourceSet resourceSet = eResource.getResourceSet();
                    if (resourceSet != null) {
                        URIConverter uriConverter = resourceSet.getURIConverter();
                        URI normalizedURI = uriConverter.normalize(eResource.getURI());
                        for (Iterator icons = appliedStereotype.getIcons().iterator(); icons.hasNext(); ) {
                            String location = ((Image) icons.next()).getLocation();
                            if (!UML2Util.isEmpty(location) && location.indexOf("ovr16") != -1) {
                                URI uri = URI.createURI(location).resolve(normalizedURI);
                                try {
                                    URL url = new URL(uriConverter.normalize(uri).toString());
                                    url.openStream().close();
                                    images.add(url);
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                }
            }
        }
        if (AdapterFactoryEditingDomain.isControlled(object)) {
            images.add(getResourceLocator().getImage("full/ovr16/ControlledObject"));
        }
        return composedImage;
    }
}
