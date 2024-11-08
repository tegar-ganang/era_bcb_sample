package net.java.amateras.uml.action;

import net.java.amateras.uml.UMLPlugin;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * @author Takahiro Shida.
 *
 */
public class CopyDiagramImageAction extends AbstractUMLEditorAction {

    public CopyDiagramImageAction(GraphicalViewer viewer) {
        super(UMLPlugin.getDefault().getResourceString("menu.saveAsImage"), viewer);
    }

    public void update(IStructuredSelection sel) {
    }

    public void run() {
        ScalableRootEditPart rootEditPart = (ScalableRootEditPart) getViewer().getRootEditPart();
        double zoom = rootEditPart.getZoomManager().getZoom();
        try {
            rootEditPart.getZoomManager().setZoom(1.0);
            IFigure figure = rootEditPart.getLayer(LayerConstants.PRINTABLE_LAYERS);
            Rectangle rectangle = figure.getBounds();
            Image image = new Image(Display.getDefault(), rectangle.width + 50, rectangle.height + 50);
            GC gc = new GC(image);
            SWTGraphics graphics = new SWTGraphics(gc);
            figure.paint(graphics);
            gc.copyArea(0, 0, figure.getBounds().width, figure.getBounds().height, figure.getBounds().x, figure.getBounds().y);
            image.dispose();
            gc.dispose();
        } catch (Exception ex) {
        } finally {
            rootEditPart.getZoomManager().setZoom(zoom);
        }
    }
}
