package com.metanology.mde.ui.pimEditor.diagrams;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.EditorPartAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.ui.IEditorPart;

/**
 * @author wwang
 *
 * @since 3.0
 */
public class DiagramPrintAction extends EditorPartAction {

    /**
	 * Constructor for DiagramPrintAction.
	 * @param editor The EditorPart associated with this PrintAction
	 */
    public DiagramPrintAction(IEditorPart editor) {
        super(editor);
    }

    /**
	 * @see org.eclipse.gef.ui.actions.EditorPartAction#calculateEnabled()
	 */
    protected boolean calculateEnabled() {
        return true;
    }

    /**
	 * @see org.eclipse.gef.ui.actions.EditorPartAction#init()
	 */
    protected void init() {
        setId(GEFActionConstants.PRINT);
    }

    /**
	 * @see org.eclipse.jface.action.Action#run()
	 */
    public void run() {
        GraphicalViewer viewer;
        viewer = (GraphicalViewer) getEditorPart().getAdapter(GraphicalViewer.class);
        PrintDialog dialog = new PrintDialog(viewer.getControl().getShell(), SWT.NULL);
        PrinterData data = dialog.open();
        if (data != null) {
            DiagramPrintOperation op = new DiagramPrintOperation(new Printer(data), viewer);
            op.run(getEditorPart().getTitle());
        }
    }
}
