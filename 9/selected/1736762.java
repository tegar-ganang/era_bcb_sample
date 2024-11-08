package hub.sam.mof.simulator.editor.diagram.providers;

import hub.sam.mof.simulator.editor.diagram.part.M3ActionsDiagramEditorPlugin;
import hub.sam.mof.simulator.editor.diagram.part.M3ActionsDiagramEditorUtil;
import java.util.Arrays;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.EditPart;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;
import org.eclipse.gmf.runtime.emf.ui.providers.marker.AbstractModelMarkerNavigationProvider;

/**
 * @generated
 */
public class M3ActionsMarkerNavigationProvider extends AbstractModelMarkerNavigationProvider {

    /**
	 * @generated
	 */
    public static final String MARKER_TYPE = hub.sam.mof.simulator.editor.diagram.part.M3ActionsDiagramEditorPlugin.ID + ".diagnostic";

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
            M3ActionsDiagramEditorUtil.selectElementsInDiagram(editor, Arrays.asList(new EditPart[] { targetEditPart }));
        }
    }

    /**
	 * @generated
	 */
    public static void deleteMarkers(IResource resource) {
        try {
            resource.deleteMarkers(MARKER_TYPE, true, org.eclipse.core.resources.IResource.DEPTH_ZERO);
        } catch (CoreException e) {
            M3ActionsDiagramEditorPlugin.getInstance().logError("Failed to delete validation markers", e);
        }
    }

    /**
	 * @generated
	 */
    public static IMarker addMarker(IFile file, String elementId, String location, String message, int statusSeverity) {
        IMarker marker = null;
        try {
            marker = file.createMarker(MARKER_TYPE);
            marker.setAttribute(org.eclipse.core.resources.IMarker.MESSAGE, message);
            marker.setAttribute(org.eclipse.core.resources.IMarker.LOCATION, location);
            marker.setAttribute(org.eclipse.gmf.runtime.common.ui.resources.IMarker.ELEMENT_ID, elementId);
            int markerSeverity = org.eclipse.core.resources.IMarker.SEVERITY_INFO;
            if (statusSeverity == org.eclipse.core.runtime.IStatus.WARNING) {
                markerSeverity = org.eclipse.core.resources.IMarker.SEVERITY_WARNING;
            } else if (statusSeverity == org.eclipse.core.runtime.IStatus.ERROR || statusSeverity == org.eclipse.core.runtime.IStatus.CANCEL) {
                markerSeverity = org.eclipse.core.resources.IMarker.SEVERITY_ERROR;
            }
            marker.setAttribute(org.eclipse.core.resources.IMarker.SEVERITY, markerSeverity);
        } catch (CoreException e) {
            M3ActionsDiagramEditorPlugin.getInstance().logError("Failed to create validation marker", e);
        }
        return marker;
    }
}
