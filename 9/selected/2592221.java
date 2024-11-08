package org.spbu.pldoctoolkit.graph.diagram.infproduct.providers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.validation.model.IConstraintStatus;
import org.eclipse.gef.EditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;
import org.eclipse.gmf.runtime.emf.core.util.EMFCoreUtil;
import org.eclipse.gmf.runtime.emf.ui.providers.marker.AbstractModelMarkerNavigationProvider;
import org.eclipse.gmf.runtime.notation.View;
import org.spbu.pldoctoolkit.graph.diagram.infproduct.part.DrlModelDiagramEditorPlugin;
import org.spbu.pldoctoolkit.graph.diagram.infproduct.part.DrlModelDiagramEditorUtil;

/**
 * @generated
 */
public class DrlModelMarkerNavigationProvider extends AbstractModelMarkerNavigationProvider {

    /**
	 * @generated
	 */
    public static final String MARKER_TYPE = DrlModelDiagramEditorPlugin.ID + ".diagnostic";

    /**
	 * @generated
	 */
    protected void doGotoMarker(IMarker marker) {
        String elementId = marker.getAttribute(org.eclipse.gmf.runtime.common.core.resources.IMarker.ELEMENT_ID, null);
        if (elementId == null || !(getEditor() instanceof DiagramEditor)) {
            return;
        }
        DiagramEditor editor = (DiagramEditor) getEditor();
        Map editPartRegistry = editor.getDiagramGraphicalViewer().getEditPartRegistry();
        EObject targetView = editor.getDiagram().eResource().getEObject(elementId);
        if (targetView == null) {
            return;
        }
        EditPart targetEditPart = (EditPart) editPartRegistry.get(targetView);
        if (targetEditPart != null) {
            DrlModelDiagramEditorUtil.selectElementsInDiagram(editor, Arrays.asList(new EditPart[] { targetEditPart }));
        }
    }

    /**
	 * @generated
	 */
    public static void createMarkers(IFile diagramFile, IStatus validationStatus, DiagramEditPart diagramEditPart) {
        if (validationStatus.isOK()) return;
        final IStatus rootStatus = validationStatus;
        List allStatuses = new ArrayList();
        DrlModelDiagramEditorUtil.LazyElement2ViewMap element2ViewMap = new DrlModelDiagramEditorUtil.LazyElement2ViewMap(diagramEditPart.getDiagramView(), collectTargetElements(rootStatus, new HashSet(), allStatuses));
        for (Iterator it = allStatuses.iterator(); it.hasNext(); ) {
            IConstraintStatus nextStatus = (IConstraintStatus) it.next();
            View view = DrlModelDiagramEditorUtil.findView(diagramEditPart, nextStatus.getTarget(), element2ViewMap);
            IMarker newMarker = addMarker(diagramFile, view.eResource().getURIFragment(view), EMFCoreUtil.getQualifiedName(nextStatus.getTarget(), true), nextStatus.getMessage(), nextStatus.getSeverity());
            if (newMarker != null) {
                adjustMarker(newMarker, nextStatus);
            }
        }
    }

    /**
	 * @generated
	 */
    public static void createMarkers(IFile diagramFile, Diagnostic emfValidationStatus, DiagramEditPart diagramEditPart) {
        if (emfValidationStatus.getSeverity() == Diagnostic.OK) return;
        final Diagnostic rootStatus = emfValidationStatus;
        List allDiagnostics = new ArrayList();
        DrlModelDiagramEditorUtil.LazyElement2ViewMap element2ViewMap = new DrlModelDiagramEditorUtil.LazyElement2ViewMap(diagramEditPart.getDiagramView(), collectTargetElements(rootStatus, new HashSet(), allDiagnostics));
        for (Iterator it = emfValidationStatus.getChildren().iterator(); it.hasNext(); ) {
            Diagnostic nextDiagnostic = (Diagnostic) it.next();
            List data = nextDiagnostic.getData();
            if (data != null && !data.isEmpty() && data.get(0) instanceof EObject) {
                EObject element = (EObject) data.get(0);
                View view = DrlModelDiagramEditorUtil.findView(diagramEditPart, element, element2ViewMap);
                IMarker newMarker = addMarker(diagramFile, view.eResource().getURIFragment(view), EMFCoreUtil.getQualifiedName(element, true), nextDiagnostic.getMessage(), diagnosticToStatusSeverity(nextDiagnostic.getSeverity()));
                if (newMarker != null) {
                    adjustMarker(newMarker, emfValidationStatus);
                }
            }
        }
    }

    /**
	 * @generated
	 */
    public static void deleteMarkers(IResource resource) {
        try {
            resource.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_ZERO);
        } catch (CoreException e) {
            DrlModelDiagramEditorPlugin.getInstance().logError("Failed to delete validation markers", e);
        }
    }

    /**
	 * @generated
	 */
    public static void adjustMarker(IMarker marker, Diagnostic sourceDiagnostic) {
        assert marker != null && sourceDiagnostic != null;
    }

    /**
	 * @generated
	 */
    public static void adjustMarker(IMarker marker, IConstraintStatus sourceStatus) {
        assert marker != null && sourceStatus != null;
    }

    /**
	 * @generated
	 */
    private static IMarker addMarker(IFile file, String elementId, String location, String message, int statusSeverity) {
        IMarker marker = null;
        try {
            marker = file.createMarker(MARKER_TYPE);
            marker.setAttribute(IMarker.MESSAGE, message);
            marker.setAttribute(IMarker.LOCATION, location);
            marker.setAttribute(org.eclipse.gmf.runtime.common.ui.resources.IMarker.ELEMENT_ID, elementId);
            int markerSeverity = IMarker.SEVERITY_INFO;
            if (statusSeverity == IStatus.WARNING) {
                markerSeverity = IMarker.SEVERITY_WARNING;
            } else if (statusSeverity == IStatus.ERROR || statusSeverity == IStatus.CANCEL) {
                markerSeverity = IMarker.SEVERITY_ERROR;
            }
            marker.setAttribute(IMarker.SEVERITY, markerSeverity);
        } catch (CoreException e) {
            DrlModelDiagramEditorPlugin.getInstance().logError("Failed to create validation marker", e);
        }
        return marker;
    }

    /**
	 * @generated
	 */
    private static int diagnosticToStatusSeverity(int diagnosticSeverity) {
        if (diagnosticSeverity == Diagnostic.OK) {
            return IStatus.OK;
        } else if (diagnosticSeverity == Diagnostic.INFO) {
            return IStatus.INFO;
        } else if (diagnosticSeverity == Diagnostic.WARNING) {
            return IStatus.WARNING;
        } else if (diagnosticSeverity == Diagnostic.ERROR || diagnosticSeverity == Diagnostic.CANCEL) {
            return IStatus.ERROR;
        }
        return IStatus.INFO;
    }

    /**
	 * @generated
	 */
    private static Set collectTargetElements(IStatus status, Set targetElementCollector, List allConstraintStatuses) {
        if (status instanceof IConstraintStatus) {
            targetElementCollector.add(((IConstraintStatus) status).getTarget());
            allConstraintStatuses.add(status);
        }
        if (status.isMultiStatus()) {
            IStatus[] children = status.getChildren();
            for (int i = 0; i < children.length; i++) {
                collectTargetElements(children[i], targetElementCollector, allConstraintStatuses);
            }
        }
        return targetElementCollector;
    }

    /**
	 * @generated
	 */
    private static Set collectTargetElements(Diagnostic diagnostic, Set targetElementCollector, List allDiagnostics) {
        List data = diagnostic.getData();
        EObject target = null;
        if (data != null && !data.isEmpty() && data.get(0) instanceof EObject) {
            target = (EObject) data.get(0);
            targetElementCollector.add(target);
            allDiagnostics.add(diagnostic);
        }
        if (diagnostic.getChildren() != null && !diagnostic.getChildren().isEmpty()) {
            for (Iterator it = diagnostic.getChildren().iterator(); it.hasNext(); ) {
                collectTargetElements((Diagnostic) it.next(), targetElementCollector, allDiagnostics);
            }
        }
        return targetElementCollector;
    }
}
