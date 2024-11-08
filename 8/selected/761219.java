package net.entropysoft.jmx.plugin.jmxdashboard;

import java.util.EventObject;
import net.entropysoft.jmx.plugin.Activator;
import net.entropysoft.jmx.plugin.chart.JmxXYChart;
import net.entropysoft.jmx.plugin.jmxdashboard.actions.popup.ChangeOrderAction;
import net.entropysoft.jmx.plugin.jmxdashboard.actions.popup.EditChartPropertiesAction;
import net.entropysoft.jmx.plugin.jmxdashboard.actions.popup.SetXYChartTimeRangeAction;
import net.entropysoft.jmx.plugin.jmxdashboard.dnd.FileImageDropTargetListener;
import net.entropysoft.jmx.plugin.jmxdashboard.dnd.JmxAttributeDropTargetListener;
import net.entropysoft.jmx.plugin.jmxdashboard.gef.parts.DashboardEditPartFactory;
import net.entropysoft.jmx.plugin.jmxdashboard.gef.ruler.DashboardRulerProvider;
import net.entropysoft.jmx.plugin.jmxdashboard.model.Dashboard;
import net.entropysoft.jmx.plugin.jmxdashboard.model.DashboardRuler;
import net.entropysoft.jmx.plugin.jmxdashboard.model.persistance.DashboardDeserializer;
import net.entropysoft.jmx.plugin.jmxdashboard.model.persistance.DashboardSerializer;
import net.entropysoft.jmx.plugin.jmxdashboard.outline.DashboardOutlinePage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.AlignmentAction;
import org.eclipse.gef.ui.actions.MatchHeightAction;
import org.eclipse.gef.ui.actions.MatchWidthAction;
import org.eclipse.gef.ui.actions.ToggleGridAction;
import org.eclipse.gef.ui.actions.ToggleRulerVisibilityAction;
import org.eclipse.gef.ui.actions.ToggleSnapToGeometryAction;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.gef.ui.rulers.RulerComposite;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * The dashboard editor
 * 
 * @author cedric
 */
public class DashboardEditor extends GraphicalEditorWithFlyoutPalette {

    private static PaletteRoot PALETTE_ROOT;

    private Dashboard dashboard = new Dashboard();

    private RulerComposite rulerComp;

    /** the undoable <code>IPropertySheetPage</code> */
    private PropertySheetPage undoablePropertySheetPage;

    public DashboardEditor() {
        DefaultEditDomain defaultEditDomain = new DefaultEditDomain(this);
        setEditDomain(defaultEditDomain);
        setPartName(net.entropysoft.jmx.plugin.jmxdashboard.Messages.DashboardEditor_DashboardPartName);
    }

    public void dispose() {
        if (dashboard != null) {
            dashboard.dispose();
        }
        super.dispose();
    }

    protected Control getGraphicalControl() {
        return rulerComp;
    }

    protected void createGraphicalViewer(Composite parent) {
        rulerComp = new RulerComposite(parent, SWT.NONE);
        super.createGraphicalViewer(rulerComp);
        rulerComp.setGraphicalViewer((ScrollingGraphicalViewer) getGraphicalViewer());
    }

    /**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
	 */
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        DashboardEditPartFactory dashboardEditPartFactory = new DashboardEditPartFactory();
        dashboardEditPartFactory.setProject(getProject());
        getGraphicalViewer().setEditPartFactory(dashboardEditPartFactory);
        ContextMenuProvider provider = new DashboardContextMenuProvider(getGraphicalViewer(), getActionRegistry());
        getGraphicalViewer().setContextMenu(provider);
        IAction showRulers = new ToggleRulerVisibilityAction(getGraphicalViewer());
        getActionRegistry().registerAction(showRulers);
        IAction snapAction = new ToggleSnapToGeometryAction(getGraphicalViewer());
        getActionRegistry().registerAction(snapAction);
        IAction showGrid = new ToggleGridAction(getGraphicalViewer());
        getActionRegistry().registerAction(showGrid);
    }

    /**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#initializeGraphicalViewer()
	 */
    protected void initializeGraphicalViewer() {
        ScalableRootEditPart rootEditPart = new ScalableRootEditPart();
        getGraphicalViewer().setRootEditPart(rootEditPart);
        getGraphicalViewer().setContents(dashboard);
        updateRulers();
        getGraphicalViewer().addDropTargetListener(new JmxAttributeDropTargetListener(getGraphicalViewer()));
        getGraphicalViewer().addDropTargetListener(new FileImageDropTargetListener(this, getGraphicalViewer()));
    }

    /**
	 * update the horizontal and vertical rulers of the graphical viewer to the
	 * rulers of the dashboard
	 */
    private void updateRulers() {
        DashboardRuler ruler = getDashboard().getRuler(PositionConstants.WEST);
        RulerProvider provider = null;
        if (ruler != null) {
            provider = new DashboardRulerProvider(ruler);
        }
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_VERTICAL_RULER, provider);
        ruler = getDashboard().getRuler(PositionConstants.NORTH);
        provider = null;
        if (ruler != null) {
            provider = new DashboardRulerProvider(ruler);
        }
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_HORIZONTAL_RULER, provider);
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_RULER_VISIBILITY, Boolean.FALSE);
    }

    /**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithPalette#getPaletteRoot()
	 */
    protected PaletteRoot getPaletteRoot() {
        if (PALETTE_ROOT == null) PALETTE_ROOT = DashBoardEditorPaletteFactory.createPalette();
        return PALETTE_ROOT;
    }

    public Object getAdapter(Class type) {
        if (type == IContentOutlinePage.class) return getContentOutlinePage(); else if (type == IPropertySheetPage.class) return getPropertySheetPage(); else if (type == CommandStack.class) return getCommandStack(); else if (type == EditDomain.class) return getEditDomain(); else if (type == ActionRegistry.class) return getActionRegistry(); else if (type == GraphicalViewer.class || type == EditPartViewer.class) return getGraphicalViewer();
        return super.getAdapter(type);
    }

    /**
	 * Returns the undoable <code>PropertySheetPage</code> for this editor
	 * 
	 * @return the undoable <code>PropertySheetPage</code>
	 */
    private IPropertySheetPage getPropertySheetPage() {
        if (null == undoablePropertySheetPage) {
            undoablePropertySheetPage = new PropertySheetPage();
            undoablePropertySheetPage.setRootEntry(new UndoablePropertySheetEntry(getCommandStack()));
        }
        return undoablePropertySheetPage;
    }

    private IContentOutlinePage getContentOutlinePage() {
        return new DashboardOutlinePage(new TreeViewer(), getDashboard(), getActionRegistry(), getEditDomain(), getSelectionSynchronizer());
    }

    /**
	 * @see org.eclipse.gef.commands.CommandStackListener#commandStackChanged(java.util.EventObject)
	 */
    public void commandStackChanged(EventObject event) {
        firePropertyChange(IEditorPart.PROP_DIRTY);
        super.commandStackChanged(event);
    }

    /**
	 * set the dashboard to edit
	 * 
	 * @param dashboard
	 */
    private void setDashboard(Dashboard dashboard) {
        if (this.dashboard != null) {
            this.dashboard.dispose();
        }
        this.dashboard = dashboard;
    }

    protected void setInput(IEditorInput input) {
        super.setInput(input);
        SafeRunner.run(new SafeRunnable() {

            public void run() throws Exception {
                DashboardDeserializer dashboardDeserializer = new DashboardDeserializer();
                setDashboard(dashboardDeserializer.deserialize(getFile(), null));
                if (getGraphicalViewer() != null) {
                    getGraphicalViewer().setContents(dashboard);
                    updateRulers();
                }
            }

            public void handleException(Throwable e) {
                if (!SafeRunnable.getIgnoreErrors()) {
                    MessageDialog.openError(null, JFaceResources.getString("error"), NLS.bind(Messages.DashboardEditor_CannotLoadFile, e.getMessage()));
                }
            }
        });
    }

    public void doSave(final IProgressMonitor monitor) {
        if (getFile() == null) {
            throw new RuntimeException(Messages.DashboardEditor_CannotSaveDashBoard);
        }
        SafeRunner.run(new SafeRunnable() {

            public void run() throws Exception {
                DashboardSerializer dashboardSerializer = new DashboardSerializer();
                dashboardSerializer.serialize(dashboard, getFile(), monitor);
                getCommandStack().markSaveLocation();
            }

            public void handleException(Throwable e) {
                if (!SafeRunnable.getIgnoreErrors()) {
                    MessageDialog.openError(null, JFaceResources.getString("error"), NLS.bind(Messages.DashboardEditor_CannotSaveFile, e.getMessage()));
                }
            }
        });
    }

    public void doSaveAs() {
        performSaveAs();
    }

    protected boolean performSaveAs() {
        SaveAsDialog dialog = new SaveAsDialog(getSite().getWorkbenchWindow().getShell());
        final IFile originalFile = getFile();
        dialog.setOriginalFile(originalFile);
        dialog.open();
        IPath path = dialog.getResult();
        if (path == null) return false;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IFile file = workspace.getRoot().getFile(path);
        WorkspaceModifyOperation op = new WorkspaceModifyOperation() {

            public void execute(final IProgressMonitor monitor) {
                try {
                    DashboardSerializer dashboardSerializer = new DashboardSerializer();
                    dashboardSerializer.serialize(dashboard, getFile(), monitor);
                } catch (Exception e) {
                    MessageDialog.openError(null, JFaceResources.getString("error"), NLS.bind(Messages.DashboardEditor_CannotSaveFile, e.getMessage()));
                }
            }
        };
        try {
            new ProgressMonitorDialog(getSite().getWorkbenchWindow().getShell()).run(true, true, op);
        } catch (Exception e) {
            Activator.logError(NLS.bind(Messages.DashboardEditor_ErrotSavingFile, e.getMessage()), e);
        }
        setInput(new FileEditorInput(file));
        getCommandStack().markSaveLocation();
        return true;
    }

    /**
	 * get the file associated with the editor input if any
	 * 
	 * @return
	 */
    private IFile getFile() {
        IEditorInput editorInput = getEditorInput();
        if (editorInput == null) {
            return null;
        }
        IFile file = (IFile) editorInput.getAdapter(IFile.class);
        return file;
    }

    /**
	 * get the project this dashboard is associated with
	 */
    public IProject getProject() {
        IFile file = getFile();
        if (file == null) {
            return null;
        } else {
            return file.getProject();
        }
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    protected void createActions() {
        super.createActions();
        ActionRegistry registry = getActionRegistry();
        IAction action;
        action = new EditChartPropertiesAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
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
        action = new SetXYChartTimeRangeAction(this, SetXYChartTimeRangeAction.ID_1_MINUTE, Messages.DashboardEditor_1Min, JmxXYChart.TIME_RANGE_1_MIN);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new SetXYChartTimeRangeAction(this, SetXYChartTimeRangeAction.ID_5_MINUTES, Messages.DashboardEditor_5Min, JmxXYChart.TIME_RANGE_5_MIN);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new SetXYChartTimeRangeAction(this, SetXYChartTimeRangeAction.ID_10_MINUTES, Messages.DashboardEditor_10Min, JmxXYChart.TIME_RANGE_10_MIN);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new SetXYChartTimeRangeAction(this, SetXYChartTimeRangeAction.ID_30_MINUTES, Messages.DashboardEditor_30Min, JmxXYChart.TIME_RANGE_30_MIN);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new SetXYChartTimeRangeAction(this, SetXYChartTimeRangeAction.ID_1_HOUR, Messages.DashboardEditor_1Hour, JmxXYChart.TIME_RANGE_1_HOUR);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new SetXYChartTimeRangeAction(this, SetXYChartTimeRangeAction.ID_2_HOURS, Messages.DashboardEditor_2Hours, JmxXYChart.TIME_RANGE_2_HOUR);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new ChangeOrderAction(this, getEditDomain(), ChangeOrderAction.MOVE_DOWN);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new ChangeOrderAction(this, getEditDomain(), ChangeOrderAction.MOVE_UP);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
    }
}
