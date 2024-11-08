package org.plazmaforge.studio.reportdesigner.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.SnapToGeometry;
import org.plazmaforge.studio.reportdesigner.ReportDesignerConstants;
import org.plazmaforge.studio.reportdesigner.editor.ReportEditor;

/** 
 * @author Oleh Hapon
 * $Id: ToggleSnapToGridAction.java,v 1.4 2010/11/10 07:06:54 ohapon Exp $
 */
public class ToggleSnapToGridAction extends AbstractPreferenceAction {

    public static final String ID = "org.plazmaforge.studio.reportdesigner.snap_to_grid";

    public static final String SNAPTOGRID;

    static {
        SNAPTOGRID = "Snap To Grid";
    }

    public ToggleSnapToGridAction(ReportEditor erdesignereditor, GraphicalViewer graphicalviewer) {
        super(SNAPTOGRID, 2, erdesignereditor, graphicalviewer);
        setToolTipText(SNAPTOGRID);
        setId(ID);
        setActionDefinitionId(ID);
        setChecked(isChecked());
    }

    public boolean isChecked() {
        return getViewerProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED);
    }

    public void run() {
        boolean flag = !isChecked();
        setViewerProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, flag);
        setPreferenceValue(ReportDesignerConstants.SNAP_TO_GRID_PREF, flag);
    }
}
