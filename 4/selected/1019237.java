package net.sf.ncsimulator.models.network.diagram.providers;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.sf.ncsimulator.models.network.NetworkPackage;
import net.sf.ncsimulator.models.network.diagram.edit.parts.ChannelEditPart;
import net.sf.ncsimulator.models.network.diagram.edit.parts.NetworkEditPart;
import net.sf.ncsimulator.models.network.diagram.edit.parts.NodeEditPart;
import net.sf.ncsimulator.models.network.diagram.part.NetworkDiagramEditorPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.gmf.runtime.emf.type.core.ElementTypeRegistry;
import org.eclipse.gmf.runtime.emf.type.core.IElementType;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * @generated
 */
public class NetworkElementTypes {

    /**
	 * @generated
	 */
    private NetworkElementTypes() {
    }

    /**
	 * @generated
	 */
    private static Map<IElementType, ENamedElement> elements;

    /**
	 * @generated
	 */
    private static ImageRegistry imageRegistry;

    /**
	 * @generated
	 */
    private static Set<IElementType> KNOWN_ELEMENT_TYPES;

    /**
	 * @generated
	 */
    public static final IElementType Network_1000 = getElementType("net.sf.ncsimulator.models.network.diagram.Network_1000");

    /**
	 * @generated
	 */
    public static final IElementType Node_2001 = getElementType("net.sf.ncsimulator.models.network.diagram.Node_2001");

    /**
	 * @generated
	 */
    public static final IElementType Channel_4003 = getElementType("net.sf.ncsimulator.models.network.diagram.Channel_4003");

    /**
	 * @generated
	 */
    private static ImageRegistry getImageRegistry() {
        if (imageRegistry == null) {
            imageRegistry = new ImageRegistry();
        }
        return imageRegistry;
    }

    /**
	 * @generated
	 */
    private static String getImageRegistryKey(ENamedElement element) {
        return element.getName();
    }

    /**
	 * @generated
	 */
    private static ImageDescriptor getProvidedImageDescriptor(ENamedElement element) {
        if (element instanceof EStructuralFeature) {
            EStructuralFeature feature = ((EStructuralFeature) element);
            EClass eContainingClass = feature.getEContainingClass();
            EClassifier eType = feature.getEType();
            if (eContainingClass != null && !eContainingClass.isAbstract()) {
                element = eContainingClass;
            } else if (eType instanceof EClass && !((EClass) eType).isAbstract()) {
                element = eType;
            }
        }
        if (element instanceof EClass) {
            EClass eClass = (EClass) element;
            if (!eClass.isAbstract()) {
                return NetworkDiagramEditorPlugin.getInstance().getItemImageDescriptor(eClass.getEPackage().getEFactoryInstance().create(eClass));
            }
        }
        return null;
    }

    /**
	 * @generated
	 */
    public static ImageDescriptor getImageDescriptor(ENamedElement element) {
        String key = getImageRegistryKey(element);
        ImageDescriptor imageDescriptor = getImageRegistry().getDescriptor(key);
        if (imageDescriptor == null) {
            imageDescriptor = getProvidedImageDescriptor(element);
            if (imageDescriptor == null) {
                imageDescriptor = ImageDescriptor.getMissingImageDescriptor();
            }
            getImageRegistry().put(key, imageDescriptor);
        }
        return imageDescriptor;
    }

    /**
	 * @generated
	 */
    public static Image getImage(ENamedElement element) {
        String key = getImageRegistryKey(element);
        Image image = getImageRegistry().get(key);
        if (image == null) {
            ImageDescriptor imageDescriptor = getProvidedImageDescriptor(element);
            if (imageDescriptor == null) {
                imageDescriptor = ImageDescriptor.getMissingImageDescriptor();
            }
            getImageRegistry().put(key, imageDescriptor);
            image = getImageRegistry().get(key);
        }
        return image;
    }

    /**
	 * @generated
	 */
    public static ImageDescriptor getImageDescriptor(IAdaptable hint) {
        ENamedElement element = getElement(hint);
        if (element == null) {
            return null;
        }
        return getImageDescriptor(element);
    }

    /**
	 * @generated
	 */
    public static Image getImage(IAdaptable hint) {
        ENamedElement element = getElement(hint);
        if (element == null) {
            return null;
        }
        return getImage(element);
    }

    /**
	 * Returns 'type' of the ecore object associated with the hint.
	 * 
	 * @generated
	 */
    public static ENamedElement getElement(IAdaptable hint) {
        Object type = hint.getAdapter(IElementType.class);
        if (elements == null) {
            elements = new IdentityHashMap<IElementType, ENamedElement>();
            elements.put(Network_1000, NetworkPackage.eINSTANCE.getNetwork());
            elements.put(Node_2001, NetworkPackage.eINSTANCE.getNode());
            elements.put(Channel_4003, NetworkPackage.eINSTANCE.getChannel());
        }
        return (ENamedElement) elements.get(type);
    }

    /**
	 * @generated
	 */
    private static IElementType getElementType(String id) {
        return ElementTypeRegistry.getInstance().getType(id);
    }

    /**
	 * @generated
	 */
    public static boolean isKnownElementType(IElementType elementType) {
        if (KNOWN_ELEMENT_TYPES == null) {
            KNOWN_ELEMENT_TYPES = new HashSet<IElementType>();
            KNOWN_ELEMENT_TYPES.add(Network_1000);
            KNOWN_ELEMENT_TYPES.add(Node_2001);
            KNOWN_ELEMENT_TYPES.add(Channel_4003);
        }
        return KNOWN_ELEMENT_TYPES.contains(elementType);
    }

    /**
	 * @generated
	 */
    public static IElementType getElementType(int visualID) {
        switch(visualID) {
            case NetworkEditPart.VISUAL_ID:
                return Network_1000;
            case NodeEditPart.VISUAL_ID:
                return Node_2001;
            case ChannelEditPart.VISUAL_ID:
                return Channel_4003;
        }
        return null;
    }
}
