package com.prolix.editor.main.workspace.prolix;

import java.util.List;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionFactory;
import uk.ac.reload.straker.datamodel.learningdesign.LearningDesign;
import com.prolix.editor.graph.actions.CopyModelAction;
import com.prolix.editor.graph.actions.EditInteractionActivityAction;
import com.prolix.editor.graph.actions.EditModelAction;
import com.prolix.editor.graph.actions.PasteModelAction;
import com.prolix.editor.graph.actions.RenameModelAction;
import com.prolix.editor.graph.actions.SetEnvironmentsAction;
import com.prolix.editor.graph.actions.SetNumberToSelectAction;
import com.prolix.editor.graph.model.ModelDiagramMain;
import com.prolix.editor.main.navi.bar.IMainNavigationListener;
import com.prolix.editor.main.workspace.GLMMainFrame;
import com.prolix.editor.main.workspace.prolix.mainEditor.GEFEditor;
import com.prolix.editor.main.workspace.prolix.menue.InstanceTreeComposite;
import com.prolix.editor.resourcemanager.zip.LearningDesignDataModel;
import com.prolix.editor.roleview.roles.RoleGroupMain;

public class ProlixWorkspace extends SashForm {

    private InstanceTreeComposite naviComposite;

    private GEFEditor editor;

    private GLMMainFrame parent;

    public ProlixWorkspace(GLMMainFrame parent) {
        super(parent, SWT.NONE);
        this.parent = parent;
        setupView();
        addNavigationListener(naviComposite);
    }

    private void setupView() {
        naviComposite = new InstanceTreeComposite(this);
        editor = new GEFEditor(this);
        editor.setBackground(ColorConstants.cyan);
        setWeights(new int[] { 2, 8 });
    }

    public void addNavigationListener(IMainNavigationListener listener) {
        parent.addNavigationListener(listener);
    }

    public void removeNavigationListener(IMainNavigationListener listener) {
        parent.removeNavigationListener(listener);
    }

    public CommandStack getCommandStack() {
        return parent.getCommandStack();
    }

    public LearningDesign getLearningDesign() {
        return parent.getLearningDesign();
    }

    public DefaultEditDomain getEditDomain() {
        return parent.getEditDomain();
    }

    public IWorkbenchPartSite getSite() {
        return parent.getSite();
    }

    public void createActions() {
        ActionRegistry registry = getActionRegistry();
        List selectionActions = parent.getSelectionActions();
        List propertyActions = parent.getPropertyActions();
        List stackActions = parent.getStackActions();
        IAction action;
        action = new DeleteAction((IWorkbenchPart) parent.getEditor());
        registry.registerAction(action);
        selectionActions.add(action.getId());
        action = new SaveAction(parent.getEditor());
        registry.registerAction(action);
        propertyActions.add(action.getId());
        action = new EditModelAction((IWorkbenchPart) parent.getEditor());
        registry.registerAction(action);
        selectionActions.add(action.getId());
        action = new RenameModelAction((IWorkbenchPart) parent.getEditor(), getCommandStack());
        registry.registerAction(action);
        selectionActions.add(action.getId());
        action = new SetNumberToSelectAction((IWorkbenchPart) parent.getEditor());
        registry.registerAction(action);
        selectionActions.add(action.getId());
        action = new SetEnvironmentsAction((IWorkbenchPart) parent.getEditor(), getCommandStack());
        registry.registerAction(action);
        selectionActions.add(action.getId());
        action = new EditInteractionActivityAction((IWorkbenchPart) parent.getEditor(), getCommandStack());
        registry.registerAction(action);
        selectionActions.add(action.getId());
        action = new UndoAction(parent.getEditor());
        registry.registerAction(action);
        stackActions.add(action.getId());
        action = new RedoAction(parent.getEditor());
        registry.registerAction(action);
        stackActions.add(action.getId());
        action = new CopyModelAction(parent.getEditor());
        registry.registerAction(action);
        selectionActions.add(action.getId());
        action = new PasteModelAction(parent.getEditor());
        registry.registerAction(action);
        stackActions.add(action.getId());
        editor.getGraphicalViewer().setKeyHandler(getKeyHandler());
    }

    private KeyHandler getKeyHandler() {
        GraphicalViewerKeyHandler keyHandler = new GraphicalViewerKeyHandler(editor.getGraphicalViewer());
        keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), parent.getActionRegistry().getAction(ActionFactory.DELETE.getId()));
        keyHandler.put(KeyStroke.getPressed((char) 3, 99, 262144), parent.getActionRegistry().getAction(CopyModelAction.ID));
        keyHandler.put(KeyStroke.getPressed((char) 22, 118, 262144), parent.getActionRegistry().getAction(PasteModelAction.ID));
        return keyHandler;
    }

    public ActionRegistry getActionRegistry() {
        return parent.getActionRegistry();
    }

    public GraphicalViewer getGraphicalViewer() {
        return editor.getGraphicalViewer();
    }

    public GEFEditor getGEFEditor() {
        return this.editor;
    }

    public ModelDiagramMain getModel() {
        return parent.getModel();
    }

    public RoleGroupMain getRoles() {
        return parent.getRoles();
    }

    /**
	 * @return
	 * @see com.prolix.editor.main.workspace.GLMMainFrame#getContainer()
	 */
    public LearningDesignDataModel getContainer() {
        return parent.getContainer();
    }
}
