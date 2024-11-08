package com.ivis.xprocess.ui.diagram.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.ui.IWorkbenchPart;
import com.ivis.xprocess.ui.UIType;

public class ZoomOriginalAction extends AbstractZoomAction {

    private static final String ID = "com.ivis.xprocess.ui.diagram.actions.zoom_original";

    public ZoomOriginalAction(IWorkbenchPart part, GraphicalViewer graphicalViewer) {
        super(part, graphicalViewer, UIType.zoom_original, ID);
    }

    @Override
    protected boolean calculateEnabled(ZoomManager zoomer) {
        return true;
    }

    @Override
    protected void run(ZoomManager zoomer) {
        zoomer.setZoom(1);
    }
}
