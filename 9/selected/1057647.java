package net.sf.redsetter.editor;

import net.sf.redsetter.factory.MappingEditPartFactory;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorSite;

/**
 * Functionality for configuring the GraphicalViewer
 */
public class GraphicalViewerCreator {

    private GraphicalViewer viewer;

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
        viewer.setEditPartFactory(getEditPartFactory());
        return viewer;
    }

    /**
	 * Returns the <code>EditPartFactory</code> that the
	 * <code>GraphicalViewer</code> will use.
	 * 
	 * @return the <code>EditPartFactory</code>
	 */
    protected EditPartFactory getEditPartFactory() {
        return new MappingEditPartFactory();
    }

    /**
	 * @return Returns the viewer.
	 */
    public GraphicalViewer getViewer() {
        return viewer;
    }
}
