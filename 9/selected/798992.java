package bpmetrics.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.bpel.ui.*;
import org.eclipse.bpel.common.extension.model.ExtensionMap;
import org.eclipse.bpel.common.ui.editmodel.IEditModelListener;
import org.eclipse.bpel.common.ui.editmodel.ResourceInfo;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.CorrelationSet;
import org.eclipse.bpel.model.ExtensibleElement;
import org.eclipse.bpel.model.MessageExchange;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.ui.BPELUIPlugin;
import org.eclipse.bpel.ui.IBPELUIConstants;
import org.eclipse.bpel.ui.adapters.AdapterNotification;
import org.eclipse.bpel.ui.editparts.ProcessTrayEditPart;
import org.eclipse.bpel.ui.editparts.util.OutlineTreePartFactory;
import org.eclipse.bpel.ui.properties.BPELPropertySection;
import org.eclipse.bpel.ui.uiextensionmodel.StartNode;
import org.eclipse.bpel.ui.util.BPELEditModelClient;
import org.eclipse.bpel.ui.util.BPELEditorUtil;
import org.eclipse.bpel.ui.util.BPELReader;
import org.eclipse.bpel.ui.util.ModelHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.draw2d.parts.Thumbnail;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.NotificationImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.internal.views.properties.tabbed.view.Tab;
import org.eclipse.ui.internal.views.properties.tabbed.view.TabDescriptor;
import org.eclipse.ui.internal.views.properties.tabbed.view.TabbedPropertyViewer;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import bpmetrics.views.BPElementViewImpl;
import bpmetrics.views.BPStartWSView;

public class XMLEditor extends MultiPageEditorPart implements IEditModelListener, IGotoMarker {

    class OutlinePage extends ContentOutlinePage {

        private PageBook pageBook;

        private Control outline;

        private Canvas overview;

        private IAction showOutlineAction, showOverviewAction;

        static final int ID_OUTLINE = 0;

        static final int ID_OVERVIEW = 1;

        private Thumbnail thumbnail;

        public OutlinePage(EditPartViewer viewer) {
            super(viewer);
        }

        @Override
        public EditPartViewer getViewer() {
            return super.getViewer();
        }

        private void configureOutlineViewer() {
            getViewer().setEditDomain(getEditDomain());
            getViewer().setEditPartFactory(new OutlineTreePartFactory());
            fDesignViewer.registerViewer(getViewer());
            ContextMenuProvider provider = new ProcessContextMenuProvider(getViewer(), fDesignViewer.getActionRegistry());
            getViewer().setContextMenu(provider);
            getSite().registerContextMenu("org.eclipse.bpel.outline.contextmenu", provider, getSite().getSelectionProvider());
            getViewer().setKeyHandler(fDesignViewer.getKeyHandler());
            IToolBarManager tbm = getSite().getActionBars().getToolBarManager();
            showOutlineAction = new Action() {

                @Override
                public void run() {
                    showPage(ID_OUTLINE);
                }

                @Override
                public String getToolTipText() {
                    return Messages.OutlinePage_showOutlineView;
                }
            };
            showOutlineAction.setImageDescriptor(BPELUIPlugin.INSTANCE.getImageDescriptor(IBPELUIConstants.ICON_OUTLINE_16));
            tbm.add(showOutlineAction);
            showOverviewAction = new Action() {

                @Override
                public void run() {
                    showPage(ID_OVERVIEW);
                }

                @Override
                public String getToolTipText() {
                    return Messages.OutlinePage_showOverviewView;
                }
            };
            showOverviewAction.setImageDescriptor(BPELUIPlugin.INSTANCE.getImageDescriptor(IBPELUIConstants.ICON_OVERVIEW_16));
            tbm.add(showOverviewAction);
            showPage(ID_OUTLINE);
        }

        @Override
        public Control getControl() {
            return pageBook;
        }

        @Override
        public void createControl(Composite parent) {
            pageBook = new PageBook(parent, SWT.NONE);
            outline = getViewer().createControl(pageBook);
            overview = new Canvas(pageBook, SWT.NONE);
            pageBook.showPage(outline);
            configureOutlineViewer();
            getViewer().setContents(getProcess());
        }

        private void initializeOverview() {
            LightweightSystem lws = new LightweightSystem(overview);
            RootEditPart rep = fDesignViewer.getGraphicalViewer().getRootEditPart();
            if (rep instanceof GraphicalBPELRootEditPart) {
                GraphicalBPELRootEditPart root = (GraphicalBPELRootEditPart) rep;
                thumbnail = new ScrollableThumbnail((Viewport) root.getFigure());
                thumbnail.setSource(root.getLayer(LayerConstants.PRINTABLE_LAYERS));
                lws.setContents(thumbnail);
            }
        }

        private void showPage(int id) {
            if (id == ID_OUTLINE) {
                showOutlineAction.setChecked(true);
                showOverviewAction.setChecked(false);
                pageBook.showPage(outline);
                if (thumbnail != null) thumbnail.setVisible(false);
            } else if (id == ID_OVERVIEW) {
                initializeOverview();
                showOutlineAction.setChecked(false);
                showOverviewAction.setChecked(true);
                pageBook.showPage(overview);
                thumbnail.setVisible(true);
            }
        }

        @Override
        public void dispose() {
            super.dispose();
        }

        @Override
        public void init(IPageSite pageSite) {
            super.init(pageSite);
            ActionRegistry registry = fDesignViewer.getActionRegistry();
            IActionBars bars = pageSite.getActionBars();
            String id = ActionFactory.UNDO.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = ActionFactory.REDO.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = ActionFactory.DELETE.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            id = ActionFactory.REVERT.getId();
            bars.setGlobalActionHandler(id, registry.getAction(id));
            bars.updateActionBars();
        }
    }

    protected class TextEditorSelectionListener implements ISelectionChangedListener {

        public void selectionChanged(SelectionChangedEvent event) {
            if (getActivePage() != DESIGN_PAGE_INDEX) {
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    List<Object> selections = new ArrayList<Object>();
                    for (Iterator<Object> i = ((IStructuredSelection) selection).iterator(); i.hasNext(); ) {
                        Object domNode = i.next();
                        if (domNode instanceof Element) {
                            Object facade = BPELEditorUtil.getInstance().findModelObjectForElement(process, (Element) domNode);
                            if (facade != null) {
                                selections.add(facade);
                            }
                        }
                    }
                    if (!selections.isEmpty()) {
                        StructuredSelection bpelSelection = new StructuredSelection(selections);
                        fDesignViewer.getAdaptingSelectionProvider().setSelection(bpelSelection);
                    }
                }
            }
        }
    }

    protected class DesignViewerSelectionListener implements ISelectionChangedListener {

        public void selectionChanged(SelectionChangedEvent event) {
            if (getActivePage() != SOURCE_PAGE_INDEX) {
                try {
                    ISelection sel = fDesignViewer.getSelection();
                    Object selectedNode = ((IStructuredSelection) sel).getFirstElement();
                    Element selectedNodeElement = null;
                    if (selectedNode instanceof StartNode) {
                        selectedNodeElement = ((StartNode) selectedNode).getProcess().getElement();
                    } else if (selectedNode instanceof ExtensibleElement) {
                        selectedNodeElement = ((ExtensibleElement) selectedNode).getElement();
                    }
                    if (selectedNodeElement != null) {
                        StructuredSelection nodeSelection = new StructuredSelection(selectedNodeElement);
                        getTextEditor().getSelectionProvider().setSelection(nodeSelection);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Process process;

    private DefaultEditDomain editDomain;

    protected ModelListenerAdapter modelListenerAdapter;

    private Resource extensionsResource;

    private ExtensionMap extensionMap;

    protected StructuredTextEditor fTextEditor = null;

    protected NewBPELEditor fDesignViewer = null;

    protected int currentPage = -1;

    protected TextEditorSelectionListener textEditorSelectionListener;

    protected DesignViewerSelectionListener designViewerSelectionListener;

    private IFileChangeListener fileChangeListener;

    protected IResourceChangeListener postBuildRefactoringListener;

    private OutlinePage outlinePage;

    protected BPELTabbedPropertySheetPage currentPropertySheetPage;

    protected ActionRegistry actionRegistry;

    private static int DESIGN_PAGE_INDEX = 0;

    private static int SOURCE_PAGE_INDEX = 1;

    private Map<Long, EObject> fMarkers2EObject = new HashMap<Long, EObject>();

    private Notification fMarkersStale = new NotificationImpl(AdapterNotification.NOTIFICATION_MARKERS_STALE, null, null);

    public XMLEditor() {
        super();
        setEditDomain(new BPELEditDomain(this));
    }

    /**
	 * Connects the design viewer with the viewer selection manager. Should be
	 * done after createSourcePage() is done because we need to get the
	 * ViewerSelectionManager from the TextEditor. setModel is also done here
	 * because getModel() needs to reference the TextEditor.
	 */
    protected void connectDesignPage() {
        designViewerSelectionListener = new DesignViewerSelectionListener();
        fDesignViewer.getAdaptingSelectionProvider().addSelectionChangedListener(designViewerSelectionListener);
        textEditorSelectionListener = new TextEditorSelectionListener();
        ISelectionProvider provider = getTextEditor().getSelectionProvider();
        if (provider instanceof IPostSelectionProvider) {
            ((IPostSelectionProvider) provider).addPostSelectionChangedListener(textEditorSelectionListener);
        } else {
            provider.addSelectionChangedListener(textEditorSelectionListener);
        }
    }

    /**
	 * Creates the design page of the multi-page editor.
	 */
    protected void createDesignPage() {
        fDesignViewer = new NewBPELEditor(getEditDomain());
        loadModel();
        try {
            addPage(0, fDesignViewer, getEditorInput());
            setPageText(0, "Design");
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(), "Error creating Design page", null, e.getStatus());
        }
        showBPElementsView(getProcess());
        showBPStartWSView(getProcess());
    }

    /**
	 * Creates the source page of the multi-page editor.
	 */
    protected void createSourcePage() throws PartInitException {
        fTextEditor = new StructuredTextEditor();
        try {
            addPage(0, fTextEditor, getEditorInput());
            setPageText(0, "Source");
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(), "Error creating Source page", null, e.getStatus());
        }
    }

    /**
	 * Creates the pages of this multi-page editor.
	 */
    @Override
    protected void createPages() {
        try {
            createSourcePage();
            createDesignPage();
            firePropertyChange(PROP_TITLE);
            connectDesignPage();
            initializeFileChangeListener();
            initializeRefactoringListener();
        } catch (PartInitException e) {
            throw new RuntimeException(e);
        }
        if (BPELUIPlugin.INSTANCE.getDefaultPage().equals(IBPELUIConstants.SOURCE_PAGE)) {
            setActivePage(SOURCE_PAGE_INDEX);
        } else {
            setActivePage(DESIGN_PAGE_INDEX);
        }
    }

    @Override
    public void dispose() {
        if (currentPage == SOURCE_PAGE_INDEX) {
            BPELUIPlugin.INSTANCE.setDefaultPage(IBPELUIConstants.SOURCE_PAGE);
        } else {
            BPELUIPlugin.INSTANCE.setDefaultPage(IBPELUIConstants.DESIGN_PAGE);
        }
        outlinePage = null;
        process = null;
        if (fileChangeListener != null) {
            BPELUIPlugin.INSTANCE.getResourceChangeListener().removeListener(fileChangeListener);
        }
        if (postBuildRefactoringListener != null) {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            workspace.removeResourceChangeListener(postBuildRefactoringListener);
        }
        IStructuredModel model = fTextEditor.getModel();
        model.releaseFromEdit();
        fDesignViewer.dispose();
        fTextEditor.dispose();
        showBPElementsView(null);
        showBPStartWSView(null);
        super.dispose();
    }

    public void doRevertToSaved(IProgressMonitor monitor) {
    }

    /**
	 * @see org.eclipse.ui.IEditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
    @Override
    public void doSave(IProgressMonitor progressMonitor) {
        fDesignViewer.getCommandFramework().applyCurrentChange();
        fTextEditor.doSave(progressMonitor);
        fDesignViewer.getEditModelClient().getPrimaryResourceInfo().resetSynchronizeStamp();
        fDesignViewer.getEditModelClient().getPrimaryResourceInfo().getResource().setModified(false);
        fDesignViewer.doSave(progressMonitor);
    }

    @Override
    public void doSaveAs() {
    }

    protected BPELTabbedPropertySheetPage createBPELTabbedPropertySheetPage() {
        return new BPELTabbedPropertySheetPage(new ITabbedPropertySheetPageContributor() {

            public String getContributorId() {
                return IBPELUIConstants.BPEL_EDITOR_ID;
            }
        }, fDesignViewer);
    }

    protected ActionRegistry getActionRegistry() {
        if (actionRegistry == null) actionRegistry = new ActionRegistry();
        return actionRegistry;
    }

    @Override
    public Object getAdapter(Class type) {
        if (type == Process.class) {
            return process;
        }
        if (type == BPELEditModelClient.class) {
            return process;
        }
        if (type == ModelListenerAdapter.class) {
            return modelListenerAdapter;
        }
        if (type == Resource.class) {
            return extensionsResource;
        }
        if (type == ExtensionMap.class) {
            return extensionMap;
        }
        if (type == CommandStack.class) {
            return getCommandStack();
        }
        if (type == IContentOutlinePage.class) {
            if (outlinePage == null) {
                outlinePage = new OutlinePage(new TreeViewer());
            }
            return outlinePage;
        }
        if (type == IPropertySheetPage.class) {
            currentPropertySheetPage = createBPELTabbedPropertySheetPage();
            return currentPropertySheetPage;
        }
        if (type == ActionRegistry.class) {
            return getActionRegistry();
        }
        return super.getAdapter(type);
    }

    public CommandStack getCommandStack() {
        return getEditDomain().getCommandStack();
    }

    protected int getDefaultPageTypeIndex() {
        int pageIndex = DESIGN_PAGE_INDEX;
        if (BPELUIPlugin.INSTANCE.getDefaultPage().equals(IBPELUIConstants.SOURCE_PAGE)) {
            pageIndex = SOURCE_PAGE_INDEX;
        }
        return pageIndex;
    }

    /**
	 * Returns the design viewer
	 * @return the design viewer
	 */
    protected NewBPELEditor getDesignEditor() {
        return fDesignViewer;
    }

    /**
	 * Returns the edit domain.
	 * @return the edit domain
	 */
    protected DefaultEditDomain getEditDomain() {
        return editDomain;
    }

    protected IFile getFileInput() {
        return ((IFileEditorInput) getEditorInput()).getFile();
    }

    public Process getProcess() {
        return process;
    }

    /**
	 * Returns the design viewer
	 * @return the design viewer
	 */
    protected StructuredTextEditor getSourceViewer() {
        return fTextEditor;
    }

    StructuredTextEditor getTextEditor() {
        return fTextEditor;
    }

    public void gotoMarker(IMarker marker) {
        String href = null;
        try {
            href = (String) marker.getAttribute("address.model");
        } catch (CoreException ex) {
            BPELUIPlugin.log(ex);
        }
        EObject modelObject = null;
        if (href != null) {
            try {
                modelObject = fDesignViewer.getResource().getEObject(href);
            } catch (Throwable t) {
                BPELUIPlugin.log(t);
            }
        }
        gotoText(marker);
        if (modelObject == null) {
            return;
        }
        gotoMarker(marker, modelObject);
    }

    private void gotoText(IMarker marker) {
        Integer charStart = null;
        Integer charEnd = null;
        try {
            charStart = (Integer) marker.getAttribute("charStart");
            charEnd = (Integer) marker.getAttribute("charEnd");
        } catch (CoreException ex) {
            BPELUIPlugin.log(ex);
        }
        charStart = charStart == null ? 0 : charStart;
        charEnd = charEnd == null ? charStart : charEnd;
        try {
            fTextEditor.setHighlightRange(charStart, charEnd - charStart, true);
        } catch (Throwable t) {
            BPELUIPlugin.log(t);
        }
    }

    void gotoMarker(IMarker marker, EObject modelObject) {
        GraphicalViewer graphViewer = fDesignViewer.getGraphicalViewer();
        EObject refObj = null;
        EditPart editPart = null;
        if (modelObject instanceof Variable || modelObject instanceof PartnerLink || modelObject instanceof CorrelationSet || modelObject instanceof MessageExchange) {
            refObj = ModelHelper.getContainingScope(modelObject);
            editPart = (EditPart) graphViewer.getEditPartRegistry().get(refObj);
            if (editPart != null) {
                graphViewer.reveal(editPart);
            }
            fDesignViewer.selectModelObject(modelObject);
        } else if (modelObject instanceof Activity) {
            refObj = modelObject;
            editPart = (EditPart) graphViewer.getEditPartRegistry().get(refObj);
            if (editPart != null) {
                graphViewer.reveal(editPart);
            }
            fDesignViewer.selectModelObject(modelObject);
        } else {
            refObj = modelObject;
            while (refObj != null && !(refObj instanceof Activity)) {
                refObj = refObj.eContainer();
            }
            if (refObj == null) {
                refObj = ModelHelper.getProcess(modelObject);
            }
            modelObject = refObj;
            editPart = (EditPart) graphViewer.getEditPartRegistry().get(modelObject);
            if (editPart != null) {
                graphViewer.reveal(editPart);
            }
            fDesignViewer.selectModelObject(modelObject);
        }
        BPELTabbedPropertySheetPage propertySheetPage = currentPropertySheetPage;
        if (propertySheetPage == null) {
            return;
        }
        TabbedPropertyViewer viewer = propertySheetPage.getTabbedPropertyViewer();
        int j = 0;
        while (true) {
            TabDescriptor descriptor = null;
            try {
                descriptor = (TabDescriptor) viewer.getElementAt(j++);
            } catch (IndexOutOfBoundsException iobe) {
                break;
            }
            if (descriptor == null) {
                break;
            }
            Tab tab = descriptor.createTab();
            ISection[] sections = tab.getSections();
            for (int i = 0; i < sections.length; i++) {
                if (BPELPropertySection.class.isInstance(sections[i]) == false) {
                    continue;
                }
                BPELPropertySection section = (BPELPropertySection) sections[i];
                section.createControls(new Composite(getSite().getShell(), 0), propertySheetPage);
                section.setInput(this, new StructuredSelection(modelObject));
                if (section.isValidMarker(marker)) {
                    showPropertiesView();
                    viewer = currentPropertySheetPage.getTabbedPropertyViewer();
                    viewer.setSelection(new StructuredSelection(descriptor));
                    tab = currentPropertySheetPage.getCurrentTab();
                    section = (BPELPropertySection) tab.getSectionAtIndex(i);
                    section.gotoMarker(marker);
                    return;
                }
            }
        }
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        try {
            super.init(site, input);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setPartName(input.getName());
    }

    protected void initializeFileChangeListener() {
        fileChangeListener = new IFileChangeListener() {

            public void deleted(IFile file) {
                IFile current = ((IFileEditorInput) getEditorInput()).getFile();
                if (current.equals(file)) {
                    Display display = getSite().getShell().getDisplay();
                    display.asyncExec(new Runnable() {

                        public void run() {
                            getSite().getPage().closeEditor(XMLEditor.this, false);
                        }
                    });
                }
            }

            public void moved(IFile source, final IFile destination) {
            }
        };
        BPELUIPlugin.INSTANCE.getResourceChangeListener().addListener(fileChangeListener);
    }

    /**
	 * Installs the refactoring listener
	 */
    protected void initializeRefactoringListener() {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        postBuildRefactoringListener = new IResourceChangeListener() {

            public void resourceChanged(IResourceChangeEvent event) {
                IFile newFile = ((FileEditorInput) getEditorInput()).getFile();
                final IResourceDelta bpelFileDelta = event.getDelta().findMember(newFile.getFullPath());
                if (bpelFileDelta != null && (bpelFileDelta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
                    getSite().getShell().getDisplay().syncExec(new Runnable() {

                        public void run() {
                            doRevertToSaved(null);
                        }
                    });
                }
            }
        };
        workspace.addResourceChangeListener(postBuildRefactoringListener, IResourceChangeEvent.POST_BUILD);
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    protected void loadModel() {
        Document structuredDocument = null;
        try {
            IDocument doc = fTextEditor.getDocumentProvider().getDocument(getEditorInput());
            if (doc instanceof IStructuredDocument) {
                IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForEdit(doc);
                if (model == null) {
                    model = StructuredModelManager.getModelManager().getModelForEdit((IStructuredDocument) doc);
                }
                if (model != null) {
                    structuredDocument = ((IDOMModel) model).getDocument();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        HashMap<String, Document> loadOptions = null;
        if (structuredDocument != null) {
            loadOptions = new HashMap<String, Document>(1);
            loadOptions.put("DOMDocument", structuredDocument);
        }
        BPELEditModelClient editModelClient = new BPELEditModelClient(this, ((IFileEditorInput) getEditorInput()).getFile(), this, loadOptions);
        fDesignViewer.setEditModelClient(editModelClient);
        getEditDomain().setCommandStack(editModelClient.getCommandStack());
        Resource bpelResource = editModelClient.getPrimaryResourceInfo().getResource();
        IFile file = getFileInput();
        BPELReader reader = new BPELReader();
        reader.read(bpelResource, file, fDesignViewer.getResourceSet());
        process = reader.getProcess();
        if (getEditDomain() != null) {
            ((BPELEditDomain) getEditDomain()).setProcess(getProcess());
        }
        extensionsResource = reader.getExtensionsResource();
        extensionMap = reader.getExtensionMap();
        modelListenerAdapter = new ModelListenerAdapter();
        modelListenerAdapter.setExtensionMap(extensionMap);
    }

    public void modelDeleted(ResourceInfo resourceInfo) {
        if (!isDirty()) {
            getSite().getPage().closeEditor(this, false);
        }
    }

    public void modelDirtyStateChanged(ResourceInfo resourceInfo) {
        firePropertyChange(PROP_DIRTY);
    }

    public void modelLocationChanged(ResourceInfo resourceInfo, IFile movedToFile) {
    }

    public void modelReloaded(ResourceInfo resourceInfo) {
        Resource bpelResource = fDesignViewer.getEditModelClient().getPrimaryResourceInfo().getResource();
        IFile file = getFileInput();
        BPELReader reader = new BPELReader();
        reader.read(bpelResource, file, fDesignViewer.getResourceSet());
        process = reader.getProcess();
        if (getEditDomain() != null) {
            ((BPELEditDomain) getEditDomain()).setProcess(getProcess());
        }
        extensionMap = reader.getExtensionMap();
        modelListenerAdapter.setExtensionMap(fDesignViewer.getExtensionMap());
        fDesignViewer.getGraphicalViewer().setContents(getProcess());
        updateMarkersHard();
    }

    protected void updateMarkersHard() {
        for (EObject obj : fMarkers2EObject.values()) {
            obj.eNotify(fMarkersStale);
        }
        fMarkers2EObject.clear();
        IMarker[] markers = null;
        IFile file = getFileInput();
        Resource resource = getProcess().eResource();
        try {
            markers = file.findMarkers(null, true, IResource.DEPTH_ZERO);
        } catch (CoreException ex) {
            BPELUIPlugin.log(ex);
            return;
        }
        for (IMarker m : markers) {
            String href = null;
            EObject target = null;
            try {
                href = (String) m.getAttribute("address.model");
                if (href == null) {
                    continue;
                }
                target = resource.getEObject(href);
            } catch (CoreException ex) {
                continue;
            }
            if (target == null) {
                continue;
            }
            fMarkers2EObject.put(m.getId(), target);
            target.eNotify(new NotificationImpl(AdapterNotification.NOTIFICATION_MARKER_ADDED, null, m));
        }
    }

    @Override
    protected void pageChange(int newPageIndex) {
        currentPage = newPageIndex;
        super.pageChange(newPageIndex);
    }

    /**
	 * Sets the EditDomain for this EditorPart.
	 * @param ed the domain
	 */
    protected void setEditDomain(DefaultEditDomain ed) {
        this.editDomain = ed;
    }

    protected void showPropertiesView() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
        try {
            page.showView(IBPELUIConstants.PROPERTY_VIEW_ID);
        } catch (PartInitException e) {
            BPELUIPlugin.log(e);
        }
    }

    protected void showBPElementsView(Process process) {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
        if (page != null) {
            IViewPart view = page.findView("bpmetrics.views.BpelElementView");
            if (view instanceof BPElementViewImpl) ((BPElementViewImpl) view).setContent(process);
        }
    }

    protected void showBPStartWSView(Process process) {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
        if (page != null) {
            IViewPart view = page.findView("bpmetrics.views.BPStartWSView");
            if (view instanceof BPStartWSView) ((BPStartWSView) view).setContent(process);
        }
    }

    /**
	 * The editor part name should be the same as the one appearing in the logical view.
	 */
    protected void updateTitle() {
        setPartName(getProcess().getName());
    }

    public void modelMarkersChanged(ResourceInfo resourceInfo, IMarkerDelta[] markerDelta) {
        Resource resource = resourceInfo.getResource();
        for (IMarkerDelta delta : markerDelta) {
            String href = (String) delta.getAttribute("address.model");
            if (href == null) {
                continue;
            }
            EObject target = null;
            switch(delta.getKind()) {
                case IResourceDelta.ADDED:
                    target = resource.getEObject(href);
                    if (target != null) {
                        fMarkers2EObject.put(delta.getId(), target);
                        target.eNotify(new NotificationImpl(AdapterNotification.NOTIFICATION_MARKER_ADDED, null, delta.getMarker()));
                    }
                    break;
                case IResourceDelta.CHANGED:
                    target = fMarkers2EObject.remove(delta.getId());
                    if (target != null) {
                        target.eNotify(new NotificationImpl(AdapterNotification.NOTIFICATION_MARKER_CHANGED, delta.getMarker(), null));
                    }
                    break;
                case IResourceDelta.REMOVED:
                    target = fMarkers2EObject.remove(delta.getId());
                    if (target != null) {
                        target.eNotify(new NotificationImpl(AdapterNotification.NOTIFICATION_MARKER_DELETED, delta.getMarker(), null));
                    }
                    break;
            }
        }
    }

    @Override
    public boolean isDirty() {
        return fDesignViewer.isDirty();
    }

    @Override
    public IEditorPart getActiveEditor() {
        return super.getActiveEditor();
    }
}
