package org.wsmostudio.bpmo.ui.editor;

import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.*;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPageSite;

public class BpmoContentOutlinePage extends ContentOutlinePage {

    private TreeViewer treeViewer = null;

    ScrollableThumbnail thumbnail = null;

    private Canvas canvas;

    private BpmoEditor editor;

    public BpmoContentOutlinePage(ActionRegistry registry, BpmoEditor editor) {
        super(new TreeViewer());
        this.treeViewer = (TreeViewer) this.getViewer();
        this.editor = editor;
    }

    @Override
    public void init(IPageSite pageSite) {
        super.init(pageSite);
        ActionRegistry editorRegistry = (ActionRegistry) this.editor.getAdapter(ActionRegistry.class);
        IActionBars bars = pageSite.getActionBars();
        String id = ActionFactory.UNDO.getId();
        bars.setGlobalActionHandler(id, editorRegistry.getAction(id));
        id = ActionFactory.REDO.getId();
        bars.setGlobalActionHandler(id, editorRegistry.getAction(id));
        id = ActionFactory.DELETE.getId();
        bars.setGlobalActionHandler(id, editorRegistry.getAction(id));
        bars.updateActionBars();
    }

    @Override
    public void createControl(Composite parent) {
        EditDomain editDomain = (EditDomain) this.editor.getAdapter(EditDomain.class);
        this.treeViewer.setEditDomain(editDomain);
        SelectionSynchronizer synchronizer = (SelectionSynchronizer) this.editor.getAdapter(SelectionSynchronizer.class);
        synchronizer.addViewer(this.treeViewer);
        canvas = new Canvas(parent, SWT.BORDER);
        LightweightSystem lws = new LightweightSystem(canvas);
        this.treeViewer.createControl(canvas);
        GraphicalViewer viewer = (GraphicalViewer) this.editor.getAdapter(GraphicalViewer.class);
        ScalableRootEditPart root = (ScalableRootEditPart) viewer.getRootEditPart();
        this.thumbnail = new ScrollableThumbnail((Viewport) root.getFigure());
        this.thumbnail.setSource(root.getLayer(LayerConstants.PRINTABLE_LAYERS));
        lws.setContents(this.thumbnail);
    }

    @Override
    public Control getControl() {
        return this.canvas;
    }

    @Override
    public void dispose() {
        SelectionSynchronizer synchronizer = (SelectionSynchronizer) this.editor.getAdapter(SelectionSynchronizer.class);
        synchronizer.removeViewer(this.treeViewer);
        super.dispose();
    }
}
