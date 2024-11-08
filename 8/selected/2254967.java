package org.plazmaforge.studio.reportdesigner.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.DefaultRangeModel;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.TreeEditPart;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.dnd.TransferDragSourceListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomListener;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.actions.AlignmentAction;
import org.eclipse.gef.ui.actions.MatchHeightAction;
import org.eclipse.gef.ui.actions.MatchWidthAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.plazmaforge.studio.core.editors.EditorInputFactory;
import org.plazmaforge.studio.core.util.StatusInfo;
import org.plazmaforge.studio.modeling.j2d.J2DGraphicalEditorWithFlyoutPalette;
import org.plazmaforge.studio.modeling.j2d.J2DScalableFreeformLayeredPane;
import org.plazmaforge.studio.modeling.j2d.J2DScrollingGraphicalViewer;
import org.plazmaforge.studio.reportdesigner.ReportDesignerConstants;
import org.plazmaforge.studio.reportdesigner.ReportDesignerPlugin;
import org.plazmaforge.studio.reportdesigner.actions.AlignToContainerAction;
import org.plazmaforge.studio.reportdesigner.actions.BandListAction;
import org.plazmaforge.studio.reportdesigner.actions.BorderAction;
import org.plazmaforge.studio.reportdesigner.actions.BorderBottomAction;
import org.plazmaforge.studio.reportdesigner.actions.BorderLeftAction;
import org.plazmaforge.studio.reportdesigner.actions.BorderRightAction;
import org.plazmaforge.studio.reportdesigner.actions.BorderTopAction;
import org.plazmaforge.studio.reportdesigner.actions.ClasspathListAction;
import org.plazmaforge.studio.reportdesigner.actions.CompileAction;
import org.plazmaforge.studio.reportdesigner.actions.CopyAction;
import org.plazmaforge.studio.reportdesigner.actions.CrosstabCellEditAction;
import org.plazmaforge.studio.reportdesigner.actions.CrosstabEditAction;
import org.plazmaforge.studio.reportdesigner.actions.CutAction;
import org.plazmaforge.studio.reportdesigner.actions.DataSourceEditAction;
import org.plazmaforge.studio.reportdesigner.actions.DatasetListAction;
import org.plazmaforge.studio.reportdesigner.actions.EditExpressionAction;
import org.plazmaforge.studio.reportdesigner.actions.ElementEditAction;
import org.plazmaforge.studio.reportdesigner.actions.ExportAction;
import org.plazmaforge.studio.reportdesigner.actions.FontBoldAction;
import org.plazmaforge.studio.reportdesigner.actions.FontItalicAction;
import org.plazmaforge.studio.reportdesigner.actions.FontStrikeoutAction;
import org.plazmaforge.studio.reportdesigner.actions.FontUnderlineAction;
import org.plazmaforge.studio.reportdesigner.actions.GroupListAction;
import org.plazmaforge.studio.reportdesigner.actions.IdentifierListAction;
import org.plazmaforge.studio.reportdesigner.actions.ImportListAction;
import org.plazmaforge.studio.reportdesigner.actions.LineTransformAction;
import org.plazmaforge.studio.reportdesigner.actions.OpenSubreportAction;
import org.plazmaforge.studio.reportdesigner.actions.PasteAction;
import org.plazmaforge.studio.reportdesigner.actions.PreviewAction;
import org.plazmaforge.studio.reportdesigner.actions.PreviewWithEmptyDataAction;
import org.plazmaforge.studio.reportdesigner.actions.ReportEditAction;
import org.plazmaforge.studio.reportdesigner.actions.SizeToContainerAction;
import org.plazmaforge.studio.reportdesigner.actions.TextBottomAlignAction;
import org.plazmaforge.studio.reportdesigner.actions.TextCenterAlignAction;
import org.plazmaforge.studio.reportdesigner.actions.TextJustifiedAlignAction;
import org.plazmaforge.studio.reportdesigner.actions.TextLeftAlignAction;
import org.plazmaforge.studio.reportdesigner.actions.TextMiddleAlignAction;
import org.plazmaforge.studio.reportdesigner.actions.TextRightAlignAction;
import org.plazmaforge.studio.reportdesigner.actions.TextTopAlignAction;
import org.plazmaforge.studio.reportdesigner.actions.ToggleGridVisibilityAction;
import org.plazmaforge.studio.reportdesigner.actions.ToggleSnapToGridAction;
import org.plazmaforge.studio.reportdesigner.dnd.DropExpressionListener;
import org.plazmaforge.studio.reportdesigner.dnd.ExpressionTransfer;
import org.plazmaforge.studio.reportdesigner.model.Report;
import org.plazmaforge.studio.reportdesigner.model.ReportConstants;
import org.plazmaforge.studio.reportdesigner.model.data.Expression;
import org.plazmaforge.studio.reportdesigner.model.data.Identifier;
import org.plazmaforge.studio.reportdesigner.parts.ReportPartFactory;
import org.plazmaforge.studio.reportdesigner.parts.ReportTreePartFactory;
import org.plazmaforge.studio.reportdesigner.rules.HorizontalRulerProvider;
import org.plazmaforge.studio.reportdesigner.rules.ReportRulerComposite;
import org.plazmaforge.studio.reportdesigner.rules.VerticalRulerProvider;
import org.plazmaforge.studio.reportdesigner.storage.ReportManager;
import org.plazmaforge.studio.reportdesigner.storage.ReportManagerFactory;
import org.plazmaforge.studio.reportdesigner.storage.ReportReader;
import org.plazmaforge.studio.reportdesigner.storage.ReportWriter;
import org.plazmaforge.studio.reportdesigner.views.ContentPage;
import org.plazmaforge.studio.reportdesigner.views.IContentPage;
import org.plazmaforge.studio.reportdesigner.views.OutlinePage;

/** 
 * @author Oleh Hapon
 * $Id: ReportEditor.java,v 1.37 2010/11/16 07:57:07 ohapon Exp $
 */
public class ReportEditor extends J2DGraphicalEditorWithFlyoutPalette {

    public static final String ID = ReportEditor.class.getName();

    public static final int EDITOR_MARGIN = 20;

    private PaletteRoot paletteRoot;

    private ReportRulerComposite rulerComposite;

    private Report report;

    private ReportManager reportManager;

    private OutlinePage outlinePage;

    private ContentPage contentPage;

    private boolean actionActivity;

    private boolean savePreviouslyNeeded;

    private boolean editorSaving;

    private Object fileObject;

    private KeyHandler sharedKeyHandler;

    public ReportEditor() {
        super();
        DefaultEditDomain editDomain = new DefaultEditDomain(this);
        editDomain.setActiveTool(new PanningSelectionTool());
        setEditDomain(editDomain);
        IPreferenceStore store = ReportDesignerPlugin.getDefault().getPreferenceStore();
        store.addPropertyChangeListener(new ReportPreferenceChangeListener(this, store));
    }

    public PaletteRoot getPaletteRoot() {
        if (paletteRoot == null) {
            paletteRoot = new PaletteCreator(this).createPaletteRoot();
        }
        return paletteRoot;
    }

    private ContentPage getContentPage() {
        if (contentPage == null) {
            contentPage = new ContentPage(this);
        }
        return contentPage;
    }

    public PaletteRoot createPaletteRoot() {
        PaletteRoot paletteRoot = new PaletteRoot();
        return paletteRoot;
    }

    protected void createGraphicalViewer(Composite parent) {
        rulerComposite = new ReportRulerComposite(parent, SWT.NONE);
        GraphicalViewer viewer = new J2DScrollingGraphicalViewer();
        viewer.createControl(rulerComposite);
        setGraphicalViewer(viewer);
        configureGraphicalViewer();
        hookGraphicalViewer();
        parent.layout();
        initializeGraphicalViewer();
        FigureCanvas editor = (FigureCanvas) viewer.getControl();
        ((DefaultRangeModel) editor.getViewport().getHorizontalRangeModel()).setMinimum(30);
        rulerComposite.setGraphicalViewer((ScrollingGraphicalViewer) viewer);
    }

    protected Control getGraphicalControl() {
        return rulerComposite;
    }

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        final ScrollingGraphicalViewer scrollingGraphicalViewer = (ScrollingGraphicalViewer) getGraphicalViewer();
        ScalableFreeformRootEditPart root = new J2DScalableFreeformRootEditPart();
        scrollingGraphicalViewer.setRootEditPart(root);
        ZoomManager zoomManager = root.getZoomManager();
        ArrayList arraylist = new ArrayList(3);
        arraylist.add(ZoomManager.FIT_ALL);
        arraylist.add(ZoomManager.FIT_WIDTH);
        arraylist.add(ZoomManager.FIT_HEIGHT);
        zoomManager.setZoomLevelContributions(arraylist);
        double ad[] = { 0.01D, 0.10000000000000001D, 0.25D, 0.5D, 0.75D, 1.0D, 1.5D, 2D, 2.5D, 3D, 4D };
        zoomManager.setZoomLevels(ad);
        ReportContextMenuProvider erdcontextmenuprovider = new ReportContextMenuProvider(getGraphicalViewer(), getActionRegistry(), this);
        getGraphicalViewer().setContextMenu(erdcontextmenuprovider);
        ZoomInAction zoomInAction = new ZoomInAction(root.getZoomManager());
        ZoomOutAction zoomOutAction = new ZoomOutAction(root.getZoomManager());
        getActionRegistry().registerAction(zoomInAction);
        getActionRegistry().registerAction(zoomOutAction);
        getSite().getKeyBindingService().registerAction(zoomInAction);
        getSite().getKeyBindingService().registerAction(zoomOutAction);
        IAction action = null;
        action = new ToggleGridVisibilityAction(this, getGraphicalViewer());
        getActionRegistry().registerAction(action);
        action = new ToggleSnapToGridAction(this, getGraphicalViewer());
        getActionRegistry().registerAction(action);
        IPreferenceStore store = ReportDesignerPlugin.getDefault().getPreferenceStore();
        getGraphicalViewer().getControl().setBackground(ReportDesignerConstants.DEFAULT_EDITOR_BACKGROUND);
        getGraphicalViewer().addDropTargetListener((TransferDropTargetListener) new DropExpressionListener(getGraphicalViewer()));
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_HORIZONTAL_RULER, new HorizontalRulerProvider());
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_VERTICAL_RULER, new VerticalRulerProvider());
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_RULER_VISIBILITY, store.getBoolean(ReportDesignerConstants.RULERS_VISIBLE_PREF));
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, store.getBoolean(ReportDesignerConstants.GRID_VISIBLE_PREF));
        getGraphicalViewer().setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, store.getBoolean(ReportDesignerConstants.SNAP_TO_GRID_PREF));
        int gridSpacing = store.getInt(ReportDesignerConstants.GRID_SPACING_PREF);
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(gridSpacing, gridSpacing));
        getGraphicalViewer().setEditPartFactory(new ReportPartFactory());
        getGraphicalViewer().setKeyHandler((new GraphicalViewerKeyHandler(scrollingGraphicalViewer)).setParent(getCommonKeyHandler()));
        zoomManager.addZoomListener(new ZoomListener() {

            public void zoomChanged(double d) {
                Composite parent = scrollingGraphicalViewer.getControl().getParent();
                parent.layout();
            }
        });
    }

    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        if (report != null) {
            getGraphicalViewer().setContents(report);
        }
    }

    public void setInput(IEditorInput input) {
        super.setInput(input);
        actionActivity = false;
        if (input != null && input instanceof FileEditorInput) {
            IFile file = ((FileEditorInput) input).getFile();
            setPartName(file.getName());
        } else {
            setPartName(input.getName());
        }
        readReportFromInput(input);
    }

    private void readReportFromInput(IEditorInput input) {
        try {
            fileObject = EditorInputFactory.getFileObject(input);
            if (input instanceof ReportEditorInput) {
                report = ((ReportEditorInput) input).getReport();
            } else {
                readReportFromFile(fileObject);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readReportFromFile(Object fileObject) throws Exception {
        if (fileObject instanceof IFile) {
            report = getReportReader().readFromFile((IFile) fileObject);
        } else if (fileObject instanceof File) {
            report = getReportReader().readFromFile((File) fileObject);
        }
    }

    private void writeReportToFile(Object fileObject) throws Exception {
        if (fileObject instanceof IFile) {
            getReportWriter().writeToFile(report, (IFile) fileObject);
        } else if (fileObject instanceof File) {
            getReportWriter().writeToFile(report, (File) fileObject);
        }
    }

    public void doSave(IProgressMonitor iprogressmonitor) {
        try {
            if (!isValidReport()) {
                return;
            }
            editorSaving = true;
            writeReportToFile(fileObject);
            actionActivity = false;
            getCommandStack().markSaveLocation();
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            editorSaving = false;
        }
        return;
    }

    public void doSaveAs() {
        if (!isValidReport()) {
            return;
        }
        Shell shell = getSite().getShell();
        IPath path;
        final Object opFile;
        if (fileObject instanceof IFile) {
            SaveAsDialog saveAsDialog = new SaveAsDialog(shell);
            saveAsDialog.setOriginalFile((IFile) fileObject);
            if (saveAsDialog.open() != 0) {
                return;
            }
            path = saveAsDialog.getResult();
            if (path == null) {
                return;
            }
            opFile = getWorkspaceRoot().getFile(path);
        } else {
            FileDialog fileDialog = new FileDialog(shell);
            fileDialog.setText("Save As");
            fileDialog.setFilterPath(((File) fileObject).getPath());
            fileDialog.setFileName(((File) fileObject).getName());
            String filePath = fileDialog.open();
            if (filePath == null) {
                return;
            }
            path = new Path(filePath);
            opFile = path.toFile();
        }
        IEditorInput editorInput = EditorInputFactory.getEditorInput(opFile);
        WorkspaceModifyOperation modifyOperation = new WorkspaceModifyOperation() {

            public void execute(IProgressMonitor iprogressmonitor) throws CoreException {
                try {
                    writeReportToFile(opFile);
                } catch (Exception ex) {
                    StatusInfo i = new StatusInfo(IStatus.ERROR, "" + ex.getMessage());
                    throw new CoreException(i);
                }
            }
        };
        try {
            editorSaving = true;
            (new ProgressMonitorDialog(getSite().getShell())).run(false, false, modifyOperation);
            setInput(editorInput);
            getGraphicalViewer().setContents(report);
            flushStack();
            actionActivity = false;
            getCommandStack().markSaveLocation();
        } catch (Exception exception) {
        } finally {
            editorSaving = false;
        }
        return;
    }

    public CommandStack getCommandStack() {
        return super.getCommandStack();
    }

    private boolean isValidReport() {
        return true;
    }

    private ReportReader getReportReader() {
        return getReportManager().getReportReader();
    }

    private ReportWriter getReportWriter() {
        return getReportManager().getReportWriter();
    }

    private ReportManager getReportManager() {
        if (reportManager == null) {
            reportManager = ReportManagerFactory.getReportManager();
        }
        return reportManager;
    }

    public Report getReport() {
        return report;
    }

    public Object getAdapter(Class type) {
        if (type == org.eclipse.ui.views.contentoutline.IContentOutlinePage.class) {
            return outlinePage = createOutlinePage();
        } else if (type.equals(IContentPage.class)) {
            return getContentPage();
        } else if (type == org.eclipse.gef.editparts.ZoomManager.class) {
            return ((ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();
        } else if (type == ReportEditorSelectionAdapter.class) {
            return getSelectionAdapter();
        } else if (type == Report.class) {
            return getReport();
        }
        return super.getAdapter(type);
    }

    public void configureOutlineViewer(EditPartViewer viewer) {
        viewer.setEditDomain(getEditDomain());
        viewer.setEditPartFactory(new ReportTreePartFactory());
        viewer.setKeyHandler(getCommonKeyHandler());
    }

    public void hookOutlineViewer(EditPartViewer viewer) {
        getSelectionSynchronizer().addViewer(viewer);
    }

    public void unhookOutlineViewer(EditPartViewer viewer) {
        getSelectionSynchronizer().removeViewer(viewer);
    }

    private OutlinePage createOutlinePage() {
        final TreeViewer treeViewer = new TreeViewer();
        treeViewer.addDragSourceListener(new TransferDragSourceListener() {

            public Transfer getTransfer() {
                return ExpressionTransfer.getInstance();
            }

            public void dragFinished(DragSourceEvent arg0) {
            }

            public void dragSetData(DragSourceEvent arg0) {
            }

            public void dragStart(DragSourceEvent event) {
                ISelection selection = treeViewer.getSelection();
                if (selection.isEmpty() || !(selection instanceof StructuredSelection)) {
                    event.doit = false;
                } else {
                    final List<Expression> expressions = new ArrayList<Expression>();
                    for (Iterator iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
                        Object part = iter.next();
                        if (!(part instanceof TreeEditPart)) {
                            continue;
                        }
                        Object element = ((TreeEditPart) part).getModel();
                        if (element instanceof Identifier) {
                            expressions.add(createExpression((Identifier) element));
                        } else {
                            event.doit = false;
                            return;
                        }
                    }
                    ExpressionTransfer.getInstance().setExpressions(expressions);
                    event.doit = true;
                }
            }

            private Expression createExpression(Identifier identifier) {
                return identifier.createExpression();
            }
        });
        return new OutlinePage(this, treeViewer);
    }

    protected void createActions() {
        super.createActions();
        IWorkbenchPart workbenchPart = (IWorkbenchPart) this;
        addSelectionAction(new MatchWidthAction(this));
        addSelectionAction(new MatchHeightAction(this));
        addSelectionAction(new AlignmentAction(workbenchPart, PositionConstants.LEFT));
        addSelectionAction(new AlignmentAction(workbenchPart, PositionConstants.CENTER));
        addSelectionAction(new AlignmentAction(workbenchPart, PositionConstants.RIGHT));
        addSelectionAction(new AlignmentAction(workbenchPart, PositionConstants.TOP));
        addSelectionAction(new AlignmentAction(workbenchPart, PositionConstants.MIDDLE));
        addSelectionAction(new AlignmentAction(workbenchPart, PositionConstants.BOTTOM));
        addAction(new IdentifierListAction(this));
        addAction(new BandListAction(this));
        addAction(new GroupListAction(this));
        addAction(new DataSourceEditAction(this));
        addAction(new DatasetListAction(this));
        addAction(new ReportEditAction(this));
        addAction(new ClasspathListAction(this));
        addAction(new ImportListAction(this));
        addAction(new CompileAction(this));
        addAction(new PreviewAction(this));
        addAction(new PreviewWithEmptyDataAction(this));
        addAction(new ExportAction(this));
        addSelectionAction(new EditExpressionAction(this));
        addSelectionAction(new ElementEditAction(this));
        addSelectionAction(new OpenSubreportAction(this));
        addSelectionAction(new CrosstabCellEditAction(this));
        addSelectionAction(new CrosstabEditAction(this));
        addSelectionAction(new FontBoldAction(this));
        addSelectionAction(new FontItalicAction(this));
        addSelectionAction(new FontUnderlineAction(this));
        addSelectionAction(new FontStrikeoutAction(this));
        addSelectionAction(new TextLeftAlignAction(this));
        addSelectionAction(new TextCenterAlignAction(this));
        addSelectionAction(new TextRightAlignAction(this));
        addSelectionAction(new TextJustifiedAlignAction(this));
        addSelectionAction(new TextTopAlignAction(this));
        addSelectionAction(new TextMiddleAlignAction(this));
        addSelectionAction(new TextBottomAlignAction(this));
        addSelectionAction(new BorderLeftAction(this));
        addSelectionAction(new BorderTopAction(this));
        addSelectionAction(new BorderRightAction(this));
        addSelectionAction(new BorderBottomAction(this));
        addSelectionAction(new BorderAction(this));
        addSelectionAction(new CopyAction(this));
        addSelectionAction(new PasteAction(this));
        addSelectionAction(new CutAction(this));
        addSelectionAction(new LineTransformAction(this, LineTransformAction.HORIZONTAL));
        addSelectionAction(new LineTransformAction(this, LineTransformAction.VERTICAL));
        addSelectionAction(new AlignToContainerAction(this, PositionConstants.LEFT));
        addSelectionAction(new AlignToContainerAction(this, PositionConstants.CENTER));
        addSelectionAction(new AlignToContainerAction(this, PositionConstants.RIGHT));
        addSelectionAction(new AlignToContainerAction(this, PositionConstants.TOP));
        addSelectionAction(new AlignToContainerAction(this, PositionConstants.MIDDLE));
        addSelectionAction(new AlignToContainerAction(this, PositionConstants.BOTTOM));
        addSelectionAction(new SizeToContainerAction(this, SizeToContainerAction.WIDTH));
        addSelectionAction(new SizeToContainerAction(this, SizeToContainerAction.HEIGHT));
        addSelectionAction(new SizeToContainerAction(this, SizeToContainerAction.BOTH));
    }

    private void addSelectionAction(IAction action) {
        addAction(action, true);
    }

    private void addAction(IAction action) {
        addAction(action, false);
    }

    private void addAction(IAction action, boolean isSelectionAction) {
        if (action == null) {
            return;
        }
        getActionRegistry().registerAction(action);
        if (!isSelectionAction) {
            return;
        }
        getSelectionActions().add(action.getId());
    }

    public static IWorkspace getWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    public static IWorkspaceRoot getWorkspaceRoot() {
        return getWorkspace().getRoot();
    }

    public void flushStack() {
        getCommandStack().flush();
    }

    public boolean isDirty() {
        return isSaveOnCloseNeeded();
    }

    public boolean isSaveOnCloseNeeded() {
        return actionActivity | getCommandStack().isDirty();
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Force mark dirty
     *
     */
    public void makeDirty() {
        actionActivity = true;
        firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
    }

    public void commandStackChanged(EventObject eventobject) {
        if (isDirty()) {
            if (!savePreviouslyNeeded) {
                savePreviouslyNeeded = true;
                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
            }
        } else {
            savePreviouslyNeeded = false;
            firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
        }
        super.commandStackChanged(eventobject);
    }

    private KeyHandler getCommonKeyHandler() {
        if (sharedKeyHandler == null) {
            sharedKeyHandler = new KeyHandler();
            sharedKeyHandler.put(KeyStroke.getPressed('\177', 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
            sharedKeyHandler.put(KeyStroke.getPressed(0x100000b, 0), getActionRegistry().getAction("org.eclipse.gef.direct_edit"));
        }
        return sharedKeyHandler;
    }

    public Object getFileObject() {
        return fileObject;
    }

    private ReportEditorSelectionAdapter selectionAdapter;

    public ReportEditorSelectionAdapter getSelectionAdapter() {
        if (selectionAdapter == null) {
            selectionAdapter = new ReportEditorSelectionAdapter(this);
        }
        return selectionAdapter;
    }

    public void refreshContentPage() {
        if (contentPage == null) {
            return;
        }
        contentPage.refresh();
    }

    public void updateActions(List actionIds) {
        super.updateActions(actionIds);
    }

    public void updateAction(String actionId) {
        if (actionId == null) {
            return;
        }
        List actions = new ArrayList();
        actions.add(actionId);
        super.updateActions(actions);
    }

    public GraphicalViewer getGraphicalViewer() {
        return super.getGraphicalViewer();
    }

    class J2DScalableFreeformRootEditPart extends ScalableFreeformRootEditPart {

        protected ScalableFreeformLayeredPane createScaledLayers() {
            ScalableFreeformLayeredPane layers = new J2DScalableFreeformLayeredPane();
            layers.add(createGridLayer(), GRID_LAYER);
            layers.add(getPrintableLayers(), PRINTABLE_LAYERS);
            layers.add(new FeedbackLayer(), SCALED_FEEDBACK_LAYER);
            return layers;
        }
    }

    class FeedbackLayer extends FreeformLayer {

        FeedbackLayer() {
            setEnabled(false);
        }
    }
}
