package net.sf.vgap4.assistant.views;

import java.util.Iterator;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.AbstractEditPart;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import net.sf.vgap4.assistant.editor.GraphicalEditor;
import net.sf.vgap4.assistant.factories.DetailedEditPartFactory;
import net.sf.vgap4.assistant.factories.DetailedFigureFactory;
import net.sf.vgap4.assistant.models.AssistantMap;
import net.sf.vgap4.assistant.models.AssistantNode;
import net.sf.vgap4.assistant.models.DetailedDiagram;
import net.sf.vgap4.assistant.models.Spot;
import net.sf.vgap4.assistant.ui.Activator;
import net.sf.vgap4.projecteditor.editparts.ISelectablePart;

public class GraphicalDetailedView extends ViewPart implements ISelectionListener, ISelectionChangedListener {

    public static final String ID = "net.sf.vgap4.assistant.views.GraphicalDetailedView.id";

    private static Logger logger = Logger.getLogger("net.sf.vgap4.assistant.views");

    /** The view cannot be an editor at the same time, so delegate all editor actions to this editor. */
    GraphicalDetailedEditor detailEditor = null;

    /** This is the root of the editor's model. */
    private DetailedDiagram editorContainer = new DetailedDiagram();

    private Composite viewerRoot;

    private IViewSite viewSite;

    public GraphicalDetailedView() {
        Activator.addReference(GraphicalDetailedView.ID, this);
    }

    /**
	 * This is the method called during creation and initialization of the view. The view must be able to change
	 * their presentation dynamically depending on the selection, so there should be a link point where other
	 * content structures can plug-in to be displayed.<br>
	 * This class will set as the top presentation element a new <code>GraphicalDetailedEditor</code> that
	 * will present the selection received as a new MVC pattern
	 */
    @Override
    public void createPartControl(final Composite parent) {
        this.viewerRoot = parent;
        this.detailEditor = new GraphicalDetailedEditor(parent, this);
    }

    @Override
    public void dispose() {
        final Object provider = Activator.getByID("SelectionInfoView.SelectionProvider");
        if (null != provider) ((ISelectionProvider) provider).removeSelectionChangedListener(this);
        super.dispose();
    }

    @Override
    public void init(final IViewSite site) throws PartInitException {
        this.viewSite = site;
        super.init(site);
        final Object provider = Activator.getByID("SelectionInfoView.SelectionProvider");
        if (null != provider) ((ISelectionProvider) provider).addSelectionChangedListener(this);
    }

    /**
	 * This event is fired any time the selection in the <code>SelectionInfoView</code> is changed. This
	 * method should get the selection parts that match a <code>ISelectablePart</code> and then create their
	 * visualization page to be added to the presentation list.<br>
	 * The parameter is a selection event that contains the final selection.<br>
	 * If the selection is a single object then visualize all their contents, but if the selection are multiple
	 * object, present them in the reduced form and let the user to click on them to expand their contents.
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
    public void selectionChanged(final IWorkbenchPart editorPart, final ISelection selection) {
        if ((!selection.isEmpty()) && (selection instanceof StructuredSelection)) {
            processSelection((StructuredSelection) selection);
        }
    }

    public void selectionChanged(final SelectionChangedEvent event) {
        final ISelection selection = event.getSelection();
        if ((!selection.isEmpty()) && (selection instanceof StructuredSelection)) {
            processSelection((StructuredSelection) selection);
        }
    }

    private void processSelection(StructuredSelection selection) {
        final Iterator<Object> sit = (selection).iterator();
        this.editorContainer.clear();
        while (sit.hasNext()) {
            final Object element = sit.next();
            if (element instanceof ISelectablePart) {
                Object model = ((AbstractEditPart) element).getModel();
                if (model instanceof Spot) {
                    this.editorContainer.addChild(((Spot) model).getContents());
                    continue;
                }
                if (model instanceof AssistantNode) this.editorContainer.addChild((AssistantNode) model);
            }
            if (element instanceof Spot) {
                this.editorContainer.addChild(((Spot) element).getContents());
                continue;
            }
            if (element instanceof AssistantNode) this.editorContainer.addChild((AssistantNode) element);
        }
        this.editorContainer.fireStructureChange(AssistantMap.DATA_ADDED_PROP, null, null);
    }

    public DetailedDiagram getContainer() {
        return this.editorContainer;
    }

    /**
	 * Passing the focus request to the viewer's control.
	 */
    @Override
    public void setFocus() {
        this.detailEditor.setFocus();
    }
}

class GraphicalDetailedEditor extends GraphicalEditor {

    private static Logger logger = Logger.getLogger("net.sf.vgap4.assistant.views");

    private static final String ID = "net.sf.vgap4.assistant.editors.GraphicalDetailedEditor.id";

    private GraphicalDetailedView detailedView;

    public GraphicalDetailedEditor(Composite parent, GraphicalDetailedView detailedView) {
        try {
            setEditDomain(new DefaultEditDomain(this));
            this.detailedView = detailedView;
            Activator.addReference(ID, this);
            init(this.detailedView.getSite());
            this.createGraphicalViewer(parent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DetailedDiagram getContents() {
        if (null != detailedView) return this.detailedView.getContainer(); else return new DetailedDiagram();
    }

    public void init(IWorkbenchPartSite site) throws PartInitException {
        EmptyEditorSite editorSite = new EmptyEditorSite(site);
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
        viewer.setEditPartFactory(new DetailedEditPartFactory(new DetailedFigureFactory()));
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }
}

class EmptyEditorSite implements IEditorSite {

    private IWorkbenchPartSite workbenchSite;

    public EmptyEditorSite(IWorkbenchPartSite site) {
        this.workbenchSite = site;
    }

    public Object getAdapter(Class adapter) {
        return workbenchSite.getAdapter(adapter);
    }

    public String getId() {
        return workbenchSite.getId();
    }

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

    public Object getService(Class api) {
        return workbenchSite.getService(api);
    }

    public Shell getShell() {
        return workbenchSite.getShell();
    }

    public IWorkbenchWindow getWorkbenchWindow() {
        return workbenchSite.getWorkbenchWindow();
    }

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
