package org.plazmaforge.studio.dbdesigner.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.jface.action.Action;
import org.plazmaforge.studio.dbdesigner.editor.ERDesignerEditor;

public class ToggleGridVisibilityAction extends Action {

    public ToggleGridVisibilityAction(ERDesignerEditor erdesignereditor, GraphicalViewer graphicalviewer) {
        super(GEFMessages.ToggleGrid_Label, 2);
        diagramViewer = graphicalviewer;
        setToolTipText(GEFMessages.ToggleGrid_Tooltip);
        setId("org.eclipse.gef.toggle_grid_visibility");
        setActionDefinitionId("org.eclipse.gef.toggle_grid_visibility");
        setChecked(isChecked());
        editor = erdesignereditor;
    }

    public boolean isChecked() {
        Boolean boolean1 = (Boolean) diagramViewer.getProperty("SnapToGrid.isVisible");
        if (boolean1 != null) return boolean1.booleanValue(); else return false;
    }

    public void run() {
        boolean flag = !isChecked();
        diagramViewer.setProperty("SnapToGrid.isVisible", new Boolean(flag));
        editor.makeDirty();
    }

    private GraphicalViewer diagramViewer;

    private ERDesignerEditor editor;
}
