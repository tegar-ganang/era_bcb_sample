package org.cubictest.ui.gef.editors;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import org.cubictest.model.Test;
import org.cubictest.persistence.TestPersistance;
import org.cubictest.pluginsupport.CustomElementLoader;
import org.cubictest.resources.ResourceMonitor;
import org.cubictest.resources.interfaces.IResourceMonitor;
import org.cubictest.ui.gef.actions.AddExtensionPointAction;
import org.cubictest.ui.gef.actions.CopyAction;
import org.cubictest.ui.gef.actions.CutAction;
import org.cubictest.ui.gef.actions.PasteAction;
import org.cubictest.ui.gef.actions.PopulateCommonAction;
import org.cubictest.ui.gef.actions.PresentAction;
import org.cubictest.ui.gef.actions.ResetTestAction;
import org.cubictest.ui.gef.actions.RunCubicUnitAction;
import org.cubictest.ui.gef.actions.TestContextMenuProvider;
import org.cubictest.ui.gef.dnd.DataEditDropTargetListner;
import org.cubictest.ui.gef.dnd.FileTransferDropTargetListener;
import org.cubictest.ui.gef.factory.PaletteRootCreator;
import org.cubictest.ui.gef.factory.TestEditPartFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.EditorPartAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/** 
 * @author SK Skytteren
 *
 * The graphical editor for editing the tests. 
 */
public class GraphicalTestEditor extends EditorPart implements IAdaptable, ITabbedPropertySheetPageContributor {

    private GraphicalViewer graphicalViewer;

    private EditDomain editDomain;

    private boolean isDirty;

    private CommandStackListener commandStackListener = new CommandStackListener() {

        public void commandStackChanged(EventObject event) {
            updateActions(stackActionIDs);
            setDirty(getCommandStack().isDirty());
        }
    };

    private ISelectionListener selectionListener = new ISelectionListener() {

        public void selectionChanged(IWorkbenchPart part, ISelection selection) {
            updateActions(editPartActionIDs);
        }
    };

    private PaletteViewer paletteViewer;

    private PaletteRoot paletteRoot;

    private ActionRegistry actionRegistry;

    private List<String> editPartActionIDs = new ArrayList<String>();

    private List<String> stackActionIDs = new ArrayList<String>();

    private List<String> editorActionIDs = new ArrayList<String>();

    private TestOverviewOutlinePage testOverviewOutlinePage;

    private IPropertySheetPage undoablePropertySheetPage;

    private IResourceMonitor resourceMonitor;

    private CustomElementLoader customTestStepLoader;

    /**
	 * Constructor.
	 */
    public GraphicalTestEditor() {
        super();
    }

    public ActionRegistry getActionRegistry() {
        if (actionRegistry == null) {
            actionRegistry = new ActionRegistry();
        }
        return actionRegistry;
    }

    public CommandStack getCommandStack() {
        return getEditDomain().getCommandStack();
    }

    protected CommandStackListener getCommandStackListener() {
        return commandStackListener;
    }

    public void createPartControl(Composite parent) {
        SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
        paletteViewer = createPaletteViewer(sashForm);
        graphicalViewer = createGraphicalViewer(sashForm);
        sashForm.setWeights(new int[] { 25, 75 });
    }

    protected GraphicalViewer createGraphicalViewer(Composite parent) {
        GraphicalViewer viewer = new ScrollingGraphicalViewer();
        viewer.createControl(parent);
        viewer.getControl().setBackground(parent.getBackground());
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        GraphicalViewerKeyHandler graphicalViewerKeyHandler = new GraphicalViewerKeyHandler(viewer);
        KeyHandler parentKeyHandler = graphicalViewerKeyHandler.setParent(getCommonKeyHandler());
        viewer.setKeyHandler(parentKeyHandler);
        getEditDomain().addViewer(viewer);
        getSite().setSelectionProvider(viewer);
        ContextMenuProvider provider = new TestContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(provider);
        getSite().registerContextMenu("cubicTestPlugin.editor.contextmenu", provider, viewer);
        viewer.addDropTargetListener(new DataEditDropTargetListner(((IFileEditorInput) getEditorInput()).getFile().getProject(), viewer));
        viewer.addDropTargetListener(new FileTransferDropTargetListener(viewer));
        viewer.setEditPartFactory(getEditPartFactory());
        viewer.setContents(getContent());
        return viewer;
    }

    public GraphicalViewer getGraphicalViewer() {
        return graphicalViewer;
    }

    private Test getContent() {
        Test test = TestPersistance.loadFromFile(((IFileEditorInput) getEditorInput()).getFile());
        test.setResourceMonitor(resourceMonitor);
        test.setCustomTestStepLoader(customTestStepLoader);
        return test;
    }

    public IResourceMonitor getResourceMonitor() {
        if (resourceMonitor == null) {
            resourceMonitor = new ResourceMonitor(((IFileEditorInput) getEditorInput()).getFile().getProject());
        }
        return resourceMonitor;
    }

    /**
	 * @return
	 */
    private EditPartFactory getEditPartFactory() {
        return new TestEditPartFactory();
    }

    /**
	 * The <code>MultiPageEditorPart</code> implementation of this 
	 * <code>IWorkbenchPart</code> method disposes all nested editors.
	 * Subclasses may extend.
	 */
    public void dispose() {
        getCommandStack().removeCommandStackListener(getCommandStackListener());
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(getSelectionListener());
        getActionRegistry().dispose();
        getResourceMonitor().dispose();
        super.dispose();
    }

    public Test getTest() {
        EditPart part = graphicalViewer.getContents();
        return (Test) part.getModel();
    }

    /**
	 * Saves the multi-page editor's document.
	 */
    public void doSave(IProgressMonitor monitor) {
        TestPersistance.saveToFile((Test) graphicalViewer.getContents().getModel(), ((IFileEditorInput) getEditorInput()).getFile());
        getCommandStack().markSaveLocation();
        try {
            ((IFileEditorInput) getEditorInput()).getFile().refreshLocal(1, monitor);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Unsupported and throws an <code>UnsupportedOperationException</code>
	 */
    public void doSaveAs() {
        throw new UnsupportedOperationException();
    }

    public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
        if (!(editorInput instanceof IFileEditorInput)) {
            throw new PartInitException("Input must be a valid file.");
        }
        setSite(site);
        setInput(editorInput);
        setPartName(editorInput.getName());
        getCommandStack().addCommandStackListener(getCommandStackListener());
        getCommandStack().setUndoLimit(16);
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(getSelectionListener());
        createActions();
    }

    private void createActions() {
        addStackAction(new UndoAction(this));
        addStackAction(new RedoAction(this));
        addEditPartAction(new CutAction((IWorkbenchPart) this));
        addEditPartAction(new CopyAction((IWorkbenchPart) this));
        addEditPartAction(new PasteAction((IWorkbenchPart) this));
        addEditPartAction(new DeleteAction((IWorkbenchPart) this));
        addEditPartAction(new DirectEditAction((IWorkbenchPart) this));
        addEditPartAction(new PresentAction((IWorkbenchPart) this));
        addEditPartAction(new PopulateCommonAction((IWorkbenchPart) this));
        addEditPartAction(new AddExtensionPointAction((IWorkbenchPart) this));
        addEditorAction(new SaveAction(this));
        addAction(new PrintAction(this));
        addEditorAction(new RunCubicUnitAction(this));
        addEditorAction(new ResetTestAction(this));
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    public EditDomain getEditDomain() {
        if (editDomain == null) {
            editDomain = new DefaultEditDomain(this);
        }
        return editDomain;
    }

    public boolean isDirty() {
        return isDirty;
    }

    /**
	 * @param isDirty The isDirty to set.
	 */
    protected void setDirty(boolean dirty) {
        if (isDirty != dirty) {
            isDirty = dirty;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public void setFocus() {
    }

    public Object getAdapter(Class adapter) {
        if (adapter == GraphicalViewer.class || adapter == EditPartViewer.class) return getGraphicalViewer();
        if (adapter == CommandStack.class) return getCommandStack();
        if (adapter == EditDomain.class) return getEditDomain();
        if (adapter == ActionRegistry.class) return getActionRegistry();
        if (adapter == IPropertySheetPage.class) return getPropertySheetPage();
        if (adapter == IContentOutlinePage.class) return getTestOverviewOutlinePage();
        if (adapter == ZoomManager.class) return ((ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();
        return super.getAdapter(adapter);
    }

    private PaletteViewer createPaletteViewer(Composite parent) {
        PaletteViewer viewer = new PaletteViewer();
        viewer.createControl(parent);
        getEditDomain().setPaletteViewer(viewer);
        getEditDomain().setPaletteRoot(getPaletteRoot());
        viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
        return viewer;
    }

    protected PaletteRoot getPaletteRoot() {
        if (paletteRoot == null) {
            paletteRoot = new PaletteRootCreator(((IFileEditorInput) getEditorInput()).getFile().getProject(), getCustomTestStepLoader());
        }
        return paletteRoot;
    }

    public CustomElementLoader getCustomTestStepLoader() {
        if (customTestStepLoader == null) {
            customTestStepLoader = new CustomElementLoader(((IFileEditorInput) getEditorInput()).getFile().getProject(), getResourceMonitor());
        }
        return customTestStepLoader;
    }

    protected PaletteViewer getPaletteViewer() {
        return paletteViewer;
    }

    /**
	 * Adds a <code>SelectionAction</code>.
	 * @param action
	 */
    protected void addEditPartAction(SelectionAction action) {
        getActionRegistry().registerAction(action);
        editPartActionIDs.add(action.getId());
    }

    /**
	 * Adds a <code>StackAction</code>.
	 * @param action
	 */
    protected void addStackAction(StackAction action) {
        getActionRegistry().registerAction(action);
        stackActionIDs.add(action.getId());
    }

    /**
	 * Adds an <code>EditorPartAction</code>.
	 * @param action
	 */
    protected void addEditorAction(EditorPartAction action) {
        getActionRegistry().registerAction(action);
        editorActionIDs.add(action.getId());
    }

    /**
	 * Used to add an action.
	 * @param action
	 */
    protected void addAction(IAction action) {
        getActionRegistry().registerAction(action);
    }

    private void updateActions(List actionIds) {
        for (int i = 0; i < actionIds.size(); i++) {
            IAction action = getActionRegistry().getAction(actionIds.get(i));
            if (action != null && action instanceof UpdateAction) {
                ((UpdateAction) action).update();
            }
        }
    }

    protected ISelectionListener getSelectionListener() {
        return selectionListener;
    }

    protected void firePropertyChange(int propertyId) {
        super.firePropertyChange(propertyId);
        updateActions(editorActionIDs);
    }

    protected KeyHandler getCommonKeyHandler() {
        KeyHandler sharedKeyHandler = new KeyHandler();
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry().getAction(GEFActionConstants.DIRECT_EDIT));
        return sharedKeyHandler;
    }

    protected TestOverviewOutlinePage getTestOverviewOutlinePage() {
        if (null == testOverviewOutlinePage && null != getGraphicalViewer()) {
            RootEditPart rootEditPart = getGraphicalViewer().getRootEditPart();
            if (rootEditPart instanceof ScalableFreeformRootEditPart) {
                testOverviewOutlinePage = new TestOverviewOutlinePage((ScalableFreeformRootEditPart) rootEditPart);
            }
        }
        return testOverviewOutlinePage;
    }

    protected IPropertySheetPage getPropertySheetPage() {
        if (null == undoablePropertySheetPage) {
            undoablePropertySheetPage = new TabbedPropertySheetPage(this);
        }
        return undoablePropertySheetPage;
    }

    public String getContributorId() {
        return getSite().getId();
    }
}
