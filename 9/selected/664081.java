package net.sourceforge.dynamicgmf.diagram.navigator;

import java.util.Iterator;
import net.sourceforge.dynamicgmf.diagram.part.EcoreModelDocumentProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.ui.URIEditorInput;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.workspace.util.WorkspaceSynchronizer;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.document.IDiagramDocument;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.document.IDiagramDocumentProvider;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.navigator.ILinkHelper;
import org.eclipse.ui.part.FileEditorInput;

/**
 * @generated
 */
public class EcoreModelNavigatorLinkHelper implements ILinkHelper {

    /**
	 * @generated
	 */
    private static IEditorInput getEditorInput(Diagram diagram) {
        Resource diagramResource = diagram.eResource();
        for (Iterator it = diagramResource.getContents().iterator(); it.hasNext(); ) {
            EObject nextEObject = (EObject) it.next();
            if (nextEObject == diagram) {
                return new FileEditorInput(WorkspaceSynchronizer.getFile(diagramResource));
            }
            if (nextEObject instanceof Diagram) {
                break;
            }
        }
        URI uri = EcoreUtil.getURI(diagram);
        String editorName = uri.lastSegment() + "#" + diagram.eResource().getContents().indexOf(diagram);
        IEditorInput editorInput = new URIEditorInput(uri, editorName);
        return editorInput;
    }

    /**
	 * @generated NOT
	 */
    public IStructuredSelection findSelection(IEditorInput anInput) {
        IDiagramDocumentProvider documentProvider = anInput.getName().endsWith("xml_diagram") ? new EcoreModelDocumentProvider.XML() : new EcoreModelDocumentProvider();
        IDiagramDocument document = documentProvider.getDiagramDocument(anInput);
        if (document == null) {
            return StructuredSelection.EMPTY;
        }
        Diagram diagram = document.getDiagram();
        IFile file = WorkspaceSynchronizer.getFile(diagram.eResource());
        if (file != null) {
            EcoreModelNavigatorItem item = new EcoreModelNavigatorItem(diagram, file, false);
            return new StructuredSelection(item);
        }
        return StructuredSelection.EMPTY;
    }

    /**
	 * @generated
	 */
    public void activateEditor(IWorkbenchPage aPage, IStructuredSelection aSelection) {
        if (aSelection == null || aSelection.isEmpty()) {
            return;
        }
        if (false == aSelection.getFirstElement() instanceof EcoreModelAbstractNavigatorItem) {
            return;
        }
        EcoreModelAbstractNavigatorItem abstractNavigatorItem = (EcoreModelAbstractNavigatorItem) aSelection.getFirstElement();
        View navigatorView = null;
        if (abstractNavigatorItem instanceof EcoreModelNavigatorItem) {
            navigatorView = ((EcoreModelNavigatorItem) abstractNavigatorItem).getView();
        } else if (abstractNavigatorItem instanceof EcoreModelNavigatorGroup) {
            EcoreModelNavigatorGroup navigatorGroup = (EcoreModelNavigatorGroup) abstractNavigatorItem;
            if (navigatorGroup.getParent() instanceof EcoreModelNavigatorItem) {
                navigatorView = ((EcoreModelNavigatorItem) navigatorGroup.getParent()).getView();
            }
        }
        if (navigatorView == null) {
            return;
        }
        IEditorInput editorInput = getEditorInput(navigatorView.getDiagram());
        IEditorPart editor = aPage.findEditor(editorInput);
        if (editor == null) {
            return;
        }
        aPage.bringToTop(editor);
        if (editor instanceof DiagramEditor) {
            DiagramEditor diagramEditor = (DiagramEditor) editor;
            ResourceSet diagramEditorResourceSet = diagramEditor.getEditingDomain().getResourceSet();
            EObject selectedView = diagramEditorResourceSet.getEObject(EcoreUtil.getURI(navigatorView), true);
            if (selectedView == null) {
                return;
            }
            GraphicalViewer graphicalViewer = (GraphicalViewer) diagramEditor.getAdapter(GraphicalViewer.class);
            EditPart selectedEditPart = (EditPart) graphicalViewer.getEditPartRegistry().get(selectedView);
            if (selectedEditPart != null) {
                graphicalViewer.select(selectedEditPart);
            }
        }
    }
}