package org.plazmaforge.studio.dbdesigner.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;

public class OrthogonalLayoutAction extends GridLayoutAction {

    public OrthogonalLayoutAction(IWorkbenchPart iworkbenchpart) {
        super(iworkbenchpart);
        setId(ORTHOGONAL_LAYOUT);
        layoutMode = 1;
    }

    public void run() {
        if (MessageDialog.openQuestion(null, "Warning", "Relayout message")) {
            createLayoutCommand().execute();
            editor.getGraphicalViewer().setSelection(new StructuredSelection());
            editor.flushStack();
            editor.makeDirty();
        }
    }

    public static final String ORTHOGONAL_LAYOUT;

    static {
        ORTHOGONAL_LAYOUT = "Auto layout expanded";
    }
}
