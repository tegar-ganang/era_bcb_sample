package com.safi.workshop.part;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.Viewport;
import org.eclipse.emf.common.ui.URIEditorInput;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.Tool;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerPreferences;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.gmf.runtime.common.core.util.Log;
import org.eclipse.gmf.runtime.common.core.util.Trace;
import org.eclipse.gmf.runtime.diagram.core.preferences.PreferencesHint;
import org.eclipse.gmf.runtime.diagram.core.util.ViewUtil;
import org.eclipse.gmf.runtime.diagram.ui.actions.ActionIds;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramRootEditPart;
import org.eclipse.gmf.runtime.diagram.ui.internal.DiagramUIDebugOptions;
import org.eclipse.gmf.runtime.diagram.ui.internal.DiagramUIPlugin;
import org.eclipse.gmf.runtime.diagram.ui.internal.DiagramUIStatusCodes;
import org.eclipse.gmf.runtime.diagram.ui.internal.actions.ToggleRouterAction;
import org.eclipse.gmf.runtime.diagram.ui.internal.editparts.DiagramRootTreeEditPart;
import org.eclipse.gmf.runtime.diagram.ui.internal.l10n.DiagramUIPluginImages;
import org.eclipse.gmf.runtime.diagram.ui.internal.parts.PaletteToolTransferDragSourceListener;
import org.eclipse.gmf.runtime.diagram.ui.internal.properties.WorkspaceViewerProperties;
import org.eclipse.gmf.runtime.diagram.ui.l10n.DiagramUIMessages;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditDomain;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.document.IDiagramDocument;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.document.IDocument;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.document.IDocumentProvider;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.parts.DiagramDocumentEditor;
import org.eclipse.gmf.runtime.diagram.ui.tools.ConnectionCreationTool;
import org.eclipse.gmf.runtime.diagram.ui.tools.CreationTool;
import org.eclipse.gmf.runtime.emf.core.resources.GMFResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import com.safi.core.saflet.Saflet;
import com.safi.server.saflet.mbean.DebugRemoteControl;
import com.safi.workshop.SafiNavigator;
import com.safi.workshop.edit.parts.HandlerEditPart;
import com.safi.workshop.part.AsteriskDiagramEditingDomainFactory.AsteriskDiagramEditingDomain;
import com.safi.workshop.util.SafletPersistenceManager;

/**
 * @generated
 */
public class AsteriskDiagramEditor extends DiagramDocumentEditor {

    /**
   * @generated
   */
    public static final String ID = "com.safi.workshop.part.AsteriskDiagramEditorID";

    /**
   * @generated
   */
    public static final String CONTEXT_ID = "com.safi.workshop.ui.diagramContext";

    /**
   * @generated NOT
   */
    private boolean dirty;

    private boolean debug;

    private WeakReference<PaletteRoot> currentPaletteRoot;

    private DebugRemoteControl control;

    /**
   * @generated
   */
    public AsteriskDiagramEditor() {
        super(true);
    }

    public void setBypassTransactionRecorder(boolean bypassTransactionRecorder) {
        TransactionalEditingDomain editingDomain = getEditingDomain();
        if (editingDomain instanceof AsteriskDiagramEditingDomainFactory.AsteriskDiagramEditingDomain) {
            ((AsteriskDiagramEditingDomainFactory.AsteriskDiagramEditingDomain) editingDomain).setBypassTransactionRecorder(bypassTransactionRecorder);
        }
    }

    @Override
    public Object getAdapter(Class type) {
        if (type == IContentOutlinePage.class) {
            TreeViewer viewer = new TreeViewer();
            viewer.setRootEditPart(new DiagramRootTreeEditPart());
            return new SafiDiagramOutlinePage(viewer);
        }
        return super.getAdapter(type);
    }

    @Override
    protected void initializeActionRegistry() {
        super.initializeActionRegistry();
        ActionRegistry registry = getActionRegistry();
    }

    /**
   * @generated
   */
    @Override
    protected String getContextID() {
        return CONTEXT_ID;
    }

    /**
   * @generated NOT
   */
    @Override
    protected PaletteRoot createPaletteRoot(PaletteRoot existingPaletteRoot) {
        PaletteRoot root = super.createPaletteRoot(existingPaletteRoot);
        new AsteriskPaletteFactory().fillPalette(root);
        boolean isFirst = true;
        for (Object child : root.getChildren()) {
            if (child instanceof PaletteDrawer) {
                ((PaletteDrawer) child).setInitialState(isFirst ? PaletteDrawer.INITIAL_STATE_OPEN : PaletteDrawer.INITIAL_STATE_CLOSED);
                isFirst = false;
            }
        }
        return root;
    }

    @Override
    public PreferenceStore getWorkspaceViewerPreferenceStore() {
        if (workspaceViewerPreferenceStore != null) {
            return workspaceViewerPreferenceStore;
        } else {
            IPath path = DiagramUIPlugin.getInstance().getStateLocation();
            String id = ViewUtil.getIdStr(getDiagram());
            String fileName = path.toString() + "/" + id;
            java.io.File file = new File(fileName);
            workspaceViewerPreferenceStore = new PreferenceStore(fileName);
            if (file.exists()) {
                try {
                    workspaceViewerPreferenceStore.load();
                } catch (Exception e) {
                    addDefaultPreferences();
                }
            } else {
                addDefaultPreferences();
            }
            return workspaceViewerPreferenceStore;
        }
    }

    @Override
    protected void configurePaletteViewer() {
        super.configurePaletteViewer();
    }

    @Override
    protected int getInitialPaletteSize() {
        return 300;
    }

    @Override
    protected PaletteViewerProvider createPaletteViewerProvider() {
        currentPaletteRoot = new WeakReference<PaletteRoot>(createPaletteRoot(null));
        getEditDomain().setPaletteRoot(currentPaletteRoot.get());
        return new PaletteViewerProvider(getEditDomain()) {

            @Override
            public PaletteViewer createPaletteViewer(Composite parent) {
                PaletteViewer pViewer = new PaletteViewer();
                PaletteViewerPreferences prefs = pViewer.getPaletteViewerPreferences();
                prefs.setAutoCollapseSetting(PaletteViewerPreferences.COLLAPSE_ALWAYS);
                prefs.setLayoutSetting(PaletteViewerPreferences.LAYOUT_COLUMNS);
                pViewer.createControl(parent);
                configurePaletteViewer(pViewer);
                hookPaletteViewer(pViewer);
                return pViewer;
            }

            @Override
            protected void hookPaletteViewer(PaletteViewer viewer) {
                super.hookPaletteViewer(viewer);
            }

            /**
       * Override to provide the additional behavior for the tools. Will intialize with a
       * PaletteEditPartFactory that has a TrackDragger that understand how to handle the
       * mouseDoubleClick event for shape creation tools. Also will initialize the palette
       * with a defaultTool that is the SelectToolEx that undestands how to handle the
       * enter key which will result in the creation of the shape also.
       */
            @Override
            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
                viewer.getKeyHandler().setParent(getPaletteKeyHandler());
                viewer.getControl().addMouseListener(getPaletteMouseListener());
                viewer.addDragSourceListener(new PaletteToolTransferDragSourceListener(viewer));
            }

            /** Key Saflet for the palette */
            KeyHandler paletteKeyHandler = null;

            /** Mouse listener for the palette */
            MouseListener paletteMouseListener = null;

            /**
       * @return Palette Key Saflet for the palette
       */
            private KeyHandler getPaletteKeyHandler() {
                if (paletteKeyHandler == null) {
                    paletteKeyHandler = new KeyHandler() {

                        /**
             * Processes a <i>key released </i> event. This method is called by the Tool
             * whenever a key is released, and the Tool is in the proper state. Override
             * to support pressing the enter key to create a shape or connection (between
             * two selected shapes)
             * 
             * @param event
             *          the KeyEvent
             * @return <code>true</code> if KeyEvent was handled in some way
             */
                        @Override
                        public boolean keyReleased(KeyEvent event) {
                            if (event.keyCode == SWT.Selection) {
                                Tool tool = getPaletteViewer().getActiveTool().createTool();
                                if (tool instanceof CreationTool || tool instanceof ConnectionCreationTool) {
                                    tool.keyUp(event, getDiagramGraphicalViewer());
                                    getPaletteViewer().setActiveTool(null);
                                    return true;
                                }
                            }
                            return super.keyReleased(event);
                        }
                    };
                }
                return paletteKeyHandler;
            }

            /**
       * @return Palette Mouse listener for the palette
       */
            private MouseListener getPaletteMouseListener() {
                if (paletteMouseListener == null) {
                    paletteMouseListener = new MouseListener() {

                        /**
             * Flag to indicate that the current active tool should be cleared after a
             * mouse double-click event.
             */
                        private boolean clearActiveTool = false;

                        /**
             * Override to support double-clicking a palette tool entry to create a shape
             * or connection (between two selected shapes).
             * 
             * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
             */
                        public void mouseDoubleClick(MouseEvent e) {
                            Tool tool = getPaletteViewer().getActiveTool().createTool();
                            if (tool instanceof CreationTool || tool instanceof ConnectionCreationTool) {
                                tool.setViewer(getDiagramGraphicalViewer());
                                tool.setEditDomain(getDiagramGraphicalViewer().getEditDomain());
                                tool.mouseDoubleClick(e, getDiagramGraphicalViewer());
                                clearActiveTool = true;
                            }
                        }

                        public void mouseDown(MouseEvent e) {
                        }

                        public void mouseUp(MouseEvent e) {
                            if (clearActiveTool) {
                                getPaletteViewer().setActiveTool(null);
                                clearActiveTool = false;
                            }
                        }
                    };
                }
                return paletteMouseListener;
            }
        };
    }

    /**
   * Helper method to returns the PaletteViewer from the page.
   * 
   * @return the palette viewer
   */
    private PaletteViewer getPaletteViewer() {
        return getEditDomain().getPaletteViewer();
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
    }

    /**
   * @generated
   */
    @Override
    protected PreferencesHint getPreferencesHint() {
        return AsteriskDiagramEditorPlugin.DIAGRAM_PREFERENCES_HINT;
    }

    /**
   * @generated
   */
    @Override
    public String getContributorId() {
        return AsteriskDiagramEditorPlugin.ID;
    }

    /**
   * @generated
   */
    @Override
    protected IDocumentProvider getDocumentProvider(IEditorInput input) {
        if (input instanceof URIEditorInput) {
            return AsteriskDiagramEditorPlugin.getInstance().getDocumentProvider();
        }
        return super.getDocumentProvider(input);
    }

    /**
   * @generated
   */
    @Override
    public TransactionalEditingDomain getEditingDomain() {
        IDocument document = getEditorInput() != null ? getDocumentProvider().getDocument(getEditorInput()) : null;
        if (document instanceof IDiagramDocument) {
            return ((IDiagramDocument) document).getEditingDomain();
        }
        return super.getEditingDomain();
    }

    /**
   * @generated not
   */
    @Override
    protected void setDocumentProvider(IEditorInput input) {
        if (input instanceof URIEditorInput) {
            setDocumentProvider(AsteriskDiagramEditorPlugin.getDefault().getDocumentProvider());
        } else if (input instanceof FileEditorInput) {
            FileEditorInput fInput = (FileEditorInput) input;
            String inputName = input.getName();
            if (inputName != null && inputName.endsWith(".saflet")) {
                org.eclipse.emf.common.util.URI euri = org.eclipse.emf.common.util.URI.createURI(fInput.getURI().toString());
                URIEditorInput uInput = new URIEditorInput(euri);
                this.setInput(uInput);
            }
        } else {
            super.setDocumentProvider(input);
        }
    }

    @Override
    protected boolean shouldAddUndoContext(IUndoableOperation operation) {
        return true;
    }

    @Override
    public void setInput(IEditorInput input) {
        IEditorInput defaultInput = null;
        if (input instanceof FileEditorInput) {
            FileEditorInput fInput = (FileEditorInput) input;
            String inputName = input.getName();
            if (inputName != null && inputName.endsWith(".saflet")) {
                org.eclipse.emf.common.util.URI euri = org.eclipse.emf.common.util.URI.createURI(fInput.getURI().toString());
                URIEditorInput uInput = new URIEditorInput(euri);
                defaultInput = uInput;
            }
        } else {
            defaultInput = input;
        }
        super.setInput(defaultInput);
        if (getEditingDomain() != null) setDebug(SafiWorkshopEditorUtil.hasDebugFile(getEditingDomain().getResourceSet()));
        File file = new File(input.getName());
        this.setPartName(file.getName());
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isDebug() {
        return debug;
    }

    /**
   * @generated NOT
   */
    public void setDirty() {
        if (debug) return;
        if (super.isDirty()) return;
        dirty = true;
        firePropertyChange(IEditorPart.PROP_DIRTY);
    }

    /**
   * @generated NOT
   */
    @Override
    public boolean isDirty() {
        return !debug && (super.isDirty() || dirty);
    }

    /**
   * @generated NOT
   */
    @Override
    protected void editorSaved() {
        dirty = false;
        try {
            EList<Resource> resources = getEditingDomain().getResourceSet().getResources();
            if (resources != null && !resources.isEmpty()) {
                GMFResource gmfResource = null;
                ResourceSet set = getEditingDomain().getResourceSet();
                for (Resource r : set.getResources()) {
                    if (r instanceof GMFResource && ("saflet".equalsIgnoreCase(r.getURI().fileExtension()))) {
                        gmfResource = (GMFResource) r;
                        break;
                    }
                }
                if (gmfResource != null) {
                    Saflet handler = (Saflet) gmfResource.getContents().get(0);
                    if (handler != null) {
                        IFile file = WorkspaceSynchronizer.getFile(gmfResource);
                        file.setPersistentProperty(SafletPersistenceManager.MODIFIED_KEY, String.valueOf(System.currentTimeMillis()));
                        file.setPersistentProperty(SafletPersistenceManager.SAFLET_NAME_KEY, handler.getName());
                        SafiNavigator safiNavigator = SafiWorkshopEditorUtil.getSafiNavigator();
                        if (safiNavigator != null) safiNavigator.refresh();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public org.eclipse.core.resources.IResource getCurrentResource() {
        try {
            EList<Resource> resources = getEditingDomain().getResourceSet().getResources();
            if (resources != null && !resources.isEmpty()) {
                GMFResource gmfResource = null;
                ResourceSet set = getEditingDomain().getResourceSet();
                for (Resource r : set.getResources()) {
                    if (r instanceof GMFResource && ("saflet".equalsIgnoreCase(r.getURI().fileExtension()))) {
                        gmfResource = (GMFResource) r;
                        break;
                    }
                }
                if (gmfResource != null) {
                    Saflet handler = (Saflet) gmfResource.getContents().get(0);
                    if (handler != null) {
                        return WorkspaceSynchronizer.getFile(gmfResource);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
   * @generated NOT
   */
    @Override
    protected void handleExceptionOnSave(CoreException exception, IProgressMonitor progressMonitor) {
        dirty = true;
        super.handleExceptionOnSave(exception, progressMonitor);
    }

    /**
   * @generated NOT
   */
    @Override
    protected void performSave(boolean overwrite, IProgressMonitor progressMonitor) {
        dirty = false;
        super.performSave(overwrite, progressMonitor);
        final IEditorInput input = getEditorInput();
        if (input instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput) input).getFile();
            try {
                file.setPersistentProperty(SafletPersistenceManager.MODIFIED_KEY, String.valueOf(System.currentTimeMillis()));
            } catch (CoreException e) {
                e.printStackTrace();
                MessageDialog.openError(getSite().getShell(), "Save Error", "Couldn't set persistent property: " + e.getLocalizedMessage());
            }
        }
    }

    @Override
    protected void performSaveAs(IProgressMonitor progressMonitor) {
        super.performSaveAs(progressMonitor);
        Shell shell = getSite().getShell();
        final IEditorInput input = getEditorInput();
        IDocumentProvider provider = getDocumentProvider();
        final IEditorInput newInput;
        if (input instanceof IURIEditorInput && !(input instanceof IFileEditorInput)) {
            FileDialog dialog = new FileDialog(shell, SWT.SAVE);
            IPath oldPath = URIUtil.toPath(((IURIEditorInput) input).getURI());
            if (oldPath != null) {
                dialog.setFileName(oldPath.lastSegment());
                dialog.setFilterPath(oldPath.toOSString());
            }
            String path = dialog.open();
            if (path == null) {
                if (progressMonitor != null) progressMonitor.setCanceled(true);
                return;
            }
            final File localFile = new File(path);
            if (localFile.exists()) {
                MessageDialog overwriteDialog = new MessageDialog(shell, "File Exists", null, MessageFormat.format("File {0} exists, would you like to overwrite?", path), MessageDialog.WARNING, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 1);
                if (overwriteDialog.open() != Window.OK) {
                    if (progressMonitor != null) {
                        progressMonitor.setCanceled(true);
                        return;
                    }
                }
            }
            IFileStore fileStore;
            try {
                fileStore = EFS.getStore(localFile.toURI());
            } catch (CoreException ex) {
                ex.printStackTrace();
                AsteriskDiagramEditorPlugin.getDefault().logError("Couldn't store file", ex);
                String title = "Save As Error";
                String msg = MessageFormat.format("Couldn't save file: {0}", ex.getMessage());
                MessageDialog.openError(shell, title, msg);
                return;
            }
            IFile file = getWorkspaceFile(fileStore);
            if (file != null) newInput = new URIEditorInput(URI.createFileURI(localFile.getAbsolutePath())); else newInput = new FileStoreEditorInput(fileStore);
        } else {
            SaveAsDialog dialog = new SaveAsDialog(shell);
            String original = null;
            IFile originalFile = null;
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            if (input instanceof IFileEditorInput) {
                originalFile = ((IFileEditorInput) input).getFile();
                original = originalFile.getProjectRelativePath().toPortableString();
            } else {
                URI uri = null;
                if (input instanceof URIEditorInput && (uri = ((URIEditorInput) input).getURI()).isFile()) {
                    original = uri.toFileString();
                    IPath path = Path.fromOSString(uri.toFileString());
                    originalFile = workspace.getRoot().getFile(path);
                    if (originalFile != null && originalFile.exists()) {
                        original = originalFile.getProject().getLocation().toPortableString() + '/' + originalFile.getName();
                    }
                }
            }
            if (original != null) dialog.setOriginalFile(originalFile);
            dialog.create();
            if (provider.isDeleted(input) && original != null) {
                String message = MessageFormat.format("Warning the original file {0} has been deleted", new Object[] { original });
                dialog.setErrorMessage(null);
                dialog.setMessage(message, IMessageProvider.WARNING);
            }
            if (dialog.open() == Window.CANCEL) {
                if (progressMonitor != null) progressMonitor.setCanceled(true);
                return;
            }
            IPath filePath = dialog.getResult();
            if (filePath == null) {
                if (progressMonitor != null) progressMonitor.setCanceled(true);
                return;
            }
            IFile file = workspace.getRoot().getFile(filePath);
            try {
                String uriStr = file.getProject().getLocationURI().getPath();
                String fullPath = uriStr + '/' + file.getName();
                File tf = new File(fullPath);
                if (!tf.exists()) {
                    tf.createNewFile();
                }
                IFileSystem fileSystem = EFS.getLocalFileSystem();
                IFileStore originalStore = fileSystem.getStore(originalFile.getFullPath());
                IFileStore saveFileStore = fileSystem.getStore(file.getRawLocation());
                originalStore.copy(saveFileStore, EFS.OVERWRITE, null);
                IFileInfo info = EFS.createFileInfo();
                long modifiedTime = System.currentTimeMillis();
                info.setLastModified(modifiedTime);
                saveFileStore.putInfo(info, EFS.SET_LAST_MODIFIED, null);
                SafletPersistenceManager.getInstance().renameSaflet(file, file.getName());
                file.setPersistentProperty(SafletPersistenceManager.MODIFIED_KEY, String.valueOf(modifiedTime));
            } catch (Exception e) {
                e.printStackTrace();
                String title = "Save As Error";
                String msg = MessageFormat.format("Couldn't save file: {0}", e.getMessage());
                MessageDialog.openError(shell, title, msg);
            }
            final org.eclipse.emf.common.util.URI fileURI = org.eclipse.emf.common.util.URI.createFileURI(file.getRawLocation().toString());
            newInput = new URIEditorInput(fileURI);
        }
        if (provider == null) {
            return;
        }
        boolean success = false;
        if (newInput instanceof URIEditorInput) {
            try {
                provider.aboutToChange(newInput);
                provider.connect(newInput);
                SafiWorkshopEditorUtil.openDiagram(((URIEditorInput) newInput).getURI(), false, true);
                success = true;
            } catch (CoreException x) {
                x.printStackTrace();
                final IStatus status = x.getStatus();
                if (status == null || status.getSeverity() != IStatus.CANCEL) {
                    String title = "Save As Error";
                    String msg = MessageFormat.format("Couldn't save file: {0}", x.getMessage());
                    MessageDialog.openError(shell, title, msg);
                }
            } finally {
                provider.changed(newInput);
                if (success) {
                    close(false);
                }
            }
        }
        if (progressMonitor != null) progressMonitor.setCanceled(!success);
    }

    private IFile getWorkspaceFile(IFileStore fileStore) {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IFile[] files = workspaceRoot.findFilesForLocation(URIUtil.toPath(fileStore.toURI()));
        if (files != null && files.length == 1) return files[0];
        return null;
    }

    @Override
    public int askUserSaveClose() {
        return super.askUserSaveClose();
    }

    @Override
    public void dispose() {
        if (getActionRegistry() != null) {
            IAction toggleAction = getActionRegistry().getAction(ActionIds.ACTION_TOGGLE_ROUTER);
            if (toggleAction != null) {
                getActionRegistry().removeAction(toggleAction);
                ((ToggleRouterAction) toggleAction).dispose();
            }
        }
        if (getActionManager() != null) getActionManager().clear();
        if (getActionRegistry() != null) getActionRegistry().dispose();
        HandlerEditPart handlerPart = getHandlerEditPart();
        if (debug) {
            if (control != null) try {
                control.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            control = null;
            Saflet handler = handlerPart.getHandlerModel();
            Object lock = handler.getSafletContext().getDebugLock();
            handler.getSafletContext().setDebugLock(null);
            if (lock != null) synchronized (lock) {
                lock.notifyAll();
            }
        }
        if (handlerPart != null) {
            handlerPart.deactivate();
            handlerPart.getChildren().clear();
            if (handlerPart.getEditingDomain() != null) {
            }
            handlerPart = null;
        }
        super.dispose();
        DiagramEditDomain domain = (DiagramEditDomain) getEditDomain();
        if (domain != null) {
            if (domain.getActionManager() != null) domain.getActionManager().clear();
            if (domain.getCommandStack() != null) domain.getCommandStack().dispose();
            if (domain.getDiagramCommandStack() != null) {
                domain.getDiagramCommandStack().dispose();
            }
            if (domain.getPaletteViewer() != null) {
                domain.getPaletteViewer().deselectAll();
            }
        }
        AsteriskDiagramEditingDomain dm = (AsteriskDiagramEditingDomain) getEditingDomain();
        if (dm != null) {
            dm.dispose();
        }
    }

    public HandlerEditPart getHandlerEditPart() {
        return (HandlerEditPart) getDiagramEditPart();
    }

    @Override
    protected void createGraphicalViewer(Composite parent) {
        super.createGraphicalViewer(parent);
    }

    @Override
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        if (debug && getPaletteViewer() != null && getPaletteViewer().getControl() != null) {
            getPaletteViewer().getControl().setVisible(false);
        }
    }

    @Override
    protected void addDefaultPreferences() {
        super.addDefaultPreferences();
        IPreferenceStore globalPreferenceStore = (IPreferenceStore) getPreferencesHint().getPreferenceStore();
        getWorkspaceViewerPreferenceStore().setDefault(WorkspaceViewerProperties.GRIDORDER, false);
    }

    @Override
    public void persistViewerSettings() {
        boolean gridOrder = getWorkspaceViewerPreferenceStore().getBoolean(WorkspaceViewerProperties.GRIDORDER);
        getWorkspaceViewerPreferenceStore().setValue(WorkspaceViewerProperties.GRIDORDER, !gridOrder);
        getWorkspaceViewerPreferenceStore().setValue(WorkspaceViewerProperties.GRIDORDER, gridOrder);
        super.persistViewerSettings();
    }

    public PaletteRoot getCurrentPaletteRoot() {
        return currentPaletteRoot == null ? null : currentPaletteRoot.get();
    }

    class SafiDiagramOutlinePage extends ContentOutlinePage implements IAdaptable {

        private PageBook pageBook;

        private Control outline;

        private Canvas overview;

        private IAction showOutlineAction, showOverviewAction;

        private boolean overviewInitialized;

        private SafiScrollableThumbnail thumbnail;

        private DisposeListener disposeListener;

        /**
     * @param viewer
     */
        public SafiDiagramOutlinePage(EditPartViewer viewer) {
            super(viewer);
        }

        @Override
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
            bars.updateActionBars();
            bars.getToolBarManager().markDirty();
        }

        /**
     * configures the outline viewer
     */
        protected void configureOutlineViewer() {
            getViewer().setEditDomain(getEditDomain());
            getViewer().setEditPartFactory(getOutlineViewEditPartFactory());
            getViewer().setKeyHandler(getKeyHandler());
            IToolBarManager tbm = this.getSite().getActionBars().getToolBarManager();
            showOutlineAction = new Action() {

                @Override
                public void run() {
                    showPage(ID_OUTLINE);
                }
            };
            showOutlineAction.setImageDescriptor(DiagramUIPluginImages.DESC_OUTLINE);
            showOutlineAction.setToolTipText(DiagramUIMessages.OutlineView_OutlineTipText);
            tbm.add(showOutlineAction);
            showOverviewAction = new Action() {

                @Override
                public void run() {
                    showPage(ID_OVERVIEW);
                }
            };
            showOverviewAction.setImageDescriptor(DiagramUIPluginImages.DESC_OVERVIEW);
            showOverviewAction.setToolTipText(DiagramUIMessages.OutlineView_OverviewTipText);
            tbm.add(showOverviewAction);
            showPage(getDefaultOutlineViewMode());
        }

        @Override
        public void createControl(Composite parent) {
            pageBook = new PageBook(parent, SWT.NONE);
            outline = getViewer().createControl(pageBook);
            overview = new Canvas(pageBook, SWT.NONE);
            pageBook.showPage(outline);
            configureOutlineViewer();
            hookOutlineViewer();
            initializeOutlineViewer();
        }

        @Override
        public void dispose() {
            unhookOutlineViewer();
            if (thumbnail != null) {
                thumbnail.deactivate();
            }
            this.overviewInitialized = false;
            super.dispose();
        }

        public Object getAdapter(Class type) {
            return null;
        }

        @Override
        public Control getControl() {
            return pageBook;
        }

        /**
     * hook the outline viewer
     */
        protected void hookOutlineViewer() {
            getSelectionSynchronizer().addViewer(getViewer());
        }

        /**
     * initialize the outline viewer
     */
        protected void initializeOutlineViewer() {
            try {
                TransactionUtil.getEditingDomain(getDiagram()).runExclusive(new Runnable() {

                    public void run() {
                        getViewer().setContents(getDiagram());
                    }
                });
            } catch (InterruptedException e) {
                Trace.catching(DiagramUIPlugin.getInstance(), DiagramUIDebugOptions.EXCEPTIONS_CATCHING, getClass(), "initializeOutlineViewer", e);
                Log.error(DiagramUIPlugin.getInstance(), DiagramUIStatusCodes.IGNORED_EXCEPTION_WARNING, "initializeOutlineViewer", e);
            }
        }

        /**
     * initialize the overview
     */
        protected void initializeOverview() {
            LightweightSystem lws = new LightweightSystem(overview);
            RootEditPart rep = getGraphicalViewer().getRootEditPart();
            DiagramRootEditPart root = (DiagramRootEditPart) rep;
            thumbnail = new SafiScrollableThumbnail((Viewport) root.getFigure());
            thumbnail.setSource(root.getLayer(LayerConstants.SCALABLE_LAYERS));
            lws.setContents(thumbnail);
            disposeListener = new DisposeListener() {

                public void widgetDisposed(DisposeEvent e) {
                    if (thumbnail != null) {
                        thumbnail.deactivate();
                        thumbnail = null;
                    }
                }
            };
            getEditor().addDisposeListener(disposeListener);
            this.overviewInitialized = true;
        }

        /**
     * show page with a specific ID, possibel values are ID_OUTLINE and ID_OVERVIEW
     * 
     * @param id
     */
        protected void showPage(int id) {
            if (id == ID_OUTLINE) {
                showOutlineAction.setChecked(true);
                showOverviewAction.setChecked(false);
                pageBook.showPage(outline);
                if (thumbnail != null) thumbnail.setVisible(false);
            } else if (id == ID_OVERVIEW) {
                if (!overviewInitialized) initializeOverview();
                showOutlineAction.setChecked(false);
                showOverviewAction.setChecked(true);
                pageBook.showPage(overview);
                thumbnail.setVisible(true);
            }
        }

        /**
     * unhook the outline viewer
     */
        protected void unhookOutlineViewer() {
            getSelectionSynchronizer().removeViewer(getViewer());
            if (disposeListener != null && getEditor() != null && !getEditor().isDisposed()) getEditor().removeDisposeListener(disposeListener);
        }

        /**
     * getter for the editor conrolo
     * 
     * @return <code>Control</code>
     */
        protected Control getEditor() {
            return getGraphicalViewer().getControl();
        }
    }

    @Override
    public void close(final boolean save) {
        super.close(save);
    }

    public void setDebugControl(DebugRemoteControl control) {
        this.control = control;
    }

    public DebugRemoteControl getDebugControl() {
        return control;
    }
}
