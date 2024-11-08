package com.prolix.editor.resourcemanager.zip;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Control;
import com.prolix.editor.main.GLMEditor;

public class ImageSaveManager {

    private String pfad;

    private int filetype;

    private GLMEditor editor;

    public ImageSaveManager(String pfad, GLMEditor editor) {
        this.editor = editor;
        this.pfad = pfad;
        if (this.pfad == null || this.editor == null) {
            this.filetype = -1;
            return;
        }
        this.findFileType();
    }

    private void findFileType() {
        if (this.pfad == null) {
            return;
        }
        int lastIndexPoint = pfad.lastIndexOf(".");
        if (lastIndexPoint <= 0) {
            this.filetype = SWT.IMAGE_PNG;
            this.pfad += ".png";
            return;
        }
        String postFix = pfad.substring(pfad.lastIndexOf(".") + 1);
        if ("jpeg".equalsIgnoreCase(postFix) || "jpg".equalsIgnoreCase(postFix)) {
            this.filetype = SWT.IMAGE_JPEG;
            return;
        }
        if ("png".equalsIgnoreCase(postFix)) {
            this.filetype = SWT.IMAGE_PNG;
            return;
        }
        this.filetype = SWT.IMAGE_PNG;
        this.pfad += ".png";
    }

    private GraphicalViewer getGraphicalViewer() {
        return this.editor.getGraphicalViewer();
    }

    public void saveImage() {
        if (this.pfad == null) {
            throw new IllegalAccessError("Pfad ist nicht angegeben");
        }
        if (this.filetype <= 0) {
            throw new IllegalAccessError("Filetype ist nicht angegeben");
        }
        GraphicalViewer viewer = this.getGraphicalViewer();
        ScalableFreeformRootEditPart rootEditPart = (ScalableFreeformRootEditPart) viewer.getEditPartRegistry().get(LayerManager.ID);
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
        Graphics imgGraphics = new SWTGraphics(imageGC);
        rootFigure.paint(imgGraphics);
        ImageData[] imgData = new ImageData[1];
        imgData[0] = img.getImageData();
        ImageLoader imgLoader = new ImageLoader();
        imgLoader.data = imgData;
        imgLoader.save(this.pfad, this.filetype);
        figureCanvasGC.dispose();
        imageGC.dispose();
        img.dispose();
    }
}
