package uk.ac.bolton.archimate.editor.views.tree;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import uk.ac.bolton.archimate.editor.diagram.IArchimateDiagramEditor;
import uk.ac.bolton.archimate.editor.diagram.IDiagramModelEditor;
import uk.ac.bolton.archimate.editor.preferences.Preferences;
import uk.ac.bolton.archimate.editor.ui.components.PartListenerAdapter;
import uk.ac.bolton.archimate.model.IArchimateElement;
import uk.ac.bolton.archimate.model.IDiagramModel;
import uk.ac.bolton.archimate.model.IDiagramModelArchimateConnection;
import uk.ac.bolton.archimate.model.IDiagramModelArchimateObject;

/**
 * Keeps Tree and Diagram Editors in Sync
 * 
 * @author Phillip Beauvoir
 */
public class TreeSelectionSynchroniser implements ISelectionChangedListener {

    public static TreeSelectionSynchroniser INSTANCE = new TreeSelectionSynchroniser();

    private ITreeModelView fTreeView;

    /**
     * List of active editors
     */
    private List<IDiagramModelEditor> fDiagramEditors = new ArrayList<IDiagramModelEditor>();

    private boolean isDispatching = false;

    private boolean fDoSync = true;

    private SelectionChangedEvent fLastEvent;

    private TreeSelectionSynchroniser() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        window.getPartService().addPartListener(new PartListenerAdapter() {

            @Override
            public void partActivated(IWorkbenchPart part) {
                if (part instanceof IDiagramModelEditor) {
                    IDiagramModelEditor diagramEditor = (IDiagramModelEditor) part;
                    if (!fDiagramEditors.contains(diagramEditor)) {
                        GraphicalViewer viewer = (GraphicalViewer) diagramEditor.getAdapter(GraphicalViewer.class);
                        if (viewer != null) {
                            viewer.addSelectionChangedListener(TreeSelectionSynchroniser.this);
                            fDiagramEditors.add(diagramEditor);
                        }
                    }
                }
            }

            @Override
            public void partClosed(IWorkbenchPart part) {
                if (part instanceof IDiagramModelEditor) {
                    IDiagramModelEditor diagramEditor = (IDiagramModelEditor) part;
                    if (fDiagramEditors.contains(diagramEditor)) {
                        GraphicalViewer viewer = (GraphicalViewer) diagramEditor.getAdapter(GraphicalViewer.class);
                        if (viewer != null) {
                            viewer.removeSelectionChangedListener(TreeSelectionSynchroniser.this);
                        }
                        fDiagramEditors.remove(diagramEditor);
                    }
                }
            }
        });
    }

    void setTreeModelView(ITreeModelView treeView) {
        if (treeView != fTreeView) {
            fTreeView = treeView;
            fTreeView.getViewer().addSelectionChangedListener(this);
        }
    }

    void removeTreeModelView() {
        if (fTreeView != null) {
            fTreeView.getViewer().removeSelectionChangedListener(this);
            fTreeView = null;
        }
    }

    void setSynchronise(boolean set) {
        fDoSync = set;
    }

    public void refresh() {
        if (fLastEvent != null) {
            selectionChanged(fLastEvent);
        }
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        if (isDispatching) {
            return;
        }
        fLastEvent = event;
        if (!Preferences.doLinkView() || !fDoSync) {
            return;
        }
        isDispatching = true;
        ISelection selection = event.getSelection();
        Object source = event.getSource();
        if (source instanceof GraphicalViewer && fTreeView != null) {
            List<Object> list = new ArrayList<Object>();
            for (Object o : ((IStructuredSelection) selection).toArray()) {
                if (o instanceof EditPart) {
                    Object model = ((EditPart) o).getModel();
                    if (model instanceof IDiagramModelArchimateObject) {
                        model = ((IDiagramModelArchimateObject) model).getArchimateElement();
                        list.add(model);
                    } else if (model instanceof IDiagramModelArchimateConnection) {
                        model = ((IDiagramModelArchimateConnection) model).getRelationship();
                        list.add(model);
                    } else if (model instanceof IDiagramModel) {
                        list.add(model);
                    }
                }
            }
            fTreeView.getViewer().setSelection(new StructuredSelection(list), true);
        } else if (source instanceof TreeViewer) {
            List<IArchimateElement> list = new ArrayList<IArchimateElement>();
            for (Object o : ((IStructuredSelection) selection).toArray()) {
                if (o instanceof IArchimateElement) {
                    list.add((IArchimateElement) o);
                }
            }
            for (IDiagramModelEditor diagramEditor : fDiagramEditors) {
                if (diagramEditor instanceof IArchimateDiagramEditor) {
                    ((IArchimateDiagramEditor) diagramEditor).selectElements(list.toArray(new IArchimateElement[list.size()]));
                }
            }
        }
        isDispatching = false;
    }
}
