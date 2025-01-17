package com.abstratt.imageviewer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import com.abstratt.content.ContentSupport;
import com.abstratt.content.IContentProviderRegistry.IProviderDescription;

/**
 * A view that wraps a {@link GraphicalViewer}.
 */
public class GraphicalView extends ViewPart implements IResourceChangeListener, IPartListener2, ISelectionListener {

    public static final String VIEW_ID = "com.abstratt.imageviewer.GraphicalView";

    private Canvas canvas;

    private GraphicalViewer viewer;

    private String basePartName;

    private IFile selectedFile;

    private IProviderDescription providerDefinition;

    /**
	 * The constructor.
	 */
    public GraphicalView() {
    }

    /**
	 * This is a callback that wil l allow us to create the viewer and
	 * initialize it.
	 */
    @Override
    public void createPartControl(Composite parent) {
        basePartName = getPartName();
        canvas = new Canvas(parent, SWT.NONE);
        viewer = new GraphicalViewer(canvas);
        installResourceListener();
        installSelectionListener();
        installPartListener();
    }

    @Override
    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        getSite().getPage().removePartListener(this);
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
        super.dispose();
    }

    private void installPartListener() {
        getSite().getPage().addPartListener(this);
        final IWorkbenchPartReference activePartReference = getSite().getPage().getActivePartReference();
        if (activePartReference != null) reactToPartChange(activePartReference);
    }

    private void installResourceListener() {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    private void installSelectionListener() {
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) return;
        IStructuredSelection structured = (IStructuredSelection) selection;
        if (structured.size() != 1) return;
        Object selected = structured.getFirstElement();
        IFile file = (IFile) Platform.getAdapterManager().getAdapter(selected, IFile.class);
        reload(file);
    }

    public void partActivated(IWorkbenchPartReference partRef) {
        reactToPartChange(partRef);
    }

    public void partBroughtToTop(IWorkbenchPartReference partRef) {
        reactToPartChange(partRef);
    }

    public void partClosed(IWorkbenchPartReference partRef) {
    }

    public void partDeactivated(IWorkbenchPartReference partRef) {
    }

    public void partHidden(IWorkbenchPartReference partRef) {
    }

    public void partInputChanged(IWorkbenchPartReference partRef) {
        reactToPartChange(partRef);
    }

    public void partOpened(IWorkbenchPartReference partRef) {
        reactToPartChange(partRef);
    }

    public void partVisible(IWorkbenchPartReference partRef) {
        reactToPartChange(partRef);
    }

    private void reactToPartChange(IWorkbenchPartReference part) {
        if (!(part.getPart(false) instanceof IEditorPart)) return;
        IEditorPart editorPart = (IEditorPart) part.getPart(false);
        if (!getViewSite().getPage().isPartVisible(editorPart)) return;
        IGraphicalFileProvider graphicalSource = (IGraphicalFileProvider) editorPart.getAdapter(IGraphicalFileProvider.class);
        if (graphicalSource != null) selectedFile = graphicalSource.getGraphicalFile(); else selectedFile = null;
        if (selectedFile == null) {
            IFile asFile = (IFile) editorPart.getAdapter(IFile.class);
            if (asFile == null) asFile = (IFile) editorPart.getEditorInput().getAdapter(IFile.class);
            if (asFile == null) return;
            selectedFile = asFile;
        }
        requestUpdate();
    }

    private void reload(IFile file) {
        setPartName(basePartName);
        this.providerDefinition = null;
        if (file == null || !file.exists()) return;
        selectedFile = null;
        if (viewer.getContentProvider() != null) viewer.setInput(null);
        IContentDescription contentDescription = null;
        try {
            contentDescription = file.getContentDescription();
        } catch (CoreException e) {
            if (Platform.inDebugMode()) Activator.logUnexpected(null, e);
        }
        if (contentDescription == null || contentDescription.getContentType() == null) return;
        IProviderDescription providerDefinition = ContentSupport.getContentProviderRegistry().findContentProvider(contentDescription.getContentType(), IGraphicalContentProvider.class);
        if (providerDefinition == null) {
            return;
        }
        setPartName(basePartName + " - " + file.getName());
        selectedFile = file;
        this.providerDefinition = providerDefinition;
        IGraphicalContentProvider provider = (IGraphicalContentProvider) providerDefinition.getProvider();
        viewer.setContentProvider(provider);
        viewer.setInput(providerDefinition.read(file));
    }

    private void requestUpdate() {
        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                if (getSite() == null || !GraphicalView.this.getSite().getPage().isPartVisible(GraphicalView.this)) return;
                reload(selectedFile);
            }
        });
    }

    public void resourceChanged(IResourceChangeEvent event) {
        if (selectedFile == null || !selectedFile.exists()) return;
        if (event.getDelta() == null) return;
        IResourceDelta interestingChange = event.getDelta().findMember(selectedFile.getFullPath());
        if (interestingChange != null) requestUpdate();
    }

    /**
	 * Passing the focus request to the viewer's control.
	 */
    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    public IProviderDescription getContentProviderDescription() {
        return providerDefinition;
    }

    public IFile getSelectedFile() {
        return selectedFile;
    }
}
