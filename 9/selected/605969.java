package visualbiology.reactionEditor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import myGEF.GraphicalEditorWithPalette;
import myGEF.MyTemplateTransferDropTargetListener;
import myGEF.commands.GefChangeLayoutCommand;
import mySBML.Model;
import mySBML.Reaction;
import mySBML.utilities.DomUtilities;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.SelectionToolEntry;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import visualbiology.reactionEditor.actions.EditLocalParamsAction;
import visualbiology.reactionEditor.actions.EditSpeciesRefAction;
import visualbiology.reactionEditor.actions.EditStoichiometryAction;
import visualbiology.reactionEditor.actions.OpenKineticLawAction;
import visualbiology.reactionEditor.editparts.AppEditPartFactory;
import visualbiology.reactionEditor.model.ElementCreationFactory;
import visualbiology.reactionEditor.model.ReactionDiagram;
import visualbiology.reactionEditor.treeparts.AppTreeEditPartFactory;
import visualbiology.sbmlEditor.SbmlModelEditor;

public class ReactionEditor extends GraphicalEditorWithPalette {

    public static final String ID = "visualbiology.ReactionEditor";

    private ReactionDiagram diagram;

    private Reaction reaction;

    private IPath pathToLayoutFile;

    private SbmlModelEditor sbmlEditorReference;

    private KeyHandler keyHandler;

    private PaletteGroup creationGroup;

    public void refreshPage() {
        diagram.refreshNames();
        refreshPalette();
        if (getEditorInput() != null) {
            setPartName(getEditorInput().getName());
            setTitleToolTip(getEditorInput().getToolTipText());
        }
    }

    protected void setInput(IEditorInput input) {
        super.setInput(input);
        reaction = (Reaction) input.getAdapter(Reaction.class);
        setEditDomain(new DefaultEditDomain(this));
        setPartName(input.getName());
        setTitleToolTip(input.getToolTipText());
        diagram = new ReactionDiagram(reaction);
        loadCoordinates((String) input.getAdapter(String.class));
        IEditorInput originalInput = (IEditorInput) input.getAdapter(FileEditorInput.class);
        IWorkbench wb = PlatformUI.getWorkbench();
        IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
        IWorkbenchPage page = win.getActivePage();
        sbmlEditorReference = (SbmlModelEditor) page.findEditor(originalInput);
    }

    private void loadCoordinates(String path) {
        int index = path.lastIndexOf(".xml");
        path = path.substring(0, index) + ".layout";
        pathToLayoutFile = new Path(path);
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(pathToLayoutFile);
        refreshFile(file);
        if (file.exists()) {
            try {
                InputStream stream = file.getContents();
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document document = db.parse(stream);
                document.getDocumentElement().normalize();
                constructCoordinates(document.getFirstChild());
            } catch (CoreException e) {
                e.printStackTrace();
                constructCoordinates();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                constructCoordinates();
            } catch (SAXException e) {
                e.printStackTrace();
                constructCoordinates();
            } catch (IOException e) {
                e.printStackTrace();
                constructCoordinates();
            }
        } else constructCoordinates();
    }

    private void constructCoordinates() {
        ArrayList<GefChangeLayoutCommand> cmds = diagram.autoArrange();
        for (int i = 0; i < cmds.size(); i++) cmds.get(i).execute();
    }

    private void constructCoordinates(Node rootElement) {
        boolean success = false;
        String reactionId = reaction.getId().toString();
        DomUtilities.removeEmptyTextChildren(rootElement);
        if (rootElement != null && rootElement.getNodeName().equals("sbsiVisual")) {
            for (int i = 0; i < rootElement.getChildNodes().getLength(); i++) {
                Node current = rootElement.getChildNodes().item(i);
                if (!current.getNodeName().equals("reactionDiagram")) continue;
                Node idAttr = current.getAttributes().getNamedItem("reactionID");
                if (idAttr != null && idAttr.getNodeValue().equals(reactionId)) {
                    success = diagram.loadXML(current);
                    break;
                }
            }
        }
        if (!success) constructCoordinates();
    }

    protected PaletteRoot getPaletteRoot() {
        PaletteRoot root = new PaletteRoot();
        PaletteGroup manipGroup = new PaletteGroup("Object Manipulation");
        root.add(manipGroup);
        SelectionToolEntry selectionToolEntry = new SelectionToolEntry();
        manipGroup.add(selectionToolEntry);
        manipGroup.add(new MarqueeToolEntry());
        PaletteSeparator sep2 = new PaletteSeparator();
        root.add(sep2);
        creationGroup = new PaletteGroup("Element Creation");
        root.add(creationGroup);
        root.setDefaultEntry(selectionToolEntry);
        refreshPalette();
        return root;
    }

    private void refreshPalette() {
        Model model = reaction.getModel();
        List<?> contents = creationGroup.getChildren();
        int size = contents.size();
        for (int i = 0; i < size; i++) contents.remove(0);
        for (int j = 0; j < model.getSpecies().size(); j++) if (model.getSpecies().get(j).getSpeciesTypeID().isEmpty()) creationGroup.add(new CombinedTemplateCreationEntry(model.getSpecies().get(j).getId().toString(), model.getSpecies().get(j).getName().toString(), new ElementCreationFactory(model.getSpecies().get(j).getId().toString()), null, null));
        for (int i = 0; i < model.getSpeciesTypes().size(); i++) {
            PaletteDrawer drawer = new PaletteDrawer("");
            drawer.setLabel(model.getSpeciesTypes().get(i).getId().toString());
            for (int j = 0; j < model.getSpecies().size(); j++) if (model.getSpeciesTypes().get(i).getId().equalTo(model.getSpecies().get(j).getSpeciesTypeID())) drawer.add(new CombinedTemplateCreationEntry(model.getSpecies().get(j).getId().toString(), model.getSpecies().get(j).getName().toString(), new ElementCreationFactory(model.getSpecies().get(j).getId().toString()), null, null));
            creationGroup.add(drawer);
        }
    }

    @SuppressWarnings("unchecked")
    public void createActions() {
        super.createActions();
        ActionRegistry registry = getActionRegistry();
        IAction action;
        action = new OpenKineticLawAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new EditLocalParamsAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new EditStoichiometryAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new EditSpeciesRefAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
    }

    protected void configureGraphicalViewer() {
        double[] zoomLevels;
        ArrayList<String> zoomContributions;
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new AppEditPartFactory());
        ScalableFreeformRootEditPart rootEditPart = new ScalableFreeformRootEditPart();
        viewer.setRootEditPart(rootEditPart);
        ZoomManager manager = rootEditPart.getZoomManager();
        getActionRegistry().registerAction(new ZoomInAction(manager));
        getActionRegistry().registerAction(new ZoomOutAction(manager));
        zoomLevels = new double[] { 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 10.0, 20.0 };
        manager.setZoomLevels(zoomLevels);
        zoomContributions = new ArrayList<String>();
        zoomContributions.add(ZoomManager.FIT_ALL);
        zoomContributions.add(ZoomManager.FIT_HEIGHT);
        zoomContributions.add(ZoomManager.FIT_WIDTH);
        manager.setZoomLevelContributions(zoomContributions);
        keyHandler = new KeyHandler();
        keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
        keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0), getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));
        keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0), getActionRegistry().getAction(GEFActionConstants.ZOOM_OUT));
        viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.CONTROL), MouseWheelZoomHandler.SINGLETON);
        viewer.setKeyHandler(keyHandler);
        keyHandler = new KeyHandler();
        keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
        keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0), getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));
        keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0), getActionRegistry().getAction(GEFActionConstants.ZOOM_OUT));
        viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.CONTROL), MouseWheelZoomHandler.SINGLETON);
        viewer.setKeyHandler(keyHandler);
        ContextMenuProvider provider = new AppContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(provider);
    }

    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class type) {
        if (type == ZoomManager.class) return ((ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();
        if (type == IContentOutlinePage.class) {
            return new OutlinePage();
        }
        return super.getAdapter(type);
    }

    protected void initializeGraphicalViewer() {
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(diagram);
        viewer.addDropTargetListener(new MyTemplateTransferDropTargetListener(viewer));
    }

    public void commandStackChanged(EventObject event) {
        super.commandStackChanged(event);
        firePropertyChange(PROP_DIRTY);
        sbmlEditorReference.refreshPage();
    }

    public void doSave(IProgressMonitor monitor) {
        saveLayouts();
        getCommandStack().markSaveLocation();
        firePropertyChange(PROP_DIRTY);
        IEditorInput sourceInput = (IEditorInput) getEditorInput().getAdapter(FileEditorInput.class);
        IWorkbench wb = PlatformUI.getWorkbench();
        IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
        IWorkbenchPage page = win.getActivePage();
        SbmlModelEditor source = (SbmlModelEditor) page.findEditor(sourceInput);
        source.refreshPage();
        source.doSave(null);
    }

    public void dispose() {
        super.dispose();
    }

    private void saveLayouts() {
        IFile eclipseFile = ResourcesPlugin.getWorkspace().getRoot().getFile(pathToLayoutFile);
        String path = eclipseFile.getProject().getLocation().toString();
        path += "/" + eclipseFile.getProjectRelativePath();
        refreshFile(eclipseFile);
        File file = new File(path);
        Node root = null;
        if (file.exists()) {
            try {
                InputStream inStream = eclipseFile.getContents();
                root = modifyXMLRoot(inStream);
            } catch (ParserConfigurationException e) {
                root = createXMLRoot();
                e.printStackTrace();
            } catch (CoreException e) {
                root = createXMLRoot();
                e.printStackTrace();
            } catch (SAXException e) {
                root = createXMLRoot();
                e.printStackTrace();
            } catch (IOException e) {
                root = createXMLRoot();
                e.printStackTrace();
            }
        } else root = createXMLRoot();
        FileOutputStream stream = null;
        try {
            file.createNewFile();
            stream = new FileOutputStream(file);
        } catch (FileNotFoundException e1) {
            return;
        } catch (IOException e) {
            return;
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
        }
        DOMSource source = new DOMSource(root);
        StreamResult result = new StreamResult(stream);
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "5");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
        }
        refreshFile(eclipseFile);
    }

    /**
	 * Given an InputSream that corresponds to an XML document, the method
	 * removes the current reactionDiagram from the "sbsiVisual" root. If any
	 * exception is thrown, the document is not valid and the root has to be
	 * created.
	 * */
    private Node modifyXMLRoot(InputStream inStream) throws CoreException, ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(inStream);
        document.getDocumentElement().normalize();
        Node root = document.getFirstChild();
        String reactionId = reaction.getId().toString();
        DomUtilities.removeEmptyTextChildren(root);
        if (root != null && root.getNodeName().equals("sbsiVisual")) {
            for (int i = 0; i < root.getChildNodes().getLength(); i++) {
                Node current = root.getChildNodes().item(i);
                if (!current.getNodeName().equals("reactionDiagram")) continue;
                Node idAttr = current.getAttributes().getNamedItem("reactionID");
                if (idAttr != null && idAttr.getNodeValue().equals(reactionId)) root.removeChild(current);
            }
        } else throw new SAXException("Not a valid '.layout' file. " + "The root elementmust be a \"sbsiVisual\"");
        root.appendChild(diagram.toXML(root));
        return root;
    }

    private Node createXMLRoot() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser;
            parser = factory.newDocumentBuilder();
            Document document;
            document = parser.newDocument();
            document.setXmlStandalone(true);
            Node root = document.createElement("sbsiVisual");
            document.appendChild(root);
            root.appendChild(diagram.toXML(root));
            return root;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void refreshFile(IFile file) {
        try {
            file.refreshLocal(0, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    protected class OutlinePage extends ContentOutlinePage {

        private SashForm sash;

        private ScrollableThumbnail thumbnail;

        private DisposeListener disposeListener;

        public OutlinePage() {
            super(new TreeViewer());
        }

        public void createControl(Composite parent) {
            sash = new SashForm((org.eclipse.swt.widgets.Composite) parent, SWT.VERTICAL);
            getViewer().createControl(sash);
            getViewer().setEditDomain(getEditDomain());
            getViewer().setEditPartFactory(new AppTreeEditPartFactory());
            getViewer().setContents(diagram);
            getSelectionSynchronizer().addViewer(getViewer());
            Canvas canvas = new Canvas(sash, SWT.BORDER);
            LightweightSystem lws = new LightweightSystem(canvas);
            thumbnail = new ScrollableThumbnail((Viewport) ((ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart()).getFigure());
            thumbnail.setSource(((ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart()).getLayer(LayerConstants.PRINTABLE_LAYERS));
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
            IActionBars bars = getSite().getActionBars();
            ActionRegistry ar = getActionRegistry();
            bars.setGlobalActionHandler(ActionFactory.COPY.getId(), ar.getAction(ActionFactory.COPY.getId()));
            bars.setGlobalActionHandler(ActionFactory.PASTE.getId(), ar.getAction(ActionFactory.PASTE.getId()));
        }

        public void init(IPageSite pageSite) {
            super.init(pageSite);
            IActionBars bars = getSite().getActionBars();
            bars.setGlobalActionHandler(ActionFactory.UNDO.getId(), getActionRegistry().getAction(ActionFactory.UNDO.getId()));
            bars.setGlobalActionHandler(ActionFactory.REDO.getId(), getActionRegistry().getAction(ActionFactory.REDO.getId()));
            bars.setGlobalActionHandler(ActionFactory.DELETE.getId(), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
            bars.updateActionBars();
            getViewer().setKeyHandler(keyHandler);
            ContextMenuProvider provider = new AppContextMenuProvider(getViewer(), getActionRegistry());
            getViewer().setContextMenu(provider);
        }

        public Control getControl() {
            return sash;
        }

        public void dispose() {
            getSelectionSynchronizer().removeViewer(getViewer());
            if (getGraphicalViewer().getControl() != null && !getGraphicalViewer().getControl().isDisposed()) getGraphicalViewer().getControl().removeDisposeListener(disposeListener);
            super.dispose();
        }
    }
}
