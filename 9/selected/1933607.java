package org.xaware.ide.xadev.gui.editor;

import org.eclipse.draw2d.PrintFigureOperation;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.print.PrintGraphicalViewerOperation;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.ui.IWorkbenchPart;
import org.xaware.ide.xadev.processview.ProcessViewEditor;

/**
 * Class to print activity diagram from the graphical viewer.
 * 
 * @author abhatt
 * 
 */
public class XAPrintAction extends PrintAction {

    public XAPrintAction(final IWorkbenchPart part) {
        super(part);
    }

    /**
     * Print Activity Diagram page from the graphical viewer
     */
    @Override
    public void run() {
        GraphicalViewer viewer;
        viewer = ((ProcessViewEditor) getWorkbenchPart()).getViewer();
        final PrintDialog dialog = new PrintDialog(viewer.getControl().getShell(), SWT.NULL);
        final PrinterData data = dialog.open();
        if (data != null) {
            final PrintGraphicalViewerOperation op = new PrintGraphicalViewerOperation(new Printer(data), viewer);
            op.setPrintMode(PrintFigureOperation.FIT_PAGE);
            op.run(getWorkbenchPart().getTitle());
        }
    }
}
