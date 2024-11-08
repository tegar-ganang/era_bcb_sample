package net.sf.figuredeveloper.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import net.sf.figuredeveloper.factories.FigureDeveloperEditPartFactory;
import net.sf.figuredeveloper.factories.FigureDeveloperFigureFactory;
import net.sf.figuredeveloper.models.FigureDeveloperModelStore;
import net.sf.gef.core.models.AbstractGEFNode;
import net.sf.sandbox.app.SandBoxActivator;
import net.sf.sandbox.models.GenericModelStore;
import net.sf.sandbox.models.UISandBoxModelStore;

public class FigureDeveloperView extends GenericGraphicalView implements PropertyChangeListener {

    private static Logger logger = Logger.getLogger("net.sf.figuredeveloper");

    public static final String ID = "net.sf.figuredeveloper.views.FigureDeveloperView.id";

    public static final String FIGUREDEVELOPERDRAFTMODELSTORE_ID = "net.sf.figuredeveloper.views.FigureDeveloperView.ModelStore.id";

    public FigureDeveloperView() {
    }

    @Override
    public void init(final IViewSite site) throws PartInitException {
        super.init(site);
        final GenericModelStore container = (GenericModelStore) SandBoxActivator.getByID(FigureDeveloperView.FIGUREDEVELOPERDRAFTMODELSTORE_ID);
        if (null == container) {
            this.setContainer(new FigureDeveloperModelStore());
            SandBoxActivator.addReference(FigureDeveloperView.FIGUREDEVELOPERDRAFTMODELSTORE_ID, this.getContainer());
        }
        this.getContainer().addPropertyChangeListener(this);
    }

    public void propertyChange(final PropertyChangeEvent evt) {
        final String prop = evt.getPropertyName();
        if (AbstractGEFNode.CHILD_ADDED_PROP.equals(prop)) this.getContainer().fireStructureChange(UISandBoxModelStore.MODEL_STRUCTURE_CHANGED, null, null);
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        final GraphicalViewer viewer = this.getGraphicalViewer();
        viewer.setRootEditPart(new ScalableRootEditPart());
        viewer.setEditPartFactory(new FigureDeveloperEditPartFactory(new FigureDeveloperFigureFactory()));
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
    }
}
