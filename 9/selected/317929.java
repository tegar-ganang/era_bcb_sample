package uk.ac.bolton.archimate.editor.diagram.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.print.PrintGraphicalViewerOperation;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import uk.ac.bolton.archimate.editor.utils.PlatformUtils;

/**
 * Print Diagram Action
 * 
 * @author Phillip Beauvoir
 */
public class PrintDiagramAction extends WorkbenchPartAction {

    public PrintDiagramAction(IWorkbenchPart part) {
        super(part);
        setId(ActionFactory.PRINT.getId());
    }

    @Override
    protected boolean calculateEnabled() {
        if (PlatformUtils.isLinux()) {
            return true;
        }
        PrinterData[] printers = Printer.getPrinterList();
        return printers != null && printers.length > 0;
    }

    @Override
    public void run() {
        GraphicalViewer viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(GraphicalViewer.class);
        int printMode = new PrintModeDialog(viewer.getControl().getShell()).open();
        if (printMode == -1) {
            return;
        }
        PrintDialog dialog = new PrintDialog(viewer.getControl().getShell(), SWT.NULL);
        PrinterData data = dialog.open();
        if (data != null) {
            PrintGraphicalViewerOperation op = new PrintGraphicalViewerOperation(new Printer(data), viewer);
            op.setPrintMode(printMode);
            op.run(getWorkbenchPart().getTitle());
        }
    }
}
