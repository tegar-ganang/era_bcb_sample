package com.ivis.xprocess.ui.diagram.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.ui.IWorkbenchPart;
import com.ivis.xprocess.ui.UIType;

public class ZoomInAction extends AbstractZoomAction {

    private static final String ID = "com.ivis.xprocess.ui.diagram.actions.zoom_in";

    public ZoomInAction(IWorkbenchPart part, GraphicalViewer graphicalViewer) {
        super(part, graphicalViewer, UIType.zoom_in, ID);
    }

    @Override
    protected boolean calculateEnabled(ZoomManager zoomer) {
        return zoomer.canZoomIn();
    }

    @Override
    protected void run(ZoomManager zoomer) {
        zoomer.setZoom(zoomer.getZoom() * ZOOM_STEP);
    }
}
