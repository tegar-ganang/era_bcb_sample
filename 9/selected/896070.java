package net.sf.freenote.action;

import java.io.File;
import net.sf.freenote.FreeNoteConstants;
import net.sf.freenote.ShapesEditor;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * 将模型导出为图片
 * @author levin
 * @since 2008-1-27 下午12:57:23
 */
public class ExportShapeAction extends Action implements IWorkbenchWindowActionDelegate {

    private IWorkbenchWindow window;

    public ExportShapeAction() {
        super();
    }

    public ExportShapeAction(IWorkbenchWindow window) {
        super();
        this.window = window;
        setId(FreeNoteConstants.EXPORT_ACTION);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void init(IWorkbenchWindow window) {
        this.window = window;
    }

    @Override
    public void run() {
        if (getGraphicalViewer() == null) return;
        try {
            String fname = openFileDialog();
            if (fname != null && (!new File(fname).exists() || MessageDialog.openConfirm(window.getShell(), "文件覆盖确认", "文件已经存在，是否覆盖?"))) {
                exportImage(fname);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(IAction action) {
        this.run();
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    }

    public void exportImage(String fileName) {
        Device device = getGraphicalViewer().getControl().getDisplay();
        LayerManager lm = (LayerManager) getGraphicalViewer().getEditPartRegistry().get(LayerManager.ID);
        IFigure figure = lm.getLayer(LayerConstants.PRINTABLE_LAYERS);
        Rectangle r = figure.getClientArea();
        Image image = null;
        GC gc = null;
        Graphics g = null;
        try {
            image = new Image(device, r.width, r.height);
            gc = new GC(image);
            g = new SWTGraphics(gc);
            g.translate(r.x * -1, r.y * -1);
            figure.paint(g);
            ImageLoader imageLoader = new ImageLoader();
            imageLoader.data = new ImageData[] { image.getImageData() };
            imageLoader.save(fileName, getImageFormat(fileName));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (g != null) {
                g.dispose();
            }
            if (gc != null) {
                gc.dispose();
            }
            if (image != null) {
                image.dispose();
            }
        }
    }

    private int getImageFormat(String fileName) {
        if (fileName.endsWith(".bmp")) return SWT.IMAGE_BMP_RLE; else if (fileName.endsWith(".gif")) return SWT.IMAGE_GIF; else if (fileName.endsWith(".jpg")) return SWT.IMAGE_JPEG; else if (fileName.endsWith(".png")) return SWT.IMAGE_PNG;
        return SWT.IMAGE_BMP;
    }

    private String openFileDialog() {
        FileDialog fd = new FileDialog(window.getShell(), SWT.SAVE);
        fd.setText("请输入一个文件名");
        fd.setFilterExtensions(new String[] { "*.png", "*.gif", "*.jpg", "*.bmp" });
        return fd.open();
    }

    private GraphicalViewer getGraphicalViewer() {
        try {
            ShapesEditor se = (ShapesEditor) window.getActivePage().getActiveEditor();
            return se.getGraphicalViewer();
        } catch (NullPointerException npe) {
            return null;
        }
    }
}
