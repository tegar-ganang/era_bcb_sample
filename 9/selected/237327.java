package org.wsmostudio.bpmo.ui.editor;

import java.io.*;
import java.util.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.*;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.*;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.omwg.ontology.Ontology;
import org.omwg.ontology.Value;
import org.sbpm.bpmo.*;
import org.sbpm.bpmo.Process;
import org.sbpm.bpmo.factory.Factory;
import org.sbpm.bpmo.io.BpmoParser;
import org.sbpm.bpmo.io.BpmoSerializer;
import org.wsmo.common.Identifier;
import org.wsmo.common.TopEntity;
import org.wsmo.wsml.Parser;
import org.wsmo.wsml.Serializer;
import org.wsmostudio.bpmo.Activator;
import org.wsmostudio.bpmo.ImagePool;
import org.wsmostudio.bpmo.model.*;
import org.wsmostudio.bpmo.model.connectors.GraphConnector;
import org.wsmostudio.bpmo.model.io.BPMOExporter;
import org.wsmostudio.bpmo.model.io.BPMOImporter;
import org.wsmostudio.bpmo.model.io.UIModelValidator;
import org.wsmostudio.bpmo.model.properties.PropertiesView;
import org.wsmostudio.bpmo.ui.actions.*;
import org.wsmostudio.bpmo.ui.editor.dnd.WsmoTransferDropTargetListener;
import org.wsmostudio.bpmo.ui.editor.editpart.GraphicalPartFactory;
import org.wsmostudio.runtime.LogManager;
import org.wsmostudio.runtime.WSMORuntime;

public class BpmoEditor extends GraphicalEditorWithFlyoutPalette {

    public static final String ID = "org.wsmostudio.bpmo.ui.editor";

    private SelectionSynchronizer synchronizer = new SelectionSynchronizer();

    private String outputLocation = null;

    private String fileName = null;

    private String nonBpmoWsmlData = null;

    private Map<Identifier, Map<Identifier, Set<Value>>> nonBpmoAttributes = null;

    private boolean dirty = false;

    public BpmoEditor() {
        setEditDomain(new DefaultEditDomain(this));
        setActionRegistry(BpmoActionRegistry.getRegistry().createEditorRegistry(this));
    }

    public void init(final IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);
        fileName = input.getName();
        if (input instanceof IPathEditorInput) {
            outputLocation = ((IPathEditorInput) input).getPath().toFile().getAbsolutePath();
        } else if (input instanceof IStorageEditorInput) {
            try {
                outputLocation = ((IStorageEditorInput) input).getStorage().getFullPath().toFile().getAbsolutePath();
            } catch (CoreException error) {
                LogManager.logError(error);
            }
        }
        WSMORuntime.forceEarlyInit();
        getPalettePreferences().setPaletteState(FlyoutPaletteComposite.STATE_PINNED_OPEN);
    }

    public void commandStackChanged(EventObject event) {
        firePropertyChange(IEditorPart.PROP_DIRTY);
        super.commandStackChanged(event);
    }

    /**
   * @see org.eclipse.gef.ui.parts.GraphicalEditor#initializeGraphicalViewer()
  **/
    protected void initializeGraphicalViewer() {
        getGraphicalViewer().setContents(loadModel());
        getGraphicalViewer().addDropTargetListener(createTransferDropTargetListener());
        getGraphicalViewer().addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                if (event.getSelection().isEmpty()) {
                    return;
                }
                loadProperties(((StructuredSelection) event.getSelection()).getFirstElement());
            }
        });
    }

    public GraphicalViewer getEditPartViewer() {
        return getGraphicalViewer();
    }

    public void updateView() {
        getGraphicalViewer().getContents().refresh();
    }

    private TransferDropTargetListener createTransferDropTargetListener() {
        return new WsmoTransferDropTargetListener(getGraphicalViewer());
    }

    public void loadProperties(Object selection) {
        IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (activePage == null) {
            return;
        }
        IViewPart view = activePage.findView(PropertiesView.ID);
        if (view == null || false == view instanceof PropertiesView) {
            return;
        }
        if (false == selection instanceof EditPart) {
            ((PropertiesView) view).clearModel();
        } else {
            EditPart ePart = (EditPart) selection;
            if (ePart.getModel() instanceof WorkflowEntityNode) {
                ((PropertiesView) view).setModel(ePart.getModel());
            } else if (ePart.getModel() instanceof GraphConnector) {
                ((PropertiesView) view).setModel(ePart.getModel());
            } else {
                ((PropertiesView) view).clearModel();
            }
        }
    }

    /**
     * @see org.eclipse.ui.IEditorPart#doSave(IProgressMonitor)
     **/
    public void doSave(IProgressMonitor iMonitor) {
        saveModel(new File(outputLocation));
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    public String getPartName() {
        return super.getPartName() + " - " + fileName;
    }

    public void dispose() {
        loadProperties(null);
        super.dispose();
    }

    public BpmoModel getModel() {
        return (BpmoModel) getGraphicalViewer().getContents().getModel();
    }

    private void saveModel(File targetFile) {
        BpmoModel model = (BpmoModel) getGraphicalViewer().getContents().getModel();
        if (model.getNSHolder().getDefaultNamespace() != null) {
            Map<String, Object> newConfig = new HashMap<String, Object>();
            newConfig.put(BpmoSerializer.RESULT_NAMESPACE, model.getNSHolder().getDefaultNamespace());
            Activator.getBpmoFactory().configure(newConfig);
        }
        Map<WorkflowEntityNode, String> errors = new HashMap<WorkflowEntityNode, String>();
        UIModelValidator.validateModelConsistency(model, errors);
        if (errors.size() > 0) {
            WorkflowEntityNode errorNode = errors.keySet().iterator().next();
            Utils.expandPathFromRoot(errorNode);
            EditPart errorEditpart = (EditPart) getEditPartViewer().getEditPartRegistry().get(errorNode);
            if (errorEditpart != null) {
                getEditPartViewer().select(errorEditpart);
            }
            if (false == MessageDialog.openConfirm(getSite().getShell(), "Inconsistent Diagram", "The current diagram might not be represented correctly in BPMO:\n\n " + errors.get(errorNode) + ((errors.size() > 1) ? ("\n\n  (+ " + (errors.size() - 1) + " more error(s))") : "") + "\n\nDo you want to proceed (at your risk) with saving anyway?")) {
                return;
            }
        }
        if (targetFile == null) {
            doSaveAs();
            return;
        }
        BPMOExporter uiExporter = new BPMOExporter();
        Identifiable[] bpmoModel = null;
        try {
            bpmoModel = uiExporter.convertVisualModel(model);
        } catch (Exception ex) {
            MessageDialog.openError(getSite().getShell(), "BPMO Export", "Error in converting diagram to BPMO model:\n" + ex.getMessage());
            LogManager.logError(ex);
            return;
        }
        Map<String, Object> props = new HashMap<String, Object>();
        if (model.getNSHolder().getDefaultNamespace() != null) {
            props.put(BpmoSerializer.RESULT_NAMESPACE, model.getNSHolder().getDefaultNamespace());
        }
        props.put(BpmoSerializer.RESULT_ONTOLOGY_ID, model.getNSHolder().getIdentifier());
        BpmoSerializer bpmoSerializer = Factory.createSerializer(props);
        Ontology instOntology = bpmoSerializer.serialize(bpmoModel);
        Utils.recoverNonBPMOData(instOntology, this.nonBpmoWsmlData, this.nonBpmoAttributes);
        org.wsmostudio.runtime.io.Utils.updateStudioNFP(instOntology);
        model.updateNamespacesTo(instOntology);
        Serializer wsmlSerializar = WSMORuntime.getRuntime().getWsmlSerializer();
        StringBuffer str = new StringBuffer();
        wsmlSerializar.serialize(new TopEntity[] { instOntology }, str);
        try {
            WSMORuntime.getIOManager().saveContent(instOntology, new Path(targetFile.getAbsolutePath()));
            this.getEditDomain().getCommandStack().markSaveLocation();
            this.dirty = false;
        } catch (Exception e) {
            LogManager.logError(e);
        }
        File layoutFile = new File(targetFile.getAbsolutePath() + ".layout");
        try {
            uiExporter.exportLayout(layoutFile, instOntology.getIdentifier());
            IFile iResource = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(layoutFile.getAbsolutePath()));
            if (iResource == null) {
                return;
            }
            iResource.refreshLocal(IResource.DEPTH_ONE, null);
        } catch (Exception ex) {
            MessageDialog.openWarning(getSite().getShell(), "Saving Layout", "Error while saving diagram layout:\n" + ex.getMessage());
            LogManager.logError(ex);
        }
        firePropertyChange(PROP_DIRTY);
    }

    private BpmoModel loadModel() {
        IPath sourcePath = null;
        if (getEditorInput() instanceof IPathEditorInput) {
            sourcePath = ((IPathEditorInput) getEditorInput()).getPath();
        } else if (getEditorInput() instanceof IStorageEditorInput) {
            try {
                sourcePath = ((IStorageEditorInput) getEditorInput()).getStorage().getFullPath();
            } catch (CoreException error) {
                LogManager.logError(error);
            }
        }
        if (sourcePath == null) {
            MessageDialog.openError(getSite().getShell(), "Improper Usage", "The editor can not read the selected source (" + getEditorInput() + ")");
            return null;
        }
        BpmoModel model = null;
        BpmoParser parser = Factory.createParser(null);
        Parser wsmlParser = WSMORuntime.getRuntime().getWsmlParser();
        try {
            TopEntity[] te = wsmlParser.parse(new FileReader(sourcePath.toFile()));
            if (te.length != 1 || false == te[0] instanceof Ontology) {
                throw new Exception("The input wsml content is not a BPMO instance ontology!");
            }
            nonBpmoAttributes = new HashMap<Identifier, Map<Identifier, Set<Value>>>();
            this.nonBpmoWsmlData = Utils.preserveNonBPMOData((Ontology) te[0], nonBpmoAttributes);
            Process[] bpmoData = parser.parse((Ontology) te[0]);
            if (bpmoData.length == 0) {
                throw new Exception("No BPMO input detected or the source is not a BPMO instance ontology!");
            }
            BPMOImporter importer = new BPMOImporter();
            model = importer.importBpmo(bpmoData, sourcePath.toFile().getAbsoluteFile() + ".layout", (Ontology) te[0]);
        } catch (Exception fnfe) {
            MessageDialog.openError(getSite().getShell(), "Error Opening WSML", "Message: " + fnfe.getMessage());
            LogManager.logError(fnfe);
            if (!getSite().getShell().isDisposed() && !getSite().getShell().getDisplay().isDisposed()) {
                getSite().getShell().getDisplay().asyncExec(new Runnable() {

                    public void run() {
                        getSite().getPage().closeEditor(BpmoEditor.this, false);
                    }
                });
            }
        }
        if (model != null) {
            model.setContainingEditor(this);
        }
        return model;
    }

    /**
     * @see org.eclipse.ui.IEditorPart#isDirty()
     **/
    public boolean isDirty() {
        return this.getEditDomain().getCommandStack().isDirty() || this.dirty;
    }

    public void markDirty() {
        this.dirty = true;
        firePropertyChange(PROP_DIRTY);
    }

    /**
   * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
  **/
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        ScalableRootEditPart rootEditPart = new ScalableRootEditPart();
        viewer.setRootEditPart(rootEditPart);
        viewer.setEditPartFactory(new GraphicalPartFactory());
        configureZoomManager(rootEditPart);
        getEditDomain().addViewer(viewer);
        this.synchronizer.addViewer(viewer);
        getSite().setSelectionProvider(viewer);
        ContextMenuProvider cmProvider = new BpmoEditorContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(cmProvider);
        getSite().registerContextMenu(cmProvider, viewer);
        ((FigureCanvas) viewer.getControl()).setScrollBarVisibility(FigureCanvas.ALWAYS);
    }

    @SuppressWarnings("unchecked")
    protected void createActions() {
        super.createActions();
        ActionRegistry registry = getActionRegistry();
        IAction action;
        action = new CopyAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        getEditorSite().getActionBars().setGlobalActionHandler(action.getId(), action);
        action = new PasteAction(this);
        action.setId(ActionFactory.PASTE.getId());
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new DirectEditAction((IWorkbenchPart) this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new ExportImageAction(this);
        action.setImageDescriptor(ImagePool.getImage(ImagePool.EXPORT_IMAGE_ICON));
        action.setDisabledImageDescriptor(ImagePool.getImage(ImagePool.EXPORT_IMAGE_ICON_DISABLED));
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
        action = new LayoutAction(this);
        action.setImageDescriptor(ImagePool.getImage(ImagePool.LAYOUT_ICON));
        action.setDisabledImageDescriptor(ImagePool.getImage(ImagePool.LAYOUT_ICON_DISABLED));
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
    }

    @SuppressWarnings("unchecked")
    public List getSelectionActions() {
        return super.getSelectionActions();
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class type) {
        if (type == IPropertySheetPage.class) {
            PropertySheetPage page = new PropertySheetPage();
            page.setRootEntry(new UndoablePropertySheetEntry(getEditDomain().getCommandStack()));
            return page;
        }
        if (type == IContentOutlinePage.class) {
            return new BpmoContentOutlinePage(getActionRegistry(), BpmoEditor.this);
        }
        if (type == GraphicalViewer.class) {
            return getGraphicalViewer();
        }
        if (type == EditPart.class) {
            return getGraphicalViewer().getRootEditPart();
        }
        if (type == IFigure.class) {
            return ((GraphicalEditPart) getGraphicalViewer().getRootEditPart()).getFigure();
        }
        if (type == ZoomManager.class) {
            return getGraphicalViewer().getProperty(ZoomManager.class.toString());
        }
        if (type == ActionRegistry.class) {
            return getActionRegistry();
        }
        if (type == SelectionSynchronizer.class) {
            return this.synchronizer;
        }
        return super.getAdapter(type);
    }

    private void configureZoomManager(ScalableRootEditPart rootEditPart) {
        ZoomManager manager = rootEditPart.getZoomManager();
        double[] zoomLevels = new double[] { 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 10.0, 20.0 };
        manager.setZoomLevels(zoomLevels);
        ArrayList<String> zoomContributions = new ArrayList<String>();
        zoomContributions.add(ZoomManager.FIT_ALL);
        zoomContributions.add(ZoomManager.FIT_HEIGHT);
        zoomContributions.add(ZoomManager.FIT_WIDTH);
        manager.setZoomLevelContributions(zoomContributions);
        Action zoomIn = new ZoomInAction(manager);
        Action zoomOut = new ZoomOutAction(manager);
        getActionRegistry().registerAction(zoomIn);
        getActionRegistry().registerAction(zoomOut);
    }

    /**
   * Returns the palette root.
   */
    protected PaletteRoot getPaletteRoot() {
        return PaletteFactory.createPalette();
    }
}
