package net.sf.freenote.action;

import java.util.ArrayList;
import java.util.List;
import net.sf.freenote.FreeNoteConstants;
import net.sf.freenote.parts.ShapeEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;

/**
 * 选择Model类型相同的形体
 * @author levin
 * @since 2008-2-23 下午01:22:43
 */
public class SelectSameAction extends SelectionAction {

    public SelectSameAction(IWorkbenchPart part) {
        super(part);
        setText("Select Same");
        setToolTipText("批量选择类型相同的形体");
        setId(FreeNoteConstants.SELECT_SAME);
    }

    @Override
    protected boolean calculateEnabled() {
        if (getSelectedObjects().size() == 1) return true;
        return false;
    }

    @Override
    public void run() {
        GraphicalViewer viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(GraphicalViewer.class);
        if (viewer != null) {
            Class clazz = ((EditPart) getSelectedObjects().get(0)).getModel().getClass();
            List<AbstractGraphicalEditPart> list = new ArrayList<AbstractGraphicalEditPart>();
            visit((AbstractGraphicalEditPart) viewer.getContents(), list, clazz);
            viewer.setSelection(new StructuredSelection(list));
        }
    }

    private void visit(AbstractGraphicalEditPart parent, List<AbstractGraphicalEditPart> list, Class clazz) {
        for (Object o : parent.getChildren()) {
            visit((AbstractGraphicalEditPart) o, list, clazz);
        }
        for (Object o : parent.getSourceConnections()) {
            visit((AbstractGraphicalEditPart) o, list, clazz);
        }
        for (Object o : parent.getTargetConnections()) {
            visit((AbstractGraphicalEditPart) o, list, clazz);
        }
        if (parent.getModel().getClass() == clazz) list.add(parent);
    }
}
