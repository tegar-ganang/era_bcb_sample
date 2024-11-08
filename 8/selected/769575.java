package com.keggview.application.editors;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.SelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.EditorPartAction;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.gef.ui.actions.StackAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import com.keggview.application.Activator;
import com.keggview.application.views.OverviewOutlinePage;

public class EditorGraphView extends EditorPart {

    /**
	 * the <code>EditDomain</code> will be initialized lazily
	 */
    private EditDomain editDomain;

    /** the dirty state */
    private boolean isDirty;

    private GraphicalViewer graphicalViewer;

    private PaletteViewer paletteViewer;

    private PaletteRoot paletteRoot;

    private ActionRegistry actionRegistry;

    private List<String> editPartActionIDs = new ArrayList<String>();

    private List<String> stackActionIDs = new ArrayList<String>();

    private List<String> editorActionIDs = new ArrayList<String>();

    private PropertySheetPage undoablePropertySheetPage;

    private OverviewOutlinePage overviewOutlinePage;

    private ISelectionListener selectionListener = new ISelectionListener() {

        @Override
        public void selectionChanged(IWorkbenchPart part, ISelection selection) {
            updateActions(editPartActionIDs);
        }
    };

    /**
	 * return selection listener
	 * @return
	 */
    protected ISelectionListener getSelectionListener() {
        return selectionListener;
    }

    /**
	 * the <code>CommandStackListener</code> that listens for
	 * <code>CommandStack</code> changes
	 * 
	 */
    private CommandStackListener commandStackListener = new CommandStackListener() {

        /**
		 * set as dirty editor is commandstack will be dirty (has been changed)
		 * 
		 */
        @Override
        public void commandStackChanged(EventObject arg0) {
            updateActions(stackActionIDs);
            setDirty(getCommandStack().isDirty());
        }
    };

    /**
	 * Returns action registry of this editor
	 * 
	 * @return
	 */
    public ActionRegistry getActionRegistry() {
        if (actionRegistry == null) {
            actionRegistry = new ActionRegistry();
        }
        return actionRegistry;
    }

    /**
	 * returns the <code>CommandStack</code> of this editor's
	 * <code>EditDomain</code>
	 * 
	 * @return the <code>CommandStack</code>
	 */
    public CommandStack getCommandStack() {
        return getEditDomain().getCommandStack();
    }

    /**
	 * returns <code>CommandStackListener</code>.
	 * 
	 * @return the <code>CommandStackListener</code>.
	 */
    protected CommandStackListener getCommandStackListener() {
        return commandStackListener;
    }

    /**
	 * TODO: Implement "doSave"
	 * 
	 * @see EditorPart#doSave(IProgressMonitor)
	 */
    @Override
    public void doSave(IProgressMonitor monitor) {
        getCommandStack().markSaveLocation();
    }

    /**
	 * TODO: Implement "doSaveAs"
	 * 
	 * @see EditorPart#doSaveAs()
	 */
    @Override
    public void doSaveAs() {
        getCommandStack().markSaveLocation();
    }

    /**
	 * initializes the editor.
	 * 
	 * @see EditorPart#init(IEditorSite, IEditorInput)
	 */
    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        getCommandStack().addCommandStackListener(this.getCommandStackListener());
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(getSelectionListener());
        createActions();
    }

    protected void createActions() {
        addStackAction(new UndoAction(this));
        addStackAction(new RedoAction(this));
        addEditPartAction(new DeleteAction((IWorkbenchPart) this));
        addEditorAction(new SaveAction(this));
        addAction(new PrintAction(this));
    }

    /**
	 * Indicates if editor has unsaved changes
	 * 
	 * @see EditorPart#isDirty()
	 */
    @Override
    public boolean isDirty() {
        return this.isDirty;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(Composite parent) {
        SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
        sashForm.setWeights(new int[] { 30, 70 });
        paletteViewer = createPaletteViewer(parent);
        graphicalViewer = new ScrollingGraphicalViewer();
    }

    @Override
    public void setFocus() {
    }

    /**
	 * Returns the <code>EditDomain</code> used by this editor
	 * 
	 * @return the <code>EditDomain</code> used by this editor
	 */
    public EditDomain getEditDomain() {
        if (editDomain == null) {
            editDomain = new DefaultEditDomain(this);
        }
        return editDomain;
    }

    /**
	 * Sets the dirty state of this editor
	 * <p>
	 * An event will be fired immediately it the new state is different than
	 * currend one.
	 * </p>
	 * 
	 * @param isDirty
	 *            the new dirty state to set
	 */
    protected void setDirty(boolean isDirty) {
        if (isDirty != this.isDirty) {
            this.isDirty = isDirty;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    /**
	 * remove listener at the end of editor
	 */
    public void dispose() {
        getCommandStack().removeCommandStackListener(getCommandStackListener());
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(getSelectionListener());
        getActionRegistry().dispose();
        super.dispose();
    }

    @SuppressWarnings("unused")
    private GraphicalViewer createGraphicalViewer(Composite parent) {
        GraphicalViewer viewer = new ScrollingGraphicalViewer();
        viewer.createControl(parent);
        viewer.getControl().setBackground(parent.getBackground());
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        getEditDomain().addViewer(viewer);
        getSite().setSelectionProvider(viewer);
        viewer.setEditPartFactory(getEditPartFactory());
        viewer.setContents(getContent());
        return viewer;
    }

    /**
	 * create a new <code>PaletteViewer</code>, configures, registers &
	 * initializes it
	 * 
	 * @param parent
	 *            the parent composite
	 * @return a new <code>PaletteViewer</code>
	 */
    private PaletteViewer createPaletteViewer(Composite parent) {
        PaletteViewer viewer = new PaletteViewer();
        viewer.createControl(parent);
        getEditDomain().addViewer(viewer);
        getEditDomain().setPaletteRoot(getPaletteRoot());
        return viewer;
    }

    /**
	 * returns the <code>PaletteRoot</code> this editor's palette uses.
	 * 
	 * @return the <code>PaletteRoot</code> this editor's palette uses.
	 */
    protected PaletteRoot getPaletteRoot() {
        if (this.paletteRoot == null) {
            this.paletteRoot = new PaletteRoot();
            List<Object> categories = new ArrayList<Object>();
            PaletteGroup controls = new PaletteGroup("Controls");
            ToolEntry tool = new SelectionToolEntry();
            controls.add(tool);
            paletteRoot.setDefaultEntry(tool);
            controls.add(new MarqueeToolEntry());
            PaletteSeparator separator = new PaletteSeparator(Activator.PLUGIN_ID + ".palette.separator");
            separator.setUserModificationPermission(PaletteEntry.PERMISSION_NO_MODIFICATION);
            controls.add(separator);
            ToolEntry creationConn = new ConnectionCreationToolEntry("Connections", "Create Connections", null, ImageDescriptor.createFromFile(getClass(), "icons/connection16.gif"), ImageDescriptor.createFromFile(getClass(), "icons/connection24.gif"));
            controls.add(creationConn);
            paletteRoot.addAll(categories);
        }
        return paletteRoot;
    }

    /**
	 * returns the <code>GraphicalViewer</code> of this editor
	 * 
	 * @return the <code>GraphicalViewer</code> of this editor
	 */
    public GraphicalViewer getGraphicalViewer() {
        return graphicalViewer;
    }

    protected Object getContent() {
        return null;
    }

    protected EditPartFactory getEditPartFactory() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == GraphicalViewer.class || adapter == EditPartViewer.class) return getGraphicalViewer(); else if (adapter == CommandStack.class) return this.getCommandStack(); else if (adapter == EditDomain.class) return this.getEditDomain(); else if (adapter == ActionRegistry.class) return this.actionRegistry; else if (adapter == IPropertySheetPage.class) return getPropertySheetPage(); else if (adapter == IContentOutlinePage.class) {
            return getOverviewOutlinePage();
        }
        return super.getAdapter(adapter);
    }

    /**
	 * Returns the <code>PaletteViewer</code> of this editor
	 * 
	 * @return the <code>PaletteViewer</code> of this editor
	 */
    public PaletteViewer getPaletteViewer() {
        return paletteViewer;
    }

    /**
	 * Add an <code>EditPart</code> action to this editor.
	 * <p>
	 * <code>EditPart</code> actions are actions that depend and work on the
	 * selected <code>EditPart</code>s.
	 * 
	 * @param action
	 *            the <code>EditPart</code> action
	 */
    protected void addEditPartAction(SelectionAction action) {
        getActionRegistry().registerAction(action);
        editPartActionIDs.add(action.getId());
    }

    /**
	 * Adds an <code>CommandStack</code> action to this editor.
	 * <p>
	 * <code>CommandStack</code> actions are actions that depend and work on
	 * the <code>CommandStack</code>.
	 * 
	 * @param action
	 *            the <code>CommandStack</code> action
	 */
    protected void addStackAction(StackAction action) {
        getActionRegistry().registerAction(action);
        editPartActionIDs.add(action.getId());
    }

    protected void addEditorAction(EditorPartAction action) {
        getActionRegistry().registerAction(action);
        editPartActionIDs.add(action.getId());
    }

    /**
	 * add an action to this editor's ActionRegistry.
	 * 
	 * @param action
	 *            the action to add
	 */
    protected void addAction(IAction action) {
        getActionRegistry().registerAction(action);
    }

    /**
	 * Updates the specified actions.
	 * 
	 * @param actionIds
	 *            the list of ids of actions to update
	 */
    private void updateActions(List<String> actionIds) {
        for (Iterator<String> ids = actionIds.iterator(); ids.hasNext(); ) {
            IAction action = getActionRegistry().getAction(ids.next());
            if (null != action && action instanceof UpdateAction) ((UpdateAction) action).update();
        }
    }

    protected void firePropertyChange(int propertyId) {
        super.firePropertyChange(propertyId);
        updateActions(editorActionIDs);
    }

    protected PropertySheetPage getPropertySheetPage() {
        if (this.undoablePropertySheetPage == null) {
            this.undoablePropertySheetPage = new PropertySheetPage();
            this.undoablePropertySheetPage.setRootEntry(new UndoablePropertySheetEntry(getCommandStack()));
        }
        return this.undoablePropertySheetPage;
    }

    private OverviewOutlinePage getOverviewOutlinePage() {
        if ((this.overviewOutlinePage == null) && (getGraphicalViewer() != null)) {
            RootEditPart root = getGraphicalViewer().getRootEditPart();
            if (root instanceof ScalableFreeformRootEditPart) {
                this.overviewOutlinePage = new OverviewOutlinePage((ScalableFreeformRootEditPart) root);
            }
        }
        return this.overviewOutlinePage;
    }
}
