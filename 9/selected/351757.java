package org.isistan.flabot.util;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorPart;
import org.isistan.flabot.messages.Messages;

/**
 * Save format should be any of ... SWT.IMAGE_BMP, SWT.IMAGE_JPEG, SWT.IMAGE_ICO
 */
public class ImageSaveUtil {

    public static boolean save(IEditorPart editorPart, GraphicalViewer viewer, String saveFilePath, int format) {
        Assert.isNotNull(editorPart, "null editorPart passed to ImageSaveUtil::save");
        Assert.isNotNull(viewer, "null viewer passed to ImageSaveUtil::save");
        Assert.isNotNull(saveFilePath, "null saveFilePath passed to ImageSaveUtil::save");
        if (format != SWT.IMAGE_BMP && format != SWT.IMAGE_JPEG && format != SWT.IMAGE_ICO) throw new IllegalArgumentException(Messages.getString("org.isistan.flabot.edit.ImageSaveUtil.exceptionName"));
        try {
            saveEditorContentsAsImage(editorPart, viewer, saveFilePath, format);
        } catch (Exception ex) {
            MessageDialog.openError(editorPart.getEditorSite().getShell(), Messages.getString("org.isistan.flabot.edit.ImageSaveUtil.dialogName"), Messages.getString("org.isistan.flabot.edit.ImageSaveUtil.dialogDescription") + ex);
            return false;
        }
        return true;
    }

    public static boolean save(IEditorPart editorPart, GraphicalViewer viewer) {
        Assert.isNotNull(editorPart, "null editorPart passed to ImageSaveUtil::save");
        Assert.isNotNull(viewer, "null viewer passed to ImageSaveUtil::save");
        String saveFilePath = getSaveFilePath(editorPart, viewer, -1);
        if (saveFilePath == null) return false;
        int format = SWT.IMAGE_JPEG;
        if (saveFilePath.endsWith(".jpeg")) format = SWT.IMAGE_JPEG; else if (saveFilePath.endsWith(".bmp")) format = SWT.IMAGE_BMP; else if (saveFilePath.endsWith(".ico")) format = SWT.IMAGE_ICO;
        return save(editorPart, viewer, saveFilePath, format);
    }

    private static String getSaveFilePath(IEditorPart editorPart, GraphicalViewer viewer, int format) {
        FileDialog fileDialog = new FileDialog(editorPart.getEditorSite().getShell(), SWT.SAVE);
        String[] filterExtensions = new String[] { Messages.getString("org.isistan.flabot.util.ImageSaveUtil.jpeg"), Messages.getString("org.isistan.flabot.util.ImageSaveUtil.bmp"), Messages.getString("org.isistan.flabot.util.ImageSaveUtil.ico") };
        if (format == SWT.IMAGE_BMP) filterExtensions = new String[] { Messages.getString("org.isistan.flabot.util.ImageSaveUtil.bmp") }; else if (format == SWT.IMAGE_JPEG) filterExtensions = new String[] { Messages.getString("org.isistan.flabot.util.ImageSaveUtil.jpeg") }; else if (format == SWT.IMAGE_ICO) filterExtensions = new String[] { Messages.getString("org.isistan.flabot.util.ImageSaveUtil.ico") };
        fileDialog.setFilterExtensions(filterExtensions);
        return fileDialog.open();
    }

    @SuppressWarnings("deprecation")
    private static void saveEditorContentsAsImage(IEditorPart editorPart, GraphicalViewer viewer, String saveFilePath, int format) {
        FigureCanvas figureCanvas = (FigureCanvas) viewer.getControl();
        IFigure rootFigure = figureCanvas.getContents();
        Rectangle rootFigureBounds = rootFigure.getBounds();
        GC figureCanvasGC = new GC(figureCanvas);
        Image img = new Image(null, rootFigureBounds.width, rootFigureBounds.height);
        GC imageGC = new GC(img);
        imageGC.setTransform(new Transform(null, 1, 0, 0, 1, -rootFigureBounds.x, -rootFigureBounds.y));
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

    /**
	 * Print
	 * 
	 * @param editorPart
	 * @param viewer
	 */
    @SuppressWarnings("deprecation")
    public static void print(IEditorPart editorPart, GraphicalViewer viewer, String printJobName) {
        PrintDialog dialog = new PrintDialog(editorPart.getSite().getShell());
        PrinterData printerData = dialog.open();
        if (printerData != null) {
            Printer printer = new Printer(printerData);
            if (printer.startJob(printJobName)) {
                GC printerGC = new GC(printer);
                if (printer.startPage()) {
                    org.eclipse.swt.graphics.Rectangle printableBounds = printer.getClientArea();
                    printerGC.drawString(printJobName, 100, printableBounds.height - 100);
                    FigureCanvas figureCanvas = (FigureCanvas) viewer.getControl();
                    IFigure rootFigure = figureCanvas.getContents();
                    Rectangle rootFigureBounds = rootFigure.getBounds();
                    float scaleX = (float) printableBounds.width / rootFigureBounds.width;
                    float scaleY = (float) printableBounds.height / rootFigureBounds.height;
                    float scale = Math.min(scaleX, scaleY);
                    Point screenDPI = figureCanvas.getDisplay().getDPI();
                    Point printerDPI = printer.getDPI();
                    float scaleFactor = (float) printerDPI.x / screenDPI.x;
                    org.eclipse.swt.graphics.Rectangle trim = printer.computeTrim(0, 0, 0, 0);
                    scale /= scaleFactor;
                    scale *= 0.8f;
                    GC figureCanvasGC = new GC(figureCanvas);
                    float dx = -rootFigureBounds.x;
                    dx *= scale;
                    dx -= trim.x;
                    dx += 0.1f * trim.width;
                    float dy = -rootFigureBounds.x;
                    dy *= scale;
                    dy -= trim.y;
                    dy += 0.1f * trim.height;
                    printerGC.setTransform(new Transform(null, scale, 0, 0, scale, dx, dy));
                    printerGC.setBackground(figureCanvasGC.getBackground());
                    printerGC.setForeground(figureCanvasGC.getForeground());
                    printerGC.setFont(figureCanvasGC.getFont());
                    printerGC.setLineStyle(figureCanvasGC.getLineStyle());
                    printerGC.setLineWidth(figureCanvasGC.getLineWidth());
                    printerGC.setXORMode(figureCanvasGC.getXORMode());
                    Graphics printerGraphics = new SWTGraphics(printerGC);
                    rootFigure.paint(printerGraphics);
                    printer.endPage();
                    figureCanvasGC.dispose();
                    printerGraphics.dispose();
                }
                printerGC.dispose();
                printer.endJob();
            }
            printer.dispose();
        }
    }
}
