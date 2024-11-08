package de.mpiwg.vspace.util.images;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Control;

public class SceneImageCreator {

    public static void getImageFromScene(GraphicalViewer viewer, String saveFilePath, int format) {
        ScalableRootEditPart rootEditPart = (ScalableRootEditPart) viewer.getEditPartRegistry().get(LayerManager.ID);
        IFigure rootFigure = ((LayerManager) rootEditPart).getLayer(LayerConstants.PRINTABLE_LAYERS);
        Rectangle rootFigureBounds = rootFigure.getBounds();
        Control figureCanvas = viewer.getControl();
        GC figureCanvasGC = new GC(figureCanvas);
        Image img = new Image(null, rootFigureBounds.width, rootFigureBounds.height);
        GC imageGC = new GC(img);
        imageGC.setBackground(figureCanvasGC.getBackground());
        imageGC.setForeground(figureCanvasGC.getForeground());
        imageGC.setFont(figureCanvasGC.getFont());
        imageGC.setLineStyle(figureCanvasGC.getLineStyle());
        imageGC.setLineWidth(figureCanvasGC.getLineWidth());
        imageGC.setXORMode(figureCanvasGC.getXORMode());
        Graphics imgGraphics = new SWTGraphics(imageGC);
        rootFigure.paint(imgGraphics);
        ImageData[] imgData = new ImageData[1];
        imgData[0] = img.getImageData();
        ImageLoader imgLoader = new ImageLoader();
        imgLoader.data = imgData;
        imgLoader.save(saveFilePath, format);
        figureCanvasGC.dispose();
        imageGC.dispose();
        img.dispose();
    }
}
