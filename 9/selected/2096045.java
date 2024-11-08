package net.entropysoft.dashboard.plugin.dashboard.outline;

import net.entropysoft.dashboard.plugin.dashboard.DashboardContextMenuProvider;
import net.entropysoft.dashboard.plugin.dashboard.DashboardEditor;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPageSite;

/**
 * Outline page for the dashboard 
 * @author cedric
 */
public class DashboardOutlinePage extends ContentOutlinePage {

    private DashboardEditor dashboardEditor;

    private SashForm sashForm;

    private ScrollableThumbnail thumbnail;

    public DashboardOutlinePage(DashboardEditor dashboardEditor) {
        super(new TreeViewer());
        this.dashboardEditor = dashboardEditor;
    }

    public void createControl(Composite parent) {
        sashForm = new SashForm(parent, SWT.VERTICAL);
        getViewer().createControl(sashForm);
        getViewer().setEditDomain(getEditDomain());
        getViewer().setEditPartFactory(new DashboardTreeEditPartFactory());
        ContextMenuProvider cmProvider = new DashboardContextMenuProvider(getViewer(), getActionRegistry());
        getViewer().setContextMenu(cmProvider);
        getSite().registerContextMenu("net.entropysoft.dashboard.plugin.dashboard.outline.contextmenu", cmProvider, getSite().getSelectionProvider());
        getSelectionSynchronizer().addViewer(getViewer());
        getViewer().setContents(dashboardEditor.getDashboard());
        ScalableRootEditPart rootEditPart = (ScalableRootEditPart) getGraphicalViewer().getRootEditPart();
        Canvas canvas = new Canvas(sashForm, SWT.BORDER);
        LightweightSystem lightweightSystem = new LightweightSystem(canvas);
        thumbnail = new ScrollableThumbnail((Viewport) rootEditPart.getFigure());
        thumbnail.setSource(rootEditPart.getLayer(LayerConstants.PRINTABLE_LAYERS));
        lightweightSystem.setContents(thumbnail);
    }

    private ActionRegistry getActionRegistry() {
        return (ActionRegistry) dashboardEditor.getAdapter(ActionRegistry.class);
    }

    private SelectionSynchronizer getSelectionSynchronizer() {
        return (SelectionSynchronizer) dashboardEditor.getAdapter(SelectionSynchronizer.class);
    }

    private EditDomain getEditDomain() {
        return (EditDomain) dashboardEditor.getAdapter(EditDomain.class);
    }

    private GraphicalViewer getGraphicalViewer() {
        return (GraphicalViewer) dashboardEditor.getAdapter(GraphicalViewer.class);
    }

    public void dispose() {
        getSelectionSynchronizer().removeViewer(getViewer());
        thumbnail.deactivate();
        super.dispose();
    }

    public Control getControl() {
        return sashForm;
    }

    public void init(IPageSite pageSite) {
        super.init(pageSite);
        ActionRegistry registry = getActionRegistry();
        IActionBars bars = pageSite.getActionBars();
        String id = ActionFactory.UNDO.getId();
        bars.setGlobalActionHandler(id, registry.getAction(id));
        id = ActionFactory.REDO.getId();
        bars.setGlobalActionHandler(id, registry.getAction(id));
        id = ActionFactory.DELETE.getId();
        bars.setGlobalActionHandler(id, registry.getAction(id));
    }
}
