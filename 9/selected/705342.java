package org.isistan.flabot.edit.editor.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.isistan.flabot.messages.Messages;

/**
 * This action is used to select all edit parts of a diagram, including edit part's children 
 * 
 * @author $Author: franco $
 *
 */
public class SelectAllAction extends Action {

    private IWorkbenchPart part;

    /**
	 * Creates a new SelectAllAction in the given workbench part
	 * @param part
	 */
    public SelectAllAction(IWorkbenchPart part) {
        this.part = part;
        setText(Messages.getString("org.isistan.flabot.edit.editor.actions.SelectAllAction.text"));
        setToolTipText(Messages.getString("org.isistan.flabot.edit.editor.actions.SelectAllAction.toolTipText"));
        setId(ActionFactory.SELECT_ALL.getId());
    }

    /**
	 * Selects all edit parts in the active workbench part.
	 * Including the children of all edit parts.
	 */
    public void run() {
        GraphicalViewer viewer = (GraphicalViewer) part.getAdapter(GraphicalViewer.class);
        if (viewer != null) {
            Collection c = viewer.getEditPartRegistry().values();
            List selection = new ArrayList();
            for (Iterator iter = c.iterator(); iter.hasNext(); ) {
                EditPart part = (EditPart) iter.next();
                if (part != viewer.getRootEditPart() && !viewer.getRootEditPart().getChildren().contains(part)) selection.add(part);
            }
            viewer.setSelection(new StructuredSelection(selection));
        }
    }
}
