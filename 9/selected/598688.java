package ieditor;

import ieditor.actions.BackAction;
import ieditor.actions.ImageInsertAction;
import ieditor.actions.MoveToBackAction;
import ieditor.actions.MoveToFrontAction;
import ieditor.actions.NavigateAction;
import ieditor.model.PageDiagram;
import ieditor.parts.InfoKeyHandler;
import ieditor.parts.ShapesEditPartFactory;
import ieditor.parts.ShapesTreeEditPartFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.EventObject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;

/**
 * A graphical editor with flyout palette that can edit .shapes files.
 * The binding between the .shapes file extension and this editor is done in plugin.xml
 * @author Elias Volanakis
 */
public class ElementEditor extends GraphicalEditorWithPalettes implements ITabbedPropertySheetPageContributor, IReusableEditor {

    /** This is the root of the editor's model. */
    private PageDiagram diagram;

    /** Palette component, holding the tools and shapes. */
    private static PaletteRoot PALETTE_MODEL;

    public static final String ID = "ieditor.editor";

    private HistoryAdapter historyAdapter = new HistoryAdapter();

    /** Create a new ShapesEditor instance. This is called by the Workspace. */
    public ElementEditor() {
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
        FigureCanvas canvas = (FigureCanvas) viewer.getControl();
        GridData data = new GridData();
        data.horizontalAlignment = GridData.CENTER;
        data.grabExcessHorizontalSpace = true;
        data.verticalAlignment = GridData.CENTER;
        data.grabExcessVerticalSpace = true;
        canvas.setLayoutData(data);
        canvas.setBorder(new LineBorder(1));
        canvas.setSize(720, 576);
        canvas.setVerticalScrollBarVisibility(FigureCanvas.NEVER);
        canvas.setHorizontalScrollBarVisibility(FigureCanvas.NEVER);
        InfoKeyHandler keyHandler = new InfoKeyHandler(viewer, this);
        keyHandler.put(KeyStroke.getPressed(SWT.BS, 8, 0), getActionRegistry().getAction(BackAction.GO_BACK));
        keyHandler.put(KeyStroke.getPressed(SWT.ARROW_LEFT, 0), getActionRegistry().getAction(NavigateAction.LEFT));
        keyHandler.put(KeyStroke.getPressed(SWT.ARROW_RIGHT, 0), getActionRegistry().getAction(NavigateAction.RIGHT));
        keyHandler.put(KeyStroke.getPressed(SWT.ARROW_UP, 0), getActionRegistry().getAction(NavigateAction.UP));
        keyHandler.put(KeyStroke.getPressed(SWT.ARROW_DOWN, 0), getActionRegistry().getAction(NavigateAction.DOWN));
        keyHandler.put(KeyStroke.getPressed(SWT.CR, 13, 0), getActionRegistry().getAction(NavigateAction.OK));
        viewer.setEditPartFactory(new ShapesEditPartFactory());
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.setKeyHandler(keyHandler);
        ContextMenuProvider cmProvider = new ElementEditorContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(cmProvider);
    }

    public void commandStackChanged(EventObject event) {
        firePropertyChange(IEditorPart.PROP_DIRTY);
        super.commandStackChanged(event);
    }

    private void createOutputStream(OutputStream os) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(getModel());
        oos.close();
    }

    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new PaletteViewerProvider(getEditDomain()) {

            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
            }
        };
    }

    private TransferDropTargetListener createTransferDropTargetListener() {
        return new TemplateTransferDropTargetListener(getGraphicalViewer()) {

            protected CreationFactory getFactory(Object template) {
                return new SimpleFactory((Class) template);
            }
        };
    }

    public void doSave(IProgressMonitor monitor) {
        System.out.println("Saving");
        try {
            JAXBContext context = JAXBContext.newInstance(PageDiagram.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            IPath path = ((ElementEditorInput) getEditorInput()).getPath();
            File file = path.toFile();
            m.marshal(getModel(), file);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public void doSaveAs() {
        System.out.println("Do save as");
        Shell shell = getSite().getWorkbenchWindow().getShell();
        SaveAsDialog dialog = new SaveAsDialog(shell);
        IPath pathTmp = ((ElementEditorInput) getEditorInput()).getPath();
        IFile fileTmp = ResourcesPlugin.getWorkspace().getRoot().getFile(pathTmp);
        dialog.setOriginalFile(fileTmp);
        dialog.open();
        IPath path = dialog.getResult();
        if (path != null) {
            final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
            try {
                new ProgressMonitorDialog(shell).run(false, false, new WorkspaceModifyOperation() {

                    public void execute(final IProgressMonitor monitor) {
                        try {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            createOutputStream(out);
                            file.create(new ByteArrayInputStream(out.toByteArray()), true, monitor);
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

    public Object getAdapter(Class type) {
        if (type == IContentOutlinePage.class) return new ShapesOutlinePage(new TreeViewer());
        if (type == ElementEditor.class) return this;
        if (type == HistoryAdapter.class) return historyAdapter;
        return super.getAdapter(type);
    }

    PageDiagram getModel() {
        return diagram;
    }

    protected PaletteRoot getPaletteRoot() {
        if (PALETTE_MODEL == null) PALETTE_MODEL = ElementEditorPaletteFactory.createPalette();
        return PALETTE_MODEL;
    }

    @SuppressWarnings("unused")
    private void handleLoadException(Exception e) {
        System.err.println("** Load failed. Using default model. **");
        e.printStackTrace();
        diagram = new PageDiagram();
    }

    /**
 * Set up the editor's inital content (after creation).
 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#initializeGraphicalViewer()
 */
    protected void initializeGraphicalViewer() {
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getModel());
        viewer.addDropTargetListener(createTransferDropTargetListener());
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    public void setInput(IEditorInput input) {
        ElementEditorInput shapesInput = ((ElementEditorInput) input);
        diagram = shapesInput.getPageDiagram();
        setPartName(shapesInput.getName());
        setInputWithNotify(input);
        this.firePropertyChange(IEditorPart.PROP_INPUT);
        if (getGraphicalViewer() != null) getGraphicalViewer().setContents(getModel());
    }

    /**
 * Creates an outline pagebook for this editor.
 */
    @SuppressWarnings("unchecked")
    public void createActions() {
        super.createActions();
        ActionRegistry registry = getActionRegistry();
        IAction action = new ImageInsertAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new MoveToFrontAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new MoveToBackAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new NavigateAction(this, NavigateAction.RIGHT);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new NavigateAction(this, NavigateAction.LEFT);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new NavigateAction(this, NavigateAction.UP);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new NavigateAction(this, NavigateAction.DOWN);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new NavigateAction(this, NavigateAction.OK);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
    }

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
            getViewer().setEditPartFactory(new ShapesTreeEditPartFactory());
            ContextMenuProvider cmProvider = new ElementEditorContextMenuProvider(getViewer(), getActionRegistry());
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

    public String getContributorId() {
        return getSite().getId();
    }
}
