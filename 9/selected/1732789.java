package de.mpiwg.vspace.maps.diagram.part;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.ui.URIEditorInput;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gmf.runtime.common.ui.services.marker.MarkerNavigationService;
import org.eclipse.gmf.runtime.diagram.core.preferences.PreferencesHint;
import org.eclipse.gmf.runtime.diagram.ui.actions.ActionIds;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.document.IDiagramDocument;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.document.IDocument;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.document.IDocumentProvider;
import org.eclipse.gmf.runtime.notation.Bounds;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.LayoutConstraint;
import org.eclipse.gmf.runtime.notation.NotationPackage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import de.mpiwg.vspace.common.project.ProjectObservable;
import de.mpiwg.vspace.common.project.ProjectManager;
import de.mpiwg.vspace.diagram.edit.parts.IVirtualSpaceEditPart;
import de.mpiwg.vspace.diagram.part.ExhibitionDiagramEditorPlugin;
import de.mpiwg.vspace.diagram.patch.MyDiagramDocumentEditor;
import de.mpiwg.vspace.diagram.providers.EditingDomainManager;
import de.mpiwg.vspace.metamodel.LinkedMapContainer;
import de.mpiwg.vspace.metamodel.OverviewMap;
import de.mpiwg.vspace.metamodel.Scene;
import de.mpiwg.vspace.util.PropertyHandler;
import de.mpiwg.vspace.util.PropertyHandlerRegistry;

/**
 * @generated NOT
 */
public class OverviewMapsDiagramEditor extends MyDiagramDocumentEditor implements IGotoMarker {

    /**
	 * @generated
	 */
    public static final String ID = "de.mpiwg.vspace.maps.diagram.part.OverviewMapsDiagramEditorID";

    /**
	 * @generated
	 */
    public static final String CONTEXT_ID = "de.mpiwg.vspace.maps.diagram.ui.diagramContext";

    /**
	 * @generated
	 */
    public OverviewMapsDiagramEditor() {
        super(true);
    }

    /**
	 * @generated
	 */
    protected String getContextID() {
        return CONTEXT_ID;
    }

    /**
	 * @generated
	 */
    protected PaletteRoot createPaletteRoot(PaletteRoot existingPaletteRoot) {
        PaletteRoot root = super.createPaletteRoot(existingPaletteRoot);
        new OverviewMapsPaletteFactory().fillPalette(root);
        return root;
    }

    /**
	 * @generated
	 */
    protected PreferencesHint getPreferencesHint() {
        return OverviewMapsDiagramEditorPlugin.DIAGRAM_PREFERENCES_HINT;
    }

    /**
	 * @generated NOT
	 */
    public String getContributorId() {
        return ExhibitionDiagramEditorPlugin.ID;
    }

    /**
	 * @generated
	 */
    public Object getAdapter(Class type) {
        if (type == IShowInTargetList.class) {
            return new IShowInTargetList() {

                public String[] getShowInTargetIds() {
                    return new String[] { ProjectExplorer.VIEW_ID };
                }
            };
        }
        return super.getAdapter(type);
    }

    /**
	 * @generated
	 */
    protected IDocumentProvider getDocumentProvider(IEditorInput input) {
        if (input instanceof IFileEditorInput || input instanceof URIEditorInput) {
            return OverviewMapsDiagramEditorPlugin.getInstance().getDocumentProvider();
        }
        return super.getDocumentProvider(input);
    }

    /**
	 * @generated
	 */
    public TransactionalEditingDomain getEditingDomain() {
        IDocument document = getEditorInput() != null && getDocumentProvider() != null ? getDocumentProvider().getDocument(getEditorInput()) : null;
        if (document instanceof IDiagramDocument) {
            return ((IDiagramDocument) document).getEditingDomain();
        }
        return super.getEditingDomain();
    }

    /**
	 * @generated
	 */
    protected void setDocumentProvider(IEditorInput input) {
        if (input instanceof IFileEditorInput || input instanceof URIEditorInput) {
            setDocumentProvider(OverviewMapsDiagramEditorPlugin.getInstance().getDocumentProvider());
        } else {
            super.setDocumentProvider(input);
        }
    }

    /**
	 * @generated
	 */
    public void gotoMarker(IMarker marker) {
        MarkerNavigationService.getInstance().gotoMarker(this, marker);
    }

    /**
	 * @generated NOT
	 */
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
	 * @generated
	 */
    public void doSaveAs() {
        performSaveAs(new NullProgressMonitor());
    }

    /**
	 * @generated
	 */
    protected void performSaveAs(IProgressMonitor progressMonitor) {
        Shell shell = getSite().getShell();
        IEditorInput input = getEditorInput();
        SaveAsDialog dialog = new SaveAsDialog(shell);
        IFile original = input instanceof IFileEditorInput ? ((IFileEditorInput) input).getFile() : null;
        if (original != null) {
            dialog.setOriginalFile(original);
        }
        dialog.create();
        IDocumentProvider provider = getDocumentProvider();
        if (provider == null) {
            return;
        }
        if (provider.isDeleted(input) && original != null) {
            String message = NLS.bind(Messages.OverviewMapsDiagramEditor_SavingDeletedFile, original.getName());
            dialog.setErrorMessage(null);
            dialog.setMessage(message, IMessageProvider.WARNING);
        }
        if (dialog.open() == Window.CANCEL) {
            if (progressMonitor != null) {
                progressMonitor.setCanceled(true);
            }
            return;
        }
        IPath filePath = dialog.getResult();
        if (filePath == null) {
            if (progressMonitor != null) {
                progressMonitor.setCanceled(true);
            }
            return;
        }
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IFile file = workspaceRoot.getFile(filePath);
        final IEditorInput newInput = new FileEditorInput(file);
        IEditorMatchingStrategy matchingStrategy = getEditorDescriptor().getEditorMatchingStrategy();
        IEditorReference[] editorRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
        for (int i = 0; i < editorRefs.length; i++) {
            if (matchingStrategy.matches(editorRefs[i], newInput)) {
                MessageDialog.openWarning(shell, Messages.OverviewMapsDiagramEditor_SaveAsErrorTitle, Messages.OverviewMapsDiagramEditor_SaveAsErrorMessage);
                return;
            }
        }
        boolean success = false;
        try {
            provider.aboutToChange(newInput);
            getDocumentProvider(newInput).saveDocument(progressMonitor, newInput, getDocumentProvider().getDocument(getEditorInput()), true);
            success = true;
        } catch (CoreException x) {
            IStatus status = x.getStatus();
            if (status == null || status.getSeverity() != IStatus.CANCEL) {
                ErrorDialog.openError(shell, Messages.OverviewMapsDiagramEditor_SaveErrorTitle, Messages.OverviewMapsDiagramEditor_SaveErrorMessage, x.getStatus());
            }
        } finally {
            provider.changed(newInput);
            if (success) {
                setInput(newInput);
            }
        }
        if (progressMonitor != null) {
            progressMonitor.setCanceled(!success);
        }
    }

    /**
	 * @generated
	 */
    public ShowInContext getShowInContext() {
        return new ShowInContext(getEditorInput(), getNavigatorSelection());
    }

    /**
	 * @generated NOT
	 */
    private ISelection getNavigatorSelection() {
        return StructuredSelection.EMPTY;
    }

    /**
	 * @generated
	 */
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        DiagramEditorContextMenuProvider provider = new DiagramEditorContextMenuProvider(this, getDiagramGraphicalViewer());
        getDiagramGraphicalViewer().setContextMenu(provider);
        getSite().registerContextMenu(ActionIds.DIAGRAM_EDITOR_CONTEXT_MENU, provider, getDiagramGraphicalViewer());
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        String workspace = null;
        if ((input != null) && (input instanceof URIEditorInput)) {
            if (!ProjectObservable.INSTANCE.isProjectOpen()) {
                ProjectObservable.INSTANCE.projectOpened(((URIEditorInput) input).getURI(), ExhibitionDiagramEditorPlugin.EDITINGDOMAIN_ID);
            }
        }
        super.init(site, input);
    }

    @Override
    public void doSetInput(IEditorInput input, boolean releaseEditorContents) throws CoreException {
        URIEditorInput uriInput = (URIEditorInput) input;
        if (!uriInput.exists()) {
            MessageBox messageBox = new MessageBox(PlatformUI.getWorkbench().getDisplay().getActiveShell(), SWT.OK | SWT.ICON_ERROR);
            PropertyHandler handler = PropertyHandlerRegistry.REGISTRY.getPropertyHandler(OverviewMapsDiagramEditorPlugin.ID, OverviewMapsDiagramEditorPlugin.PROPERTIES_FILE);
            messageBox.setText(handler.getProperty("_msgbox_title_init_failed"));
            messageBox.setMessage(handler.getProperty("_msgbox_text_init_failed"));
            messageBox.open();
            return;
        }
        super.doSetInput(input, releaseEditorContents);
    }

    @Override
    public Diagram getDiagram() {
        Diagram diagram = super.getDiagram();
        if (diagram != null) {
            TransactionalEditingDomain editingDomain = EditingDomainManager.INSTANCE.getEditingDomain();
            diagram = (Diagram) EcoreUtil.resolve(diagram, editingDomain.getResourceSet());
        }
        return diagram;
    }

    @Override
    public void doSave(IProgressMonitor progressMonitor) {
        setDefaultLayoutData();
        super.doSave(progressMonitor);
    }

    /**
	 * This method is a workaround. It is needed to get rid of the default size values (-1). The default size values
	 * are used as long as the user does not change the size of a link, exhibition module reference or scene by hand.
	 * This method adds the current dimension values to the layout constraints of a Node.
	 */
    protected void setDefaultLayoutData() {
        EObject eobject = this.getDiagram().getElement();
        if (!(eobject instanceof LinkedMapContainer)) return;
        LinkedMapContainer container = (LinkedMapContainer) eobject;
        List<EObject> graphicalObjects = new ArrayList<EObject>();
        List<OverviewMap> maps = container.getOverviewMaps();
        if (maps != null) {
            graphicalObjects.addAll(maps);
            for (OverviewMap m : maps) {
                if (m.getLinks() != null) graphicalObjects.addAll(m.getLinks());
            }
        }
        for (EObject o : graphicalObjects) {
            EditPart oEditPart = this.getDiagramEditPart().findEditPart(this.getDiagramEditPart(), o);
            if (oEditPart instanceof IVirtualSpaceEditPart) {
                IVirtualSpaceEditPart vsed = (IVirtualSpaceEditPart) oEditPart;
                Rectangle rec = vsed.getPrimaryShapeFigure().getBounds();
                Object nodeObject = oEditPart.getModel();
                if (nodeObject instanceof org.eclipse.gmf.runtime.notation.Node) {
                    org.eclipse.gmf.runtime.notation.Node node = (org.eclipse.gmf.runtime.notation.Node) nodeObject;
                    LayoutConstraint constraint = node.getLayoutConstraint();
                    if (constraint instanceof Bounds) {
                        if (((Bounds) constraint).getHeight() == -1) {
                            Command cmd = SetCommand.create(getEditingDomain(), constraint, NotationPackage.Literals.SIZE__HEIGHT, rec.height);
                            getEditingDomain().getCommandStack().execute(cmd);
                        }
                        if (((Bounds) constraint).getWidth() == -1) {
                            Command cmd = SetCommand.create(getEditingDomain(), constraint, NotationPackage.Literals.SIZE__WIDTH, rec.width);
                            getEditingDomain().getCommandStack().execute(cmd);
                        }
                        if (((Bounds) constraint).getX() == -1) {
                            Command cmd = SetCommand.create(getEditingDomain(), constraint, NotationPackage.Literals.LOCATION__X, rec.x);
                            getEditingDomain().getCommandStack().execute(cmd);
                        }
                        if (((Bounds) constraint).getY() == -1) {
                            Command cmd = SetCommand.create(getEditingDomain(), constraint, NotationPackage.Literals.LOCATION__Y, rec.y);
                            getEditingDomain().getCommandStack().execute(cmd);
                        }
                    }
                }
            }
        }
    }
}
