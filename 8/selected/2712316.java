package org.gvt.action;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.gvt.ChisioMain;
import org.gvt.model.BioPAXGraph;
import org.gvt.model.biopaxl2.ComplexMember;
import org.patika.mada.graph.GraphObject;
import org.patika.mada.graph.Node;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This class is an abstract class for Local Query Actions.
 *
 * @author Shatlyk Ashyralyev
 * 
 * Copyright: I-Vis Research Group, Bilkent University, 2007
 */
public abstract class AbstractLocalQueryAction extends Action {

    ChisioMain main;

    /**
	 * Constructor
	 */
    public AbstractLocalQueryAction(ChisioMain main, String arg) {
        super(arg);
        this.main = main;
    }

    /**
	 * Method that finds selected Nodes in graph
	 */
    protected Set<Node> getSelectedNodes() {
        Set<Node> selectedNodes = new HashSet<Node>();
        ScrollingGraphicalViewer viewer = main.getViewer();
        Iterator selectedObjectsIterator = ((IStructuredSelection) viewer.getSelection()).iterator();
        Set<GraphObject> selectedObjects = new HashSet<GraphObject>();
        while (selectedObjectsIterator.hasNext()) {
            Object model = ((EditPart) selectedObjectsIterator.next()).getModel();
            if (model instanceof Node) {
                Node node = (Node) model;
                if (node instanceof ComplexMember) {
                    node = node.getParents().iterator().next();
                }
                selectedObjects.add(node);
            }
        }
        Set<GraphObject> sourceSet = main.getPathwayGraph().getCorrespOrig(selectedObjects);
        for (GraphObject go : sourceSet) {
            if (go instanceof Node) {
                selectedNodes.add((Node) go);
            }
        }
        return selectedNodes;
    }

    /**
	 * Views result of Query in current view or new view
	 * Highlights resultant objects
	 */
    protected void viewAndHighlightResult(Set<GraphObject> result, boolean isCurrentView, String query) {
        if (isCurrentView) {
            Set<GraphObject> content = new HashSet<GraphObject>();
            content.addAll(main.getPathwayGraph().getNodes());
            content.addAll(result);
            UpdatePathwayAction upa = new UpdatePathwayAction(main, content);
            upa.run();
            result = main.getPathwayGraph().getCorrespMember(result);
        } else {
            BioPAXGraph pathwayGraph = main.getRootGraph().excise(result);
            pathwayGraph.setName(query);
            main.createNewTab(pathwayGraph);
            new CoSELayoutAction(main).run();
            result = pathwayGraph.getCorrespMember(result);
        }
        for (GraphObject go : result) {
            go.setHighlight(true);
        }
    }
}
