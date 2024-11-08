package com.prolix.editor.main.workspace;

import java.util.List;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import uk.ac.reload.straker.datamodel.learningdesign.LearningDesign;
import com.prolix.editor.graph.model.ModelDiagramMain;
import com.prolix.editor.listener.ProlixGLMSaveListener;
import com.prolix.editor.main.GLMEditor;
import com.prolix.editor.main.navi.bar.GLMNavigationBar;
import com.prolix.editor.main.navi.bar.IMainNavigationListener;
import com.prolix.editor.main.navi.bar.id.GLMNavigationKey;
import com.prolix.editor.main.workspace.dialog.WorkspaceProlixDialogHook;
import com.prolix.editor.main.workspace.export.ExportWorkspace;
import com.prolix.editor.main.workspace.prolix.ProlixWorkspace;
import com.prolix.editor.main.workspace.prolix.mainEditor.GEFEditor;
import com.prolix.editor.main.workspace.reload.ReloadWorkspace;
import com.prolix.editor.resourcemanager.zip.LearningDesignDataModel;
import com.prolix.editor.roleview.roles.RoleGroupMain;

public class GLMMainFrame extends Composite implements IMainNavigationListener {

    private ProlixWorkspace prolixWorkspace;

    private ReloadWorkspace reloadWorkspace;

    private ExportWorkspace exportWorkspace;

    private StackLayout stackLayout;

    private GLMEditor owner;

    public GLMMainFrame(Composite parent, GLMEditor owner) {
        super(parent, SWT.NONE);
        this.owner = owner;
        setupView();
        addNavigationListener(this);
        setTopControl(prolixWorkspace);
        new WorkspaceProlixDialogHook(this);
    }

    private void setupView() {
        stackLayout = new StackLayout();
        setLayout(stackLayout);
        prolixWorkspace = new ProlixWorkspace(this);
        reloadWorkspace = new ReloadWorkspace(this);
        exportWorkspace = new ExportWorkspace(this);
    }

    private void setTopControl(Composite composite) {
        stackLayout.topControl = composite;
        layout();
    }

    public void update(GLMNavigationKey key) {
        if (key.isGroup(GLMNavigationKey.group_prolix)) setTopControl(prolixWorkspace);
        if (key.isGroup(GLMNavigationKey.group_reload)) setTopControl(reloadWorkspace);
        if (key.isGroup(GLMNavigationKey.group_export)) setTopControl(exportWorkspace);
        if (key.isType(GLMNavigationKey.key_overview)) getLearningDesignDataModel().setDirty(true);
    }

    public void addNavigationListener(IMainNavigationListener listener) {
        owner.addNavigationListener(listener);
    }

    public void removeNavigationListener(IMainNavigationListener listener) {
        owner.removeNavigationListener(listener);
    }

    public LearningDesign getLearningDesign() {
        return owner.getLearningDesign();
    }

    public LearningDesignDataModel getLearningDesignDataModel() {
        return owner.getLearningDesignDataModel();
    }

    public CommandStack getCommandStack() {
        return owner.getCommandStack();
    }

    public DefaultEditDomain getEditDomain() {
        return owner.getEditDomain();
    }

    public IWorkbenchPartSite getSite() {
        return owner.getSite();
    }

    public ActionRegistry getActionRegistry() {
        return owner.getActionRegistry();
    }

    public List getPropertyActions() {
        return owner.getPropertyActions();
    }

    public List getSelectionActions() {
        return owner.getSelectionActions();
    }

    public List getStackActions() {
        return owner.getStackActions();
    }

    public GLMEditor getEditor() {
        return owner;
    }

    public ModelDiagramMain getModel() {
        return owner.getModel();
    }

    public RoleGroupMain getRoles() {
        return owner.getRoles();
    }

    public LearningDesignDataModel getContainer() {
        return owner.getContainer();
    }

    public GLMNavigationBar getNavigationBar() {
        return owner.getNavigationBar();
    }

    public GEFEditor getGEFEditor() {
        return this.prolixWorkspace.getGEFEditor();
    }

    public void createActions() {
        prolixWorkspace.createActions();
    }

    public GraphicalViewer getGraphicalViewer() {
        return prolixWorkspace.getGraphicalViewer();
    }

    /**
	 * @param listener
	 * @see com.prolix.editor.main.GLMEditor#addProlixGLMSaveListener(com.prolix.editor.listener.ProlixGLMSaveListener)
	 */
    public void addProlixGLMSaveListener(ProlixGLMSaveListener listener) {
        owner.addProlixGLMSaveListener(listener);
    }

    /**
	 * @param listener
	 * @see com.prolix.editor.main.GLMEditor#removeProlixGLMSaveListener(com.prolix.editor.listener.ProlixGLMSaveListener)
	 */
    public void removeProlixGLMSaveListener(ProlixGLMSaveListener listener) {
        owner.removeProlixGLMSaveListener(listener);
    }
}
