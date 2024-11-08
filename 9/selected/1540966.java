package net.sf.povclipsetextur.imagemeta.core.ui.editors.imagebrowser;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.ui.parts.GraphicalEditor;

/**
 * @author Christoph Graupner
 *
 */
public class EditorImageBrowser extends GraphicalEditor {

    public static final String ID = "net.sf.povclipsetextur.imagemeta.core.ui.editors.imagebrowser";

    /**
	 * 
	 */
    public EditorImageBrowser() {
        setEditDomain(new DefaultEditDomain(this));
    }

    @Override
    protected void initializeGraphicalViewer() {
        setPartName("IM: " + getEditorInput().getName());
    }

    @Override
    public void doSave(IProgressMonitor aMonitor) {
    }
}
