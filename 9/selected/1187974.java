package net.sf.vorg.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import net.sf.gef.core.model.AbstractGEFNode;
import net.sf.vorg.app.Activator;
import net.sf.vorg.factories.PilotEditPartFactory;
import net.sf.vorg.factories.PilotFigureFactory;
import net.sf.vorg.models.UIPilotModelStore;
import net.sf.vorg.vorgautopilot.models.PilotModelStore;

@SuppressWarnings("deprecation")
public class PilotStatusView extends ViewPart implements PropertyChangeListener {

    public static final String ID = "net.sf.vorg.views.PilotStatusView.id";

    /** The view cannot be an editor at the same time, so delegate all editor actions to this editor. */
    LocalGraphicalDetailedEditor detailEditor = null;

    /** This is the root of the editor's model. */
    private PilotModelStore editorContainer;

    @SuppressWarnings("unused")
    private Composite viewerRoot;

    @SuppressWarnings("unused")
    private IViewSite viewSite;

    public PilotStatusView() {
        Activator.addReference(PilotStatusView.ID, this);
    }

    /**
	 * This is the method called during creation and initialization of the view. The view must be able to change
	 * their presentation dynamically depending on the selection, so there should be a link point where other
	 * content structures can plug-in to be displayed.<br>
	 * This class will set as the top presentation element a new <code>GraphicalDetailedEditor</code> that will
	 * present the selection received as a new MVC pattern
	 */
    @Override
    public void createPartControl(final Composite parent) {
        viewerRoot = parent;
        detailEditor = new LocalGraphicalDetailedEditor(parent, this);
    }

    @Override
    public void dispose() {
        if (null != editorContainer) {
            editorContainer.removePropertyChangeListener(this);
        }
        super.dispose();
    }

    @Override
    public void init(final IViewSite site) throws PartInitException {
        viewSite = site;
        super.init(site);
        editorContainer = (UIPilotModelStore) Activator.getByID(UIPilotModelStore.PILOTMODELID);
        if (null == editorContainer) {
            editorContainer = new UIPilotModelStore();
        }
        editorContainer.addPropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        final String prop = evt.getPropertyName();
        if (AbstractGEFNode.CHILD_ADDED_PROP.equals(prop)) {
            editorContainer.fireStructureChange(PilotModelStore.MODEL_STRUCTURE_CHANGED, null, null);
        }
    }

    public PilotModelStore getContainer() {
        return editorContainer;
    }

    /**
	 * Passing the focus request to the viewer's control.
	 */
    @Override
    public void setFocus() {
        detailEditor.setFocus();
    }
}

class LocalGraphicalDetailedEditor extends BaseGraphicalEditor {

    private static final String ID = "net.sf.vorg.editors.LocalGraphicalDetailedEditor.id";

    private PilotStatusView detailedView;

    public LocalGraphicalDetailedEditor(Composite parent, PilotStatusView detailedView) {
        try {
            setEditDomain(new DefaultEditDomain(this));
            this.detailedView = detailedView;
            Activator.addReference(ID, this);
            init(this.detailedView.getSite());
            createGraphicalViewer(parent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PilotModelStore getContents() {
        if (null != detailedView) return detailedView.getContainer(); else return new PilotModelStore();
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
        viewer.setEditPartFactory(new PilotEditPartFactory(new PilotFigureFactory()));
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }
}

class LocalEmptyEditorSite implements IEditorSite {

    private final IWorkbenchPartSite workbenchSite;

    public LocalEmptyEditorSite(IWorkbenchPartSite site) {
        workbenchSite = site;
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class adapter) {
        return workbenchSite.getAdapter(adapter);
    }

    public String getId() {
        return workbenchSite.getId();
    }

    @SuppressWarnings("deprecation")
    public IKeyBindingService getKeyBindingService() {
        return workbenchSite.getKeyBindingService();
    }

    public IWorkbenchPage getPage() {
        return workbenchSite.getPage();
    }

    public IWorkbenchPart getPart() {
        return workbenchSite.getPart();
    }

    public String getPluginId() {
        return workbenchSite.getPluginId();
    }

    public String getRegisteredName() {
        return workbenchSite.getRegisteredName();
    }

    public ISelectionProvider getSelectionProvider() {
        return workbenchSite.getSelectionProvider();
    }

    @SuppressWarnings("unchecked")
    public Object getService(Class api) {
        return workbenchSite.getService(api);
    }

    public Shell getShell() {
        return workbenchSite.getShell();
    }

    public IWorkbenchWindow getWorkbenchWindow() {
        return workbenchSite.getWorkbenchWindow();
    }

    @SuppressWarnings("unchecked")
    public boolean hasService(Class api) {
        return workbenchSite.hasService(api);
    }

    public void registerContextMenu(MenuManager menuManager, ISelectionProvider selectionProvider) {
        workbenchSite.registerContextMenu(menuManager, selectionProvider);
    }

    public void registerContextMenu(String menuId, MenuManager menuManager, ISelectionProvider selectionProvider) {
        workbenchSite.registerContextMenu(menuId, menuManager, selectionProvider);
    }

    public void setSelectionProvider(ISelectionProvider provider) {
        workbenchSite.setSelectionProvider(provider);
    }

    public IEditorActionBarContributor getActionBarContributor() {
        return null;
    }

    public IActionBars getActionBars() {
        return null;
    }

    public void registerContextMenu(MenuManager menuManager, ISelectionProvider selectionProvider, boolean includeEditorInput) {
    }

    public void registerContextMenu(String menuId, MenuManager menuManager, ISelectionProvider selectionProvider, boolean includeEditorInput) {
    }
}
