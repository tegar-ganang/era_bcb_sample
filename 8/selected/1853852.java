package org.gems.designer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.draw2d.parts.Thumbnail;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.AlignmentAction;
import org.eclipse.gef.ui.actions.CopyTemplateAction;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.MatchHeightAction;
import org.eclipse.gef.ui.actions.MatchWidthAction;
import org.eclipse.gef.ui.actions.ToggleGridAction;
import org.eclipse.gef.ui.actions.ToggleRulerVisibilityAction;
import org.eclipse.gef.ui.actions.ToggleSnapToGeometryAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.gef.ui.rulers.RulerComposite;
import org.eclipse.gef.ui.stackview.CommandStackInspectorPage;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.gems.designer.actions.InterpretAction;
import org.gems.designer.actions.LogicPasteTemplateAction;
import org.gems.designer.dnd.ModelTemplateTransferDropTargetListener;
import org.gems.designer.dnd.TextTransferDropTargetListener;
import org.gems.designer.edit.GraphicalPartFactory;
import org.gems.designer.edit.LogicEditPart;
import org.gems.designer.edit.TreePartFactory;
import org.gems.designer.metamodel.MetaModelProvider;
import org.gems.designer.model.Container;
import org.gems.designer.model.LogicElement;
import org.gems.designer.model.LogicRuler;
import org.gems.designer.model.ModelObject;
import org.gems.designer.model.Root;
import org.gems.designer.model.actions.ActionSet;
import org.gems.designer.model.actions.ContextAwareActionSet;
import org.gems.designer.model.actions.CoreConstraintSet;
import org.gems.designer.model.actions.EventInterestFactory;
import org.gems.designer.model.actions.EventInterestFactoryRepository;
import org.gems.designer.model.actions.ModelActionRegistry;
import org.gems.designer.model.actions.ModelEventInterest;
import org.gems.designer.model.actions.PersistentModelAction;
import org.gems.designer.model.actions.PersistentModelEventInterest;
import org.gems.designer.model.event.ElementGroupChangeEvent;
import org.gems.designer.model.event.ModelChangeEvent;
import org.gems.designer.model.event.ModelEventDispatcher;
import org.gems.designer.palette.LogicPaletteCustomizer;
import org.gems.designer.remoting.AsynchronousModelUpdater;
import org.gems.designer.rulers.LogicRulerProvider;

public class GemsEditor extends GraphicalEditorWithFlyoutPalette implements InstanceListener {

    private class GemsObjectInputStream extends ObjectInputStream {

        private ClassLoader classLoader_;

        public GemsObjectInputStream(InputStream in, ClassLoader cl) throws IOException {
            super(in);
            classLoader_ = cl;
        }

        protected Class resolveClass(ObjectStreamClass desc) throws java.io.IOException, ClassNotFoundException {
            String name = desc.getName();
            try {
                return Class.forName(name, false, classLoader_);
            } catch (Exception e) {
                return super.resolveClass(desc);
            }
        }
    }

    class OutlinePage extends ContentOutlinePage implements IAdaptable {

        private PageBook pageBook;

        private Control outline;

        private Canvas overview;

        private IAction showOutlineAction, showOverviewAction;

        static final int ID_OUTLINE = 0;

        static final int ID_OVERVIEW = 1;

        private Thumbnail thumbnail;

        private DisposeListener disposeListener;

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
            id = IWorkbenchActionConstants.DELETE;
            bars.setGlobalActionHandler(id, registry.getAction(id));
            bars.updateActionBars();
            AsynchronousModelUpdater.initInstance(Display.getCurrent());
        }

        protected void configureOutlineViewer() {
            getViewer().setEditDomain(getEditDomain());
            getViewer().setEditPartFactory(new TreePartFactory());
            ContextMenuProvider provider = new LogicContextMenuProvider(getViewer(), getActionRegistry());
            getViewer().setContextMenu(provider);
            getSite().registerContextMenu("org.eclipse.gef.examples.logic.outline.contextmenu", provider, getSite().getSelectionProvider());
            getViewer().setKeyHandler(getCommonKeyHandler());
            getViewer().addDropTargetListener(new ModelTemplateTransferDropTargetListener(getViewer()));
            IToolBarManager tbm = getSite().getActionBars().getToolBarManager();
            showOutlineAction = new Action() {

                public void run() {
                    showPage(ID_OUTLINE);
                }
            };
            showOutlineAction.setImageDescriptor(ImageDescriptor.createFromFile(GemsPlugin.class, "icons/outline.gif"));
            tbm.add(showOutlineAction);
            showOverviewAction = new Action() {

                public void run() {
                    showPage(ID_OVERVIEW);
                }
            };
            showOverviewAction.setImageDescriptor(ImageDescriptor.createFromFile(GemsPlugin.class, "icons/overview.gif"));
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
            if (thumbnail != null) {
                thumbnail.deactivate();
                thumbnail = null;
            }
            super.dispose();
            GemsEditor.this.outlinePage = null;
        }

        public Object getAdapter(Class type) {
            if (type == ZoomManager.class) return getGraphicalViewer().getProperty(ZoomManager.class.toString());
            return null;
        }

        public Control getControl() {
            return pageBook;
        }

        protected void hookOutlineViewer() {
            getSelectionSynchronizer().addViewer(getViewer());
        }

        protected void initializeOutlineViewer() {
            setContents(getLogicDiagram());
        }

        protected void initializeOverview() {
            LightweightSystem lws = new LightweightSystem(overview);
            RootEditPart rep = getGraphicalViewer().getRootEditPart();
            if (rep instanceof ScalableFreeformRootEditPart) {
                ScalableFreeformRootEditPart root = (ScalableFreeformRootEditPart) rep;
                thumbnail = new ScrollableThumbnail((Viewport) root.getFigure());
                thumbnail.setBorder(new MarginBorder(3));
                thumbnail.setSource(root.getLayer(LayerConstants.PRINTABLE_LAYERS));
                lws.setContents(thumbnail);
                disposeListener = new DisposeListener() {

                    public void widgetDisposed(DisposeEvent e) {
                        if (thumbnail != null) {
                            thumbnail.deactivate();
                            thumbnail = null;
                        }
                    }
                };
                getEditor().addDisposeListener(disposeListener);
            }
        }

        public void setContents(Object contents) {
            getViewer().setContents(contents);
        }

        protected void showPage(int id) {
            if (id == ID_OUTLINE) {
                showOutlineAction.setChecked(true);
                showOverviewAction.setChecked(false);
                pageBook.showPage(outline);
                if (thumbnail != null) thumbnail.setVisible(false);
            } else if (id == ID_OVERVIEW) {
                if (thumbnail == null) initializeOverview();
                showOutlineAction.setChecked(false);
                showOverviewAction.setChecked(true);
                pageBook.showPage(overview);
                thumbnail.setVisible(true);
            }
        }

        protected void unhookOutlineViewer() {
            getSelectionSynchronizer().removeViewer(getViewer());
            if (disposeListener != null && getEditor() != null && !getEditor().isDisposed()) getEditor().removeDisposeListener(disposeListener);
        }
    }

    private KeyHandler sharedKeyHandler;

    protected PaletteRoot root;

    private OutlinePage outlinePage;

    protected boolean editorSaving = false;

    class ResourceTracker implements IResourceChangeListener, IResourceDeltaVisitor {

        public void resourceChanged(IResourceChangeEvent event) {
            IResourceDelta delta = event.getDelta();
            try {
                if (delta != null) delta.accept(this);
            } catch (CoreException exception) {
            }
        }

        public boolean visit(IResourceDelta delta) {
            if (delta == null || !delta.getResource().equals(((FileEditorInput) getEditorInput()).getFile())) return true;
            if (delta.getKind() == IResourceDelta.REMOVED) {
                Display display = getSite().getShell().getDisplay();
                if ((IResourceDelta.MOVED_TO & delta.getFlags()) == 0) {
                    display.asyncExec(new Runnable() {

                        public void run() {
                            if (!isDirty()) closeEditor(false);
                        }
                    });
                } else {
                    final IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(delta.getMovedToPath());
                    display.asyncExec(new Runnable() {

                        public void run() {
                            superSetInput(new FileEditorInput(newFile));
                        }
                    });
                }
            } else if (delta.getKind() == IResourceDelta.CHANGED) {
                if (!editorSaving) {
                    final IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(delta.getFullPath());
                    Display display = getSite().getShell().getDisplay();
                    display.asyncExec(new Runnable() {

                        public void run() {
                            setInput(new FileEditorInput(newFile));
                            getCommandStack().flush();
                        }
                    });
                }
            }
            return false;
        }
    }

    private IPartListener partListener = new IPartListener() {

        public void partActivated(IWorkbenchPart part) {
            if (part != GemsEditor.this) return;
            if (!((FileEditorInput) getEditorInput()).getFile().exists()) {
                Shell shell = getSite().getShell();
                String title = LogicMessages.GraphicalEditor_FILE_DELETED_TITLE_UI;
                String message = LogicMessages.GraphicalEditor_FILE_DELETED_WITHOUT_SAVE_INFO;
                String[] buttons = { LogicMessages.GraphicalEditor_SAVE_BUTTON_UI, LogicMessages.GraphicalEditor_CLOSE_BUTTON_UI };
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
        }

        public void partDeactivated(IWorkbenchPart part) {
        }

        public void partOpened(IWorkbenchPart part) {
        }
    };

    private Root logicDiagram = new Root();

    private Container currentRootObject_ = null;

    private boolean savePreviouslyNeeded = false;

    private ResourceTracker resourceListener = new ResourceTracker();

    private RulerComposite rulerComp;

    protected static final String PALETTE_DOCK_LOCATION = "Dock location";

    protected static final String PALETTE_SIZE = "Palette Size";

    protected static final String PALETTE_STATE = "Palette state";

    protected static final int DEFAULT_PALETTE_SIZE = 130;

    public GemsEditor() {
        setEditDomain(new DefaultEditDomain(this));
        logicDiagram.setConnectionRouter(Container.ROUTER_MANHATTAN);
        logicDiagram.setModelID(getModelID());
        ModelRepository.getInstance().getInstanceRepository().getInstance(logicDiagram.getModelInstanceID()).addRootElement(logicDiagram);
    }

    public void resetRoot(Container c) {
        currentRootObject_ = c;
        getGraphicalViewer().setContents(c);
    }

    public void resetRoot(EditPart c) {
        currentRootObject_ = (Container) ((LogicEditPart) c).getModel();
        getGraphicalViewer().setContents(c);
        c.refresh();
    }

    public Container getCurrentRootObject() {
        if (currentRootObject_ == null) currentRootObject_ = logicDiagram;
        return currentRootObject_;
    }

    protected void closeEditor(boolean save) {
        getSite().getPage().closeEditor(GemsEditor.this, save);
    }

    protected String getModelID() {
        return MetaModelProvider.MODEL_ID;
    }

    public void commandStackChanged(EventObject event) {
        if (isDirty()) {
            if (!savePreviouslyNeeded()) {
                setSavePreviouslyNeeded(true);
                firePropertyChange(IEditorPart.PROP_DIRTY);
            }
        } else {
            setSavePreviouslyNeeded(false);
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
        super.commandStackChanged(event);
    }

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        ScrollingGraphicalViewer viewer = (ScrollingGraphicalViewer) getGraphicalViewer();
        ScalableFreeformRootEditPart root = new ScalableFreeformRootEditPart();
        List zoomLevels = new ArrayList(3);
        zoomLevels.add(ZoomManager.FIT_ALL);
        zoomLevels.add(ZoomManager.FIT_WIDTH);
        zoomLevels.add(ZoomManager.FIT_HEIGHT);
        root.getZoomManager().setZoomLevelContributions(zoomLevels);
        IAction zoomIn = new ZoomInAction(root.getZoomManager());
        IAction zoomOut = new ZoomOutAction(root.getZoomManager());
        getActionRegistry().registerAction(zoomIn);
        getActionRegistry().registerAction(zoomOut);
        getSite().getKeyBindingService().registerAction(zoomIn);
        getSite().getKeyBindingService().registerAction(zoomOut);
        viewer.setRootEditPart(root);
        viewer.setEditPartFactory(new GraphicalPartFactory());
        ContextMenuProvider provider = new LogicContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(provider);
        getSite().registerContextMenu("org.eclipse.gef.examples.logic.editor.contextmenu", provider, viewer);
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer).setParent(getCommonKeyHandler()));
        loadProperties();
        IAction showRulers = new ToggleRulerVisibilityAction(getGraphicalViewer());
        getActionRegistry().registerAction(showRulers);
        IAction snapAction = new ToggleSnapToGeometryAction(getGraphicalViewer());
        getActionRegistry().registerAction(snapAction);
        IAction showGrid = new ToggleGridAction(getGraphicalViewer());
        getActionRegistry().registerAction(showGrid);
        Listener listener = new Listener() {

            public void handleEvent(Event event) {
                handleActivationChanged(event);
            }
        };
        getGraphicalControl().addListener(SWT.Activate, listener);
        getGraphicalControl().addListener(SWT.Deactivate, listener);
    }

    protected void saveTo(IFile file, String filext) throws Exception {
        ModelInstance tosave = ModelRepository.getInstance().getInstanceRepository().getInstance(getLogicDiagram().getModelInstanceID());
        tosave.removeInstanceListener(this);
        tosave.prepareForSerialization();
        if (tosave.getRoot() == null) tosave.setRoot(logicDiagram);
        Serializers serials = Serializers.getSerializers(tosave.getModelID());
        ModelSerializer serial = serials.getSerializerByFileExtension(filext);
        ModelPostprocessor[] post = PluginUtilities.getPostprocessors(logicDiagram.getModelID());
        for (ModelPostprocessor p : post) {
            p.process(tosave);
        }
        serial.serializeModel(tosave, file, null);
        configureEventListeners(tosave);
    }

    protected CustomPalettePage createPalettePage() {
        return new CustomPalettePage(getPaletteViewerProvider()) {

            public void init(IPageSite pageSite) {
                super.init(pageSite);
                IAction copy = getActionRegistry().getAction(ActionFactory.COPY.getId());
                pageSite.getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), copy);
            }
        };
    }

    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new PaletteViewerProvider(getEditDomain()) {

            private IMenuListener menuListener;

            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
                viewer.setCustomizer(new LogicPaletteCustomizer());
                viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
            }

            protected void hookPaletteViewer(PaletteViewer viewer) {
                super.hookPaletteViewer(viewer);
                final CopyTemplateAction copy = (CopyTemplateAction) getActionRegistry().getAction(ActionFactory.COPY.getId());
                viewer.addSelectionChangedListener(copy);
                if (menuListener == null) menuListener = new IMenuListener() {

                    public void menuAboutToShow(IMenuManager manager) {
                        manager.appendToGroup(GEFActionConstants.GROUP_COPY, copy);
                    }
                };
                viewer.getContextMenu().addMenuListener(menuListener);
            }
        };
    }

    public void dispose() {
        ModelInstance inst = ModelRepository.getInstance().getInstanceRepository().getInstance(logicDiagram.getModelInstanceID());
        ModelAccessListener[] list = PluginUtilities.getModelAccessListeners(logicDiagram.getModelID());
        if (list != null) {
            for (ModelAccessListener l : list) l.modelClosed(this, inst);
        }
        getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);
        partListener = null;
        ((FileEditorInput) getEditorInput()).getFile().getWorkspace().removeResourceChangeListener(resourceListener);
        super.dispose();
    }

    public void doSave(IProgressMonitor progressMonitor) {
        try {
            editorSaving = true;
            saveProperties();
            IFile file = ((IFileEditorInput) getEditorInput()).getFile();
            saveTo(file, file.getFileExtension());
            getCommandStack().markSaveLocation();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            editorSaving = false;
        }
    }

    public void doSaveAs() {
        performSaveAs();
    }

    public Object getAdapter(Class type) {
        if (type == CommandStackInspectorPage.class) return new CommandStackInspectorPage(getCommandStack());
        if (type == IContentOutlinePage.class) {
            outlinePage = new OutlinePage(new TreeViewer());
            return outlinePage;
        }
        if (type == ZoomManager.class) return getGraphicalViewer().getProperty(ZoomManager.class.toString());
        return super.getAdapter(type);
    }

    protected Control getGraphicalControl() {
        return rulerComp;
    }

    /**
     * Returns the KeyHandler with common bindings for both the Outline and
     * Graphical Views. For example, delete is a common action.
     */
    protected KeyHandler getCommonKeyHandler() {
        if (sharedKeyHandler == null) {
            sharedKeyHandler = new KeyHandler();
            sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(GEFActionConstants.DELETE));
            sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry().getAction(GEFActionConstants.DIRECT_EDIT));
        }
        return sharedKeyHandler;
    }

    public Root getLogicDiagram() {
        return logicDiagram;
    }

    protected FlyoutPreferences getPalettePreferences() {
        return new FlyoutPreferences() {

            public int getDockLocation() {
                return GemsPlugin.getDefault().getPreferenceStore().getInt(PALETTE_DOCK_LOCATION);
            }

            public int getPaletteState() {
                return GemsPlugin.getDefault().getPreferenceStore().getInt(PALETTE_STATE);
            }

            public int getPaletteWidth() {
                return GemsPlugin.getDefault().getPreferenceStore().getInt(PALETTE_SIZE);
            }

            public void setDockLocation(int location) {
                GemsPlugin.getDefault().getPreferenceStore().setValue(PALETTE_DOCK_LOCATION, location);
            }

            public void setPaletteState(int state) {
                GemsPlugin.getDefault().getPreferenceStore().setValue(PALETTE_STATE, state);
            }

            public void setPaletteWidth(int width) {
                GemsPlugin.getDefault().getPreferenceStore().setValue(PALETTE_SIZE, width);
            }
        };
    }

    protected void configurePaletteRoot(ModelInstance inst) {
        PaletteRoot root = getPaletteRoot();
        Root r = inst.getRoot();
        ModelProvider mp = r.getModelProvider();
        mp.getPaletteProvider().configurePalette(root, inst);
    }

    protected PaletteRoot getPaletteRoot() {
        if (root == null) {
            root = GemsPlugin.createPalette();
        }
        return root;
    }

    public void gotoMarker(IMarker marker) {
    }

    protected void handleActivationChanged(Event event) {
        IAction copy = null;
        if (event.type == SWT.Deactivate) copy = getActionRegistry().getAction(ActionFactory.COPY.getId());
        if (getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()) != copy) {
            getEditorSite().getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), copy);
            getEditorSite().getActionBars().updateActionBars();
        }
    }

    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        getGraphicalViewer().setContents(getLogicDiagram());
        getGraphicalViewer().addDropTargetListener(new ModelTemplateTransferDropTargetListener(getGraphicalViewer()));
        getGraphicalViewer().addDropTargetListener(new TextTransferDropTargetListener(getGraphicalViewer(), TextTransfer.getInstance()));
    }

    protected void createActions() {
        super.createActions();
        ActionRegistry registry = getActionRegistry();
        IAction action;
        action = new InterpretAction(this);
        registry.registerAction(action);
        action = new CopyTemplateAction(this);
        registry.registerAction(action);
        action = new MatchWidthAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new MatchHeightAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new LogicPasteTemplateAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new DirectEditAction((IWorkbenchPart) this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.LEFT);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.RIGHT);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.TOP);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.BOTTOM);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.CENTER);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.MIDDLE);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
    }

    protected void createGraphicalViewer(Composite parent) {
        rulerComp = new RulerComposite(parent, SWT.NONE);
        super.createGraphicalViewer(rulerComp);
        rulerComp.setGraphicalViewer((ScrollingGraphicalViewer) getGraphicalViewer());
    }

    protected FigureCanvas getEditor() {
        return (FigureCanvas) getGraphicalViewer().getControl();
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

    protected void loadProperties() {
        LogicRuler ruler = getLogicDiagram().getRuler(PositionConstants.WEST);
        RulerProvider provider = null;
        if (ruler != null) {
            provider = new LogicRulerProvider(ruler);
        }
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_VERTICAL_RULER, provider);
        ruler = getLogicDiagram().getRuler(PositionConstants.NORTH);
        provider = null;
        if (ruler != null) {
            provider = new LogicRulerProvider(ruler);
        }
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_HORIZONTAL_RULER, provider);
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_RULER_VISIBILITY, new Boolean(getLogicDiagram().getRulerVisibility()));
        getGraphicalViewer().setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, new Boolean(getLogicDiagram().isSnapToGeometryEnabled()));
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, new Boolean(getLogicDiagram().isGridEnabled()));
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, new Boolean(getLogicDiagram().isGridEnabled()));
        ZoomManager manager = (ZoomManager) getGraphicalViewer().getProperty(ZoomManager.class.toString());
        if (manager != null) manager.setZoom(getLogicDiagram().getZoom());
    }

    protected boolean performSaveAs() {
        SaveAsDialog dialog = new SaveAsDialog(getSite().getWorkbenchWindow().getShell());
        dialog.setOriginalFile(((IFileEditorInput) getEditorInput()).getFile());
        dialog.open();
        IPath path = dialog.getResult();
        if (path == null) return false;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IFile file = workspace.getRoot().getFile(path);
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {

            public void execute(final IProgressMonitor monitor) {
                saveProperties();
                try {
                    saveTo(file, file.getFileExtension());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            new ProgressMonitorDialog(getSite().getWorkbenchWindow().getShell()).run(false, true, op);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            superSetInput(new FileEditorInput(file));
            getCommandStack().markSaveLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean savePreviouslyNeeded() {
        return savePreviouslyNeeded;
    }

    protected void saveProperties() {
        getLogicDiagram().setRulerVisibility(((Boolean) getGraphicalViewer().getProperty(RulerProvider.PROPERTY_RULER_VISIBILITY)).booleanValue());
        getLogicDiagram().setGridEnabled(((Boolean) getGraphicalViewer().getProperty(SnapToGrid.PROPERTY_GRID_ENABLED)).booleanValue());
        getLogicDiagram().setSnapToGeometry(((Boolean) getGraphicalViewer().getProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED)).booleanValue());
        ZoomManager manager = (ZoomManager) getGraphicalViewer().getProperty(ZoomManager.class.toString());
        if (manager != null) getLogicDiagram().setZoom(manager.getZoom());
    }

    protected ClassLoader getClassLoaderForSerialization() {
        return getClass().getClassLoader();
    }

    protected ObjectInputStream getInputStream(IEditorInput input) {
        try {
            IFile file = ((IFileEditorInput) input).getFile();
            InputStream is = file.getContents(false);
            ObjectInputStream ois = new GemsObjectInputStream(is, getClassLoaderForSerialization());
            return ois;
        } catch (Exception e) {
        }
        return null;
    }

    protected ModelInstance readModelInstance(IEditorInput input) {
        try {
            IFile file = ((IFileEditorInput) input).getFile();
            Serializers serials = Serializers.getReaders();
            ModelSerializer serial = serials.getSerializerByFileExtension(file.getFileExtension());
            return serial.readModel(file, getClassLoaderForSerialization(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setInput(IEditorInput input) {
        superSetInput(input);
        try {
            ModelEventDispatcher.setEventsEnabled(false);
            Object obj = readModelInstance(input);
            ModelEventDispatcher.setEventsEnabled(true);
            Root c = null;
            if (obj instanceof String) {
            }
            if (obj instanceof Container && !(obj instanceof Root)) {
                obj = convertToRoot((Container) obj);
            }
            if (obj instanceof Container) {
                c = (Root) obj;
                c.setModelInstanceID(logicDiagram.getModelInstanceID());
            } else if (obj instanceof ModelInstance) {
                ModelInstance inst = (ModelInstance) obj;
                Container croot = null;
                if (inst.getRoot() != null) croot = inst.getRoot(); else if (inst.getRootElements().length > 0) croot = (Container) inst.getRootElements()[0];
                if (!(croot instanceof Root)) croot = convertToRoot(croot);
                c = (Root) croot;
                InstanceRepository repo = ModelRepository.getInstance().getInstanceRepository();
                repo.registerInstance(c.getModelInstanceID(), inst);
            }
            if (c.getModelID() == null) c.setModelID(getModelID());
            ModelInstance instance = ModelRepository.getInstance().getInstanceRepository().getInstance(c.getModelInstanceID());
            IFile file = ((FileEditorInput) getEditorInput()).getFile();
            ModelRepository.getInstance().getInstanceRepository().setFile(c.getModelInstanceID(), file);
            ModelPreprocessor[] pre = PluginUtilities.getPreprocessors(c.getModelID());
            for (ModelPreprocessor p : pre) {
                p.process(instance);
            }
            configureEventListeners(instance);
            configurePaletteRoot(instance);
            setLogicDiagram(c);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!editorSaving) {
            if (getGraphicalViewer() != null) {
                getGraphicalViewer().setContents(getLogicDiagram());
                loadProperties();
            }
            if (outlinePage != null) {
                outlinePage.setContents(getLogicDiagram());
            }
        }
        ModelInstance inst = ModelRepository.getInstance().getInstanceRepository().getInstance(logicDiagram.getModelInstanceID());
        ModelRepository.getInstance().getInstanceRepository().setEditor(inst, this);
        ModelAccessListener[] list = PluginUtilities.getModelAccessListeners(logicDiagram.getModelID());
        if (list != null) {
            for (ModelAccessListener l : list) l.modelOpened(this, inst);
        }
    }

    protected Root convertToRoot(Container con) {
        Root root = new Root();
        List children = con.getChildren();
        root.setName(con.getName());
        root.setModelID(con.getModelID());
        root.setModelInstanceID(con.getModelInstanceID());
        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            con.removeChild((LogicElement) child);
            root.addChild((LogicElement) child);
        }
        return root;
    }

    protected void configureEventListeners(ModelInstance instance) {
        ActionSet[] actions = PluginUtilities.getActionSetsForModel(instance.getModelID());
        ModelActionRegistry actionregistry = new ModelActionRegistry();
        for (int i = 0; i < actions.length; i++) {
            if (actions[i] instanceof PersistentModelAction) continue;
            if (actions[i] instanceof ContextAwareActionSet) ((ContextAwareActionSet) actions[i]).setModelContext(instance);
            if (actions[i] instanceof EditorAware) ((EditorAware) actions[i]).setEditor(this);
            ModelEventInterest[] interests = actions[i].getEventInterests();
            for (int j = 0; j < interests.length; j++) actionregistry.addInterest(interests[j]);
        }
        ModelProvider provider = ModelRepository.getInstance().getModelProvider(instance.getModelID());
        ConstraintsChecker checker = provider.getConstraintsChecker();
        CoreConstraintSet corecons = new CoreConstraintSet();
        List<Memento> mems = checker.getConstraintMementos();
        for (Memento mem : mems) {
            try {
                EventInterestFactory fact = EventInterestFactoryRepository.getInstance().getFactoryByID(mem.getId());
                if (fact != null) {
                    PersistentModelEventInterest pei = fact.loadInterest(mem);
                    if (pei != null) corecons.addInterest(pei);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        instance.addInstanceListener(corecons);
        instance.addInstanceListener(actionregistry);
        instance.addInstanceListener(this);
    }

    public void setLogicDiagram(Root diagram) {
        if (diagram.getModelID() == null) diagram.setModelID(getModelID());
        logicDiagram = diagram;
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
        logicDiagram.setModelInstanceID(logicDiagram.getModelInstanceID());
        if (getEditorInput() != null) {
            IFile file = ((FileEditorInput) getEditorInput()).getFile();
            file.getWorkspace().addResourceChangeListener(resourceListener);
            setTitle(file.getName());
        }
    }

    protected void setSite(IWorkbenchPartSite site) {
        super.setSite(site);
        getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
    }

    public void instanceChanged(String e, ModelChangeEvent mce) {
        if (mce instanceof ElementGroupChangeEvent) {
            System.out.println("Reconfigure palette for elementgroup change");
            configurePaletteRoot(((ElementGroupChangeEvent) mce).getInstance());
        }
    }
}
