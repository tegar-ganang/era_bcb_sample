package com.ivis.xprocess.ui.viewpoints.util;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.swt.printing.Printer;
import org.eclipse.ui.IWorkbenchPart;
import com.ivis.xprocess.ui.diagram.print.AbstractPrintAction;
import com.ivis.xprocess.ui.diagram.print.DiagramPrintableFigure;
import com.ivis.xprocess.ui.diagram.print.PrintableFigure;

public class PrintAction extends AbstractPrintAction {

    private static final String ID = "com.ivis.xprocess.ui.diagram.actions.print";

    private GraphicalViewer myGraphicalViewer;

    public PrintAction(IWorkbenchPart part) {
        this(part, null);
    }

    public PrintAction(IWorkbenchPart part, GraphicalViewer graphicalViewer) {
        super(part);
        myGraphicalViewer = graphicalViewer;
        setActionDefinitionId(ID);
    }

    public void setGraphicalViewer(GraphicalViewer graphicalViewer) {
        myGraphicalViewer = graphicalViewer;
    }

    @Override
    protected boolean calculateEnabled() {
        return Printer.getPrinterList().length > 0;
    }

    @Override
    protected PrintableFigure createPrintableFigure() {
        if (myGraphicalViewer != null) {
            LayerManager layerManager = (LayerManager) myGraphicalViewer.getEditPartRegistry().get(LayerManager.ID);
            IFigure contentLayer = layerManager.getLayer(LayerConstants.PRIMARY_LAYER);
            IFigure connectionLayer = layerManager.getLayer(LayerConstants.CONNECTION_LAYER);
            Dimension size = ((AbstractGraphicalEditPart) myGraphicalViewer.getRootEditPart().getContents()).getFigure().getSize();
            return new DiagramPrintableFigure(contentLayer, connectionLayer, size);
        }
        return null;
    }
}
