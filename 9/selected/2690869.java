package ieditor.actions;

import ieditor.AdvancedFactory;
import ieditor.ElementEditor;
import ieditor.model.shape.IImage;
import java.util.TreeMap;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Tool;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.tools.CreationTool;
import org.eclipse.gef.ui.actions.EditorPartAction;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

/**
 * EditPart used for Shape instances (more specific for EllipticalShape and
 * RectangularShape instances).
 * <p>This edit part must implement the PropertyChangeListener interface, 
 * so it can be notified of property changes in the corresponding model element.
 * </p>
 * 
 * @author Elias Volanakis
 */
public class ImageInsertAction extends EditorPartAction {

    public static String ADD_IMAGE = "Test";

    private Tool tool;

    private CreationFactory factory;

    private IEditorPart editor;

    public ImageInsertAction(IEditorPart editor) {
        super(editor);
        setId(ADD_IMAGE);
        System.out.println("Creating new image");
    }

    public void setEditorPart(IEditorPart editor) {
        this.editor = editor;
    }

    public void run() {
        System.out.println("Calling run method on insert action");
        tool = new CreationTool(factory);
        ElementEditor edit = (ElementEditor) this.editor;
        CreateRequest request = new CreateRequest("create child");
        System.out.println("Opening dialog");
        FileDialog dialog = new FileDialog(new Shell());
        dialog.open();
        String filename = dialog.getFileName();
        System.out.println("Got filename");
        TreeMap<String, String> map = new TreeMap<String, String>();
        map.put("filename", filename);
        AdvancedFactory factory = new AdvancedFactory((Class) IImage.class, map);
        request.setFactory(factory);
        System.out.println("Factory finished");
        Rectangle bounds = new Rectangle(-1, -1, 0, 0);
        request.setSize(bounds.getSize());
        request.setLocation(bounds.getLocation());
        System.out.println("Rectangle finished");
        Object shaper = edit.getAdapter(GraphicalViewer.class);
        System.out.println(shaper);
        System.out.println("Rectangle finished");
        System.out.println(shaper.getClass().toString());
        GraphicalViewer shape = (GraphicalViewer) edit.getAdapter(GraphicalViewer.class);
        System.out.println("Getting adapter");
        EditPart root = shape.getContents();
        System.out.println("Getting command");
        Command command = root.getCommand(request);
        System.out.println("Calling execute");
        shape.getEditDomain().getCommandStack().execute(command);
        System.out.println("End insert action");
    }

    protected boolean calculateEnabled() {
        return true;
    }
}
