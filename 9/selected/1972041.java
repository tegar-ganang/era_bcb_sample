package org.ist.contract.editor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.AlignmentAction;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.EditorPartAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.gef.ui.actions.StackAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.ist.contract.ContractUIPlugin;
import org.ist.contract.actions.EditAction;
import org.ist.contract.delegate.UIDelegate;
import org.ist.contract.editor.input.ContractModelInput;
import org.ist.contract.editor.input.FileContractModelInput;
import org.ist.contract.impl.ContractImpl;
import org.ist.contract.model.resource.ContractModelManager;
import org.ist.contract.model.resource.IContractModelManager;
import org.ist.contract.store.core.i18n.Messages;
import org.ist.contract.store.core.i18n.MessagesConstants;
import org.ist.contract.util.UIUtil;

/**
 * Main class for the GEF based Contract-Editor.
 * Extending a Multipage-Editor.
 */
public class ContractEditor extends MultiPageEditorPart implements IAdaptable {

    public static final String ID = "contract.editor.MultiPageEditor";

    /** the model manager */
    private IContractModelManager modelManager;

    /**
	 * This class listens for command stack changes of the pages
	 * contained in this editor and decides if the editor is dirty or not.
	 */
    private class MultiPageCommandStackListener implements CommandStackListener {

        /** the observed command stacks */
        private List commandStacks = new ArrayList(2);

        /**
		 * Adds a <code>CommandStack</code> to observe.
		 * @param commandStack
		 */
        public void addCommandStack(CommandStack commandStack) {
            commandStacks.add(commandStack);
            commandStack.addCommandStackListener(this);
        }

        public void commandStackChanged(EventObject event) {
            if (((CommandStack) event.getSource()).isDirty()) {
                setDirty(true);
            } else {
                boolean oneIsDirty = false;
                for (Iterator stacks = commandStacks.iterator(); stacks.hasNext(); ) {
                    CommandStack stack = (CommandStack) stacks.next();
                    if (stack.isDirty()) {
                        oneIsDirty = true;
                        break;
                    }
                }
                setDirty(oneIsDirty);
            }
        }

        /**
		 * Disposed the listener
		 */
        public void dispose() {
            for (Iterator stacks = commandStacks.iterator(); stacks.hasNext(); ) {
                ((CommandStack) stacks.next()).removeCommandStackListener(this);
            }
            commandStacks.clear();
        }

        /**
		 * Marks every observed command stack beeing saved.
		 * This method should be called whenever the editor/model
		 * was saved.
		 */
        public void markSaveLocations() {
            for (Iterator stacks = commandStacks.iterator(); stacks.hasNext(); ) {
                CommandStack stack = (CommandStack) stacks.next();
                stack.markSaveLocation();
            }
        }
    }

    /**
	 * This class listens to changes to the file system in the workspace, and
	 * makes changes accordingly.
	 * 1) An open, saved file gets deleted -> close the editor
	 * 2) An open file gets renamed or moved -> change the editor's input accordingly
	 * 
	 */
    private class ResourceTracker implements IResourceChangeListener, IResourceDeltaVisitor {

        public void resourceChanged(IResourceChangeEvent event) {
            IResourceDelta delta = event.getDelta();
            try {
                if (delta != null) delta.accept(this);
            } catch (CoreException exception) {
                ContractUIPlugin.getDefault().getLog().log(exception.getStatus());
                exception.printStackTrace();
            }
        }

        public boolean visit(IResourceDelta delta) {
            if (delta == null || !delta.getResource().equals(((IFileEditorInput) getEditorInput()).getFile())) return true;
            if (delta.getKind() == IResourceDelta.REMOVED) {
                if ((IResourceDelta.MOVED_TO & delta.getFlags()) == 0) {
                    if (!isDirty()) closeEditor(false);
                } else {
                    final IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(delta.getMovedToPath());
                    Display display = getSite().getShell().getDisplay();
                    display.asyncExec(new Runnable() {

                        public void run() {
                            setInput(new FileEditorInput(newFile));
                        }
                    });
                }
            }
            return false;
        }
    }

    /** the editor's action registry */
    private ActionRegistry actionRegistry;

    /** the delegating CommandStack */
    private DelegatingCommandStack delegatingCommandStack;

    /**
	 * The <code>CommandStackListener</code> that listens for
	 * changes of the <code>DelegatingCommandStack</code>.
	 */
    private CommandStackListener delegatingCommandStackListener = new CommandStackListener() {

        public void commandStackChanged(EventObject event) {
            updateActions(stackActionIDs);
        }
    };

    /** the list of action ids that are editor actions */
    private List editorActionIDs = new ArrayList();

    /** the list of action ids that are to EditPart actions */
    private List editPartActionIDs = new ArrayList();

    /** the multi page editor's dirty state */
    private boolean isDirty = false;

    /** the <code>CommandStackListener</code> */
    private MultiPageCommandStackListener multiPageCommandStackListener;

    /** the palette root */
    private PaletteRoot paletteRoot;

    /** the resource tracker instance */
    private ResourceTracker resourceTracker;

    /** the selection listener */
    private ISelectionListener selectionListener = new ISelectionListener() {

        public void selectionChanged(IWorkbenchPart part, ISelection selection) {
            updateActions(editPartActionIDs);
        }
    };

    /** the shared key handler */
    private KeyHandler sharedKeyHandler;

    /** the list of action ids that are to CommandStack actions */
    private List stackActionIDs = new ArrayList();

    /** the selection synchronizer for the edit part viewer */
    private SelectionSynchronizer synchronizer;

    /** the undoable <code>IPropertySheetPage</code> */
    private PropertySheetPage undoablePropertySheetPage;

    /** this is out Contract */
    private ContractImpl Contract;

    /** id of the ContractPage */
    private int ContractPageID;

    /**
	 * Adds an action to this editor's <code>ActionRegistry</code>.
	 * (This is a helper method.)
	 * 
	 * @param action the action to add.
	 */
    protected void addAction(IAction action) {
        getActionRegistry().registerAction(action);
    }

    /**
	 * Adds an editor action to this editor.
	 * 
	 * <p><Editor actions are actions that depend
	 * and work on the editor.
	 * 
	 * @param action the editor action
	 */
    protected void addEditorAction(EditorPartAction action) {
        getActionRegistry().registerAction(action);
        editorActionIDs.add(action.getId());
    }

    protected void addEditorAction(SaveAction action) {
        getActionRegistry().registerAction(action);
        editorActionIDs.add(action.getId());
    }

    protected void addEditorAction(PrintAction action) {
        getActionRegistry().registerAction(action);
        editorActionIDs.add(action.getId());
    }

    /**
	 * Adds an <code>EditPart</code> action to this editor.
	 * 
	 * <p><code>EditPart</code> actions are actions that depend
	 * and work on the selected <code>EditPart</code>s.
	 * 
	 * @param action the <code>EditPart</code> action
	 */
    protected void addEditPartAction(SelectionAction action) {
        getActionRegistry().registerAction(action);
        editPartActionIDs.add(action.getId());
    }

    /**
	 * Adds an <code>CommandStack</code> action to this editor.
	 * 
	 * <p><code>CommandStack</code> actions are actions that depend
	 * and work on the <code>CommandStack</code>.
	 * 
	 * @param action the <code>CommandStack</code> action
	 */
    protected void addStackAction(StackAction action) {
        getActionRegistry().registerAction(action);
        stackActionIDs.add(action.getId());
    }

    /**
	 * Closes this editor.
	 * @param save
	 */
    void closeEditor(final boolean save) {
        getSite().getShell().getDisplay().syncExec(new Runnable() {

            public void run() {
                getSite().getPage().closeEditor(ContractEditor.this, save);
            }
        });
    }

    /**
	 * Creates actions and registers them to the ActionRegistry.
	 */
    protected void createActions() {
        addStackAction(new UndoAction(this));
        addStackAction(new RedoAction(this));
        addEditPartAction(new DeleteAction((IWorkbenchPart) this));
        addEditPartAction(new AlignmentAction((IWorkbenchPart) this, PositionConstants.LEFT));
        addEditPartAction(new AlignmentAction((IWorkbenchPart) this, PositionConstants.RIGHT));
        addEditPartAction(new AlignmentAction((IWorkbenchPart) this, PositionConstants.TOP));
        addEditPartAction(new AlignmentAction((IWorkbenchPart) this, PositionConstants.BOTTOM));
        addEditPartAction(new AlignmentAction((IWorkbenchPart) this, PositionConstants.CENTER));
        addEditPartAction(new AlignmentAction((IWorkbenchPart) this, PositionConstants.MIDDLE));
        addEditorAction(new SaveAction(this));
        addEditorAction(new PrintAction(this));
        IAction zoomIn = new ZoomInAction(getDelegatingZoomManager());
        IAction zoomOut = new ZoomOutAction(getDelegatingZoomManager());
        addAction(zoomIn);
        addAction(zoomOut);
        addEditPartAction(new EditAction((IWorkbenchPart) this));
    }

    protected void createPages() {
        try {
            ContractPageID = addPage(new ContractPage(this), getEditorInput());
            setPageText(ContractPageID, getContractPage().getPageName());
            getMultiPageCommandStackListener().addCommandStack(getContractPage().getCommandStack());
            getDelegatingCommandStack().setCurrentCommandStack(getContractPage().getCommandStack());
            setActivePage(ContractPageID);
        } catch (PartInitException e) {
            UIDelegate.displayError("", e.getMessage(), null);
        }
    }

    public void dispose() {
        getMultiPageCommandStackListener().dispose();
        getDelegatingCommandStack().removeCommandStackListener(getDelegatingCommandStackListener());
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(getSelectionListener());
        getActionRegistry().dispose();
        super.dispose();
    }

    public void doSave(IProgressMonitor monitor) {
        try {
            save(monitor);
            getMultiPageCommandStackListener().markSaveLocations();
        } catch (CoreException e) {
            UIDelegate.displayError("", e.getMessage(), null);
        }
    }

    public void doSaveAs() {
        SaveAsDialog dialog = new SaveAsDialog(getSite().getShell());
        dialog.setOriginalFile(((IFileEditorInput) getEditorInput()).getFile());
        dialog.open();
        IPath path = dialog.getResult();
        if (path == null) return;
        ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(getSite().getShell());
        IProgressMonitor progressMonitor = progressMonitorDialog.getProgressMonitor();
        try {
            save(progressMonitor);
            getMultiPageCommandStackListener().markSaveLocations();
        } catch (CoreException e) {
            UIDelegate.displayError("", e.getMessage(), null);
        }
    }

    protected void firePropertyChange(int propertyId) {
        super.firePropertyChange(propertyId);
        updateActions(editorActionIDs);
    }

    /**
	 * Returns the action registry of this editor.
	 * @return the action registry
	 */
    protected ActionRegistry getActionRegistry() {
        if (actionRegistry == null) actionRegistry = new ActionRegistry();
        return actionRegistry;
    }

    public Object getAdapter(Class type) {
        if (type == IPropertySheetPage.class) return getPropertySheetPage(); else if (type == CommandStack.class) return getDelegatingCommandStack(); else if (type == ActionRegistry.class) return getActionRegistry(); else if (type == ZoomManager.class) return getDelegatingZoomManager();
        return super.getAdapter(type);
    }

    /**
	 * Returns the current active page. 
	 * @return the current active page or <code>null</code>
	 */
    private AbstractEditorPage getCurrentPage() {
        if (getActivePage() == -1) return null;
        return (AbstractEditorPage) getEditor(getActivePage());
    }

    /** the delegating ZoomManager */
    private DelegatingZoomManager delegatingZoomManager;

    /**
	 * Returns the <code>DelegatingZoomManager</code> for this editor.
	 * @return the <code>DelegatingZoomManager</code>
	 */
    protected DelegatingZoomManager getDelegatingZoomManager() {
        if (null == delegatingZoomManager) {
            delegatingZoomManager = new DelegatingZoomManager();
            if (null != getCurrentPage() && null != getCurrentPage().getGraphicalViewer()) delegatingZoomManager.setCurrentZoomManager(getZoomManager(getCurrentPage().getGraphicalViewer()));
        }
        return delegatingZoomManager;
    }

    /**
	 * Returns the <code>CommandStack</code> for this editor.
	 * @return the <code>CommandStack</code>
	 */
    protected DelegatingCommandStack getDelegatingCommandStack() {
        if (null == delegatingCommandStack) {
            delegatingCommandStack = new DelegatingCommandStack();
            if (null != getCurrentPage()) delegatingCommandStack.setCurrentCommandStack(getCurrentPage().getCommandStack());
        }
        return delegatingCommandStack;
    }

    /**
	 * Returns the <code>CommandStackListener</code> for 
	 * the <code>DelegatingCommandStack</code>.
	 * @return the <code>CommandStackListener</code>
	 */
    protected CommandStackListener getDelegatingCommandStackListener() {
        return delegatingCommandStackListener;
    }

    /**
	 * Returns the global command stack listener.
	 * @return the <code>CommandStackListener</code>
	 */
    protected MultiPageCommandStackListener getMultiPageCommandStackListener() {
        if (null == multiPageCommandStackListener) multiPageCommandStackListener = new MultiPageCommandStackListener();
        return multiPageCommandStackListener;
    }

    /**
	 * Returns the default <code>PaletteRoot</code> for this editor and all
	 * its pages.
	 * @return the default <code>PaletteRoot</code>
	 */
    protected PaletteRoot getPaletteRoot() {
        if (null == paletteRoot) {
            paletteRoot = new ContractPaletteRoot();
        }
        return paletteRoot;
    }

    /**
	 * Returns the undoable <code>PropertySheetPage</code> for
	 * this editor.
	 * 
	 * @return the undoable <code>PropertySheetPage</code>
	 */
    protected PropertySheetPage getPropertySheetPage() {
        if (null == undoablePropertySheetPage) {
            undoablePropertySheetPage = new PropertySheetPage();
            undoablePropertySheetPage.setRootEntry(new org.eclipse.gef.ui.properties.UndoablePropertySheetEntry(getDelegatingCommandStack()));
        }
        return undoablePropertySheetPage;
    }

    /**
	 * Returns the resource tracker instance
	 * @return
	 */
    private ResourceTracker getResourceTracker() {
        if (resourceTracker == null) {
            resourceTracker = new ResourceTracker();
        }
        return resourceTracker;
    }

    /**
	 * Returns the selection listener.
	 * 
	 * @return the <code>ISelectionListener</code>
	 */
    protected ISelectionListener getSelectionListener() {
        return selectionListener;
    }

    /**
	 * Returns the selection syncronizer object. 
	 * The synchronizer can be used to sync the selection of 2 or more
	 * EditPartViewers.
	 * @return the syncrhonizer
	 */
    protected SelectionSynchronizer getSelectionSynchronizer() {
        if (synchronizer == null) synchronizer = new SelectionSynchronizer();
        return synchronizer;
    }

    /**
	 * Returns the shared KeyHandler that should be used for 
	 * all viewers.
	 * 
	 * @return the shared KeyHandler
	 */
    protected KeyHandler getSharedKeyHandler() {
        if (sharedKeyHandler == null) {
            sharedKeyHandler = new KeyHandler();
            sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
            sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry().getAction(GEFActionConstants.DIRECT_EDIT));
        }
        return sharedKeyHandler;
    }

    /**
	 * Returns the Contract used by this editor.
	 * @return the Contract
	 */
    public ContractImpl getContract() {
        return Contract;
    }

    /**
	 * Returns the page for editing the Contract.
	 * @return the page for editing the Contract
	 */
    private ContractPage getContractPage() {
        return (ContractPage) getEditor(ContractPageID);
    }

    /**
	 * Returns the zoom manager of the specified viewer.
	 * @param viewer the viewer to get the zoom manager from
	 * @return the zoom manager
	 */
    private ZoomManager getZoomManager(GraphicalViewer viewer) {
        RootEditPart rootEditPart = viewer.getRootEditPart();
        ZoomManager zoomManager = null;
        if (rootEditPart instanceof ScalableFreeformRootEditPart) {
            zoomManager = ((ScalableFreeformRootEditPart) rootEditPart).getZoomManager();
        } else if (rootEditPart instanceof ScalableRootEditPart) {
            zoomManager = ((ScalableRootEditPart) rootEditPart).getZoomManager();
        }
        return zoomManager;
    }

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        try {
            if (input instanceof ContractModelInput) {
                modelManager = new ContractModelManager((ContractModelInput) input);
                Contract = (ContractImpl) create();
            } else if (input instanceof IFileEditorInput) {
                IFile file = ((IFileEditorInput) input).getFile();
                ContractModelInput lModelInput = new FileContractModelInput(file);
                modelManager = new ContractModelManager(lModelInput);
                Contract = (ContractImpl) create();
            }
            if (null == getContract() || Contract == null) {
                throw new PartInitException("");
            }
        } catch (CoreException e) {
            throw new PartInitException(e.getStatus());
        } catch (ClassCastException e) {
            throw e;
        } catch (Exception ex) {
            if (modelManager != null) {
                modelManager.handleError(ex);
            }
        }
        super.init(site, input);
        getDelegatingCommandStack().addCommandStackListener(getDelegatingCommandStackListener());
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(getSelectionListener());
        createActions();
    }

    public boolean isDirty() {
        return isDirty;
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);
        currentPageChanged();
    }

    protected void setActivePage(int pageIndex) {
        super.setActivePage(pageIndex);
        currentPageChanged();
    }

    /**
	 * Indicates that the current page has changed.
	 * <p>
	 * We update the DelegatingCommandStack, OutlineViewer
	 * and other things here.
	 */
    protected void currentPageChanged() {
        getDelegatingCommandStack().setCurrentCommandStack(getCurrentPage().getCommandStack());
        getDelegatingZoomManager().setCurrentZoomManager(getZoomManager(getCurrentPage().getGraphicalViewer()));
    }

    /**
	 * Changes the dirty state.
	 * @param dirty
	 */
    private void setDirty(boolean dirty) {
        if (isDirty != dirty) {
            isDirty = dirty;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    protected void setInput(IEditorInput input) {
        if (getEditorInput() != null && getEditorInput() instanceof FileEditorInput) {
            IFile file = ((FileEditorInput) getEditorInput()).getFile();
            file.getWorkspace().removeResourceChangeListener(getResourceTracker());
        }
        super.setInput(input);
        if (getEditorInput() != null && getEditorInput() instanceof FileEditorInput) {
            if (getEditorInput() instanceof FileEditorInput) {
                IFile file = ((FileEditorInput) getEditorInput()).getFile();
                file.getWorkspace().addResourceChangeListener(getResourceTracker());
                setPartName(file.getName());
            }
        }
    }

    /**
	 * Updates the specified actions.
	 * 
	 * @param actionIds the list of ids of actions to update
	 */
    private void updateActions(List actionIds) {
        for (Iterator ids = actionIds.iterator(); ids.hasNext(); ) {
            IAction action = getActionRegistry().getAction(ids.next());
            if (null != action && action instanceof UpdateAction) ((UpdateAction) action).update();
        }
    }

    /**
	 * Returns the Contract object from the specified file.
	 * 
	 * @param file
	 * @return the Contract object from the specified file
	 */
    private ContractImpl create() throws CoreException {
        boolean lCreateIfNotExist = false;
        ContractImpl Contract = null;
        try {
            modelManager.load();
        } catch (Exception e) {
            if (!lCreateIfNotExist) {
                String lMessage = e.getMessage();
                if (lMessage == null || lMessage.length() == 0) {
                    lMessage = Messages.getMessage(ContractUIPlugin.I18N_LOCATION, MessagesConstants.ERROR_LOADING);
                }
                UIDelegate.displayError(lMessage, lMessage, null);
                return null;
            } else {
                if (!UIDelegate.displayYesNo(Messages.getMessage(ContractUIPlugin.I18N_LOCATION, MessagesConstants.DIALOG_OPEN_CONTRACT_LABEL), Messages.getMessage(ContractUIPlugin.I18N_LOCATION, MessagesConstants.DIALOG_OPEN_CONTRACT_MESSAGE), null)) {
                    return null;
                }
                modelManager.createContract();
            }
        }
        Contract = modelManager.getModel();
        TreeIterator lIterator = Contract.eResource().getAllContents();
        while (lIterator.hasNext()) {
            EObject lContent = ((EObject) lIterator.next());
            UIUtil.resolve(lContent);
        }
        if (null == Contract) {
            throw new CoreException(new Status(IStatus.ERROR, ContractUIPlugin.ID, 0, Messages.getMessage(ContractUIPlugin.I18N_LOCATION, MessagesConstants.ERROR_LOADING), null));
        }
        return Contract;
    }

    /**
	 * Saves the Contract under the specified path.
	 * 
	 * @param Contract
	 * @param path
	 *            workspace relative path
	 * @param progressMonitor
	 */
    private void save(IProgressMonitor progressMonitor) throws CoreException {
        if (null == progressMonitor) progressMonitor = new NullProgressMonitor();
        progressMonitor.beginTask(Messages.getMessage(ContractUIPlugin.I18N_LOCATION, MessagesConstants.ACTION_COMMAND_SAVING_CONTRACT), 2);
        if (null == modelManager) {
            IStatus status = new Status(IStatus.ERROR, ContractUIPlugin.ID, 0, Messages.getMessage(ContractUIPlugin.I18N_LOCATION, MessagesConstants.ERROR_STORING), null);
            throw new CoreException(status);
        }
        try {
            modelManager.save();
            progressMonitor.worked(1);
            progressMonitor.done();
        } catch (FileNotFoundException e) {
            IStatus status = new Status(IStatus.ERROR, ContractUIPlugin.ID, 0, Messages.getMessage(ContractUIPlugin.I18N_LOCATION, MessagesConstants.ERROR_STORING), e);
            throw new CoreException(status);
        } catch (IOException e) {
            IStatus status = new Status(IStatus.ERROR, ContractUIPlugin.ID, 0, Messages.getMessage(ContractUIPlugin.I18N_LOCATION, MessagesConstants.ERROR_STORING), e);
            throw new CoreException(status);
        }
    }
}
