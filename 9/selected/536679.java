package org.plazmaforge.studio.dbdesigner.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.action.Action;
import org.plazmaforge.studio.dbdesigner.editor.ERDesignerEditor;

public class ToggleSnapToGridAction extends Action {

    public ToggleSnapToGridAction(ERDesignerEditor erdesignereditor, GraphicalViewer graphicalviewer) {
        super(SNAPTOGRID, 2);
        diagramViewer = graphicalviewer;
        setToolTipText(SNAPTOGRID);
        setId(ID);
        setActionDefinitionId(ID);
        setChecked(isChecked());
        editor = erdesignereditor;
    }

    public boolean isChecked() {
        Boolean boolean1 = (Boolean) diagramViewer.getProperty(SNAPTOGRID);
        if (boolean1 != null) return boolean1.booleanValue(); else return false;
    }

    public void run() {
        diagramViewer.setProperty(SNAPTOGRID, new Boolean(!isChecked()));
        editor.makeDirty();
    }

    public static final String ID = "org.plazmaforge.studio.dbdesigner.snap_to_grid";

    public static final String SNAPTOGRID;

    private GraphicalViewer diagramViewer;

    private ERDesignerEditor editor;

    static {
        SNAPTOGRID = "Snap To Grid";
    }
}
