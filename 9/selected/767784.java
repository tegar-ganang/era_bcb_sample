package net.sf.sandbox.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import org.apache.batik.swing.JSVGCanvas;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import net.sf.gef.core.editors.BaseGraphicalEditor;
import net.sf.gef.core.model.AbstractGEFNode;
import net.sf.sandbox.models.UISandBoxModelStore;

public class DraftHexagonView extends View implements PropertyChangeListener {

    private static Logger logger = Logger.getLogger("net.sf.sandbox.rcpapplication");

    public static final String ID = "net.sf.sandbox.views.DraftHexagonView.id";

    /** The view cannot be an editor at the same time, so delegate all editor actions to this editor. */
    LocalGraphicalDetailedEditor detailEditor = null;

    /** This is the root of the editor's model. */
    private UISandBoxModelStore editorContainer;

    private Composite viewerRoot;

    private IViewSite viewSite;

    /** The SVG canvas. */
    protected JSVGCanvas svgCanvas = null;

    public DraftHexagonView() {
    }

    /**
	 * This is a required method that is get called when the view is created and is responsible for the creation
	 * of all the view data structures and content management. This is the method called during creation and
	 * initialization of the view. The view must be able to change their presentation dynamically depending on
	 * the selection, so there should be a link point where other content structures can plug-in to be
	 * displayed.<br>
	 * This class will set as the top presentation element of a new <code>GraphicalDetailedEditor</code> that
	 * will present the selection received as a new MVC pattern
	 */
    @Override
    public void createPartControl(final Composite parent) {
        viewerRoot = parent;
        detailEditor = new LocalGraphicalDetailedEditor(parent, this);
    }

    public UISandBoxModelStore getContainer() {
        return editorContainer;
    }

    /**
	 * Passing the focus request to the viewer's control.
	 */
    @Override
    public void setFocus() {
        detailEditor.setFocus();
    }

    @Override
    public void init(final IViewSite site) throws PartInitException {
        viewSite = site;
        super.init(site);
        if (null == editorContainer) {
            editorContainer = new UISandBoxModelStore();
            svgCanvas.addPropertyChangeListener(this);
        }
        editorContainer.addPropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        final String prop = evt.getPropertyName();
        if (AbstractGEFNode.CHILD_ADDED_PROP.equals(prop)) {
            editorContainer.fireStructureChange(UISandBoxModelStore.MODEL_STRUCTURE_CHANGED, null, null);
        }
    }
}

class LocalGraphicalDetailedEditor extends BaseGraphicalEditor {

    private static final String ID = "net.sf.sandbox.DraftHexagonView.LocalGraphicalDetailedEditor.id";

    private DraftHexagonView detailedView;

    public LocalGraphicalDetailedEditor(Composite parent, DraftHexagonView detailedView) {
        try {
            setEditDomain(new DefaultEditDomain(this));
            this.detailedView = detailedView;
            init(this.detailedView.getSite());
            createGraphicalViewer(parent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public UISandBoxModelStore getContents() {
        if (null != detailedView) return detailedView.getContainer(); else return new UISandBoxModelStore();
    }

    public void init(IWorkbenchPartSite site) throws PartInitException {
        LocalEmptyEditorSite editorSite = new LocalEmptyEditorSite(site);
        setSite(editorSite);
        setInput(null);
        getCommandStack().addCommandStackListener(this);
        initializeActionRegistry();
    }

    @Override
    protected void initializeGraphicalViewer() {
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getContents());
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setRootEditPart(new ScalableRootEditPart());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }
}
