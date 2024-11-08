package net.sf.ncsimulator.models.network.diagram.part;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.sf.ncsimulator.models.network.Channel;
import net.sf.ncsimulator.models.network.Network;
import net.sf.ncsimulator.models.network.NetworkPackage;
import net.sf.ncsimulator.models.network.Node;
import net.sf.ncsimulator.models.network.diagram.edit.parts.ChannelEditPart;
import net.sf.ncsimulator.models.network.diagram.edit.parts.NetworkEditPart;
import net.sf.ncsimulator.models.network.diagram.edit.parts.NodeEditPart;
import net.sf.ncsimulator.models.network.diagram.providers.NetworkElementTypes;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.gmf.runtime.notation.View;

/**
 * @generated
 */
public class NetworkDiagramUpdater {

    /**
	 * @generated
	 */
    public static List<NetworkNodeDescriptor> getSemanticChildren(View view) {
        switch(NetworkVisualIDRegistry.getVisualID(view)) {
            case NetworkEditPart.VISUAL_ID:
                return getNetwork_1000SemanticChildren(view);
        }
        return Collections.emptyList();
    }

    /**
	 * @generated
	 */
    public static List<NetworkNodeDescriptor> getNetwork_1000SemanticChildren(View view) {
        if (!view.isSetElement()) {
            return Collections.emptyList();
        }
        Network modelElement = (Network) view.getElement();
        LinkedList<NetworkNodeDescriptor> result = new LinkedList<NetworkNodeDescriptor>();
        for (Iterator<?> it = modelElement.getNodes().iterator(); it.hasNext(); ) {
            Node childElement = (Node) it.next();
            int visualID = NetworkVisualIDRegistry.getNodeVisualID(view, childElement);
            if (visualID == NodeEditPart.VISUAL_ID) {
                result.add(new NetworkNodeDescriptor(childElement, visualID));
                continue;
            }
        }
        return result;
    }

    /**
	 * @generated
	 */
    public static List<NetworkLinkDescriptor> getContainedLinks(View view) {
        switch(NetworkVisualIDRegistry.getVisualID(view)) {
            case NetworkEditPart.VISUAL_ID:
                return getNetwork_1000ContainedLinks(view);
            case NodeEditPart.VISUAL_ID:
                return getNode_2001ContainedLinks(view);
            case ChannelEditPart.VISUAL_ID:
                return getChannel_4003ContainedLinks(view);
        }
        return Collections.emptyList();
    }

    /**
	 * @generated
	 */
    public static List<NetworkLinkDescriptor> getIncomingLinks(View view) {
        switch(NetworkVisualIDRegistry.getVisualID(view)) {
            case NodeEditPart.VISUAL_ID:
                return getNode_2001IncomingLinks(view);
            case ChannelEditPart.VISUAL_ID:
                return getChannel_4003IncomingLinks(view);
        }
        return Collections.emptyList();
    }

    /**
	 * @generated
	 */
    public static List<NetworkLinkDescriptor> getOutgoingLinks(View view) {
        switch(NetworkVisualIDRegistry.getVisualID(view)) {
            case NodeEditPart.VISUAL_ID:
                return getNode_2001OutgoingLinks(view);
            case ChannelEditPart.VISUAL_ID:
                return getChannel_4003OutgoingLinks(view);
        }
        return Collections.emptyList();
    }

    /**
	 * @generated
	 */
    public static List<NetworkLinkDescriptor> getNetwork_1000ContainedLinks(View view) {
        Network modelElement = (Network) view.getElement();
        LinkedList<NetworkLinkDescriptor> result = new LinkedList<NetworkLinkDescriptor>();
        result.addAll(getContainedTypeModelFacetLinks_Channel_4003(modelElement));
        return result;
    }

    /**
	 * @generated
	 */
    public static List<NetworkLinkDescriptor> getNode_2001ContainedLinks(View view) {
        return Collections.emptyList();
    }

    /**
	 * @generated
	 */
    public static List<NetworkLinkDescriptor> getChannel_4003ContainedLinks(View view) {
        return Collections.emptyList();
    }

    /**
	 * @generated
	 */
    public static List<NetworkLinkDescriptor> getNode_2001IncomingLinks(View view) {
        Node modelElement = (Node) view.getElement();
        Map<EObject, Collection<EStructuralFeature.Setting>> crossReferences = EcoreUtil.CrossReferencer.find(view.eResource().getResourceSet().getResources());
        LinkedList<NetworkLinkDescriptor> result = new LinkedList<NetworkLinkDescriptor>();
        result.addAll(getIncomingTypeModelFacetLinks_Channel_4003(modelElement, crossReferences));
        return result;
    }

    /**
	 * @generated
	 */
    public static List<NetworkLinkDescriptor> getChannel_4003IncomingLinks(View view) {
        return Collections.emptyList();
    }

    /**
	 * @generated
	 */
    public static List<NetworkLinkDescriptor> getNode_2001OutgoingLinks(View view) {
        Node modelElement = (Node) view.getElement();
        LinkedList<NetworkLinkDescriptor> result = new LinkedList<NetworkLinkDescriptor>();
        result.addAll(getOutgoingTypeModelFacetLinks_Channel_4003(modelElement));
        return result;
    }

    /**
	 * @generated
	 */
    public static List<NetworkLinkDescriptor> getChannel_4003OutgoingLinks(View view) {
        return Collections.emptyList();
    }

    /**
	 * @generated
	 */
    private static Collection<NetworkLinkDescriptor> getContainedTypeModelFacetLinks_Channel_4003(Network container) {
        LinkedList<NetworkLinkDescriptor> result = new LinkedList<NetworkLinkDescriptor>();
        for (Iterator<?> links = container.getChannels().iterator(); links.hasNext(); ) {
            EObject linkObject = (EObject) links.next();
            if (false == linkObject instanceof Channel) {
                continue;
            }
            Channel link = (Channel) linkObject;
            if (ChannelEditPart.VISUAL_ID != NetworkVisualIDRegistry.getLinkWithClassVisualID(link)) {
                continue;
            }
            Node dst = link.getTarget();
            Node src = link.getSource();
            result.add(new NetworkLinkDescriptor(src, dst, link, NetworkElementTypes.Channel_4003, ChannelEditPart.VISUAL_ID));
        }
        return result;
    }

    /**
	 * @generated
	 */
    private static Collection<NetworkLinkDescriptor> getIncomingTypeModelFacetLinks_Channel_4003(Node target, Map<EObject, Collection<EStructuralFeature.Setting>> crossReferences) {
        LinkedList<NetworkLinkDescriptor> result = new LinkedList<NetworkLinkDescriptor>();
        Collection<EStructuralFeature.Setting> settings = crossReferences.get(target);
        for (EStructuralFeature.Setting setting : settings) {
            if (setting.getEStructuralFeature() != NetworkPackage.eINSTANCE.getChannel_Target() || false == setting.getEObject() instanceof Channel) {
                continue;
            }
            Channel link = (Channel) setting.getEObject();
            if (ChannelEditPart.VISUAL_ID != NetworkVisualIDRegistry.getLinkWithClassVisualID(link)) {
                continue;
            }
            Node src = link.getSource();
            result.add(new NetworkLinkDescriptor(src, target, link, NetworkElementTypes.Channel_4003, ChannelEditPart.VISUAL_ID));
        }
        return result;
    }

    /**
	 * @generated
	 */
    private static Collection<NetworkLinkDescriptor> getOutgoingTypeModelFacetLinks_Channel_4003(Node source) {
        Network container = null;
        for (EObject element = source; element != null && container == null; element = element.eContainer()) {
            if (element instanceof Network) {
                container = (Network) element;
            }
        }
        if (container == null) {
            return Collections.emptyList();
        }
        LinkedList<NetworkLinkDescriptor> result = new LinkedList<NetworkLinkDescriptor>();
        for (Iterator<?> links = container.getChannels().iterator(); links.hasNext(); ) {
            EObject linkObject = (EObject) links.next();
            if (false == linkObject instanceof Channel) {
                continue;
            }
            Channel link = (Channel) linkObject;
            if (ChannelEditPart.VISUAL_ID != NetworkVisualIDRegistry.getLinkWithClassVisualID(link)) {
                continue;
            }
            Node dst = link.getTarget();
            Node src = link.getSource();
            if (src != source) {
                continue;
            }
            result.add(new NetworkLinkDescriptor(src, dst, link, NetworkElementTypes.Channel_4003, ChannelEditPart.VISUAL_ID));
        }
        return result;
    }
}
