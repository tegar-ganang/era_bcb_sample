package net.sourceforge.dotclipse.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.tools.ConnectionCreationTool;
import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

/**
 * @author Leo
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DotGraphicalEditorPage extends GraphicalEditorWithPalette {

    private PaletteRoot paletteRoot;

    private ScalableRootEditPart root;

    public DotGraphicalEditorPage() {
        DefaultEditDomain defaultEditDomain = new DefaultEditDomain(this);
        defaultEditDomain.setActiveTool(new ConnectionCreationTool());
        setEditDomain(defaultEditDomain);
    }

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);
    }

    protected PaletteRoot getPaletteRoot() {
        if (paletteRoot == null) {
            paletteRoot = new PaletteRoot();
        }
        return paletteRoot;
    }

    protected void initializeGraphicalViewer() {
    }

    public void doSave(IProgressMonitor monitor) {
    }

    public void doSaveAs() {
    }

    public boolean isDirty() {
        return false;
    }

    public boolean isSaveAsAllowed() {
        return false;
    }
}
