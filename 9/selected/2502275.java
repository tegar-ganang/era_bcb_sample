package com.prolix.editor.main;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import uk.ac.reload.straker.datamodel.IDataFileListener;
import uk.ac.reload.straker.datamodel.learningdesign.LearningDesign;
import com.prolix.editor.graph.model.ModelDiagramMain;
import com.prolix.editor.listener.ProlixGLMSaveListener;
import com.prolix.editor.main.navi.bar.GLMNavigationBar;
import com.prolix.editor.main.navi.bar.IMainNavigationListener;
import com.prolix.editor.main.workspace.GLMMainFrame;
import com.prolix.editor.main.workspace.prolix.mainEditor.GEFEditor;
import com.prolix.editor.resourcemanager.zip.LearningDesignDataModel;
import com.prolix.editor.roleview.roles.RoleGroupMain;
import com.prolix.editor.systempreferences.AutoHintMessages;

public class GLMEditor extends EditorPart implements CommandStackListener, ISelectionListener {

    public static final String id = "com.prolix.editor.main.glmeditor";

    public static final int barsize = 100;

    private GLMNavigationBar navigationBar;

    private GLMMainFrame mainFrame;

    private DefaultEditDomain editDomain;

    private ActionRegistry actionRegistry;

    private List selectionActions = new ArrayList();

    private List stackActions = new ArrayList();

    private List propertyActions = new ArrayList();

    private LearningDesignDataModel container;

    private List prolixGLMSaveListener;

    private boolean dontShowAutoText;

    public void showAutoText() {
        if (dontShowAutoText) return;
        dontShowAutoText = true;
        AutoHintMessages.showEditorStart();
    }

    public GLMEditor() {
        editDomain = new DefaultEditDomain(this);
        prolixGLMSaveListener = new ArrayList();
    }

    public void doSave(IProgressMonitor monitor) {
        fireProlixGLMSaveListener();
        getLearningDesignDataModel().save();
        getCommandStack().markSaveLocation();
        container.setDirty(false);
        firePropertyChange(IEditorPart.PROP_DIRTY);
    }

    public void doSaveAs() {
    }

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (!(input instanceof GLMEditorInput)) {
            System.err.println("Error!!!");
            throw new PartInitException("Wrong Editor Input");
        }
        container = ((GLMEditorInput) input).getContainer();
        container.setEditor(this);
        setPartName(container.getTitle());
        setSite(site);
        setInput(input);
        container.addIDataFileListener(new IDataFileListener() {

            public void fileDirty() {
                firePropertyChange(IEditorPart.PROP_DIRTY);
            }

            public void fileLoaded() {
            }

            public void fileSaved() {
            }
        });
    }

    public void updatePartName() {
        setPartName(container.getTitle());
    }

    public boolean isDirty() {
        return getCommandStack().isDirty() || container.isDirty();
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    public void createPartControl(Composite parent) {
        Composite main = new Composite(parent, SWT.NONE);
        GridLayout layout = GridLayoutFactory.fillDefaults().create();
        layout.numColumns = 2;
        main.setLayout(layout);
        GridData gridData;
        navigationBar = new GLMNavigationBar(main);
        gridData = new GridData(GridData.FILL_VERTICAL);
        gridData.widthHint = barsize;
        navigationBar.setLayoutData(gridData);
        mainFrame = new GLMMainFrame(main, this);
        gridData = new GridData(GridData.FILL_BOTH);
        mainFrame.setLayoutData(gridData);
        initializeActionRegistry();
        navigationBar.fireInitNavigation();
    }

    public void addNavigationListener(IMainNavigationListener listener) {
        if (navigationBar == null) throw new RuntimeException("navi bar == NULL should not happen");
        navigationBar.addNavigationListener(listener);
    }

    public void removeNavigationListener(IMainNavigationListener listener) {
        if (navigationBar == null) throw new RuntimeException("navi bar == NULL should not happen");
        navigationBar.removeNavigationListener(listener);
    }

    public void setFocus() {
        getGEFEditor().setFocus();
        getGEFEditor().getScrollbarOffset();
    }

    public void dispose() {
        container.dispose();
        super.dispose();
    }

    public LearningDesign getLearningDesign() {
        return container.getLearningDesign();
    }

    public LearningDesignDataModel getLearningDesignDataModel() {
        return container;
    }

    public ModelDiagramMain getModel() {
        return container.getModel();
    }

    public RoleGroupMain getRoles() {
        return container.getRoleGroupMain();
    }

    public LearningDesignDataModel getContainer() {
        return container;
    }

    public DefaultEditDomain getEditDomain() {
        return editDomain;
    }

    public CommandStack getCommandStack() {
        return editDomain.getCommandStack();
    }

    public ActionRegistry getActionRegistry() {
        if (actionRegistry == null) actionRegistry = new ActionRegistry();
        return actionRegistry;
    }

    public List getSelectionActions() {
        return selectionActions;
    }

    public List getStackActions() {
        return stackActions;
    }

    public List getPropertyActions() {
        return propertyActions;
    }

    public GLMNavigationBar getNavigationBar() {
        return navigationBar;
    }

    public GEFEditor getGEFEditor() {
        return this.mainFrame.getGEFEditor();
    }

    protected void updateActions(List actionIds) {
        ActionRegistry registry = getActionRegistry();
        Iterator iter = actionIds.iterator();
        while (iter.hasNext()) {
            IAction action = registry.getAction(iter.next());
            if (action instanceof UpdateAction) ((UpdateAction) action).update();
        }
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (this.equals(getSite().getPage().getActiveEditor())) updateActions(selectionActions);
    }

    public void commandStackChanged(EventObject event) {
        firePropertyChange(IEditorPart.PROP_DIRTY);
        updateActions(stackActions);
    }

    public GraphicalViewer getGraphicalViewer() {
        return this.mainFrame.getGraphicalViewer();
    }

    private void initializeActionRegistry() {
        mainFrame.createActions();
        updateActions(propertyActions);
        updateActions(stackActions);
        registerListener();
    }

    private void registerListener() {
        editDomain.getCommandStack().addCommandStackListener(this);
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
    }

    public Object getAdapter(Class adapter) {
        if (adapter == GraphicalViewer.class || adapter == EditPartViewer.class) return mainFrame.getGraphicalViewer();
        if (adapter == CommandStack.class) return getCommandStack();
        if (adapter == EditDomain.class) return getEditDomain();
        if (adapter == ActionRegistry.class) return getActionRegistry();
        return super.getAdapter(adapter);
    }

    public void addProlixGLMSaveListener(ProlixGLMSaveListener listener) {
        if (listener == null) return;
        prolixGLMSaveListener.add(listener);
    }

    public void removeProlixGLMSaveListener(ProlixGLMSaveListener listener) {
        prolixGLMSaveListener.remove(listener);
    }

    private void fireProlixGLMSaveListener() {
        Iterator it = prolixGLMSaveListener.iterator();
        while (it.hasNext()) ((ProlixGLMSaveListener) it.next()).finalise();
    }
}
