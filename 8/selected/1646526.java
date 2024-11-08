package de.tud.eclipse.plugins.controlflow.view;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.editparts.FreeformGraphicalRootEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import de.tud.eclipse.plugins.controlflow.control.settings.ViewSettings;
import de.tud.eclipse.plugins.controlflow.model.ICFlowModel;
import de.tud.eclipse.plugins.controlflow.model.ICFlowNode;
import de.tud.eclipse.plugins.controlflow.view.gef.CFlowEditPartFactory;
import de.tud.eclipse.plugins.controlflow.view.gef.CFlowGraphicalEditor;

/**
 * The view that creates the graph
 * @author leo, E. Stoffregen
 *
 */
public class GraphViewer implements PropertyChangeListener, ViewBackend {

    ScrollingGraphicalViewer graphicalViewer;

    /**
	 * The viewer which scrolls synchronously with this viewer
	 */
    private GraphViewer connectedView = null;

    /**
	 * Avoids event listener loops
	 */
    private boolean muteScrollUpdate = false;

    private ICFlowModel model = null;

    private ViewSettings settings;

    private ICFlowNode currentlySelectedNode = null;

    public GraphViewer(Composite comp, ViewSettings settings) {
        this.settings = settings;
        graphicalViewer = new ScrollingGraphicalViewer();
        graphicalViewer.createControl(comp);
        CFlowGraphicalEditor editor = new CFlowGraphicalEditor(this);
        graphicalViewer.setEditDomain(new DefaultEditDomain(editor));
        graphicalViewer.setRootEditPart(new FreeformGraphicalRootEditPart());
        graphicalViewer.setEditPartFactory(new CFlowEditPartFactory(this));
        graphicalViewer.getRootEditPart().getViewer().getControl().setBackground(comp.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        getFigureCanvas().getViewport().addPropertyChangeListener(Viewport.PROPERTY_VIEW_LOCATION, this);
    }

    /**
	 * Returns the the FigureCanvas used to draw the components.
	 * @return
	 */
    public FigureCanvas getFigureCanvas() {
        return (FigureCanvas) graphicalViewer.getControl();
    }

    /**
	 * Connects this viewer to another viewer, e.g. for comparison
	 * and scrolling.
	 * @param scrollBuddy
	 */
    public void connectTo(GraphViewer scrollBuddy) {
        this.connectedView = scrollBuddy;
    }

    /**
	 * Returns the SWT control of the graphical viewer.
	 * @return
	 */
    public Control getControl() {
        return graphicalViewer.getControl();
    }

    public void setContent(ICFlowModel model) {
        this.model = model;
        graphicalViewer.setContents(model);
    }

    public ICFlowModel getContent() {
        return model;
    }

    /**
	 * Updates the scroll position of the connected view.
	 */
    protected void updateScroll() {
        if (connectedView != null && !muteScrollUpdate) {
            Point loc = this.getFigureCanvas().getViewport().getViewLocation();
            connectedView.muteScrollUpdate = true;
            connectedView.getFigureCanvas().getViewport().setViewLocation(loc);
            connectedView.muteScrollUpdate = false;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent arg0) {
        if (arg0.getPropertyName() == Viewport.PROPERTY_VIEW_LOCATION) updateScroll();
    }

    public ViewSettings getSettings() {
        return settings;
    }

    public ICFlowNode getCurrentlySelectedNode() {
        return currentlySelectedNode;
    }

    public void setCurrentlySelectedNode(ICFlowNode currentlySelectedNode) {
        this.currentlySelectedNode = currentlySelectedNode;
        if (connectedView != null) connectedView.currentlySelectedNode = currentlySelectedNode;
        settings.settingsChanged(ViewSettings.OTHER_NODE_SELECTED);
    }
}
