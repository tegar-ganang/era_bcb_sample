package org.cheetahworkflow.designer.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import org.cheetahworkflow.designer.dnd.ChartTemplateTransferDropTargetListener;
import org.cheetahworkflow.designer.model.Chart;
import org.cheetahworkflow.designer.model.Node;
import org.cheetahworkflow.designer.part.PartFactory;
import org.cheetahworkflow.designer.tools.PaletteFactory;
import org.cheetahworkflow.designer.utils.XMLDeserializer;
import org.cheetahworkflow.designer.utils.XMLSerializer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;

public class WorkflowChartEditor extends GraphicalEditorWithPalette {

    private Chart chart = new Chart(this);

    private PaletteRoot paletteRoot;

    private CommandStackListener listener = new CommandStackListener() {

        @Override
        public void commandStackChanged(EventObject event) {
            firePropertyChange(PROP_DIRTY);
        }
    };

    public WorkflowChartEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    public Chart getChart() {
        return chart;
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        if (paletteRoot == null) {
            paletteRoot = PaletteFactory.createPalette();
        }
        return paletteRoot;
    }

    @Override
    public boolean isDirty() {
        return getCommandStack().isDirty();
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        WorkflowRootEditPart root = new WorkflowRootEditPart();
        getGraphicalViewer().setRootEditPart(root);
        List<String> zoomLevels = new ArrayList<String>(3);
        zoomLevels.add(ZoomManager.FIT_ALL);
        zoomLevels.add(ZoomManager.FIT_WIDTH);
        zoomLevels.add(ZoomManager.FIT_HEIGHT);
        root.getZoomManager().setZoomLevelContributions(zoomLevels);
        getGraphicalViewer().setEditPartFactory(new PartFactory());
        getCommandStack().addCommandStackListener(listener);
    }

    @Override
    protected void initializeGraphicalViewer() {
        getGraphicalViewer().addDropTargetListener(new ChartTemplateTransferDropTargetListener(getGraphicalViewer()));
        drawChart();
    }

    @Override
    protected void initializePaletteViewer() {
        super.initializePaletteViewer();
        getPaletteViewer().addDragSourceListener(new TemplateTransferDragSourceListener(getPaletteViewer()));
    }

    @Override
    public Object getAdapter(Class type) {
        if (type == ZoomManager.class) {
            return getGraphicalViewer().getProperty(ZoomManager.class.toString());
        }
        return super.getAdapter(type);
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
    }

    public void drawChart() {
        IFile file = (IFile) getEditorInput().getAdapter(IFile.class);
        InputStream in;
        try {
            in = file.getContents();
            drawChart(in);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    public void drawChart(InputStream in) {
        chart = new Chart(this);
        getGraphicalViewer().setContents(chart);
        try {
            XMLDeserializer deserializer = new XMLDeserializer(in);
            if (deserializer.isValid()) {
                Node.resetNodeID();
                deserializer.addStartNode(chart);
                deserializer.addLogicNodes(chart);
                deserializer.addEndNodes(chart);
                deserializer.addArrows(chart);
                deserializer.addComments(chart);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doSave(final IProgressMonitor monitor) {
        Display display = this.getGraphicalViewer().getControl().getDisplay();
        display.syncExec(new Runnable() {

            @Override
            public void run() {
                String xml = XMLSerializer.serialize(chart);
                try {
                    InputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
                    IFile file = (IFile) getEditorInput().getAdapter(IFile.class);
                    file.setContents(in, false, true, monitor);
                    getCommandStack().markSaveLocation();
                } catch (UnsupportedEncodingException e) {
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void dispose() {
        getCommandStack().removeCommandStackListener(listener);
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        updateActions(getSelectionActions());
    }

    public String serialize() {
        return XMLSerializer.serialize(chart);
    }

    public void deserialize(String xml) {
        InputStream in;
        try {
            in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            drawChart(in);
        } catch (UnsupportedEncodingException e) {
        }
    }

    public org.eclipse.draw2d.geometry.Rectangle getViewportBounds() {
        FigureCanvas canvas = (FigureCanvas) this.getGraphicalViewer().getControl();
        return canvas.getViewport().getBounds();
    }

    public int getHorizontalScroll() {
        FigureCanvas canvas = (FigureCanvas) this.getGraphicalViewer().getControl();
        return canvas.getViewport().getHorizontalRangeModel().getValue();
    }

    public int getVerticalScroll() {
        FigureCanvas canvas = (FigureCanvas) this.getGraphicalViewer().getControl();
        return canvas.getViewport().getVerticalRangeModel().getValue();
    }
}
