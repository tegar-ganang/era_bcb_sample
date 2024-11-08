package it.cnr.stlab.xd.plugin.editor;

import it.cnr.stlab.xd.manager.IManager.ManagerException;
import it.cnr.stlab.xd.plugin.XDPlugin;
import it.cnr.stlab.xd.plugin.FileUtils;
import it.cnr.stlab.xd.plugin.editor.delegates.OWLModelDelegate;
import it.cnr.stlab.xd.plugin.editor.model.ModelFactory;
import it.cnr.stlab.xd.plugin.editor.model.WorkingOntology;
import it.cnr.stlab.xd.plugin.editor.parts.XDEditorPartFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.EventObject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.views.palette.PalettePage;
import org.eclipse.gef.ui.views.palette.PaletteViewerPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.swt.SWT;
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
import org.semanticweb.owl.model.OWLOntologyStorageException;
import org.semanticweb.owl.model.UnknownOWLOntologyException;

/**
 * This is the XD Graphical Editor class TODO Outline page
 * 
 * @author Enrico Daga
 * 
 */
public class XDGraphicalEditor extends GraphicalEditor {

    public static final String ID = "it.cnr.stlab.xd.editors.XDGraphicalEditor";

    private PaletteViewerProvider provider;

    private FlyoutPaletteComposite splitter;

    private XDPalettePage page;

    private static PaletteRoot PALETTE_MODEL;

    private WorkingOntology ontology;

    /**
	 * Constructor. Initialize the EditDomain.
	 */
    public XDGraphicalEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
    }

    @Override
    public void createPartControl(Composite parent) {
        splitter = new FlyoutPaletteComposite(parent, SWT.NONE, getSite().getPage(), getPaletteViewerProvider(), getPalettePreferences());
        super.createPartControl(splitter);
        splitter.setGraphicalControl(getGraphicalControl());
        if (page != null) {
            splitter.setExternalViewer(page.getPaletteViewer());
            page = null;
        }
    }

    protected XDPalettePage createPalettePage() {
        return new XDPalettePage(getPaletteViewerProvider());
    }

    @Override
    protected void initializeGraphicalViewer() {
        IEditorInput editorI = getEditorInput();
        XDGuiEditorInput in;
        if (editorI instanceof XDGuiEditorInput) {
        } else if (editorI instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) editorI).getFile();
            try {
                setInput(new XDGuiEditorInput(file.getProject().getName(), XDPlugin.getManager().getLogicalURI(file.getProject().getName(), file)));
            } catch (ManagerException e) {
                e.printStackTrace();
                System.err.println(e.getLocalizedMessage());
            }
        } else {
            System.err.println("Unsupported editor input");
        }
        in = ((XDGuiEditorInput) getEditorInput());
        OWLModelDelegate delegate = in.getOWLModelDelegate();
        ontology = ModelFactory.createWorkingOntology(delegate);
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(ontology);
        splitter.hookDropTargetListener(getGraphicalViewer());
        viewer.addDropTargetListener(createTransferDropTargetListener());
        getEditDomain().setPaletteRoot(getPaletteRoot());
    }

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new XDEditorPartFactory());
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        ActionRegistry registry = getActionRegistry();
        IActionBars bars = getEditorSite().getActionBars();
        String id = ActionFactory.UNDO.getId();
        bars.setGlobalActionHandler(id, registry.getAction(id));
        id = ActionFactory.REDO.getId();
        bars.setGlobalActionHandler(id, registry.getAction(id));
        id = ActionFactory.DELETE.getId();
        bars.setGlobalActionHandler(id, registry.getAction(id));
        ContextMenuProvider cmProvider = new XDEditorContextMenuProvider(viewer, getActionRegistry());
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

    private TransferDropTargetListener createTransferDropTargetListener() {
        return new TemplateTransferDropTargetListener(getGraphicalViewer()) {

            protected CreationFactory getFactory(Object template) {
                return new SimpleFactory((Class<?>) template);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class type) {
        if (type == PalettePage.class) {
            if (splitter == null) {
                page = createPalettePage();
                return page;
            }
            return createPalettePage();
        }
        return super.getAdapter(type);
    }

    protected Control getGraphicalControl() {
        return getGraphicalViewer().getControl();
    }

    protected FlyoutPreferences getPalettePreferences() {
        return FlyoutPaletteComposite.createFlyoutPreferences(XDPlugin.getDefault().getPluginPreferences());
    }

    protected PaletteRoot getPaletteRoot() {
        if (PALETTE_MODEL == null) PALETTE_MODEL = new XDEditorPaletteFactory(this).createPalette();
        return PALETTE_MODEL;
    }

    protected final PaletteViewerProvider getPaletteViewerProvider() {
        if (provider == null) provider = createPaletteViewerProvider();
        return provider;
    }

    protected void setEditDomain(DefaultEditDomain ed) {
        super.setEditDomain(ed);
    }

    private void handleLoadException(Exception e) {
        System.err.println("** Load failed.  **");
        e.printStackTrace();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        Shell shell = getSite().getWorkbenchWindow().getShell();
        try {
            new ProgressMonitorDialog(shell).run(false, false, new WorkspaceModifyOperation() {

                public void execute(final IProgressMonitor monitor) {
                    try {
                        IFile file = ((IFileEditorInput) getEditorInput()).getFile();
                        URI f = new FileUtils().getLocalURI(file);
                        IProject project = file.getProject();
                        ontology.getOWLModelDelegate().getOWLOntologyManager().saveOntology(ontology.getOWLModelDelegate().getOWLOntology(), f);
                        getCommandStack().markSaveLocation();
                        project.refreshLocal(IProject.DEPTH_INFINITE, monitor);
                    } catch (UnknownOWLOntologyException e) {
                        e.printStackTrace();
                    } catch (OWLOntologyStorageException e) {
                        e.printStackTrace();
                    } catch (CoreException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            handleLoadException(e);
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
                            getModel().getOWLModelDelegate().getOWLOntologyManager().saveOntology(getModel().getOWLModelDelegate().getOWLOntology(), new FileUtils().getLocalURI(file));
                        } catch (UnknownOWLOntologyException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e.getMessage());
                        } catch (OWLOntologyStorageException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e.getMessage());
                        }
                    }
                });
                setInput(new XDGuiEditorInput(file.getProject().getName(), XDPlugin.getManager().getLogicalURI(file.getProject().getName(), file)));
                getCommandStack().markSaveLocation();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } catch (InvocationTargetException ite) {
                ite.printStackTrace();
            } catch (RuntimeException ex) {
                MessageDialog.openInformation(getSite().getWorkbenchWindow().getShell(), "Save as failed", ex.getMessage());
            } catch (ManagerException exx) {
                exx.printStackTrace();
                MessageDialog.openInformation(getSite().getWorkbenchWindow().getShell(), "Save as failed", exx.getMessage());
            }
        }
    }

    protected class XDPalettePage extends PaletteViewerPage {

        /**
		 * Constructor
		 * 
		 * @param provider
		 *            the provider used to create a PaletteViewer
		 */
        public XDPalettePage(PaletteViewerProvider provider) {
            super(provider);
        }

        /**
		 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
        public void createControl(Composite parent) {
            super.createControl(parent);
            if (splitter != null) splitter.setExternalViewer(viewer);
        }

        /**
		 * @see org.eclipse.ui.part.IPage#dispose()
		 */
        public void dispose() {
            if (splitter != null) splitter.setExternalViewer(null);
            super.dispose();
        }

        /**
		 * @return the PaletteViewer created and displayed by this page
		 */
        public PaletteViewer getPaletteViewer() {
            return viewer;
        }
    }

    public WorkingOntology getModel() {
        return ontology;
    }
}
