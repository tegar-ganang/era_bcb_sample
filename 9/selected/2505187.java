package org.plazmaforge.studio.dbdesigner.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;

public class AutoLayoutAction extends GridLayoutAction {

    public AutoLayoutAction(IWorkbenchPart iworkbenchpart) {
        super(iworkbenchpart);
        setId(ORGANIC_LAYOUT);
        layoutMode = 2;
    }

    public void run() {
        if (MessageDialog.openQuestion(null, "Warning", "Auto Layout will erase your layout settings. Proceed ?")) {
            execute(createLayoutCommand());
            editor.getGraphicalViewer().setSelection(new StructuredSelection());
            editor.flushStack();
            editor.makeDirty();
        }
    }

    public static final String ID = "org.plazmaforge.studio.dbdesigner.auto_layout";

    public static final String ORGANIC_LAYOUT;

    static {
        ORGANIC_LAYOUT = "Auto Layout";
    }
}
