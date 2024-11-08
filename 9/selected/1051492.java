package org.plazmaforge.studio.dbdesigner.actions;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.plazmaforge.studio.dbdesigner.editor.ERDesignerEditor;
import org.plazmaforge.studio.dbdesigner.model.ERDiagram;
import org.plazmaforge.studio.dbdesigner.model.ERTableNode;
import org.plazmaforge.studio.dbdesigner.parts.ERDiagramEditPart;

public class ShowTablesAction extends Action {

    public static final String SELECT_TABLES = "Show tables";

    private ERDesignerEditor editor;

    public ShowTablesAction(ERDesignerEditor erdesignereditor) {
        editor = erdesignereditor;
        setId(SELECT_TABLES);
        setText(SELECT_TABLES);
    }

    public void run() {
        GraphicalViewer graphicalViewer = editor.getGraphicalViewer();
        ERDiagramEditPart diagramEditPart = (ERDiagramEditPart) graphicalViewer.getContents();
        ERDiagram diagram = (ERDiagram) diagramEditPart.getModel();
        Object[] elements = diagram.getTableNodes().toArray();
        if (elements.length == 0) {
            MessageDialog.openWarning(null, "Warning", "No elements");
            return;
        }
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(null, new LabelProvider() {

            public String getText(Object obj) {
                return ((ERTableNode) obj).getName();
            }
        });
        dialog.setTitle("Select table title");
        dialog.setMessage("Select table message");
        dialog.setElements(elements);
        dialog.open();
        Object aobj[] = dialog.getResult();
        if (aobj == null) {
            return;
        }
        EditPart editPart = (EditPart) graphicalViewer.getEditPartRegistry().get(aobj[0]);
        graphicalViewer.reveal(editPart);
        graphicalViewer.select(editPart);
    }
}
