package org.plazmaforge.studio.modeling.j2d;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.swt.widgets.Composite;

public abstract class J2DGraphicalEditor extends GraphicalEditor {

    /**
	 * Instantiates a new j2 d graphical editor with flyout palette.
	 */
    public J2DGraphicalEditor() {
        super();
    }

    /**
	 * Creates the GraphicalViewer on the specified <code>Composite</code>. A J2DScrollingGraphicalViewer is internally
	 * created.
	 * 
	 * @param parent
	 *          The parent composite
	 */
    protected void createGraphicalViewer(Composite parent) {
        GraphicalViewer viewer = new J2DScrollingGraphicalViewer();
        viewer.createControl(parent);
        setGraphicalViewer(viewer);
        configureGraphicalViewer();
        hookGraphicalViewer();
        initializeGraphicalViewer();
    }
}
