package net.sourceforge.copernicus.client;

import java.util.ArrayList;
import net.sourceforge.copernicus.client.cim.CimClient;
import net.sourceforge.copernicus.client.cim.SblimWbemClient;
import net.sourceforge.copernicus.client.controller.Configuration;
import net.sourceforge.copernicus.client.controller.Factory;
import net.sourceforge.copernicus.client.controller.editparts.tree.TreeFactory;
import net.sourceforge.copernicus.client.controller.windows.LoginDialog;
import net.sourceforge.copernicus.client.model.ModelConfiguration;
import net.sourceforge.copernicus.client.model.SchemaEventsHandler;
import net.sourceforge.copernicus.client.model.gef.GefSchema;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class GraphicalEditor extends org.eclipse.gef.ui.parts.GraphicalEditor implements SchemaEventsHandler {

    public static final String ID = "net.sourceforge.copernicus.client.graphicaleditor";

    private GefSchema model;

    private CimClient cimClient;

    protected class OutlinePage extends ContentOutlinePage {

        private SashForm sashForm;

        private ScrollableThumbnail thumbnail;

        private DisposeListener disposeListener;

        public OutlinePage() {
            super(new TreeViewer());
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public void createControl(Composite parent) {
            sashForm = new SashForm(parent, SWT.VERTICAL);
            EditPartViewer viewer = getViewer();
            viewer.createControl(sashForm);
            viewer.setEditDomain(getEditDomain());
            viewer.setEditPartFactory(new TreeFactory());
            viewer.setContents(model);
            getSelectionSynchronizer().addViewer(viewer);
            Canvas canvas = new Canvas(sashForm, SWT.BORDER);
            LightweightSystem lws = new LightweightSystem(canvas);
            ScalableRootEditPart rootEditPart = (ScalableRootEditPart) getGraphicalViewer().getRootEditPart();
            thumbnail = new ScrollableThumbnail((Viewport) rootEditPart.getFigure());
            thumbnail.setSource(rootEditPart.getLayer(LayerConstants.PRINTABLE_LAYERS));
            lws.setContents(thumbnail);
            disposeListener = new DisposeListener() {

                public void widgetDisposed(DisposeEvent e) {
                    if (thumbnail != null) {
                        thumbnail.deactivate();
                        thumbnail = null;
                    }
                }
            };
            getGraphicalViewer().getControl().addDisposeListener(disposeListener);
        }

        @Override
        public Control getControl() {
            return sashForm;
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public void dispose() {
            getSelectionSynchronizer().removeViewer(getViewer());
            Control control = getGraphicalViewer().getControl();
            if (control != null && !control.isDisposed()) control.removeDisposeListener(disposeListener);
            super.dispose();
        }
    }

    public GraphicalEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    @Override
    protected void initializeGraphicalViewer() {
        try {
            LoginDialog loginDialog = new LoginDialog(getGraphicalViewer().getControl().getShell(), Configuration.getInstance());
            loginDialog.setBlockOnOpen(true);
            loginDialog.open();
            if (loginDialog.getReturnCode() == Window.OK) {
                Configuration config = Configuration.getInstance();
                ModelConfiguration modelConfig = config.getModelConfiguration();
                cimClient = new SblimWbemClient(modelConfig.getCimNamespace(), config.getLocaleName());
                cimClient.connect(config.getServerUrl(), modelConfig.getCimUsername(), modelConfig.getCimPassword());
                model = new GefSchema(modelConfig, this, cimClient, getGraphicalViewer().getControl().getDisplay());
                model.enumerateRootInstances();
                getGraphicalViewer().setContents(model);
                model.setMode(GefSchema.Mode.AFTER_ENUMERATE);
                cimClient.subscribeToIndications(model);
            } else {
                PlatformUI.getWorkbench().close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new Factory());
        ScalableRootEditPart rootEditPart = new ScalableRootEditPart();
        viewer.setRootEditPart(rootEditPart);
        ZoomManager zoomManager = rootEditPart.getZoomManager();
        getActionRegistry().registerAction(new ZoomInAction(zoomManager));
        getActionRegistry().registerAction(new ZoomOutAction(zoomManager));
        zoomManager.setZoomLevels(new double[] { 0.25, 0.5, 0.75, 1.0, 1.25, 1.5 });
        ArrayList<String> zoomContributions = new ArrayList<String>();
        zoomContributions.add(ZoomManager.FIT_ALL);
        zoomContributions.add(ZoomManager.FIT_HEIGHT);
        zoomContributions.add(ZoomManager.FIT_WIDTH);
        zoomManager.setZoomLevelContributions(zoomContributions);
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getAdapter(Class type) {
        if (type == IContentOutlinePage.class) return new OutlinePage();
        if (type == ZoomManager.class) return ((ScalableRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();
        return super.getAdapter(type);
    }

    @Override
    public void dispose() {
        if (cimClient != null) {
            cimClient.unsubscribeFromIndications();
            cimClient.disconnect();
        }
        super.dispose();
    }
}
