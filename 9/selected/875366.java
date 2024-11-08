package com.android.ide.eclipse.adt.internal.editors.layout.gle1;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.IconFactory;
import com.android.ide.eclipse.adt.internal.editors.layout.ExplodedRenderingHelper;
import com.android.ide.eclipse.adt.internal.editors.layout.IGraphicalLayoutEditor;
import com.android.ide.eclipse.adt.internal.editors.layout.LayoutEditor;
import com.android.ide.eclipse.adt.internal.editors.layout.LayoutReloadMonitor;
import com.android.ide.eclipse.adt.internal.editors.layout.ProjectCallback;
import com.android.ide.eclipse.adt.internal.editors.layout.UiElementPullParser;
import com.android.ide.eclipse.adt.internal.editors.layout.WidgetPullParser;
import com.android.ide.eclipse.adt.internal.editors.layout.LayoutEditor.UiEditorActions;
import com.android.ide.eclipse.adt.internal.editors.layout.LayoutReloadMonitor.ChangeFlags;
import com.android.ide.eclipse.adt.internal.editors.layout.LayoutReloadMonitor.ILayoutReloadListener;
import com.android.ide.eclipse.adt.internal.editors.layout.configuration.ConfigurationComposite;
import com.android.ide.eclipse.adt.internal.editors.layout.configuration.LayoutCreatorDialog;
import com.android.ide.eclipse.adt.internal.editors.layout.configuration.ConfigurationComposite.CustomToggle;
import com.android.ide.eclipse.adt.internal.editors.layout.configuration.ConfigurationComposite.IConfigListener;
import com.android.ide.eclipse.adt.internal.editors.layout.descriptors.ViewElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.ElementCreateCommand;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.UiElementEditPart;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.UiElementsEditPartFactory;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.UiElementsEditPartFactory.IOutlineProvider;
import com.android.ide.eclipse.adt.internal.editors.ui.tree.CopyCutAction;
import com.android.ide.eclipse.adt.internal.editors.ui.tree.PasteAction;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.manager.ProjectResources;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFile;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFolderType;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;
import com.android.ide.eclipse.adt.internal.sdk.LoadStatus;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData.LayoutBridge;
import com.android.ide.eclipse.adt.internal.sdk.Sdk.ITargetChangeListener;
import com.android.layoutlib.api.ILayoutBridge;
import com.android.layoutlib.api.ILayoutLog;
import com.android.layoutlib.api.ILayoutResult;
import com.android.layoutlib.api.IProjectCallback;
import com.android.layoutlib.api.IResourceValue;
import com.android.layoutlib.api.IXmlPullParser;
import com.android.layoutlib.api.ILayoutResult.ILayoutViewInfo;
import com.android.sdklib.IAndroidTarget;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.SelectionManager;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Graphical layout editor, based on GEF.
 * <p/>
 * To understand GEF: http://www.ibm.com/developerworks/opensource/library/os-gef/
 * <p/>
 * To understand Drag'n'drop: http://www.eclipse.org/articles/Article-Workbench-DND/drag_drop.html
 *
 * @since GLE1
 */
public class GraphicalLayoutEditor extends GraphicalEditorWithPalette implements IGraphicalLayoutEditor, IConfigListener, ILayoutReloadListener, IOutlineProvider {

    /** Reference to the layout editor */
    private final LayoutEditor mLayoutEditor;

    /** reference to the file being edited. */
    private IFile mEditedFile;

    private Clipboard mClipboard;

    private Composite mParent;

    private ConfigurationComposite mConfigComposite;

    private PaletteRoot mPaletteRoot;

    private Map<String, Map<String, IResourceValue>> mConfiguredFrameworkRes;

    private Map<String, Map<String, IResourceValue>> mConfiguredProjectRes;

    private ProjectCallback mProjectCallback;

    private ILayoutLog mLogger;

    private boolean mUseExplodeMode;

    private boolean mUseOutlineMode;

    private boolean mNeedsXmlReload = false;

    private boolean mNeedsRecompute = false;

    /** Listener to update the root node if the target of the file is changed because of a
     * SDK location change or a project target change */
    private ITargetChangeListener mTargetListener = new ITargetChangeListener() {

        public void onProjectTargetChange(IProject changedProject) {
            if (changedProject != null && changedProject.equals(getProject())) {
                updateEditor();
            }
        }

        public void onTargetLoaded(IAndroidTarget target) {
            IProject project = getProject();
            if (target != null && target.equals(Sdk.getCurrent().getTarget(project))) {
                updateEditor();
            }
        }

        public void onSdkLoaded() {
            Sdk currentSdk = Sdk.getCurrent();
            if (currentSdk != null) {
                IAndroidTarget target = currentSdk.getTarget(mEditedFile.getProject());
                if (target != null) {
                    mConfigComposite.onSdkLoaded(target);
                    onConfigurationChange();
                }
            }
        }

        private void updateEditor() {
            mLayoutEditor.commitPages(false);
            mConfiguredFrameworkRes = mConfiguredProjectRes = null;
            mProjectCallback = null;
            mLayoutEditor.initUiRootNode(true);
        }

        private IProject getProject() {
            return getLayoutEditor().getProject();
        }
    };

    private final Runnable mConditionalRecomputeRunnable = new Runnable() {

        public void run() {
            if (mLayoutEditor.isGraphicalEditorActive()) {
                recomputeLayout();
            } else {
                mNeedsRecompute = true;
            }
        }
    };

    private final Runnable mLocaleUpdaterFromUiRunnable = new Runnable() {

        public void run() {
            mConfigComposite.updateLocales();
        }
    };

    public GraphicalLayoutEditor(LayoutEditor layoutEditor) {
        mLayoutEditor = layoutEditor;
        setEditDomain(new DefaultEditDomain(this));
        setPartName("Layout");
        AdtPlugin.getDefault().addTargetListener(mTargetListener);
    }

    @Override
    public void createPartControl(Composite parent) {
        mParent = parent;
        GridLayout gl;
        mClipboard = new Clipboard(parent.getDisplay());
        parent.setLayout(gl = new GridLayout(1, false));
        gl.marginHeight = gl.marginWidth = 0;
        CustomToggle[] toggles = new CustomToggle[] { new CustomToggle("Explode", null, "Displays extra margins in the layout.") {

            @Override
            public void onSelected(boolean newState) {
                mUseExplodeMode = newState;
                recomputeLayout();
            }
        }, new CustomToggle("Outline", null, "Shows the outline of all views in the layout.") {

            @Override
            public void onSelected(boolean newState) {
                mUseOutlineMode = newState;
                recomputeLayout();
            }
        } };
        mConfigComposite = new ConfigurationComposite(this, toggles, parent, SWT.NONE);
        Composite editorParent = new Composite(parent, SWT.NONE);
        editorParent.setLayoutData(new GridData(GridData.FILL_BOTH));
        editorParent.setLayout(new FillLayout());
        super.createPartControl(editorParent);
    }

    @Override
    public void dispose() {
        if (mTargetListener != null) {
            AdtPlugin.getDefault().removeTargetListener(mTargetListener);
            mTargetListener = null;
        }
        LayoutReloadMonitor.getMonitor().removeListener(mEditedFile.getProject(), this);
        if (mClipboard != null) {
            mClipboard.dispose();
            mClipboard = null;
        }
        super.dispose();
    }

    /**
     * Returns the selection synchronizer object.
     * The synchronizer can be used to sync the selection of 2 or more EditPartViewers.
     * <p/>
     * This is changed from protected to public so that the outline can use it.
     *
     * @return the synchronizer
     */
    @Override
    public SelectionSynchronizer getSelectionSynchronizer() {
        return super.getSelectionSynchronizer();
    }

    /**
     * Returns the edit domain.
     * <p/>
     * This is changed from protected to public so that the outline can use it.
     *
     * @return the edit domain
     */
    @Override
    public DefaultEditDomain getEditDomain() {
        return super.getEditDomain();
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        mPaletteRoot = PaletteFactory.createPaletteRoot(mPaletteRoot, mLayoutEditor.getTargetData());
        return mPaletteRoot;
    }

    public Clipboard getClipboard() {
        return mClipboard;
    }

    /**
     * Save operation in the Graphical Layout Editor.
     * <p/>
     * In our workflow, the model is owned by the Structured XML Editor.
     * The graphical layout editor just displays it -- thus we don't really
     * save anything here.
     * <p/>
     * This must NOT call the parent editor part. At the contrary, the parent editor
     * part will call this *after* having done the actual save operation.
     * <p/>
     * The only action this editor must do is mark the undo command stack as
     * being no longer dirty.
     */
    @Override
    public void doSave(IProgressMonitor monitor) {
        getCommandStack().markSaveLocation();
        firePropertyChange(PROP_DIRTY);
    }

    @Override
    protected void configurePaletteViewer() {
        super.configurePaletteViewer();
        TemplateTransferDragSourceListener dragSource = new TemplateTransferDragSourceListener(getPaletteViewer());
        getPaletteViewer().addDragSourceListener(dragSource);
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new UiElementsEditPartFactory(mParent.getDisplay(), this));
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.addDropTargetListener(new DropListener(viewer));
    }

    class DropListener extends TemplateTransferDropTargetListener {

        public DropListener(EditPartViewer viewer) {
            super(viewer);
        }

        @Override
        protected CreationFactory getFactory(final Object template) {
            return new CreationFactory() {

                public Object getNewObject() {
                    return null;
                }

                public Object getObjectType() {
                    return template;
                }
            };
        }
    }

    @Override
    protected void initializeGraphicalViewer() {
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getModel());
        IEditorInput input = getEditorInput();
        if (input instanceof FileEditorInput) {
            FileEditorInput fileInput = (FileEditorInput) input;
            mEditedFile = fileInput.getFile();
            LayoutReloadMonitor.getMonitor().addListener(mEditedFile.getProject(), this);
        } else {
            mEditedFile = null;
            AdtPlugin.log(IStatus.ERROR, "Input is not of type FileEditorInput: %1$s", input.toString());
        }
    }

    @Override
    protected void setGraphicalViewer(GraphicalViewer viewer) {
        super.setGraphicalViewer(viewer);
        viewer.setContextMenu(createContextMenu(viewer));
    }

    /**
     * Used by LayoutEditor.UiEditorActions.selectUiNode to select a new UI Node
     * created by {@link ElementCreateCommand#execute()}.
     *
     * @param uiNodeModel The {@link UiElementNode} to select.
     */
    public void selectModel(UiElementNode uiNodeModel) {
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.getControl().forceFocus();
        Object editPart = viewer.getEditPartRegistry().get(uiNodeModel);
        if (editPart instanceof EditPart) {
            viewer.select((EditPart) editPart);
        }
    }

    public LayoutEditor getLayoutEditor() {
        return mLayoutEditor;
    }

    private MenuManager createContextMenu(GraphicalViewer viewer) {
        MenuManager menuManager = new MenuManager();
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new ActionMenuListener(viewer));
        return menuManager;
    }

    private class ActionMenuListener implements IMenuListener {

        private final GraphicalViewer mViewer;

        public ActionMenuListener(GraphicalViewer viewer) {
            mViewer = viewer;
        }

        /**
         * The menu is about to be shown. The menu manager has already been
         * requested to remove any existing menu item. This method gets the
         * tree selection and if it is of the appropriate type it re-creates
         * the necessary actions.
         */
        public void menuAboutToShow(IMenuManager manager) {
            ArrayList<UiElementNode> selected = new ArrayList<UiElementNode>();
            for (Object obj : mViewer.getSelectedEditParts()) {
                if (obj instanceof UiElementEditPart) {
                    UiElementEditPart part = (UiElementEditPart) obj;
                    UiElementNode uiNode = part.getUiNode();
                    if (uiNode != null) {
                        selected.add(uiNode);
                    }
                }
            }
            if (selected.size() > 0) {
                doCreateMenuAction(manager, mViewer, selected);
            }
        }
    }

    private void doCreateMenuAction(IMenuManager manager, final GraphicalViewer viewer, final ArrayList<UiElementNode> selected) {
        if (selected != null) {
            boolean hasXml = false;
            for (UiElementNode uiNode : selected) {
                if (uiNode.getXmlNode() != null) {
                    hasXml = true;
                    break;
                }
            }
            if (hasXml) {
                manager.add(new CopyCutAction(mLayoutEditor, getClipboard(), null, selected, true));
                manager.add(new CopyCutAction(mLayoutEditor, getClipboard(), null, selected, false));
                if (selected.size() <= 1) {
                    UiElementNode ui_root = selected.get(0).getUiRoot();
                    if (ui_root.getDescriptor().hasChildren() || !(ui_root.getUiParent() instanceof UiDocumentNode)) {
                        manager.add(new PasteAction(mLayoutEditor, getClipboard(), selected.get(0)));
                    }
                }
                manager.add(new Separator());
            }
        }
        IconFactory factory = IconFactory.getInstance();
        final UiEditorActions uiActions = mLayoutEditor.getUiEditorActions();
        if (selected == null || selected.size() <= 1) {
            manager.add(new Action("Add...", factory.getImageDescriptor("add")) {

                @Override
                public void run() {
                    UiElementNode node = selected != null && selected.size() > 0 ? selected.get(0) : null;
                    uiActions.doAdd(node, viewer.getControl().getShell());
                }
            });
        }
        if (selected != null) {
            manager.add(new Action("Remove", factory.getImageDescriptor("delete")) {

                @Override
                public void run() {
                    uiActions.doRemove(selected, viewer.getControl().getShell());
                }
            });
            manager.add(new Separator());
            manager.add(new Action("Up", factory.getImageDescriptor("up")) {

                @Override
                public void run() {
                    uiActions.doUp(selected);
                }
            });
            manager.add(new Action("Down", factory.getImageDescriptor("down")) {

                @Override
                public void run() {
                    uiActions.doDown(selected);
                }
            });
        }
    }

    /**
     * Opens and initialize the editor with a new file.
     * @param file the file being edited.
     */
    public void openFile(IFile file) {
        mEditedFile = file;
        mConfigComposite.openFile(mEditedFile);
    }

    /**
     * Resets the editor with a replacement file.
     * @param file the replacement file.
     */
    public void replaceFile(IFile file) {
        resetInput();
        mEditedFile = file;
        mConfigComposite.replaceFile(mEditedFile);
    }

    /**
     * Resets the editor with a replacement file coming from a config change in the config
     * selector.
     * @param file the replacement file.
     */
    public void changeFileOnNewConfig(IFile file) {
        resetInput();
        mEditedFile = file;
        mConfigComposite.changeFileOnNewConfig(mEditedFile);
    }

    public void onTargetChange() {
        resetInput();
        mConfigComposite.onTargetChange();
        onConfigurationChange();
    }

    public void onSdkChange() {
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null) {
            IAndroidTarget target = currentSdk.getTarget(mEditedFile.getProject());
            if (target != null) {
                mConfigComposite.onSdkLoaded(target);
                onConfigurationChange();
            }
        }
    }

    /**
     * Resets the editor's input and the viewer model.
     */
    private void resetInput() {
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getModel());
        IEditorInput input = mLayoutEditor.getEditorInput();
        setInput(input);
    }

    public Rectangle getBounds() {
        return mConfigComposite.getScreenBounds();
    }

    /**
     * Renders an Android View described by a {@link ViewElementDescriptor}.
     * <p/>This uses the <code>wrap_content</code> mode for both <code>layout_width</code> and
     * <code>layout_height</code>, and use the class name for the <code>text</code> attribute.
     * @param descriptor the descriptor for the class to render.
     * @return an ImageData containing the rendering or <code>null</code> if rendering failed.
     */
    public ImageData renderWidget(ViewElementDescriptor descriptor) {
        if (mEditedFile == null) {
            return null;
        }
        IAndroidTarget target = Sdk.getCurrent().getTarget(mEditedFile.getProject());
        if (target == null) {
            return null;
        }
        AndroidTargetData data = Sdk.getCurrent().getTargetData(target);
        if (data == null) {
            return null;
        }
        LayoutBridge bridge = data.getLayoutBridge();
        if (bridge.bridge != null) {
            ResourceManager resManager = ResourceManager.getInstance();
            ProjectCallback projectCallback = null;
            Map<String, Map<String, IResourceValue>> configuredProjectResources = null;
            if (mEditedFile != null) {
                ProjectResources projectRes = resManager.getProjectResources(mEditedFile.getProject());
                projectCallback = new ProjectCallback(bridge.classLoader, projectRes, mEditedFile.getProject());
                if (mConfiguredProjectRes == null && projectRes != null) {
                    projectRes.loadAll();
                    mConfiguredProjectRes = projectRes.getConfiguredResources(mConfigComposite.getCurrentConfig());
                }
                configuredProjectResources = mConfiguredProjectRes;
            } else {
                configuredProjectResources = new HashMap<String, Map<String, IResourceValue>>();
            }
            Map<String, Map<String, IResourceValue>> frameworkResources = getConfiguredFrameworkResources();
            if (configuredProjectResources != null && frameworkResources != null) {
                String theme = mConfigComposite.getTheme();
                if (theme != null) {
                    WidgetPullParser parser = new WidgetPullParser(descriptor);
                    ILayoutResult result = computeLayout(bridge, parser, null, 1, 1, true, 160, 160.f, 160.f, theme, mConfigComposite.isProjectTheme(), configuredProjectResources, frameworkResources, projectCallback, null);
                    if (result.getSuccess() == ILayoutResult.SUCCESS) {
                        BufferedImage largeImage = result.getImage();
                        int width = result.getRootView().getRight();
                        int height = result.getRootView().getBottom();
                        Raster raster = largeImage.getData(new java.awt.Rectangle(width, height));
                        int[] imageDataBuffer = ((DataBufferInt) raster.getDataBuffer()).getData();
                        ImageData imageData = new ImageData(width, height, 32, new PaletteData(0x00FF0000, 0x0000FF00, 0x000000FF));
                        imageData.setPixels(0, 0, imageDataBuffer.length, imageDataBuffer, 0);
                        return imageData;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Callback for XML model changed. Only update/recompute the layout if the editor is visible
     */
    public void onXmlModelChanged() {
        if (mLayoutEditor.isGraphicalEditorActive()) {
            doXmlReload(true);
            recomputeLayout();
        } else {
            mNeedsXmlReload = true;
        }
    }

    /**
     * Actually performs the XML reload
     * @see #onXmlModelChanged()
     */
    private void doXmlReload(boolean force) {
        if (force || mNeedsXmlReload) {
            GraphicalViewer viewer = getGraphicalViewer();
            SelectionManager selMan = viewer.getSelectionManager();
            ISelection selection = selMan.getSelection();
            try {
                viewer.setContents(getModel());
            } finally {
                selMan.setSelection(selection);
            }
            mNeedsXmlReload = false;
        }
    }

    public UiDocumentNode getModel() {
        return mLayoutEditor.getUiRootNode();
    }

    public void reloadPalette() {
        PaletteFactory.createPaletteRoot(mPaletteRoot, mLayoutEditor.getTargetData());
    }

    /**
     * Looks for a file matching the new {@link FolderConfiguration} and attempts to open it.
     * <p/>If there is no match, notify the user.
     */
    public void onConfigurationChange() {
        mConfiguredFrameworkRes = mConfiguredProjectRes = null;
        if (mEditedFile == null || mConfigComposite.getEditedConfig() == null) {
            return;
        }
        if (mLayoutEditor.isCreatingPages()) {
            recomputeLayout();
        } else {
            ProjectResources resources = ResourceManager.getInstance().getProjectResources(mEditedFile.getProject());
            ResourceFile match = null;
            if (resources != null) {
                match = resources.getMatchingFile(mEditedFile.getName(), ResourceFolderType.LAYOUT, mConfigComposite.getCurrentConfig());
            }
            if (match != null) {
                if (match.getFile().equals(mEditedFile) == false) {
                    try {
                        mLayoutEditor.setNewFileOnConfigChange(true);
                        IDE.openEditor(getSite().getWorkbenchWindow().getActivePage(), match.getFile().getIFile());
                        return;
                    } catch (PartInitException e) {
                    }
                }
                recomputeLayout();
            } else {
                FolderConfiguration currentConfig = mConfigComposite.getCurrentConfig();
                String message = String.format("No resources match the configuration\n \n\t%1$s\n \nChange the configuration or create:\n \n\tres/%2$s/%3$s\n \nYou can also click the 'Create' button above.", currentConfig.toDisplayString(), currentConfig.getFolderName(ResourceFolderType.LAYOUT, Sdk.getCurrent().getTarget(mEditedFile.getProject())), mEditedFile.getName());
                showErrorInEditor(message);
            }
        }
    }

    public void onThemeChange() {
        recomputeLayout();
    }

    public void onClippingChange() {
        recomputeLayout();
    }

    public void onCreate() {
        LayoutCreatorDialog dialog = new LayoutCreatorDialog(mParent.getShell(), mEditedFile.getName(), Sdk.getCurrent().getTarget(mEditedFile.getProject()), mConfigComposite.getCurrentConfig());
        if (dialog.open() == Dialog.OK) {
            final FolderConfiguration config = new FolderConfiguration();
            dialog.getConfiguration(config);
            createAlternateLayout(config);
        }
    }

    /**
     * Recomputes the layout with the help of layoutlib.
     */
    public void recomputeLayout() {
        doXmlReload(false);
        try {
            if (mEditedFile.exists() == false) {
                String message = String.format("Resource '%1$s' does not exist.", mEditedFile.getFullPath().toString());
                showErrorInEditor(message);
                return;
            }
            IProject iProject = mEditedFile.getProject();
            if (mEditedFile.isSynchronized(IResource.DEPTH_ZERO) == false) {
                String message = String.format("%1$s is out of sync. Please refresh.", mEditedFile.getName());
                showErrorInEditor(message);
                AdtPlugin.printErrorToConsole(iProject.getName(), message);
                return;
            }
            Sdk currentSdk = Sdk.getCurrent();
            if (currentSdk != null) {
                IAndroidTarget target = currentSdk.getTarget(mEditedFile.getProject());
                if (target == null) {
                    showErrorInEditor("The project target is not set.");
                    return;
                }
                AndroidTargetData data = currentSdk.getTargetData(target);
                if (data == null) {
                    LoadStatus targetLoadStatus = currentSdk.checkAndLoadTargetData(target, null);
                    switch(targetLoadStatus) {
                        case LOADING:
                            showErrorInEditor(String.format("The project target (%1$s) is still loading.\n%2$s will refresh automatically once the process is finished.", target.getName(), mEditedFile.getName()));
                            break;
                        case FAILED:
                        case LOADED:
                            showErrorInEditor(String.format("The project target (%s) was not properly loaded.", target.getName()));
                            break;
                    }
                    return;
                }
                UiDocumentNode model = getModel();
                if (model.getUiChildren().size() == 0) {
                    showErrorInEditor("No Xml content. Go to the Outline view and add nodes.");
                    return;
                }
                LayoutBridge bridge = data.getLayoutBridge();
                if (bridge.bridge != null) {
                    ResourceManager resManager = ResourceManager.getInstance();
                    ProjectResources projectRes = resManager.getProjectResources(iProject);
                    if (projectRes == null) {
                        return;
                    }
                    Map<String, Map<String, IResourceValue>> configuredProjectRes = getConfiguredProjectResources();
                    Map<String, Map<String, IResourceValue>> frameworkResources = getConfiguredFrameworkResources();
                    if (configuredProjectRes != null && frameworkResources != null) {
                        if (mProjectCallback == null) {
                            mProjectCallback = new ProjectCallback(bridge.classLoader, projectRes, iProject);
                        }
                        if (mLogger == null) {
                            mLogger = new ILayoutLog() {

                                public void error(String message) {
                                    AdtPlugin.printErrorToConsole(mEditedFile.getName(), message);
                                }

                                public void error(Throwable error) {
                                    String message = error.getMessage();
                                    if (message == null) {
                                        message = error.getClass().getName();
                                    }
                                    PrintStream ps = new PrintStream(AdtPlugin.getErrorStream());
                                    error.printStackTrace(ps);
                                }

                                public void warning(String message) {
                                    AdtPlugin.printToConsole(mEditedFile.getName(), message);
                                }
                            };
                        }
                        String theme = mConfigComposite.getTheme();
                        if (theme != null) {
                            Rectangle rect = getBounds();
                            int width = rect.width;
                            int height = rect.height;
                            if (mUseExplodeMode) {
                                List<UiElementNode> children = getModel().getUiChildren();
                                if (children.size() == 1) {
                                    ExplodedRenderingHelper helper = new ExplodedRenderingHelper(children.get(0).getXmlNode(), iProject);
                                    int paddingValue = ExplodedRenderingHelper.PADDING_VALUE * 2;
                                    width += helper.getWidthPadding() * paddingValue;
                                    height += helper.getHeightPadding() * paddingValue;
                                }
                            }
                            int density = mConfigComposite.getDensity().getDpiValue();
                            float xdpi = mConfigComposite.getXDpi();
                            float ydpi = mConfigComposite.getYDpi();
                            boolean isProjectTheme = mConfigComposite.isProjectTheme();
                            UiElementPullParser parser = new UiElementPullParser(getModel(), mUseExplodeMode, density, xdpi, iProject);
                            ILayoutResult result = computeLayout(bridge, parser, iProject, width, height, !mConfigComposite.getClipping(), density, xdpi, ydpi, theme, isProjectTheme, configuredProjectRes, frameworkResources, mProjectCallback, mLogger);
                            if (result.getSuccess() == ILayoutResult.SUCCESS) {
                                model.setEditData(result.getImage());
                                updateNodeWithBounds(result.getRootView());
                            } else {
                                String message = result.getErrorMessage();
                                resetNodeBounds(model);
                                if (message != null) {
                                    model.setEditData(message);
                                }
                            }
                            model.refreshUi();
                        }
                    }
                } else {
                    String message = null;
                    if (bridge.status == LoadStatus.LOADING) {
                        message = String.format("Eclipse is loading framework information and the Layout library from the SDK folder.\n%1$s will refresh automatically once the process is finished.", mEditedFile.getName());
                    } else {
                        message = String.format("Eclipse failed to load the framework information and the Layout library!");
                    }
                    showErrorInEditor(message);
                }
            } else {
                String message = String.format("Eclipse is loading the SDK.\n%1$s will refresh automatically once the process is finished.", mEditedFile.getName());
                showErrorInEditor(message);
            }
        } finally {
            mNeedsRecompute = false;
        }
    }

    private void showErrorInEditor(String message) {
        UiDocumentNode model = getModel();
        resetNodeBounds(model);
        if (message != null) {
            model.setEditData(message);
        }
        model.refreshUi();
    }

    private void resetNodeBounds(UiElementNode node) {
        node.setEditData(null);
        List<UiElementNode> children = node.getUiChildren();
        for (UiElementNode child : children) {
            resetNodeBounds(child);
        }
    }

    private void updateNodeWithBounds(ILayoutViewInfo r) {
        if (r != null) {
            Object viewKey = r.getViewKey();
            if (viewKey instanceof UiElementNode) {
                Rectangle bounds = new Rectangle(r.getLeft(), r.getTop(), r.getRight() - r.getLeft(), r.getBottom() - r.getTop());
                ((UiElementNode) viewKey).setEditData(bounds);
            }
            ILayoutViewInfo[] children = r.getChildren();
            if (children != null) {
                for (ILayoutViewInfo child : children) {
                    updateNodeWithBounds(child);
                }
            }
        }
    }

    public void reloadLayout(ChangeFlags flags) {
        boolean recompute = false;
        if (flags.rClass) {
            recompute = true;
            ProjectResources projectRes = getProjectResources();
            if (projectRes != null) {
                projectRes.resetDynamicIds();
            }
        }
        if (flags.localeList) {
            mParent.getDisplay().asyncExec(mLocaleUpdaterFromUiRunnable);
        }
        if (flags.resources) {
            recompute = true;
            mConfiguredProjectRes = null;
            IAndroidTarget target = Sdk.getCurrent().getTarget(mEditedFile.getProject());
            if (target != null) {
                AndroidTargetData data = Sdk.getCurrent().getTargetData(target);
                if (data != null) {
                    LayoutBridge bridge = data.getLayoutBridge();
                    if (bridge.bridge != null) {
                        bridge.bridge.clearCaches(mEditedFile.getProject());
                    }
                }
            }
        }
        if (flags.code) {
            if (mProjectCallback != null && mProjectCallback.isUsed()) {
                mProjectCallback = null;
                recompute = true;
            }
        }
        if (recompute) {
            mParent.getDisplay().asyncExec(mConditionalRecomputeRunnable);
        }
    }

    /**
     * Responds to a page change that made the Graphical editor page the activated page.
     */
    public void activated() {
        if (mNeedsRecompute || mNeedsXmlReload) {
            recomputeLayout();
        }
    }

    /**
     * Responds to a page change that made the Graphical editor page the deactivated page
     */
    public void deactivated() {
    }

    public Map<String, Map<String, IResourceValue>> getConfiguredFrameworkResources() {
        if (mConfiguredFrameworkRes == null) {
            ProjectResources frameworkRes = getFrameworkResources();
            if (frameworkRes == null) {
                AdtPlugin.log(IStatus.ERROR, "Failed to get ProjectResource for the framework");
            } else {
                mConfiguredFrameworkRes = frameworkRes.getConfiguredResources(mConfigComposite.getCurrentConfig());
            }
        }
        return mConfiguredFrameworkRes;
    }

    public Map<String, Map<String, IResourceValue>> getConfiguredProjectResources() {
        if (mConfiguredProjectRes == null) {
            ProjectResources project = getProjectResources();
            project.loadAll();
            mConfiguredProjectRes = project.getConfiguredResources(mConfigComposite.getCurrentConfig());
        }
        return mConfiguredProjectRes;
    }

    /**
     * Returns a {@link ProjectResources} for the framework resources.
     * @return the framework resources or null if not found.
     */
    public ProjectResources getFrameworkResources() {
        if (mEditedFile != null) {
            Sdk currentSdk = Sdk.getCurrent();
            if (currentSdk != null) {
                IAndroidTarget target = currentSdk.getTarget(mEditedFile.getProject());
                if (target != null) {
                    AndroidTargetData data = currentSdk.getTargetData(target);
                    if (data != null) {
                        return data.getFrameworkResources();
                    }
                }
            }
        }
        return null;
    }

    public ProjectResources getProjectResources() {
        if (mEditedFile != null) {
            ResourceManager manager = ResourceManager.getInstance();
            return manager.getProjectResources(mEditedFile.getProject());
        }
        return null;
    }

    /**
     * Creates a new layout file from the specified {@link FolderConfiguration}.
     */
    private void createAlternateLayout(final FolderConfiguration config) {
        new Job("Create Alternate Resource") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                String folderName = config.getFolderName(ResourceFolderType.LAYOUT, Sdk.getCurrent().getTarget(mEditedFile.getProject()));
                try {
                    IFolder res = (IFolder) mEditedFile.getParent().getParent();
                    String path = res.getLocation().toOSString();
                    File newLayoutFolder = new File(path + File.separator + folderName);
                    if (newLayoutFolder.isFile()) {
                        String message = String.format("File 'res/%1$s' is in the way!", folderName);
                        AdtPlugin.displayError("Layout Creation", message);
                        return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, message);
                    } else if (newLayoutFolder.exists() == false) {
                        newLayoutFolder.mkdir();
                    }
                    File newLayoutFile = new File(newLayoutFolder.getAbsolutePath() + File.separator + mEditedFile.getName());
                    newLayoutFile.createNewFile();
                    InputStream input = mEditedFile.getContents();
                    FileOutputStream fos = new FileOutputStream(newLayoutFile);
                    byte[] data = new byte[512];
                    int count;
                    while ((count = input.read(data)) != -1) {
                        fos.write(data, 0, count);
                    }
                    input.close();
                    fos.close();
                    res.refreshLocal(IResource.DEPTH_INFINITE, new IProgressMonitor() {

                        public void done() {
                            mParent.getDisplay().asyncExec(new Runnable() {

                                public void run() {
                                    onConfigurationChange();
                                }
                            });
                        }

                        public void beginTask(String name, int totalWork) {
                        }

                        public void internalWorked(double work) {
                        }

                        public boolean isCanceled() {
                            return false;
                        }

                        public void setCanceled(boolean value) {
                        }

                        public void setTaskName(String name) {
                        }

                        public void subTask(String name) {
                        }

                        public void worked(int work) {
                        }
                    });
                } catch (IOException e2) {
                    String message = String.format("Failed to create File 'res/%1$s/%2$s' : %3$s", folderName, mEditedFile.getName(), e2.getMessage());
                    AdtPlugin.displayError("Layout Creation", message);
                    return new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID, message, e2);
                } catch (CoreException e2) {
                    String message = String.format("Failed to create File 'res/%1$s/%2$s' : %3$s", folderName, mEditedFile.getName(), e2.getMessage());
                    AdtPlugin.displayError("Layout Creation", message);
                    return e2.getStatus();
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    /**
     * Computes a layout by calling the correct computeLayout method of ILayoutBridge based on
     * the implementation API level.
     */
    @SuppressWarnings("deprecation")
    private static ILayoutResult computeLayout(LayoutBridge bridge, IXmlPullParser layoutDescription, Object projectKey, int screenWidth, int screenHeight, boolean renderFullSize, int density, float xdpi, float ydpi, String themeName, boolean isProjectTheme, Map<String, Map<String, IResourceValue>> projectResources, Map<String, Map<String, IResourceValue>> frameworkResources, IProjectCallback projectCallback, ILayoutLog logger) {
        if (bridge.apiLevel >= ILayoutBridge.API_CURRENT) {
            return bridge.bridge.computeLayout(layoutDescription, projectKey, screenWidth, screenHeight, renderFullSize, density, xdpi, ydpi, themeName, isProjectTheme, projectResources, frameworkResources, projectCallback, logger);
        } else if (bridge.apiLevel == 3) {
            return bridge.bridge.computeLayout(layoutDescription, projectKey, screenWidth, screenHeight, density, xdpi, ydpi, themeName, isProjectTheme, projectResources, frameworkResources, projectCallback, logger);
        } else if (bridge.apiLevel == 2) {
            return bridge.bridge.computeLayout(layoutDescription, projectKey, screenWidth, screenHeight, themeName, isProjectTheme, projectResources, frameworkResources, projectCallback, logger);
        } else {
            if (isProjectTheme) {
                themeName = "*" + themeName;
            }
            return bridge.bridge.computeLayout(layoutDescription, projectKey, screenWidth, screenHeight, themeName, projectResources, frameworkResources, projectCallback, logger);
        }
    }

    public boolean hasOutline() {
        return mUseOutlineMode;
    }
}
