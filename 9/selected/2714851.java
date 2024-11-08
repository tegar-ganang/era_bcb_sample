package com.ivis.xprocess.ui.diagram.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.ui.IWorkbenchPart;
import com.ivis.xprocess.ui.UIType;

public class ZoomFitAction extends AbstractZoomAction {

    private static final String ID = "com.ivis.xprocess.ui.diagram.actions.zoom_fit";

    public ZoomFitAction(IWorkbenchPart part, GraphicalViewer graphicalViewer) {
        super(part, graphicalViewer, UIType.zoom_fit, ID);
    }

    @Override
    protected boolean calculateEnabled(ZoomManager zoomer) {
        return true;
    }

    @Override
    protected void run(ZoomManager zoomer) {
        zoomer.setZoomAsText(ZoomManager.FIT_ALL);
    }
}
