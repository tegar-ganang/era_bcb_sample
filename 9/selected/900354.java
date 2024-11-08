package org.isistan.flabot.edit.componenteditor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.ShortestPathConnectionRouter;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.isistan.flabot.ExtensionPointConstants;
import org.isistan.flabot.edit.componenteditor.actions.AssignResponsibilitiesAction;
import org.isistan.flabot.edit.componenteditor.actions.CopyAction;
import org.isistan.flabot.edit.componenteditor.actions.CutAction;
import org.isistan.flabot.edit.componenteditor.actions.EditComponentAction;
import org.isistan.flabot.edit.componenteditor.actions.EditPropertiesAction;
import org.isistan.flabot.edit.componenteditor.actions.EditResponsibilitiesAction;
import org.isistan.flabot.edit.componenteditor.actions.EditStereotypesAction;
import org.isistan.flabot.edit.componenteditor.editparts.ComponentEditPartFactory;
import org.isistan.flabot.edit.editor.ActionLoader;
import org.isistan.flabot.edit.editor.FlabotGraphicalEditor;
import org.isistan.flabot.edit.editor.actions.ArrangeAction;
import org.isistan.flabot.edit.editor.actions.EditVisualizationAction;
import org.isistan.flabot.edit.editor.actions.EditorSnapGeometryAction;
import org.isistan.flabot.edit.editor.actions.EditorToggleGridAction;
import org.isistan.flabot.edit.editor.actions.ExportDiagramAction;
import org.isistan.flabot.edit.editor.actions.PasteAction;
import org.isistan.flabot.edit.editor.actions.PrintDiagramAction;
import org.isistan.flabot.edit.editor.actions.RunConsistencyCheckAction;
import org.isistan.flabot.edit.editor.actions.RunEventManagerAction;
import org.isistan.flabot.edit.editor.actions.RunFamilyManagerAction;
import org.isistan.flabot.edit.editor.actions.SelectAllAction;
import org.isistan.flabot.edit.editor.commands.ArrangeCommand;
import org.isistan.flabot.edit.multipage.FlabotMultiPageEditor;
import org.isistan.flabot.edit.multipage.UnsettableDirtyStateEditor;
import org.isistan.flabot.util.problems.MessageAccumulator;
import org.isistan.flabot.util.problems.log.LoggerMessageAccumulator;

/** *  * @author $Author: franco $ * */
public class ComponentEditor extends FlabotGraphicalEditor implements UnsettableDirtyStateEditor {

    public ComponentEditor(FlabotMultiPageEditor parentEditor) {
        super(parentEditor);
    }

    /** Palette component, holding the tools and elements. */
    private static PaletteRoot PALETTE_MODEL;

    private IAction[] extensionActions;

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        IEditorPart activeEditor = getSite().getPage().getActiveEditor();
        if (this.equals(activeEditor) || ((parentEditor != null) && parentEditor.equals(activeEditor) && parentEditor.getActiveEditor() == this)) updateActions(getSelectionActions());
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
        viewer.setEditPartFactory(new ComponentEditPartFactory());
        ScalableFreeformRootEditPart rootEditPart = new ScalableFreeformRootEditPart();
        viewer.setRootEditPart(rootEditPart);
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        ContextMenuProvider cmProvider = new ComponentEditorContextMenuProvider(viewer, getActionRegistry(), this);
        viewer.setContextMenu(cmProvider);
        getSite().registerContextMenu(cmProvider, viewer);
        getGraphicalViewer().setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, getModel().getSnapToGeometryEnabled());
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, getModel().getGridEnabled());
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, getModel().getGridEnabled());
        getGraphicalViewer().addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                if (SnapToGrid.PROPERTY_GRID_VISIBLE.equals(event.getPropertyName())) getModel().setGridEnabled((Boolean) event.getNewValue());
                if (SnapToGeometry.PROPERTY_SNAP_ENABLED.equals(event.getPropertyName())) getModel().setSnapToGeometryEnabled((Boolean) event.getNewValue());
            }
        });
        IAction action = new ExportDiagramAction(getGraphicalViewer(), this);
        getActionRegistry().registerAction(action);
        getSelectionActions().add(action.getId());
        action = new PrintDiagramAction(getGraphicalViewer(), this);
        getActionRegistry().registerAction(action);
        getSelectionActions().add(action.getId());
        action = new EditorSnapGeometryAction(viewer);
        getActionRegistry().registerAction(action);
        action = new EditorToggleGridAction(viewer);
        getActionRegistry().registerAction(action);
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

            protected CreationFactory getFactory(Object template) {
                return new SimpleFactory((Class) template);
            }
        };
    }

    public void doSave(IProgressMonitor monitor) {
    }

    public void doSaveAs() {
    }

    public Object getAdapter(Class type) {
        if (type == ZoomManager.class) return getGraphicalViewer().getProperty(ZoomManager.class.toString());
        return super.getAdapter(type);
    }

    protected FlyoutPreferences getPalettePreferences() {
        return ComponentEditorPaletteFactory.createPalettePreferences();
    }

    protected PaletteRoot getPaletteRoot() {
        if (PALETTE_MODEL == null) PALETTE_MODEL = ComponentEditorPaletteFactory.createPalette();
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
        ScalableFreeformRootEditPart root = (ScalableFreeformRootEditPart) viewer.getRootEditPart();
        ConnectionLayer connLayer = (ConnectionLayer) root.getLayer(LayerConstants.CONNECTION_LAYER);
        GraphicalEditPart contentEditPart = (GraphicalEditPart) root.getContents();
        ShortestPathConnectionRouter router = new ShortestPathConnectionRouter(contentEditPart.getFigure());
        connLayer.setConnectionRouter(router);
        viewer.addDropTargetListener(createTransferDropTargetListener());
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    protected void createActions() {
        super.createActions();
        ActionRegistry registry = getActionRegistry();
        IAction action;
        action = new SelectAllAction(this);
        registry.registerAction(action);
        action = new CopyAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new CutAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new PasteAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new EditResponsibilitiesAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new EditPropertiesAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new EditStereotypesAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new EditVisualizationAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new RunConsistencyCheckAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new EditComponentAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new AssignResponsibilitiesAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new ArrangeAction(this, ArrangeCommand.BRING_FORWARD);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new ArrangeAction(this, ArrangeCommand.BRING_TO_FRONT);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new ArrangeAction(this, ArrangeCommand.SEND_TO_BACK);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new ArrangeAction(this, ArrangeCommand.SEND_BACKWARD);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new RunFamilyManagerAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new RunEventManagerAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        MessageAccumulator messageAccumulator = new LoggerMessageAccumulator();
        extensionActions = ActionLoader.loadAllActions(this, ExtensionPointConstants.COMPONENT_DIAGRAM_EDITOR_CONTEXT_MENU_ACTION, messageAccumulator);
        for (IAction extensionAction : extensionActions) {
            registry.registerAction(extensionAction);
            getSelectionActions().add(extensionAction.getId());
        }
    }

    public IAction getAction(String actionID) {
        ActionRegistry registry = getActionRegistry();
        return registry.getAction(actionID);
    }

    public void unsetDirty() {
        super.unsetDirty();
        getCommandStack().markSaveLocation();
        firePropertyChange(PROP_DIRTY);
    }

    IAction[] getExtensionActions() {
        return extensionActions;
    }
}
