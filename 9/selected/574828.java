package net.sf.graphiti.ui.editors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.EventObject;
import net.sf.graphiti.GraphitiModelPlugin;
import net.sf.graphiti.io.GenericGraphParser;
import net.sf.graphiti.io.GenericGraphWriter;
import net.sf.graphiti.model.Graph;
import net.sf.graphiti.model.IValidator;
import net.sf.graphiti.ui.GraphitiUiPlugin;
import net.sf.graphiti.ui.actions.CopyAction;
import net.sf.graphiti.ui.actions.CutAction;
import net.sf.graphiti.ui.actions.PasteAction;
import net.sf.graphiti.ui.actions.SetRefinementAction;
import net.sf.graphiti.ui.editparts.EditPartFactoryImpl;
import net.sf.graphiti.ui.editparts.GraphEditPart;
import net.sf.graphiti.ui.properties.PropertiesConstants;
import net.sf.graphiti.ui.wizards.SaveAsWizard;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.SelectAllAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * This class provides the graph editor.
 * 
 * @author Matthieu Wipliez
 * 
 */
public class GraphEditor extends GraphicalEditorWithFlyoutPalette implements ITabbedPropertySheetPageContributor {

    /**
	 * The editor ID
	 */
    public static final String ID = "net.sf.graphiti.ui.editors.GraphEditor";

    private Graph graph;

    private ZoomManager manager;

    private ThumbnailOutlinePage outlinePage;

    private PaletteRoot paletteRoot;

    private IStatus status;

    private TabbedPropertySheetPage tabbedPropertySheetPage;

    /**
	 * Create an editor
	 */
    public GraphEditor() {
        setEditDomain(new DefaultEditDomain(this));
        getPalettePreferences().setPaletteState(FlyoutPaletteComposite.STATE_PINNED_OPEN);
    }

    /**
	 * Automatically layout the graph edited with the given direction.
	 * 
	 * @param direction
	 *            The direction, one of:
	 *            <UL>
	 *            <LI>{@link org.eclipse.draw2d.PositionConstants#EAST}
	 *            <LI>{@link org.eclipse.draw2d.PositionConstants#SOUTH}
	 *            </UL>
	 */
    public void automaticallyLayout(int direction) {
        GraphEditPart doc = (GraphEditPart) getGraphicalViewer().getRootEditPart().getContents();
        doc.automaticallyLayoutGraphs(direction);
    }

    @Override
    public void commandStackChanged(EventObject event) {
        firePropertyChange(PROP_DIRTY);
        super.commandStackChanged(event);
    }

    @Override
    protected void configureGraphicalViewer() {
        double[] zoomLevels;
        ArrayList<String> zoomContributions;
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new EditPartFactoryImpl());
        ScalableFreeformRootEditPart rootEditPart = new ScalableFreeformRootEditPart();
        viewer.setRootEditPart(rootEditPart);
        manager = rootEditPart.getZoomManager();
        getActionRegistry().registerAction(new ZoomInAction(manager));
        getActionRegistry().registerAction(new ZoomOutAction(manager));
        zoomLevels = new double[] { 0.1, 0.15, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0 };
        manager.setZoomLevels(zoomLevels);
        zoomContributions = new ArrayList<String>();
        zoomContributions.add(ZoomManager.FIT_ALL);
        zoomContributions.add(ZoomManager.FIT_HEIGHT);
        zoomContributions.add(ZoomManager.FIT_WIDTH);
        manager.setZoomLevelContributions(zoomContributions);
        viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.CTRL), MouseWheelZoomHandler.SINGLETON);
        ContextMenuProvider provider = new GraphEditorContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(provider);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void createActions() {
        super.createActions();
        ActionRegistry registry = getActionRegistry();
        Class<?> actions[] = { CopyAction.class, CutAction.class, PasteAction.class, PrintAction.class, SelectAllAction.class, SetRefinementAction.class };
        for (Class<?> clz : actions) {
            try {
                Constructor<?> ctor = clz.getConstructor(IWorkbenchPart.class);
                IAction action = (IAction) ctor.newInstance(this);
                registry.registerAction(action);
                getSelectionActions().add(action.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new PaletteViewerProvider(getEditDomain()) {

            @Override
            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
                viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
            }
        };
    }

    @Override
    public void dispose() {
        removeMarkers();
        super.dispose();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        validate();
        IFile file = ((IFileEditorInput) getEditorInput()).getFile();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GenericGraphWriter writer = new GenericGraphWriter(graph);
        try {
            writer.write(file.getLocation().toString(), out);
            file.setContents(new ByteArrayInputStream(out.toByteArray()), true, false, monitor);
            try {
                out.close();
            } catch (IOException e) {
            }
            getCommandStack().markSaveLocation();
            file.getParent().refreshLocal(IFile.DEPTH_ONE, null);
        } catch (Exception e) {
            errorMessage(e.getMessage(), e);
            monitor.setCanceled(true);
        }
    }

    @Override
    public void doSaveAs() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        IEditorPart editor = page.getActiveEditor();
        SaveAsWizard wizard = new SaveAsWizard();
        wizard.init(workbench, new StructuredSelection(editor));
        WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
        dialog.open();
    }

    /**
	 * Displays an error message with the given exception.
	 * 
	 * @param message
	 *            A description of the error.
	 * @param exception
	 *            An exception.
	 */
    private void errorMessage(String message, Throwable exception) {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        Shell shell = window.getShell();
        IStatus status = new Status(IStatus.ERROR, GraphitiUiPlugin.PLUGIN_ID, message, exception);
        ErrorDialog.openError(shell, "Save error", "The file could not be saved.", status, IStatus.ERROR);
    }

    /**
	 * Executes the given command.
	 * 
	 * @param command
	 */
    public void executeCommand(Command command) {
        CommandStack stack = getEditDomain().getCommandStack();
        stack.execute(command);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class type) {
        if (type == ZoomManager.class) {
            return ((ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();
        } else if (type == IContentOutlinePage.class) {
            outlinePage = new ThumbnailOutlinePage(this);
            return outlinePage;
        } else if (type == IPropertySheetPage.class) {
            return tabbedPropertySheetPage;
        } else {
            return super.getAdapter(type);
        }
    }

    /**
	 * Returns the contents of this editor.
	 * 
	 * @return The contents of this editor.
	 */
    public Graph getContents() {
        return graph;
    }

    @Override
    public String getContributorId() {
        return PropertiesConstants.CONTRIBUTOR_ID;
    }

    @Override
    public GraphicalViewer getGraphicalViewer() {
        return super.getGraphicalViewer();
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        if (paletteRoot == null) {
            paletteRoot = GraphitiPalette.getPaletteRoot(graph);
        }
        return paletteRoot;
    }

    @Override
    public SelectionSynchronizer getSelectionSynchronizer() {
        return super.getSelectionSynchronizer();
    }

    /**
	 * Gives the current zoom factor
	 * 
	 * @return double
	 */
    public double getZoom() {
        return manager.getZoom();
    }

    @Override
    protected void initializeGraphicalViewer() {
        GraphicalViewer viewer = getGraphicalViewer();
        if (graph == null) {
            viewer.setContents(status);
        } else {
            viewer.setContents(graph);
            if (!(Boolean) graph.getValue(Graph.PROPERTY_HAS_LAYOUT)) {
                automaticallyLayout(PositionConstants.EAST);
            }
        }
        this.tabbedPropertySheetPage = new TabbedPropertySheetPage(this);
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
	 * Remove existing markers.
	 */
    private void removeMarkers() {
        IFile file = ((IFileEditorInput) getEditorInput()).getFile();
        try {
            file.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
        }
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        IFile file = ((IFileEditorInput) input).getFile();
        setPartName(file.getName());
        try {
            GenericGraphParser parser = new GenericGraphParser(GraphitiModelPlugin.getDefault().getConfigurations());
            graph = parser.parse(file);
            getEditDomain().setPaletteRoot(getPaletteRoot());
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            try {
                if (page != null) {
                    page.showView(IPageLayout.ID_PROP_SHEET);
                }
            } catch (PartInitException e) {
                e.printStackTrace();
            }
            validate();
            firePropertyChange(PROP_INPUT);
        } catch (Throwable e) {
            status = new Status(Status.ERROR, GraphitiUiPlugin.PLUGIN_ID, "An error occurred while parsing the file", e);
        }
    }

    /**
	 * Sets the zoom to see the entire width of the graph in editor
	 */
    public void setWidthZoom() {
        manager.setZoomAsText(ZoomManager.FIT_WIDTH);
    }

    /**
	 * Validate the graph.
	 * 
	 * @return True if the graph is valid, false otherwise.
	 */
    private void validate() {
        removeMarkers();
        IFile file = ((IFileEditorInput) getEditorInput()).getFile();
        IValidator validator = graph.getConfiguration().getValidator();
        if (!validator.validate(graph, file)) {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            try {
                page.showView(IPageLayout.ID_PROBLEM_VIEW);
            } catch (PartInitException e) {
            }
        }
    }
}
