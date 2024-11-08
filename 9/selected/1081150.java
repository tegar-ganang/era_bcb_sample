package de.mpiwg.vspace.diagram.part;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.ui.URIEditorInput;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gmf.runtime.diagram.core.preferences.PreferencesHint;
import org.eclipse.gmf.runtime.diagram.ui.actions.ActionIds;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.render.editparts.RenderedDiagramRootEditPart;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.document.IDiagramDocument;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.document.IDocument;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.document.IDocumentProvider;
import org.eclipse.gmf.runtime.emf.core.util.EMFCoreUtil;
import org.eclipse.gmf.runtime.notation.Bounds;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.LayoutConstraint;
import org.eclipse.gmf.runtime.notation.Node;
import org.eclipse.gmf.runtime.notation.NotationPackage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import de.mpiwg.vspace.common.project.ProjectObservable;
import de.mpiwg.vspace.diagram.edit.parts.ExhibitionModuleReferenceEditPart;
import de.mpiwg.vspace.diagram.edit.parts.IVirtualSpaceEditPart;
import de.mpiwg.vspace.diagram.modulecategories.ui.ModuleCategoriesView;
import de.mpiwg.vspace.diagram.patch.MyDiagramDocumentEditor;
import de.mpiwg.vspace.diagram.providers.EditingDomainManager;
import de.mpiwg.vspace.diagram.rcpviews.validation.ValidationResult;
import de.mpiwg.vspace.diagram.rcpviews.validation.ValidationResultView;
import de.mpiwg.vspace.diagram.util.PropertyHandler;
import de.mpiwg.vspace.extension.ExceptionHandlingService;
import de.mpiwg.vspace.metamodel.Exhibition;
import de.mpiwg.vspace.metamodel.ExhibitionModule;
import de.mpiwg.vspace.metamodel.ExhibitionModuleReference;
import de.mpiwg.vspace.metamodel.Scene;

/**
 * @generated NOT
 */
public class ExhibitionDiagramEditor extends MyDiagramDocumentEditor {

    /**
	 * @generated
	 */
    public static final String ID = "de.mpiwg.vspace.diagram.part.ExhibitionDiagramEditorID";

    /**
	 * @generated
	 */
    public static final String CONTEXT_ID = "de.mpiwg.vspace.diagram.ui.diagramContext";

    private ZoomManager zm = null;

    /**
	 * @generated
	 */
    public ExhibitionDiagramEditor() {
        super(true);
    }

    /**
	 * @generated
	 */
    protected String getContextID() {
        return CONTEXT_ID;
    }

    /**
	 * Handle editor input changed, but don't ask for reload, when editor input
	 * has changed outside the editor.
	 */
    @Override
    protected void handleEditorInputChanged() {
        final IDocumentProvider provider = getDocumentProvider();
        final IEditorInput input = getEditorInput();
        if (!provider.isDeleted(input)) {
            try {
                provider.synchronize(input);
            } catch (CoreException e) {
                ExceptionHandlingService.INSTANCE.handleException(e);
            }
        } else super.handleEditorInputChanged();
    }

    /**
	 * @generated
	 */
    protected PaletteRoot createPaletteRoot(PaletteRoot existingPaletteRoot) {
        PaletteRoot root = super.createPaletteRoot(existingPaletteRoot);
        new ExhibitionPaletteFactory().fillPalette(root);
        return root;
    }

    /**
	 * @generated
	 */
    protected PreferencesHint getPreferencesHint() {
        return ExhibitionDiagramEditorPlugin.DIAGRAM_PREFERENCES_HINT;
    }

    /**
	 * @generated
	 */
    public String getContributorId() {
        return ExhibitionDiagramEditorPlugin.ID;
    }

    /**
	 * @generated
	 */
    protected IDocumentProvider getDocumentProvider(IEditorInput input) {
        if (input instanceof URIEditorInput) {
            return ExhibitionDiagramEditorPlugin.getInstance().getDocumentProvider();
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
        if (input instanceof URIEditorInput) {
            setDocumentProvider(ExhibitionDiagramEditorPlugin.getInstance().getDocumentProvider());
        } else {
            super.setDocumentProvider(input);
        }
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
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (part instanceof ValidationResultView) {
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection structuredSelection = (IStructuredSelection) selection;
                Object selectedObject = structuredSelection.getFirstElement();
                if (selectedObject instanceof ValidationResult) {
                    if (!(((ValidationResult) selectedObject).marker.getElement() instanceof EObject)) return;
                    EObject eObject = (EObject) ((ValidationResult) selectedObject).marker.getElement();
                    EObject resolvedProxy = eObject;
                    if (eObject.eIsProxy()) resolvedProxy = EMFCoreUtil.resolve(getEditingDomain(), eObject);
                    EditPart editPartToSelect = getDiagramEditPart().findEditPart(getDiagramEditPart(), resolvedProxy);
                    if (editPartToSelect != null) {
                        editPartToSelect.getSelected();
                        List<EditPart> editPartsToSelect = new ArrayList<EditPart>();
                        editPartsToSelect.add(editPartToSelect);
                        ExhibitionDiagramEditorUtil.selectElementsInDiagram(this, editPartsToSelect);
                    }
                }
            }
        } else {
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection structuredSelection = (IStructuredSelection) selection;
                Object selectedObject = structuredSelection.getFirstElement();
                if (selectedObject instanceof EObject) {
                    EObject eObject = (EObject) selectedObject;
                    if (eObject instanceof ExhibitionModule) {
                        Exhibition currentEx = EditingDomainManager.INSTANCE.getExhibition();
                        List<ExhibitionModuleReference> refs = currentEx.getExhibitionModuleReferences();
                        for (ExhibitionModuleReference r : refs) {
                            if (r.getExhibitionModule() == eObject) {
                                eObject = r;
                                break;
                            }
                        }
                    }
                    EObject resolvedProxy = eObject;
                    if (eObject.eIsProxy()) resolvedProxy = EMFCoreUtil.resolve(getEditingDomain(), eObject);
                    EditPart editPartToSelect = getDiagramEditPart().findEditPart(getDiagramEditPart(), resolvedProxy);
                    if (editPartToSelect != null) {
                        editPartToSelect.getSelected();
                        List<EditPart> editPartsToSelect = new ArrayList<EditPart>();
                        editPartsToSelect.add(editPartToSelect);
                        ExhibitionDiagramEditorUtil.selectElementsInDiagram(this, editPartsToSelect);
                    }
                }
            }
        }
        if (part instanceof ModuleCategoriesView) {
            IStructuredSelection sel = (IStructuredSelection) selection;
            DiagramEditPart editPart = getDiagramEditPart();
            List<?> children = editPart.getChildren();
            for (Object child : children) {
                if (child instanceof ExhibitionModuleReferenceEditPart) {
                    ExhibitionModuleReferenceEditPart refEditPart = (ExhibitionModuleReferenceEditPart) child;
                    Object o = refEditPart.getModel();
                    if (o instanceof Node) {
                        ExhibitionModuleReference reference = (ExhibitionModuleReference) ((Node) o).getElement();
                        ExhibitionModule model = reference.getExhibitionModule();
                    }
                }
            }
        }
        super.selectionChanged(part, selection);
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        ExhibitionDiagramEditorPlugin.getInstance().logInfo("init " + site + " with " + input);
        if ((input != null) && (input instanceof URIEditorInput)) {
            if (!ProjectObservable.INSTANCE.isProjectOpen()) {
                ProjectObservable.INSTANCE.projectOpened(((URIEditorInput) input).getURI(), ExhibitionDiagramEditorPlugin.EDITINGDOMAIN_ID);
                ProjectObservable.INSTANCE.setMainEditorInput(input);
            }
            input = new VSpaceEditorInput(((URIEditorInput) input).getURI());
        }
        super.init(site, input);
    }

    @Override
    public void doSetInput(IEditorInput input, boolean releaseEditorContents) throws CoreException {
        URIEditorInput uriInput = (URIEditorInput) input;
        if (!uriInput.exists()) {
            MessageBox messageBox = new MessageBox(PlatformUI.getWorkbench().getDisplay().getActiveShell(), SWT.OK | SWT.ICON_ERROR);
            messageBox.setText(PropertyHandler.getInstance().getProperty("_msgbox_title_init_failed"));
            messageBox.setMessage(PropertyHandler.getInstance().getProperty("_msgbox_text_init_failed"));
            messageBox.open();
            return;
        }
        super.doSetInput(input, releaseEditorContents);
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
        if (!(eobject instanceof Exhibition)) return;
        Exhibition exhibition = (Exhibition) eobject;
        List<EObject> graphicalObjects = new ArrayList<EObject>();
        List<Scene> scenes = exhibition.getScenes();
        if (scenes != null) {
            graphicalObjects.addAll(scenes);
            for (Scene scene : scenes) {
                if (scene.getLinks() != null) graphicalObjects.addAll(scene.getLinks());
            }
        }
        if (exhibition.getExhibitionModuleReferences() != null) graphicalObjects.addAll(exhibition.getExhibitionModuleReferences());
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

    @Override
    public boolean isDirty() {
        return super.isDirty();
    }

    @Override
    public Object getAdapter(Class type) {
        if (type == ZoomManager.class) {
            if (zm == null) {
                RenderedDiagramRootEditPart r = (RenderedDiagramRootEditPart) getDiagramEditPart().getParent();
                zm = r.getZoomManager();
                zm.setZoomAnimationStyle(ZoomManager.ANIMATE_NEVER);
            }
            return zm;
        }
        return super.getAdapter(type);
    }

    @Override
    public DiagramEditPart getDiagramEditPart() {
        if (getDiagramGraphicalViewer() == null) return null;
        return super.getDiagramEditPart();
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
}
