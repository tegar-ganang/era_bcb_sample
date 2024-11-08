package org.eclipse.gef.e4;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.tools.SelectionTool;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

public abstract class GraphicalWidget extends BaseWidget {

    public GraphicalWidget() {
        EditDomain editDomain = new EditDomain();
        editDomain.setActiveTool(new SelectionTool());
        setEditDomain(editDomain);
    }

    @Override
    public void createPartControl(Composite parent) {
        createGraphicalViewer(parent);
    }

    protected void createGraphicalViewer(Composite parent) {
        GraphicalViewer viewer = new ScrollingGraphicalViewer();
        viewer.createControl(parent);
        setGraphicalViewer(viewer);
        configureGraphicalViewer();
        hookGraphicalViewer();
        initializeGraphicalViewer();
    }

    protected void setGraphicalViewer(GraphicalViewer viewer) {
        getEditDomain().addViewer(viewer);
        this.graphicalViewer = viewer;
    }

    protected void configureGraphicalViewer() {
        getGraphicalViewer().getControl().setBackground(ColorConstants.listBackground);
        GraphicalViewer viewer = getGraphicalViewer();
    }

    protected EditDomain getEditDomain() {
        return editDomain;
    }

    protected void setEditDomain(EditDomain ed) {
        editDomain = ed;
    }

    protected GraphicalViewer getGraphicalViewer() {
        return graphicalViewer;
    }

    private EditDomain editDomain;

    private GraphicalViewer graphicalViewer;

    protected void hookGraphicalViewer() {
        getSelectionSynchronizer().addViewer(getGraphicalViewer());
        getSite().setSelectionProvider(getGraphicalViewer());
    }

    private SelectionSynchronizer synchronizer;

    protected SelectionSynchronizer getSelectionSynchronizer() {
        if (synchronizer == null) synchronizer = new SelectionSynchronizer();
        return synchronizer;
    }

    protected abstract void initializeGraphicalViewer();

    private ActionRegistry actionRegistry;

    protected ActionRegistry getActionRegistry() {
        if (actionRegistry == null) actionRegistry = new ActionRegistry();
        return actionRegistry;
    }

    protected CommandStack getCommandStack() {
        return getEditDomain().getCommandStack();
    }

    protected void updateActions(List actionIds) {
        ActionRegistry registry = getActionRegistry();
        Iterator iter = actionIds.iterator();
        while (iter.hasNext()) {
            IAction action = registry.getAction(iter.next());
            if (action instanceof UpdateAction) ((UpdateAction) action).update();
        }
    }

    protected List getStackActions() {
        return stackActions;
    }

    protected List getSelectionActions() {
        return selectionActions;
    }

    protected List getPropertyActions() {
        return propertyActions;
    }

    @SuppressWarnings("serial")
    private static class ActionIDList extends ArrayList {

        @SuppressWarnings("unchecked")
        @Override
        public boolean add(Object o) {
            if (o instanceof IAction) {
                try {
                    IAction action = (IAction) o;
                    o = action.getId();
                    throw new IllegalArgumentException("Action IDs should be added to lists, not the action: " + action);
                } catch (IllegalArgumentException exc) {
                    exc.printStackTrace();
                }
            }
            return super.add(o);
        }
    }

    private List selectionActions = new ActionIDList();

    private List stackActions = new ActionIDList();

    private List propertyActions = new ActionIDList();

    public void commandStackChanged(EventObject event) {
        updateActions(stackActions);
    }

    public Object getAdapter(Class type) {
        if (type == GraphicalViewer.class) return getGraphicalViewer();
        if (type == CommandStack.class) return getCommandStack();
        if (type == ActionRegistry.class) return getActionRegistry();
        if (type == EditPart.class && getGraphicalViewer() != null) return getGraphicalViewer().getRootEditPart();
        if (type == IFigure.class && getGraphicalViewer() != null) return ((GraphicalEditPart) getGraphicalViewer().getRootEditPart()).getFigure();
        return null;
    }
}
