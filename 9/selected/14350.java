package com.byterefinery.rmbench.editors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.Request;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.SelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.tools.AbstractTool;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.AlignmentAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.SelectAllAction;
import org.eclipse.gef.ui.actions.ToggleGridAction;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import com.byterefinery.rmbench.EventManager;
import com.byterefinery.rmbench.RMBenchConstants;
import com.byterefinery.rmbench.RMBenchMessages;
import com.byterefinery.rmbench.RMBenchPlugin;
import com.byterefinery.rmbench.EventManager.Event;
import com.byterefinery.rmbench.actions.AddStubbedTablesAction;
import com.byterefinery.rmbench.actions.CopyDiagramTablesAction;
import com.byterefinery.rmbench.actions.CutDiagramTablesAction;
import com.byterefinery.rmbench.actions.DeleteAction;
import com.byterefinery.rmbench.actions.DetailsViewAction;
import com.byterefinery.rmbench.actions.DiagramExportAction;
import com.byterefinery.rmbench.actions.ForeignKeyAction;
import com.byterefinery.rmbench.actions.LayoutFiguresAction;
import com.byterefinery.rmbench.actions.PageOutlineAction;
import com.byterefinery.rmbench.actions.PasteTablesAction;
import com.byterefinery.rmbench.actions.PrintAction;
import com.byterefinery.rmbench.actions.PrinterSetupAction;
import com.byterefinery.rmbench.actions.TablesDiagramAction;
import com.byterefinery.rmbench.dnd.DiagramDropTargetListener;
import com.byterefinery.rmbench.editparts.ColumnEditPart;
import com.byterefinery.rmbench.editparts.CustomEditPartFactory;
import com.byterefinery.rmbench.editparts.DiagramEditPart;
import com.byterefinery.rmbench.editparts.ForeignKeyEditPart;
import com.byterefinery.rmbench.editparts.TableEditPart;
import com.byterefinery.rmbench.editpolicies.ComponentFactory;
import com.byterefinery.rmbench.exceptions.ExceptionMessages;
import com.byterefinery.rmbench.figures.PageOutlineLayer;
import com.byterefinery.rmbench.model.diagram.Diagram;
import com.byterefinery.rmbench.model.schema.Column;
import com.byterefinery.rmbench.model.schema.ForeignKey;
import com.byterefinery.rmbench.model.schema.Table;
import com.byterefinery.rmbench.operations.RMBenchOperation;
import com.byterefinery.rmbench.operations.UndoRedoActionGroup;
import com.byterefinery.rmbench.util.ImageConstants;
import com.byterefinery.rmbench.util.MarqueeSelectionTool2;
import com.byterefinery.rmbench.util.ModelManager;
import com.byterefinery.rmbench.views.property.RMBenchPropertySheetPage;

/**
 * a graphical editor for a relational database schema
 * 
 * @author cse
 */
public class DiagramEditor extends EditorPart implements ISelectionListener {

    public static final String ID = "com.byterefinery.rmbench.editors.diagramEditor";

    private static final double[] ZOOM_LEVELS = new double[] { .2, .3, .4, .5, .6, .7, .8, .9, 1, 1.25, 1.5 };

    private static final String MODEL_MEMENTO_KEY = "rmbench.model";

    private static final String DIAGRAM_MEMENTO_KEY = "rmbench.diagram";

    private IPropertySheetPage propertySheetPage;

    private PaletteViewerProvider provider;

    private FlyoutPaletteComposite splitter;

    private GraphicalViewer viewer;

    private SelectionSynchronizer synchronizer;

    private final DefaultEditDomain editDomain;

    private final ActionRegistry actionRegistry = new ActionRegistry();

    private final List<String> selectionActionIDs = new ArrayList<String>();

    private final List<String> editPartActionIDs = new ArrayList<String>();

    private final List<String> editorActionIDs = new ArrayList<String>();

    private UndoRedoActionGroup undoRedoGroup;

    private PasteTablesAction pasteTablesAction;

    private class Listener extends EventManager.Listener {

        public void eventOccurred(int eventType, Event event) {
            switch(eventType) {
                case DIAGRAM_DELETED:
                    {
                        if (event.element == getDiagram()) getSite().getPage().closeEditor(DiagramEditor.this, true);
                    }
                case DIAGRAM_MODIFIED:
                    {
                        if (event.info == NAME) setPartName(getDiagram().getName());
                    }
            }
        }

        public void register() {
            RMBenchPlugin.getEventManager().addListener(DIAGRAM_DELETED | DIAGRAM_MODIFIED, this);
        }

        public void unregister() {
            super.unregister();
        }
    }

    ;

    private Listener listener = new Listener();

    public DiagramEditor() {
        this.editDomain = new DefaultEditDomain(this);
        this.editDomain.setPaletteRoot(createPaletteRoot());
    }

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (!(input instanceof Input)) {
            throw new PartInitException("unsupported input type " + input.getClass());
        }
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
        undoRedoGroup = new UndoRedoActionGroup(getSite(), RMBenchOperation.CONTEXT, false);
        undoRedoGroup.fillActionBars(site.getActionBars());
        WorkbenchPartAction action;
        action = new DeleteAction(this, DeleteAction.REMOVE);
        addEditPartAction(action);
        selectionActionIDs.add(action.getId());
        action = new DeleteAction(this, DeleteAction.DELETE);
        addEditPartAction(action);
        selectionActionIDs.add(action.getId());
        action = new CutDiagramTablesAction(this);
        addEditPartAction(action);
        selectionActionIDs.add(action.getId());
        action = new CopyDiagramTablesAction(this);
        addEditPartAction(action);
        selectionActionIDs.add(action.getId());
        pasteTablesAction = new PasteTablesAction(this);
        addEditPartAction(pasteTablesAction);
        selectionActionIDs.add(pasteTablesAction.getId());
        addEditorAction(new SelectAllAction(this));
        addEditorAction(new SaveAction(this));
        addEditorAction(new PrintAction(this));
        action = new LayoutFiguresAction(this);
        addEditorAction(action);
        selectionActionIDs.add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.LEFT);
        addEditorAction(action);
        selectionActionIDs.add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.RIGHT);
        addEditorAction(action);
        selectionActionIDs.add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.TOP);
        addEditorAction(action);
        selectionActionIDs.add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.BOTTOM);
        addEditorAction(action);
        selectionActionIDs.add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.CENTER);
        addEditorAction(action);
        selectionActionIDs.add(action.getId());
        action = new AlignmentAction((IWorkbenchPart) this, PositionConstants.MIDDLE);
        addEditorAction(action);
        selectionActionIDs.add(action.getId());
        action = new DetailsViewAction((IWorkbenchPart) this);
        addEditorAction(action);
        selectionActionIDs.add(action.getId());
        action = new TablesDiagramAction((IWorkbenchPart) this, getDiagram().getModel());
        addEditorAction(action);
        selectionActionIDs.add(action.getId());
        action = new AddStubbedTablesAction((IWorkbenchPart) this);
        addEditorAction(action);
        selectionActionIDs.add(action.getId());
        listener.register();
    }

    public void createPartControl(Composite parent) {
        splitter = new FlyoutPaletteComposite(parent, SWT.NONE, getSite().getPage(), getPaletteViewerProvider(), new PaletteFlyoutPreferences());
        viewer = new ScrollingGraphicalViewer();
        viewer.createControl(splitter);
        editDomain.addViewer(viewer);
        viewer.getControl().setBackground(ColorConstants.listBackground);
        viewer.setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(45, 45));
        ScalableFreeformRootEditPart root = new CustomRootEditPart();
        viewer.setRootEditPart(root);
        viewer.setEditPartFactory(new CustomEditPartFactory());
        getSelectionSynchronizer().addViewer(viewer);
        getSite().setSelectionProvider(viewer);
        splitter.hookDropTargetListener(viewer);
        splitter.setGraphicalControl(viewer.getControl());
        configureViewerActions(root);
        configureKeyHandler();
        ContextMenuProvider comtextMenuProvider = new DiagramContextMenuProvider(viewer, actionRegistry, undoRedoGroup);
        viewer.setContextMenu(comtextMenuProvider);
        viewer.addDropTargetListener(DiagramDropTargetListener.forImport(viewer));
        viewer.addDropTargetListener(DiagramDropTargetListener.forModel(viewer));
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                if (selection.size() == 1) {
                    EditPart selectedPart = (EditPart) selection.getFirstElement();
                    if (selectedPart instanceof TableEditPart) {
                        Table table = ((TableEditPart) selectedPart).getTable();
                        RMBenchPlugin.getEventManager().fireTableSelected(DiagramEditor.this, table);
                    } else if (selectedPart instanceof ColumnEditPart) {
                        Column column = ((ColumnEditPart) selectedPart).getColumn();
                        RMBenchPlugin.getEventManager().fireColumnSelected(DiagramEditor.this, column);
                    } else if (selectedPart instanceof ForeignKeyEditPart) {
                        ForeignKey key = ((ForeignKeyEditPart) selectedPart).getForeignKey();
                        RMBenchPlugin.getEventManager().fireForeignKeySelected(DiagramEditor.this, key);
                    } else {
                        RMBenchPlugin.getEventManager().fireTableSelected(DiagramEditor.this, null);
                    }
                }
                updateActions(selectionActionIDs);
            }
        });
        viewer.setContents(getDiagram());
        if (RMBenchPlugin.getModelManager().isDirty()) firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
        pasteTablesAction.hookEvents();
    }

    private void configureKeyHandler() {
        KeyHandler keyHandler = new GraphicalViewerKeyHandler(viewer);
        keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), actionRegistry.getAction(DeleteAction.REMOVE));
        keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, SWT.CTRL), actionRegistry.getAction(DeleteAction.DELETE));
        viewer.setKeyHandler(keyHandler);
    }

    public void dispose() {
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
        listener.unregister();
        undoRedoGroup.dispose();
        super.dispose();
    }

    @SuppressWarnings("deprecation")
    private void configureViewerActions(ScalableFreeformRootEditPart root) {
        root.getZoomManager().setZoomLevels(ZOOM_LEVELS);
        List<String> zoomContribs = new ArrayList<String>(3);
        zoomContribs.add(ZoomManager.FIT_ALL);
        zoomContribs.add(ZoomManager.FIT_WIDTH);
        zoomContribs.add(ZoomManager.FIT_HEIGHT);
        root.getZoomManager().setZoomLevelContributions(zoomContribs);
        IAction zoomIn = new ZoomInAction(root.getZoomManager());
        IAction zoomOut = new ZoomOutAction(root.getZoomManager());
        addEditorAction(zoomIn);
        addEditorAction(zoomOut);
        getSite().getKeyBindingService().registerAction(zoomIn);
        getSite().getKeyBindingService().registerAction(zoomOut);
        IAction pageOutline = new PageOutlineAction(viewer.getControl().getShell(), root);
        actionRegistry.registerAction(pageOutline);
        IAction printSetup = new PrinterSetupAction(viewer.getControl().getShell(), root);
        actionRegistry.registerAction(printSetup);
        IAction diagramExport = new DiagramExportAction(viewer.getControl().getShell(), this);
        actionRegistry.registerAction(diagramExport);
        IAction showGrid = new ToggleGridAction(viewer);
        actionRegistry.registerAction(showGrid);
        IAction fkAction = new ForeignKeyAction((IWorkbenchPart) this, getDiagram(), viewer);
        addEditorAction(fkAction);
        selectionActionIDs.add(fkAction.getId());
    }

    /**
     * @return a <code>PaletteRoot</code> to be used by this editor's palette
     */
    protected PaletteRoot createPaletteRoot() {
        PaletteRoot paletteRoot = new PaletteRoot();
        PaletteGroup controls = new PaletteGroup("Controls");
        paletteRoot.add(controls);
        ToolEntry tool = new SelectionToolEntry();
        controls.add(tool);
        paletteRoot.setDefaultEntry(tool);
        MarqueeToolEntry marqueeEntry = new MarqueeToolEntry();
        controls.add(marqueeEntry);
        marqueeEntry.setToolClass(MarqueeSelectionTool2.class);
        PaletteSeparator separator = new PaletteSeparator(RMBenchConstants.PLUGIN_ID + ".palette.seperator");
        separator.setUserModificationPermission(PaletteEntry.PERMISSION_NO_MODIFICATION);
        controls.add(separator);
        PaletteDrawer drawer = new PaletteDrawer(RMBenchMessages.DiagramEditor_schemaelements, RMBenchPlugin.getImageDescriptor(ImageConstants.SCHEMA));
        ToolEntry entry = new CombinedTemplateCreationEntry(RMBenchMessages.DiagramEditor_table, RMBenchMessages.DiagramEditor_table_desc, null, ComponentFactory.TABLE, RMBenchPlugin.getImageDescriptor(ImageConstants.TABLE), RMBenchPlugin.getImageDescriptor(ImageConstants.TABLE));
        drawer.add(entry);
        entry = new ConnectionCreationToolEntry(RMBenchMessages.DiagramEditor_connection, RMBenchMessages.DiagramEditor_connection_desc, null, RMBenchPlugin.getImageDescriptor(ImageConstants.FOREIGN_KEY), RMBenchPlugin.getImageDescriptor(ImageConstants.FOREIGN_KEY));
        entry.setToolProperty(AbstractTool.PROPERTY_UNLOAD_WHEN_FINISHED, Boolean.TRUE);
        drawer.add(entry);
        entry = new CombinedTemplateCreationEntry(RMBenchMessages.DiagramEditor_constraint, RMBenchMessages.DiagramEditor_constraint_desc, null, ComponentFactory.CONSTRAINT, RMBenchPlugin.getImageDescriptor(ImageConstants.CONSTRAINT), RMBenchPlugin.getImageDescriptor(ImageConstants.CONSTRAINT));
        drawer.add(entry);
        entry = new CombinedTemplateCreationEntry(RMBenchMessages.DiagramEditor_index, RMBenchMessages.DiagramEditor_index_desc, null, ComponentFactory.INDEX, RMBenchPlugin.getImageDescriptor(ImageConstants.INDEX), RMBenchPlugin.getImageDescriptor(ImageConstants.INDEX));
        drawer.add(entry);
        paletteRoot.add(drawer);
        return paletteRoot;
    }

    /**
     * Returns the palette viewer provider that is used to create palettes for the view and
     * the flyout.  Creates one if it doesn't already exist.
     * 
     * @return  the PaletteViewerProvider that can be used to create PaletteViewers for
     *          this editor
     */
    protected final PaletteViewerProvider getPaletteViewerProvider() {
        if (provider == null) provider = new PaletteViewerProvider(editDomain);
        return provider;
    }

    /**
     * Returns the selection syncronizer object. The synchronizer can be used to sync the
     * selection of 2 or more EditPartViewers.
     * @return the syncrhonizer
     */
    protected SelectionSynchronizer getSelectionSynchronizer() {
        if (synchronizer == null) synchronizer = new SelectionSynchronizer();
        return synchronizer;
    }

    public void setFocus() {
        viewer.getControl().setFocus();
    }

    public void doSave(IProgressMonitor monitor) {
        RMBenchPlugin.getModelManager().doSave(getSite().getShell(), monitor);
    }

    public void doSaveAs() {
        IProgressMonitor progressMonitor = getEditorSite().getActionBars().getStatusLineManager().getProgressMonitor();
        RMBenchPlugin.getModelManager().doSaveAs(getSite().getShell(), progressMonitor);
    }

    public boolean isDirty() {
        return RMBenchPlugin.getModelManager().isDirty();
    }

    public void dirtyChanged(boolean dirty) {
        if (!dirty) {
            Input input = (Input) getEditorInput();
            input.wasSaved();
        }
        firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    public boolean isSaveOnCloseNeeded() {
        return false;
    }

    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter) {
        if (adapter == GraphicalViewer.class || adapter == EditPartViewer.class) return viewer; else if (adapter == CommandStack.class) return editDomain.getCommandStack(); else if (adapter == EditDomain.class) return editDomain; else if (adapter == ActionRegistry.class) return getActionRegistry(); else if (adapter == ZoomManager.class) return getZoomManager(); else if (adapter == IContentOutlinePage.class) return new OutlinePage(this); else if (adapter == IPropertySheetPage.class) {
            if (propertySheetPage == null) propertySheetPage = new RMBenchPropertySheetPage();
            return propertySheetPage;
        }
        return super.getAdapter(adapter);
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (this.equals(getSite().getPage().getActiveEditor())) updateActions(selectionActionIDs);
    }

    public Object getZoomManager() {
        return viewer.getProperty(ZoomManager.class.toString());
    }

    protected ActionRegistry getActionRegistry() {
        return actionRegistry;
    }

    protected FigureCanvas getViewerControl() {
        return (FigureCanvas) viewer.getControl();
    }

    protected DefaultEditDomain getEditDomain() {
        return editDomain;
    }

    public GraphicalViewer getViewer() {
        return viewer;
    }

    public Diagram getDiagram() {
        return ((Input) getEditorInput()).diagram;
    }

    public DiagramEditPart getDiagramPart() {
        return (DiagramEditPart) getViewer().getEditPartRegistry().get(getDiagram());
    }

    private void updateActions(List<String> actionIds) {
        for (Iterator<String> ids = actionIds.iterator(); ids.hasNext(); ) {
            IAction action = actionRegistry.getAction(ids.next());
            if (null != action && action instanceof UpdateAction) {
                ((UpdateAction) action).update();
            }
        }
    }

    private void addEditorAction(IAction action) {
        actionRegistry.registerAction(action);
        editorActionIDs.add(action.getId());
    }

    private void addEditPartAction(IAction action) {
        actionRegistry.registerAction(action);
        editPartActionIDs.add(action.getId());
    }

    /**
     * root edit part that adds a layer for printable page feedback
     */
    private static class CustomRootEditPart extends ScalableFreeformRootEditPart {

        protected ScalableFreeformLayeredPane createScaledLayers() {
            ScalableFreeformLayeredPane layers = super.createScaledLayers();
            layers.add(new PageOutlineLayer(false), PageOutlineLayer.LAYER_ID, 0);
            return layers;
        }

        public DragTracker getDragTracker(Request req) {
            return new MarqueeSelectionTool2.DragTracker();
        }
    }

    public static class Input implements IEditorInput, IPersistableElement {

        private final Diagram diagram;

        private String name;

        private boolean isNew;

        /**
         * @param diagram a diagram
         */
        public Input(Diagram diagram) {
            this.diagram = diagram;
            this.name = diagram.getName();
            this.isNew = isNewStorage();
        }

        /**
         * @param diagram a diagram
         * @param isNew <code>true</code> if the diagram was not previously saved into the model
         */
        public Input(Diagram diagram, boolean isNew) {
            this.diagram = diagram;
            this.isNew = isNew || isNewStorage();
        }

        public void wasSaved() {
            isNew = false;
            name = diagram.getName();
        }

        private boolean isNewStorage() {
            ModelManager manager = RMBenchPlugin.getModelManager();
            return manager.isNewStorage();
        }

        public boolean exists() {
            return false;
        }

        public ImageDescriptor getImageDescriptor() {
            return ImageDescriptor.getMissingImageDescriptor();
        }

        public String getName() {
            return diagram.getName();
        }

        public IPersistableElement getPersistable() {
            return isNew ? null : this;
        }

        public String getToolTipText() {
            return RMBenchMessages.DiagramEditor_tooltip;
        }

        @SuppressWarnings("rawtypes")
        public Object getAdapter(Class adapter) {
            if (adapter == IPersistableElement.class) return this;
            return null;
        }

        public boolean equals(Object obj) {
            return obj instanceof Input && diagram.equals(((Input) obj).diagram);
        }

        public int hashCode() {
            return diagram.hashCode();
        }

        public String getFactoryId() {
            return InputFactory.ID;
        }

        public void saveState(IMemento memento) {
            String modelKey = RMBenchPlugin.getModelManager().getModelStorageKey();
            memento.putString(MODEL_MEMENTO_KEY, modelKey);
            memento.putString(DIAGRAM_MEMENTO_KEY, name);
        }
    }

    public static class InputFactory implements IElementFactory {

        static final String ID = "com.byterefinery.rmbench.diagramInputFactory";

        public IAdaptable createElement(IMemento memento) {
            String modelKey = memento.getString(MODEL_MEMENTO_KEY);
            if (modelKey != null) {
                boolean activated = RMBenchPlugin.getModelManager().activateModelStorage(modelKey, null);
                Diagram diagram = null;
                if (activated) {
                    String diagramName = memento.getString(DIAGRAM_MEMENTO_KEY);
                    diagram = RMBenchPlugin.getModelManager().getModel().getDiagram(diagramName);
                } else {
                    String msg = MessageFormat.format(ExceptionMessages.errorActivateModel, new Object[] { modelKey });
                    RMBenchPlugin.logError(msg);
                }
                return diagram != null ? new Input(diagram) : null;
            } else return null;
        }
    }
}
