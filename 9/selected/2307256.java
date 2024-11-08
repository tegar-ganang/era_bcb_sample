package com.byterefinery.rmbench.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.SelectionAction;
import com.byterefinery.rmbench.editors.DiagramEditor;
import com.byterefinery.rmbench.operations.LayoutOperation;

/**
 * action for automatically laying out the curretly selected figures, or the 
 * whole diagram if none are selected
 * 
 * @author sell
 */
public class LayoutFiguresAction extends SelectionAction {

    public static final String ACTION_ID = LayoutFiguresAction.class.getName();

    public LayoutFiguresAction(DiagramEditor editor) {
        super(editor, AS_PUSH_BUTTON);
        setId(ACTION_ID);
    }

    public void run() {
        GraphicalViewer viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(GraphicalViewer.class);
        LayoutOperation operation = new LayoutOperation(viewer);
        operation.execute(this);
    }

    protected boolean calculateEnabled() {
        return true;
    }
}
