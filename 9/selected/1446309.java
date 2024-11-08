package net.sf.graphiti.ui.editors;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.draw2d.parts.Thumbnail;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * This class provides a thumbnail outline page.
 * 
 * @author Matthieu Wipliez
 * 
 */
public class ThumbnailOutlinePage extends ContentOutlinePage {

    private Canvas canvas;

    private DisposeListener disposeListener;

    private GraphEditor editor;

    private Thumbnail thumbnail;

    public ThumbnailOutlinePage(GraphEditor editor) {
        super(new GraphicalViewerImpl());
        this.editor = editor;
    }

    public void createControl(Composite parent) {
        canvas = new Canvas(parent, SWT.BORDER);
        LightweightSystem lws = new LightweightSystem(canvas);
        RootEditPart root = editor.getGraphicalViewer().getRootEditPart();
        ScalableFreeformRootEditPart scalable = (ScalableFreeformRootEditPart) root;
        thumbnail = new ScrollableThumbnail((Viewport) scalable.getFigure());
        thumbnail.setSource(scalable.getLayer(LayerConstants.PRINTABLE_LAYERS));
        lws.setContents(thumbnail);
        disposeListener = new DisposeListener() {

            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (thumbnail != null) {
                    thumbnail.deactivate();
                    thumbnail = null;
                }
            }
        };
        Control control = editor.getGraphicalViewer().getControl();
        control.addDisposeListener(disposeListener);
    }

    public void dispose() {
        editor.getSelectionSynchronizer().removeViewer(getViewer());
        Control control = editor.getGraphicalViewer().getControl();
        if (control != null && !control.isDisposed()) {
            control.removeDisposeListener(disposeListener);
        }
        super.dispose();
    }

    public Control getControl() {
        return canvas;
    }
}
