package de.beas.explicanto.client.rcp.editor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.gef.ui.views.palette.PalettePage;
import org.eclipse.gef.ui.views.palette.PaletteViewerPage;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.PropertySheet;
import de.bea.services.vidya.client.datasource.VidUtil;
import de.bea.services.vidya.client.datasource.VidyaDataTree;
import de.bea.services.vidya.client.datastructures.CCourse;
import de.bea.services.vidya.client.datastructures.CPage;
import de.bea.services.vidya.client.datastructures.CStatusNode;
import de.bea.services.vidya.client.datastructures.CUnit;
import de.bea.services.vidya.client.datastructures.CUnitItem;
import de.bea.services.vidya.client.datastructures.TreeNode;
import de.beas.explicanto.client.ExplicantoClientPlugin;
import de.beas.explicanto.client.I18N;
import de.beas.explicanto.client.model.CloneFactory;
import de.beas.explicanto.client.model.Diagram;
import de.beas.explicanto.client.model.Document;
import de.beas.explicanto.client.model.Node;
import de.beas.explicanto.client.model.ParentNode;
import de.beas.explicanto.client.rcp.dialogs.SingleLineInputDialog;
import de.beas.explicanto.client.rcp.editor.actions.CopyAction;
import de.beas.explicanto.client.rcp.editor.actions.CopyIntoAction;
import de.beas.explicanto.client.rcp.editor.actions.CutAction;
import de.beas.explicanto.client.rcp.editor.actions.ElementDeleteAction;
import de.beas.explicanto.client.rcp.editor.actions.ExpandAction;
import de.beas.explicanto.client.rcp.editor.actions.PasteAction;
import de.beas.explicanto.client.rcp.editor.palette.CsdePaletteFactory;
import de.beas.explicanto.client.rcp.editor.palette.CsdePaletteViewerProvider;
import de.beas.explicanto.client.rcp.editor.palette.PaletteBuilder;
import de.beas.explicanto.client.rcp.editor.palette.TooltipPaletteViewer;
import de.beas.explicanto.client.rcp.editor.parts.CsdeEditPartFactory;
import de.beas.explicanto.client.rcp.editor.parts.CsdeTreeEditPartFactory;
import de.beas.explicanto.client.rcp.editor.parts.NodeEditPart;
import de.beas.explicanto.client.rcp.editor.parts.NodeTreeEditPart;

/**
 * CsdeEditor Editor class, initializes and maintains the editor, builds the
 * views, the editparts, acts as a link between most GEF components in this
 * project. Creates the palette, responds to requests to save the document.
 * 
 * @author Lucian Brancovean
 * @version 1.0
 * 
 */
public class CsdeEditor extends GraphicalEditorWithFlyoutPalette implements IRunnableWithProgress {

    private static final Logger log = Logger.getLogger(CsdeEditor.class);

    private static final int UNDO_LEVELS = 10;

    /** reference to the palette root */
    protected PaletteRoot palette;

    /** reference to the document open in this editor */
    protected Document document;

    /** true if the open document has been assigned a name */
    private boolean hasName = false;

    private Point clickPoint = null;

    /** id for reference in the plugin descriptor */
    public static final String ID_EDITOR = "de.beas.explicanto.client.model.rcp.editor.CsdeEditor";

    /**
	 * creates EditDomain, sets undo level
	 */
    public CsdeEditor() {
        super();
        DefaultEditDomain defaultEditDomain = new DefaultEditDomain(this);
        defaultEditDomain.getCommandStack().setUndoLimit(UNDO_LEVELS);
        setEditDomain(defaultEditDomain);
        this.addPropertyListener(new IPropertyListener() {

            public void propertyChanged(Object source, int propId) {
                log.debug("propertyChanged: " + source + ", " + propId);
            }
        });
        hasName = false;
    }

    /**
	 * Creates and registers the action objects, for operations such as
	 * copy/paste, delete, show content, etc.
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#createActions()
	 */
    protected void createActions() {
        ActionRegistry registry = getActionRegistry();
        IAction action;
        action = new CutAction(this);
        registry.registerAction(action);
        action = new CopyAction(this);
        registry.registerAction(action);
        action = new PasteAction(this);
        registry.registerAction(action);
        action = new ExpandAction(this);
        registry.registerAction(action);
        action = new CopyIntoAction(this);
        registry.registerAction(action);
        action = new ElementDeleteAction(this);
        registry.registerAction(action);
        getSelectionActions().add(action.getId());
    }

    /**
	 * It set's the pallete state to STATE_PINNED_OPEN before is created by
	 * GraphicalEditorWithFlyoutPalette.createControl
	 */
    public void createPartControl(Composite parent) {
        getPalettePreferences().setPaletteState(4);
        super.createPartControl(parent);
    }

    /**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithPalette#getPaletteRoot()
	 */
    protected PaletteRoot getPaletteRoot() {
        if (palette == null) palette = CsdePaletteFactory.createPalette();
        return palette;
    }

    private SelectionSynchronizer selSync;

    protected SelectionSynchronizer getSelectionSynchronizer() {
        if (selSync == null) selSync = new SelectionSynchronizer() {

            protected EditPart convert(EditPartViewer viewer, EditPart part) {
                log.debug("selSync convert " + viewer + " " + part);
                EditPart ep;
                if (viewer instanceof TreeViewer) {
                    TreeViewer tViewer = (TreeViewer) viewer;
                    ep = (EditPart) tViewer.getEditPartRegistry().get(part.getModel());
                } else ep = super.convert(viewer, part);
                log.debug("selSync converted " + ep);
                return ep;
            }

            public void selectionChanged(SelectionChangedEvent event) {
                log.debug("selChanged");
                super.selectionChanged(event);
            }
        };
        return selSync;
    }

    /**
	 * Initializes the GrpahicalViewer, obtains the document from the edit part,
	 * sets the label of this editor, calls the palette creation process, sets
	 * the root model object (the Diagram), form which gef automatically builds
	 * the editparts. This method is automatically called at the appropriate
	 * time by the framework.
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#initializeGraphicalViewer()
	 */
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        EditPartViewer viewer = getGraphicalViewer();
        Object objDoc = getEditorInput().getAdapter(Document.class);
        if (objDoc != null) {
            document = (Document) objDoc;
        }
        PaletteBuilder.populatePalette(document.getTemplate(), getPaletteRoot());
        viewer.setContents(getDiagram());
        viewer.addDropTargetListener(createTransferDropTargetListener(getGraphicalViewer()));
        paletteOperator = new PaletteOperator(csdePaletteViewerProvider.getViewer(), PaletteBuilder.getDrawers());
        viewer.addSelectionChangedListener(paletteOperator);
        log.debug("Palette visible: " + getPaletteRoot().isVisible());
        getGraphicalControl().addMouseListener(new MouseAdapter() {

            public void mouseUp(MouseEvent e) {
                if (clickPoint == null) clickPoint = new Point(e.x, e.y); else {
                    clickPoint.x = e.x;
                    clickPoint.y = e.y;
                }
            }
        });
    }

    protected void changeName() {
        IStructuredSelection selection = (IStructuredSelection) getGraphicalViewer().getSelection();
        Object objSel = selection.getFirstElement();
        TreeNode node;
        boolean editable = true;
        if (!(objSel instanceof NodeEditPart)) return;
        Node csdeNode = ((NodeEditPart) objSel).getNode();
        node = csdeNode.getTemporaryTreeNode();
        if (node instanceof CUnitItem) return;
        if (node instanceof CPage) editable = csdeNode.getParent().getParent().isLocked(); else if (!csdeNode.isLocked()) editable = false;
        if (node instanceof CUnitItem) editable = false;
        String nodeName = VidUtil.getNodeName(node);
        String prefix = null;
        if (node instanceof CCourse) {
            prefix = nodeName.substring(nodeName.lastIndexOf("."));
            nodeName = nodeName.substring(0, nodeName.lastIndexOf("."));
        }
        SingleLineInputDialog dialog = new SingleLineInputDialog(getGraphicalControl().getShell(), I18N.translate("csdeEditor.nameChange.title"), I18N.translate("csdeEditor.nameChange.msg"), nodeName, null, editable);
        if (dialog.open() != Dialog.OK) return;
        VidUtil.setNodeName(node, dialog.getValue());
        ((NodeEditPart) objSel).getNode().setProperty(Node.TITLE_PROP, dialog.getValue());
        ((NodeEditPart) objSel).getNode().setProperty(Node.LABEL_PROP, dialog.getValue());
        if (prefix != null) ((NodeEditPart) objSel).getNode().setProperty(Node.EDITOR_TITLE, dialog.getValue() + prefix);
        IEditorPart editor = getSite().getPage().getActiveEditor();
        IViewPart view = getSite().getPage().findView(IPageLayout.ID_PROP_SHEET);
        if (view != null) {
            PropertySheet propertyView = (PropertySheet) view;
            propertyView.selectionChanged(editor, editor.getSite().getSelectionProvider().getSelection());
        }
    }

    public Point getClickPoint() {
        return clickPoint;
    }

    /**
	 * Reconstructs the editor with the new document This is used when a draft
	 * is shitched from read-only to read-write mode.
	 * 
	 * @param newDraft
	 * @return true if successfull, false otherways
	 */
    public boolean reopenDraft(Document newDraft) {
        document = newDraft;
        PaletteBuilder.populatePalette(document.getTemplate(), getPaletteRoot());
        EditPartViewer viewer = getGraphicalViewer();
        viewer.setContents(getDiagram());
        viewer.addDropTargetListener(createTransferDropTargetListener(getGraphicalViewer()));
        return true;
    }

    /**
	 * Creates the root EditPart, sets the factory that creates editparts for
	 * model objects, sets the context menu for the editor. This method is
	 * automatically called at the appropriate time by the framework.
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
	 */
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        ScalableFreeformRootEditPart rootPart = new ScalableFreeformRootEditPart();
        viewer.setEditPartFactory(new CsdeEditPartFactory());
        rootPart.getZoomManager().setZoomLevels(new double[] { .1, .2, .3, .4, .5, .6, .7, .8, .9, 1.0 });
        List zoomLevels = new ArrayList(3);
        zoomLevels.add(ZoomManager.FIT_ALL);
        zoomLevels.add(ZoomManager.FIT_WIDTH);
        zoomLevels.add(ZoomManager.FIT_HEIGHT);
        rootPart.getZoomManager().setZoomLevelContributions(zoomLevels);
        IAction zoomIn = new ZoomInAction(rootPart.getZoomManager());
        IAction zoomOut = new ZoomOutAction(rootPart.getZoomManager());
        getActionRegistry().registerAction(zoomIn);
        getActionRegistry().registerAction(zoomOut);
        getSite().getKeyBindingService().registerAction(zoomIn);
        getSite().getKeyBindingService().registerAction(zoomOut);
        viewer.setRootEditPart(rootPart);
        ContextMenuProvider cmProvider = new CsdeContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(cmProvider);
        viewer.setKeyHandler(getCommonKeyHandler());
        getSite().registerContextMenu(cmProvider, viewer);
    }

    /**
	 * Create a transfer drop target listener. When using a
	 * CombinedTemplateCreationEntry tool in the palette, this will enable model
	 * element creation by dragging from the palette.
	 * 
	 * @see #createPaletteViewerProvider()
	 */
    private TransferDropTargetListener createTransferDropTargetListener(EditPartViewer viewer) {
        return new TemplateTransferDropTargetListener(viewer) {

            protected CreationFactory getFactory(Object template) {
                if (template instanceof Node) try {
                    return new CloneFactory((Node) template);
                } catch (CloneNotSupportedException exception) {
                    throw new RuntimeException(exception);
                } else if (template instanceof CreationFactory) return (CreationFactory) template; else return new SimpleFactory((Class) template);
            }
        };
    }

    /**
	 * @return the Diagram represented in this editor
	 */
    public Diagram getDiagram() {
        return document.getDiagram();
    }

    /**
	 * @return the server ID of the template of the model represented in this
	 *         editor
	 */
    public long getTemplateServerID() {
        return document.getTemplate().getServerID();
    }

    /**
	 * @return the server ID of the model represented in this editor
	 */
    public long getDraftID() {
        return document.getModelID();
    }

    /**
	 * @return the server ID of the model represented in this editor
	 */
    public void setDraftID(long id) {
        document.setModelID(id);
    }

    /**
	 * @return the outline page for the outline view.
	 */
    public Object getAdapter(Class type) {
        if (type == IContentOutlinePage.class) return new CsdeOutlinePage(new TreeViewer()); else if (type == ZoomManager.class) return getGraphicalViewer().getProperty(ZoomManager.class.toString()); else if (type == TreeEditor.class) return new TreeEditor(new TreeViewer()); else if (type == PalettePage.class) {
            PaletteViewerPage paletteViewerPage = new PaletteViewerPage(getPaletteViewerProvider());
            return paletteViewerPage;
        } else return super.getAdapter(type);
    }

    /**
	 * Obtains a XML representation of the model being shown.
	 * 
	 * @return a byte array contaning the XML
	 */
    public byte[] getXMLDiagram() {
        try {
            return new byte[0];
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
	 * Creates a image with the diagram and returns the image data. This is done
	 * by instantiating a new shell, and opening a new viewer in it, and
	 * rebuilding the whole editpart hierarchy in it.
	 * 
	 * @return the <code>ImageData</code> of the image
	 */
    public ImageData getJPEGDiagram() {
        Shell shell = new Shell();
        GraphicalViewer viewer = new ScrollingGraphicalViewer();
        viewer.createControl(shell);
        viewer.setEditDomain(new DefaultEditDomain(null));
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.setEditPartFactory(new CsdeEditPartFactory());
        viewer.setContents(getDiagram());
        viewer.flush();
        LayerManager lm = (LayerManager) viewer.getEditPartRegistry().get(LayerManager.ID);
        IFigure fig = lm.getLayer(LayerConstants.PRINTABLE_LAYERS);
        Dimension d = fig.getSize();
        Image image = new Image(null, d.width, d.height);
        GC tmpGC = new GC(image);
        SWTGraphics graphics = new SWTGraphics(tmpGC);
        fig.paint(graphics);
        shell.dispose();
        return image.getImageData();
    }

    /**
	 * Marks the document as saved.
	 * 
	 * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
    public void doSave(IProgressMonitor monitor) {
        List lockedNodes = getDiagram().getLockedNodes();
        monitor.beginTask("", lockedNodes.size() + 2);
        monitor.worked(1);
        Node node;
        TreeNode tempNode = null;
        TreeNode oldNode;
        int i = 2;
        try {
            while (lockedNodes.size() > 0) {
                node = (Node) lockedNodes.get(0);
                tempNode = node.getTemporaryTreeNode();
                oldNode = node.getTreeNode();
                monitor.setTaskName(I18N.translate("editors.messages.saving") + ": " + VidUtil.getNodeName(tempNode));
                saveNode(tempNode, oldNode, node);
                ++i;
                monitor.worked(i);
                lockedNodes.remove(0);
                node.setLocked(false);
            }
        } catch (Exception e) {
            ExplicantoClientPlugin.handleException(e, tempNode);
        }
        monitor.done();
        getEditDomain().getCommandStack().markSaveLocation();
        firePropertyChange(PROP_DIRTY);
    }

    public boolean unlockAll() {
        ProgressMonitorDialog dlg = new ProgressMonitorDialog(getSite().getShell());
        dlg.setCancelable(false);
        dlg.setBlockOnOpen(false);
        dlg.setOpenOnRun(true);
        try {
            dlg.run(false, false, this);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected void saveNode(TreeNode tempNode, TreeNode selectedTreeNode, Node node) throws Exception {
        if (!(selectedTreeNode instanceof CUnit)) {
            VidyaDataTree.getDefault().storeUnlockObject((TreeNode) tempNode);
        } else VidyaDataTree.getDefault().storeUnlockObject(tempNode);
        VidUtil.transferAttributes(tempNode, selectedTreeNode);
        if (selectedTreeNode instanceof CStatusNode) {
            selectedTreeNode.propagateStatusDown(((CStatusNode) selectedTreeNode).getStatus());
        }
        node.setTemporaryTreeNode(selectedTreeNode);
        node.setProperty(Node.LABEL_PROP, VidUtil.getNodeName(selectedTreeNode));
        if (selectedTreeNode instanceof CUnit) {
            setTemporaryNodes((ParentNode) node, selectedTreeNode);
        }
    }

    protected void unlockNode(TreeNode tempNode, TreeNode selectedTreeNode, Node node) throws Exception {
        VidyaDataTree.getDefault().unlockObject(selectedTreeNode);
        node.setTreeNode(selectedTreeNode);
        node.setProperty(Node.TITLE_PROP, VidUtil.getNodeName(selectedTreeNode));
        node.setProperty(Node.LABEL_PROP, VidUtil.getNodeName(selectedTreeNode));
        if (selectedTreeNode instanceof CUnit) {
            setTemporaryNodes((ParentNode) node, selectedTreeNode);
        }
    }

    protected void setTemporaryNodes(ParentNode csdeNode, TreeNode treeNode) {
        if (csdeNode.getVisibleChildren().size() == 0) return;
        List csdeChildren = csdeNode.getVisibleChildren();
        List treeChildren = treeNode.getChildren();
        Node cNode;
        TreeNode tNode;
        for (int i = 0; i < csdeChildren.size(); i++) {
            cNode = (Node) csdeChildren.get(i);
            if (i < treeChildren.size()) {
                tNode = (TreeNode) treeChildren.get(i);
                cNode.setTemporaryTreeNode(tNode);
                if (cNode instanceof ParentNode) setTemporaryNodes((ParentNode) cNode, tNode);
            }
        }
    }

    /**
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
    public void doSaveAs() {
    }

    /**
	 * Decides whether the framework should indicate this editor has unsaved
	 * changes.
	 * 
	 * @see org.eclipse.ui.ISaveablePart#isDirty()
	 */
    public boolean isDirty() {
        return getDiagram().getLockedNodes().size() > 0;
    }

    /**
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
	 * Redirects to the palette preferences creation method.
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#getPalettePreferences()
	 */
    protected FlyoutPreferences getPalettePreferences() {
        return CsdePaletteFactory.createPalettePreferences();
    }

    public boolean hasName() {
        return hasName;
    }

    public void setHasName() {
        hasName = true;
    }

    public void setHasName(String name) {
        setPartName(name);
        document.setName(name);
        hasName = true;
    }

    /**
	 * Creates and returns the palette viewer provider for this editdomain.
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#createPaletteViewerProvider()
	 */
    protected PaletteViewerProvider createPaletteViewerProvider() {
        csdePaletteViewerProvider = new CsdePaletteViewerProvider(getEditDomain(), this);
        return csdePaletteViewerProvider;
    }

    /**
	 * used to receive and respond to keyboard events, such as the delete key to
	 * launch the delete action.
	 */
    private KeyHandler sharedKeyHandler;

    private CsdePaletteViewerProvider csdePaletteViewerProvider;

    private PaletteOperator paletteOperator;

    /**
	 * Returns the KeyHandler with common bindings for both the Outline and
	 * Graphical Views. For example, delete is a common action.
	 */
    protected KeyHandler getCommonKeyHandler() {
        if (sharedKeyHandler == null) {
            sharedKeyHandler = new KeyHandler();
            sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
        }
        return sharedKeyHandler;
    }

    /**
	 * Creates an outline pagebook for this editor.
	 */
    public class CsdeOutlinePage extends ContentOutlinePage {

        /**
		 * Create a new outline page for the shapes editor.
		 * 
		 * @param viewer
		 *            a viewer (TreeViewer instance) used for this outline page
		 * @throws IllegalArgumentException
		 *             if editor is null
		 */
        public CsdeOutlinePage(EditPartViewer viewer) {
            super(viewer);
        }

        /**
		 * Creates and configures the control for this outline page. this is
		 * similar to the editor configureGraphicalViewer method.
		 * 
		 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
        public void createControl(Composite parent) {
            getViewer().createControl(parent);
            getViewer().setEditDomain(getEditDomain());
            getViewer().setEditPartFactory(new CsdeTreeEditPartFactory());
            ContextMenuProvider cmProvider = new CsdeContextMenuProvider(getViewer(), getActionRegistry());
            getViewer().setContextMenu(cmProvider);
            getViewer().setKeyHandler(getCommonKeyHandler());
            getSite().registerContextMenu("de.beas.explicanto.capture.designeeditor.rcp.editors.contextmenu", cmProvider, getSite().getSelectionProvider());
            getViewer().addDropTargetListener(createTransferDropTargetListener(getViewer()));
            getSelectionSynchronizer().addViewer(getViewer());
            getViewer().setContents(getDiagram());
        }

        /**
		 * @see org.eclipse.ui.part.IPage#dispose()
		 */
        public void dispose() {
            getSelectionSynchronizer().removeViewer(getViewer());
            super.dispose();
        }

        /**
		 * @see org.eclipse.ui.part.IPage#getControl()
		 */
        public Control getControl() {
            return getViewer().getControl();
        }

        /**
		 * Registers actions from this view, so that they are usable from the
		 * palette view as well as from the main editor.
		 * 
		 * @see org.eclipse.ui.part.IPageBookViewPage#init(org.eclipse.ui.part.IPageSite)
		 */
        public void init(IPageSite pageSite) {
            super.init(pageSite);
            ActionRegistry registry = getActionRegistry();
            IActionBars bars = pageSite.getActionBars();
            String id;
            id = ActionFactory.UNDO.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = ActionFactory.REDO.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = ActionFactory.DELETE.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = ActionFactory.CUT.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = ActionFactory.COPY.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = ActionFactory.PASTE.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
        }
    }

    public class TreeEditor extends EditorPart {

        private EditPartViewer viewer;

        private IEditorSite site;

        public TreeEditor(EditPartViewer viewer) {
            this.viewer = viewer;
        }

        public void doSave(IProgressMonitor monitor) {
        }

        public void doSaveAs() {
        }

        public void init(IEditorSite site, IEditorInput input) throws PartInitException {
            this.site = site;
            this.setInput(input);
            setSite(site);
        }

        public boolean isDirty() {
            return false;
        }

        public boolean isSaveAsAllowed() {
            return false;
        }

        public void createPartControl(Composite parent) {
            viewer.createControl(parent);
            viewer.setEditDomain(getEditDomain());
            viewer.setEditPartFactory(new CsdeTreeEditPartFactory());
            ContextMenuProvider cmProvider = new CsdeContextMenuProvider(viewer, getActionRegistry());
            viewer.setContextMenu(cmProvider);
            viewer.setKeyHandler(getCommonKeyHandler());
            site.registerContextMenu("de.beas.explicanto.capture.designeeditor.rcp.editors.contextmenu", cmProvider, site.getSelectionProvider());
            viewer.addDropTargetListener(createTransferDropTargetListener(viewer));
            getSelectionSynchronizer().addViewer(viewer);
            viewer.setContents(getDiagram());
            viewer.addSelectionChangedListener(paletteOperator);
            getSite().setSelectionProvider(viewer);
            viewer.getControl().addMouseListener(new MouseAdapter() {

                public void mouseDoubleClick(MouseEvent e) {
                    IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
                    changeName();
                    if (sel.getFirstElement() instanceof NodeTreeEditPart) ((NodeTreeEditPart) sel.getFirstElement()).refresh();
                }
            });
        }

        public void setFocus() {
        }
    }

    public TooltipPaletteViewer getPaletteViewer() {
        return csdePaletteViewerProvider.getViewer();
    }

    public void addPaletteOperator(PaletteOperator newPaletteOperator) {
        if (paletteOperator != null) {
            getGraphicalViewer().removeSelectionChangedListener(paletteOperator);
            paletteOperator = newPaletteOperator;
        }
        GraphicalViewer viewer = getGraphicalViewer();
        if (viewer != null) viewer.addSelectionChangedListener(paletteOperator);
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        super.selectionChanged(part, selection);
        if (getSite().getPage().getActiveEditor() instanceof MainEditor) {
            updateActions(getSelectionActions());
        }
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        List lockedNodes = getDiagram().getLockedNodes();
        monitor.beginTask("", lockedNodes.size() + 2);
        monitor.worked(1);
        Node node;
        TreeNode tempNode = null;
        TreeNode oldNode;
        int i = 2;
        try {
            while (lockedNodes.size() > 0) {
                node = (Node) lockedNodes.get(0);
                tempNode = node.getTemporaryTreeNode();
                oldNode = node.getTreeNode();
                monitor.setTaskName(I18N.translate("editors.messages.unlocking") + ": " + VidUtil.getNodeName(tempNode));
                unlockNode(tempNode, oldNode, node);
                ++i;
                monitor.worked(i);
                lockedNodes.remove(0);
                node.setLocked(false);
            }
        } catch (Exception e) {
            ExplicantoClientPlugin.handleException(e, tempNode);
            throw new InvocationTargetException(e);
        }
        monitor.done();
        getEditDomain().getCommandStack().markSaveLocation();
        firePropertyChange(PROP_DIRTY);
    }
}
