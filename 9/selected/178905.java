package net.taylor.uml2.activitydiagram.part;

import net.taylor.uml2.activitydiagram.edit.parts.UMLEditPartFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.draw2d.DelegatingLayout;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramRootEditPart;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.ide.document.StorageDiagramDocumentProvider;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.ide.editor.FileDiagramEditor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.IGotoMarker;

/**
 * @generated
 */
public class UMLDiagramEditor extends FileDiagramEditor implements IGotoMarker {

    /**
	 * @generated
	 */
    public static final String ID = "net.taylor.uml2.activitydiagram.part.UMLDiagramEditorID";

    /**
	 * @generated
	 */
    public UMLDiagramEditor() {
        super(true);
    }

    /**
	 * @generated NOT
	 */
    protected String getEditingDomainID() {
        return "net.taylor.EditingDomain";
    }

    /**
	 * @NOT generated
	 */
    protected TransactionalEditingDomain createEditingDomain() {
        TransactionalEditingDomain d = TransactionalEditingDomain.Registry.INSTANCE.getEditingDomain("net.taylor.EditingDomain");
        return d;
    }

    /**
	 * @generated
	 */
    protected void setDocumentProvider(IEditorInput input) {
        if (input.getAdapter(IFile.class) != null) {
            setDocumentProvider(new UMLDocumentProvider());
        } else {
            setDocumentProvider(new StorageDiagramDocumentProvider());
        }
    }

    /**
	 * @generated
	 */
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        DiagramRootEditPart root = (DiagramRootEditPart) getDiagramGraphicalViewer().getRootEditPart();
        LayeredPane printableLayers = (LayeredPane) root.getLayer(LayerConstants.PRINTABLE_LAYERS);
        FreeformLayer extLabelsLayer = new FreeformLayer();
        extLabelsLayer.setLayoutManager(new DelegatingLayout());
        printableLayers.addLayerAfter(extLabelsLayer, UMLEditPartFactory.EXTERNAL_NODE_LABELS_LAYER, LayerConstants.PRIMARY_LAYER);
        LayeredPane scalableLayers = (LayeredPane) root.getLayer(LayerConstants.SCALABLE_LAYERS);
        FreeformLayer scaledFeedbackLayer = new FreeformLayer();
        scaledFeedbackLayer.setEnabled(false);
        scalableLayers.addLayerAfter(scaledFeedbackLayer, LayerConstants.SCALED_FEEDBACK_LAYER, DiagramRootEditPart.DECORATION_UNPRINTABLE_LAYER);
    }
}
