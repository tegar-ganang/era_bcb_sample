package org.plazmaforge.studio.reportdesigner.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.internal.GEFMessages;
import org.plazmaforge.studio.reportdesigner.ReportDesignerConstants;
import org.plazmaforge.studio.reportdesigner.editor.ReportEditor;

/** 
 * @author Oleh Hapon
 * $Id: ToggleGridVisibilityAction.java,v 1.3 2010/11/10 07:06:54 ohapon Exp $
 */
public class ToggleGridVisibilityAction extends AbstractPreferenceAction {

    public ToggleGridVisibilityAction(ReportEditor reportEditor, GraphicalViewer viewer) {
        super(GEFMessages.ToggleGrid_Label, 2, reportEditor, viewer);
        setToolTipText(GEFMessages.ToggleGrid_Tooltip);
        setId("org.eclipse.gef.toggle_grid_visibility");
        setActionDefinitionId("org.eclipse.gef.toggle_grid_visibility");
        setChecked(isChecked());
    }

    public boolean isChecked() {
        return getViewerProperty(SnapToGrid.PROPERTY_GRID_ENABLED);
    }

    public void run() {
        boolean flag = !isChecked();
        setViewerProperty(SnapToGrid.PROPERTY_GRID_ENABLED, flag);
        setPreferenceValue(ReportDesignerConstants.GRID_VISIBLE_PREF, flag);
    }
}
