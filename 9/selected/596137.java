package com.ivis.xprocess.ui.diagram.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.ui.IWorkbenchPart;
import com.ivis.xprocess.ui.UIType;

public class ZoomOutAction extends AbstractZoomAction {

    private static final String ID = "com.ivis.xprocess.ui.diagram.actions.zoom_out";

    public ZoomOutAction(IWorkbenchPart part, GraphicalViewer graphicalViewer) {
        super(part, graphicalViewer, UIType.zoom_out, ID);
    }

    @Override
    protected boolean calculateEnabled(ZoomManager zoomer) {
        return zoomer.canZoomOut();
    }

    @Override
    protected void run(ZoomManager zoomer) {
        zoomer.setZoom(zoomer.getZoom() / ZOOM_STEP);
    }
}
