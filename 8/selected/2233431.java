package org.eclipse.swt.e4.examples.gef;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.draw2d.parts.Thumbnail;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.e4.GraphicalWidgetWithFlyoutPalette;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.AlignmentAction;
import org.eclipse.gef.ui.actions.CopyTemplateAction;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.MatchHeightAction;
import org.eclipse.gef.ui.actions.MatchWidthAction;
import org.eclipse.gef.ui.actions.ToggleGridAction;
import org.eclipse.gef.ui.actions.ToggleRulerVisibilityAction;
import org.eclipse.gef.ui.actions.ToggleSnapToGeometryAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.gef.ui.rulers.RulerComposite;
import org.eclipse.gef.examples.logicdesigner.LogicContextMenuProvider;
import org.eclipse.gef.examples.logicdesigner.LogicPlugin;
import org.eclipse.gef.examples.logicdesigner.actions.IncrementDecrementAction;
import org.eclipse.gef.examples.logicdesigner.actions.LogicPasteTemplateAction;
import org.eclipse.gef.examples.logicdesigner.dnd.TextTransferDropTargetListener;
import org.eclipse.gef.examples.logicdesigner.edit.GraphicalPartFactory;
import org.eclipse.gef.examples.logicdesigner.edit.TreePartFactory;
import org.eclipse.gef.examples.logicdesigner.model.LogicDiagram;
import org.eclipse.gef.examples.logicdesigner.model.LogicDiagramFactory;
import org.eclipse.gef.examples.logicdesigner.model.LogicElement;
import org.eclipse.gef.examples.logicdesigner.model.LogicRuler;
import org.eclipse.gef.examples.logicdesigner.palette.LogicPaletteCustomizer;
import org.eclipse.gef.examples.logicdesigner.rulers.LogicRulerProvider;
import org.eclipse.gef.examples.shapes.model.EllipticalShape;
import org.eclipse.gef.examples.shapes.model.RectangularShape;
import org.eclipse.gef.examples.shapes.model.Shape;
import org.eclipse.gef.examples.shapes.model.ShapesDiagram;

public class LogicWidget extends GraphicalWidgetWithFlyoutPalette {

    public LogicWidget() {
        logicDiagram.addChild((LogicElement) LogicDiagramFactory.createLargeModel());
        setEditDomain(new DefaultEditDomain(null));
    }

    private PaletteRoot root;

    private LogicDiagram logicDiagram = new LogicDiagram();

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        ScrollingGraphicalViewer viewer = (ScrollingGraphicalViewer) getGraphicalViewer();
        ScalableFreeformRootEditPart root = new ScalableFreeformRootEditPart();
        List zoomLevels = new ArrayList(3);
        zoomLevels.add(ZoomManager.FIT_ALL);
        zoomLevels.add(ZoomManager.FIT_WIDTH);
        zoomLevels.add(ZoomManager.FIT_HEIGHT);
        root.getZoomManager().setZoomLevelContributions(zoomLevels);
        IAction zoomIn = new ZoomInAction(root.getZoomManager());
        IAction zoomOut = new ZoomOutAction(root.getZoomManager());
        getActionRegistry().registerAction(zoomIn);
        getActionRegistry().registerAction(zoomOut);
        viewer.setRootEditPart(root);
        viewer.setEditPartFactory(new GraphicalPartFactory());
        ContextMenuProvider provider = new LogicContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(provider);
        loadProperties();
        IAction showRulers = new ToggleRulerVisibilityAction(getGraphicalViewer());
        getActionRegistry().registerAction(showRulers);
        IAction snapAction = new ToggleSnapToGeometryAction(getGraphicalViewer());
        getActionRegistry().registerAction(snapAction);
        IAction showGrid = new ToggleGridAction(getGraphicalViewer());
        getActionRegistry().registerAction(showGrid);
        Listener listener = new Listener() {

            public void handleEvent(Event event) {
            }
        };
        getGraphicalControl().addListener(SWT.Activate, listener);
        getGraphicalControl().addListener(SWT.Deactivate, listener);
    }

    protected PaletteViewerProvider createPaletteViewerProvider() {
        return new PaletteViewerProvider(getEditDomain()) {

            private IMenuListener menuListener;

            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
                viewer.setCustomizer(new LogicPaletteCustomizer());
                viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
            }

            protected void hookPaletteViewer(PaletteViewer viewer) {
                super.hookPaletteViewer(viewer);
                final CopyTemplateAction copy = (CopyTemplateAction) getActionRegistry_lcl().getAction(ActionFactory.COPY.getId());
                viewer.addSelectionChangedListener(copy);
                if (menuListener == null) menuListener = new IMenuListener() {

                    public void menuAboutToShow(IMenuManager manager) {
                        manager.appendToGroup(GEFActionConstants.GROUP_COPY, copy);
                    }
                };
                viewer.getContextMenu().addMenuListener(menuListener);
            }
        };
    }

    protected ActionRegistry getActionRegistry_lcl() {
        return getActionRegistry();
    }

    protected LogicDiagram getLogicDiagram() {
        return logicDiagram;
    }

    protected PaletteRoot getPaletteRoot() {
        if (root == null) {
            root = LogicPlugin.createPalette();
        }
        return root;
    }

    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        getGraphicalViewer().setContents(getLogicDiagram());
        getGraphicalViewer().addDropTargetListener((TransferDropTargetListener) new TemplateTransferDropTargetListener(getGraphicalViewer()));
        getGraphicalViewer().addDropTargetListener((TransferDropTargetListener) new TextTransferDropTargetListener(getGraphicalViewer(), TextTransfer.getInstance()));
    }

    protected FigureCanvas getEditor() {
        return (FigureCanvas) getGraphicalViewer().getControl();
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    protected void loadProperties() {
        LogicRuler ruler = getLogicDiagram().getRuler(PositionConstants.WEST);
        RulerProvider provider = null;
        if (ruler != null) {
            provider = new LogicRulerProvider(ruler);
        }
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_VERTICAL_RULER, provider);
        ruler = getLogicDiagram().getRuler(PositionConstants.NORTH);
        provider = null;
        if (ruler != null) {
            provider = new LogicRulerProvider(ruler);
        }
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_HORIZONTAL_RULER, provider);
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_RULER_VISIBILITY, new Boolean(getLogicDiagram().getRulerVisibility()));
        getGraphicalViewer().setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, new Boolean(getLogicDiagram().isSnapToGeometryEnabled()));
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, new Boolean(getLogicDiagram().isGridEnabled()));
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, new Boolean(getLogicDiagram().isGridEnabled()));
        ZoomManager manager = (ZoomManager) getGraphicalViewer().getProperty(ZoomManager.class.toString());
        if (manager != null) manager.setZoom(getLogicDiagram().getZoom());
        getGraphicalViewer().setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), MouseWheelZoomHandler.SINGLETON);
    }

    public void setLogicDiagram(LogicDiagram diagram) {
        logicDiagram = diagram;
    }
}
