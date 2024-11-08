package de.mpiwg.vspace.diagram.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import de.mpiwg.vspace.common.error.UserErrorException;
import de.mpiwg.vspace.common.error.UserErrorService;
import de.mpiwg.vspace.common.project.ProjectManager;
import de.mpiwg.vspace.diagram.util.PropertyHandler;
import de.mpiwg.vspace.extension.ExceptionHandlingService;
import de.mpiwg.vspace.metamodel.Link;
import de.mpiwg.vspace.metamodel.Scene;
import de.mpiwg.vspace.util.swt.MyDialogCellEditor;

public class ImageFileChooserCellEditor extends MyDialogCellEditor {

    private Composite parent;

    private String imageFolderPath;

    public ImageFileChooserCellEditor(Composite parent, EObject object) {
        super(parent);
        this.parent = parent;
        if (object instanceof Scene) imageFolderPath = ProjectManager.getInstance().getImageFolderName(); else if (object instanceof Link) imageFolderPath = ProjectManager.getInstance().getLinkIconFolderName(); else imageFolderPath = ProjectManager.getInstance().getExhibitionModuleImageFolderName();
    }

    protected Object openDialogBox(Control cellEditorWindow) {
        FileDialog dialog = new FileDialog(parent.getShell(), SWT.OPEN);
        dialog.setFilterExtensions(new String[] { "*.jpg;*.JPG;*.JPEG;*.gif;*.GIF;*.png;*.PNG", "*.jpg;*.JPG;*.JPEG", "*.gif;*.GIF", "*.png;*.PNG" });
        dialog.setFilterNames(new String[] { "All", "Joint Photographic Experts Group (JPEG)", "Graphics Interchange Format (GIF)", "Portable Network Graphics (PNG)" });
        String imagePath = dialog.open();
        if (imagePath == null) return null;
        IProject project = ProjectManager.getInstance().getCurrentProject();
        String projectFolderPath = project.getLocation().toOSString();
        File imageFile = new File(imagePath);
        String fileName = imageFile.getName();
        ImageData imageData = null;
        try {
            imageData = new ImageData(imagePath);
        } catch (SWTException e) {
            UserErrorException error = new UserErrorException(PropertyHandler.getInstance().getProperty("_invalid_image_title"), PropertyHandler.getInstance().getProperty("_invalid_image_text"));
            UserErrorService.INSTANCE.showError(error);
            return null;
        }
        if (imageData == null) {
            UserErrorException error = new UserErrorException(PropertyHandler.getInstance().getProperty("_invalid_image_title"), PropertyHandler.getInstance().getProperty("_invalid_image_text"));
            UserErrorService.INSTANCE.showError(error);
            return null;
        }
        File copiedImageFile = new File(projectFolderPath + File.separator + imageFolderPath + File.separator + fileName);
        if (copiedImageFile.exists()) {
            Path path = new Path(copiedImageFile.getPath());
            copiedImageFile = new File(projectFolderPath + File.separator + imageFolderPath + File.separator + UUID.randomUUID().toString() + "." + path.getFileExtension());
        }
        try {
            copiedImageFile.createNewFile();
        } catch (IOException e1) {
            ExceptionHandlingService.INSTANCE.handleException(e1);
            copiedImageFile = null;
        }
        if (copiedImageFile == null) {
            copiedImageFile = new File(projectFolderPath + File.separator + imageFolderPath + File.separator + UUID.randomUUID().toString());
            try {
                copiedImageFile.createNewFile();
            } catch (IOException e) {
                ExceptionHandlingService.INSTANCE.handleException(e);
                return "";
            }
        }
        FileReader in = null;
        FileWriter out = null;
        try {
            in = new FileReader(imageFile);
            out = new FileWriter(copiedImageFile);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            ExceptionHandlingService.INSTANCE.handleException(e);
            return "";
        } catch (IOException e) {
            ExceptionHandlingService.INSTANCE.handleException(e);
            return "";
        }
        return imageFolderPath + File.separator + copiedImageFile.getName();
    }
}
