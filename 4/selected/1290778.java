package net.sf.ncsimulator.models.network.diagram.edit.policies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.sf.ncsimulator.models.network.NetworkPackage;
import net.sf.ncsimulator.models.network.diagram.edit.parts.ChannelEditPart;
import net.sf.ncsimulator.models.network.diagram.edit.parts.NetworkEditPart;
import net.sf.ncsimulator.models.network.diagram.edit.parts.NodeEditPart;
import net.sf.ncsimulator.models.network.diagram.part.NetworkDiagramUpdater;
import net.sf.ncsimulator.models.network.diagram.part.NetworkLinkDescriptor;
import net.sf.ncsimulator.models.network.diagram.part.NetworkNodeDescriptor;
import net.sf.ncsimulator.models.network.diagram.part.NetworkVisualIDRegistry;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gmf.runtime.diagram.core.util.ViewUtil;
import org.eclipse.gmf.runtime.diagram.ui.commands.DeferredLayoutCommand;
import org.eclipse.gmf.runtime.diagram.ui.commands.ICommandProxy;
import org.eclipse.gmf.runtime.diagram.ui.commands.SetViewMutabilityCommand;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IGraphicalEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editpolicies.CanonicalEditPolicy;
import org.eclipse.gmf.runtime.diagram.ui.requests.CreateConnectionViewRequest;
import org.eclipse.gmf.runtime.diagram.ui.requests.CreateViewRequest;
import org.eclipse.gmf.runtime.diagram.ui.requests.RequestConstants;
import org.eclipse.gmf.runtime.emf.core.util.EObjectAdapter;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.Edge;
import org.eclipse.gmf.runtime.notation.Node;
import org.eclipse.gmf.runtime.notation.View;

/**
 * @generated
 */
public class NetworkCanonicalEditPolicy extends CanonicalEditPolicy {

    /**
	 * @generated
	 */
    protected EStructuralFeature getFeatureToSynchronize() {
        return NetworkPackage.eINSTANCE.getNetwork_Nodes();
    }

    /**
	 * @generated
	 */
    @SuppressWarnings("rawtypes")
    protected List getSemanticChildrenList() {
        View viewObject = (View) getHost().getModel();
        LinkedList<EObject> result = new LinkedList<EObject>();
        List<NetworkNodeDescriptor> childDescriptors = NetworkDiagramUpdater.getNetwork_1000SemanticChildren(viewObject);
        for (NetworkNodeDescriptor d : childDescriptors) {
            result.add(d.getModelElement());
        }
        return result;
    }

    /**
	 * @generated
	 */
    protected boolean isOrphaned(Collection<EObject> semanticChildren, final View view) {
        return isMyDiagramElement(view) && !semanticChildren.contains(view.getElement());
    }

    /**
	 * @generated
	 */
    private boolean isMyDiagramElement(View view) {
        return NodeEditPart.VISUAL_ID == NetworkVisualIDRegistry.getVisualID(view);
    }

    /**
	 * @generated
	 */
    protected void refreshSemantic() {
        if (resolveSemanticElement() == null) {
            return;
        }
        LinkedList<IAdaptable> createdViews = new LinkedList<IAdaptable>();
        List<NetworkNodeDescriptor> childDescriptors = NetworkDiagramUpdater.getNetwork_1000SemanticChildren((View) getHost().getModel());
        LinkedList<View> orphaned = new LinkedList<View>();
        LinkedList<View> knownViewChildren = new LinkedList<View>();
        for (View v : getViewChildren()) {
            if (isMyDiagramElement(v)) {
                knownViewChildren.add(v);
            }
        }
        for (Iterator<NetworkNodeDescriptor> descriptorsIterator = childDescriptors.iterator(); descriptorsIterator.hasNext(); ) {
            NetworkNodeDescriptor next = descriptorsIterator.next();
            String hint = NetworkVisualIDRegistry.getType(next.getVisualID());
            LinkedList<View> perfectMatch = new LinkedList<View>();
            for (View childView : getViewChildren()) {
                EObject semanticElement = childView.getElement();
                if (next.getModelElement().equals(semanticElement)) {
                    if (hint.equals(childView.getType())) {
                        perfectMatch.add(childView);
                    }
                }
            }
            if (perfectMatch.size() > 0) {
                descriptorsIterator.remove();
                knownViewChildren.remove(perfectMatch.getFirst());
            }
        }
        orphaned.addAll(knownViewChildren);
        ArrayList<CreateViewRequest.ViewDescriptor> viewDescriptors = new ArrayList<CreateViewRequest.ViewDescriptor>(childDescriptors.size());
        for (NetworkNodeDescriptor next : childDescriptors) {
            String hint = NetworkVisualIDRegistry.getType(next.getVisualID());
            IAdaptable elementAdapter = new CanonicalElementAdapter(next.getModelElement(), hint);
            CreateViewRequest.ViewDescriptor descriptor = new CreateViewRequest.ViewDescriptor(elementAdapter, Node.class, hint, ViewUtil.APPEND, false, host().getDiagramPreferencesHint());
            viewDescriptors.add(descriptor);
        }
        boolean changed = deleteViews(orphaned.iterator());
        CreateViewRequest request = getCreateViewRequest(viewDescriptors);
        Command cmd = getCreateViewCommand(request);
        if (cmd != null && cmd.canExecute()) {
            SetViewMutabilityCommand.makeMutable(new EObjectAdapter(host().getNotationView())).execute();
            executeCommand(cmd);
            @SuppressWarnings("unchecked") List<IAdaptable> nl = (List<IAdaptable>) request.getNewObject();
            createdViews.addAll(nl);
        }
        if (changed || createdViews.size() > 0) {
            postProcessRefreshSemantic(createdViews);
        }
        Collection<IAdaptable> createdConnectionViews = refreshConnections();
        if (createdViews.size() > 1) {
            DeferredLayoutCommand layoutCmd = new DeferredLayoutCommand(host().getEditingDomain(), createdViews, host());
            executeCommand(new ICommandProxy(layoutCmd));
        }
        createdViews.addAll(createdConnectionViews);
        makeViewsImmutable(createdViews);
    }

    /**
	 * @generated
	 */
    private Collection<IAdaptable> refreshConnections() {
        Map<EObject, View> domain2NotationMap = new HashMap<EObject, View>();
        Collection<NetworkLinkDescriptor> linkDescriptors = collectAllLinks(getDiagram(), domain2NotationMap);
        Collection existingLinks = new LinkedList(getDiagram().getEdges());
        for (Iterator linksIterator = existingLinks.iterator(); linksIterator.hasNext(); ) {
            Edge nextDiagramLink = (Edge) linksIterator.next();
            int diagramLinkVisualID = NetworkVisualIDRegistry.getVisualID(nextDiagramLink);
            if (diagramLinkVisualID == -1) {
                if (nextDiagramLink.getSource() != null && nextDiagramLink.getTarget() != null) {
                    linksIterator.remove();
                }
                continue;
            }
            EObject diagramLinkObject = nextDiagramLink.getElement();
            EObject diagramLinkSrc = nextDiagramLink.getSource().getElement();
            EObject diagramLinkDst = nextDiagramLink.getTarget().getElement();
            for (Iterator<NetworkLinkDescriptor> linkDescriptorsIterator = linkDescriptors.iterator(); linkDescriptorsIterator.hasNext(); ) {
                NetworkLinkDescriptor nextLinkDescriptor = linkDescriptorsIterator.next();
                if (diagramLinkObject == nextLinkDescriptor.getModelElement() && diagramLinkSrc == nextLinkDescriptor.getSource() && diagramLinkDst == nextLinkDescriptor.getDestination() && diagramLinkVisualID == nextLinkDescriptor.getVisualID()) {
                    linksIterator.remove();
                    linkDescriptorsIterator.remove();
                    break;
                }
            }
        }
        deleteViews(existingLinks.iterator());
        return createConnections(linkDescriptors, domain2NotationMap);
    }

    /**
	 * @generated
	 */
    private Collection<NetworkLinkDescriptor> collectAllLinks(View view, Map<EObject, View> domain2NotationMap) {
        if (!NetworkEditPart.MODEL_ID.equals(NetworkVisualIDRegistry.getModelID(view))) {
            return Collections.emptyList();
        }
        LinkedList<NetworkLinkDescriptor> result = new LinkedList<NetworkLinkDescriptor>();
        switch(NetworkVisualIDRegistry.getVisualID(view)) {
            case NetworkEditPart.VISUAL_ID:
                {
                    if (!domain2NotationMap.containsKey(view.getElement())) {
                        result.addAll(NetworkDiagramUpdater.getNetwork_1000ContainedLinks(view));
                    }
                    if (!domain2NotationMap.containsKey(view.getElement()) || view.getEAnnotation("Shortcut") == null) {
                        domain2NotationMap.put(view.getElement(), view);
                    }
                    break;
                }
            case NodeEditPart.VISUAL_ID:
                {
                    if (!domain2NotationMap.containsKey(view.getElement())) {
                        result.addAll(NetworkDiagramUpdater.getNode_2001ContainedLinks(view));
                    }
                    if (!domain2NotationMap.containsKey(view.getElement()) || view.getEAnnotation("Shortcut") == null) {
                        domain2NotationMap.put(view.getElement(), view);
                    }
                    break;
                }
            case ChannelEditPart.VISUAL_ID:
                {
                    if (!domain2NotationMap.containsKey(view.getElement())) {
                        result.addAll(NetworkDiagramUpdater.getChannel_4003ContainedLinks(view));
                    }
                    if (!domain2NotationMap.containsKey(view.getElement()) || view.getEAnnotation("Shortcut") == null) {
                        domain2NotationMap.put(view.getElement(), view);
                    }
                    break;
                }
        }
        for (Iterator children = view.getChildren().iterator(); children.hasNext(); ) {
            result.addAll(collectAllLinks((View) children.next(), domain2NotationMap));
        }
        for (Iterator edges = view.getSourceEdges().iterator(); edges.hasNext(); ) {
            result.addAll(collectAllLinks((View) edges.next(), domain2NotationMap));
        }
        return result;
    }

    /**
	 * @generated
	 */
    private Collection<IAdaptable> createConnections(Collection<NetworkLinkDescriptor> linkDescriptors, Map<EObject, View> domain2NotationMap) {
        LinkedList<IAdaptable> adapters = new LinkedList<IAdaptable>();
        for (NetworkLinkDescriptor nextLinkDescriptor : linkDescriptors) {
            EditPart sourceEditPart = getEditPart(nextLinkDescriptor.getSource(), domain2NotationMap);
            EditPart targetEditPart = getEditPart(nextLinkDescriptor.getDestination(), domain2NotationMap);
            if (sourceEditPart == null || targetEditPart == null) {
                continue;
            }
            CreateConnectionViewRequest.ConnectionViewDescriptor descriptor = new CreateConnectionViewRequest.ConnectionViewDescriptor(nextLinkDescriptor.getSemanticAdapter(), NetworkVisualIDRegistry.getType(nextLinkDescriptor.getVisualID()), ViewUtil.APPEND, false, ((IGraphicalEditPart) getHost()).getDiagramPreferencesHint());
            CreateConnectionViewRequest ccr = new CreateConnectionViewRequest(descriptor);
            ccr.setType(RequestConstants.REQ_CONNECTION_START);
            ccr.setSourceEditPart(sourceEditPart);
            sourceEditPart.getCommand(ccr);
            ccr.setTargetEditPart(targetEditPart);
            ccr.setType(RequestConstants.REQ_CONNECTION_END);
            Command cmd = targetEditPart.getCommand(ccr);
            if (cmd != null && cmd.canExecute()) {
                executeCommand(cmd);
                IAdaptable viewAdapter = (IAdaptable) ccr.getNewObject();
                if (viewAdapter != null) {
                    adapters.add(viewAdapter);
                }
            }
        }
        return adapters;
    }

    /**
	 * @generated
	 */
    private EditPart getEditPart(EObject domainModelElement, Map<EObject, View> domain2NotationMap) {
        View view = (View) domain2NotationMap.get(domainModelElement);
        if (view != null) {
            return (EditPart) getHost().getViewer().getEditPartRegistry().get(view);
        }
        return null;
    }

    /**
	 * @generated
	 */
    private Diagram getDiagram() {
        return ((View) getHost().getModel()).getDiagram();
    }
}
