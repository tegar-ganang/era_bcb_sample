package org.isistan.flabot.edit.editor.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.Action;
import org.isistan.flabot.messages.Messages;

/**
 * @author $Author: franco $
 *
 */
public class EditorSnapGeometryAction extends Action {

    private GraphicalViewer diagramViewer;

    public EditorSnapGeometryAction(GraphicalViewer diagramViewer) {
        super(Messages.getString("org.isistan.flabot.edit.editor.actions.EditorSnapGeometryAction.text"), AS_CHECK_BOX);
        this.diagramViewer = diagramViewer;
        setToolTipText(Messages.getString("org.isistan.flabot.edit.editor.actions.EditorSnapGeometryAction.toolTipText"));
        setId(GEFActionConstants.TOGGLE_SNAP_TO_GEOMETRY);
        setChecked((Boolean) diagramViewer.getProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED));
    }

    public void run() {
        Boolean val = (Boolean) diagramViewer.getProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED);
        setChecked(!val);
        diagramViewer.setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, new Boolean(!val));
    }
}
