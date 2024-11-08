package org.destecs.ide.modelmanagement.ui.showlog;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

public class LogGraphEditor extends GraphicalEditor {

    public static final String ID = "org.destecs.ide.modelmanagement.ui.showlog.LogGraphEditor";

    public LogGraphEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(Composite parent) {
    }

    @Override
    public void setFocus() {
    }

    @Override
    protected void initializeGraphicalViewer() {
    }
}
