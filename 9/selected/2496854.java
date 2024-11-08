package org.plazmaforge.studio.reportdesigner.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.plazmaforge.studio.reportdesigner.ReportDesignerPlugin;
import org.plazmaforge.studio.reportdesigner.editor.ReportEditor;

public class AbstractPreferenceAction extends Action {

    private ReportEditor reportEditor;

    private GraphicalViewer viewer;

    public AbstractPreferenceAction(String text, ImageDescriptor image, ReportEditor reportEditor, GraphicalViewer viewer) {
        super(text, image);
        this.reportEditor = reportEditor;
        this.viewer = viewer;
    }

    public AbstractPreferenceAction(String text, int style, ReportEditor reportEditor, GraphicalViewer viewer) {
        super(text, style);
        this.reportEditor = reportEditor;
        this.viewer = viewer;
    }

    public AbstractPreferenceAction(String text, ReportEditor reportEditor, GraphicalViewer viewer) {
        super(text);
        this.reportEditor = reportEditor;
        this.viewer = viewer;
    }

    protected ReportEditor getReportEditor() {
        return reportEditor;
    }

    protected GraphicalViewer getViewer() {
        return viewer;
    }

    protected boolean getViewerProperty(String name) {
        Boolean value = (Boolean) getViewer().getProperty(name);
        return value == null ? false : value.booleanValue();
    }

    protected void setViewerProperty(String name, boolean value) {
        getViewer().setProperty(name, value);
    }

    protected void setPreferenceValue(String name, boolean value) {
        getPreferenceStore().setValue(name, value);
    }

    protected IPreferenceStore getPreferenceStore() {
        return ReportDesignerPlugin.getDefault().getPreferenceStore();
    }
}
