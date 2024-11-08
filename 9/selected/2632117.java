package de.tud.eclipse.plugins.controlflow.view.gef;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.FreeformGraphicalRootEditPart;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import de.tud.eclipse.plugins.controlflow.view.ViewBackend;

public class CFlowGraphicalEditor extends GraphicalEditor {

    private ViewBackend view;

    public CFlowGraphicalEditor(ViewBackend view) {
        this.view = view;
        this.setEditDomain(new DefaultEditDomain(this));
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setRootEditPart(new FreeformGraphicalRootEditPart());
        getGraphicalViewer().setEditPartFactory(new CFlowEditPartFactory(view));
    }

    @Override
    protected void initializeGraphicalViewer() {
    }

    @Override
    public void doSave(IProgressMonitor arg0) {
    }
}
