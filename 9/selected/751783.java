package com.ivis.xprocess.ui.viewpoints.editor;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.IHandlerService;
import com.ivis.xprocess.core.Viewpoint;
import com.ivis.xprocess.framework.XchangeElement;
import com.ivis.xprocess.ui.UIPlugin;
import com.ivis.xprocess.ui.datawrappers.DataCacheManager;
import com.ivis.xprocess.ui.datawrappers.IElementWrapper;
import com.ivis.xprocess.ui.datawrappers.project.ViewpointWrapper;
import com.ivis.xprocess.ui.diagram.actions.ZoomFitAction;
import com.ivis.xprocess.ui.diagram.actions.ZoomOriginalAction;
import com.ivis.xprocess.ui.editors.editorparts.ElementEditorInput;
import com.ivis.xprocess.ui.properties.EditorMessages;
import com.ivis.xprocess.ui.refresh.ChangeEventFactory;
import com.ivis.xprocess.ui.refresh.ChangeRecord;
import com.ivis.xprocess.ui.refresh.IRefreshListener;
import com.ivis.xprocess.ui.refresh.ChangeEventFactory.ChangeEvent;
import com.ivis.xprocess.ui.util.IRestorable;
import com.ivis.xprocess.ui.util.RestoreEditorManager;
import com.ivis.xprocess.ui.util.ViewUtil;
import com.ivis.xprocess.ui.viewpoint.parts.DiagramEditPart;
import com.ivis.xprocess.ui.viewpoint.parts.ViewpointEditPartFactory;
import com.ivis.xprocess.ui.viewpoints.types.ViewpointDiagram;
import com.ivis.xprocess.ui.viewpoints.util.PrintAction;
import com.ivis.xprocess.ui.viewpoints.util.ViewpointTypeFactory;

public class ViewpointEditor extends GraphicalMultiPartEditor implements IRefreshListener, IRestorable {

    private ElementEditorInput elementEditorInput;

    private IViewPointType diagram;

    private ViewpointPage editorPage;

    private SwimlaneEditorPage swimlaneEditorPage;

    private IElementWrapper subjectElementWrapper;

    protected boolean needsRefreshAfterEditing = false;

    private boolean dirty;

    private XchangeElement baseTransientElement;

    private XchangeElement localTransientElement;

    private Composite pageComposite;

    private PrintAction pa;

    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        elementEditorInput = (ElementEditorInput) input;
        ChangeEventFactory.getInstance().addRefreshListener(this);
        if (diagram == null) {
            if (elementEditorInput.getElementWrapper() instanceof ViewpointWrapper) {
                diagram = ViewpointTypeFactory.getViewPointType((ViewpointWrapper) elementEditorInput.getElementWrapper());
                updatePartName();
            } else {
                if (elementEditorInput.getElementWrapper() == null) {
                    RestoreEditorManager.registerPartToRestore(this);
                    diagram = ViewpointTypeFactory.getViewPointType(null);
                } else {
                    System.err.println("Editor base is not a ViewpointWrapper - " + elementEditorInput.getElementWrapper());
                }
            }
        }
        super.init(site, input);
    }

    @Override
    public void dispose() {
        ChangeEventFactory.getInstance().removeRefreshListener(this);
        super.dispose();
    }

    @Override
    protected void createEditorPages() {
        pageComposite = createDiagramPage();
        addPage(0, pageComposite);
        setPageText(0, EditorMessages.viewpoint_editor_view_pagename);
        editorPage = new ViewpointPage();
        Composite editorComposite = editorPage.createPage(this);
        addPage(1, editorComposite);
        setPageText(1, EditorMessages.viewpoint_editor_configure_pagename);
        swimlaneEditorPage = new SwimlaneEditorPage();
        Composite swimlaneComposite = swimlaneEditorPage.createPage(this);
        addPage(2, swimlaneComposite);
        setPageText(2, EditorMessages.viewpoint_editor_swimlane_pagename);
        refreshTransientElements();
        displayData();
    }

    @Override
    protected void initializeGraphicalViewer() {
        if (diagram.isType("GRID")) {
            GraphicalViewer viewer = getGraphicalViewer();
            if (viewer != null) {
                viewer.setContents(diagram);
            }
        }
    }

    private Composite createDiagramPage() {
        Composite pageComposite = diagram.create(getContainer());
        if (diagram.isType("GRID")) {
            ViewpointDiagram viewpointDiagram = (ViewpointDiagram) diagram;
            createGraphicalViewer(viewpointDiagram.getDiagramComposite());
            createToolBarActions(viewpointDiagram.getToolbar());
            createAdditionalActions();
        } else {
        }
        return pageComposite;
    }

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new ViewpointEditPartFactory());
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
    }

    protected void createToolBarActions(CoolBar toolBar) {
        if (toolBar != null) {
            CoolBarManager coolBarManager = new CoolBarManager(toolBar);
            ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT | SWT.WRAP | SWT.HORIZONTAL);
            ZoomManager zoomManager = (ZoomManager) getAdapter(ZoomManager.class);
            if (zoomManager != null) {
                IAction zoomIn = new ZoomInAction(zoomManager);
                toolBarManager.add(zoomIn);
                IAction zoomOut = new ZoomOutAction(zoomManager);
                toolBarManager.add(zoomOut);
                IAction zoomOriginal = new ZoomOriginalAction(this, getGraphicalViewer());
                toolBarManager.add(zoomOriginal);
                IAction zoomFit = new ZoomFitAction(this, getGraphicalViewer());
                toolBarManager.add(zoomFit);
            }
            coolBarManager.add(toolBarManager);
            toolBarManager = new ToolBarManager(SWT.FLAT | SWT.WRAP | SWT.HORIZONTAL);
            pa = new PrintAction(ViewpointEditor.this, getGraphicalViewer());
            pa.setToolTipText(EditorMessages.viewpoint_editor_print_tooltip);
            toolBarManager.add(pa);
            coolBarManager.add(toolBarManager);
            coolBarManager.update(true);
        }
    }

    protected void createAdditionalActions() {
        ZoomManager zoomManager = (ZoomManager) getAdapter(ZoomManager.class);
        if (zoomManager != null) {
            List<String> zoomLevels = new ArrayList<String>(3);
            zoomLevels.add(ZoomManager.FIT_ALL);
            zoomLevels.add(ZoomManager.FIT_WIDTH);
            zoomLevels.add(ZoomManager.FIT_HEIGHT);
            zoomManager.setZoomLevelContributions(zoomLevels);
            IAction zoomIn = new ZoomInAction(zoomManager);
            IAction zoomOut = new ZoomOutAction(zoomManager);
            getActionRegistry().registerAction(zoomIn);
            getActionRegistry().registerAction(zoomOut);
            IHandlerService service = (IHandlerService) getEditorSite().getService(IHandlerService.class);
            service.activateHandler(zoomIn.getActionDefinitionId(), new ActionHandler(zoomIn));
            service.activateHandler(zoomOut.getActionDefinitionId(), new ActionHandler(zoomOut));
        } else {
            System.err.println("No zoom manager....");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getAdapter(Class type) {
        if (type == ZoomManager.class) return ((ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();
        return super.getAdapter(type);
    }

    public IEditorPart getEditorPart() {
        return null;
    }

    public String getUuid() {
        return null;
    }

    public boolean restore() {
        if (UIPlugin.getPersistenceHelper() == null) {
            return false;
        }
        IElementWrapper elementWrapper = DataCacheManager.getWrapperByUuid(elementEditorInput.getElementUuid());
        elementEditorInput.setElementWrapper(elementWrapper);
        diagram = ViewpointTypeFactory.getViewPointType((ViewpointWrapper) elementWrapper);
        restoreAsync();
        return true;
    }

    private void restoreAsync() {
        Display display = ViewUtil.getDisplay();
        display.asyncExec(new Runnable() {

            public void run() {
                int currentPage = ViewpointEditor.this.getActivePage();
                initializeGraphicalViewer();
                ViewpointEditor.this.removePage(0);
                ViewpointEditor.this.removePage(0);
                ViewpointEditor.this.removePage(0);
                createEditorPages();
                diagram.refreshChildren();
                updatePartName();
                ViewpointEditor.this.setActivePage(currentPage);
            }
        });
    }

    private void updatePartName() {
        Display display = ViewUtil.getDisplay();
        display.asyncExec(new Runnable() {

            public void run() {
                if (elementEditorInput.getElementWrapper() != null) {
                    IElementWrapper elementWrapper = elementEditorInput.getElementWrapper();
                    if ((elementWrapper.getElement() != null) && !elementWrapper.isGhost()) {
                        setPartName(EditorMessages.viewpoint_editor_partname + " " + elementWrapper.getLabel());
                    }
                }
            }
        });
    }

    public void refreshEvent(ChangeRecord changeRecord) {
        if (changeRecord.hasChange(ChangeEvent.VCS_UPDATE) || changeRecord.hasChange(ChangeEvent.RESCHEDULED)) {
            if (!dirty) {
                diagram.refreshChildren();
                refreshAsync();
                return;
            } else {
                needsRefreshAfterEditing = true;
            }
        }
        if (elementEditorInput.getElementWrapper() != null) {
            IElementWrapper elementWrapper = elementEditorInput.getElementWrapper();
            if (changeRecord.getElementUuid().equals(elementWrapper.getUuid())) {
                boolean refreshChildren = false;
                boolean type_changed = false;
                if (changeRecord.hasChange(ChangeEvent.CHILD_REMOVED) || changeRecord.hasChange(ChangeEvent.NEW_CHILD)) {
                    refreshChildren = true;
                }
                if (changeRecord.hasProperty("ICON_WIDTH") || changeRecord.hasProperty("ICON_WIDTH")) {
                    refreshChildren = true;
                }
                if (changeRecord.hasProperty("ICON_COLOUR") || changeRecord.hasProperty("ICON_TEXT")) {
                    refreshChildren = true;
                }
                if (changeRecord.hasProperty("SUBJECT")) {
                    refreshChildren = true;
                }
                if (changeRecord.hasProperty("SWIMLANE_CHANGE")) {
                    refreshChildren = true;
                }
                if (changeRecord.hasProperty("VIEWPOINT_TYPE")) {
                    type_changed = true;
                }
                if (!dirty) {
                    if (type_changed) {
                        diagram = ViewpointTypeFactory.getViewPointType((ViewpointWrapper) elementEditorInput.getElementWrapper());
                        restoreAsync();
                        return;
                    }
                    if (refreshChildren) {
                        diagram.refreshChildren();
                    }
                    refreshAsync();
                } else {
                    needsRefreshAfterEditing = true;
                }
                return;
            }
        }
        IElementWrapper elementWrapper = elementEditorInput.getElementWrapper();
        if (elementWrapper != null) {
            Viewpoint viewpoint = ((ViewpointWrapper) elementWrapper).getViewpoint();
            if (viewpoint.getSubject() != null) {
                if (viewpoint.getSubject().getUuid().equals(changeRecord.getElementUuid())) {
                    if (!dirty) {
                        diagram.refreshChildren();
                        refreshAsync();
                    } else {
                        needsRefreshAfterEditing = true;
                    }
                    return;
                }
                if (changeRecord.getParentWrapper() != null) {
                    if (viewpoint.getSubject().getUuid().equals(changeRecord.getParentWrapper().getUuid())) {
                        if (!dirty) {
                            diagram.refreshChildren();
                            refreshAsync();
                        } else {
                            needsRefreshAfterEditing = true;
                        }
                        return;
                    }
                }
            }
        }
    }

    public void refreshAsync() {
        if (dirty) {
            return;
        }
        Display display = ViewUtil.getDisplay();
        display.asyncExec(new Runnable() {

            public void run() {
                if (elementEditorInput.getElementWrapper() != null) {
                    refreshTransientElements();
                    DiagramEditPart diagramEditPart = getDiagramEditPart();
                    if (diagramEditPart != null) {
                        diagramEditPart.refresh();
                        updatePartName();
                    }
                    IElementWrapper elementWrapper = elementEditorInput.getElementWrapper();
                    if (!elementWrapper.isGhost()) {
                        setPartName(EditorMessages.viewpoint_editor_partname + " " + elementWrapper.getLabel());
                    }
                    displayData();
                }
            }
        });
    }

    protected void internalRefreshAsync() {
        if (dirty) {
            return;
        }
        Display display = ViewUtil.getDisplay();
        display.asyncExec(new Runnable() {

            public void run() {
                if (elementEditorInput.getElementWrapper() != null) {
                    DiagramEditPart diagramEditPart = getDiagramEditPart();
                    if (diagramEditPart != null) {
                        diagramEditPart.refresh();
                        updatePartName();
                    }
                    IElementWrapper elementWrapper = elementEditorInput.getElementWrapper();
                    if (!elementWrapper.isGhost()) {
                        setPartName(EditorMessages.viewpoint_editor_partname + " " + elementWrapper.getLabel());
                    }
                    displayData();
                }
            }
        });
    }

    private DiagramEditPart getDiagramEditPart() {
        if (getGraphicalViewer() != null) {
            for (Object obj : getGraphicalViewer().getRootEditPart().getChildren()) {
                if (obj instanceof DiagramEditPart) {
                    return (DiagramEditPart) obj;
                }
            }
        }
        return null;
    }

    public IElementWrapper getElementWrapper() {
        return elementEditorInput.getElementWrapper();
    }

    @Override
    public void doSave(IProgressMonitor arg0) {
        ViewpointWrapper viewpointWrapper = getViewpointWrapper();
        ChangeEventFactory.startChangeRecording(viewpointWrapper);
        ChangeEventFactory.addChange(viewpointWrapper, ChangeEvent.FIELDS_CHANGED);
        editorPage.save();
        swimlaneEditorPage.save();
        localTransientElement.mergeAndSave(baseTransientElement);
        ChangeEventFactory.saveChanges();
        setDirty(false);
        ChangeEventFactory.stopChangeRecording();
        if (needsRefreshAfterEditing) {
            diagram.refreshChildren();
        }
        refreshAsync();
    }

    private void displayData() {
        if (elementEditorInput.getElementWrapper() != null) {
            editorPage.displayData();
            swimlaneEditorPage.displayData();
            diagram.refreshChildren();
        }
    }

    public void setDirty(boolean value) {
        dirty = value;
        Display display = ViewUtil.getDisplay();
        display.asyncExec(new Runnable() {

            public void run() {
                firePropertyChange(PROP_DIRTY);
            }
        });
    }

    public boolean isDirty() {
        return dirty;
    }

    public Viewpoint getViewpoint() {
        if (localTransientElement == null) {
            return null;
        }
        return (Viewpoint) localTransientElement;
    }

    public ViewpointWrapper getViewpointWrapper() {
        if (elementEditorInput.getElementWrapper() != null) {
            return (ViewpointWrapper) elementEditorInput.getElementWrapper();
        }
        return null;
    }

    public IElementWrapper getSubjectWrapper() {
        return subjectElementWrapper;
    }

    public Composite getContainer() {
        return super.getContainer();
    }

    public void refreshTransientElements() {
        IElementWrapper elementWrapper = elementEditorInput.getElementWrapper();
        if (!dirty && elementWrapper != null && !elementWrapper.isGhost()) {
            if (elementWrapper.getElement() instanceof XchangeElement) {
                baseTransientElement = UIPlugin.getPersistenceHelper().createTransientExchangeElement((XchangeElement) elementWrapper.getElement());
                localTransientElement = UIPlugin.getPersistenceHelper().createTransientExchangeElement(baseTransientElement);
            }
        }
    }
}
