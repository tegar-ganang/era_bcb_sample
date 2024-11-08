package com.byterefinery.rmbench.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.ui.IWorkbenchPart;
import com.byterefinery.rmbench.RMBenchPlugin;
import com.byterefinery.rmbench.editparts.TableStubEditPart;
import com.byterefinery.rmbench.model.diagram.DTableStub;
import com.byterefinery.rmbench.operations.AddStubbedTablesOperation;
import com.byterefinery.rmbench.util.ImageConstants;

/**
 * an action that will import stubbed tables into a diagram
 * 
 * @author hannesn
 */
public class AddStubbedTablesAction extends SelectionAction {

    public static final String ACTION_ID = "com.byterefinery.rmbench.action.AddStubbedTablesAction";

    public AddStubbedTablesAction(IWorkbenchPart part) {
        super(part);
        setId(ACTION_ID);
        setImageDescriptor(RMBenchPlugin.getImageDescriptor(ImageConstants.LEFT_RIGHT_ARROW));
        setText(Messages.AddStubbedTablesAction_Text);
    }

    protected boolean calculateEnabled() {
        if ((getSelectedObjects().size() == 1) && (getSelectedObjects().get(0) instanceof TableStubEditPart)) return true;
        return false;
    }

    protected void init() {
        super.init();
        setEnabled(false);
    }

    public void run() {
        TableStubEditPart ep = (TableStubEditPart) getSelectedObjects().get(0);
        GraphicalViewer viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(GraphicalViewer.class);
        AddStubbedTablesOperation op = new AddStubbedTablesOperation((DTableStub) ep.getModel(), viewer);
        op.execute(this);
    }
}
