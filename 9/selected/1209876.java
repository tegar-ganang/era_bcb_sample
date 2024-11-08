package uk.ac.bolton.archimate.editor.diagram;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.gef.AutoexposeHelper;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import uk.ac.bolton.archimate.editor.diagram.actions.CreateDerivedRelationAction;
import uk.ac.bolton.archimate.editor.diagram.actions.DeleteFromModelAction;
import uk.ac.bolton.archimate.editor.diagram.actions.ShowStructuralChainsAction;
import uk.ac.bolton.archimate.editor.diagram.actions.ViewpointAction;
import uk.ac.bolton.archimate.editor.diagram.dnd.ArchimateDiagramTransferDropTargetListener;
import uk.ac.bolton.archimate.editor.diagram.editparts.ArchimateDiagramEditPartFactory;
import uk.ac.bolton.archimate.editor.diagram.util.ExtendedViewportAutoexposeHelper;
import uk.ac.bolton.archimate.editor.model.DiagramModelUtils;
import uk.ac.bolton.archimate.editor.model.viewpoints.IViewpoint;
import uk.ac.bolton.archimate.editor.model.viewpoints.ViewpointsManager;
import uk.ac.bolton.archimate.editor.preferences.ConnectionPreferences;
import uk.ac.bolton.archimate.editor.preferences.IPreferenceConstants;
import uk.ac.bolton.archimate.editor.preferences.Preferences;
import uk.ac.bolton.archimate.model.IArchimateDiagramModel;
import uk.ac.bolton.archimate.model.IArchimateElement;
import uk.ac.bolton.archimate.model.IArchimatePackage;
import uk.ac.bolton.archimate.model.IDiagramModelArchimateObject;
import uk.ac.bolton.archimate.model.IDiagramModelComponent;
import uk.ac.bolton.archimate.model.IRelationship;

/**
 * Archimate Diagram Editor
 * 
 * @author Phillip Beauvoir
 */
public class ArchimateDiagramEditor extends AbstractDiagramEditor implements IArchimateDiagramEditor {

    /**
     * Palette
     */
    private ArchimateDiagramEditorPalette fPalette;

    @Override
    protected void applicationPreferencesChanged(PropertyChangeEvent event) {
        if (IPreferenceConstants.VIEWPOINTS_HIDE_PALETTE_ELEMENTS == event.getProperty()) {
            if (Boolean.TRUE == event.getNewValue()) {
                setPaletteViewpoint();
            } else {
                getPaletteRoot().setViewpoint(null);
            }
        } else if (IPreferenceConstants.VIEWPOINTS_HIDE_DIAGRAM_ELEMENTS == event.getProperty()) {
            getGraphicalViewer().setContents(getModel());
        } else {
            super.applicationPreferencesChanged(event);
        }
    }

    /**
     * Set Viewpoint to current Viewpoint in model
     */
    protected void setViewpoint() {
        setPaletteViewpoint();
        getGraphicalViewer().setContents(getModel());
    }

    /**
     * Set Palette to current Viewpoint in model if Preference set
     */
    protected void setPaletteViewpoint() {
        if (Preferences.STORE.getBoolean(IPreferenceConstants.VIEWPOINTS_HIDE_PALETTE_ELEMENTS)) {
            getPaletteRoot().setViewpoint(ViewpointsManager.INSTANCE.getViewpoint(getModel().getViewpoint()));
        }
    }

    @Override
    public void doCreatePartControl(Composite parent) {
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, HELP_ID);
    }

    @Override
    public ArchimateDiagramEditorPalette getPaletteRoot() {
        if (fPalette == null) {
            fPalette = new ArchimateDiagramEditorPalette();
            setPaletteViewpoint();
        }
        return fPalette;
    }

    @Override
    public IArchimateDiagramModel getModel() {
        return (IArchimateDiagramModel) super.getModel();
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new ArchimateDiagramEditPartFactory());
        viewer.setContents(getModel());
        viewer.addDropTargetListener(new ArchimateDiagramTransferDropTargetListener(viewer));
    }

    @Override
    protected void configurePaletteViewer(PaletteViewer viewer) {
        super.configurePaletteViewer(viewer);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), PALETTE_HELP_ID);
    }

    @Override
    protected void createRootEditPart(GraphicalViewer viewer) {
        viewer.setRootEditPart(new ScalableFreeformRootEditPart() {

            @SuppressWarnings("rawtypes")
            @Override
            public Object getAdapter(Class adapter) {
                if (adapter == AutoexposeHelper.class) {
                    return new ExtendedViewportAutoexposeHelper(this, new Insets(50), false);
                }
                return super.getAdapter(adapter);
            }
        });
    }

    /**
     * Set up and register the context menu
     */
    @Override
    protected void registerContextMenu(GraphicalViewer viewer) {
        MenuManager provider = new ArchimateDiagramEditorContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(provider);
        getSite().registerContextMenu(ArchimateDiagramEditorContextMenuProvider.ID, provider, viewer);
    }

    @Override
    public void selectElements(IArchimateElement[] elements) {
        List<EditPart> editParts = new ArrayList<EditPart>();
        for (IArchimateElement element : elements) {
            for (IDiagramModelComponent dc : DiagramModelUtils.findDiagramModelComponentsForElement(getModel(), element)) {
                EditPart editPart = (EditPart) getGraphicalViewer().getEditPartRegistry().get(dc);
                if (editPart != null && editPart.isSelectable() && !editParts.contains(editPart)) {
                    editParts.add(editPart);
                }
            }
            if (ConnectionPreferences.useNestedConnections() && element instanceof IRelationship) {
                for (IDiagramModelArchimateObject[] list : DiagramModelUtils.findNestedComponentsForRelationship(getModel(), (IRelationship) element)) {
                    EditPart editPart1 = (EditPart) getGraphicalViewer().getEditPartRegistry().get(list[0]);
                    EditPart editPart2 = (EditPart) getGraphicalViewer().getEditPartRegistry().get(list[1]);
                    if (editPart1 != null && editPart1.isSelectable() && !editParts.contains(editPart1)) {
                        editParts.add(editPart1);
                    }
                    if (editPart2 != null && editPart2.isSelectable() && !editParts.contains(editPart2)) {
                        editParts.add(editPart2);
                    }
                }
            }
        }
        if (!editParts.isEmpty()) {
            getGraphicalViewer().setSelection(new StructuredSelection(editParts));
            getGraphicalViewer().reveal(editParts.get(0));
        } else {
            getGraphicalViewer().setSelection(StructuredSelection.EMPTY);
        }
    }

    /**
     * Add some extra Actions - *after* the graphical viewer has been created
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void createActions(GraphicalViewer viewer) {
        super.createActions(viewer);
        ActionRegistry registry = getActionRegistry();
        IAction action;
        action = new ShowStructuralChainsAction(this);
        registry.registerAction(action);
        action = new CreateDerivedRelationAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new DeleteFromModelAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        for (IViewpoint viewPoint : ViewpointsManager.INSTANCE.getAllViewpoints()) {
            action = new ViewpointAction(this, viewPoint);
            registry.registerAction(action);
        }
    }

    @Override
    protected void eCoreModelChanged(Notification msg) {
        super.eCoreModelChanged(msg);
        if (msg.getEventType() == Notification.SET) {
            if (msg.getNotifier() == getModel() && msg.getFeature() == IArchimatePackage.Literals.ARCHIMATE_DIAGRAM_MODEL__VIEWPOINT) {
                setViewpoint();
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (fPalette != null) {
            fPalette.dispose();
        }
    }

    public int getContextChangeMask() {
        return NONE;
    }

    public IContext getContext(Object target) {
        return HelpSystem.getContext(HELP_ID);
    }

    public String getSearchExpression(Object target) {
        return Messages.ArchimateDiagramEditor_0;
    }
}
