package org.plazmaforge.studio.dbdesigner.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.plazmaforge.framework.client.swt.dialogs.FormatSelectionDialog;
import org.plazmaforge.framework.core.FileFormat;
import org.plazmaforge.studio.core.util.FileUtils;

public class ExportImageAction extends AbstractSelectionAction {

    public static final int WinBMP_TYPE = 1;

    public static final int GIF_TYPE = 2;

    public static final int WinICO_TYPE = 3;

    public static final int JPEG_TYPE = 4;

    public static final int PNG_TYPE = 5;

    public static final int TIFF_TYPE = 6;

    public static final int OS2BMP_TYPE = 7;

    public ExportImageAction(IWorkbenchPart iworkbenchpart) {
        super(iworkbenchpart);
        setText("Export to image");
    }

    protected boolean calculateEnabled() {
        return true;
    }

    protected FileFormat getImageFormat() {
        java.util.List<FileFormat> formats;
        formats = new ArrayList<FileFormat>();
        formats.add(new FileFormat("PNG", "PNG", "PNG format", new String[] { "*.png" }, PNG_TYPE));
        formats.add(new FileFormat("JPEG", "JPEG", "JPEG format", new String[] { "*.jpg", "*.jpeg" }, JPEG_TYPE));
        formats.add(new FileFormat("WinBMP", "BMP", "WinBMP format", new String[] { "*.bmp" }, WinBMP_TYPE));
        FormatSelectionDialog dialog = new FormatSelectionDialog(getShell());
        dialog.setElements(formats.toArray(new FileFormat[0]));
        dialog.open();
        Object result[] = dialog.getResult();
        if (result == null || result.length == 0) {
            return null;
        }
        return (FileFormat) result[0];
    }

    public void run() {
        if (!hasDiagramElements()) {
            openMessageNoElements();
            return;
        }
        GraphicalViewer graphicalViewer = getDesignerEditor().getGraphicalViewer();
        final FileFormat imageFormat = getImageFormat();
        if (imageFormat == null) {
            return;
        }
        final String fileName = FileUtils.getInputFileName(getShell(), imageFormat.getFileExtensions(), SWT.SAVE, true);
        if (fileName == null) {
            return;
        }
        LayerManager layerManager = (LayerManager) graphicalViewer.getEditPartRegistry().get(LayerManager.ID);
        final IFigure diagramFigure = layerManager.getLayer("Printable Layers");
        Dimension dimension = diagramFigure.getSize();
        Display display = graphicalViewer.getControl().getDisplay();
        Image image = null;
        try {
            image = new Image(display, dimension.width, dimension.height);
        } catch (SWTException _ex) {
            MessageDialog.openError(null, "Error", "Error exporting image");
            return;
        }
        final Image exportImage = image;
        ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(getShell());
        try {
            monitorDialog.run(true, false, new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Exporting...", -1);
                    try {
                        exportToImage(exportImage, diagramFigure, imageFormat, fileName);
                    } catch (Exception ex) {
                        throw new InvocationTargetException(ex);
                    }
                }
            });
        } catch (Throwable ex) {
            handleProcessError(ex);
        }
    }

    protected void exportToImage(Image exportImage, IFigure diagramFigure, FileFormat imageFormat, String fileName) {
        Dimension dimension = diagramFigure.getSize();
        GC gc = new GC(exportImage);
        SWTGraphics swtgraphics = new SWTGraphics(gc);
        Rectangle rectangle = diagramFigure.getBounds();
        int j = rectangle.x >= 0 ? 0 : -rectangle.x;
        int k = rectangle.y >= 0 ? 0 : -rectangle.y;
        swtgraphics.translate(j, k);
        diagramFigure.paint(swtgraphics);
        swtgraphics.dispose();
        gc.dispose();
        ImageLoader imageloader = new ImageLoader();
        imageloader.logicalScreenWidth = dimension.width;
        imageloader.logicalScreenHeight = dimension.height;
        imageloader.data = (new ImageData[] { exportImage.getImageData() });
        imageloader.save(fileName, imageFormat.getType());
        exportImage.dispose();
    }
}
