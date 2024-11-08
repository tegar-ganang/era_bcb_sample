package ca.ucalgary.cpsc.agilePlanner.applicationWorkbench;

import java.io.File;
import java.util.EventObject;
import java.util.Hashtable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import ca.ucalgary.cpsc.agilePlanner.applicationWorkbench.actions.ArrangeAction;
import ca.ucalgary.cpsc.agilePlanner.applicationWorkbench.actions.CollapseStoryCardAction;
import ca.ucalgary.cpsc.agilePlanner.applicationWorkbench.actions.DropDownColorActionMenu;
import ca.ucalgary.cpsc.agilePlanner.applicationWorkbench.actions.ExpandStoryCardAction;
import ca.ucalgary.cpsc.agilePlanner.cards.CardConstants;
import ca.ucalgary.cpsc.agilePlanner.cards.commands.ProjectLoadCommand;
import ca.ucalgary.cpsc.agilePlanner.cards.commands.StoryCardRotateCommand;
import ca.ucalgary.cpsc.agilePlanner.cards.editpart.ProjectEditPartFactory;
import ca.ucalgary.cpsc.agilePlanner.cards.model.ProjectModel;
import ca.ucalgary.cpsc.agilePlanner.filesystemaccess.FileSystemUtility;
import ca.ucalgary.cpsc.agilePlanner.persister.ConnectionFailedException;
import ca.ucalgary.cpsc.agilePlanner.persister.NotConnectedException;
import ca.ucalgary.cpsc.agilePlanner.persister.datachangeimplement.DataCallback;
import ca.ucalgary.cpsc.agilePlanner.persister.factory.PersisterConnectionInfo;
import ca.ucalgary.cpsc.agilePlanner.persister.factory.PersisterFactory;

public class Editor extends GraphicalEditorWithPalette {

    private static PaletteRoot TABLE_ROOT;

    public static final String ID = "rallydemogef.Editor";

    private ProjectModel projectModel = null;

    private ProjectLoadCommand loadProjectCommand;

    private String tooltip = CardConstants.APPLICATIONNAME;

    private DataCallback dataCallback;

    public Editor() {
        super();
        dataCallback = new DataCallback();
        dataCallback.setEditor(this);
        PersisterFactory.getPersister().addPlannerDataChangeListener(dataCallback);
        PersisterFactory.getUIEventPropagator().addPlannerUIChangeListener(dataCallback);
        loadProjectCommand = new ProjectLoadCommand();
        if (isProjectRecieved()) {
            this.setEditDomain(new DefaultEditDomain(this));
        } else {
            resetPersisterToLocalMode("c:\\ProjectFile");
            this.setEditDomain(new DefaultEditDomain(this));
        }
    }

    public void resetPersisterToLocalMode(String absoluteFileName) {
        PersisterConnectionInfo.getPersisterConnectionInfo().setPersisterType("LOCAL");
        PersisterConnectionInfo.getPersisterConnectionInfo().setPersisterConnectionInfo();
        PersisterFactory.getPersister().removePlannerDataChangeListener(dataCallback);
        PersisterFactory.getUIEventPropagator().removePlannerUIChangeListener(dataCallback);
        PersisterFactory.deletePersister();
        PersisterFactory.getPersister();
        PersisterFactory.getPersister().addPlannerDataChangeListener(dataCallback);
        PersisterFactory.getUIEventPropagator().addPlannerUIChangeListener(dataCallback);
        try {
            PersisterFactory.getPersister().connect();
        } catch (ConnectionFailedException e) {
            ca.ucalgary.cpsc.agilePlanner.util.Logger.singleton().debug(e.getMessage() + "\r\n" + e.getStackTrace());
        }
        if (!absoluteFileName.equalsIgnoreCase("dummy")) {
            MessageBox mbox = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.ICON_ERROR);
            mbox.setText("Connection problem.");
            mbox.setMessage("Could not retrieve project model data from server. Switching to local mode now.");
            mbox.open();
        }
    }

    public boolean isProjectRecieved() {
        int timeCount = 0;
        while (projectModel == null && timeCount < 16000) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                ca.ucalgary.cpsc.agilePlanner.util.Logger.singleton().debug(e.getMessage() + "\r\n" + e.getStackTrace());
            }
            timeCount += 500;
        }
        if (projectModel == null) return false; else return true;
    }

    private TransferDropTargetListener createTransferDropTargetListener() {
        return new TemplateTransferDropTargetListener(this.getGraphicalViewer()) {

            @Override
            protected CreationFactory getFactory(Object template) {
                return new SimpleFactory((Class) template);
            }
        };
    }

    private static Hashtable<Long, StoryCardRotateCommand> rotateHelper = new Hashtable<Long, StoryCardRotateCommand>();

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = this.getGraphicalViewer();
        viewer.setEditPartFactory(new ProjectEditPartFactory());
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        KeyHandler keyHandler = new GraphicalViewerKeyHandler(viewer);
        KeyStroke delKey = KeyStroke.getPressed(SWT.DEL, 127, 0);
        IAction delAction = getActionRegistry().getAction(ActionFactory.DELETE.getId());
        keyHandler.put(delKey, delAction);
        KeyStroke backSpaceKey = KeyStroke.getPressed(SWT.BS, 8, 0);
        IAction backSpaceKeyAction = getActionRegistry().getAction(ActionFactory.DELETE.getId());
        keyHandler.put(backSpaceKey, backSpaceKeyAction);
        viewer.setKeyHandler(keyHandler);
        if (PersisterConnectionInfo.getPersisterConnectionInfo().isMouseMessageOut()) {
            viewer.getControl().addMouseMoveListener(new ZoomedMouseMoveListener(((ScalableFreeformRootEditPart) (viewer.getRootEditPart())).getZoomManager()));
        }
        ;
        ContextMenuProvider provider = new MasePlannerContextMenuProvider(viewer, this.getActionRegistry());
        viewer.setContextMenu(provider);
    }

    @Override
    protected void createActions() {
        super.createActions();
        IAction action = new ArrangeAction(this);
        action.setId("ArrangeCards");
        action.setText("Arrange all the cards in the Editor");
        getActionRegistry().registerAction(action);
        getSelectionActions().add(action.getId());
        IAction actionExpandStoryCards = new ExpandStoryCardAction(this);
        actionExpandStoryCards.setId("ExpandStoryCards");
        actionExpandStoryCards.setText("Expand all the story cards in the table.");
        getActionRegistry().registerAction(actionExpandStoryCards);
        getSelectionActions().add(actionExpandStoryCards.getId());
        IAction actionCollapseStoryCards = new CollapseStoryCardAction(this);
        actionCollapseStoryCards.setId("CollapseStoryCards");
        actionCollapseStoryCards.setText("Collapse all the story cards in the table.");
        getActionRegistry().registerAction(actionCollapseStoryCards);
        getSelectionActions().add(actionCollapseStoryCards.getId());
    }

    /**
	 * Thmais methode modified after the GEF shapes editor provided by
	 * 
	 * @return
	 */
    protected PaletteViewerProvider createPaletteViewProvider() {
        return new PaletteViewerProvider(this.getEditDomain()) {

            @Override
            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
                viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
            }
        };
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        TABLE_ROOT = ProjectPaletteRootFactory.createPalette(this);
        return TABLE_ROOT;
    }

    @Override
    protected void initializeGraphicalViewer() {
        EditPartViewer viewer = this.getGraphicalViewer();
        assert (this.projectModel != null);
        viewer.setContents(this.projectModel);
        viewer.addDropTargetListener(this.createTransferDropTargetListener());
        String titleTab = "";
        if (PersisterConnectionInfo.getPersisterConnectionInfo().getPersisterType().equalsIgnoreCase("LOCAL")) {
            titleTab = PersisterConnectionInfo.getPersisterConnectionInfo().getProjectName() + ":" + PersisterConnectionInfo.getPersisterConnectionInfo().getInitialLocal();
        } else {
            titleTab = PersisterConnectionInfo.getPersisterConnectionInfo().getProjectName() + ":" + PersisterConnectionInfo.getPersisterConnectionInfo().getInitialDis();
        }
        this.updateTitleTab(titleTab);
    }

    @Override
    public void commandStackChanged(EventObject event) {
        this.firePropertyChange(IEditorPart.PROP_DIRTY);
        super.commandStackChanged(event);
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    /** ************************************************************************************* * */
    @SuppressWarnings("static-access")
    @Override
    public void doSaveAs() {
        FileSystemUtility utility = FileSystemUtility.getFileSystemUtility();
        utility.saveFileAs(this.projectModel);
    }

    @Override
    public Object getAdapter(Class type) {
        if (type == ZoomManager.class) {
            return getGraphicalViewer().getProperty(ZoomManager.class.toString());
        }
        return super.getAdapter(type);
    }

    public ProjectModel getModel() {
        return this.projectModel;
    }

    /***************************************************************************
	 * Warning, eclipse recomends that this method should not be overridden,
	 * however this was not possible as teh setTitleToolTip() did not change the
	 * tooltip.
	 **************************************************************************/
    @Override
    public String getTitleToolTip() {
        return this.tooltip;
    }

    public GraphicalViewer getViewer() {
        return this.getGraphicalViewer();
    }

    /**
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.ISaveablePart#isDirty() Removes the save option from
	 *      the file when a change occures.
	 */
    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    @SuppressWarnings("static-access")
    public void setDirty() {
        this.firePropertyChange(this.PROP_DIRTY);
    }

    public void setProjectModel(ProjectModel projectModel) {
        this.projectModel = projectModel;
        updateTitleTab(this.projectModel.getProjectDataObject().getName());
    }

    public void updateTitleTab(String titleTabName) {
        this.setPartName(titleTabName);
        this.tooltip = titleTabName;
        this.firePropertyChange(IWorkbenchPart.PROP_TITLE);
    }

    private class ZoomedMouseMoveListener extends ca.ucalgary.cpsc.agilePlanner.util.Object implements MouseMoveListener {

        private ZoomManager manager;

        public ZoomedMouseMoveListener(ZoomManager manager) {
            this.manager = manager;
        }

        public void mouseMove(MouseEvent e) {
            int xSend = 0;
            int ySend = 0;
            FigureCanvas fg;
            fg = (FigureCanvas) e.getSource();
            double ratio = manager.getZoom();
            ySend = (int) (e.y / ratio + fg.getVerticalBar().getSelection() / ratio - 7 / ratio);
            xSend = (int) (e.x / ratio + fg.getHorizontalBar().getSelection() / ratio - 10 / ratio);
            try {
                PersisterFactory.getUIEventPropagator().moveMouse(PersisterConnectionInfo.getPersisterConnectionInfo().getInitialDis(), xSend, ySend);
            } catch (NotConnectedException ex) {
                ca.ucalgary.cpsc.agilePlanner.util.Logger.singleton().debug(ex.getMessage() + "\r\n" + ex.getStackTrace());
            }
        }
    }

    public DataCallback getDataCallback() {
        return dataCallback;
    }

    public void setDataCallback(DataCallback dataCallback) {
        this.dataCallback = dataCallback;
    }

    public PaletteRoot getTABLE_ROOT() {
        return TABLE_ROOT;
    }

    public void setTABLE_ROOT(PaletteRoot table_root) {
        TABLE_ROOT = table_root;
    }
}
