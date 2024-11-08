package odm.diagram.part;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import odm.OWL;
import odm.diagram.edit.parts.OdmEditPartFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.draw2d.DelegatingLayout;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gmf.runtime.diagram.core.preferences.PreferencesHint;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramRootEditPart;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramDropTargetListener;
import org.eclipse.gmf.runtime.diagram.ui.requests.DropObjectsRequest;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.ide.document.StorageDiagramDocumentProvider;
import org.eclipse.gmf.runtime.diagram.ui.resources.editor.ide.editor.FileDiagramEditor;
import org.eclipse.gmf.runtime.emf.core.GMFEditingDomainFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.IGotoMarker;
import com.ontoprise.ontostudio.gui.navigator.SelectionTransfer;
import com.ontoprise.ontostudio.owl.gui.navigator.AbstractOwlEntityTreeElement;
import de.uka.aifb.owl.module.OdmModelSearcher;

/**
 * @generated
 */
public class OdmDiagramEditor extends FileDiagramEditor implements IGotoMarker {

    /**
	 * @generated
	 */
    public static final String ID = "odm.diagram.part.OdmDiagramEditorID";

    /**
	 * @generated
	 */
    public OdmDiagramEditor() {
        super(true);
    }

    /**
	 * @generated
	 */
    protected String getEditingDomainID() {
        return "de.uka.aifb.owl.diagram.EditingDomain";
    }

    /**
	 * @generated NOT
	 */
    protected TransactionalEditingDomain createEditingDomain() {
        TransactionalEditingDomain domain;
        domain = TransactionalEditingDomain.Registry.INSTANCE.getEditingDomain(getEditingDomainID());
        if (domain == null) {
            domain = GMFEditingDomainFactory.INSTANCE.createEditingDomain();
            System.out.println("create new editing domain " + domain);
            domain.setID(getEditingDomainID());
        }
        return domain;
    }

    /**
	 * @generated
	 */
    protected void setDocumentProvider(IEditorInput input) {
        if (input.getAdapter(IFile.class) != null) {
            setDocumentProvider(new OdmDocumentProvider());
        } else {
            setDocumentProvider(new StorageDiagramDocumentProvider());
        }
    }

    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        getDiagramGraphicalViewer().addDropTargetListener(new DiagramDropTargetListener(getDiagramGraphicalViewer(), SelectionTransfer.getInstance()) {

            @Override
            protected List getObjectsBeingDropped() {
                if (getCurrentEvent().data == null) {
                    return null;
                }
                List<Object> objectsBeingDropped = new ArrayList<Object>();
                TransferData[] data = getCurrentEvent().dataTypes;
                for (int i = 0; i < data.length; i++) {
                    if (SelectionTransfer.getInstance().isSupportedType(data[i])) {
                        IStructuredSelection selection = (IStructuredSelection) SelectionTransfer.getInstance().nativeToJava(data[i]);
                        for (Object o : selection.toList()) {
                            if (o instanceof TreeItem && ((TreeItem) o).getData() instanceof AbstractOwlEntityTreeElement) {
                                AbstractOwlEntityTreeElement treeElement = (AbstractOwlEntityTreeElement) ((TreeItem) o).getData();
                                OWL owl = (OWL) getDiagram().getElement();
                                OdmModelSearcher searcher = new OdmModelSearcher(owl);
                                objectsBeingDropped.addAll(searcher.getEObjectsAndTreeElement(treeElement));
                            }
                        }
                    }
                }
                return objectsBeingDropped;
            }

            protected void handleDragOver() {
                getCurrentEvent().detail = DND.DROP_COPY;
                super.handleDragOver();
            }

            @Override
            public boolean isEnabled(DropTargetEvent event) {
                super.isEnabled(event);
                ISelection selection = SelectionTransfer.getInstance().getSelection();
                if (selection instanceof IStructuredSelection) {
                    List selectedElements = ((IStructuredSelection) selection).toList();
                    if (selectedElements == null) return false;
                    for (Iterator i = selectedElements.iterator(); i.hasNext(); ) {
                        Object o = i.next();
                        if (!(o instanceof TreeItem)) return false;
                    }
                    return true;
                }
                return false;
            }

            @Override
            protected void updateTargetRequest() {
                ((DropObjectsRequest) getTargetRequest()).setLocation(getDropLocation());
            }
        });
        DiagramRootEditPart root = (DiagramRootEditPart) getDiagramGraphicalViewer().getRootEditPart();
        LayeredPane printableLayers = (LayeredPane) root.getLayer(LayerConstants.PRINTABLE_LAYERS);
        FreeformLayer extLabelsLayer = new FreeformLayer();
        extLabelsLayer.setLayoutManager(new DelegatingLayout());
        printableLayers.addLayerAfter(extLabelsLayer, OdmEditPartFactory.EXTERNAL_NODE_LABELS_LAYER, LayerConstants.PRIMARY_LAYER);
        LayeredPane scalableLayers = (LayeredPane) root.getLayer(LayerConstants.SCALABLE_LAYERS);
        FreeformLayer scaledFeedbackLayer = new FreeformLayer();
        scaledFeedbackLayer.setEnabled(false);
        scalableLayers.addLayerAfter(scaledFeedbackLayer, LayerConstants.SCALED_FEEDBACK_LAYER, DiagramRootEditPart.DECORATION_UNPRINTABLE_LAYER);
    }

    /**
	 * @generated
	 */
    protected PreferencesHint getPreferencesHint() {
        return OdmDiagramEditorPlugin.DIAGRAM_PREFERENCES_HINT;
    }
}
