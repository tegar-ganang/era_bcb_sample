package com.ivis.xprocess.ui.processdesigner;

import java.util.ArrayList;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import com.ivis.xprocess.core.Pattern;
import com.ivis.xprocess.ui.datawrappers.IElementWrapper;
import com.ivis.xprocess.ui.diagram.editor.DiagramEditor;
import com.ivis.xprocess.ui.menu.MenuUtil;
import com.ivis.xprocess.ui.processdesigner.actions.FilterAction;
import com.ivis.xprocess.ui.processdesigner.actions.GoBackAction;
import com.ivis.xprocess.ui.processdesigner.actions.GoForwardAction;
import com.ivis.xprocess.ui.processdesigner.actions.GoHomeAction;
import com.ivis.xprocess.ui.processdesigner.actions.LayoutHorizontalAction;
import com.ivis.xprocess.ui.processdesigner.actions.OverviewAction;
import com.ivis.xprocess.ui.processdesigner.actions.RefreshAction;
import com.ivis.xprocess.ui.processdesigner.actions.ShowChildrenAsListAction;
import com.ivis.xprocess.ui.processdesigner.actions.ZoomFitAction;
import com.ivis.xprocess.ui.processdesigner.actions.ZoomInAction;
import com.ivis.xprocess.ui.processdesigner.actions.ZoomOneAction;
import com.ivis.xprocess.ui.processdesigner.actions.ZoomOutAction;
import com.ivis.xprocess.ui.processdesigner.diagram.ModelProvider;
import com.ivis.xprocess.ui.processdesigner.diagram.ProcessDesignerInplaceManager;
import com.ivis.xprocess.ui.processdesigner.diagram.Visualizer;
import com.ivis.xprocess.ui.processdesigner.diagram.palette.PaletteBuilder;
import com.ivis.xprocess.ui.processdesigner.history.ActualityChecker;
import com.ivis.xprocess.ui.processdesigner.history.HistoryTracker;
import com.ivis.xprocess.ui.processdesigner.print.PrintAction;
import com.ivis.xprocess.ui.processdesigner.util.Util;
import com.ivis.xprocess.util.LicensingEnums.Feature;

public class ProcessDesignerEditor extends DiagramEditor {

    public static final String ID = "com.ivis.xprocess.ui.processdesigner.ProcessDesignerEditor";

    private static final String CONTEXT_ID = "com.ivis.xprocess.ui.processdesigner.processDesignerContext";

    private static final String ACTIONS_ID_PREFIX = "com.ivis.xprocess.ui.processdesigner.actions";

    private HistoryTracker.Listener<String> myHistoryListener = new HistoryTracker.Listener<String>() {

        public void currentChanged(String newCurrentUuid) {
            myModelProvider.setRootElementUuid(newCurrentUuid);
            ProcessDesignerEditorInput editorInput = (ProcessDesignerEditorInput) getEditorInput();
            editorInput.setInputElementUuid(myModelProvider.getRootElementUuid());
            setPartName(getEditorInput().getName());
        }
    };

    private HistoryTracker<String> myHistoryTracker = new HistoryTracker<String>();

    private ModelProvider myModelProvider = new ModelProvider();

    private Visualizer myVisualizer;

    private RefreshPerformer myRefreshPerformer;

    private ProcessDesignerInplaceManager myInplaceManager;

    public ProcessDesignerEditor() {
        myHistoryTracker.addListener(myHistoryListener);
        myHistoryTracker.setActualityChecker(new ActualityChecker<String>() {

            public boolean isItemActual(String uuid) {
                return !Util.isDeleted(uuid);
            }
        });
    }

    @Override
    protected Feature getFeatureId() {
        return null;
    }

    @Override
    protected String getFeatureName() {
        return null;
    }

    @Override
    protected String getContextId() {
        return CONTEXT_ID;
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        if ((input != null) && input instanceof ProcessDesignerEditorInput) {
            getHistoryTracker().setCurrentItem(((ProcessDesignerEditorInput) input).getInputElementUuid());
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
        myHistoryTracker.removeListener(myHistoryListener);
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

    @Override
    public void refresh(boolean rebuildModel, boolean rebuildVisualModel) {
        if (myVisualizer != null) {
            myVisualizer.refresh(rebuildModel || rebuildVisualModel);
        }
        setPartName(getEditorInput().getName());
    }

    public HistoryTracker<String> getHistoryTracker() {
        return myHistoryTracker;
    }

    public ProcessDesignerInplaceManager getInplaceManager() {
        return myInplaceManager;
    }

    public ModelProvider getModelProvider() {
        return myModelProvider;
    }

    @Override
    public String getActionsIdPrefix() {
        return ACTIONS_ID_PREFIX;
    }

    @Override
    public boolean isAffected(IElementWrapper elementWrapper) {
        return false;
    }

    protected PaletteRoot buildPalette() {
        boolean isPatternNotProject = Util.getElement(((ProcessDesignerEditorInput) getEditorInput()).getPatternUuid()) instanceof Pattern;
        return PaletteBuilder.buildPalette(isPatternNotProject);
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
        navigateActions.add(new GoHomeAction(myHistoryTracker));
        navigateActions.add(new GoBackAction(myHistoryTracker));
        navigateActions.add(new GoForwardAction(myHistoryTracker));
        navigateActions.add(new RefreshAction(myRefreshPerformer));
        toolbar.add(navigateActions);
        ArrayList<IAction> zoomActions = new ArrayList<IAction>();
        zoomActions.add(new ZoomInAction(this));
        zoomActions.add(new ZoomOutAction(this));
        zoomActions.add(new ZoomFitAction(this));
        zoomActions.add(new ZoomOneAction(this));
        toolbar.add(zoomActions);
        ArrayList<IAction> viewActions = new ArrayList<IAction>();
        viewActions.add(new ShowChildrenAsListAction(this));
        viewActions.add(new LayoutHorizontalAction(this));
        viewActions.add(new FilterAction(this));
        viewActions.add(new OverviewAction(this, graphicalViewer));
        toolbar.add(viewActions);
        ArrayList<IAction> printActions = new ArrayList<IAction>();
        printActions.add(new PrintAction(this));
        toolbar.add(printActions);
        return toolbar;
    }

    @Override
    protected void showDiagram() {
        if (myVisualizer == null) {
            assert getEditorInput() instanceof ProcessDesignerEditorInput;
            myRefreshPerformer = new RefreshPerformer(this);
            MenuManager contextMenu = buildContextMenu();
            PaletteRoot palette = buildPalette();
            myInplaceManager = new ProcessDesignerInplaceManager();
            myVisualizer = new Visualizer(myDiagramPane, myModelProvider, contextMenu, getSite().getPage(), palette, myInplaceManager);
            myInplaceManager.setVisualizer(myVisualizer);
            getSite().registerContextMenu(contextMenu, myVisualizer.getSelectionProvider());
            mySelectionProvider.setDelegate(myVisualizer.getSelectionProvider());
            Iterable<Iterable<IAction>> toolbar = buildToolbar((GraphicalViewer) myVisualizer.getViewer());
            myVisualizer.setupActions(toolbar);
        }
        myPageBook.showPage(myDiagramPane);
    }

    public IEditorPart getEditorPart() {
        return this;
    }
}
