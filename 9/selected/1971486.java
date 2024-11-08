package com.ivis.xprocess.ui.viewpoints.editor;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.print.PrintGraphicalViewerOperation;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import com.ivis.xprocess.ui.UIType;
import com.ivis.xprocess.ui.properties.ProcessDesignerMessages;

public class PrintAction extends WorkbenchPartAction {

    public PrintAction(IWorkbenchPart part) {
        super(part);
        setToolTipText(ProcessDesignerMessages.PrintAction_toolTip);
        setImageDescriptor(UIType.print_diagram.getImageDescriptor());
    }

    @Override
    protected boolean calculateEnabled() {
        return Printer.getPrinterList().length > 0;
    }

    @Override
    public void run() {
        IWorkbenchPart part = getWorkbenchPart();
        if (part != null) {
            GraphicalViewer viewer = (GraphicalViewer) part.getAdapter(GraphicalViewer.class);
            if (viewer != null) {
                int style = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getStyle();
                Shell shell = new Shell((style & SWT.MIRRORED) != 0 ? SWT.RIGHT_TO_LEFT : SWT.NONE);
                PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
                PrinterData data = dialog.open();
                if (data != null) {
                    PrintGraphicalViewerOperation operation = new PrintGraphicalViewerOperation(new Printer(data), viewer);
                    operation.setPrintMode(2);
                    operation.run("Printing...");
                }
            }
        }
    }
}
