package org.xaware.ide.xadev.processview;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ScalableFigure;
import org.eclipse.draw2d.Viewport;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.internal.editors.text.JavaFileEditorInput;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xaware.ide.xadev.processview.controller.ProcessViewControllerFactory;
import org.xaware.ide.xadev.processview.model.BizViewModelBuilder;
import org.xaware.ide.xadev.processview.model.ProcessViewModel;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * A graphical editor for process view.
 */
public class ProcessViewEditor extends GraphicalEditor {

    /** Root of the process view editor model */
    private ProcessViewModel rootModel;

    /** Free form to place the components for process view */
    private ScalableFreeformRootEditPart rootEditPart;

    /** Graphical viewer to display components */
    private GraphicalViewer viewer;

    /** XAware Logger */
    public static XAwareLogger lf = XAwareLogger.getXAwareLogger("org.xaware.ide.xadev.processview.ProcessViewEditor");

    /** Zoom manager */
    ZoomManager zoomManager;

    /** Create a new ProcessViewEditor instance. */
    public ProcessViewEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    /** Create zoom manager */
    public void createZoomManager() {
        final double[] zoomLevels = { .1, .15, .20, .25, .30, .35, .4, .45, .5, .55, .6, .65, .7, .75, .8, .85, .9, .95, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 2.75, 3, 3.25, 3.5, 3.75, 4 };
        zoomManager = new ZoomManager((ScalableFigure) null, (Viewport) null);
        if (this.getViewer() != null) {
            zoomManager = ((ScalableFreeformRootEditPart) this.getViewer().getRootEditPart()).getZoomManager();
        }
        zoomManager.setZoomLevels(zoomLevels);
    }

    /**
     * Get zoom manager for the graphical editor
     * 
     * @return Zoom manager
     */
    public ZoomManager getZoomManager() {
        if (zoomManager == null) {
            createZoomManager();
        }
        return zoomManager;
    }

    /**
     * Configure the graphical viewer.
     */
    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        setViewer(getGraphicalViewer());
        getViewer().setEditPartFactory(new ProcessViewControllerFactory());
        rootEditPart = new ScalableFreeformRootEditPart();
        getViewer().setRootEditPart(rootEditPart);
        getViewer().setKeyHandler(new GraphicalViewerKeyHandler(getViewer()));
    }

    /**
     * Get root of the process view model.
     * 
     * @return Root of the process view model
     */
    ProcessViewModel getModel() {
        return rootModel;
    }

    /**
     * Set up the process view editor's inital content
     */
    @Override
    protected void initializeGraphicalViewer() {
        final GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getModel());
    }

    /**
     * The editor framework calls this method to set the input for the editor page
     * 
     * @param input
     *            The input for the editor page
     */
    @Override
    protected void setInput(final IEditorInput input) {
        super.setInput(input);
        InputStream stream = null;
        Document result = null;
        try {
            if (input instanceof IFileEditorInput) {
                final IFile file = ((IFileEditorInput) input).getFile();
                stream = file.getContents();
                final SAXBuilder builder = new SAXBuilder();
                result = builder.build(stream);
            } else {
                final File file = ((JavaFileEditorInput) input).getPath().toFile();
                final SAXBuilder builder = new SAXBuilder();
                result = builder.build(file);
            }
            lf.finest("Create process view model.", "ProcessViewEditor", "setInput");
            rootModel = createInitialModel(result.getRootElement());
        } catch (final CoreException e) {
            lf.severe("Could not load activity diagram model. ", "ProcessViewEditor", "setInput");
            lf.printStackTrace(e);
            throw new RuntimeException("Could not load activity diagram model. Please see log for details.");
        } catch (final JDOMException e) {
            lf.severe("Could not load activity diagram model. ", "ProcessViewEditor", "setInput");
            lf.printStackTrace(e);
            throw new RuntimeException("Could not load activity diagram model. Please see log for details.");
        } catch (final IOException e) {
            lf.severe("Could not load activity diagram model. ", "ProcessViewEditor", "setInput");
            lf.printStackTrace(e);
            throw new RuntimeException("Could not load activity diagram model. Please see log for details.");
        }
    }

    /**
     * Save the process view diagram in binary form.
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {
    }

    /**
     * Save the process view diagram in binary form
     */
    @Override
    public void doSaveAs() {
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Reset the process view model with the given model.
     * 
     * @param model
     *            Root model for process view
     */
    public void resetRootModel(final ProcessViewModel model) {
        lf.finest("Refresh process view model.", "ProcessViewEditor", "setInput");
        rootModel.removeAllChildren();
        rootModel = model;
        final GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(rootModel);
    }

    /**
     * Creates initial process view diagram model based on the BizDoc file
     * 
     * @return Model of the of the process view
     */
    public static ProcessViewModel createInitialModel(final Element rootElement) {
        return BizViewModelBuilder.createInitialModel(rootElement);
    }

    /**
     * Set the graphical view for the editor page.
     * 
     * @param viewer
     *            Graphical view for the editor page
     */
    public void setViewer(final GraphicalViewer viewer) {
        this.viewer = viewer;
    }

    /**
     * Get the graphical viwer for the editor page.
     * 
     * @return Graphical viwer for the editor page.
     */
    public GraphicalViewer getViewer() {
        return viewer;
    }
}
