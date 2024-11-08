package uk.ac.bolton.archimate.editor.diagram;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteListener;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.tools.AbstractTool;
import org.eclipse.gef.tools.CreationTool;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.AlignmentAction;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.MatchHeightAction;
import org.eclipse.gef.ui.actions.MatchWidthAction;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import uk.ac.bolton.archimate.editor.ArchimateEditorPlugin;
import uk.ac.bolton.archimate.editor.diagram.actions.BorderColorAction;
import uk.ac.bolton.archimate.editor.diagram.actions.BringForwardAction;
import uk.ac.bolton.archimate.editor.diagram.actions.BringToFrontAction;
import uk.ac.bolton.archimate.editor.diagram.actions.ConnectionLineColorAction;
import uk.ac.bolton.archimate.editor.diagram.actions.ConnectionLineWidthAction;
import uk.ac.bolton.archimate.editor.diagram.actions.ConnectionRouterAction;
import uk.ac.bolton.archimate.editor.diagram.actions.CopyAction;
import uk.ac.bolton.archimate.editor.diagram.actions.CutAction;
import uk.ac.bolton.archimate.editor.diagram.actions.DefaultEditPartSizeAction;
import uk.ac.bolton.archimate.editor.diagram.actions.ExportAsImageAction;
import uk.ac.bolton.archimate.editor.diagram.actions.ExportAsImageToClipboardAction;
import uk.ac.bolton.archimate.editor.diagram.actions.FillColorAction;
import uk.ac.bolton.archimate.editor.diagram.actions.FontAction;
import uk.ac.bolton.archimate.editor.diagram.actions.FontColorAction;
import uk.ac.bolton.archimate.editor.diagram.actions.FullScreenAction;
import uk.ac.bolton.archimate.editor.diagram.actions.LockObjectAction;
import uk.ac.bolton.archimate.editor.diagram.actions.PasteAction;
import uk.ac.bolton.archimate.editor.diagram.actions.PrintDiagramAction;
import uk.ac.bolton.archimate.editor.diagram.actions.PropertiesAction;
import uk.ac.bolton.archimate.editor.diagram.actions.ResetAspectRatioAction;
import uk.ac.bolton.archimate.editor.diagram.actions.SelectAllAction;
import uk.ac.bolton.archimate.editor.diagram.actions.SelectElementInTreeAction;
import uk.ac.bolton.archimate.editor.diagram.actions.SendBackwardAction;
import uk.ac.bolton.archimate.editor.diagram.actions.SendToBackAction;
import uk.ac.bolton.archimate.editor.diagram.actions.TextAlignmentAction;
import uk.ac.bolton.archimate.editor.diagram.actions.TextPositionAction;
import uk.ac.bolton.archimate.editor.diagram.actions.ToggleGridEnabledAction;
import uk.ac.bolton.archimate.editor.diagram.actions.ToggleGridVisibleAction;
import uk.ac.bolton.archimate.editor.diagram.actions.ToggleSnapToAlignmentGuidesAction;
import uk.ac.bolton.archimate.editor.diagram.dnd.PaletteTemplateTransferDropTargetListener;
import uk.ac.bolton.archimate.editor.diagram.tools.FormatPainterInfo;
import uk.ac.bolton.archimate.editor.diagram.tools.FormatPainterToolEntry;
import uk.ac.bolton.archimate.editor.preferences.IPreferenceConstants;
import uk.ac.bolton.archimate.editor.preferences.Preferences;
import uk.ac.bolton.archimate.editor.ui.services.ComponentSelectionManager;
import uk.ac.bolton.archimate.editor.utils.PlatformUtils;
import uk.ac.bolton.archimate.model.IArchimateModel;
import uk.ac.bolton.archimate.model.IArchimatePackage;
import uk.ac.bolton.archimate.model.IDiagramModel;

/**
 * Abstract GEF Diagram Editor that checks for valid Editor Input.
 * If the Editor Input is of type NullDiagramEditorInput it shows a warning message.
 * This can happen when Eclipse tries to restore an Editor Part and the Diagram Model cannot be restored
 * because the model's file may have been deleted, renamed or moved on the file system.
 * 
 * @author Phillip Beauvoir
 */
public abstract class AbstractDiagramEditor extends GraphicalEditorWithFlyoutPalette implements IDiagramModelEditor, IContextProvider, ITabbedPropertySheetPageContributor {

    private Composite fErrorComposite;

    private NullDiagramEditorInput fNullInput;

    /**
     * Graphics Model
     */
    protected IDiagramModel fDiagramModel;

    /**
     * Actions that need to be updated after CommandStack changed
     */
    protected List<UpdateAction> fUpdateCommandStackActions = new ArrayList<UpdateAction>();

    /**
     * Listen to User Preferences Changes
     */
    protected IPropertyChangeListener appPreferencesListener = new IPropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent event) {
            applicationPreferencesChanged(event);
        }
    };

    /**
     * Application Preference changed
     * @param event
     */
    protected void applicationPreferencesChanged(PropertyChangeEvent event) {
        if (IPreferenceConstants.GRID_SIZE == event.getProperty()) {
            applyUserGridPreferences();
        } else if (IPreferenceConstants.GRID_VISIBLE == event.getProperty()) {
            applyUserGridPreferences();
        } else if (IPreferenceConstants.GRID_SNAP == event.getProperty()) {
            applyUserGridPreferences();
        } else if (IPreferenceConstants.GRID_SHOW_GUIDELINES == event.getProperty()) {
            applyUserGridPreferences();
        }
    }

    /**
     * Adapter class to respond to Archimate Model notifications.
     */
    protected Adapter eCoreAdapter = new EContentAdapter() {

        @Override
        public void notifyChanged(Notification msg) {
            super.notifyChanged(msg);
            eCoreModelChanged(msg);
        }
    };

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (input instanceof NullDiagramEditorInput) {
            fNullInput = (NullDiagramEditorInput) input;
            super.setSite(site);
            super.setInput(input);
            setPartName(input.getName());
        } else {
            super.init(site, input);
        }
    }

    @Override
    public void setInput(IEditorInput input) {
        super.setInput(input);
        fDiagramModel = ((DiagramEditorInput) input).getDiagramModel();
        fDiagramModel.getArchimateModel().eAdapters().add(eCoreAdapter);
        DefaultEditDomain domain = new DefaultEditDomain(this) {

            private CommandStack stack;

            @Override
            public CommandStack getCommandStack() {
                if (stack == null) {
                    stack = (CommandStack) fDiagramModel.getAdapter(CommandStack.class);
                }
                return stack;
            }
        };
        setEditDomain(domain);
        setPartName(input.getName());
        Preferences.STORE.addPropertyChangeListener(appPreferencesListener);
    }

    @Override
    public void createPartControl(Composite parent) {
        if (fNullInput != null) {
            createErrorComposite(parent);
        } else {
            super.createPartControl(parent);
            doCreatePartControl(parent);
            fixBug321560();
        }
    }

    /**
     * Create the Error composite messate
     * @param parent
     */
    protected void createErrorComposite(Composite parent) {
        fErrorComposite = new Composite(parent, SWT.NULL);
        fErrorComposite.setLayout(new GridLayout());
        fErrorComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        String message1 = Messages.AbstractDiagramEditor_0;
        String message2 = Messages.AbstractDiagramEditor_1;
        CLabel imageLabel = new CLabel(fErrorComposite, SWT.NULL);
        imageLabel.setImage(Display.getDefault().getSystemImage(SWT.ICON_INFORMATION));
        imageLabel.setText(message1);
        String fileName = fNullInput.getFileName();
        if (fileName != null) {
            message2 += " " + fileName;
        }
        Label l = new Label(fErrorComposite, SWT.NULL);
        l.setText(message2);
    }

    public IDiagramModel getModel() {
        return fDiagramModel;
    }

    /**
     * Do the createPartControl(Composite parent) method
     */
    protected abstract void doCreatePartControl(Composite parent);

    /**
     * Register a context menu
     */
    protected abstract void registerContextMenu(GraphicalViewer viewer);

    /**
     * Create the Root Edit Part
     */
    protected abstract void createRootEditPart(GraphicalViewer viewer);

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        registerContextMenu(viewer);
        createRootEditPart(viewer);
        createActions(viewer);
        viewer.addDropTargetListener(new PaletteTemplateTransferDropTargetListener(this));
        setProperties();
    }

    @Override
    public void setFocus() {
        if (fNullInput != null) {
            fErrorComposite.setFocus();
        } else {
            super.setFocus();
        }
    }

    @Override
    public GraphicalViewer getGraphicalViewer() {
        return super.getGraphicalViewer();
    }

    @Override
    protected DefaultEditDomain getEditDomain() {
        if (fNullInput != null) {
            return new DefaultEditDomain(this);
        } else {
            return super.getEditDomain();
        }
    }

    /**
     * Set Graphical Properties
     */
    protected void setProperties() {
        applyUserGridPreferences();
        getGraphicalViewer().setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), MouseWheelZoomHandler.SINGLETON);
    }

    /**
     * Apply grid Prefs
     */
    protected void applyUserGridPreferences() {
        int gridSize = Preferences.getGridSize();
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(gridSize, gridSize));
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, Preferences.isGridVisible());
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, Preferences.isGridSnap());
        getGraphicalViewer().setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, Preferences.doShowGuideLines());
    }

    /**
     * Create the PaletteViewerProvider.
     * Over-ride this so we can hook into the creation of the PaletteViewer.
     */
    @Override
    protected PaletteViewerProvider createPaletteViewerProvider() {
        boolean showPalette = Preferences.doShowPalette();
        getPalettePreferences().setPaletteState(showPalette ? FlyoutPaletteComposite.STATE_PINNED_OPEN : FlyoutPaletteComposite.STATE_COLLAPSED);
        return new PaletteViewerProvider(getEditDomain()) {

            @Override
            protected void hookPaletteViewer(PaletteViewer viewer) {
                super.hookPaletteViewer(viewer);
                AbstractDiagramEditor.this.configurePaletteViewer(viewer);
            }
        };
    }

    /**
     * Configure the Palette Viewer
     */
    protected void configurePaletteViewer(final PaletteViewer viewer) {
        viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
        viewer.addPaletteListener(new PaletteListener() {

            @Override
            public void activeToolChanged(PaletteViewer palette, ToolEntry toolEntry) {
                CreationFactory factory = (CreationFactory) toolEntry.getToolProperty(CreationTool.PROPERTY_CREATION_FACTORY);
                if (factory != null) {
                    ComponentSelectionManager.INSTANCE.fireSelectionEvent(toolEntry, factory.getObjectType());
                }
            }
        });
        viewer.getControl().addMouseTrackListener(new MouseTrackAdapter() {

            @Override
            public void mouseHover(MouseEvent e) {
                ToolEntry toolEntry = findToolEntryAt(viewer, new Point(e.x, e.y));
                if (toolEntry != null) {
                    CreationFactory factory = (CreationFactory) toolEntry.getToolProperty(CreationTool.PROPERTY_CREATION_FACTORY);
                    if (factory != null) {
                        ComponentSelectionManager.INSTANCE.fireSelectionEvent(toolEntry, factory.getObjectType());
                    }
                }
            }
        });
        viewer.getControl().addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDown(MouseEvent e) {
                ToolEntry toolEntry = findToolEntryAt(viewer, new Point(e.x, e.y));
                if (toolEntry != null) {
                    boolean shiftKey = (e.stateMask & SWT.SHIFT) != 0;
                    toolEntry.setToolProperty(AbstractTool.PROPERTY_UNLOAD_WHEN_FINISHED, !shiftKey);
                }
            }

            @Override
            public void mouseDoubleClick(MouseEvent e) {
                ToolEntry toolEntry = findToolEntryAt(viewer, new Point(e.x, e.y));
                if (toolEntry instanceof FormatPainterToolEntry) {
                    FormatPainterInfo.INSTANCE.reset();
                }
            }
        });
    }

    /**
     * Find a Tool Entry on the palette at point, or return null
     */
    private ToolEntry findToolEntryAt(PaletteViewer viewer, Point pt) {
        EditPart ep = viewer.findObjectAt(pt);
        if (ep != null && ep.getModel() instanceof ToolEntry) {
            return (ToolEntry) ep.getModel();
        }
        return null;
    }

    @Override
    public void commandStackChanged(EventObject event) {
        super.commandStackChanged(event);
        updateCommandStackActions();
        setDirty(getCommandStack().isDirty());
    }

    /**
     * Update those actions that need updating when the Command Stack changes
     */
    protected void updateCommandStackActions() {
        if (this.equals(getSite().getPage().getActiveEditor())) {
            for (UpdateAction action : getUpdateCommandStackActions()) {
                action.update();
            }
        }
    }

    protected List<UpdateAction> getUpdateCommandStackActions() {
        return fUpdateCommandStackActions;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    protected void setDirty(boolean dirty) {
        firePropertyChange(IEditorPart.PROP_DIRTY);
    }

    @Override
    public boolean isSaveOnCloseNeeded() {
        return false;
    }

    /**
     * Add some extra Actions - *after* the graphical viewer has been created
     */
    @SuppressWarnings("unchecked")
    protected void createActions(GraphicalViewer viewer) {
        ActionRegistry registry = getActionRegistry();
        IAction action;
        ZoomManager zoomManager = (ZoomManager) getAdapter(ZoomManager.class);
        double[] zoomLevels = { .25, .5, .75, 1.0, 1.5, 2.0, 2.5, 3, 4 };
        zoomManager.setZoomLevels(zoomLevels);
        List<String> zoomContributionLevels = new ArrayList<String>();
        zoomContributionLevels.add(ZoomManager.FIT_ALL);
        zoomContributionLevels.add(ZoomManager.FIT_WIDTH);
        zoomContributionLevels.add(ZoomManager.FIT_HEIGHT);
        zoomManager.setZoomLevelContributions(zoomContributionLevels);
        IAction zoomIn = new ZoomInAction(zoomManager);
        IAction zoomOut = new ZoomOutAction(zoomManager);
        registry.registerAction(zoomIn);
        registry.registerAction(zoomOut);
        IHandlerService service = (IHandlerService) getEditorSite().getService(IHandlerService.class);
        service.activateHandler(zoomIn.getActionDefinitionId(), new ActionHandler(zoomIn));
        service.activateHandler(zoomOut.getActionDefinitionId(), new ActionHandler(zoomOut));
        action = new SelectAllAction(this);
        registry.registerAction(action);
        action = new PrintDiagramAction(this);
        registry.registerAction(action);
        action = new DirectEditAction(this);
        action.setId(ActionFactory.RENAME.getId());
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = registry.getAction(ActionFactory.DELETE.getId());
        action.setText(Messages.AbstractDiagramEditor_2);
        action.setToolTipText(action.getText());
        getUpdateCommandStackActions().add((UpdateAction) action);
        PasteAction pasteAction = new PasteAction(this, viewer);
        registry.registerAction(pasteAction);
        getSelectionActions().add(pasteAction.getId());
        action = new CutAction(this, pasteAction);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new CopyAction(this, pasteAction);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new ToggleGridEnabledAction();
        registry.registerAction(action);
        action = new ToggleGridVisibleAction();
        registry.registerAction(action);
        action = new ToggleSnapToAlignmentGuidesAction();
        registry.registerAction(action);
        action = new MatchWidthAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new MatchHeightAction(this);
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
        action = new DefaultEditPartSizeAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new ResetAspectRatioAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new PropertiesAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new FillColorAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new ConnectionLineWidthAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new ConnectionLineColorAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new FontAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new FontColorAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new ExportAsImageAction(viewer);
        registry.registerAction(action);
        action = new ExportAsImageToClipboardAction(viewer);
        registry.registerAction(action);
        action = new ConnectionRouterAction.BendPointConnectionRouterAction(this);
        registry.registerAction(action);
        action = new ConnectionRouterAction.ShortestPathConnectionRouterAction(this);
        registry.registerAction(action);
        action = new ConnectionRouterAction.ManhattanConnectionRouterAction(this);
        registry.registerAction(action);
        action = new SendBackwardAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new BringForwardAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new SendToBackAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new BringToFrontAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        for (TextAlignmentAction a : TextAlignmentAction.createActions(this)) {
            registry.registerAction(a);
            getSelectionActions().add(a.getId());
            getUpdateCommandStackActions().add(a);
        }
        for (TextPositionAction a : TextPositionAction.createActions(this)) {
            registry.registerAction(a);
            getSelectionActions().add(a.getId());
            getUpdateCommandStackActions().add(a);
        }
        action = new LockObjectAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new BorderColorAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getUpdateCommandStackActions().add((UpdateAction) action);
        action = new FullScreenAction(this);
        registry.registerAction(action);
        action = new SelectElementInTreeAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
    }

    @Override
    public String getContributorId() {
        return ArchimateEditorPlugin.PLUGIN_ID;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == ZoomManager.class && getGraphicalViewer() != null) {
            return getGraphicalViewer().getProperty(ZoomManager.class.toString());
        }
        if (adapter == IContentOutlinePage.class && getGraphicalViewer() != null) {
            return new OverviewOutlinePage(this);
        }
        if (adapter == IPropertySheetPage.class) {
            return new TabbedPropertySheetPage(this);
        }
        if (adapter == IArchimateModel.class && getModel() != null) {
            return getModel().getArchimateModel();
        }
        if (adapter == IDiagramModel.class) {
            return getModel();
        }
        return super.getAdapter(adapter);
    }

    /**
     * The eCore Model changed
     * @param msg
     */
    protected void eCoreModelChanged(Notification msg) {
        if (msg.getEventType() == Notification.SET) {
            if (msg.getNotifier() == getModel() || msg.getNotifier() == getModel().getArchimateModel()) {
                if (msg.getFeature() == IArchimatePackage.Literals.NAMEABLE__NAME) {
                    setPartName(getEditorInput().getName());
                }
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        Preferences.STORE.removePropertyChangeListener(appPreferencesListener);
        if (getModel() != null && getModel().getArchimateModel() != null) {
            getModel().getArchimateModel().eAdapters().remove(eCoreAdapter);
        }
    }

    /**
     * Bug 321560 - [Palette] Using GTK, resizing palette does not work well and
     * loose keyboard: https://bugs.eclipse.org/bugs/show_bug.cgi?id=321560
     * <ul>
     * <li>Platform: Linux GTK
     * <li>Version: >= 3.5.2
     * <p>
     * The fix consists in removing the FlyoutComposite#Sash#SashDragManager and
     * replacing it with running the FlyoutComposite#ResizeAction. We remove the
     * SashDragManager by removing mouse and mouseMove listeners from sash. We
     * find the reference to ResizeAction by inspecting the context menu of the
     * title part in paletteContainer.
     */
    private void fixBug321560() {
        if (PlatformUtils.isGTK() && SWT.getVersion() >= 3520) {
            try {
                final Composite splitter = (Composite) getPrivateFieldValue(this, GraphicalEditorWithFlyoutPalette.class, "splitter");
                Control[] children = splitter.getChildren();
                Control sash = children[0];
                Composite paletteContainer = (Composite) children[1];
                Control[] paletteChildren = paletteContainer.getChildren();
                Control title = paletteChildren[0];
                Menu contextMenu = title.getMenu();
                Listener[] listeners = getListeners(contextMenu, SWT.Show);
                Object innerListener = listeners[0];
                if (innerListener instanceof TypedListener) {
                    innerListener = ((TypedListener) innerListener).getEventListener();
                }
                IMenuManager mgr = (IMenuManager) getPrivateFieldValue(innerListener, innerListener.getClass(), "this$0");
                final IAction resizeAction = ((ActionContributionItem) mgr.getItems()[0]).getAction();
                if (resizeAction == null) {
                    return;
                }
                removeListeners(sash, SWT.MouseMove);
                removeListeners(sash, SWT.MouseUp);
                removeListeners(sash, SWT.MouseDown);
                removeListeners(sash, SWT.MouseDoubleClick);
                sash.addListener(SWT.MouseDown, new Listener() {

                    public void handleEvent(Event event) {
                        if (resizeAction.isEnabled()) {
                            resizeAction.run();
                        }
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private Object getPrivateFieldValue(Object object, Class<?> cls, String string) throws Exception {
        Field f = cls.getDeclaredField(string);
        f.setAccessible(true);
        return f.get(object);
    }

    private void removeListeners(Control control, int eventType) throws Exception {
        Listener[] listeners = getListeners(control, eventType);
        for (int i = 0; i < listeners.length; i++) {
            control.removeListener(eventType, listeners[i]);
        }
    }

    private Listener[] getListeners(Widget w, int eventType) throws Exception {
        Method method = w.getClass().getMethod("getListeners", int.class);
        return (Listener[]) method.invoke(w, eventType);
    }
}
