package net.confex.schema.editor;

import net.confex.schema.directedit.StatusLineValidationMessageHandler;
import net.confex.schema.dnd.DataEditDropTargetListener;
import net.confex.schema.factory.SchemaEditPartFactory;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorSite;

/**
 * Functionality for configuring the GraphicalViewer
 * @author Eremeev Roman
 */
public class GraphicalViewerCreator {

    private KeyHandler sharedKeyHandler;

    private GraphicalViewer viewer;

    /** the editor's action registry */
    private ActionRegistry actionRegistry;

    private IEditorSite editorSite;

    /**
	 * @param editorSite
	 */
    public GraphicalViewerCreator(IEditorSite editorSite) {
        this.editorSite = editorSite;
    }

    /**
	 * Creates a new <code>PaletteViewer</code>, configures, registers and
	 * initializes it.
	 * 
	 * @param parent
	 *            the parent composite
	 */
    public void createGraphicalViewer(Composite parent) {
        viewer = createViewer(parent);
    }

    protected ContextMenuProvider getContextMenuProvider(EditPartViewer viewer) {
        return new DefaultContextMenuProvider(viewer, actionRegistry);
    }

    /**
	 * @param parent
	 * @return
	 */
    protected GraphicalViewer createViewer(Composite parent) {
        StatusLineValidationMessageHandler validationMessageHandler = new StatusLineValidationMessageHandler(editorSite);
        GraphicalViewer viewer = new ValidationEnabledGraphicalViewer(validationMessageHandler);
        viewer.createControl(parent);
        viewer.getControl().setBackground(ColorConstants.white);
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        viewer.addDropTargetListener(new DataEditDropTargetListener(viewer));
        viewer.setEditPartFactory(getEditPartFactory());
        ContextMenuProvider provider = getContextMenuProvider(viewer);
        viewer.setContextMenu(provider);
        return viewer;
    }

    /**
	 * Returns the <code>EditPartFactory</code> that the
	 * <code>GraphicalViewer</code> will use.
	 * 
	 * @return the <code>EditPartFactory</code>
	 */
    protected EditPartFactory getEditPartFactory() {
        return new SchemaEditPartFactory();
    }

    /**
	 * @return Returns the viewer.
	 */
    public GraphicalViewer getViewer() {
        return viewer;
    }
}
