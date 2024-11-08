package com.ivis.xprocess.ui.editors.hierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IEditorPart;
import com.ivis.xprocess.core.RequiredResource;
import com.ivis.xprocess.framework.Xelement;
import com.ivis.xprocess.ui.UIPlugin;
import com.ivis.xprocess.ui.UIType;
import com.ivis.xprocess.ui.datawrappers.IElementWrapper;
import com.ivis.xprocess.ui.diagram.DiagramVisualizer;
import com.ivis.xprocess.ui.diagram.DiagramVisualizerSettings;
import com.ivis.xprocess.ui.diagram.IHierarchyContainer;
import com.ivis.xprocess.ui.diagram.PointsBasedConnectionRouter;
import com.ivis.xprocess.ui.diagram.RefreshPerformer;
import com.ivis.xprocess.ui.diagram.DiagramVisualizerSettings.ToolbarBuilder;
import com.ivis.xprocess.ui.diagram.actions.FilterAction;
import com.ivis.xprocess.ui.diagram.actions.PrintAction;
import com.ivis.xprocess.ui.diagram.actions.RefreshAction;
import com.ivis.xprocess.ui.diagram.actions.ZoomFitAction;
import com.ivis.xprocess.ui.diagram.actions.ZoomInAction;
import com.ivis.xprocess.ui.diagram.actions.ZoomOriginalAction;
import com.ivis.xprocess.ui.diagram.actions.ZoomOutAction;
import com.ivis.xprocess.ui.diagram.editor.DiagramEditor;
import com.ivis.xprocess.ui.diagram.editor.DiagramEditorInput;
import com.ivis.xprocess.ui.diagram.editor.DummyDiagramEditorInput;
import com.ivis.xprocess.ui.diagram.inplace.InplaceManager;
import com.ivis.xprocess.ui.diagram.inplace.InplaceManagerImpl;
import com.ivis.xprocess.ui.diagram.model.DiagNode;
import com.ivis.xprocess.ui.diagram.model.ModelProvider;
import com.ivis.xprocess.ui.diagram.model.ModelProvider.ModelChangeListener;
import com.ivis.xprocess.ui.diagram.util.FilterItem;
import com.ivis.xprocess.ui.diagram.util.Util;
import com.ivis.xprocess.ui.menu.MenuUtil;
import com.ivis.xprocess.ui.properties.HierarchyMessages;
import com.ivis.xprocess.util.LicensingEnums.Feature;

public class HierarchyEditor extends DiagramEditor implements ModelChangeListener, IHierarchyContainer {

    public static final String ID = "com.ivis.xprocess.ui.hierarchyEditor";

    private static final String CONTEXT_ID = "com.ivis.xprocess.ui.editors.HierarchyEditorContext";

    public static final String HIDE_CONSTRAINTS_PREFERENCE = "hierarchy_editor_hide_constraints";

    public static final String HIDE_PATTERNS_PREFERENCE = "hierarchy_editor_hide_patterns";

    public static final String HIDE_TASKFOLDERS_PREFERENCE = "hierarchy_editor_hide_taskfolders";

    public static final String HIDE_CLOSED_TASKFOLDERS_PREFERENCE = "hierarchy_editor_hide_closed_taskfolders";

    public static final String HIDE_CLOSED_PREFERENCE = "hierarchy_editor_hide_closed";

    public static final String HIDE_TASKS_PREFERENCE = "hierarchy_editor_hide_tasks";

    private static final String ACTIONS_ID_PREFIX = "com.ivis.xprocess.ui.editors.hierarchy.actions";

    public static final Collection<String> FILTER_PROPERTIES = Arrays.asList(new String[] { HIDE_CONSTRAINTS_PREFERENCE, HIDE_PATTERNS_PREFERENCE, HIDE_TASKFOLDERS_PREFERENCE, HIDE_CLOSED_TASKFOLDERS_PREFERENCE, HIDE_TASKS_PREFERENCE, HIDE_CLOSED_PREFERENCE });

    private static final Collection<FilterItem> FILTER_ITEMS;

    static {
        FILTER_ITEMS = new ArrayList<FilterItem>();
        FILTER_ITEMS.add(new FilterItem(UIType.hierarchy_constraint_link.label, HierarchyMessages.constraint_filter_description, UIType.hierarchy_constraint_link.getImageDescriptor(), HIDE_CONSTRAINTS_PREFERENCE, getPreferenceStoreStatic()));
        FILTER_ITEMS.add(new FilterItem(UIType.pattern.label, HierarchyMessages.pattern_filter_description, UIType.pattern.getImageDescriptor(), HIDE_PATTERNS_PREFERENCE, getPreferenceStoreStatic()));
        FILTER_ITEMS.add(new FilterItem(UIType.taskfolder.label, HierarchyMessages.folder_filter_description, UIType.taskfolder.getImageDescriptor(), HIDE_TASKFOLDERS_PREFERENCE, getPreferenceStoreStatic()));
        FILTER_ITEMS.add(new FilterItem(UIType.closed_taskfolder.label, HierarchyMessages.closed_taskfolder_filter_description, UIType.closed_taskfolder.getImageDescriptor(), HIDE_CLOSED_TASKFOLDERS_PREFERENCE, getPreferenceStoreStatic()));
        FILTER_ITEMS.add(new FilterItem(UIType.task.label, HierarchyMessages.task_filter_description, UIType.task.getImageDescriptor(), HIDE_TASKS_PREFERENCE, getPreferenceStoreStatic()));
        FILTER_ITEMS.add(new FilterItem(UIType.closed_task.label, HierarchyMessages.closed_task_filter_description, UIType.closed_task.getImageDescriptor(), HIDE_CLOSED_PREFERENCE, getPreferenceStoreStatic()));
    }

    private DiagramVisualizer myVisualizer;

    private ModelProvider myModelProvider;

    private RefreshPerformer myRefreshPerformer;

    private InplaceManagerImpl myInplaceManager;

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
    public void setFocus() {
        if (myVisualizer != null) {
            myVisualizer.getControl().setFocus();
        }
    }

    @Override
    public void dispose() {
        if (myVisualizer != null) {
            myVisualizer.dispose();
            myVisualizer = null;
        }
        if (myRefreshPerformer != null) {
            myRefreshPerformer.dispose();
            myRefreshPerformer = null;
        }
        if (myModelProvider != null) {
            myModelProvider.removeModelChangeListener(this);
            myModelProvider.dispose();
            myModelProvider = null;
        }
        super.dispose();
    }

    public void modelChanged(Object input) {
        myVisualizer.refresh(true);
    }

    public void refresh(boolean rebuildModel, boolean rebuildVisualModel) {
        if (myVisualizer != null) {
            if (rebuildModel) {
                myModelProvider.rebuildModel();
            }
            myVisualizer.refresh(rebuildModel || rebuildVisualModel);
        }
        setPartName(getEditorInput().getName());
    }

    protected MenuManager buildContextMenu() {
        final MenuManager contextMenu = new MenuManager();
        contextMenu.setRemoveAllWhenShown(true);
        contextMenu.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                MenuUtil.createBasicMenuStructure(contextMenu, mySelectionProvider.getSelection());
            }
        });
        return contextMenu;
    }

    @Override
    protected void showDiagram() {
        if (myVisualizer == null) {
            assert getEditorInput() instanceof HierarchyEditorInput;
            myModelProvider = new HierarchyModelProvider();
            myModelProvider.addModelChangeListener(this);
            myRefreshPerformer = new RefreshPerformer(this);
            MenuManager contextMenu = buildContextMenu();
            DiagramVisualizerSettings settings = new DiagramVisualizerSettings();
            settings.setEditPartFactory(new HierarchyEditPartFactory());
            settings.setModelProvider(myModelProvider);
            settings.setLayoutPerformer(new HierarchyLayoutPerformer());
            settings.setInfiniteDiagramSize(true);
            settings.setContentMenu(contextMenu);
            ScalableFreeformRootEditPart rootEditPart = new ScalableFreeformRootEditPart();
            ConnectionLayer connectionLayer = (ConnectionLayer) rootEditPart.getLayer(LayerConstants.CONNECTION_LAYER);
            connectionLayer.setConnectionRouter(new PointsBasedConnectionRouter());
            settings.setRootEditPart(rootEditPart);
            settings.setToolbarBuilder(new ToolbarBuilder() {

                public Iterable<Iterable<IAction>> buildToolbar(GraphicalViewer graphicalViewer) {
                    ArrayList<Iterable<IAction>> toolbar = new ArrayList<Iterable<IAction>>();
                    ArrayList<IAction> navigateActions = new ArrayList<IAction>();
                    navigateActions.add(new RefreshAction(myRefreshPerformer));
                    navigateActions.add(new ExpandAction(myModelProvider));
                    navigateActions.add(new CollapseAction(myModelProvider));
                    toolbar.add(navigateActions);
                    ArrayList<IAction> zoomActions = new ArrayList<IAction>();
                    zoomActions.add(new ZoomInAction(HierarchyEditor.this, graphicalViewer));
                    zoomActions.add(new ZoomOutAction(HierarchyEditor.this, graphicalViewer));
                    zoomActions.add(new ZoomFitAction(HierarchyEditor.this, graphicalViewer));
                    zoomActions.add(new ZoomOriginalAction(HierarchyEditor.this, graphicalViewer));
                    toolbar.add(zoomActions);
                    ArrayList<IAction> filterActions = new ArrayList<IAction>();
                    filterActions.add(new FilterAction(HierarchyEditor.this, FILTER_ITEMS));
                    toolbar.add(filterActions);
                    ArrayList<IAction> printActions = new ArrayList<IAction>();
                    printActions.add(new PrintAction(HierarchyEditor.this, graphicalViewer));
                    toolbar.add(printActions);
                    return toolbar;
                }
            });
            myVisualizer = new DiagramVisualizer(settings, myDiagramPane);
            myVisualizer.getControl().setBackground(ColorConstants.white);
            getSite().registerContextMenu(contextMenu, myVisualizer.getSelectionProvider());
            mySelectionProvider.setDelegate(myVisualizer.getSelectionProvider());
            myInplaceManager = new InplaceManagerImpl();
            myInplaceManager.setVisualizer(myVisualizer);
            myModelProvider.setInput(((HierarchyEditorInput) getEditorInput()).getInputElementWrapper(), true);
            expandTasks(false);
            expandTasks(true);
        }
        myPageBook.showPage(myDiagramPane);
    }

    private void expandTasks(boolean expand) {
        List<DiagNode> children = myModelProvider.getModel().getRoot().getChildren();
        for (DiagNode node : children) {
            if (node instanceof HierarchyNode) {
                HierarchyNode hierarchyNode = (HierarchyNode) node;
                hierarchyNode.setExpanded(expand);
            }
        }
    }

    @Override
    public boolean isAffected(IElementWrapper elementWrapper) {
        if ((myModelProvider != null) && (myModelProvider.getModel() != null)) {
            HierarchyNode rootNode = (HierarchyNode) myModelProvider.getModel().getRoot();
            return isElementAffectedRecursively(rootNode, elementWrapper);
        }
        return false;
    }

    private boolean isElementAffectedRecursively(HierarchyNode node, IElementWrapper changedWrapper) {
        Xelement changed = changedWrapper.getElement();
        Xelement changedParent = (changedWrapper.getParent() != null) ? changedWrapper.getParent().getElement() : null;
        if (node.getUuid() != null) {
            if (Util.isDeleted(node.getUuid())) {
                return true;
            }
            Xelement nodeXelement = Util.getElement(node.getUuid());
            if (nodeXelement.equals(changed)) {
                return true;
            }
            if ((changedParent != null) && nodeXelement.equals(changedParent)) {
                return true;
            }
            if (nodeXelement instanceof RequiredResource) {
                if (((RequiredResource) nodeXelement).getRoleType().equals(changed)) {
                    return true;
                }
            }
        }
        for (DiagNode childNode : node.getChildren()) {
            if (isElementAffectedRecursively((HierarchyNode) childNode, changedWrapper)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IPreferenceStore getPreferenceStore() {
        return getPreferenceStoreStatic();
    }

    public static IPreferenceStore getPreferenceStoreStatic() {
        return UIPlugin.getDefault().getPreferenceStore();
    }

    @Override
    public void refreshOnPropertyChange(String propertyName) {
        if (FILTER_PROPERTIES.contains(propertyName)) {
            refresh(true, true);
        }
    }

    @Override
    public InplaceManager getInplaceManager() {
        return myInplaceManager;
    }

    @Override
    public String getActionsIdPrefix() {
        return ACTIONS_ID_PREFIX;
    }

    public IEditorPart getEditorPart() {
        return this;
    }

    public String getInputElementUuid() {
        return ((DiagramEditorInput) getEditorInput()).getInputElementUuid();
    }

    public boolean restore() {
        boolean result = true;
        if (getEditorInput() instanceof DummyDiagramEditorInput) {
            DiagramEditorInput restoredInput = ((DummyDiagramEditorInput) getEditorInput()).createRestoredInput();
            if (restoredInput != null) {
                setInput(restoredInput);
                showDiagram();
            } else {
                result = false;
            }
        }
        if (!result) {
            getSite().getPage().closeEditor(this, false);
        }
        return result;
    }
}
