package com.metanology.mde.ui.pimEditor.diagrams;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.draw2d.parts.Thumbnail;
import org.eclipse.gef.*;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.AlignmentAction;
import org.eclipse.gef.ui.actions.CopyTemplateAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.PaletteContextMenuProvider;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.parts.*;
import org.eclipse.gef.ui.stackview.CommandStackInspectorPage;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import com.metanology.mde.core.ui.common.MDEPluginImages;
import com.metanology.mde.core.ui.plugin.MDEPlugin;
import com.metanology.mde.utils.Messages;
import com.metanology.mde.core.metaModel.Component;
import com.metanology.mde.core.metaModel.MetaClass;
import com.metanology.mde.core.metaModel.MetaObject;
import com.metanology.mde.core.metaModel.Package;
import com.metanology.mde.core.metaModel.Subsystem;
import com.metanology.mde.ui.pimEditor.actions.PIMDeleteFromDiagramAction;
import com.metanology.mde.ui.pimEditor.actions.PIMDeleteFromPIMAction;
import com.metanology.mde.ui.pimEditor.actions.PIMDirectEditAction;
import com.metanology.mde.ui.pimEditor.actions.PIMExecuteMetaProgramAction;
import com.metanology.mde.ui.pimEditor.actions.PIMPasteTemplateAction;
import com.metanology.mde.ui.pimEditor.actions.PIMRefreshRelationAction;
import com.metanology.mde.ui.pimEditor.actions.PIMSelectInExplorerAction;
import com.metanology.mde.ui.pimEditor.dnd.PIMDiagramExplorerTransferDropTargetListener;
import com.metanology.mde.ui.pimEditor.dnd.PIMDiagramTemplateTransferDropTargetListener;
import com.metanology.mde.ui.pimEditor.edit.PIMEditPartFactory;
import com.metanology.mde.ui.pimEditor.model.ClassNode;
import com.metanology.mde.ui.pimEditor.model.ComponentNode;
import com.metanology.mde.ui.pimEditor.model.DiagramObjectNode;
import com.metanology.mde.ui.pimEditor.model.PIMDiagram;
import com.metanology.mde.ui.pimEditor.model.PackageNode;
import com.metanology.mde.ui.pimEditor.model.SubsystemNode;
import com.metanology.mde.ui.pimEditor.xmlserializers.PIMDiagramSerializer;
import com.metanology.mde.ui.pimExplorer.PIMTreeView;

/**
 * Abstract Editor for the pim diagram
 * 
 * @author wwang
 */
public abstract class AbstractDiagramEditor extends GraphicalEditorWithPalette implements CommandStackListener, PropertyChangeListener {

    class OutlinePage extends ContentOutlinePage {

        private PageBook pageBook;

        private Control outline;

        private Canvas overview;

        private IAction showOutlineAction, showOverviewAction;

        static final int ID_OUTLINE = 0;

        static final int ID_OVERVIEW = 1;

        private boolean overviewInitialized;

        private Thumbnail thumbnail;

        public OutlinePage(EditPartViewer viewer) {
            super(viewer);
        }

        public void init(IPageSite pageSite) {
            super.init(pageSite);
            ActionRegistry registry = getActionRegistry();
            IActionBars bars = pageSite.getActionBars();
            String id = IWorkbenchActionConstants.UNDO;
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = IWorkbenchActionConstants.REDO;
            bars.setGlobalActionHandler(id, registry.getAction(id));
            bars.updateActionBars();
        }

        protected void configureOutlineViewer() {
            getViewer().setEditDomain(getEditDomain());
            getViewer().setEditPartFactory(new TreePartFactory());
            ContextMenuProvider provider = new PIMDiagramContextMenuProvider(getViewer(), getActionRegistry());
            getViewer().setContextMenu(provider);
            getSite().registerContextMenu("com.metanology.mde.ui.pimEditor.diagrams.outline.contextmenu", provider, getSite().getSelectionProvider());
            getViewer().setKeyHandler(getCommonKeyHandler());
            getViewer().addDropTargetListener(new PIMDiagramTemplateTransferDropTargetListener(getViewer()));
            IToolBarManager tbm = getSite().getActionBars().getToolBarManager();
            showOutlineAction = new Action() {

                public void run() {
                    showPage(ID_OUTLINE);
                }
            };
            showOutlineAction.setImageDescriptor(MDEPluginImages.DESC_PIM_EDT_OUTLINE);
            tbm.add(showOutlineAction);
            showOverviewAction = new Action() {

                public void run() {
                    showPage(ID_OVERVIEW);
                }
            };
            showOverviewAction.setImageDescriptor(MDEPluginImages.DESC_PIM_EDT_OVERVIEW);
            tbm.add(showOverviewAction);
            showPage(ID_OVERVIEW);
        }

        public void createControl(Composite parent) {
            pageBook = new PageBook(parent, SWT.NONE);
            outline = getViewer().createControl(pageBook);
            overview = new Canvas(pageBook, SWT.NONE);
            pageBook.showPage(outline);
            configureOutlineViewer();
            hookOutlineViewer();
            initializeOutlineViewer();
        }

        public void dispose() {
            unhookOutlineViewer();
            super.dispose();
        }

        public Control getControl() {
            return pageBook;
        }

        protected void hookOutlineViewer() {
            getSelectionSynchronizer().addViewer(getViewer());
        }

        protected void initializeOutlineViewer() {
            getViewer().setContents(getPIMDiagram());
        }

        protected void initializeOverview() {
            LightweightSystem lws = new LightweightSystem(overview);
            RootEditPart rep = getGraphicalViewer().getRootEditPart();
            if (rep instanceof ScalableFreeformRootEditPart) {
                ScalableFreeformRootEditPart root = (ScalableFreeformRootEditPart) rep;
                thumbnail = new ScrollableThumbnail((Viewport) root.getFigure());
                thumbnail.setSource(root.getLayer(LayerConstants.PRINTABLE_LAYERS));
                lws.setContents(thumbnail);
            }
        }

        protected void showPage(int id) {
            if (id == ID_OUTLINE) {
                showOutlineAction.setChecked(true);
                showOverviewAction.setChecked(false);
                pageBook.showPage(outline);
                if (thumbnail != null) thumbnail.setVisible(false);
            } else if (id == ID_OVERVIEW) {
                if (!overviewInitialized) initializeOverview();
                showOutlineAction.setChecked(false);
                showOverviewAction.setChecked(true);
                pageBook.showPage(overview);
                thumbnail.setVisible(true);
            }
        }

        protected void unhookOutlineViewer() {
            getSelectionSynchronizer().removeViewer(getViewer());
        }
    }

    private static final char CHAR_CTRL_D = (char) 4;

    private static final int PALETTE_W = 30;

    protected PIMDiagram pimDiagram;

    protected KeyHandler sharedKeyHandler;

    private boolean savePreviouslyNeeded = false;

    private ResourceTracker resourceListener = new ResourceTracker();

    protected static final String PALETTE_SIZE = "Palette Size";

    protected static final int DEFAULT_PALETTE_SIZE = 30;

    class ResourceTracker implements IResourceChangeListener, IResourceDeltaVisitor {

        public void resourceChanged(IResourceChangeEvent event) {
            IResourceDelta delta = event.getDelta();
            try {
                if (delta != null) delta.accept(this);
            } catch (CoreException exception) {
                MDEPlugin.showMessage(MDEPlugin.getResourceString("AbstractDiagramEditor.handleResourceChangeFailed"));
            }
        }

        public boolean visit(IResourceDelta delta) {
            if (delta == null || !delta.getResource().equals(((FileEditorInput) getEditorInput()).getFile())) return true;
            if (delta.getKind() == IResourceDelta.REMOVED) {
                if ((IResourceDelta.MOVED_TO & delta.getFlags()) == 0) {
                    if (!isDirty()) closeEditor(false);
                } else {
                    final IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(delta.getMovedToPath());
                    Display display = getSite().getShell().getDisplay();
                    display.asyncExec(new Runnable() {

                        public void run() {
                            superSetInput(new FileEditorInput(newFile));
                        }
                    });
                }
            }
            return false;
        }
    }

    private IPartListener partListener = new IPartListener() {

        public void partActivated(IWorkbenchPart part) {
            if (part != AbstractDiagramEditor.this) return;
            if (!((FileEditorInput) getEditorInput()).getFile().exists()) {
                Shell shell = getSite().getShell();
                String title = OldGEFMessages.GraphicalEditor_FILE_DELETED_TITLE_UI;
                String message = OldGEFMessages.GraphicalEditor_FILE_DELETED_WITHOUT_SAVE_INFO;
                String[] buttons = { OldGEFMessages.GraphicalEditor_SAVE_BUTTON_UI, OldGEFMessages.GraphicalEditor_CLOSE_BUTTON_UI };
                MessageDialog dialog = new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, buttons, 0);
                if (dialog.open() == 0) {
                    if (!performSaveAs()) partActivated(part);
                } else {
                    closeEditor(false);
                }
            }
        }

        public void partBroughtToTop(IWorkbenchPart part) {
        }

        public void partClosed(IWorkbenchPart part) {
            if (part != AbstractDiagramEditor.this) return;
            MDEPlugin.getDefault().getRuntime().removePropertyChangeListener(AbstractDiagramEditor.this);
        }

        public void partDeactivated(IWorkbenchPart part) {
        }

        public void partOpened(IWorkbenchPart part) {
            if (part != AbstractDiagramEditor.this) return;
            MDEPlugin.getDefault().getRuntime().addPropertyChangeListener(AbstractDiagramEditor.this);
        }
    };

    /**
     * Constructor for ClassDiagramEditor.
     */
    public AbstractDiagramEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    protected String getEditorTitle(IFile file) {
        return file.getName();
    }

    protected void closeEditor(final boolean save) {
        Display.getDefault().syncExec(new Runnable() {

            public void run() {
                getSite().getPage().closeEditor(AbstractDiagramEditor.this, save);
            }
        });
    }

    public void commandStackChanged(EventObject e) {
        if (isDirty()) {
            if (!savePreviouslyNeeded()) {
                setSavePreviouslyNeeded(true);
                firePropertyChange(IEditorPart.PROP_DIRTY);
            }
        } else {
            setSavePreviouslyNeeded(false);
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
        super.commandStackChanged(e);
    }

    /**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithPalette#configurePaletteViewer()
	 */
    protected void configurePaletteViewer() {
        super.configurePaletteViewer();
        PaletteViewer viewer = (PaletteViewer) getPaletteViewer();
        ContextMenuProvider provider = new PaletteContextMenuProvider(viewer);
        getPaletteViewer().setContextMenu(provider);
        viewer.setCustomizer(new PIMDiagramPaletteCustomizer());
        viewer.setPaletteViewerPreferences(new DiagramPaletteViewerPreferences());
        Control c = viewer.getControl();
        if (c instanceof FigureCanvas) {
            ((Scrollable) c).getVerticalBar().setVisible(false);
            ((Scrollable) c).getHorizontalBar().setVisible(false);
        }
    }

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        ScrollingGraphicalViewer viewer = (ScrollingGraphicalViewer) getGraphicalViewer();
        ScalableFreeformRootEditPart root = new ScalableFreeformRootEditPart();
        getActionRegistry().registerAction(new ZoomInAction(root.getZoomManager()));
        getActionRegistry().registerAction(new ZoomOutAction(root.getZoomManager()));
        viewer.setRootEditPart(root);
        viewer.setEditPartFactory(new PIMEditPartFactory());
        ContextMenuProvider provider = new PIMDiagramContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(provider);
        getSite().registerContextMenu("com.metanology.mde.ui.pimEditor.diagrams.editor.contextmenu", provider, viewer);
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer).setParent(getCommonKeyHandler()));
    }

    public abstract void handleDropTargetEvent(IResource[] data, DropTargetEvent evt);

    protected void createOutputStream(OutputStream os) throws IOException {
        Thread cur = Thread.currentThread();
        ClassLoader save = cur.getContextClassLoader();
        cur.setContextClassLoader(getClass().getClassLoader());
        try {
            PIMDiagramSerializer s = new PIMDiagramSerializer(getPIMDiagram());
            s.writeXml(os);
        } finally {
            cur.setContextClassLoader(save);
        }
    }

    public void dispose() {
        try {
            CopyTemplateAction copy = (CopyTemplateAction) getActionRegistry().getAction(GEFActionConstants.COPY);
            getPaletteViewer().removeSelectionChangedListener(copy);
            getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);
            partListener = null;
            ((FileEditorInput) getEditorInput()).getFile().getWorkspace().removeResourceChangeListener(resourceListener);
            this.removeSelectionChangedListener((PIMTreeView) MDEPlugin.getViewer(PIMTreeView.ID));
        } catch (Exception ignore) {
        } finally {
            super.dispose();
        }
    }

    public void doSave(IProgressMonitor progressMonitor) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                createOutputStream(out);
                IFile file = ((IFileEditorInput) getEditorInput()).getFile();
                if (file.isReadOnly()) {
                    MDEPlugin.showMessage(Messages.get(Messages.MSG_READONLY_FILE_ARG1, file.getLocation().toOSString()));
                } else {
                    file.setContents(new ByteArrayInputStream(out.toByteArray()), true, false, progressMonitor);
                    getCommandStack().flush();
                }
            } finally {
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doSaveAs() {
        performSaveAs();
    }

    public Object getAdapter(Class type) {
        if (type == CommandStackInspectorPage.class) return new CommandStackInspectorPage(getCommandStack());
        if (type == IContentOutlinePage.class) return new OutlinePage(new TreeViewer());
        if (type == ZoomManager.class) return ((ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();
        return super.getAdapter(type);
    }

    /**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithPalette#getInitialPaletteSize()
	 */
    protected int getInitialPaletteSize() {
        return MDEPlugin.getDefault().getPreferenceStore().getInt(PALETTE_SIZE);
    }

    /**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithPalette#handlePaletteResized(int)
	 */
    protected void handlePaletteResized(int newSize) {
        MDEPlugin.getDefault().getPreferenceStore().setValue(PALETTE_SIZE, newSize);
    }

    /**
	 * Returns the KeyHandler with common bindings for both the Outline and Graphical Views.
	 * For example, delete is a common action.
	 */
    protected KeyHandler getCommonKeyHandler() {
        if (sharedKeyHandler == null) {
            sharedKeyHandler = new KeyHandler();
            sharedKeyHandler.put(KeyStroke.getPressed(CHAR_CTRL_D, SWT.CONTROL), getActionRegistry().getAction(PIMDeleteFromPIMAction.ID));
            sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(PIMDeleteFromDiagramAction.ID));
            sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry().getAction(PIMDirectEditAction.ID));
        }
        return sharedKeyHandler;
    }

    protected PIMDiagram getPIMDiagram() {
        return pimDiagram;
    }

    /**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithPalette#getPaletteRoot()
	 */
    protected abstract PaletteRoot getPaletteRoot();

    public void gotoMarker(IMarker marker) {
    }

    protected void hookPaletteViewer() {
        super.hookPaletteViewer();
        final CopyTemplateAction copy = (CopyTemplateAction) getActionRegistry().getAction(GEFActionConstants.COPY);
        getPaletteViewer().addSelectionChangedListener(copy);
        getPaletteViewer().getContextMenu().addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                manager.appendToGroup(GEFActionConstants.GROUP_COPY, copy);
            }
        });
    }

    /**
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#initializeGraphicalViewer()
     */
    protected void initializeGraphicalViewer() {
        if (getPIMDiagram() != null) {
            getPIMDiagram().refreshChildren();
            getGraphicalViewer().setContents(getPIMDiagram());
        }
        getGraphicalViewer().addDropTargetListener(new PIMDiagramTemplateTransferDropTargetListener(getGraphicalViewer()));
        getGraphicalViewer().addDropTargetListener(new PIMDiagramExplorerTransferDropTargetListener(this, getGraphicalViewer()));
    }

    protected void initializePaletteViewer() {
        super.initializePaletteViewer();
        getPaletteViewer().addDragSourceListener(new TemplateTransferDragSourceListener(getPaletteViewer()));
        MDEPlugin.getDefault().getPreferenceStore().setDefault(PALETTE_SIZE, DEFAULT_PALETTE_SIZE);
    }

    protected void createActions() {
        ActionRegistry registry = getActionRegistry();
        IAction action;
        action = new UndoAction(this);
        registry.registerAction(action);
        getStackActions().add(action.getId());
        action = new RedoAction(this);
        registry.registerAction(action);
        getStackActions().add(action.getId());
        action = new SaveAction(this);
        registry.registerAction(action);
        getPropertyActions().add(action.getId());
        action = new DiagramPrintAction(this);
        registry.registerAction(action);
        action = new CopyTemplateAction(this);
        registry.registerAction(action);
        action = new PIMPasteTemplateAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new PIMDirectEditAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new PIMDeleteFromDiagramAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new PIMDeleteFromPIMAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new PIMRefreshRelationAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new PIMExecuteMetaProgramAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new PIMSelectInExplorerAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction(this, PositionConstants.LEFT);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction(this, PositionConstants.RIGHT);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction(this, PositionConstants.TOP);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction(this, PositionConstants.BOTTOM);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction(this, PositionConstants.CENTER);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction(this, PositionConstants.MIDDLE);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
    }

    public boolean isDirty() {
        return isSaveOnCloseNeeded();
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    public boolean isSaveOnCloseNeeded() {
        return getCommandStack().isDirty();
    }

    protected boolean performSaveAs() {
        SaveAsDialog dialog = new SaveAsDialog(getSite().getWorkbenchWindow().getShell());
        IFile originalFile = ((IFileEditorInput) getEditorInput()).getFile();
        dialog.setOriginalFile(originalFile);
        dialog.open();
        IPath path = dialog.getResult();
        if (path == null) return false;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IFile file = workspace.getRoot().getFile(path);
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {

            public void execute(final IProgressMonitor monitor) throws CoreException {
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    createOutputStream(out);
                    file.create(new ByteArrayInputStream(out.toByteArray()), true, monitor);
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            new ProgressMonitorDialog(getSite().getWorkbenchWindow().getShell()).run(false, true, op);
            setInput(new FileEditorInput((IFile) file));
            getCommandStack().markSaveLocation();
        } catch (Exception e) {
            MDEPlugin.showMessage(Messages.get(Messages.ERR_SAVE_FILE_ARG1 + "[" + e.getMessage() + "]", file.getFullPath().toOSString()));
        }
        return true;
    }

    private boolean savePreviouslyNeeded() {
        return savePreviouslyNeeded;
    }

    public void setInput(IEditorInput input) {
        superSetInput(input);
        IFile file = ((IFileEditorInput) input).getFile();
        IResource parent = file.getParent();
        String parentPath = null;
        if (parent != null) {
            IPath path = parent.getFullPath();
            path = path.removeFirstSegments(1);
            parentPath = path.toOSString();
        }
        File localFile = new File(file.getLocation().toOSString());
        if (localFile.exists() && localFile.length() == 0) {
            setPIMDiagram(new PIMDiagram());
            getPIMDiagram().setParentPath(parentPath);
            return;
        }
        try {
            InputStream is = file.getContents(false);
            Thread cur = Thread.currentThread();
            ClassLoader save = cur.getContextClassLoader();
            cur.setContextClassLoader(getClass().getClassLoader());
            PIMDiagram diagram = null;
            try {
                try {
                    diagram = (PIMDiagram) PIMDiagramSerializer.readXml(is);
                } catch (Exception e) {
                    diagram = null;
                } finally {
                    is.close();
                }
                if (diagram == null && Class.forName("java.beans.XMLDecoder") != null) {
                    is = file.getContents(false);
                    java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(is);
                    try {
                        diagram = (PIMDiagram) decoder.readObject();
                    } finally {
                        decoder.close();
                        is.close();
                    }
                }
                if (diagram != null) {
                    setPIMDiagram(diagram);
                    getPIMDiagram().setParentPath(parentPath);
                }
            } finally {
                cur.setContextClassLoader(save);
            }
        } catch (Exception e) {
            String err = "";
            if (e.getMessage() != null) {
                err = "[" + e.getMessage() + "]";
            }
            MDEPlugin.showMessage(Messages.get(Messages.ERR_OPEN_PIM_DIAGRAM_INVALID_FORMAT_ARG1 + err, localFile.getName()));
            setPIMDiagram(new PIMDiagram());
            getPIMDiagram().setParentPath(parentPath);
        }
    }

    protected void setPIMDiagram(PIMDiagram diagram) {
        pimDiagram = diagram;
    }

    private void setSavePreviouslyNeeded(boolean value) {
        savePreviouslyNeeded = value;
    }

    protected void superSetInput(IEditorInput input) {
        if (getEditorInput() != null) {
            IFile file = ((FileEditorInput) getEditorInput()).getFile();
            file.getWorkspace().removeResourceChangeListener(resourceListener);
        }
        super.setInput(input);
        if (getEditorInput() != null) {
            IFile file = ((FileEditorInput) getEditorInput()).getFile();
            file.getWorkspace().addResourceChangeListener(resourceListener);
            String title = getEditorTitle(file);
            setTitle(title);
        }
    }

    protected void setSite(IWorkbenchPartSite site) {
        super.setSite(site);
        getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
    }

    /**
	 * @see java.beans.PropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if ("delete".equalsIgnoreCase(prop)) {
            this.getPIMDiagram().refreshChildren();
        } else {
            Object obj = evt.getNewValue();
            if (obj instanceof MetaObject) {
                DiagramObjectNode node = findNode((MetaObject) obj);
                if (node != null) {
                    node.fireContentChanged();
                }
            }
        }
    }

    private DiagramObjectNode findNode(MetaObject mobj) {
        if (mobj == null) return null;
        String id = mobj.getObjId();
        List children = this.getPIMDiagram().getChildren();
        if (children != null && children.size() > 0) {
            for (Iterator i = children.iterator(); i.hasNext(); ) {
                Object node = i.next();
                MetaObject nodeMetaObj = null;
                if (mobj instanceof Package && node instanceof PackageNode) {
                    nodeMetaObj = ((PackageNode) node).getMpackage();
                } else if (mobj instanceof MetaClass && node instanceof ClassNode) {
                    nodeMetaObj = ((ClassNode) node).getMclass();
                } else if (mobj instanceof Subsystem && node instanceof SubsystemNode) {
                    nodeMetaObj = ((SubsystemNode) node).getMsubsystem();
                } else if (mobj instanceof Component && node instanceof ComponentNode) {
                    nodeMetaObj = ((ComponentNode) node).getMcomponent();
                }
                if (nodeMetaObj != null && mobj.equals(nodeMetaObj)) {
                    return (DiagramObjectNode) node;
                }
            }
        }
        return null;
    }

    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        this.getGraphicalViewer().addSelectionChangedListener(listener);
    }

    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        this.getGraphicalViewer().removeSelectionChangedListener(listener);
    }

    protected void createPaletteViewer(Composite parent) {
        PaletteViewer viewer = new PaletteViewer();
        viewer.createControl(parent);
        setPaletteViewer(viewer);
        configurePaletteViewer();
        hookPaletteViewer();
        initializePaletteViewer();
    }

    public void createPartControl(Composite parent) {
        Splitter splitter = new Splitter(parent, SWT.HORIZONTAL);
        createPaletteViewer(splitter);
        createGraphicalViewer(splitter);
        splitter.maintainSize(getPaletteViewer().getControl());
        splitter.setFixedSize(getInitialPaletteSize() - 1);
        splitter.setFixedSize(getInitialPaletteSize());
        splitter.addFixedSizeChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                handlePaletteResized(((Splitter) evt.getSource()).getFixedSize());
            }
        });
    }
}
