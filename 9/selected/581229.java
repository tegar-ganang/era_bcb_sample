package com.prolix.editor.main.workspace.prolix.mainEditor;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import uk.ac.reload.straker.StrakerPlugin;
import com.prolix.editor.EditorContextMenuProvider;
import com.prolix.editor.LDT_Constrains;
import com.prolix.editor.PaletteProvider;
import com.prolix.editor.DND.EnvironmentItemTransfer;
import com.prolix.editor.DND.InteractionOperationItemTransfer;
import com.prolix.editor.DND.RoleItemTransfer;
import com.prolix.editor.graph.editparts.ModelEditPartFactory;
import com.prolix.editor.graph.editparts.activities.ActivityEditPart;
import com.prolix.editor.graph.editparts.points.TextNodeEditPart;
import com.prolix.editor.listener.EnvironmentTransferDropTargetListener;
import com.prolix.editor.listener.InteractionOperationTransferDropTargetListener;
import com.prolix.editor.listener.RoleTransferDropTargetListener;
import com.prolix.editor.main.workspace.prolix.ProlixWorkspace;

public class GEFEditor extends Composite {

    private ProlixWorkspace parent;

    private GraphicalViewer graphicalViewer;

    private FlyoutPaletteComposite paletteContainer;

    private Point scrollbarOffset;

    public GEFEditor(ProlixWorkspace parent) {
        super(parent, SWT.NONE);
        this.parent = parent;
        setupView();
        graphicalViewer.setContents(parent.getModel());
    }

    private void setupView() {
        setLayout(new FillLayout());
        createPalette();
    }

    public void createPalette() {
        parent.getEditDomain().setPaletteRoot(PaletteProvider.createPalette());
        paletteContainer = new FlyoutPaletteComposite(this, SWT.NONE, parent.getSite().getPage(), createPaletteViewProvieder(), getPalettePreferences());
        createGraphicalViewer();
        paletteContainer.setGraphicalControl(graphicalViewer.getControl());
    }

    private PaletteViewerProvider createPaletteViewProvieder() {
        return new PaletteViewerProvider(parent.getEditDomain()) {

            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
                viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
            }
        };
    }

    protected FlyoutPreferences getPalettePreferences() {
        FlyoutPreferences prefs = FlyoutPaletteComposite.createFlyoutPreferences(StrakerPlugin.getDefault().getPluginPreferences());
        prefs.setPaletteState(FlyoutPaletteComposite.STATE_PINNED_OPEN);
        return prefs;
    }

    private void createGraphicalViewer() {
        graphicalViewer = new ProlixScrollingGraphicalViewer(this);
        graphicalViewer.createControl(paletteContainer);
        parent.getEditDomain().addViewer(graphicalViewer);
        graphicalViewer.getControl().setBackground(LDT_Constrains.ColorDiagramBackground);
        graphicalViewer.setRootEditPart(new ScalableFreeformRootEditPart());
        graphicalViewer.setKeyHandler(new GraphicalViewerKeyHandler(graphicalViewer));
        parent.getSite().setSelectionProvider(graphicalViewer);
        graphicalViewer.setEditPartFactory(new ModelEditPartFactory());
        ContextMenuProvider cmProvider = new EditorContextMenuProvider(graphicalViewer, parent.getActionRegistry());
        graphicalViewer.setContextMenu(cmProvider);
        parent.getSite().registerContextMenu(cmProvider, graphicalViewer);
        initGraphicalViewer();
    }

    private void initGraphicalViewer() {
        graphicalViewer.addDropTargetListener(new EnvironmentTransferDropTargetListener(graphicalViewer, EnvironmentItemTransfer.getInstance()));
        graphicalViewer.addDropTargetListener(new RoleTransferDropTargetListener(graphicalViewer, RoleItemTransfer.getInstance()));
        graphicalViewer.addDropTargetListener(new InteractionOperationTransferDropTargetListener(graphicalViewer, InteractionOperationItemTransfer.getInstance()));
        graphicalViewer.addDropTargetListener((TransferDropTargetListener) new TemplateTransferDropTargetListener(graphicalViewer));
        graphicalViewer.getControl().addMouseListener(new MouseListener() {

            public void mouseDoubleClick(MouseEvent e) {
                EditPart part = graphicalViewer.findObjectAt(new Point(e.x, e.y));
                if (part instanceof ActivityEditPart) ((ActivityEditPart) part).openEditDialog();
                if (part instanceof TextNodeEditPart) {
                    ((TextNodeEditPart) part).startDoubleClickEding();
                }
            }

            public void mouseDown(MouseEvent e) {
            }

            public void mouseUp(MouseEvent e) {
            }
        });
    }

    public GraphicalViewer getGraphicalViewer() {
        return graphicalViewer;
    }

    public org.eclipse.swt.graphics.Point getAbsolutPosition() {
        int x = 0;
        int y = 0;
        Composite composite = this;
        while (composite != null) {
            x += composite.getLocation().x;
            y += composite.getLocation().y;
            composite = composite.getParent();
        }
        return new org.eclipse.swt.graphics.Point(x, y);
    }

    public synchronized Point getScrollbarOffset() {
        if (scrollbarOffset == null) {
            FigureCanvas control = (FigureCanvas) graphicalViewer.getControl();
            scrollbarOffset = new Point(control.getHorizontalBar().getSelection(), control.getVerticalBar().getSelection());
            if (scrollbarOffset.x == 0 && scrollbarOffset.y == 0) {
                scrollbarOffset.x = 300;
                scrollbarOffset.y = 300;
            }
        }
        return scrollbarOffset;
    }

    public Point translateLocationScrollbar(Point location) {
        location = location.getCopy();
        FigureCanvas control = (FigureCanvas) graphicalViewer.getControl();
        location.translate(control.getHorizontalBar().getSelection() - getScrollbarOffset().x, control.getVerticalBar().getSelection() - getScrollbarOffset().y);
        return location;
    }

    public boolean setFocus() {
        return graphicalViewer.getControl().setFocus();
    }
}
