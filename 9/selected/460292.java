package com.byterefinery.rmbench.operations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import com.byterefinery.rmbench.RMBenchConstants;
import com.byterefinery.rmbench.RMBenchPlugin;
import com.byterefinery.rmbench.editparts.TableEditPart;
import com.byterefinery.rmbench.editparts.TableStubEditPart;
import com.byterefinery.rmbench.model.diagram.DTable;
import com.byterefinery.rmbench.model.diagram.DTableStub;
import com.byterefinery.rmbench.model.schema.Table;
import com.byterefinery.rmbench.util.ImageConstants;

/**
 * an undoable operation that will import tables into a diagram that are represented by 
 * a table stub (i.e., tables that reference or are referenced by a table from the diagram)
 * 
 * @author Hannes Niederhausen
 */
public class AddStubbedTablesOperation extends RMBenchOperation {

    private static final class TableStubSelectionDialog extends ListSelectionDialog {

        public TableStubSelectionDialog(Shell parentShell, Object input, IStructuredContentProvider contentProvider, ILabelProvider labelProvider, String message) {
            super(parentShell, input, contentProvider, labelProvider, message);
        }

        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, RMBenchConstants.HelpContexts.TableStubSelectionDialog);
            setHelpAvailable(true);
        }
    }

    private DTableStub dTableStub;

    private GraphicalViewer viewer;

    private List<DTable> dTableList;

    public AddStubbedTablesOperation(DTableStub dTableStub, GraphicalViewer viewer) {
        super(Messages.Operation_AddStubbedTables);
        this.dTableStub = dTableStub;
        this.viewer = viewer;
    }

    public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        dTableList = new ArrayList<DTable>();
        for (Iterator<Table> it = dTableStub.foreignKeyTables(); it.hasNext(); ) {
            dTableList.add(new DTable(it.next(), new Point(10, 10)));
        }
        for (Iterator<Table> it = dTableStub.referenceTables(); it.hasNext(); ) {
            dTableList.add(new DTable(it.next(), new Point(10, 10)));
        }
        for (Iterator<Table> it = dTableStub.referenceAndForeignKeyTables(); it.hasNext(); ) {
            dTableList.add(new DTable(it.next(), new Point(10, 10)));
        }
        TableStubSelectionDialog dlg = new TableStubSelectionDialog(viewer.getControl().getShell(), dTableList, new ArrayContentProvider(), new TableListLabelProvider(dTableStub), Messages.Operation_AddStubbedTables_SelectTablesMessage);
        dlg.setTitle(Messages.Operation_AddStubbedTables_SelectTablesTitle);
        if (dlg.open() != Dialog.OK) return Status.CANCEL_STATUS;
        dTableList.clear();
        Object[] tables = dlg.getResult();
        for (int i = 0; i < tables.length; i++) {
            dTableList.add((DTable) tables[i]);
        }
        for (Iterator<DTable> it = dTableList.iterator(); it.hasNext(); ) {
            dTableStub.getDTable().getDiagram().addTable(it.next());
        }
        layoutTables();
        return Status.OK_STATUS;
    }

    public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        for (Iterator<DTable> it = dTableList.iterator(); it.hasNext(); ) {
            dTableStub.getDTable().getDiagram().addTable(it.next());
        }
        return Status.OK_STATUS;
    }

    public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        for (Iterator<DTable> it = dTableList.iterator(); it.hasNext(); ) {
            dTableStub.getDTable().getDiagram().removeTable(it.next());
        }
        return Status.OK_STATUS;
    }

    private void layoutTables() {
        Map<?, ?> registry = viewer.getEditPartRegistry();
        TableEditPart stubTableEp = (TableEditPart) registry.get(dTableStub.getDTable());
        Point loc = new Point(stubTableEp.getFigure().getBounds().getLocation().x, stubTableEp.getFigure().getBounds().getLocation().y - 5);
        List<TableEditPart> editPartList = new ArrayList<TableEditPart>(dTableList.size());
        int height = 0;
        for (Iterator<DTable> it = dTableList.iterator(); it.hasNext(); ) {
            DTable dTable = (DTable) it.next();
            TableEditPart tablePart = (TableEditPart) registry.get(dTable);
            editPartList.add(tablePart);
            tablePart.getFigure().setSize(tablePart.getFigure().getPreferredSize());
            if (height < tablePart.getFigure().getBounds().height) {
                height = tablePart.getFigure().getBounds().height;
            }
        }
        loc.y -= height + 5;
        for (Iterator<TableEditPart> it = editPartList.iterator(); it.hasNext(); ) {
            TableEditPart tablePart = it.next();
            IFigure figure = tablePart.getFigure();
            tablePart.getDTable().setLocation(loc);
            loc = loc.getCopy();
            loc.x += figure.getBounds().width + 5;
            DTableStub stub = tablePart.getDTable().getTableStub();
            TableStubEditPart tableStubPart = (TableStubEditPart) viewer.getEditPartRegistry().get(stub);
            if (tableStubPart != null) tableStubPart.refresh();
            tablePart.refresh();
        }
        viewer.setSelection(new StructuredSelection(editPartList));
    }

    private static final class TableListLabelProvider extends LabelProvider {

        DTableStub stub;

        public TableListLabelProvider(DTableStub stub) {
            super();
            this.stub = stub;
        }

        public Image getImage(Object element) {
            if (stub.hasReferenceAndForeignKeyTable(((DTable) element).getTable())) return RMBenchPlugin.getImage(ImageConstants.LEFT_RIGHT_ARROW); else if (stub.hasReferenceTable(((DTable) element).getTable())) return RMBenchPlugin.getImage(ImageConstants.RIGHT_ARROW);
            return RMBenchPlugin.getImage(ImageConstants.LEFT_ARROW);
        }

        public String getText(Object element) {
            return ((DTable) element).getTable().getFullName();
        }
    }
}
