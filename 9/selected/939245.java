package uk.ac.bolton.archimate.editor.diagram.actions;

import java.util.List;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IWorkbenchPart;
import uk.ac.bolton.archimate.model.IDiagramModelContainer;
import uk.ac.bolton.archimate.model.IDiagramModelObject;
import uk.ac.bolton.archimate.model.ILockable;

/**
 * Bring Forward Action
 * Simply brings the child forward in order by one position
 * 
 * @author Phillip Beauvoir
 */
public class BringForwardAction extends SelectionAction {

    public static final String ID = "BringForwardAction";

    public static final String TEXT = Messages.BringForwardAction_0;

    public BringForwardAction(IWorkbenchPart part) {
        super(part);
        setText(TEXT);
        setId(ID);
        setSelectionProvider((ISelectionProvider) part.getAdapter(GraphicalViewer.class));
    }

    @Override
    protected boolean calculateEnabled() {
        List<?> selected = getSelectedObjects();
        if (selected.isEmpty()) {
            return false;
        }
        for (Object object : selected) {
            if (!(object instanceof EditPart)) {
                return false;
            }
        }
        Command command = createCommand(selected);
        if (command == null) {
            return false;
        }
        return command.canExecute();
    }

    @Override
    public void run() {
        execute(createCommand(getSelectedObjects()));
    }

    private Command createCommand(List<?> selection) {
        GraphicalViewer viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(GraphicalViewer.class);
        CompoundCommand result = new CompoundCommand(Messages.BringForwardAction_0);
        for (Object object : selection) {
            if (object instanceof GraphicalEditPart) {
                GraphicalEditPart editPart = (GraphicalEditPart) object;
                Object model = editPart.getModel();
                if (viewer != editPart.getViewer()) {
                    System.err.println("Wrong selection for viewer in " + getClass());
                }
                if (model instanceof ILockable && ((ILockable) model).isLocked()) {
                    continue;
                }
                if (model instanceof IDiagramModelObject) {
                    IDiagramModelObject diagramObject = (IDiagramModelObject) model;
                    IDiagramModelContainer parent = (IDiagramModelContainer) diagramObject.eContainer();
                    if (parent == null) {
                        continue;
                    }
                    List<IDiagramModelObject> modelChildren = parent.getChildren();
                    int originalPos = modelChildren.indexOf(diagramObject);
                    if (originalPos < modelChildren.size() - 1) {
                        result.add(new BringForwardCommand(parent, originalPos));
                    }
                }
            }
        }
        return result.unwrap();
    }

    private static class BringForwardCommand extends Command {

        private IDiagramModelContainer fParent;

        private int fOldPos;

        public BringForwardCommand(IDiagramModelContainer parent, int oldPos) {
            fParent = parent;
            fOldPos = oldPos;
            setLabel(Messages.BringForwardAction_0);
        }

        @Override
        public boolean canExecute() {
            return fParent != null && fOldPos < fParent.getChildren().size() - 1;
        }

        @Override
        public void execute() {
            fParent.getChildren().move(fOldPos + 1, fOldPos);
        }

        @Override
        public void undo() {
            fParent.getChildren().move(fOldPos, fOldPos + 1);
        }

        @Override
        public void dispose() {
            fParent = null;
        }
    }
}
