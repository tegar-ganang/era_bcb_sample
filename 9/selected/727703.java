package org.plazmaforge.studio.dbdesigner.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.plazmaforge.framework.util.StringUtils;
import org.plazmaforge.studio.core.util.ErrorUtils;
import org.plazmaforge.studio.core.util.PlatformUtils;
import org.plazmaforge.studio.dbdesigner.editor.ERDesignerEditor;
import org.plazmaforge.studio.dbdesigner.model.ERDiagram;
import org.plazmaforge.studio.dbdesigner.parts.ERDiagramEditPart;

public abstract class AbstractSelectionAction extends SelectionAction {

    public AbstractSelectionAction(IWorkbenchPart part) {
        super(part);
    }

    protected Shell getShell() {
        return getWorkbenchPart().getSite().getShell();
    }

    protected ERDesignerEditor getDesignerEditor() {
        return (ERDesignerEditor) getWorkbenchPart();
    }

    protected ERDiagram getDiagram() {
        GraphicalViewer graphicalViewer = getDesignerEditor().getGraphicalViewer();
        ERDiagramEditPart diagramEditPart = (ERDiagramEditPart) graphicalViewer.getContents();
        return (ERDiagram) diagramEditPart.getModel();
    }

    protected boolean hasDiagramElements() {
        return getDiagram().hasChildren();
    }

    protected void openMessageNoElements() {
        MessageDialog.openWarning(getShell(), "Warning", "No elements");
    }

    protected boolean isEmpty(String str) {
        return StringUtils.isEmpty(str);
    }

    protected void handleProcessError(Throwable throwable) {
        String message = ErrorUtils.getMessage(throwable);
        message = PlatformUtils.isNullOrEmpty(message) ? "" : message;
        MessageDialog.openError(null, "Error", message);
    }
}
