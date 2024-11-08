package org.eclipse.rap.flexdraw2d.examples.gef;

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
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;

public abstract class GraphicalView extends ViewPart implements CommandStackListener, ISelectionListener {

    @Override
    public void setFocus() {
    }

    public abstract static class GraphicalAction implements IViewActionDelegate, IPropertyChangeListener, IActionDelegate2 {

        protected GraphicalView view = null;

        protected IAction proxyAction = null;

        public void init(IViewPart _view) {
            view = (GraphicalView) _view;
            getChildAction().addPropertyChangeListener(this);
        }

        protected abstract IAction getChildAction();

        public void init(IAction action) {
            proxyAction = action;
        }

        public void run(IAction action) {
        }

        public void selectionChanged(IAction action, ISelection selection) {
        }

        public void propertyChange(PropertyChangeEvent event) {
            if (proxyAction != null) {
                if (getChildAction().isEnabled()) proxyAction.setEnabled(true); else proxyAction.setEnabled(false);
            }
        }

        public void dispose() {
            getChildAction().removePropertyChangeListener(this);
        }

        public void runWithEvent(IAction action, Event event) {
            getChildAction().run();
        }
    }

    public static class ViewEditDomain extends EditDomain {

        private IViewPart viewPart;

        public ViewEditDomain(IViewPart viewPart) {
            setViewPart(viewPart);
        }

        public IViewPart getViewPart() {
            return viewPart;
        }

        protected void setViewPart(IViewPart viewPart) {
            this.viewPart = viewPart;
        }
    }

    public GraphicalView() {
        EditDomain viewEditDomain = new ViewEditDomain(this);
        viewEditDomain.setActiveTool(new SelectionTool());
        setEditDomain(viewEditDomain);
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

    protected boolean trapKeys(Event e) {
        return false;
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

    @Override
    public void init(IViewSite site) throws PartInitException {
        setSite(site);
        getCommandStack().addCommandStackListener(this);
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
        initializeActionRegistry();
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (this.equals(getSite().getPage().getActivePart())) updateActions(selectionActions);
    }

    protected void initializeActionRegistry() {
        createActions();
        updateActions(propertyActions);
        updateActions(stackActions);
    }

    @SuppressWarnings("unchecked")
    protected void createActions() {
        ActionRegistry registry = getActionRegistry();
        IAction action;
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

    @Override
    public Object getAdapter(Class type) {
        if (type == GraphicalViewer.class) return getGraphicalViewer();
        if (type == CommandStack.class) return getCommandStack();
        if (type == ActionRegistry.class) return getActionRegistry();
        if (type == EditPart.class && getGraphicalViewer() != null) return getGraphicalViewer().getRootEditPart();
        if (type == IFigure.class && getGraphicalViewer() != null) return ((GraphicalEditPart) getGraphicalViewer().getRootEditPart()).getFigure();
        return super.getAdapter(type);
    }
}
