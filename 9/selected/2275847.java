package edu.thu.keg.iw.app.description.ui.editor;

import java.util.EventObject;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import edu.thu.keg.iw.app.description.model.flow.Diagram;
import edu.thu.keg.iw.app.description.model.flow.FlowModel;
import edu.thu.keg.iw.app.description.ui.editor.flow.palette.PaletteFactory;
import edu.thu.keg.iw.app.description.ui.editor.flow.palette.dnd.DiagramTemplateTransferDropTargetListener;
import edu.thu.keg.iw.app.description.ui.flow.actions.EditFlowVarsAction;
import edu.thu.keg.iw.app.description.ui.flow.actions.EditInputVarAction;
import edu.thu.keg.iw.app.description.ui.flow.editparts.PartFactory;

public class FlowEditorPage extends GraphicalEditorWithPalette {

    private Diagram diagram;

    private PaletteRoot paletteRoot;

    private List<FlowModel> flowList;

    public FlowEditorPage(List<FlowModel> flowList) {
        this.flowList = flowList;
        setEditDomain(new DefaultEditDomain(this));
    }

    public Diagram getDiagram() {
        return diagram;
    }

    protected void setInput(IEditorInput input) {
        super.setInput(input);
        diagram = new Diagram(flowList);
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        if (this.paletteRoot == null) this.paletteRoot = PaletteFactory.createPalette();
        return this.paletteRoot;
    }

    public void initializePaletteViewer() {
        super.initializePaletteViewer();
        getPaletteViewer().addDragSourceListener(new TemplateTransferDragSourceListener(getPaletteViewer()));
    }

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        getGraphicalViewer().setRootEditPart(new ScalableRootEditPart());
        getGraphicalViewer().setEditPartFactory(new PartFactory());
        MenuManager provider = new ContextMenuProvider(getGraphicalViewer()) {

            @Override
            public void buildContextMenu(IMenuManager menu) {
                GEFActionConstants.addStandardActionGroups(menu);
                IAction action = getActionRegistry().getAction(ActionFactory.DELETE.getId());
                if (action.isEnabled()) menu.appendToGroup(GEFActionConstants.GROUP_REST, action);
                action = getActionRegistry().getAction(ActionFactory.UNDO.getId());
                menu.appendToGroup(GEFActionConstants.GROUP_UNDO, action);
                action = getActionRegistry().getAction(ActionFactory.REDO.getId());
                menu.appendToGroup(GEFActionConstants.GROUP_UNDO, action);
                action = getActionRegistry().getAction(EditFlowVarsAction.EDIT_FLOW_VAR_ACTION_ID);
                if (action.isEnabled()) menu.appendToGroup(GEFActionConstants.GROUP_EDIT, action);
                action = getActionRegistry().getAction(EditInputVarAction.EDIT_INPUT_VAR_ACTION_ID);
                if (action.isEnabled()) menu.appendToGroup(GEFActionConstants.GROUP_EDIT, action);
            }
        };
        getGraphicalViewer().setContextMenu(provider);
    }

    @Override
    protected void initializeGraphicalViewer() {
        getGraphicalViewer().setContents(this.diagram);
        getGraphicalViewer().addDropTargetListener(new DiagramTemplateTransferDropTargetListener(getGraphicalViewer()));
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        getCommandStack().markSaveLocation();
    }

    /**
   * ���Ǵ˷�����֤�����ݸı�󹤾����ϵı��水ť���ò��ڱ���ǰ��*��;
   */
    public void commandStackChanged(EventObject event) {
        firePropertyChange(PROP_DIRTY);
        super.commandStackChanged(event);
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        updateActions(getSelectionActions());
    }

    @SuppressWarnings("unchecked")
    public void createActions() {
        super.createActions();
        IAction action;
        action = new EditFlowVarsAction((IWorkbenchPart) this);
        getActionRegistry().registerAction(action);
        getSelectionActions().add(action.getId());
        action = new EditInputVarAction((IWorkbenchPart) this);
        getActionRegistry().registerAction(action);
        getSelectionActions().add(action.getId());
    }
}
