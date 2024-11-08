package org.plazmaforge.studio.dbdesigner.editor;

import java.io.*;
import java.util.*;
import java.util.List;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.draw2d.parts.Thumbnail;
import org.eclipse.gef.*;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.actions.*;
import org.eclipse.gef.ui.parts.*;
import org.eclipse.gef.ui.stackview.CommandStackInspectorPage;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.*;
import org.plazmaforge.studio.core.dialogs.IDialogCommand;
import org.plazmaforge.studio.core.editors.EditorInputFactory;
import org.plazmaforge.studio.dbdesigner.DBDesignerPlugin;
import org.plazmaforge.studio.dbdesigner.ERDConstants;
import org.plazmaforge.studio.dbdesigner.actions.AutoLayoutAction;
import org.plazmaforge.studio.dbdesigner.actions.GridLayoutAction;
import org.plazmaforge.studio.dbdesigner.actions.ResetRoutingAction;
import org.plazmaforge.studio.dbdesigner.actions.ShowTablesAction;
import org.plazmaforge.studio.dbdesigner.actions.ToggleGridVisibilityAction;
import org.plazmaforge.studio.dbdesigner.actions.ToggleLabelVisibilitAction;
import org.plazmaforge.studio.dbdesigner.actions.ToggleRoutingTypeAction;
import org.plazmaforge.studio.dbdesigner.actions.ToggleSnapToGridAction;
import org.plazmaforge.studio.dbdesigner.actions.ToggleTableDetailAction;
import org.plazmaforge.studio.dbdesigner.dialogs.NoteEditDialog;
import org.plazmaforge.studio.dbdesigner.dialogs.RelationshipEditDialog;
import org.plazmaforge.studio.dbdesigner.dialogs.TableEditDialog;
import org.plazmaforge.studio.dbdesigner.dialogs.ViewEditDialog;
import org.plazmaforge.studio.dbdesigner.editor.creator.ERDiagramPalleteCreator;
import org.plazmaforge.studio.dbdesigner.model.ERColumn;
import org.plazmaforge.studio.dbdesigner.model.ERDiagram;
import org.plazmaforge.studio.dbdesigner.model.ERNoteNode;
import org.plazmaforge.studio.dbdesigner.model.ERRelationship;
import org.plazmaforge.studio.dbdesigner.model.ERTableNode;
import org.plazmaforge.studio.dbdesigner.model.ERViewNode;
import org.plazmaforge.studio.dbdesigner.model.factory.ERDTreePartFactory;
import org.plazmaforge.studio.dbdesigner.model.factory.ERPartFactory;
import org.plazmaforge.studio.dbdesigner.parts.ERColumnEditPart;
import org.plazmaforge.studio.dbdesigner.parts.ERDScalableFreeformRootEditPart;
import org.plazmaforge.studio.dbdesigner.parts.ERDScrollingGraphicalViewer;
import org.plazmaforge.studio.dbdesigner.parts.ERNoteNodeEditPart;
import org.plazmaforge.studio.dbdesigner.parts.ERRelationshipEditPart;
import org.plazmaforge.studio.dbdesigner.parts.ERTableNodeEditPart;
import org.plazmaforge.studio.dbdesigner.parts.ERTreeColumnEditPart;
import org.plazmaforge.studio.dbdesigner.parts.ERViewNodeEditPart;
import org.plazmaforge.studio.dbdesigner.storage.DiagramFileManager;

public class ERDesignerEditor extends GraphicalEditorWithFlyoutPalette {

    public static String ID = "org.plazmaforge.studio.dbdesigner.editor.ERDesignerEditor";

    public static final int GRID_LAYOUT = 0;

    public static final int ORTHOGONAL_LAYOUT = 1;

    public static final int ORGANIC_LAYOUT = 2;

    private ERDiagram diagram;

    private boolean editorSaving;

    private KeyHandler sharedKeyHandler;

    private Object file;

    private boolean savePreviouslyNeeded;

    public boolean actionActivity;

    private boolean reLayout;

    private int layoutMode;

    private SelectionSynchronizer synchronizer;

    private OutlinePage outlinePage;

    private ERDScalableFreeformRootEditPart root;

    private ResourceTracker resourceListener;

    private ERDPartListener activationListener;

    private ERDPartListener2 projectListener;

    private ERDPreferenceChangeListener preferenceListener;

    private IPreferenceStore preferenceStore;

    private boolean creatingNewDiagram;

    public static interface IContentOverviewPage extends IPage, ISelectionProvider {
    }

    private PaletteRoot paletteRoot;

    public PaletteRoot getPaletteRoot() {
        if (paletteRoot == null) {
            paletteRoot = (new ERDiagramPalleteCreator()).createPaletteRoot();
        }
        return paletteRoot;
    }

    class OutlinePage extends ContentOutlinePage implements IAdaptable {

        private PageBook pageBook;

        private Control outline;

        public OutlinePage(EditPartViewer editpartviewer) {
            super(editpartviewer);
        }

        public void setActionBars(IActionBars iactionbars) {
            super.setActionBars(iactionbars);
        }

        protected void configureOutlineViewer() {
            getViewer().setEditDomain(getEditDomain());
            getViewer().setEditPartFactory(new ERDTreePartFactory());
            getViewer().setKeyHandler(getCommonKeyHandler());
        }

        public void createControl(Composite composite) {
            pageBook = new PageBook(composite, 0);
            outline = getViewer().createControl(pageBook);
            pageBook.showPage(outline);
            configureOutlineViewer();
            hookOutlineViewer();
            initializeOutlineViewer();
        }

        public void dispose() {
            unhookOutlineViewer();
            super.dispose();
            outlinePage = null;
        }

        public Object getAdapter(Class class1) {
            return null;
        }

        public Control getControl() {
            return pageBook;
        }

        protected void hookOutlineViewer() {
            getSelectionSynchronizer().addViewer(getViewer());
        }

        protected void initializeOutlineViewer() {
            setContents(diagram);
        }

        public void setContents(Object obj) {
            getViewer().setContents(obj);
        }

        protected void unhookOutlineViewer() {
            getSelectionSynchronizer().removeViewer(getViewer());
        }
    }

    public class OverviewPage extends ContentOutlinePage implements IContentOverviewPage, IAdaptable {

        private PageBook pageBook;

        private Control outline;

        private Canvas overview;

        private Thumbnail thumbnail;

        private DisposeListener disposeListener;

        public OverviewPage(EditPartViewer editpartviewer) {
            super(editpartviewer);
        }

        protected void configureOutlineViewer() {
            getViewer().setEditDomain(getEditDomain());
            getViewer().setKeyHandler(getCommonKeyHandler());
            if (thumbnail == null) {
                initializeOverview();
            }
            thumbnail.setVisible(true);
        }

        public void createControl(Composite composite) {
            pageBook = new PageBook(composite, 0);
            outline = getViewer().createControl(pageBook);
            overview = new Canvas(pageBook, 0);
            pageBook.showPage(overview);
            configureOutlineViewer();
        }

        public void dispose() {
            if (disposeListener != null && getEditor() != null && !getEditor().isDisposed()) {
                getEditor().removeDisposeListener(disposeListener);
            }
            if (thumbnail != null) {
                thumbnail.deactivate();
                thumbnail = null;
            }
            super.dispose();
        }

        public Control getControl() {
            return pageBook;
        }

        protected void initializeOverview() {
            LightweightSystem lightweightsystem = new LightweightSystem(overview);
            org.eclipse.gef.RootEditPart rooteditpart = getGraphicalViewer().getRootEditPart();
            if (rooteditpart instanceof ScalableFreeformRootEditPart) {
                ScalableFreeformRootEditPart scalablefreeformrooteditpart = (ScalableFreeformRootEditPart) rooteditpart;
                thumbnail = new ScrollableThumbnail((Viewport) scalablefreeformrooteditpart.getFigure());
                thumbnail.setBorder(new MarginBorder(3));
                thumbnail.setSource(scalablefreeformrooteditpart.getLayer("Printable Layers"));
                lightweightsystem.setContents(thumbnail);
                disposeListener = new DisposeListener() {

                    public void widgetDisposed(DisposeEvent disposeevent) {
                        if (thumbnail != null) {
                            thumbnail.deactivate();
                            thumbnail = null;
                        }
                    }
                };
                getEditor().addDisposeListener(disposeListener);
            }
        }

        public Object getAdapter(Class class1) {
            return null;
        }
    }

    class ResourceTracker implements IResourceChangeListener, IResourceDeltaVisitor {

        public void resourceChanged(IResourceChangeEvent resourceChangeEvent) {
            IResourceDelta resourceDelta = resourceChangeEvent.getDelta();
            try {
                if (resourceDelta != null) resourceDelta.accept(this);
            } catch (CoreException _ex) {
            }
        }

        public boolean visit(IResourceDelta resourceDelta) {
            if (resourceDelta == null || !resourceDelta.getResource().equals(((FileEditorInput) getEditorInput()).getFile())) {
                return true;
            }
            if (resourceDelta.getKind() == 2) {
                Display display = getSite().getShell().getDisplay();
                if ((0x2000 & resourceDelta.getFlags()) == 0) {
                    display.asyncExec(new Runnable() {

                        public void run() {
                            if (!isDirty()) {
                                closeEditor(false);
                            }
                        }
                    });
                } else {
                    final IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(resourceDelta.getMovedToPath());
                    display.asyncExec(new Runnable() {

                        public void run() {
                            superSetInput(new FileEditorInput(newFile));
                        }
                    });
                }
            } else if (resourceDelta.getKind() == 4 && !editorSaving) {
                final IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(resourceDelta.getFullPath());
                Display display1 = getSite().getShell().getDisplay();
                display1.asyncExec(new Runnable() {

                    public void run() {
                        setInput(new FileEditorInput(newFile));
                        getCommandStack().flush();
                    }
                });
            }
            return false;
        }

        ResourceTracker() {
            super();
        }
    }

    public ERDesignerEditor() {
        editorSaving = false;
        resourceListener = new ResourceTracker();
        DefaultEditDomain defaultEditDomain = new MyDefaultEditDomain(this);
        defaultEditDomain.setActiveTool(new PanningSelectionTool());
        setEditDomain(defaultEditDomain);
        preferenceStore = DBDesignerPlugin.getDefault().getPreferenceStore();
        preferenceListener = new ERDPreferenceChangeListener(this, preferenceStore);
        preferenceStore.addPropertyChangeListener(preferenceListener);
    }

    class MyDefaultEditDomain extends DefaultEditDomain {

        public MyDefaultEditDomain(IEditorPart editorPart) {
            super(editorPart);
        }

        public void mouseDoubleClick(MouseEvent mouseEvent, EditPartViewer viewer) {
            super.mouseDoubleClick(mouseEvent, viewer);
            List selection = viewer.getSelectedEditParts();
            if (selection == null || selection.size() == 0) {
                return;
            }
            performModifyEditPart((EditPart) selection.get(0));
        }
    }

    protected void performModifyEditPart(EditPart editPart) {
        if (editPart.getClass() == ERTableNodeEditPart.class) {
            ERTableNodeEditPart tableNodeEditPart = (ERTableNodeEditPart) editPart;
            ERTableNode table = (ERTableNode) tableNodeEditPart.getModel();
            TableEditDialog dialog = new TableEditDialog(Display.getCurrent().getActiveShell(), IDialogCommand.EDIT, table);
            dialog.open();
            if (dialog.isModify()) {
                makeDirty();
            }
        } else if (editPart.getClass() == ERViewNodeEditPart.class) {
            ERViewNodeEditPart tableNodeEditPart = (ERViewNodeEditPart) editPart;
            ERViewNode table = (ERViewNode) tableNodeEditPart.getModel();
            ViewEditDialog dialog = new ViewEditDialog(Display.getCurrent().getActiveShell(), IDialogCommand.EDIT, table);
            dialog.open();
            if (dialog.isModify()) {
                makeDirty();
            }
        } else if (editPart.getClass() == ERRelationshipEditPart.class) {
            ERRelationshipEditPart relationshipEditPart = (ERRelationshipEditPart) editPart;
            ERRelationship relationship = (ERRelationship) relationshipEditPart.getModel();
            RelationshipEditDialog dialog = new RelationshipEditDialog(Display.getCurrent().getActiveShell(), IDialogCommand.EDIT, relationship);
            dialog.open();
            if (dialog.isModify()) {
                makeDirty();
            }
        } else if (editPart.getClass() == ERNoteNodeEditPart.class) {
            ERNoteNodeEditPart noteEditPart = (ERNoteNodeEditPart) editPart;
            ERNoteNode note = (ERNoteNode) noteEditPart.getModel();
            NoteEditDialog dialog = new NoteEditDialog(Display.getCurrent().getActiveShell(), IDialogCommand.EDIT, note);
            dialog.open();
            if (dialog.isModify()) {
                makeDirty();
            }
        }
    }

    public GraphicalViewer getGraphicalViewer() {
        return super.getGraphicalViewer();
    }

    public ERDiagram getDiagram() {
        return diagram;
    }

    protected void createGraphicalViewer(Composite composite) {
        ERDScrollingGraphicalViewer erdscrollinggraphicalviewer = new ERDScrollingGraphicalViewer();
        erdscrollinggraphicalviewer.createControl(composite);
        setGraphicalViewer(erdscrollinggraphicalviewer);
        configureGraphicalViewer();
        hookGraphicalViewer();
        initializeGraphicalViewer();
    }

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        ScrollingGraphicalViewer scrollingGraphicalViewer = (ScrollingGraphicalViewer) getGraphicalViewer();
        root = new ERDScalableFreeformRootEditPart();
        scrollingGraphicalViewer.setRootEditPart(root);
        scrollingGraphicalViewer.setEditPartFactory(new ERPartFactory());
        scrollingGraphicalViewer.setKeyHandler((new GraphicalViewerKeyHandler(scrollingGraphicalViewer)).setParent(getCommonKeyHandler()));
        ZoomManager zoomManager = root.getZoomManager();
        List<String> zoomLevelContributions = new ArrayList<String>();
        zoomLevelContributions.add(ZoomManager.FIT_ALL);
        zoomLevelContributions.add(ZoomManager.FIT_WIDTH);
        zoomLevelContributions.add(ZoomManager.FIT_HEIGHT);
        zoomManager.setZoomLevelContributions(zoomLevelContributions);
        double ad[] = { 0.01D, 0.10D, 0.25D, 0.5D, 0.75D, 1.0D, 1.5D, 2D, 2.5D, 3D, 4D };
        zoomManager.setZoomLevels(ad);
        ERDContextMenuProvider menuProvider = new ERDContextMenuProvider(scrollingGraphicalViewer, getActionRegistry(), this);
        scrollingGraphicalViewer.setContextMenu(menuProvider);
        loadProperties();
        ZoomInAction zoomInAction = new ZoomInAction(root.getZoomManager());
        ZoomOutAction zoomOutAction = new ZoomOutAction(root.getZoomManager());
        getActionRegistry().registerAction(zoomInAction);
        getActionRegistry().registerAction(zoomOutAction);
        getSite().getKeyBindingService().registerAction(zoomInAction);
        getSite().getKeyBindingService().registerAction(zoomOutAction);
        ToggleGridVisibilityAction togglegridvisibilityaction = new ToggleGridVisibilityAction(this, getGraphicalViewer());
        getActionRegistry().registerAction(togglegridvisibilityaction);
        IAction action = new ToggleSnapToGridAction(this, getGraphicalViewer());
        getActionRegistry().registerAction(action);
        action = new ToggleLabelVisibilitAction(diagram);
        getActionRegistry().registerAction(action);
    }

    protected void loadProperties() {
        GraphicalViewer graphicalviewer = getGraphicalViewer();
        if (diagram != null) {
            graphicalviewer.setProperty(ToggleSnapToGridAction.SNAPTOGRID, new Boolean(diagram.isSnapOn()));
            graphicalviewer.setProperty("SnapToGrid.isVisible", new Boolean(diagram.isGridVisible()));
        }
        IPreferenceStore store = DBDesignerPlugin.getDefault().getPreferenceStore();
        ERDConstants.TITLE_COLOR = new Color(null, PreferenceConverter.getColor(store, ERDConstants.TITLE_COLOR_PREF));
        ERDConstants.DETAIL_COLOR = new Color(null, PreferenceConverter.getColor(store, ERDConstants.DETAIL_COLOR_PREF));
        ERDConstants.CONNECTION_COLOR = new Color(null, PreferenceConverter.getColor(store, ERDConstants.CONNECTION_COLOR_PREF));
        ERDConstants.GRID_COLOR = new Color(null, PreferenceConverter.getColor(store, ERDConstants.GRID_COLOR_PREF));
        ERDConstants.TITLE_FONT = new Font(null, PreferenceConverter.getFontData(store, ERDConstants.TITLE_FONT_PREF));
        ERDConstants.DETAIL_FONT = new Font(null, PreferenceConverter.getFontData(store, ERDConstants.DETAIL_FONT_PREF));
        ERDConstants.CONNECTION_FONT = new Font(null, PreferenceConverter.getFontData(store, ERDConstants.CONNECTION_FONT_PREF));
        ERDConstants.CONNECTION_WIDTH = store.getInt(ERDConstants.CONNECTION_WIDTH_PREF);
        int gridSpacing = store.getInt(ERDConstants.GRID_SPACING_PREF);
        getGraphicalViewer().setProperty("SnapToGrid.GridSpacing", new Dimension(gridSpacing, gridSpacing));
        ERColumnEditPart.updateFonts(ERDConstants.DETAIL_FONT);
    }

    protected void createActions() {
        super.createActions();
        ActionRegistry actionregistry = getActionRegistry();
        IAction action = null;
        action = new MatchWidthAction(this);
        actionregistry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new MatchHeightAction(this);
        actionregistry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction(this, 1);
        actionregistry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction(this, 4);
        actionregistry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction(this, 8);
        actionregistry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction(this, 32);
        actionregistry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction(this, 2);
        actionregistry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AlignmentAction(this, 16);
        actionregistry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new ToggleTableDetailAction(this);
        actionregistry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new ToggleRoutingTypeAction(this);
        actionregistry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new ResetRoutingAction(this);
        actionregistry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new GridLayoutAction(this);
        actionregistry.registerAction(action);
        action = new AutoLayoutAction(this);
        actionregistry.registerAction(action);
        action = new ShowTablesAction(this);
        actionregistry.registerAction(action);
    }

    protected void initializeGraphicalViewer() {
        GraphicalViewer graphicalViewer = getGraphicalViewer();
        graphicalViewer.setContents(diagram);
        graphicalViewer.addDropTargetListener(new TableDropTargetListener(graphicalViewer));
    }

    public void makeDirty() {
        actionActivity = true;
        firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
    }

    public void commandStackChanged(EventObject eventobject) {
        if (isDirty()) {
            if (!savePreviouslyNeeded) {
                savePreviouslyNeeded = true;
                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
            }
        } else {
            savePreviouslyNeeded = false;
            firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
        }
        super.commandStackChanged(eventobject);
    }

    private KeyHandler getCommonKeyHandler() {
        if (sharedKeyHandler == null) {
            sharedKeyHandler = new KeyHandler();
            sharedKeyHandler.put(KeyStroke.getPressed('\177', 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
            sharedKeyHandler.put(KeyStroke.getPressed(0x100000b, 0), getActionRegistry().getAction("org.eclipse.gef.direct_edit"));
            sharedKeyHandler.put(KeyStroke.getPressed(0x100000c, 0x10000), getActionRegistry().getAction(ShowTablesAction.SELECT_TABLES));
        }
        return sharedKeyHandler;
    }

    public void doSave(IProgressMonitor iprogressmonitor) {
        try {
            if (!isValidDiagram()) {
                return;
            }
            editorSaving = true;
            storeToXMLFile(file);
            actionActivity = false;
            getCommandStack().markSaveLocation();
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            editorSaving = false;
        }
        return;
    }

    public void doSaveAs() {
        if (!isValidDiagram()) {
            return;
        }
        Shell shell = getSite().getShell();
        IPath path;
        final Object opFile;
        if (file instanceof IFile) {
            SaveAsDialog saveAsDialog = new SaveAsDialog(shell);
            saveAsDialog.setOriginalFile((IFile) file);
            if (saveAsDialog.open() != 0) {
                return;
            }
            path = saveAsDialog.getResult();
            if (path == null) {
                return;
            }
            opFile = getWorkspaceRoot().getFile(path);
        } else {
            FileDialog fileDialog = new FileDialog(shell);
            fileDialog.setText("Save As");
            fileDialog.setFilterPath(((File) file).getPath());
            fileDialog.setFileName(((File) file).getName());
            String filePath = fileDialog.open();
            if (filePath == null) {
                return;
            }
            path = new Path(filePath);
            opFile = path.toFile();
        }
        IEditorInput editorInput = EditorInputFactory.getEditorInput(opFile);
        WorkspaceModifyOperation modifyOperation = new WorkspaceModifyOperation() {

            public void execute(IProgressMonitor iprogressmonitor) throws CoreException {
                storeToXMLFile(opFile);
            }
        };
        try {
            editorSaving = true;
            (new ProgressMonitorDialog(getSite().getShell())).run(false, false, modifyOperation);
            setInput(editorInput);
            getGraphicalViewer().setContents(diagram);
            flushStack();
            actionActivity = false;
            getCommandStack().markSaveLocation();
        } catch (Exception exception) {
        } finally {
            editorSaving = false;
        }
        return;
    }

    public void flushStack() {
        getCommandStack().flush();
    }

    public boolean isDirty() {
        return isSaveOnCloseNeeded();
    }

    public boolean isSaveOnCloseNeeded() {
        return actionActivity | getCommandStack().isDirty();
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    protected void superSetInput(IEditorInput ieditorinput) {
        IEditorInput oldEditorInput = getEditorInput();
        if (oldEditorInput != null && (oldEditorInput instanceof FileEditorInput)) {
            IFile file = ((FileEditorInput) oldEditorInput).getFile();
            file.getWorkspace().removeResourceChangeListener(resourceListener);
        }
        super.setInput(ieditorinput);
        IEditorInput newEditorInput = getEditorInput();
        if (newEditorInput == null) {
            return;
        }
        if (newEditorInput instanceof FileEditorInput) {
            IFile file = ((FileEditorInput) newEditorInput).getFile();
            file.getWorkspace().addResourceChangeListener(resourceListener);
            setPartName(file.getName());
        } else {
            setPartName(newEditorInput.getName());
        }
    }

    public Object getAdapter(Class type) {
        if (type == org.eclipse.gef.ui.stackview.CommandStackInspectorPage.class) {
            return new CommandStackInspectorPage(getCommandStack());
        }
        if (type == org.eclipse.ui.views.contentoutline.IContentOutlinePage.class) {
            return outlinePage = new OutlinePage(new TreeViewer());
        }
        if (type == IContentOverviewPage.class) {
            return new OverviewPage(new TreeViewer());
        }
        if (type == org.eclipse.gef.editparts.ZoomManager.class) {
            return ((ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();
        } else {
            return super.getAdapter(type);
        }
    }

    private boolean getBooleanProperty(String property) {
        if (property == null) {
            return false;
        }
        Object value = getGraphicalViewer().getProperty(property);
        if (value == null) {
            return false;
        }
        return ((Boolean) value).booleanValue();
    }

    public void init(IEditorSite ieditorsite, IEditorInput ieditorinput) throws PartInitException {
        if (!EditorInputFactory.isFileOrStoreEditorInput(ieditorinput)) {
            MessageDialog.openError(null, "Messages.ERDesignerEditor_dialog_invalid_input_title", "Messages.ERDesignerEditor_dialog_invalid_input_message");
            throw new PartInitException("Invalid Input");
        }
        super.init(ieditorsite, ieditorinput);
        projectListener = new ERDPartListener2(this, ieditorsite.getWorkbenchWindow().getPartService());
    }

    public void dispose() {
        if (activationListener != null) {
            activationListener.dispose();
        }
        if (projectListener != null) {
            projectListener.dispose();
        }
        if (preferenceListener != null && preferenceStore != null) {
            preferenceStore.removePropertyChangeListener(preferenceListener);
        }
        super.dispose();
    }

    public void setInput(IEditorInput editorInput) {
        try {
            superSetInput(editorInput);
            file = getFileObject(editorInput);
            if (editorInput instanceof ERDiagramEditorInput) {
                diagram = ((ERDiagramEditorInput) editorInput).getDiagram();
                layoutMode = 2;
                creatingNewDiagram = true;
            } else {
                loadFromXMLFile(file);
            }
            diagram.setEditor(this);
            if (outlinePage != null) {
                outlinePage.setContents(diagram);
            }
        } catch (Exception e) {
            DBDesignerPlugin.getDefault().handleError("Error open designer resource", e);
        }
    }

    private void loadFromXMLFile(Object fileObject) throws CoreException {
        DiagramFileManager manager = new DiagramFileManager();
        diagram = manager.loadDiagram(file);
    }

    private void storeToXMLFile(Object fileObject) throws CoreException {
        boolean flag = getBooleanProperty("SnapToGrid.isVisible");
        boolean flag1 = getBooleanProperty(ToggleSnapToGridAction.SNAPTOGRID);
        diagram.setGridSnap(flag, flag1);
        DiagramFileManager manager = new DiagramFileManager();
        manager.storeDiagram(diagram, fileObject);
    }

    private Object getFileObject(IEditorInput editorInput) {
        return EditorInputFactory.getFileObject(editorInput);
    }

    public void closeEditor(boolean flag) {
        getSite().getPage().closeEditor(this, flag);
    }

    public boolean getRelayout() {
        return reLayout;
    }

    public void setRelayout(boolean flag) {
        reLayout = flag;
    }

    public void setLayoutMode(int i) {
        layoutMode = i;
    }

    public int getLayoutMode() {
        return layoutMode;
    }

    protected FigureCanvas getEditor() {
        return (FigureCanvas) getGraphicalViewer().getControl();
    }

    public void updateGridColor() {
        root.gridLayer.updateGridColor();
    }

    protected SelectionSynchronizer getSelectionSynchronizer() {
        if (synchronizer == null) {
            synchronizer = new SelectionSynchronizer() {

                protected EditPart convert(EditPartViewer editpartviewer, EditPart editpart) {
                    if (editpart instanceof ERTreeColumnEditPart) {
                        editpart = editpart.getParent();
                    }
                    Object obj = editpartviewer.getEditPartRegistry().get(editpart.getModel());
                    EditPart editpart1 = null;
                    if (obj != null) {
                        editpart1 = (EditPart) obj;
                    }
                    return editpart1;
                }
            };
        }
        return synchronizer;
    }

    public void enableSanityChecking(boolean flag) {
        if (activationListener != null) {
            activationListener.enableSanityChecking(flag);
        }
    }

    public void setAutoLayout() {
    }

    public static IWorkspace getWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    public static IWorkspaceRoot getWorkspaceRoot() {
        return getWorkspace().getRoot();
    }

    private boolean isValidDiagram() {
        List<ERRelationship> relationships = diagram.getRelationships();
        if (relationships == null || relationships.size() == 0) {
            return true;
        }
        for (ERRelationship relationship : relationships) {
            List<ERColumn> pkColumns = relationship.getPkColumns();
            List<ERColumn> fkColumns = relationship.getFkColumns();
            boolean isValidPk = true;
            boolean isValidFk = true;
            if (pkColumns == null || pkColumns.size() == 0) {
                isValidPk = false;
            }
            if (fkColumns == null || fkColumns.size() == 0) {
                isValidFk = false;
            }
            if (isValidPk && isValidFk) {
                continue;
            }
            String message = null;
            if (!isValidPk) {
                message = "PK";
            }
            if (!isValidFk) {
                if (message != null) {
                    message = message + " and FK";
                } else {
                    message = "FK";
                }
            }
            message = message + " columns are empty.\n" + "Relationship = [" + relationship.getName() + "]\n" + "PK table = [" + relationship.getPkTableName() + "]\n" + "FK table = [" + relationship.getFkTableName() + "]";
            MessageDialog.openError(null, "Validation Error", message);
            return false;
        }
        return true;
    }
}
