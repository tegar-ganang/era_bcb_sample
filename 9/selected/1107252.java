package com.byterefinery.rmbench.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPart;
import com.byterefinery.rmbench.RMBenchPlugin;
import com.byterefinery.rmbench.dialogs.TargetTableChooser;
import com.byterefinery.rmbench.editparts.ColumnEditPart;
import com.byterefinery.rmbench.editparts.DiagramEditPart;
import com.byterefinery.rmbench.editparts.TableEditPart;
import com.byterefinery.rmbench.model.Model;
import com.byterefinery.rmbench.model.diagram.Diagram;
import com.byterefinery.rmbench.model.schema.Column;
import com.byterefinery.rmbench.model.schema.Table;
import com.byterefinery.rmbench.operations.AddForeignKeyOperation;
import com.byterefinery.rmbench.operations.AddToDiagramOperation;
import com.byterefinery.rmbench.operations.CompoundOperation;
import com.byterefinery.rmbench.util.ImageConstants;

/**
 * an action that will create a foreign key based on the currently selected columns
 * in a diagram
 * 
 * @author cse
 */
public class ForeignKeyAction extends SelectionAction {

    public static final String ACTION_ID = "com.byterefinery.rmbench.create_foreignkey";

    private final GraphicalViewer viewer;

    private final Diagram diagram;

    private final Model model;

    private Column[] selectedColumnGroup;

    private Table groupTable;

    public ForeignKeyAction(IWorkbenchPart part, Diagram diagram, GraphicalViewer viewer) {
        super(part);
        setId(ACTION_ID);
        setImageDescriptor(RMBenchPlugin.getImageDescriptor(ImageConstants.FK_OUT));
        setText(Messages.ForeignKey_Label);
        setToolTipText(Messages.ForeignKey_Label);
        setDescription(Messages.ForeignKey_Description);
        this.viewer = viewer;
        this.diagram = diagram;
        this.model = diagram.getModel();
    }

    protected boolean calculateEnabled() {
        selectedColumnGroup = null;
        groupTable = null;
        ISelection sel = (ISelection) getSelection();
        if (!(sel instanceof IStructuredSelection)) return false;
        IStructuredSelection selection = (IStructuredSelection) sel;
        List<Column> columnGroup = new ArrayList<Column>(selection.size());
        for (Iterator<?> it = selection.iterator(); it.hasNext(); ) {
            Object selected = it.next();
            if (!(selected instanceof ColumnEditPart)) {
                return false;
            } else {
                Column column = ((ColumnEditPart) selected).getColumn();
                if (groupTable == null) groupTable = column.getTable(); else if (groupTable != column.getTable()) {
                    return false;
                }
                columnGroup.add(column);
            }
        }
        selectedColumnGroup = (Column[]) columnGroup.toArray(new Column[columnGroup.size()]);
        return true;
    }

    public void run() {
        List<Table> matchingTables = model.findMatchingTables(selectedColumnGroup);
        if (matchingTables.isEmpty()) {
            MessageDialog.openInformation(getWorkbenchPart().getSite().getShell(), null, Messages.No_PrimaryKey_matches);
        } else {
            TargetTableChooser chooser = new TargetTableChooser(getWorkbenchPart().getSite().getShell(), matchingTables, diagram);
            if (chooser.open() == Window.OK) {
                AddForeignKeyOperation addFKOp = new AddForeignKeyOperation(groupTable);
                addFKOp.setTargetTable(chooser.getResultTable());
                addFKOp.setColumns(selectedColumnGroup);
                if (chooser.getDoImport()) {
                    TableEditPart tablePart = (TableEditPart) viewer.getEditPartRegistry().get(groupTable);
                    DiagramEditPart diagramPart = (DiagramEditPart) tablePart.getParent();
                    Point newLocation = tablePart.getLocation();
                    newLocation.translate(tablePart.getFigure().getSize().scale(1.5));
                    AddToDiagramOperation addTableOp = new AddToDiagramOperation(diagramPart, new Object[] { chooser.getResultTable() }, newLocation);
                    CompoundOperation compound = new CompoundOperation(addFKOp);
                    compound.addFirst(addTableOp);
                    compound.execute(this);
                } else {
                    addFKOp.execute(this);
                }
            }
        }
    }
}
