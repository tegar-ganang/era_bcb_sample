package org.plazmaforge.studio.modeling.j2d;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.swt.widgets.Composite;

public abstract class J2DGraphicalEditorWithFlyoutPalette extends GraphicalEditorWithFlyoutPalette {

    /**
	 * Instantiates a new j2 d graphical editor with flyout palette.
	 */
    public J2DGraphicalEditorWithFlyoutPalette() {
        super();
    }

    /**
	 * Creates the GraphicalViewer on the specified <code>Composite</code>. A
	 * J2DScrollingGraphicalViewer is internally created.
	 * 
	 * @param parent
	 *            The parent composite
	 */
    protected void createGraphicalViewer(Composite parent) {
        System.out.println("Creating graphical viewer!!1");
        System.out.flush();
        GraphicalViewer viewer = new J2DScrollingGraphicalViewer();
        viewer.createControl(parent);
        setGraphicalViewer(viewer);
        configureGraphicalViewer();
        hookGraphicalViewer();
        initializeGraphicalViewer();
    }
}
