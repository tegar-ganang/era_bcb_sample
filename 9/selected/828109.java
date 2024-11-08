package uk.ac.bolton.archimate.editor.diagram.actions;

import java.io.File;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import uk.ac.bolton.archimate.editor.diagram.util.DiagramUtils;

/**
 * Exmport As Image Action
 * 
 * @author Phillip Beauvoir
 */
public class ExportAsImageAction extends Action {

    public static final String ID = "ExportAsImageAction";

    public static final String TEXT = Messages.ExportAsImageAction_0;

    private GraphicalViewer fDiagramViewer;

    public ExportAsImageAction(GraphicalViewer diagramViewer) {
        super(TEXT);
        fDiagramViewer = diagramViewer;
        setId(ID);
    }

    @Override
    public void run() {
        String file = askSaveFile();
        if (file == null) {
            return;
        }
        Image image = null;
        try {
            image = DiagramUtils.createImage(fDiagramViewer);
            ImageLoader loader = new ImageLoader();
            loader.data = new ImageData[] { image.getImageData() };
            if (file.endsWith(".bmp")) {
                loader.save(file, SWT.IMAGE_BMP);
            } else if (file.endsWith(".jpg") || file.endsWith(".jpeg")) {
                loader.save(file, SWT.IMAGE_JPEG);
            } else if (file.endsWith(".png")) {
                loader.save(file, SWT.IMAGE_PNG);
            } else {
                file = file + ".png";
                loader.save(file, SWT.IMAGE_PNG);
            }
        } catch (Exception ex) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.ExportAsImageAction_1, ex.getMessage());
        } finally {
            if (image != null) {
                image.dispose();
            }
        }
    }

    /**
     * Ask user for file name to save to
     * @return
     */
    private String askSaveFile() {
        FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
        dialog.setText(Messages.ExportAsImageAction_2);
        dialog.setFilterExtensions(new String[] { "*.png", "*.jpg;*.jpeg", "*.bmp" });
        String path = dialog.open();
        if (path == null) {
            return null;
        }
        switch(dialog.getFilterIndex()) {
            case 0:
                if (!path.endsWith(".png")) {
                    path += ".png";
                }
                break;
            case 1:
                if (!path.endsWith(".jpg") && !path.endsWith(".jpeg")) {
                    path += ".jpg";
                }
                break;
            case 2:
                if (!path.endsWith(".bmp")) {
                    path += ".bmp";
                }
                break;
            default:
                break;
        }
        File file = new File(path);
        if (file.exists()) {
            boolean result = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), Messages.ExportAsImageAction_1, NLS.bind(Messages.ExportAsImageAction_3, file));
            if (!result) {
                return null;
            }
        }
        return path;
    }
}
