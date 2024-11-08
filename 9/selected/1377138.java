package ieditor.actions;

import ieditor.model.PageDiagram;
import java.util.List;
import java.util.TreeMap;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.ui.actions.SelectionAction;
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
public class MoveToFrontAction extends SelectionAction {

    public static String MOVETOFRONT = "Send to front";

    private IEditorPart editor;

    public MoveToFrontAction(IEditorPart editor) {
        super(editor);
        this.editor = editor;
        this.setText("Send to front");
        setId(MOVETOFRONT);
    }

    public void setEditorPart(IEditorPart editor) {
        this.editor = editor;
    }

    public void run() {
        Command result = null;
        List selection = getSelectedObjects();
        if (selection != null) {
            Object obj = selection.get(0);
            if (obj instanceof GraphicalEditPart) {
                GraphicalEditPart gep = (GraphicalEditPart) obj;
                ChangeBoundsRequest request = new ChangeBoundsRequest();
                TreeMap<String, Integer> map = new TreeMap<String, Integer>();
                GraphicalViewer shape = (GraphicalViewer) this.editor.getAdapter(GraphicalViewer.class);
                System.out.println(shape.getContents().getClass());
                PageDiagram root = (PageDiagram) (shape.getContents().getModel());
                System.out.println(root.getChildren().size() - 1);
                map.put("zIndex", root.getChildren().size() - 1);
                request.setExtendedData(map);
                request.setEditParts(selection);
                request.setType(RequestConstants.REQ_MOVE_CHILDREN);
                result = gep.getCommand(request);
                execute(result);
            }
        }
    }

    protected boolean calculateEnabled() {
        return true;
    }
}
