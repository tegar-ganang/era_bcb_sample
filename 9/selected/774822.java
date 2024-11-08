package com.ivis.xprocess.ui.workflowdesigner;

import java.util.ArrayList;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import com.ivis.xprocess.ui.datawrappers.IElementWrapper;
import com.ivis.xprocess.ui.diagram.editor.DiagramEditor;
import com.ivis.xprocess.ui.diagram.inplace.InplaceManager;
import com.ivis.xprocess.ui.menu.MenuUtil;
import com.ivis.xprocess.ui.processdesigner.actions.OverviewAction;
import com.ivis.xprocess.ui.processdesigner.actions.ZoomFitAction;
import com.ivis.xprocess.ui.processdesigner.actions.ZoomInAction;
import com.ivis.xprocess.ui.processdesigner.actions.ZoomOneAction;
import com.ivis.xprocess.ui.processdesigner.actions.ZoomOutAction;
import com.ivis.xprocess.ui.workflowdesigner.actions.FilterAction;
import com.ivis.xprocess.ui.workflowdesigner.actions.RefreshAction;
import com.ivis.xprocess.ui.workflowdesigner.diagram.Visualizer;
import com.ivis.xprocess.ui.workflowdesigner.diagram.WorkflowDesignerInplaceManager;
import com.ivis.xprocess.ui.workflowdesigner.diagram.model.ModelProvider;
import com.ivis.xprocess.ui.workflowdesigner.diagram.palette.PaletteBuilder;
import com.ivis.xprocess.ui.workflowdesigner.print.PrintAction;
import com.ivis.xprocess.util.LicensingEnums.Feature;

public class WorkflowDesignerEditor extends DiagramEditor {

    public static final String ID = "com.ivis.xprocess.ui.workflowdesigner.WorkflowDesignerEditor";

    private static final String CONTEXT_ID = "com.ivis.xprocess.ui.workflowdesigner.workflowDesignerContext";

    private static final String ACTIONS_ID_PREFIX = "com.ivis.xprocess.ui.workflowdesigner.actions";

    private ModelProvider myModelProvider = new ModelProvider();

    private Visualizer myVisualizer;

    private WorkflowDesignerInplaceManager myInplaceManager;

    private RefreshPerformer myRefreshPerformer;

    private IElementWrapper myElementWrapperToCallInplace;

    @Override
    protected String getContextId() {
        return CONTEXT_ID;
    }

    @Override
    protected Feature getFeatureId() {
        return Feature.WORKFLOW_DESIGNER;
    }

    @Override
    protected String getFeatureName() {
        return null;
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        if ((input != null) && input instanceof WorkflowDesignerEditorInput) {
            myModelProvider.setRootElementUuid(((WorkflowDesignerEditorInput) input).getInputElementUuid());
        }
    }

    @Override
    public void setFocus() {
        if (myVisualizer != null) {
            myVisualizer.getControl().setFocus();
        }
    }

    @Override
    public void dispose() {
        if (myVisualizer != null) {
            myVisualizer.dispose();
        }
        if (myRefreshPerformer != null) {
            myRefreshPerformer.dispose();
            myRefreshPerformer = null;
        }
        super.dispose();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getAdapter(Class adapter) {
        if (GraphicalViewer.class.equals(adapter)) {
            if (myVisualizer != null) {
                return myVisualizer.getViewer();
            } else {
                return null;
            }
        }
        return super.getAdapter(adapter);
    }

    public ModelProvider getModelProvider() {
        return myModelProvider;
    }

    @Override
    public void refresh(boolean rebuildModel, boolean rebuildVisualModel) {
        setPartName(getEditorInput().getName());
        if (myVisualizer != null) {
            myVisualizer.refresh(rebuildModel || rebuildVisualModel);
        }
        if ((myElementWrapperToCallInplace != null) && myInplaceManager.canOpenInplace(myElementWrapperToCallInplace, null)) {
            myInplaceManager.openInplace(myElementWrapperToCallInplace, null);
            myElementWrapperToCallInplace = null;
        }
    }

    @Override
    public boolean isAffected(IElementWrapper elementWrapper) {
        return false;
    }

    protected PaletteRoot buildPalette() {
        return PaletteBuilder.buildPalette(this);
    }

    protected MenuManager buildContextMenu() {
        final MenuManager contextMenu = new MenuManager();
        contextMenu.setRemoveAllWhenShown(true);
        contextMenu.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                MenuUtil.createBasicMenuStructure(contextMenu, myVisualizer.getSelectionProvider().getSelection());
            }
        });
        return contextMenu;
    }

    protected Iterable<Iterable<IAction>> buildToolbar(GraphicalViewer graphicalViewer) {
        ArrayList<Iterable<IAction>> toolbar = new ArrayList<Iterable<IAction>>();
        ArrayList<IAction> navigateActions = new ArrayList<IAction>();
        navigateActions.add(new RefreshAction(myRefreshPerformer));
        toolbar.add(navigateActions);
        ArrayList<IAction> zoomActions = new ArrayList<IAction>();
        zoomActions.add(new ZoomInAction(this));
        zoomActions.add(new ZoomOutAction(this));
        zoomActions.add(new ZoomFitAction(this));
        zoomActions.add(new ZoomOneAction(this));
        toolbar.add(zoomActions);
        ArrayList<IAction> viewActions = new ArrayList<IAction>();
        viewActions.add(new OverviewAction(this, graphicalViewer));
        viewActions.add(new FilterAction(this));
        toolbar.add(viewActions);
        ArrayList<IAction> printActions = new ArrayList<IAction>();
        printActions.add(new PrintAction(this));
        toolbar.add(printActions);
        return toolbar;
    }

    @Override
    protected void showDiagram() {
        if (myVisualizer == null) {
            assert getEditorInput() instanceof WorkflowDesignerEditorInput;
            myRefreshPerformer = new RefreshPerformer(this);
            MenuManager contextMenu = buildContextMenu();
            PaletteRoot palette = buildPalette();
            myVisualizer = new Visualizer(myDiagramPane, myModelProvider, contextMenu, getSite().getPage(), palette);
            getSite().registerContextMenu(contextMenu, myVisualizer.getSelectionProvider());
            mySelectionProvider.setDelegate(myVisualizer.getSelectionProvider());
            myInplaceManager = new WorkflowDesignerInplaceManager(myVisualizer);
            Iterable<Iterable<IAction>> toolbar = buildToolbar((GraphicalViewer) myVisualizer.getViewer());
            myVisualizer.setupActions(toolbar);
        }
        myPageBook.showPage(myDiagramPane);
    }

    public InplaceManager getInplaceManager() {
        return myInplaceManager;
    }

    public void setElementWrapperToCallInplace(IElementWrapper elementWrapper) {
        myElementWrapperToCallInplace = elementWrapper;
    }

    @Override
    public String getActionsIdPrefix() {
        return ACTIONS_ID_PREFIX;
    }

    public IEditorPart getEditorPart() {
        return this;
    }
}
