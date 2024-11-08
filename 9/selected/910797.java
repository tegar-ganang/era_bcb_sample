package org.eclipse.gef.util;

import java.util.EventObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

/**
 * A GEF editor for an EMF resource. It supports
 * <ul>
 * <li>command stack operations</li>
 * <li>selection</li>
 * <li>zooming</li>
 * <li>undoable property sheet page</li>
 * <li>tree and graphical outline</li>
 * <li>direct edit</li>
 * </ul>
 */
public abstract class GraphicalResourceEditor extends EditorPart implements CommandStackListener, ISelectionListener {

    protected final ScrollingGraphicalViewer graphicalViewer = new ScrollingGraphicalViewer();

    protected final DefaultEditDomain editDomain = new DefaultEditDomain(this);

    protected final EditPartFactory editPartFactory;

    protected final ActionRegistry actionRegistry = new ActionRegistry();

    protected final IAction undoAction = new UndoAction(this);

    protected final IAction redoAction = new RedoAction(this);

    protected final IAction deleteAction = new DeleteAction((IWorkbenchPart) this);

    protected final IAction directEditAction = new DirectEditAction((IWorkbenchPart) this);

    public GraphicalResourceEditor(EditPartFactory editPartFactory) {
        super();
        this.editPartFactory = editPartFactory;
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        updateInput(input);
        getCommandStack().addCommandStackListener(this);
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
        initializeActionRegistry();
    }

    @Override
    public void createPartControl(Composite parent) {
        initializeGraphicalViewer(parent);
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    @Override
    public void setFocus() {
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
    }

    @Override
    public void dispose() {
        getCommandStack().removeCommandStackListener(this);
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
        getActionRegistry().dispose();
        super.dispose();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == GraphicalViewer.class) {
            return getGraphicalViewer();
        } else if (adapter == CommandStack.class) {
            return getCommandStack();
        } else if (adapter == ZoomManager.class) {
            return getZoomManager();
        } else {
            return super.getAdapter(adapter);
        }
    }

    public void commandStackChanged(EventObject event) {
        updateAction(getUndoAction());
        updateAction(getRedoAction());
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        updateAction(getDeleteAction());
        updateAction(getDirectEditAction());
    }

    protected void initializeGraphicalViewer(Composite parent) {
        ScrollingGraphicalViewer viewer = getGraphicalViewer();
        viewer.createControl(parent);
        viewer.setRootEditPart(new ScalableRootEditPart());
        viewer.getControl().setBackground(ColorConstants.white);
        getEditDomain().addViewer(viewer);
        getSite().setSelectionProvider(viewer);
        viewer.setEditPartFactory(getEditPartFactory());
        getGraphicalViewer().setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), MouseWheelZoomHandler.SINGLETON);
    }

    protected void initializeActionRegistry() {
        ActionRegistry registry = getActionRegistry();
        registry.registerAction(getUndoAction());
        registry.registerAction(getRedoAction());
        registry.registerAction(getDeleteAction());
        IAction printAction = new PrintAction(this);
        registry.registerAction(printAction);
        IAction zoomInAction = new ZoomInAction(getZoomManager());
        registry.registerAction(zoomInAction);
        IAction zoomOutAction = new ZoomOutAction(getZoomManager());
        registry.registerAction(zoomOutAction);
        registry.registerAction(directEditAction);
    }

    /**
	 * Updates an action's state in the action registry.
	 * 
	 * @param action
	 * the action to update
	 */
    protected void updateAction(IAction action) {
        ActionRegistry registry = getActionRegistry();
        IAction registeredAction = registry.getAction(action.getId());
        if ((registeredAction != null) && (registeredAction instanceof UpdateAction)) {
            ((UpdateAction) registeredAction).update();
        }
    }

    /**
	 * Updates the editor's input.
	 * 
	 * @param input
	 * the new editor input
	 */
    protected void updateInput(IEditorInput input) {
        setInputWithNotify(input);
        setPartName(input.getName());
    }

    public ScrollingGraphicalViewer getGraphicalViewer() {
        return graphicalViewer;
    }

    protected ScalableRootEditPart getRootEditPart() {
        return (ScalableRootEditPart) getGraphicalViewer().getRootEditPart();
    }

    protected ZoomManager getZoomManager() {
        return getRootEditPart().getZoomManager();
    }

    protected DefaultEditDomain getEditDomain() {
        return editDomain;
    }

    protected CommandStack getCommandStack() {
        return getEditDomain().getCommandStack();
    }

    protected ActionRegistry getActionRegistry() {
        return actionRegistry;
    }

    protected IAction getUndoAction() {
        return undoAction;
    }

    protected IAction getRedoAction() {
        return redoAction;
    }

    protected IAction getDeleteAction() {
        return deleteAction;
    }

    protected IAction getDirectEditAction() {
        return directEditAction;
    }

    public EditPartFactory getEditPartFactory() {
        return editPartFactory;
    }
}
