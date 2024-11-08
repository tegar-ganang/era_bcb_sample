package org.ist.contract.editor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.ist.contract.impl.ContractImpl;

/**
 * An abstract base class for all editor pages used in the
 * <code>ContractEditor</code>.
 * 
 * <p>It provides basic support for typical GEF handling like 
 * <code>CommandStack</code>s, <code>GraphicalViewer</code>s, a
 * Palette and so on.
 * 
 */
public abstract class AbstractEditorPage extends EditorPart {

    /** the parent multi page editor */
    private final ContractEditor parent;

    /** the edit domain */
    private final EditDomain domain;

    /**
	 * Creates a new AbstractEditorPage instance.
	 * @param parent the parent multi page editor
	 * @param domain the edit domain
	 */
    public AbstractEditorPage(ContractEditor parent, EditDomain domain) {
        super();
        this.parent = parent;
        this.domain = domain;
    }

    public final void doSave(IProgressMonitor monitor) {
        getContractEditor().doSave(monitor);
    }

    public final void doSaveAs() {
        getContractEditor().doSaveAs();
    }

    public void gotoMarker(IMarker marker) {
    }

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        setPartName(input.getName() + ": " + getPageName());
    }

    public final boolean isDirty() {
        return getContractEditor().isDirty();
    }

    /**
	 * Returns the <code>CommandStack</code> of this editor page.
	 * @return the <code>CommandStack</code> of this editor page
	 */
    protected final CommandStack getCommandStack() {
        return getEditDomain().getCommandStack();
    }

    /**
	 * Returns the <code>PaletteRoot</code> this editor page uses.
	 * @return the <code>PaletteRoot</code>
	 */
    protected PaletteRoot getPaletteRoot() {
        return getContractEditor().getPaletteRoot();
    }

    public final boolean isSaveAsAllowed() {
        return getContractEditor().isSaveAsAllowed();
    }

    public void setFocus() {
        getGraphicalViewer().getControl().setFocus();
    }

    public final void createPartControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 10;
        layout.marginWidth = 10;
        layout.verticalSpacing = 5;
        layout.horizontalSpacing = 5;
        layout.numColumns = 1;
        composite.setLayout(layout);
        composite.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        composite.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
        Label label = new Label(composite, SWT.HORIZONTAL | SWT.SHADOW_OUT | SWT.LEFT);
        label.setText(getTitle());
        label.setFont(JFaceResources.getFontRegistry().get(JFaceResources.HEADER_FONT));
        label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        label.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
        label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        composite = new Composite(composite, SWT.NONE);
        composite.setLayout(new FillLayout());
        composite.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        composite.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        createPageControl(composite);
    }

    /**
	 * Returns the human readable name of this editor page.
	 * @return the human readable name of this editor page
	 */
    protected abstract String getPageName();

    /**
	 * Creates the cotrol of this editor page.
	 * @param parent
	 */
    protected abstract void createPageControl(Composite parent);

    /**
	 * Returns the multi page Contract editor this editor page is contained in.
	 * @return the parent multi page editor
	 */
    protected final ContractEditor getContractEditor() {
        return parent;
    }

    /**
	 * Returns the edit domain this editor page uses.
	 * @return the edit domain this editor page uses
	 */
    public final EditDomain getEditDomain() {
        return domain;
    }

    /**
	 * Hooks a <code>EditPartViewer</code> to the rest of the Editor.
	 * 
	 * <p>By default, the viewer is added to the SelectionSynchronizer, 
	 * which can be used to keep 2 or more EditPartViewers in sync.
	 * The viewer is also registered as the ISelectionProvider
	 * for the Editor's PartSite.
	 * 
	 * @param viewer the viewer to hook into the editor
	 */
    protected void registerEditPartViewer(EditPartViewer viewer) {
        getEditDomain().addViewer(viewer);
        getContractEditor().getSelectionSynchronizer().addViewer(viewer);
        getSite().setSelectionProvider(viewer);
    }

    /**
	 * Configures the specified <code>EditPartViewer</code>.
	 * 
	 * @param viewer
	 */
    protected void configureEditPartViewer(EditPartViewer viewer) {
        if (viewer.getKeyHandler() != null) viewer.getKeyHandler().setParent(getContractEditor().getSharedKeyHandler());
    }

    /**
	 * Returns the Contract that is edited.
	 * @return the Contract that is edited
	 */
    protected ContractImpl getContract() {
        return (ContractImpl) getContractEditor().getContract();
    }

    /** the palette viewer */
    private PaletteViewer paletteViewer;

    /**
	 * Creates the createPaletteViewer on the specified <code>Composite</code>.
	 * @param parent the parent composite
	 */
    protected void createPaletteViewer(Composite parent) {
        paletteViewer = new PaletteViewer();
        paletteViewer.createControl(parent);
        paletteViewer.getControl().setBackground(parent.getBackground());
        getEditDomain().setPaletteViewer(paletteViewer);
        getEditDomain().setPaletteRoot(getPaletteRoot());
    }

    /**
	 * Returns the palette viewer.
	 * @return the palette viewer
	 */
    protected PaletteViewer getPaletteViewer() {
        return paletteViewer;
    }

    /**
	 * Returns the graphical viewer of this page.
	 * 
	 * <p>This viewer is used for example for zoom support 
	 * and for the thumbnail in the overview of the outline page.
	 * 
	 * @return the viewer
	 */
    protected abstract GraphicalViewer getGraphicalViewer();
}
