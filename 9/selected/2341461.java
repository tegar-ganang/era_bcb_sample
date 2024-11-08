package com.byterefinery.rmbench.actions;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import com.byterefinery.rmbench.RMBenchPlugin;
import com.byterefinery.rmbench.dialogs.PrintDialog;
import com.byterefinery.rmbench.operations.RMBenchPrintOperation;

/**
 * a print action that will use our customized print dialog for user interaction
 * 
 * @author sell
 */
public class PrintAction extends WorkbenchPartAction {

    /**
     * @param part The workbench part associated with this PrintAction
     */
    public PrintAction(IWorkbenchPart part) {
        super(part);
    }

    /**
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    protected boolean calculateEnabled() {
        PrinterData[] printers = Printer.getPrinterList();
        return printers != null && printers.length > 0;
    }

    /**
     * @see org.eclipse.gef.ui.actions.EditorPartAction#init()
     */
    protected void init() {
        super.init();
        setText(Messages.Print_Label);
        setToolTipText(Messages.Print_Tooltip);
        setId(ActionFactory.PRINT.getId());
    }

    /**
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        GraphicalViewer viewer;
        viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(GraphicalViewer.class);
        LayerManager lm = (LayerManager) viewer.getEditPartRegistry().get(LayerManager.ID);
        IFigure layer = lm.getLayer(LayerConstants.PRINTABLE_LAYERS);
        PrintDialog dialog = new PrintDialog(viewer.getControl().getShell(), layer, true);
        dialog.open();
        PrinterData data = dialog.getPrinter();
        if (data != null) {
            RMBenchPrintOperation op = new RMBenchPrintOperation(new Printer(data), dialog.getPages(), viewer);
            double margin = RMBenchPlugin.getPrintState().margin;
            op.setMargin((int) (margin * Display.getCurrent().getDPI().x));
            op.setPrintMode(dialog.getPrintMode());
            op.run(getWorkbenchPart().getTitle());
        }
    }
}
