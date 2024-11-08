package com.koutra.dist.proc.designer.editor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.EventObject;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import com.koutra.dist.proc.designer.Messages;
import com.koutra.dist.proc.designer.editor.parts.ProcessEditPartFactory;
import com.koutra.dist.proc.designer.editor.parts.ProcessTreeEditPartFactory;
import com.koutra.dist.proc.designer.model.Process;
import com.koutra.dist.proc.designer.model.ProcessStep;
import com.koutra.dist.proc.designer.model.ValidationError;
import com.koutra.dist.proc.designer.utils.DesignerLog;

public class ProcessEditor extends GraphicalEditorWithFlyoutPalette {

    /** This is the root of the editor's model. */
    private com.koutra.dist.proc.designer.model.Process process;

    /** Palette component, holding the tools and shapes. */
    private static PaletteRoot PALETTE_MODEL;

    /** Create a new ShapesEditor instance. This is called by the Workspace. */
    public ProcessEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    /**
	 * Configure the graphical viewer before it receives contents.
	 * <p>This is the place to choose an appropriate RootEditPart and EditPartFactory
	 * for your editor. The RootEditPart determines the behavior of the editor's "work-area".
	 * For example, GEF includes zoomable and scrollable root edit parts. The EditPartFactory
	 * maps model elements to edit parts (controllers).</p>
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
	 */
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new ProcessEditPartFactory());
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        ContextMenuProvider cmProvider = new ProcessEditorContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(cmProvider);
        getSite().registerContextMenu(cmProvider, viewer);
    }

    public void commandStackChanged(EventObject event) {
        firePropertyChange(IEditorPart.PROP_DIRTY);
        super.commandStackChanged(event);
    }

    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new PaletteViewerProvider(getEditDomain()) {

            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
                viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
            }
        };
    }

    /**
	 * Create a transfer drop target listener. When using a CombinedTemplateCreationEntry
	 * tool in the palette, this will enable model element creation by dragging from the palette.
	 * @see #createPaletteViewerProvider()
	 */
    private TransferDropTargetListener createTransferDropTargetListener() {
        return new TemplateTransferDropTargetListener(getGraphicalViewer()) {

            @SuppressWarnings("unchecked")
            protected CreationFactory getFactory(Object template) {
                return new SimpleFactory((Class) template);
            }
        };
    }

    public void doSave(IProgressMonitor monitor) {
        try {
            IFile file = ((IFileEditorInput) getEditorInput()).getFile();
            Process.dehydrate(file, process, monitor);
            getCommandStack().markSaveLocation();
        } catch (CoreException ce) {
            ce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void doSaveAs() {
        Shell shell = getSite().getWorkbenchWindow().getShell();
        SaveAsDialog dialog = new SaveAsDialog(shell);
        dialog.setOriginalFile(((IFileEditorInput) getEditorInput()).getFile());
        dialog.open();
        IPath path = dialog.getResult();
        if (path != null) {
            final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
            try {
                new ProgressMonitorDialog(shell).run(false, false, new WorkspaceModifyOperation() {

                    public void execute(final IProgressMonitor monitor) {
                        try {
                            Process.dehydrate(file, process, monitor);
                        } catch (CoreException ce) {
                            ce.printStackTrace();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                });
                setInput(new FileEditorInput(file));
                getCommandStack().markSaveLocation();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } catch (InvocationTargetException ite) {
                ite.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class type) {
        if (type == IContentOutlinePage.class) return new ShapesOutlinePage(new TreeViewer());
        return super.getAdapter(type);
    }

    public com.koutra.dist.proc.designer.model.Process getModel() {
        return process;
    }

    @SuppressWarnings("unchecked")
    protected EditPart getPartFor(ProcessStep step) {
        List steps = getGraphicalViewer().getContents().getChildren();
        for (Object s : steps) {
            EditPart editPart = (EditPart) s;
            if (editPart.getModel() == step) return editPart;
        }
        return null;
    }

    public String validate() {
        List<ValidationError> validationErrors = process.validate();
        if (validationErrors.size() == 0) return null;
        ValidationError error = validationErrors.get(0);
        EditPart childEditPart = getPartFor((ProcessStep) error.getElement());
        getGraphicalViewer().setSelection(new StructuredSelection(childEditPart));
        return Messages.ProcessEditor_ValidationErrorPreamble + error.getErrorMessage();
    }

    protected PaletteRoot getPaletteRoot() {
        if (PALETTE_MODEL == null) PALETTE_MODEL = ProcessEditorPaletteFactory.createPalette();
        return PALETTE_MODEL;
    }

    /**
	 * Set up the editor's inital content (after creation).
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#initializeGraphicalViewer()
	 */
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getModel());
        viewer.addDropTargetListener(createTransferDropTargetListener());
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    protected void setInput(IEditorInput input) {
        super.setInput(input);
        try {
            IFile file = ((IFileEditorInput) input).getFile();
            process = Process.hydrate(file);
            setPartName(file.getName());
        } catch (Exception e) {
            DesignerLog.logError("Unable to load input", e);
        }
    }

    /**
	 * Creates an outline pagebook for this editor.
	 */
    public class ShapesOutlinePage extends ContentOutlinePage {

        /**
		 * Create a new outline page for the shapes editor.
		 * @param viewer a viewer (TreeViewer instance) used for this outline page
		 * @throws IllegalArgumentException if editor is null
		 */
        public ShapesOutlinePage(EditPartViewer viewer) {
            super(viewer);
        }

        public void createControl(Composite parent) {
            getViewer().createControl(parent);
            getViewer().setEditDomain(getEditDomain());
            getViewer().setEditPartFactory(new ProcessTreeEditPartFactory());
            ContextMenuProvider cmProvider = new ProcessEditorContextMenuProvider(getViewer(), getActionRegistry());
            getViewer().setContextMenu(cmProvider);
            getSite().registerContextMenu("org.eclipse.gef.examples.shapes.outline.contextmenu", cmProvider, getSite().getSelectionProvider());
            getSelectionSynchronizer().addViewer(getViewer());
            getViewer().setContents(getModel());
        }

        public void dispose() {
            getSelectionSynchronizer().removeViewer(getViewer());
            super.dispose();
        }

        public Control getControl() {
            return getViewer().getControl();
        }

        /**
		 * @see org.eclipse.ui.part.IPageBookViewPage#init(org.eclipse.ui.part.IPageSite)
		 */
        public void init(IPageSite pageSite) {
            super.init(pageSite);
            ActionRegistry registry = getActionRegistry();
            IActionBars bars = pageSite.getActionBars();
            String id = ActionFactory.UNDO.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = ActionFactory.REDO.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = ActionFactory.DELETE.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
        }
    }
}
