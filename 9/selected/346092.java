package org.plazmaforge.studio.reportdesigner.editor;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.plazmaforge.studio.reportdesigner.ReportDesignerConstants;

public class ReportPreferenceChangeListener implements IPropertyChangeListener {

    private ReportEditor editor;

    private IPreferenceStore store;

    public ReportPreferenceChangeListener(ReportEditor editor, IPreferenceStore store) {
        this.editor = editor;
        this.store = store;
    }

    public void propertyChange(PropertyChangeEvent event) {
        String property = event.getProperty();
        if (property.equals(ReportDesignerConstants.GRID_SPACING_PREF)) {
            Integer value = (Integer) event.getNewValue();
            editor.getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(value.intValue(), value.intValue()));
        } else if (property.equals(ReportDesignerConstants.RULERS_VISIBLE_PREF)) {
            Boolean value = (Boolean) event.getNewValue();
            editor.getGraphicalViewer().setProperty(RulerProvider.PROPERTY_RULER_VISIBILITY, value);
        }
    }
}
