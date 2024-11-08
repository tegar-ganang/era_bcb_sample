package net.sf.vorg.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;
import es.ftgroup.gef.model.IGEFModel;
import es.ftgroup.gef.model.IWireModel;
import es.ftgroup.ui.figures.IFigureFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import net.sf.gef.core.editors.BaseGraphicalEditor;
import net.sf.gef.core.editors.LocalEmptyEditorSite;
import net.sf.gef.core.editparts.AbstractEditPartFactory;
import net.sf.gef.core.editparts.AbstractGenericEditPart;
import net.sf.vorg.app.Activator;

public class GlobalMapView extends ViewPart implements PropertyChangeListener {

    public static final String ID = "net.sf.vorg.views.GlobalMapView.id";

    /**
	 * The view cannot be an editor at the same time, so delegate all editor actions to this editor that is
	 * created during the <code>createPartControl</code> creation phase.
	 */
    LocalGraphicalDetailedEditor detailEditor = null;

    /** This is the root of the editor's model. */
    private final MapDataStore editorContainer = null;

    public GlobalMapView() {
        Activator.addReference(GlobalMapView.ID, this);
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
        detailEditor = new LocalGraphicalDetailedEditor(parent, this);
    }

    @Override
    public void init(final IViewSite site) throws PartInitException {
        super.init(site);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        final String prop = evt.getPropertyName();
    }

    public MapDataStore getContainer() {
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

class MapDataStore {

    private static Logger logger = Logger.getLogger("net.sf.vorg");

    private String coastLineDataFile = "E:\\Docs\\ldediego\\Workstage\\ProjectsSourceforge\\VORGAutopilot\\net.sf.vorg.map.perspective\\mapdata\\coast\\30240.dat";

    private final Vector coastLines = new Vector();

    public void readCoastLineData(String coastLineFile) {
        if (null != coastLineFile) coastLineDataFile = coastLineFile;
        try {
            FileSequenceProcessor processor = new FileSequenceProcessor(coastLineDataFile);
            processor.process(new CoastLineProcessor());
        } catch (final FileNotFoundException fnfe) {
            logger.severe("Coast Data lines file does not exist. [" + fnfe.getLocalizedMessage() + "]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class FileSequenceProcessor {

    private static Logger logger = Logger.getLogger("net.sf.vorg");

    private String currentFilePath = null;

    private final BufferedReader dataInput;

    private ILineProcessor processor;

    private String dataLine;

    public FileSequenceProcessor(String filePath) throws IOException {
        if (null == filePath) throw new FileNotFoundException("The file path received is not valid. Cannot open this file [" + filePath + "]");
        currentFilePath = filePath;
        dataInput = new BufferedReader(new FileReader(currentFilePath));
        dataLine = dataInput.readLine();
    }

    public void setProcessor(ILineProcessor newProcessor) {
        processor = newProcessor;
    }

    public void process(ILineProcessor newProcessor) throws IOException {
        if (null != newProcessor) setProcessor(newProcessor);
        if (null == processor) throw new IOException("No line processor set to process the file.");
        while (null != dataLine) {
            processor.process(dataLine);
            dataLine = dataInput.readLine();
        }
    }
}

class CoastLineProcessor implements ILineProcessor {

    public void process(String dataLine) {
    }
}

interface ILineProcessor {

    void process(String dataLine);
}

class LocalGraphicalDetailedEditor extends BaseGraphicalEditor {

    private static final String ID = "net.sf.vorg.editors.LocalGraphicalDetailedEditor.id";

    private GlobalMapView detailedView;

    public LocalGraphicalDetailedEditor(Composite parent, GlobalMapView detailedView) {
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

    public MapDataStore getContents() {
        if (null != detailedView) return detailedView.getContainer(); else return new MapDataStore();
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
        viewer.setEditPartFactory(new MapEditPartFactory(new MapFigureFactory()));
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }
}

class MapEditPartFactory extends AbstractEditPartFactory {

    public MapEditPartFactory(IFigureFactory factory) {
        super(factory);
    }

    @Override
    protected AbstractGenericEditPart getPartForElement(Object modelElement) {
        return null;
    }
}

class MapFigureFactory implements IFigureFactory {

    public PolylineConnection createConnection(IWireModel newWire) {
        return null;
    }

    public Figure createFigure(EditPart part, IGEFModel unit) {
        return null;
    }

    public Figure createFigure(EditPart part, IGEFModel unit, String subType) {
        return null;
    }
}
