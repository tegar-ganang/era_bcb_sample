package net.sf.figuredeveloper.views;

import java.util.logging.Logger;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import net.sf.sandbox.models.GenericModelStore;

public abstract class GenericGraphicalView extends ViewPart {

    private static Logger logger = Logger.getLogger("net.sf.figuredeveloper");

    private IViewSite viewSite;

    private DefaultEditDomain editDomain;

    private GraphicalViewer graphicalViewer;

    private SelectionSynchronizer synchronizer;

    /** This is the structure that contains the model data to be presented on the viewer. */
    private GenericModelStore dataContainer;

    public GenericGraphicalView() {
        this.setEditDomain(new DefaultEditDomain(null));
    }

    @Override
    public void createPartControl(final Composite parent) {
        final GraphicalViewer viewer = new ScrollingGraphicalViewer();
        viewer.createControl(parent);
        this.getEditDomain().addViewer(viewer);
        graphicalViewer = viewer;
        this.configureGraphicalViewer();
        {
            this.getSelectionSynchronizer().addViewer(this.getGraphicalViewer());
            this.getSite().setSelectionProvider(this.getGraphicalViewer());
        }
        this.initializeGraphicalViewer();
    }

    /**
	 * This <code>init</code> has to create and connect the <code>AbstractModelStore</code> to the Viewer. For
	 * this reason any new View must declare this method and perform the initialization of such fields.
	 */
    @Override
    public void init(final IViewSite site) throws PartInitException {
        viewSite = site;
        super.init(site);
    }

    /**
	 * Passing the focus request to the viewer's control.
	 */
    @Override
    public void setFocus() {
        graphicalViewer.getControl().setFocus();
    }

    /**
	 * Called to configure the graphical viewer before it receives its contents. This is where the root editpart
	 * should be configured. Subclasses should extend or override this method as needed.
	 */
    protected void configureGraphicalViewer() {
        this.getGraphicalViewer().getControl().setBackground(ColorConstants.white);
    }

    protected GenericModelStore getContainer() {
        return dataContainer;
    }

    protected GenericModelStore getContents() {
        return this.getContainer();
    }

    protected DefaultEditDomain getEditDomain() {
        return editDomain;
    }

    protected GraphicalViewer getGraphicalViewer() {
        return graphicalViewer;
    }

    public void setContainer(GenericModelStore dataContainer) {
        this.dataContainer = dataContainer;
    }

    /**
	 * Returns the selection synchronizer object. The synchronizer can be used to sync the selection of 2 or
	 * more EditPartViewers.
	 * 
	 * @return the synchronizer
	 */
    protected SelectionSynchronizer getSelectionSynchronizer() {
        if (synchronizer == null) synchronizer = new SelectionSynchronizer();
        return synchronizer;
    }

    protected void initializeGraphicalViewer() {
        final GraphicalViewer viewer = this.getGraphicalViewer();
        viewer.setContents(this.getContents());
    }

    /**
	 * Sets the EditDomain for this EditorPart.
	 * 
	 * @param ed
	 *          the domain
	 */
    protected void setEditDomain(final DefaultEditDomain ed) {
        editDomain = ed;
    }
}
