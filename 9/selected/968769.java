package org.eclipse.gef.util;

import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * A graphical editor with:
 * <ul>
 * <li>freeform root edit part</li>
 * <li>flyout palette</li>
 * <li>zoom support</li>
 * <li>direct edit support</li>
 * </ul>
 */
public abstract class AdvancedGraphicalEditor extends GraphicalEditorWithFlyoutPalette {

    public ScalableFreeformRootEditPart getRootEditPart() {
        return (ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart();
    }

    protected ZoomManager getZoomManager() {
        return getRootEditPart().getZoomManager();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getAdapter(Class type) {
        if (type == ZoomManager.class) {
            return getZoomManager();
        } else {
            return super.getAdapter(type);
        }
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        getGraphicalViewer().setRootEditPart(new ScalableFreeformRootEditPart());
        IAction zoomInAction = new ZoomInAction(getZoomManager());
        IAction zoomOutAction = new ZoomOutAction(getZoomManager());
        getActionRegistry().registerAction(zoomInAction);
        getActionRegistry().registerAction(zoomOutAction);
        IHandlerService handlerService = (IHandlerService) getSite().getWorkbenchWindow().getService(IHandlerService.class);
        handlerService.activateHandler(zoomInAction.getActionDefinitionId(), new ActionHandler(zoomInAction));
        handlerService.activateHandler(zoomOutAction.getActionDefinitionId(), new ActionHandler(zoomOutAction));
        getGraphicalViewer().setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), MouseWheelZoomHandler.SINGLETON);
    }

    /**
	 * Scales the zoom levels without affecting their UI text representations.
	 */
    public void scaleZoomLevels(double ratio) {
        double[] zoomLevels = getZoomManager().getZoomLevels();
        for (int i = 0; i < zoomLevels.length; i++) {
            zoomLevels[i] *= ratio;
        }
        getZoomManager().setZoomLevels(zoomLevels);
        getZoomManager().setUIMultiplier(1 / ratio);
        getZoomManager().setZoomAsText("100%");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void createActions() {
        super.createActions();
        Action action = new DirectEditAction((IWorkbenchPart) this);
        getActionRegistry().registerAction(action);
        getSelectionActions().add(action.getId());
    }
}
